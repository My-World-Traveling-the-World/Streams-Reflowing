package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.compat.streamscontrol.StreamsStartupProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.streamsreflowing.worldgen.LoadingNotice", remap = false)
public abstract class StreamsLoadingNoticeMixin {
    @Inject(method = "currentText", at = @At("HEAD"), cancellable = true, require = 0)
    private static void plaincraft$hideEstimatedProgress(CallbackInfoReturnable<String> cir) {
        if (StreamsStartupProgress.isActive()) {
            cir.setReturnValue(null);
        }
    }
}
