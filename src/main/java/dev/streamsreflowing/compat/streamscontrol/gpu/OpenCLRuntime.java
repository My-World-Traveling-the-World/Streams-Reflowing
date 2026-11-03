/* 鏂囦欢鑱岃矗锛氱鐞嗗彲澶嶇敤鐨?OpenCL 璁惧浼氳瘽銆乲ernel 璧勬簮涓庝覆琛屾彁浜よ竟鐣屻€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import dev.streamsreflowing.compat.streamscontrol.gpu.bridge.OpenCLBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * OpenCL 楂樺眰杩愯鏃躲€傛墍鏈?native 鎿嶄綔鐢?bridge 鎵ц銆? */
public final class OpenCLRuntime implements AutoCloseable {
    public static final String ACCURATE_LATTICE_KERNEL = "plaincraft_streams_accurate_lattice";
    public static final String ACCURATE_SLOT_ZERO_PATCH_KERNEL = "plaincraft_streams_patch_slot_zero";
    public static final String ACCURATE_PRELIMINARY_INIT_KERNEL = "plaincraft_streams_accurate_preliminary_init";
    public static final String ACCURATE_PRELIMINARY_KERNEL = "plaincraft_streams_accurate_preliminary";
    public static final String ACCURATE_AQUIFER_KERNEL = "plaincraft_streams_accurate_aquifer";
    public static final String ACCURATE_HEIGHT_INIT_KERNEL = "plaincraft_streams_accurate_height_init";
    public static final String ACCURATE_HEIGHT_PARALLEL_KERNEL = "plaincraft_streams_accurate_height_parallel";
    public static final String ACCURATE_OCEAN_FLOOR_KERNEL = "plaincraft_streams_accurate_ocean_floor";
    public static final String ACCURATE_HEIGHT_KERNEL = "plaincraft_streams_accurate_height";

    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final String KERNEL_RESOURCE =
            "/assets/streamsreflowing/opencl/StreamsExactHeightKernel.cl";
    private static final Object RUNTIME_LOCK = new Object();
    private static final Map<RuntimeKey, OpenCLRuntime> RUNTIMES = new LinkedHashMap<>();

    private final Object operationLock = new Object();
    private final OpenCLSubmissionGate submissionGate = new OpenCLSubmissionGate();
    private final RuntimeKey key;
    private final OpenCLBridge.Session session;
    private final AtomicBoolean failed = new AtomicBoolean();

    private OpenCLRuntime(RuntimeKey key, OpenCLBridge.Session session) {
        this.key = key;
        this.session = session;
    }

    public static OpenCLRuntime tryCreate(OpenCLDevicePreference preference) {
        if (!OpenCLAvailability.isAvailable()) {
            return null;
        }
        OpenCLDevicePreference safePreference = preference == null ? OpenCLDevicePreference.AUTO : preference;
        RuntimeKey key = new RuntimeKey(safePreference, false, true);
        synchronized (RUNTIME_LOCK) {
            OpenCLRuntime current = findReusable(safePreference, false);
            if (current != null) {
                return current;
            }
            try {
                OpenCLRuntime runtime = open(key);
                if (runtime == null) {
                    OpenCLAvailability.disable("鏈彂鐜版敮鎸?FP64 鐨勫尮閰?OpenCL 璁惧");
                    OpenCLAvailability.logDisabledOnce(null);
                    return null;
                }
                RUNTIMES.put(key, runtime);
                LOGGER.info("OpenCL 鍦板舰閲囨牱杩愯鏃跺凡鍒濆鍖?preference={} device={} fp64={}",
                        safePreference,
                        runtime.deviceName(),
                        runtime.deviceInfo().fp64());
                return runtime;
            } catch (Throwable failure) {
                OpenCLAvailability.disable(runtimeFailureReason(failure), failure);
                return null;
            }
        }
    }

    public static OpenCLRuntime tryCreateAccurateGpu() {
        RuntimeKey key = new RuntimeKey(OpenCLDevicePreference.GPU, true, true);
        synchronized (RUNTIME_LOCK) {
            OpenCLRuntime current = findReusable(OpenCLDevicePreference.GPU, true);
            if (current != null) {
                return current;
            }
            try {
                OpenCLRuntime runtime = open(key);
                if (runtime != null) {
                    RUNTIMES.put(key, runtime);
                    OpenCLBridge.DeviceInfo device = runtime.deviceInfo();
                    LOGGER.info("Streams OpenCL device selected: name={}, gpu={}, fp64={}, globalMiB={}, maxAllocMiB={}, maxWorkGroup={}.",
                            device.name(), device.gpu(), device.fp64(),
                            device.globalMemoryBytes() / 1024L / 1024L,
                            device.maxAllocationBytes() / 1024L / 1024L,
                            device.maxWorkGroupSize());
                } else {
                    LOGGER.info("Streams OpenCL device selection found no GPU with required FP64 support.");
                }
                return runtime;
            } catch (Throwable failure) {
                LOGGER.warn("Streams OpenCL runtime initialization failed: {}",
                        failure.getMessage(), failure);
                return null;
            }
        }
    }

    private static OpenCLRuntime open(RuntimeKey key) throws IOException {
        OpenCLBridge.Session session = OpenCLBridge.open(new OpenCLBridge.OpenRequest(
                key.preference(),
                key.gpuOnly(),
                key.requireFp64(),
                readKernelSource(),
                List.of(ACCURATE_LATTICE_KERNEL, ACCURATE_SLOT_ZERO_PATCH_KERNEL,
                         ACCURATE_PRELIMINARY_INIT_KERNEL,
                         ACCURATE_PRELIMINARY_KERNEL,
                         ACCURATE_AQUIFER_KERNEL, ACCURATE_HEIGHT_INIT_KERNEL,
                         ACCURATE_HEIGHT_PARALLEL_KERNEL, ACCURATE_OCEAN_FLOOR_KERNEL,
                         ACCURATE_HEIGHT_KERNEL)));
        return session == null ? null : new OpenCLRuntime(key, session);
    }

    private static OpenCLRuntime findReusable(OpenCLDevicePreference preference, boolean gpuOnly) {
        if (!gpuOnly && preference == OpenCLDevicePreference.AUTO) {
            OpenCLRuntime gpu = findReusable(OpenCLDevicePreference.GPU, false);
            if (gpu != null) {
                return gpu;
            }
            return findReusable(OpenCLDevicePreference.CPU, false);
        }
        for (OpenCLRuntime runtime : RUNTIMES.values()) {
            if (!runtime.isUsable()) {
                continue;
            }
            boolean gpu = runtime.deviceInfo().gpu();
            if (gpuOnly && !gpu) {
                continue;
            }
            if (preference == OpenCLDevicePreference.GPU && !gpu) {
                continue;
            }
            if (preference == OpenCLDevicePreference.CPU && gpu) {
                continue;
            }
            return runtime;
        }
        return null;
    }

    private static String runtimeFailureReason(Throwable failure) {
        String message = failure == null ? "" : String.valueOf(failure.getMessage());
        if (message.startsWith("clBuildProgram failed")) {
            return "OpenCL kernel 缂栬瘧澶辫触";
        }
        if (message.startsWith("clCreateKernel")) {
            return "OpenCL kernel 鍒涘缓澶辫触";
        }
        return "OpenCL initialization failed";
    }

    public OpenCLDevicePreference devicePreference() {
        return key.preference();
    }

    public String deviceName() {
        return session.deviceInfo().name();
    }

    public OpenCLBridge.DeviceInfo deviceInfo() {
        return session.deviceInfo();
    }

    public Object operationLock() {
        return operationLock;
    }

    public <T> T submit(OpenCLSubmissionPriority priority, Supplier<T> operation) {
        if (!isUsable()) {
            throw new IllegalStateException("OpenCL device session is unavailable");
        }
        synchronized (operationLock) {
            return submissionGate.submit(priority, () -> {
                if (!isUsable()) {
                    throw new IllegalStateException("OpenCL device session became unavailable while waiting");
                }
                return operation.get();
            });
        }
    }

    public OpenCLBridge.DeviceBuffer upload(int[] values) {
        return session.upload(values);
    }

    public void writeInts(OpenCLBridge.DeviceBuffer buffer, int[] values) {
        session.writeInts(buffer, values);
    }

    public OpenCLBridge.DeviceBuffer upload(double[] values) {
        return session.upload(values);
    }

    public OpenCLBridge.DeviceBuffer allocate(OpenCLBridge.BufferAccess access, long bytes) {
        return session.allocate(access, bytes);
    }

    public long execute(String kernelName, List<OpenCLBridge.DeviceBuffer> arguments, long workItems) {
        if (!isUsable()) {
            throw new IllegalStateException("OpenCL device session is unavailable");
        }
        return session.execute(kernelName, arguments, workItems);
    }

    public OpenCLBridge.CommandEvent enqueue(String kernelName,
                                             List<OpenCLBridge.DeviceBuffer> arguments,
                                             long workItems) {
        return enqueue(kernelName, arguments, 0L, workItems);
    }

    public OpenCLBridge.CommandEvent enqueue(String kernelName,
                                             List<OpenCLBridge.DeviceBuffer> arguments,
                                             long workOffset,
                                             long workItems) {
        if (!isUsable()) {
            throw new IllegalStateException("OpenCL device session is unavailable");
        }
        return session.enqueue(kernelName, arguments, workOffset, workItems);
    }

    public int[] readInts(OpenCLBridge.DeviceBuffer buffer, int count) {
        return session.readInts(buffer, count);
    }

    public double[] readDoubles(OpenCLBridge.DeviceBuffer buffer, int count) {
        return session.readDoubles(buffer, count);
    }

    public boolean isUsable() {
        return !failed.get();
    }

    public void invalidate(Throwable failure) {
        if (!failed.compareAndSet(false, true)) {
            return;
        }
        LOGGER.warn("Streams OpenCL device session invalidated: device={}, reason={}",
                deviceName(), failure == null ? "unknown" : failure.getMessage());
        submissionGate.submit(OpenCLSubmissionPriority.MAINTENANCE, () -> {
            session.close();
            return null;
        });
    }

    @Override
    public void close() {
        // The shared server-level runtime is released by closeAll().
    }

    public static void closeSharedRuntimeForTests() {
        closeAll();
    }

    public static void closeAll() {
        synchronized (RUNTIME_LOCK) {
            OpenCLAccurateProgramCache.clear();
            Set<OpenCLRuntime> uniqueRuntimes = new LinkedHashSet<>(RUNTIMES.values());
            for (OpenCLRuntime runtime : uniqueRuntimes) {
                runtime.closeSession();
            }
            RUNTIMES.clear();
        }
    }

    private static String readKernelSource() throws IOException {
        try (InputStream stream = OpenCLRuntime.class.getResourceAsStream(KERNEL_RESOURCE)) {
            if (stream == null) {
                throw new IOException("missing kernel resource: " + KERNEL_RESOURCE);
            }
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return LithostitchedKernelSupport.inject(source);
        }
    }

    private void closeSession() {
        if (failed.compareAndSet(false, true)) {
            submissionGate.submit(OpenCLSubmissionPriority.MAINTENANCE, () -> {
                session.close();
                return null;
            });
        }
        submissionGate.close();
    }

    private record RuntimeKey(OpenCLDevicePreference preference, boolean gpuOnly, boolean requireFp64) {}
}

