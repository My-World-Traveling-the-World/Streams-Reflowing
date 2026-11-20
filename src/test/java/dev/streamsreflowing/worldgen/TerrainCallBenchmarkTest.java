package dev.streamsreflowing.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** 用采样器推进的单调时钟测真实诊断循环，不能只验证三个外部给定数字的格式。 */
class TerrainCallBenchmarkTest {
    @Test void separateStageBoundariesKeepSeedCoordinatesOrderCountsAndCumulativeSink() {
        AtomicLong clock = new AtomicLong(500);
        var estimate = new ArrayList<Long>();
        var base = new ArrayList<Long>();
        var biome = new ArrayList<Long>();
        var results = new ArrayList<TerrainCallBenchmark.Pass>();
        double actual = TerrainCallBenchmark.measure((x, z) -> {
            clock.addAndGet(7); estimate.add(key(x, z)); return x + z;
        }, (x, z) -> {
            clock.addAndGet(11); base.add(key(x, z)); return x - z;
        }, (x, z) -> {
            clock.addAndGet(3); biome.add(key(x, z)); return Math.floorMod(x ^ z, 3);
        }, clock::get, results::add);
        assertEquals(2, results.size());
        assertEquals(2000, estimate.size());
        assertEquals(estimate, base);
        assertEquals(estimate, biome);
        Random oracle = new Random(20260810L);
        var expectedCoordinates = new ArrayList<Long>();
        double expected = 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 1000; i++) {
                int x = oracle.nextInt(8192) - 4096, z = oracle.nextInt(8192) - 4096;
                expectedCoordinates.add(key(x, z));
                expected += 2 * x + Math.floorMod(x ^ z, 3);
            }
            var result = results.get(pass);
            assertEquals(pass + 1, result.number());
            assertEquals(1000, result.calls());
            assertEquals(7000, result.estimateNanos());
            assertEquals(11000, result.baseNanos());
            assertEquals(3000, result.biomeNanos());
            assertEquals(expected, result.sink());
        }
        assertEquals(expectedCoordinates, estimate);
        assertNotEquals(estimate.subList(0, 1000), estimate.subList(1000, 2000),
                "原坐标是不同批次，日志不得再把第二轮宣称成同点热缓存");
        assertEquals(expected, actual);
    }

    @Test void absentEstimatorIsUnmeasurableWhileOtherStagesStillRunBothPasses() {
        AtomicLong clock = new AtomicLong(), bases = new AtomicLong(), biomes = new AtomicLong();
        var results = new ArrayList<TerrainCallBenchmark.Pass>();
        double total = TerrainCallBenchmark.measure(null, (x, z) -> {
            bases.incrementAndGet(); clock.addAndGet(13); return 4;
        }, (x, z) -> {
            biomes.incrementAndGet(); clock.addAndGet(17); return 2;
        }, clock::get, results::add);
        assertEquals(2, results.size());
        assertEquals(2000, bases.get());
        assertEquals(2000, biomes.get());
        for (var result : results) {
            assertEquals(-1, result.estimateNanos());
            assertEquals(13000, result.baseNanos());
            assertEquals(17000, result.biomeNanos());
        }
        assertEquals(12000, total);
    }

    @Test void failedStageDoesNotPublishCompletedMeasurementOrContinueNextStages() {
        var failure = new IllegalStateException("故障采样器");
        var results = new ArrayList<TerrainCallBenchmark.Pass>();
        AtomicLong estimates = new AtomicLong(), bases = new AtomicLong(), biomes = new AtomicLong();
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                TerrainCallBenchmark.measure((x, z) -> { estimates.incrementAndGet(); return 1; },
                        (x, z) -> { bases.incrementAndGet(); throw failure; },
                        (x, z) -> { biomes.incrementAndGet(); return 1; }, () -> 1L, results::add)));
        assertEquals(1000, estimates.get());
        assertEquals(1, bases.get());
        assertEquals(0, biomes.get());
        assertEquals(List.of(), results);
    }

    private static long key(int x, int z) { return (long) x << 32 ^ (z & 0xffffffffL); }
}
