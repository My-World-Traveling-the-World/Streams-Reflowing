/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={WorldCarver.class})
public abstract class WorldCarverMixin {
    @Inject(method={"carveBlock"}, at={@At(value="HEAD")}, cancellable=true)
    private void streamsreflowing$skipRiverCorridor(CarvingContext context, CarverConfiguration config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor, CarvingMask mask, BlockPos.MutableBlockPos pos, BlockPos.MutableBlockPos checkPos, Aquifer aquifer, MutableBoolean reachedSurface, CallbackInfoReturnable<Boolean> cir) {
        if (!StreamsConfigFabric.generateRivers()) {
            return;
        }
        RiverEngine engine = StreamsWorldgen.active();
        if (engine != null && engine.inCorridor(pos.getX(), pos.getZ())) {
            cir.setReturnValue(false);
        }
    }
}

