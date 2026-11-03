/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.client;

import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public final class StreamAudio {
    private static final int SCAN_R = 40;
    private static final double BROKEN_COS = 0.5;
    private static final double EMITTER_SPACING = 13.0;
    private static final double LOCAL_R = 9.0;
    private static final double LOCAL_REF = 45.0;
    private static final int GENTLE_VOICES = 4;
    private static final int BROKEN_VOICES = 3;
    private static final double GENTLE_VOL = 0.1;
    private static final double GENTLE_MAX_VOL = 0.25;
    private static final double BROKEN_VOL = 0.35;
    private static final double BROKEN_MAX_VOL = 0.75;
    private static final double STREAM_PRESENCE = 0.5;
    private static final double GENTLE_HOLD = 4.0;
    private static final double GENTLE_GONE = 24.0;
    private static final double BROKEN_HOLD = 6.0;
    private static final double BROKEN_GONE = 32.0;
    private static final double V_SLACK = 10.0;
    private static final double LINK_DIST = 6.0;
    private static final int MAX_FEET = 256;
    private static final int WF_VOICES = 4;
    private static final double WF_HOLD = 12.0;
    private static final double WF_GONE = 64.0;
    private static final double WF_ENERGY_REF = 8.0;
    private static final double WF_BASE = 0.3;
    private static final double WF_MAX_VOL = 0.55;
    private static final double WF_PRESENCE = 0.4;
    private static final double ROAR_THRESHOLD = 0.8;
    private static final double ROAR_BASE = 0.32;
    private static final double ROAR_MAX_VOL = 1.0;
    private static final double ROAR_PRESENCE = 1.0;
    private static final double ROAR_FAR_HOLD = 20.0;
    private static final double ROAR_FAR_GONE = 96.0;
    private static final double DB_PER_DOUBLE = 1.5;
    private static final double LN2 = Math.log(2.0);
    private static final double SMOOTH = 0.3;
    private static final double POS_SMOOTH = 0.12;
    private static final int SILENT_GRACE_TICKS = 60;
    private static final int TICKS_PER_UPDATE = 5;
    private static final float SLEW = 0.04f;
    private static final float AUDIBLE = 0.008f;
    private static final ResourceLocation[] IDS = new ResourceLocation[]{StreamAudio.rl("stream_gentle"), StreamAudio.rl("stream_gentle"), StreamAudio.rl("stream_gentle"), StreamAudio.rl("stream_gentle"), StreamAudio.rl("stream_small"), StreamAudio.rl("stream_small"), StreamAudio.rl("stream_small"), StreamAudio.rl("waterfall_large_close"), StreamAudio.rl("waterfall_large_close"), StreamAudio.rl("waterfall_large_close"), StreamAudio.rl("waterfall_roar_close")};
    private static final int GENTLE_BASE_IDX = 0;
    private static final int BROKEN_BASE_IDX = 4;
    private static final int WF_BASE_IDX = 7;
    private static final int ROAR = 10;
    private static final Voice[] VOICES = new Voice[IDS.length];
    private static final float[] target = new float[IDS.length];
    private static final double[][] at = new double[IDS.length][3];
    private static final int[] wetIdx = new int[6561];
    private static final HashMap<Long, Double> heightCache = new HashMap();
    private static int tickCounter;
    private static ClientLevel lastLevel;
    private static double smRoar;
    private static Map<Long, long[]> lastFeet;
    private static double lastMaster;
    private static double lastRoar;
    private static int lastFlat;
    private static int lastBroken;
    private static final HashMap<Long, long[]> WET;
    private static int wetCursor;
    private static final int WET_MAX_AGE = 120;
    private static final int CANOPY_REACH = 24;

    private StreamAudio() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("streamsreflowing", path);
    }

    public static void tick(ClientLevel level, LocalPlayer player, Map<Long, long[]> feet, boolean enabled, double masterVolume) {
        lastFeet = feet;
        lastMaster = masterVolume;
        if (level != lastLevel) {
            StreamAudio.stopAll();
            lastLevel = level;
            WET.clear();
        }
        if (!enabled || masterVolume <= 0.0) {
            StreamAudio.stopAll();
            return;
        }
        if (++tickCounter >= 5) {
            tickCounter = 0;
            StreamAudio.recompute(level, player, feet, masterVolume);
        }
        for (int i = 0; i < VOICES.length; ++i) {
            StreamAudio.drive(i);
        }
    }

    private static void recompute(ClientLevel level, LocalPlayer player, Map<Long, long[]> feet, double master) {
        Arrays.fill(target, 0.0f);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        StreamAudio.streamLayer(level, px, py, pz, master);
        StreamAudio.fallComponents(feet, px, py, pz, master);
    }

    private static long[] wetMask(ClientLevel level, int cx, int cz, long now, boolean allowRefresh, byte[] grid) {
        long key = (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
        long[] m = WET.get(key);
        if (m != null && now - m[4] <= 120L) {
            return m;
        }
        if (m != null && !allowRefresh) {
            return m;
        }
        if (!level.hasChunk(cx, cz)) {
            WET.remove(key);
            return null;
        }
        long[] out = new long[5];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; ++lz) {
            block1: for (int lx = 0; lx < 16; ++lx) {
                int i = lz * 16 + lx;
                if (grid[lx + lz * 16] == -1) continue;
                int wx = (cx << 4) + lx;
                int wz = (cz << 4) + lz;
                int from = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
                int floor = Math.max(level.getMinBuildHeight(), from - 24);
                for (int y = from; y >= floor; --y) {
                    if (!level.getFluidState(p.set(wx, y, wz)).is(FluidTags.WATER)) continue;
                    if (!level.getBlockState(p.set(wx, y + 1, wz)).isAir()) continue block1;
                    int n = i >> 6;
                    out[n] = out[n] | 1L << (i & 0x3F);
                    continue block1;
                }
            }
        }
        out[4] = now;
        WET.put(key, out);
        if (WET.size() > 4096) {
            WET.clear();
        }
        return out;
    }

    private static void streamLayer(ClientLevel level, double px, double py, double pz, double master) {
        int ipx = Mth.floor(px);
        int ipz = Mth.floor(pz);
        int side = 81;
        byte[] ang = new byte[side * side];
        Arrays.fill(ang, (byte)-1);
        int x0 = ipx - 40;
        int x1 = ipx + 40;
        int z0 = ipz - 40;
        int z1 = ipz + 40;
        long now = level.getGameTime();
        int chunksWide = (x1 >> 4) - (x0 >> 4) + 1;
        int chunksDeep = (z1 >> 4) - (z0 >> 4) + 1;
        int refreshSlot = chunksWide * chunksDeep > 0 ? wetCursor++ % (chunksWide * chunksDeep) : 0;
        int slot = 0;
        for (int cz = z0 >> 4; cz <= z1 >> 4; ++cz) {
            for (int cx = x0 >> 4; cx <= x1 >> 4; ++cx) {
                long[] wet;
                boolean mayRefresh = slot++ == refreshSlot;
                byte[] g = ClientFlowGrids.gridAt(cx, cz);
                if (g == null || (wet = StreamAudio.wetMask(level, cx, cz, now, mayRefresh, g)) == null) continue;
                int bx0 = Math.max(x0, cx << 4);
                int bx1 = Math.min(x1, (cx << 4) + 15);
                int bz0 = Math.max(z0, cz << 4);
                int bz1 = Math.min(z1, (cz << 4) + 15);
                for (int wz = bz0; wz <= bz1; ++wz) {
                    int dz = wz - ipz;
                    int row = (dz + 40) * side;
                    int gz = (wz & 0xF) * 16;
                    for (int wx = bx0; wx <= bx1; ++wx) {
                        int li;
                        int dx = wx - ipx;
                        if (dx * dx + dz * dz > 1600 || (wet[(li = (wz & 0xF) * 16 + (wx & 0xF)) >> 6] & 1L << (li & 0x3F)) == 0L) continue;
                        ang[row + dx + 40] = g[(wx & 0xF) + gz];
                    }
                }
            }
        }
        byte[] kind = new byte[side * side];
        int flatCount = 0;
        int brokenCount = 0;
        int wetN = 0;
        for (int i = 0; i < ang.length; ++i) {
            if (ang[i] == -1) continue;
            int cx = i % side;
            int cz = i / side;
            boolean broken = false;
            for (int n = 0; n < 4 && !broken; ++n) {
                double dot;
                byte na;
                int nx = cx + (n == 0 ? -1 : (n == 1 ? 1 : 0));
                int nz = cz + (n == 2 ? -1 : (n == 3 ? 1 : 0));
                if (nx < 0 || nz < 0 || nx >= side || nz >= side || (na = ang[nz * side + nx]) == -1 || !((dot = StreamFlowGrid.vx(na) * StreamFlowGrid.vx(ang[i]) + StreamFlowGrid.vz(na) * StreamFlowGrid.vz(ang[i])) < 0.5)) continue;
                broken = true;
            }
            kind[i] = (byte)(broken ? 2 : 1);
            if (wetN < wetIdx.length) {
                StreamAudio.wetIdx[wetN++] = i;
            }
            if (broken) {
                ++brokenCount;
                continue;
            }
            ++flatCount;
        }
        lastFlat = flatCount;
        lastBroken = brokenCount;
        StreamAudio.placeEmitters(level, kind, wetIdx, wetN, (byte)1, side, ipx, ipz, px, py, pz, master, 0, 4, 0.1, 0.25, 4.0, 24.0);
        StreamAudio.placeEmitters(level, kind, wetIdx, wetN, (byte)2, side, ipx, ipz, px, py, pz, master, 4, 3, 0.35, 0.75, 6.0, 32.0);
    }

    private static void placeEmitters(ClientLevel level, byte[] kind, int[] wetIdx, int wetN, byte want, int side, int ipx, int ipz, double px, double py, double pz, double master, int baseIdx, int voices, double vol, double maxVol, double hold, double gone) {
        for (int v = 0; v < voices; ++v) {
            double wz;
            double wx;
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int w = 0; w < wetN; ++w) {
                double d;
                int i = wetIdx[w];
                if (kind[i] != want || (d = Math.hypot((wx = (double)(ipx + i % side - 40)) - px, (wz = (double)(ipz + i / side - 40)) - pz)) >= bestD) continue;
                boolean tooClose = false;
                for (int u = 0; u < v && !tooClose; ++u) {
                    tooClose = Math.hypot(wx - at[baseIdx + u][0], wz - at[baseIdx + u][2]) < 13.0;
                }
                if (tooClose) continue;
                bestD = d;
                best = i;
            }
            if (best < 0) {
                return;
            }
            int bx = best % side;
            int bz = best / side;
            wx = ipx + bx - 40;
            wz = ipz + bz - 40;
            int n = 0;
            int r = 9;
            for (int dz = -r; dz <= r; ++dz) {
                int zz = bz + dz;
                if (zz < 0 || zz >= side) continue;
                for (int dx = -r; dx <= r; ++dx) {
                    int xx = bx + dx;
                    if (xx < 0 || xx >= side || dx * dx + dz * dz > r * r || kind[zz * side + xx] != want) continue;
                    ++n;
                }
            }
            double wy = StreamAudio.waterHeight(level, wx, wz, py);
            double dyEff = Math.max(0.0, Math.abs(py - wy) - 10.0);
            double d = Math.sqrt((wx - px) * (wx - px) + (wz - pz) * (wz - pz) + dyEff * dyEff);
            double loud = StreamAudio.decibelLevel((double)n / 45.0, vol, maxVol, 0.5) * StreamAudio.linearFade(d, hold, gone);
            if (loud < (double)0.008f) {
                return;
            }
            StreamAudio.setAt(baseIdx + v, wx, wy, wz);
            StreamAudio.target[baseIdx + v] = (float)(master * loud);
        }
    }

    private static double waterHeight(ClientLevel level, double cx, double cz, double fallback) {
        int z;
        int x = Mth.floor(cx);
        long k = (long)x << 32 ^ (long)(z = Mth.floor(cz)) & 0xFFFFFFFFL;
        Double c = heightCache.get(k);
        if (c != null) {
            return c;
        }
        double h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (heightCache.size() > 256) {
            heightCache.clear();
        }
        heightCache.put(k, h);
        return h;
    }

    private static void fallComponents(Map<Long, long[]> feet, double px, double py, double pz, double master) {
        if (feet == null || feet.isEmpty()) {
            smRoar += (0.0 - smRoar) * 0.3;
            lastRoar = smRoar;
            return;
        }
        int n = feet.size();
        if (n > 256) {
            n = 256;
        }
        double[] fx = new double[n];
        double[] fy = new double[n];
        double[] fz = new double[n];
        double[] fw = new double[n];
        int[] parent = new int[n];
        int k = 0;
        for (Map.Entry<Long, long[]> e : feet.entrySet()) {
            if (k >= n) break;
            long packed = e.getKey();
            fx[k] = (double)BlockPos.getX(packed) + 0.5;
            fy[k] = (double)BlockPos.getY(packed) + 0.5;
            fz[k] = (double)BlockPos.getZ(packed) + 0.5;
            double dx = fx[k] - px;
            double dy = fy[k] - py;
            double dz = fz[k] - pz;
            fw[k] = (double)e.getValue()[0] / (1.0 + (dx * dx + dy * dy + dz * dz) / 576.0);
            parent[k] = k;
            ++k;
        }
        for (int i = 0; i < n; ++i) {
            for (int j = i + 1; j < n; ++j) {
                double dx = fx[i] - fx[j];
                double dy = fy[i] - fy[j];
                double dz = fz[i] - fz[j];
                if (!(dx * dx + dy * dy + dz * dz <= 36.0)) continue;
                StreamAudio.union(parent, i, j);
            }
        }
        HashMap<Integer, double[]> groups = new HashMap<Integer, double[]>();
        for (int i = 0; i < n; ++i) {
            double[] g = groups.computeIfAbsent(StreamAudio.find(parent, i), r -> new double[4]);
            g[0] = g[0] + fw[i];
            g[1] = g[1] + fx[i] * fw[i];
            g[2] = g[2] + fy[i] * fw[i];
            g[3] = g[3] + fz[i] * fw[i];
        }
        double total = 0.0;
        ArrayList<double[]> falls = new ArrayList<double[]>(groups.size());
        for (double[] g : groups.values()) {
            if (g[0] <= 0.0) continue;
            total += g[0];
            falls.add(new double[]{g[0], g[1] / g[0], g[2] / g[0], g[3] / g[0]});
        }
        falls.sort((a, b) -> Double.compare(b[0], a[0]));
        for (int v = 0; v < 4 && v < falls.size(); ++v) {
            double[] g;
            g = (double[])falls.get(v);
            double d = Math.sqrt((g[1] - px) * (g[1] - px) + (g[2] - py) * (g[2] - py) + (g[3] - pz) * (g[3] - pz));
            double loud = StreamAudio.decibelLevel(g[0] / 8.0, 0.3, 0.55, 0.4) * StreamAudio.linearFade(d, 12.0, 64.0);
            if (loud < (double)0.008f) break;
            StreamAudio.setAt(7 + v, g[1], g[2], g[3]);
            StreamAudio.target[7 + v] = (float)(master * loud);
        }
        smRoar += (total / 8.0 - smRoar) * 0.3;
        lastRoar = smRoar;
        double roar = StreamAudio.decibelLevel(smRoar - 0.8, 0.32, 1.0, 1.0);
        if (roar > 0.0 && !falls.isEmpty()) {
            double[] g = (double[])falls.get(0);
            double d = Math.sqrt((g[1] - px) * (g[1] - px) + (g[2] - py) * (g[2] - py) + (g[3] - pz) * (g[3] - pz));
            StreamAudio.setAt(10, g[1], g[2], g[3]);
            StreamAudio.target[10] = (float)(master * roar * StreamAudio.linearFade(d, 20.0, 96.0));
        }
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int rb;
        int ra = StreamAudio.find(parent, a);
        if (ra != (rb = StreamAudio.find(parent, b))) {
            parent[rb] = ra;
        }
    }

    private static double decibelLevel(double amount, double base, double max, double presence) {
        if (amount <= 0.0) {
            return 0.0;
        }
        double doublings = Math.log(Math.max(1.0, amount)) / LN2;
        double amp = Math.pow(10.0, 1.5 * doublings / 20.0);
        return Math.min(max, base * Math.min(1.0, amount / presence) * amp);
    }

    private static double linearFade(double d, double hold, double gone) {
        if (d <= hold) {
            return 1.0;
        }
        if (d >= gone) {
            return 0.0;
        }
        return 1.0 - (d - hold) / (gone - hold);
    }

    private static double fadeOut(double d, double hold, double gone) {
        if (d <= hold) {
            return 1.0;
        }
        if (d >= gone) {
            return 0.0;
        }
        return 0.5 * (1.0 + Math.cos(Math.PI * (d - hold) / (gone - hold)));
    }

    private static double fadeIn(double d, double hold, double gone) {
        return 1.0 - StreamAudio.fadeOut(d, hold, gone);
    }

    private static void setAt(int i, double x, double y, double z) {
        StreamAudio.at[i][0] = x;
        StreamAudio.at[i][1] = y;
        StreamAudio.at[i][2] = z;
    }

    private static void drive(int i) {
        boolean live;
        Minecraft mc = Minecraft.getInstance();
        Voice v = VOICES[i];
        float want = target[i];
        boolean bl = live = v != null && !v.isStopped() && mc.getSoundManager().isActive(v);
        if (!live) {
            if (want < 0.008f) {
                return;
            }
            v = new Voice(IDS[i]);
            v.set(at[i][0], at[i][1], at[i][2], 0.012f);
            StreamAudio.VOICES[i] = v;
            mc.getSoundManager().play(v);
        }
        v.approach(want, at[i][0], at[i][1], at[i][2]);
    }

    public static String debugSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("ambience volume: ").append(String.format("%.2f", lastMaster)).append(lastMaster <= 0.0 ? "  (OFF -- set waterAmbience in the client config)" : "").append('\n');
        sb.append("flat columns: ").append(lastFlat).append("   broken: ").append(lastBroken).append(lastFlat + lastBroken == 0 ? "  (no flowing water found near you)" : "").append("    roar drive: ").append(String.format("%.2f", lastRoar)).append('\n');
        Map<Long, long[]> feet = lastFeet;
        sb.append("waterfall drops: ").append(feet == null ? 0 : feet.size()).append('\n');
        String[] names = new String[]{"gentle_1", "gentle_2", "gentle_3", "gentle_4", "broken_1", "broken_2", "broken_3", "fall_1", "fall_2", "fall_3", "roar"};
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < names.length && i < VOICES.length; ++i) {
            Voice v = VOICES[i];
            boolean live = v != null && !v.isStopped() && mc.getSoundManager().isActive(v);
            sb.append(String.format("  %-10s target %.3f  actual %.3f  %s%n", names[i], Float.valueOf(target[i]), Float.valueOf(v == null ? 0.0f : v.getVolume()), live ? "playing" : "-"));
        }
        return sb.toString();
    }

    private static void stopAll() {
        smRoar = 0.0;
        heightCache.clear();
        for (int i = 0; i < VOICES.length; ++i) {
            if (VOICES[i] != null) {
                VOICES[i].kill();
                StreamAudio.VOICES[i] = null;
            }
            StreamAudio.target[i] = 0.0f;
        }
    }

    static {
        WET = new HashMap();
    }

    private static final class Voice
    extends AbstractTickableSoundInstance {
        private float want;
        private boolean dying;
        private int silentTicks;

        private Voice(ResourceLocation id) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.AMBIENT, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0f;
            this.pitch = 1.0f;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void set(double x, double y, double z, float vol) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = vol;
        }

        void approach(float w, double x, double y, double z) {
            this.want = w;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        void kill() {
            this.dying = true;
            this.want = 0.0f;
        }

        @Override
        public void tick() {
            float d = this.want - this.volume;
            this.volume += Mth.clamp(d, -0.04f, 0.04f);
            if (this.volume < 0.008f && this.want < 0.008f) {
                this.volume = 0.0f;
                if (this.dying) {
                    this.stop();
                } else if (++this.silentTicks > 60) {
                    this.stop();
                }
            } else {
                this.silentTicks = 0;
            }
        }
    }

    private static interface SoundInstanceAttenuationNone {
    }
}

