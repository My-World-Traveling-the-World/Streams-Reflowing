/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class NearestRiverIndex {
    private static final double a = 16.0;
    private static final double b = 4.0;
    private static final double c = 6.0;
    private static final double d = 6.0;
    private static final double e = 64.0;
    private final List<RiverSegment> f;
    private final double[] g;
    private final double[] h;
    private final double[] i;
    private final double[] j;
    private final double[] k;
    private final double[] l;
    private final double[] m;
    private final double[] n;
    private final double[] o;
    private final double[] p;
    private final double[] q;
    private final double[] r;
    private final double s;
    private final boolean[] t;
    private final double[] u;
    private final boolean[] v;
    private final double[] w;
    private final int[] x;
    private final int y;
    private final Map<Long, int[]> z;
    private static final ThreadLocal<TieredCand[]> A = ThreadLocal.withInitial(() -> {
        TieredCand[] tieredCandArray = new TieredCand[8];
        for (int i = 0; i < tieredCandArray.length; ++i) {
            tieredCandArray[i] = new TieredCand();
        }
        return tieredCandArray;
    });
    private static final ThreadLocal<int[]> B = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<long[]> C = ThreadLocal.withInitial(() -> new long[1]);
    private static final ThreadLocal<int[][]> D = ThreadLocal.withInitial(() -> new int[][]{new int[512]});

    public NearestRiverIndex(List<RiverSegment> list, double d, int n, double d2) {
        int n2;
        Object object;
        RiverSegment riverSegment;
        int n3;
        Integer integer;
        Map<Long, Integer> serializable;
        int n4;
        this.f = list;
        this.y = Math.max(1, n);
        this.s = d2;
        HashMap<Long, List> hashMap = new HashMap<Long, List>();
        for (n4 = 0; n4 < list.size(); ++n4) {
            RiverSegment riverSegment2 = list.get(n4);
            int n5 = this.a(riverSegment2.minX() - d);
            int n6 = this.a(riverSegment2.maxX() + d);
            int n7 = this.b(riverSegment2.minZ() - d);
            int n8 = this.b(riverSegment2.maxZ() + d);
            for (int i = n5; i <= n6; ++i) {
                for (int j = n7; j <= n8; ++j) {
                    hashMap.computeIfAbsent(NearestRiverIndex.a(i, j), l -> new ArrayList()).add(n4);
                }
            }
        }
        this.z = new HashMap<Long, int[]>(hashMap.size() * 2);
        n4 = list.size();
        this.g = new double[n4];
        this.h = new double[n4];
        this.i = new double[n4];
        this.j = new double[n4];
        this.k = new double[n4];
        this.l = new double[n4];
        this.m = new double[n4];
        this.n = new double[n4];
        this.o = new double[n4];
        this.p = new double[n4];
        this.q = new double[n4];
        this.r = new double[n4];
        for (int i = 0; i < n4; ++i) {
            RiverSegment riverSegment3 = list.get(i);
            this.p[i] = riverSegment3.w1();
            this.q[i] = riverSegment3.w2();
            double d3 = Math.max(riverSegment3.w1(), riverSegment3.w2());
            double d4 = d3 + Math.max(5.0, d3 * d2);
            this.r[i] = d4 * d4;
            double d5 = riverSegment3.x2() - riverSegment3.x1();
            double d6 = riverSegment3.z2() - riverSegment3.z1();
            this.g[i] = riverSegment3.x1();
            this.h[i] = riverSegment3.z1();
            this.i[i] = d5;
            this.j[i] = d6;
            this.k[i] = d5 * d5 + d6 * d6;
            this.l[i] = Math.min(riverSegment3.x1(), riverSegment3.x2());
            this.m[i] = Math.max(riverSegment3.x1(), riverSegment3.x2());
            this.n[i] = Math.min(riverSegment3.z1(), riverSegment3.z2());
            this.o[i] = Math.max(riverSegment3.z1(), riverSegment3.z2());
        }
        for (Map.Entry entry : hashMap.entrySet()) {
            List list2 = (List)entry.getValue();
            int[] nArray = new int[list2.size()];
            for (int i = 0; i < nArray.length; ++i) {
                nArray[i] = (Integer)list2.get(i);
            }
            this.z.put((Long)entry.getKey(), nArray);
        }
        this.t = new boolean[list.size()];
        this.v = new boolean[list.size()];
        this.u = new double[list.size()];
        Arrays.fill(this.u, Double.MAX_VALUE);
        HashSet hashSet = new HashSet(list.size() * 2);
        HashMap<Long, Integer> hashMap2 = new HashMap<Long, Integer>(list.size() * 2);
        HashMap<Long, Integer> hashMap3 = new HashMap<Long, Integer>(list.size() * 2);
        for (int i = 0; i < list.size(); ++i) {
            RiverSegment riverSegment4 = list.get(i);
            hashSet.add(NearestRiverIndex.a(riverSegment4.x1(), riverSegment4.z1()));
            hashMap3.put(NearestRiverIndex.a(riverSegment4.x1(), riverSegment4.z1()), i);
            long l2 = NearestRiverIndex.a(riverSegment4.x2(), riverSegment4.z2());
            integer = hashMap2.get(l2);
            if (integer != null && !(NearestRiverIndex.a(riverSegment4) > NearestRiverIndex.a(list.get(integer)))) continue;
            hashMap2.put(l2, i);
        }
        boolean[] blArray = new boolean[list.size()];
        for (n3 = 0; n3 < list.size(); ++n3) {
            RiverSegment riverSegment5 = list.get(n3);
            blArray[n3] = !hashSet.contains(NearestRiverIndex.a(riverSegment5.x2(), riverSegment5.z2()));
        }
        block8: for (n3 = 0; n3 < list.size(); ++n3) {
            int n9 = n3;
            double d7 = 0.0;
            for (int i = 0; i < 64; ++i) {
                if (blArray[n9]) {
                    this.t[n3] = d7 <= 6.0;
                    this.u[n3] = d7;
                    continue block8;
                }
                riverSegment = list.get(n9);
                object = (Integer)hashMap3.get(NearestRiverIndex.a(riverSegment.x2(), riverSegment.z2()));
                if (object == null || (Integer)object == n9 || (d7 += Math.hypot(riverSegment.x2() - riverSegment.x1(), riverSegment.z2() - riverSegment.z1())) > 64.0) continue block8;
                n9 = (Integer)object;
            }
        }
        block10: for (n3 = 0; n3 < list.size(); ++n3) {
            int n10 = n3;
            double d8 = 0.0;
            for (int i = 0; i < 64; ++i) {
                riverSegment = list.get(n10);
                object = (Integer)hashMap2.get(NearestRiverIndex.a(riverSegment.x1(), riverSegment.z1()));
                if (object == null || (Integer)object == n10) {
                    this.v[n3] = d8 <= 6.0;
                    continue block10;
                }
                if ((d8 += Math.hypot(riverSegment.x2() - riverSegment.x1(), riverSegment.z2() - riverSegment.z1())) > 6.0) continue block10;
                n10 = (Integer)object;
            }
        }
        double[] dArray = new double[list.size()];
        for (n2 = 0; n2 < list.size(); ++n2) {
            dArray[n2] = this.a(n2, hashMap2);
        }
        this.w = new double[list.size()];
        for (n2 = 0; n2 < list.size(); ++n2) {
            RiverSegment riverSegment6;
            Integer n11;
            RiverSegment riverSegment7;
            double d9 = dArray[n2];
            int n12 = n2;
            double d10 = 0.0;
            for (int i = 0; i < 64 && d10 < 16.0 && (n11 = (Integer)hashMap2.get(NearestRiverIndex.a((riverSegment6 = list.get(n12)).x1(), riverSegment6.z1()))) != null && n11 != n12; d10 += Math.hypot(riverSegment7.x2() - riverSegment7.x1(), riverSegment7.z2() - riverSegment7.z1()), ++i) {
                d9 = Math.max(d9, dArray[n11]);
                riverSegment7 = list.get(n11);
                n12 = n11;
            }
            this.w[n2] = d9;
        }
        HashMap<Long, Integer> hashMap4 = new HashMap<Long, Integer>(list.size() * 2);
        HashMap<Long, Integer> hashMap5 = new HashMap<Long, Integer>(list.size() * 2);
        serializable = new HashMap<Long, Integer>(list.size() * 2);
        HashMap<Long, Integer> hashMap6 = new HashMap<Long, Integer>(list.size() * 2);
        for (int i = 0; i < list.size(); ++i) {
            object = list.get(i);
            long l3 = NearestRiverIndex.a(((RiverSegment)object).x1(), ((RiverSegment)object).z1());
            long l4 = NearestRiverIndex.a(((RiverSegment)object).x2(), ((RiverSegment)object).z2());
            hashMap4.merge(l3, 1, Integer::sum);
            serializable.put(l3, i);
            hashMap5.merge(l4, 1, Integer::sum);
            hashMap6.put(l4, i);
        }
        int[] nArray = new int[list.size()];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = i;
        }
        for (Map.Entry entry : serializable.entrySet()) {
            long l5 = (Long)entry.getKey();
            if (hashMap4.getOrDefault(l5, 0) != 1 || hashMap5.getOrDefault(l5, 0) != 1) continue;
            NearestRiverIndex.a(nArray, (Integer)hashMap6.get(l5), (Integer)entry.getValue());
        }
        this.x = new int[list.size()];
        for (int i = 0; i < this.x.length; ++i) {
            this.x[i] = NearestRiverIndex.a(nArray, i);
        }
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
        int n4 = NearestRiverIndex.a(nArray, n);
        if (n4 != (n3 = NearestRiverIndex.a(nArray, n2))) {
            nArray[n4] = n3;
        }
    }

    private double a(int n, Map<Long, Integer> map) {
        RiverSegment riverSegment;
        Integer n2;
        RiverSegment riverSegment2;
        RiverSegment riverSegment3 = this.f.get(n);
        double d = riverSegment3.y1() - riverSegment3.y2();
        double d2 = Math.hypot(riverSegment3.x2() - riverSegment3.x1(), riverSegment3.z2() - riverSegment3.z1());
        int n3 = n;
        for (int i = 0; i < 64 && d2 < 4.0 && (n2 = map.get(NearestRiverIndex.a((riverSegment = this.f.get(n3)).x1(), riverSegment.z1()))) != null && n2 != n3; d2 += Math.hypot(riverSegment2.x2() - riverSegment2.x1(), riverSegment2.z2() - riverSegment2.z1()), ++i) {
            riverSegment2 = this.f.get(n2);
            d += riverSegment2.y1() - riverSegment2.y2();
            n3 = n2;
        }
        return d2 > 1.0E-6 ? Math.max(0.0, d) / d2 : 0.0;
    }

    private static double a(RiverSegment riverSegment) {
        double d = Math.hypot(riverSegment.x2() - riverSegment.x1(), riverSegment.z2() - riverSegment.z1());
        return d > 1.0E-6 ? (riverSegment.y1() - riverSegment.y2()) / d : 0.0;
    }

    private static long a(double d, double d2) {
        long l = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l2 = Math.round(d2 * 16.0) & 0xFFFFFFFFL;
        return l << 32 | l2;
    }

    public RiverSample nearest(double d, double d2, double d3) {
        if (this.f.isEmpty()) {
            return null;
        }
        int[] nArray = this.a(d, d2, d3);
        double d4 = Double.MAX_VALUE;
        int n = -1;
        double d5 = 0.0;
        double d6 = d3;
        int n2 = 0;
        while (nArray[n2] >= 0) {
            int n3 = nArray[n2];
            double d9 = d < this.l[n3] ? this.l[n3] - d : (d > this.m[n3] ? d - this.m[n3] : 0.0);
            double d10 = d2 < this.n[n3] ? this.n[n3] - d2 : (d2 > this.o[n3] ? d2 - this.o[n3] : 0.0);
            if (!(d9 * d9 + d10 * d10 > d6 * d6)) {
                double d11;
                double d12;
                double d13 = this.j[n3];
                double d14;
                double d15 = this.i[n3];
                double d16 = this.k[n3];
                d14 = d16 <= 1.0E-9 ? 0.0 : ((d14 = ((d - this.g[n3]) * d15 + (d2 - this.h[n3]) * d13) / d16) < 0.0 ? 0.0 : (d14 > 1.0 ? 1.0 : d14));
                double d17 = this.g[n3] + d15 * d14;
                double d18 = d - d17;
                double d19 = Math.sqrt(d18 * d18 + (d12 = d2 - (d11 = this.h[n3] + (d13 = this.j[n3]) * d14)) * d12);
                if (d19 < d4 || d19 == d4 && n3 < n) {
                    d4 = d19;
                    n = n3;
                    d6 = d19 < d3 ? d19 : d3;
                    d5 = d14;
                }
            }
            ++n2;
        }
        if (n < 0 || d4 > d3) {
            return null;
        }
        return this.a(n, d5, d4);
    }

    public RiverSample nearestTiered(double d, double d2, double d3) {
        if (this.f.isEmpty()) {
            return null;
        }
        int[] nArray = this.a(d, d2, d3);
        double d4 = Double.MAX_VALUE;
        int n = -1;
        double d5 = 0.0;
        double d6 = Double.MAX_VALUE;
        int n2 = -1;
        double d7 = 0.0;
        double d8 = Double.MAX_VALUE;
        int n3 = -1;
        double d9 = 0.0;
        double d10 = d3;
        int n4 = 0;
        while (nArray[n4] >= 0) {
            int n5 = nArray[n4];
            double d12 = d < this.l[n5] ? this.l[n5] - d : (d > this.m[n5] ? d - this.m[n5] : 0.0);
            double d13 = d2 < this.n[n5] ? this.n[n5] - d2 : (d2 > this.o[n5] ? d2 - this.o[n5] : 0.0);
            double d14 = d12 * d12 + d13 * d13;
            if (!(d14 > d10 * d10) || !(d14 > this.r[n5])) {
                double d15;
                double d16;
                double d17 = this.j[n5];
                double d18;
                double d19 = this.i[n5];
                double d20 = this.k[n5];
                d18 = d20 <= 1.0E-9 ? 0.0 : ((d18 = ((d - this.g[n5]) * d19 + (d2 - this.h[n5]) * d17) / d20) < 0.0 ? 0.0 : (d18 > 1.0 ? 1.0 : d18));
                double d21 = this.g[n5] + d19 * d18;
                double d22 = d - d21;
                double d23 = Math.sqrt(d22 * d22 + (d16 = d2 - (d15 = this.h[n5] + (d17 = this.j[n5]) * d18)) * d16);
                if (d23 < d4 || d23 == d4 && n5 < n) {
                    d4 = d23;
                    n = n5;
                    d10 = d23 < d3 ? d23 : d3;
                    d5 = d18;
                }
                if (d23 <= d3) {
                    double d24 = this.p[n5] + (this.q[n5] - this.p[n5]) * d18;
                    if (d23 <= d24 && (d23 < d8 || d23 == d8 && n5 < n3)) {
                        d8 = d23;
                        n3 = n5;
                        d9 = d18;
                    }
                    if (d23 <= d24 + Math.max(5.0, d24 * this.s) && (d23 < d6 || d23 == d6 && n5 < n2)) {
                        d6 = d23;
                        n2 = n5;
                        d7 = d18;
                    }
                }
            }
            ++n4;
        }
        if (n3 >= 0) {
            n = n3;
            d4 = d8;
            d5 = d9;
        } else if (n2 >= 0) {
            n = n2;
            d4 = d6;
            d5 = d7;
        }
        if (n < 0 || d4 > d3) {
            return null;
        }
        return this.a(n, d5, d4);
    }

    /*
     * WARNING - void declaration
     */
    private int[] a(double d, double d2, double d3) {
        int[] nArray;
        int n;
        int n2;
        int n3;
        long l;
        TieredCand[] tieredCandArray;
        int n4 = (int)Math.floor(d / 16.0);
        int n5 = (int)Math.floor(d2 / 16.0);
        long l2 = (long)n4 << 32 ^ (long)n5 & 0xFFFFFFFFL;
        for (TieredCand d4 : tieredCandArray = A.get()) {
            if (d4.a != this || d4.b != l2 || d4.c != d3) continue;
            return d4.d;
        }
        long[] lArray = C.get();
        if (lArray[0] != (l = l2 * 31L + Double.doubleToLongBits(d3) + (long)System.identityHashCode(this))) {
            lArray[0] = l;
            int[] nArray2 = D.get()[0];
            int n6 = 0;
            int n7 = this.a(d - d3);
            int n8 = this.a(d + d3);
            int n9 = this.b(d2 - d3);
            int n10 = this.b(d2 + d3);
            for (int i = n7; i <= n8; ++i) {
                for (int j = n9; j <= n10; ++j) {
                    int[] nArray3 = this.z.get(NearestRiverIndex.a(i, j));
                    if (nArray3 == null) continue;
                    if (n6 + nArray3.length + 1 > nArray2.length) {
                        int[] nArray4 = Arrays.copyOf(nArray2, Math.max(nArray2.length * 2, n6 + nArray3.length + 1));
                        nArray2 = nArray4;
                        NearestRiverIndex.D.get()[0] = nArray4;
                    }
                    System.arraycopy(nArray3, 0, nArray2, n6, nArray3.length);
                    n6 += nArray3.length;
                }
            }
            if (n6 + 1 > nArray2.length) {
                int[] nArray5 = Arrays.copyOf(nArray2, n6 + 1);
                nArray2 = nArray5;
                NearestRiverIndex.D.get()[0] = nArray5;
            }
            nArray2[n6] = -1;
            return nArray2;
        }
        double d4 = (double)n4 * 16.0;
        double d5 = (double)n5 * 16.0;
        int n11 = this.a(d4 - d3);
        int n12 = this.a(d4 + 16.0 + d3);
        int n13 = this.b(d5 - d3);
        int n14 = this.b(d5 + 16.0 + d3);
        int n15 = 0;
        for (int i = n11; i <= n12; ++i) {
            for (n3 = n13; n3 <= n14; ++n3) {
                int[] nArray6 = this.z.get(NearestRiverIndex.a(i, n3));
                if (nArray6 == null) continue;
                n15 += nArray6.length;
            }
        }
        int[] nArray7 = new int[n15];
        n3 = 0;
        for (n2 = n11; n2 <= n12; ++n2) {
            for (n = n13; n <= n14; ++n) {
                nArray = this.z.get(NearestRiverIndex.a(n2, n));
                if (nArray == null) continue;
                System.arraycopy(nArray, 0, nArray7, n3, nArray.length);
                n3 += nArray.length;
            }
        }
        Arrays.sort(nArray7);
        n2 = 0;
        for (n = 0; n < nArray7.length; ++n) {
            if (n != 0 && nArray7[n] == nArray7[n - 1]) continue;
            nArray7[n2++] = nArray7[n];
        }
        int[] nArray8 = Arrays.copyOf(nArray7, n2 + 1);
        nArray8[n2] = -1;
        nArray = B.get();
        TieredCand tieredCand = tieredCandArray[nArray[0]];
        nArray[0] = nArray[0] + 1 & 7;
        tieredCand.a = this;
        tieredCand.b = l2;
        tieredCand.c = d3;
        tieredCand.d = nArray8;
        return nArray8;
    }

    public boolean anyCorridorTouchesBox(double d, double d2, double d3, double d4) {
        if (this.f.isEmpty()) {
            return false;
        }
        int n = this.a(d);
        int n2 = this.a(d3);
        int n3 = this.b(d2);
        int n4 = this.b(d4);
        for (int i = n; i <= n2; ++i) {
            for (int j = n3; j <= n4; ++j) {
                int[] nArray = this.z.get(NearestRiverIndex.a(i, j));
                if (nArray == null) continue;
                for (int n5 : nArray) {
                    double d7 = this.l[n5] > d3 ? this.l[n5] - d3 : (this.m[n5] < d ? d - this.m[n5] : 0.0);
                    double d8 = this.n[n5] > d4 ? this.n[n5] - d4 : (this.o[n5] < d2 ? d2 - this.o[n5] : 0.0);
                    if (!(d7 * d7 + d8 * d8 <= this.r[n5])) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private RiverSample a(int n, double d, double d2) {
        double d3;
        RiverSegment riverSegment = this.f.get(n);
        double d4 = riverSegment.x2() - riverSegment.x1();
        double d5 = Math.sqrt(d4 * d4 + (d3 = riverSegment.z2() - riverSegment.z1()) * d3);
        double d6 = d5 > 1.0E-6 ? (riverSegment.y1() - riverSegment.y2()) / d5 : 0.0;
        double d7 = d5 > 1.0E-6 ? (riverSegment.x2() - riverSegment.x1()) / d5 : 0.0;
        double d8 = d5 > 1.0E-6 ? (riverSegment.z2() - riverSegment.z1()) / d5 : 0.0;
        return new RiverSample(d2, riverSegment.gradeAt(d), riverSegment.widthAt(d), d6, d7, d8, this.t[n], this.v[n], this.w[n], this.u[n], n);
    }

    public RiverSample nearestCoveringForeign(double d, double d2, double d3, int n) {
        if (this.f.isEmpty()) {
            return null;
        }
        int n2 = n >= 0 ? this.x[n] : Integer.MIN_VALUE;
        int n3 = this.a(d - d3);
        int n4 = this.a(d + d3);
        int n5 = this.b(d2 - d3);
        int n6 = this.b(d2 + d3);
        double d4 = Double.MAX_VALUE;
        int n7 = -1;
        double d5 = 0.0;
        for (int i = n3; i <= n4; ++i) {
            for (int j = n5; j <= n6; ++j) {
                int[] nArray = this.z.get(NearestRiverIndex.a(i, j));
                if (nArray == null) continue;
                double d6 = d4 < d3 ? d4 : d3;
                for (int n8 : nArray) {
                    double d7;
                    double d8;
                    double d9;
                    double d10 = this.j[n8];
                    double d11;
                    if (this.x[n8] == n2) continue;
                    double d13 = d < this.l[n8] ? this.l[n8] - d : (d > this.m[n8] ? d - this.m[n8] : 0.0);
                    double d14 = d2 < this.n[n8] ? this.n[n8] - d2 : (d2 > this.o[n8] ? d2 - this.o[n8] : 0.0);
                    double d15 = d13 * d13 + d14 * d14;
                    if (d15 > d6 * d6 || d15 > this.r[n8]) continue;
                    double d16 = this.i[n8];
                    double d17 = this.k[n8];
                    d11 = d17 <= 1.0E-9 ? 0.0 : ((d11 = ((d - this.g[n8]) * d16 + (d2 - this.h[n8]) * d10) / d17) < 0.0 ? 0.0 : (d11 > 1.0 ? 1.0 : d11));
                    double d18 = this.g[n8] + d16 * d11;
                    double d19 = d - d18;
                    double d20 = Math.sqrt(d19 * d19 + (d9 = d2 - (d8 = this.h[n8] + (d10 = this.j[n8]) * d11)) * d9);
                    if (d20 > d3 || d20 > d4 || d20 == d4 && n8 >= n7 || !(d20 <= (d7 = this.p[n8] + (this.q[n8] - this.p[n8]) * d11) + Math.max(5.0, d7 * this.s))) continue;
                    d4 = d20;
                    n7 = n8;
                    d5 = d11;
                }
            }
        }
        return n7 < 0 ? null : this.a(n7, d5, d4);
    }

    public double lowerNeighborGrade(double d, double d2, double d3, int n, double d4, double d5) {
        if (this.f.isEmpty()) {
            return Double.NaN;
        }
        int n2 = n >= 0 ? this.x[n] : -1;
        double d6 = d4 - d5;
        int n3 = this.a(d - d3);
        int n4 = this.a(d + d3);
        int n5 = this.b(d2 - d3);
        int n6 = this.b(d2 + d3);
        double d7 = Double.NaN;
        for (int i = n3; i <= n4; ++i) {
            for (int j = n5; j <= n6; ++j) {
                int[] nArray = this.z.get(NearestRiverIndex.a(i, j));
                if (nArray == null) continue;
                for (int n7 : nArray) {
                    double d8;
                    double d9;
                    RiverSegment riverSegment;
                    if (n7 == n || this.x[n7] == n2 || (riverSegment = this.f.get(n7)).distanceAt(d, d2, d9 = riverSegment.clampParam(d, d2)) > d3 || !((d8 = riverSegment.gradeAt(d9)) < d6) || !Double.isNaN(d7) && !(d8 < d7)) continue;
                    d7 = d8;
                }
            }
        }
        return d7;
    }

    public boolean inAnyChannel(double d, double d2, double d3) {
        if (this.f.isEmpty()) {
            return false;
        }
        int n = this.a(d - d3);
        int n2 = this.a(d + d3);
        int n3 = this.b(d2 - d3);
        int n4 = this.b(d2 + d3);
        for (int i = n; i <= n2; ++i) {
            for (int j = n3; j <= n4; ++j) {
                int[] nArray = this.z.get(NearestRiverIndex.a(i, j));
                if (nArray == null) continue;
                for (int n5 : nArray) {
                    double d4;
                    RiverSegment riverSegment = this.f.get(n5);
                    if (!(riverSegment.distanceAt(d, d2, d4 = riverSegment.clampParam(d, d2)) <= riverSegment.widthAt(d4))) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private int a(double d) {
        return (int)Math.floor(d / (double)this.y);
    }

    private int b(double d) {
        return (int)Math.floor(d / (double)this.y);
    }

    private static long a(int n, int n2) {
        return (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
    }

    private static final class TieredCand {
        NearestRiverIndex a;
        long b;
        double c;
        int[] d;

        private TieredCand() {
        }
    }
}

