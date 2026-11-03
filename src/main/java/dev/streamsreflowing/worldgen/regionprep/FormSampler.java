/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FormEval;
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
    // A38: this field used to be a Thread-keyed map standing in for a thread-local. Nothing ever
    // removed the entries, so every sampler thread that retired parked its Work (FormEval plus a
    // per-thread shape array) in that map for the life of the JVM. A real ThreadLocal is dropped with
    // its thread, and its initial value is exactly what the old lookup built.
    //
    // Assigned in the constructor, not in a field initializer: a field initializer runs before the
    // constructor body, so a lambda there cannot read the final fields b/c/d/e (javac: "might not
    // have been initialized"). The constructor also captures the two values as locals instead of
    // through `this`, so the lambda holds no reference to the sampler at all.
    private final ThreadLocal<Work> i;
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
        // Same expression the old computeIfAbsent lambda built, evaluated once here with constructor
        // parameters as the captured values: new Work(fieldForm, (n2 - n) / n3 + 1).
        final FieldForm form = fieldForm;
        final int workSize = (n2 - n) / n3 + 1;
        this.i = ThreadLocal.withInitial(() -> new Work(form, workSize));
    }

    @Override
    public double height(int n, int n2) {
        Work work = this.i.get();
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
        Work work = this.i.get();
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
        Work work = this.i.get();
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

