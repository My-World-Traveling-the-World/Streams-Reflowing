package dev.streamsreflowing.core.river;

public record RiverSegment() {
   private final double x1;
   private final double z1;
   private final double y1;
   private final double w1;
   private final double x2;
   private final double z2;
   private final double y2;
   private final double w2;

   public RiverSegment(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15) {
      this.x1 = var1;
      this.z1 = var3;
      this.y1 = var5;
      this.w1 = var7;
      this.x2 = var9;
      this.z2 = var11;
      this.y2 = var13;
      this.w2 = var15;
   }

   public double clampParam(double var1, double var3) {
      double var5 = this.x2 - this.x1;
      double var7 = this.z2 - this.z1;
      double var9 = var5 * var5 + var7 * var7;
      if (var9 <= 1.0E-9) {
         return 0.0;
      } else {
         double var11 = ((var1 - this.x1) * var5 + (var3 - this.z1) * var7) / var9;
         return var11 < 0.0 ? 0.0 : (var11 > 1.0 ? 1.0 : var11);
      }
   }

   public double distanceAt(double var1, double var3, double var5) {
      double var7 = this.x1 + (this.x2 - this.x1) * var5;
      double var9 = this.z1 + (this.z2 - this.z1) * var5;
      double var11 = var1 - var7;
      double var13 = var3 - var9;
      return Math.sqrt(var11 * var11 + var13 * var13);
   }

   public double gradeAt(double var1) {
      return this.y1 + (this.y2 - this.y1) * var1;
   }

   public double widthAt(double var1) {
      return this.w1 + (this.w2 - this.w1) * var1;
   }

   public double minX() {
      return Math.min(this.x1, this.x2);
   }

   public double maxX() {
      return Math.max(this.x1, this.x2);
   }

   public double minZ() {
      return Math.min(this.z1, this.z2);
   }

   public double maxZ() {
      return Math.max(this.z1, this.z2);
   }
}
