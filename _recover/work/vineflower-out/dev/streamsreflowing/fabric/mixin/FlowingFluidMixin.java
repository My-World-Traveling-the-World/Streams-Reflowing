package dev.streamsreflowing.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlowingFluid.class})
public abstract class FlowingFluidMixin {
   private static boolean streamsreflowing$announced = false;
   private static final double STREAMS_INFLOW_COS = -0.5;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void streamsreflowing$confirmApplied(CallbackInfo ci) {
      if (!streamsreflowing$announced) {
         streamsreflowing$announced = true;
         LoggerFactory.getLogger("StreamsReflowing").debug("FlowingFluidMixin loaded — spill-over-water gated by config 'spillOverWater'");
      }
   }

   @Inject(
      method = {"isWaterHole"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void streamsreflowing$spillOverWater(
      BlockGetter level, Fluid fluid, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState, CallbackInfoReturnable<Boolean> cir
   ) {
      if (this instanceof WaterFluid && StreamsConfigFabric.spillOverWater()) {
         if (!StreamsConfigFabric.forceBakedFluid() || this == StreamFluids.SOURCE || this == StreamFluids.FLOWING) {
            if (!StreamsConfigFabric.spillOnStreamsOnly() || StreamFlowGrid.angleForPhysics(level, pos.getX(), pos.getZ()) != -1) {
               FluidState below = spreadState.getFluidState();
               if (below.isSource() && below.getType().isSame((FlowingFluid)this)) {
                  cir.setReturnValue(false);
               }
            }
         }
      }
   }

   @ModifyReturnValue(
      method = {"getFlow"},
      at = {@At("RETURN")}
   )
   private Vec3 streamsreflowing$imposeVanillaWaterFlow(Vec3 original, BlockGetter level, BlockPos pos, FluidState state) {
      if (this instanceof WaterFluid && StreamsConfigFabric.vanillaWaterFlow()) {
         byte angle = level instanceof Level
            ? StreamFlowGrid.angleForEntityPush(level, pos.getX(), pos.getZ())
            : StreamFlowGrid.angleForPhysics(level, pos.getX(), pos.getZ());
         return angle == -1 ? original : new Vec3(StreamFlowGrid.vx(angle), 0.0, StreamFlowGrid.vz(angle));
      } else {
         return original;
      }
   }
}
