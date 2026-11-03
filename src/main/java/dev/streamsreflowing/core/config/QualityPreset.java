/*
 * Decompiled with CFR 0.152.
 * Repaired: CFR emitted this as `final class extends Enum<QualityPreset>` with
 * synthetic values()/valueOf()/static-array; converted back to a real enum.
 */
package dev.streamsreflowing.core.config;

public enum QualityPreset {
    POTATO(24, 512, 48, 0, 1, 0, 0, false, 1, 2, 0, 0.0, 0.0),
    LOW(20, 768, 40, 8, 1, 1, 1, false, 1, 2, 1, 2.0, 6.0),
    MEDIUM(16, 768, 32, 8, 1, 1, 1, true, 1, 3, 1, 2.0, 6.0),
    HIGH(12, 1024, 24, 8, 1, 1, 1, true, 1, 3, 1, 1.0, 5.0),
    EXTREME(8, 1536, 16, 4, 1, 1, 1, true, 1, 3, 1, 1.0, 3.0),
    MAX(6, 2048, 0, 2, 1, 1, 1, true, 1, 3, 2, 0.0, 0.0);

    public final int gridSpacing;
    public final int plateScale;
    public final int terrainSampleSpacing;
    public final int valleySnapRadius;
    public final int prefetchRadius;
    public final int vegetation;
    public final int allWaterFlora;
    public final boolean clearFloatingSnow;
    public final int allWaterBanks;
    public final int underwaterFlora;
    public final int terrainSamplingMode;
    public final double gradeWindowAbove;
    public final double gradeWindowBelow;

    QualityPreset(int gridSpacing,
                  int plateScale,
                  int terrainSampleSpacing,
                  int valleySnapRadius,
                  int prefetchRadius,
                  int vegetation,
                  int allWaterFlora,
                  boolean clearFloatingSnow,
                  int allWaterBanks,
                  int underwaterFlora,
                  int terrainSamplingMode,
                  double gradeWindowAbove,
                  double gradeWindowBelow) {
        this.gridSpacing = gridSpacing;
        this.plateScale = plateScale;
        this.terrainSampleSpacing = terrainSampleSpacing;
        this.valleySnapRadius = valleySnapRadius;
        this.prefetchRadius = prefetchRadius;
        this.vegetation = vegetation;
        this.allWaterFlora = allWaterFlora;
        this.clearFloatingSnow = clearFloatingSnow;
        this.allWaterBanks = allWaterBanks;
        this.underwaterFlora = underwaterFlora;
        this.terrainSamplingMode = terrainSamplingMode;
        this.gradeWindowAbove = gradeWindowAbove;
        this.gradeWindowBelow = gradeWindowBelow;
    }
}
