package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.BakedBandTable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;

/** 地形估算、校准文件与误差诊断放在一起；世界注册只取该次确定的采样器。 */
final class StreamTerrainCalibration {
    static final String CALIBRATION_FILENAME = "fastcal-v10.txt";
    private static final boolean q;
    private static final Class<?> x;
    private static final Method y;
    private static final Method z;
    private static final Object A;
    private static final Object B;
    private static final boolean BISHINT = Boolean.getBoolean("streamsreflowing.bishint");
    private static final ThreadLocal<int[]> HINT = ThreadLocal.withInitial(() -> new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE});

    static {
        q = Boolean.getBoolean("streamsreflowing.caldiag");
        Class<?> clazz = null;
        Method method = null;
        Method method2 = null;
        Object object = null;
        Object object2 = null;
        try {
            DensityFunction densityFunction = DensityFunctions.flatCache(DensityFunctions.zero());
            DensityFunction densityFunction2 = DensityFunctions.cache2d(DensityFunctions.zero());
            if (densityFunction.getClass() == densityFunction2.getClass()) {
                clazz = densityFunction.getClass();
                for (Method method3 : clazz.getDeclaredMethods()) {
                    if (method3.getParameterCount() != 0) continue;
                    if (method3.getReturnType().isEnum()) {
                        method = method3;
                        continue;
                    }
                    if (!DensityFunction.class.isAssignableFrom(method3.getReturnType())) continue;
                    method2 = method3;
                }
                if (method != null && method2 != null) {
                    method.setAccessible(true);
                    method2.setAccessible(true);
                    object = method.invoke((Object)densityFunction, new Object[0]);
                    object2 = method.invoke((Object)densityFunction2, new Object[0]);
                } else {
                    clazz = null;
                }
            }
        }
        catch (Throwable throwable) {
            clazz = null;
        }
        x = clazz;
        y = clazz != null ? method : null;
        z = clazz != null ? method2 : null;
        A = clazz != null ? object : null;
        B = clazz != null ? object2 : null;
    }

    private StreamTerrainCalibration() {}

    private static double a(DensityFunction densityFunction, int n, int n2, int n3, int n4, int n5) {
        int n6;
        MutableCtx mutableCtx = new MutableCtx(n, n4, n2);
        if (densityFunction.compute(mutableCtx) > 0.390625) {
            return StreamTerrainCalibration.withHint(n, n2, n4 + 1);
        }
        int n7 = n3;
        int n8 = n4;
        if (BISHINT) {
            int[] nArray = HINT.get();
            if (nArray[1] == n2 && n > nArray[0] && n - nArray[0] <= 64) {
                int n9 = Math.min(n4, Math.max(n3, nArray[2]));
                int n10 = Math.max(n3, n9 - 8);
                int n11 = Math.min(n4, n9 + 8);
                if (n10 < n11) {
                    mutableCtx.y = n9;
                    if (densityFunction.compute(mutableCtx) > 0.390625) {
                        if (n11 == n4) {
                            n7 = n9;
                            n8 = n11;
                        } else {
                            mutableCtx.y = n11;
                            if (!(densityFunction.compute(mutableCtx) > 0.390625)) {
                                n7 = n9;
                                n8 = n11;
                            }
                        }
                    } else if (n10 == n3) {
                        n7 = n10;
                        n8 = n9;
                    } else {
                        mutableCtx.y = n10;
                        if (densityFunction.compute(mutableCtx) > 0.390625) {
                            n7 = n10;
                            n8 = n9;
                        }
                    }
                }
            }
        }
        while (n8 - n7 > 1) {
            n6 = n7 + n8 >>> 1;
            mutableCtx.y = n6;
            if (densityFunction.compute(mutableCtx) > 0.390625) {
                n7 = n6;
                continue;
            }
            n8 = n6;
        }
        n6 = Math.min(n4, n8 + n5);
        mutableCtx.y = n6;
        if (densityFunction.compute(mutableCtx) > 0.390625) {
            return StreamTerrainCalibration.withHint(n, n2, StreamTerrainCalibration.b(densityFunction, n, n2, n3, n4, n5));
        }
        return StreamTerrainCalibration.withHint(n, n2, n7 + 1);
    }

    private static double withHint(int n, int n2, double d) {
        if (BISHINT) {
            int[] nArray = HINT.get();
            nArray[0] = n;
            nArray[1] = n2;
            nArray[2] = (int)d;
        }
        return d;
    }

    private static DensityFunction a(DensityFunction densityFunction2) {
        if (x == null) {
            return densityFunction2;
        }
        boolean[] blArray = new boolean[]{false};
        try {
            DensityFunction densityFunction3 = densityFunction2.mapAll(densityFunction -> {
                if (x.isInstance(densityFunction)) {
                    try {
                        Object object = y.invoke((Object)densityFunction, new Object[0]);
                        if (object == A || object == B) {
                            blArray[0] = true;
                            return new ColumnCachedNode((DensityFunction)z.invoke((Object)densityFunction, new Object[0]));
                        }
                    }
                    catch (ReflectiveOperationException reflectiveOperationException) {
                        throw new IllegalStateException(reflectiveOperationException);
                    }
                }
                return densityFunction;
            });
            return blArray[0] ? densityFunction3 : densityFunction2;
        }
        catch (RuntimeException runtimeException) {
            return densityFunction2;
        }
    }

    private static double b(DensityFunction densityFunction, int n, int n2, int n3, int n4, int n5) {
        MutableCtx mutableCtx = new MutableCtx(n, 0, n2);
        for (int i = n4; i >= n3; i -= n5) {
            int n6;
            mutableCtx.y = i;
            if (!(densityFunction.compute(mutableCtx) > 0.390625)) continue;
            for (int j = n6 = Math.min(n4, i + n5 - 1); j > i; --j) {
                mutableCtx.y = j;
                if (!(densityFunction.compute(mutableCtx) > 0.390625)) continue;
                return j + 1;
            }
            return i + 1;
        }
        return n3;
    }

    static FastCal estimate(ChunkGenerator chunkGenerator, RandomState randomState, BiomeSource biomeSource, Climate.Sampler sampler, int n2, TerrainSampler terrainSampler, ServerLevel serverLevel) {
        int n3;
        int n6;
        int n7;
        boolean bl;
        double d;
        List<double[]> object3;
        int n8;
        TerrainSampler terrainSampler2;
        int n9 = Boolean.getBoolean("streamsreflowing.exact") ? 2 : (Services.PLATFORM.fastTerrainEstimate() ? Services.PLATFORM.terrainSamplingMode() : 2);
        if (n9 >= 2 || !(chunkGenerator instanceof NoiseBasedChunkGenerator)) {
            return new FastCal(terrainSampler, null, false, 0, null, null);
        }
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = (NoiseBasedChunkGenerator)chunkGenerator;
        double d2 = Services.PLATFORM.gradeWindowAbove();
        double d3 = Services.PLATFORM.gradeWindowBelow();
        NoiseSettings noiseSettings = noiseBasedChunkGenerator.generatorSettings().value().noiseSettings();
        DensityFunction densityFunction = randomState.router().initialDensityWithoutJaggedness();
        int n11 = noiseSettings.minY();
        int n12 = noiseSettings.minY() + noiseSettings.height();
        int n13 = Math.max(1, noiseSettings.getCellHeight());
        DensityFunction densityFunction2 = StreamTerrainCalibration.a(densityFunction);
        ThreadLocal<DensityFunction> threadLocal = densityFunction2 == densityFunction ? null : ThreadLocal.withInitial(() -> StreamTerrainCalibration.a(densityFunction));
        TerrainSampler terrainSampler3 = terrainSampler2 = threadLocal == null ? (n4, n5) -> StreamTerrainCalibration.a(densityFunction, n4, n5, n11, n12, n13) : (n4, n5) -> StreamTerrainCalibration.a((DensityFunction)threadLocal.get(), n4, n5, n11, n12, n13);
        if (Boolean.getBoolean("streamsreflowing.calanalyze")) {
            StreamTerrainCalibration.b(terrainSampler2, terrainSampler, biomeSource, sampler, n2);
        }
        if ((n8 = Integer.getInteger("streamsreflowing.fastcaldata", 0).intValue()) > 0) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of("streamsreflowing-fastcal-data.csv", new String[0]), new OpenOption[0]);){
                bufferedWriter.write("x,z,est,real\n");
                for (int i = -n8; i <= n8; i += 64) {
                    for (int j = -n8; j <= n8; j += 64) {
                        if (biomeSource.getNoiseBiome(QuartPos.fromBlock(j), n2, QuartPos.fromBlock(i), sampler).is(BiomeTags.IS_OCEAN)) continue;
                        bufferedWriter.write(j + "," + i + "," + terrainSampler2.height(j, i) + "," + terrainSampler.height(j, i) + "\n");
                    }
                }
                Constants.LOG.info("[SR-FASTCALDATA] wrote streamsreflowing-fastcal-data.csv");
            }
            catch (IOException iOException) {
                Constants.LOG.warn("fastcaldata dump failed", iOException);
            }
        }
        boolean bl2 = Boolean.getBoolean("streamsreflowing.fastcaldump");
        Path path = null;
        try {
            FastCal fastCal;
            String string = serverLevel.dimension().location().toString().replace(':', '_').replace('/', '_');
            Path path2 = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(string);
            Files.createDirectories(path2, new FileAttribute[0]);
            path = path2.resolve(CALIBRATION_FILENAME);
            if (!bl2 && (fastCal = StreamTerrainCalibration.a(path, terrainSampler, terrainSampler2, n9, d2, d3, chunkGenerator.getSeaLevel())) != null) {
                return fastCal;
            }
        }
        catch (IOException iOException) {
            path = null;
        }
        if (!bl2) {
            BakedBandTable baked = BakedBandTable.lookup(Services.PLATFORM::isModLoaded, Services.PLATFORM::modVersion);
            if (baked != null) {
                boolean[] blArray = baked.safeForWindow(d2, d3, 20);
                int n14 = 0;
                for (boolean bl4 : blArray) {
                    if (bl4 == false) continue;
                    ++n14;
                }
                BandCorrection bandCorrection = new BandCorrection(baked.centers(), baked.bias(), blArray);
                int n15 = StreamTerrainCalibration.a(baked.centers(), blArray, chunkGenerator.getSeaLevel()) ? 1 : 0;
                if ("HYBRID".equals(baked.verdict()) || n15 == 0) {
                    if (q) {
                        Constants.LOG.info("fast terrain estimate: HYBRID (baked '{}' table{})", (Object)baked.name(), (Object)("HYBRID".equals(baked.verdict()) ? "" : "; routing-critical bands unproven"));
                    }
                    return new FastCal(terrainSampler, StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null, false);
                }
                if (q) {
                    Constants.LOG.info("fast terrain estimate: ON (baked '{}' table, {} bands, {} grade-safe for [-{},+{}], mode {})", baked.name(), blArray.length, n14, d3, d2, n9);
                }
                return new FastCal(StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 1.0), StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), false, n9 == 0 ? 2 : 1, bandCorrection, terrainSampler2, true);
            }
        }
        if (Boolean.getBoolean("streamsreflowing.bakedonly")) {
            if (q) {
                Constants.LOG.info("fast terrain estimate: OFF (bakedonly -- no table for this combo, no per-world fit)");
            }
            return new FastCal(terrainSampler, null, false, 0, null, null);
        }
        object3 = new ArrayList<double[]>();
        int n16 = bl2 ? 289 : 800;
        int n17 = bl2 ? 289 : Math.min(200, 384);
        ArrayList<int[]> arrayList = new ArrayList<int[]>();
        for (int i = 0; i < n16 && arrayList.size() < n17; ++i) {
            int n18;
            int n19 = bl2 ? 128 * (i % 17 - 8) : 768 * (i * 5 % 29 - 14);
            int n20 = n18 = bl2 ? 128 * (i / 17 - 8) : 768 * (i * 7 % 23 - 11);
            if (biomeSource.getNoiseBiome(QuartPos.fromBlock(n19), n2, QuartPos.fromBlock(n18), sampler).is(BiomeTags.IS_OCEAN)) continue;
            arrayList.add(new int[]{n19, n18});
        }
        double[][] dArrayArray = new double[arrayList.size()][];
        AtomicLong atomicLong = new AtomicLong();
        AtomicLong atomicLong2 = new AtomicLong();
        LowResGrid.parallelFor(arrayList.size(), n -> {
            int[] nArray = (int[])arrayList.get(n);
            long l = System.nanoTime();
            double d8 = terrainSampler2.height(nArray[0], nArray[1]);
            long l2 = System.nanoTime();
            double d9 = terrainSampler.height(nArray[0], nArray[1]);
            atomicLong.addAndGet(l2 - l);
            atomicLong2.addAndGet(System.nanoTime() - l2);
            dArrayArray[n] = new double[]{d8, d9};
        });
        Collections.addAll(object3, dArrayArray);
        long l = atomicLong.get();
        long l2 = atomicLong2.get();
        double d4 = d = l2 > 0L ? (double)l / (double)l2 : 1.0;
        if (d > 0.7) {
            if (q) {
                Constants.LOG.debug("fast terrain estimate: OFF (estimate too costly: {}% of a real call)", (Object)Math.round(d * 100.0));
            }
            return StreamTerrainCalibration.a(path, d2, d3, "OFF", null, new FastCal(terrainSampler, null, false, 0, null, null));
        }
        boolean bl3 = bl = d <= 0.5;
        if (object3.size() < 100) {
            if (!bl) {
                if (q) {
                    Constants.LOG.debug("fast terrain estimate: OFF (few land points; estimate {}% of a real call, too costly for hybrid)", (Object)Math.round(d * 100.0));
                }
                return StreamTerrainCalibration.a(path, d2, d3, "OFF", null, new FastCal(terrainSampler, null, false, 0, null, null));
            }
            if (q) {
                Constants.LOG.debug("fast terrain estimate: HYBRID (only {} land calibration points found)", (Object)object3.size());
            }
            return StreamTerrainCalibration.a(path, d2, d3, "HYBRID", null, new FastCal(terrainSampler, terrainSampler2, true, 0, null, null));
        }
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        Iterator iterator = object3.iterator();
        while (iterator.hasNext()) {
            double[] dArray = (double[])iterator.next();
            hashMap.merge((int)Math.floor(dArray[0] / 16.0) * 16, 1, Integer::sum);
        }
        int n21 = object3.size();
        int n22 = 0;
        int n23 = bl2 ? 300 : Math.max(0, Math.min(300, 384 - object3.size()));
        ArrayList<int[]> arrayList2 = new ArrayList<int[]>();
        ArrayList<Double> arrayList3 = new ArrayList<Double>();
        for (int i = 0; i < 2000 && n22 < n23; ++i) {
            double d5;
            Integer n24;
            n7 = 768 * (i * 11 % 31 - 15);
            n6 = 768 * (i * 13 % 27 - 13);
            if (biomeSource.getNoiseBiome(QuartPos.fromBlock(n7), n2, QuartPos.fromBlock(n6), sampler).is(BiomeTags.IS_OCEAN) || (n24 = (Integer)hashMap.get((int)Math.floor((d5 = terrainSampler2.height(n7, n6)) / 16.0) * 16)) == null || n24 >= 40) continue;
            arrayList2.add(new int[]{n7, n6});
            arrayList3.add(d5);
            hashMap.merge((int)Math.floor(d5 / 16.0) * 16, 1, Integer::sum);
            ++n22;
        }
        double[] dArray = new double[arrayList2.size()];
        LowResGrid.parallelFor(arrayList2.size(), n -> {
            int[] nArray = (int[])arrayList2.get(n);
            dArray[n] = terrainSampler.height(nArray[0], nArray[1]);
        });
        for (n7 = 0; n7 < arrayList2.size(); ++n7) {
            object3.add(new double[]{(Double)arrayList3.get(n7), dArray[n7]});
        }
        BandCorrection bandCorrection = StreamTerrainCalibration.a(object3, d2, d3);
        n6 = 0;
        for (n3 = 0; n3 < n21; ++n3) {
            double[] dArray3 = object3.get(n3);
            double d6 = (bandCorrection != null ? bandCorrection.correct(dArray3[0]) : dArray3[0]) - dArray3[1];
            if (!(Math.abs(d6) <= 2.0)) continue;
            ++n6;
        }
        n3 = n6 * 100 / n21;
        if (bl2) {
            Constants.LOG.debug("fastcal dump: n={} bands={} corrected within +/-{}: {}%  costRatio={}%", object3.size(), bandCorrection == null ? 0 : bandCorrection.centers().length, 2, n3, Math.round(d * 100.0));
        }
        if (bandCorrection == null || n3 < 80) {
            if (!bl) {
                if (q) {
                    Constants.LOG.debug("fast terrain estimate: OFF (banded correction {}% within +/-{} AND estimate {}% of a real call, too costly for hybrid)", n3, 2, Math.round(d * 100.0));
                }
                return StreamTerrainCalibration.a(path, d2, d3, "OFF", null, new FastCal(terrainSampler, null, false, 0, null, null));
            }
            if (q) {
                Constants.LOG.debug("fast terrain estimate: HYBRID (banded correction {}% of {} points within +/-{}; estimate {}% of a real call)", n3, n21, 2, Math.round(d * 100.0));
            }
            return StreamTerrainCalibration.a(path, d2, d3, "HYBRID", bandCorrection, new FastCal(terrainSampler, bandCorrection == null ? terrainSampler2 : StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null));
        }
        if (!StreamTerrainCalibration.a(bandCorrection.centers(), bandCorrection.safe(), chunkGenerator.getSeaLevel())) {
            if (q) {
                Constants.LOG.debug("fast terrain estimate: HYBRID (banded correction passed {}% but routing-critical bands unproven)", (Object)n3);
            }
            return StreamTerrainCalibration.a(path, d2, d3, "HYBRID", bandCorrection, new FastCal(terrainSampler, StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null));
        }
        if (q) {
            Constants.LOG.debug("fast terrain estimate: ON (banded correction, {} bands, {} grade-safe, {}% of {} points within +/-{})", bandCorrection.centers().length, bandCorrection.safeCount(), n3, n21, 2);
        }
        return StreamTerrainCalibration.a(path, d2, d3, "ON", bandCorrection, new FastCal(StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 1.0), StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), false, n9 == 0 ? 2 : 1, bandCorrection, terrainSampler2));
    }

    private static TerrainSampler a(TerrainSampler terrainSampler, BandCorrection bandCorrection, double d) {
        return (n, n2) -> bandCorrection.correct(terrainSampler.height(n, n2)) - d;
    }

    private static BandCorrection a(List<double[]> list, double d, double d2) {
        TreeMap<Integer, List> treeMap = new TreeMap<Integer, List>();
        for (int i = 0; i < list.size(); i += 2) {
            double[] dArray = list.get(i);
            treeMap.computeIfAbsent((int)Math.floor(dArray[0] / 16.0) * 16, n -> new ArrayList()).add(dArray);
        }
        ArrayList<Double> arrayList = new ArrayList<Double>();
        ArrayList<Double> arrayList2 = new ArrayList<Double>();
        for (Map.Entry entry2 : treeMap.entrySet()) {
            if (((List)entry2.getValue()).size() < 6) continue;
            ArrayList<Double> arrayList3 = new ArrayList<Double>();
            for (double[] dArray : (List<double[]>)entry2.getValue()) {
                arrayList3.add(dArray[0] - dArray[1]);
            }
            Collections.sort(arrayList3);
            double d4 = arrayList3.get(arrayList3.size() / 2);
            double d5 = 0.0;
            for (double d3 = 0.0; d3 <= 2.0; d3 += 1.0) {
                int n2 = 0;
                int n3 = 0;
                for (double d6 : arrayList3) {
                    double d7 = d6 - d4 - d3 - 1.0;
                    if (d7 >= 6.0) continue;
                    ++n2;
                    if (!(d7 > d) && !(d7 < -d2)) continue;
                    ++n3;
                }
                if (n2 <= 0 || !((double)n3 <= (double)n2 * 0.050000000000000044)) continue;
                d5 = d3;
                break;
            }
            arrayList.add((double)((Integer)entry2.getKey()).intValue() + 8.0);
            arrayList2.add(d4 + d5);
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        double[] dArray2 = arrayList.stream().mapToDouble(Double::doubleValue).toArray();
        double[] dArray4 = arrayList2.stream().mapToDouble(Double::doubleValue).toArray();
        BandCorrection bandCorrection = new BandCorrection(dArray2, dArray4, new boolean[dArray2.length]);
        int[] nArray = new int[dArray2.length];
        int[] nArray2 = new int[dArray2.length];
        for (int i = 1; i < list.size(); i += 2) {
            double[] dArray5 = list.get(i);
            double d3 = Math.floor(dArray5[0] / 16.0) * 16.0 + 8.0;
            int n2 = Arrays.binarySearch(dArray2, d3);
            if (n2 < 0) continue;
            double d8 = bandCorrection.correct(dArray5[0]) - 1.0 - dArray5[1];
            if (d8 >= 6.0) continue;
            nArray[n2] = nArray[n2] + 1;
            if (!(d8 > d) && !(d8 < -d2)) continue;
            nArray2[n2] = nArray2[n2] + 1;
        }
        boolean[] blArray = new boolean[dArray2.length];
        for (int i = 0; i < dArray2.length; ++i) {
            blArray[i] = nArray[i] >= 20 && (double)nArray2[i] <= (double)nArray[i] * 0.050000000000000044;
        }
        return new BandCorrection(dArray2, dArray4, blArray);
    }

    private static boolean a(double[] dArray, boolean[] blArray, int n) {
        for (int i = 0; i < blArray.length; ++i) {
            if (!(dArray[i] >= (double)(n - 24)) || !(dArray[i] <= (double)(n + 24)) || blArray[i]) continue;
            return false;
        }
        return true;
    }

    private static FastCal a(Path path, double d, double d2, String string, BandCorrection bandCorrection, FastCal fastCal) {
        block5: {
            if (path != null) {
                try {
                    StringBuilder stringBuilder = new StringBuilder(string).append(' ').append(d).append(' ').append(d2).append('\n');
                    if (bandCorrection != null) {
                        for (int i = 0; i < bandCorrection.centers().length; ++i) {
                            stringBuilder.append(bandCorrection.centers()[i]).append(',').append(bandCorrection.bias()[i]).append(',').append(bandCorrection.safe()[i] ? 1 : 0).append('\n');
                        }
                    }
                    Files.writeString(path, (CharSequence)stringBuilder.toString(), new OpenOption[0]);
                }
                catch (IOException iOException) {
                    if (!q) break block5;
                    Constants.LOG.debug("fast terrain calibration save failed: {}", (Object)iOException.toString());
                }
            }
        }
        return fastCal;
    }

    private static FastCal a(Path path, TerrainSampler terrainSampler, TerrainSampler terrainSampler2, int n, double d, double d2, int n2) {
        try {
            BandCorrection bandCorrection;
            if (!Files.isRegularFile(path, new LinkOption[0])) {
                return null;
            }
            List<String> list = Files.readAllLines(path);
            if (list.isEmpty()) {
                return null;
            }
            String[] stringArray = list.get(0).trim().split(" ");
            String string = stringArray[0];
            if (stringArray.length < 3 || Math.abs(Double.parseDouble(stringArray[1]) - d) > 1.0E-9 || Math.abs(Double.parseDouble(stringArray[2]) - d2) > 1.0E-9) {
                return null;
            }
            ArrayList<Double> arrayList = new ArrayList<Double>();
            ArrayList<Double> arrayList2 = new ArrayList<Double>();
            ArrayList<Boolean> arrayList3 = new ArrayList<Boolean>();
            for (int i = 1; i < list.size(); ++i) {
                String string2 = list.get(i).trim();
                if (string2.isEmpty()) continue;
                String[] stringArray2 = string2.split(",");
                arrayList.add(Double.parseDouble(stringArray2[0]));
                arrayList2.add(Double.parseDouble(stringArray2[1]));
                arrayList3.add("1".equals(stringArray2[2]));
            }
            if (arrayList.isEmpty()) {
                bandCorrection = null;
            } else {
                boolean[] blArray = new boolean[arrayList3.size()];
                for (int i = 0; i < blArray.length; ++i) {
                    blArray[i] = arrayList3.get(i);
                }
                bandCorrection = new BandCorrection(arrayList.stream().mapToDouble(Double::doubleValue).toArray(), arrayList2.stream().mapToDouble(Double::doubleValue).toArray(), blArray);
            }
            switch (string) {
                case "OFF": {
                    if (q) {
                        Constants.LOG.debug("fast terrain estimate: OFF (persisted calibration)");
                    }
                    return new FastCal(terrainSampler, null, false, 0, null, null);
                }
                case "HYBRID": {
                    if (q) {
                        Constants.LOG.debug("fast terrain estimate: HYBRID (persisted calibration)");
                    }
                    return new FastCal(terrainSampler, bandCorrection == null ? terrainSampler2 : StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null);
                }
                case "ON": {
                    if (bandCorrection == null) {
                        return null;
                    }
                    if (!StreamTerrainCalibration.a(bandCorrection.centers(), bandCorrection.safe(), n2)) {
                        if (q) {
                            Constants.LOG.debug("fast terrain estimate: HYBRID (persisted ON calibration; routing-critical bands unproven)");
                        }
                        return new FastCal(terrainSampler, StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null);
                    }
                    if (q) {
                        Constants.LOG.debug("fast terrain estimate: ON (persisted calibration, {} bands, {} grade-safe, mode {})", bandCorrection.centers().length, bandCorrection.safeCount(), n);
                    }
                    return new FastCal(StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 1.0), StreamTerrainCalibration.a(terrainSampler2, bandCorrection, 0.0), false, n == 0 ? 2 : 1, bandCorrection, terrainSampler2);
                }
            }
            return null;
        }
        catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static void b(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, BiomeSource biomeSource, Climate.Sampler sampler, int n3) {
        int n4;
        int n5 = Integer.getInteger("streamsreflowing.calanalyze.n", 800);
        int n6 = Integer.getInteger("streamsreflowing.calanalyze.spread", 512);
        int n7 = Math.max(6000, n5 * 8);
        ArrayList<int[]> arrayList = new ArrayList<int[]>();
        for (n4 = 0; n4 < n7 && arrayList.size() < n5; ++n4) {
            int n8 = n4 * 13 % 79 - 39;
            int n9 = n4 * 29 % 83 - 41;
            int n10 = n8 * n6;
            int n11 = n9 * n6;
            if (biomeSource.getNoiseBiome(QuartPos.fromBlock(n10), n3, QuartPos.fromBlock(n11), sampler).is(BiomeTags.IS_OCEAN)) continue;
            arrayList.add(new int[]{n10, n11});
        }
        n4 = arrayList.size();
        double[] dArray = new double[n4];
        double[] dArray2 = new double[n4];
        String[] stringArray = new String[n4];
        double[][] dArrayArray = new double[n4][];
        LowResGrid.parallelFor(n4, n2 -> {
            int[] nArray = (int[])arrayList.get(n2);
            dArray[n2] = terrainSampler.height(nArray[0], nArray[1]);
            dArray2[n2] = terrainSampler2.height(nArray[0], nArray[1]);
            Holder<Biome> holder = biomeSource.getNoiseBiome(QuartPos.fromBlock(nArray[0]), n3, QuartPos.fromBlock(nArray[1]), sampler);
            stringArray[n2] = holder.unwrapKey().map(resourceKey -> resourceKey.location().toString()).orElse("?");
            dArrayArray[n2] = StreamTerrainCalibration.a(sampler, nArray[0], nArray[1]);
        });
        StringBuilder stringBuilder = new StringBuilder("x,z,est,real,err,biome,continentalness,erosion,weirdness,temperature,humidity\n");
        for (int i = 0; i < n4; ++i) {
            int[] nArray = (int[])arrayList.get(i);
            stringBuilder.append(nArray[0]).append(',').append(nArray[1]).append(',').append(dArray[i]).append(',').append(dArray2[i]).append(',').append(dArray[i] - dArray2[i]).append(',').append(stringArray[i]);
            for (double d : dArrayArray[i]) {
                stringBuilder.append(',').append(String.format(Locale.ROOT, "%.4f", d));
            }
            stringBuilder.append('\n');
        }
        try {
            Files.writeString(Path.of("streamsreflowing-calanalysis.csv", new String[0]), (CharSequence)stringBuilder, new OpenOption[0]);
        }
        catch (IOException iOException) {
            Constants.LOG.info("[SR-CAL] could not write analysis csv: {}", iOException);
        }
        double[] dArray3 = new double[n4];
        for (int i = 0; i < n4; ++i) {
            dArray3[i] = dArray[i] - dArray2[i];
        }
        Constants.LOG.info("[SR-CAL] calibration analysis: {} land columns, raw error sigma {} (mean {})", n4, StreamTerrainCalibration.b(StreamTerrainCalibration.b(dArray3)), StreamTerrainCalibration.b(StreamTerrainCalibration.a(dArray3)));
        String[] stringArray2 = new String[]{"elevation/16 (SHIPPED)", "biome", "continentalness/0.05", "erosion/0.05", "weirdness/0.05", "temperature/0.05", "humidity/0.05", "elevation/16 x biome"};
        List<IntFunction<String>> object = List.of(n -> "e" + Math.floorDiv((int)dArray2[n], 16), n -> stringArray[n], n -> "c" + Math.round(dArrayArray[n][0] / 0.05), n -> "r" + Math.round(dArrayArray[n][1] / 0.05), n -> "w" + Math.round(dArrayArray[n][2] / 0.05), n -> "t" + Math.round(dArrayArray[n][3] / 0.05), n -> "h" + Math.round(dArrayArray[n][4] / 0.05), n -> "e" + Math.floorDiv((int)dArray2[n], 16) + "|" + stringArray[n]);
        for (int i = 0; i < stringArray2.length; ++i) {
            int n12;
            IntFunction<String> intFunction = object.get(i);
            LinkedHashMap<String, List> linkedHashMap = new LinkedHashMap<String, List>();
            for (int j = 0; j < n4; ++j) {
                linkedHashMap.computeIfAbsent((String)intFunction.apply(j), string -> new ArrayList()).add(j);
            }
            ArrayList<Double> arrayList2 = new ArrayList<Double>();
            ArrayList arrayList3 = new ArrayList();
            int n13 = 0;
            for (List list : linkedHashMap.values()) {
                int n14;
                if (list.size() < 6) {
                    arrayList3.addAll(list);
                    continue;
                }
                ++n13;
                double d = 0.0;
                Iterator iterator = list.iterator();
                while (iterator.hasNext()) {
                    n14 = (Integer)iterator.next();
                    d += dArray3[n14];
                }
                d /= (double)list.size();
                iterator = list.iterator();
                while (iterator.hasNext()) {
                    n14 = (Integer)iterator.next();
                    arrayList2.add(dArray3[n14] - d);
                }
            }
            if (!arrayList3.isEmpty()) {
                int n15;
                double d = 0.0;
                Iterator iterator = arrayList3.iterator();
                while (iterator.hasNext()) {
                    n15 = (Integer)iterator.next();
                    d += dArray3[n15];
                }
                d /= (double)arrayList3.size();
                iterator = arrayList3.iterator();
                while (iterator.hasNext()) {
                    n15 = (Integer)iterator.next();
                    arrayList2.add(dArray3[n15] - d);
                }
            }
            double[] dArray4 = new double[arrayList2.size()];
            for (n12 = 0; n12 < dArray4.length; ++n12) {
                dArray4[n12] = arrayList2.get(n12);
            }
            n12 = 0;
            for (double d5 : dArray4) {
                if (!(Math.abs(d5) <= 2.0)) continue;
                ++n12;
            }
            Constants.LOG.info("[SR-CAL]   {}: residual sigma {}, {}% within +-2 ({} usable buckets, {} sparse pts)", stringArray2[i], StreamTerrainCalibration.b(StreamTerrainCalibration.b(dArray4)), Math.round(100.0 * (double)n12 / (double)Math.max(1, dArray4.length)), n13, arrayList3.size());
        }
        Constants.LOG.info("[SR-CAL] lowest residual sigma = the variable the correction should key on; wrote streamsreflowing-calanalysis.csv");
    }

    private static double[] a(Climate.Sampler sampler, int n, int n2) {
        double[] dArray = new double[5];
        try {
            Climate.TargetPoint targetPoint = sampler.sample(QuartPos.fromBlock(n), 0, QuartPos.fromBlock(n2));
            String[] stringArray = new String[]{"continentalness", "erosion", "weirdness", "temperature", "humidity"};
            for (int i = 0; i < stringArray.length; ++i) {
                Object object = targetPoint.getClass().getMethod(stringArray[i], new Class[0]).invoke((Object)targetPoint, new Object[0]);
                dArray[i] = ((Number)object).doubleValue() / 10000.0;
            }
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            Arrays.fill(dArray, Double.NaN);
        }
        return dArray;
    }

    private static double a(double[] dArray) {
        double d = 0.0;
        for (double d2 : dArray) {
            d += d2;
        }
        return dArray.length == 0 ? 0.0 : d / (double)dArray.length;
    }

    private static double b(double[] dArray) {
        if (dArray.length < 2) {
            return 0.0;
        }
        double d = StreamTerrainCalibration.a(dArray);
        double d2 = 0.0;
        for (double d3 : dArray) {
            d2 += (d3 - d) * (d3 - d);
        }
        return Math.sqrt(d2 / (double)(dArray.length - 1));
    }

    private static String b(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    record FastCal(TerrainSampler land, TerrainSampler estimator, boolean hybridGrid, int gradePolicy, BandCorrection corr, TerrainSampler rawEst, boolean baked) {
        FastCal(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, boolean bl, int n, BandCorrection bandCorrection, TerrainSampler terrainSampler3) {
            this(terrainSampler, terrainSampler2, bl, n, bandCorrection, terrainSampler3, false);
        }
    }

    record BandCorrection(double[] centers, double[] bias, boolean[] safe) {
        private int bandOf(double d) {
            int n = 0;
            int n2 = this.centers.length;
            while (n < n2) {
                int n3 = n + n2 >>> 1;
                if (this.centers[n3] < d) {
                    n = n3 + 1;
                    continue;
                }
                n2 = n3;
            }
            return n;
        }

        double correct(double d) {
            if (this.centers.length == 1 || d <= this.centers[0]) {
                return d - this.bias[0];
            }
            if (d >= this.centers[this.centers.length - 1]) {
                return d - this.bias[this.bias.length - 1];
            }
            int n = this.bandOf(d);
            if (n < 1) {
                n = 1;
            }
            if (n > this.centers.length - 1) {
                n = this.centers.length - 1;
            }
            double d2 = (d - this.centers[n - 1]) / (this.centers[n] - this.centers[n - 1]);
            return d - (this.bias[n - 1] + d2 * (this.bias[n] - this.bias[n - 1]));
        }

        boolean gradeSafe(double d) {
            int n;
            if (d < this.centers[0] - 16.0 || d > this.centers[this.centers.length - 1] + 16.0) {
                return false;
            }
            if (this.centers.length == 1) {
                return this.safe[0];
            }
            n = this.bandOf(d);
            if (n < 1) {
                n = 1;
            }
            boolean bl = this.safe[Math.max(0, n - 1)];
            boolean bl2 = this.safe[Math.min(this.safe.length - 1, n)];
            return bl && bl2;
        }

        int safeCount() {
            int n = 0;
            for (boolean bl : this.safe) {
                if (!bl) continue;
                ++n;
            }
            return n;
        }
    }

    static final class MutableCtx
    implements DensityFunction.FunctionContext {
        int x;
        int y;
        int z;

        MutableCtx(int n, int n2, int n3) {
            this.x = n;
            this.y = n2;
            this.z = n3;
        }

        @Override
        public int blockX() {
            return this.x;
        }

        @Override
        public int blockY() {
            return this.y;
        }

        @Override
        public int blockZ() {
            return this.z;
        }
    }

    private static final class ColumnCachedNode
    implements DensityFunction.SimpleFunction {
        private final DensityFunction a;
        private long b = Long.MIN_VALUE;
        private double c;

        ColumnCachedNode(DensityFunction densityFunction) {
            this.a = densityFunction;
        }

        @Override
        public double compute(DensityFunction.FunctionContext functionContext) {
            long l = (long)functionContext.blockX() << 32 ^ (long)functionContext.blockZ() & 0xFFFFFFFFL;
            if (l != this.b) {
                this.c = this.a.compute(functionContext);
                this.b = l;
            }
            return this.c;
        }

        @Override
        public double minValue() {
            return this.a.minValue();
        }

        @Override
        public double maxValue() {
            return this.a.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.a.codec();
        }
    }
}
