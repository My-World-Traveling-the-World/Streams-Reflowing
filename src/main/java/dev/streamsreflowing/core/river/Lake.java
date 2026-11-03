/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.math.Hashing;

public final class Lake {
    private final int a;
    private final int b;
    private final int c;
    private final int d;
    private final int e;
    private final int f;
    private final int g;
    private final long[] h;
    private static final double i = 4.0;
    private static final int j = 16;

    public Lake(int n, int n2, int n3, int n4, int n5, int n6, int n7, long[] lArray) {
        this.a = n;
        this.b = n2;
        this.c = n3;
        this.d = n4;
        this.e = n5;
        this.f = n6;
        this.g = n7;
        this.h = lArray;
    }

    public int level() {
        return this.a;
    }

    public int digDepth() {
        return this.b;
    }

    public boolean covers(int n, int n2) {
        int n3 = Math.floorDiv(n - this.d + (this.c >> 1), this.c);
        int n4 = Math.floorDiv(n2 - this.e + (this.c >> 1), this.c);
        if (n3 < 0 || n3 >= this.f || n4 < 0 || n4 >= this.g) {
            return false;
        }
        int n5 = n4 * this.f + n3;
        return (this.h[n5 >>> 6] & 1L << (n5 & 0x3F)) != 0L;
    }

    public boolean coversWithin(int n, int n2, int n3) {
        int n4 = Math.floorDiv(n - this.d + (this.c >> 1), this.c);
        int n5 = Math.floorDiv(n2 - this.e + (this.c >> 1), this.c);
        for (int i = Math.max(0, n5 - n3); i <= Math.min(this.g - 1, n5 + n3); ++i) {
            for (int j = Math.max(0, n4 - n3); j <= Math.min(this.f - 1, n4 + n3); ++j) {
                int n6 = i * this.f + j;
                if ((this.h[n6 >>> 6] & 1L << (n6 & 0x3F)) == 0L) continue;
                return true;
            }
        }
        return false;
    }

    public double coverageSmooth(int n, int n2) {
        long l = (long)this.d * 341873128712L ^ (long)this.e * 132897987541L;
        double d = (double)n + (Lake.a(l, n, n2, 3) - 0.5) * 2.0 * 4.0;
        double d2 = (double)n2 + (Lake.a(l, n, n2, 4) - 0.5) * 2.0 * 4.0;
        double d3 = (d - (double)this.d) / (double)this.c;
        double d4 = (d2 - (double)this.e) / (double)this.c;
        int n3 = (int)Math.floor(d3);
        int n4 = (int)Math.floor(d4);
        double d5 = d3 - (double)n3;
        double d6 = d4 - (double)n4;
        double d7 = this.a(n3, n4);
        double d8 = this.a(n3 + 1, n4);
        double d9 = this.a(n3, n4 + 1);
        double d10 = this.a(n3 + 1, n4 + 1);
        double d11 = d7 * (1.0 - d5) + d8 * d5;
        double d12 = d9 * (1.0 - d5) + d10 * d5;
        return d11 * (1.0 - d6) + d12 * d6;
    }

    private static double a(long l, int n, int n2, int n3) {
        int n4 = Math.floorDiv(n, 16);
        int n5 = Math.floorDiv(n2, 16);
        double d = ((double)n - (double)n4 * 16.0) / 16.0;
        double d2 = ((double)n2 - (double)n5 * 16.0) / 16.0;
        d = d * d * (3.0 - 2.0 * d);
        d2 = d2 * d2 * (3.0 - 2.0 * d2);
        double d3 = Hashing.unit(l, n4, n5, n3);
        double d4 = Hashing.unit(l, n4 + 1, n5, n3);
        double d5 = Hashing.unit(l, n4, n5 + 1, n3);
        double d6 = Hashing.unit(l, n4 + 1, n5 + 1, n3);
        double d7 = d3 + (d4 - d3) * d;
        double d8 = d5 + (d6 - d5) * d;
        return d7 + (d8 - d7) * d2;
    }

    private double a(int n, int n2) {
        if (n < 0 || n >= this.f || n2 < 0 || n2 >= this.g) {
            return 0.0;
        }
        int n3 = n2 * this.f + n;
        return (this.h[n3 >>> 6] & 1L << (n3 & 0x3F)) != 0L ? 1.0 : 0.0;
    }

    public int spacing() {
        return this.c;
    }

    public int cellMinX() {
        return this.d;
    }

    public int cellMinZ() {
        return this.e;
    }

    public int cellsWide() {
        return this.f;
    }

    public int cellsHigh() {
        return this.g;
    }

    public long[] mask() {
        return this.h;
    }
}

