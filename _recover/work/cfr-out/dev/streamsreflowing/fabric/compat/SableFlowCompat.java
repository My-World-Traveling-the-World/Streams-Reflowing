/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.compat;

import dev.ryanhcode.sable.fabric.event.FabricSablePrePhysicsTickEvent;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.SableFlowCore;

public final class SableFlowCompat {
    private SableFlowCompat() {
    }

    public static void register() {
        FabricSablePrePhysicsTickEvent.EVENT.register((system, dt) -> SableFlowCore.onPrePhysics(system));
        Constants.LOG.info("[SR-SABLE] compat registered (Sable present)");
    }
}

