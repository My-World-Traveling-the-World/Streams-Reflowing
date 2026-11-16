package dev.streamsreflowing.fabric.mixin;

import java.util.function.BooleanSupplier;
import dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmService;
import net.shiroha233.roadweaver.api.outgoing.RiverReadinessBroadcast;
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

// 保留既有优先级；依赖顺序由就绪契约确认，不能仅靠源码优先级推断最终注入次序。
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
        // 由河流自己的结果门决定是否发布，异常返回不再等价于预热完成。
        StreamsPrewarmService.publishStartupReadiness(level);
    }

    @Inject(method = "prepareLevels", at = @At("HEAD"))
    private void plaincraft$beginStreamsStartupPrewarm(ChunkProgressListener listener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        ServerLevel level = server.overworld();
        if (level == null) return;
        LOGGER.info("Streams startup prewarm phase entered before RoadWeaver initial planning.");
        StreamsPrewarmService.beginStartup(level);
        // 重开同样走结果门；重复入口不重启失败批次，也不绕过失败判定。
        StreamsPrewarmService.publishStartupReadiness(level);
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
        // Clear the "rivers ready" flag so the next save starts clean. Subscribers stay
        // registered - they belong to the mod lifecycle, not the save lifecycle.
        RiverReadinessBroadcast.reset();
    }
}
