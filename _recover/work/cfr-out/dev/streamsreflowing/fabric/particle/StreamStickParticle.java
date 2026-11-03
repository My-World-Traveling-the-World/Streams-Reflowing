/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.particle;

import dev.streamsreflowing.fabric.StreamsConfigFabric;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;

public class StreamStickParticle
extends TextureSheetParticle {
    private static final double FALL_ACCEL = 0.045;
    private final boolean pathed;
    private final BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

    protected StreamStickParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z);
        boolean bl = this.pathed = StreamsConfigFabric.streamBubbles() >= 2;
        if (this.pathed) {
            this.xd = vx;
            this.zd = vz;
        } else {
            this.xd = 0.0;
            this.zd = 0.0;
        }
        this.yd = 0.0;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.lifetime = 80 + this.random.nextInt(80);
        this.quadSize = 0.26f + this.random.nextFloat() * 0.12f;
    }

    @Override
    public void tick() {
        int fadeStart;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        if (this.pathed) {
            double ydelta;
            double surfaceY = this.waterSurfaceNear(this.x, this.y, this.z);
            if (Double.isNaN(surfaceY) || this.y > surfaceY + 0.08) {
                this.yd -= 0.045;
                ydelta = this.yd;
            } else {
                ydelta = (surfaceY + 0.12 - this.y) * 0.05;
                this.yd = 0.0;
            }
            this.move(this.xd, ydelta, this.zd);
            this.xd *= 0.99;
            this.zd *= 0.99;
        }
        if (this.age > (fadeStart = this.lifetime * 2 / 3)) {
            this.alpha = Math.max(0.0f, 1.0f - (float)(this.age - fadeStart) / (float)(this.lifetime - fadeStart));
        }
    }

    private double waterSurfaceNear(double x, double y, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int topY = Mth.floor(y + 0.1);
        for (int dy = 0; dy <= 3; ++dy) {
            int by = topY - dy;
            this.scan.set(bx, by, bz);
            FluidState fs = this.level.getFluidState(this.scan);
            if (fs.isEmpty()) continue;
            return (float)by + fs.getHeight(this.level, this.scan);
        }
        return Double.NaN;
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return (quaternion, camera, partialTick) -> quaternion.rotationX(-1.4807963f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            StreamStickParticle particle = new StreamStickParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}

