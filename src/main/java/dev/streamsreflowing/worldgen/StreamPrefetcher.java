/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.streamscontrol.StreamsStartupProgress;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class StreamPrefetcher {
    private static final int a = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 2, Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
    private static final ThreadPoolExecutor b = new ThreadPoolExecutor(a, a, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(), runnable -> {
        Thread thread = new Thread(runnable, "streamsreflowing-prefetch");
        thread.setDaemon(true);
        thread.setPriority(1);
        return thread;
    });
    private static final int c = 2;
    private static final ThreadPoolExecutor d = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(), runnable -> {
        Thread thread = new Thread(runnable, "streamsreflowing-urgent");
        thread.setDaemon(true);
        thread.setPriority(5);
        return thread;
    });
    private static final AtomicLong e = new AtomicLong();
    private static final Object DEMAND_GATE = new Object();
    private static final AtomicInteger f = new AtomicInteger();
    private static final int g = 3;
    private static final Semaphore h = new Semaphore(3);
    private static final boolean i = !Boolean.getBoolean("streamsreflowing.nourgentlane");
    private static final int j = 60;
    private static final boolean k = Boolean.getBoolean("streamsreflowing.nojoingrace");
    private static final int l = -1;
    private static final int m = 0;
    private static final int n = 1;
    private static final int o = 2;
    private static final int p = 256;
    private static final Set<Long> q = ConcurrentHashMap.newKeySet();
    private static final Set<Long> r = ConcurrentHashMap.newKeySet();
    private static final Set<Long> s = ConcurrentHashMap.newKeySet();
    private static final Set<Long> t = ConcurrentHashMap.newKeySet();
    private static final int u = 128;
    private static final ConcurrentHashMap<Long, Long> v = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Long, Runnable> w = new ConcurrentHashMap();
    private static final int x = 12;
    private static final int y = 8;
    private static final double z = 25.0;
    // 0.7.118(streams)：原来是 HashMap + 非 volatile 的 long/int。
    // A 被四处并发改写（isEmpty / keySet().retainAll / clear / computeIfAbsent），
    // 分别发生在 server tick、prefetch 池与 urgent 线程上 ⇒ HashMap 并发写会丢更新甚至死循环。
    // H / I 是跨线程读写的计数与时间戳，非 volatile 意味着读者可能长期看不到新值
    // （:155 的 ++I 节奏判断、:279 的 H 计时都会因此失真）。
    // 语义不变：只是把容器换成并发安全的、把两个标量补上可见性。
    private static final Map<UUID, ArrayDeque<double[]>> A = new ConcurrentHashMap<UUID, ArrayDeque<double[]>>();
    private static final int B = 400;
    private static final int C = 160;
    private static final double D = 24.0;
    private static final double E = 1600.0;
    private static final double F = 1.25;
    private static final double G = 1.6;
    private static volatile long H;
    private static volatile int I;
    private static final boolean J;
    private static final double K = 42.0;
    private static volatile boolean L;
    private static final int M = 480;
    private static final int N = 3;
    private static final int O = 10;
    private static final double P = 0.5;
    // 预热期是否允许拦掉 prefetch 建板。-Dstreamsreflowing.prewarmPrefetch=false 回退到现行为。
    // prefetch 线程优先级是 1、预热 owner 是 0；低优先级 prefetch 一旦先 putIfAbsent 成为 builder，
    // 高优先级 owner 会在 PlateRiverCache.join() 上等它（真机实测一块板 233s）。预热期玩家还没进
    // 世界，prefetch 停摆无感知代价；owner 建板是独立调用路径（RiverEngine.networkFor -> 同一个
    // PlateRiverCache.get，谁先 putIfAbsent 谁建），所以跳过不会让任何板永远缺失。
    private static final boolean PREWARM_PREFETCH_ALLOWED =
            Boolean.parseBoolean(System.getProperty("streamsreflowing.prewarmPrefetch", "true"));
    // laneDiag 定时输出开关；-Dstreamsreflowing.laneDiag=true 时每 20s 打一行，观察 urgentWaiters。
    private static final boolean LANE_DIAG = Boolean.getBoolean("streamsreflowing.laneDiag");
    private static volatile long lastLaneDiagMs;

    private static void a() {
        int n = b.getMaximumPoolSize();
        if (n < Math.min(a, 4) && b.getActiveCount() >= n) {
            int n2 = n + 1;
            b.setMaximumPoolSize(n2);
            b.setCorePoolSize(n2);
        }
    }

    private StreamPrefetcher() {
    }

    /*
     * WARNING - void declaration
     */
    public static void onServerTick(MinecraftServer minecraftServer) {
        Iterator<Runnable> object;
        boolean bl;
        FlowTrace.maybeReport();
        if (LANE_DIAG) {
            long now = System.currentTimeMillis();
            if (now - lastLaneDiagMs >= 20000L) {
                lastLaneDiagMs = now;
                Constants.LOG.info("[SR-LANE] {}", laneDiag());
            }
        }
        if ((++I & 0x3FF) == 0) {
            RawFlowTileStore.flush();
        }
        if (J) {
            return;
        }
        if (!Services.PLATFORM.generateRivers()) {
            if (Services.PLATFORM.flowVanillaRivers()) {
                for (ServerLevel serverLevel : minecraftServer.getAllLevels()) {
                    if (!serverLevel.dimension().equals(Level.OVERWORLD) || serverLevel.players().isEmpty()) continue;
                    StreamPrefetcher.a(serverLevel);
                }
            }
            return;
        }
        boolean bl2 = bl = !w.isEmpty();
        if (bl && (I & 3) == 0) {
            int n = 12;
            object = w.values().iterator();
            ArrayList<Runnable> object2 = new ArrayList<Runnable>(n);
            while (object.hasNext() && object2.size() < n) {
                object2.add((Runnable)object.next());
                object.remove();
            }
            for (Runnable runnable : object2) {
                runnable.run();
            }
        }
        if ((I & 0x1F) != 0) {
            return;
        }
        if (!A.isEmpty()) {
            HashSet<UUID> hashSet = new HashSet<UUID>();
            for (ServerPlayer serverPlayer : minecraftServer.getPlayerList().getPlayers()) {
                hashSet.add(serverPlayer.getUUID());
            }
            A.keySet().retainAll(hashSet);
        }
        ArrayList<int[]> arrayList = new ArrayList<int[]>();
        for (ServerLevel n4 : minecraftServer.getAllLevels()) {
            if (!n4.dimension().equals(Level.OVERWORLD)) continue;
            for (ServerPlayer serverPlayer : n4.players()) {
                arrayList.add(new int[]{serverPlayer.blockPosition().getX(), serverPlayer.blockPosition().getZ()});
            }
        }
        int[] focusPoints = new int[arrayList.size() * 2];
        boolean bl3 = false;
        for (int focusIndex = 0; focusIndex < arrayList.size(); ++focusIndex) {
            focusPoints[focusIndex * 2] = arrayList.get(focusIndex)[0];
            focusPoints[focusIndex * 2 + 1] = arrayList.get(focusIndex)[1];
        }
        LowResGrid.setFocusPoints(focusPoints);
        int n = 0;
        boolean bl32 = false;
        if (!k) {
            block7: for (ServerLevel serverLevel : minecraftServer.getAllLevels()) {
                if (!serverLevel.dimension().equals(Level.OVERWORLD)) continue;
                for (ServerPlayer serverPlayer : serverLevel.players()) {
                    if (serverPlayer.tickCount >= 60) continue;
                    bl32 = true;
                    continue block7;
                }
            }
        }
        for (ServerLevel serverLevel : minecraftServer.getAllLevels()) {
            Object object2;
            if (!serverLevel.dimension().equals(Level.OVERWORLD)) continue;
            RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
            if (serverLevel.players().isEmpty()) continue;
            int n2 = (int)Math.max(256.0, riverEngine.config().plateScale());
            int n3 = Math.min(n2, Math.max(160, minecraftServer.getPlayerList().getViewDistance() * 16));
            HashMap<PlateKey, Double> hashMap = new HashMap<PlateKey, Double>();
            for (ServerPlayer serverPlayer : serverLevel.players()) {
                StreamPrefetcher.a(riverEngine, serverPlayer.blockPosition(), n3);
                if (bl32) continue;
                object2 = StreamPrefetcher.a(serverPlayer);
                StreamPrefetcher.a(riverEngine, serverPlayer.blockPosition(), (double[])object2, n2, hashMap);
                if (n >= 2) continue;
                n = Math.max(n, StreamPrefetcher.a(riverEngine, serverPlayer, n2));
            }
            if (bl32) {
                StreamPrefetcher.a(serverLevel);
                StreamPrefetcher.a(serverLevel, riverEngine, minecraftServer);
                continue;
            }
            StreamPrefetcher.d();
            ArrayList arrayList2 = new ArrayList(hashMap.entrySet());
            arrayList2.sort(Map.Entry.comparingByValue());
            Iterator iterator = arrayList2.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                StreamPrefetcher.a(riverEngine, (PlateKey)entry.getKey(), 0);
            }
            ServerLevel serverLevel2 = minecraftServer.getLevel(Level.NETHER);
            if (serverLevel2 != null && !serverLevel2.players().isEmpty()) {
                double d = serverLevel2.dimensionType().coordinateScale();
                for (ServerPlayer serverPlayer : serverLevel2.players()) {
                    double d2 = serverPlayer.getX() * d;
                    double d3 = serverPlayer.getZ() * d;
                    for (PlateKey plateKey : riverEngine.platesNear(d2, d3, 0.0)) {
                        StreamPrefetcher.a(riverEngine, plateKey, 0);
                    }
                }
            }
            StreamPrefetcher.a(riverEngine, serverLevel);
            StreamPrefetcher.a(serverLevel);
            StreamPrefetcher.a(serverLevel, riverEngine, minecraftServer);
        }
        if ((n = Math.max(0, n - StreamPrefetcher.b())) >= 1 && n <= 2 && StreamPrefetcher.a(minecraftServer) > 42.0) {
            --n;
        }
        int n4 = w.size();
        if (!bl32 && (bl || n4 > 0)) {
            n = Math.max(n, 2);
        }
        StreamPrefetcher.a(n);
    }

    private static double a(MinecraftServer minecraftServer) {
        return (double)minecraftServer.getAverageTickTimeNanos() / 1000000.0;
    }

    private static int b() {
        long l = System.nanoTime();
        long l2 = H;
        H = l;
        if (l2 == 0L) {
            return 0;
        }
        double d = (double)(l - l2) / 1000000.0;
        if (d >= 2560.0) {
            return 2;
        }
        return d >= 2000.0 ? 1 : 0;
    }

    private static int a(RiverEngine riverEngine, ServerPlayer serverPlayer, int n) {
        int n2;
        int n3 = serverPlayer.getBlockX();
        if (!riverEngine.isPlateReady(riverEngine.plateAt(n3, n2 = serverPlayer.getBlockZ())) || StreamPrefetcher.a(riverEngine, n3, n2, 16)) {
            return 4;
        }
        if (StreamPrefetcher.a(riverEngine, n3, n2, n / 3)) {
            return 3;
        }
        if (StreamPrefetcher.a(riverEngine, n3, n2, n * 2 / 3)) {
            return 2;
        }
        return StreamPrefetcher.a(riverEngine, n3, n2, n * 5 / 3) ? 1 : 0;
    }

    private static boolean a(RiverEngine riverEngine, int n, int n2, int n3) {
        for (int i = 0; i < 16; ++i) {
            int n4;
            double d = (double)i * 0.39269908169872414;
            int n5 = n + (int)Math.round(Math.cos(d) * (double)n3);
            if (riverEngine.isPlateReady(riverEngine.plateAt(n5, n4 = n2 + (int)Math.round(Math.sin(d) * (double)n3)))) continue;
            return true;
        }
        return false;
    }

    private static void a(int n) {
        int n2;
        // 预热期让路：启动预热期间（StreamsStartupProgress.begin(...) 到 finish()/clear() 之间）
        // 把 prefetch 当作最低活动等级 0 处理，池宽落到 1，CPU 全让给预热。finish() 会把
        // ACTIVE 置 false，下一次 tick 自然回到下面原有的分级逻辑，不需要单独恢复路径；
        // 停机路径 onServerStopping() 仍会显式 setBackgroundThrottle(4)。
        //
        // 只压 prefetch 池：方法末尾透传给 LowResGrid.setBackgroundThrottle 的仍是原始活动
        // 等级 n3。sampler 池正是预热自己的地形采样依赖（prewarm build -> RiverEngine
        // .networkFor -> NetworkTracer -> LowResGrid.parallelFor），预热期把它压到 1 会让
        // band 退化成 n3 <= 1 的内联串行分支，反而拖慢预热，与让路目的相反。
        int n3 = n;
        boolean bl = StreamsStartupProgress.isActive();
        if (bl) {
            n = 0;
        }
        if (n >= 4) {
            n2 = a;
        } else if (n == 3) {
            n2 = Math.max(1, a * 3 / 4);
        } else if (n == 2) {
            n2 = Math.max(1, a / 3);
        } else {
            n2 = n == 1 ? Math.max(1, a / 5) : 1;
        }
        if (i) {
            n2 = Math.max(n2, Math.min(3, f.get()));
        }
        if (bl) {
            // 上面紧急通道的下限可能把 n2 抬到 2~3；预热期一律压平到最小宽度 1。
            n2 = 1;
        }
        if (b.getCorePoolSize() != n2 || b.getMaximumPoolSize() != n2) {
            if (n2 >= b.getMaximumPoolSize()) {
                b.setMaximumPoolSize(n2);
                b.setCorePoolSize(n2);
            } else {
                b.setCorePoolSize(n2);
                b.setMaximumPoolSize(n2);
            }
        }
        LowResGrid.setBackgroundThrottle(n3);
    }

    public static void clearPending() {
        // 停服入口已关闭接单和板块 Future；撤下未运行的急件时也要归还计数。
        for (Runnable task : b.getQueue()) {
            if (i && task instanceof PrioritizedTask priority && priority.b == -1
                    && b.getQueue().remove(task)) f.decrementAndGet();
        }
        b.getQueue().clear();
        d.getQueue().clear();
        q.clear();
        r.clear();
        s.clear();
        t.clear();
        w.clear();
        v.clear();
        A.clear();
    }

    private static void a(ServerLevel serverLevel) {
        boolean bl;
        boolean bl2 = bl = Services.PLATFORM.vanillaWaterFlow() || !Services.PLATFORM.streamWaterState(0).is(Blocks.WATER);
        if (!Services.PLATFORM.flowVanillaRivers() || !bl) {
            return;
        }
        RiverFlowField.BiomeClass biomeClass = RiverFlowFields.classifierFor(serverLevel);
        RiverFlowField.OrientationField orientationField = RiverFlowFields.orientationFor(serverLevel);
        int n = RiverFlowFields.regionSize();
        for (ServerPlayer serverPlayer : serverLevel.players()) {
            int n2 = serverPlayer.getBlockX();
            int n3 = serverPlayer.getBlockZ();
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    StreamPrefetcher.a(biomeClass, orientationField, n2 + j * n, n3 + i * n);
                }
            }
        }
    }

    private static void a(RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField, int n, int n2) {
        long l = RiverFlowFields.owningRegionKey(n, n2);
        if (!s.add(l)) {
            return;
        }
        b.execute(new PrioritizedTask(1, e.getAndIncrement(), () -> {
            try {
                RiverFlowFields.ensureRegion(biomeClass, orientationField, n, n2);
            }
            catch (Throwable throwable) {
                Constants.LOG.warn("Prefetch of river-flow field near ({},{}) failed", n, n2, throwable);
            }
            finally {
                s.remove(l);
            }
        }));
    }

    private static void a(ServerLevel serverLevel, RiverEngine riverEngine, MinecraftServer minecraftServer) {
        if (!Services.PLATFORM.vanillaWaterFlow()) {
            return;
        }
        RiverFlowField.BiomeClass biomeClass = RiverFlowFields.classifierFor(serverLevel);
        RiverFlowField.OrientationField orientationField = RiverFlowFields.orientationFor(serverLevel);
        boolean bl = Services.PLATFORM.flowVanillaRivers();
        int n = minecraftServer.getPlayerList().getViewDistance() + 2;
        int n2 = 128;
        for (ServerPlayer serverPlayer : serverLevel.players()) {
            int n3 = serverPlayer.getBlockX() >> 4;
            int n4 = serverPlayer.getBlockZ() >> 4;
            for (int i = -n; i <= n && n2 > 0; ++i) {
                for (int j = -n; j <= n && n2 > 0; ++j) {
                    int n5 = n3 + j;
                    int n6 = n4 + i;
                    if (StreamFlowGrid.peek(n5, n6) != null) continue;
                    StreamPrefetcher.submitGridBuild(serverLevel, riverEngine, biomeClass, orientationField, bl, n5, n6, false, null);
                    --n2;
                }
            }
        }
    }

    public static void submitGridBuild(ServerLevel serverLevel, RiverEngine riverEngine, RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField, boolean bl, int n, int n2, boolean bl2, Runnable runnable) {
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        if (!bl2 && !t.add(l)) {
            return;
        }
        (bl2 ? d : b).execute(new PrioritizedTask(bl2 ? 0 : 2, e.getAndIncrement(), () -> {
            boolean bl3 = true;
            try {
                Long l2;
                if (StreamFlowGrid.peek(n, n2) == null || StreamFlowGrid.isIncomplete(n, n2)) {
                    if (!StreamPrefetcher.a(riverEngine, biomeClass, orientationField, bl, n, n2, bl2)) {
                        if (bl2) {
                            boolean bl4;
                            w.putIfAbsent(l, () -> StreamPrefetcher.submitGridBuild(serverLevel, riverEngine, biomeClass, orientationField, bl, n, n2, true, runnable));
                            boolean bl5 = bl4 = v.putIfAbsent(l, System.currentTimeMillis()) == null;
                            if (bl4) {
                                StreamFlowGrid.buildProvisional(serverLevel, riverEngine, biomeClass, orientationField, bl, n, n2);
                                if (runnable != null) {
                                    runnable.run();
                                }
                            }
                            bl3 = false;
                        }
                        return;
                    }
                    StreamFlowGrid.getOrBuild(serverLevel, riverEngine, biomeClass, orientationField, bl, n, n2);
                }
                if ((l2 = v.remove(l)) != null && FlowTrace.ON) {
                    FlowTrace.parkedDelivery(System.currentTimeMillis() - l2);
                }
            }
            catch (Throwable throwable) {
                Constants.LOG.warn("Flow-grid build at chunk ({},{}) failed", n, n2, throwable);
            }
            finally {
                if (!bl2) {
                    t.remove(l);
                }
                if (bl3 && runnable != null) {
                    runnable.run();
                }
            }
        }));
    }

    private static boolean a(RiverEngine riverEngine, RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField, boolean bl, int n, int n2, boolean bl2) {
        int n3;
        boolean bl3 = true;
        int n4 = (n << 4) + 8;
        int n5 = (n2 << 4) + 8;
        PlateKey[] plateKeyArray = riverEngine.platesNear(n4, n5, 25.0);
        int n6 = plateKeyArray.length;
        for (n3 = 0; n3 < n6; ++n3) {
            PlateKey plateKey = plateKeyArray[n3];
            if (riverEngine.isPlateCached(plateKey)) continue;
            if (!bl2) {
                return false;
            }
            FlowTrace.plateRequests.incrementAndGet();
            StreamPrefetcher.a(riverEngine, plateKey, 0);
            bl3 = false;
        }
        if (bl) {
            for (int i = -1; i <= 1; i += 2) {
                for (n6 = -1; n6 <= 1; n6 += 2) {
                    n3 = n4 + n6 * 17;
                    int n7 = n5 + i * 17;
                    if (RiverFlowFields.isRegionReady(n3, n7)) continue;
                    if (!bl2) {
                        return false;
                    }
                    FlowTrace.fieldRequests.incrementAndGet();
                    StreamPrefetcher.a(biomeClass, orientationField, n3, n7);
                    bl3 = false;
                }
            }
        }
        return bl3;
    }

    public static String plateDiag(RiverEngine riverEngine, PlateKey plateKey) {
        long l = (long)plateKey.cx() << 32 ^ (long)plateKey.cz() & 0xFFFFFFFFL;
        String string = "none";
        for (Runnable runnable : b.getQueue()) {
            if (!(runnable instanceof PrioritizedTask)) continue;
            PrioritizedTask prioritizedTask = (PrioritizedTask)runnable;
            if (prioritizedTask.d != l) continue;
            string = prioritizedTask.b == -1 ? "URGENT" : (prioritizedTask.b == 0 ? "ahead" : "passive");
            break;
        }
        return "cached=" + riverEngine.isPlateCached(plateKey) + " marker=" + q.contains(l) + " queued=" + string;
    }

    public static String laneDiag() {
        return "exec=" + b.getPoolSize() + "/" + b.getCorePoolSize() + " queue=" + b.getQueue().size() + " activeBuilds=" + RiverEngine.activeBuildCount() + " urgentWaiters=" + LowResGrid.urgentWaiterCount();
    }

    private static void a(RiverEngine engine, PlateKey key, int priority) {
        if (L || engine.isClosed() || engine.isPlateCached(key)) return;
        long packed = (long)key.cx() << 32 ^ (long)key.cz() & 0xFFFFFFFFL;
        if (!q.add(packed)) return;
        b.execute(new PrioritizedTask(priority, e.getAndIncrement(), packed,
                () -> prefetchPlate(engine, key, packed, priority)));
    }

    static CompletableFuture<Void> requestPlates(RiverEngine engine, PlateKey[] keys) {
        // 急件的去重和重试由板块缓存持有，不能被低优先级预取的队列标记挡住。
        return engine.whenPlatesReady(keys, StreamPrefetcher::enqueueDemand);
    }

    private static void enqueueDemand(Runnable build) {
        synchronized (DEMAND_GATE) {
            if (L) throw new CancellationException("河流已停止接收区块请求");
            if (i) f.incrementAndGet();
            try {
                b.execute(new PrioritizedTask(-1, e.getAndIncrement(), () -> {
                    if (i) h.acquireUninterruptibly();
                    try {
                        build.run();
                    } finally {
                        if (i) {
                            h.release();
                            f.decrementAndGet();
                        }
                    }
                }));
            } catch (RuntimeException rejected) {
                if (i) f.decrementAndGet();
                throw rejected;
            }
            if (i) StreamPrefetcher.a();
        }
    }

    private static boolean urgentQueued() {
        for (Runnable task : b.getQueue()) {
            if (task instanceof PrioritizedTask priority && priority.b == -1) return true;
        }
        return false;
    }

    private static void prefetchPlate(RiverEngine engine, PlateKey key, long packed, int priority) {
        boolean requeued = false;
        try {
            // 启动期低优先级预取不能抢占正式预热采样。
            if (L || engine.isClosed() || (PREWARM_PREFETCH_ALLOWED && StreamsStartupProgress.isActive())) return;
            long deadline = System.nanoTime() + 60_000_000_000L;
            while (LowResGrid.urgentWaiterCount() > 0 && RiverEngine.activeBuildCount() >= 4
                    && System.nanoTime() < deadline) {
                if (L || engine.isClosed() || engine.isPlateCached(key)) return;
                if (urgentQueued()) {
                    b.execute(new PrioritizedTask(priority, e.getAndIncrement(), packed,
                            () -> prefetchPlate(engine, key, packed, priority)));
                    requeued = true;
                    return;
                }
                Thread.sleep(250L);
            }
            engine.networkFor(key);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (CancellationException cancelled) {
            if (!engine.isClosed()) Constants.LOG.warn("[SR-PREFETCH] 河流板块 {} 预取取消", key, cancelled);
        } catch (Throwable failure) {
            Constants.LOG.warn("[SR-PREFETCH] 河流板块 {} 预取失败", key, failure);
        } finally {
            if (!requeued) q.remove(packed);
        }
    }

    private static void d() {
        for (Runnable runnable : b.getQueue()) {
            if (!(runnable instanceof PrioritizedTask)) continue;
            PrioritizedTask prioritizedTask = (PrioritizedTask)runnable;
            if (prioritizedTask.b != 0 || prioritizedTask.d == Long.MIN_VALUE || !b.getQueue().remove(prioritizedTask)) continue;
            q.remove(prioritizedTask.d);
        }
    }

    private static void a(RiverEngine riverEngine, ServerLevel serverLevel) {
        int n = Services.PLATFORM.prefetchRadius();
        for (ServerPlayer serverPlayer : serverLevel.players()) {
            BlockPos blockPos = serverPlayer.blockPosition();
            PlateKey plateKey = riverEngine.plateAt(blockPos.getX(), blockPos.getZ());
            for (int i = -n; i <= n; ++i) {
                for (int j = -n; j <= n; ++j) {
                    if (b.getQueue().size() >= 256) {
                        return;
                    }
                    StreamPrefetcher.a(riverEngine, new PlateKey(plateKey.cx() + j, plateKey.cz() + i), 2);
                }
            }
        }
    }

    private static void a(RiverEngine riverEngine, BlockPos blockPos, int n) {
        StreamPrefetcher.a(riverEngine, riverEngine.plateAt(blockPos.getX(), blockPos.getZ()));
        StreamPrefetcher.a(riverEngine, riverEngine.plateAt(blockPos.getX() + n, blockPos.getZ() + n));
        StreamPrefetcher.a(riverEngine, riverEngine.plateAt(blockPos.getX() + n, blockPos.getZ() - n));
        StreamPrefetcher.a(riverEngine, riverEngine.plateAt(blockPos.getX() - n, blockPos.getZ() + n));
        StreamPrefetcher.a(riverEngine, riverEngine.plateAt(blockPos.getX() - n, blockPos.getZ() - n));
    }

    public static void submitUrgentAt(ServerLevel serverLevel, int n, int n2) {
        try {
            if (serverLevel == null || !Services.PLATFORM.generateRivers() || !serverLevel.dimension().equals(Level.OVERWORLD)) {
                return;
            }
            RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
            int n3 = (int)Math.max(256.0, riverEngine.config().plateScale());
            int n4 = Math.min(n3, Math.max(160, serverLevel.getServer().getPlayerList().getViewDistance() * 16));
            StreamPrefetcher.a(riverEngine, new BlockPos(n, serverLevel.getSeaLevel(), n2), n4);
        }
        catch (Throwable throwable) {
            Constants.LOG.warn("Urgent prefetch for teleport destination ({},{}) failed (ignored)", n, n2, throwable);
        }
    }

    public static boolean isServerStopping() {
        return L;
    }

    public static void onServerStarting() {
        synchronized (DEMAND_GATE) { L = false; }
    }

    public static void onServerStopping() {
        // 这里只关接单。等待者必须由所属世界结案，再清队列。
        synchronized (DEMAND_GATE) { L = true; }
        LowResGrid.setBackgroundThrottle(4);
    }

    public static void warmWorldOrigin(RiverEngine riverEngine) {
        LowResGrid.setFocusPoints(new int[]{0, 0});
        PlateKey plateKey = riverEngine.plateAt(0, 0);
        StreamPrefetcher.a(riverEngine, plateKey);
        HashSet<PlateKey> hashSet = new HashSet<PlateKey>();
        for (int i = -480; i <= 480; i += 160) {
            for (int j = -480; j <= 480; j += 160) {
                PlateKey plateKey2 = riverEngine.plateAt(j, i);
                if (plateKey2.equals(plateKey)) continue;
                hashSet.add(plateKey2);
            }
        }
        for (PlateKey plateKey3 : hashSet) {
            StreamPrefetcher.a(riverEngine, plateKey3, 0);
        }
    }

    private static void a(RiverEngine riverEngine, PlateKey plateKey) {
        if (L || riverEngine.isClosed() || riverEngine.isPlateCached(plateKey)) {
            return;
        }
        long l = (long)plateKey.cx() << 32 ^ (long)plateKey.cz() & 0xFFFFFFFFL;
        if (!r.add(l)) {
            return;
        }
        d.execute(new PrioritizedTask(-1, e.getAndIncrement(), () -> {
            try {
                if (!riverEngine.isClosed()) riverEngine.networkFor(plateKey);
            } catch (CancellationException cancelled) {
                if (!riverEngine.isClosed()) Constants.LOG.warn("[SR-URGENT] 河流板块 {} 紧急预取取消", plateKey, cancelled);
            } catch (Throwable failure) {
                Constants.LOG.warn("[SR-URGENT] 河流板块 {} 紧急预取失败", plateKey, failure);
            }
            finally {
                r.remove(l);
            }
        }));
    }

    private static void a(RiverEngine riverEngine, BlockPos blockPos, double[] dArray, int n, Map<PlateKey, Double> map) {
        int n2 = Math.max(256, n / 2);
        int n3 = n;
        for (int i = -n3; i <= n3; i += n2) {
            for (int j = -n3; j <= n3; j += n2) {
                double d;
                double d2 = d = Math.hypot(j, i);
                if (d > 1.0E-6 && (dArray[0] != 0.0 || dArray[1] != 0.0)) {
                    double d3 = ((double)j * dArray[0] + (double)i * dArray[1]) / d;
                    d2 = d * (1.25 - 0.75 * d3);
                }
                map.merge(riverEngine.plateAt(blockPos.getX() + j, blockPos.getZ() + i), d2, Math::min);
            }
        }
    }

    private static double[] a(ServerPlayer serverPlayer) {
        double[] dArray;
        double d;
        ArrayDeque arrayDeque = A.computeIfAbsent(serverPlayer.getUUID(), uUID -> new ArrayDeque());
        arrayDeque.addLast(new double[]{I, serverPlayer.getX(), serverPlayer.getZ()});
        while (!arrayDeque.isEmpty() && (double)I - ((double[])arrayDeque.peekFirst())[0] > 400.0) {
            arrayDeque.removeFirst();
        }
        double[] dArray2 = (double[])arrayDeque.peekFirst();
        if (dArray2 == null || (double)I - dArray2[0] < 160.0) {
            return new double[]{0.0, 0.0};
        }
        double d2 = serverPlayer.getX() - dArray2[1];
        double d3 = Math.hypot(d2, d = serverPlayer.getZ() - dArray2[2]);
        if (d3 >= 24.0) {
            double[] dArray3 = new double[2];
            dArray3[0] = d2 / d3;
            dArray = dArray3;
            dArray3[1] = d / d3;
        } else {
            double[] dArray4 = new double[2];
            dArray4[0] = 0.0;
            dArray = dArray4;
            dArray4[1] = 0.0;
        }
        return dArray;
    }

    static {
        J = Boolean.getBoolean("streamsreflowing.noprefetch");
    }

    private static final class PrioritizedTask
    implements Comparable<PrioritizedTask>,
    Runnable {
        static final long a = Long.MIN_VALUE;
        private final int b;
        private final long c;
        private final long d;
        private final Runnable e;

        PrioritizedTask(int n, long l, Runnable runnable) {
            this(n, l, Long.MIN_VALUE, runnable);
        }

        PrioritizedTask(int n, long l, long l2, Runnable runnable) {
            this.b = n;
            this.c = l;
            this.d = l2;
            this.e = runnable;
        }

        @Override
        public void run() {
            this.e.run();
        }

        @Override
        public int compareTo(PrioritizedTask prioritizedTask) {
            int n = Integer.compare(this.b, prioritizedTask.b);
            return n != 0 ? n : Long.compare(this.c, prioritizedTask.c);
        }
    }
}

