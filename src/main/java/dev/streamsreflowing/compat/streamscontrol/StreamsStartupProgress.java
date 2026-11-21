package dev.streamsreflowing.compat.streamscontrol;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class StreamsStartupProgress {
    /** 进度属于发起它的会话；旧任务晚到只能处理旧数据。 */
    private static Object owner;
    // 展示阶段不参与存档，也不改变预热就绪契约。
    public enum Stage { CACHE_CHECK, BUILDING, FINISHED, FAILED }
    private static Stage stage = Stage.FINISHED;
    private static int checked;
    private static int failedPlates;
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
        boolean continuingCacheCheck = owner == session && stage == Stage.CACHE_CHECK;
        owner = session;
        stage = Stage.BUILDING;
        failedPlates = 0;
        TOTAL_PLATES.set(Math.max(0, totalPlates));
        COMPLETED_PLATES.set(Math.max(0, Math.min(totalPlates, alreadyReadyPlates)));
        ALREADY_READY_PLATES.set(Math.max(0, Math.min(totalPlates, alreadyReadyPlates)));
        BUILD_WORKERS.set(Math.max(1, buildWorkers));
        // 缓存探测也是玩家真实等待的一部分，构建阶段不能把计时重新归零。
        if (!continuingCacheCheck) {
            checked = 0;
            STARTED_AT_NANOS.set(System.nanoTime());
        }
        ACTIVE.set(totalPlates > 0);
    }

    static synchronized void beginCache(Object session, int total, int workers) {
        begin(session, total, 0, workers);
        checked = 0;
        stage = Stage.CACHE_CHECK;
    }

    static synchronized void cacheChecked(Object session) {
        if (owner == session && stage == Stage.CACHE_CHECK) checked = Math.min(TOTAL_PLATES.get(), checked + 1);
    }

    static synchronized void buildFailed(Object session) {
        if (owner == session && stage == Stage.BUILDING) failedPlates++;
    }

    static synchronized void fail(Object session) {
        if (owner != session) return;
        stage = Stage.FAILED;
        ACTIVE.set(false);
    }

    public static synchronized void setCompleted(int completedPlates) {
        COMPLETED_PLATES.set(Math.max(0, Math.min(TOTAL_PLATES.get(), completedPlates)));
    }

    static synchronized void setCompleted(Object session, int completedPlates) {
        if (owner == session) setCompleted(completedPlates);
    }

    public static synchronized void finish() {
        stage = Stage.FINISHED;
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
        stage = Stage.FINISHED;
        checked = 0;
        failedPlates = 0;
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
                BUILD_WORKERS.get(), elapsedMillis, stage, checked, failedPlates);
    }

    public record Snapshot(boolean active, int totalPlates, int completedPlates,
                           int alreadyReadyPlates, int buildWorkers, long elapsedMillis,
                           Stage stage, int checkedPlates, int failedPlates) {
        public int percent() {
            if (totalPlates <= 0) return 0;
            // 缓存阶段占10%，避免缓存检查结束切构建时总百分比从100跳回零。
            if (stage == Stage.CACHE_CHECK) return Math.min(10, Math.round(checkedPlates * 10.0F / totalPlates));
            if (stage == Stage.FINISHED) return 100;
            return Math.max(0, Math.min(99, 10 + Math.round(completedPlates * 89.0F / totalPlates)));
        }
    }
}
