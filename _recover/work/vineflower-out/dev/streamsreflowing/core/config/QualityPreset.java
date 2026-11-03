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

   private QualityPreset(
      int var3, int var4, int var5, int var6, int var7, int var8, int var9, boolean var10, int var11, int var12, int var13, double var14, double var16
   ) {
      this.gridSpacing = var3;
      this.plateScale = var4;
      this.terrainSampleSpacing = var5;
      this.valleySnapRadius = var6;
      this.prefetchRadius = var7;
      this.vegetation = var8;
      this.allWaterFlora = var9;
      this.clearFloatingSnow = var10;
      this.allWaterBanks = var11;
      this.underwaterFlora = var12;
      this.terrainSamplingMode = var13;
      this.gradeWindowAbove = var14;
      this.gradeWindowBelow = var16;
   }
}
