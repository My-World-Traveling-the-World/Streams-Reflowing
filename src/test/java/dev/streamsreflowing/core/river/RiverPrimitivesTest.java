package dev.streamsreflowing.core.river;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 迁移必须保持同权堆次序和旧链根标签；不用两个新入口互相比证明等价。 */
class RiverPrimitivesTest {
    @Test void heapMatchesIndependentPriorityQueueIncludingTiesAndExceptionalDoubles() {
        var actual = new DoubleMinHeap();
        var expected = new PriorityQueue<double[]>(Comparator.comparingDouble(v -> v[0]));
        var random = new Random(31877);
        double[] choices = {-0.0, 0.0, 1, 1, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int popped = 0;
        // 初始大批同权项强制扩容，避免小夹具错过siftDown和左右同权选择。
        for (int i = 0; i < 1024; i++) {
            actual.add(1, i);
            expected.add(new double[]{1, i});
        }
        for (int i = 1024; i < 26000; i++) {
            if (random.nextBoolean() && !expected.isEmpty()) {
                comparePop(actual, expected);
                popped++;
            } else {
                double key = i % 5 == 0 ? random.nextDouble() : choices[random.nextInt(choices.length)];
                actual.add(key, i);
                expected.add(new double[]{key, i});
            }
            assertEquals(expected.isEmpty(), actual.isEmpty());
        }
        while (!expected.isEmpty()) { comparePop(actual, expected); popped++; }
        assertTrue(popped > 10000);
        assertTrue(actual.isEmpty());
    }

    private static void comparePop(DoubleMinHeap actual, PriorityQueue<double[]> expected) {
        var entry = expected.remove();
        assertEquals(Double.doubleToLongBits(entry[0]), Double.doubleToLongBits(actual.key()));
        assertEquals((int) entry[1], actual.value());
        actual.pop();
    }

    @Test void uniqueEndpointsJoinButForksRemainSeparate() {
        var list = List.of(segment(-3, 0, -2, 0), segment(-2, 0, -1, 0),
                segment(-1, 0, 0, 0), segment(0, 0, 1, 0), segment(0, 0, 1, 1),
                segment(9, 9, 10, 9));
        var ids = NetworkTracer.chainIds(list);
        assertEquals(list.size(), ids.length);
        assertEquals(ids[0], ids[1]);
        assertEquals(ids[1], ids[2]);
        assertNotEquals(ids[2], ids[3]);
        assertNotEquals(ids[3], ids[4]);
        assertNotEquals(ids[0], ids[5]);
        assertArrayEquals(FrozenRiverChains.chainIds(list), ids);
    }

    @Test void oldExactLabelsAndQuantizationSurviveShuffledCyclesCollisionsAndNegativeRounding() throws Exception {
        var random = new Random(851901);
        int compared = 0;
        for (int round = 0; round < 320; round++) {
            var list = new ArrayList<RiverSegment>();
            int count = round == 0 ? 0 : random.nextInt(350) + 1;
            for (int i = 0; i < count; i++) {
                // 小坐标域产生大量重复端点、环和分叉；半格附近覆盖负数Math.round语义。
                double x = random.nextInt(17) - 8, z = random.nextInt(17) - 8;
                double dx = (random.nextInt(3) - 1) / 32.0;
                list.add(segment(x + dx, z, random.nextInt(17) - 8, random.nextInt(17) - 8));
            }
            java.util.Collections.shuffle(list, random);
            assertArrayEquals(FrozenRiverChains.chainIds(list), NetworkTracer.chainIds(list));
            compared += list.size();
        }
        assertTrue(compared > 40000);
        var method = NetworkTracer.class.getDeclaredMethod("a", double.class, double.class);
        method.setAccessible(true);
        for (double x : new double[]{-1.03125, -0.03125, 0, 0.03125, 1.03125, 268435457.0}) {
            long oldKey = (Math.round(x * 16) & 0xffffffffL) << 32 | (Math.round(-x * 16) & 0xffffffffL);
            assertEquals(oldKey, method.invoke(null, x, -x));
        }
    }

    @Test void tracerUsesSharedPrimitiveTypesRatherThanHiddenOldCopies() {
        var nested = java.util.Arrays.stream(NetworkTracer.class.getDeclaredClasses()).map(Class::getSimpleName).toList();
        assertFalse(nested.contains("DoubleMinHeap"));
        assertFalse(nested.contains("ChainEndpointIndex"));
        assertTrue(java.lang.reflect.Modifier.isFinal(DoubleMinHeap.class.getModifiers()));
    }

    private static RiverSegment segment(double x1, double z1, double x2, double z2) {
        return new RiverSegment(x1, z1, 70, 2, x2, z2, 69, 2);
    }
}
