/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.fabric.StreamsReflowingFabric;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.worldgen.StreamBiomeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Biome.class})
public abstract class BiomeMixin {
    @WrapOperation(method={"shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;")})
    private Fluid streamsreflowing$streamFreezesLikeWater(FluidState state, Operation<Fluid> original) {
        if (state == null) {
            return Fluids.EMPTY;
        }
        if (!StreamsReflowingFabric.streamFluidRegistered()) {
            return (Fluid)original.call(new Object[]{state});
        }
        Fluid type = state.getType();
        return type == StreamFluids.SOURCE || type == StreamFluids.FLOWING ? Fluids.WATER : (Fluid)original.call(new Object[]{state});
    }

    @Unique
    private static Biome streamsreflowing$localLand(Object self, BlockPos pos) {
        Biome ours = StreamBiomeBridge.ours;
        if (ours == null || self != ours) {
            return null;
        }
        StreamBiomeBridge.LandProbe probe = StreamBiomeBridge.landProbe;
        if (probe == null) {
            return null;
        }
        Holder<Biome> land = probe.at(pos.getX(), pos.getY(), pos.getZ());
        return land == null || land.value() == ours ? null : land.value();
    }

    @Inject(method={"warmEnoughToRain(Lnet/minecraft/core/BlockPos;)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void streamsreflowing$adaptiveWarmEnough(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Biome land = BiomeMixin.streamsreflowing$localLand(this, pos);
        if (land != null) {
            cir.setReturnValue(land.warmEnoughToRain(pos));
        }
    }

    @Inject(method={"coldEnoughToSnow(Lnet/minecraft/core/BlockPos;)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void streamsreflowing$adaptiveColdEnough(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Biome land = BiomeMixin.streamsreflowing$localLand(this, pos);
        if (land != null) {
            cir.setReturnValue(land.coldEnoughToSnow(pos));
        }
    }
}

