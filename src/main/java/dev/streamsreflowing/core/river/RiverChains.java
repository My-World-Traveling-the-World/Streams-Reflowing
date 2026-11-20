/* 文件职责：集中端点量化、唯一端点连接和链编号，保持旧槽位迭代及根标签次序。 */
package dev.streamsreflowing.core.river;

import java.util.List;

/** 不对并查集合并排序，以免改变链标签和下游相等性判断的输入。 */
final class RiverChains {
    static int[] chainIds(List<RiverSegment> list) {
        int n = list.size();
        ChainEndpointIndex chainEndpointIndex = new ChainEndpointIndex(n);
        ChainEndpointIndex chainEndpointIndex2 = new ChainEndpointIndex(n);
        for (int i = 0; i < n; ++i) {
            RiverSegment riverSegment = list.get(i);
            chainEndpointIndex.add(RiverChains.key(riverSegment.x1(), riverSegment.z1()), i);
            chainEndpointIndex2.add(RiverChains.key(riverSegment.x2(), riverSegment.z2()), i);
        }
        int[] nArray = new int[n];
        for (int i = 0; i < n; ++i) {
            nArray[i] = i;
        }
        for (int i = 0; i < chainEndpointIndex.counts.length; ++i) {
            if (chainEndpointIndex.counts[i] != 1) continue;
            long l = chainEndpointIndex.keys[i];
            if (chainEndpointIndex2.count(l) != 1) continue;
            RiverChains.union(nArray, chainEndpointIndex2.id(l), chainEndpointIndex.ids[i]);
        }
        int[] nArray5 = new int[n];
        for (int i = 0; i < n; ++i) {
            nArray5[i] = RiverChains.find(nArray, i);
        }
        return nArray5;
    }

    static long key(double d, double d2) {
        long l = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l2 = Math.round(d2 * 16.0) & 0xFFFFFFFFL;
        return l << 32 | l2;
    }

    private static int find(int[] nArray, int n) {
        while (nArray[n] != n) {
            nArray[n] = nArray[nArray[n]];
            n = nArray[n];
        }
        return n;
    }

    private static void union(int[] nArray, int n, int n2) {
        int n3;
        int n4 = RiverChains.find(nArray, n);
        if (n4 != (n3 = RiverChains.find(nArray, n2))) {
            nArray[n4] = n3;
        }
    }

    // 计数与最后写入索引伴随编号迁移；容量和散列保持原样。
    private static final class ChainEndpointIndex {
        final long[] keys;
        final int[] counts;
        final int[] ids;
        private final int mask;

        ChainEndpointIndex(int n) {
            int n2 = 16;
            while (n2 < n * 2) {
                n2 <<= 1;
            }
            this.keys = new long[n2];
            this.counts = new int[n2];
            this.ids = new int[n2];
            this.mask = n2 - 1;
        }

        private int slot(long l) {
            long l2 = l * 0x9E3779B97F4A7C15L;
            int n = (int)(l2 ^ l2 >>> 32) & this.mask;
            while (this.counts[n] != 0 && this.keys[n] != l) {
                n = n + 1 & this.mask;
            }
            return n;
        }

        void add(long l, int n) {
            int n2 = this.slot(l);
            if (this.counts[n2] == 0) {
                this.keys[n2] = l;
            }
            ++this.counts[n2];
            this.ids[n2] = n;
        }

        int count(long l) {
            return this.counts[this.slot(l)];
        }

        int id(long l) {
            return this.ids[this.slot(l)];
        }
    }
}
