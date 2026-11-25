package dev.streamsreflowing.core;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.streamscontrol.StreamsSamplerModeController;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/** 两层板块缓存：连接网可以读取基础板，基础板不会反过来等待连接网。 */
final class RiverPlateNetworks implements AutoCloseable {
    private final PlateRiverCache base = new PlateRiverCache(StreamsSamplerModeController::desiredPlateCacheCapacity);
    private final PlateRiverCache connected = new PlateRiverCache(StreamsSamplerModeController::desiredPlateCacheCapacity);
    private final boolean connect;
    private final Function<PlateKey, RiverNetwork> buildBase;
    private final Function<PlateKey, RiverNetwork> buildConnected;
    private final Set<Memo> memos = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Memo> local = ThreadLocal.withInitial(() -> {
        Memo memo = new Memo();
        memos.add(memo);
        return memo;
    });
    private volatile NetworkStore store;
    private volatile boolean closed;

    /** 整组引用在关服时摘除，避免并发地清掉命中的 value、却留下 key。 */
    private static final class Memo {
        volatile Object[] slots = new Object[18];
    }

    RiverPlateNetworks(boolean connect, Function<PlateKey, RiverNetwork> buildBase,
                       Function<PlateKey, RiverNetwork> buildConnected) {
        this.connect = connect;
        this.buildBase = buildBase;
        this.buildConnected = buildConnected;
    }

    void setStore(NetworkStore store) {
        this.store = store;
    }

    RiverNetwork baseFor(PlateKey key) {
        Object[] slots = openSlots();
        if (key.equals(slots[16])) return (RiverNetwork) slots[17];
        RiverNetwork network = base.get(key, () -> loadOrBuildBase(key), () -> buildBase.apply(key));
        slots[16] = key;
        slots[17] = network;
        return network;
    }

    RiverNetwork networkFor(PlateKey key) {
        if (!connect) return baseFor(key);
        Object[] slots = openSlots();
        int index = (key.hashCode() * 0x9E3779B1 >>> 29) << 1;
        if (key.equals(slots[index])) return (RiverNetwork) slots[index + 1];
        Supplier<RiverNetwork> builder = () -> buildConnected.apply(key);
        RiverNetwork network = connected.get(key, builder, builder);
        slots[index] = key;
        slots[index + 1] = network;
        return network;
    }

    /** 异步认领后直接构建；再调 networkFor 会等待自己的认领。 */
    CompletableFuture<Void> request(PlateKey key, Executor executor) {
        if (closed) return CompletableFuture.failedFuture(closedFailure());
        return activeCache().request(key, executor, () -> connect ? buildConnected.apply(key) : loadOrBuildBase(key));
    }

    private RiverNetwork loadOrBuildBase(PlateKey key) {
        NetworkStore disk = store;
        RiverNetwork stored = disk == null ? null : disk.load(key);
        if (stored != null) return stored;
        RiverNetwork network = buildBase.apply(key);
        if (disk != null && !closed) disk.save(key, network);
        return network;
    }

    boolean contains(PlateKey key) { return activeCache().contains(key); }
    /** 复制不可变河网引用后再采样，避免检查就绪后被LRU淘汰导致隐式重建。 */
    RiverNetwork peek(PlateKey key) { return closed?null:activeCache().peek(key); }
    RiverNetwork peekBase(PlateKey key) { return closed?null:base.peek(key); }
    boolean isReady(PlateKey key) { return activeCache().isReady(key); }
    int cachedBaseCount() { return base.size(); }
    boolean isClosed() { return closed; }

    private PlateRiverCache activeCache() { return connect ? connected : base; }

    int cacheState(PlateKey key) {
        if (isReady(key)) return RiverEngine.PLATE_READY;
        NetworkStore disk = store;
        if (closed || connect || disk == null || base.contains(key)) return RiverEngine.PLATE_MISSING;
        RiverNetwork restored;
        try {
            restored = disk.load(key);
        } catch (RuntimeException failure) {
            Constants.LOG.warn("[SR-PLATE-RESTORE] 河流板块 {} 读取失败，将重新构建", key, failure);
            return RiverEngine.PLATE_MISSING;
        }
        if (restored == null) return RiverEngine.PLATE_MISSING;
        return base.restoreIfIdle(key, restored) ? RiverEngine.PLATE_RESTORED : RiverEngine.PLATE_MISSING;
    }

    private Object[] openSlots() {
        if (closed) throw closedFailure();
        Memo memo = local.get();
        Object[] slots = memo.slots;
        if (closed) {
            // 允许线程恰好在关闭后完成 ThreadLocal 初始化，但不能再保留它的表。
            memo.slots = null;
            memos.remove(memo);
            throw closedFailure();
        }
        return slots;
    }

    @Override public void close() {
        closed = true;
        connected.close();
        base.close();
        for (Memo memo : memos) memo.slots = null;
        memos.clear();
    }

    private static CancellationException closedFailure() {
        return new CancellationException("河流世界已关闭");
    }
}
