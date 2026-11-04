/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.grid;

import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class LowResGrid {
    public final int nx;
    public final int nz;
    public final int spacing;
    public final int originX;
    public final int originZ;
    private final double[] a;
    private final boolean[] b;
    private static final int c = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    private static final AtomicLong d = new AtomicLong();
    private static final ThreadPoolExecutor e = new ThreadPoolExecutor(c, c, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(), runnable -> {
        Thread thread = new Thread(runnable, "streamsreflowing-sampler");
        thread.setDaemon(true);
        thread.setPriority(1);
        return thread;
    });
    private static final int f = 4096;
    private static final AtomicInteger g = new AtomicInteger();
    private static volatile int[] h = new int[0];
    private static final ThreadLocal<long[]> i = ThreadLocal.withInitial(() -> new long[]{Long.MAX_VALUE});
    private static final double j = 8.0;
    private static final double k = 1.0;
    private long[] l;

    public static void setBackgroundThrottle(int n) {
        if (g.get() > 0) {
            LowResGrid.a(c);
            return;
        }
        LowResGrid.a(n >= 4 ? c : (n == 3 ? Math.max(2, c * 3 / 4) : (n == 2 ? Math.max(2, c / 3) : (n == 1 ? Math.max(2, c / 5) : 1))));
    }

    /**
     * 登记一个「紧急等待者」：调用方即将阻塞等待某个 plate，这段时间 sampler 池可按满宽跑。
     *
     * <p>返回本次是否真的计了数。prefetch 线程不计数（它自己就是被让路的对象），因此
     * 调用方必须据此决定 finally 里要不要调 {@link #urgentWaiterDone()}。否则计数器会单向
     * 漂负，{@code g > 0} 恒假，依赖它的让路逻辑与配额快路径会静默失效。
     */
    public static boolean noteUrgentWaiter() {
        if (Thread.currentThread().getName().startsWith("streamsreflowing-prefetch")) {
            return false;
        }
        g.incrementAndGet();
        LowResGrid.a(c);
        return true;
    }

    public static void urgentWaiterDone() {
        g.decrementAndGet();
    }

    public static int urgentWaiterCount() {
        return g.get();
    }

    public static void setFocusPoints(int[] nArray) {
        h = nArray != null ? nArray : new int[]{};
    }

    private static long a(int n, int n2) {
        int[] nArray = h;
        long l = Long.MAX_VALUE;
        int n3 = 0;
        while (n3 + 1 < nArray.length) {
            long l2 = nArray[n3] - n;
            long l3 = nArray[n3 + 1] - n2;
            long l4 = l2 * l2 + l3 * l3;
            if (l4 < l) {
                l = l4;
            }
            n3 += 2;
        }
        return l;
    }

    public static void enterBuildFocus(int n, int n2) {
        LowResGrid.i.get()[0] = LowResGrid.a(n, n2);
    }

    public static void exitBuildFocus() {
        LowResGrid.i.get()[0] = Long.MAX_VALUE;
    }

    public static int samplerWidth() {
        return e.getCorePoolSize();
    }

    public static int samplerFullWidth() {
        return c;
    }

    private static void a(int n) {
        if (e.getCorePoolSize() == n && e.getMaximumPoolSize() == n) {
            return;
        }
        if (n >= e.getMaximumPoolSize()) {
            e.setMaximumPoolSize(n);
            e.setCorePoolSize(n);
        } else {
            e.setCorePoolSize(n);
            e.setMaximumPoolSize(n);
        }
    }

    private static int a() {
        return Thread.currentThread().getName().startsWith("streamsreflowing-prefetch") ? 1 : 0;
    }

    public LowResGrid(PlateDomain plateDomain, TerrainSampler terrainSampler, StreamsConfig streamsConfig) {
        this.spacing = streamsConfig.gridSpacing();
        this.originX = plateDomain.minX;
        this.originZ = plateDomain.minZ;
        this.nx = Math.max(1, (plateDomain.maxX - plateDomain.minX) / this.spacing + 1);
        this.nz = Math.max(1, (plateDomain.maxZ - plateDomain.minZ) / this.spacing + 1);
        this.a = new double[this.nx * this.nz];
        this.b = new boolean[this.nx * this.nz];
        int n = Math.max(this.spacing, streamsConfig.terrainSampleSpacing());
        if (n > this.spacing) {
            this.a(plateDomain, terrainSampler, streamsConfig, n);
        } else if (terrainSampler.batchCapable()) {
            this.a(plateDomain, terrainSampler, streamsConfig);
        } else if (this.nx * this.nz >= 4096) {
            this.b(plateDomain, terrainSampler, streamsConfig);
        } else {
            this.a(0, this.nz, plateDomain, terrainSampler, streamsConfig);
        }
    }

    private void a(PlateDomain plateDomain, TerrainSampler terrainSampler, StreamsConfig streamsConfig) {
        int n;
        int[] nArray = new int[this.nx * this.nz];
        int[] nArray2 = new int[this.nx * this.nz];
        int[] nArray3 = new int[this.nx * this.nz];
        int n2 = 0;
        for (int i = 0; i < this.nz; ++i) {
            for (n = 0; n < this.nx; ++n) {
                boolean bl;
                int n3 = this.worldX(n);
                int n4 = this.worldZ(i);
                int n5 = this.idx(n, i);
                this.b[n5] = bl = plateDomain.contains(n3, n4) && plateDomain.borderDistance(n3, n4) >= streamsConfig.borderMargin();
                if (!bl) continue;
                nArray[n2] = n3;
                nArray2[n2] = n4;
                nArray3[n2] = n5;
                ++n2;
            }
        }
        double[] dArray = new double[n2];
        terrainSampler.heightsBatch(nArray, nArray2, dArray, n2);
        for (n = 0; n < n2; ++n) {
            this.a[nArray3[n]] = dArray[n];
        }
    }

    private void a(int n, int n2, PlateDomain plateDomain, TerrainSampler terrainSampler, StreamsConfig streamsConfig) {
        for (int i = n; i < n2; ++i) {
            for (int j = 0; j < this.nx; ++j) {
                boolean bl;
                int n3 = this.worldX(j);
                int n4 = this.worldZ(i);
                int n5 = this.idx(j, i);
                this.b[n5] = bl = plateDomain.contains(n3, n4) && plateDomain.borderDistance(n3, n4) >= streamsConfig.borderMargin();
                if (!bl) continue;
                this.a[n5] = terrainSampler.height(n3, n4);
            }
        }
    }

    private void b(PlateDomain plateDomain, TerrainSampler terrainSampler, StreamsConfig streamsConfig) {
        LowResGrid.a(this.nz, (n, n2) -> this.a(n, n2, plateDomain, terrainSampler, streamsConfig));
    }

    private void a(PlateDomain plateDomain, TerrainSampler terrainSampler, StreamsConfig streamsConfig, int n) {
        int n2 = Math.floorDiv(this.originX, n) * n;
        int n3 = Math.floorDiv(this.originZ, n) * n;
        int n4 = (Math.floorDiv(this.worldX(this.nx - 1), n) * n + n - n2) / n + 1;
        int n7 = (Math.floorDiv(this.worldZ(this.nz - 1), n) * n + n - n3) / n + 1;
        double[] dArray = new double[n4 * n7];
        boolean[] blArray = new boolean[n4 * n7];
        LowResGrid.a(this.nz, (n5, n6) -> {
            for (int i = n5; i < n6; ++i) {
                for (int j = 0; j < this.nx; ++j) {
                    int wz;
                    boolean bl;
                    int n8 = this.worldX(j);
                    this.b[this.idx((int)j, (int)i)] = bl = plateDomain.contains(n8, wz = this.worldZ(i)) && plateDomain.borderDistance(n8, wz) >= streamsConfig.borderMargin();
                    if (!bl) continue;
                    int n9 = (Math.floorDiv(n8, n) * n - n2) / n;
                    int n10 = (Math.floorDiv(wz, n) * n - n3) / n;
                    int n11 = n9 + n10 * n4;
                    blArray[n11] = true;
                    blArray[n11 + 1] = true;
                    blArray[n11 + n4] = true;
                    blArray[n11 + n4 + 1] = true;
                }
            }
        });
        if (terrainSampler.useHybridGridFill()) {
            this.a(terrainSampler, streamsConfig, n, n2, n3, n4, n7, dArray, blArray);
        } else if (terrainSampler.batchCapable()) {
            int n8;
            int[] nArray = new int[n4 * n7];
            int[] nArray2 = new int[n4 * n7];
            int[] nArray3 = new int[n4 * n7];
            int n9 = 0;
            for (int i = 0; i < n7; ++i) {
                n8 = n3 + i * n;
                for (int j = 0; j < n4; ++j) {
                    int n10 = j + i * n4;
                    if (!blArray[n10]) continue;
                    nArray[n9] = n2 + j * n;
                    nArray2[n9] = n8;
                    nArray3[n9] = n10;
                    ++n9;
                }
            }
            double[] dArray2 = new double[n9];
            terrainSampler.heightsBatch(nArray, nArray2, dArray2, n9);
            for (n8 = 0; n8 < n9; ++n8) {
                dArray[nArray3[n8]] = dArray2[n8];
            }
        } else {
            LowResGrid.a(n7, (n5, n6) -> {
                for (int i = n5; i < n6; ++i) {
                    int wu = n3 + i * n;
                    for (int j = 0; j < n4; ++j) {
                        int n8 = j + i * n4;
                        if (!blArray[n8]) continue;
                        dArray[n8] = terrainSampler.height(n2 + j * n, wu);
                    }
                }
            });
        }
        LowResGrid.a(this.nz, (n5, n6) -> {
            for (int i = n5; i < n6; ++i) {
                for (int j = 0; j < this.nx; ++j) {
                    int wq = this.idx(j, i);
                    if (!this.b[wq]) continue;
                    this.a[wq] = LowResGrid.a(this.worldX(j), this.worldZ(i), n, dArray, n4, n2, n3);
                }
            }
        });
    }

    private void a(TerrainSampler terrainSampler, StreamsConfig streamsConfig, int n, int n2, int n3, int n4, int n7, double[] dArray, boolean[] blArray) {
        int n10 = Math.floorDiv(n2, n);
        int n11 = Math.floorDiv(n3, n);
        boolean[] blArray2 = new boolean[n4 * n7];
        for (int i = 0; i < n7; ++i) {
            boolean bl = (n11 + i & 1) == 0;
            for (int j = 0; j < n4; ++j) {
                int n12 = j + i * n4;
                if (!blArray[n12]) continue;
                if (bl && (n10 + j & 1) == 0) {
                    blArray2[n12] = true;
                    continue;
                }
                int n13 = (n10 + j & 1) == 0 ? j : j - 1;
                int n14 = bl ? i : i - 1;
                for (int k = 0; k <= 2; k += 2) {
                    for (int i2 = 0; i2 <= 2; i2 += 2) {
                        int n15 = n13 + i2;
                        int n16 = n14 + k;
                        if (n15 < 0 || n15 >= n4 || n16 < 0 || n16 >= n7) continue;
                        blArray2[n15 + n16 * n4] = true;
                    }
                }
            }
        }
        LowResGrid.a(n7, (n5, n6) -> {
            for (int i = n5; i < n6; ++i) {
                int wv = n3 + i * n;
                for (int j = 0; j < n4; ++j) {
                    int n8 = j + i * n4;
                    if (!blArray2[n8]) continue;
                    dArray[n8] = terrainSampler.height(n2 + j * n, wv);
                }
            }
        });
        double[] dArray2 = new double[n4 * n7];
        LowResGrid.a(n7, (n5, n6) -> {
            for (int i = n5; i < n6; ++i) {
                int wy = n3 + i * n;
                for (int j = 0; j < n4; ++j) {
                    int n8 = j + i * n4;
                    if (!blArray[n8] && !blArray2[n8]) continue;
                    dArray[n8] = terrainSampler.estimate(n2 + j * n, wy);
                }
            }
        });
        double d = (double)streamsConfig.seaLevel() + 8.0;
        LowResGrid.a(n7, (n8, n9) -> {
            for (int i = n8; i < n9; ++i) {
                boolean bl = (n11 + i & 1) == 0;
                int wr = n3 + i * n;
                for (int j = 0; j < n4; ++j) {
                    int wc = j + i * n4;
                    if (!blArray[wc] || blArray2[wc]) continue;
                    int n12 = n2 + j * n;
                    int n13 = (wr + j & 1) == 0 ? j : j - 1;
                    int n14 = bl ? i : i - 1;
                    dArray[wc] = LowResGrid.a(terrainSampler, dArray, dArray2, n4, n7, n13, n14, j, i, n12, wr, d);
                }
            }
        });
    }

    private static double a(TerrainSampler terrainSampler, double[] dArray, double[] dArray2, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, double d) {
        double d2;
        if (n3 < 0 || n4 < 0 || n3 + 2 >= n || n4 + 2 >= n2) {
            return terrainSampler.height(n7, n8);
        }
        int n9 = n3 + n4 * n;
        int n10 = n9 + 2;
        int n11 = n9 + 2 * n;
        int n12 = n11 + 2;
        double d3 = dArray[n9];
        double d4 = dArray[n10];
        double d5 = dArray[n11];
        double d6 = dArray[n12];
        if (d3 == -1000.0 || d4 == -1000.0 || d5 == -1000.0 || d6 == -1000.0) {
            return terrainSampler.height(n7, n8);
        }
        double d7 = (double)(n5 - n3) / 2.0;
        double d8 = (double)(n6 - n4) / 2.0;
        double d9 = dArray2[n9];
        double d10 = dArray2[n10];
        double d11 = dArray2[n11];
        double d12 = dArray2[n12];
        double d13 = dArray2[n5 + n6 * n];
        double d14 = Math.min(Math.min(d9, d10), Math.min(d11, d12));
        double d15 = Math.max(Math.max(d9, d10), Math.max(d11, d12));
        if (d13 > d15 + 1.0 || d13 < d14 - 1.0) {
            return terrainSampler.height(n7, n8);
        }
        double d16 = LowResGrid.a(d3, d4, d5, d6, d7, d8);
        double d17 = d16 + (d13 - (d2 = LowResGrid.a(d9, d10, d11, d12, d7, d8)));
        if (d17 <= d || d16 <= d) {
            return terrainSampler.height(n7, n8);
        }
        return d17;
    }

    private static double a(double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = d + (d2 - d) * d5;
        double d8 = d3 + (d4 - d3) * d5;
        return d7 + (d8 - d7) * d6;
    }

    private static double a(int n, int n2, int n3, double[] dArray, int n4, int n5, int n6) {
        int n7 = Math.floorDiv(n, n3) * n3;
        int n8 = Math.floorDiv(n2, n3) * n3;
        double d = (double)(n - n7) / (double)n3;
        double d2 = (double)(n2 - n8) / (double)n3;
        int n9 = (n7 - n5) / n3;
        int n10 = (n8 - n6) / n3;
        int n11 = n9 + n10 * n4;
        double d3 = dArray[n11];
        double d4 = dArray[n11 + 1];
        double d5 = dArray[n11 + n4];
        double d6 = dArray[n11 + n4 + 1];
        double d7 = d3 + (d4 - d3) * d;
        double d8 = d5 + (d6 - d5) * d;
        return d7 + (d8 - d7) * d2;
    }

    private static void a(int n, RowBand rowBand) {
        int n2;
        if (n <= 0) {
            return;
        }
        if (Thread.currentThread().getName().startsWith("streamsreflowing-sampler")) {
            rowBand.run(0, n);
            return;
        }
        // 切分粒度跟随「当前 sampler 池的实际宽度」，不能写死硬件核数常量 c（= cores - 2）。
        // 预热结束后池宽常被压到 1~2，此时若仍切 min(c, n) 份，就是 N 份工作串行挤 1~2 个
        // 线程，白付队列与闩锁开销。上限仍用 c（满宽），下限 1，再按行数 n 收口。
        int n3 = Math.min(c, Math.min(Math.max(1, LowResGrid.samplerWidth()), n));
        if (n3 <= 1) {
            rowBand.run(0, n);
            return;
        }
        int n4 = (n + n3 - 1) / n3;
        ArrayList<int[]> arrayList = new ArrayList<int[]>(n3);
        for (n2 = 0; n2 < n3; ++n2) {
            int n5 = n2 * n4;
            int n6 = Math.min(n, n5 + n4);
            if (n5 >= n6) continue;
            arrayList.add(new int[]{n5, n6});
        }
        if (arrayList.size() <= 1) {
            rowBand.run(0, n);
            return;
        }
        n2 = LowResGrid.a();
        long l = i.get()[0];
        if (n2 == 0) {
            LowResGrid.a(c);
        }
        CountDownLatch countDownLatch = new CountDownLatch(arrayList.size());
        AtomicReference atomicReference = new AtomicReference();
        try {
            for (int[] nArray : arrayList) {
                int n7 = nArray[0];
                int n8 = nArray[1];
                e.execute(new PrioritizedTask(n2, l, d.getAndIncrement(), () -> {
                    try {
                        rowBand.run(n7, n8);
                    }
                    catch (Throwable throwable) {
                        atomicReference.compareAndSet(null, throwable);
                    }
                    finally {
                        countDownLatch.countDown();
                    }
                }));
            }
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            rowBand.run(0, n);
            return;
        }
        try {
            countDownLatch.await();
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted during parallel terrain sampling", interruptedException);
        }
        Throwable throwable = (Throwable)atomicReference.get();
        if (throwable != null) {
            throw new RuntimeException("parallel terrain sampling failed", throwable);
        }
    }

    public static void parallelFor(int n3, IntConsumer intConsumer) {
        LowResGrid.a(n3, (n, n2) -> {
            for (int i = n; i < n2; ++i) {
                intConsumer.accept(i);
            }
        });
    }

    public int worldX(int n) {
        return this.originX + n * this.spacing;
    }

    public int worldZ(int n) {
        return this.originZ + n * this.spacing;
    }

    public int idx(int n, int n2) {
        return n + n2 * this.nx;
    }

    public int gi(int n) {
        return n % this.nx;
    }

    public int gj(int n) {
        return n / this.nx;
    }

    public boolean valid(int n, int n2) {
        return n >= 0 && n < this.nx && n2 >= 0 && n2 < this.nz;
    }

    public double height(int n) {
        return this.a[n];
    }

    public void overrideHeight(int n, double d) {
        this.a[n] = d;
    }

    public void markRiverOutlet(int n) {
        if (this.l == null) {
            this.l = new long[this.size() + 63 >>> 6];
        }
        int n2 = n >>> 6;
        this.l[n2] = this.l[n2] | 1L << (n & 0x3F);
    }

    public boolean riverOutlet(int n) {
        return this.l != null && (this.l[n >>> 6] & 1L << (n & 0x3F)) != 0L;
    }

    public boolean inDomain(int n) {
        return this.b[n];
    }

    public int size() {
        return this.nx * this.nz;
    }

    @FunctionalInterface
    private static interface RowBand {
        public void run(int var1, int var2);
    }

    private static final class PrioritizedTask
    implements Comparable<PrioritizedTask>,
    Runnable {
        private final int a;
        private final long b;
        private final long c;
        private final Runnable d;

        PrioritizedTask(int n, long l, long l2, Runnable runnable) {
            this.a = n;
            this.b = l;
            this.c = l2;
            this.d = runnable;
        }

        @Override
        public void run() {
            this.d.run();
        }

        @Override
        public int compareTo(PrioritizedTask prioritizedTask) {
            int n = Integer.compare(this.a, prioritizedTask.a);
            if (n != 0) {
                return n;
            }
            n = Long.compare(this.b, prioritizedTask.b);
            return n != 0 ? n : Long.compare(this.c, prioritizedTask.c);
        }
    }
}

