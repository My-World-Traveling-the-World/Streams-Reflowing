package dev.streamsreflowing.core.plate;

public record PlateKey() {
   private final int cx;
   private final int cz;

   public PlateKey(int var1, int var2) {
      this.cx = var1;
      this.cz = var2;
   }
}
