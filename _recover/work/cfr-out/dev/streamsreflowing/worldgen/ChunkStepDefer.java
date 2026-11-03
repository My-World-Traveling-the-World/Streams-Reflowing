/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.Util;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;

public final class ChunkStepDefer {
    private static final boolean a = Boolean.getBoolean("streamsreflowing.nochunkdefer");
    private static final boolean b = Boolean.getBoolean("streamsreflowing.caldiag");
    private static final ConcurrentHashMap<Long, Object[]> c = new ConcurrentHashMap();
    private static final AtomicBoolean d = new AtomicBoolean();
    private static volatile Boolean e;
    private static final double f = 48.0;

    private ChunkStepDefer() {
    }

    private static void a(ChunkPos chunkPos, double d, double d2, RiverEngine riverEngine) {
        ArrayList<PlateKey> arrayList = new ArrayList<PlateKey>(2);
        for (PlateKey plateKey : riverEngine.platesNear(d, d2, 48.0)) {
            if (riverEngine.isPlateReady(plateKey)) continue;
            arrayList.add(plateKey);
        }
        c.put((long)chunkPos.x << 32 ^ (long)chunkPos.z & 0xFFFFFFFFL, new Object[]{System.nanoTime(), riverEngine, arrayList});
        if (ChunkStepDefer.d.compareAndSet(false, true)) {
            ChunkStepDefer.a();
        }
    }

    private static void a() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    block4: while (true) {
                        Thread.sleep(10000L);
                        long l = System.nanoTime();
                        Iterator<Map.Entry<Long, Object[]>> iterator = c.entrySet().iterator();
                        while (true) {
                            if (!iterator.hasNext()) continue block4;
                            Map.Entry<Long, Object[]> entry = iterator.next();
                            Object[] objectArray = entry.getValue();
                            long l2 = (l - (Long)objectArray[0]) / 1000000L;
                            if (l2 < 15000L) continue;
                            RiverEngine riverEngine = (RiverEngine)objectArray[1];
                            List list = (List)objectArray[2];
                            StringBuilder stringBuilder = new StringBuilder();
                            for (Object e : list) {
                                PlateKey plateKey = (PlateKey)e;
                                stringBuilder.append(" [").append(plateKey).append(" ready=").append(riverEngine.isPlateReady(plateKey)).append(' ').append(StreamPrefetcher.plateDiag(riverEngine, plateKey)).append(']');
                            }
                            Constants.LOG.warn("[SR-WEDGE] chunk ({},{}) deferred {} ms and NOT resumed; plates:{} | {}", (int)(entry.getKey() >> 32), entry.getKey().intValue(), l2, stringBuilder, StreamPrefetcher.laneDiag());
                        }
                        break;
                    }
                }
                catch (InterruptedException interruptedException) {
                    return;
                }
                catch (Throwable throwable) {
                    Constants.LOG.warn("[SR-WEDGE] watchdog error", throwable);
                    continue;
                }
                break;
            }
        }, "streamsreflowing-defer-watchdog");
        thread.setDaemon(true);
        thread.setPriority(1);
        thread.start();
    }

    private static boolean b() {
        if (a || !Services.PLATFORM.fastChunkLoading()) {
            return true;
        }
        Boolean bl = e;
        if (bl == null) {
            e = bl = Boolean.valueOf(Services.PLATFORM.isModLoaded("c2me"));
        }
        return bl == false;
    }

    public static CompletableFuture<ChunkAccess> defer(ChunkStep chunkStep, WorldGenContext worldGenContext, StaticCache2D<GenerationChunkHolder> staticCache2D, ChunkAccess chunkAccess) {
        double d;
        if (ChunkStepDefer.b()) {
            return null;
        }
        if (StreamPrefetcher.isServerStopping()) {
            return null;
        }
        ChunkStatus chunkStatus = chunkStep.targetStatus();
        if (chunkStatus != ChunkStatus.CARVERS && chunkStatus != ChunkStatus.FEATURES) {
            return null;
        }
        if (!Services.PLATFORM.generateRivers()) {
            return null;
        }
        ServerLevel serverLevel = worldGenContext.level();
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return null;
        }
        if (chunkAccess.getPersistedStatus().isOrAfter(chunkStatus)) {
            return null;
        }
        RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
        if (riverEngine == null) {
            return null;
        }
        ChunkPos chunkPos = chunkAccess.getPos();
        double d2 = (double)chunkPos.getMinBlockX() + 8.0;
        if (riverEngine.platesReadyNear(d2, d = (double)chunkPos.getMinBlockZ() + 8.0, 48.0)) {
            return null;
        }
        FlowTrace.deferred.incrementAndGet();
        if (b) {
            ChunkStepDefer.a(chunkPos, d2, d, riverEngine);
        }
        long l = System.nanoTime();
        int n = chunkPos.x;
        int n2 = chunkPos.z;
        return riverEngine.whenPlatesReadyNear(d2, d, 48.0).thenComposeAsync(void_ -> {
            long l2;
            FlowTrace.resumed.incrementAndGet();
            if (b) {
                c.remove((long)n << 32 ^ (long)n2 & 0xFFFFFFFFL);
            }
            if (Boolean.getBoolean("streamsreflowing.caldiag") && (l2 = (System.nanoTime() - l) / 1000000L) >= 50L) {
                Constants.LOG.info("[SR-DEFER] chunk ({},{}) {} waited {} ms for plates", n, n2, chunkStatus == ChunkStatus.FEATURES ? "FEATURES" : "CARVERS", l2);
            }
            return chunkStep.apply(worldGenContext, staticCache2D, chunkAccess);
        }, (Executor)Util.backgroundExecutor());
    }
}

