package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.math.Hashing;

public final class Lake {
   private final int a;
   private final int b;
   private final int c;
   private final int d;
   private final int e;
   private final int f;
   private final int g;
   private final long[] h;
   private static final double i = 4.0;
   private static final int j = 16;

   public Lake(int var1, int var2, int var3, int var4, int var5, int var6, int var7, long[] var8) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      this.e = var5;
      this.f = var6;
      this.g = var7;
      this.h = var8;
   }

   public int level() {
      return this.a;
   }

   public int digDepth() {
      return this.b;
   }

   public boolean covers(int var1, int var2) {
      int var3 = Math.floorDiv(var1 - this.d + (this.c >> 1), this.c);
      int var4 = Math.floorDiv(var2 - this.e + (this.c >> 1), this.c);
      if (var3 >= 0 && var3 < this.f && var4 >= 0 && var4 < this.g) {
         int var5 = var4 * this.f + var3;
         return (this.h[var5 >>> 6] & 1L << (var5 & 63)) != 0L;
      } else {
         return false;
      }
   }

   public boolean coversWithin(int var1, int var2, int var3) {
      int var4 = Math.floorDiv(var1 - this.d + (this.c >> 1), this.c);
      int var5 = Math.floorDiv(var2 - this.e + (this.c >> 1), this.c);

      for (int var6 = Math.max(0, var5 - var3); var6 <= Math.min(this.g - 1, var5 + var3); var6++) {
         for (int var7 = Math.max(0, var4 - var3); var7 <= Math.min(this.f - 1, var4 + var3); var7++) {
            int var8 = var6 * this.f + var7;
            if ((this.h[var8 >>> 6] & 1L << (var8 & 63)) != 0L) {
               return true;
            }
         }
      }

      return false;
   }

   public double coverageSmooth(int var1, int var2) {
      long var3 = (long)this.d * 341873128712L ^ (long)this.e * 132897987541L;
      double var5 = (double)var1 + (a(var3, var1, var2, 3) - 0.5) * 2.0 * 4.0;
      double var7 = (double)var2 + (a(var3, var1, var2, 4) - 0.5) * 2.0 * 4.0;
      double var9 = (var5 - (double)this.d) / (double)this.c;
      double var11 = (var7 - (double)this.e) / (double)this.c;
      int var13 = (int)Math.floor(var9);
      int var14 = (int)Math.floor(var11);
      double var15 = var9 - (double)var13;
      double var17 = var11 - (double)var14;
      double var19 = this.a(var13, var14);
      double var21 = this.a(var13 + 1, var14);
      double var23 = this.a(var13, var14 + 1);
      double var25 = this.a(var13 + 1, var14 + 1);
      double var27 = var19 * (1.0 - var15) + var21 * var15;
      double var29 = var23 * (1.0 - var15) + var25 * var15;
      return var27 * (1.0 - var17) + var29 * var17;
   }

   private static double a(long var0, int var2, int var3, int var4) {
      int var5 = Math.floorDiv(var2, 16);
      int var6 = Math.floorDiv(var3, 16);
      double var7 = ((double)var2 - (double)var5 * 16.0) / 16.0;
      double var9 = ((double)var3 - (double)var6 * 16.0) / 16.0;
      var7 = var7 * var7 * (3.0 - 2.0 * var7);
      var9 = var9 * var9 * (3.0 - 2.0 * var9);
      double var11 = Hashing.unit(var0, (long)var5, (long)var6, (long)var4);
      double var13 = Hashing.unit(var0, (long)(var5 + 1), (long)var6, (long)var4);
      double var15 = Hashing.unit(var0, (long)var5, (long)(var6 + 1), (long)var4);
      double var17 = Hashing.unit(var0, (long)(var5 + 1), (long)(var6 + 1), (long)var4);
      double var19 = var11 + (var13 - var11) * var7;
      double var21 = var15 + (var17 - var15) * var7;
      return var19 + (var21 - var19) * var9;
   }

   private double a(int var1, int var2) {
      if (var1 >= 0 && var1 < this.f && var2 >= 0 && var2 < this.g) {
         int var3 = var2 * this.f + var1;
         return (this.h[var3 >>> 6] & 1L << (var3 & 63)) != 0L ? 1.0 : 0.0;
      } else {
         return 0.0;
      }
   }

   public int spacing() {
      return this.c;
   }

   public int cellMinX() {
      return this.d;
   }

   public int cellMinZ() {
      return this.e;
   }

   public int cellsWide() {
      return this.f;
   }

   public int cellsHigh() {
      return this.g;
   }

   public long[] mask() {
      return this.h;
   }
}
