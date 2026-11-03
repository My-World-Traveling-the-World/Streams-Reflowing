/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.config;

public final class QualityPreset
extends Enum<QualityPreset> {
    public static final /* enum */ QualityPreset POTATO = new QualityPreset(24, 512, 48, 0, 1, 0, 0, false, 1, 2, 0, 0.0, 0.0);
    public static final /* enum */ QualityPreset LOW = new QualityPreset(20, 768, 40, 8, 1, 1, 1, false, 1, 2, 1, 2.0, 6.0);
    public static final /* enum */ QualityPreset MEDIUM = new QualityPreset(16, 768, 32, 8, 1, 1, 1, true, 1, 3, 1, 2.0, 6.0);
    public static final /* enum */ QualityPreset HIGH = new QualityPreset(12, 1024, 24, 8, 1, 1, 1, true, 1, 3, 1, 1.0, 5.0);
    public static final /* enum */ QualityPreset EXTREME = new QualityPreset(8, 1536, 16, 4, 1, 1, 1, true, 1, 3, 1, 1.0, 3.0);
    public static final /* enum */ QualityPreset MAX = new QualityPreset(6, 2048, 0, 2, 1, 1, 1, true, 1, 3, 2, 0.0, 0.0);
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
    private static final /* synthetic */ QualityPreset[] a;

    public static QualityPreset[] values() {
        return (QualityPreset[])a.clone();
    }

    public static QualityPreset valueOf(String string) {
        return Enum.valueOf(QualityPreset.class, string);
    }

    private QualityPreset(int n2, int n3, int n4, int n5, int n6, int n7, int n8, boolean bl, int n9, int n10, int n11, double d, double d2) {
        this.gridSpacing = n2;
        this.plateScale = n3;
        this.terrainSampleSpacing = n4;
        this.valleySnapRadius = n5;
        this.prefetchRadius = n6;
        this.vegetation = n7;
        this.allWaterFlora = n8;
        this.clearFloatingSnow = bl;
        this.allWaterBanks = n9;
        this.underwaterFlora = n10;
        this.terrainSamplingMode = n11;
        this.gradeWindowAbove = d;
        this.gradeWindowBelow = d2;
    }

    private static /* synthetic */ QualityPreset[] a() {
        return new QualityPreset[]{POTATO, LOW, MEDIUM, HIGH, EXTREME, MAX};
    }

    static {
        a = QualityPreset.a();
    }
}

