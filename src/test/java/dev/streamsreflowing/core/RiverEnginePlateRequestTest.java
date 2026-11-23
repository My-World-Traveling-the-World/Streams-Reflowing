package dev.streamsreflowing.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RiverEnginePlateRequestTest {
    @Test void asyncDemandUsesSameDiskNetworkAndRetriesAnInFlightLoad() {
        var config = StreamsConfig.defaults();
        var engine = new RiverEngine(41, config, (x, z) -> fail("命中落盘河网后不该再采高"));
        var key = engine.plateAt(200, -900);
        var network = NetworkTracer.assemble(key, List.of(), List.of(), config);
        var loads = new AtomicInteger();
        engine.setNetworkStore(new NetworkStore() {
            @Override public RiverNetwork load(PlateKey found) {
                assertEquals(key, found);
                if (loads.incrementAndGet() == 1) throw new IllegalStateException("模拟第一次读取失败");
                return network;
            }
            @Override public void save(PlateKey key, RiverNetwork network) { fail("恢复命中不应写回"); }
        });
        try {
            // 选板中心的查询点，使夹具只有一个明确的生产认领键。
            var query = plateInterior(engine, key);
            var queue = new ArrayDeque<Runnable>();
            var first = engine.whenPlatesReadyNear(query[0], query[1], 0, queue::add);
            var second = engine.whenPlatesReadyNear(query[0], query[1], 0, queue::add);
            assertEquals(1, queue.size());
            assertFalse(first.isDone());
            queue.remove().run();
            first.join();
            second.join();
            assertTrue(engine.isPlateReady(key));
            assertSame(network, engine.networkFor(key));
            assertEquals(2, loads.get());
        } finally {
            engine.close();
        }
        assertThrows(java.util.concurrent.CancellationException.class, () -> engine.networkFor(key),
                "已关闭世界不能从上一把线程内缓存读回河网");
    }

    @Test void closingEngineCompletesWaitingChunksWithoutRunningTheirQueue() {
        var engine = new RiverEngine(41, StreamsConfig.defaults(), (x, z) -> fail("关服前并未执行建板"));
        var queue = new ArrayDeque<Runnable>();
        var waiting = engine.whenPlatesReadyNear(200, -900, 48, queue::add);
        assertFalse(queue.isEmpty());
        engine.close();
        assertInstanceOf(java.util.concurrent.CancellationException.class,
                assertThrows(CompletionException.class, waiting::join).getCause());
        queue.forEach(Runnable::run);
        assertEquals(0, engine.cachedPlateCount());
    }

    @Test void oneFailedPlateEndsChunkWaitEvenIfOtherPlateHasNotRun() {
        var attempts = new AtomicInteger();
        var failure = new IllegalStateException("这块板无法构建");
        var engine = new RiverEngine(41, StreamsConfig.defaults(), (x, z) -> {
            throw new AssertionError("应先尝试从磁盘恢复");
        });
        engine.setNetworkStore(new NetworkStore() {
            @Override public RiverNetwork load(PlateKey key) {
                attempts.incrementAndGet();
                throw failure;
            }
            @Override public void save(PlateKey key, RiverNetwork network) { fail("失败后不应写回"); }
        });
        try {
            var queue = new ArrayDeque<Runnable>();
            var waiting = engine.whenPlatesReadyNear(0, 0, 1800, queue::add);
            assertTrue(queue.size() > 1, "夹具须覆盖多个板块");
            assertFalse(waiting.isDone());
            queue.remove().run();
            assertFalse(queue.isEmpty(), "其它板块还没开工");
            assertTrue(waiting.isCompletedExceptionally(), "不能继续等待未运行的板块");
            assertSame(failure, assertThrows(CompletionException.class, waiting::join).getCause());
            assertEquals(2, attempts.get());
        } finally {
            engine.close();
        }
    }

    @Test void sharedNeighborhoodWaitsForEveryColdKeyAndDoesNotQueueWarmKeys() {
        var config = StreamsConfig.defaults();
        var engine = new RiverEngine(41, config, (x, z) -> fail("夹具内的板块都从存储恢复"));
        var loaded = new java.util.ArrayList<PlateKey>();
        engine.setNetworkStore(new NetworkStore() {
            @Override public RiverNetwork load(PlateKey key) {
                loaded.add(key);
                return NetworkTracer.assemble(key, List.of(), List.of(), config);
            }
            @Override public void save(PlateKey key, RiverNetwork network) { fail("恢复命中不应写回"); }
        });
        try {
            var keys = engine.platesNear(200, -900, 1800);
            assertTrue(keys.length > 1, "夹具必须跨板边界");
            assertFalse(engine.platesReady(keys));
            var queue = new ArrayDeque<Runnable>();
            var waiting = engine.whenPlatesReady(keys, queue::add);
            assertEquals(keys.length, queue.size(), "每个缺板都必须排入且只排一次");
            queue.remove().run();
            assertFalse(waiting.isDone(), "只完成一块不能放行区块");
            assertFalse(engine.platesReady(keys));
            queue.forEach(Runnable::run);
            waiting.join();
            assertEquals(List.of(keys), loaded, "须按原顺序完成同一组邻板");
            assertTrue(engine.platesReady(keys));
            engine.whenPlatesReady(keys, ignored -> fail("热板不再排队")).join();
            assertEquals(keys.length, loaded.size());
        } finally {
            engine.close();
        }
    }

    private static int[] plateInterior(RiverEngine engine, PlateKey key) {
        for (int x = 120; x < 500; x += 32) {
            for (int z = -1024; z < -560; z += 32) {
                if (java.util.Arrays.equals(new PlateKey[]{key}, engine.platesNear(x, z, 0))) return new int[]{x, z};
            }
        }
        throw new AssertionError("夹具没有找到所选板块的内部查询点");
    }
}
