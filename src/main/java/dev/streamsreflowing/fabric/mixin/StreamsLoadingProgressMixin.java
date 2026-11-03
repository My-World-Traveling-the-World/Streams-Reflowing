package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.compat.streamscontrol.client.StreamsLoadingProgressRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelLoadingScreen.class, priority = 900)
public abstract class StreamsLoadingProgressMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void plaincraft$renderStartupProgress(GuiGraphics graphics, int mouseX, int mouseY,
                                                   float partialTick, CallbackInfo ci) {
        StreamsLoadingProgressRenderer.render(graphics);
    }
}
