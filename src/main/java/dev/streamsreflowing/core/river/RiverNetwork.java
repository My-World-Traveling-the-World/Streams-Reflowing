/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.river;

import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NearestRiverIndex;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import java.util.List;

public final class RiverNetwork {
    public static final int NO_LAKE = Integer.MIN_VALUE;
    private final PlateKey a;
    private final List<RiverSegment> b;
    private final NearestRiverIndex c;
    private final List<Lake> d;

    public RiverNetwork(PlateKey plateKey, List<RiverSegment> list, NearestRiverIndex nearestRiverIndex, List<Lake> list2) {
        this.a = plateKey;
        this.b = list;
        this.c = nearestRiverIndex;
        this.d = list2;
    }

    public PlateKey key() {
        return this.a;
    }

    public List<RiverSegment> segments() {
        return this.b;
    }

    public int segmentCount() {
        return this.b.size();
    }

    public List<Lake> lakes() {
        return this.d;
    }

    public RiverSample sample(double d, double d2, double d3) {
        return this.c.nearest(d, d2, d3);
    }

    public RiverSample sampleTiered(double d, double d2, double d3) {
        return this.c.nearestTiered(d, d2, d3);
    }

    public RiverSample sampleChannel(double x, double z, double radius) {
        return this.c.nearestChannel(x, z, radius);
    }

    public RiverSample sampleCoveringForeign(double d, double d2, double d3, int n) {
        return this.c.nearestCoveringForeign(d, d2, d3, n);
    }

    public boolean corridorTouchesBox(double d, double d2, double d3, double d4) {
        return this.c.anyCorridorTouchesBox(d, d2, d3, d4);
    }

    public boolean inAnyChannel(double d, double d2, double d3) {
        return this.c.inAnyChannel(d, d2, d3);
    }

    public double lowerNeighborGrade(double d, double d2, double d3, int n, double d4, double d5) {
        return this.c.lowerNeighborGrade(d, d2, d3, n, d4, d5);
    }

    public Lake lakeAt(int n, int n2) {
        for (Lake lake : this.d) {
            if (!lake.covers(n, n2)) continue;
            return lake;
        }
        return null;
    }

    public int lakeLevelAt(int n, int n2) {
        Lake lake = this.lakeAt(n, n2);
        return lake == null ? Integer.MIN_VALUE : lake.level();
    }

    public Lake lakeNear(int n, int n2, int n3) {
        Lake lake = null;
        for (Lake lake2 : this.d) {
            if (!lake2.coversWithin(n, n2, n3) || lake != null && lake2.level() >= lake.level()) continue;
            lake = lake2;
        }
        return lake;
    }
}

