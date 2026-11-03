package dev.streamsreflowing.compat.streamscontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import dev.streamsreflowing.compat.streamscontrol.gpu.AccurateHeightSample;
import dev.streamsreflowing.compat.streamscontrol.gpu.StreamsExactOpenCLBackend;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Aggregates exact terrain requests so one GPU submission serves several Streams plates. */
final class StreamsGpuBatchDispatcher implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final int MAX_COLUMNS = 16_384;
    private static final long MERGE_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);
    private static final long STARTUP_MERGE_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(150L);
    private static final int PROBES_PER_BATCH = 16;
    private static final int MAX_EXACT_CACHE_COLUMNS = 262_144;

    private final StreamsExactOpenCLBackend backend;
    private final LinkedBlockingDeque<Request> queue = new LinkedBlockingDeque<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong inputColumns = new AtomicLong();
    private final AtomicLong uniqueColumns = new AtomicLong();
    private final AtomicLong outstandingColumns = new AtomicLong();
    private final AtomicLong gpuNanos = new AtomicLong();
    private final AtomicLong queueWaitNanos = new AtomicLong();
    private final AtomicLong mergeWaitNanos = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final Map<Long, AccurateHeightSample> exactCache = Collections.synchronizedMap(
            new LinkedHashMap<>(16_384, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, AccurateHeightSample> eldest) {
                    return size() > MAX_EXACT_CACHE_COLUMNS;
                }
            });
    private final Thread worker;

    StreamsGpuBatchDispatcher(StreamsExactOpenCLBackend backend) {
        this.backend = backend;
        this.worker = Thread.ofPlatform()
                .name("plaincraft-streams-gpu-batcher")
                .daemon(true)
                .start(this::run);
    }

    CompletableFuture<Map<Long, AccurateHeightSample>> trySubmit(
            int[] xs, int[] zs, int count, long maxOutstandingColumns,
            ExactCpuSampler cpuSampler) {
        if (!running.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Streams GPU batch dispatcher is closed"));
        }
        if (!reserveColumns(count, maxOutstandingColumns)) {
            return null;
        }
        int[] requestXs = java.util.Arrays.copyOf(xs, count);
        int[] requestZs = java.util.Arrays.copyOf(zs, count);
        Request request = new Request(requestXs, requestZs, cpuSampler, System.nanoTime());
        queue.addLast(request);
        return request.result();
    }

    int queueDepth() {
        return queue.size();
    }

    long outstandingColumns() {
        return outstandingColumns.get();
    }

    long completedBatches() {
        return batches.get();
    }

    long completedUniqueColumns() {
        return uniqueColumns.get();
    }

    long completedGpuNanos() {
        return gpuNanos.get();
    }

    private void run() {
        while (running.get() || !queue.isEmpty()) {
            try {
                Request first = queue.pollFirst(100L, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                List<Request> merged = collect(first);
                execute(merged);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable failure) {
                LOGGER.warn("Streams GPU batch worker recovered from an unexpected failure.", failure);
            }
        }
        failQueued(new IllegalStateException("Streams GPU batch dispatcher stopped"));
    }

    private List<Request> collect(Request first) throws InterruptedException {
        long started = System.nanoTime();
        int columns = first.size();
        List<Request> merged = new ArrayList<>();
        merged.add(first);
        boolean startup = StreamsSamplerModeController.isInitialGeneration();
        long deadline = started + (startup ? STARTUP_MERGE_WINDOW_NANOS : MERGE_WINDOW_NANOS);
        int minimumColumns = startup ? startupMinimumMergeColumns() : 0;
        while (columns < MAX_COLUMNS && (columns < minimumColumns || System.nanoTime() < deadline)) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
                break;
            }
            Request next = queue.pollFirst(remaining, TimeUnit.NANOSECONDS);
            if (next == null) {
                break;
            }
            if (columns + next.size() > MAX_COLUMNS && !merged.isEmpty()) {
                queue.addFirst(next);
                break;
            }
            merged.add(next);
            columns += next.size();
        }
        mergeWaitNanos.addAndGet(System.nanoTime() - started);
        return merged;
    }

    private static int startupMinimumMergeColumns() {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        return Math.max(128, Math.min(512, (processors / 8) * 128));
    }

    private void execute(List<Request> merged) {
        long started = System.nanoTime();
        long now = started;
        int rawColumns = 0;
        LinkedHashMap<Long, Candidate> unique = new LinkedHashMap<>();
        HashSet<Long> uniqueChunks = new HashSet<>();
        for (Request request : merged) {
            rawColumns += request.size();
            queueWaitNanos.addAndGet(Math.max(0L, now - request.enqueuedNanos()));
            for (int index = 0; index < request.size(); index++) {
                int x = request.xs()[index];
                int z = request.zs()[index];
                uniqueChunks.add(net.minecraft.world.level.ChunkPos.asLong(x >> 4, z >> 4));
                unique.putIfAbsent(AccurateHeightSample.key(x, z),
                        new Candidate(x, z, request.cpuSampler()));
            }
        }

        try {
            LinkedHashMap<Long, AccurateHeightSample> allSamples = new LinkedHashMap<>(unique.size());
            LinkedHashMap<Long, Candidate> uncached = new LinkedHashMap<>(unique.size());
            for (Map.Entry<Long, Candidate> entry : unique.entrySet()) {
                AccurateHeightSample cached = exactCache.get(entry.getKey());
                if (cached != null) {
                    allSamples.put(entry.getKey(), cached);
                    cacheHits.incrementAndGet();
                } else {
                    uncached.put(entry.getKey(), entry.getValue());
                    cacheMisses.incrementAndGet();
                }
            }
            List<BlockPos> positions = new ArrayList<>(uncached.size());
            for (Candidate candidate : uncached.values()) {
                positions.add(new BlockPos(candidate.x(), 0, candidate.z()));
            }
            Map<Long, AccurateHeightSample> samples = positions.isEmpty()
                    ? Map.of() : backend.sampleOceanFloorPositions(positions);
            validate(uncached, samples, batches.get() == 0L && !uncached.isEmpty());
            allSamples.putAll(samples);
            exactCache.putAll(samples);
            List<Map<Long, AccurateHeightSample>> responses = new ArrayList<>(merged.size());
            for (Request request : merged) {
                LinkedHashMap<Long, AccurateHeightSample> response = new LinkedHashMap<>(request.size());
                for (int index = 0; index < request.size(); index++) {
                    long key = AccurateHeightSample.key(request.xs()[index], request.zs()[index]);
                    AccurateHeightSample sample = allSamples.get(key);
                    if (sample == null) {
                        throw new IllegalStateException("GPU cache omitted terrain column "
                                + request.xs()[index] + "," + request.zs()[index]);
                    }
                    response.put(key, sample);
                }
                responses.add(response);
            }
            for (int index = 0; index < merged.size(); index++) {
                merged.get(index).result().complete(responses.get(index));
                outstandingColumns.addAndGet(-merged.get(index).size());
            }
            long elapsed = System.nanoTime() - started;
            long batch = batches.incrementAndGet();
            requests.addAndGet(merged.size());
            inputColumns.addAndGet(rawColumns);
            uniqueColumns.addAndGet(uncached.size());
            gpuNanos.addAndGet(elapsed);
            if (batch == 1L || batch % 8L == 0L) {
                double throughput = gpuNanos.get() == 0L ? 0.0D
                        : uniqueColumns.get() * 1_000_000_000.0D / gpuNanos.get();
                LOGGER.info("Streams GPU aggregate: batches={}, requests={}, inputColumns={}, uniqueColumns={}, "
                                + "lastRequests={}, lastInput={}, lastUnique={}, lastChunks={}, colsPerChunk={}, "
                                + "lastMs={}, throughput={} cols/s, "
                                + "queueDepth={}, queueWaitMs={}, mergeWaitMs={}, cacheHits={}, cacheMisses={}, "
                                + "cacheSize={}, validation=exact-pass, device={}.",
                        batch, requests.get(), inputColumns.get(), uniqueColumns.get(),
                        merged.size(), rawColumns, uncached.size(), uniqueChunks.size(),
                        uniqueChunks.isEmpty() ? 0.0D : uncached.size() / (double) uniqueChunks.size(),
                        elapsed / 1_000_000L,
                        Math.round(throughput), queue.size(), queueWaitNanos.get() / 1_000_000L,
                        mergeWaitNanos.get() / 1_000_000L, cacheHits.get(), cacheMisses.get(),
                        exactCache.size(), backend.deviceName());
            }
        } catch (Throwable failure) {
            running.set(false);
            CompletionException wrapped = new CompletionException(failure);
            for (Request request : merged) {
                request.result().completeExceptionally(wrapped);
                outstandingColumns.addAndGet(-request.size());
            }
            failQueued(wrapped);
        }
    }

    private boolean reserveColumns(int count, long maximum) {
        while (running.get()) {
            long current = outstandingColumns.get();
            if (current + count > maximum) {
                return false;
            }
            if (outstandingColumns.compareAndSet(current, current + count)) {
                return true;
            }
        }
        return false;
    }

    private static void validate(Map<Long, Candidate> candidates,
                                 Map<Long, AccurateHeightSample> samples,
                                 boolean validateAll) {
        if (candidates.isEmpty()) {
            return;
        }
        List<Map.Entry<Long, Candidate>> entries = new ArrayList<>(candidates.entrySet());
        int probes = validateAll ? entries.size() : Math.min(PROBES_PER_BATCH, entries.size());
        for (int probe = 0; probe < probes; probe++) {
            int index = probes == 1 ? 0
                    : (int) ((long) probe * (entries.size() - 1L) / (probes - 1L));
            Map.Entry<Long, Candidate> entry = entries.get(index);
            Candidate candidate = entry.getValue();
            AccurateHeightSample sample = samples.get(entry.getKey());
            if (sample == null) {
                throw new IllegalStateException("OpenCL omitted terrain column "
                        + candidate.x() + "," + candidate.z());
            }
            double expected = candidate.cpuSampler().sample(candidate.x(), candidate.z());
            if (Double.compare(expected, sample.oceanFloorWg()) != 0) {
                throw new IllegalStateException("strict CPU/GPU terrain mismatch at "
                        + candidate.x() + "," + candidate.z() + ": cpu=" + expected
                        + ", gpu=" + sample.oceanFloorWg());
            }
        }
        if (validateAll) {
            LOGGER.info("Streams optimized ocean-floor path passed full first-batch CPU validation: columns={}.",
                    entries.size());
        }
    }

    private void failQueued(Throwable failure) {
        Request request;
        while ((request = queue.pollFirst()) != null) {
            request.result().completeExceptionally(failure);
            outstandingColumns.addAndGet(-request.size());
        }
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            worker.interrupt();
            failQueued(new IllegalStateException("Streams GPU batch dispatcher closed"));
        }
        exactCache.clear();
        if (Thread.currentThread() != worker) {
            try {
                worker.join(5_000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @FunctionalInterface
    interface ExactCpuSampler {
        double sample(int x, int z);
    }

    private record Candidate(int x, int z, ExactCpuSampler cpuSampler) {
    }

    private record Request(int[] xs, int[] zs, ExactCpuSampler cpuSampler,
                           long enqueuedNanos,
                           CompletableFuture<Map<Long, AccurateHeightSample>> result) {
        private Request(int[] xs, int[] zs, ExactCpuSampler cpuSampler, long enqueuedNanos) {
            this(xs, zs, cpuSampler, enqueuedNanos, new CompletableFuture<>());
        }

        private int size() {
            return xs.length;
        }
    }
}
