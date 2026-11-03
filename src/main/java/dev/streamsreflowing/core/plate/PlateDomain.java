/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.plate;

import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.plate.PlateLattice;

public final class PlateDomain {
    private final PlateLattice a;
    private final PlateKey b;
    public final double centerX;
    public final double centerZ;
    public final int minX;
    public final int minZ;
    public final int maxX;
    public final int maxZ;

    public PlateDomain(PlateLattice plateLattice, PlateKey plateKey) {
        this.a = plateLattice;
        this.b = plateKey;
        this.centerX = plateLattice.seedX(plateKey.cx(), plateKey.cz());
        this.centerZ = plateLattice.seedZ(plateKey.cx(), plateKey.cz());
        double d = plateLattice.scale();
        this.minX = (int)Math.floor(this.centerX - d);
        this.minZ = (int)Math.floor(this.centerZ - d);
        this.maxX = (int)Math.ceil(this.centerX + d);
        this.maxZ = (int)Math.ceil(this.centerZ + d);
    }

    public PlateKey key() {
        return this.b;
    }

    public boolean contains(double d, double d2) {
        return this.a.plateAt(d, d2).equals(this.b);
    }

    public double borderDistance(double d, double d2) {
        return this.a.borderDistance(d, d2);
    }
}

