/* 冻结4ec0b9b的链编号与槽位次序，独立旧实现禁止委托新算法。 */
package dev.streamsreflowing.core.river;

import java.util.List;

final class FrozenRiverChains {
    public static int[] chainIds(List<RiverSegment> list) {
        int n = list.size();
        ChainEndpointIndex chainEndpointIndex = new ChainEndpointIndex(n);
        ChainEndpointIndex chainEndpointIndex2 = new ChainEndpointIndex(n);
        for (int i = 0; i < n; ++i) {
            RiverSegment riverSegment = list.get(i);
            chainEndpointIndex.add(FrozenRiverChains.a(riverSegment.x1(), riverSegment.z1()), i);
            chainEndpointIndex2.add(FrozenRiverChains.a(riverSegment.x2(), riverSegment.z2()), i);
        }
        int[] nArray = new int[n];
        for (int i = 0; i < n; ++i) {
            nArray[i] = i;
        }
        for (int i = 0; i < chainEndpointIndex.counts.length; ++i) {
            if (chainEndpointIndex.counts[i] != 1) continue;
            long l = chainEndpointIndex.keys[i];
            if (chainEndpointIndex2.count(l) != 1) continue;
            FrozenRiverChains.a(nArray, chainEndpointIndex2.id(l), chainEndpointIndex.ids[i]);
        }
        int[] nArray5 = new int[n];
        for (int i = 0; i < n; ++i) {
            nArray5[i] = FrozenRiverChains.a(nArray, i);
        }
        return nArray5;
    }

    private static long a(double d, double d2) {
        long l = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l2 = Math.round(d2 * 16.0) & 0xFFFFFFFFL;
        return l << 32 | l2;
    }

    private static int a(int[] nArray, int n) {
        while (nArray[n] != n) {
            nArray[n] = nArray[nArray[n]];
            n = nArray[n];
        }
        return n;
    }

    private static void a(int[] nArray, int n, int n2) {
        int n3;
        int n4 = FrozenRiverChains.a(nArray, n);
        if (n4 != (n3 = FrozenRiverChains.a(nArray, n2))) {
            nArray[n4] = n3;
        }
    }

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
