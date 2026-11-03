/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public final class RiverFlowFields {
    private static volatile int a = -1;
    private static final int b = 512;
    private static final int c = 8;
    private static final int d = 256;
    private static final ConcurrentHashMap<Long, RiverFlowField> e = new ConcurrentHashMap();
    private static final ConcurrentLinkedQueue<Long> f = new ConcurrentLinkedQueue();
    private static final int g = 1397900849;
    private static final int h = 18;
    private static volatile Path i;
    private static volatile long j;
    private static final Map<ServerLevel, RiverFlowField.BiomeClass> k;
    private static final int l = 262144;
    private static final int[] m;
    private static final int[] n;

    private static int a() {
        int n = a;
        if (n <= 0) {
            a = n = (switch (Services.PLATFORM.riverDrainageSize()) {
                case 2 -> 1024;
                case 3 -> 2048;
                default -> 512;
            });
        }
        return n;
    }

    private RiverFlowFields() {
    }

    public static int dirAt(RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField, int n, int n2) {
        RiverFlowField riverFlowField = RiverFlowFields.a(Math.floorDiv(n, RiverFlowFields.a()), Math.floorDiv(n2, RiverFlowFields.a()));
        return riverFlowField == null ? -1 : riverFlowField.dirAt(n, n2);
    }

    public static double flowRadAt(RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField, int n, int n2) {
        RiverFlowField riverFlowField = RiverFlowFields.a(Math.floorDiv(n, RiverFlowFields.a()), Math.floorDiv(n2, RiverFlowFields.a()));
        return riverFlowField == null ? Double.NaN : riverFlowField.flowRadAt(n, n2);
    }

    private static RiverFlowField a(int n, int n2) {
        return e.get((long)n << 32 ^ (long)n2 & 0xFFFFFFFFL);
    }

    public static void initStore(Path path, long l) {
        i = path;
        j = l;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static RiverFlowField b(int n, int n2) {
        Path path = i;
        if (path == null) {
            return null;
        }
        Path path2 = path.resolve("f." + n + "." + n2 + ".srf");
        if (!Files.isRegularFile(path2, new LinkOption[0])) {
            return null;
        }
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(Files.newInputStream(path2, new OpenOption[0])));){
            if (dataInputStream.readInt() != 1397900849 || dataInputStream.readInt() != 18 || dataInputStream.readLong() != j || dataInputStream.readInt() != RiverFlowFields.a()) {
                RiverFlowField riverFlowField = null;
                return riverFlowField;
            }
            RiverFlowField riverFlowField = RiverFlowField.readFrom(dataInputStream);
            return riverFlowField;
        }
        catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static void a(int n, int n2, RiverFlowField riverFlowField) {
        Path path = i;
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path, new FileAttribute[0]);
            Path path2 = path.resolve("f." + n + "." + n2 + ".srf");
            Path path3 = path.resolve("f." + n + "." + n2 + ".srf.tmp");
            try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path3, new OpenOption[0])));){
                dataOutputStream.writeInt(1397900849);
                dataOutputStream.writeInt(18);
                dataOutputStream.writeLong(j);
                dataOutputStream.writeInt(RiverFlowFields.a());
                riverFlowField.writeTo(dataOutputStream);
            }
            Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException | RuntimeException exception) {
            Constants.LOG.debug("Flow-field save failed for region ({}, {}): {}", n, n2, exception.toString());
        }
    }

    private static RiverFlowField a(int n, int n2, RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField) {
        Long l2;
        long l3 = (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
        RiverFlowField riverFlowField = e.computeIfAbsent(l3, l -> {
            RiverFlowField riverFlowField;
            RiverFlowField riverFlowField2 = RiverFlowFields.b(n, n2);
            RiverFlowField riverFlowField3 = riverFlowField = riverFlowField2 != null ? riverFlowField2 : RiverFlowFields.b(n, n2, biomeClass, orientationField);
            if (riverFlowField2 == null) {
                RiverFlowFields.a(n, n2, riverFlowField);
            }
            f.add((Long)l);
            return riverFlowField;
        });
        while (e.size() > 256 && (l2 = f.poll()) != null) {
            e.remove(l2);
        }
        return riverFlowField;
    }

    private static RiverFlowField b(int n, int n2, RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField) {
        long l = System.nanoTime();
        int n3 = 2;
        RiverFlowField riverFlowField = RiverFlowField.build(n * RiverFlowFields.a(), n2 * RiverFlowFields.a(), RiverFlowFields.a(), RiverFlowFields.a(), 512, 8, n3, biomeClass, orientationField);
        double d = (double)(System.nanoTime() - l) / 1000000.0;
        FlowTrace.fieldBuilds.incrementAndGet();
        FlowTrace.fieldBuildMs.addAndGet((long)d);
        StreamFlowGrid.invalidateArea(n * RiverFlowFields.a(), n2 * RiverFlowFields.a(), RiverFlowFields.a());
        int n4 = RiverFlowField.lastSampled;
        int n5 = RiverFlowField.lastCells;
        String string = "Built river-flow field ({}, {}): {} ocean, {} river, {} flowed cells (spacing {}), {} of {} cells classified ({}%), {} ms";
        Object[] objectArray = new Object[]{n, n2, riverFlowField.oceanCells, riverFlowField.riverCells, riverFlowField.flowedCells, 8, n4, n5, n5 == 0 ? 0 : 100 * n4 / n5, String.format("%.1f", d)};
        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
            Constants.LOG.info(string, objectArray);
        } else {
            Constants.LOG.debug(string, objectArray);
        }
        return riverFlowField;
    }

    public static RiverFlowField.BiomeClass classifierFor(ServerLevel serverLevel) {
        return k.computeIfAbsent(serverLevel, RiverFlowFields::a);
    }

    private static RiverFlowField.BiomeClass a(ServerLevel serverLevel) {
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        BiomeSource biomeSource = chunkGenerator.getBiomeSource();
        Climate.Sampler sampler = randomState.sampler();
        int n = QuartPos.fromBlock(chunkGenerator.getSeaLevel());
        ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
        AtomicInteger atomicInteger = new AtomicInteger();
        int n4 = 13;
        int n5 = 8191;
        ThreadLocal<long[]> threadLocal = ThreadLocal.withInitial(() -> {
            long[] lArray = new long[8192];
            Arrays.fill(lArray, Long.MIN_VALUE);
            return lArray;
        });
        ThreadLocal<byte[]> threadLocal2 = ThreadLocal.withInitial(() -> new byte[8192]);
        return (n2, n3) -> {
            byte by;
            int n4 = n2 >> 2;
            int n5 = n3 >> 2;
            long l = (long)n4 << 32 ^ (long)n5 & 0xFFFFFFFFL;
            long[] lArray = (long[])threadLocal.get();
            byte[] byArray = (byte[])threadLocal2.get();
            int n6 = (int)(l ^ l >>> 32) & 0x1FFF;
            if (lArray[n6] == l) {
                return byArray[n6];
            }
            Byte by2 = (Byte)concurrentHashMap.get(l);
            if (by2 != null) {
                byArray[n6] = by2;
                lArray[n6] = l;
                return by2.byteValue();
            }
            Holder<Biome> holder = biomeSource.getNoiseBiome(n4, n, n5, sampler);
            byArray[n6] = by = holder.is(BiomeTags.IS_OCEAN) ? (byte)2 : (holder.is(BiomeTags.IS_RIVER) || holder.is(Biomes.RIVER) || holder.is(Biomes.FROZEN_RIVER) ? (byte)1 : (byte)0);
            lArray[n6] = l;
            if (concurrentHashMap.putIfAbsent(l, by) == null && (atomicInteger.incrementAndGet() & 0xFFF) == 0 && concurrentHashMap.size() > 262144) {
                concurrentHashMap.clear();
            }
            return by;
        };
    }

    public static void unloadLevel(ServerLevel serverLevel) {
        k.remove(serverLevel);
    }

    public static RiverFlowField.OrientationField orientationFor(ServerLevel serverLevel) {
        DensityFunction densityFunction;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        if (!(chunkGenerator instanceof NoiseBasedChunkGenerator)) {
            return null;
        }
        try {
            densityFunction = serverLevel.getChunkSource().randomState().router().continents();
        }
        catch (Throwable throwable) {
            return null;
        }
        int n = chunkGenerator.getSeaLevel();
        return (n2, n3) -> densityFunction.compute(new DensityFunction.SinglePointContext(n2, n, n3));
    }

    public static int regionSize() {
        return RiverFlowFields.a();
    }

    public static boolean isRegionReady(int n, int n2) {
        long l = (long)Math.floorDiv(n, RiverFlowFields.a()) << 32 ^ (long)Math.floorDiv(n2, RiverFlowFields.a()) & 0xFFFFFFFFL;
        return e.containsKey(l);
    }

    public static void ensureRegion(RiverFlowField.BiomeClass biomeClass, RiverFlowField.OrientationField orientationField, int n, int n2) {
        RiverFlowFields.a(Math.floorDiv(n, RiverFlowFields.a()), Math.floorDiv(n2, RiverFlowFields.a()), biomeClass, orientationField);
    }

    public static long owningRegionKey(int n, int n2) {
        return (long)Math.floorDiv(n, RiverFlowFields.a()) << 32 ^ (long)Math.floorDiv(n2, RiverFlowFields.a()) & 0xFFFFFFFFL;
    }

    public static void flowMap(int n, int n2, int n3) {
        int n4 = Math.max(4, Math.min(48, n3));
        RiverFlowField riverFlowField = RiverFlowFields.a(Math.floorDiv(n, RiverFlowFields.a()), Math.floorDiv(n2, RiverFlowFields.a()));
        if (riverFlowField == null) {
            Constants.LOG.info("[SR-FLOWMAP] no river-current field built yet around ({},{}) -- it builds in the background; try again in a moment.", (Object)n, (Object)n2);
            return;
        }
        Constants.LOG.info("[SR-FLOWMAP] river currents around ({},{}), one char per {} blocks, region size {}", n, n2, riverFlowField.spacing(), RiverFlowFields.a());
        Constants.LOG.info("[SR-FLOWMAP] E=east e=SE S=south s=SW W=west w=NW N=north n=NE  '.'=no current  UPPERCASE-X marks a cell a neighbour flows straight back into (a split/collision)");
        String string = "EeSsWwNn";
        for (int i = -n4; i <= n4; ++i) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int j = -n4; j <= n4; ++j) {
                int n5;
                int n6 = n + j * riverFlowField.spacing();
                int n7 = riverFlowField.dirAt(n6, n5 = n2 + i * riverFlowField.spacing());
                if (n7 < 0) {
                    stringBuilder.append('.');
                    continue;
                }
                boolean bl = false;
                for (int k = 0; k < 8 && !bl; ++k) {
                    int n8 = riverFlowField.dirAt(n6 + m[k] * riverFlowField.spacing(), n5 + RiverFlowFields.n[k] * riverFlowField.spacing());
                    if (n8 < 0) continue;
                    int n9 = n8 - n7 & 7;
                    bl = n9 >= 3 && n9 <= 5;
                }
                stringBuilder.append(bl ? (char)'#' : string.charAt(n7));
            }
            Constants.LOG.info("[SR-FLOWMAP] {}", (Object)stringBuilder);
        }
    }

    public static void clear() {
        e.clear();
        f.clear();
        k.clear();
        i = null;
    }

    static {
        k = new ConcurrentHashMap<ServerLevel, RiverFlowField.BiomeClass>();
        m = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        n = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    }
}

