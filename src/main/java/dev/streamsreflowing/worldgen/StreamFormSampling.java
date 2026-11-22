package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.worldgen.StreamTerrainCalibration.MutableCtx;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FieldFormBuilder;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import dev.streamsreflowing.worldgen.regionprep.HeightAccuracy;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;

/** 只有在原精度验证通过后才用密度图采样器；将验证与世界注册分开。 */
final class StreamFormSampling {
    private StreamFormSampling() {}

    static TerrainSampler chooseGradeSampler(TerrainSampler terrainSampler, ChunkGenerator chunkGenerator, RandomState randomState, ServerLevel serverLevel, long l) {
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
            if (!StreamFormSampling.a(terrainSampler, formSampler, randomState.router().finalDensity(), biomeClass)) {
                StreamFormSampling.a(terrainSampler, formSampler, biomeClass);
                return terrainSampler;
            }
            if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
                StreamFormSampling.a(terrainSampler, formSampler);
                Constants.LOG.info("[SR-PREP] grade sampling: accelerated path in use for {}", (Object)serverLevel.dimension().location());
            }
            StreamFormSampling.a((TerrainSampler)formSampler, biomeClass);
            StreamFormSampling.a(terrainSampler, formSampler, biomeClass);
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
            Constants.LOG.info("[SR-PREP] cost per column: classify cold {}us, warm {}us; height {}us -> height is {}x a cold classify, {}x a warm one", StreamFormSampling.a(d), StreamFormSampling.a(d2), StreamFormSampling.a(d3), String.format(Locale.ROOT, "%.0f", d > 0.0 ? d3 / d : 0.0), String.format(Locale.ROOT, "%.0f", d2 > 0.0 ? d3 / d2 : 0.0));
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
        if (!(bl = StreamFormSampling.a(terrainSampler, terrainSampler2, densityFunction, formSampler2 != null, biomeClass)) && formSampler2 != null) {
            Constants.LOG.info("[SR-PREP] arming the aquifer hand-back and re-judging");
            formSampler2.setAquiferHandBack(true);
            bl = StreamFormSampling.a(terrainSampler, terrainSampler2, densityFunction, false, biomeClass);
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
            StreamFormSampling.a(terrainSampler2, densityFunction, n3, n4, d4, d5);
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
            MutableCtx mutableCtx = new MutableCtx(n, 0, n2);
            for (int i = n4; i >= n3; --i) {
                double d4;
                double d5 = formSampler.density(n, i, n2);
                mutableCtx.y = i;
                double d6 = Math.abs(d5 - (d4 = densityFunction.compute(mutableCtx)));
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
}
