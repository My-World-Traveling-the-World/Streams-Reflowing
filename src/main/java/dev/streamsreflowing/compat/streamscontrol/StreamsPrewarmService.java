package dev.streamsreflowing.compat.streamscontrol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.api.outgoing.RiverReadinessBroadcast;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StreamsPrewarmService {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams Safety");
    private static final double CHUNK_DEFER_MARGIN_BLOCKS = 48.0D;
    private static final int RUNTIME_QUEUE_CAPACITY = 256;
    /**
     * 0.1.73：运行时扫描的相位偏移（tick）。
     *
     * <p>RoadWeaver 的后台规划也从服务端启动起按 1200 tick 周期触发
     * （ServerPlanningHooks 的 {@code tick++ % 1200 == 0}，首次在 tick=0）。
     * 本服务的 {@code SERVER_TICKS.incrementAndGet() % 1200} 首次在 tick=1200，
     * 与 RoadWeaver 自第二个周期起完全同帧 —— 两者在同 tick 叠加玩家回填 +
     * 覆盖推进 + 河流 plate 反射枚举，是游玩掉帧的一个来源。加 600 tick 偏移
     * 把本服务的扫描错开到半周期（600/1800/3000...），与 RoadWeaver 的
     * 0/1200/2400... 完全错开，消除同帧双重扫描。</p>
     */
    private static final long SCAN_PHASE_OFFSET = 600L;
    /**
     * 0.1.73：负载探测与运行时容量刷新的降频周期（tick）。
     *
     * <p>原来 {@code recommendedThreads()} / {@code refreshRuntimeCapacity()} /
     * {@code isTravelPaused()} 每 tick 执行（约 20Hz 的反射 Method.invoke +
     * synchronized），且不受 60 秒扫描节流约束。降到每 20 tick（约 1 秒）一次，
     * 消除每 tick 的持续微卡；Chunky 独占仍保持每 tick 检查以保证及时让路。</p>
     */
    private static final long CAPACITY_REFRESH_TICKS = 20L;
    /**
     * Runtime 预暖的单次扫描最多入队 plate 数与软队列深度上限。历史事故:
     * 玩家移动触发一次 scan 就把 57 个 plate 全塞进队列,而 owner 被压力
     * 限流压到 1,脏 chunk 越积越多,撞上 autosave 单次写盘 132~251 秒。
     * 这里把每轮入队量与在途量都封顶,让消化进度追上产生速度,避免保存时
     * 一次性背几十个 plate 的脏数据。
     */
    private static final int MAX_RUNTIME_PLATES_PER_SCAN = 8;
    private static final int MAX_RUNTIME_QUEUE_DEPTH = 16;
    /**
     * 0.1.69：运行期容量变更的迟滞窗口（纳秒）。
     *
     * <p>消除真机实测的 5⇄3 每秒振荡（15:59 一分钟 40 次、全程 84 次）。
     * 上升要求目标值连续保持 2 秒才采纳，下降只要求 1 秒 —— 下降更快是为了
     * 在真正出现压力时能及时让出资源，同时仍足以滤掉负载的瞬时抖动。</p>
     */
    private static final long RUNTIME_CAPACITY_RISE_HOLD_NANOS = 2_000_000_000L;
    private static final long RUNTIME_CAPACITY_FALL_HOLD_NANOS = 1_000_000_000L;
    /** 最近一次观察到的目标容量，以及它开始保持的时刻（迟滞用）。 */
    private static volatile int lastRuntimeCapacityTarget = -1;
    private static volatile long lastRuntimeCapacityTargetSinceNanos = 0L;
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final AtomicLong SERVER_TICKS = new AtomicLong();
    /** 保存窗口深度计数:>0 表示服务端正在写盘(含嵌套调用)。 */
    private static final AtomicInteger SAVING_DEPTH = new AtomicInteger();
    private static final ConcurrentHashMap<ServerLevel, WorldState> STATES = new ConcurrentHashMap<>();
    private static volatile Access access;
    private static volatile boolean accessInitialized;
    private static volatile ExecutorService startupExecutor;
    private static volatile int startupExecutorWorkers;
    private static final ThreadPoolExecutor RUNTIME_EXECUTOR = new ThreadPoolExecutor(
            1, StreamsRuntimeLoadPolicy.hardMaximumThreads(), 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(RUNTIME_QUEUE_CAPACITY),
            new NamedThreadFactory("Streams-Runtime-Prewarm-", Thread.MIN_PRIORITY),
            new ThreadPoolExecutor.AbortPolicy());

    static {
        RUNTIME_EXECUTOR.allowCoreThreadTimeOut(true);
    }

    private StreamsPrewarmService() {
    }

    public static void beginStartup(ServerLevel level) {
        if (level == null) return;
        BlockPos spawn = level.getSharedSpawnPos();
        beginStartup(level, spawn.getX(), spawn.getZ());
    }

    public static void beginStartup(ServerLevel level, int centerX, int centerZ) {
        if (!isOverworld(level)) return;
        WorldState state = STATES.computeIfAbsent(level, ignored -> new WorldState());
        if (!state.startup.begin()) return;
        try {
            StreamsPrewarmConfig config = StreamsPrewarmConfig.reload();
            if (!config.enabled() || config.startupRadiusChunks() <= 0) {
                state.startup.disabled();
                return;
            }
            // 对外只交付本轮真实请求的半径，不能把加载后修改的 UI 数值冒充已预热范围。
            state.macroRadiusBlocks = config.startupRadiusChunks() * 16;
            state.startupWork.enter();
            EngineContext context = context(level);
            if (context == null) {
                // 启用预热却没有引擎是失败，不能当成没有前置工作。
                throw new IllegalStateException("Streams startup engine unavailable");
            }
            List<PlateRef> plates = enumeratePlates(context, centerX, centerZ,
                    config.startupRadiusChunks());
            StreamsSamplerModeController.ensureInitialPlateCacheCapacity(plates.size());
            ExecutorService executor = startupExecutor(config.startupPlateWorkers());
            // 第一段：并行探测磁盘缓存。上一局预热完成时每块板都已增量落盘
            // （saves/<world>/streamsreflowing/<dim>/r.<cx>.<cz>.srn），这里把它们读回内存，
            // 既计入 alreadyReady，也不再进入第二段的 build —— 第二次进同一存档就是 0 次构建。
            // 在磁盘缓存探测前点亮卡片，缓存全命中仍能看到真实等待。
            state.startupWork.beginCache(plates.size(), config.startupPlateWorkers());
            int[] states = primePlateStates(context, plates, executor, state.startupWork);
            int ready = 0;
            int restoredFromDisk = 0;
            List<PlateRef> missing = new ArrayList<>();
            for (int index = 0; index < plates.size(); index++) {
                if (states[index] > 0) {
                    ready++;
                    if (states[index] == 2) restoredFromDisk++;
                } else {
                    missing.add(plates.get(index));
                }
            }
            final int alreadyReady = ready;
            final int restored = restoredFromDisk;
            LOGGER.info("Streams startup prewarm scheduled: center=({},{}), radius={} chunks, "
                            + "plates={}, alreadyReady={}, restoredFromDisk={}, buildOwners={}, "
                            + "samplerThreads={}, plateScale={}.",
                    centerX, centerZ, config.startupRadiusChunks(), plates.size(), alreadyReady,
                    restored, config.startupPlateWorkers(), config.startupSamplerThreads(),
                    context.plateScale());
            // 阶段切换保留会话起点，命中缓存也计入已就绪板块。
            state.startupWork.progress(plates.size(), alreadyReady, config.startupPlateWorkers());
            long started = System.nanoTime();
            int progressStep = Math.max(1, plates.size() / 10);
            List<CompletableFuture<Void>> futures = new ArrayList<>(missing.size());
            for (PlateRef plate : missing) {
                CompletableFuture<Void> future = state.startupWork.submit(
                        executor, () -> build(context, plate.key()));
                // allOf 等计数回调结束，避免失败结果先封口、最后一块错误计数随后被丢弃。
                CompletableFuture<Void> tracked = future.whenComplete((ignored, failure) -> {
                    int done = state.startupWork.completedBuild(failure == null);
                    if (done >= 0 && (done == plates.size() || done % progressStep == 0)) {
                        LOGGER.info("Streams startup prewarm progress: {}/{} plates, elapsedMs={}.",
                                done, plates.size(), elapsedMillis(started));
                    }
                });
                futures.add(tracked);
            }
            CompletableFuture<Void> combined = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            state.startup.completeWhen(combined.whenComplete((ignored, failure) -> {
                state.startupWork.finish(failure == null);
                if (failure == null) {
                    LOGGER.info("Streams startup prewarm complete: radius={} chunks, plates={}, "
                                    + "alreadyReady={}, restoredFromDisk={}, built={}, elapsedMs={}.",
                            config.startupRadiusChunks(), plates.size(), alreadyReady, restored,
                            missing.size(), elapsedMillis(started));
                } else {
                    LOGGER.warn("Streams startup prewarm finished with an error after {} ms",
                            elapsedMillis(started), unwrap(failure));
                }
            }));
        } catch (Throwable failure) {
            // 同步初始化与异步构建失败都必须留下结果，防止下游误报 ready。
            state.startup.failed(unwrap(failure));
            state.startupWork.failAndClose();
            LOGGER.warn("Could not start Streams startup prewarm", unwrap(failure));
        }
    }

    public static void awaitStartup(ServerLevel level) {
        WorldState state = STATES.get(level);
        if (state != null) state.startup.await();
    }

    /** 仅查询现有状态；不创建引擎、世界状态或新的后台任务。 */
    public static dev.streamsreflowing.api.RiverPlanApi.Startup startupStatus(ServerLevel level) {
        WorldState state = STATES.get(level);
        return state == null
                ? new dev.streamsreflowing.api.RiverPlanApi.Startup(0,
                        dev.streamsreflowing.api.RiverPlanApi.Phase.NOT_STARTED, "本世界尚无启动会话")
                : state.startup.snapshot();
    }

    public static int completedRadiusBlocks(ServerLevel level) {
        var state = STATES.get(level);
        return state != null && state.startup.snapshot().phase() == dev.streamsreflowing.api.RiverPlanApi.Phase.COMPLETE
                ? state.macroRadiusBlocks : 0;
    }

    /** 结果门属于河流发布方；保留旧 awaitStartup 签名以兼容已有调用者。 */
    public static void publishStartupReadiness(ServerLevel level) {
        WorldState state = STATES.get(level);
        if (state == null) {
            LOGGER.warn("STREAMS_STARTUP_NOT_READY: outcome=NOT_STARTED");
            return;
        }
        try {
            RiverStartupReadiness.Outcome outcome = state.startup.publishIfReady(() -> {
                int count = RiverReadinessBroadcast.publishReady(RiverReadinessBroadcast.PROVIDER_STREAMS);
                LOGGER.info("STREAMS_STARTUP_READY: subscribers={}", count);
            });
            // 禁用代表用户没有要求预热，不应永久挡住后续模组；日志与真实完成分开。
            if (outcome == RiverStartupReadiness.Outcome.DISABLED) {
                LOGGER.info("STREAMS_STARTUP_BYPASSED: prewarm disabled by configuration");
            } else if (outcome != RiverStartupReadiness.Outcome.COMPLETE) {
                LOGGER.warn("STREAMS_STARTUP_NOT_READY: outcome={}", outcome);
            }
        } catch (Throwable failure) {
            LOGGER.warn("Streams could not broadcast river readiness", unwrap(failure));
        }
    }

    public static void tick(MinecraftServer server) {
        StreamsPrewarmConfig config = StreamsPrewarmConfig.current();
        if (!config.enabled() || !config.runtimeEnabled() || config.runtimeRadiusChunks() <= 0) return;
        // 0.1.73：Chunky 独占仍每 tick 检查（及时让路）；普通游玩下负载探测与
        // 容量刷新降频到每 20 tick（约 1 秒）一次，消除每 tick 的反射+synchronized 微卡。
        boolean chunkyExclusive = StreamsSamplerModeController.isChunkyExclusive();
        long tick = SERVER_TICKS.incrementAndGet();
        boolean refreshTick = chunkyExclusive || (tick % CAPACITY_REFRESH_TICKS == 0L);
        if (refreshTick) {
            if (chunkyExclusive || StreamsRuntimeLoadPolicy.recommendedThreads() <= 0) return;
            refreshRuntimeCapacity();
            if (StreamsRuntimeLoadPolicy.isTravelPaused()) return;
        } else if (chunkyExclusive) {
            return;
        }
        // 0.1.73：相位偏移 600 tick，把 60 秒扫描错开到 RoadWeaver 的半周期，
        // 避免两者同帧双重扫描（见 SCAN_PHASE_OFFSET 注释）。
        if ((tick + SCAN_PHASE_OFFSET) % config.runtimeScanIntervalTicks() != 0L) return;
        ServerLevel level = server.overworld();
        if (!isOverworld(level)) return;
        try {
            EngineContext context = context(level);
            if (context == null) return;
            WorldState state = STATES.computeIfAbsent(level, ignored -> new WorldState());
            int scheduled = 0;
            Set<Object> activeCoverage = new HashSet<>();
            // 0.1.73：多玩家按「已扫描中心」去重，同一中心只 enumeratePlates 一次，
            // 避免 N 个玩家 N 次反射 newInstance + 排序（O(玩家×plateCount) → O(去重后中心数×plateCount)）。
            Map<Long, List<PlateRef>> platesByCenter = new HashMap<>();
            for (ServerPlayer player : level.players()) {
                int chunkX = player.chunkPosition().x;
                int chunkZ = player.chunkPosition().z;
                long center = chunkKey(chunkX, chunkZ);
                List<PlateRef> plates = platesByCenter.get(center);
                if (plates == null) {
                    plates = enumeratePlates(context,
                            chunkX * 16 + 8, chunkZ * 16 + 8, config.runtimeRadiusChunks());
                    platesByCenter.put(center, plates);
                    for (PlateRef plate : plates) activeCoverage.add(plate.key());
                }
                Long previous = state.playerCenters.get(player.getUUID());
                boolean retryDue = plates.stream().anyMatch(plate -> state.runtimeWork.retryDue(plate.key()));
                if (!retryDue && previous != null && chebyshevDistance(previous, center) < config.runtimeStrideChunks()) {
                    continue;
                }
                state.playerCenters.put(player.getUUID(), center);
                int budget = MAX_RUNTIME_PLATES_PER_SCAN;
                boolean saturated = false;
                for (PlateRef plate : plates) {
                    if (isReady(context, plate.key())) continue;
                    // 队列深度封顶:在途 plate 已经很多时不再入队,留待后续
                    // scan 继续补,避免一次性堆积几十个 plate 的脏 chunk。
                    if (RUNTIME_EXECUTOR.getQueue().size() + RUNTIME_EXECUTOR.getActiveCount()
                            >= MAX_RUNTIME_QUEUE_DEPTH) {
                        saturated = true;
                        break;
                    }
                    if (budget <= 0) {
                        saturated = true;
                        break;
                    }
                    int result = scheduleRuntime(state, context, plate.key());
                    if (result > 0) {
                        budget--;
                        scheduled++;
                    } else if (result < 0) {
                        saturated = true;
                    }
                }
                if (saturated) state.playerCenters.remove(player.getUUID(), center);
            }
            StreamsSamplerModeController.ensureRuntimePlateCacheCapacity(activeCoverage.size());
            state.runtimeWork.retainCoverage(activeCoverage);
            state.playerCenters.keySet().removeIf(uuid -> !isOnline(level, uuid));
            if (scheduled > 0) {
                LOGGER.info("Streams runtime prewarm queued {} plate(s); queue={}, active={}, radius={} chunks.",
                        scheduled, RUNTIME_EXECUTOR.getQueue().size(),
                        RUNTIME_EXECUTOR.getActiveCount(), config.runtimeRadiusChunks());
            }
        } catch (Throwable failure) {
            LOGGER.warn("Streams runtime prewarm scan failed", unwrap(failure));
        }
    }

    public static void stop(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) return;
        WorldState state = STATES.remove(level);
        if (state != null) {
            state.closed = true;
            state.startup.stop();
            state.startupWork.close();
            state.runtimeWork.close();
        }
        StreamsGpuTerrainSampler.close(level);
    }

    /**
     * 保存窗口避让:服务端开始写盘时进入,写盘结束时退出。plate build 在
     * 保存期间退让,避免一边写盘一边继续改 chunk 放大脏数据量。
     */
    public static void enterSaving() {
        SAVING_DEPTH.incrementAndGet();
    }

    public static void exitSaving() {
        SAVING_DEPTH.updateAndGet(value -> Math.max(0, value - 1));
    }

    private static boolean isSaving() {
        return SAVING_DEPTH.get() > 0;
    }

    private static int scheduleRuntime(WorldState state, EngineContext context, Object plate) {
        if (state.closed) return 0;
        return state.runtimeWork.submit(plate, RUNTIME_EXECUTOR, new RuntimePrewarmQueue.Access<>() {
            @Override public boolean permit() {
                if (!StreamsRuntimeLoadPolicy.awaitPermit(() -> !state.closed)) return false;
                // 保存避让仍保留，但关闭会话也必须能终止这一层等待。
                while (!state.closed && isSaving() && !Thread.currentThread().isInterrupted()) {
                    LockSupport.parkNanos(StreamsRuntimeLoadPolicy.pauseNanos());
                    if (!StreamsRuntimeLoadPolicy.awaitPermit(() -> !state.closed)) return false;
                }
                return !state.closed && !Thread.currentThread().isInterrupted();
            }
            @Override public boolean ready(Object key) { return isReady(context, key); }
            @Override public void build(Object key) { StreamsPrewarmService.build(context, key); }
            @Override public void retry(Object key, RuntimePrewarmQueue.Reason reason,
                                        int attempt, long delayNanos, Throwable failure) {
                state.playerCenters.clear();
                LOGGER.warn("【河流运行期预热延后重试】 plate={} reason={} attempt={} delayMs={} failure={}",
                        key, reason, attempt, TimeUnit.NANOSECONDS.toMillis(delayNanos),
                        failure == null ? "none" : unwrap(failure).toString());
            }
        });
    }

    private static void refreshRuntimeCapacity() {
        int recommended = StreamsRuntimeLoadPolicy.recommendedThreads();
        if (recommended <= 0) return;
        int target = Math.max(1, recommended);
        // 0.1.69：加迟滞，消除 5⇄3 每秒振荡。
        //
        // 真机取证（2026-09-18）：日志里 `Streams Reflowing runtime adaptive capacity
        // changed` 在 15:59 一分钟内出现 40 次，全程 84 次，模式恒为 `5 -> 3` 与
        // `3 -> 5` 交替。根因是这里【没有任何防抖】—— 只要
        // StreamsRuntimeLoadPolicy.recommendedThreads() 的返回值变一次，就立刻
        // setCorePoolSize/setMaximumPoolSize/prestartAllCoreThreads，并反射调用
        // LowResGrid.setBackgroundThrottle。而那个返回值来自 chunky-autogen 的
        // recommendedStreamsWorkerThreads()，其逻辑是"升得慢（1.5s 才 +1）、降得快
        // （critical/pressured 立即降）"，于是负载轻微抖动就让容量在两个值之间
        // 高频横跳，每次横跳都要重建线程池并重排任务 —— 白白的调度开销，
        // 还让下游看到的能力值忽大忽小。
        //
        // 迟滞规则（两个方向都要持续一段时间才生效，且下降比上升更快响应）：
        //   · 上升：新值连续保持 RUNTIME_CAPACITY_RISE_HOLD_NANOS 才采纳（2s）
        //   · 下降：新值连续保持 RUNTIME_CAPACITY_FALL_HOLD_NANOS 才采纳（1s）
        // 期间若目标值变回当前值，直接取消待定变更，容量保持不动。
        // 注意：Chunky 独占/巡航等"必须立刻让路"的情形在上游
        // recommendedThreads() 里返回 0，走上面的 return，不受本迟滞影响。
        if (target != lastRuntimeCapacityTarget) {
            lastRuntimeCapacityTarget = target;
            lastRuntimeCapacityTargetSinceNanos = System.nanoTime();
        }
        synchronized (RUNTIME_EXECUTOR) {
            int current = RUNTIME_EXECUTOR.getMaximumPoolSize();
            if (current == target && RUNTIME_EXECUTOR.getCorePoolSize() == target) return;
            long held = System.nanoTime() - lastRuntimeCapacityTargetSinceNanos;
            long required = target > current
                    ? RUNTIME_CAPACITY_RISE_HOLD_NANOS
                    : RUNTIME_CAPACITY_FALL_HOLD_NANOS;
            if (held < required) return;
            if (target > current) {
                RUNTIME_EXECUTOR.setMaximumPoolSize(target);
                RUNTIME_EXECUTOR.setCorePoolSize(target);
            } else {
                RUNTIME_EXECUTOR.setCorePoolSize(target);
                RUNTIME_EXECUTOR.setMaximumPoolSize(target);
            }
            RUNTIME_EXECUTOR.prestartAllCoreThreads();
            StreamsSamplerModeController.setRuntimeAdaptiveThreads(target);
            LOGGER.info("Streams runtime prewarm capacity changed: {} -> {} owner(s), queue={}, active={}, heldMs={}.",
                    current, target, RUNTIME_EXECUTOR.getQueue().size(), RUNTIME_EXECUTOR.getActiveCount(),
                    held / 1_000_000L);
        }
    }

    private static List<PlateRef> enumeratePlates(EngineContext context, int centerX, int centerZ,
                                                   int radiusChunks) throws ReflectiveOperationException {
        double radiusBlocks = radiusChunks * 16.0D;
        double scale = context.plateScale();
        int minPlateX = floor((centerX - radiusBlocks - CHUNK_DEFER_MARGIN_BLOCKS) / scale) - 1;
        int maxPlateX = floor((centerX + radiusBlocks + CHUNK_DEFER_MARGIN_BLOCKS) / scale) + 1;
        int minPlateZ = floor((centerZ - radiusBlocks - CHUNK_DEFER_MARGIN_BLOCKS) / scale) - 1;
        int maxPlateZ = floor((centerZ + radiusBlocks + CHUNK_DEFER_MARGIN_BLOCKS) / scale) + 1;
        int centerPlateX = floor(centerX / scale);
        int centerPlateZ = floor(centerZ / scale);
        long width = (long) maxPlateX - minPlateX + 1L;
        long depth = (long) maxPlateZ - minPlateZ + 1L;
        long plateCount = width * depth;
        if (width <= 0L || depth <= 0L || plateCount > StreamsPrewarmConfig.MAX_PREWARM_PLATES) {
            throw new IllegalArgumentException("Streams prewarm radius would enumerate "
                    + plateCount + " plates; maximum is " + StreamsPrewarmConfig.MAX_PREWARM_PLATES
                    + ". Reduce the configured radius.");
        }
        List<PlateRef> plates = new ArrayList<>((int) plateCount);
        for (int plateZ = minPlateZ; plateZ <= maxPlateZ; plateZ++) {
            for (int plateX = minPlateX; plateX <= maxPlateX; plateX++) {
                Object key = context.access().plateConstructor().newInstance(plateX, plateZ);
                plates.add(new PlateRef(key, plateX, plateZ));
            }
        }
        plates.sort(Comparator
                .comparingInt((PlateRef p) -> Math.max(
                        Math.abs(p.x() - centerPlateX), Math.abs(p.z() - centerPlateZ)))
                .thenComparingInt(p -> square(p.x() - centerPlateX) + square(p.z() - centerPlateZ)));
        return plates;
    }

    private static EngineContext context(ServerLevel level) throws ReflectiveOperationException {
        Access current = access();
        if (current == null) return null;
        Object engine = current.engine().invoke(null, level);
        if (engine == null) return null;
        Object config = current.config().invoke(engine);
        double plateScale = ((Number) current.plateScale().invoke(config)).doubleValue();
        if (!(plateScale > 0.0D)) throw new IllegalStateException("Invalid Streams plate scale " + plateScale);
        return new EngineContext(current, engine, plateScale);
    }

    /**
     * 运行期扫描用的就绪判定：只看内存缓存。
     *
     * <p>调用点在服务器线程上（tick / scheduleRuntime），这里绝不读盘；磁盘命中留给
     * 后台 owner 的 {@code networkFor}（它在本线程外 load/build），行为与改动前一致。</p>
     */
    private static boolean isReady(EngineContext context, Object plate) {
        try {
            return (boolean) context.access().isPlateReady().invoke(context.engine(), plate);
        } catch (ReflectiveOperationException failure) {
            throw new CompletionException(failure);
        }
    }

    /**
     * 启动预热第一段专用的缓存状态探针，经反射取 {@code RiverEngine.plateCacheState(PlateKey)}：
     * {@code 0} = 缺（要建）、{@code 1} = 内存已就绪、{@code 2} = 本次从磁盘缓存读回。
     *
     * <p>旧版 {@code RiverEngine} 没有这个方法时退回只看内存的 {@code isPlateReady}
     * （返回 1/0），行为与改动前一致。</p>
     */
    private static int plateState(EngineContext context, Object plate) {
        try {
            Access current = context.access();
            Method probe = current.plateCacheState();
            if (probe != null) {
                return ((Number) probe.invoke(context.engine(), plate)).intValue();
            }
            return ((boolean) current.isPlateReady().invoke(context.engine(), plate)) ? 1 : 0;
        } catch (ReflectiveOperationException failure) {
            throw new CompletionException(failure);
        }
    }

    /**
     * 预热第一段：并行探测每块板的磁盘缓存。
     *
     * <p>命中时 {@code plateCacheState} 会把上一局落盘的产物读回内存，因此这些板既计入
     * alreadyReady，也不会进入第二段的构建队列。单块探测失败（反射/IO）只当未命中，
     * 绝不让它打断整轮启动预热。</p>
     */
    private static int[] primePlateStates(EngineContext context, List<PlateRef> plates,
                                          ExecutorService executor, RiverStartupSession session) {
        int[] states = new int[plates.size()];
        List<CompletableFuture<Void>> priming = new ArrayList<>(plates.size());
        for (int index = 0; index < plates.size(); index++) {
            int slot = index;
            Object key = plates.get(index).key();
            priming.add(session.submit(executor, () -> {
                try {
                    states[slot] = plateState(context, key);
                } catch (Throwable failure) {
                    states[slot] = 0;
                } finally {
                    // 未命中也完成了探测，不能让检查计数永远卡在最后几块。
                    session.cacheChecked();
                }
            }));
        }
        CompletableFuture.allOf(priming.toArray(new CompletableFuture[0])).join();
        return states;
    }

    private static void build(EngineContext context, Object plate) {
        try {
            context.access().networkFor().invoke(context.engine(), plate);
        } catch (InvocationTargetException failure) {
            throw new CompletionException(unwrap(failure));
        } catch (ReflectiveOperationException failure) {
            throw new CompletionException(failure);
        }
    }

    private static Access access() {
        if (accessInitialized) return access;
        synchronized (StreamsPrewarmService.class) {
            if (accessInitialized) return access;
            accessInitialized = true;
            try {
                Class<?> worldgen = Class.forName("dev.streamsreflowing.worldgen.StreamsWorldgen");
                Class<?> engineClass = Class.forName("dev.streamsreflowing.core.RiverEngine");
                Class<?> configClass = Class.forName("dev.streamsreflowing.core.config.StreamsConfig");
                Class<?> plateClass = Class.forName("dev.streamsreflowing.core.plate.PlateKey");
                access = new Access(
                        findEngineMethod(worldgen),
                        engineClass.getMethod("config"),
                        configClass.getMethod("plateScale"),
                        plateClass.getConstructor(int.class, int.class),
                        engineClass.getMethod("networkFor", plateClass),
                        engineClass.getMethod("isPlateReady", plateClass),
                        findPlateCacheState(engineClass, plateClass));
            } catch (Throwable failure) {
                access = null;
                LOGGER.warn("Streams prewarm API initialization failed", failure);
            }
            return access;
        }
    }

    private static Method findEngineMethod(Class<?> worldgen) throws NoSuchMethodException {
        for (Method method : worldgen.getMethods()) {
            if (method.getName().equals("engine") && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 1) return method;
        }
        throw new NoSuchMethodException("StreamsWorldgen.engine");
    }

    /** {@code RiverEngine.plateCacheState(PlateKey)} 可选：老版本没有它时返回 null，退回内存探针。 */
    private static Method findPlateCacheState(Class<?> engineClass, Class<?> plateClass) {
        try {
            return engineClass.getMethod("plateCacheState", plateClass);
        } catch (NoSuchMethodException | RuntimeException absent) {
            return null;
        }
    }

    private static synchronized ExecutorService startupExecutor(int workers) {
        if (startupExecutor != null && startupExecutorWorkers == workers) return startupExecutor;
        if (startupExecutor != null) startupExecutor.shutdown();
        startupExecutorWorkers = workers;
        startupExecutor = Executors.newFixedThreadPool(workers,
                new NamedThreadFactory("Streams-Startup-Prewarm-", Thread.NORM_PRIORITY));
        return startupExecutor;
    }

    private static boolean isOverworld(ServerLevel level) {
        return level != null && Level.OVERWORLD.equals(level.dimension());
    }

    private static boolean isOnline(ServerLevel level, UUID uuid) {
        for (ServerPlayer player : level.players()) {
            if (player.getUUID().equals(uuid)) return true;
        }
        return false;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int square(int value) {
        return value * value;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int chebyshevDistance(long a, long b) {
        int ax = (int) (a >> 32);
        int az = (int) a;
        int bx = (int) (b >> 32);
        int bz = (int) b;
        return Math.max(Math.abs(ax - bx), Math.abs(az - bz));
    }

    private static long elapsedMillis(long started) {
        return Math.round((System.nanoTime() - started) / 1_000_000.0D);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof InvocationTargetException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    private record Access(Method engine, Method config, Method plateScale,
                          Constructor<?> plateConstructor, Method networkFor,
                          Method isPlateReady, Method plateCacheState) {
    }

    private record EngineContext(Access access, Object engine, double plateScale) {
    }

    private record PlateRef(Object key, int x, int z) {
    }

    private static final class WorldState {
        private final Map<UUID, Long> playerCenters = new ConcurrentHashMap<>();
        private final RuntimePrewarmQueue<Object> runtimeWork =
                new RuntimePrewarmQueue<>(RUNTIME_QUEUE_CAPACITY, System::nanoTime);
        private final RiverStartupReadiness startup = new RiverStartupReadiness();
        private final RiverStartupSession startupWork = new RiverStartupSession(
                StreamsSamplerModeController::enterInitialGeneration,
                StreamsSamplerModeController::exitInitialGeneration);
        private volatile int macroRadiusBlocks;
        private volatile boolean closed;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final int priority;

        private NamedThreadFactory(String prefix, int priority) {
            this.prefix = prefix;
            this.priority = priority;
        }

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, prefix + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(priority);
            return thread;
        }
    }
}
