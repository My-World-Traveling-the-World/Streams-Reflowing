/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.math;

public final class Hashing {
    private static final long a = -7046029254386353131L;
    private static final long b = -3335678366873096957L;
    private static final long c = -6884282663029611473L;
    private static final long d = -1800455987208640293L;

    private Hashing() {
    }

    public static long mix(long l) {
        l = (l ^ l >>> 30) * -4658895280553007687L;
        l = (l ^ l >>> 27) * -7723592293110705685L;
        return l ^ l >>> 31;
    }

    public static long hash(long l, long l2, long l3) {
        long l4 = l * -7046029254386353131L;
        l4 = Hashing.mix(l4 ^ l2 * -3335678366873096957L);
        l4 = Hashing.mix(l4 ^ l3 * -6884282663029611473L);
        return Hashing.mix(l4);
    }

    public static long hash(long l, long l2, long l3, long l4) {
        return Hashing.mix(Hashing.hash(l, l2, l3) ^ l4 * -1800455987208640293L);
    }

    public static double toUnit(long l) {
        return (double)(l >>> 11) * (double)1.110223E-16f;
    }

    public static double unit(long l, long l2, long l3) {
        return Hashing.toUnit(Hashing.hash(l, l2, l3));
    }

    public static double unit(long l, long l2, long l3, long l4) {
        return Hashing.toUnit(Hashing.hash(l, l2, l3, l4));
    }
}

