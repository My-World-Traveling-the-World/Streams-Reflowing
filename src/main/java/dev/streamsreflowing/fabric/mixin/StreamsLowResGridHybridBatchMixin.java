package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.terrain.TerrainSampler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import dev.streamsreflowing.compat.streamscontrol.StreamsGpuTerrainSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Batches the two exact-height passes used by Streams' hybrid low-resolution grid. */
@Mixin(targets = "dev.streamsreflowing.core.grid.LowResGrid", remap = false)
public abstract class StreamsLowResGridHybridBatchMixin {
    private static final Logger PLAINCRAFT_LOGGER =
            LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final AtomicBoolean PLAINCRAFT_LOGGED = new AtomicBoolean();

    @Inject(
            method = "a(Ldev/streamsreflowing/core/terrain/TerrainSampler;"
                    + "Ldev/streamsreflowing/core/config/StreamsConfig;IIIII[D[Z)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void plaincraft$batchHybridExactPasses(
            TerrainSampler sampler,
            StreamsConfig config,
            int sampleSpacing,
            int sampleOriginX,
            int sampleOriginZ,
            int sampleWidth,
            int sampleHeight,
            double[] exactHeights,
            boolean[] required,
            CallbackInfo ci) {
        if (!StreamsGpuTerrainSampler.isAvailable(sampler)) {
            return;
        }

        int gridOriginX = Math.floorDiv(sampleOriginX, sampleSpacing);
        int gridOriginZ = Math.floorDiv(sampleOriginZ, sampleSpacing);
        int size = Math.multiplyExact(sampleWidth, sampleHeight);
        boolean[] anchors = new boolean[size];

        for (int z = 0; z < sampleHeight; z++) {
            boolean evenZ = ((gridOriginZ + z) & 1) == 0;
            for (int x = 0; x < sampleWidth; x++) {
                int index = x + z * sampleWidth;
                if (!required[index]) {
                    continue;
                }
                if (evenZ && ((gridOriginX + x) & 1) == 0) {
                    anchors[index] = true;
                    continue;
                }
                int anchorX = ((gridOriginX + x) & 1) == 0 ? x : x - 1;
                int anchorZ = evenZ ? z : z - 1;
                for (int dz = 0; dz <= 2; dz += 2) {
                    for (int dx = 0; dx <= 2; dx += 2) {
                        int targetX = anchorX + dx;
                        int targetZ = anchorZ + dz;
                        if (targetX >= 0 && targetX < sampleWidth
                                && targetZ >= 0 && targetZ < sampleHeight) {
                            anchors[targetX + targetZ * sampleWidth] = true;
                        }
                    }
                }
            }
        }

        batchExact(sampler, sampleSpacing, sampleOriginX, sampleOriginZ,
                sampleWidth, sampleHeight, anchors, exactHeights);

        double[] estimates = new double[size];
        LowResGrid.parallelFor(sampleHeight, z -> {
            int worldZ = sampleOriginZ + z * sampleSpacing;
            for (int x = 0; x < sampleWidth; x++) {
                int index = x + z * sampleWidth;
                if (required[index] || anchors[index]) {
                    estimates[index] = sampler.estimate(
                            sampleOriginX + x * sampleSpacing, worldZ);
                }
            }
        });

        boolean[] fallback = new boolean[size];
        double seaGuard = config.seaLevel() + 8.0D;
        LowResGrid.parallelFor(sampleHeight, z -> {
            boolean evenZ = ((gridOriginZ + z) & 1) == 0;
            for (int x = 0; x < sampleWidth; x++) {
                int index = x + z * sampleWidth;
                if (!required[index] || anchors[index]) {
                    continue;
                }
                int anchorX = ((gridOriginX + x) & 1) == 0 ? x : x - 1;
                int anchorZ = evenZ ? z : z - 1;
                if (needsExact(anchorX, anchorZ, x, z, sampleWidth, sampleHeight,
                        exactHeights, estimates, seaGuard)) {
                    fallback[index] = true;
                    continue;
                }
                exactHeights[index] = interpolatedHeight(
                        anchorX, anchorZ, x, z, sampleWidth, exactHeights, estimates);
            }
        });

        int fallbackCount = batchExact(sampler, sampleSpacing, sampleOriginX, sampleOriginZ,
                sampleWidth, sampleHeight, fallback, exactHeights);
        if (PLAINCRAFT_LOGGED.compareAndSet(false, true)) {
            PLAINCRAFT_LOGGER.info(
                    "Streams hybrid LowResGrid now batches exact anchors and fallback columns; "
                            + "first fallback count={}.", fallbackCount);
        }
        ci.cancel();
    }

    private static int batchExact(
            TerrainSampler sampler,
            int spacing,
            int originX,
            int originZ,
            int width,
            int height,
            boolean[] selected,
            double[] output) {
        AtomicInteger count = new AtomicInteger();
        for (boolean value : selected) {
            if (value) {
                count.incrementAndGet();
            }
        }
        int selectedCount = count.get();
        if (selectedCount == 0) {
            return 0;
        }

        int[] xs = new int[selectedCount];
        int[] zs = new int[selectedCount];
        int[] indexes = new int[selectedCount];
        int write = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int index = x + z * width;
                if (!selected[index]) {
                    continue;
                }
                xs[write] = originX + x * spacing;
                zs[write] = originZ + z * spacing;
                indexes[write] = index;
                write++;
            }
        }

        double[] values = new double[selectedCount];
        if (!StreamsGpuTerrainSampler.sampleOceanFloor(sampler, xs, zs, values, selectedCount)) {
            sampler.heightsBatch(xs, zs, values, selectedCount);
        }
        for (int index = 0; index < selectedCount; index++) {
            output[indexes[index]] = values[index];
        }
        return selectedCount;
    }

    private static boolean needsExact(
            int anchorX,
            int anchorZ,
            int x,
            int z,
            int width,
            int height,
            double[] exact,
            double[] estimates,
            double seaGuard) {
        if (anchorX < 0 || anchorZ < 0 || anchorX + 2 >= width || anchorZ + 2 >= height) {
            return true;
        }
        int topLeft = anchorX + anchorZ * width;
        int topRight = topLeft + 2;
        int bottomLeft = topLeft + 2 * width;
        int bottomRight = bottomLeft + 2;
        double h00 = exact[topLeft];
        double h10 = exact[topRight];
        double h01 = exact[bottomLeft];
        double h11 = exact[bottomRight];
        if (h00 == 0.0D || h10 == 0.0D || h01 == 0.0D || h11 == 0.0D) {
            return true;
        }

        double e00 = estimates[topLeft];
        double e10 = estimates[topRight];
        double e01 = estimates[bottomLeft];
        double e11 = estimates[bottomRight];
        double targetEstimate = estimates[x + z * width];
        double minimum = Math.min(Math.min(e00, e10), Math.min(e01, e11));
        double maximum = Math.max(Math.max(e00, e10), Math.max(e01, e11));
        if (targetEstimate > maximum + 1.0D || targetEstimate < minimum - 1.0D) {
            return true;
        }

        double fx = (x - anchorX) / 2.0D;
        double fz = (z - anchorZ) / 2.0D;
        double base = bilerp(h00, h10, h01, h11, fx, fz);
        double adjusted = base + targetEstimate - bilerp(e00, e10, e01, e11, fx, fz);
        return adjusted <= seaGuard || base <= seaGuard;
    }

    private static double interpolatedHeight(
            int anchorX,
            int anchorZ,
            int x,
            int z,
            int width,
            double[] exact,
            double[] estimates) {
        int topLeft = anchorX + anchorZ * width;
        int topRight = topLeft + 2;
        int bottomLeft = topLeft + 2 * width;
        int bottomRight = bottomLeft + 2;
        double fx = (x - anchorX) / 2.0D;
        double fz = (z - anchorZ) / 2.0D;
        double base = bilerp(exact[topLeft], exact[topRight], exact[bottomLeft], exact[bottomRight], fx, fz);
        double estimate = bilerp(estimates[topLeft], estimates[topRight],
                estimates[bottomLeft], estimates[bottomRight], fx, fz);
        return base + estimates[x + z * width] - estimate;
    }

    private static double bilerp(
            double topLeft,
            double topRight,
            double bottomLeft,
            double bottomRight,
            double fx,
            double fz) {
        double top = topLeft + (topRight - topLeft) * fx;
        double bottom = bottomLeft + (bottomRight - bottomLeft) * fx;
        return top + (bottom - top) * fz;
    }
}
