package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FieldFormBuilder;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import dev.streamsreflowing.worldgen.regionprep.HeightAccuracy;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;

public final class StreamsWorldgen {
   public static final TagKey<EntityType<?>> IGNORES_CURRENT = TagKey.create(
      Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "ignores_current")
   );
   public static final ResourceLocation RIVER_CARVER_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_carver");
   public static final Feature<NoneFeatureConfiguration> RIVER_CARVER = new RiverCarverFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river")
   );
   public static final ResourceKey<PlacedFeature> RIVER_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river")
   );
   public static final ResourceLocation STREAM_VEGETATION_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_vegetation");
   public static final Feature<NoneFeatureConfiguration> STREAM_VEGETATION = new StreamVegetationFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> STREAM_VEGETATION_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_vegetation")
   );
   public static final ResourceKey<PlacedFeature> STREAM_VEGETATION_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_vegetation")
   );
   public static final TagKey<PlacedFeature> BIOME_BOOST_EXCLUDED = TagKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "biome_boost_excluded")
   );
   public static final TagKey<Biome> NO_BANK_GRASS = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "no_bank_grass"));
   public static final TagKey<Block> UNDERWATER_VEGETATION_FLOOR = TagKey.create(
      Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "underwater_vegetation_floor")
   );
   public static final ResourceLocation SNOW_CLEANUP_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "snow_cleanup");
   public static final Feature<NoneFeatureConfiguration> SNOW_CLEANUP = new StreamSnowCleanupFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> SNOW_CLEANUP_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "snow_cleanup")
   );
   public static final ResourceKey<PlacedFeature> SNOW_CLEANUP_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "snow_cleanup")
   );
   public static final ResourceLocation RIVER_FLOW_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_flow");
   public static final Feature<NoneFeatureConfiguration> RIVER_FLOW = new StreamRiverFlowFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_FLOW_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_flow")
   );
   public static final ResourceKey<PlacedFeature> RIVER_FLOW_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_flow")
   );
   public static final ResourceLocation WATER_SKIN_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "water_skin");
   public static final Feature<NoneFeatureConfiguration> WATER_SKIN = new WaterSkinFeature(NoneFeatureConfiguration.CODEC);
   public static final ResourceKey<ConfiguredFeature<?, ?>> WATER_SKIN_CONFIGURED = ResourceKey.create(
      Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "water_skin")
   );
   public static final ResourceKey<PlacedFeature> WATER_SKIN_PLACED = ResourceKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "water_skin")
   );
   private static final Map<ServerLevel, RiverEngine> a = Collections.synchronizedMap(new WeakHashMap<>());
   private static final double b = 6.0;
   private static final int c = 65536;
   private static volatile RiverEngine d;
   private static final double e = 0.390625;
   private static final int f = 200;
   private static final int g = 800;
   private static final int h = 768;
   private static final int i = 2;
   private static final int j = 80;
   private static final int k = 16;
   private static final int l = 6;
   private static final String m = "fastcal-v10.txt";
   private static final double n = 0.95;
   private static final double o = 6.0;
   private static final int p = 20;
   private static final boolean q = Boolean.getBoolean("streamsreflowing.caldiag");
   private static final int r = 40;
   private static final int s = 2000;
   private static final int t = 300;
   private static final int u = 384;
   private static final double v = 0.7;
   private static final double w = 0.5;
   private static final Class<?> x;
   private static final Method y;
   private static final Method z;
   private static final Object A;
   private static final Object B;
   private static final double C = 1.1;
   private static final int D = 120;
   private static final int E;
   private static final int F = 6;

   private StreamsWorldgen() {
   }

   private static boolean a(TerrainSampler var0, TerrainSampler var1, BiomeSource var2, Sampler var3, int var4) {
      long var5 = 0L;
      long var7 = 0L;
      int var9 = 0;

      for (int var10 = 0; var10 < 200 && var9 < 16; var10++) {
         int var11 = 768 * (var10 * 5 % 29 - 14);
         int var12 = 768 * (var10 * 7 % 23 - 11);
         if (!var2.getNoiseBiome(QuartPos.fromBlock(var11), var4, QuartPos.fromBlock(var12), var3).is(BiomeTags.IS_OCEAN)) {
            long var13 = System.nanoTime();
            var0.height(var11, var12);
            long var15 = System.nanoTime();
            var1.height(var11, var12);
            var5 += var15 - var13;
            var7 += System.nanoTime() - var15;
            var9++;
         }
      }

      return var7 > 0L && var9 >= 8 && (double)var5 / (double)var7 > 0.7;
   }

   private static double a(DensityFunction var0, int var1, int var2, int var3, int var4, int var5) {
      if (var0.compute(new SinglePointContext(var1, var4, var2)) > 0.390625) {
         return (double)(var4 + 1);
      } else {
         int var6 = var3;
         int var7 = var4;

         while (var7 - var6 > 1) {
            int var8 = var6 + var7 >>> 1;
            if (var0.compute(new SinglePointContext(var1, var8, var2)) > 0.390625) {
               var6 = var8;
            } else {
               var7 = var8;
            }
         }

         int var9 = Math.min(var4, var7 + var5);
         return var0.compute(new SinglePointContext(var1, var9, var2)) > 0.390625 ? b(var0, var1, var2, var3, var4, var5) : (double)(var6 + 1);
      }
   }

   private static DensityFunction a(DensityFunction var0) {
      if (x == null) {
         return var0;
      } else {
         boolean[] var1 = new boolean[]{false};

         try {
            DensityFunction var2 = var0.mapAll(var1x -> {
               if (x.isInstance(var1x)) {
                  try {
                     Object var2x = y.invoke(var1x);
                     if (var2x == A || var2x == B) {
                        var1[0] = true;
                        return new StreamsWorldgen.ColumnCachedNode((DensityFunction)z.invoke(var1x));
                     }
                  } catch (ReflectiveOperationException var3x) {
                     throw new IllegalStateException(var3x);
                  }
               }

               return var1x;
            });
            return var1[0] ? var2 : var0;
         } catch (RuntimeException var3) {
            return var0;
         }
      }
   }

   private static double b(DensityFunction var0, int var1, int var2, int var3, int var4, int var5) {
      int var6 = var4;

      while (var6 >= var3) {
         if (var0.compute(new SinglePointContext(var1, var6, var2)) > 0.390625) {
            int var7 = Math.min(var4, var6 + var5 - 1);

            for (int var8 = var7; var8 > var6; var8--) {
               if (var0.compute(new SinglePointContext(var1, var8, var2)) > 0.390625) {
                  return (double)(var8 + 1);
               }
            }

            return (double)(var6 + 1);
         }

         var6 -= var5;
      }

      return (double)var3;
   }

   private static TerrainSampler a(TerrainSampler var0, ChunkGenerator var1, RandomState var2, ServerLevel var3, long var4) {
      if (!Boolean.getBoolean("streamsreflowing.noformgrades") && var1 instanceof NoiseBasedChunkGenerator var6) {
         try {
            NoiseSettings var7 = var6.generatorSettings().value().noiseSettings().clampToHeightAccessor(var3);
            int var8 = var7.minY();
            int var9 = var7.minY() + var7.height();
            int var10 = Math.max(1, var7.getCellHeight());
            FieldForm var11 = FieldFormBuilder.build(
               var3.registryAccess(), var4, var6.generatorSettings().value().useLegacyRandomSource(), var2.router().finalDensity(), var8, var9, var10
            );
            FormSampler var12 = new FormSampler(var0, var11, var8, var9, var10, Math.max(1, var7.getCellWidth()), 0.0, var6.getSeaLevel());
            RiverFlowField.BiomeClass var13 = RiverFlowFields.classifierFor(var3);
            if (!a(var0, var12, var2.router().finalDensity(), var13)) {
               a(var0, var12, var13);
               return var0;
            } else {
               if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                  a(var0, var12);
                  Constants.LOG.info("[SR-PREP] grade sampling: accelerated path in use for {}", var3.dimension().location());
               }

               a(var12, var13);
               a(var0, var12, var13);
               Constants.LOG.info("Streams Reflowing: stream preparation optimised for {}.", var3.dimension().location());
               return var12;
            }
         } catch (RuntimeException var14) {
            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
               Constants.LOG.info("[SR-PREP] IR grades declined (full stack):", (Throwable)var14);
            }

            return var0;
         }
      } else {
         if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            Constants.LOG
               .info(
                  "[SR-PREP] grade sampling not attempted: generator is {} (need a noise generator){}",
                  var1.getClass().getName(),
                  Boolean.getBoolean("streamsreflowing.noformgrades") ? ", and the kill switch is on" : ""
               );
         }

         return var0;
      }
   }

   private static void a(TerrainSampler var0, RiverFlowField.BiomeClass var1) {
      if (Boolean.getBoolean("streamsreflowing.prepdiag") && var1 != null) {
         try {
            byte var2 = 15;
            short var3 = 211;
            int var4 = 100000;
            long var5 = System.nanoTime();
            int var7 = 0;

            for (int var8 = 0; var8 < 15; var8++) {
               for (int var9 = 0; var9 < 15; var9++) {
                  var1.classify(100000 + var8 * 211, 100000 + var9 * 211);
                  var7++;
               }
            }

            var5 = System.nanoTime() - var5;
            long var20 = System.nanoTime();

            for (int var10 = 0; var10 < 15; var10++) {
               for (int var11 = 0; var11 < 15; var11++) {
                  var1.classify(100000 + var10 * 211, 100000 + var11 * 211);
               }
            }

            var20 = System.nanoTime() - var20;
            long var22 = System.nanoTime();

            for (int var12 = 0; var12 < 15; var12++) {
               for (int var13 = 0; var13 < 15; var13++) {
                  var0.height(100000 + var12 * 211, 100000 + var13 * 211);
               }
            }

            var22 = System.nanoTime() - var22;
            double var24 = (double)var5 / (double)var7;
            double var14 = (double)var20 / (double)var7;
            double var16 = (double)var22 / (double)var7;
            Constants.LOG
               .info(
                  "[SR-PREP] cost per column: classify cold {}us, warm {}us; height {}us -> height is {}x a cold classify, {}x a warm one",
                  a(var24),
                  a(var14),
                  a(var16),
                  String.format(Locale.ROOT, "%.0f", var24 > 0.0 ? var16 / var24 : 0.0),
                  String.format(Locale.ROOT, "%.0f", var14 > 0.0 ? var16 / var14 : 0.0)
               );
         } catch (RuntimeException var18) {
            Constants.LOG.info("[SR-PREP] cost probe failed:", (Throwable)var18);
         }
      }
   }

   private static String a(double var0) {
      return String.format(Locale.ROOT, "%.2f", var0 / 1000.0);
   }

   private static void a(TerrainSampler var0, TerrainSampler var1, RiverFlowField.BiomeClass var2) {
      int var3 = Integer.getInteger("streamsreflowing.iraudit", 0);
      if (var3 > 0) {
         int var4 = 0;
         int var5 = Math.max(3, (int)Math.round(Math.sqrt((double)var3 / 2.0)));
         int var6 = var5 / 2;
         int[] var7 = new int[4];
         int var8 = 0;
         int var9 = 0;
         double var10 = 0.0;
         double var12 = 0.0;
         StringBuilder var14 = new StringBuilder();
         long var15 = System.nanoTime();

         for (int var17 = 0; var17 < 2; var17++) {
            int var18 = var17 == 0 ? 211 : 512;

            for (int var19 = -var6; var19 <= var6; var19++) {
               for (int var20 = -var6; var20 <= var6; var20++) {
                  int var21 = var19 * var18;
                  int var22 = var20 * var18;
                  if (var2 != null && var2.classify(var21, var22) == 2) {
                     var4++;
                  } else {
                     double var23;
                     try {
                        var23 = var1.height(var21, var22) - var0.height(var21, var22);
                     } catch (RuntimeException var27) {
                        continue;
                     }

                     if (Double.isFinite(var23)) {
                        var9++;
                        var10 = Math.max(var10, var23);
                        var12 = Math.min(var12, var23);
                        double var25 = Math.abs(var23);
                        var7[var25 == 0.0 ? 0 : (var25 <= 3.0 ? 1 : (var25 <= 6.0 ? 2 : 3))]++;
                        if (!HeightAccuracy.inWindow(var23)) {
                           var8++;
                           if (var14.length() < 700) {
                              var14.append(String.format(Locale.ROOT, "%n    (%d,%d) d=%.1f", var21, var22, var23));
                           }
                        }
                     }
                  }
               }
            }
         }

         Constants.LOG
            .info(
               "[SR-AUDIT] {} land columns in {}s ({} ocean skipped): {} outside the window (+{}/-{}), highest {}, lowest {} | exact {}, 1-3 {}, 4-6 {}, 7+ {}{}",
               var9,
               String.format(Locale.ROOT, "%.1f", (double)(System.nanoTime() - var15) / 1.0E9),
               var4,
               var8,
               3.0,
               6.0,
               var10,
               var12,
               var7[0],
               var7[1],
               var7[2],
               var7[3],
               var8 == 0 ? "" : var14
            );
      }
   }

   private static boolean a(TerrainSampler var0, TerrainSampler var1, DensityFunction var2, RiverFlowField.BiomeClass var3) {
      FormSampler var4 = var1 instanceof FormSampler var5 ? var5 : null;
      if (var4 != null) {
         var4.setAquiferHandBack(false);
      }

      boolean var6 = a(var0, var1, var2, var4 != null, var3);
      if (!var6 && var4 != null) {
         Constants.LOG.info("[SR-PREP] arming the aquifer hand-back and re-judging");
         var4.setAquiferHandBack(true);
         var6 = a(var0, var1, var2, false, var3);
         if (!var6) {
            var4.setAquiferHandBack(false);
         }
      }

      return var6;
   }

   private static boolean a(TerrainSampler var0, TerrainSampler var1, DensityFunction var2, boolean var3, RiverFlowField.BiomeClass var4) {
      int var5 = 0;
      double var6 = 0.0;
      double var8 = 0.0;
      int var10 = 0;
      int var11 = 0;
      double var12 = 0.0;
      double var14 = 0.0;
      double var16 = 0.0;
      int var18 = 0;
      int var19 = 0;
      long var20 = 0L;
      long var22 = 0L;
      StringBuilder var24 = new StringBuilder();

      try {
         for (int var25 = 0; var25 < 2; var25++) {
            int var26 = var25 == 0 ? 211 : 512;

            for (int var27 = -7; var27 <= 7; var27++) {
               for (int var28 = -7; var28 <= 7; var28++) {
                  int var29 = var27 * var26;
                  int var30 = var28 * var26;
                  if (var4 != null && var4.classify(var29, var30) == 2) {
                     var5++;
                  } else {
                     long var31 = System.nanoTime();
                     double var33 = var1.height(var29, var30);
                     long var35 = System.nanoTime();
                     double var37 = var0.height(var29, var30);
                     long var39 = System.nanoTime();
                     if (var25 == 1) {
                        var20 += var35 - var31;
                        var22 += var39 - var35;
                     }

                     double var41 = var33 - var37;
                     if (!Double.isFinite(var41)) {
                        return false;
                     }

                     var19++;
                     var6 = Math.max(var6, var41);
                     var8 = Math.min(var8, var41);
                     if (!HeightAccuracy.inWindow(var41)) {
                        var18++;
                        if (var24.length() < 900) {
                           var24.append(String.format(Locale.ROOT, "%n    (%d,%d) ours=%.1f vanilla=%.1f d=%.1f", var29, var30, var33, var37, var41));
                        }
                     }

                     if (Math.abs(var41) > var16) {
                        var16 = Math.abs(var41);
                        var10 = var29;
                        var11 = var30;
                        var12 = var33;
                        var14 = var37;
                     }
                  }
               }
            }
         }
      } catch (RuntimeException var43) {
         return false;
      }

      if (var19 < 120) {
         Constants.LOG
            .info(
               "[SR-PREP] only {} land columns in {} sampled ({} ocean, answered by the fast path) -- too few to judge on, staying on host terrain",
               var19,
               var19 + var5,
               var5
            );
         return false;
      } else {
         if (Boolean.getBoolean("streamsreflowing.prepdiag") && var5 > 0) {
            Constants.LOG
               .info("[SR-PREP] judged {} land columns; {} ocean columns skipped (the fast path answers those before this sampler is asked)", var19, var5);
         }

         boolean var44 = var18 == 0;
         if (!var44 && var3) {
            return false;
         } else {
            if (!var44 && Boolean.getBoolean("streamsreflowing.prepdiag")) {
               Constants.LOG
                  .info(
                     "[SR-PREP] IR grades REFUSED: {}; worst at ({},{}) ours={} vanilla={}",
                     HeightAccuracy.describe(var18, var19, var6, var8),
                     var10,
                     var11,
                     var12,
                     var14
                  );
               Constants.LOG.info("[SR-PREP] offending columns:{}", var24);
               a(var1, var2, var10, var11, var12, var14);
            }

            if (!var44) {
               return false;
            } else {
               double var45 = var20 > 0L ? (double)var22 / (double)var20 : 0.0;
               if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                  long var46 = FormSampler.handedBack.sum();
                  long var47 = FormSampler.keptIr.sum();
                  Constants.LOG
                     .info(
                        "[SR-PREP] aquifer hand-back {} of {} sampled columns ({}%)",
                        var46,
                        var46 + var47,
                        String.format(Locale.ROOT, "%.1f", var46 + var47 == 0L ? 0.0 : 100.0 * (double)var46 / (double)(var46 + var47))
                     );
               }

               if (var45 < 1.1) {
                  Constants.LOG
                     .info("[SR-PREP] grade sampling: host terrain is already fast here ({}x), staying on it", String.format(Locale.ROOT, "%.2f", var45));
                  return false;
               } else {
                  if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                     Constants.LOG.info("[SR-PREP] grade speed: {}x over host terrain on the timed lattice", String.format(Locale.ROOT, "%.2f", var45));
                  }

                  return true;
               }
            }
         }
      }
   }

   private static void a(TerrainSampler var0, DensityFunction var1, int var2, int var3, double var4, double var6) {
      if (var0 instanceof FormSampler var8 && var1 != null) {
         try {
            Constants.LOG
               .info(
                  "[SR-PREP] walkCheck ({},{}) ours={} vanilla={} | both={} pinOnly={} interpOnly={} neither={}",
                  var2,
                  var3,
                  var4,
                  var6,
                  var8.heightWithSemantics(var2, var3, true, true),
                  var8.heightWithSemantics(var2, var3, true, false),
                  var8.heightWithSemantics(var2, var3, false, true),
                  var8.heightWithSemantics(var2, var3, false, false)
               );
            int var9 = (int)Math.min(var4, var6) - 2;
            int var10 = (int)Math.max(var4, var6) + 2;
            double var11 = 0.0;
            int var13 = Integer.MIN_VALUE;
            StringBuilder var14 = new StringBuilder();

            for (int var15 = var10; var15 >= var9; var15--) {
               double var16 = var8.density(var2, var15, var3);
               double var18 = var1.compute(new SinglePointContext(var2, var15, var3));
               double var20 = Math.abs(var16 - var18);
               if (var20 > var11) {
                  var11 = var20;
                  var13 = var15;
               }

               var14.append(
                  String.format(
                     Locale.ROOT,
                     "%n    y=%d ours=%+.6f real=%+.6f d=%.3e%s",
                     var15,
                     var16,
                     var18,
                     var20,
                     var18 > var8.solidThreshold() ? "  <- real says SOLID" : ""
                  )
               );
            }

            Constants.LOG
               .info(
                  "[SR-PREP] walkCheck density band [{}..{}] worst d={} at y={} -> {}{}",
                  var9,
                  var10,
                  String.format(Locale.ROOT, "%.3e", var11),
                  var13,
                  var11 < 1.0E-9 ? "densities AGREE, so the disagreement is in the column WALK" : "density DIVERGES, so the disagreement is in the TRANSLATION",
                  var14
               );
         } catch (RuntimeException var22) {
            Constants.LOG.info("[SR-PREP] walkCheck failed:", (Throwable)var22);
         }

         return;
      }
   }

   private static void a(TerrainSampler var0, TerrainSampler var1) {
      for (int var2 = -2; var2 <= 2; var2++) {
         for (int var3 = -2; var3 <= 2; var3++) {
            var0.height(var2 * 512, var3 * 512);
            var1.height(var2 * 512, var3 * 512);
         }
      }

      long var29 = 0L;
      long var4 = 0L;
      double var6 = 0.0;
      int var8 = 0;
      int var9 = 0;
      double var10 = 0.0;
      double var12 = 0.0;
      int var14 = 0;

      for (int var15 = -7; var15 <= 7; var15++) {
         for (int var16 = -7; var16 <= 7; var16++) {
            int var17 = var15 * 211;
            int var18 = var16 * 211;
            long var19 = System.nanoTime();
            double var21 = var0.height(var17, var18);
            long var23 = System.nanoTime();
            double var25 = var1.height(var17, var18);
            long var27 = System.nanoTime();
            var29 += var23 - var19;
            var4 += var27 - var23;
            if (Math.abs(var21 - var25) > var6) {
               var6 = Math.abs(var21 - var25);
               var8 = var17;
               var9 = var18;
               var10 = var25;
               var12 = var21;
            }

            var14++;
         }
      }

      Constants.LOG
         .info(
            "[SR-PREP] IR-grades benchmark, {} columns: vanilla {} ms, FormEval {} ms ({}x faster), maxDiff={} blocks at ({},{}) ours={} real={}",
            var14,
            String.format(Locale.ROOT, "%.1f", (double)var29 / 1000000.0),
            String.format(Locale.ROOT, "%.1f", (double)var4 / 1000000.0),
            String.format(Locale.ROOT, "%.2f", (double)var29 / (double)Math.max(1L, var4)),
            var6,
            var8,
            var9,
            var10,
            var12
         );
   }

   private static StreamsWorldgen.FastCal a(
      ChunkGenerator var0, RandomState var1, BiomeSource var2, Sampler var3, int var4, TerrainSampler var5, ServerLevel var6
   ) {
      int var7 = Boolean.getBoolean("streamsreflowing.exact") ? 2 : (Services.PLATFORM.fastTerrainEstimate() ? Services.PLATFORM.terrainSamplingMode() : 2);
      if (var7 < 2 && var0 instanceof NoiseBasedChunkGenerator var8) {
         double var9 = Services.PLATFORM.gradeWindowAbove();
         double var11 = Services.PLATFORM.gradeWindowBelow();
         NoiseSettings var13 = var8.generatorSettings().value().noiseSettings();
         DensityFunction var14 = var1.router().initialDensityWithoutJaggedness();
         int var15 = var13.minY();
         int var16 = var13.minY() + var13.height();
         int var17 = Math.max(1, var13.getCellHeight());
         DensityFunction var18 = a(var14);
         ThreadLocal var19 = var18 == var14 ? null : ThreadLocal.withInitial(() -> a(var14));
         TerrainSampler var20 = var19 == null
            ? (var4x, var5x) -> a(var14, var4x, var5x, var15, var16, var17)
            : (var4x, var5x) -> a((DensityFunction)var19.get(), var4x, var5x, var15, var16, var17);
         if (Boolean.getBoolean("streamsreflowing.calanalyze")) {
            b(var20, var5, var2, var3, var4);
         }

         int var21 = Integer.getInteger("streamsreflowing.fastcaldata", 0);
         if (var21 > 0) {
            try (BufferedWriter var22 = Files.newBufferedWriter(Path.of("streamsreflowing-fastcal-data.csv"))) {
               var22.write("x,z,est,real\n");

               for (int var23 = -var21; var23 <= var21; var23 += 64) {
                  for (int var24 = -var21; var24 <= var21; var24 += 64) {
                     if (!var2.getNoiseBiome(QuartPos.fromBlock(var24), var4, QuartPos.fromBlock(var23), var3).is(BiomeTags.IS_OCEAN)) {
                        var22.write(var24 + "," + var23 + "," + var20.height(var24, var23) + "," + var5.height(var24, var23) + "\n");
                     }
                  }
               }

               Constants.LOG.info("[SR-FASTCALDATA] wrote streamsreflowing-fastcal-data.csv");
            } catch (IOException var54) {
               Constants.LOG.warn("fastcaldata dump failed", (Throwable)var54);
            }
         }

         boolean var55 = Boolean.getBoolean("streamsreflowing.fastcaldump");
         Object var56 = null;

         try {
            String var58 = var6.dimension().location().toString().replace(':', '_').replace('/', '_');
            Path var25 = var6.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var58);
            Files.createDirectories(var25);
            var56 = var25.resolve("fastcal-v10.txt");
            if (!var55) {
               StreamsWorldgen.FastCal var26 = a((Path)var56, var5, var20, var7, var9, var11, var0.getSeaLevel());
               if (var26 != null) {
                  return var26;
               }
            }
         } catch (IOException var51) {
            var56 = null;
         }

         if (!var55) {
            BakedBandTable var59 = BakedBandTable.lookup(Services.PLATFORM::isModLoaded, Services.PLATFORM::modVersion);
            if (var59 != null) {
               boolean[] var62 = var59.safeForWindow(var9, var11, 20);
               int var64 = 0;

               for (boolean var73 : var62) {
                  if (var73) {
                     var64++;
                  }
               }

               StreamsWorldgen.BandCorrection var66 = new StreamsWorldgen.BandCorrection(var59.centers(), var59.bias(), var62);
               boolean var69 = a(var59.centers(), var62, var0.getSeaLevel());
               if (!"HYBRID".equals(var59.verdict()) && var69) {
                  if (q) {
                     Constants.LOG
                        .info(
                           "fast terrain estimate: ON (baked '{}' table, {} bands, {} grade-safe for [-{},+{}], mode {})",
                           var59.name(),
                           var62.length,
                           var64,
                           var11,
                           var9,
                           var7
                        );
                  }

                  return new StreamsWorldgen.FastCal(a(var20, var66, 1.0), a(var20, var66, 0.0), false, var7 == 0 ? 2 : 1, var66, var20, true);
               }

               if (q) {
                  Constants.LOG
                     .info(
                        "fast terrain estimate: HYBRID (baked '{}' table{})",
                        var59.name(),
                        "HYBRID".equals(var59.verdict()) ? "" : "; routing-critical bands unproven"
                     );
               }

               return new StreamsWorldgen.FastCal(var5, a(var20, var66, 0.0), true, 0, null, null, false);
            }
         }

         if (Boolean.getBoolean("streamsreflowing.bakedonly")) {
            if (q) {
               Constants.LOG.info("fast terrain estimate: OFF (bakedonly -- no table for this combo, no per-world fit)");
            }

            return new StreamsWorldgen.FastCal(var5, null, false, 0, null, null);
         } else {
            ArrayList var60 = new ArrayList();
            int var61 = var55 ? 289 : 800;
            int var63 = var55 ? 289 : Math.min(200, 384);
            ArrayList var27 = new ArrayList();

            for (int var28 = 0; var28 < var61 && var27.size() < var63; var28++) {
               int var29 = var55 ? 128 * (var28 % 17 - 8) : 768 * (var28 * 5 % 29 - 14);
               int var30 = var55 ? 128 * (var28 / 17 - 8) : 768 * (var28 * 7 % 23 - 11);
               if (!var2.getNoiseBiome(QuartPos.fromBlock(var29), var4, QuartPos.fromBlock(var30), var3).is(BiomeTags.IS_OCEAN)) {
                  var27.add(new int[]{var29, var30});
               }
            }

            double[][] var67 = new double[var27.size()][];
            AtomicLong var70 = new AtomicLong();
            AtomicLong var72 = new AtomicLong();
            LowResGrid.parallelFor(var27.size(), var6x -> {
               int[] var7x = (int[])var27.get(var6x);
               long var8x = System.nanoTime();
               double var10 = var20.height(var7x[0], var7x[1]);
               long var12 = System.nanoTime();
               double var14x = var5.height(var7x[0], var7x[1]);
               var70.addAndGet(var12 - var8x);
               var72.addAndGet(System.nanoTime() - var12);
               var67[var6x] = new double[]{var10, var14x};
            });
            Collections.addAll(var60, var67);
            long var31 = var70.get();
            long var33 = var72.get();
            double var35 = var33 > 0L ? (double)var31 / (double)var33 : 1.0;
            if (var35 > 0.7) {
               if (q) {
                  Constants.LOG.debug("fast terrain estimate: OFF (estimate too costly: {}% of a real call)", Math.round(var35 * 100.0));
               }

               return a((Path)var56, var9, var11, "OFF", null, new StreamsWorldgen.FastCal(var5, null, false, 0, null, null));
            } else {
               boolean var37 = var35 <= 0.5;
               if (var60.size() < 100) {
                  if (!var37) {
                     if (q) {
                        Constants.LOG
                           .debug("fast terrain estimate: OFF (few land points; estimate {}% of a real call, too costly for hybrid)", Math.round(var35 * 100.0));
                     }

                     return a((Path)var56, var9, var11, "OFF", null, new StreamsWorldgen.FastCal(var5, null, false, 0, null, null));
                  } else {
                     if (q) {
                        Constants.LOG.debug("fast terrain estimate: HYBRID (only {} land calibration points found)", var60.size());
                     }

                     return a((Path)var56, var9, var11, "HYBRID", null, new StreamsWorldgen.FastCal(var5, var20, true, 0, null, null));
                  }
               } else {
                  HashMap var38 = new HashMap();

                  for (double[] var40 : var60) {
                     var38.merge((int)Math.floor(var40[0] / 16.0) * 16, 1, Integer::sum);
                  }

                  int var74 = var60.size();
                  int var75 = 0;
                  int var41 = var55 ? 300 : Math.max(0, Math.min(300, 384 - var60.size()));
                  ArrayList var42 = new ArrayList();
                  ArrayList var43 = new ArrayList();

                  for (int var44 = 0; var44 < 2000 && var75 < var41; var44++) {
                     int var45 = 768 * (var44 * 11 % 31 - 15);
                     int var46 = 768 * (var44 * 13 % 27 - 13);
                     if (!var2.getNoiseBiome(QuartPos.fromBlock(var45), var4, QuartPos.fromBlock(var46), var3).is(BiomeTags.IS_OCEAN)) {
                        double var47 = var20.height(var45, var46);
                        Integer var49 = (Integer)var38.get((int)Math.floor(var47 / 16.0) * 16);
                        if (var49 != null && var49 < 40) {
                           var42.add(new int[]{var45, var46});
                           var43.add(var47);
                           var38.merge((int)Math.floor(var47 / 16.0) * 16, 1, Integer::sum);
                           var75++;
                        }
                     }
                  }

                  double[] var76 = new double[var42.size()];
                  LowResGrid.parallelFor(var42.size(), var3x -> {
                     int[] var4x = (int[])var42.get(var3x);
                     var76[var3x] = var5.height(var4x[0], var4x[1]);
                  });

                  for (int var77 = 0; var77 < var42.size(); var77++) {
                     var60.add(new double[]{(Double)var43.get(var77), var76[var77]});
                  }

                  StreamsWorldgen.BandCorrection var78 = a(var60, var9, var11);
                  int var79 = 0;

                  for (int var80 = 0; var80 < var74; var80++) {
                     double[] var48 = (double[])var60.get(var80);
                     double var83 = (var78 != null ? var78.correct(var48[0]) : var48[0]) - var48[1];
                     if (Math.abs(var83) <= 2.0) {
                        var79++;
                     }
                  }

                  int var81 = var79 * 100 / var74;
                  if (var55) {
                     Constants.LOG
                        .debug(
                           "fastcal dump: n={} bands={} corrected within +/-{}: {}%  costRatio={}%",
                           var60.size(),
                           var78 == null ? 0 : var78.centers().length,
                           2,
                           var81,
                           Math.round(var35 * 100.0)
                        );
                  }

                  if (var78 != null && var81 >= 80) {
                     if (!a(var78.centers(), var78.safe(), var0.getSeaLevel())) {
                        if (q) {
                           Constants.LOG.debug("fast terrain estimate: HYBRID (banded correction passed {}% but routing-critical bands unproven)", var81);
                        }

                        return a((Path)var56, var9, var11, "HYBRID", var78, new StreamsWorldgen.FastCal(var5, a(var20, var78, 0.0), true, 0, null, null));
                     } else {
                        if (q) {
                           Constants.LOG
                              .debug(
                                 "fast terrain estimate: ON (banded correction, {} bands, {} grade-safe, {}% of {} points within +/-{})",
                                 var78.centers().length,
                                 var78.safeCount(),
                                 var81,
                                 var74,
                                 2
                              );
                        }

                        return a(
                           (Path)var56,
                           var9,
                           var11,
                           "ON",
                           var78,
                           new StreamsWorldgen.FastCal(a(var20, var78, 1.0), a(var20, var78, 0.0), false, var7 == 0 ? 2 : 1, var78, var20)
                        );
                     }
                  } else if (!var37) {
                     if (q) {
                        Constants.LOG
                           .debug(
                              "fast terrain estimate: OFF (banded correction {}% within +/-{} AND estimate {}% of a real call, too costly for hybrid)",
                              var81,
                              2,
                              Math.round(var35 * 100.0)
                           );
                     }

                     return a((Path)var56, var9, var11, "OFF", null, new StreamsWorldgen.FastCal(var5, null, false, 0, null, null));
                  } else {
                     if (q) {
                        Constants.LOG
                           .debug(
                              "fast terrain estimate: HYBRID (banded correction {}% of {} points within +/-{}; estimate {}% of a real call)",
                              var81,
                              var74,
                              2,
                              Math.round(var35 * 100.0)
                           );
                     }

                     TerrainSampler var82 = var78 == null ? var20 : a(var20, var78, 0.0);
                     return a((Path)var56, var9, var11, "HYBRID", var78, new StreamsWorldgen.FastCal(var5, var82, true, 0, null, null));
                  }
               }
            }
         }
      } else {
         return new StreamsWorldgen.FastCal(var5, null, false, 0, null, null);
      }
   }

   private static TerrainSampler a(TerrainSampler var0, StreamsWorldgen.BandCorrection var1, double var2) {
      return (var4, var5) -> var1.correct(var0.height(var4, var5)) - var2;
   }

   private static StreamsWorldgen.BandCorrection a(List<double[]> var0, double var1, double var3) {
      TreeMap var5 = new TreeMap();

      for (byte var6 = 0; var6 < var0.size(); var6 += 2) {
         double[] var7 = (double[])var0.get(var6);
         var5.computeIfAbsent((int)Math.floor(var7[0] / 16.0) * 16, var0x -> new ArrayList<>()).add(var7);
      }

      ArrayList var24 = new ArrayList();
      ArrayList var25 = new ArrayList();

      for (Entry var9 : var5.entrySet()) {
         if (((List)var9.getValue()).size() >= 6) {
            ArrayList var10 = new ArrayList();

            for (double[] var12 : (List)var9.getValue()) {
               var10.add(var12[0] - var12[1]);
            }

            Collections.sort(var10);
            double var29 = (Double)var10.get(var10.size() / 2);
            double var13 = 0.0;

            for (double var15 = 0.0; var15 <= 2.0; var15++) {
               int var17 = 0;
               int var18 = 0;

               for (double var20 : var10) {
                  double var22 = var20 - var29 - var15 - 1.0;
                  if (!(var22 >= 6.0)) {
                     var17++;
                     if (var22 > var1 || var22 < -var3) {
                        var18++;
                     }
                  }
               }

               if (var17 > 0 && (double)var18 <= (double)var17 * 0.050000000000000044) {
                  var13 = var15;
                  break;
               }
            }

            var24.add((double)((Integer)var9.getKey()).intValue() + 8.0);
            var25.add(var29 + var13);
         }
      }

      if (var24.isEmpty()) {
         return null;
      } else {
         double[] var26 = var24.stream().mapToDouble(Double::doubleValue).toArray();
         double[] var27 = var25.stream().mapToDouble(Double::doubleValue).toArray();
         StreamsWorldgen.BandCorrection var28 = new StreamsWorldgen.BandCorrection(var26, var27, new boolean[var26.length]);
         int[] var30 = new int[var26.length];
         int[] var31 = new int[var26.length];

         for (byte var32 = 1; var32 < var0.size(); var32 += 2) {
            double[] var14 = (double[])var0.get(var32);
            double var35 = Math.floor(var14[0] / 16.0) * 16.0 + 8.0;
            int var36 = Arrays.binarySearch(var26, var35);
            if (var36 >= 0) {
               double var37 = var28.correct(var14[0]) - 1.0 - var14[1];
               if (!(var37 >= 6.0)) {
                  var30[var36]++;
                  if (var37 > var1 || var37 < -var3) {
                     var31[var36]++;
                  }
               }
            }
         }

         boolean[] var33 = new boolean[var26.length];

         for (int var34 = 0; var34 < var26.length; var34++) {
            var33[var34] = var30[var34] >= 20 && (double)var31[var34] <= (double)var30[var34] * 0.050000000000000044;
         }

         return new StreamsWorldgen.BandCorrection(var26, var27, var33);
      }
   }

   private static boolean a(double[] var0, boolean[] var1, int var2) {
      for (int var3 = 0; var3 < var1.length; var3++) {
         if (var0[var3] >= (double)(var2 - 24) && var0[var3] <= (double)(var2 + 24) && !var1[var3]) {
            return false;
         }
      }

      return true;
   }

   private static StreamsWorldgen.FastCal a(Path var0, double var1, double var3, String var5, StreamsWorldgen.BandCorrection var6, StreamsWorldgen.FastCal var7) {
      if (var0 != null) {
         try {
            StringBuilder var8 = new StringBuilder(var5).append(' ').append(var1).append(' ').append(var3).append('\n');
            if (var6 != null) {
               for (int var9 = 0; var9 < var6.centers().length; var9++) {
                  var8.append(var6.centers()[var9]).append(',').append(var6.bias()[var9]).append(',').append(var6.safe()[var9] ? 1 : 0).append('\n');
               }
            }

            Files.writeString(var0, var8.toString());
         } catch (IOException var10) {
            if (q) {
               Constants.LOG.debug("fast terrain calibration save failed: {}", var10.toString());
            }
         }
      }

      return var7;
   }

   private static StreamsWorldgen.FastCal a(Path var0, TerrainSampler var1, TerrainSampler var2, int var3, double var4, double var6, int var8) {
      try {
         if (!Files.isRegularFile(var0)) {
            return null;
         } else {
            List var9 = Files.readAllLines(var0);
            if (var9.isEmpty()) {
               return null;
            } else {
               String[] var10 = ((String)var9.get(0)).trim().split(" ");
               String var11 = var10[0];
               if (var10.length >= 3 && !(Math.abs(Double.parseDouble(var10[1]) - var4) > 1.0E-9) && !(Math.abs(Double.parseDouble(var10[2]) - var6) > 1.0E-9)) {
                  ArrayList var12 = new ArrayList();
                  ArrayList var13 = new ArrayList();
                  ArrayList var14 = new ArrayList();

                  for (int var15 = 1; var15 < var9.size(); var15++) {
                     String var16 = ((String)var9.get(var15)).trim();
                     if (!var16.isEmpty()) {
                        String[] var17 = var16.split(",");
                        var12.add(Double.parseDouble(var17[0]));
                        var13.add(Double.parseDouble(var17[1]));
                        var14.add("1".equals(var17[2]));
                     }
                  }

                  StreamsWorldgen.BandCorrection var19;
                  if (var12.isEmpty()) {
                     var19 = null;
                  } else {
                     boolean[] var20 = new boolean[var14.size()];

                     for (int var21 = 0; var21 < var20.length; var21++) {
                        var20[var21] = (Boolean)var14.get(var21);
                     }

                     var19 = new StreamsWorldgen.BandCorrection(
                        var12.stream().mapToDouble(Double::doubleValue).toArray(), var13.stream().mapToDouble(Double::doubleValue).toArray(), var20
                     );
                  }

                  switch (var11) {
                     case "OFF":
                        if (q) {
                           Constants.LOG.debug("fast terrain estimate: OFF (persisted calibration)");
                        }

                        return new StreamsWorldgen.FastCal(var1, null, false, 0, null, null);
                     case "HYBRID":
                        if (q) {
                           Constants.LOG.debug("fast terrain estimate: HYBRID (persisted calibration)");
                        }

                        return new StreamsWorldgen.FastCal(var1, var19 == null ? var2 : a(var2, var19, 0.0), true, 0, null, null);
                     case "ON":
                        if (var19 == null) {
                           return null;
                        } else {
                           if (!a(var19.centers(), var19.safe(), var8)) {
                              if (q) {
                                 Constants.LOG.debug("fast terrain estimate: HYBRID (persisted ON calibration; routing-critical bands unproven)");
                              }

                              return new StreamsWorldgen.FastCal(var1, a(var2, var19, 0.0), true, 0, null, null);
                           }

                           if (q) {
                              Constants.LOG
                                 .debug(
                                    "fast terrain estimate: ON (persisted calibration, {} bands, {} grade-safe, mode {})",
                                    var19.centers().length,
                                    var19.safeCount(),
                                    var3
                                 );
                           }

                           return new StreamsWorldgen.FastCal(a(var2, var19, 1.0), a(var2, var19, 0.0), false, var3 == 0 ? 2 : 1, var19, var2);
                        }
                     default:
                        return null;
                  }
               } else {
                  return null;
               }
            }
         }
      } catch (IOException | RuntimeException var18) {
         return null;
      }
   }

   public static RiverEngine engineIfPresent(ServerLevel var0) {
      synchronized (a) {
         return a.get(var0);
      }
   }

   public static RiverEngine engine(WorldGenLevel var0) {
      final ServerLevel var1 = var0.getLevel();
      synchronized (a) {
         RiverEngine var3 = a.get(var1);
         if (var3 != null) {
            if (var1.dimension().equals(Level.OVERWORLD)) {
               d = var3;
            }

            return var3;
         } else {
            final ChunkGenerator var4 = var1.getChunkSource().getGenerator();
            final RandomState var5 = var1.getChunkSource().randomState();
            long var6 = var0.getSeed();
            final BiomeSource var8 = var4.getBiomeSource();
            final Sampler var9 = var5.sampler();
            final TerrainSampler var10 = a((var3x, var4x) -> (double)var4.getBaseHeight(var3x, var4x, Types.OCEAN_FLOOR_WG, var1, var5), var4, var5, var1, var6);
            final ArrayList var11 = new ArrayList();
            final HashSet var12 = new HashSet();

            for (String var16 : Services.PLATFORM.outletBiomes().split(",")) {
               String var17 = var16.trim();
               if (!var17.isEmpty()) {
                  boolean var18 = var17.charAt(0) == '#';
                  ResourceLocation var19 = ResourceLocation.tryParse(var18 ? var17.substring(1) : var17);
                  if (var19 == null) {
                     Constants.LOG.warn("Streams Reflowing: ignoring unreadable outlet biome entry '{}'.", var17);
                  } else if (var18) {
                     var11.add(TagKey.create(Registries.BIOME, var19));
                  } else {
                     var12.add(var19);
                  }
               }
            }

            final boolean var57 = !var11.isEmpty() || !var12.isEmpty();
            final boolean var58 = Services.PLATFORM.fastOceanSkip();
            final RTFBridge.Handle var59 = Boolean.getBoolean("streamsreflowing.nortfheights") ? null : RTFBridge.handleFor(var5);
            final RiverFlowField.BiomeClass var60 = Boolean.getBoolean("streamsreflowing.noshareoceancache")
               ? (var3x, var4x) -> var8.getNoiseBiome(QuartPos.fromBlock(var3x), QuartPos.fromBlock(var4.getSeaLevel()), QuartPos.fromBlock(var4x), var9)
                        .is(BiomeTags.IS_OCEAN)
                     ? 2
                     : 0
               : RiverFlowFields.classifierFor(var1);
            int var61 = QuartPos.fromBlock(var4.getSeaLevel());
            final int var62 = var4.getSeaLevel();
            StreamBiomeBridge.landProbe = (var2, var3x, var4x) -> var8.getNoiseBiome(
                  QuartPos.fromBlock(var2), QuartPos.fromBlock(var3x), QuartPos.fromBlock(var4x), var9
               );
            StreamsWorldgen.FastCal var63 = a(var4, var5, var8, var9, var61, var10, var1);
            StreamsWorldgen.FastCal var20 = var63.gradePolicy() < 1 && !Boolean.getBoolean("streamsreflowing.hybridgrid")
               ? new StreamsWorldgen.FastCal(var10, null, false, 0, null, null)
               : var63;
            if (var20.gradePolicy() >= 1) {
               Constants.LOG.info("Streams Reflowing: fast terrain mode ENABLED for {}.", var1.dimension().location());
            } else if (var20.hybridGrid()) {
               Constants.LOG.info("Streams Reflowing: fast terrain mode PARTIALLY enabled for {}.", var1.dimension().location());
            } else {
               Constants.LOG.info("Streams Reflowing: fast terrain mode OFF for {}.", var1.dimension().location());
            }

            final TerrainSampler var21 = var20.land();
            final TerrainSampler var22 = var20.estimator();
            final boolean var23 = var20.hybridGrid();
            final int var24 = var20.gradePolicy();
            final StreamsWorldgen.BandCorrection var25 = var20.corr();
            final TerrainSampler var26 = var20.rawEst();
            final boolean var27 = var20.baked();
            final DensityFunction var28 = null;
            if (var4 instanceof NoiseBasedChunkGenerator) {
               var28 = var5.router().initialDensityWithoutJaggedness();
            }

            TerrainSampler var30 = new TerrainSampler() {
               @Override
               public boolean outletBiomeOk(int var1x, int var2, int var3) {
                  if (!var57) {
                     return true;
                  } else {
                     Holder var4x = var8.getNoiseBiome(QuartPos.fromBlock(var1x), QuartPos.fromBlock(var2), QuartPos.fromBlock(var3), var9);

                     for (TagKey var6 : var11) {
                        if (var4x.is(var6)) {
                           return true;
                        }
                     }

                     return !var12.isEmpty() && var4x.unwrapKey().map(var1xxx -> var12.contains(var1xxx.location())).orElse(false);
                  }
               }

               @Override
               public int waterBiomeClass(int var1x, int var2) {
                  return var59 != null && var59.river(var1x, var2) ? 1 : var60.classify(var1x, var2);
               }

               @Override
               public int waterBiomeClassAt(int var1x, int var2, int var3) {
                  if (var59 != null) {
                     return this.waterBiomeClass(var1x, var3);
                  } else {
                     Holder var4x = var8.getNoiseBiome(QuartPos.fromBlock(var1x), QuartPos.fromBlock(var2), QuartPos.fromBlock(var3), var9);
                     if (var4x.is(BiomeTags.IS_OCEAN)) {
                        return 2;
                     } else {
                        return !var4x.is(BiomeTags.IS_RIVER) && !var4x.is(Biomes.RIVER) && !var4x.is(Biomes.FROZEN_RIVER) ? 0 : 1;
                     }
                  }
               }

               @Override
               public double riverSurface(int var1x, int var2) {
                  return var59 != null ? var59.riverSurface(var1x, var2) : Double.NaN;
               }

               @Override
               public double height(int var1x, int var2) {
                  if (var58 && var60.classify(var1x, var2) == 2) {
                     return -1000.0;
                  } else {
                     return var59 != null ? var59.height(var1x, var2) : var21.height(var1x, var2);
                  }
               }

               @Override
               public boolean batchCapable() {
                  return false;
               }

               @Override
               public void heightsBatch(int[] var1x, int[] var2, double[] var3, int var4x) {
                  int[] var5x = new int[var4x];
                  int[] var6 = new int[var4x];
                  int[] var7 = new int[var4x];
                  int var8x = 0;

                  for (int var9x = 0; var9x < var4x; var9x++) {
                     if (var58 && var60.classify(var1x[var9x], var2[var9x]) == 2) {
                        var3[var9x] = -1000.0;
                     } else {
                        var5x[var8x] = var1x[var9x];
                        var6[var8x] = var2[var9x];
                        var7[var8x] = var9x;
                        var8x++;
                     }
                  }

                  if (var8x != 0) {
                     double[] var11x = new double[var8x];

                     for (int var10x = 0; var10x < var8x; var10x++) {
                        var11x[var10x] = var21.height(var5x[var10x], var6[var10x]);
                     }

                     for (int var12x = 0; var12x < var8x; var12x++) {
                        var3[var7[var12x]] = var11x[var12x];
                     }
                  }
               }

               @Override
               public double groundHeight(int var1x, int var2) {
                  if (var24 == 2) {
                     return this.height(var1x, var2);
                  } else {
                     if (var24 == 1) {
                        double var3 = var26.height(var1x, var2);
                        if (var25.gradeSafe(var3)) {
                           return var25.correct(var3) - 1.0;
                        }
                     }

                     return var58 && var60.classify(var1x, var2) == 2 ? -1000.0 : var10.height(var1x, var2);
                  }
               }

               @Override
               public double groundHeightExact(int var1x, int var2) {
                  return var58 && var60.classify(var1x, var2) == 2 ? -1000.0 : var10.height(var1x, var2);
               }

               @Override
               public boolean bakedCalibration() {
                  return var27;
               }

               @Override
               public boolean hasEstimate() {
                  return var59 != null || var22 != null;
               }

               @Override
               public boolean useHybridGridFill() {
                  return var23;
               }

               @Override
               public double estimate(int var1x, int var2) {
                  if (var22 != null) {
                     return var22.height(var1x, var2);
                  } else {
                     return var59 != null ? var59.height(var1x, var2) : this.height(var1x, var2);
                  }
               }

               @Override
               public double surfaceHeight(int var1x, int var2) {
                  return var60.classify(var1x, var2) == 2 ? (double)var62 : (double)var4.getBaseHeight(var1x, var2, Types.WORLD_SURFACE_WG, var1, var5);
               }

               @Override
               public boolean openAt(int var1x, int var2, int var3) {
                  return var28 == null ? (double)var2 > this.height(var1x, var3) : var28.compute(new SinglePointContext(var1x, var2, var3)) <= 0.390625;
               }
            };
            CachingTerrainSampler var31 = new CachingTerrainSampler(var30, 65536, -1000.0);
            WetnessSampler var32 = (var2, var3x, var4x) -> Services.PLATFORM
                  .biomeWetness(var8.getNoiseBiome(QuartPos.fromBlock(var2), QuartPos.fromBlock(var3x), QuartPos.fromBlock(var4x), var9));
            int var33 = Services.PLATFORM.gridSpacing();
            RiverEngine var34 = new RiverEngine(
               var6,
               StreamsConfig.defaults(
                     var33,
                     Services.PLATFORM.widthScale(),
                     Services.PLATFORM.plateScale(),
                     Services.PLATFORM.terrainSampleSpacing(),
                     Services.PLATFORM.valleySnapRadius()
                  )
                  .withMinRiverAccumulation(6.0)
                  .withWetnessMultiplier(Services.PLATFORM.wetnessMultiplier())
                  .withWetnessDryFloor(Services.PLATFORM.wetnessDryFloor())
                  .withLakeArea(Services.PLATFORM.generateLakes() ? Services.PLATFORM.lakeMinArea() : Double.MAX_VALUE, Services.PLATFORM.lakeMaxArea())
                  .withMinSeaChainNodes(Services.PLATFORM.minStreamLength() / var33)
                  .withOutletBiomes(Services.PLATFORM.outletBiomes())
                  .withConnect(Services.PLATFORM.connectNearbyWater(), (double)Services.PLATFORM.connectRadius())
                  .withSinkRooted(Services.PLATFORM.sinkRooted())
                  .withGrowDensity(Services.PLATFORM.growDensity())
                  .withConfineToBanks(Services.PLATFORM.confineToBanks())
                  .withContainmentBerm(Services.PLATFORM.containmentBerm())
                  .withAppearance(
                     Services.PLATFORM.meanderStrength(), Services.PLATFORM.baseWidth(), Services.PLATFORM.maxWidth(), Services.PLATFORM.streamDepth()
                  )
                  .withBankNoise(Services.PLATFORM.bankNoise()),
               var31,
               var32
            );
            if (System.getProperty("streamsreflowing.benchcalls") != null) {
               RiverFlowField.BiomeClass var35 = RiverFlowFields.classifierFor(var1);
               TerrainSampler var36 = var63.estimator();
               Random var37 = new Random(20260810L);
               short var38 = 1000;
               int[] var39 = new int[var38];
               int[] var40 = new int[var38];
               double var41 = 0.0;

               for (int var43 = 1; var43 <= 2; var43++) {
                  for (int var44 = 0; var44 < var38; var44++) {
                     var39[var44] = var37.nextInt(8192) - 4096;
                     var40[var44] = var37.nextInt(8192) - 4096;
                  }

                  long var74 = System.nanoTime();
                  if (var36 != null) {
                     for (int var46 = 0; var46 < var38; var46++) {
                        var41 += var36.height(var39[var46], var40[var46]);
                     }
                  }

                  long var75 = System.nanoTime();

                  for (int var48 = 0; var48 < var38; var48++) {
                     var41 += (double)var4.getBaseHeight(var39[var48], var40[var48], Types.OCEAN_FLOOR_WG, var1, var5);
                  }

                  long var76 = System.nanoTime();

                  for (int var50 = 0; var50 < var38; var50++) {
                     var41 += (double)var35.classify(var39[var50], var40[var50]);
                  }

                  long var77 = System.nanoTime();
                  Constants.LOG
                     .info(
                        "[SR-BENCH] pass {} ({}), {} calls each: IR estimate {} | getBaseHeight {} ms ({} us/call) | biome classify {} ms ({} us/call) [sink {}]",
                        var43,
                        var43 == 1 ? "cold" : "warm",
                        Integer.valueOf(var38),
                        var36 != null
                           ? String.format(
                              Locale.ROOT, "%.1f ms (%.2f us/call)", (double)(var75 - var74) / 1000000.0, (double)(var75 - var74) / 1000.0 / (double)var38
                           )
                           : "n/a (no calibrated evaluator for this router)",
                        String.format(Locale.ROOT, "%.1f", (double)(var76 - var75) / 1000000.0),
                        String.format(Locale.ROOT, "%.2f", (double)(var76 - var75) / 1000.0 / (double)var38),
                        String.format(Locale.ROOT, "%.1f", (double)(var77 - var76) / 1000000.0),
                        String.format(Locale.ROOT, "%.2f", (double)(var77 - var76) / 1000.0 / (double)var38),
                        (long)var41
                     );
               }
            }

            var34.setBuildListener(
               (var2, var3x, var4x, var5x, var7) -> {
                  int var8x = var3x + var4x;
                  int var9x = var8x > 0 ? var4x * 100 / var8x : 0;
                  Constants.LOG
                     .debug(
                        "Built river region {} @ gridSpacing {}: {} samples ({} real, {} ocean-skipped = {}%), {} segments, {} ms, sample-cache {}% hits",
                        var2,
                        var33,
                        var8x,
                        var3x,
                        var4x,
                        var9x,
                        var7,
                        String.format("%.1f", var5x),
                        var31.hitPercent()
                     );
               }
            );

            try {
               String var64 = var1.dimension().location().toString().replace(':', '_').replace('/', '_');
               Path var66 = var1.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var64);
               FileNetworkStore var68 = new FileNetworkStore(var66, var6, var34.config());
               var34.setNetworkStore(var68);
               if (var1.dimension().equals(Level.OVERWORLD)) {
                  RiverFlowFields.initStore(var66, var68.fingerprint());
                  RawFlowTileStore.initStore(var66, var68.fingerprint());
               }
            } catch (RuntimeException var55) {
               Constants.LOG.warn("River network cache unavailable; networks won't persist this session ({})", var55.toString());
            }

            if (var27 || var24 >= 1 || var23) {
               try {
                  String var65 = var1.dimension().location().toString().replace(':', '_').replace('/', '_');
                  Path var67 = var1.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(var65);
                  Path var69 = var67.resolve("fastcal-v10.txt");
                  Path var70 = var67.resolve("estcost.flag");
                  double var71 = Services.PLATFORM.gradeWindowAbove();
                  double var72 = Services.PLATFORM.gradeWindowBelow();

                  try {
                     if (Files.isRegularFile(var70)) {
                        String var73 = Files.readString(var70).trim();
                        if (var73.startsWith("ON")) {
                           var34.setEstCostVerdict(1);
                        } else if (var73.startsWith("OFF")) {
                           var34.setEstCostVerdict(2);
                        }
                     }
                  } catch (IOException var53) {
                  }

                  var34.setEstCostPersist(var6x -> {
                     try {
                        Files.writeString(var70, var6x == 2 ? "OFF\n" : "ON\n");
                        if (var6x == 2) {
                           Files.writeString(var69, "OFF " + var71 + " " + var72 + "\n");
                        }
                     } catch (IOException var8x) {
                     }
                  });
               } catch (RuntimeException var54) {
               }
            }

            var34.setStreamsEnabled(Services.PLATFORM.generateStreams());
            a.put(var1, var34);
            if (var1.dimension().equals(Level.OVERWORLD)) {
               d = var34;
               StreamPrefetcher.warmWorldOrigin(var34);
            }

            return var34;
         }
      }
   }

   public static RiverEngine active() {
      return d;
   }

   public static boolean structureFootprintOnStream(BoundingBox var0) {
      RiverEngine var1 = d;
      if (var1 == null) {
         return false;
      } else {
         int var2 = E;
         double var3 = (double)(var0.maxX() - var0.minX()) / 2.0 + (double)var2 + 1.0;
         double var5 = (double)(var0.maxZ() - var0.minZ()) / 2.0 + (double)var2 + 1.0;
         double var7 = (double)(var0.minX() + var0.maxX()) / 2.0;
         double var9 = (double)(var0.minZ() + var0.maxZ()) / 2.0;
         double var11 = Math.sqrt(var3 * var3 + var5 * var5);
         if (!var1.platesReadyNear(var7, var9, var11)) {
            var1.whenPlatesReadyNear(var7, var9, var11);
            return false;
         } else {
            int var13 = var0.minX() - var2;
            int var14 = var0.maxX() + var2;
            int var15 = var0.minZ() - var2;
            int var16 = var0.maxZ() + var2;

            for (int var17 = var13; var17 <= var14; var17 += 16) {
               for (int var18 = var15; var18 <= var16; var18 += 16) {
                  if (var1.corridorNearChunk(var17, var18, var2)) {
                     int var19 = Math.max(var13, var17 - var2);
                     int var20 = Math.min(var14, var17 + 15 + var2);
                     int var21 = Math.max(var15, var18 - var2);
                     int var22 = Math.min(var16, var18 + 15 + var2);

                     for (int var23 = var19; var23 <= var20 + 3; var23 += 4) {
                        int var24 = Math.min(var23, var20);

                        for (int var25 = var21; var25 <= var22 + 3; var25 += 4) {
                           int var26 = Math.min(var25, var22);
                           if (var1.inCorridor(var24, var26) && var0.maxY() >= var1.plan(var24, var26).waterSurfaceY() - 6) {
                              return true;
                           }
                        }
                     }
                  }
               }
            }

            for (int var27 = var13; var27 <= var14 + 7; var27 += 8) {
               int var28 = Math.min(var27, var14);

               for (int var29 = var15; var29 <= var16 + 7; var29 += 8) {
                  int var30 = Math.min(var29, var16);
                  int var31 = var1.lakeSurfaceAt(var28, var30);
                  if (var31 != Integer.MIN_VALUE && var0.maxY() >= var31 - 6) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public static void unloadLevel(ServerLevel var0) {
      RiverEngine var1;
      synchronized (a) {
         var1 = a.remove(var0);
      }

      if (var1 != null) {
         var1.releaseThreadMemos();
         if (var1 == d) {
            d = null;
         }
      }

      RiverFlowFields.unloadLevel(var0);
   }

   public static void clearActive() {
      d = null;
      synchronized (a) {
         for (RiverEngine var2 : a.values()) {
            var2.releaseThreadMemos();
         }

         a.clear();
      }

      StreamPrefetcher.clearPending();
      RiverFlowFields.clear();
      StreamFlowGrid.clear();
      ChunkPlanCache.clear();
      RawFlowTileStore.close();
      RawFlowTiles.clear();
   }

   private static void b(TerrainSampler var0, TerrainSampler var1, BiomeSource var2, Sampler var3, int var4) {
      int var5 = Integer.getInteger("streamsreflowing.calanalyze.n", 800);
      int var6 = Integer.getInteger("streamsreflowing.calanalyze.spread", 512);
      int var7 = Math.max(6000, var5 * 8);
      ArrayList var8 = new ArrayList();

      for (int var9 = 0; var9 < var7 && var8.size() < var5; var9++) {
         int var10 = var9 * 13 % 79 - 39;
         int var11 = var9 * 29 % 83 - 41;
         int var12 = var10 * var6;
         int var13 = var11 * var6;
         if (!var2.getNoiseBiome(QuartPos.fromBlock(var12), var4, QuartPos.fromBlock(var13), var3).is(BiomeTags.IS_OCEAN)) {
            var8.add(new int[]{var12, var13});
         }
      }

      int var32 = var8.size();
      double[] var33 = new double[var32];
      double[] var34 = new double[var32];
      String[] var35 = new String[var32];
      double[][] var36 = new double[var32][];
      LowResGrid.parallelFor(var32, var10x -> {
         int[] var11x = (int[])var8.get(var10x);
         var33[var10x] = var0.height(var11x[0], var11x[1]);
         var34[var10x] = var1.height(var11x[0], var11x[1]);
         Holder var12x = var2.getNoiseBiome(QuartPos.fromBlock(var11x[0]), var4, QuartPos.fromBlock(var11x[1]), var3);
         var35[var10x] = var12x.unwrapKey().map(var0xx -> var0xx.location().toString()).orElse("?");
         var36[var10x] = a(var3, var11x[0], var11x[1]);
      });
      StringBuilder var14 = new StringBuilder("x,z,est,real,err,biome,continentalness,erosion,weirdness,temperature,humidity\n");

      for (int var15 = 0; var15 < var32; var15++) {
         int[] var16 = (int[])var8.get(var15);
         var14.append(var16[0])
            .append(',')
            .append(var16[1])
            .append(',')
            .append(var33[var15])
            .append(',')
            .append(var34[var15])
            .append(',')
            .append(var33[var15] - var34[var15])
            .append(',')
            .append(var35[var15]);

         for (double var20 : var36[var15]) {
            var14.append(',').append(String.format(Locale.ROOT, "%.4f", var20));
         }

         var14.append('\n');
      }

      try {
         Files.writeString(Path.of("streamsreflowing-calanalysis.csv"), var14);
      } catch (IOException var31) {
         Constants.LOG.info("[SR-CAL] could not write analysis csv: {}", (Throwable)var31);
      }

      double[] var37 = new double[var32];

      for (int var38 = 0; var38 < var32; var38++) {
         var37[var38] = var33[var38] - var34[var38];
      }

      Constants.LOG.info("[SR-CAL] calibration analysis: {} land columns, raw error sigma {} (mean {})", var32, b(b(var37)), b(a(var37)));
      String[] var39 = new String[]{
         "elevation/16 (SHIPPED)",
         "biome",
         "continentalness/0.05",
         "erosion/0.05",
         "weirdness/0.05",
         "temperature/0.05",
         "humidity/0.05",
         "elevation/16 x biome"
      };
      List var40 = List.of(
         var1x -> "e" + Math.floorDiv((int)var34[var1x], 16),
         var1x -> var35[var1x],
         var1x -> "c" + Math.round(var36[var1x][0] / 0.05),
         var1x -> "r" + Math.round(var36[var1x][1] / 0.05),
         var1x -> "w" + Math.round(var36[var1x][2] / 0.05),
         var1x -> "t" + Math.round(var36[var1x][3] / 0.05),
         var1x -> "h" + Math.round(var36[var1x][4] / 0.05),
         var2x -> "e" + Math.floorDiv((int)var34[var2x], 16) + "|" + var35[var2x]
      );

      for (int var41 = 0; var41 < var39.length; var41++) {
         IntFunction var42 = (IntFunction)var40.get(var41);
         LinkedHashMap var43 = new LinkedHashMap();

         for (int var21 = 0; var21 < var32; var21++) {
            var43.computeIfAbsent((String)var42.apply(var21), var0x -> new ArrayList<>()).add(var21);
         }

         ArrayList var44 = new ArrayList();
         ArrayList var22 = new ArrayList();
         int var23 = 0;

         for (List var25 : var43.values()) {
            if (var25.size() < 6) {
               var22.addAll(var25);
            } else {
               var23++;
               double var26 = 0.0;

               for (int var29 : var25) {
                  var26 += var37[var29];
               }

               var26 /= (double)var25.size();

               for (int var58 : var25) {
                  var44.add(var37[var58] - var26);
               }
            }
         }

         if (!var22.isEmpty()) {
            double var45 = 0.0;

            for (int var27 : var22) {
               var45 += var37[var27];
            }

            var45 /= (double)var22.size();

            for (int var54 : var22) {
               var44.add(var37[var54] - var45);
            }
         }

         double[] var47 = new double[var44.size()];

         for (int var48 = 0; var48 < var47.length; var48++) {
            var47[var48] = (Double)var44.get(var48);
         }

         int var49 = 0;

         for (double var59 : var47) {
            if (Math.abs(var59) <= 2.0) {
               var49++;
            }
         }

         Constants.LOG
            .info(
               "[SR-CAL]   {}: residual sigma {}, {}% within +-2 ({} usable buckets, {} sparse pts)",
               var39[var41],
               b(b(var47)),
               Math.round(100.0 * (double)var49 / (double)Math.max(1, var47.length)),
               var23,
               var22.size()
            );
      }

      Constants.LOG.info("[SR-CAL] lowest residual sigma = the variable the correction should key on; wrote streamsreflowing-calanalysis.csv");
   }

   private static double[] a(Sampler var0, int var1, int var2) {
      double[] var3 = new double[5];

      try {
         TargetPoint var4 = var0.sample(QuartPos.fromBlock(var1), 0, QuartPos.fromBlock(var2));
         String[] var5 = new String[]{"continentalness", "erosion", "weirdness", "temperature", "humidity"};

         for (int var6 = 0; var6 < var5.length; var6++) {
            Object var7 = var4.getClass().getMethod(var5[var6]).invoke(var4);
            var3[var6] = ((Number)var7).doubleValue() / 10000.0;
         }
      } catch (RuntimeException | ReflectiveOperationException var8) {
         Arrays.fill(var3, Double.NaN);
      }

      return var3;
   }

   private static double a(double[] var0) {
      double var1 = 0.0;

      for (double var6 : var0) {
         var1 += var6;
      }

      return var0.length == 0 ? 0.0 : var1 / (double)var0.length;
   }

   private static double b(double[] var0) {
      if (var0.length < 2) {
         return 0.0;
      } else {
         double var1 = a(var0);
         double var3 = 0.0;

         for (double var8 : var0) {
            var3 += (var8 - var1) * (var8 - var1);
         }

         return Math.sqrt(var3 / (double)(var0.length - 1));
      }
   }

   private static String b(double var0) {
      return String.format(Locale.ROOT, "%.2f", var0);
   }

   static {
      RiverEngine.setBuildRequester(StreamPrefetcher::submitChunkDemand);
      Class var0 = null;
      Method var1 = null;
      Method var2 = null;
      Object var3 = null;
      Object var4 = null;

      try {
         DensityFunction var5 = DensityFunctions.flatCache(DensityFunctions.zero());
         DensityFunction var6 = DensityFunctions.cache2d(DensityFunctions.zero());
         if (var5.getClass() == var6.getClass()) {
            var0 = var5.getClass();

            for (Method var10 : var0.getDeclaredMethods()) {
               if (var10.getParameterCount() == 0) {
                  if (var10.getReturnType().isEnum()) {
                     var1 = var10;
                  } else if (DensityFunction.class.isAssignableFrom(var10.getReturnType())) {
                     var2 = var10;
                  }
               }
            }

            if (var1 != null && var2 != null) {
               var1.setAccessible(true);
               var2.setAccessible(true);
               var3 = var1.invoke(var5);
               var4 = var1.invoke(var6);
            } else {
               var0 = null;
            }
         }
      } catch (Throwable var11) {
         var0 = null;
      }

      x = var0;
      y = var0 != null ? var1 : null;
      z = var0 != null ? var2 : null;
      A = var0 != null ? var3 : null;
      B = var0 != null ? var4 : null;
      E = Math.max(0, Integer.getInteger("streamsreflowing.structurepad", 8));
   }

   private static record BandCorrection() {
      private final double[] centers;
      private final double[] bias;
      private final boolean[] safe;

      private BandCorrection(double[] var1, double[] var2, boolean[] var3) {
         this.centers = var1;
         this.bias = var2;
         this.safe = var3;
      }

      double correct(double var1) {
         if (this.centers.length != 1 && !(var1 <= this.centers[0])) {
            if (var1 >= this.centers[this.centers.length - 1]) {
               return var1 - this.bias[this.bias.length - 1];
            } else {
               int var3 = 1;

               while (this.centers[var3] < var1) {
                  var3++;
               }

               double var4 = (var1 - this.centers[var3 - 1]) / (this.centers[var3] - this.centers[var3 - 1]);
               return var1 - (this.bias[var3 - 1] + var4 * (this.bias[var3] - this.bias[var3 - 1]));
            }
         } else {
            return var1 - this.bias[0];
         }
      }

      boolean gradeSafe(double var1) {
         if (var1 < this.centers[0] - 16.0 || var1 > this.centers[this.centers.length - 1] + 16.0) {
            return false;
         } else if (this.centers.length == 1) {
            return this.safe[0];
         } else {
            int var3 = 1;

            while (var3 < this.centers.length && this.centers[var3] < var1) {
               var3++;
            }

            boolean var4 = this.safe[Math.max(0, var3 - 1)];
            boolean var5 = this.safe[Math.min(this.safe.length - 1, var3)];
            return var4 && var5;
         }
      }

      int safeCount() {
         int var1 = 0;

         for (boolean var5 : this.safe) {
            if (var5) {
               var1++;
            }
         }

         return var1;
      }
   }

   private static final class ColumnCachedNode implements SimpleFunction {
      private final DensityFunction a;
      private long b = Long.MIN_VALUE;
      private double c;

      ColumnCachedNode(DensityFunction var1) {
         this.a = var1;
      }

      @Override
      public double compute(FunctionContext var1) {
         long var2 = (long)var1.blockX() << 32 ^ (long)var1.blockZ() & 4294967295L;
         if (var2 != this.b) {
            this.c = this.a.compute(var1);
            this.b = var2;
         }

         return this.c;
      }

      @Override
      public double minValue() {
         return this.a.minValue();
      }

      @Override
      public double maxValue() {
         return this.a.maxValue();
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return this.a.codec();
      }
   }

   private static record FastCal() {
      private final TerrainSampler land;
      private final TerrainSampler estimator;
      private final boolean hybridGrid;
      private final int gradePolicy;
      private final StreamsWorldgen.BandCorrection corr;
      private final TerrainSampler rawEst;
      private final boolean baked;

      FastCal(TerrainSampler var1, TerrainSampler var2, boolean var3, int var4, StreamsWorldgen.BandCorrection var5, TerrainSampler var6) {
         this(var1, var2, var3, var4, var5, var6, false);
      }

      private FastCal(TerrainSampler var1, TerrainSampler var2, boolean var3, int var4, StreamsWorldgen.BandCorrection var5, TerrainSampler var6, boolean var7) {
         this.land = var1;
         this.estimator = var2;
         this.hybridGrid = var3;
         this.gradePolicy = var4;
         this.corr = var5;
         this.rawEst = var6;
         this.baked = var7;
      }
   }
}
