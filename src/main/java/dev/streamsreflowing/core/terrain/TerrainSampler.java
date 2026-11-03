/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.terrain;

@FunctionalInterface
public interface TerrainSampler {
    public static final int CLASS_LAND = 0;
    public static final int CLASS_RIVER = 1;
    public static final int CLASS_OCEAN = 2;

    default public int waterBiomeClass(int n, int n2) {
        return 0;
    }

    default public int waterBiomeClassAt(int n, int n2, int n3) {
        return this.waterBiomeClass(n, n3);
    }

    default public boolean outletBiomeOk(int n, int n2, int n3) {
        return true;
    }

    default public double riverSurface(int n, int n2) {
        return Double.NaN;
    }

    public double height(int var1, int var2);

    default public double surfaceHeight(int n, int n2) {
        return this.height(n, n2);
    }

    default public double groundHeight(int n, int n2) {
        return this.height(n, n2);
    }

    default public boolean hasEstimate() {
        return false;
    }

    default public boolean useHybridGridFill() {
        return false;
    }

    default public double estimate(int n, int n2) {
        return this.height(n, n2);
    }

    default public double groundHeightExact(int n, int n2) {
        return this.groundHeight(n, n2);
    }

    default public boolean bakedCalibration() {
        return false;
    }

    default public boolean batchCapable() {
        return false;
    }

    default public void heightsBatch(int[] nArray, int[] nArray2, double[] dArray, int n) {
        for (int i = 0; i < n; ++i) {
            dArray[i] = this.height(nArray[i], nArray2[i]);
        }
    }

    default public boolean openAt(int n, int n2, int n3) {
        return (double)n2 > this.height(n, n3);
    }
}

