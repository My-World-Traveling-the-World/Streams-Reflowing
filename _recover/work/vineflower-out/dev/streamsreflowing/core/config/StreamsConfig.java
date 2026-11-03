package dev.streamsreflowing.core.config;

public record StreamsConfig() {
   private final int seaLevel;
   private final double plateScale;
   private final double plateJitter;
   private final int gridSpacing;
   private final int terrainSampleSpacing;
   private final int valleySnapRadius;
   private final double minRiverAccumulation;
   private final double baseWidth;
   private final double maxWidth;
   private final double widthScale;
   private final double streamDepth;
   private final int stepDeltaY;
   private final double bankWidth;
   private final double bankSlope;
   private final double bankNoise;
   private final double bankSteepFactor;
   private final double ditchInset;
   private final double aRiver;
   private final double borderMargin;
   private final int smoothingPasses;
   private final double meanderStrength;
   private final int seaLevelExtension;
   private final double reconnectRadius;
   private final double wetnessDryCutoff;
   private final double wetnessWetReference;
   private final boolean connectNearbyWater;
   private final double connectRadius;
   private final boolean sinkRooted;
   private final boolean growFromWater;
   private final double growDensity;
   private final boolean confineToBanks;
   private final boolean containmentBerm;
   private final double wetnessMultiplier;
   private final double wetnessDryFloor;
   private final int minSeaChainNodes;
   private final double lakeMinArea;
   private final double lakeMaxArea;
   private final String outletBiomes;

   public StreamsConfig(
      int var1,
      double var2,
      double var4,
      int var6,
      int var7,
      int var8,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      int var19,
      double var20,
      double var22,
      double var24,
      double var26,
      double var28,
      double var30,
      double var32,
      int var34,
      double var35,
      int var37,
      double var38,
      double var40,
      double var42,
      boolean var44,
      double var45,
      boolean var47,
      boolean var48,
      double var49,
      boolean var51,
      boolean var52,
      double var53,
      double var55,
      int var57,
      double var58,
      double var60,
      String var62
   ) {
      this.seaLevel = var1;
      this.plateScale = var2;
      this.plateJitter = var4;
      this.gridSpacing = var6;
      this.terrainSampleSpacing = var7;
      this.valleySnapRadius = var8;
      this.minRiverAccumulation = var9;
      this.baseWidth = var11;
      this.maxWidth = var13;
      this.widthScale = var15;
      this.streamDepth = var17;
      this.stepDeltaY = var19;
      this.bankWidth = var20;
      this.bankSlope = var22;
      this.bankNoise = var24;
      this.bankSteepFactor = var26;
      this.ditchInset = var28;
      this.aRiver = var30;
      this.borderMargin = var32;
      this.smoothingPasses = var34;
      this.meanderStrength = var35;
      this.seaLevelExtension = var37;
      this.reconnectRadius = var38;
      this.wetnessDryCutoff = var40;
      this.wetnessWetReference = var42;
      this.connectNearbyWater = var44;
      this.connectRadius = var45;
      this.sinkRooted = var47;
      this.growFromWater = var48;
      this.growDensity = var49;
      this.confineToBanks = var51;
      this.containmentBerm = var52;
      this.wetnessMultiplier = var53;
      this.wetnessDryFloor = var55;
      this.minSeaChainNodes = var57;
      this.lakeMinArea = var58;
      this.lakeMaxArea = var60;
      this.outletBiomes = var62;
   }

   public static StreamsConfig defaults() {
      return defaults(12, 1.0, 1024, 0, 0);
   }

   public static StreamsConfig defaults(int var0, double var1, int var3, int var4, int var5) {
      return new StreamsConfig(
         63,
         (double)var3,
         0.45,
         var0,
         var4,
         var5,
         3.0,
         0.4,
         16.0,
         var1,
         0.34,
         1,
         2.0,
         0.2,
         0.21,
         3.5,
         3.0,
         1.0,
         0.0,
         3,
         1.2,
         128,
         96.0,
         0.2,
         0.7,
         false,
         24.0,
         false,
         true,
         0.75,
         false,
         true,
         1.0,
         0.02,
         3,
         1024.0,
         0.0,
         ""
      );
   }

   public StreamsConfig withOutletBiomes(String var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         var1
      );
   }

   public StreamsConfig withLakeArea(double var1, double var3) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         var1,
         var3,
         this.outletBiomes
      );
   }

   public StreamsConfig withMinSeaChainNodes(int var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         var1,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withMinRiverAccumulation(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         var1,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withWetnessMultiplier(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         var1,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withWetnessDryFloor(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         var1,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withConnect(boolean var1, double var2) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         var1,
         var2,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withSinkRooted(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         var1,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withGrowFromWater(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         var1,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withGrowDensity(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         var1,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withConfineToBanks(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         var1,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withContainmentBerm(boolean var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         var1,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withAppearance(double var1, double var3, double var5, double var7) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         var3,
         var5,
         this.widthScale,
         var7,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         this.bankNoise,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         var1,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }

   public StreamsConfig withBankNoise(double var1) {
      return new StreamsConfig(
         this.seaLevel,
         this.plateScale,
         this.plateJitter,
         this.gridSpacing,
         this.terrainSampleSpacing,
         this.valleySnapRadius,
         this.minRiverAccumulation,
         this.baseWidth,
         this.maxWidth,
         this.widthScale,
         this.streamDepth,
         this.stepDeltaY,
         this.bankWidth,
         this.bankSlope,
         var1,
         this.bankSteepFactor,
         this.ditchInset,
         this.aRiver,
         this.borderMargin,
         this.smoothingPasses,
         this.meanderStrength,
         this.seaLevelExtension,
         this.reconnectRadius,
         this.wetnessDryCutoff,
         this.wetnessWetReference,
         this.connectNearbyWater,
         this.connectRadius,
         this.sinkRooted,
         this.growFromWater,
         this.growDensity,
         this.confineToBanks,
         this.containmentBerm,
         this.wetnessMultiplier,
         this.wetnessDryFloor,
         this.minSeaChainNodes,
         this.lakeMinArea,
         this.lakeMaxArea,
         this.outletBiomes
      );
   }
}
