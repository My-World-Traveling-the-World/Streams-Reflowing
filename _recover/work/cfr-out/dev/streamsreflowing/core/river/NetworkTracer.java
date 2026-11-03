/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.river;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.ColumnCache;
import dev.streamsreflowing.core.ColumnKey;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.math.Hashing;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NearestRiverIndex;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Function;

public final class NetworkTracer {
    private static final int[] a = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] b = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    private static final double c = 0.001;
    private static final int d = 64;
    private static final double e = 1.0;
    private static final double f = 6.0;
    private static final double g = 2.0;
    private static final double h = 1.0;
    private static final double i = 2.0;
    private static final double j = 5.0;
    private static final double k = 40.0;
    private static final double l = 4.0;
    private static final double m = 6.0;
    private static final double n = 3.0;
    private static final double o = 4.0;
    private static final double p = 8.0;
    private static final double q = 2.0;
    private static final double r = 2.0;
    private static final double s = 8.0;
    private static final double t = 24.0;
    private static final int u = 2;
    private static final int v = 1;
    private static final double w = 1.0;
    private static final int x = 4;
    private static final boolean y = Boolean.getBoolean("streamsreflowing.mouthsingle");
    private static final int z = 24;
    private static final int A = 24;
    private static final double B = 8.0;
    private static final int C = 8;
    private static final int D = 5;
    private static final double E = 24.0;
    private static final double F = 3.0;
    private static final int G = 2;
    private static final double H = 32.0;
    private static final int[] I = NetworkTracer.a();
    private static final int J = 96;
    private static final int[] K = NetworkTracer.b();
    private static final double L = 0.5;
    private static final double M = 0.5;
    private static final double N = 1.5;
    private static final boolean O = Boolean.getBoolean("streamsreflowing.deepdiag");
    private static final AtomicLongArray P = new AtomicLongArray(6);
    private static final int Q = 0;
    private static final int R = 1;
    private static final int S = 2;
    private static final int T = 3;
    private static final int U = 4;
    private static final int V = 5;
    private static final String[] W = new String[]{"snap", "followGround", "meander", "confine", "smooth", "emit"};
    private static final ThreadLocal<long[]> X = ThreadLocal.withInitial(() -> new long[8]);
    private static final int Y = 0;
    private static final int Z = 1;
    private static final int aa = 2;
    private static final int ab = 3;
    private static final int ac = 4;
    private static final int ad = 5;
    private static final int ae = 6;
    private static final int af = 7;
    private static final String[] ag = new String[]{"bfs+qualify", "borderPre", "probes", "redetect", "poolAt", "flood", "emit", "floodWarm"};
    private static final double ah = 6.0;
    private static final double ai = 3.0;
    private static final double aj = 2.0;
    private static final double ak = 1.5;
    private static final double al = Integer.getInteger("streamsreflowing.lakeminarea", 1024).intValue();
    private static final double am = 3.0;
    private static final double an = 4.0;
    private static final double ao = 4.0;
    private static final int ap = 48;
    private static final int aq = 2;
    private static final double ar = 4.0;
    private static final int as = Integer.getInteger("streamsreflowing.lakethinband", 8);
    private static final double at = (double)Integer.getInteger("streamsreflowing.lakethinsoft", 20).intValue() / 10.0;
    private static final double au = 5.0;
    private static final int av = 8;
    private static final int aw = 107749;
    private static final int ax = 2;
    private static final double ay = 16.0;
    private static final double az = 0.03;
    private static final int aA = 3;
    private static final double aB = 0.3;
    private static final int aC = 6;
    private static final double aD = 1.0;
    private static final double aE = 0.5;
    private static final double aF = 3.5;
    private static final double aG = 3.0;
    private static final double aH = 1.0;
    private static final double aI = 4.0;
    private static final long aJ = 0x4000000000000000L;

    private NetworkTracer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static RiverNetwork build(PlateDomain plateDomain, TerrainSampler terrainSampler, WetnessSampler wetnessSampler, StreamsConfig streamsConfig, long l2) {
        int n3;
        int n4;
        int n5;
        List[] listArray;
        int n6;
        Object object;
        int n7;
        int n8;
        int n9;
        double d;
        int n10;
        int n11;
        int n12;
        int n13;
        int n14;
        Object object2;
        Object object3;
        double d2;
        int n15;
        int n16;
        int n17;
        int n18;
        Iterator<Lake> iterator;
        int n19;
        int n20;
        int n21;
        int n22;
        int n23;
        int n24;
        int n25;
        int n26;
        int n27;
        int n28;
        int n29;
        long l3 = System.nanoTime();
        LowResGrid lowResGrid = new LowResGrid(plateDomain, terrainSampler, streamsConfig);
        long l4 = System.nanoTime();
        int n30 = lowResGrid.size();
        int n31 = streamsConfig.seaLevel();
        boolean[] blArray = new boolean[n30];
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        for (n29 = 0; n29 < n30; ++n29) {
            if (!lowResGrid.inDomain(n29) || lowResGrid.height(n29) == -1000.0) continue;
            n28 = terrainSampler.waterBiomeClass(Math.round(lowResGrid.worldX(lowResGrid.gi(n29))), Math.round(lowResGrid.worldZ(lowResGrid.gj(n29))));
            if (n28 == 1) {
                lowResGrid.overrideHeight(n29, -1001.0);
                lowResGrid.markRiverOutlet(n29);
                blArray[n29] = true;
                arrayDeque.add(n29);
                continue;
            }
            if (n28 != 2) continue;
            lowResGrid.overrideHeight(n29, -1000.0);
            blArray[n29] = true;
        }
        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            n29 = 0;
            for (n28 = 0; n28 < n30; ++n28) {
                if (!lowResGrid.riverOutlet(n28)) continue;
                ++n29;
            }
            Constants.LOG.info("[SR-RTF] plate {}: {} river-outlet cells after tier 1", (Object)plateDomain.key(), (Object)n29);
        }
        if (!Boolean.getBoolean("streamsreflowing.nohighrivers")) {
            int n32;
            int n33;
            int n34;
            for (n29 = 0; n29 < n30; ++n29) {
                if (blArray[n29] || !lowResGrid.inDomain(n29) || lowResGrid.height(n29) <= (double)(n31 + 8)) continue;
                n28 = 0;
                n34 = lowResGrid.gi(n29);
                n27 = lowResGrid.gj(n29);
                for (n33 = 0; n33 < 8; ++n33) {
                    n32 = n34 + a[n33];
                    n26 = n27 + b[n33];
                    if (!lowResGrid.valid(n32, n26) || !lowResGrid.inDomain(lowResGrid.idx(n32, n26)) || !(lowResGrid.height(lowResGrid.idx(n32, n26)) > lowResGrid.height(n29))) continue;
                    ++n28;
                }
                if (n28 < 5) continue;
                blArray[n29] = true;
                if (terrainSampler.waterBiomeClassAt(Math.round(lowResGrid.worldX(n34)), (int)Math.round(lowResGrid.height(n29)), Math.round(lowResGrid.worldZ(n27))) != 1) continue;
                lowResGrid.overrideHeight(n29, -1001.0);
                lowResGrid.markRiverOutlet(n29);
                arrayDeque.add(n29);
            }
            n29 = 0;
            while (!arrayDeque.isEmpty()) {
                n28 = (Integer)arrayDeque.poll();
                n34 = lowResGrid.gi(n28);
                n27 = lowResGrid.gj(n28);
                for (n33 = 0; n33 < 8; ++n33) {
                    n32 = n34 + a[n33];
                    n26 = n27 + b[n33];
                    if (!lowResGrid.valid(n32, n26) || blArray[n25 = lowResGrid.idx(n32, n26)] || !lowResGrid.inDomain(n25) || lowResGrid.height(n25) == -1000.0) continue;
                    blArray[n25] = true;
                    if (terrainSampler.waterBiomeClassAt(Math.round(lowResGrid.worldX(n32)), (int)Math.round(lowResGrid.height(n25)), Math.round(lowResGrid.worldZ(n26))) != 1) continue;
                    lowResGrid.overrideHeight(n25, -1001.0);
                    lowResGrid.markRiverOutlet(n25);
                    arrayDeque.add(n25);
                    ++n29;
                }
            }
            if (n29 > 0) {
                Constants.LOG.debug("[SR-MERGE] high-river outlets: {} extra river cells found above the sea-level probe", (Object)n29);
            }
        }
        if (O) {
            n29 = Math.max(1, n30 / 64);
            n28 = 0;
            double d3 = 0.0;
            double d4 = 0.0;
            n26 = 0;
            n25 = 0;
            n24 = 0;
            for (n23 = 0; n23 < n30; n23 += n29) {
                double d5;
                if (!lowResGrid.inDomain(n23)) continue;
                n22 = lowResGrid.worldX(lowResGrid.gi(n23));
                n21 = lowResGrid.worldZ(lowResGrid.gj(n23));
                double d6 = lowResGrid.height(n23);
                if (d6 == -1000.0 || d6 == -1001.0 || Double.isNaN(d5 = terrainSampler.groundHeightExact(n22, n21))) continue;
                double d7 = d6 - d5;
                ++n28;
                d3 += Math.abs(d7);
                if (Math.abs(d7) > 2.0) {
                    ++n24;
                }
                if (!(Math.abs(d7) > Math.abs(d4))) continue;
                d4 = d7;
                n26 = n22;
                n25 = n21;
            }
            if (n28 > 0) {
                Constants.LOG.info("[SR-SAMPLEAUDIT] plate {}: sampled-vs-exact over {} cells -- meanAbs {}, >2 blocks {} ({}%), worst {} at ({},{}); sampler baked={} hasEstimate={}", plateDomain.key(), n28, String.format(Locale.ROOT, "%.2f", d3 / (double)n28), n24, String.format(Locale.ROOT, "%.0f", 100.0 * (double)n24 / (double)n28), String.format(Locale.ROOT, "%+.1f", d4), n26, n25, terrainSampler.bakedCalibration(), terrainSampler.hasEstimate());
            }
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (n28 = 0; n28 < lowResGrid.nz; ++n28) {
            for (int i = 0; i < lowResGrid.nx; ++i) {
                n27 = lowResGrid.idx(i, n28);
                if (!lowResGrid.inDomain(n27)) continue;
                arrayList.add(n27);
            }
        }
        double[] dArray = new double[n30];
        boolean[] blArray2 = new boolean[n30];
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<Integer>((n, n2) -> Double.compare(dArray[n], dArray[n2]));
        boolean[] blArray3 = new boolean[n30];
        ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
        Object object4 = arrayList.iterator();
        block14: while (object4.hasNext()) {
            n25 = (Integer)object4.next();
            if (!lowResGrid.riverOutlet(n25)) continue;
            n24 = lowResGrid.gi(n25);
            n23 = lowResGrid.gj(n25);
            for (n22 = 0; n22 < 8; ++n22) {
                int n35;
                n21 = n24 + a[n22];
                int n36 = n23 + b[n22];
                if (!lowResGrid.valid(n21, n36) || !lowResGrid.inDomain(n35 = lowResGrid.idx(n21, n36)) || lowResGrid.riverOutlet(n35)) continue;
                blArray3[n25] = true;
                arrayList2.add(n25);
                continue block14;
            }
        }
        if (!arrayList2.isEmpty()) {
            object4 = new double[arrayList2.size()];
            ArrayList<Integer> arrayList3 = arrayList2;
            LowResGrid.parallelFor(arrayList3.size(), arg_0 -> NetworkTracer.a(arrayList3, lowResGrid, terrainSampler, (double[])object4, arg_0));
            for (n24 = 0; n24 < arrayList3.size(); ++n24) {
                lowResGrid.overrideHeight((Integer)arrayList3.get(n24), (double)object4[n24]);
            }
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-RTF] plate {}: {} river bank cells seeded", (Object)plateDomain.key(), (Object)arrayList3.size());
            }
        }
        object4 = arrayList.iterator();
        while (object4.hasNext()) {
            int n37 = (Integer)object4.next();
            dArray[n37] = lowResGrid.height(n37);
            if (lowResGrid.riverOutlet(n37)) {
                blArray2[n37] = true;
                if (!blArray3[n37]) continue;
                priorityQueue.add(n37);
                continue;
            }
            if (!(lowResGrid.height(n37) <= (double)n31) && !NetworkTracer.a(lowResGrid, n37)) continue;
            blArray2[n37] = true;
            priorityQueue.add(n37);
        }
        while (!priorityQueue.isEmpty()) {
            int n38 = (Integer)priorityQueue.poll();
            int n39 = lowResGrid.gi(n38);
            n24 = lowResGrid.gj(n38);
            for (n23 = 0; n23 < 8; ++n23) {
                int n40;
                n22 = n39 + a[n23];
                n21 = n24 + b[n23];
                if (!lowResGrid.valid(n22, n21) || !lowResGrid.inDomain(n40 = lowResGrid.idx(n22, n21)) || blArray2[n40]) continue;
                dArray[n40] = Math.max(lowResGrid.height(n40), dArray[n38] + 0.001);
                blArray2[n40] = true;
                priorityQueue.add(n40);
            }
        }
        long l5 = System.nanoTime();
        int[] nArray = new int[n30];
        Object object5 = arrayList.iterator();
        while (object5.hasNext()) {
            n22 = (Integer)object5.next();
            nArray[n22] = -1;
            n21 = lowResGrid.gi(n22);
            int n41 = lowResGrid.gj(n22);
            double d8 = dArray[n22];
            double d9 = 0.0;
            int n42 = -1;
            for (n20 = 0; n20 < 8; ++n20) {
                double d10;
                int n43;
                n19 = n21 + a[n20];
                int n44 = n41 + b[n20];
                if (!lowResGrid.valid(n19, n44) || !lowResGrid.inDomain(n43 = lowResGrid.idx(n19, n44))) continue;
                if (lowResGrid.riverOutlet(n43)) {
                    n42 = n43;
                    break;
                }
                double d11 = d8 - dArray[n43];
                if (d11 <= 0.0 || d8 <= (double)n31 - 2.0 && d11 < 2.0 || !((d10 = d11 / (n20 % 2 == 0 ? 1.0 : 1.4142135623730951)) > d9)) continue;
                d9 = d10;
                n42 = n43;
            }
            nArray[n22] = n42;
        }
        arrayList.sort((n, n2) -> {
            int n3 = Double.compare(dArray[n2], dArray[n]);
            return n3 != 0 ? n3 : Integer.compare(n, n2);
        });
        object5 = new double[n30];
        double d12 = Math.max(0.0, streamsConfig.wetnessMultiplier());
        Iterator iterator2 = arrayList.iterator();
        while (iterator2.hasNext()) {
            int n45 = (Integer)iterator2.next();
            object5[n45] = d12;
        }
        iterator2 = arrayList.iterator();
        while (iterator2.hasNext()) {
            int n46 = (Integer)iterator2.next();
            if (nArray[n46] < 0) continue;
            Object object6 = object5;
            int n47 = nArray[n46];
            object6[n47] = object6[n47] + object5[n46];
        }
        double d13 = streamsConfig.minRiverAccumulation();
        if (streamsConfig.growFromWater()) {
            d13 = Math.max(1.0, d13 * streamsConfig.growDensity());
        }
        boolean[] blArray4 = new boolean[n30];
        Iterator iterator3 = arrayList.iterator();
        while (iterator3.hasNext()) {
            int n48 = (Integer)iterator3.next();
            blArray4[n48] = dArray[n48] > (double)n31 && object5[n48] >= d13 && !lowResGrid.riverOutlet(n48);
        }
        int n49 = (int)Math.round(streamsConfig.reconnectRadius() / (double)Math.max(1, streamsConfig.gridSpacing()));
        if (n49 >= 1 && !streamsConfig.growFromWater()) {
            boolean bl = false;
            Iterator iterator4 = arrayList.iterator();
            while (iterator4.hasNext()) {
                n20 = (Integer)iterator4.next();
                if (!blArray4[n20] || nArray[n20] >= 0 || dArray[n20] <= (double)n31 || (n19 = NetworkTracer.a(lowResGrid, n20, dArray, blArray4, n31, n49)) < 0) continue;
                nArray[n20] = n19;
                bl = true;
            }
            if (bl) {
                iterator4 = arrayList.iterator();
                while (iterator4.hasNext()) {
                    n20 = (Integer)iterator4.next();
                    object5[n20] = d12;
                }
                iterator4 = arrayList.iterator();
                while (iterator4.hasNext()) {
                    n20 = (Integer)iterator4.next();
                    if (nArray[n20] < 0) continue;
                    Object object7 = object5;
                    int n50 = nArray[n20];
                    object7[n50] = object7[n50] + object5[n20];
                }
                iterator4 = arrayList.iterator();
                while (iterator4.hasNext()) {
                    n20 = (Integer)iterator4.next();
                    blArray4[n20] = dArray[n20] > (double)n31 && object5[n20] >= d13;
                }
            }
        }
        double d14 = streamsConfig.wetnessDryCutoff();
        double d15 = streamsConfig.wetnessWetReference();
        double d16 = streamsConfig.wetnessDryFloor();
        if (d15 > d14) {
            int n51;
            boolean[] blArray5 = new boolean[n30];
            Object object8 = arrayList.iterator();
            while (object8.hasNext()) {
                int n52 = (Integer)object8.next();
                if (!blArray4[n52] || nArray[n52] < 0 || !blArray4[nArray[n52]]) continue;
                blArray5[nArray[n52]] = true;
            }
            object8 = new boolean[n30];
            ColumnCache columnCache = new ColumnCache();
            iterator = arrayList.iterator();
            while (iterator.hasNext()) {
                n51 = (Integer)iterator.next();
                if (!blArray4[n51] || blArray5[n51] || NetworkTracer.a(n51, lowResGrid, wetnessSampler, columnCache, d14, d15, d16, l2)) continue;
                object8[n51] = true;
            }
            iterator = arrayList.iterator();
            while (iterator.hasNext()) {
                n51 = (Integer)iterator.next();
                if (object8[n51] == false || nArray[n51] < 0 || !blArray4[nArray[n51]]) continue;
                object8[nArray[n51]] = true;
            }
            iterator = arrayList.iterator();
            while (iterator.hasNext()) {
                n51 = (Integer)iterator.next();
                blArray4[n51] = blArray4[n51] && object8[n51] != false;
            }
        }
        long l6 = System.nanoTime();
        int[] nArray2 = new int[n30];
        Arrays.fill(nArray2, -1);
        iterator = new ArrayList();
        ArrayList<PendingOutflow> arrayList4 = new ArrayList<PendingOutflow>();
        double d17 = System.getProperty("streamsreflowing.lakeminarea") != null ? al : (streamsConfig.lakeMinArea() > 0.0 ? streamsConfig.lakeMinArea() : al);
        int n53 = Math.max(4, (int)Math.round(d17 / ((double)streamsConfig.gridSpacing() * (double)streamsConfig.gridSpacing())));
        boolean[] blArray6 = new boolean[n30];
        ArrayDeque<Integer> arrayDeque2 = new ArrayDeque<Integer>();
        ArrayList<Integer> arrayList5 = new ArrayList<Integer>();
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        boolean bl = terrainSampler.hasEstimate() && !Boolean.getBoolean("streamsreflowing.noestslack");
        double d18 = bl ? 0.25 : 0.5;
        double d19 = bl ? 1.0 : 0.0;
        double d20 = bl ? Double.parseDouble(System.getProperty("streamsreflowing.lakedepthgate", "0")) : 0.0;
        int n54 = 0;
        int n55 = 0;
        int n56 = 0;
        int n57 = 0;
        int n58 = 0;
        int n59 = 0;
        int n60 = 0;
        long l7 = 0L;
        int n61 = 0;
        boolean bl2 = Boolean.getBoolean("streamsreflowing.caldiag") || O;
        long[] lArray = X.get();
        Arrays.fill(lArray, 0L);
        Iterator iterator5 = arrayList.iterator();
        while (iterator5.hasNext()) {
            double d21;
            int n62;
            int n63 = (Integer)iterator5.next();
            if (blArray6[n63] || !NetworkTracer.a(n63, dArray, lowResGrid, n31, d18)) continue;
            arrayList5.clear();
            arrayDeque2.clear();
            blArray6[n63] = true;
            arrayDeque2.add(n63);
            double d22 = Double.MAX_VALUE;
            int n64 = n63;
            double d23 = Double.MAX_VALUE;
            n18 = n63;
            double d24 = 0.0;
            while (!arrayDeque2.isEmpty()) {
                n62 = (Integer)arrayDeque2.poll();
                arrayList5.add(n62);
                if (dArray[n62] < d22) {
                    d22 = dArray[n62];
                    n64 = n62;
                }
                if (lowResGrid.height(n62) < d23) {
                    d23 = lowResGrid.height(n62);
                    n18 = n62;
                }
                d24 = Math.max(d24, (double)object5[n62]);
                int n65 = lowResGrid.gi(n62);
                n17 = lowResGrid.gj(n62);
                for (n16 = 0; n16 < 8; ++n16) {
                    int n66 = n65 + a[n16];
                    int n67 = n17 + b[n16];
                    if (!lowResGrid.valid(n66, n67) || blArray6[n15 = lowResGrid.idx(n66, n67)] || !NetworkTracer.a(n15, dArray, lowResGrid, n31, d18)) continue;
                    blArray6[n15] = true;
                    arrayDeque2.add(n15);
                }
            }
            ++n54;
            n62 = arrayList.size() / 4;
            if (streamsConfig.lakeMaxArea() > 0.0) {
                n62 = Math.min(n62, (int)Math.round(streamsConfig.lakeMaxArea() / ((double)streamsConfig.gridSpacing() * (double)streamsConfig.gridSpacing())));
            }
            if (arrayList5.size() > n62) {
                ++n55;
                continue;
            }
            double d25 = d22 - d23;
            n16 = d24 >= d13 * 1.5 ? 1 : 0;
            double d26 = (n16 != 0 ? 2.0 : 3.0) - d19;
            int n68 = n15 = n16 != 0 ? Math.max(2, n53 / 2) : n53;
            if (d25 < d26 || arrayList5.size() < n15 || d24 < d13) {
                if (!bl2 || arrayList5.size() < Math.max(4, n53 / 2)) continue;
                Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: qualify spillDepth={} minDepth={} cells={} minCells={} acc={} thr={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), String.format("%.1f", d25), String.format("%.1f", d26), arrayList5.size(), n15, String.format("%.0f", d24), String.format("%.0f", d13));
                continue;
            }
            if (n16 == 0 && d25 < d20) {
                ++n58;
                continue;
            }
            d2 = Math.sqrt(arrayList5.size()) * (double)streamsConfig.gridSpacing();
            if (d23 < (double)streamsConfig.seaLevel() && d25 > 0.5 * d2) {
                ++n59;
                if (!bl2) continue;
                Constants.LOG.info("[SR-LAKEREJ] plate {} shaft-like: floor={} sea={} depth={} width={} cells={}", plateDomain.key(), String.format(Locale.ROOT, "%.1f", d23), streamsConfig.seaLevel(), String.format(Locale.ROOT, "%.1f", d25), String.format(Locale.ROOT, "%.0f", d2), arrayList5.size());
                continue;
            }
            ++n56;
            double d27 = d24 * (double)streamsConfig.gridSpacing() * (double)streamsConfig.gridSpacing();
            double d28 = 3.0 + 4.0 * Math.sqrt(d27 / 2500.0);
            int n69 = object3 = d28 >= d25 - 1.0 ? 1 : 0;
            if (streamsConfig.growFromWater()) {
                object2 = Math.min((int)Math.floor(d22) - (bl ? 2 : 1), (int)Math.floor(d23 + d28));
                object3 = 0;
            } else {
                object2 = (int)Math.floor(d23 + Math.min(d25 - 1.0, d28));
            }
            double d29 = NetworkTracer.a(arrayList5, lowResGrid, terrainSampler);
            if (streamsConfig.growFromWater()) {
                if (!Double.isNaN(d29)) {
                    continue;
                }
            } else if (!Double.isNaN(d29) && (n14 = (int)Math.floor(d29) - 1) < object2) {
                object2 = n14;
                object3 = 0;
            }
            n14 = lowResGrid.worldX(lowResGrid.gi(n64));
            n13 = lowResGrid.worldZ(lowResGrid.gj(n64));
            double d30 = wetnessSampler.wetness(n14, (int)object2, n13);
            double d31 = Math.max(streamsConfig.wetnessDryFloor(), Math.min(1.0, (d30 - streamsConfig.wetnessDryCutoff()) / Math.max(1.0E-6, streamsConfig.wetnessWetReference() - streamsConfig.wetnessDryCutoff())));
            double d32 = 1.0 - d31;
            n12 = 0;
            if (d32 > 0.0) {
                double d33 = Hashing.unit(l2, n14, n13, 107749L);
                n12 = (int)Math.round(8.0 * d32 * d33);
                object2 -= n12;
            }
            if (!(object2 > (int)Math.floor(d23) || bl && streamsConfig.growFromWater())) {
                if (!bl2) continue;
                Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: no-room spill={} floor={} level={} cells={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), String.format("%.1f", d22), String.format("%.1f", d23), object2, arrayList5.size());
                continue;
            }
            if (!bl || !streamsConfig.growFromWater()) {
                object2 = Math.max((int)Math.floor(d23) + 1, object2 - 2);
            }
            if (n16 == 0) {
                int n70 = Math.floorDiv(object2, as);
                n11 = hashMap.getOrDefault(n70, 0);
                double d34 = Math.min(1.0, (d25 - d26) / 5.0);
                double d35 = at / (at + (double)n11);
                d21 = d34 + (1.0 - d34) * d35;
                double d36 = Hashing.unit(l2, lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), n70);
                if (d36 >= d21) {
                    ++n57;
                    continue;
                }
                hashMap.put(n70, n11 + 1);
            }
            int n71 = iterator.size();
            if (streamsConfig.growFromWater()) {
                double[] dArray2;
                Lake lake;
                int n72;
                block184: {
                    int n73;
                    int n74;
                    block183: {
                        int n75;
                        int n76;
                        var105_246 = new ArrayList<Integer>(arrayList5);
                        boolean[] blArray7 = new boolean[n30];
                        Object object9 = arrayList5.iterator();
                        while (object9.hasNext()) {
                            int n77 = (Integer)object9.next();
                            blArray7[n77] = true;
                        }
                        object9 = new ArrayDeque<Integer>(arrayList5);
                        while (!((ArrayDeque)object9).isEmpty()) {
                            int n78 = (Integer)((ArrayDeque)object9).poll();
                            n10 = lowResGrid.gi(n78);
                            int n79 = lowResGrid.gj(n78);
                            for (n76 = 0; n76 < 8; ++n76) {
                                int n80 = n10 + a[n76];
                                int n81 = n79 + b[n76];
                                if (!lowResGrid.valid(n80, n81) || !lowResGrid.inDomain(n75 = lowResGrid.idx(n80, n81)) || !(dArray[n75] - lowResGrid.height(n75) > 0.001) || blArray7[n75]) continue;
                                blArray7[n75] = true;
                                var105_246.add(n75);
                                ((ArrayDeque)object9).add(n75);
                            }
                        }
                        long l8 = System.nanoTime();
                        try {
                            int n82;
                            if (bl && !Boolean.getBoolean("streamsreflowing.nolakeborderpre")) {
                                d21 = var105_246.size() >= 64 ? streamsConfig.borderMargin() + 3.0 * (double)lowResGrid.spacing : streamsConfig.borderMargin() + (double)(lowResGrid.spacing >> 1) + 15.0;
                                boolean bl3 = false;
                                Iterator iterator6 = var105_246.iterator();
                                while (iterator6.hasNext()) {
                                    n75 = (Integer)iterator6.next();
                                    if (!(plateDomain.borderDistance(lowResGrid.worldX(lowResGrid.gi(n75)), lowResGrid.worldZ(lowResGrid.gj(n75))) < d21)) continue;
                                    bl3 = true;
                                    break;
                                }
                                if (bl3) {
                                    ++n61;
                                    if (!bl2) continue;
                                    Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: border-pre fillCells={} reach={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), var105_246.size(), String.format("%.0f", d21));
                                    continue;
                                }
                            }
                            if (!bl) break block183;
                            int n83 = 0;
                            n76 = 0;
                            Iterator iterator7 = var105_246.iterator();
                            while (iterator7.hasNext()) {
                                int n84 = (Integer)iterator7.next();
                                n75 = lowResGrid.gi(n84);
                                n74 = lowResGrid.gj(n84);
                                for (n72 = 0; n72 < 8; ++n72) {
                                    n73 = n75 + a[n72];
                                    n82 = n74 + b[n72];
                                    if (!lowResGrid.valid(n73, n82) || blArray7[lowResGrid.idx(n73, n82)]) continue;
                                    ++n83;
                                    if (!(lowResGrid.height(lowResGrid.idx(n73, n82)) - (double)object2 < 2.5)) continue;
                                    ++n76;
                                }
                            }
                            if (n83 > 0 && n76 * 2 > n83) {
                                ++n61;
                                if (!bl2) continue;
                                Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: stage0 margLow={}/{} level={} fillCells={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), n76, n83, object2, var105_246.size());
                                continue;
                            }
                            int n85 = Math.min(24, Math.max(8, var105_246.size() / 16));
                            int n86 = Math.max(3, n85 / 3);
                            n75 = 0;
                            n74 = 0;
                            n72 = Math.max(1, var105_246.size() / n85);
                            block42: for (n73 = 0; n73 < var105_246.size() && n74 < n85 && n75 < n86; n73 += n72) {
                                n82 = (Integer)var105_246.get(n73);
                                int n87 = lowResGrid.gi(n82);
                                int n88 = lowResGrid.gj(n82);
                                for (int i = 0; i < 8; ++i) {
                                    int n89 = n87 + a[i];
                                    int n90 = n88 + b[i];
                                    if (!lowResGrid.valid(n89, n90) || blArray7[lowResGrid.idx(n89, n90)]) continue;
                                    ++n74;
                                    if (!(terrainSampler.groundHeightExact(lowResGrid.worldX(n89), lowResGrid.worldZ(n90)) < (double)object2)) continue block42;
                                    ++n75;
                                    continue block42;
                                }
                            }
                            if (n75 >= n86) {
                                ++n61;
                                if (!bl2) continue;
                                Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: stage1 wetRim={}/{} probed={} level={} fillCells={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), n75, n86, n74, object2, var105_246.size());
                                continue;
                            }
                        }
                        finally {
                            lArray[2] = lArray[2] + (System.nanoTime() - l8);
                            continue;
                        }
                    }
                    lake = null;
                    dArray2 = new double[2];
                    if (bl) {
                        var112_280 = System.nanoTime();
                        LocalBasin localBasin = NetworkTracer.a(lowResGrid, terrainSampler, var105_246, n18);
                        lArray[3] = lArray[3] + (System.nanoTime() - var112_280);
                        if (localBasin == null) {
                            ++n61;
                            if (!bl2) continue;
                            Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: real-redetect-null fillCells={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), var105_246.size());
                            continue;
                        }
                        n74 = (int)Math.floor(localBasin.floor());
                        n72 = (int)Math.floor(localBasin.spill()) - 2 - n12;
                        ++n60;
                        for (n73 = n72; n73 > n74 && n73 > n72 - 4; --n73) {
                            Object object10;
                            long l9 = System.nanoTime();
                            List<Integer> list = localBasin.poolAt(n73, lowResGrid);
                            lArray[4] = lArray[4] + (System.nanoTime() - l9);
                            if (list == null) continue;
                            dArray2[0] = 0.0;
                            dArray2[1] = 0.0;
                            long l10 = System.nanoTime();
                            Lake lake2 = NetworkTracer.a(list, n73, terrainSampler, lowResGrid, plateDomain, streamsConfig.borderMargin(), dArray2);
                            lArray[5] = lArray[5] + (System.nanoTime() - l10);
                            if (lake2 == null) continue;
                            for (Object object11 = object10 = Math.max(n74 + 1, n73 - 2); object11 <= n73; ++object11) {
                                long l11 = System.nanoTime();
                                List<Integer> list2 = object11 == n73 ? list : localBasin.poolAt((int)object11, lowResGrid);
                                lArray[4] = lArray[4] + (System.nanoTime() - l11);
                                if (list2 == null) continue;
                                if (object11 != n73) {
                                    dArray2[0] = 0.0;
                                    dArray2[1] = 0.0;
                                }
                                long l12 = System.nanoTime();
                                Lake lake3 = object11 == n73 ? lake2 : NetworkTracer.a(list2, object11, terrainSampler, lowResGrid, plateDomain, streamsConfig.borderMargin(), dArray2);
                                lArray[5] = lArray[5] + (System.nanoTime() - l12);
                                if (lake3 == null) continue;
                                lake = lake3;
                                object2 = object11;
                                var105_246 = list2;
                                l7 += (long)list2.size();
                                break block184;
                            }
                            break;
                        }
                    } else {
                        ++n60;
                        l7 += (long)var105_246.size();
                        var112_280 = System.nanoTime();
                        lake = NetworkTracer.a(var105_246, object2, terrainSampler, lowResGrid, plateDomain, streamsConfig.borderMargin(), dArray2);
                        lArray[5] = lArray[5] + (System.nanoTime() - var112_280);
                    }
                }
                if (lake == null) {
                    ++n61;
                    if (!bl2) continue;
                    Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: flood-null level={} fillCells={}", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), object2, var105_246.size());
                    continue;
                }
                double d37 = dArray2[0];
                int n91 = (int)dArray2[1];
                if (bl2 && n91 > 0 && d37 / (double)n91 < 1.5) {
                    Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: film level={} meanDepth={} over {} cells (gate OFF: building anyway)", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), object2, String.format(Locale.ROOT, "%.2f", d37 / (double)n91), n91);
                }
                if (!NetworkTracer.a(lake, 0.9)) {
                    ++n61;
                    if (!bl2 && !Boolean.getBoolean("streamsreflowing.caldiag")) continue;
                    Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: fragmented mask level={} (largest component < 90%)", plateDomain.key(), lowResGrid.worldX(lowResGrid.gi(n64)), lowResGrid.worldZ(lowResGrid.gj(n64)), object2);
                    continue;
                }
                iterator.add(lake);
                Iterator iterator8 = var105_246.iterator();
                while (iterator8.hasNext()) {
                    n72 = (Integer)iterator8.next();
                    if (!(lowResGrid.height(n72) < (double)object2) || !lake.covers(lowResGrid.worldX(lowResGrid.gi(n72)), lowResGrid.worldZ(lowResGrid.gj(n72)))) continue;
                    nArray2[n72] = n71;
                }
            } else {
                iterator.add(NetworkTracer.a(arrayList5, object2, 0, 1, lowResGrid));
                var105_246 = arrayList5.iterator();
                while (var105_246.hasNext()) {
                    int n92 = (Integer)var105_246.next();
                    nArray2[n92] = n71;
                }
            }
            int n93 = nArray[n64];
            if (object3 == 0 || n93 < 0 || nArray2[n93] >= 0 || !NetworkTracer.a(n93, nArray, dArray, n31, d22, 4.0, 48)) continue;
            arrayList4.add(new PendingOutflow(n93, (double)object2, d24));
        }
        int n94 = 0;
        Iterator iterator9 = arrayList.iterator();
        while (iterator9.hasNext()) {
            int n95 = (Integer)iterator9.next();
            if (!blArray4[n95] || nArray2[n95] >= 0 || !(dArray[n95] - lowResGrid.height(n95) > 6.0)) continue;
            blArray4[n95] = false;
            ++n94;
        }
        if (n94 > 0 && bl2) {
            Constants.LOG.info("[SR-LAKEREJ] plate {}: dropped {} channel cells standing >{} above ground on unfilled basins", plateDomain.key(), n94, 6.0);
        }
        if (O) {
            int n96 = 0;
            int n97 = 0;
            d = 0.0;
            int n98 = 0;
            int n99 = 0;
            n18 = 0;
            Iterator iterator10 = arrayList.iterator();
            while (iterator10.hasNext()) {
                int n100 = (Integer)iterator10.next();
                if (!blArray4[n100]) continue;
                ++n96;
                if (nArray2[n100] >= 0) {
                    ++n97;
                    continue;
                }
                double d38 = dArray[n100] - lowResGrid.height(n100);
                if (d38 > 3.0) {
                    ++n18;
                }
                if (!(d38 > d)) continue;
                d = d38;
                n98 = lowResGrid.worldX(lowResGrid.gi(n100));
                n99 = lowResGrid.worldZ(lowResGrid.gj(n100));
            }
            Constants.LOG.info("[SR-FILLSTATS] plate {}: {} river cells ({} on lakes); non-lake fill >3 blocks: {}; deepest {} at ({},{})", plateDomain.key(), n96, n97, n18, String.format(Locale.ROOT, "%.1f", d), n98, n99);
        }
        long l13 = System.nanoTime();
        if (bl2) {
            long l14 = lArray[2] + lArray[3] + lArray[4] + lArray[5];
            Constants.LOG.info("[SR-LAKESTEP] plate {}: probes {} ms, redetect {} ms, poolAt {} ms, flood {} ms (warm {} ms of it), other {} ms of {} ms total", plateDomain.key(), lArray[2] / 1000000L, lArray[3] / 1000000L, lArray[4] / 1000000L, lArray[5] / 1000000L, lArray[7] / 1000000L, (l13 - l6 - l14) / 1000000L, (l13 - l6) / 1000000L);
        }
        d = d13 * 16.0;
        boolean[] blArray8 = new boolean[n30];
        Object object12 = arrayList.iterator();
        while (object12.hasNext()) {
            n18 = (Integer)object12.next();
            if (!blArray4[n18] || nArray2[n18] >= 0 || object5[n18] < d) continue;
            int n101 = nArray[n18];
            double d39 = 0.0;
            if (n101 >= 0) {
                double d40 = NetworkTracer.a(lowResGrid, n18, n101);
                double d41 = d39 = d40 > 1.0E-9 ? Math.abs(dArray[n18] - dArray[n101]) / d40 : 0.0;
            }
            if (!(d39 <= 0.03)) continue;
            blArray8[n18] = true;
        }
        object12 = new boolean[n30];
        ArrayDeque<Integer> arrayDeque3 = new ArrayDeque<Integer>();
        ArrayList<Integer> arrayList6 = new ArrayList<Integer>();
        Object object13 = arrayList.iterator();
        while (object13.hasNext()) {
            int n102;
            int n103;
            int n104;
            n9 = (Integer)object13.next();
            if (!blArray8[n9] || object12[n9] != false) continue;
            arrayList6.clear();
            arrayDeque3.clear();
            object12[n9] = true;
            arrayDeque3.add(n9);
            int n105 = Integer.MAX_VALUE;
            n17 = Integer.MAX_VALUE;
            n16 = Integer.MIN_VALUE;
            int n106 = Integer.MIN_VALUE;
            double d42 = Double.MAX_VALUE;
            d2 = 0.0;
            while (!arrayDeque3.isEmpty()) {
                n104 = (Integer)arrayDeque3.poll();
                arrayList6.add(n104);
                n8 = lowResGrid.gi(n104);
                n103 = lowResGrid.gj(n104);
                n105 = Math.min(n105, n8);
                n16 = Math.max(n16, n8);
                n17 = Math.min(n17, n103);
                n106 = Math.max(n106, n103);
                d42 = Math.min(d42, dArray[n104]);
                d2 = Math.max(d2, (double)object5[n104]);
                for (int i = 0; i < 8; ++i) {
                    object3 = n8 + a[i];
                    object2 = n103 + b[i];
                    if (!lowResGrid.valid((int)object3, (int)object2) || !blArray8[n102 = lowResGrid.idx((int)object3, (int)object2)] || object12[n102] != false) continue;
                    object12[n102] = true;
                    arrayDeque3.add(n102);
                }
            }
            n104 = n16 - n105 + 1;
            n8 = n106 - n17 + 1;
            if (arrayList6.size() < n53 || Math.min(n104, n8) < 3 || (double)arrayList6.size() < 0.3 * (double)n104 * (double)n8 || streamsConfig.growFromWater() || (n103 = (int)Math.floor(d42) - 1) <= n31) continue;
            double d43 = d2 * (double)streamsConfig.gridSpacing() * (double)streamsConfig.gridSpacing();
            object2 = Math.max(1, Math.min(6, (int)Math.round(3.0 + 4.0 * Math.sqrt(d43 / 2500.0))));
            n102 = iterator.size();
            iterator.add(NetworkTracer.a(arrayList6, n103, object2, 0, lowResGrid));
            Iterator iterator11 = arrayList6.iterator();
            while (iterator11.hasNext()) {
                n14 = (Integer)iterator11.next();
                nArray2[n14] = n102;
            }
        }
        object13 = arrayList.iterator();
        while (object13.hasNext()) {
            n9 = (Integer)object13.next();
            if (nArray2[n9] < 0) continue;
            blArray4[n9] = false;
        }
        if (streamsConfig.growFromWater()) {
            object13 = new int[n30];
            Arrays.fill((int[])object13, -1);
            for (n9 = arrayList.size() - 1; n9 >= 0; --n9) {
                int n107 = (Integer)arrayList.get(n9);
                if (!blArray4[n107]) continue;
                n17 = nArray[n107];
                if (n17 >= 0 && nArray2[n17] >= 0) {
                    object13[n107] = nArray2[n17];
                    continue;
                }
                if (n17 < 0 || !blArray4[n17] || !(dArray[n17] > (double)n31)) continue;
                object13[n107] = object13[n17];
            }
            n9 = 0;
            Iterator iterator12 = arrayList.iterator();
            while (iterator12.hasNext()) {
                n17 = (Integer)iterator12.next();
                double d44 = lowResGrid.height(n17);
                if (!blArray4[n17] || object13[n17] < 0 || nArray2[n17] >= 0 || !(d44 > (double)n31) || !(d44 < (double)((Lake)iterator.get((int)object13[n17])).level() - 6.0)) continue;
                blArray4[n17] = false;
                ++n9;
            }
            if (n9 > 0) {
                Constants.LOG.debug("[SR-MERGE] lake-dip cut: {} cells on sub-lake routes removed (subtrees pruned below)", (Object)n9);
            }
        }
        long l15 = System.nanoTime();
        if (streamsConfig.growFromWater()) {
            boolean[] blArray9 = new boolean[n30];
            for (n17 = arrayList.size() - 1; n17 >= 0; --n17) {
                int n108 = (Integer)arrayList.get(n17);
                boolean bl4 = nArray2[n108] >= 0 || dArray[n108] <= (double)n31 || lowResGrid.riverOutlet(n108);
                blArray9[n108] = bl4 || nArray[n108] >= 0 && blArray9[nArray[n108]];
            }
            if (terrainSampler.hasEstimate() && !Boolean.getBoolean("streamsreflowing.norescue")) {
                HashSet<Integer> hashSet = new HashSet<Integer>();
                HashSet<Integer> hashSet2 = new HashSet<Integer>();
                Iterator iterator13 = arrayList.iterator();
                while (iterator13.hasNext()) {
                    int n109 = (Integer)iterator13.next();
                    if (!blArray4[n109] || blArray9[n109]) continue;
                    n15 = n109;
                    int n110 = n30;
                    while (nArray[n15] >= 0 && n110-- > 0) {
                        n15 = nArray[n15];
                    }
                    if (!hashSet.add(n15) || dArray[n15] > (double)n31 + 3.0) continue;
                    n7 = lowResGrid.gi(n15);
                    int n111 = lowResGrid.gj(n15);
                    n8 = 0;
                    block61: for (int i = -2; i <= 2; ++i) {
                        for (int j = -2; j <= 2; ++j) {
                            object3 = n7 + j;
                            object2 = n111 + i;
                            if (!lowResGrid.valid((int)object3, (int)object2) || !(lowResGrid.height(lowResGrid.idx((int)object3, (int)object2)) <= (double)(n31 - 2))) continue;
                            n8 = 1;
                            break block61;
                        }
                    }
                    if (n8 == 0) continue;
                    hashSet2.add(n15);
                }
                if (!hashSet2.isEmpty()) {
                    for (int i = arrayList.size() - 1; i >= 0; --i) {
                        int n112 = (Integer)arrayList.get(i);
                        blArray9[n112] = blArray9[n112] || hashSet2.contains(n112) || nArray[n112] >= 0 && blArray9[nArray[n112]];
                    }
                }
            }
            Iterator iterator14 = arrayList.iterator();
            while (iterator14.hasNext()) {
                int n113 = (Integer)iterator14.next();
                if (!blArray4[n113] || blArray9[n113]) continue;
                blArray4[n113] = false;
            }
        }
        long l16 = System.nanoTime();
        int[] nArray3 = new int[n30];
        Arrays.fill(nArray3, -1);
        for (int i = arrayList.size() - 1; i >= 0; --i) {
            int n114 = (Integer)arrayList.get(i);
            if (!blArray4[n114]) continue;
            nArray3[n114] = nArray[n114] >= 0 && blArray4[nArray[n114]] ? nArray3[nArray[n114]] : n114;
        }
        HashMap<Integer, Integer> hashMap2 = new HashMap<Integer, Integer>();
        Object object14 = arrayList.iterator();
        while (object14.hasNext()) {
            n15 = (Integer)object14.next();
            if (!blArray4[n15]) continue;
            hashMap2.merge(nArray3[n15], 1, Integer::sum);
        }
        object14 = new boolean[n30];
        Iterator iterator15 = arrayList.iterator();
        block67: while (iterator15.hasNext()) {
            int n115;
            int n116;
            int n117 = (Integer)iterator15.next();
            if (!blArray4[n117]) continue;
            n7 = nArray[n117];
            if (n7 < 0 || nArray2[n7] >= 0 || dArray[n7] <= (double)n31) {
                if (K == null || !NetworkTracer.b((double)lowResGrid.worldX(lowResGrid.gi(n117)), lowResGrid.worldZ(lowResGrid.gj(n117)))) continue;
                for (n116 = 0; n116 < 8; ++n116) {
                    n8 = lowResGrid.gi(n117) + a[n116];
                    if (!lowResGrid.valid(n8, n115 = lowResGrid.gj(n117) + b[n116]) || !blArray4[lowResGrid.idx(n8, n115)] || nArray3[lowResGrid.idx(n8, n115)] == nArray3[n117]) continue;
                    Constants.LOG.info("[SR-SEGDUMP] 4b6 contact EXEMPT (own cell is mouth): cell ({},{}) sys {} touches cell ({},{}) sys {}", lowResGrid.worldX(lowResGrid.gi(n117)), lowResGrid.worldZ(lowResGrid.gj(n117)), nArray3[n117], lowResGrid.worldX(n8), lowResGrid.worldZ(n115), nArray3[lowResGrid.idx(n8, n115)]);
                }
                continue;
            }
            n116 = nArray3[n117];
            n8 = (Integer)hashMap2.get(n116);
            n115 = lowResGrid.gi(n117);
            int n118 = lowResGrid.gj(n117);
            for (object3 = 0; object3 < 8; ++object3) {
                int n119;
                object2 = n115 + a[object3];
                int n120 = n118 + b[object3];
                if (!lowResGrid.valid((int)object2, n120) || !lowResGrid.inDomain(n119 = lowResGrid.idx((int)object2, n120)) || !blArray4[n119] || (n14 = nArray3[n119]) == n116) continue;
                n13 = nArray[n119];
                if (n13 < 0 || nArray2[n13] >= 0 || dArray[n13] <= (double)n31) {
                    if (K == null || !NetworkTracer.b((double)lowResGrid.worldX(n115), lowResGrid.worldZ(n118))) continue;
                    Constants.LOG.info("[SR-SEGDUMP] 4b6 contact EXEMPT (neighbour is mouth): cell ({},{}) sys {} touches cell ({},{}) sys {}", lowResGrid.worldX(n115), lowResGrid.worldZ(n118), n116, lowResGrid.worldX(lowResGrid.gi(n119)), lowResGrid.worldZ(lowResGrid.gj(n119)), n14);
                    continue;
                }
                int n121 = (Integer)hashMap2.get(n14);
                if (K != null && NetworkTracer.b((double)lowResGrid.worldX(n115), lowResGrid.worldZ(n118))) {
                    Constants.LOG.info("[SR-SEGDUMP] 4b6 contact: cell ({},{}) sys {} size {} vs cell ({},{}) sys {} size {} -> {}", lowResGrid.worldX(n115), lowResGrid.worldZ(n118), n116, n8, lowResGrid.worldX(lowResGrid.gi(n119)), lowResGrid.worldZ(lowResGrid.gj(n119)), n14, n121, n8 < n121 || n8 == n121 && n116 > n14 ? "DROP this cell" : "keep");
                }
                if (n8 >= n121 && (n8 != n121 || n116 <= n14)) continue;
                object14[n117] = true;
                continue block67;
            }
        }
        int n122 = 0;
        Object object15 = arrayList.iterator();
        while (object15.hasNext()) {
            n7 = (Integer)object15.next();
            if (object14[n7] == false) continue;
            blArray4[n7] = false;
            ++n122;
        }
        if (n122 > 0) {
            object15 = new boolean[n30];
            for (n7 = arrayList.size() - 1; n7 >= 0; --n7) {
                int n123 = (Integer)arrayList.get(n7);
                if (!blArray4[n123]) continue;
                n8 = nArray[n123];
                boolean bl5 = n8 >= 0 && (nArray2[n8] >= 0 || dArray[n8] <= (double)n31);
                object15[n123] = bl5 || n8 >= 0 && blArray4[n8] && object15[n8] != false;
            }
            n7 = 0;
            Iterator iterator16 = arrayList.iterator();
            while (iterator16.hasNext()) {
                n8 = (Integer)iterator16.next();
                if (!blArray4[n8] || object15[n8] != false) continue;
                blArray4[n8] = false;
                ++n7;
            }
            Constants.LOG.debug("[SR-MERGE] local separation: trimmed {} contact cells, pruned {} orphaned", (Object)n122, (Object)n7);
        }
        nArray3 = new int[n30];
        Arrays.fill(nArray3, -1);
        for (int i = arrayList.size() - 1; i >= 0; --i) {
            int n124 = (Integer)arrayList.get(i);
            if (!blArray4[n124]) continue;
            nArray3[n124] = nArray[n124] >= 0 && blArray4[nArray[n124]] ? nArray3[nArray[n124]] : n124;
        }
        Object object16 = new int[n30];
        Iterator iterator17 = arrayList.iterator();
        while (iterator17.hasNext()) {
            n122 = (Integer)iterator17.next();
            if (!blArray4[n122] || nArray[n122] < 0 || !blArray4[nArray[n122]]) continue;
            Object object17 = object16;
            int n125 = nArray[n122];
            object17[n125] = object17[n125] + true;
        }
        int n126 = 0;
        n122 = 0;
        for (int i = 0; i < arrayList.size(); ++i) {
            int n127;
            int n128;
            n7 = (Integer)arrayList.get(i);
            if (!blArray4[n7] || (n128 = nArray[n7]) < 0 || nArray2[n128] >= 0 || dArray[n128] <= (double)n31) continue;
            n8 = lowResGrid.gi(n128) - lowResGrid.gi(n7);
            int n129 = lowResGrid.gj(n128) - lowResGrid.gj(n7);
            int n130 = lowResGrid.gi(n7);
            object3 = lowResGrid.gj(n7);
            object2 = -1;
            double d45 = dArray[n7];
            for (n14 = 0; n14 < 8; ++n14) {
                int n131;
                int n132;
                n13 = n130 + a[n14];
                n127 = object3 + b[n14];
                if (!lowResGrid.valid(n13, n127) || !lowResGrid.inDomain((int)(object = lowResGrid.idx(n13, n127))) || !blArray4[object] || nArray3[object] != nArray3[n7] || dArray[object] >= dArray[n7] || object == n128 || (n132 = nArray[object]) < 0 || n8 * (n6 = lowResGrid.gi(n132) - lowResGrid.gi((int)object)) + n129 * (n131 = lowResGrid.gj(n132) - lowResGrid.gj((int)object)) <= 0 || a[n14] * n8 + b[n14] * n129 < 0) continue;
                boolean bl6 = false;
                n12 = n128;
                for (int j = 0; j < 24 && n12 >= 0; ++j) {
                    if (n12 == object) {
                        bl6 = true;
                        break;
                    }
                    n12 = nArray[n12];
                }
                if (bl6 || !(dArray[object] < d45)) continue;
                d45 = dArray[object];
                object2 = object;
            }
            if (object2 < 0) continue;
            n14 = nArray[n7];
            Object object18 = object16;
            int n133 = n14;
            object18[n133] = object18[n133] - true;
            nArray[n7] = object2;
            Object object19 = object16;
            int n134 = object2;
            object19[n134] = object19[n134] + true;
            ++n126;
            n13 = n14;
            while (n13 >= 0 && blArray4[n13] && object16[n13] == false) {
                blArray4[n13] = false;
                n127 = nArray[n13];
                if (n127 >= 0 && blArray4[n127]) {
                    Object object20 = object16;
                    int n135 = n127;
                    object20[n135] = object20[n135] - true;
                }
                n13 = n127;
                ++n122;
            }
        }
        if (n126 > 0) {
            Constants.LOG.debug("[SR-MERGE] branch fuse: {} parallel branches fused, {} redundant cells trimmed", (Object)n126, (Object)n122);
        }
        nArray3 = new int[n30];
        object16 = arrayList.iterator();
        while (object16.hasNext()) {
            n126 = (Integer)object16.next();
            if (!blArray4[n126] || nArray[n126] < 0) continue;
            int n136 = nArray[n126];
            nArray3[n136] = nArray3[n136] + 1;
        }
        long l17 = System.nanoTime();
        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-ROUTE] plate {}: pflood {} ms, d8+thin {} ms, lakes {} ms, ponds {} ms, reach {} ms, trim {} ms", plateDomain.key(), (l5 - l4) / 1000000L, (l6 - l5) / 1000000L, (l13 - l6) / 1000000L, (l15 - l13) / 1000000L, (l16 - l15) / 1000000L, (l17 - l16) / 1000000L);
        }
        for (n122 = 0; n122 < P.length(); ++n122) {
            P.set(n122, 0L);
        }
        ArrayList<RiverSegment> arrayList7 = new ArrayList<RiverSegment>();
        ArrayList<Integer> arrayList8 = new ArrayList<Integer>();
        Object object21 = arrayList.iterator();
        while (object21.hasNext()) {
            int n137 = (Integer)object21.next();
            if (!blArray4[n137] || nArray3[n137] == 1) continue;
            arrayList8.add(n137);
        }
        object21 = new int[n30];
        Arrays.fill((int[])object21, -1);
        for (int i = arrayList.size() - 1; i >= 0; --i) {
            n8 = (Integer)arrayList.get(i);
            if (!blArray4[n8]) continue;
            object21[n8] = nArray[n8] >= 0 && blArray4[nArray[n8]] ? (int)object21[nArray[n8]] : n8;
        }
        HashMap<Integer, Lake> hashMap3 = new HashMap<Integer, Lake>();
        Object object22 = arrayList8.iterator();
        while (object22.hasNext()) {
            int n138 = (Integer)object22.next();
            Object object23 = object21[n138];
            if (object23 < 0 || hashMap3.containsKey((int)object23)) continue;
            object3 = nArray[object23];
            hashMap3.put((int)object23, object3 >= 0 && nArray2[object3] >= 0 ? (Lake)iterator.get(nArray2[object3]) : null);
        }
        object22 = new ArrayList();
        if (streamsConfig.growFromWater()) {
            listArray = new List[arrayList8.size()];
            LowResGrid.parallelFor(arrayList8.size(), arg_0 -> NetworkTracer.a(listArray, arrayList8, nArray, nArray2, iterator, hashMap3, (int[])object21, blArray4, nArray3, dArray, (double[])object5, lowResGrid, terrainSampler, wetnessSampler, streamsConfig, l2, n31, arg_0));
            for (int i = 0; i < listArray.length; ++i) {
                object3 = object21[(Integer)arrayList8.get(i)];
                for (object2 = 0; object2 < listArray[i].size(); ++object2) {
                    object22.add(object3);
                }
                arrayList7.addAll(listArray[i]);
            }
        } else {
            listArray = arrayList8.iterator();
            while (listArray.hasNext()) {
                int n139 = (Integer)listArray.next();
                List<RiverSegment> list = NetworkTracer.a(n139, nArray, nArray2, iterator, (Lake)hashMap3.get((int)object21[n139]), blArray4, nArray3, dArray, (double[])object5, lowResGrid, terrainSampler, wetnessSampler, streamsConfig, l2, n31);
                object2 = object21[n139];
                for (int i = 0; i < list.size(); ++i) {
                    object22.add(object2);
                }
                arrayList7.addAll(list);
            }
        }
        listArray = new List[arrayList4.size()];
        LowResGrid.parallelFor(arrayList4.size(), n2 -> {
            ArrayList<RiverSegment> arrayList = new ArrayList<RiverSegment>();
            NetworkTracer.a((PendingOutflow)arrayList4.get(n2), lowResGrid, nArray, nArray2, iterator, dArray, n31, terrainSampler, streamsConfig, l2, arrayList);
            listArray[n2] = arrayList;
        });
        for (int i = 0; i < listArray.length; ++i) {
            PendingOutflow pendingOutflow = (PendingOutflow)arrayList4.get(i);
            object2 = pendingOutflow.outlet() >= 0 && object21[pendingOutflow.outlet()] >= 0 ? (Object)object21[pendingOutflow.outlet()] : -(pendingOutflow.outlet() + 2);
            for (RiverSegment riverSegment : listArray[i] == null ? List.of() : listArray[i]) {
                arrayList7.add(riverSegment);
                object22.add(object2);
            }
        }
        long l18 = System.nanoTime();
        boolean[] blArray10 = new boolean[iterator.size()];
        NetworkTracer.a(arrayList7, (List<Integer>)object22, iterator, blArray10, n31, streamsConfig, terrainSampler);
        Object[] objectArray = new double[arrayList7.size()];
        int[] nArray4 = new int[arrayList7.size()];
        for (n14 = 0; n14 <= iterator.size() && !arrayList7.isEmpty(); ++n14) {
            NetworkTracer.a(arrayList7, iterator, blArray10, objectArray, nArray4);
            int[] nArray5 = NetworkTracer.chainIds(arrayList7);
            HashSet<Long> hashSet = new HashSet<Long>();
            for (RiverSegment riverSegment : arrayList7) {
                hashSet.add(NetworkTracer.a(riverSegment.x2(), riverSegment.z2()));
            }
            object = 0;
            int n140 = Integer.MIN_VALUE;
            for (n6 = 0; n6 < arrayList7.size(); ++n6) {
                double d46;
                RiverSegment riverSegment;
                if (nArray5[n6] == n140) continue;
                n140 = nArray5[n6];
                if (nArray4[n6] < 0 || blArray10[nArray4[n6]] || hashSet.contains(NetworkTracer.a((riverSegment = (RiverSegment)arrayList7.get(n6)).x1(), riverSegment.z1())) || !((d46 = riverSegment.y1() - (double)RiverEngine.insetFor(riverSegment.w1(), streamsConfig)) < objectArray[n6] - 0.01)) continue;
                blArray10[nArray4[n6]] = true;
                object = 1;
                Lake lake = (Lake)iterator.get(nArray4[n6]);
                if (!Boolean.getBoolean("streamsreflowing.caldiag") && K == null) continue;
                Constants.LOG.info("[SR-LAKEKILL] plate {}: lake at {},{} level {} killed: a contacting stream's head surface {} is below it (cannot be grade-matched)", plateDomain.key(), lake.cellMinX(), lake.cellMinZ(), lake.level(), String.format(Locale.ROOT, "%.1f", d46));
            }
            if (object == 0) break;
            NetworkTracer.a(arrayList7, (List<Integer>)object22, iterator, blArray10, n31, streamsConfig, terrainSampler);
            objectArray = new double[arrayList7.size()];
            nArray4 = new int[arrayList7.size()];
        }
        objectArray = new int[object22.size()];
        for (int i = 0; i < objectArray.length; ++i) {
            objectArray[i] = ((Integer)object22.get(i)).intValue();
        }
        HashMap<Integer, Integer> hashMap4 = new HashMap<Integer, Integer>();
        int[] nArray6 = new int[objectArray.length];
        for (int i = 0; i < objectArray.length; ++i) {
            double d47 = objectArray[i];
            if (d47 < 0) {
                nArray6[i] = Integer.MIN_VALUE;
                continue;
            }
            Integer n141 = (Integer)hashMap4.get((int)d47);
            if (n141 == null) {
                int n142 = nArray[d47];
                n141 = n142 >= 0 && nArray2[n142] >= 0 && !blArray10[nArray2[n142]] ? ((Lake)iterator.get(nArray2[n142])).level() : Integer.MIN_VALUE;
                hashMap4.put((int)d47, n141);
            }
            nArray6[i] = n141 == Integer.MIN_VALUE ? Integer.MIN_VALUE : n141 + RiverEngine.insetFor((((RiverSegment)arrayList7.get(i)).w1() + ((RiverSegment)arrayList7.get(i)).w2()) * 0.5, streamsConfig);
        }
        Object[] objectArray2 = new double[arrayList7.size()];
        int[] nArray7 = new int[arrayList7.size()];
        NetworkTracer.a(arrayList7, iterator, blArray10, objectArray2, nArray7);
        for (int i = 0; i < arrayList7.size(); ++i) {
            if (nArray7[i] < 0) continue;
            RiverSegment riverSegment = (RiverSegment)arrayList7.get(i);
            nArray6[i] = Math.max(nArray6[i], (int)Math.round(objectArray2[i]) + RiverEngine.insetFor((riverSegment.w1() + riverSegment.w2()) * 0.5, streamsConfig));
        }
        objectArray2 = NetworkTracer.chainIds(arrayList7);
        double d48 = Math.max(8.0, streamsConfig.maxWidth() * 2.0);
        HashMap<Long, List<Integer>> hashMap5 = new HashMap<Long, List<Integer>>();
        for (n6 = 0; n6 < arrayList7.size(); ++n6) {
            RiverSegment riverSegment = (RiverSegment)arrayList7.get(n6);
            int n143 = (int)Math.floor(riverSegment.minX() / d48);
            n12 = (int)Math.floor(riverSegment.maxX() / d48);
            int n144 = (int)Math.floor(riverSegment.minZ() / d48);
            n11 = (int)Math.floor(riverSegment.maxZ() / d48);
            for (int i = n143; i <= n12; ++i) {
                for (int j = n144; j <= n11; ++j) {
                    hashMap5.computeIfAbsent((long)i << 32 ^ (long)j & 0xFFFFFFFFL, l -> new ArrayList()).add(n6);
                }
            }
        }
        for (n6 = 0; n6 < 4; ++n6) {
            boolean bl7 = NetworkTracer.a(arrayList7, (int[])objectArray, nArray6, streamsConfig, terrainSampler, (int[])objectArray2, hashMap5, d48);
            for (int i = 0; i < arrayList7.size(); ++i) {
                RiverSegment riverSegment;
                n12 = nArray6[i];
                if (n12 == Integer.MIN_VALUE || !((riverSegment = (RiverSegment)arrayList7.get(i)).y1() < (double)n12) && !(riverSegment.y2() < (double)n12)) continue;
                if (K != null && NetworkTracer.b((riverSegment.x1() + riverSegment.x2()) * 0.5, (riverSegment.z1() + riverSegment.z2()) * 0.5)) {
                    Constants.LOG.info("[SR-SEGDUMP] modifier lakeFloor RAISE seg {} y {}..{} -> floor {}", i, String.format(Locale.ROOT, "%.1f", riverSegment.y1()), String.format(Locale.ROOT, "%.1f", riverSegment.y2()), n12);
                }
                arrayList7.set(i, new RiverSegment(riverSegment.x1(), riverSegment.z1(), Math.max(riverSegment.y1(), (double)n12), riverSegment.w1(), riverSegment.x2(), riverSegment.z2(), Math.max(riverSegment.y2(), (double)n12), riverSegment.w2()));
            }
            NetworkTracer.a(arrayList7, nArray6);
            if (!bl7) break;
        }
        if (K != null) {
            NetworkTracer.a(plateDomain, arrayList7, (int[])objectArray, iterator);
        }
        if (I != null) {
            NetworkTracer.a(I[0], I[1], arrayList, lowResGrid, dArray, nArray, (double[])object5, blArray4, nArray2, n31, plateDomain);
        }
        if (Boolean.getBoolean("streamsreflowing.caldiag") || O) {
            long l19 = System.nanoTime();
            Constants.LOG.info("[SR-PHASES] plate {}: fill {} ms, route {} ms, chains+lakes {} ms, post {} ms ({} segs, sampler {}/{})", plateDomain.key(), (l4 - l3) / 1000000L, (l17 - l4) / 1000000L, (l18 - l17) / 1000000L, (l19 - l18) / 1000000L, arrayList7.size(), LowResGrid.samplerWidth(), LowResGrid.samplerFullWidth());
            StringBuilder stringBuilder = new StringBuilder();
            for (n12 = 0; n12 < W.length; ++n12) {
                stringBuilder.append(' ').append(W[n12]).append(' ').append(P.get(n12) / 1000000L).append("ms");
            }
            Constants.LOG.info("[SR-CHAINSTEP] plate {} (CPU across the parallel chain tasks, read as proportions):{}", (Object)plateDomain.key(), (Object)stringBuilder);
            double d49 = 0.0;
            double d50 = 0.0;
            double d51 = 0.0;
            for (RiverSegment riverSegment : arrayList7) {
                d49 += Math.hypot(riverSegment.x2() - riverSegment.x1(), riverSegment.z2() - riverSegment.z1());
                d50 += (riverSegment.y1() + riverSegment.y2()) * 0.5;
                d51 += (riverSegment.w1() + riverSegment.w2()) * 0.5;
            }
            n10 = Math.max(1, arrayList7.size());
            Constants.LOG.info("[SR-QUALITY] plate {}: segs {}, length {}, meanGrade {}, meanWidth {}, lakes {}", plateDomain.key(), arrayList7.size(), String.format(Locale.ROOT, "%.1f", d49), String.format(Locale.ROOT, "%.3f", d50 / (double)n10), String.format(Locale.ROOT, "%.3f", d51 / (double)n10), iterator.size());
            Constants.LOG.info("[SR-LAKES] plate {}: basins {} oversize {} depthGated {} qualified {} thinned {} fineFloods {} ({} coarse cells, {} rejected) built {}", plateDomain.key(), n54, n55, n58, n56, n57, n60, l7, n61, iterator.size());
        }
        if ((n5 = NetworkTracer.b(arrayList7, streamsConfig)) > 0 && Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-MERGE] plate {}: absorbed {} thin engulfed crossing segment(s)", (Object)plateDomain.key(), (Object)n5);
        }
        if ((n4 = NetworkTracer.a(arrayList7, streamsConfig)) > 0 && Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-WIDTH] plate {}: relaxed {} waist segment(s) to half the upstream max", (Object)plateDomain.key(), (Object)n4);
        }
        for (n3 = iterator.size() - 1; n3 >= 0; --n3) {
            if (!blArray10[n3]) continue;
            iterator.remove(n3);
        }
        for (n3 = iterator.size() - 1; n3 >= 0; --n3) {
            Lake lake = (Lake)iterator.get(n3);
            if (lake.digDepth() > 0) continue;
            boolean bl8 = false;
            for (RiverSegment riverSegment : arrayList7) {
                int n145;
                int n146;
                double d52 = (riverSegment.w1() + riverSegment.w2()) * 0.5;
                if (d52 <= 3.0 || Math.min(riverSegment.y1(), riverSegment.y2()) >= (double)(lake.level() - 3) || !lake.coversWithin(n10 = (int)Math.round((riverSegment.x1() + riverSegment.x2()) * 0.5), n146 = (int)Math.round((riverSegment.z1() + riverSegment.z2()) * 0.5), n145 = (int)Math.ceil(d52 / (double)Math.max(1, lake.spacing())))) continue;
                if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                    Constants.LOG.info("[SR-LAKEREJ] plate {} at {},{}: drained-through level={} by w={} channel y={} at {},{}", plateDomain.key(), lake.cellMinX(), lake.cellMinZ(), lake.level(), String.format(Locale.ROOT, "%.1f", d52), String.format(Locale.ROOT, "%.1f", Math.min(riverSegment.y1(), riverSegment.y2())), n10, n146);
                }
                bl8 = true;
                break;
            }
            if (!bl8) continue;
            iterator.remove(n3);
        }
        return NetworkTracer.assemble(plateDomain.key(), arrayList7, iterator, streamsConfig);
    }

    private static void a(List<RiverSegment> list, int[] nArray) {
        double d;
        int n;
        int n2;
        int n3;
        int n4;
        Object object;
        int n5;
        int n6 = list.size();
        if (n6 == 0) {
            return;
        }
        HashMap<Long, Integer> hashMap = new HashMap<Long, Integer>();
        ArrayList<Double> arrayList = new ArrayList<Double>();
        int[] nArray2 = new int[n6];
        int[] nArray3 = new int[n6];
        for (n5 = 0; n5 < n6; ++n5) {
            object = list.get(n5);
            nArray2[n5] = NetworkTracer.a(((RiverSegment)object).x1(), ((RiverSegment)object).z1(), ((RiverSegment)object).y1(), hashMap, arrayList);
            nArray3[n5] = NetworkTracer.a(((RiverSegment)object).x2(), ((RiverSegment)object).z2(), ((RiverSegment)object).y2(), hashMap, arrayList);
        }
        n5 = arrayList.size();
        object = new double[n5];
        Arrays.fill((double[])object, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < n6; ++i) {
            if (nArray[i] == Integer.MIN_VALUE) continue;
            object[nArray2[i]] = Math.max((double)object[nArray2[i]], (double)nArray[i]);
            object[nArray3[i]] = Math.max((double)object[nArray3[i]], (double)nArray[i]);
        }
        int[] nArray4 = new int[n5];
        ArrayList arrayList2 = new ArrayList(n5);
        for (n4 = 0; n4 < n5; ++n4) {
            arrayList2.add(new ArrayList());
        }
        for (n4 = 0; n4 < n6; ++n4) {
            if (nArray2[n4] == nArray3[n4]) continue;
            ((List)arrayList2.get(nArray2[n4])).add(nArray3[n4]);
            int n7 = nArray3[n4];
            nArray4[n7] = nArray4[n7] + 1;
        }
        double[] dArray = new double[n5];
        for (int i = 0; i < n5; ++i) {
            dArray[i] = Math.max((Double)arrayList.get(i), (double)object[i]);
        }
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        for (n3 = 0; n3 < n5; ++n3) {
            if (nArray4[n3] != 0) continue;
            arrayDeque.add(n3);
        }
        while (!arrayDeque.isEmpty()) {
            n3 = (Integer)arrayDeque.poll();
            Iterator iterator = ((List)arrayList2.get(n3)).iterator();
            while (iterator.hasNext()) {
                n2 = (Integer)iterator.next();
                if (dArray[n3] < dArray[n2]) {
                    dArray[n2] = Math.max(dArray[n3], (double)object[n2]);
                }
                int n8 = n2;
                nArray4[n8] = nArray4[n8] - 1;
                if (nArray4[n8] != 0) continue;
                arrayDeque.add(n2);
            }
        }
        n3 = 1;
        for (n = 0; n3 != 0 && n < 64; ++n) {
            n3 = 0;
            for (n2 = 0; n2 < n6; ++n2) {
                if (nArray2[n2] == nArray3[n2] || !(dArray[nArray2[n2]] < dArray[nArray3[n2]]) || !((d = Math.max(dArray[nArray2[n2]], (double)object[nArray3[n2]])) < dArray[nArray3[n2]])) continue;
                dArray[nArray3[n2]] = d;
                n3 = 1;
            }
        }
        for (n = 0; n < n6; ++n) {
            RiverSegment riverSegment = list.get(n);
            d = dArray[nArray2[n]];
            double d2 = Math.min(d, dArray[nArray3[n]]);
            if (d == riverSegment.y1() && d2 == riverSegment.y2()) continue;
            list.set(n, new RiverSegment(riverSegment.x1(), riverSegment.z1(), d, riverSegment.w1(), riverSegment.x2(), riverSegment.z2(), d2, riverSegment.w2()));
        }
    }

    private static void a(List<RiverSegment> list) {
        Object object;
        int n;
        Object object2;
        int n2;
        int n3 = list.size();
        if (n3 == 0) {
            return;
        }
        HashMap<Long, Integer> hashMap = new HashMap<Long, Integer>();
        ArrayList<Double> arrayList = new ArrayList<Double>();
        int[] nArray = new int[n3];
        int[] nArray2 = new int[n3];
        for (n2 = 0; n2 < n3; ++n2) {
            object2 = list.get(n2);
            nArray[n2] = NetworkTracer.a(((RiverSegment)object2).x1(), ((RiverSegment)object2).z1(), ((RiverSegment)object2).y1(), hashMap, arrayList);
            nArray2[n2] = NetworkTracer.a(((RiverSegment)object2).x2(), ((RiverSegment)object2).z2(), ((RiverSegment)object2).y2(), hashMap, arrayList);
        }
        n2 = arrayList.size();
        object2 = new ArrayList(n2);
        for (int i = 0; i < n2; ++i) {
            object2.add(new ArrayList());
        }
        int[] nArray3 = new int[n2];
        for (int i = 0; i < n3; ++i) {
            if (nArray[i] == nArray2[i]) continue;
            ((List)object2.get(nArray2[i])).add(i);
            int n4 = nArray[i];
            nArray3[n4] = nArray3[n4] + 1;
        }
        double[] dArray = new double[n2];
        for (int i = 0; i < n2; ++i) {
            dArray[i] = (Double)arrayList.get(i);
        }
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        for (n = 0; n < n2; ++n) {
            if (nArray3[n] != 0) continue;
            arrayDeque.add(n);
        }
        while (!arrayDeque.isEmpty()) {
            n = (Integer)arrayDeque.poll();
            object = ((List)object2.get(n)).iterator();
            while (object.hasNext()) {
                int n5 = (Integer)object.next();
                int n6 = nArray[n5];
                RiverSegment riverSegment = list.get(n5);
                double d = Math.hypot(riverSegment.x2() - riverSegment.x1(), riverSegment.z2() - riverSegment.z1());
                double d2 = dArray[n] + 1.0 * d;
                if (d2 < dArray[n6]) {
                    dArray[n6] = d2;
                }
                int n7 = n6;
                nArray3[n7] = nArray3[n7] - 1;
                if (nArray3[n7] != 0) continue;
                arrayDeque.add(n6);
            }
        }
        for (n = 0; n < n3; ++n) {
            object = list.get(n);
            double d = Math.min(((RiverSegment)object).y1(), dArray[nArray[n]]);
            double d3 = Math.min(d, Math.min(((RiverSegment)object).y2(), dArray[nArray2[n]]));
            if (d == ((RiverSegment)object).y1() && d3 == ((RiverSegment)object).y2()) continue;
            list.set(n, new RiverSegment(((RiverSegment)object).x1(), ((RiverSegment)object).z1(), d, ((RiverSegment)object).w1(), ((RiverSegment)object).x2(), ((RiverSegment)object).z2(), d3, ((RiverSegment)object).w2()));
        }
    }

    private static int a(double d, double d2, double d3, Map<Long, Integer> map, List<Double> list) {
        long l;
        long l2 = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l3 = l2 << 32 | (l = Math.round(d2 * 16.0) & 0xFFFFFFFFL);
        Integer n = map.get(l3);
        if (n == null) {
            map.put(l3, list.size());
            list.add(d3);
            return list.size() - 1;
        }
        if (d3 < list.get(n)) {
            list.set(n, d3);
        }
        return n;
    }

    private static boolean a(List<RiverSegment> list, int[] nArray, int[] nArray2, StreamsConfig streamsConfig, TerrainSampler terrainSampler, int[] nArray3, Map<Long, List<Integer>> map, double d) {
        int n2;
        int n3 = list.size();
        if (n3 < 2) {
            return false;
        }
        ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
        double d2 = Math.max(streamsConfig.maxWidth() * 2.0 + 2.0, 40.0);
        double[] dArray = new double[n3];
        for (n2 = 0; n2 < n3; ++n2) {
            dArray[n2] = list.get(n2).y2();
        }
        LowResGrid.parallelFor(n3, n -> {
            RiverSegment riverSegment = (RiverSegment)list.get(n);
            double d3 = (riverSegment.x1() + riverSegment.x2()) * 0.5;
            double d4 = (riverSegment.z1() + riverSegment.z2()) * 0.5;
            double d5 = riverSegment.widthAt(0.5);
            double d6 = Double.NaN;
            int n2 = (int)Math.floor((d3 - d2) / d);
            int n3 = (int)Math.floor((d3 + d2) / d);
            int n4 = (int)Math.floor((d4 - d2) / d);
            int n5 = (int)Math.floor((d4 + d2) / d);
            for (int i = n2; i <= n3; ++i) {
                for (int j = n4; j <= n5; ++j) {
                    List list2 = (List)map.get((long)i << 32 ^ (long)j & 0xFFFFFFFFL);
                    if (list2 == null) continue;
                    Iterator iterator = list2.iterator();
                    while (iterator.hasNext()) {
                        double d7;
                        double d8;
                        double d9;
                        int n6 = (Integer)iterator.next();
                        if (nArray3[n6] == nArray3[n]) continue;
                        RiverSegment riverSegment2 = (RiverSegment)list.get(n6);
                        double d10 = riverSegment2.distanceAt(d3, d4, d9 = riverSegment2.clampParam(d3, d4));
                        if (d10 > d5 + (d8 = riverSegment2.widthAt(d9)) + 2.0) {
                            double d11;
                            d7 = d5 + Math.max(5.0, d5 * streamsConfig.bankWidth()) + d8 + Math.max(5.0, d8 * streamsConfig.bankWidth());
                            if (d10 > Math.min(40.0, d7) || (d11 = riverSegment2.gradeAt(d9)) >= riverSegment.y2() - 1.0 || !Double.isNaN(d6) && d11 >= d6) continue;
                            double d12 = riverSegment2.x1() + (riverSegment2.x2() - riverSegment2.x1()) * d9;
                            double d13 = riverSegment2.z1() + (riverSegment2.z2() - riverSegment2.z1()) * d9;
                            double d14 = Double.NEGATIVE_INFINITY;
                            for (int k = 1; k <= 3; ++k) {
                                double d15 = (double)k / 4.0;
                                d14 = Math.max(d14, NetworkTracer.a(terrainSampler, concurrentHashMap, d3 + (d12 - d3) * d15, d4 + (d13 - d4) * d15));
                            }
                            if (d14 >= riverSegment.y2() + 5.0) continue;
                            d6 = d11;
                            continue;
                        }
                        d7 = riverSegment2.gradeAt(d9);
                        if (!(d7 < riverSegment.y2() - 1.0) || !Double.isNaN(d6) && !(d7 < d6)) continue;
                        d6 = d7;
                    }
                }
            }
            if (!Double.isNaN(d6)) {
                dArray[n] = Math.min(dArray[n], d6);
            }
        });
        n2 = 0;
        for (int i = 0; i < n3; ++i) {
            double d3;
            RiverSegment riverSegment = list.get(i);
            double d4 = d3 = nArray2[i] == Integer.MIN_VALUE ? dArray[i] : Math.max(dArray[i], (double)nArray2[i]);
            if (!(d3 < riverSegment.y2())) continue;
            n2 = 1;
            if (K != null && NetworkTracer.b((riverSegment.x1() + riverSegment.x2()) * 0.5, (riverSegment.z1() + riverSegment.z2()) * 0.5)) {
                Constants.LOG.info("[SR-SEGDUMP] capture DROP: seg {} sys={} y2 {} -> {}{}", i, nArray[i], String.format(Locale.ROOT, "%.2f", riverSegment.y2()), String.format(Locale.ROOT, "%.2f", d3), nArray2[i] != Integer.MIN_VALUE && dArray[i] < (double)nArray2[i] ? " (held at lake floor " + nArray2[i] + ")" : "");
            }
            list.set(i, new RiverSegment(riverSegment.x1(), riverSegment.z1(), Math.max(riverSegment.y1(), d3), riverSegment.w1(), riverSegment.x2(), riverSegment.z2(), d3, riverSegment.w2()));
        }
        return n2 != 0;
    }

    /*
     * WARNING - void declaration
     */
    private static int a(List<RiverSegment> list, StreamsConfig streamsConfig) {
        void var8_11;
        HashMap<Long, Double> hashMap;
        int n2 = list.size();
        if (n2 == 0 || Boolean.getBoolean("streamsreflowing.nowidthwaist")) {
            return 0;
        }
        int[] nArray = NetworkTracer.chainIds(list);
        HashMap<Integer, Map> hashMap2 = new HashMap<Integer, Map>();
        HashMap<Integer, Set> hashMap3 = new HashMap<Integer, Set>();
        for (int i = 0; i < n2; ++i) {
            hashMap = list.get(i);
            hashMap2.computeIfAbsent(nArray[i], n -> new HashMap()).put(NetworkTracer.a(((RiverSegment)((Object)hashMap)).x1(), ((RiverSegment)((Object)hashMap)).z1()), i);
            hashMap3.computeIfAbsent(nArray[i], n -> new HashSet()).add(NetworkTracer.a(((RiverSegment)((Object)hashMap)).x2(), ((RiverSegment)((Object)hashMap)).z2()));
        }
        ArrayList<int[]> arrayList = new ArrayList<int[]>();
        block1: for (Map.Entry entry : hashMap2.entrySet()) {
            Set i = (Set)hashMap3.get(entry.getKey());
            for (Map.Entry entry2 : ((Map)entry.getValue()).entrySet()) {
                if (i.contains(entry2.getKey())) continue;
                arrayList.add(new int[]{(Integer)entry.getKey(), (Integer)entry2.getValue()});
                continue block1;
            }
        }
        hashMap = new HashMap<Long, Double>();
        boolean n3 = false;
        for (int i = 0; i < 64; ++i) {
            boolean bl = false;
            for (int[] nArray2 : arrayList) {
                Map map = (Map)hashMap2.get(nArray2[0]);
                RiverSegment riverSegment = list.get(nArray2[1]);
                double d = hashMap.getOrDefault(NetworkTracer.a(riverSegment.x1(), riverSegment.z1()), 0.0);
                int n4 = nArray2[1];
                long l = 0L;
                for (int j = 0; j < n2 && n4 >= 0; ++j) {
                    RiverSegment riverSegment2 = list.get(n4);
                    d = Math.max(d, riverSegment2.w1());
                    double d2 = Math.max(d * 0.5, NetworkTracer.a(d, streamsConfig));
                    if (riverSegment2.w1() < d2 || riverSegment2.w2() < d2) {
                        list.set(n4, new RiverSegment(riverSegment2.x1(), riverSegment2.z1(), riverSegment2.y1(), Math.max(riverSegment2.w1(), d2), riverSegment2.x2(), riverSegment2.z2(), riverSegment2.y2(), Math.max(riverSegment2.w2(), d2)));
                        ++var8_11;
                    }
                    d = Math.max(d, riverSegment2.w2());
                    l = NetworkTracer.a(riverSegment2.x2(), riverSegment2.z2());
                    Integer n5 = (Integer)map.get(l);
                    n4 = n5 == null ? -1 : n5;
                }
                Double j = (Double)hashMap.get(l);
                if (j != null && !(d > j)) continue;
                hashMap.put(l, d);
                bl = true;
            }
            if (!bl) break;
        }
        return (int)var8_11;
    }

    private static double a(double d, StreamsConfig streamsConfig) {
        int n = RiverEngine.insetFor(d, streamsConfig);
        double d2 = 0.0;
        double d3 = d;
        for (int i = 0; i < 24; ++i) {
            double d4 = (d2 + d3) * 0.5;
            if (RiverEngine.insetFor(d4, streamsConfig) >= n) {
                d3 = d4;
                continue;
            }
            d2 = d4;
        }
        return d3;
    }

    private static int b(List<RiverSegment> list, StreamsConfig streamsConfig) {
        int n2;
        int n3;
        Object object;
        int n4 = list.size();
        if (n4 < 2 || Boolean.getBoolean("streamsreflowing.nomergecrossings")) {
            return 0;
        }
        int[] nArray = NetworkTracer.chainIds(list);
        HashMap<Long, List> hashMap = new HashMap<Long, List>(n4 * 2);
        HashMap<Integer, Set> hashMap2 = new HashMap<Integer, Set>();
        for (int i = 0; i < n4; ++i) {
            RiverSegment riverSegment = list.get(i);
            for (Object object2 : object = (Object)new long[]{NetworkTracer.a(riverSegment.x1(), riverSegment.z1()), NetworkTracer.a(riverSegment.x2(), riverSegment.z2())}) {
                List list2 = hashMap.computeIfAbsent((long)object2, l -> new ArrayList(2));
                if (list2.contains(nArray[i])) continue;
                Iterator iterator = list2.iterator();
                while (iterator.hasNext()) {
                    n3 = (Integer)iterator.next();
                    hashMap2.computeIfAbsent(nArray[i], n -> new HashSet()).add(n3);
                    hashMap2.computeIfAbsent(n3, n -> new HashSet()).add(nArray[i]);
                }
                list2.add(nArray[i]);
            }
        }
        double d = Math.max(8.0, streamsConfig.maxWidth() * 2.0);
        object = new HashMap();
        for (int i = 0; i < n4; ++i) {
            RiverSegment riverSegment = list.get(i);
            int n5 = (int)Math.floor(riverSegment.minX() / d);
            int n6 = (int)Math.floor(riverSegment.maxX() / d);
            n2 = (int)Math.floor(riverSegment.minZ() / d);
            int n7 = (int)Math.floor(riverSegment.maxZ() / d);
            for (int j = n5; j <= n6; ++j) {
                for (n3 = n2; n3 <= n7; ++n3) {
                    object.computeIfAbsent((long)j << 32 ^ (long)n3 & 0xFFFFFFFFL, l -> new ArrayList()).add(i);
                }
            }
        }
        double d2 = streamsConfig.maxWidth() + 1.0 + 2.0;
        boolean[] blArray = new boolean[n4];
        int n8 = 0;
        block6: for (n2 = 0; n2 < n4; ++n2) {
            RiverSegment riverSegment = list.get(n2);
            if (riverSegment.widthAt(0.5) > 3.5) continue;
            double d3 = riverSegment.widthAt(0.5);
            double d4 = (riverSegment.x1() + riverSegment.x2()) * 0.5;
            double d5 = (riverSegment.z1() + riverSegment.z2()) * 0.5;
            Set set = (Set)hashMap2.get(nArray[n2]);
            int n9 = (int)Math.floor((d4 - d2) / d);
            int n10 = (int)Math.floor((d4 + d2) / d);
            int n11 = (int)Math.floor((d5 - d2) / d);
            int n12 = (int)Math.floor((d5 + d2) / d);
            for (int i = n9; i <= n10; ++i) {
                for (int j = n11; j <= n12; ++j) {
                    List list3 = (List)object.get((long)i << 32 ^ (long)j & 0xFFFFFFFFL);
                    if (list3 == null) continue;
                    Iterator iterator = list3.iterator();
                    while (iterator.hasNext()) {
                        double d6;
                        RiverSegment riverSegment2;
                        double d7;
                        int n13 = (Integer)iterator.next();
                        if (nArray[n13] == nArray[n2] || set != null && set.contains(nArray[n13]) || (d7 = (riverSegment2 = list.get(n13)).widthAt(d6 = riverSegment2.clampParam(d4, d5))) < d3 * 3.0 || !(riverSegment2.distanceAt(d4, d5, d6) <= d7 - 1.0)) continue;
                        blArray[n2] = true;
                        ++n8;
                        continue block6;
                    }
                }
            }
        }
        if (n8 == 0) {
            return 0;
        }
        ArrayList<RiverSegment> arrayList = new ArrayList<RiverSegment>(n4 - n8);
        for (int i = 0; i < n4; ++i) {
            if (blArray[i]) continue;
            arrayList.add(list.get(i));
        }
        list.clear();
        list.addAll(arrayList);
        return n8;
    }

    private static double a(TerrainSampler terrainSampler, int n, int n2, int n3) {
        double d = terrainSampler.riverSurface(n, n2);
        if (!Double.isNaN(d)) {
            return d;
        }
        int n4 = Math.max(2, (int)Math.round((double)n3 * 0.375));
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (j == 0 && i == 0 || Double.isNaN(d = terrainSampler.riverSurface(n + j * n4, n2 + i * n4))) continue;
                return d;
            }
        }
        return Double.NaN;
    }

    private static void a(List<RiverSegment> list, List<Integer> list2, List<Lake> list3, boolean[] blArray, int n, StreamsConfig streamsConfig, TerrainSampler terrainSampler) {
        if (!streamsConfig.growFromWater() || list.isEmpty()) {
            return;
        }
        for (int i = 0; i < 64; ++i) {
            Object object;
            int n2 = list.size();
            int[] nArray = NetworkTracer.chainIds(list);
            HashMap<Long, Integer> hashMap = new HashMap<Long, Integer>(n2 * 2);
            for (int j = 0; j < n2; ++j) {
                object = list.get(j);
                hashMap.putIfAbsent(NetworkTracer.a(((RiverSegment)object).x1(), ((RiverSegment)object).z1()), nArray[j]);
            }
            HashMap<Integer, Integer> hashMap2 = new HashMap<Integer, Integer>();
            for (int j = 0; j < n2; ++j) {
                hashMap2.put(nArray[j], j);
            }
            object = new HashSet();
            for (Map.Entry entry : hashMap2.entrySet()) {
                int n3;
                int n4;
                Integer n5;
                RiverSegment riverSegment = list.get((Integer)entry.getValue());
                if (riverSegment.y2() <= (double)(n + 1)) continue;
                boolean bl = false;
                for (int j = 0; j < list3.size(); ++j) {
                    if (blArray[j] || !list3.get(j).covers((int)Math.floor(riverSegment.x2()), (int)Math.floor(riverSegment.z2()))) continue;
                    bl = true;
                    break;
                }
                if (bl || (n5 = (Integer)hashMap.get(NetworkTracer.a(riverSegment.x2(), riverSegment.z2()))) != null && !n5.equals(entry.getKey()) || !Double.isNaN(NetworkTracer.a(terrainSampler, n4 = (int)Math.floor(riverSegment.x2()), n3 = (int)Math.floor(riverSegment.z2()), streamsConfig.gridSpacing())) || terrainSampler.waterBiomeClassAt(n4, (int)Math.round(riverSegment.y2()), n3) == 1) continue;
                object.add((Integer)entry.getKey());
            }
            if (object.isEmpty()) {
                return;
            }
            int n6 = 0;
            for (int j = 0; j < n2; ++j) {
                if (object.contains(nArray[j])) continue;
                list.set(n6, list.get(j));
                list2.set(n6, list2.get(j));
                ++n6;
            }
            while (list.size() > n6) {
                list.remove(list.size() - 1);
                list2.remove(list2.size() - 1);
            }
            Constants.LOG.debug("[SR-MERGE] orphan prune pass {}: removed {} chains", (Object)i, (Object)object.size());
        }
    }

    private static void a(List<RiverSegment> list, List<Lake> list2, boolean[] blArray, double[] dArray, int[] nArray) {
        int n;
        Object object;
        int n2;
        int n3 = list.size();
        Arrays.fill(dArray, Double.NaN);
        Arrays.fill(nArray, -1);
        if (n3 == 0) {
            return;
        }
        int[] nArray2 = NetworkTracer.chainIds(list);
        double[] dArray2 = new double[n3];
        int[] nArray3 = new int[n3];
        for (n2 = 0; n2 < n3; ++n2) {
            object = list.get(n2);
            dArray2[n2] = Double.NaN;
            nArray3[n2] = -1;
            n = (int)Math.floor((((RiverSegment)object).x1() + ((RiverSegment)object).x2()) * 0.5);
            int n4 = (int)Math.floor((((RiverSegment)object).z1() + ((RiverSegment)object).z2()) * 0.5);
            for (int i = 0; i < list2.size(); ++i) {
                Lake lake;
                if (blArray[i] || !(lake = list2.get(i)).covers(n, n4) || !Double.isNaN(dArray2[n2]) && !((double)lake.level() > dArray2[n2])) continue;
                dArray2[n2] = lake.level();
                nArray3[n2] = i;
            }
        }
        n2 = 1;
        while (n2 != 0) {
            n2 = 0;
            object = new HashMap();
            for (n = 0; n < n3; ++n) {
                RiverSegment riverSegment;
                long l;
                double[] dArray3;
                if (Double.isNaN(dArray[n]) || (dArray3 = (double[])object.get(l = NetworkTracer.a((riverSegment = list.get(n)).x1(), riverSegment.z1()))) != null && !(dArray[n] > dArray3[0])) continue;
                object.put(l, new double[]{dArray[n], nArray[n]});
            }
            n = Integer.MIN_VALUE;
            double d = Double.NaN;
            int n5 = -1;
            for (int i = n3 - 1; i >= 0; --i) {
                if (nArray2[i] != n) {
                    n = nArray2[i];
                    RiverSegment riverSegment = list.get(i);
                    double[] dArray4 = (double[])object.get(NetworkTracer.a(riverSegment.x2(), riverSegment.z2()));
                    d = dArray4 != null ? dArray4[0] : Double.NaN;
                    int n6 = n5 = dArray4 != null ? (int)dArray4[1] : -1;
                }
                if (!Double.isNaN(dArray2[i]) && (Double.isNaN(d) || dArray2[i] > d)) {
                    d = dArray2[i];
                    n5 = nArray3[i];
                }
                if (Double.isNaN(d) || !Double.isNaN(dArray[i]) && !(d > dArray[i] + 1.0E-9)) continue;
                dArray[i] = d;
                nArray[i] = n5;
                n2 = 1;
            }
        }
    }

    public static int[] chainIds(List<RiverSegment> list) {
        int n = list.size();
        HashMap<Long, Integer> hashMap = new HashMap<Long, Integer>(n * 2);
        HashMap<Long, Integer> hashMap2 = new HashMap<Long, Integer>(n * 2);
        HashMap<Long, Integer> hashMap3 = new HashMap<Long, Integer>(n * 2);
        HashMap<Long, Integer> hashMap4 = new HashMap<Long, Integer>(n * 2);
        for (int i = 0; i < n; ++i) {
            RiverSegment riverSegment = list.get(i);
            long l = NetworkTracer.a(riverSegment.x1(), riverSegment.z1());
            long l2 = NetworkTracer.a(riverSegment.x2(), riverSegment.z2());
            hashMap.merge(l, 1, Integer::sum);
            hashMap3.put(l, i);
            hashMap2.merge(l2, 1, Integer::sum);
            hashMap4.put(l2, i);
        }
        int[] nArray = new int[n];
        for (int i = 0; i < n; ++i) {
            nArray[i] = i;
        }
        for (Map.Entry entry : hashMap3.entrySet()) {
            long l = (Long)entry.getKey();
            if (hashMap.getOrDefault(l, 0) != 1 || hashMap2.getOrDefault(l, 0) != 1) continue;
            NetworkTracer.a(nArray, (int)((Integer)hashMap4.get(l)), (int)((Integer)entry.getValue()));
        }
        Object object = new int[n];
        for (int i = 0; i < n; ++i) {
            object[i] = NetworkTracer.a(nArray, i);
        }
        return object;
    }

    private static long a(double d, double d2) {
        long l = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l2 = Math.round(d2 * 16.0) & 0xFFFFFFFFL;
        return l << 32 | l2;
    }

    private static int a(int[] nArray, int n) {
        while (nArray[n] != n) {
            nArray[n] = nArray[nArray[n]];
            n = nArray[n];
        }
        return n;
    }

    private static void a(int[] nArray, int n, int n2) {
        int n3;
        int n4 = NetworkTracer.a(nArray, n);
        if (n4 != (n3 = NetworkTracer.a(nArray, n2))) {
            nArray[n4] = n3;
        }
    }

    private static List<RiverSegment> a(int n, int[] nArray, int[] nArray2, List<Lake> list, Lake lake, boolean[] blArray, int[] nArray3, double[] dArray, double[] dArray2, LowResGrid lowResGrid, TerrainSampler terrainSampler, WetnessSampler wetnessSampler, StreamsConfig streamsConfig, long l, int n2) {
        Lake lake2;
        long l2;
        int n3;
        int n4;
        double d;
        int n5;
        Lake lake3;
        double d2;
        int n6;
        int n7;
        boolean bl;
        int n8;
        ArrayList<Pt> arrayList;
        ColumnCache columnCache;
        ArrayList<RiverSegment> arrayList2;
        block32: {
            arrayList2 = new ArrayList<RiverSegment>();
            columnCache = new ColumnCache();
            arrayList = new ArrayList<Pt>();
            arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n)), lowResGrid.worldZ(lowResGrid.gj(n)), dArray[n], dArray2[n]));
            n8 = n;
            bl = false;
            n7 = -1;
            n6 = -1;
            d2 = Double.NaN;
            lake3 = null;
            while (true) {
                if ((n5 = nArray[n8]) < 0) {
                    bl = true;
                } else if (nArray2[n5] >= 0) {
                    lake3 = list.get(nArray2[n5]);
                    arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n5)), lowResGrid.worldZ(lowResGrid.gj(n5)), lake3.level(), dArray2[n8]));
                    d2 = lake3.level();
                } else if (lowResGrid.riverOutlet(n5)) {
                    d = dArray[n5];
                    arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n5)), lowResGrid.worldZ(lowResGrid.gj(n5)), Math.min(dArray[n8], d), dArray2[n8]));
                    d2 = d;
                    n6 = n5;
                } else {
                    if (!blArray[n5]) break;
                    arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n5)), lowResGrid.worldZ(lowResGrid.gj(n5)), dArray[n5], dArray2[n5]));
                    if (nArray3[n5] == 1) {
                        n8 = n5;
                        continue;
                    }
                }
                break block32;
                break;
            }
            arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n5)), lowResGrid.worldZ(lowResGrid.gj(n5)), n2, dArray2[n8]));
            d2 = n2;
            n7 = n5;
        }
        n5 = 0;
        d = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;
        if (nArray3[n] == 0) {
            n4 = lowResGrid.gi(n);
            int n9 = lowResGrid.gj(n);
            int n10 = -1;
            int n11 = Integer.MAX_VALUE;
            for (int i = -2; i <= 2; ++i) {
                for (n3 = -2; n3 <= 2; ++n3) {
                    int n12;
                    int n13 = n4 + n3;
                    int n14 = n9 + i;
                    if (!lowResGrid.valid(n13, n14) || nArray2[lowResGrid.idx(n13, n14)] < 0 || (n12 = n3 * n3 + i * i) >= n11) continue;
                    n11 = n12;
                    n10 = lowResGrid.idx(n13, n14);
                }
            }
            if (n10 >= 0 && Math.abs((double)list.get(nArray2[n10]).level() - (dArray[n] - streamsConfig.ditchInset())) <= 4.0) {
                n5 = 1;
                d = lowResGrid.worldX(lowResGrid.gi(n10));
                d3 = lowResGrid.worldZ(lowResGrid.gj(n10));
                d4 = list.get(nArray2[n10]).level();
                if (lake3 != null && (double)lake3.level() >= d4 - 1.0) {
                    return arrayList2;
                }
            }
        }
        if (n7 >= 0 && nArray3[n] == 0 && n5 == 0 && arrayList.size() - 1 < streamsConfig.minSeaChainNodes() && wetnessSampler.wetness((int)((Pt)arrayList.get(0)).x(), (int)dArray[n], (int)((Pt)arrayList.get(0)).z()) > streamsConfig.wetnessDryCutoff()) {
            return arrayList2;
        }
        n4 = nArray3[n] == 0 && n5 == 0 ? 1 : 0;
        long l3 = System.nanoTime();
        List<Pt> list2 = streamsConfig.valleySnapRadius() > 0 ? NetworkTracer.a(arrayList, terrainSampler, columnCache, streamsConfig, n4 != 0, list) : (n4 != 0 ? NetworkTracer.a(arrayList, terrainSampler, columnCache) : arrayList);
        P.addAndGet(0, System.nanoTime() - l3);
        if (streamsConfig.valleySnapRadius() > 0 && list2.size() >= 2) {
            double d5 = ((Pt)arrayList.get(0)).y();
            double d6 = NetworkTracer.a(terrainSampler, columnCache, ((Pt)arrayList.get(0)).x(), ((Pt)arrayList.get(0)).z());
            double d7 = list2.get(0).y();
            l2 = System.nanoTime();
            list2 = NetworkTracer.a(NetworkTracer.a(list2, (double)streamsConfig.gridSpacing() * 0.4), terrainSampler, columnCache, streamsConfig, list);
            P.addAndGet(1, System.nanoTime() - l2);
            if (n4 != 0 && list2.get(0).y() < d6 - 8.0) {
                Constants.LOG.debug("[SR-SRCTRACE] dug source ({},{}) fineGround={}: coarse={} -> snapToValley={} -> followGround={}", (int)Math.round(((Pt)arrayList.get(0)).x()), (int)Math.round(((Pt)arrayList.get(0)).z()), (int)d6, String.format("%.1f", d5), String.format("%.1f", d7), String.format("%.1f", list2.get(0).y()));
            }
        }
        if (n4 != 0 && !list2.isEmpty() && ((Pt)list2.get(0)).y() < (double)(n2 + 2)) {
            return arrayList2;
        }
        if (n5 != 0 && !list2.isEmpty()) {
            ArrayList<Pt> arrayList3 = new ArrayList<Pt>(list2.size() + 1);
            arrayList3.add(new Pt(d, d3, d4 + (double)RiverEngine.insetFor(NetworkTracer.b(((Pt)list2.get(0)).acc(), streamsConfig), streamsConfig), list2.get(0).acc()));
            arrayList3.addAll(list2);
            list2 = arrayList3;
        }
        Lake lake4 = lake2 = lake3 != null ? lake3 : lake;
        if (lake2 != null && list2.size() >= 2) {
            n3 = list2.size();
            if ((list2 = NetworkTracer.a(list2, lake2, terrainSampler, columnCache, streamsConfig)).size() < n3 || list2.size() < 2) {
                return arrayList2;
            }
        }
        if (list2.size() >= 2) {
            if (n7 >= 0) {
                List<Pt> list3 = NetworkTracer.a(list2, lowResGrid, n7, n2, terrainSampler, streamsConfig);
                if (list3 == list2) {
                    return arrayList2;
                }
                list2 = list3;
            } else if (n6 >= 0) {
                List<Pt> list4 = NetworkTracer.a(list2, lowResGrid, n6, terrainSampler, streamsConfig);
                if (list4 == list2) {
                    return arrayList2;
                }
                list2 = list4;
            } else if (!Double.isNaN(d2)) {
                list2 = NetworkTracer.b(list2, d2, 8.0);
            } else if (bl) {
                List<Pt> list5 = NetworkTracer.a(list2, lowResGrid, n8, n2, terrainSampler, streamsConfig);
                if (list5 == list2) {
                    return arrayList2;
                }
                list2 = list5;
            }
            long l4 = System.nanoTime();
            List<Pt> list6 = NetworkTracer.a(NetworkTracer.a(list2, (double)streamsConfig.gridSpacing() * 0.4), l, n, streamsConfig);
            long l5 = System.nanoTime();
            if (streamsConfig.confineToBanks()) {
                list6 = NetworkTracer.a(list6, terrainSampler, columnCache, streamsConfig);
            }
            l2 = System.nanoTime();
            list6 = NetworkTracer.a(list6, streamsConfig.smoothingPasses());
            long l6 = System.nanoTime();
            NetworkTracer.a(list6, streamsConfig, arrayList2, n4 != 0);
            P.addAndGet(2, l5 - l4);
            P.addAndGet(3, l2 - l5);
            P.addAndGet(4, l6 - l2);
            P.addAndGet(5, System.nanoTime() - l6);
        }
        return arrayList2;
    }

    private static List<Pt> a(List<Pt> list, Lake lake, TerrainSampler terrainSampler, ColumnCache columnCache, StreamsConfig streamsConfig) {
        int n = lake.level();
        ArrayList<Pt> arrayList = new ArrayList<Pt>(list.size());
        boolean bl = false;
        for (Pt pt : list) {
            if (!lake.covers((int)Math.floor(pt.x()), (int)Math.floor(pt.z()))) {
                double d = NetworkTracer.a(terrainSampler, columnCache, pt.x(), pt.z());
                if (d < (double)n - 4.0) {
                    if (K == null || !NetworkTracer.b(pt.x(), pt.z())) break;
                    Constants.LOG.info("[SR-SEGDUMP] modifier lakeApproach TRUNCATE at ({},{}) ground={} < lake {} - {}", (int)pt.x(), (int)pt.z(), (int)d, n, 4);
                    break;
                }
                double d2 = n + RiverEngine.insetFor(NetworkTracer.b(pt.acc(), streamsConfig), streamsConfig);
                if (pt.y() < d2) {
                    if (!bl && K != null && NetworkTracer.b(pt.x(), pt.z())) {
                        Constants.LOG.info("[SR-SEGDUMP] modifier lakeApproach HOLD from ({},{}) y {} -> {} (lake {} + inset)", (int)pt.x(), (int)pt.z(), String.format(Locale.ROOT, "%.1f", pt.y()), String.format(Locale.ROOT, "%.1f", d2), n);
                        bl = true;
                    }
                    pt = new Pt(pt.x(), pt.z(), d2, pt.acc());
                }
            }
            arrayList.add(pt);
        }
        return arrayList;
    }

    public static RiverNetwork assemble(PlateKey plateKey, List<RiverSegment> list, List<Lake> list2, StreamsConfig streamsConfig) {
        NearestRiverIndex nearestRiverIndex = new NearestRiverIndex(list, streamsConfig.maxWidth() * (1.0 + streamsConfig.bankWidth()) + 2.0, Math.max(16, streamsConfig.gridSpacing()), streamsConfig.bankWidth());
        return new RiverNetwork(plateKey, list, nearestRiverIndex, list2);
    }

    private static int[] a() {
        String string = System.getProperty("streamsreflowing.laketrace");
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            String[] stringArray = string.split(",");
            return new int[]{Integer.parseInt(stringArray[0].trim()), Integer.parseInt(stringArray[1].trim())};
        }
        catch (RuntimeException runtimeException) {
            System.out.println("[SR-LAKETRACE] bad streamsreflowing.laketrace value: " + string);
            return null;
        }
    }

    private static int[] b() {
        String string = System.getProperty("streamsreflowing.segdump");
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            String[] stringArray = string.split(",");
            int n = stringArray.length >= 3 ? Integer.parseInt(stringArray[2].trim()) : 64;
            return new int[]{Integer.parseInt(stringArray[0].trim()), Integer.parseInt(stringArray[1].trim()), n};
        }
        catch (RuntimeException runtimeException) {
            System.out.println("[SR-SEGDUMP] bad streamsreflowing.segdump value: " + string);
            return null;
        }
    }

    private static boolean b(double d, double d2) {
        return K != null && Math.abs(d - (double)K[0]) <= (double)K[2] && Math.abs(d2 - (double)K[1]) <= (double)K[2];
    }

    public static void dumpLoadedSegments(PlateKey plateKey, List<RiverSegment> list, List<Lake> list2) {
        if (K == null) {
            return;
        }
        int[] nArray = new int[list.size()];
        Arrays.fill(nArray, -1);
        NetworkTracer.a("plate " + String.valueOf(plateKey) + " (LOADED from store)", list, nArray, list2);
    }

    private static void a(PlateDomain plateDomain, List<RiverSegment> list, int[] nArray, List<Lake> list2) {
        NetworkTracer.a("plate " + String.valueOf(plateDomain.key()), list, nArray, list2);
    }

    private static void a(String string, List<RiverSegment> list, int[] nArray, List<Lake> list2) {
        int[] nArray2 = NetworkTracer.chainIds(list);
        StringBuilder stringBuilder = new StringBuilder();
        int n = 0;
        for (int i = 0; i < list.size(); ++i) {
            double d;
            RiverSegment object = list.get(i);
            double d2 = (object.x1() + object.x2()) * 0.5;
            if (!NetworkTracer.b(d2, d = (object.z1() + object.z2()) * 0.5)) continue;
            stringBuilder.append(String.format(Locale.ROOT, "%n  seg %5d sys=%-10d chain=%-5d (%8.1f,%8.1f y=%7.2f w=%4.1f) -> (%8.1f,%8.1f y=%7.2f w=%4.1f)", i, nArray[i], nArray2[i], object.x1(), object.z1(), object.y1(), object.w1(), object.x2(), object.z2(), object.y2(), object.w2()));
            ++n;
        }
        for (Lake lake : list2) {
            int n2;
            double d;
            int n3 = lake.cellMinX();
            int n4 = lake.cellMinZ();
            int n5 = n3 + lake.cellsWide() * lake.spacing();
            double d3 = (double)(n3 + n5) * 0.5;
            boolean bl = NetworkTracer.b(d3, d = (double)(n4 + (n2 = n4 + lake.cellsHigh() * lake.spacing())) * 0.5) || K != null && K[0] >= n3 - K[2] && K[0] <= n5 + K[2] && K[1] >= n4 - K[2] && K[1] <= n2 + K[2];
            if (!bl) continue;
            stringBuilder.append(String.format(Locale.ROOT, "%n  lake level=%d dig=%d bbox=[%d..%d, %d..%d] covers-target=%s", lake.level(), lake.digDepth(), n3, n5, n4, n2, lake.covers(K[0], K[1])));
        }
        if (n > 0 || stringBuilder.length() > 0) {
            Constants.LOG.info("[SR-SEGDUMP] {} segments/lakes within {} of ({},{}):{}", string, K[2], K[0], K[1], stringBuilder);
        }
    }

    private static void a(int n3, int n4, List<Integer> list, LowResGrid lowResGrid, double[] dArray, int[] nArray, double[] dArray2, boolean[] blArray, int[] nArray2, int n5, PlateDomain plateDomain) {
        int n6;
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        Object object = list.iterator();
        while (object.hasNext()) {
            n6 = object.next();
            if (Math.abs(lowResGrid.worldX(lowResGrid.gi(n6)) - n3) > 96 || Math.abs(lowResGrid.worldZ(lowResGrid.gj(n6)) - n4) > 96) continue;
            arrayList.add(n6);
        }
        if (arrayList.isEmpty()) {
            return;
        }
        arrayList.sort((n, n2) -> {
            int n3;
            int n4 = lowResGrid.worldZ(lowResGrid.gj((int)n));
            return n4 != (n3 = lowResGrid.worldZ(lowResGrid.gj((int)n2))) ? Integer.compare(n4, n3) : Integer.compare(lowResGrid.worldX(lowResGrid.gi((int)n)), lowResGrid.worldX(lowResGrid.gi((int)n2)));
        });
        object = new StringBuilder();
        ((StringBuilder)object).append(String.format("%n[SR-LAKETRACE] ===== plate=%s target=(%d,%d) sea=%d window=%d cells=%d =====%n", plateDomain.key(), n3, n4, n5, 96, arrayList.size()));
        ((StringBuilder)object).append(String.format("[SR-LAKETRACE]   worldX  worldZ  terrainH  filled  f-h   acc    lakeId  flags%n", new Object[0]));
        n6 = 0;
        int n7 = 0;
        Iterator iterator = arrayList.iterator();
        while (iterator.hasNext()) {
            int n8;
            int n9 = (Integer)iterator.next();
            int n10 = lowResGrid.worldX(lowResGrid.gi(n9));
            int n11 = lowResGrid.worldZ(lowResGrid.gj(n9));
            double d = lowResGrid.height(n9);
            double d2 = dArray[n9];
            boolean bl = d <= (double)n5;
            boolean bl2 = d2 - d > 0.001;
            boolean bl3 = NetworkTracer.a(n9, dArray, lowResGrid, n5);
            boolean bl4 = d2 <= (double)n5;
            StringBuilder stringBuilder = new StringBuilder();
            if (bl) {
                stringBuilder.append("OUTLET ");
            }
            if (bl2) {
                stringBuilder.append("RAISED ");
            }
            if (bl3) {
                stringBuilder.append("BASIN ");
            }
            if (bl4) {
                stringBuilder.append("SUBSEA ");
            }
            if (blArray[n9]) {
                stringBuilder.append("RIVER ");
            }
            if (nArray2[n9] >= 0) {
                stringBuilder.append("LAKE ");
            }
            if (nArray2[n9] >= 0) {
                ++n6;
            }
            if (bl2 && bl4 && !bl3) {
                ++n7;
            }
            String string = (n8 = nArray[n9]) >= 0 ? "->(" + lowResGrid.worldX(lowResGrid.gi(n8)) + "," + lowResGrid.worldZ(lowResGrid.gj(n8)) + ")" : "sink";
            ((StringBuilder)object).append(String.format("[SR-LAKETRACE]  %7d %7d %9.1f %7.1f %5.1f %6.1f %6d   %s%s%n", n10, n11, d, d2, d2 - d, dArray2[n9], nArray2[n9], stringBuilder.toString().trim(), " " + string));
        }
        ((StringBuilder)object).append(String.format("[SR-LAKETRACE] summary: %d cells in window, %d are lake cells, %d are RAISED+SUBSEA but NOT basin-eligible (the perch source: a sub-sea fill that isBasinCell rejects because filled<=sea, so it never becomes a contained lake)%n", arrayList.size(), n6, n7));
        ((StringBuilder)object).append(String.format("[SR-LAKETRACE] legend: OUTLET=seeded as ocean (terrainH<=sea); RAISED=Priority-Flood lifted it above terrain; BASIN=isBasinCell true (lake-eligible); SUBSEA=filled<=sea; RIVER=carries a stream; LAKE=assigned to a lake%n", new Object[0]));
        System.out.print(object);
        System.out.flush();
    }

    private static boolean a(int n, double[] dArray, LowResGrid lowResGrid, int n2) {
        return NetworkTracer.a(n, dArray, lowResGrid, n2, 0.5);
    }

    private static boolean a(int n, double[] dArray, LowResGrid lowResGrid, int n2, double d) {
        return lowResGrid.inDomain(n) && !lowResGrid.riverOutlet(n) && dArray[n] > (double)n2 && dArray[n] - lowResGrid.height(n) > d;
    }

    private static boolean a(int n, int[] nArray, double[] dArray, int n2, double d, double d2, int n3) {
        int n4 = n;
        for (int i = 0; n4 >= 0 && i < n3; ++i) {
            if (dArray[n4] <= (double)n2) {
                return true;
            }
            if (d - dArray[n4] >= d2) {
                return true;
            }
            n4 = nArray[n4];
        }
        return false;
    }

    private static LocalBasin a(LowResGrid lowResGrid, TerrainSampler terrainSampler, List<Integer> list, int n) {
        int n2;
        int n3;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n132;
        int n14 = Integer.MAX_VALUE;
        int n15 = Integer.MIN_VALUE;
        int n16 = Integer.MAX_VALUE;
        int n17 = Integer.MIN_VALUE;
        for (int n132 : list) {
            n14 = Math.min(n14, lowResGrid.gi(n132));
            n15 = Math.max(n15, lowResGrid.gi(n132));
            n16 = Math.min(n16, lowResGrid.gj(n132));
            n17 = Math.max(n17, lowResGrid.gj(n132));
        }
        n16 -= 8;
        n17 += 8;
        if ((n15 += 8) - (n14 -= 8) + 1 > 32) {
            int n18 = n14 + n15 >> 1;
            n14 = n18 - 16;
            n15 = n18 + 15;
        }
        if (n17 - n16 + 1 > 32) {
            int n19 = n16 + n17 >> 1;
            n16 = n19 - 16;
            n17 = n19 + 15;
        }
        int n20 = n15 - n14 + 1;
        n132 = n17 - n16 + 1;
        double[] dArray3 = new double[n20 * n132];
        boolean[] blArray = new boolean[n20 * n132];
        for (n12 = 0; n12 < n132; ++n12) {
            for (n11 = 0; n11 < n20; ++n11) {
                blArray[n12 * n20 + n11] = lowResGrid.valid(n14 + n11, n16 + n12);
            }
        }
        n12 = n14;
        n11 = n16;
        int n21 = n20;
        LowResGrid.parallelFor(n20 * n132, n4 -> {
            if (blArray[n4]) {
                dArray[n4] = terrainSampler.groundHeightExact(lowResGrid.worldX(n12 + n4 % n21), lowResGrid.worldZ(n11 + n4 / n21));
            }
        });
        double[] dArray4 = new double[n20 * n132];
        Arrays.fill(dArray4, Double.MAX_VALUE);
        PriorityQueue<double[]> priorityQueue = new PriorityQueue<double[]>((dArray, dArray2) -> Double.compare(dArray[0], dArray2[0]));
        for (n10 = 0; n10 < n132; ++n10) {
            for (n9 = 0; n9 < n20; ++n9) {
                n8 = n10 * n20 + n9;
                if (!blArray[n8]) continue;
                int n22 = n7 = n9 == 0 || n10 == 0 || n9 == n20 - 1 || n10 == n132 - 1 ? 1 : 0;
                if (n7 == 0) {
                    for (n6 = 0; n6 < 8 && n7 == 0; ++n6) {
                        n5 = n10 + b[n6];
                        n3 = n9 + a[n6];
                        n7 = !blArray[n5 * n20 + n3] ? 1 : 0;
                    }
                }
                if (n7 == 0) continue;
                dArray4[n8] = dArray3[n8];
                priorityQueue.add(new double[]{dArray3[n8], n8});
            }
        }
        while (!priorityQueue.isEmpty()) {
            double[] dArray5 = (double[])priorityQueue.poll();
            if (dArray5[0] > dArray4[n9 = (int)dArray5[1]] + 1.0E-9) continue;
            n8 = n9 % n20;
            n7 = n9 / n20;
            for (n6 = 0; n6 < 8; ++n6) {
                double d;
                n3 = n8 + a[n6];
                n5 = n7 + b[n6];
                if (n3 < 0 || n3 >= n20 || n5 < 0 || n5 >= n132 || !blArray[n5 * n20 + n3] || !((d = Math.max(dArray3[n2 = n5 * n20 + n3], dArray4[n9])) < dArray4[n2])) continue;
                dArray4[n2] = d;
                priorityQueue.add(new double[]{d, n2});
            }
        }
        n10 = lowResGrid.gi(n) - n14;
        n9 = lowResGrid.gj(n) - n16;
        if (n10 < 0 || n10 >= n20 || n9 < 0 || n9 >= n132 || !blArray[n9 * n20 + n10]) {
            return null;
        }
        n8 = n9 * n20 + n10;
        if (dArray4[n8] - dArray3[n8] <= 0.001) {
            return null;
        }
        double d = dArray4[n8];
        double d2 = dArray3[n8];
        n2 = n8;
        boolean[] blArray2 = new boolean[n20 * n132];
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        blArray2[n8] = true;
        arrayDeque.add(n8);
        while (!arrayDeque.isEmpty()) {
            int n23 = (Integer)arrayDeque.poll();
            if (dArray3[n23] < d2) {
                d2 = dArray3[n23];
                n2 = n23;
            }
            int n24 = n23 % n20;
            int n25 = n23 / n20;
            for (int i = 0; i < 8; ++i) {
                int n26;
                int n27 = n24 + a[i];
                int n28 = n25 + b[i];
                if (n27 < 0 || n27 >= n20 || n28 < 0 || n28 >= n132 || !blArray[n26 = n28 * n20 + n27] || blArray2[n26] || dArray3[n26] >= d - 1.0E-6) continue;
                blArray2[n26] = true;
                arrayDeque.add(n26);
            }
        }
        return new LocalBasin(n14, n16, n20, n132, dArray3, blArray, n2, d, d2);
    }

    private static double a(List<Integer> list, LowResGrid lowResGrid, TerrainSampler terrainSampler) {
        double d = 0.0;
        double d2 = 0.0;
        int n = 0;
        for (int n2 : list) {
            int n3 = lowResGrid.gi(n2);
            int n4 = lowResGrid.gj(n2);
            for (int i = 0; i < 8; ++i) {
                int n5 = n3 + a[i];
                int n6 = n4 + b[i];
                if (lowResGrid.valid(n5, n6) && lowResGrid.inDomain(lowResGrid.idx(n5, n6))) continue;
                d += (double)(lowResGrid.worldX(n3) + lowResGrid.worldX(n5)) * 0.5;
                d2 += (double)(lowResGrid.worldZ(n4) + lowResGrid.worldZ(n6)) * 0.5;
                ++n;
            }
        }
        if (n == 0) {
            return Double.NaN;
        }
        double d3 = d / (double)n;
        double d4 = d2 / (double)n;
        double d5 = lowResGrid.spacing;
        double d6 = Double.MAX_VALUE;
        for (double d7 = -d5; d7 <= d5 + 1.0E-9; d7 += d5 / 2.0) {
            for (double d8 = -d5; d8 <= d5 + 1.0E-9; d8 += d5 / 2.0) {
                d6 = Math.min(d6, NetworkTracer.a(terrainSampler, d3 + d8, d4 + d7));
            }
        }
        return d6;
    }

    private static boolean a(LowResGrid lowResGrid, boolean[] blArray, int n, int n2, int n3) {
        if (n == n2) {
            return true;
        }
        int n4 = lowResGrid.gi(n);
        int n5 = lowResGrid.gj(n);
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                if (j == 0 && i == 0 || lowResGrid.valid(n4 + j, n5 + i) && blArray[lowResGrid.idx(n4 + j, n5 + i)]) continue;
                return false;
            }
        }
        return true;
    }

    private static Lake a(List<Integer> list, int n4, TerrainSampler terrainSampler, LowResGrid lowResGrid, PlateDomain plateDomain, double d, double[] dArray) {
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n142;
        int n15 = 3;
        int n16 = 3 * lowResGrid.spacing;
        int n17 = lowResGrid.spacing >> 1;
        int n18 = Integer.MAX_VALUE;
        int n19 = Integer.MAX_VALUE;
        int n20 = Integer.MIN_VALUE;
        int n21 = Integer.MIN_VALUE;
        boolean[] blArray = new boolean[lowResGrid.size()];
        for (int n142 : list) {
            blArray[n142] = true;
        }
        for (int n142 : list) {
            n13 = lowResGrid.worldX(lowResGrid.gi(n142));
            n12 = lowResGrid.worldZ(lowResGrid.gj(n142));
            n18 = Math.min(n18, n13);
            n20 = Math.max(n20, n13);
            n19 = Math.min(n19, n12);
            n21 = Math.max(n21, n12);
        }
        int n22 = n18 - n17 - n16;
        n142 = n19 - n17 - n16;
        n13 = (n20 + n17 + n16 - n22) / 3 + 1;
        n12 = (n21 + n17 + n16 - n142) / 3 + 1;
        long[] lArray = new long[n13 * n12 + 63 >>> 6];
        boolean[] blArray2 = new boolean[n13 * n12];
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        ArrayList<Integer> arrayList = new ArrayList<Integer>(list);
        arrayList.sort((n, n2) -> Double.compare(lowResGrid.height((int)n), lowResGrid.height((int)n2)));
        int n23 = -1;
        for (int i = 0; i < Math.min(4, arrayList.size()); ++i) {
            int n24 = (Integer)arrayList.get(i);
            if (!(terrainSampler.groundHeight(lowResGrid.worldX(lowResGrid.gi(n24)), lowResGrid.worldZ(lowResGrid.gj(n24))) < (double)n4)) continue;
            n23 = n24;
            break;
        }
        if (n23 < 0) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-FINEFLOOD] floor-dry level={} fill={}", (Object)n4, (Object)list.size());
            }
            return null;
        }
        ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
        for (int n25 : list) {
            if (!NetworkTracer.a(lowResGrid, blArray, n25, n23, n4)) continue;
            arrayList2.add(n25);
        }
        long l = System.nanoTime();
        ArrayList<Integer> arrayList3 = arrayList2;
        LowResGrid.parallelFor(arrayList3.size(), n -> {
            int n2 = (Integer)arrayList3.get(n);
            terrainSampler.groundHeight(lowResGrid.worldX(lowResGrid.gi(n2)), lowResGrid.worldZ(lowResGrid.gj(n2)));
        });
        long[] lArray2 = X.get();
        lArray2[7] = lArray2[7] + (System.nanoTime() - l);
        int n26 = 0;
        Object object = arrayList2.iterator();
        while (object.hasNext()) {
            int n27;
            int n28 = (Integer)object.next();
            int n29 = lowResGrid.worldX(lowResGrid.gi(n28));
            double d2 = terrainSampler.groundHeight(n29, n27 = lowResGrid.worldZ(lowResGrid.gj(n28)));
            if (d2 >= (double)n4) {
                ++n26;
                continue;
            }
            if (dArray != null) {
                dArray[0] = dArray[0] + ((double)n4 - d2);
                dArray[1] = dArray[1] + 1.0;
            }
            n11 = (n29 - n17 - n22) / 3;
            n10 = (n29 + n17 - n22) / 3;
            n9 = (n27 - n17 - n142) / 3;
            n8 = (n27 + n17 - n142) / 3;
            for (n7 = n9; n7 <= n8; ++n7) {
                for (n6 = n11; n6 <= n10; ++n6) {
                    if (n6 < 0 || n6 >= n13 || n7 < 0 || n7 >= n12 || blArray2[n5 = n7 * n13 + n6]) continue;
                    blArray2[n5] = true;
                    int n30 = n5 >>> 6;
                    lArray[n30] = lArray[n30] | 1L << (n5 & 0x3F);
                    arrayDeque.add(n5);
                }
            }
        }
        if (n26 * 2 > arrayList2.size()) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-FINEFLOOD] interior-dry {}/{} level={} fill={}", n26, arrayList2.size(), n4, list.size());
            }
            return null;
        }
        object = new boolean[n13 * n12];
        int n31 = n17 / 3 + 5;
        Object object2 = object;
        AbstractCollection abstractCollection = new ArrayDeque();
        AbstractCollection abstractCollection2 = list.iterator();
        while (abstractCollection2.hasNext()) {
            int n32 = abstractCollection2.next();
            if (NetworkTracer.a(lowResGrid, blArray, n32, n23, n4)) continue;
            n11 = (lowResGrid.worldX(lowResGrid.gi(n32)) - n22) / 3;
            n10 = (lowResGrid.worldZ(lowResGrid.gj(n32)) - n142) / 3;
            if (n11 < 0 || n11 >= n13 || n10 < 0 || n10 >= n12 || object2[n10 * n13 + n11] != false) continue;
            object2[n10 * n13 + n11] = true;
            ((ArrayDeque)abstractCollection).add(new int[]{n11, n10, 0});
        }
        abstractCollection2 = new ArrayList();
        while (!((ArrayDeque)abstractCollection).isEmpty()) {
            int[] nArray = (int[])((ArrayDeque)abstractCollection).poll();
            abstractCollection2.add(nArray);
            if (nArray[2] >= n31) continue;
            for (n11 = -1; n11 <= 1; ++n11) {
                for (n10 = -1; n10 <= 1; ++n10) {
                    n9 = nArray[0] + n10;
                    n8 = nArray[1] + n11;
                    if (n10 == 0 && n11 == 0 || n9 < 0 || n9 >= n13 || n8 < 0 || n8 >= n12 || object2[n8 * n13 + n9] != false) continue;
                    object2[n8 * n13 + n9] = true;
                    ((ArrayDeque)abstractCollection).add(new int[]{n9, n8, nArray[2] + 1});
                }
            }
        }
        ArrayList<int[]> arrayList4 = abstractCollection2;
        long l2 = System.nanoTime();
        LowResGrid.parallelFor(arrayList4.size(), n3 -> {
            int[] nArray = (int[])arrayList4.get(n3);
            terrainSampler.groundHeight(n22 + nArray[0] * 3, n142 + nArray[1] * 3);
        });
        long[] lArray3 = X.get();
        lArray3[7] = lArray3[7] + (System.nanoTime() - l2);
        n31 = Math.max(4096, 4 * list.size() * (lowResGrid.spacing / 3 + 1) * (lowResGrid.spacing / 3 + 1));
        int n33 = 0;
        abstractCollection = new ArrayList();
        while (!arrayDeque.isEmpty()) {
            abstractCollection.add((Integer)((Integer)arrayDeque.poll()));
        }
        abstractCollection2 = new ArrayList<int[]>();
        while (!abstractCollection.isEmpty()) {
            int n34;
            int n35;
            int n36;
            int n37;
            int n38;
            int n39;
            int n40;
            arrayList4 = new ArrayList();
            Iterator iterator = abstractCollection.iterator();
            while (iterator.hasNext()) {
                n10 = (Integer)iterator.next();
                int n41 = n10 % n13;
                n8 = n10 / n13;
                for (n7 = 0; n7 < 4; ++n7) {
                    n6 = n41 + (n7 == 0 ? -1 : (n7 == 1 ? 1 : 0));
                    n5 = n8 + (n7 == 2 ? -1 : (n7 == 3 ? 1 : 0));
                    if (n6 < 0 || n6 >= n13 || n5 < 0 || n5 >= n12 || blArray2[n5 * n13 + n6]) continue;
                    n40 = n22 + n6 * 3;
                    n39 = n142 + n5 * 3;
                    if (object[n5 * n13 + n6] == false) {
                        object[n5 * n13 + n6] = true;
                        arrayList4.add(new int[]{n40, n39});
                    }
                    n38 = n22 + n41 * 3;
                    n37 = n142 + n8 * 3;
                    n36 = Integer.signum(n40 - n38);
                    n35 = Integer.signum(n39 - n37);
                    for (n34 = 1; n34 < 3; ++n34) {
                        arrayList4.add(new int[]{n38 + n36 * n34, n37 + n35 * n34});
                    }
                }
            }
            if (!arrayList4.isEmpty()) {
                long l3 = System.nanoTime();
                ArrayList<int[]> arrayList5 = arrayList4;
                LowResGrid.parallelFor(arrayList5.size(), n -> {
                    int[] nArray = (int[])arrayList5.get(n);
                    terrainSampler.groundHeight(nArray[0], nArray[1]);
                });
                long[] lArray4 = X.get();
                lArray4[7] = lArray4[7] + (System.nanoTime() - l3);
            }
            for (int i = 0; i < abstractCollection.size(); ++i) {
                if (++n33 > n31) {
                    if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                        Constants.LOG.info("[SR-FINEFLOOD] budget level={} fill={} budget={}", n4, list.size(), n31);
                    }
                    return null;
                }
                n10 = (Integer)abstractCollection.get(i);
                int n42 = n10 % n13;
                n8 = n10 / n13;
                for (n7 = 0; n7 < 4; ++n7) {
                    n6 = n42 + (n7 == 0 ? -1 : (n7 == 1 ? 1 : 0));
                    n5 = n8 + (n7 == 2 ? -1 : (n7 == 3 ? 1 : 0));
                    if (n6 < 0 || n6 >= n13 || n5 < 0 || n5 >= n12 || blArray2[n40 = n5 * n13 + n6]) continue;
                    blArray2[n40] = true;
                    n39 = n22 + n6 * 3;
                    n38 = n142 + n5 * 3;
                    if (!(terrainSampler.groundHeight(n39, n38) < (double)n4)) continue;
                    n37 = n22 + n42 * 3;
                    n36 = n142 + n8 * 3;
                    n35 = Integer.signum(n39 - n37);
                    n34 = Integer.signum(n38 - n36);
                    boolean bl = false;
                    for (int j = 1; j < 3 && !bl; ++j) {
                        bl = terrainSampler.groundHeight(n37 + n35 * j, n36 + n34 * j) >= (double)n4;
                    }
                    if (bl) {
                        blArray2[n40] = false;
                        continue;
                    }
                    if (!plateDomain.contains(n39, n38) || plateDomain.borderDistance(n39, n38) < d) {
                        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                            Constants.LOG.info("[SR-FINEFLOOD] border at {},{} level={} fill={}", n39, n38, n4, list.size());
                        }
                        return null;
                    }
                    if (n6 == 0 || n6 == n13 - 1 || n5 == 0 || n5 == n12 - 1) {
                        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                            Constants.LOG.info("[SR-FINEFLOOD] bound at {},{} level={} fill={}", n39, n38, n4, list.size());
                        }
                        return null;
                    }
                    if (dArray != null) {
                        dArray[0] = dArray[0] + ((double)n4 - terrainSampler.groundHeight(n39, n38));
                        dArray[1] = dArray[1] + 1.0;
                    }
                    int n43 = n40 >>> 6;
                    lArray[n43] = lArray[n43] | 1L << (n40 & 0x3F);
                    abstractCollection2.add(n40);
                }
            }
            AbstractCollection abstractCollection3 = abstractCollection;
            abstractCollection = abstractCollection2;
            abstractCollection2 = abstractCollection3;
            abstractCollection2.clear();
        }
        return new Lake(n4, 0, 3, n22, n142, n13, n12, lArray);
    }

    private static boolean a(Lake lake, double d) {
        int n = lake.cellsWide();
        int n2 = lake.cellsHigh();
        long[] lArray = lake.mask();
        int n3 = 0;
        for (long i : lArray) {
            n3 += Long.bitCount(i);
        }
        if (n3 == 0) {
            return false;
        }
        boolean[] blArray = new boolean[n * n2];
        int n4 = 0;
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        for (int i = 0; i < n * n2; ++i) {
            if (blArray[i] || (lArray[i >>> 6] & 1L << (i & 0x3F)) == 0L) continue;
            int n5 = 0;
            blArray[i] = true;
            arrayDeque.add(i);
            while (!arrayDeque.isEmpty()) {
                int[][] nArrayArray;
                int n6 = (Integer)arrayDeque.poll();
                ++n5;
                int n7 = n6 % n;
                int n8 = n6 / n;
                for (int[] nArray : nArrayArray = new int[][]{{n7 - 1, n8}, {n7 + 1, n8}, {n7, n8 - 1}, {n7, n8 + 1}}) {
                    int n9;
                    if (nArray[0] < 0 || nArray[0] >= n || nArray[1] < 0 || nArray[1] >= n2 || blArray[n9 = nArray[1] * n + nArray[0]] || (lArray[n9 >>> 6] & 1L << (n9 & 0x3F)) == 0L) continue;
                    blArray[n9] = true;
                    arrayDeque.add(n9);
                }
            }
            if (!((double)(n4 = Math.max(n4, n5)) >= (double)n3 * d)) continue;
            return true;
        }
        return (double)n4 >= (double)n3 * d;
    }

    private static Lake a(List<Integer> list, int n, int n2, int n3, LowResGrid lowResGrid) {
        int n42;
        int n5 = Integer.MAX_VALUE;
        int n6 = Integer.MAX_VALUE;
        int n7 = Integer.MIN_VALUE;
        int n8 = Integer.MIN_VALUE;
        for (int n42 : list) {
            n5 = Math.min(n5, lowResGrid.gi(n42));
            n7 = Math.max(n7, lowResGrid.gi(n42));
            n6 = Math.min(n6, lowResGrid.gj(n42));
            n8 = Math.max(n8, lowResGrid.gj(n42));
        }
        int n9 = n3;
        n42 = (n7 += n9) - (n5 -= n9) + 1;
        int n10 = (n8 += n9) - (n6 -= n9) + 1;
        long[] lArray = new long[n42 * n10 + 63 >>> 6];
        for (int n11 : list) {
            int n12 = lowResGrid.gi(n11) - n5;
            int n13 = lowResGrid.gj(n11) - n6;
            for (int i = -n9; i <= n9; ++i) {
                for (int j = -n9; j <= n9; ++j) {
                    int n14 = n12 + j;
                    int n15 = n13 + i;
                    if (n14 < 0 || n14 >= n42 || n15 < 0 || n15 >= n10) continue;
                    int n16 = n15 * n42 + n14;
                    int n17 = n16 >>> 6;
                    lArray[n17] = lArray[n17] | 1L << (n16 & 0x3F);
                }
            }
        }
        return new Lake(n, n2, lowResGrid.spacing, lowResGrid.worldX(n5), lowResGrid.worldZ(n6), n42, n10, lArray);
    }

    private static double a(LowResGrid lowResGrid, int n, int n2) {
        double d = lowResGrid.gi(n) - lowResGrid.gi(n2);
        double d2 = lowResGrid.gj(n) - lowResGrid.gj(n2);
        return Math.hypot(d, d2) * (double)lowResGrid.spacing;
    }

    private static void a(PendingOutflow pendingOutflow, LowResGrid lowResGrid, int[] nArray, int[] nArray2, List<Lake> list, double[] dArray, int n, TerrainSampler terrainSampler, StreamsConfig streamsConfig, long l, List<RiverSegment> list2) {
        List<Pt> list3;
        Object object;
        int n2;
        ArrayList<Pt> arrayList = new ArrayList<Pt>();
        double d = pendingOutflow.startLevel() + (double)RiverEngine.insetFor(NetworkTracer.b(pendingOutflow.acc(), streamsConfig), streamsConfig);
        arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(pendingOutflow.outlet())), lowResGrid.worldZ(lowResGrid.gj(pendingOutflow.outlet())), d, pendingOutflow.acc()));
        int n3 = pendingOutflow.outlet();
        boolean bl = false;
        double d2 = Double.NaN;
        int n4 = -1;
        Object object2 = null;
        int n5 = lowResGrid.size();
        while (n5-- > 0) {
            n2 = nArray[n3];
            if (n2 < 0) {
                bl = true;
                break;
            }
            if (nArray2[n2] >= 0) {
                object = list.get(nArray2[n2]);
                if ((double)((Lake)object).level() >= pendingOutflow.startLevel() - 1.0) {
                    return;
                }
                arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n2)), lowResGrid.worldZ(lowResGrid.gj(n2)), ((Lake)object).level(), pendingOutflow.acc()));
                d2 = ((Lake)object).level();
                object2 = object;
                break;
            }
            double d3 = dArray[n2];
            double d4 = NetworkTracer.a(list, (double)lowResGrid.worldX(lowResGrid.gi(n2)), (double)lowResGrid.worldZ(lowResGrid.gj(n2)), pendingOutflow.acc(), streamsConfig);
            if (!Double.isNaN(d4)) {
                d3 = Math.max(d3, d4);
            }
            double d5 = Math.min(((Pt)arrayList.get(arrayList.size() - 1)).y(), Math.max((double)n, d3));
            arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n2)), lowResGrid.worldZ(lowResGrid.gj(n2)), d5, pendingOutflow.acc()));
            if (dArray[n2] <= (double)n) {
                d2 = n;
                n4 = n2;
                break;
            }
            n3 = n2;
        }
        n2 = pendingOutflow.outlet() * 131 + 9;
        object = new ColumnCache();
        List<Pt> list4 = list3 = streamsConfig.valleySnapRadius() > 0 ? NetworkTracer.a(arrayList, terrainSampler, (ColumnCache)object, streamsConfig, false, list) : arrayList;
        if (object2 != null && list3.size() >= 2) {
            int n6 = list3.size();
            if ((list3 = NetworkTracer.a(list3, object2, terrainSampler, (ColumnCache)object, streamsConfig)).size() < n6 || list3.size() < 2) {
                return;
            }
        }
        if (list3.size() >= 2) {
            if (n4 >= 0) {
                List<Pt> list5 = NetworkTracer.a(list3, lowResGrid, n4, n, terrainSampler, streamsConfig);
                if (list5 == list3) {
                    return;
                }
                list3 = list5;
            } else if (!Double.isNaN(d2)) {
                list3 = NetworkTracer.b(list3, d2, 8.0);
            } else if (bl) {
                List<Pt> list6 = NetworkTracer.a(list3, lowResGrid, n3, n, terrainSampler, streamsConfig);
                if (list6 == list3) {
                    return;
                }
                list3 = list6;
            }
            List<Pt> list7 = NetworkTracer.a(NetworkTracer.a(list3, (double)streamsConfig.gridSpacing() * 0.4), l, n2, streamsConfig);
            if (streamsConfig.confineToBanks()) {
                list7 = NetworkTracer.a(list7, terrainSampler, (ColumnCache)object, streamsConfig);
            }
            list7 = NetworkTracer.a(list7, streamsConfig.smoothingPasses());
            NetworkTracer.a(list7, streamsConfig, list2, false);
        }
    }

    private static int a(LowResGrid lowResGrid, int n, double[] dArray, boolean[] blArray, int n2, int n3) {
        int n4 = lowResGrid.gi(n);
        int n5 = lowResGrid.gj(n);
        int n6 = -1;
        int n7 = Integer.MAX_VALUE;
        for (int i = -n3; i <= n3; ++i) {
            for (int j = -n3; j <= n3; ++j) {
                int n8;
                boolean bl;
                int n9;
                int n10;
                int n11;
                if (j == 0 && i == 0 || !lowResGrid.valid(n11 = n4 + j, n10 = n5 + i) || !lowResGrid.inDomain(n9 = lowResGrid.idx(n11, n10))) continue;
                boolean bl2 = blArray[n9] && dArray[n9] < dArray[n];
                boolean bl3 = bl = dArray[n9] <= (double)n2;
                if (!bl2 && !bl || (n8 = j * j + i * i) >= n7) continue;
                n7 = n8;
                n6 = n9;
            }
        }
        return n6;
    }

    private static boolean a(int n, LowResGrid lowResGrid, WetnessSampler wetnessSampler, ColumnCache columnCache, double d, double d2, double d3, long l) {
        double d4;
        int n2 = lowResGrid.worldX(lowResGrid.gi(n));
        int n3 = lowResGrid.worldZ(lowResGrid.gj(n));
        long l2 = (long)Math.floorDiv(n2, 64) << 32 ^ (long)Math.floorDiv(n3, 64) & 0xFFFFFFFFL;
        double d5 = columnCache.get(l2, Double.NaN);
        if (!Double.isNaN(d5)) {
            d4 = d5;
        } else {
            d4 = wetnessSampler.wetness(n2, (int)lowResGrid.height(n), n3);
            columnCache.put(l2, d4);
        }
        double d6 = Math.max(d3, Math.min(1.0, (d4 - d) / (d2 - d)));
        return Hashing.unit(l, n, 71L) >= d6;
    }

    private static boolean a(LowResGrid lowResGrid, int n) {
        int n2 = lowResGrid.gi(n);
        int n3 = lowResGrid.gj(n);
        for (int i = 0; i < 8; ++i) {
            int n4 = n2 + a[i];
            int n5 = n3 + b[i];
            if (lowResGrid.valid(n4, n5) && lowResGrid.inDomain(lowResGrid.idx(n4, n5))) continue;
            return true;
        }
        return false;
    }

    private static List<Pt> a(List<Pt> list, TerrainSampler terrainSampler, ColumnCache columnCache, StreamsConfig streamsConfig, boolean bl, List<Lake> list2) {
        int n = list.size();
        if (n < 2) {
            return list;
        }
        int n2 = streamsConfig.valleySnapRadius();
        double d = Math.max(1.0, (double)n2 / 8.0);
        double d2 = streamsConfig.seaLevel() - 1;
        ArrayList<Pt> arrayList = new ArrayList<Pt>(n);
        Pt pt = list.get(0);
        double d3 = NetworkTracer.a(list2, pt.x(), pt.z(), pt.acc(), streamsConfig);
        double d4 = bl ? NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt.acc(), pt.x(), pt.z()) : (Double.isNaN(d3) ? pt.y() : Math.max(pt.y(), d3));
        arrayList.add(new Pt(pt.x(), pt.z(), d4, pt.acc()));
        double d5 = d4;
        for (int i = 1; i < n - 1; ++i) {
            double d6;
            Pt pt2 = list.get(i - 1);
            Pt pt3 = list.get(i);
            Pt pt4 = list.get(i + 1);
            double d7 = pt4.x() - pt2.x();
            double d8 = pt4.z() - pt2.z();
            double d9 = Math.hypot(d7, d8);
            double d10 = pt3.x();
            double d11 = pt3.z();
            double d12 = NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt3.acc(), pt3.x(), pt3.z());
            if (d9 > 1.0E-6 && Double.isNaN(NetworkTracer.a(list2, pt3.x(), pt3.z()))) {
                d6 = -d8 / d9;
                double d13 = d7 / d9;
                double d14 = 0.0;
                if (terrainSampler.hasEstimate()) {
                    var42_29 = NetworkTracer.b(terrainSampler, columnCache, pt3.x(), pt3.z());
                    for (var44_30 = (double)(-n2); var44_30 <= (double)n2 + 1.0E-9; var44_30 += d) {
                        double d15;
                        double d16 = pt3.x() + d6 * var44_30;
                        double d17 = pt3.z() + d13 * var44_30;
                        if (Math.abs(var44_30) < 1.0E-9 || !Double.isNaN(NetworkTracer.a(list2, d16, d17)) || !((d15 = NetworkTracer.b(terrainSampler, columnCache, d16, d17)) < var42_29)) continue;
                        var42_29 = d15;
                        d14 = var44_30;
                    }
                    if (Math.abs(d14) > 1.0E-9 && Math.abs(d14) < (double)n2 - 1.0E-9) {
                        d10 = pt3.x() + d6 * d14;
                        d11 = pt3.z() + d13 * d14;
                        d12 = NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt3.acc(), d10, d11);
                    }
                } else {
                    for (var42_29 = (double)(-n2); var42_29 <= (double)n2 + 1.0E-9; var42_29 += d) {
                        if (Math.abs(var42_29) < 1.0E-9 || !((var44_30 = NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt3.acc(), pt3.x() + d6 * var42_29, pt3.z() + d13 * var42_29)) < d12)) continue;
                        d12 = var44_30;
                        d14 = var42_29;
                    }
                    if (Math.abs(d14) < (double)n2 - 1.0E-9) {
                        d10 = pt3.x() + d6 * d14;
                        d11 = pt3.z() + d13 * d14;
                    } else {
                        d12 = NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt3.acc(), pt3.x(), pt3.z());
                    }
                }
            }
            d5 = d6 = Math.max(d2, Math.min(d5, d12));
            arrayList.add(new Pt(d10, d11, d6, pt3.acc()));
        }
        Pt pt5 = list.get(n - 1);
        arrayList.add(new Pt(pt5.x(), pt5.z(), Math.min(d5, pt5.y()), pt5.acc()));
        return arrayList;
    }

    private static double a(TerrainSampler terrainSampler, ColumnCache columnCache, List<Lake> list, StreamsConfig streamsConfig, double d, double d2, double d3) {
        double d4 = NetworkTracer.a(terrainSampler, columnCache, d2, d3);
        double d5 = NetworkTracer.a(list, d2, d3, d, streamsConfig);
        return Double.isNaN(d5) ? d4 : Math.max(d4, d5);
    }

    private static double a(List<Lake> list, double d, double d2, double d3, StreamsConfig streamsConfig) {
        double d4 = NetworkTracer.a(list, d, d2);
        if (Double.isNaN(d4) && Double.isNaN(d4 = NetworkTracer.a(list, d, d2, 4.0))) {
            return Double.NaN;
        }
        return d4 + (double)RiverEngine.insetFor(NetworkTracer.b(d3, streamsConfig), streamsConfig);
    }

    private static double a(List<Lake> list, double d, double d2, double d3) {
        int n = (int)Math.floor(d);
        int n2 = (int)Math.floor(d2);
        int n3 = (int)Math.ceil(d3);
        double d4 = Double.NaN;
        for (Lake lake : list) {
            block1: for (int i = -n3; i <= n3; ++i) {
                for (int j = -n3; j <= n3; ++j) {
                    if ((double)(j * j + i * i) > d3 * d3 || !lake.covers(n + j, n2 + i)) continue;
                    if (Double.isNaN(d4) || (double)lake.level() < d4) {
                        d4 = lake.level();
                    }
                    i = n3;
                    continue block1;
                }
            }
        }
        return d4;
    }

    private static double a(List<Lake> list, double d, double d2) {
        int n = (int)Math.floor(d);
        int n2 = (int)Math.floor(d2);
        for (Lake lake : list) {
            if (!lake.covers(n, n2)) continue;
            return lake.level();
        }
        return Double.NaN;
    }

    private static List<Pt> a(List<Pt> list, TerrainSampler terrainSampler, ColumnCache columnCache, StreamsConfig streamsConfig, List<Lake> list2) {
        int n;
        int n2 = list.size();
        if (n2 < 3) {
            return list;
        }
        double d = streamsConfig.seaLevel() - 1;
        double[] dArray = new double[n2];
        boolean[] blArray = new boolean[n2];
        double[] dArray2 = new double[n2];
        double[] dArray3 = new double[n2];
        for (n = 0; n < n2; ++n) {
            dArray[n] = NetworkTracer.a(terrainSampler, columnCache, list.get(n).x(), list.get(n).z());
            dArray3[n] = NetworkTracer.a(list2, list.get(n).x(), list.get(n).z());
            dArray2[n] = n == 0 ? 0.0 : dArray2[n - 1] + Math.hypot(list.get(n).x() - list.get(n - 1).x(), list.get(n).z() - list.get(n - 1).z());
        }
        n = -1;
        int n3 = -1;
        for (int i = 0; i < n2; ++i) {
            if (Double.isNaN(dArray3[i])) continue;
            if (n < 0) {
                n = i;
            }
            if (n3 >= 0 && dArray3[n3] == dArray3[i] && dArray2[i] - dArray2[n3] <= 24.0) {
                for (int j = n3 + 1; j < i; ++j) {
                    dArray3[j] = dArray3[i];
                }
            }
            n3 = i;
        }
        if (n3 >= 0) {
            double d2 = dArray3[n3];
            for (int i = n3 + 1; i < n2 && dArray2[i] - dArray2[n3] <= 8.0 && dArray[i] >= d2 - 1.0; ++i) {
                dArray3[i] = d2;
            }
        }
        double d3 = n >= 0 ? dArray3[n] : Double.NaN;
        double[] dArray4 = new double[n2];
        for (int i = 0; i < n2; ++i) {
            blArray[i] = !Double.isNaN(dArray3[i]);
            double d4 = dArray4[i] = blArray[i] ? dArray3[i] + (double)RiverEngine.insetFor(NetworkTracer.b(list.get(i).acc(), streamsConfig), streamsConfig) : Double.NaN;
            if (!blArray[i]) continue;
            dArray[i] = Math.max(dArray[i], dArray4[i]);
        }
        ArrayList<Pt> arrayList = new ArrayList<Pt>(n2);
        Pt pt = list.get(0);
        double d5 = blArray[0] ? Math.max(pt.y(), dArray4[0]) : (n > 0 ? Math.max(pt.y(), d3 + (double)RiverEngine.insetFor(NetworkTracer.b(pt.acc(), streamsConfig), streamsConfig)) : pt.y());
        arrayList.add(new Pt(pt.x(), pt.z(), d5, pt.acc()));
        double d6 = d5;
        for (int i = 1; i < n2 - 1; ++i) {
            double d7;
            double d8;
            double d9;
            Pt pt2 = list.get(i - 1);
            Pt pt3 = list.get(i);
            Pt pt4 = list.get(i + 1);
            double d10 = d9 = dArray[i];
            double d11 = blArray[i] ? Math.max(d, Math.min(d6, d10)) : Math.max(d, Math.min(d6, Math.min(pt3.y(), d10)));
            double d12 = pt4.x() - pt2.x();
            double d13 = Math.hypot(d12, d8 = pt4.z() - pt2.z());
            if (d13 > 1.0E-6 && !blArray[i]) {
                double d14;
                d7 = -d8 / d13;
                double d15 = d12 / d13;
                double d16 = NetworkTracer.b(pt3.acc(), streamsConfig) + 2.0;
                double d17 = NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt3.acc(), pt3.x() + d7 * d16, pt3.z() + d15 * d16);
                double d18 = Math.min(d17, d14 = NetworkTracer.a(terrainSampler, columnCache, list2, streamsConfig, pt3.acc(), pt3.x() - d7 * d16, pt3.z() - d15 * d16));
                if (d18 > d && d18 < d11) {
                    d11 = Math.min(d11, Math.max(d18, d9 - 6.0));
                }
            }
            if (n >= 0 && i < n) {
                d7 = d3 + (double)RiverEngine.insetFor(NetworkTracer.b(pt3.acc(), streamsConfig), streamsConfig);
                d11 = Math.min(Math.max(d11, d7), d6);
            }
            d6 = d11;
            arrayList.add(new Pt(pt3.x(), pt3.z(), d11, pt3.acc()));
        }
        Pt pt5 = list.get(n2 - 1);
        double d19 = blArray[n2 - 1] ? Math.min(d6, dArray[n2 - 1]) : Math.min(d6, pt5.y());
        arrayList.add(new Pt(pt5.x(), pt5.z(), d19, pt5.acc()));
        return arrayList;
    }

    private static List<Pt> a(List<Pt> list, TerrainSampler terrainSampler, ColumnCache columnCache) {
        if (list.size() < 2) {
            return list;
        }
        Pt pt = list.get(0);
        double d = Math.min(pt.y(), NetworkTracer.a(terrainSampler, columnCache, pt.x(), pt.z()));
        if (d >= pt.y()) {
            return list;
        }
        ArrayList<Pt> arrayList = new ArrayList<Pt>(list);
        arrayList.set(0, new Pt(pt.x(), pt.z(), d, pt.acc()));
        return arrayList;
    }

    private static List<Pt> a(List<Pt> list, TerrainSampler terrainSampler, ColumnCache columnCache, StreamsConfig streamsConfig) {
        int n = list.size();
        if (n < 3) {
            return list;
        }
        double d = streamsConfig.gridSpacing();
        double d2 = Math.max(1.0, d / 8.0);
        ArrayList<Pt> arrayList = new ArrayList<Pt>(n);
        arrayList.add(list.get(0));
        for (int i = 1; i < n - 1; ++i) {
            double d3;
            double d4;
            Pt pt = list.get(i - 1);
            Pt pt2 = list.get(i);
            Pt pt3 = list.get(i + 1);
            double d5 = pt3.x() - pt.x();
            double d6 = Math.hypot(d5, d4 = pt3.z() - pt.z());
            if (d6 < 1.0E-6) {
                arrayList.add(pt2);
                continue;
            }
            double d7 = -d4 / d6;
            double d8 = d5 / d6;
            double d9 = NetworkTracer.b(pt2.acc(), streamsConfig) + 3.0;
            double d10 = NetworkTracer.a(terrainSampler, columnCache, pt2.x() - d7 * d9, pt2.z() - d8 * d9);
            if (Math.min(d10, d3 = NetworkTracer.a(terrainSampler, columnCache, pt2.x() + d7 * d9, pt2.z() + d8 * d9)) >= pt2.y()) {
                arrayList.add(pt2);
                continue;
            }
            boolean bl = terrainSampler.hasEstimate();
            double d11 = 0.0;
            double d12 = bl ? NetworkTracer.b(terrainSampler, columnCache, pt2.x(), pt2.z()) : NetworkTracer.a(terrainSampler, columnCache, pt2.x(), pt2.z());
            for (double d13 = -d; d13 <= d + 1.0E-9; d13 += d2) {
                double d14;
                double d15 = pt2.x() + d7 * d13;
                double d16 = pt2.z() + d8 * d13;
                double d17 = d14 = bl ? NetworkTracer.b(terrainSampler, columnCache, d15, d16) : NetworkTracer.a(terrainSampler, columnCache, d15, d16);
                if (!(d14 < d12)) continue;
                d12 = d14;
                d11 = d13;
            }
            if (Math.abs(d11) > 1.0E-9 && Math.abs(d11) < d - 1.0E-9) {
                arrayList.add(new Pt(pt2.x() + d7 * d11, pt2.z() + d8 * d11, pt2.y(), pt2.acc()));
                continue;
            }
            arrayList.add(pt2);
        }
        arrayList.add(list.get(n - 1));
        return arrayList;
    }

    private static double a(TerrainSampler terrainSampler, double d, double d2) {
        return terrainSampler.groundHeight((int)Math.round(d), (int)Math.round(d2));
    }

    private static double a(TerrainSampler terrainSampler, ColumnCache columnCache, double d, double d2) {
        int n;
        int n2 = (int)Math.round(d);
        long l = ColumnKey.of(n2, n = (int)Math.round(d2));
        double d3 = columnCache.get(l, Double.NaN);
        if (!Double.isNaN(d3)) {
            return d3;
        }
        double d4 = terrainSampler.groundHeight(n2, n);
        columnCache.put(l, d4);
        return d4;
    }

    private static double a(TerrainSampler terrainSampler, Map<Long, Double> map, double d, double d2) {
        int n;
        int n2 = (int)Math.round(d);
        long l = ColumnKey.of(n2, n = (int)Math.round(d2));
        Double d3 = map.get(l);
        if (d3 != null) {
            return d3;
        }
        double d4 = terrainSampler.groundHeight(n2, n);
        map.put(l, d4);
        return d4;
    }

    private static double b(TerrainSampler terrainSampler, ColumnCache columnCache, double d, double d2) {
        int n;
        int n2 = (int)Math.round(d);
        long l = ColumnKey.mix(ColumnKey.pack(n2, n = (int)Math.round(d2)) ^ 0x4000000000000000L);
        double d3 = columnCache.get(l, Double.NaN);
        if (!Double.isNaN(d3)) {
            return d3;
        }
        double d4 = terrainSampler.estimate(n2, n);
        columnCache.put(l, d4);
        return d4;
    }

    private static List<Pt> a(List<Pt> list, double d) {
        if (list.size() < 2 || d < 1.0) {
            return list;
        }
        ArrayList<Pt> arrayList = new ArrayList<Pt>();
        for (int i = 0; i < list.size() - 1; ++i) {
            Pt pt = list.get(i);
            Pt pt2 = list.get(i + 1);
            arrayList.add(pt);
            double d2 = Math.hypot(pt2.x() - pt.x(), pt2.z() - pt.z());
            int n = (int)Math.floor(d2 / d);
            for (int j = 1; j <= n; ++j) {
                double d3 = (double)j * d / d2;
                if (!(d3 < 1.0)) continue;
                arrayList.add(NetworkTracer.a(pt, pt2, d3));
            }
        }
        arrayList.add(list.get(list.size() - 1));
        return arrayList;
    }

    private static List<Pt> a(List<Pt> list, long l, int n, StreamsConfig streamsConfig) {
        int n2 = list.size();
        if (n2 < 3) {
            return list;
        }
        double d = (double)streamsConfig.gridSpacing() * streamsConfig.meanderStrength();
        double d2 = Hashing.unit(l, n, 11L) * Math.PI * 2.0;
        double d3 = Hashing.unit(l, n, 12L) * Math.PI * 2.0;
        double d4 = Hashing.unit(l, n, 13L) * Math.PI * 2.0;
        double[] dArray = new double[n2];
        for (int i = 1; i < n2; ++i) {
            dArray[i] = dArray[i - 1] + Math.hypot(list.get(i).x() - list.get(i - 1).x(), list.get(i).z() - list.get(i - 1).z());
        }
        ArrayList<Pt> arrayList = new ArrayList<Pt>(n2);
        arrayList.add(list.get(0));
        for (int i = 1; i < n2 - 1; ++i) {
            double d5;
            Pt pt = list.get(i - 1);
            Pt pt2 = list.get(i);
            Pt pt3 = list.get(i + 1);
            double d6 = pt3.x() - pt.x();
            double d7 = Math.hypot(d6, d5 = pt3.z() - pt.z());
            if (d7 < 1.0E-6) {
                arrayList.add(pt2);
                continue;
            }
            double d8 = -d5 / d7;
            double d9 = d6 / d7;
            double d10 = Math.sin(Math.PI * (double)i / (double)(n2 - 1));
            double d11 = Math.abs(pt.y() - pt3.y()) / d7;
            double d12 = Math.max(0.0, 1.0 - d11 / 1.0);
            double d13 = d * d10 * d12 * (0.5 * Math.sin(dArray[i] * 0.03 + d2) + 0.3 * Math.sin(dArray[i] * 0.07 + d3) + 0.2 * Math.sin(dArray[i] * 0.017 + d4));
            arrayList.add(new Pt(pt2.x() + d8 * d13, pt2.z() + d9 * d13, pt2.y(), pt2.acc()));
        }
        arrayList.add(list.get(n2 - 1));
        return arrayList;
    }

    private static List<Pt> a(List<Pt> list, int n) {
        for (int i = 0; i < n && list.size() >= 3; ++i) {
            ArrayList<Pt> arrayList = new ArrayList<Pt>(list.size() * 2);
            arrayList.add(list.get(0));
            for (int j = 0; j < list.size() - 1; ++j) {
                arrayList.add(NetworkTracer.a(list.get(j), list.get(j + 1), 0.25));
                arrayList.add(NetworkTracer.a(list.get(j), list.get(j + 1), 0.75));
            }
            arrayList.add(list.get(list.size() - 1));
            list = arrayList;
        }
        return list;
    }

    private static Pt a(Pt pt, Pt pt2, double d) {
        return new Pt(pt.x() + (pt2.x() - pt.x()) * d, pt.z() + (pt2.z() - pt.z()) * d, pt.y() + (pt2.y() - pt.y()) * d, pt.acc() + (pt2.acc() - pt.acc()) * d);
    }

    private static void a(List<Pt> list, StreamsConfig streamsConfig, List<RiverSegment> list2, boolean bl) {
        double d = bl ? (double)streamsConfig.gridSpacing() * 1.5 : 0.0;
        double d2 = 0.0;
        for (int i = 0; i < list.size() - 1; ++i) {
            Pt pt = list.get(i);
            Pt pt2 = list.get(i + 1);
            double d3 = Math.hypot(pt2.x() - pt.x(), pt2.z() - pt.z());
            double d4 = NetworkTracer.b(pt.acc(), streamsConfig) * NetworkTracer.c(d2, d);
            double d5 = NetworkTracer.b(pt2.acc(), streamsConfig) * NetworkTracer.c(d2 + d3, d);
            list2.add(new RiverSegment(pt.x(), pt.z(), pt.y(), d4, pt2.x(), pt2.z(), pt2.y(), d5));
            d2 += d3;
        }
    }

    private static double c(double d, double d2) {
        return d2 <= 0.0 ? 1.0 : Math.min(1.0, d / d2);
    }

    private static List<Pt> a(List<Pt> list, LowResGrid lowResGrid, int n, int n2, TerrainSampler terrainSampler, StreamsConfig streamsConfig) {
        int n3;
        List<Object> list2;
        int n4;
        int n5 = list.size();
        if (n5 < 2 || n < 0) {
            return list;
        }
        Pt pt = list.get(n5 - 1);
        double d = Math.min(pt.y(), (double)n2);
        int n6 = n4 = NetworkTracer.b(pt.acc(), streamsConfig) < 1.0 ? 1 : 2;
        if (y) {
            int n7;
            int n82;
            int n9;
            int n10;
            List<Integer> list3 = NetworkTracer.a(lowResGrid, n, (double)(n2 + 1), (double)(n2 - 1), 1);
            if (list3 == null || NetworkTracer.a(list3, lowResGrid, terrainSampler, n2)) {
                return list;
            }
            if (Boolean.getBoolean("streamsreflowing.mouthconfirm") && terrainSampler.surfaceHeight(n10 = Math.round(lowResGrid.worldX(lowResGrid.gi(n9 = list3.isEmpty() ? n : list3.get(list3.size() - 1)))), n82 = Math.round(lowResGrid.worldZ(lowResGrid.gj(n9)))) - terrainSampler.groundHeight(n10, n82) < 0.99) {
                return list;
            }
            ArrayList<Pt> arrayList = new ArrayList<Pt>(list);
            for (int n82 : list3) {
                arrayList.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n82)), lowResGrid.worldZ(lowResGrid.gj(n82)), d, pt.acc()));
            }
            int n11 = n7 = list3.isEmpty() ? n : list3.get(list3.size() - 1);
            if (!NetworkTracer.b(lowResGrid, terrainSampler, n7, n2)) {
                return list;
            }
            return lowResGrid.height(n7) <= (double)(n2 - 4) ? NetworkTracer.b(arrayList, d, 8.0) : NetworkTracer.b(arrayList, d, Math.max(32.0, 3.0 * NetworkTracer.b(pt.acc(), streamsConfig)));
        }
        List<Integer> list4 = NetworkTracer.a(lowResGrid, n, (double)(n2 + 1), (double)(n2 - 2), n4);
        if (list4 != null && NetworkTracer.a(list4, lowResGrid, terrainSampler, n2)) {
            list4 = null;
        }
        if (list4 == null) {
            list2 = NetworkTracer.a(lowResGrid, n, (double)(n2 + 1), (double)(n2 - 1), n4);
            if (list2 == null || NetworkTracer.a(list2, lowResGrid, terrainSampler, n2)) {
                return list;
            }
            int n12 = list2.isEmpty() ? n : (Integer)list2.get(list2.size() - 1);
            List<Integer> list5 = NetworkTracer.a(lowResGrid, n12, (double)(n2 + 1), (double)(n2 - 2), n4);
            if (list5 == null || NetworkTracer.a(list5, lowResGrid, terrainSampler, n2)) {
                return list;
            }
            list4 = new ArrayList<Object>(list2);
            list4.addAll(list5);
        }
        list2 = new ArrayList<Pt>(list);
        for (int n13 : list4) {
            list2.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n13)), lowResGrid.worldZ(lowResGrid.gj(n13)), d, pt.acc()));
        }
        int n14 = n3 = list4.isEmpty() ? n : list4.get(list4.size() - 1);
        if (terrainSampler.hasEstimate()) {
            List<Integer> list6;
            int n15 = n3;
            double d2 = NetworkTracer.a(lowResGrid, terrainSampler, n15, n2);
            for (int i = 0; i < 24 && d2 > (double)(n2 - 24) && (list6 = NetworkTracer.a(lowResGrid, terrainSampler, n15, (double)(n2 + 1), d2 - 1.0, n4, n2)) != null && !list6.isEmpty() && !NetworkTracer.a(list6, lowResGrid, terrainSampler, n2); ++i) {
                for (int n16 : list6) {
                    list2.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n16)), lowResGrid.worldZ(lowResGrid.gj(n16)), d, pt.acc()));
                }
                n15 = list6.get(list6.size() - 1);
                d2 = NetworkTracer.a(lowResGrid, terrainSampler, n15, n2);
            }
            if (!NetworkTracer.b(lowResGrid, terrainSampler, n15, n2)) {
                return list;
            }
            return d2 <= (double)(n2 - 4) ? NetworkTracer.b(list2, d, 8.0) : NetworkTracer.b(list2, d, Math.max(32.0, 3.0 * NetworkTracer.b(pt.acc(), streamsConfig)));
        }
        List<Integer> list7 = NetworkTracer.a(lowResGrid, n3, (double)(n2 + 1), (double)(n2 - 4 + 1), n4);
        if (list7 != null && !NetworkTracer.a(list7, lowResGrid, terrainSampler, n2)) {
            int n17;
            for (int n18 : list7) {
                list2.add(new Pt(lowResGrid.worldX(lowResGrid.gi(n18)), lowResGrid.worldZ(lowResGrid.gj(n18)), d, pt.acc()));
            }
            int n19 = n17 = list7.isEmpty() ? n3 : list7.get(list7.size() - 1);
            if (!NetworkTracer.b(lowResGrid, terrainSampler, n17, n2)) {
                return list;
            }
            if (!NetworkTracer.a(terrainSampler, Math.round(lowResGrid.worldX(lowResGrid.gi(n17))), (int)Math.round(d), Math.round(lowResGrid.worldZ(lowResGrid.gj(n17))), streamsConfig)) {
                return list;
            }
            return NetworkTracer.b(list2, d, 8.0);
        }
        if (!NetworkTracer.b(lowResGrid, terrainSampler, n3, n2)) {
            return list;
        }
        if (!NetworkTracer.a(terrainSampler, Math.round(lowResGrid.worldX(lowResGrid.gi(n3))), (int)Math.round(d), Math.round(lowResGrid.worldZ(lowResGrid.gj(n3))), streamsConfig)) {
            return list;
        }
        return NetworkTracer.b(list2, d, Math.max(32.0, 3.0 * NetworkTracer.b(pt.acc(), streamsConfig)));
    }

    private static List<Pt> a(List<Pt> list, LowResGrid lowResGrid, int n, TerrainSampler terrainSampler, StreamsConfig streamsConfig) {
        boolean bl;
        int n2;
        int n3 = list.size();
        if (n3 < 2 || n < 0) {
            return list;
        }
        Pt pt = list.get(n3 - 1);
        int n4 = Math.round(lowResGrid.worldX(lowResGrid.gi(n)));
        double d = terrainSampler.riverSurface(n4, n2 = Math.round(lowResGrid.worldZ(lowResGrid.gj(n))));
        boolean bl2 = bl = !Double.isNaN(d);
        if (!bl && (d = terrainSampler.surfaceHeight(n4, n2)) - terrainSampler.groundHeightExact(n4, n2) < 0.99) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-RTF] river mouth at {},{}: DRY (surf={} ground={}) -> chain dropped", n4, n2, d, terrainSampler.groundHeightExact(n4, n2));
            }
            return list;
        }
        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-RTF] river mouth at {},{}: surf={} ({}) -> accepted", n4, n2, d, bl ? "generator" : "placed water");
        }
        double d2 = Math.min(pt.y(), d + (double)RiverEngine.insetFor(NetworkTracer.b(pt.acc(), streamsConfig), streamsConfig));
        ArrayList<Pt> arrayList = new ArrayList<Pt>(list);
        arrayList.add(new Pt(n4, n2, d2, pt.acc()));
        return NetworkTracer.b(arrayList, d2, 8.0);
    }

    private static double a(LowResGrid lowResGrid, TerrainSampler terrainSampler, int n, int n2) {
        double d = lowResGrid.height(n);
        if (d == -1001.0) {
            return terrainSampler.surfaceHeight(Math.round(lowResGrid.worldX(lowResGrid.gi(n))), Math.round(lowResGrid.worldZ(lowResGrid.gj(n))));
        }
        if (d != -1000.0) {
            return d;
        }
        double d2 = terrainSampler.estimate(Math.round(lowResGrid.worldX(lowResGrid.gi(n))), Math.round(lowResGrid.worldZ(lowResGrid.gj(n))));
        return Math.max(d2, (double)(n2 - 24));
    }

    private static boolean a(TerrainSampler terrainSampler, int n, int n2, int n3, StreamsConfig streamsConfig) {
        String string = streamsConfig.outletBiomes();
        if (string == null || string.isBlank()) {
            return true;
        }
        int n4 = Math.max(1, streamsConfig.gridSpacing());
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (!terrainSampler.outletBiomeOk(n + j * n4, n2, n3 + i * n4)) continue;
                return true;
            }
        }
        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-MOUTH] no whitelisted outlet biome within one cell of {},{} -> chain dropped", (Object)n, (Object)n3);
        }
        return false;
    }

    private static boolean b(LowResGrid lowResGrid, TerrainSampler terrainSampler, int n, int n2) {
        int n3;
        boolean bl;
        int n4 = Math.round(lowResGrid.worldX(lowResGrid.gi(n)));
        boolean bl2 = bl = terrainSampler.surfaceHeight(n4, n3 = Math.round(lowResGrid.worldZ(lowResGrid.gj(n)))) - terrainSampler.groundHeightExact(n4, n3) >= 0.99;
        if (!bl && Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-MOUTH] final connection check failed at {},{} -> chain dropped", (Object)n4, (Object)n3);
        }
        return bl;
    }

    private static List<Integer> a(LowResGrid lowResGrid, TerrainSampler terrainSampler, int n, double d, double d2, int n2, int n3) {
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> hashMap2 = new HashMap<Integer, Integer>();
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        hashMap.put(n, n);
        hashMap2.put(n, 0);
        arrayDeque.add(n);
        while (!arrayDeque.isEmpty()) {
            int n4;
            int n5 = (Integer)arrayDeque.poll();
            if (n5 != n && NetworkTracer.a(lowResGrid, terrainSampler, n5, n3) <= d2) {
                LinkedList<Integer> linkedList = new LinkedList<Integer>();
                n4 = n5;
                while (n4 != n) {
                    linkedList.addFirst(n4);
                    n4 = (Integer)hashMap.get(n4);
                }
                return linkedList;
            }
            int n6 = (Integer)hashMap2.get(n5);
            if (n6 >= n2) continue;
            n4 = lowResGrid.gi(n5);
            int n7 = lowResGrid.gj(n5);
            for (int i = 0; i < 8; ++i) {
                int n8;
                int n9 = n4 + a[i];
                int n10 = n7 + b[i];
                if (!lowResGrid.valid(n9, n10) || hashMap.containsKey(n8 = lowResGrid.idx(n9, n10)) || NetworkTracer.a(lowResGrid, terrainSampler, n8, n3) > d) continue;
                hashMap.put(n8, n5);
                hashMap2.put(n8, n6 + 1);
                arrayDeque.add(n8);
            }
        }
        return null;
    }

    private static boolean a(List<Integer> list, LowResGrid lowResGrid, TerrainSampler terrainSampler, int n) {
        for (int n2 : list) {
            if (!(terrainSampler.groundHeight(lowResGrid.worldX(lowResGrid.gi(n2)), lowResGrid.worldZ(lowResGrid.gj(n2))) > (double)n + 24.0)) continue;
            return true;
        }
        return false;
    }

    private static List<Integer> a(LowResGrid lowResGrid, int n, double d, double d2, int n2) {
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> hashMap2 = new HashMap<Integer, Integer>();
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        hashMap.put(n, n);
        hashMap2.put(n, 0);
        arrayDeque.add(n);
        while (!arrayDeque.isEmpty()) {
            int n3;
            int n4 = (Integer)arrayDeque.poll();
            if (lowResGrid.height(n4) <= d2) {
                LinkedList<Integer> linkedList = new LinkedList<Integer>();
                n3 = n4;
                while (n3 != n) {
                    linkedList.addFirst(n3);
                    n3 = (Integer)hashMap.get(n3);
                }
                return linkedList;
            }
            int n5 = (Integer)hashMap2.get(n4);
            if (n5 >= n2) continue;
            n3 = lowResGrid.gi(n4);
            int n6 = lowResGrid.gj(n4);
            for (int i = 0; i < 8; ++i) {
                int n7;
                int n8 = n3 + a[i];
                int n9 = n6 + b[i];
                if (!lowResGrid.valid(n8, n9) || hashMap.containsKey(n7 = lowResGrid.idx(n8, n9)) || lowResGrid.height(n7) > d) continue;
                hashMap.put(n7, n4);
                hashMap2.put(n7, n5 + 1);
                arrayDeque.add(n7);
            }
        }
        return null;
    }

    private static List<Pt> b(List<Pt> list, double d, double d2) {
        double d3;
        int n = list.size();
        if (n < 2) {
            return list;
        }
        Pt pt = list.get(n - 1);
        Pt pt2 = list.get(n - 2);
        double d4 = pt.x() - pt2.x();
        double d5 = Math.hypot(d4, d3 = pt.z() - pt2.z());
        if (d5 < 1.0E-6) {
            return list;
        }
        double d6 = Math.min(pt.y(), d);
        ArrayList<Pt> arrayList = new ArrayList<Pt>(list);
        arrayList.add(new Pt(pt.x() + d4 / d5 * d2, pt.z() + d3 / d5 * d2, d6, pt.acc()));
        return arrayList;
    }

    public static RiverNetwork connectNetwork(PlateKey plateKey, Function<PlateKey, RiverNetwork> function, PlateLattice plateLattice, TerrainSampler terrainSampler, StreamsConfig streamsConfig, long l) {
        RiverNetwork riverNetwork = function.apply(plateKey);
        double d = streamsConfig.connectRadius();
        int n = streamsConfig.seaLevel();
        ArrayList<RiverSegment> arrayList = new ArrayList<RiverSegment>();
        HashSet<RiverSegment> hashSet = new HashSet<RiverSegment>();
        HashMap<PlateKey, RiverNetwork> hashMap = new HashMap<PlateKey, RiverNetwork>();
        hashMap.put(plateKey, riverNetwork);
        for (RiverSegment object : riverNetwork.segments()) {
            if (!NetworkTracer.a(object, n)) continue;
            double riverSegment = object.x1();
            double d2 = object.z1();
            double d3 = object.y1();
            PlateKey[] plateKeyArray = plateLattice.platesWithin(riverSegment, d2, d);
            for (PlateKey plateKey2 : plateKeyArray) {
                hashMap.computeIfAbsent(plateKey2, function);
            }
            Connector connector = NetworkTracer.a(riverSegment, d2, d3, plateKeyArray, hashMap, terrainSampler, streamsConfig, n, d);
            if (connector == null) continue;
            List<Pt> list = NetworkTracer.a(NetworkTracer.a(NetworkTracer.a(connector.path(), (double)streamsConfig.gridSpacing() * 0.4), l, NetworkTracer.d(riverSegment, d2), streamsConfig), streamsConfig.smoothingPasses());
            NetworkTracer.a(list, connector.wStart(), connector.wEnd(), arrayList);
            hashSet.add(object);
        }
        if (arrayList.isEmpty()) {
            return riverNetwork;
        }
        ArrayList arrayList2 = new ArrayList(riverNetwork.segments().size() + arrayList.size());
        for (RiverSegment riverSegment : riverNetwork.segments()) {
            if (hashSet.contains(riverSegment)) continue;
            arrayList2.add(riverSegment);
        }
        arrayList2.addAll(arrayList);
        return NetworkTracer.assemble(plateKey, arrayList2, riverNetwork.lakes(), streamsConfig);
    }

    private static boolean a(RiverSegment riverSegment, int n) {
        return riverSegment.x1() == riverSegment.x2() && riverSegment.z1() == riverSegment.z2() && riverSegment.y1() > (double)(n + 1);
    }

    private static Connector a(double d, double d2, double d3, PlateKey[] plateKeyArray, Map<PlateKey, RiverNetwork> map, TerrainSampler terrainSampler, StreamsConfig streamsConfig, int n, double d4) {
        double[] dArray = NetworkTracer.a(d, d2, plateKeyArray, map, n);
        double d5 = Double.MAX_VALUE;
        double d6 = 0.0;
        double d7 = 0.0;
        double d8 = 0.0;
        double d9 = 0.0;
        for (PlateKey d16 : plateKeyArray) {
            RiverNetwork riverNetwork = map.get(d16);
            if (riverNetwork == null) continue;
            for (RiverSegment riverSegment : riverNetwork.segments()) {
                double d10;
                double d11;
                if (NetworkTracer.a(riverSegment, n)) continue;
                double d12 = riverSegment.clampParam(d, d2);
                double d13 = riverSegment.x1() + (riverSegment.x2() - riverSegment.x1()) * d12;
                double d14 = Math.hypot(d13 - d, (d11 = riverSegment.z1() + (riverSegment.z2() - riverSegment.z1()) * d12) - d2);
                if (d14 < 3.0 || d14 > d4 || d14 >= d5 || (d10 = riverSegment.y1() + (riverSegment.y2() - riverSegment.y1()) * d12) > d3 || !NetworkTracer.a(dArray, d13 - d, d11 - d2)) continue;
                d5 = d14;
                d6 = d13;
                d7 = d11;
                d8 = d10;
                d9 = riverSegment.widthAt(d12);
            }
        }
        double[] dArray2 = NetworkTracer.a(d, d2, dArray, terrainSampler, streamsConfig, n, d4);
        if (dArray2 != null && dArray2[3] < d5) {
            d6 = dArray2[0];
            d7 = dArray2[1];
            d8 = n;
            d9 = 0.0;
            d5 = dArray2[3];
        }
        if (d5 == Double.MAX_VALUE) {
            return null;
        }
        if (NetworkTracer.a(d, d2, d6, d7, d3, terrainSampler) > 4.0) {
            return null;
        }
        double d15 = NetworkTracer.b(d, d2, plateKeyArray, map, n);
        double d16 = d9 > 0.0 ? d9 : d15;
        ArrayList arrayList = new ArrayList(2);
        arrayList.add(new Pt(d, d2, d3, 0.0));
        arrayList.add(new Pt(d6, d7, d8, 0.0));
        return new Connector(arrayList, d15, d16);
    }

    private static double[] a(double d, double d2, PlateKey[] plateKeyArray, Map<PlateKey, RiverNetwork> map, int n) {
        double[] dArray;
        double d3 = Double.MAX_VALUE;
        double d4 = 0.0;
        double d5 = 0.0;
        for (PlateKey plateKey : plateKeyArray) {
            RiverNetwork riverNetwork = map.get(plateKey);
            if (riverNetwork == null) continue;
            for (RiverSegment riverSegment : riverNetwork.segments()) {
                if (NetworkTracer.a(riverSegment, n)) continue;
                double d6 = Math.hypot(riverSegment.x2() - d, riverSegment.z2() - d2);
                double d7 = Math.hypot(riverSegment.x2() - riverSegment.x1(), riverSegment.z2() - riverSegment.z1());
                if (!(d6 < d3) || !(d7 > 1.0E-6)) continue;
                d3 = d6;
                d4 = (riverSegment.x2() - riverSegment.x1()) / d7;
                d5 = (riverSegment.z2() - riverSegment.z1()) / d7;
            }
        }
        if (d3 <= 3.0) {
            double[] dArray2 = new double[2];
            dArray2[0] = d4;
            dArray = dArray2;
            dArray2[1] = d5;
        } else {
            dArray = null;
        }
        return dArray;
    }

    private static double b(double d, double d2, PlateKey[] plateKeyArray, Map<PlateKey, RiverNetwork> map, int n) {
        double d3 = Double.MAX_VALUE;
        double d4 = -1.0;
        for (PlateKey plateKey : plateKeyArray) {
            RiverNetwork riverNetwork = map.get(plateKey);
            if (riverNetwork == null) continue;
            for (RiverSegment riverSegment : riverNetwork.segments()) {
                double d5;
                if (NetworkTracer.a(riverSegment, n) || !((d5 = Math.hypot(riverSegment.x2() - d, riverSegment.z2() - d2)) < d3)) continue;
                d3 = d5;
                d4 = riverSegment.w2();
            }
        }
        return d4 > 0.0 ? d4 : 0.5;
    }

    private static boolean a(double[] dArray, double d, double d2) {
        if (dArray == null) {
            return true;
        }
        return dArray[0] * d + dArray[1] * d2 >= 0.0;
    }

    private static double[] a(double d, double d2, double[] dArray, TerrainSampler terrainSampler, StreamsConfig streamsConfig, int n, double d3) {
        double[] dArray2;
        int n2 = Math.max(4, streamsConfig.gridSpacing() / 2);
        double d4 = Double.MAX_VALUE;
        double d5 = 0.0;
        double d6 = 0.0;
        block0: for (int i = 0; i < 16; ++i) {
            double d7;
            double d8 = (double)i * 0.39269908169872414;
            double d9 = Math.cos(d8);
            if (!NetworkTracer.a(dArray, d9, d7 = Math.sin(d8))) continue;
            for (double d10 = 3.0; d10 <= d3; d10 += (double)n2) {
                double d11 = d + d9 * d10;
                double d12 = d2 + d7 * d10;
                if (!(NetworkTracer.a(terrainSampler, d11, d12) <= (double)n)) continue;
                if (!(d10 < d4)) continue block0;
                d4 = d10;
                d5 = d11;
                d6 = d12;
                continue block0;
            }
        }
        if (d4 == Double.MAX_VALUE) {
            dArray2 = null;
        } else {
            double[] dArray3 = new double[4];
            dArray3[0] = d5;
            dArray3[1] = d6;
            dArray3[2] = n;
            dArray2 = dArray3;
            dArray3[3] = d4;
        }
        return dArray2;
    }

    private static double a(double d, double d2, double d3, double d4, double d5, TerrainSampler terrainSampler) {
        double d6 = Math.hypot(d3 - d, d4 - d2);
        int n = Math.max(1, (int)Math.round(d6 / 4.0));
        double d7 = 0.0;
        for (int i = 1; i < n; ++i) {
            double d8 = (double)i / (double)n;
            double d9 = NetworkTracer.a(terrainSampler, d + (d3 - d) * d8, d2 + (d4 - d2) * d8);
            d7 = Math.max(d7, d9 - d5);
        }
        return d7;
    }

    private static void a(List<Pt> list, double d, double d2, List<RiverSegment> list2) {
        double d3 = 0.0;
        for (int i = 0; i < list.size() - 1; ++i) {
            d3 += Math.hypot(list.get(i + 1).x() - list.get(i).x(), list.get(i + 1).z() - list.get(i).z());
        }
        double d4 = 0.0;
        for (int i = 0; i < list.size() - 1; ++i) {
            Pt pt = list.get(i);
            Pt pt2 = list.get(i + 1);
            double d5 = Math.hypot(pt2.x() - pt.x(), pt2.z() - pt.z());
            double d6 = d3 > 1.0E-9 ? d + (d2 - d) * (d4 / d3) : d;
            double d7 = d3 > 1.0E-9 ? d + (d2 - d) * ((d4 + d5) / d3) : d2;
            list2.add(new RiverSegment(pt.x(), pt.z(), pt.y(), d6, pt2.x(), pt2.z(), pt2.y(), d7));
            d4 += d5;
        }
    }

    private static int d(double d, double d2) {
        return (int)(Math.round(d) * 73856093L ^ Math.round(d2) * 19349663L);
    }

    private static double b(double d, StreamsConfig streamsConfig) {
        double d2 = d * (double)streamsConfig.gridSpacing() * (double)streamsConfig.gridSpacing();
        return Math.min(streamsConfig.maxWidth(), streamsConfig.baseWidth() + streamsConfig.widthScale() * Math.sqrt(d2 / 2500.0));
    }

    private static /* synthetic */ void a(List[] listArray, List list, int[] nArray, int[] nArray2, List list2, Map map, int[] nArray3, boolean[] blArray, int[] nArray4, double[] dArray, double[] dArray2, LowResGrid lowResGrid, TerrainSampler terrainSampler, WetnessSampler wetnessSampler, StreamsConfig streamsConfig, long l, int n, int n2) {
        listArray[n2] = NetworkTracer.a((Integer)list.get(n2), nArray, nArray2, list2, (Lake)map.get(nArray3[(Integer)list.get(n2)]), blArray, nArray4, dArray, dArray2, lowResGrid, terrainSampler, wetnessSampler, streamsConfig, l, n);
    }

    private static /* synthetic */ void a(List list, LowResGrid lowResGrid, TerrainSampler terrainSampler, double[] dArray, int n) {
        int n2;
        int n3 = (Integer)list.get(n);
        int n4 = Math.round(lowResGrid.worldX(lowResGrid.gi(n3)));
        double d = terrainSampler.riverSurface(n4, n2 = Math.round(lowResGrid.worldZ(lowResGrid.gj(n3))));
        dArray[n] = Double.isNaN(d) ? terrainSampler.surfaceHeight(n4, n2) : d;
    }

    private record LocalBasin(int i0, int j0, int w, int h, double[] hgt, boolean[] ok, int fb, double spill, double floor) {
        List<Integer> poolAt(int n, LowResGrid lowResGrid) {
            if (this.hgt[this.fb] >= (double)n) {
                return null;
            }
            ArrayList<Integer> arrayList = new ArrayList<Integer>();
            boolean[] blArray = new boolean[this.w * this.h];
            ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
            blArray[this.fb] = true;
            arrayDeque.add(this.fb);
            while (!arrayDeque.isEmpty()) {
                int n2 = (Integer)arrayDeque.poll();
                int n3 = n2 % this.w;
                int n4 = n2 / this.w;
                if (n3 == 0 || n4 == 0 || n3 == this.w - 1 || n4 == this.h - 1) {
                    return null;
                }
                arrayList.add(lowResGrid.idx(this.i0 + n3, this.j0 + n4));
                for (int i = 0; i < 8; ++i) {
                    int n5 = n3 + a[i];
                    int n6 = n4 + b[i];
                    int n7 = n6 * this.w + n5;
                    if (n5 < 0 || n5 >= this.w || n6 < 0 || n6 >= this.h || !this.ok[n7] || blArray[n7] || !(this.hgt[n7] < (double)n - 1.0E-6)) continue;
                    blArray[n7] = true;
                    arrayDeque.add(n7);
                }
            }
            return arrayList.isEmpty() ? null : arrayList;
        }
    }

    private record PendingOutflow(int outlet, double startLevel, double acc) {
    }

    private record Pt(double x, double z, double y, double acc) {
    }

    private record Connector(List<Pt> path, double wStart, double wEnd) {
    }
}

