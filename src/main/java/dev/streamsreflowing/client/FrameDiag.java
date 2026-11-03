/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.client.HarnessPilot;
import dev.streamsreflowing.flow.FlowTrace;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FrameDiag {
    private static final boolean ON = Boolean.getBoolean("streamsreflowing.framediag");
    private static final int STALL_MS = Math.max(16, Integer.getInteger("streamsreflowing.framestall", 100));
    private static final long WATCH_INTERVAL_MS = 10L;
    private static final long CAPTURE_GAP_MS = 50L;
    private static final int MAX_CAPTURES_PER_STALL = 14;
    private static final long REPORT_EVERY_MS = 30000L;
    private static final long WARMUP_MS = 30000L;
    private static volatile long heartbeatMs;
    private static volatile Thread clientThread;
    private static volatile boolean perFrame;
    private static volatile boolean started;
    private static volatile long firstTickMs;
    private static long ticks;
    private static final ThreadMXBean MX;
    private static final boolean CPU_TIME_OK;
    private static final long[] BAND_MAX;
    private static final String[] BAND_NAME;
    private static final Band[] bands;
    private static final List<Long> durations;
    private static final List<Stall> worst;
    private static long totalStallMs;
    private static long worstStallMs;
    private static int stallCount;
    private static int idleCount;
    private static long idleMs;

    private static boolean cpuTimeAvailable() {
        try {
            if (!MX.isThreadCpuTimeSupported()) {
                return false;
            }
            if (!MX.isThreadCpuTimeEnabled()) {
                MX.setThreadCpuTimeEnabled(true);
            }
            return MX.isThreadCpuTimeEnabled();
        }
        catch (Throwable t) {
            return false;
        }
    }

    private static Band[] newBands() {
        Band[] b = new Band[BAND_MAX.length];
        for (int i = 0; i < b.length; ++i) {
            b[i] = new Band();
        }
        return b;
    }

    private FrameDiag() {
    }

    public static void tick(int fps) {
        FrameDiag.beat();
    }

    public static void frame() {
        perFrame = true;
        FrameDiag.beat();
    }

    private static void beat() {
        if (!ON) {
            return;
        }
        long now = System.currentTimeMillis();
        clientThread = Thread.currentThread();
        heartbeatMs = now;
        ++ticks;
        if (!started) {
            firstTickMs = now;
            started = true;
            FrameDiag.startWatchdog();
        }
    }

    private static void startWatchdog() {
        Thread t = new Thread(FrameDiag::watch, "streamsreflowing-framediag");
        t.setDaemon(true);
        t.setPriority(Math.min(10, 6));
        t.start();
    }

    private static void watch() {
        long lastReportMs = System.currentTimeMillis();
        long lastSeenBeat = 0L;
        long baseCpuNanos = -1L;
        long baseAtMs = 0L;
        while (true) {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException e) {
                return;
            }
            try {
                long beat = heartbeatMs;
                long now = System.currentTimeMillis();
                if (beat != lastSeenBeat) {
                    lastSeenBeat = beat;
                    baseCpuNanos = FrameDiag.clientCpuNanos();
                    baseAtMs = now;
                }
                if (now - beat >= (long)STALL_MS && now - firstTickMs >= 30000L) {
                    if (HarnessPilot.active()) {
                        FlowTrace.harnessStalls.incrementAndGet();
                    }
                    FrameDiag.observeStall(beat, baseCpuNanos, baseAtMs);
                    now = System.currentTimeMillis();
                }
                if (now - lastReportMs < 30000L) continue;
                lastReportMs = now;
                FrameDiag.report();
            }
            catch (Throwable throwable) {
            }
        }
    }

    private static long clientCpuNanos() {
        Thread t = clientThread;
        if (!CPU_TIME_OK || t == null) {
            return -1L;
        }
        try {
            return MX.getThreadCpuTime(t.getId());
        }
        catch (Throwable ignored) {
            return -1L;
        }
    }

    private static void observeStall(long beatAtStart, long baseCpuNanos, long baseAtMs) {
        long endCpu;
        HashMap<String, Long> blame = new HashMap<String, Long>();
        String clientState = "unknown";
        int captures = 0;
        int sleeping = 0;
        int awake = 0;
        long lastCaptureMs = 0L;
        ArrayList<String> pending = new ArrayList<String>();
        HashMap<String, Long> clientWhere = new HashMap<String, Long>();
        ArrayList<String> clientPending = new ArrayList<String>();
        long watchdogStartMs = System.currentTimeMillis();
        long gap = 50L;
        long cpuWindowFromMs = System.currentTimeMillis();
        Map<Long, Long> cpuAtStart = FrameDiag.snapshotCpu();
        while (heartbeatMs == beatAtStart) {
            long now = System.currentTimeMillis();
            if (captures < 14 && now - lastCaptureMs >= gap) {
                long span = captures == 0 ? 0L : now - lastCaptureMs;
                FrameDiag.credit(blame, pending, span);
                FrameDiag.credit(clientWhere, clientPending, span);
                lastCaptureMs = now;
                ++captures;
                gap = Math.min(1000L, gap * 8L / 5L);
                String st = FrameDiag.sampleInto(pending);
                if (st != null) {
                    clientState = st;
                    clientPending.add(st);
                    if (st.contains("in Thread.sleep")) {
                        ++sleeping;
                    } else {
                        ++awake;
                    }
                }
            }
            if (now - watchdogStartMs > 60000L) {
                return;
            }
            try {
                Thread.sleep(captures >= 14 ? 50L : 10L);
            }
            catch (InterruptedException e) {
                return;
            }
        }
        long tail = Math.max(0L, heartbeatMs - lastCaptureMs);
        FrameDiag.credit(blame, pending, tail);
        FrameDiag.credit(clientWhere, clientPending, tail);
        long duration = Math.max(0L, heartbeatMs - beatAtStart);
        HashMap<String, Long> cpuMs = new HashMap<String, Long>();
        long[] totalCpu = new long[1];
        FrameDiag.diffCpu(cpuAtStart, cpuMs, totalCpu);
        long totalWindow = Math.max(1L, heartbeatMs - cpuWindowFromMs);
        long clientWindow = Math.max(1L, heartbeatMs - baseAtMs);
        long clientCpuMs = -1L;
        if (baseCpuNanos >= 0L && (endCpu = FrameDiag.clientCpuNanos()) >= 0L) {
            clientCpuMs = Math.max(0L, (endCpu - baseCpuNanos) / 1000000L);
        }
        long sleepMs = 0L;
        long awakeMs = 0L;
        for (Map.Entry e : clientWhere.entrySet()) {
            if (((String)e.getKey()).contains("in Thread.sleep")) {
                sleepMs += ((Long)e.getValue()).longValue();
                continue;
            }
            awakeMs += ((Long)e.getValue()).longValue();
        }
        if (sleepMs > awakeMs || sleeping > 0 && awake == 0) {
            ++idleCount;
            idleMs += duration;
            return;
        }
        ++stallCount;
        totalStallMs += duration;
        if (duration > worstStallMs) {
            worstStallMs = duration;
        }
        Band band = bands[FrameDiag.bandOf(duration)];
        ++band.count;
        band.totalMs += duration;
        if (clientCpuMs >= 0L) {
            band.clientCpuMs += clientCpuMs;
            band.clientCpuOverMs += clientWindow;
        }
        band.totalCpuMs += totalCpu[0];
        band.totalCpuOverMs += totalWindow;
        blame.forEach((k, v) -> band.blame.merge((String)k, (Long)v, Long::sum));
        cpuMs.forEach((k, v) -> band.cpu.merge((String)k, (Long)v, Long::sum));
        clientWhere.forEach((k, v) -> band.clientWhere.merge((String)k, (Long)v, Long::sum));
        if (durations.size() < 20000) {
            durations.add(duration);
        }
        Stall stall = new Stall(duration, clientState, blame, cpuMs, clientCpuMs, clientWhere, totalCpu[0]);
        worst.add(stall);
        worst.sort(Comparator.comparingLong(Stall::durationMs).reversed());
        while (worst.size() > 3) {
            worst.remove(worst.size() - 1);
        }
    }

    private static int bandOf(long durationMs) {
        for (int i = 0; i < BAND_MAX.length; ++i) {
            if (durationMs >= BAND_MAX[i]) continue;
            return i;
        }
        return BAND_MAX.length - 1;
    }

    private static void credit(Map<String, Long> blame, List<String> pending, long ms) {
        if (ms > 0L) {
            for (String frame : pending) {
                blame.merge(frame, ms, Long::sum);
            }
        }
        pending.clear();
    }

    private static Map<Long, Long> snapshotCpu() {
        HashMap<Long, Long> out = new HashMap<Long, Long>();
        if (!CPU_TIME_OK) {
            return out;
        }
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                long cpu = MX.getThreadCpuTime(t.getId());
                if (cpu < 0L) continue;
                out.put(t.getId(), cpu);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return out;
    }

    private static long diffCpu(Map<Long, Long> start, Map<String, Long> perPoolMs, long[] totalOut) {
        if (!CPU_TIME_OK || start.isEmpty()) {
            return -1L;
        }
        long clientMs = -1L;
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                long ms;
                long now;
                Long before = start.get(t.getId());
                if (before == null || (now = MX.getThreadCpuTime(t.getId())) < 0L || (ms = (now - before) / 1000000L) <= 0L) continue;
                totalOut[0] = totalOut[0] + ms;
                if (t.getName().equals("Render thread")) {
                    clientMs = ms;
                    continue;
                }
                if (!FrameDiag.accountable(t.getName())) continue;
                perPoolMs.merge(FrameDiag.pool(t.getName()), ms, Long::sum);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return clientMs;
    }

    private static boolean accountable(String name) {
        return name.equals("Render thread") || name.startsWith("streamsreflowing-") || name.startsWith("c2me") || name.startsWith("Worker-Main") || name.startsWith("Server thread") || name.equals("main");
    }

    private static String sampleInto(List<String> out) {
        String clientState = null;
        Thread self = Thread.currentThread();
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            Thread t = e.getKey();
            String name = t.getName();
            StackTraceElement[] st = e.getValue();
            if (st.length == 0 || t == self) continue;
            if (name.equals("Render thread")) {
                clientState = String.valueOf((Object)t.getState()) + " in " + FrameDiag.shorten(st[0]) + FrameDiag.lockDetail(t);
                continue;
            }
            if (t.getState() != Thread.State.RUNNABLE || !name.startsWith("streamsreflowing-") && !name.startsWith("c2me-") && !name.startsWith("Worker-Main") && !name.equals("main") && !name.startsWith("Server thread")) continue;
            Object frame = null;
            for (StackTraceElement f : st) {
                String cls = f.getClassName();
                if (cls.endsWith(".FrameDiag") || cls.contains("ReflowingFabricClient") || cls.contains("ReflowingNeoForgeClient") || !cls.startsWith("dev.streamsreflowing.")) continue;
                frame = FrameDiag.shorten(f);
                break;
            }
            if (frame == null) {
                frame = "[not ours] " + FrameDiag.shorten(st[0]);
            }
            out.add(frame + " [" + FrameDiag.pool(name) + "]");
        }
        return clientState;
    }

    private static String lockDetail(Thread t) {
        if (!CPU_TIME_OK) {
            return "";
        }
        try {
            ThreadInfo info = MX.getThreadInfo(t.getId());
            if (info == null) {
                return "";
            }
            String owner = info.getLockOwnerName();
            String lock = info.getLockName();
            if (owner != null) {
                return " blocked by " + FrameDiag.pool(owner);
            }
            if (lock != null) {
                int at = lock.indexOf(64);
                String cls = at > 0 ? lock.substring(0, at) : lock;
                int dot = cls.lastIndexOf(46);
                return " on " + (dot < 0 ? cls : cls.substring(dot + 1));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "";
    }

    private static String pool(String name) {
        if (name.startsWith("streamsreflowing-")) {
            return "sr-" + name.substring("streamsreflowing-".length()).replaceAll("[-_]?\\d+$", "");
        }
        if (name.startsWith("c2me")) {
            return "c2me";
        }
        if (name.startsWith("Worker-Main")) {
            return "mc-worker";
        }
        if (name.startsWith("Server thread")) {
            return "server";
        }
        return name;
    }

    private static String shorten(StackTraceElement f) {
        String cls = f.getClassName();
        int dot = cls.lastIndexOf(46);
        return (dot < 0 ? cls : cls.substring(dot + 1)) + "." + f.getMethodName();
    }

    private static String top(Map<String, Long> blame, int limit) {
        StringBuilder sb = new StringBuilder();
        blame.entrySet().stream().sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()).limit(limit).forEach(en -> sb.append(sb.length() == 0 ? "" : "; ").append(en.getValue()).append("ms ").append((String)en.getKey()));
        return sb.length() == 0 ? "nothing of ours was running" : sb.toString();
    }

    private static void report() {
        if (stallCount == 0) {
            Constants.LOG.info("[SR-FRAME] {} ticks, no freeze longer than {}ms ({} idle stretches ignored)", ticks, STALL_MS, idleCount);
            return;
        }
        Constants.LOG.info("[SR-FRAME] {} {}, {} freezes over {}ms, {}ms lost, worst {}ms, median {}ms; {} idle ignored ({}ms)", ticks, perFrame ? "frames" : "TICKS ONLY (sub-100ms figures are tick jitter, not freezes)", stallCount, STALL_MS, totalStallMs, worstStallMs, FrameDiag.medianMs(), idleCount, idleMs);
        for (int i = 0; i < bands.length; ++i) {
            Band b = bands[i];
            if (b.count == 0) continue;
            Constants.LOG.info("[SR-FRAME]   {} x{}: {}ms lost, avg {}ms, client {}, whole game used {} | cpu: {}", BAND_NAME[i], b.count, b.totalMs, b.totalMs / (long)b.count, FrameDiag.share(b), FrameDiag.cores(b), FrameDiag.cpuLine(b.cpu));
            Constants.LOG.info("[SR-FRAME]       client was: {}", (Object)FrameDiag.top(b.clientWhere, 3));
            Constants.LOG.info("[SR-FRAME]       running: {}", (Object)FrameDiag.top(b.blame, 6));
        }
        for (Stall s : worst) {
            Constants.LOG.info("[SR-FRAME]   longest {}ms: client {}{} | running: {}", s.durationMs(), s.clientState(), FrameDiag.clientShare(s), FrameDiag.top(s.blame(), 4));
        }
    }

    private static String clientShare(Stall s) {
        if (s.clientCpuMs() < 0L || s.durationMs() <= 0L) {
            return "";
        }
        return String.format(", got %dms cpu (%d%% of a core)", s.clientCpuMs(), Math.round(100.0 * (double)s.clientCpuMs() / (double)s.durationMs()));
    }

    private static String cpuLine(Map<String, Long> cpuMs) {
        if (cpuMs.isEmpty()) {
            return "unavailable";
        }
        StringBuilder sb = new StringBuilder();
        cpuMs.entrySet().stream().sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()).limit(5L).forEach(en -> sb.append(sb.length() == 0 ? "" : ", ").append((String)en.getKey()).append(' ').append(en.getValue()).append("ms"));
        return sb.toString();
    }

    private static String share(Band b) {
        if (b.clientCpuOverMs <= 0L) {
            return "cpu unknown";
        }
        return "held " + Math.round(100.0 * (double)b.clientCpuMs / (double)b.clientCpuOverMs) + "% of a core";
    }

    private static String cores(Band b) {
        if (b.totalCpuOverMs <= 0L) {
            return "?";
        }
        double used = (double)b.totalCpuMs / (double)b.totalCpuOverMs;
        return String.format("%.1f of %d cores", used, Runtime.getRuntime().availableProcessors());
    }

    private static long medianMs() {
        if (durations.isEmpty()) {
            return 0L;
        }
        long[] d = durations.stream().mapToLong(Long::longValue).sorted().toArray();
        return d[d.length / 2];
    }

    static {
        MX = ManagementFactory.getThreadMXBean();
        CPU_TIME_OK = FrameDiag.cpuTimeAvailable();
        BAND_MAX = new long[]{100L, 250L, 1000L, Long.MAX_VALUE};
        BAND_NAME = new String[]{"under 100ms", "100-250ms", "250ms-1s", "over 1s"};
        bands = FrameDiag.newBands();
        durations = new ArrayList<Long>();
        worst = new ArrayList<Stall>();
    }

    private static final class Band {
        int count;
        long totalMs;
        long clientCpuMs;
        long clientCpuOverMs;
        long totalCpuMs;
        long totalCpuOverMs;
        final Map<String, Long> blame = new HashMap<String, Long>();
        final Map<String, Long> cpu = new HashMap<String, Long>();
        final Map<String, Long> clientWhere = new HashMap<String, Long>();

        private Band() {
        }
    }

    private record Stall(long durationMs, String clientState, Map<String, Long> blame, Map<String, Long> cpuMs, long clientCpuMs, Map<String, Long> clientWhere, long totalCpuMs) {
    }
}

