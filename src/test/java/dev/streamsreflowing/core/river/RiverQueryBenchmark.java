package dev.streamsreflowing.core.river;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** 固定查询输入，交替顺序测批次耗时；不把合成样本当作游戏世界的拉块开销。 */
final class RiverQueryBenchmark {
    private static final int QUERIES = 4096, WARMUP = 8, BATCHES = 15;

    private RiverQueryBenchmark() {}

    @FunctionalInterface
    private interface LowerQuery {
        double sample(double x, double z, double radius, int excluded, double height, double drop);
    }

    static void run() {
        for (int type : new int[] {0, 2}) run(type);
        RiverStreamingBenchmark.run();
    }

    private static void run(int type) {
        var segments = NearestRiverIndexEquivalenceTest.fixture(type, new Random(8116103));
        var current = new NearestRiverIndex(segments, 24, 16, 1.4);
        var reference = new FrozenNearestRiverIndex(segments, 24, 16, 1.4);
        var random = new Random(242411);
        var x = new double[QUERIES];
        var z = new double[QUERIES];
        int hits = 0;
        for (int i = 0; i < QUERIES; i++) {
            x[i] = random.nextDouble(-128, 160);
            z[i] = random.nextDouble(-128, 160);
            if (Double.isFinite(current.lowerNeighborGrade(x[i], z[i], 64, i % segments.size(), 96, 2))) hits++;
        }
        if (hits == 0) throw new AssertionError("密集夹具未命中低邻河");
        long[] old = new long[BATCHES], next = new long[BATCHES];
        for (int i = -WARMUP; i < BATCHES; i++) {
            Batch a, b;
            // 调换顺序，减轻同一进程预热或调度波动偏向一侧。
            if ((i & 1) == 0) {
                a = time(reference::lowerNeighborGrade, x, z, segments.size());
                b = time(current::lowerNeighborGrade, x, z, segments.size());
            } else {
                b = time(current::lowerNeighborGrade, x, z, segments.size());
                a = time(reference::lowerNeighborGrade, x, z, segments.size());
            }
            if (a.checksum() != b.checksum()) throw new AssertionError("基准查询输出与冻结实现不同");
            if (i >= 0) {
                old[i] = a.nanos();
                next[i] = b.nanos();
            }
        }
        Arrays.sort(old);
        Arrays.sort(next);
        System.out.printf(Locale.ROOT,
                "【河网低邻河合成基准】 RIVER_QUERY_BENCH fixture=%s segments=%d hits=%d queriesPerBatch=%d batches=%d "
                        + "oldP50NsPerQuery=%d newP50NsPerQuery=%d oldP90NsPerQuery=%d newP90NsPerQuery=%d%n",
                type == 0 ? "overlap" : "short", segments.size(), hits, QUERIES, BATCHES,
                old[BATCHES / 2] / QUERIES, next[BATCHES / 2] / QUERIES,
                old[(int) Math.ceil(BATCHES * .90) - 1] / QUERIES,
                next[(int) Math.ceil(BATCHES * .90) - 1] / QUERIES);
    }

    private static Batch time(LowerQuery query, double[] x, double[] z, int segments) {
        long checksum = 0;
        long start = System.nanoTime();
        for (int i = 0; i < x.length; i++) {
            checksum = 31 * checksum + Double.doubleToLongBits(query.sample(x[i], z[i], 64, i % segments, 96, 2));
        }
        return new Batch(System.nanoTime() - start, checksum);
    }

    private record Batch(long nanos, long checksum) {}
}
