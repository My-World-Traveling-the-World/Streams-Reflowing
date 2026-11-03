/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class FormEval {
    private static final double g = 1.0181268882175227;
    private static final double h = 3.3554432E7;
    private static final int[][] i = new int[][]{{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}, {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}};
    private final FieldForm j;
    private final IdentityHashMap<FieldForm.Node, Integer> k = new IdentityHashMap();
    private final FieldForm.Node[] l;
    private final int[][] m;
    private final boolean[] n;
    private final CSpline[] o;
    private final int p;
    private static final int[] q = new int[0];
    private IdentityHashMap<FieldForm.Node, Double> r;
    private boolean s;
    private double[] t;
    private int[] u;
    private int v;
    private double[][] w;
    private List<FieldForm.Interp> x;
    private final double[] y;
    private final int[] z;
    private int A = 1;
    private boolean B;
    private boolean C;
    static volatile double a = Double.POSITIVE_INFINITY;
    static volatile double b = 0.0;
    static volatile double c = 0.0;
    boolean d = true;
    boolean e = true;
    private List<FieldForm.Interp> D;
    private int[] E;
    private int[] F;
    boolean f;

    public FormEval(FieldForm fieldForm) {
        this.j = fieldForm;
        ArrayList<FieldForm.Node> arrayList = new ArrayList<FieldForm.Node>();
        this.p = this.a(fieldForm.root, arrayList);
        int n = arrayList.size();
        this.l = arrayList.toArray(new FieldForm.Node[0]);
        this.m = new int[n][];
        this.o = new CSpline[n];
        IdentityHashMap<FieldForm.Spline, CSpline> identityHashMap = new IdentityHashMap<FieldForm.Spline, CSpline>();
        for (int i = 0; i < n; ++i) {
            this.m[i] = this.a(this.l[i]);
            FieldForm.Node node = this.l[i];
            if (!(node instanceof FieldForm.SplineNode)) continue;
            FieldForm.SplineNode splineNode = (FieldForm.SplineNode)node;
            this.o[i] = this.a(splineNode.spline(), identityHashMap);
        }
        this.n = new boolean[n];
        byte[] byArray = new byte[n];
        for (int i = 0; i < n; ++i) {
            this.n[i] = this.a(i, byArray);
        }
        this.y = new double[n];
        this.z = new int[n];
        this.t = new double[n];
        this.u = new int[n];
    }

    private int a(FieldForm.Node node, List<FieldForm.Node> list) {
        Integer n = this.k.get(node);
        if (n != null) {
            return n;
        }
        int n2 = list.size();
        list.add(node);
        this.k.put(node, n2);
        if (node instanceof FieldForm.Bin) {
            FieldForm.Bin bin = (FieldForm.Bin)node;
            this.a(bin.l(), list);
            this.a(bin.r(), list);
        } else if (node instanceof FieldForm.Map1) {
            FieldForm.Map1 map1 = (FieldForm.Map1)node;
            this.a(map1.in(), list);
        } else if (node instanceof FieldForm.Clamp) {
            FieldForm.Clamp clamp = (FieldForm.Clamp)node;
            this.a(clamp.in(), list);
        } else if (node instanceof FieldForm.RangeChoice) {
            FieldForm.RangeChoice rangeChoice = (FieldForm.RangeChoice)node;
            this.a(rangeChoice.input(), list);
            this.a(rangeChoice.whenInRange(), list);
            this.a(rangeChoice.whenOutOfRange(), list);
        } else if (node instanceof FieldForm.ShiftedNoise) {
            FieldForm.ShiftedNoise shiftedNoise = (FieldForm.ShiftedNoise)node;
            this.a(shiftedNoise.shiftX(), list);
            this.a(shiftedNoise.shiftY(), list);
            this.a(shiftedNoise.shiftZ(), list);
        } else if (node instanceof FieldForm.WeirdScaled) {
            FieldForm.WeirdScaled weirdScaled = (FieldForm.WeirdScaled)node;
            this.a(weirdScaled.input(), list);
        } else if (node instanceof FieldForm.IntervalSelect) {
            FieldForm.IntervalSelect intervalSelect = (FieldForm.IntervalSelect)node;
            this.a(intervalSelect.input(), list);
            for (FieldForm.Node node2 : intervalSelect.functions()) {
                this.a(node2, list);
            }
        } else if (node instanceof FieldForm.Cache2D) {
            FieldForm.Cache2D cache2D = (FieldForm.Cache2D)node;
            this.a(cache2D.in(), list);
        } else if (node instanceof FieldForm.FlatCache) {
            FieldForm.FlatCache flatCache = (FieldForm.FlatCache)node;
            this.a(flatCache.in(), list);
        } else if (node instanceof FieldForm.Interp) {
            FieldForm.Interp interp = (FieldForm.Interp)node;
            this.a(interp.in(), list);
        } else if (node instanceof FieldForm.SplineNode) {
            FieldForm.SplineNode splineNode = (FieldForm.SplineNode)node;
            this.a(splineNode.spline(), list);
        }
        return n2;
    }

    private void a(FieldForm.Spline spline, List<FieldForm.Node> list) {
        this.a(spline.coordinate(), list);
        for (FieldForm.SplineValue splineValue : spline.values()) {
            if (!(splineValue instanceof FieldForm.SplineNested)) continue;
            FieldForm.SplineNested splineNested = (FieldForm.SplineNested)splineValue;
            this.a(splineNested.spline(), list);
        }
    }

    private int[] a(FieldForm.Node node) {
        if (node instanceof FieldForm.Bin) {
            FieldForm.Bin bin = (FieldForm.Bin)node;
            return new int[]{this.b(bin.l()), this.b(bin.r())};
        }
        if (node instanceof FieldForm.Map1) {
            FieldForm.Map1 map1 = (FieldForm.Map1)node;
            return new int[]{this.b(map1.in())};
        }
        if (node instanceof FieldForm.Clamp) {
            FieldForm.Clamp clamp = (FieldForm.Clamp)node;
            return new int[]{this.b(clamp.in())};
        }
        if (node instanceof FieldForm.RangeChoice) {
            FieldForm.RangeChoice rangeChoice = (FieldForm.RangeChoice)node;
            return new int[]{this.b(rangeChoice.input()), this.b(rangeChoice.whenInRange()), this.b(rangeChoice.whenOutOfRange())};
        }
        if (node instanceof FieldForm.ShiftedNoise) {
            FieldForm.ShiftedNoise shiftedNoise = (FieldForm.ShiftedNoise)node;
            return new int[]{this.b(shiftedNoise.shiftX()), this.b(shiftedNoise.shiftY()), this.b(shiftedNoise.shiftZ())};
        }
        if (node instanceof FieldForm.WeirdScaled) {
            FieldForm.WeirdScaled weirdScaled = (FieldForm.WeirdScaled)node;
            return new int[]{this.b(weirdScaled.input())};
        }
        if (node instanceof FieldForm.IntervalSelect) {
            FieldForm.IntervalSelect intervalSelect = (FieldForm.IntervalSelect)node;
            FieldForm.Node[] nodeArray = intervalSelect.functions();
            int[] nArray = new int[nodeArray.length + 1];
            nArray[0] = this.b(intervalSelect.input());
            for (int i = 0; i < nodeArray.length; ++i) {
                nArray[i + 1] = this.b(nodeArray[i]);
            }
            return nArray;
        }
        if (node instanceof FieldForm.Cache2D) {
            FieldForm.Cache2D cache2D = (FieldForm.Cache2D)node;
            return new int[]{this.b(cache2D.in())};
        }
        if (node instanceof FieldForm.FlatCache) {
            FieldForm.FlatCache flatCache = (FieldForm.FlatCache)node;
            return new int[]{this.b(flatCache.in())};
        }
        if (node instanceof FieldForm.Interp) {
            FieldForm.Interp interp = (FieldForm.Interp)node;
            return new int[]{this.b(interp.in())};
        }
        return q;
    }

    private int b(FieldForm.Node node) {
        Integer n = this.k.get(node);
        if (n == null) {
            throw new IllegalStateException("node reached by evaluation but not by index assignment: " + String.valueOf(node));
        }
        return n;
    }

    private CSpline a(FieldForm.Spline spline, IdentityHashMap<FieldForm.Spline, CSpline> identityHashMap) {
        CSpline cSpline = identityHashMap.get(spline);
        if (cSpline != null) {
            return cSpline;
        }
        FieldForm.SplineValue[] splineValueArray = spline.values();
        float[] fArray = new float[splineValueArray.length];
        CSpline[] cSplineArray = new CSpline[splineValueArray.length];
        CSpline cSpline2 = new CSpline(this.b(spline.coordinate()), spline.locations(), spline.derivatives(), fArray, cSplineArray);
        identityHashMap.put(spline, cSpline2);
        for (int i = 0; i < splineValueArray.length; ++i) {
            FieldForm.SplineValue splineValue = splineValueArray[i];
            if (splineValue instanceof FieldForm.SplineConst) {
                FieldForm.SplineConst splineConst = (FieldForm.SplineConst)splineValue;
                fArray[i] = splineConst.v();
                continue;
            }
            cSplineArray[i] = this.a(((FieldForm.SplineNested)splineValueArray[i]).spline(), identityHashMap);
        }
        return cSpline2;
    }

    private static double a(double d, double d2, double d3) {
        return d2 + d * (d3 - d2);
    }

    public double compute(int n, int n2, int n3) {
        return this.a(this.p, n, n2, n3);
    }

    public int interpMarkerCount() {
        return this.a().size();
    }

    private List<FieldForm.Interp> a() {
        if (this.x == null) {
            ArrayList<FieldForm.Interp> arrayList = new ArrayList<FieldForm.Interp>();
            FormEval.a(this.j.root, arrayList, Collections.newSetFromMap(new IdentityHashMap()));
            this.x = arrayList;
        }
        return this.x;
    }

    private static void a(FieldForm.Node node, List<FieldForm.Interp> list, Set<FieldForm.Node> set) {
        if (node == null || !set.add(node)) {
            return;
        }
        if (node instanceof FieldForm.Interp) {
            FieldForm.Interp interp = (FieldForm.Interp)node;
            list.add(interp);
            FormEval.a(interp.in(), list, set);
        } else if (node instanceof FieldForm.Bin) {
            FieldForm.Bin bin = (FieldForm.Bin)node;
            FormEval.a(bin.l(), list, set);
            FormEval.a(bin.r(), list, set);
        } else if (node instanceof FieldForm.IntervalSelect) {
            FieldForm.IntervalSelect intervalSelect = (FieldForm.IntervalSelect)node;
            FormEval.a(intervalSelect.input(), list, set);
            for (FieldForm.Node node2 : intervalSelect.functions()) {
                FormEval.a(node2, list, set);
            }
        } else if (node instanceof FieldForm.Cache2D) {
            FieldForm.Cache2D cache2D = (FieldForm.Cache2D)node;
            FormEval.a(cache2D.in(), list, set);
        } else if (node instanceof FieldForm.FlatCache) {
            FieldForm.FlatCache flatCache = (FieldForm.FlatCache)node;
            FormEval.a(flatCache.in(), list, set);
        } else if (node instanceof FieldForm.Map1) {
            FieldForm.Map1 map1 = (FieldForm.Map1)node;
            FormEval.a(map1.in(), list, set);
        } else if (node instanceof FieldForm.Clamp) {
            FieldForm.Clamp clamp = (FieldForm.Clamp)node;
            FormEval.a(clamp.in(), list, set);
        } else if (node instanceof FieldForm.RangeChoice) {
            FieldForm.RangeChoice rangeChoice = (FieldForm.RangeChoice)node;
            FormEval.a(rangeChoice.input(), list, set);
            FormEval.a(rangeChoice.whenInRange(), list, set);
            FormEval.a(rangeChoice.whenOutOfRange(), list, set);
        } else if (node instanceof FieldForm.ShiftedNoise) {
            FieldForm.ShiftedNoise shiftedNoise = (FieldForm.ShiftedNoise)node;
            FormEval.a(shiftedNoise.shiftX(), list, set);
            FormEval.a(shiftedNoise.shiftY(), list, set);
            FormEval.a(shiftedNoise.shiftZ(), list, set);
        } else if (node instanceof FieldForm.WeirdScaled) {
            FieldForm.WeirdScaled weirdScaled = (FieldForm.WeirdScaled)node;
            FormEval.a(weirdScaled.input(), list, set);
        } else if (node instanceof FieldForm.SplineNode) {
            FieldForm.SplineNode splineNode = (FieldForm.SplineNode)node;
            FormEval.a(splineNode.spline(), list, set);
        }
    }

    private static void a(FieldForm.Spline spline, List<FieldForm.Interp> list, Set<FieldForm.Node> set) {
        FormEval.a(spline.coordinate(), list, set);
        for (FieldForm.SplineValue splineValue : spline.values()) {
            if (!(splineValue instanceof FieldForm.SplineNested)) continue;
            FieldForm.SplineNested splineNested = (FieldForm.SplineNested)splineValue;
            FormEval.a(splineNested.spline(), list, set);
        }
    }

    public IdentityHashMap<FieldForm.Node, Double> traceCompute(int n, int n2, int n3) {
        this.r = new IdentityHashMap();
        this.a(this.p, n, n2, n3);
        IdentityHashMap<FieldForm.Node, Double> identityHashMap = this.r;
        this.r = null;
        return identityHashMap;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public double surfaceHeight(int n, int n2, int n3, int n4, int n5, int n6, double d, double[] dArray) {
        this.d();
        this.B = true;
        this.C = true;
        try {
            List<Object> list;
            int n7 = Math.floorDiv(n3, n5);
            int n8 = Math.floorDiv(n4 - n3, n5);
            List<Object> list2 = list = this.e ? this.a() : List.of();
            if (list.isEmpty()) {
                for (int i = n8 - 1; i >= 0; --i) {
                    for (int j = n5 - 1; j >= 0; --j) {
                        int n9 = (n7 + i) * n5 + j;
                        if (!(this.a(this.p, n, n9, n2) > d)) continue;
                        double d2 = n9 + 1;
                        return d2;
                    }
                }
                double d3 = n3;
                return d3;
            }
            int n10 = list.size();
            int n11 = Math.floorDiv(n, n6);
            int n12 = Math.floorDiv(n2, n6);
            int n13 = n11 * n6;
            int n14 = n12 * n6;
            double d4 = (double)(n - n13) / (double)n6;
            double d5 = (double)(n2 - n14) / (double)n6;
            int[] nArray = new int[]{n13, n13 + n6, n13, n13 + n6};
            int[] nArray2 = new int[]{n14, n14, n14 + n6, n14 + n6};
            if (this.w == null || this.w.length != n10 * 4 || this.w[0].length != n8 + 1) {
                this.w = new double[n10 * 4][n8 + 1];
            }
            this.a(list);
            this.b();
            try {
                this.a(list, this.w, nArray, nArray2, n3, n5, 0, n8);
                this.d();
                double d6 = this.a(list, this.w, n, n2, n3, n5, n8, d4, d5, d, 0.0);
                this.c();
                return d6;
            }
            catch (Throwable throwable) {
                this.c();
                throw throwable;
            }
        }
        finally {
            this.B = false;
            this.C = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public double surfaceHeightFromCorners(List<FieldForm.Interp> list, double[][] dArray, int n, int n2, int n3, int n4, int n5, int n6, double d, double d2) {
        this.d();
        this.B = true;
        this.C = true;
        this.a(list);
        this.b();
        try {
            int n7 = Math.floorDiv(n4 - n3, n5);
            int n8 = Math.floorDiv(n, n6) * n6;
            int n9 = Math.floorDiv(n2, n6) * n6;
            double d3 = (double)(n - n8) / (double)n6;
            double d4 = (double)(n2 - n9) / (double)n6;
            double d5 = this.a(list, dArray, n, n2, n3, n5, n7, d3, d4, d, d2);
            return d5;
        }
        finally {
            this.c();
            this.B = false;
            this.C = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public double[][] markerCorners(List<FieldForm.Interp> list, int n, int n2, int n3, int n4, int n5, int n6) {
        this.B = true;
        this.C = true;
        this.a(list);
        this.b();
        try {
            int n7 = Math.floorDiv(n4 - n3, n5);
            int n8 = Math.floorDiv(n, n6) * n6;
            int n9 = Math.floorDiv(n2, n6) * n6;
            int[] nArray = new int[]{n8, n8 + n6, n8, n8 + n6};
            int[] nArray2 = new int[]{n9, n9, n9 + n6, n9 + n6};
            double[][] dArray = new double[list.size() * 4][n7 + 1];
            this.a(list, dArray, nArray, nArray2, n3, n5, 0, n7);
            double[][] dArray2 = dArray;
            return dArray2;
        }
        finally {
            this.c();
            this.B = false;
            this.C = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void refineCorners(List<FieldForm.Interp> list, double[][] dArray, int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        this.B = true;
        this.C = true;
        this.a(list);
        this.b();
        try {
            int n8 = Math.floorDiv(n, n5) * n5;
            int n9 = Math.floorDiv(n2, n5) * n5;
            int[] nArray = new int[]{n8, n8 + n5, n8, n8 + n5};
            int[] nArray2 = new int[]{n9, n9, n9 + n5, n9 + n5};
            this.a(list, dArray, nArray, nArray2, n3, n4, n6, n7);
        }
        finally {
            this.c();
            this.B = false;
            this.C = false;
        }
    }

    private void a(List<FieldForm.Interp> list, double[][] dArray, int[] nArray, int[] nArray2, int n, int n2, int n3, int n4) {
        int n5 = Math.floorDiv(n, n2);
        int n6 = list.size();
        int[] nArray3 = this.F;
        for (int i = 0; i < 4; ++i) {
            this.d();
            for (int j = n3; j <= n4; ++j) {
                int n7 = (n5 + j) * n2;
                for (int k = 0; k < n6; ++k) {
                    dArray[k * 4 + i][j] = this.a(nArray3[k], nArray[i], n7, nArray2[i]);
                }
            }
        }
    }

    private void a(List<FieldForm.Interp> list) {
        if (list == this.D) {
            return;
        }
        int n = list.size();
        int[] nArray = new int[n];
        int[] nArray2 = new int[n];
        for (int i = 0; i < n; ++i) {
            FieldForm.Interp interp = list.get(i);
            Integer n2 = this.k.get(interp);
            Integer n3 = this.k.get(interp.in());
            if (n2 == null || n3 == null) {
                throw new IllegalArgumentException("marker does not belong to this evaluator's tree");
            }
            nArray[i] = n2;
            nArray2[i] = n3;
        }
        this.E = nArray;
        this.F = nArray2;
        this.D = list;
    }

    private void b() {
        if (this.v == Integer.MAX_VALUE) {
            Arrays.fill(this.u, 0);
            this.v = 1;
        } else {
            ++this.v;
        }
        this.s = true;
    }

    private void c() {
        this.s = false;
    }

    private double a(List<FieldForm.Interp> list, double[][] dArray, int n, int n2, int n3, int n4, int n5, double d, double d2, double d3, double d4) {
        int n6 = Math.floorDiv(n3, n4);
        int n7 = list.size();
        int[] nArray = this.E;
        for (int i = n5 - 1; i >= 0; --i) {
            for (int j = n4 - 1; j >= 0; --j) {
                int n8;
                double d5 = (double)j / (double)n4;
                for (n8 = 0; n8 < n7; ++n8) {
                    double d6 = FormEval.a(d5, dArray[n8 * 4][i], dArray[n8 * 4][i + 1]);
                    double d7 = FormEval.a(d5, dArray[n8 * 4 + 1][i], dArray[n8 * 4 + 1][i + 1]);
                    double d8 = FormEval.a(d5, dArray[n8 * 4 + 2][i], dArray[n8 * 4 + 2][i + 1]);
                    double d9 = FormEval.a(d5, dArray[n8 * 4 + 3][i], dArray[n8 * 4 + 3][i + 1]);
                    int n9 = nArray[n8];
                    this.t[n9] = FormEval.a(d2, FormEval.a(d, d6, d7), FormEval.a(d, d8, d9)) + d4;
                    this.u[n9] = this.v;
                }
                n8 = (n6 + i) * n4 + j;
                if (!(this.a(this.p, n, n8, n2) > d3)) continue;
                return n8 + 1;
            }
        }
        return n3;
    }

    private boolean a(int n, byte[] byArray) {
        FieldForm.ShiftedNoise shiftedNoise;
        FieldForm.ShiftNoise shiftNoise;
        FieldForm.NoiseNode noiseNode;
        byte by = byArray[n];
        if (by != 0) {
            return by == 1;
        }
        FieldForm.Node node = this.l[n];
        int[] nArray = this.m[n];
        boolean bl = node instanceof FieldForm.Extern ? false : (node instanceof FieldForm.Cache2D ? this.a(nArray[0], byArray) : (node instanceof FieldForm.IntervalSelect ? false : (node instanceof FieldForm.FlatCache ? this.a(nArray[0], byArray) : (node instanceof FieldForm.NoiseNode ? (noiseNode = (FieldForm.NoiseNode)node).yScale() == 0.0 : (node instanceof FieldForm.ShiftNoise ? (shiftNoise = (FieldForm.ShiftNoise)node).kind() != FieldForm.ShiftKind.SHIFT : (node instanceof FieldForm.ShiftedNoise ? (shiftedNoise = (FieldForm.ShiftedNoise)node).yScale() == 0.0 && this.a(nArray[0], byArray) && this.a(nArray[1], byArray) && this.a(nArray[2], byArray) : (node instanceof FieldForm.Bin ? this.a(nArray[0], byArray) && this.a(nArray[1], byArray) : (node instanceof FieldForm.Map1 ? this.a(nArray[0], byArray) : (node instanceof FieldForm.Clamp ? this.a(nArray[0], byArray) : (node instanceof FieldForm.RangeChoice ? this.a(nArray[0], byArray) && this.a(nArray[1], byArray) && this.a(nArray[2], byArray) : (node instanceof FieldForm.SplineNode ? this.a(this.o[n], byArray) : (node instanceof FieldForm.Const ? true : node instanceof FieldForm.ZCoord))))))))))));
        byArray[n] = (byte)(bl ? 1 : 2);
        return bl;
    }

    private boolean a(CSpline cSpline, byte[] byArray) {
        if (!this.a(cSpline.a, byArray)) {
            return false;
        }
        for (CSpline cSpline2 : cSpline.e) {
            if (cSpline2 == null || this.a(cSpline2, byArray)) continue;
            return false;
        }
        return true;
    }

    private void d() {
        if (this.A == Integer.MAX_VALUE) {
            Arrays.fill(this.z, 0);
            this.A = 1;
        } else {
            ++this.A;
        }
    }

    private double a(int n, int n2, int n3, int n4) {
        double d;
        if (this.B && this.n[n]) {
            if (this.z[n] == this.A) {
                double d2 = this.y[n];
                return this.f ? (double)((float)d2) : d2;
            }
            this.y[n] = d = this.b(n, n2, n3, n4);
            this.z[n] = this.A;
        } else {
            d = this.b(n, n2, n3, n4);
        }
        if (this.r != null) {
            this.r.put(this.l[n], d);
        }
        return this.f ? (double)((float)d) : d;
    }

    private double b(int n, int n2, int n3, int n4) {
        FieldForm.Node node = this.l[n];
        int[] nArray = this.m[n];
        if (node instanceof FieldForm.Extern) {
            FieldForm.Extern extern = (FieldForm.Extern)node;
            return extern.fn().at(n2, n3, n4);
        }
        if (node instanceof FieldForm.Const) {
            FieldForm.Const const_ = (FieldForm.Const)node;
            return const_.v();
        }
        if (node instanceof FieldForm.YGradient) {
            FieldForm.YGradient yGradient = (FieldForm.YGradient)node;
            double d = FormEval.b(((double)n3 - (double)yGradient.fromY()) / ((double)yGradient.toY() - (double)yGradient.fromY()), 0.0, 1.0);
            return yGradient.fromV() + d * (yGradient.toV() - yGradient.fromV());
        }
        if (node instanceof FieldForm.Bin) {
            FieldForm.Bin bin = (FieldForm.Bin)node;
            double d = this.a(nArray[0], n2, n3, n4);
            double d2 = this.a(nArray[1], n2, n3, n4);
            return switch (bin.op()) {
                default -> throw new MatchException(null, null);
                case FieldForm.BinOp.ADD -> d + d2;
                case FieldForm.BinOp.MUL -> d * d2;
                case FieldForm.BinOp.MIN -> Math.min(d, d2);
                case FieldForm.BinOp.MAX -> Math.max(d, d2);
                case FieldForm.BinOp.DIV -> d2 == 0.0 ? 0.0 : d / d2;
            };
        }
        if (node instanceof FieldForm.Map1) {
            FieldForm.Map1 map1 = (FieldForm.Map1)node;
            double d = this.a(nArray[0], n2, n3, n4);
            return switch (map1.op()) {
                default -> throw new MatchException(null, null);
                case FieldForm.MapOp.ABS -> Math.abs(d);
                case FieldForm.MapOp.SQUARE -> d * d;
                case FieldForm.MapOp.CUBE -> d * d * d;
                case FieldForm.MapOp.HALF_NEGATIVE -> {
                    if (d > 0.0) {
                        yield d;
                    }
                    yield d * 0.5;
                }
                case FieldForm.MapOp.QUARTER_NEGATIVE -> {
                    if (d > 0.0) {
                        yield d;
                    }
                    yield d * 0.25;
                }
                case FieldForm.MapOp.RECIPROCAL -> {
                    double var17_34;
                    float var16_33;
                    double var12_30;
                    double var14_32;
                    double var10_28 = Math.abs(d);
                    if (var10_28 < a) {
                        a = var10_28;
                    }
                    if ((var14_32 = Math.abs(var12_30 = 1.0 / d)) > b) {
                        b = var14_32;
                    }
                    double v2 = (var16_33 = (float)d) == 0.0f ? Double.POSITIVE_INFINITY : (var17_34 = Math.abs((1.0 / (double)var16_33 - var12_30) / (var12_30 == 0.0 ? 1.0 : var12_30)));
                    if (var17_34 > c) {
                        c = var17_34;
                    }
                    yield var12_30;
                }
                case FieldForm.MapOp.SQUEEZE -> {
                    double var10_29 = FormEval.b(d, -1.0, 1.0);
                    yield var10_29 / 2.0 - var10_29 * var10_29 * var10_29 / 24.0;
                }
                case FieldForm.MapOp.SIGNUM -> Math.signum(d);
                case FieldForm.MapOp.SIN -> Math.sin(d);
            };
        }
        if (node instanceof FieldForm.Clamp) {
            FieldForm.Clamp clamp = (FieldForm.Clamp)node;
            return FormEval.b(this.a(nArray[0], n2, n3, n4), clamp.min(), clamp.max());
        }
        if (node instanceof FieldForm.RangeChoice) {
            FieldForm.RangeChoice rangeChoice = (FieldForm.RangeChoice)node;
            double d = this.a(nArray[0], n2, n3, n4);
            return d >= rangeChoice.minInclusive() && d < rangeChoice.maxExclusive() ? this.a(nArray[1], n2, n3, n4) : this.a(nArray[2], n2, n3, n4);
        }
        boolean bl = this.f;
        if (node instanceof FieldForm.NoiseNode) {
            FieldForm.NoiseNode noiseNode = (FieldForm.NoiseNode)node;
            return FormEval.a(this.j.noises.get(noiseNode.noiseIndex()), FormEval.b((double)n2 * noiseNode.xzScale(), bl), FormEval.b((double)n3 * noiseNode.yScale(), bl), FormEval.b((double)n4 * noiseNode.xzScale(), bl), bl);
        }
        if (node instanceof FieldForm.ShiftNoise) {
            FieldForm.ShiftNoise shiftNoise = (FieldForm.ShiftNoise)node;
            FieldForm.NoiseState noiseState = this.j.noises.get(shiftNoise.noiseIndex());
            return switch (shiftNoise.kind()) {
                default -> throw new MatchException(null, null);
                case FieldForm.ShiftKind.SHIFT_A -> FormEval.b(FormEval.a(noiseState, FormEval.b((double)n2 * 0.25, bl), 0.0, FormEval.b((double)n4 * 0.25, bl), bl) * 4.0, bl);
                case FieldForm.ShiftKind.SHIFT_B -> FormEval.b(FormEval.a(noiseState, FormEval.b((double)n4 * 0.25, bl), FormEval.b((double)n2 * 0.25, bl), 0.0, bl) * 4.0, bl);
                case FieldForm.ShiftKind.SHIFT -> FormEval.b(FormEval.a(noiseState, FormEval.b((double)n2 * 0.25, bl), FormEval.b((double)n3 * 0.25, bl), FormEval.b((double)n4 * 0.25, bl), bl) * 4.0, bl);
            };
        }
        if (node instanceof FieldForm.ShiftedNoise) {
            FieldForm.ShiftedNoise shiftedNoise = (FieldForm.ShiftedNoise)node;
            double d = FormEval.b(FormEval.b((double)n2 * shiftedNoise.xzScale(), bl) + this.a(nArray[0], n2, n3, n4), bl);
            double d3 = FormEval.b(FormEval.b((double)n3 * shiftedNoise.yScale(), bl) + this.a(nArray[1], n2, n3, n4), bl);
            double d4 = FormEval.b(FormEval.b((double)n4 * shiftedNoise.xzScale(), bl) + this.a(nArray[2], n2, n3, n4), bl);
            return FormEval.a(this.j.noises.get(shiftedNoise.noiseIndex()), d, d3, d4, bl);
        }
        if (node instanceof FieldForm.WeirdScaled) {
            FieldForm.WeirdScaled weirdScaled = (FieldForm.WeirdScaled)node;
            double d = this.a(nArray[0], n2, n3, n4);
            double d5 = weirdScaled.rarity() == FieldForm.RarityKind.TYPE1 ? FormEval.b(d) : FormEval.c(d);
            return FormEval.b(d5 * Math.abs(FormEval.a(this.j.noises.get(weirdScaled.noiseIndex()), FormEval.b((double)n2 / d5, bl), FormEval.b((double)n3 / d5, bl), FormEval.b((double)n4 / d5, bl), bl)), bl);
        }
        if (node instanceof FieldForm.IntervalSelect) {
            FieldForm.IntervalSelect intervalSelect = (FieldForm.IntervalSelect)node;
            double d = this.a(nArray[0], n2, n3, n4);
            double[] dArray = intervalSelect.thresholds();
            for (int i = 0; i < dArray.length; ++i) {
                if (!(d < dArray[i])) continue;
                return this.a(nArray[1 + i], n2, n3, n4);
            }
            return this.a(nArray[nArray.length - 1], n2, n3, n4);
        }
        if (node instanceof FieldForm.Cache2D) {
            FieldForm.Cache2D cache2D = (FieldForm.Cache2D)node;
            return this.a(nArray[0], n2, n3, n4);
        }
        if (node instanceof FieldForm.FlatCache) {
            FieldForm.FlatCache flatCache = (FieldForm.FlatCache)node;
            return this.C && this.d ? this.a(nArray[0], n2 & 0xFFFFFFFC, 0, n4 & 0xFFFFFFFC) : this.a(nArray[0], n2, n3, n4);
        }
        if (node instanceof FieldForm.Interp) {
            if (this.s && this.u[n] == this.v) {
                return this.t[n];
            }
            return this.a(nArray[0], n2, n3, n4);
        }
        if (node instanceof FieldForm.Blended) {
            FieldForm.Blended blended = (FieldForm.Blended)node;
            return this.a(blended, n2, n3, n4);
        }
        if (node instanceof FieldForm.ZCoord) {
            return FormEval.b(n4, -3.0E7, 3.0E7);
        }
        return this.a(this.o[n], n2, n3, n4);
    }

    private double a(FieldForm.Blended blended, int n, int n2, int n3) {
        double d;
        double d2;
        FieldForm.PerlinState perlinState = this.j.noises.get((int)blended.main()).first;
        FieldForm.PerlinState perlinState2 = this.j.noises.get((int)blended.minLimit()).first;
        FieldForm.PerlinState perlinState3 = this.j.noises.get((int)blended.maxLimit()).first;
        boolean bl = this.f;
        double d3 = FormEval.b((double)n * blended.xzMultiplier(), bl);
        double d4 = FormEval.b((double)n2 * blended.yMultiplier(), bl);
        double d5 = FormEval.b((double)n3 * blended.xzMultiplier(), bl);
        double d6 = FormEval.b(d3 / blended.xzFactor(), bl);
        double d7 = FormEval.b(d4 / blended.yFactor(), bl);
        double d8 = FormEval.b(d5 / blended.xzFactor(), bl);
        double d9 = FormEval.b(blended.yMultiplier() * blended.smearScaleMultiplier(), bl);
        double d10 = FormEval.b(d9 / blended.yFactor(), bl);
        double d11 = 0.0;
        double d12 = 1.0;
        for (int i = 0; i < perlinState.perm.length; ++i) {
            int n4 = perlinState.perm.length - 1 - i;
            if (perlinState.perm[n4] != null) {
                d11 = FormEval.b(d11 + FormEval.b(FormEval.a(perlinState, n4, FormEval.a(FormEval.b(d6 * d12, bl), bl), FormEval.a(FormEval.b(d7 * d12, bl), bl), FormEval.a(FormEval.b(d8 * d12, bl), bl), FormEval.b(d10 * d12, bl), FormEval.b(d7 * d12, bl), bl) / d12, bl), bl);
            }
            d12 /= 2.0;
        }
        double d13 = FormEval.b(FormEval.b(FormEval.b(d11 / 10.0, bl) + 1.0, bl) / 2.0, bl);
        boolean bl2 = d13 >= 1.0;
        boolean bl3 = d13 <= 0.0;
        double d14 = 0.0;
        double d15 = 0.0;
        d12 = 1.0;
        for (int i = 0; i < perlinState2.perm.length; ++i) {
            int n5 = perlinState2.perm.length - 1 - i;
            d2 = FormEval.a(FormEval.b(d3 * d12, bl), bl);
            d = FormEval.a(FormEval.b(d4 * d12, bl), bl);
            double d16 = FormEval.a(FormEval.b(d5 * d12, bl), bl);
            double d17 = FormEval.b(d9 * d12, bl);
            if (!bl2 && perlinState2.perm[n5] != null) {
                d14 = FormEval.b(d14 + FormEval.b(FormEval.a(perlinState2, n5, d2, d, d16, d17, FormEval.b(d4 * d12, bl), bl) / d12, bl), bl);
            }
            if (!bl3 && perlinState3.perm[n5] != null) {
                d15 = FormEval.b(d15 + FormEval.b(FormEval.a(perlinState3, n5, d2, d, d16, d17, FormEval.b(d4 * d12, bl), bl) / d12, bl), bl);
            }
            d12 /= 2.0;
        }
        double d18 = FormEval.b(d13, 0.0, 1.0);
        d2 = FormEval.b(d14 / 512.0, bl);
        d = FormEval.b(d15 / 512.0, bl);
        return FormEval.b(FormEval.b(d2 + FormEval.b(d18 * FormEval.b(d - d2, bl), bl), bl) / 128.0, bl);
    }

    private static double a(FieldForm.PerlinState perlinState, int n, double d, double d2, double d3, double d4, double d5, boolean bl) {
        double d6 = FormEval.b(d + perlinState.xo[n], bl);
        double d7 = FormEval.b(d2 + perlinState.yo[n], bl);
        double d8 = FormEval.b(d3 + perlinState.zo[n], bl);
        int n2 = FormEval.e(d6);
        int n3 = FormEval.e(d7);
        int n4 = FormEval.e(d8);
        double d9 = FormEval.b(d6 - (double)n2, bl);
        double d10 = FormEval.b(d7 - (double)n3, bl);
        double d11 = FormEval.b(d8 - (double)n4, bl);
        double d12 = 0.0;
        if (d4 != 0.0) {
            double d13 = d5 >= 0.0 && d5 < d10 ? d5 : d10;
            d12 = FormEval.b((bl ? Math.floor(FormEval.b(FormEval.b(d13 / d4, bl) + 1.0E-7, bl)) : (double)FormEval.d(d13 / d4 + 1.0E-7)) * d4, bl);
        }
        return FormEval.a(perlinState.perm[n], n2, n3, n4, d9, FormEval.b(d10 - d12, bl), d11, d10, bl);
    }

    private static double a(int[] nArray, int n, int n2, int n3, double d, double d2, double d3, double d4, boolean bl) {
        int n4 = nArray[n & 0xFF];
        int n5 = nArray[n + 1 & 0xFF];
        int n6 = nArray[n4 + n2 & 0xFF];
        int n7 = nArray[n4 + n2 + 1 & 0xFF];
        int n8 = nArray[n5 + n2 & 0xFF];
        int n9 = nArray[n5 + n2 + 1 & 0xFF];
        double d5 = FormEval.a(nArray[n6 + n3 & 0xFF], d, d2, d3, bl);
        double d6 = FormEval.a(nArray[n8 + n3 & 0xFF], FormEval.b(d - 1.0, bl), d2, d3, bl);
        double d7 = FormEval.a(nArray[n7 + n3 & 0xFF], d, FormEval.b(d2 - 1.0, bl), d3, bl);
        double d8 = FormEval.a(nArray[n9 + n3 & 0xFF], FormEval.b(d - 1.0, bl), FormEval.b(d2 - 1.0, bl), d3, bl);
        double d9 = FormEval.a(nArray[n6 + n3 + 1 & 0xFF], d, d2, FormEval.b(d3 - 1.0, bl), bl);
        double d10 = FormEval.a(nArray[n8 + n3 + 1 & 0xFF], FormEval.b(d - 1.0, bl), d2, FormEval.b(d3 - 1.0, bl), bl);
        double d11 = FormEval.a(nArray[n7 + n3 + 1 & 0xFF], d, FormEval.b(d2 - 1.0, bl), FormEval.b(d3 - 1.0, bl), bl);
        double d12 = FormEval.a(nArray[n9 + n3 + 1 & 0xFF], FormEval.b(d - 1.0, bl), FormEval.b(d2 - 1.0, bl), FormEval.b(d3 - 1.0, bl), bl);
        double d13 = FormEval.c(d, bl);
        double d14 = FormEval.c(d4, bl);
        double d15 = FormEval.c(d3, bl);
        return FormEval.a(d13, d14, d15, d5, d6, d7, d8, d9, d10, d11, d12, bl);
    }

    private static double b(double d) {
        if (d < -0.5) {
            return 0.75;
        }
        if (d < 0.0) {
            return 1.0;
        }
        if (d < 0.5) {
            return 1.5;
        }
        return 2.0;
    }

    private static double c(double d) {
        if (d < -0.75) {
            return 0.5;
        }
        if (d < -0.5) {
            return 0.75;
        }
        if (d < 0.5) {
            return 1.0;
        }
        if (d < 0.75) {
            return 2.0;
        }
        return 3.0;
    }

    private double a(CSpline cSpline, int n, int n2, int n3) {
        float f = (float)this.a(cSpline.a, n, n2, n3);
        float[] fArray = cSpline.b;
        int n4 = fArray.length;
        int n5 = FormEval.a(fArray, f);
        if (n5 < 0) {
            return this.a(cSpline, 0, n, n2, n3) + cSpline.c[0] * (f - fArray[0]);
        }
        if (n5 == n4 - 1) {
            return this.a(cSpline, n4 - 1, n, n2, n3) + cSpline.c[n4 - 1] * (f - fArray[n4 - 1]);
        }
        float f2 = fArray[n5];
        float f3 = fArray[n5 + 1];
        float f4 = (f - f2) / (f3 - f2);
        float f5 = this.a(cSpline, n5, n, n2, n3);
        float f6 = this.a(cSpline, n5 + 1, n, n2, n3);
        float f7 = cSpline.c[n5];
        float f8 = cSpline.c[n5 + 1];
        float f9 = f3 - f2;
        float f10 = f7 * f9 - (f6 - f5);
        float f11 = -f8 * f9 + (f6 - f5);
        return FormEval.a(f4, f5, f6) + f4 * (1.0f - f4) * FormEval.a(f4, f10, f11);
    }

    static int a(float[] fArray, float f) {
        int n = 0;
        int n2 = fArray.length;
        while (n2 > 0) {
            int n3 = n2 / 2;
            int n4 = n + n3;
            if (f < fArray[n4]) {
                n2 = n3;
                continue;
            }
            n = n4 + 1;
            n2 -= n3 + 1;
        }
        return n - 1;
    }

    private float a(CSpline cSpline, int n, int n2, int n3, int n4) {
        CSpline cSpline2 = cSpline.e[n];
        return cSpline2 == null ? cSpline.d[n] : (float)this.a(cSpline2, n2, n3, n4);
    }

    private static float a(float f, float f2, float f3) {
        return f2 + f * (f3 - f2);
    }

    static double a(FieldForm.NoiseState noiseState, double d, double d2, double d3) {
        return FormEval.a(noiseState, d, d2, d3, false);
    }

    static double a(FieldForm.NoiseState noiseState, double d, double d2, double d3, boolean bl) {
        double d4 = FormEval.a(noiseState.first, FormEval.b(d, bl), FormEval.b(d2, bl), FormEval.b(d3, bl), bl);
        if (!noiseState.normal) {
            return d4;
        }
        double d5 = FormEval.a(noiseState.second, FormEval.b(d * 1.0181268882175227, bl), FormEval.b(d2 * 1.0181268882175227, bl), FormEval.b(d3 * 1.0181268882175227, bl), bl);
        return FormEval.b(FormEval.b(d4 + d5, bl) * noiseState.valueFactor, bl);
    }

    private static double b(double d, boolean bl) {
        return bl ? (double)((float)d) : d;
    }

    private static double a(FieldForm.PerlinState perlinState, double d, double d2, double d3, boolean bl) {
        double d4 = perlinState.lowestFreqInputFactor;
        double d5 = perlinState.lowestFreqValueFactor;
        double d6 = 0.0;
        for (int i = 0; i < perlinState.amplitudes.length; ++i) {
            if (perlinState.perm[i] != null) {
                d6 = FormEval.b(d6 + FormEval.b(FormEval.b(perlinState.amplitudes[i] * d5, bl) * FormEval.a(perlinState.perm[i], perlinState.xo[i], perlinState.yo[i], perlinState.zo[i], FormEval.a(FormEval.b(d * d4, bl), bl), FormEval.a(FormEval.b(d2 * d4, bl), bl), FormEval.a(FormEval.b(d3 * d4, bl), bl), bl), bl), bl);
            }
            d4 *= 2.0;
            d5 /= 2.0;
        }
        return d6;
    }

    static double a(double d) {
        return FormEval.a(d, false);
    }

    static double a(double d, boolean bl) {
        return FormEval.b(d - (bl ? Math.floor(FormEval.b(FormEval.b(d / 3.3554432E7, bl) + 0.5, bl)) : (double)FormEval.d(d / 3.3554432E7 + 0.5)) * 3.3554432E7, bl);
    }

    private static long d(double d) {
        long l = (long)d;
        return d < (double)l ? l - 1L : l;
    }

    private static double a(int[] nArray, double d, double d2, double d3, double d4, double d5, double d6, boolean bl) {
        double d7 = FormEval.b(d4 + d, bl);
        double d8 = FormEval.b(d5 + d2, bl);
        double d9 = FormEval.b(d6 + d3, bl);
        int n = FormEval.e(d7);
        int n2 = FormEval.e(d8);
        int n3 = FormEval.e(d9);
        double d10 = FormEval.b(d7 - (double)n, bl);
        double d11 = FormEval.b(d8 - (double)n2, bl);
        double d12 = FormEval.b(d9 - (double)n3, bl);
        int n4 = nArray[n & 0xFF];
        int n5 = nArray[n + 1 & 0xFF];
        int n6 = nArray[n4 + n2 & 0xFF];
        int n7 = nArray[n4 + n2 + 1 & 0xFF];
        int n8 = nArray[n5 + n2 & 0xFF];
        int n9 = nArray[n5 + n2 + 1 & 0xFF];
        double d13 = FormEval.a(nArray[n6 + n3 & 0xFF], d10, d11, d12, bl);
        double d14 = FormEval.a(nArray[n8 + n3 & 0xFF], FormEval.b(d10 - 1.0, bl), d11, d12, bl);
        double d15 = FormEval.a(nArray[n7 + n3 & 0xFF], d10, FormEval.b(d11 - 1.0, bl), d12, bl);
        double d16 = FormEval.a(nArray[n9 + n3 & 0xFF], FormEval.b(d10 - 1.0, bl), FormEval.b(d11 - 1.0, bl), d12, bl);
        double d17 = FormEval.a(nArray[n6 + n3 + 1 & 0xFF], d10, d11, FormEval.b(d12 - 1.0, bl), bl);
        double d18 = FormEval.a(nArray[n8 + n3 + 1 & 0xFF], FormEval.b(d10 - 1.0, bl), d11, FormEval.b(d12 - 1.0, bl), bl);
        double d19 = FormEval.a(nArray[n7 + n3 + 1 & 0xFF], d10, FormEval.b(d11 - 1.0, bl), FormEval.b(d12 - 1.0, bl), bl);
        double d20 = FormEval.a(nArray[n9 + n3 + 1 & 0xFF], FormEval.b(d10 - 1.0, bl), FormEval.b(d11 - 1.0, bl), FormEval.b(d12 - 1.0, bl), bl);
        double d21 = FormEval.c(d10, bl);
        double d22 = FormEval.c(d11, bl);
        double d23 = FormEval.c(d12, bl);
        return FormEval.a(d21, d22, d23, d13, d14, d15, d16, d17, d18, d19, d20, bl);
    }

    private static int e(double d) {
        int n = (int)d;
        return d < (double)n ? n - 1 : n;
    }

    private static double a(int n, double d, double d2, double d3, boolean bl) {
        int[] nArray = i[n & 0xF];
        return FormEval.b(FormEval.b(FormEval.b((double)nArray[0] * d, bl) + FormEval.b((double)nArray[1] * d2, bl), bl) + FormEval.b((double)nArray[2] * d3, bl), bl);
    }

    private static double c(double d, boolean bl) {
        return FormEval.b(FormEval.b(FormEval.b(FormEval.b(d * d, bl) * d, bl) * FormEval.b(FormEval.b(d * FormEval.b(FormEval.b(d * 6.0, bl) - 15.0, bl), bl) + 10.0, bl), bl), bl);
    }

    private static double a(double d, double d2, double d3, boolean bl) {
        return bl ? FormEval.b(FormEval.b(d * FormEval.b(1.0 - d3, bl), bl) + FormEval.b(d2 * d3, bl), bl) : d + d3 * (d2 - d);
    }

    private static double a(double d, double d2, double d3, double d4, double d5, double d6, double d7, double d8, double d9, double d10, double d11, boolean bl) {
        double d12 = FormEval.a(d4, d5, d, bl);
        double d13 = FormEval.a(d6, d7, d, bl);
        double d14 = FormEval.a(d8, d9, d, bl);
        double d15 = FormEval.a(d10, d11, d, bl);
        double d16 = FormEval.a(d12, d13, d2, bl);
        double d17 = FormEval.a(d14, d15, d2, bl);
        return FormEval.a(d16, d17, d3, bl);
    }

    private static double b(double d, double d2, double d3) {
        return d < d2 ? d2 : Math.min(d, d3);
    }

    private static final class CSpline {
        final int a;
        final float[] b;
        final float[] c;
        final float[] d;
        final CSpline[] e;

        CSpline(int n, float[] fArray, float[] fArray2, float[] fArray3, CSpline[] cSplineArray) {
            this.a = n;
            this.b = fArray;
            this.c = fArray2;
            this.d = fArray3;
            this.e = cSplineArray;
        }
    }
}

