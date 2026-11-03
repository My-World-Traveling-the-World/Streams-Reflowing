/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core;

public final class ColumnKey {
    private ColumnKey() {
    }

    public static long of(int n, int n2) {
        return ColumnKey.mix(ColumnKey.pack(n, n2));
    }

    public static long pack(int n, int n2) {
        return (long)n << 32 ^ (long)n2 & 0xFFFFFFFFL;
    }

    public static long mix(long l) {
        long l2 = l;
        l2 ^= l2 >>> 33;
        l2 *= -49064778989728563L;
        l2 ^= l2 >>> 33;
        l2 *= -4265267296055464877L;
        l2 ^= l2 >>> 33;
        return l2;
    }
}

