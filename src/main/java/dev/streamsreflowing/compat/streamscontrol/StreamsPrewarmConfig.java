package dev.streamsreflowing.compat.streamscontrol;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StreamsPrewarmConfig(
        boolean enabled,
        boolean runtimeEnabled,
        boolean gpuTerrainSampling,
        int startupRadiusChunks,
        int runtimeRadiusChunks,
        int runtimeStrideChunks,
        int runtimeScanIntervalTicks,
        int startupSamplerThreads,
        int runtimeSamplerThreads,
        int startupPlateWorkers,
        // River height gate. It is not a prewarm knob, but this properties file is the
        // mod's single persisted configuration and the config screen is the only place a
        // player can reach, so the gate lives here instead of in a second file that would
        // need its own screen section. Default 140 reproduces the previous constant in
        // StreamsRiverHeightGateMixin exactly.
        int maxRiverPlanY) {
    /**
     * Configuration safety bound, not a Streams/gameplay radius limit.
     * The prewarm implementation materializes every plate and Future up front,
     * so accepting arbitrary integers would turn a typo into an OOM.
     */
    // No longer the old 512-chunk gameplay limit. This is only an input guard;
    // the actual workload is bounded dynamically by MAX_PREWARM_PLATES below.
    public static final int MAX_PREWARM_RADIUS_CHUNKS = 8192;
    public static final int MAX_PREWARM_PLATES = 300_000;
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams Safety");
    private static final String FILE_NAME = "plaincraft-streams-prewarm.properties";
    private static final String DEFAULT_FILE = """
            # PlainCraft Streams Reflowing prewarm configuration.
            # Restart the game or server after changing this file.
            enabled=true

            # Streams GPU terrain sampling is disabled by PlainCraft policy.
            # OpenCL is reserved for C2ME and RoadWeaver.
            gpuTerrainSampling=false

            # Exact river plans are built for this radius around world spawn before entry.
            # This does not generate Minecraft chunks. 0 disables startup prewarm.
            # The old 512-chunk UI cap is removed; an internal plate-count guard prevents OOM.
            startupRadiusChunks=128

            # During play, one low-priority owner gradually keeps this radius ready around players.
            # 0 disables runtime prewarm.
            runtimeRadiusChunks=128

            # Master switch for the in-play background river planning (runtime prewarm).
            # This does NOT affect startup prewarm. Set false to stop all in-play planning.
            runtimeEnabled=true

            # Re-scan after a player moves this many chunks from the last scheduled center.
            # 0.1.68：32 -> 128（与 RoadWeaver 的 followStep 同口径）。32 区块 = 512 方块太短，
            # 游玩中几乎一直在重扫；且本值同时是「移动触发距离」，放大后移动不再频繁触发。
            runtimeStrideChunks=128

            # Runtime re-scan interval. 1200 ticks = 60 seconds.
            # 0.1.68：100(5 秒) -> 1200(60 秒)，与 RoadWeaver 的动态规划检查同口径。
            # 真机取证：5 秒一次的扫描叠加 CPU 侧地形采样，是「一移动就掉帧」与
            # 「突然高占用」（Streams 分族峰值 10.73 核）的主因。
            runtimeScanIntervalTicks=1200

            # 0 selects a CPU/heap-adaptive startup value and keeps coordination capacity free.
            startupSamplerThreads=0

            # Terrain sampler workers used after world entry.
            runtimeSamplerThreads=2

            # 0 automatically scales concurrent plate owners with the sampler pool.
            startupPlateWorkers=0

            # River height gate: a river plan whose water surface or bed sits above this Y
            # is reported as absent, so the worldgen and RoadWeaver see plain terrain with
            # no riverbed to avoid or bridge. 140 is the long-standing value and is the
            # default; raise it only together with the world-height ceiling.
            maxRiverPlanY=140
            """;
    private static volatile StreamsPrewarmConfig current = defaults();

    public static StreamsPrewarmConfig current() {
        return current;
    }

    public static StreamsPrewarmConfig reload() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            current = from(properties);
            LOGGER.info("Streams prewarm config: enabled={}, gpuTerrainSampling={}, runtimeEnabled={}, "
                            + "startupRadius={} chunks, runtimeRadius={} chunks, runtimeStride={} chunks, "
                            + "scanInterval={} ticks, startupSamplers={}, runtimeSamplers={}, plateWorkers={}, "
                            + "maxRiverPlanY={}, processors={}, maxHeapMiB={}.",
                    current.enabled(), current.gpuTerrainSampling(), current.runtimeEnabled(),
                    current.startupRadiusChunks(), current.runtimeRadiusChunks(),
                    current.runtimeStrideChunks(), current.runtimeScanIntervalTicks(),
                    current.startupSamplerThreads(),
                    current.runtimeSamplerThreads(), current.startupPlateWorkers(),
                    current.maxRiverPlanY(),
                    Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() / (1024L * 1024L));
        } catch (IOException | RuntimeException failure) {
            current = defaults();
            LOGGER.warn("Could not load {}; using defaults", path, failure);
        }
        return current;
    }

    public static synchronized StreamsPrewarmConfig saveUiSettings(int startupRadiusChunks,
                                                                   int runtimeRadiusChunks,
                                                                   boolean gpuTerrainSampling,
                                                                   boolean runtimeEnabled,
                                                                   Map<String, String> extraProperties) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        int startup = clampRadius(startupRadiusChunks);
        int runtime = clampRadius(runtimeRadiusChunks);
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
            String text = Files.readString(path, StandardCharsets.UTF_8);
            text = replaceProperty(text, "gpuTerrainSampling", Boolean.toString(gpuTerrainSampling));
            text = replaceProperty(text, "runtimeEnabled", Boolean.toString(runtimeEnabled));
            text = replaceProperty(text, "startupRadiusChunks", Integer.toString(startup));
            text = replaceProperty(text, "runtimeRadiusChunks", Integer.toString(runtime));
            // 0.1.81: the performance page's keys used to be file-only. They are written
            // through the same "replace when present, append when missing" path as the
            // radii; the caller owns the key names and the clamping, this method never
            // invents a value.
            if (extraProperties != null) {
                for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) continue;
                    text = replaceProperty(text, entry.getKey(), entry.getValue());
                }
            }
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            LOGGER.warn("Could not save Streams prewarm settings to {}", path, failure);
        }
        return reload();
    }

    /**
     * Raw integer straight from the properties file. Needed by the config screen for the
     * two "0 means auto" knobs: {@link #from} resolves them through
     * StreamsStartupHardwareProfile, so the record only carries the resolved number and
     * the screen would otherwise display (and later write back) the auto-picked value,
     * silently turning "auto" into a fixed number.
     */
    public static int rawInt(String key, int fallback) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.notExists(path)) return fallback;
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return integer(properties, key, fallback, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } catch (IOException | RuntimeException failure) {
            return fallback;
        }
    }

    private static String replaceProperty(String text, String key, String value) {
        String source = text == null ? "" : text;
        String line = key + "=" + value;
        Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(key) + "\\s*=.*$");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) return matcher.replaceFirst(Matcher.quoteReplacement(line));
        String separator = source.isEmpty() || source.endsWith("\n") ? "" : System.lineSeparator();
        return source + separator + line + System.lineSeparator();
    }

    private static StreamsPrewarmConfig from(Properties properties) {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int configuredStartupThreads = integer(properties, "startupSamplerThreads", 0, 0, processors);
        int configuredPlateWorkers = integer(properties, "startupPlateWorkers", 0, 0,
                Math.min(12, processors));
        StreamsStartupHardwareProfile startupProfile = StreamsStartupHardwareProfile.current(
                configuredStartupThreads, configuredPlateWorkers);
        int runtimeRadius = integer(properties, "runtimeRadiusChunks", 128, 0,
                MAX_PREWARM_RADIUS_CHUNKS);
        // 0.1.68：默认 32 -> 128，上限仍是 MAX_PREWARM_RADIUS_CHUNKS。
        int runtimeStride = integer(properties, "runtimeStrideChunks", 128, 1,
                MAX_PREWARM_RADIUS_CHUNKS);
        if (runtimeRadius > 0) runtimeStride = Math.min(runtimeStride, runtimeRadius);
        return new StreamsPrewarmConfig(
                bool(properties, "enabled", true),
                bool(properties, "runtimeEnabled", true),
                // PlainCraft policy：默认关闭 Streams 的 GPU 地形采样，OpenCL 留给
                // C2ME 与 RoadWeaver。改 false 的原因见 DEFAULT_FILE 内注释
                // （2026-09-18 AMD RX 9070 XT 上的 GPU 挂起 + 0xC0000005 崩溃）。
                // 旧版默认是 true；若用户配置文件里显式写了 gpuTerrainSampling=true，
                // 仍以用户配置为准（autotune 会在 preLaunch 强制写回 false）。
                bool(properties, "gpuTerrainSampling", false),
                integer(properties, "startupRadiusChunks", 128, 0,
                        MAX_PREWARM_RADIUS_CHUNKS),
                runtimeRadius,
                runtimeStride,
                // 0.1.68：默认 100(5 秒) -> 1200(60 秒)；上限本就是 1200，无需改动。
                integer(properties, "runtimeScanIntervalTicks", 1200, 20, 1200),
                startupProfile.samplerThreads(),
                integer(properties, "runtimeSamplerThreads", 2, 1, Math.min(8, processors)),
                startupProfile.plateWorkers(),
                integer(properties, "maxRiverPlanY", 140, -64, 3200));
    }

    private static StreamsPrewarmConfig defaults() {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        StreamsStartupHardwareProfile startupProfile = StreamsStartupHardwareProfile.current(0, 0);
        // 0.1.68：runtimeStride 32 -> 128，runtimeScanIntervalTicks 100 -> 1200（与 DEFAULT_FILE 一致）。
        return new StreamsPrewarmConfig(true, true, true, 128, 128, 128, 1200,
                startupProfile.samplerThreads(), 2, startupProfile.plateWorkers(), 140);
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String raw = properties.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }

    private static int integer(Properties properties, String key, int fallback, int min, int max) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clampRadius(int radius) {
        return Math.max(0, Math.min(MAX_PREWARM_RADIUS_CHUNKS, radius));
    }
}
