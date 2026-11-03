package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.platform.Services;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class StreamSnowCleanupFeature extends Feature<NoneFeatureConfiguration> {
   private static final int a = 32;
   private static final int b = 12;
   private static final int c = 8;
   private static final List<Entry<TagKey<Biome>, ResourceKey<Biome>>> d = List.of(
      Map.entry(a("c", "is_swamp"), b("nomansland", "blackwater_river")),
      Map.entry(a("c", "is_jungle"), b("nomansland", "lush_river")),
      Map.entry(a("c", "is_lush"), b("nomansland", "lush_river")),
      Map.entry(a("c", "is_desert"), b("nomansland", "desert_river"))
   );
   private static final AtomicBoolean e = new AtomicBoolean(true);
   private static final AtomicBoolean f = new AtomicBoolean(true);
   private static final float[] g = new float[]{0.0F, -0.05F, 0.05F, -0.12F, 0.12F, -0.2F, 0.2F, -0.3F, 0.3F};
   private static final ResourceKey<Biome> h = b("still_life", "arctic_river");
   private static final ResourceKey<Biome> i = b("still_life", "tundra_river");
   private static final ResourceKey<Biome> j = b("still_life", "boreal_river");
   private static final ResourceKey<Biome> k = b("still_life", "temperate_river");
   private static final ResourceKey<Biome> l = b("still_life", "warm_temperate_river");
   private static final ResourceKey<Biome> m = b("still_life", "steppe_river");
   private static final ResourceKey<Biome> n = b("still_life", "mediterranean_river");
   private static final ResourceKey<Biome> o = b("still_life", "desert_river");
   private static final ResourceKey<Biome> p = b("still_life", "savanna_river");
   private static final ResourceKey<Biome> q = b("still_life", "tropical_rainforest_river");
   private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> r = Map.ofEntries(
      Map.entry(b("still_life", "arctic_beach"), h),
      Map.entry(b("still_life", "arctic_desert"), h),
      Map.entry(b("still_life", "arctic_desert_basin"), h),
      Map.entry(b("still_life", "arctic_wetlands"), h),
      Map.entry(b("still_life", "barren_arctic_desert"), h),
      Map.entry(b("still_life", "boreal_alpine_tundra"), j),
      Map.entry(b("still_life", "cold_taiga"), j),
      Map.entry(b("still_life", "cold_taiga_clearings"), j),
      Map.entry(b("still_life", "evergreen_taiga"), j),
      Map.entry(b("still_life", "fen"), j),
      Map.entry(b("still_life", "taiga_beach"), j),
      Map.entry(b("still_life", "taiga_highlands"), j),
      Map.entry(b("still_life", "arid_beach"), o),
      Map.entry(b("still_life", "arid_desert"), o),
      Map.entry(b("still_life", "arid_highlands"), o),
      Map.entry(b("still_life", "barren_peaks"), o),
      Map.entry(b("still_life", "desert_wetlands"), o),
      Map.entry(b("still_life", "semiarid_desert"), o),
      Map.entry(b("still_life", "wooded_semiarid_desert"), o),
      Map.entry(b("still_life", "xeric_shrubland"), o),
      Map.entry(b("still_life", "barren_mediterranean_highlands"), n),
      Map.entry(b("still_life", "mediterranean_beach"), n),
      Map.entry(b("still_life", "mediterranean_forest"), n),
      Map.entry(b("still_life", "mediterranean_highlands"), n),
      Map.entry(b("still_life", "mediterranean_marsh"), n),
      Map.entry(b("still_life", "mediterranean_shrubland"), n),
      Map.entry(b("still_life", "mediterranean_steppe"), n),
      Map.entry(b("still_life", "mediterranean_wooded_highlands"), n),
      Map.entry(b("still_life", "snowy_barren_peaks"), n),
      Map.entry(b("still_life", "wooded_mediterranean_steppe"), n),
      Map.entry(b("still_life", "dry_tropical_highlands"), p),
      Map.entry(b("still_life", "flooded_savanna"), p),
      Map.entry(b("still_life", "grassland_savanna"), p),
      Map.entry(b("still_life", "humid_savanna"), p),
      Map.entry(b("still_life", "savanna_beach"), p),
      Map.entry(b("still_life", "savanna_highlands"), p),
      Map.entry(b("still_life", "tropical_dry_forest"), p),
      Map.entry(b("still_life", "wooded_savanna"), p),
      Map.entry(b("still_life", "cold_barren_highlands"), m),
      Map.entry(b("still_life", "cold_desert"), m),
      Map.entry(b("still_life", "cold_semi_desert"), m),
      Map.entry(b("still_life", "cold_steppe"), m),
      Map.entry(b("still_life", "dry_cold_steppe"), m),
      Map.entry(b("still_life", "flooded_grasslands"), m),
      Map.entry(b("still_life", "shrub_steppe"), m),
      Map.entry(b("still_life", "alpine_heathlands"), k),
      Map.entry(b("still_life", "alpine_plains"), k),
      Map.entry(b("still_life", "alpine_tundra"), k),
      Map.entry(b("still_life", "birch_forest"), k),
      Map.entry(b("still_life", "larch_woodlands"), k),
      Map.entry(b("still_life", "mire"), k),
      Map.entry(b("still_life", "mixed_forest_steppe"), k),
      Map.entry(b("still_life", "old_growth_temperate_forest"), k),
      Map.entry(b("still_life", "pine_woods"), k),
      Map.entry(b("still_life", "snowy_peaks"), k),
      Map.entry(b("still_life", "temperate_beach"), k),
      Map.entry(b("still_life", "temperate_forest"), k),
      Map.entry(b("still_life", "temperate_marsh"), k),
      Map.entry(b("still_life", "temperate_mixed_forest"), k),
      Map.entry(b("still_life", "temperate_swamp"), k),
      Map.entry(b("still_life", "wooded_highlands"), k),
      Map.entry(b("still_life", "mangrove_marsh"), q),
      Map.entry(b("still_life", "monsoon_forest"), q),
      Map.entry(b("still_life", "old_growth_tropical_rainforest"), q),
      Map.entry(b("still_life", "tropical_beach"), q),
      Map.entry(b("still_life", "tropical_highlands"), q),
      Map.entry(b("still_life", "tropical_rainforest"), q),
      Map.entry(b("still_life", "tropical_wetlands"), q),
      Map.entry(b("still_life", "barren_tundra"), i),
      Map.entry(b("still_life", "bog"), i),
      Map.entry(b("still_life", "cold_tundra_beach"), i),
      Map.entry(b("still_life", "lush_tundra"), i),
      Map.entry(b("still_life", "snowy_boreal_alpine_tundra"), i),
      Map.entry(b("still_life", "snowy_tundra"), i),
      Map.entry(b("still_life", "tundra_beach"), i),
      Map.entry(b("still_life", "wooded_tundra"), i),
      Map.entry(b("still_life", "mediterranean_swamp"), l),
      Map.entry(b("still_life", "warm_temperate_clearings"), l),
      Map.entry(b("still_life", "warm_temperate_woodlands"), l)
   );
   private static final int s = 8;
   private static final int t = 6;
   private static final int u = 48;
   private static final int v = 6;
   private static final int w = 30;
   private static final int x = 12;
   private static final int y = 4;
   private static final int z = 10;
   private static final int A = 6;
   private static final int[] B = new int[]{1, -1, 0, 0, 1, 1, -1, -1};
   private static final int[] C = new int[]{0, 0, 1, -1, 1, -1, 1, -1};
   public static final ResourceKey<Biome> STREAM_BIOME = ResourceKey.create(
      Registries.BIOME, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream")
   );
   private static final AtomicBoolean D = new AtomicBoolean();

   public StreamSnowCleanupFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   private static TagKey<Biome> a(String var0, String var1) {
      return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(var0, var1));
   }

   private static ResourceKey<Biome> b(String var0, String var1) {
      return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(var0, var1));
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      } else {
         WorldGenLevel var2 = var1.level();
         RiverEngine var3 = StreamsWorldgen.engine(var2);
         ChunkPos var4 = new ChunkPos(var1.origin());
         int var5 = var4.getMinBlockX();
         int var6 = var4.getMinBlockZ();
         ColumnPlan[] var7 = ChunkPlanCache.take(var3, var5, var6);
         int var8 = RiverEngine.LEAN_CALLS.get();
         int var9 = 0;
         if (var7 != null) {
            for (ColumnPlan var13 : var7) {
               if (var13 == null) {
                  var9++;
               }
            }
         }

         boolean var31 = var3.corridorNearChunk(var5, var6, 8);
         if (!var31 && !var3.lakeNearChunk(var5, var6, 8)) {
            return false;
         } else {
            boolean var32 = false;
            int var33 = Services.PLATFORM.rebiomeStreamsMode();
            int var34 = Services.PLATFORM.rebiomeLakesMode();
            if (var33 != 0 || var34 != 0) {
               var32 |= a(var2, var3, var7, var5, var6, var33, var34);
            }

            if (Services.PLATFORM.clearFloatingSnow() && var31) {
               MutableBlockPos var14 = new MutableBlockPos();
               MutableBlockPos var15 = new MutableBlockPos();
               boolean var16 = false;

               for (int var17 = 0; var17 < 16; var17++) {
                  for (int var18 = 0; var18 < 16; var18++) {
                     int var19 = var5 + var17;
                     int var20 = var6 + var18;
                     ColumnPlan var21 = var7 != null ? var7[var17 * 16 + var18] : null;
                     if (var21 == null) {
                        var21 = var3.planLean(var19, var20);
                     }

                     if (var21 != ColumnPlan.NONE) {
                        boolean var22 = var21.inCorridor();
                        int var23 = var21.waterSurfaceY();
                        int var24 = var2.getHeight(Types.WORLD_SURFACE_WG, var19, var20) - 1;
                        int var25 = var2.getHeight(Types.WORLD_SURFACE, var19, var20) - 1;
                        int var26 = Math.max(Math.max(var24, var25), var23 + 32);

                        for (int var27 = var26; var27 >= var23 - 2; var27--) {
                           var14.set(var19, var27, var20);
                           BlockState var28 = var2.getBlockState(var14);
                           if (!var28.isAir()) {
                              if (var28.is(Blocks.SNOW) || var28.is(Blocks.SNOW_BLOCK) || var28.is(Blocks.POWDER_SNOW)) {
                                 BlockState var37 = var2.getBlockState(var15.set(var19, var27 - 1, var20));
                                 boolean var30 = var37.is(Blocks.ICE)
                                    || var37.is(Blocks.PACKED_ICE)
                                    || var37.is(Blocks.BLUE_ICE)
                                    || var37.is(Blocks.FROSTED_ICE);
                                 if (var37.isAir() || var30 && var22) {
                                    var2.setBlock(var14, Blocks.AIR.defaultBlockState(), 2);
                                    var16 = true;
                                 }
                              } else if (var28.is(Blocks.GRASS_BLOCK)) {
                                 BlockState var29 = var2.getBlockState(var15.set(var19, var27 + 1, var20));
                                 if (var29.isCollisionShapeFullBlock(var2, var15)) {
                                    var2.setBlock(var14, Blocks.DIRT.defaultBlockState(), 2);
                                    var16 = true;
                                 }
                              } else if (var28.getFluidState().isEmpty() && a(var2, var15, var19, var27, var20)) {
                                 var2.setBlock(var14, Blocks.AIR.defaultBlockState(), 2);
                                 var16 = true;
                              }
                           }
                        }
                     }
                  }
               }

               var16 |= this.a(var2, var3, var7, var5, var6);
               if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                  int var36 = RiverEngine.LEAN_CALLS.get() - var8;
                  if (var36 > 0) {
                     Constants.LOG
                        .info("[SR-SNOWPASS] chunk ({},{}): cachedArray={} nullCols={} leanQueries={}", var5 >> 4, var6 >> 4, var7 != null, var9, var36);
                  }
               }

               return var32 || var16;
            } else {
               return var32;
            }
         }
      }
   }

   private boolean a(WorldGenLevel var1, RiverEngine var2, ColumnPlan[] var3, int var4, int var5) {
      MutableBlockPos var6 = new MutableBlockPos();
      boolean var7 = false;
      int var8 = 0;
      int var9 = var4 - 16;
      int var10 = var4 + 31;
      int var11 = var5 - 16;
      int var12 = var5 + 31;

      for (int var13 = -8; var13 < 24; var13++) {
         for (int var14 = -8; var14 < 24; var14++) {
            int var15 = var4 + var13;
            int var16 = var5 + var14;
            int var17 = var1.getHeight(Types.MOTION_BLOCKING, var15, var16) - 1;
            int var18 = var1.getHeight(Types.OCEAN_FLOOR, var15, var16) - 1;
            if (var17 > var18) {
               ColumnPlan var19 = var3 != null && var13 >= 0 && var13 < 16 && var14 >= 0 && var14 < 16 ? var3[var13 * 16 + var14] : var2.planLean(var15, var16);
               if (var19 == null) {
                  var19 = var2.planLean(var15, var16);
               }

               if (var19 != ColumnPlan.NONE && var19.inChannel()) {
                  int var20 = var19.waterSurfaceY();
                  if (!var1.getBlockState(var6.set(var15, var20, var16)).getFluidState().isEmpty()
                     || !var1.getBlockState(var6.set(var15, var20 - 1, var16)).getFluidState().isEmpty()) {
                     int var21 = Integer.MIN_VALUE;

                     for (int var22 = var20 - 6; var22 <= var20 + 2; var22++) {
                        if (var1.getBlockState(var6.set(var15, var22, var16)).is(BlockTags.LOGS)) {
                           var21 = var22;
                           break;
                        }
                     }

                     if (var21 == Integer.MIN_VALUE) {
                        if (var8 < 4) {
                           for (int var26 = var20 + 1; var26 <= var20 + 12; var26++) {
                              if (var1.getBlockState(var6.set(var15, var26, var16)).is(BlockTags.LEAVES)) {
                                 if (a(var1, var15, var26, var16, var9, var10, var11, var12)) {
                                    var7 = true;
                                 }

                                 var8++;
                                 break;
                              }
                           }
                        }
                     } else {
                        List var25 = a(var1, var15, var21, var16);
                        if (var25 != null) {
                           for (BlockPos var24 : var25) {
                              var1.setBlock(var24, var24.getY() <= var20 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                           }

                           a(var1, var15, var21 + 4, var16, var9, var10, var11, var12);
                           var7 = true;
                        }
                     }
                  }
               }
            }
         }
      }

      return var7;
   }

   private static List<BlockPos> a(WorldGenLevel var0, int var1, int var2, int var3) {
      ArrayDeque var4 = new ArrayDeque();
      HashSet var5 = new HashSet();
      ArrayList var6 = new ArrayList();
      BlockPos var7 = new BlockPos(var1, var2, var3);
      var4.add(var7);
      var5.add(var7);

      while (!var4.isEmpty()) {
         BlockPos var8 = (BlockPos)var4.poll();
         if (var0.getBlockState(var8).is(BlockTags.LOGS)) {
            var6.add(var8);
            if (var6.size() > 48) {
               return null;
            }

            for (int var9 = -1; var9 <= 1; var9++) {
               for (int var10 = -1; var10 <= 1; var10++) {
                  for (int var11 = -1; var11 <= 1; var11++) {
                     if (var9 != 0 || var10 != 0 || var11 != 0) {
                        BlockPos var12 = var8.offset(var10, var9, var11);
                        if (Math.abs(var12.getX() - var1) <= 6
                           && Math.abs(var12.getZ() - var3) <= 6
                           && var12.getY() >= var2 - 2
                           && var12.getY() <= var2 + 30
                           && var5.add(var12)) {
                           var4.add(var12);
                        }
                     }
                  }
               }
            }
         }
      }

      return var6;
   }

   private static boolean a(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = var1 - 10;
      int var9 = var1 + 10;
      int var10 = var3 - 10;
      int var11 = var3 + 10;
      int var12 = var2 - 10;
      int var13 = var2 + 10;
      byte var14 = 6;
      int var15 = Math.max(var8 - var14, var4);
      int var16 = Math.min(var9 + var14, var5);
      int var17 = Math.max(var10 - var14, var6);
      int var18 = Math.min(var11 + var14, var7);
      MutableBlockPos var19 = new MutableBlockPos();
      HashSet var20 = new HashSet();
      ArrayDeque var21 = new ArrayDeque();
      HashSet var22 = new HashSet();

      for (int var23 = var12 - var14; var23 <= var13 + var14; var23++) {
         for (int var24 = var15; var24 <= var16; var24++) {
            for (int var25 = var17; var25 <= var18; var25++) {
               BlockState var26 = var0.getBlockState(var19.set(var24, var23, var25));
               if (var26.is(BlockTags.LEAVES)) {
                  var20.add(BlockPos.asLong(var24, var23, var25));
               } else if (var26.is(BlockTags.LOGS)) {
                  var21.add(new long[]{BlockPos.asLong(var24, var23, var25), 0L});
               }
            }
         }
      }

      if (var20.isEmpty()) {
         return false;
      } else {
         while (!var21.isEmpty()) {
            long[] var33 = (long[])var21.poll();
            if (var33[1] < 6L) {
               int var35 = BlockPos.getX(var33[0]);
               int var37 = BlockPos.getY(var33[0]);
               int var39 = BlockPos.getZ(var33[0]);

               for (int var27 = 0; var27 < 6; var27++) {
                  int var28 = var35 + (var27 == 0 ? 1 : (var27 == 1 ? -1 : 0));
                  int var29 = var37 + (var27 == 2 ? 1 : (var27 == 3 ? -1 : 0));
                  int var30 = var39 + (var27 == 4 ? 1 : (var27 == 5 ? -1 : 0));
                  long var31 = BlockPos.asLong(var28, var29, var30);
                  if (var20.contains(var31) && var22.add(var31)) {
                     var21.add(new long[]{var31, var33[1] + 1L});
                  }
               }
            }
         }

         boolean var34 = false;

         for (long var38 : var20) {
            if (!var22.contains(var38)) {
               int var40 = BlockPos.getX(var38);
               int var41 = BlockPos.getY(var38);
               int var42 = BlockPos.getZ(var38);
               if (var40 >= var8
                  && var40 <= var9
                  && var41 >= var12
                  && var41 <= var13
                  && var42 >= var10
                  && var42 <= var11
                  && var40 >= var15 + var14
                  && var40 <= var16 - var14
                  && var42 >= var17 + var14
                  && var42 <= var18 - var14
                  && var40 >= var4
                  && var40 <= var5
                  && var42 >= var6
                  && var42 <= var7) {
                  var19.set(var40, BlockPos.getY(var38), var42);
                  BlockState var43 = var0.getBlockState(var19);
                  if (!var43.hasProperty(LeavesBlock.PERSISTENT) || !var43.getValue(LeavesBlock.PERSISTENT)) {
                     var0.setBlock(var19, Blocks.AIR.defaultBlockState(), 2);
                     var34 = true;
                  }
               }
            }
         }

         return var34;
      }
   }

   private static boolean a(WorldGenLevel var0, RiverEngine var1, ColumnPlan[] var2, int var3, int var4, int var5, int var6) {
      boolean var7 = var5 != 0;
      boolean var8 = var6 != 0;
      Registry var9 = var0.registryAccess().registry(Registries.BIOME).orElse(null);
      if (var9 == null) {
         return false;
      } else {
         ResourceKey var10 = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "river"));
         Holder var11 = (Holder)var9.getHolder(var10).orElse(null);
         if (var11 == null) {
            return false;
         } else {
            Holder var12 = a(var0, var9, var3 + 8, var4 + 8, var0.getSeaLevel(), var11);
            Holder var13 = a(var9, STREAM_BIOME);
            Holder var14 = a(var5, var13, var12);
            Holder var15 = a(var6, var13, var12);
            if (Boolean.getBoolean("streamsreflowing.caldiag") || f.compareAndSet(true, false)) {
               Constants.LOG
                  .info(
                     "[SR-REBIOME] chunk {},{} streamMode={} lakeMode={} | picked river={} | ours={} | streamStamp={} lakeStamp={}",
                     var3,
                     var4,
                     var5,
                     var6,
                     var12.unwrapKey().map(Object::toString).orElse("?"),
                     var13 == null ? "MISSING" : var13.unwrapKey().map(Object::toString).orElse("?"),
                     var14.unwrapKey().map(Object::toString).orElse("?"),
                     var15.unwrapKey().map(Object::toString).orElse("?")
                  );
            }

            if (var13 != null) {
               StreamBiomeBridge.set((Biome)var13.value(), ((Biome)var11.value()).getMobSettings());
            }

            ChunkAccess var16 = var0.getChunk(var3 >> 4, var4 >> 4);
            boolean var17 = false;
            Holder[] var18 = new Holder[16];
            int[] var19 = new int[16];
            int[] var20 = new int[16];

            for (int var21 = 0; var21 < 4; var21++) {
               for (int var22 = 0; var22 < 4; var22++) {
                  int var23 = Integer.MAX_VALUE;
                  int var24 = Integer.MIN_VALUE;
                  int var25 = Integer.MAX_VALUE;
                  int var26 = Integer.MIN_VALUE;
                  boolean var27 = false;

                  for (int var28 = 0; var28 < 4; var28++) {
                     for (int var29 = 0; var29 < 4; var29++) {
                        int var30 = var21 * 4 + var28;
                        int var31 = var22 * 4 + var29;
                        int var32 = var3 + var30;
                        int var33 = var4 + var31;
                        int var34 = var0.getHeight(Types.MOTION_BLOCKING, var32, var33) - 1;
                        int var35 = var0.getHeight(Types.OCEAN_FLOOR, var32, var33) - 1;
                        if (var34 > var35) {
                           var27 = true;
                           if (var8) {
                              Lake var36 = var1.lakeAt(var32, var33);
                              if (var36 != null && Math.abs(var34 - var36.level()) <= 2) {
                                 var25 = Math.min(var25, var34);
                                 var26 = Math.max(var26, var34);
                                 continue;
                              }
                           }

                           if (var7) {
                              ColumnPlan var57 = var2 != null ? var2[var30 * 16 + var31] : null;
                              if (var57 == null) {
                                 var57 = var1.planLean(var32, var33);
                              }

                              if (var57 != ColumnPlan.NONE && (var57.inChannel() || Math.abs(var34 - var57.waterSurfaceY()) <= 2)) {
                                 var23 = Math.min(var23, var34);
                                 var24 = Math.max(var24, var34);
                              }
                           }
                        }
                     }
                  }

                  if (!var27) {
                     for (int var46 = 0; var46 < 4 && var23 == Integer.MAX_VALUE; var46++) {
                        for (int var50 = 0; var50 < 4 && var23 == Integer.MAX_VALUE; var50++) {
                           int var52 = var21 * 4 + var46;
                           int var54 = var22 * 4 + var50;
                           if (var7) {
                              ColumnPlan var55 = var2 != null ? var2[var52 * 16 + var54] : null;
                              if (var55 == null) {
                                 var55 = var1.planLean(var3 + var52, var4 + var54);
                              }

                              if (var55 != ColumnPlan.NONE && var55.inChannel()) {
                                 var23 = var55.waterSurfaceY();
                                 var24 = var23;
                              }
                           }

                           if (var8 && var25 == Integer.MAX_VALUE) {
                              Lake var56 = var1.lakeAt(var3 + var46 + var21 * 4, var4 + var50 + var22 * 4);
                              if (var56 != null) {
                                 var25 = var56.level();
                                 var26 = var25;
                              }
                           }
                        }
                     }
                  }

                  Holder var47 = var14;
                  int var51;
                  int var53;
                  if (var23 != Integer.MAX_VALUE) {
                     var51 = var23;
                     var53 = var24;
                  } else if (var25 != Integer.MAX_VALUE) {
                     var51 = var25;
                     var53 = var26;
                     var47 = var15;
                  } else {
                     var51 = Integer.MIN_VALUE;
                     var53 = Integer.MIN_VALUE;
                  }

                  if (var51 == Integer.MIN_VALUE && var27 && var8) {
                     var51 = a(var0, var1, var3 + var21 * 4, var4 + var22 * 4);
                     var53 = var51;
                     var47 = var15;
                  }

                  if (var51 != Integer.MIN_VALUE) {
                     var18[var21 * 4 + var22] = var47;
                     var19[var21 * 4 + var22] = var51;
                     var20[var21 * 4 + var22] = var53;
                  }
               }
            }

            for (int var37 = 0; var37 < 4; var37++) {
               for (int var38 = 0; var38 < 4; var38++) {
                  Holder var39 = var18[var37 * 4 + var38];
                  int var40;
                  int var41;
                  if (var39 != null) {
                     var40 = var19[var37 * 4 + var38];
                     var41 = var20[var37 * 4 + var38];
                  } else {
                     var40 = Integer.MAX_VALUE;
                     var41 = Integer.MIN_VALUE;

                     for (int var42 = Math.max(0, var37 - 1); var42 <= Math.min(3, var37 + 1); var42++) {
                        for (int var44 = Math.max(0, var38 - 1); var44 <= Math.min(3, var38 + 1); var44++) {
                           Holder var48 = var18[var42 * 4 + var44];
                           if (var48 != null) {
                              var39 = var48;
                              var40 = Math.min(var40, var19[var42 * 4 + var44]);
                              var41 = Math.max(var41, var20[var42 * 4 + var44]);
                           }
                        }
                     }

                     if (var39 == null) {
                        continue;
                     }
                  }

                  int var43 = var40 - 12;
                  int var45 = var41 + 8;

                  for (int var49 = var43 & -4; var49 <= var45; var49 += 4) {
                     if (a(var16, var37, var49, var38, var39)) {
                        var17 = true;
                     }
                  }
               }
            }

            return var17;
         }
      }
   }

   public static void tintMap(ServerLevel var0, int var1, int var2) {
      RiverEngine var3 = StreamsWorldgen.engine(var0);
      if (var3 == null) {
         Constants.LOG.info("[SR-TINTMAP] no engine (rivers off?)");
      } else {
         Registry var4 = var0.registryAccess().registry(Registries.BIOME).orElse(null);
         int var5 = (var1 >> 4) - 1 << 4;
         int var6 = (var2 >> 4) - 1 << 4;
         int var7 = 0;
         int var8 = 0;
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;
         int var12 = 0;
         StringBuilder var13 = new StringBuilder();
         StringBuilder[] var14 = new StringBuilder[12];

         for (int var15 = 0; var15 < 12; var15++) {
            var14[var15] = new StringBuilder();
         }

         for (int var36 = 0; var36 < 12; var36++) {
            for (int var16 = 0; var16 < 12; var16++) {
               char var17 = '.';

               for (int var18 = 0; var18 < 4 && var17 == '.'; var18++) {
                  for (int var19 = 0; var19 < 4 && var17 == '.'; var19++) {
                     int var20 = var5 + var16 * 4 + var18;
                     int var21 = var6 + var36 * 4 + var19;
                     int var22 = var0.getHeight(Types.MOTION_BLOCKING, var20, var21) - 1;
                     int var23 = var0.getHeight(Types.OCEAN_FLOOR, var20, var21) - 1;
                     if (var22 > var23) {
                        BlockPos var24 = new BlockPos(var20, var22, var21);
                        Holder var25 = var0.getBiome(var24);
                        boolean var26 = var25.is(STREAM_BIOME) || var25.is(BiomeTags.IS_RIVER);
                        Lake var27 = var3.lakeAt(var20, var21);
                        boolean var28 = var27 != null && Math.abs(var22 - var27.level()) <= 2;
                        ColumnPlan var29 = var3.planLean(var20, var21);
                        boolean var30 = var29 != ColumnPlan.NONE && var29.inChannel();
                        int var31 = var29 != ColumnPlan.NONE ? Math.abs(var22 - var29.waterSurfaceY()) : -1;
                        boolean var32 = var29 != ColumnPlan.NONE && (var30 || var31 <= 2);
                        boolean var33 = !var28 && !var32 && a(var0, var3, var20 & -4, var21 & -4) != Integer.MIN_VALUE;
                        boolean var34 = var28 || var32 || var33;
                        if (var26) {
                           var10++;
                           var17 = 'S';
                        } else if (var34) {
                           var12++;
                           var17 = 'X';
                           boolean var35 = var0.getBiome(var24.above(4)).is(STREAM_BIOME) || var0.getBiome(var24.below(4)).is(STREAM_BIOME);
                           if (var35) {
                              var9++;
                              var17 = 'v';
                           }

                           if (var13.length() < 900) {
                              var13.append(
                                 String.format(
                                    " (%d,%d,%d %s plan=%s chan=%b d=%d lake=%b rim=%b)",
                                    var20,
                                    var22,
                                    var21,
                                    var4 != null ? var25.unwrapKey().map(var0x -> var0x.location().getPath()).orElse("?") : "?",
                                    var29 == ColumnPlan.NONE ? "NONE" : "yes",
                                    var30,
                                    var31,
                                    var28,
                                    var33
                                 )
                              );
                           }
                        } else {
                           var11++;
                           var17 = 'o';
                           if (var29 == ColumnPlan.NONE) {
                              var7++;
                           } else {
                              var8++;
                           }
                        }
                     }
                  }
               }

               var14[var36].append(var17);
            }
         }

         Constants.LOG.info("[SR-TINTMAP] 3x3 chunks at ({},{}) cellgrid (N=up, 4-block cells):", var1, var2);

         for (int var37 = 0; var37 < 12; var37++) {
            Constants.LOG.info("[SR-TINTMAP]   {}", var14[var37]);
         }

         Constants.LOG
            .info(
               "[SR-TINTMAP] watered cells: stampedOK={} SHOULD-STAMP-BUT-LAND={} (of which vertical-miss={}) correctly-skipped={} (planNONE={} corridorDeltaFail={})",
               var10,
               var12,
               var9,
               var11,
               var7,
               var8
            );
         if (var13.length() > 0) {
            Constants.LOG.info("[SR-TINTMAP] samples:{}", var13);
         }
      }
   }

   private static int a(WorldGenLevel var0, RiverEngine var1, int var2, int var3) {
      int var4 = Integer.MIN_VALUE;

      for (int var5 = 0; var5 < 4; var5++) {
         int var6 = var2 + (var5 & 1) * 3;
         int var7 = var3 + (var5 >> 1 & 1) * 3;
         int var8 = var0.getHeight(Types.MOTION_BLOCKING, var6, var7) - 1;
         int var9 = var0.getHeight(Types.OCEAN_FLOOR, var6, var7) - 1;
         if (var8 > var9) {
            var4 = var8;
            break;
         }
      }

      if (var4 == Integer.MIN_VALUE) {
         return Integer.MIN_VALUE;
      } else {
         int var10 = 2 + var1.config().gridSpacing() / 2;

         for (int var11 = 0; var11 < 8; var11++) {
            Lake var12 = var1.lakeAt(var2 + 1 + B[var11] * var10, var3 + 1 + C[var11] * var10);
            if (var12 != null && Math.abs(var4 - var12.level()) <= 2) {
               return var12.level();
            }
         }

         return Integer.MIN_VALUE;
      }
   }

   private static Holder<Biome> a(int var0, Holder<Biome> var1, Holder<Biome> var2) {
      return var0 == 2 && var1 != null ? var1 : var2;
   }

   private static Holder<Biome> a(WorldGenLevel var0, Registry<Biome> var1, int var2, int var3, int var4, Holder<Biome> var5) {
      Holder var6 = var0.getUncachedNoiseBiome(var2 >> 2, var4 >> 2, var3 >> 2);
      boolean var7 = Boolean.getBoolean("streamsreflowing.caldiag") || e.compareAndSet(true, false);

      try {
         BiomeSource var8 = var0.getLevel().getChunkSource().getGenerator().getBiomeSource();
         if (var8 instanceof MultiNoiseBiomeSource var9) {
            Sampler var10 = var0.getLevel().getChunkSource().randomState().sampler();
            TargetPoint var11 = var10.sample(var2 >> 2, var4 >> 2, var3 >> 2);

            for (float var15 : g) {
               TargetPoint var16 = new TargetPoint(
                  var11.temperature(), var11.humidity(), var11.continentalness(), var11.erosion(), Climate.quantizeCoord(0.0F), Climate.quantizeCoord(var15)
               );
               Holder var17 = var9.getNoiseBiome(var16);
               if (var7) {
                  Constants.LOG
                     .info(
                        "[SR-RIVERPICK] at {},{} weirdness={} -> {} (river={})",
                        var2,
                        var3,
                        var15,
                        var17 == null ? "null" : var17.unwrapKey().map(Object::toString).orElse("?"),
                        var17 != null && var17.is(BiomeTags.IS_RIVER)
                     );
               }

               if (var17 != null && var17.is(BiomeTags.IS_RIVER)) {
                  return var17;
               }
            }

            if (var7) {
               Constants.LOG
                  .info(
                     "[SR-RIVERPICK] at {},{}: no river-tagged biome from any valley probe (source={}) -> falling through to the tables",
                     var2,
                     var3,
                     var8.getClass().getName()
                  );
            }
         } else if (var7) {
            Constants.LOG.info("[SR-RIVERPICK] at {},{}: biome source is {} (not MultiNoise) -> tables", var2, var3, var8.getClass().getName());
         }
      } catch (Throwable var18) {
         if (var7) {
            Constants.LOG.info("[SR-RIVERPICK] at {},{}: probe failed ({})", var2, var3, var18.toString());
         }
      }

      ResourceKey var19 = var6.unwrapKey().map(r::get).orElse(null);
      if (var19 != null) {
         Holder var20 = a(var1, var19);
         if (var20 != null) {
            return var20;
         }
      }

      for (Entry var22 : d) {
         if (var6.is((TagKey)var22.getKey())) {
            Holder var23 = a(var1, (ResourceKey<Biome>)var22.getValue());
            if (var23 != null) {
               return var23;
            }
         }
      }

      return var5;
   }

   private static Holder<Biome> a(Registry<Biome> var0, ResourceKey<Biome> var1) {
      return var0.getHolder(var1).map(var0x -> (Holder<Biome>)var0x).orElse(null);
   }

   private static boolean a(ChunkAccess var0, int var1, int var2, int var3, Holder<Biome> var4) {
      int var5 = var0.getSectionIndex(var2);
      if (var5 >= 0 && var5 < var0.getSections().length) {
         LevelChunkSection var6 = var0.getSection(var5);
         PalettedContainer var7 = (PalettedContainer)var6.getBiomes();

         try {
            var7.getAndSet(var1, var2 >> 2 & 3, var3, var4);
            return true;
         } catch (RuntimeException var9) {
            if (D.compareAndSet(false, true)) {
               Constants.LOG
                  .warn(
                     "Streams Reflowing: this world's chunk format doesn't accept biome relabeling; stream and lake areas keep their original biome names (cosmetic only). ({})",
                     var9.toString()
                  );
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      return var0.getBlockState(var1.set(var2, var3 - 1, var4)).isAir()
         && var0.getBlockState(var1.set(var2, var3 + 1, var4)).isAir()
         && var0.getBlockState(var1.set(var2 - 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2 + 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 - 1)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 + 1)).isAir();
   }
}
