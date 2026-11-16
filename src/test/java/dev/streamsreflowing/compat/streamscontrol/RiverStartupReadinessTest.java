package dev.streamsreflowing.compat.streamscontrol;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static dev.streamsreflowing.compat.streamscontrol.RiverStartupReadiness.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;

/** 测河流自己的发布出口；发布回调作为端口，不用道路 Fake 替代河流状态。 */
class RiverStartupReadinessTest {
    @Test
    void unstartedCannotPublish() {
        RiverStartupReadiness gate = new RiverStartupReadiness();
        AtomicInteger calls = new AtomicInteger();
        assertEquals(NOT_STARTED, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(0, calls.get());
    }

    @Test
    void successfulBuildPublishes() {
        RiverStartupReadiness gate = started();
        AtomicInteger calls = new AtomicInteger();
        gate.completeWhen(CompletableFuture.completedFuture(null));
        assertEquals(COMPLETE, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void disabledIsExplicitBypass() {
        RiverStartupReadiness gate = started();
        AtomicInteger calls = new AtomicInteger();
        gate.disabled();
        assertEquals(DISABLED, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void asynchronousFailureCannotPublish() {
        RiverStartupReadiness gate = started();
        AtomicInteger calls = new AtomicInteger();
        gate.completeWhen(CompletableFuture.failedFuture(new IllegalStateException("build")));
        assertEquals(FAILED, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(0, calls.get());
    }

    @Test
    void setupFailureCannotPublishOrBeOverwrittenBySecondHook() {
        RiverStartupReadiness gate = started();
        AtomicInteger calls = new AtomicInteger();
        gate.failed();
        assertFalse(gate.begin());
        gate.completeWhen(CompletableFuture.completedFuture(null));
        assertEquals(FAILED, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(0, calls.get());
    }

    @Test
    void cleanupFailureAlsoBlocksReadiness() {
        RiverStartupReadiness gate = started();
        AtomicInteger calls = new AtomicInteger();
        gate.completeWhen(CompletableFuture.<Void>completedFuture(null).whenComplete((v, error) -> {
            throw new IllegalStateException("cleanup");
        }));
        assertEquals(FAILED, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(0, calls.get());
    }

    @Test
    void allPlatesMustCompleteBeforePublishing() throws Exception {
        RiverStartupReadiness gate = started();
        CompletableFuture<Void> first = new CompletableFuture<>();
        CompletableFuture<Void> second = new CompletableFuture<>();
        gate.completeWhen(CompletableFuture.allOf(first, second));
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CompletableFuture<RiverStartupReadiness.Outcome> publishing = CompletableFuture.supplyAsync(() -> {
            entered.countDown();
            return gate.publishIfReady(calls::incrementAndGet);
        });
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            first.complete(null);
            // 等待确实不能在只完成一块时返回，避免只碰巧在工作线程运行前检查。
            assertThrows(TimeoutException.class, () -> publishing.get(100, TimeUnit.MILLISECONDS));
            assertEquals(0, calls.get());
            second.complete(null);
            assertEquals(COMPLETE, publishing.get(2, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
        } finally {
            gate.stop();
            publishing.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void stopReleasesWaitAndLateSuccessCannotPublish() throws Exception {
        RiverStartupReadiness gate = started();
        CompletableFuture<Void> work = new CompletableFuture<>();
        gate.completeWhen(work);
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<RiverStartupReadiness.Outcome> waiting = CompletableFuture.supplyAsync(
                () -> gate.publishIfReady(calls::incrementAndGet));
        try {
            gate.stop();
            assertEquals(STOPPED, waiting.get(2, TimeUnit.SECONDS));
            assertFalse(work.isCancelled());
            work.complete(null);
            assertEquals(STOPPED, gate.publishIfReady(calls::incrementAndGet));
            assertEquals(0, calls.get());
            assertFalse(gate.begin());
        } finally {
            gate.stop();
        }
    }

    @Test
    void stopAfterCompletionStillPreventsBroadcast() {
        RiverStartupReadiness gate = started();
        gate.completeWhen(CompletableFuture.completedFuture(null));
        gate.stop();
        AtomicInteger calls = new AtomicInteger();
        assertEquals(STOPPED, gate.publishIfReady(calls::incrementAndGet));
        assertEquals(0, calls.get());
    }

    @Test
    void reopenedWorldHasIndependentResult() {
        RiverStartupReadiness old = started();
        old.failed();
        old.stop();
        RiverStartupReadiness reopened = started();
        reopened.completeWhen(CompletableFuture.completedFuture(null));
        AtomicInteger calls = new AtomicInteger();
        assertEquals(COMPLETE, reopened.publishIfReady(calls::incrementAndGet));
        assertEquals(1, calls.get());
        assertEquals(STOPPED, old.publishIfReady(calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    private static RiverStartupReadiness started() {
        RiverStartupReadiness gate = new RiverStartupReadiness();
        assertTrue(gate.begin());
        return gate;
    }
}
