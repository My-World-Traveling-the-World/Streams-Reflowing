/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.client;

import dev.streamsreflowing.client.StreamAudio;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.fabric.particle.StreamParticles;
import dev.streamsreflowing.fabric.particle.StreamSplashParticle;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class StreamParticleScanner {
    public static SpriteSet splashSprites;
    public static SpriteSet foamSprites;
    private static final int NEAR = 16;
    private static final int SCAN_FROM = 4;
    private static final int COLUMNS_PER_TICK = 160;
    private static final int SCAN_DEPTH = 16;
    private static final int MAX_FALL_SCAN = 16;
    private static final int REF_DIST = 16;
    private static final double SIZE_EXP = 0.65;
    private static final double NEAR_RATE = 1.5;
    private static final int MAX_SPAWN_PER_TICK = 1000;
    private static final int REVERIFY = 40;
    private static final int EXPIRE = 300;
    private static final RandomSource RAND;
    private static final BlockPos.MutableBlockPos COL;
    private static final List<int[]> gridOffsets;
    private static int gridRadius;
    private static int scanCursor;
    private static HashMap<Long, long[]> feet;
    private static int sweepRound;
    private static boolean foundThisSweep;
    private static Object lastLevel;
    private static final int SURFACE_SAMPLES = 667;
    private static long surfaceTick;
    private static int surfaceSpawned;
    private static Fluid customFlowing;
    private static int idleSweeps;
    private static int throttleTick;

    private StreamParticleScanner() {
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            return;
        }
        double ambience = StreamsConfigFabric.waterAmbience();
        boolean audio = ambience > 0.0;
        int mode = StreamsConfigFabric.streamBubbles();
        StreamParticleScanner.surfaceFlowEmit(level, player, mode, audio);
        StreamAudio.tick(level, player, feet, audio, ambience);
        if (mode == 0) {
            return;
        }
        if (splashSprites == null || foamSprites == null) {
            return;
        }
        int radius = StreamsConfigFabric.streamParticleScanRadius();
        if (radius <= 16) {
            if (!feet.isEmpty()) {
                feet.clear();
            }
            return;
        }
        if (level != lastLevel) {
            feet.clear();
            scanCursor = 0;
            sweepRound = 0;
            lastLevel = level;
        }
        if (radius != gridRadius) {
            StreamParticleScanner.buildGrid(radius);
        }
        int cx = Mth.floor(player.getX());
        int cz = Mth.floor(player.getZ());
        long now = level.getGameTime();
        boolean anyWater = StreamsConfigFabric.particlesOnVanillaWater();
        Fluid fluid = customFlowing = StreamsConfigFabric.bakedStreamFluid() ? StreamFluids.FLOWING : null;
        if (idleSweeps >= 2 && feet.isEmpty() && (++throttleTick & 7) != 0) {
            return;
        }
        if (!gridOffsets.isEmpty()) {
            int perTick = Math.min(160, gridOffsets.size());
            for (int n = 0; n < perTick; ++n) {
                int[] off = gridOffsets.get(scanCursor);
                int step = off[2];
                int cellX = Math.floorDiv(cx + off[0], step);
                int cellZ = Math.floorDiv(cz + off[1], step);
                int wx = cellX * step + Math.floorMod(StreamParticleScanner.cellHash(cellX, cellZ, -1640531527) + sweepRound, step);
                int wz = cellZ * step + Math.floorMod(StreamParticleScanner.cellHash(cellX, cellZ, -2048144789) + sweepRound / step, step);
                if (!anyWater && ClientFlowGrids.angleAt(wx, wz) == -1) continue;
                int[] foot = StreamParticleScanner.scanColumn(level, wx, wz);
                if (foot != null) {
                    idleSweeps = 0;
                    foundThisSweep = true;
                    feet.put(BlockPos.asLong(foot[0], foot[1], foot[2]), new long[]{foot[3], now, foot[4]});
                }
                if (++scanCursor < gridOffsets.size()) continue;
                scanCursor = 0;
                ++sweepRound;
                if (!foundThisSweep && feet.isEmpty()) {
                    idleSweeps = Math.min(idleSweeps + 1, 100);
                }
                foundThisSweep = false;
            }
        }
        double maxScale = Math.pow((double)radius / 16.0, 0.65);
        int spawned = 0;
        ArrayList<long[]> moved = null;
        Iterator<Map.Entry<Long, long[]>> it = feet.entrySet().iterator();
        while (it.hasNext() && spawned < 1000) {
            Map.Entry<Long, long[]> e = it.next();
            long[] val = e.getValue();
            if (now - val[1] > 300L) {
                it.remove();
                continue;
            }
            long key = e.getKey();
            int fx = BlockPos.getX(key);
            int fy = BlockPos.getY(key);
            int fz = BlockPos.getZ(key);
            double dx = (double)fx + 0.5 - player.getX();
            double dz = (double)fz + 0.5 - player.getZ();
            double d2 = dx * dx + dz * dz;
            double minD = (int)val[2] == 2 ? 4.0 : 16.0;
            double maxD = radius + 4;
            if (d2 < minD * minD || d2 > maxD * maxD) continue;
            double d = Math.sqrt(d2);
            double sizeKnob = StreamsConfigFabric.particleSize();
            double rateKnob = StreamsConfigFabric.particleRate();
            if (now - val[1] > 40L) {
                int[] again = StreamParticleScanner.scanColumn(level, fx, fz);
                if (again == null) {
                    it.remove();
                    continue;
                }
                if (again[1] != fy) {
                    it.remove();
                    if (moved == null) {
                        moved = new ArrayList<long[]>();
                    }
                    moved.add(new long[]{BlockPos.asLong(again[0], again[1], again[2]), again[3], now, again[4]});
                    continue;
                }
                val[0] = again[3];
                val[1] = now;
                val[2] = again[4];
            }
            double lod = Mth.clamp(Math.pow(d / 16.0, 0.65), 1.0, maxScale);
            double scale = lod * sizeKnob;
            double rate = Math.min(1.0, 1.5 / (lod * lod)) * rateKnob / (sizeKnob * sizeKnob);
            spawned += StreamParticleScanner.emit(mc, level, fx, fy, fz, (int)val[0], (float)scale, rate);
        }
        if (moved != null) {
            for (long[] m : moved) {
                feet.put(m[0], new long[]{m[1], m[2], m[3]});
            }
        }
    }

    private static void surfaceFlowEmit(ClientLevel level, LocalPlayer player, int mode, boolean audio) {
        boolean particles;
        long now = level.getGameTime();
        if (now != surfaceTick) {
            surfaceTick = now;
            surfaceSpawned = 0;
        }
        int cap = StreamsConfigFabric.maxStreamParticles();
        boolean bl = particles = mode != 0;
        if (!particles && !audio) {
            return;
        }
        boolean debris = StreamsConfigFabric.streamDebris();
        double drift = mode >= 2 ? 0.13 : 0.0;
        double pxd = player.getX();
        double pyd = player.getY();
        double pzd = player.getZ();
        int px0 = Mth.floor(pxd);
        int py0 = Mth.floor(pyd);
        int pz0 = Mth.floor(pzd);
        for (int i = 0; i < 667; ++i) {
            int x = px0 + RAND.nextInt(33) - 16;
            int y = py0 + RAND.nextInt(33) - 16;
            int z = pz0 + RAND.nextInt(33) - 16;
            byte angle = ClientFlowGrids.angleAt(x, z);
            if (angle == -1) continue;
            COL.set(x, y, z);
            if (!level.getFluidState(COL).is(FluidTags.WATER)) continue;
            COL.set(x, y + 1, z);
            if (!level.getBlockState(COL).isAir() || !particles || surfaceSpawned >= cap) continue;
            StreamParticleScanner.emitSurfaceAt(level, x, y, z, angle, drift, debris);
        }
    }

    private static void emitSurfaceAt(ClientLevel level, int x, int y, int z, byte angle, double drift, boolean debris) {
        double fx = StreamFlowGrid.vx(angle);
        double fz = StreamFlowGrid.vz(angle);
        double px = (double)x + RAND.nextDouble();
        double pz = (double)z + RAND.nextDouble();
        double density = StreamsConfigFabric.particleRate();
        if (RAND.nextDouble() < density / 24.0) {
            int clump = 2 + RAND.nextInt(3);
            for (int i = 0; i < clump; ++i) {
                StreamParticleScanner.surfaceSpawn(level, StreamParticles.STREAM_BUBBLE, px + (RAND.nextDouble() - 0.5) * 0.4, (double)y + 0.8, pz + (RAND.nextDouble() - 0.5) * 0.4, fx * drift, 0.0, fz * drift);
            }
        }
        if (debris && RAND.nextDouble() < density / 120.0) {
            StreamParticleScanner.surfaceSpawn(level, StreamParticles.STREAM_LEAF, px, (double)y + 0.9, pz, fx * drift, 0.0, fz * drift);
        }
        if (debris && RAND.nextDouble() < density / 2000.0) {
            StreamParticleScanner.surfaceSpawn(level, StreamParticles.STREAM_STICK, px, (double)y + 0.9, pz, fx * drift, 0.0, fz * drift);
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int nx = x + d.getStepX();
            int nz = z + d.getStepZ();
            byte na = ClientFlowGrids.angleAt(nx, nz);
            COL.set(nx, y, nz);
            boolean sameDir = na != -1 && level.getFluidState(COL).is(FluidTags.WATER) && StreamParticleScanner.angleClose(na, angle);
            if (sameDir || !(RAND.nextDouble() < density / 8.0)) continue;
            double bx = px + (double)d.getStepX() * 0.5;
            double bz = pz + (double)d.getStepZ() * 0.5;
            int flecks = 1 + RAND.nextInt(2);
            for (int i = 0; i < flecks; ++i) {
                double ang = RAND.nextDouble() * (Math.PI * 2);
                double spd = 0.05 * (0.5 + RAND.nextDouble());
                StreamParticleScanner.surfaceSpawn(level, StreamParticles.STREAM_SPLASH, bx + (RAND.nextDouble() - 0.5) * 0.25, (double)y + 0.82, bz + (RAND.nextDouble() - 0.5) * 0.25, Math.cos(ang) * spd, 0.06 + RAND.nextDouble() * 0.08, Math.sin(ang) * spd);
            }
        }
        int aox = (int)Math.round(fx);
        int aoz = (int)Math.round(fz);
        if (aox != 0 || aoz != 0) {
            boolean streamAhead;
            COL.set(x + aox, y, z + aoz);
            boolean bl = streamAhead = ClientFlowGrids.angleAt(x + aox, z + aoz) != -1 && level.getFluidState(COL).is(FluidTags.WATER);
            if (!streamAhead && RAND.nextDouble() < density / 7.0) {
                StreamParticleScanner.surfaceSpawn(level, StreamParticles.STREAM_SPLASH, px + (double)aox * 0.4, (double)y + 0.85, pz + (double)aoz * 0.4, fx * 0.12, 0.05 + RAND.nextDouble() * 0.07, fz * 0.12);
            }
        }
    }

    private static boolean angleClose(byte a, byte b) {
        int diff = Math.abs((a & 0xFF) - (b & 0xFF));
        if (diff > 128) {
            diff = 256 - diff;
        }
        return diff <= 32;
    }

    private static void surfaceSpawn(ClientLevel level, ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
        ++surfaceSpawned;
        level.addParticle(type, x, y, z, vx, vy, vz);
    }

    private static void buildGrid(int radius) {
        gridOffsets.clear();
        int band = 12;
        for (int dMin = 4; dMin < radius; dMin += 12) {
            int dMax = Math.min(dMin + 12, radius);
            double dMid = (double)(dMin + dMax) * 0.5;
            int step = dMid < 48.0 ? 2 : (dMid < 96.0 ? 3 : (dMid < 160.0 ? 5 : (dMid < 320.0 ? 8 : 12)));
            int dMinSq = dMin * dMin;
            int dMaxSq = dMax * dMax;
            for (int ox = -dMax; ox <= dMax; ox += step) {
                for (int oz = -dMax; oz <= dMax; oz += step) {
                    int dsq = ox * ox + oz * oz;
                    if (dsq < dMinSq || dsq >= dMaxSq) continue;
                    gridOffsets.add(new int[]{ox, oz, step});
                }
            }
        }
        gridRadius = radius;
        scanCursor = 0;
    }

    private static int cellHash(int x, int z, int salt) {
        int h = x * -1640531535 ^ z * -2048144777 ^ salt;
        h ^= h >>> 15;
        h *= 739982445;
        h ^= h >>> 12;
        return h & Integer.MAX_VALUE;
    }

    private static boolean streamsreflowing$openToAir(ClientLevel level, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        return level.getBlockState(p.set(x + 1, y, z)).isAir() || level.getBlockState(p.set(x - 1, y, z)).isAir() || level.getBlockState(p.set(x, y, z + 1)).isAir() || level.getBlockState(p.set(x, y, z - 1)).isAir();
    }

    private static int[] scanColumn(ClientLevel level, int x, int z) {
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int bottomY = topY - 16;
        for (int y = topY; y >= bottomY && y > -64; --y) {
            int dy;
            int aboveKind = StreamParticleScanner.fluidKind(level, x, y + 1, z);
            if (aboveKind == 0 || StreamParticleScanner.fluidKind(level, x, y - 1, z) != 0) continue;
            boolean thisFalling = StreamParticleScanner.fluidKind(level, x, y, z) != 0;
            int h = 0;
            int n = dy = thisFalling ? 0 : 1;
            while (dy <= 16 && StreamParticleScanner.fluidKind(level, x, y + dy, z) != 0) {
                ++h;
                ++dy;
            }
            if (h < 2 || !StreamParticleScanner.streamsreflowing$openToAir(level, x, y + 1, z)) continue;
            return new int[]{x, y, z, h, aboveKind};
        }
        return null;
    }

    private static int fluidKind(ClientLevel level, int x, int y, int z) {
        COL.set(x, y, z);
        FluidState fs = level.getFluidState(COL);
        if (fs.isEmpty() || !fs.hasProperty(FlowingFluid.FALLING) || !fs.getValue(FlowingFluid.FALLING).booleanValue()) {
            return 0;
        }
        if (customFlowing != null && fs.getType() == customFlowing) {
            return 1;
        }
        return fs.getType() == Fluids.FLOWING_WATER ? 2 : 0;
    }

    private static int emit(Minecraft mc, ClientLevel level, int fx, int fy, int fz, int h, float scale, double rate) {
        double oz;
        double baseX = (double)fx + 0.5;
        double baseZ = (double)fz + 0.5;
        int count = 0;
        int spray = Mth.clamp(4 + h * 2, 4, 16);
        double want = (double)spray * rate;
        int sprayNow = (int)want;
        if (RAND.nextDouble() < want - (double)sprayNow) {
            ++sprayNow;
        }
        double power = 0.18 + (double)h * 0.04;
        for (int i = 0; i < sprayNow; ++i) {
            double ang = RAND.nextDouble() * (Math.PI * 2);
            double spd = power * (0.4 + RAND.nextDouble() * 0.8);
            double ox = Math.cos(ang);
            double oz2 = Math.sin(ang);
            mc.particleEngine.add(new StreamSplashParticle(level, baseX + ox * 0.25, (double)fy + 0.6 + RAND.nextDouble() * 0.3, baseZ + oz2 * 0.25, ox * spd, 0.06 + RAND.nextDouble() * 0.08, oz2 * spd, scale, splashSprites));
            ++count;
        }
        if (RAND.nextDouble() < (double)Math.min(h, 3) / 5.0 * rate) {
            double ox = (RAND.nextDouble() - 0.5) * 0.6;
            oz = (RAND.nextDouble() - 0.5) * 0.6;
            mc.particleEngine.add(new StreamSplashParticle(level, baseX + ox, (double)fy + 0.9 + RAND.nextDouble() * 0.2, baseZ + oz, ox * 0.04, 0.03 + RAND.nextDouble() * 0.03, oz * 0.04, scale, true, splashSprites));
            ++count;
        }
        if (h >= 2 && (double)scale < 1.3 && RAND.nextDouble() < (double)Math.min(h, 9) / 9.0 * rate) {
            double ox = (RAND.nextDouble() - 0.5) * 0.6;
            oz = (RAND.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.CLOUD, baseX + ox, (double)fy + 0.9 + RAND.nextDouble() * 0.2, baseZ + oz, ox * 0.15, 0.04 + RAND.nextDouble() * 0.06, oz * 0.15);
            ++count;
        }
        return count;
    }

    static {
        RAND = RandomSource.create();
        COL = new BlockPos.MutableBlockPos();
        gridOffsets = new ArrayList<int[]>();
        gridRadius = -1;
        scanCursor = 0;
        feet = new HashMap();
        sweepRound = 0;
        foundThisSweep = false;
        lastLevel = null;
        surfaceTick = -1L;
        surfaceSpawned = 0;
    }
}

