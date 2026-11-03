package dev.streamsreflowing.flow;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public final class StreamFlowGrid {
   public static final int SIZE = 256;
   public static final byte NONE = -1;
   private static final double TWO_PI = Math.PI * 2;
   public static final double MIN_FLOW = 0.5;
   private static final int SMOOTH_R = 2;
   private static final int FEATHER = 4;
   private static final int WIDE_MIN = 10;
   private static final int Q_DILATE = 1;
   private static final double BANK_FOLLOW = 1.6;
   private static final int BANK_R = 8;
   private static final int REACH_BLOCKS = 8;
   private static final int[] NX4 = new int[]{1, -1, 0, 0};
   private static final int[] NZ4 = new int[]{0, 0, 1, -1};
   private static final int MOUTH_LOOK = 6;
   private static final double MOUTH_FAN = 0.9;
   private static final double MOUTH_TURN_MAX = Math.PI / 4;
   private static final double RIVER_FLARE = 1.5;
   private static final double FLARE_MAX_R = 0.5;
   private static final double[] LUT_COS = new double[256];
   private static final double[] LUT_SIN = new double[256];
   private static final int MAX_CACHED = 4096;
   private static final byte[] EMPTY;
   private static final ConcurrentHashMap<Long, byte[]> CACHE;
   private static final ConcurrentLinkedQueue<Long> ORDER;
   private static final Set<Long> INCOMPLETE;
   private static final RawFlowTiles.Tile STILL_TILE;

   private StreamFlowGrid() {
   }

   public static byte encode(double dx, double dz) {
      double a = Math.atan2(dz, dx);
      if (a < 0.0) {
         a += Math.PI * 2;
      }

      int idx = (int)Math.round(a / (Math.PI * 2) * 255.0);
      if (idx >= 255) {
         idx -= 255;
      }

      return (byte)idx;
   }

   public static double vx(byte b) {
      return LUT_COS[b & 255];
   }

   public static double vz(byte b) {
      return LUT_SIN[b & 255];
   }

   private static long key(int cx, int cz) {
      return (long)cx << 32 ^ (long)cz & 4294967295L;
   }

   public static byte angleForPhysics(BlockGetter level, int x, int z) {
      if (level instanceof Level lv && !lv.dimension().equals(Level.OVERWORLD)) {
         return -1;
      }

      byte own = angleForPhysics(x, z);
      if (own != -1) {
         return own;
      } else if (RTFFlowUnderlay.empty()) {
         return -1;
      } else {
         boolean var10000;
         label22: {
            if (level instanceof Level l2 && !l2.isClientSide()) {
               var10000 = false;
               break label22;
            }

            var10000 = true;
         }

         boolean client = var10000;
         return RTFFlowUnderlay.angleAt(client, x, z);
      }
   }

   public static byte angleForEntityPush(BlockGetter level, int x, int z) {
      if (level instanceof Level lv && !lv.dimension().equals(Level.OVERWORLD)) {
         return -1;
      }

      return angleForPhysics(x, z);
   }

   private static byte angleForPhysics(int x, int z) {
      byte a = ClientFlowGrids.angleAt(x, z);
      if (a != -1) {
         return a;
      } else {
         byte[] grid = CACHE.get(key(x >> 4, z >> 4));
         return grid != null && grid.length != 0 ? grid[(x & 15) + ((z & 15) << 4)] : -1;
      }
   }

   public static boolean isIncomplete(int cx, int cz) {
      return INCOMPLETE.contains(key(cx, cz));
   }

   public static byte[] getOrBuild(
      ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, boolean flowRivers, int cx, int cz
   ) {
      long k = key(cx, cz);
      byte[] cached = CACHE.get(k);
      if (cached != null && !INCOMPLETE.contains(k)) {
         return cached.length == 0 ? null : cached;
      } else {
         FlowTrace.gridBuilds.incrementAndGet();
         byte[] grid = build(engine, classifier, orient, flowRivers, cx, cz, false, null);
         byte[] out = cache(k, grid, cached == null);
         INCOMPLETE.remove(k);
         return out;
      }
   }

   public static byte[] buildProvisional(
      ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, boolean flowRivers, int cx, int cz
   ) {
      long k = key(cx, cz);
      byte[] cached = CACHE.get(k);
      if (cached != null) {
         return cached.length == 0 ? null : cached;
      } else {
         FlowTrace.gridBuilds.incrementAndGet();
         boolean[] incomplete = new boolean[]{false};
         byte[] grid = build(engine, classifier, orient, flowRivers, cx, cz, true, incomplete);
         byte[] out = cache(k, grid, true);
         if (incomplete[0]) {
            INCOMPLETE.add(k);
         }

         return out;
      }
   }

   private static byte[] cache(long k, byte[] grid, boolean newEntry) {
      boolean any = false;

      for (byte b : grid) {
         if (b != -1) {
            any = true;
            break;
         }
      }

      CACHE.put(k, any ? grid : EMPTY);
      if (newEntry) {
         ORDER.add(k);
      }

      while (CACHE.size() > 4096) {
         Long old = ORDER.poll();
         if (old == null) {
            break;
         }

         CACHE.remove(old);
         INCOMPLETE.remove(old);
      }

      return any ? grid : null;
   }

   private static RawFlowTiles.Tile makeStillTile() {
      short[] sy = new short[256];
      Arrays.fill(sy, (short)-32768);
      return new RawFlowTiles.Tile(new byte[256], new byte[256], sy);
   }

   private static double flowRadReady(
      RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, int x, int z, boolean readyOnly, boolean[] incomplete
   ) {
      if (readyOnly && !RiverFlowFields.isRegionReady(x, z)) {
         incomplete[0] = true;
         return Double.NaN;
      } else {
         return RiverFlowFields.flowRadAt(classifier, orient, x, z);
      }
   }

   private static byte[] build(
      RiverEngine engine,
      RiverFlowField.BiomeClass classifier,
      RiverFlowField.OrientationField orient,
      boolean flowRivers,
      int cx,
      int cz,
      boolean readyOnly,
      boolean[] incomplete
   ) {
      int m = 1 + Math.max(8, Math.max(4, 8));
      int span = 16 + 2 * m;
      int baseX = (cx << 4) - m;
      int baseZ = (cz << 4) - m;
      boolean anyRiverNear = false;
      if (flowRivers) {
         label568:
         for (int j = 0; j < span + 7; j += 8) {
            int pz = Math.min(j, span - 1);

            for (int i = 0; i < span + 7; i += 8) {
               int px = Math.min(i, span - 1);
               if (!Double.isNaN(flowRadReady(classifier, orient, baseX + px, baseZ + pz, readyOnly, incomplete))) {
                  anyRiverNear = true;
                  break label568;
               }
            }
         }
      }

      boolean useRivers = flowRivers && anyRiverNear;
      if (!anyRiverNear
         && (!readyOnly || engine.platesReadyNear((double)baseX + (double)span / 2.0, (double)baseZ + (double)span / 2.0, (double)span * 0.7072))
         && !engine.anyStreamsNear((double)baseX + (double)span / 2.0, (double)baseZ + (double)span / 2.0, (double)span * 0.7072)) {
         byte[] none = new byte[256];
         Arrays.fill(none, (byte)-1);
         return none;
      } else {
         double[] rawX = new double[span * span];
         double[] rawZ = new double[span * span];
         boolean[] has = new boolean[span * span];
         double[] riverAngle = null;
         if (useRivers) {
            riverAngle = new double[span * span];
            Arrays.fill(riverAngle, Double.NaN);
         }

         boolean anyRiver = false;
         int baseChunkX = baseX >> 4;
         int baseChunkZ = baseZ >> 4;
         RawFlowTiles.Tile[] tiles = new RawFlowTiles.Tile[9];

         for (int tz = 0; tz < 3; tz++) {
            for (int tx = 0; tx < 3; tx++) {
               int tcx = baseChunkX + tx;
               int tcz = baseChunkZ + tz;
               if (readyOnly && !engine.platesReadyNear((double)(tcx << 4) + 8.0, (double)(tcz << 4) + 8.0, 9.0)) {
                  incomplete[0] = true;
                  tiles[tz * 3 + tx] = STILL_TILE;
               } else {
                  tiles[tz * 3 + tx] = tileOrCompute(engine, tcx, tcz);
               }
            }
         }

         for (int j = 0; j < span; j++) {
            for (int ix = 0; ix < span; ix++) {
               int wx = baseX + ix;
               int wz = baseZ + j;
               int idx = j * span + ix;
               double ux = 0.0;
               double uz = 0.0;
               RawFlowTiles.Tile tile = tileFor(tiles, baseChunkX, baseChunkZ, wx, wz);
               boolean hasFlow;
               if (tile != null) {
                  int ti = (wx & 15) * 16 + (wz & 15);
                  byte bx = tile.dirX()[ti];
                  byte bz = tile.dirZ()[ti];
                  hasFlow = bx != 0 || bz != 0;
                  if (hasFlow) {
                     ux = (double)bx / 127.0;
                     uz = (double)bz / 127.0;
                  }
               } else if (readyOnly && !engine.platesReadyNear((double)wx + 0.5, (double)wz + 0.5, 0.0)) {
                  incomplete[0] = true;
                  hasFlow = false;
               } else {
                  ColumnPlan p = engine.plan(wx, wz);
                  double pfx = p.fluidFlowDirX();
                  double pfz = p.fluidFlowDirZ();
                  double pl = Math.sqrt(pfx * pfx + pfz * pfz);
                  hasFlow = p.inChannel() && pl >= 0.5;
                  if (hasFlow) {
                     ux = pfx / pl;
                     uz = pfz / pl;
                  }
               }

               if (hasFlow) {
                  rawX[idx] = ux;
                  rawZ[idx] = uz;
                  has[idx] = true;
               } else if (useRivers) {
                  double a = flowRadReady(classifier, orient, wx, wz, readyOnly, incomplete);
                  if (!Double.isNaN(a)) {
                     riverAngle[idx] = a;
                     anyRiver = true;
                  }
               }
            }
         }

         if (anyRiver) {
            int qBaseX = (baseX >> 2) - 1;
            int qBaseZ = (baseZ >> 2) - 1;
            int qSpanX = (baseX + span - 1 >> 2) - qBaseX + 1 + 1;
            int qSpanZ = (baseZ + span - 1 >> 2) - qBaseZ + 1 + 1;
            boolean[] riverQ = new boolean[qSpanX * qSpanZ];

            for (int qj = 0; qj < qSpanZ; qj++) {
               for (int qi = 0; qi < qSpanX; qi++) {
                  riverQ[qj * qSpanX + qi] = classifier.classify(qBaseX + qi << 2, qBaseZ + qj << 2) == 1;
               }
            }

            float[] rqf = new float[qSpanX * qSpanZ];

            for (int qi = 0; qi < riverQ.length; qi++) {
               rqf[qi] = riverQ[qi] ? 1.0F : 0.0F;
            }

            for (int j = 0; j < span; j++) {
               for (int ix = 0; ix < span; ix++) {
                  int idxx = j * span + ix;
                  if (!Double.isNaN(riverAngle[idxx]) && bilinearRiver(rqf, qSpanX, qSpanZ, qBaseX, qBaseZ, baseX + ix, baseZ + j) >= 0.5) {
                     double a = riverAngle[idxx];
                     rawX[idxx] = Math.cos(a);
                     rawZ[idxx] = Math.sin(a);
                     has[idxx] = true;
                  }
               }
            }
         }

         boolean[] core = (boolean[])has.clone();
         boolean[] hasB = new boolean[span * span];
         double[] rxB = new double[span * span];
         double[] rzB = new double[span * span];

         for (int pass = 0; pass < 8; pass++) {
            System.arraycopy(has, 0, hasB, 0, has.length);
            System.arraycopy(rawX, 0, rxB, 0, rawX.length);
            System.arraycopy(rawZ, 0, rzB, 0, rawZ.length);

            for (int j = 1; j < span - 1; j++) {
               for (int ixx = 1; ixx < span - 1; ixx++) {
                  int idxx = j * span + ixx;
                  if (!has[idxx]) {
                     for (int k = 0; k < 4; k++) {
                        int nidx = (j + NZ4[k]) * span + ixx + NX4[k];
                        if (has[nidx]) {
                           hasB[idxx] = true;
                           rxB[idxx] = rawX[nidx];
                           rzB[idxx] = rawZ[nidx];
                           break;
                        }
                     }
                  }
               }
            }

            boolean[] ht = has;
            has = hasB;
            hasB = ht;
            double[] xt = rawX;
            rawX = rxB;
            rxB = xt;
            double[] zt = rawZ;
            rawZ = rzB;
            rzB = zt;
         }

         double[] mouthX = new double[span * span];
         double[] mouthZ = new double[span * span];
         double[] mouthW = new double[span * span];
         int[] seedOf = new int[span * span];
         Arrays.fill(seedOf, -1);
         ArrayDeque<Integer> mouths = new ArrayDeque<>();

         for (int j = 1; j < span - 1; j++) {
            for (int ixxx = 1; ixxx < span - 1; ixxx++) {
               int idxx = j * span + ixxx;
               if (core[idxx]) {
                  double dx = rawX[idxx];
                  double dz = rawZ[idxx];
                  if (!(dx * dx + dz * dz < 1.0E-9)) {
                     int ai = ixxx + (int)Math.round(dx * 2.0);
                     int aj = j + (int)Math.round(dz * 2.0);
                     if (ai >= 0 && aj >= 0 && ai < span && aj < span && !core[aj * span + ai] && classifier.classify(baseX + ai, baseZ + aj) == 2) {
                        seedOf[idxx] = idxx;
                        mouths.add(idxx);
                     }
                  }
               }
            }
         }

         if (!mouths.isEmpty()) {
            ArrayDeque<Integer> sea = new ArrayDeque<>(mouths);

            while (!sea.isEmpty()) {
               int cur = sea.poll();
               int ci0 = cur % span;
               int cj0 = cur / span;

               for (int kx = 0; kx < 4; kx++) {
                  int ni = ci0 + NX4[kx];
                  int nj = cj0 + NZ4[kx];
                  if (ni >= 1 && nj >= 1 && ni < span - 1 && nj < span - 1) {
                     int nid = nj * span + ni;
                     if (seedOf[nid] < 0 && !core[nid] && has[nid]) {
                        seedOf[nid] = seedOf[cur];
                        sea.add(nid);
                     }
                  }
               }
            }

            for (int idxx = 0; idxx < seedOf.length; idxx++) {
               if (seedOf[idxx] >= 0 && !core[idxx]) {
                  double vx = (double)(idxx % span - seedOf[idxx] % span);
                  double vz = (double)(idxx / span - seedOf[idxx] / span);
                  double vl = Math.hypot(vx, vz);
                  if (vl > 1.0E-9) {
                     mouthX[idxx] = vx / vl;
                     mouthZ[idxx] = vz / vl;
                     mouthW[idxx] = 1.0;
                  }
               }
            }

            int[] up = new int[span * span];
            Arrays.fill(up, -1);
            ArrayDeque<Integer> chan = new ArrayDeque<>();

            for (int idxxx : mouths) {
               up[idxxx] = 0;
               chan.add(idxxx);
            }

            while (!chan.isEmpty()) {
               int cur = chan.poll();
               if (up[cur] < 6) {
                  int ci0 = cur % span;
                  int cj0 = cur / span;

                  for (int kxx = 0; kxx < 4; kxx++) {
                     int ni = ci0 + NX4[kxx];
                     int nj = cj0 + NZ4[kxx];
                     if (ni >= 1 && nj >= 1 && ni < span - 1 && nj < span - 1) {
                        int nid = nj * span + ni;
                        if (up[nid] < 0 && core[nid]) {
                           up[nid] = up[cur] + 1;
                           chan.add(nid);
                        }
                     }
                  }
               }
            }

            for (int idxxx = 0; idxxx < up.length; idxxx++) {
               if (up[idxxx] >= 0) {
                  mouthW[idxxx] = 1.0 - (double)up[idxxx] / 6.0;
               }
            }
         }

         int blurR = m - 1;
         byte[] padded = new byte[324];
         Arrays.fill(padded, (byte)-1);

         for (int lz = -1; lz <= 16; lz++) {
            for (int lx = -1; lx <= 16; lx++) {
               int ci = lx + m;
               int cj = lz + m;
               if (has[cj * span + ci]) {
                  double sx = 0.0;
                  double sz = 0.0;

                  for (int dj = -blurR; dj <= blurR; dj++) {
                     for (int di = -blurR; di <= blurR; di++) {
                        int idxxxx = (cj + dj) * span + ci + di;
                        if (has[idxxxx]) {
                           double wgt = 1.0 / (1.0 + (double)(di * di) + (double)(dj * dj));
                           sx += rawX[idxxxx] * wgt;
                           sz += rawZ[idxxxx] * wgt;
                        }
                     }
                  }

                  double dirX = sx;
                  double dirZ = sz;
                  if (sx * sx + sz * sz <= 1.0E-9) {
                     dirX = rawX[cj * span + ci];
                     dirZ = rawZ[cj * span + ci];
                  }

                  int mi = cj * span + ci;
                  double fan = mouthW[mi];
                  if (fan > 0.0) {
                     double turn;
                     if (mouthX[mi] == 0.0 && mouthZ[mi] == 0.0) {
                        double px = -dirZ;
                        int runP = perpRun(core, span, ci, cj, px, dirX);
                        int runM = perpRun(core, span, ci, cj, -px, -dirX);
                        turn = 0.9 * ((double)(runM - runP) / (double)Math.max(1, runM + runP));
                     } else {
                        turn = Math.atan2(dirX * mouthZ[mi] - dirZ * mouthX[mi], dirX * mouthX[mi] + dirZ * mouthZ[mi]);
                     }

                     turn = Math.max(-Math.PI / 4, Math.min(Math.PI / 4, turn * fan));
                     double cs = Math.cos(turn);
                     double sn = Math.sin(turn);
                     double txx = dirX * cs - dirZ * sn;
                     double tz = dirX * sn + dirZ * cs;
                     double tl = Math.hypot(txx, tz);
                     if (tl > 1.0E-9) {
                        dirX = txx / tl;
                        dirZ = tz / tl;
                     }
                  }

                  double bnx = 0.0;
                  double bnz = 0.0;
                  int nonFlow = 0;

                  for (int dj = -8; dj <= 8; dj++) {
                     for (int dix = -8; dix <= 8; dix++) {
                        if ((dix != 0 || dj != 0) && !has[(cj + dj) * span + ci + dix]) {
                           double d = Math.sqrt((double)(dix * dix + dj * dj));
                           bnx += (double)dix / d;
                           bnz += (double)dj / d;
                           nonFlow++;
                        }
                     }
                  }

                  if (nonFlow > 0) {
                     double nlen = Math.hypot(bnx, bnz);
                     double asym = nlen / (double)nonFlow;
                     if (nlen > 1.0E-6 && asym > 0.001) {
                        double px = -bnz / nlen;
                        double pz = bnx / nlen;
                        if (px * dirX + pz * dirZ < 0.0) {
                           px = -px;
                           pz = -pz;
                        }

                        double w = Math.min(0.85, asym * 1.6) * (1.0 - fan);
                        double bx = (1.0 - w) * dirX + w * px;
                        double bz = (1.0 - w) * dirZ + w * pz;
                        double bl = Math.hypot(bx, bz);
                        if (bl > 1.0E-9) {
                           dirX = bx / bl;
                           dirZ = bz / bl;
                        }

                        int dci = ci + (int)Math.round(dirX * 4.0);
                        int dcj = cj + (int)Math.round(dirZ * 4.0);
                        if (dci >= 0 && dcj >= 0 && dci < span && dcj < span) {
                           double dWds = (double)(perpWidth(has, span, dci, dcj, dirX, dirZ) - perpWidth(has, span, ci, cj, dirX, dirZ)) / 4.0;
                           double flare = Math.max(-0.5, Math.min(0.5, 1.5 * dWds * asym));
                           double fx2 = dirX + flare * (bnx / nlen);
                           double fz2 = dirZ + flare * (bnz / nlen);
                           double fl2 = Math.hypot(fx2, fz2);
                           if (fl2 > 1.0E-9) {
                              dirX = fx2 / fl2;
                              dirZ = fz2 / fl2;
                           }
                        }
                     }
                  }

                  double edge = edgeDistance(has, span, ci, cj, 4);
                  if (!(edge < 4.0) || perpWidth(has, span, ci, cj, dirX, dirZ) < 10 || !(edge < wave((cx << 4) + lx, (cz << 4) + lz))) {
                     padded[lx + 1 + (lz + 1) * 18] = encode(dirX, dirZ);
                  }
               }
            }
         }

         byte[] grid = new byte[256];
         Arrays.fill(grid, (byte)-1);

         for (int lz = 0; lz < 16; lz++) {
            for (int lxx = 0; lxx < 16; lxx++) {
               byte own = padded[lxx + 1 + (lz + 1) * 18];
               if (own != -1) {
                  double sx = 0.0;
                  double sz = 0.0;

                  for (int dj = -1; dj <= 1; dj++) {
                     for (int dixx = -1; dixx <= 1; dixx++) {
                        byte b = padded[lxx + 1 + dixx + (lz + 1 + dj) * 18];
                        if (b != -1) {
                           double wgt = 1.0 / (1.0 + (double)(dixx * dixx) + (double)(dj * dj));
                           sx += LUT_COS[b & 255] * wgt;
                           sz += LUT_SIN[b & 255] * wgt;
                        }
                     }
                  }

                  grid[lxx + lz * 16] = sx * sx + sz * sz > 1.0E-9 ? encode(sx, sz) : own;
               }
            }
         }

         return grid;
      }
   }

   private static double bilinearRiver(float[] rqf, int qSpanX, int qSpanZ, int qBaseX, int qBaseZ, int wx, int wz) {
      double fx = ((double)wx - 1.5) / 4.0 - (double)qBaseX;
      double fz = ((double)wz - 1.5) / 4.0 - (double)qBaseZ;
      int i0 = (int)Math.floor(fx);
      int j0 = (int)Math.floor(fz);
      double tx = fx - (double)i0;
      double tz = fz - (double)j0;
      double a = rqAt(rqf, qSpanX, qSpanZ, i0, j0) + (rqAt(rqf, qSpanX, qSpanZ, i0 + 1, j0) - rqAt(rqf, qSpanX, qSpanZ, i0, j0)) * tx;
      double b = rqAt(rqf, qSpanX, qSpanZ, i0, j0 + 1) + (rqAt(rqf, qSpanX, qSpanZ, i0 + 1, j0 + 1) - rqAt(rqf, qSpanX, qSpanZ, i0, j0 + 1)) * tx;
      return a + (b - a) * tz;
   }

   private static double rqAt(float[] rqf, int qSpanX, int qSpanZ, int qi, int qj) {
      return qi >= 0 && qj >= 0 && qi < qSpanX && qj < qSpanZ ? (double)rqf[qj * qSpanX + qi] : 0.0;
   }

   private static double edgeDistance(boolean[] has, int span, int ci, int cj, int r) {
      double best = (double)r;

      for (int dj = -r; dj <= r; dj++) {
         for (int di = -r; di <= r; di++) {
            if ((di != 0 || dj != 0) && !has[(cj + dj) * span + ci + di]) {
               double d = Math.sqrt((double)(di * di + dj * dj));
               if (d < best) {
                  best = d;
               }
            }
         }
      }

      return best;
   }

   private static int perpWidth(boolean[] has, int span, int ci, int cj, double dirX, double dirZ) {
      double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
      if (len < 1.0E-9) {
         return 0;
      } else {
         double px = -dirZ / len;
         double pz = dirX / len;
         return 1 + perpRun(has, span, ci, cj, px, pz) + perpRun(has, span, ci, cj, -px, -pz);
      }
   }

   private static int perpRun(boolean[] has, int span, int ci, int cj, double px, double pz) {
      int run = 0;

      for (int k = 1; k <= 10; k++) {
         int i = ci + (int)Math.round(px * (double)k);
         int j = cj + (int)Math.round(pz * (double)k);
         if (i < 0 || j < 0 || i >= span || j >= span) {
            return 10;
         }

         if (!has[j * span + i]) {
            return run;
         }

         run++;
      }

      return run;
   }

   private static double wave(int x, int z) {
      double s = Math.sin((double)x * 0.14 + (double)z * 0.05) + 0.7 * Math.sin((double)x * 0.045 - (double)z * 0.17);
      return (s / 1.7 + 1.0) * 0.5 * 4.0;
   }

   private static RawFlowTiles.Tile tileOrCompute(RiverEngine engine, int cx, int cz) {
      RawFlowTiles.Tile tile = RawFlowTiles.get(cx, cz);
      if (tile != null) {
         return tile;
      } else {
         FlowTrace.tilesComputed.incrementAndGet();
         byte[] dirX = new byte[256];
         byte[] dirZ = new byte[256];
         short[] surfaceY = new short[256];
         Arrays.fill(surfaceY, (short)-32768);
         int baseX = cx << 4;
         int baseZ = cz << 4;

         for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
               ColumnPlan p = engine.plan(baseX + dx, baseZ + dz);
               if (p.inChannel()) {
                  double fx = p.fluidFlowDirX();
                  double fz = p.fluidFlowDirZ();
                  double len = Math.sqrt(fx * fx + fz * fz);
                  if (!(len < 0.5)) {
                     int i = dx * 16 + dz;
                     dirX[i] = (byte)((int)Math.round(fx / len * 127.0));
                     dirZ[i] = (byte)((int)Math.round(fz / len * 127.0));
                     int ws = p.waterSurfaceY();
                     surfaceY[i] = ws >= -32768 && ws <= 32767 ? (short)ws : -32768;
                  }
               }
            }
         }

         tile = new RawFlowTiles.Tile(dirX, dirZ, surfaceY);
         RawFlowTiles.put(cx, cz, tile);
         return tile;
      }
   }

   private static RawFlowTiles.Tile tileFor(RawFlowTiles.Tile[] tiles, int baseChunkX, int baseChunkZ, int wx, int wz) {
      int tx = (wx >> 4) - baseChunkX;
      int tz = (wz >> 4) - baseChunkZ;
      return tx >= 0 && tx <= 2 && tz >= 0 && tz <= 2 ? tiles[tz * 3 + tx] : null;
   }

   public static byte[] peek(int cx, int cz) {
      return CACHE.get(key(cx, cz));
   }

   public static void invalidateArea(int minX, int minZ, int size) {
      int c0x = minX >> 4;
      int c0z = minZ >> 4;
      int c1x = minX + size - 1 >> 4;
      int c1z = minZ + size - 1 >> 4;

      for (int cx = c0x; cx <= c1x; cx++) {
         for (int cz = c0z; cz <= c1z; cz++) {
            CACHE.remove(key(cx, cz));
         }
      }
   }

   public static void clear() {
      RTFFlowUnderlay.clearServer();
      CACHE.clear();
      ORDER.clear();
   }

   static {
      for (int b = 0; b < 256; b++) {
         LUT_COS[b] = Math.cos((double)b / 255.0 * (Math.PI * 2));
         LUT_SIN[b] = Math.sin((double)b / 255.0 * (Math.PI * 2));
      }

      EMPTY = new byte[0];
      CACHE = new ConcurrentHashMap<>();
      ORDER = new ConcurrentLinkedQueue<>();
      INCOMPLETE = ConcurrentHashMap.newKeySet();
      STILL_TILE = makeStillTile();
   }
}
