/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.compat.PhysicsModCompat;
import dev.streamsreflowing.core.config.QualityPreset;
import dev.streamsreflowing.flow.MobFlowOverrides;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

public final class StreamsConfigFabric {
    private static final String FILE_NAME = "streamsreflowing.properties";
    private static final String FILE_NAME_CLIENT = "streamsreflowing-client.properties";
    private static final String K_GENERATE_STREAMS = "generateStreams";
    private static final String K_GENERATE_LAKES = "generateLakes";
    private static final String K_QUALITY_PRESET = "terrainAccuracyLevel";
    private static final String K_QUALITY_PRESET_LEGACY = "qualityPreset";
    private static final String K_PLATE_SIZE = "plateSize";
    private static final String K_WETNESS_MULTIPLIER = "wetnessMultiplier";
    private static final String K_WETNESS_DRY_FLOOR = "wetnessDryFloor";
    private static final String K_GROW_DENSITY = "growDensity";
    private static final String K_MIN_STREAM_LENGTH = "minStreamLength";
    private static final String K_RIVER_DRAINAGE_SIZE = "riverDrainageSize";
    private static final String K_LAKE_MIN_AREA = "lakeMinArea";
    private static final String K_LAKE_MAX_AREA = "lakeMaxArea";
    private static final String K_WIDTH_SCALE = "widthScale";
    private static final String K_BASE_WIDTH = "baseWidth";
    private static final String K_MAX_WIDTH = "maxWidth";
    private static final String K_MEANDER_STRENGTH = "meanderStrength";
    private static final String K_STREAM_DEPTH = "streamDepth";
    private static final String K_CONTAINMENT_BERM = "containmentBerm";
    private static final String K_CUSTOM_BANKS = "customBanks";
    private static final String K_VEGETATION = "vegetation";
    private static final String K_ALL_WATER_FLORA = "allWaterFlora";
    private static final String K_ALL_WATER_BANKS = "allWaterBanks";
    private static final String K_FLORA_RADIUS = "floraRadius";
    private static final String K_BIOME_BOOST_PERCENT = "biomeBoostPercent";
    private static final String K_BIOME_BOOST_BLACKLIST = "biomeBoostBlacklist";
    private static final String K_STREAM_BIOME_BLACKLIST = "streamBiomeBlacklist";
    private static final String K_MOBS_HOLD_IN_CURRENT = "mobsHoldInCurrent";
    private static final String K_MOBS_DRIFT_IN_CURRENT = "mobsDriftInCurrent";
    private static final String K_BANK_GRASS_PERCENT = "bankGrassPercent";
    private static final String K_UNDERWATER_FLORA = "underwaterFlora";
    private static final String K_UNDERWATER_FLORA_PERCENT = "underwaterFloraPercent";
    private static final String K_UNDERWATER_FLORA_BIOME_BLACKLIST = "underwaterFloraBiomeBlacklist";
    private static final String K_SPILL_OVER_WATER = "spillOverWater";
    private static final String K_SPILL_STREAMS_ONLY = "spillOnStreamsOnly";
    private static final String K_FAST_CHUNK_LOADING = "fastChunkLoading";
    private static final String K_BOATS_FLOAT = "boatsFloatOnFlowingWater";
    private static final String K_WATER_MOBS_IGNORE_FLOW = "waterMobsIgnoreFlow";
    private static final String K_ITEM_DRIFT_SPEED = "itemDriftSpeed";
    private static final String K_VESSEL_DRIFT_SPEED = "vesselDriftSpeed";
    private static final String K_VANILLA_WATER_FLOW = "vanillaWaterFlow";
    private static final String K_LEGACY_STREAM_FLUID = "legacyStreamFluid";
    private static final String K_FLOW_VANILLA_RIVERS = "flowVanillaRivers";
    private static final String K_RETERRAFORGED_COMPAT = "reterraforgedCompat";
    private static final String K_BAKED_FLUID_WFF = "bakedFluidWhenFlowingFluids";
    private static final String K_OUTLET_BIOMES = "outletBiomes";
    private static final String K_PHYSICS_MOD_WATER = "physicsModWater";
    private static final String K_FORCE_BAKED_FLUID = "forceBakedFluid";
    private static final String K_STREAM_BUBBLES = "streamBubbles";
    private static final String K_WATER_AMBIENCE = "waterAmbience";
    private static final String K_MAX_STREAM_PARTICLES = "maxStreamParticlesPerTick";
    private static final String K_STREAM_PARTICLE_SCAN_RADIUS = "streamParticleScanRadius";
    private static final String K_PARTICLES_ON_VANILLA_WATER = "particlesOnVanillaWater";
    private static final String K_STREAM_DEBRIS = "streamDebris";
    private static final String K_PARTICLE_SIZE = "particleSize";
    private static final String K_PARTICLE_RATE = "particleDensity";
    private static final String K_LOADING_NOTICE = "loadingNotice";
    private static final String K_CONNECT_NEARBY_WATER = "connectNearbyWater";
    private static final String K_CONNECT_RADIUS = "connectRadius";
    private static final String K_REBIOME_STREAMS = "rebiomeStreams";
    private static final String K_REBIOME_LAKES = "rebiomeLakes";
    private static final String K_WELCOME_MESSAGE = "welcomeMessage";
    private static final boolean D_GENERATE_STREAMS = true;
    private static final boolean D_GENERATE_LAKES = true;
    private static final QualityPreset D_QUALITY_PRESET = QualityPreset.MEDIUM;
    private static final String D_PLATE_SIZE = "-1";
    private static final int PLATE_SIZE_MIN = 256;
    private static final int PLATE_SIZE_MAX = 8192;
    private static final double D_WETNESS_MULTIPLIER = 1.0;
    private static final double D_WETNESS_DRY_FLOOR = 0.02;
    private static final double D_GROW_DENSITY = 0.75;
    private static final int D_MIN_STREAM_LENGTH = 50;
    private static final int D_RIVER_DRAINAGE_SIZE = 1;
    private static final double D_LAKE_MIN_AREA = 1024.0;
    private static final double D_LAKE_MAX_AREA = 0.0;
    private static final double D_WIDTH_SCALE = 1.0;
    private static final double D_BASE_WIDTH = 0.4;
    private static final double D_MAX_WIDTH = 16.0;
    private static final double D_MEANDER_STRENGTH = 1.2;
    private static final double D_STREAM_DEPTH = 0.34;
    private static final boolean D_CONTAINMENT_BERM = true;
    private static final boolean D_CUSTOM_BANKS = true;
    private static final int D_VEGETATION = -1;
    private static final int D_ALL_WATER_FLORA = -1;
    private static final int D_ALL_WATER_BANKS = -1;
    private static final int D_FLORA_RADIUS = 5;
    private static final int D_BIOME_BOOST_PERCENT = 4;
    private static final String D_BIOME_BOOST_BLACKLIST = "";
    private static final String D_STREAM_BIOME_BLACKLIST = "terralith:yellowstone,terrestria:caldera";
    private static final int D_BANK_GRASS_PERCENT = 20;
    private static final int D_UNDERWATER_FLORA = -1;
    private static final int D_UNDERWATER_FLORA_PERCENT = 15;
    private static final String D_UNDERWATER_FLORA_BIOME_BLACKLIST = "";
    private static final boolean D_SPILL_OVER_WATER = true;
    private static final boolean D_SPILL_STREAMS_ONLY = true;
    private static final boolean D_FAST_CHUNK_LOADING = true;
    private static final boolean D_BOATS_FLOAT = true;
    private static final boolean D_WATER_MOBS_IGNORE_FLOW = true;
    private static final double D_ITEM_DRIFT_SPEED = 3.0;
    private static final double D_VESSEL_DRIFT_SPEED = 30.0;
    private static final boolean D_VANILLA_WATER_FLOW = true;
    private static final boolean D_LEGACY_STREAM_FLUID = false;
    private static final boolean D_FLOW_VANILLA_RIVERS = true;
    private static final boolean D_RETERRAFORGED_COMPAT = true;
    private static final int D_PHYSICS_MOD_WATER = 1;
    private static final String D_OUTLET_BIOMES = "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp,#c:is_wet";
    private static final boolean D_BAKED_FLUID_WFF = true;
    private static final boolean D_FORCE_BAKED_FLUID = false;
    private static final int D_STREAM_BUBBLES = 2;
    private static final double D_WATER_AMBIENCE = 1.0;
    private static final int D_MAX_STREAM_PARTICLES = 3000;
    private static final int D_STREAM_PARTICLE_SCAN_RADIUS = 256;
    private static final boolean D_PARTICLES_ON_VANILLA_WATER = true;
    private static final boolean D_STREAM_DEBRIS = true;
    private static final double D_PARTICLE_SIZE = 1.0;
    private static final double D_PARTICLE_RATE = 1.0;
    private static final boolean D_LOADING_NOTICE = true;
    private static final boolean D_CONNECT_NEARBY_WATER = false;
    private static final int D_CONNECT_RADIUS = -1;
    private static final int D_REBIOME_STREAMS = 1;
    private static final int D_REBIOME_LAKES = 1;
    private static final boolean D_WELCOME_MESSAGE = true;
    private static volatile boolean generateStreams = true;
    private static volatile boolean generateLakes = true;
    private static volatile QualityPreset qualityPreset = D_QUALITY_PRESET;
    private static volatile int plateSizeOverride = -1;
    private static volatile double wetnessMultiplier = 1.0;
    private static volatile double wetnessDryFloor = 0.02;
    private static volatile double growDensity = 0.75;
    private static volatile int minStreamLength = 50;
    private static volatile int riverDrainageSize = 1;
    private static volatile double lakeMinArea = 1024.0;
    private static volatile double lakeMaxArea = 0.0;
    private static volatile double widthScale = 1.0;
    private static volatile double baseWidth = 0.4;
    private static volatile double maxWidth = 16.0;
    private static volatile double meanderStrength = 1.2;
    private static volatile double streamDepth = 0.34;
    private static volatile boolean containmentBerm = true;
    private static volatile boolean customBanks = true;
    private static volatile int vegetation = -1;
    private static volatile int allWaterFlora = -1;
    private static volatile int allWaterBanks = -1;
    private static volatile int floraRadius = 5;
    private static volatile int biomeBoostPercent = 4;
    private static volatile String biomeBoostBlacklist = "";
    private static volatile String streamBiomeBlacklist = "terralith:yellowstone,terrestria:caldera";
    private static volatile String mobsHoldInCurrent = "";
    private static volatile String mobsDriftInCurrent = "";
    private static volatile int bankGrassPercent = 20;
    private static volatile int underwaterFlora = -1;
    private static volatile int underwaterFloraPercent = 15;
    private static volatile String underwaterFloraBiomeBlacklist = "";
    private static volatile boolean spillOverWater = true;
    private static volatile boolean spillOnStreamsOnly = true;
    private static volatile boolean fastChunkLoading = true;
    private static volatile boolean boatsFloatOnFlowingWater = true;
    private static volatile boolean waterMobsIgnoreFlow = true;
    private static volatile double itemDriftSpeed = 3.0;
    private static volatile double vesselDriftSpeed = 30.0;
    private static volatile boolean vanillaWaterFlow = true;
    private static volatile boolean legacyStreamFluid = false;
    private static volatile boolean flowVanillaRivers = true;
    private static volatile boolean reterraforgedCompat = true;
    private static volatile boolean bakedFluidWhenFlowingFluids = true;
    private static volatile String outletBiomes = "#minecraft:is_river,#minecraft:is_ocean,#c:is_swamp,#c:is_wet";
    private static volatile int physicsModWater = 1;
    private static volatile boolean forceBakedFluid = false;
    private static volatile int streamBubbles = 2;
    private static volatile double waterAmbience = 1.0;
    private static volatile int maxStreamParticles = 3000;
    private static volatile int streamParticleScanRadius = 256;
    private static volatile boolean particlesOnVanillaWater = true;
    private static volatile boolean streamDebris = true;
    private static volatile double particleSize = 1.0;
    private static volatile double particleRate = 1.0;
    private static volatile boolean loadingNotice = true;
    private static volatile boolean connectNearbyWater = false;
    private static volatile int connectRadius = -1;
    private static volatile int rebiomeStreams = 1;
    private static volatile int rebiomeLakes = 1;
    private static volatile boolean welcomeMessage = true;
    private static Boolean flowingFluidsLoaded;
    private static final Map<String, String> LOADED_DIGEST;
    private static volatile Thread watchThread;

    private StreamsConfigFabric() {
    }

    public static void load() {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
            if (!Files.exists(path, new LinkOption[0])) {
                StreamsConfigFabric.writeDefaults(path, StreamsConfigFabric.defaultFileContents());
            }
            StreamsConfigFabric.refreshComments(path, StreamsConfigFabric.defaultFileContents());
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(path, new OpenOption[0]);){
                props.load(in);
            }
            generateStreams = StreamsConfigFabric.parseBool(props, K_GENERATE_STREAMS, true);
            generateLakes = StreamsConfigFabric.parseBool(props, K_GENERATE_LAKES, true);
            String presetRaw = props.getProperty(K_QUALITY_PRESET);
            if (presetRaw == null) {
                presetRaw = props.getProperty(K_QUALITY_PRESET_LEGACY);
            }
            qualityPreset = StreamsConfigFabric.parsePreset(presetRaw);
            plateSizeOverride = StreamsConfigFabric.parsePlateSize(props.getProperty(K_PLATE_SIZE));
            wetnessMultiplier = StreamsConfigFabric.parseDouble(props, K_WETNESS_MULTIPLIER, 1.0);
            wetnessDryFloor = StreamsConfigFabric.parseDouble(props, K_WETNESS_DRY_FLOOR, 0.02);
            growDensity = Math.max(0.1, Math.min(2.0, StreamsConfigFabric.parseDouble(props, K_GROW_DENSITY, 0.75)));
            minStreamLength = Math.max(0, Math.min(2000, StreamsConfigFabric.parseInt(props, K_MIN_STREAM_LENGTH, 50)));
            riverDrainageSize = Math.max(1, Math.min(3, StreamsConfigFabric.parseInt(props, K_RIVER_DRAINAGE_SIZE, 1)));
            lakeMinArea = StreamsConfigFabric.parseDouble(props, K_LAKE_MIN_AREA, 1024.0);
            lakeMaxArea = StreamsConfigFabric.parseDouble(props, K_LAKE_MAX_AREA, 0.0);
            widthScale = StreamsConfigFabric.parseDouble(props, K_WIDTH_SCALE, 1.0);
            baseWidth = StreamsConfigFabric.parseDouble(props, K_BASE_WIDTH, 0.4);
            maxWidth = StreamsConfigFabric.parseDouble(props, K_MAX_WIDTH, 16.0);
            meanderStrength = StreamsConfigFabric.parseDouble(props, K_MEANDER_STRENGTH, 1.2);
            streamDepth = StreamsConfigFabric.parseDouble(props, K_STREAM_DEPTH, 0.34);
            containmentBerm = StreamsConfigFabric.parseBool(props, K_CONTAINMENT_BERM, true);
            customBanks = StreamsConfigFabric.parseBool(props, K_CUSTOM_BANKS, true);
            vegetation = StreamsConfigFabric.parseInt(props, K_VEGETATION, -1);
            allWaterFlora = StreamsConfigFabric.parseInt(props, K_ALL_WATER_FLORA, -1);
            allWaterBanks = StreamsConfigFabric.parseInt(props, K_ALL_WATER_BANKS, -1);
            floraRadius = StreamsConfigFabric.parseInt(props, K_FLORA_RADIUS, 5);
            biomeBoostPercent = StreamsConfigFabric.parseInt(props, K_BIOME_BOOST_PERCENT, 4);
            biomeBoostBlacklist = props.getProperty(K_BIOME_BOOST_BLACKLIST, "");
            streamBiomeBlacklist = props.getProperty(K_STREAM_BIOME_BLACKLIST, D_STREAM_BIOME_BLACKLIST);
            mobsHoldInCurrent = props.getProperty(K_MOBS_HOLD_IN_CURRENT, "");
            mobsDriftInCurrent = props.getProperty(K_MOBS_DRIFT_IN_CURRENT, "");
            MobFlowOverrides.set(mobsHoldInCurrent, mobsDriftInCurrent);
            bankGrassPercent = StreamsConfigFabric.parseInt(props, K_BANK_GRASS_PERCENT, 20);
            underwaterFlora = StreamsConfigFabric.parseInt(props, K_UNDERWATER_FLORA, -1);
            underwaterFloraPercent = StreamsConfigFabric.parseInt(props, K_UNDERWATER_FLORA_PERCENT, 15);
            underwaterFloraBiomeBlacklist = props.getProperty(K_UNDERWATER_FLORA_BIOME_BLACKLIST, "");
            spillOverWater = StreamsConfigFabric.parseBool(props, K_SPILL_OVER_WATER, true);
            spillOnStreamsOnly = StreamsConfigFabric.parseBool(props, K_SPILL_STREAMS_ONLY, true);
            fastChunkLoading = StreamsConfigFabric.parseBool(props, K_FAST_CHUNK_LOADING, true);
            boatsFloatOnFlowingWater = StreamsConfigFabric.parseBool(props, K_BOATS_FLOAT, true);
            waterMobsIgnoreFlow = StreamsConfigFabric.parseBool(props, K_WATER_MOBS_IGNORE_FLOW, true);
            itemDriftSpeed = StreamsConfigFabric.parseDouble(props, K_ITEM_DRIFT_SPEED, 3.0);
            vesselDriftSpeed = StreamsConfigFabric.parseDouble(props, K_VESSEL_DRIFT_SPEED, 30.0);
            vanillaWaterFlow = StreamsConfigFabric.parseBool(props, K_VANILLA_WATER_FLOW, true);
            legacyStreamFluid = StreamsConfigFabric.parseBool(props, K_LEGACY_STREAM_FLUID, false);
            flowVanillaRivers = StreamsConfigFabric.parseBool(props, K_FLOW_VANILLA_RIVERS, true);
            reterraforgedCompat = StreamsConfigFabric.parseBool(props, K_RETERRAFORGED_COMPAT, true);
            bakedFluidWhenFlowingFluids = StreamsConfigFabric.parseBool(props, K_BAKED_FLUID_WFF, true);
            outletBiomes = props.getProperty(K_OUTLET_BIOMES, D_OUTLET_BIOMES);
            physicsModWater = StreamsConfigFabric.parseMode(props, K_PHYSICS_MOD_WATER, 1, 1);
            if (physicsModWater == 1) {
                PhysicsModCompat.silenceWaterEffect();
            }
            forceBakedFluid = StreamsConfigFabric.parseBool(props, K_FORCE_BAKED_FLUID, false);
            connectNearbyWater = StreamsConfigFabric.parseBool(props, K_CONNECT_NEARBY_WATER, false);
            connectRadius = StreamsConfigFabric.parseInt(props, K_CONNECT_RADIUS, -1);
            rebiomeStreams = StreamsConfigFabric.parseMode(props, K_REBIOME_STREAMS, 1, 2);
            rebiomeLakes = StreamsConfigFabric.parseMode(props, K_REBIOME_LAKES, 1, 2);
            welcomeMessage = StreamsConfigFabric.parseBool(props, K_WELCOME_MESSAGE, true);
            Path clientPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME_CLIENT);
            if (!Files.exists(clientPath, new LinkOption[0])) {
                StreamsConfigFabric.writeDefaults(clientPath, StreamsConfigFabric.defaultClientFileContents());
            }
            StreamsConfigFabric.refreshComments(clientPath, StreamsConfigFabric.defaultClientFileContents());
            Properties clientProps = new Properties();
            try (InputStream in = Files.newInputStream(clientPath, new OpenOption[0]);){
                clientProps.load(in);
            }
            streamBubbles = StreamsConfigFabric.parseInt(clientProps, K_STREAM_BUBBLES, 2);
            waterAmbience = StreamsConfigFabric.parseDouble(clientProps, K_WATER_AMBIENCE, 1.0);
            maxStreamParticles = StreamsConfigFabric.parseInt(clientProps, K_MAX_STREAM_PARTICLES, 3000);
            streamParticleScanRadius = StreamsConfigFabric.parseInt(clientProps, K_STREAM_PARTICLE_SCAN_RADIUS, 256);
            particlesOnVanillaWater = StreamsConfigFabric.parseBool(clientProps, K_PARTICLES_ON_VANILLA_WATER, true);
            streamDebris = StreamsConfigFabric.parseBool(clientProps, K_STREAM_DEBRIS, true);
            particleSize = Math.max(0.05, Math.min(8.0, StreamsConfigFabric.parseDouble(clientProps, K_PARTICLE_SIZE, 1.0)));
            particleRate = Math.max(0.0, Math.min(8.0, StreamsConfigFabric.parseDouble(clientProps, K_PARTICLE_RATE, 1.0)));
            loadingNotice = StreamsConfigFabric.parseBool(clientProps, K_LOADING_NOTICE, true);
            StreamsConfigFabric.migrateFile(path, props, clientPath, clientProps);
        }
        catch (Exception e) {
            Constants.LOG.warn("Failed to load {} -- using defaults", (Object)FILE_NAME, (Object)e);
        }
    }

    public static List<String[]> snapshotOrdered(boolean clientFile) {
        ArrayList<String[]> out = new ArrayList<String[]>();
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve(clientFile ? FILE_NAME_CLIENT : FILE_NAME);
            if (Files.exists(path, new LinkOption[0])) {
                StringBuilder comment = new StringBuilder();
                String section = clientFile ? "PARTICLES & SOUND" : "GENERAL";
                for (String line : Files.readAllLines(path)) {
                    String t = line.trim();
                    if (t.isEmpty()) {
                        comment.setLength(0);
                        continue;
                    }
                    if (t.startsWith("#")) {
                        String body = t.substring(1).trim();
                        if (body.startsWith("=====")) {
                            section = body.replace("=", "").trim();
                            comment.setLength(0);
                            continue;
                        }
                        if (comment.length() > 0) {
                            comment.append('\n');
                        }
                        comment.append(body);
                        continue;
                    }
                    int eq = t.indexOf(61);
                    if (eq > 0) {
                        out.add(new String[]{t.substring(0, eq).trim(), t.substring(eq + 1).trim(), comment.toString(), section});
                    }
                    comment.setLength(0);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return out;
    }

    public static void setValues(Map<String, String> common, Map<String, String> client) {
        try {
            if (common != null && !common.isEmpty()) {
                StreamsConfigFabric.editProperties(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME), common);
            }
            if (client != null && !client.isEmpty()) {
                StreamsConfigFabric.editProperties(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME_CLIENT), client);
            }
        }
        catch (Exception e) {
            Constants.LOG.warn("Failed to update config from screen", e);
        }
        StreamsConfigFabric.load();
        StreamsConfigFabric.recordDigests();
    }

    private static void refreshComments(Path path, String template) throws IOException {
        String current = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        if (current.contains("# Perf:")) {
            return;
        }
        LinkedHashMap<String, String> existing = new LinkedHashMap<String, String>();
        for (String line : Files.readAllLines(path)) {
            String t = line.trim();
            int eq = t.indexOf(61);
            if (t.isEmpty() || t.startsWith("#") || eq <= 0) continue;
            existing.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
        }
        StreamsConfigFabric.writeDefaults(path, template);
        if (!existing.isEmpty()) {
            StreamsConfigFabric.editProperties(path, existing);
        }
    }

    private static void migrateFile(Path path, Properties props, Path clientPath, Properties clientProps) throws IOException {
        boolean dropLegacy;
        LinkedHashMap<String, String> fix = new LinkedHashMap<String, String>();
        StreamsConfigFabric.canon(props, fix, K_GENERATE_STREAMS, Boolean.toString(generateStreams));
        StreamsConfigFabric.canon(props, fix, K_GENERATE_LAKES, Boolean.toString(generateLakes));
        StreamsConfigFabric.canon(props, fix, K_QUALITY_PRESET, qualityPreset.name());
        StreamsConfigFabric.canon(props, fix, K_PLATE_SIZE, plateSizeOverride < 0 ? "auto" : Integer.toString(plateSizeOverride));
        StreamsConfigFabric.canon(props, fix, K_WETNESS_MULTIPLIER, Double.toString(wetnessMultiplier));
        StreamsConfigFabric.canon(props, fix, K_WETNESS_DRY_FLOOR, Double.toString(wetnessDryFloor));
        StreamsConfigFabric.canon(props, fix, K_GROW_DENSITY, Double.toString(growDensity));
        StreamsConfigFabric.canon(props, fix, K_MIN_STREAM_LENGTH, Integer.toString(minStreamLength));
        StreamsConfigFabric.canon(props, fix, K_RIVER_DRAINAGE_SIZE, Integer.toString(riverDrainageSize));
        StreamsConfigFabric.canon(props, fix, K_LAKE_MIN_AREA, Double.toString(lakeMinArea));
        StreamsConfigFabric.canon(props, fix, K_LAKE_MAX_AREA, Double.toString(lakeMaxArea));
        StreamsConfigFabric.canon(props, fix, K_WIDTH_SCALE, Double.toString(widthScale));
        StreamsConfigFabric.canon(props, fix, K_BASE_WIDTH, Double.toString(baseWidth));
        StreamsConfigFabric.canon(props, fix, K_MAX_WIDTH, Double.toString(maxWidth));
        StreamsConfigFabric.canon(props, fix, K_MEANDER_STRENGTH, Double.toString(meanderStrength));
        StreamsConfigFabric.canon(props, fix, K_STREAM_DEPTH, Double.toString(streamDepth));
        StreamsConfigFabric.canon(props, fix, K_CONTAINMENT_BERM, Boolean.toString(containmentBerm));
        StreamsConfigFabric.canon(props, fix, K_CUSTOM_BANKS, Boolean.toString(customBanks));
        StreamsConfigFabric.canon(props, fix, K_VEGETATION, Integer.toString(vegetation));
        StreamsConfigFabric.canon(props, fix, K_ALL_WATER_FLORA, Integer.toString(allWaterFlora));
        StreamsConfigFabric.canon(props, fix, K_ALL_WATER_BANKS, Integer.toString(allWaterBanks));
        StreamsConfigFabric.canon(props, fix, K_FLORA_RADIUS, Integer.toString(floraRadius));
        StreamsConfigFabric.canon(props, fix, K_BIOME_BOOST_PERCENT, Integer.toString(biomeBoostPercent));
        StreamsConfigFabric.canon(props, fix, K_BANK_GRASS_PERCENT, Integer.toString(bankGrassPercent));
        StreamsConfigFabric.canon(props, fix, K_UNDERWATER_FLORA, Integer.toString(underwaterFlora));
        StreamsConfigFabric.canon(props, fix, K_UNDERWATER_FLORA_PERCENT, Integer.toString(underwaterFloraPercent));
        StreamsConfigFabric.canon(props, fix, K_SPILL_OVER_WATER, Boolean.toString(spillOverWater));
        StreamsConfigFabric.canon(props, fix, K_SPILL_STREAMS_ONLY, Boolean.toString(spillOnStreamsOnly));
        StreamsConfigFabric.canon(props, fix, K_FAST_CHUNK_LOADING, Boolean.toString(fastChunkLoading));
        StreamsConfigFabric.canon(props, fix, K_BOATS_FLOAT, Boolean.toString(boatsFloatOnFlowingWater));
        StreamsConfigFabric.canon(props, fix, K_WATER_MOBS_IGNORE_FLOW, Boolean.toString(waterMobsIgnoreFlow));
        StreamsConfigFabric.canon(props, fix, K_ITEM_DRIFT_SPEED, Double.toString(itemDriftSpeed));
        StreamsConfigFabric.canon(props, fix, K_VESSEL_DRIFT_SPEED, Double.toString(vesselDriftSpeed));
        StreamsConfigFabric.canon(props, fix, K_VANILLA_WATER_FLOW, Boolean.toString(vanillaWaterFlow));
        StreamsConfigFabric.canon(props, fix, K_LEGACY_STREAM_FLUID, Boolean.toString(legacyStreamFluid));
        StreamsConfigFabric.canon(props, fix, K_FLOW_VANILLA_RIVERS, Boolean.toString(flowVanillaRivers));
        StreamsConfigFabric.canon(props, fix, K_RETERRAFORGED_COMPAT, Boolean.toString(reterraforgedCompat));
        StreamsConfigFabric.appendIfMissing(props, fix, K_OUTLET_BIOMES, outletBiomes);
        StreamsConfigFabric.canon(props, fix, K_PHYSICS_MOD_WATER, Integer.toString(physicsModWater));
        StreamsConfigFabric.canon(props, fix, K_BAKED_FLUID_WFF, Boolean.toString(bakedFluidWhenFlowingFluids));
        StreamsConfigFabric.canon(props, fix, K_FORCE_BAKED_FLUID, Boolean.toString(forceBakedFluid));
        StreamsConfigFabric.canon(props, fix, K_CONNECT_NEARBY_WATER, Boolean.toString(connectNearbyWater));
        StreamsConfigFabric.canon(props, fix, K_CONNECT_RADIUS, Integer.toString(connectRadius));
        StreamsConfigFabric.canon(props, fix, K_REBIOME_STREAMS, Integer.toString(rebiomeStreams));
        StreamsConfigFabric.canon(props, fix, K_REBIOME_LAKES, Integer.toString(rebiomeLakes));
        StreamsConfigFabric.canon(props, fix, K_WELCOME_MESSAGE, Boolean.toString(welcomeMessage));
        StreamsConfigFabric.appendIfMissing(props, fix, K_BIOME_BOOST_BLACKLIST, biomeBoostBlacklist);
        StreamsConfigFabric.appendIfMissing(props, fix, K_STREAM_BIOME_BLACKLIST, streamBiomeBlacklist);
        StreamsConfigFabric.appendIfMissing(props, fix, K_MOBS_HOLD_IN_CURRENT, mobsHoldInCurrent);
        StreamsConfigFabric.appendIfMissing(props, fix, K_MOBS_DRIFT_IN_CURRENT, mobsDriftInCurrent);
        StreamsConfigFabric.appendIfMissing(props, fix, K_UNDERWATER_FLORA_BIOME_BLACKLIST, underwaterFloraBiomeBlacklist);
        boolean bl = dropLegacy = props.getProperty(K_QUALITY_PRESET_LEGACY) != null;
        if (!fix.isEmpty() || dropLegacy) {
            StreamsConfigFabric.editProperties(path, fix);
            if (dropLegacy) {
                StreamsConfigFabric.removeKeyLine(path, K_QUALITY_PRESET_LEGACY);
            }
        }
        LinkedHashMap<String, String> cfix = new LinkedHashMap<String, String>();
        StreamsConfigFabric.canon(clientProps, cfix, K_STREAM_BUBBLES, Integer.toString(streamBubbles));
        StreamsConfigFabric.canon(clientProps, cfix, K_WATER_AMBIENCE, Double.toString(waterAmbience));
        StreamsConfigFabric.canon(clientProps, cfix, K_MAX_STREAM_PARTICLES, Integer.toString(maxStreamParticles));
        StreamsConfigFabric.canon(clientProps, cfix, K_STREAM_PARTICLE_SCAN_RADIUS, Integer.toString(streamParticleScanRadius));
        StreamsConfigFabric.canon(clientProps, cfix, K_PARTICLES_ON_VANILLA_WATER, Boolean.toString(particlesOnVanillaWater));
        StreamsConfigFabric.canon(clientProps, cfix, K_STREAM_DEBRIS, Boolean.toString(streamDebris));
        StreamsConfigFabric.canon(clientProps, cfix, K_PARTICLE_SIZE, Double.toString(particleSize));
        StreamsConfigFabric.canon(clientProps, cfix, K_PARTICLE_RATE, Double.toString(particleRate));
        StreamsConfigFabric.canon(clientProps, cfix, K_LOADING_NOTICE, Boolean.toString(loadingNotice));
        if (!cfix.isEmpty()) {
            StreamsConfigFabric.editProperties(clientPath, cfix);
        }
    }

    private static void canon(Properties props, Map<String, String> fix, String key, String canonical) {
        String raw = props.getProperty(key);
        if (raw == null || !raw.trim().equals(canonical)) {
            fix.put(key, canonical);
        }
    }

    private static void appendIfMissing(Properties props, Map<String, String> fix, String key, String value) {
        if (props.getProperty(key) == null) {
            fix.put(key, value);
        }
    }

    private static void removeKeyLine(Path path, String key) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        String out = text.replaceAll("(?m)^" + Pattern.quote(key) + "\\s*=.*\\R?", "");
        if (!out.equals(text)) {
            Files.writeString(path, (CharSequence)out, StandardCharsets.UTF_8, new OpenOption[0]);
        }
    }

    private static void editProperties(Path path, Map<String, String> values) throws IOException {
        Object text = Files.exists(path, new LinkOption[0]) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        for (Map.Entry<String, String> e : values.entrySet()) {
            String line = e.getKey() + "=" + e.getValue();
            Pattern keyLine = Pattern.compile("(?m)^" + Pattern.quote(e.getKey()) + "\\s*=.*$");
            if (keyLine.matcher((CharSequence)text).find()) {
                text = keyLine.matcher((CharSequence)text).replaceFirst(Matcher.quoteReplacement(line));
                continue;
            }
            text = (String)text + (((String)text).endsWith("\n") || ((String)text).isEmpty() ? "" : "\n") + line + "\n";
        }
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
        }
        Files.writeString(path, (CharSequence)text, StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private static void writeDefaults(Path path, String contents) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent(), new FileAttribute[0]);
            }
            try (OutputStream out = Files.newOutputStream(path, new OpenOption[0]);){
                out.write(contents.getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String defaultFileContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Streams Reflowing -- Fabric configuration\n");
        sb.append("# Plain key=value properties. Edit and restart to apply (no hot reload on Fabric).\n");
        sb.append("# Mirrors the NeoForge config (config/streamsreflowing-common.toml).\n\n");
        sb.append("# GENERATE STREAMS -- carve this mod's streams and rivers into newly generated land.\n");
        sb.append("#   true  = streams are carved (the normal mod).\n");
        sb.append("#   false = no streams anywhere; lakes below can still form on their own.\n");
        sb.append("# With BOTH this and generateLakes off, the mod does no terrain work at all -- no height sampling, no\n");
        sb.append("# watershed building -- and world generation costs exactly what it would without the mod installed.\n");
        sb.append("# Perf: THE BIGGEST DIAL, together with generateLakes.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_GENERATE_STREAMS).append('=').append(true).append("\n\n");
        sb.append("# GENERATE LAKES -- fill the basins this mod finds with lakes, in newly generated land.\n");
        sb.append("#   true  = lakes form (the normal mod).\n");
        sb.append("#   false = no lakes; streams above route to the sea and to rivers instead.\n");
        sb.append("# With BOTH this and generateStreams off, the mod does no terrain work at all.\n");
        sb.append("# Perf: THE BIGGEST DIAL, together with generateStreams.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_GENERATE_LAKES).append('=').append(true).append("\n\n");
        sb.append("# Mark carved streams as a river biome after they are placed, so a stream reads as a river for fish and\n");
        sb.append("# mob spawns, ambience and water colour. Runs AFTER every other feature, so it never changes what the\n");
        sb.append("# surrounding biome placed.\n");
        sb.append("#   0 = off: streams keep the biome they run through.\n");
        sb.append("#   1 = rebiome as river (default): the best matching river biome for your world -- vanilla's river, or a\n");
        sb.append("#       terrain mod's own river variant where one fits the surrounding land.\n");
        sb.append("#   2 = chameleon river: this mod's own river biome, which takes its water colour from the land around it,\n");
        sb.append("#       so a stream through a swamp keeps swamp-coloured water instead of turning river-blue.\n");
        sb.append("# Perf: light. One extra pass over each new chunk at generation.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_REBIOME_STREAMS).append('=').append(1).append("\n\n");
        sb.append("# The same for the LAKES this mod fills, and independent of the stream setting -- either can run on its own.\n");
        sb.append("#   0 = off: lakes keep the biome they sit in.\n");
        sb.append("#   1 = rebiome as river (default): the best matching river biome for your world.\n");
        sb.append("#   2 = chameleon river: this mod's own river biome, tinted from the land around it.\n");
        sb.append("# Perf: light. One extra pass over each new chunk at generation.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_REBIOME_LAKES).append('=').append(1).append("\n\n");
        sb.append("# Show the one-time Streams Reflowing notice in chat the first time this world is opened.\n");
        sb.append("#   true = show it once per world (default).  false = never show it.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_WELCOME_MESSAGE).append('=').append(true).append("\n\n");
        sb.append("# ===================== PERFORMANCE =====================\n");
        sb.append("# How FAST vs. how GOOD generation is. CPU at world-gen time only; each region builds once and is\n");
        sb.append("# saved to disk forever, so the cost is one-time per area.\n\n");
        sb.append("# The one dial that sets the advanced knobs together. Higher = finer, more accurate rivers but a\n");
        sb.append("# longer one-time build per region; lower = coarser, near-instant.\n");
        sb.append("#   POTATO ~3s/region | LOW ~8s | MEDIUM ~15s (default) | HIGH ~45s | EXTREME ~4min | MAX ~45min\n");
        sb.append("# (Rough, per region, on heavy Tectonic; vanilla is far cheaper.)\n");
        sb.append("# Allowed: POTATO, LOW, MEDIUM, HIGH, EXTREME, MAX.\n");
        sb.append("# Perf: THE MAIN SPEED DIAL (see the per-level timings above). One-time per region.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_QUALITY_PRESET).append('=').append(D_QUALITY_PRESET.name()).append("\n\n");
        sb.append("# Watershed (region) size in blocks -- how large one continuous stream system is before it hands off\n");
        sb.append("# to the next. -1 = auto (follow the quality preset; recommended). Otherwise 256 to 8192.\n");
        sb.append("#   RECOMMENDED: leave at auto for live gameplay. Raising it can cause worldgen pauses when you\n");
        sb.append("#   cross a watershed border (each build covers more ground at once).\n");
        sb.append("#   For PRE-GENERATED worlds, larger watersheds produce stream systems that reach much further\n");
        sb.append("#   inland, at overall similar total generation time. New chunks only.\n");
        sb.append("#   NOTE: while pre-generating, the generation status can appear FROZEN/paused as a stream region\n");
        sb.append("#   builds -- chunk generation waits for that watershed to finish, then resumes. This is normal;\n");
        sb.append("#   larger sizes make each pause longer but less frequent.\n");
        sb.append("# Perf: major. Larger = longer pauses when crossing a border, but fewer of them.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_PLATE_SIZE).append('=').append(D_PLATE_SIZE).append("\n\n");
        sb.append("# ===================== APPEARANCE =====================\n");
        sb.append("# How streams LOOK. No meaningful speed cost. Network-changing knobs apply to NEW chunks only.\n\n");
        sb.append("# WETNESS -- how much water flows through the land overall, like turning up the rainfall. Scales the\n");
        sb.append("# drainage every cell sheds, so it does two things at once the way real discharge does:\n");
        sb.append("#   HIGHER = more streams AND wider, deeper rivers everywhere. LOWER = a drier, sparser, thinner land.\n");
        sb.append("# The master 'how wet is the continent' dial. For branch fineness use growDensity; width alone, widthScale.\n");
        sb.append("#   Default 1.0.\n");
        sb.append("# Perf: moderate at generation. More water = more channels to route and carve.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_WETNESS_MULTIPLIER).append('=').append(1.0).append("\n\n");
        sb.append("# DRY-BIOME STREAM FLOOR -- the minimum share of streams (and humidity-scaled lake fill) that still\n");
        sb.append("# form in the very driest biomes (deserts, badlands). 0.0 = fully-dry biomes get NO streams at all;\n");
        sb.append("#   0.02 keeps ~2%, so a desert still has the occasional wash or oasis pond. Default 0.02.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_WETNESS_DRY_FLOOR).append('=').append(0.02).append("\n\n");
        sb.append("# How finely streams BRANCH -- how much upstream drainage a cell needs to become a stream.\n");
        sb.append("#   LOWER = more, finer tributaries (denser, more fractal).\n");
        sb.append("#   ABOVE 1.0 = sparser than the classic network: only the better-fed channels form, so a\n");
        sb.append("#   valley carries a single stream instead of a branching set. 2.0 is the sparsest.\n");
        sb.append("#   Range 0.1 to 2.0. Default 0.75.\n");
        sb.append("# Perf: moderate at generation. Lower values mean many more channels to carve.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_GROW_DENSITY).append('=').append(0.75).append("\n\n");
        sb.append("# SMALLEST LAKE -- the smallest basin (in blocks of water surface area) that fills as a lake.\n");
        sb.append("#   1024 is roughly a 32x32 pond. Lower = more small ponds; higher = only sizable lakes.\n");
        sb.append("#   Default 1024. New chunks only.\n");
        sb.append("# SHORTEST STREAM -- the shortest stream, in BLOCKS, allowed to run from its own source\n");
        sb.append("# straight into the sea. Anything shorter is dropped, which clears the short nubs that\n");
        sb.append("# otherwise dot a coastline. Streams joining another stream, or ending in a lake, are\n");
        sb.append("# never affected. 0 = keep every stream. Dry biomes are always exempt.\n");
        sb.append("# Default 50. New chunks only.\n");
        sb.append("# Perf: negligible (it only removes streams).\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_MIN_STREAM_LENGTH).append('=').append(50).append("\n\n");
        sb.append("# VANILLA-RIVER DRAINAGE AREA -- how large an area each river flow computation covers, as a\n");
        sb.append("# factor of the watershed size. Larger areas let long rivers find their real ocean outlet, so\n");
        sb.append("# their current runs the right way for their whole length.\n");
        sb.append("#   1 = watershed-sized.  2 = twice the watershed (default).  3 = four times.\n");
        sb.append("# Each area is computed once in the background and saved with the world; larger areas take\n");
        sb.append("# proportionally longer before distant river water starts moving. New chunks only.\n");
        sb.append("# Perf: moderate background work. Each step up covers four times the area.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_RIVER_DRAINAGE_SIZE).append('=').append(1).append("\n\n");
        sb.append("# Perf: light. Lower values leave many more small basins to evaluate.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_LAKE_MIN_AREA).append('=').append(1024.0).append("\n\n");
        sb.append("# LARGEST LAKE -- the largest lake (in blocks of water surface area) allowed to form.\n");
        sb.append("#   0 = automatic. New chunks only.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_LAKE_MAX_AREA).append('=').append(0.0).append("\n\n");
        sb.append("# How WIDE rivers get for a given drainage area.\n");
        sb.append("#   HIGHER = broader rivers. LOWER = narrow, creek-like. maxWidth still caps the main stem. Default 1.0.\n");
        sb.append("# Perf: negligible. Shape only.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_WIDTH_SCALE).append('=').append(1.0).append("\n\n");
        sb.append("# Headwater thinness -- the minimum half-width a stream starts at (it tapers to a point above this).\n");
        sb.append("#   HIGHER = chunkier sources. LOWER = finer, threadier headwaters. Default 0.4.\n");
        sb.append("# Perf: negligible. Shape only.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_BASE_WIDTH).append('=').append(0.4).append("\n\n");
        sb.append("# The widest a river can reach -- the half-width cap on the biggest main stem (near the ocean).\n");
        sb.append("#   HIGHER = broad rivers. LOWER = everything stays creek-sized. Default 16.0.\n");
        sb.append("# Perf: negligible. Shape only.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_MAX_WIDTH).append('=').append(16.0).append("\n\n");
        sb.append("# How much streams WIND (steep reaches straighten automatically).\n");
        sb.append("#   HIGHER = lazier, more meandering. LOWER = straighter, more direct. Default 1.2.\n");
        sb.append("# Perf: negligible. Shape only.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_MEANDER_STRENGTH).append('=').append(1.2).append("\n\n");
        sb.append("# How DEEP streams carve in, as a fraction of their width (smooth bowl, deepest at the center).\n");
        sb.append("#   HIGHER = deeper, more incised. LOWER = shallow, flatter beds. Default 0.34 (~1/3 of width). Carve-only.\n");
        sb.append("# Perf: negligible. Shape only.\n");
        sb.append("# World: safe on an existing world, but takes effect only after the world is RELOADED (quit to title and reopen, or restart the server) -- and then only in newly generated chunks.\n");
        sb.append(K_STREAM_DEPTH).append('=').append(0.34).append("\n\n");
        sb.append("# Where a stream crosses a slope and water would sheet off the downhill side, wall it in with a\n");
        sb.append("# terrain-matching berm and floor the bed (held only up to surrounding ground, never on a pillar).\n");
        sb.append("#   true = on (default). false = off. Carve-only: shapes new chunks; toggling never regenerates terrain.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world, but takes effect only after the world is RELOADED (quit to title and reopen, or restart the server) -- and then only in newly generated chunks.\n");
        sb.append(K_CONTAINMENT_BERM).append('=').append(true).append("\n\n");
        sb.append("# Skin the carved stream channel with the bank styles in config/streamsreflowing/bank_styles/\n");
        sb.append("# (bed / waterline / bank blocks per biome) instead of leaving whatever the cut exposed.\n");
        sb.append("#   true = on (default). false = off (raw cut banks). Carve-only: applies to new chunks.\n");
        sb.append("# Perf: light at generation (per-column surface skinning).\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_CUSTOM_BANKS).append('=').append(true).append("\n\n");
        sb.append("# Extra plant life near water (decoration only -- never changes terrain or the network).\n");
        sb.append("#   0 = none.\n");
        sb.append("#   1 = simple: extra grass, tall grass, ferns and a few flowers near the water, thinning out.\n");
        sb.append("#   2 = biome-boost: as 1, plus the BIOME'S OWN plants and rocks (flowers, cacti, dead bushes,\n");
        sb.append("#       boulders, etc.) at higher density near the water, so banks match their biome and terrain\n");
        sb.append("#       mods. (Trees are never re-placed.)\n");
        sb.append("#  -1 = auto: simple on every accuracy level except the lowest, which places none.\n");
        sb.append("#       Biome-boost is never automatic -- set 2 here to turn it on.\n");
        sb.append("# Applies to NEW chunks only.\n");
        sb.append("# Perf: light at generation.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_VEGETATION).append('=').append(-1).append("\n\n");
        sb.append("# Extra flora around ALL water sources -- vanilla rivers, lakes and oceans -- not just this mod's\n");
        sb.append("# streams. Costs a little extra world-gen scanning in every water-adjacent chunk.\n");
        sb.append("#   0 = off (only this mod's streams, at the vegetation level above).\n");
        sb.append("#   1 = extra grass/ferns/flowers around all water.  2 = biome-boost + extra grass around all water.\n");
        sb.append("#  -1 = auto: off on the lowest accuracy level, level 1 above it. Biome-boost is never\n");
        sb.append("#       automatic -- set 2 here to turn it on.\n");
        sb.append("# Perf: light to moderate. Scans ALL water in a chunk, not just streams.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_ALL_WATER_FLORA).append('=').append(-1).append("\n\n");
        sb.append("# Bank materials (bed/waterline/bank) on ALL water bodies -- vanilla rivers, lakes, oceans -- not just\n");
        sb.append("# this mod's streams/lakes, using the same per-biome bank styles. Requires customBanks = true.\n");
        sb.append("#   0 = off (only this mod's generated streams/lakes get banks).\n");
        sb.append("#   1 = also skin vanilla rivers + lakes/ponds (oceans excluded).\n");
        sb.append("#   2 = also skin oceans (shipped styles touch only the shallow shoreline; deep floor stays raw).\n");
        sb.append("#  -1 = auto (follow preset: level 1 on every preset).\n");
        sb.append("# Perf: light to moderate. Scans ALL water in a chunk, not just streams.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_ALL_WATER_BANKS).append('=').append(-1).append("\n\n");
        sb.append("# How far (blocks) from the water's edge the extra flora reaches; density fades out to this edge.\n");
        sb.append("#   HIGHER = a wider planted band. LOWER = hugs the shoreline. Default 5.\n");
        sb.append("# Perf: light. A larger radius scans more columns per chunk.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_FLORA_RADIUS).append('=').append(5).append("\n\n");
        sb.append("# Biome-boost density (PERCENT): at the water's edge, the chance a near-water column also re-runs one\n");
        sb.append("# of the BIOME'S OWN plant/rock features (only when vegetation/allWaterFlora = 2); fades to 0 at\n");
        sb.append("# floraRadius. Runs BEFORE the simple grass, so grass never blocks a biome feature. 0 = grass only.\n");
        sb.append("#   HIGHER = denser biome plants/rocks by the water. Default 7.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_BIOME_BOOST_PERCENT).append('=').append(4).append("\n\n");
        sb.append("# Biome-boost blacklist: comma-separated placed-feature ids the biome-boost must NOT re-run near\n");
        sb.append("# water (e.g. \"terralith:cave_pumpkin_patch, minecraft:trees_birch\"). Also honoured: the datapack\n");
        sb.append("# tag #streamsreflowing:biome_boost_excluded. Default empty.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_BIOME_BOOST_BLACKLIST).append('=').append("").append("\n\n");
        sb.append("# Stream biome blacklist: biomes where streams and lakes are NOT carved at all. Comma-separated\n");
        sb.append("# biome ids (\"minecraft:mushroom_fields\"), biome tags (\"#c:is_dead\"), and whole-mod wildcards\n");
        sb.append("# (\"somemod:*\" or just \"somemod\"). Also honoured: the datapack tag #streamsreflowing:no_streams.\n");
        sb.append("# Carve-only: applies to NEW chunks. Default empty.\n");
        sb.append("# Perf: none (it only skips work).\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_STREAM_BIOME_BLACKLIST).append('=').append(D_STREAM_BIOME_BLACKLIST).append("\n\n");
        sb.append("# Bank-grass density (PERCENT): at the water's edge, the chance a near-water column gets a simple\n");
        sb.append("# grass / fern / flower; fades to 0 at floraRadius. (Independent of the biome-boost above.)\n");
        sb.append("#   HIGHER = lusher, grassier banks. 0 = bare banks (biome-boost only). Default 20.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_BANK_GRASS_PERCENT).append('=').append(20).append("\n\n");
        sb.append("# Underwater flora SCOPE -- entirely separate from vegetation/allWaterFlora/biomeBoostPercent above:\n");
        sb.append("# plants the vanilla RIVER biome's own underwater plants (seagrass and the like) onto submerged\n");
        sb.append("# floors, regardless of the water body's surrounding biome (a lake dug into a mountain has no water\n");
        sb.append("# plants of its own to copy). Only ever plants on a natural, water-plantable floor (see the datapack\n");
        sb.append("# tag #streamsreflowing:underwater_vegetation_floor) -- never on stone/ore.\n");
        sb.append("#   0 = off. 1 = our LAKES only. 2 = lakes AND our stream channels.\n");
        sb.append("#   3 = ALL bodies of water -- vanilla rivers, lakes, ponds and oceans too, not just ours.\n");
        sb.append("#  -1 = auto (follow preset: POTATO/LOW 2, MEDIUM/HIGH/EXTREME/MAX 3). Default -1 (auto).\n");
        sb.append("# Perf: light at generation.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_UNDERWATER_FLORA).append('=').append(-1).append("\n\n");
        sb.append("# Underwater flora density (PERCENT): the chance a valid submerged floor column (within whatever\n");
        sb.append("# scope underwaterFlora selects) gets a plant.\n");
        sb.append("#   HIGHER = denser underwater growth. 0 = bare floors (same as underwaterFlora=0). Default 15.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_UNDERWATER_FLORA_PERCENT).append('=').append(15).append("\n\n");
        sb.append("# Underwater-flora biome blacklist: biomes where the underwaterFlora pass plants NO seagrass.\n");
        sb.append("# Comma-separated biome ids (\"minecraft:desert\"), biome tags (\"#c:is_snowy\"), and whole-mod\n");
        sb.append("# wildcards (\"somemod:*\" or just \"somemod\"). Merged with the datapack tag\n");
        sb.append("# #streamsreflowing:no_underwater_flora, which SHIPS with snowy + desert biomes. New chunks only.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_UNDERWATER_FLORA_BIOME_BLACKLIST).append('=').append("").append("\n\n");
        sb.append("# Load chunks faster on packs that generate chunks in parallel. Has no effect otherwise.\n");
        sb.append("# Turn this OFF if world generation crashes, or if chunks stop loading around you.\n");
        sb.append("#  true = faster loading where supported (default).  false = always use the safe path.\n");
        sb.append("# Perf: IMPROVES chunk loading. Leave on unless diagnosing a problem.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_FAST_CHUNK_LOADING).append('=').append(true).append("\n\n");
        sb.append("# ===================== WATER & BOATS =====================\n");
        sb.append("# Runtime behaviour of water itself. Affects ALL water in the world, not just this mod's streams.\n\n");
        sb.append("# Let water spread across SOURCE-water surfaces so streams flow continuously over their own pools\n");
        sb.append("# instead of stalling -- smoother, more natural flow.\n");
        sb.append("#   true = on (default). false = fully vanilla water spreading.\n");
        sb.append("# Perf: light while playing. Affects how often water re-spreads.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_SPILL_OVER_WATER).append('=').append(true).append("\n\n");
        sb.append("# Limit 'water spills over water' to water this mod gives a current to.\n");
        sb.append("#   true  = only this mod's streams spread across a pool surface; all other water keeps vanilla\n");
        sb.append("#           spreading exactly as it is.\n");
        sb.append("#   false = every water block in the world spills over water (default, the classic behaviour).\n");
        sb.append("# Turn this on if you want the stream look without changing how water behaves anywhere else.\n");
        sb.append("# Perf: negligible either way.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_SPILL_STREAMS_ONLY).append('=').append(true).append("\n\n");
        sb.append("# Let boats float on flowing stream water instead of sinking when spilled water tops them (the current\n");
        sb.append("# still carries you downstream; SOURCE water can still submerge a boat).\n");
        sb.append("#   true = on (default). false = vanilla boat behaviour.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_BOATS_FLOAT).append('=').append(true).append("\n\n");
        sb.append("# Exempt water-dwelling mobs (fish, squid, dolphins, axolotls, glow squid, tadpoles, and\n");
        sb.append("# water-breathing mobs that spawn in water like drowned and guardians) from being pushed by stream\n");
        sb.append("# currents, so they hold their position in a stream instead of being swept downstream.\n");
        sb.append("#   true = water mobs ignore the current (default). false = the current carries them like anything else.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_WATER_MOBS_IGNORE_FLOW).append('=').append(true).append("\n\n");
        sb.append("# How fast the current may carry dropped items (blocks per second). Streams run deeper than a thin\n");
        sb.append("# vanilla spill, so items would otherwise race downstream; 3 matches the vanilla spilled-water pace.\n");
        sb.append("#   HIGHER = faster drift. Default 3.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_ITEM_DRIFT_SPEED).append('=').append(3.0).append("\n\n");
        sb.append("# How fast the current may carry Sable physics vessels (blocks per second). Only matters with the\n");
        sb.append("# Sable mod installed.\n");
        sb.append("#   0 = off: we neither carry vessels nor hold back any other mod's water push, so vessels behave\n");
        sb.append("#       exactly as the rest of your mods want them to. Default 30.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_VESSEL_DRIFT_SPEED).append('=').append(30.0).append("\n\n");
        sb.append("# Give streams (and, with flowVanillaRivers, vanilla rivers) a downstream current while keeping the\n");
        sb.append("# water as plain minecraft:water -- the flow direction is sent to the client per chunk and imposed at\n");
        sb.append("# render time, so nothing is a custom fluid. Because the block stays real water, shaders, particle mods,\n");
        sb.append("# texture packs, water sounds and gameplay mods (seasons/freezing, Create) all just work.\n");
        sb.append("#   true = impose flow on vanilla water (default). false = streams stay still water.\n");
        sb.append("# Perf: light. Builds a small current grid per chunk in the background.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_VANILLA_WATER_FLOW).append('=').append(true).append("\n\n");
        sb.append("# OLD-WORLD STREAM FLUID (dedicated servers only). Worlds generated on 2.3-2.4 era versions can\n");
        sb.append("# contain the mod's dedicated stream fluid; turn this ON so those blocks keep resolving. While\n");
        sb.append("# it is on, players WITHOUT the mod cannot join. Off (default), a server whose streams are plain\n");
        sb.append("# water accepts players who do not have the mod installed.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: takes effect after you restart the game (this one is set up as the game starts).\n");
        sb.append(K_LEGACY_STREAM_FLUID).append('=').append(false).append("\n\n");
        sb.append("# Give flat vanilla / terrain-mod rivers one coherent downstream current toward the sea via an\n");
        sb.append("# ocean-seeded biome flood, through the same per-chunk flow grid as our own streams. Rivers not\n");
        sb.append("# connected to an ocean within range stay still. New chunks only.\n");
        sb.append("#   true = flow vanilla rivers (default). false = leave still.\n");
        sb.append("# Perf: moderate background work when new areas load; nothing once built.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_FLOW_VANILLA_RIVERS).append('=').append(true).append("\n\n");
        sb.append("# Deep ReTerraForged integration, only when that mod is installed: streams follow its terrain and\n");
        sb.append("# connect into its rivers at any elevation, and its rivers gain a flowing current plus spill-over.\n");
        sb.append("#   true = integrate with ReTerraForged (default).  false = treat it like any other terrain mod.\n");
        sb.append("# Perf: faster stream generation on ReTerraForged worlds. No effect at all without ReTerraForged.\n");
        sb.append("# World: safe on an existing world; turning it off fully applies after leaving and re-entering.\n");
        sb.append(K_RETERRAFORGED_COMPAT).append('=').append(true).append("\n\n");
        sb.append("# Which biomes a stream is allowed to open its mouth into: a comma-separated list of biome TAGS (written\n");
        sb.append("# with a leading #) and plain biome ids. A mouth is accepted when any listed biome sits within one grid\n");
        sb.append("# cell of it, so water lying just outside the biome that owns it -- the last blocks of a river, the edge of\n");
        sb.append("# a swamp pool -- still counts. Leave it EMPTY to accept any body of water at all.\n");
        sb.append("# This mod's own lakes are never subject to it and end a stream whatever the list says.\n");
        sb.append("# A stream that cannot reach water it may join is not generated, so a shorter list means fewer streams.\n");
        sb.append("# Perf: none directly; a shorter list means fewer streams to build.\n");
        sb.append("# World: RESHAPES THE STREAM NETWORK -- new land will not line up with land already generated.\n");
        sb.append(K_OUTLET_BIOMES).append('=').append(D_OUTLET_BIOMES).append("\n\n");
        sb.append("# The Physics Mod draws its own effect over water that is flowing, so on a stream it lays a second,\n");
        sb.append("# shimmering animation over this mod's current.\n");
        sb.append("#   0 = leave it alone, and see both.\n");
        sb.append("#   1 = switch the Physics Mod's water effect off, leaving only the stream current (default). Its ocean\n");
        sb.append("#       waves are untouched.\n");
        sb.append("# This mod's own animation is never the one turned off: suppressing it would leave the water still, since\n");
        sb.append("# a stream is source water with no current of its own to fall back to.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save.\n");
        sb.append(K_PHYSICS_MOD_WATER).append('=').append(1).append("\n\n");
        sb.append("# Mobs that must HOLD position in a stream current, whatever the automatic rules decide: a\n");
        sb.append("# comma-separated list of entity ids (minecraft:cow, alexsmobs:crocodile). Use it for a modded\n");
        sb.append("# swimmer the game does not class as aquatic, or anything you would rather was not washed away.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save.\n");
        sb.append(K_MOBS_HOLD_IN_CURRENT).append("=\n\n");
        sb.append("# Mobs that must DRIFT with a stream current, whatever the automatic rules decide -- same format.\n");
        sb.append("# Use it for anything exempted by default that you would rather the water carried. A mob named in\n");
        sb.append("# both lists drifts.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save.\n");
        sb.append(K_MOBS_DRIFT_IN_CURRENT).append("=\n\n");
        sb.append("# When the Flowing Fluids mod is installed, fill stream channels with our own dedicated stream fluid\n");
        sb.append("# instead of plain water. Flowing Fluids makes vanilla water finite and lets it drain away, which\n");
        sb.append("# empties water-based streams; our stream fluid stays put and keeps its downstream current. Has no\n");
        sb.append("# effect unless Flowing Fluids is present. New chunks only.\n");
        sb.append("#   true = use the stream fluid under Flowing Fluids (default). false = always plain water.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_BAKED_FLUID_WFF).append('=').append(true).append("\n\n");
        sb.append("# ALWAYS fill stream channels with the mod's own dedicated stream water instead of plain water,\n");
        sb.append("# whatever other mods are present. For setups where something interferes with plain-water\n");
        sb.append("# streams -- water physics or finite-water mods draining them, or structures whose own water\n");
        sb.append("# spills into a channel: the stream fluid stays put, keeps its downstream current, and cannot\n");
        sb.append("# be displaced. While this is on, the 'water spills over water' behaviour applies ONLY to the\n");
        sb.append("# stream fluid: plain vanilla water everywhere keeps untouched vanilla spreading.\n");
        sb.append("# New chunks only. Players joining a server with this on need the mod installed.\n");
        sb.append("#   false = automatic (default): plain water; the stream fluid only under Flowing Fluids.\n");
        sb.append("# Perf: negligible.\n");
        sb.append("# World: safe on an existing world -- applies to chunks generated from now on, no restart needed. Land already generated keeps its current look.\n");
        sb.append(K_FORCE_BAKED_FLUID).append('=').append(false).append("\n\n");
        sb.append("# (Client-only particle settings are in the separate streamsreflowing-client.properties file.)\n\n");
        sb.append("# ===================== EXPERIMENTAL =====================\n");
        sb.append("# Off by default, rough edges. Most change the network (NEW chunks only).\n\n");
        sb.append("# Join a stream that peters out near ANOTHER stream (even in a neighbouring region) into it, so\n");
        sb.append("# cross-region streams that just miss each other link up. Only routes downhill/forward.\n");
        sb.append("#   MODERATE COST: a region must also build its neighbours. Sea/lake-bound streams already connect.\n");
        sb.append("#   true = on. false = off (default).\n");
        sb.append("# Perf: light at generation.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_CONNECT_NEARBY_WATER).append('=').append(false).append("\n\n");
        sb.append("# How far (blocks) a dead-end looks for another stream to join, when connectNearbyWater is on.\n");
        sb.append("#   HIGHER = bridges bigger gaps (less natural). LOWER = only near-touching streams. -1 = auto (24).\n");
        sb.append("# Perf: light at generation. Larger radius searches further for water.\n");
        sb.append("# World: CHANGES THE STREAM NETWORK. Takes effect only after the world is RELOADED (quit to title and reopen, or restart the server), and only in NEW chunks -- streams will not line up where new land meets old. Best chosen before starting a world.\n");
        sb.append(K_CONNECT_RADIUS).append('=').append(-1).append("\n\n");
        return sb.toString();
    }

    private static String defaultClientFileContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Streams Reflowing -- Fabric CLIENT configuration (particles / rendering)\n");
        sb.append("# These render on your client only; they have no effect on world generation and are read-but-unused\n");
        sb.append("# on a dedicated server. (On NeoForge/Forge these live in streamsreflowing-client.toml.)\n\n");
        sb.append("# Ambient particles on the stream fluid: bubbles riding the current + whitewater foam/spray at drops.\n");
        sb.append("#   0 = off.  1 = static (bubbles fade in place; cheapest).  2 = pathed (bubbles flow downstream and\n");
        sb.append("#   follow waterfalls down; default, priciest -- samples the world each tick).\n");
        sb.append("# Perf: CLIENT FPS. Turn down first if streams cost you frames.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_STREAM_BUBBLES).append('=').append(2).append("\n\n");
        sb.append("# How loud running water sounds. Streams murmur when you are beside them, and waterfalls carry --\n");
        sb.append("# a big fall can be heard from a long way off, growing from a distant roar into its full sound as\n");
        sb.append("# you approach. Rides the Ambient volume slider in your sound settings.\n");
        sb.append("#   0 = silent.  1 = normal (default).  Above 1 is louder.\n");
        sb.append("# Perf: client, light.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_WATER_AMBIENCE).append('=').append(1.0).append("\n\n");
        sb.append("# Hard cap on stream particles spawned PER TICK across all nearby stream water -- a safety valve so\n");
        sb.append("# heavy rivers/waterfalls can't flood the particle engine on weaker machines. Default high enough that\n");
        sb.append("# normal play never hits it; lower it if you're chasing frames.\n");
        sb.append("# Perf: CLIENT FPS -- the main particle cost dial. Lower = cheaper.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_MAX_STREAM_PARTICLES).append('=').append(3000).append("\n\n");
        sb.append("# How far out (blocks) a client-side scanner hunts for waterfalls to spray, BEYOND vanilla's ~16-block\n");
        sb.append("# ambient-particle range (distant waterfalls are otherwise silent/dry). Distance LOD keeps it cheap:\n");
        sb.append("# the further a waterfall, the FEWER but LARGER particles it fires.   0 = off. Higher = farther (pricier).\n");
        sb.append("# Perf: CLIENT FPS. A larger radius searches more blocks every tick.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_STREAM_PARTICLE_SCAN_RADIUS).append('=').append(256).append("\n\n");
        sb.append("# Apply the mod's waterfall whitewater (the distant LOD scanner's spray + foam) to VANILLA water as\n");
        sb.append("# well -- rivers, lakes, vanilla waterfalls -- not just this mod's own streams.\n");
        sb.append("#   true = on (default).  false = only this mod's streams get the whitewater.\n");
        sb.append("# Perf: CLIENT FPS. Extends particle work to all water, not just streams.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_PARTICLES_ON_VANILLA_WATER).append('=').append(true).append("\n\n");
        sb.append("# Floating debris on the stream surface: sticks and (on 1.21.4+) leaves drifting downstream with the\n");
        sb.append("# current. Purely visual and client-side.\n");
        sb.append("#   true = on (default).  false = no sticks or leaves (bubbles and whitewater are unaffected).\n");
        sb.append("# Perf: client, light.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_STREAM_DEBRIS).append('=').append(true).append("\n\n");
        sb.append("# PARTICLE SIZE -- how big each droplet of spray is, as a multiplier.\n");
        sb.append("#   1.0 = normal. 2.0 = twice the size. 0.5 = fine mist.\n");
        sb.append("# Spray DENSITY follows size automatically, in inverse square: bigger droplets means proportionally\n");
        sb.append("# fewer of them, so a waterfall throws the same total amount of spray whatever you set here -- the\n");
        sb.append("# same rule the distance detail already uses, so far falls keep matching near ones.\n");
        sb.append("# Perf: CLIENT FPS, but only mildly -- bigger particles are also rarer, so the count stays about the same.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_PARTICLE_SIZE).append('=').append(1.0).append("\n\n");
        sb.append("# PARTICLE DENSITY -- how much spray is thrown, as a multiplier.\n");
        sb.append("#   1.0 = normal. 2.0 = twice as much water in the air. 0.0 = none.\n");
        sb.append("# This is the dial to turn DOWN for frames, and it is independent of size: the size setting keeps the\n");
        sb.append("# total amount of spray constant, this setting is what changes it.\n");
        sb.append("# Perf: CLIENT FPS -- the most direct particle cost dial there is.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_PARTICLE_RATE).append('=').append(1.0).append("\n\n");
        sb.append("# Show the Streams Reflowing progress line on the world-loading screen while your waterways are\n");
        sb.append("# prepared during world creation. Purely visual and client-side.\n");
        sb.append("#   true = show (default).  false = hide it.\n");
        sb.append("# Perf: none.\n");
        sb.append("# World: safe on an existing world -- applies the moment you save, everywhere.\n");
        sb.append(K_LOADING_NOTICE).append('=').append(true).append("\n\n");
        return sb.toString();
    }

    private static boolean parseBool(Properties props, String key, boolean def) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        if ((v = v.trim()).equalsIgnoreCase("true")) {
            return true;
        }
        if (v.equalsIgnoreCase("false")) {
            return false;
        }
        return def;
    }

    private static int parseMode(Properties props, String key, int def, int max) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        if ((v = v.trim()).equalsIgnoreCase("false")) {
            return 0;
        }
        if (v.equalsIgnoreCase("true")) {
            return 2;
        }
        try {
            int n = Integer.parseInt(v);
            return n < 0 || n > max ? def : n;
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(Properties props, String key, int def) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private static double parseDouble(Properties props, String key, double def) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(v.trim());
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private static QualityPreset parsePreset(String v) {
        if (v == null) {
            return D_QUALITY_PRESET;
        }
        try {
            return QualityPreset.valueOf(v.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            return D_QUALITY_PRESET;
        }
    }

    private static int parsePlateSize(String v) {
        if (v == null || v.trim().equalsIgnoreCase("auto") || v.trim().isEmpty()) {
            return -1;
        }
        try {
            int n = Integer.parseInt(v.trim());
            return n < 0 ? -1 : Math.max(256, Math.min(8192, n));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean generateStreams() {
        return generateStreams;
    }

    public static boolean generateLakes() {
        return generateLakes;
    }

    public static boolean generateRivers() {
        return generateStreams || generateLakes;
    }

    public static int minStreamLength() {
        return minStreamLength;
    }

    public static int riverDrainageSize() {
        return riverDrainageSize;
    }

    public static QualityPreset qualityPreset() {
        return qualityPreset;
    }

    public static boolean reterraforgedCompat() {
        return reterraforgedCompat;
    }

    public static int physicsModWater() {
        return physicsModWater;
    }

    public static String outletBiomes() {
        return outletBiomes;
    }

    public static boolean fastOceanSkip() {
        return true;
    }

    public static int gridSpacing() {
        return StreamsConfigFabric.qualityPreset.gridSpacing;
    }

    public static int plateScale() {
        return plateSizeOverride >= 0 ? plateSizeOverride : StreamsConfigFabric.qualityPreset.plateScale;
    }

    public static int terrainSampleSpacing() {
        return StreamsConfigFabric.qualityPreset.terrainSampleSpacing;
    }

    public static int valleySnapRadius() {
        return StreamsConfigFabric.qualityPreset.valleySnapRadius;
    }

    public static int prefetchRadius() {
        return StreamsConfigFabric.qualityPreset.prefetchRadius;
    }

    public static boolean clearFloatingSnow() {
        return StreamsConfigFabric.qualityPreset.clearFloatingSnow;
    }

    public static double wetnessMultiplier() {
        return wetnessMultiplier;
    }

    public static double wetnessDryFloor() {
        return wetnessDryFloor;
    }

    public static double growDensity() {
        return growDensity;
    }

    public static double lakeMinArea() {
        return lakeMinArea;
    }

    public static double lakeMaxArea() {
        return lakeMaxArea;
    }

    public static double widthScale() {
        return widthScale;
    }

    public static double baseWidth() {
        return baseWidth;
    }

    public static double maxWidth() {
        return maxWidth;
    }

    public static double meanderStrength() {
        return meanderStrength;
    }

    public static double streamDepth() {
        return streamDepth;
    }

    public static boolean containmentBerm() {
        return containmentBerm;
    }

    public static int vegetation() {
        return vegetation >= 0 ? vegetation : StreamsConfigFabric.qualityPreset.vegetation;
    }

    public static int allWaterFlora() {
        return allWaterFlora >= 0 ? allWaterFlora : StreamsConfigFabric.qualityPreset.allWaterFlora;
    }

    public static int allWaterBanks() {
        return allWaterBanks >= 0 ? allWaterBanks : StreamsConfigFabric.qualityPreset.allWaterBanks;
    }

    public static boolean customBanks() {
        return customBanks;
    }

    public static boolean fastTerrainEstimate() {
        return true;
    }

    public static int terrainSamplingMode() {
        return StreamsConfigFabric.qualityPreset.terrainSamplingMode;
    }

    public static int floraRadius() {
        return Math.max(1, floraRadius);
    }

    public static int biomeBoostPercent() {
        return Math.max(0, Math.min(100, biomeBoostPercent));
    }

    public static String biomeBoostBlacklist() {
        return biomeBoostBlacklist;
    }

    public static String mobsHoldInCurrent() {
        return mobsHoldInCurrent;
    }

    public static String mobsDriftInCurrent() {
        return mobsDriftInCurrent;
    }

    public static String streamBiomeBlacklist() {
        return streamBiomeBlacklist;
    }

    public static int bankGrassPercent() {
        return Math.max(0, Math.min(100, bankGrassPercent));
    }

    public static int underwaterFlora() {
        return underwaterFlora >= 0 ? Math.min(3, underwaterFlora) : StreamsConfigFabric.qualityPreset().underwaterFlora;
    }

    public static int underwaterFloraPercent() {
        return Math.max(0, Math.min(100, underwaterFloraPercent));
    }

    public static String underwaterFloraBiomeBlacklist() {
        return underwaterFloraBiomeBlacklist;
    }

    public static boolean fastChunkLoading() {
        return fastChunkLoading;
    }

    public static boolean spillOverWater() {
        return spillOverWater;
    }

    public static boolean spillOnStreamsOnly() {
        return spillOnStreamsOnly;
    }

    public static boolean boatsFloatOnFlowingWater() {
        return boatsFloatOnFlowingWater;
    }

    public static boolean waterMobsIgnoreFlow() {
        return waterMobsIgnoreFlow;
    }

    public static double itemDriftSpeed() {
        return itemDriftSpeed;
    }

    public static double vesselDriftSpeed() {
        return vesselDriftSpeed;
    }

    public static boolean vanillaWaterFlow() {
        return vanillaWaterFlow;
    }

    private static boolean flowingFluidsLoaded() {
        if (flowingFluidsLoaded == null) {
            try {
                flowingFluidsLoaded = FabricLoader.getInstance().isModLoaded("flowing_fluids");
            }
            catch (Throwable t) {
                flowingFluidsLoaded = false;
            }
        }
        return flowingFluidsLoaded;
    }

    public static boolean bakedStreamFluid() {
        return forceBakedFluid || bakedFluidWhenFlowingFluids && StreamsConfigFabric.flowingFluidsLoaded();
    }

    public static boolean forceBakedFluid() {
        return forceBakedFluid;
    }

    public static boolean legacyStreamFluid() {
        return legacyStreamFluid;
    }

    public static boolean flowVanillaRivers() {
        return flowVanillaRivers;
    }

    public static double waterAmbience() {
        return Math.max(0.0, Math.min(2.0, waterAmbience));
    }

    public static int streamBubbles() {
        return Math.max(0, Math.min(2, streamBubbles));
    }

    public static int maxStreamParticles() {
        return Math.max(0, maxStreamParticles);
    }

    public static int streamParticleScanRadius() {
        return Math.max(0, Math.min(512, streamParticleScanRadius));
    }

    public static boolean particlesOnVanillaWater() {
        return particlesOnVanillaWater;
    }

    public static boolean streamDebris() {
        return streamDebris;
    }

    public static double particleSize() {
        return particleSize;
    }

    public static double particleRate() {
        return particleRate;
    }

    public static boolean loadingNotice() {
        return loadingNotice;
    }

    public static boolean confineToBanks() {
        return false;
    }

    public static boolean connectNearbyWater() {
        return connectNearbyWater;
    }

    public static int connectRadius() {
        return connectRadius >= 8 ? connectRadius : 24;
    }

    public static boolean sinkRooted() {
        return false;
    }

    public static int rebiomeStreams() {
        return rebiomeStreams;
    }

    public static int rebiomeLakes() {
        return rebiomeLakes;
    }

    public static boolean welcomeMessage() {
        return welcomeMessage;
    }

    private static String digestOf(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            MessageDigest md = MessageDigest.getInstance("MD5");
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        }
        catch (Exception e) {
            return "";
        }
    }

    private static void recordDigests() {
        Path dir = FabricLoader.getInstance().getConfigDir();
        LOADED_DIGEST.put(FILE_NAME, StreamsConfigFabric.digestOf(dir.resolve(FILE_NAME)));
        LOADED_DIGEST.put(FILE_NAME_CLIENT, StreamsConfigFabric.digestOf(dir.resolve(FILE_NAME_CLIENT)));
    }

    private static boolean changedOnDisk() {
        Path dir = FabricLoader.getInstance().getConfigDir();
        for (String name : new String[]{FILE_NAME, FILE_NAME_CLIENT}) {
            String now = StreamsConfigFabric.digestOf(dir.resolve(name));
            if (now.isEmpty() || now.equals(LOADED_DIGEST.get(name))) continue;
            return true;
        }
        return false;
    }

    public static synchronized void startWatching() {
        if (watchThread != null) {
            return;
        }
        StreamsConfigFabric.recordDigests();
        Thread t = new Thread(() -> {
            Path dir = FabricLoader.getInstance().getConfigDir();
            try (WatchService ws = dir.getFileSystem().newWatchService();){
                dir.register(ws, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = ws.take();
                    boolean mine = false;
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        Object ctx = ev.context();
                        if (!(ctx instanceof Path)) continue;
                        Path name = (Path)ctx;
                        String n = name.getFileName().toString();
                        mine |= FILE_NAME.equals(n) || FILE_NAME_CLIENT.equals(n);
                    }
                    if (!key.reset()) {
                        break;
                    }
                    if (!mine) continue;
                    Thread.sleep(300L);
                    if (!StreamsConfigFabric.changedOnDisk()) continue;
                    StreamsConfigFabric.load();
                    StreamsConfigFabric.recordDigests();
                    Constants.LOG.info("Streams Reflowing: config reloaded from disk.");
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                Constants.LOG.debug("Streams Reflowing: config file watching unavailable ({}).", (Object)e.toString());
            }
        }, "streamsreflowing-config-watch");
        t.setDaemon(true);
        t.start();
        watchThread = t;
    }

    static {
        LOADED_DIGEST = new ConcurrentHashMap<String, String>();
    }
}

