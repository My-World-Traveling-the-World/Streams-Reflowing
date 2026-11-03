/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.core.ColumnKey;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class CachingTerrainSampler
implements TerrainSampler {
    private final TerrainSampler a;
    private final int b;
    private final double c;
    private final ConcurrentHashMap<Long, Double> d = new ConcurrentHashMap();
    private final ConcurrentLinkedQueue<Long> e = new ConcurrentLinkedQueue();
    private final ConcurrentHashMap<Long, Double> f = new ConcurrentHashMap();
    private final ConcurrentLinkedQueue<Long> g = new ConcurrentLinkedQueue();
    private final AtomicLong h = new AtomicLong();
    private final AtomicLong i = new AtomicLong();
    private final ConcurrentHashMap<Long, Double> j = new ConcurrentHashMap();
    private final ConcurrentLinkedQueue<Long> k = new ConcurrentLinkedQueue();
    private final ConcurrentHashMap<Long, Double> l = new ConcurrentHashMap();
    private final ConcurrentLinkedQueue<Long> m = new ConcurrentLinkedQueue();

    private static long a(int n, int n2) {
        return ColumnKey.of(n, n2);
    }

    public CachingTerrainSampler(TerrainSampler terrainSampler, int n) {
        this(terrainSampler, n, Double.NaN);
    }

    public CachingTerrainSampler(TerrainSampler terrainSampler, int n, double d) {
        this.a = terrainSampler;
        this.b = n;
        this.c = d;
    }

    @Override
    public int waterBiomeClass(int n, int n2) {
        return this.a.waterBiomeClass(n, n2);
    }

    @Override
    public int waterBiomeClassAt(int n, int n2, int n3) {
        return this.a.waterBiomeClassAt(n, n2, n3);
    }

    @Override
    public double riverSurface(int n, int n2) {
        return this.a.riverSurface(n, n2);
    }

    @Override
    public boolean outletBiomeOk(int n, int n2, int n3) {
        return this.a.outletBiomeOk(n, n2, n3);
    }

    @Override
    public double height(int n, int n2) {
        Long l;
        long l2 = CachingTerrainSampler.a(n, n2);
        Double d = this.d.get(l2);
        if (d != null) {
            this.h.incrementAndGet();
            return d;
        }
        double d2 = this.a.height(n, n2);
        if (d2 == this.c) {
            return d2;
        }
        Double d3 = this.d.putIfAbsent(l2, d2);
        if (d3 != null) {
            this.h.incrementAndGet();
            return d3;
        }
        this.i.incrementAndGet();
        this.e.offer(l2);
        while (this.d.size() > this.b && (l = this.e.poll()) != null) {
            this.d.remove(l);
        }
        return d2;
    }

    @Override
    public double surfaceHeight(int n, int n2) {
        Long l;
        long l2 = CachingTerrainSampler.a(n, n2);
        Double d = this.f.get(l2);
        if (d != null) {
            this.h.incrementAndGet();
            return d;
        }
        double d2 = this.a.surfaceHeight(n, n2);
        Double d3 = this.f.putIfAbsent(l2, d2);
        if (d3 != null) {
            this.h.incrementAndGet();
            return d3;
        }
        this.i.incrementAndGet();
        this.g.offer(l2);
        while (this.f.size() > this.b && (l = this.g.poll()) != null) {
            this.f.remove(l);
        }
        return d2;
    }

    @Override
    public double groundHeight(int n, int n2) {
        Long l;
        long l2 = CachingTerrainSampler.a(n, n2);
        Double d = this.j.get(l2);
        if (d != null) {
            this.h.incrementAndGet();
            return d;
        }
        double d2 = this.a.groundHeight(n, n2);
        if (d2 == this.c) {
            return d2;
        }
        Double d3 = this.j.putIfAbsent(l2, d2);
        if (d3 != null) {
            this.h.incrementAndGet();
            return d3;
        }
        this.i.incrementAndGet();
        this.k.offer(l2);
        while (this.j.size() > this.b && (l = this.k.poll()) != null) {
            this.j.remove(l);
        }
        return d2;
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
    public boolean openAt(int n, int n2, int n3) {
        return this.a.openAt(n, n2, n3);
    }

    @Override
    public double groundHeightExact(int n, int n2) {
        Long l;
        long l2 = CachingTerrainSampler.a(n, n2);
        Double d = this.l.get(l2);
        if (d != null) {
            this.h.incrementAndGet();
            return d;
        }
        double d2 = this.a.groundHeightExact(n, n2);
        if (d2 == this.c) {
            return d2;
        }
        Double d3 = this.l.putIfAbsent(l2, d2);
        if (d3 != null) {
            this.h.incrementAndGet();
            return d3;
        }
        this.i.incrementAndGet();
        this.m.offer(l2);
        while (this.l.size() > this.b && (l = this.m.poll()) != null) {
            this.l.remove(l);
        }
        return d2;
    }

    @Override
    public boolean bakedCalibration() {
        return this.a.bakedCalibration();
    }

    public long hits() {
        return this.h.get();
    }

    public long misses() {
        return this.i.get();
    }

    public int hitPercent() {
        long l = this.h.get();
        long l2 = l + this.i.get();
        return l2 > 0L ? (int)(l * 100L / l2) : 0;
    }
}

