/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.command;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class WelcomedPlayers
extends SavedData {
    public static final String ID = "streamsreflowing_welcomed";
    private boolean welcomed;

    public static SavedData.Factory<WelcomedPlayers> factory() {
        return new SavedData.Factory<WelcomedPlayers>(WelcomedPlayers::new, WelcomedPlayers::load, null);
    }

    private WelcomedPlayers() {
    }

    private static WelcomedPlayers load(CompoundTag tag, HolderLookup.Provider registries) {
        WelcomedPlayers data = new WelcomedPlayers();
        data.welcomed = tag.getBoolean("welcomed");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("welcomed", this.welcomed);
        return tag;
    }

    public boolean markWelcomed() {
        if (!this.welcomed) {
            this.welcomed = true;
            this.setDirty();
            return true;
        }
        return false;
    }
}

