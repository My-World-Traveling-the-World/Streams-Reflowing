package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 同一生产掩码连续借用，断言拒绝/异常后的真实位值，避免只测计数。 */
class TouchedCellMaskTest {
    @Test void duplicatesGrowthAndCloseLeaveNoPreviousCells() {
        var mask = new TouchedCellMask(100000);
        var indices = java.util.stream.IntStream.range(0, 600).boxed().toList();
        boolean[] values;
        try (var lease = mask.prepare(indices)) {
            values = lease.cells();
            for (int i = 0; i < values.length; i++) assertEquals(i < 600, values[i]);
            assertThrows(IllegalStateException.class, () -> mask.prepare(List.of(9999)));
        }
        mask.close();
        for (boolean value : values) assertFalse(value);
        try (var lease = mask.prepare(List.of(9999, 9999, 3))) {
            for (int i = 0; i < values.length; i++) assertEquals(i == 9999 || i == 3, lease.cells()[i]);
        }
        for (boolean value : values) assertFalse(value);
        assertThrows(IllegalStateException.class, mask::cells);
    }

    @Test void partialPrepareFailureCleansAndAllowsReuse() {
        var mask = new TouchedCellMask(1024);
        boolean[] values;
        try (var lease = mask.prepare(List.of(0))) { values = lease.cells(); }
        assertThrows(IndexOutOfBoundsException.class, () -> mask.prepare(List.of(3, 5, 1024)));
        for (boolean value : values) assertFalse(value);
        try (var lease = mask.prepare(List.of(7))) {
            assertTrue(lease.cells()[7]);
            assertFalse(lease.cells()[3]);
            assertFalse(lease.cells()[5]);
        }
    }

    @Test void realRefinerReturnsMaskOnDryRejectionAndSamplingException() {
        TerrainSampler dry = (x, z) -> 75;
        var grid = FineLakeRefinerTest.grid(dry);
        var mask = new TouchedCellMask(grid.size());
        var cells = FineLakeRefinerTest.cells(grid, 96, 96, 25);
        var domain = new PlateDomain(new PlateLattice(17, 192, 0), new PlateKey(0, 0));
        boolean[] values;
        try (var lease = mask.prepare(List.of(0))) { values = lease.cells(); }
        assertNull(FineLakeRefiner.refine(cells, 60, dry, grid, domain, 0, null, mask, new long[8]));
        for (boolean value : values) assertFalse(value);
        TerrainSampler broken = (x, z) -> { throw new IllegalStateException("采样故障"); };
        assertThrows(IllegalStateException.class,
                () -> FineLakeRefiner.refine(cells, 60, broken, grid, domain, 0, null, mask, new long[8]));
        for (boolean value : values) assertFalse(value);
        var wet = FineLakeRefinerTest.bowl(96, 96, 25);
        var wetGrid = FineLakeRefinerTest.grid(wet);
        Lake lake = FineLakeRefiner.refine(cells, 60, wet, wetGrid, domain, 0, null, mask, new long[8]);
        assertNotNull(lake);
        assertTrue(lake.covers(96, 96));
        for (boolean value : values) assertFalse(value);
    }

    @Test void zeroSizeAndEmptyMembershipRemainReusable() {
        var mask = new TouchedCellMask(0);
        try (var lease = mask.prepare(List.of())) { assertEquals(0, lease.cells().length); }
        assertThrows(IndexOutOfBoundsException.class, () -> mask.prepare(List.of(0)));
        try (var lease = mask.prepare(List.of())) { assertEquals(0, lease.cells().length); }
    }

    @Test void densePartialFailureAndSwitchToSparseAreClean() {
        var mask = new TouchedCellMask(256);
        boolean[] values;
        try (var lease = mask.prepare(List.of(0))) { values = lease.cells(); }
        assertThrows(IndexOutOfBoundsException.class, () -> mask.prepare(List.of(1, 2, 3, 4, 256)));
        for (boolean value : values) assertFalse(value);
        try (var lease = mask.prepare(List.of(20))) {
            for (int i = 0; i < values.length; i++) assertEquals(i == 20, lease.cells()[i]);
        }
        for (boolean value : values) assertFalse(value);
    }
}
