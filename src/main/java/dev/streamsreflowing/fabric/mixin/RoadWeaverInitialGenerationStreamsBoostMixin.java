package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.compat.streamscontrol.StreamsSamplerModeController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.shiroha233.roadweaver.generation.InitialGenManager", remap = false)
public abstract class RoadWeaverInitialGenerationStreamsBoostMixin {
    @Inject(method = "blockUntilDone", at = @At("HEAD"), require = 0, remap = false)
    private static void plaincraft$enableStreamsStartupBoost(CallbackInfo ci) {
        StreamsSamplerModeController.enterInitialGeneration();
    }

    @Inject(method = "blockUntilDone", at = @At("RETURN"), require = 0, remap = false)
    private static void plaincraft$restoreStreamsGameplayLimit(CallbackInfo ci) {
        StreamsSamplerModeController.exitInitialGeneration();
    }
}
