/* 文件职责：在河网洪泛前标记海洋、原版河口与高处河道，保留旧扫描顺序。 */
package dev.streamsreflowing.core.river;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.ArrayDeque;

final class PlateOutletSeeds {
    private PlateOutletSeeds() {}

    static void mark(PlateKey key, LowResGrid grid, TerrainSampler terrain, int seaLevel, int[] dx, int[] dz) {
        var seen = new boolean[grid.size()];
        var pending = new ArrayDeque<Integer>();
        for (int cell = 0; cell < grid.size(); cell++) {
            if (!grid.inDomain(cell) || grid.height(cell) == -1000.0) continue;
            int type = terrain.waterBiomeClass(Math.round(grid.worldX(grid.gi(cell))), Math.round(grid.worldZ(grid.gj(cell))));
            if (type == TerrainSampler.CLASS_RIVER) {
                markRiver(grid, pending, cell);
                seen[cell] = true;
            } else if (type == TerrainSampler.CLASS_OCEAN) {
                grid.overrideHeight(cell, -1000.0);
                seen[cell] = true;
            }
        }
        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            int outlets = 0;
            for (int cell = 0; cell < grid.size(); cell++) {
                if (grid.riverOutlet(cell)) outlets++;
            }
            Constants.LOG.info("[SR-RTF] plate {}: {} river-outlet cells after tier 1", key, outlets);
        }
        if (Boolean.getBoolean("streamsreflowing.nohighrivers")) return;
        // 高处洼槽按原顺序入队；前面刚被改为出口的格子会影响后续邻居判定。
        for (int cell = 0; cell < grid.size(); cell++) {
            if (seen[cell] || !grid.inDomain(cell) || grid.height(cell) <= seaLevel + 8) continue;
            int i = grid.gi(cell), j = grid.gj(cell), higher = 0;
            for (int d = 0; d < dx.length; d++) {
                int x = i + dx[d], z = j + dz[d];
                if (!grid.valid(x, z)) continue;
                int neighbor = grid.idx(x, z);
                if (grid.inDomain(neighbor) && grid.height(neighbor) > grid.height(cell)) higher++;
            }
            if (higher < 5) continue;
            seen[cell] = true;
            if (isRiverAtHeight(grid, terrain, cell, i, j)) markRiver(grid, pending, cell);
        }
        int extra = 0;
        while (!pending.isEmpty()) {
            int cell = pending.removeFirst();
            int i = grid.gi(cell), j = grid.gj(cell);
            for (int d = 0; d < dx.length; d++) {
                int x = i + dx[d], z = j + dz[d];
                if (!grid.valid(x, z)) continue;
                int neighbor = grid.idx(x, z);
                if (seen[neighbor] || !grid.inDomain(neighbor) || grid.height(neighbor) == -1000.0) continue;
                // 负结果也只查一次；保留海面探测与真实海拔探测两种口径。
                seen[neighbor] = true;
                if (!isRiverAtHeight(grid, terrain, neighbor, x, z)) continue;
                markRiver(grid, pending, neighbor);
                extra++;
            }
        }
        if (extra > 0) {
            Constants.LOG.debug("[SR-MERGE] high-river outlets: {} extra river cells found above the sea-level probe", extra);
        }
    }

    private static boolean isRiverAtHeight(LowResGrid grid, TerrainSampler terrain, int cell, int i, int j) {
        return terrain.waterBiomeClassAt(Math.round(grid.worldX(i)), (int) Math.round(grid.height(cell)),
                Math.round(grid.worldZ(j))) == TerrainSampler.CLASS_RIVER;
    }

    private static void markRiver(LowResGrid grid, ArrayDeque<Integer> pending, int cell) {
        grid.overrideHeight(cell, -1001.0);
        grid.markRiverOutlet(cell);
        pending.addLast(cell);
    }
}
