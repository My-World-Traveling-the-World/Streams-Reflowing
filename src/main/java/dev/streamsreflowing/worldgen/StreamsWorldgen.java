/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.terrain.CachingTerrainSampler;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.core.terrain.WetnessSampler;
import dev.streamsreflowing.flow.RawFlowTileStore;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;
import dev.streamsreflowing.worldgen.StreamTerrainCalibration.FastCal;
import dev.streamsreflowing.worldgen.StreamTerrainCalibration.BandCorrection;
import dev.streamsreflowing.worldgen.StreamTerrainCalibration.MutableCtx;

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
    private static final double n = 0.95;
    private static final double o = 6.0;
    private static final int p = 20;
    private static final int r = 40;
    private static final int s = 2000;
    private static final int t = 300;
    private static final int u = 384;
    private static final double v = 0.7;
    private static final double w = 0.5;
    private static final double C = 1.1;
    private static final int D = 120;
    private static final int E;
    private static final int F = 6;
    private static final ThreadLocal<MutableCtx> OPEN_CTX = ThreadLocal.withInitial(() -> new MutableCtx(0, 0, 0));

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
            int n5;
            RiverEngine riverEngine = a.get(serverLevel);
            if (riverEngine != null) {
                if (!riverEngine.isClosed() && serverLevel.dimension().equals(Level.OVERWORLD)) {
                    StreamsWorldgen.d = riverEngine;
                }
                return riverEngine;
            }
            ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
            RandomState randomState = serverLevel.getChunkSource().randomState();
            long l = worldGenLevel.getSeed();
            final BiomeSource biomeSource = chunkGenerator.getBiomeSource();
            final Climate.Sampler sampler = randomState.sampler();
            TerrainSampler terrainSampler = StreamFormSampling.chooseGradeSampler((int n, int n2) -> chunkGenerator.getBaseHeight(n, n2, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState), chunkGenerator, randomState, serverLevel, l);
            final ArrayList<TagKey<Biome>> arrayList = new ArrayList<TagKey<Biome>>();
            final HashSet<ResourceLocation> hashSet = new HashSet<ResourceLocation>();
            for (String object62 : Services.PLATFORM.outletBiomes().split(",")) {
                String n7 = object62.trim();
                if (n7.isEmpty()) continue;
                boolean bl5 = n7.charAt(0) == '#';
                ResourceLocation resourceLocation = ResourceLocation.tryParse(bl5 ? n7.substring(1) : n7);
                if (resourceLocation == null) {
                    Constants.LOG.warn("Streams Reflowing: ignoring unreadable outlet biome entry '{}'.", (Object)n7);
                    continue;
                }
                if (bl5) {
                    arrayList.add(TagKey.create(Registries.BIOME, resourceLocation));
                    continue;
                }
                hashSet.add(resourceLocation);
            }
            final boolean bl = !arrayList.isEmpty() || !hashSet.isEmpty();
            boolean bl4 = Services.PLATFORM.fastOceanSkip();
            final RTFBridge.Handle handle = Boolean.getBoolean("streamsreflowing.nortfheights") ? null : RTFBridge.handleFor(randomState);
            final RiverFlowField.BiomeClass biomeClass = Boolean.getBoolean("streamsreflowing.noshareoceancache") ? (n, n2) -> biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(chunkGenerator.getSeaLevel()), QuartPos.fromBlock(n2), sampler).is(BiomeTags.IS_OCEAN) ? 2 : 0 : RiverFlowFields.classifierFor(serverLevel);
            int n7 = QuartPos.fromBlock(chunkGenerator.getSeaLevel());
            n5 = chunkGenerator.getSeaLevel();
            StreamBiomeBridge.landProbe = (n, n2, n3) -> biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(n2), QuartPos.fromBlock(n3), sampler);
            FastCal fastCal = StreamTerrainCalibration.estimate(chunkGenerator, randomState, biomeSource, sampler, n7, terrainSampler, serverLevel);
            FastCal fastCal2 = fastCal.gradePolicy() >= 1 || Boolean.getBoolean("streamsreflowing.hybridgrid") ? fastCal : new FastCal(terrainSampler, null, false, 0, null, null);
            if (fastCal2.gradePolicy() >= 1) {
                Constants.LOG.info("Streams Reflowing: fast terrain mode ENABLED for {}.", (Object)serverLevel.dimension().location());
            } else if (fastCal2.hybridGrid()) {
                Constants.LOG.info("Streams Reflowing: fast terrain mode PARTIALLY enabled for {}.", (Object)serverLevel.dimension().location());
            } else {
                Constants.LOG.info("Streams Reflowing: fast terrain mode OFF for {}.", (Object)serverLevel.dimension().location());
            }
            TerrainSampler terrainSampler2 = fastCal2.land();
            TerrainSampler terrainSampler3 = fastCal2.estimator();
            boolean bl2 = fastCal2.hybridGrid();
            int n8 = fastCal2.gradePolicy();
            BandCorrection bandCorrection = fastCal2.corr();
            TerrainSampler terrainSampler4 = fastCal2.rawEst();
            boolean bl3 = fastCal2.baked();
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
                    this.h = bl4;
                    this.i = terrainSampler2;
                    this.j = n8;
                    this.k = terrainSampler4;
                    this.l = bandCorrection;
                    this.m = terrainSampler;
                    this.n = bl3;
                    this.o = terrainSampler3;
                    this.p = bl2;
                    this.q = n5;
                    this.r = chunkGenerator;
                    this.s = serverLevel;
                    this.t = randomState;
                    this.u = densityFunction2;
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
                    MutableCtx mutableCtx = OPEN_CTX.get();
                    mutableCtx.x = n;
                    mutableCtx.y = n2;
                    mutableCtx.z = n3;
                    return this.u.compute(mutableCtx) <= 0.390625;
                }
            };
            CachingTerrainSampler cachingTerrainSampler = new CachingTerrainSampler(terrainSampler5, 262144, -1000.0);
            WetnessSampler wetnessSampler = (n, n2, n3) -> Services.PLATFORM.biomeWetness(biomeSource.getNoiseBiome(QuartPos.fromBlock(n), QuartPos.fromBlock(n2), QuartPos.fromBlock(n3), sampler));
            int n9 = Services.PLATFORM.gridSpacing();
            RiverEngine riverEngine2 = new RiverEngine(l, StreamsConfig.defaults(n9, Services.PLATFORM.widthScale(), Services.PLATFORM.plateScale(), Services.PLATFORM.terrainSampleSpacing(), Services.PLATFORM.valleySnapRadius()).withMinRiverAccumulation(6.0).withWetnessMultiplier(Services.PLATFORM.wetnessMultiplier()).withWetnessDryFloor(Services.PLATFORM.wetnessDryFloor()).withLakeArea(Services.PLATFORM.generateLakes() ? Services.PLATFORM.lakeMinArea() : Double.MAX_VALUE, Services.PLATFORM.lakeMaxArea()).withMinSeaChainNodes(Services.PLATFORM.minStreamLength() / n9).withOutletBiomes(Services.PLATFORM.outletBiomes()).withConnect(Services.PLATFORM.connectNearbyWater(), Services.PLATFORM.connectRadius()).withSinkRooted(Services.PLATFORM.sinkRooted()).withGrowDensity(Services.PLATFORM.growDensity()).withConfineToBanks(Services.PLATFORM.confineToBanks()).withContainmentBerm(Services.PLATFORM.containmentBerm()).withAppearance(Services.PLATFORM.meanderStrength(), Services.PLATFORM.baseWidth(), Services.PLATFORM.maxWidth(), Services.PLATFORM.streamDepth()).withBankNoise(Services.PLATFORM.bankNoise()), cachingTerrainSampler, wetnessSampler);
            if (System.getProperty("streamsreflowing.benchcalls") != null) {
                var classifier = RiverFlowFields.classifierFor(serverLevel);
                var estimator = fastCal.estimator();
                // 诊断沿用原两批坐标及读取顺序；计时与输出迁出，避免局部变量复用产生恒零。
                TerrainCallBenchmark.run(estimator == null ? null : estimator::height,
                        (x, z) -> chunkGenerator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                                serverLevel, randomState),
                        (x, z) -> classifier.classify(x, z));
            }
            riverEngine2.setBuildListener((plateKey, n2, n3, d, n4) -> {
                int n10 = n2 + n3;
                int n11 = n10 > 0 ? n3 * 100 / n10 : 0;
                Constants.LOG.debug("Built river region {} @ gridSpacing {}: {} samples ({} real, {} ocean-skipped = {}%), {} segments, {} ms, sample-cache {}% hits", plateKey, n9, n10, n2, n3, n11, n4, String.format("%.1f", d), cachingTerrainSampler.hitPercent());
            });
            try {
                String string2 = serverLevel.dimension().location().toString().replace(':', '_').replace('/', '_');
                Path path2 = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(string2);
                FileNetworkStore fileNetworkStore = new FileNetworkStore(
                        path2, l, riverEngine2.config(), ModSetFingerprint.current());
                riverEngine2.setNetworkStore(fileNetworkStore);
                if (serverLevel.dimension().equals(Level.OVERWORLD)) {
                    RiverFlowFields.initStore(path2, fileNetworkStore.fingerprint());
                    RawFlowTileStore.initStore(path2, fileNetworkStore.fingerprint());
                }
            }
            catch (RuntimeException runtimeException2) {
                Constants.LOG.warn("River network cache unavailable; networks won't persist this session ({})", (Object)runtimeException2.toString());
            }
            if (bl3 || n8 >= 1 || bl2) {
                try {
                    String string3 = serverLevel.dimension().location().toString().replace(':', '_').replace('/', '_');
                    Path path3 = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("streamsreflowing").resolve(string3);
                    Path path5 = path3.resolve(StreamTerrainCalibration.CALIBRATION_FILENAME);
                    Path path = path3.resolve("estcost.flag");
                    double d3 = Services.PLATFORM.gradeWindowAbove();
                    double d4 = Services.PLATFORM.gradeWindowBelow();
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
                    riverEngine2.setEstCostPersist(arg_0 -> StreamsWorldgen.a(path, path5, d3, d4, arg_0));
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
        RiverEngine riverEngine = StreamsWorldgen.d;
        if (riverEngine == null) {
            return false;
        }
        int n6 = E;
        double d3 = (double)(boundingBox.maxX() - boundingBox.minX()) / 2.0 + (double)n6 + 1.0;
        double d4 = (double)(boundingBox.maxZ() - boundingBox.minZ()) / 2.0 + (double)n6 + 1.0;
        double centerX = (double)(boundingBox.minX() + boundingBox.maxX()) / 2.0;
        double centerZ = (double)(boundingBox.minZ() + boundingBox.maxZ()) / 2.0;
        // 缺板时复用上一次查询的邻板，不再按同一中心和半径重新枚举。
        var plates = riverEngine.platesNear(centerX, centerZ, Math.sqrt(d3 * d3 + d4 * d4));
        if (!riverEngine.platesReady(plates)) {
            StreamPrefetcher.requestPlates(riverEngine, plates);
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
            if (riverEngine == d) d = null;
        }
        if (riverEngine != null) riverEngine.close();
        RiverFlowFields.unloadLevel(serverLevel);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clearActive() {
        List<RiverEngine> closing;
        synchronized (a) {
            d = null;
            closing = List.copyOf(a.values());
        }
        // 等待回调脱离世界注册锁执行；先结案，再丢弃没有运行的队项。
        closing.forEach(RiverEngine::close);
        StreamPrefetcher.clearPending();
        RiverFlowFields.clear();
        StreamFlowGrid.clear();
        ChunkPlanCache.clear();
        RawFlowTileStore.close();
        RawFlowTiles.clear();
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
        E = Math.max(0, Integer.getInteger("streamsreflowing.structurepad", 8));
    }


}

