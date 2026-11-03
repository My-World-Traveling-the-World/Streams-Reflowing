package dev.streamsreflowing.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   targets = {"me.yassigame.sable_beyond.event.FlowingFluidSubLevelForceHandler"},
   remap = false
)
public abstract class SableBeyondWaterMixin {
   @WrapOperation(
      method = {"sampleFluidBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
         remap = true
      )},
      require = 0,
      expect = 0,
      remap = false
   )
   private static Vec3 streamsreflowing$waterIsOurs(FluidState state, BlockGetter level, BlockPos pos, Operation<Vec3> original) {
      return StreamsConfigFabric.vesselDriftSpeed() > 0.0 && StreamsConfigFabric.vanillaWaterFlow() && state.is(FluidTags.WATER)
         ? Vec3.ZERO
         : (Vec3)original.call(new Object[]{state, level, pos});
   }
}
