package dev.streamsreflowing.compat.streamscontrol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 使用生产队列的真实worker入口，故障后不移动玩家也能重试，而非只验证计数器非负。 */
class RuntimePrewarmQueueTest {
    private static final Executor DIRECT = Runnable::run;
    private static final class World implements RuntimePrewarmQueue.Access<Integer> {
        boolean permit = true;
        int builds, failures;
        final Set<Integer> ready = new HashSet<>();
        final ArrayList<RuntimePrewarmQueue.Reason> reasons = new ArrayList<>();
        final ArrayList<Long> delays = new ArrayList<>();
        @Override public boolean permit() { return permit; }
        @Override public boolean ready(Integer key) { return ready.contains(key); }
        @Override public void build(Integer key) {
            builds++;
            if (failures-- > 0) throw new IllegalStateException("构建故障");
            ready.add(key);
        }
        @Override public void retry(Integer key, RuntimePrewarmQueue.Reason reason, int attempt,
                                    long delay, Throwable failure) {
            reasons.add(reason); delays.add(delay);
            if (reason == RuntimePrewarmQueue.Reason.FAILED) assertNotNull(failure);
        }
    }

    @Test void asynchronousFailureRetriesWithoutAChangedCenter() {
        var clock = new AtomicLong();
        var queue = new RuntimePrewarmQueue<Integer>(16, clock::get);
        var world = new World(); world.failures = 1;
        assertEquals(1, queue.submit(4, DIRECT, world));
        assertEquals(1, queue.retryCount()); assertFalse(queue.retryDue(4));
        assertEquals(0, queue.submit(4, DIRECT, world));
        assertEquals(1, world.builds);
        clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
        assertTrue(queue.retryDue(4));
        assertEquals(1, queue.submit(4, DIRECT, world));
        assertEquals(2, world.builds); assertTrue(world.ready.contains(4));
        assertEquals(0, queue.retryCount()); assertEquals(0, queue.pendingCount());
        assertEquals(java.util.List.of(RuntimePrewarmQueue.Reason.FAILED), world.reasons);
    }

    @Test void withdrawnPermitIsDeferredAndCanResume() {
        var clock = new AtomicLong();
        var queue = new RuntimePrewarmQueue<Integer>(16, clock::get);
        var world = new World(); world.permit = false;
        queue.submit(7, DIRECT, world);
        assertEquals(0, world.builds);
        assertEquals(java.util.List.of(RuntimePrewarmQueue.Reason.WITHDRAWN), world.reasons);
        clock.set(TimeUnit.SECONDS.toNanos(1)); world.permit = true;
        queue.submit(7, DIRECT, world);
        assertEquals(1, world.builds); assertEquals(0, queue.retryCount());
    }

    @Test void samePlateHasOnlyOneQueuedOwner() {
        var tasks = new ArrayDeque<Runnable>();
        var queue = new RuntimePrewarmQueue<Integer>(16, () -> 0);
        var world = new World();
        assertEquals(1, queue.submit(3, tasks::add, world));
        assertEquals(0, queue.submit(3, tasks::add, world));
        assertEquals(1, queue.pendingCount()); assertEquals(1, tasks.size());
        tasks.remove().run();
        assertEquals(1, world.builds); assertEquals(0, queue.pendingCount());
    }

    @Test void closingStopsQueuedWorkAndForbidsRetryResurrection() {
        var tasks = new ArrayDeque<Runnable>();
        var queue = new RuntimePrewarmQueue<Integer>(16, () -> 0);
        var world = new World(); world.failures = 5;
        queue.submit(3, tasks::add, world); queue.close(); tasks.remove().run();
        assertEquals(0, world.builds); assertTrue(world.reasons.isEmpty());
        assertEquals(0, queue.retryCount()); assertEquals(0, queue.pendingCount());
        assertEquals(0, queue.submit(3, DIRECT, world));
    }

    @Test void executorRejectionIsAnExplicitRetryAndReleasesClaim() {
        var clock = new AtomicLong();
        var queue = new RuntimePrewarmQueue<Integer>(16, clock::get);
        var world = new World();
        assertEquals(-1, queue.submit(2, task -> { throw new RejectedExecutionException(); }, world));
        assertEquals(0, queue.pendingCount()); assertEquals(1, queue.retryCount());
        assertEquals(java.util.List.of(RuntimePrewarmQueue.Reason.REJECTED), world.reasons);
        clock.set(TimeUnit.SECONDS.toNanos(1));
        assertEquals(1, queue.submit(2, DIRECT, world)); assertEquals(1, world.builds);
    }

    @Test void retryTableIsBoundedAndCoverageDropsObsoleteEntries() {
        var queue = new RuntimePrewarmQueue<Integer>(2, () -> 0);
        var world = new World(); world.failures = 5;
        for (int key = 0; key < 3; key++) queue.submit(key, DIRECT, world);
        assertEquals(3, world.reasons.size()); assertEquals(2, queue.retryCount());
        queue.retainCoverage(Set.of(2));
        assertEquals(1, queue.retryCount());
        queue.retainCoverage(Set.of()); assertEquals(0, queue.retryCount());
    }

    @Test void repeatedFailuresBackOffAndCapAtSixtySeconds() {
        var clock = new AtomicLong();
        var queue = new RuntimePrewarmQueue<Integer>(16, clock::get);
        var world = new World(); world.failures = 20;
        for (int attempt = 0; attempt < 9; attempt++) {
            assertEquals(1, queue.submit(8, DIRECT, world));
            long expected = TimeUnit.SECONDS.toNanos(Math.min(60, 1L << attempt));
            assertEquals(expected, world.delays.getLast());
            clock.addAndGet(expected - 1); assertFalse(queue.retryDue(8));
            clock.incrementAndGet(); assertTrue(queue.retryDue(8));
        }
        assertEquals(9, world.builds);
    }

    @Test void monotonicClockWrapDoesNotLoseRetry() {
        var clock = new AtomicLong(Long.MAX_VALUE - 10);
        var queue = new RuntimePrewarmQueue<Integer>(16, clock::get);
        var world = new World(); world.failures = 1;
        queue.submit(8, DIRECT, world);
        assertFalse(queue.retryDue(8));
        clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
        assertTrue(queue.retryDue(8)); queue.submit(8, DIRECT, world);
        assertEquals(2, world.builds);
    }
}
