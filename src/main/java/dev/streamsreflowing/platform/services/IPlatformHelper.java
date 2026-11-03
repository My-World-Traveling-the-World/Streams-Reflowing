/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.platform.services;

import java.nio.file.Path;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public interface IPlatformHelper {
    default public Path configDir() {
        return Path.of("config", new String[0]);
    }

    public String getPlatformName();

    public boolean isModLoaded(String var1);

    default public String modVersion(String modId) {
        return null;
    }

    public boolean isDevelopmentEnvironment();

    default public boolean generateStreams() {
        return true;
    }

    default public boolean generateLakes() {
        return true;
    }

    default public boolean generateRivers() {
        return this.generateStreams() || this.generateLakes();
    }

    default public BlockState streamWaterState(int flowDir8) {
        return Blocks.WATER.defaultBlockState();
    }

    default public BlockState streamFallingState(int flowDir8) {
        return null;
    }

    default public boolean flowVanillaRivers() {
        return false;
    }

    default public boolean vanillaWaterFlow() {
        return false;
    }

    default public double vesselDriftSpeed() {
        return 30.0;
    }

    default public boolean fastChunkLoading() {
        return true;
    }

    default public double biomeWetness(Holder<Biome> biome) {
        return 1.0;
    }

    default public int gridSpacing() {
        return 16;
    }

    default public double widthScale() {
        return 1.0;
    }

    default public double wetnessMultiplier() {
        return 1.0;
    }

    default public double wetnessDryFloor() {
        return 0.02;
    }

    default public int minStreamLength() {
        return 50;
    }

    default public int riverDrainageSize() {
        return 1;
    }

    default public double lakeMinArea() {
        return 1024.0;
    }

    default public double lakeMaxArea() {
        return 0.0;
    }

    default public int plateScale() {
        return 768;
    }

    default public int terrainSampleSpacing() {
        return 32;
    }

    default public int valleySnapRadius() {
        return 8;
    }

    default public int prefetchRadius() {
        return 5;
    }

    default public boolean connectNearbyWater() {
        return false;
    }

    default public int connectRadius() {
        return 24;
    }

    default public boolean sinkRooted() {
        return false;
    }

    default public double growDensity() {
        return 0.75;
    }

    default public boolean confineToBanks() {
        return false;
    }

    default public boolean containmentBerm() {
        return true;
    }

    default public double meanderStrength() {
        return 1.2;
    }

    default public double baseWidth() {
        return 0.4;
    }

    default public double maxWidth() {
        return 10.0;
    }

    default public double streamDepth() {
        return 0.34;
    }

    default public double bankNoise() {
        return 0.21;
    }

    default public boolean bermYieldToStreams() {
        return true;
    }

    default public boolean customBanks() {
        return true;
    }

    default public int physicsModWater() {
        return 1;
    }

    default public String outletBiomes() {
        return "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp,#c:is_wet";
    }

    default public boolean reterraforgedCompat() {
        return true;
    }

    default public boolean fastOceanSkip() {
        return true;
    }

    default public boolean fastTerrainEstimate() {
        return true;
    }

    default public int terrainSamplingMode() {
        return 1;
    }

    default public double gradeWindowAbove() {
        return 2.0;
    }

    default public double gradeWindowBelow() {
        return 6.0;
    }

    default public int vegetation() {
        return 2;
    }

    default public int allWaterFlora() {
        return 2;
    }

    default public int allWaterBanks() {
        return 1;
    }

    default public int floraRadius() {
        return 5;
    }

    default public int biomeBoostPercent() {
        return 4;
    }

    default public String biomeBoostBlacklist() {
        return "";
    }

    default public String streamBiomeBlacklist() {
        return "";
    }

    default public String underwaterFloraBiomeBlacklist() {
        return "";
    }

    default public int bankGrassPercent() {
        return 40;
    }

    default public int underwaterFlora() {
        return 2;
    }

    default public int underwaterFloraPercent() {
        return 35;
    }

    default public boolean clearFloatingSnow() {
        return true;
    }

    default public boolean welcomeMessage() {
        return true;
    }

    default public boolean rebiomeStreams() {
        return this.rebiomeStreamsMode() != 0;
    }

    default public boolean rebiomeLakes() {
        return this.rebiomeLakesMode() != 0;
    }

    default public int rebiomeStreamsMode() {
        return 1;
    }

    default public int rebiomeLakesMode() {
        return 1;
    }

    default public String getEnvironmentName() {
        return this.isDevelopmentEnvironment() ? "development" : "production";
    }
}

