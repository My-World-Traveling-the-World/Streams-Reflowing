package dev.streamsreflowing.compat.streamscontrol;

/** Resolves startup-only river concurrency from CPU and heap capacity. */
record StreamsStartupHardwareProfile(
        int processors,
        long maxHeapMiB,
        int samplerThreads,
        int plateWorkers) {

    private static final long MIB = 1024L * 1024L;

    static StreamsStartupHardwareProfile current(int configuredSamplerThreads,
                                                 int configuredPlateWorkers) {
        return resolve(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory(),
                configuredSamplerThreads,
                configuredPlateWorkers);
    }

    static StreamsStartupHardwareProfile resolve(int availableProcessors,
                                                  long maxHeapBytes,
                                                  int configuredSamplerThreads,
                                                  int configuredPlateWorkers) {
        int processors = Math.max(1, availableProcessors);
        long heapMiB = Math.max(512L, maxHeapBytes / MIB);
        long heapGiB = Math.max(1L, heapMiB / 1024L);

        int reserved = processors >= 12 ? 2 : processors >= 6 ? 1 : 0;
        int cpuBudget = Math.max(1, processors - reserved);
        int memoryBudget = heapGiB <= 4L
                ? Math.max(2, processors / 3)
                : (int) Math.min(64L, Math.max(4L, (heapGiB - 2L) * 3L));
        int samplerCap = configuredSamplerThreads <= 0 ? processors : configuredSamplerThreads;
        int samplerThreads = Math.max(1, Math.min(cpuBudget, Math.min(memoryBudget, samplerCap)));

        // Keep enough independent plate owners feeding the sampler so the GPU
        // batch queue does not drain between plates. This remains bounded below
        // the sampler count and is only used during startup prewarm.
        int automaticPlateWorkers = Math.max(2, Math.min(12, (samplerThreads + 1) / 2));
        int plateWorkers = configuredPlateWorkers <= 0
                ? automaticPlateWorkers
                : Math.max(1, Math.min(processors, configuredPlateWorkers));
        return new StreamsStartupHardwareProfile(
                processors, heapMiB, samplerThreads, plateWorkers);
    }
}
