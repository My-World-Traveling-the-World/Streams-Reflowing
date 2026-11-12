package dev.streamsreflowing.compat.streamscontrol;

import net.shiroha233.roadweaver.api.incoming.PlanningLifecycleApi;
import net.shiroha233.roadweaver.api.incoming.PlanningLifecycleListener;
import net.shiroha233.roadweaver.api.outgoing.RoadLoadSignalsRegistry;
import net.shiroha233.roadweaver.api.outgoing.StreamsThreadControl;

/**
 * 通过 RoadWeaver 的<b>标准 API</b>做两件事：
 *
 * <ol>
 *   <li><b>接收</b>初始生成开始/结束通知 → 进入/退出采样加强档
 *       （替代 {@code RoadWeaverInitialGenerationStreamsBoostMixin} 的 HEAD + RETURN 注入）；</li>
 *   <li><b>提供</b> {@link StreamsThreadControl} 通道，让道路按契约设置供应线程数
 *       （替代道路反射写 {@code StreamsSamplerModeController} 的静态方法）。</li>
 * </ol>
 *
 * <h2>为什么两个方向都要走本桥</h2>
 * <p>改造前是两个方向各一套硬编码：</p>
 * <pre>
 * streams → RoadWeaver：@Pseudo @Mixin(targets = "...InitialGenManager")
 *                       &#64;Inject(method = "blockUntilDone", at = HEAD / RETURN, require = 0)
 * RoadWeaver → streams：反射 "local.plaincraft.streamscontrol.StreamsSamplerModeController",
 *                       "setChunkySupplyThreads" / "setChunkyExclusive"
 * </pre>
 * <p>{@code require = 0} 意味着目标方法一改名，注入就静默跳过；反射那一侧同理。
 * 两边都是「不报错、只是不生效」。</p>
 */
public final class PlainCraftRoadWeaverBridge {

    private PlainCraftRoadWeaverBridge() {
    }

    /** 在 mod 初始化时调用一次。 */
    public static void register() {
        RoadLoadSignalsRegistry.registerStreamsThreadControl(new StreamsThreadControl() {
            @Override
            public void setChunkySupplyThreads(int threads) {
                StreamsSamplerModeController.setChunkySupplyThreads(threads);
            }

            @Override
            public void setChunkyExclusive(boolean exclusive) {
                StreamsSamplerModeController.setChunkyExclusive(exclusive);
            }
        });

        PlanningLifecycleApi.addListener(new PlanningLifecycleListener() {
            @Override
            public void onInitialGenerationStart() {
                try {
                    StreamsSamplerModeController.enterInitialGeneration();
                } catch (Throwable ignored) {
                    // 从道路的启动路径回调进来：本条链失败不能让道路生成失败。
                }
            }

            @Override
            public void onInitialGenerationEnd(InitialGenerationSummary summary) {
                try {
                    StreamsSamplerModeController.exitInitialGeneration();
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
