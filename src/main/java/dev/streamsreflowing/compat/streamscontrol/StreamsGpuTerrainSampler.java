package dev.streamsreflowing.compat.streamscontrol;

import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateHeightSample;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateSamplingStats;
import dev.streamsreflowing.compat.streamscontrol.gpu.StreamsExactOpenCLBackend;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns Streams' exact OpenCL terrain sampler without calling RoadWeaver at runtime. */
public final class StreamsGpuTerrainSampler {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final int MIN_HYBRID_COLUMNS = 256;
    private static final int STARTUP_MIN_HYBRID_COLUMNS = 128;
    private static final int MIN_GPU_SUBMISSION_COLUMNS = 64;
    private static final int STARTUP_MIN_GPU_SUBMISSION_COLUMNS = 32;
    private static final int STARTUP_GPU_COLUMNS_PER_REQUEST = 256;
    private static final int FALLBACK_STARTUP_GPU_MIN_COLUMNS = 256;
    /**
     * Startup is a throughput phase. Keep a substantial CPU share for native
     * slot-zero preparation, while ensuring the GPU is continuously supplied.
     */
    private static final double FALLBACK_STARTUP_GPU_SHARE = 0.65D;
    private static final long MAX_GPU_OUTSTANDING_COLUMNS = 1_024L;
    private static final long FALLBACK_STARTUP_MAX_GPU_OUTSTANDING_COLUMNS = 2_048L;
    private static final int INITIAL_GPU_COLUMNS_PER_REQUEST = 64;
    private static final double INITIAL_GPU_SHARE = 0.10D;
    private static final double MAX_GPU_SHARE = 0.18D;
    private static final double MIN_GPU_SHARE = 0.05D;
    private static final Map<ServerLevel, Holder> HOLDERS = new ConcurrentHashMap<>();
    private static final Map<TerrainSampler, SamplerContext> CONTEXTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<String> LOGGED_DIAGNOSTICS = ConcurrentHashMap.newKeySet();
    private static final Object CREATION_LOCK = new Object();
    private static final AtomicInteger CPU_THREAD_ID = new AtomicInteger();

    private StreamsGpuTerrainSampler() {
    }

    public static boolean isAvailable(TerrainSampler sampler) {
        // GPU 采样被关闭时直接短路:不做任何反射探测、不恢复上下文、不刷
        // "context recovered" 日志。历史缺陷:先调 context(sampler) 反射遍历
        // 再检查开关,导致关闭 GPU 后每批采样仍反复跑昂贵的反射 + 刷屏。
        if (!StreamsPrewarmConfig.current().gpuTerrainSampling()) {
            return false;
        }
        SamplerContext context = context(sampler);
        if (context == null) {
            return false;
        }
        if (!StreamsPrewarmConfig.current().gpuTerrainSampling()) {
            logOnce("config-disabled", "Streams GPU terrain sampling is disabled by configuration.");
            close(context.level());
            return false;
        }
        return holder(context.level()).accelerated();
    }

    public static boolean sampleOceanFloor(TerrainSampler sampler, int[] xs, int[] zs,
                                           double[] output, int count) {
        if (count <= 0) {
            return true;
        }
        if (!StreamsPrewarmConfig.current().gpuTerrainSampling()) {
            return false;
        }
        SamplerContext context = context(sampler);
        if (context == null) {
            return false;
        }
        Holder holder = holder(context.level());
        if (!holder.accelerated()) {
            return false;
        }

        return sampleOceanFloorAccelerated(holder, context, sampler, xs, zs, output, count);
    }

    private static boolean sampleOceanFloorAccelerated(Holder holder, SamplerContext context,
                                                        TerrainSampler sampler,
                                                        int[] xs, int[] zs, double[] output, int count) {
        holder.enterSampling();
        try {
            long started = System.nanoTime();
            int[] sampleXs = new int[count];
            int[] sampleZs = new int[count];
            int[] outputIndexes = new int[count];
            int sampleCount = 0;
            for (int index = 0; index < count; index++) {
                if (context.isOcean(xs[index], zs[index])) {
                    output[index] = 0.0D;
                    continue;
                }
                sampleXs[sampleCount] = xs[index];
                sampleZs[sampleCount] = zs[index];
                outputIndexes[sampleCount] = index;
                sampleCount++;
            }

            GpuPlan gpuPlan = chooseGpuPlan(holder, sampleCount);
            int gpuCount = selectGpuChunks(
                    sampleXs, sampleZs, outputIndexes, sampleCount,
                    gpuPlan.columns(), gpuPlan.maxOutstandingColumns(),
                    gpuPlan.minimumColumns());
            int cpuCount = sampleCount - gpuCount;
            CompletableFuture<Map<Long, AccurateHeightSample>> gpuFuture = null;
            long gpuSubmitted = System.nanoTime();
            if (gpuCount > 0) {
                int gpuOffset = cpuCount;
                int[] gpuXs = java.util.Arrays.copyOfRange(sampleXs, gpuOffset, sampleCount);
                int[] gpuZs = java.util.Arrays.copyOfRange(sampleZs, gpuOffset, sampleCount);
                gpuFuture = holder.dispatcher.trySubmit(
                        gpuXs, gpuZs, gpuCount, gpuPlan.maxOutstandingColumns(),
                        context.landHeight()::height);
                if (gpuFuture == null) {
                    gpuCount = 0;
                    cpuCount = sampleCount;
                }
            }

            if (gpuFuture != null) {
                int cpuWorkCount = cpuCount;
                CompletableFuture<Void> cpuFuture = sampleCpuAsync(
                        holder, context, sampleXs, sampleZs, outputIndexes, output, cpuWorkCount);
                cpuFuture.join();
                Map<Long, AccurateHeightSample> samples = gpuFuture.join();
                copyGpuResults(sampleXs, sampleZs, outputIndexes, output,
                        cpuCount, sampleCount, samples);
                holder.gpuColumns.addAndGet(gpuCount);
                holder.gpuWaitNanos.addAndGet(System.nanoTime() - gpuSubmitted);
            } else {
                sampleCpuAsync(holder, context, sampleXs, sampleZs, outputIndexes,
                        output, sampleCount).join();
            }
            long elapsed = System.nanoTime() - started;
            long batch = holder.batches.incrementAndGet();
            long columns = holder.columns.addAndGet(count);
            long nanos = holder.nanos.addAndGet(elapsed);
            if (batch == 1 || batch % 8 == 0) {
                AccurateSamplingStats.BackendSnapshot stats = AccurateSamplingStats.backendSnapshot();
                LOGGER.info("Streams embedded exact GPU sampling: batches={}, columns={}, sampled={}, oceanSkipped={}, "
                                + "lastMs={}, totalMs={}, kernelMs={}, latticeMs={}, preliminaryMs={}, aquiferMs={}, "
                                + "heightMs={}, queueWaitMs={}, nativeSlot0CpuMs={}, slot0UploadMs={}, "
                                + "slot0PatchMs={}, cpuColumns={}, gpuColumns={}, cpuMs={}, gpuWaitMs={}, "
                                + "gpuShare={}%, activeSamplers={}, cpuPoolActive={}, cpuPoolQueue={}, "
                                + "gpuOutstanding={}, aggregateQueue={}, "
                                + "heightMode={}, device={}.",
                        batch, columns, sampleCount, count - sampleCount,
                        elapsed / 1_000_000L, nanos / 1_000_000L,
                        stats.gpuKernelNanos() / 1_000_000L,
                        stats.gpuLatticeKernelNanos() / 1_000_000L,
                        stats.gpuPreliminaryKernelNanos() / 1_000_000L,
                        stats.gpuAquiferKernelNanos() / 1_000_000L,
                        stats.gpuHeightKernelNanos() / 1_000_000L,
                        stats.gpuQueueWaitNanos() / 1_000_000L,
                        stats.nativeSlotZeroCpuNanos() / 1_000_000L,
                        stats.nativeSlotZeroUploadNanos() / 1_000_000L,
                        stats.nativeSlotZeroPatchNanos() / 1_000_000L,
                        holder.cpuColumns.get(), holder.gpuColumns.get(),
                        holder.cpuNanos.get() / 1_000_000L,
                        holder.gpuWaitNanos.get() / 1_000_000L,
                        Math.round(holder.gpuShare() * 100.0D),
                        holder.activeSamplers.get(), holder.cpuExecutor.getActiveCount(),
                        holder.cpuExecutor.getQueue().size(), holder.dispatcher.outstandingColumns(),
                        holder.dispatcher.queueDepth(),
                        holder.backend.serialHeightMode() ? "serial" : "parallel",
                        holder.backend.deviceName());
            }
            return true;
        } catch (CompletionException failure) {
            disable(holder, failure.getCause() == null ? failure : failure.getCause());
            sampler.heightsBatch(xs, zs, output, count);
            return true;
        } catch (Throwable failure) {
            disable(holder, failure);
            sampler.heightsBatch(xs, zs, output, count);
            return true;
        } finally {
            holder.leaveSampling();
        }
    }

    private static GpuPlan chooseGpuPlan(Holder holder, int sampleCount) {
        boolean startup = StreamsSamplerModeController.isInitialGeneration();
        int minimumHybridColumns = startup ? STARTUP_MIN_HYBRID_COLUMNS : MIN_HYBRID_COLUMNS;
        int minimumGpuColumns = startup ? STARTUP_MIN_GPU_SUBMISSION_COLUMNS : MIN_GPU_SUBMISSION_COLUMNS;
        if (sampleCount < minimumHybridColumns) {
            return GpuPlan.CPU_ONLY;
        }
        int gpuCount = (int) Math.round(sampleCount * holder.gpuShare());
        if (gpuCount < minimumGpuColumns) {
            return GpuPlan.CPU_ONLY;
        }

        if (startup) {
            // Startup is a throughput phase. Give the GPU a real batch first,
            // then let the CPU process the remaining columns in parallel.
            StartupGpuBudget budget = startupGpuBudget(holder);
            if (sampleCount >= budget.minimumColumnsPerRequest()) {
                gpuCount = Math.max(gpuCount, budget.minimumColumnsPerRequest());
            }
            return new GpuPlan(
                    Math.min(gpuCount, budget.maxColumnsPerRequest()),
                    budget.maxOutstandingColumns(),
                    minimumGpuColumns);
        }

        double cpuRate = holder.cpuThreadRate();
        double gpuRate = holder.gpuRate();
        long maxOutstanding = MAX_GPU_OUTSTANDING_COLUMNS;
        if (cpuRate <= 0.0D || gpuRate <= 0.0D) {
            gpuCount = Math.min(gpuCount, INITIAL_GPU_COLUMNS_PER_REQUEST);
        } else {
            int activeRequests = Math.max(1, holder.activeSamplers.get());
            int totalCpuWorkers = Math.max(1, StreamsSamplerModeController.desiredThreadLimit());
            int requestCpuParallelism = Math.max(1, totalCpuWorkers / activeRequests);
            double cpuSeconds = (sampleCount - gpuCount)
                    / (cpuRate * requestCpuParallelism);
            maxOutstanding = Math.min(MAX_GPU_OUTSTANDING_COLUMNS,
                    Math.max(MIN_GPU_SUBMISSION_COLUMNS,
                            (long) Math.floor(gpuRate * cpuSeconds * 1.15D)));
        }
        gpuCount = Math.min(gpuCount, (int) maxOutstanding);
        return new GpuPlan(gpuCount, maxOutstanding, minimumGpuColumns);
    }

    private static int selectGpuChunks(int[] xs, int[] zs, int[] outputIndexes,
                                       int count, int targetColumns, long maxColumns,
                                       int minimumColumns) {
        if (targetColumns <= 0 || maxColumns < minimumColumns) {
            return 0;
        }

        Map<Long, List<Integer>> indexesByChunk = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            long chunk = net.minecraft.world.level.ChunkPos.asLong(xs[index] >> 4, zs[index] >> 4);
            indexesByChunk.computeIfAbsent(chunk, ignored -> new ArrayList<>()).add(index);
        }
        List<List<Integer>> groups = new ArrayList<>(indexesByChunk.values());
        groups.sort(Comparator.comparingInt((List<Integer> group) -> group.size()).reversed());

        boolean[] gpu = new boolean[count];
        int selected = 0;
        int budget = (int) Math.min(Integer.MAX_VALUE, maxColumns);
        for (List<Integer> group : groups) {
            if (selected >= targetColumns) {
                break;
            }
            if (selected + group.size() > budget) {
                continue;
            }
            for (int index : group) {
                gpu[index] = true;
            }
            selected += group.size();
        }
        if (selected < minimumColumns) {
            return 0;
        }

        int[] orderedXs = new int[count];
        int[] orderedZs = new int[count];
        int[] orderedOutputIndexes = new int[count];
        int cpuWrite = 0;
        int gpuWrite = count - selected;
        for (int index = 0; index < count; index++) {
            int write = gpu[index] ? gpuWrite++ : cpuWrite++;
            orderedXs[write] = xs[index];
            orderedZs[write] = zs[index];
            orderedOutputIndexes[write] = outputIndexes[index];
        }
        System.arraycopy(orderedXs, 0, xs, 0, count);
        System.arraycopy(orderedZs, 0, zs, 0, count);
        System.arraycopy(orderedOutputIndexes, 0, outputIndexes, 0, count);
        return selected;
    }

    private static void sampleCpu(SamplerContext context,
                                  int[] xs, int[] zs, int[] outputIndexes,
                                  double[] output, int from, int to) {
        for (int index = from; index < to; index++) {
            output[outputIndexes[index]] = context.landHeight().height(xs[index], zs[index]);
        }
    }

    private static CompletableFuture<Void> sampleCpuAsync(
            Holder holder, SamplerContext context,
            int[] xs, int[] zs, int[] outputIndexes, double[] output, int count) {
        return sampleCpuAsync(holder, context, xs, zs, outputIndexes, output, 0, count);
    }

    private static CompletableFuture<Void> sampleCpuAsync(
            Holder holder, SamplerContext context,
            int[] xs, int[] zs, int[] outputIndexes, double[] output, int from, int to) {
        int count = to - from;
        if (count <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        int desiredWorkers = Math.max(1, StreamsSamplerModeController.desiredThreadLimit());
        int tasks = Math.min(desiredWorkers, Math.max(1, (count + 31) / 32));
        CompletableFuture<?>[] futures = new CompletableFuture<?>[tasks];
        for (int task = 0; task < tasks; task++) {
            int taskFrom = from + task * count / tasks;
            int taskTo = from + (task + 1) * count / tasks;
            futures[task] = CompletableFuture.runAsync(() -> {
                long started = System.nanoTime();
                sampleCpu(context, xs, zs, outputIndexes, output, taskFrom, taskTo);
                holder.cpuColumns.addAndGet(taskTo - taskFrom);
                holder.cpuNanos.addAndGet(System.nanoTime() - started);
            }, holder.cpuExecutor());
        }
        return CompletableFuture.allOf(futures);
    }

    private static void copyGpuResults(int[] xs, int[] zs, int[] outputIndexes,
                                       double[] output, int from, int to,
                                       Map<Long, AccurateHeightSample> samples) {
        for (int index = from; index < to; index++) {
            AccurateHeightSample sample = samples.get(AccurateHeightSample.key(xs[index], zs[index]));
            if (sample == null) {
                throw new IllegalStateException("OpenCL omitted terrain column "
                        + xs[index] + "," + zs[index]);
            }
            output[outputIndexes[index]] = sample.oceanFloorWg();
        }
    }

    public static void close(ServerLevel level) {
        Holder holder = HOLDERS.remove(level);
        if (holder != null) {
            holder.close();
        }
        if (HOLDERS.isEmpty()) {
            StreamsExactOpenCLBackend.closeRuntime();
        }
    }

    private static Holder holder(ServerLevel level) {
        Holder existing = HOLDERS.get(level);
        if (existing != null) {
            return existing;
        }
        synchronized (CREATION_LOCK) {
            existing = HOLDERS.get(level);
            if (existing != null) {
                return existing;
            }
            if (HOLDERS.isEmpty()) {
                AccurateSamplingStats.reset();
            }
            StreamsExactOpenCLBackend.Creation creation = StreamsExactOpenCLBackend.create(level);
            StreamsExactOpenCLBackend backend = creation.backend();
            Holder created = creation.successful() ? new Holder(backend, true) : Holder.unavailable();
            HOLDERS.put(level, created);
            if (!creation.successful()) {
                if (creation.failure() == null) {
                    LOGGER.info("Streams embedded exact GPU sampling unavailable after {} ms; using native CPU sampling. reason={}",
                            creation.initializationNanos() / 1_000_000L, creation.reason());
                } else {
                    LOGGER.warn("Streams embedded exact GPU sampling unavailable after {} ms; using native CPU sampling. reason={}",
                            creation.initializationNanos() / 1_000_000L, creation.reason(), creation.failure());
                }
            } else {
                LOGGER.info("Streams embedded exact GPU sampling initialized in {} ms: device={}.",
                        creation.initializationNanos() / 1_000_000L, backend.deviceName());
            }
            return created;
        }
    }

    private static void disable(Holder holder, Throwable failure) {
        if (!holder.accelerated) {
            return;
        }
        holder.accelerated = false;
        holder.dispatcher.close();
        holder.backend.invalidate(failure);
        LOGGER.warn("Streams embedded exact GPU sampling disabled; reverting to native CPU sampling.", failure);
    }

    private static SamplerContext context(TerrainSampler sampler) {
        if (sampler == null) {
            return null;
        }
        SamplerContext cached = CONTEXTS.get(sampler);
        if (cached != null) {
            return cached;
        }
        SamplerContext found = findContext(sampler, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        if (found != null) {
            CONTEXTS.put(sampler, found);
        } else {
            logOnce("sampler-chain:" + sampler.getClass().getName(),
                    "Streams GPU sampling could not locate an exact StreamsWorldgen sampler. chain="
                            + samplerChain(sampler));
        }
        return found;
    }

    private static SamplerContext findContext(TerrainSampler sampler, Set<Object> visited, int depth) {
        if (sampler == null || depth > 10 || !visited.add(sampler)) {
            return null;
        }
        if (sampler.getClass().getName().equals("dev.streamsreflowing.worldgen.StreamsWorldgen$1")) {
            return inspectBaseSampler(sampler);
        }
        for (Field field : allFields(sampler.getClass())) {
            if (!TerrainSampler.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(sampler);
                if (nested instanceof TerrainSampler child) {
                    SamplerContext context = findContext(child, visited, depth + 1);
                    if (context != null) {
                        return context;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static SamplerContext inspectBaseSampler(TerrainSampler sampler) {
        ServerLevel level = read(sampler, "val$level", ServerLevel.class);
        TerrainSampler landHeight = read(sampler, "val$landHeight", TerrainSampler.class);
        TerrainSampler realHeight = read(sampler, "val$realHeight", TerrainSampler.class);
        Boolean fastOceanSkip = read(sampler, "val$fastOceanSkip", Boolean.class);
        BiomeSource biomeSource = read(sampler, "val$biomeSource", BiomeSource.class);
        Climate.Sampler climateSampler = read(sampler, "val$climateSampler", Climate.Sampler.class);
        Integer oceanProbeY = read(sampler, "val$oceanProbeY", Integer.class);

        // Streams 2.12.x is compiled without the captured-variable field names.
        // Its anonymous sampler keeps the same semantic layout, so recover only
        // the fields needed for exact GPU sampling by type and adjacent fields.
        if (level == null || landHeight == null || realHeight == null || fastOceanSkip == null) {
            ObfuscatedSamplerFields fields = inspectObfuscatedSampler(sampler);
            if (fields != null) {
                if (level == null) {
                    level = fields.level();
                }
                if (landHeight == null) {
                    landHeight = fields.landHeight();
                }
                if (realHeight == null) {
                    realHeight = fields.realHeight();
                }
                if (fastOceanSkip == null) {
                    fastOceanSkip = fields.fastOceanSkip();
                }
                if (biomeSource == null) {
                    biomeSource = fields.biomeSource();
                }
                if (climateSampler == null) {
                    climateSampler = fields.climateSampler();
                }
                if (oceanProbeY == null) {
                    oceanProbeY = fields.oceanProbeY();
                }
            }
        }
        if (level == null || landHeight == null || realHeight == null || landHeight != realHeight
                || fastOceanSkip == null) {
            if (landHeight != null && realHeight != null && landHeight != realHeight) {
                logOnce("non-exact-preset",
                        "Streams GPU sampling bypassed because the selected terrain preset is not full accuracy; "
                                + "the selected Streams algorithm remains authoritative.");
            } else {
                logOnce("base-fields-missing",
                        "Streams GPU sampling found StreamsWorldgen$1 but required runtime fields were missing: "
                                + "level=" + (level != null) + ", landHeight=" + (landHeight != null)
                                + ", realHeight=" + (realHeight != null)
                                + ", fastOceanSkip=" + (fastOceanSkip != null) + ".");
            }
            return null;
        }
        if (fastOceanSkip && (biomeSource == null || climateSampler == null || oceanProbeY == null)) {
            logOnce("ocean-fields-missing",
                    "Streams GPU sampling could not preserve fast-ocean skipping because biome runtime fields were missing.");
            return null;
        }
        logOnce("sampler-context-ready",
                "Streams GPU sampler context resolved: dimension=" + level.dimension().location()
                        + ", fastOceanSkip=" + fastOceanSkip + ", exactTerrain=true.");
        return new SamplerContext(level, landHeight, fastOceanSkip, biomeSource, climateSampler,
                oceanProbeY == null ? 0 : oceanProbeY);
    }

    private static ObfuscatedSamplerFields inspectObfuscatedSampler(TerrainSampler sampler) {
        List<Field> fields = allFields(sampler.getClass());
        ServerLevel level = null;
        BiomeSource biomeSource = null;
        Climate.Sampler climateSampler = null;
        Integer oceanProbeY = null;
        TerrainSampler landHeight = null;
        TerrainSampler realHeight = null;
        Boolean fastOceanSkip = null;
        int fastFlagIndex = -1;
        int correctionIndex = -1;

        for (int index = 0; index < fields.size(); index++) {
            Field field = fields.get(index);
            Object value;
            try {
                field.setAccessible(true);
                value = field.get(sampler);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                continue;
            }
            if (level == null && value instanceof ServerLevel serverLevel) {
                level = serverLevel;
            }
            if (biomeSource == null && value instanceof BiomeSource source) {
                biomeSource = source;
            }
            if (climateSampler == null && value instanceof Climate.Sampler climate) {
                climateSampler = climate;
            }
            if (field.getType() == boolean.class && index + 1 < fields.size()
                    && TerrainSampler.class.isAssignableFrom(fields.get(index + 1).getType())
                    && index > 0 && fields.get(index - 1).getType().getName().contains("BiomeClass")) {
                fastFlagIndex = index;
                fastOceanSkip = (Boolean) value;
                landHeight = readFieldValue(sampler, fields.get(index + 1), TerrainSampler.class);
            }
            if (oceanProbeY == null && field.getType() == boolean.class && index > 0
                    && TerrainSampler.class.isAssignableFrom(fields.get(index - 1).getType())
                    && index + 1 < fields.size() && fields.get(index + 1).getType() == int.class) {
                oceanProbeY = readFieldValue(sampler, fields.get(index + 1), Integer.class);
            }
            if (field.getType().getName().contains("BandCorrection")) {
                correctionIndex = index;
            }
        }
        if (correctionIndex >= 0 && correctionIndex + 1 < fields.size()) {
            realHeight = readFieldValue(sampler, fields.get(correctionIndex + 1), TerrainSampler.class);
        }
        if (fastFlagIndex < 0 || level == null || landHeight == null || realHeight == null
                || fastOceanSkip == null) {
            return null;
        }
        LOGGER.info("Streams GPU sampler context recovered from obfuscated Streams 2.12 layout.");
        return new ObfuscatedSamplerFields(level, biomeSource, climateSampler, oceanProbeY,
                landHeight, realHeight, fastOceanSkip);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readFieldValue(Object owner, Field field, Class<T> type) {
        try {
            field.setAccessible(true);
            Object value = field.get(owner);
            return value == null || !type.isInstance(value) ? null : (T) value;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String samplerChain(TerrainSampler sampler) {
        List<String> chain = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        TerrainSampler current = sampler;
        while (current != null && chain.size() < 12 && visited.add(current)) {
            chain.add(current.getClass().getName());
            TerrainSampler next = null;
            for (Field field : allFields(current.getClass())) {
                if (!TerrainSampler.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.get(current) instanceof TerrainSampler nested) {
                        next = nested;
                        break;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
            current = next;
        }
        return String.join(" -> ", chain);
    }

    private static void logOnce(String key, String message) {
        if (LOGGED_DIAGNOSTICS.add(key)) {
            LOGGER.info(message);
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Collections.addAll(fields, current.getDeclaredFields());
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(Object owner, String name, Class<T> boxedType) {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(owner);
                return value == null || !boxedType.isInstance(value) ? null : (T) value;
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException | RuntimeException failure) {
                return null;
            }
        }
        return null;
    }

    private record SamplerContext(ServerLevel level, TerrainSampler landHeight,
                                  boolean fastOceanSkip, BiomeSource biomeSource,
                                  Climate.Sampler climateSampler, int oceanProbeY) {
        boolean isOcean(int x, int z) {
            return fastOceanSkip && biomeSource.getNoiseBiome(
                    QuartPos.fromBlock(x), oceanProbeY, QuartPos.fromBlock(z), climateSampler)
                    .is(BiomeTags.IS_OCEAN);
        }
    }

    private record ObfuscatedSamplerFields(ServerLevel level, BiomeSource biomeSource,
                                           Climate.Sampler climateSampler, Integer oceanProbeY,
                                           TerrainSampler landHeight, TerrainSampler realHeight,
                                           Boolean fastOceanSkip) {
    }

    private record GpuPlan(int columns, long maxOutstandingColumns, int minimumColumns) {
        private static final GpuPlan CPU_ONLY = new GpuPlan(0, 0L, Integer.MAX_VALUE);
    }

    private static final class Holder {
        private final StreamsExactOpenCLBackend backend;
        private final AtomicLong batches = new AtomicLong();
        private final AtomicLong columns = new AtomicLong();
        private final AtomicLong nanos = new AtomicLong();
        private final AtomicLong cpuColumns = new AtomicLong();
        private final AtomicLong gpuColumns = new AtomicLong();
        private final AtomicLong cpuNanos = new AtomicLong();
        private final AtomicLong gpuWaitNanos = new AtomicLong();
        private final AtomicInteger activeSamplers = new AtomicInteger();
        private final StreamsGpuBatchDispatcher dispatcher;
        private final ThreadPoolExecutor cpuExecutor;
        private volatile boolean accelerated;

        private Holder(StreamsExactOpenCLBackend backend, boolean accelerated) {
            this.backend = backend;
            this.accelerated = accelerated;
            this.dispatcher = backend == null ? null : new StreamsGpuBatchDispatcher(backend);
            this.cpuExecutor = backend == null ? null : createCpuExecutor();
        }

        private static Holder unavailable() {
            return new Holder(null, false);
        }

        private boolean accelerated() {
            return accelerated && backend != null && backend.isUsable();
        }

        private double gpuShare() {
            if (StreamsSamplerModeController.isInitialGeneration()) {
                StartupGpuBudget budget = startupGpuBudget(this);
                double target = budget.targetShare();
                double cpuRate = cpuThreadRate();
                double gpuRate = gpuRate();
                if (cpuRate > 0.0D && gpuRate > 0.0D) {
                    // One producer must keep the device fed; the remaining CPU
                    // workers continue native preparation in parallel. Do not
                    // multiply by all CPU workers here, or the adaptive policy
                    // would incorrectly starve the GPU on high-core machines.
                    double measured = gpuRate / (gpuRate + cpuRate);
                    target = clampDouble((target + measured) * 0.5D,
                            0.55D, 0.85D);
                }
                return target;
            }
            double cpuRate = cpuThreadRate();
            double gpuRate = gpuRate();
            if (cpuRate <= 0.0D || gpuRate <= 0.0D) {
                return INITIAL_GPU_SHARE;
            }
            int cpuWorkers = Math.max(1, StreamsSamplerModeController.desiredThreadLimit());
            double share = gpuRate / (gpuRate + cpuRate * cpuWorkers);
            return Math.max(MIN_GPU_SHARE, Math.min(MAX_GPU_SHARE, share));
        }

        private double cpuThreadRate() {
            long columns = cpuColumns.get();
            long elapsed = cpuNanos.get();
            return columns == 0L || elapsed == 0L
                    ? 0.0D : columns * 1_000_000_000.0D / elapsed;
        }

        private double gpuRate() {
            long columns = dispatcher == null ? 0L : dispatcher.completedUniqueColumns();
            long elapsed = dispatcher == null ? 0L : dispatcher.completedGpuNanos();
            return columns == 0L || elapsed == 0L
                    ? 0.0D : columns * 1_000_000_000.0D / elapsed;
        }

        private synchronized ThreadPoolExecutor cpuExecutor() {
            int desired = Math.max(1, StreamsSamplerModeController.desiredThreadLimit());
            int currentMaximum = cpuExecutor.getMaximumPoolSize();
            if (currentMaximum != desired) {
                if (desired > currentMaximum) {
                    cpuExecutor.setMaximumPoolSize(desired);
                    cpuExecutor.setCorePoolSize(desired);
                } else {
                    cpuExecutor.setCorePoolSize(desired);
                    cpuExecutor.setMaximumPoolSize(desired);
                }
                cpuExecutor.prestartAllCoreThreads();
            }
            return cpuExecutor;
        }

        private static ThreadPoolExecutor createCpuExecutor() {
            int workers = Math.max(1, StreamsSamplerModeController.desiredThreadLimit());
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    workers, workers, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), task -> {
                        Thread thread = new Thread(task,
                                "plaincraft-streams-exact-cpu-" + CPU_THREAD_ID.incrementAndGet());
                        thread.setDaemon(true);
                        // 0.1.73：CPU 采样是真正的重活（每 plate 上千采样点），
                        // 降到最低优先级，避免与主线程抢核导致游玩掉帧。此前 NORM_PRIORITY
                        // 与主线程同优先级抢核，是「一移动就掉帧/突然高占用」的根因之一。
                        thread.setPriority(Thread.MIN_PRIORITY);
                        return thread;
                    });
            executor.prestartAllCoreThreads();
            return executor;
        }

        private void enterSampling() {
            activeSamplers.incrementAndGet();
        }

        private void leaveSampling() {
            activeSamplers.decrementAndGet();
        }

        private void close() {
            accelerated = false;
            if (dispatcher != null) {
                dispatcher.close();
            }
            if (cpuExecutor != null) {
                cpuExecutor.shutdownNow();
            }
            if (backend != null) {
                backend.close();
            }
        }
    }

    private record StartupGpuBudget(int minimumColumnsPerRequest,
                                    int maxColumnsPerRequest,
                                    long maxOutstandingColumns,
                                    double targetShare,
                                    long globalMemoryMiB,
                                    long maxAllocationMiB) {
    }

    private static StartupGpuBudget startupGpuBudget(Holder holder) {
        if (holder.backend == null) {
            return new StartupGpuBudget(
                    FALLBACK_STARTUP_GPU_MIN_COLUMNS,
                    STARTUP_GPU_COLUMNS_PER_REQUEST,
                    FALLBACK_STARTUP_MAX_GPU_OUTSTANDING_COLUMNS,
                    FALLBACK_STARTUP_GPU_SHARE,
                    0L,
                    0L);
        }
        var device = holder.backend.deviceInfo();
        long globalMiB = Math.max(1L, device.globalMemoryBytes() / (1024L * 1024L));
        long maxAllocationMiB = Math.max(1L,
                device.maxAllocationBytes() / (1024L * 1024L));

        // Scale the startup batch from reported GPU memory, with conservative
        // bounds for integrated/low-memory devices and large discrete GPUs.
        int minimum = clampInt((int) (globalMiB / 32L), 128, 512);
        int maxPerRequest = clampInt((int) (globalMiB / 8L), 256, 1_024);
        maxPerRequest = Math.min(maxPerRequest,
                clampInt((int) Math.min(Integer.MAX_VALUE, maxAllocationMiB * 2L),
                        256, 1_024));
        long outstanding = clampLong(globalMiB / 4L, 1_024L, 8_192L);

        // This is a startup target, not a fixed GPU model table. Larger GPU
        // memory allows a larger continuously-fed share while CPU handles the
        // remainder in parallel.
        double targetShare = clampDouble(0.55D + Math.min(globalMiB, 24_576L)
                / 24_576.0D * 0.30D, 0.55D, 0.85D);
        return new StartupGpuBudget(minimum, maxPerRequest, outstanding,
                targetShare, globalMiB, maxAllocationMiB);
    }

    private static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static long clampLong(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clampDouble(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
