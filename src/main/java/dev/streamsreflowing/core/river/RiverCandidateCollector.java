package dev.streamsreflowing.core.river;

import java.util.Arrays;

/** 先按河段编号去重再投影；分字代次位集只随最大河段数增长，不按每次桶重复数分配。 */
final class RiverCandidateCollector {
    private long[] bits = new long[0];
    private int[] stamps = new int[0];
    private int[] ids = new int[512];
    private int epoch, size, inputs;

    void begin(int segments) {
        int words = (segments >>> 6) + ((segments & 63) == 0 ? 0 : 1);
        if (words > bits.length) {
            bits = Arrays.copyOf(bits, words);
            stamps = Arrays.copyOf(stamps, words);
        }
        // 代次回卷时只清戳，不能让很早以前的位误认成当前已访问。
        if (++epoch == 0) { Arrays.fill(stamps, 0); epoch = 1; }
        size = inputs = 0;
    }

    void addRange(int[] source, int from, int to) {
        inputs += to - from;
        for (int i = from; i < to; i++) {
            int id = source[i];
            if (!firstVisit(id)) continue;
            // 保留第一次出现的桶遍历次序；热缓存仍在唯一项上按原编号排序。
            if (size + 1 >= ids.length) ids = Arrays.copyOf(ids, ids.length * 2);
            ids[size++] = id;
        }
    }

    /** 外河流式查询只需要判重，不收集候选数组，避免为去重再分配和排序。 */
    boolean firstVisit(int id) {
        int word = id >>> 6;
        if (stamps[word] != epoch) { stamps[word] = epoch; bits[word] = 0; }
        long bit = 1L << id;
        if ((bits[word] & bit) != 0) return false;
        bits[word] |= bit;
        return true;
    }

    int[] scratch() { ids[size] = -1; return ids; }
    int[] sortedCopy() {
        int[] result = Arrays.copyOf(ids, size + 1);
        Arrays.sort(result, 0, size);
        result[size] = -1;
        return result;
    }
    int inputs() { return inputs; }
    int size() { return size; }
}
