package dev.streamsreflowing.worldgen;

import java.util.Arrays;

/** 固定容量的身份键缓存；索引冲突使用探测链，不能把整条对角线压成一个存储槽。 */
final class BoundedPlanTable<P> {
    record Stats(long hits, long misses, long evictions, long probes) {}
    private final long[] keys;
    private final Object[] owners;
    private final Object[] values;
    private final int[] slots;
    private final int[] free;
    private final boolean diagnostics;
    private int available;
    private int evict;
    private long hits, misses, evictions, probes;

    BoundedPlanTable(int capacity, boolean diagnostics) {
        if (capacity <= 0 || Integer.bitCount(capacity) != 1 || capacity > 1024)
            throw new IllegalArgumentException("缓存容量必须是1至1024内的二次幂");
        this.diagnostics = diagnostics;
        keys = new long[capacity];
        owners = new Object[capacity];
        values = new Object[capacity];
        slots = new int[capacity * 2];
        free = new int[capacity];
        clear();
    }

    void put(Object owner, long key, P value) {
        int slot = locate(owner, key);
        int entry = slots[slot];
        if (entry >= 0) { values[entry] = value; return; }
        if (available > 0) entry = free[--available];
        else {
            entry = evict++ & (keys.length - 1);
            removeSlot(locate(owners[entry], keys[entry]));
            if (diagnostics) evictions++;
            // 删除会移动探测链，先前找到的空槽必须重新定位。
            slot = locate(owner, key);
        }
        keys[entry] = key;
        owners[entry] = owner;
        values[entry] = value;
        slots[slot] = entry;
    }

    @SuppressWarnings("unchecked")
    P get(Object owner, long key, boolean take) {
        int slot = locate(owner, key);
        int entry = slots[slot];
        if (entry < 0) { if (diagnostics) misses++; return null; }
        if (diagnostics) hits++;
        P value = (P) values[entry];
        if (take) {
            removeSlot(slot);
            owners[entry] = null;
            values[entry] = null;
            free[available++] = entry;
        }
        return value;
    }

    private int locate(Object owner, long key) {
        int slot = home(owner, key);
        // 正常装载率不超过一半；仍限定探测次数，避免索引损坏把世界生成线程困住。
        for (int visited = 0; visited < slots.length; visited++) {
            if (slots[slot] < 0) return slot;
            if (diagnostics) probes++;
            int entry = slots[slot];
            if (keys[entry] == key && owners[entry] == owner) return slot;
            slot = (slot + 1) & (slots.length - 1);
        }
        throw new IllegalStateException("区块计划缓存探测链已满");
    }

    private int home(Object owner, long key) {
        long hash = key ^ Integer.toUnsignedLong(System.identityHashCode(owner));
        hash = (hash ^ hash >>> 33) * 0xff51afd7ed558ccdL;
        hash = (hash ^ hash >>> 33) * 0xc4ceb9fe1a85ec53L;
        return (int) (hash ^ hash >>> 33) & (slots.length - 1);
    }

    private void removeSlot(int hole) {
        int mask = slots.length - 1;
        for (int next = (hole + 1) & mask; slots[next] >= 0; next = (next + 1) & mask) {
            int entry = slots[next];
            int home = home(owners[entry], keys[entry]);
            // 只有空洞处于该项原探测路径内才前移，跨零点的链也适用。
            if (((next - home) & mask) >= ((next - hole) & mask)) {
                slots[hole] = entry;
                hole = next;
            }
        }
        slots[hole] = -1;
    }

    void clear() {
        Arrays.fill(owners, null);
        Arrays.fill(values, null);
        Arrays.fill(slots, -1);
        for (int i = 0; i < free.length; i++) free[i] = free.length - i - 1;
        available = free.length;
        evict = 0;
        hits = misses = evictions = probes = 0;
    }

    Stats stats() { return new Stats(hits, misses, evictions, probes); }
}
