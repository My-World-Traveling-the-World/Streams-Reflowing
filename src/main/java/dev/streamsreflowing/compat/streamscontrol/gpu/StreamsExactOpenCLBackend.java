package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.Collection;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/** Self-contained exact FP64 OpenCL backend used only by Streams terrain batches. */
public final class StreamsExactOpenCLBackend implements AutoCloseable {
    private final OpenCLRuntime runtime;
    private final OpenCLAccurateProgramCache.ProgramState programState;
    private final RandomState randomState;
    private volatile boolean serialHeightMode;

    private StreamsExactOpenCLBackend(OpenCLRuntime runtime,
                                      OpenCLAccurateProgramCache.ProgramState programState,
                                      RandomState randomState) {
        this.runtime = runtime;
        this.programState = programState;
        this.randomState = randomState;
    }

    public static Creation create(ServerLevel level) {
        long started = System.nanoTime();
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator)) {
            return Creation.failed("chunk generator is not NoiseBasedChunkGenerator", started, null);
        }
        try {
            OpenCLRuntime runtime = OpenCLRuntime.tryCreateAccurateGpu();
            if (runtime == null) {
                return Creation.failed("no usable FP64 OpenCL GPU runtime", started, null);
            }
            if (!runtime.deviceInfo().gpu()) {
                return Creation.failed("selected OpenCL device is not a GPU: " + runtime.deviceName(), started, null);
            }
            if (!runtime.deviceInfo().fp64()) {
                return Creation.failed("selected GPU does not expose FP64: " + runtime.deviceName(), started, null);
            }
            RandomState randomState = level.getChunkSource().getGeneratorState().randomState();
            OpenCLAccurateProgramCache.ProgramState state = OpenCLAccurateProgramCache.getOrCompile(
                    level, generator, randomState, runtime.deviceInfo());
            if (!state.supported()) {
                return Creation.failed("density program unsupported: " + state.unsupportedReason(), started, null);
            }
            return Creation.success(
                    new StreamsExactOpenCLBackend(runtime, state, randomState), started);
        } catch (Throwable failure) {
            return Creation.failed("OpenCL initialization threw " + failure.getClass().getSimpleName()
                    + ": " + String.valueOf(failure.getMessage()), started, failure);
        }
    }

    public Map<Long, AccurateHeightSample> samplePositions(Collection<BlockPos> positions) {
        if (!isUsable()) {
            throw new IllegalStateException("Streams exact OpenCL backend is unavailable");
        }
        return programState.program().samplePositions(
                runtime, positions, AccurateSamplingProgress.NONE, serialHeightMode);
    }

    public Map<Long, AccurateHeightSample> sampleOceanFloorPositions(Collection<BlockPos> positions) {
        if (!isUsable()) {
            throw new IllegalStateException("Streams exact OpenCL backend is unavailable");
        }
        return programState.program().sampleOceanFloorPositions(
                runtime, positions, AccurateSamplingProgress.NONE);
    }

    public void useSerialHeightMode() {
        serialHeightMode = true;
    }

    public boolean serialHeightMode() {
        return serialHeightMode;
    }

    public boolean isUsable() {
        return programState.supported() && runtime.isUsable();
    }

    public String deviceName() {
        return runtime.deviceName();
    }

    public dev.streamsreflowing.compat.streamscontrol.gpu.bridge.OpenCLBridge.DeviceInfo deviceInfo() {
        return runtime.deviceInfo();
    }

    public void invalidate(Throwable failure) {
        if (programState.supported()) {
            runtime.invalidate(failure);
        }
    }

    @Override
    public void close() {
        OpenCLAccurateProgramCache.clear(randomState);
    }

    public static void closeRuntime() {
        OpenCLRuntime.closeAll();
    }

    public record Creation(StreamsExactOpenCLBackend backend, String reason,
                           long initializationNanos, Throwable failure) {
        private static Creation success(StreamsExactOpenCLBackend backend, long started) {
            return new Creation(backend, "", System.nanoTime() - started, null);
        }

        private static Creation failed(String reason, long started, Throwable failure) {
            return new Creation(null, reason, System.nanoTime() - started, failure);
        }

        public boolean successful() {
            return backend != null;
        }
    }
}
