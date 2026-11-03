/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.fabric.StreamsConfigFabric;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Boat.class})
public abstract class BoatMixin {
    private static final double STREAMSREFLOWING$MAX_FLOAT_STEP = 1.25;

    @Inject(method={"isUnderwater"}, at={@At(value="RETURN")}, cancellable=true)
    private void streamsreflowing$floatOnFlowingWater(CallbackInfoReturnable<Boat.Status> cir) {
        if (StreamsConfigFabric.boatsFloatOnFlowingWater() && cir.getReturnValue() == Boat.Status.UNDER_FLOWING_WATER) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method={"floatBoat"}, at={@At(value="TAIL")})
    private void streamsreflowing$rideTheSurface(CallbackInfo ci) {
        if (!StreamsConfigFabric.boatsFloatOnFlowingWater()) {
            return;
        }
        Boat self = (Boat)((Object)this);
        double surface = BoatMixin.streamsreflowing$streamSurface(self);
        if (Double.isInfinite(surface)) {
            return;
        }
        double target = surface - (double)self.getBbHeight() + 0.1;
        double y = self.getY();
        Vec3 v = self.getDeltaMovement();
        if (y < target - 0.05) {
            self.setDeltaMovement(v.x, Math.min(0.15, (target - y) * 0.4), v.z);
        } else if (y <= target + 0.05) {
            self.setDeltaMovement(v.x, 0.0, v.z);
        }
    }

    private static double streamsreflowing$streamSurface(Boat self) {
        AABB bb = self.getBoundingBox();
        int x0 = Mth.floor(bb.minX);
        int x1 = Mth.ceil(bb.maxX);
        int z0 = Mth.floor(bb.minZ);
        int z1 = Mth.ceil(bb.maxZ);
        int yTop = Mth.ceil(bb.maxY) + 1;
        int yBot = Mth.floor(bb.minY) - 1;
        boolean flowingPresent = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double[] columns = new double[(x1 - x0 + 1) * (z1 - z0 + 1)];
        int nCols = 0;
        for (int x = x0; x <= x1; ++x) {
            block1: for (int z = z0; z <= z1; ++z) {
                for (int y = yTop; y >= yBot; --y) {
                    FluidState fs = self.level().getFluidState(p.set(x, y, z));
                    if (fs.isEmpty() || !fs.is(FluidTags.WATER)) continue;
                    if (!fs.isSource()) {
                        flowingPresent = true;
                    }
                    columns[nCols++] = (float)y + fs.getHeight(self.level(), p);
                    continue block1;
                }
            }
        }
        if (!flowingPresent || nCols == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        double lowest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nCols; ++i) {
            lowest = Math.min(lowest, columns[i]);
        }
        double surface = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < nCols; ++i) {
            if (!(columns[i] <= lowest + 1.25)) continue;
            surface = Math.max(surface, columns[i]);
        }
        return surface;
    }
}

