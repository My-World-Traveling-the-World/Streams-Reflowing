package dev.streamsreflowing.fabric.mixin;

import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunk.NoiseInterpolator.class)
public interface NoiseInterpolatorLatticeAccess {
    @Accessor("slice0")
    double[][] plaincraft$getSlice0();

    @Accessor("slice1")
    double[][] plaincraft$getSlice1();

    @Invoker("swapSlices")
    void plaincraft$swapSlices();
}
