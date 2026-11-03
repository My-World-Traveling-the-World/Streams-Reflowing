package dev.streamsreflowing.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

public final class StreamBiomeBridge {
   public static volatile Biome ours;
   public static volatile MobSpawnSettings riverSpawns;
   public static volatile StreamBiomeBridge.LandProbe landProbe;

   private StreamBiomeBridge() {
   }

   public static void set(Biome var0, MobSpawnSettings var1) {
      if (ours != var0) {
         ours = var0;
         riverSpawns = var1;
      }
   }

   public static void clear() {
      ours = null;
      riverSpawns = null;
      landProbe = null;
   }

   public interface LandProbe {
      Holder<Biome> at(int var1, int var2, int var3);
   }
}
