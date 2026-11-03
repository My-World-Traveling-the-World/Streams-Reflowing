/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.flow;

import dev.streamsreflowing.flow.FlowRemeshQueue;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public final class RTFFlowClientSweep {
    private static final int PERIOD_TICKS = 20;
    private static final int RADIUS_CHUNKS = 8;
    private static int tick;

    private RTFFlowClientSweep() {
    }

    public static void onClientTick(Level level, BlockPos playerPos) {
        if (level == null || playerPos == null || ++tick % 20 != 0) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD) || !RTFBridge.available()) {
            return;
        }
        int pcx = playerPos.getX() >> 4;
        int pcz = playerPos.getZ() >> 4;
        for (int dz = -8; dz <= 8; ++dz) {
            for (int dx = -8; dx <= 8; ++dx) {
                byte[] u;
                LevelChunk chunk;
                int cx = pcx + dx;
                int cz = pcz + dz;
                if (RTFFlowUnderlay.has(true, cx, cz) || (chunk = level.getChunkSource().getChunk(cx, cz, false)) == null || (u = RTFBridge.flowUnderlay(chunk)) == null) continue;
                RTFFlowUnderlay.put(true, cx, cz, u);
                FlowRemeshQueue.enqueue(cx, cz);
            }
        }
    }
}

