/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.river;

public record RiverSegment(double x1, double z1, double y1, double w1, double x2, double z2, double y2, double w2) {
    public double clampParam(double d, double d2) {
        double d3 = this.x2 - this.x1;
        double d4 = this.z2 - this.z1;
        double d5 = d3 * d3 + d4 * d4;
        if (d5 <= 1.0E-9) {
            return 0.0;
        }
        double d6 = ((d - this.x1) * d3 + (d2 - this.z1) * d4) / d5;
        return d6 < 0.0 ? 0.0 : (d6 > 1.0 ? 1.0 : d6);
    }

    public double distanceAt(double d, double d2, double d3) {
        double d4 = this.x1 + (this.x2 - this.x1) * d3;
        double d5 = this.z1 + (this.z2 - this.z1) * d3;
        double d6 = d - d4;
        double d7 = d2 - d5;
        return Math.sqrt(d6 * d6 + d7 * d7);
    }

    public double gradeAt(double d) {
        return this.y1 + (this.y2 - this.y1) * d;
    }

    public double widthAt(double d) {
        return this.w1 + (this.w2 - this.w1) * d;
    }

    public double minX() {
        return Math.min(this.x1, this.x2);
    }

    public double maxX() {
        return Math.max(this.x1, this.x2);
    }

    public double minZ() {
        return Math.min(this.z1, this.z2);
    }

    public double maxZ() {
        return Math.max(this.z1, this.z2);
    }
}

