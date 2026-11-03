/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.neoforge.mixin;

import dev.streamsreflowing.worldgen.StreamBiomeBridge;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Biome.class})
public abstract class StreamBiomeSpawnsMixin {
    @Inject(method={"getMobSettings"}, at={@At(value="HEAD")}, cancellable=true, require=0, expect=0)
    private void streamsreflowing$riverSpawns(CallbackInfoReturnable<MobSpawnSettings> cir) {
        if (this == StreamBiomeBridge.ours && StreamBiomeBridge.riverSpawns != null) {
            cir.setReturnValue(StreamBiomeBridge.riverSpawns);
        }
    }
}

