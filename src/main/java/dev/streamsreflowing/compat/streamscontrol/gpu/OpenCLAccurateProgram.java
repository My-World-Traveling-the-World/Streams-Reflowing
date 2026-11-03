/* 鏂囦欢鑱岃矗锛氱紪璇戝苟鎵ц鍗曚釜涓栫晫鐢熸垚璁剧疆瀵瑰簲鐨?OpenCL 绮剧‘楂樺害绋嬪簭銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.util.CubicSpline;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateHeightChunk;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateHeightGrid;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateHeightGridRequest;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateHeightSample;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateSamplingProgress;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateSamplingStats;
import dev.streamsreflowing.compat.streamscontrol.gpu.bridge.OpenCLBridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 涓庝竴涓?RandomState 缁戝畾鐨勭簿纭噰鏍?GPU 绋嬪簭銆? */
final class OpenCLAccurateProgram implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final int HEIGHT_PARALLEL_LANES = 64;
    private static final int MAX_SLOT_ZERO_CACHE_CHUNKS = 4096;

    private final DensityGraphProgram graph;
    private final OpenCLDensityProgramPayload payload;
    private final NoiseSettings noiseSettings;
    private final RandomState randomState;
    private final NoiseGeneratorSettings generatorSettings;
    private final Aquifer.FluidPicker fluidPicker;
    private final PositionalRandomFactory aquiferRandom;
    private final boolean aquifersEnabled;
    private final int seaLevel;
    private final int defaultFluidKind;
    private final int defaultBlockMask;
    private final int defaultFluidMask;
    private final int lavaMask;
    private final IdentityHashMap<OpenCLRuntime, OpenCLDensityProgramBuffers> buffers = new IdentityHashMap<>();
    private final IdentityHashMap<OpenCLRuntime, OpenCLAccurateWorkspace> workspaces = new IdentityHashMap<>();
    private final ExecutorService slotZeroPrefetchExecutor = Executors.newSingleThreadExecutor(task ->
            Thread.ofPlatform().name("plaincraft-streams-slot0-prefetch").daemon(true).unstarted(task));
    private final AtomicBoolean preliminarySegmentationLogged = new AtomicBoolean();
    private final AtomicBoolean sparseAquiferLogged = new AtomicBoolean();
    private final Map<Long, double[]> slotZeroCache = new ConcurrentHashMap<>();
    private final AtomicLong slotZeroCacheHits = new AtomicLong();
    private final AtomicLong slotZeroCacheMisses = new AtomicLong();
    private boolean oceanFloorKernelValidated;
    private boolean closed;

    private OpenCLAccurateProgram(DensityGraphProgram graph,
                                  NoiseSettings noiseSettings,
                                  RandomState randomState,
                                  PositionalRandomFactory aquiferRandom,
                                  NoiseGeneratorSettings settings,
                                  int defaultBlockMask,
                                  int defaultFluidMask,
                                  int lavaMask) {
        this.graph = graph;
        this.payload = OpenCLDensityProgramPayload.from(graph);
        this.noiseSettings = noiseSettings;
        this.randomState = randomState;
        this.generatorSettings = settings;
        this.fluidPicker = createFluidPicker(settings);
        this.aquiferRandom = aquiferRandom;
        this.aquifersEnabled = settings.isAquifersEnabled();
        this.seaLevel = settings.seaLevel();
        this.defaultFluidKind = HeightmapMaterialClassifier.fluidKind(settings.defaultFluid());
        this.defaultBlockMask = defaultBlockMask;
        this.defaultFluidMask = defaultFluidMask;
        this.lavaMask = lavaMask;
    }

    static CompileResult compile(ServerLevel level, NoiseBasedChunkGenerator generator, RandomState randomState) {
        return compile(level, generator.generatorSettings().value(), randomState);
    }

    static CompileResult compile(LevelHeightAccessor level,
                                 NoiseGeneratorSettings settings,
                                 RandomState randomState) {
        try {
            int defaultBlockMask = HeightmapMaterialClassifier.mask(settings.defaultBlock());
            if (defaultBlockMask != HeightmapMaterialClassifier.REQUIRED_SOLID_MASK) {
                return CompileResult.unsupported("default block heightmap predicates are not solid");
            }
            if (settings.oreVeinsEnabled() && !HeightmapMaterialClassifier.oreVeinOutputsMatch(defaultBlockMask)) {
                return CompileResult.unsupported("ore vein material predicates differ from default block");
            }

            NoiseRouter router = randomState.router();
            EnumMap<DensityGraphRoot, net.minecraft.world.level.levelgen.DensityFunction> roots =
                    new EnumMap<>(DensityGraphRoot.class);
            roots.put(DensityGraphRoot.FINAL_DENSITY, router.finalDensity());
            roots.put(DensityGraphRoot.INITIAL_DENSITY_WITHOUT_JAGGEDNESS, router.initialDensityWithoutJaggedness());
            roots.put(DensityGraphRoot.BARRIER, router.barrierNoise());
            roots.put(DensityGraphRoot.FLUID_FLOODEDNESS, router.fluidLevelFloodednessNoise());
            roots.put(DensityGraphRoot.FLUID_SPREAD, router.fluidLevelSpreadNoise());
            roots.put(DensityGraphRoot.LAVA, router.lavaNoise());
            roots.put(DensityGraphRoot.EROSION, router.erosion());
            roots.put(DensityGraphRoot.DEPTH, router.depth());
            DensityGraphCompileResult compiled = DensityGraphCompiler.compile(roots);
            if (!compiled.supported()) {
                return CompileResult.unsupported(compiled.unsupportedReason());
            }
            NoiseSettings noiseSettings = settings.noiseSettings().clampToHeightAccessor(level);
            return CompileResult.supported(new OpenCLAccurateProgram(
                    compiled.program(),
                    noiseSettings,
                    randomState,
                    randomState.aquiferRandom(),
                    settings,
                    defaultBlockMask,
                    HeightmapMaterialClassifier.mask(settings.defaultFluid()),
                    HeightmapMaterialClassifier.mask(Blocks.LAVA.defaultBlockState())));
        } catch (Throwable failure) {
            return CompileResult.unsupported("accurate OpenCL program compile failed: "
                    + failure.getClass().getSimpleName()
                    + (failure.getMessage() == null ? "" : ": " + failure.getMessage()));
        }
    }

    synchronized Map<Long, AccurateHeightChunk> sample(OpenCLRuntime runtime, Collection<Long> requestedKeys) {
        List<Long> keys = List.copyOf(requestedKeys);
        if (keys.isEmpty()) {
            return Map.of();
        }
        int cellCountXZ = 16 / noiseSettings.getCellWidth();
        int cellCountY = Math.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
        int latticePoints = OpenCLAccurateLayout.latticePointCount(cellCountXZ, cellCountY);
        int minGridY = Math.floorDiv(noiseSettings.minY(), 12) - 1;
        int gridYSize = Math.floorDiv(noiseSettings.minY() + noiseSettings.height(), 12) + 1 - minGridY + 1;
        int aquiferPoints = aquifersEnabled ? Math.multiplyExact(9, gridYSize) : 0;
        int maxChunks = OpenCLAccurateBatchPlanner.maxChunks(
                runtime.deviceInfo(),
                graph.nodes().size(),
                graph.interpolatedNodes().size(),
                latticePoints,
                aquiferPoints);
        if (maxChunks <= 0) {
            throw new IllegalStateException("OpenCL device memory cannot fit one accurate chunk batch");
        }

        LinkedHashMap<Long, AccurateHeightChunk> result = new LinkedHashMap<>(keys.size());
        long startedAt = System.nanoTime();
        long[] kernelNanos = {0L};
        runtime.submit(OpenCLSubmissionPriority.ACCURATE, () -> {
            for (List<Long> batchChunks : OpenCLAccurateBatchPlanner.spatialBatches(keys, maxChunks)) {
                BatchResult batch = sampleBatch(runtime, batchChunks, latticePoints);
                result.putAll(batch.chunks());
                kernelNanos[0] += batch.kernelNanos();
            }
            return null;
        });
        AccurateSamplingStats.recordGpuBatch(result.size(), System.nanoTime() - startedAt, kernelNanos[0]);
        return result;
    }

    Map<Long, AccurateHeightSample> samplePositions(OpenCLRuntime runtime, Collection<BlockPos> requestedPositions) {
        return samplePositions(runtime, requestedPositions, AccurateSamplingProgress.NONE, false, false);
    }

    synchronized Map<Long, AccurateHeightSample> samplePositions(OpenCLRuntime runtime,
                                                                 Collection<BlockPos> requestedPositions,
                                                                 AccurateSamplingProgress progress) {
        return samplePositions(runtime, requestedPositions, progress, false, false);
    }

    synchronized Map<Long, AccurateHeightSample> samplePositions(OpenCLRuntime runtime,
                                                                 Collection<BlockPos> requestedPositions,
                                                                 AccurateSamplingProgress progress,
                                                                 boolean forceSerialHeight) {
        return samplePositions(runtime, requestedPositions, progress, forceSerialHeight, false);
    }

    synchronized Map<Long, AccurateHeightSample> sampleOceanFloorPositions(
            OpenCLRuntime runtime,
            Collection<BlockPos> requestedPositions,
            AccurateSamplingProgress progress) {
        return samplePositions(runtime, requestedPositions, progress, false, true);
    }

    private Map<Long, AccurateHeightSample> samplePositions(OpenCLRuntime runtime,
                                                            Collection<BlockPos> requestedPositions,
                                                            AccurateSamplingProgress progress,
                                                            boolean forceSerialHeight,
                                                            boolean oceanFloorOnly) {
        LinkedHashMap<Long, ColumnRequest> requested = uniqueColumns(requestedPositions);
        if (requested.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<Long, List<ColumnRequest>> columnsByChunk = groupColumnsByChunk(requested.values());
        List<Long> chunkKeys = new ArrayList<>(columnsByChunk.keySet());
        int latticePoints = latticePoints();
        int maxChunks = maxChunks(runtime, latticePoints);
        if (maxChunks <= 0) {
            throw new IllegalStateException("OpenCL device memory cannot fit one accurate chunk batch");
        }

        LinkedHashMap<Long, AccurateHeightSample> result = new LinkedHashMap<>(requested.size());
        long startedAt = System.nanoTime();
        long[] kernelNanos = {0L};
        long[] completedColumns = {0L};
        AccurateSamplingProgress sink = progress == null ? AccurateSamplingProgress.NONE : progress;
        List<List<Long>> spatialBatches = OpenCLAccurateBatchPlanner.spatialBatches(chunkKeys, maxChunks);
        CompletableFuture<NativeSlotZeroBatch> firstSlotZero = spatialBatches.isEmpty()
                ? null : prefetchNativeSlotZero(spatialBatches.getFirst(), latticePoints);
        runtime.submit(OpenCLSubmissionPriority.ACCURATE, () -> {
            CompletableFuture<NativeSlotZeroBatch> slotZeroFuture = firstSlotZero;
            for (int batchIndex = 0; batchIndex < spatialBatches.size(); batchIndex++) {
                List<Long> batchChunks = spatialBatches.get(batchIndex);
                long batchStartedAt = System.nanoTime();
                LinkedHashMap<Long, Integer> localChunkIndexes = new LinkedHashMap<>();
                for (int index = 0; index < batchChunks.size(); index++) {
                    localChunkIndexes.put(batchChunks.get(index), index);
                }
                int batchColumnCount = 0;
                for (long chunkKey : batchChunks) {
                    batchColumnCount += columnsByChunk.get(chunkKey).size();
                }
                List<ColumnRequest> batchColumns = new ArrayList<>(batchColumnCount);
                for (long chunkKey : batchChunks) {
                    batchColumns.addAll(columnsByChunk.get(chunkKey));
                }
                NativeSlotZeroBatch nativeSlotZero = slotZeroFuture.join();
                slotZeroFuture = batchIndex + 1 < spatialBatches.size()
                        ? prefetchNativeSlotZero(spatialBatches.get(batchIndex + 1), latticePoints)
                        : null;
                HeightColumns heights = sampleColumnsBatch(
                        runtime,
                        batchChunks,
                        latticePoints,
                        columnReferences(batchColumns, localChunkIndexes),
                        forceSerialHeight,
                        oceanFloorOnly,
                        nativeSlotZero);
                kernelNanos[0] += heights.kernelNanos();
                for (int index = 0; index < batchColumns.size(); index++) {
                    ColumnRequest column = batchColumns.get(index);
                    int oceanFloor = heights.oceanFloor()[index];
                    result.put(column.key(), oceanFloorOnly
                            ? new AccurateHeightSample(oceanFloor, oceanFloor, oceanFloor)
                            : new AccurateHeightSample(
                                    heights.worldSurface()[index],
                                    oceanFloor,
                                    heights.motionBlocking()[index]));
                }
                completedColumns[0] += batchColumns.size();
                reportProgress(sink, new AccurateSamplingProgress.Batch(
                        completedColumns[0], requested.size(), batchColumns.size(),
                        System.nanoTime() - startedAt, System.nanoTime() - batchStartedAt,
                        heights.kernelNanos()));
            }
            return null;
        });
        AccurateSamplingStats.recordGpuColumnBatch(result.size(), System.nanoTime() - startedAt, kernelNanos[0]);
        return result;
    }

    synchronized AccurateHeightGrid sampleGrid(OpenCLRuntime runtime,
                                               AccurateHeightGridRequest request,
                                               AccurateSamplingProgress progress) {
        LinkedHashMap<Long, GridIndexList> indicesByChunk = new LinkedHashMap<>();
        for (int index = 0; index < request.sampleCount(); index++) {
            long chunkKey = ChunkPos.asLong(request.blockX(index) >> 4, request.blockZ(index) >> 4);
            indicesByChunk.computeIfAbsent(chunkKey, ignored -> new GridIndexList()).add(index);
        }
        List<Long> chunkKeys = new ArrayList<>(indicesByChunk.keySet());
        int latticePoints = latticePoints();
        int maxChunks = maxChunks(runtime, latticePoints);
        if (maxChunks <= 0) {
            throw new IllegalStateException("OpenCL device memory cannot fit one accurate chunk batch");
        }

        int[] worldSurface = new int[request.sampleCount()];
        int[] oceanFloor = new int[request.sampleCount()];
        int[] motionBlocking = new int[request.sampleCount()];
        long startedAt = System.nanoTime();
        long[] kernelNanos = {0L};
        long[] completedColumns = {0L};
        AccurateSamplingProgress sink = progress == null ? AccurateSamplingProgress.NONE : progress;
        runtime.submit(OpenCLSubmissionPriority.ACCURATE, () -> {
            for (List<Long> batchChunks : OpenCLAccurateBatchPlanner.spatialBatches(chunkKeys, maxChunks)) {
                long batchStartedAt = System.nanoTime();
                LinkedHashMap<Long, Integer> localChunkIndexes = new LinkedHashMap<>();
                int batchColumnCount = 0;
                for (int index = 0; index < batchChunks.size(); index++) {
                    long chunkKey = batchChunks.get(index);
                    localChunkIndexes.put(chunkKey, index);
                    batchColumnCount += indicesByChunk.get(chunkKey).size();
                }

                int[] columnReferences = new int[Math.multiplyExact(batchColumnCount, 3)];
                int[] outputIndices = new int[batchColumnCount];
                int columnIndex = 0;
                for (long chunkKey : batchChunks) {
                    int localChunkIndex = localChunkIndexes.get(chunkKey);
                    GridIndexList gridIndices = indicesByChunk.get(chunkKey);
                    for (int offset = 0; offset < gridIndices.size(); offset++) {
                        int gridIndex = gridIndices.get(offset);
                        int blockX = request.blockX(gridIndex);
                        int blockZ = request.blockZ(gridIndex);
                        int referenceOffset = columnIndex * 3;
                        columnReferences[referenceOffset] = localChunkIndex;
                        columnReferences[referenceOffset + 1] = blockX & 15;
                        columnReferences[referenceOffset + 2] = blockZ & 15;
                        outputIndices[columnIndex] = gridIndex;
                        columnIndex++;
                    }
                }

                HeightColumns heights = sampleColumnsBatch(
                        runtime, batchChunks, latticePoints, columnReferences);
                kernelNanos[0] += heights.kernelNanos();
                for (int index = 0; index < outputIndices.length; index++) {
                    int outputIndex = outputIndices[index];
                    worldSurface[outputIndex] = heights.worldSurface()[index];
                    oceanFloor[outputIndex] = heights.oceanFloor()[index];
                    motionBlocking[outputIndex] = heights.motionBlocking()[index];
                }
                completedColumns[0] += batchColumnCount;
                reportProgress(sink, new AccurateSamplingProgress.Batch(
                        completedColumns[0], request.sampleCount(), batchColumnCount,
                        System.nanoTime() - startedAt, System.nanoTime() - batchStartedAt,
                        heights.kernelNanos()));
            }
            return null;
        });
        AccurateSamplingStats.recordGpuColumnBatch(
                request.sampleCount(), System.nanoTime() - startedAt, kernelNanos[0]);
        return new AccurateHeightGrid(request, worldSurface, oceanFloor, motionBlocking);
    }

    private BatchResult sampleBatch(OpenCLRuntime runtime,
                                    List<Long> chunkKeys,
                                    int latticePointsPerChunk) {
        HeightColumns heights = sampleColumnsBatch(
                runtime,
                chunkKeys,
                latticePointsPerChunk,
                fullColumnReferences(chunkKeys.size()),
                false);
        return new BatchResult(unpack(chunkKeys, heights.worldSurface(), heights.oceanFloor(), heights.motionBlocking()),
                heights.kernelNanos());
    }

    private HeightColumns sampleColumnsBatch(OpenCLRuntime runtime,
                                             List<Long> chunkKeys,
                                             int latticePointsPerChunk,
                                             int[] columnReferences) {
        return sampleColumnsBatch(runtime, chunkKeys, latticePointsPerChunk, columnReferences, false, false);
    }

    private HeightColumns sampleColumnsBatch(OpenCLRuntime runtime,
                                             List<Long> chunkKeys,
                                             int latticePointsPerChunk,
                                             int[] columnReferences,
                                             boolean forceSerialHeight) {
        return sampleColumnsBatch(
                runtime, chunkKeys, latticePointsPerChunk, columnReferences, forceSerialHeight, false);
    }

    private HeightColumns sampleColumnsBatch(OpenCLRuntime runtime,
                                             List<Long> chunkKeys,
                                             int latticePointsPerChunk,
                                             int[] columnReferences,
                                             boolean forceSerialHeight,
                                             boolean oceanFloorOnly) {
        return sampleColumnsBatch(runtime, chunkKeys, latticePointsPerChunk, columnReferences,
                forceSerialHeight, oceanFloorOnly, null);
    }

    private HeightColumns sampleColumnsBatch(OpenCLRuntime runtime,
                                             List<Long> chunkKeys,
                                             int latticePointsPerChunk,
                                             int[] columnReferences,
                                             boolean forceSerialHeight,
                                             boolean oceanFloorOnly,
                                             NativeSlotZeroBatch prefetchedSlotZero) {
        AquiferPositionPlanner.Plan aquiferPlan = aquifersEnabled
                ? AquiferPositionPlanner.plan(
                        chunkKeys, columnReferences, aquiferRandom, noiseSettings.minY(), noiseSettings.height())
                : emptyAquiferPlan(chunkKeys);
        int[] params = params(chunkKeys.size(), latticePointsPerChunk, aquiferPlan);
        int chunkCount = chunkKeys.size();
        int nodeCount = graph.nodes().size();
        int interpolatorCount = graph.interpolatedNodes().size();
        SparseLatticePlanner.Plan latticePlan = SparseLatticePlanner.plan(
                chunkCount,
                columnReferences,
                noiseSettings.getCellWidth(),
                16 / noiseSettings.getCellWidth(),
                noiseSettings.height() / noiseSettings.getCellHeight());
        params[33] = latticePlan.sparse() ? 1 : 0;
                int latticeWorkItems = interpolatorCount <= 1
                ? 0
                : latticePlan.workItemCount();
        int preliminaryPointCount = aquifersEnabled ? aquiferPlan.preliminaryPointCount() : 0;
        int preliminaryWorkItems = Math.multiplyExact(
                preliminaryPointCount,
                noiseSettings.height() / noiseSettings.getCellHeight() + 1);
        int aquiferWorkItems = aquifersEnabled ? aquiferPlan.uniquePointCount() : 0;
        if (aquifersEnabled && sparseAquiferLogged.compareAndSet(false, true)) {
            long fullPointUpperBound = Math.multiplyExact(
                    Math.multiplyExact((long) chunkCount, 9L),
                    (long) aquiferPlan.gridYSize());
            LOGGER.info("Streams sparse aquifer plan: chunks={}, columns={}, uniquePoints={}, "
                            + "fullPointUpperBound={}, retained={}%, preliminaryPoints={}.",
                    chunkCount,
                    Math.floorDiv(columnReferences.length, 3),
                    aquiferPlan.uniquePointCount(),
                    fullPointUpperBound,
                    fullPointUpperBound == 0L ? 0L
                            : Math.round(aquiferPlan.uniquePointCount() * 100.0D / fullPointUpperBound),
                    preliminaryPointCount);
        }
        int columnWorkItems = Math.floorDiv(columnReferences.length, 3);
        int parallelHeightWorkItems = Math.multiplyExact(columnWorkItems, HEIGHT_PARALLEL_LANES);
        boolean parallelHeight = oceanFloorOnly || (!forceSerialHeight && shouldParallelizeHeight(
                runtime, parallelHeightWorkItems, latticeWorkItems, nodeCount, interpolatorCount,
                latticePointsPerChunk, chunkCount));
        int heightScratchItems = parallelHeight ? parallelHeightWorkItems : columnWorkItems;
        long bytesPerWorkItem = Math.multiplyExact((long) Math.max(1, nodeCount), Double.BYTES);
        OpenCLBridge.DeviceInfo device = runtime.deviceInfo();
        long maxAllocation = device.maxAllocationBytes() > 0L
                ? device.maxAllocationBytes()
                : 128L * 1024L * 1024L;
        long globalMemory = device.globalMemoryBytes() > 0L
                ? device.globalMemoryBytes()
                : 1024L * 1024L * 1024L;
        long scratchBudget = Math.min(maxAllocation - maxAllocation / 4L, globalMemory / 8L);
        long segmentCapacity = Math.max(1L, scratchBudget / bytesPerWorkItem);
        int preliminarySegmentItems = (int) Math.min(
                (long) Math.max(1, preliminaryWorkItems),
                Math.min((long) Integer.MAX_VALUE, segmentCapacity));
        int boundedPreliminaryItems = Math.min(preliminaryWorkItems, preliminarySegmentItems);
        int scratchItems = Math.max(heightScratchItems,
                Math.max(latticeWorkItems, Math.max(boundedPreliminaryItems, aquiferWorkItems)));
        long scratchBytes = Math.multiplyExact(
                (long) Math.max(1, scratchItems), bytesPerWorkItem);
        long latticeBytes = Math.multiplyExact(
                Math.multiplyExact(
                        Math.multiplyExact((long) chunkCount, (long) Math.max(1, interpolatorCount)),
                        (long) latticePointsPerChunk),
                Double.BYTES);
        long aquiferStatusBytes = Math.multiplyExact(
                Math.multiplyExact((long) Math.max(1, aquiferWorkItems), 2L),
                Integer.BYTES);
        long preliminarySurfaceBytes = Math.multiplyExact(
                (long) Math.max(1, preliminaryPointCount), Integer.BYTES);
        long outputBytes = Math.multiplyExact((long) columnWorkItems, Integer.BYTES);

        OpenCLDensityProgramBuffers graphBuffers = buffers(runtime);
        OpenCLAccurateWorkspace.BatchBuffers batchBuffers = workspace(runtime).prepare(
                runtime,
                params,
                aquiferPlan.chunkCoordinates(),
                columnReferences,
                latticePlan.references(),
                aquiferPlan.positions(),
                aquiferPlan.chunkPointIndices(),
                aquiferPlan.preliminaryCoordinates(),
                aquiferPlan.pointPreliminaryIndices(),
                latticeBytes,
                preliminarySurfaceBytes,
                aquiferStatusBytes,
                scratchBytes,
                outputBytes);
        OpenCLBridge.DeviceBuffer paramsBuffer = batchBuffers.params();
        OpenCLBridge.DeviceBuffer chunksBuffer = batchBuffers.chunks();
        OpenCLBridge.DeviceBuffer columnsBuffer = batchBuffers.columns();
        OpenCLBridge.DeviceBuffer latticeReferencesBuffer = batchBuffers.latticeReferences();
        OpenCLBridge.DeviceBuffer aquiferPositionsBuffer = batchBuffers.aquiferPositions();
        OpenCLBridge.DeviceBuffer aquiferPointIndicesBuffer = batchBuffers.aquiferPointIndices();
        OpenCLBridge.DeviceBuffer preliminaryPositionsBuffer = batchBuffers.preliminaryPositions();
        OpenCLBridge.DeviceBuffer pointPreliminaryIndicesBuffer = batchBuffers.pointPreliminaryIndices();
        OpenCLBridge.DeviceBuffer latticeBuffer = batchBuffers.lattice();
        OpenCLBridge.DeviceBuffer preliminarySurfaceBuffer = batchBuffers.preliminarySurface();
        OpenCLBridge.DeviceBuffer aquiferStatusBuffer = batchBuffers.aquiferStatus();
        OpenCLBridge.DeviceBuffer scratchBuffer = batchBuffers.scratch();
        OpenCLBridge.DeviceBuffer worldSurfaceBuffer = batchBuffers.worldSurface();
        OpenCLBridge.DeviceBuffer oceanFloorBuffer = batchBuffers.oceanFloor();
        OpenCLBridge.DeviceBuffer motionBlockingBuffer = batchBuffers.motionBlocking();

        ArrayList<OpenCLBridge.CommandEvent> events = new ArrayList<>(6);
        ArrayList<OpenCLBridge.CommandEvent> latticeEvents = new ArrayList<>(1);
        ArrayList<OpenCLBridge.CommandEvent> slotZeroPatchEvents = new ArrayList<>(1);
        ArrayList<OpenCLBridge.CommandEvent> preliminaryEvents = new ArrayList<>(2);
        ArrayList<OpenCLBridge.CommandEvent> aquiferEvents = new ArrayList<>(1);
        ArrayList<OpenCLBridge.CommandEvent> heightEvents = new ArrayList<>(2);
        try {
                if (latticeWorkItems > 0) {
                    ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(20);
                    arguments.add(paramsBuffer);
                    arguments.add(chunksBuffer);
                    arguments.add(latticeReferencesBuffer);
                    arguments.addAll(graphBuffers.arguments());
                    arguments.add(graphBuffers.interpolatedNodes());
                    arguments.add(latticeBuffer);
                    arguments.add(scratchBuffer);
                    OpenCLBridge.CommandEvent event = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_LATTICE_KERNEL, arguments, latticeWorkItems);
                    events.add(event);
                    latticeEvents.add(event);
                }
                NativeSlotZeroBatch slotZero = prefetchedSlotZero == null
                        ? sampleNativeSlotZeroCached(chunkKeys, latticePointsPerChunk)
                        : prefetchedSlotZero;
                double[] nativeSlotZero = slotZero.values();
                long nativeSlotZeroNanos = slotZero.computeNanos();
                int expectedSlotZeroValues = Math.multiplyExact(chunkCount, latticePointsPerChunk);
                if (nativeSlotZero.length != expectedSlotZeroValues) {
                    throw new IllegalStateException("prefetched native slot zero size mismatch: "
                            + nativeSlotZero.length + " != " + expectedSlotZeroValues);
                }
                long latticeKernelNanos = awaitEvents(latticeEvents);
                long uploadStarted = System.nanoTime();
                try (OpenCLBridge.DeviceBuffer nativeSlotZeroBuffer = runtime.upload(nativeSlotZero)) {
                    long slotZeroUploadNanos = System.nanoTime() - uploadStarted;
                    ArrayList<OpenCLBridge.DeviceBuffer> patchArguments = new ArrayList<>(3);
                    patchArguments.add(paramsBuffer);
                    patchArguments.add(nativeSlotZeroBuffer);
                    patchArguments.add(latticeBuffer);
                    OpenCLBridge.CommandEvent patchEvent = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_SLOT_ZERO_PATCH_KERNEL,
                            patchArguments,
                            Math.multiplyExact((long) chunkCount, (long) latticePointsPerChunk));
                    events.add(patchEvent);
                    slotZeroPatchEvents.add(patchEvent);
                    long slotZeroPatchNanos = awaitEvents(slotZeroPatchEvents);
                    AccurateSamplingStats.recordNativeSlotZero(
                            nativeSlotZeroNanos, slotZeroUploadNanos, slotZeroPatchNanos);
                }
                if (preliminaryWorkItems > 0) {
                    ArrayList<OpenCLBridge.DeviceBuffer> initArguments = new ArrayList<>(2);
                    initArguments.add(paramsBuffer);
                    initArguments.add(preliminarySurfaceBuffer);
                    OpenCLBridge.CommandEvent initEvent = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_PRELIMINARY_INIT_KERNEL,
                            initArguments,
                            preliminaryPointCount);
                    events.add(initEvent);
                    preliminaryEvents.add(initEvent);

                    ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(19);
                    arguments.add(paramsBuffer);
                    arguments.add(preliminaryPositionsBuffer);
                    arguments.addAll(graphBuffers.arguments());
                    arguments.add(scratchBuffer);
                    arguments.add(preliminarySurfaceBuffer);
                    int segmentCount = (int) Math.floorDiv(
                            (long) preliminaryWorkItems + preliminarySegmentItems - 1L,
                            preliminarySegmentItems);
                    if (preliminarySegmentationLogged.compareAndSet(false, true)) {
                        LOGGER.info("Streams OpenCL preliminary segmented: chunks={}, nodeCount={}, "
                                        + "preliminaryWorkItems={}, segmentItems={}, segmentCount={}, "
                                        + "scratchMiB={}, maxAllocMiB={}.",
                                chunkCount,
                                nodeCount,
                                preliminaryWorkItems,
                                preliminarySegmentItems,
                                segmentCount,
                                scratchBytes / 1024L / 1024L,
                                maxAllocation / 1024L / 1024L);
                    }
                    for (long offset = 0L; offset < preliminaryWorkItems; offset += preliminarySegmentItems) {
                        long count = Math.min((long) preliminarySegmentItems, preliminaryWorkItems - offset);
                        OpenCLBridge.CommandEvent preliminaryEvent = runtime.enqueue(
                                OpenCLRuntime.ACCURATE_PRELIMINARY_KERNEL, arguments, offset, count);
                        events.add(preliminaryEvent);
                        preliminaryEvents.add(preliminaryEvent);
                    }
                }
                if (aquiferWorkItems > 0) {
                    ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(21);
                    arguments.add(paramsBuffer);
                    arguments.add(chunksBuffer);
                    arguments.add(aquiferPositionsBuffer);
                    arguments.add(pointPreliminaryIndicesBuffer);
                    arguments.add(preliminarySurfaceBuffer);
                    arguments.addAll(graphBuffers.arguments());
                    arguments.add(graphBuffers.interpolatedNodes());
                    arguments.add(latticeBuffer);
                    arguments.add(aquiferStatusBuffer);
                    arguments.add(scratchBuffer);
                    OpenCLBridge.CommandEvent event = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_AQUIFER_KERNEL, arguments, aquiferWorkItems);
                    events.add(event);
                    aquiferEvents.add(event);
                }

                if (oceanFloorOnly) {
                    ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(25);
                    arguments.add(paramsBuffer);
                    arguments.add(chunksBuffer);
                    arguments.add(columnsBuffer);
                    arguments.add(aquiferPositionsBuffer);
                    arguments.add(aquiferPointIndicesBuffer);
                    arguments.addAll(graphBuffers.arguments());
                    arguments.add(latticeBuffer);
                    arguments.add(aquiferStatusBuffer);
                    arguments.add(scratchBuffer);
                    arguments.add(oceanFloorBuffer);
                    OpenCLBridge.CommandEvent oceanFloorEvent = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_OCEAN_FLOOR_KERNEL,
                            arguments,
                            parallelHeightWorkItems);
                    events.add(oceanFloorEvent);
                    heightEvents.add(oceanFloorEvent);
                } else if (parallelHeight) {
                    ArrayList<OpenCLBridge.DeviceBuffer> initArguments = new ArrayList<>(4);
                    initArguments.add(paramsBuffer);
                    initArguments.add(worldSurfaceBuffer);
                    initArguments.add(oceanFloorBuffer);
                    initArguments.add(motionBlockingBuffer);
                    OpenCLBridge.CommandEvent initEvent = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_HEIGHT_INIT_KERNEL, initArguments, columnWorkItems);
                    events.add(initEvent);
                    heightEvents.add(initEvent);

                    ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(27);
                    arguments.add(paramsBuffer);
                    arguments.add(chunksBuffer);
                    arguments.add(columnsBuffer);
                    arguments.add(aquiferPositionsBuffer);
                    arguments.add(aquiferPointIndicesBuffer);
                    arguments.addAll(graphBuffers.arguments());
                    arguments.add(latticeBuffer);
                    arguments.add(aquiferStatusBuffer);
                    arguments.add(scratchBuffer);
                    arguments.add(worldSurfaceBuffer);
                    arguments.add(oceanFloorBuffer);
                    arguments.add(motionBlockingBuffer);
                    OpenCLBridge.CommandEvent parallelEvent = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_HEIGHT_PARALLEL_KERNEL,
                            arguments,
                            parallelHeightWorkItems);
                    events.add(parallelEvent);
                    heightEvents.add(parallelEvent);
                } else {
                    ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(27);
                    arguments.add(paramsBuffer);
                    arguments.add(chunksBuffer);
                    arguments.add(columnsBuffer);
                    arguments.add(aquiferPositionsBuffer);
                    arguments.add(aquiferPointIndicesBuffer);
                    arguments.addAll(graphBuffers.arguments());
                    arguments.add(graphBuffers.interpolatedNodes());
                    arguments.add(latticeBuffer);
                    arguments.add(aquiferStatusBuffer);
                    arguments.add(scratchBuffer);
                    arguments.add(worldSurfaceBuffer);
                    arguments.add(oceanFloorBuffer);
                    arguments.add(motionBlockingBuffer);
                    OpenCLBridge.CommandEvent event = runtime.enqueue(
                            OpenCLRuntime.ACCURATE_HEIGHT_KERNEL, arguments, columnWorkItems);
                    events.add(event);
                    heightEvents.add(event);
                }

                int[] worldSurface = oceanFloorOnly ? null : runtime.readInts(worldSurfaceBuffer, columnWorkItems);
                int[] oceanFloor = runtime.readInts(oceanFloorBuffer, columnWorkItems);
                int[] motionBlocking = oceanFloorOnly ? null : runtime.readInts(motionBlockingBuffer, columnWorkItems);
                long preliminaryKernelNanos = awaitEvents(preliminaryEvents);
                long aquiferKernelNanos = awaitEvents(aquiferEvents);
                long heightKernelNanos = awaitEvents(heightEvents);
                if (oceanFloorOnly && !oceanFloorKernelValidated) {
                    validateOceanFloorKernel(
                            runtime, paramsBuffer, chunksBuffer, columnsBuffer,
                            aquiferPositionsBuffer, aquiferPointIndicesBuffer,
                            graphBuffers, latticeBuffer, aquiferStatusBuffer, scratchBuffer,
                            worldSurfaceBuffer, oceanFloorBuffer, motionBlockingBuffer,
                            parallelHeightWorkItems, columnWorkItems, oceanFloor);
                    oceanFloorKernelValidated = true;
                    LOGGER.info("Streams ocean-floor-only OpenCL kernel passed full-column exact validation: columns={}.",
                            columnWorkItems);
                }
                long kernelNanos = latticeKernelNanos + preliminaryKernelNanos
                        + aquiferKernelNanos + heightKernelNanos;
                AccurateSamplingStats.recordGpuKernelStages(
                        latticeKernelNanos, preliminaryKernelNanos, aquiferKernelNanos, heightKernelNanos);
                return new HeightColumns(worldSurface, oceanFloor, motionBlocking, kernelNanos);
        } finally {
            events.forEach(OpenCLBridge.CommandEvent::close);
        }
    }

    private static void validateOceanFloorKernel(
            OpenCLRuntime runtime,
            OpenCLBridge.DeviceBuffer paramsBuffer,
            OpenCLBridge.DeviceBuffer chunksBuffer,
            OpenCLBridge.DeviceBuffer columnsBuffer,
            OpenCLBridge.DeviceBuffer aquiferPositionsBuffer,
            OpenCLBridge.DeviceBuffer aquiferPointIndicesBuffer,
            OpenCLDensityProgramBuffers graphBuffers,
            OpenCLBridge.DeviceBuffer latticeBuffer,
            OpenCLBridge.DeviceBuffer aquiferStatusBuffer,
            OpenCLBridge.DeviceBuffer scratchBuffer,
            OpenCLBridge.DeviceBuffer worldSurfaceBuffer,
            OpenCLBridge.DeviceBuffer oceanFloorBuffer,
            OpenCLBridge.DeviceBuffer motionBlockingBuffer,
            int parallelHeightWorkItems,
            int columnWorkItems,
            int[] oceanFloorOnly) {
        ArrayList<OpenCLBridge.CommandEvent> validationEvents = new ArrayList<>(2);
        try {
            OpenCLBridge.CommandEvent initEvent = runtime.enqueue(
                    OpenCLRuntime.ACCURATE_HEIGHT_INIT_KERNEL,
                    List.of(paramsBuffer, worldSurfaceBuffer, oceanFloorBuffer, motionBlockingBuffer),
                    columnWorkItems);
            validationEvents.add(initEvent);

            ArrayList<OpenCLBridge.DeviceBuffer> arguments = new ArrayList<>(27);
            arguments.add(paramsBuffer);
            arguments.add(chunksBuffer);
            arguments.add(columnsBuffer);
            arguments.add(aquiferPositionsBuffer);
            arguments.add(aquiferPointIndicesBuffer);
            arguments.addAll(graphBuffers.arguments());
            arguments.add(latticeBuffer);
            arguments.add(aquiferStatusBuffer);
            arguments.add(scratchBuffer);
            arguments.add(worldSurfaceBuffer);
            arguments.add(oceanFloorBuffer);
            arguments.add(motionBlockingBuffer);
            OpenCLBridge.CommandEvent legacyEvent = runtime.enqueue(
                    OpenCLRuntime.ACCURATE_HEIGHT_PARALLEL_KERNEL,
                    arguments,
                    parallelHeightWorkItems);
            validationEvents.add(legacyEvent);

            int[] legacyOceanFloor = runtime.readInts(oceanFloorBuffer, columnWorkItems);
            awaitEvents(validationEvents);
            if (!Arrays.equals(oceanFloorOnly, legacyOceanFloor)) {
                for (int index = 0; index < columnWorkItems; index++) {
                    if (oceanFloorOnly[index] != legacyOceanFloor[index]) {
                        throw new IllegalStateException("ocean-floor-only OpenCL mismatch at batch column "
                                + index + ": optimized=" + oceanFloorOnly[index]
                                + ", legacy=" + legacyOceanFloor[index]);
                    }
                }
            }
        } finally {
            validationEvents.forEach(OpenCLBridge.CommandEvent::close);
        }
    }

    private static long awaitEvents(List<OpenCLBridge.CommandEvent> events) {
        long nanos = 0L;
        for (OpenCLBridge.CommandEvent event : events) {
            nanos += event.awaitNanos();
        }
        return nanos;
    }

    private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings settings) {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        int lavaLevel = Math.min(-54, settings.seaLevel());
        return (x, y, z) -> y < lavaLevel ? lava : defaultFluid;
    }

    private static void reportProgress(AccurateSamplingProgress progress, AccurateSamplingProgress.Batch batch) {
        try {
            progress.onBatch(batch);
        } catch (RuntimeException ignored) {}
    }

    private int latticePoints() {
        int cellCountXZ = 16 / noiseSettings.getCellWidth();
        int cellCountY = Math.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
        return OpenCLAccurateLayout.latticePointCount(cellCountXZ, cellCountY);
    }

    private static boolean shouldParallelizeHeight(OpenCLRuntime runtime,
                                                   int parallelWorkItems,
                                                   int latticeWorkItems,
                                                   int nodeCount,
                                                   int interpolatorCount,
                                                   int latticePointsPerChunk,
                                                   int chunkCount) {
        long parallelScratchBytes = Math.multiplyExact(
                Math.multiplyExact((long) parallelWorkItems, (long) Math.max(1, nodeCount)),
                Double.BYTES);
        long latticeBytes = Math.multiplyExact(
                Math.multiplyExact((long) chunkCount, (long) latticePointsPerChunk),
                Math.multiplyExact((long) Math.max(1, interpolatorCount), Double.BYTES));
        OpenCLBridge.DeviceInfo device = runtime.deviceInfo();
        long maxAllocation = device.maxAllocationBytes() > 0L
                ? device.maxAllocationBytes()
                : 128L * 1024L * 1024L;
        long globalBudget = device.globalMemoryBytes() > 0L
                ? device.globalMemoryBytes() / 3L
                : 512L * 1024L * 1024L;
        long usefulParallelismLimit = Math.max(262_144L, Math.multiplyExact((long) latticeWorkItems, 2L));
        return parallelWorkItems <= usefulParallelismLimit
                && parallelScratchBytes <= maxAllocation
                && parallelScratchBytes + latticeBytes <= globalBudget;
    }

    private int maxChunks(OpenCLRuntime runtime, int latticePoints) {
        int minGridY = Math.floorDiv(noiseSettings.minY(), 12) - 1;
        int gridYSize = Math.floorDiv(noiseSettings.minY() + noiseSettings.height(), 12) + 1 - minGridY + 1;
        int aquiferPoints = aquifersEnabled ? Math.multiplyExact(9, gridYSize) : 0;
        return OpenCLAccurateBatchPlanner.maxChunks(
                runtime.deviceInfo(),
                graph.nodes().size(),
                graph.interpolatedNodes().size(),
                latticePoints,
                aquiferPoints);
    }

    private int[] params(int chunkCount,
                         int latticePointsPerChunk,
                         AquiferPositionPlanner.Plan aquiferPlan) {
        int cellWidth = noiseSettings.getCellWidth();
        int cellHeight = noiseSettings.getCellHeight();
        int cellCountXZ = 16 / cellWidth;
        int cellCountY = Math.floorDiv(noiseSettings.height(), cellHeight);
        return new int[]{
                chunkCount,
                noiseSettings.minY(),
                noiseSettings.height(),
                cellWidth,
                cellHeight,
                cellCountXZ,
                cellCountY,
                Math.floorDiv(noiseSettings.minY(), cellHeight),
                graph.nodes().size(),
                graph.interpolatedNodes().size(),
                latticePointsPerChunk,
                aquiferPlan.gridYSize(),
                aquiferPlan.pointsPerChunk(),
                aquiferPlan.minGridY(),
                aquifersEnabled ? 1 : 0,
                seaLevel,
                -54,
                Math.min(-54, seaLevel),
                DimensionType.WAY_BELOW_MIN_Y,
                defaultFluidKind,
                defaultBlockMask,
                defaultFluidMask,
                lavaMask,
                graph.root(DensityGraphRoot.FINAL_DENSITY),
                graph.root(DensityGraphRoot.INITIAL_DENSITY_WITHOUT_JAGGEDNESS),
                graph.root(DensityGraphRoot.BARRIER),
                graph.root(DensityGraphRoot.FLUID_FLOODEDNESS),
                graph.root(DensityGraphRoot.FLUID_SPREAD),
                graph.root(DensityGraphRoot.LAVA),
                graph.root(DensityGraphRoot.EROSION),
                graph.root(DensityGraphRoot.DEPTH),
                aquiferPlan.uniquePointCount(),
                aquiferPlan.preliminaryPointCount(),
                0
        };
    }

    private synchronized OpenCLDensityProgramBuffers buffers(OpenCLRuntime runtime) {
        if (closed) {
            throw new IllegalStateException("accurate OpenCL program is closed");
        }
        return buffers.computeIfAbsent(runtime, key -> OpenCLDensityProgramBuffers.upload(key, payload));
    }

    private synchronized OpenCLAccurateWorkspace workspace(OpenCLRuntime runtime) {
        if (closed) {
            throw new IllegalStateException("accurate OpenCL program is closed");
        }
        return workspaces.computeIfAbsent(runtime, ignored -> new OpenCLAccurateWorkspace());
    }

    private CompletableFuture<NativeSlotZeroBatch> prefetchNativeSlotZero(
            List<Long> chunkKeys, int latticePointsPerChunk) {
        List<Long> immutableChunkKeys = List.copyOf(chunkKeys);
        return CompletableFuture.supplyAsync(
                () -> sampleNativeSlotZeroCached(immutableChunkKeys, latticePointsPerChunk),
                slotZeroPrefetchExecutor);
    }

    /** Reuses native slot-zero interpolation data across adjacent GPU batches. */
    private NativeSlotZeroBatch sampleNativeSlotZeroCached(List<Long> chunkKeys,
                                                            int latticePointsPerChunk) {
        long started = System.nanoTime();
        Map<Long, double[]> valuesByChunk = new LinkedHashMap<>(chunkKeys.size());
        List<Long> missing = new ArrayList<>();
        for (long chunkKey : chunkKeys) {
            double[] cached = slotZeroCache.get(chunkKey);
            if (cached != null && cached.length == latticePointsPerChunk) {
                valuesByChunk.put(chunkKey, cached);
                slotZeroCacheHits.incrementAndGet();
            } else {
                missing.add(chunkKey);
                slotZeroCacheMisses.incrementAndGet();
            }
        }
        long computedNanos = 0L;
        if (!missing.isEmpty()) {
            NativeSlotZeroBatch computed = sampleNativeSlotZero(missing, latticePointsPerChunk);
            computedNanos = computed.computeNanos();
            for (int index = 0; index < missing.size(); index++) {
                int from = Math.multiplyExact(index, latticePointsPerChunk);
                int to = Math.addExact(from, latticePointsPerChunk);
                double[] chunkValues = Arrays.copyOfRange(computed.values(), from, to);
                if (slotZeroCache.size() >= MAX_SLOT_ZERO_CACHE_CHUNKS) {
                    Long eldest = slotZeroCache.keySet().stream().findFirst().orElse(null);
                    if (eldest != null) slotZeroCache.remove(eldest);
                }
                slotZeroCache.put(missing.get(index), chunkValues);
                valuesByChunk.put(missing.get(index), chunkValues);
            }
        }
        double[] result = new double[Math.multiplyExact(chunkKeys.size(), latticePointsPerChunk)];
        for (int index = 0; index < chunkKeys.size(); index++) {
            double[] chunkValues = valuesByChunk.get(chunkKeys.get(index));
            if (chunkValues == null || chunkValues.length != latticePointsPerChunk) {
                throw new IllegalStateException("slot zero cache omitted chunk " + chunkKeys.get(index));
            }
            System.arraycopy(chunkValues, 0, result,
                    Math.multiplyExact(index, latticePointsPerChunk), latticePointsPerChunk);
        }
        long calls = slotZeroCacheHits.get() + slotZeroCacheMisses.get();
        if (calls == chunkKeys.size() || calls % 256L < chunkKeys.size()) {
            LOGGER.info("Streams native slot zero cache: hits={}, misses={}, cachedChunks={}, "
                            + "lastChunks={}, computedMs={}.",
                    slotZeroCacheHits.get(), slotZeroCacheMisses.get(), slotZeroCache.size(),
                    chunkKeys.size(), computedNanos / 1_000_000L);
        }
        return new NativeSlotZeroBatch(result, System.nanoTime() - started);
    }

    private NativeSlotZeroBatch sampleNativeSlotZero(List<Long> chunkKeys, int latticePointsPerChunk) {
        long started = System.nanoTime();
        double[] values = NativeSlotZeroLatticeSampler.sample(
                chunkKeys, randomState, generatorSettings, noiseSettings, fluidPicker,
                graph.interpolatedNodes().size());
        int expected = Math.multiplyExact(chunkKeys.size(), latticePointsPerChunk);
        if (values.length != expected) {
            throw new IllegalStateException("native slot zero size mismatch: "
                    + values.length + " != " + expected);
        }
        return new NativeSlotZeroBatch(values, System.nanoTime() - started);
    }

    private static AquiferPositionPlanner.Plan emptyAquiferPlan(List<Long> chunkKeys) {
        int[] coordinates = new int[Math.multiplyExact(chunkKeys.size(), 2)];
        for (int i = 0; i < chunkKeys.size(); i++) {
            coordinates[i * 2] = ChunkPos.getX(chunkKeys.get(i));
            coordinates[i * 2 + 1] = ChunkPos.getZ(chunkKeys.get(i));
        }
        return new AquiferPositionPlanner.Plan(
                coordinates, new int[]{0}, new int[]{0}, new int[]{0}, new int[]{0},
                0, 0, 0, 0, 0);
    }

    private static Map<Long, AccurateHeightChunk> unpack(List<Long> keys,
                                                         int[] worldSurface,
                                                         int[] oceanFloor,
                                                         int[] motionBlocking) {
        LinkedHashMap<Long, AccurateHeightChunk> result = new LinkedHashMap<>(keys.size());
        for (int index = 0; index < keys.size(); index++) {
            int from = index * AccurateHeightChunk.COLUMN_COUNT;
            int to = from + AccurateHeightChunk.COLUMN_COUNT;
            long key = keys.get(index);
            result.put(key, new AccurateHeightChunk(
                    ChunkPos.getX(key),
                    ChunkPos.getZ(key),
                    Arrays.copyOfRange(worldSurface, from, to),
                    Arrays.copyOfRange(oceanFloor, from, to),
                    Arrays.copyOfRange(motionBlocking, from, to)));
        }
        return result;
    }

    private static LinkedHashMap<Long, ColumnRequest> uniqueColumns(Collection<BlockPos> positions) {
        LinkedHashMap<Long, ColumnRequest> result = new LinkedHashMap<>();
        if (positions == null) {
            return result;
        }
        for (BlockPos position : positions) {
            if (position == null) {
                continue;
            }
            long key = AccurateHeightSample.key(position.getX(), position.getZ());
            result.putIfAbsent(key, new ColumnRequest(key, position.getX(), position.getZ()));
        }
        return result;
    }

    private static LinkedHashMap<Long, List<ColumnRequest>> groupColumnsByChunk(
            Collection<ColumnRequest> columns) {
        LinkedHashMap<Long, List<ColumnRequest>> grouped = new LinkedHashMap<>();
        for (ColumnRequest column : columns) {
            grouped.computeIfAbsent(column.chunkKey(), ignored -> new ArrayList<>()).add(column);
        }
        return grouped;
    }

    private static int[] fullColumnReferences(int chunkCount) {
        int[] result = new int[Math.multiplyExact(Math.multiplyExact(chunkCount, AccurateHeightChunk.COLUMN_COUNT), 3)];
        for (int chunk = 0; chunk < chunkCount; chunk++) {
            for (int column = 0; column < AccurateHeightChunk.COLUMN_COUNT; column++) {
                int offset = (chunk * AccurateHeightChunk.COLUMN_COUNT + column) * 3;
                result[offset] = chunk;
                result[offset + 1] = column & 15;
                result[offset + 2] = column >> 4;
            }
        }
        return result;
    }

    private static int[] columnReferences(List<ColumnRequest> columns,
                                          Map<Long, Integer> chunkIndexes) {
        int[] result = new int[Math.multiplyExact(columns.size(), 3)];
        for (int index = 0; index < columns.size(); index++) {
            ColumnRequest column = columns.get(index);
            Integer chunkIndex = chunkIndexes.get(column.chunkKey());
            if (chunkIndex == null) {
                throw new IllegalArgumentException("Column chunk is missing from batch");
            }
            int offset = index * 3;
            result[offset] = chunkIndex;
            result[offset + 1] = column.blockX() & 15;
            result[offset + 2] = column.blockZ() & 15;
        }
        return result;
    }

    @Override
    public void close() {
        Map<OpenCLRuntime, OpenCLDensityProgramBuffers> closing;
        Map<OpenCLRuntime, OpenCLAccurateWorkspace> closingWorkspaces;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            closing = new IdentityHashMap<>(buffers);
            buffers.clear();
            closingWorkspaces = new IdentityHashMap<>(workspaces);
            workspaces.clear();
            slotZeroCache.clear();
        }
        slotZeroPrefetchExecutor.shutdownNow();
        for (Map.Entry<OpenCLRuntime, OpenCLDensityProgramBuffers> entry : closing.entrySet()) {
            synchronized (entry.getKey().operationLock()) {
                entry.getValue().close();
            }
        }
        for (Map.Entry<OpenCLRuntime, OpenCLAccurateWorkspace> entry : closingWorkspaces.entrySet()) {
            synchronized (entry.getKey().operationLock()) {
                entry.getValue().close();
            }
        }
    }

    record CompileResult(OpenCLAccurateProgram program, String unsupportedReason) {
        static CompileResult supported(OpenCLAccurateProgram program) {
            return new CompileResult(program, null);
        }

        static CompileResult unsupported(String reason) {
            return new CompileResult(null, reason == null || reason.isBlank() ? "unknown" : reason);
        }

        boolean supported() {
            return program != null && unsupportedReason == null;
        }
    }

    private record BatchResult(Map<Long, AccurateHeightChunk> chunks, long kernelNanos) {}

    private record HeightColumns(int[] worldSurface,
                                 int[] oceanFloor,
                                 int[] motionBlocking,
                                 long kernelNanos) {}

    private record NativeSlotZeroBatch(double[] values, long computeNanos) {}

    private record ColumnRequest(long key, int blockX, int blockZ) {
        private long chunkKey() {
            return ChunkPos.asLong(blockX >> 4, blockZ >> 4);
        }
    }

    private static final class GridIndexList {
        private int[] values = new int[4];
        private int size;

        private void add(int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, size * 2);
            }
            values[size++] = value;
        }

        private int get(int index) {
            return values[index];
        }

        private int size() {
            return size;
        }
    }
}

