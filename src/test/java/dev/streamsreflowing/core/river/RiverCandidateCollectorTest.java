package dev.streamsreflowing.core.river;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证去重不漏、顺序、扩容、索引切换及代次回卷，空集合不能替代有效候选。 */
class RiverCandidateCollectorTest {
    @Test void realIndexColdAndWarmCandidatePathsContainEachCrossBucketSegmentOnce() throws Exception {
        var lines = java.util.List.of(
                new RiverSegment(-200, -200, 70, 4, 200, 200, 68, 4),
                new RiverSegment(-190, -200, 72, 4, 210, 200, 70, 4),
                new RiverSegment(-180, -200, 74, 4, 220, 200, 72, 4));
        var index = new NearestRiverIndex(lines, 24, 16, 1.4);
        var method = NearestRiverIndex.class.getDeclaredMethod("a", double.class, double.class, double.class);
        method.setAccessible(true);
        for (int pass = 0; pass < 3; pass++) {
            var candidates = (int[]) method.invoke(index, 0., 0., 60.);
            assertArrayEquals(new int[]{0, 1, 2, -1}, Arrays.copyOf(candidates, 4));
        }
        var field = NearestRiverIndex.class.getDeclaredField("D");
        field.setAccessible(true);
        var collector = (RiverCandidateCollector) ((ThreadLocal<?>) field.get(null)).get();
        assertTrue(collector.inputs() > 100, "夹具必须真的从多个桶重复读到相同河段");
        assertEquals(3, collector.size());
        assertNotNull(index.nearestTiered(0, 0, 60));
    }

    @Test void duplicatesReduceToUniqueFirstOccurrenceAndSortedCacheOrder() {
        var collector = new RiverCandidateCollector();
        collector.begin(1024);
        int[] source = {65, 1, 65, 64, 1023, 0, 64, 65};
        collector.addRange(source, 0, source.length);
        assertEquals(8, collector.inputs());
        assertEquals(5, collector.size());
        assertArrayEquals(new int[]{65, 1, 64, 1023, 0, -1}, Arrays.copyOf(collector.scratch(), 6));
        assertArrayEquals(new int[]{0, 1, 64, 65, 1023, -1}, collector.sortedCopy());
    }

    @Test void randomRangesGrowthAndSuccessiveIndexesMatchIndependentOrderedSet() {
        var collector = new RiverCandidateCollector();
        var random = new Random(93751);
        int total = 0;
        for (int round = 0; round < 140; round++) {
            int bound = round % 3 == 0 ? 4096 : 193;
            collector.begin(bound);
            var expected = new LinkedHashSet<Integer>();
            int[] source = random.ints(5000, 0, bound).toArray();
            for (int from = 0; from < source.length; from += 200) {
                collector.addRange(source, from, from + 200);
                for (int j = from; j < from + 200; j++) expected.add(source[j]);
            }
            int[] ordered = expected.stream().mapToInt(Integer::intValue).toArray();
            int[] actual = Arrays.copyOf(collector.scratch(), ordered.length);
            assertArrayEquals(ordered, actual);
            assertEquals(-1, collector.scratch()[ordered.length]);
            assertEquals(5000, collector.inputs());
            Arrays.sort(ordered);
            var sorted = collector.sortedCopy();
            assertArrayEquals(ordered, Arrays.copyOf(sorted, ordered.length));
            assertEquals(-1, sorted[ordered.length]);
            total += ordered.length;
        }
        assertTrue(total > 100000);
    }

    @Test void epochWrapClearsAncientBitsAndEmptyDoesNotLeakPreviousCandidates() throws Exception {
        var collector = new RiverCandidateCollector();
        collector.begin(64);
        collector.addRange(new int[]{7, 7}, 0, 2);
        var field = RiverCandidateCollector.class.getDeclaredField("epoch");
        field.setAccessible(true);
        field.setInt(collector, -1);
        collector.begin(64);
        collector.addRange(new int[]{7, 9}, 0, 2);
        assertArrayEquals(new int[]{7, 9, -1}, collector.sortedCopy());
        collector.begin(0);
        assertArrayEquals(new int[]{-1}, collector.sortedCopy());
    }
}
