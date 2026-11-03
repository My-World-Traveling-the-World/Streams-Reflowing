/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.river;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;

public final class RiverFlowField {
    public static final int LAND = 0;
    public static final int RIVER = 1;
    public static final int OCEAN = 2;
    private static final int[] a = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] b = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    private static final double c = Math.sqrt(2.0);
    private static final int d = 2;
    private static final int e = 3;
    private static final double[] f = new double[8];
    private static final double[] g = new double[8];
    private final int h;
    private final int i;
    private final int j;
    private final int k;
    private final int l;
    private final byte[] m;
    public final int oceanCells;
    public final int riverCells;
    public final int flowedCells;
    private static final int n = 4;
    private static final int o = 2;
    private static final int p = 8;
    private static final int q = 2;
    public static volatile int lastSampled;
    public static volatile int lastCells;
    private static final long r = 9221120239188049920L;

    private RiverFlowField(int n, int n2, int n3, int n4, int n5, byte[] byArray, int n6, int n7, int n8) {
        this.h = n;
        this.i = n2;
        this.j = n3;
        this.k = n4;
        this.l = n5;
        this.m = byArray;
        this.oceanCells = n6;
        this.riverCells = n7;
        this.flowedCells = n8;
    }

    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(this.h);
        dataOutputStream.writeInt(this.i);
        dataOutputStream.writeInt(this.j);
        dataOutputStream.writeInt(this.k);
        dataOutputStream.writeInt(this.l);
        dataOutputStream.write(this.m);
        dataOutputStream.writeInt(this.oceanCells);
        dataOutputStream.writeInt(this.riverCells);
        dataOutputStream.writeInt(this.flowedCells);
    }

    public static RiverFlowField readFrom(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        int n2 = dataInputStream.readInt();
        int n3 = dataInputStream.readInt();
        int n4 = dataInputStream.readInt();
        int n5 = dataInputStream.readInt();
        if (n3 <= 0 || n4 <= 0 || n5 <= 0 || n4 > 4096 || n5 > 4096) {
            return null;
        }
        byte[] byArray = new byte[n4 * n5];
        dataInputStream.readFully(byArray);
        int n6 = dataInputStream.readInt();
        int n7 = dataInputStream.readInt();
        int n8 = dataInputStream.readInt();
        return new RiverFlowField(n, n2, n3, n4, n5, byArray, n6, n7, n8);
    }

    private static void a(byte[] byArray, int n, int n2, int n3, int n4, int n5, int n6, BiomeClass biomeClass) {
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n14;
        int n15 = Math.max(2, Math.min(8, n6 > 0 ? n6 : 4));
        boolean[] blArray = new boolean[n * n2];
        int n16 = 0;
        for (n14 = 0; n14 < n2; n14 += n15) {
            for (n13 = 0; n13 < n; n13 += n15) {
                int n17 = n14 * n + n13;
                byArray[n17] = (byte)biomeClass.classify(n3 + n13 * n5, n4 + n14 * n5);
                blArray[n17] = true;
                ++n16;
            }
        }
        for (n14 = n15; n14 > 1; n14 >>= 1) {
            n13 = n14 >> 1;
            boolean[] blArray2 = new boolean[n * n2];
            for (n12 = 0; n12 < n2; n12 += n14) {
                for (n11 = 0; n11 < n; n11 += n14) {
                    if (!RiverFlowField.a(byArray, blArray, n, n2, n11, n12, n14)) continue;
                    n10 = 2 * n14;
                    for (n9 = -n10; n9 <= n10; n9 += n13) {
                        for (n8 = -n10; n8 <= n10; n8 += n13) {
                            n7 = n11 + n8;
                            int n18 = n12 + n9;
                            if (n7 < 0 || n7 >= n || n18 < 0 || n18 >= n2) continue;
                            blArray2[n18 * n + n7] = true;
                        }
                    }
                }
            }
            for (n12 = 0; n12 < n2; n12 += n13) {
                for (n11 = 0; n11 < n; n11 += n13) {
                    n10 = n12 * n + n11;
                    if (!blArray2[n10] || blArray[n10]) continue;
                    byArray[n10] = (byte)biomeClass.classify(n3 + n11 * n5, n4 + n12 * n5);
                    blArray[n10] = true;
                    ++n16;
                }
            }
        }
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        for (n13 = 0; n13 < n * n2; ++n13) {
            if (!blArray[n13] || byArray[n13] != 1) continue;
            arrayDeque.add(n13);
        }
        n13 = n * n2 - n16;
        while (!arrayDeque.isEmpty() && n13 > 0) {
            int n19 = (Integer)arrayDeque.poll();
            n12 = n19 % n;
            n11 = n19 / n;
            for (n10 = 0; n10 < 8 && n13 > 0; ++n10) {
                n9 = n12 + a[n10];
                n8 = n11 + b[n10];
                if (n9 < 0 || n9 >= n || n8 < 0 || n8 >= n2 || blArray[n7 = n8 * n + n9]) continue;
                byArray[n7] = (byte)biomeClass.classify(n3 + n9 * n5, n4 + n8 * n5);
                blArray[n7] = true;
                ++n16;
                --n13;
                if (byArray[n7] != 1) continue;
                arrayDeque.add(n7);
            }
        }
        for (int i = 0; i < n2; ++i) {
            for (n13 = 0; n13 < n; ++n13) {
                int n20 = i * n + n13;
                if (blArray[n20]) continue;
                n12 = Math.min(n13 - n13 % n15, n - 1);
                n11 = Math.min(i - i % n15, n2 - 1);
                byArray[n20] = byArray[n11 * n + n12];
            }
        }
        lastSampled = n16;
        lastCells = n * n2;
    }

    private static boolean a(byte[] byArray, boolean[] blArray, int n, int n2, int n3, int n4, int n5) {
        int n6 = n4 * n + n3;
        if (!blArray[n6]) {
            return false;
        }
        if (byArray[n6] == 1) {
            return true;
        }
        for (int i = 0; i < 4; ++i) {
            int n7;
            int n8 = n3 + (i == 0 ? n5 : (i == 1 ? -n5 : 0));
            int n9 = n4 + (i == 2 ? n5 : (i == 3 ? -n5 : 0));
            if (n8 < 0 || n8 >= n || n9 < 0 || n9 >= n2 || !blArray[n7 = n9 * n + n8] || byArray[n7] == byArray[n6]) continue;
            return true;
        }
        return false;
    }

    public static RiverFlowField build(int n, int n2, int n3, int n4, int n5, int n6, int n7, BiomeClass biomeClass, OrientationField orientationField) {
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n14;
        int n15;
        int n16;
        int n17 = n - n5;
        int n18 = n2 - n5;
        int n19 = (n3 + 2 * n5) / n6 + 1;
        int n20 = (n4 + 2 * n5) / n6 + 1;
        byte[] byArray = new byte[n19 * n20];
        float[] fArray = new float[n19 * n20];
        Arrays.fill(fArray, Float.POSITIVE_INFINITY);
        PriorityQueue<Long> priorityQueue = new PriorityQueue<Long>();
        RiverFlowField.a(byArray, n19, n20, n17, n18, n6, n7, biomeClass);
        int n21 = 0;
        int n22 = 0;
        for (int i = 0; i < n19 * n20; ++i) {
            if (byArray[i] == 2) {
                ++n21;
                continue;
            }
            if (byArray[i] != 1) continue;
            ++n22;
        }
        boolean[] blArray = new boolean[n19 * n20];
        boolean[] blArray2 = new boolean[n19 * n20];
        int[] nArray = new int[n19 * n20];
        Arrays.fill(nArray, -1);
        int n23 = 0;
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        for (n16 = 0; n16 < n19 * n20; ++n16) {
            if (byArray[n16] != 1 || blArray2[n16]) continue;
            n15 = n23++;
            blArray2[n16] = true;
            nArray[n16] = n15;
            arrayDeque.add(n16);
            n14 = n16;
            boolean bl = false;
            while (!arrayDeque.isEmpty()) {
                n14 = n13 = ((Integer)arrayDeque.poll()).intValue();
                n12 = n13 % n19;
                n11 = n13 / n19;
                for (n10 = 0; n10 < 8; ++n10) {
                    n9 = n12 + a[n10];
                    n8 = n11 + b[n10];
                    if (n9 < 0 || n8 < 0 || n9 >= n19 || n8 >= n20) continue;
                    int n24 = n8 * n19 + n9;
                    if (byArray[n24] == 1) {
                        if (blArray2[n24]) continue;
                        blArray2[n24] = true;
                        nArray[n24] = n15;
                        arrayDeque.add(n24);
                        continue;
                    }
                    if (byArray[n24] != 2) continue;
                    bl = true;
                }
            }
            if (!bl && orientationField == null) continue;
            blArray[n14] = true;
        }
        for (n16 = 0; n16 < n19 * n20; ++n16) {
            if (!blArray[n16]) continue;
            fArray[n16] = 0.0f;
            priorityQueue.add(RiverFlowField.a(0.0f, n16));
        }
        while (!priorityQueue.isEmpty()) {
            long l = (Long)priorityQueue.poll();
            n14 = (int)(l & 0xFFFFFFFFL);
            float f = Float.intBitsToFloat((int)(l >>> 32));
            if (f > fArray[n14]) continue;
            n13 = n14 % n19;
            n12 = n14 / n19;
            for (n11 = 0; n11 < 8; ++n11) {
                float f2;
                n10 = n13 + a[n11];
                n9 = n12 + b[n11];
                if (n10 < 0 || n9 < 0 || n10 >= n19 || n9 >= n20 || byArray[n8 = n9 * n19 + n10] != 1 || !((f2 = f + ((n11 & 1) == 0 ? 1.0f : (float)c)) < fArray[n8])) continue;
                fArray[n8] = f2;
                priorityQueue.add(RiverFlowField.a(f2, n8));
            }
        }
        byte[] byArray2 = new byte[n19 * n20];
        Arrays.fill(byArray2, (byte)-1);
        n15 = 0;
        for (n14 = 0; n14 < n20; ++n14) {
            for (int i = 0; i < n19; ++i) {
                n13 = n14 * n19 + i;
                if (byArray[n13] != 1 || fArray[n13] == Float.POSITIVE_INFINITY || (n12 = RiverFlowField.a(fArray, n19, n20, i, n14)) < 0) continue;
                byArray2[n13] = (byte)n12;
                ++n15;
            }
        }
        if (orientationField != null && n23 > 0) {
            n14 = 8;
            int n25 = (n19 - 1) / n14 + 2;
            n13 = (n20 - 1) / n14 + 2;
            float[] fArray2 = new float[n25 * n13];
            for (n11 = 0; n11 < n13; ++n11) {
                for (n10 = 0; n10 < n25; ++n10) {
                    n9 = Math.min(n19 - 1, n10 * n14);
                    n8 = Math.min(n20 - 1, n11 * n14);
                    fArray2[n11 * n25 + n10] = (float)orientationField.sample(n17 + n9 * n6, n18 + n8 * n6);
                }
            }
            double[] dArray = new double[n23];
            for (n10 = 0; n10 < n20; ++n10) {
                for (n9 = 0; n9 < n19; ++n9) {
                    n8 = n10 * n19 + n9;
                    if (byArray2[n8] < 0 || nArray[n8] < 0) continue;
                    int n26 = Math.min(n25 - 2, n9 / n14);
                    int n27 = Math.min(n13 - 2, n10 / n14);
                    double d = -(fArray2[n27 * n25 + n26 + 1] - fArray2[n27 * n25 + n26]);
                    double d2 = -(fArray2[(n27 + 1) * n25 + n26] - fArray2[n27 * n25 + n26]);
                    int n28 = nArray[n8];
                    dArray[n28] = dArray[n28] + (f[byArray2[n8]] * d + g[byArray2[n8]] * d2);
                }
            }
            for (n10 = 0; n10 < n19 * n20; ++n10) {
                if (byArray2[n10] < 0 || nArray[n10] < 0 || !(dArray[nArray[n10]] < 0.0)) continue;
                byArray2[n10] = (byte)(byArray2[n10] + 4 & 7);
            }
        }
        return new RiverFlowField(n17, n18, n6, n19, n20, byArray2, n21, n22, n15);
    }

    private static long a(float f, int n) {
        return (long)Float.floatToIntBits(f) << 32 | (long)n & 0xFFFFFFFFL;
    }

    private static int a(float[] fArray, int n, int n2, int n3, int n4) {
        double d = 0.0;
        double d2 = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                float f;
                int n5 = n3 + j;
                int n6 = n4 + i;
                if (n5 < 0 || n6 < 0 || n5 >= n || n6 >= n2 || (f = fArray[n6 * n + n5]) == Float.POSITIVE_INFINITY) continue;
                d += 1.0;
                d2 += (double)j;
                d3 += (double)i;
                d4 += (double)f;
            }
        }
        if (d < 2.0) {
            return -1;
        }
        double d5 = d2 / d;
        double d6 = d3 / d;
        double d7 = d4 / d;
        double d8 = 0.0;
        double d9 = 0.0;
        double d10 = 0.0;
        double d11 = 0.0;
        double d12 = 0.0;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                float f;
                int n7 = n3 + j;
                int n8 = n4 + i;
                if (n7 < 0 || n8 < 0 || n7 >= n || n8 >= n2 || (f = fArray[n8 * n + n7]) == Float.POSITIVE_INFINITY) continue;
                double d13 = (double)j - d5;
                double d14 = (double)i - d6;
                double d15 = (double)f - d7;
                d8 += d13 * d13;
                d9 += d13 * d14;
                d10 += d14 * d14;
                d11 += d13 * d15;
                d12 += d14 * d15;
            }
        }
        double d16 = 0.001 * (d8 + d10) + 1.0E-6;
        double d17 = d8 + d16;
        double d18 = d10 + d16;
        double d19 = d17 * d18 - d9 * d9;
        if (d19 <= 1.0E-12) {
            return -1;
        }
        double d20 = (d18 * d11 - d9 * d12) / d19;
        double d21 = -d20;
        double d22 = (d17 * d12 - d9 * d11) / d19;
        double d23 = -d22;
        if (d21 * d21 + d23 * d23 < 1.0E-9) {
            return -1;
        }
        return RiverFlowField.a(d21, d23);
    }

    private static int a(double d, double d2) {
        int n = (int)Math.round(Math.atan2(d2, d) / 0.7853981633974483);
        return (n % 8 + 8) % 8;
    }

    public int spacing() {
        return this.j;
    }

    public int dirAt(int n, int n2) {
        long l = this.a(n, n2);
        if (l == 9221120239188049920L) {
            return -1;
        }
        return RiverFlowField.a(RiverFlowField.a(l), RiverFlowField.b(l));
    }

    public double flowRadAt(int n, int n2) {
        long l = this.a(n, n2);
        return l == 9221120239188049920L ? Double.NaN : Math.atan2(RiverFlowField.b(l), RiverFlowField.a(l));
    }

    private long a(int n, int n2) {
        double d = (double)(n - this.h) / (double)this.j;
        double d2 = (double)(n2 - this.i) / (double)this.j;
        int n3 = (int)Math.floor(d);
        int n4 = (int)Math.floor(d2);
        double d3 = 0.0;
        double d4 = 0.0;
        double d5 = 0.0;
        for (int i = -3; i <= 3; ++i) {
            for (int j = -3; j <= 3; ++j) {
                byte by;
                int n5 = n3 + j;
                int n6 = n4 + i;
                if (n5 < 0 || n6 < 0 || n5 >= this.k || n6 >= this.l || (by = this.m[n6 * this.k + n5]) < 0) continue;
                double d6 = (double)n5 - d;
                double d7 = (double)n6 - d2;
                double d8 = 1.0 / (1.0 + d6 * d6 + d7 * d7);
                d3 += f[by] * d8;
                d4 += g[by] * d8;
                d5 += d8;
            }
        }
        if (d5 <= 0.0) {
            return 9221120239188049920L;
        }
        return (long)Float.floatToRawIntBits((float)d3) << 32 | (long)Float.floatToRawIntBits((float)d4) & 0xFFFFFFFFL;
    }

    private static double a(long l) {
        return Float.intBitsToFloat((int)(l >>> 32));
    }

    private static double b(long l) {
        return Float.intBitsToFloat((int)l);
    }

    static {
        for (int i = 0; i < 8; ++i) {
            RiverFlowField.f[i] = Math.cos((double)i * 0.7853981633974483);
            RiverFlowField.g[i] = Math.sin((double)i * 0.7853981633974483);
        }
    }

    public static interface BiomeClass {
        public int classify(int var1, int var2);
    }

    public static interface OrientationField {
        public double sample(int var1, int var2);
    }
}

