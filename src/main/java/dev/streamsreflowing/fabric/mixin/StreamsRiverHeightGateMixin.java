package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Rivers above the configured world-height ceiling are rejected at the query
// boundary. RoadWeaver and the worldgen callers both read ColumnPlan through
// plan/planLean/altPlan, so returning NONE makes them see ordinary terrain with
// no riverbed to avoid or bridge.
@Mixin(value = RiverEngine.class, remap = false)
public abstract class StreamsRiverHeightGateMixin {
    // 0.1.81: the ceiling is configurable (config/plaincraft-streams-prewarm.properties,
    // key maxRiverPlanY; 140 is both the old constant and the default). Read per call so
    // the config screen can change it without a restart - current() is a volatile field
    // read, and the gate runs once per plan query, far below terrain sampling cost.
    private static int plaincraft$maxRiverPlanY() {
        return StreamsPrewarmConfig.current().maxRiverPlanY();
    }

    @Inject(method = "plan", at = @At("RETURN"), cancellable = true, remap = false)
    private void plaincraft$gatePlan(int x, int z, CallbackInfoReturnable<ColumnPlan> cir) {
        plaincraft$gate(cir);
    }

    @Inject(method = "planLean", at = @At("RETURN"), cancellable = true, remap = false)
    private void plaincraft$gatePlanLean(int x, int z, CallbackInfoReturnable<ColumnPlan> cir) {
        plaincraft$gate(cir);
    }

    @Inject(method = "altPlan", at = @At("RETURN"), cancellable = true, remap = false)
    private void plaincraft$gateAltPlan(int x, int z, CallbackInfoReturnable<ColumnPlan> cir) {
        plaincraft$gate(cir);
    }

    private static void plaincraft$gate(CallbackInfoReturnable<ColumnPlan> cir) {
        ColumnPlan plan = cir.getReturnValue();
        if (plan == null || plan == ColumnPlan.NONE) {
            return;
        }
        int ceiling = plaincraft$maxRiverPlanY();
        if (plan.waterSurfaceY() > ceiling || plan.bedY() > ceiling) {
            cir.setReturnValue(ColumnPlan.NONE);
        }
    }
}