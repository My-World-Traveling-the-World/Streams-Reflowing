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
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
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
   private static final int[] I = a();
   private static final int J = 96;
   private static final int[] K = b();
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
   private static final double al = (double)Integer.getInteger("streamsreflowing.lakeminarea", 1024).intValue();
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
   private static final long aJ = 4611686018427387904L;

   private NetworkTracer() {
   }

   public static RiverNetwork build(PlateDomain var0, TerrainSampler var1, WetnessSampler var2, StreamsConfig var3, long var4) {
      long var6 = System.nanoTime();
      LowResGrid var8 = new LowResGrid(var0, var1, var3);
      long var9 = System.nanoTime();
      int var11 = var8.size();
      int var12 = var3.seaLevel();
      boolean[] var13 = new boolean[var11];
      ArrayDeque var14 = new ArrayDeque();

      for (int var15 = 0; var15 < var11; var15++) {
         if (var8.inDomain(var15) && var8.height(var15) != -1000.0) {
            int var16 = var1.waterBiomeClass(Math.round((float)var8.worldX(var8.gi(var15))), Math.round((float)var8.worldZ(var8.gj(var15))));
            if (var16 == 1) {
               var8.overrideHeight(var15, -1001.0);
               var8.markRiverOutlet(var15);
               var13[var15] = true;
               var14.add(var15);
            } else if (var16 == 2) {
               var8.overrideHeight(var15, -1000.0);
               var13[var15] = true;
            }
         }
      }

      if (Boolean.getBoolean("streamsreflowing.caldiag")) {
         int var134 = 0;

         for (int var139 = 0; var139 < var11; var139++) {
            if (var8.riverOutlet(var139)) {
               var134++;
            }
         }

         Constants.LOG.info("[SR-RTF] plate {}: {} river-outlet cells after tier 1", var0.key(), var134);
      }

      if (!Boolean.getBoolean("streamsreflowing.nohighrivers")) {
         for (int var135 = 0; var135 < var11; var135++) {
            if (!var13[var135] && var8.inDomain(var135) && !(var8.height(var135) <= (double)(var12 + 8))) {
               int var140 = 0;
               int var17 = var8.gi(var135);
               int var18 = var8.gj(var135);

               for (int var19 = 0; var19 < 8; var19++) {
                  int var20 = var17 + a[var19];
                  int var21 = var18 + b[var19];
                  if (var8.valid(var20, var21) && var8.inDomain(var8.idx(var20, var21)) && var8.height(var8.idx(var20, var21)) > var8.height(var135)) {
                     var140++;
                  }
               }

               if (var140 >= 5) {
                  var13[var135] = true;
                  if (var1.waterBiomeClassAt(Math.round((float)var8.worldX(var17)), (int)Math.round(var8.height(var135)), Math.round((float)var8.worldZ(var18)))
                     == 1) {
                     var8.overrideHeight(var135, -1001.0);
                     var8.markRiverOutlet(var135);
                     var14.add(var135);
                  }
               }
            }
         }

         int var136 = 0;

         while (!var14.isEmpty()) {
            int var141 = (Integer)var14.poll();
            int var145 = var8.gi(var141);
            int var149 = var8.gj(var141);

            for (int var152 = 0; var152 < 8; var152++) {
               int var155 = var145 + a[var152];
               int var157 = var149 + b[var152];
               if (var8.valid(var155, var157)) {
                  int var22 = var8.idx(var155, var157);
                  if (!var13[var22] && var8.inDomain(var22) && var8.height(var22) != -1000.0) {
                     var13[var22] = true;
                     if (var1.waterBiomeClassAt(
                           Math.round((float)var8.worldX(var155)), (int)Math.round(var8.height(var22)), Math.round((float)var8.worldZ(var157))
                        )
                        == 1) {
                        var8.overrideHeight(var22, -1001.0);
                        var8.markRiverOutlet(var22);
                        var14.add(var22);
                        var136++;
                     }
                  }
               }
            }
         }

         if (var136 > 0) {
            Constants.LOG.debug("[SR-MERGE] high-river outlets: {} extra river cells found above the sea-level probe", var136);
         }
      }

      if (O) {
         int var137 = Math.max(1, var11 / 64);
         int var142 = 0;
         double var146 = 0.0;
         double var153 = 0.0;
         int var158 = 0;
         int var164 = 0;
         int var23 = 0;

         for (int var24 = 0; var24 < var11; var24 += var137) {
            if (var8.inDomain(var24)) {
               int var25 = var8.worldX(var8.gi(var24));
               int var26 = var8.worldZ(var8.gj(var24));
               double var27 = var8.height(var24);
               if (var27 != -1000.0 && var27 != -1001.0) {
                  double var29 = var1.groundHeightExact(var25, var26);
                  if (!Double.isNaN(var29)) {
                     double var31 = var27 - var29;
                     var142++;
                     var146 += Math.abs(var31);
                     if (Math.abs(var31) > 2.0) {
                        var23++;
                     }

                     if (Math.abs(var31) > Math.abs(var153)) {
                        var153 = var31;
                        var158 = var25;
                        var164 = var26;
                     }
                  }
               }
            }
         }

         if (var142 > 0) {
            Constants.LOG
               .info(
                  "[SR-SAMPLEAUDIT] plate {}: sampled-vs-exact over {} cells -- meanAbs {}, >2 blocks {} ({}%), worst {} at ({},{}); sampler baked={} hasEstimate={}",
                  var0.key(),
                  var142,
                  String.format(Locale.ROOT, "%.2f", var146 / (double)var142),
                  var23,
                  String.format(Locale.ROOT, "%.0f", 100.0 * (double)var23 / (double)var142),
                  String.format(Locale.ROOT, "%+.1f", var153),
                  var158,
                  var164,
                  var1.bakedCalibration(),
                  var1.hasEstimate()
               );
         }
      }

      ArrayList var138 = new ArrayList();

      for (int var143 = 0; var143 < var8.nz; var143++) {
         for (int var147 = 0; var147 < var8.nx; var147++) {
            int var150 = var8.idx(var147, var143);
            if (var8.inDomain(var150)) {
               var138.add(var150);
            }
         }
      }

      double[] var144 = new double[var11];
      boolean[] var148 = new boolean[var11];
      PriorityQueue var151 = new PriorityQueue<>((var1x, var2x) -> Double.compare(var144[var1x], var144[var2x]));
      boolean[] var154 = new boolean[var11];
      ArrayList var156 = new ArrayList();

      for (int var165 : var138) {
         if (var8.riverOutlet(var165)) {
            int var169 = var8.gi(var165);
            int var173 = var8.gj(var165);

            for (int var177 = 0; var177 < 8; var177++) {
               int var181 = var169 + a[var177];
               int var184 = var173 + b[var177];
               if (var8.valid(var181, var184)) {
                  int var28 = var8.idx(var181, var184);
                  if (var8.inDomain(var28) && !var8.riverOutlet(var28)) {
                     var154[var165] = true;
                     var156.add(var165);
                     break;
                  }
               }
            }
         }
      }

      if (!var156.isEmpty()) {
         double[] var160 = new double[var156.size()];
         ArrayList var166 = var156;
         LowResGrid.parallelFor(var156.size(), var4x -> {
            int var5 = (Integer)var156.get(var4x);
            int var6x = Math.round((float)var8.worldX(var8.gi(var5)));
            int var7 = Math.round((float)var8.worldZ(var8.gj(var5)));
            double var8x = var1.riverSurface(var6x, var7);
            var160[var4x] = Double.isNaN(var8x) ? var1.surfaceHeight(var6x, var7) : var8x;
         });

         for (int var170 = 0; var170 < var166.size(); var170++) {
            var8.overrideHeight((Integer)var166.get(var170), var160[var170]);
         }

         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-RTF] plate {}: {} river bank cells seeded", var0.key(), var166.size());
         }
      }

      for (int var167 : var138) {
         var144[var167] = var8.height(var167);
         if (var8.riverOutlet(var167)) {
            var148[var167] = true;
            if (var154[var167]) {
               var151.add(var167);
            }
         } else if (var8.height(var167) <= (double)var12 || a(var8, var167)) {
            var148[var167] = true;
            var151.add(var167);
         }
      }

      while (!var151.isEmpty()) {
         int var162 = (Integer)var151.poll();
         int var168 = var8.gi(var162);
         int var171 = var8.gj(var162);

         for (int var174 = 0; var174 < 8; var174++) {
            int var178 = var168 + a[var174];
            int var182 = var171 + b[var174];
            if (var8.valid(var178, var182)) {
               int var185 = var8.idx(var178, var182);
               if (var8.inDomain(var185) && !var148[var185]) {
                  var144[var185] = Math.max(var8.height(var185), var144[var162] + 0.001);
                  var148[var185] = true;
                  var151.add(var185);
               }
            }
         }
      }

      long var163 = System.nanoTime();
      int[] var172 = new int[var11];

      for (int var179 : var138) {
         var172[var179] = -1;
         int var183 = var8.gi(var179);
         int var186 = var8.gj(var179);
         double var190 = var144[var179];
         double var30 = 0.0;
         int var32 = -1;

         for (int var33 = 0; var33 < 8; var33++) {
            int var34 = var183 + a[var33];
            int var35 = var186 + b[var33];
            if (var8.valid(var34, var35)) {
               int var36 = var8.idx(var34, var35);
               if (var8.inDomain(var36)) {
                  if (var8.riverOutlet(var36)) {
                     var32 = var36;
                     break;
                  }

                  double var37 = var190 - var144[var36];
                  if (!(var37 <= 0.0) && (!(var190 <= (double)var12 - 2.0) || !(var37 < 2.0))) {
                     double var39 = var37 / (var33 % 2 == 0 ? 1.0 : 1.4142135623730951);
                     if (var39 > var30) {
                        var30 = var39;
                        var32 = var36;
                     }
                  }
               }
            }
         }

         var172[var179] = var32;
      }

      var138.sort((var1x, var2x) -> {
         int var3x = Double.compare(var144[var2x], var144[var1x]);
         return var3x != 0 ? var3x : Integer.compare(var1x, var2x);
      });
      double[] var176 = new double[var11];
      double var180 = Math.max(0.0, var3.wetnessMultiplier());

      for (int var191 : var138) {
         var176[var191] = var180;
      }

      for (int var192 : var138) {
         if (var172[var192] >= 0) {
            var176[var172[var192]] = var176[var172[var192]] + var176[var192];
         }
      }

      double var189 = var3.minRiverAccumulation();
      if (var3.growFromWater()) {
         var189 = Math.max(1.0, var189 * var3.growDensity());
      }

      boolean[] var193 = new boolean[var11];

      for (int var196 : var138) {
         var193[var196] = var144[var196] > (double)var12 && var176[var196] >= var189 && !var8.riverOutlet(var196);
      }

      int var195 = (int)Math.round(var3.reconnectRadius() / (double)Math.max(1, var3.gridSpacing()));
      if (var195 >= 1 && !var3.growFromWater()) {
         boolean var197 = false;

         for (int var203 : var138) {
            if (var193[var203] && var172[var203] < 0 && !(var144[var203] <= (double)var12)) {
               int var208 = a(var8, var203, var144, var193, var12, var195);
               if (var208 >= 0) {
                  var172[var203] = var208;
                  var197 = true;
               }
            }
         }

         if (var197) {
            for (int var204 : var138) {
               var176[var204] = var180;
            }

            for (int var205 : var138) {
               if (var172[var205] >= 0) {
                  var176[var172[var205]] = var176[var172[var205]] + var176[var205];
               }
            }

            for (int var206 : var138) {
               var193[var206] = var144[var206] > (double)var12 && var176[var206] >= var189;
            }
         }
      }

      double var198 = var3.wetnessDryCutoff();
      double var207 = var3.wetnessWetReference();
      double var209 = var3.wetnessDryFloor();
      if (var207 > var198) {
         boolean[] var210 = new boolean[var11];

         for (int var213 : var138) {
            if (var193[var213] && var172[var213] >= 0 && var193[var172[var213]]) {
               var210[var172[var213]] = true;
            }
         }

         boolean[] var212 = new boolean[var11];
         ColumnCache var214 = new ColumnCache();

         for (int var41 : var138) {
            if (var193[var41] && !var210[var41] && !a(var41, var8, var2, var214, var198, var207, var209, var4)) {
               var212[var41] = true;
            }
         }

         for (int var219 : var138) {
            if (var212[var219] && var172[var219] >= 0 && var193[var172[var219]]) {
               var212[var172[var219]] = true;
            }
         }

         for (int var220 : var138) {
            var193[var220] = var193[var220] && var212[var220];
         }
      }

      long var211 = System.nanoTime();
      int[] var215 = new int[var11];
      Arrays.fill(var215, -1);
      ArrayList var218 = new ArrayList();
      ArrayList var221 = new ArrayList();
      double var42 = System.getProperty("streamsreflowing.lakeminarea") != null ? al : (var3.lakeMinArea() > 0.0 ? var3.lakeMinArea() : al);
      int var44 = Math.max(4, (int)Math.round(var42 / ((double)var3.gridSpacing() * (double)var3.gridSpacing())));
      boolean[] var45 = new boolean[var11];
      ArrayDeque var46 = new ArrayDeque();
      ArrayList var47 = new ArrayList();
      HashMap var48 = new HashMap();
      boolean var49 = var1.hasEstimate() && !Boolean.getBoolean("streamsreflowing.noestslack");
      double var50 = var49 ? 0.25 : 0.5;
      double var52 = var49 ? 1.0 : 0.0;
      double var54 = var49 ? Double.parseDouble(System.getProperty("streamsreflowing.lakedepthgate", "0")) : 0.0;
      int var56 = 0;
      int var57 = 0;
      int var58 = 0;
      int var59 = 0;
      int var60 = 0;
      int var61 = 0;
      int var62 = 0;
      long var63 = 0L;
      int var65 = 0;
      boolean var66 = Boolean.getBoolean("streamsreflowing.caldiag") || O;
      long[] var67 = X.get();
      Arrays.fill(var67, 0L);

      for (int var69 : var138) {
         if (!var45[var69] && a(var69, var144, var8, var12, var50)) {
            var47.clear();
            var46.clear();
            var45[var69] = true;
            var46.add(var69);
            double var70 = Double.MAX_VALUE;
            int var72 = var69;
            double var73 = Double.MAX_VALUE;
            int var75 = var69;
            double var76 = 0.0;

            while (!var46.isEmpty()) {
               int var78 = (Integer)var46.poll();
               var47.add(var78);
               if (var144[var78] < var70) {
                  var70 = var144[var78];
                  var72 = var78;
               }

               if (var8.height(var78) < var73) {
                  var73 = var8.height(var78);
                  var75 = var78;
               }

               var76 = Math.max(var76, var176[var78]);
               int var79 = var8.gi(var78);
               int var80 = var8.gj(var78);

               for (int var81 = 0; var81 < 8; var81++) {
                  int var82 = var79 + a[var81];
                  int var83 = var80 + b[var81];
                  if (var8.valid(var82, var83)) {
                     int var84 = var8.idx(var82, var83);
                     if (!var45[var84] && a(var84, var144, var8, var12, var50)) {
                        var45[var84] = true;
                        var46.add(var84);
                     }
                  }
               }
            }

            var56++;
            int var245 = var138.size() / 4;
            if (var3.lakeMaxArea() > 0.0) {
               var245 = Math.min(var245, (int)Math.round(var3.lakeMaxArea() / ((double)var3.gridSpacing() * (double)var3.gridSpacing())));
            }

            if (var47.size() > var245) {
               var57++;
            } else {
               double var251 = var70 - var73;
               boolean var264 = var76 >= var189 * 1.5;
               double var273 = (var264 ? 2.0 : 3.0) - var52;
               int var294 = var264 ? Math.max(2, var44 / 2) : var44;
               if (!(var251 < var273) && var47.size() >= var294 && !(var76 < var189)) {
                  if (!var264 && var251 < var54) {
                     var60++;
                  } else {
                     double var85 = Math.sqrt((double)var47.size()) * (double)var3.gridSpacing();
                     if (var73 < (double)var3.seaLevel() && var251 > 0.5 * var85) {
                        var61++;
                        if (var66) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEREJ] plate {} shaft-like: floor={} sea={} depth={} width={} cells={}",
                                 var0.key(),
                                 String.format(Locale.ROOT, "%.1f", var73),
                                 var3.seaLevel(),
                                 String.format(Locale.ROOT, "%.1f", var251),
                                 String.format(Locale.ROOT, "%.0f", var85),
                                 var47.size()
                              );
                        }
                     } else {
                        var58++;
                        double var87 = var76 * (double)var3.gridSpacing() * (double)var3.gridSpacing();
                        double var89 = 3.0 + 4.0 * Math.sqrt(var87 / 2500.0);
                        boolean var91 = var89 >= var251 - 1.0;
                        int var92;
                        if (var3.growFromWater()) {
                           var92 = Math.min((int)Math.floor(var70) - (var49 ? 2 : 1), (int)Math.floor(var73 + var89));
                           var91 = false;
                        } else {
                           var92 = (int)Math.floor(var73 + Math.min(var251 - 1.0, var89));
                        }

                        double var93 = a(var47, var8, var1);
                        if (var3.growFromWater()) {
                           if (!Double.isNaN(var93)) {
                              continue;
                           }
                        } else if (!Double.isNaN(var93)) {
                           int var95 = (int)Math.floor(var93) - 1;
                           if (var95 < var92) {
                              var92 = var95;
                              var91 = false;
                           }
                        }

                        int var388 = var8.worldX(var8.gi(var72));
                        int var96 = var8.worldZ(var8.gj(var72));
                        double var97 = var2.wetness(var388, var92, var96);
                        double var99 = Math.max(
                           var3.wetnessDryFloor(),
                           Math.min(1.0, (var97 - var3.wetnessDryCutoff()) / Math.max(1.0E-6, var3.wetnessWetReference() - var3.wetnessDryCutoff()))
                        );
                        double var101 = 1.0 - var99;
                        int var103 = 0;
                        if (var101 > 0.0) {
                           double var104 = Hashing.unit(var4, (long)var388, (long)var96, 107749L);
                           var103 = (int)Math.round(8.0 * var101 * var104);
                           var92 -= var103;
                        }

                        if (var92 > (int)Math.floor(var73) || var49 && var3.growFromWater()) {
                           if (!var49 || !var3.growFromWater()) {
                              var92 = Math.max((int)Math.floor(var73) + 1, var92 - 2);
                           }

                           if (!var264) {
                              int var441 = Math.floorDiv(var92, as);
                              int var105 = var48.getOrDefault(var441, 0);
                              double var106 = Math.min(1.0, (var251 - var273) / 5.0);
                              double var108 = at / (at + (double)var105);
                              double var110 = var106 + (1.0 - var106) * var108;
                              double var112 = Hashing.unit(var4, (long)var8.worldX(var8.gi(var72)), (long)var8.worldZ(var8.gj(var72)), (long)var441);
                              if (var112 >= var110) {
                                 var59++;
                                 continue;
                              }

                              var48.put(var441, var105 + 1);
                           }

                           int var442 = var218.size();
                           if (var3.growFromWater()) {
                              Object var448 = new ArrayList(var47);
                              boolean[] var454 = new boolean[var11];

                              for (int var462 : var47) {
                                 var454[var462] = true;
                              }

                              ArrayDeque var458 = new ArrayDeque(var47);

                              while (!var458.isEmpty()) {
                                 int var463 = (Integer)var458.poll();
                                 int var109 = var8.gi(var463);
                                 int var468 = var8.gj(var463);

                                 for (int var111 = 0; var111 < 8; var111++) {
                                    int var477 = var109 + a[var111];
                                    int var113 = var468 + b[var111];
                                    if (var8.valid(var477, var113)) {
                                       int var114 = var8.idx(var477, var113);
                                       if (var8.inDomain(var114) && var144[var114] - var8.height(var114) > 0.001 && !var454[var114]) {
                                          var454[var114] = true;
                                          var448.add(var114);
                                          var458.add(var114);
                                       }
                                    }
                                 }
                              }

                              long var464 = System.nanoTime();

                              try {
                                 if (var49 && !Boolean.getBoolean("streamsreflowing.nolakeborderpre")) {
                                    double var469 = var448.size() >= 64
                                       ? var3.borderMargin() + 3.0 * (double)var8.spacing
                                       : var3.borderMargin() + (double)(var8.spacing >> 1) + 15.0;
                                    boolean var478 = false;

                                    for (int var487 : var448) {
                                       if (var0.borderDistance((double)var8.worldX(var8.gi(var487)), (double)var8.worldZ(var8.gj(var487))) < var469) {
                                          var478 = true;
                                          break;
                                       }
                                    }

                                    if (var478) {
                                       var65++;
                                       if (var66) {
                                          Constants.LOG
                                             .info(
                                                "[SR-LAKEREJ] plate {} at {},{}: border-pre fillCells={} reach={}",
                                                var0.key(),
                                                var8.worldX(var8.gi(var72)),
                                                var8.worldZ(var8.gj(var72)),
                                                var448.size(),
                                                String.format("%.0f", var469)
                                             );
                                       }
                                       continue;
                                    }
                                 }

                                 if (var49) {
                                    int var470 = 0;
                                    int var474 = 0;

                                    for (int var485 : var448) {
                                       int var488 = var8.gi(var485);
                                       int var115 = var8.gj(var485);

                                       for (int var116 = 0; var116 < 8; var116++) {
                                          int var117 = var488 + a[var116];
                                          int var118 = var115 + b[var116];
                                          if (var8.valid(var117, var118) && !var454[var8.idx(var117, var118)]) {
                                             var470++;
                                             if (var8.height(var8.idx(var117, var118)) - (double)var92 < 2.5) {
                                                var474++;
                                             }
                                          }
                                       }
                                    }

                                    if (var470 > 0 && var474 * 2 > var470) {
                                       var65++;
                                       if (var66) {
                                          Constants.LOG
                                             .info(
                                                "[SR-LAKEREJ] plate {} at {},{}: stage0 margLow={}/{} level={} fillCells={}",
                                                var0.key(),
                                                var8.worldX(var8.gi(var72)),
                                                var8.worldZ(var8.gj(var72)),
                                                var474,
                                                var470,
                                                var92,
                                                var448.size()
                                             );
                                       }
                                       continue;
                                    }

                                    int var480 = Math.min(24, Math.max(8, var448.size() / 16));
                                    int var486 = Math.max(3, var480 / 3);
                                    int var489 = 0;
                                    int var492 = 0;
                                    int var495 = Math.max(1, var448.size() / var480);

                                    for (int var498 = 0; var498 < var448.size() && var492 < var480 && var489 < var486; var498 += var495) {
                                       int var500 = (Integer)var448.get(var498);
                                       int var119 = var8.gi(var500);
                                       int var120 = var8.gj(var500);

                                       for (int var121 = 0; var121 < 8; var121++) {
                                          int var122 = var119 + a[var121];
                                          int var123 = var120 + b[var121];
                                          if (var8.valid(var122, var123) && !var454[var8.idx(var122, var123)]) {
                                             var492++;
                                             if (var1.groundHeightExact(var8.worldX(var122), var8.worldZ(var123)) < (double)var92) {
                                                var489++;
                                             }
                                             break;
                                          }
                                       }
                                    }

                                    if (var489 >= var486) {
                                       var65++;
                                       if (var66) {
                                          Constants.LOG
                                             .info(
                                                "[SR-LAKEREJ] plate {} at {},{}: stage1 wetRim={}/{} probed={} level={} fillCells={}",
                                                var0.key(),
                                                var8.worldX(var8.gi(var72)),
                                                var8.worldZ(var8.gj(var72)),
                                                var489,
                                                var486,
                                                var492,
                                                var92,
                                                var448.size()
                                             );
                                       }
                                       continue;
                                    }
                                 }
                              } finally {
                                 var67[2] += System.nanoTime() - var464;
                              }

                              Lake var471 = null;
                              double[] var475 = new double[2];
                              if (var49) {
                                 long var482 = System.nanoTime();
                                 NetworkTracer.LocalBasin var490 = a(var8, var1, (List<Integer>)var448, var75);
                                 var67[3] += System.nanoTime() - var482;
                                 if (var490 == null) {
                                    var65++;
                                    if (var66) {
                                       Constants.LOG
                                          .info(
                                             "[SR-LAKEREJ] plate {} at {},{}: real-redetect-null fillCells={}",
                                             var0.key(),
                                             var8.worldX(var8.gi(var72)),
                                             var8.worldZ(var8.gj(var72)),
                                             var448.size()
                                          );
                                    }
                                    continue;
                                 }

                                 int var493 = (int)Math.floor(var490.floor());
                                 int var496 = (int)Math.floor(var490.spill()) - 2 - var103;
                                 var62++;

                                 label3168:
                                 for (int var499 = var496; var499 > var493 && var499 > var496 - 4; var499--) {
                                    long var501 = System.nanoTime();
                                    List var502 = var490.poolAt(var499, var8);
                                    var67[4] += System.nanoTime() - var501;
                                    if (var502 != null) {
                                       var475[0] = 0.0;
                                       var475[1] = 0.0;
                                       long var503 = System.nanoTime();
                                       Lake var504 = a(var502, var499, var1, var8, var0, var3.borderMargin(), var475);
                                       var67[5] += System.nanoTime() - var503;
                                       if (var504 != null) {
                                          int var124 = Math.max(var493 + 1, var499 - 2);

                                          for (int var125 = var124; var125 <= var499; var125++) {
                                             long var126 = System.nanoTime();
                                             List var128 = var125 == var499 ? var502 : var490.poolAt(var125, var8);
                                             var67[4] += System.nanoTime() - var126;
                                             if (var128 != null) {
                                                if (var125 != var499) {
                                                   var475[0] = 0.0;
                                                   var475[1] = 0.0;
                                                }

                                                long var129 = System.nanoTime();
                                                Lake var131 = var125 == var499 ? var504 : a(var128, var125, var1, var8, var0, var3.borderMargin(), var475);
                                                var67[5] += System.nanoTime() - var129;
                                                if (var131 != null) {
                                                   var471 = var131;
                                                   var92 = var125;
                                                   var448 = var128;
                                                   var63 += (long)var128.size();
                                                   break label3168;
                                                }
                                             }
                                          }
                                          break;
                                       }
                                    }
                                 }
                              } else {
                                 var62++;
                                 var63 += (long)var448.size();
                                 long var481 = System.nanoTime();
                                 var471 = a((List<Integer>)var448, var92, var1, var8, var0, var3.borderMargin(), var475);
                                 var67[5] += System.nanoTime() - var481;
                              }

                              if (var471 == null) {
                                 var65++;
                                 if (var66) {
                                    Constants.LOG
                                       .info(
                                          "[SR-LAKEREJ] plate {} at {},{}: flood-null level={} fillCells={}",
                                          var0.key(),
                                          var8.worldX(var8.gi(var72)),
                                          var8.worldZ(var8.gj(var72)),
                                          var92,
                                          var448.size()
                                       );
                                 }
                                 continue;
                              }

                              double var483 = var475[0];
                              int var491 = (int)var475[1];
                              if (var66 && var491 > 0 && var483 / (double)var491 < 1.5) {
                                 Constants.LOG
                                    .info(
                                       "[SR-LAKEREJ] plate {} at {},{}: film level={} meanDepth={} over {} cells (gate OFF: building anyway)",
                                       var0.key(),
                                       var8.worldX(var8.gi(var72)),
                                       var8.worldZ(var8.gj(var72)),
                                       var92,
                                       String.format(Locale.ROOT, "%.2f", var483 / (double)var491),
                                       var491
                                    );
                              }

                              if (!a(var471, 0.9)) {
                                 var65++;
                                 if (var66 || Boolean.getBoolean("streamsreflowing.caldiag")) {
                                    Constants.LOG
                                       .info(
                                          "[SR-LAKEREJ] plate {} at {},{}: fragmented mask level={} (largest component < 90%)",
                                          var0.key(),
                                          var8.worldX(var8.gi(var72)),
                                          var8.worldZ(var8.gj(var72)),
                                          var92
                                       );
                                 }
                                 continue;
                              }

                              var218.add(var471);

                              for (int var497 : var448) {
                                 if (var8.height(var497) < (double)var92 && var471.covers(var8.worldX(var8.gi(var497)), var8.worldZ(var8.gj(var497)))) {
                                    var215[var497] = var442;
                                 }
                              }
                           } else {
                              var218.add(a(var47, var92, 0, 1, var8));

                              for (int var455 : var47) {
                                 var215[var455] = var442;
                              }
                           }

                           int var450 = var172[var72];
                           if (var91 && var450 >= 0 && var215[var450] < 0 && a(var450, var172, var144, var12, var70, 4.0, 48)) {
                              var221.add(new NetworkTracer.PendingOutflow(var450, (double)var92, var76));
                           }
                        } else if (var66) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEREJ] plate {} at {},{}: no-room spill={} floor={} level={} cells={}",
                                 var0.key(),
                                 var8.worldX(var8.gi(var72)),
                                 var8.worldZ(var8.gj(var72)),
                                 String.format("%.1f", var70),
                                 String.format("%.1f", var73),
                                 var92,
                                 var47.size()
                              );
                        }
                     }
                  }
               } else if (var66 && var47.size() >= Math.max(4, var44 / 2)) {
                  Constants.LOG
                     .info(
                        "[SR-LAKEREJ] plate {} at {},{}: qualify spillDepth={} minDepth={} cells={} minCells={} acc={} thr={}",
                        var0.key(),
                        var8.worldX(var8.gi(var72)),
                        var8.worldZ(var8.gj(var72)),
                        String.format("%.1f", var251),
                        String.format("%.1f", var273),
                        var47.size(),
                        var294,
                        String.format("%.0f", var76),
                        String.format("%.0f", var189)
                     );
               }
            }
         }
      }

      int var222 = 0;

      for (int var226 : var138) {
         if (var193[var226] && var215[var226] < 0 && var144[var226] - var8.height(var226) > 6.0) {
            var193[var226] = false;
            var222++;
         }
      }

      if (var222 > 0 && var66) {
         Constants.LOG.info("[SR-LAKEREJ] plate {}: dropped {} channel cells standing >{} above ground on unfilled basins", var0.key(), var222, 6.0);
      }

      if (O) {
         int var224 = 0;
         int var227 = 0;
         double var71 = 0.0;
         int var230 = 0;
         int var74 = 0;
         int var234 = 0;

         for (int var77 : var138) {
            if (var193[var77]) {
               var224++;
               if (var215[var77] >= 0) {
                  var227++;
               } else {
                  double var246 = var144[var77] - var8.height(var77);
                  if (var246 > 3.0) {
                     var234++;
                  }

                  if (var246 > var71) {
                     var71 = var246;
                     var230 = var8.worldX(var8.gi(var77));
                     var74 = var8.worldZ(var8.gj(var77));
                  }
               }
            }
         }

         Constants.LOG
            .info(
               "[SR-FILLSTATS] plate {}: {} river cells ({} on lakes); non-lake fill >3 blocks: {}; deepest {} at ({},{})",
               var0.key(),
               var224,
               var227,
               var234,
               String.format(Locale.ROOT, "%.1f", var71),
               var230,
               var74
            );
      }

      long var225 = System.nanoTime();
      if (var66) {
         long var228 = var67[2] + var67[3] + var67[4] + var67[5];
         Constants.LOG
            .info(
               "[SR-LAKESTEP] plate {}: probes {} ms, redetect {} ms, poolAt {} ms, flood {} ms (warm {} ms of it), other {} ms of {} ms total",
               var0.key(),
               var67[2] / 1000000L,
               var67[3] / 1000000L,
               var67[4] / 1000000L,
               var67[5] / 1000000L,
               var67[7] / 1000000L,
               (var225 - var211 - var228) / 1000000L,
               (var225 - var211) / 1000000L
            );
      }

      double var229 = var189 * 16.0;
      boolean[] var231 = new boolean[var11];

      for (int var235 : var138) {
         if (var193[var235] && var215[var235] < 0 && !(var176[var235] < var229)) {
            int var238 = var172[var235];
            double var240 = 0.0;
            if (var238 >= 0) {
               double var252 = a(var8, var235, var238);
               var240 = var252 > 1.0E-9 ? Math.abs(var144[var235] - var144[var238]) / var252 : 0.0;
            }

            if (var240 <= 0.03) {
               var231[var235] = true;
            }
         }
      }

      boolean[] var233 = new boolean[var11];
      ArrayDeque var236 = new ArrayDeque();
      ArrayList var239 = new ArrayList();

      for (int var247 : var138) {
         if (var231[var247] && !var233[var247]) {
            var239.clear();
            var236.clear();
            var233[var247] = true;
            var236.add(var247);
            int var253 = Integer.MAX_VALUE;
            int var258 = Integer.MAX_VALUE;
            int var265 = Integer.MIN_VALUE;
            int var274 = Integer.MIN_VALUE;
            double var284 = Double.MAX_VALUE;
            double var303 = 0.0;

            while (!var236.isEmpty()) {
               int var317 = (Integer)var236.poll();
               var239.add(var317);
               int var88 = var8.gi(var317);
               int var338 = var8.gj(var317);
               var253 = Math.min(var253, var88);
               var265 = Math.max(var265, var88);
               var258 = Math.min(var258, var338);
               var274 = Math.max(var274, var338);
               var284 = Math.min(var284, var144[var317]);
               var303 = Math.max(var303, var176[var317]);

               for (int var90 = 0; var90 < 8; var90++) {
                  int var358 = var88 + a[var90];
                  int var366 = var338 + b[var90];
                  if (var8.valid(var358, var366)) {
                     int var375 = var8.idx(var358, var366);
                     if (var231[var375] && !var233[var375]) {
                        var233[var375] = true;
                        var236.add(var375);
                     }
                  }
               }
            }

            int var318 = var265 - var253 + 1;
            int var328 = var274 - var258 + 1;
            if (var239.size() >= var44
               && Math.min(var318, var328) >= 3
               && !((double)var239.size() < 0.3 * (double)var318 * (double)var328)
               && !var3.growFromWater()) {
               int var339 = (int)Math.floor(var284) - 1;
               if (var339 > var12) {
                  double var349 = var303 * (double)var3.gridSpacing() * (double)var3.gridSpacing();
                  int var367 = Math.max(1, Math.min(6, (int)Math.round(3.0 + 4.0 * Math.sqrt(var349 / 2500.0))));
                  int var376 = var218.size();
                  var218.add(a(var239, var339, var367, 0, var8));

                  for (int var389 : var239) {
                     var215[var389] = var376;
                  }
               }
            }
         }
      }

      for (int var248 : var138) {
         if (var215[var248] >= 0) {
            var193[var248] = false;
         }
      }

      if (var3.growFromWater()) {
         int[] var243 = new int[var11];
         Arrays.fill(var243, -1);

         for (int var249 = var138.size() - 1; var249 >= 0; var249--) {
            int var254 = (Integer)var138.get(var249);
            if (var193[var254]) {
               int var259 = var172[var254];
               if (var259 >= 0 && var215[var259] >= 0) {
                  var243[var254] = var215[var259];
               } else if (var259 >= 0 && var193[var259] && var144[var259] > (double)var12) {
                  var243[var254] = var243[var259];
               }
            }
         }

         int var250 = 0;

         for (int var260 : var138) {
            double var266 = var8.height(var260);
            if (var193[var260]
               && var243[var260] >= 0
               && var215[var260] < 0
               && var266 > (double)var12
               && var266 < (double)((Lake)var218.get(var243[var260])).level() - 6.0) {
               var193[var260] = false;
               var250++;
            }
         }

         if (var250 > 0) {
            Constants.LOG.debug("[SR-MERGE] lake-dip cut: {} cells on sub-lake routes removed (subtrees pruned below)", var250);
         }
      }

      long var244 = System.nanoTime();
      if (var3.growFromWater()) {
         boolean[] var256 = new boolean[var11];

         for (int var261 = var138.size() - 1; var261 >= 0; var261--) {
            int var267 = (Integer)var138.get(var261);
            boolean var275 = var215[var267] >= 0 || var144[var267] <= (double)var12 || var8.riverOutlet(var267);
            var256[var267] = var275 || var172[var267] >= 0 && var256[var172[var267]];
         }

         if (var1.hasEstimate() && !Boolean.getBoolean("streamsreflowing.norescue")) {
            HashSet var262 = new HashSet();
            HashSet var268 = new HashSet();

            for (int var285 : var138) {
               if (var193[var285] && !var256[var285]) {
                  int var295 = var285;
                  int var304 = var11;

                  while (var172[var295] >= 0 && var304-- > 0) {
                     var295 = var172[var295];
                  }

                  if (var262.add(var295) && !(var144[var295] > (double)var12 + 3.0)) {
                     int var86 = var8.gi(var295);
                     int var319 = var8.gj(var295);
                     boolean var329 = false;

                     label2962:
                     for (int var340 = -2; var340 <= 2; var340++) {
                        for (int var350 = -2; var350 <= 2; var350++) {
                           int var359 = var86 + var350;
                           int var368 = var319 + var340;
                           if (var8.valid(var359, var368) && var8.height(var8.idx(var359, var368)) <= (double)(var12 - 2)) {
                              var329 = true;
                              break label2962;
                           }
                        }
                     }

                     if (var329) {
                        var268.add(var295);
                     }
                  }
               }
            }

            if (!var268.isEmpty()) {
               for (int var277 = var138.size() - 1; var277 >= 0; var277--) {
                  int var286 = (Integer)var138.get(var277);
                  var256[var286] = var256[var286] || var268.contains(var286) || var172[var286] >= 0 && var256[var172[var286]];
               }
            }
         }

         for (int var269 : var138) {
            if (var193[var269] && !var256[var269]) {
               var193[var269] = false;
            }
         }
      }

      long var257 = System.nanoTime();
      int[] var270 = new int[var11];
      Arrays.fill(var270, -1);

      for (int var278 = var138.size() - 1; var278 >= 0; var278--) {
         int var287 = (Integer)var138.get(var278);
         if (var193[var287]) {
            var270[var287] = var172[var287] >= 0 && var193[var172[var287]] ? var270[var172[var287]] : var287;
         }
      }

      HashMap var279 = new HashMap();

      for (int var296 : var138) {
         if (var193[var296]) {
            var279.merge(var270[var296], 1, Integer::sum);
         }
      }

      boolean[] var289 = new boolean[var11];

      for (int var305 : var138) {
         if (var193[var305]) {
            int var310 = var172[var305];
            if (var310 >= 0 && var215[var310] < 0 && !(var144[var310] <= (double)var12)) {
               int var321 = var270[var305];
               int var331 = (Integer)var279.get(var321);
               int var342 = var8.gi(var305);
               int var351 = var8.gj(var305);

               for (int var360 = 0; var360 < 8; var360++) {
                  int var369 = var342 + a[var360];
                  int var377 = var351 + b[var360];
                  if (var8.valid(var369, var377)) {
                     int var383 = var8.idx(var369, var377);
                     if (var8.inDomain(var383) && var193[var383]) {
                        int var390 = var270[var383];
                        if (var390 != var321) {
                           int var395 = var172[var383];
                           if (var395 >= 0 && var215[var395] < 0 && !(var144[var395] <= (double)var12)) {
                              int var402 = (Integer)var279.get(var390);
                              if (K != null && b((double)var8.worldX(var342), (double)var8.worldZ(var351))) {
                                 Constants.LOG
                                    .info(
                                       "[SR-SEGDUMP] 4b6 contact: cell ({},{}) sys {} size {} vs cell ({},{}) sys {} size {} -> {}",
                                       var8.worldX(var342),
                                       var8.worldZ(var351),
                                       var321,
                                       var331,
                                       var8.worldX(var8.gi(var383)),
                                       var8.worldZ(var8.gj(var383)),
                                       var390,
                                       var402,
                                       var331 >= var402 && (var331 != var402 || var321 <= var390) ? "keep" : "DROP this cell"
                                    );
                              }

                              if (var331 < var402 || var331 == var402 && var321 > var390) {
                                 var289[var305] = true;
                                 break;
                              }
                           } else if (K != null && b((double)var8.worldX(var342), (double)var8.worldZ(var351))) {
                              Constants.LOG
                                 .info(
                                    "[SR-SEGDUMP] 4b6 contact EXEMPT (neighbour is mouth): cell ({},{}) sys {} touches cell ({},{}) sys {}",
                                    var8.worldX(var342),
                                    var8.worldZ(var351),
                                    var321,
                                    var8.worldX(var8.gi(var383)),
                                    var8.worldZ(var8.gj(var383)),
                                    var390
                                 );
                           }
                        }
                     }
                  }
               }
            } else if (K != null && b((double)var8.worldX(var8.gi(var305)), (double)var8.worldZ(var8.gj(var305)))) {
               for (int var320 = 0; var320 < 8; var320++) {
                  int var330 = var8.gi(var305) + a[var320];
                  int var341 = var8.gj(var305) + b[var320];
                  if (var8.valid(var330, var341) && var193[var8.idx(var330, var341)] && var270[var8.idx(var330, var341)] != var270[var305]) {
                     Constants.LOG
                        .info(
                           "[SR-SEGDUMP] 4b6 contact EXEMPT (own cell is mouth): cell ({},{}) sys {} touches cell ({},{}) sys {}",
                           var8.worldX(var8.gi(var305)),
                           var8.worldZ(var8.gj(var305)),
                           var270[var305],
                           var8.worldX(var330),
                           var8.worldZ(var341),
                           var270[var8.idx(var330, var341)]
                        );
                  }
               }
            }
         }
      }

      int var298 = 0;

      for (int var311 : var138) {
         if (var289[var311]) {
            var193[var311] = false;
            var298++;
         }
      }

      if (var298 > 0) {
         boolean[] var307 = new boolean[var11];

         for (int var312 = var138.size() - 1; var312 >= 0; var312--) {
            int var322 = (Integer)var138.get(var312);
            if (var193[var322]) {
               int var332 = var172[var322];
               boolean var343 = var332 >= 0 && (var215[var332] >= 0 || var144[var332] <= (double)var12);
               var307[var322] = var343 || var332 >= 0 && var193[var332] && var307[var332];
            }
         }

         int var313 = 0;

         for (int var333 : var138) {
            if (var193[var333] && !var307[var333]) {
               var193[var333] = false;
               var313++;
            }
         }

         Constants.LOG.debug("[SR-MERGE] local separation: trimmed {} contact cells, pruned {} orphaned", var298, var313);
      }

      var270 = new int[var11];
      Arrays.fill(var270, -1);

      for (int var280 = var138.size() - 1; var280 >= 0; var280--) {
         int var290 = (Integer)var138.get(var280);
         if (var193[var290]) {
            var270[var290] = var172[var290] >= 0 && var193[var172[var290]] ? var270[var172[var290]] : var290;
         }
      }

      int[] var281 = new int[var11];

      for (int var299 : var138) {
         if (var193[var299] && var172[var299] >= 0 && var193[var172[var299]]) {
            var281[var172[var299]]++;
         }
      }

      int var292 = 0;
      var298 = 0;

      for (int var308 = 0; var308 < var138.size(); var308++) {
         int var314 = (Integer)var138.get(var308);
         if (var193[var314]) {
            int var324 = var172[var314];
            if (var324 >= 0 && var215[var324] < 0 && !(var144[var324] <= (double)var12)) {
               int var334 = var8.gi(var324) - var8.gi(var314);
               int var344 = var8.gj(var324) - var8.gj(var314);
               int var352 = var8.gi(var314);
               int var361 = var8.gj(var314);
               int var370 = -1;
               double var378 = var144[var314];

               for (int var391 = 0; var391 < 8; var391++) {
                  int var396 = var352 + a[var391];
                  int var403 = var361 + b[var391];
                  if (var8.valid(var396, var403)) {
                     int var98 = var8.idx(var396, var403);
                     if (var8.inDomain(var98) && var193[var98] && var270[var98] == var270[var314] && !(var144[var98] >= var144[var314]) && var98 != var324) {
                        int var413 = var172[var98];
                        if (var413 >= 0) {
                           int var100 = var8.gi(var413) - var8.gi(var98);
                           int var424 = var8.gj(var413) - var8.gj(var98);
                           if (var334 * var100 + var344 * var424 > 0 && a[var391] * var334 + b[var391] * var344 >= 0) {
                              boolean var102 = false;
                              int var435 = var324;

                              for (int var443 = 0; var443 < 24 && var435 >= 0; var443++) {
                                 if (var435 == var98) {
                                    var102 = true;
                                    break;
                                 }

                                 var435 = var172[var435];
                              }

                              if (!var102 && var144[var98] < var378) {
                                 var378 = var144[var98];
                                 var370 = var98;
                              }
                           }
                        }
                     }
                  }
               }

               if (var370 >= 0) {
                  int var392 = var172[var314];
                  var281[var392]--;
                  var172[var314] = var370;
                  var281[var370]++;
                  var292++;

                  for (int var397 = var392; var397 >= 0 && var193[var397] && var281[var397] == 0; var298++) {
                     var193[var397] = false;
                     int var404 = var172[var397];
                     if (var404 >= 0 && var193[var404]) {
                        var281[var404]--;
                     }

                     var397 = var404;
                  }
               }
            }
         }
      }

      if (var292 > 0) {
         Constants.LOG.debug("[SR-MERGE] branch fuse: {} parallel branches fused, {} redundant cells trimmed", var292, var298);
      }

      var270 = new int[var11];

      for (int var293 : var138) {
         if (var193[var293] && var172[var293] >= 0) {
            var270[var172[var293]]++;
         }
      }

      long var283 = System.nanoTime();
      if (Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG
            .info(
               "[SR-ROUTE] plate {}: pflood {} ms, d8+thin {} ms, lakes {} ms, ponds {} ms, reach {} ms, trim {} ms",
               var0.key(),
               (var163 - var9) / 1000000L,
               (var211 - var163) / 1000000L,
               (var225 - var211) / 1000000L,
               (var244 - var225) / 1000000L,
               (var257 - var244) / 1000000L,
               (var283 - var257) / 1000000L
            );
      }

      for (int var301 = 0; var301 < P.length(); var301++) {
         P.set(var301, 0L);
      }

      ArrayList var302 = new ArrayList();
      ArrayList var309 = new ArrayList();

      for (int var325 : var138) {
         if (var193[var325] && var270[var325] != 1) {
            var309.add(var325);
         }
      }

      int[] var316 = new int[var11];
      Arrays.fill(var316, -1);

      for (int var326 = var138.size() - 1; var326 >= 0; var326--) {
         int var335 = (Integer)var138.get(var326);
         if (var193[var335]) {
            var316[var335] = var172[var335] >= 0 && var193[var172[var335]] ? var316[var172[var335]] : var335;
         }
      }

      HashMap var327 = new HashMap();

      for (int var345 : var309) {
         int var353 = var316[var345];
         if (var353 >= 0 && !var327.containsKey(var353)) {
            int var362 = var172[var353];
            var327.put(var353, var362 >= 0 && var215[var362] >= 0 ? (Lake)var218.get(var215[var362]) : null);
         }
      }

      ArrayList var337 = new ArrayList();
      if (var3.growFromWater()) {
         List[] var346 = new List[var309.size()];
         LowResGrid.parallelFor(
            var309.size(),
            var18x -> var346[var18x] = a(
                  (Integer)var309.get(var18x),
                  var172,
                  var215,
                  var218,
                  (Lake)var327.get(var316[var309.get(var18x)]),
                  var193,
                  var270,
                  var144,
                  var176,
                  var8,
                  var1,
                  var2,
                  var3,
                  var4,
                  var12
               )
         );

         for (int var354 = 0; var354 < var346.length; var354++) {
            int var363 = var316[var309.get(var354)];

            for (int var371 = 0; var371 < var346[var354].size(); var371++) {
               var337.add(var363);
            }

            var302.addAll(var346[var354]);
         }
      } else {
         for (int var355 : var309) {
            List var364 = a(
               var355, var172, var215, var218, (Lake)var327.get(var316[var355]), var193, var270, var144, var176, var8, var1, var2, var3, var4, var12
            );
            int var372 = var316[var355];

            for (int var379 = 0; var379 < var364.size(); var379++) {
               var337.add(var372);
            }

            var302.addAll(var364);
         }
      }

      List[] var348 = new List[var221.size()];
      LowResGrid.parallelFor(var221.size(), var12x -> {
         ArrayList var13x = new ArrayList();
         a((NetworkTracer.PendingOutflow)var221.get(var12x), var8, var172, var215, var218, var144, var12, var1, var3, var4, var13x);
         var348[var12x] = var13x;
      });

      for (int var356 = 0; var356 < var348.length; var356++) {
         NetworkTracer.PendingOutflow var365 = (NetworkTracer.PendingOutflow)var221.get(var356);
         int var373 = var365.outlet() >= 0 && var316[var365.outlet()] >= 0 ? var316[var365.outlet()] : -(var365.outlet() + 2);

         for (RiverSegment var384 : var348[var356] == null ? List.of() : var348[var356]) {
            var302.add(var384);
            var337.add(var373);
         }
      }

      long var357 = System.nanoTime();
      boolean[] var374 = new boolean[var218.size()];
      a(var302, var337, var218, var374, var12, var3, var1);
      double[] var381 = new double[var302.size()];
      int[] var385 = new int[var302.size()];

      for (int var393 = 0; var393 <= var218.size() && !var302.isEmpty(); var393++) {
         a(var302, var218, var374, var381, var385);
         int[] var398 = chainIds(var302);
         HashSet var405 = new HashSet();

         for (RiverSegment var414 : var302) {
            var405.add(a(var414.x2(), var414.z2()));
         }

         boolean var410 = false;
         int var415 = Integer.MIN_VALUE;

         for (int var419 = 0; var419 < var302.size(); var419++) {
            if (var398[var419] != var415) {
               var415 = var398[var419];
               if (var385[var419] >= 0 && !var374[var385[var419]]) {
                  RiverSegment var425 = (RiverSegment)var302.get(var419);
                  if (!var405.contains(a(var425.x1(), var425.z1()))) {
                     double var429 = var425.y1() - (double)RiverEngine.insetFor(var425.w1(), var3);
                     if (var429 < var381[var419] - 0.01) {
                        var374[var385[var419]] = true;
                        var410 = true;
                        Lake var444 = (Lake)var218.get(var385[var419]);
                        if (Boolean.getBoolean("streamsreflowing.caldiag") || K != null) {
                           Constants.LOG
                              .info(
                                 "[SR-LAKEKILL] plate {}: lake at {},{} level {} killed: a contacting stream's head surface {} is below it (cannot be grade-matched)",
                                 var0.key(),
                                 var444.cellMinX(),
                                 var444.cellMinZ(),
                                 var444.level(),
                                 String.format(Locale.ROOT, "%.1f", var429)
                              );
                        }
                     }
                  }
               }
            }
         }

         if (!var410) {
            break;
         }

         a(var302, var337, var218, var374, var12, var3, var1);
         var381 = new double[var302.size()];
         var385 = new int[var302.size()];
      }

      int[] var382 = new int[var337.size()];

      for (int var386 = 0; var386 < var382.length; var386++) {
         var382[var386] = (Integer)var337.get(var386);
      }

      HashMap var387 = new HashMap();
      int[] var394 = new int[var382.length];

      for (int var399 = 0; var399 < var382.length; var399++) {
         int var406 = var382[var399];
         if (var406 < 0) {
            var394[var399] = Integer.MIN_VALUE;
         } else {
            Integer var411 = (Integer)var387.get(var406);
            if (var411 == null) {
               int var416 = var172[var406];
               var411 = var416 >= 0 && var215[var416] >= 0 && !var374[var215[var416]] ? ((Lake)var218.get(var215[var416])).level() : Integer.MIN_VALUE;
               var387.put(var406, var411);
            }

            var394[var399] = var411 == Integer.MIN_VALUE
               ? Integer.MIN_VALUE
               : var411 + RiverEngine.insetFor((((RiverSegment)var302.get(var399)).w1() + ((RiverSegment)var302.get(var399)).w2()) * 0.5, var3);
         }
      }

      double[] var400 = new double[var302.size()];
      int[] var407 = new int[var302.size()];
      a(var302, var218, var374, var400, var407);

      for (int var412 = 0; var412 < var302.size(); var412++) {
         if (var407[var412] >= 0) {
            RiverSegment var417 = (RiverSegment)var302.get(var412);
            var394[var412] = Math.max(var394[var412], (int)Math.round(var400[var412]) + RiverEngine.insetFor((var417.w1() + var417.w2()) * 0.5, var3));
         }
      }

      int[] var401 = chainIds(var302);
      double var408 = Math.max(8.0, var3.maxWidth() * 2.0);
      HashMap var418 = new HashMap();

      for (int var420 = 0; var420 < var302.size(); var420++) {
         RiverSegment var426 = (RiverSegment)var302.get(var420);
         int var430 = (int)Math.floor(var426.minX() / var408);
         int var436 = (int)Math.floor(var426.maxX() / var408);
         int var445 = (int)Math.floor(var426.minZ() / var408);
         int var451 = (int)Math.floor(var426.maxZ() / var408);

         for (int var456 = var430; var456 <= var436; var456++) {
            for (int var459 = var445; var459 <= var451; var459++) {
               var418.computeIfAbsent((long)var456 << 32 ^ (long)var459 & 4294967295L, var0x -> new ArrayList<>()).add(var420);
            }
         }
      }

      for (int var421 = 0; var421 < 4; var421++) {
         boolean var427 = a(var302, var382, var394, var3, var1, var401, var418, var408);

         for (int var431 = 0; var431 < var302.size(); var431++) {
            int var437 = var394[var431];
            if (var437 != Integer.MIN_VALUE) {
               RiverSegment var446 = (RiverSegment)var302.get(var431);
               if (var446.y1() < (double)var437 || var446.y2() < (double)var437) {
                  if (K != null && b((var446.x1() + var446.x2()) * 0.5, (var446.z1() + var446.z2()) * 0.5)) {
                     Constants.LOG
                        .info(
                           "[SR-SEGDUMP] modifier lakeFloor RAISE seg {} y {}..{} -> floor {}",
                           var431,
                           String.format(Locale.ROOT, "%.1f", var446.y1()),
                           String.format(Locale.ROOT, "%.1f", var446.y2()),
                           var437
                        );
                  }

                  var302.set(
                     var431,
                     new RiverSegment(
                        var446.x1(),
                        var446.z1(),
                        Math.max(var446.y1(), (double)var437),
                        var446.w1(),
                        var446.x2(),
                        var446.z2(),
                        Math.max(var446.y2(), (double)var437),
                        var446.w2()
                     )
                  );
               }
            }
         }

         a(var302, var394);
         if (!var427) {
            break;
         }
      }

      if (K != null) {
         a(var0, var302, var382, var218);
      }

      if (I != null) {
         a(I[0], I[1], var138, var8, var144, var172, var176, var193, var215, var12, var0);
      }

      if (Boolean.getBoolean("streamsreflowing.caldiag") || O) {
         long var422 = System.nanoTime();
         Constants.LOG
            .info(
               "[SR-PHASES] plate {}: fill {} ms, route {} ms, chains+lakes {} ms, post {} ms ({} segs, sampler {}/{})",
               var0.key(),
               (var9 - var6) / 1000000L,
               (var283 - var9) / 1000000L,
               (var357 - var283) / 1000000L,
               (var422 - var357) / 1000000L,
               var302.size(),
               LowResGrid.samplerWidth(),
               LowResGrid.samplerFullWidth()
            );
         StringBuilder var432 = new StringBuilder();

         for (int var438 = 0; var438 < W.length; var438++) {
            var432.append(' ').append(W[var438]).append(' ').append(P.get(var438) / 1000000L).append("ms");
         }

         Constants.LOG.info("[SR-CHAINSTEP] plate {} (CPU across the parallel chain tasks, read as proportions):{}", var0.key(), var432);
         double var439 = 0.0;
         double var452 = 0.0;
         double var460 = 0.0;

         for (RiverSegment var472 : var302) {
            var439 += Math.hypot(var472.x2() - var472.x1(), var472.z2() - var472.z1());
            var452 += (var472.y1() + var472.y2()) * 0.5;
            var460 += (var472.w1() + var472.w2()) * 0.5;
         }

         int var466 = Math.max(1, var302.size());
         Constants.LOG
            .info(
               "[SR-QUALITY] plate {}: segs {}, length {}, meanGrade {}, meanWidth {}, lakes {}",
               var0.key(),
               var302.size(),
               String.format(Locale.ROOT, "%.1f", var439),
               String.format(Locale.ROOT, "%.3f", var452 / (double)var466),
               String.format(Locale.ROOT, "%.3f", var460 / (double)var466),
               var218.size()
            );
         Constants.LOG
            .info(
               "[SR-LAKES] plate {}: basins {} oversize {} depthGated {} qualified {} thinned {} fineFloods {} ({} coarse cells, {} rejected) built {}",
               var0.key(),
               var56,
               var57,
               var60,
               var58,
               var59,
               var62,
               var63,
               var65,
               var218.size()
            );
      }

      int var423 = b(var302, var3);
      if (var423 > 0 && Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info("[SR-MERGE] plate {}: absorbed {} thin engulfed crossing segment(s)", var0.key(), var423);
      }

      int var428 = a(var302, var3);
      if (var428 > 0 && Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info("[SR-WIDTH] plate {}: relaxed {} waist segment(s) to half the upstream max", var0.key(), var428);
      }

      for (int var433 = var218.size() - 1; var433 >= 0; var433--) {
         if (var374[var433]) {
            var218.remove(var433);
         }
      }

      for (int var434 = var218.size() - 1; var434 >= 0; var434--) {
         Lake var440 = (Lake)var218.get(var434);
         if (var440.digDepth() <= 0) {
            boolean var447 = false;

            for (RiverSegment var457 : var302) {
               double var461 = (var457.w1() + var457.w2()) * 0.5;
               if (!(var461 <= 3.0) && !(Math.min(var457.y1(), var457.y2()) >= (double)(var440.level() - 3))) {
                  int var467 = (int)Math.round((var457.x1() + var457.x2()) * 0.5);
                  int var473 = (int)Math.round((var457.z1() + var457.z2()) * 0.5);
                  int var476 = (int)Math.ceil(var461 / (double)Math.max(1, var440.spacing()));
                  if (var440.coversWithin(var467, var473, var476)) {
                     if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                        Constants.LOG
                           .info(
                              "[SR-LAKEREJ] plate {} at {},{}: drained-through level={} by w={} channel y={} at {},{}",
                              var0.key(),
                              var440.cellMinX(),
                              var440.cellMinZ(),
                              var440.level(),
                              String.format(Locale.ROOT, "%.1f", var461),
                              String.format(Locale.ROOT, "%.1f", Math.min(var457.y1(), var457.y2())),
                              var467,
                              var473
                           );
                     }

                     var447 = true;
                     break;
                  }
               }
            }

            if (var447) {
               var218.remove(var434);
            }
         }
      }

      return assemble(var0.key(), var302, var218, var3);
   }

   private static void a(List<RiverSegment> var0, int[] var1) {
      int var2 = var0.size();
      if (var2 != 0) {
         HashMap var3 = new HashMap();
         ArrayList var4 = new ArrayList();
         int[] var5 = new int[var2];
         int[] var6 = new int[var2];

         for (int var7 = 0; var7 < var2; var7++) {
            RiverSegment var8 = (RiverSegment)var0.get(var7);
            var5[var7] = a(var8.x1(), var8.z1(), var8.y1(), var3, var4);
            var6[var7] = a(var8.x2(), var8.z2(), var8.y2(), var3, var4);
         }

         int var20 = var4.size();
         double[] var21 = new double[var20];
         Arrays.fill(var21, Double.NEGATIVE_INFINITY);

         for (int var9 = 0; var9 < var2; var9++) {
            if (var1[var9] != Integer.MIN_VALUE) {
               var21[var5[var9]] = Math.max(var21[var5[var9]], (double)var1[var9]);
               var21[var6[var9]] = Math.max(var21[var6[var9]], (double)var1[var9]);
            }
         }

         int[] var22 = new int[var20];
         ArrayList var10 = new ArrayList(var20);

         for (int var11 = 0; var11 < var20; var11++) {
            var10.add(new ArrayList());
         }

         for (int var23 = 0; var23 < var2; var23++) {
            if (var5[var23] != var6[var23]) {
               ((List)var10.get(var5[var23])).add(var6[var23]);
               var22[var6[var23]]++;
            }
         }

         double[] var24 = new double[var20];

         for (int var12 = 0; var12 < var20; var12++) {
            var24[var12] = Math.max((Double)var4.get(var12), var21[var12]);
         }

         ArrayDeque var25 = new ArrayDeque();

         for (int var13 = 0; var13 < var20; var13++) {
            if (var22[var13] == 0) {
               var25.add(var13);
            }
         }

         while (!var25.isEmpty()) {
            int var26 = (Integer)var25.poll();

            for (int var15 : (List)var10.get(var26)) {
               if (var24[var26] < var24[var15]) {
                  var24[var15] = Math.max(var24[var26], var21[var15]);
               }

               if (--var22[var15] == 0) {
                  var25.add(var15);
               }
            }
         }

         boolean var27 = true;

         for (int var28 = 0; var27 && var28 < 64; var28++) {
            var27 = false;

            for (int var30 = 0; var30 < var2; var30++) {
               if (var5[var30] != var6[var30] && var24[var5[var30]] < var24[var6[var30]]) {
                  double var16 = Math.max(var24[var5[var30]], var21[var6[var30]]);
                  if (var16 < var24[var6[var30]]) {
                     var24[var6[var30]] = var16;
                     var27 = true;
                  }
               }
            }
         }

         for (int var29 = 0; var29 < var2; var29++) {
            RiverSegment var31 = (RiverSegment)var0.get(var29);
            double var32 = var24[var5[var29]];
            double var18 = Math.min(var32, var24[var6[var29]]);
            if (var32 != var31.y1() || var18 != var31.y2()) {
               var0.set(var29, new RiverSegment(var31.x1(), var31.z1(), var32, var31.w1(), var31.x2(), var31.z2(), var18, var31.w2()));
            }
         }
      }
   }

   private static void a(List<RiverSegment> var0) {
      int var1 = var0.size();
      if (var1 != 0) {
         HashMap var2 = new HashMap();
         ArrayList var3 = new ArrayList();
         int[] var4 = new int[var1];
         int[] var5 = new int[var1];

         for (int var6 = 0; var6 < var1; var6++) {
            RiverSegment var7 = (RiverSegment)var0.get(var6);
            var4[var6] = a(var7.x1(), var7.z1(), var7.y1(), var2, var3);
            var5[var6] = a(var7.x2(), var7.z2(), var7.y2(), var2, var3);
         }

         int var20 = var3.size();
         ArrayList var21 = new ArrayList(var20);

         for (int var8 = 0; var8 < var20; var8++) {
            var21.add(new ArrayList());
         }

         int[] var22 = new int[var20];

         for (int var9 = 0; var9 < var1; var9++) {
            if (var4[var9] != var5[var9]) {
               ((List)var21.get(var5[var9])).add(var9);
               var22[var4[var9]]++;
            }
         }

         double[] var23 = new double[var20];

         for (int var10 = 0; var10 < var20; var10++) {
            var23[var10] = (Double)var3.get(var10);
         }

         ArrayDeque var24 = new ArrayDeque();

         for (int var11 = 0; var11 < var20; var11++) {
            if (var22[var11] == 0) {
               var24.add(var11);
            }
         }

         while (!var24.isEmpty()) {
            int var25 = (Integer)var24.poll();

            for (int var13 : (List)var21.get(var25)) {
               int var14 = var4[var13];
               RiverSegment var15 = (RiverSegment)var0.get(var13);
               double var16 = Math.hypot(var15.x2() - var15.x1(), var15.z2() - var15.z1());
               double var18 = var23[var25] + 1.0 * var16;
               if (var18 < var23[var14]) {
                  var23[var14] = var18;
               }

               if (--var22[var14] == 0) {
                  var24.add(var14);
               }
            }
         }

         for (int var26 = 0; var26 < var1; var26++) {
            RiverSegment var27 = (RiverSegment)var0.get(var26);
            double var28 = Math.min(var27.y1(), var23[var4[var26]]);
            double var29 = Math.min(var28, Math.min(var27.y2(), var23[var5[var26]]));
            if (var28 != var27.y1() || var29 != var27.y2()) {
               var0.set(var26, new RiverSegment(var27.x1(), var27.z1(), var28, var27.w1(), var27.x2(), var27.z2(), var29, var27.w2()));
            }
         }
      }
   }

   private static int a(double var0, double var2, double var4, Map<Long, Integer> var6, List<Double> var7) {
      long var8 = Math.round(var0 * 16.0) & 4294967295L;
      long var10 = Math.round(var2 * 16.0) & 4294967295L;
      long var12 = var8 << 32 | var10;
      Integer var14 = (Integer)var6.get(var12);
      if (var14 == null) {
         var6.put(var12, var7.size());
         var7.add(var4);
         return var7.size() - 1;
      } else {
         if (var4 < (Double)var7.get(var14)) {
            var7.set(var14, var4);
         }

         return var14;
      }
   }

   private static boolean a(
      List<RiverSegment> var0, int[] var1, int[] var2, StreamsConfig var3, TerrainSampler var4, int[] var5, Map<Long, List<Integer>> var6, double var7
   ) {
      int var9 = var0.size();
      if (var9 < 2) {
         return false;
      } else {
         ConcurrentHashMap var10 = new ConcurrentHashMap();
         double var11 = Math.max(var3.maxWidth() * 2.0 + 2.0, 40.0);
         double[] var13 = new double[var9];

         for (int var14 = 0; var14 < var9; var14++) {
            var13[var14] = ((RiverSegment)var0.get(var14)).y2();
         }

         LowResGrid.parallelFor(var9, var11x -> {
            RiverSegment var12 = (RiverSegment)var0.get(var11x);
            double var13x = (var12.x1() + var12.x2()) * 0.5;
            double var15x = (var12.z1() + var12.z2()) * 0.5;
            double var17x = var12.widthAt(0.5);
            double var19x = Double.NaN;
            int var21 = (int)Math.floor((var13x - var11) / var7);
            int var22 = (int)Math.floor((var13x + var11) / var7);
            int var23 = (int)Math.floor((var15x - var11) / var7);
            int var24 = (int)Math.floor((var15x + var11) / var7);

            for (int var25 = var21; var25 <= var22; var25++) {
               for (int var26 = var23; var26 <= var24; var26++) {
                  List var27 = (List)var6.get((long)var25 << 32 ^ (long)var26 & 4294967295L);
                  if (var27 != null) {
                     for (int var29 : var27) {
                        if (var5[var29] != var5[var11x]) {
                           RiverSegment var30 = (RiverSegment)var0.get(var29);
                           double var31 = var30.clampParam(var13x, var15x);
                           double var33 = var30.distanceAt(var13x, var15x, var31);
                           double var35 = var30.widthAt(var31);
                           if (var33 > var17x + var35 + 2.0) {
                              double var50 = var17x + Math.max(5.0, var17x * var3.bankWidth()) + var35 + Math.max(5.0, var35 * var3.bankWidth());
                              if (!(var33 > Math.min(40.0, var50))) {
                                 double var39 = var30.gradeAt(var31);
                                 if (!(var39 >= var12.y2() - 1.0) && (Double.isNaN(var19x) || !(var39 >= var19x))) {
                                    double var41 = var30.x1() + (var30.x2() - var30.x1()) * var31;
                                    double var43 = var30.z1() + (var30.z2() - var30.z1()) * var31;
                                    double var45 = Double.NEGATIVE_INFINITY;

                                    for (int var47 = 1; var47 <= 3; var47++) {
                                       double var48 = (double)var47 / 4.0;
                                       var45 = Math.max(var45, a(var4, var10, var13x + (var41 - var13x) * var48, var15x + (var43 - var15x) * var48));
                                    }

                                    if (!(var45 >= var12.y2() + 5.0)) {
                                       var19x = var39;
                                    }
                                 }
                              }
                           } else {
                              double var37 = var30.gradeAt(var31);
                              if (var37 < var12.y2() - 1.0 && (Double.isNaN(var19x) || var37 < var19x)) {
                                 var19x = var37;
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (!Double.isNaN(var19x)) {
               var13[var11x] = Math.min(var13[var11x], var19x);
            }
         });
         boolean var19 = false;

         for (int var15 = 0; var15 < var9; var15++) {
            RiverSegment var16 = (RiverSegment)var0.get(var15);
            double var17 = var2[var15] == Integer.MIN_VALUE ? var13[var15] : Math.max(var13[var15], (double)var2[var15]);
            if (var17 < var16.y2()) {
               var19 = true;
               if (K != null && b((var16.x1() + var16.x2()) * 0.5, (var16.z1() + var16.z2()) * 0.5)) {
                  Constants.LOG
                     .info(
                        "[SR-SEGDUMP] capture DROP: seg {} sys={} y2 {} -> {}{}",
                        var15,
                        var1[var15],
                        String.format(Locale.ROOT, "%.2f", var16.y2()),
                        String.format(Locale.ROOT, "%.2f", var17),
                        var2[var15] != Integer.MIN_VALUE && var13[var15] < (double)var2[var15] ? " (held at lake floor " + var2[var15] + ")" : ""
                     );
               }

               var0.set(var15, new RiverSegment(var16.x1(), var16.z1(), Math.max(var16.y1(), var17), var16.w1(), var16.x2(), var16.z2(), var17, var16.w2()));
            }
         }

         return var19;
      }
   }

   private static int a(List<RiverSegment> var0, StreamsConfig var1) {
      int var2 = var0.size();
      if (var2 != 0 && !Boolean.getBoolean("streamsreflowing.nowidthwaist")) {
         int[] var3 = chainIds(var0);
         HashMap var4 = new HashMap();
         HashMap var5 = new HashMap();

         for (int var6 = 0; var6 < var2; var6++) {
            RiverSegment var7 = (RiverSegment)var0.get(var6);
            var4.computeIfAbsent(var3[var6], var0x -> new HashMap<>()).put(a(var7.x1(), var7.z1()), var6);
            var5.computeIfAbsent(var3[var6], var0x -> new HashSet<>()).add(a(var7.x2(), var7.z2()));
         }

         ArrayList var25 = new ArrayList();

         for (Entry var8 : var4.entrySet()) {
            Set var9 = (Set)var5.get(var8.getKey());

            for (Entry var11 : ((Map)var8.getValue()).entrySet()) {
               if (!var9.contains(var11.getKey())) {
                  var25.add(new int[]{(Integer)var8.getKey(), (Integer)var11.getValue()});
                  break;
               }
            }
         }

         HashMap var27 = new HashMap();
         int var28 = 0;

         for (int var29 = 0; var29 < 64; var29++) {
            boolean var30 = false;

            for (int[] var12 : var25) {
               Map var13 = (Map)var4.get(var12[0]);
               RiverSegment var14 = (RiverSegment)var0.get(var12[1]);
               double var15 = var27.getOrDefault(a(var14.x1(), var14.z1()), 0.0);
               int var17 = var12[1];
               long var18 = 0L;

               for (int var20 = 0; var20 < var2 && var17 >= 0; var20++) {
                  RiverSegment var21 = (RiverSegment)var0.get(var17);
                  var15 = Math.max(var15, var21.w1());
                  double var22 = Math.max(var15 * 0.5, a(var15, var1));
                  if (var21.w1() < var22 || var21.w2() < var22) {
                     var0.set(
                        var17,
                        new RiverSegment(
                           var21.x1(), var21.z1(), var21.y1(), Math.max(var21.w1(), var22), var21.x2(), var21.z2(), var21.y2(), Math.max(var21.w2(), var22)
                        )
                     );
                     var28++;
                  }

                  var15 = Math.max(var15, var21.w2());
                  var18 = a(var21.x2(), var21.z2());
                  Integer var24 = (Integer)var13.get(var18);
                  var17 = var24 == null ? -1 : var24;
               }

               Double var33 = (Double)var27.get(var18);
               if (var33 == null || var15 > var33) {
                  var27.put(var18, var15);
                  var30 = true;
               }
            }

            if (!var30) {
               break;
            }
         }

         return var28;
      } else {
         return 0;
      }
   }

   private static double a(double var0, StreamsConfig var2) {
      int var3 = RiverEngine.insetFor(var0, var2);
      double var4 = 0.0;
      double var6 = var0;

      for (int var8 = 0; var8 < 24; var8++) {
         double var9 = (var4 + var6) * 0.5;
         if (RiverEngine.insetFor(var9, var2) >= var3) {
            var6 = var9;
         } else {
            var4 = var9;
         }
      }

      return var6;
   }

   private static int b(List<RiverSegment> var0, StreamsConfig var1) {
      int var2 = var0.size();
      if (var2 >= 2 && !Boolean.getBoolean("streamsreflowing.nomergecrossings")) {
         int[] var3 = chainIds(var0);
         HashMap var4 = new HashMap(var2 * 2);
         HashMap var5 = new HashMap();

         for (int var6 = 0; var6 < var2; var6++) {
            RiverSegment var7 = (RiverSegment)var0.get(var6);
            long[] var8 = new long[]{a(var7.x1(), var7.z1()), a(var7.x2(), var7.z2())};

            for (long var12 : var8) {
               List var14 = var4.computeIfAbsent(var12, var0x -> new ArrayList(2));
               if (!var14.contains(var3[var6])) {
                  for (int var16 : var14) {
                     var5.computeIfAbsent(var3[var6], var0x -> new HashSet<>()).add(var16);
                     var5.computeIfAbsent(var16, var0x -> new HashSet<>()).add(var3[var6]);
                  }

                  var14.add(var3[var6]);
               }
            }
         }

         double var36 = Math.max(8.0, var1.maxWidth() * 2.0);
         HashMap var37 = new HashMap();

         for (int var38 = 0; var38 < var2; var38++) {
            RiverSegment var40 = (RiverSegment)var0.get(var38);
            int var41 = (int)Math.floor(var40.minX() / var36);
            int var43 = (int)Math.floor(var40.maxX() / var36);
            int var13 = (int)Math.floor(var40.minZ() / var36);
            int var47 = (int)Math.floor(var40.maxZ() / var36);

            for (int var50 = var41; var50 <= var43; var50++) {
               for (int var52 = var13; var52 <= var47; var52++) {
                  var37.computeIfAbsent((long)var50 << 32 ^ (long)var52 & 4294967295L, var0x -> new ArrayList<>()).add(var38);
               }
            }
         }

         double var39 = var1.maxWidth() + 1.0 + 2.0;
         boolean[] var42 = new boolean[var2];
         int var44 = 0;

         label98:
         for (int var45 = 0; var45 < var2; var45++) {
            RiverSegment var48 = (RiverSegment)var0.get(var45);
            if (!(var48.widthAt(0.5) > 3.5)) {
               double var51 = var48.widthAt(0.5);
               double var17 = (var48.x1() + var48.x2()) * 0.5;
               double var19 = (var48.z1() + var48.z2()) * 0.5;
               Set var21 = (Set)var5.get(var3[var45]);
               int var22 = (int)Math.floor((var17 - var39) / var36);
               int var23 = (int)Math.floor((var17 + var39) / var36);
               int var24 = (int)Math.floor((var19 - var39) / var36);
               int var25 = (int)Math.floor((var19 + var39) / var36);

               for (int var26 = var22; var26 <= var23; var26++) {
                  for (int var27 = var24; var27 <= var25; var27++) {
                     List var28 = (List)var37.get((long)var26 << 32 ^ (long)var27 & 4294967295L);
                     if (var28 != null) {
                        for (int var30 : var28) {
                           if (var3[var30] != var3[var45] && (var21 == null || !var21.contains(var3[var30]))) {
                              RiverSegment var31 = (RiverSegment)var0.get(var30);
                              double var32 = var31.clampParam(var17, var19);
                              double var34 = var31.widthAt(var32);
                              if (!(var34 < var51 * 3.0) && var31.distanceAt(var17, var19, var32) <= var34 - 1.0) {
                                 var42[var45] = true;
                                 var44++;
                                 continue label98;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var44 == 0) {
            return 0;
         } else {
            ArrayList var46 = new ArrayList(var2 - var44);

            for (int var49 = 0; var49 < var2; var49++) {
               if (!var42[var49]) {
                  var46.add((RiverSegment)var0.get(var49));
               }
            }

            var0.clear();
            var0.addAll(var46);
            return var44;
         }
      } else {
         return 0;
      }
   }

   private static double a(TerrainSampler var0, int var1, int var2, int var3) {
      double var4 = var0.riverSurface(var1, var2);
      if (!Double.isNaN(var4)) {
         return var4;
      } else {
         int var6 = Math.max(2, (int)Math.round((double)var3 * 0.375));

         for (int var7 = -1; var7 <= 1; var7++) {
            for (int var8 = -1; var8 <= 1; var8++) {
               if (var8 != 0 || var7 != 0) {
                  var4 = var0.riverSurface(var1 + var8 * var6, var2 + var7 * var6);
                  if (!Double.isNaN(var4)) {
                     return var4;
                  }
               }
            }
         }

         return Double.NaN;
      }
   }

   private static void a(List<RiverSegment> var0, List<Integer> var1, List<Lake> var2, boolean[] var3, int var4, StreamsConfig var5, TerrainSampler var6) {
      if (var5.growFromWater() && !var0.isEmpty()) {
         for (int var7 = 0; var7 < 64; var7++) {
            int var8 = var0.size();
            int[] var9 = chainIds(var0);
            HashMap var10 = new HashMap(var8 * 2);

            for (int var11 = 0; var11 < var8; var11++) {
               RiverSegment var12 = (RiverSegment)var0.get(var11);
               var10.putIfAbsent(a(var12.x1(), var12.z1()), var9[var11]);
            }

            HashMap var20 = new HashMap();

            for (int var21 = 0; var21 < var8; var21++) {
               var20.put(var9[var21], var21);
            }

            HashSet var22 = new HashSet();

            for (Entry var14 : var20.entrySet()) {
               RiverSegment var15 = (RiverSegment)var0.get((Integer)var14.getValue());
               if (!(var15.y2() <= (double)(var4 + 1))) {
                  boolean var16 = false;

                  for (int var17 = 0; var17 < var2.size(); var17++) {
                     if (!var3[var17] && ((Lake)var2.get(var17)).covers((int)Math.floor(var15.x2()), (int)Math.floor(var15.z2()))) {
                        var16 = true;
                        break;
                     }
                  }

                  if (!var16) {
                     Integer var25 = (Integer)var10.get(a(var15.x2(), var15.z2()));
                     if (var25 == null || var25.equals(var14.getKey())) {
                        int var18 = (int)Math.floor(var15.x2());
                        int var19 = (int)Math.floor(var15.z2());
                        if (Double.isNaN(a(var6, var18, var19, var5.gridSpacing())) && var6.waterBiomeClassAt(var18, (int)Math.round(var15.y2()), var19) != 1) {
                           var22.add((Integer)var14.getKey());
                        }
                     }
                  }
               }
            }

            if (var22.isEmpty()) {
               return;
            }

            int var23 = 0;

            for (int var24 = 0; var24 < var8; var24++) {
               if (!var22.contains(var9[var24])) {
                  var0.set(var23, (RiverSegment)var0.get(var24));
                  var1.set(var23, (Integer)var1.get(var24));
                  var23++;
               }
            }

            while (var0.size() > var23) {
               var0.remove(var0.size() - 1);
               var1.remove(var1.size() - 1);
            }

            Constants.LOG.debug("[SR-MERGE] orphan prune pass {}: removed {} chains", var7, var22.size());
         }
      }
   }

   private static void a(List<RiverSegment> var0, List<Lake> var1, boolean[] var2, double[] var3, int[] var4) {
      int var5 = var0.size();
      Arrays.fill(var3, Double.NaN);
      Arrays.fill(var4, -1);
      if (var5 != 0) {
         int[] var6 = chainIds(var0);
         double[] var7 = new double[var5];
         int[] var8 = new int[var5];

         for (int var9 = 0; var9 < var5; var9++) {
            RiverSegment var10 = (RiverSegment)var0.get(var9);
            var7[var9] = Double.NaN;
            var8[var9] = -1;
            int var11 = (int)Math.floor((var10.x1() + var10.x2()) * 0.5);
            int var12 = (int)Math.floor((var10.z1() + var10.z2()) * 0.5);

            for (int var13 = 0; var13 < var1.size(); var13++) {
               if (!var2[var13]) {
                  Lake var14 = (Lake)var1.get(var13);
                  if (var14.covers(var11, var12) && (Double.isNaN(var7[var9]) || (double)var14.level() > var7[var9])) {
                     var7[var9] = (double)var14.level();
                     var8[var9] = var13;
                  }
               }
            }
         }

         boolean var18 = true;

         while (var18) {
            var18 = false;
            HashMap var19 = new HashMap();

            for (int var20 = 0; var20 < var5; var20++) {
               if (!Double.isNaN(var3[var20])) {
                  RiverSegment var22 = (RiverSegment)var0.get(var20);
                  long var24 = a(var22.x1(), var22.z1());
                  double[] var15 = (double[])var19.get(var24);
                  if (var15 == null || var3[var20] > var15[0]) {
                     var19.put(var24, new double[]{var3[var20], (double)var4[var20]});
                  }
               }
            }

            int var21 = Integer.MIN_VALUE;
            double var23 = Double.NaN;
            int var25 = -1;

            for (int var26 = var5 - 1; var26 >= 0; var26--) {
               if (var6[var26] != var21) {
                  var21 = var6[var26];
                  RiverSegment var16 = (RiverSegment)var0.get(var26);
                  double[] var17 = (double[])var19.get(a(var16.x2(), var16.z2()));
                  var23 = var17 != null ? var17[0] : Double.NaN;
                  var25 = var17 != null ? (int)var17[1] : -1;
               }

               if (!Double.isNaN(var7[var26]) && (Double.isNaN(var23) || var7[var26] > var23)) {
                  var23 = var7[var26];
                  var25 = var8[var26];
               }

               if (!Double.isNaN(var23) && (Double.isNaN(var3[var26]) || var23 > var3[var26] + 1.0E-9)) {
                  var3[var26] = var23;
                  var4[var26] = var25;
                  var18 = true;
               }
            }
         }
      }
   }

   public static int[] chainIds(List<RiverSegment> var0) {
      int var1 = var0.size();
      HashMap var2 = new HashMap(var1 * 2);
      HashMap var3 = new HashMap(var1 * 2);
      HashMap var4 = new HashMap(var1 * 2);
      HashMap var5 = new HashMap(var1 * 2);

      for (int var6 = 0; var6 < var1; var6++) {
         RiverSegment var7 = (RiverSegment)var0.get(var6);
         long var8 = a(var7.x1(), var7.z1());
         long var10 = a(var7.x2(), var7.z2());
         var2.merge(var8, 1, Integer::sum);
         var4.put(var8, var6);
         var3.merge(var10, 1, Integer::sum);
         var5.put(var10, var6);
      }

      int[] var12 = new int[var1];
      int var13 = 0;

      while (var13 < var1) {
         var12[var13] = var13++;
      }

      for (Entry var16 : var4.entrySet()) {
         long var9 = (Long)var16.getKey();
         if (var2.getOrDefault(var9, 0) == 1 && var3.getOrDefault(var9, 0) == 1) {
            a(var12, ((Integer)var5.get(var9)).intValue(), ((Integer)var16.getValue()).intValue());
         }
      }

      int[] var15 = new int[var1];

      for (int var17 = 0; var17 < var1; var17++) {
         var15[var17] = a(var12, var17);
      }

      return var15;
   }

   private static long a(double var0, double var2) {
      long var4 = Math.round(var0 * 16.0) & 4294967295L;
      long var6 = Math.round(var2 * 16.0) & 4294967295L;
      return var4 << 32 | var6;
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

   private static List<RiverSegment> a(
      int var0,
      int[] var1,
      int[] var2,
      List<Lake> var3,
      Lake var4,
      boolean[] var5,
      int[] var6,
      double[] var7,
      double[] var8,
      LowResGrid var9,
      TerrainSampler var10,
      WetnessSampler var11,
      StreamsConfig var12,
      long var13,
      int var15
   ) {
      ArrayList var16 = new ArrayList();
      ColumnCache var17 = new ColumnCache();
      ArrayList var18 = new ArrayList();
      var18.add(new NetworkTracer.Pt((double)var9.worldX(var9.gi(var0)), (double)var9.worldZ(var9.gj(var0)), var7[var0], var8[var0]));
      int var19 = var0;
      boolean var20 = false;
      int var21 = -1;
      int var22 = -1;
      double var23 = Double.NaN;
      Lake var25 = null;

      while (true) {
         int var26 = var1[var19];
         if (var26 < 0) {
            var20 = true;
            break;
         }

         if (var2[var26] >= 0) {
            var25 = (Lake)var3.get(var2[var26]);
            var18.add(new NetworkTracer.Pt((double)var9.worldX(var9.gi(var26)), (double)var9.worldZ(var9.gj(var26)), (double)var25.level(), var8[var19]));
            var23 = (double)var25.level();
            break;
         }

         if (var9.riverOutlet(var26)) {
            double var27 = var7[var26];
            var18.add(new NetworkTracer.Pt((double)var9.worldX(var9.gi(var26)), (double)var9.worldZ(var9.gj(var26)), Math.min(var7[var19], var27), var8[var19]));
            var23 = var27;
            var22 = var26;
            break;
         }

         if (!var5[var26]) {
            var18.add(new NetworkTracer.Pt((double)var9.worldX(var9.gi(var26)), (double)var9.worldZ(var9.gj(var26)), (double)var15, var8[var19]));
            var23 = (double)var15;
            var21 = var26;
            break;
         }

         var18.add(new NetworkTracer.Pt((double)var9.worldX(var9.gi(var26)), (double)var9.worldZ(var9.gj(var26)), var7[var26], var8[var26]));
         if (var6[var26] != 1) {
            break;
         }

         var19 = var26;
      }

      boolean var47 = false;
      double var48 = 0.0;
      double var29 = 0.0;
      double var31 = 0.0;
      if (var6[var0] == 0) {
         int var33 = var9.gi(var0);
         int var34 = var9.gj(var0);
         int var35 = -1;
         int var36 = Integer.MAX_VALUE;

         for (int var37 = -2; var37 <= 2; var37++) {
            for (int var38 = -2; var38 <= 2; var38++) {
               int var39 = var33 + var38;
               int var40 = var34 + var37;
               if (var9.valid(var39, var40) && var2[var9.idx(var39, var40)] >= 0) {
                  int var41 = var38 * var38 + var37 * var37;
                  if (var41 < var36) {
                     var36 = var41;
                     var35 = var9.idx(var39, var40);
                  }
               }
            }
         }

         if (var35 >= 0 && Math.abs((double)((Lake)var3.get(var2[var35])).level() - (var7[var0] - var12.ditchInset())) <= 4.0) {
            var47 = true;
            var48 = (double)var9.worldX(var9.gi(var35));
            var29 = (double)var9.worldZ(var9.gj(var35));
            var31 = (double)((Lake)var3.get(var2[var35])).level();
            if (var25 != null && (double)var25.level() >= var31 - 1.0) {
               return var16;
            }
         }
      }

      if (var21 >= 0
         && var6[var0] == 0
         && !var47
         && var18.size() - 1 < var12.minSeaChainNodes()
         && var11.wetness((int)((NetworkTracer.Pt)var18.get(0)).x(), (int)var7[var0], (int)((NetworkTracer.Pt)var18.get(0)).z()) > var12.wetnessDryCutoff()) {
         return var16;
      } else {
         boolean var49 = var6[var0] == 0 && !var47;
         long var50 = System.nanoTime();
         Object var51 = var12.valleySnapRadius() > 0 ? a(var18, var10, var17, var12, var49, var3) : (var49 ? a(var18, var10, var17) : var18);
         P.addAndGet(0, System.nanoTime() - var50);
         if (var12.valleySnapRadius() > 0 && var51.size() >= 2) {
            double var52 = ((NetworkTracer.Pt)var18.get(0)).y();
            double var60 = a(var10, var17, ((NetworkTracer.Pt)var18.get(0)).x(), ((NetworkTracer.Pt)var18.get(0)).z());
            double var63 = ((NetworkTracer.Pt)var51.get(0)).y();
            long var43 = System.nanoTime();
            var51 = a(a((List<NetworkTracer.Pt>)var51, (double)var12.gridSpacing() * 0.4), var10, var17, var12, var3);
            P.addAndGet(1, System.nanoTime() - var43);
            if (var49 && ((NetworkTracer.Pt)var51.get(0)).y() < var60 - 8.0) {
               Constants.LOG
                  .debug(
                     "[SR-SRCTRACE] dug source ({},{}) fineGround={}: coarse={} -> snapToValley={} -> followGround={}",
                     (int)Math.round(((NetworkTracer.Pt)var18.get(0)).x()),
                     (int)Math.round(((NetworkTracer.Pt)var18.get(0)).z()),
                     (int)var60,
                     String.format("%.1f", var52),
                     String.format("%.1f", var63),
                     String.format("%.1f", ((NetworkTracer.Pt)var51.get(0)).y())
                  );
            }
         }

         if (var49 && !var51.isEmpty() && ((NetworkTracer.Pt)var51.get(0)).y() < (double)(var15 + 2)) {
            return var16;
         } else {
            if (var47 && !var51.isEmpty()) {
               ArrayList var53 = new ArrayList(var51.size() + 1);
               var53.add(
                  new NetworkTracer.Pt(
                     var48,
                     var29,
                     var31 + (double)RiverEngine.insetFor(b(((NetworkTracer.Pt)var51.get(0)).acc(), var12), var12),
                     ((NetworkTracer.Pt)var51.get(0)).acc()
                  )
               );
               var53.addAll((Collection)var51);
               var51 = var53;
            }

            Lake var54 = var25 != null ? var25 : var4;
            if (var54 != null && var51.size() >= 2) {
               int var55 = var51.size();
               var51 = a((List<NetworkTracer.Pt>)var51, var54, var10, var17, var12);
               if (var51.size() < var55 || var51.size() < 2) {
                  return var16;
               }
            }

            if (var51.size() >= 2) {
               if (var21 >= 0) {
                  List var56 = a((List<NetworkTracer.Pt>)var51, var9, var21, var15, var10, var12);
                  if (var56 == var51) {
                     return var16;
                  }

                  var51 = var56;
               } else if (var22 >= 0) {
                  List var57 = a((List<NetworkTracer.Pt>)var51, var9, var22, var10, var12);
                  if (var57 == var51) {
                     return var16;
                  }

                  var51 = var57;
               } else if (!Double.isNaN(var23)) {
                  var51 = b((List<NetworkTracer.Pt>)var51, var23, 8.0);
               } else if (var20) {
                  List var58 = a((List<NetworkTracer.Pt>)var51, var9, var19, var15, var10, var12);
                  if (var58 == var51) {
                     return var16;
                  }

                  var51 = var58;
               }

               long var59 = System.nanoTime();
               List var61 = a(a((List<NetworkTracer.Pt>)var51, (double)var12.gridSpacing() * 0.4), var13, var0, var12);
               long var64 = System.nanoTime();
               if (var12.confineToBanks()) {
                  var61 = a(var61, var10, var17, var12);
               }

               long var65 = System.nanoTime();
               var61 = a(var61, var12.smoothingPasses());
               long var45 = System.nanoTime();
               a(var61, var12, var16, var49);
               P.addAndGet(2, var64 - var59);
               P.addAndGet(3, var65 - var64);
               P.addAndGet(4, var45 - var65);
               P.addAndGet(5, System.nanoTime() - var45);
            }

            return var16;
         }
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, Lake var1, TerrainSampler var2, ColumnCache var3, StreamsConfig var4) {
      int var5 = var1.level();
      ArrayList var6 = new ArrayList(var0.size());
      boolean var7 = false;

      for (NetworkTracer.Pt var9 : var0) {
         if (!var1.covers((int)Math.floor(var9.x()), (int)Math.floor(var9.z()))) {
            double var10 = a(var2, var3, var9.x(), var9.z());
            if (var10 < (double)var5 - 4.0) {
               if (K != null && b(var9.x(), var9.z())) {
                  Constants.LOG
                     .info("[SR-SEGDUMP] modifier lakeApproach TRUNCATE at ({},{}) ground={} < lake {} - {}", (int)var9.x(), (int)var9.z(), (int)var10, var5, 4);
               }
               break;
            }

            double var12 = (double)(var5 + RiverEngine.insetFor(b(var9.acc(), var4), var4));
            if (var9.y() < var12) {
               if (!var7 && K != null && b(var9.x(), var9.z())) {
                  Constants.LOG
                     .info(
                        "[SR-SEGDUMP] modifier lakeApproach HOLD from ({},{}) y {} -> {} (lake {} + inset)",
                        (int)var9.x(),
                        (int)var9.z(),
                        String.format(Locale.ROOT, "%.1f", var9.y()),
                        String.format(Locale.ROOT, "%.1f", var12),
                        var5
                     );
                  var7 = true;
               }

               var9 = new NetworkTracer.Pt(var9.x(), var9.z(), var12, var9.acc());
            }
         }

         var6.add(var9);
      }

      return var6;
   }

   public static RiverNetwork assemble(PlateKey var0, List<RiverSegment> var1, List<Lake> var2, StreamsConfig var3) {
      NearestRiverIndex var4 = new NearestRiverIndex(var1, var3.maxWidth() * (1.0 + var3.bankWidth()) + 2.0, Math.max(16, var3.gridSpacing()), var3.bankWidth());
      return new RiverNetwork(var0, var1, var4, var2);
   }

   private static int[] a() {
      String var0 = System.getProperty("streamsreflowing.laketrace");
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim())};
         } catch (RuntimeException var2) {
            System.out.println("[SR-LAKETRACE] bad streamsreflowing.laketrace value: " + var0);
            return null;
         }
      } else {
         return null;
      }
   }

   private static int[] b() {
      String var0 = System.getProperty("streamsreflowing.segdump");
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            int var2 = var1.length >= 3 ? Integer.parseInt(var1[2].trim()) : 64;
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim()), var2};
         } catch (RuntimeException var3) {
            System.out.println("[SR-SEGDUMP] bad streamsreflowing.segdump value: " + var0);
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean b(double var0, double var2) {
      return K != null && Math.abs(var0 - (double)K[0]) <= (double)K[2] && Math.abs(var2 - (double)K[1]) <= (double)K[2];
   }

   public static void dumpLoadedSegments(PlateKey var0, List<RiverSegment> var1, List<Lake> var2) {
      if (K != null) {
         int[] var3 = new int[var1.size()];
         Arrays.fill(var3, -1);
         a("plate " + var0 + " (LOADED from store)", var1, var3, var2);
      }
   }

   private static void a(PlateDomain var0, List<RiverSegment> var1, int[] var2, List<Lake> var3) {
      a("plate " + var0.key(), var1, var2, var3);
   }

   private static void a(String var0, List<RiverSegment> var1, int[] var2, List<Lake> var3) {
      int[] var4 = chainIds(var1);
      StringBuilder var5 = new StringBuilder();
      int var6 = 0;

      for (int var7 = 0; var7 < var1.size(); var7++) {
         RiverSegment var8 = (RiverSegment)var1.get(var7);
         double var9 = (var8.x1() + var8.x2()) * 0.5;
         double var11 = (var8.z1() + var8.z2()) * 0.5;
         if (b(var9, var11)) {
            var5.append(
               String.format(
                  Locale.ROOT,
                  "%n  seg %5d sys=%-10d chain=%-5d (%8.1f,%8.1f y=%7.2f w=%4.1f) -> (%8.1f,%8.1f y=%7.2f w=%4.1f)",
                  var7,
                  var2[var7],
                  var4[var7],
                  var8.x1(),
                  var8.z1(),
                  var8.y1(),
                  var8.w1(),
                  var8.x2(),
                  var8.z2(),
                  var8.y2(),
                  var8.w2()
               )
            );
            var6++;
         }
      }

      for (Lake var19 : var3) {
         int var20 = var19.cellMinX();
         int var10 = var19.cellMinZ();
         int var21 = var20 + var19.cellsWide() * var19.spacing();
         int var12 = var10 + var19.cellsHigh() * var19.spacing();
         double var13 = (double)(var20 + var21) * 0.5;
         double var15 = (double)(var10 + var12) * 0.5;
         boolean var17 = b(var13, var15) || K != null && K[0] >= var20 - K[2] && K[0] <= var21 + K[2] && K[1] >= var10 - K[2] && K[1] <= var12 + K[2];
         if (var17) {
            var5.append(
               String.format(
                  Locale.ROOT,
                  "%n  lake level=%d dig=%d bbox=[%d..%d, %d..%d] covers-target=%s",
                  var19.level(),
                  var19.digDepth(),
                  var20,
                  var21,
                  var10,
                  var12,
                  var19.covers(K[0], K[1])
               )
            );
         }
      }

      if (var6 > 0 || var5.length() > 0) {
         Constants.LOG.info("[SR-SEGDUMP] {} segments/lakes within {} of ({},{}):{}", var0, K[2], K[0], K[1], var5);
      }
   }

   private static void a(
      int var0,
      int var1,
      List<Integer> var2,
      LowResGrid var3,
      double[] var4,
      int[] var5,
      double[] var6,
      boolean[] var7,
      int[] var8,
      int var9,
      PlateDomain var10
   ) {
      ArrayList var11 = new ArrayList();

      for (int var13 : var2) {
         if (Math.abs(var3.worldX(var3.gi(var13)) - var0) <= 96 && Math.abs(var3.worldZ(var3.gj(var13)) - var1) <= 96) {
            var11.add(var13);
         }
      }

      if (!var11.isEmpty()) {
         var11.sort((var1x, var2x) -> {
            int var3x = var3.worldZ(var3.gj(var1x));
            int var4x = var3.worldZ(var3.gj(var2x));
            return var3x != var4x ? Integer.compare(var3x, var4x) : Integer.compare(var3.worldX(var3.gi(var1x)), var3.worldX(var3.gi(var2x)));
         });
         StringBuilder var30 = new StringBuilder();
         var30.append(
            String.format("%n[SR-LAKETRACE] ===== plate=%s target=(%d,%d) sea=%d window=%d cells=%d =====%n", var10.key(), var0, var1, var9, 96, var11.size())
         );
         var30.append(String.format("[SR-LAKETRACE]   worldX  worldZ  terrainH  filled  f-h   acc    lakeId  flags%n"));
         int var31 = 0;
         int var14 = 0;

         for (int var16 : var11) {
            int var17 = var3.worldX(var3.gi(var16));
            int var18 = var3.worldZ(var3.gj(var16));
            double var19 = var3.height(var16);
            double var21 = var4[var16];
            boolean var23 = var19 <= (double)var9;
            boolean var24 = var21 - var19 > 0.001;
            boolean var25 = a(var16, var4, var3, var9);
            boolean var26 = var21 <= (double)var9;
            StringBuilder var27 = new StringBuilder();
            if (var23) {
               var27.append("OUTLET ");
            }

            if (var24) {
               var27.append("RAISED ");
            }

            if (var25) {
               var27.append("BASIN ");
            }

            if (var26) {
               var27.append("SUBSEA ");
            }

            if (var7[var16]) {
               var27.append("RIVER ");
            }

            if (var8[var16] >= 0) {
               var27.append("LAKE ");
            }

            if (var8[var16] >= 0) {
               var31++;
            }

            if (var24 && var26 && !var25) {
               var14++;
            }

            int var28 = var5[var16];
            String var29 = var28 >= 0 ? "->(" + var3.worldX(var3.gi(var28)) + "," + var3.worldZ(var3.gj(var28)) + ")" : "sink";
            var30.append(
               String.format(
                  "[SR-LAKETRACE]  %7d %7d %9.1f %7.1f %5.1f %6.1f %6d   %s%s%n",
                  var17,
                  var18,
                  var19,
                  var21,
                  var21 - var19,
                  var6[var16],
                  var8[var16],
                  var27.toString().trim(),
                  " " + var29
               )
            );
         }

         var30.append(
            String.format(
               "[SR-LAKETRACE] summary: %d cells in window, %d are lake cells, %d are RAISED+SUBSEA but NOT basin-eligible (the perch source: a sub-sea fill that isBasinCell rejects because filled<=sea, so it never becomes a contained lake)%n",
               var11.size(),
               var31,
               var14
            )
         );
         var30.append(
            String.format(
               "[SR-LAKETRACE] legend: OUTLET=seeded as ocean (terrainH<=sea); RAISED=Priority-Flood lifted it above terrain; BASIN=isBasinCell true (lake-eligible); SUBSEA=filled<=sea; RIVER=carries a stream; LAKE=assigned to a lake%n"
            )
         );
         System.out.print(var30);
         System.out.flush();
      }
   }

   private static boolean a(int var0, double[] var1, LowResGrid var2, int var3) {
      return a(var0, var1, var2, var3, 0.5);
   }

   private static boolean a(int var0, double[] var1, LowResGrid var2, int var3, double var4) {
      return var2.inDomain(var0) && !var2.riverOutlet(var0) && var1[var0] > (double)var3 && var1[var0] - var2.height(var0) > var4;
   }

   private static boolean a(int var0, int[] var1, double[] var2, int var3, double var4, double var6, int var8) {
      int var9 = var0;

      for (int var10 = 0; var9 >= 0 && var10 < var8; var10++) {
         if (var2[var9] <= (double)var3) {
            return true;
         }

         if (var4 - var2[var9] >= var6) {
            return true;
         }

         var9 = var1[var9];
      }

      return false;
   }

   private static NetworkTracer.LocalBasin a(LowResGrid var0, TerrainSampler var1, List<Integer> var2, int var3) {
      int var4 = Integer.MAX_VALUE;
      int var5 = Integer.MIN_VALUE;
      int var6 = Integer.MAX_VALUE;
      int var7 = Integer.MIN_VALUE;

      for (int var9 : var2) {
         var4 = Math.min(var4, var0.gi(var9));
         var5 = Math.max(var5, var0.gi(var9));
         var6 = Math.min(var6, var0.gj(var9));
         var7 = Math.max(var7, var0.gj(var9));
      }

      var4 -= 8;
      var5 += 8;
      var6 -= 8;
      var7 += 8;
      if (var5 - var4 + 1 > 32) {
         int var38 = var4 + var5 >> 1;
         var4 = var38 - 16;
         var5 = var38 + 15;
      }

      if (var7 - var6 + 1 > 32) {
         int var39 = var6 + var7 >> 1;
         var6 = var39 - 16;
         var7 = var39 + 15;
      }

      int var40 = var5 - var4 + 1;
      int var41 = var7 - var6 + 1;
      double[] var10 = new double[var40 * var41];
      boolean[] var11 = new boolean[var40 * var41];

      for (int var12 = 0; var12 < var41; var12++) {
         for (int var13 = 0; var13 < var40; var13++) {
            var11[var12 * var40 + var13] = var0.valid(var4 + var13, var6 + var12);
         }
      }

      int var42 = var4;
      int var43 = var6;
      LowResGrid.parallelFor(var40 * var41, var7x -> {
         if (var11[var7x]) {
            var10[var7x] = var1.groundHeightExact(var0.worldX(var42 + var7x % var40), var0.worldZ(var43 + var7x / var40));
         }
      });
      double[] var15 = new double[var40 * var41];
      Arrays.fill(var15, Double.MAX_VALUE);
      PriorityQueue var16 = new PriorityQueue<>((var0x, var1x) -> Double.compare(var0x[0], var1x[0]));

      for (int var17 = 0; var17 < var41; var17++) {
         for (int var18 = 0; var18 < var40; var18++) {
            int var19 = var17 * var40 + var18;
            if (var11[var19]) {
               boolean var20 = var18 == 0 || var17 == 0 || var18 == var40 - 1 || var17 == var41 - 1;
               if (!var20) {
                  for (int var21 = 0; var21 < 8 && !var20; var21++) {
                     int var22 = var18 + a[var21];
                     int var23 = var17 + b[var21];
                     var20 = !var11[var23 * var40 + var22];
                  }
               }

               if (var20) {
                  var15[var19] = var10[var19];
                  var16.add(new double[]{var10[var19], (double)var19});
               }
            }
         }
      }

      while (!var16.isEmpty()) {
         double[] var44 = (double[])var16.poll();
         int var46 = (int)var44[1];
         if (!(var44[0] > var15[var46] + 1.0E-9)) {
            int var48 = var46 % var40;
            int var50 = var46 / var40;

            for (int var52 = 0; var52 < 8; var52++) {
               int var53 = var48 + a[var52];
               int var55 = var50 + b[var52];
               if (var53 >= 0 && var53 < var40 && var55 >= 0 && var55 < var41 && var11[var55 * var40 + var53]) {
                  int var24 = var55 * var40 + var53;
                  double var25 = Math.max(var10[var24], var15[var46]);
                  if (var25 < var15[var24]) {
                     var15[var24] = var25;
                     var16.add(new double[]{var25, (double)var24});
                  }
               }
            }
         }
      }

      int var45 = var0.gi(var3) - var4;
      int var47 = var0.gj(var3) - var6;
      if (var45 >= 0 && var45 < var40 && var47 >= 0 && var47 < var41 && var11[var47 * var40 + var45]) {
         int var49 = var47 * var40 + var45;
         if (var15[var49] - var10[var49] <= 0.001) {
            return null;
         } else {
            double var51 = var15[var49];
            double var54 = var10[var49];
            int var56 = var49;
            boolean[] var57 = new boolean[var40 * var41];
            ArrayDeque var26 = new ArrayDeque();
            var57[var49] = true;
            var26.add(var49);

            while (!var26.isEmpty()) {
               int var27 = (Integer)var26.poll();
               if (var10[var27] < var54) {
                  var54 = var10[var27];
                  var56 = var27;
               }

               int var28 = var27 % var40;
               int var29 = var27 / var40;

               for (int var30 = 0; var30 < 8; var30++) {
                  int var31 = var28 + a[var30];
                  int var32 = var29 + b[var30];
                  if (var31 >= 0 && var31 < var40 && var32 >= 0 && var32 < var41) {
                     int var33 = var32 * var40 + var31;
                     if (var11[var33] && !var57[var33] && !(var10[var33] >= var51 - 1.0E-6)) {
                        var57[var33] = true;
                        var26.add(var33);
                     }
                  }
               }
            }

            return new NetworkTracer.LocalBasin(var4, var6, var40, var41, var10, var11, var56, var51, var54);
         }
      } else {
         return null;
      }
   }

   private static double a(List<Integer> var0, LowResGrid var1, TerrainSampler var2) {
      double var3 = 0.0;
      double var5 = 0.0;
      int var7 = 0;

      for (int var9 : var0) {
         int var10 = var1.gi(var9);
         int var11 = var1.gj(var9);

         for (int var12 = 0; var12 < 8; var12++) {
            int var13 = var10 + a[var12];
            int var14 = var11 + b[var12];
            if (!var1.valid(var13, var14) || !var1.inDomain(var1.idx(var13, var14))) {
               var3 += (double)(var1.worldX(var10) + var1.worldX(var13)) * 0.5;
               var5 += (double)(var1.worldZ(var11) + var1.worldZ(var14)) * 0.5;
               var7++;
            }
         }
      }

      if (var7 == 0) {
         return Double.NaN;
      } else {
         double var20 = var3 / (double)var7;
         double var21 = var5 / (double)var7;
         double var22 = (double)var1.spacing;
         double var23 = Double.MAX_VALUE;

         for (double var16 = -var22; var16 <= var22 + 1.0E-9; var16 += var22 / 2.0) {
            for (double var18 = -var22; var18 <= var22 + 1.0E-9; var18 += var22 / 2.0) {
               var23 = Math.min(var23, a(var2, var20 + var18, var21 + var16));
            }
         }

         return var23;
      }
   }

   private static boolean a(LowResGrid var0, boolean[] var1, int var2, int var3, int var4) {
      if (var2 == var3) {
         return true;
      } else {
         int var5 = var0.gi(var2);
         int var6 = var0.gj(var2);

         for (int var7 = -2; var7 <= 2; var7++) {
            for (int var8 = -2; var8 <= 2; var8++) {
               if ((var8 != 0 || var7 != 0) && (!var0.valid(var5 + var8, var6 + var7) || !var1[var0.idx(var5 + var8, var6 + var7)])) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private static Lake a(List<Integer> var0, int var1, TerrainSampler var2, LowResGrid var3, PlateDomain var4, double var5, double[] var7) {
      byte var8 = 3;
      int var9 = 3 * var3.spacing;
      int var10 = var3.spacing >> 1;
      int var11 = Integer.MAX_VALUE;
      int var12 = Integer.MAX_VALUE;
      int var13 = Integer.MIN_VALUE;
      int var14 = Integer.MIN_VALUE;
      boolean[] var15 = new boolean[var3.size()];

      for (int var17 : var0) {
         var15[var17] = true;
      }

      for (int var51 : var0) {
         int var18 = var3.worldX(var3.gi(var51));
         int var19 = var3.worldZ(var3.gj(var51));
         var11 = Math.min(var11, var18);
         var13 = Math.max(var13, var18);
         var12 = Math.min(var12, var19);
         var14 = Math.max(var14, var19);
      }

      int var50 = var11 - var10 - var9;
      int var52 = var12 - var10 - var9;
      int var53 = (var13 + var10 + var9 - var50) / 3 + 1;
      int var54 = (var14 + var10 + var9 - var52) / 3 + 1;
      long[] var20 = new long[var53 * var54 + 63 >>> 6];
      boolean[] var21 = new boolean[var53 * var54];
      ArrayDeque var22 = new ArrayDeque();
      ArrayList var23 = new ArrayList(var0);
      var23.sort((var1x, var2x) -> Double.compare(var3.height(var1x), var3.height(var2x)));
      int var24 = -1;

      for (int var25 = 0; var25 < Math.min(4, var23.size()); var25++) {
         int var26 = (Integer)var23.get(var25);
         if (var2.groundHeight(var3.worldX(var3.gi(var26)), var3.worldZ(var3.gj(var26))) < (double)var1) {
            var24 = var26;
            break;
         }
      }

      if (var24 < 0) {
         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-FINEFLOOD] floor-dry level={} fill={}", var1, var0.size());
         }

         return null;
      } else {
         ArrayList var55 = new ArrayList();

         for (int var27 : var0) {
            if (a(var3, var15, var27, var24, var1)) {
               var55.add(var27);
            }
         }

         long var57 = System.nanoTime();
         LowResGrid.parallelFor(var55.size(), var3x -> {
            int var4x = (Integer)var55.get(var3x);
            var2.groundHeight(var3.worldX(var3.gi(var4x)), var3.worldZ(var3.gj(var4x)));
         });
         X.get()[7] += System.nanoTime() - var57;
         int var58 = 0;

         for (int var28 : var55) {
            int var29 = var3.worldX(var3.gi(var28));
            int var30 = var3.worldZ(var3.gj(var28));
            double var31 = var2.groundHeight(var29, var30);
            if (var31 >= (double)var1) {
               var58++;
            } else {
               if (var7 != null) {
                  var7[0] += (double)var1 - var31;
                  var7[1]++;
               }

               int var33 = (var29 - var10 - var50) / 3;
               int var34 = (var29 + var10 - var50) / 3;
               int var35 = (var30 - var10 - var52) / 3;
               int var36 = (var30 + var10 - var52) / 3;

               for (int var37 = var35; var37 <= var36; var37++) {
                  for (int var38 = var33; var38 <= var34; var38++) {
                     if (var38 >= 0 && var38 < var53 && var37 >= 0 && var37 < var54) {
                        int var39 = var37 * var53 + var38;
                        if (!var21[var39]) {
                           var21[var39] = true;
                           var20[var39 >>> 6] = var20[var39 >>> 6] | 1L << (var39 & 63);
                           var22.add(var39);
                        }
                     }
                  }
               }
            }
         }

         if (var58 * 2 > var55.size()) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
               Constants.LOG.info("[SR-FINEFLOOD] interior-dry {}/{} level={} fill={}", var58, var55.size(), var1, var0.size());
            }

            return null;
         } else {
            boolean[] var60 = new boolean[var53 * var54];
            int var61 = var10 / 3 + 5;
            boolean[] var63 = var60;
            ArrayDeque var65 = new ArrayDeque();

            for (int var32 : var0) {
               if (!a(var3, var15, var32, var24, var1)) {
                  int var73 = (var3.worldX(var3.gi(var32)) - var50) / 3;
                  int var80 = (var3.worldZ(var3.gj(var32)) - var52) / 3;
                  if (var73 >= 0 && var73 < var53 && var80 >= 0 && var80 < var54 && !var63[var80 * var53 + var73]) {
                     var63[var80 * var53 + var73] = true;
                     var65.add(new int[]{var73, var80, 0});
                  }
               }
            }

            ArrayList var68 = new ArrayList();

            while (!var65.isEmpty()) {
               int[] var70 = (int[])var65.poll();
               var68.add(var70);
               if (var70[2] < var61) {
                  for (int var74 = -1; var74 <= 1; var74++) {
                     for (int var81 = -1; var81 <= 1; var81++) {
                        int var84 = var70[0] + var81;
                        int var87 = var70[1] + var74;
                        if ((var81 != 0 || var74 != 0) && var84 >= 0 && var84 < var53 && var87 >= 0 && var87 < var54 && !var63[var87 * var53 + var84]) {
                           var63[var87 * var53 + var84] = true;
                           var65.add(new int[]{var84, var87, var70[2] + 1});
                        }
                     }
                  }
               }
            }

            ArrayList var71 = var68;
            long var75 = System.nanoTime();
            LowResGrid.parallelFor(var71.size(), var4x -> {
               int[] var5x = (int[])var71.get(var4x);
               var2.groundHeight(var50 + var5x[0] * 3, var52 + var5x[1] * 3);
            });
            X.get()[7] += System.nanoTime() - var75;
            var61 = Math.max(4096, 4 * var0.size() * (var3.spacing / 3 + 1) * (var3.spacing / 3 + 1));
            int var64 = 0;
            ArrayList var66 = new ArrayList();

            while (!var22.isEmpty()) {
               var66.add((Integer)var22.poll());
            }

            var68 = new ArrayList();

            while (!var66.isEmpty()) {
               var71 = new ArrayList();

               for (int var82 : var66) {
                  int var85 = var82 % var53;
                  int var88 = var82 / var53;

                  for (int var90 = 0; var90 < 4; var90++) {
                     int var92 = var85 + (var90 == 0 ? -1 : (var90 == 1 ? 1 : 0));
                     int var94 = var88 + (var90 == 2 ? -1 : (var90 == 3 ? 1 : 0));
                     if (var92 >= 0 && var92 < var53 && var94 >= 0 && var94 < var54 && !var21[var94 * var53 + var92]) {
                        int var40 = var50 + var92 * 3;
                        int var41 = var52 + var94 * 3;
                        if (!var60[var94 * var53 + var92]) {
                           var60[var94 * var53 + var92] = true;
                           var71.add(new int[]{var40, var41});
                        }

                        int var42 = var50 + var85 * 3;
                        int var43 = var52 + var88 * 3;
                        int var44 = Integer.signum(var40 - var42);
                        int var45 = Integer.signum(var41 - var43);

                        for (int var46 = 1; var46 < 3; var46++) {
                           var71.add(new int[]{var42 + var44 * var46, var43 + var45 * var46});
                        }
                     }
                  }
               }

               if (!var71.isEmpty()) {
                  var75 = System.nanoTime();
                  LowResGrid.parallelFor(var71.size(), var2x -> {
                     int[] var3x = (int[])var71.get(var2x);
                     var2.groundHeight(var3x[0], var3x[1]);
                  });
                  X.get()[7] += System.nanoTime() - var75;
               }

               for (int var78 = 0; var78 < var66.size(); var78++) {
                  if (++var64 > var61) {
                     if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                        Constants.LOG.info("[SR-FINEFLOOD] budget level={} fill={} budget={}", var1, var0.size(), var61);
                     }

                     return null;
                  }

                  int var83 = (Integer)var66.get(var78);
                  int var86 = var83 % var53;
                  int var89 = var83 / var53;

                  for (int var91 = 0; var91 < 4; var91++) {
                     int var93 = var86 + (var91 == 0 ? -1 : (var91 == 1 ? 1 : 0));
                     int var95 = var89 + (var91 == 2 ? -1 : (var91 == 3 ? 1 : 0));
                     if (var93 >= 0 && var93 < var53 && var95 >= 0 && var95 < var54) {
                        int var96 = var95 * var53 + var93;
                        if (!var21[var96]) {
                           var21[var96] = true;
                           int var97 = var50 + var93 * 3;
                           int var98 = var52 + var95 * 3;
                           if (var2.groundHeight(var97, var98) < (double)var1) {
                              int var99 = var50 + var86 * 3;
                              int var100 = var52 + var89 * 3;
                              int var101 = Integer.signum(var97 - var99);
                              int var102 = Integer.signum(var98 - var100);
                              boolean var47 = false;

                              for (int var48 = 1; var48 < 3 && !var47; var48++) {
                                 var47 = var2.groundHeight(var99 + var101 * var48, var100 + var102 * var48) >= (double)var1;
                              }

                              if (var47) {
                                 var21[var96] = false;
                              } else {
                                 if (var4.contains((double)var97, (double)var98) && !(var4.borderDistance((double)var97, (double)var98) < var5)) {
                                    if (var93 != 0 && var93 != var53 - 1 && var95 != 0 && var95 != var54 - 1) {
                                       if (var7 != null) {
                                          var7[0] += (double)var1 - var2.groundHeight(var97, var98);
                                          var7[1]++;
                                       }

                                       var20[var96 >>> 6] = var20[var96 >>> 6] | 1L << (var96 & 63);
                                       var68.add(var96);
                                       continue;
                                    }

                                    if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                                       Constants.LOG.info("[SR-FINEFLOOD] bound at {},{} level={} fill={}", var97, var98, var1, var0.size());
                                    }

                                    return null;
                                 }

                                 if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                                    Constants.LOG.info("[SR-FINEFLOOD] border at {},{} level={} fill={}", var97, var98, var1, var0.size());
                                 }

                                 return null;
                              }
                           }
                        }
                     }
                  }
               }

               ArrayList var79 = var66;
               var66 = var68;
               var68 = var79;
               var79.clear();
            }

            return new Lake(var1, 0, 3, var50, var52, var53, var54, var20);
         }
      }
   }

   private static boolean a(Lake var0, double var1) {
      int var3 = var0.cellsWide();
      int var4 = var0.cellsHigh();
      long[] var5 = var0.mask();
      int var6 = 0;

      for (long var10 : var5) {
         var6 += Long.bitCount(var10);
      }

      if (var6 == 0) {
         return false;
      } else {
         boolean[] var21 = new boolean[var3 * var4];
         int var22 = 0;
         ArrayDeque var23 = new ArrayDeque();

         for (int var24 = 0; var24 < var3 * var4; var24++) {
            if (!var21[var24] && (var5[var24 >>> 6] & 1L << (var24 & 63)) != 0L) {
               int var11 = 0;
               var21[var24] = true;
               var23.add(var24);

               while (!var23.isEmpty()) {
                  int var12 = (Integer)var23.poll();
                  var11++;
                  int var13 = var12 % var3;
                  int var14 = var12 / var3;
                  int[][] var15 = new int[][]{{var13 - 1, var14}, {var13 + 1, var14}, {var13, var14 - 1}, {var13, var14 + 1}};

                  for (int[] var19 : var15) {
                     if (var19[0] >= 0 && var19[0] < var3 && var19[1] >= 0 && var19[1] < var4) {
                        int var20 = var19[1] * var3 + var19[0];
                        if (!var21[var20] && (var5[var20 >>> 6] & 1L << (var20 & 63)) != 0L) {
                           var21[var20] = true;
                           var23.add(var20);
                        }
                     }
                  }
               }

               var22 = Math.max(var22, var11);
               if ((double)var22 >= (double)var6 * var1) {
                  return true;
               }
            }
         }

         return (double)var22 >= (double)var6 * var1;
      }
   }

   private static Lake a(List<Integer> var0, int var1, int var2, int var3, LowResGrid var4) {
      int var5 = Integer.MAX_VALUE;
      int var6 = Integer.MAX_VALUE;
      int var7 = Integer.MIN_VALUE;
      int var8 = Integer.MIN_VALUE;

      for (int var10 : var0) {
         var5 = Math.min(var5, var4.gi(var10));
         var7 = Math.max(var7, var4.gi(var10));
         var6 = Math.min(var6, var4.gj(var10));
         var8 = Math.max(var8, var4.gj(var10));
      }

      int var26 = var3;
      var5 -= var3;
      var6 -= var3;
      var7 += var3;
      var8 += var3;
      int var27 = var7 - var5 + 1;
      int var11 = var8 - var6 + 1;
      long[] var12 = new long[var27 * var11 + 63 >>> 6];

      for (int var14 : var0) {
         int var15 = var4.gi(var14) - var5;
         int var16 = var4.gj(var14) - var6;

         for (int var17 = -var26; var17 <= var26; var17++) {
            for (int var18 = -var26; var18 <= var26; var18++) {
               int var19 = var15 + var18;
               int var20 = var16 + var17;
               if (var19 >= 0 && var19 < var27 && var20 >= 0 && var20 < var11) {
                  int var21 = var20 * var27 + var19;
                  var12[var21 >>> 6] = var12[var21 >>> 6] | 1L << (var21 & 63);
               }
            }
         }
      }

      return new Lake(var1, var2, var4.spacing, var4.worldX(var5), var4.worldZ(var6), var27, var11, var12);
   }

   private static double a(LowResGrid var0, int var1, int var2) {
      double var3 = (double)(var0.gi(var1) - var0.gi(var2));
      double var5 = (double)(var0.gj(var1) - var0.gj(var2));
      return Math.hypot(var3, var5) * (double)var0.spacing;
   }

   private static void a(
      NetworkTracer.PendingOutflow var0,
      LowResGrid var1,
      int[] var2,
      int[] var3,
      List<Lake> var4,
      double[] var5,
      int var6,
      TerrainSampler var7,
      StreamsConfig var8,
      long var9,
      List<RiverSegment> var11
   ) {
      ArrayList var12 = new ArrayList();
      double var13 = var0.startLevel() + (double)RiverEngine.insetFor(b(var0.acc(), var8), var8);
      var12.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var0.outlet())), (double)var1.worldZ(var1.gj(var0.outlet())), var13, var0.acc()));
      int var15 = var0.outlet();
      boolean var16 = false;
      double var17 = Double.NaN;
      int var19 = -1;
      Lake var20 = null;
      int var21 = var1.size();

      while (var21-- > 0) {
         int var22 = var2[var15];
         if (var22 < 0) {
            var16 = true;
            break;
         }

         if (var3[var22] >= 0) {
            Lake var30 = (Lake)var4.get(var3[var22]);
            if ((double)var30.level() >= var0.startLevel() - 1.0) {
               return;
            }

            var12.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var22)), (double)var1.worldZ(var1.gj(var22)), (double)var30.level(), var0.acc()));
            var17 = (double)var30.level();
            var20 = var30;
            break;
         }

         double var23 = var5[var22];
         double var25 = a(var4, (double)var1.worldX(var1.gi(var22)), (double)var1.worldZ(var1.gj(var22)), var0.acc(), var8);
         if (!Double.isNaN(var25)) {
            var23 = Math.max(var23, var25);
         }

         double var27 = Math.min(((NetworkTracer.Pt)var12.get(var12.size() - 1)).y(), Math.max((double)var6, var23));
         var12.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var22)), (double)var1.worldZ(var1.gj(var22)), var27, var0.acc()));
         if (var5[var22] <= (double)var6) {
            var17 = (double)var6;
            var19 = var22;
            break;
         }

         var15 = var22;
      }

      int var29 = var0.outlet() * 131 + 9;
      ColumnCache var31 = new ColumnCache();
      Object var24 = var8.valleySnapRadius() > 0 ? a(var12, var7, var31, var8, false, var4) : var12;
      if (var20 != null && var24.size() >= 2) {
         int var32 = var24.size();
         var24 = a((List<NetworkTracer.Pt>)var24, var20, var7, var31, var8);
         if (var24.size() < var32 || var24.size() < 2) {
            return;
         }
      }

      if (var24.size() >= 2) {
         if (var19 >= 0) {
            List var33 = a((List<NetworkTracer.Pt>)var24, var1, var19, var6, var7, var8);
            if (var33 == var24) {
               return;
            }

            var24 = var33;
         } else if (!Double.isNaN(var17)) {
            var24 = b((List<NetworkTracer.Pt>)var24, var17, 8.0);
         } else if (var16) {
            List var34 = a((List<NetworkTracer.Pt>)var24, var1, var15, var6, var7, var8);
            if (var34 == var24) {
               return;
            }

            var24 = var34;
         }

         List var35 = a(a((List<NetworkTracer.Pt>)var24, (double)var8.gridSpacing() * 0.4), var9, var29, var8);
         if (var8.confineToBanks()) {
            var35 = a(var35, var7, var31, var8);
         }

         var35 = a(var35, var8.smoothingPasses());
         a(var35, var8, var11, false);
      }
   }

   private static int a(LowResGrid var0, int var1, double[] var2, boolean[] var3, int var4, int var5) {
      int var6 = var0.gi(var1);
      int var7 = var0.gj(var1);
      int var8 = -1;
      int var9 = Integer.MAX_VALUE;

      for (int var10 = -var5; var10 <= var5; var10++) {
         for (int var11 = -var5; var11 <= var5; var11++) {
            if (var11 != 0 || var10 != 0) {
               int var12 = var6 + var11;
               int var13 = var7 + var10;
               if (var0.valid(var12, var13)) {
                  int var14 = var0.idx(var12, var13);
                  if (var0.inDomain(var14)) {
                     boolean var15 = var3[var14] && var2[var14] < var2[var1];
                     boolean var16 = var2[var14] <= (double)var4;
                     if (var15 || var16) {
                        int var17 = var11 * var11 + var10 * var10;
                        if (var17 < var9) {
                           var9 = var17;
                           var8 = var14;
                        }
                     }
                  }
               }
            }
         }
      }

      return var8;
   }

   private static boolean a(int var0, LowResGrid var1, WetnessSampler var2, ColumnCache var3, double var4, double var6, double var8, long var10) {
      int var12 = var1.worldX(var1.gi(var0));
      int var13 = var1.worldZ(var1.gj(var0));
      long var14 = (long)Math.floorDiv(var12, 64) << 32 ^ (long)Math.floorDiv(var13, 64) & 4294967295L;
      double var16 = var3.get(var14, Double.NaN);
      double var18;
      if (!Double.isNaN(var16)) {
         var18 = var16;
      } else {
         var18 = var2.wetness(var12, (int)var1.height(var0), var13);
         var3.put(var14, var18);
      }

      double var20 = Math.max(var8, Math.min(1.0, (var18 - var4) / (var6 - var4)));
      return Hashing.unit(var10, (long)var0, 71L) >= var20;
   }

   private static boolean a(LowResGrid var0, int var1) {
      int var2 = var0.gi(var1);
      int var3 = var0.gj(var1);

      for (int var4 = 0; var4 < 8; var4++) {
         int var5 = var2 + a[var4];
         int var6 = var3 + b[var4];
         if (!var0.valid(var5, var6) || !var0.inDomain(var0.idx(var5, var6))) {
            return true;
         }
      }

      return false;
   }

   private static List<NetworkTracer.Pt> a(
      List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3, boolean var4, List<Lake> var5
   ) {
      int var6 = var0.size();
      if (var6 < 2) {
         return var0;
      } else {
         int var7 = var3.valleySnapRadius();
         double var8 = Math.max(1.0, (double)var7 / 8.0);
         double var10 = (double)(var3.seaLevel() - 1);
         ArrayList var12 = new ArrayList(var6);
         NetworkTracer.Pt var13 = (NetworkTracer.Pt)var0.get(0);
         double var14 = a(var5, var13.x(), var13.z(), var13.acc(), var3);
         double var16 = var4 ? a(var1, var2, var5, var3, var13.acc(), var13.x(), var13.z()) : (Double.isNaN(var14) ? var13.y() : Math.max(var13.y(), var14));
         var12.add(new NetworkTracer.Pt(var13.x(), var13.z(), var16, var13.acc()));
         double var18 = var16;

         for (int var20 = 1; var20 < var6 - 1; var20++) {
            NetworkTracer.Pt var21 = (NetworkTracer.Pt)var0.get(var20 - 1);
            NetworkTracer.Pt var22 = (NetworkTracer.Pt)var0.get(var20);
            NetworkTracer.Pt var23 = (NetworkTracer.Pt)var0.get(var20 + 1);
            double var24 = var23.x() - var21.x();
            double var26 = var23.z() - var21.z();
            double var28 = Math.hypot(var24, var26);
            double var30 = var22.x();
            double var32 = var22.z();
            double var34 = a(var1, var2, var5, var3, var22.acc(), var22.x(), var22.z());
            if (var28 > 1.0E-6 && Double.isNaN(a(var5, var22.x(), var22.z()))) {
               double var36 = -var26 / var28;
               double var38 = var24 / var28;
               double var40 = 0.0;
               if (var1.hasEstimate()) {
                  double var54 = b(var1, var2, var22.x(), var22.z());

                  for (double var55 = (double)(-var7); var55 <= (double)var7 + 1.0E-9; var55 += var8) {
                     double var46 = var22.x() + var36 * var55;
                     double var48 = var22.z() + var38 * var55;
                     if (!(Math.abs(var55) < 1.0E-9) && Double.isNaN(a(var5, var46, var48))) {
                        double var50 = b(var1, var2, var46, var48);
                        if (var50 < var54) {
                           var54 = var50;
                           var40 = var55;
                        }
                     }
                  }

                  if (Math.abs(var40) > 1.0E-9 && Math.abs(var40) < (double)var7 - 1.0E-9) {
                     var30 = var22.x() + var36 * var40;
                     var32 = var22.z() + var38 * var40;
                     var34 = a(var1, var2, var5, var3, var22.acc(), var30, var32);
                  }
               } else {
                  for (double var42 = (double)(-var7); var42 <= (double)var7 + 1.0E-9; var42 += var8) {
                     if (!(Math.abs(var42) < 1.0E-9)) {
                        double var44 = a(var1, var2, var5, var3, var22.acc(), var22.x() + var36 * var42, var22.z() + var38 * var42);
                        if (var44 < var34) {
                           var34 = var44;
                           var40 = var42;
                        }
                     }
                  }

                  if (Math.abs(var40) < (double)var7 - 1.0E-9) {
                     var30 = var22.x() + var36 * var40;
                     var32 = var22.z() + var38 * var40;
                  } else {
                     var34 = a(var1, var2, var5, var3, var22.acc(), var22.x(), var22.z());
                  }
               }
            }

            double var53 = Math.max(var10, Math.min(var18, var34));
            var18 = var53;
            var12.add(new NetworkTracer.Pt(var30, var32, var53, var22.acc()));
         }

         NetworkTracer.Pt var52 = (NetworkTracer.Pt)var0.get(var6 - 1);
         var12.add(new NetworkTracer.Pt(var52.x(), var52.z(), Math.min(var18, var52.y()), var52.acc()));
         return var12;
      }
   }

   private static double a(TerrainSampler var0, ColumnCache var1, List<Lake> var2, StreamsConfig var3, double var4, double var6, double var8) {
      double var10 = a(var0, var1, var6, var8);
      double var12 = a(var2, var6, var8, var4, var3);
      return Double.isNaN(var12) ? var10 : Math.max(var10, var12);
   }

   private static double a(List<Lake> var0, double var1, double var3, double var5, StreamsConfig var7) {
      double var8 = a(var0, var1, var3);
      if (Double.isNaN(var8)) {
         var8 = a(var0, var1, var3, 4.0);
         if (Double.isNaN(var8)) {
            return Double.NaN;
         }
      }

      return var8 + (double)RiverEngine.insetFor(b(var5, var7), var7);
   }

   private static double a(List<Lake> var0, double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1);
      int var8 = (int)Math.floor(var3);
      int var9 = (int)Math.ceil(var5);
      double var10 = Double.NaN;

      for (Lake var13 : var0) {
         for (int var14 = -var9; var14 <= var9; var14++) {
            for (int var15 = -var9; var15 <= var9; var15++) {
               if (!((double)(var15 * var15 + var14 * var14) > var5 * var5) && var13.covers(var7 + var15, var8 + var14)) {
                  if (Double.isNaN(var10) || (double)var13.level() < var10) {
                     var10 = (double)var13.level();
                  }

                  var14 = var9;
                  break;
               }
            }
         }
      }

      return var10;
   }

   private static double a(List<Lake> var0, double var1, double var3) {
      int var5 = (int)Math.floor(var1);
      int var6 = (int)Math.floor(var3);

      for (Lake var8 : var0) {
         if (var8.covers(var5, var6)) {
            return (double)var8.level();
         }
      }

      return Double.NaN;
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3, List<Lake> var4) {
      int var5 = var0.size();
      if (var5 < 3) {
         return var0;
      } else {
         double var6 = (double)(var3.seaLevel() - 1);
         double[] var8 = new double[var5];
         boolean[] var9 = new boolean[var5];
         double[] var10 = new double[var5];
         double[] var11 = new double[var5];

         for (int var12 = 0; var12 < var5; var12++) {
            var8[var12] = a(var1, var2, ((NetworkTracer.Pt)var0.get(var12)).x(), ((NetworkTracer.Pt)var0.get(var12)).z());
            var11[var12] = a(var4, ((NetworkTracer.Pt)var0.get(var12)).x(), ((NetworkTracer.Pt)var0.get(var12)).z());
            var10[var12] = var12 == 0
               ? 0.0
               : var10[var12 - 1]
                  + Math.hypot(
                     ((NetworkTracer.Pt)var0.get(var12)).x() - ((NetworkTracer.Pt)var0.get(var12 - 1)).x(),
                     ((NetworkTracer.Pt)var0.get(var12)).z() - ((NetworkTracer.Pt)var0.get(var12 - 1)).z()
                  );
         }

         int var51 = -1;
         int var13 = -1;

         for (int var14 = 0; var14 < var5; var14++) {
            if (!Double.isNaN(var11[var14])) {
               if (var51 < 0) {
                  var51 = var14;
               }

               if (var13 >= 0 && var11[var13] == var11[var14] && var10[var14] - var10[var13] <= 24.0) {
                  for (int var15 = var13 + 1; var15 < var14; var15++) {
                     var11[var15] = var11[var14];
                  }
               }

               var13 = var14;
            }
         }

         if (var13 >= 0) {
            double var52 = var11[var13];

            for (int var16 = var13 + 1; var16 < var5 && var10[var16] - var10[var13] <= 8.0 && var8[var16] >= var52 - 1.0; var16++) {
               var11[var16] = var52;
            }
         }

         double var53 = var51 >= 0 ? var11[var51] : Double.NaN;
         double[] var54 = new double[var5];

         for (int var17 = 0; var17 < var5; var17++) {
            var9[var17] = !Double.isNaN(var11[var17]);
            var54[var17] = var9[var17] ? var11[var17] + (double)RiverEngine.insetFor(b(((NetworkTracer.Pt)var0.get(var17)).acc(), var3), var3) : Double.NaN;
            if (var9[var17]) {
               var8[var17] = Math.max(var8[var17], var54[var17]);
            }
         }

         ArrayList var55 = new ArrayList(var5);
         NetworkTracer.Pt var18 = (NetworkTracer.Pt)var0.get(0);
         double var19 = var9[0]
            ? Math.max(var18.y(), var54[0])
            : (var51 > 0 ? Math.max(var18.y(), var53 + (double)RiverEngine.insetFor(b(var18.acc(), var3), var3)) : var18.y());
         var55.add(new NetworkTracer.Pt(var18.x(), var18.z(), var19, var18.acc()));
         double var21 = var19;

         for (int var23 = 1; var23 < var5 - 1; var23++) {
            NetworkTracer.Pt var24 = (NetworkTracer.Pt)var0.get(var23 - 1);
            NetworkTracer.Pt var25 = (NetworkTracer.Pt)var0.get(var23);
            NetworkTracer.Pt var26 = (NetworkTracer.Pt)var0.get(var23 + 1);
            double var27 = var8[var23];
            double var31 = var9[var23] ? Math.max(var6, Math.min(var21, var27)) : Math.max(var6, Math.min(var21, Math.min(var25.y(), var27)));
            double var33 = var26.x() - var24.x();
            double var35 = var26.z() - var24.z();
            double var37 = Math.hypot(var33, var35);
            if (var37 > 1.0E-6 && !var9[var23]) {
               double var39 = -var35 / var37;
               double var41 = var33 / var37;
               double var43 = b(var25.acc(), var3) + 2.0;
               double var45 = a(var1, var2, var4, var3, var25.acc(), var25.x() + var39 * var43, var25.z() + var41 * var43);
               double var47 = a(var1, var2, var4, var3, var25.acc(), var25.x() - var39 * var43, var25.z() - var41 * var43);
               double var49 = Math.min(var45, var47);
               if (var49 > var6 && var49 < var31) {
                  var31 = Math.min(var31, Math.max(var49, var27 - 6.0));
               }
            }

            if (var51 >= 0 && var23 < var51) {
               double var58 = var53 + (double)RiverEngine.insetFor(b(var25.acc(), var3), var3);
               var31 = Math.min(Math.max(var31, var58), var21);
            }

            var21 = var31;
            var55.add(new NetworkTracer.Pt(var25.x(), var25.z(), var31, var25.acc()));
         }

         NetworkTracer.Pt var56 = (NetworkTracer.Pt)var0.get(var5 - 1);
         double var57 = var9[var5 - 1] ? Math.min(var21, var8[var5 - 1]) : Math.min(var21, var56.y());
         var55.add(new NetworkTracer.Pt(var56.x(), var56.z(), var57, var56.acc()));
         return var55;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2) {
      if (var0.size() < 2) {
         return var0;
      } else {
         NetworkTracer.Pt var3 = (NetworkTracer.Pt)var0.get(0);
         double var4 = Math.min(var3.y(), a(var1, var2, var3.x(), var3.z()));
         if (var4 >= var3.y()) {
            return var0;
         } else {
            ArrayList var6 = new ArrayList(var0);
            var6.set(0, new NetworkTracer.Pt(var3.x(), var3.z(), var4, var3.acc()));
            return var6;
         }
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, TerrainSampler var1, ColumnCache var2, StreamsConfig var3) {
      int var4 = var0.size();
      if (var4 < 3) {
         return var0;
      } else {
         double var5 = (double)var3.gridSpacing();
         double var7 = Math.max(1.0, var5 / 8.0);
         ArrayList var9 = new ArrayList(var4);
         var9.add((NetworkTracer.Pt)var0.get(0));

         for (int var10 = 1; var10 < var4 - 1; var10++) {
            NetworkTracer.Pt var11 = (NetworkTracer.Pt)var0.get(var10 - 1);
            NetworkTracer.Pt var12 = (NetworkTracer.Pt)var0.get(var10);
            NetworkTracer.Pt var13 = (NetworkTracer.Pt)var0.get(var10 + 1);
            double var14 = var13.x() - var11.x();
            double var16 = var13.z() - var11.z();
            double var18 = Math.hypot(var14, var16);
            if (var18 < 1.0E-6) {
               var9.add(var12);
            } else {
               double var20 = -var16 / var18;
               double var22 = var14 / var18;
               double var24 = b(var12.acc(), var3) + 3.0;
               double var26 = a(var1, var2, var12.x() - var20 * var24, var12.z() - var22 * var24);
               double var28 = a(var1, var2, var12.x() + var20 * var24, var12.z() + var22 * var24);
               if (Math.min(var26, var28) >= var12.y()) {
                  var9.add(var12);
               } else {
                  boolean var30 = var1.hasEstimate();
                  double var31 = 0.0;
                  double var33 = var30 ? b(var1, var2, var12.x(), var12.z()) : a(var1, var2, var12.x(), var12.z());

                  for (double var35 = -var5; var35 <= var5 + 1.0E-9; var35 += var7) {
                     double var37 = var12.x() + var20 * var35;
                     double var39 = var12.z() + var22 * var35;
                     double var41 = var30 ? b(var1, var2, var37, var39) : a(var1, var2, var37, var39);
                     if (var41 < var33) {
                        var33 = var41;
                        var31 = var35;
                     }
                  }

                  if (Math.abs(var31) > 1.0E-9 && Math.abs(var31) < var5 - 1.0E-9) {
                     var9.add(new NetworkTracer.Pt(var12.x() + var20 * var31, var12.z() + var22 * var31, var12.y(), var12.acc()));
                  } else {
                     var9.add(var12);
                  }
               }
            }
         }

         var9.add((NetworkTracer.Pt)var0.get(var4 - 1));
         return var9;
      }
   }

   private static double a(TerrainSampler var0, double var1, double var3) {
      return var0.groundHeight((int)Math.round(var1), (int)Math.round(var3));
   }

   private static double a(TerrainSampler var0, ColumnCache var1, double var2, double var4) {
      int var6 = (int)Math.round(var2);
      int var7 = (int)Math.round(var4);
      long var8 = ColumnKey.of(var6, var7);
      double var10 = var1.get(var8, Double.NaN);
      if (!Double.isNaN(var10)) {
         return var10;
      } else {
         double var12 = var0.groundHeight(var6, var7);
         var1.put(var8, var12);
         return var12;
      }
   }

   private static double a(TerrainSampler var0, Map<Long, Double> var1, double var2, double var4) {
      int var6 = (int)Math.round(var2);
      int var7 = (int)Math.round(var4);
      long var8 = ColumnKey.of(var6, var7);
      Double var10 = (Double)var1.get(var8);
      if (var10 != null) {
         return var10;
      } else {
         double var11 = var0.groundHeight(var6, var7);
         var1.put(var8, var11);
         return var11;
      }
   }

   private static double b(TerrainSampler var0, ColumnCache var1, double var2, double var4) {
      int var6 = (int)Math.round(var2);
      int var7 = (int)Math.round(var4);
      long var8 = ColumnKey.mix(ColumnKey.pack(var6, var7) ^ 4611686018427387904L);
      double var10 = var1.get(var8, Double.NaN);
      if (!Double.isNaN(var10)) {
         return var10;
      } else {
         double var12 = var0.estimate(var6, var7);
         var1.put(var8, var12);
         return var12;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, double var1) {
      if (var0.size() >= 2 && !(var1 < 1.0)) {
         ArrayList var3 = new ArrayList();

         for (int var4 = 0; var4 < var0.size() - 1; var4++) {
            NetworkTracer.Pt var5 = (NetworkTracer.Pt)var0.get(var4);
            NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var4 + 1);
            var3.add(var5);
            double var7 = Math.hypot(var6.x() - var5.x(), var6.z() - var5.z());
            int var9 = (int)Math.floor(var7 / var1);

            for (int var10 = 1; var10 <= var9; var10++) {
               double var11 = (double)var10 * var1 / var7;
               if (var11 < 1.0) {
                  var3.add(a(var5, var6, var11));
               }
            }
         }

         var3.add((NetworkTracer.Pt)var0.get(var0.size() - 1));
         return var3;
      } else {
         return var0;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, long var1, int var3, StreamsConfig var4) {
      int var5 = var0.size();
      if (var5 < 3) {
         return var0;
      } else {
         double var6 = (double)var4.gridSpacing() * var4.meanderStrength();
         double var8 = Hashing.unit(var1, (long)var3, 11L) * Math.PI * 2.0;
         double var10 = Hashing.unit(var1, (long)var3, 12L) * Math.PI * 2.0;
         double var12 = Hashing.unit(var1, (long)var3, 13L) * Math.PI * 2.0;
         double[] var14 = new double[var5];

         for (int var15 = 1; var15 < var5; var15++) {
            var14[var15] = var14[var15 - 1]
               + Math.hypot(
                  ((NetworkTracer.Pt)var0.get(var15)).x() - ((NetworkTracer.Pt)var0.get(var15 - 1)).x(),
                  ((NetworkTracer.Pt)var0.get(var15)).z() - ((NetworkTracer.Pt)var0.get(var15 - 1)).z()
               );
         }

         ArrayList var38 = new ArrayList(var5);
         var38.add((NetworkTracer.Pt)var0.get(0));

         for (int var16 = 1; var16 < var5 - 1; var16++) {
            NetworkTracer.Pt var17 = (NetworkTracer.Pt)var0.get(var16 - 1);
            NetworkTracer.Pt var18 = (NetworkTracer.Pt)var0.get(var16);
            NetworkTracer.Pt var19 = (NetworkTracer.Pt)var0.get(var16 + 1);
            double var20 = var19.x() - var17.x();
            double var22 = var19.z() - var17.z();
            double var24 = Math.hypot(var20, var22);
            if (var24 < 1.0E-6) {
               var38.add(var18);
            } else {
               double var26 = -var22 / var24;
               double var28 = var20 / var24;
               double var30 = Math.sin(Math.PI * (double)var16 / (double)(var5 - 1));
               double var32 = Math.abs(var17.y() - var19.y()) / var24;
               double var34 = Math.max(0.0, 1.0 - var32 / 1.0);
               double var36 = var6
                  * var30
                  * var34
                  * (0.5 * Math.sin(var14[var16] * 0.03 + var8) + 0.3 * Math.sin(var14[var16] * 0.07 + var10) + 0.2 * Math.sin(var14[var16] * 0.017 + var12));
               var38.add(new NetworkTracer.Pt(var18.x() + var26 * var36, var18.z() + var28 * var36, var18.y(), var18.acc()));
            }
         }

         var38.add((NetworkTracer.Pt)var0.get(var5 - 1));
         return var38;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, int var1) {
      for (int var2 = 0; var2 < var1 && var0.size() >= 3; var2++) {
         ArrayList var3 = new ArrayList(var0.size() * 2);
         var3.add((NetworkTracer.Pt)var0.get(0));

         for (int var4 = 0; var4 < var0.size() - 1; var4++) {
            var3.add(a((NetworkTracer.Pt)var0.get(var4), (NetworkTracer.Pt)var0.get(var4 + 1), 0.25));
            var3.add(a((NetworkTracer.Pt)var0.get(var4), (NetworkTracer.Pt)var0.get(var4 + 1), 0.75));
         }

         var3.add((NetworkTracer.Pt)var0.get(var0.size() - 1));
         var0 = var3;
      }

      return (List<NetworkTracer.Pt>)var0;
   }

   private static NetworkTracer.Pt a(NetworkTracer.Pt var0, NetworkTracer.Pt var1, double var2) {
      return new NetworkTracer.Pt(
         var0.x() + (var1.x() - var0.x()) * var2,
         var0.z() + (var1.z() - var0.z()) * var2,
         var0.y() + (var1.y() - var0.y()) * var2,
         var0.acc() + (var1.acc() - var0.acc()) * var2
      );
   }

   private static void a(List<NetworkTracer.Pt> var0, StreamsConfig var1, List<RiverSegment> var2, boolean var3) {
      double var4 = var3 ? (double)var1.gridSpacing() * 1.5 : 0.0;
      double var6 = 0.0;

      for (int var8 = 0; var8 < var0.size() - 1; var8++) {
         NetworkTracer.Pt var9 = (NetworkTracer.Pt)var0.get(var8);
         NetworkTracer.Pt var10 = (NetworkTracer.Pt)var0.get(var8 + 1);
         double var11 = Math.hypot(var10.x() - var9.x(), var10.z() - var9.z());
         double var13 = b(var9.acc(), var1) * c(var6, var4);
         double var15 = b(var10.acc(), var1) * c(var6 + var11, var4);
         var2.add(new RiverSegment(var9.x(), var9.z(), var9.y(), var13, var10.x(), var10.z(), var10.y(), var15));
         var6 += var11;
      }
   }

   private static double c(double var0, double var2) {
      return var2 <= 0.0 ? 1.0 : Math.min(1.0, var0 / var2);
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, LowResGrid var1, int var2, int var3, TerrainSampler var4, StreamsConfig var5) {
      int var6 = var0.size();
      if (var6 >= 2 && var2 >= 0) {
         NetworkTracer.Pt var7 = (NetworkTracer.Pt)var0.get(var6 - 1);
         double var8 = Math.min(var7.y(), (double)var3);
         int var10 = b(var7.acc(), var5) < 1.0 ? 1 : 2;
         if (y) {
            List var21 = a(var1, var2, (double)(var3 + 1), (double)(var3 - 1), 1);
            if (var21 != null && !a(var21, var1, var4, var3)) {
               if (Boolean.getBoolean("streamsreflowing.mouthconfirm")) {
                  int var23 = var21.isEmpty() ? var2 : (Integer)var21.get(var21.size() - 1);
                  int var27 = Math.round((float)var1.worldX(var1.gi(var23)));
                  int var33 = Math.round((float)var1.worldZ(var1.gj(var23)));
                  if (var4.surfaceHeight(var27, var33) - var4.groundHeight(var27, var33) < 0.99) {
                     return var0;
                  }
               }

               ArrayList var24 = new ArrayList(var0);

               for (int var34 : var21) {
                  var24.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var34)), (double)var1.worldZ(var1.gj(var34)), var8, var7.acc()));
               }

               int var29 = var21.isEmpty() ? var2 : (Integer)var21.get(var21.size() - 1);
               if (!b(var1, var4, var29, var3)) {
                  return var0;
               } else {
                  return var1.height(var29) <= (double)(var3 - 4) ? b(var24, var8, 8.0) : b(var24, var8, Math.max(32.0, 3.0 * b(var7.acc(), var5)));
               }
            } else {
               return var0;
            }
         } else {
            Object var11 = a(var1, var2, (double)(var3 + 1), (double)(var3 - 2), var10);
            if (var11 != null && a((List<Integer>)var11, var1, var4, var3)) {
               var11 = null;
            }

            if (var11 == null) {
               List var12 = a(var1, var2, (double)(var3 + 1), (double)(var3 - 1), var10);
               if (var12 == null || a(var12, var1, var4, var3)) {
                  return var0;
               }

               int var13 = var12.isEmpty() ? var2 : (Integer)var12.get(var12.size() - 1);
               List var14 = a(var1, var13, (double)(var3 + 1), (double)(var3 - 2), var10);
               if (var14 == null || a(var14, var1, var4, var3)) {
                  return var0;
               }

               var11 = new ArrayList(var12);
               var11.addAll(var14);
            }

            ArrayList var22 = new ArrayList(var0);

            for (int var30 : var11) {
               var22.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var30)), (double)var1.worldZ(var1.gj(var30)), var8, var7.acc()));
            }

            int var26 = var11.isEmpty() ? var2 : (Integer)var11.get(var11.size() - 1);
            if (var4.hasEstimate()) {
               int var32 = var26;
               double var36 = a(var1, var4, var26, var3);

               for (int var17 = 0; var17 < 24 && var36 > (double)(var3 - 24); var17++) {
                  List var18 = a(var1, var4, var32, (double)(var3 + 1), var36 - 1.0, var10, var3);
                  if (var18 == null || var18.isEmpty() || a(var18, var1, var4, var3)) {
                     break;
                  }

                  for (int var20 : var18) {
                     var22.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var20)), (double)var1.worldZ(var1.gj(var20)), var8, var7.acc()));
                  }

                  var32 = (Integer)var18.get(var18.size() - 1);
                  var36 = a(var1, var4, var32, var3);
               }

               if (!b(var1, var4, var32, var3)) {
                  return var0;
               } else {
                  return var36 <= (double)(var3 - 4) ? b(var22, var8, 8.0) : b(var22, var8, Math.max(32.0, 3.0 * b(var7.acc(), var5)));
               }
            } else {
               List var31 = a(var1, var26, (double)(var3 + 1), (double)(var3 - 4 + 1), var10);
               if (var31 != null && !a(var31, var1, var4, var3)) {
                  for (int var16 : var31) {
                     var22.add(new NetworkTracer.Pt((double)var1.worldX(var1.gi(var16)), (double)var1.worldZ(var1.gj(var16)), var8, var7.acc()));
                  }

                  int var35 = var31.isEmpty() ? var26 : (Integer)var31.get(var31.size() - 1);
                  if (!b(var1, var4, var35, var3)) {
                     return var0;
                  } else {
                     return !a(
                           var4, Math.round((float)var1.worldX(var1.gi(var35))), (int)Math.round(var8), Math.round((float)var1.worldZ(var1.gj(var35))), var5
                        )
                        ? var0
                        : b(var22, var8, 8.0);
                  }
               } else if (!b(var1, var4, var26, var3)) {
                  return var0;
               } else {
                  return !a(var4, Math.round((float)var1.worldX(var1.gi(var26))), (int)Math.round(var8), Math.round((float)var1.worldZ(var1.gj(var26))), var5)
                     ? var0
                     : b(var22, var8, Math.max(32.0, 3.0 * b(var7.acc(), var5)));
               }
            }
         }
      } else {
         return var0;
      }
   }

   private static List<NetworkTracer.Pt> a(List<NetworkTracer.Pt> var0, LowResGrid var1, int var2, TerrainSampler var3, StreamsConfig var4) {
      int var5 = var0.size();
      if (var5 >= 2 && var2 >= 0) {
         NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var5 - 1);
         int var7 = Math.round((float)var1.worldX(var1.gi(var2)));
         int var8 = Math.round((float)var1.worldZ(var1.gj(var2)));
         double var9 = var3.riverSurface(var7, var8);
         boolean var11 = !Double.isNaN(var9);
         if (!var11) {
            var9 = var3.surfaceHeight(var7, var8);
            if (var9 - var3.groundHeightExact(var7, var8) < 0.99) {
               if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                  Constants.LOG
                     .info("[SR-RTF] river mouth at {},{}: DRY (surf={} ground={}) -> chain dropped", var7, var8, var9, var3.groundHeightExact(var7, var8));
               }

               return var0;
            }
         }

         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-RTF] river mouth at {},{}: surf={} ({}) -> accepted", var7, var8, var9, var11 ? "generator" : "placed water");
         }

         double var12 = Math.min(var6.y(), var9 + (double)RiverEngine.insetFor(b(var6.acc(), var4), var4));
         ArrayList var14 = new ArrayList(var0);
         var14.add(new NetworkTracer.Pt((double)var7, (double)var8, var12, var6.acc()));
         return b(var14, var12, 8.0);
      } else {
         return var0;
      }
   }

   private static double a(LowResGrid var0, TerrainSampler var1, int var2, int var3) {
      double var4 = var0.height(var2);
      if (var4 == -1001.0) {
         return var1.surfaceHeight(Math.round((float)var0.worldX(var0.gi(var2))), Math.round((float)var0.worldZ(var0.gj(var2))));
      } else if (var4 != -1000.0) {
         return var4;
      } else {
         double var6 = var1.estimate(Math.round((float)var0.worldX(var0.gi(var2))), Math.round((float)var0.worldZ(var0.gj(var2))));
         return Math.max(var6, (double)(var3 - 24));
      }
   }

   private static boolean a(TerrainSampler var0, int var1, int var2, int var3, StreamsConfig var4) {
      String var5 = var4.outletBiomes();
      if (var5 != null && !var5.isBlank()) {
         int var6 = Math.max(1, var4.gridSpacing());

         for (int var7 = -1; var7 <= 1; var7++) {
            for (int var8 = -1; var8 <= 1; var8++) {
               if (var0.outletBiomeOk(var1 + var8 * var6, var2, var3 + var7 * var6)) {
                  return true;
               }
            }
         }

         if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info("[SR-MOUTH] no whitelisted outlet biome within one cell of {},{} -> chain dropped", var1, var3);
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean b(LowResGrid var0, TerrainSampler var1, int var2, int var3) {
      int var4 = Math.round((float)var0.worldX(var0.gi(var2)));
      int var5 = Math.round((float)var0.worldZ(var0.gj(var2)));
      boolean var6 = var1.surfaceHeight(var4, var5) - var1.groundHeightExact(var4, var5) >= 0.99;
      if (!var6 && Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info("[SR-MOUTH] final connection check failed at {},{} -> chain dropped", var4, var5);
      }

      return var6;
   }

   private static List<Integer> a(LowResGrid var0, TerrainSampler var1, int var2, double var3, double var5, int var7, int var8) {
      HashMap var9 = new HashMap();
      HashMap var10 = new HashMap();
      ArrayDeque var11 = new ArrayDeque();
      var9.put(var2, var2);
      var10.put(var2, 0);
      var11.add(var2);

      while (!var11.isEmpty()) {
         int var12 = (Integer)var11.poll();
         if (var12 != var2 && a(var0, var1, var12, var8) <= var5) {
            LinkedList var20 = new LinkedList();

            for (int var21 = var12; var21 != var2; var21 = (Integer)var9.get(var21)) {
               var20.addFirst(var21);
            }

            return var20;
         }

         int var13 = (Integer)var10.get(var12);
         if (var13 < var7) {
            int var14 = var0.gi(var12);
            int var15 = var0.gj(var12);

            for (int var16 = 0; var16 < 8; var16++) {
               int var17 = var14 + a[var16];
               int var18 = var15 + b[var16];
               if (var0.valid(var17, var18)) {
                  int var19 = var0.idx(var17, var18);
                  if (!var9.containsKey(var19) && !(a(var0, var1, var19, var8) > var3)) {
                     var9.put(var19, var12);
                     var10.put(var19, var13 + 1);
                     var11.add(var19);
                  }
               }
            }
         }
      }

      return null;
   }

   private static boolean a(List<Integer> var0, LowResGrid var1, TerrainSampler var2, int var3) {
      for (int var5 : var0) {
         if (var2.groundHeight(var1.worldX(var1.gi(var5)), var1.worldZ(var1.gj(var5))) > (double)var3 + 24.0) {
            return true;
         }
      }

      return false;
   }

   private static List<Integer> a(LowResGrid var0, int var1, double var2, double var4, int var6) {
      HashMap var7 = new HashMap();
      HashMap var8 = new HashMap();
      ArrayDeque var9 = new ArrayDeque();
      var7.put(var1, var1);
      var8.put(var1, 0);
      var9.add(var1);

      while (!var9.isEmpty()) {
         int var10 = (Integer)var9.poll();
         if (var0.height(var10) <= var4) {
            LinkedList var18 = new LinkedList();

            for (int var19 = var10; var19 != var1; var19 = (Integer)var7.get(var19)) {
               var18.addFirst(var19);
            }

            return var18;
         }

         int var11 = (Integer)var8.get(var10);
         if (var11 < var6) {
            int var12 = var0.gi(var10);
            int var13 = var0.gj(var10);

            for (int var14 = 0; var14 < 8; var14++) {
               int var15 = var12 + a[var14];
               int var16 = var13 + b[var14];
               if (var0.valid(var15, var16)) {
                  int var17 = var0.idx(var15, var16);
                  if (!var7.containsKey(var17) && !(var0.height(var17) > var2)) {
                     var7.put(var17, var10);
                     var8.put(var17, var11 + 1);
                     var9.add(var17);
                  }
               }
            }
         }
      }

      return null;
   }

   private static List<NetworkTracer.Pt> b(List<NetworkTracer.Pt> var0, double var1, double var3) {
      int var5 = var0.size();
      if (var5 < 2) {
         return var0;
      } else {
         NetworkTracer.Pt var6 = (NetworkTracer.Pt)var0.get(var5 - 1);
         NetworkTracer.Pt var7 = (NetworkTracer.Pt)var0.get(var5 - 2);
         double var8 = var6.x() - var7.x();
         double var10 = var6.z() - var7.z();
         double var12 = Math.hypot(var8, var10);
         if (var12 < 1.0E-6) {
            return var0;
         } else {
            double var14 = Math.min(var6.y(), var1);
            ArrayList var16 = new ArrayList(var0);
            var16.add(new NetworkTracer.Pt(var6.x() + var8 / var12 * var3, var6.z() + var10 / var12 * var3, var14, var6.acc()));
            return var16;
         }
      }
   }

   public static RiverNetwork connectNetwork(
      PlateKey var0, Function<PlateKey, RiverNetwork> var1, PlateLattice var2, TerrainSampler var3, StreamsConfig var4, long var5
   ) {
      RiverNetwork var7 = (RiverNetwork)var1.apply(var0);
      double var8 = var4.connectRadius();
      int var10 = var4.seaLevel();
      ArrayList var11 = new ArrayList();
      HashSet var12 = new HashSet();
      HashMap var13 = new HashMap();
      var13.put(var0, var7);

      for (RiverSegment var15 : var7.segments()) {
         if (a(var15, var10)) {
            double var16 = var15.x1();
            double var18 = var15.z1();
            double var20 = var15.y1();
            PlateKey[] var22 = var2.platesWithin(var16, var18, var8);

            for (PlateKey var26 : var22) {
               var13.computeIfAbsent(var26, var1);
            }

            NetworkTracer.Connector var30 = a(var16, var18, var20, var22, var13, var3, var4, var10, var8);
            if (var30 != null) {
               List var31 = a(a(a(var30.path(), (double)var4.gridSpacing() * 0.4), var5, d(var16, var18), var4), var4.smoothingPasses());
               a(var31, var30.wStart(), var30.wEnd(), var11);
               var12.add(var15);
            }
         }
      }

      if (var11.isEmpty()) {
         return var7;
      } else {
         ArrayList var27 = new ArrayList(var7.segments().size() + var11.size());

         for (RiverSegment var29 : var7.segments()) {
            if (!var12.contains(var29)) {
               var27.add(var29);
            }
         }

         var27.addAll(var11);
         return assemble(var0, var27, var7.lakes(), var4);
      }
   }

   private static boolean a(RiverSegment var0, int var1) {
      return var0.x1() == var0.x2() && var0.z1() == var0.z2() && var0.y1() > (double)(var1 + 1);
   }

   private static NetworkTracer.Connector a(
      double var0,
      double var2,
      double var4,
      PlateKey[] var6,
      Map<PlateKey, RiverNetwork> var7,
      TerrainSampler var8,
      StreamsConfig var9,
      int var10,
      double var11
   ) {
      double[] var13 = a(var0, var2, var6, var7, var10);
      double var14 = Double.MAX_VALUE;
      double var16 = 0.0;
      double var18 = 0.0;
      double var20 = 0.0;
      double var22 = 0.0;

      for (PlateKey var27 : var6) {
         RiverNetwork var28 = (RiverNetwork)var7.get(var27);
         if (var28 != null) {
            for (RiverSegment var30 : var28.segments()) {
               if (!a(var30, var10)) {
                  double var31 = var30.clampParam(var0, var2);
                  double var33 = var30.x1() + (var30.x2() - var30.x1()) * var31;
                  double var35 = var30.z1() + (var30.z2() - var30.z1()) * var31;
                  double var37 = Math.hypot(var33 - var0, var35 - var2);
                  if (!(var37 < 3.0) && !(var37 > var11) && !(var37 >= var14)) {
                     double var39 = var30.y1() + (var30.y2() - var30.y1()) * var31;
                     if (!(var39 > var4) && a(var13, var33 - var0, var35 - var2)) {
                        var14 = var37;
                        var16 = var33;
                        var18 = var35;
                        var20 = var39;
                        var22 = var30.widthAt(var31);
                     }
                  }
               }
            }
         }
      }

      double[] var41 = a(var0, var2, var13, var8, var9, var10, var11);
      if (var41 != null && var41[3] < var14) {
         var16 = var41[0];
         var18 = var41[1];
         var20 = (double)var10;
         var22 = 0.0;
         var14 = var41[3];
      }

      if (var14 == Double.MAX_VALUE) {
         return null;
      } else if (a(var0, var2, var16, var18, var4, var8) > 4.0) {
         return null;
      } else {
         double var42 = b(var0, var2, var6, var7, var10);
         double var43 = var22 > 0.0 ? var22 : var42;
         ArrayList var44 = new ArrayList(2);
         var44.add(new NetworkTracer.Pt(var0, var2, var4, 0.0));
         var44.add(new NetworkTracer.Pt(var16, var18, var20, 0.0));
         return new NetworkTracer.Connector(var44, var42, var43);
      }
   }

   private static double[] a(double var0, double var2, PlateKey[] var4, Map<PlateKey, RiverNetwork> var5, int var6) {
      double var7 = Double.MAX_VALUE;
      double var9 = 0.0;
      double var11 = 0.0;

      for (PlateKey var16 : var4) {
         RiverNetwork var17 = (RiverNetwork)var5.get(var16);
         if (var17 != null) {
            for (RiverSegment var19 : var17.segments()) {
               if (!a(var19, var6)) {
                  double var20 = Math.hypot(var19.x2() - var0, var19.z2() - var2);
                  double var22 = Math.hypot(var19.x2() - var19.x1(), var19.z2() - var19.z1());
                  if (var20 < var7 && var22 > 1.0E-6) {
                     var7 = var20;
                     var9 = (var19.x2() - var19.x1()) / var22;
                     var11 = (var19.z2() - var19.z1()) / var22;
                  }
               }
            }
         }
      }

      return var7 <= 3.0 ? new double[]{var9, var11} : null;
   }

   private static double b(double var0, double var2, PlateKey[] var4, Map<PlateKey, RiverNetwork> var5, int var6) {
      double var7 = Double.MAX_VALUE;
      double var9 = -1.0;

      for (PlateKey var14 : var4) {
         RiverNetwork var15 = (RiverNetwork)var5.get(var14);
         if (var15 != null) {
            for (RiverSegment var17 : var15.segments()) {
               if (!a(var17, var6)) {
                  double var18 = Math.hypot(var17.x2() - var0, var17.z2() - var2);
                  if (var18 < var7) {
                     var7 = var18;
                     var9 = var17.w2();
                  }
               }
            }
         }
      }

      return var9 > 0.0 ? var9 : 0.5;
   }

   private static boolean a(double[] var0, double var1, double var3) {
      return var0 == null ? true : var0[0] * var1 + var0[1] * var3 >= 0.0;
   }

   private static double[] a(double var0, double var2, double[] var4, TerrainSampler var5, StreamsConfig var6, int var7, double var8) {
      int var10 = Math.max(4, var6.gridSpacing() / 2);
      double var11 = Double.MAX_VALUE;
      double var13 = 0.0;
      double var15 = 0.0;

      for (int var17 = 0; var17 < 16; var17++) {
         double var18 = (double)var17 * (Math.PI / 8);
         double var20 = Math.cos(var18);
         double var22 = Math.sin(var18);
         if (a(var4, var20, var22)) {
            for (double var24 = 3.0; var24 <= var8; var24 += (double)var10) {
               double var26 = var0 + var20 * var24;
               double var28 = var2 + var22 * var24;
               if (a(var5, var26, var28) <= (double)var7) {
                  if (var24 < var11) {
                     var11 = var24;
                     var13 = var26;
                     var15 = var28;
                  }
                  break;
               }
            }
         }
      }

      return var11 == Double.MAX_VALUE ? null : new double[]{var13, var15, (double)var7, var11};
   }

   private static double a(double var0, double var2, double var4, double var6, double var8, TerrainSampler var10) {
      double var11 = Math.hypot(var4 - var0, var6 - var2);
      int var13 = Math.max(1, (int)Math.round(var11 / 4.0));
      double var14 = 0.0;

      for (int var16 = 1; var16 < var13; var16++) {
         double var17 = (double)var16 / (double)var13;
         double var19 = a(var10, var0 + (var4 - var0) * var17, var2 + (var6 - var2) * var17);
         var14 = Math.max(var14, var19 - var8);
      }

      return var14;
   }

   private static void a(List<NetworkTracer.Pt> var0, double var1, double var3, List<RiverSegment> var5) {
      double var6 = 0.0;

      for (int var8 = 0; var8 < var0.size() - 1; var8++) {
         var6 += Math.hypot(
            ((NetworkTracer.Pt)var0.get(var8 + 1)).x() - ((NetworkTracer.Pt)var0.get(var8)).x(),
            ((NetworkTracer.Pt)var0.get(var8 + 1)).z() - ((NetworkTracer.Pt)var0.get(var8)).z()
         );
      }

      double var19 = 0.0;

      for (int var10 = 0; var10 < var0.size() - 1; var10++) {
         NetworkTracer.Pt var11 = (NetworkTracer.Pt)var0.get(var10);
         NetworkTracer.Pt var12 = (NetworkTracer.Pt)var0.get(var10 + 1);
         double var13 = Math.hypot(var12.x() - var11.x(), var12.z() - var11.z());
         double var15 = var6 > 1.0E-9 ? var1 + (var3 - var1) * (var19 / var6) : var1;
         double var17 = var6 > 1.0E-9 ? var1 + (var3 - var1) * ((var19 + var13) / var6) : var3;
         var5.add(new RiverSegment(var11.x(), var11.z(), var11.y(), var15, var12.x(), var12.z(), var12.y(), var17));
         var19 += var13;
      }
   }

   private static int d(double var0, double var2) {
      return (int)(Math.round(var0) * 73856093L ^ Math.round(var2) * 19349663L);
   }

   private static double b(double var0, StreamsConfig var2) {
      double var3 = var0 * (double)var2.gridSpacing() * (double)var2.gridSpacing();
      return Math.min(var2.maxWidth(), var2.baseWidth() + var2.widthScale() * Math.sqrt(var3 / 2500.0));
   }

   private static record Connector() {
      private final List<NetworkTracer.Pt> path;
      private final double wStart;
      private final double wEnd;

      private Connector(List<NetworkTracer.Pt> var1, double var2, double var4) {
         this.path = var1;
         this.wStart = var2;
         this.wEnd = var4;
      }
   }

   private static record LocalBasin() {
      private final int i0;
      private final int j0;
      private final int w;
      private final int h;
      private final double[] hgt;
      private final boolean[] ok;
      private final int fb;
      private final double spill;
      private final double floor;

      private LocalBasin(int var1, int var2, int var3, int var4, double[] var5, boolean[] var6, int var7, double var8, double var10) {
         this.i0 = var1;
         this.j0 = var2;
         this.w = var3;
         this.h = var4;
         this.hgt = var5;
         this.ok = var6;
         this.fb = var7;
         this.spill = var8;
         this.floor = var10;
      }

      List<Integer> poolAt(int var1, LowResGrid var2) {
         if (this.hgt[this.fb] >= (double)var1) {
            return null;
         } else {
            ArrayList var3 = new ArrayList();
            boolean[] var4 = new boolean[this.w * this.h];
            ArrayDeque var5 = new ArrayDeque();
            var4[this.fb] = true;
            var5.add(this.fb);

            while (!var5.isEmpty()) {
               int var6 = (Integer)var5.poll();
               int var7 = var6 % this.w;
               int var8 = var6 / this.w;
               if (var7 == 0 || var8 == 0 || var7 == this.w - 1 || var8 == this.h - 1) {
                  return null;
               }

               var3.add(var2.idx(this.i0 + var7, this.j0 + var8));

               for (int var9 = 0; var9 < 8; var9++) {
                  int var10 = var7 + NetworkTracer.a[var9];
                  int var11 = var8 + NetworkTracer.b[var9];
                  int var12 = var11 * this.w + var10;
                  if (var10 >= 0 && var10 < this.w && var11 >= 0 && var11 < this.h && this.ok[var12] && !var4[var12] && this.hgt[var12] < (double)var1 - 1.0E-6
                     )
                   {
                     var4[var12] = true;
                     var5.add(var12);
                  }
               }
            }

            return var3.isEmpty() ? null : var3;
         }
      }
   }

   private static record PendingOutflow() {
      private final int outlet;
      private final double startLevel;
      private final double acc;

      private PendingOutflow(int var1, double var2, double var4) {
         this.outlet = var1;
         this.startLevel = var2;
         this.acc = var4;
      }
   }

   private static record Pt() {
      private final double x;
      private final double z;
      private final double y;
      private final double acc;

      private Pt(double var1, double var3, double var5, double var7) {
         this.x = var1;
         this.z = var3;
         this.y = var5;
         this.acc = var7;
      }
   }
}
