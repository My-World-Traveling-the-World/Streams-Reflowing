package dev.streamsreflowing.worldgen.regionprep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FieldForm {
   public final FieldForm.Node root;
   public final List<FieldForm.NoiseState> noises;
   public final int minY;
   public final int maxY;
   public final int cellHeight;

   public FieldForm(FieldForm.Node var1, List<FieldForm.NoiseState> var2, int var3, int var4, int var5) {
      this.root = var1;
      this.noises = var2;
      this.minY = var3;
      this.maxY = var4;
      this.cellHeight = var5;
   }

   public static record Bin() implements FieldForm.Node {
      private final FieldForm.BinOp op;
      private final FieldForm.Node l;
      private final FieldForm.Node r;

      public Bin(FieldForm.BinOp var1, FieldForm.Node var2, FieldForm.Node var3) {
         this.op = var1;
         this.l = var2;
         this.r = var3;
      }
   }

   public static enum BinOp {
      ADD,
      MUL,
      MIN,
      MAX,
      DIV;
   }

   public static record Blended() implements FieldForm.Node {
      private final int minLimit;
      private final int maxLimit;
      private final int main;
      private final double xzMultiplier;
      private final double yMultiplier;
      private final double xzFactor;
      private final double yFactor;
      private final double smearScaleMultiplier;

      public Blended(int var1, int var2, int var3, double var4, double var6, double var8, double var10, double var12) {
         this.minLimit = var1;
         this.maxLimit = var2;
         this.main = var3;
         this.xzMultiplier = var4;
         this.yMultiplier = var6;
         this.xzFactor = var8;
         this.yFactor = var10;
         this.smearScaleMultiplier = var12;
      }
   }

   public static record Cache2D() implements FieldForm.Node {
      private final FieldForm.Node in;

      public Cache2D(FieldForm.Node var1) {
         this.in = var1;
      }
   }

   public static record Clamp() implements FieldForm.Node {
      private final FieldForm.Node in;
      private final double min;
      private final double max;

      public Clamp(FieldForm.Node var1, double var2, double var4) {
         this.in = var1;
         this.min = var2;
         this.max = var4;
      }
   }

   public static record Const() implements FieldForm.Node {
      private final double v;

      public Const(double var1) {
         this.v = var1;
      }
   }

   public static record Extern() implements FieldForm.Node {
      private final FieldForm.Fn fn;
      private final String type;

      public Extern(FieldForm.Fn var1, String var2) {
         this.fn = var1;
         this.type = var2;
      }
   }

   public static record FlatCache() implements FieldForm.Node {
      private final FieldForm.Node in;

      public FlatCache(FieldForm.Node var1) {
         this.in = var1;
      }
   }

   @FunctionalInterface
   public interface Fn {
      double at(int var1, int var2, int var3);
   }

   public static record Interp() implements FieldForm.Node {
      private final FieldForm.Node in;

      public Interp(FieldForm.Node var1) {
         this.in = var1;
      }
   }

   public static record IntervalSelect() implements FieldForm.Node {
      private final FieldForm.Node input;
      private final double[] thresholds;
      private final FieldForm.Node[] functions;

      public IntervalSelect(FieldForm.Node var1, double[] var2, FieldForm.Node[] var3) {
         this.input = var1;
         this.thresholds = var2;
         this.functions = var3;
      }
   }

   public static record Map1() implements FieldForm.Node {
      private final FieldForm.MapOp op;
      private final FieldForm.Node in;

      public Map1(FieldForm.MapOp var1, FieldForm.Node var2) {
         this.op = var1;
         this.in = var2;
      }
   }

   public static enum MapOp {
      ABS,
      SQUARE,
      CUBE,
      HALF_NEGATIVE,
      QUARTER_NEGATIVE,
      SQUEEZE,
      RECIPROCAL,
      SIGNUM,
      SIN;
   }

   public sealed interface Node
      permits FieldForm.Const,
      FieldForm.YGradient,
      FieldForm.Bin,
      FieldForm.Map1,
      FieldForm.Clamp,
      FieldForm.RangeChoice,
      FieldForm.NoiseNode,
      FieldForm.ShiftedNoise,
      FieldForm.ShiftNoise,
      FieldForm.WeirdScaled,
      FieldForm.Blended,
      FieldForm.SplineNode,
      FieldForm.Interp,
      FieldForm.FlatCache,
      FieldForm.IntervalSelect,
      FieldForm.Cache2D,
      FieldForm.Extern,
      FieldForm.ZCoord {
   }

   public static record NoiseNode() implements FieldForm.Node {
      private final int noiseIndex;
      private final double xzScale;
      private final double yScale;

      public NoiseNode(int var1, double var2, double var4) {
         this.noiseIndex = var1;
         this.xzScale = var2;
         this.yScale = var4;
      }
   }

   static final class NoiseRegistry {
      final List<Object> a = new ArrayList<>();
      final List<FieldForm.NoiseState> b = new ArrayList<>();

      int a(Object var1, Supplier<FieldForm.NoiseState> var2) {
         for (int var3 = 0; var3 < this.a.size(); var3++) {
            if (this.a.get(var3) == var1) {
               return var3;
            }
         }

         this.a.add(var1);
         this.b.add((FieldForm.NoiseState)var2.get());
         return this.b.size() - 1;
      }
   }

   public static final class NoiseState {
      public final String name;
      public final boolean normal;
      public final FieldForm.PerlinState first;
      public final FieldForm.PerlinState second;
      public final double valueFactor;

      public NoiseState(String var1, boolean var2, FieldForm.PerlinState var3, FieldForm.PerlinState var4, double var5) {
         this.name = var1;
         this.normal = var2;
         this.first = var3;
         this.second = var4;
         this.valueFactor = var5;
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

      public PerlinState(double var1, double var3, double[] var5, int[][] var6, double[] var7, double[] var8, double[] var9) {
         this.lowestFreqInputFactor = var1;
         this.lowestFreqValueFactor = var3;
         this.amplitudes = var5;
         this.perm = var6;
         this.xo = var7;
         this.yo = var8;
         this.zo = var9;
      }
   }

   public static record RangeChoice() implements FieldForm.Node {
      private final FieldForm.Node input;
      private final double minInclusive;
      private final double maxExclusive;
      private final FieldForm.Node whenInRange;
      private final FieldForm.Node whenOutOfRange;

      public RangeChoice(FieldForm.Node var1, double var2, double var4, FieldForm.Node var6, FieldForm.Node var7) {
         this.input = var1;
         this.minInclusive = var2;
         this.maxExclusive = var4;
         this.whenInRange = var6;
         this.whenOutOfRange = var7;
      }
   }

   public static enum RarityKind {
      TYPE1,
      TYPE2;
   }

   public static enum ShiftKind {
      SHIFT_A,
      SHIFT_B,
      SHIFT;
   }

   public static record ShiftNoise() implements FieldForm.Node {
      private final FieldForm.ShiftKind kind;
      private final int noiseIndex;

      public ShiftNoise(FieldForm.ShiftKind var1, int var2) {
         this.kind = var1;
         this.noiseIndex = var2;
      }
   }

   public static record ShiftedNoise() implements FieldForm.Node {
      private final FieldForm.Node shiftX;
      private final FieldForm.Node shiftY;
      private final FieldForm.Node shiftZ;
      private final double xzScale;
      private final double yScale;
      private final int noiseIndex;

      public ShiftedNoise(FieldForm.Node var1, FieldForm.Node var2, FieldForm.Node var3, double var4, double var6, int var8) {
         this.shiftX = var1;
         this.shiftY = var2;
         this.shiftZ = var3;
         this.xzScale = var4;
         this.yScale = var6;
         this.noiseIndex = var8;
      }
   }

   public static record Spline() {
      private final FieldForm.Node coordinate;
      private final float[] locations;
      private final FieldForm.SplineValue[] values;
      private final float[] derivatives;

      public Spline(FieldForm.Node var1, float[] var2, FieldForm.SplineValue[] var3, float[] var4) {
         this.coordinate = var1;
         this.locations = var2;
         this.values = var3;
         this.derivatives = var4;
      }
   }

   public static record SplineConst() implements FieldForm.SplineValue {
      private final float v;

      public SplineConst(float var1) {
         this.v = var1;
      }
   }

   public static record SplineNested() implements FieldForm.SplineValue {
      private final FieldForm.Spline spline;

      public SplineNested(FieldForm.Spline var1) {
         this.spline = var1;
      }
   }

   public static record SplineNode() implements FieldForm.Node {
      private final FieldForm.Spline spline;

      public SplineNode(FieldForm.Spline var1) {
         this.spline = var1;
      }
   }

   public sealed interface SplineValue permits FieldForm.SplineConst, FieldForm.SplineNested {
   }

   public static record WeirdScaled() implements FieldForm.Node {
      private final FieldForm.Node input;
      private final int noiseIndex;
      private final FieldForm.RarityKind rarity;

      public WeirdScaled(FieldForm.Node var1, int var2, FieldForm.RarityKind var3) {
         this.input = var1;
         this.noiseIndex = var2;
         this.rarity = var3;
      }
   }

   public static record YGradient() implements FieldForm.Node {
      private final int fromY;
      private final int toY;
      private final double fromV;
      private final double toV;

      public YGradient(int var1, int var2, double var3, double var5) {
         this.fromY = var1;
         this.toY = var2;
         this.fromV = var3;
         this.toV = var5;
      }
   }

   public static record ZCoord() implements FieldForm.Node {
   }
}
