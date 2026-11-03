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
import net.minecraft.core.particles.SimpleParticleType;

public class StreamFoamParticle
extends TextureSheetParticle {
    private float baseAlpha = 0.4f;

    protected StreamFoamParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.0f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        this.lifetime = 24 + this.random.nextInt(16);
        this.quadSize = 0.9f + this.random.nextFloat() * 0.6f;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.alpha = this.baseAlpha;
    }

    public StreamFoamParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float scale, boolean solid, SpriteSet sprites) {
        this(level, x, y, z, vx, vy, vz);
        this.pickSprite(sprites);
        this.quadSize *= scale;
        this.alpha = this.baseAlpha = solid ? 0.85f : 0.4f;
    }

    @Override
    public void tick() {
        super.tick();
        int fadeStart = this.lifetime * 2 / 3;
        if (this.age > fadeStart) {
            this.alpha = this.baseAlpha * Math.max(0.0f, 1.0f - (float)(this.age - fadeStart) / (float)(this.lifetime - fadeStart));
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
            StreamFoamParticle particle = new StreamFoamParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}

