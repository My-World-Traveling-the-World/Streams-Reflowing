/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.fabric.StreamsReflowingFabric;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={WaterFluid.class})
public class WaterFluidMixin {
    @Inject(method={"isSame"}, at={@At(value="HEAD")}, cancellable=true)
    private void streamsreflowing$sameAsStream(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (!StreamsReflowingFabric.streamFluidRegistered()) {
            return;
        }
        if (fluid == StreamFluids.SOURCE || fluid == StreamFluids.FLOWING) {
            cir.setReturnValue(true);
        }
    }
}

