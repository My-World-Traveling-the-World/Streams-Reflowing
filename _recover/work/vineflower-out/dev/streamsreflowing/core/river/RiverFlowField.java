package dev.streamsreflowing.core.river;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;

public final class RiverFlowField {
   public static final int LAND = 0;
   public static final int RIVER = 1;
   public static final int OCEAN = 2;
   private static final int[] a = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] b = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final double c = Math.sqrt(2.0);
   private static final int d = 2;
   private static final int e = 3;
   private static final double[] f = new double[8];
   private static final double[] g = new double[8];
   private final int h;
   private final int i;
   private final int j;
   private final int k;
   private final int l;
   private final byte[] m;
   public final int oceanCells;
   public final int riverCells;
   public final int flowedCells;
   private static final int n = 4;
   private static final int o = 2;
   private static final int p = 8;
   private static final int q = 2;
   public static volatile int lastSampled;
   public static volatile int lastCells;
   private static final long r = 9221120239188049920L;

   private RiverFlowField(int var1, int var2, int var3, int var4, int var5, byte[] var6, int var7, int var8, int var9) {
      this.h = var1;
      this.i = var2;
      this.j = var3;
      this.k = var4;
      this.l = var5;
      this.m = var6;
      this.oceanCells = var7;
      this.riverCells = var8;
      this.flowedCells = var9;
   }

   public void writeTo(DataOutputStream var1) throws IOException {
      var1.writeInt(this.h);
      var1.writeInt(this.i);
      var1.writeInt(this.j);
      var1.writeInt(this.k);
      var1.writeInt(this.l);
      var1.write(this.m);
      var1.writeInt(this.oceanCells);
      var1.writeInt(this.riverCells);
      var1.writeInt(this.flowedCells);
   }

   public static RiverFlowField readFrom(DataInputStream var0) throws IOException {
      int var1 = var0.readInt();
      int var2 = var0.readInt();
      int var3 = var0.readInt();
      int var4 = var0.readInt();
      int var5 = var0.readInt();
      if (var3 > 0 && var4 > 0 && var5 > 0 && var4 <= 4096 && var5 <= 4096) {
         byte[] var6 = new byte[var4 * var5];
         var0.readFully(var6);
         int var7 = var0.readInt();
         int var8 = var0.readInt();
         int var9 = var0.readInt();
         return new RiverFlowField(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      } else {
         return null;
      }
   }

   private static void a(byte[] var0, int var1, int var2, int var3, int var4, int var5, int var6, RiverFlowField.BiomeClass var7) {
      int var8 = Math.max(2, Math.min(8, var6 > 0 ? var6 : 4));
      boolean[] var9 = new boolean[var1 * var2];
      int var10 = 0;

      for (int var11 = 0; var11 < var2; var11 += var8) {
         for (int var12 = 0; var12 < var1; var12 += var8) {
            int var13 = var11 * var1 + var12;
            var0[var13] = (byte)var7.classify(var3 + var12 * var5, var4 + var11 * var5);
            var9[var13] = true;
            var10++;
         }
      }

      for (int var21 = var8; var21 > 1; var21 >>= 1) {
         int var24 = var21 >> 1;
         boolean[] var28 = new boolean[var1 * var2];

         for (int var14 = 0; var14 < var2; var14 += var21) {
            for (int var15 = 0; var15 < var1; var15 += var21) {
               if (a(var0, var9, var1, var2, var15, var14, var21)) {
                  int var16 = 2 * var21;

                  for (int var17 = -var16; var17 <= var16; var17 += var24) {
                     for (int var18 = -var16; var18 <= var16; var18 += var24) {
                        int var19 = var15 + var18;
                        int var20 = var14 + var17;
                        if (var19 >= 0 && var19 < var1 && var20 >= 0 && var20 < var2) {
                           var28[var20 * var1 + var19] = true;
                        }
                     }
                  }
               }
            }
         }

         for (int var31 = 0; var31 < var2; var31 += var24) {
            for (int var34 = 0; var34 < var1; var34 += var24) {
               int var37 = var31 * var1 + var34;
               if (var28[var37] && !var9[var37]) {
                  var0[var37] = (byte)var7.classify(var3 + var34 * var5, var4 + var31 * var5);
                  var9[var37] = true;
                  var10++;
               }
            }
         }
      }

      ArrayDeque var22 = new ArrayDeque();

      for (int var25 = 0; var25 < var1 * var2; var25++) {
         if (var9[var25] && var0[var25] == 1) {
            var22.add(var25);
         }
      }

      int var26 = var1 * var2 - var10;

      while (!var22.isEmpty() && var26 > 0) {
         int var29 = (Integer)var22.poll();
         int var32 = var29 % var1;
         int var35 = var29 / var1;

         for (int var38 = 0; var38 < 8 && var26 > 0; var38++) {
            int var39 = var32 + a[var38];
            int var40 = var35 + b[var38];
            if (var39 >= 0 && var39 < var1 && var40 >= 0 && var40 < var2) {
               int var41 = var40 * var1 + var39;
               if (!var9[var41]) {
                  var0[var41] = (byte)var7.classify(var3 + var39 * var5, var4 + var40 * var5);
                  var9[var41] = true;
                  var10++;
                  var26--;
                  if (var0[var41] == 1) {
                     var22.add(var41);
                  }
               }
            }
         }
      }

      for (int var23 = 0; var23 < var2; var23++) {
         for (int var27 = 0; var27 < var1; var27++) {
            int var30 = var23 * var1 + var27;
            if (!var9[var30]) {
               int var33 = Math.min(var27 - var27 % var8, var1 - 1);
               int var36 = Math.min(var23 - var23 % var8, var2 - 1);
               var0[var30] = var0[var36 * var1 + var33];
            }
         }
      }

      lastSampled = var10;
      lastCells = var1 * var2;
   }

   private static boolean a(byte[] var0, boolean[] var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = var5 * var2 + var4;
      if (!var1[var7]) {
         return false;
      } else if (var0[var7] == 1) {
         return true;
      } else {
         for (int var8 = 0; var8 < 4; var8++) {
            int var9 = var4 + (var8 == 0 ? var6 : (var8 == 1 ? -var6 : 0));
            int var10 = var5 + (var8 == 2 ? var6 : (var8 == 3 ? -var6 : 0));
            if (var9 >= 0 && var9 < var2 && var10 >= 0 && var10 < var3) {
               int var11 = var10 * var2 + var9;
               if (var1[var11] && var0[var11] != var0[var7]) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static RiverFlowField build(
      int var0, int var1, int var2, int var3, int var4, int var5, int var6, RiverFlowField.BiomeClass var7, RiverFlowField.OrientationField var8
   ) {
      int var9 = var0 - var4;
      int var10 = var1 - var4;
      int var11 = (var2 + 2 * var4) / var5 + 1;
      int var12 = (var3 + 2 * var4) / var5 + 1;
      byte[] var13 = new byte[var11 * var12];
      float[] var14 = new float[var11 * var12];
      Arrays.fill(var14, Float.POSITIVE_INFINITY);
      PriorityQueue var15 = new PriorityQueue();
      a(var13, var11, var12, var9, var10, var5, var6, var7);
      int var16 = 0;
      int var17 = 0;

      for (int var18 = 0; var18 < var11 * var12; var18++) {
         if (var13[var18] == 2) {
            var16++;
         } else if (var13[var18] == 1) {
            var17++;
         }
      }

      boolean[] var39 = new boolean[var11 * var12];
      boolean[] var19 = new boolean[var11 * var12];
      int[] var20 = new int[var11 * var12];
      Arrays.fill(var20, -1);
      int var21 = 0;
      ArrayDeque var22 = new ArrayDeque();

      for (int var23 = 0; var23 < var11 * var12; var23++) {
         if (var13[var23] == 1 && !var19[var23]) {
            int var24 = var21++;
            var19[var23] = true;
            var20[var23] = var24;
            var22.add(var23);
            int var25 = var23;
            boolean var26 = false;

            while (!var22.isEmpty()) {
               int var27 = (Integer)var22.poll();
               var25 = var27;
               int var28 = var27 % var11;
               int var29 = var27 / var11;

               for (int var30 = 0; var30 < 8; var30++) {
                  int var31 = var28 + a[var30];
                  int var32 = var29 + b[var30];
                  if (var31 >= 0 && var32 >= 0 && var31 < var11 && var32 < var12) {
                     int var33 = var32 * var11 + var31;
                     if (var13[var33] == 1) {
                        if (!var19[var33]) {
                           var19[var33] = true;
                           var20[var33] = var24;
                           var22.add(var33);
                        }
                     } else if (var13[var33] == 2) {
                        var26 = true;
                     }
                  }
               }
            }

            if (var26 || var8 != null) {
               var39[var25] = true;
            }
         }
      }

      for (int var40 = 0; var40 < var11 * var12; var40++) {
         if (var39[var40]) {
            var14[var40] = 0.0F;
            var15.add(a(0.0F, var40));
         }
      }

      while (!var15.isEmpty()) {
         long var41 = (Long)var15.poll();
         int var44 = (int)(var41 & 4294967295L);
         float var47 = Float.intBitsToFloat((int)(var41 >>> 32));
         if (!(var47 > var14[var44])) {
            int var50 = var44 % var11;
            int var53 = var44 / var11;

            for (int var56 = 0; var56 < 8; var56++) {
               int var59 = var50 + a[var56];
               int var63 = var53 + b[var56];
               if (var59 >= 0 && var63 >= 0 && var59 < var11 && var63 < var12) {
                  int var66 = var63 * var11 + var59;
                  if (var13[var66] == 1) {
                     float var69 = var47 + ((var56 & 1) == 0 ? 1.0F : (float)c);
                     if (var69 < var14[var66]) {
                        var14[var66] = var69;
                        var15.add(a(var69, var66));
                     }
                  }
               }
            }
         }
      }

      byte[] var42 = new byte[var11 * var12];
      Arrays.fill(var42, (byte)-1);
      int var43 = 0;

      for (int var45 = 0; var45 < var12; var45++) {
         for (int var48 = 0; var48 < var11; var48++) {
            int var51 = var45 * var11 + var48;
            if (var13[var51] == 1 && var14[var51] != Float.POSITIVE_INFINITY) {
               int var54 = a(var14, var11, var12, var48, var45);
               if (var54 >= 0) {
                  var42[var51] = (byte)var54;
                  var43++;
               }
            }
         }
      }

      if (var8 != null && var21 > 0) {
         byte var46 = 8;
         int var49 = (var11 - 1) / var46 + 2;
         int var52 = (var12 - 1) / var46 + 2;
         float[] var55 = new float[var49 * var52];

         for (int var57 = 0; var57 < var52; var57++) {
            for (int var60 = 0; var60 < var49; var60++) {
               int var64 = Math.min(var11 - 1, var60 * var46);
               int var67 = Math.min(var12 - 1, var57 * var46);
               var55[var57 * var49 + var60] = (float)var8.sample(var9 + var64 * var5, var10 + var67 * var5);
            }
         }

         double[] var58 = new double[var21];

         for (int var61 = 0; var61 < var12; var61++) {
            for (int var65 = 0; var65 < var11; var65++) {
               int var68 = var61 * var11 + var65;
               if (var42[var68] >= 0 && var20[var68] >= 0) {
                  int var70 = Math.min(var49 - 2, var65 / var46);
                  int var34 = Math.min(var52 - 2, var61 / var46);
                  double var35 = (double)(-(var55[var34 * var49 + var70 + 1] - var55[var34 * var49 + var70]));
                  double var37 = (double)(-(var55[(var34 + 1) * var49 + var70] - var55[var34 * var49 + var70]));
                  var58[var20[var68]] = var58[var20[var68]] + f[var42[var68]] * var35 + g[var42[var68]] * var37;
               }
            }
         }

         for (int var62 = 0; var62 < var11 * var12; var62++) {
            if (var42[var62] >= 0 && var20[var62] >= 0 && var58[var20[var62]] < 0.0) {
               var42[var62] = (byte)(var42[var62] + 4 & 7);
            }
         }
      }

      return new RiverFlowField(var9, var10, var5, var11, var12, var42, var16, var17, var43);
   }

   private static long a(float var0, int var1) {
      return (long)Float.floatToIntBits(var0) << 32 | (long)var1 & 4294967295L;
   }

   private static int a(float[] var0, int var1, int var2, int var3, int var4) {
      double var5 = 0.0;
      double var7 = 0.0;
      double var9 = 0.0;
      double var11 = 0.0;

      for (int var13 = -2; var13 <= 2; var13++) {
         for (int var14 = -2; var14 <= 2; var14++) {
            int var15 = var3 + var14;
            int var16 = var4 + var13;
            if (var15 >= 0 && var16 >= 0 && var15 < var1 && var16 < var2) {
               float var17 = var0[var16 * var1 + var15];
               if (var17 != Float.POSITIVE_INFINITY) {
                  var5++;
                  var7 += (double)var14;
                  var9 += (double)var13;
                  var11 += (double)var17;
               }
            }
         }
      }

      if (var5 < 2.0) {
         return -1;
      } else {
         double var45 = var7 / var5;
         double var46 = var9 / var5;
         double var47 = var11 / var5;
         double var19 = 0.0;
         double var21 = 0.0;
         double var23 = 0.0;
         double var25 = 0.0;
         double var27 = 0.0;

         for (int var29 = -2; var29 <= 2; var29++) {
            for (int var30 = -2; var30 <= 2; var30++) {
               int var31 = var3 + var30;
               int var32 = var4 + var29;
               if (var31 >= 0 && var32 >= 0 && var31 < var1 && var32 < var2) {
                  float var33 = var0[var32 * var1 + var31];
                  if (var33 != Float.POSITIVE_INFINITY) {
                     double var34 = (double)var30 - var45;
                     double var36 = (double)var29 - var46;
                     double var38 = (double)var33 - var47;
                     var19 += var34 * var34;
                     var21 += var34 * var36;
                     var23 += var36 * var36;
                     var25 += var34 * var38;
                     var27 += var36 * var38;
                  }
               }
            }
         }

         double var48 = 0.001 * (var19 + var23) + 1.0E-6;
         double var49 = var19 + var48;
         double var50 = var23 + var48;
         double var35 = var49 * var50 - var21 * var21;
         if (var35 <= 1.0E-12) {
            return -1;
         } else {
            double var37 = (var50 * var25 - var21 * var27) / var35;
            double var39 = (var49 * var27 - var21 * var25) / var35;
            double var41 = -var37;
            double var43 = -var39;
            return var41 * var41 + var43 * var43 < 1.0E-9 ? -1 : a(var41, var43);
         }
      }
   }

   private static int a(double var0, double var2) {
      int var4 = (int)Math.round(Math.atan2(var2, var0) / (Math.PI / 4));
      return (var4 % 8 + 8) % 8;
   }

   public int spacing() {
      return this.j;
   }

   public int dirAt(int var1, int var2) {
      long var3 = this.a(var1, var2);
      return var3 == 9221120239188049920L ? -1 : a(a(var3), b(var3));
   }

   public double flowRadAt(int var1, int var2) {
      long var3 = this.a(var1, var2);
      return var3 == 9221120239188049920L ? Double.NaN : Math.atan2(b(var3), a(var3));
   }

   private long a(int var1, int var2) {
      double var3 = (double)(var1 - this.h) / (double)this.j;
      double var5 = (double)(var2 - this.i) / (double)this.j;
      int var7 = (int)Math.floor(var3);
      int var8 = (int)Math.floor(var5);
      double var9 = 0.0;
      double var11 = 0.0;
      double var13 = 0.0;

      for (int var15 = -3; var15 <= 3; var15++) {
         for (int var16 = -3; var16 <= 3; var16++) {
            int var17 = var7 + var16;
            int var18 = var8 + var15;
            if (var17 >= 0 && var18 >= 0 && var17 < this.k && var18 < this.l) {
               byte var19 = this.m[var18 * this.k + var17];
               if (var19 >= 0) {
                  double var20 = (double)var17 - var3;
                  double var22 = (double)var18 - var5;
                  double var24 = 1.0 / (1.0 + var20 * var20 + var22 * var22);
                  var9 += f[var19] * var24;
                  var11 += g[var19] * var24;
                  var13 += var24;
               }
            }
         }
      }

      return var13 <= 0.0 ? 9221120239188049920L : (long)Float.floatToRawIntBits((float)var9) << 32 | (long)Float.floatToRawIntBits((float)var11) & 4294967295L;
   }

   private static double a(long var0) {
      return (double)Float.intBitsToFloat((int)(var0 >>> 32));
   }

   private static double b(long var0) {
      return (double)Float.intBitsToFloat((int)var0);
   }

   static {
      for (int var0 = 0; var0 < 8; var0++) {
         f[var0] = Math.cos((double)var0 * (Math.PI / 4));
         g[var0] = Math.sin((double)var0 * (Math.PI / 4));
      }
   }

   public interface BiomeClass {
      int classify(int var1, int var2);
   }

   public interface OrientationField {
      double sample(int var1, int var2);
   }
}
