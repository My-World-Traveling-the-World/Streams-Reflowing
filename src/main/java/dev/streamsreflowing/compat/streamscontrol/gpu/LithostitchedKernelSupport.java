package dev.streamsreflowing.compat.streamscontrol.gpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

final class LithostitchedKernelSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private static final String TABLE_PLACEHOLDER = "/*__LITHOSTITCHED_FAST_NOISE_TABLE__*/";
    private static final String FNL_CLASS =
            "dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL";
    private static volatile float[] randomVectors;
    private static volatile boolean resolved;

    private LithostitchedKernelSupport() {}

    static String inject(String source) {
        float[] vectors = randomVectors();
        String declaration;
        if (vectors == null || vectors.length != 1024) {
            declaration = "static __constant float lithostitched_rand_vecs3d[1] = {0.0f};";
        } else {
            StringBuilder builder = new StringBuilder(18_000);
            builder.append("static __constant float lithostitched_rand_vecs3d[1024] = {");
            for (float vector : vectors) {
                builder.append(Float.toHexString(vector)).append('f').append(',');
            }
            builder.append("};");
            declaration = builder.toString();
        }
        if (!source.contains(TABLE_PLACEHOLDER)) {
            throw new IllegalStateException("Lithostitched FastNoise kernel placeholder is missing");
        }
        return source.replace(TABLE_PLACEHOLDER, declaration);
    }

    static boolean hasRandomVectors() {
        float[] vectors = randomVectors();
        return vectors != null && vectors.length == 1024;
    }

    private static float[] randomVectors() {
        if (resolved) {
            return randomVectors;
        }
        synchronized (LithostitchedKernelSupport.class) {
            if (resolved) {
                return randomVectors;
            }
            try {
                Class<?> fnl = Class.forName(FNL_CLASS, false,
                        LithostitchedKernelSupport.class.getClassLoader());
                Field field = fnl.getDeclaredField("RandVecs3D");
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof float[] vectors && vectors.length == 1024) {
                    randomVectors = vectors.clone();
                    LOGGER.info("Streams OpenCL loaded Lithostitched FastNoise vectors: count={}", vectors.length);
                } else {
                    LOGGER.info("Streams OpenCL Lithostitched FastNoise vectors have unexpected shape");
                }
            } catch (Throwable failure) {
                LOGGER.info("Streams OpenCL Lithostitched FastNoise vectors unavailable: {}",
                        failure.toString());
            } finally {
                resolved = true;
            }
            return randomVectors;
        }
    }
}
