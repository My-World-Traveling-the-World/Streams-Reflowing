/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.flow;

import dev.streamsreflowing.flow.FlowRemeshQueue;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientFlowGrids {
    private static final ConcurrentHashMap<Long, byte[]> GRIDS = new ConcurrentHashMap();
    private static final int MAX = 16384;
    private static volatile int version;
    private static final ThreadLocal<Memo> MEMO;

    private ClientFlowGrids() {
    }

    private static long key(int cx, int cz) {
        return (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
    }

    public static boolean put(int cx, int cz, byte[] grid) {
        boolean changed;
        byte[] prev = GRIDS.put(ClientFlowGrids.key(cx, cz), grid);
        ++version;
        boolean bl = changed = prev == null || !Arrays.equals(prev, grid);
        if (GRIDS.size() > 16384) {
            Iterator it = ((ConcurrentHashMap.KeySetView)GRIDS.keySet()).iterator();
            while (GRIDS.size() > 12288 && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return changed;
    }

    public static void remove(int cx, int cz) {
        GRIDS.remove(ClientFlowGrids.key(cx, cz));
        ++version;
    }

    public static void clear() {
        RTFFlowUnderlay.clearClient();
        GRIDS.clear();
        FlowRemeshQueue.clear();
        ++version;
    }

    public static byte[] gridAt(int cx, int cz) {
        return GRIDS.get(ClientFlowGrids.key(cx, cz));
    }

    public static byte angleAt(int x, int z) {
        byte[] g;
        long k = ClientFlowGrids.key(x >> 4, z >> 4);
        Memo memo = MEMO.get();
        int v = version;
        if (memo.key != k || memo.version != v) {
            memo.grid = GRIDS.get(k);
            memo.key = k;
            memo.version = v;
        }
        if ((g = memo.grid) == null) {
            return -1;
        }
        return g[(x & 0xF) + (z & 0xF) * 16];
    }

    static {
        MEMO = ThreadLocal.withInitial(Memo::new);
    }

    private static final class Memo {
        long key = Long.MIN_VALUE;
        int version = -1;
        byte[] grid;

        private Memo() {
        }
    }
}

