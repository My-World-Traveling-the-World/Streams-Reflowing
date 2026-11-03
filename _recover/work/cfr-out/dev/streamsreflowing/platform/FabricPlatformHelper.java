/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.platform;

import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.fabric.fluid.StreamLiquidBlock;
import dev.streamsreflowing.fabric.mixin.BiomeClimateAccessor;
import dev.streamsreflowing.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FabricPlatformHelper
implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public BlockState streamWaterState(int flowDir8) {
        if (!StreamsConfigFabric.bakedStreamFluid()) {
            return IPlatformHelper.super.streamWaterState(flowDir8);
        }
        return (BlockState)StreamFluids.STREAM_BLOCK.defaultBlockState().setValue(StreamLiquidBlock.FLOW, Mth.clamp(flowDir8, 0, 8));
    }

    @Override
    public BlockState streamFallingState(int flowDir8) {
        if (!StreamsConfigFabric.bakedStreamFluid()) {
            return IPlatformHelper.super.streamFallingState(flowDir8);
        }
        return (BlockState)((BlockState)StreamFluids.STREAM_BLOCK.defaultBlockState().setValue(BlockStateProperties.LEVEL, 8)).setValue(StreamLiquidBlock.FLOW, Mth.clamp(flowDir8, 0, 8));
    }

    @Override
    public boolean flowVanillaRivers() {
        return StreamsConfigFabric.flowVanillaRivers();
    }

    @Override
    public double vesselDriftSpeed() {
        return StreamsConfigFabric.vesselDriftSpeed();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse(null);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean generateStreams() {
        return StreamsConfigFabric.generateStreams();
    }

    @Override
    public boolean generateLakes() {
        return StreamsConfigFabric.generateLakes();
    }

    @Override
    public double biomeWetness(Holder<Biome> biome) {
        return ((BiomeClimateAccessor)((Object)biome.value())).streamsreflowing$climate().downfall();
    }

    @Override
    public int gridSpacing() {
        return StreamsConfigFabric.gridSpacing();
    }

    @Override
    public double widthScale() {
        return StreamsConfigFabric.widthScale();
    }

    @Override
    public double wetnessMultiplier() {
        return StreamsConfigFabric.wetnessMultiplier();
    }

    @Override
    public double wetnessDryFloor() {
        return StreamsConfigFabric.wetnessDryFloor();
    }

    @Override
    public int minStreamLength() {
        return StreamsConfigFabric.minStreamLength();
    }

    @Override
    public int riverDrainageSize() {
        return StreamsConfigFabric.riverDrainageSize();
    }

    @Override
    public double lakeMinArea() {
        return StreamsConfigFabric.lakeMinArea();
    }

    @Override
    public double lakeMaxArea() {
        return StreamsConfigFabric.lakeMaxArea();
    }

    @Override
    public int plateScale() {
        return StreamsConfigFabric.plateScale();
    }

    @Override
    public int terrainSampleSpacing() {
        return StreamsConfigFabric.terrainSampleSpacing();
    }

    @Override
    public int valleySnapRadius() {
        return StreamsConfigFabric.valleySnapRadius();
    }

    @Override
    public boolean fastChunkLoading() {
        return StreamsConfigFabric.fastChunkLoading();
    }

    @Override
    public int prefetchRadius() {
        return StreamsConfigFabric.prefetchRadius();
    }

    @Override
    public boolean connectNearbyWater() {
        return StreamsConfigFabric.connectNearbyWater();
    }

    @Override
    public int connectRadius() {
        return StreamsConfigFabric.connectRadius();
    }

    @Override
    public double growDensity() {
        return StreamsConfigFabric.growDensity();
    }

    @Override
    public double baseWidth() {
        return StreamsConfigFabric.baseWidth();
    }

    @Override
    public double maxWidth() {
        return StreamsConfigFabric.maxWidth();
    }

    @Override
    public double meanderStrength() {
        return StreamsConfigFabric.meanderStrength();
    }

    @Override
    public double streamDepth() {
        return StreamsConfigFabric.streamDepth();
    }

    @Override
    public int vegetation() {
        return StreamsConfigFabric.vegetation();
    }

    @Override
    public int allWaterFlora() {
        return StreamsConfigFabric.allWaterFlora();
    }

    @Override
    public int allWaterBanks() {
        return StreamsConfigFabric.allWaterBanks();
    }

    @Override
    public boolean customBanks() {
        return StreamsConfigFabric.customBanks();
    }

    @Override
    public boolean fastTerrainEstimate() {
        return StreamsConfigFabric.fastTerrainEstimate();
    }

    @Override
    public int terrainSamplingMode() {
        return StreamsConfigFabric.terrainSamplingMode();
    }

    @Override
    public double gradeWindowAbove() {
        return StreamsConfigFabric.qualityPreset().gradeWindowAbove;
    }

    @Override
    public double gradeWindowBelow() {
        return StreamsConfigFabric.qualityPreset().gradeWindowBelow;
    }

    @Override
    public int physicsModWater() {
        return StreamsConfigFabric.physicsModWater();
    }

    @Override
    public String outletBiomes() {
        return StreamsConfigFabric.outletBiomes();
    }

    @Override
    public boolean reterraforgedCompat() {
        return StreamsConfigFabric.reterraforgedCompat();
    }

    @Override
    public boolean fastOceanSkip() {
        return StreamsConfigFabric.fastOceanSkip();
    }

    @Override
    public int floraRadius() {
        return StreamsConfigFabric.floraRadius();
    }

    @Override
    public int biomeBoostPercent() {
        return StreamsConfigFabric.biomeBoostPercent();
    }

    @Override
    public String biomeBoostBlacklist() {
        return StreamsConfigFabric.biomeBoostBlacklist();
    }

    @Override
    public String streamBiomeBlacklist() {
        return StreamsConfigFabric.streamBiomeBlacklist();
    }

    @Override
    public String underwaterFloraBiomeBlacklist() {
        return StreamsConfigFabric.underwaterFloraBiomeBlacklist();
    }

    @Override
    public int bankGrassPercent() {
        return StreamsConfigFabric.bankGrassPercent();
    }

    @Override
    public int underwaterFlora() {
        return StreamsConfigFabric.underwaterFlora();
    }

    @Override
    public int underwaterFloraPercent() {
        return StreamsConfigFabric.underwaterFloraPercent();
    }

    @Override
    public boolean containmentBerm() {
        return StreamsConfigFabric.containmentBerm();
    }

    @Override
    public boolean confineToBanks() {
        return StreamsConfigFabric.confineToBanks();
    }

    @Override
    public boolean clearFloatingSnow() {
        return StreamsConfigFabric.clearFloatingSnow();
    }

    @Override
    public int rebiomeStreamsMode() {
        return StreamsConfigFabric.rebiomeStreams();
    }

    @Override
    public int rebiomeLakesMode() {
        return StreamsConfigFabric.rebiomeLakes();
    }

    @Override
    public boolean welcomeMessage() {
        return StreamsConfigFabric.welcomeMessage();
    }

    @Override
    public boolean sinkRooted() {
        return StreamsConfigFabric.sinkRooted();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}

