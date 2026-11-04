/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.streamscontrol.StreamsStartupProgress;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

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
    private static final Map<UUID, ArrayDeque<double[]>> A = new HashMap<UUID, ArrayDeque<double[]>>();
    private static final int B = 400;
    private static final int C = 160;
    private static final double D = 24.0;
    private static final double E = 1600.0;
    private static final double F = 1.25;
    private static final double G = 1.6;
    private static long H;
    private static int I;
    private static final boolean J;
    private static final double K = 42.0;
    private static volatile boolean L;
    private static final int M = 480;
    private static final int N = 3;
    private static final int O = 10;
    private static final double P = 0.5;
    private static final int[] Q;
    private static final int[] R;
    private static final AtomicBoolean S;

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

    private static void a(RiverEngine riverEngine, PlateKey plateKey, int n) {
        if (L && n != -1) {
            return;
        }
        if (riverEngine.isPlateCached(plateKey)) {
            return;
        }
        long l = (long)plateKey.cx() << 32 ^ (long)plateKey.cz() & 0xFFFFFFFFL;
        if (!q.add(l)) {
            if (i && n == -1) {
                for (Runnable runnable : b.getQueue()) {
                    if (!(runnable instanceof PrioritizedTask)) continue;
                    PrioritizedTask prioritizedTask = (PrioritizedTask)runnable;
                    if (prioritizedTask.d != l || prioritizedTask.b == -1 || !b.getQueue().remove(prioritizedTask)) continue;
                    f.incrementAndGet();
                    b.execute(new PrioritizedTask(-1, e.getAndIncrement(), l, () -> StreamPrefetcher.a(riverEngine, plateKey, l, -1)));
                    StreamPrefetcher.a();
                    break;
                }
            }
            return;
        }
        if (i && n == -1) {
            f.incrementAndGet();
        }
        b.execute(new PrioritizedTask(n, e.getAndIncrement(), l, () -> StreamPrefetcher.a(riverEngine, plateKey, l, n)));
        if (i && n == -1) {
            StreamPrefetcher.a();
        }
    }

    public static void submitChunkDemand(RiverEngine riverEngine, PlateKey plateKey) {
        StreamPrefetcher.a(riverEngine, plateKey, -1);
    }

    private static boolean c() {
        for (Runnable runnable : b.getQueue()) {
            if (!(runnable instanceof PrioritizedTask)) continue;
            PrioritizedTask prioritizedTask = (PrioritizedTask)runnable;
            if (prioritizedTask.b != -1) continue;
            return true;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void a(RiverEngine riverEngine, PlateKey plateKey, long l, int n) {
        if (L && n != -1) {
            q.remove(l);
            return;
        }
        boolean bl = false;
        try {
            if (n != -1) {
                long l2 = System.nanoTime() + 60000000000L;
                while (LowResGrid.urgentWaiterCount() > 0 && RiverEngine.activeBuildCount() >= 4 && System.nanoTime() < l2) {
                    if (L) {
                        return;
                    }
                    if (riverEngine.isPlateCached(plateKey)) {
                        return;
                    }
                    if (StreamPrefetcher.c()) {
                        b.execute(new PrioritizedTask(n, e.getAndIncrement(), l, () -> StreamPrefetcher.a(riverEngine, plateKey, l, n)));
                        bl = true;
                        return;
                    }
                    Thread.sleep(250L);
                }
            }
            if (i && n == -1) {
                h.acquireUninterruptibly();
            }
            try {
                if (i && n == -1 && riverEngine.isPlateReady(plateKey)) {
                    return;
                }
                riverEngine.networkFor(plateKey);
            }
            finally {
                if (i && n == -1) {
                    h.release();
                }
            }
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        catch (Throwable throwable) {
            Constants.LOG.warn("Prefetch of river region {} failed", (Object)plateKey, (Object)throwable);
        }
        finally {
            if (!bl) {
                q.remove(l);
            }
            if (i && n == -1) {
                f.decrementAndGet();
            }
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

    public static void onServerStopping() {
        PrioritizedTask prioritizedTask;
        L = true;
        PlateRiverCache.releaseAllWaiters();
        for (Runnable runnable : b.getQueue()) {
            if (!(runnable instanceof PrioritizedTask)) continue;
            prioritizedTask = (PrioritizedTask)runnable;
            if (prioritizedTask.b == -1 || !b.getQueue().remove(prioritizedTask) || prioritizedTask.d == Long.MIN_VALUE) continue;
            q.remove(prioritizedTask.d);
        }
        for (Runnable runnable : d.getQueue()) {
            if (!(runnable instanceof PrioritizedTask)) continue;
            prioritizedTask = (PrioritizedTask)runnable;
            if (!d.getQueue().remove(prioritizedTask) || prioritizedTask.d == Long.MIN_VALUE) continue;
            r.remove(prioritizedTask.d);
        }
        StreamPrefetcher.clearPending();
        LowResGrid.setBackgroundThrottle(4);
    }

    public static void warmWorldOrigin(RiverEngine riverEngine) {
        L = false;
        PlateRiverCache.reopen();
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
        if (riverEngine.isPlateCached(plateKey)) {
            return;
        }
        long l = (long)plateKey.cx() << 32 ^ (long)plateKey.cz() & 0xFFFFFFFFL;
        if (!r.add(l)) {
            return;
        }
        d.execute(new PrioritizedTask(-1, e.getAndIncrement(), () -> {
            try {
                riverEngine.networkFor(plateKey);
            }
            catch (Throwable throwable) {
                Constants.LOG.warn("Urgent prefetch of river region {} failed", (Object)plateKey, (Object)throwable);
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

    public static void biomeBench(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        int n3 = (int)Math.round(riverEngine.config().plateScale());
        int n4 = 4 * n3;
        int n5 = 8;
        RiverFlowField.BiomeClass biomeClass = RiverFlowFields.classifierFor(serverLevel);
        Thread thread = new Thread(() -> {
            int n6 = n4 / 2;
            long l = 0L;
            for (int i = n2 - n6; i < n2 - n6 + n5 * 48; i += n5) {
                for (int j = n - n6; j < n - n6 + n5 * 48; j += n5) {
                    l += (long)biomeClass.classify(j, i);
                }
            }
            long l2 = System.nanoTime();
            long l3 = 0L;
            long l4 = 0L;
            for (int i = n2 - n6; i <= n2 + n6; i += n5) {
                for (int j = n - n6; j <= n + n6; j += n5) {
                    if (biomeClass.classify(j, i) == 2) {
                        ++l4;
                    }
                    ++l3;
                }
            }
            double d = (double)(System.nanoTime() - l2) / 1000000.0;
            double d2 = d * 1000.0 / (double)Math.max(1L, l3);
            Constants.LOG.info("[SR-BIOMEBENCH] 4x plateScale tile {}x{} (plateScale={}), spacing {}: {} getNoiseBiome samples in {} ms = {} us/call, {}% ocean [warm {}]", n4, n4, n3, n5, l3, String.format("%.0f", d), String.format("%.3f", d2), String.format("%.1f", 100.0 * (double)l4 / (double)Math.max(1L, l3)), l & 1L);
        }, "streamsreflowing-biomebench");
        thread.setDaemon(true);
        thread.start();
    }

    public static void diagnoseColumn(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        StreamPrefetcher.a(riverEngine, serverLevel, n, n2);
        StreamPrefetcher.a(riverEngine, serverLevel, new ChunkPos(new BlockPos(n, 0, n2)));
    }

    public static void autoTrace(MinecraftServer minecraftServer, int n, int n2) {
        int n3;
        String[] stringArray;
        ServerLevel serverLevel = minecraftServer.overworld();
        int n4 = n >> 4;
        int n5 = n2 >> 4;
        Constants.LOG.info("[SR-AUTOTRACE] target=({},{}) chunk=({},{}) -- force-generating a 5x5 chunk block (the precise plate region from the diagnostics); nothing else is loaded", n, n2, n4, n5);
        long l = FormSampler.keptIr.sum();
        long l2 = FormSampler.handedBack.sum();
        long l3 = System.nanoTime();
        int n6 = 0;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                serverLevel.getChunk(n4 + i, n5 + j);
                ++n6;
            }
        }
        long l4 = (System.nanoTime() - l3) / 1000000L;
        long l5 = FormSampler.keptIr.sum() - l;
        long l6 = FormSampler.handedBack.sum() - l2;
        Constants.LOG.info("[SR-AUTOTRACE] generated {} chunks in {} ms; dumping placed-column diagnostics", (Object)n6, (Object)l4);
        Constants.LOG.info("[SR-AUTOTRACE] height queries through the walk during that build: {} served, {} handed to the host, {} total{}", l5, l6, l5 + l6, l5 + l6 == 0L ? " (zero -- either the walk is off, or this build asks it for nothing)" : String.format(Locale.ROOT, "; %.1f us each if they owned the whole %d ms", (double)l4 * 1000.0 / (double)(l5 + l6), l4));
        RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
        for (int i = -24; i <= 24; i += 4) {
            StreamPrefetcher.diagnoseColumn(riverEngine, serverLevel, n, n2 + i);
        }
        StreamPrefetcher.traceColumn(riverEngine, serverLevel, n, n2);
        StreamPrefetcher.superTrace(riverEngine, serverLevel, n, n2);
        StreamPrefetcher.spoutScan(riverEngine, serverLevel, n, n2, 768);
        String string = System.getProperty("streamsreflowing.sweep");
        if (string != null && !string.isBlank()) {
            stringArray = string.split(";");
            int n7 = stringArray.length;
            for (n3 = 0; n3 < n7; ++n3) {
                String string2 = stringArray[n3];
                String[] stringArray2 = string2.split(",");
                try {
                    int n8 = Integer.parseInt(stringArray2[0].trim());
                    int n9 = Integer.parseInt(stringArray2[1].trim());
                    for (int i = -1; i <= 1; ++i) {
                        for (int j = -1; j <= 1; ++j) {
                            serverLevel.getChunk((n8 >> 4) + i, (n9 >> 4) + j);
                        }
                    }
                    StreamPrefetcher.diagnoseColumn(riverEngine, serverLevel, n8, n9);
                    StreamPrefetcher.traceColumn(riverEngine, serverLevel, n8, n9);
                    StreamPrefetcher.superTrace(riverEngine, serverLevel, n8, n9);
                    StreamPrefetcher.spoutScan(riverEngine, serverLevel, n8, n9, 768);
                    continue;
                }
                catch (RuntimeException runtimeException) {
                    Constants.LOG.warn("[SR-AUTOTRACE] bad sweep target '{}'", (Object)string2);
                }
            }
        }
        String string3 = System.getProperty("streamsreflowing.monoaudit");
        if (string3 != null && !string3.isBlank()) {
            String[] stringArray3 = string3.split(",");
            try {
                n3 = Integer.parseInt(stringArray3[0].trim());
                int n10 = Integer.parseInt(stringArray3[1].trim());
                int n11 = stringArray3.length >= 3 ? Integer.parseInt(stringArray3[2].trim()) : 96;
                StreamPrefetcher.monoAudit(riverEngine, serverLevel, n3, n10, n11);
            }
            catch (RuntimeException runtimeException) {
                Constants.LOG.warn("[SR-MONOAUDIT] bad streamsreflowing.monoaudit '{}'", (Object)string3, (Object)runtimeException);
            }
        }
        Constants.LOG.info("[SR-AUTOTRACE] diagnostics complete; halting server");
        minecraftServer.halt(false);
    }

    public static void monoAudit(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        int n11 = n3 * 2 + 1;
        int[] nArray = new int[n11 * n11];
        double[] dArray = new double[n11 * n11];
        double[] dArray2 = new double[n11 * n11];
        int[] nArray2 = new int[n11 * n11];
        Arrays.fill(nArray, Integer.MIN_VALUE);
        Arrays.fill(nArray2, Integer.MIN_VALUE);
        PlateKey plateKey = riverEngine.plateAt(n, n2);
        int n12 = 32;
        for (n10 = -n3; n10 <= n3; ++n10) {
            for (int i = -n3; i <= n3; ++i) {
                int n13 = n + i;
                n9 = n2 + n10;
                if (!riverEngine.plateAt(n13, n9).equals(plateKey) || !riverEngine.plateAt(n13 - n12, n9).equals(plateKey) || !riverEngine.plateAt(n13 + n12, n9).equals(plateKey) || !riverEngine.plateAt(n13, n9 - n12).equals(plateKey) || !riverEngine.plateAt(n13, n9 + n12).equals(plateKey)) continue;
                n8 = (n10 + n3) * n11 + (i + n3);
                ColumnPlan columnPlan = riverEngine.plan(n13, n9);
                n6 = n7 = chunkGenerator.getBaseHeight(n13, n9, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                n5 = Integer.MIN_VALUE;
                if (columnPlan.inCorridor()) {
                    double d;
                    int n14;
                    n5 = columnPlan.waterSurfaceY();
                    if (columnPlan.distToOutlet() <= 24.0 && (n4 = RiverCarverFeature.nearbyLakeLevel(riverEngine, n13, n9, serverLevel.getMinBuildHeight(), serverLevel.getMaxBuildHeight())) != Integer.MIN_VALUE) {
                        n5 = Math.max(n5, Math.min(n4, columnPlan.waterSurfaceY() + 4));
                    }
                    if ((n6 = Math.min(n7, n4 = columnPlan.waterSurfaceY() + (int)Math.round(columnPlan.bankBlend() * (double)Math.max(0, n7 - columnPlan.waterSurfaceY())))) < n7 && riverEngine.lakeAt(n13, n9) == null && columnPlan.distToOutlet() > 24.0 && (n14 = RiverCarverFeature.adjacentCoveringLakeLevel(riverEngine, n13, n9)) != Integer.MIN_VALUE && n7 > n14 && n6 <= n14) {
                        n6 = Math.min(n7, n14 + 1);
                    }
                    if ((d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ())) > 1.0E-6) {
                        dArray[n8] = columnPlan.flowDirX() / d;
                        dArray2[n8] = columnPlan.flowDirZ() / d;
                    }
                }
                if ((n4 = riverEngine.lakeSurfaceAt(n13, n9)) != Integer.MIN_VALUE && n4 > n5) {
                    n5 = n4;
                    dArray[n8] = 0.0;
                    dArray2[n8] = 0.0;
                }
                nArray[n8] = n5;
                nArray2[n8] = n6;
            }
        }
        n10 = 0;
        int[] nArray3 = new int[33];
        StringBuilder stringBuilder = new StringBuilder();
        n9 = 0;
        for (n8 = -n3; n8 <= n3; ++n8) {
            for (int i = -n3; i <= n3; ++i) {
                int n15;
                n7 = (n8 + n3) * n11 + (i + n3);
                n6 = nArray[n7];
                if (n6 == Integer.MIN_VALUE) continue;
                n5 = 0;
                n4 = 0;
                int n16 = 0;
                int n17 = 0;
                block4: for (n15 = 0; n15 < 8; ++n15) {
                    int n18 = Q[n15];
                    int n19 = R[n15];
                    double d = Math.sqrt(n18 * n18 + n19 * n19);
                    if (dArray[n7] * (double)n18 / d + dArray2[n7] * (double)n19 / d >= 0.5) continue;
                    int n20 = Integer.MIN_VALUE;
                    for (int j = 1; j <= 10; ++j) {
                        int n21 = i + n18 * j;
                        int n22 = n8 + n19 * j;
                        if (n21 < -n3 || n21 > n3 || n22 < -n3 || n22 > n3) continue block4;
                        int n23 = (n22 + n3) * n11 + (n21 + n3);
                        int n24 = nArray[n23];
                        int n25 = nArray2[n23];
                        if (n25 == Integer.MIN_VALUE) continue block4;
                        if (n24 != Integer.MIN_VALUE && j >= 2 && n6 - n24 >= 3 && n20 < n6) {
                            int n26 = n6 - n24;
                            if (n26 <= n5) continue block4;
                            n5 = n26;
                            n4 = n24;
                            n16 = n + n21;
                            n17 = n2 + n22;
                            continue block4;
                        }
                        if ((n20 = Math.max(n20, Math.max(n25, n24))) >= n6) continue block4;
                    }
                }
                if (n5 <= 0 || StreamPrefetcher.a(riverEngine, n + i, n2 + n8, n4 + 1, (n15 = (int)Math.ceil(Math.hypot(n16 - (n + i), n17 - (n2 + n8)))) + 4)) continue;
                ++n10;
                int n27 = Math.min(32, n5);
                nArray3[n27] = nArray3[n27] + 1;
                if (n9 >= 20 && (n5 < 12 || n9 >= 90)) continue;
                stringBuilder.append(String.format(Locale.ROOT, "%n  water %d at (%d,%d) would pour onto water %d at (%d,%d) drop=%d", n6, n + i, n2 + n8, n4, n16, n17, n5));
                ++n9;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        for (int i = 3; i < nArray3.length; ++i) {
            if (nArray3[i] <= 0) continue;
            stringBuilder2.append(String.format("%n   drop %2d : %d columns", i, nArray3[i]));
        }
        Constants.LOG.info("[SR-MONOAUDIT] {}x{} around ({},{}): {} uphill-water columns (drop >= {} onto nearby lower water, no containing ground line between, cascades exempt).{}{}{}", n11, n11, n, n2, n10, 3, stringBuilder2, n10 > 0 ? "\n worst offenders:" : " CLEAN.", stringBuilder);
    }

    public static void segmentMap(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        record Row(double d, String text) {
        }
        int n4;
        StringBuilder stringBuilder = new StringBuilder(32768);
        stringBuilder.append(String.format(Locale.ROOT, "%n=== STREAM SEGMENT MAP @ (%d,%d) radius %d, step 2 ===%n", n, n2, n3));
        stringBuilder.append("  digit = channel water surface (last digit) | # = bank/berm corridor | L = lake | . = untouched | @ = here").append(String.format(Locale.ROOT, "%n", new Object[0]));
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        for (int i = n2 - n3; i <= n2 + n3; i += 2) {
            stringBuilder.append(String.format(Locale.ROOT, "  z%6d  ", i));
            for (int j = n - n3; j <= n + n3; j += 2) {
                if (Math.abs(j - n) <= 1 && Math.abs(i - n2) <= 1) {
                    n4 = 64;
                } else {
                    Lake lake = riverEngine.lakeAt(j, i);
                    ColumnPlan columnPlan = riverEngine.plan(j, i);
                    if (lake != null) {
                        n4 = 76;
                    } else if (columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inChannel()) {
                        int n5 = columnPlan.waterSurfaceY();
                        hashMap.merge(n5, 1, Integer::sum);
                        n4 = (char)(48 + Math.floorMod(n5, 10));
                    } else {
                        n4 = columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inCorridor() ? 35 : 46;
                    }
                }
                stringBuilder.append((char)n4);
            }
            stringBuilder.append(String.format(Locale.ROOT, "%n", new Object[0]));
        }
        stringBuilder.append(String.format(Locale.ROOT, "  x from %d (left) to %d (right)%n", n - n3, n + n3));
        ArrayList arrayList = new ArrayList(hashMap.keySet());
        Collections.sort(arrayList);
        stringBuilder.append("  channel water levels on the map: ");
        Iterator object = arrayList.iterator();
        while (object.hasNext()) {
            n4 = (Integer)object.next();
            stringBuilder.append(n4).append('(').append(hashMap.get(n4)).append("col) ");
        }
        stringBuilder.append(String.format(Locale.ROOT, "%n%n  segments within %d blocks (nearest first):%n", n3));
        ArrayList<Row> rows = new ArrayList<Row>();
        PlateKey[] plateKeyArray = riverEngine.platesNear(n, n2, n3);
        int n6 = plateKeyArray.length;
        for (int i = 0; i < n6; ++i) {
            PlateKey plateKey = plateKeyArray[i];
            RiverNetwork riverNetwork = riverEngine.networkFor(plateKey);
            List<RiverSegment> list = riverNetwork.segments();
            int[] nArray = NetworkTracer.chainIds(list);
            for (int j = 0; j < list.size(); ++j) {
                double d;
                RiverSegment riverSegment = list.get(j);
                double d2 = riverSegment.distanceAt((double)n + 0.5, (double)n2 + 0.5, d = riverSegment.clampParam((double)n + 0.5, (double)n2 + 0.5));
                if (!(d2 <= (double)n3)) continue;
                rows.add(new Row(d2, String.format(Locale.ROOT, "  d%5.1f | %s chain#%d seg %d | (%.0f,%.0f) y%.1f w%.1f -> (%.0f,%.0f) y%.1f w%.1f", d2, plateKey, nArray[j], j, riverSegment.x1(), riverSegment.z1(), riverSegment.y1(), riverSegment.w1(), riverSegment.x2(), riverSegment.z2(), riverSegment.y2(), riverSegment.w2())));
            }
        }
        rows.sort(Comparator.comparingDouble(Row::d));
        int n7 = Math.min(rows.size(), 80);
        for (n6 = 0; n6 < n7; ++n6) {
            stringBuilder.append(rows.get(n6).text()).append(String.format(Locale.ROOT, "%n", new Object[0]));
        }
        if (rows.size() > n7) {
            stringBuilder.append("  (+").append(rows.size() - n7).append(" more)").append(String.format(Locale.ROOT, "%n", new Object[0]));
        }
        Constants.LOG.info(stringBuilder.toString());
    }

    public static void superTrace(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        Object object;
        int n3;
        int n4;
        Object object3;
        int n5;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        RiverNetwork riverNetwork = riverEngine.networkAt(n, n2);
        RiverSample riverSample = riverNetwork.sample((double)n + 0.5, (double)n2 + 0.5, 64.0);
        if (riverSample == null || riverSample.segId() < 0) {
            Constants.LOG.info("=== STREAM SUPER TRACE: no stream within 64 blocks of ({},{}) in this plate ===", (Object)n, (Object)n2);
            return;
        }
        List<RiverSegment> list = riverNetwork.segments();
        int[] nArray = NetworkTracer.chainIds(list);
        int n6 = nArray[riverSample.segId()];
        HashMap<Long, List> hashMap = new HashMap<Long, List>();
        HashMap<Long, Integer> hashMap2 = new HashMap<Long, Integer>();
        HashMap<Long, Integer> hashMap3 = new HashMap<Long, Integer>();
        for (n5 = 0; n5 < list.size(); ++n5) {
            object3 = list.get(n5);
            hashMap.computeIfAbsent(StreamPrefetcher.a(((RiverSegment)object3).x2(), ((RiverSegment)object3).z2()), l -> new ArrayList()).add(n5);
            hashMap3.put(StreamPrefetcher.a(((RiverSegment)object3).x1(), ((RiverSegment)object3).z1()), n5);
            if (nArray[n5] != n6) continue;
            hashMap2.put(StreamPrefetcher.a(((RiverSegment)object3).x1(), ((RiverSegment)object3).z1()), n5);
        }
        n5 = -1;
        for (Map.Entry object22 : hashMap2.entrySet()) {
            List n8 = (List)hashMap.get(object22.getKey());
            n4 = 0;
            if (n8 != null) {
                Iterator d = n8.iterator();
                while (d.hasNext()) {
                    int n7 = (Integer)d.next();
                    if (nArray[n7] != n6) continue;
                    n4 = 1;
                    break;
                }
            }
            if (n4 != 0) continue;
            n5 = (Integer)object22.getValue();
            break;
        }
        if (n5 < 0) {
            n5 = riverSample.segId();
        }
        object3 = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        ((StringBuilder)object3).append(String.format(Locale.ROOT, "%n=== STREAM SUPER TRACE @ (%d,%d): chain #%d, plate %s ===%n", n, n2, n6, riverNetwork.key()));
        ((StringBuilder)object3).append("  cols: pos | preGround | rawGrade | surface(plan) | placedWater | placedWall | lake | halfW | flags\n");
        int n8 = n5;
        n4 = 0;
        double d = 0.0;
        int n9 = Integer.MIN_VALUE;
        double d2 = Double.NaN;
        int n10 = 0;
        int n11 = n6;
        RiverSegment riverSegment = null;
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (n3 = 0; n3 < 100000 && n8 >= 0 && hashSet.add(n8); ++n3) {
            int n12;
            int lake;
            CharSequence charSequence;
            int n13;
            int string;
            Object n21;
            RiverSegment n26 = list.get(n8);
            d += Math.hypot(n26.x2() - n26.x1(), n26.z2() - n26.z1());
            object = (List)hashMap.get(StreamPrefetcher.a(n26.x1(), n26.z1()));
            if (object != null) {
                Iterator junctionIterator = ((List)object).iterator();
                while (junctionIterator.hasNext()) {
                    string = (Integer)junctionIterator.next();
                    if (nArray[string] == nArray[n8]) continue;
                    RiverSegment n22 = list.get(string);
                    int columnPlan = StreamPrefetcher.a(riverEngine, n26.y1(), n26.w1());
                    n13 = StreamPrefetcher.a(riverEngine, n22.y2(), n22.w2());
                    string = StreamPrefetcher.a(riverEngine, n26.y2(), n26.w2());
                    charSequence = new StringBuilder();
                    lake = string;
                    double n23 = 0.0;
                    for (n12 = 0; n12 < 400 && n23 < 16.0; ++n12) {
                        RiverSegment riverSegment2 = list.get(lake);
                        n23 += Math.hypot(riverSegment2.x2() - riverSegment2.x1(), riverSegment2.z2() - riverSegment2.z1());
                        if (n12 % 8 == 0) {
                            ((StringBuilder)charSequence).insert(0, StreamPrefetcher.a(riverEngine, riverSegment2.y1(), riverSegment2.w1()) + " > ");
                        }
                        List list2 = (List)hashMap.get(StreamPrefetcher.a(riverSegment2.x1(), riverSegment2.z1()));
                        int n14 = -1;
                        if (list2 != null) {
                            Iterator iterator = list2.iterator();
                            while (iterator.hasNext()) {
                                int n15 = (Integer)iterator.next();
                                if (nArray[n15] != nArray[string]) continue;
                                n14 = n15;
                                break;
                            }
                        }
                        if (n14 < 0) break;
                        lake = n14;
                    }
                    stringBuilder.append(String.format(Locale.ROOT, "%n  JUNCTION at (%d,%d): inflow chain #%d [%s%d] (grade %.2f w %.1f) joins chain #%d at %d (grade %.2f w %.1f) -> downstream %d%s", (int)n26.x1(), (int)n26.z1(), nArray[string], charSequence, n13, n22.y2(), n22.w2(), nArray[n8], columnPlan, n26.y1(), n26.w1(), string, string > Math.min(n13, columnPlan) ? "  << RISE (downstream above an inflow!)" : ""));
                }
            }
            if (d >= (double)n4 * 4.0) {
                int n16;
                int n27 = (int)Math.floor(n26.x1());
                string = (int)Math.floor(n26.z1());
                int n28 = chunkGenerator.getBaseHeight(n27, string, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                ColumnPlan columnPlan = riverEngine.plan(n27, string);
                n13 = columnPlan.inCorridor() ? columnPlan.waterSurfaceY() : StreamPrefetcher.a(riverEngine, n26.y1(), n26.w1());
                String string2 = "-";
                charSequence = "-";
                if (serverLevel.hasChunk(n27 >> 4, string >> 4)) {
                    lake = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n27, string) - 1;
                    charSequence = String.valueOf(lake);
                    int n24 = -9999;
                    for (n16 = Math.max(lake, n13) + 4; n16 >= n13 - 12; --n16) {
                        if (serverLevel.getBlockState(new BlockPos(n27, n16, string)).getFluidState().isEmpty()) continue;
                        n24 = n16;
                        break;
                    }
                    string2 = n24 == -9999 ? "-" : String.valueOf(n24);
                }
                Lake lake2 = riverEngine.lakeAt(n27, string);
                int n17 = RiverCarverFeature.nearbyLakeLevel(riverEngine, n27, string, serverLevel.getMinBuildHeight(), serverLevel.getMaxBuildHeight());
                n16 = !Double.isNaN(d2) && n26.y1() > d2 + 0.01 ? 1 : 0;
                int n18 = n12 = n9 != Integer.MIN_VALUE && n13 > n9 ? 1 : 0;
                if (n16 != 0 || n12 != 0) {
                    ++n10;
                }
                Object[] objectArray = new Object[13];
                objectArray[0] = n27;
                objectArray[1] = string;
                objectArray[2] = n28;
                objectArray[3] = n26.y1();
                objectArray[4] = n13;
                objectArray[5] = string2;
                objectArray[6] = charSequence;
                objectArray[7] = lake2 != null ? "L" + lake2.level() : "  - ";
                objectArray[8] = lake2 == null && n17 != Integer.MIN_VALUE ? "~L" + n17 : "";
                objectArray[9] = n26.w1();
                objectArray[10] = columnPlan.terminal() ? " TERM" : "";
                Object object2 = objectArray[11] = columnPlan.source() ? " SRC" : "";
                objectArray[12] = n16 != 0 || n12 != 0 ? "  << RISE " + (String)(n16 != 0 ? "raw " + d2 + "->" + n26.y1() : "") + (String)(n12 != 0 ? "surf " + n9 + "->" + n13 : "") : "";
                ((StringBuilder)object3).append(String.format(Locale.ROOT, "  (%6d,%6d) | g %3d | raw %7.2f | surf %3d | water %4s | wall %4s | %s%s | w %4.1f |%s%s%s%n", objectArray));
                n9 = n13;
                d2 = n26.y1();
                ++n4;
            }
            riverSegment = n26;
            n21 = (Integer)hashMap3.get(StreamPrefetcher.a(n26.x2(), n26.z2()));
            if (n21 != null && (Integer)n21 != n8 && nArray[(Integer)n21] != n11) {
                ((StringBuilder)object3).append(String.format(Locale.ROOT, "  -- continues as chain #%d --%n", nArray[(Integer)n21]));
                n11 = nArray[(Integer)n21];
            }
            n8 = n21 != null && (Integer)n21 != n8 ? (Integer)n21 : -1;
        }
        if (riverSegment != null) {
            String string;
            n3 = (int)Math.floor(riverSegment.x2());
            int n19 = (int)Math.floor(riverSegment.z2());
            object = riverEngine.lakeAt(n3, n19);
            int n20 = StreamPrefetcher.a(riverEngine, riverSegment.y2(), riverSegment.w2());
            if (object != null) {
                string = "ends IN lake (level " + ((Lake)object).level() + "): our surface " + n20 + (String)(n20 >= ((Lake)object).level() ? " -> FLUSH/above (ok)" : " -> BELOW LAKE by " + (((Lake)object).level() - n20) + " << PROBLEM");
            } else if (riverSegment.y2() <= (double)riverEngine.config().seaLevel()) {
                string = "ends at the SEA (grade " + String.format(Locale.ROOT, "%.1f", riverSegment.y2()) + ")";
            } else {
                int n21 = chunkGenerator.getBaseHeight(n3, n19, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                string = "DRY end at grade " + String.format(Locale.ROOT, "%.1f", riverSegment.y2()) + " over ground " + n21 + " (truncated/terminal pool)";
            }
            ((StringBuilder)object3).append(String.format(Locale.ROOT, "  END at (%d,%d): %s%n", n3, n19, string));
        }
        ((StringBuilder)object3).append(String.format(Locale.ROOT, "  walked ~%.0f blocks to the terminus; %d downstream RISE row(s)%s; junctions:%s%n", d, n10, n10 > 0 ? " << NOT MONOTONE" : " (monotone)", stringBuilder.length() == 0 ? " none" : stringBuilder.toString()));
        ((StringBuilder)object3).append("  (per-pass modifier history: rebuild fresh with -Dstreamsreflowing.segdump=x,z,r -- a store-loaded network no longer carries it)");
        Constants.LOG.info(((StringBuilder)object3).toString());
    }

    private static int a(RiverEngine riverEngine, double d, double d2) {
        double d3 = 0.5 + 0.5 * Math.min(1.0, d2 / 3.0);
        return Math.max(riverEngine.config().seaLevel() - 1, (int)Math.floor(d) - (int)Math.round(riverEngine.config().ditchInset() * d3));
    }

    private static long a(double d, double d2) {
        long l = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l2 = Math.round(d2 * 16.0) & 0xFFFFFFFFL;
        return l << 32 | l2;
    }

    private static boolean a(RiverEngine riverEngine, int n, int n2, int n3, int n4) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        for (int i = 0; i < n4 / 2; ++i) {
            ColumnPlan columnPlan = riverEngine.plan((int)Math.floor(d), (int)Math.floor(d2));
            if (!columnPlan.inCorridor()) {
                return false;
            }
            if (columnPlan.waterSurfaceY() <= n3) {
                return true;
            }
            double d3 = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
            if (d3 < 1.0E-6) {
                return false;
            }
            d += columnPlan.flowDirX() / d3 * 2.0;
            d2 += columnPlan.flowDirZ() / d3 * 2.0;
        }
        return false;
    }

    public static void spoutScan(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        int n12 = n3 * 2 + 1;
        int[] nArray = new int[n12 * n12];
        boolean[] blArray = new boolean[n12 * n12];
        PlateKey plateKey = riverEngine.plateAt(n, n2);
        int n13 = 32;
        for (n11 = -n3; n11 <= n3; ++n11) {
            for (n10 = -n3; n10 <= n3; ++n10) {
                n9 = n + n10;
                n8 = n2 + n11;
                if (!riverEngine.plateAt(n9, n8).equals(plateKey) || !riverEngine.plateAt(n9 - n13, n8).equals(plateKey) || !riverEngine.plateAt(n9 + n13, n8).equals(plateKey) || !riverEngine.plateAt(n9, n8 - n13).equals(plateKey) || !riverEngine.plateAt(n9, n8 + n13).equals(plateKey) || !riverEngine.plateAt(n9 - n13, n8 - n13).equals(plateKey) || !riverEngine.plateAt(n9 + n13, n8 + n13).equals(plateKey) || !riverEngine.plateAt(n9 - n13, n8 + n13).equals(plateKey) || !riverEngine.plateAt(n9 + n13, n8 - n13).equals(plateKey)) continue;
                n7 = (n11 + n3) * n12 + (n10 + n3);
                n6 = Integer.MIN_VALUE;
                ColumnPlan columnPlan = riverEngine.plan(n9, n8);
                if (columnPlan.inCorridor()) {
                    n6 = columnPlan.waterSurfaceY();
                }
                if ((n5 = riverEngine.lakeSurfaceAt(n9, n8)) != Integer.MIN_VALUE && n5 > n6) {
                    n6 = n5;
                }
                if (n6 == Integer.MIN_VALUE) continue;
                blArray[n7] = true;
                nArray[n7] = n6;
            }
        }
        n11 = 6;
        n10 = 1;
        n9 = 4;
        n8 = 0;
        n7 = 0;
        n6 = 0;
        int n14 = 0;
        n5 = 0;
        int n15 = 0;
        int[] nArray2 = new int[48];
        StringBuilder stringBuilder = new StringBuilder();
        int n16 = 0;
        for (int i = 0; i < n12; ++i) {
            for (n4 = 0; n4 < n12; ++n4) {
                int n17;
                int n18;
                int n19;
                int n20;
                int n21;
                int n22;
                int n23 = i * n12 + n4;
                if (!blArray[n23]) continue;
                int n24 = nArray[n23];
                int n25 = Integer.MAX_VALUE;
                int n26 = 0;
                int n27 = 0;
                for (n22 = -n11; n22 <= n11; ++n22) {
                    for (n21 = -n11; n21 <= n11; ++n21) {
                        if (n22 == 0 && n21 == 0) continue;
                        n20 = i + n22;
                        n19 = n4 + n21;
                        if (n20 < 0 || n19 < 0 || n20 >= n12 || n19 >= n12 || !blArray[n18 = n20 * n12 + n19]) continue;
                        ++n27;
                        n17 = nArray[n18];
                        if (n17 < n25) {
                            n25 = n17;
                        }
                        if (n17 < n24) continue;
                        ++n26;
                    }
                }
                if (n27 < 8 || n26 > n27 / 10 || n24 - n25 < n10) continue;
                n22 = 0;
                for (n21 = -n11; n21 <= n11; ++n21) {
                    for (n20 = -n11; n20 <= n11; ++n20) {
                        n19 = i + n21;
                        n18 = n4 + n20;
                        if (n19 < 0 || n18 < 0 || n19 >= n12 || n18 >= n12 || !blArray[n17 = n19 * n12 + n18] || nArray[n17] > n25 + n9) continue;
                        ++n22;
                    }
                }
                if (n22 * 10 < n27 * 6) {
                    ++n7;
                    continue;
                }
                n21 = n + (n4 - n3);
                n20 = n2 + (i - n3);
                n19 = chunkGenerator.getBaseHeight(n21, n20, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                if (n24 <= n19) {
                    ++n6;
                    continue;
                }
                ++n8;
                n18 = n24 - n25;
                int n28 = Math.min(nArray2.length - 1, n18);
                nArray2[n28] = nArray2[n28] + 1;
                if (n18 > n14) {
                    n14 = n18;
                    n5 = n + (n4 - n3);
                    n15 = n2 + (i - n3);
                }
                if (n16 >= 24) continue;
                stringBuilder.append(" (").append(n + (n4 - n3)).append(',').append(n2 + (i - n3)).append(")+").append(n18).append("@y").append(n24);
                ++n16;
            }
        }
        Constants.LOG.info("[SR-SPOUTSCAN] plate {} (whole-plate sweep from {},{}): {} REAL perched-spout columns (water poking >= {} above a surrounding flat water body AND above natural ground) -- ignored {} cascades + {} buried (planned surface below ground)", plateKey, n, n2, n8, n10, n7, n6);
        if (n8 == 0) {
            Constants.LOG.info("[SR-SPOUTSCAN] CLEAN: no water poking above a surrounding water body in the region.");
            return;
        }
        Constants.LOG.info("[SR-SPOUTSCAN] worst: +{} at ({},{})", n14, n5, n15);
        StringBuilder stringBuilder2 = new StringBuilder("[SR-SPOUTSCAN] histogram (height:columns):");
        for (n4 = n10; n4 < nArray2.length; ++n4) {
            if (nArray2[n4] <= 0) continue;
            stringBuilder2.append(" +").append(n4).append(":").append(nArray2[n4]);
        }
        Constants.LOG.info(stringBuilder2.toString());
        Constants.LOG.info("[SR-SPOUTSCAN] columns:{}", (Object)stringBuilder.toString());
    }

    public static void traceColumn(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        int n3 = n;
        int n4 = n2;
        ColumnPlan columnPlan = riverEngine.plan(n, n2);
        if (!columnPlan.inCorridor()) {
            double d = Double.MAX_VALUE;
            for (int i = -10; i <= 10; ++i) {
                for (int j = -10; j <= 10; ++j) {
                    ColumnPlan columnPlan2 = riverEngine.plan(n + j, n2 + i);
                    if (!columnPlan2.inCorridor() || !(columnPlan2.distance() < d)) continue;
                    d = columnPlan2.distance();
                    columnPlan = columnPlan2;
                    n3 = n + j;
                    n4 = n2 + i;
                }
            }
        }
        if (!columnPlan.inCorridor()) {
            Constants.LOG.info("=== STREAM TRACE: no stream corridor within 10 blocks of ({},{}) ===", (Object)n, (Object)n2);
            return;
        }
        int n5 = riverEngine.config().seaLevel();
        int n6 = columnPlan.waterSurfaceY();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        int n7 = chunkGenerator.getBaseHeight(n3, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%n=== STREAM TRACE (%d,%d) ===%n", n3, n4));
        stringBuilder.append(String.format("plan: rawGradeY=%.2f  waterSurfaceY=%d  ground=%d  perch(water-ground)=%+d%n", columnPlan.gradeY(), n6, n7, n6 - n7));
        stringBuilder.append(String.format("  sea floor (seaLevel-1) = %d  ->  %s%n", n5 - 1, n6 <= n5 - 1 ? "water is AT/below the sea floor -- the floor MAY be lifting it (check ground vs grade below)" : "water is ABOVE the sea floor -- floor is NOT the cause; the grade itself sits high here"));
        stringBuilder.append(String.format("  distance=%.1f halfWidth=%.1f source=%b terminal=%b inChannel=%b%n", columnPlan.distance(), columnPlan.width(), columnPlan.source(), columnPlan.terminal(), columnPlan.inChannel()));
        int n8 = columnPlan.bedY();
        int n9 = 24;
        double d = columnPlan.distToOutlet();
        boolean bl = n7 < n6;
        String string = "-";
        if (n7 > n8) {
            double d2 = d >= 8.988465674311579E307 ? 1.0 : Math.min(1.0, d / (double)n9);
            double d3 = d2 * d2 * (3.0 - 2.0 * d2);
            string = String.valueOf((int)Math.round((double)n7 + (double)(n8 - n7) * d3));
        }
        stringBuilder.append(String.format("  MOUTH NOTCH(offshore): deepBed=%d ambientFloor=%d distToOutlet=%s  submergedHere=%b  notchBed=%s%n", n8, n7, d >= 8.988465674311579E307 ? "none" : String.format("%.1f", d), bl, string));
        stringBuilder.append("longitudinal (follow the channel; ground is the floor beneath the centreline):\n");
        List<String> list = StreamPrefetcher.a(riverEngine, serverLevel, n3, n4, -1);
        Collections.reverse(list);
        for (String string2 : list) {
            stringBuilder.append(string2).append('\n');
        }
        stringBuilder.append(String.format("   >> HERE (%d,%d): rawGrade=%.2f water=%d ground=%d perch=%+d%n", n3, n4, columnPlan.gradeY(), n6, n7, n6 - n7));
        for (String string3 : StreamPrefetcher.a(riverEngine, serverLevel, n3, n4, 1)) {
            stringBuilder.append(string3).append('\n');
        }
        Constants.LOG.info(stringBuilder.toString());
    }

    private static List<String> a(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        ArrayList<String> arrayList = new ArrayList<String>();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        for (int i = 1; i <= 30; ++i) {
            int n4;
            int n5;
            ColumnPlan columnPlan;
            ColumnPlan columnPlan2 = riverEngine.plan((int)Math.floor(d), (int)Math.floor(d2));
            double d3 = Math.hypot(columnPlan2.flowDirX(), columnPlan2.flowDirZ());
            if (!columnPlan2.inCorridor() || d3 < 1.0E-6 || !(columnPlan = riverEngine.plan(n5 = (int)Math.floor(d += (double)n3 * (columnPlan2.flowDirX() / d3) * 2.0), n4 = (int)Math.floor(d2 += (double)n3 * (columnPlan2.flowDirZ() / d3) * 2.0))).inCorridor()) break;
            int n6 = chunkGenerator.getBaseHeight(n5, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
            int n7 = columnPlan.waterSurfaceY();
            arrayList.add(String.format("   %s%-2d (%d,%d): rawGrade=%6.2f water=%d ground=%d perch=%+d", n3 < 0 ? "up" : "dn", i, n5, n4, columnPlan.gradeY(), n7, n6, n7 - n6));
        }
        return arrayList;
    }

    private static void a(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        int n3;
        int n4;
        int n5;
        int n6;
        int n7;
        int n8 = n;
        int n9 = n2;
        ColumnPlan columnPlan = riverEngine.plan(n, n2);
        if (!columnPlan.inCorridor()) {
            double d = Double.MAX_VALUE;
            for (int i = -10; i <= 10; ++i) {
                for (n7 = -10; n7 <= 10; ++n7) {
                    ColumnPlan columnPlan2 = riverEngine.plan(n + n7, n2 + i);
                    if (!columnPlan2.inCorridor() || !(columnPlan2.distance() < d)) continue;
                    d = columnPlan2.distance();
                    columnPlan = columnPlan2;
                    n8 = n + n7;
                    n9 = n2 + i;
                }
            }
        }
        if (!columnPlan.inCorridor()) {
            Constants.LOG.info("=== PERCH DIAG: no stream corridor within 10 blocks of ({},{}) ===", (Object)n, (Object)n2);
            return;
        }
        int n10 = columnPlan.waterSurfaceY();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        n7 = chunkGenerator.getBaseHeight(n8, n9, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
        int n11 = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n8, n9) - 1;
        int n12 = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, n8, n9) - 1;
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        StringBuilder stringBuilder = new StringBuilder();
        Object object = "n/a";
        Object object2 = "n/a";
        if (d > 1.0E-6) {
            double d2 = -columnPlan.flowDirZ() / d;
            double d3 = columnPlan.flowDirX() / d;
            for (int i = -1; i <= 1; i += 2) {
                Object object3;
                StringBuilder stringBuilder2 = new StringBuilder();
                int n13 = -1;
                for (int j = 1; j <= 16; ++j) {
                    int n14 = n8 + (int)Math.round(d2 * (double)j * (double)i);
                    int n15 = n9 + (int)Math.round(d3 * (double)j * (double)i);
                    n6 = chunkGenerator.getBaseHeight(n14, n15, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                    stringBuilder2.append(String.format("%4d", n6));
                    if (n13 >= 0 || n6 < n10) continue;
                    n13 = j;
                }
                String string = i < 0 ? "L" : "R";
                stringBuilder.append("  ").append(string).append("(out->):").append((CharSequence)stringBuilder2).append('\n');
                Object object4 = object3 = n13 < 0 ? "NEVER (>= grade)" : "rises to grade at d=" + n13;
                if (i < 0) {
                    object = object3;
                    continue;
                }
                object2 = object3;
            }
        }
        boolean bl = !"NEVER (>= grade)".equals(object) && !"NEVER (>= grade)".equals(object2) && d > 1.0E-6;
        StringBuilder stringBuilder3 = new StringBuilder();
        boolean bl2 = false;
        boolean bl3 = false;
        if (d > 1.0E-6) {
            double d4 = -columnPlan.flowDirZ() / d;
            double d5 = columnPlan.flowDirX() / d;
            for (int i = -1; i <= 1; i += 2) {
                StringBuilder stringBuilder4 = new StringBuilder();
                for (n6 = 1; n6 <= 12; ++n6) {
                    n5 = riverEngine.plan(n8 + (int)Math.round(d4 * (double)n6 * (double)i), n9 + (int)Math.round(d5 * (double)n6 * (double)i)).distance() <= 2.0 ? 1 : 0;
                    stringBuilder4.append(n5 != 0 ? " C" : " .");
                    if (n5 == 0) continue;
                    if (i < 0) {
                        bl3 = true;
                        continue;
                    }
                    bl2 = true;
                }
                stringBuilder3.append("  ").append(i < 0 ? "L" : "R").append("(out->):").append((CharSequence)stringBuilder4).append('\n');
            }
        }
        if (d > 1.0E-6) {
            double d6 = -columnPlan.flowDirZ() / d;
            double d7 = columnPlan.flowDirX() / d;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("containment walk (d out each side; grade=col waterSurfaceY, g=pre-carve ground, T=terminal[disables cup+berm], ch=inChannel):\n");
            stringBuilder5.append(String.format("    d= 0 (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%n", n8, n9, columnPlan.waterSurfaceY(), n7, columnPlan.waterSurfaceY() - n7, columnPlan.terminal(), columnPlan.inChannel(), columnPlan.source(), columnPlan.distance()));
            for (int i = -1; i <= 1; i += 2) {
                stringBuilder5.append("  ").append(i < 0 ? "L (rim/mountain side):" : "R (open/sea side):").append('\n');
                for (n6 = 1; n6 <= 12; ++n6) {
                    n5 = n8 + (int)Math.round(d6 * (double)n6 * (double)i);
                    n4 = n9 + (int)Math.round(d7 * (double)n6 * (double)i);
                    ColumnPlan columnPlan3 = riverEngine.plan(n5, n4);
                    n3 = chunkGenerator.getBaseHeight(n5, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                    stringBuilder5.append(String.format("    d=%2d (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%s%n", n6, n5, n4, columnPlan3.waterSurfaceY(), n3, columnPlan3.waterSurfaceY() - n3, columnPlan3.terminal(), columnPlan3.inChannel(), columnPlan3.source(), columnPlan3.distance(), columnPlan3.inCorridor() ? "" : " [OUT-OF-CORRIDOR]"));
                }
            }
            Constants.LOG.info(stringBuilder5.toString());
        }
        Lake lake = riverEngine.lakeAt(n8, n9);
        int n16 = -1;
        int n17 = Integer.MIN_VALUE;
        for (int i = 1; i <= 24 && n16 < 0; ++i) {
            for (int j = -i; j <= i && n16 < 0; ++j) {
                for (int k = -i; k <= i && n16 < 0; ++k) {
                    Lake lake2;
                    if (Math.max(Math.abs(k), Math.abs(j)) != i || (lake2 = riverEngine.lakeAt(n8 + k, n9 + j)) == null) continue;
                    n16 = i;
                    n17 = lake2.level();
                }
            }
        }
        String string = "covering this column: " + (String)(lake != null ? "YES (level=" + lake.level() + ")" : "none") + ";  nearest Lake object within 24 blocks: " + (String)(n16 >= 0 ? "d=" + n16 + " level=" + n17 : "NONE (this water is a hollow-pool / pond, not a basin Lake)");
        Constants.LOG.info("=== PERCH DIAG column ({},{}) ===\nLAKE object {}\ngrade(waterSurfaceY)={}  rawGradeY={}  bedY={}  GROUND(pre-carve)={}  WALL(post-carve)={}  waterTop(placed)={}\nperch? grade-ground = {}   (>0 = water sits ABOVE ground here)\ninChannel={} distance={} halfWidth={} slope={} upstreamSlope={} terminal={} source={}\nperpendicular ground profile (each col = +1 block out from channel, to 16):\n{}left rim: {}   right rim: {}\n=> hollowEnclosed would be {} (both sides must rise to grade within 16) -> {}\nperpendicular STREAM scan (engine; C=stream centerline within 2 blocks, .=none, out to 12):\n{}this col inAnyChannel={}  stream on L-side={} R-side={}  => bermYieldsToNeighbor(both sides)={}", n8, n9, string, n10, String.format("%.1f", columnPlan.gradeY()), columnPlan.bedY(), n7, n11, n12, n10 - n7, columnPlan.inChannel(), String.format("%.1f", columnPlan.distance()), String.format("%.1f", columnPlan.width()), String.format("%.3f", columnPlan.slope()), String.format("%.3f", columnPlan.upstreamSlope()), columnPlan.terminal(), columnPlan.source(), stringBuilder, object, object2, bl, bl ? "POOLS to grade" : "falls through to berm/bank carve (stays perched if grade>ground)", stringBuilder3, riverEngine.inAnyChannel(n8, n9), bl3, bl2, bl2 && bl3);
        if (d > 1.0E-6) {
            Object object5;
            int n18;
            int n19;
            int n20;
            double d8 = -columnPlan.flowDirZ() / d;
            double d9 = columnPlan.flowDirX() / d;
            n4 = 16;
            int n21 = n10 + 2;
            n3 = n10 - 34;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("     ").append(" ".repeat(n4)).append("v  centre (d=0), L=-d out to left, R=+d out to right\n");
            for (n20 = n21; n20 >= n3; --n20) {
                stringBuilder6.append(String.format("y%3d ", n20));
                for (int i = -n4; i <= n4; ++i) {
                    n19 = n8 + (int)Math.round(d8 * (double)i);
                    object5 = serverLevel.getFluidState(mutableBlockPos.set(n19, n20, n18 = n9 + (int)Math.round(d9 * (double)i)));
                    int n22 = !((FluidState)object5).isEmpty() && ((FluidState)object5).is(FluidTags.WATER) ? (((FluidState)object5).getType() == Fluids.WATER || ((FluidState)object5).getType() == Fluids.FLOWING_WATER ? 111 : 126) : (serverLevel.getBlockState(mutableBlockPos).isAir() ? 32 : 35);
                    stringBuilder6.append((char)n22);
                }
                stringBuilder6.append('\n');
            }
            n20 = 48;
            StringBuilder stringBuilder7 = new StringBuilder();
            for (n19 = -2; n19 <= 2; ++n19) {
                for (n18 = -2; n18 <= 2; ++n18) {
                    object5 = riverEngine.plateAt(n8 + n18 * n20, n9 + n19 * n20);
                    stringBuilder7.append(String.format(" %3d,%-3d", ((PlateKey)object5).cx(), ((PlateKey)object5).cz()));
                }
                stringBuilder7.append('\n');
            }
            Constants.LOG.info("cross-section perpendicular to flow (post-carve blocks; ~=stream o=vanilla-water #=solid):\n{}plate (cx,cz) on a {}-block grid, centre row/col = this column (differing ids => a plate seam):\n{}", stringBuilder6, n20, stringBuilder7);
        }
    }

    private static void a(RiverEngine riverEngine, ServerLevel serverLevel, ChunkPos chunkPos) {
        int n = chunkPos.getMinBlockX();
        int n2 = chunkPos.getMinBlockZ();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder();
        StringBuilder stringBuilder3 = new StringBuilder();
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n3 = n + j;
                int n4 = n2 + i;
                stringBuilder2.append(String.format("%4d", chunkGenerator.getBaseHeight(n3, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1));
                stringBuilder3.append(String.format("%4d", serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n3, n4) - 1));
                ColumnPlan columnPlan = riverEngine.plan(n3, n4);
                stringBuilder.append(columnPlan.inCorridor() ? String.format("%4d", columnPlan.waterSurfaceY()) : "   .");
            }
            stringBuilder.append('\n');
            stringBuilder2.append('\n');
            stringBuilder3.append('\n');
        }
        Constants.LOG.info("=== PERCH DIAG chunk ({},{}) X[{}..{}] Z[{}..{}] (rows=+Z down, cols=+X right) ===\nGRADE (engine waterSurfaceY; . = not in corridor):\n{}\nGROUND (pre-carve OCEAN_FLOOR_WG, ALL cols):\n{}\nWALL (post-carve OCEAN_FLOOR, ALL cols -- pool/berm shows here):\n{}", chunkPos.x, chunkPos.z, n, n + 15, n2, n2 + 15, stringBuilder, stringBuilder2, stringBuilder3);
    }

    public static boolean startCensus(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        if (n3 <= 0 || !S.compareAndSet(false, true)) {
            return false;
        }
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        Thread thread = new Thread(() -> {
            try {
                StreamPrefetcher.a(riverEngine, chunkGenerator, randomState, serverLevel, n, n2, n3);
            }
            finally {
                S.set(false);
            }
        }, "streams-perch-census");
        thread.setDaemon(true);
        thread.setPriority(1);
        thread.start();
        Constants.LOG.info("=== PERCH CENSUS started: {}x{} blocks around ({},{}); building networks, please wait ===", n3 * 2, n3 * 2, n, n2);
        return true;
    }

    private static void a(RiverEngine riverEngine, ChunkGenerator chunkGenerator, RandomState randomState, ServerLevel serverLevel, int n, int n2, int n3) {
        try {
            int n4;
            int n5 = riverEngine.config().seaLevel();
            int[] nArray = new int[24];
            int n6 = 0;
            int n7 = 0;
            int n8 = 0;
            int n9 = 0;
            int n10 = Integer.MIN_VALUE;
            int n11 = n;
            int n12 = n2;
            int n13 = Integer.MAX_VALUE;
            int n14 = Integer.MIN_VALUE;
            int n15 = Integer.MAX_VALUE;
            int n16 = Integer.MIN_VALUE;
            int n17 = 0;
            StringBuilder stringBuilder = new StringBuilder();
            int n18 = 0;
            for (int i = -n3; i <= n3; ++i) {
                for (n4 = -n3; n4 <= n3; ++n4) {
                    int n19;
                    double d;
                    int n20 = n + n4;
                    int n21 = n2 + i;
                    ColumnPlan columnPlan = riverEngine.plan(n20, n21);
                    if (!columnPlan.inChannel() || (d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ())) < 1.0E-6) continue;
                    double d2 = -columnPlan.flowDirZ() / d;
                    double d3 = columnPlan.flowDirX() / d;
                    double d4 = columnPlan.width() + 2.0;
                    int n22 = chunkGenerator.getBaseHeight((int)Math.round((double)n20 + d2 * d4), (int)Math.round((double)n21 + d3 * d4), Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                    int n23 = Math.min(n22, n19 = chunkGenerator.getBaseHeight((int)Math.round((double)n20 - d2 * d4), (int)Math.round((double)n21 - d3 * d4), Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1);
                    if (n23 < n5) {
                        ++n7;
                        continue;
                    }
                    ++n6;
                    int n24 = columnPlan.waterSurfaceY() - n23;
                    if (n24 > n10) {
                        n10 = n24;
                        n11 = n20;
                        n12 = n21;
                    }
                    if (n24 >= 1) {
                        ++n8;
                    }
                    if (n24 >= 4) {
                        ++n9;
                    }
                    if (n24 >= 13) {
                        ++n17;
                        n13 = Math.min(n13, n20);
                        n14 = Math.max(n14, n20);
                        n15 = Math.min(n15, n21);
                        n16 = Math.max(n16, n21);
                        if (n18 < 16 && (n4 + i & 7) == 0) {
                            int n25 = chunkGenerator.getBaseHeight(n20, n21, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                            stringBuilder.append(String.format("    (%d,%d) grade=%d colGround=%d bankL=%d bankR=%d perch=%d%n", n20, n21, columnPlan.waterSurfaceY(), n25, n19, n22, n24));
                            ++n18;
                        }
                    }
                    int n26 = Math.max(0, Math.min(nArray.length - 1, n24));
                    nArray[n26] = nArray[n26] + 1;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(String.format("%n=== PERCH CENSUS COMPLETE: %dx%d blocks around (%d,%d) ===%n", n3 * 2, n3 * 2, n, n2));
            stringBuilder2.append(String.format("inland channel columns scanned: %d (excluded %d coastal/water-meeting)%n", n6, n7));
            if (n6 == 0) {
                stringBuilder2.append("  (no stream channels in this area -- try a larger radius or a different spawn)\n");
            } else {
                stringBuilder2.append(String.format("perched (water above lower bank >= 1): %d (%.1f%%)%n", n8, 100.0 * (double)n8 / (double)n6));
                stringBuilder2.append(String.format("BADLY perched (>= 4, a raised aqueduct): %d (%.1f%%)%n", n9, 100.0 * (double)n9 / (double)n6));
                stringBuilder2.append(String.format("max perch: %d blocks, at (%d,%d)%n", n10, n11, n12));
                stringBuilder2.append("histogram (perch height in blocks : column count):\n");
                for (n4 = 0; n4 < nArray.length; ++n4) {
                    if (nArray[n4] <= 0) continue;
                    stringBuilder2.append(String.format("  %2d%s : %d%n", n4, n4 == nArray.length - 1 ? "+" : " ", nArray[n4]));
                }
                if (n17 > 0) {
                    stringBuilder2.append(String.format("badly-perched (>=13) cluster: %d cols in X[%d..%d] Z[%d..%d] (span %dx%d)%n", n17, n13, n14, n15, n16, n14 - n13, n16 - n15));
                    stringBuilder2.append("samples (grade vs column ground vs the two banks):\n");
                    stringBuilder2.append((CharSequence)stringBuilder);
                }
            }
            Constants.LOG.info(stringBuilder2.toString());
        }
        catch (Throwable throwable) {
            Constants.LOG.warn("Perch census failed", throwable);
        }
    }

    static {
        J = Boolean.getBoolean("streamsreflowing.noprefetch");
        Q = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        R = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
        S = new AtomicBoolean(false);
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

