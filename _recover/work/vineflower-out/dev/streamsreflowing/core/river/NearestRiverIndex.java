package dev.streamsreflowing.core.river;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
   private static final ThreadLocal<NearestRiverIndex.TieredCand[]> A = ThreadLocal.withInitial(() -> {
      NearestRiverIndex.TieredCand[] var0 = new NearestRiverIndex.TieredCand[8];

      for (int var1 = 0; var1 < var0.length; var1++) {
         var0[var1] = new NearestRiverIndex.TieredCand();
      }

      return var0;
   });
   private static final ThreadLocal<int[]> B = ThreadLocal.withInitial(() -> new int[1]);
   private static final ThreadLocal<long[]> C = ThreadLocal.withInitial(() -> new long[1]);
   private static final ThreadLocal<int[][]> D = ThreadLocal.withInitial(() -> new int[][]{new int[512]});

   public NearestRiverIndex(List<RiverSegment> var1, double var2, int var4, double var5) {
      this.f = var1;
      this.y = Math.max(1, var4);
      this.s = var5;
      HashMap var7 = new HashMap();

      for (int var8 = 0; var8 < var1.size(); var8++) {
         RiverSegment var9 = (RiverSegment)var1.get(var8);
         int var10 = this.a(var9.minX() - var2);
         int var11 = this.a(var9.maxX() + var2);
         int var12 = this.b(var9.minZ() - var2);
         int var13 = this.b(var9.maxZ() + var2);

         for (int var14 = var10; var14 <= var11; var14++) {
            for (int var15 = var12; var15 <= var13; var15++) {
               var7.computeIfAbsent(a(var14, var15), var0 -> new ArrayList<>()).add(var8);
            }
         }
      }

      this.z = new HashMap<>(var7.size() * 2);
      int var24 = var1.size();
      this.g = new double[var24];
      this.h = new double[var24];
      this.i = new double[var24];
      this.j = new double[var24];
      this.k = new double[var24];
      this.l = new double[var24];
      this.m = new double[var24];
      this.n = new double[var24];
      this.o = new double[var24];
      this.p = new double[var24];
      this.q = new double[var24];
      this.r = new double[var24];

      for (int var25 = 0; var25 < var24; var25++) {
         RiverSegment var28 = (RiverSegment)var1.get(var25);
         this.p[var25] = var28.w1();
         this.q[var25] = var28.w2();
         double var31 = Math.max(var28.w1(), var28.w2());
         double var37 = var31 + Math.max(5.0, var31 * var5);
         this.r[var25] = var37 * var37;
         double var51 = var28.x2() - var28.x1();
         double var17 = var28.z2() - var28.z1();
         this.g[var25] = var28.x1();
         this.h[var25] = var28.z1();
         this.i[var25] = var51;
         this.j[var25] = var17;
         this.k[var25] = var51 * var51 + var17 * var17;
         this.l[var25] = Math.min(var28.x1(), var28.x2());
         this.m[var25] = Math.max(var28.x1(), var28.x2());
         this.n[var25] = Math.min(var28.z1(), var28.z2());
         this.o[var25] = Math.max(var28.z1(), var28.z2());
      }

      for (Entry var29 : var7.entrySet()) {
         List var32 = (List)var29.getValue();
         int[] var34 = new int[var32.size()];

         for (int var38 = 0; var38 < var34.length; var38++) {
            var34[var38] = (Integer)var32.get(var38);
         }

         this.z.put((Long)var29.getKey(), var34);
      }

      this.t = new boolean[var1.size()];
      this.v = new boolean[var1.size()];
      this.u = new double[var1.size()];
      Arrays.fill(this.u, Double.MAX_VALUE);
      HashSet var27 = new HashSet(var1.size() * 2);
      HashMap var30 = new HashMap(var1.size() * 2);
      HashMap var33 = new HashMap(var1.size() * 2);

      for (int var35 = 0; var35 < var1.size(); var35++) {
         RiverSegment var39 = (RiverSegment)var1.get(var35);
         var27.add(a(var39.x1(), var39.z1()));
         var33.put(a(var39.x1(), var39.z1()), var35);
         long var44 = a(var39.x2(), var39.z2());
         Integer var16 = (Integer)var30.get(var44);
         if (var16 == null || a(var39) > a((RiverSegment)var1.get(var16))) {
            var30.put(var44, var35);
         }
      }

      boolean[] var36 = new boolean[var1.size()];

      for (int var40 = 0; var40 < var1.size(); var40++) {
         RiverSegment var45 = (RiverSegment)var1.get(var40);
         var36[var40] = !var27.contains(a(var45.x2(), var45.z2()));
      }

      for (int var41 = 0; var41 < var1.size(); var41++) {
         int var46 = var41;
         double var52 = 0.0;

         for (int var57 = 0; var57 < 64; var57++) {
            if (var36[var46]) {
               this.t[var41] = var52 <= 6.0;
               this.u[var41] = var52;
               break;
            }

            RiverSegment var18 = (RiverSegment)var1.get(var46);
            Integer var19 = (Integer)var33.get(a(var18.x2(), var18.z2()));
            if (var19 == null || var19 == var46) {
               break;
            }

            var52 += Math.hypot(var18.x2() - var18.x1(), var18.z2() - var18.z1());
            if (var52 > 64.0) {
               break;
            }

            var46 = var19;
         }
      }

      for (int var42 = 0; var42 < var1.size(); var42++) {
         int var47 = var42;
         double var53 = 0.0;

         for (int var58 = 0; var58 < 64; var58++) {
            RiverSegment var61 = (RiverSegment)var1.get(var47);
            Integer var65 = (Integer)var30.get(a(var61.x1(), var61.z1()));
            if (var65 == null || var65 == var47) {
               this.v[var42] = var53 <= 6.0;
               break;
            }

            var53 += Math.hypot(var61.x2() - var61.x1(), var61.z2() - var61.z1());
            if (var53 > 6.0) {
               break;
            }

            var47 = var65;
         }
      }

      double[] var43 = new double[var1.size()];

      for (int var48 = 0; var48 < var1.size(); var48++) {
         var43[var48] = this.a(var48, var30);
      }

      this.w = new double[var1.size()];

      for (int var49 = 0; var49 < var1.size(); var49++) {
         double var54 = var43[var49];
         int var59 = var49;
         double var62 = 0.0;

         for (int var20 = 0; var20 < 64 && var62 < 16.0; var20++) {
            RiverSegment var21 = (RiverSegment)var1.get(var59);
            Integer var22 = (Integer)var30.get(a(var21.x1(), var21.z1()));
            if (var22 == null || var22 == var59) {
               break;
            }

            var54 = Math.max(var54, var43[var22]);
            RiverSegment var23 = (RiverSegment)var1.get(var22);
            var62 += Math.hypot(var23.x2() - var23.x1(), var23.z2() - var23.z1());
            var59 = var22;
         }

         this.w[var49] = var54;
      }

      HashMap var50 = new HashMap(var1.size() * 2);
      HashMap var55 = new HashMap(var1.size() * 2);
      HashMap var56 = new HashMap(var1.size() * 2);
      HashMap var60 = new HashMap(var1.size() * 2);

      for (int var63 = 0; var63 < var1.size(); var63++) {
         RiverSegment var66 = (RiverSegment)var1.get(var63);
         long var70 = a(var66.x1(), var66.z1());
         long var73 = a(var66.x2(), var66.z2());
         var50.merge(var70, 1, Integer::sum);
         var56.put(var70, var63);
         var55.merge(var73, 1, Integer::sum);
         var60.put(var73, var63);
      }

      int[] var64 = new int[var1.size()];
      int var67 = 0;

      while (var67 < var64.length) {
         var64[var67] = var67++;
      }

      for (Entry var71 : var56.entrySet()) {
         long var72 = (Long)var71.getKey();
         if (var50.getOrDefault(var72, 0) == 1 && var55.getOrDefault(var72, 0) == 1) {
            a(var64, (Integer)var60.get(var72), (Integer)var71.getValue());
         }
      }

      this.x = new int[var1.size()];

      for (int var69 = 0; var69 < this.x.length; var69++) {
         this.x[var69] = a(var64, var69);
      }
   }

   private static int a(int[] var0, int var1) {
      while (var0[var1] != var1) {
         var0[var1] = var0[var0[var1]];
         var1 = var0[var1];
      }

      return var1;
   }

   private static void a(int[] var0, int var1, int var2) {
      int var3 = a(var0, var1);
      int var4 = a(var0, var2);
      if (var3 != var4) {
         var0[var3] = var4;
      }
   }

   private double a(int var1, Map<Long, Integer> var2) {
      RiverSegment var3 = this.f.get(var1);
      double var4 = var3.y1() - var3.y2();
      double var6 = Math.hypot(var3.x2() - var3.x1(), var3.z2() - var3.z1());
      int var8 = var1;

      for (int var9 = 0; var9 < 64 && var6 < 4.0; var9++) {
         RiverSegment var10 = this.f.get(var8);
         Integer var11 = (Integer)var2.get(a(var10.x1(), var10.z1()));
         if (var11 == null || var11 == var8) {
            break;
         }

         RiverSegment var12 = this.f.get(var11);
         var4 += var12.y1() - var12.y2();
         var6 += Math.hypot(var12.x2() - var12.x1(), var12.z2() - var12.z1());
         var8 = var11;
      }

      return var6 > 1.0E-6 ? Math.max(0.0, var4) / var6 : 0.0;
   }

   private static double a(RiverSegment var0) {
      double var1 = Math.hypot(var0.x2() - var0.x1(), var0.z2() - var0.z1());
      return var1 > 1.0E-6 ? (var0.y1() - var0.y2()) / var1 : 0.0;
   }

   private static long a(double var0, double var2) {
      long var4 = Math.round(var0 * 16.0) & 4294967295L;
      long var6 = Math.round(var2 * 16.0) & 4294967295L;
      return var4 << 32 | var6;
   }

   public RiverSample nearest(double var1, double var3, double var5) {
      if (this.f.isEmpty()) {
         return null;
      } else {
         int[] var7 = this.a(var1, var3, var5);
         double var8 = Double.MAX_VALUE;
         int var10 = -1;
         double var11 = 0.0;
         double var13 = var5;

         for (int var15 = 0; var7[var15] >= 0; var15++) {
            int var16 = var7[var15];
            double var17 = var1 < this.l[var16] ? this.l[var16] - var1 : (var1 > this.m[var16] ? var1 - this.m[var16] : 0.0);
            double var19 = var3 < this.n[var16] ? this.n[var16] - var3 : (var3 > this.o[var16] ? var3 - this.o[var16] : 0.0);
            if (!(var17 * var17 + var19 * var19 > var13 * var13)) {
               double var21 = this.i[var16];
               double var23 = this.j[var16];
               double var25 = this.k[var16];
               double var27;
               if (var25 <= 1.0E-9) {
                  var27 = 0.0;
               } else {
                  var27 = ((var1 - this.g[var16]) * var21 + (var3 - this.h[var16]) * var23) / var25;
                  var27 = var27 < 0.0 ? 0.0 : (var27 > 1.0 ? 1.0 : var27);
               }

               double var29 = this.g[var16] + var21 * var27;
               double var31 = this.h[var16] + var23 * var27;
               double var33 = var1 - var29;
               double var35 = var3 - var31;
               double var37 = Math.sqrt(var33 * var33 + var35 * var35);
               if (var37 < var8 || var37 == var8 && var16 < var10) {
                  var8 = var37;
                  var10 = var16;
                  var13 = var37 < var5 ? var37 : var5;
                  var11 = var27;
               }
            }
         }

         return var10 >= 0 && !(var8 > var5) ? this.a(var10, var11, var8) : null;
      }
   }

   public RiverSample nearestTiered(double var1, double var3, double var5) {
      if (this.f.isEmpty()) {
         return null;
      } else {
         int[] var7 = this.a(var1, var3, var5);
         double var8 = Double.MAX_VALUE;
         int var10 = -1;
         double var11 = 0.0;
         double var13 = Double.MAX_VALUE;
         int var15 = -1;
         double var16 = 0.0;
         double var18 = Double.MAX_VALUE;
         int var20 = -1;
         double var21 = 0.0;
         double var23 = var5;

         for (int var25 = 0; var7[var25] >= 0; var25++) {
            int var26 = var7[var25];
            double var27 = var1 < this.l[var26] ? this.l[var26] - var1 : (var1 > this.m[var26] ? var1 - this.m[var26] : 0.0);
            double var29 = var3 < this.n[var26] ? this.n[var26] - var3 : (var3 > this.o[var26] ? var3 - this.o[var26] : 0.0);
            double var31 = var27 * var27 + var29 * var29;
            if (!(var31 > var23 * var23) || !(var31 > this.r[var26])) {
               double var33 = this.i[var26];
               double var35 = this.j[var26];
               double var37 = this.k[var26];
               double var39;
               if (var37 <= 1.0E-9) {
                  var39 = 0.0;
               } else {
                  var39 = ((var1 - this.g[var26]) * var33 + (var3 - this.h[var26]) * var35) / var37;
                  var39 = var39 < 0.0 ? 0.0 : (var39 > 1.0 ? 1.0 : var39);
               }

               double var41 = this.g[var26] + var33 * var39;
               double var43 = this.h[var26] + var35 * var39;
               double var45 = var1 - var41;
               double var47 = var3 - var43;
               double var49 = Math.sqrt(var45 * var45 + var47 * var47);
               if (var49 < var8 || var49 == var8 && var26 < var10) {
                  var8 = var49;
                  var10 = var26;
                  var23 = var49 < var5 ? var49 : var5;
                  var11 = var39;
               }

               if (var49 <= var5) {
                  double var51 = this.p[var26] + (this.q[var26] - this.p[var26]) * var39;
                  if (var49 <= var51 && (var49 < var18 || var49 == var18 && var26 < var20)) {
                     var18 = var49;
                     var20 = var26;
                     var21 = var39;
                  }

                  if (var49 <= var51 + Math.max(5.0, var51 * this.s) && (var49 < var13 || var49 == var13 && var26 < var15)) {
                     var13 = var49;
                     var15 = var26;
                     var16 = var39;
                  }
               }
            }
         }

         if (var20 >= 0) {
            var10 = var20;
            var8 = var18;
            var11 = var21;
         } else if (var15 >= 0) {
            var10 = var15;
            var8 = var13;
            var11 = var16;
         }

         return var10 >= 0 && !(var8 > var5) ? this.a(var10, var11, var8) : null;
      }
   }

   private int[] a(double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1 / 16.0);
      int var8 = (int)Math.floor(var3 / 16.0);
      long var9 = (long)var7 << 32 ^ (long)var8 & 4294967295L;
      NearestRiverIndex.TieredCand[] var11 = A.get();

      for (NearestRiverIndex.TieredCand var15 : var11) {
         if (var15.a == this && var15.b == var9 && var15.c == var5) {
            return var15.d;
         }
      }

      long[] var30 = C.get();
      long var31 = var9 * 31L + Double.doubleToLongBits(var5) + (long)System.identityHashCode(this);
      if (var30[0] != var31) {
         var30[0] = var31;
         int[] var33 = D.get()[0];
         int var16 = 0;
         int var34 = this.a(var1 - var5);
         int var18 = this.a(var1 + var5);
         int var35 = this.b(var3 - var5);
         int var36 = this.b(var3 + var5);

         for (int var37 = var34; var37 <= var18; var37++) {
            for (int var38 = var35; var38 <= var36; var38++) {
               int[] var39 = this.z.get(a(var37, var38));
               if (var39 != null) {
                  if (var16 + var39.length + 1 > var33.length) {
                     var33 = Arrays.copyOf(var33, Math.max(var33.length * 2, var16 + var39.length + 1));
                     D.get()[0] = var33;
                  }

                  System.arraycopy(var39, 0, var33, var16, var39.length);
                  var16 += var39.length;
               }
            }
         }

         if (var16 + 1 > var33.length) {
            var33 = Arrays.copyOf(var33, var16 + 1);
            D.get()[0] = var33;
         }

         var33[var16] = -1;
         return var33;
      } else {
         double var32 = (double)var7 * 16.0;
         double var17 = (double)var8 * 16.0;
         int var19 = this.a(var32 - var5);
         int var20 = this.a(var32 + 16.0 + var5);
         int var21 = this.b(var17 - var5);
         int var22 = this.b(var17 + 16.0 + var5);
         int var23 = 0;

         for (int var24 = var19; var24 <= var20; var24++) {
            for (int var25 = var21; var25 <= var22; var25++) {
               int[] var26 = this.z.get(a(var24, var25));
               if (var26 != null) {
                  var23 += var26.length;
               }
            }
         }

         int[] var40 = new int[var23];
         int var41 = 0;

         for (int var42 = var19; var42 <= var20; var42++) {
            for (int var27 = var21; var27 <= var22; var27++) {
               int[] var28 = this.z.get(a(var42, var27));
               if (var28 != null) {
                  System.arraycopy(var28, 0, var40, var41, var28.length);
                  var41 += var28.length;
               }
            }
         }

         Arrays.sort(var40);
         int var43 = 0;

         for (int var44 = 0; var44 < var40.length; var44++) {
            if (var44 == 0 || var40[var44] != var40[var44 - 1]) {
               var40[var43++] = var40[var44];
            }
         }

         int[] var45 = Arrays.copyOf(var40, var43 + 1);
         var45[var43] = -1;
         int[] var46 = B.get();
         NearestRiverIndex.TieredCand var29 = var11[var46[0]];
         var46[0] = var46[0] + 1 & 7;
         var29.a = this;
         var29.b = var9;
         var29.c = var5;
         var29.d = var45;
         return var45;
      }
   }

   public boolean anyCorridorTouchesBox(double var1, double var3, double var5, double var7) {
      if (this.f.isEmpty()) {
         return false;
      } else {
         int var9 = this.a(var1);
         int var10 = this.a(var5);
         int var11 = this.b(var3);
         int var12 = this.b(var7);

         for (int var13 = var9; var13 <= var10; var13++) {
            for (int var14 = var11; var14 <= var12; var14++) {
               int[] var15 = this.z.get(a(var13, var14));
               if (var15 != null) {
                  for (int var19 : var15) {
                     double var20 = this.l[var19] > var5 ? this.l[var19] - var5 : (this.m[var19] < var1 ? var1 - this.m[var19] : 0.0);
                     double var22 = this.n[var19] > var7 ? this.n[var19] - var7 : (this.o[var19] < var3 ? var3 - this.o[var19] : 0.0);
                     if (var20 * var20 + var22 * var22 <= this.r[var19]) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private RiverSample a(int var1, double var2, double var4) {
      RiverSegment var6 = this.f.get(var1);
      double var7 = var6.x2() - var6.x1();
      double var9 = var6.z2() - var6.z1();
      double var11 = Math.sqrt(var7 * var7 + var9 * var9);
      double var13 = var11 > 1.0E-6 ? (var6.y1() - var6.y2()) / var11 : 0.0;
      double var15 = var11 > 1.0E-6 ? (var6.x2() - var6.x1()) / var11 : 0.0;
      double var17 = var11 > 1.0E-6 ? (var6.z2() - var6.z1()) / var11 : 0.0;
      return new RiverSample(var4, var6.gradeAt(var2), var6.widthAt(var2), var13, var15, var17, this.t[var1], this.v[var1], this.w[var1], this.u[var1], var1);
   }

   public RiverSample nearestCoveringForeign(double var1, double var3, double var5, int var7) {
      if (this.f.isEmpty()) {
         return null;
      } else {
         int var8 = var7 >= 0 ? this.x[var7] : Integer.MIN_VALUE;
         int var9 = this.a(var1 - var5);
         int var10 = this.a(var1 + var5);
         int var11 = this.b(var3 - var5);
         int var12 = this.b(var3 + var5);
         double var13 = Double.MAX_VALUE;
         int var15 = -1;
         double var16 = 0.0;

         for (int var18 = var9; var18 <= var10; var18++) {
            for (int var19 = var11; var19 <= var12; var19++) {
               int[] var20 = this.z.get(a(var18, var19));
               if (var20 != null) {
                  double var21 = var13 < var5 ? var13 : var5;

                  for (int var26 : var20) {
                     if (this.x[var26] != var8) {
                        double var27 = var1 < this.l[var26] ? this.l[var26] - var1 : (var1 > this.m[var26] ? var1 - this.m[var26] : 0.0);
                        double var29 = var3 < this.n[var26] ? this.n[var26] - var3 : (var3 > this.o[var26] ? var3 - this.o[var26] : 0.0);
                        double var31 = var27 * var27 + var29 * var29;
                        if (!(var31 > var21 * var21) && !(var31 > this.r[var26])) {
                           double var33 = this.i[var26];
                           double var35 = this.j[var26];
                           double var37 = this.k[var26];
                           double var39;
                           if (var37 <= 1.0E-9) {
                              var39 = 0.0;
                           } else {
                              var39 = ((var1 - this.g[var26]) * var33 + (var3 - this.h[var26]) * var35) / var37;
                              var39 = var39 < 0.0 ? 0.0 : (var39 > 1.0 ? 1.0 : var39);
                           }

                           double var41 = this.g[var26] + var33 * var39;
                           double var43 = this.h[var26] + var35 * var39;
                           double var45 = var1 - var41;
                           double var47 = var3 - var43;
                           double var49 = Math.sqrt(var45 * var45 + var47 * var47);
                           if (!(var49 > var5) && !(var49 > var13) && (var49 != var13 || var26 < var15)) {
                              double var51 = this.p[var26] + (this.q[var26] - this.p[var26]) * var39;
                              if (var49 <= var51 + Math.max(5.0, var51 * this.s)) {
                                 var13 = var49;
                                 var15 = var26;
                                 var16 = var39;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return var15 < 0 ? null : this.a(var15, var16, var13);
      }
   }

   public double lowerNeighborGrade(double var1, double var3, double var5, int var7, double var8, double var10) {
      if (this.f.isEmpty()) {
         return Double.NaN;
      } else {
         int var12 = var7 >= 0 ? this.x[var7] : -1;
         double var13 = var8 - var10;
         int var15 = this.a(var1 - var5);
         int var16 = this.a(var1 + var5);
         int var17 = this.b(var3 - var5);
         int var18 = this.b(var3 + var5);
         double var19 = Double.NaN;

         for (int var21 = var15; var21 <= var16; var21++) {
            for (int var22 = var17; var22 <= var18; var22++) {
               int[] var23 = this.z.get(a(var21, var22));
               if (var23 != null) {
                  for (int var27 : var23) {
                     if (var27 != var7 && this.x[var27] != var12) {
                        RiverSegment var28 = this.f.get(var27);
                        double var29 = var28.clampParam(var1, var3);
                        if (!(var28.distanceAt(var1, var3, var29) > var5)) {
                           double var31 = var28.gradeAt(var29);
                           if (var31 < var13 && (Double.isNaN(var19) || var31 < var19)) {
                              var19 = var31;
                           }
                        }
                     }
                  }
               }
            }
         }

         return var19;
      }
   }

   public boolean inAnyChannel(double var1, double var3, double var5) {
      if (this.f.isEmpty()) {
         return false;
      } else {
         int var7 = this.a(var1 - var5);
         int var8 = this.a(var1 + var5);
         int var9 = this.b(var3 - var5);
         int var10 = this.b(var3 + var5);

         for (int var11 = var7; var11 <= var8; var11++) {
            for (int var12 = var9; var12 <= var10; var12++) {
               int[] var13 = this.z.get(a(var11, var12));
               if (var13 != null) {
                  for (int var17 : var13) {
                     RiverSegment var18 = this.f.get(var17);
                     double var19 = var18.clampParam(var1, var3);
                     if (var18.distanceAt(var1, var3, var19) <= var18.widthAt(var19)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private int a(double var1) {
      return (int)Math.floor(var1 / (double)this.y);
   }

   private int b(double var1) {
      return (int)Math.floor(var1 / (double)this.y);
   }

   private static long a(int var0, int var1) {
      return (long)var0 << 32 ^ (long)var1 & 4294967295L;
   }

   private static final class TieredCand {
      NearestRiverIndex a;
      long b;
      double c;
      int[] d;
   }
}
