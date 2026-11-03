/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FormEval;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class FormSampler
implements TerrainSampler {
    private final TerrainSampler a;
    private final FieldForm b;
    private final int c;
    private final int d;
    private final int e;
    private final int f;
    private final double g;
    private final int h;
    private final ConcurrentHashMap<Thread, Work> i = new ConcurrentHashMap();
    private static final int j = 0;
    private volatile boolean k;
    public static final LongAdder handedBack = new LongAdder();
    public static final LongAdder keptIr = new LongAdder();

    public FormSampler(TerrainSampler terrainSampler, FieldForm fieldForm, int n, int n2, int n3, int n4, double d, int n5) {
        this.a = terrainSampler;
        this.b = fieldForm;
        this.c = n;
        this.d = n2;
        this.e = n3;
        this.f = Math.max(1, n4);
        this.g = d;
        this.h = n5;
    }

    @Override
    public double height(int n, int n2) {
        Work work = this.i.computeIfAbsent(Thread.currentThread(), thread -> new Work(this.b, (this.d - this.c) / this.e + 1));
        if (this.k && work.a.compute(n, this.h + 0, n2) <= this.g) {
            handedBack.increment();
            return this.a.height(n, n2);
        }
        keptIr.increment();
        return work.a.surfaceHeight(n, n2, this.c, this.d, this.e, this.f, this.g, work.b);
    }

    public void setAquiferHandBack(boolean bl) {
        this.k = bl;
        handedBack.reset();
        keptIr.reset();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public double heightWithSemantics(int n, int n2, boolean bl, boolean bl2) {
        Work work = this.i.computeIfAbsent(Thread.currentThread(), thread -> new Work(this.b, (this.d - this.c) / this.e + 1));
        boolean bl3 = work.a.d;
        boolean bl4 = work.a.e;
        try {
            work.a.d = bl;
            work.a.e = bl2;
            double d = work.a.surfaceHeight(n, n2, this.c, this.d, this.e, this.f, this.g, work.b);
            return d;
        }
        finally {
            work.a.d = bl3;
            work.a.e = bl4;
        }
    }

    public double density(int n, int n2, int n3) {
        Work work = this.i.computeIfAbsent(Thread.currentThread(), thread -> new Work(this.b, (this.d - this.c) / this.e + 1));
        return work.a.compute(n, n2, n3);
    }

    public double solidThreshold() {
        return this.g;
    }

    @Override
    public double surfaceHeight(int n, int n2) {
        return this.a.surfaceHeight(n, n2);
    }

    @Override
    public double groundHeight(int n, int n2) {
        return this.a.groundHeight(n, n2);
    }

    @Override
    public double groundHeightExact(int n, int n2) {
        return this.a.groundHeightExact(n, n2);
    }

    @Override
    public boolean hasEstimate() {
        return this.a.hasEstimate();
    }

    @Override
    public boolean useHybridGridFill() {
        return this.a.useHybridGridFill();
    }

    @Override
    public double estimate(int n, int n2) {
        return this.a.estimate(n, n2);
    }

    @Override
    public boolean bakedCalibration() {
        return this.a.bakedCalibration();
    }

    @Override
    public boolean openAt(int n, int n2, int n3) {
        return this.a.openAt(n, n2, n3);
    }

    private static final class Work {
        final FormEval a;
        final double[] b;

        Work(FieldForm fieldForm, int n) {
            this.a = new FormEval(fieldForm);
            this.b = new double[n];
        }
    }
}

