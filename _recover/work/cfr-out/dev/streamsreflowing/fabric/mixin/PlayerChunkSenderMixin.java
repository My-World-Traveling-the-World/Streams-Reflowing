/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.fabric.net.StreamFlowNet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={PlayerChunkSender.class})
public abstract class PlayerChunkSenderMixin {
    @Inject(method={"sendChunk"}, at={@At(value="TAIL")})
    private static void streamsreflowing$sendFlowWithChunk(ServerGamePacketListenerImpl listener, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        StreamFlowNet.onChunkSentToPlayer(listener.player, level, chunk.getPos().x, chunk.getPos().z);
    }
}

