package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** 诊断采样独立于世界注册；各阶段保留自己的边界时间，不能覆盖后用终点减自身。 */
final class TerrainCallBenchmark {
    @FunctionalInterface interface Probe { double sample(int x, int z); }
    record Pass(int number, int calls, long estimateNanos, long baseNanos, long biomeNanos, double sink) {}

    private TerrainCallBenchmark() {}

    static double run(Probe estimate, Probe base, Probe biome) {
        return measure(estimate, base, biome, System::nanoTime, TerrainCallBenchmark::log);
    }

    static double measure(Probe estimate, Probe base, Probe biome, LongSupplier clock, Consumer<Pass> output) {
        Objects.requireNonNull(base);
        Objects.requireNonNull(biome);
        Objects.requireNonNull(clock);
        Objects.requireNonNull(output);
        Random random = new Random(20260810L);
        int calls = 1000;
        int[] xs = new int[calls], zs = new int[calls];
        double sink = 0;
        for (int pass = 1; pass <= 2; pass++) {
            // 保留旧种子和坐标生成顺序；两批坐标不同，第二批不能冒充同点热缓存。
            for (int i = 0; i < calls; i++) {
                xs[i] = random.nextInt(8192) - 4096;
                zs[i] = random.nextInt(8192) - 4096;
            }
            long start = clock.getAsLong();
            if (estimate != null) for (int i = 0; i < calls; i++) sink += estimate.sample(xs[i], zs[i]);
            long estimateEnd = clock.getAsLong();
            for (int i = 0; i < calls; i++) sink += base.sample(xs[i], zs[i]);
            long baseEnd = clock.getAsLong();
            for (int i = 0; i < calls; i++) sink += biome.sample(xs[i], zs[i]);
            long biomeEnd = clock.getAsLong();
            // 未提供估算器是“不可测”，不能输出零耗时伪装成已测且极快。
            output.accept(new Pass(pass, calls, estimate == null ? -1 : estimateEnd - start,
                    baseEnd - estimateEnd, biomeEnd - baseEnd, sink));
        }
        return sink;
    }

    private static void log(Pass pass) {
        String estimate = pass.estimateNanos() < 0 ? "n/a (无已校准估算器)"
                : String.format(Locale.ROOT, "%.1f ms (%.2f us/call)",
                        pass.estimateNanos() / 1e6, pass.estimateNanos() / 1e3 / pass.calls());
        Constants.LOG.info("【河流三类采样实测；不同坐标批次，不代表缓存冷热】 "
                        + "[SR-BENCH] pass {} (batch), {} calls each: IR estimate {} "
                        + "| getBaseHeight {} ms ({} us/call) | biome classify {} ms ({} us/call) [sink {}]",
                pass.number(), pass.calls(), estimate,
                String.format(Locale.ROOT, "%.1f", pass.baseNanos() / 1e6),
                String.format(Locale.ROOT, "%.2f", pass.baseNanos() / 1e3 / pass.calls()),
                String.format(Locale.ROOT, "%.1f", pass.biomeNanos() / 1e6),
                String.format(Locale.ROOT, "%.2f", pass.biomeNanos() / 1e3 / pass.calls()), (long) pass.sink());
    }
}
