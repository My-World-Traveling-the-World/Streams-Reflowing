/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ConfiguredFeature.class})
public abstract class ConfiguredFeatureMixin {
    @Inject(method={"place"}, at={@At(value="HEAD")}, cancellable=true)
    private void streamsreflowing$avoidStreams(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!StreamsConfigFabric.generateRivers()) {
            return;
        }
        if (((ConfiguredFeature)((Object)this)).feature().getClass().getName().startsWith("dev.streamsreflowing")) {
            return;
        }
        RiverEngine engine = StreamsWorldgen.engine(level);
        long probe = engine.vetoProbeAt(pos.getX(), pos.getZ());
        int lakeY = (int)(probe >> 32);
        if (lakeY != Integer.MIN_VALUE && pos.getY() <= lakeY) {
            int bedY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
            if (pos.getY() >= bedY) {
                cir.setReturnValue(false);
            }
            return;
        }
        int chanY = (int)probe;
        if (chanY != Integer.MIN_VALUE) {
            int bedY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
            if (pos.getY() >= Math.min(bedY, chanY - 1)) {
                cir.setReturnValue(false);
            }
        }
    }
}

