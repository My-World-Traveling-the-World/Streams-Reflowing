package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** 一个世界内，同一板块的同步调用、预取和区块等待共用建板结果。 */
public final class PlateRiverCache implements AutoCloseable {
    private final ConcurrentHashMap<PlateKey, Entry> entries = new ConcurrentHashMap<>();
    private final ArrayDeque<PlateKey> completed = new ArrayDeque<>();
    private final Object gate = new Object();
    private final IntSupplier capacity;
    private volatile boolean closed;

    private static final class Entry {
        final CompletableFuture<RiverNetwork> result = new CompletableFuture<>();
        volatile boolean claimed;
        volatile RiverNetwork ready;
        boolean demanded;
    }

    public PlateRiverCache(IntSupplier capacity) {
        this.capacity = capacity;
    }

    public RiverNetwork get(PlateKey key, Supplier<RiverNetwork> build, Supplier<RiverNetwork> timeoutBuild) {
        if (closed) throw closedFailure();
        Entry entry = entries.get(key);
        if (entry != null && entry.claimed) return await(entry, timeoutBuild);
        boolean owner;
        synchronized (gate) {
            if (closed) throw closedFailure();
            entry = entries.computeIfAbsent(key, ignored -> new Entry());
            owner = !entry.claimed;
            entry.claimed = true;
        }
        return owner ? buildClaimed(key, entry, build) : await(entry, timeoutBuild);
    }

    /** 排队尚未开始时，允许启动预热直接接手，避免工作线程互等。 */
    public CompletableFuture<Void> request(PlateKey key, Executor executor, Supplier<RiverNetwork> build) {
        Entry entry;
        boolean submit;
        synchronized (gate) {
            if (closed) return CompletableFuture.failedFuture(closedFailure());
            entry = entries.computeIfAbsent(key, ignored -> new Entry());
            if (entry.ready != null) return CompletableFuture.completedFuture(null);
            submit = !entry.claimed && !entry.demanded;
            entry.demanded = true;
        }
        if (submit) {
            try {
                executor.execute(() -> runQueued(key, entry, build));
            } catch (RuntimeException rejected) {
                boolean discarded;
                synchronized (gate) {
                    discarded = !entry.claimed && entries.remove(key, entry);
                }
                if (discarded) entry.result.completeExceptionally(rejected);
            }
        }
        // 区块自行取消时不能取消其他区块共享的建板。
        return entry.result.thenApply(network -> null);
    }

    private void runQueued(PlateKey key, Entry entry, Supplier<RiverNetwork> build) {
        synchronized (gate) {
            if (closed || entry.claimed || entries.get(key) != entry) return;
            entry.claimed = true;
        }
        try {
            buildClaimed(key, entry, build);
        } catch (CancellationException ignored) {
            // 退出世界已经通知全部等待者，迟到的队项不再发布板块。
        } catch (RuntimeException failure) {
            Constants.LOG.warn("[SR-PLATE-FAILED] 河流板块 {} 构建失败，区块结束等待", key, failure);
        }
    }

    private RiverNetwork buildClaimed(PlateKey key, Entry entry, Supplier<RiverNetwork> build) {
        RiverNetwork network;
        for (int attempt = 0;; attempt++) {
            if (closed) throw closedFailure();
            try {
                network = Objects.requireNonNull(build.get(), "建板未返回河网");
                break;
            } catch (Throwable failure) {
                Throwable cause = failure instanceof CompletionException ? failure.getCause() : failure;
                boolean retry;
                synchronized (gate) {
                    // 重试期间保留认领，后来订阅的区块不能另起一轮重试。
                    retry = !closed && attempt == 0 && entry.demanded
                            && cause instanceof RuntimeException && !(cause instanceof CancellationException);
                    if (!retry) entries.remove(key, entry);
                }
                if (!retry) {
                    entry.result.completeExceptionally(failure);
                    throw failure;
                }
                Constants.LOG.debug("[SR-PLATE-RETRY] 河流板块 {} 构建失败，重试一次", key, failure);
            }
        }
        synchronized (gate) {
            if (closed) throw closedFailure();
            publish(key, entry, network);
        }
        // Future 的回调可能接着查询其他板块，所以在锁外通知。
        entry.result.complete(network);
        return network;
    }

    /** 磁盘检查只负责立即恢复，不应在“检查”和“安装”之间输给并发构建后转为同步等待。 */
    public boolean restoreIfIdle(PlateKey key, RiverNetwork network) {
        Entry entry;
        synchronized (gate) {
            if (closed) return false;
            entry = entries.computeIfAbsent(key, ignored -> new Entry());
            if (entry.claimed) return false;
            entry.claimed = true;
            publish(key, entry, network);
        }
        entry.result.complete(network);
        return true;
    }

    private void publish(PlateKey key, Entry entry, RiverNetwork network) {
        entry.ready = network;
        completed.addLast(key);
        // 缓存预算只针对已发布的河网；驱逐在途项会让同一板块被重复计算。
        int limit = capacity.getAsInt();
        while (completed.size() > limit) entries.remove(completed.removeFirst());
    }

    public boolean contains(PlateKey key) {
        Entry entry = entries.get(key);
        return entry != null && entry.claimed;
    }

    /** 远景只读已发布引用；未命中绝不认领、等待或访问磁盘。 */
    public RiverNetwork peek(PlateKey key) {
        Entry entry=entries.get(key);
        return closed||entry==null?null:entry.ready;
    }

    public boolean isReady(PlateKey key) {
        Entry entry = entries.get(key);
        return entry != null && entry.ready != null;
    }

    public int size() {
        synchronized (gate) {
            return completed.size();
        }
    }

    @Override
    public void close() {
        ArrayList<Entry> unfinished = new ArrayList<>();
        synchronized (gate) {
            if (closed) return;
            closed = true;
            for (Entry entry : entries.values()) if (entry.ready == null) unfinished.add(entry);
            entries.clear();
            completed.clear();
        }
        unfinished.forEach(entry -> entry.result.cancel(false));
    }

    private static CancellationException closedFailure() {
        return new CancellationException("河流板块缓存已关闭");
    }

    private static RiverNetwork await(Entry entry, Supplier<RiverNetwork> timeoutBuild) {
        RiverNetwork ready = entry.ready;
        if (ready != null) return ready;
        if (entry.result.isDone()) return entry.result.join();
        boolean counted = LowResGrid.noteUrgentWaiter();
        try {
            // 自有线程的依赖只从连接网指向独立基础板；外来线程保留原来的有界等待。
            if (internalWorker(Thread.currentThread().getName()) || timeoutBuild == null) return entry.result.join();
            return entry.result.get(300, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            return timeoutBuild.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return timeoutBuild.get();
        } catch (ExecutionException failed) {
            throw new CompletionException(failed.getCause());
        } finally {
            if (counted) LowResGrid.urgentWaiterDone();
        }
    }

    private static boolean internalWorker(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.startsWith("streamsreflowing-") || lower.startsWith("streams-startup-prewarm-")
                || lower.startsWith("streams-runtime-prewarm-");
    }
}
