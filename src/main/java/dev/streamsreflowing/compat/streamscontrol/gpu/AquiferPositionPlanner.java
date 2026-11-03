/* 鏂囦欢鑱岃矗锛氭寜鍘熺増 1.21.1 aquifer 缃戞牸鐢熸垚 GPU 鎵规鎵€闇€鐨勭‘瀹氭€у€欓€変綅缃€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * aquifer 鍊欓€夊潗鏍囪緭鍏ヨ鍒掑櫒銆? */
final class AquiferPositionPlanner {
    private static final int GRID_XZ_SIZE = 3;
    private static final int[] SURFACE_OFFSETS = {
            0, 0,
            -2, -1,
            -1, -1,
            0, -1,
            1, -1,
            -3, 0,
            -2, 0,
            -1, 0,
            1, 0,
            -2, 1,
            -1, 1,
            0, 1,
            1, 1
    };

    private AquiferPositionPlanner() {}

    static Plan plan(List<Long> chunkKeys,
                     int[] columnReferences,
                     PositionalRandomFactory randomFactory,
                     int minY,
                     int height) {
        int minGridY = Math.floorDiv(minY, 12) - 1;
        int maxGridY = Math.floorDiv(minY + height, 12) + 1;
        int gridYSize = maxGridY - minGridY + 1;
        int pointsPerChunk = GRID_XZ_SIZE * gridYSize * GRID_XZ_SIZE;
        int[] chunkCoordinates = new int[Math.multiplyExact(chunkKeys.size(), 2)];
        int[] chunkPointIndices = new int[Math.multiplyExact(chunkKeys.size(), pointsPerChunk)];
        Arrays.fill(chunkPointIndices, -1);
        boolean[] requiredHorizontal = requiredHorizontalPoints(chunkKeys.size(), columnReferences);
        Map<GridPoint, Integer> uniqueIndices = new LinkedHashMap<>();
        List<GridPoint> uniquePoints = new ArrayList<>();

        for (int chunkIndex = 0; chunkIndex < chunkKeys.size(); chunkIndex++) {
            long key = chunkKeys.get(chunkIndex);
            int chunkX = ChunkPos.getX(key);
            int chunkZ = ChunkPos.getZ(key);
            chunkCoordinates[chunkIndex * 2] = chunkX;
            chunkCoordinates[chunkIndex * 2 + 1] = chunkZ;
            int minGridX = chunkX - 1;
            int minGridZ = chunkZ - 1;
            for (int gridYIndex = 0; gridYIndex < gridYSize; gridYIndex++) {
                int gridY = minGridY + gridYIndex;
                for (int gridZIndex = 0; gridZIndex < GRID_XZ_SIZE; gridZIndex++) {
                    int gridZ = minGridZ + gridZIndex;
                    for (int gridXIndex = 0; gridXIndex < GRID_XZ_SIZE; gridXIndex++) {
                        if (!requiredHorizontal[horizontalIndex(chunkIndex, gridXIndex, gridZIndex)]) {
                            continue;
                        }
                        int gridX = minGridX + gridXIndex;
                        int point = ((gridYIndex * GRID_XZ_SIZE + gridZIndex) * GRID_XZ_SIZE + gridXIndex);
                        GridPoint gridPoint = new GridPoint(gridX, gridY, gridZ);
                        int uniqueIndex = uniqueIndices.computeIfAbsent(gridPoint, ignored -> {
                            int next = uniquePoints.size();
                            uniquePoints.add(gridPoint);
                            return next;
                        });
                        chunkPointIndices[chunkIndex * pointsPerChunk + point] = uniqueIndex;
                    }
                }
            }
        }

        verifyRequiredCoverage(chunkPointIndices, columnReferences, gridYSize, pointsPerChunk);
        for (int index = 0; index < chunkPointIndices.length; index++) {
            if (chunkPointIndices[index] < 0) {
                chunkPointIndices[index] = 0;
            }
        }

        int[] positions = new int[Math.multiplyExact(uniquePoints.size(), 3)];
        for (int index = 0; index < uniquePoints.size(); index++) {
            GridPoint point = uniquePoints.get(index);
            RandomSource random = randomFactory.at(point.x(), point.y(), point.z());
            int offset = index * 3;
            positions[offset] = point.x() * 16 + random.nextInt(10);
            positions[offset + 1] = point.y() * 12 + random.nextInt(9);
            positions[offset + 2] = point.z() * 16 + random.nextInt(10);
        }

        int[] pointPreliminaryIndices = new int[Math.multiplyExact(uniquePoints.size(), 13)];
        Map<SurfacePoint, Integer> preliminaryIndices = new LinkedHashMap<>();
        List<SurfacePoint> preliminaryPoints = new ArrayList<>();
        for (int pointIndex = 0; pointIndex < uniquePoints.size(); pointIndex++) {
            int positionOffset = pointIndex * 3;
            int x = positions[positionOffset];
            int z = positions[positionOffset + 2];
            for (int offsetIndex = 0; offsetIndex < 13; offsetIndex++) {
                int sampleX = alignToQuart(x + SURFACE_OFFSETS[offsetIndex * 2] * 16);
                int sampleZ = alignToQuart(z + SURFACE_OFFSETS[offsetIndex * 2 + 1] * 16);
                SurfacePoint surfacePoint = new SurfacePoint(sampleX, sampleZ);
                int preliminaryIndex = preliminaryIndices.computeIfAbsent(surfacePoint, ignored -> {
                    int next = preliminaryPoints.size();
                    preliminaryPoints.add(surfacePoint);
                    return next;
                });
                pointPreliminaryIndices[pointIndex * 13 + offsetIndex] = preliminaryIndex;
            }
        }
        int[] preliminaryCoordinates = new int[Math.multiplyExact(preliminaryPoints.size(), 2)];
        for (int index = 0; index < preliminaryPoints.size(); index++) {
            SurfacePoint point = preliminaryPoints.get(index);
            preliminaryCoordinates[index * 2] = point.x();
            preliminaryCoordinates[index * 2 + 1] = point.z();
        }
        return new Plan(
                chunkCoordinates,
                positions,
                chunkPointIndices,
                preliminaryCoordinates,
                pointPreliminaryIndices,
                minGridY,
                gridYSize,
                pointsPerChunk,
                uniquePoints.size(),
                preliminaryPoints.size());
    }

    private static boolean[] requiredHorizontalPoints(int chunkCount, int[] columnReferences) {
        if (columnReferences == null || columnReferences.length == 0
                || columnReferences.length % 3 != 0) {
            throw new IllegalArgumentException("invalid aquifer column references");
        }
        boolean[] required = new boolean[Math.multiplyExact(chunkCount, 9)];
        for (int offset = 0; offset < columnReferences.length; offset += 3) {
            int chunkIndex = columnReferences[offset];
            int localX = columnReferences[offset + 1];
            int localZ = columnReferences[offset + 2];
            if (chunkIndex < 0 || chunkIndex >= chunkCount
                    || localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16) {
                throw new IllegalArgumentException("aquifer column reference is outside its chunk");
            }
            int firstX = Math.floorDiv(localX - 5, 16) + 1;
            int firstZ = Math.floorDiv(localZ - 5, 16) + 1;
            for (int zOffset = 0; zOffset <= 1; zOffset++) {
                for (int xOffset = 0; xOffset <= 1; xOffset++) {
                    required[horizontalIndex(chunkIndex, firstX + xOffset, firstZ + zOffset)] = true;
                }
            }
        }
        return required;
    }

    private static void verifyRequiredCoverage(int[] pointIndices,
                                               int[] columnReferences,
                                               int gridYSize,
                                               int pointsPerChunk) {
        for (int offset = 0; offset < columnReferences.length; offset += 3) {
            int chunkIndex = columnReferences[offset];
            int firstX = Math.floorDiv(columnReferences[offset + 1] - 5, 16) + 1;
            int firstZ = Math.floorDiv(columnReferences[offset + 2] - 5, 16) + 1;
            for (int gridY = 0; gridY < gridYSize; gridY++) {
                for (int zOffset = 0; zOffset <= 1; zOffset++) {
                    for (int xOffset = 0; xOffset <= 1; xOffset++) {
                        int point = (gridY * 3 + firstZ + zOffset) * 3 + firstX + xOffset;
                        if (pointIndices[chunkIndex * pointsPerChunk + point] < 0) {
                            throw new IllegalStateException("sparse aquifer planner omitted a required grid point");
                        }
                    }
                }
            }
        }
    }

    private static int horizontalIndex(int chunkIndex, int xIndex, int zIndex) {
        if (xIndex < 0 || xIndex >= GRID_XZ_SIZE || zIndex < 0 || zIndex >= GRID_XZ_SIZE) {
            throw new IllegalArgumentException("aquifer horizontal grid index is outside 3x3 neighborhood");
        }
        return chunkIndex * 9 + zIndex * 3 + xIndex;
    }

    record Plan(int[] chunkCoordinates,
                int[] positions,
                int[] chunkPointIndices,
                int[] preliminaryCoordinates,
                int[] pointPreliminaryIndices,
                int minGridY,
                int gridYSize,
                int pointsPerChunk,
                int uniquePointCount,
                int preliminaryPointCount) {}

    private static int alignToQuart(int value) {
        return Math.floorDiv(value, 4) * 4;
    }

    private record GridPoint(int x, int y, int z) {}

    private record SurfacePoint(int x, int z) {}
}

