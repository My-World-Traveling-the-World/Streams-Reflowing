/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.client;

import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.FlowRemeshQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class ClientFlowHook {
    private ClientFlowHook() {
    }

    public static void store(int cx, int cz, byte[] grid) {
        if (ClientFlowGrids.put(cx, cz, grid)) {
            FlowRemeshQueue.enqueue(cx, cz);
        }
    }

    public static void drainTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lvl = mc.level;
        if (lvl == null) {
            FlowRemeshQueue.clear();
            return;
        }
        FlowRemeshQueue.drain((cx, cz) -> {
            if (!lvl.hasChunk(cx, cz)) {
                return;
            }
            LevelChunk chunk = lvl.getChunk(cx, cz);
            LevelChunkSection[] secs = chunk.getSections();
            for (int i = 0; i < secs.length; ++i) {
                if (secs[i].hasOnlyAir() || !secs[i].maybeHas(st -> !st.getFluidState().isEmpty())) continue;
                mc.levelRenderer.setSectionDirty(cx, (lvl.getMinBuildHeight() >> 4) + i, cz);
            }
        });
    }
}

