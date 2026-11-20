package dev.streamsreflowing.compat.streamscontrol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** 单世界的在途去重与有界退避；失败不再依赖玩家移动才能重新取得调度机会。 */
final class RuntimePrewarmQueue<K> {
    enum Reason { FAILED, WITHDRAWN, REJECTED }
    interface Access<K> {
        boolean permit();
        boolean ready(K key);
        void build(K key);
        void retry(K key, Reason reason, int attempt, long delayNanos, Throwable failure);
    }
    private record Retry(int attempt, long after) {}
    private record Job(int attempt) {}
    private final Map<K, Job> pending = new LinkedHashMap<>();
    private final Map<K, Retry> retries = new LinkedHashMap<>();
    private final int capacity;
    private final LongSupplier clock;
    private boolean closed;

    RuntimePrewarmQueue(int capacity, LongSupplier clock) {
        if (capacity <= 0) throw new IllegalArgumentException("重试容量必须为正数");
        this.capacity = capacity;
        this.clock = clock;
    }

    int submit(K key, Executor executor, Access<K> access) {
        Job job;
        synchronized (this) {
            Retry retry = retries.get(key);
            if (closed || pending.containsKey(key)
                    || retry != null && clock.getAsLong() - retry.after() < 0) return 0;
            job = new Job(retry == null ? 1 : Math.min(64, retry.attempt() + 1));
            pending.put(key, job);
        }
        try {
            executor.execute(() -> run(key, job, access));
            return 1;
        } catch (RejectedExecutionException failure) {
            complete(key, job, access, Reason.REJECTED, failure);
            return -1;
        }
    }

    private void run(K key, Job job, Access<K> access) {
        Reason reason = Reason.WITHDRAWN;
        Throwable failure = null;
        try {
            synchronized (this) { if (closed) return; }
            if (!access.permit()) return;
            synchronized (this) { if (closed) return; }
            reason = Reason.FAILED;
            if (!access.ready(key)) access.build(key);
            reason = null;
        } catch (RuntimeException | LinkageError problem) {
            reason = Reason.FAILED;
            failure = problem;
        } finally {
            complete(key, job, access, reason, failure);
        }
    }

    private void complete(K key, Job job, Access<K> access, Reason reason, Throwable failure) {
        long delay;
        synchronized (this) {
            // 关闭或替换后的迟到任务不能重新写入本世界队列。
            if (!pending.remove(key, job) || closed) return;
            if (reason == null) { retries.remove(key); return; }
            delay = TimeUnit.SECONDS.toNanos(Math.min(60, 1L << Math.min(6, job.attempt() - 1)));
            retries.remove(key);
            retries.put(key, new Retry(job.attempt(), clock.getAsLong() + delay));
            while (retries.size() > capacity) retries.remove(retries.keySet().iterator().next());
        }
        // 端口会使扫描中心失效；即使容量淘汰了旧重试，下一轮仍能重新检查它。
        access.retry(key, reason, job.attempt(), delay, failure);
    }

    synchronized boolean retryDue(K key) {
        Retry retry = retries.get(key);
        return !closed && retry != null && clock.getAsLong() - retry.after() >= 0;
    }

    synchronized void retainCoverage(Set<K> coverage) {
        retries.keySet().retainAll(coverage);
    }

    synchronized int pendingCount() { return pending.size(); }
    synchronized int retryCount() { return retries.size(); }
    synchronized void close() { closed = true; pending.clear(); retries.clear(); }
}
