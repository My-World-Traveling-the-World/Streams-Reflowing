/*
 * Decompiled with CFR 0.152.
 */
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

@Mixin(value={ItemEntity.class})
public abstract class ItemEntityMixin {
    @Inject(method={"tick"}, at={@At(value="TAIL")})
    private void streamsreflowing$streamItemDrag(CallbackInfo ci) {
        boolean carried;
        ItemEntity self = (ItemEntity)((Object)this);
        if (!self.isInWater()) {
            return;
        }
        BlockPos bp = self.blockPosition();
        boolean bl = carried = self.level().getBlockState(bp).getBlock() instanceof StreamLiquidBlock || StreamsConfigFabric.vanillaWaterFlow() && StreamFlowGrid.angleForPhysics(self.level(), bp.getX(), bp.getZ()) != -1;
        if (!carried) {
            return;
        }
        double cap = StreamsConfigFabric.itemDriftSpeed() / 20.0;
        Vec3 v = self.getDeltaMovement();
        double h = Math.hypot(v.x, v.z);
        if (h > cap) {
            double s = cap / h;
            self.setDeltaMovement(v.x * s, v.y, v.z * s);
        }
    }
}

