package dev.streamsreflowing.compat.streamscontrol;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StreamsSamplerModeController {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams Safety");
    private static final int GAMEPLAY_PLATE_CACHE_CAPACITY = 128;
    private static final int CHUNKY_SUPPLY_PLATE_CACHE_CAPACITY = 256;
    private static final int MAX_RUNTIME_PLATE_CACHE_CAPACITY = 1024;
    private static final int MAX_INITIAL_GENERATION_PLATE_CACHE_CAPACITY = 1024;
    private static final AtomicInteger INITIAL_GENERATION_DEPTH = new AtomicInteger();
    private static final AtomicInteger INITIAL_GENERATION_PLATE_CACHE_CAPACITY = new AtomicInteger(512);
    private static final AtomicInteger RUNTIME_PLATE_CACHE_CAPACITY =
            new AtomicInteger(GAMEPLAY_PLATE_CACHE_CAPACITY);
    private static final AtomicInteger CHUNKY_SUPPLY_THREADS = new AtomicInteger();
    private static final AtomicInteger RUNTIME_ADAPTIVE_THREADS = new AtomicInteger();
    private static final AtomicBoolean CHUNKY_EXCLUSIVE = new AtomicBoolean();

    private StreamsSamplerModeController() {
    }

    public static int desiredThreadLimit() {
        if (CHUNKY_EXCLUSIVE.get()) return 0;
        StreamsPrewarmConfig config = StreamsPrewarmConfig.current();
        int adaptiveRuntime = RUNTIME_ADAPTIVE_THREADS.get();
        int runtimeThreads = adaptiveRuntime > 0 ? adaptiveRuntime : config.runtimeSamplerThreads();
        return INITIAL_GENERATION_DEPTH.get() > 0
                ? config.startupSamplerThreads()
                : resolveRuntimeThreadLimit(runtimeThreads, CHUNKY_SUPPLY_THREADS.get());
    }

    /**
     * Java ThreadPoolExecutor rejects a maximum size of zero. Chunky exclusive
     * mode therefore pauses work at the admission layer while keeping the
     * physical sampler executor at its legal minimum.
     */
    public static int threadPoolThreadLimit() {
        return Math.max(1, desiredThreadLimit());
    }

    static int resolveRuntimeThreadLimit(int adaptiveThreads, int supplyThreads) {
        int adaptive = Math.max(1, adaptiveThreads);
        return supplyThreads <= 0 ? adaptive : Math.min(adaptive, Math.max(1, supplyThreads));
    }

    static boolean isInitialGeneration() {
        return INITIAL_GENERATION_DEPTH.get() > 0;
    }

    /**
     * Chunky owns the world-generation budget after RoadWeaver readiness.
     * Streams runtime work is resumed when the task ends; startup generation
     * remains unaffected because this flag is only set after task dispatch.
     */
    public static void setChunkyExclusive(boolean active) {
        boolean previous = CHUNKY_EXCLUSIVE.getAndSet(active);
        if (previous == active) return;
        requestSamplerResize(active ? 0 : desiredThreadLimit());
        LOGGER.info("Streams Reflowing Chunky exclusive mode {}.", active ? "enabled" : "disabled");
    }

    static boolean isChunkyExclusive() {
        return CHUNKY_EXCLUSIVE.get();
    }

    public static int desiredPlateCacheCapacity() {
        int runtimeOwners = Math.max(CHUNKY_SUPPLY_THREADS.get(), RUNTIME_ADAPTIVE_THREADS.get());
        return INITIAL_GENERATION_DEPTH.get() > 0
                ? INITIAL_GENERATION_PLATE_CACHE_CAPACITY.get()
                : runtimeOwners > StreamsPrewarmConfig.current().runtimeSamplerThreads()
                ? Math.max(CHUNKY_SUPPLY_PLATE_CACHE_CAPACITY, RUNTIME_PLATE_CACHE_CAPACITY.get())
                : RUNTIME_PLATE_CACHE_CAPACITY.get();
    }

    public static void enterInitialGeneration() {
        enterInitialGeneration(0);
    }

    public static void enterInitialGeneration(int requiredPlates) {
        ensureInitialPlateCacheCapacity(requiredPlates);
        if (INITIAL_GENERATION_DEPTH.getAndIncrement() == 0) {
            int startupThreads = StreamsPrewarmConfig.current().startupSamplerThreads();
            requestSamplerResize(startupThreads);
            LOGGER.info("Streams Reflowing sampler startup boost enabled: {} workers.",
                    startupThreads);
        }
    }

    public static void ensureInitialPlateCacheCapacity(int requiredPlates) {
        if (requiredPlates <= 0) return;
        int capacity = Math.min(MAX_INITIAL_GENERATION_PLATE_CACHE_CAPACITY,
                Math.max(512, requiredPlates + 32));
        INITIAL_GENERATION_PLATE_CACHE_CAPACITY.accumulateAndGet(capacity, Math::max);
    }

    public static void exitInitialGeneration() {
        int remaining = INITIAL_GENERATION_DEPTH.updateAndGet(value -> Math.max(0, value - 1));
        if (remaining == 0) {
            int desired = desiredThreadLimit();
            requestSamplerResize(desired);
            LOGGER.info("Streams Reflowing sampler runtime limit restored: {} worker(s).", desired);
        }
    }

    public static void ensureRuntimePlateCacheCapacity(int requiredPlates) {
        if (requiredPlates <= 0) return;
        int capacity = Math.min(MAX_RUNTIME_PLATE_CACHE_CAPACITY,
                Math.max(GAMEPLAY_PLATE_CACHE_CAPACITY, requiredPlates + 32));
        RUNTIME_PLATE_CACHE_CAPACITY.accumulateAndGet(capacity, Math::max);
    }

    /** Called reflectively by RoadWeaver's Chunky readiness supply controller. */
    public static void setChunkySupplyThreads(int requestedThreads) {
        int desired = requestedThreads <= 0 ? 0 : Math.max(1,
                Math.min(StreamsRuntimeLoadPolicy.hardMaximumThreads(), requestedThreads));
        int previous = CHUNKY_SUPPLY_THREADS.getAndSet(desired);
        if (previous == desired || INITIAL_GENERATION_DEPTH.get() > 0) return;
        requestSamplerResize(desiredThreadLimit());
        LOGGER.info("Streams Reflowing Chunky supply capacity changed: {} -> {} worker(s).",
                previous, desired);
    }

    static void setRuntimeAdaptiveThreads(int requestedThreads) {
        int desired = Math.max(1, Math.min(
                StreamsRuntimeLoadPolicy.hardMaximumThreads(), requestedThreads));
        int previous = RUNTIME_ADAPTIVE_THREADS.getAndSet(desired);
        if (previous == desired || INITIAL_GENERATION_DEPTH.get() > 0) return;
        requestSamplerResize(desiredThreadLimit());
        LOGGER.info("Streams Reflowing runtime adaptive capacity changed: {} -> {} worker(s).",
                previous, desired);
    }

    private static void requestSamplerResize(int requestedThreads) {
        try {
            Class<?> lowResGrid = Class.forName("dev.streamsreflowing.core.grid.LowResGrid");
            Method resize = lowResGrid.getMethod("setBackgroundThrottle", int.class);
            resize.invoke(null, requestedThreads);
        } catch (ReflectiveOperationException failure) {
            LOGGER.warn("Could not update Streams Reflowing sampler worker limit", failure);
        }
    }
}
