package dev.streamsreflowing.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * plate 缓存落盘的验收测试：存 → 读 → isPlateReady 为 true，以及版本号/指纹不符、文件截断
 * 时整份丢弃的安全降级。
 *
 * <p>全程纯 Java：{@link RiverEngine} 只被用来查缓存状态，地形采样器一旦被调用就抛错 ——
 * 所以「命中磁盘后不重建」是被断言出来的，而不是靠日志推断。</p>
 */
class FileNetworkStoreTest {
    private static final long SEED = 20261107L;
    private static final long MOD_SET = 0x5EEDL;
    /** 文件头：magic(int) + 版本号(int) + 指纹(long)。版本号字段偏移 4。 */
    private static final int VERSION_OFFSET = 4;

    @TempDir
    Path dir;

    @Test
    void storeThenLoadMakesThePlateReadyWithoutBuilding() throws IOException {
        StreamsConfig config = StreamsConfig.defaults();
        PlateKey key = new PlateKey(3, -2);
        FileNetworkStore store = new FileNetworkStore(dir, SEED, config, MOD_SET);
        RiverNetwork built = sampleNetwork(key, config);

        store.save(key, built);
        assertTrue(store.isStored(key), "刚写下去的 plate 必须报命中");

        RiverEngine engine = new RiverEngine(SEED, config, neverSamples(), (x, y, z) -> 1.0D);
        engine.setNetworkStore(store);
        assertFalse(engine.isPlateReady(key), "新引擎的内存缓存一开始必须是空的");

        assertEquals(RiverEngine.PLATE_RESTORED, engine.plateCacheState(key),
                "磁盘命中必须报 PLATE_RESTORED");
        assertTrue(engine.isPlateReady(key), "磁盘命中后 isPlateReady 必须为 true");

        RiverNetwork remembered = engine.networkFor(key);
        assertNotNull(remembered);
        assertSameNetwork(built, remembered);
        // 再问一次就只剩内存命中，不会再读盘。
        assertEquals(RiverEngine.PLATE_READY, engine.plateCacheState(key));
    }

    @Test
    void fileVersionMismatchDiscardsTheCacheAndForcesARebuild() throws IOException {
        StreamsConfig config = StreamsConfig.defaults();
        PlateKey key = new PlateKey(-1, 4);
        FileNetworkStore store = new FileNetworkStore(dir, SEED, config, MOD_SET);
        store.save(key, sampleNetwork(key, config));
        assertTrue(store.isStored(key), "前置条件：文件先要能用");

        // 把文件头里的版本号改回旧值，模拟「上一版模组写出的缓存」。
        Path file = onlyPlateFile();
        ByteBuffer header = ByteBuffer.wrap(Files.readAllBytes(file));
        header.putInt(VERSION_OFFSET, 28);
        Files.write(file, header.array());

        FileNetworkStore reopened = new FileNetworkStore(dir, SEED, config, MOD_SET);
        assertFalse(reopened.isStored(key), "版本号不符必须整份丢弃");
        assertNull(reopened.load(key), "版本号不符时 load 必须返回 null");

        RiverEngine engine = new RiverEngine(SEED, config, neverSamples(), (x, y, z) -> 1.0D);
        engine.setNetworkStore(reopened);
        assertEquals(RiverEngine.PLATE_MISSING, engine.plateCacheState(key));
        assertFalse(engine.isPlateReady(key), "被丢弃的缓存不能算就绪");
    }

    @Test
    void fingerprintMismatchFromSeedModSetOrSamplingStepDiscardsTheCache() throws IOException {
        StreamsConfig config = StreamsConfig.defaults();
        StreamsConfig otherSamplingStep = StreamsConfig.defaults(12, 1.0D, 1024, 4, 0);
        PlateKey key = new PlateKey(0, 0);
        FileNetworkStore store = new FileNetworkStore(dir, SEED, config, MOD_SET);
        store.save(key, sampleNetwork(key, config));

        // 阳性对照：同一口径必须还能读出来，否则下面的 false 就没有意义。
        assertTrue(new FileNetworkStore(dir, SEED, config, MOD_SET).isStored(key));

        assertDiscarded(new FileNetworkStore(dir, SEED + 1L, config, MOD_SET), key, "世界种子变了");
        assertDiscarded(new FileNetworkStore(dir, SEED, config, MOD_SET + 1L), key, "模组集变了");
        assertDiscarded(new FileNetworkStore(dir, SEED, otherSamplingStep, MOD_SET), key, "采样步长变了");
    }

    @Test
    void truncatedCacheFileDegradesToAMissInsteadOfThrowing() throws IOException {
        StreamsConfig config = StreamsConfig.defaults();
        PlateKey key = new PlateKey(7, 7);
        FileNetworkStore store = new FileNetworkStore(dir, SEED, config, MOD_SET);
        store.save(key, sampleNetwork(key, config));
        Path file = onlyPlateFile();

        // 连文件头都不完整：直接读不出来，按未命中处理。
        byte[] whole = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(whole, 12));
        FileNetworkStore headTruncated = new FileNetworkStore(dir, SEED, config, MOD_SET);
        assertFalse(headTruncated.isStored(key));
        assertNull(headTruncated.load(key));

        // 文件头完好、正文被截断：isStored 会答命中（只读头），但 load 必须失败，
        // plateCacheState 因此仍然按「缺」处理 —— 宁可重建，也不能拿半份数据去生成地形。
        Files.write(file, java.util.Arrays.copyOf(whole, 24));
        FileNetworkStore bodyTruncated = new FileNetworkStore(dir, SEED, config, MOD_SET);
        assertTrue(bodyTruncated.isStored(key), "文件头完好时 isStored 只看头部");
        assertNull(bodyTruncated.load(key), "正文截断时 load 必须返回 null");

        RiverEngine engine = new RiverEngine(SEED, config, neverSamples(), (x, y, z) -> 1.0D);
        engine.setNetworkStore(bodyTruncated);
        assertEquals(RiverEngine.PLATE_MISSING, engine.plateCacheState(key));
        assertFalse(engine.isPlateReady(key));
    }

    @Test
    void loadOfAnAbsentPlateIsANormalMiss() {
        StreamsConfig config = StreamsConfig.defaults();
        PlateKey key = new PlateKey(99, -99);
        FileNetworkStore store = new FileNetworkStore(dir, SEED, config, MOD_SET);
        assertFalse(store.isStored(key));
        assertNull(store.load(key));
        RiverEngine engine = new RiverEngine(SEED, config, neverSamples(), (x, y, z) -> 1.0D);
        engine.setNetworkStore(store);
        assertEquals(RiverEngine.PLATE_MISSING, engine.plateCacheState(key));
    }

    private static void assertDiscarded(FileNetworkStore store, PlateKey key, String why) {
        assertFalse(store.isStored(key), why + "：必须整份丢弃");
        assertNull(store.load(key), why + "：load 必须返回 null");
    }

    private Path onlyPlateFile() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> plateFiles = new ArrayList<>();
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("r.")
                            && path.getFileName().toString().endsWith(".srn"))
                    .forEach(plateFiles::add);
            assertEquals(1, plateFiles.size(), "一次 save 只应留下一个 plate 文件");
            return plateFiles.get(0);
        }
    }

    /** 一块板建出来的最终产物：段 + 湖，正是 FileNetworkStore 落盘的东西。 */
    private static RiverNetwork sampleNetwork(PlateKey key, StreamsConfig config) {
        List<RiverSegment> segments = new ArrayList<>();
        segments.add(new RiverSegment(10.5D, -3.25D, 64.75D, 2.5D,
                40.125D, 12.5D, 62.0D, 1.75D));
        segments.add(new RiverSegment(40.125D, 12.5D, 62.0D, 1.75D,
                71.0D, 33.5D, 58.25D, 1.25D));
        List<Lake> lakes = new ArrayList<>();
        lakes.add(new Lake(62, 3, 16, -32, 0, 2, 1, new long[]{0b11L}));
        return NetworkTracer.assemble(key, segments, lakes, config);
    }

    /** 逐位比较读回来的网络与当初写下去的那一份。 */
    private static void assertSameNetwork(RiverNetwork expected, RiverNetwork actual) {
        assertEquals(expected.key(), actual.key());
        assertEquals(expected.segmentCount(), actual.segmentCount());
        for (int i = 0; i < expected.segmentCount(); i++) {
            RiverSegment left = expected.segments().get(i);
            RiverSegment right = actual.segments().get(i);
            assertEquals(left, right, "段 " + i + " 必须逐位相同");
            assertEquals(Double.doubleToLongBits(left.x1()), Double.doubleToLongBits(right.x1()));
            assertEquals(Double.doubleToLongBits(left.y2()), Double.doubleToLongBits(right.y2()));
            assertEquals(Double.doubleToLongBits(left.w2()), Double.doubleToLongBits(right.w2()));
        }
        assertEquals(expected.lakes().size(), actual.lakes().size());
        for (int i = 0; i < expected.lakes().size(); i++) {
            Lake left = expected.lakes().get(i);
            Lake right = actual.lakes().get(i);
            assertEquals(left.level(), right.level());
            assertEquals(left.digDepth(), right.digDepth());
            assertEquals(left.spacing(), right.spacing());
            assertEquals(left.cellMinX(), right.cellMinX());
            assertEquals(left.cellMinZ(), right.cellMinZ());
            assertEquals(left.cellsWide(), right.cellsWide());
            assertEquals(left.cellsHigh(), right.cellsHigh());
            assertTrue(java.util.Arrays.equals(left.mask(), right.mask()), "湖掩码必须逐位相同");
        }
    }

    /** 只要命中磁盘后仍然去采地形，测试就失败 —— 用它证明「命中后零构建」。 */
    private static TerrainSampler neverSamples() {
        return (x, z) -> {
            throw new AssertionError("命中磁盘缓存后不应再采地形：(" + x + ", " + z + ")");
        };
    }
}
