package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import dev.streamsreflowing.fabric.mixin.NoiseChunkLatticeAccess;
import dev.streamsreflowing.fabric.mixin.NoiseChunkSlotZeroAccess;
import dev.streamsreflowing.fabric.mixin.NoiseInterpolatorLatticeAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extracts slot zero through the native NoiseChunk interpolation lifecycle. */
final class NativeSlotZeroLatticeSampler {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final AtomicBoolean TARGETED_VALIDATED = new AtomicBoolean();
    private static volatile boolean targetedEnabled = true;

    private NativeSlotZeroLatticeSampler() {
    }

    static double[] sample(List<Long> chunkKeys,
                           RandomState randomState,
                           NoiseGeneratorSettings generatorSettings,
                           NoiseSettings noiseSettings,
                           Aquifer.FluidPicker fluidPicker,
                           int expectedInterpolatorCount) {
        int cellCountXZ = 16 / noiseSettings.getCellWidth();
        int cellCountY = noiseSettings.height() / noiseSettings.getCellHeight();
        int latticePoints = OpenCLAccurateLayout.latticePointCount(cellCountXZ, cellCountY);
        if (targetedEnabled && !TARGETED_VALIDATED.get()) {
            try {
                List<Long> probeChunk = List.of(chunkKeys.getFirst());
                double[] targetedProbe = sampleTargeted(probeChunk, randomState, generatorSettings,
                        noiseSettings, fluidPicker, expectedInterpolatorCount,
                        latticePoints, cellCountXZ, cellCountY);
                double[] fullProbe = sampleFull(probeChunk, randomState, generatorSettings,
                        noiseSettings, fluidPicker, expectedInterpolatorCount,
                        latticePoints, cellCountXZ, cellCountY);
                if (!java.util.Arrays.equals(targetedProbe, fullProbe)) {
                    targetedEnabled = false;
                    LOGGER.warn("Streams targeted native slot zero failed exact first-chunk validation; "
                            + "using the complete native NoiseChunk interpolation path.");
                } else {
                    TARGETED_VALIDATED.set(true);
                    LOGGER.info("Streams targeted native slot zero passed exact first-chunk validation.");
                }
            } catch (Throwable failure) {
                targetedEnabled = false;
                LOGGER.warn("Streams targeted native slot zero disabled; using the complete native "
                        + "NoiseChunk interpolation path.", failure);
            }
        }
        if (targetedEnabled && TARGETED_VALIDATED.get()) {
            return sampleTargeted(chunkKeys, randomState, generatorSettings, noiseSettings,
                    fluidPicker, expectedInterpolatorCount, latticePoints, cellCountXZ, cellCountY);
        }
        return sampleFull(chunkKeys, randomState, generatorSettings, noiseSettings,
                fluidPicker, expectedInterpolatorCount, latticePoints, cellCountXZ, cellCountY);
    }

    private static double[] sampleTargeted(List<Long> chunkKeys,
                                           RandomState randomState,
                                           NoiseGeneratorSettings generatorSettings,
                                           NoiseSettings noiseSettings,
                                           Aquifer.FluidPicker fluidPicker,
                                           int expectedInterpolatorCount,
                                           int latticePoints,
                                           int cellCountXZ,
                                           int cellCountY) {
        double[] output = new double[Math.multiplyExact(chunkKeys.size(), latticePoints)];
        for (int chunkIndex = 0; chunkIndex < chunkKeys.size(); chunkIndex++) {
            NoiseChunk noiseChunk = createNoiseChunk(chunkKeys.get(chunkIndex), randomState,
                    generatorSettings, noiseSettings, fluidPicker, cellCountXZ);
            List<NoiseChunk.NoiseInterpolator> interpolators = interpolators(
                    noiseChunk, expectedInterpolatorCount);
            NoiseInterpolatorLatticeAccess slotZero =
                    (NoiseInterpolatorLatticeAccess) (Object) interpolators.getFirst();
            NoiseChunkSlotZeroAccess access = (NoiseChunkSlotZeroAccess) (Object) noiseChunk;
            int chunkBase = chunkIndex * latticePoints;
            beginTargeted(access);
            try {
                fillTargetedSlice(access, slotZero, slotZero.plaincraft$getSlice0(),
                        access.plaincraft$getFirstCellX(), cellCountXZ);
                for (int cellX = 0; cellX < cellCountXZ; cellX++) {
                    fillTargetedSlice(access, slotZero, slotZero.plaincraft$getSlice1(),
                            access.plaincraft$getFirstCellX() + cellX + 1, cellCountXZ);
                    access.plaincraft$setCellStartBlockX(
                            (access.plaincraft$getFirstCellX() + cellX) * access.plaincraft$getCellWidth());
                    copySlice(slotZero.plaincraft$getSlice0(), output, chunkBase,
                            cellX, cellCountXZ, cellCountY);
                    if (cellX == cellCountXZ - 1) {
                        copySlice(slotZero.plaincraft$getSlice1(), output, chunkBase,
                                cellX + 1, cellCountXZ, cellCountY);
                    }
                    slotZero.plaincraft$swapSlices();
                }
            } finally {
                access.plaincraft$setInterpolating(false);
            }
        }
        return output;
    }

    private static double[] sampleFull(List<Long> chunkKeys,
                                       RandomState randomState,
                                       NoiseGeneratorSettings generatorSettings,
                                       NoiseSettings noiseSettings,
                                       Aquifer.FluidPicker fluidPicker,
                                       int expectedInterpolatorCount,
                                       int latticePoints,
                                       int cellCountXZ,
                                       int cellCountY) {
        double[] output = new double[Math.multiplyExact(chunkKeys.size(), latticePoints)];

        for (int chunkIndex = 0; chunkIndex < chunkKeys.size(); chunkIndex++) {
            NoiseChunk noiseChunk = createNoiseChunk(chunkKeys.get(chunkIndex), randomState,
                    generatorSettings, noiseSettings, fluidPicker, cellCountXZ);
            List<NoiseChunk.NoiseInterpolator> interpolators = interpolators(
                    noiseChunk, expectedInterpolatorCount);

            NoiseInterpolatorLatticeAccess slotZero =
                    (NoiseInterpolatorLatticeAccess) (Object) interpolators.getFirst();
            int chunkBase = chunkIndex * latticePoints;
            noiseChunk.initializeForFirstCellX();
            try {
                for (int cellX = 0; cellX < cellCountXZ; cellX++) {
                    noiseChunk.advanceCellX(cellX);
                    copySlice(slotZero.plaincraft$getSlice0(), output, chunkBase,
                            cellX, cellCountXZ, cellCountY);
                    if (cellX == cellCountXZ - 1) {
                        copySlice(slotZero.plaincraft$getSlice1(), output, chunkBase,
                                cellX + 1, cellCountXZ, cellCountY);
                    }
                    noiseChunk.swapSlices();
                }
            } finally {
                noiseChunk.stopInterpolation();
            }
        }
        return output;
    }

    private static NoiseChunk createNoiseChunk(long chunkKey,
                                               RandomState randomState,
                                               NoiseGeneratorSettings generatorSettings,
                                               NoiseSettings noiseSettings,
                                               Aquifer.FluidPicker fluidPicker,
                                               int cellCountXZ) {
        ChunkPos chunkPos = new ChunkPos(chunkKey);
        return new NoiseChunk(
                cellCountXZ,
                randomState,
                chunkPos.getMinBlockX(),
                chunkPos.getMinBlockZ(),
                noiseSettings,
                ZeroBeardifier.INSTANCE,
                generatorSettings,
                fluidPicker,
                Blender.empty());
    }

    private static List<NoiseChunk.NoiseInterpolator> interpolators(
            NoiseChunk noiseChunk, int expectedInterpolatorCount) {
        List<NoiseChunk.NoiseInterpolator> interpolators =
                ((NoiseChunkLatticeAccess) (Object) noiseChunk).plaincraft$getInterpolators();
        if (interpolators.size() < expectedInterpolatorCount || interpolators.isEmpty()) {
            throw new IllegalStateException("native/GPU interpolator count mismatch: native="
                    + interpolators.size() + ", gpu=" + expectedInterpolatorCount);
        }
        return interpolators;
    }

    private static void beginTargeted(NoiseChunkSlotZeroAccess access) {
        if (access.plaincraft$isInterpolating()) {
            throw new IllegalStateException("native slot zero interpolation already started");
        }
        access.plaincraft$setInterpolating(true);
        access.plaincraft$setInterpolationCounter(0L);
    }

    private static void fillTargetedSlice(NoiseChunkSlotZeroAccess access,
                                          NoiseInterpolatorLatticeAccess slotZero,
                                          double[][] slice,
                                          int cellX,
                                          int cellCountXZ) {
        int cellWidth = access.plaincraft$getCellWidth();
        access.plaincraft$setCellStartBlockX(cellX * cellWidth);
        access.plaincraft$setInCellX(0);
        for (int cellZ = 0; cellZ <= cellCountXZ; cellZ++) {
            access.plaincraft$setCellStartBlockZ(
                    (access.plaincraft$getFirstCellZ() + cellZ) * cellWidth);
            access.plaincraft$setInCellZ(0);
            access.plaincraft$setArrayInterpolationCounter(
                    access.plaincraft$getArrayInterpolationCounter() + 1L);
            ((NoiseChunk.NoiseInterpolator) slotZero).fillArray(
                    slice[cellZ], access.plaincraft$getSliceFillingContextProvider());
        }
        access.plaincraft$setArrayInterpolationCounter(
                access.plaincraft$getArrayInterpolationCounter() + 1L);
    }

    private enum ZeroBeardifier implements DensityFunctions.BeardifierOrMarker {
        INSTANCE;

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 0.0D;
        }

        @Override
        public double minValue() {
            return 0.0D;
        }

        @Override
        public double maxValue() {
            return 0.0D;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return DensityFunctions.BeardifierOrMarker.CODEC;
        }
    }

    private static void copySlice(double[][] slice,
                                  double[] output,
                                  int chunkBase,
                                  int cellX,
                                  int cellCountXZ,
                                  int cellCountY) {
        int latticeXZ = cellCountXZ + 1;
        int latticeY = cellCountY + 1;
        if (slice.length != latticeXZ) {
            throw new IllegalStateException("native slot zero slice width mismatch: " + slice.length
                    + " != " + latticeXZ);
        }
        for (int cellZ = 0; cellZ < latticeXZ; cellZ++) {
            double[] column = slice[cellZ];
            if (column.length != latticeY) {
                throw new IllegalStateException("native slot zero slice height mismatch: " + column.length
                        + " != " + latticeY);
            }
            int target = chunkBase + OpenCLAccurateLayout.latticePointIndex(
                    cellCountXZ, cellCountY, cellX, 0, cellZ);
            System.arraycopy(column, 0, output, target, latticeY);
        }
    }
}
