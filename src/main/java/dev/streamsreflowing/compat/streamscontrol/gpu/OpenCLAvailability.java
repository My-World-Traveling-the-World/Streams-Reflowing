/* 鏂囦欢鑱岃矗锛氱淮鎶ゅ綋鍓嶆湇鍔″櫒浼氳瘽鍐?OpenCL 绮楅噰鏍风殑鍙敤鎬т笌鍥為€€鍘熷洜銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenCL 鍙敤鎬х姸鎬併€? */
public final class OpenCLAvailability {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final AtomicReference<String> DISABLED_REASON = new AtomicReference<>();
    private static final AtomicBoolean LOGGED_DISABLED = new AtomicBoolean();

    private OpenCLAvailability() {}

    public static boolean isAvailable() {
        return DISABLED_REASON.get() == null;
    }

    public static String disabledReason() {
        return DISABLED_REASON.get();
    }

    public static void disable(String reason) {
        String safeReason = (reason == null || reason.isBlank()) ? "unknown" : reason;
        DISABLED_REASON.compareAndSet(null, safeReason);
    }

    public static void disable(String reason, Throwable cause) {
        disable(reason);
        logDisabledOnce(cause);
    }

    public static void logDisabledOnce(Throwable cause) {
        String reason = DISABLED_REASON.get();
        if (reason == null || !LOGGED_DISABLED.compareAndSet(false, true)) {
            return;
        }
        if (cause == null) {
            LOGGER.info("Streams OpenCL unavailable; using CPU: {}", reason);
        } else {
            LOGGER.info("Streams OpenCL unavailable; using CPU: {}", reason, cause);
        }
    }

    public static void reset() {
        DISABLED_REASON.set(null);
        LOGGED_DISABLED.set(false);
    }

    public static void resetForTests() {
        reset();
    }
}

