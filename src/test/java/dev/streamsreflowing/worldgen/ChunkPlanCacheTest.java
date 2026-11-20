package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.core.compose.ColumnPlan;
import java.util.HashMap;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 保留完整键/身份与取走语义；32项容量不因几何方向而坍缩。 */
class ChunkPlanCacheTest {
    @Test void productionFacadeRetainsThirtyTwoDiagonalChunks() {
        ChunkPlanCache.clear();
        var plans = new ColumnPlan[32][];
        for (int i = 0; i < 32; i++) {
            plans[i] = new ColumnPlan[] { ColumnPlan.NONE };
            ChunkPlanCache.put(null, i * 16, i * 16, plans[i]);
        }
        for (int i = 0; i < 32; i++)
            assertSame(plans[i], ChunkPlanCache.get(null, i * 16, i * 16));
        for (int i = 0; i < 32; i++) {
            assertSame(plans[i], ChunkPlanCache.take(null, i * 16, i * 16));
            assertNull(ChunkPlanCache.get(null, i * 16, i * 16));
        }
        ChunkPlanCache.clear();
    }

    @Test void fullIdentityNotEqualsAndSignedKeysAreIndependent() throws Exception {
        var table = new BoundedPlanTable<String>(32, true);
        // 必须让不同身份确实落入同一桶；不同桶时删除身份判据仍可能全绿。
        var home = BoundedPlanTable.class.getDeclaredMethod("home", Object.class, long.class);
        home.setAccessible(true);
        var buckets = new HashMap<Integer, Object>();
        Object a = null, b = null;
        for (int attempt = 0; attempt <= 64; attempt++) {
            Object next = new String("同名");
            Object previous = buckets.putIfAbsent((Integer) home.invoke(table, next, 0L), next);
            if (previous != null) { a = previous; b = next; break; }
        }
        assertNotNull(a); assertNotNull(b); assertNotSame(a, b); assertEquals(a, b);
        for (long key : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            table.put(a, key, "甲" + key);
            table.put(b, key, "乙" + key);
            assertEquals("甲" + key, table.get(a, key, false));
            assertEquals("乙" + key, table.get(b, key, false));
            assertNull(table.get(null, key, false));
        }
    }

    @Test void removalAndReinsertionPreserveEveryProbeChain() {
        var table = new BoundedPlanTable<Integer>(32, true);
        var expected = new HashMap<Long, Integer>();
        var random = new Random(20261120);
        Object owner = new Object();
        for (int step = 0; step < 15000; step++) {
            long key = random.nextInt(32) - 16;
            if (random.nextBoolean()) {
                table.put(owner, key, step); expected.put(key, step);
            } else {
                assertEquals(expected.remove(key), table.get(owner, key, true));
            }
            // 键宇宙不超容量，每个操作后都对照独立Map，不允许空集合使断言空转。
            for (long check = -16; check < 16; check++)
                assertEquals(expected.get(check), table.get(owner, check, false));
        }
    }

    @Test void capacityEvictionAndFreeSlotsRemainBounded() {
        var table = new BoundedPlanTable<Integer>(4, true);
        Object owner = new Object();
        for (int i = 0; i < 4; i++) table.put(owner, i, i);
        table.get(owner, 2, true); table.put(owner, 8, 8);
        assertEquals(0, table.stats().evictions());
        // 缓存容量固定，反复淘汰也必须终止且保持索引完整，不能把挂起算成抓错。
        assertDoesNotThrow(() -> {
            for (int i = 10; i < 100; i++) table.put(owner, i, i);
        });
        int retained = 0;
        for (int i = 0; i < 100; i++) if (table.get(owner, i, false) != null) retained++;
        assertEquals(4, retained); assertEquals(90, table.stats().evictions());
    }

    @Test void clearDropsAllOwnersAndDiagnosticCounts() {
        var table = new BoundedPlanTable<Integer>(4, true);
        table.put(null, 0, 4); assertEquals(4, table.get(null, 0, false));
        assertNull(table.get(null, 1, false));
        assertEquals(1, table.stats().hits()); assertEquals(1, table.stats().misses());
        table.clear();
        assertEquals(new BoundedPlanTable.Stats(0, 0, 0, 0), table.stats());
        assertNull(table.get(null, 0, false));
        var quiet = new BoundedPlanTable<Integer>(4, false);
        quiet.put(null, 0, 1); quiet.get(null, 0, false); quiet.get(null, 3, false);
        assertEquals(new BoundedPlanTable.Stats(0, 0, 0, 0), quiet.stats());
    }

    @Test void productionClearInvalidatesAnotherThreadsRing() throws Exception {
        var ready = new java.util.concurrent.CountDownLatch(1);
        var cleared = new java.util.concurrent.CountDownLatch(1);
        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread thread = new Thread(() -> {
            try {
                ChunkPlanCache.put(null, -16, -16, new ColumnPlan[] { ColumnPlan.NONE });
                ready.countDown();
                assertTrue(cleared.await(3, java.util.concurrent.TimeUnit.SECONDS));
                assertNull(ChunkPlanCache.get(null, -16, -16));
            } catch (Throwable failure) { error.set(failure); ready.countDown(); }
        });
        thread.start();
        try { assertTrue(ready.await(3, java.util.concurrent.TimeUnit.SECONDS)); ChunkPlanCache.clear(); }
        finally { cleared.countDown(); thread.join(4000); }
        assertFalse(thread.isAlive()); assertNull(error.get());
    }
}
