package dev.streamsreflowing.flow;

import dev.streamsreflowing.Constants;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class RawFlowTileStore {
   private static final int MAGIC = 1397904433;
   private static final int VERSION = 1;
   private static final int REGION_CHUNKS = 32;
   private static final int PER_REGION = 1024;
   private static volatile Path storeDir;
   private static volatile long fingerprint;
   private static final Map<Long, RawFlowTileStore.Region> REGIONS = new ConcurrentHashMap<>();

   private RawFlowTileStore() {
   }

   public static void initStore(Path dir, long fp) {
      storeDir = dir;
      fingerprint = fp;
      REGIONS.clear();
   }

   private static boolean enabled() {
      return storeDir != null;
   }

   private static long regionKey(int rx, int rz) {
      return (long)rx << 32 ^ (long)rz & 4294967295L;
   }

   private static int slot(int cx, int cz) {
      return Math.floorMod(cz, 32) * 32 + Math.floorMod(cx, 32);
   }

   public static RawFlowTiles.Tile load(int cx, int cz) {
      if (!enabled()) {
         return null;
      } else {
         int rx = Math.floorDiv(cx, 32);
         int rz = Math.floorDiv(cz, 32);
         RawFlowTileStore.Region region = REGIONS.computeIfAbsent(regionKey(rx, rz), k -> readRegion(rx, rz));
         return region.tiles[slot(cx, cz)];
      }
   }

   public static void store(int cx, int cz, RawFlowTiles.Tile tile) {
      if (enabled()) {
         int rx = Math.floorDiv(cx, 32);
         int rz = Math.floorDiv(cz, 32);
         RawFlowTileStore.Region region = REGIONS.computeIfAbsent(regionKey(rx, rz), k -> readRegion(rx, rz));
         region.tiles[slot(cx, cz)] = tile;
         region.dirty = true;
      }
   }

   private static Path fileFor(int rx, int rz) {
      return storeDir.resolve("t." + rx + "." + rz + ".srt");
   }

   private static RawFlowTileStore.Region readRegion(int rx, int rz) {
      RawFlowTileStore.Region region = new RawFlowTileStore.Region();
      Path f = fileFor(rx, rz);
      if (!Files.isRegularFile(f)) {
         return region;
      } else {
         try {
            label98: {
               RawFlowTileStore.Region var17;
               try (
                  InputStream raw = Files.newInputStream(f);
                  DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(raw)));
               ) {
                  if (in.readInt() == 1397904433 && in.readInt() == 1 && in.readLong() == fingerprint) {
                     byte[] present = new byte[128];
                     in.readFully(present);

                     for (int i = 0; i < 1024; i++) {
                        if ((present[i >> 3] & 1 << (i & 7)) != 0) {
                           byte[] dirX = new byte[256];
                           byte[] dirZ = new byte[256];
                           short[] surfaceY = new short[256];
                           in.readFully(dirX);
                           in.readFully(dirZ);

                           for (int c = 0; c < 256; c++) {
                              surfaceY[c] = in.readShort();
                           }

                           region.tiles[i] = new RawFlowTiles.Tile(dirX, dirZ, surfaceY);
                        }
                     }
                     break label98;
                  }

                  var17 = region;
               }

               return var17;
            }

            return region;
         } catch (RuntimeException | IOException var16) {
            Constants.LOG.debug("Flow-tile region ({},{}) unreadable, rebuilding: {}", rx, rz, var16.toString());
            return new RawFlowTileStore.Region();
         }
      }
   }

   public static void flush() {
      if (enabled()) {
         for (Entry<Long, RawFlowTileStore.Region> e : REGIONS.entrySet()) {
            RawFlowTileStore.Region region = e.getValue();
            if (region.dirty) {
               region.dirty = false;
               long k = e.getKey();
               writeRegion((int)(k >> 32), (int)k, region);
            }
         }
      }
   }

   private static void writeRegion(int rx, int rz, RawFlowTileStore.Region region) {
      try {
         Files.createDirectories(storeDir);
         byte[] present = new byte[128];
         int count = 0;

         for (int i = 0; i < 1024; i++) {
            if (region.tiles[i] != null && hasFlow(region.tiles[i])) {
               present[i >> 3] = (byte)(present[i >> 3] | (byte)(1 << (i & 7)));
               count++;
            }
         }

         Path tmp = fileFor(rx, rz).resolveSibling(fileFor(rx, rz).getFileName() + ".tmp");

         try (
            OutputStream raw = Files.newOutputStream(tmp);
            DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(raw)));
         ) {
            out.writeInt(1397904433);
            out.writeInt(1);
            out.writeLong(fingerprint);
            out.write(present);

            for (int ix = 0; ix < 1024; ix++) {
               if ((present[ix >> 3] & 1 << (ix & 7)) != 0) {
                  RawFlowTiles.Tile t = region.tiles[ix];
                  out.write(t.dirX());
                  out.write(t.dirZ());

                  for (short s : t.surfaceY()) {
                     out.writeShort(s);
                  }
               }
            }
         }

         Files.move(tmp, fileFor(rx, rz), StandardCopyOption.REPLACE_EXISTING);
         Constants.LOG.debug("Saved flow-tile region ({},{}): {} chunks with flow", rx, rz, count);
      } catch (RuntimeException | IOException var18) {
         Constants.LOG.debug("Could not save flow-tile region ({},{}): {}", rx, rz, var18.toString());
      }
   }

   private static boolean hasFlow(RawFlowTiles.Tile t) {
      for (int i = 0; i < t.dirX().length; i++) {
         if (t.dirX()[i] != 0 || t.dirZ()[i] != 0) {
            return true;
         }
      }

      return false;
   }

   public static void close() {
      flush();
      REGIONS.clear();
      storeDir = null;
   }

   private static final class Region {
      final RawFlowTiles.Tile[] tiles = new RawFlowTiles.Tile[1024];
      volatile boolean dirty;
   }
}
