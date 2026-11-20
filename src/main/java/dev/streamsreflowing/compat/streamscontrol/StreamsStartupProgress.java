package dev.streamsreflowing.compat.streamscontrol;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class StreamsStartupProgress {
    /** 进度属于发起它的会话；旧任务晚到只能处理旧数据。 */
    private static Object owner;
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();
    private static final AtomicInteger TOTAL_PLATES = new AtomicInteger();
    private static final AtomicInteger COMPLETED_PLATES = new AtomicInteger();
    private static final AtomicInteger ALREADY_READY_PLATES = new AtomicInteger();
    private static final AtomicInteger BUILD_WORKERS = new AtomicInteger();
    private static final AtomicLong STARTED_AT_NANOS = new AtomicLong();

    private StreamsStartupProgress() {
    }

    public static synchronized void begin(int totalPlates, int alreadyReadyPlates, int buildWorkers) {
        begin(new Object(), totalPlates, alreadyReadyPlates, buildWorkers);
    }

    static synchronized void begin(Object session, int totalPlates, int alreadyReadyPlates, int buildWorkers) {
        owner = session;
        TOTAL_PLATES.set(Math.max(0, totalPlates));
        COMPLETED_PLATES.set(Math.max(0, Math.min(totalPlates, alreadyReadyPlates)));
        ALREADY_READY_PLATES.set(Math.max(0, Math.min(totalPlates, alreadyReadyPlates)));
        BUILD_WORKERS.set(Math.max(1, buildWorkers));
        STARTED_AT_NANOS.set(System.nanoTime());
        ACTIVE.set(totalPlates > 0);
    }

    public static synchronized void setCompleted(int completedPlates) {
        COMPLETED_PLATES.set(Math.max(0, Math.min(TOTAL_PLATES.get(), completedPlates)));
    }

    static synchronized void setCompleted(Object session, int completedPlates) {
        if (owner == session) setCompleted(completedPlates);
    }

    public static synchronized void finish() {
        COMPLETED_PLATES.set(TOTAL_PLATES.get());
        ACTIVE.set(false);
    }

    static synchronized void finish(Object session) {
        if (owner == session) finish();
    }

    static synchronized void clear(Object session) {
        if (owner == session) clear();
    }

    public static synchronized void clear() {
        owner = null;
        ACTIVE.set(false);
        TOTAL_PLATES.set(0);
        COMPLETED_PLATES.set(0);
        ALREADY_READY_PLATES.set(0);
        BUILD_WORKERS.set(0);
        STARTED_AT_NANOS.set(0L);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static synchronized Snapshot snapshot() {
        int total = TOTAL_PLATES.get();
        int completed = Math.min(total, COMPLETED_PLATES.get());
        long startedAt = STARTED_AT_NANOS.get();
        long elapsedMillis = startedAt == 0L
                ? 0L
                : TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAt));
        return new Snapshot(ACTIVE.get(), total, completed, ALREADY_READY_PLATES.get(),
                BUILD_WORKERS.get(), elapsedMillis);
    }

    public record Snapshot(boolean active, int totalPlates, int completedPlates,
                           int alreadyReadyPlates, int buildWorkers, long elapsedMillis) {
        public int percent() {
            if (totalPlates <= 0) return 0;
            return Math.max(0, Math.min(100, Math.round(completedPlates * 100.0F / totalPlates)));
        }
    }
}
