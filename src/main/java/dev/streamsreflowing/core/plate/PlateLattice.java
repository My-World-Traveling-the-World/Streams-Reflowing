/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.plate;

import dev.streamsreflowing.core.math.Hashing;
import dev.streamsreflowing.core.plate.PlateKey;

public final class PlateLattice {
    private final long a;
    private final double b;
    private final double c;
    private static final int d = 1023;
    private final PlateKey[] e = new PlateKey[1024];
    private final ThreadLocal<Seeds> f = ThreadLocal.withInitial(Seeds::new);

    public PlateLattice(long l, double d, double d2) {
        this.a = l;
        this.b = d;
        this.c = Math.min(0.49, Math.max(0.0, d2));
    }

    private static final class Seeds {
        int a = Integer.MIN_VALUE;
        int b = Integer.MIN_VALUE;
        final double[] c = new double[9];
        final double[] d = new double[9];
    }

    private Seeds seeds(int n, int n2) {
        Seeds seeds = this.f.get();
        if (seeds.a != n || seeds.b != n2) {
            int n3 = 0;
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    seeds.c[n3] = this.seedX(n + j, n2 + i);
                    seeds.d[n3] = this.seedZ(n + j, n2 + i);
                    ++n3;
                }
            }
            seeds.a = n;
            seeds.b = n2;
        }
        return seeds;
    }

    public double scale() {
        return this.b;
    }

    public double seedX(int n, int n2) {
        return ((double)n + 0.5 + (Hashing.unit(this.a, n, n2, 1L) - 0.5) * 2.0 * this.c) * this.b;
    }

    public double seedZ(int n, int n2) {
        return ((double)n2 + 0.5 + (Hashing.unit(this.a, n, n2, 2L) - 0.5) * 2.0 * this.c) * this.b;
    }

    public PlateKey plateAt(double d, double d2) {
        int n = (int)Math.floor(d / this.b);
        int n2 = (int)Math.floor(d2 / this.b);
        Seeds seeds = this.seeds(n, n2);
        double d3 = Double.MAX_VALUE;
        int n3 = n;
        int n4 = n2;
        int n7 = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                double d4;
                int n5 = n + j;
                int n6 = n2 + i;
                double d5 = seeds.c[n7] - d;
                double d6 = d5 * d5 + (d4 = seeds.d[n7] - d2) * d4;
                ++n7;
                if (!(d6 < d3) && (d6 != d3 || n5 >= n3 && (n5 != n3 || n6 >= n4))) continue;
                d3 = d6;
                n3 = n5;
                n4 = n6;
            }
        }
        return new PlateKey(n3, n4);
    }

    public PlateKey[] nearestTwo(double d, double d2) {
        PlateKey plateKey;
        int n = (int)Math.floor(d / this.b);
        int n2 = (int)Math.floor(d2 / this.b);
        Seeds seeds = this.seeds(n, n2);
        double d3 = Double.MAX_VALUE;
        double d4 = Double.MAX_VALUE;
        PlateKey plateKey2 = plateKey = new PlateKey(n, n2);
        int n5 = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                double d5;
                int n3 = n + j;
                int n4 = n2 + i;
                double d6 = seeds.c[n5] - d;
                double d7 = d6 * d6 + (d5 = seeds.d[n5] - d2) * d5;
                ++n5;
                if (d7 < d3) {
                    d4 = d3;
                    plateKey2 = plateKey;
                    d3 = d7;
                    plateKey = new PlateKey(n3, n4);
                    continue;
                }
                if (!(d7 < d4)) continue;
                d4 = d7;
                plateKey2 = new PlateKey(n3, n4);
            }
        }
        return new PlateKey[]{plateKey, plateKey2};
    }

    public PlateKey[] platesWithin(double d, double d2, double d3) {
        int n = (int)Math.floor(d / this.b);
        int n2 = (int)Math.floor(d2 / this.b);
        Seeds seeds = this.seeds(n, n2);
        double[] dArray = new double[9];
        double d4 = Double.MAX_VALUE;
        int n3 = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                double d5 = seeds.c[n3] - d;
                double d6 = seeds.d[n3] - d2;
                double d7 = Math.sqrt(d5 * d5 + d6 * d6);
                dArray[n3++] = d7;
                if (!(d7 < d4)) continue;
                d4 = d7;
            }
        }
        double d8 = d4 + 2.0 * d3;
        int n4 = 0;
        for (int i = 0; i < 9; ++i) {
            if (!(dArray[i] <= d8)) continue;
            ++n4;
        }
        PlateKey[] plateKeyArray = new PlateKey[n4];
        int n5 = 0;
        n3 = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                int n6 = n3++;
                if (!(dArray[n6] <= d8)) continue;
                plateKeyArray[n5++] = this.a(n + j, n2 + i);
            }
        }
        return plateKeyArray;
    }

    private PlateKey a(int n, int n2) {
        PlateKey plateKey;
        int n3 = (n * -1640531535 ^ n2 * -2048144777) & 0x3FF;
        PlateKey plateKey2 = this.e[n3];
        if (plateKey2 != null && plateKey2.cx() == n && plateKey2.cz() == n2) {
            return plateKey2;
        }
        this.e[n3] = plateKey = new PlateKey(n, n2);
        return plateKey;
    }

    public double borderDistance(double d, double d2) {
        int n = (int)Math.floor(d / this.b);
        int n2 = (int)Math.floor(d2 / this.b);
        Seeds seeds = this.seeds(n, n2);
        double d3 = Double.MAX_VALUE;
        double d4 = Double.MAX_VALUE;
        int n3 = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                double d5;
                double d6 = seeds.c[n3] - d;
                double d7 = Math.sqrt(d6 * d6 + (d5 = seeds.d[n3] - d2) * d5);
                ++n3;
                if (d7 < d3) {
                    d4 = d3;
                    d3 = d7;
                    continue;
                }
                if (!(d7 < d4)) continue;
                d4 = d7;
            }
        }
        return 0.5 * (d4 - d3);
    }
}

