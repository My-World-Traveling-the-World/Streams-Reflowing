/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.BakedBandTable;
import dev.streamsreflowing.worldgen.ChunkPlanCache;
import dev.streamsreflowing.worldgen.FileNetworkStore;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamBiomeBridge;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamRiverFlowFeature;
import dev.streamsreflowing.worldgen.StreamSnowCleanupFeature;
import dev.streamsreflowing.worldgen.StreamVegetationFeature;
import dev.streamsreflowing.worldgen.WaterSkinFeature;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FieldFormBuilder;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import dev.streamsreflowing.worldgen.regionprep.HeightAccuracy;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;

public final class StreamsWorldgen {
    public static final TagKey<EntityType<?>> IGNORES_CURRENT;
    public static final ResourceLocation RIVER_CARVER_ID;
    public static final Feature<NoneFeatureConfiguration> RIVER_CARVER;
    public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_CONFIGURED;
    public static final ResourceKey<PlacedFeature> RIVER_PLACED;
    public static final ResourceLocation STREAM_VEGETATION_ID;
    public static final Feature<NoneFeatureConfiguration> STREAM_VEGETATION;
    public static final ResourceKey<ConfiguredFeature<?, ?>> STREAM_VEGETATION_CONFIGURED;
    public static final ResourceKey<PlacedFeature> STREAM_VEGETATION_PLACED;
    public static final TagKey<PlacedFeature> BIOME_BOOST_EXCLUDED;
    public static final TagKey<Biome> NO_BANK_GRASS;
    public static final TagKey<Block> UNDERWATER_VEGETATION_FLOOR;
    public static final ResourceLocation SNOW_CLEANUP_ID;
    public static final Feature<NoneFeatureConfiguration> SNOW_CLEANUP;
    public static final ResourceKey<ConfiguredFeature<?, ?>> SNOW_CLEANUP_CONFIGURED;
    public static final ResourceKey<PlacedFeature> SNOW_CLEANUP_PLACED;
    public static final ResourceLocation RIVER_FLOW_ID;
    public static final Feature<NoneFeatureConfiguration> RIVER_FLOW;
    public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_FLOW_CONFIGURED;
    public static final ResourceKey<PlacedFeature> RIVER_FLOW_PLACED;
    public static final ResourceLocation WATER_SKIN_ID;
    public static final Feature<NoneFeatureConfiguration> WATER_SKIN;
    public static final ResourceKey<ConfiguredFeature<?, ?>> WATER_SKIN_CONFIGURED;
    public static final ResourceKey<PlacedFeature> WATER_SKIN_PLACED;
    private static final Map<ServerLevel, RiverEngine> a;
    private static final double b = 6.0;
    private static final int c = 65536;
    private static volatile RiverEngine d;
    private static final double e = 0.390625;
    private static final int f = 200;
    private static final int g = 800;
    private static final int h = 768;
    private static final int i = 2;
    private static final int j = 80;
    private static final int k = 16;
    private static final int l = 6;
    private static final String m = "fastcal-v10.txt";
    private static final double n = 0.95;
    private static final double o = 6.0;
    private static final int p = 20;
    private static final boolean q;
    private static final int r = 40;
    private static final int s = 2000;
    private static final int t = 300;
    private static final int u = 384;
    private static final double v = 0.7;
    private static final double w = 0.5;
    private static final Class<?> x;
    private static final Method y;
    private static final Method z;
    private static final Object A;
    private static final Object B;
    private static final double C = 1.1;
    private static final int D = 120;
    private static final int E;
    private static final int F = 6;

    private StreamsWorldgen() {
    }

    private static boolean a(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, BiomeSource biomeSource, Climate.Sampler sampler, int n) {
        long l = 0L;
        long l2 = 0L;
        int n2 = 0;
        for (int i = 0; i < 200 && n2 < 16; ++i) {
            int n3 = 768 * (i * 5 % 29 - 14);
            int n4 = 768 * (i * 7 % 23 - 11);
            if (biomeSource.getNoiseBiome(QuartPos.fromBlock(n3), n, QuartPos.fromBlock(n4), sampler).is(BiomeTags.IS_OCEAN)) continue;
            long l3 = System.nanoTime();
            terrainSampler.height(n3, n4);
            long l4 = System.nanoTime();
            terrainSampler2.height(n3, n4);
            l += l4 - l3;
            l2 += System.nanoTime() - l4;
            ++n2;
        }
        return l2 > 0L && n2 >= 8 && (double)l / (double)l2 > 0.7;
    }

    private static double a(DensityFunction densityFunction, int n, int n2, int n3, int n4, int n5) {
        int n6;
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(n, n4, n2);
        if (densityFunction.compute(singlePointContext) > 0.390625) {
            return n4 + 1;
        }
        int n7 = n3;
        int n8 = n4;
        while (n8 - n7 > 1) {
            n6 = n7 + n8 >>> 1;
            DensityFunction.SinglePointContext singlePointContext2 = new DensityFunction.SinglePointContext(n, n6, n2);
            if (densityFunction.compute(singlePointContext2) > 0.390625) {
                n7 = n6;
                continue;
            }
            n8 = n6;
        }
        n6 = Math.min(n4, n8 + n5);
        DensityFunction.SinglePointContext singlePointContext3 = new DensityFunction.SinglePointContext(n, n6, n2);
        if (densityFunction.compute(singlePointContext3) > 0.390625) {
            return StreamsWorldgen.b(densityFunction, n, n2, n3, n4, n5);
        }
        return n7 + 1;
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
        for (int i = n4; i >= n3; i -= n5) {
            int n6;
            DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(n, i, n2);
            if (!(densityFunction.compute(singlePointContext) > 0.390625)) continue;
            for (int j = n6 = Math.min(n4, i + n5 - 1); j > i; --j) {
                DensityFunction.SinglePointContext singlePointContext2 = new DensityFunction.SinglePointContext(n, j, n2);
                if (!(densityFunction.compute(singlePointContext2) > 0.390625)) continue;
                return j + 1;
            }
            return i + 1;
        }
        return n3;
    }

    private static TerrainSampler a(TerrainSampler terrainSampler, ChunkGenerator chunkGenerator, RandomState randomState, ServerLevel serverLevel, long l) {
        if (Boolean.getBoolean("streamsreflowing.noformgrades") || !(chunkGenerator instanceof NoiseBasedChunkGenerator)) {
            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                Constants.LOG.info("[SR-PREP] grade sampling not attempted: generator is {} (need a noise generator){}", (Object)chunkGenerator.getClass().getName(), (Object)(Boolean.getBoolean("streamsreflowing.noformgrades") ? ", and the kill switch is on" : ""));
            }
            return terrainSampler;
        }
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = (NoiseBasedChunkGenerator)chunkGenerator;
        try {
            NoiseSettings noiseSettings = noiseBasedChunkGenerator.generatorSettings().value().noiseSettings().clampToHeightAccessor(serverLevel);
            int n = noiseSettings.minY();
            int n2 = noiseSettings.minY() + noiseSettings.height();
            int n3 = Math.max(1, noiseSettings.getCellHeight());
            FieldForm fieldForm = FieldFormBuilder.build(serverLevel.registryAccess(), l, noiseBasedChunkGenerator.generatorSettings().value().useLegacyRandomSource(), randomState.router().finalDensity(), n, n2, n3);
            FormSampler formSampler = new FormSampler(terrainSampler, fieldForm, n, n2, n3, Math.max(1, noiseSettings.getCellWidth()), 0.0, noiseBasedChunkGenerator.getSeaLevel());
            RiverFlowField.BiomeClass biomeClass = RiverFlowFields.classifierFor(serverLevel);
            if (!StreamsWorldgen.a(terrainSampler, formSampler, randomState.router().finalDensity(), biomeClass)) {
                StreamsWorldgen.a(terrainSampler, formSampler, biomeClass);
                return terrainSampler;
            }
            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                StreamsWorldgen.a(terrainSampler, formSampler);
                Constants.LOG.info("[SR-PREP] grade sampling: accelerated path in use for {}", (Object)serverLevel.dimension().location());
            }
            StreamsWorldgen.a((TerrainSampler)formSampler, biomeClass);
            StreamsWorldgen.a(terrainSampler, formSampler, biomeClass);
            Constants.LOG.info("Streams Reflowing: stream preparation optimised for {}.", (Object)serverLevel.dimension().location());
            return formSampler;
        }
        catch (RuntimeException runtimeException) {
            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                Constants.LOG.info("[SR-PREP] IR grades declined (full stack):", runtimeException);
            }
            return terrainSampler;
        }
    }

    private static void a(TerrainSampler terrainSampler, RiverFlowField.BiomeClass biomeClass) {
        if (!Boolean.getBoolean("streamsreflowing.prepdiag") || biomeClass == null) {
            return;
        }
        try {
            int n = 15;
            int n2 = 211;
            int n3 = 100000;
            long l = System.nanoTime();
            int n4 = 0;
            for (int i = 0; i < 15; ++i) {
                for (int j = 0; j < 15; ++j) {
                    biomeClass.classify(100000 + i * 211, 100000 + j * 211);
                    ++n4;
                }
            }
            l = System.nanoTime() - l;
            long l2 = System.nanoTime();
            for (int i = 0; i < 15; ++i) {
                for (int j = 0; j < 15; ++j) {
                    biomeClass.classify(100000 + i * 211, 100000 + j * 211);
                }
            }
            l2 = System.nanoTime() - l2;
            long l3 = System.nanoTime();
            for (int i = 0; i < 15; ++i) {
                for (int j = 0; j < 15; ++j) {
                    terrainSampler.height(100000 + i * 211, 100000 + j * 211);
                }
            }
            l3 = System.nanoTime() - l3;
            double d = (double)l / (double)n4;
            double d2 = (double)l2 / (double)n4;
            double d3 = (double)l3 / (double)n4;
            Constants.LOG.info("[SR-PREP] cost per column: classify cold {}us, warm {}us; height {}us -> height is {}x a cold classify, {}x a warm one", StreamsWorldgen.a(d), StreamsWorldgen.a(d2), StreamsWorldgen.a(d3), String.format(Locale.ROOT, "%.0f", d > 0.0 ? d3 / d : 0.0), String.format(Locale.ROOT, "%.0f", d2 > 0.0 ? d3 / d2 : 0.0));
        }
        catch (RuntimeException runtimeException) {
            Constants.LOG.info("[SR-PREP] cost probe failed:", runtimeException);
        }
    }

    private static String a(double d) {
        return String.format(Locale.ROOT, "%.2f", d / 1000.0);
    }

    private static void a(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, RiverFlowField.BiomeClass biomeClass) {
        int n = Integer.getInteger("streamsreflowing.iraudit", 0);
        if (n <= 0) {
            return;
        }
        int n2 = 0;
        int n3 = Math.max(3, (int)Math.round(Math.sqrt((double)n / 2.0)));
        int n4 = n3 / 2;
        int[] nArray = new int[4];
        int n5 = 0;
        int n6 = 0;
        double d = 0.0;
        double d2 = 0.0;
        StringBuilder stringBuilder = new StringBuilder();
        long l = System.nanoTime();
        for (int i = 0; i < 2; ++i) {
            int n7 = i == 0 ? 211 : 512;
            for (int j = -n4; j <= n4; ++j) {
                for (int k = -n4; k <= n4; ++k) {
                    double d3;
                    int n8 = j * n7;
                    int n9 = k * n7;
                    if (biomeClass != null && biomeClass.classify(n8, n9) == 2) {
                        ++n2;
                        continue;
                    }
                    try {
                        d3 = terrainSampler2.height(n8, n9) - terrainSampler.height(n8, n9);
                    }
                    catch (RuntimeException runtimeException) {
                        continue;
                    }
                    if (!Double.isFinite(d3)) continue;
                    ++n6;
                    d = Math.max(d, d3);
                    d2 = Math.min(d2, d3);
                    double d4 = Math.abs(d3);
                    int n10 = d4 == 0.0 ? 0 : (d4 <= 3.0 ? 1 : (d4 <= 6.0 ? 2 : 3));
                    nArray[n10] = nArray[n10] + 1;
                    if (HeightAccuracy.inWindow(d3)) continue;
                    ++n5;
                    if (stringBuilder.length() >= 700) continue;
                    stringBuilder.append(String.format(Locale.ROOT, "%n    (%d,%d) d=%.1f", n8, n9, d3));
                }
            }
        }
        Constants.LOG.info("[SR-AUDIT] {} land columns in {}s ({} ocean skipped): {} outside the window (+{}/-{}), highest {}, lowest {} | exact {}, 1-3 {}, 4-6 {}, 7+ {}{}", n6, String.format(Locale.ROOT, "%.1f", (double)(System.nanoTime() - l) / 1.0E9), n2, n5, 3.0, 6.0, d, d2, nArray[0], nArray[1], nArray[2], nArray[3], n5 == 0 ? "" : stringBuilder);
    }

    private static boolean a(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, DensityFunction densityFunction, RiverFlowField.BiomeClass biomeClass) {
        boolean bl;
        FormSampler formSampler;
        FormSampler formSampler2;
        FormSampler formSampler3 = formSampler2 = terrainSampler2 instanceof FormSampler ? (formSampler = (FormSampler)terrainSampler2) : null;
        if (formSampler2 != null) {
            formSampler2.setAquiferHandBack(false);
        }
        if (!(bl = StreamsWorldgen.a(terrainSampler, terrainSampler2, densityFunction, formSampler2 != null, biomeClass)) && formSampler2 != null) {
            Constants.LOG.info("[SR-PREP] arming the aquifer hand-back and re-judging");
            formSampler2.setAquiferHandBack(true);
            bl = StreamsWorldgen.a(terrainSampler, terrainSampler2, densityFunction, false, biomeClass);
            if (!bl) {
                formSampler2.setAquiferHandBack(false);
            }
        }
        return bl;
    }

    private static boolean a(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, DensityFunction densityFunction, boolean bl, RiverFlowField.BiomeClass biomeClass) {
        double d;
        int n;
        int n2 = 0;
        double d2 = 0.0;
        double d3 = 0.0;
        int n3 = 0;
        int n4 = 0;
        double d4 = 0.0;
        double d5 = 0.0;
        double d6 = 0.0;
        int n5 = 0;
        int n6 = 0;
        long l = 0L;
        long l2 = 0L;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            for (n = 0; n < 2; ++n) {
                int n7 = n == 0 ? 211 : 512;
                for (int i = -7; i <= 7; ++i) {
                    for (int j = -7; j <= 7; ++j) {
                        double d7;
                        int n8 = i * n7;
                        int n9 = j * n7;
                        if (biomeClass != null && biomeClass.classify(n8, n9) == 2) {
                            ++n2;
                            continue;
                        }
                        long l3 = System.nanoTime();
                        double d8 = terrainSampler2.height(n8, n9);
                        long l4 = System.nanoTime();
                        double d9 = terrainSampler.height(n8, n9);
                        long l5 = System.nanoTime();
                        if (n == 1) {
                            l += l4 - l3;
                            l2 += l5 - l4;
                        }
                        if (!Double.isFinite(d7 = d8 - d9)) {
                            return false;
                        }
                        ++n6;
                        d2 = Math.max(d2, d7);
                        d3 = Math.min(d3, d7);
                        if (!HeightAccuracy.inWindow(d7)) {
                            ++n5;
                            if (stringBuilder.length() < 900) {
                                stringBuilder.append(String.format(Locale.ROOT, "%n    (%d,%d) ours=%.1f vanilla=%.1f d=%.1f", n8, n9, d8, d9, d7));
                            }
                        }
                        if (!(Math.abs(d7) > d6)) continue;
                        d6 = Math.abs(d7);
                        n3 = n8;
                        n4 = n9;
                        d4 = d8;
                        d5 = d9;
                    }
                }
            }
        }
        catch (RuntimeException runtimeException) {
            return false;
        }
        if (n6 < 120) {
            Constants.LOG.info("[SR-PREP] only {} land columns in {} sampled ({} ocean, answered by the fast path) -- too few to judge on, staying on host terrain", n6, n6 + n2, n2);
            return false;
        }
        if (Boolean.getBoolean("streamsreflowing.prepdiag") && n2 > 0) {
            Constants.LOG.info("[SR-PREP] judged {} land columns; {} ocean columns skipped (the fast path answers those before this sampler is asked)", (Object)n6, (Object)n2);
        }
        int n10 = n = n5 == 0 ? 1 : 0;
        if (n == 0 && bl) {
            return false;
        }
        if (n == 0 && Boolean.getBoolean("streamsreflowing.prepdiag")) {
            Constants.LOG.info("[SR-PREP] IR grades REFUSED: {}; worst at ({},{}) ours={} vanilla={}", HeightAccuracy.describe(n5, n6, d2, d3), n3, n4, d4, d5);
            Constants.LOG.info("[SR-PREP] offending columns:{}", (Object)stringBuilder);
            StreamsWorldgen.a(terrainSampler2, densityFunction, n3, n4, d4, d5);
        }
        if (n == 0) {
            return false;
        }
        double d10 = d = l > 0L ? (double)l2 / (double)l : 0.0;
        if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            long l6 = FormSampler.handedBack.sum();
            long l7 = FormSampler.keptIr.sum();
            Constants.LOG.info("[SR-PREP] aquifer hand-back {} of {} sampled columns ({}%)", l6, l6 + l7, String.format(Locale.ROOT, "%.1f", l6 + l7 == 0L ? 0.0 : 100.0 * (double)l6 / (double)(l6 + l7)));
        }
        if (d < 1.1) {
            Constants.LOG.info("[SR-PREP] grade sampling: host terrain is already fast here ({}x), staying on it", (Object)String.format(Locale.ROOT, "%.2f", d));
            return false;
        }
        if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            Constants.LOG.info("[SR-PREP] grade speed: {}x over host terrain on the timed lattice", (Object)String.format(Locale.ROOT, "%.2f", d));
        }
        return true;
    }

    private static void a(TerrainSampler terrainSampler, DensityFunction densityFunction, int n, int n2, double d, double d2) {
        FormSampler formSampler;
        block8: {
            block7: {
                if (!(terrainSampler instanceof FormSampler)) break block7;
                formSampler = (FormSampler)terrainSampler;
                if (densityFunction != null) break block8;
            }
            return;
        }
        try {
            Constants.LOG.info("[SR-PREP] walkCheck ({},{}) ours={} vanilla={} | both={} pinOnly={} interpOnly={} neither={}", n, n2, d, d2, formSampler.heightWithSemantics(n, n2, true, true), formSampler.heightWithSemantics(n, n2, true, false), formSampler.heightWithSemantics(n, n2, false, true), formSampler.heightWithSemantics(n, n2, false, false));
            int n3 = (int)Math.min(d, d2) - 2;
            int n4 = (int)Math.max(d, d2) + 2;
            double d3 = 0.0;
            int n5 = Integer.MIN_VALUE;
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = n4; i >= n3; --i) {
                double d4;
                double d5 = formSampler.density(n, i, n2);
                double d6 = Math.abs(d5 - (d4 = densityFunction.compute(new DensityFunction.SinglePointContext(n, i, n2))));
                if (d6 > d3) {
                    d3 = d6;
                    n5 = i;
                }
                stringBuilder.append(String.format(Locale.ROOT, "%n    y=%d ours=%+.6f real=%+.6f d=%.3e%s", i, d5, d4, d6, d4 > formSampler.solidThreshold() ? "  <- real says SOLID" : ""));
            }
            Constants.LOG.info("[SR-PREP] walkCheck density band [{}..{}] worst d={} at y={} -> {}{}", n3, n4, String.format(Locale.ROOT, "%.3e", d3), n5, d3 < 1.0E-9 ? "densities AGREE, so the disagreement is in the column WALK" : "density DIVERGES, so the disagreement is in the TRANSLATION", stringBuilder);
        }
        catch (RuntimeException runtimeException) {
            Constants.LOG.info("[SR-PREP] walkCheck failed:", runtimeException);
        }
    }

    private static void a(TerrainSampler terrainSampler, TerrainSampler terrainSampler2) {
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                terrainSampler.height(i * 512, j * 512);
                terrainSampler2.height(i * 512, j * 512);
            }
        }
        long l = 0L;
        long l2 = 0L;
        double d = 0.0;
        int n = 0;
        int n2 = 0;
        double d2 = 0.0;
        double d3 = 0.0;
        int n3 = 0;
        for (int i = -7; i <= 7; ++i) {
            for (int j = -7; j <= 7; ++j) {
                int n4 = i * 211;
                int n5 = j * 211;
                long l3 = System.nanoTime();
                double d4 = terrainSampler.height(n4, n5);
                long l4 = System.nanoTime();
                double d5 = terrainSampler2.height(n4, n5);
                long l5 = System.nanoTime();
                l += l4 - l3;
                l2 += l5 - l4;
                if (Math.abs(d4 - d5) > d) {
                    d = Math.abs(d4 - d5);
                    n = n4;
                    n2 = n5;
                    d2 = d5;
                    d3 = d4;
                }
                ++n3;
            }
        }
        Constants.LOG.info("[SR-PREP] IR-grades benchmark, {} columns: vanilla {} ms, FormEval {} ms ({}x faster), maxDiff={} blocks at ({},{}) ours={} real={}", n3, String.format(Locale.ROOT, "%.1f", (double)l / 1000000.0), String.format(Locale.ROOT, "%.1f", (double)l2 / 1000000.0), String.format(Locale.ROOT, "%.2f", (double)l / (double)Math.max(1L, l2)), d, n, n2, d2, d3);
    }

    private static FastCal a(ChunkGenerator chunkGenerator, RandomState randomState, BiomeSource biomeSource, Climate.Sampler sampler, int n2, TerrainSampler terrainSampler, ServerLevel serverLevel) {
        Object object;
        int n3;
        int n6;
        int n7;
        boolean bl;
        double d;
        Object object2;
        Object object3;
        int n8;
        TerrainSampler terrainSampler2;
        int n9;
        int n10 = Boolean.getBoolean("streamsreflowing.exact") ? 2 : (n9 = Services.PLATFORM.fastTerrainEstimate() ? Services.PLATFORM.terrainSamplingMode() : 2);
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
        DensityFunction densityFunction2 = StreamsWorldgen.a(densityFunction);
        ThreadLocal<DensityFunction> threadLocal = densityFunction2 == densityFunction ? null : ThreadLocal.withInitial(() -> StreamsWorldgen.a(densityFunction));
        TerrainSampler terrainSampler3 = terrainSampler2 = threadLocal == null ? (n4, n5) -> StreamsWorldgen.a(densityFunction, n4, n5, n11, n12, n13) : (n4, n5) -> StreamsWorldgen.a((DensityFunction)threadLocal.get(), n4, n5, n11, n12, n13);
        if (Boolean.getBoolean("streamsreflowing.calanalyze")) {
            StreamsWorldgen.b(terrainSampler2, terrainSampler, biomeSource, sampler, n2);
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
            object3 = serverLevel.dimension().location().toString().replace(':', '_').replace('/', '_');
            object2 = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve((String)object3);
            Files.createDirectories((Path)object2, new FileAttribute[0]);
            path = object2.resolve(m);
            if (!bl2 && (fastCal = StreamsWorldgen.a(path, terrainSampler, terrainSampler2, n9, d2, d3, chunkGenerator.getSeaLevel())) != null) {
                return fastCal;
            }
        }
        catch (IOException iOException) {
            path = null;
        }
        if (!bl2) {
            object3 = BakedBandTable.lookup(Services.PLATFORM::isModLoaded, Services.PLATFORM::modVersion);
            if (object3 != null) {
                object2 = ((BakedBandTable)object3).safeForWindow(d2, d3, 20);
                int n14 = 0;
                for (Object object4 : object2) {
                    if (object4 == false) continue;
                    ++n14;
                }
                BandCorrection bandCorrection = new BandCorrection(((BakedBandTable)object3).centers(), ((BakedBandTable)object3).bias(), (boolean[])object2);
                int n15 = StreamsWorldgen.a(((BakedBandTable)object3).centers(), (boolean[])object2, chunkGenerator.getSeaLevel()) ? 1 : 0;
                if ("HYBRID".equals(((BakedBandTable)object3).verdict()) || n15 == 0) {
                    if (q) {
                        Constants.LOG.info("fast terrain estimate: HYBRID (baked '{}' table{})", (Object)((BakedBandTable)object3).name(), (Object)("HYBRID".equals(((BakedBandTable)object3).verdict()) ? "" : "; routing-critical bands unproven"));
                    }
                    return new FastCal(terrainSampler, StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null, false);
                }
                if (q) {
                    Constants.LOG.info("fast terrain estimate: ON (baked '{}' table, {} bands, {} grade-safe for [-{},+{}], mode {})", ((BakedBandTable)object3).name(), ((Object)object2).length, n14, d3, d2, n9);
                }
                return new FastCal(StreamsWorldgen.a(terrainSampler2, bandCorrection, 1.0), StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), false, n9 == 0 ? 2 : 1, bandCorrection, terrainSampler2, true);
            }
        }
        if (Boolean.getBoolean("streamsreflowing.bakedonly")) {
            if (q) {
                Constants.LOG.info("fast terrain estimate: OFF (bakedonly -- no table for this combo, no per-world fit)");
            }
            return new FastCal(terrainSampler, null, false, 0, null, null);
        }
        object3 = new ArrayList();
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
            double d = terrainSampler2.height(nArray[0], nArray[1]);
            long l2 = System.nanoTime();
            double d2 = terrainSampler.height(nArray[0], nArray[1]);
            atomicLong.addAndGet(l2 - l);
            atomicLong2.addAndGet(System.nanoTime() - l2);
            dArray[n] = new double[]{d, d2};
        });
        Collections.addAll(object3, dArrayArray);
        long l = atomicLong.get();
        long l2 = atomicLong2.get();
        double d4 = d = l2 > 0L ? (double)l / (double)l2 : 1.0;
        if (d > 0.7) {
            if (q) {
                Constants.LOG.debug("fast terrain estimate: OFF (estimate too costly: {}% of a real call)", (Object)Math.round(d * 100.0));
            }
            return StreamsWorldgen.a(path, d2, d3, "OFF", null, new FastCal(terrainSampler, null, false, 0, null, null));
        }
        boolean bl3 = bl = d <= 0.5;
        if (object3.size() < 100) {
            if (!bl) {
                if (q) {
                    Constants.LOG.debug("fast terrain estimate: OFF (few land points; estimate {}% of a real call, too costly for hybrid)", (Object)Math.round(d * 100.0));
                }
                return StreamsWorldgen.a(path, d2, d3, "OFF", null, new FastCal(terrainSampler, null, false, 0, null, null));
            }
            if (q) {
                Constants.LOG.debug("fast terrain estimate: HYBRID (only {} land calibration points found)", (Object)object3.size());
            }
            return StreamsWorldgen.a(path, d2, d3, "HYBRID", null, new FastCal(terrainSampler, terrainSampler2, true, 0, null, null));
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
        BandCorrection bandCorrection = StreamsWorldgen.a((List<double[]>)object3, d2, d3);
        n6 = 0;
        for (n3 = 0; n3 < n21; ++n3) {
            double d6 = (bandCorrection != null ? bandCorrection.correct(object[0]) : object[0]) - (object = (double[])object3.get(n3))[1];
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
                return StreamsWorldgen.a(path, d2, d3, "OFF", null, new FastCal(terrainSampler, null, false, 0, null, null));
            }
            if (q) {
                Constants.LOG.debug("fast terrain estimate: HYBRID (banded correction {}% of {} points within +/-{}; estimate {}% of a real call)", n3, n21, 2, Math.round(d * 100.0));
            }
            object = bandCorrection == null ? terrainSampler2 : StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0);
            return StreamsWorldgen.a(path, d2, d3, "HYBRID", bandCorrection, new FastCal(terrainSampler, (TerrainSampler)object, true, 0, null, null));
        }
        if (!StreamsWorldgen.a(bandCorrection.centers(), bandCorrection.safe(), chunkGenerator.getSeaLevel())) {
            if (q) {
                Constants.LOG.debug("fast terrain estimate: HYBRID (banded correction passed {}% but routing-critical bands unproven)", (Object)n3);
            }
            return StreamsWorldgen.a(path, d2, d3, "HYBRID", bandCorrection, new FastCal(terrainSampler, StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null));
        }
        if (q) {
            Constants.LOG.debug("fast terrain estimate: ON (banded correction, {} bands, {} grade-safe, {}% of {} points within +/-{})", bandCorrection.centers().length, bandCorrection.safeCount(), n3, n21, 2);
        }
        return StreamsWorldgen.a(path, d2, d3, "ON", bandCorrection, new FastCal(StreamsWorldgen.a(terrainSampler2, bandCorrection, 1.0), StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), false, n9 == 0 ? 2 : 1, bandCorrection, terrainSampler2));
    }

    private static TerrainSampler a(TerrainSampler terrainSampler, BandCorrection bandCorrection, double d) {
        return (n, n2) -> bandCorrection.correct(terrainSampler.height(n, n2)) - d;
    }

    private static BandCorrection a(List<double[]> list, double d, double d2) {
        int n2;
        double d3;
        Object[] objectArray2;
        Object object;
        Map.Entry entry2;
        Object object2;
        TreeMap<Integer, List> treeMap = new TreeMap<Integer, List>();
        for (int i = 0; i < list.size(); i += 2) {
            object2 = list.get(i);
            treeMap.computeIfAbsent((int)Math.floor(object2[0] / 16.0) * 16, n -> new ArrayList()).add(object2);
        }
        ArrayList<Double> arrayList = new ArrayList<Double>();
        object2 = new ArrayList();
        for (Map.Entry entry2 : treeMap.entrySet()) {
            if (((List)entry2.getValue()).size() < 6) continue;
            object = new ArrayList();
            for (Object[] objectArray2 : (List)entry2.getValue()) {
                object.add(objectArray2[0] - objectArray2[1]);
            }
            Collections.sort(object);
            double d4 = (Double)object.get(object.size() / 2);
            double d5 = 0.0;
            for (d3 = 0.0; d3 <= 2.0; d3 += 1.0) {
                n2 = 0;
                int n3 = 0;
                Iterator iterator = object.iterator();
                while (iterator.hasNext()) {
                    double d6 = (Double)iterator.next();
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
            object2.add(d4 + d5);
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        Object object3 = arrayList.stream().mapToDouble(Double::doubleValue).toArray();
        entry2 = (Map.Entry)object2.stream().mapToDouble(Double::doubleValue).toArray();
        object = new BandCorrection((double[])object3, (double[])entry2, new boolean[((Object)object3).length]);
        int[] nArray = new int[((Object)object3).length];
        objectArray2 = new int[((Object)object3).length];
        for (int i = 1; i < list.size(); i += 2) {
            double d8;
            double[] dArray = list.get(i);
            d3 = Math.floor(dArray[0] / 16.0) * 16.0 + 8.0;
            n2 = Arrays.binarySearch((double[])object3, d3);
            if (n2 < 0 || (d8 = ((BandCorrection)object).correct(dArray[0]) - 1.0 - dArray[1]) >= 6.0) continue;
            int n4 = n2;
            nArray[n4] = nArray[n4] + 1;
            if (!(d8 > d) && !(d8 < -d2)) continue;
            int n5 = n2;
            objectArray2[n5] = objectArray2[n5] + true;
        }
        boolean[] blArray = new boolean[((Object)object3).length];
        for (int i = 0; i < ((Object)object3).length; ++i) {
            blArray[i] = nArray[i] >= 20 && (double)objectArray2[i] <= (double)nArray[i] * 0.050000000000000044;
        }
        return new BandCorrection((double[])object3, (double[])entry2, blArray);
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
            Object object;
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
                object = list.get(i).trim();
                if (((String)object).isEmpty()) continue;
                String[] stringArray2 = ((String)object).split(",");
                arrayList.add(Double.parseDouble(stringArray2[0]));
                arrayList2.add(Double.parseDouble(stringArray2[1]));
                arrayList3.add("1".equals(stringArray2[2]));
            }
            if (arrayList.isEmpty()) {
                bandCorrection = null;
            } else {
                object = new boolean[arrayList3.size()];
                for (int i = 0; i < ((Object)object).length; ++i) {
                    object[i] = (Boolean)arrayList3.get(i);
                }
                bandCorrection = new BandCorrection(arrayList.stream().mapToDouble(Double::doubleValue).toArray(), arrayList2.stream().mapToDouble(Double::doubleValue).toArray(), (boolean[])object);
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
                    return new FastCal(terrainSampler, bandCorrection == null ? terrainSampler2 : StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null);
                }
                case "ON": {
                    if (bandCorrection == null) {
                        return null;
                    }
                    if (!StreamsWorldgen.a(bandCorrection.centers(), bandCorrection.safe(), n2)) {
                        if (q) {
                            Constants.LOG.debug("fast terrain estimate: HYBRID (persisted ON calibration; routing-critical bands unproven)");
                        }
                        return new FastCal(terrainSampler, StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), true, 0, null, null);
                    }
                    if (q) {
                        Constants.LOG.debug("fast terrain estimate: ON (persisted calibration, {} bands, {} grade-safe, mode {})", bandCorrection.centers().length, bandCorrection.safeCount(), n);
                    }
                    return new FastCal(StreamsWorldgen.a(terrainSampler2, bandCorrection, 1.0), StreamsWorldgen.a(terrainSampler2, bandCorrection, 0.0), false, n == 0 ? 2 : 1, bandCorrection, terrainSampler2);
                }
            }
            return null;
        }
        catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static RiverEngine engineIfPresent(ServerLevel serverLevel) {
        Map<ServerLevel, RiverEngine> map = a;
        synchronized (map) {
            return a.get(serverLevel);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static RiverEngine engine(WorldGenLevel worldGenLevel) {
        ServerLevel serverLevel = worldGenLevel.getLevel();
        Map<ServerLevel, RiverEngine> map = a;
        synchronized (map) {
            double d2;
            Object object;
            Object object2;
            Object runtimeException;
            Object object3;
            Object object4;
            int n5;
            RiverEngine riverEngine = a.get(serverLevel);
            if (riverEngine != null) {
                if (serverLevel.dimension().equals(Level.OVERWORLD)) {
                    StreamsWorldgen.d = riverEngine;
                }
                return riverEngine;
            }
            ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
            RandomState randomState = serverLevel.getChunkSource().randomState();
            long l = worldGenLevel.getSeed();
            final BiomeSource biomeSource = chunkGenerator.getBiomeSource();
            final Climate.Sampler sampler = randomState.sampler();
            TerrainSampler terrainSampler = StreamsWorldgen.a((int n, int n2) -> chunkGenerator.getBaseHeight(n, n2, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState), chunkGenerator, randomState, serverLevel, l);
            final ArrayList<TagKey<Biome>> arrayList = new ArrayList<TagKey<Biome>>();
            final HashSet<ResourceLocation> hashSet = new HashSet<ResourceLocation>();
            for (String object62 : Services.PLATFORM.outletBiomes().split(",")) {
                String n7 = object62.trim();
                if (n7.isEmpty()) continue;
                n5 = n7.charAt(0) == '#' ? 1 : 0;
                object4 = ResourceLocation.tryParse(n5 != 0 ? n7.substring(1) : n7);
                if (object4 == null) {
                    Constants.LOG.warn("Streams Reflowing: ignoring unreadable outlet biome entry '{}'.", (Object)n7);
                    continue;
                }
                if (n5 != 0) {
                    arrayList.add(TagKey.create(Registries.BIOME, (ResourceLocation)object4));
                    continue;
                }
                hashSet.add((ResourceLocation)object4);
            }
            final boolean bl = !arrayList.isEmpty() || !hashSet.isEmpty();
            int n6 = Services.PLATFORM.fastOceanSkip() ? 1 : 0;
            final RTFBridge.Handle handle = Boolean.getBoolean("streamsreflowing.nortfheights") ? null : RTFBridge.handleFor(randomState);
            final RiverFlowField.BiomeClass biomeClass = Boolean.getBoolean("streamsreflowing.noshareoceancache") ? (n, n2) -> biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(chunkGenerator.getSeaLevel()), QuartPos.fromBlock(n2), sampler).is(BiomeTags.IS_OCEAN) ? 2 : 0 : RiverFlowFields.classifierFor(serverLevel);
            int n7 = QuartPos.fromBlock(chunkGenerator.getSeaLevel());
            n5 = chunkGenerator.getSeaLevel();
            StreamBiomeBridge.landProbe = (n, n2, n3) -> biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(n2), QuartPos.fromBlock(n3), sampler);
            object4 = StreamsWorldgen.a(chunkGenerator, randomState, biomeSource, sampler, n7, terrainSampler, serverLevel);
            Object object5 = object3 = ((FastCal)object4).gradePolicy() >= 1 || Boolean.getBoolean("streamsreflowing.hybridgrid") ? object4 : new FastCal(terrainSampler, null, false, 0, null, null);
            if (((FastCal)object3).gradePolicy() >= 1) {
                Constants.LOG.info("Streams Reflowing: fast terrain mode ENABLED for {}.", (Object)serverLevel.dimension().location());
            } else if (((FastCal)object3).hybridGrid()) {
                Constants.LOG.info("Streams Reflowing: fast terrain mode PARTIALLY enabled for {}.", (Object)serverLevel.dimension().location());
            } else {
                Constants.LOG.info("Streams Reflowing: fast terrain mode OFF for {}.", (Object)serverLevel.dimension().location());
            }
            TerrainSampler terrainSampler2 = ((FastCal)object3).land();
            TerrainSampler terrainSampler3 = ((FastCal)object3).estimator();
            boolean bl2 = ((FastCal)object3).hybridGrid();
            int n8 = ((FastCal)object3).gradePolicy();
            BandCorrection bandCorrection = ((FastCal)object3).corr();
            TerrainSampler terrainSampler4 = ((FastCal)object3).rawEst();
            boolean bl3 = ((FastCal)object3).baked();
            DensityFunction densityFunction = null;
            if (chunkGenerator instanceof NoiseBasedChunkGenerator) {
                densityFunction = randomState.router().initialDensityWithoutJaggedness();
            }
            DensityFunction densityFunction2 = densityFunction;
            TerrainSampler terrainSampler5 = new TerrainSampler(){
                final /* synthetic */ boolean h;
                final /* synthetic */ TerrainSampler i;
                final /* synthetic */ int j;
                final /* synthetic */ TerrainSampler k;
                final /* synthetic */ BandCorrection l;
                final /* synthetic */ TerrainSampler m;
                final /* synthetic */ boolean n;
                final /* synthetic */ TerrainSampler o;
                final /* synthetic */ boolean p;
                final /* synthetic */ int q;
                final /* synthetic */ ChunkGenerator r;
                final /* synthetic */ ServerLevel s;
                final /* synthetic */ RandomState t;
                final /* synthetic */ DensityFunction u;
                {
                    this.h = bl2;
                    this.i = terrainSampler;
                    this.j = n;
                    this.k = terrainSampler2;
                    this.l = bandCorrection;
                    this.m = terrainSampler3;
                    this.n = bl3;
                    this.o = terrainSampler4;
                    this.p = bl4;
                    this.q = n2;
                    this.r = chunkGenerator;
                    this.s = serverLevel;
                    this.t = randomState;
                    this.u = densityFunction;
                }

                @Override
                public boolean outletBiomeOk(int n, int n2, int n3) {
                    if (!bl) {
                        return true;
                    }
                    Holder<Biome> holder = biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(n2), QuartPos.fromBlock(n3), sampler);
                    for (TagKey tagKey : arrayList) {
                        if (!holder.is(tagKey)) continue;
                        return true;
                    }
                    return !hashSet.isEmpty() && holder.unwrapKey().map(resourceKey -> hashSet.contains(resourceKey.location())).orElse(false) != false;
                }

                @Override
                public int waterBiomeClass(int n, int n2) {
                    if (handle != null && handle.river(n, n2)) {
                        return 1;
                    }
                    return biomeClass.classify(n, n2);
                }

                @Override
                public int waterBiomeClassAt(int n, int n2, int n3) {
                    if (handle != null) {
                        return this.waterBiomeClass(n, n3);
                    }
                    Holder<Biome> holder = biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(n2), QuartPos.fromBlock(n3), sampler);
                    if (holder.is(BiomeTags.IS_OCEAN)) {
                        return 2;
                    }
                    return holder.is(BiomeTags.IS_RIVER) || holder.is(Biomes.RIVER) || holder.is(Biomes.FROZEN_RIVER) ? 1 : 0;
                }

                @Override
                public double riverSurface(int n, int n2) {
                    return handle != null ? handle.riverSurface(n, n2) : Double.NaN;
                }

                @Override
                public double height(int n, int n2) {
                    if (this.h && biomeClass.classify(n, n2) == 2) {
                        return -1000.0;
                    }
                    return handle != null ? handle.height(n, n2) : this.i.height(n, n2);
                }

                @Override
                public boolean batchCapable() {
                    return false;
                }

                @Override
                public void heightsBatch(int[] nArray, int[] nArray2, double[] dArray, int n) {
                    int n2;
                    int[] nArray3 = new int[n];
                    int[] nArray4 = new int[n];
                    int[] nArray5 = new int[n];
                    int n3 = 0;
                    for (int i = 0; i < n; ++i) {
                        if (this.h && biomeClass.classify(nArray[i], nArray2[i]) == 2) {
                            dArray[i] = -1000.0;
                            continue;
                        }
                        nArray3[n3] = nArray[i];
                        nArray4[n3] = nArray2[i];
                        nArray5[n3] = i;
                        ++n3;
                    }
                    if (n3 == 0) {
                        return;
                    }
                    double[] dArray2 = new double[n3];
                    for (n2 = 0; n2 < n3; ++n2) {
                        dArray2[n2] = this.i.height(nArray3[n2], nArray4[n2]);
                    }
                    for (n2 = 0; n2 < n3; ++n2) {
                        dArray[nArray5[n2]] = dArray2[n2];
                    }
                }

                @Override
                public double groundHeight(int n, int n2) {
                    double d;
                    if (this.j == 2) {
                        return this.height(n, n2);
                    }
                    if (this.j == 1 && this.l.gradeSafe(d = this.k.height(n, n2))) {
                        return this.l.correct(d) - 1.0;
                    }
                    return this.h && biomeClass.classify(n, n2) == 2 ? -1000.0 : this.m.height(n, n2);
                }

                @Override
                public double groundHeightExact(int n, int n2) {
                    return this.h && biomeClass.classify(n, n2) == 2 ? -1000.0 : this.m.height(n, n2);
                }

                @Override
                public boolean bakedCalibration() {
                    return this.n;
                }

                @Override
                public boolean hasEstimate() {
                    return handle != null || this.o != null;
                }

                @Override
                public boolean useHybridGridFill() {
                    return this.p;
                }

                @Override
                public double estimate(int n, int n2) {
                    if (this.o != null) {
                        return this.o.height(n, n2);
                    }
                    return handle != null ? handle.height(n, n2) : this.height(n, n2);
                }

                @Override
                public double surfaceHeight(int n, int n2) {
                    if (biomeClass.classify(n, n2) == 2) {
                        return this.q;
                    }
                    return this.r.getBaseHeight(n, n2, Heightmap.Types.WORLD_SURFACE_WG, this.s, this.t);
                }

                @Override
                public boolean openAt(int n, int n2, int n3) {
                    if (this.u == null) {
                        return (double)n2 > this.height(n, n3);
                    }
                    return this.u.compute(new DensityFunction.SinglePointContext(n, n2, n3)) <= 0.390625;
                }
            };
            CachingTerrainSampler cachingTerrainSampler = new CachingTerrainSampler(terrainSampler5, 65536, -1000.0);
            WetnessSampler wetnessSampler = (n, n2, n3) -> Services.PLATFORM.biomeWetness(biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(n2), QuartPos.fromBlock(n3), sampler));
            int n9 = Services.PLATFORM.gridSpacing();
            RiverEngine riverEngine2 = new RiverEngine(l, StreamsConfig.defaults(n9, Services.PLATFORM.widthScale(), Services.PLATFORM.plateScale(), Services.PLATFORM.terrainSampleSpacing(), Services.PLATFORM.valleySnapRadius()).withMinRiverAccumulation(6.0).withWetnessMultiplier(Services.PLATFORM.wetnessMultiplier()).withWetnessDryFloor(Services.PLATFORM.wetnessDryFloor()).withLakeArea(Services.PLATFORM.generateLakes() ? Services.PLATFORM.lakeMinArea() : Double.MAX_VALUE, Services.PLATFORM.lakeMaxArea()).withMinSeaChainNodes(Services.PLATFORM.minStreamLength() / n9).withOutletBiomes(Services.PLATFORM.outletBiomes()).withConnect(Services.PLATFORM.connectNearbyWater(), Services.PLATFORM.connectRadius()).withSinkRooted(Services.PLATFORM.sinkRooted()).withGrowDensity(Services.PLATFORM.growDensity()).withConfineToBanks(Services.PLATFORM.confineToBanks()).withContainmentBerm(Services.PLATFORM.containmentBerm()).withAppearance(Services.PLATFORM.meanderStrength(), Services.PLATFORM.baseWidth(), Services.PLATFORM.maxWidth(), Services.PLATFORM.streamDepth()).withBankNoise(Services.PLATFORM.bankNoise()), cachingTerrainSampler, wetnessSampler);
            if (System.getProperty("streamsreflowing.benchcalls") != null) {
                runtimeException = RiverFlowFields.classifierFor(serverLevel);
                object2 = ((FastCal)object4).estimator();
                object = new Random(20260810L);
                int path = 1000;
                int[] d3 = new int[path];
                int[] nArray = new int[path];
                d2 = 0.0;
                for (int string = 1; string <= 2; ++string) {
                    for (int i = 0; i < path; ++i) {
                        d3[i] = ((Random)object).nextInt(8192) - 4096;
                        nArray[i] = ((Random)object).nextInt(8192) - 4096;
                    }
                    long j = System.nanoTime();
                    if (object2 != null) {
                        for (int i = 0; i < path; ++i) {
                            d2 += object2.height(d3[i], nArray[i]);
                        }
                    }
                    j = System.nanoTime();
                    for (int i = 0; i < path; ++i) {
                        d2 += (double)chunkGenerator.getBaseHeight(d3[i], nArray[i], Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState);
                    }
                    j = System.nanoTime();
                    for (int i = 0; i < path; ++i) {
                        d2 += (double)runtimeException.classify(d3[i], nArray[i]);
                    }
                    j = System.nanoTime();
                    Constants.LOG.info("[SR-BENCH] pass {} ({}), {} calls each: IR estimate {} | getBaseHeight {} ms ({} us/call) | biome classify {} ms ({} us/call) [sink {}]", string, string == 1 ? "cold" : "warm", path, object2 != null ? String.format(Locale.ROOT, "%.1f ms (%.2f us/call)", (double)(j - j) / 1000000.0, (double)(j - j) / 1000.0 / (double)path) : "n/a (no calibrated evaluator for this router)", String.format(Locale.ROOT, "%.1f", (double)(j - j) / 1000000.0), String.format(Locale.ROOT, "%.2f", (double)(j - j) / 1000.0 / (double)path), String.format(Locale.ROOT, "%.1f", (double)(j - j) / 1000000.0), String.format(Locale.ROOT, "%.2f", (double)(j - j) / 1000.0 / (double)path), (long)d2);
                }
            }
            riverEngine2.setBuildListener((plateKey, n2, n3, d, n4) -> {
                int n5 = n2 + n3;
                int n6 = n5 > 0 ? n3 * 100 / n5 : 0;
                Constants.LOG.debug("Built river region {} @ gridSpacing {}: {} samples ({} real, {} ocean-skipped = {}%), {} segments, {} ms, sample-cache {}% hits", plateKey, n9, n5, n2, n3, n6, n4, String.format("%.1f", d), cachingTerrainSampler.hitPercent());
            });
            try {
                runtimeException = serverLevel.dimension().location().toString().replace(':', '_').replace('/', '_');
                object2 = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve((String)runtimeException);
                object = new FileNetworkStore((Path)object2, l, riverEngine2.config());
                riverEngine2.setNetworkStore((NetworkStore)object);
                if (serverLevel.dimension().equals(Level.OVERWORLD)) {
                    RiverFlowFields.initStore((Path)object2, ((FileNetworkStore)object).fingerprint());
                    RawFlowTileStore.initStore((Path)object2, ((FileNetworkStore)object).fingerprint());
                }
            }
            catch (RuntimeException runtimeException2) {
                Constants.LOG.warn("River network cache unavailable; networks won't persist this session ({})", (Object)runtimeException2.toString());
            }
            if (bl3 || n8 >= 1 || bl2) {
                try {
                    runtimeException = serverLevel.dimension().location().toString().replace(':', '_').replace('/', '_');
                    object2 = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve((String)runtimeException);
                    object = object2.resolve(m);
                    Path path = object2.resolve("estcost.flag");
                    double d3 = Services.PLATFORM.gradeWindowAbove();
                    d2 = Services.PLATFORM.gradeWindowBelow();
                    try {
                        if (Files.isRegularFile(path, new LinkOption[0])) {
                            String iOException = Files.readString(path).trim();
                            if (iOException.startsWith("ON")) {
                                riverEngine2.setEstCostVerdict(1);
                            } else if (iOException.startsWith("OFF")) {
                                riverEngine2.setEstCostVerdict(2);
                            }
                        }
                    }
                    catch (IOException iOException) {
                        // empty catch block
                    }
                    riverEngine2.setEstCostPersist(arg_0 -> StreamsWorldgen.a(path, (Path)object, d3, d2, arg_0));
                }
                catch (RuntimeException runtimeException3) {
                    // empty catch block
                }
            }
            riverEngine2.setStreamsEnabled(Services.PLATFORM.generateStreams());
            a.put(serverLevel, riverEngine2);
            if (serverLevel.dimension().equals(Level.OVERWORLD)) {
                StreamsWorldgen.d = riverEngine2;
                StreamPrefetcher.warmWorldOrigin(riverEngine2);
            }
            return riverEngine2;
        }
    }

    public static RiverEngine active() {
        return d;
    }

    public static boolean structureFootprintOnStream(BoundingBox boundingBox) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        double d;
        double d2;
        RiverEngine riverEngine = StreamsWorldgen.d;
        if (riverEngine == null) {
            return false;
        }
        int n6 = E;
        double d3 = (double)(boundingBox.maxX() - boundingBox.minX()) / 2.0 + (double)n6 + 1.0;
        double d4 = (double)(boundingBox.maxZ() - boundingBox.minZ()) / 2.0 + (double)n6 + 1.0;
        double d5 = (double)(boundingBox.minX() + boundingBox.maxX()) / 2.0;
        if (!riverEngine.platesReadyNear(d5, d2 = (double)(boundingBox.minZ() + boundingBox.maxZ()) / 2.0, d = Math.sqrt(d3 * d3 + d4 * d4))) {
            riverEngine.whenPlatesReadyNear(d5, d2, d);
            return false;
        }
        int n7 = boundingBox.minX() - n6;
        int n8 = boundingBox.maxX() + n6;
        int n9 = boundingBox.minZ() - n6;
        int n10 = boundingBox.maxZ() + n6;
        for (n5 = n7; n5 <= n8; n5 += 16) {
            for (n4 = n9; n4 <= n10; n4 += 16) {
                if (!riverEngine.corridorNearChunk(n5, n4, n6)) continue;
                n3 = Math.max(n7, n5 - n6);
                n2 = Math.min(n8, n5 + 15 + n6);
                n = Math.max(n9, n4 - n6);
                int n11 = Math.min(n10, n4 + 15 + n6);
                for (int i = n3; i <= n2 + 3; i += 4) {
                    int n12 = Math.min(i, n2);
                    for (int j = n; j <= n11 + 3; j += 4) {
                        int n13 = Math.min(j, n11);
                        if (!riverEngine.inCorridor(n12, n13) || boundingBox.maxY() < riverEngine.plan(n12, n13).waterSurfaceY() - 6) continue;
                        return true;
                    }
                }
            }
        }
        for (n5 = n7; n5 <= n8 + 7; n5 += 8) {
            n4 = Math.min(n5, n8);
            for (n3 = n9; n3 <= n10 + 7; n3 += 8) {
                n2 = Math.min(n3, n10);
                n = riverEngine.lakeSurfaceAt(n4, n2);
                if (n == Integer.MIN_VALUE || boundingBox.maxY() < n - 6) continue;
                return true;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void unloadLevel(ServerLevel serverLevel) {
        RiverEngine riverEngine;
        Map<ServerLevel, RiverEngine> map = a;
        synchronized (map) {
            riverEngine = a.remove(serverLevel);
        }
        if (riverEngine != null) {
            riverEngine.releaseThreadMemos();
            if (riverEngine == d) {
                d = null;
            }
        }
        RiverFlowFields.unloadLevel(serverLevel);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clearActive() {
        d = null;
        Map<ServerLevel, RiverEngine> map = a;
        synchronized (map) {
            for (RiverEngine riverEngine : a.values()) {
                riverEngine.releaseThreadMemos();
            }
            a.clear();
        }
        StreamPrefetcher.clearPending();
        RiverFlowFields.clear();
        StreamFlowGrid.clear();
        ChunkPlanCache.clear();
        RawFlowTileStore.close();
        RawFlowTiles.clear();
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
            dArray3[n2] = StreamsWorldgen.a(sampler, nArray[0], nArray[1]);
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
        Constants.LOG.info("[SR-CAL] calibration analysis: {} land columns, raw error sigma {} (mean {})", n4, StreamsWorldgen.b(StreamsWorldgen.b(dArray3)), StreamsWorldgen.b(StreamsWorldgen.a(dArray3)));
        String[] stringArray2 = new String[]{"elevation/16 (SHIPPED)", "biome", "continentalness/0.05", "erosion/0.05", "weirdness/0.05", "temperature/0.05", "humidity/0.05", "elevation/16 x biome"};
        Object object = List.of(n -> "e" + Math.floorDiv((int)dArray2[n], 16), n -> stringArray[n], n -> "c" + Math.round(dArrayArray[n][0] / 0.05), n -> "r" + Math.round(dArrayArray[n][1] / 0.05), n -> "w" + Math.round(dArrayArray[n][2] / 0.05), n -> "t" + Math.round(dArrayArray[n][3] / 0.05), n -> "h" + Math.round(dArrayArray[n][4] / 0.05), n -> "e" + Math.floorDiv((int)dArray2[n], 16) + "|" + stringArray[n]);
        for (int i = 0; i < stringArray2.length; ++i) {
            int n12;
            IntFunction intFunction = (IntFunction)object.get(i);
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
            Object object2 = new double[arrayList2.size()];
            for (n12 = 0; n12 < ((Object)object2).length; ++n12) {
                object2[n12] = (Double)arrayList2.get(n12);
            }
            n12 = 0;
            for (Object object3 : object2) {
                if (!(Math.abs((double)object3) <= 2.0)) continue;
                ++n12;
            }
            Constants.LOG.info("[SR-CAL]   {}: residual sigma {}, {}% within +-2 ({} usable buckets, {} sparse pts)", stringArray2[i], StreamsWorldgen.b(StreamsWorldgen.b((double[])object2)), Math.round(100.0 * (double)n12 / (double)Math.max(1, ((Object)object2).length)), n13, arrayList3.size());
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
        double d = StreamsWorldgen.a(dArray);
        double d2 = 0.0;
        for (double d3 : dArray) {
            d2 += (d3 - d) * (d3 - d);
        }
        return Math.sqrt(d2 / (double)(dArray.length - 1));
    }

    private static String b(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static /* synthetic */ void a(Path path, Path path2, double d, double d2, int n) {
        try {
            Files.writeString(path, (CharSequence)(n == 2 ? "OFF\n" : "ON\n"), new OpenOption[0]);
            if (n == 2) {
                Files.writeString(path2, (CharSequence)("OFF " + d + " " + d2 + "\n"), new OpenOption[0]);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    static {
        RiverEngine.setBuildRequester(StreamPrefetcher::submitChunkDemand);
        IGNORES_CURRENT = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "ignores_current"));
        RIVER_CARVER_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_carver");
        RIVER_CARVER = new RiverCarverFeature(NoneFeatureConfiguration.CODEC);
        RIVER_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river"));
        RIVER_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river"));
        STREAM_VEGETATION_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_vegetation");
        STREAM_VEGETATION = new StreamVegetationFeature(NoneFeatureConfiguration.CODEC);
        STREAM_VEGETATION_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_vegetation"));
        STREAM_VEGETATION_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_vegetation"));
        BIOME_BOOST_EXCLUDED = TagKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "biome_boost_excluded"));
        NO_BANK_GRASS = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "no_bank_grass"));
        UNDERWATER_VEGETATION_FLOOR = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "underwater_vegetation_floor"));
        SNOW_CLEANUP_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "snow_cleanup");
        SNOW_CLEANUP = new StreamSnowCleanupFeature(NoneFeatureConfiguration.CODEC);
        SNOW_CLEANUP_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "snow_cleanup"));
        SNOW_CLEANUP_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "snow_cleanup"));
        RIVER_FLOW_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_flow");
        RIVER_FLOW = new StreamRiverFlowFeature(NoneFeatureConfiguration.CODEC);
        RIVER_FLOW_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_flow"));
        RIVER_FLOW_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "river_flow"));
        WATER_SKIN_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "water_skin");
        WATER_SKIN = new WaterSkinFeature(NoneFeatureConfiguration.CODEC);
        WATER_SKIN_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "water_skin"));
        WATER_SKIN_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "water_skin"));
        a = Collections.synchronizedMap(new WeakHashMap());
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
        E = Math.max(0, Integer.getInteger("streamsreflowing.structurepad", 8));
    }

    private record FastCal(TerrainSampler land, TerrainSampler estimator, boolean hybridGrid, int gradePolicy, BandCorrection corr, TerrainSampler rawEst, boolean baked) {
        FastCal(TerrainSampler terrainSampler, TerrainSampler terrainSampler2, boolean bl, int n, BandCorrection bandCorrection, TerrainSampler terrainSampler3) {
            this(terrainSampler, terrainSampler2, bl, n, bandCorrection, terrainSampler3, false);
        }
    }

    private record BandCorrection(double[] centers, double[] bias, boolean[] safe) {
        double correct(double d) {
            if (this.centers.length == 1 || d <= this.centers[0]) {
                return d - this.bias[0];
            }
            if (d >= this.centers[this.centers.length - 1]) {
                return d - this.bias[this.bias.length - 1];
            }
            int n = 1;
            while (this.centers[n] < d) {
                ++n;
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
            for (n = 1; n < this.centers.length && this.centers[n] < d; ++n) {
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

