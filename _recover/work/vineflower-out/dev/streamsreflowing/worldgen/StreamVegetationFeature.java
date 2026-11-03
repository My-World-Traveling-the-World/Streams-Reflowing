package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.bank.BankFeature;
import dev.streamsreflowing.worldgen.bank.BankFeatureLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class StreamVegetationFeature extends Feature<NoneFeatureConfiguration> {
   private static final double a = 0.75;
   private static final BlockState b = Blocks.SHORT_GRASS.defaultBlockState();
   private static final BlockState c = Blocks.TALL_GRASS.defaultBlockState();
   private static final BlockState d = Blocks.FERN.defaultBlockState();
   private static volatile Set<ResourceLocation> e;
   private static volatile Set<String> f;
   private static volatile String g;
   private static volatile Object h;
   private static volatile List<BankFeature> i = List.of();

   public StreamVegetationFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      } else {
         WorldGenLevel var2 = var1.level();
         ChunkPos var3 = new ChunkPos(var1.origin());
         int var4 = var3.getMinBlockX();
         int var5 = var3.getMinBlockZ();
         RiverEngine var6 = StreamsWorldgen.engine(var2);
         RandomSource var7 = var1.random();
         ChunkGenerator var8 = var1.chunkGenerator();
         int var9 = Mth.clamp(Services.PLATFORM.floraRadius(), 1, 16);
         MutableBlockPos var10 = new MutableBlockPos();
         boolean var11 = false;
         var11 |= this.a(var2, var6, var8, var7, var4, var5, var9, var10);
         int var12 = Services.PLATFORM.underwaterFlora();
         int var13 = Services.PLATFORM.underwaterFloraPercent();
         if (var12 > 0 && var13 > 0) {
            if (var12 >= 3) {
               var11 |= this.a(var2, var7, var4, var5, var13);
            } else {
               var11 |= this.a(var2, var6, var7, var4, var5, var13);
               if (var12 >= 2) {
                  var11 |= this.b(var2, var6, var7, var4, var5, var13);
               }
            }
         }

         int var14 = Services.PLATFORM.vegetation();
         int var15 = Services.PLATFORM.allWaterFlora();
         if (var14 <= 0 && var15 <= 0) {
            return var11;
         } else {
            if (var15 > 0) {
               var11 |= this.a(var2, var4, var5, var9, var10, (var6x, var7x, var8x) -> this.a(var2, var8, var7, var10, var6x, var7x, var8x, var15));
            } else {
               var11 |= this.a(var2, var6, var4, var5, var9, (var6x, var7x, var8x) -> this.a(var2, var8, var7, var10, var6x, var7x, var8x, var14));
            }

            return var11;
         }
      }
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, int var3, int var4, int var5, StreamVegetationFeature.ColumnAction var6) {
      ColumnPlan[] var7 = ChunkPlanCache.get(var2, var3, var4);
      new MutableBlockPos();
      boolean var9 = false;

      for (int var10 = 0; var10 < 16; var10++) {
         for (int var11 = 0; var11 < 16; var11++) {
            int var12 = var3 + var10;
            int var13 = var4 + var11;
            ColumnPlan var14 = var7 != null ? var7[var10 * 16 + var11] : null;
            if (var14 == null) {
               var14 = var2.plan(var12, var13);
            }

            if (var14 != ColumnPlan.NONE
               && !var14.inChannel()
               && !StreamBiomeBlacklist.isBlocked(var1.getUncachedNoiseBiome(var12 >> 2, var14.waterSurfaceY() >> 2, var13 >> 2))) {
               double var15 = var14.distance() - var14.width();
               if (!(var15 > (double)var5)) {
                  var9 |= var6.apply(var12, var13, Mth.clamp(var15 / (double)var5, 0.0, 1.0));
               }
            }
         }
      }

      return var9;
   }

   private boolean a(WorldGenLevel var1, int var2, int var3, int var4, MutableBlockPos var5, StreamVegetationFeature.ColumnAction var6) {
      int var7 = 16 + 2 * var4;
      boolean[] var8 = new boolean[var7 * var7];
      boolean var9 = false;

      for (int var10 = -var4; var10 < 16 + var4; var10++) {
         for (int var11 = -var4; var11 < 16 + var4; var11++) {
            int var12 = var2 + var10;
            int var13 = var3 + var11;
            int var14 = var1.getHeight(Types.OCEAN_FLOOR_WG, var12, var13);
            var5.set(var12, var14, var13);
            if (var1.getBlockState(var5).getFluidState().is(FluidTags.WATER)) {
               var8[(var10 + var4) * var7 + var11 + var4] = true;
               var9 = true;
            }
         }
      }

      if (!var9) {
         return false;
      } else {
         double var21 = (double)var4 * (double)var4;
         boolean var22 = false;

         for (int var23 = 0; var23 < 16; var23++) {
            for (int var24 = 0; var24 < 16; var24++) {
               if (!var8[(var23 + var4) * var7 + var24 + var4]) {
                  double var15 = Double.MAX_VALUE;

                  for (int var17 = -var4; var17 <= var4; var17++) {
                     for (int var18 = -var4; var18 <= var4; var18++) {
                        if (var8[(var23 + var17 + var4) * var7 + var24 + var18 + var4]) {
                           double var19 = (double)(var17 * var17 + var18 * var18);
                           if (var19 < var15) {
                              var15 = var19;
                           }
                        }
                     }
                  }

                  if (!(var15 > var21)) {
                     var22 |= var6.apply(var2 + var23, var3 + var24, Mth.clamp(Math.sqrt(var15) / (double)var4, 0.0, 1.0));
                  }
               }
            }
         }

         return var22;
      }
   }

   private static List<BankFeature> a(WorldGenLevel var0) {
      RegistryAccess var1 = var0.registryAccess();
      if (h != var1) {
         ArrayList var2 = new ArrayList<>(BankFeatureLoader.configEntries());
         var0.registryAccess().registry(BankFeature.REGISTRY_KEY).ifPresent(var1x -> var1x.forEach(var2::add));
         i = List.copyOf(var2);
         h = var1;
      }

      return i;
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, ChunkGenerator var3, RandomSource var4, int var5, int var6, int var7, MutableBlockPos var8) {
      List var9 = a(var1);
      if (var9.isEmpty()) {
         return false;
      } else {
         Optional var10 = var1.registryAccess().registry(Registries.PLACED_FEATURE);
         if (var10.isEmpty()) {
            return false;
         } else {
            Registry var11 = (Registry)var10.get();
            ArrayList var12 = new ArrayList();

            for (BankFeature var14 : var9) {
               ArrayList var15 = new ArrayList();

               for (ResourceLocation var17 : var14.features()) {
                  var11.getOptional(var17)
                     .ifPresentOrElse(
                        var1x -> var15.add(var1x.feature().value()),
                        () -> Constants.LOG.warn("Custom bank feature references unknown placed feature {}", var17)
                     );
               }

               if (!var15.isEmpty()) {
                  var12.add(new StreamVegetationFeature.ResolvedBankFeature(var14, var15));
               }
            }

            if (var12.isEmpty()) {
               return false;
            } else {
               StreamVegetationFeature.ColumnAction var18 = (var6x, var7x, var8x) -> this.a(var1, var3, var4, var8, var6x, var7x, var12);
               return Services.PLATFORM.allWaterBanks() <= 0 ? this.a(var1, var2, var5, var6, var7, var18) : this.a(var1, var5, var6, var7, var8, var18);
            }
         }
      }
   }

   private boolean a(
      WorldGenLevel var1,
      ChunkGenerator var2,
      RandomSource var3,
      MutableBlockPos var4,
      int var5,
      int var6,
      List<StreamVegetationFeature.ResolvedBankFeature> var7
   ) {
      int var8 = var1.getHeight(Types.OCEAN_FLOOR_WG, var5, var6);
      var4.set(var5, var8, var6);
      if (!var1.getBlockState(var4).isAir()) {
         return false;
      } else {
         BlockPos var9 = var4.below();
         if (!var1.getBlockState(var9).isFaceSturdy(var1, var9, Direction.UP)) {
            return false;
         } else {
            Holder var10 = var1.getBiome(var4);
            ResourceKey var11 = (ResourceKey)var10.unwrapKey().orElse(null);
            BlockPos var12 = var4.immutable();
            boolean var13 = false;

            for (StreamVegetationFeature.ResolvedBankFeature var15 : var7) {
               if (var15.entry().matches(var10, var11) && var3.nextInt(100) < var15.entry().chance()) {
                  for (ConfiguredFeature var17 : var15.features()) {
                     try {
                        var13 |= var17.place(var1, var2, var3, var12);
                     } catch (RuntimeException var19) {
                     }
                  }
               }
            }

            return var13;
         }
      }
   }

   private boolean a(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, MutableBlockPos var4, int var5, int var6, double var7, int var9) {
      int var10 = var1.getHeight(Types.OCEAN_FLOOR_WG, var5, var6);
      var4.set(var5, var10, var6);
      if (!var1.getBlockState(var4).isAir()) {
         return false;
      } else {
         boolean var11 = false;
         if (var9 >= 2) {
            double var12 = (double)Mth.clamp(Services.PLATFORM.biomeBoostPercent(), 0, 100) / 100.0;
            if ((double)var3.nextFloat() < var12 * (1.0 - var7)) {
               var11 = this.a(var1, var2, var3, var4);
            }
         }

         double var14 = (double)Mth.clamp(Services.PLATFORM.bankGrassPercent(), 0, 100) / 100.0;
         if ((double)var3.nextFloat() < var14 * (1.0 - 0.75 * var7)
            && var1.getBlockState(var4).isAir()
            && !var1.getBiome(var4).is(StreamsWorldgen.NO_BANK_GRASS)) {
            var11 |= this.a(var1, var4, var3);
         }

         return var11;
      }
   }

   private boolean a(WorldGenLevel var1, BlockPos var2, RandomSource var3) {
      float var4 = var3.nextFloat();
      if (var4 < 0.6F) {
         return this.a(var1, var2, b);
      } else {
         return var4 < 0.8F ? this.a(var1, var2) : this.a(var1, var2, d);
      }
   }

   private boolean a(WorldGenLevel var1, BlockPos var2, BlockState var3) {
      if (var3.canSurvive(var1, var2)) {
         var1.setBlock(var2, var3, 2);
         return true;
      } else {
         return false;
      }
   }

   private boolean a(WorldGenLevel var1, BlockPos var2) {
      if (c.canSurvive(var1, var2) && var1.getBlockState(var2.above()).isAir()) {
         DoublePlantBlock.placeAt(var1, c, var2, 2);
         return true;
      } else {
         return false;
      }
   }

   private boolean a(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      BlockPos var5 = var4.below();
      if (!var1.getBlockState(var5).isFaceSturdy(var1, var5, Direction.UP)) {
         return false;
      } else {
         Holder var6 = var1.getBiome(var4);
         BiomeGenerationSettings var7 = ((Biome)var6.value()).getGenerationSettings();
         List var8 = var7.features();
         ArrayList var9 = new ArrayList();
         a(var8, Decoration.VEGETAL_DECORATION, var9);
         a(var8, Decoration.LOCAL_MODIFICATIONS, var9);
         var9.removeIf(StreamVegetationFeature::b);
         var9.removeIf(StreamVegetationFeature::a);
         if (var9.isEmpty()) {
            return false;
         } else {
            ConfiguredFeature var10 = ((PlacedFeature)((Holder)var9.get(var3.nextInt(var9.size()))).value()).feature().value();

            try {
               boolean var11 = var10.place(var1, var2, var3, var4.immutable());
               if (var11) {
                  b(var1, var4);
               }

               return var11;
            } catch (RuntimeException var12) {
               return false;
            }
         }
      }
   }

   private static boolean a(Holder<PlacedFeature> var0) {
      PlacedFeature var1 = var0.value();

      for (PlacementModifier var3 : var1.placement()) {
         if (var3 instanceof HeightRangePlacement) {
            return true;
         }
      }

      Feature var4 = var1.feature().value().feature();
      return var4 == StreamsWorldgen.STREAM_VEGETATION
         || var4 == StreamsWorldgen.RIVER_CARVER
         || var4 == Feature.TREE
         || var4 == Feature.RANDOM_SELECTOR
         || var4 == Feature.SIMPLE_RANDOM_SELECTOR;
   }

   private static void b(WorldGenLevel var0, BlockPos var1) {
      MutableBlockPos var2 = new MutableBlockPos();

      for (int var3 = -1; var3 <= 7; var3++) {
         for (int var4 = -2; var4 <= 2; var4++) {
            for (int var5 = -2; var5 <= 2; var5++) {
               var2.set(var1.getX() + var4, var1.getY() + var3, var1.getZ() + var5);
               BlockState var6 = var0.getBlockState(var2);
               if (var6.hasProperty(LeavesBlock.PERSISTENT) && !var6.getValue(LeavesBlock.PERSISTENT)) {
                  var0.setBlock(var2, var6.setValue(LeavesBlock.PERSISTENT, Boolean.valueOf(true)), 2);
               }
            }
         }
      }
   }

   private static boolean b(Holder<PlacedFeature> var0) {
      if (var0.is(StreamsWorldgen.BIOME_BOOST_EXCLUDED)) {
         return true;
      } else {
         Set var1 = a();
         Set var2 = f;
         return !var1.isEmpty() || var2 != null && !var2.isEmpty()
            ? var0.unwrapKey().map(var2x -> var1.contains(var2x.location()) || var2 != null && var2.contains(var2x.location().getNamespace())).orElse(false)
            : false;
      }
   }

   private static Set<ResourceLocation> a() {
      String var0 = Services.PLATFORM.biomeBoostBlacklist();
      if (var0 == null) {
         var0 = "";
      }

      Set var1 = e;
      if (var1 != null && var0.equals(g)) {
         return var1;
      } else {
         HashSet var2 = new HashSet();
         HashSet var3 = new HashSet();

         for (String var7 : var0.split(",")) {
            String var8 = var7.trim();
            if (!var8.isEmpty()) {
               if (var8.endsWith(":*")) {
                  var3.add(var8.substring(0, var8.length() - 2));
               } else if (var8.indexOf(58) < 0) {
                  var3.add(var8);
               } else {
                  ResourceLocation var9 = ResourceLocation.tryParse(var8);
                  if (var9 != null) {
                     var2.add(var9);
                  }
               }
            }
         }

         f = var3;
         g = var0;
         e = var2;
         return var2;
      }
   }

   private static void a(List<HolderSet<PlacedFeature>> var0, Decoration var1, List<Holder<PlacedFeature>> var2) {
      int var3 = var1.ordinal();
      if (var3 < var0.size()) {
         ((HolderSet)var0.get(var3)).forEach(var2::add);
      }
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, MutableBlockPos var3, int var4, int var5, int var6, int var7) {
      var3.set(var4, var5, var6);
      if (!var1.getBlockState(var3).is(StreamsWorldgen.UNDERWATER_VEGETATION_FLOOR)) {
         return false;
      } else {
         Holder var8 = var1.getBiome(var3);
         if (UnderwaterFloraBlacklist.isBlocked(var8)) {
            return false;
         } else {
            double var9 = Mth.clamp(((double)((Biome)var8.value()).getBaseTemperature() - 0.15) / 0.35, 0.0, 1.0);
            double var11 = Mth.clamp((Services.PLATFORM.biomeWetness(var8) - 0.1) / 0.3, 0.0, 1.0);
            int var13 = (int)Math.round((double)var7 * var9 * var11);
            if (var13 > 0 && var2.nextInt(100) < var13) {
               var3.set(var4, var5 + 1, var6);
               if (!var1.getBlockState(var3).is(Blocks.WATER)) {
                  return false;
               } else {
                  if (var1.getBlockState(var3.above()).is(Blocks.WATER) && var2.nextBoolean()) {
                     DoublePlantBlock.placeAt(var1, Blocks.TALL_SEAGRASS.defaultBlockState(), var3, 2);
                  } else {
                     var1.setBlock(var3, Blocks.SEAGRASS.defaultBlockState(), 2);
                  }

                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, RandomSource var3, int var4, int var5, int var6) {
      MutableBlockPos var7 = new MutableBlockPos();
      boolean var8 = false;

      for (int var9 = 0; var9 < 16; var9++) {
         for (int var10 = 0; var10 < 16; var10++) {
            int var11 = var4 + var9;
            int var12 = var5 + var10;
            Lake var13 = var2.lakeAt(var11, var12);
            if (var13 != null) {
               int var14 = var1.getHeight(Types.OCEAN_FLOOR, var11, var12) - 1;
               if (var14 < var13.level()) {
                  var8 |= this.a(var1, var3, var7, var11, var14, var12, var6);
               }
            }
         }
      }

      return var8;
   }

   private boolean b(WorldGenLevel var1, RiverEngine var2, RandomSource var3, int var4, int var5, int var6) {
      ColumnPlan[] var7 = ChunkPlanCache.get(var2, var4, var5);
      MutableBlockPos var8 = new MutableBlockPos();
      new MutableBlockPos();
      boolean var10 = false;

      for (int var11 = 0; var11 < 16; var11++) {
         for (int var12 = 0; var12 < 16; var12++) {
            int var13 = var4 + var11;
            int var14 = var5 + var12;
            ColumnPlan var15 = var7 != null ? var7[var11 * 16 + var12] : null;
            if (var15 == null) {
               var15 = var2.plan(var13, var14);
            }

            if (var15 != ColumnPlan.NONE
               && var15.inChannel()
               && !StreamBiomeBlacklist.isBlocked(var1.getUncachedNoiseBiome(var13 >> 2, var15.waterSurfaceY() >> 2, var14 >> 2))) {
               int var16 = var1.getHeight(Types.OCEAN_FLOOR, var13, var14) - 1;
               if (var16 < var15.waterSurfaceY()) {
                  var10 |= this.a(var1, var3, var8, var13, var16, var14, var6);
               }
            }
         }
      }

      return var10;
   }

   private boolean a(WorldGenLevel var1, RandomSource var2, int var3, int var4, int var5) {
      MutableBlockPos var6 = new MutableBlockPos();
      boolean var7 = false;

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var3 + var8;
            int var11 = var4 + var9;
            int var12 = var1.getHeight(Types.OCEAN_FLOOR, var10, var11);
            var6.set(var10, var12, var11);
            if (var1.getBlockState(var6).is(Blocks.WATER)) {
               var7 |= this.a(var1, var2, var6, var10, var12 - 1, var11, var5);
            }
         }
      }

      return var7;
   }

   @FunctionalInterface
   private interface ColumnAction {
      boolean apply(int var1, int var2, double var3);
   }

   private static record ResolvedBankFeature() {
      private final BankFeature entry;
      private final List<ConfiguredFeature<?, ?>> features;

      private ResolvedBankFeature(BankFeature var1, List<ConfiguredFeature<?, ?>> var2) {
         this.entry = var1;
         this.features = var2;
      }
   }
}
