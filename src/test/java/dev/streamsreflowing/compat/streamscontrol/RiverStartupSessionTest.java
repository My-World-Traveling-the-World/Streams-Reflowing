package dev.streamsreflowing.compat.streamscontrol;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 测试真实任务端口和进度组件；不借Minecraft启动结果推断会话清理正确。 */
class RiverStartupSessionTest {
    @AfterEach void clear() { StreamsStartupProgress.clear(); }

    @Test void lateOldCallbackCannotFinishOrAdvanceNewProgress() {
        var depth = new AtomicInteger();
        var old = session(depth);
        old.enter(); old.progress(1000, 0, 7); old.close();
        var fresh = session(depth);
        fresh.enter(); fresh.progress(20, 0, 3);
        assertEquals(-1, old.completedOne());
        old.finish(true); old.close();
        var current = StreamsStartupProgress.snapshot();
        assertTrue(current.active());
        assertEquals(0, current.completedPlates());
        assertEquals(20, current.totalPlates());
        assertEquals(1, depth.get());
        fresh.finish(true);
        assertFalse(StreamsStartupProgress.isActive());
        assertEquals(20, StreamsStartupProgress.snapshot().completedPlates());
        assertEquals(0, depth.get());
    }

    @Test void overlappingOldFinishCannotClearNewOwner() {
        var depth = new AtomicInteger();
        var a = session(depth); a.enter(); a.progress(10, 0, 2);
        var b = session(depth); b.enter(); b.progress(30, 7, 3);
        a.completedOne(); a.finish(false); a.close();
        assertTrue(StreamsStartupProgress.isActive());
        assertEquals(7, StreamsStartupProgress.snapshot().completedPlates());
        assertEquals(30, StreamsStartupProgress.snapshot().totalPlates());
        assertEquals(1, depth.get());
        b.close(); assertEquals(0, depth.get());
    }

    @Test void successfulOldFinishCannotCompleteNewProgress() {
        var depth = new AtomicInteger();
        var a = session(depth); a.enter(); a.progress(10, 0, 2);
        var b = session(depth); b.enter(); b.progress(30, 7, 3);
        a.finish(true);
        assertTrue(StreamsStartupProgress.isActive());
        assertEquals(7, StreamsStartupProgress.snapshot().completedPlates());
        b.close(); assertEquals(0, depth.get());
    }

    @Test void closeCancelsQueuedWorkAndReleasesExactlyOnce() {
        var queue = new ArrayDeque<Runnable>();
        var depth = new AtomicInteger();
        var body = new AtomicInteger();
        var session = session(depth); session.enter(); session.enter();
        var a = session.submit(queue::add, body::incrementAndGet);
        var b = session.submit(queue::add, body::incrementAndGet);
        assertEquals(2, queue.size());
        session.close(); session.finish(true); session.close();
        while (!queue.isEmpty()) queue.remove().run();
        assertTrue(a.isCancelled()); assertTrue(b.isCancelled());
        assertEquals(0, body.get()); assertEquals(0, depth.get());
        assertThrows(RejectedExecutionException.class, () -> session.submit(Runnable::run, body::incrementAndGet));
    }

    @Test void alreadyRunningSharedBuildIsNotInterrupted() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var ended = new CountDownLatch(1);
        var depth = new AtomicInteger();
        var interrupted = new AtomicInteger();
        var session = session(depth); session.enter();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            session.submit(executor, () -> {
                entered.countDown();
                try { assertTrue(release.await(3, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { interrupted.incrementAndGet(); }
                finally { ended.countDown(); }
            });
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            session.close();
            assertEquals(0, depth.get());
            release.countDown();
            assertTrue(ended.await(3, TimeUnit.SECONDS));
            assertEquals(0, interrupted.get());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void failedWorkAndRepeatedCleanupDoNotPublishCompleteProgress() {
        var depth = new AtomicInteger();
        var session = session(depth); session.enter(); session.progress(3, 0, 1);
        CompletableFuture<Void> work = session.submit(Runnable::run, () -> { throw new IllegalStateException("故障注入"); });
        assertTrue(work.isCompletedExceptionally());
        session.finish(false); session.close(); session.finish(true);
        assertEquals(0, depth.get()); assertFalse(StreamsStartupProgress.isActive());
        assertEquals(0, StreamsStartupProgress.snapshot().totalPlates());
    }

    @Test void partialSubmissionFailureCancelsAlreadyQueuedTasks() {
        var queue = new ArrayDeque<Runnable>();
        var calls = new AtomicInteger();
        var depth = new AtomicInteger();
        var session = session(depth); session.enter();
        var first = session.submit(queue::add, calls::incrementAndGet);
        assertThrows(RejectedExecutionException.class, () -> session.submit(
                task -> { throw new RejectedExecutionException("队列拒绝"); }, calls::incrementAndGet));
        session.close();
        queue.remove().run();
        assertTrue(first.isCancelled()); assertEquals(0, calls.get()); assertEquals(0, depth.get());
    }

    @Test void releaseFailureStillClearsOwnedProgressAndDoesNotReleaseTwice() {
        var releases = new AtomicInteger();
        var session = new RiverStartupSession(() -> {}, () -> {
            releases.incrementAndGet(); throw new IllegalStateException("释放故障");
        });
        session.enter(); session.progress(2, 0, 1);
        assertThrows(IllegalStateException.class, session::close);
        session.close();
        assertEquals(1, releases.get()); assertFalse(StreamsStartupProgress.isActive());
    }

    private static RiverStartupSession session(AtomicInteger depth) {
        return new RiverStartupSession(depth::incrementAndGet, depth::decrementAndGet);
    }
}
