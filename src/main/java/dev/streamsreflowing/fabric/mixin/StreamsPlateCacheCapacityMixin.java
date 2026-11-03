package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.compat.streamscontrol.StreamsSamplerModeController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "dev.streamsreflowing.core.cache.PlateRiverCache", remap = false)
public abstract class StreamsPlateCacheCapacityMixin {
    // Targets the Streams build actually deployed in the instance, which is 2.12.8.
    //
    // Upstream moved this constant in 2.13.8: the capacity check left get(PlateKey) for
    // a private eviction method b(PlateKey) and started appearing twice, so the 0.1.73
    // revision of this file injects into "b" with require = 2.
    //
    // That revision does NOT work here. A @ModifyConstant target is resolved at class
    // load, not at compile time, so injecting into a method that 2.12.8 does not have
    // compiles cleanly and then fails silently in production - the capacity control
    // this class exists to provide would simply stop applying. Verified by reading the
    // annotation out of the deployed 0.1.70 jar, which carries method="get", require=1.
    //
    // If the instance ever moves to 2.13.8+, this is the one line that must change back.
    @ModifyConstant(method = "get", constant = @Constant(intValue = 128), require = 1, remap = false)
    private int plaincraft$dynamicPlateCacheCapacity(int originalCapacity) {
        return StreamsSamplerModeController.desiredPlateCacheCapacity();
    }
}
