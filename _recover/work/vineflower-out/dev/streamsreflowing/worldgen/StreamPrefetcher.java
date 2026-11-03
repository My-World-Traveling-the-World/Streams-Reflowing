package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.cache.PlateRiverCache;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class StreamPrefetcher {
   private static final int a = Math.max(
      1, Math.min(Runtime.getRuntime().availableProcessors() - 2, Math.max(2, Runtime.getRuntime().availableProcessors() / 2))
   );
   private static final ThreadPoolExecutor b = new ThreadPoolExecutor(a, a, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), var0 -> {
      Thread var1 = new Thread(var0, "streamsreflowing-prefetch");
      var1.setDaemon(true);
      var1.setPriority(1);
      return var1;
   });
   private static final int c = 2;
   private static final ThreadPoolExecutor d = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), var0 -> {
      Thread var1 = new Thread(var0, "streamsreflowing-urgent");
      var1.setDaemon(true);
      var1.setPriority(5);
      return var1;
   });
   private static final AtomicLong e = new AtomicLong();
   private static final AtomicInteger f = new AtomicInteger();
   private static final int g = 3;
   private static final Semaphore h = new Semaphore(3);
   private static final boolean i = !Boolean.getBoolean("streamsreflowing.nourgentlane");
   private static final int j = 60;
   private static final boolean k = Boolean.getBoolean("streamsreflowing.nojoingrace");
   private static final int l = -1;
   private static final int m = 0;
   private static final int n = 1;
   private static final int o = 2;
   private static final int p = 256;
   private static final Set<Long> q = ConcurrentHashMap.newKeySet();
   private static final Set<Long> r = ConcurrentHashMap.newKeySet();
   private static final Set<Long> s = ConcurrentHashMap.newKeySet();
   private static final Set<Long> t = ConcurrentHashMap.newKeySet();
   private static final int u = 128;
   private static final ConcurrentHashMap<Long, Long> v = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Long, Runnable> w = new ConcurrentHashMap<>();
   private static final int x = 12;
   private static final int y = 8;
   private static final double z = 25.0;
   private static final Map<UUID, ArrayDeque<double[]>> A = new HashMap<>();
   private static final int B = 400;
   private static final int C = 160;
   private static final double D = 24.0;
   private static final double E = 1600.0;
   private static final double F = 1.25;
   private static final double G = 1.6;
   private static long H;
   private static int I;
   private static final boolean J = Boolean.getBoolean("streamsreflowing.noprefetch");
   private static final double K = 42.0;
   private static volatile boolean L;
   private static final int M = 480;
   private static final int N = 3;
   private static final int O = 10;
   private static final double P = 0.5;
   private static final int[] Q = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] R = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final AtomicBoolean S = new AtomicBoolean(false);

   private static void a() {
      int var0 = b.getMaximumPoolSize();
      if (var0 < Math.min(a, 4) && b.getActiveCount() >= var0) {
         int var1 = var0 + 1;
         b.setMaximumPoolSize(var1);
         b.setCorePoolSize(var1);
      }
   }

   private StreamPrefetcher() {
   }

   public static void onServerTick(MinecraftServer var0) {
      I++;
      FlowTrace.maybeReport();
      if ((I & 1023) == 0) {
         RawFlowTileStore.flush();
      }

      if (!J) {
         if (!Services.PLATFORM.generateRivers()) {
            if (Services.PLATFORM.flowVanillaRivers()) {
               for (ServerLevel var29 : var0.getAllLevels()) {
                  if (var29.dimension().equals(Level.OVERWORLD) && !var29.players().isEmpty()) {
                     a(var29);
                  }
               }
            }
         } else {
            boolean var1 = !w.isEmpty();
            if (var1 && (I & 3) == 0) {
               byte var2 = 12;
               Iterator var3 = w.values().iterator();
               ArrayList var4 = new ArrayList(var2);

               while (var3.hasNext() && var4.size() < var2) {
                  var4.add((Runnable)var3.next());
                  var3.remove();
               }

               for (Runnable var6 : var4) {
                  var6.run();
               }
            }

            if ((I & 31) == 0) {
               if (!A.isEmpty()) {
                  HashSet var25 = new HashSet();

                  for (ServerPlayer var34 : var0.getPlayerList().getPlayers()) {
                     var25.add(var34.getUUID());
                  }

                  A.keySet().retainAll(var25);
               }

               ArrayList var26 = new ArrayList();

               for (ServerLevel var35 : var0.getAllLevels()) {
                  if (var35.dimension().equals(Level.OVERWORLD)) {
                     for (ServerPlayer var43 : var35.players()) {
                        var26.add(new int[]{var43.blockPosition().getX(), var43.blockPosition().getZ()});
                     }
                  }
               }

               int[] var32 = new int[var26.size() * 2];

               for (int var36 = 0; var36 < var26.size(); var36++) {
                  var32[var36 * 2] = ((int[])var26.get(var36))[0];
                  var32[var36 * 2 + 1] = ((int[])var26.get(var36))[1];
               }

               LowResGrid.setFocusPoints(var32);
               int var27 = 0;
               boolean var33 = false;
               if (!k) {
                  for (ServerLevel var41 : var0.getAllLevels()) {
                     if (var41.dimension().equals(Level.OVERWORLD)) {
                        for (ServerPlayer var7 : var41.players()) {
                           if (var7.tickCount < 60) {
                              var33 = true;
                              break;
                           }
                        }
                     }
                  }
               }

               for (ServerLevel var42 : var0.getAllLevels()) {
                  if (var42.dimension().equals(Level.OVERWORLD)) {
                     RiverEngine var45 = StreamsWorldgen.engine(var42);
                     if (!var42.players().isEmpty()) {
                        int var46 = (int)Math.max(256.0, var45.config().plateScale());
                        int var8 = Math.min(var46, Math.max(160, var0.getPlayerList().getViewDistance() * 16));
                        HashMap var9 = new HashMap();

                        for (ServerPlayer var11 : var42.players()) {
                           a(var45, var11.blockPosition(), var8);
                           if (!var33) {
                              double[] var12 = a(var11);
                              a(var45, var11.blockPosition(), var12, var46, var9);
                              if (var27 < 2) {
                                 var27 = Math.max(var27, a(var45, var11, var46));
                              }
                           }
                        }

                        if (var33) {
                           a(var42);
                           a(var42, var45, var0);
                        } else {
                           d();
                           ArrayList var47 = new ArrayList(var9.entrySet());
                           var47.sort(Entry.comparingByValue());

                           for (Entry var50 : var47) {
                              a(var45, (PlateKey)var50.getKey(), 0);
                           }

                           ServerLevel var49 = var0.getLevel(Level.NETHER);
                           if (var49 != null && !var49.players().isEmpty()) {
                              double var51 = var49.dimensionType().coordinateScale();

                              for (ServerPlayer var15 : var49.players()) {
                                 double var16 = var15.getX() * var51;
                                 double var18 = var15.getZ() * var51;

                                 for (PlateKey var23 : var45.platesNear(var16, var18, 0.0)) {
                                    a(var45, var23, 0);
                                 }
                              }
                           }

                           a(var45, var42);
                           a(var42);
                           a(var42, var45, var0);
                        }
                     }
                  }
               }

               int var28 = Math.max(0, var27 - b());
               if (var28 >= 1 && var28 <= 2 && a(var0) > 42.0) {
                  var28--;
               }

               int var39 = w.size();
               if (!var33 && (var1 || var39 > 0)) {
                  var28 = Math.max(var28, 2);
               }

               a(var28);
            }
         }
      }
   }

   private static double a(MinecraftServer var0) {
      return (double)var0.getAverageTickTimeNanos() / 1000000.0;
   }

   private static int b() {
      long var0 = System.nanoTime();
      long var2 = H;
      H = var0;
      if (var2 == 0L) {
         return 0;
      } else {
         double var4 = (double)(var0 - var2) / 1000000.0;
         if (var4 >= 2560.0) {
            return 2;
         } else {
            return var4 >= 2000.0 ? 1 : 0;
         }
      }
   }

   private static int a(RiverEngine var0, ServerPlayer var1, int var2) {
      int var3 = var1.getBlockX();
      int var4 = var1.getBlockZ();
      if (!var0.isPlateReady(var0.plateAt(var3, var4)) || a(var0, var3, var4, 16)) {
         return 4;
      } else if (a(var0, var3, var4, var2 / 3)) {
         return 3;
      } else if (a(var0, var3, var4, var2 * 2 / 3)) {
         return 2;
      } else {
         return a(var0, var3, var4, var2 * 5 / 3) ? 1 : 0;
      }
   }

   private static boolean a(RiverEngine var0, int var1, int var2, int var3) {
      for (int var4 = 0; var4 < 16; var4++) {
         double var5 = (double)var4 * (Math.PI / 8);
         int var7 = var1 + (int)Math.round(Math.cos(var5) * (double)var3);
         int var8 = var2 + (int)Math.round(Math.sin(var5) * (double)var3);
         if (!var0.isPlateReady(var0.plateAt(var7, var8))) {
            return true;
         }
      }

      return false;
   }

   private static void a(int var0) {
      int var1 = var0 >= 4 ? a : (var0 == 3 ? Math.max(1, a * 3 / 4) : (var0 == 2 ? Math.max(1, a / 3) : (var0 == 1 ? Math.max(1, a / 5) : 1)));
      if (i) {
         var1 = Math.max(var1, Math.min(3, f.get()));
      }

      if (b.getCorePoolSize() != var1 || b.getMaximumPoolSize() != var1) {
         if (var1 >= b.getMaximumPoolSize()) {
            b.setMaximumPoolSize(var1);
            b.setCorePoolSize(var1);
         } else {
            b.setCorePoolSize(var1);
            b.setMaximumPoolSize(var1);
         }
      }

      LowResGrid.setBackgroundThrottle(var0);
   }

   public static void clearPending() {
      b.getQueue().clear();
      d.getQueue().clear();
      q.clear();
      r.clear();
      s.clear();
      t.clear();
      w.clear();
      v.clear();
      A.clear();
   }

   private static void a(ServerLevel var0) {
      boolean var1 = Services.PLATFORM.vanillaWaterFlow() || !Services.PLATFORM.streamWaterState(0).is(Blocks.WATER);
      if (Services.PLATFORM.flowVanillaRivers() && var1) {
         RiverFlowField.BiomeClass var2 = RiverFlowFields.classifierFor(var0);
         RiverFlowField.OrientationField var3 = RiverFlowFields.orientationFor(var0);
         int var4 = RiverFlowFields.regionSize();

         for (ServerPlayer var6 : var0.players()) {
            int var7 = var6.getBlockX();
            int var8 = var6.getBlockZ();

            for (int var9 = -1; var9 <= 1; var9++) {
               for (int var10 = -1; var10 <= 1; var10++) {
                  a(var2, var3, var7 + var10 * var4, var8 + var9 * var4);
               }
            }
         }
      }
   }

   private static void a(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      long var4 = RiverFlowFields.owningRegionKey(var2, var3);
      if (s.add(var4)) {
         b.execute(new StreamPrefetcher.PrioritizedTask(1, e.getAndIncrement(), () -> {
            try {
               RiverFlowFields.ensureRegion(var0, var1, var2, var3);
            } catch (Throwable var10) {
               Constants.LOG.warn("Prefetch of river-flow field near ({},{}) failed", var2, var3, var10);
            } finally {
               s.remove(var4);
            }
         }));
      }
   }

   private static void a(ServerLevel var0, RiverEngine var1, MinecraftServer var2) {
      if (Services.PLATFORM.vanillaWaterFlow()) {
         RiverFlowField.BiomeClass var3 = RiverFlowFields.classifierFor(var0);
         RiverFlowField.OrientationField var4 = RiverFlowFields.orientationFor(var0);
         boolean var5 = Services.PLATFORM.flowVanillaRivers();
         int var6 = var2.getPlayerList().getViewDistance() + 2;
         int var7 = 128;

         for (ServerPlayer var9 : var0.players()) {
            int var10 = var9.getBlockX() >> 4;
            int var11 = var9.getBlockZ() >> 4;

            for (int var12 = -var6; var12 <= var6 && var7 > 0; var12++) {
               for (int var13 = -var6; var13 <= var6 && var7 > 0; var13++) {
                  int var14 = var10 + var13;
                  int var15 = var11 + var12;
                  if (StreamFlowGrid.peek(var14, var15) == null) {
                     submitGridBuild(var0, var1, var3, var4, var5, var14, var15, false, null);
                     var7--;
                  }
               }
            }
         }
      }
   }

   public static void submitGridBuild(
      ServerLevel var0,
      RiverEngine var1,
      RiverFlowField.BiomeClass var2,
      RiverFlowField.OrientationField var3,
      boolean var4,
      int var5,
      int var6,
      boolean var7,
      Runnable var8
   ) {
      long var9 = (long)var5 << 32 ^ (long)var6 & 4294967295L;
      if (var7 || t.add(var9)) {
         (var7 ? d : b).execute(new StreamPrefetcher.PrioritizedTask(var7 ? 0 : 2, e.getAndIncrement(), () -> {
            boolean var11 = true;

            try {
               try {
                  if (StreamFlowGrid.peek(var5, var6) == null || StreamFlowGrid.isIncomplete(var5, var6)) {
                     if (!a(var1, var2, var3, var4, var5, var6, var7)) {
                        if (var7) {
                           w.putIfAbsent(var9, () -> submitGridBuild(var0, var1, var2, var3, var4, var5, var6, true, var8));
                           boolean var18 = v.putIfAbsent(var9, System.currentTimeMillis()) == null;
                           if (var18) {
                              StreamFlowGrid.buildProvisional(var0, var1, var2, var3, var4, var5, var6);
                              if (var8 != null) {
                                 var8.run();
                              }
                           }

                           var11 = false;
                        }

                        return;
                     }

                     StreamFlowGrid.getOrBuild(var0, var1, var2, var3, var4, var5, var6);
                  }

                  Long var12 = v.remove(var9);
                  if (var12 != null && FlowTrace.ON) {
                     FlowTrace.parkedDelivery(System.currentTimeMillis() - var12);
                     return;
                  }
               } catch (Throwable var16) {
                  Constants.LOG.warn("Flow-grid build at chunk ({},{}) failed", var5, var6, var16);
               }
            } finally {
               if (!var7) {
                  t.remove(var9);
               }

               if (var11 && var8 != null) {
                  var8.run();
               }
            }
         }));
      }
   }

   private static boolean a(
      RiverEngine var0, RiverFlowField.BiomeClass var1, RiverFlowField.OrientationField var2, boolean var3, int var4, int var5, boolean var6
   ) {
      boolean var7 = true;
      int var8 = (var4 << 4) + 8;
      int var9 = (var5 << 4) + 8;

      for (PlateKey var13 : var0.platesNear((double)var8, (double)var9, 25.0)) {
         if (!var0.isPlateCached(var13)) {
            if (!var6) {
               return false;
            }

            FlowTrace.plateRequests.incrementAndGet();
            a(var0, var13, 0);
            var7 = false;
         }
      }

      if (var3) {
         for (byte var14 = -1; var14 <= 1; var14 += 2) {
            for (byte var15 = -1; var15 <= 1; var15 += 2) {
               int var16 = var8 + var15 * 17;
               int var17 = var9 + var14 * 17;
               if (!RiverFlowFields.isRegionReady(var16, var17)) {
                  if (!var6) {
                     return false;
                  }

                  FlowTrace.fieldRequests.incrementAndGet();
                  a(var1, var2, var16, var17);
                  var7 = false;
               }
            }
         }
      }

      return var7;
   }

   public static String plateDiag(RiverEngine var0, PlateKey var1) {
      long var2 = (long)var1.cx() << 32 ^ (long)var1.cz() & 4294967295L;
      String var4 = "none";

      for (Runnable var6 : b.getQueue()) {
         if (var6 instanceof StreamPrefetcher.PrioritizedTask var7 && var7.d == var2) {
            var4 = var7.b == -1 ? "URGENT" : (var7.b == 0 ? "ahead" : "passive");
            break;
         }
      }

      return "cached=" + var0.isPlateCached(var1) + " marker=" + q.contains(var2) + " queued=" + var4;
   }

   public static String laneDiag() {
      return "exec="
         + b.getPoolSize()
         + "/"
         + b.getCorePoolSize()
         + " queue="
         + b.getQueue().size()
         + " activeBuilds="
         + RiverEngine.activeBuildCount()
         + " urgentWaiters="
         + LowResGrid.urgentWaiterCount();
   }

   private static void a(RiverEngine var0, PlateKey var1, int var2) {
      if (!L || var2 == -1) {
         if (!var0.isPlateCached(var1)) {
            long var3 = (long)var1.cx() << 32 ^ (long)var1.cz() & 4294967295L;
            if (q.add(var3)) {
               if (i && var2 == -1) {
                  f.incrementAndGet();
               }

               b.execute(new StreamPrefetcher.PrioritizedTask(var2, e.getAndIncrement(), var3, () -> a(var0, var1, var3, var2)));
               if (i && var2 == -1) {
                  a();
               }
            } else {
               if (i && var2 == -1) {
                  for (Runnable var6 : b.getQueue()) {
                     if (var6 instanceof StreamPrefetcher.PrioritizedTask var7 && var7.d == var3 && var7.b != -1 && b.getQueue().remove(var7)) {
                        f.incrementAndGet();
                        b.execute(new StreamPrefetcher.PrioritizedTask(-1, e.getAndIncrement(), var3, () -> a(var0, var1, var3, -1)));
                        a();
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   public static void submitChunkDemand(RiverEngine var0, PlateKey var1) {
      a(var0, var1, -1);
   }

   private static boolean c() {
      for (Runnable var1 : b.getQueue()) {
         if (var1 instanceof StreamPrefetcher.PrioritizedTask var2 && var2.b == -1) {
            return true;
         }
      }

      return false;
   }

   private static void a(RiverEngine var0, PlateKey var1, long var2, int var4) {
      if (L && var4 != -1) {
         q.remove(var2);
      } else {
         boolean var5 = false;

         try {
            if (var4 != -1) {
               long var6 = System.nanoTime() + 60000000000L;

               while (LowResGrid.urgentWaiterCount() > 0 && RiverEngine.activeBuildCount() >= 4 && System.nanoTime() < var6) {
                  if (L) {
                     return;
                  }

                  if (var0.isPlateCached(var1)) {
                     return;
                  }

                  if (c()) {
                     b.execute(new StreamPrefetcher.PrioritizedTask(var4, e.getAndIncrement(), var2, () -> a(var0, var1, var2, var4)));
                     var5 = true;
                     return;
                  }

                  Thread.sleep(250L);
               }
            }

            if (i && var4 == -1) {
               h.acquireUninterruptibly();
            }

            try {
               if (!i || var4 != -1 || !var0.isPlateReady(var1)) {
                  var0.networkFor(var1);
                  return;
               }
            } finally {
               if (i && var4 == -1) {
                  h.release();
               }
            }
         } catch (InterruptedException var19) {
            Thread.currentThread().interrupt();
            return;
         } catch (Throwable var20) {
            Constants.LOG.warn("Prefetch of river region {} failed", var1, var20);
            return;
         } finally {
            if (!var5) {
               q.remove(var2);
            }

            if (i && var4 == -1) {
               f.decrementAndGet();
            }
         }
      }
   }

   private static void d() {
      for (Runnable var1 : b.getQueue()) {
         if (var1 instanceof StreamPrefetcher.PrioritizedTask) {
            StreamPrefetcher.PrioritizedTask var2 = (StreamPrefetcher.PrioritizedTask)var1;
            if (var2.b == 0 && var2.d != Long.MIN_VALUE && b.getQueue().remove(var2)) {
               q.remove(var2.d);
            }
         }
      }
   }

   private static void a(RiverEngine var0, ServerLevel var1) {
      int var2 = Services.PLATFORM.prefetchRadius();

      for (ServerPlayer var4 : var1.players()) {
         BlockPos var5 = var4.blockPosition();
         PlateKey var6 = var0.plateAt(var5.getX(), var5.getZ());

         for (int var7 = -var2; var7 <= var2; var7++) {
            for (int var8 = -var2; var8 <= var2; var8++) {
               if (b.getQueue().size() >= 256) {
                  return;
               }

               a(var0, new PlateKey(var6.cx() + var8, var6.cz() + var7), 2);
            }
         }
      }
   }

   private static void a(RiverEngine var0, BlockPos var1, int var2) {
      a(var0, var0.plateAt(var1.getX(), var1.getZ()));
      a(var0, var0.plateAt(var1.getX() + var2, var1.getZ() + var2));
      a(var0, var0.plateAt(var1.getX() + var2, var1.getZ() - var2));
      a(var0, var0.plateAt(var1.getX() - var2, var1.getZ() + var2));
      a(var0, var0.plateAt(var1.getX() - var2, var1.getZ() - var2));
   }

   public static void submitUrgentAt(ServerLevel var0, int var1, int var2) {
      try {
         if (var0 == null || !Services.PLATFORM.generateRivers() || !var0.dimension().equals(Level.OVERWORLD)) {
            return;
         }

         RiverEngine var3 = StreamsWorldgen.engine(var0);
         int var4 = (int)Math.max(256.0, var3.config().plateScale());
         int var5 = Math.min(var4, Math.max(160, var0.getServer().getPlayerList().getViewDistance() * 16));
         a(var3, new BlockPos(var1, var0.getSeaLevel(), var2), var5);
      } catch (Throwable var6) {
         Constants.LOG.warn("Urgent prefetch for teleport destination ({},{}) failed (ignored)", var1, var2, var6);
      }
   }

   public static boolean isServerStopping() {
      return L;
   }

   public static void onServerStopping() {
      L = true;
      PlateRiverCache.releaseAllWaiters();

      for (Runnable var1 : b.getQueue()) {
         if (var1 instanceof StreamPrefetcher.PrioritizedTask) {
            StreamPrefetcher.PrioritizedTask var2 = (StreamPrefetcher.PrioritizedTask)var1;
            if (var2.b != -1 && b.getQueue().remove(var2) && var2.d != Long.MIN_VALUE) {
               q.remove(var2.d);
            }
         }
      }

      for (Runnable var4 : d.getQueue()) {
         if (var4 instanceof StreamPrefetcher.PrioritizedTask) {
            StreamPrefetcher.PrioritizedTask var5 = (StreamPrefetcher.PrioritizedTask)var4;
            if (d.getQueue().remove(var5) && var5.d != Long.MIN_VALUE) {
               r.remove(var5.d);
            }
         }
      }

      clearPending();
      LowResGrid.setBackgroundThrottle(4);
   }

   public static void warmWorldOrigin(RiverEngine var0) {
      L = false;
      PlateRiverCache.reopen();
      LowResGrid.setFocusPoints(new int[]{0, 0});
      PlateKey var1 = var0.plateAt(0, 0);
      a(var0, var1);
      HashSet var2 = new HashSet();

      for (short var3 = -480; var3 <= 480; var3 += 160) {
         for (short var4 = -480; var4 <= 480; var4 += 160) {
            PlateKey var5 = var0.plateAt(var4, var3);
            if (!var5.equals(var1)) {
               var2.add(var5);
            }
         }
      }

      for (PlateKey var7 : var2) {
         a(var0, var7, 0);
      }
   }

   private static void a(RiverEngine var0, PlateKey var1) {
      if (!var0.isPlateCached(var1)) {
         long var2 = (long)var1.cx() << 32 ^ (long)var1.cz() & 4294967295L;
         if (r.add(var2)) {
            d.execute(new StreamPrefetcher.PrioritizedTask(-1, e.getAndIncrement(), () -> {
               try {
                  var0.networkFor(var1);
               } catch (Throwable var8) {
                  Constants.LOG.warn("Urgent prefetch of river region {} failed", var1, var8);
               } finally {
                  r.remove(var2);
               }
            }));
         }
      }
   }

   private static void a(RiverEngine var0, BlockPos var1, double[] var2, int var3, Map<PlateKey, Double> var4) {
      int var5 = Math.max(256, var3 / 2);
      int var6 = var3;

      for (int var7 = -var3; var7 <= var6; var7 += var5) {
         for (int var8 = -var6; var8 <= var6; var8 += var5) {
            double var9 = Math.hypot((double)var8, (double)var7);
            double var11 = var9;
            if (var9 > 1.0E-6 && (var2[0] != 0.0 || var2[1] != 0.0)) {
               double var13 = ((double)var8 * var2[0] + (double)var7 * var2[1]) / var9;
               var11 = var9 * (1.25 - 0.75 * var13);
            }

            var4.merge(var0.plateAt(var1.getX() + var8, var1.getZ() + var7), var11, Math::min);
         }
      }
   }

   private static double[] a(ServerPlayer var0) {
      ArrayDeque var1 = A.computeIfAbsent(var0.getUUID(), var0x -> new ArrayDeque<>());
      var1.addLast(new double[]{(double)I, var0.getX(), var0.getZ()});

      while (!var1.isEmpty() && (double)I - ((double[])var1.peekFirst())[0] > 400.0) {
         var1.removeFirst();
      }

      double[] var2 = (double[])var1.peekFirst();
      if (var2 != null && !((double)I - var2[0] < 160.0)) {
         double var3 = var0.getX() - var2[1];
         double var5 = var0.getZ() - var2[2];
         double var7 = Math.hypot(var3, var5);
         return var7 >= 24.0 ? new double[]{var3 / var7, var5 / var7} : new double[]{0.0, 0.0};
      } else {
         return new double[]{0.0, 0.0};
      }
   }

   public static void biomeBench(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      int var4 = (int)Math.round(var0.config().plateScale());
      int var5 = 4 * var4;
      byte var6 = 8;
      RiverFlowField.BiomeClass var7 = RiverFlowFields.classifierFor(var1);
      Thread var8 = new Thread(
         () -> {
            int var6x = var5 / 2;
            long var7x = 0L;
            int var9 = var3 - var6x;

            while (var9 < var3 - var6x + var6 * 48) {
               for (int var10 = var2 - var6x; var10 < var2 - var6x + var6 * 48; var10 += var6) {
                  var7x += (long)var7.classify(var10, var9);
               }

               var9 += var6;
            }

            long var19 = System.nanoTime();
            long var11 = 0L;
            long var13 = 0L;

            for (int var15 = var3 - var6x; var15 <= var3 + var6x; var15 += var6) {
               for (int var16 = var2 - var6x; var16 <= var2 + var6x; var16 += var6) {
                  if (var7.classify(var16, var15) == 2) {
                     var13++;
                  }

                  var11++;
               }
            }

            double var20 = (double)(System.nanoTime() - var19) / 1000000.0;
            double var17 = var20 * 1000.0 / (double)Math.max(1L, var11);
            Constants.LOG
               .info(
                  "[SR-BIOMEBENCH] 4x plateScale tile {}x{} (plateScale={}), spacing {}: {} getNoiseBiome samples in {} ms = {} us/call, {}% ocean [warm {}]",
                  var5,
                  var5,
                  var4,
                  var6,
                  var11,
                  String.format("%.0f", var20),
                  String.format("%.3f", var17),
                  String.format("%.1f", 100.0 * (double)var13 / (double)Math.max(1L, var11)),
                  var7x & 1L
               );
         },
         "streamsreflowing-biomebench"
      );
      var8.setDaemon(true);
      var8.start();
   }

   public static void diagnoseColumn(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      a(var0, var1, var2, var3);
      a(var0, var1, new ChunkPos(new BlockPos(var2, 0, var3)));
   }

   public static void autoTrace(MinecraftServer var0, int var1, int var2) {
      ServerLevel var3 = var0.overworld();
      int var4 = var1 >> 4;
      int var5 = var2 >> 4;
      Constants.LOG
         .info(
            "[SR-AUTOTRACE] target=({},{}) chunk=({},{}) -- force-generating a 5x5 chunk block (the precise plate region from the diagnostics); nothing else is loaded",
            var1,
            var2,
            var4,
            var5
         );
      long var6 = FormSampler.keptIr.sum();
      long var8 = FormSampler.handedBack.sum();
      long var10 = System.nanoTime();
      int var12 = 0;

      for (int var13 = -2; var13 <= 2; var13++) {
         for (int var14 = -2; var14 <= 2; var14++) {
            var3.getChunk(var4 + var13, var5 + var14);
            var12++;
         }
      }

      long var32 = (System.nanoTime() - var10) / 1000000L;
      long var15 = FormSampler.keptIr.sum() - var6;
      long var17 = FormSampler.handedBack.sum() - var8;
      Constants.LOG.info("[SR-AUTOTRACE] generated {} chunks in {} ms; dumping placed-column diagnostics", var12, var32);
      Constants.LOG
         .info(
            "[SR-AUTOTRACE] height queries through the walk during that build: {} served, {} handed to the host, {} total{}",
            var15,
            var17,
            var15 + var17,
            var15 + var17 == 0L
               ? " (zero -- either the walk is off, or this build asks it for nothing)"
               : String.format(Locale.ROOT, "; %.1f us each if they owned the whole %d ms", (double)var32 * 1000.0 / (double)(var15 + var17), var32)
         );
      RiverEngine var19 = StreamsWorldgen.engine(var3);

      for (byte var20 = -24; var20 <= 24; var20 += 4) {
         diagnoseColumn(var19, var3, var1, var2 + var20);
      }

      traceColumn(var19, var3, var1, var2);
      superTrace(var19, var3, var1, var2);
      spoutScan(var19, var3, var1, var2, 768);
      String var33 = System.getProperty("streamsreflowing.sweep");
      if (var33 != null && !var33.isBlank()) {
         for (String var24 : var33.split(";")) {
            String[] var25 = var24.split(",");

            try {
               int var26 = Integer.parseInt(var25[0].trim());
               int var27 = Integer.parseInt(var25[1].trim());

               for (int var28 = -1; var28 <= 1; var28++) {
                  for (int var29 = -1; var29 <= 1; var29++) {
                     var3.getChunk((var26 >> 4) + var28, (var27 >> 4) + var29);
                  }
               }

               diagnoseColumn(var19, var3, var26, var27);
               traceColumn(var19, var3, var26, var27);
               superTrace(var19, var3, var26, var27);
               spoutScan(var19, var3, var26, var27, 768);
            } catch (RuntimeException var31) {
               Constants.LOG.warn("[SR-AUTOTRACE] bad sweep target '{}'", var24);
            }
         }
      }

      String var34 = System.getProperty("streamsreflowing.monoaudit");
      if (var34 != null && !var34.isBlank()) {
         String[] var35 = var34.split(",");

         try {
            int var36 = Integer.parseInt(var35[0].trim());
            int var37 = Integer.parseInt(var35[1].trim());
            int var38 = var35.length >= 3 ? Integer.parseInt(var35[2].trim()) : 96;
            monoAudit(var19, var3, var36, var37, var38);
         } catch (RuntimeException var30) {
            Constants.LOG.warn("[SR-MONOAUDIT] bad streamsreflowing.monoaudit '{}'", var34, var30);
         }
      }

      Constants.LOG.info("[SR-AUTOTRACE] diagnostics complete; halting server");
      var0.halt(false);
   }

   public static void monoAudit(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      ChunkGenerator var5 = var1.getChunkSource().getGenerator();
      RandomState var6 = var1.getChunkSource().randomState();
      int var7 = var4 * 2 + 1;
      int[] var8 = new int[var7 * var7];
      double[] var9 = new double[var7 * var7];
      double[] var10 = new double[var7 * var7];
      int[] var11 = new int[var7 * var7];
      Arrays.fill(var8, Integer.MIN_VALUE);
      Arrays.fill(var11, Integer.MIN_VALUE);
      PlateKey var12 = var0.plateAt(var2, var3);
      byte var13 = 32;

      for (int var14 = -var4; var14 <= var4; var14++) {
         for (int var15 = -var4; var15 <= var4; var15++) {
            int var16 = var2 + var15;
            int var17 = var3 + var14;
            if (var0.plateAt(var16, var17).equals(var12)
               && var0.plateAt(var16 - var13, var17).equals(var12)
               && var0.plateAt(var16 + var13, var17).equals(var12)
               && var0.plateAt(var16, var17 - var13).equals(var12)
               && var0.plateAt(var16, var17 + var13).equals(var12)) {
               int var18 = (var14 + var4) * var7 + var15 + var4;
               ColumnPlan var19 = var0.plan(var16, var17);
               int var20 = var5.getBaseHeight(var16, var17, Types.OCEAN_FLOOR_WG, var1, var6) - 1;
               int var21 = var20;
               int var22 = Integer.MIN_VALUE;
               if (var19.inCorridor()) {
                  var22 = var19.waterSurfaceY();
                  if (var19.distToOutlet() <= 24.0) {
                     int var23 = RiverCarverFeature.nearbyLakeLevel(var0, var16, var17, var1.getMinBuildHeight(), var1.getMaxBuildHeight());
                     if (var23 != Integer.MIN_VALUE) {
                        var22 = Math.max(var22, Math.min(var23, var19.waterSurfaceY() + 4));
                     }
                  }

                  int var50 = var19.waterSurfaceY() + (int)Math.round(var19.bankBlend() * (double)Math.max(0, var20 - var19.waterSurfaceY()));
                  var21 = Math.min(var20, var50);
                  if (var21 < var20 && var0.lakeAt(var16, var17) == null && var19.distToOutlet() > 24.0) {
                     int var24 = RiverCarverFeature.adjacentCoveringLakeLevel(var0, var16, var17);
                     if (var24 != Integer.MIN_VALUE && var20 > var24 && var21 <= var24) {
                        var21 = Math.min(var20, var24 + 1);
                     }
                  }

                  double var53 = Math.hypot(var19.flowDirX(), var19.flowDirZ());
                  if (var53 > 1.0E-6) {
                     var9[var18] = var19.flowDirX() / var53;
                     var10[var18] = var19.flowDirZ() / var53;
                  }
               }

               int var51 = var0.lakeSurfaceAt(var16, var17);
               if (var51 != Integer.MIN_VALUE && var51 > var22) {
                  var22 = var51;
                  var9[var18] = 0.0;
                  var10[var18] = 0.0;
               }

               var8[var18] = var22;
               var11[var18] = var21;
            }
         }
      }

      int var39 = 0;
      int[] var40 = new int[33];
      StringBuilder var41 = new StringBuilder();
      int var42 = 0;

      for (int var43 = -var4; var43 <= var4; var43++) {
         for (int var45 = -var4; var45 <= var4; var45++) {
            int var47 = (var43 + var4) * var7 + var45 + var4;
            int var48 = var8[var47];
            if (var48 != Integer.MIN_VALUE) {
               int var49 = 0;
               int var52 = 0;
               int var54 = 0;
               int var25 = 0;

               for (int var26 = 0; var26 < 8; var26++) {
                  int var27 = Q[var26];
                  int var28 = R[var26];
                  double var29 = Math.sqrt((double)(var27 * var27 + var28 * var28));
                  if (!(var9[var47] * (double)var27 / var29 + var10[var47] * (double)var28 / var29 >= 0.5)) {
                     int var31 = Integer.MIN_VALUE;

                     for (int var32 = 1; var32 <= 10; var32++) {
                        int var33 = var45 + var27 * var32;
                        int var34 = var43 + var28 * var32;
                        if (var33 < -var4 || var33 > var4 || var34 < -var4 || var34 > var4) {
                           break;
                        }

                        int var35 = (var34 + var4) * var7 + var33 + var4;
                        int var36 = var8[var35];
                        int var37 = var11[var35];
                        if (var37 == Integer.MIN_VALUE) {
                           break;
                        }

                        if (var36 != Integer.MIN_VALUE && var32 >= 2 && var48 - var36 >= 3 && var31 < var48) {
                           int var38 = var48 - var36;
                           if (var38 > var49) {
                              var49 = var38;
                              var52 = var36;
                              var54 = var2 + var33;
                              var25 = var3 + var34;
                           }
                           break;
                        }

                        var31 = Math.max(var31, Math.max(var37, var36));
                        if (var31 >= var48) {
                           break;
                        }
                     }
                  }
               }

               if (var49 > 0) {
                  int var55 = (int)Math.ceil(Math.hypot((double)(var54 - (var2 + var45)), (double)(var25 - (var3 + var43))));
                  if (!a(var0, var2 + var45, var3 + var43, var52 + 1, var55 + 4)) {
                     var39++;
                     var40[Math.min(32, var49)]++;
                     if (var42 < 20 || var49 >= 12 && var42 < 90) {
                        var41.append(
                           String.format(
                              Locale.ROOT,
                              "%n  water %d at (%d,%d) would pour onto water %d at (%d,%d) drop=%d",
                              var48,
                              var2 + var45,
                              var3 + var43,
                              var52,
                              var54,
                              var25,
                              var49
                           )
                        );
                        var42++;
                     }
                  }
               }
            }
         }
      }

      StringBuilder var44 = new StringBuilder();

      for (int var46 = 3; var46 < var40.length; var46++) {
         if (var40[var46] > 0) {
            var44.append(String.format("%n   drop %2d : %d columns", var46, var40[var46]));
         }
      }

      Constants.LOG
         .info(
            "[SR-MONOAUDIT] {}x{} around ({},{}): {} uphill-water columns (drop >= {} onto nearby lower water, no containing ground line between, cascades exempt).{}{}{}",
            var7,
            var7,
            var2,
            var3,
            var39,
            3,
            var44,
            var39 > 0 ? "\n worst offenders:" : " CLEAN.",
            var41
         );
   }

   public static void segmentMap(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      StringBuilder var5 = new StringBuilder(32768);
      var5.append(String.format(Locale.ROOT, "%n=== STREAM SEGMENT MAP @ (%d,%d) radius %d, step 2 ===%n", var2, var3, var4));
      var5.append("  digit = channel water surface (last digit) | # = bank/berm corridor | L = lake | . = untouched | @ = here")
         .append(String.format(Locale.ROOT, "%n"));
      HashMap var6 = new HashMap();

      record 1Row() {
         private final double d;
         private final String text;

         _Row/* $VF was: 1Row*/(double var1, String var3) {
            this.d = var1;
            this.text = var3;
         }
      }

      for (int var7 = var3 - var4; var7 <= var3 + var4; var7 += 2) {
         var5.append(String.format(Locale.ROOT, "  z%6d  ", var7));

         for (int var8 = var2 - var4; var8 <= var2 + var4; var8 += 2) {
            char var9;
            if (Math.abs(var8 - var2) <= 1 && Math.abs(var7 - var3) <= 1) {
               var9 = '@';
            } else {
               Lake var10 = var0.lakeAt(var8, var7);
               ColumnPlan var11 = var0.plan(var8, var7);
               if (var10 != null) {
                  var9 = 'L';
               } else if (var11 != null && var11 != ColumnPlan.NONE && var11.inChannel()) {
                  int var12 = var11.waterSurfaceY();
                  var6.merge(var12, 1, Integer::sum);
                  var9 = (char)(48 + Math.floorMod(var12, 10));
               } else if (var11 != null && var11 != ColumnPlan.NONE && var11.inCorridor()) {
                  var9 = '#';
               } else {
                  var9 = '.';
               }
            }

            var5.append(var9);
         }

         var5.append(String.format(Locale.ROOT, "%n"));
      }

      var5.append(String.format(Locale.ROOT, "  x from %d (left) to %d (right)%n", var2 - var4, var2 + var4));
      ArrayList var22 = new ArrayList(var6.keySet());
      Collections.sort(var22);
      var5.append("  channel water levels on the map: ");

      for (int var25 : var22) {
         var5.append(var25).append('(').append(var6.get(var25)).append("col) ");
      }

      var5.append(String.format(Locale.ROOT, "%n%n  segments within %d blocks (nearest first):%n", var4));
      ArrayList var24 = new ArrayList();

      for (PlateKey var31 : var0.platesNear((double)var2, (double)var3, (double)var4)) {
         RiverNetwork var13 = var0.networkFor(var31);
         List var14 = var13.segments();
         int[] var15 = NetworkTracer.chainIds(var14);

         for (int var16 = 0; var16 < var14.size(); var16++) {
            RiverSegment var17 = (RiverSegment)var14.get(var16);
            double var18 = var17.clampParam((double)var2 + 0.5, (double)var3 + 0.5);
            double var20 = var17.distanceAt((double)var2 + 0.5, (double)var3 + 0.5, var18);
            if (var20 <= (double)var4) {
               var24.add(
                  new 1Row(
                     var20,
                     String.format(
                        Locale.ROOT,
                        "  d%5.1f | %s chain#%d seg %d | (%.0f,%.0f) y%.1f w%.1f -> (%.0f,%.0f) y%.1f w%.1f",
                        var20,
                        var31,
                        var15[var16],
                        var16,
                        var17.x1(),
                        var17.z1(),
                        var17.y1(),
                        var17.w1(),
                        var17.x2(),
                        var17.z2(),
                        var17.y2(),
                        var17.w2()
                     )
                  )
               );
            }
         }
      }

      var24.sort(Comparator.comparingDouble(1Row::d));
      int var27 = Math.min(var24.size(), 80);

      for (int var29 = 0; var29 < var27; var29++) {
         var5.append(((1Row)var24.get(var29)).text()).append(String.format(Locale.ROOT, "%n"));
      }

      if (var24.size() > var27) {
         var5.append("  (+").append(var24.size() - var27).append(" more)").append(String.format(Locale.ROOT, "%n"));
      }

      Constants.LOG.info(var5.toString());
   }

   public static void superTrace(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      ChunkGenerator var4 = var1.getChunkSource().getGenerator();
      RandomState var5 = var1.getChunkSource().randomState();
      RiverNetwork var6 = var0.networkAt(var2, var3);
      RiverSample var7 = var6.sample((double)var2 + 0.5, (double)var3 + 0.5, 64.0);
      if (var7 != null && var7.segId() >= 0) {
         List var8 = var6.segments();
         int[] var9 = NetworkTracer.chainIds(var8);
         int var10 = var9[var7.segId()];
         HashMap var11 = new HashMap();
         HashMap var12 = new HashMap();
         HashMap var13 = new HashMap();

         for (int var14 = 0; var14 < var8.size(); var14++) {
            RiverSegment var15 = (RiverSegment)var8.get(var14);
            var11.computeIfAbsent(a(var15.x2(), var15.z2()), var0x -> new ArrayList<>()).add(var14);
            var13.put(a(var15.x1(), var15.z1()), var14);
            if (var9[var14] == var10) {
               var12.put(a(var15.x1(), var15.z1()), var14);
            }
         }

         int var47 = -1;

         for (Entry var16 : var12.entrySet()) {
            List var17 = (List)var11.get(var16.getKey());
            boolean var18 = false;
            if (var17 != null) {
               for (int var20 : var17) {
                  if (var9[var20] == var10) {
                     var18 = true;
                     break;
                  }
               }
            }

            if (!var18) {
               var47 = (Integer)var16.getValue();
               break;
            }
         }

         if (var47 < 0) {
            var47 = var7.segId();
         }

         StringBuilder var49 = new StringBuilder();
         StringBuilder var50 = new StringBuilder();
         var49.append(String.format(Locale.ROOT, "%n=== STREAM SUPER TRACE @ (%d,%d): chain #%d, plate %s ===%n", var2, var3, var10, var6.key()));
         var49.append("  cols: pos | preGround | rawGrade | surface(plan) | placedWater | placedWall | lake | halfW | flags\n");
         int var51 = var47;
         int var52 = 0;
         double var53 = 0.0;
         int var21 = Integer.MIN_VALUE;
         double var22 = Double.NaN;
         int var24 = 0;
         int var25 = var10;
         RiverSegment var26 = null;
         HashSet var27 = new HashSet();

         for (int var28 = 0; var28 < 100000 && var51 >= 0 && var27.add(var51); var28++) {
            RiverSegment var29 = (RiverSegment)var8.get(var51);
            var53 += Math.hypot(var29.x2() - var29.x1(), var29.z2() - var29.z1());
            List var30 = (List)var11.get(a(var29.x1(), var29.z1()));
            if (var30 != null) {
               for (int var32 : var30) {
                  if (var9[var32] != var9[var51]) {
                     RiverSegment var33 = (RiverSegment)var8.get(var32);
                     int var34 = a(var0, var29.y1(), var29.w1());
                     int var35 = a(var0, var33.y2(), var33.w2());
                     int var36 = a(var0, var29.y2(), var29.w2());
                     StringBuilder var37 = new StringBuilder();
                     int var38 = var32;
                     double var39 = 0.0;

                     for (int var41 = 0; var41 < 400 && var39 < 16.0; var41++) {
                        RiverSegment var42 = (RiverSegment)var8.get(var38);
                        var39 += Math.hypot(var42.x2() - var42.x1(), var42.z2() - var42.z1());
                        if (var41 % 8 == 0) {
                           var37.insert(0, a(var0, var42.y1(), var42.w1()) + " > ");
                        }

                        List var43 = (List)var11.get(a(var42.x1(), var42.z1()));
                        int var44 = -1;
                        if (var43 != null) {
                           for (int var46 : var43) {
                              if (var9[var46] == var9[var32]) {
                                 var44 = var46;
                                 break;
                              }
                           }
                        }

                        if (var44 < 0) {
                           break;
                        }

                        var38 = var44;
                     }

                     var50.append(
                        String.format(
                           Locale.ROOT,
                           "%n  JUNCTION at (%d,%d): inflow chain #%d [%s%d] (grade %.2f w %.1f) joins chain #%d at %d (grade %.2f w %.1f) -> downstream %d%s",
                           (int)var29.x1(),
                           (int)var29.z1(),
                           var9[var32],
                           var37,
                           var35,
                           var33.y2(),
                           var33.w2(),
                           var9[var51],
                           var34,
                           var29.y1(),
                           var29.w1(),
                           var36,
                           var36 > Math.min(var35, var34) ? "  << RISE (downstream above an inflow!)" : ""
                        )
                     );
                  }
               }
            }

            if (var53 >= (double)var52 * 4.0) {
               int var57 = (int)Math.floor(var29.x1());
               int var60 = (int)Math.floor(var29.z1());
               int var62 = var4.getBaseHeight(var57, var60, Types.OCEAN_FLOOR_WG, var1, var5) - 1;
               ColumnPlan var64 = var0.plan(var57, var60);
               int var65 = var64.inCorridor() ? var64.waterSurfaceY() : a(var0, var29.y1(), var29.w1());
               String var66 = "-";
               String var67 = "-";
               if (var1.hasChunk(var57 >> 4, var60 >> 4)) {
                  int var68 = var1.getHeight(Types.OCEAN_FLOOR, var57, var60) - 1;
                  var67 = String.valueOf(var68);
                  int var70 = -9999;

                  for (int var40 = Math.max(var68, var65) + 4; var40 >= var65 - 12; var40--) {
                     if (!var1.getBlockState(new BlockPos(var57, var40, var60)).getFluidState().isEmpty()) {
                        var70 = var40;
                        break;
                     }
                  }

                  var66 = var70 == -9999 ? "-" : String.valueOf(var70);
               }

               Lake var69 = var0.lakeAt(var57, var60);
               int var71 = RiverCarverFeature.nearbyLakeLevel(var0, var57, var60, var1.getMinBuildHeight(), var1.getMaxBuildHeight());
               boolean var72 = !Double.isNaN(var22) && var29.y1() > var22 + 0.01;
               boolean var73 = var21 != Integer.MIN_VALUE && var65 > var21;
               if (var72 || var73) {
                  var24++;
               }

               var49.append(
                  String.format(
                     Locale.ROOT,
                     "  (%6d,%6d) | g %3d | raw %7.2f | surf %3d | water %4s | wall %4s | %s%s | w %4.1f |%s%s%s%n",
                     var57,
                     var60,
                     var62,
                     var29.y1(),
                     var65,
                     var66,
                     var67,
                     var69 != null ? "L" + var69.level() : "  - ",
                     var69 == null && var71 != Integer.MIN_VALUE ? "~L" + var71 : "",
                     var29.w1(),
                     var64.terminal() ? " TERM" : "",
                     var64.source() ? " SRC" : "",
                     !var72 && !var73 ? "" : "  << RISE " + (var72 ? "raw " + var22 + "->" + var29.y1() : "") + (var73 ? "surf " + var21 + "->" + var65 : "")
                  )
               );
               var21 = var65;
               var22 = var29.y1();
               var52++;
            }

            var26 = var29;
            Integer var58 = (Integer)var13.get(a(var29.x2(), var29.z2()));
            if (var58 != null && var58 != var51 && var9[var58] != var25) {
               var49.append(String.format(Locale.ROOT, "  -- continues as chain #%d --%n", var9[var58]));
               var25 = var9[var58];
            }

            var51 = var58 != null && var58 != var51 ? var58 : -1;
         }

         if (var26 != null) {
            int var54 = (int)Math.floor(var26.x2());
            int var55 = (int)Math.floor(var26.z2());
            Lake var56 = var0.lakeAt(var54, var55);
            int var59 = a(var0, var26.y2(), var26.w2());
            String var61;
            if (var56 != null) {
               var61 = "ends IN lake (level "
                  + var56.level()
                  + "): our surface "
                  + var59
                  + (var59 >= var56.level() ? " -> FLUSH/above (ok)" : " -> BELOW LAKE by " + (var56.level() - var59) + " << PROBLEM");
            } else if (var26.y2() <= (double)var0.config().seaLevel()) {
               var61 = "ends at the SEA (grade " + String.format(Locale.ROOT, "%.1f", var26.y2()) + ")";
            } else {
               int var63 = var4.getBaseHeight(var54, var55, Types.OCEAN_FLOOR_WG, var1, var5) - 1;
               var61 = "DRY end at grade " + String.format(Locale.ROOT, "%.1f", var26.y2()) + " over ground " + var63 + " (truncated/terminal pool)";
            }

            var49.append(String.format(Locale.ROOT, "  END at (%d,%d): %s%n", var54, var55, var61));
         }

         var49.append(
            String.format(
               Locale.ROOT,
               "  walked ~%.0f blocks to the terminus; %d downstream RISE row(s)%s; junctions:%s%n",
               var53,
               var24,
               var24 > 0 ? " << NOT MONOTONE" : " (monotone)",
               var50.length() == 0 ? " none" : var50.toString()
            )
         );
         var49.append("  (per-pass modifier history: rebuild fresh with -Dstreamsreflowing.segdump=x,z,r -- a store-loaded network no longer carries it)");
         Constants.LOG.info(var49.toString());
      } else {
         Constants.LOG.info("=== STREAM SUPER TRACE: no stream within 64 blocks of ({},{}) in this plate ===", var2, var3);
      }
   }

   private static int a(RiverEngine var0, double var1, double var3) {
      double var5 = 0.5 + 0.5 * Math.min(1.0, var3 / 3.0);
      return Math.max(var0.config().seaLevel() - 1, (int)Math.floor(var1) - (int)Math.round(var0.config().ditchInset() * var5));
   }

   private static long a(double var0, double var2) {
      long var4 = Math.round(var0 * 16.0) & 4294967295L;
      long var6 = Math.round(var2 * 16.0) & 4294967295L;
      return var4 << 32 | var6;
   }

   private static boolean a(RiverEngine var0, int var1, int var2, int var3, int var4) {
      double var5 = (double)var1 + 0.5;
      double var7 = (double)var2 + 0.5;

      for (int var9 = 0; var9 < var4 / 2; var9++) {
         ColumnPlan var10 = var0.plan((int)Math.floor(var5), (int)Math.floor(var7));
         if (!var10.inCorridor()) {
            return false;
         }

         if (var10.waterSurfaceY() <= var3) {
            return true;
         }

         double var11 = Math.hypot(var10.flowDirX(), var10.flowDirZ());
         if (var11 < 1.0E-6) {
            return false;
         }

         var5 += var10.flowDirX() / var11 * 2.0;
         var7 += var10.flowDirZ() / var11 * 2.0;
      }

      return false;
   }

   public static void spoutScan(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      ChunkGenerator var5 = var1.getChunkSource().getGenerator();
      RandomState var6 = var1.getChunkSource().randomState();
      int var7 = var4 * 2 + 1;
      int[] var8 = new int[var7 * var7];
      boolean[] var9 = new boolean[var7 * var7];
      PlateKey var10 = var0.plateAt(var2, var3);
      byte var11 = 32;

      for (int var12 = -var4; var12 <= var4; var12++) {
         for (int var13 = -var4; var13 <= var4; var13++) {
            int var14 = var2 + var13;
            int var15 = var3 + var12;
            if (var0.plateAt(var14, var15).equals(var10)
               && var0.plateAt(var14 - var11, var15).equals(var10)
               && var0.plateAt(var14 + var11, var15).equals(var10)
               && var0.plateAt(var14, var15 - var11).equals(var10)
               && var0.plateAt(var14, var15 + var11).equals(var10)
               && var0.plateAt(var14 - var11, var15 - var11).equals(var10)
               && var0.plateAt(var14 + var11, var15 + var11).equals(var10)
               && var0.plateAt(var14 - var11, var15 + var11).equals(var10)
               && var0.plateAt(var14 + var11, var15 - var11).equals(var10)) {
               int var16 = (var12 + var4) * var7 + var13 + var4;
               int var17 = Integer.MIN_VALUE;
               ColumnPlan var18 = var0.plan(var14, var15);
               if (var18.inCorridor()) {
                  var17 = var18.waterSurfaceY();
               }

               int var19 = var0.lakeSurfaceAt(var14, var15);
               if (var19 != Integer.MIN_VALUE && var19 > var17) {
                  var17 = var19;
               }

               if (var17 != Integer.MIN_VALUE) {
                  var9[var16] = true;
                  var8[var16] = var17;
               }
            }
         }
      }

      byte var37 = 6;
      byte var38 = 1;
      byte var39 = 4;
      int var40 = 0;
      int var41 = 0;
      int var42 = 0;
      int var43 = 0;
      int var44 = 0;
      int var20 = 0;
      int[] var21 = new int[48];
      StringBuilder var22 = new StringBuilder();
      int var23 = 0;

      for (int var24 = 0; var24 < var7; var24++) {
         for (int var25 = 0; var25 < var7; var25++) {
            int var26 = var24 * var7 + var25;
            if (var9[var26]) {
               int var27 = var8[var26];
               int var28 = Integer.MAX_VALUE;
               int var29 = 0;
               int var30 = 0;

               for (int var31 = -var37; var31 <= var37; var31++) {
                  for (int var32 = -var37; var32 <= var37; var32++) {
                     if (var31 != 0 || var32 != 0) {
                        int var33 = var24 + var31;
                        int var34 = var25 + var32;
                        if (var33 >= 0 && var34 >= 0 && var33 < var7 && var34 < var7) {
                           int var35 = var33 * var7 + var34;
                           if (var9[var35]) {
                              var30++;
                              int var36 = var8[var35];
                              if (var36 < var28) {
                                 var28 = var36;
                              }

                              if (var36 >= var27) {
                                 var29++;
                              }
                           }
                        }
                     }
                  }
               }

               if (var30 >= 8 && var29 <= var30 / 10 && var27 - var28 >= var38) {
                  int var47 = 0;

                  for (int var48 = -var37; var48 <= var37; var48++) {
                     for (int var50 = -var37; var50 <= var37; var50++) {
                        int var52 = var24 + var48;
                        int var54 = var25 + var50;
                        if (var52 >= 0 && var54 >= 0 && var52 < var7 && var54 < var7) {
                           int var56 = var52 * var7 + var54;
                           if (var9[var56] && var8[var56] <= var28 + var39) {
                              var47++;
                           }
                        }
                     }
                  }

                  if (var47 * 10 < var30 * 6) {
                     var41++;
                  } else {
                     int var49 = var2 + (var25 - var4);
                     int var51 = var3 + (var24 - var4);
                     int var53 = var5.getBaseHeight(var49, var51, Types.OCEAN_FLOOR_WG, var1, var6) - 1;
                     if (var27 <= var53) {
                        var42++;
                     } else {
                        var40++;
                        int var55 = var27 - var28;
                        var21[Math.min(var21.length - 1, var55)]++;
                        if (var55 > var43) {
                           var43 = var55;
                           var44 = var2 + (var25 - var4);
                           var20 = var3 + (var24 - var4);
                        }

                        if (var23 < 24) {
                           var22.append(" (")
                              .append(var2 + (var25 - var4))
                              .append(',')
                              .append(var3 + (var24 - var4))
                              .append(")+")
                              .append(var55)
                              .append("@y")
                              .append(var27);
                           var23++;
                        }
                     }
                  }
               }
            }
         }
      }

      Constants.LOG
         .info(
            "[SR-SPOUTSCAN] plate {} (whole-plate sweep from {},{}): {} REAL perched-spout columns (water poking >= {} above a surrounding flat water body AND above natural ground) -- ignored {} cascades + {} buried (planned surface below ground)",
            var10,
            var2,
            var3,
            var40,
            Integer.valueOf(var38),
            var41,
            var42
         );
      if (var40 == 0) {
         Constants.LOG.info("[SR-SPOUTSCAN] CLEAN: no water poking above a surrounding water body in the region.");
      } else {
         Constants.LOG.info("[SR-SPOUTSCAN] worst: +{} at ({},{})", var43, var44, var20);
         StringBuilder var45 = new StringBuilder("[SR-SPOUTSCAN] histogram (height:columns):");

         for (int var46 = var38; var46 < var21.length; var46++) {
            if (var21[var46] > 0) {
               var45.append(" +").append(var46).append(":").append(var21[var46]);
            }
         }

         Constants.LOG.info(var45.toString());
         Constants.LOG.info("[SR-SPOUTSCAN] columns:{}", var22.toString());
      }
   }

   public static void traceColumn(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      int var4 = var2;
      int var5 = var3;
      ColumnPlan var6 = var0.plan(var2, var3);
      if (!var6.inCorridor()) {
         double var7 = Double.MAX_VALUE;

         for (int var9 = -10; var9 <= 10; var9++) {
            for (int var10 = -10; var10 <= 10; var10++) {
               ColumnPlan var11 = var0.plan(var2 + var10, var3 + var9);
               if (var11.inCorridor() && var11.distance() < var7) {
                  var7 = var11.distance();
                  var6 = var11;
                  var4 = var2 + var10;
                  var5 = var3 + var9;
               }
            }
         }
      }

      if (!var6.inCorridor()) {
         Constants.LOG.info("=== STREAM TRACE: no stream corridor within 10 blocks of ({},{}) ===", var2, var3);
      } else {
         int var23 = var0.config().seaLevel();
         int var8 = var6.waterSurfaceY();
         ChunkGenerator var24 = var1.getChunkSource().getGenerator();
         RandomState var25 = var1.getChunkSource().randomState();
         int var26 = var24.getBaseHeight(var4, var5, Types.OCEAN_FLOOR_WG, var1, var25) - 1;
         StringBuilder var12 = new StringBuilder();
         var12.append(String.format("%n=== STREAM TRACE (%d,%d) ===%n", var4, var5));
         var12.append(String.format("plan: rawGradeY=%.2f  waterSurfaceY=%d  ground=%d  perch(water-ground)=%+d%n", var6.gradeY(), var8, var26, var8 - var26));
         var12.append(
            String.format(
               "  sea floor (seaLevel-1) = %d  ->  %s%n",
               var23 - 1,
               var8 <= var23 - 1
                  ? "water is AT/below the sea floor -- the floor MAY be lifting it (check ground vs grade below)"
                  : "water is ABOVE the sea floor -- floor is NOT the cause; the grade itself sits high here"
            )
         );
         var12.append(
            String.format(
               "  distance=%.1f halfWidth=%.1f source=%b terminal=%b inChannel=%b%n",
               var6.distance(),
               var6.width(),
               var6.source(),
               var6.terminal(),
               var6.inChannel()
            )
         );
         int var13 = var6.bedY();
         byte var14 = 24;
         double var15 = var6.distToOutlet();
         boolean var17 = var26 < var8;
         String var18 = "-";
         if (var26 > var13) {
            double var19 = var15 >= 8.988465674311579E307 ? 1.0 : Math.min(1.0, var15 / (double)var14);
            double var21 = var19 * var19 * (3.0 - 2.0 * var19);
            var18 = String.valueOf((int)Math.round((double)var26 + (double)(var13 - var26) * var21));
         }

         var12.append(
            String.format(
               "  MOUTH NOTCH(offshore): deepBed=%d ambientFloor=%d distToOutlet=%s  submergedHere=%b  notchBed=%s%n",
               var13,
               var26,
               var15 >= 8.988465674311579E307 ? "none" : String.format("%.1f", var15),
               var17,
               var18
            )
         );
         var12.append("longitudinal (follow the channel; ground is the floor beneath the centreline):\n");
         List var27 = a(var0, var1, var4, var5, -1);
         Collections.reverse(var27);

         for (String var29 : var27) {
            var12.append(var29).append('\n');
         }

         var12.append(String.format("   >> HERE (%d,%d): rawGrade=%.2f water=%d ground=%d perch=%+d%n", var4, var5, var6.gradeY(), var8, var26, var8 - var26));

         for (String var30 : a(var0, var1, var4, var5, 1)) {
            var12.append(var30).append('\n');
         }

         Constants.LOG.info(var12.toString());
      }
   }

   private static List<String> a(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      ArrayList var5 = new ArrayList();
      ChunkGenerator var6 = var1.getChunkSource().getGenerator();
      RandomState var7 = var1.getChunkSource().randomState();
      double var8 = (double)var2 + 0.5;
      double var10 = (double)var3 + 0.5;

      for (int var12 = 1; var12 <= 30; var12++) {
         ColumnPlan var13 = var0.plan((int)Math.floor(var8), (int)Math.floor(var10));
         double var14 = Math.hypot(var13.flowDirX(), var13.flowDirZ());
         if (!var13.inCorridor() || var14 < 1.0E-6) {
            break;
         }

         var8 += (double)var4 * (var13.flowDirX() / var14) * 2.0;
         var10 += (double)var4 * (var13.flowDirZ() / var14) * 2.0;
         int var16 = (int)Math.floor(var8);
         int var17 = (int)Math.floor(var10);
         ColumnPlan var18 = var0.plan(var16, var17);
         if (!var18.inCorridor()) {
            break;
         }

         int var19 = var6.getBaseHeight(var16, var17, Types.OCEAN_FLOOR_WG, var1, var7) - 1;
         int var20 = var18.waterSurfaceY();
         var5.add(
            String.format(
               "   %s%-2d (%d,%d): rawGrade=%6.2f water=%d ground=%d perch=%+d",
               var4 < 0 ? "up" : "dn",
               var12,
               var16,
               var17,
               var18.gradeY(),
               var20,
               var19,
               var20 - var19
            )
         );
      }

      return var5;
   }

   private static void a(RiverEngine var0, ServerLevel var1, int var2, int var3) {
      int var4 = var2;
      int var5 = var3;
      ColumnPlan var6 = var0.plan(var2, var3);
      if (!var6.inCorridor()) {
         double var7 = Double.MAX_VALUE;

         for (int var9 = -10; var9 <= 10; var9++) {
            for (int var10 = -10; var10 <= 10; var10++) {
               ColumnPlan var11 = var0.plan(var2 + var10, var3 + var9);
               if (var11.inCorridor() && var11.distance() < var7) {
                  var7 = var11.distance();
                  var6 = var11;
                  var4 = var2 + var10;
                  var5 = var3 + var9;
               }
            }
         }
      }

      if (!var6.inCorridor()) {
         Constants.LOG.info("=== PERCH DIAG: no stream corridor within 10 blocks of ({},{}) ===", var2, var3);
      } else {
         int var41 = var6.waterSurfaceY();
         ChunkGenerator var8 = var1.getChunkSource().getGenerator();
         RandomState var42 = var1.getChunkSource().randomState();
         int var43 = var8.getBaseHeight(var4, var5, Types.OCEAN_FLOOR_WG, var1, var42) - 1;
         int var44 = var1.getHeight(Types.OCEAN_FLOOR, var4, var5) - 1;
         int var12 = var1.getHeight(Types.WORLD_SURFACE, var4, var5) - 1;
         double var13 = Math.hypot(var6.flowDirX(), var6.flowDirZ());
         StringBuilder var15 = new StringBuilder();
         String var16 = "n/a";
         String var17 = "n/a";
         if (var13 > 1.0E-6) {
            double var18 = -var6.flowDirZ() / var13;
            double var20 = var6.flowDirX() / var13;

            for (byte var22 = -1; var22 <= 1; var22 += 2) {
               StringBuilder var23 = new StringBuilder();
               int var24 = -1;

               for (int var25 = 1; var25 <= 16; var25++) {
                  int var26 = var4 + (int)Math.round(var18 * (double)var25 * (double)var22);
                  int var27 = var5 + (int)Math.round(var20 * (double)var25 * (double)var22);
                  int var28 = var8.getBaseHeight(var26, var27, Types.OCEAN_FLOOR_WG, var1, var42) - 1;
                  var23.append(String.format("%4d", var28));
                  if (var24 < 0 && var28 >= var41) {
                     var24 = var25;
                  }
               }

               String var54 = var22 < 0 ? "L" : "R";
               var15.append("  ").append(var54).append("(out->):").append((CharSequence)var23).append('\n');
               String var57 = var24 < 0 ? "NEVER (>= grade)" : "rises to grade at d=" + var24;
               if (var22 < 0) {
                  var16 = var57;
               } else {
                  var17 = var57;
               }
            }
         }

         boolean var45 = !"NEVER (>= grade)".equals(var16) && !"NEVER (>= grade)".equals(var17) && var13 > 1.0E-6;
         StringBuilder var19 = new StringBuilder();
         boolean var46 = false;
         boolean var21 = false;
         if (var13 > 1.0E-6) {
            double var47 = -var6.flowDirZ() / var13;
            double var51 = var6.flowDirX() / var13;

            for (byte var58 = -1; var58 <= 1; var58 += 2) {
               StringBuilder var62 = new StringBuilder();

               for (int var65 = 1; var65 <= 12; var65++) {
                  boolean var29 = var0.plan(
                           var4 + (int)Math.round(var47 * (double)var65 * (double)var58), var5 + (int)Math.round(var51 * (double)var65 * (double)var58)
                        )
                        .distance()
                     <= 2.0;
                  var62.append(var29 ? " C" : " .");
                  if (var29) {
                     if (var58 < 0) {
                        var21 = true;
                     } else {
                        var46 = true;
                     }
                  }
               }

               var19.append("  ").append(var58 < 0 ? "L" : "R").append("(out->):").append((CharSequence)var62).append('\n');
            }
         }

         if (var13 > 1.0E-6) {
            double var48 = -var6.flowDirZ() / var13;
            double var52 = var6.flowDirX() / var13;
            StringBuilder var59 = new StringBuilder();
            var59.append("containment walk (d out each side; grade=col waterSurfaceY, g=pre-carve ground, T=terminal[disables cup+berm], ch=inChannel):\n");
            var59.append(
               String.format(
                  "    d= 0 (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%n",
                  var4,
                  var5,
                  var6.waterSurfaceY(),
                  var43,
                  var6.waterSurfaceY() - var43,
                  var6.terminal(),
                  var6.inChannel(),
                  var6.source(),
                  var6.distance()
               )
            );

            for (byte var63 = -1; var63 <= 1; var63 += 2) {
               var59.append("  ").append(var63 < 0 ? "L (rim/mountain side):" : "R (open/sea side):").append('\n');

               for (int var66 = 1; var66 <= 12; var66++) {
                  int var69 = var4 + (int)Math.round(var48 * (double)var66 * (double)var63);
                  int var30 = var5 + (int)Math.round(var52 * (double)var66 * (double)var63);
                  ColumnPlan var31 = var0.plan(var69, var30);
                  int var32 = var8.getBaseHeight(var69, var30, Types.OCEAN_FLOOR_WG, var1, var42) - 1;
                  var59.append(
                     String.format(
                        "    d=%2d (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%s%n",
                        var66,
                        var69,
                        var30,
                        var31.waterSurfaceY(),
                        var32,
                        var31.waterSurfaceY() - var32,
                        var31.terminal(),
                        var31.inChannel(),
                        var31.source(),
                        var31.distance(),
                        var31.inCorridor() ? "" : " [OUT-OF-CORRIDOR]"
                     )
                  );
               }
            }

            Constants.LOG.info(var59.toString());
         }

         Lake var49 = var0.lakeAt(var4, var5);
         int var50 = -1;
         int var53 = Integer.MIN_VALUE;

         for (int var55 = 1; var55 <= 24 && var50 < 0; var55++) {
            for (int var60 = -var55; var60 <= var55 && var50 < 0; var60++) {
               for (int var64 = -var55; var64 <= var55 && var50 < 0; var64++) {
                  if (Math.max(Math.abs(var64), Math.abs(var60)) == var55) {
                     Lake var67 = var0.lakeAt(var4 + var64, var5 + var60);
                     if (var67 != null) {
                        var50 = var55;
                        var53 = var67.level();
                     }
                  }
               }
            }
         }

         String var56 = "covering this column: "
            + (var49 != null ? "YES (level=" + var49.level() + ")" : "none")
            + ";  nearest Lake object within 24 blocks: "
            + (var50 >= 0 ? "d=" + var50 + " level=" + var53 : "NONE (this water is a hollow-pool / pond, not a basin Lake)");
         Constants.LOG
            .info(
               "=== PERCH DIAG column ({},{}) ===\nLAKE object {}\ngrade(waterSurfaceY)={}  rawGradeY={}  bedY={}  GROUND(pre-carve)={}  WALL(post-carve)={}  waterTop(placed)={}\nperch? grade-ground = {}   (>0 = water sits ABOVE ground here)\ninChannel={} distance={} halfWidth={} slope={} upstreamSlope={} terminal={} source={}\nperpendicular ground profile (each col = +1 block out from channel, to 16):\n{}left rim: {}   right rim: {}\n=> hollowEnclosed would be {} (both sides must rise to grade within 16) -> {}\nperpendicular STREAM scan (engine; C=stream centerline within 2 blocks, .=none, out to 12):\n{}this col inAnyChannel={}  stream on L-side={} R-side={}  => bermYieldsToNeighbor(both sides)={}",
               var4,
               var5,
               var56,
               var41,
               String.format("%.1f", var6.gradeY()),
               var6.bedY(),
               var43,
               var44,
               var12,
               var41 - var43,
               var6.inChannel(),
               String.format("%.1f", var6.distance()),
               String.format("%.1f", var6.width()),
               String.format("%.3f", var6.slope()),
               String.format("%.3f", var6.upstreamSlope()),
               var6.terminal(),
               var6.source(),
               var15,
               var16,
               var17,
               var45,
               var45 ? "POOLS to grade" : "falls through to berm/bank carve (stays perched if grade>ground)",
               var19,
               var0.inAnyChannel(var4, var5),
               var21,
               var46,
               var46 && var21
            );
         if (var13 > 1.0E-6) {
            double var61 = -var6.flowDirZ() / var13;
            double var68 = var6.flowDirX() / var13;
            byte var70 = 16;
            int var71 = var41 + 2;
            int var72 = var41 - 34;
            MutableBlockPos var33 = new MutableBlockPos();
            StringBuilder var34 = new StringBuilder();
            var34.append("     ").append(" ".repeat(var70)).append("v  centre (d=0), L=-d out to left, R=+d out to right\n");

            for (int var35 = var71; var35 >= var72; var35--) {
               var34.append(String.format("y%3d ", var35));

               for (int var36 = -var70; var36 <= var70; var36++) {
                  int var37 = var4 + (int)Math.round(var61 * (double)var36);
                  int var38 = var5 + (int)Math.round(var68 * (double)var36);
                  FluidState var39 = var1.getFluidState(var33.set(var37, var35, var38));
                  int var40;
                  if (!var39.isEmpty() && var39.is(FluidTags.WATER)) {
                     var40 = var39.getType() != Fluids.WATER && var39.getType() != Fluids.FLOWING_WATER ? 126 : 111;
                  } else if (var1.getBlockState(var33).isAir()) {
                     var40 = 32;
                  } else {
                     var40 = 35;
                  }

                  var34.append((char)var40);
               }

               var34.append('\n');
            }

            byte var73 = 48;
            StringBuilder var74 = new StringBuilder();

            for (int var75 = -2; var75 <= 2; var75++) {
               for (int var76 = -2; var76 <= 2; var76++) {
                  PlateKey var77 = var0.plateAt(var4 + var76 * var73, var5 + var75 * var73);
                  var74.append(String.format(" %3d,%-3d", var77.cx(), var77.cz()));
               }

               var74.append('\n');
            }

            Constants.LOG
               .info(
                  "cross-section perpendicular to flow (post-carve blocks; ~=stream o=vanilla-water #=solid):\n{}plate (cx,cz) on a {}-block grid, centre row/col = this column (differing ids => a plate seam):\n{}",
                  var34,
                  Integer.valueOf(var73),
                  var74
               );
         }
      }
   }

   private static void a(RiverEngine var0, ServerLevel var1, ChunkPos var2) {
      int var3 = var2.getMinBlockX();
      int var4 = var2.getMinBlockZ();
      ChunkGenerator var5 = var1.getChunkSource().getGenerator();
      RandomState var6 = var1.getChunkSource().randomState();
      StringBuilder var7 = new StringBuilder();
      StringBuilder var8 = new StringBuilder();
      StringBuilder var9 = new StringBuilder();

      for (int var10 = 0; var10 < 16; var10++) {
         for (int var11 = 0; var11 < 16; var11++) {
            int var12 = var3 + var11;
            int var13 = var4 + var10;
            var8.append(String.format("%4d", var5.getBaseHeight(var12, var13, Types.OCEAN_FLOOR_WG, var1, var6) - 1));
            var9.append(String.format("%4d", var1.getHeight(Types.OCEAN_FLOOR, var12, var13) - 1));
            ColumnPlan var14 = var0.plan(var12, var13);
            var7.append(var14.inCorridor() ? String.format("%4d", var14.waterSurfaceY()) : "   .");
         }

         var7.append('\n');
         var8.append('\n');
         var9.append('\n');
      }

      Constants.LOG
         .info(
            "=== PERCH DIAG chunk ({},{}) X[{}..{}] Z[{}..{}] (rows=+Z down, cols=+X right) ===\nGRADE (engine waterSurfaceY; . = not in corridor):\n{}\nGROUND (pre-carve OCEAN_FLOOR_WG, ALL cols):\n{}\nWALL (post-carve OCEAN_FLOOR, ALL cols -- pool/berm shows here):\n{}",
            var2.x,
            var2.z,
            var3,
            var3 + 15,
            var4,
            var4 + 15,
            var7,
            var8,
            var9
         );
   }

   public static boolean startCensus(RiverEngine var0, ServerLevel var1, int var2, int var3, int var4) {
      if (var4 > 0 && S.compareAndSet(false, true)) {
         ChunkGenerator var5 = var1.getChunkSource().getGenerator();
         RandomState var6 = var1.getChunkSource().randomState();
         Thread var7 = new Thread(() -> {
            try {
               a(var0, var5, var6, var1, var2, var3, var4);
            } finally {
               S.set(false);
            }
         }, "streams-perch-census");
         var7.setDaemon(true);
         var7.setPriority(1);
         var7.start();
         Constants.LOG.info("=== PERCH CENSUS started: {}x{} blocks around ({},{}); building networks, please wait ===", var4 * 2, var4 * 2, var2, var3);
         return true;
      } else {
         return false;
      }
   }

   private static void a(RiverEngine var0, ChunkGenerator var1, RandomState var2, ServerLevel var3, int var4, int var5, int var6) {
      try {
         int var7 = var0.config().seaLevel();
         int[] var8 = new int[24];
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;
         int var12 = 0;
         int var13 = Integer.MIN_VALUE;
         int var14 = var4;
         int var15 = var5;
         int var16 = Integer.MAX_VALUE;
         int var17 = Integer.MIN_VALUE;
         int var18 = Integer.MAX_VALUE;
         int var19 = Integer.MIN_VALUE;
         int var20 = 0;
         StringBuilder var21 = new StringBuilder();
         int var22 = 0;

         for (int var23 = -var6; var23 <= var6; var23++) {
            for (int var24 = -var6; var24 <= var6; var24++) {
               int var25 = var4 + var24;
               int var26 = var5 + var23;
               ColumnPlan var27 = var0.plan(var25, var26);
               if (var27.inChannel()) {
                  double var28 = Math.hypot(var27.flowDirX(), var27.flowDirZ());
                  if (!(var28 < 1.0E-6)) {
                     double var30 = -var27.flowDirZ() / var28;
                     double var32 = var27.flowDirX() / var28;
                     double var34 = var27.width() + 2.0;
                     int var36 = var1.getBaseHeight(
                           (int)Math.round((double)var25 + var30 * var34), (int)Math.round((double)var26 + var32 * var34), Types.OCEAN_FLOOR_WG, var3, var2
                        )
                        - 1;
                     int var37 = var1.getBaseHeight(
                           (int)Math.round((double)var25 - var30 * var34), (int)Math.round((double)var26 - var32 * var34), Types.OCEAN_FLOOR_WG, var3, var2
                        )
                        - 1;
                     int var38 = Math.min(var36, var37);
                     if (var38 < var7) {
                        var10++;
                     } else {
                        var9++;
                        int var39 = var27.waterSurfaceY() - var38;
                        if (var39 > var13) {
                           var13 = var39;
                           var14 = var25;
                           var15 = var26;
                        }

                        if (var39 >= 1) {
                           var11++;
                        }

                        if (var39 >= 4) {
                           var12++;
                        }

                        if (var39 >= 13) {
                           var20++;
                           var16 = Math.min(var16, var25);
                           var17 = Math.max(var17, var25);
                           var18 = Math.min(var18, var26);
                           var19 = Math.max(var19, var26);
                           if (var22 < 16 && (var24 + var23 & 7) == 0) {
                              int var40 = var1.getBaseHeight(var25, var26, Types.OCEAN_FLOOR_WG, var3, var2) - 1;
                              var21.append(
                                 String.format(
                                    "    (%d,%d) grade=%d colGround=%d bankL=%d bankR=%d perch=%d%n",
                                    var25,
                                    var26,
                                    var27.waterSurfaceY(),
                                    var40,
                                    var37,
                                    var36,
                                    var39
                                 )
                              );
                              var22++;
                           }
                        }

                        var8[Math.max(0, Math.min(var8.length - 1, var39))]++;
                     }
                  }
               }
            }
         }

         StringBuilder var42 = new StringBuilder();
         var42.append(String.format("%n=== PERCH CENSUS COMPLETE: %dx%d blocks around (%d,%d) ===%n", var6 * 2, var6 * 2, var4, var5));
         var42.append(String.format("inland channel columns scanned: %d (excluded %d coastal/water-meeting)%n", var9, var10));
         if (var9 == 0) {
            var42.append("  (no stream channels in this area -- try a larger radius or a different spawn)\n");
         } else {
            var42.append(String.format("perched (water above lower bank >= 1): %d (%.1f%%)%n", var11, 100.0 * (double)var11 / (double)var9));
            var42.append(String.format("BADLY perched (>= 4, a raised aqueduct): %d (%.1f%%)%n", var12, 100.0 * (double)var12 / (double)var9));
            var42.append(String.format("max perch: %d blocks, at (%d,%d)%n", var13, var14, var15));
            var42.append("histogram (perch height in blocks : column count):\n");

            for (int var43 = 0; var43 < var8.length; var43++) {
               if (var8[var43] > 0) {
                  var42.append(String.format("  %2d%s : %d%n", var43, var43 == var8.length - 1 ? "+" : " ", var8[var43]));
               }
            }

            if (var20 > 0) {
               var42.append(
                  String.format(
                     "badly-perched (>=13) cluster: %d cols in X[%d..%d] Z[%d..%d] (span %dx%d)%n",
                     var20,
                     var16,
                     var17,
                     var18,
                     var19,
                     var17 - var16,
                     var19 - var18
                  )
               );
               var42.append("samples (grade vs column ground vs the two banks):\n");
               var42.append((CharSequence)var21);
            }
         }

         Constants.LOG.info(var42.toString());
      } catch (Throwable var41) {
         Constants.LOG.warn("Perch census failed", var41);
      }
   }

   private static final class PrioritizedTask implements Comparable<StreamPrefetcher.PrioritizedTask>, Runnable {
      static final long a = Long.MIN_VALUE;
      private final int b;
      private final long c;
      private final long d;
      private final Runnable e;

      PrioritizedTask(int var1, long var2, Runnable var4) {
         this(var1, var2, Long.MIN_VALUE, var4);
      }

      PrioritizedTask(int var1, long var2, long var4, Runnable var6) {
         this.b = var1;
         this.c = var2;
         this.d = var4;
         this.e = var6;
      }

      @Override
      public void run() {
         this.e.run();
      }

      public int compareTo(StreamPrefetcher.PrioritizedTask var1) {
         int var2 = Integer.compare(this.b, var1.b);
         return var2 != 0 ? var2 : Long.compare(this.c, var1.c);
      }
   }
}
