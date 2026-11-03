package dev.streamsreflowing.worldgen.compat;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.ColumnKey;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import dev.streamsreflowing.platform.Services;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RTFBridge {
   private static volatile boolean a;
   private static volatile RTFBridge.Surface b;
   private static final Map<Object, Object> c = new WeakHashMap<>();
   private static final Object d = new Object();
   private static final AtomicBoolean e = new AtomicBoolean();

   private RTFBridge() {
   }

   private static RTFBridge.Surface a() {
      if (!a) {
         synchronized (RTFBridge.class) {
            if (!a) {
               if (!Boolean.getBoolean("streamsreflowing.nortf")) {
                  try {
                     b = new RTFBridge.Surface();
                     Constants.LOG.debug("Streams Reflowing: ReTerraForged bridge available.");
                  } catch (LinkageError | ReflectiveOperationException var3) {
                     b = null;
                     Constants.LOG.debug("Streams Reflowing: no ReTerraForged bridge ({}).", var3.toString());
                  }
               }

               a = true;
            }
         }
      }

      return b;
   }

   public static boolean available() {
      return Services.PLATFORM.reterraforgedCompat() && a() != null;
   }

   public static byte[] flowUnderlay(Object var0) {
      if (!Services.PLATFORM.reterraforgedCompat()) {
         return null;
      } else {
         RTFBridge.Surface var1 = a();

         try {
            if (var1 != null && var0 != null && var1.k.isInstance(var0)) {
               Object var2 = var1.l.invoke(var0);
               return var2 != null && var1.m.invoke(var2) ? RTFFlowUnderlay.convert((byte[])var1.n.invoke(var2)) : null;
            } else {
               return null;
            }
         } catch (LinkageError | ReflectiveOperationException var3) {
            return null;
         }
      }
   }

   public static RTFBridge.Handle handleFor(Object var0) {
      if (!Services.PLATFORM.reterraforgedCompat()) {
         return null;
      } else {
         RTFBridge.Surface var1 = a();
         if (var1 != null && var0 != null) {
            synchronized (c) {
               Object var3 = c.get(var0);
               if (var3 != null) {
                  return var3 == d ? null : (RTFBridge.Handle)var3;
               }
            }

            RTFBridge.Handle var11 = null;
            boolean var12 = false;

            try {
               if (Class.forName("raccoonman.reterraforged.world.worldgen.RTFRandomState").isInstance(var0)) {
                  Object var4 = var1.a.invoke(var0);
                  if (var4 == null) {
                     var12 = true;
                  } else {
                     Object var5 = var1.b.get(var4);
                     Object var6 = var1.c.get(var4);
                     if (var5 != null && var6 != null) {
                        var11 = new RTFBridge.Handle(var1, var5, var1.d.getInt(var6));
                        Constants.LOG.info("Streams Reflowing: reading terrain and rivers from ReTerraForged for this world.");
                     }
                  }
               }
            } catch (LinkageError | ReflectiveOperationException var9) {
               Constants.LOG.debug("Streams Reflowing: RTF handle failed ({}).", var9.toString());
            }

            if (var12) {
               b();
               return null;
            } else {
               synchronized (c) {
                  c.put(var0, var11 == null ? d : var11);
                  return var11;
               }
            }
         } else {
            return null;
         }
      }
   }

   private static void b() {
      if (e.compareAndSet(false, true)) {
         Constants.LOG
            .warn(
               "Streams Reflowing: ReTerraForged is installed but its terrain was not available when this world's streams were prepared, so heights are being read the slow way. If stream generation is slow here, please report this line."
            );
      }
   }

   public static final class Handle {
      private final RTFBridge.Surface a;
      private final Object b;
      private final int c;
      private final ThreadLocal<Object> d;
      private final Map<Long, Long> e = new ConcurrentHashMap<>();
      private static final int f = 400000;
      private final Map<Long, Float> g = new ConcurrentHashMap<>();

      Handle(RTFBridge.Surface var1, Object var2, int var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = ThreadLocal.withInitial(() -> {
            try {
               return var1.f.newInstance();
            } catch (ReflectiveOperationException var2x) {
               throw new IllegalStateException(var2x);
            }
         });
      }

      private long a(int var1, int var2) {
         try {
            Object var3 = this.d.get();
            this.a.e.invoke(this.b, var3, var1, var2, false);
            float var4 = this.a.g.getFloat(var3) * (float)this.c;
            boolean var5 = this.a.h.get(var3) == this.a.j;
            long var6 = (long)Float.floatToIntBits(var4) << 32 | (var5 ? 1L : 0L);
            if (var5) {
               this.g.put(ColumnKey.of(var1, var2), this.a.i.getFloat(var3) * (float)this.c);
            }

            if (this.e.size() >= 400000) {
               this.e.clear();
               this.g.clear();
            }

            this.e.put(ColumnKey.of(var1, var2), var6);
            return var6;
         } catch (ReflectiveOperationException var8) {
            throw new IllegalStateException("RTF bridge invocation failed", var8);
         }
      }

      private long b(int var1, int var2) {
         Long var3 = this.e.get(ColumnKey.of(var1, var2));
         return var3 != null ? var3 : this.a(var1, var2);
      }

      public double height(int var1, int var2) {
         return (double)Float.intBitsToFloat((int)(this.b(var1, var2) >>> 32));
      }

      public boolean river(int var1, int var2) {
         return (this.b(var1, var2) & 1L) != 0L;
      }

      public double riverSurface(int var1, int var2) {
         if ((this.b(var1, var2) & 1L) == 0L) {
            return Double.NaN;
         } else {
            Float var3 = this.g.get(ColumnKey.of(var1, var2));
            if (var3 == null) {
               this.a(var1, var2);
               var3 = this.g.get(ColumnKey.of(var1, var2));
            }

            return var3 != null ? (double)var3.floatValue() : Double.NaN;
         }
      }
   }

   private static final class Surface {
      final Method a;
      final Field b;
      final Field c;
      final Field d;
      final Method e;
      final Constructor<?> f;
      final Field g;
      final Field h;
      final Field i;
      final Object j;
      final Class<?> k;
      final Method l;
      final Method m;
      final Method n;

      Surface() throws ReflectiveOperationException {
         Class var1 = Class.forName("raccoonman.reterraforged.world.worldgen.RTFRandomState");
         Class var2 = Class.forName("raccoonman.reterraforged.world.worldgen.GeneratorContext");
         Class var3 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup");
         Class var4 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels");
         Class var5 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.Cell");
         Class var6 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType");
         this.a = var1.getMethod("generatorContext");
         this.b = var2.getField("lookup");
         this.c = var2.getField("levels");
         this.d = var4.getField("worldHeight");
         this.e = var3.getMethod("applyCell", var5, int.class, int.class, boolean.class);
         this.f = var5.getConstructor();
         this.g = var5.getField("height");
         this.h = var5.getField("terrain");
         this.i = var5.getField("riverWaterLevel");
         this.j = var6.getField("RIVER").get(null);
         this.k = Class.forName("raccoonman.reterraforged.world.worldgen.IFlowFieldHolder");
         Class var7 = Class.forName("raccoonman.reterraforged.world.worldgen.ChunkFlowField");
         this.l = this.k.getMethod("reterraforged$getFlowField");
         this.m = var7.getMethod("hasRivers");
         this.n = var7.getMethod("getRawGrid");
      }
   }
}
