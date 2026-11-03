/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WaterSkinFeature
extends Feature<NoneFeatureConfiguration> {
    public WaterSkinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        if (!Services.PLATFORM.generateRivers()) {
            return false;
        }
        ChunkPos chunkPos = new ChunkPos(featurePlaceContext.origin());
        RiverCarverFeature.skinAllWater(featurePlaceContext.level(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(), Services.PLATFORM.allWaterBanks());
        return true;
    }
}

