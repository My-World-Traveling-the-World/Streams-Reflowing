package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmConfig;
import java.util.concurrent.atomic.AtomicLong;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Rivers above the configured world-height ceiling are rejected at the query
// boundary. RoadWeaver and the worldgen callers both read ColumnPlan through
// plan/planLean/altPlan, so returning NONE makes them see ordinary terrain with
// no riverbed to avoid or bridge.
@Mixin(value = RiverEngine.class, remap = false)
public abstract class StreamsRiverHeightGateMixin {
    /** RiverEngine's own StreamsConfig, needed for the exact inset formula. */
    @Shadow @Final private StreamsConfig f;

    /**
     * How many columns the early exit discarded before the expensive part.
     *
     * <p>Zero-cost unless read. Log it with {@code -Dplaincraft.streams.heightGateDiag=true}.
     */
    private static final AtomicLong EARLY_EXITS = new AtomicLong();
    private static final boolean DIAG =
            Boolean.getBoolean("plaincraft.streams.heightGateDiag");

    // 0.1.81: the ceiling is configurable (config/plaincraft-streams-prewarm.properties,
    // key maxRiverPlanY; 140 is both the old constant and the default). Read per call so
    // the config screen can change it without a restart - current() is a volatile field
    // read, and the gate runs once per plan query, far below terrain sampling cost.
    private static int plaincraft$maxRiverPlanY() {
        return StreamsPrewarmConfig.current().maxRiverPlanY();
    }

    /**
     * Rejects over-ceiling columns <b>before</b> the expensive half of the plan runs.
     *
     * <h2>Why this is exactly equivalent to the RETURN gate below</h2>
     * {@code plan}, {@code planLean} and {@code altPlan} all funnel into one private worker,
     * {@code a(double,double,RiverSample,boolean)}, and every one of them is covered by a
     * RETURN gate. That gate rejects when {@code waterSurfaceY > ceiling || bedY > ceiling}.
     * Inside the worker:
     * <pre>
     *   waterSurfaceY = max(seaLevel - 1, floor(gradeY) - insetFor(width, cfg))
     *   bedY          = waterSurfaceY - depth          // strictly below waterSurfaceY
     * </pre>
     * so {@code waterSurfaceY >= floor(gradeY) - insetFor(width, cfg)} always holds (the
     * {@code max} can only raise it). Therefore
     * <pre>
     *   floor(gradeY) - insetFor(width, cfg) > ceiling
     *     ⇒ waterSurfaceY > ceiling
     *     ⇒ the RETURN gate would have replaced this plan with NONE
     * </pre>
     * Returning NONE here produces the identical observable result while skipping the
     * section-inset solve and the flow-direction solve — the two expensive steps, one of
     * which calls {@code lakeAt}. {@code gradeY} comes from the river segment's grade
     * profile, already computed by the flow network, so this check itself costs nothing.
     *
     * <p>{@code require = 0} keeps the mod loadable if a future upstream renames the worker;
     * the RETURN gates below then remain the only enforcement, i.e. today's behaviour.
     */
    @Inject(
            method = "a(DDLdev/streamsreflowing/core/river/RiverSample;Z)Ldev/streamsreflowing/core/compose/ColumnPlan;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private void plaincraft$gateBeforePlanWork(
            double x, double z, RiverSample sample, boolean computeDirection,
            CallbackInfoReturnable<ColumnPlan> cir) {
        if (sample == null) {
            return;
        }
        int inset = RiverEngine.insetFor(sample.width(), this.f);
        if ((int)Math.floor(sample.gradeY()) - inset <= plaincraft$maxRiverPlanY()) {
            return;
        }
        cir.setReturnValue(ColumnPlan.NONE);
        if (DIAG) {
            long total = EARLY_EXITS.incrementAndGet();
            if ((total & 0xFFFL) == 0L) {
                dev.streamsreflowing.Constants.LOG.info(
                        "[SR-HEIGHTGATE] early exits so far: {}", total);
            }
        }
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