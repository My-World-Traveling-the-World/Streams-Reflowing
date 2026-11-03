/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class StreamParticles {
    public static final SimpleParticleType STREAM_BUBBLE = FabricParticleTypes.simple();
    public static final SimpleParticleType STREAM_SPLASH = FabricParticleTypes.simple();
    public static final SimpleParticleType STREAM_FOAM = FabricParticleTypes.simple();
    public static final SimpleParticleType STREAM_LEAF = FabricParticleTypes.simple();
    public static final SimpleParticleType STREAM_STICK = FabricParticleTypes.simple();

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_bubble"), STREAM_BUBBLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_splash"), STREAM_SPLASH);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_foam"), STREAM_FOAM);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_leaf"), STREAM_LEAF);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream_stick"), STREAM_STICK);
    }

    private StreamParticles() {
    }
}

