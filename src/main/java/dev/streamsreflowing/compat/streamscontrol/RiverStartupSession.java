package dev.streamsreflowing.compat.streamscontrol;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/** 每轮拥有启动任务和模式租约；停止只撤销自己的工作，不能清理下个世界的进度。 */
final class RiverStartupSession {
    private final Runnable acquireMode;
    private final Runnable releaseMode;
    private final Set<CompletableFuture<Void>> tasks = new HashSet<>();
    private boolean entered;
    private boolean closed;
    private boolean finished;
    private int completed;

    RiverStartupSession(Runnable acquireMode, Runnable releaseMode) {
        this.acquireMode = acquireMode;
        this.releaseMode = releaseMode;
    }

    synchronized void enter() {
        if (closed || finished) throw new IllegalStateException("河流预热会话已经结束");
        if (entered) return;
        acquireMode.run();
        entered = true;
    }

    synchronized void beginCache(int total, int workers) {
        if (!closed && !finished) StreamsStartupProgress.beginCache(this, total, workers);
    }

    synchronized void cacheChecked() {
        if (!closed && !finished) StreamsStartupProgress.cacheChecked(this);
    }

    /** 失败不计入成功板块，否则最后一个失败也会显示为全部完成。 */
    synchronized int completedBuild(boolean success) {
        if (closed || finished) return -1;
        if (!success) {
            StreamsStartupProgress.buildFailed(this);
            return -1;
        }
        return completedOne();
    }

    synchronized void progress(int total, int ready, int workers) {
        if (closed || finished) return;
        completed = ready;
        StreamsStartupProgress.begin(this, total, ready, workers);
    }

    synchronized int completedOne() {
        if (closed || finished) return -1;
        StreamsStartupProgress.setCompleted(this, ++completed);
        return completed;
    }

    synchronized CompletableFuture<Void> submit(Executor executor, Runnable body) {
        if (closed || finished) throw new RejectedExecutionException("河流预热会话已经关闭");
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // 已在执行的引擎构建可被别处共享；仅阻止尚未开始的任务，不打断共享构建。
            synchronized (this) {
                if (closed || finished) return;
            }
            body.run();
        }, executor);
        tasks.add(future);
        future.whenComplete((ignored, failure) -> {
            synchronized (this) { tasks.remove(future); }
        });
        return future;
    }

    synchronized void finish(boolean success) {
        if (closed || finished) return;
        finished = true;
        boolean released = false;
        try {
            release();
            released = true;
        } finally {
            // 释放失败也属于失败，不能把清理异常显示成完成。
            if (success && released) StreamsStartupProgress.finish(this);
            else StreamsStartupProgress.fail(this);
        }
    }

    /** 初始化异常同样撤销未开始任务，但留下失败卡片而不是悄悄消失。 */
    synchronized void failAndClose() {
        close(true);
    }

    synchronized void close() { close(false); }

    private void close(boolean failed) {
        // 初始化失败保留卡片，但世界停止时仍须按所有者清理它。
        if (closed) {
            if (!failed) StreamsStartupProgress.clear(this);
            return;
        }
        closed = true;
        // 先置关闭再撤销Future；同步触发的allOf回调不能再次释放本会话租约。
        for (var task : Set.copyOf(tasks)) task.cancel(false);
        tasks.clear();
        try {
            release();
        } finally {
            if (failed) StreamsStartupProgress.fail(this);
            else StreamsStartupProgress.clear(this);
        }
    }

    private void release() {
        if (!entered) return;
        entered = false;
        releaseMode.run();
    }
}
