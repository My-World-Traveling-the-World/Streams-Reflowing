package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.plate.PlateKey;
import java.util.List;

public final class RiverNetwork {
   public static final int NO_LAKE = Integer.MIN_VALUE;
   private final PlateKey a;
   private final List<RiverSegment> b;
   private final NearestRiverIndex c;
   private final List<Lake> d;

   public RiverNetwork(PlateKey var1, List<RiverSegment> var2, NearestRiverIndex var3, List<Lake> var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
   }

   public PlateKey key() {
      return this.a;
   }

   public List<RiverSegment> segments() {
      return this.b;
   }

   public int segmentCount() {
      return this.b.size();
   }

   public List<Lake> lakes() {
      return this.d;
   }

   public RiverSample sample(double var1, double var3, double var5) {
      return this.c.nearest(var1, var3, var5);
   }

   public RiverSample sampleTiered(double var1, double var3, double var5) {
      return this.c.nearestTiered(var1, var3, var5);
   }

   public RiverSample sampleCoveringForeign(double var1, double var3, double var5, int var7) {
      return this.c.nearestCoveringForeign(var1, var3, var5, var7);
   }

   public boolean corridorTouchesBox(double var1, double var3, double var5, double var7) {
      return this.c.anyCorridorTouchesBox(var1, var3, var5, var7);
   }

   public boolean inAnyChannel(double var1, double var3, double var5) {
      return this.c.inAnyChannel(var1, var3, var5);
   }

   public double lowerNeighborGrade(double var1, double var3, double var5, int var7, double var8, double var10) {
      return this.c.lowerNeighborGrade(var1, var3, var5, var7, var8, var10);
   }

   public Lake lakeAt(int var1, int var2) {
      for (Lake var4 : this.d) {
         if (var4.covers(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   public int lakeLevelAt(int var1, int var2) {
      Lake var3 = this.lakeAt(var1, var2);
      return var3 == null ? Integer.MIN_VALUE : var3.level();
   }

   public Lake lakeNear(int var1, int var2, int var3) {
      Lake var4 = null;

      for (Lake var6 : this.d) {
         if (var6.coversWithin(var1, var2, var3) && (var4 == null || var6.level() < var4.level())) {
            var4 = var6;
         }
      }

      return var4;
   }
}
