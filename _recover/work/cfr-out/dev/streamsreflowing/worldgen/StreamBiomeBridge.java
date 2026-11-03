/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

public final class StreamBiomeBridge {
    public static volatile Biome ours;
    public static volatile MobSpawnSettings riverSpawns;
    public static volatile LandProbe landProbe;

    private StreamBiomeBridge() {
    }

    public static void set(Biome biome, MobSpawnSettings mobSpawnSettings) {
        if (ours != biome) {
            ours = biome;
            riverSpawns = mobSpawnSettings;
        }
    }

    public static void clear() {
        ours = null;
        riverSpawns = null;
        landProbe = null;
    }

    public static interface LandProbe {
        public Holder<Biome> at(int var1, int var2, int var3);
    }
}

