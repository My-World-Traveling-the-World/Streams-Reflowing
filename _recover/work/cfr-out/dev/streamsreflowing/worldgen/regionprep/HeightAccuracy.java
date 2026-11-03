/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

public final class HeightAccuracy {
    public static final double MAX_ABOVE = 3.0;
    public static final double MAX_BELOW = 6.0;
    public static final int SAMPLE_COLUMNS = 1000;

    private HeightAccuracy() {
    }

    public static boolean inWindow(double d) {
        return !Double.isNaN(d) && d <= 3.0 && d >= -6.0;
    }

    public static String describe(int n, int n2, double d, double d2) {
        return n + "/" + n2 + " columns outside +3.0/-6.0 (highest " + d + ", lowest " + d2 + ")";
    }
}

