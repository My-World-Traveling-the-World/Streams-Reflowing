package dev.streamsreflowing.compat.streamscontrol;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

final class StreamsRuntimeLoadPolicy {
    private static final long PAUSE_NANOS = TimeUnit.MILLISECONDS.toNanos(25L);
    private static final OptionalBooleanMethod CRUISE_ACTIVE = new OptionalBooleanMethod(
            "local.plaincraft.hyperspeed.CruiseState", "isCpuProtectionActive");
    private static final OptionalBooleanMethod INTERACTION_ACTIVE = new OptionalBooleanMethod(
            "local.plaincraftchunkyautogen.PlayerChunkPriorityGuard", "isInteractionActive");
    private static final OptionalBooleanMethod FOREGROUND_ACTIVE = new OptionalBooleanMethod(
            "local.plaincraftchunkyautogen.PlayerChunkPriorityGuard", "isForegroundActivityActive");
    private static final OptionalBooleanMethod CHUNKY_ACTIVE = new OptionalBooleanMethod(
            "local.plaincraftchunkyautogen.AdaptiveChunkController", "hasActiveTasks");
    private static final OptionalIntMethod ADAPTIVE_THREADS = new OptionalIntMethod(
            "local.plaincraftchunkyautogen.AdaptiveChunkController", "recommendedStreamsWorkerThreads");

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
        if (CRUISE_ACTIVE.invoke()) return 0;
        if (CHUNKY_ACTIVE.invoke()) return 0;
        int maximum = hardMaximumThreads();
        Integer adaptive = ADAPTIVE_THREADS.invoke(maximum);
        if (adaptive != null) return Math.max(1, Math.min(maximum, adaptive));
        return fallbackThreads(
                maximum,
                CHUNKY_ACTIVE.invoke(),
                FOREGROUND_ACTIVE.invoke(),
                INTERACTION_ACTIVE.invoke());
    }

    static boolean isTravelPaused() {
        return CRUISE_ACTIVE.invoke();
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
        if (StreamsSamplerModeController.isChunkyExclusive() || CHUNKY_ACTIVE.invoke()) return false;
        while (recommendedThreads() <= 0 && !Thread.currentThread().isInterrupted()) {
            if (StreamsSamplerModeController.isChunkyExclusive() || CHUNKY_ACTIVE.invoke()) return false;
            LockSupport.parkNanos(PAUSE_NANOS);
        }
        return !Thread.currentThread().isInterrupted();
    }

    private static final class OptionalBooleanMethod {
        private final String className;
        private final String methodName;
        private volatile boolean resolved;
        private volatile Method method;

        private OptionalBooleanMethod(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        private boolean invoke() {
            Method target = resolve();
            if (target == null) return false;
            try {
                return Boolean.TRUE.equals(target.invoke(null));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return false;
            }
        }

        private Method resolve() {
            if (resolved) return method;
            synchronized (this) {
                if (resolved) return method;
                try {
                    method = Class.forName(className, false, StreamsRuntimeLoadPolicy.class.getClassLoader())
                            .getMethod(methodName);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    method = null;
                }
                resolved = true;
                return method;
            }
        }
    }

    private static final class OptionalIntMethod {
        private final String className;
        private final String methodName;
        private volatile boolean resolved;
        private volatile Method method;

        private OptionalIntMethod(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        private Integer invoke(int value) {
            Method target = resolve();
            if (target == null) return null;
            try {
                Object result = target.invoke(null, value);
                return result instanceof Number number ? number.intValue() : null;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        private Method resolve() {
            if (resolved) return method;
            synchronized (this) {
                if (resolved) return method;
                try {
                    method = Class.forName(className, false, StreamsRuntimeLoadPolicy.class.getClassLoader())
                            .getMethod(methodName, int.class);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    method = null;
                }
                resolved = true;
                return method;
            }
        }
    }
}
