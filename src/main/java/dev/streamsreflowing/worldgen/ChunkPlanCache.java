/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;

public final class ChunkPlanCache {
    private static final int SLOTS = 32;
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("streamsreflowing.chunkPlanCacheStats");
    private static final ThreadLocal<Ring> RING = ThreadLocal.withInitial(Ring::new);
    private static volatile int generation;

    private ChunkPlanCache() {
    }

    private static long a(int n, int n2) {
        return (long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL;
    }

    private static Ring ring() {
        Ring ring = RING.get();
        if (ring.gen != generation) {
            ring.table.clear();
            ring.gen = generation;
        }
        return ring;
    }

    public static void put(RiverEngine riverEngine, int n, int n2, ColumnPlan[] columnPlanArray) {
        Ring ring = ChunkPlanCache.ring();
        long l = ChunkPlanCache.a(n, n2);
        ring.table.put(riverEngine, l, columnPlanArray);
    }

    public static ColumnPlan[] get(RiverEngine riverEngine, int n, int n2) {
        Ring ring = ChunkPlanCache.ring();
        long l = ChunkPlanCache.a(n, n2);
        return ring.table.get(riverEngine, l, false);
    }

    public static ColumnPlan[] take(RiverEngine riverEngine, int n, int n2) {
        Ring ring = ChunkPlanCache.ring();
        long l = ChunkPlanCache.a(n, n2);
        return ring.table.get(riverEngine, l, true);
    }

    public static void clear() {
        ++ChunkPlanCache.generation;
    }

    /** 只统计当前线程/世界代次；默认关闭，不能把这里的数字冒充所有生成线程总和。 */
    public static Stats stats() {
        var stats = ring().table.stats();
        return new Stats(stats.hits(), stats.misses(), stats.evictions(), stats.probes());
    }

    public record Stats(long hits, long misses, long evictions, long probes) {}

    private static final class Ring {
        final BoundedPlanTable<ColumnPlan[]> table = new BoundedPlanTable<>(SLOTS, DIAGNOSTICS);
        int gen = -1;
    }
}

