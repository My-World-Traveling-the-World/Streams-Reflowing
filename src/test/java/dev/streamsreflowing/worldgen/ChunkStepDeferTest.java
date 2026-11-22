package dev.streamsreflowing.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChunkStepDeferTest {
    @Test void successfulReadinessResumesOnceOnExecutor() {
        var queue = new ArrayDeque<Runnable>();
        var ready = new CompletableFuture<Void>();
        var calls = new AtomicInteger();
        var chunk = ChunkStepDefer.resumeAfterPlates(ready, () -> true,
                () -> CompletableFuture.completedFuture(calls.incrementAndGet()), queue::add);
        assertTrue(queue.isEmpty());
        ready.complete(null);
        assertEquals(1, queue.size());
        assertFalse(chunk.isDone());
        queue.remove().run();
        assertEquals(1, chunk.join());
    }

    @Test void failedPlateCannotResumeChunkGeneration() {
        var failure = new IllegalStateException("板块构建失败");
        var chunk = ChunkStepDefer.resumeAfterPlates(CompletableFuture.failedFuture(failure), () -> true,
                () -> fail("失败的河网不能继续区块生成"), Runnable::run);
        assertSame(failure, assertThrows(CompletionException.class, chunk::join).getCause());
    }

    @Test void closeAfterReadyButBeforeCallbackStopsTheOldChunk() {
        var queue = new ArrayDeque<Runnable>();
        var open = new AtomicBoolean(true);
        var chunk = ChunkStepDefer.resumeAfterPlates(CompletableFuture.completedFuture(null), open::get,
                () -> fail("已经退出的世界不能再次进入区块生成"), queue::add);
        assertEquals(1, queue.size());
        open.set(false);
        queue.remove().run();
        assertInstanceOf(CancellationException.class, assertThrows(CompletionException.class, chunk::join).getCause());
    }
}
