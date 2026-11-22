package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.RiverSegment;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.regionprep.FormSampler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** 河段测量与链路追踪只由诊断入口触发，避免预取调度承担扫描职责。 */
public final class StreamChannelDiagnostics {
    private static final int[] Q = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] R = {0, 1, 1, 1, 0, -1, -1, -1};

    private StreamChannelDiagnostics() {}

    public static void biomeBench(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        int n3 = (int)Math.round(riverEngine.config().plateScale());
        int n4 = 4 * n3;
        int n5 = 8;
        RiverFlowField.BiomeClass biomeClass = RiverFlowFields.classifierFor(serverLevel);
        Thread thread = new Thread(() -> {
            int n6 = n4 / 2;
            long l = 0L;
            for (int i = n2 - n6; i < n2 - n6 + n5 * 48; i += n5) {
                for (int j = n - n6; j < n - n6 + n5 * 48; j += n5) {
                    l += (long)biomeClass.classify(j, i);
                }
            }
            long l2 = System.nanoTime();
            long l3 = 0L;
            long l4 = 0L;
            for (int i = n2 - n6; i <= n2 + n6; i += n5) {
                for (int j = n - n6; j <= n + n6; j += n5) {
                    if (biomeClass.classify(j, i) == 2) {
                        ++l4;
                    }
                    ++l3;
                }
            }
            double d = (double)(System.nanoTime() - l2) / 1000000.0;
            double d2 = d * 1000.0 / (double)Math.max(1L, l3);
            Constants.LOG.info("[SR-BIOMEBENCH] 4x plateScale tile {}x{} (plateScale={}), spacing {}: {} getNoiseBiome samples in {} ms = {} us/call, {}% ocean [warm {}]", n4, n4, n3, n5, l3, String.format("%.0f", d), String.format("%.3f", d2), String.format("%.1f", 100.0 * (double)l4 / (double)Math.max(1L, l3)), l & 1L);
        }, "streamsreflowing-biomebench");
        thread.setDaemon(true);
        thread.start();
    }

    public static void autoTrace(MinecraftServer minecraftServer, int n, int n2) {
        int n3;
        String[] stringArray;
        ServerLevel serverLevel = minecraftServer.overworld();
        int n4 = n >> 4;
        int n5 = n2 >> 4;
        Constants.LOG.info("[SR-AUTOTRACE] target=({},{}) chunk=({},{}) -- force-generating a 5x5 chunk block (the precise plate region from the diagnostics); nothing else is loaded", n, n2, n4, n5);
        long l = FormSampler.keptIr.sum();
        long l2 = FormSampler.handedBack.sum();
        long l3 = System.nanoTime();
        int n6 = 0;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                serverLevel.getChunk(n4 + i, n5 + j);
                ++n6;
            }
        }
        long l4 = (System.nanoTime() - l3) / 1000000L;
        long l5 = FormSampler.keptIr.sum() - l;
        long l6 = FormSampler.handedBack.sum() - l2;
        Constants.LOG.info("[SR-AUTOTRACE] generated {} chunks in {} ms; dumping placed-column diagnostics", (Object)n6, (Object)l4);
        Constants.LOG.info("[SR-AUTOTRACE] height queries through the walk during that build: {} served, {} handed to the host, {} total{}", l5, l6, l5 + l6, l5 + l6 == 0L ? " (zero -- either the walk is off, or this build asks it for nothing)" : String.format(Locale.ROOT, "; %.1f us each if they owned the whole %d ms", (double)l4 * 1000.0 / (double)(l5 + l6), l4));
        RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
        for (int i = -24; i <= 24; i += 4) {
            StreamColumnDiagnostics.diagnoseColumn(riverEngine, serverLevel, n, n2 + i);
        }
        StreamColumnDiagnostics.traceColumn(riverEngine, serverLevel, n, n2);
        StreamChannelDiagnostics.superTrace(riverEngine, serverLevel, n, n2);
        StreamChannelDiagnostics.spoutScan(riverEngine, serverLevel, n, n2, 768);
        String string = System.getProperty("streamsreflowing.sweep");
        if (string != null && !string.isBlank()) {
            stringArray = string.split(";");
            int n7 = stringArray.length;
            for (n3 = 0; n3 < n7; ++n3) {
                String string2 = stringArray[n3];
                String[] stringArray2 = string2.split(",");
                try {
                    int n8 = Integer.parseInt(stringArray2[0].trim());
                    int n9 = Integer.parseInt(stringArray2[1].trim());
                    for (int i = -1; i <= 1; ++i) {
                        for (int j = -1; j <= 1; ++j) {
                            serverLevel.getChunk((n8 >> 4) + i, (n9 >> 4) + j);
                        }
                    }
                    StreamColumnDiagnostics.diagnoseColumn(riverEngine, serverLevel, n8, n9);
                    StreamColumnDiagnostics.traceColumn(riverEngine, serverLevel, n8, n9);
                    StreamChannelDiagnostics.superTrace(riverEngine, serverLevel, n8, n9);
                    StreamChannelDiagnostics.spoutScan(riverEngine, serverLevel, n8, n9, 768);
                    continue;
                }
                catch (RuntimeException runtimeException) {
                    Constants.LOG.warn("[SR-AUTOTRACE] bad sweep target '{}'", (Object)string2);
                }
            }
        }
        String string3 = System.getProperty("streamsreflowing.monoaudit");
        if (string3 != null && !string3.isBlank()) {
            String[] stringArray3 = string3.split(",");
            try {
                n3 = Integer.parseInt(stringArray3[0].trim());
                int n10 = Integer.parseInt(stringArray3[1].trim());
                int n11 = stringArray3.length >= 3 ? Integer.parseInt(stringArray3[2].trim()) : 96;
                StreamChannelDiagnostics.monoAudit(riverEngine, serverLevel, n3, n10, n11);
            }
            catch (RuntimeException runtimeException) {
                Constants.LOG.warn("[SR-MONOAUDIT] bad streamsreflowing.monoaudit '{}'", (Object)string3, (Object)runtimeException);
            }
        }
        Constants.LOG.info("[SR-AUTOTRACE] diagnostics complete; halting server");
        minecraftServer.halt(false);
    }

    public static void monoAudit(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        int n11 = n3 * 2 + 1;
        int[] nArray = new int[n11 * n11];
        double[] dArray = new double[n11 * n11];
        double[] dArray2 = new double[n11 * n11];
        int[] nArray2 = new int[n11 * n11];
        Arrays.fill(nArray, Integer.MIN_VALUE);
        Arrays.fill(nArray2, Integer.MIN_VALUE);
        PlateKey plateKey = riverEngine.plateAt(n, n2);
        int n12 = 32;
        for (n10 = -n3; n10 <= n3; ++n10) {
            for (int i = -n3; i <= n3; ++i) {
                int n13 = n + i;
                n9 = n2 + n10;
                if (!riverEngine.plateAt(n13, n9).equals(plateKey) || !riverEngine.plateAt(n13 - n12, n9).equals(plateKey) || !riverEngine.plateAt(n13 + n12, n9).equals(plateKey) || !riverEngine.plateAt(n13, n9 - n12).equals(plateKey) || !riverEngine.plateAt(n13, n9 + n12).equals(plateKey)) continue;
                n8 = (n10 + n3) * n11 + (i + n3);
                ColumnPlan columnPlan = riverEngine.plan(n13, n9);
                n6 = n7 = chunkGenerator.getBaseHeight(n13, n9, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                n5 = Integer.MIN_VALUE;
                if (columnPlan.inCorridor()) {
                    double d;
                    int n14;
                    n5 = columnPlan.waterSurfaceY();
                    if (columnPlan.distToOutlet() <= 24.0 && (n4 = RiverCarverFeature.nearbyLakeLevel(riverEngine, n13, n9, serverLevel.getMinBuildHeight(), serverLevel.getMaxBuildHeight())) != Integer.MIN_VALUE) {
                        n5 = Math.max(n5, Math.min(n4, columnPlan.waterSurfaceY() + 4));
                    }
                    if ((n6 = Math.min(n7, n4 = columnPlan.waterSurfaceY() + (int)Math.round(columnPlan.bankBlend() * (double)Math.max(0, n7 - columnPlan.waterSurfaceY())))) < n7 && riverEngine.lakeAt(n13, n9) == null && columnPlan.distToOutlet() > 24.0 && (n14 = RiverCarverFeature.adjacentCoveringLakeLevel(riverEngine, n13, n9)) != Integer.MIN_VALUE && n7 > n14 && n6 <= n14) {
                        n6 = Math.min(n7, n14 + 1);
                    }
                    if ((d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ())) > 1.0E-6) {
                        dArray[n8] = columnPlan.flowDirX() / d;
                        dArray2[n8] = columnPlan.flowDirZ() / d;
                    }
                }
                if ((n4 = riverEngine.lakeSurfaceAt(n13, n9)) != Integer.MIN_VALUE && n4 > n5) {
                    n5 = n4;
                    dArray[n8] = 0.0;
                    dArray2[n8] = 0.0;
                }
                nArray[n8] = n5;
                nArray2[n8] = n6;
            }
        }
        n10 = 0;
        int[] nArray3 = new int[33];
        StringBuilder stringBuilder = new StringBuilder();
        n9 = 0;
        for (n8 = -n3; n8 <= n3; ++n8) {
            for (int i = -n3; i <= n3; ++i) {
                int n15;
                n7 = (n8 + n3) * n11 + (i + n3);
                n6 = nArray[n7];
                if (n6 == Integer.MIN_VALUE) continue;
                n5 = 0;
                n4 = 0;
                int n16 = 0;
                int n17 = 0;
                block4: for (n15 = 0; n15 < 8; ++n15) {
                    int n18 = Q[n15];
                    int n19 = R[n15];
                    double d = Math.sqrt(n18 * n18 + n19 * n19);
                    if (dArray[n7] * (double)n18 / d + dArray2[n7] * (double)n19 / d >= 0.5) continue;
                    int n20 = Integer.MIN_VALUE;
                    for (int j = 1; j <= 10; ++j) {
                        int n21 = i + n18 * j;
                        int n22 = n8 + n19 * j;
                        if (n21 < -n3 || n21 > n3 || n22 < -n3 || n22 > n3) continue block4;
                        int n23 = (n22 + n3) * n11 + (n21 + n3);
                        int n24 = nArray[n23];
                        int n25 = nArray2[n23];
                        if (n25 == Integer.MIN_VALUE) continue block4;
                        if (n24 != Integer.MIN_VALUE && j >= 2 && n6 - n24 >= 3 && n20 < n6) {
                            int n26 = n6 - n24;
                            if (n26 <= n5) continue block4;
                            n5 = n26;
                            n4 = n24;
                            n16 = n + n21;
                            n17 = n2 + n22;
                            continue block4;
                        }
                        if ((n20 = Math.max(n20, Math.max(n25, n24))) >= n6) continue block4;
                    }
                }
                if (n5 <= 0 || StreamChannelDiagnostics.a(riverEngine, n + i, n2 + n8, n4 + 1, (n15 = (int)Math.ceil(Math.hypot(n16 - (n + i), n17 - (n2 + n8)))) + 4)) continue;
                ++n10;
                int n27 = Math.min(32, n5);
                nArray3[n27] = nArray3[n27] + 1;
                if (n9 >= 20 && (n5 < 12 || n9 >= 90)) continue;
                stringBuilder.append(String.format(Locale.ROOT, "%n  water %d at (%d,%d) would pour onto water %d at (%d,%d) drop=%d", n6, n + i, n2 + n8, n4, n16, n17, n5));
                ++n9;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        for (int i = 3; i < nArray3.length; ++i) {
            if (nArray3[i] <= 0) continue;
            stringBuilder2.append(String.format("%n   drop %2d : %d columns", i, nArray3[i]));
        }
        Constants.LOG.info("[SR-MONOAUDIT] {}x{} around ({},{}): {} uphill-water columns (drop >= {} onto nearby lower water, no containing ground line between, cascades exempt).{}{}{}", n11, n11, n, n2, n10, 3, stringBuilder2, n10 > 0 ? "\n worst offenders:" : " CLEAN.", stringBuilder);
    }

    public static void segmentMap(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        record Row(double d, String text) {
        }
        int n4;
        StringBuilder stringBuilder = new StringBuilder(32768);
        stringBuilder.append(String.format(Locale.ROOT, "%n=== STREAM SEGMENT MAP @ (%d,%d) radius %d, step 2 ===%n", n, n2, n3));
        stringBuilder.append("  digit = channel water surface (last digit) | # = bank/berm corridor | L = lake | . = untouched | @ = here").append(String.format(Locale.ROOT, "%n", new Object[0]));
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        for (int i = n2 - n3; i <= n2 + n3; i += 2) {
            stringBuilder.append(String.format(Locale.ROOT, "  z%6d  ", i));
            for (int j = n - n3; j <= n + n3; j += 2) {
                if (Math.abs(j - n) <= 1 && Math.abs(i - n2) <= 1) {
                    n4 = 64;
                } else {
                    Lake lake = riverEngine.lakeAt(j, i);
                    ColumnPlan columnPlan = riverEngine.plan(j, i);
                    if (lake != null) {
                        n4 = 76;
                    } else if (columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inChannel()) {
                        int n5 = columnPlan.waterSurfaceY();
                        hashMap.merge(n5, 1, Integer::sum);
                        n4 = (char)(48 + Math.floorMod(n5, 10));
                    } else {
                        n4 = columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inCorridor() ? 35 : 46;
                    }
                }
                stringBuilder.append((char)n4);
            }
            stringBuilder.append(String.format(Locale.ROOT, "%n", new Object[0]));
        }
        stringBuilder.append(String.format(Locale.ROOT, "  x from %d (left) to %d (right)%n", n - n3, n + n3));
        ArrayList arrayList = new ArrayList(hashMap.keySet());
        Collections.sort(arrayList);
        stringBuilder.append("  channel water levels on the map: ");
        Iterator object = arrayList.iterator();
        while (object.hasNext()) {
            n4 = (Integer)object.next();
            stringBuilder.append(n4).append('(').append(hashMap.get(n4)).append("col) ");
        }
        stringBuilder.append(String.format(Locale.ROOT, "%n%n  segments within %d blocks (nearest first):%n", n3));
        ArrayList<Row> rows = new ArrayList<Row>();
        PlateKey[] plateKeyArray = riverEngine.platesNear(n, n2, n3);
        int n6 = plateKeyArray.length;
        for (int i = 0; i < n6; ++i) {
            PlateKey plateKey = plateKeyArray[i];
            RiverNetwork riverNetwork = riverEngine.networkFor(plateKey);
            List<RiverSegment> list = riverNetwork.segments();
            int[] nArray = NetworkTracer.chainIds(list);
            for (int j = 0; j < list.size(); ++j) {
                double d;
                RiverSegment riverSegment = list.get(j);
                double d2 = riverSegment.distanceAt((double)n + 0.5, (double)n2 + 0.5, d = riverSegment.clampParam((double)n + 0.5, (double)n2 + 0.5));
                if (!(d2 <= (double)n3)) continue;
                rows.add(new Row(d2, String.format(Locale.ROOT, "  d%5.1f | %s chain#%d seg %d | (%.0f,%.0f) y%.1f w%.1f -> (%.0f,%.0f) y%.1f w%.1f", d2, plateKey, nArray[j], j, riverSegment.x1(), riverSegment.z1(), riverSegment.y1(), riverSegment.w1(), riverSegment.x2(), riverSegment.z2(), riverSegment.y2(), riverSegment.w2())));
            }
        }
        rows.sort(Comparator.comparingDouble(Row::d));
        int n7 = Math.min(rows.size(), 80);
        for (n6 = 0; n6 < n7; ++n6) {
            stringBuilder.append(rows.get(n6).text()).append(String.format(Locale.ROOT, "%n", new Object[0]));
        }
        if (rows.size() > n7) {
            stringBuilder.append("  (+").append(rows.size() - n7).append(" more)").append(String.format(Locale.ROOT, "%n", new Object[0]));
        }
        Constants.LOG.info(stringBuilder.toString());
    }

    public static void superTrace(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2) {
        Object object;
        int n3;
        int n4;
        Object object3;
        int n5;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        RiverNetwork riverNetwork = riverEngine.networkAt(n, n2);
        RiverSample riverSample = riverNetwork.sample((double)n + 0.5, (double)n2 + 0.5, 64.0);
        if (riverSample == null || riverSample.segId() < 0) {
            Constants.LOG.info("=== STREAM SUPER TRACE: no stream within 64 blocks of ({},{}) in this plate ===", (Object)n, (Object)n2);
            return;
        }
        List<RiverSegment> list = riverNetwork.segments();
        int[] nArray = NetworkTracer.chainIds(list);
        int n6 = nArray[riverSample.segId()];
        HashMap<Long, List> hashMap = new HashMap<Long, List>();
        HashMap<Long, Integer> hashMap2 = new HashMap<Long, Integer>();
        HashMap<Long, Integer> hashMap3 = new HashMap<Long, Integer>();
        for (n5 = 0; n5 < list.size(); ++n5) {
            object3 = list.get(n5);
            hashMap.computeIfAbsent(StreamChannelDiagnostics.a(((RiverSegment)object3).x2(), ((RiverSegment)object3).z2()), l -> new ArrayList()).add(n5);
            hashMap3.put(StreamChannelDiagnostics.a(((RiverSegment)object3).x1(), ((RiverSegment)object3).z1()), n5);
            if (nArray[n5] != n6) continue;
            hashMap2.put(StreamChannelDiagnostics.a(((RiverSegment)object3).x1(), ((RiverSegment)object3).z1()), n5);
        }
        n5 = -1;
        for (Map.Entry object22 : hashMap2.entrySet()) {
            List n8 = (List)hashMap.get(object22.getKey());
            n4 = 0;
            if (n8 != null) {
                Iterator d = n8.iterator();
                while (d.hasNext()) {
                    int n7 = (Integer)d.next();
                    if (nArray[n7] != n6) continue;
                    n4 = 1;
                    break;
                }
            }
            if (n4 != 0) continue;
            n5 = (Integer)object22.getValue();
            break;
        }
        if (n5 < 0) {
            n5 = riverSample.segId();
        }
        object3 = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        ((StringBuilder)object3).append(String.format(Locale.ROOT, "%n=== STREAM SUPER TRACE @ (%d,%d): chain #%d, plate %s ===%n", n, n2, n6, riverNetwork.key()));
        ((StringBuilder)object3).append("  cols: pos | preGround | rawGrade | surface(plan) | placedWater | placedWall | lake | halfW | flags\n");
        int n8 = n5;
        n4 = 0;
        double d = 0.0;
        int n9 = Integer.MIN_VALUE;
        double d2 = Double.NaN;
        int n10 = 0;
        int n11 = n6;
        RiverSegment riverSegment = null;
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (n3 = 0; n3 < 100000 && n8 >= 0 && hashSet.add(n8); ++n3) {
            int n12;
            int lake;
            CharSequence charSequence;
            int n13;
            int string;
            Object n21;
            RiverSegment n26 = list.get(n8);
            d += Math.hypot(n26.x2() - n26.x1(), n26.z2() - n26.z1());
            object = (List)hashMap.get(StreamChannelDiagnostics.a(n26.x1(), n26.z1()));
            if (object != null) {
                Iterator junctionIterator = ((List)object).iterator();
                while (junctionIterator.hasNext()) {
                    string = (Integer)junctionIterator.next();
                    if (nArray[string] == nArray[n8]) continue;
                    RiverSegment n22 = list.get(string);
                    int columnPlan = StreamChannelDiagnostics.a(riverEngine, n26.y1(), n26.w1());
                    n13 = StreamChannelDiagnostics.a(riverEngine, n22.y2(), n22.w2());
                    string = StreamChannelDiagnostics.a(riverEngine, n26.y2(), n26.w2());
                    charSequence = new StringBuilder();
                    lake = string;
                    double n23 = 0.0;
                    for (n12 = 0; n12 < 400 && n23 < 16.0; ++n12) {
                        RiverSegment riverSegment2 = list.get(lake);
                        n23 += Math.hypot(riverSegment2.x2() - riverSegment2.x1(), riverSegment2.z2() - riverSegment2.z1());
                        if (n12 % 8 == 0) {
                            ((StringBuilder)charSequence).insert(0, StreamChannelDiagnostics.a(riverEngine, riverSegment2.y1(), riverSegment2.w1()) + " > ");
                        }
                        List list2 = (List)hashMap.get(StreamChannelDiagnostics.a(riverSegment2.x1(), riverSegment2.z1()));
                        int n14 = -1;
                        if (list2 != null) {
                            Iterator iterator = list2.iterator();
                            while (iterator.hasNext()) {
                                int n15 = (Integer)iterator.next();
                                if (nArray[n15] != nArray[string]) continue;
                                n14 = n15;
                                break;
                            }
                        }
                        if (n14 < 0) break;
                        lake = n14;
                    }
                    stringBuilder.append(String.format(Locale.ROOT, "%n  JUNCTION at (%d,%d): inflow chain #%d [%s%d] (grade %.2f w %.1f) joins chain #%d at %d (grade %.2f w %.1f) -> downstream %d%s", (int)n26.x1(), (int)n26.z1(), nArray[string], charSequence, n13, n22.y2(), n22.w2(), nArray[n8], columnPlan, n26.y1(), n26.w1(), string, string > Math.min(n13, columnPlan) ? "  << RISE (downstream above an inflow!)" : ""));
                }
            }
            if (d >= (double)n4 * 4.0) {
                int n16;
                int n27 = (int)Math.floor(n26.x1());
                string = (int)Math.floor(n26.z1());
                int n28 = chunkGenerator.getBaseHeight(n27, string, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                ColumnPlan columnPlan = riverEngine.plan(n27, string);
                n13 = columnPlan.inCorridor() ? columnPlan.waterSurfaceY() : StreamChannelDiagnostics.a(riverEngine, n26.y1(), n26.w1());
                String string2 = "-";
                charSequence = "-";
                if (serverLevel.hasChunk(n27 >> 4, string >> 4)) {
                    lake = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n27, string) - 1;
                    charSequence = String.valueOf(lake);
                    int n24 = -9999;
                    for (n16 = Math.max(lake, n13) + 4; n16 >= n13 - 12; --n16) {
                        if (serverLevel.getBlockState(new BlockPos(n27, n16, string)).getFluidState().isEmpty()) continue;
                        n24 = n16;
                        break;
                    }
                    string2 = n24 == -9999 ? "-" : String.valueOf(n24);
                }
                Lake lake2 = riverEngine.lakeAt(n27, string);
                int n17 = RiverCarverFeature.nearbyLakeLevel(riverEngine, n27, string, serverLevel.getMinBuildHeight(), serverLevel.getMaxBuildHeight());
                n16 = !Double.isNaN(d2) && n26.y1() > d2 + 0.01 ? 1 : 0;
                int n18 = n12 = n9 != Integer.MIN_VALUE && n13 > n9 ? 1 : 0;
                if (n16 != 0 || n12 != 0) {
                    ++n10;
                }
                Object[] objectArray = new Object[13];
                objectArray[0] = n27;
                objectArray[1] = string;
                objectArray[2] = n28;
                objectArray[3] = n26.y1();
                objectArray[4] = n13;
                objectArray[5] = string2;
                objectArray[6] = charSequence;
                objectArray[7] = lake2 != null ? "L" + lake2.level() : "  - ";
                objectArray[8] = lake2 == null && n17 != Integer.MIN_VALUE ? "~L" + n17 : "";
                objectArray[9] = n26.w1();
                objectArray[10] = columnPlan.terminal() ? " TERM" : "";
                Object object2 = objectArray[11] = columnPlan.source() ? " SRC" : "";
                objectArray[12] = n16 != 0 || n12 != 0 ? "  << RISE " + (String)(n16 != 0 ? "raw " + d2 + "->" + n26.y1() : "") + (String)(n12 != 0 ? "surf " + n9 + "->" + n13 : "") : "";
                ((StringBuilder)object3).append(String.format(Locale.ROOT, "  (%6d,%6d) | g %3d | raw %7.2f | surf %3d | water %4s | wall %4s | %s%s | w %4.1f |%s%s%s%n", objectArray));
                n9 = n13;
                d2 = n26.y1();
                ++n4;
            }
            riverSegment = n26;
            n21 = (Integer)hashMap3.get(StreamChannelDiagnostics.a(n26.x2(), n26.z2()));
            if (n21 != null && (Integer)n21 != n8 && nArray[(Integer)n21] != n11) {
                ((StringBuilder)object3).append(String.format(Locale.ROOT, "  -- continues as chain #%d --%n", nArray[(Integer)n21]));
                n11 = nArray[(Integer)n21];
            }
            n8 = n21 != null && (Integer)n21 != n8 ? (Integer)n21 : -1;
        }
        if (riverSegment != null) {
            String string;
            n3 = (int)Math.floor(riverSegment.x2());
            int n19 = (int)Math.floor(riverSegment.z2());
            object = riverEngine.lakeAt(n3, n19);
            int n20 = StreamChannelDiagnostics.a(riverEngine, riverSegment.y2(), riverSegment.w2());
            if (object != null) {
                string = "ends IN lake (level " + ((Lake)object).level() + "): our surface " + n20 + (String)(n20 >= ((Lake)object).level() ? " -> FLUSH/above (ok)" : " -> BELOW LAKE by " + (((Lake)object).level() - n20) + " << PROBLEM");
            } else if (riverSegment.y2() <= (double)riverEngine.config().seaLevel()) {
                string = "ends at the SEA (grade " + String.format(Locale.ROOT, "%.1f", riverSegment.y2()) + ")";
            } else {
                int n21 = chunkGenerator.getBaseHeight(n3, n19, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                string = "DRY end at grade " + String.format(Locale.ROOT, "%.1f", riverSegment.y2()) + " over ground " + n21 + " (truncated/terminal pool)";
            }
            ((StringBuilder)object3).append(String.format(Locale.ROOT, "  END at (%d,%d): %s%n", n3, n19, string));
        }
        ((StringBuilder)object3).append(String.format(Locale.ROOT, "  walked ~%.0f blocks to the terminus; %d downstream RISE row(s)%s; junctions:%s%n", d, n10, n10 > 0 ? " << NOT MONOTONE" : " (monotone)", stringBuilder.length() == 0 ? " none" : stringBuilder.toString()));
        ((StringBuilder)object3).append("  (per-pass modifier history: rebuild fresh with -Dstreamsreflowing.segdump=x,z,r -- a store-loaded network no longer carries it)");
        Constants.LOG.info(((StringBuilder)object3).toString());
    }

    private static int a(RiverEngine riverEngine, double d, double d2) {
        double d3 = 0.5 + 0.5 * Math.min(1.0, d2 / 3.0);
        return Math.max(riverEngine.config().seaLevel() - 1, (int)Math.floor(d) - (int)Math.round(riverEngine.config().ditchInset() * d3));
    }

    private static long a(double d, double d2) {
        long l = Math.round(d * 16.0) & 0xFFFFFFFFL;
        long l2 = Math.round(d2 * 16.0) & 0xFFFFFFFFL;
        return l << 32 | l2;
    }

    private static boolean a(RiverEngine riverEngine, int n, int n2, int n3, int n4) {
        double d = (double)n + 0.5;
        double d2 = (double)n2 + 0.5;
        for (int i = 0; i < n4 / 2; ++i) {
            ColumnPlan columnPlan = riverEngine.plan((int)Math.floor(d), (int)Math.floor(d2));
            if (!columnPlan.inCorridor()) {
                return false;
            }
            if (columnPlan.waterSurfaceY() <= n3) {
                return true;
            }
            double d3 = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
            if (d3 < 1.0E-6) {
                return false;
            }
            d += columnPlan.flowDirX() / d3 * 2.0;
            d2 += columnPlan.flowDirZ() / d3 * 2.0;
        }
        return false;
    }

    public static void spoutScan(RiverEngine riverEngine, ServerLevel serverLevel, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        int n12 = n3 * 2 + 1;
        int[] nArray = new int[n12 * n12];
        boolean[] blArray = new boolean[n12 * n12];
        PlateKey plateKey = riverEngine.plateAt(n, n2);
        int n13 = 32;
        for (n11 = -n3; n11 <= n3; ++n11) {
            for (n10 = -n3; n10 <= n3; ++n10) {
                n9 = n + n10;
                n8 = n2 + n11;
                if (!riverEngine.plateAt(n9, n8).equals(plateKey) || !riverEngine.plateAt(n9 - n13, n8).equals(plateKey) || !riverEngine.plateAt(n9 + n13, n8).equals(plateKey) || !riverEngine.plateAt(n9, n8 - n13).equals(plateKey) || !riverEngine.plateAt(n9, n8 + n13).equals(plateKey) || !riverEngine.plateAt(n9 - n13, n8 - n13).equals(plateKey) || !riverEngine.plateAt(n9 + n13, n8 + n13).equals(plateKey) || !riverEngine.plateAt(n9 - n13, n8 + n13).equals(plateKey) || !riverEngine.plateAt(n9 + n13, n8 - n13).equals(plateKey)) continue;
                n7 = (n11 + n3) * n12 + (n10 + n3);
                n6 = Integer.MIN_VALUE;
                ColumnPlan columnPlan = riverEngine.plan(n9, n8);
                if (columnPlan.inCorridor()) {
                    n6 = columnPlan.waterSurfaceY();
                }
                if ((n5 = riverEngine.lakeSurfaceAt(n9, n8)) != Integer.MIN_VALUE && n5 > n6) {
                    n6 = n5;
                }
                if (n6 == Integer.MIN_VALUE) continue;
                blArray[n7] = true;
                nArray[n7] = n6;
            }
        }
        n11 = 6;
        n10 = 1;
        n9 = 4;
        n8 = 0;
        n7 = 0;
        n6 = 0;
        int n14 = 0;
        n5 = 0;
        int n15 = 0;
        int[] nArray2 = new int[48];
        StringBuilder stringBuilder = new StringBuilder();
        int n16 = 0;
        for (int i = 0; i < n12; ++i) {
            for (n4 = 0; n4 < n12; ++n4) {
                int n17;
                int n18;
                int n19;
                int n20;
                int n21;
                int n22;
                int n23 = i * n12 + n4;
                if (!blArray[n23]) continue;
                int n24 = nArray[n23];
                int n25 = Integer.MAX_VALUE;
                int n26 = 0;
                int n27 = 0;
                for (n22 = -n11; n22 <= n11; ++n22) {
                    for (n21 = -n11; n21 <= n11; ++n21) {
                        if (n22 == 0 && n21 == 0) continue;
                        n20 = i + n22;
                        n19 = n4 + n21;
                        if (n20 < 0 || n19 < 0 || n20 >= n12 || n19 >= n12 || !blArray[n18 = n20 * n12 + n19]) continue;
                        ++n27;
                        n17 = nArray[n18];
                        if (n17 < n25) {
                            n25 = n17;
                        }
                        if (n17 < n24) continue;
                        ++n26;
                    }
                }
                if (n27 < 8 || n26 > n27 / 10 || n24 - n25 < n10) continue;
                n22 = 0;
                for (n21 = -n11; n21 <= n11; ++n21) {
                    for (n20 = -n11; n20 <= n11; ++n20) {
                        n19 = i + n21;
                        n18 = n4 + n20;
                        if (n19 < 0 || n18 < 0 || n19 >= n12 || n18 >= n12 || !blArray[n17 = n19 * n12 + n18] || nArray[n17] > n25 + n9) continue;
                        ++n22;
                    }
                }
                if (n22 * 10 < n27 * 6) {
                    ++n7;
                    continue;
                }
                n21 = n + (n4 - n3);
                n20 = n2 + (i - n3);
                n19 = chunkGenerator.getBaseHeight(n21, n20, Heightmap.Types.OCEAN_FLOOR_WG, serverLevel, randomState) - 1;
                if (n24 <= n19) {
                    ++n6;
                    continue;
                }
                ++n8;
                n18 = n24 - n25;
                int n28 = Math.min(nArray2.length - 1, n18);
                nArray2[n28] = nArray2[n28] + 1;
                if (n18 > n14) {
                    n14 = n18;
                    n5 = n + (n4 - n3);
                    n15 = n2 + (i - n3);
                }
                if (n16 >= 24) continue;
                stringBuilder.append(" (").append(n + (n4 - n3)).append(',').append(n2 + (i - n3)).append(")+").append(n18).append("@y").append(n24);
                ++n16;
            }
        }
        Constants.LOG.info("[SR-SPOUTSCAN] plate {} (whole-plate sweep from {},{}): {} REAL perched-spout columns (water poking >= {} above a surrounding flat water body AND above natural ground) -- ignored {} cascades + {} buried (planned surface below ground)", plateKey, n, n2, n8, n10, n7, n6);
        if (n8 == 0) {
            Constants.LOG.info("[SR-SPOUTSCAN] CLEAN: no water poking above a surrounding water body in the region.");
            return;
        }
        Constants.LOG.info("[SR-SPOUTSCAN] worst: +{} at ({},{})", n14, n5, n15);
        StringBuilder stringBuilder2 = new StringBuilder("[SR-SPOUTSCAN] histogram (height:columns):");
        for (n4 = n10; n4 < nArray2.length; ++n4) {
            if (nArray2[n4] <= 0) continue;
            stringBuilder2.append(" +").append(n4).append(":").append(nArray2[n4]);
        }
        Constants.LOG.info(stringBuilder2.toString());
        Constants.LOG.info("[SR-SPOUTSCAN] columns:{}", (Object)stringBuilder.toString());
    }
}
