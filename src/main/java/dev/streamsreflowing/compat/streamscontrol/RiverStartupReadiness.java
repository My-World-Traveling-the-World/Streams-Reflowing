package dev.streamsreflowing.compat.streamscontrol;

import java.util.concurrent.CompletableFuture;

/** 每个世界独立记录启动结果，避免“等待结束”被误认为“河流已经准备好”。 */
final class RiverStartupReadiness {
    enum Outcome { NOT_STARTED, COMPLETE, DISABLED, FAILED, STOPPED }

    private final CompletableFuture<Outcome> completion = new CompletableFuture<>();
    private boolean started;
    private boolean stopped;

    synchronized boolean begin() {
        // 两个生命周期入口共用同一轮；失败不能在第二个入口被悄悄覆盖。
        if (started || stopped) return false;
        started = true;
        return true;
    }

    void disabled() {
        // 用户关闭预热表示没有启动前置工作，并不宣称真的构建过板块。
        completion.complete(Outcome.DISABLED);
    }

    void failed() {
        completion.complete(Outcome.FAILED);
    }

    void completeWhen(CompletableFuture<Void> work) {
        // 清理回调也属于启动流程；其异常不能留下永远等不到的结果。
        work.whenComplete((ignored, failure) ->
                completion.complete(failure == null ? Outcome.COMPLETE : Outcome.FAILED));
    }

    Outcome await() {
        synchronized (this) {
            if (stopped) return Outcome.STOPPED;
            if (!started) return Outcome.NOT_STARTED;
        }
        return completion.join();
    }

    Outcome publishIfReady(Runnable publish) {
        Outcome outcome = await();
        synchronized (this) {
            // 与 stop 共用锁，保证停服重置 ready 后旧任务不能再迟到发布。
            if (stopped) return Outcome.STOPPED;
            if (outcome == Outcome.COMPLETE || outcome == Outcome.DISABLED) publish.run();
            return outcome;
        }
    }

    synchronized void stop() {
        stopped = true;
        // 只终止本世界的等待，不取消引擎共享的板块构建。
        completion.complete(Outcome.STOPPED);
    }
}
