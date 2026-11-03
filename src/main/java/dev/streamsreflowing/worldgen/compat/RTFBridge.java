/*
 * Decompiled with CFR 0.152.
 */
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
    private static volatile Surface b;
    private static final Map<Object, Object> c;
    private static final Object d;
    private static final AtomicBoolean e;

    private RTFBridge() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static Surface a() {
        if (a) return b;
        Class<RTFBridge> clazz = RTFBridge.class;
        synchronized (RTFBridge.class) {
            if (a) return b;
            if (!Boolean.getBoolean("streamsreflowing.nortf")) {
                try {
                    b = new Surface();
                    Constants.LOG.debug("Streams Reflowing: ReTerraForged bridge available.");
                }
                catch (LinkageError | ReflectiveOperationException throwable) {
                    b = null;
                    Constants.LOG.debug("Streams Reflowing: no ReTerraForged bridge ({}).", (Object)throwable.toString());
                }
            }
            a = true;
            // ** MonitorExit[var0] (shouldn't be in output)
            return b;
        }
    }

    public static boolean available() {
        return Services.PLATFORM.reterraforgedCompat() && RTFBridge.a() != null;
    }

    public static byte[] flowUnderlay(Object object) {
        if (!Services.PLATFORM.reterraforgedCompat()) {
            return null;
        }
        Surface surface = RTFBridge.a();
        try {
            if (surface == null || object == null || !surface.k.isInstance(object)) {
                return null;
            }
            Object object2 = surface.l.invoke(object, new Object[0]);
            if (object2 == null || !((Boolean)surface.m.invoke(object2, new Object[0])).booleanValue()) {
                return null;
            }
            return RTFFlowUnderlay.convert((byte[])surface.n.invoke(object2, new Object[0]));
        }
        catch (LinkageError | ReflectiveOperationException throwable) {
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Handle handleFor(Object object) {
        Object object2;
        if (!Services.PLATFORM.reterraforgedCompat()) {
            return null;
        }
        Surface surface = RTFBridge.a();
        if (surface == null || object == null) {
            return null;
        }
        Object object3 = c;
        synchronized (object3) {
            Object object4 = c.get(object);
            if (object4 != null) {
                return object4 == d ? null : (Handle)object4;
            }
        }
        object3 = null;
        boolean bl = false;
        try {
            if (Class.forName("raccoonman.reterraforged.world.worldgen.RTFRandomState").isInstance(object)) {
                object2 = surface.a.invoke(object, new Object[0]);
                if (object2 == null) {
                    bl = true;
                } else {
                    Object object5 = surface.b.get(object2);
                    Object object6 = surface.c.get(object2);
                    if (object5 != null && object6 != null) {
                        object3 = new Handle(surface, object5, surface.d.getInt(object6));
                        Constants.LOG.info("Streams Reflowing: reading terrain and rivers from ReTerraForged for this world.");
                    }
                }
            }
        }
        catch (LinkageError | ReflectiveOperationException throwable) {
            Constants.LOG.debug("Streams Reflowing: RTF handle failed ({}).", (Object)throwable.toString());
        }
        if (bl) {
            RTFBridge.b();
            return null;
        }
        object2 = c;
        synchronized (object2) {
            c.put(object, object3 == null ? d : object3);
        }
        return (Handle)object3;
    }

    private static void b() {
        if (e.compareAndSet(false, true)) {
            Constants.LOG.warn("Streams Reflowing: ReTerraForged is installed but its terrain was not available when this world's streams were prepared, so heights are being read the slow way. If stream generation is slow here, please report this line.");
        }
    }

    static {
        c = new WeakHashMap<Object, Object>();
        d = new Object();
        e = new AtomicBoolean();
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
            Class<?> clazz = Class.forName("raccoonman.reterraforged.world.worldgen.RTFRandomState");
            Class<?> clazz2 = Class.forName("raccoonman.reterraforged.world.worldgen.GeneratorContext");
            Class<?> clazz3 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup");
            Class<?> clazz4 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels");
            Class<?> clazz5 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.Cell");
            Class<?> clazz6 = Class.forName("raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType");
            this.a = clazz.getMethod("generatorContext", new Class[0]);
            this.b = clazz2.getField("lookup");
            this.c = clazz2.getField("levels");
            this.d = clazz4.getField("worldHeight");
            this.e = clazz3.getMethod("applyCell", clazz5, Integer.TYPE, Integer.TYPE, Boolean.TYPE);
            this.f = clazz5.getConstructor(new Class[0]);
            this.g = clazz5.getField("height");
            this.h = clazz5.getField("terrain");
            this.i = clazz5.getField("riverWaterLevel");
            this.j = clazz6.getField("RIVER").get(null);
            this.k = Class.forName("raccoonman.reterraforged.world.worldgen.IFlowFieldHolder");
            Class<?> clazz7 = Class.forName("raccoonman.reterraforged.world.worldgen.ChunkFlowField");
            this.l = this.k.getMethod("reterraforged$getFlowField", new Class[0]);
            this.m = clazz7.getMethod("hasRivers", new Class[0]);
            this.n = clazz7.getMethod("getRawGrid", new Class[0]);
        }
    }

    public static final class Handle {
        private final Surface a;
        private final Object b;
        private final int c;
        private final ThreadLocal<Object> d;
        private final Map<Long, Long> e = new ConcurrentHashMap<Long, Long>();
        private static final int f = 400000;
        private final Map<Long, Float> g = new ConcurrentHashMap<Long, Float>();

        Handle(Surface surface, Object object, int n) {
            this.a = surface;
            this.b = object;
            this.c = n;
            this.d = ThreadLocal.withInitial(() -> {
                try {
                    return surface.f.newInstance(new Object[0]);
                }
                catch (ReflectiveOperationException reflectiveOperationException) {
                    throw new IllegalStateException(reflectiveOperationException);
                }
            });
        }

        private long a(int n, int n2) {
            try {
                Object object = this.d.get();
                this.a.e.invoke(this.b, object, n, n2, false);
                float f = this.a.g.getFloat(object) * (float)this.c;
                boolean bl = this.a.h.get(object) == this.a.j;
                long l = (long)Float.floatToIntBits(f) << 32 | (bl ? 1L : 0L);
                if (bl) {
                    this.g.put(ColumnKey.of(n, n2), Float.valueOf(this.a.i.getFloat(object) * (float)this.c));
                }
                if (this.e.size() >= 400000) {
                    this.e.clear();
                    this.g.clear();
                }
                this.e.put(ColumnKey.of(n, n2), l);
                return l;
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                throw new IllegalStateException("RTF bridge invocation failed", reflectiveOperationException);
            }
        }

        private long b(int n, int n2) {
            Long l = this.e.get(ColumnKey.of(n, n2));
            return l != null ? l.longValue() : this.a(n, n2);
        }

        public double height(int n, int n2) {
            return Float.intBitsToFloat((int)(this.b(n, n2) >>> 32));
        }

        public boolean river(int n, int n2) {
            return (this.b(n, n2) & 1L) != 0L;
        }

        public double riverSurface(int n, int n2) {
            if ((this.b(n, n2) & 1L) == 0L) {
                return Double.NaN;
            }
            Float f = this.g.get(ColumnKey.of(n, n2));
            if (f == null) {
                this.a(n, n2);
                f = this.g.get(ColumnKey.of(n, n2));
            }
            return f != null ? (double)f.floatValue() : Double.NaN;
        }
    }
}

