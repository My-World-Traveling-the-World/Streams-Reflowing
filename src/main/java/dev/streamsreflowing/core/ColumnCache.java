/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core;

public final class ColumnCache {
    private long[] a;
    private double[] b;
    private boolean[] c;
    private int d;
    private int e;
    private int f;

    public ColumnCache() {
        this(1024);
    }

    public ColumnCache(int n) {
        int n2 = Integer.highestOneBit(Math.max(16, n - 1)) << 1;
        this.a = new long[n2];
        this.b = new double[n2];
        this.c = new boolean[n2];
        this.d = n2 - 1;
        this.f = n2 >>> 1;
    }

    private int a(long l) {
        int n = (int)l & this.d;
        while (this.c[n] && this.a[n] != l) {
            n = n + 1 & this.d;
        }
        return n;
    }

    public double get(long l, double d) {
        int n = this.a(l);
        return this.c[n] ? this.b[n] : d;
    }

    public void put(long l, double d) {
        int n = this.a(l);
        if (!this.c[n]) {
            this.c[n] = true;
            this.a[n] = l;
            ++this.e;
            if (this.e > this.f) {
                this.a();
                n = this.a(l);
            }
        }
        this.b[n] = d;
    }

    private void a() {
        long[] lArray = this.a;
        double[] dArray = this.b;
        boolean[] blArray = this.c;
        int n = lArray.length << 1;
        this.a = new long[n];
        this.b = new double[n];
        this.c = new boolean[n];
        this.d = n - 1;
        this.f = n >>> 1;
        for (int i = 0; i < lArray.length; ++i) {
            if (!blArray[i]) continue;
            int n2 = this.a(lArray[i]);
            this.c[n2] = true;
            this.a[n2] = lArray[i];
            this.b[n2] = dArray[i];
        }
    }

    public int size() {
        return this.e;
    }
}

