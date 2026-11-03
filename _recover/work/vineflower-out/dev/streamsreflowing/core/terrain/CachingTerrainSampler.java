package dev.streamsreflowing.core.terrain;

import dev.streamsreflowing.core.ColumnKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class CachingTerrainSampler implements TerrainSampler {
   private final TerrainSampler a;
   private final int b;
   private final double c;
   private final ConcurrentHashMap<Long, Double> d = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> e = new ConcurrentLinkedQueue<>();
   private final ConcurrentHashMap<Long, Double> f = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> g = new ConcurrentLinkedQueue<>();
   private final AtomicLong h = new AtomicLong();
   private final AtomicLong i = new AtomicLong();
   private final ConcurrentHashMap<Long, Double> j = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> k = new ConcurrentLinkedQueue<>();
   private final ConcurrentHashMap<Long, Double> l = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long> m = new ConcurrentLinkedQueue<>();

   private static long a(int var0, int var1) {
      return ColumnKey.of(var0, var1);
   }

   public CachingTerrainSampler(TerrainSampler var1, int var2) {
      this(var1, var2, Double.NaN);
   }

   public CachingTerrainSampler(TerrainSampler var1, int var2, double var3) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
   }

   @Override
   public int waterBiomeClass(int var1, int var2) {
      return this.a.waterBiomeClass(var1, var2);
   }

   @Override
   public int waterBiomeClassAt(int var1, int var2, int var3) {
      return this.a.waterBiomeClassAt(var1, var2, var3);
   }

   @Override
   public double riverSurface(int var1, int var2) {
      return this.a.riverSurface(var1, var2);
   }

   @Override
   public boolean outletBiomeOk(int var1, int var2, int var3) {
      return this.a.outletBiomeOk(var1, var2, var3);
   }

   @Override
   public double height(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.d.get(var3);
      if (var5 != null) {
         this.h.incrementAndGet();
         return var5;
      } else {
         double var6 = this.a.height(var1, var2);
         if (var6 == this.c) {
            return var6;
         } else {
            Double var8 = this.d.putIfAbsent(var3, var6);
            if (var8 != null) {
               this.h.incrementAndGet();
               return var8;
            } else {
               this.i.incrementAndGet();
               this.e.offer(var3);

               while (this.d.size() > this.b) {
                  Long var9 = this.e.poll();
                  if (var9 == null) {
                     break;
                  }

                  this.d.remove(var9);
               }

               return var6;
            }
         }
      }
   }

   @Override
   public double surfaceHeight(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.f.get(var3);
      if (var5 != null) {
         this.h.incrementAndGet();
         return var5;
      } else {
         double var6 = this.a.surfaceHeight(var1, var2);
         Double var8 = this.f.putIfAbsent(var3, var6);
         if (var8 != null) {
            this.h.incrementAndGet();
            return var8;
         } else {
            this.i.incrementAndGet();
            this.g.offer(var3);

            while (this.f.size() > this.b) {
               Long var9 = this.g.poll();
               if (var9 == null) {
                  break;
               }

               this.f.remove(var9);
            }

            return var6;
         }
      }
   }

   @Override
   public double groundHeight(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.j.get(var3);
      if (var5 != null) {
         this.h.incrementAndGet();
         return var5;
      } else {
         double var6 = this.a.groundHeight(var1, var2);
         if (var6 == this.c) {
            return var6;
         } else {
            Double var8 = this.j.putIfAbsent(var3, var6);
            if (var8 != null) {
               this.h.incrementAndGet();
               return var8;
            } else {
               this.i.incrementAndGet();
               this.k.offer(var3);

               while (this.j.size() > this.b) {
                  Long var9 = this.k.poll();
                  if (var9 == null) {
                     break;
                  }

                  this.j.remove(var9);
               }

               return var6;
            }
         }
      }
   }

   @Override
   public boolean hasEstimate() {
      return this.a.hasEstimate();
   }

   @Override
   public boolean useHybridGridFill() {
      return this.a.useHybridGridFill();
   }

   @Override
   public double estimate(int var1, int var2) {
      return this.a.estimate(var1, var2);
   }

   @Override
   public boolean openAt(int var1, int var2, int var3) {
      return this.a.openAt(var1, var2, var3);
   }

   @Override
   public double groundHeightExact(int var1, int var2) {
      long var3 = a(var1, var2);
      Double var5 = this.l.get(var3);
      if (var5 != null) {
         this.h.incrementAndGet();
         return var5;
      } else {
         double var6 = this.a.groundHeightExact(var1, var2);
         if (var6 == this.c) {
            return var6;
         } else {
            Double var8 = this.l.putIfAbsent(var3, var6);
            if (var8 != null) {
               this.h.incrementAndGet();
               return var8;
            } else {
               this.i.incrementAndGet();
               this.m.offer(var3);

               while (this.l.size() > this.b) {
                  Long var9 = this.m.poll();
                  if (var9 == null) {
                     break;
                  }

                  this.l.remove(var9);
               }

               return var6;
            }
         }
      }
   }

   @Override
   public boolean bakedCalibration() {
      return this.a.bakedCalibration();
   }

   public long hits() {
      return this.h.get();
   }

   public long misses() {
      return this.i.get();
   }

   public int hitPercent() {
      long var1 = this.h.get();
      long var3 = var1 + this.i.get();
      return var3 > 0L ? (int)(var1 * 100L / var3) : 0;
   }
}
