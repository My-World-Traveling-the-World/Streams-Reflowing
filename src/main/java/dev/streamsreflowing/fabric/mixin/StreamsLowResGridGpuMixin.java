package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.core.terrain.TerrainSampler;
import dev.streamsreflowing.compat.streamscontrol.StreamsGpuTerrainSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.streamsreflowing.core.grid.LowResGrid", remap = false)
public abstract class StreamsLowResGridGpuMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Ldev/streamsreflowing/core/terrain/TerrainSampler;batchCapable()Z"),
            remap = false)
    private boolean plaincraft$constructorBatchCapable(TerrainSampler sampler) {
        return sampler.batchCapable() || StreamsGpuTerrainSampler.isAvailable(sampler);
    }

    @Redirect(
            method = "a(Ldev/streamsreflowing/core/plate/PlateDomain;Ldev/streamsreflowing/core/terrain/TerrainSampler;Ldev/streamsreflowing/core/config/StreamsConfig;I)V",
            at = @At(value = "INVOKE",
                    target = "Ldev/streamsreflowing/core/terrain/TerrainSampler;batchCapable()Z"),
            remap = false)
    private boolean plaincraft$coarseBatchCapable(TerrainSampler sampler) {
        return sampler.batchCapable() || StreamsGpuTerrainSampler.isAvailable(sampler);
    }

    @Redirect(
            method = "a(Ldev/streamsreflowing/core/plate/PlateDomain;Ldev/streamsreflowing/core/terrain/TerrainSampler;Ldev/streamsreflowing/core/config/StreamsConfig;)V",
            at = @At(value = "INVOKE",
                    target = "Ldev/streamsreflowing/core/terrain/TerrainSampler;heightsBatch([I[I[DI)V"),
            remap = false)
    private void plaincraft$exactBatch(TerrainSampler sampler, int[] xs, int[] zs,
                                       double[] output, int count) {
        if (!StreamsGpuTerrainSampler.sampleOceanFloor(sampler, xs, zs, output, count)) {
            sampler.heightsBatch(xs, zs, output, count);
        }
    }

    @Redirect(
            method = "a(Ldev/streamsreflowing/core/plate/PlateDomain;Ldev/streamsreflowing/core/terrain/TerrainSampler;Ldev/streamsreflowing/core/config/StreamsConfig;I)V",
            at = @At(value = "INVOKE",
                    target = "Ldev/streamsreflowing/core/terrain/TerrainSampler;heightsBatch([I[I[DI)V"),
            remap = false)
    private void plaincraft$coarseGridBatch(TerrainSampler sampler, int[] xs, int[] zs,
                                            double[] output, int count) {
        if (!StreamsGpuTerrainSampler.sampleOceanFloor(sampler, xs, zs, output, count)) {
            sampler.heightsBatch(xs, zs, output, count);
        }
    }
}
