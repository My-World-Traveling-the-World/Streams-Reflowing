/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkPlanCache {
    private static final int a = 1024;
    private static final ConcurrentHashMap<Key, ColumnPlan[]> b = new ConcurrentHashMap();

    private ChunkPlanCache() {
    }

    private static long a(int n, int n2) {
        return (long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL;
    }

    public static void put(RiverEngine riverEngine, int n, int n2, ColumnPlan[] columnPlanArray) {
        b.put(new Key(riverEngine, ChunkPlanCache.a(n, n2)), columnPlanArray);
        if (b.size() > 1024) {
            Iterator iterator = ((ConcurrentHashMap.KeySetView)b.keySet()).iterator();
            while (b.size() > 768 && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    public static ColumnPlan[] get(RiverEngine riverEngine, int n, int n2) {
        return b.get(new Key(riverEngine, ChunkPlanCache.a(n, n2)));
    }

    public static ColumnPlan[] take(RiverEngine riverEngine, int n, int n2) {
        return b.remove(new Key(riverEngine, ChunkPlanCache.a(n, n2)));
    }

    public static void clear() {
        b.clear();
    }

    private record Key(RiverEngine engine, long chunk) {
    }
}

