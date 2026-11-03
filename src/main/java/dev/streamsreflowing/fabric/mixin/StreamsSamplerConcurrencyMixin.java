package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.compat.streamscontrol.StreamsSamplerModeController;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.streamsreflowing.core.grid.LowResGrid", remap = false)
public abstract class StreamsSamplerConcurrencyMixin {
    private static final Logger PLAINCRAFT_LOGGER = LoggerFactory.getLogger("PlainCraft Streams Safety");

    @Shadow(remap = false)
    @Final
    private static ThreadPoolExecutor e;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void plaincraft$capInitialSamplerPool(CallbackInfo ci) {
        plaincraft$applyDesiredSamplerLimit();
        PLAINCRAFT_LOGGER.info("Streams Reflowing sampler workers initialized at {}.",
                StreamsSamplerModeController.threadPoolThreadLimit());
    }

    @Inject(method = "a(I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void plaincraft$capSamplerResize(int requestedThreads, CallbackInfo ci) {
        plaincraft$applyDesiredSamplerLimit();
        ci.cancel();
    }

    private static void plaincraft$applyDesiredSamplerLimit() {
        int desiredThreads = StreamsSamplerModeController.threadPoolThreadLimit();
        if (e.getCorePoolSize() == desiredThreads && e.getMaximumPoolSize() == desiredThreads) {
            plaincraft$deprioritizeSamplerWorkers();
            return;
        }
        if (desiredThreads >= e.getMaximumPoolSize()) {
            e.setMaximumPoolSize(desiredThreads);
            e.setCorePoolSize(desiredThreads);
        } else {
            e.setCorePoolSize(desiredThreads);
            e.setMaximumPoolSize(desiredThreads);
        }
        e.prestartAllCoreThreads();
        plaincraft$deprioritizeSamplerWorkers();
    }

    /**
     * 0.1.73：给外部 Streams 采样池的 worker 线程降优先级。
     *
     * <p>LowResGrid 的采样池 {@code e} 由外部 Streams 的线程工厂创建，优先级是
     * NORM_PRIORITY —— 地形采样是真正的 CPU 重活（每 plate 上千采样点），与主线程
     * 同优先级抢核是「游玩时掉帧/高占用」的根因之一（0.1.68 真机分族峰值 10.73 核）。
     * 线程工厂闭包改不动，这里反射遍历池内 worker 线程统一降到 MIN_PRIORITY。</p>
     *
     * <p>纯降优先级、不改任何采样逻辑；反射失败（外部类字段结构变化）时静默降级，
     * 不影响采样正确性。</p>
     */
    private static void plaincraft$deprioritizeSamplerWorkers() {
        try {
            Field workersField = ThreadPoolExecutor.class.getDeclaredField("workers");
            workersField.setAccessible(true);
            Object workers = workersField.get(e);
            if (!(workers instanceof Set<?> workerSet)) return;
            for (Object worker : workerSet) {
                try {
                    Field threadField = worker.getClass().getDeclaredField("thread");
                    threadField.setAccessible(true);
                    Object threadObj = threadField.get(worker);
                    if (threadObj instanceof Thread thread) {
                        thread.setPriority(Thread.MIN_PRIORITY);
                    }
                } catch (Throwable ignored) {
                    // 单个 worker 降级失败不阻断其余。
                }
            }
        } catch (Throwable failure) {
            // JDK 9+ 模块封印：Field.setAccessible 抛的是 InaccessibleObjectException
            // （RuntimeException 子类，不是 ReflectiveOperationException）。必须按 Throwable
            // 兜住，否则异常穿透到 LowResGrid.<clinit> 注入点会把整个类初始化炸掉，
            // 后续所有引用它的地方都抛 NoClassDefFoundError（真机建档卡死的根因）。
            PLAINCRAFT_LOGGER.debug("Could not deprioritize Streams sampler workers", failure);
        }
    }
}
