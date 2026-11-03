/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.compat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ParticularCompat {
    private static final ResourceLocation CASCADE_ID = ResourceLocation.fromNamespaceAndPath("particular", "cascade");
    private static boolean resolved = false;
    private static ParticleOptions cascade = null;

    private ParticularCompat() {
    }

    public static ParticleOptions cascade() {
        if (!resolved) {
            ParticleOptions particle;
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(CASCADE_ID);
            cascade = type instanceof ParticleOptions ? (particle = (ParticleOptions)((Object)type)) : null;
            resolved = true;
        }
        return cascade;
    }
}

