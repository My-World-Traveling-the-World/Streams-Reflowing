package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** 区块截面与普查保留原命令及采样参数，不参与正常区块队列。 */
public final class StreamColumnDiagnostics {
    private static final AtomicBoolean S = new AtomicBoolean(false);

    private StreamColumnDiagnostics() {}

    public static void diagnoseColumn(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        StreamColumnDiagnostics.a(riverEngine, serverLevel, n, n2);
        StreamColumnDiagnostics.a(riverEngine, serverLevel, new ChunkPos(new BlockPos(n, 0, n2)));
    }

    public static void traceColumn(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        int n3 = n;
        int n4 = n2;
        ColumnPlan columnPlan = riverEngine.plan(n, n2);
        if (!columnPlan.inCorridor()) {
            double d = Double.MAX_VALUE;
            for (int i = -10; i <= 10; ++i) {
                for (int j = -10; j <= 10; ++j) {
                    ColumnPlan columnPlan2 = riverEngine.plan(n + j, n2 + i);
                    if (!columnPlan2.inCorridor() || !(columnPlan2.distance() < d)) continue;
                    d = columnPlan2.distance();
                    columnPlan = columnPlan2;
                    n3 = n + j;
                    n4 = n2 + i;
                }
            }
        }
        if (!columnPlan.inCorridor()) {
            Constants.LOG.info("=== STREAM TRACE: no stream corridor within 10 blocks of ({},{}) ===", (Object)n, (Object)n2);
            return;
        }
        int n5 = riverEngine.config().seaLevel();
        int n6 = columnPlan.waterSurfaceY();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        int n7 = chunkGenerator.getBaseHeight(n3, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%n=== STREAM TRACE (%d,%d) ===%n", n3, n4));
        stringBuilder.append(String.format("plan: rawGradeY=%.2f  waterSurfaceY=%d  ground=%d  perch(water-ground)=%+d%n", columnPlan.gradeY(), n6, n7, n6 - n7));
        stringBuilder.append(String.format("  sea floor (seaLevel-1) = %d  ->  %s%n", n5 - 1, n6 <= n5 - 1 ? "water is AT/below the sea floor -- the floor MAY be lifting it (check ground vs grade below)" : "water is ABOVE the sea floor -- floor is NOT the cause; the grade itself sits high here"));
        stringBuilder.append(String.format("  distance=%.1f halfWidth=%.1f source=%b terminal=%b inChannel=%b%n", columnPlan.distance(), columnPlan.width(), columnPlan.source(), columnPlan.terminal(), columnPlan.inChannel()));
        int n8 = columnPlan.bedY();
        int n9 = 24;
        double d = columnPlan.distToOutlet();
        boolean bl = n7 < n6;
        String string = "-";
        if (n7 > n8) {
            double d2 = d >= 8.988465674311579E307 ? 1.0 : Math.min(1.0, d / (double)n9);
            double d3 = d2 * d2 * (3.0 - 2.0 * d2);
            string = String.valueOf((int)Math.round((double)n7 + (double)(n8 - n7) * d3));
        }
        stringBuilder.append(String.format("  MOUTH NOTCH(offshore): deepBed=%d ambientFloor=%d distToOutlet=%s  submergedHere=%b  notchBed=%s%n", n8, n7, d >= 8.988465674311579E307 ? "none" : String.format("%.1f", d), bl, string));
        stringBuilder.append("longitudinal (follow the channel; ground is the floor beneath the centreline):\n");
        List<String> list = StreamColumnDiagnostics.a(riverEngine, serverLevel, n3, n4, -1);
        Collections.reverse(list);
        for (String string2 : list) {
            stringBuilder.append(string2).append('\n');
        }
        stringBuilder.append(String.format("   >> HERE (%d,%d): rawGrade=%.2f water=%d ground=%d perch=%+d%n", n3, n4, columnPlan.gradeY(), n6, n7, n6 - n7));
        for (String string3 : StreamColumnDiagnostics.a(riverEngine, serverLevel, n3, n4, 1)) {
            stringBuilder.append(string3).append('\n');
        }
        Constants.LOG.info(stringBuilder.toString());
    }

    private static List<String> a(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        ArrayList<String> arrayList = new ArrayList<String>();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        for (int i = 1; i <= 30; ++i) {
            int n4;
            int n5;
            ColumnPlan columnPlan;
            ColumnPlan columnPlan2 = riverEngine.plan((int)Math.floor(d), (int)Math.floor(d2));
            double d3 = Math.hypot(columnPlan2.flowDirX(), columnPlan2.flowDirZ());
            if (!columnPlan2.inCorridor() || d3 < 1.0E-6 || !(columnPlan = riverEngine.plan(n5 = (int)Math.floor(d += (double)n3 * (columnPlan2.flowDirX() / d3) * 2.0), n4 = (int)Math.floor(d2 += (double)n3 * (columnPlan2.flowDirZ() / d3) * 2.0))).inCorridor()) break;
            int n6 = chunkGenerator.getBaseHeight(n5, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
            int n7 = columnPlan.waterSurfaceY();
            arrayList.add(String.format("   %s%-2d (%d,%d): rawGrade=%6.2f water=%d ground=%d perch=%+d", n3 < 0 ? "up" : "dn", i, n5, n4, columnPlan.gradeY(), n7, n6, n7 - n6));
        }
        return arrayList;
    }

    private static void a(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        int n3;
        int n4;
        int n5;
        int n6;
        int n7;
        int n8 = n;
        int n9 = n2;
        ColumnPlan columnPlan = riverEngine.plan(n, n2);
        if (!columnPlan.inCorridor()) {
            double d = Double.MAX_VALUE;
            for (int i = -10; i <= 10; ++i) {
                for (n7 = -10; n7 <= 10; ++n7) {
                    ColumnPlan columnPlan2 = riverEngine.plan(n + n7, n2 + i);
                    if (!columnPlan2.inCorridor() || !(columnPlan2.distance() < d)) continue;
                    d = columnPlan2.distance();
                    columnPlan = columnPlan2;
                    n8 = n + n7;
                    n9 = n2 + i;
                }
            }
        }
        if (!columnPlan.inCorridor()) {
            Constants.LOG.info("=== PERCH DIAG: no stream corridor within 10 blocks of ({},{}) ===", (Object)n, (Object)n2);
            return;
        }
        int n10 = columnPlan.waterSurfaceY();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        n7 = chunkGenerator.getBaseHeight(n8, n9, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
        int n11 = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n8, n9) - 1;
        int n12 = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, n8, n9) - 1;
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        StringBuilder stringBuilder = new StringBuilder();
        Object object = "n/a";
        Object object2 = "n/a";
        if (d > 1.0E-6) {
            double d2 = -columnPlan.flowDirZ() / d;
            double d3 = columnPlan.flowDirX() / d;
            for (int i = -1; i <= 1; i += 2) {
                Object object3;
                StringBuilder stringBuilder2 = new StringBuilder();
                int n13 = -1;
                for (int j = 1; j <= 16; ++j) {
                    int n14 = n8 + (int)Math.round(d2 * (double)j * (double)i);
                    int n15 = n9 + (int)Math.round(d3 * (double)j * (double)i);
                    n6 = chunkGenerator.getBaseHeight(n14, n15, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                    stringBuilder2.append(String.format("%4d", n6));
                    if (n13 >= 0 || n6 < n10) continue;
                    n13 = j;
                }
                String string = i < 0 ? "L" : "R";
                stringBuilder.append("  ").append(string).append("(out->):").append((CharSequence)stringBuilder2).append('\n');
                Object object4 = object3 = n13 < 0 ? "NEVER (>= grade)" : "rises to grade at d=" + n13;
                if (i < 0) {
                    object = object3;
                    continue;
                }
                object2 = object3;
            }
        }
        boolean bl = !"NEVER (>= grade)".equals(object) && !"NEVER (>= grade)".equals(object2) && d > 1.0E-6;
        StringBuilder stringBuilder3 = new StringBuilder();
        boolean bl2 = false;
        boolean bl3 = false;
        if (d > 1.0E-6) {
            double d4 = -columnPlan.flowDirZ() / d;
            double d5 = columnPlan.flowDirX() / d;
            for (int i = -1; i <= 1; i += 2) {
                StringBuilder stringBuilder4 = new StringBuilder();
                for (n6 = 1; n6 <= 12; ++n6) {
                    n5 = riverEngine.plan(n8 + (int)Math.round(d4 * (double)n6 * (double)i), n9 + (int)Math.round(d5 * (double)n6 * (double)i)).distance() <= 2.0 ? 1 : 0;
                    stringBuilder4.append(n5 != 0 ? " C" : " .");
                    if (n5 == 0) continue;
                    if (i < 0) {
                        bl3 = true;
                        continue;
                    }
                    bl2 = true;
                }
                stringBuilder3.append("  ").append(i < 0 ? "L" : "R").append("(out->):").append((CharSequence)stringBuilder4).append('\n');
            }
        }
        if (d > 1.0E-6) {
            double d6 = -columnPlan.flowDirZ() / d;
            double d7 = columnPlan.flowDirX() / d;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("containment walk (d out each side; grade=col waterSurfaceY, g=pre-carve ground, T=terminal[disables cup+berm], ch=inChannel):\n");
            stringBuilder5.append(String.format("    d= 0 (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%n", n8, n9, columnPlan.waterSurfaceY(), n7, columnPlan.waterSurfaceY() - n7, columnPlan.terminal(), columnPlan.inChannel(), columnPlan.source(), columnPlan.distance()));
            for (int i = -1; i <= 1; i += 2) {
                stringBuilder5.append("  ").append(i < 0 ? "L (rim/mountain side):" : "R (open/sea side):").append('\n');
                for (n6 = 1; n6 <= 12; ++n6) {
                    n5 = n8 + (int)Math.round(d6 * (double)n6 * (double)i);
                    n4 = n9 + (int)Math.round(d7 * (double)n6 * (double)i);
                    ColumnPlan columnPlan3 = riverEngine.plan(n5, n4);
                    n3 = chunkGenerator.getBaseHeight(n5, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                    stringBuilder5.append(String.format("    d=%2d (%d,%d) grade=%d g=%d perch=%+d  T=%b ch=%b src=%b dist=%.1f%s%n", n6, n5, n4, columnPlan3.waterSurfaceY(), n3, columnPlan3.waterSurfaceY() - n3, columnPlan3.terminal(), columnPlan3.inChannel(), columnPlan3.source(), columnPlan3.distance(), columnPlan3.inCorridor() ? "" : " [OUT-OF-CORRIDOR]"));
                }
            }
            Constants.LOG.info(stringBuilder5.toString());
        }
        Lake lake = riverEngine.lakeAt(n8, n9);
        int n16 = -1;
        int n17 = Integer.MIN_VALUE;
        for (int i = 1; i <= 24 && n16 < 0; ++i) {
            for (int j = -i; j <= i && n16 < 0; ++j) {
                for (int k = -i; k <= i && n16 < 0; ++k) {
                    Lake lake2;
                    if (Math.max(Math.abs(k), Math.abs(j)) != i || (lake2 = riverEngine.lakeAt(n8 + k, n9 + j)) == null) continue;
                    n16 = i;
                    n17 = lake2.level();
                }
            }
        }
        String string = "covering this column: " + (String)(lake != null ? "YES (level=" + lake.level() + ")" : "none") + ";  nearest Lake object within 24 blocks: " + (String)(n16 >= 0 ? "d=" + n16 + " level=" + n17 : "NONE (this water is a hollow-pool / pond, not a basin Lake)");
        Constants.LOG.info("=== PERCH DIAG column ({},{}) ===\nLAKE object {}\ngrade(waterSurfaceY)={}  rawGradeY={}  bedY={}  GROUND(pre-carve)={}  WALL(post-carve)={}  waterTop(placed)={}\nperch? grade-ground = {}   (>0 = water sits ABOVE ground here)\ninChannel={} distance={} halfWidth={} slope={} upstreamSlope={} terminal={} source={}\nperpendicular ground profile (each col = +1 block out from channel, to 16):\n{}left rim: {}   right rim: {}\n=> hollowEnclosed would be {} (both sides must rise to grade within 16) -> {}\nperpendicular STREAM scan (engine; C=stream centerline within 2 blocks, .=none, out to 12):\n{}this col inAnyChannel={}  stream on L-side={} R-side={}  => bermYieldsToNeighbor(both sides)={}", n8, n9, string, n10, String.format("%.1f", columnPlan.gradeY()), columnPlan.bedY(), n7, n11, n12, n10 - n7, columnPlan.inChannel(), String.format("%.1f", columnPlan.distance()), String.format("%.1f", columnPlan.width()), String.format("%.3f", columnPlan.slope()), String.format("%.3f", columnPlan.upstreamSlope()), columnPlan.terminal(), columnPlan.source(), stringBuilder, object, object2, bl, bl ? "POOLS to grade" : "falls through to berm/bank carve (stays perched if grade>ground)", stringBuilder3, riverEngine.inAnyChannel(n8, n9), bl3, bl2, bl2 && bl3);
        if (d > 1.0E-6) {
            Object object5;
            int n18;
            int n19;
            int n20;
            double d8 = -columnPlan.flowDirZ() / d;
            double d9 = columnPlan.flowDirX() / d;
            n4 = 16;
            int n21 = n10 + 2;
            n3 = n10 - 34;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("     ").append(" ".repeat(n4)).append("v  centre (d=0), L=-d out to left, R=+d out to right\n");
            for (n20 = n21; n20 >= n3; --n20) {
                stringBuilder6.append(String.format("y%3d ", n20));
                for (int i = -n4; i <= n4; ++i) {
                    n19 = n8 + (int)Math.round(d8 * (double)i);
                    object5 = serverLevel.getFluidState(mutableBlockPos.set(n19, n20, n18 = n9 + (int)Math.round(d9 * (double)i)));
                    int n22 = !((FluidState)object5).isEmpty() && ((FluidState)object5).is(FluidTags.WATER) ? (((FluidState)object5).getType() == Fluids.WATER || ((FluidState)object5).getType() == Fluids.FLOWING_WATER ? 111 : 126) : (serverLevel.getBlockState(mutableBlockPos).isAir() ? 32 : 35);
                    stringBuilder6.append((char)n22);
                }
                stringBuilder6.append('\n');
            }
            n20 = 48;
            StringBuilder stringBuilder7 = new StringBuilder();
            for (n19 = -2; n19 <= 2; ++n19) {
                for (n18 = -2; n18 <= 2; ++n18) {
                    object5 = riverEngine.plateAt(n8 + n18 * n20, n9 + n19 * n20);
                    stringBuilder7.append(String.format(" %3d,%-3d", ((PlateKey)object5).cx(), ((PlateKey)object5).cz()));
                }
                stringBuilder7.append('\n');
            }
            Constants.LOG.info("cross-section perpendicular to flow (post-carve blocks; ~=stream o=vanilla-water #=solid):\n{}plate (cx,cz) on a {}-block grid, centre row/col = this column (differing ids => a plate seam):\n{}", stringBuilder6, n20, stringBuilder7);
        }
    }

    private static void a(RiverEngine riverEngine, ServerLevel serverLevel, ChunkPos chunkPos) {
        int n = chunkPos.getMinBlockX();
        int n2 = chunkPos.getMinBlockZ();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder();
        StringBuilder stringBuilder3 = new StringBuilder();
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n3 = n + j;
                int n4 = n2 + i;
                stringBuilder2.append(String.format("%4d", chunkGenerator.getBaseHeight(n3, n4, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1));
                stringBuilder3.append(String.format("%4d", serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n3, n4) - 1));
                ColumnPlan columnPlan = riverEngine.plan(n3, n4);
                stringBuilder.append(columnPlan.inCorridor() ? String.format("%4d", columnPlan.waterSurfaceY()) : "   .");
            }
            stringBuilder.append('\n');
            stringBuilder2.append('\n');
            stringBuilder3.append('\n');
        }
        Constants.LOG.info("=== PERCH DIAG chunk ({},{}) X[{}..{}] Z[{}..{}] (rows=+Z down, cols=+X right) ===\nGRADE (engine waterSurfaceY; . = not in corridor):\n{}\nGROUND (pre-carve OCEAN_FLOOR_WG, ALL cols):\n{}\nWALL (post-carve OCEAN_FLOOR, ALL cols -- pool/berm shows here):\n{}", chunkPos.x, chunkPos.z, n, n + 15, n2, n2 + 15, stringBuilder, stringBuilder2, stringBuilder3);
    }

    public static boolean startCensus(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        if (n3 <= 0 || !S.compareAndSet(false, true)) {
            return false;
        }
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        Thread thread = new Thread(() -> {
            try {
                StreamColumnDiagnostics.a(riverEngine, chunkGenerator, randomState, serverLevel, n, n2, n3);
            }
            finally {
                S.set(false);
            }
        }, "streams-perch-census");
        thread.setDaemon(true);
        thread.setPriority(1);
        thread.start();
        Constants.LOG.info("=== PERCH CENSUS started: {}x{} blocks around ({},{}); building networks, please wait ===", n3 * 2, n3 * 2, n, n2);
        return true;
    }

    private static void a(RiverEngine riverEngine, ChunkGenerator chunkGenerator, RandomState randomState, ServerLevel serverLevel, int n, int n2, int n3) {
        try {
            int n4;
            int n5 = riverEngine.config().seaLevel();
            int[] nArray = new int[24];
            int n6 = 0;
            int n7 = 0;
            int n8 = 0;
            int n9 = 0;
            int n10 = Integer.MIN_VALUE;
            int n11 = n;
            int n12 = n2;
            int n13 = Integer.MAX_VALUE;
            int n14 = Integer.MIN_VALUE;
            int n15 = Integer.MAX_VALUE;
            int n16 = Integer.MIN_VALUE;
            int n17 = 0;
            StringBuilder stringBuilder = new StringBuilder();
            int n18 = 0;
            for (int i = -n3; i <= n3; ++i) {
                for (n4 = -n3; n4 <= n3; ++n4) {
                    int n19;
                    double d;
                    int n20 = n + n4;
                    int n21 = n2 + i;
                    ColumnPlan columnPlan = riverEngine.plan(n20, n21);
                    if (!columnPlan.inChannel() || (d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ())) < 1.0E-6) continue;
                    double d2 = -columnPlan.flowDirZ() / d;
                    double d3 = columnPlan.flowDirX() / d;
                    double d4 = columnPlan.width() + 2.0;
                    int n22 = chunkGenerator.getBaseHeight((int)Math.round((double)n20 + d2 * d4), (int)Math.round((double)n21 + d3 * d4), Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                    int n23 = Math.min(n22, n19 = chunkGenerator.getBaseHeight((int)Math.round((double)n20 - d2 * d4), (int)Math.round((double)n21 - d3 * d4), Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1);
                    if (n23 < n5) {
                        ++n7;
                        continue;
                    }
                    ++n6;
                    int n24 = columnPlan.waterSurfaceY() - n23;
                    if (n24 > n10) {
                        n10 = n24;
                        n11 = n20;
                        n12 = n21;
                    }
                    if (n24 >= 1) {
                        ++n8;
                    }
                    if (n24 >= 4) {
                        ++n9;
                    }
                    if (n24 >= 13) {
                        ++n17;
                        n13 = Math.min(n13, n20);
                        n14 = Math.max(n14, n20);
                        n15 = Math.min(n15, n21);
                        n16 = Math.max(n16, n21);
                        if (n18 < 16 && (n4 + i & 7) == 0) {
                            int n25 = chunkGenerator.getBaseHeight(n20, n21, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                            stringBuilder.append(String.format("    (%d,%d) grade=%d colGround=%d bankL=%d bankR=%d perch=%d%n", n20, n21, columnPlan.waterSurfaceY(), n25, n19, n22, n24));
                            ++n18;
                        }
                    }
                    int n26 = Math.max(0, Math.min(nArray.length - 1, n24));
                    nArray[n26] = nArray[n26] + 1;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(String.format("%n=== PERCH CENSUS COMPLETE: %dx%d blocks around (%d,%d) ===%n", n3 * 2, n3 * 2, n, n2));
            stringBuilder2.append(String.format("inland channel columns scanned: %d (excluded %d coastal/water-meeting)%n", n6, n7));
            if (n6 == 0) {
                stringBuilder2.append("  (no stream channels in this area -- try a larger radius or a different spawn)\n");
            } else {
                stringBuilder2.append(String.format("perched (water above lower bank >= 1): %d (%.1f%%)%n", n8, 100.0 * (double)n8 / (double)n6));
                stringBuilder2.append(String.format("BADLY perched (>= 4, a raised aqueduct): %d (%.1f%%)%n", n9, 100.0 * (double)n9 / (double)n6));
                stringBuilder2.append(String.format("max perch: %d blocks, at (%d,%d)%n", n10, n11, n12));
                stringBuilder2.append("histogram (perch height in blocks : column count):\n");
                for (n4 = 0; n4 < nArray.length; ++n4) {
                    if (nArray[n4] <= 0) continue;
                    stringBuilder2.append(String.format("  %2d%s : %d%n", n4, n4 == nArray.length - 1 ? "+" : " ", nArray[n4]));
                }
                if (n17 > 0) {
                    stringBuilder2.append(String.format("badly-perched (>=13) cluster: %d cols in X[%d..%d] Z[%d..%d] (span %dx%d)%n", n17, n13, n14, n15, n16, n14 - n13, n16 - n15));
                    stringBuilder2.append("samples (grade vs column ground vs the two banks):\n");
                    stringBuilder2.append((CharSequence)stringBuilder);
                }
            }
            Constants.LOG.info(stringBuilder2.toString());
        }
        catch (Throwable throwable) {
            Constants.LOG.warn("Perch census failed", throwable);
        }
    }
}
