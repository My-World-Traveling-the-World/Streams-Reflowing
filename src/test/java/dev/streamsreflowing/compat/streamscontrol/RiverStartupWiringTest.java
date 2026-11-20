package dev.streamsreflowing.compat.streamscontrol;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 静态接线守卫只保证入口未旁路结果门，不冒充 Minecraft Mixin 运行时验证。 */
class RiverStartupWiringTest {
    private static final Path MAIN = Path.of("src/main/java/dev/streamsreflowing");

    @Test
    void bothLifecycleHooksUsePublisherGate() throws Exception {
        String source = Files.readString(MAIN.resolve("fabric/mixin/StreamsServerLifecycleMixin.java"));
        assertEquals(2, source.split(java.util.regex.Pattern.quote(
                "StreamsPrewarmService.publishStartupReadiness(level);"), -1).length - 1);
        assertFalse(source.contains("RiverReadinessBroadcast.publishReady("));
        assertTrue(source.contains("StreamsPrewarmService.stop((MinecraftServer) (Object) this);"));
        assertTrue(source.contains("RiverReadinessBroadcast.reset();"));
    }

    @Test
    void serviceRecordsAllFailurePathsAndStopsWorldGate() throws Exception {
        String source = Files.readString(MAIN.resolve("compat/streamscontrol/StreamsPrewarmService.java"));
        assertTrue(source.contains("if (!state.startup.begin()) return;"));
        assertTrue(source.contains("state.startup.disabled();"));
        assertTrue(source.contains("throw new IllegalStateException(\"Streams startup engine unavailable\");"));
        assertTrue(source.contains("state.startup.failed();"));
        assertTrue(source.contains("state.startup.completeWhen(combined.whenComplete("));
        assertTrue(source.contains("state.startup.publishIfReady(() -> {"));
        assertTrue(source.contains("state.startup.stop();"));
        // 结果门与进度租约是两个契约；只保护ready仍会留下旧任务污染新进度。
        assertTrue(source.contains("state.startupWork.close();"));
        assertTrue(source.contains("state.startup.stop();\n            state.startupWork.close();"));
        assertTrue(source.contains("state.startupWork.submit("));
        assertTrue(source.contains("session.submit(executor, () -> {"));
        assertFalse(source.contains("StreamsStartupProgress.setCompleted("));
        assertFalse(source.contains("StreamsStartupProgress.finish();"));
        assertFalse(source.contains("startupFuture"));
    }

    @Test void runtimeRetryBypassesUnchangedCenterAndStopsWithWorld() throws Exception {
        String source = Files.readString(MAIN.resolve("compat/streamscontrol/StreamsPrewarmService.java"));
        // 行为测试走生产队列；另守正式扫描没有绕开重试与停止端口。
        assertTrue(source.contains("boolean retryDue = plates.stream().anyMatch(plate -> state.runtimeWork.retryDue(plate.key()));"));
        assertTrue(source.contains("if (!retryDue && previous != null"));
        assertTrue(source.contains("state.runtimeWork.submit(plate, RUNTIME_EXECUTOR,"));
        assertTrue(source.contains("state.runtimeWork.close();"));
        assertTrue(source.contains("state.runtimeWork.retainCoverage(activeCoverage);"));
        assertTrue(source.contains("state.playerCenters.clear();"));
    }
}
