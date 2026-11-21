package dev.streamsreflowing.compat.streamscontrol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 驱动真实会话租约与计数器，覆盖缓存探测、失败和旧世界迟到回调。 */
class StartupProgressUiTest {
    @AfterEach void clear() { StreamsStartupProgress.clear(); }
    private RiverStartupSession session() { var s = new RiverStartupSession(() -> {}, () -> {}); s.enter(); return s; }

    @Test void cacheChecksAreVisibleBeforeBuildingAndPercentDoesNotDrop() {
        var s = session(); s.beginCache(12, 3);
        assertTrue(StreamsStartupProgress.snapshot().active());
        assertEquals(StreamsStartupProgress.Stage.CACHE_CHECK, StreamsStartupProgress.snapshot().stage());
        for (int i = 0; i < 12; i++) s.cacheChecked();
        var cache = StreamsStartupProgress.snapshot();
        assertEquals(12, cache.checkedPlates()); assertEquals(0, cache.completedPlates());
        assertEquals(10, cache.percent());
        s.progress(12, 3, 3);
        var build = StreamsStartupProgress.snapshot();
        assertEquals(StreamsStartupProgress.Stage.BUILDING, build.stage());
        assertEquals(3, build.completedPlates()); assertTrue(build.percent() >= cache.percent());
        assertEquals(4, s.completedBuild(true));
        s.finish(true);
        assertEquals(100, StreamsStartupProgress.snapshot().percent());
        assertFalse(StreamsStartupProgress.snapshot().active());
    }

    @Test void failedBuildIsNeverCountedAsSuccessfulAndFailureStaysVisible() {
        var s = session(); s.beginCache(3, 1); s.cacheChecked(); s.cacheChecked(); s.cacheChecked();
        s.progress(3, 1, 1);
        assertEquals(-1, s.completedBuild(false));
        s.finish(false);
        var failure = StreamsStartupProgress.snapshot();
        assertEquals(1, failure.completedPlates()); assertEquals(1, failure.failedPlates());
        assertEquals(StreamsStartupProgress.Stage.FAILED, failure.stage());
        assertFalse(failure.active()); assertTrue(failure.percent() < 100);
    }

    @Test void allCachedAndLateOldCallbacksHaveCorrectOwnership() {
        var old = session(); old.beginCache(2, 1); old.cacheChecked(); old.cacheChecked(); old.progress(2, 2, 1); old.finish(true);
        assertEquals(100, StreamsStartupProgress.snapshot().percent());
        var fresh = session(); fresh.beginCache(10, 2);
        old.cacheChecked(); old.completedBuild(false); old.close();
        var current = StreamsStartupProgress.snapshot();
        assertEquals(0, current.checkedPlates()); assertEquals(0, current.failedPlates());
        assertEquals(StreamsStartupProgress.Stage.CACHE_CHECK, current.stage());
        assertTrue(current.active()); fresh.close();
    }

    @Test void initializationFailureCancelsQueuedWorkAndRetainsFailureUntilClose() {
        var queue = new ArrayDeque<Runnable>(); var s = session(); s.beginCache(2, 1);
        var work = s.submit(queue::add, () -> fail("cancelled task must not run"));
        assertEquals(1, queue.size()); s.failAndClose(); queue.remove().run();
        assertTrue(work.isCancelled());
        assertEquals(StreamsStartupProgress.Stage.FAILED, StreamsStartupProgress.snapshot().stage());
        // 世界停止必须清理失败卡片，新世界不得继承上一轮错误。
        s.close();
        assertEquals(StreamsStartupProgress.Stage.FINISHED, StreamsStartupProgress.snapshot().stage());
    }

    @Test void cardFitsEveryLogicalScreenWithoutDroppingRows() {
        // 从小窗口到4K逻辑尺寸遍历；先断言实际有内容，再核对四条屏幕边界。
        for (int width : new int[] {160, 240, 320, 426, 854, 1920}) {
            for (int height : new int[] {90, 180, 240, 360, 480, 1080}) {
                for (int rows : new int[] {1, 7, 8, 10, 11}) {
                    var layout = LoadingCardLayout.fit(width, height, 280, rows);
                    assertTrue(layout.width() > 0 && layout.height() > 0);
                    assertTrue(layout.scale() > 0 && layout.scale() <= 1);
                    assertTrue(layout.x() >= 0 && layout.y() >= 0);
                    assertTrue(layout.x() + layout.width() * layout.scale() <= width + 0.001);
                    assertTrue(layout.y() + layout.height() * layout.scale() <= height + 0.001);
                    assertEquals(82 + rows * 12 + layout.bandHeight(), layout.height());
                }
            }
        }
        assertEquals(1F, LoadingCardLayout.fit(854, 480, 280, 8).scale());
        assertEquals(120, LoadingCardLayout.fit(854, 480, 280, 8).bandHeight());
    }

    @Test void productionPublishesCacheBeforeProbeAndDoesNotCountFailedBuild() throws Exception {
        var service = Files.readString(Path.of("src/main/java/dev/streamsreflowing/compat/streamscontrol/StreamsPrewarmService.java"));
        int start = service.indexOf("state.startupWork.beginCache(");
        int probe = service.indexOf("int[] states = primePlateStates(");
        assertTrue(start >= 0 && probe > start);
        assertTrue(service.contains("session.cacheChecked();"));
        assertTrue(service.contains("state.startupWork.completedBuild(failure == null)"));
        assertTrue(service.contains("state.startupWork.failAndClose();"));
        var renderer = Files.readString(Path.of("src/main/java/dev/streamsreflowing/compat/streamscontrol/client/StreamsLoadingProgressRenderer.java"));
        assertTrue(renderer.contains("case CACHE_CHECK ->"));
        assertTrue(renderer.contains("case FAILED ->"));
        assertTrue(renderer.contains("LoadingCardLayout.fit("));
        assertTrue(renderer.contains("graphics.pose().scale(layout.scale(), layout.scale(), 1F)"));
    }
}
