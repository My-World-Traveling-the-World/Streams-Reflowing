package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.client.FrameDiag;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public class FrameHeartbeatMixin {
   @Inject(
      method = {"runTick"},
      at = {@At("HEAD")},
      require = 0
   )
   private void streamsreflowing$frameHeartbeat(boolean renderLevel, CallbackInfo ci) {
      FrameDiag.frame();
   }
}
