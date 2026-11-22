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
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
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
    private static final ConcurrentHashMap<WaitKey, Pending> c = new ConcurrentHashMap<>();
    private static final AtomicBoolean d = new AtomicBoolean();
    private static volatile Boolean e;
    private static final double f = 48.0;

    private ChunkStepDefer() {
    }

    private record WaitKey(RiverEngine engine, ChunkPos chunk, ChunkStatus status) {}
    private record Pending(long started, List<PlateKey> plates) {}

    private static Pending watch(WaitKey key, double x, double z, long started) {
        List<PlateKey> waiting = new ArrayList<>(2);
        for (PlateKey plate : key.engine().platesNear(x, z, 48.0)) {
            if (!key.engine().isPlateReady(plate)) waiting.add(plate);
        }
        Pending pending = new Pending(started, waiting);
        c.put(key, pending);
        if (d.compareAndSet(false, true)) startWatchdog();
        return pending;
    }

    private static void startWatchdog() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000L);
                    long now = System.nanoTime();
                    for (var entry : c.entrySet()) {
                        WaitKey key = entry.getKey();
                        Pending pending = entry.getValue();
                        long ms = (now - pending.started()) / 1_000_000L;
                        if (ms < 15_000L) continue;
                        StringBuilder plates = new StringBuilder();
                        for (PlateKey plate : pending.plates()) {
                            plates.append(" [").append(plate).append(" ready=")
                                    .append(key.engine().isPlateReady(plate)).append(' ')
                                    .append(StreamPrefetcher.plateDiag(key.engine(), plate)).append(']');
                        }
                        Constants.LOG.warn("[SR-WEDGE] 区块 {} 的 {} 已等候河流 {} ms；板块:{} | {}",
                                key.chunk(), key.status(), ms, plates, StreamPrefetcher.laneDiag());
                    }
                } catch (InterruptedException interrupted) {
                    return;
                } catch (RuntimeException failure) {
                    Constants.LOG.warn("[SR-WEDGE] 河流区块等候诊断失败", failure);
                }
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
        if (riverEngine.isClosed()) return null;
        ChunkPos chunkPos = chunkAccess.getPos();
        double d2 = (double)chunkPos.getMinBlockX() + 8.0;
        if (riverEngine.platesReadyNear(d2, d = (double)chunkPos.getMinBlockZ() + 8.0, 48.0)) {
            return null;
        }
        FlowTrace.deferred.incrementAndGet();
        long started = System.nanoTime();
        WaitKey key = b ? new WaitKey(riverEngine, chunkPos, chunkStatus) : null;
        Pending pending = b ? watch(key, d2, d, started) : null;
        CompletableFuture<ChunkAccess> deferred = resumeAfterPlates(
                StreamPrefetcher.requestPlates(riverEngine, d2, d, 48.0),
                () -> !riverEngine.isClosed() && !StreamPrefetcher.isServerStopping(), () -> {
                    FlowTrace.resumed.incrementAndGet();
                    long ms = (System.nanoTime() - started) / 1_000_000L;
                    if (b && ms >= 50L) Constants.LOG.info("[SR-DEFER] 区块 {} 进入 {} 前等待河流板块 {} ms",
                            chunkPos, chunkStatus, ms);
                    return chunkStep.apply(worldGenContext, staticCache2D, chunkAccess);
                }, Util.backgroundExecutor());
        // 失败和取消也必须清掉诊断，且不能删掉同一区块后来登记的等待。
        if (b) deferred.whenComplete((chunk, failure) -> c.remove(key, pending));
        return deferred;
    }

    static <T> CompletableFuture<T> resumeAfterPlates(CompletableFuture<Void> ready, BooleanSupplier open,
                                                       Supplier<CompletableFuture<T>> resume, Executor executor) {
        return ready.thenComposeAsync(ignored -> {
            // 就绪与执行之间可以关服；不能让旧世界的区块在回调队列里复活。
            if (!open.getAsBoolean()) return CompletableFuture.failedFuture(new CancellationException("河流世界已关闭"));
            return resume.get();
        }, executor);
    }
}

