/* 文件职责：河流优先洪泛和局部盆地共用原始类型最小堆。 */
package dev.streamsreflowing.core.river;

import java.util.Arrays;

/** 保持PriorityQueue的浮点比较与同权交换次序，迁移不能改变洪泛取点顺序。 */
final class DoubleMinHeap {
    private double[] keys = new double[16];
    private int[] vals = new int[16];
    private int size;

    boolean isEmpty() {
        return this.size == 0;
    }

    double key() {
        return this.keys[0];
    }

    int value() {
        return this.vals[0];
    }

    void add(double d, int n) {
        if (this.size == this.keys.length) {
            this.grow();
        }
        this.siftUp(this.size, d, n);
        ++this.size;
    }

    void pop() {
        int n = this.size - 1;
        this.size = n;
        if (n == 0) {
            return;
        }
        this.siftDown(0, this.keys[n], this.vals[n]);
    }

    private void grow() {
        int n = this.keys.length < 64 ? this.keys.length + 2 : this.keys.length + (this.keys.length >> 1);
        this.keys = Arrays.copyOf(this.keys, n);
        this.vals = Arrays.copyOf(this.vals, n);
    }

    private void siftUp(int n, double d, int n2) {
        while (n > 0) {
            int n3 = n - 1 >>> 1;
            double d2 = this.keys[n3];
            if (Double.compare(d, d2) >= 0) break;
            this.keys[n] = d2;
            this.vals[n] = this.vals[n3];
            n = n3;
        }
        this.keys[n] = d;
        this.vals[n] = n2;
    }

    private void siftDown(int n, double d, int n2) {
        int n3 = this.size >>> 1;
        while (n < n3) {
            int n4 = (n << 1) + 1;
            double d2 = this.keys[n4];
            int n5 = this.vals[n4];
            int n6 = n4 + 1;
            if (n6 < this.size && Double.compare(d2, this.keys[n6]) > 0) {
                d2 = this.keys[n6];
                n5 = this.vals[n6];
                n4 = n6;
            }
            if (Double.compare(d, d2) <= 0) break;
            this.keys[n] = d2;
            this.vals[n] = n5;
            n = n4;
        }
        this.keys[n] = d;
        this.vals[n] = n2;
    }
}
