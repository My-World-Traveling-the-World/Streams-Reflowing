/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.worldgen.ChunkStepDefer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ChunkStep.class})
public abstract class ChunkStepMixin {
    @Inject(method={"apply"}, at={@At(value="HEAD")}, cancellable=true)
    private void streamsreflowing$deferUntilStreamRegionsReady(WorldGenContext ctx, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        CompletableFuture<ChunkAccess> deferred = ChunkStepDefer.defer((ChunkStep)((Object)this), ctx, cache, chunk);
        if (deferred != null) {
            cir.setReturnValue(deferred);
        }
    }
}

