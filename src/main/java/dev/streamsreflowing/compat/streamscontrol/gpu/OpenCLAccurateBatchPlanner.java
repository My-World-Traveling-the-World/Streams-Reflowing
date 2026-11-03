/* 鏂囦欢鑱岃矗锛氭牴鎹簿閲囩▼搴忚妯′笌璁惧鏄惧瓨闄愬埗纭畾瀹夊叏鐨?chunk 瀛愭壒澶у皬銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import net.minecraft.world.level.ChunkPos;
import dev.streamsreflowing.compat.streamscontrol.gpu.bridge.OpenCLBridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 绮鹃噰鏍?GPU 鎵规鍐呭瓨瑙勫垝鍣ㄣ€? */
public final class OpenCLAccurateBatchPlanner {
    private static final long MIN_DEVICE_BUDGET = 16L * 1024L * 1024L;
    private static final int WATCHDOG_SAFE_MAX_CHUNKS = 128;

    private OpenCLAccurateBatchPlanner() {}

    public static int maxChunks(OpenCLBridge.DeviceInfo device,
                                int nodeCount,
                                int interpolatorCount,
                                int latticePointsPerChunk,
                                int aquiferPointsPerChunk) {
        long scratchPerChunk = Math.multiplyExact(
                (long) Math.max(latticePointsPerChunk, Math.max(aquiferPointsPerChunk, 256)),
                Math.multiplyExact((long) Math.max(1, nodeCount), Double.BYTES));
        long latticePerChunk = Math.multiplyExact(
                Math.multiplyExact((long) Math.max(1, interpolatorCount), (long) latticePointsPerChunk),
                Double.BYTES);
        long aquiferPerChunk = Math.multiplyExact((long) Math.max(1, aquiferPointsPerChunk), 5L * Integer.BYTES);
        long outputPerChunk = 3L * 256L * Integer.BYTES;
        long totalPerChunk = Math.addExact(Math.addExact(scratchPerChunk, latticePerChunk),
                Math.addExact(aquiferPerChunk, outputPerChunk));

        long maxAllocation = positiveOr(device.maxAllocationBytes(), 128L * 1024L * 1024L);
        long globalBudget = Math.max(MIN_DEVICE_BUDGET,
                positiveOr(device.globalMemoryBytes(), 512L * 1024L * 1024L) / 4L);
        long byScratch = maxAllocation / Math.max(1L, scratchPerChunk);
        long byLattice = maxAllocation / Math.max(1L, latticePerChunk);
        long byTotal = globalBudget / Math.max(1L, totalPerChunk);
        long result = Math.min(byScratch, Math.min(byLattice, byTotal));
        if (result <= 0L) {
            return 0;
        }
        // 淇濇寔鍦ㄦ闈?GPU 鐪嬮棬鐙楀拰澶у垎閰嶆嫄鐐瑰唴锛涙暣涓尯鍩熶粛鐢变竴娆￠珮浼樺厛绾ц澶囦换鍔¤繛缁彁浜ゃ€?        return (int) Math.min(WATCHDOG_SAFE_MAX_CHUNKS, result);
        return (int) Math.min(WATCHDOG_SAFE_MAX_CHUNKS, result);
    }

    static List<List<Long>> spatialBatches(List<Long> chunkKeys, int maxChunks) {
        if (maxChunks <= 0) {
            throw new IllegalArgumentException("maxChunks must be positive");
        }
        if (chunkKeys.isEmpty()) {
            return List.of();
        }

        int tileSize = Math.max(1, (int) Math.floor(Math.sqrt(maxChunks)));
        Map<Tile, List<Long>> chunksByTile = new LinkedHashMap<>();
        for (long chunkKey : chunkKeys) {
            int tileX = Math.floorDiv(ChunkPos.getX(chunkKey), tileSize);
            int tileZ = Math.floorDiv(ChunkPos.getZ(chunkKey), tileSize);
            chunksByTile.computeIfAbsent(new Tile(tileX, tileZ), ignored -> new ArrayList<>()).add(chunkKey);
        }

        List<List<Long>> batches = new ArrayList<>(
                Math.max(1, Math.floorDiv(chunkKeys.size() + maxChunks - 1, maxChunks)));
        List<Long> current = new ArrayList<>(maxChunks);
        for (List<Long> tileChunks : chunksByTile.values()) {
            for (long chunkKey : tileChunks) {
                if (current.size() == maxChunks) {
                    batches.add(List.copyOf(current));
                    current.clear();
                }
                current.add(chunkKey);
            }
        }
        if (!current.isEmpty()) {
            batches.add(List.copyOf(current));
        }
        return List.copyOf(batches);
    }

    private static long positiveOr(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    private record Tile(int x, int z) {}
}

