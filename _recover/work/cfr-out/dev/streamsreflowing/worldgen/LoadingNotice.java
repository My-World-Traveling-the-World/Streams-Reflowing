/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.RiverEngine;

public final class LoadingNotice {
    private static final double a = 20.0;
    private static final long b = 3000000000L;
    private static final long c = 1000000000L;
    private static final long d = 1200000000L;
    private static long e;
    private static long f;
    private static long g;
    private static long h;

    private LoadingNotice() {
    }

    public static synchronized String currentText() {
        long l = System.nanoTime();
        if (l - e > 3000000000L) {
            f = 0L;
            h = 0L;
        }
        e = l;
        boolean bl = RiverEngine.buildingNetworks();
        if (f == 0L) {
            if (!bl) {
                return null;
            }
            f = l;
            g = l;
        }
        if (bl) {
            g = l;
        }
        if (h == 0L && !bl && l - g > 1000000000L) {
            h = l;
        }
        if (h == 0L) {
            double d = (double)(l - f) / 1.0E9;
            int n = (int)Math.min(99L, Math.round(99.0 * (1.0 - Math.exp(-d / 20.0))));
            return "Streams Reflowing is weaving your waterways... [" + n + "%]";
        }
        if (l - h < 1200000000L) {
            return "Streams Reflowing is weaving your waterways... [100%]";
        }
        return "Streams Reflowing has woven your waterways, generating world...";
    }
}

