package dev.streamsreflowing.core.cache;

import static org.junit.jupiter.api.Assertions.*;

import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PlateRiverCacheTest {
    @Test void previewDoesNotCreateOrWaitAndCannotSurviveClose() {
        var cache=new PlateRiverCache(()->4);
        assertNull(cache.peek(KEY));assertEquals(0,cache.size());
        var queue=new ArrayDeque<Runnable>();cache.request(KEY,queue::add,()->NETWORK);
        assertNull(cache.peek(KEY));assertEquals(1,queue.size());
        queue.remove().run();assertSame(NETWORK,cache.peek(KEY));
        cache.close();assertNull(cache.peek(KEY));
    }
    private static final PlateKey KEY = new PlateKey(-4, 7);
    private static final PlateKey OTHER = new PlateKey(1, -3);
    private static final RiverNetwork NETWORK = new RiverNetwork(KEY, List.of(), null, List.of());

    @Test void syncPrewarmCanClaimAQueuedPlateWithoutBuildingItTwice() {
        var queue = new ArrayDeque<Runnable>();
        try (var cache = new PlateRiverCache(() -> 4)) {
            var first = cache.request(KEY, queue::add, PlateRiverCacheTest::unexpectedBuild);
            var second = cache.request(KEY, queue::add, PlateRiverCacheTest::unexpectedBuild);
            assertFalse(first.isDone());
            assertEquals(1, queue.size());
            assertSame(NETWORK, cache.get(KEY, () -> NETWORK, PlateRiverCacheTest::unexpectedBuild));
            queue.remove().run();
            first.join();
            second.join();
            assertTrue(cache.isReady(KEY));
            assertEquals(1, cache.size());
        }
    }

    @Test void subscriptionDuringRetrySharesItsResultAndDoesNotStartThirdAttempt() {
        var queue = new ArrayDeque<Runnable>();
        var attempts = new AtomicInteger();
        var retrying = new CountDownLatch(1);
        var finishRetry = new CountDownLatch(1);
        var failure = new IllegalStateException("两次都失败");
        try (var cache = new PlateRiverCache(() -> 4);
             var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var first = cache.request(KEY, queue::add, () -> {
                if (attempts.incrementAndGet() == 2) {
                    retrying.countDown();
                    waitFor(finishRetry);
                }
                throw failure;
            });
            var running = worker.submit(queue.remove());
            try {
                waitFor(retrying);
                var later = cache.request(KEY, queue::add, PlateRiverCacheTest::unexpectedBuild);
                assertFalse(first.isDone());
                assertFalse(later.isDone());
                assertTrue(queue.isEmpty(), "重试时订阅不能再排新的建板任务");
                finishRetry.countDown();
                running.get(5, TimeUnit.SECONDS);
                assertTrue(first.isCompletedExceptionally(), "首次订阅必须拿到失败结论");
                assertTrue(later.isCompletedExceptionally(), "重试期间订阅也必须结案");
                assertSame(failure, assertThrows(CompletionException.class, first::join).getCause());
                assertSame(failure, assertThrows(CompletionException.class, later::join).getCause());
                assertFalse(cache.isReady(KEY));
                assertEquals(2, attempts.get());
                // 下一批请求允许再次尝试，当前失败不会永久污染板块。
                assertSame(NETWORK, cache.get(KEY, () -> NETWORK, PlateRiverCacheTest::unexpectedBuild));
            } finally {
                finishRetry.countDown();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failureInWorker) {
            throw new AssertionError(failureInWorker);
        }
    }

    @Test void chunkCanSubscribeAfterBackgroundOwnerStartedAndReceiveRetrySuccess() throws Exception {
        var attempts = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var failFirst = new CountDownLatch(1);
        try (var cache = new PlateRiverCache(() -> 4);
             var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var owner = worker.submit(() -> cache.get(KEY, () -> {
                if (attempts.incrementAndGet() == 1) {
                    entered.countDown();
                    waitFor(failFirst);
                    throw new IllegalStateException("首次投放失败");
                }
                return NETWORK;
            }, PlateRiverCacheTest::unexpectedBuild));
            try {
                waitFor(entered);
                var waiter = cache.request(KEY, unexpectedQueue(), PlateRiverCacheTest::unexpectedBuild);
                assertFalse(waiter.isDone());
                failFirst.countDown();
                waiter.get(5, TimeUnit.SECONDS);
                assertSame(NETWORK, owner.get(5, TimeUnit.SECONDS));
                assertEquals(2, attempts.get());
                assertTrue(cache.isReady(KEY));
            } finally {
                failFirst.countDown();
            }
        }
    }

    @Test void closingCancelsQueuedAndRunningPlateAndLateResultCannotPublish() throws Exception {
        var queue = new ArrayDeque<Runnable>();
        var entered = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try (var cache = new PlateRiverCache(() -> 4);
             var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var queued = cache.request(OTHER, queue::add, PlateRiverCacheTest::unexpectedBuild);
            var running = cache.request(KEY, worker, () -> {
                entered.countDown();
                waitFor(finish);
                return NETWORK;
            });
            var independent = new PlateRiverCache(() -> 4);
            try {
                waitFor(entered);
                cache.close();
                assertTrue(queued.isCompletedExceptionally());
                assertTrue(running.isCompletedExceptionally());
                assertInstanceOf(CancellationException.class, assertThrows(CompletionException.class, queued::join).getCause());
                assertInstanceOf(CancellationException.class, assertThrows(CompletionException.class, running::join).getCause());
                queue.remove().run();
                assertSame(NETWORK, independent.get(KEY, () -> NETWORK, PlateRiverCacheTest::unexpectedBuild));
            } finally {
                finish.countDown();
                independent.close();
            }
            worker.submit(() -> {}).get(5, TimeUnit.SECONDS);
            assertFalse(cache.isReady(KEY));
            assertEquals(0, cache.size());
        }
    }

    @Test void restoringWhileOwnerBuildsReturnsWithoutWaitingOrReplacingResult() throws Exception {
        var entered = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try (var cache = new PlateRiverCache(() -> 4);
             var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var owner = worker.submit(() -> cache.get(KEY, () -> {
                entered.countDown();
                waitFor(finish);
                return NETWORK;
            }, PlateRiverCacheTest::unexpectedBuild));
            try {
                waitFor(entered);
                // 已占有的入口不能把磁盘探测变成需要等地形采样的调用。
                assertFalse(cache.restoreIfIdle(KEY, new RiverNetwork(KEY, List.of(), null, List.of())));
                assertFalse(cache.isReady(KEY));
                assertEquals(0, cache.size());
            } finally {
                finish.countDown();
            }
            assertSame(NETWORK, owner.get(5, TimeUnit.SECONDS));
        }
    }

    @Test void capacityCountsReadyPlatesAndRetainsRunningOwner() throws Exception {
        var queue = new ArrayDeque<Runnable>();
        var maximum = new AtomicInteger(2);
        try (var cache = new PlateRiverCache(maximum::get)) {
            var pending = cache.request(KEY, queue::add, () -> NETWORK);
            int count = 0;
            for (PlateKey key : List.of(OTHER, new PlateKey(-7, 4), new PlateKey(-1, -2))) {
                assertTrue(cache.restoreIfIdle(key, new RiverNetwork(key, List.of(), null, List.of())));
                assertEquals(Math.min(++count, 2), cache.size());
            }
            assertFalse(cache.isReady(OTHER), "最旧的成品板块应当先离开缓存");
            cache.request(KEY, unexpectedQueue(), PlateRiverCacheTest::unexpectedBuild);
            maximum.set(1);
            queue.remove().run();
            pending.get(5, TimeUnit.SECONDS);
            assertTrue(cache.isReady(KEY));
            assertEquals(1, cache.size());
        }
    }

    private static Executor unexpectedQueue() {
        return task -> { throw new AssertionError("这个板块已有认领者"); };
    }

    private static RiverNetwork unexpectedBuild() {
        throw new AssertionError("这个板块不应重复构建");
    }

    private static void waitFor(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "未等到夹具通知");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
