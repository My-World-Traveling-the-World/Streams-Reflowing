/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;

public interface NetworkStore {
    public RiverNetwork load(PlateKey var1);

    public void save(PlateKey var1, RiverNetwork var2);
}

