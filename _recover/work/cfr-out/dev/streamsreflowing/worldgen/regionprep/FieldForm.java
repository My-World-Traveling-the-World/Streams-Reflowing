/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FieldForm {
    public final Node root;
    public final List<NoiseState> noises;
    public final int minY;
    public final int maxY;
    public final int cellHeight;

    public FieldForm(Node node, List<NoiseState> list, int n, int n2, int n3) {
        this.root = node;
        this.noises = list;
        this.minY = n;
        this.maxY = n2;
        this.cellHeight = n3;
    }

    public static sealed interface Node
    permits Const, YGradient, Bin, Map1, Clamp, RangeChoice, NoiseNode, ShiftedNoise, ShiftNoise, WeirdScaled, Blended, SplineNode, Interp, FlatCache, IntervalSelect, Cache2D, Extern, ZCoord {
    }

    static final class NoiseRegistry {
        final List<Object> a = new ArrayList<Object>();
        final List<NoiseState> b = new ArrayList<NoiseState>();

        NoiseRegistry() {
        }

        int a(Object object, Supplier<NoiseState> supplier) {
            for (int i = 0; i < this.a.size(); ++i) {
                if (this.a.get(i) != object) continue;
                return i;
            }
            this.a.add(object);
            this.b.add(supplier.get());
            return this.b.size() - 1;
        }
    }

    public static final class PerlinState {
        public final double lowestFreqInputFactor;
        public final double lowestFreqValueFactor;
        public final double[] amplitudes;
        public final int[][] perm;
        public final double[] xo;
        public final double[] yo;
        public final double[] zo;

        public PerlinState(double d, double d2, double[] dArray, int[][] nArray, double[] dArray2, double[] dArray3, double[] dArray4) {
            this.lowestFreqInputFactor = d;
            this.lowestFreqValueFactor = d2;
            this.amplitudes = dArray;
            this.perm = nArray;
            this.xo = dArray2;
            this.yo = dArray3;
            this.zo = dArray4;
        }
    }

    public static final class NoiseState {
        public final String name;
        public final boolean normal;
        public final PerlinState first;
        public final PerlinState second;
        public final double valueFactor;

        public NoiseState(String string, boolean bl, PerlinState perlinState, PerlinState perlinState2, double d) {
            this.name = string;
            this.normal = bl;
            this.first = perlinState;
            this.second = perlinState2;
            this.valueFactor = d;
        }
    }

    public record SplineNested(Spline spline) implements SplineValue
    {
    }

    public record SplineConst(float v) implements SplineValue
    {
    }

    public static sealed interface SplineValue
    permits SplineConst, SplineNested {
    }

    public record Spline(Node coordinate, float[] locations, SplineValue[] values, float[] derivatives) {
    }

    public record SplineNode(Spline spline) implements Node
    {
    }

    public record WeirdScaled(Node input, int noiseIndex, RarityKind rarity) implements Node
    {
    }

    public static final class RarityKind
    extends Enum<RarityKind> {
        public static final /* enum */ RarityKind TYPE1 = new RarityKind();
        public static final /* enum */ RarityKind TYPE2 = new RarityKind();
        private static final /* synthetic */ RarityKind[] a;

        public static RarityKind[] values() {
            return (RarityKind[])a.clone();
        }

        public static RarityKind valueOf(String string) {
            return Enum.valueOf(RarityKind.class, string);
        }

        private static /* synthetic */ RarityKind[] a() {
            return new RarityKind[]{TYPE1, TYPE2};
        }

        static {
            a = RarityKind.a();
        }
    }

    public record ShiftNoise(ShiftKind kind, int noiseIndex) implements Node
    {
    }

    public static final class ShiftKind
    extends Enum<ShiftKind> {
        public static final /* enum */ ShiftKind SHIFT_A = new ShiftKind();
        public static final /* enum */ ShiftKind SHIFT_B = new ShiftKind();
        public static final /* enum */ ShiftKind SHIFT = new ShiftKind();
        private static final /* synthetic */ ShiftKind[] a;

        public static ShiftKind[] values() {
            return (ShiftKind[])a.clone();
        }

        public static ShiftKind valueOf(String string) {
            return Enum.valueOf(ShiftKind.class, string);
        }

        private static /* synthetic */ ShiftKind[] a() {
            return new ShiftKind[]{SHIFT_A, SHIFT_B, SHIFT};
        }

        static {
            a = ShiftKind.a();
        }
    }

    public record ShiftedNoise(Node shiftX, Node shiftY, Node shiftZ, double xzScale, double yScale, int noiseIndex) implements Node
    {
    }

    public record NoiseNode(int noiseIndex, double xzScale, double yScale) implements Node
    {
    }

    public record RangeChoice(Node input, double minInclusive, double maxExclusive, Node whenInRange, Node whenOutOfRange) implements Node
    {
    }

    public record Clamp(Node in, double min, double max) implements Node
    {
    }

    public record Map1(MapOp op, Node in) implements Node
    {
    }

    public static final class MapOp
    extends Enum<MapOp> {
        public static final /* enum */ MapOp ABS = new MapOp();
        public static final /* enum */ MapOp SQUARE = new MapOp();
        public static final /* enum */ MapOp CUBE = new MapOp();
        public static final /* enum */ MapOp HALF_NEGATIVE = new MapOp();
        public static final /* enum */ MapOp QUARTER_NEGATIVE = new MapOp();
        public static final /* enum */ MapOp SQUEEZE = new MapOp();
        public static final /* enum */ MapOp RECIPROCAL = new MapOp();
        public static final /* enum */ MapOp SIGNUM = new MapOp();
        public static final /* enum */ MapOp SIN = new MapOp();
        private static final /* synthetic */ MapOp[] a;

        public static MapOp[] values() {
            return (MapOp[])a.clone();
        }

        public static MapOp valueOf(String string) {
            return Enum.valueOf(MapOp.class, string);
        }

        private static /* synthetic */ MapOp[] a() {
            return new MapOp[]{ABS, SQUARE, CUBE, HALF_NEGATIVE, QUARTER_NEGATIVE, SQUEEZE, RECIPROCAL, SIGNUM, SIN};
        }

        static {
            a = MapOp.a();
        }
    }

    public record Bin(BinOp op, Node l, Node r) implements Node
    {
    }

    public static final class BinOp
    extends Enum<BinOp> {
        public static final /* enum */ BinOp ADD = new BinOp();
        public static final /* enum */ BinOp MUL = new BinOp();
        public static final /* enum */ BinOp MIN = new BinOp();
        public static final /* enum */ BinOp MAX = new BinOp();
        public static final /* enum */ BinOp DIV = new BinOp();
        private static final /* synthetic */ BinOp[] a;

        public static BinOp[] values() {
            return (BinOp[])a.clone();
        }

        public static BinOp valueOf(String string) {
            return Enum.valueOf(BinOp.class, string);
        }

        private static /* synthetic */ BinOp[] a() {
            return new BinOp[]{ADD, MUL, MIN, MAX, DIV};
        }

        static {
            a = BinOp.a();
        }
    }

    public record ZCoord() implements Node
    {
    }

    public record YGradient(int fromY, int toY, double fromV, double toV) implements Node
    {
    }

    public record Const(double v) implements Node
    {
    }

    public record Blended(int minLimit, int maxLimit, int main, double xzMultiplier, double yMultiplier, double xzFactor, double yFactor, double smearScaleMultiplier) implements Node
    {
    }

    public record Interp(Node in) implements Node
    {
    }

    public record FlatCache(Node in) implements Node
    {
    }

    public record IntervalSelect(Node input, double[] thresholds, Node[] functions) implements Node
    {
    }

    public record Cache2D(Node in) implements Node
    {
    }

    @FunctionalInterface
    public static interface Fn {
        public double at(int var1, int var2, int var3);
    }

    public record Extern(Fn fn, String type) implements Node
    {
    }
}

