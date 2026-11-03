package dev.streamsreflowing.core.grid;

import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class LowResGrid {
   public final int nx;
   public final int nz;
   public final int spacing;
   public final int originX;
   public final int originZ;
   private final double[] a;
   private final boolean[] b;
   private static final int c = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
   private static final AtomicLong d = new AtomicLong();
   private static final ThreadPoolExecutor e = new ThreadPoolExecutor(c, c, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), var0 -> {
      Thread var1 = new Thread(var0, "streamsreflowing-sampler");
      var1.setDaemon(true);
      var1.setPriority(1);
      return var1;
   });
   private static final int f = 4096;
   private static final AtomicInteger g = new AtomicInteger();
   private static volatile int[] h = new int[0];
   private static final ThreadLocal<long[]> i = ThreadLocal.withInitial(() -> new long[]{Long.MAX_VALUE});
   private static final double j = 8.0;
   private static final double k = 1.0;
   private long[] l;

   public static void setBackgroundThrottle(int var0) {
      if (g.get() > 0) {
         a(c);
      } else {
         a(var0 >= 4 ? c : (var0 == 3 ? Math.max(2, c * 3 / 4) : (var0 == 2 ? Math.max(2, c / 3) : (var0 == 1 ? Math.max(2, c / 5) : 1))));
      }
   }

   public static void noteUrgentWaiter() {
      if (!Thread.currentThread().getName().startsWith("streamsreflowing-prefetch")) {
         g.incrementAndGet();
         a(c);
      }
   }

   public static void urgentWaiterDone() {
      g.decrementAndGet();
   }

   public static int urgentWaiterCount() {
      return g.get();
   }

   public static void setFocusPoints(int[] var0) {
      h = var0 != null ? var0 : new int[0];
   }

   private static long a(int var0, int var1) {
      int[] var2 = h;
      long var3 = Long.MAX_VALUE;

      for (byte var5 = 0; var5 + 1 < var2.length; var5 += 2) {
         long var6 = (long)(var2[var5] - var0);
         long var8 = (long)(var2[var5 + 1] - var1);
         long var10 = var6 * var6 + var8 * var8;
         if (var10 < var3) {
            var3 = var10;
         }
      }

      return var3;
   }

   public static void enterBuildFocus(int var0, int var1) {
      i.get()[0] = a(var0, var1);
   }

   public static void exitBuildFocus() {
      i.get()[0] = Long.MAX_VALUE;
   }

   public static int samplerWidth() {
      return e.getCorePoolSize();
   }

   public static int samplerFullWidth() {
      return c;
   }

   private static void a(int var0) {
      if (e.getCorePoolSize() != var0 || e.getMaximumPoolSize() != var0) {
         if (var0 >= e.getMaximumPoolSize()) {
            e.setMaximumPoolSize(var0);
            e.setCorePoolSize(var0);
         } else {
            e.setCorePoolSize(var0);
            e.setMaximumPoolSize(var0);
         }
      }
   }

   private static int a() {
      return Thread.currentThread().getName().startsWith("streamsreflowing-prefetch") ? 1 : 0;
   }

   public LowResGrid(PlateDomain var1, TerrainSampler var2, StreamsConfig var3) {
      this.spacing = var3.gridSpacing();
      this.originX = var1.minX;
      this.originZ = var1.minZ;
      this.nx = Math.max(1, (var1.maxX - var1.minX) / this.spacing + 1);
      this.nz = Math.max(1, (var1.maxZ - var1.minZ) / this.spacing + 1);
      this.a = new double[this.nx * this.nz];
      this.b = new boolean[this.nx * this.nz];
      int var4 = Math.max(this.spacing, var3.terrainSampleSpacing());
      if (var4 > this.spacing) {
         this.a(var1, var2, var3, var4);
      } else if (var2.batchCapable()) {
         this.a(var1, var2, var3);
      } else if (this.nx * this.nz >= 4096) {
         this.b(var1, var2, var3);
      } else {
         this.a(0, this.nz, var1, var2, var3);
      }
   }

   private void a(PlateDomain var1, TerrainSampler var2, StreamsConfig var3) {
      int[] var4 = new int[this.nx * this.nz];
      int[] var5 = new int[this.nx * this.nz];
      int[] var6 = new int[this.nx * this.nz];
      int var7 = 0;

      for (int var8 = 0; var8 < this.nz; var8++) {
         for (int var9 = 0; var9 < this.nx; var9++) {
            int var10 = this.worldX(var9);
            int var11 = this.worldZ(var8);
            int var12 = this.idx(var9, var8);
            boolean var13 = var1.contains((double)var10, (double)var11) && var1.borderDistance((double)var10, (double)var11) >= var3.borderMargin();
            this.b[var12] = var13;
            if (var13) {
               var4[var7] = var10;
               var5[var7] = var11;
               var6[var7] = var12;
               var7++;
            }
         }
      }

      double[] var14 = new double[var7];
      var2.heightsBatch(var4, var5, var14, var7);

      for (int var15 = 0; var15 < var7; var15++) {
         this.a[var6[var15]] = var14[var15];
      }
   }

   private void a(int var1, int var2, PlateDomain var3, TerrainSampler var4, StreamsConfig var5) {
      for (int var6 = var1; var6 < var2; var6++) {
         for (int var7 = 0; var7 < this.nx; var7++) {
            int var8 = this.worldX(var7);
            int var9 = this.worldZ(var6);
            int var10 = this.idx(var7, var6);
            boolean var11 = var3.contains((double)var8, (double)var9) && var3.borderDistance((double)var8, (double)var9) >= var5.borderMargin();
            this.b[var10] = var11;
            if (var11) {
               this.a[var10] = var4.height(var8, var9);
            }
         }
      }
   }

   private void b(PlateDomain var1, TerrainSampler var2, StreamsConfig var3) {
      a(this.nz, (var4, var5) -> this.a(var4, var5, var1, var2, var3));
   }

   private void a(PlateDomain var1, TerrainSampler var2, StreamsConfig var3, int var4) {
      int var5 = Math.floorDiv(this.originX, var4) * var4;
      int var6 = Math.floorDiv(this.originZ, var4) * var4;
      int var7 = (Math.floorDiv(this.worldX(this.nx - 1), var4) * var4 + var4 - var5) / var4 + 1;
      int var8 = (Math.floorDiv(this.worldZ(this.nz - 1), var4) * var4 + var4 - var6) / var4 + 1;
      double[] var9 = new double[var7 * var8];
      boolean[] var10 = new boolean[var7 * var8];
      a(this.nz, (var8x, var9x) -> {
         for (int var10x = var8x; var10x < var9x; var10x++) {
            for (int var11x = 0; var11x < this.nx; var11x++) {
               int var12x = this.worldX(var11x);
               int var13x = this.worldZ(var10x);
               boolean var14x = var1.contains((double)var12x, (double)var13x) && var1.borderDistance((double)var12x, (double)var13x) >= var3.borderMargin();
               this.b[this.idx(var11x, var10x)] = var14x;
               if (var14x) {
                  int var15x = (Math.floorDiv(var12x, var4) * var4 - var5) / var4;
                  int var16x = (Math.floorDiv(var13x, var4) * var4 - var6) / var4;
                  int var17x = var15x + var16x * var7;
                  var10[var17x] = true;
                  var10[var17x + 1] = true;
                  var10[var17x + var7] = true;
                  var10[var17x + var7 + 1] = true;
               }
            }
         }
      });
      if (var2.useHybridGridFill()) {
         this.a(var2, var3, var4, var5, var6, var7, var8, var9, var10);
      } else if (var2.batchCapable()) {
         int[] var11 = new int[var7 * var8];
         int[] var12 = new int[var7 * var8];
         int[] var13 = new int[var7 * var8];
         int var14 = 0;

         for (int var15 = 0; var15 < var8; var15++) {
            int var16 = var6 + var15 * var4;

            for (int var17 = 0; var17 < var7; var17++) {
               int var18 = var17 + var15 * var7;
               if (var10[var18]) {
                  var11[var14] = var5 + var17 * var4;
                  var12[var14] = var16;
                  var13[var14] = var18;
                  var14++;
               }
            }
         }

         double[] var19 = new double[var14];
         var2.heightsBatch(var11, var12, var19, var14);

         for (int var20 = 0; var20 < var14; var20++) {
            var9[var13[var20]] = var19[var20];
         }
      } else {
         a(var8, (var7x, var8x) -> {
            for (int var9x = var7x; var9x < var8x; var9x++) {
               int var10x = var6 + var9x * var4;

               for (int var11x = 0; var11x < var7; var11x++) {
                  int var12x = var11x + var9x * var7;
                  if (var10[var12x]) {
                     var9[var12x] = var2.height(var5 + var11x * var4, var10x);
                  }
               }
            }
         });
      }

      a(this.nz, (var6x, var7x) -> {
         for (int var8x = var6x; var8x < var7x; var8x++) {
            for (int var9x = 0; var9x < this.nx; var9x++) {
               int var10x = this.idx(var9x, var8x);
               if (this.b[var10x]) {
                  this.a[var10x] = a(this.worldX(var9x), this.worldZ(var8x), var4, var9, var7, var5, var6);
               }
            }
         }
      });
   }

   private void a(TerrainSampler var1, StreamsConfig var2, int var3, int var4, int var5, int var6, int var7, double[] var8, boolean[] var9) {
      int var10 = Math.floorDiv(var4, var3);
      int var11 = Math.floorDiv(var5, var3);
      boolean[] var12 = new boolean[var6 * var7];

      for (int var13 = 0; var13 < var7; var13++) {
         boolean var14 = (var11 + var13 & 1) == 0;

         for (int var15 = 0; var15 < var6; var15++) {
            int var16 = var15 + var13 * var6;
            if (var9[var16]) {
               if (var14 && (var10 + var15 & 1) == 0) {
                  var12[var16] = true;
               } else {
                  int var17 = (var10 + var15 & 1) == 0 ? var15 : var15 - 1;
                  int var18 = var14 ? var13 : var13 - 1;

                  for (byte var19 = 0; var19 <= 2; var19 += 2) {
                     for (byte var20 = 0; var20 <= 2; var20 += 2) {
                        int var21 = var17 + var20;
                        int var22 = var18 + var19;
                        if (var21 >= 0 && var21 < var6 && var22 >= 0 && var22 < var7) {
                           var12[var21 + var22 * var6] = true;
                        }
                     }
                  }
               }
            }
         }
      }

      a(var7, (var7x, var8x) -> {
         for (int var9x = var7x; var9x < var8x; var9x++) {
            int var10x = var5 + var9x * var3;

            for (int var11x = 0; var11x < var6; var11x++) {
               int var12x = var11x + var9x * var6;
               if (var12[var12x]) {
                  var8[var12x] = var1.height(var4 + var11x * var3, var10x);
               }
            }
         }
      });
      double[] var23 = new double[var6 * var7];
      a(var7, (var8x, var9x) -> {
         for (int var10x = var8x; var10x < var9x; var10x++) {
            int var11x = var5 + var10x * var3;

            for (int var12x = 0; var12x < var6; var12x++) {
               int var13x = var12x + var10x * var6;
               if (var9[var13x] || var12[var13x]) {
                  var23[var13x] = var1.estimate(var4 + var12x * var3, var11x);
               }
            }
         }
      });
      double var24 = (double)var2.seaLevel() + 8.0;
      a(var7, (var14x, var15x) -> {
         for (int var16x = var14x; var16x < var15x; var16x++) {
            boolean var17x = (var11 + var16x & 1) == 0;
            int var18x = var5 + var16x * var3;

            for (int var19x = 0; var19x < var6; var19x++) {
               int var20x = var19x + var16x * var6;
               if (var9[var20x] && !var12[var20x]) {
                  int var21x = var4 + var19x * var3;
                  int var22x = (var10 + var19x & 1) == 0 ? var19x : var19x - 1;
                  int var23x = var17x ? var16x : var16x - 1;
                  var8[var20x] = a(var1, var8, var23, var6, var7, var22x, var23x, var19x, var16x, var21x, var18x, var24);
               }
            }
         }
      });
   }

   private static double a(
      TerrainSampler var0, double[] var1, double[] var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, double var11
   ) {
      if (var5 >= 0 && var6 >= 0 && var5 + 2 < var3 && var6 + 2 < var4) {
         int var13 = var5 + var6 * var3;
         int var14 = var13 + 2;
         int var15 = var13 + 2 * var3;
         int var16 = var15 + 2;
         double var17 = var1[var13];
         double var19 = var1[var14];
         double var21 = var1[var15];
         double var23 = var1[var16];
         if (var17 != -1000.0 && var19 != -1000.0 && var21 != -1000.0 && var23 != -1000.0) {
            double var25 = (double)(var7 - var5) / 2.0;
            double var27 = (double)(var8 - var6) / 2.0;
            double var29 = var2[var13];
            double var31 = var2[var14];
            double var33 = var2[var15];
            double var35 = var2[var16];
            double var37 = var2[var7 + var8 * var3];
            double var39 = Math.min(Math.min(var29, var31), Math.min(var33, var35));
            double var41 = Math.max(Math.max(var29, var31), Math.max(var33, var35));
            if (!(var37 > var41 + 1.0) && !(var37 < var39 - 1.0)) {
               double var43 = a(var17, var19, var21, var23, var25, var27);
               double var45 = a(var29, var31, var33, var35, var25, var27);
               double var47 = var43 + (var37 - var45);
               return !(var47 <= var11) && !(var43 <= var11) ? var47 : var0.height(var9, var10);
            } else {
               return var0.height(var9, var10);
            }
         } else {
            return var0.height(var9, var10);
         }
      } else {
         return var0.height(var9, var10);
      }
   }

   private static double a(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = var0 + (var2 - var0) * var8;
      double var14 = var4 + (var6 - var4) * var8;
      return var12 + (var14 - var12) * var10;
   }

   private static double a(int var0, int var1, int var2, double[] var3, int var4, int var5, int var6) {
      int var7 = Math.floorDiv(var0, var2) * var2;
      int var8 = Math.floorDiv(var1, var2) * var2;
      double var9 = (double)(var0 - var7) / (double)var2;
      double var11 = (double)(var1 - var8) / (double)var2;
      int var13 = (var7 - var5) / var2;
      int var14 = (var8 - var6) / var2;
      int var15 = var13 + var14 * var4;
      double var16 = var3[var15];
      double var18 = var3[var15 + 1];
      double var20 = var3[var15 + var4];
      double var22 = var3[var15 + var4 + 1];
      double var24 = var16 + (var18 - var16) * var9;
      double var26 = var20 + (var22 - var20) * var9;
      return var24 + (var26 - var24) * var11;
   }

   private static void a(int var0, LowResGrid.RowBand var1) {
      if (var0 > 0) {
         if (Thread.currentThread().getName().startsWith("streamsreflowing-sampler")) {
            var1.run(0, var0);
         } else {
            int var2 = Math.min(c, var0);
            if (var2 <= 1) {
               var1.run(0, var0);
            } else {
               int var3 = (var0 + var2 - 1) / var2;
               ArrayList var4 = new ArrayList(var2);

               for (int var5 = 0; var5 < var2; var5++) {
                  int var6 = var5 * var3;
                  int var7 = Math.min(var0, var6 + var3);
                  if (var6 < var7) {
                     var4.add(new int[]{var6, var7});
                  }
               }

               if (var4.size() <= 1) {
                  var1.run(0, var0);
               } else {
                  int var16 = a();
                  long var17 = i.get()[0];
                  if (var16 == 0) {
                     a(c);
                  }

                  CountDownLatch var8 = new CountDownLatch(var4.size());
                  AtomicReference var9 = new AtomicReference();

                  try {
                     for (int[] var11 : var4) {
                        int var12 = var11[0];
                        int var13 = var11[1];
                        e.execute(new LowResGrid.PrioritizedTask(var16, var17, d.getAndIncrement(), () -> {
                           try {
                              var1.run(var12, var13);
                           } catch (Throwable var9x) {
                              var9.compareAndSet(null, var9x);
                           } finally {
                              var8.countDown();
                           }
                        }));
                     }
                  } catch (RejectedExecutionException var15) {
                     var1.run(0, var0);
                     return;
                  }

                  try {
                     var8.await();
                  } catch (InterruptedException var14) {
                     Thread.currentThread().interrupt();
                     throw new RuntimeException("interrupted during parallel terrain sampling", var14);
                  }

                  Throwable var18 = (Throwable)var9.get();
                  if (var18 != null) {
                     throw new RuntimeException("parallel terrain sampling failed", var18);
                  }
               }
            }
         }
      }
   }

   public static void parallelFor(int var0, IntConsumer var1) {
      a(var0, (var1x, var2) -> {
         for (int var3 = var1x; var3 < var2; var3++) {
            var1.accept(var3);
         }
      });
   }

   public int worldX(int var1) {
      return this.originX + var1 * this.spacing;
   }

   public int worldZ(int var1) {
      return this.originZ + var1 * this.spacing;
   }

   public int idx(int var1, int var2) {
      return var1 + var2 * this.nx;
   }

   public int gi(int var1) {
      return var1 % this.nx;
   }

   public int gj(int var1) {
      return var1 / this.nx;
   }

   public boolean valid(int var1, int var2) {
      return var1 >= 0 && var1 < this.nx && var2 >= 0 && var2 < this.nz;
   }

   public double height(int var1) {
      return this.a[var1];
   }

   public void overrideHeight(int var1, double var2) {
      this.a[var1] = var2;
   }

   public void markRiverOutlet(int var1) {
      if (this.l == null) {
         this.l = new long[this.size() + 63 >>> 6];
      }

      this.l[var1 >>> 6] = this.l[var1 >>> 6] | 1L << (var1 & 63);
   }

   public boolean riverOutlet(int var1) {
      return this.l != null && (this.l[var1 >>> 6] & 1L << (var1 & 63)) != 0L;
   }

   public boolean inDomain(int var1) {
      return this.b[var1];
   }

   public int size() {
      return this.nx * this.nz;
   }

   private static final class PrioritizedTask implements Comparable<LowResGrid.PrioritizedTask>, Runnable {
      private final int a;
      private final long b;
      private final long c;
      private final Runnable d;

      PrioritizedTask(int var1, long var2, long var4, Runnable var6) {
         this.a = var1;
         this.b = var2;
         this.c = var4;
         this.d = var6;
      }

      @Override
      public void run() {
         this.d.run();
      }

      public int compareTo(LowResGrid.PrioritizedTask var1) {
         int var2 = Integer.compare(this.a, var1.a);
         if (var2 != 0) {
            return var2;
         } else {
            var2 = Long.compare(this.b, var1.b);
            return var2 != 0 ? var2 : Long.compare(this.c, var1.c);
         }
      }
   }

   @FunctionalInterface
   private interface RowBand {
      void run(int var1, int var2);
   }
}
