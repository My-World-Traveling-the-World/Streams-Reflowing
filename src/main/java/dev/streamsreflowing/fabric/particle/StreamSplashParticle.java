/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public class StreamSplashParticle
extends TextureSheetParticle {
    protected StreamSplashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.25f;
        this.friction = 0.9f;
        this.hasPhysics = false;
        this.lifetime = 12 + this.random.nextInt(12);
        this.quadSize = 0.45f + this.random.nextFloat() * 0.35f;
        int wc = BiomeColors.getAverageWaterColor(level, BlockPos.containing(x, y, z));
        this.rCol = 0.82f + 0.18f * ((float)(wc >> 16 & 0xFF) / 255.0f);
        this.gCol = 0.82f + 0.18f * ((float)(wc >> 8 & 0xFF) / 255.0f);
        this.bCol = 0.82f + 0.18f * ((float)(wc & 0xFF) / 255.0f);
        this.alpha = 0.92f;
    }

    public StreamSplashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float scale, SpriteSet sprites) {
        this(level, x, y, z, vx, vy, vz);
        this.pickSprite(sprites);
        this.quadSize *= scale;
    }

    public StreamSplashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float scale, boolean poof, SpriteSet sprites) {
        this(level, x, y, z, vx, vy, vz, scale, sprites);
        if (poof) {
            this.gravity = 0.03f;
            this.friction = 0.96f;
            this.lifetime = 28 + this.random.nextInt(20);
            this.quadSize *= 1.7f;
            this.rCol = 1.0f;
            this.gCol = 1.0f;
            this.bCol = 1.0f;
        }
    }

    @Override
    public void tick() {
        super.tick();
        int fadeStart = this.lifetime * 2 / 3;
        if (this.age > fadeStart) {
            this.alpha = 0.92f * Math.max(0.0f, 1.0f - (float)(this.age - fadeStart) / (float)(this.lifetime - fadeStart));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            StreamSplashParticle particle = new StreamSplashParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}

