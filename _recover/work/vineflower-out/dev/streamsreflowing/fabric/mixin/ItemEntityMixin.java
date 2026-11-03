package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.fabric.fluid.StreamLiquidBlock;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemEntity.class})
public abstract class ItemEntityMixin {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void streamsreflowing$streamItemDrag(CallbackInfo ci) {
      ItemEntity self = (ItemEntity)this;
      if (self.isInWater()) {
         BlockPos bp = self.blockPosition();
         boolean carried = self.level().getBlockState(bp).getBlock() instanceof StreamLiquidBlock
            || StreamsConfigFabric.vanillaWaterFlow() && StreamFlowGrid.angleForPhysics(self.level(), bp.getX(), bp.getZ()) != -1;
         if (carried) {
            double cap = StreamsConfigFabric.itemDriftSpeed() / 20.0;
            Vec3 v = self.getDeltaMovement();
            double h = Math.hypot(v.x, v.z);
            if (h > cap) {
               double s = cap / h;
               self.setDeltaMovement(v.x * s, v.y, v.z * s);
            }
         }
      }
   }
}
