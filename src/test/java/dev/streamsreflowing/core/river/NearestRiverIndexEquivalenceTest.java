package dev.streamsreflowing.core.river;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 独立旧查询核对全部采样字段；冷查询、同格复用和跨索引缓存必须同时覆盖。 */
class NearestRiverIndexEquivalenceTest {
    @Test void allQueriesMatchFrozenOnLongOverlappingAndDegenerateSegments() {
        var random = new Random(8116103);
        int samples = 0, hits = 0;
        for (int fixture = 0; fixture < 4; fixture++) {
            var segments = fixture(fixture, random);
            var actual = new NearestRiverIndex(segments, 24, 16, 1.4);
            var old = new FrozenNearestRiverIndex(segments, 24, 16, 1.4);
            for (int i = 0; i < 360; i++) {
                double x = random.nextInt(449) - 224 + .25;
                double z = random.nextInt(449) - 224 + .75;
                double radius = new double[]{0, 8, 16, 40, 96}[i % 5];
                int exclude = i % segments.size();
                // 两次相同查询让旧实现同时经过点范围冷路径与整16格缓存路径。
                for (int pass = 0; pass < 2; pass++) {
                    assertEquals(old.nearest(x, z, radius), actual.nearest(x, z, radius));
                    var tiered = actual.nearestTiered(x, z, radius);
                    assertEquals(old.nearestTiered(x, z, radius), tiered);
                    if (tiered != null) hits++;
                    assertEquals(old.nearestCoveringForeign(x, z, radius, exclude),
                            actual.nearestCoveringForeign(x, z, radius, exclude));
                    assertEquals(old.lowerNeighborGrade(x, z, radius, exclude, 96, 2),
                            actual.lowerNeighborGrade(x, z, radius, exclude, 96, 2));
                    assertEquals(old.inAnyChannel(x, z, radius), actual.inAnyChannel(x, z, radius));
                    assertEquals(old.anyCorridorTouchesBox(x, z, x + 4, z + 7),
                            actual.anyCorridorTouchesBox(x, z, x + 4, z + 7));
                    samples++;
                }
            }
        }
        assertEquals(2880, samples);
        assertTrue(hits > 500, "非空命中不足不能证明产物被保留");
        if (Boolean.getBoolean("streamsreflowing.riverQueryBenchmark")) RiverQueryBenchmark.run();
    }

    @Test void tieAndChannelPriorityKeepOriginalSegmentIdentityAndEverySampleField() {
        var segments = List.of(seg(-200, 0, 200, 0, 70, 68, 1),
                seg(-200, 0, 200, 0, 90, 88, 12), seg(0, -200, 0, 200, 80, 78, 3));
        var actual = new NearestRiverIndex(segments, 24, 16, 1.4);
        var old = new FrozenNearestRiverIndex(segments, 24, 16, 1.4);
        for (int pass = 0; pass < 4; pass++) {
            var nearest = actual.nearest(10, 0, 40);
            var tiered = actual.nearestTiered(10, 5, 40);
            assertNotNull(nearest, "候选消失必须由明确产物断言报错");
            assertNotNull(tiered);
            assertEquals(0, nearest.segId());
            assertEquals(1, tiered.segId());
            assertEquals(old.nearest(10, 0, 40), actual.nearest(10, 0, 40));
            assertEquals(old.nearestTiered(10, 5, 40), actual.nearestTiered(10, 5, 40));
        }
    }

    @Test void fullCellReuseAndDifferentIndexIdentityDoNotLoseBoundaryCandidates() {
        var indexes = new ArrayList<NearestRiverIndex>();
        var frozen = new ArrayList<FrozenNearestRiverIndex>();
        for (int i = 0; i < 70; i++) {
            var lines = List.of(seg(-25, 15, 40, 15, i + 64, i + 63, 2),
                    seg(15, -25, 15, 40, i + 74, i + 73, 2));
            indexes.add(new NearestRiverIndex(lines, 24, 16, 1.4));
            frozen.add(new FrozenNearestRiverIndex(lines, 24, 16, 1.4));
        }
        int hits = 0;
        for (int pass = 0; pass < 3; pass++) for (int i = 0; i < indexes.size(); i++) {
            for (double x : new double[]{-.01, 0, 15.99, 16, -16.01}) {
                for (double radius : new double[]{0, 1, 17}) {
                    var a = indexes.get(i);
                    var b = frozen.get(i);
                    assertEquals(b.nearestTiered(x, 15.99, radius), a.nearestTiered(x, 15.99, radius));
                    var sample = a.nearestTiered(x + .001, .01, radius);
                    assertEquals(b.nearestTiered(x + .001, .01, radius), sample);
                    if (sample != null) hits++;
                }
            }
        }
        assertTrue(hits > 1000);
    }

    @Test void emptyAndOutsideIndexStayEmpty() {
        var empty = new NearestRiverIndex(List.of(), 24, 16, 1.4);
        assertNull(empty.nearest(0, 0, 40));
        assertNull(empty.nearestTiered(0, 0, 40));
        assertFalse(empty.inAnyChannel(0, 0, 40));
        var segments = List.of(seg(-20, -20, 20, 20, 70, 68, 2));
        var index = new NearestRiverIndex(segments, 24, 16, 1.4);
        for (int pass = 0; pass < 3; pass++) assertNull(index.nearestTiered(10000, -10000, 40));
    }

    static List<RiverSegment> fixture(int type, Random random) {
        var result = new ArrayList<RiverSegment>();
        for (int i = 0; i < 64; i++) {
            double x = random.nextInt(321) - 160, z = random.nextInt(321) - 160;
            double dx = type == 0 ? 300 : type == 1 ? 0 : random.nextInt(161) - 80;
            double dz = type == 0 ? 300 : type == 1 ? 0 : random.nextInt(161) - 80;
            result.add(seg(x, z, x + dx, z + dz, 70 + i % 9, 68 + i % 7, 1 + i % 12));
        }
        return List.copyOf(result);
    }
    private static RiverSegment seg(double x, double z, double x2, double z2, double y, double y2, double width) {
        return new RiverSegment(x, z, y, width, x2, z2, y2, width);
    }
}
