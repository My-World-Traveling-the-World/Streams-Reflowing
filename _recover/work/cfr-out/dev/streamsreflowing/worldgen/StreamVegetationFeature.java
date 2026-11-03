/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.ChunkPlanCache;
import dev.streamsreflowing.worldgen.StreamBiomeBlacklist;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.UnderwaterFloraBlacklist;
import dev.streamsreflowing.worldgen.bank.BankFeature;
import dev.streamsreflowing.worldgen.bank.BankFeatureLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class StreamVegetationFeature
extends Feature<NoneFeatureConfiguration> {
    private static final double a = 0.75;
    private static final BlockState b = Blocks.SHORT_GRASS.defaultBlockState();
    private static final BlockState c = Blocks.TALL_GRASS.defaultBlockState();
    private static final BlockState d = Blocks.FERN.defaultBlockState();
    private static volatile Set<ResourceLocation> e;
    private static volatile Set<String> f;
    private static volatile String g;
    private static volatile Object h;
    private static volatile List<BankFeature> i;

    public StreamVegetationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        if (!Services.PLATFORM.generateRivers()) {
            return false;
        }
        WorldGenLevel worldGenLevel = featurePlaceContext.level();
        ChunkPos chunkPos = new ChunkPos(featurePlaceContext.origin());
        int n = chunkPos.getMinBlockX();
        int n4 = chunkPos.getMinBlockZ();
        RiverEngine riverEngine = StreamsWorldgen.engine(worldGenLevel);
        RandomSource randomSource = featurePlaceContext.random();
        ChunkGenerator chunkGenerator = featurePlaceContext.chunkGenerator();
        int n5 = Mth.clamp(Services.PLATFORM.floraRadius(), 1, 16);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl = false;
        bl |= this.a(worldGenLevel, riverEngine, chunkGenerator, randomSource, n, n4, n5, mutableBlockPos);
        int n6 = Services.PLATFORM.underwaterFlora();
        int n7 = Services.PLATFORM.underwaterFloraPercent();
        if (n6 > 0 && n7 > 0) {
            if (n6 >= 3) {
                bl |= this.a(worldGenLevel, randomSource, n, n4, n7);
            } else {
                bl |= this.a(worldGenLevel, riverEngine, randomSource, n, n4, n7);
                if (n6 >= 2) {
                    bl |= this.b(worldGenLevel, riverEngine, randomSource, n, n4, n7);
                }
            }
        }
        int n8 = Services.PLATFORM.vegetation();
        int n9 = Services.PLATFORM.allWaterFlora();
        if (n8 <= 0 && n9 <= 0) {
            return bl;
        }
        bl = n9 > 0 ? (bl |= this.a(worldGenLevel, n, n4, n5, mutableBlockPos, (int n2, int n3, double d) -> this.a(worldGenLevel, chunkGenerator, randomSource, mutableBlockPos, n2, n3, d, n9))) : (bl |= this.a(worldGenLevel, riverEngine, n, n4, n5, (int n2, int n3, double d) -> this.a(worldGenLevel, chunkGenerator, randomSource, mutableBlockPos, n2, n3, d, n8)));
        return bl;
    }

    private boolean a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2, int n3, ColumnAction columnAction) {
        ColumnPlan[] columnPlanArray = ChunkPlanCache.get(riverEngine, n, n2);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl = false;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                double d;
                ColumnPlan columnPlan;
                int n4 = n + i;
                int n5 = n2 + j;
                ColumnPlan columnPlan2 = columnPlan = columnPlanArray != null ? columnPlanArray[i * 16 + j] : null;
                if (columnPlan == null) {
                    columnPlan = riverEngine.plan(n4, n5);
                }
                if (columnPlan == ColumnPlan.NONE || columnPlan.inChannel() || StreamBiomeBlacklist.isBlocked(worldGenLevel.getUncachedNoiseBiome(n4 >> 2, columnPlan.waterSurfaceY() >> 2, n5 >> 2)) || (d = columnPlan.distance() - columnPlan.width()) > (double)n3) continue;
                bl |= columnAction.apply(n4, n5, Mth.clamp(d / (double)n3, 0.0, 1.0));
            }
        }
        return bl;
    }

    private boolean a(WorldGenLevel worldGenLevel, int n, int n2, int n3, BlockPos.MutableBlockPos mutableBlockPos, ColumnAction columnAction) {
        int n4;
        int n5;
        int n6;
        int n7 = 16 + 2 * n3;
        boolean[] blArray = new boolean[n7 * n7];
        boolean bl = false;
        for (int i = -n3; i < 16 + n3; ++i) {
            for (int j = -n3; j < 16 + n3; ++j) {
                n6 = n + i;
                n5 = n2 + j;
                n4 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n6, n5);
                mutableBlockPos.set(n6, n4, n5);
                if (!worldGenLevel.getBlockState(mutableBlockPos).getFluidState().is(FluidTags.WATER)) continue;
                blArray[(i + n3) * n7 + (j + n3)] = true;
                bl = true;
            }
        }
        if (!bl) {
            return false;
        }
        double d = (double)n3 * (double)n3;
        n6 = 0;
        for (n5 = 0; n5 < 16; ++n5) {
            for (n4 = 0; n4 < 16; ++n4) {
                if (blArray[(n5 + n3) * n7 + (n4 + n3)]) continue;
                double d2 = Double.MAX_VALUE;
                for (int i = -n3; i <= n3; ++i) {
                    for (int j = -n3; j <= n3; ++j) {
                        double d3;
                        if (!blArray[(n5 + i + n3) * n7 + (n4 + j + n3)] || !((d3 = (double)(i * i + j * j)) < d2)) continue;
                        d2 = d3;
                    }
                }
                if (d2 > d) continue;
                n6 |= columnAction.apply(n + n5, n2 + n4, Mth.clamp(Math.sqrt(d2) / (double)n3, 0.0, 1.0));
            }
        }
        return n6 != 0;
    }

    private static List<BankFeature> a(WorldGenLevel worldGenLevel) {
        RegistryAccess registryAccess = worldGenLevel.registryAccess();
        if (h != registryAccess) {
            ArrayList<BankFeature> arrayList = new ArrayList<BankFeature>(BankFeatureLoader.configEntries());
            worldGenLevel.registryAccess().registry(BankFeature.REGISTRY_KEY).ifPresent(registry -> registry.forEach(arrayList::add));
            i = List.copyOf(arrayList);
            h = registryAccess;
        }
        return i;
    }

    private boolean a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, ChunkGenerator chunkGenerator, RandomSource randomSource, int n3, int n4, int n5, BlockPos.MutableBlockPos mutableBlockPos) {
        List<BankFeature> list = StreamVegetationFeature.a(worldGenLevel);
        if (list.isEmpty()) {
            return false;
        }
        Optional<Registry<PlacedFeature>> optional = worldGenLevel.registryAccess().registry(Registries.PLACED_FEATURE);
        if (optional.isEmpty()) {
            return false;
        }
        Registry<PlacedFeature> registry = optional.get();
        ArrayList<ResolvedBankFeature> arrayList = new ArrayList<ResolvedBankFeature>();
        for (BankFeature bankFeature : list) {
            ArrayList arrayList2 = new ArrayList();
            for (ResourceLocation resourceLocation : bankFeature.features()) {
                registry.getOptional(resourceLocation).ifPresentOrElse(placedFeature -> arrayList2.add(placedFeature.feature().value()), () -> Constants.LOG.warn("Custom bank feature references unknown placed feature {}", (Object)resourceLocation));
            }
            if (arrayList2.isEmpty()) continue;
            arrayList.add(new ResolvedBankFeature(bankFeature, arrayList2));
        }
        if (arrayList.isEmpty()) {
            return false;
        }
        ColumnAction columnAction = (n, n2, d) -> this.a(worldGenLevel, chunkGenerator, randomSource, mutableBlockPos, n, n2, arrayList);
        if (Services.PLATFORM.allWaterBanks() <= 0) {
            return this.a(worldGenLevel, riverEngine, n3, n4, n5, columnAction);
        }
        return this.a(worldGenLevel, n3, n4, n5, mutableBlockPos, columnAction);
    }

    private boolean a(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, RandomSource randomSource, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, List<ResolvedBankFeature> list) {
        int n3 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n, n2);
        mutableBlockPos.set(n, n3, n2);
        if (!worldGenLevel.getBlockState(mutableBlockPos).isAir()) {
            return false;
        }
        Vec3i vec3i = mutableBlockPos.below();
        if (!worldGenLevel.getBlockState((BlockPos)vec3i).isFaceSturdy(worldGenLevel, (BlockPos)vec3i, Direction.UP)) {
            return false;
        }
        Holder<Biome> holder = worldGenLevel.getBiome(mutableBlockPos);
        ResourceKey resourceKey = holder.unwrapKey().orElse(null);
        BlockPos blockPos = mutableBlockPos.immutable();
        boolean bl = false;
        for (ResolvedBankFeature resolvedBankFeature : list) {
            if (!resolvedBankFeature.entry().matches(holder, resourceKey) || randomSource.nextInt(100) >= resolvedBankFeature.entry().chance()) continue;
            for (ConfiguredFeature<?, ?> configuredFeature : resolvedBankFeature.features()) {
                try {
                    bl |= configuredFeature.place(worldGenLevel, chunkGenerator, randomSource, blockPos);
                }
                catch (RuntimeException runtimeException) {}
            }
        }
        return bl;
    }

    private boolean a(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, RandomSource randomSource, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, double d, int n3) {
        double d2;
        int n4 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n, n2);
        mutableBlockPos.set(n, n4, n2);
        if (!worldGenLevel.getBlockState(mutableBlockPos).isAir()) {
            return false;
        }
        boolean bl = false;
        if (n3 >= 2) {
            d2 = (double)Mth.clamp(Services.PLATFORM.biomeBoostPercent(), 0, 100) / 100.0;
            if ((double)randomSource.nextFloat() < d2 * (1.0 - d)) {
                bl = this.a(worldGenLevel, chunkGenerator, randomSource, mutableBlockPos);
            }
        }
        d2 = (double)Mth.clamp(Services.PLATFORM.bankGrassPercent(), 0, 100) / 100.0;
        if ((double)randomSource.nextFloat() < d2 * (1.0 - 0.75 * d) && worldGenLevel.getBlockState(mutableBlockPos).isAir() && !worldGenLevel.getBiome(mutableBlockPos).is(StreamsWorldgen.NO_BANK_GRASS)) {
            bl |= this.a(worldGenLevel, (BlockPos)mutableBlockPos, randomSource);
        }
        return bl;
    }

    private boolean a(WorldGenLevel worldGenLevel, BlockPos blockPos, RandomSource randomSource) {
        float f = randomSource.nextFloat();
        if (f < 0.6f) {
            return this.a(worldGenLevel, blockPos, b);
        }
        if (f < 0.8f) {
            return this.a(worldGenLevel, blockPos);
        }
        return this.a(worldGenLevel, blockPos, d);
    }

    private boolean a(WorldGenLevel worldGenLevel, BlockPos blockPos, BlockState blockState) {
        if (blockState.canSurvive(worldGenLevel, blockPos)) {
            worldGenLevel.setBlock(blockPos, blockState, 2);
            return true;
        }
        return false;
    }

    private boolean a(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        if (c.canSurvive(worldGenLevel, blockPos) && worldGenLevel.getBlockState(blockPos.above()).isAir()) {
            DoublePlantBlock.placeAt(worldGenLevel, c, blockPos, 2);
            return true;
        }
        return false;
    }

    private boolean a(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, RandomSource randomSource, BlockPos blockPos) {
        BlockPos blockPos2 = blockPos.below();
        if (!worldGenLevel.getBlockState(blockPos2).isFaceSturdy(worldGenLevel, blockPos2, Direction.UP)) {
            return false;
        }
        Holder<Biome> holder = worldGenLevel.getBiome(blockPos);
        BiomeGenerationSettings biomeGenerationSettings = holder.value().getGenerationSettings();
        List<HolderSet<PlacedFeature>> list = biomeGenerationSettings.features();
        ArrayList<Holder<PlacedFeature>> arrayList = new ArrayList<Holder<PlacedFeature>>();
        StreamVegetationFeature.a(list, GenerationStep.Decoration.VEGETAL_DECORATION, arrayList);
        StreamVegetationFeature.a(list, GenerationStep.Decoration.LOCAL_MODIFICATIONS, arrayList);
        arrayList.removeIf(StreamVegetationFeature::b);
        arrayList.removeIf(StreamVegetationFeature::a);
        if (arrayList.isEmpty()) {
            return false;
        }
        ConfiguredFeature<?, ?> configuredFeature = ((PlacedFeature)((Holder)arrayList.get(randomSource.nextInt(arrayList.size()))).value()).feature().value();
        try {
            boolean bl = configuredFeature.place(worldGenLevel, chunkGenerator, randomSource, blockPos.immutable());
            if (bl) {
                StreamVegetationFeature.b(worldGenLevel, blockPos);
            }
            return bl;
        }
        catch (RuntimeException runtimeException) {
            return false;
        }
    }

    private static boolean a(Holder<PlacedFeature> holder) {
        PlacedFeature placedFeature = holder.value();
        for (PlacementModifier placementModifier : placedFeature.placement()) {
            if (!(placementModifier instanceof HeightRangePlacement)) continue;
            return true;
        }
        Iterator<PlacementModifier> iterator = placedFeature.feature().value().feature();
        return iterator == StreamsWorldgen.STREAM_VEGETATION || iterator == StreamsWorldgen.RIVER_CARVER || iterator == Feature.TREE || iterator == Feature.RANDOM_SELECTOR || iterator == Feature.SIMPLE_RANDOM_SELECTOR;
    }

    private static void b(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (int i = -1; i <= 7; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -2; k <= 2; ++k) {
                    mutableBlockPos.set(blockPos.getX() + j, blockPos.getY() + i, blockPos.getZ() + k);
                    BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
                    if (!blockState.hasProperty(LeavesBlock.PERSISTENT) || blockState.getValue(LeavesBlock.PERSISTENT).booleanValue()) continue;
                    worldGenLevel.setBlock(mutableBlockPos, (BlockState)blockState.setValue(LeavesBlock.PERSISTENT, true), 2);
                }
            }
        }
    }

    private static boolean b(Holder<PlacedFeature> holder) {
        if (holder.is(StreamsWorldgen.BIOME_BOOST_EXCLUDED)) {
            return true;
        }
        Set<ResourceLocation> set = StreamVegetationFeature.a();
        Set<String> set2 = f;
        if (set.isEmpty() && (set2 == null || set2.isEmpty())) {
            return false;
        }
        return holder.unwrapKey().map(resourceKey -> set.contains(resourceKey.location()) || set2 != null && set2.contains(resourceKey.location().getNamespace())).orElse(false);
    }

    private static Set<ResourceLocation> a() {
        Set<ResourceLocation> set;
        String string = Services.PLATFORM.biomeBoostBlacklist();
        if (string == null) {
            string = "";
        }
        if ((set = e) != null && string.equals(g)) {
            return set;
        }
        HashSet<ResourceLocation> hashSet = new HashSet<ResourceLocation>();
        HashSet<String> hashSet2 = new HashSet<String>();
        for (String string2 : string.split(",")) {
            String string3 = string2.trim();
            if (string3.isEmpty()) continue;
            if (string3.endsWith(":*")) {
                hashSet2.add(string3.substring(0, string3.length() - 2));
                continue;
            }
            if (string3.indexOf(58) < 0) {
                hashSet2.add(string3);
                continue;
            }
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string3);
            if (resourceLocation == null) continue;
            hashSet.add(resourceLocation);
        }
        f = hashSet2;
        g = string;
        e = hashSet;
        return hashSet;
    }

    private static void a(List<HolderSet<PlacedFeature>> list, GenerationStep.Decoration decoration, List<Holder<PlacedFeature>> list2) {
        int n = decoration.ordinal();
        if (n < list.size()) {
            list.get(n).forEach(list2::add);
        }
    }

    private boolean a(WorldGenLevel worldGenLevel, RandomSource randomSource, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        double d;
        mutableBlockPos.set(n, n2, n3);
        if (!worldGenLevel.getBlockState(mutableBlockPos).is(StreamsWorldgen.UNDERWATER_VEGETATION_FLOOR)) {
            return false;
        }
        Holder<Biome> holder = worldGenLevel.getBiome(mutableBlockPos);
        if (UnderwaterFloraBlacklist.isBlocked(holder)) {
            return false;
        }
        double d2 = Mth.clamp(((double)holder.value().getBaseTemperature() - 0.15) / 0.35, 0.0, 1.0);
        int n5 = (int)Math.round((double)n4 * d2 * (d = Mth.clamp((Services.PLATFORM.biomeWetness(holder) - 0.1) / 0.3, 0.0, 1.0)));
        if (n5 <= 0 || randomSource.nextInt(100) >= n5) {
            return false;
        }
        mutableBlockPos.set(n, n2 + 1, n3);
        if (!worldGenLevel.getBlockState(mutableBlockPos).is(Blocks.WATER)) {
            return false;
        }
        if (worldGenLevel.getBlockState((BlockPos)mutableBlockPos.above()).is(Blocks.WATER) && randomSource.nextBoolean()) {
            DoublePlantBlock.placeAt(worldGenLevel, Blocks.TALL_SEAGRASS.defaultBlockState(), mutableBlockPos, 2);
        } else {
            worldGenLevel.setBlock(mutableBlockPos, Blocks.SEAGRASS.defaultBlockState(), 2);
        }
        return true;
    }

    private boolean a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, RandomSource randomSource, int n, int n2, int n3) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl = false;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n4;
                int n5 = n + i;
                int n6 = n2 + j;
                Lake lake = riverEngine.lakeAt(n5, n6);
                if (lake == null || (n4 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n5, n6) - 1) >= lake.level()) continue;
                bl |= this.a(worldGenLevel, randomSource, mutableBlockPos, n5, n4, n6, n3);
            }
        }
        return bl;
    }

    private boolean b(WorldGenLevel worldGenLevel, RiverEngine riverEngine, RandomSource randomSource, int n, int n2, int n3) {
        ColumnPlan[] columnPlanArray = ChunkPlanCache.get(riverEngine, n, n2);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        boolean bl = false;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n4;
                ColumnPlan columnPlan;
                int n5 = n + i;
                int n6 = n2 + j;
                ColumnPlan columnPlan2 = columnPlan = columnPlanArray != null ? columnPlanArray[i * 16 + j] : null;
                if (columnPlan == null) {
                    columnPlan = riverEngine.plan(n5, n6);
                }
                if (columnPlan == ColumnPlan.NONE || !columnPlan.inChannel() || StreamBiomeBlacklist.isBlocked(worldGenLevel.getUncachedNoiseBiome(n5 >> 2, columnPlan.waterSurfaceY() >> 2, n6 >> 2)) || (n4 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n5, n6) - 1) >= columnPlan.waterSurfaceY()) continue;
                bl |= this.a(worldGenLevel, randomSource, mutableBlockPos, n5, n4, n6, n3);
            }
        }
        return bl;
    }

    private boolean a(WorldGenLevel worldGenLevel, RandomSource randomSource, int n, int n2, int n3) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl = false;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n4 = n + i;
                int n5 = n2 + j;
                int n6 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n4, n5);
                mutableBlockPos.set(n4, n6, n5);
                if (!worldGenLevel.getBlockState(mutableBlockPos).is(Blocks.WATER)) continue;
                bl |= this.a(worldGenLevel, randomSource, mutableBlockPos, n4, n6 - 1, n5, n3);
            }
        }
        return bl;
    }

    static {
        i = List.of();
    }

    @FunctionalInterface
    private static interface ColumnAction {
        public boolean apply(int var1, int var2, double var3);
    }

    private record ResolvedBankFeature(BankFeature entry, List<ConfiguredFeature<?, ?>> features) {
    }
}

