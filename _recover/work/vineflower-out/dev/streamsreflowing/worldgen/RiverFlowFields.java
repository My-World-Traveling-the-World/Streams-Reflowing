package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;

public final class RiverFlowFields {
   private static volatile int a = -1;
   private static final int b = 512;
   private static final int c = 8;
   private static final int d = 256;
   private static final ConcurrentHashMap<Long, RiverFlowField> e = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<Long> f = new ConcurrentLinkedQueue<>();
   private static final int g = 1397900849;
   private static final int h = 18;
   private static volatile Path i;
   private static volatile long j;
   private static final Map<ServerLevel, RiverFlowField.BiomeClass> k = new ConcurrentHashMap<>();
   private static final int l = 262144;
   private static final int[] m = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] n = new int[]{0, 1, 1, 1, 0, -1, -1, -1};

   private static int a() {
      int var0 = a;
      if (var0 <= 0) {
         var0 = switch (Services.PLATFORM.riverDrainageSize()) {
            case 2 -> 1024;
            case 3 -> 2048;
            default -> 512;
         };
         a = var0;
      }

      return var0;
   }

   private RiverFlowFields() {
   }

   public static int dirAt(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      RiverFlowField var4 = a(Math.floorDiv(var2, a()), Math.floorDiv(var3, a()));
      return var4 == null ? -1 : var4.dirAt(var2, var3);
   }

   public static double flowRadAt(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      RiverFlowField var4 = a(Math.floorDiv(var2, a()), Math.floorDiv(var3, a()));
      return var4 == null ? Double.NaN : var4.flowRadAt(var2, var3);
   }

   private static RiverFlowField a(int var0, int var1) {
      return e.get((long)var0 << 32 ^ (long)var1 & 4294967295L);
   }

   public static void initStore(Path var0, long var1) {
      i = var0;
      j = var1;
   }

   private static RiverFlowField b(int var0, int var1) {
      Path var2 = i;
      if (var2 == null) {
         return null;
      } else {
         Path var3 = var2.resolve("f." + var0 + "." + var1 + ".srf");
         if (!Files.isRegularFile(var3)) {
            return null;
         } else {
            try {
               RiverFlowField var5;
               try (DataInputStream var4 = new DataInputStream(new BufferedInputStream(Files.newInputStream(var3)))) {
                  if (var4.readInt() != 1397900849 || var4.readInt() != 18 || var4.readLong() != j || var4.readInt() != a()) {
                     return null;
                  }

                  var5 = RiverFlowField.readFrom(var4);
               }

               return var5;
            } catch (RuntimeException | IOException var9) {
               return null;
            }
         }
      }
   }

   private static void a(int var0, int var1, RiverFlowField var2) {
      Path var3 = i;
      if (var3 != null) {
         try {
            Files.createDirectories(var3);
            Path var4 = var3.resolve("f." + var0 + "." + var1 + ".srf");
            Path var5 = var3.resolve("f." + var0 + "." + var1 + ".srf.tmp");

            try (DataOutputStream var6 = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(var5)))) {
               var6.writeInt(1397900849);
               var6.writeInt(18);
               var6.writeLong(j);
               var6.writeInt(a());
               var2.writeTo(var6);
            }

            Files.move(var5, var4, StandardCopyOption.REPLACE_EXISTING);
         } catch (RuntimeException | IOException var11) {
            Constants.LOG.debug("Flow-field save failed for region ({}, {}): {}", var0, var1, var11.toString());
         }
      }
   }

   private static RiverFlowField a(int var0, int var1, RiverFlowField.BiomeClass var2, RiverFlowField.OrientationField var3) {
      long var4 = (long)var0 << 32 ^ (long)var1 & 4294967295L;
      RiverFlowField var6 = e.computeIfAbsent(var4, var4x -> {
         RiverFlowField var5 = b(var0, var1);
         RiverFlowField var6x = var5 != null ? var5 : b(var0, var1, var2, var3);
         if (var5 == null) {
            a(var0, var1, var6x);
         }

         f.add(var4x);
         return var6x;
      });

      while (e.size() > 256) {
         Long var7 = f.poll();
         if (var7 == null) {
            break;
         }

         e.remove(var7);
      }

      return var6;
   }

   private static RiverFlowField b(int var0, int var1, RiverFlowField.BiomeClass var2, RiverFlowField.OrientationField var3) {
      long var4 = System.nanoTime();
      byte var6 = 2;
      RiverFlowField var7 = RiverFlowField.build(var0 * a(), var1 * a(), a(), a(), 512, 8, var6, var2, var3);
      double var8 = (double)(System.nanoTime() - var4) / 1000000.0;
      FlowTrace.fieldBuilds.incrementAndGet();
      FlowTrace.fieldBuildMs.addAndGet((long)var8);
      StreamFlowGrid.invalidateArea(var0 * a(), var1 * a(), a());
      int var10 = RiverFlowField.lastSampled;
      int var11 = RiverFlowField.lastCells;
      String var12 = "Built river-flow field ({}, {}): {} ocean, {} river, {} flowed cells (spacing {}), {} of {} cells classified ({}%), {} ms";
      Object[] var13 = new Object[]{
         var0, var1, var7.oceanCells, var7.riverCells, var7.flowedCells, 8, var10, var11, var11 == 0 ? 0 : 100 * var10 / var11, String.format("%.1f", var8)
      };
      if (Boolean.getBoolean("streamsreflowing.caldiag")) {
         Constants.LOG.info(var12, var13);
      } else {
         Constants.LOG.debug(var12, var13);
      }

      return var7;
   }

   public static RiverFlowField.BiomeClass classifierFor(ServerLevel var0) {
      return k.computeIfAbsent(var0, RiverFlowFields::a);
   }

   private static RiverFlowField.BiomeClass a(ServerLevel var0) {
      ChunkGenerator var1 = var0.getChunkSource().getGenerator();
      RandomState var2 = var0.getChunkSource().randomState();
      BiomeSource var3 = var1.getBiomeSource();
      Sampler var4 = var2.sampler();
      int var5 = QuartPos.fromBlock(var1.getSeaLevel());
      ConcurrentHashMap var6 = new ConcurrentHashMap();
      AtomicInteger var7 = new AtomicInteger();
      byte var8 = 13;
      short var9 = 8191;
      ThreadLocal var10 = ThreadLocal.withInitial(() -> {
         long[] var0x = new long[8192];
         Arrays.fill(var0x, Long.MIN_VALUE);
         return var0x;
      });
      ThreadLocal var11 = ThreadLocal.withInitial(() -> new byte[8192]);
      return (var7x, var8x) -> {
         int var9x = var7x >> 2;
         int var10x = var8x >> 2;
         long var11x = (long)var9x << 32 ^ (long)var10x & 4294967295L;
         long[] var13 = (long[])var10.get();
         byte[] var14 = (byte[])var11.get();
         int var15 = (int)(var11x ^ var11x >>> 32) & 8191;
         if (var13[var15] == var11x) {
            return var14[var15];
         } else {
            Byte var16 = (Byte)var6.get(var11x);
            if (var16 != null) {
               var14[var15] = var16;
               var13[var15] = var11x;
               return var16;
            } else {
               Holder var17 = var3.getNoiseBiome(var9x, var5, var10x, var4);
               int var18 = var17.is(BiomeTags.IS_OCEAN)
                  ? 2
                  : (!var17.is(BiomeTags.IS_RIVER) && !var17.is(Biomes.RIVER) && !var17.is(Biomes.FROZEN_RIVER) ? 0 : 1);
               var14[var15] = (byte)var18;
               var13[var15] = var11x;
               if (var6.putIfAbsent(var11x, Byte.valueOf((byte)var18)) == null && (var7.incrementAndGet() & 4095) == 0 && var6.size() > 262144) {
                  var6.clear();
               }

               return var18;
            }
         }
      };
   }

   public static void unloadLevel(ServerLevel var0) {
      k.remove(var0);
   }

   public static RiverFlowField.OrientationField orientationFor(ServerLevel var0) {
      ChunkGenerator var1 = var0.getChunkSource().getGenerator();
      if (!(var1 instanceof NoiseBasedChunkGenerator)) {
         return null;
      } else {
         DensityFunction var2;
         try {
            var2 = var0.getChunkSource().randomState().router().continents();
         } catch (Throwable var4) {
            return null;
         }

         int var3 = var1.getSeaLevel();
         return (var2x, var3x) -> var2.compute(new SinglePointContext(var2x, var3, var3x));
      }
   }

   public static int regionSize() {
      return a();
   }

   public static boolean isRegionReady(int var0, int var1) {
      long var2 = (long)Math.floorDiv(var0, a()) << 32 ^ (long)Math.floorDiv(var1, a()) & 4294967295L;
      return e.containsKey(var2);
   }

   public static void ensureRegion(RiverFlowField.BiomeClass var0, RiverFlowField.OrientationField var1, int var2, int var3) {
      a(Math.floorDiv(var2, a()), Math.floorDiv(var3, a()), var0, var1);
   }

   public static long owningRegionKey(int var0, int var1) {
      return (long)Math.floorDiv(var0, a()) << 32 ^ (long)Math.floorDiv(var1, a()) & 4294967295L;
   }

   public static void flowMap(int var0, int var1, int var2) {
      int var3 = Math.max(4, Math.min(48, var2));
      RiverFlowField var4 = a(Math.floorDiv(var0, a()), Math.floorDiv(var1, a()));
      if (var4 == null) {
         Constants.LOG.info("[SR-FLOWMAP] no river-current field built yet around ({},{}) -- it builds in the background; try again in a moment.", var0, var1);
      } else {
         Constants.LOG.info("[SR-FLOWMAP] river currents around ({},{}), one char per {} blocks, region size {}", var0, var1, var4.spacing(), a());
         Constants.LOG
            .info(
               "[SR-FLOWMAP] E=east e=SE S=south s=SW W=west w=NW N=north n=NE  '.'=no current  UPPERCASE-X marks a cell a neighbour flows straight back into (a split/collision)"
            );
         String var5 = "EeSsWwNn";

         for (int var6 = -var3; var6 <= var3; var6++) {
            StringBuilder var7 = new StringBuilder();

            for (int var8 = -var3; var8 <= var3; var8++) {
               int var9 = var0 + var8 * var4.spacing();
               int var10 = var1 + var6 * var4.spacing();
               int var11 = var4.dirAt(var9, var10);
               if (var11 < 0) {
                  var7.append('.');
               } else {
                  boolean var12 = false;

                  for (int var13 = 0; var13 < 8 && !var12; var13++) {
                     int var14 = var4.dirAt(var9 + m[var13] * var4.spacing(), var10 + n[var13] * var4.spacing());
                     if (var14 >= 0) {
                        int var15 = var14 - var11 & 7;
                        var12 = var15 >= 3 && var15 <= 5;
                     }
                  }

                  var7.append(var12 ? '#' : var5.charAt(var11));
               }
            }

            Constants.LOG.info("[SR-FLOWMAP] {}", var7);
         }
      }
   }

   public static void clear() {
      e.clear();
      f.clear();
      k.clear();
      i = null;
   }
}
