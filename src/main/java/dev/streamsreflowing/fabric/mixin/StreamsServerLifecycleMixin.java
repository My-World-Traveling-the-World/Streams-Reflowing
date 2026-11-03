package dev.streamsreflowing.fabric.mixin;

import java.util.function.BooleanSupplier;
import dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// RoadWeaver uses the default mixin priority (1000) at this same lifecycle
// point. Run first so its initial planner cannot enter before river plates are
// fully prepared.
@Mixin(value = MinecraftServer.class, priority = 900)
public abstract class StreamsServerLifecycleMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams Safety");

    @Inject(
            method = "createLevels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;setInitialSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/storage/ServerLevelData;ZZ)V",
                    shift = At.Shift.BEFORE))
    private void plaincraft$prewarmStreamsBeforeInitialSpawn(ChunkProgressListener listener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        ServerLevel level = server.overworld();
        if (level == null) return;
        BlockPos spawnCandidate = level.getChunkSource().randomState().sampler().findSpawnPosition();
        LOGGER.info("Streams startup prewarm entering before initial spawn chunk generation at candidate ({},{}).",
                spawnCandidate.getX(), spawnCandidate.getZ());
        StreamsPrewarmService.beginStartup(level, spawnCandidate.getX(), spawnCandidate.getZ());
        StreamsPrewarmService.awaitStartup(level);
        // Rivers are fully built, and this is the earliest point at which that is true:
        // awaitStartup returns only once every required plate exists. It is therefore where
        // the Great Wall's prewarm may begin.
        //
        // It cannot be driven from RoadWeaver. RoadWeaver's own river step
        // (InitialGenManager.prewarmInitialRiverPlates) runs much later, after its structure
        // scan, so hooking that left the wall waiting on roads rather than following them;
        // a measured session showed the wall's prewarm never starting at all while road work
        // was already under way.
        plaincraft$notifyGreatWallRiversReady();
    }

    /**
     * Tells the Great Wall its prewarm may start, without depending on it.
     *
     * <p>Reflection because the wall is an optional mod: naming its class directly would make
     * this patch fail to load without it. Every failure is swallowed - no wall, a renamed
     * entry point, a wall that throws - so river generation is never affected by the wall's
     * presence or health. The notification is idempotent on the wall's side, so a second one
     * from RoadWeaver or from the wall's own fallback costs nothing.
     */
    private static void plaincraft$notifyGreatWallRiversReady() {
        try {
            Class<?> services = Class.forName("local.plaincraft.greatwall.lifecycle.WorldServices");
            services.getMethod("riversPrewarmed").invoke(null);
            LOGGER.info("Streams startup prewarm complete; Great Wall prewarm signalled.");
        } catch (ClassNotFoundException noWall) {
            // Expected whenever the wall is not installed.
        } catch (Throwable failure) {
            LOGGER.warn("Streams could not signal the Great Wall; river generation is unaffected", failure);
        }
    }

    @Inject(method = "prepareLevels", at = @At("HEAD"))
    private void plaincraft$beginStreamsStartupPrewarm(ChunkProgressListener listener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        ServerLevel level = server.overworld();
        if (level == null) return;
        LOGGER.info("Streams startup prewarm phase entered before RoadWeaver initial planning.");
        StreamsPrewarmService.beginStartup(level);
        StreamsPrewarmService.awaitStartup(level);
        // The createLevels hook that signals the wall only runs on a freshly created world
        // (setInitialSpawn is skipped when the world is already initialised). On a re-opened
        // world the rivers still finish here, so signal the wall again - idempotent on the
        // wall's side - rather than leaving an existing world without a wall prewarm.
        plaincraft$notifyGreatWallRiversReady();
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void plaincraft$tickStreamsRuntimePrewarm(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        StreamsPrewarmService.tick((MinecraftServer) (Object) this);
    }

    // 保存窗口避让:进入/退出 saveAllChunks(自动保存与手动保存都会走到这),
    // 让 runtime plate build 在写盘期间退让,避免边写边改放大脏 chunk 量。
    // 注意 saveAllChunks(boolean,boolean,boolean) 返回 boolean,故用
    // CallbackInfoReturnable<Boolean> 而不是 CallbackInfo。
    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    private void plaincraft$enterSaving(boolean suppressLogs, boolean flush, boolean forced,
            CallbackInfoReturnable<Boolean> cir) {
        StreamsPrewarmService.enterSaving();
    }

    @Inject(method = "saveAllChunks", at = @At("TAIL"))
    private void plaincraft$exitSaving(boolean suppressLogs, boolean flush, boolean forced,
            CallbackInfoReturnable<Boolean> cir) {
        StreamsPrewarmService.exitSaving();
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void plaincraft$stopStreamsPrewarm(CallbackInfo ci) {
        StreamsPrewarmService.stop((MinecraftServer) (Object) this);
    }
}
