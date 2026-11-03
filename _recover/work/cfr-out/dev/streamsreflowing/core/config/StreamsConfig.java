/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.config;

public record StreamsConfig(int seaLevel, double plateScale, double plateJitter, int gridSpacing, int terrainSampleSpacing, int valleySnapRadius, double minRiverAccumulation, double baseWidth, double maxWidth, double widthScale, double streamDepth, int stepDeltaY, double bankWidth, double bankSlope, double bankNoise, double bankSteepFactor, double ditchInset, double aRiver, double borderMargin, int smoothingPasses, double meanderStrength, int seaLevelExtension, double reconnectRadius, double wetnessDryCutoff, double wetnessWetReference, boolean connectNearbyWater, double connectRadius, boolean sinkRooted, boolean growFromWater, double growDensity, boolean confineToBanks, boolean containmentBerm, double wetnessMultiplier, double wetnessDryFloor, int minSeaChainNodes, double lakeMinArea, double lakeMaxArea, String outletBiomes) {
    public static StreamsConfig defaults() {
        return StreamsConfig.defaults(12, 1.0, 1024, 0, 0);
    }

    public static StreamsConfig defaults(int n, double d, int n2, int n3, int n4) {
        return new StreamsConfig(63, n2, 0.45, n, n3, n4, 3.0, 0.4, 16.0, d, 0.34, 1, 2.0, 0.2, 0.21, 3.5, 3.0, 1.0, 0.0, 3, 1.2, 128, 96.0, 0.2, 0.7, false, 24.0, false, true, 0.75, false, true, 1.0, 0.02, 3, 1024.0, 0.0, "");
    }

    public StreamsConfig withOutletBiomes(String string) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, string);
    }

    public StreamsConfig withLakeArea(double d, double d2) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, d, d2, this.outletBiomes);
    }

    public StreamsConfig withMinSeaChainNodes(int n) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, n, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withMinRiverAccumulation(double d) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, d, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withWetnessMultiplier(double d) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, d, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withWetnessDryFloor(double d) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, d, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withConnect(boolean bl, double d) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, bl, d, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withSinkRooted(boolean bl) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, bl, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withGrowFromWater(boolean bl) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, bl, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withGrowDensity(double d) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, d, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withConfineToBanks(boolean bl) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, bl, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withContainmentBerm(boolean bl) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, bl, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withAppearance(double d, double d2, double d3, double d4) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, d2, d3, this.widthScale, d4, this.stepDeltaY, this.bankWidth, this.bankSlope, this.bankNoise, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, d, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }

    public StreamsConfig withBankNoise(double d) {
        return new StreamsConfig(this.seaLevel, this.plateScale, this.plateJitter, this.gridSpacing, this.terrainSampleSpacing, this.valleySnapRadius, this.minRiverAccumulation, this.baseWidth, this.maxWidth, this.widthScale, this.streamDepth, this.stepDeltaY, this.bankWidth, this.bankSlope, d, this.bankSteepFactor, this.ditchInset, this.aRiver, this.borderMargin, this.smoothingPasses, this.meanderStrength, this.seaLevelExtension, this.reconnectRadius, this.wetnessDryCutoff, this.wetnessWetReference, this.connectNearbyWater, this.connectRadius, this.sinkRooted, this.growFromWater, this.growDensity, this.confineToBanks, this.containmentBerm, this.wetnessMultiplier, this.wetnessDryFloor, this.minSeaChainNodes, this.lakeMinArea, this.lakeMaxArea, this.outletBiomes);
    }
}

