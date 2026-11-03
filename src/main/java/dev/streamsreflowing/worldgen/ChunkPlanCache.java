/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import java.util.Arrays;

public final class ChunkPlanCache {
    private static final int SLOTS = 32;
    private static final int MASK = 31;
    private static final ThreadLocal<Ring> RING = ThreadLocal.withInitial(Ring::new);
    private static volatile int generation;

    private ChunkPlanCache() {
    }

    private static long a(int n, int n2) {
        return (long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL;
    }

    private static int slot(long l) {
        return (int)(l ^ l >>> 32) & MASK;
    }

    private static Ring ring() {
        Ring ring = RING.get();
        if (ring.gen != generation) {
            Arrays.fill(ring.engines, null);
            Arrays.fill(ring.plans, null);
            ring.gen = generation;
        }
        return ring;
    }

    public static void put(RiverEngine riverEngine, int n, int n2, ColumnPlan[] columnPlanArray) {
        Ring ring = ChunkPlanCache.ring();
        long l = ChunkPlanCache.a(n, n2);
        int n3 = ChunkPlanCache.slot(l);
        ring.keys[n3] = l;
        ring.engines[n3] = riverEngine;
        ring.plans[n3] = columnPlanArray;
    }

    public static ColumnPlan[] get(RiverEngine riverEngine, int n, int n2) {
        Ring ring = ChunkPlanCache.ring();
        long l = ChunkPlanCache.a(n, n2);
        int n3 = ChunkPlanCache.slot(l);
        if (ring.keys[n3] != l || ring.engines[n3] != riverEngine) {
            return null;
        }
        return ring.plans[n3];
    }

    public static ColumnPlan[] take(RiverEngine riverEngine, int n, int n2) {
        Ring ring = ChunkPlanCache.ring();
        long l = ChunkPlanCache.a(n, n2);
        int n3 = ChunkPlanCache.slot(l);
        if (ring.keys[n3] != l || ring.engines[n3] != riverEngine) {
            return null;
        }
        ColumnPlan[] columnPlanArray = ring.plans[n3];
        ring.engines[n3] = null;
        ring.plans[n3] = null;
        return columnPlanArray;
    }

    public static void clear() {
        ++ChunkPlanCache.generation;
    }

    private static final class Ring {
        final long[] keys = new long[SLOTS];
        final RiverEngine[] engines = new RiverEngine[SLOTS];
        final ColumnPlan[][] plans = new ColumnPlan[SLOTS][];
        int gen = -1;
    }
}

