package dev.streamsreflowing.fabric.mixin;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseChunk.class)
public interface NoiseChunkSlotZeroAccess {
    @Accessor("firstCellX")
    int plaincraft$getFirstCellX();

    @Accessor("firstCellZ")
    int plaincraft$getFirstCellZ();

    @Accessor("cellWidth")
    int plaincraft$getCellWidth();

    @Accessor("sliceFillingContextProvider")
    DensityFunction.ContextProvider plaincraft$getSliceFillingContextProvider();

    @Accessor("interpolating")
    boolean plaincraft$isInterpolating();

    @Accessor("interpolating")
    void plaincraft$setInterpolating(boolean value);

    @Accessor("cellStartBlockX")
    void plaincraft$setCellStartBlockX(int value);

    @Accessor("inCellX")
    void plaincraft$setInCellX(int value);

    @Accessor("cellStartBlockZ")
    void plaincraft$setCellStartBlockZ(int value);

    @Accessor("inCellZ")
    void plaincraft$setInCellZ(int value);

    @Accessor("interpolationCounter")
    void plaincraft$setInterpolationCounter(long value);

    @Accessor("arrayInterpolationCounter")
    long plaincraft$getArrayInterpolationCounter();

    @Accessor("arrayInterpolationCounter")
    void plaincraft$setArrayInterpolationCounter(long value);
}
