/* 文件职责：固定整板坐标、河段顺序和湖掩码，为后续拆分提供独立对照。 */
package dev.streamsreflowing.core.river;

import static org.junit.jupiter.api.Assertions.*;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 在拆整板算法前冻结完整河段顺序和湖掩码；不能拿重构后的同一实现互相比。 */
class WholePlateFixtureTest {
    private static final int SCALE = 384;
    private static final long SEED = 17;
    private static final StreamsConfig CONFIG = StreamsConfig.defaults(8, 1, SCALE, 0, 0)
            .withMinRiverAccumulation(2.0).withLakeArea(96, 60_000).withMinSeaChainNodes(1);

    @Test void emptyCoastAndNegativePlateBasinKeepFrozenNetworks() throws Exception {
        var coast = trace(new PlateKey(0, 0), false);
        var basin = trace(new PlateKey(-2, -1), true);
        // 2026-11-23 用拆分前的 NetworkTracer 采集，并分别钉住无河与非空河湖。
        assertEquals("0/0/374708fff7719dd5979ec875d56cd2286f6d3cf7ec317a3b25632aab28ec37bb", signature(coast));
        assertEquals("1878/1/12e0ef17f9c04a2a55a63cf4aa7580ac52100d2c8849b4eb45ed1e2e1609100a", signature(basin));
    }

    @Test void seaLevelRiverContinuesIntoAnAdjacentHighRiverButNotTheOcean() {
        var key = new PlateKey(-2, -1);
        var domain = new PlateDomain(new PlateLattice(SEED, SCALE, 0), key);
        var grid = new LowResGrid(domain, (x, z) -> 100, CONFIG);
        int center = -1;
        for (int i = 1; i < grid.nx - 1 && center < 0; i++) {
            for (int j = 1; j < grid.nz - 1; j++) {
                if (grid.inDomain(grid.idx(i - 1, j)) && grid.inDomain(grid.idx(i, j)) && grid.inDomain(grid.idx(i + 1, j))) {
                    center = grid.idx(i, j);
                    break;
                }
            }
        }
        assertTrue(center >= 0, "夹具必须真有三个相邻的板内格");
        int i = grid.gi(center), j = grid.gj(center);
        int x0 = grid.worldX(i), x1 = grid.worldX(i + 1), west = grid.worldX(i - 1), z0 = grid.worldZ(j);
        var terrain = new TerrainSampler() {
            @Override public double height(int x, int z) { return 100; }
            @Override public int waterBiomeClass(int x, int z) {
                if (z != z0) return CLASS_LAND;
                if (x == x0) return CLASS_RIVER;
                return x == west ? CLASS_OCEAN : CLASS_LAND;
            }
            @Override public int waterBiomeClassAt(int x, int y, int z) {
                return x == x1 && y == 100 && z == z0 ? CLASS_RIVER : CLASS_LAND;
            }
        };
        // 平坦相邻格不满足高处凹槽独立种子条件，只能由已标记河口按实际海拔扩展。
        PlateOutletSeeds.mark(key, grid, terrain, CONFIG.seaLevel(),
                new int[]{1, 1, 0, -1, -1, -1, 0, 1}, new int[]{0, 1, 1, 1, 0, -1, -1, -1});
        assertTrue(grid.riverOutlet(center));
        assertTrue(grid.riverOutlet(grid.idx(i + 1, j)));
        assertFalse(grid.riverOutlet(grid.idx(i - 1, j)));
        assertEquals(-1000.0, grid.height(grid.idx(i - 1, j)));
    }

    @Test void priorityFloodKeepsFlatInteriorDrainingAndMatchesBothQueues() {
        var key = new PlateKey(-2, -1);
        var domain = new PlateDomain(new PlateLattice(SEED, SCALE, 0), key);
        TerrainSampler terrain = (x, z) -> 70;
        var grid = new LowResGrid(domain, terrain, CONFIG);
        var cells = new java.util.ArrayList<Integer>();
        for (int i = 0; i < grid.size(); i++) if (grid.inDomain(i)) cells.add(i);
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dz = {0, 1, 1, 1, 0, -1, -1, -1};
        int center = grid.idx(grid.nx / 2, grid.nz / 2);
        assertTrue(grid.inDomain(center));
        var filled = PlatePriorityFlood.fill(key, grid, terrain, cells, CONFIG.seaLevel(), dx, dz, true);
        var reference = PlatePriorityFlood.fill(key, grid, terrain, cells, CONFIG.seaLevel(), dx, dz, false);
        assertArrayEquals(reference, filled, "两种队列的填洼高度必须完全一致");
        assertTrue(filled[center] > 70.0, "无天然坡面仍要能从内陆流向边界");
        var flow = PlatePriorityFlood.downstream(grid, cells, filled, CONFIG.seaLevel(), dx, dz);
        assertTrue(flow[center] >= 0);
        assertTrue(filled[flow[center]] < filled[center]);
    }

    private static RiverNetwork trace(PlateKey key, boolean bowl) {
        var domain = new PlateDomain(new PlateLattice(SEED, SCALE, 0), key);
        var terrain = new TerrainSampler() {
            @Override public double height(int x, int z) {
                int u = x - key.cx() * SCALE, v = z - key.cz() * SCALE;
                int dx = u - 220, dz = v - 240;
                if (bowl && dx * dx + dz * dz < 64 * 64) return 67 + (dx * dx + dz * dz) / 2048.0;
                // 同一坡面加深凹地后必须产生非空河湖；无凹地时保持无产物。
                return 53 + u / 8.0 + Math.abs(v - 128) / 8.0 + ((x ^ z) & 7) / 16.0;
            }
            @Override public int waterBiomeClass(int x, int z) {
                int u = x - key.cx() * SCALE, v = z - key.cz() * SCALE;
                if (u < 24) return CLASS_OCEAN;
                if (u < 112 && Math.abs(v - 128) < 13) return CLASS_RIVER;
                return CLASS_LAND;
            }
        };
        return NetworkTracer.build(domain, terrain, (x, y, z) -> 1.0, CONFIG, SEED);
    }

    private static String signature(RiverNetwork net) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(net.key().cx());
            out.writeInt(net.key().cz());
            out.writeInt(net.segmentCount());
            for (var s : net.segments()) writeGeometry(out, s.x1(), s.z1(), s.y1(), s.w1(), s.x2(), s.z2(), s.y2(), s.w2());
            out.writeInt(net.lakes().size());
            for (var lake : net.lakes()) {
                for (int v : new int[]{lake.level(), lake.digDepth(), lake.spacing(), lake.cellMinX(), lake.cellMinZ(),
                        lake.cellsWide(), lake.cellsHigh()}) out.writeInt(v);
                long[] mask = lake.mask();
                out.writeInt(mask.length);
                for (long word : mask) out.writeLong(word);
            }
        }
        return net.segmentCount() + "/" + net.lakes().size() + "/"
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    }

    private static void writeGeometry(DataOutputStream out, double... values) throws IOException {
        for (double v : values) {
            assertTrue(Double.isFinite(v), "河段几何必须是有限值");
            // 保留远小于方块的分辨率，屏蔽数学库跨处理器末尾一位差异。
            out.writeLong(Math.round(v * 100_000_000.0));
        }
    }
}
