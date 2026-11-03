package dev.streamsreflowing.fabric.mixin;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import dev.streamsreflowing.compat.streamscontrol.client.StreamsUnifiedConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.streamsreflowing.fabric.client.StreamsModMenu", remap = false)
public abstract class StreamsModMenuMixin {
    @Inject(method = "getModConfigScreenFactory", at = @At("HEAD"), cancellable = true, remap = false)
    private void plaincraft$useUnifiedStreamsConfig(
            CallbackInfoReturnable<ConfigScreenFactory<?>> cir) {
        cir.setReturnValue(StreamsUnifiedConfigScreen::new);
    }
}
