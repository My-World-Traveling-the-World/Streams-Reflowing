package dev.streamsreflowing.core.river;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateDomain;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** 细湖洪泛阶段单独拥有采样与边界判定；保留原迭代顺序以免改变湖形。 */
final class FineLakeRefiner {
    private static boolean a(LowResGrid lowResGrid, boolean[] blArray, int n, int n2, int n3) {
        if (n == n2) {
            return true;
        }
        int n4 = lowResGrid.gi(n);
        int n5 = lowResGrid.gj(n);
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                if (j == 0 && i == 0 || lowResGrid.valid(n4 + j, n5 + i) && blArray[lowResGrid.idx(n4 + j, n5 + i)]) continue;
                return false;
            }
        }
        return true;
    }

    static Lake refine(List<Integer> list, int n4, TerrainSampler terrainSampler, LowResGrid lowResGrid, PlateDomain plateDomain, double d, double[] dArray, boolean[] blArrayScratch, long[] timings) {
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n142;
        int n15 = 3;
        int n16 = 3 * lowResGrid.spacing;
        int n17 = lowResGrid.spacing >> 1;
        int n18 = Integer.MAX_VALUE;
        int n19 = Integer.MAX_VALUE;
        int n20 = Integer.MIN_VALUE;
        int n21 = Integer.MIN_VALUE;
        boolean[] blArray = blArrayScratch != null && blArrayScratch.length >= lowResGrid.size() ? blArrayScratch : new boolean[lowResGrid.size()];
        Arrays.fill(blArray, 0, lowResGrid.size(), false);
        for (int n144 : list) {
            blArray[n144] = true;
        }
        for (int n144 : list) {
            n13 = lowResGrid.worldX(lowResGrid.gi(n144));
            n12 = lowResGrid.worldZ(lowResGrid.gj(n144));
            n18 = Math.min(n18, n13);
            n20 = Math.max(n20, n13);
            n19 = Math.min(n19, n12);
            n21 = Math.max(n21, n12);
        }
        int n22 = n18 - n17 - n16;
        n142 = n19 - n17 - n16;
        n13 = (n20 + n17 + n16 - n22) / 3 + 1;
        n12 = (n21 + n17 + n16 - n142) / 3 + 1;
        long[] lArray = new long[n13 * n12 + 63 >>> 6];
        boolean[] blArray2 = new boolean[n13 * n12];
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        int[] nTop = new int[4];
        double[] dTop = new double[4];
        int nTopCount = 0;
        for (int n24 : list) {
            double dH = lowResGrid.height(n24);
            if (nTopCount == 4 && Double.compare(dH, dTop[3]) >= 0) continue;
            int nSlot = nTopCount < 4 ? nTopCount : 3;
            while (nSlot > 0 && Double.compare(dH, dTop[nSlot - 1]) < 0) {
                nTop[nSlot] = nTop[nSlot - 1];
                dTop[nSlot] = dTop[nSlot - 1];
                --nSlot;
            }
            nTop[nSlot] = n24;
            dTop[nSlot] = dH;
            if (nTopCount < 4) {
                ++nTopCount;
            }
        }
        int n23 = -1;
        for (int i = 0; i < nTopCount; ++i) {
            int n24 = nTop[i];
            if (!(terrainSampler.groundHeight(lowResGrid.worldX(lowResGrid.gi(n24)), lowResGrid.worldZ(lowResGrid.gj(n24))) < (double)n4)) continue;
            n23 = n24;
            break;
        }
        if (n23 < 0) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-FINEFLOOD] floor-dry level={} fill={}", (Object)n4, (Object)list.size());
            }
            return null;
        }
        ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
        for (int n25 : list) {
            if (!FineLakeRefiner.a(lowResGrid, blArray, n25, n23, n4)) continue;
            arrayList2.add(n25);
        }
        long l = System.nanoTime();
        ArrayList<Integer> arrayList3 = arrayList2;
        LowResGrid.parallelFor(arrayList3.size(), n -> {
            int n2 = (Integer)arrayList3.get(n);
            terrainSampler.groundHeight(lowResGrid.worldX(lowResGrid.gi(n2)), lowResGrid.worldZ(lowResGrid.gj(n2)));
        });
        long[] lArray2 = timings;
        lArray2[7] = lArray2[7] + (System.nanoTime() - l);
        int n26 = 0;
        Iterator<Integer> iteratorCells = arrayList2.iterator();
        while (iteratorCells.hasNext()) {
            int n27;
            int n28 = iteratorCells.next();
            int n29 = lowResGrid.worldX(lowResGrid.gi(n28));
            double d2 = terrainSampler.groundHeight(n29, n27 = lowResGrid.worldZ(lowResGrid.gj(n28)));
            if (d2 >= (double)n4) {
                ++n26;
                continue;
            }
            if (dArray != null) {
                dArray[0] = dArray[0] + ((double)n4 - d2);
                dArray[1] = dArray[1] + 1.0;
            }
            n11 = (n29 - n17 - n22) / 3;
            n10 = (n29 + n17 - n22) / 3;
            n9 = (n27 - n17 - n142) / 3;
            n8 = (n27 + n17 - n142) / 3;
            for (n7 = n9; n7 <= n8; ++n7) {
                for (n6 = n11; n6 <= n10; ++n6) {
                    if (n6 < 0 || n6 >= n13 || n7 < 0 || n7 >= n12 || blArray2[n5 = n7 * n13 + n6]) continue;
                    blArray2[n5] = true;
                    int n30 = n5 >>> 6;
                    lArray[n30] = lArray[n30] | 1L << (n5 & 0x3F);
                    arrayDeque.add(n5);
                }
            }
        }
        if (n26 * 2 > arrayList2.size()) {
            if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                Constants.LOG.info("[SR-FINEFLOOD] interior-dry {}/{} level={} fill={}", n26, arrayList2.size(), n4, list.size());
            }
            return null;
        }
        boolean[] blArray5 = new boolean[n13 * n12];
        int n31 = n17 / 3 + 5;
        boolean[] blArray6 = blArray5;
        AbstractCollection abstractCollection = null;
        AbstractCollection abstractCollection2 = null;
        // 队列改用 long 打包（原来是 int：(gx << 14) | (gz << 4) | depth）。
        // int 版本里 gz 只有 10 位（掩码 1023）、深度只有 4 位（& 15）：
        //   · 窗口 z 跨度超过 1024 个 3 格单元（>3072 格）时 gz 会溢进 gx 位，静默变成另一个格子；
        //   · 深度上限 n31 超过 15 时 (pd+1) & 15 回绕到 0，BFS 会重复入队/绕圈。
        // 两者都不打日志、只表现为"采样结果不对"。long 打包给 gx/gz 各 24 位、depth 16 位，
        // 位宽不可能撞；入队顺序与格子集合与 int 版完全一致 ⇒ 等价改造。
        long[] bfsQueue = new long[128];
        int bfsHead = 0;
        int bfsTail = 0;
        Iterator<Integer> iteratorBase = list.iterator();
        while (iteratorBase.hasNext()) {
            int n32 = iteratorBase.next();
            if (FineLakeRefiner.a(lowResGrid, blArray, n32, n23, n4)) continue;
            n11 = (lowResGrid.worldX(lowResGrid.gi(n32)) - n22) / 3;
            n10 = (lowResGrid.worldZ(lowResGrid.gj(n32)) - n142) / 3;
            if (n11 < 0 || n11 >= n13 || n10 < 0 || n10 >= n12 || blArray6[n10 * n13 + n11] != false) continue;
            blArray6[n10 * n13 + n11] = true;
            if (bfsTail == bfsQueue.length) bfsQueue = Arrays.copyOf(bfsQueue, bfsQueue.length * 2);
            bfsQueue[bfsTail++] = ((long) n11 << 40) | ((long) n10 << 16);
        }
        long[] bfsCollected = new long[Math.max(16, bfsTail)];
        int bfsCount = 0;
        while (bfsHead < bfsTail) {
            long packed = bfsQueue[bfsHead++];
            int px = (int) (packed >>> 40);
            int pz = (int) ((packed >>> 16) & 0xFFFFFFL);
            int pd = (int) (packed & 0xFFFFL);
            if (bfsCount == bfsCollected.length) bfsCollected = Arrays.copyOf(bfsCollected, bfsCollected.length * 2);
            bfsCollected[bfsCount++] = packed;
            if (pd >= n31) continue;
            for (n11 = -1; n11 <= 1; ++n11) {
                for (n10 = -1; n10 <= 1; ++n10) {
                    n9 = px + n10;
                    n8 = pz + n11;
                    if (n10 == 0 && n11 == 0 || n9 < 0 || n9 >= n13 || n8 < 0 || n8 >= n12 || blArray6[n8 * n13 + n9] != false) continue;
                    blArray6[n8 * n13 + n9] = true;
                    if (bfsTail == bfsQueue.length) bfsQueue = Arrays.copyOf(bfsQueue, bfsQueue.length * 2);
                    bfsQueue[bfsTail++] = ((long) n9 << 40) | ((long) n8 << 16)
                            | (((long) (pd + 1)) & 0xFFFFL);
                }
            }
        }
        long l2 = System.nanoTime();
        final long[] arrayList4 = bfsCollected;
        LowResGrid.parallelFor(bfsCount, n3 -> {
            long packed = arrayList4[n3];
            terrainSampler.groundHeight(n22 + (int) (packed >>> 40) * 3,
                    n142 + (int) ((packed >>> 16) & 0xFFFFFFL) * 3);
        });
        long[] lArray3 = timings;
        lArray3[7] = lArray3[7] + (System.nanoTime() - l2);
        n31 = Math.max(4096, 4 * list.size() * (lowResGrid.spacing / 3 + 1) * (lowResGrid.spacing / 3 + 1));
        int n33 = 0;
        abstractCollection = new ArrayList();
        while (!arrayDeque.isEmpty()) {
            abstractCollection.add((Integer)((Integer)arrayDeque.poll()));
        }
        abstractCollection2 = new ArrayList<int[]>();
        while (!abstractCollection.isEmpty()) {
            int n34;
            int n35;
            int n36;
            int n37;
            int n38;
            int n39;
            int n40;
            long[] arrayListWork = new long[128];
            int workCount = 0;
            Iterator iterator = abstractCollection.iterator();
            while (iterator.hasNext()) {
                n10 = (Integer)iterator.next();
                int n41 = n10 % n13;
                n8 = n10 / n13;
                for (n7 = 0; n7 < 4; ++n7) {
                    n6 = n41 + (n7 == 0 ? -1 : (n7 == 1 ? 1 : 0));
                    n5 = n8 + (n7 == 2 ? -1 : (n7 == 3 ? 1 : 0));
                    if (n6 < 0 || n6 >= n13 || n5 < 0 || n5 >= n12 || blArray2[n5 * n13 + n6]) continue;
                    n40 = n22 + n6 * 3;
                    n39 = n142 + n5 * 3;
                    if (blArray5[n5 * n13 + n6] == false) {
                        blArray5[n5 * n13 + n6] = true;
                        if (workCount == arrayListWork.length) arrayListWork = Arrays.copyOf(arrayListWork, arrayListWork.length * 2);
                        arrayListWork[workCount++] = ((long)n40 << 32) | ((long)n39 & 0xFFFFFFFFL);
                    }
                    n38 = n22 + n41 * 3;
                    n37 = n142 + n8 * 3;
                    n36 = Integer.signum(n40 - n38);
                    n35 = Integer.signum(n39 - n37);
                    for (n34 = 1; n34 < 3; ++n34) {
                        if (workCount == arrayListWork.length) arrayListWork = Arrays.copyOf(arrayListWork, arrayListWork.length * 2);
                        arrayListWork[workCount++] = ((long)(n38 + n36 * n34) << 32) | ((long)(n37 + n35 * n34) & 0xFFFFFFFFL);
                    }
                }
            }
            if (workCount > 0) {
                long l3 = System.nanoTime();
                final long[] arrayList5 = arrayListWork;
                LowResGrid.parallelFor(workCount, n -> {
                    long packed = arrayList5[n];
                    terrainSampler.groundHeight((int)(packed >> 32), (int)packed);
                });
                long[] lArray4 = timings;
                lArray4[7] = lArray4[7] + (System.nanoTime() - l3);
            }
            for (int i = 0; i < abstractCollection.size(); ++i) {
                if (++n33 > n31) {
                    if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                        Constants.LOG.info("[SR-FINEFLOOD] budget level={} fill={} budget={}", n4, list.size(), n31);
                    }
                    return null;
                }
                n10 = (Integer)((List)abstractCollection).get(i);
                int n42 = n10 % n13;
                n8 = n10 / n13;
                for (n7 = 0; n7 < 4; ++n7) {
                    n6 = n42 + (n7 == 0 ? -1 : (n7 == 1 ? 1 : 0));
                    n5 = n8 + (n7 == 2 ? -1 : (n7 == 3 ? 1 : 0));
                    if (n6 < 0 || n6 >= n13 || n5 < 0 || n5 >= n12 || blArray2[n40 = n5 * n13 + n6]) continue;
                    blArray2[n40] = true;
                    n39 = n22 + n6 * 3;
                    n38 = n142 + n5 * 3;
                    if (!(terrainSampler.groundHeight(n39, n38) < (double)n4)) continue;
                    n37 = n22 + n42 * 3;
                    n36 = n142 + n8 * 3;
                    n35 = Integer.signum(n39 - n37);
                    n34 = Integer.signum(n38 - n36);
                    boolean bl = false;
                    for (int j = 1; j < 3 && !bl; ++j) {
                        bl = terrainSampler.groundHeight(n37 + n35 * j, n36 + n34 * j) >= (double)n4;
                    }
                    if (bl) {
                        blArray2[n40] = false;
                        continue;
                    }
                    if (!plateDomain.contains(n39, n38) || plateDomain.borderDistance(n39, n38) < d) {
                        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                            Constants.LOG.info("[SR-FINEFLOOD] border at {},{} level={} fill={}", n39, n38, n4, list.size());
                        }
                        return null;
                    }
                    if (n6 == 0 || n6 == n13 - 1 || n5 == 0 || n5 == n12 - 1) {
                        if (Boolean.getBoolean("streamsreflowing.caldiag")) {
                            Constants.LOG.info("[SR-FINEFLOOD] bound at {},{} level={} fill={}", n39, n38, n4, list.size());
                        }
                        return null;
                    }
                    if (dArray != null) {
                        dArray[0] = dArray[0] + ((double)n4 - terrainSampler.groundHeight(n39, n38));
                        dArray[1] = dArray[1] + 1.0;
                    }
                    int n43 = n40 >>> 6;
                    lArray[n43] = lArray[n43] | 1L << (n40 & 0x3F);
                    abstractCollection2.add(n40);
                }
            }
            AbstractCollection abstractCollection3 = abstractCollection;
            abstractCollection = abstractCollection2;
            abstractCollection2 = abstractCollection3;
            abstractCollection2.clear();
        }
        return new Lake(n4, 0, 3, n22, n142, n13, n12, lArray);
    }

}
