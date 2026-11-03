/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.flow;

import java.util.Iterator;
import java.util.LinkedHashSet;

public final class FlowRemeshQueue {
    private static final int PER_TICK = 16;
    private static final LinkedHashSet<Long> PENDING = new LinkedHashSet();

    private FlowRemeshQueue() {
    }

    public static synchronized void enqueue(int cx, int cz) {
        PENDING.add((long)cx << 32 ^ (long)cz & 0xFFFFFFFFL);
    }

    public static synchronized void clear() {
        PENDING.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void drain(ChunkDirty mark) {
        Class<FlowRemeshQueue> clazz = FlowRemeshQueue.class;
        synchronized (FlowRemeshQueue.class) {
            if (PENDING.isEmpty()) {
                // ** MonitorExit[var2_1] (shouldn't be in output)
                return;
            }
            int n = Math.min(16, PENDING.size());
            long[] batch = new long[n];
            Iterator it = PENDING.iterator();
            for (int i = 0; i < n; ++i) {
                batch[i] = (Long)it.next();
                it.remove();
            }
            // ** MonitorExit[var2_1] (shouldn't be in output)
            for (long k : batch) {
                mark.mark((int)(k >> 32), (int)k);
            }
            return;
        }
    }

    public static interface ChunkDirty {
        public void mark(int var1, int var2);
    }
}

