/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Structure.class})
public abstract class StructureMixin {
    @Inject(method={"generate"}, at={@At(value="RETURN")}, cancellable=true)
    private void streamsreflowing$keepFootprintOffRivers(CallbackInfoReturnable<StructureStart> cir) {
        if (!StreamsConfigFabric.generateRivers()) {
            return;
        }
        StructureStart start = cir.getReturnValue();
        if (start != null && start.isValid() && StreamsWorldgen.structureFootprintOnStream(start.getBoundingBox())) {
            cir.setReturnValue(StructureStart.INVALID_START);
        }
    }
}

