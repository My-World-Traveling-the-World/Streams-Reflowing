/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class StreamRiverFlowFeature
extends Feature<NoneFeatureConfiguration> {
    public StreamRiverFlowFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        if (!Services.PLATFORM.flowVanillaRivers()) {
            return false;
        }
        if (Services.PLATFORM.streamWaterState(0).is(Blocks.WATER)) {
            return false;
        }
        WorldGenLevel worldGenLevel = featurePlaceContext.level();
        RiverFlowField.BiomeClass biomeClass = RiverFlowFields.classifierFor(worldGenLevel.getLevel());
        RiverFlowField.OrientationField orientationField = RiverFlowFields.orientationFor(worldGenLevel.getLevel());
        ChunkPos chunkPos = new ChunkPos(featurePlaceContext.origin());
        int n = chunkPos.getMinBlockX();
        int n2 = chunkPos.getMinBlockZ();
        int n3 = RiverFlowFields.regionSize();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl = false;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n4;
                int n5;
                int n6 = n + i;
                int n7 = n2 + j;
                int n8 = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, n6, n7) - 1;
                if (n8 <= (n5 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n6, n7) - 1)) continue;
                mutableBlockPos.set(n6, n8, n7);
                if (worldGenLevel.getBiome(mutableBlockPos).is(BiomeTags.IS_OCEAN) || (n4 = RiverFlowFields.dirAt(biomeClass, orientationField, n6, n7)) < 0) continue;
                BlockState blockState = Services.PLATFORM.streamWaterState(n4);
                for (int k = n8; k > n5; --k) {
                    mutableBlockPos.set(n6, k, n7);
                    if (!worldGenLevel.getBlockState(mutableBlockPos).is(Blocks.WATER)) continue;
                    worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
                    bl = true;
                }
            }
        }
        return bl;
    }
}

