/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.diag;

import java.util.concurrent.atomic.LongAdder;

public final class SampleTimer {
    public static final boolean ON = Boolean.getBoolean("streamsreflowing.sampletime");
    private static final LongAdder a = new LongAdder();
    private static final LongAdder b = new LongAdder();
    private static final LongAdder c = new LongAdder();
    private static final LongAdder d = new LongAdder();
    private static final LongAdder e = new LongAdder();

    private SampleTimer() {
    }

    public static long start() {
        return ON ? System.nanoTime() : 0L;
    }

    public static void height(long l) {
        if (ON) {
            a.add(System.nanoTime() - l);
            b.increment();
        }
    }

    public static void biome(long l) {
        if (ON) {
            c.add(System.nanoTime() - l);
            d.increment();
        }
    }

    public static void plateBuilt() {
        if (ON) {
            e.increment();
        }
    }

    public static String report() {
        long l = a.sum();
        long l2 = b.sum();
        long l3 = c.sum();
        long l4 = d.sum();
        double d = (double)l + (double)l3;
        return String.format("plates %,d | heights %,d calls / %,.1f ms cpu (%.2f us each) | biomes %,d calls / %,.1f ms cpu (%.2f us each) | biome share %.1f%%", e.sum(), l2, (double)l / 1000000.0, l2 == 0L ? 0.0 : (double)l / 1000.0 / (double)l2, l4, (double)l3 / 1000000.0, l4 == 0L ? 0.0 : (double)l3 / 1000.0 / (double)l4, d == 0.0 ? 0.0 : 100.0 * (double)l3 / d);
    }
}

