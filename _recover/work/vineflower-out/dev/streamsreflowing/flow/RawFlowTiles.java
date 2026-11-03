package dev.streamsreflowing.flow;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RawFlowTiles {
   public static final int COLUMNS = 256;
   public static final short NO_SURFACE = -32768;
   private static final int MAX_CACHED = 4096;
   private static final ConcurrentHashMap<Long, RawFlowTiles.Tile> CACHE = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<Long> ORDER = new ConcurrentLinkedQueue<>();

   private RawFlowTiles() {
   }

   private static long key(int cx, int cz) {
      return (long)cx << 32 ^ (long)cz & 4294967295L;
   }

   public static void put(int cx, int cz, RawFlowTiles.Tile tile) {
      RawFlowTileStore.store(cx, cz, tile);
      if (CACHE.put(key(cx, cz), tile) == null) {
         ORDER.offer(key(cx, cz));
      }

      while (CACHE.size() > 4096) {
         Long old = ORDER.poll();
         if (old == null) {
            break;
         }

         CACHE.remove(old);
      }
   }

   public static RawFlowTiles.Tile get(int cx, int cz) {
      RawFlowTiles.Tile t = CACHE.get(key(cx, cz));
      if (t != null) {
         return t;
      } else {
         t = RawFlowTileStore.load(cx, cz);
         if (t != null && CACHE.put(key(cx, cz), t) == null) {
            ORDER.offer(key(cx, cz));
         }

         return t;
      }
   }

   public static void clear() {
      CACHE.clear();
      ORDER.clear();
   }

   public static record Tile(byte[] dirX, byte[] dirZ, short[] surfaceY) {
   }
}
