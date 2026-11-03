package dev.streamsreflowing.core.river;

public record RiverSample() {
   private final double distance;
   private final double gradeY;
   private final double width;
   private final double slope;
   private final double dirX;
   private final double dirZ;
   private final boolean terminal;
   private final boolean source;
   private final double upstreamSlope;
   private final double distToOutlet;
   private final int segId;

   public RiverSample(
      double var1, double var3, double var5, double var7, double var9, double var11, boolean var13, boolean var14, double var15, double var17, int var19
   ) {
      this.distance = var1;
      this.gradeY = var3;
      this.width = var5;
      this.slope = var7;
      this.dirX = var9;
      this.dirZ = var11;
      this.terminal = var13;
      this.source = var14;
      this.upstreamSlope = var15;
      this.distToOutlet = var17;
      this.segId = var19;
   }
}
