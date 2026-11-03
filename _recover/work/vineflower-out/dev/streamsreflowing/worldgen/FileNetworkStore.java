package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class FileNetworkStore implements NetworkStore {
   private static final int a = 1397902897;
   private static final int b = 28;
   private static final int c = 20000000;
   private static final int d = 1000000;
   private static final int e = 4000000;
   private final Path f;
   private final StreamsConfig g;
   private final long h;
   private volatile boolean i;

   public FileNetworkStore(Path var1, long var2, StreamsConfig var4) {
      this.f = var1;
      this.g = var4;
      this.h = a(var2, var4);
   }

   public long fingerprint() {
      return this.h;
   }

   @Override
   public RiverNetwork load(PlateKey var1) {
      Path var2 = this.f.resolve(a(var1));
      if (!Files.isRegularFile(var2)) {
         return null;
      } else {
         try {
            RiverNetwork var27;
            try (DataInputStream var3 = new DataInputStream(new BufferedInputStream(Files.newInputStream(var2)))) {
               if (var3.readInt() != 1397902897 || var3.readInt() != 28 || var3.readLong() != this.h) {
                  return null;
               }

               int var4 = var3.readInt();
               if (var4 < 0 || var4 > 20000000) {
                  return null;
               }

               ArrayList var5 = new ArrayList(var4);

               for (int var6 = 0; var6 < var4; var6++) {
                  var5.add(
                     new RiverSegment(
                        var3.readDouble(),
                        var3.readDouble(),
                        var3.readDouble(),
                        var3.readDouble(),
                        var3.readDouble(),
                        var3.readDouble(),
                        var3.readDouble(),
                        var3.readDouble()
                     )
                  );
               }

               int var24 = var3.readInt();
               if (var24 < 0 || var24 > 1000000) {
                  return null;
               }

               ArrayList var7 = new ArrayList(var24);

               for (int var8 = 0; var8 < var24; var8++) {
                  int var9 = var3.readInt();
                  int var10 = var3.readInt();
                  int var11 = var3.readInt();
                  int var12 = var3.readInt();
                  int var13 = var3.readInt();
                  int var14 = var3.readInt();
                  int var15 = var3.readInt();
                  int var16 = var3.readInt();
                  if (var16 < 0 || var16 > 4000000) {
                     return null;
                  }

                  long[] var17 = new long[var16];

                  for (int var18 = 0; var18 < var16; var18++) {
                     var17[var18] = var3.readLong();
                  }

                  var7.add(new Lake(var9, var10, var11, var12, var13, var14, var15, var17));
               }

               RiverNetwork var26 = NetworkTracer.assemble(var1, var5, var7, this.g);
               NetworkTracer.dumpLoadedSegments(var1, var5, var7);
               Constants.LOG.debug("Loaded river region {} from cache ({} segments, {} lakes)", var1, var4, var24);
               var27 = var26;
            }

            return var27;
         } catch (RuntimeException | IOException var21) {
            Constants.LOG.warn("Failed to read cached river region {} ({}); rebuilding", var1, var21.toString());
            return null;
         }
      }
   }

   @Override
   public void save(PlateKey var1, RiverNetwork var2) {
      try {
         this.a();
         Path var3 = this.f.resolve(a(var1));
         Path var4 = this.f.resolve(a(var1) + ".tmp");
         List var5 = var2.segments();

         try (DataOutputStream var6 = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(var4)))) {
            var6.writeInt(1397902897);
            var6.writeInt(28);
            var6.writeLong(this.h);
            var6.writeInt(var5.size());

            for (RiverSegment var8 : var5) {
               var6.writeDouble(var8.x1());
               var6.writeDouble(var8.z1());
               var6.writeDouble(var8.y1());
               var6.writeDouble(var8.w1());
               var6.writeDouble(var8.x2());
               var6.writeDouble(var8.z2());
               var6.writeDouble(var8.y2());
               var6.writeDouble(var8.w2());
            }

            List var19 = var2.lakes();
            var6.writeInt(var19.size());

            for (Lake var9 : var19) {
               var6.writeInt(var9.level());
               var6.writeInt(var9.digDepth());
               var6.writeInt(var9.spacing());
               var6.writeInt(var9.cellMinX());
               var6.writeInt(var9.cellMinZ());
               var6.writeInt(var9.cellsWide());
               var6.writeInt(var9.cellsHigh());
               long[] var10 = var9.mask();
               var6.writeInt(var10.length);

               for (long var14 : var10) {
                  var6.writeLong(var14);
               }
            }
         }

         Files.move(var4, var3, StandardCopyOption.REPLACE_EXISTING);
      } catch (RuntimeException | IOException var18) {
         Constants.LOG.warn("Failed to persist river region {} ({})", var1, var18.toString());
      }
   }

   private void a() throws IOException {
      if (!this.i) {
         Files.createDirectories(this.f);
         this.i = true;
      }
   }

   private static String a(PlateKey var0) {
      return "r." + var0.cx() + "." + var0.cz() + ".srn";
   }

   private static long a(long var0, StreamsConfig var2) {
      long var3 = 1469598103934665603L;
      var3 = a(var3, var0);
      var3 = a(var3, (long)var2.seaLevel());
      var3 = a(var3, Double.doubleToLongBits(var2.plateScale()));
      var3 = a(var3, Double.doubleToLongBits(var2.plateJitter()));
      var3 = a(var3, (long)var2.gridSpacing());
      var3 = a(var3, (long)var2.terrainSampleSpacing());
      var3 = a(var3, (long)var2.valleySnapRadius());
      var3 = a(var3, Double.doubleToLongBits(var2.minRiverAccumulation()));
      var3 = a(var3, Double.doubleToLongBits(var2.wetnessMultiplier()));
      var3 = a(var3, Double.doubleToLongBits(var2.baseWidth()));
      var3 = a(var3, Double.doubleToLongBits(var2.maxWidth()));
      var3 = a(var3, Double.doubleToLongBits(var2.widthScale()));
      var3 = a(var3, Double.doubleToLongBits(var2.borderMargin()));
      var3 = a(var3, (long)var2.smoothingPasses());
      var3 = a(var3, Double.doubleToLongBits(var2.meanderStrength()));
      var3 = a(var3, (long)var2.seaLevelExtension());
      var3 = a(var3, Double.doubleToLongBits(var2.reconnectRadius()));
      var3 = a(var3, Double.doubleToLongBits(var2.wetnessDryCutoff()));
      var3 = a(var3, Double.doubleToLongBits(var2.wetnessWetReference()));
      var3 = a(var3, var2.sinkRooted() ? 1L : 0L);
      var3 = a(var3, var2.growFromWater() ? 1L : 0L);
      var3 = a(var3, Double.doubleToLongBits(var2.growDensity()));
      var3 = a(var3, var2.confineToBanks() ? 1L : 0L);
      return a(var3, var2.outletBiomes() == null ? 0L : (long)var2.outletBiomes().hashCode());
   }

   private static long a(long var0, long var2) {
      return (var0 ^ var2) * 1099511628211L;
   }
}
