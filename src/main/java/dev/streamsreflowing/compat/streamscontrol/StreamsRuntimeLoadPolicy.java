package dev.streamsreflowing.compat.streamscontrol;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import net.shiroha233.roadweaver.api.outgoing.LoadKind;
import net.shiroha233.roadweaver.api.outgoing.RoadLoadSignals;
import net.shiroha233.roadweaver.api.outgoing.RoadLoadSignalsRegistry;

/**
 * 运行时线程数的决策。
 *
 * <h2>改造前后</h2>
 * <p>改造前这里有 <b>5 个硬编码反射探针</b>，全部打在别的 mod 的<b>内部实现类</b>上：</p>
 * <pre>
 * local.plaincraft.hyperspeed.CruiseState            isCpuProtectionActive
 * local.plaincraftchunkyautogen.PlayerChunkPriorityGuard  isInteractionActive
 * local.plaincraftchunkyautogen.PlayerChunkPriorityGuard  isForegroundActivityActive
 * local.plaincraftchunkyautogen.AdaptiveChunkController   hasActiveTasks
 * local.plaincraftchunkyautogen.AdaptiveChunkController   recommendedStreamsWorkerThreads
 * </pre>
 * <p>被探测方完全不知道自己被依赖 —— 改个方法名，本 mod 就静默退回默认线程数，
 * 表现为「超速飞行时河流采样还是很吃 CPU」这种无从排查的现象。</p>
 *
 * <p>现在全部改成查 {@link RoadLoadSignalsRegistry}（跨 mod 标准契约）。语义逐项对齐：</p>
 * <table border="1">
 *   <caption>探针 → 契约 的对应关系</caption>
 *   <tr><th>原探针</th><th>现在的查询</th></tr>
 *   <tr><td>{@code CruiseState.isCpuProtectionActive}</td>
 *       <td>{@link RoadLoadSignals#cpuProtection()}</td></tr>
 *   <tr><td>{@code PlayerChunkPriorityGuard.isInteractionActive}</td>
 *       <td>{@link RoadLoadSignals#foregroundInteraction()}</td></tr>
 *   <tr><td>{@code PlayerChunkPriorityGuard.isForegroundActivityActive}</td>
 *       <td>{@link RoadLoadSignals#foregroundActivity()}</td></tr>
 *   <tr><td>{@code AdaptiveChunkController.hasActiveTasks}</td>
 *       <td>{@link RoadLoadSignalsRegistry#isBusy(LoadKind)} 的 {@link LoadKind#PREGENERATION}</td></tr>
 *   <tr><td>{@code AdaptiveChunkController.recommendedStreamsWorkerThreads(int)}</td>
 *       <td>{@link RoadLoadSignals#recommendedThreadsFor(String, int)} 的
 *           {@link RoadLoadSignals#CONSUMER_STREAMS}</td></tr>
 * </table>
 *
 * <p>注册表为空时全部退化为 {@code false} / 无建议，与改造前「探针解析失败」的行为一致。</p>
 */
final class StreamsRuntimeLoadPolicy {

    private static final long PAUSE_NANOS = TimeUnit.MILLISECONDS.toNanos(25L);

    private StreamsRuntimeLoadPolicy() {
    }

    static int hardMaximumThreads() {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        return Math.max(1, Math.min(8, processors / 3));
    }

    static long pauseNanos() {
        return PAUSE_NANOS;
    }

    static int recommendedThreads() {
        if (StreamsSamplerModeController.isChunkyExclusive()) return 0;
        if (cruiseActive()) return 0;
        if (chunkyActive()) return 0;
        int maximum = hardMaximumThreads();
        Integer adaptive = adaptiveThreads(maximum);
        if (adaptive != null) return Math.max(1, Math.min(maximum, adaptive));
        return fallbackThreads(
                maximum,
                chunkyActive(),
                foregroundActivity(),
                interactionActive());
    }

    static boolean isTravelPaused() {
        return cruiseActive();
    }

    static int fallbackThreads(
            int hardMaximum,
            boolean chunkyActive,
            boolean foregroundActive,
            boolean interactionActive) {
        int maximum = Math.max(1, hardMaximum);
        if (interactionActive) return 1;
        if (foregroundActive) return Math.min(maximum, Math.max(2, (maximum + 1) / 2));
        if (chunkyActive) return 0;
        return maximum;
    }

    static boolean awaitPermit() {
        if (StreamsSamplerModeController.isChunkyExclusive() || chunkyActive()) return false;
        while (recommendedThreads() <= 0 && !Thread.currentThread().isInterrupted()) {
            if (StreamsSamplerModeController.isChunkyExclusive() || chunkyActive()) return false;
            LockSupport.parkNanos(PAUSE_NANOS);
        }
        return !Thread.currentThread().isInterrupted();
    }

    // ── 注册表查询（全部容错：单个 provider 抛异常只跳过它）──────────────

    /** 是否有任何 provider 处于 CPU 保护模式（超速飞行巡航）。 */
    static boolean cruiseActive() {
        for (RoadLoadSignals signals : RoadLoadSignalsRegistry.sources()) {
            try {
                if (signals.cpuProtection()) return true;
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    /** 是否有 provider 报告玩家正在前台交互。 */
    static boolean interactionActive() {
        for (RoadLoadSignals signals : RoadLoadSignalsRegistry.sources()) {
            try {
                if (signals.foregroundInteraction()) return true;
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    /** 是否有 provider 报告玩家正在前台活动（移动或交互）。 */
    static boolean foregroundActivity() {
        for (RoadLoadSignals signals : RoadLoadSignalsRegistry.sources()) {
            try {
                if (signals.foregroundActivity()) return true;
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    /** 是否正在进行区块预生成（Chunky 预热 / 正式运行）。 */
    static boolean chunkyActive() {
        return RoadLoadSignalsRegistry.isBusy(LoadKind.PREGENERATION);
    }

    /**
     * 各 provider 给 streams 的推荐线程数的<b>最小值</b>。
     *
     * @return {@code null} 表示无人推荐（调用方用 {@link #fallbackThreads}）
     */
    static Integer adaptiveThreads(int currentThreads) {
        int best = -1;
        for (RoadLoadSignals signals : RoadLoadSignalsRegistry.sources()) {
            try {
                int v = signals.recommendedThreadsFor(RoadLoadSignals.CONSUMER_STREAMS, currentThreads);
                if (v >= 0) {
                    best = (best < 0) ? v : Math.min(best, v);
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return best < 0 ? null : best;
    }
}
