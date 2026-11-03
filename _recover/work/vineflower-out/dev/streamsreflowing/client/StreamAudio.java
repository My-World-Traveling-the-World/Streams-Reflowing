package dev.streamsreflowing.client;

import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;

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
   private static final float SLEW = 0.04F;
   private static final float AUDIBLE = 0.008F;
   private static final ResourceLocation[] IDS = new ResourceLocation[]{
      rl("stream_gentle"),
      rl("stream_gentle"),
      rl("stream_gentle"),
      rl("stream_gentle"),
      rl("stream_small"),
      rl("stream_small"),
      rl("stream_small"),
      rl("waterfall_large_close"),
      rl("waterfall_large_close"),
      rl("waterfall_large_close"),
      rl("waterfall_roar_close")
   };
   private static final int GENTLE_BASE_IDX = 0;
   private static final int BROKEN_BASE_IDX = 4;
   private static final int WF_BASE_IDX = 7;
   private static final int ROAR = 10;
   private static final StreamAudio.Voice[] VOICES = new StreamAudio.Voice[IDS.length];
   private static final float[] target = new float[IDS.length];
   private static final double[][] at = new double[IDS.length][3];
   private static final int[] wetIdx = new int[6561];
   private static final HashMap<Long, Double> heightCache = new HashMap<>();
   private static int tickCounter;
   private static ClientLevel lastLevel;
   private static double smRoar;
   private static Map<Long, long[]> lastFeet;
   private static double lastMaster;
   private static double lastRoar;
   private static int lastFlat;
   private static int lastBroken;
   private static final HashMap<Long, long[]> WET = new HashMap<>();
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
         stopAll();
         lastLevel = level;
         WET.clear();
      }

      if (enabled && !(masterVolume <= 0.0)) {
         if (++tickCounter >= 5) {
            tickCounter = 0;
            recompute(level, player, feet, masterVolume);
         }

         for (int i = 0; i < VOICES.length; i++) {
            drive(i);
         }
      } else {
         stopAll();
      }
   }

   private static void recompute(ClientLevel level, LocalPlayer player, Map<Long, long[]> feet, double master) {
      Arrays.fill(target, 0.0F);
      double px = player.getX();
      double py = player.getY();
      double pz = player.getZ();
      streamLayer(level, px, py, pz, master);
      fallComponents(feet, px, py, pz, master);
   }

   private static long[] wetMask(ClientLevel level, int cx, int cz, long now, boolean allowRefresh, byte[] grid) {
      long key = (long)cx << 32 ^ (long)cz & 4294967295L;
      long[] m = WET.get(key);
      if (m != null && now - m[4] <= 120L) {
         return m;
      } else if (m != null && !allowRefresh) {
         return m;
      } else if (!level.hasChunk(cx, cz)) {
         WET.remove(key);
         return null;
      } else {
         long[] out = new long[5];
         MutableBlockPos p = new MutableBlockPos();

         for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
               int i = lz * 16 + lx;
               if (grid[lx + lz * 16] != -1) {
                  int wx = (cx << 4) + lx;
                  int wz = (cz << 4) + lz;
                  int from = level.getHeight(Types.MOTION_BLOCKING, wx, wz) - 1;
                  int floor = Math.max(level.getMinBuildHeight(), from - 24);

                  for (int y = from; y >= floor; y--) {
                     if (level.getFluidState(p.set(wx, y, wz)).is(FluidTags.WATER)) {
                        if (level.getBlockState(p.set(wx, y + 1, wz)).isAir()) {
                           out[i >> 6] = out[i >> 6] | 1L << (i & 63);
                        }
                        break;
                     }
                  }
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

      for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
         for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            boolean mayRefresh = slot++ == refreshSlot;
            byte[] g = ClientFlowGrids.gridAt(cx, cz);
            if (g != null) {
               long[] wet = wetMask(level, cx, cz, now, mayRefresh, g);
               if (wet != null) {
                  int bx0 = Math.max(x0, cx << 4);
                  int bx1 = Math.min(x1, (cx << 4) + 15);
                  int bz0 = Math.max(z0, cz << 4);
                  int bz1 = Math.min(z1, (cz << 4) + 15);

                  for (int wz = bz0; wz <= bz1; wz++) {
                     int dz = wz - ipz;
                     int row = (dz + 40) * side;
                     int gz = (wz & 15) * 16;

                     for (int wx = bx0; wx <= bx1; wx++) {
                        int dx = wx - ipx;
                        if (dx * dx + dz * dz <= 1600) {
                           int li = (wz & 15) * 16 + (wx & 15);
                           if ((wet[li >> 6] & 1L << (li & 63)) != 0L) {
                              ang[row + dx + 40] = g[(wx & 15) + gz];
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      byte[] kind = new byte[side * side];
      int flatCount = 0;
      int brokenCount = 0;
      int wetN = 0;

      for (int i = 0; i < ang.length; i++) {
         if (ang[i] != -1) {
            int cxx = i % side;
            int cz = i / side;
            boolean broken = false;

            for (int n = 0; n < 4 && !broken; n++) {
               int nx = cxx + (n == 0 ? -1 : (n == 1 ? 1 : 0));
               int nz = cz + (n == 2 ? -1 : (n == 3 ? 1 : 0));
               if (nx >= 0 && nz >= 0 && nx < side && nz < side) {
                  byte na = ang[nz * side + nx];
                  if (na != -1) {
                     double dot = StreamFlowGrid.vx(na) * StreamFlowGrid.vx(ang[i]) + StreamFlowGrid.vz(na) * StreamFlowGrid.vz(ang[i]);
                     if (dot < 0.5) {
                        broken = true;
                     }
                  }
               }
            }

            kind[i] = (byte)(broken ? 2 : 1);
            if (wetN < wetIdx.length) {
               wetIdx[wetN++] = i;
            }

            if (broken) {
               brokenCount++;
            } else {
               flatCount++;
            }
         }
      }

      lastFlat = flatCount;
      lastBroken = brokenCount;
      placeEmitters(level, kind, wetIdx, wetN, (byte)1, side, ipx, ipz, px, py, pz, master, 0, 4, 0.1, 0.25, 4.0, 24.0);
      placeEmitters(level, kind, wetIdx, wetN, (byte)2, side, ipx, ipz, px, py, pz, master, 4, 3, 0.35, 0.75, 6.0, 32.0);
   }

   private static void placeEmitters(
      ClientLevel level,
      byte[] kind,
      int[] wetIdx,
      int wetN,
      byte want,
      int side,
      int ipx,
      int ipz,
      double px,
      double py,
      double pz,
      double master,
      int baseIdx,
      int voices,
      double vol,
      double maxVol,
      double hold,
      double gone
   ) {
      for (int v = 0; v < voices; v++) {
         int best = -1;
         double bestD = Double.MAX_VALUE;

         for (int w = 0; w < wetN; w++) {
            int i = wetIdx[w];
            if (kind[i] == want) {
               double wx = (double)(ipx + i % side - 40);
               double wz = (double)(ipz + i / side - 40);
               double d = Math.hypot(wx - px, wz - pz);
               if (!(d >= bestD)) {
                  boolean tooClose = false;

                  for (int u = 0; u < v && !tooClose; u++) {
                     tooClose = Math.hypot(wx - at[baseIdx + u][0], wz - at[baseIdx + u][2]) < 13.0;
                  }

                  if (!tooClose) {
                     bestD = d;
                     best = i;
                  }
               }
            }
         }

         if (best < 0) {
            return;
         }

         int bx = best % side;
         int bz = best / side;
         double wx = (double)(ipx + bx - 40);
         double wz = (double)(ipz + bz - 40);
         int n = 0;
         int r = 9;

         for (int dz = -r; dz <= r; dz++) {
            int zz = bz + dz;
            if (zz >= 0 && zz < side) {
               for (int dx = -r; dx <= r; dx++) {
                  int xx = bx + dx;
                  if (xx >= 0 && xx < side && dx * dx + dz * dz <= r * r && kind[zz * side + xx] == want) {
                     n++;
                  }
               }
            }
         }

         double wy = waterHeight(level, wx, wz, py);
         double dyEff = Math.max(0.0, Math.abs(py - wy) - 10.0);
         double d = Math.sqrt((wx - px) * (wx - px) + (wz - pz) * (wz - pz) + dyEff * dyEff);
         double loud = decibelLevel((double)n / 45.0, vol, maxVol, 0.5) * linearFade(d, hold, gone);
         if (loud < 0.008F) {
            return;
         }

         setAt(baseIdx + v, wx, wy, wz);
         target[baseIdx + v] = (float)(master * loud);
      }
   }

   private static double waterHeight(ClientLevel level, double cx, double cz, double fallback) {
      int x = Mth.floor(cx);
      int z = Mth.floor(cz);
      long k = (long)x << 32 ^ (long)z & 4294967295L;
      Double c = heightCache.get(k);
      if (c != null) {
         return c;
      } else {
         double h = (double)level.getHeight(Types.WORLD_SURFACE, x, z);
         if (heightCache.size() > 256) {
            heightCache.clear();
         }

         heightCache.put(k, h);
         return h;
      }
   }

   private static void fallComponents(Map<Long, long[]> feet, double px, double py, double pz, double master) {
      if (feet != null && !feet.isEmpty()) {
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

         for (Entry<Long, long[]> e : feet.entrySet()) {
            if (k >= n) {
               break;
            }

            long packed = e.getKey();
            fx[k] = (double)BlockPos.getX(packed) + 0.5;
            fy[k] = (double)BlockPos.getY(packed) + 0.5;
            fz[k] = (double)BlockPos.getZ(packed) + 0.5;
            double dx = fx[k] - px;
            double dy = fy[k] - py;
            double dz = fz[k] - pz;
            fw[k] = (double)e.getValue()[0] / (1.0 + (dx * dx + dy * dy + dz * dz) / 576.0);
            parent[k] = k++;
         }

         for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
               double dx = fx[i] - fx[j];
               double dy = fy[i] - fy[j];
               double dz = fz[i] - fz[j];
               if (dx * dx + dy * dy + dz * dz <= 36.0) {
                  union(parent, i, j);
               }
            }
         }

         HashMap<Integer, double[]> groups = new HashMap<>();

         for (int i = 0; i < n; i++) {
            double[] g = groups.computeIfAbsent(find(parent, i), r -> new double[4]);
            g[0] += fw[i];
            g[1] += fx[i] * fw[i];
            g[2] += fy[i] * fw[i];
            g[3] += fz[i] * fw[i];
         }

         double total = 0.0;
         List<double[]> falls = new ArrayList<>(groups.size());

         for (double[] g : groups.values()) {
            if (!(g[0] <= 0.0)) {
               total += g[0];
               falls.add(new double[]{g[0], g[1] / g[0], g[2] / g[0], g[3] / g[0]});
            }
         }

         falls.sort((a, b) -> Double.compare(b[0], a[0]));

         for (int v = 0; v < 4 && v < falls.size(); v++) {
            double[] gx = falls.get(v);
            double d = Math.sqrt((gx[1] - px) * (gx[1] - px) + (gx[2] - py) * (gx[2] - py) + (gx[3] - pz) * (gx[3] - pz));
            double loud = decibelLevel(gx[0] / 8.0, 0.3, 0.55, 0.4) * linearFade(d, 12.0, 64.0);
            if (loud < 0.008F) {
               break;
            }

            setAt(7 + v, gx[1], gx[2], gx[3]);
            target[7 + v] = (float)(master * loud);
         }

         smRoar = smRoar + (total / 8.0 - smRoar) * 0.3;
         lastRoar = smRoar;
         double roar = decibelLevel(smRoar - 0.8, 0.32, 1.0, 1.0);
         if (roar > 0.0 && !falls.isEmpty()) {
            double[] gx = falls.get(0);
            double d = Math.sqrt((gx[1] - px) * (gx[1] - px) + (gx[2] - py) * (gx[2] - py) + (gx[3] - pz) * (gx[3] - pz));
            setAt(10, gx[1], gx[2], gx[3]);
            target[10] = (float)(master * roar * linearFade(d, 20.0, 96.0));
         }
      } else {
         smRoar = smRoar + (0.0 - smRoar) * 0.3;
         lastRoar = smRoar;
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
      int ra = find(parent, a);
      int rb = find(parent, b);
      if (ra != rb) {
         parent[rb] = ra;
      }
   }

   private static double decibelLevel(double amount, double base, double max, double presence) {
      if (amount <= 0.0) {
         return 0.0;
      } else {
         double doublings = Math.log(Math.max(1.0, amount)) / LN2;
         double amp = Math.pow(10.0, 1.5 * doublings / 20.0);
         return Math.min(max, base * Math.min(1.0, amount / presence) * amp);
      }
   }

   private static double linearFade(double d, double hold, double gone) {
      if (d <= hold) {
         return 1.0;
      } else {
         return d >= gone ? 0.0 : 1.0 - (d - hold) / (gone - hold);
      }
   }

   private static double fadeOut(double d, double hold, double gone) {
      if (d <= hold) {
         return 1.0;
      } else {
         return d >= gone ? 0.0 : 0.5 * (1.0 + Math.cos(Math.PI * (d - hold) / (gone - hold)));
      }
   }

   private static double fadeIn(double d, double hold, double gone) {
      return 1.0 - fadeOut(d, hold, gone);
   }

   private static void setAt(int i, double x, double y, double z) {
      at[i][0] = x;
      at[i][1] = y;
      at[i][2] = z;
   }

   private static void drive(int i) {
      Minecraft mc = Minecraft.getInstance();
      StreamAudio.Voice v = VOICES[i];
      float want = target[i];
      boolean live = v != null && !v.isStopped() && mc.getSoundManager().isActive(v);
      if (!live) {
         if (want < 0.008F) {
            return;
         }

         v = new StreamAudio.Voice(IDS[i]);
         v.set(at[i][0], at[i][1], at[i][2], 0.012F);
         VOICES[i] = v;
         mc.getSoundManager().play(v);
      }

      v.approach(want, at[i][0], at[i][1], at[i][2]);
   }

   public static String debugSummary() {
      StringBuilder sb = new StringBuilder();
      sb.append("ambience volume: ")
         .append(String.format("%.2f", lastMaster))
         .append(lastMaster <= 0.0 ? "  (OFF -- set waterAmbience in the client config)" : "")
         .append('\n');
      sb.append("flat columns: ")
         .append(lastFlat)
         .append("   broken: ")
         .append(lastBroken)
         .append(lastFlat + lastBroken == 0 ? "  (no flowing water found near you)" : "")
         .append("    roar drive: ")
         .append(String.format("%.2f", lastRoar))
         .append('\n');
      Map<Long, long[]> feet = lastFeet;
      sb.append("waterfall drops: ").append(feet == null ? 0 : feet.size()).append('\n');
      String[] names = new String[]{"gentle_1", "gentle_2", "gentle_3", "gentle_4", "broken_1", "broken_2", "broken_3", "fall_1", "fall_2", "fall_3", "roar"};
      Minecraft mc = Minecraft.getInstance();

      for (int i = 0; i < names.length && i < VOICES.length; i++) {
         StreamAudio.Voice v = VOICES[i];
         boolean live = v != null && !v.isStopped() && mc.getSoundManager().isActive(v);
         sb.append(String.format("  %-10s target %.3f  actual %.3f  %s%n", names[i], target[i], v == null ? 0.0F : v.getVolume(), live ? "playing" : "-"));
      }

      return sb.toString();
   }

   private static void stopAll() {
      smRoar = 0.0;
      heightCache.clear();

      for (int i = 0; i < VOICES.length; i++) {
         if (VOICES[i] != null) {
            VOICES[i].kill();
            VOICES[i] = null;
         }

         target[i] = 0.0F;
      }
   }

   private interface SoundInstanceAttenuationNone {
   }

   private static final class Voice extends AbstractTickableSoundInstance {
      private float want;
      private boolean dying;
      private int silentTicks;

      private Voice(ResourceLocation id) {
         super(SoundEvent.createVariableRangeEvent(id), SoundSource.AMBIENT, RandomSource.create());
         this.looping = true;
         this.delay = 0;
         this.volume = 0.0F;
         this.pitch = 1.0F;
         this.attenuation = Attenuation.NONE;
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
         this.want = 0.0F;
      }

      @Override
      public void tick() {
         float d = this.want - this.volume;
         this.volume = this.volume + Mth.clamp(d, -0.04F, 0.04F);
         if (this.volume < 0.008F && this.want < 0.008F) {
            this.volume = 0.0F;
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
}
