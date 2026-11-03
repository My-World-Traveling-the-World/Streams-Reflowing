package dev.streamsreflowing.core;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.diag.SampleTimer;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public final class RiverEngine {
   public static final double OCEAN_SKIP_HEIGHT = -1000.0;
   public static final double RIVER_SKIP_HEIGHT = -1001.0;
   private static final double a = 3.0;
   private static final double b = 1.5;
   private static final double c = 2.0;
   private static final double d = 0.5;
   private final long e;
   private final StreamsConfig f;
   private final TerrainSampler g;
   private final WetnessSampler h;
   private final PlateLattice i;
   private final PlateRiverCache j = new PlateRiverCache();
   private final PlateRiverCache k = new PlateRiverCache();
   private final boolean l;
   private final double m;
   private final double n;
   private final ThreadLocal<long[]> o = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0L});
   private final ThreadLocal<Object[]> p = ThreadLocal.withInitial(this::a);
   private final ThreadLocal<Object[]> q = ThreadLocal.withInitial(this::a);
   private final ThreadLocal<double[]> r = ThreadLocal.withInitial(() -> new double[2]);
   private final Set<Object[]> s = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private volatile RiverEngine.BuildListener t;
   private volatile NetworkStore u;
   private static final AtomicInteger v = new AtomicInteger();
   private static volatile long w = System.nanoTime() - 60000000000L;
   private static final int x = 4;
   private static final int y = 1024;
   private static final int z = 30;
   private static final double A = 0.9375;
   private static final double B = 2.0;
   private static final double C = 6.0;
   private static final double D = 6.0;
   private static final double E = 16.0;
   private static final int F = 6;
   private static final double G = 0.1;
   private static final double H = 0.25;
   private volatile int I;
   private final Object J = new Object();
   private boolean K;
   private volatile IntConsumer L;
   private volatile boolean M;
   private static final int N = 8192;
   private final ThreadLocal<long[]> O = ThreadLocal.withInitial(() -> {
      long[] var0 = new long[8192];
      Arrays.fill(var0, Long.MIN_VALUE);
      return var0;
   });
   private final ThreadLocal<ColumnPlan[]> P = ThreadLocal.withInitial(() -> new ColumnPlan[8192]);
   private final ThreadLocal<long[]> Q = ThreadLocal.withInitial(() -> {
      long[] var0 = new long[8192];
      Arrays.fill(var0, Long.MIN_VALUE);
      return var0;
   });
   private final ThreadLocal<ColumnPlan[]> R = ThreadLocal.withInitial(() -> new ColumnPlan[8192]);
   private final ThreadLocal<RiverNetwork[]> S = ThreadLocal.withInitial(() -> new RiverNetwork[8192]);
   private final ThreadLocal<int[]> T = ThreadLocal.withInitial(() -> new int[8192]);
   private final ThreadLocal<RiverNetwork[]> U = ThreadLocal.withInitial(() -> new RiverNetwork[1]);
   private final ThreadLocal<int[]> V = ThreadLocal.withInitial(() -> new int[]{-1});
   private final ThreadLocal<long[]> W = ThreadLocal.withInitial(() -> {
      long[] var0 = new long[8192];
      Arrays.fill(var0, Long.MIN_VALUE);
      return var0;
   });
   private final ThreadLocal<ColumnPlan[]> X = ThreadLocal.withInitial(() -> new ColumnPlan[8192]);
   private volatile boolean Y = true;
   private static final int Z = 6;
   public static final AtomicInteger LEAN_CALLS = new AtomicInteger();
   private static final int aa = 4096;
   private final ThreadLocal<long[]> ab = ThreadLocal.withInitial(() -> {
      long[] var0 = new long[4096];
      Arrays.fill(var0, Long.MIN_VALUE);
      return var0;
   });
   private final ThreadLocal<long[]> ac = ThreadLocal.withInitial(() -> new long[4096]);
   private final ThreadLocal<long[]> ad = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0L, 0L});
   private static volatile BiConsumer<RiverEngine, PlateKey> ae;

   public static int insetFor(double var0, StreamsConfig var2) {
      double var3 = 0.5 + 0.5 * Math.min(1.0, var0 / 3.0);
      return (int)Math.round(var2.ditchInset() * var3);
   }

   private Object[] a() {
      Object[] var1 = new Object[2];
      this.s.add(var1);
      return var1;
   }

   public void releaseThreadMemos() {
      for (Object[] var2 : this.s) {
         var2[0] = null;
         var2[1] = null;
      }

      this.s.clear();
   }

   public RiverEngine(long var1, StreamsConfig var3, TerrainSampler var4) {
      this(var1, var3, var4, (var0, var1x, var2) -> 1.0);
   }

   public RiverEngine(long var1, StreamsConfig var3, TerrainSampler var4, WetnessSampler var5) {
      this.e = var1;
      this.f = var3;
      this.g = var4;
      this.h = var5;
      this.i = new PlateLattice(var1, var3.plateScale(), var3.plateJitter());
      this.l = var3.connectNearbyWater();
      this.m = var3.maxWidth() * (1.0 + var3.bankWidth()) + 2.0;
      this.n = this.l ? Math.max(this.m, var3.connectRadius()) : this.m;
   }

   public StreamsConfig config() {
      return this.f;
   }

   public double sampledHeight(int var1, int var2) {
      return this.g.height(var1, var2);
   }

   public double exactHeight(int var1, int var2) {
      return this.g.groundHeightExact(var1, var2);
   }

   public PlateKey plateAt(int var1, int var2) {
      return this.i.plateAt((double)var1 + 0.5, (double)var2 + 0.5);
   }

   public void setBuildListener(RiverEngine.BuildListener var1) {
      this.t = var1;
   }

   public void setNetworkStore(NetworkStore var1) {
      this.u = var1;
   }

   public RiverNetwork networkFor(PlateKey var1) {
      if (!this.l) {
         return this.baseFor(var1);
      } else {
         Object[] var2 = this.p.get();
         if (var1.equals(var2[0])) {
            return (RiverNetwork)var2[1];
         } else {
            RiverNetwork var3 = this.k
               .get(
                  var1,
                  () -> NetworkTracer.connectNetwork(var1, this::baseFor, this.i, this.g, this.f, this.e),
                  () -> NetworkTracer.connectNetwork(var1, this::baseFor, this.i, this.g, this.f, this.e)
               );
            var2[0] = var1;
            var2[1] = var3;
            return var3;
         }
      }
   }

   public RiverNetwork baseFor(PlateKey var1) {
      Object[] var2 = this.q.get();
      if (var1.equals(var2[0])) {
         return (RiverNetwork)var2[1];
      } else {
         RiverNetwork var3 = this.a(var1);
         var2[0] = var1;
         var2[1] = var3;
         return var3;
      }
   }

   private RiverNetwork a(PlateKey var1) {
      return this.j.get(var1, () -> {
         NetworkStore var2 = this.u;
         if (var2 != null) {
            RiverNetwork var3 = var2.load(var1);
            if (var3 != null) {
               return var3;
            }
         }

         RiverNetwork var4 = this.b(var1);
         if (var2 != null) {
            var2.save(var1, var4);
         }

         return var4;
      }, () -> this.b(var1));
   }

   public static boolean buildingNetworks() {
      return v.get() > 0 || System.nanoTime() - w < 700000000L;
   }

   public static int activeBuildCount() {
      return v.get();
   }

   private RiverNetwork b(PlateKey var1) {
      v.incrementAndGet();
      PlateDomain var2 = new PlateDomain(this.i, var1);
      LowResGrid.enterBuildFocus((var2.minX + var2.maxX) / 2, (var2.minZ + var2.maxZ) / 2);

      RiverNetwork var3;
      try {
         var3 = this.c(var1);
      } finally {
         LowResGrid.exitBuildFocus();
         v.decrementAndGet();
         w = System.nanoTime();
      }

      return var3;
   }

   private RiverNetwork c(PlateKey var1) {
      final TerrainSampler var2 = this.d(var1);
      RiverEngine.BuildListener var3 = this.t;
      if (var3 == null) {
         return NetworkTracer.build(new PlateDomain(this.i, var1), var2, this.h, this.f, this.e);
      } else {
         final int[] var4 = new int[]{0};
         final int[] var5 = new int[]{0};
         TerrainSampler var6 = new TerrainSampler() {
            @Override
            public double height(int var1, int var2x) {
               long var3 = SampleTimer.start();
               double var5x = var2.height(var1, var2x);
               SampleTimer.height(var3);
               if (var5x == -1000.0) {
                  var5[0]++;
               } else {
                  var4[0]++;
               }

               return var5x;
            }

            @Override
            public double surfaceHeight(int var1, int var2x) {
               var4[0]++;
               long var3 = SampleTimer.start();
               double var5x = var2.surfaceHeight(var1, var2x);
               SampleTimer.height(var3);
               return var5x;
            }

            @Override
            public double groundHeight(int var1, int var2x) {
               long var3 = SampleTimer.start();
               double var5x = var2.groundHeight(var1, var2x);
               SampleTimer.height(var3);
               if (var5x == -1000.0) {
                  var5[0]++;
               } else {
                  var4[0]++;
               }

               return var5x;
            }

            @Override
            public double groundHeightExact(int var1, int var2x) {
               long var3 = SampleTimer.start();
               double var5x = var2.groundHeightExact(var1, var2x);
               SampleTimer.height(var3);
               if (var5x == -1000.0) {
                  var5[0]++;
               } else {
                  var4[0]++;
               }

               return var5x;
            }

            @Override
            public boolean bakedCalibration() {
               return var2.bakedCalibration();
            }

            @Override
            public boolean hasEstimate() {
               return var2.hasEstimate();
            }

            @Override
            public boolean useHybridGridFill() {
               return var2.useHybridGridFill();
            }

            @Override
            public double estimate(int var1, int var2x) {
               return var2.estimate(var1, var2x);
            }

            @Override
            public boolean openAt(int var1, int var2x, int var3) {
               return var2.openAt(var1, var2x, var3);
            }

            @Override
            public int waterBiomeClass(int var1, int var2x) {
               long var3 = SampleTimer.start();
               int var5x = var2.waterBiomeClass(var1, var2x);
               SampleTimer.biome(var3);
               return var5x;
            }

            @Override
            public int waterBiomeClassAt(int var1, int var2x, int var3) {
               long var4x = SampleTimer.start();
               int var6 = var2.waterBiomeClassAt(var1, var2x, var3);
               SampleTimer.biome(var4x);
               return var6;
            }

            @Override
            public double riverSurface(int var1, int var2x) {
               return var2.riverSurface(var1, var2x);
            }

            @Override
            public boolean outletBiomeOk(int var1, int var2x, int var3) {
               long var4x = SampleTimer.start();
               boolean var6 = var2.outletBiomeOk(var1, var2x, var3);
               SampleTimer.biome(var4x);
               return var6;
            }
         };
         long var7 = System.nanoTime();
         RiverNetwork var9 = NetworkTracer.build(new PlateDomain(this.i, var1), var6, this.h, this.f, this.e);
         double var10 = (double)(System.nanoTime() - var7) / 1000000.0;
         SampleTimer.plateBuilt();
         if (SampleTimer.ON) {
            Constants.LOG.info("SAMPLETIME plate {} wall {} ms | {}", var1, String.format("%,.1f", var10), SampleTimer.report());
         }

         var3.onBuilt(var1, var4[0], var5[0], var10, var9.segmentCount());
         return var9;
      }
   }

   private static long a(int var0, int var1) {
      return (long)var0 << 32 ^ (long)var1 & 4294967295L;
   }

   private static int a(double var0) {
      return (int)(Math.floor(var0 / 16.0) * 16.0);
   }

   public void setEstCostPersist(IntConsumer var1) {
      this.L = var1;
   }

   public void setEstCostVerdict(int var1) {
      this.I = var1;
      if (var1 == 2) {
         this.M = true;
      }
   }

   private static TerrainSampler a(final TerrainSampler var0) {
      return new TerrainSampler() {
         @Override
         public int waterBiomeClass(int var1, int var2) {
            return var0.waterBiomeClass(var1, var2);
         }

         @Override
         public int waterBiomeClassAt(int var1, int var2, int var3) {
            return var0.waterBiomeClassAt(var1, var2, var3);
         }

         @Override
         public double riverSurface(int var1, int var2) {
            return var0.riverSurface(var1, var2);
         }

         @Override
         public boolean outletBiomeOk(int var1, int var2, int var3) {
            return var0.outletBiomeOk(var1, var2, var3);
         }

         @Override
         public double height(int var1, int var2) {
            return var0.groundHeightExact(var1, var2);
         }

         @Override
         public double groundHeight(int var1, int var2) {
            return var0.groundHeightExact(var1, var2);
         }

         @Override
         public double groundHeightExact(int var1, int var2) {
            return var0.groundHeightExact(var1, var2);
         }

         @Override
         public double surfaceHeight(int var1, int var2) {
            return var0.surfaceHeight(var1, var2);
         }

         @Override
         public boolean openAt(int var1, int var2, int var3) {
            return var0.openAt(var1, var2, var3);
         }
      };
   }

   private TerrainSampler d(PlateKey var1) {
      boolean var2 = this.g.bakedCalibration();
      if (!var2 && !this.g.hasEstimate()) {
         return this.g;
      } else if (this.M) {
         return a(this.g);
      } else {
         boolean var3 = false;
         if (this.I == 0) {
            synchronized (this.J) {
               if (this.I == 0 && !this.K) {
                  this.K = true;
                  var3 = true;
               } else {
                  while (this.I == 0) {
                     try {
                        this.J.wait(1000L);
                     } catch (InterruptedException var34) {
                        Thread.currentThread().interrupt();
                        break;
                     }
                  }
               }
            }

            if (!var3 && this.M) {
               return a(this.g);
            }
         }

         PlateDomain var36 = new PlateDomain(this.i, var1);
         int var5 = Math.max(this.f.gridSpacing(), this.f.terrainSampleSpacing());
         int var6 = 4 * var5;

         while ((long)((var36.maxX - var36.minX) / var6 + 1) * (long)((var36.maxZ - var36.minZ) / var6 + 1) > 1024L) {
            var6 *= 2;
         }

         int var7 = Math.floorDiv(var36.minX, var6) * var6;
         int var8 = Math.floorDiv(var36.minZ, var6) * var6;
         ArrayList var9 = new ArrayList();

         for (int var10 = var7; var10 <= var36.maxX; var10 += var6) {
            for (int var11 = var8; var11 <= var36.maxZ; var11 += var6) {
               if (var36.contains((double)var10, (double)var11) && this.g.height(var10, var11) != -1000.0) {
                  var9.add(new int[]{var10, var11});
               }
            }
         }

         double[] var37 = new double[var9.size()];
         double[] var38 = new double[var9.size()];
         AtomicLong var12 = new AtomicLong();
         AtomicLong var13 = new AtomicLong();
         AtomicInteger var14 = new AtomicInteger();
         AtomicInteger var15 = new AtomicInteger();
         AtomicBoolean var16 = new AtomicBoolean();
         boolean var17 = var3;
         LowResGrid.parallelFor(var9.size(), var10x -> {
            int[] var11x = (int[])var9.get(var10x);
            long var12x = System.nanoTime();
            var37[var10x] = this.g.groundHeightExact(var11x[0], var11x[1]);
            long var14x = System.nanoTime();
            var12.addAndGet(var14x - var12x);
            var14.incrementAndGet();
            if (var17 && var16.get()) {
               var38[var10x] = var37[var10x];
            } else {
               var38[var10x] = this.g.estimate(var11x[0], var11x[1]);
               long var16x = System.nanoTime() - var14x;
               var13.addAndGet(var16x);
               int var18x = var15.incrementAndGet();
               if (var17 && var18x >= 8 && var13.get() / (long)var18x > var12.get() / (long)Math.max(1, var14.get())) {
                  var16.set(true);
               }
            }
         });
         final HashMap var18 = new HashMap(var9.size() * 2);
         HashMap var19 = new HashMap();
         int var20 = 0;
         int var21 = 0;

         for (int var22 = 0; var22 < var9.size(); var22++) {
            int[] var23 = (int[])var9.get(var22);
            double var24 = var37[var22];
            if (var24 != -1000.0) {
               var18.put(a(var23[0], var23[1]), var24);
               double var26 = this.g.groundHeight(var23[0], var23[1]);
               double var28 = var26 - var24;
               if (!(var28 >= 6.0)) {
                  int[] var30 = var19.computeIfAbsent(a(var38[var22]), var0 -> new int[2]);
                  var30[0]++;
                  var20++;
                  if (var28 > 2.0 || var28 < -6.0) {
                     var30[1]++;
                     var21++;
                  }
               }
            }
         }

         boolean var39 = Boolean.getBoolean("streamsreflowing.caldiag");
         final TerrainSampler var40 = this.g;
         double var41 = var14.get() > 0 ? (double)var12.get() / (double)var14.get() : 0.0;
         double var42 = var15.get() > 0 ? (double)var13.get() / (double)var15.get() : 0.0;
         boolean var43 = false;
         if (var3) {
            var43 = (var16.get() || var15.get() >= 8) && var41 > 0.0 && (var16.get() || var42 / var41 > 0.9375);
            int var29 = var43 ? 2 : 1;
            IntConsumer var45 = this.L;
            synchronized (this.J) {
               this.I = var29;
               this.K = false;
               if (var29 == 2) {
                  this.M = true;
               }

               this.J.notifyAll();
            }

            if (var45 != null) {
               var45.accept(var29);
            }

            Constants.LOG
               .info(
                  "Streams Reflowing: fast terrain cost verdict {} for this world ({}% of a real read){}. Remembered for future loads.",
                  var29 == 2 ? "OFF" : "ON",
                  var41 > 0.0 ? Math.round(100.0 * var42 / var41) : -1L,
                  var16.get() ? " (early-abort)" : ""
               );
         }

         if (var43 || var20 >= 30 && (double)var21 > (double)var20 * 0.25) {
            if (var39) {
               Constants.LOG
                  .info(
                     "[SR-BAKEDCAL] plate {},{} failed table verification ({}/{} outside [-{},+{}]) -> real sampling for this plate",
                     var1.cx(),
                     var1.cz(),
                     var21,
                     var20,
                     6.0,
                     2.0
                  );
            }

            return a(var40);
         } else if (!var2) {
            return this.g;
         } else {
            final HashSet var44 = new HashSet();

            for (Entry var47 : var19.entrySet()) {
               int[] var32 = (int[])var47.getValue();
               if (var32[0] >= 6 && (double)var32[1] > (double)var32[0] * 0.1) {
                  var44.add((Integer)var47.getKey());
               }
            }

            if (var39 && !var44.isEmpty()) {
               Constants.LOG
                  .info(
                     "[SR-BAKEDCAL] plate {},{}: bands {} failed verification ({}/{} points outside [-{},+{}]) -> real grades for those bands only; {} anchors kept",
                     var1.cx(),
                     var1.cz(),
                     new TreeSet(var44),
                     var21,
                     var20,
                     6.0,
                     2.0,
                     var18.size()
                  );
            }

            return var18.isEmpty() && var44.isEmpty()
               ? this.g
               : new TerrainSampler() {
                  @Override
                  public int waterBiomeClass(int var1, int var2) {
                     return var40.waterBiomeClass(var1, var2);
                  }

                  @Override
                  public int waterBiomeClassAt(int var1, int var2, int var3) {
                     return var40.waterBiomeClassAt(var1, var2, var3);
                  }

                  @Override
                  public double riverSurface(int var1, int var2) {
                     return var40.riverSurface(var1, var2);
                  }

                  @Override
                  public boolean outletBiomeOk(int var1, int var2, int var3) {
                     return var40.outletBiomeOk(var1, var2, var3);
                  }

                  @Override
                  public double height(int var1, int var2) {
                     Double var3 = (Double)var18.get(RiverEngine.a(var1, var2));
                     return var3 != null ? var3 : var40.height(var1, var2);
                  }

                  @Override
                  public double groundHeight(int var1, int var2) {
                     Double var3 = (Double)var18.get(RiverEngine.a(var1, var2));
                     if (var3 != null) {
                        return var3;
                     } else {
                        return !var44.isEmpty() && var44.contains(RiverEngine.a(var40.estimate(var1, var2)))
                           ? var40.groundHeightExact(var1, var2)
                           : var40.groundHeight(var1, var2);
                     }
                  }

                  @Override
                  public double groundHeightExact(int var1, int var2) {
                     Double var3 = (Double)var18.get(RiverEngine.a(var1, var2));
                     return var3 != null ? var3 : var40.groundHeightExact(var1, var2);
                  }

                  @Override
                  public double surfaceHeight(int var1, int var2) {
                     return var40.surfaceHeight(var1, var2);
                  }

                  @Override
                  public boolean hasEstimate() {
                     return var40.hasEstimate();
                  }

                  @Override
                  public double estimate(int var1, int var2) {
                     return var40.estimate(var1, var2);
                  }

                  @Override
                  public boolean useHybridGridFill() {
                     return var40.useHybridGridFill();
                  }

                  @Override
                  public boolean bakedCalibration() {
                     return var40.bakedCalibration();
                  }

                  @Override
                  public boolean openAt(int var1, int var2, int var3) {
                     return var40.openAt(var1, var2, var3);
                  }
               };
         }
      }
   }

   public RiverNetwork networkAt(int var1, int var2) {
      return this.networkFor(this.plateAt(var1, var2));
   }

   public void setStreamsEnabled(boolean var1) {
      this.Y = var1;
   }

   public ColumnPlan plan(int var1, int var2) {
      if (!this.Y) {
         return ColumnPlan.NONE;
      } else {
         long var3 = (long)var1 << 32 ^ (long)var2 & 4294967295L;
         int var5 = (int)((long)var1 * 31L + (long)var2 & 2147483647L & 8191L);
         long[] var6 = this.O.get();
         ColumnPlan[] var7 = this.P.get();
         if (var6[var5] == var3) {
            ColumnPlan var8 = var7[var5];
            if (var8 != null) {
               return var8;
            }
         }

         ColumnPlan var9 = this.b(var1, var2);
         var6[var5] = var3;
         var7[var5] = var9;
         this.S.get()[var5] = this.U.get()[0];
         this.T.get()[var5] = this.V.get()[0];
         return var9;
      }
   }

   public String planDiag(int var1, int var2) {
      double var3 = (double)var1 + 0.5;
      double var5 = (double)var2 + 0.5;
      RiverSample var7 = this.b(var3, var5, this.n);
      if (var7 == null) {
         return String.format(
            Locale.ROOT, "NOSAMPLE radius=%.1f plates=%d segs=%d", this.n, this.i.platesWithin(var3, var5, this.n).length, this.a(var3, var5, this.n)
         );
      } else {
         double var8 = var7.width();
         double var10 = Math.max(5.0, var8 * this.f.bankWidth());
         return String.format(
            Locale.ROOT,
            "SAMPLED d=%.2f w=%.2f bank=%.2f limit=%.2f over=%+.2f grade=%.1f term=%s src=%s",
            var7.distance(),
            var8,
            var10,
            var8 + var10,
            var7.distance() - (var8 + var10),
            var7.gradeY(),
            var7.terminal(),
            var7.source()
         );
      }
   }

   private int a(double var1, double var3, double var5) {
      int var7 = 0;

      for (PlateKey var11 : this.i.platesWithin(var1, var3, var5)) {
         var7 += this.networkFor(var11).segmentCount();
      }

      return var7;
   }

   private ColumnPlan b(int var1, int var2) {
      double var3 = (double)var1 + 0.5;
      double var5 = (double)var2 + 0.5;
      double var7 = this.n;
      RiverSample var9 = this.c(var3, var5, var7);
      return var9 == null ? ColumnPlan.NONE : this.a(var3, var5, var9, true);
   }

   private ColumnPlan a(double var1, double var3, RiverSample var5, boolean var6) {
      double var7 = var5.width();
      double var9 = var5.distance();
      double var11 = Math.max(5.0, var7 * this.f.bankWidth());
      if (var9 > var7 + var11) {
         return ColumnPlan.NONE;
      } else {
         double var13 = var5.gradeY();
         int var15 = Math.max(this.f.seaLevel() - 1, (int)Math.floor(var13) - this.a(var7, var1, var3, var5.dirX(), var5.dirZ(), var13));
         int var16 = Math.max(1, (int)Math.round(2.0 * var7 * this.f.streamDepth()));
         int var17 = var15 - var16;
         boolean var18 = var9 <= var7;
         double var19 = Math.pow(Math.max(0.0, Math.min(1.0, (var9 - var7) / var11)), this.f.aRiver());
         double var21 = var5.dirX();
         double var23 = var5.dirZ();
         if (var6) {
            double[] var25 = this.a(var1, var3, var9, var7, var5.dirX(), var5.dirZ(), var18, this.n, this.r.get());
            var21 = var25[0];
            var23 = var25[1];
         }

         return new ColumnPlan(
            var18,
            true,
            var15,
            var17,
            var19,
            var9,
            var7,
            var13,
            var5.slope(),
            var5.dirX(),
            var5.dirZ(),
            var5.terminal(),
            var5.source(),
            var5.upstreamSlope(),
            var21,
            var23,
            var5.distToOutlet()
         );
      }
   }

   public ColumnPlan altPlan(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ (long)var2 & 4294967295L;
      int var5 = (int)((long)var1 * 31L + (long)var2 & 2147483647L & 8191L);
      long[] var6 = this.W.get();
      ColumnPlan[] var7 = this.X.get();
      if (var6[var5] == var3) {
         ColumnPlan var8 = var7[var5];
         if (var8 != null) {
            return var8 == ColumnPlan.NONE ? null : var8;
         }
      }

      ColumnPlan var9 = this.c(var1, var2);
      var6[var5] = var3;
      var7[var5] = var9 == null ? ColumnPlan.NONE : var9;
      return var9;
   }

   private ColumnPlan c(int var1, int var2) {
      double var3 = (double)var1 + 0.5;
      double var5 = (double)var2 + 0.5;
      long var7 = (long)var1 << 32 ^ (long)var2 & 4294967295L;
      int var9 = (int)((long)var1 * 31L + (long)var2 & 2147483647L & 8191L);
      RiverNetwork var10 = null;
      int var11 = -1;
      if (this.O.get()[var9] == var7 && this.P.get()[var9] != null) {
         if (!this.P.get()[var9].inCorridor()) {
            return null;
         }

         var10 = this.S.get()[var9];
         var11 = this.T.get()[var9];
      }

      if (var10 == null) {
         RiverSample var12 = this.c(var3, var5, this.n);
         if (var12 == null || !this.a(var12)) {
            return null;
         }

         var10 = this.U.get()[0];
         var11 = var12.segId();
      }

      RiverSample var19 = null;

      for (PlateKey var16 : this.i.platesWithin(var3, var5, this.n)) {
         RiverNetwork var17 = this.networkFor(var16);
         RiverSample var18 = var17.sampleCoveringForeign(var3, var5, this.n, var17 == var10 ? var11 : -1);
         if (var18 != null && (var19 == null || var18.distance() < var19.distance())) {
            var19 = var18;
         }
      }

      return var19 == null ? null : this.a(var3, var5, var19, false);
   }

   private int a(double var1, double var3, double var5, double var7, double var9, double var11) {
      int var13 = insetFor(var1, this.f);
      double var14 = Math.sqrt(var7 * var7 + var9 * var9);
      if (var13 > 0 && !(var14 < 1.0E-6)) {
         if (this.lakeAt((int)Math.floor(var3), (int)Math.floor(var5)) != null) {
            return var13;
         } else {
            double var16 = -var7 / var14;
            double var18 = -var9 / var14;

            for (int var20 = 1; var20 <= 6; var20++) {
               Lake var21 = this.lakeAt((int)Math.floor(var3 + var16 * (double)var20), (int)Math.floor(var5 + var18 * (double)var20));
               if (var21 != null) {
                  int var22 = (int)Math.round((double)var13 * ((double)var20 / 7.0));
                  int var23 = (int)Math.floor(var11) - var21.level();
                  return Math.max(var22, Math.min(var13, var23));
               }
            }

            return var13;
         }
      } else {
         return var13;
      }
   }

   public ColumnPlan planLean(int var1, int var2) {
      if (!this.Y) {
         return ColumnPlan.NONE;
      } else {
         long var3 = (long)var1 << 32 ^ (long)var2 & 4294967295L;
         int var5 = (int)((long)var1 * 31L + (long)var2 & 2147483647L & 8191L);
         long[] var6 = this.Q.get();
         ColumnPlan[] var7 = this.R.get();
         if (var6[var5] == var3) {
            ColumnPlan var8 = var7[var5];
            if (var8 != null) {
               return var8;
            }
         }

         LEAN_CALLS.incrementAndGet();
         RiverSample var10 = this.b((double)var1 + 0.5, (double)var2 + 0.5, this.n);
         ColumnPlan var9 = var10 == null ? ColumnPlan.NONE : this.a((double)var1 + 0.5, (double)var2 + 0.5, var10, false);
         var6[var5] = var3;
         var7[var5] = var9;
         return var9;
      }
   }

   private RiverSample b(double var1, double var3, double var5) {
      RiverSample var7 = null;

      for (PlateKey var11 : this.i.platesWithin(var1, var3, var5)) {
         RiverSample var12 = this.networkFor(var11).sample(var1, var3, var5);
         if (var12 != null && (var7 == null || var12.distance() < var7.distance())) {
            var7 = var12;
         }
      }

      return var7;
   }

   private RiverSample c(double var1, double var3, double var5) {
      RiverSample var7 = null;
      RiverNetwork var8 = null;
      boolean var9 = false;
      boolean var10 = false;

      for (PlateKey var14 : this.i.platesWithin(var1, var3, var5)) {
         RiverNetwork var15 = this.networkFor(var14);
         RiverSample var16 = var15.sampleTiered(var1, var3, var5);
         if (var16 != null) {
            boolean var17 = var16.distance() <= var16.width();
            boolean var18 = this.a(var16);
            if (var7 == null || var17 && !var9 || var17 == var9 && (var18 && !var10 || var18 == var10 && var16.distance() < var7.distance())) {
               var7 = var16;
               var8 = var15;
               var9 = var17;
               var10 = var18;
            }
         }
      }

      this.U.get()[0] = var8;
      this.V.get()[0] = var7 == null ? -1 : var7.segId();
      return var7;
   }

   public boolean lakeNearChunk(int var1, int var2, int var3) {
      double var4 = (double)var1 + 8.0;
      double var6 = (double)var2 + 8.0;

      for (PlateKey var11 : this.i.platesWithin(var4, var6, (double)(this.f.gridSpacing() + var3))) {
         for (Lake var13 : this.baseFor(var11).lakes()) {
            int var14 = var13.cellMinX();
            int var15 = var14 + var13.cellsWide() * var13.spacing();
            if (var15 >= var1 - var3 && var14 <= var1 + 16 + var3) {
               int var16 = var13.cellMinZ();
               int var17 = var16 + var13.cellsHigh() * var13.spacing();
               if (var17 >= var2 - var3 && var16 <= var2 + 16 + var3) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean a(RiverSample var1) {
      double var2 = var1.width();
      return var1.distance() <= var2 + Math.max(5.0, var2 * this.f.bankWidth());
   }

   private double[] a(double var1, double var3, double var5, double var7, double var9, double var11, boolean var13, double var14, double[] var16) {
      double var17 = Math.sqrt(var9 * var9 + var11 * var11);
      if (var13 && !(var17 < 1.0E-6) && !(var7 < 0.5)) {
         double var19 = var9 / var17;
         double var21 = var11 / var17;
         double var23 = -var21;
         double var27 = Math.max(4.0, var7);
         RiverSample var29 = this.b(var1 + var19 * var27, var3 + var21 * var27, var14);
         double var30 = 0.0;
         if (var29 != null) {
            double var32 = Math.sqrt(var29.dirX() * var29.dirX() + var29.dirZ() * var29.dirZ());
            if (var32 > 1.0E-6) {
               var30 = var19 * (var29.dirZ() / var32) - var21 * (var29.dirX() / var32);
            }
         }

         RiverSample var45 = this.b(var1 + var23 * 1.5, var3 + var19 * 1.5, var14);
         double var33 = var45 != null && var45.distance() < var5 ? -var5 : var5;
         double var35 = Math.max(-1.0, Math.min(1.0, var33 / var7));
         double var37 = 1.5 * var30 * var35;
         double var39 = var29 != null ? (var29.width() - var7) / var27 : 0.0;
         var37 += Math.max(-0.5, Math.min(0.5, 2.0 * var39 * var35));
         double var41 = Math.cos(var37);
         double var43 = Math.sin(var37);
         var16[0] = var19 * var41 - var21 * var43;
         var16[1] = var19 * var43 + var21 * var41;
         return var16;
      } else {
         var16[0] = var9;
         var16[1] = var11;
         return var16;
      }
   }

   public boolean inAnyChannel(int var1, int var2) {
      double var3 = (double)var1 + 0.5;
      double var5 = (double)var2 + 0.5;
      double var7 = this.m;

      for (PlateKey var12 : this.i.platesWithin(var3, var5, var7)) {
         if (this.networkFor(var12).inAnyChannel(var3, var5, var7)) {
            return true;
         }
      }

      return false;
   }

   public boolean anyStreamsNear(double var1, double var3, double var5) {
      for (PlateKey var10 : this.i.platesWithin(var1, var3, var5 + this.n)) {
         if (this.networkFor(var10).segmentCount() > 0) {
            return true;
         }
      }

      return false;
   }

   public PlateKey[] platesNear(double var1, double var3, double var5) {
      return this.i.platesWithin(var1, var3, var5 + this.n);
   }

   public boolean inCorridor(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ (long)var2 & 4294967295L;
      long[] var5 = this.o.get();
      if (var5[0] == var3) {
         return var5[1] != 0L;
      } else {
         boolean var6 = false;
         RiverSample var7 = this.b((double)var1 + 0.5, (double)var2 + 0.5, this.n);
         if (var7 != null) {
            double var8 = Math.max(5.0, var7.width() * this.f.bankWidth());
            var6 = var7.distance() <= var7.width() + var8;
         }

         var5[0] = var3;
         var5[1] = var6 ? 1L : 0L;
         return var6;
      }
   }

   public boolean corridorNearChunk(int var1, int var2, int var3) {
      RiverSample var4 = this.b((double)var1 + 8.0, (double)var2 + 8.0, this.n + (double)var3);
      if (var4 == null) {
         return false;
      } else {
         double var5 = var4.width() + Math.max(5.0, var4.width() * this.f.bankWidth());
         return var4.distance() <= 11.32 + (double)var3 + var5;
      }
   }

   public boolean corridorTouchesChunk(int var1, int var2) {
      for (PlateKey var6 : this.i.platesWithin((double)var1 + 8.0, (double)var2 + 8.0, this.n + 12.0)) {
         if (this.networkFor(var6).corridorTouchesBox((double)var1, (double)var2, (double)(var1 + 15), (double)(var2 + 15))) {
            return true;
         }
      }

      return false;
   }

   public long vetoProbeAt(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ (long)var2 & 4294967295L;
      int var5 = (int)((long)var1 * 31L + (long)var2 & 2147483647L & 4095L);
      long[] var6 = this.ab.get();
      long[] var7 = this.ac.get();
      if (var6[var5] == var3) {
         return var7[var5];
      } else {
         long var8 = (long)(var1 >> 4) << 32 ^ (long)(var2 >> 4) & 4294967295L;
         long[] var10 = this.ad.get();
         if (var10[0] != var8) {
            var10[1] = this.corridorTouchesChunk(var1 >> 4 << 4, var2 >> 4 << 4) ? 1L : 0L;
            var10[2] = this.lakeNearChunk(var1 >> 4 << 4, var2 >> 4 << 4, 8) ? 1L : 0L;
            var10[0] = var8;
         }

         int var11 = Integer.MIN_VALUE;
         if (var10[1] != 0L) {
            RiverSample var12 = this.b((double)var1 + 0.5, (double)var2 + 0.5, this.n);
            if (var12 != null && var12.distance() <= var12.width()) {
               var11 = Math.max(this.f.seaLevel() - 1, (int)Math.floor(var12.gradeY()) - insetFor(var12.width(), this.f));
            }
         }

         int var15 = var10[2] != 0L ? this.lakeSurfaceAt(var1, var2) : Integer.MIN_VALUE;
         long var13 = (long)var15 << 32 | (long)var11 & 4294967295L;
         var6[var5] = var3;
         var7[var5] = var13;
         return var13;
      }
   }

   public int lakeSurfaceAt(int var1, int var2) {
      Lake var3 = this.lakeAt(var1, var2);
      return var3 == null ? Integer.MIN_VALUE : var3.level();
   }

   public Lake lakeAt(int var1, int var2) {
      double var3 = (double)var1 + 0.5;
      double var5 = (double)var2 + 0.5;
      Lake var7 = null;

      for (PlateKey var11 : this.i.platesWithin(var3, var5, (double)this.f.gridSpacing())) {
         Lake var12 = this.baseFor(var11).lakeAt(var1, var2);
         if (var12 != null && (var7 == null || var12.level() < var7.level())) {
            var7 = var12;
         }
      }

      return var7;
   }

   public Lake lakeNear(int var1, int var2, int var3) {
      double var4 = (double)var1 + 0.5;
      double var6 = (double)var2 + 0.5;
      Lake var8 = null;

      for (PlateKey var12 : this.i.platesWithin(var4, var6, (double)this.f.gridSpacing() * (1.0 + (double)var3))) {
         Lake var13 = this.baseFor(var12).lakeNear(var1, var2, var3);
         if (var13 != null && (var8 == null || var13.level() < var8.level())) {
            var8 = var13;
         }
      }

      return var8;
   }

   public boolean isPlateCached(PlateKey var1) {
      return (this.l ? this.k : this.j).contains(var1);
   }

   public boolean isPlateReady(PlateKey var1) {
      return (this.l ? this.k : this.j).isReady(var1);
   }

   public boolean platesReadyNear(double var1, double var3, double var5) {
      for (PlateKey var10 : this.platesNear(var1, var3, var5)) {
         if (!this.isPlateReady(var10)) {
            return false;
         }
      }

      return true;
   }

   public static void setBuildRequester(BiConsumer<RiverEngine, PlateKey> var0) {
      ae = var0;
   }

   public CompletableFuture<Void> whenPlatesReadyNear(double var1, double var3, double var5) {
      ArrayList var7 = new ArrayList(2);
      BiConsumer var8 = ae;

      for (PlateKey var12 : this.platesNear(var1, var3, var5)) {
         if (!this.isPlateReady(var12)) {
            var7.add((this.l ? this.k : this.j).whenReady(var12));
            if (var8 != null) {
               var8.accept(this, var12);
            }
         }
      }

      return var7.isEmpty() ? CompletableFuture.completedFuture(null) : CompletableFuture.allOf(var7.toArray(new CompletableFuture[0]));
   }

   public int cachedPlateCount() {
      return this.j.size();
   }

   @FunctionalInterface
   public interface BuildListener {
      void onBuilt(PlateKey var1, int var2, int var3, double var4, int var6);
   }
}
