/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.diag.SampleTimer;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

public final class RiverEngine {
    public static final double OCEAN_SKIP_HEIGHT = -1000.0;
    public static final double RIVER_SKIP_HEIGHT = -1001.0;
    private static final double a = 3.0;
    private static final double b = 1.5;
    private static final double c = 2.0;
    private static final double d = 0.5;
    private final long e;
    private final StreamsConfig f;
    private final TerrainSampler g;
    private final WetnessSampler h;
    private final PlateLattice i;
    private final RiverPlateNetworks plates;
    private final double m;
    private final double n;
    private final ThreadLocal<long[]> o = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0L});
    private final ThreadLocal<double[]> r = ThreadLocal.withInitial(() -> new double[2]);
    private volatile BuildListener t;
    private static final AtomicInteger v = new AtomicInteger();
    private static volatile long w = System.nanoTime() - 60000000000L;
    private static final int x = 4;
    private static final int y = 1024;
    private static final int z = 30;
    private static final double A = 0.9375;
    private static final double B = 2.0;
    private static final double C = 6.0;
    private static final double D = 6.0;
    private static final double E = 16.0;
    private static final int F = 6;
    private static final double G = 0.1;
    private static final double H = 0.25;
    private volatile int I;
    private final Object J = new Object();
    private boolean K;
    private volatile IntConsumer L;
    private volatile boolean M;
    private static final int N = 8192;
    private final ThreadLocal<long[]> O = ThreadLocal.withInitial(() -> {
        long[] lArray = new long[8192];
        Arrays.fill(lArray, Long.MIN_VALUE);
        return lArray;
    });
    private final ThreadLocal<ColumnPlan[]> P = ThreadLocal.withInitial(() -> new ColumnPlan[8192]);
    private final ThreadLocal<long[]> Q = ThreadLocal.withInitial(() -> {
        long[] lArray = new long[8192];
        Arrays.fill(lArray, Long.MIN_VALUE);
        return lArray;
    });
    private final ThreadLocal<ColumnPlan[]> R = ThreadLocal.withInitial(() -> new ColumnPlan[8192]);
    private final ThreadLocal<RiverNetwork[]> S = ThreadLocal.withInitial(() -> new RiverNetwork[8192]);
    private final ThreadLocal<int[]> T = ThreadLocal.withInitial(() -> new int[8192]);
    private final ThreadLocal<RiverNetwork[]> U = ThreadLocal.withInitial(() -> new RiverNetwork[1]);
    private final ThreadLocal<int[]> V = ThreadLocal.withInitial(() -> new int[]{-1});
    private final ThreadLocal<long[]> W = ThreadLocal.withInitial(() -> {
        long[] lArray = new long[8192];
        Arrays.fill(lArray, Long.MIN_VALUE);
        return lArray;
    });
    private final ThreadLocal<ColumnPlan[]> X = ThreadLocal.withInitial(() -> new ColumnPlan[8192]);
    private volatile boolean Y = true;
    private static final int Z = 6;
    public static final AtomicInteger LEAN_CALLS = new AtomicInteger();
    private static final int aa = 4096;
    private final ThreadLocal<long[]> ab = ThreadLocal.withInitial(() -> {
        long[] lArray = new long[4096];
        Arrays.fill(lArray, Long.MIN_VALUE);
        return lArray;
    });
    private final ThreadLocal<long[]> ac = ThreadLocal.withInitial(() -> new long[4096]);
    private final ThreadLocal<long[]> ad = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0L, 0L});

    public static int insetFor(double d, StreamsConfig streamsConfig) {
        double d2 = 0.5 + 0.5 * Math.min(1.0, d / 3.0);
        return (int)Math.round(streamsConfig.ditchInset() * d2);
    }

    public RiverEngine(long l, StreamsConfig streamsConfig, TerrainSampler terrainSampler) {
        this(l, streamsConfig, terrainSampler, (n, n2, n3) -> 1.0);
    }

    public RiverEngine(long l, StreamsConfig streamsConfig, TerrainSampler terrainSampler, WetnessSampler wetnessSampler) {
        this.e = l;
        this.f = streamsConfig;
        this.g = terrainSampler;
        this.h = wetnessSampler;
        this.i = new PlateLattice(l, streamsConfig.plateScale(), streamsConfig.plateJitter());
        // 连接后的河网仍依赖基础板；缓存随其所属的世界关闭。
        this.plates = new RiverPlateNetworks(streamsConfig.connectNearbyWater(), this::b,
                key -> NetworkTracer.connectNetwork(key, this::baseFor, this.i, this.g, this.f, this.e));
        this.m = streamsConfig.maxWidth() * (1.0 + streamsConfig.bankWidth()) + 2.0;
        this.n = streamsConfig.connectNearbyWater() ? Math.max(this.m, streamsConfig.connectRadius()) : this.m;
    }

    public StreamsConfig config() {
        return this.f;
    }

    public double sampledHeight(int n, int n2) {
        return this.g.height(n, n2);
    }

    public double exactHeight(int n, int n2) {
        return this.g.groundHeightExact(n, n2);
    }

    public PlateKey plateAt(int n, int n2) {
        return this.i.plateAt((double)n + 0.5, (double)n2 + 0.5);
    }

    public void setBuildListener(BuildListener buildListener) {
        this.t = buildListener;
    }

    public void setNetworkStore(NetworkStore networkStore) { plates.setStore(networkStore); }

    public RiverNetwork networkFor(PlateKey key) { return plates.networkFor(key); }

    public RiverNetwork baseFor(PlateKey key) { return plates.baseFor(key); }

    public boolean isClosed() { return plates.isClosed(); }

    public void close() { plates.close(); }

    public static boolean buildingNetworks() {
        return v.get() > 0 || System.nanoTime() - w < 700000000L;
    }

    public static int activeBuildCount() {
        return v.get();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RiverNetwork b(PlateKey plateKey) {
        v.incrementAndGet();
        PlateDomain plateDomain = new PlateDomain(this.i, plateKey);
        LowResGrid.enterBuildFocus((plateDomain.minX + plateDomain.maxX) / 2, (plateDomain.minZ + plateDomain.maxZ) / 2);
        try {
            RiverNetwork riverNetwork = this.c(plateKey);
            return riverNetwork;
        }
        finally {
            LowResGrid.exitBuildFocus();
            v.decrementAndGet();
            w = System.nanoTime();
        }
    }

    private RiverNetwork c(PlateKey plateKey) {
        final TerrainSampler terrainSampler = this.d(plateKey);
        BuildListener buildListener = this.t;
        if (buildListener == null) {
            return NetworkTracer.build(new PlateDomain(this.i, plateKey), terrainSampler, this.h, this.f, this.e);
        }
        final int[] nArray = new int[]{0};
        final int[] nArray2 = new int[]{0};
        TerrainSampler terrainSampler2 = new TerrainSampler(){

            @Override
            public double height(int n, int n2) {
                long l = SampleTimer.start();
                double d = terrainSampler.height(n, n2);
                SampleTimer.height(l);
                if (d == -1000.0) {
                    nArray2[0] = nArray2[0] + 1;
                } else {
                    nArray[0] = nArray[0] + 1;
                }
                return d;
            }

            @Override
            public double surfaceHeight(int n, int n2) {
                nArray[0] = nArray[0] + 1;
                long l = SampleTimer.start();
                double d = terrainSampler.surfaceHeight(n, n2);
                SampleTimer.height(l);
                return d;
            }

            @Override
            public double groundHeight(int n, int n2) {
                long l = SampleTimer.start();
                double d = terrainSampler.groundHeight(n, n2);
                SampleTimer.height(l);
                if (d == -1000.0) {
                    nArray2[0] = nArray2[0] + 1;
                } else {
                    nArray[0] = nArray[0] + 1;
                }
                return d;
            }

            @Override
            public double groundHeightExact(int n, int n2) {
                long l = SampleTimer.start();
                double d = terrainSampler.groundHeightExact(n, n2);
                SampleTimer.height(l);
                if (d == -1000.0) {
                    nArray2[0] = nArray2[0] + 1;
                } else {
                    nArray[0] = nArray[0] + 1;
                }
                return d;
            }

            @Override
            public boolean bakedCalibration() {
                return terrainSampler.bakedCalibration();
            }

            @Override
            public boolean hasEstimate() {
                return terrainSampler.hasEstimate();
            }

            @Override
            public boolean useHybridGridFill() {
                return terrainSampler.useHybridGridFill();
            }

            @Override
            public double estimate(int n, int n2) {
                return terrainSampler.estimate(n, n2);
            }

            @Override
            public boolean openAt(int n, int n2, int n3) {
                return terrainSampler.openAt(n, n2, n3);
            }

            @Override
            public int waterBiomeClass(int n, int n2) {
                long l = SampleTimer.start();
                int n3 = terrainSampler.waterBiomeClass(n, n2);
                SampleTimer.biome(l);
                return n3;
            }

            @Override
            public int waterBiomeClassAt(int n, int n2, int n3) {
                long l = SampleTimer.start();
                int n4 = terrainSampler.waterBiomeClassAt(n, n2, n3);
                SampleTimer.biome(l);
                return n4;
            }

            @Override
            public double riverSurface(int n, int n2) {
                return terrainSampler.riverSurface(n, n2);
            }

            @Override
            public boolean outletBiomeOk(int n, int n2, int n3) {
                long l = SampleTimer.start();
                boolean bl = terrainSampler.outletBiomeOk(n, n2, n3);
                SampleTimer.biome(l);
                return bl;
            }
        };
        long l = System.nanoTime();
        RiverNetwork riverNetwork = NetworkTracer.build(new PlateDomain(this.i, plateKey), terrainSampler2, this.h, this.f, this.e);
        double d = (double)(System.nanoTime() - l) / 1000000.0;
        SampleTimer.plateBuilt();
        if (SampleTimer.ON) {
            Constants.LOG.info("SAMPLETIME plate {} wall {} ms | {}", plateKey, String.format("%,.1f", d), SampleTimer.report());
        }
        buildListener.onBuilt(plateKey, nArray[0], nArray2[0], d, riverNetwork.segmentCount());
        return riverNetwork;
    }

    private static long a(int n, int n2) {
        return (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
    }

    private static int a(double d) {
        return (int)(Math.floor(d / 16.0) * 16.0);
    }

    public void setEstCostPersist(IntConsumer intConsumer) {
        this.L = intConsumer;
    }

    public void setEstCostVerdict(int n) {
        this.I = n;
        if (n == 2) {
            this.M = true;
        }
    }

    private static TerrainSampler a(final TerrainSampler terrainSampler) {
        return new TerrainSampler(){

            @Override
            public int waterBiomeClass(int n, int n2) {
                return terrainSampler.waterBiomeClass(n, n2);
            }

            @Override
            public int waterBiomeClassAt(int n, int n2, int n3) {
                return terrainSampler.waterBiomeClassAt(n, n2, n3);
            }

            @Override
            public double riverSurface(int n, int n2) {
                return terrainSampler.riverSurface(n, n2);
            }

            @Override
            public boolean outletBiomeOk(int n, int n2, int n3) {
                return terrainSampler.outletBiomeOk(n, n2, n3);
            }

            @Override
            public double height(int n, int n2) {
                return terrainSampler.groundHeightExact(n, n2);
            }

            @Override
            public double groundHeight(int n, int n2) {
                return terrainSampler.groundHeightExact(n, n2);
            }

            @Override
            public double groundHeightExact(int n, int n2) {
                return terrainSampler.groundHeightExact(n, n2);
            }

            @Override
            public double surfaceHeight(int n, int n2) {
                return terrainSampler.surfaceHeight(n, n2);
            }

            @Override
            public boolean openAt(int n, int n2, int n3) {
                return terrainSampler.openAt(n, n2, n3);
            }
        };
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private TerrainSampler d(PlateKey plateKey) {
        Object object2;
        double d;
        double d2;
        Object object3;
        int n2;
        Object object4;
        boolean bl = this.g.bakedCalibration();
        if (!bl && !this.g.hasEstimate()) {
            return this.g;
        }
        if (this.M) {
            return RiverEngine.a(this.g);
        }
        boolean bl2 = false;
        if (this.I == 0) {
            object4 = this.J;
            synchronized (object4) {
                if (this.I == 0 && !this.K) {
                    this.K = true;
                    bl2 = true;
                } else {
                    while (this.I == 0) {
                        try {
                            this.J.wait(1000L);
                        }
                        catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            if (!bl2 && this.M) {
                return RiverEngine.a(this.g);
            }
        }
        object4 = new PlateDomain(this.i, plateKey);
        int n3 = Math.max(this.f.gridSpacing(), this.f.terrainSampleSpacing());
        int n4 = 4 * n3;
        while ((long)((((PlateDomain)object4).maxX - ((PlateDomain)object4).minX) / n4 + 1) * (long)((((PlateDomain)object4).maxZ - ((PlateDomain)object4).minZ) / n4 + 1) > 1024L) {
            n4 *= 2;
        }
        int n5 = Math.floorDiv(((PlateDomain)object4).minX, n4) * n4;
        int n6 = Math.floorDiv(((PlateDomain)object4).minZ, n4) * n4;
        ArrayList<int[]> arrayList = new ArrayList<int[]>();
        for (int i = n5; i <= ((PlateDomain)object4).maxX; i += n4) {
            for (int j = n6; j <= ((PlateDomain)object4).maxZ; j += n4) {
                if (!((PlateDomain)object4).contains(i, j) || this.g.height(i, j) == -1000.0) continue;
                arrayList.add(new int[]{i, j});
            }
        }
        double[] dArray = new double[arrayList.size()];
        double[] dArray2 = new double[arrayList.size()];
        AtomicLong atomicLong = new AtomicLong();
        AtomicLong atomicLong2 = new AtomicLong();
        AtomicInteger atomicInteger = new AtomicInteger();
        AtomicInteger atomicInteger2 = new AtomicInteger();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        boolean bl3 = bl2;
        LowResGrid.parallelFor(arrayList.size(), n -> {
            int[] nArray = (int[])arrayList.get(n);
            long l = System.nanoTime();
            dArray[n] = this.g.groundHeightExact(nArray[0], nArray[1]);
            long l2 = System.nanoTime();
            atomicLong.addAndGet(l2 - l);
            atomicInteger.incrementAndGet();
            if (bl3 && atomicBoolean.get()) {
                dArray2[n] = dArray[n];
                return;
            }
            dArray2[n] = this.g.estimate(nArray[0], nArray[1]);
            long l3 = System.nanoTime() - l2;
            atomicLong2.addAndGet(l3);
            int n10 = atomicInteger2.incrementAndGet();
            if (bl3 && n10 >= 8 && atomicLong2.get() / (long)n10 > atomicLong.get() / (long)Math.max(1, atomicInteger.get())) {
                atomicBoolean.set(true);
            }
        });
        HashMap<Long, Double> hashMap = new HashMap<Long, Double>(arrayList.size() * 2);
        HashMap<Integer, int[]> hashMap2 = new HashMap<Integer, int[]>();
        int n7 = 0;
        int n8 = 0;
        for (n2 = 0; n2 < arrayList.size(); ++n2) {
            int[] nArray3 = (int[])arrayList.get(n2);
            d2 = dArray[n2];
            if (d2 == -1000.0) continue;
            hashMap.put(RiverEngine.a(nArray3[0], nArray3[1]), d2);
            d = this.g.groundHeight(nArray3[0], nArray3[1]);
            double d3 = d - d2;
            if (d3 >= 6.0) continue;
            int[] nArray4 = hashMap2.computeIfAbsent(RiverEngine.a(dArray2[n2]), n -> new int[2]);
            nArray4[0] = nArray4[0] + 1;
            ++n7;
            if (!(d3 > 2.0) && !(d3 < -6.0)) continue;
            nArray4[1] = nArray4[1] + 1;
            ++n8;
        }
        n2 = Boolean.getBoolean("streamsreflowing.caldiag") ? 1 : 0;
        object3 = this.g;
        d2 = atomicInteger.get() > 0 ? (double)atomicLong.get() / (double)atomicInteger.get() : 0.0;
        d = atomicInteger2.get() > 0 ? (double)atomicLong2.get() / (double)atomicInteger2.get() : 0.0;
        boolean bl4 = false;
        if (bl2) {
            bl4 = (atomicBoolean.get() || atomicInteger2.get() >= 8) && d2 > 0.0 && (atomicBoolean.get() || d / d2 > 0.9375);
            int n9 = bl4 ? 2 : 1;
            IntConsumer intConsumer = this.L;
            Object object7 = this.J;
            synchronized (object7) {
                this.I = n9;
                this.K = false;
                if (n9 == 2) {
                    this.M = true;
                }
                this.J.notifyAll();
            }
            if (intConsumer != null) {
                intConsumer.accept(n9);
            }
            Constants.LOG.info("Streams Reflowing: fast terrain cost verdict {} for this world ({}% of a real read){}. Remembered for future loads.", n9 == 2 ? "OFF" : "ON", d2 > 0.0 ? Math.round(100.0 * d / d2) : -1L, atomicBoolean.get() ? " (early-abort)" : "");
        }
        if (bl4 || n7 >= 30 && (double)n8 > (double)n7 * 0.25) {
            if (n2 != 0) {
                Constants.LOG.info("[SR-BAKEDCAL] plate {},{} failed table verification ({}/{} outside [-{},+{}]) -> real sampling for this plate", plateKey.cx(), plateKey.cz(), n8, n7, 6.0, 2.0);
            }
            return RiverEngine.a((TerrainSampler)object3);
        }
        if (!bl) {
            return this.g;
        }
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (Map.Entry entry : hashMap2.entrySet()) {
            int[] nArray = (int[])entry.getValue();
            if (nArray[0] < 6 || !((double)nArray[1] > (double)nArray[0] * 0.1)) continue;
            hashSet.add((Integer)entry.getKey());
        }
        if (n2 != 0 && !hashSet.isEmpty()) {
            Constants.LOG.info("[SR-BAKEDCAL] plate {},{}: bands {} failed verification ({}/{} points outside [-{},+{}]) -> real grades for those bands only; {} anchors kept", plateKey.cx(), plateKey.cz(), new TreeSet(hashSet), n8, n7, 6.0, 2.0, hashMap.size());
        }
        if (hashMap.isEmpty() && hashSet.isEmpty()) {
            return this.g;
        }
        object2 = hashMap;
        HashSet<Integer> hashSet2 = hashSet;
        return new TerrainSampler(){
            final /* synthetic */ TerrainSampler a;
            final /* synthetic */ Map b;
            final /* synthetic */ Set c;
            {
                this.a = (TerrainSampler)object3;
                this.b = hashMap;
                this.c = hashSet;
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
                Double d = (Double)this.b.get(RiverEngine.a(n, n2));
                return d != null ? d.doubleValue() : this.a.height(n, n2);
            }

            @Override
            public double groundHeight(int n, int n2) {
                Double d = (Double)this.b.get(RiverEngine.a(n, n2));
                if (d != null) {
                    return d;
                }
                if (!this.c.isEmpty() && this.c.contains(RiverEngine.a(this.a.estimate(n, n2)))) {
                    return this.a.groundHeightExact(n, n2);
                }
                return this.a.groundHeight(n, n2);
            }

            @Override
            public double groundHeightExact(int n, int n2) {
                Double d = (Double)this.b.get(RiverEngine.a(n, n2));
                return d != null ? d.doubleValue() : this.a.groundHeightExact(n, n2);
            }

            @Override
            public double surfaceHeight(int n, int n2) {
                return this.a.surfaceHeight(n, n2);
            }

            @Override
            public boolean hasEstimate() {
                return this.a.hasEstimate();
            }

            @Override
            public double estimate(int n, int n2) {
                return this.a.estimate(n, n2);
            }

            @Override
            public boolean useHybridGridFill() {
                return this.a.useHybridGridFill();
            }

            @Override
            public boolean bakedCalibration() {
                return this.a.bakedCalibration();
            }

            @Override
            public boolean openAt(int n, int n2, int n3) {
                return this.a.openAt(n, n2, n3);
            }
        };
    }

    public RiverNetwork networkAt(int n, int n2) {
        return this.networkFor(this.plateAt(n, n2));
    }

    public void setStreamsEnabled(boolean bl) {
        this.Y = bl;
    }

    public ColumnPlan plan(int n, int n2) {
        ColumnPlan columnPlan;
        if (!this.Y) {
            return ColumnPlan.NONE;
        }
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        int n3 = (int)((long)n * 31L + (long)n2 & Integer.MAX_VALUE & 0x1FFFL);
        long[] lArray = this.O.get();
        ColumnPlan[] columnPlanArray = this.P.get();
        if (lArray[n3] == l && (columnPlan = columnPlanArray[n3]) != null) {
            return columnPlan;
        }
        columnPlan = this.b(n, n2);
        lArray[n3] = l;
        columnPlanArray[n3] = columnPlan;
        this.S.get()[n3] = this.U.get()[0];
        this.T.get()[n3] = this.V.get()[0];
        return columnPlan;
    }

    public String planDiag(int n, int n2) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        RiverSample riverSample = this.b(d, d2, this.n);
        if (riverSample == null) {
            return String.format(Locale.ROOT, "NOSAMPLE radius=%.1f plates=%d segs=%d", this.n, this.i.platesWithin(d, d2, this.n).length, this.a(d, d2, this.n));
        }
        double d3 = riverSample.width();
        double d4 = Math.max(5.0, d3 * this.f.bankWidth());
        return String.format(Locale.ROOT, "SAMPLED d=%.2f w=%.2f bank=%.2f limit=%.2f over=%+.2f grade=%.1f term=%s src=%s", riverSample.distance(), d3, d4, d3 + d4, riverSample.distance() - (d3 + d4), riverSample.gradeY(), riverSample.terminal(), riverSample.source());
    }

    private int a(double d, double d2, double d3) {
        int n = 0;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, d3)) {
            n += this.networkFor(plateKey).segmentCount();
        }
        return n;
    }

    private ColumnPlan b(int n, int n2) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        double d3 = this.n;
        RiverSample riverSample = this.c(d, d2, d3);
        if (riverSample == null) {
            return ColumnPlan.NONE;
        }
        return this.a(d, d2, riverSample, true);
    }

    private ColumnPlan a(double d, double d2, RiverSample riverSample, boolean bl) {
        double d3;
        double d4 = riverSample.width();
        double d5 = riverSample.distance();
        if (d5 > d4 + (d3 = Math.max(5.0, d4 * this.f.bankWidth()))) {
            return ColumnPlan.NONE;
        }
        double d6 = riverSample.gradeY();
        int n = Math.max(this.f.seaLevel() - 1, (int)Math.floor(d6) - this.a(d4, d, d2, riverSample.dirX(), riverSample.dirZ(), d6));
        int n2 = Math.max(1, (int)Math.round(2.0 * d4 * this.f.streamDepth()));
        int n3 = n - n2;
        boolean bl2 = d5 <= d4;
        double d7 = Math.pow(Math.max(0.0, Math.min(1.0, (d5 - d4) / d3)), this.f.aRiver());
        double d8 = riverSample.dirX();
        double d9 = riverSample.dirZ();
        if (bl) {
            double[] dArray = this.a(d, d2, d5, d4, riverSample.dirX(), riverSample.dirZ(), bl2, this.n, this.r.get());
            d8 = dArray[0];
            d9 = dArray[1];
        }
        return new ColumnPlan(bl2, true, n, n3, d7, d5, d4, d6, riverSample.slope(), riverSample.dirX(), riverSample.dirZ(), riverSample.terminal(), riverSample.source(), riverSample.upstreamSlope(), d8, d9, riverSample.distToOutlet());
    }

    public ColumnPlan altPlan(int n, int n2) {
        ColumnPlan columnPlan;
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        int n3 = (int)((long)n * 31L + (long)n2 & Integer.MAX_VALUE & 0x1FFFL);
        long[] lArray = this.W.get();
        ColumnPlan[] columnPlanArray = this.X.get();
        if (lArray[n3] == l && (columnPlan = columnPlanArray[n3]) != null) {
            return columnPlan == ColumnPlan.NONE ? null : columnPlan;
        }
        columnPlan = this.c(n, n2);
        lArray[n3] = l;
        columnPlanArray[n3] = columnPlan == null ? ColumnPlan.NONE : columnPlan;
        return columnPlan;
    }

    private ColumnPlan c(int n, int n2) {
        RiverSample riverSample;
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        int n3 = (int)((long)n * 31L + (long)n2 & Integer.MAX_VALUE & 0x1FFFL);
        RiverNetwork riverNetwork = null;
        int n4 = -1;
        if (this.O.get()[n3] == l && this.P.get()[n3] != null) {
            if (!this.P.get()[n3].inCorridor()) {
                return null;
            }
            riverNetwork = this.S.get()[n3];
            n4 = this.T.get()[n3];
        }
        if (riverNetwork == null) {
            riverSample = this.c(d, d2, this.n);
            if (riverSample == null || !this.a(riverSample)) {
                return null;
            }
            riverNetwork = this.U.get()[0];
            n4 = riverSample.segId();
        }
        riverSample = null;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, this.n)) {
            RiverNetwork riverNetwork2 = this.networkFor(plateKey);
            RiverSample riverSample2 = riverNetwork2.sampleCoveringForeign(d, d2, this.n, riverNetwork2 == riverNetwork ? n4 : -1);
            if (riverSample2 == null || riverSample != null && !(riverSample2.distance() < riverSample.distance())) continue;
            riverSample = riverSample2;
        }
        return riverSample == null ? null : this.a(d, d2, riverSample, false);
    }

    private int a(double d, double d2, double d3, double d4, double d5, double d6) {
        int n = RiverEngine.insetFor(d, this.f);
        double d7 = Math.sqrt(d4 * d4 + d5 * d5);
        if (n <= 0 || d7 < 1.0E-6) {
            return n;
        }
        if (this.lakeAt((int)Math.floor(d2), (int)Math.floor(d3)) != null) {
            return n;
        }
        double d8 = -d4 / d7;
        double d9 = -d5 / d7;
        for (int i = 1; i <= 6; ++i) {
            Lake lake = this.lakeAt((int)Math.floor(d2 + d8 * (double)i), (int)Math.floor(d3 + d9 * (double)i));
            if (lake == null) continue;
            int n2 = (int)Math.round((double)n * ((double)i / 7.0));
            int n3 = (int)Math.floor(d6) - lake.level();
            return Math.max(n2, Math.min(n, n3));
        }
        return n;
    }

    public ColumnPlan planLean(int n, int n2) {
        ColumnPlan columnPlan;
        if (!this.Y) {
            return ColumnPlan.NONE;
        }
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        int n3 = (int)((long)n * 31L + (long)n2 & Integer.MAX_VALUE & 0x1FFFL);
        long[] lArray = this.Q.get();
        ColumnPlan[] columnPlanArray = this.R.get();
        if (lArray[n3] == l && (columnPlan = columnPlanArray[n3]) != null) {
            return columnPlan;
        }
        LEAN_CALLS.incrementAndGet();
        RiverSample riverSample = this.b((double)n + 0.5, (double)n2 + 0.5, this.n);
        ColumnPlan columnPlan2 = riverSample == null ? ColumnPlan.NONE : this.a((double)n + 0.5, (double)n2 + 0.5, riverSample, false);
        lArray[n3] = l;
        columnPlanArray[n3] = columnPlan2;
        return columnPlan2;
    }

    private RiverSample b(double d, double d2, double d3) {
        RiverSample riverSample = null;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, d3)) {
            RiverSample riverSample2 = this.networkFor(plateKey).sample(d, d2, d3);
            if (riverSample2 == null || riverSample != null && !(riverSample2.distance() < riverSample.distance())) continue;
            riverSample = riverSample2;
        }
        return riverSample;
    }

    private RiverSample c(double d, double d2, double d3) {
        RiverSample riverSample = null;
        RiverNetwork riverNetwork = null;
        boolean bl = false;
        boolean bl2 = false;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, d3)) {
            RiverNetwork riverNetwork2 = this.networkFor(plateKey);
            RiverSample riverSample2 = riverNetwork2.sampleTiered(d, d2, d3);
            if (riverSample2 == null) continue;
            boolean bl3 = riverSample2.distance() <= riverSample2.width();
            boolean bl4 = this.a(riverSample2);
            if (riverSample != null && (!bl3 || bl) && (bl3 != bl || (!bl4 || bl2) && (bl4 != bl2 || !(riverSample2.distance() < riverSample.distance())))) continue;
            riverSample = riverSample2;
            riverNetwork = riverNetwork2;
            bl = bl3;
            bl2 = bl4;
        }
        this.U.get()[0] = riverNetwork;
        this.V.get()[0] = riverSample == null ? -1 : riverSample.segId();
        return riverSample;
    }

    public boolean lakeNearChunk(int n, int n2, int n3) {
        double d = (double)n + 8.0;
        double d2 = (double)n2 + 8.0;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, this.f.gridSpacing() + n3)) {
            for (Lake lake : this.baseFor(plateKey).lakes()) {
                int n4;
                int n5;
                int n6 = lake.cellMinX();
                int n7 = n6 + lake.cellsWide() * lake.spacing();
                if (n7 < n - n3 || n6 > n + 16 + n3 || (n5 = (n4 = lake.cellMinZ()) + lake.cellsHigh() * lake.spacing()) < n2 - n3 || n4 > n2 + 16 + n3) continue;
                return true;
            }
        }
        return false;
    }

    private boolean a(RiverSample riverSample) {
        double d = riverSample.width();
        return riverSample.distance() <= d + Math.max(5.0, d * this.f.bankWidth());
    }

    private double[] a(double d, double d2, double d3, double d4, double d5, double d6, boolean bl, double d7, double[] dArray) {
        RiverSample riverSample;
        double d8;
        double d9 = Math.sqrt(d5 * d5 + d6 * d6);
        if (!bl || d9 < 1.0E-6 || d4 < 0.5) {
            dArray[0] = d5;
            dArray[1] = d6;
            return dArray;
        }
        double d10 = d5 / d9;
        double d11 = d6 / d9;
        double d12 = -d11;
        double d13 = d10;
        double d14 = Math.max(4.0, d4);
        RiverSample riverSample2 = this.b(d + d10 * d14, d2 + d11 * d14, d7);
        double d15 = 0.0;
        if (riverSample2 != null && (d8 = Math.sqrt(riverSample2.dirX() * riverSample2.dirX() + riverSample2.dirZ() * riverSample2.dirZ())) > 1.0E-6) {
            d15 = d10 * (riverSample2.dirZ() / d8) - d11 * (riverSample2.dirX() / d8);
        }
        double d16 = (riverSample = this.b(d + d12 * 1.5, d2 + d13 * 1.5, d7)) != null && riverSample.distance() < d3 ? -d3 : d3;
        double d17 = Math.max(-1.0, Math.min(1.0, d16 / d4));
        double d18 = 1.5 * d15 * d17;
        double d19 = riverSample2 != null ? (riverSample2.width() - d4) / d14 : 0.0;
        double d20 = Math.cos(d18 += Math.max(-0.5, Math.min(0.5, 2.0 * d19 * d17)));
        double d21 = Math.sin(d18);
        dArray[0] = d10 * d20 - d11 * d21;
        dArray[1] = d10 * d21 + d11 * d20;
        return dArray;
    }

    public boolean inAnyChannel(int n, int n2) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        double d3 = this.m;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, d3)) {
            if (!this.networkFor(plateKey).inAnyChannel(d, d2, d3)) continue;
            return true;
        }
        return false;
    }

    public boolean anyStreamsNear(double d, double d2, double d3) {
        for (PlateKey plateKey : this.i.platesWithin(d, d2, d3 + this.n)) {
            if (this.networkFor(plateKey).segmentCount() <= 0) continue;
            return true;
        }
        return false;
    }

    public PlateKey[] platesNear(double d, double d2, double d3) {
        return this.i.platesWithin(d, d2, d3 + this.n);
    }

    public boolean inCorridor(int n, int n2) {
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        long[] lArray = this.o.get();
        if (lArray[0] == l) {
            return lArray[1] != 0L;
        }
        boolean bl = false;
        RiverSample riverSample = this.b((double)n + 0.5, (double)n2 + 0.5, this.n);
        if (riverSample != null) {
            double d = Math.max(5.0, riverSample.width() * this.f.bankWidth());
            bl = riverSample.distance() <= riverSample.width() + d;
        }
        lArray[0] = l;
        lArray[1] = bl ? 1L : 0L;
        return bl;
    }

    public boolean corridorNearChunk(int n, int n2, int n3) {
        RiverSample riverSample = this.b((double)n + 8.0, (double)n2 + 8.0, this.n + (double)n3);
        if (riverSample == null) {
            return false;
        }
        double d = riverSample.width() + Math.max(5.0, riverSample.width() * this.f.bankWidth());
        return riverSample.distance() <= 11.32 + (double)n3 + d;
    }

    public boolean corridorTouchesChunk(int n, int n2) {
        for (PlateKey plateKey : this.i.platesWithin((double)n + 8.0, (double)n2 + 8.0, this.n + 12.0)) {
            if (!this.networkFor(plateKey).corridorTouchesBox(n, n2, n + 15, n2 + 15)) continue;
            return true;
        }
        return false;
    }

    public long vetoProbeAt(int n, int n2) {
        RiverSample riverSample;
        long l = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        int n3 = (int)((long)n * 31L + (long)n2 & Integer.MAX_VALUE & 0xFFFL);
        long[] lArray = this.ab.get();
        long[] lArray2 = this.ac.get();
        if (lArray[n3] == l) {
            return lArray2[n3];
        }
        long l2 = (long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL;
        long[] lArray3 = this.ad.get();
        if (lArray3[0] != l2) {
            lArray3[1] = this.corridorTouchesChunk(n >> 4 << 4, n2 >> 4 << 4) ? 1L : 0L;
            lArray3[2] = this.lakeNearChunk(n >> 4 << 4, n2 >> 4 << 4, 8) ? 1L : 0L;
            lArray3[0] = l2;
        }
        int n4 = Integer.MIN_VALUE;
        if (lArray3[1] != 0L && (riverSample = this.b((double)n + 0.5, (double)n2 + 0.5, this.n)) != null && riverSample.distance() <= riverSample.width()) {
            n4 = Math.max(this.f.seaLevel() - 1, (int)Math.floor(riverSample.gradeY()) - RiverEngine.insetFor(riverSample.width(), this.f));
        }
        int n5 = lArray3[2] != 0L ? this.lakeSurfaceAt(n, n2) : Integer.MIN_VALUE;
        long l3 = (long)n5 << 32 | (long)n4 & 0xFFFFFFFFL;
        lArray[n3] = l;
        lArray2[n3] = l3;
        return l3;
    }

    public int lakeSurfaceAt(int n, int n2) {
        Lake lake = this.lakeAt(n, n2);
        return lake == null ? Integer.MIN_VALUE : lake.level();
    }

    public Lake lakeAt(int n, int n2) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        Lake lake = null;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, this.f.gridSpacing())) {
            Lake lake2 = this.baseFor(plateKey).lakeAt(n, n2);
            if (lake2 == null || lake != null && lake2.level() >= lake.level()) continue;
            lake = lake2;
        }
        return lake;
    }

    public Lake lakeNear(int n, int n2, int n3) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        Lake lake = null;
        for (PlateKey plateKey : this.i.platesWithin(d, d2, (double)this.f.gridSpacing() * (1.0 + (double)n3))) {
            Lake lake2 = this.baseFor(plateKey).lakeNear(n, n2, n3);
            if (lake2 == null || lake != null && lake2.level() >= lake.level()) continue;
            lake = lake2;
        }
        return lake;
    }

    public boolean isPlateCached(PlateKey plateKey) {
        return plates.contains(plateKey);
    }

    public boolean isPlateReady(PlateKey plateKey) {
        return plates.isReady(plateKey);
    }

    /** {@link #plateCacheState} 返回值：没有可用缓存，调用方应照旧构建。 */
    public static final int PLATE_MISSING = 0;
    /** {@link #plateCacheState} 返回值：本会话内存缓存里已经就绪。 */
    public static final int PLATE_READY = 1;
    /** {@link #plateCacheState} 返回值：本次调用从磁盘缓存读回并填入内存缓存。 */
    public static final int PLATE_RESTORED = 2;

    /** 启动探测可恢复磁盘缓存，但不会等待别的线程正在构建的板块。 */
    public int plateCacheState(PlateKey key) { return plates.cacheState(key); }

    public boolean platesReadyNear(double x, double z, double radius) {
        return platesReady(platesNear(x, z, radius));
    }

    /** 就绪查询与未就绪后的等待共用同一组邻板，避免重复计算板块边界。 */
    public boolean platesReady(PlateKey[] keys) {
        for (PlateKey plateKey : keys) {
            if (this.isPlateReady(plateKey)) continue;
            return false;
        }
        return true;
    }

    public CompletableFuture<Void> whenPlatesReadyNear(double x, double z, double radius, Executor executor) {
        return whenPlatesReady(platesNear(x, z, radius), executor);
    }

    public CompletableFuture<Void> whenPlatesReady(PlateKey[] keys, Executor executor) {
        // 重新检查就绪状态：查询之后可能已有别的线程完成，不能重复排队。
        ArrayList<CompletableFuture<Void>> waiting = null;
        for (PlateKey key : keys) {
            if (plates.isReady(key)) continue;
            if (waiting == null) waiting = new ArrayList<>(2);
            waiting.add(plates.request(key, executor));
        }
        if (waiting == null) return CompletableFuture.completedFuture(null);
        if (waiting.size() == 1) return waiting.getFirst();
        // 一个板块已判失败，区块就不能续跑；不用等别的板块也结束才通知区块。
        var result = new CompletableFuture<Void>();
        var remaining = new AtomicInteger(waiting.size());
        for (var plate : waiting) plate.whenComplete((ignored, failure) -> {
            if (failure != null) result.completeExceptionally(failure);
            else if (remaining.decrementAndGet() == 0) result.complete(null);
        });
        return result;
    }

    public int cachedPlateCount() { return plates.cachedBaseCount(); }

    @FunctionalInterface
    public static interface BuildListener {
        public void onBuilt(PlateKey var1, int var2, int var3, double var4, int var6);
    }
}

