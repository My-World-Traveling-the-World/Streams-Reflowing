package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.Util;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;

public final class ChunkStepDefer {
   private static final boolean a = Boolean.getBoolean("streamsreflowing.nochunkdefer");
   private static final boolean b = Boolean.getBoolean("streamsreflowing.caldiag");
   private static final ConcurrentHashMap<Long, Object[]> c = new ConcurrentHashMap<>();
   private static final AtomicBoolean d = new AtomicBoolean();
   private static volatile Boolean e;
   private static final double f = 48.0;

   private ChunkStepDefer() {
   }

   private static void a(ChunkPos var0, double var1, double var3, RiverEngine var5) {
      ArrayList var6 = new ArrayList(2);

      for (PlateKey var10 : var5.platesNear(var1, var3, 48.0)) {
         if (!var5.isPlateReady(var10)) {
            var6.add(var10);
         }
      }

      c.put((long)var0.x << 32 ^ (long)var0.z & 4294967295L, new Object[]{System.nanoTime(), var5, var6});
      if (d.compareAndSet(false, true)) {
         a();
      }
   }

   private static void a() {
      Thread var0 = new Thread(
         () -> {
            while (true) {
               try {
                  Thread.sleep(10000L);
                  long var0x = System.nanoTime();

                  for (Entry var3 : c.entrySet()) {
                     Object[] var4 = (Object[])var3.getValue();
                     long var5 = (var0x - (Long)var4[0]) / 1000000L;
                     if (var5 >= 15000L) {
                        RiverEngine var7 = (RiverEngine)var4[1];
                        List var8 = (List)var4[2];
                        StringBuilder var9 = new StringBuilder();

                        for (Object var11 : var8) {
                           PlateKey var12 = (PlateKey)var11;
                           var9.append(" [")
                              .append(var12)
                              .append(" ready=")
                              .append(var7.isPlateReady(var12))
                              .append(' ')
                              .append(StreamPrefetcher.plateDiag(var7, var12))
                              .append(']');
                        }

                        Constants.LOG
                           .warn(
                              "[SR-WEDGE] chunk ({},{}) deferred {} ms and NOT resumed; plates:{} | {}",
                              (int)((Long)var3.getKey() >> 32),
                              ((Long)var3.getKey()).intValue(),
                              var5,
                              var9,
                              StreamPrefetcher.laneDiag()
                           );
                     }
                  }
               } catch (InterruptedException var13) {
                  return;
               } catch (Throwable var14) {
                  Constants.LOG.warn("[SR-WEDGE] watchdog error", var14);
               }
            }
         },
         "streamsreflowing-defer-watchdog"
      );
      var0.setDaemon(true);
      var0.setPriority(1);
      var0.start();
   }

   private static boolean b() {
      if (!a && Services.PLATFORM.fastChunkLoading()) {
         Boolean var0 = e;
         if (var0 == null) {
            var0 = Services.PLATFORM.isModLoaded("c2me");
            e = var0;
         }

         return !var0;
      } else {
         return true;
      }
   }

   public static CompletableFuture<ChunkAccess> defer(ChunkStep var0, WorldGenContext var1, StaticCache2D<GenerationChunkHolder> var2, ChunkAccess var3) {
      if (b()) {
         return null;
      } else if (StreamPrefetcher.isServerStopping()) {
         return null;
      } else {
         ChunkStatus var4 = var0.targetStatus();
         if (var4 != ChunkStatus.CARVERS && var4 != ChunkStatus.FEATURES) {
            return null;
         } else if (!Services.PLATFORM.generateRivers()) {
            return null;
         } else {
            ServerLevel var5 = var1.level();
            if (!var5.dimension().equals(Level.OVERWORLD)) {
               return null;
            } else if (var3.getPersistedStatus().isOrAfter(var4)) {
               return null;
            } else {
               RiverEngine var6 = StreamsWorldgen.engine(var5);
               if (var6 == null) {
                  return null;
               } else {
                  ChunkPos var7 = var3.getPos();
                  double var8 = (double)var7.getMinBlockX() + 8.0;
                  double var10 = (double)var7.getMinBlockZ() + 8.0;
                  if (var6.platesReadyNear(var8, var10, 48.0)) {
                     return null;
                  } else {
                     FlowTrace.deferred.incrementAndGet();
                     if (b) {
                        a(var7, var8, var10, var6);
                     }

                     long var12 = System.nanoTime();
                     int var14 = var7.x;
                     int var15 = var7.z;
                     return var6.whenPlatesReadyNear(var8, var10, 48.0)
                        .thenComposeAsync(
                           var9 -> {
                              FlowTrace.resumed.incrementAndGet();
                              if (b) {
                                 c.remove((long)var14 << 32 ^ (long)var15 & 4294967295L);
                              }

                              if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                                 long var10x = (System.nanoTime() - var12) / 1000000L;
                                 if (var10x >= 50L) {
                                    Constants.LOG
                                       .info(
                                          "[SR-DEFER] chunk ({},{}) {} waited {} ms for plates",
                                          var14,
                                          var15,
                                          var4 == ChunkStatus.FEATURES ? "FEATURES" : "CARVERS",
                                          var10x
                                       );
                                 }
                              }

                              return var0.apply(var1, var2, var3);
                           },
                           Util.backgroundExecutor()
                        );
                  }
               }
            }
         }
      }
   }
}
