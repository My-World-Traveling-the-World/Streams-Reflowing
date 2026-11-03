/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.flow;

import dev.streamsreflowing.flow.RawFlowTileStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RawFlowTiles {
    public static final int COLUMNS = 256;
    public static final short NO_SURFACE = Short.MIN_VALUE;
    private static final int MAX_CACHED = 4096;
    private static final ConcurrentHashMap<Long, Tile> CACHE = new ConcurrentHashMap();
    private static final ConcurrentLinkedQueue<Long> ORDER = new ConcurrentLinkedQueue();

    private RawFlowTiles() {
    }

    private static long key(int cx, int cz) {
        return (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
    }

    public static void put(int cx, int cz, Tile tile) {
        Long old;
        RawFlowTileStore.store(cx, cz, tile);
        if (CACHE.put(RawFlowTiles.key(cx, cz), tile) == null) {
            ORDER.offer(RawFlowTiles.key(cx, cz));
        }
        while (CACHE.size() > 4096 && (old = ORDER.poll()) != null) {
            CACHE.remove(old);
        }
    }

    public static Tile get(int cx, int cz) {
        Tile t = CACHE.get(RawFlowTiles.key(cx, cz));
        if (t != null) {
            return t;
        }
        t = RawFlowTileStore.load(cx, cz);
        if (t != null && CACHE.put(RawFlowTiles.key(cx, cz), t) == null) {
            ORDER.offer(RawFlowTiles.key(cx, cz));
        }
        return t;
    }

    public static void clear() {
        CACHE.clear();
        ORDER.clear();
    }

    public record Tile(byte[] dirX, byte[] dirZ, short[] surfaceY) {
    }
}

