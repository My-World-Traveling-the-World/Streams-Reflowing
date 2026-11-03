/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.math.Hashing;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class StreamTint {
    public static final ResourceKey<Biome> STREAM_BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream"));
    private static final int GRID = 8;
    private static final int MAX_RINGS = 6;
    private static final int COARSE_RINGS = 24;
    private static final int MAX_ENTRIES = 32768;
    private static final ConcurrentHashMap<Long, Object> CACHE = new ConcurrentHashMap();
    private static final Object NO_LAND = new Object();
    private static final Object INCOMPLETE = new Object();
    private static volatile Biome oursInstance;
    private static volatile Object oursLevel;
    private static final ConcurrentHashMap<Long, Biome> NODES;
    private static final int NODE_MAX_ENTRIES = 262144;
    private static volatile Object nodesLevel;
    private static final Set<Long> RETRY;
    private static int retryTicks;

    private static long cellKey(int gx, int gz) {
        return Hashing.mix((long)gx << 32 ^ (long)gz & 0xFFFFFFFFL);
    }

    private StreamTint() {
    }

    public static void clear() {
        CACHE.clear();
        NODES.clear();
        RETRY.clear();
        oursInstance = null;
        oursLevel = null;
    }

    public static ColorResolver wrap(ColorResolver delegate, Kind kind) {
        if (delegate == null) {
            return null;
        }
        return (biome, x, z) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || biome != StreamTint.ourBiome(level)) {
                return delegate.getColor(biome, x, z);
            }
            int xi = (int)Math.floor(x);
            int zi = (int)Math.floor(z);
            int gx = Math.floorDiv(xi, 8);
            int gz = Math.floorDiv(zi, 8);
            double fx = (x - ((double)(gx * 8) + 4.0)) / 8.0;
            double fz = (z - ((double)(gz * 8) + 4.0)) / 8.0;
            int x0 = fx < 0.0 ? gx - 1 : gx;
            int z0 = fz < 0.0 ? gz - 1 : gz;
            double tx = fx < 0.0 ? fx + 1.0 : fx;
            double tz = fz < 0.0 ? fz + 1.0 : fz;
            int c00 = StreamTint.cellColor(level, biome, kind, x0, z0, x, z);
            int c10 = StreamTint.cellColor(level, biome, kind, x0 + 1, z0, x, z);
            int c01 = StreamTint.cellColor(level, biome, kind, x0, z0 + 1, x, z);
            int c11 = StreamTint.cellColor(level, biome, kind, x0 + 1, z0 + 1, x, z);
            if (c00 == c10 && c00 == c01 && c00 == c11) {
                return c00;
            }
            return StreamTint.lerp2(c00, c10, c01, c11, tx, tz);
        };
    }

    public static void debugMap() {
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            return;
        }
        int pgx = Math.floorDiv((int)player.getX(), 8);
        int pgz = Math.floorDiv((int)player.getZ(), 8);
        Biome ours = StreamTint.ourBiome(level);
        Constants.LOG.info("[SR-TINTCLIENT] around cell ({},{}) oursRegistered={} cache={} retryBacklog={}", pgx, pgz, ours != null, CACHE.size(), RETRY.size());
        StringBuilder samples = new StringBuilder();
        LinkedHashMap<Biome, Character> letters = new LinkedHashMap<Biome, Character>();
        for (int gz = pgz - 3; gz <= pgz + 3; ++gz) {
            StringBuilder row = new StringBuilder();
            for (int gx = pgx - 3; gx <= pgx + 3; ++gx) {
                char c;
                long key = StreamTint.cellKey(gx, gz);
                Object hit = CACHE.get(key);
                if (hit == null) {
                    c = '.';
                } else if (hit == NO_LAND) {
                    c = 'N';
                } else {
                    Biome borrowed = (Biome)hit;
                    Character known = (Character)letters.get(borrowed);
                    if (known == null) {
                        known = Character.valueOf((char)(97 + letters.size() % 26));
                        letters.put(borrowed, known);
                    }
                    c = known.charValue();
                }
                if (c == '.' || c == 'N') {
                    boolean cellIsOurs;
                    int cx = gx * 8 + 4;
                    int cz = gz * 8 + 4;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz) - 1;
                    boolean bl = cellIsOurs = level.getBiome(new BlockPos(cx, y, cz)).value() == ours;
                    if (cellIsOurs) {
                        boolean[] inc = new boolean[]{false};
                        Biome found = StreamTint.nodeSpiral(gx, gz, inc);
                        char c2 = found != null ? (c == 'N' ? (char)'R' : 'f') : (c = inc[0] ? (char)'i' : 'N');
                        if (samples.length() < 700) {
                            samples.append(String.format(" (%d,%d %c y=%d found=%s inc=%b)", cx, cz, Character.valueOf(c), y, found == null ? "-" : Integer.valueOf(found.toString().hashCode() % 1000), inc[0]));
                        }
                    }
                }
                row.append(c);
            }
            Constants.LOG.info("[SR-TINTCLIENT]   {}", (Object)row);
        }
        Registry reg = level.registryAccess().registry(Registries.BIOME).orElse(null);
        for (Map.Entry e : letters.entrySet()) {
            String name = reg != null ? String.valueOf(reg.getKey((Biome)e.getKey())) : "?";
            Constants.LOG.info("[SR-TINTCLIENT] legend {} = {} water=#{} grass=#{}", e.getValue(), name, Integer.toHexString(((Biome)e.getKey()).getWaterColor()), Integer.toHexString(((Biome)e.getKey()).getGrassColor(0.0, 0.0)));
        }
        if (ours != null) {
            Constants.LOG.info("[SR-TINTCLIENT] ours registered water=#{} (the fallback blue)", (Object)Integer.toHexString(ours.getWaterColor()));
        }
        Constants.LOG.info("[SR-TINTCLIENT] other: N=cached-noland(FALLBACK BLUE) R=stale-noland-would-fix f=uncached-would-find i=incomplete(unloaded) .=dry/never-queried; samples:{}", (Object)samples);
    }

    private static int cellColor(ClientLevel level, Biome self, Kind kind, int gx, int gz, double x, double z) {
        Biome land = StreamTint.landBiomeAt(level, gx * 8 + 4, gz * 8 + 4);
        Biome src = land != null ? land : self;
        return switch (kind.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> src.getWaterColor();
            case 1 -> src.getGrassColor(x, z);
            case 2 -> src.getFoliageColor();
        };
    }

    private static int lerp2(int c00, int c10, int c01, int c11, double tx, double tz) {
        int r = StreamTint.ch(c00, 16, c10, c01, c11, tx, tz);
        int g = StreamTint.ch(c00, 8, c10, c01, c11, tx, tz);
        int b = StreamTint.ch(c00, 0, c10, c01, c11, tx, tz);
        return r << 16 | g << 8 | b;
    }

    private static int ch(int c00, int shift, int c10, int c01, int c11, double tx, double tz) {
        double a = (double)(c00 >> shift & 0xFF) * (1.0 - tx) + (double)(c10 >> shift & 0xFF) * tx;
        double b = (double)(c01 >> shift & 0xFF) * (1.0 - tx) + (double)(c11 >> shift & 0xFF) * tx;
        int v = (int)Math.round(a * (1.0 - tz) + b * tz);
        return v < 0 ? 0 : Math.min(v, 255);
    }

    private static Biome ourBiome(ClientLevel level) {
        if (oursLevel != level) {
            oursInstance = level.registryAccess().registry(Registries.BIOME).flatMap(reg -> reg.getOptional(STREAM_BIOME)).orElse(null);
            oursLevel = level;
        }
        return oursInstance;
    }

    public static void onChunkArrived(LevelChunk chunk) {
        Level lvl = chunk.getLevel();
        if (nodesLevel != lvl) {
            nodesLevel = lvl;
            NODES.clear();
            CACHE.clear();
            RETRY.clear();
        }
        if (NODES.size() > 262144) {
            NODES.clear();
        }
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int nx = 0; nx < 2; ++nx) {
            for (int nz = 0; nz < 2; ++nz) {
                int wx = minX + nx * 8 + 4;
                int wz = minZ + nz * 8 + 4;
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, wx & 0xF, wz & 0xF);
                Biome b = chunk.getNoiseBiome(wx >> 2, y >> 2, wz >> 2).value();
                NODES.put(StreamTint.cellKey(Math.floorDiv(wx, 8), Math.floorDiv(wz, 8)), b);
            }
        }
    }

    private static Biome landBiomeAt(ClientLevel level, int x, int z) {
        boolean[] incomplete;
        Biome found;
        int gz;
        int gx = Math.floorDiv(x, 8);
        long key = StreamTint.cellKey(gx, gz = Math.floorDiv(z, 8));
        Object hit = CACHE.get(key);
        if (hit != null) {
            if (hit == INCOMPLETE) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                RETRY.add(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
                return null;
            }
            return hit == NO_LAND ? null : (Biome)hit;
        }
        if (CACHE.size() > 32768) {
            CACHE.clear();
        }
        if ((found = StreamTint.nodeSpiral(gx, gz, incomplete = new boolean[]{false})) != null || !incomplete[0]) {
            CACHE.put(key, found == null ? NO_LAND : found);
        } else {
            CACHE.put(key, INCOMPLETE);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            RETRY.add(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        }
        return found;
    }

    private static Biome nodeSpiral(int gx, int gz, boolean[] incomplete) {
        Biome ours = oursInstance;
        for (int ring = 1; ring <= 24; ++ring) {
            for (int i = -ring; i <= ring; ++i) {
                long[] keys;
                for (long nk : keys = new long[]{StreamTint.cellKey(gx + i, gz - ring), StreamTint.cellKey(gx + i, gz + ring), StreamTint.cellKey(gx - ring, gz + i), StreamTint.cellKey(gx + ring, gz + i)}) {
                    Biome b = NODES.get(nk);
                    if (b == null) {
                        incomplete[0] = true;
                        continue;
                    }
                    if (b == ours) continue;
                    return b;
                }
            }
        }
        return null;
    }

    public static void clientTick() {
        if (++retryTicks % 40 != 0 || RETRY.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) {
            RETRY.clear();
            return;
        }
        CACHE.values().removeIf(v -> v == INCOMPLETE);
        int budget = 64;
        Iterator<Long> it = RETRY.iterator();
        while (it.hasNext() && budget-- > 0) {
            long s = it.next();
            it.remove();
            int sx = SectionPos.x(s);
            int sy = SectionPos.y(s);
            int sz = SectionPos.z(s);
            mc.level.onChunkLoaded(new ChunkPos(sx, sz));
            mc.levelRenderer.setSectionDirty(sx, sy, sz);
        }
    }

    static {
        NODES = new ConcurrentHashMap();
        RETRY = ConcurrentHashMap.newKeySet();
    }

    public static enum Kind {
        WATER,
        GRASS,
        FOLIAGE;

    }
}

