/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.ChunkPlanCache;
import dev.streamsreflowing.worldgen.StreamBiomeBridge;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
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
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class StreamSnowCleanupFeature
extends Feature<NoneFeatureConfiguration> {
    private static final int a = 32;
    private static final int b = 12;
    private static final int c = 8;
    private static final List<Map.Entry<TagKey<Biome>, ResourceKey<Biome>>> d = List.of(Map.entry(StreamSnowCleanupFeature.a("c", "is_swamp"), StreamSnowCleanupFeature.b("nomansland", "blackwater_river")), Map.entry(StreamSnowCleanupFeature.a("c", "is_jungle"), StreamSnowCleanupFeature.b("nomansland", "lush_river")), Map.entry(StreamSnowCleanupFeature.a("c", "is_lush"), StreamSnowCleanupFeature.b("nomansland", "lush_river")), Map.entry(StreamSnowCleanupFeature.a("c", "is_desert"), StreamSnowCleanupFeature.b("nomansland", "desert_river")));
    private static final AtomicBoolean e = new AtomicBoolean(true);
    private static final AtomicBoolean f = new AtomicBoolean(true);
    private static final float[] g = new float[]{0.0f, -0.05f, 0.05f, -0.12f, 0.12f, -0.2f, 0.2f, -0.3f, 0.3f};
    private static final ResourceKey<Biome> h = StreamSnowCleanupFeature.b("still_life", "arctic_river");
    private static final ResourceKey<Biome> i = StreamSnowCleanupFeature.b("still_life", "tundra_river");
    private static final ResourceKey<Biome> j = StreamSnowCleanupFeature.b("still_life", "boreal_river");
    private static final ResourceKey<Biome> k = StreamSnowCleanupFeature.b("still_life", "temperate_river");
    private static final ResourceKey<Biome> l = StreamSnowCleanupFeature.b("still_life", "warm_temperate_river");
    private static final ResourceKey<Biome> m = StreamSnowCleanupFeature.b("still_life", "steppe_river");
    private static final ResourceKey<Biome> n = StreamSnowCleanupFeature.b("still_life", "mediterranean_river");
    private static final ResourceKey<Biome> o = StreamSnowCleanupFeature.b("still_life", "desert_river");
    private static final ResourceKey<Biome> p = StreamSnowCleanupFeature.b("still_life", "savanna_river");
    private static final ResourceKey<Biome> q = StreamSnowCleanupFeature.b("still_life", "tropical_rainforest_river");
    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> r = Map.ofEntries(Map.entry(StreamSnowCleanupFeature.b("still_life", "arctic_beach"), h), Map.entry(StreamSnowCleanupFeature.b("still_life", "arctic_desert"), h), Map.entry(StreamSnowCleanupFeature.b("still_life", "arctic_desert_basin"), h), Map.entry(StreamSnowCleanupFeature.b("still_life", "arctic_wetlands"), h), Map.entry(StreamSnowCleanupFeature.b("still_life", "barren_arctic_desert"), h), Map.entry(StreamSnowCleanupFeature.b("still_life", "boreal_alpine_tundra"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_taiga"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_taiga_clearings"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "evergreen_taiga"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "fen"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "taiga_beach"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "taiga_highlands"), j), Map.entry(StreamSnowCleanupFeature.b("still_life", "arid_beach"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "arid_desert"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "arid_highlands"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "barren_peaks"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "desert_wetlands"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "semiarid_desert"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "wooded_semiarid_desert"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "xeric_shrubland"), o), Map.entry(StreamSnowCleanupFeature.b("still_life", "barren_mediterranean_highlands"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_beach"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_forest"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_highlands"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_marsh"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_shrubland"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_steppe"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_wooded_highlands"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "snowy_barren_peaks"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "wooded_mediterranean_steppe"), n), Map.entry(StreamSnowCleanupFeature.b("still_life", "dry_tropical_highlands"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "flooded_savanna"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "grassland_savanna"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "humid_savanna"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "savanna_beach"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "savanna_highlands"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "tropical_dry_forest"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "wooded_savanna"), p), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_barren_highlands"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_desert"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_semi_desert"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_steppe"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "dry_cold_steppe"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "flooded_grasslands"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "shrub_steppe"), m), Map.entry(StreamSnowCleanupFeature.b("still_life", "alpine_heathlands"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "alpine_plains"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "alpine_tundra"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "birch_forest"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "larch_woodlands"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "mire"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "mixed_forest_steppe"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "old_growth_temperate_forest"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "pine_woods"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "snowy_peaks"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "temperate_beach"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "temperate_forest"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "temperate_marsh"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "temperate_mixed_forest"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "temperate_swamp"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "wooded_highlands"), k), Map.entry(StreamSnowCleanupFeature.b("still_life", "mangrove_marsh"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "monsoon_forest"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "old_growth_tropical_rainforest"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "tropical_beach"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "tropical_highlands"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "tropical_rainforest"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "tropical_wetlands"), q), Map.entry(StreamSnowCleanupFeature.b("still_life", "barren_tundra"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "bog"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "cold_tundra_beach"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "lush_tundra"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "snowy_boreal_alpine_tundra"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "snowy_tundra"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "tundra_beach"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "wooded_tundra"), i), Map.entry(StreamSnowCleanupFeature.b("still_life", "mediterranean_swamp"), l), Map.entry(StreamSnowCleanupFeature.b("still_life", "warm_temperate_clearings"), l), Map.entry(StreamSnowCleanupFeature.b("still_life", "warm_temperate_woodlands"), l));
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
    public static final ResourceKey<Biome> STREAM_BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream"));
    private static final AtomicBoolean D = new AtomicBoolean();

    public StreamSnowCleanupFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static TagKey<Biome> a(String string, String string2) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(string, string2));
    }

    private static ResourceKey<Biome> b(String string, String string2) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(string, string2));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        int n;
        boolean bl;
        if (!Services.PLATFORM.generateRivers()) {
            return false;
        }
        WorldGenLevel worldGenLevel = featurePlaceContext.level();
        RiverEngine riverEngine = StreamsWorldgen.engine(worldGenLevel);
        ChunkPos chunkPos = new ChunkPos(featurePlaceContext.origin());
        int n2 = chunkPos.getMinBlockX();
        int n3 = chunkPos.getMinBlockZ();
        ColumnPlan[] columnPlanArray = ChunkPlanCache.take(riverEngine, n2, n3);
        int n4 = RiverEngine.LEAN_CALLS.get();
        int n5 = 0;
        if (columnPlanArray != null) {
            for (ColumnPlan columnPlan : columnPlanArray) {
                if (columnPlan != null) continue;
                ++n5;
            }
        }
        if (!(bl = riverEngine.corridorNearChunk(n2, n3, 8)) && !riverEngine.lakeNearChunk(n2, n3, 8)) {
            return false;
        }
        int n6 = 0;
        int n7 = Services.PLATFORM.rebiomeStreamsMode();
        int n8 = Services.PLATFORM.rebiomeLakesMode();
        if (n7 != 0 || n8 != 0) {
            if (StreamSnowCleanupFeature.a(worldGenLevel, riverEngine, columnPlanArray, n2, n3, n7, n8)) {
                n6 = 1;
            }
        }
        if (!Services.PLATFORM.clearFloatingSnow() || !bl) {
            return n6 != 0;
        }
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        boolean bl2 = false;
        for (n = 0; n < 16; ++n) {
            for (int i = 0; i < 16; ++i) {
                int n9;
                ColumnPlan columnPlan;
                int n10 = n2 + n;
                int n11 = n3 + i;
                ColumnPlan columnPlan2 = columnPlan = columnPlanArray != null ? columnPlanArray[n * 16 + i] : null;
                if (columnPlan == null) {
                    columnPlan = riverEngine.planLean(n10, n11);
                }
                if (columnPlan == ColumnPlan.NONE) continue;
                boolean bl3 = columnPlan.inCorridor();
                int n12 = columnPlan.waterSurfaceY();
                int n13 = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, n10, n11) - 1;
                int n14 = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE, n10, n11) - 1;
                for (int j = n9 = Math.max(Math.max(n13, n14), n12 + 32); j >= n12 - 2; --j) {
                    BlockState blockState;
                    mutableBlockPos.set(n10, j, n11);
                    BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos);
                    if (blockState2.isAir()) continue;
                    if (blockState2.is(Blocks.SNOW) || blockState2.is(Blocks.SNOW_BLOCK) || blockState2.is(Blocks.POWDER_SNOW)) {
                        boolean bl4;
                        blockState = worldGenLevel.getBlockState(mutableBlockPos2.set(n10, j - 1, n11));
                        boolean bl5 = bl4 = blockState.is(Blocks.ICE) || blockState.is(Blocks.PACKED_ICE) || blockState.is(Blocks.BLUE_ICE) || blockState.is(Blocks.FROSTED_ICE);
                        if (!blockState.isAir() && (!bl4 || !bl3)) continue;
                        worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
                        bl2 = true;
                        continue;
                    }
                    if (blockState2.is(Blocks.GRASS_BLOCK)) {
                        blockState = worldGenLevel.getBlockState(mutableBlockPos2.set(n10, j + 1, n11));
                        if (!blockState.isCollisionShapeFullBlock(worldGenLevel, mutableBlockPos2)) continue;
                        worldGenLevel.setBlock(mutableBlockPos, Blocks.DIRT.defaultBlockState(), 2);
                        bl2 = true;
                        continue;
                    }
                    if (!blockState2.getFluidState().isEmpty() || !StreamSnowCleanupFeature.a(worldGenLevel, mutableBlockPos2, n10, j, n11)) continue;
                    worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
                    bl2 = true;
                }
            }
        }
        bl2 |= this.a(worldGenLevel, riverEngine, columnPlanArray, n2, n3);
        if (Boolean.getBoolean("streamsreflowing.caldiag") && (n = RiverEngine.LEAN_CALLS.get() - n4) > 0) {
            Constants.LOG.info("[SR-SNOWPASS] chunk ({},{}): cachedArray={} nullCols={} leanQueries={}", n2 >> 4, n3 >> 4, columnPlanArray != null, n5, n);
        }
        return n6 != 0 || bl2;
    }

    private boolean a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, ColumnPlan[] columnPlanArray, int n, int n2) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl = false;
        int n3 = 0;
        int n4 = n - 16;
        int n5 = n + 31;
        int n6 = n2 - 16;
        int n7 = n2 + 31;
        for (int i = -8; i < 24; ++i) {
            block1: for (int j = -8; j < 24; ++j) {
                int n8;
                int n9;
                ColumnPlan columnPlan;
                int n10;
                int n11 = n + i;
                int n12 = n2 + j;
                int n13 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, n11, n12) - 1;
                if (n13 <= (n10 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n11, n12) - 1)) continue;
                ColumnPlan columnPlan2 = columnPlan = columnPlanArray != null && i >= 0 && i < 16 && j >= 0 && j < 16 ? columnPlanArray[i * 16 + j] : riverEngine.planLean(n11, n12);
                if (columnPlan == null) {
                    columnPlan = riverEngine.planLean(n11, n12);
                }
                if (columnPlan == ColumnPlan.NONE || !columnPlan.inChannel() || worldGenLevel.getBlockState(mutableBlockPos.set(n11, n9 = columnPlan.waterSurfaceY(), n12)).getFluidState().isEmpty() && worldGenLevel.getBlockState(mutableBlockPos.set(n11, n9 - 1, n12)).getFluidState().isEmpty()) continue;
                int n14 = Integer.MIN_VALUE;
                for (n8 = n9 - 6; n8 <= n9 + 2; ++n8) {
                    if (!worldGenLevel.getBlockState(mutableBlockPos.set(n11, n8, n12)).is(BlockTags.LOGS)) continue;
                    n14 = n8;
                    break;
                }
                if (n14 != Integer.MIN_VALUE) {
                    List<BlockPos> list = StreamSnowCleanupFeature.a(worldGenLevel, n11, n14, n12);
                    if (list == null) continue;
                    for (BlockPos blockPos : list) {
                        worldGenLevel.setBlock(blockPos, blockPos.getY() <= n9 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                    }
                    StreamSnowCleanupFeature.a(worldGenLevel, n11, n14 + 4, n12, n4, n5, n6, n7);
                    bl = true;
                    continue;
                }
                if (n3 >= 4) continue;
                for (n8 = n9 + 1; n8 <= n9 + 12; ++n8) {
                    if (!worldGenLevel.getBlockState(mutableBlockPos.set(n11, n8, n12)).is(BlockTags.LEAVES)) continue;
                    if (StreamSnowCleanupFeature.a(worldGenLevel, n11, n8, n12, n4, n5, n6, n7)) {
                        bl = true;
                    }
                    ++n3;
                    continue block1;
                }
            }
        }
        return bl;
    }

    private static List<BlockPos> a(WorldGenLevel worldGenLevel, int n, int n2, int n3) {
        ArrayDeque<BlockPos> arrayDeque = new ArrayDeque<BlockPos>();
        HashSet<BlockPos> hashSet = new HashSet<BlockPos>();
        ArrayList<BlockPos> arrayList = new ArrayList<BlockPos>();
        BlockPos blockPos = new BlockPos(n, n2, n3);
        arrayDeque.add(blockPos);
        hashSet.add(blockPos);
        while (!arrayDeque.isEmpty()) {
            BlockPos blockPos2 = (BlockPos)arrayDeque.poll();
            if (!worldGenLevel.getBlockState(blockPos2).is(BlockTags.LOGS)) continue;
            arrayList.add(blockPos2);
            if (arrayList.size() > 48) {
                return null;
            }
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    for (int k = -1; k <= 1; ++k) {
                        BlockPos blockPos3;
                        if (i == 0 && j == 0 && k == 0 || Math.abs((blockPos3 = blockPos2.offset(j, i, k)).getX() - n) > 6 || Math.abs(blockPos3.getZ() - n3) > 6 || blockPos3.getY() < n2 - 2 || blockPos3.getY() > n2 + 30 || !hashSet.add(blockPos3)) continue;
                        arrayDeque.add(blockPos3);
                    }
                }
            }
        }
        return arrayList;
    }

    private static boolean a(WorldGenLevel worldGenLevel, int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n14 = n - 10;
        int n15 = n + 10;
        int n16 = n3 - 10;
        int n17 = n3 + 10;
        int n18 = n2 - 10;
        int n19 = n2 + 10;
        int n20 = 6;
        int n21 = Math.max(n14 - n20, n4);
        int n22 = Math.min(n15 + n20, n5);
        int n23 = Math.max(n16 - n20, n6);
        int n24 = Math.min(n17 + n20, n7);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        HashSet<Long> hashSet = new HashSet<Long>();
        ArrayDeque<long[]> arrayDeque = new ArrayDeque<long[]>();
        HashSet<Long> hashSet2 = new HashSet<Long>();
        for (n13 = n18 - n20; n13 <= n19 + n20; ++n13) {
            for (n12 = n21; n12 <= n22; ++n12) {
                for (n11 = n23; n11 <= n24; ++n11) {
                    BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos.set(n12, n13, n11));
                    if (blockState.is(BlockTags.LEAVES)) {
                        hashSet.add(BlockPos.asLong(n12, n13, n11));
                        continue;
                    }
                    if (!blockState.is(BlockTags.LOGS)) continue;
                    arrayDeque.add(new long[]{BlockPos.asLong(n12, n13, n11), 0L});
                }
            }
        }
        if (hashSet.isEmpty()) {
            return false;
        }
        while (!arrayDeque.isEmpty()) {
            long[] lArray = (long[])arrayDeque.poll();
            if (lArray[1] >= 6L) continue;
            n12 = BlockPos.getX(lArray[0]);
            n11 = BlockPos.getY(lArray[0]);
            int n25 = BlockPos.getZ(lArray[0]);
            for (n10 = 0; n10 < 6; ++n10) {
                int n26;
                n9 = n12 + (n10 == 0 ? 1 : (n10 == 1 ? -1 : 0));
                long l = BlockPos.asLong(n9, n8 = n11 + (n10 == 2 ? 1 : (n10 == 3 ? -1 : 0)), n26 = n25 + (n10 == 4 ? 1 : (n10 == 5 ? -1 : 0)));
                if (!hashSet.contains(l) || !hashSet2.add(l)) continue;
                arrayDeque.add(new long[]{l, lArray[1] + 1L});
            }
        }
        n13 = 0;
        Iterator iterator = hashSet.iterator();
        while (iterator.hasNext()) {
            long l = (Long)iterator.next();
            if (hashSet2.contains(l)) continue;
            n10 = BlockPos.getX(l);
            n9 = BlockPos.getY(l);
            n8 = BlockPos.getZ(l);
            if (n10 < n14 || n10 > n15 || n9 < n18 || n9 > n19 || n8 < n16 || n8 > n17 || n10 < n21 + n20 || n10 > n22 - n20 || n8 < n23 + n20 || n8 > n24 - n20 || n10 < n4 || n10 > n5 || n8 < n6 || n8 > n7) continue;
            mutableBlockPos.set(n10, BlockPos.getY(l), n8);
            BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
            if (blockState.hasProperty(LeavesBlock.PERSISTENT) && blockState.getValue(LeavesBlock.PERSISTENT).booleanValue()) continue;
            worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
            n13 = 1;
        }
        return n13 != 0;
    }

    private static boolean a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, ColumnPlan[] columnPlanArray, int n, int n2, int n3, int n4) {
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        boolean bl = n3 != 0;
        boolean bl2 = n4 != 0;
        Registry registry = worldGenLevel.registryAccess().registry(Registries.BIOME).orElse(null);
        if (registry == null) {
            return false;
        }
        ResourceKey<Biome> resourceKey = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "river"));
        Holder<Biome> holder = (Holder<Biome>)registry.getHolder(resourceKey).orElse(null);
        if (holder == null) {
            return false;
        }
        Holder<Biome> holder2 = StreamSnowCleanupFeature.a(worldGenLevel, registry, n + 8, n2 + 8, worldGenLevel.getSeaLevel(), holder);
        Holder<Biome> holder3 = StreamSnowCleanupFeature.a(registry, STREAM_BIOME);
        Holder<Biome> holder4 = StreamSnowCleanupFeature.a(n3, holder3, holder2);
        Holder<Biome> holder5 = StreamSnowCleanupFeature.a(n4, holder3, holder2);
        if (Boolean.getBoolean("streamsreflowing.caldiag") || f.compareAndSet(true, false)) {
            Constants.LOG.info("[SR-REBIOME] chunk {},{} streamMode={} lakeMode={} | picked river={} | ours={} | streamStamp={} lakeStamp={}", n, n2, n3, n4, holder2.unwrapKey().map(Object::toString).orElse("?"), holder3 == null ? "MISSING" : holder3.unwrapKey().map(Object::toString).orElse("?"), holder4.unwrapKey().map(Object::toString).orElse("?"), holder5.unwrapKey().map(Object::toString).orElse("?"));
        }
        if (holder3 != null) {
            StreamBiomeBridge.set(holder3.value(), ((Biome)holder.value()).getMobSettings());
        }
        ChunkAccess chunkAccess = worldGenLevel.getChunk(n >> 4, n2 >> 4);
        boolean bl3 = false;
        Holder[] holderArray = new Holder[16];
        int[] nArray = new int[16];
        int[] nArray2 = new int[16];
        for (n10 = 0; n10 < 4; ++n10) {
            for (n9 = 0; n9 < 4; ++n9) {
                int n11;
                int n12;
                int n13;
                int n14;
                int n15 = Integer.MAX_VALUE;
                n8 = Integer.MIN_VALUE;
                n7 = Integer.MAX_VALUE;
                n6 = Integer.MIN_VALUE;
                n5 = 0;
                for (n14 = 0; n14 < 4; ++n14) {
                    for (n13 = 0; n13 < 4; ++n13) {
                        Object object;
                        int n16;
                        n12 = n10 * 4 + n14;
                        int n17 = n + n12;
                        n11 = n9 * 4 + n13;
                        int n18 = n2 + n11;
                        int n19 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, n17, n18) - 1;
                        if (n19 <= (n16 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n17, n18) - 1)) continue;
                        n5 = 1;
                        if (bl2 && (object = riverEngine.lakeAt(n17, n18)) != null && Math.abs(n19 - ((Lake)object).level()) <= 2) {
                            n7 = Math.min(n7, n19);
                            n6 = Math.max(n6, n19);
                            continue;
                        }
                        if (!bl) continue;
                        Object object2 = object = columnPlanArray != null ? columnPlanArray[n12 * 16 + n11] : null;
                        if (object == null) {
                            object = riverEngine.planLean(n17, n18);
                        }
                        if (object == ColumnPlan.NONE || !((ColumnPlan)object).inChannel() && Math.abs(n19 - ((ColumnPlan)object).waterSurfaceY()) > 2) continue;
                        n15 = Math.min(n15, n19);
                        n8 = Math.max(n8, n19);
                    }
                }
                if (n5 == 0) {
                    for (n14 = 0; n14 < 4 && n15 == Integer.MAX_VALUE; ++n14) {
                        for (n13 = 0; n13 < 4 && n15 == Integer.MAX_VALUE; ++n13) {
                            Object object;
                            n12 = n10 * 4 + n14;
                            n11 = n9 * 4 + n13;
                            if (bl) {
                                Object object3 = object = columnPlanArray != null ? columnPlanArray[n12 * 16 + n11] : null;
                                if (object == null) {
                                    object = riverEngine.planLean(n + n12, n2 + n11);
                                }
                                if (object != ColumnPlan.NONE && ((ColumnPlan)object).inChannel()) {
                                    n8 = n15 = ((ColumnPlan)object).waterSurfaceY();
                                }
                            }
                            if (!bl2 || n7 != Integer.MAX_VALUE || (object = riverEngine.lakeAt(n + n14 + n10 * 4, n2 + n13 + n9 * 4)) == null) continue;
                            n6 = n7 = ((Lake)object).level();
                        }
                    }
                }
                Holder<Biome> holder6 = holder4;
                if (n15 != Integer.MAX_VALUE) {
                    n13 = n15;
                    n12 = n8;
                } else if (n7 != Integer.MAX_VALUE) {
                    n13 = n7;
                    n12 = n6;
                    holder6 = holder5;
                } else {
                    n13 = Integer.MIN_VALUE;
                    n12 = Integer.MIN_VALUE;
                }
                if (n13 == Integer.MIN_VALUE && n5 != 0 && bl2) {
                    n12 = n13 = StreamSnowCleanupFeature.a(worldGenLevel, riverEngine, n + n10 * 4, n2 + n9 * 4);
                    holder6 = holder5;
                }
                if (n13 == Integer.MIN_VALUE) continue;
                holderArray[n10 * 4 + n9] = holder6;
                nArray[n10 * 4 + n9] = n13;
                nArray2[n10 * 4 + n9] = n12;
            }
        }
        for (n10 = 0; n10 < 4; ++n10) {
            for (n9 = 0; n9 < 4; ++n9) {
                Holder holder7 = holderArray[n10 * 4 + n9];
                if (holder7 != null) {
                    n8 = nArray[n10 * 4 + n9];
                    n7 = nArray2[n10 * 4 + n9];
                } else {
                    n8 = Integer.MAX_VALUE;
                    n7 = Integer.MIN_VALUE;
                    for (n6 = Math.max(0, n10 - 1); n6 <= Math.min(3, n10 + 1); ++n6) {
                        for (n5 = Math.max(0, n9 - 1); n5 <= Math.min(3, n9 + 1); ++n5) {
                            Holder holder8 = holderArray[n6 * 4 + n5];
                            if (holder8 == null) continue;
                            holder7 = holder8;
                            n8 = Math.min(n8, nArray[n6 * 4 + n5]);
                            n7 = Math.max(n7, nArray2[n6 * 4 + n5]);
                        }
                    }
                    if (holder7 == null) continue;
                }
                n6 = n8 - 12;
                n5 = n7 + 8;
                for (int i = n6 & 0xFFFFFFFC; i <= n5; i += 4) {
                    if (!StreamSnowCleanupFeature.a(chunkAccess, n10, i, n9, holder7)) continue;
                    bl3 = true;
                }
            }
        }
        return bl3;
    }

    public static void tintMap(ServerLevel serverLevel, int n, int n2) {
        int n3;
        RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
        if (riverEngine == null) {
            Constants.LOG.info("[SR-TINTMAP] no engine (rivers off?)");
            return;
        }
        Registry registry = serverLevel.registryAccess().registry(Registries.BIOME).orElse(null);
        int n4 = (n >> 4) - 1 << 4;
        int n5 = (n2 >> 4) - 1 << 4;
        int n6 = 0;
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        int n10 = 0;
        int n11 = 0;
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder[] stringBuilderArray = new StringBuilder[12];
        for (n3 = 0; n3 < 12; ++n3) {
            stringBuilderArray[n3] = new StringBuilder();
        }
        for (n3 = 0; n3 < 12; ++n3) {
            for (int i = 0; i < 12; ++i) {
                int n12 = 46;
                for (int j = 0; j < 4 && n12 == 46; ++j) {
                    for (int k = 0; k < 4 && n12 == 46; ++k) {
                        boolean bl;
                        int n13;
                        int n14 = n4 + i * 4 + j;
                        int n15 = n5 + n3 * 4 + k;
                        int n16 = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, n14, n15) - 1;
                        if (n16 <= (n13 = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n14, n15) - 1)) continue;
                        BlockPos blockPos = new BlockPos(n14, n16, n15);
                        Holder<Biome> holder = serverLevel.getBiome(blockPos);
                        boolean bl2 = holder.is(STREAM_BIOME) || holder.is(BiomeTags.IS_RIVER);
                        Lake lake = riverEngine.lakeAt(n14, n15);
                        boolean bl3 = lake != null && Math.abs(n16 - lake.level()) <= 2;
                        ColumnPlan columnPlan = riverEngine.planLean(n14, n15);
                        boolean bl4 = columnPlan != ColumnPlan.NONE && columnPlan.inChannel();
                        int n17 = columnPlan != ColumnPlan.NONE ? Math.abs(n16 - columnPlan.waterSurfaceY()) : -1;
                        boolean bl5 = columnPlan != ColumnPlan.NONE && (bl4 || n17 <= 2);
                        boolean bl6 = !bl3 && !bl5 && StreamSnowCleanupFeature.a((WorldGenLevel)serverLevel, riverEngine, n14 & 0xFFFFFFFC, n15 & 0xFFFFFFFC) != Integer.MIN_VALUE;
                        boolean bl7 = bl = bl3 || bl5 || bl6;
                        if (bl2) {
                            ++n9;
                            n12 = 83;
                            continue;
                        }
                        if (bl) {
                            boolean bl8;
                            ++n11;
                            n12 = 88;
                            boolean bl9 = bl8 = serverLevel.getBiome(blockPos.above(4)).is(STREAM_BIOME) || serverLevel.getBiome(blockPos.below(4)).is(STREAM_BIOME);
                            if (bl8) {
                                ++n8;
                                n12 = 118;
                            }
                            if (stringBuilder.length() >= 900) continue;
                            stringBuilder.append(String.format(" (%d,%d,%d %s plan=%s chan=%b d=%d lake=%b rim=%b)", n14, n16, n15, registry != null ? holder.unwrapKey().map(resourceKey -> resourceKey.location().getPath()).orElse("?") : "?", columnPlan == ColumnPlan.NONE ? "NONE" : "yes", bl4, n17, bl3, bl6));
                            continue;
                        }
                        ++n10;
                        n12 = 111;
                        if (columnPlan == ColumnPlan.NONE) {
                            ++n6;
                            continue;
                        }
                        ++n7;
                    }
                }
                stringBuilderArray[n3].append((char)n12);
            }
        }
        Constants.LOG.info("[SR-TINTMAP] 3x3 chunks at ({},{}) cellgrid (N=up, 4-block cells):", (Object)n, (Object)n2);
        for (n3 = 0; n3 < 12; ++n3) {
            Constants.LOG.info("[SR-TINTMAP]   {}", (Object)stringBuilderArray[n3]);
        }
        Constants.LOG.info("[SR-TINTMAP] watered cells: stampedOK={} SHOULD-STAMP-BUT-LAND={} (of which vertical-miss={}) correctly-skipped={} (planNONE={} corridorDeltaFail={})", n9, n11, n8, n10, n6, n7);
        if (stringBuilder.length() > 0) {
            Constants.LOG.info("[SR-TINTMAP] samples:{}", (Object)stringBuilder);
        }
    }

    private static int a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2) {
        int n3;
        int n4;
        int n5 = Integer.MIN_VALUE;
        for (n4 = 0; n4 < 4; ++n4) {
            int n6;
            n3 = n + (n4 & 1) * 3;
            int n7 = n2 + (n4 >> 1 & 1) * 3;
            int n8 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, n3, n7) - 1;
            if (n8 <= (n6 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n3, n7) - 1)) continue;
            n5 = n8;
            break;
        }
        if (n5 == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        n4 = 2 + riverEngine.config().gridSpacing() / 2;
        for (n3 = 0; n3 < 8; ++n3) {
            Lake lake = riverEngine.lakeAt(n + 1 + B[n3] * n4, n2 + 1 + C[n3] * n4);
            if (lake == null || Math.abs(n5 - lake.level()) > 2) continue;
            return lake.level();
        }
        return Integer.MIN_VALUE;
    }

    private static Holder<Biome> a(int n, Holder<Biome> holder, Holder<Biome> holder2) {
        return n == 2 && holder != null ? holder : holder2;
    }

    private static Holder<Biome> a(WorldGenLevel worldGenLevel, Registry<Biome> registry, int n, int n2, int n3, Holder<Biome> holder) {
        Object object;
        Holder<Biome> holder2;
        Object object2;
        MultiNoiseBiomeSource biomeSource = null;
        Holder<Biome> holder3;
        block10: {
            holder3 = worldGenLevel.getUncachedNoiseBiome(n >> 2, n3 >> 2, n2 >> 2);
            boolean bl = Boolean.getBoolean("streamsreflowing.caldiag") || e.compareAndSet(true, false);
            try {
                object2 = worldGenLevel.getLevel().getChunkSource().getGenerator().getBiomeSource();
                if (object2 instanceof MultiNoiseBiomeSource) {
                    biomeSource = (MultiNoiseBiomeSource)object2;
                    Climate.Sampler object3 = worldGenLevel.getLevel().getChunkSource().randomState().sampler();
                    object = object3.sample(n >> 2, n3 >> 2, n2 >> 2);
                    for (float f : g) {
                        Climate.TargetPoint targetPoint = new Climate.TargetPoint(((Climate.TargetPoint)object).temperature(), ((Climate.TargetPoint)object).humidity(), ((Climate.TargetPoint)object).continentalness(), ((Climate.TargetPoint)object).erosion(), Climate.quantizeCoord(0.0f), Climate.quantizeCoord(f));
                        Holder<Biome> holder4 = biomeSource.getNoiseBiome(targetPoint);
                        if (bl) {
                            Constants.LOG.info("[SR-RIVERPICK] at {},{} weirdness={} -> {} (river={})", n, n2, Float.valueOf(f), holder4 == null ? "null" : holder4.unwrapKey().map(Object::toString).orElse("?"), holder4 != null && holder4.is(BiomeTags.IS_RIVER));
                        }
                        if (holder4 == null || !holder4.is(BiomeTags.IS_RIVER)) continue;
                        return holder4;
                    }
                    if (bl) {
                        Constants.LOG.info("[SR-RIVERPICK] at {},{}: no river-tagged biome from any valley probe (source={}) -> falling through to the tables", n, n2, object2.getClass().getName());
                    }
                } else if (bl) {
                    Constants.LOG.info("[SR-RIVERPICK] at {},{}: biome source is {} (not MultiNoise) -> tables", n, n2, object2.getClass().getName());
                }
            }
            catch (Throwable throwable) {
                if (!bl) break block10;
                Constants.LOG.info("[SR-RIVERPICK] at {},{}: probe failed ({})", n, n2, throwable.toString());
            }
        }
        object2 = holder3.unwrapKey().map(r::get).orElse(null);
        if (object2 != null && (holder2 = StreamSnowCleanupFeature.a(registry, (ResourceKey<Biome>)object2)) != null) {
            return holder2;
        }
        for (Map.Entry entry : d) {
            if (!holder3.is((TagKey)entry.getKey()) || (object = StreamSnowCleanupFeature.a(registry, (ResourceKey<Biome>)entry.getValue())) == null) continue;
            return (Holder<Biome>)object;
        }
        return holder;
    }

    private static Holder<Biome> a(Registry<Biome> registry, ResourceKey<Biome> resourceKey) {
        return registry.getHolder(resourceKey).map(reference -> reference).orElse(null);
    }

    private static boolean a(ChunkAccess chunkAccess, int n, int n2, int n3, Holder<Biome> holder) {
        int n4 = chunkAccess.getSectionIndex(n2);
        if (n4 < 0 || n4 >= chunkAccess.getSections().length) {
            return false;
        }
        LevelChunkSection levelChunkSection = chunkAccess.getSection(n4);
        PalettedContainer palettedContainer = (PalettedContainer)levelChunkSection.getBiomes();
        try {
            palettedContainer.getAndSet(n, n2 >> 2 & 3, n3, holder);
            return true;
        }
        catch (RuntimeException runtimeException) {
            if (D.compareAndSet(false, true)) {
                Constants.LOG.warn("Streams Reflowing: this world's chunk format doesn't accept biome relabeling; stream and lake areas keep their original biome names (cosmetic only). ({})", (Object)runtimeException.toString());
            }
            return false;
        }
    }

    private static boolean a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3) {
        return worldGenLevel.getBlockState(mutableBlockPos.set(n, n2 - 1, n3)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n, n2 + 1, n3)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n - 1, n2, n3)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n + 1, n2, n3)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n, n2, n3 - 1)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n, n2, n3 + 1)).isAir();
    }
}

