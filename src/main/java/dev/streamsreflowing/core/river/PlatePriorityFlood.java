/* 文件职责：计算整板填洼高度与下游索引；保留队列及邻居顺序，避免重构移动河口。 */
package dev.streamsreflowing.core.river;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

final class PlatePriorityFlood {
    private PlatePriorityFlood() {}

    static double[] fill(PlateKey key, LowResGrid grid, TerrainSampler terrain, List<Integer> cells,
                         int seaLevel, int[] dx, int[] dz) {
        boolean primitive = Boolean.parseBoolean(System.getProperty("streamsreflowing.doubleminheap", "true"));
        return fill(key, grid, terrain, cells, seaLevel, dx, dz, primitive);
    }

    static double[] fill(PlateKey key, LowResGrid grid, TerrainSampler terrain, List<Integer> cells,
                         int seaLevel, int[] dx, int[] dz, boolean primitive) {
        double[] height = new double[grid.size()];
        boolean[] seen = new boolean[grid.size()];
        var primitiveQueue = primitive ? new DoubleMinHeap() : null;
        PriorityQueue<Integer> boxedQueue = primitive ? null
                : new PriorityQueue<>((first, second) -> Double.compare(height[first], height[second]));
        boolean[] riverBanks = seedBanks(key, grid, terrain, cells, dx, dz);

        for (int cell : cells) {
            height[cell] = grid.height(cell);
            if (grid.riverOutlet(cell)) {
                seen[cell] = true;
                // 河中格不会被填埋；只让邻接陆地的出口进入队列。
                if (!riverBanks[cell]) continue;
            } else {
                if (!(height[cell] <= seaLevel) && !onPlateEdge(grid, cell, dx, dz)) continue;
                seen[cell] = true;
            }
            if (primitive) primitiveQueue.add(height[cell], cell); else boxedQueue.add(cell);
        }
        while (primitive ? !primitiveQueue.isEmpty() : !boxedQueue.isEmpty()) {
            int cell = primitive ? primitiveQueue.value() : boxedQueue.poll();
            if (primitive) primitiveQueue.pop();
            int i = grid.gi(cell), j = grid.gj(cell);
            for (int k = 0; k < dx.length; k++) {
                int next = neighbor(grid, i + dx[k], j + dz[k]);
                if (next < 0 || seen[next]) continue;
                // 最小高差给平地可追踪的落坡；首次入堆锁定高度并保留等权顺序。
                height[next] = Math.max(grid.height(next), height[cell] + 0.001);
                seen[next] = true;
                if (primitive) primitiveQueue.add(height[next], next); else boxedQueue.add(next);
            }
        }
        return height;
    }

    private static boolean[] seedBanks(PlateKey key, LowResGrid grid, TerrainSampler terrain, List<Integer> cells,
                                       int[] dx, int[] dz) {
        var banks = new ArrayList<Integer>();
        boolean[] outlets = new boolean[grid.size()];
        for (int cell : cells) {
            if (!grid.riverOutlet(cell)) continue;
            int i = grid.gi(cell), j = grid.gj(cell);
            for (int k = 0; k < dx.length; k++) {
                int next = neighbor(grid, i + dx[k], j + dz[k]);
                if (next < 0 || grid.riverOutlet(next)) continue;
                outlets[cell] = true;
                banks.add(cell);
                break;
            }
        }
        if (!banks.isEmpty()) {
            // 并行只读采样，统一按扫描顺序写回，避免边界条件随并发完成顺序改变。
            double[] heights = new double[banks.size()];
            LowResGrid.parallelFor(banks.size(), i -> {
                int cell = banks.get(i);
                int x = Math.round(grid.worldX(grid.gi(cell))), z = Math.round(grid.worldZ(grid.gj(cell)));
                double water = terrain.riverSurface(x, z);
                heights[i] = Double.isNaN(water) ? terrain.surfaceHeight(x, z) : water;
            });
            for (int i = 0; i < banks.size(); i++) grid.overrideHeight(banks.get(i), heights[i]);
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-RTF] plate {}: {} river bank cells seeded", key, banks.size());
            }
        }
        return outlets;
    }

    static int[] downstream(LowResGrid grid, List<Integer> cells, double[] height, int seaLevel, int[] dx, int[] dz) {
        int[] flowTo = new int[grid.size()];
        for (int cell : cells) {
            int i = grid.gi(cell), j = grid.gj(cell);
            int target = -1;
            double best = 0.0;
            for (int k = 0; k < dx.length; k++) {
                int next = neighbor(grid, i + dx[k], j + dz[k]);
                if (next < 0) continue;
                // 原版出口优先，其余等坡保留遇到的第一格，不能按索引重排。
                if (grid.riverOutlet(next)) {
                    target = next;
                    break;
                }
                double drop = height[cell] - height[next];
                if (drop <= 0.0 || height[cell] <= seaLevel - 2.0 && drop < 2.0) continue;
                double slope = drop / (k % 2 == 0 ? 1.0 : 1.4142135623730951);
                if (slope > best) {
                    best = slope;
                    target = next;
                }
            }
            flowTo[cell] = target;
        }
        // 下一阶段由高向低传递汇流量；相同高度的索引顺序也属于产物口径。
        cells.sort((first, second) -> {
            int order = Double.compare(height[second], height[first]);
            return order != 0 ? order : Integer.compare(first, second);
        });
        return flowTo;
    }

    private static boolean onPlateEdge(LowResGrid grid, int cell, int[] dx, int[] dz) {
        int i = grid.gi(cell), j = grid.gj(cell);
        for (int k = 0; k < dx.length; k++) if (neighbor(grid, i + dx[k], j + dz[k]) < 0) return true;
        return false;
    }

    private static int neighbor(LowResGrid grid, int i, int j) {
        if (!grid.valid(i, j)) return -1;
        int next = grid.idx(i, j);
        return grid.inDomain(next) ? next : -1;
    }
}
