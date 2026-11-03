package dev.streamsreflowing.core.compose;

public record ColumnPlan() {
   private final boolean inChannel;
   private final boolean inCorridor;
   private final int waterSurfaceY;
   private final int bedY;
   private final double bankBlend;
   private final double distance;
   private final double width;
   private final double gradeY;
   private final double slope;
   private final double flowDirX;
   private final double flowDirZ;
   private final boolean terminal;
   private final boolean source;
   private final double upstreamSlope;
   private final double fluidFlowDirX;
   private final double fluidFlowDirZ;
   private final double distToOutlet;
   public static final ColumnPlan NONE = new ColumnPlan(
      false, false, 0, 0, 1.0, Double.MAX_VALUE, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 0.0, 0.0, 0.0, Double.MAX_VALUE
   );

   public ColumnPlan(
      boolean var1,
      boolean var2,
      int var3,
      int var4,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      boolean var19,
      boolean var20,
      double var21,
      double var23,
      double var25,
      double var27
   ) {
      this.inChannel = var1;
      this.inCorridor = var2;
      this.waterSurfaceY = var3;
      this.bedY = var4;
      this.bankBlend = var5;
      this.distance = var7;
      this.width = var9;
      this.gradeY = var11;
      this.slope = var13;
      this.flowDirX = var15;
      this.flowDirZ = var17;
      this.terminal = var19;
      this.source = var20;
      this.upstreamSlope = var21;
      this.fluidFlowDirX = var23;
      this.fluidFlowDirZ = var25;
      this.distToOutlet = var27;
   }
}
