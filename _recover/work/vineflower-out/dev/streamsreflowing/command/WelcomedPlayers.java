package dev.streamsreflowing.command;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class WelcomedPlayers extends SavedData {
   public static final String ID = "streamsreflowing_welcomed";
   private boolean welcomed;

   public static Factory<WelcomedPlayers> factory() {
      return new Factory<>(WelcomedPlayers::new, WelcomedPlayers::load, null);
   }

   private WelcomedPlayers() {
   }

   private static WelcomedPlayers load(CompoundTag tag, Provider registries) {
      WelcomedPlayers data = new WelcomedPlayers();
      data.welcomed = tag.getBoolean("welcomed");
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag, Provider registries) {
      tag.putBoolean("welcomed", this.welcomed);
      return tag;
   }

   public boolean markWelcomed() {
      if (!this.welcomed) {
         this.welcomed = true;
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }
}
