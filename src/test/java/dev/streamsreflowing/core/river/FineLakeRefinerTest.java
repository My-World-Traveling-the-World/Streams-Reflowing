package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 使用独立冻结算法，连掩码和统计累加也对拍；夹具必须同时产生成功湖与拒绝。 */
class FineLakeRefinerTest {
    private static final PlateDomain DOMAIN = new PlateDomain(new PlateLattice(17, 192, 0), new PlateKey(0, 0));

    @Test void bowlProducesIdenticalNonemptyLake() {
        var sampler = bowl(96, 96, 25);
        var grid = grid(sampler);
        Lake lake = compare(cells(grid, 96, 96, 25), 60, sampler, grid, 0);
        assertNotNull(lake);
        assertTrue(java.util.Arrays.stream(lake.mask()).map(Long::bitCount).sum() > 100);
        assertTrue(lake.covers(96, 96));
    }

    @Test void dryAndDomainBorderRejectIdentically() {
        TerrainSampler dry = (x, z) -> 70;
        var grid = grid(dry);
        assertNull(compare(cells(grid, 96, 96, 25), 60, dry, grid, 0));
        TerrainSampler wet = (x, z) -> 40;
        var wetGrid = grid(wet);
        assertNull(compare(cells(wetGrid, 6, 96, 25), 60, wet, wetGrid, 12));
    }

    @Test void shuffledTranslatedBasinsKeepExactMaskAndAccumulation() {
        var random = new Random(781623);
        int succeeded = 0;
        for (int i = 0; i < 20; i++) {
            int x = 65 + random.nextInt(60), z = 65 + random.nextInt(60);
            var sampler = bowl(x, z, 18 + random.nextInt(16));
            var grid = grid(sampler);
            var list = cells(grid, x, z, 29);
            Collections.shuffle(list, random);
            if (compare(list, 60, sampler, grid, 0) != null) succeeded++;
        }
        assertEquals(20, succeeded);
    }

    @Test void productionTracerDelegatesTheRealFineStage() throws Exception {
        // 直接走生产私有入口，防止只测迁出组件却漏掉正式接线。
        var sampler = bowl(96, 96, 25);
        var grid = grid(sampler);
        var stage = NetworkTracer.class.getDeclaredMethod("a", List.class, int.class, TerrainSampler.class,
                LowResGrid.class, PlateDomain.class, double.class, double[].class, TouchedCellMask.class);
        stage.setAccessible(true);
        Lake actual = (Lake) stage.invoke(null, cells(grid, 96, 96, 25), 60, sampler, grid, DOMAIN, 0,
                new double[2], new TouchedCellMask(grid.size()));
        assertNotNull(actual);
        assertTrue(actual.covers(96, 96));
    }

    static LowResGrid grid(TerrainSampler sampler) {
        return new LowResGrid(DOMAIN, sampler, StreamsConfig.defaults(12, 1, 192, 0, 0));
    }

    static TerrainSampler bowl(int cx, int cz, int radius) {
        return (x, z) -> (long)(x - cx) * (x - cx) + (long)(z - cz) * (z - cz) < radius * radius ? 48 : 75;
    }

    static ArrayList<Integer> cells(LowResGrid grid, int cx, int cz, int radius) {
        var result = new ArrayList<Integer>();
        for (int z = 0; z < grid.nz; z++) for (int x = 0; x < grid.nx; x++)
            if (Math.abs(grid.worldX(x) - cx) <= radius && Math.abs(grid.worldZ(z) - cz) <= radius)
                result.add(grid.idx(x, z));
        assertFalse(result.isEmpty());
        return result;
    }

    private static Lake compare(List<Integer> cells, int level, TerrainSampler sampler, LowResGrid grid, double margin) {
        double[] oldStats = { 7, 3 }, newStats = { 7, 3 };
        Lake expected = FrozenFineLakeRefiner.refine(cells, level, sampler, grid, DOMAIN, margin,
                oldStats, new boolean[grid.size()], new long[8]);
        Lake actual = FineLakeRefiner.refine(cells, level, sampler, grid, DOMAIN, margin,
                newStats, new TouchedCellMask(grid.size()), new long[8]);
        assertArrayEquals(oldStats, newStats);
        if (expected == null) assertNull(actual);
        else {
            assertNotNull(actual);
            assertEquals(expected.level(), actual.level()); assertEquals(expected.digDepth(), actual.digDepth());
            assertEquals(expected.spacing(), actual.spacing());
            assertEquals(expected.cellMinX(), actual.cellMinX()); assertEquals(expected.cellMinZ(), actual.cellMinZ());
            assertEquals(expected.cellsWide(), actual.cellsWide()); assertEquals(expected.cellsHigh(), actual.cellsHigh());
            assertArrayEquals(expected.mask(), actual.mask());
        }
        return actual;
    }
}
