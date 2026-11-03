/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import com.mojang.serialization.Codec;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.core.math.Hashing;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.flow.FlowTrace;
import dev.streamsreflowing.flow.RawFlowTiles;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.ChunkPlanCache;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamBiomeBlacklist;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.bank.BankStyle;
import dev.streamsreflowing.worldgen.bank.BankStyleLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

public class RiverCarverFeature
extends Feature<NoneFeatureConfiguration> {
    private static final boolean a = false;
    private static final double b = 0.15;
    private static final double c = 1.0;
    private static final int d = 16;
    private static final int e = 4;
    private static final int f = 20;
    private static final double g = 3.0;
    private static final double h = 2.0;
    private static final int i = 3;
    private static final int j = 3;
    private static final int k = 6;
    private static final boolean l = true;
    private static final int m = 24;
    private static final double n = 1.0;
    private static final int[][] o = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int p = 192;
    private static final Map<Long, BlockState[]> q = new ConcurrentHashMap<Long, BlockState[]>();
    private static final Queue<Long> r = new ConcurrentLinkedQueue<Long>();
    private static volatile Object s;
    private static final int t = 24;
    private static final ThreadLocal<int[]> u;
    private static final int v = 8;
    private static final int w = 3;
    private static final int x = 4;
    private static final int y = 4;
    private static final int z = 3;
    private static final ThreadLocal<HashMap<Long, Integer>> A;
    private static final int B = 1;
    private static final int C = 2;
    private static final int D = 3;
    private static final int E = 8;
    private static final int F = 24;
    private static final int G = 6;
    private static final int H = 4;
    private static final double I = 2.0;
    private static final int J = 3;
    private static final boolean K = false;
    private static final double L = 2.0;
    private static final int M = 5;
    private static final int N = 1;
    private static final int O = 8;
    private static final int P = 4;
    private static final double Q = 2.0;
    private static final double R = 3.0;
    private static final int S = 4;
    private static final int T = Integer.MIN_VALUE;
    private static final int U = -2147483647;
    private static final int[][] V;
    private static final int W = 6;
    private static final int X = 6;
    private static final int Y = 16;
    private static final int Z = 4;
    private static final int aa = 48;
    private static final int ab = 3;
    private static final int ac = 3;
    private static volatile Object ad;
    private static volatile List<BankStyle> ae;
    private static final int af = 2048;
    private static final int ag = 10;
    private static final int[][] ah;
    private static final Set<Block> ai;
    private static final int aj = 4;
    private static final double ak = 2.0;
    private static final double al = 0.45;
    private static final double am = 0.35;
    private static final double an = 8.0;
    private static final double ao = 16.0;
    private static final int ap = 1;
    private static final int aq = 1;
    private static final int ar = 2;
    private static final int as = 2;
    private static final double at = 2.0;
    private static final double au = 0.6;
    private static final int av = 8;
    private static final int aw = 6;
    private static final double ax = 8.0;
    private static final int ay = 2;
    private static final int[] az;
    private static volatile int[] aA;
    private static final int[] aB;
    private static final int[] aC;
    private static final boolean aD;
    private static final Map<ResourceKey<Level>, Map<Long, int[]>> aE;
    private static final int aF = 2048;
    private static final int aG = 2;
    private static final boolean aH = Boolean.getBoolean("streamsreflowing.noearlyout");

    private static void a(WorldGenLevel worldGenLevel, int n, int n2, BlockState[] blockStateArray, BlockState[] blockStateArray2) {
        ServerLevel serverLevel = worldGenLevel.getLevel();
        if (s != serverLevel) {
            q.clear();
            r.clear();
            s = serverLevel;
        }
        BlockState[] blockStateArray3 = new BlockState[512];
        System.arraycopy(blockStateArray, 0, blockStateArray3, 0, 256);
        System.arraycopy(blockStateArray2, 0, blockStateArray3, 256, 256);
        Long l = (long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL;
        if (q.put(l, blockStateArray3) == null) {
            Long l2;
            r.add(l);
            while (q.size() > 192 && (l2 = r.poll()) != null) {
                q.remove(l2);
            }
        }
    }

    private static BlockState a(int n, int n2, int n3) {
        BlockState[] blockStateArray = q.get((long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL);
        return blockStateArray == null ? null : blockStateArray[n3 + ((n & 0xF) * 16 + (n2 & 0xF))];
    }

    private static BlockState a(int n, int n2, WorldGenLevel worldGenLevel, int n3, int n4) {
        BlockState blockState = RiverCarverFeature.a(n, n2, 0);
        return blockState != null ? blockState : RiverCarverFeature.a(worldGenLevel, n, n2, n3, n4);
    }

    private static BlockState a(int n, int n2, WorldGenLevel worldGenLevel, int n3) {
        BlockState blockState = RiverCarverFeature.a(n, n2, 256);
        return blockState != null ? blockState : RiverCarverFeature.a(worldGenLevel, n, n2, n3);
    }

    private static boolean a(int n, int n2) {
        int[] nArray = u.get();
        return nArray == null || n >= nArray[0] && n <= nArray[1] && n2 >= nArray[2] && n2 <= nArray[3];
    }

    public RiverCarverFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n14;
        int n15;
        if (!Services.PLATFORM.generateRivers()) {
            return false;
        }
        WorldGenLevel worldGenLevel = featurePlaceContext.level();
        RiverEngine riverEngine = StreamsWorldgen.engine(worldGenLevel);
        RandomSource randomSource = featurePlaceContext.random();
        if (!RiverCarverFeature.aH) {
            ChunkPos chunkPosEarly = new ChunkPos(featurePlaceContext.origin());
            if (!riverEngine.corridorNearChunk(chunkPosEarly.getMinBlockX(), chunkPosEarly.getMinBlockZ(), 16) && !riverEngine.lakeNearChunk(chunkPosEarly.getMinBlockX(), chunkPosEarly.getMinBlockZ(), 16)) {
                return false;
            }
        }
        FlowTrace.carvedChunks.incrementAndGet();
        ChunkPos chunkPos = new ChunkPos(featurePlaceContext.origin());
        int n16 = chunkPos.getMinBlockX();
        int n17 = chunkPos.getMinBlockZ();
        u.set(new int[]{n16 - 16, n16 + 31, n17 - 16, n17 + 31});
        int n18 = worldGenLevel.getMinBuildHeight() + 1;
        int n19 = worldGenLevel.getMaxBuildHeight() - 1;
        int n20 = riverEngine.config().seaLevel();
        boolean bl = Services.PLATFORM.customBanks();
        BlockState blockState = Blocks.WATER.defaultBlockState();
        BlockState blockState2 = Blocks.AIR.defaultBlockState();
        BlockState blockState3 = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState blockState4 = Blocks.SHORT_GRASS.defaultBlockState();
        BlockState blockState5 = Blocks.TALL_GRASS.defaultBlockState();
        BlockState blockState6 = (BlockState)Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean bl2 = false;
        int[] nArray = new int[256];
        BlockState[] blockStateArray = new BlockState[256];
        BlockState[] blockStateArray2 = new BlockState[256];
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n21;
                nArray[i * 16 + j] = n21 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n16 + i, n17 + j) - 1;
                blockStateArray[i * 16 + j] = RiverCarverFeature.a(worldGenLevel, n16 + i, n17 + j, n21, n18);
                blockStateArray2[i * 16 + j] = RiverCarverFeature.a(worldGenLevel, n16 + i, n17 + j, n21);
            }
        }
        int[] nArray2 = new int[256];
        A.get().clear();
        int[] nArray3 = new int[256];
        Arrays.fill(nArray3, Integer.MIN_VALUE);
        int[] nArray4 = new int[256];
        Arrays.fill(nArray4, Integer.MIN_VALUE);
        int[] nArray5 = new int[256];
        Arrays.fill(nArray5, Integer.MIN_VALUE);
        int[] nArray6 = new int[256];
        Arrays.fill(nArray6, Integer.MIN_VALUE);
        int[] nArray7 = new int[256];
        if (riverEngine.config().growFromWater()) {
            int n22;
            int n23;
            for (n23 = 0; n23 < 16; ++n23) {
                for (n22 = 0; n22 < 16; ++n22) {
                    n15 = n16 + n23;
                    n14 = n17 + n22;
                    Lake lake = riverEngine.lakeNear(n15, n14, 2);
                    if (lake == null || lake.digDepth() > 0 || !(lake.coverageSmooth(n15, n14) > 0.5)) continue;
                    int n24 = n23 * 16 + n22;
                    nArray6[n24] = Mth.clamp(lake.level(), n18, n19);
                    nArray7[n24] = 3;
                }
            }
            if (riverEngine.config().containmentBerm()) {
                for (n23 = 0; n23 < 16; ++n23) {
                    for (n22 = 0; n22 < 16; ++n22) {
                        int n25;
                        int n26;
                        int n27;
                        Lake lake;
                        n15 = n23 * 16 + n22;
                        if (nArray6[n15] != Integer.MIN_VALUE || (lake = riverEngine.lakeNear(n14 = n16 + n23, n27 = n17 + n22, 4)) == null || lake.digDepth() > 0 || lake.coverageSmooth(n14, n27) > 0.5) continue;
                        n13 = 0;
                        n12 = 0;
                        n11 = 0;
                        block8: for (n10 = 1; n10 <= 8; ++n10) {
                            for (n9 = -n10; n9 <= n10; ++n9) {
                                for (n26 = -n10; n26 <= n10; ++n26) {
                                    if (Math.max(Math.abs(n9), Math.abs(n26)) != n10 || !(lake.coverageSmooth(n14 + n9, n27 + n26) > 0.5)) continue;
                                    n13 = n10;
                                    n12 = n9;
                                    n11 = n26;
                                    break block8;
                                }
                            }
                        }
                        if (n13 == 0 || riverEngine.inCorridor(n14, n27) || (n9 = nArray[n15]) >= (n10 = Mth.clamp(lake.level(), n18, n19))) continue;
                        n26 = 0;
                        n8 = Math.max(Math.abs(n12), Math.abs(n11));
                        for (n7 = 1; n7 < n8 && n26 == 0 && !(lake.coverageSmooth(n6 = n14 + (int)Math.round((double)n12 * ((double)n7 / (double)n8)), n5 = n27 + (int)Math.round((double)n11 * ((double)n7 / (double)n8))) > 0.5); ++n7) {
                            n4 = n6 - n16;
                            n3 = n5 - n17;
                            n25 = n4 >= 0 && n4 < 16 && n3 >= 0 && n3 < 16 ? nArray[n4 * 16 + n3] : worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n6, n5) - 1;
                            n26 = n25 >= n10 ? 1 : 0;
                        }
                        if (n26 != 0) continue;
                        double d = RiverCarverFeature.d(n14, n27);
                        n5 = 1 + (d > 0.8 ? 1 : 0);
                        n4 = n5 + 2 + (d < 0.2 ? 1 : 0);
                        if (n13 <= n5) {
                            n3 = n10;
                        } else if (n13 <= n4) {
                            n3 = Math.min(n19, n10 + 1);
                        } else {
                            double d2;
                            n25 = Math.min(n19, n10 + 1);
                            n3 = (int)Math.round((double)n25 - (d2 = Math.min(1.0, (double)(n13 - n4) / 4.0)) * (double)(n25 - n9));
                            if (n3 <= n9) continue;
                        }
                        BlockState blockState7 = RiverCarverFeature.a(worldGenLevel, n14, n27, n9, n18);
                        boolean bl3 = false;
                        for (n2 = n9 + 1; n2 <= n3; ++n2) {
                            mutableBlockPos.set(n14, n2, n27);
                            BlockState blockState8 = worldGenLevel.getBlockState(mutableBlockPos);
                            if (!blockState8.isAir()) continue;
                            BlockState blockState9 = RiverCarverFeature.a(worldGenLevel, n14, n27, n9, n3 - n2, blockState7, n18);
                            RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n14, n2, n27, blockState9, n18);
                            bl3 = true;
                        }
                        if (!bl3) continue;
                        bl2 = true;
                        if (!RiverCarverFeature.c(n14, n27)) continue;
                        RiverCarverFeature.b(n14, n27, "LAKE-BERM ground=" + n9 + " top=" + n3);
                    }
                }
            }
        }
        ColumnPlan[] columnPlanArray = new ColumnPlan[256];
        int[] nArray8 = new int[256];
        Arrays.fill(nArray8, Integer.MAX_VALUE);
        for (n15 = 0; n15 < 16; ++n15) {
            for (n14 = 0; n14 < 16; ++n14) {
                columnPlanArray[n15 * 16 + n14] = riverEngine.plan(n16 + n15, n17 + n14);
            }
        }
        n15 = 20;
        int[] nArray9 = new int[400];
        double[] dArray = new double[400];
        Arrays.fill(nArray9, Integer.MIN_VALUE);
        for (int i = -2; i < 18; ++i) {
            for (n13 = -2; n13 < 18; ++n13) {
                ColumnPlan columnPlan;
                n12 = (i + 2) * 20 + (n13 + 2);
                n11 = n16 + i;
                n10 = n17 + n13;
                n9 = i >= 0 && i < 16 && n13 >= 0 && n13 < 16 ? 1 : 0;
                ColumnPlan columnPlan2 = columnPlan = n9 != 0 ? columnPlanArray[i * 16 + n13] : riverEngine.planLean(n11, n10);
                if (columnPlan == null || !columnPlan.inCorridor() || columnPlan.inChannel()) continue;
                dArray[n12] = columnPlan.distance();
                nArray9[n12] = RiverCarverFeature.a(riverEngine, worldGenLevel, columnPlan, n11, n10, n18);
            }
        }
        int[] nArray10 = new int[256];
        Arrays.fill(nArray10, -2147483647);
        for (n13 = 0; n13 < 16; ++n13) {
            for (n12 = 0; n12 < 16; ++n12) {
                ColumnPlan columnPlan;
                n11 = n16 + n13;
                n10 = n17 + n12;
                columnPlanArray[n13 * 16 + n12] = columnPlan = columnPlanArray[n13 * 16 + n12];
                if (columnPlan == ColumnPlan.NONE || !columnPlan.inChannel()) continue;
                nArray8[n13 * 16 + n12] = RiverCarverFeature.a(worldGenLevel, riverEngine, mutableBlockPos, n11, n10, columnPlan, n19);
            }
        }
        int[] nArray11 = new int[256];
        int[] nArray12 = new int[256];
        for (n11 = 0; n11 < 16; ++n11) {
            for (n10 = 0; n10 < 16; ++n10) {
                ColumnPlan columnPlan = columnPlanArray[n11 * 16 + n10];
                if (columnPlan == null || !columnPlan.inCorridor()) continue;
                int n28 = columnPlan.waterSurfaceY();
                n8 = Integer.MAX_VALUE;
                n7 = Integer.MAX_VALUE;
                for (n6 = -2; n6 <= 2; ++n6) {
                    for (n5 = -2; n5 <= 2; ++n5) {
                        ColumnPlan columnPlan3;
                        if (n6 == 0 && n5 == 0) continue;
                        n4 = n11 + n6;
                        n3 = n10 + n5;
                        ColumnPlan columnPlan4 = columnPlan3 = n4 >= 0 && n4 < 16 && n3 >= 0 && n3 < 16 ? columnPlanArray[n4 * 16 + n3] : riverEngine.planLean(n16 + n4, n17 + n3);
                        if (columnPlan3 == null || !columnPlan3.inCorridor()) continue;
                        int n29 = columnPlan3.waterSurfaceY();
                        if (Math.max(Math.abs(n6), Math.abs(n5)) <= 1) {
                            n8 = Math.min(n8, n29);
                        }
                        n7 = Math.min(n7, n29);
                    }
                }
                if (n8 != Integer.MAX_VALUE && n28 - n8 >= 4) {
                    nArray11[n11 * 16 + n10] = 2;
                    nArray12[n11 * 16 + n10] = n8 + 1;
                    continue;
                }
                if (n7 == Integer.MAX_VALUE || n28 - n7 < 4) continue;
                nArray11[n11 * 16 + n10] = 1;
                nArray12[n11 * 16 + n10] = n7 + 1;
            }
        }
        int[] nArray13 = (int[])nArray8.clone();
        int[] p8RowCount = new int[256];
        int[] p8RowValid = new int[256];
        int[] p8RowMin = new int[256];
        for (int p8r = 0; p8r < 16; ++p8r) {
            for (int p8c = 0; p8c < 16; ++p8c) {
                int p8cnt = 0;
                int p8val = 0;
                int p8min = Integer.MAX_VALUE;
                for (int p8k = -4; p8k <= 4; ++p8k) {
                    int p8cc = p8c + p8k;
                    if (p8cc < 0 || p8cc > 15) continue;
                    int p8v = nArray8[p8r * 16 + p8cc];
                    if (p8v == Integer.MAX_VALUE) continue;
                    ++p8cnt;
                    if (p8v == Integer.MIN_VALUE) continue;
                    ++p8val;
                    p8min = Math.min(p8min, p8v);
                }
                p8RowCount[p8r * 16 + p8c] = p8cnt;
                p8RowValid[p8r * 16 + p8c] = p8val;
                p8RowMin[p8r * 16 + p8c] = p8min;
            }
        }
        for (int p8r = 0; p8r < 16; ++p8r) {
            for (int p8c = 0; p8c < 16; ++p8c) {
                if (nArray8[p8r * 16 + p8c] == Integer.MAX_VALUE) continue;
                int p8cnt = 0;
                int p8val = 0;
                int p8min = Integer.MAX_VALUE;
                for (int p8k = -4; p8k <= 4; ++p8k) {
                    int p8rr = p8r + p8k;
                    if (p8rr < 0 || p8rr > 15) continue;
                    int p8idx = p8rr * 16 + p8c;
                    p8cnt += p8RowCount[p8idx];
                    p8val += p8RowValid[p8idx];
                    if (p8RowValid[p8idx] == 0) continue;
                    p8min = Math.min(p8min, p8RowMin[p8idx]);
                }
                nArray13[p8r * 16 + p8c] = p8val * 2 > p8cnt ? p8min : Integer.MIN_VALUE;
            }
        }
        System.arraycopy(nArray13, 0, nArray8, 0, nArray8.length);
        for (n10 = 0; n10 < 16; ++n10) {
            for (int i = 0; i < 16; ++i) {
                BlockPos blockPos;
                BlockState blockState10;
                int n32;
                int n33;
                int n34;
                int n35;
                int n36;
                int n37;
                int n38;
                int n39;
                double d;
                double d3;
                ColumnPlan columnPlan;
                boolean bl4;
                int n40;
                Object object;
                int n41;
                int n42;
                int n43;
                int n44;
                ColumnPlan columnPlan5;
                int n45;
                int n46;
                int n47;
                int n48;
                int n49 = n16 + n10;
                n8 = n17 + i;
                n7 = nArray[n10 * 16 + i];
                mutableBlockPos.set(n49, n7, n8);
                if (StreamBiomeBlacklist.isBlocked(worldGenLevel.getUncachedNoiseBiome(n49 >> 2, n7 >> 2, n8 >> 2))) continue;
                n6 = 0;
                n5 = nArray6[n10 * 16 + i];
                if (n5 != Integer.MIN_VALUE) {
                    ColumnPlan columnPlan6 = riverEngine.plan(n49, n8);
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, String.format(Locale.ROOT, "LAKE-SNAP snapY=%d ground=%d corr=%s chan=%s dist=%.1f w=%.1f surf=%d%s", n5, n7, columnPlan6.inCorridor(), columnPlan6.inChannel(), columnPlan6.distance(), columnPlan6.width(), columnPlan6.waterSurfaceY(), columnPlan6.inCorridor() ? "" : " reject=[" + riverEngine.planDiag(n49, n8) + "]"));
                    }
                    if (columnPlan6.inChannel()) {
                        double d4 = columnPlan6.width() > 1.0E-6 ? Math.min(1.0, columnPlan6.distance() / columnPlan6.width()) : 1.0;
                        int n50 = Math.max(1, (int)Math.round(2.0 * columnPlan6.width() * riverEngine.config().streamDepth()));
                        n2 = Math.min(n5, columnPlan6.waterSurfaceY());
                        int n51 = n2 - (int)Math.round((double)n50 * (1.0 - d4 * d4));
                        n48 = Math.max(n18, Math.min(n7, n51));
                        if (RiverCarverFeature.c(n49, n8)) {
                            RiverCarverFeature.b(n49, n8, "LAKE-SNAP-DIG ground=" + n7 + " digTo=" + n48 + " bowlRef=" + n2);
                        }
                        for (n = n7; n > n48; --n) {
                            mutableBlockPos.set(n49, n, n8);
                            worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
                        }
                        if (n48 < n7) {
                            RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n48, n48, n7, n18);
                        }
                    }
                    if (n7 >= n5 && !columnPlan6.inChannel()) {
                        int n52;
                        int n53 = Math.max(n18, n5 - 2);
                        for (n52 = n7; n52 > n5 && n52 > n18; --n52) {
                            mutableBlockPos.set(n49, n52, n8);
                            worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                        }
                        for (n52 = Math.min(n5, n7); n52 > n53 && n52 > n18; --n52) {
                            mutableBlockPos.set(n49, n52, n8);
                            worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
                        }
                        RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n53, n53, n7, n18);
                        if (RiverCarverFeature.c(n49, n8)) {
                            RiverCarverFeature.b(n49, n8, "LAKE-EXCAVATE ground=" + n7 + " level=" + n5 + " bedTop=" + n53);
                        }
                    }
                    for (n3 = n5; n3 > n7 && n3 > n18; --n3) {
                        mutableBlockPos.set(n49, n3, n8);
                        RiverCarverFeature.a(worldGenLevel, (BlockPos)mutableBlockPos, blockState);
                    }
                    RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n5, n8, blockState);
                    RiverCarverFeature.c(worldGenLevel, mutableBlockPos, n49, n8, Math.max(n7 + 1, n5 + 1), n19);
                    bl2 = true;
                    columnPlanArray[n10 * 16 + i] = columnPlan6;
                    if (!columnPlan6.inCorridor()) continue;
                    n6 = 1;
                }
                int n54 = n10 * 16 + i;
                n3 = Integer.MIN_VALUE;
                for (int[] nArray14 : o) {
                    n48 = n49 + nArray14[0];
                    n = n8 + nArray14[1];
                    Lake lake = riverEngine.lakeNear(n48, n, 2);
                    if (lake == null || lake.digDepth() > 0 || !(lake.coverageSmooth(n48, n) > 0.5)) continue;
                    n3 = Math.max(n3, Mth.clamp(lake.level(), n18, n19));
                }
                Object object2 = RiverCarverFeature.a(riverEngine, n49, n8);
                int n55 = nArray10[n54] = object2 == null ? Integer.MIN_VALUE : ((Lake)object2).level();
                if (n6 == 0 && object2 != null) {
                    int n56 = Mth.clamp(((Lake)object2).level(), n18, n19);
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "LAKE level=" + n56 + " dig=" + ((Lake)object2).digDepth() + " ground=" + n7 + " fills=" + (((Lake)object2).digDepth() > 0 || n7 < n56));
                    }
                    if (((Lake)object2).digDepth() <= 0) {
                        if (n7 < n56) {
                            ColumnPlan columnPlan7;
                            n2 = n7;
                            columnPlanArray[n10 * 16 + i] = columnPlan7 = riverEngine.plan(n49, n8);
                            if (RiverCarverFeature.c(n49, n8)) {
                                RiverCarverFeature.b(n49, n8, String.format(Locale.ROOT, "LAKE-CROSS corr=%s chan=%s dist=%.1f w=%.1f surf=%d%s", columnPlan7.inCorridor(), columnPlan7.inChannel(), columnPlan7.distance(), columnPlan7.width(), columnPlan7.waterSurfaceY(), columnPlan7.inCorridor() ? "" : " reject=[" + riverEngine.planDiag(n49, n8) + "]"));
                            }
                            if (columnPlan7.inChannel()) {
                                double d5 = columnPlan7.width() > 1.0E-6 ? Math.min(1.0, columnPlan7.distance() / columnPlan7.width()) : 1.0;
                                int n57 = Math.max(1, (int)Math.round(2.0 * columnPlan7.width() * riverEngine.config().streamDepth()));
                                n47 = Math.min(n56, columnPlan7.waterSurfaceY());
                                n46 = n47 - (int)Math.round((double)n57 * (1.0 - d5 * d5));
                                n2 = Math.max(n18, Math.min(n7, n46));
                                if (RiverCarverFeature.c(n49, n8)) {
                                    RiverCarverFeature.b(n49, n8, "LAKE-DIG ground=" + n7 + " digTo=" + n2 + " bowlRef=" + n47 + " centerDepth=" + n57);
                                }
                                for (n45 = n7; n45 > n2; --n45) {
                                    mutableBlockPos.set(n49, n45, n8);
                                    worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
                                }
                                if (n2 < n7) {
                                    RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n2, n2, n7, n18);
                                }
                            }
                            for (int j = n56; j > n7 && j > n18; --j) {
                                mutableBlockPos.set(n49, j, n8);
                                RiverCarverFeature.a(worldGenLevel, (BlockPos)mutableBlockPos, blockState);
                            }
                            RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n56, n8, blockState);
                            RiverCarverFeature.c(worldGenLevel, mutableBlockPos, n49, n8, Math.max(n7 + 1, n56 + 1), n19);
                            bl2 = true;
                            if (!riverEngine.plan(n49, n8).inCorridor()) {
                                if (!RiverCarverFeature.c(n49, n8)) continue;
                                RiverCarverFeature.b(n49, n8, "LAKE-STOP not in any corridor -> lake fill only");
                                continue;
                            }
                            n6 = 1;
                        }
                        if (worldGenLevel.getBlockState(mutableBlockPos.set(n49, n56, n8)).isAir()) {
                            int n58;
                            n2 = Integer.MIN_VALUE;
                            for (n58 = n56 - 1; n58 >= Math.max(n18, n56 - 24); --n58) {
                                mutableBlockPos.set(n49, n58, n8);
                                if (worldGenLevel.getBlockState(mutableBlockPos).isAir() || !worldGenLevel.getBlockState(mutableBlockPos).getFluidState().isEmpty()) continue;
                                n2 = n58;
                                break;
                            }
                            if (n2 != Integer.MIN_VALUE) {
                                for (n58 = n56; n58 > n2; --n58) {
                                    mutableBlockPos.set(n49, n58, n8);
                                    RiverCarverFeature.a(worldGenLevel, (BlockPos)mutableBlockPos, blockState);
                                }
                                bl2 = true;
                                continue;
                            }
                        }
                    } else {
                        ColumnPlan columnPlan8;
                        n2 = Mth.clamp(Math.min(n7, ((Lake)object2).level() - ((Lake)object2).digDepth()), n18, n56);
                        columnPlanArray[n10 * 16 + i] = columnPlan8 = riverEngine.plan(n49, n8);
                        n48 = Integer.MAX_VALUE;
                        if (columnPlan8.inChannel()) {
                            double d6 = columnPlan8.width() > 1.0E-6 ? Math.min(1.0, columnPlan8.distance() / columnPlan8.width()) : 1.0;
                            int n59 = Math.max(1, (int)Math.round(2.0 * columnPlan8.width() * riverEngine.config().streamDepth()));
                            n48 = n56 - (int)Math.round((double)n59 * (1.0 - d6 * d6));
                            n2 = Mth.clamp(Math.min(n2, n48), n18, n56);
                        }
                        for (int j = n = Mth.clamp(Math.max(n7, ((Lake)object2).level()), n18, n19); j > n2; --j) {
                            mutableBlockPos.set(n49, j, n8);
                            worldGenLevel.setBlock(mutableBlockPos, j <= n56 ? blockState : blockState2, 2);
                        }
                        if (n48 != Integer.MAX_VALUE && n2 < n7) {
                            RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n2, n2, n7, n18);
                        }
                        RiverCarverFeature.c(worldGenLevel, mutableBlockPos, n49, n8, n56 + 1, n19);
                        bl2 = true;
                        continue;
                    }
                }
                columnPlanArray[n54] = columnPlan5 = riverEngine.plan(n49, n8);
                if (RiverCarverFeature.c(n49, n8)) {
                    RiverCarverFeature.b(n49, n8, String.format(Locale.ROOT, "plan corr=%s chan=%s dist=%.1f w=%.1f surf=%d term=%s src=%s ground=%d lake=%s samp=%.1f exact=%.1f%s", columnPlan5.inCorridor(), columnPlan5.inChannel(), columnPlan5.distance(), columnPlan5.width(), columnPlan5.waterSurfaceY(), columnPlan5.terminal(), columnPlan5.source(), n7, object2 == null ? "-" : String.valueOf(((Lake)object2).level()), riverEngine.sampledHeight(n49, n8), riverEngine.exactHeight(n49, n8), columnPlan5.inCorridor() ? "" : " reject=[" + riverEngine.planDiag(n49, n8) + "]"));
                }
                if (!columnPlan5.inCorridor()) {
                    if (!RiverCarverFeature.b(n49, n8)) continue;
                    RiverCarverFeature.a(n49, n8, "EXIT: not in any corridor -- the carve never touches this column");
                    continue;
                }
                if (RiverCarverFeature.b(n49, n8)) {
                    RiverCarverFeature.a(n49, n8, "=== STACK for (" + n49 + "," + n8 + ") ===");
                    RiverCarverFeature.a(n49, n8, "  plan: chan=" + columnPlan5.inChannel() + " dist=" + String.format(Locale.ROOT, "%.2f", columnPlan5.distance()) + " width=" + String.format(Locale.ROOT, "%.2f", columnPlan5.width()) + " grade=" + columnPlan5.waterSurfaceY() + " ground=" + n7 + " terminal=" + columnPlan5.terminal() + " source=" + columnPlan5.source());
                }
                mutableBlockPos.set(n49, n7 + 1, n8);
                if (n6 == 0 && !worldGenLevel.getBlockState(mutableBlockPos).getFluidState().isEmpty()) {
                    n2 = n7 + 1;
                    while (n2 + 1 <= n19 && !worldGenLevel.getBlockState(mutableBlockPos.set(n49, n2 + 1, n8)).getFluidState().isEmpty()) {
                        ++n2;
                    }
                    int n60 = 6;
                    int n61 = n48 = RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n8, columnPlan5, n19, Math.hypot(columnPlan5.flowDirX(), columnPlan5.flowDirZ())) != Integer.MIN_VALUE ? 1 : 0;
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "FLUSH check fluidTop=" + n2 + " margin=" + n60 + " fullWidth=" + (n48 != 0) + " chan=" + columnPlan5.inChannel() + " skip=" + (columnPlan5.terminal() || columnPlan5.inChannel() && n48 != 0 && columnPlan5.waterSurfaceY() <= n2 + n60));
                    }
                    int n62 = n = columnPlan5.inChannel() && RiverCarverFeature.a(worldGenLevel, riverEngine, mutableBlockPos, n49, n8, columnPlan5, n19, n10, i, nArray8) != Integer.MIN_VALUE ? 1 : 0;
                    if (RiverCarverFeature.b(n49, n8)) {
                        RiverCarverFeature.a(n49, n8, "== FLUSH stage ==");
                        RiverCarverFeature.a(n49, n8, "  fluidTop=" + n2 + " grade=" + columnPlan5.waterSurfaceY() + " terminal=" + columnPlan5.terminal() + " reachDropped=" + (n != 0) + " -> " + (columnPlan5.terminal() || n != 0 ? "SKIP (nothing built)" : "carve on"));
                    }
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "FLUSH check fluidTop=" + n2 + " chan=" + columnPlan5.inChannel() + " reachDropped=" + (n != 0) + " skip=" + (columnPlan5.terminal() || n != 0));
                    }
                    if (columnPlan5.terminal() || n != 0) {
                        if (!columnPlan5.inChannel() || !RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n8, columnPlan5, riverEngine, n7, n2, n18, blockState)) continue;
                        bl2 = true;
                        continue;
                    }
                }
                mutableBlockPos.set(n49, n7, n8);
                n2 = worldGenLevel.getBlockState(mutableBlockPos).is(Blocks.GRASS_BLOCK) ? 1 : 0;
                boolean bl5 = columnPlan5.terminal() && (columnPlan5.waterSurfaceY() <= n20 - 1 + 3 || object2 != null && ((Lake)object2).level() >= columnPlan5.waterSurfaceY() - 3);
                n48 = columnPlan5.waterSurfaceY();
                if (object2 == null && n6 == 0 && riverEngine.config().containmentBerm() && !bl5 && !columnPlan5.source() && n7 < n48 && n7 >= n48 - 4 && columnPlan5.distance() <= columnPlan5.width() && RiverCarverFeature.a(worldGenLevel, n49, n8, columnPlan5, n48) && RiverCarverFeature.b(worldGenLevel, n49, n8, n7, n48, n18)) {
                    n = Math.min(n19, n48);
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "POOL surface=" + n + " ground=" + n7);
                    }
                    for (int j = n; j > n7 && j > n18; --j) {
                        mutableBlockPos.set(n49, j, n8);
                        RiverCarverFeature.a(worldGenLevel, (BlockPos)mutableBlockPos, blockState);
                    }
                    RiverCarverFeature.c(worldGenLevel, mutableBlockPos, n49, n8, Math.max(n7 + 1, n + 1), n19);
                    bl2 = true;
                    continue;
                }
                if (columnPlan5.inChannel()) {
                    int n63;
                    boolean bl6;
                    int n64;
                    int n65;
                    int n66;
                    double d7;
                    double d8;
                    d8 = Math.hypot(columnPlan5.flowDirX(), columnPlan5.flowDirZ());
                    n = riverEngine.config().containmentBerm() && !bl5 && n6 == 0 ? 1 : 0;
                    int n67 = columnPlan5.waterSurfaceY();
                    n47 = Integer.MAX_VALUE;
                    n46 = n != 0 ? RiverCarverFeature.a(worldGenLevel, n49, n8, columnPlan5, n7, n18, n19) : Math.min(Math.min(n67, n7), n47);
                    n46 = Mth.clamp(n46, n18, n19);
                    if (RiverCarverFeature.b(n49, n8)) {
                        RiverCarverFeature.a(n49, n8, "== CHANNEL surface derivation ==");
                        RiverCarverFeature.a(n49, n8, "  plan grade=" + n67 + " ground=" + n7 + " dist=" + String.format(Locale.ROOT, "%.2f", columnPlan5.distance()) + " width=" + String.format(Locale.ROOT, "%.2f", columnPlan5.width()) + " contain=" + (n != 0) + " slopeCap=" + n47 + " terminalOpen=" + bl5 + " throughLake=" + (n6 != 0));
                        RiverCarverFeature.a(n49, n8, "  base (containedWaterLevel): " + n46);
                    }
                    if (columnPlan5.source() && n7 - 1 > n18) {
                        n46 = RiverCarverFeature.a(n49, n8, "source recess", n46, Math.min(n46, n7 - 1));
                    }
                    if ((n45 = RiverCarverFeature.a(riverEngine, worldGenLevel, columnPlan5, n49, n8, n7, RiverCarverFeature.nearbyLakeLevel(riverEngine, n49, n8, n18, n19), n20, n18, n19, d8)) != Integer.MIN_VALUE) {
                        n46 = RiverCarverFeature.a(n49, n8, "lake level-out (" + n45 + ")", n46, Mth.clamp(Math.max(n46, n45), n18, n19));
                    }
                    if (object2 != null && ((Lake)object2).level() >= n46 && n7 < ((Lake)object2).level()) {
                        if (RiverCarverFeature.c(n49, n8)) {
                            RiverCarverFeature.b(n49, n8, "LAKE-FLUSH surface " + n46 + " -> " + ((Lake)object2).level());
                        }
                        n46 = RiverCarverFeature.a(n49, n8, "submerged in lake " + ((Lake)object2).level(), n46, Mth.clamp(((Lake)object2).level(), n18, n19));
                    }
                    if (n3 != Integer.MIN_VALUE) {
                        n46 = RiverCarverFeature.a(n49, n8, "flood level (" + n3 + ")", n46, Mth.clamp(Math.max(n46, n3), n18, n19));
                    }
                    if (d8 > 1.0E-6) {
                        double d9 = -columnPlan5.flowDirZ() / d8;
                        d7 = columnPlan5.flowDirX() / d8;
                        for (n44 = -1; n44 <= 1; n44 += 2) {
                            for (n43 = 2; n43 <= 6; n43 += 2) {
                                ColumnPlan columnPlan9 = riverEngine.plan(n49 + (int)Math.round(d9 * (double)n43 * (double)n44), n8 + (int)Math.round(d7 * (double)n43 * (double)n44));
                                if (!columnPlan9.inCorridor() || !columnPlan9.inChannel() || columnPlan9.waterSurfaceY() >= n46 || n46 - columnPlan9.waterSurfaceY() > 4) continue;
                                n46 = RiverCarverFeature.a(n49, n8, "junction pool (neighbour at r=" + n43 + " side=" + n44 + ")", n46, columnPlan9.waterSurfaceY());
                            }
                        }
                    }
                    int n68 = n66 = (n65 = RiverCarverFeature.a(worldGenLevel, riverEngine, mutableBlockPos, n49, n8, columnPlan5, n19, n10, i, nArray8)) == Integer.MIN_VALUE ? n46 : Math.min(n46, n65);
                    if (n66 != n46 && RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "GRADE-CLAMP " + n46 + " -> " + n66 + " (fell into water upstream)");
                    }
                    n46 = RiverCarverFeature.a(n49, n8, "drop clamp (inherited=" + n65 + ")", n46, n66);
                    if (nArray11[n54] > 0 && (n64 = Math.max(nArray12[n54], n46 - nArray11[n54])) < n46) {
                        if (RiverCarverFeature.c(n49, n8)) {
                            RiverCarverFeature.b(n49, n8, "LIP-SHAVE surface " + n46 + " -> " + n64 + " floor=" + nArray12[n54] + " shave=" + nArray11[n54]);
                        }
                        n46 = RiverCarverFeature.a(n49, n8, "lip shave", n46, n64);
                    }
                    d7 = columnPlan5.width() > 1.0E-6 ? Math.min(1.0, columnPlan5.distance() / columnPlan5.width()) : 1.0;
                    n44 = Math.max(1, (int)Math.round(2.0 * columnPlan5.width() * riverEngine.config().streamDepth()));
                    n43 = (int)Math.round((double)n44 * (1.0 - d7 * d7));
                    n42 = Mth.clamp(n46 - n43, n18, n46 - 1);
                    n41 = Math.min(n19, Math.max(n7, n46));
                    mutableBlockPos.set(n49, n42, n8);
                    object = worldGenLevel.getBlockState(mutableBlockPos);
                    boolean bl7 = !((BlockBehaviour.BlockStateBase)object).isAir() && ((BlockBehaviour.BlockStateBase)object).getFluidState().isEmpty();
                    nArray3[n54] = n46;
                    if (RiverCarverFeature.b(n49, n8)) {
                        RiverCarverFeature.a(n49, n8, "  FINAL channel surface=" + n46 + " bed=" + n42 + " supported=" + bl7);
                    }
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "CHANNEL surface=" + n46 + " bed=" + n42 + " top=" + Math.min(n19, Math.max(n7, n46)) + " supported=" + bl7 + " contain=" + (n != 0));
                    }
                    int n69 = n42 - n7 >= 4 ? RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n8, columnPlan5, n19, d8) : Integer.MIN_VALUE;
                    boolean bl8 = bl6 = n69 != Integer.MIN_VALUE && (n42 - n7 >= 4 && !RiverCarverFeature.a(worldGenLevel, riverEngine, n49, n8, columnPlan5, n69, d8) || columnPlan5.distToOutlet() <= 24.0);
                    if (bl6 && RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "CUP released: waterfall over water (ground=" + n7 + " waterTop=" + n69 + " bed=" + n42 + ")");
                    }
                    if (n != 0 && !columnPlan5.source() && !bl6) {
                        int n70 = RiverCarverFeature.a(n49, n8, n46, columnPlan5, riverEngine.config().bankWidth());
                        RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n70, n42, n7, n18);
                        bl7 = true;
                    }
                    BlockState blockState11 = Services.PLATFORM.streamWaterState(RiverCarverFeature.a(columnPlan5));
                    for (n63 = n41; n63 > n42; --n63) {
                        BlockState blockState12;
                        mutableBlockPos.set(n49, n63, n8);
                        if (n63 > n46) {
                            blockState12 = worldGenLevel.getBlockState(mutableBlockPos);
                            if (blockState12.isAir() || blockState12.is(BlockTags.LEAVES)) continue;
                            if (blockState12.is(BlockTags.LOGS)) {
                                worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                                RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n63 + 1, n19);
                                continue;
                            }
                            if (!bl7 && !blockState12.getFluidState().isEmpty()) continue;
                            worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                            continue;
                        }
                        if (bl7) {
                            worldGenLevel.setBlock(mutableBlockPos, blockState11, 2);
                            continue;
                        }
                        blockState12 = worldGenLevel.getBlockState(mutableBlockPos);
                        if (blockState12.isAir() || !blockState12.getFluidState().isEmpty()) continue;
                        worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                    }
                    if (bl7 && RiverCarverFeature.a(riverEngine, n49, n8, n46)) {
                        n63 = 5 + Math.floorMod(Mth.getSeed(n49, 0, n8), 20);
                        worldGenLevel.scheduleTick(new BlockPos(n49, n46, n8), blockState11.getFluidState().getType(), n63);
                    }
                    RiverCarverFeature.c(worldGenLevel, mutableBlockPos, n49, n8, n7 + 1, n19);
                    if (n46 < n7) {
                        RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n8, n46 + 1, n7 + 1, n20);
                    }
                    bl2 = true;
                    continue;
                }
                n = columnPlan5.waterSurfaceY();
                if (RiverCarverFeature.b(n49, n8)) {
                    RiverCarverFeature.a(n49, n8, "== BERM derivation ==");
                    RiverCarverFeature.a(n49, n8, "  base waterLevel=" + n + " ground=" + n7);
                }
                int n77 = RiverCarverFeature.nearbyLakeLevel(riverEngine, n49, n8, n18, n19);
                if ((n40 = RiverCarverFeature.a(riverEngine, worldGenLevel, columnPlan5, n49, n8, n7, n77, n20, n18, n19)) != Integer.MIN_VALUE) {
                    n = RiverCarverFeature.a(n49, n8, "berm lake level-out (" + n40 + ")", n, Math.max(n, n40));
                }
                if (object2 != null && ((Lake)object2).level() >= n) {
                    n = ((Lake)object2).level();
                }
                Lake lake = null;
                for (n46 = 0; n46 < 4; ++n46) {
                    int n71;
                    n45 = n49 + (n46 == 0 ? 1 : (n46 == 1 ? -1 : 0));
                    Lake lake2 = riverEngine.lakeNear(n45, n71 = n8 + (n46 == 2 ? 1 : (n46 == 3 ? -1 : 0)), 2);
                    if (lake2 == null || lake2.digDepth() > 0 || lake2.level() <= n || !(lake2.coverageSmooth(n45, n71) > 0.5)) continue;
                    n = RiverCarverFeature.a(n49, n8, "adjacent lake fill (" + lake2.level() + ")", n, lake2.level());
                    lake = lake2;
                }
                ColumnPlan columnPlan10 = riverEngine.altPlan(n49, n8);
                if (columnPlan10 != null && columnPlan10.waterSurfaceY() > n) {
                    n = RiverCarverFeature.a(n49, n8, "alt stream water (" + columnPlan10.waterSurfaceY() + ")", n, columnPlan10.waterSurfaceY());
                }
                nArray5[n10 * 16 + i] = n;
                n45 = RiverCarverFeature.a(worldGenLevel, riverEngine, mutableBlockPos, n49, n8, columnPlan5, n19, n10, i, nArray8);
                boolean bl9 = bl4 = n45 != Integer.MIN_VALUE;
                if (bl4) {
                    n = RiverCarverFeature.a(n49, n8, "berm drop clamp (" + n45 + ")", n, Math.min(n, n45));
                }
                nArray5[n10 * 16 + i] = n;
                double d10 = Math.hypot(columnPlan5.flowDirX(), columnPlan5.flowDirZ());
                if (d10 > 1.0E-6 && (columnPlan = riverEngine.plan(n49 - (int)Math.round(columnPlan5.flowDirX() / d10), n8 - (int)Math.round(columnPlan5.flowDirZ() / d10))) != ColumnPlan.NONE && columnPlan.waterSurfaceY() - n <= 2) {
                    n = RiverCarverFeature.a(n49, n8, "upstream push (" + columnPlan.waterSurfaceY() + ")", n, Math.max(n, columnPlan.waterSurfaceY()));
                }
                double d11 = columnPlan5.upstreamSlope();
                boolean bl10 = false;
                if (d11 >= 0.15 && (d3 = d10) > 1.0E-6 && (object = riverEngine.plan(n42 = n49 - (int)Math.round(4.0 * columnPlan5.flowDirX() / d3), n41 = n8 - (int)Math.round(4.0 * columnPlan5.flowDirZ() / d3))) != null && object != ColumnPlan.NONE && (d = Math.hypot(((ColumnPlan)object).flowDirX(), ((ColumnPlan)object).flowDirZ())) > 1.0E-6) {
                    double d12 = (columnPlan5.flowDirX() * ((ColumnPlan)object).flowDirX() + columnPlan5.flowDirZ() * ((ColumnPlan)object).flowDirZ()) / (d3 * d);
                    bl10 = d12 < 0.906;
                }
                int n72 = n44 = bl10 && d11 >= 0.15 ? 1 : 0;
                if (columnPlan5.waterSurfaceY() < n20) {
                    n44 = Math.max(0, n44 - 1);
                }
                n43 = n + n44;
                mutableBlockPos.set(n49, n, n8);
                int n73 = n42 = bl4 && (n7 >= n || RiverCarverFeature.e(worldGenLevel, mutableBlockPos, n49, n8, n7, n19) != Integer.MIN_VALUE) ? 1 : 0;
                if (riverEngine.config().containmentBerm() && (columnPlan5.waterSurfaceY() >= n20 || columnPlan5.distToOutlet() > 24.0) && n7 < n43 && !bl5 && !columnPlan5.source() && nArray6[n10 * 16 + i] == Integer.MIN_VALUE && n42 == 0) {
                    int n74;
                    n41 = n44 > 0 ? 2 : 0;
                    double d13 = columnPlan5.distance() - columnPlan5.width();
                    double d14 = d13 - (double)n41;
                    double d15 = Math.min(2.0, riverEngine.config().bankSlope() + riverEngine.config().bankSteepFactor() * Math.pow(columnPlan5.slope(), 0.8));
                    double d16 = 2.0 * (double)n - (double)n7;
                    double d17 = (double)n + Math.max(0.0, d14) * d15 / 2.0;
                    double d18 = (double)n + columnPlan5.bankBlend() * (d16 - (double)n);
                    double d19 = 0.0;
                    int n75 = Mth.clamp((int)Math.floor(Math.max(d17, d18) + d19), n, (int)Math.max((double)n, d16));
                    n39 = 2 * n - n75 + n44;
                    if (d13 < (double)n41) {
                        n39 = Math.min(n19, n);
                    } else {
                        if (d14 <= Math.max(1.0, columnPlan5.width() / 3.0) * 2.0) {
                            n74 = n43 + (int)Math.round(Math.max(0.0, d19));
                            n39 = Math.min(n19, Math.max(n39, n74));
                        }
                        if ((n74 = n44 * 3) > 0 && d14 <= (double)n74) {
                            n38 = n43 - (int)Math.round(d14 / (double)n74 * (double)n44);
                            n39 = Math.min(n19, Math.max(n39, n38));
                        }
                    }
                    if (n44 >= 2 && d13 >= (double)n41) {
                        n74 = n + 1 + (int)Math.floor(d13 - (double)n41);
                        n39 = Math.min(n39, n74);
                    }
                    BlockState blockState13 = RiverCarverFeature.a(worldGenLevel, n49, n8, n7, n18);
                    n38 = 0;
                    if (RiverCarverFeature.c(n49, n8)) {
                        n37 = RiverCarverFeature.e(worldGenLevel, mutableBlockPos, n49, n8, n7, n19);
                        nArray4[n10 * 16 + i] = Math.max(nArray4[n10 * 16 + i], n39);
                        if (RiverCarverFeature.b(n49, n8)) {
                            RiverCarverFeature.a(n49, n8, "  BERM BUILT water=" + n + " freeboard=" + n44 + " top=" + n39);
                        }
                        RiverCarverFeature.b(n49, n8, "BERM water=" + n + " freeboard=" + n44 + " dropVerdict=" + (n45 == Integer.MIN_VALUE ? "none" : String.valueOf(n45)) + " top=" + n39 + " ground=" + n7 + " dto=" + String.valueOf(columnPlan5.distToOutlet() > 999.0 ? "999" : Integer.valueOf((int)columnPlan5.distToOutlet())) + " wtb=" + (n37 == Integer.MIN_VALUE ? "dry" : String.valueOf(n37)));
                    }
                    if ((n37 = RiverCarverFeature.e(worldGenLevel, mutableBlockPos, n49, n8, n7, n19)) != Integer.MIN_VALUE && n37 >= n) {
                        if (!RiverCarverFeature.c(n49, n8)) continue;
                        RiverCarverFeature.b(n49, n8, "BERM skipped: submerged to " + n37 + " >= containment water " + n + " (crest would be " + n39 + ")");
                        continue;
                    }
                    nArray4[n10 * 16 + i] = Math.max(nArray4[n10 * 16 + i], n39);
                    for (n36 = n35 = Math.max(n18, RiverCarverFeature.a(n49, n8, n, columnPlan5, riverEngine.config().bankWidth(), 4)); n36 <= n39 && n36 <= n19; ++n36) {
                        mutableBlockPos.set(n49, n36, n8);
                        BlockState blockState14 = worldGenLevel.getBlockState(mutableBlockPos);
                        if (!blockState14.isAir()) continue;
                        BlockState blockState15 = RiverCarverFeature.a(worldGenLevel, n49, n8, n7, n39 - n36, blockState13, n18);
                        RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n36, n8, blockState15, n18);
                        n38 = 1;
                    }
                    if (n38 != 0) {
                        bl2 = true;
                        continue;
                    }
                } else if (RiverCarverFeature.c(n49, n8) && riverEngine.config().containmentBerm() && n7 < n43 && !bl5 && !columnPlan5.source()) {
                    if (RiverCarverFeature.b(n49, n8)) {
                        RiverCarverFeature.a(n49, n8, "  BERM SKIPPED (gates below)");
                    }
                    RiverCarverFeature.b(n49, n8, "BERM skipped: waterLevel=" + n + " dropped=" + bl4 + " mouthGate=" + (columnPlan5.waterSurfaceY() < n20 && !(columnPlan5.distToOutlet() > 24.0)) + " lakeFill=" + (nArray6[n10 * 16 + i] != Integer.MIN_VALUE) + " terminalOpen=" + bl5 + " src=" + columnPlan5.source());
                }
                n41 = Integer.MAX_VALUE;
                int n76 = Math.min(Math.min(columnPlan5.waterSurfaceY(), n7), n41);
                int n78 = n40;
                if (n78 != Integer.MIN_VALUE) {
                    n76 = Math.max(n76, n78);
                }
                if (object2 != null && ((Lake)object2).level() >= n76 && n7 < ((Lake)object2).level()) {
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "BANK-LAKE-FLUSH surface " + n76 + " -> " + ((Lake)object2).level());
                    }
                    n76 = ((Lake)object2).level();
                }
                if (n3 != Integer.MIN_VALUE) {
                    n76 = Math.max(n76, n3);
                }
                double d20 = columnPlan5.distance() - columnPlan5.width();
                double d21 = Math.min(2.0, riverEngine.config().bankSlope() + riverEngine.config().bankSteepFactor() * Math.pow(columnPlan5.slope(), 0.8));
                double d22 = RiverCarverFeature.a((double)n76, d20, d21, columnPlan5.width(), Math.max(5.0, columnPlan5.width() * riverEngine.config().bankWidth()), (double)n7);
                double d23 = (double)n76 + columnPlan5.bankBlend() * (double)(n7 - n76);
                double d24 = (RiverCarverFeature.d(n49, n8) - 0.5) * riverEngine.config().bankNoise();
                int n79 = Mth.clamp((int)Math.floor(Math.max(d22, d23) + d24), n76, Math.max(n76, n7));
                ColumnPlan columnPlan11 = riverEngine.altPlan(n49, n8);
                if (columnPlan11 != null && (n39 = RiverCarverFeature.a(riverEngine, columnPlan11, n7, d24)) != Integer.MAX_VALUE && (n38 = Math.max(n39, n34 = RiverCarverFeature.a(columnPlan5, columnPlan11))) < n79) {
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "ALT-MERGE was=" + n79 + " now=" + n38 + " altCut=" + n39 + " floor=" + n34 + " altW=" + String.format(Locale.ROOT, "%.1f", columnPlan11.width()) + " altD=" + String.format(Locale.ROOT, "%.1f", columnPlan11.distance()) + " altSurf=" + columnPlan11.waterSurfaceY());
                    }
                    n79 = n38;
                }
                n39 = Integer.MAX_VALUE;
                if (n79 >= n7) {
                    for (int j = 1; j <= 2; ++j) {
                        for (n38 = 0; n38 < 4; ++n38) {
                            int n80;
                            n37 = n49 + (n38 == 0 ? j : (n38 == 1 ? -j : 0));
                            n35 = n8 + (n38 == 2 ? j : (n38 == 3 ? -j : 0));
                            n36 = n37 - n16 + 2;
                            int n81 = n35 - n17 + 2;
                            if (n36 < 0 || n36 >= 20 || n81 < 0 || n81 >= 20 || nArray9[n80 = n36 * 20 + n81] == Integer.MIN_VALUE || Math.abs(dArray[n80] - columnPlan5.distance()) > 2.0) continue;
                            n39 = Math.min(n39, nArray9[n80] + 1 * (j - 1));
                        }
                    }
                }
                if (n39 != Integer.MAX_VALUE && n79 > n39 + 1 + 1) {
                    int n82 = Mth.clamp(n39 + 1, n76, Math.max(n76, n7));
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "BANK-TIE was=" + n79 + " now=" + n82 + " neighbourAllow=" + n39 + " surf=" + n76 + " ground=" + n7);
                    }
                    n79 = n82;
                }
                if (n79 < n7) {
                    int n83 = Integer.MAX_VALUE;
                    n38 = 0;
                    for (n37 = 0; n37 < 4; ++n37) {
                        int n84;
                        n35 = n49 + (n37 == 0 ? 1 : (n37 == 1 ? -1 : 0));
                        n36 = n8 + (n37 == 2 ? 1 : (n37 == 3 ? -1 : 0));
                        int n85 = n35 - n16 + 2;
                        int n86 = n36 - n17 + 2;
                        if (n85 < 0 || n85 >= 20 || n86 < 0 || n86 >= 20 || (n84 = nArray9[n85 * 20 + n86]) == Integer.MIN_VALUE) continue;
                        ++n38;
                        n83 = Math.min(n83, n84);
                    }
                    if (n38 >= 2 && n79 < n83 && (n37 = Math.min(Math.min(n83, n7), n79 + 2)) > n79) {
                        if (RiverCarverFeature.c(n49, n8)) {
                            RiverCarverFeature.b(n49, n8, "BANK-DIMPLE was=" + n79 + " now=" + n37 + " lowestBeside=" + n83 + " beside=" + n38 + " ground=" + n7);
                        }
                        n79 = n37;
                    }
                }
                if (n77 != Integer.MIN_VALUE && object2 == null && n79 < n7 && columnPlan5.distToOutlet() > 24.0 && (n33 = RiverCarverFeature.adjacentCoveringLakeLevel(riverEngine, n49, n8)) != Integer.MIN_VALUE && n7 > n33 && n79 <= n33 && columnPlan5.waterSurfaceY() < n33) {
                    n38 = Mth.clamp(n33 + 1, n79, n7);
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "LAKE-RIM-HOLD was=" + n79 + " now=" + n38 + " rim=" + n33 + " streamSurf=" + columnPlan5.waterSurfaceY() + " ground=" + n7 + " distToOutlet=" + String.format(Locale.ROOT, "%.1f", columnPlan5.distToOutlet()));
                    }
                    n79 = n38;
                }
                if (!Boolean.getBoolean("streamsreflowing.noedgegrade") && n77 == Integer.MIN_VALUE && n79 >= n7 && n7 > n76 && d20 <= 8.0) {
                    double d25 = Math.max(0.6, d21);
                    n37 = n76 + (int)Math.round(Math.max(0.0, d20) * d25);
                    n35 = n79;
                    n79 = Mth.clamp(n37, n76, n7);
                    if (n79 != n35 && RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "EDGE-GRADE was=" + n35 + " now=" + n79 + " graded=" + n37 + " ground=" + n7 + " surf=" + n76 + String.format(Locale.ROOT, " d=%.1f gradeSlope=%.2f", d20, d25));
                    }
                }
                if (nArray11[n54] > 0 && n79 > n76 && (n32 = Math.max(Math.max(n76, nArray12[n54]), n79 - nArray11[n54])) < n79) {
                    if (RiverCarverFeature.c(n49, n8)) {
                        RiverCarverFeature.b(n49, n8, "LIP-SHAVE cutTop " + n79 + " -> " + n32 + " floor=" + nArray12[n54] + " shave=" + nArray11[n54]);
                    }
                    n79 = n32;
                }
                if (RiverCarverFeature.c(n49, n8)) {
                    nArray4[n10 * 16 + i] = Math.max(nArray4[n10 * 16 + i], Math.max(n79, n7));
                    RiverCarverFeature.b(n49, n8, "BANK ref=" + n76 + " cutTop=" + n79 + " ground=" + n7 + " d=" + String.format(Locale.ROOT, "%.1f", d20) + " cut=" + (n7 > n79) + String.format(Locale.ROOT, " slope=%.3f effSlope=%.2f slopeLine=%.2f terrainLine=%.2f blend=%.3f jitter=%+.2f", columnPlan5.slope(), d21, d22, d23, columnPlan5.bankBlend(), d24));
                }
                int n87 = n7;
                if (n7 > n79) {
                    for (n38 = n7; n38 > n79; --n38) {
                        mutableBlockPos.set(n49, n38, n8);
                        BlockState blockState16 = worldGenLevel.getBlockState(mutableBlockPos);
                        if (blockState16.isAir() || blockState16.is(BlockTags.LEAVES)) continue;
                        if (blockState16.is(BlockTags.LOGS)) {
                            worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                            RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n49, n8, n38 + 1, n19);
                            bl2 = true;
                            continue;
                        }
                        worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                        bl2 = true;
                    }
                    n87 = n79;
                    nArray2[n10 * 16 + i] = n7 - n79;
                }
                if (RiverCarverFeature.c(worldGenLevel, mutableBlockPos, n49, n8, n7 + 1, n19)) {
                    bl2 = true;
                }
                if (n87 < n7 && RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n8, n16, n17, n87 + 1, n7)) {
                    bl2 = true;
                }
                if (n87 < n7) {
                    RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n49, n8, n87 + 1, n7 + 1, n20);
                }
                if (n2 == 0 || n87 < n18 || n87 >= n19 || !(blockState10 = worldGenLevel.getBlockState(blockPos = new BlockPos(n49, n87, n8))).is(Blocks.DIRT) && !blockState10.is(Blocks.COARSE_DIRT) && !blockState10.is(Blocks.GRASS_BLOCK)) continue;
                worldGenLevel.setBlock(blockPos, blockState3, 2);
                BlockPos blockPos2 = blockPos.above();
                if (worldGenLevel.getBlockState(blockPos2).isAir()) {
                    float f = randomSource.nextFloat();
                    if (f < 0.2f && blockPos2.getY() + 1 < n19 && worldGenLevel.getBlockState(blockPos2.above()).isAir()) {
                        worldGenLevel.setBlock(blockPos2, blockState5, 2);
                        worldGenLevel.setBlock(blockPos2.above(), blockState6, 2);
                    } else if (f < 0.55f) {
                        worldGenLevel.setBlock(blockPos2, blockState4, 2);
                    }
                }
                bl2 = true;
            }
        }
        for (n10 = 0; n10 < 16; ++n10) {
            for (int i = 0; i < 16; ++i) {
                int n88;
                int n89;
                int n90;
                int n91 = n10 * 16 + i;
                n8 = nArray4[n91];
                n7 = nArray5[n91];
                if (n8 == Integer.MIN_VALUE || n7 == Integer.MIN_VALUE || n8 > n7) continue;
                n6 = 0;
                int[] nArray15 = new int[24];
                for (n90 = -2; n90 <= 2; ++n90) {
                    for (n3 = -2; n3 <= 2; ++n3) {
                        if (n90 == 0 && n3 == 0) continue;
                        n89 = n10 + n90;
                        n88 = i + n3;
                        if (n89 < 0 || n89 > 15 || n88 < 0 || n88 > 15 || (n2 = nArray4[n89 * 16 + n88]) == Integer.MIN_VALUE) continue;
                        nArray15[n6++] = n2;
                    }
                }
                if (n6 < 4) continue;
                Arrays.sort(nArray15, 0, n6);
                n90 = nArray15[n6 / 2];
                n3 = Math.min(n90, n7 + 1);
                if (n3 - n8 < 2) continue;
                n89 = n16 + n10;
                n88 = n17 + i;
                n2 = nArray[n91];
                BlockState blockState17 = RiverCarverFeature.a(worldGenLevel, n89, n88, n2, n18);
                boolean bl11 = false;
                for (n = n8 + 1; n <= n3 && n <= n19; ++n) {
                    mutableBlockPos.set(n89, n, n88);
                    BlockState blockState18 = worldGenLevel.getBlockState(mutableBlockPos);
                    if (!blockState18.isAir()) continue;
                    if (!RiverCarverFeature.d(worldGenLevel, mutableBlockPos, n89, n, n88, n18)) break;
                    RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n89, n, n88, RiverCarverFeature.a(worldGenLevel, n89, n88, n2, Math.max(1, n3 - n), blockState17, n18), n18);
                    bl11 = true;
                }
                if (!bl11) continue;
                bl2 = true;
                if (RiverCarverFeature.c(n89, n88)) {
                    RiverCarverFeature.b(n89, n88, "DIP FILL contain " + n8 + " -> " + n3 + " (median " + n90 + ", holds " + n7 + ")");
                }
                if (!RiverCarverFeature.b(n89, n88)) continue;
                RiverCarverFeature.a(n89, n88, "  DIP FILL PASS contain " + n8 + " -> " + n3);
            }
        }
        for (n10 = 0; n10 < 16; ++n10) {
            for (int i = 0; i < 16; ++i) {
                int n92;
                int n93;
                int n94 = nArray3[n10 * 16 + i];
                if (n94 == Integer.MIN_VALUE) continue;
                n8 = 0;
                int[] nArray16 = new int[24];
                for (n6 = -2; n6 <= 2; ++n6) {
                    for (n93 = -2; n93 <= 2; ++n93) {
                        int n95;
                        if (n6 == 0 && n93 == 0) continue;
                        n92 = n10 + n6;
                        n3 = i + n93;
                        if (n92 < 0 || n92 > 15 || n3 < 0 || n3 > 15 || (n95 = nArray3[n92 * 16 + n3]) == Integer.MIN_VALUE) continue;
                        nArray16[n8++] = n95;
                    }
                }
                if (n8 < 4) continue;
                Arrays.sort(nArray16, 0, n8);
                n6 = nArray16[n8 / 2];
                if (n94 - n6 < 2) continue;
                n93 = n16 + n10;
                n92 = n17 + i;
                if (RiverCarverFeature.c(n93, n92)) {
                    RiverCarverFeature.b(n93, n92, "OUTLIER surface " + n94 + " vs median " + n6 + " -> re-levelled");
                }
                if (RiverCarverFeature.b(n93, n92)) {
                    RiverCarverFeature.a(n93, n92, "  OUTLIER PASS re-levelled " + n94 + " -> " + n6);
                }
                for (n3 = n94; n3 > n6; --n3) {
                    mutableBlockPos.set(n93, n3, n92);
                    BlockState blockState19 = worldGenLevel.getBlockState(mutableBlockPos);
                    if (blockState19.isAir() || blockState19.getFluidState().isEmpty() && n3 <= n6 + 1) continue;
                    worldGenLevel.setBlock(mutableBlockPos, blockState2, 2);
                }
                mutableBlockPos.set(n93, n6, n92);
                if (worldGenLevel.getBlockState(mutableBlockPos).isAir() && RiverCarverFeature.d(worldGenLevel, mutableBlockPos, n93, n6, n92, n18)) {
                    worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
                }
                bl2 = true;
            }
        }
        if (bl2) {
            RiverCarverFeature.a(worldGenLevel, riverEngine, n16, n17, nArray, columnPlanArray, n19);
            RiverCarverFeature.a(worldGenLevel, riverEngine, n16, n17, nArray, nArray2, columnPlanArray, n18, n19);
            RiverCarverFeature.b(worldGenLevel, n16, n17, nArray2);
            RiverCarverFeature.a(worldGenLevel, n16, n17, blockStateArray, blockStateArray2);
            if (bl) {
                RiverCarverFeature.a(worldGenLevel, riverEngine, n16, n17, nArray, blockStateArray, blockStateArray2, columnPlanArray, nArray10, n18, n19);
            }
        }
        RiverCarverFeature.a(worldGenLevel, mutableBlockPos, nArray6, n16, n17, n18, blockState);
        if (bl2 && !Boolean.getBoolean("streamsreflowing.nobakeflow")) {
            try {
                ServerLevel serverLevel = worldGenLevel.getLevel();
                StreamPrefetcher.submitGridBuild(serverLevel, riverEngine, RiverFlowFields.classifierFor(serverLevel), RiverFlowFields.orientationFor(serverLevel), Services.PLATFORM.flowVanillaRivers(), n16 >> 4, n17 >> 4, false, null);
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
        ChunkPlanCache.put(riverEngine, n16, n17, columnPlanArray);
        RiverCarverFeature.a(n16, n17, columnPlanArray);
        return bl2;
    }

    private static void a(int n, int n2, ColumnPlan[] columnPlanArray) {
        try {
            byte[] byArray = new byte[256];
            byte[] byArray2 = new byte[256];
            short[] sArray = new short[256];
            Arrays.fill(sArray, (short)Short.MIN_VALUE);
            for (int i = 0; i < columnPlanArray.length && i < byArray.length; ++i) {
                double d;
                double d2;
                double d3;
                ColumnPlan columnPlan = columnPlanArray[i];
                if (columnPlan == null || !columnPlan.inChannel() || (d3 = Math.sqrt((d2 = columnPlan.fluidFlowDirX()) * d2 + (d = columnPlan.fluidFlowDirZ()) * d)) < 0.5) continue;
                byArray[i] = (byte)Math.round(d2 / d3 * 127.0);
                byArray2[i] = (byte)Math.round(d / d3 * 127.0);
                int n3 = columnPlan.waterSurfaceY();
                sArray[i] = (short)(n3 < Short.MIN_VALUE || n3 > Short.MAX_VALUE ? Short.MIN_VALUE : (short)n3);
            }
            RawFlowTiles.put(n >> 4, n2 >> 4, new RawFlowTiles.Tile(byArray, byArray2, sArray));
            FlowTrace.tilesFromCarve.incrementAndGet();
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private static boolean a(WorldGenLevel worldGenLevel, int n, int n2, int[] nArray) {
        int n3;
        int n4;
        int n5 = 18;
        int[] nArray2 = new int[324];
        boolean[] blArray = new boolean[324];
        for (n4 = 0; n4 < 2; ++n4) {
            for (n3 = -1; n3 <= 15; ++n3) {
                boolean bl2 = false;
                for (int i = 0; i < 16 && !bl2; ++i) {
                    int n7 = n4 == 0 ? i : n3;
                    int n8 = n4 == 0 ? n3 : i;
                    int n9 = n4 == 0 ? i : n3 + 1;
                    int n10 = n4 == 0 ? n3 + 1 : i;
                    bl2 = RiverCarverFeature.a(nArray, n7, n8) || RiverCarverFeature.a(nArray, n9, n10);
                }
                if (!bl2) continue;
                int n6 = 0;
                for (int i = 0; i < 16; ++i) {
                    int n7 = n4 == 0 ? i : n3;
                    int n8 = n4 == 0 ? n3 : i;
                    int n9 = n4 == 0 ? i : n3 + 1;
                    int n10 = n4 == 0 ? n3 + 1 : i;
                    boolean bl = (RiverCarverFeature.a(nArray, n7, n8) || RiverCarverFeature.a(nArray, n9, n10)) && Math.abs(RiverCarverFeature.cachedHeightAt(worldGenLevel, n, n2, nArray2, blArray, n7, n8) - RiverCarverFeature.cachedHeightAt(worldGenLevel, n, n2, nArray2, blArray, n9, n10)) >= 3;
                    int n11 = n6 = bl ? n6 + 1 : 0;
                    if (n6 < 3) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static int cachedHeightAt(WorldGenLevel worldGenLevel, int n, int n2, int[] nArray, boolean[] blArray, int n3, int n4) {
        int n5 = (n3 + 1) * 18 + (n4 + 1);
        if (!blArray[n5]) {
            nArray[n5] = RiverCarverFeature.a(worldGenLevel, n + n3, n2 + n4);
            blArray[n5] = true;
        }
        return nArray[n5];
    }

    private static boolean a(int[] nArray, int n, int n2) {
        return n >= 0 && n < 16 && n2 >= 0 && n2 < 16 && nArray[n * 16 + n2] > 0;
    }

    private static void a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2, int[] nArray, int[] nArray2, ColumnPlan[] columnPlanArray, int n3, int n4) {
        int n5;
        int n6;
        BlockState blockState;
        int n7;
        int n8;
        int n9;
        int n10;
        int n11;
        int n12;
        int n13;
        int n14;
        int n15;
        int n16;
        int n17;
        int n18;
        int n19;
        int n20;
        int n21;
        int n22;
        int n23;
        int n24;
        int n25;
        if (Boolean.getBoolean("streamsreflowing.nobanksmooth")) {
            return;
        }
        if (!Boolean.getBoolean("streamsreflowing.alwaysbanksmooth") && !RiverCarverFeature.a(worldGenLevel, n, n2, nArray2)) {
            if (RiverCarverFeature.c(n, n2)) {
                RiverCarverFeature.b(n, n2, "SEAM-SKIP no wall at a border, a carve boundary or a stream drop -> blend not run");
            }
            return;
        }
        int n26 = Math.max(1, Integer.getInteger("streamsreflowing.banksmoothstep", 2));
        int n27 = Math.max(0, Integer.getInteger("streamsreflowing.banksmoothmincut", 4));
        int n28 = 20;
        int[] nArray3 = new int[400];
        boolean[] blArray = new boolean[400];
        boolean[] blArray2 = new boolean[400];
        boolean[] blArray3 = new boolean[400];
        boolean[] blArray4 = new boolean[400];
        boolean[] blArray5 = new boolean[400];
        Arrays.fill(nArray3, n3);
        for (int i = -6; i < 22; ++i) {
            for (n25 = -6; n25 < 22; ++n25) {
                ColumnPlan columnPlan;
                n24 = i >= 0 && i < 16 && n25 >= 0 && n25 < 16 ? ((columnPlan = columnPlanArray[i * 16 + n25]) != null && columnPlan != ColumnPlan.NONE && columnPlan.inCorridor() ? 1 : 0) : (int)(riverEngine.inCorridor(n + i, n2 + n25) ? 1 : 0);
                if (n24 == 0) continue;
                for (int j = -6; j <= 6; ++j) {
                    for (int k = -6; k <= 6; ++k) {
                        int n29 = i + j;
                        n23 = n25 + k;
                        if (n29 < -2 || n29 >= 18 || n23 < -2 || n23 >= 18) continue;
                        blArray3[(n29 + 2) * 20 + (n23 + 2)] = true;
                    }
                }
            }
        }
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (n25 = 0; n25 < 16; ++n25) {
            for (n24 = 0; n24 < 16; ++n24) {
                int n30;
                int n31 = (n25 + 2) * 20 + (n24 + 2);
                blArray4[n31] = true;
                ColumnPlan columnPlan = columnPlanArray[n25 * 16 + n24];
                Lake lake = RiverCarverFeature.a(riverEngine, n + n25, n2 + n24);
                if (columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inChannel()) {
                    nArray3[n31] = columnPlan.waterSurfaceY();
                    blArray[n31] = true;
                    blArray2[n31] = true;
                    continue;
                }
                if (lake != null) {
                    nArray3[n31] = lake.level();
                    blArray[n31] = true;
                    blArray2[n31] = true;
                    continue;
                }
                n23 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n + n25, n2 + n24);
                int n32 = Math.min(n23, n30 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, n + n25, n2 + n24)) - 1;
                BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos.set(n + n25, n32, n2 + n24));
                if (blockState2.is(BlockTags.LOGS) || blockState2.is(BlockTags.LEAVES)) {
                    BlockState blockState3;
                    n22 = Math.max(n3, n32 - 48);
                    for (n21 = n32; n21 > n22 && ((blockState3 = worldGenLevel.getBlockState(mutableBlockPos.set(n + n25, n21, n2 + n24))).is(BlockTags.LOGS) || blockState3.is(BlockTags.LEAVES)); --n21) {
                    }
                    n32 = n21;
                    blArray[n31] = true;
                }
                nArray3[n31] = n32;
            }
        }
        n25 = 0;
        n24 = 0;
        boolean bl = false;
        boolean bl2 = false;
        for (n20 = 0; n20 < 16; ++n20) {
            n25 |= blArray3[40 + (n20 + 2)] ? 1 : 0;
            n24 |= blArray3[340 + (n20 + 2)] ? 1 : 0;
            bl |= blArray3[(n20 + 2) * 20 + 2];
            bl2 |= blArray3[(n20 + 2) * 20 + 17];
        }
        for (n20 = -2; n20 <= 17; ++n20) {
            for (n23 = -2; n23 <= -1; ++n23) {
                if (n25 != 0) {
                    RiverCarverFeature.a(worldGenLevel, riverEngine, n, n2, n23, n20, 20, nArray3, blArray2, blArray4, blArray5, n3);
                }
                if (!bl) continue;
                RiverCarverFeature.a(worldGenLevel, riverEngine, n, n2, n20, n23, 20, nArray3, blArray2, blArray4, blArray5, n3);
            }
            for (n23 = 16; n23 <= 17; ++n23) {
                if (n24 != 0) {
                    RiverCarverFeature.a(worldGenLevel, riverEngine, n, n2, n23, n20, 20, nArray3, blArray2, blArray4, blArray5, n3);
                }
                if (!bl2) continue;
                RiverCarverFeature.a(worldGenLevel, riverEngine, n, n2, n20, n23, 20, nArray3, blArray2, blArray4, blArray5, n3);
            }
        }
        int[] nArray4 = (int[])nArray3.clone();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        int[] nArray5 = new int[400];
        int[] nArray6 = new int[400];
        for (n19 = 0; n19 < 20; ++n19) {
            for (n21 = 0; n21 < 20; ++n21) {
                n22 = n19 * 20 + n21;
                if (nArray3[n22] == n3) {
                    nArray5[n22] = n3;
                    continue;
                }
                int n33 = nArray3[n22];
                if (n19 > 0 && nArray3[n22 - 20] != n3) {
                    n33 = Math.min(n33, nArray3[n22 - 20]);
                }
                if (n19 < 19 && nArray3[n22 + 20] != n3) {
                    n33 = Math.min(n33, nArray3[n22 + 20]);
                }
                if (n21 > 0 && nArray3[n22 - 1] != n3) {
                    n33 = Math.min(n33, nArray3[n22 - 1]);
                }
                if (n21 < 19 && nArray3[n22 + 1] != n3) {
                    n33 = Math.min(n33, nArray3[n22 + 1]);
                }
                nArray5[n22] = n33;
            }
        }
        for (n19 = 0; n19 < 20; ++n19) {
            for (n21 = 0; n21 < 20; ++n21) {
                n22 = n19 * 20 + n21;
                if (nArray3[n22] == n3) {
                    nArray6[n22] = n3;
                    continue;
                }
                int n34 = nArray5[n22];
                if (n19 > 0 && nArray5[n22 - 20] != n3) {
                    n34 = Math.max(n34, nArray5[n22 - 20]);
                }
                if (n19 < 19 && nArray5[n22 + 20] != n3) {
                    n34 = Math.max(n34, nArray5[n22 + 20]);
                }
                if (n21 > 0 && nArray5[n22 - 1] != n3) {
                    n34 = Math.max(n34, nArray5[n22 - 1]);
                }
                if (n21 < 19 && nArray5[n22 + 1] != n3) {
                    n34 = Math.max(n34, nArray5[n22 + 1]);
                }
                nArray6[n22] = n34;
            }
        }
        n19 = 0;
        n21 = 0;
        for (n22 = 0; n22 < 400; ++n22) {
            int n35 = n22 / 20 - 2;
            n18 = n22 % 20 - 2;
            if (n35 >= 0 && n35 < 16 && n18 >= 0 && n18 < 16 || !blArray3[n22]) continue;
            ++n19;
            if (blArray4[n22]) continue;
            ++n21;
        }
        if (RiverCarverFeature.c(n + 8, n2 + 8)) {
            RiverCarverFeature.b(n + 8, n2 + 8, "SEAM-TRUST apronZoned=" + n19 + " untrusted=" + n21 + " (untrusted apron = a seam this side cannot close)");
        }
        boolean[] blArray6 = new boolean[400];
        BlockPos.MutableBlockPos mutableBlockPos3 = new BlockPos.MutableBlockPos();
        for (n22 = 0; n22 < blArray6.length; ++n22) {
            if (!blArray3[n22] || nArray3[n22] == n3) continue;
            int n36 = n + n22 / 20 - 2;
            n18 = n2 + n22 % 20 - 2;
            BlockState blockState4 = worldGenLevel.getBlockState(mutableBlockPos3.set(n36, nArray3[n22], n18));
            blArray6[n22] = blockState4.is(BlockTags.LOGS) || blockState4.is(BlockTags.LEAVES);
        }
        int[] object = (int[])nArray3.clone();
        byte[] seedMod6 = new byte[400];
        for (int seedX = -2; seedX < 18; ++seedX) {
            for (int seedZ = -2; seedZ < 18; ++seedZ) {
                seedMod6[(seedX + 2) * 20 + (seedZ + 2)] = (byte)Math.floorMod(Mth.getSeed(n + seedX, 0, n2 + seedZ), 6L);
            }
        }
        for (n22 = 0; n22 < 32; ++n22) {
            boolean bl3 = false;
            for (n18 = -2; n18 < 18; ++n18) {
                for (int i = -2; i < 18; ++i) {
                    n17 = (n18 + 2) * 20 + (i + 2);
                    if (!blArray3[n17] || blArray[n17] || !blArray4[n17] || nArray3[n17] == n3) continue;
                    int n37 = n16 = n18 >= 0 && n18 < 16 && i >= 0 && i < 16 ? 1 : 0;
                    if (n16 == 0 || nArray2[n18 * 16 + i] < n27) continue;
                    n15 = n18 + 2;
                    n14 = i + 2;
                    n13 = n + n18;
                    n12 = n2 + i;
                    long l = seedMod6[n17];
                    n11 = Math.min(6, n26 + n22);
                    n10 = Math.max(1, n11 + (l == 0L ? -1 : (l == 5L ? 1 : 0)));
                    n9 = nArray3[n17];
                    n8 = Integer.MIN_VALUE;
                    if (n15 > 0 && !blArray6[n17 - 20]) {
                        n8 = Math.max(n8, nArray6[n17 - 20]);
                    }
                    if (n15 < 19 && !blArray6[n17 + 20]) {
                        n8 = Math.max(n8, nArray6[n17 + 20]);
                    }
                    if (n14 > 0 && !blArray6[n17 - 1]) {
                        n8 = Math.max(n8, nArray6[n17 - 1]);
                    }
                    if (n14 < 19 && !blArray6[n17 + 1]) {
                        n8 = Math.max(n8, nArray6[n17 + 1]);
                    }
                    if (n8 != Integer.MIN_VALUE && n8 - nArray3[n17] > 16) continue;
                    int n38 = n8 - n10;
                    if (n15 > 0 && blArray2[n17 - 20]) {
                        n38 = Math.min(n38, nArray3[n17 - 20] + 1);
                    }
                    if (n15 < 19 && blArray2[n17 + 20]) {
                        n38 = Math.min(n38, nArray3[n17 + 20] + 1);
                    }
                    if (n14 > 0 && blArray2[n17 - 1]) {
                        n38 = Math.min(n38, nArray3[n17 - 1] + 1);
                    }
                    if (n14 < 19 && blArray2[n17 + 1]) {
                        n38 = Math.min(n38, nArray3[n17 + 1] + 1);
                    }
                    if (nArray3[n17] >= (n38 = Math.min(n38, nArray4[n17])) || RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n13, n12, nArray3[n17] + 1, n38)) continue;
                    n7 = nArray3[n17];
                    int n39 = nArray4[n17];
                    blockState = RiverCarverFeature.a(worldGenLevel, n13, n12, n39, n3);
                    n6 = nArray3[n17] + 1;
                    while (n6 <= n38) {
                        mutableBlockPos2.set(n13, n6, n12);
                        BlockState blockState5 = worldGenLevel.getBlockState(mutableBlockPos2);
                        if (!blockState5.getFluidState().isEmpty()) {
                            blArray[n17] = true;
                            break;
                        }
                        if (RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n13, n6, n12)) {
                            blArray[n17] = true;
                            break;
                        }
                        mutableBlockPos2.set(n13, n6, n12);
                        if (!RiverCarverFeature.d(worldGenLevel, mutableBlockPos2, n13, n6, n12, n3)) {
                            blArray[n17] = true;
                            break;
                        }
                        BlockState blockState6 = n6 == n38 ? RiverCarverFeature.a(worldGenLevel, n13, n12, n38, n3, RiverCarverFeature.a(worldGenLevel, n13, n12, n39, 0, blockState, n3)) : RiverCarverFeature.a(worldGenLevel, n13, n12, n39, n38 - n6, blockState, n3);
                        RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n13, n6, n12, blockState6, n3);
                        RiverCarverFeature.b(worldGenLevel, mutableBlockPos2, n13, n6 - 1, n12, blockState, n3);
                        n7 = n6++;
                    }
                    if (n7 <= nArray3[n17]) continue;
                    if (RiverCarverFeature.c(n13, n12)) {
                        RiverCarverFeature.b(n13, n12, "FILL iter=" + n22 + " from=" + n9 + " to=" + n7 + " target=" + n38 + " pulledBy=" + n8 + " step=" + n10 + " orig=" + nArray4[n17]);
                    }
                    nArray3[n17] = n7;
                    nArray6[n17] = Math.max(nArray6[n17], n7);
                    if (n18 >= 0 && n18 < 16 && i >= 0 && i < 16) {
                        nArray[n18 * 16 + i] = Mth.clamp(n7, n3, n4);
                    }
                    bl3 = true;
                }
            }
            if (!bl3) break;
        }
        n22 = 3;
        int n40 = 2;
        for (n18 = 0; n18 < 2; ++n18) {
            boolean bl4 = false;
            for (n17 = -2; n17 < 18; ++n17) {
                for (n16 = -2; n16 < 18; ++n16) {
                    int n41;
                    n15 = (n17 + 2) * 20 + (n16 + 2);
                    if (!blArray3[n15] || blArray[n15] || !blArray4[n15] || nArray3[n15] == n3) continue;
                    int n42 = n14 = n17 >= 0 && n17 < 16 && n16 >= 0 && n16 < 16 ? 1 : 0;
                    if (n14 == 0) continue;
                    n13 = nArray2[n17 * 16 + n16] >= n27 ? 1 : 0;
                    for (n12 = -3; n12 <= 3 && n13 == 0; ++n12) {
                        if (n12 == 0) continue;
                        n41 = n17 + n12;
                        if (n41 >= -2 && n41 < 18) {
                            n5 = (n41 + 2) * 20 + (n16 + 2);
                            n13 = nArray3[n5] != n3 && nArray3[n5] > object[n5] ? 1 : 0;
                        }
                        n5 = n16 + n12;
                        if (n13 != 0 || n5 < -2 || n5 >= 18) continue;
                        n11 = (n17 + 2) * 20 + (n5 + 2);
                        n13 = nArray3[n11] != n3 && nArray3[n11] > object[n11] ? 1 : 0;
                    }
                    if (n13 == 0) continue;
                    n12 = Integer.MIN_VALUE;
                    for (n41 = 0; n41 < 2; ++n41) {
                        int n43;
                        n5 = Integer.MIN_VALUE;
                        n11 = Integer.MIN_VALUE;
                        for (n10 = 1; n10 <= 3; ++n10) {
                            n9 = n41 == 0 ? n17 + n10 : n17;
                            int n44 = n8 = n41 == 0 ? n16 : n16 + n10;
                            if (n9 > 17 || n8 > 17) break;
                            n43 = nArray3[(n9 + 2) * 20 + (n8 + 2)];
                            if (n43 == n3 || n43 < nArray3[n15] + 2) continue;
                            n5 = n43;
                            break;
                        }
                        if (n5 == Integer.MIN_VALUE) continue;
                        for (n10 = 1; n10 <= 3; ++n10) {
                            n9 = n41 == 0 ? n17 - n10 : n17;
                            int n45 = n8 = n41 == 0 ? n16 : n16 - n10;
                            if (n9 < -2 || n8 < -2) break;
                            n43 = nArray3[(n9 + 2) * 20 + (n8 + 2)];
                            if (n43 == n3 || n43 < nArray3[n15] + 2) continue;
                            n11 = n43;
                            break;
                        }
                        if (n11 == Integer.MIN_VALUE) continue;
                        n12 = Math.max(n12, Math.min(n5, n11));
                    }
                    if (n12 == Integer.MIN_VALUE) continue;
                    n41 = n17 + 2;
                    n5 = n16 + 2;
                    if (n41 > 0 && blArray2[n15 - 20]) {
                        n12 = Math.min(n12, nArray3[n15 - 20] + 1);
                    }
                    if (n41 < 19 && blArray2[n15 + 20]) {
                        n12 = Math.min(n12, nArray3[n15 + 20] + 1);
                    }
                    if (n5 > 0 && blArray2[n15 - 1]) {
                        n12 = Math.min(n12, nArray3[n15 - 1] + 1);
                    }
                    if (n5 < 19 && blArray2[n15 + 1]) {
                        n12 = Math.min(n12, nArray3[n15 + 1] + 1);
                    }
                    if (nArray3[n15] >= (n12 = Math.min(n12, nArray4[n15]))) continue;
                    n11 = n + n17;
                    n10 = n2 + n16;
                    if (RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n11, n10, nArray3[n15] + 1, n12)) {
                        blArray[n15] = true;
                        continue;
                    }
                    n9 = nArray3[n15];
                    n8 = nArray4[n15];
                    BlockState blockState7 = RiverCarverFeature.a(worldGenLevel, n11, n10, n8, n3);
                    n7 = nArray3[n15] + 1;
                    while (n7 <= n12) {
                        mutableBlockPos2.set(n11, n7, n10);
                        BlockState blockState8 = worldGenLevel.getBlockState(mutableBlockPos2);
                        if (!blockState8.getFluidState().isEmpty()) {
                            blArray[n15] = true;
                            break;
                        }
                        if (RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n11, n7, n10)) {
                            blArray[n15] = true;
                            break;
                        }
                        mutableBlockPos2.set(n11, n7, n10);
                        if (!RiverCarverFeature.d(worldGenLevel, mutableBlockPos2, n11, n7, n10, n3)) {
                            blArray[n15] = true;
                            break;
                        }
                        blockState = n7 == n12 ? RiverCarverFeature.a(worldGenLevel, n11, n10, n12, n3, RiverCarverFeature.a(worldGenLevel, n11, n10, n8, 0, blockState7, n3)) : RiverCarverFeature.a(worldGenLevel, n11, n10, n8, n12 - n7, blockState7, n3);
                        RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n11, n7, n10, blockState, n3);
                        RiverCarverFeature.b(worldGenLevel, mutableBlockPos2, n11, n7 - 1, n10, blockState7, n3);
                        n9 = n7++;
                    }
                    if (n9 <= nArray3[n15]) continue;
                    nArray3[n15] = n9;
                    if (n14 != 0) {
                        nArray[n17 * 16 + n16] = Mth.clamp(n9, n3, n4);
                    }
                    bl4 = true;
                }
            }
            if (!bl4) break;
        }
        n18 = 4;
        int n46 = 2;
        int[] nArray7 = (int[])nArray3.clone();
        for (n16 = 0; n16 < 16; ++n16) {
            for (n15 = 0; n15 < 16; ++n15) {
                BlockState blockState9;
                int n47;
                n14 = (n16 + 2) * 20 + (n15 + 2);
                if (!blArray3[n14] || blArray[n14] || !blArray4[n14] || nArray7[n14] == n3 || blArray2[n14]) continue;
                n13 = nArray2[n16 * 16 + n15] >= n27 ? 1 : 0;
                for (n12 = -3; n12 <= 3 && n13 == 0; ++n12) {
                    if (n12 == 0) continue;
                    n47 = n16 + n12;
                    if (n47 >= 0 && n47 < 16) {
                        int n48 = n13 = nArray2[n47 * 16 + n15] >= n27 ? 1 : 0;
                        if (n13 == 0) {
                            n5 = (n47 + 2) * 20 + (n15 + 2);
                            n13 = nArray7[n5] != n3 && nArray7[n5] > object[n5] ? 1 : 0;
                        }
                    }
                    n5 = n15 + n12;
                    if (n13 != 0 || n5 < 0 || n5 >= 16) continue;
                    int n49 = n13 = nArray2[n16 * 16 + n5] >= n27 ? 1 : 0;
                    if (n13 != 0) continue;
                    n11 = (n16 + 2) * 20 + (n5 + 2);
                    n13 = nArray7[n11] != n3 && nArray7[n11] > object[n11] ? 1 : 0;
                }
                if (n13 == 0) continue;
                n12 = Integer.MIN_VALUE;
                for (n47 = 0; n47 < 2; ++n47) {
                    int n50;
                    int n51;
                    n5 = Integer.MIN_VALUE;
                    n11 = Integer.MIN_VALUE;
                    for (n10 = 1; n10 <= 2; ++n10) {
                        n51 = n47 == 0 ? n16 + n10 : n16;
                        int n52 = n8 = n47 == 0 ? n15 : n15 + n10;
                        if (n51 > 15 || n8 > 15) break;
                        n50 = nArray7[(n51 + 2) * 20 + (n8 + 2)];
                        if (n50 == n3 || n50 > nArray7[n14] - 4) continue;
                        n5 = n50;
                        break;
                    }
                    if (n5 == Integer.MIN_VALUE) continue;
                    for (n10 = 1; n10 <= 2; ++n10) {
                        n51 = n47 == 0 ? n16 - n10 : n16;
                        int n53 = n8 = n47 == 0 ? n15 : n15 - n10;
                        if (n51 < 0 || n8 < 0) break;
                        n50 = nArray7[(n51 + 2) * 20 + (n8 + 2)];
                        if (n50 == n3 || n50 > nArray7[n14] - 4) continue;
                        n11 = n50;
                        break;
                    }
                    if (n11 == Integer.MIN_VALUE) continue;
                    n12 = Math.max(n12, Math.max(n5, n11) + 1);
                }
                if (n12 == Integer.MIN_VALUE || n12 >= nArray7[n14]) continue;
                n47 = n + n16;
                n5 = n2 + n15;
                if (RiverCarverFeature.a(worldGenLevel, mutableBlockPos2, n47, n5, n12 + 1, nArray7[n14] + 2)) {
                    blArray[n14] = true;
                    continue;
                }
                n11 = 0;
                for (n10 = nArray7[n14]; n10 > n12 && (blockState9 = worldGenLevel.getBlockState(mutableBlockPos2.set(n47, n10, n5))).getFluidState().isEmpty(); --n10) {
                    if (blockState9.isAir()) continue;
                    worldGenLevel.setBlock(mutableBlockPos2, Blocks.AIR.defaultBlockState(), 2);
                    n11 = 1;
                }
                if (n11 == 0) continue;
                if (RiverCarverFeature.c(n47, n5)) {
                    RiverCarverFeature.b(n47, n5, "PILLAR-CULL was=" + nArray7[n14] + " now=" + n12);
                }
                RiverCarverFeature.c(worldGenLevel, mutableBlockPos2, n47, n5, n12 + 1, n4);
                nArray3[n14] = n12;
                nArray[n16 * 16 + n15] = Mth.clamp(n12, n3, n4);
            }
        }
        boolean[] blArray7 = new boolean[400];
        for (n15 = 0; n15 < blArray7.length; ++n15) {
            blArray7[n15] = blArray3[n15] && blArray4[n15] && !blArray[n15] && nArray3[n15] != n3 && nArray3[n15] > nArray4[n15];
        }
        BlockState blockState10 = Blocks.AIR.defaultBlockState();
        boolean[] blArray8 = new boolean[400];
        int[] nArray8 = new int[400];
        int[] nArray9 = new int[400];
        for (int i = 0; i < blArray7.length; ++i) {
            int n54;
            int n55;
            int n56;
            int n57;
            int n58;
            int n59;
            if (!blArray7[i] || blArray8[i]) continue;
            n5 = 0;
            n11 = 0;
            n10 = 0;
            nArray8[n11++] = i;
            blArray8[i] = true;
            while (n5 < n11) {
                n59 = nArray8[n5++];
                nArray9[n10++] = n59;
                n8 = n59 / 20;
                n58 = n59 % 20;
                if (n8 > 0 && blArray7[n59 - 20] && !blArray8[n59 - 20]) {
                    blArray8[n59 - 20] = true;
                    nArray8[n11++] = n59 - 20;
                }
                if (n8 < 19 && blArray7[n59 + 20] && !blArray8[n59 + 20]) {
                    blArray8[n59 + 20] = true;
                    nArray8[n11++] = n59 + 20;
                }
                if (n58 > 0 && blArray7[n59 - 1] && !blArray8[n59 - 1]) {
                    blArray8[n59 - 1] = true;
                    nArray8[n11++] = n59 - 1;
                }
                if (n58 >= 19 || !blArray7[n59 + 1] || blArray8[n59 + 1]) continue;
                blArray8[n59 + 1] = true;
                nArray8[n11++] = n59 + 1;
            }
            n59 = 0;
            n8 = 0;
            n58 = 0;
            for (n7 = 0; n7 < n10; ++n7) {
                n57 = nArray9[n7];
                n59 = Math.max(n59, nArray3[n57] - nArray4[n57]);
                n56 = n57 / 20;
                n6 = n57 % 20;
                for (n55 = 0; n55 < 4; ++n55) {
                    switch (n55) {
                        case 0: {
                            n54 = n56 > 0 ? n57 - 20 : -1;
                            break;
                        }
                        case 1: {
                            n54 = n56 < 19 ? n57 + 20 : -1;
                            break;
                        }
                        case 2: {
                            n54 = n6 > 0 ? n57 - 1 : -1;
                            break;
                        }
                        default: {
                            int n60 = n54 = n6 < 19 ? n57 + 1 : -1;
                        }
                    }
                    if (n54 < 0 || blArray7[n54] || nArray3[n54] == n3) continue;
                    ++n8;
                    if (nArray3[n54] < nArray3[n57] - (n26 + 1)) continue;
                    ++n58;
                }
            }
            int n61 = n7 = n10 <= 24 && n59 > n26 && (n8 == 0 || n58 * 4 < n8) ? 1 : 0;
            if (n7 == 0) continue;
            for (n57 = 0; n57 < n10; ++n57) {
                int n62;
                n56 = nArray9[n57];
                n6 = n56 / 20 - 2;
                n55 = n56 % 20 - 2;
                int n63 = n54 = n6 >= 0 && n6 < 16 && n55 >= 0 && n55 < 16 ? 1 : 0;
                if (n54 == 0) continue;
                int n64 = n + n6;
                int n65 = n2 + n55;
                for (int j = n62 = nArray3[n56]; j > nArray4[n56]; --j) {
                    mutableBlockPos2.set(n64, j, n65);
                    BlockState blockState11 = worldGenLevel.getBlockState(mutableBlockPos2);
                    if (!blockState11.getFluidState().isEmpty()) break;
                    if (blockState11.isAir()) continue;
                    worldGenLevel.setBlock(mutableBlockPos2, blockState10, 2);
                }
                nArray3[n56] = nArray4[n56];
                if (n54 == 0) continue;
                nArray[n6 * 16 + n55] = Mth.clamp(nArray4[n56], n3, n4);
            }
        }
    }

    private static Lake a(RiverEngine riverEngine, int n, int n2) {
        Lake lake = riverEngine.lakeNear(n, n2, 2);
        if (lake != null && lake.digDepth() == 0 && lake.coverageSmooth(n, n2) > 0.5) {
            return lake;
        }
        Lake lake2 = riverEngine.lakeAt(n, n2);
        return lake2 != null && lake2.digDepth() > 0 ? lake2 : null;
    }

    private static void a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2, int n3, int n4, int n5, int[] nArray, boolean[] blArray, boolean[] blArray2, boolean[] blArray3, int n6) {
        int n7 = (n3 + 2) * n5 + (n4 + 2);
        if (nArray[n7] != n6) {
            return;
        }
        int n8 = n + n3;
        int n9 = n2 + n4;
        try {
            ColumnPlan columnPlan = riverEngine.plan(n8, n9);
            if (columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inChannel()) {
                blArray[n7] = true;
                nArray[n7] = columnPlan.waterSurfaceY();
                return;
            }
            Lake lake = RiverCarverFeature.a(riverEngine, n8, n9);
            if (lake != null) {
                blArray[n7] = true;
                nArray[n7] = lake.level();
                return;
            }
            if (!RiverCarverFeature.b(worldGenLevel, n8, n9)) {
                if (columnPlan != null && columnPlan != ColumnPlan.NONE && columnPlan.inCorridor()) {
                    int n10 = RiverCarverFeature.a(riverEngine, worldGenLevel, columnPlan, n8, n9, n6);
                    if (n10 != Integer.MIN_VALUE) {
                        nArray[n7] = n10;
                        blArray2[n7] = true;
                    }
                    return;
                }
                int n11 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n8, n9);
                int n12 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, n8, n9);
                nArray[n7] = Math.min(n11, n12) - 1;
                blArray2[n7] = true;
                return;
            }
            blArray2[n7] = true;
            blArray3[n7] = RiverCarverFeature.c(worldGenLevel, n8, n9);
            int n13 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n8, n9);
            int n14 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, n8, n9);
            int n15 = Math.min(n13, n14) - 1;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 48 && n15 > n6; --n15, ++i) {
                mutableBlockPos.set(n8, n15, n9);
                BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
                if (!blockState.is(BlockTags.LOGS) && !blockState.is(BlockTags.LEAVES) && !blockState.isAir()) break;
            }
            nArray[n7] = n15;
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private static boolean a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        for (int i = n3; i <= n4; ++i) {
            BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos.set(n, i, n2));
            if (!blockState.is(BlockTags.LOGS) && !blockState.is(BlockTags.LEAVES)) continue;
            return true;
        }
        return false;
    }

    private static int a(WorldGenLevel worldGenLevel, int n, int n2) {
        int n3 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n, n2);
        int n4 = worldGenLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, n, n2);
        return Math.min(n3, n4) - 1;
    }

    private static boolean b(WorldGenLevel worldGenLevel, int n, int n2) {
        ChunkAccess chunkAccess = worldGenLevel.getChunk(n >> 4, n2 >> 4, ChunkStatus.EMPTY, false);
        if (chunkAccess == null) {
            return false;
        }
        return chunkAccess.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES);
    }

    private static boolean c(WorldGenLevel worldGenLevel, int n, int n2) {
        ChunkAccess chunkAccess = worldGenLevel.getChunk(n >> 4, n2 >> 4, ChunkStatus.EMPTY, false);
        if (chunkAccess == null) {
            return false;
        }
        ChunkStatus chunkStatus = chunkAccess.getPersistedStatus();
        return chunkStatus.isOrAfter(ChunkStatus.FEATURES) && !chunkStatus.isOrAfter(ChunkStatus.LIGHT);
    }

    private static List<BankStyle> a(WorldGenLevel worldGenLevel) {
        RegistryAccess registryAccess = worldGenLevel.registryAccess();
        if (ad != registryAccess) {
            ArrayList arrayList = new ArrayList();
            worldGenLevel.registryAccess().registry(BankStyle.REGISTRY_KEY).ifPresent(registry -> registry.forEach(arrayList::add));
            ae = List.copyOf(arrayList);
            ad = registryAccess;
        }
        return ae;
    }

    private static void a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2, int[] nArray, BlockState[] blockStateArray, BlockState[] blockStateArray2, ColumnPlan[] columnPlanArray, int[] nArray2, int n3, int n4) {
        List<BankStyle> list = BankStyleLoader.configStyles();
        List<BankStyle> list2 = RiverCarverFeature.a(worldGenLevel);
        if (list.isEmpty() && list2.isEmpty()) {
            return;
        }
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        HashMap<Holder<Biome>, BankStyle> hashMap = new HashMap<Holder<Biome>, BankStyle>();
        HashMap<Long, Holder<Biome>> hashMap2 = new HashMap<Long, Holder<Biome>>();
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int n5;
                int n6 = n + i;
                int n7 = n2 + j;
                int n8 = i * 16 + j;
                int n9 = nArray[n8];
                ColumnPlan columnPlan = columnPlanArray[n8];
                if (columnPlan == null) {
                    columnPlan = riverEngine.plan(n6, n7);
                }
                boolean bl = false;
                if (columnPlan != ColumnPlan.NONE && columnPlan.inCorridor()) {
                    n5 = columnPlan.waterSurfaceY();
                } else {
                    int n10 = nArray2[n8];
                    if (n10 == -2147483647) {
                        n10 = riverEngine.lakeSurfaceAt(n6, n7);
                    }
                    if (n10 == Integer.MIN_VALUE) continue;
                    n5 = n10;
                    bl = true;
                }
                RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n6, n7, n9, blockStateArray[n8], blockStateArray2[n8], n5, !bl, list, list2, hashMap, hashMap2, n3, n4, "bank");
            }
        }
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, BlockState blockState, BlockState blockState2, int n4, boolean bl, List<BankStyle> list, List<BankStyle> list2, Map<Holder<Biome>, BankStyle> map, Map<Long, Holder<Biome>> map2, int n5, int n6, String string) {
        int n7;
        int n8;
        int n9 = Integer.MIN_VALUE;
        int n10 = Math.max(n5 + 1, n4 - 24);
        for (int i = Math.min(n6, n3); i >= n10; --i) {
            mutableBlockPos.set(n, i, n2);
            BlockState blockState3 = worldGenLevel.getBlockState(mutableBlockPos);
            if (blockState3.isAir() || !blockState3.getFluidState().isEmpty() || !blockState3.canOcclude()) continue;
            n9 = i;
            break;
        }
        if (n9 == Integer.MIN_VALUE || bl && n9 >= n3) {
            if (RiverCarverFeature.c(n, n2)) {
                RiverCarverFeature.b(n, n2, "SKIN-SKIP pass=" + string + " why=" + (n9 == Integer.MIN_VALUE ? "nosurface" : "notlowered") + " surfaceY=" + n9 + " scanTop=" + n3 + " ws=" + n4);
            }
            return;
        }
        mutableBlockPos.set(n, n9, n2);
        BlockState blockState4 = worldGenLevel.getBlockState(mutableBlockPos);
        if (blockState4.is(Blocks.BEDROCK)) {
            if (RiverCarverFeature.c(n, n2)) {
                RiverCarverFeature.b(n, n2, "SKIN-SKIP pass=" + string + " why=bedrock surfaceY=" + n9 + " scanTop=" + n3);
            }
            return;
        }
        int n11 = n + (int)(RiverCarverFeature.c(n, 0, n2) * 5.0) - 2;
        int n12 = n2 + (int)(RiverCarverFeature.c(n2, 1, n) * 5.0) - 2;
        int n13 = n9 >> 2;
        long l2 = (long)(n11 >> 2) << 32 ^ (long)(n12 >> 2) & 0xFFFFFFFFL;
        Holder<Biome> holder2 = map2.computeIfAbsent(l2, l -> worldGenLevel.getUncachedNoiseBiome(n11 >> 2, n13, n12 >> 2));
        BankStyle bankStyle = map.computeIfAbsent(holder2, holder -> RiverCarverFeature.a(list, list2, holder));
        if (bankStyle == null || !bankStyle.enabled()) {
            if (RiverCarverFeature.c(n, n2)) {
                RiverCarverFeature.b(n, n2, "SKIN-SKIP pass=" + string + " why=" + (bankStyle == null ? "nostyle" : "styleoff") + " surfaceY=" + n9 + " scanTop=" + n3 + " biome=" + holder2.unwrapKey().map(resourceKey -> resourceKey.location().toString()).orElse("?"));
            }
            return;
        }
        int n14 = n4 - bankStyle.waterlineBelow();
        int n15 = n4 + bankStyle.waterlineAbove();
        double d = (double)bankStyle.underwaterNoise() * 3.0;
        double d2 = (double)bankStyle.aboveWaterNoise() * 3.0;
        double d3 = n9 - n14;
        double d4 = n9 - n15;
        double d5 = RiverCarverFeature.c(n, n9, n2);
        if (d2 > 0.0 && Math.abs(d4) <= Math.abs(d3) && Math.abs(d4) < d2) {
            n8 = d5 < 0.5 + 0.5 * d4 / d2 ? 2 : 1;
        } else if (d > 0.0 && Math.abs(d3) < d) {
            n8 = d5 < 0.5 + 0.5 * d3 / d ? 1 : 0;
        } else {
            n8 = n9 > n15 ? 2 : (n9 < n14 ? 0 : 1);
        }
        if (!bankStyle.zoneEnabled(n8)) {
            return;
        }
        BlockState blockState5 = bankStyle.zoneBlock(n8, RiverCarverFeature.c(n2, n9, n));
        int n17 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n + 1, n2) - 1;
        int n18 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n - 1, n2) - 1;
        int n19 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n, n2 + 1) - 1;
        int n20 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n, n2 - 1) - 1;
        int n21 = Math.max(Math.max(n9 - n17, n9 - n18), Math.max(n9 - n19, n9 - n20));
        double d6 = Double.MAX_VALUE;
        for (int n22 : new int[]{n17, n18, n19, n20}) {
            d6 = Math.min(d6, (double)Math.abs(n9 - n22));
        }
        Object object = null;
        int n23 = n7 = n9 >= n3 - 1 ? 1 : 0;
        if (BankStyle.isChameleon(blockState5)) {
            blockState5 = blockState;
            if (blockState2 != null && n8 == 2 && (n7 != 0 || d6 <= 1.0)) {
                object = blockState;
                blockState5 = blockState2;
            }
        }
        if (RiverCarverFeature.c(n, n2)) {
            RiverCarverFeature.b(n, n2, "SKIN pass=" + string + " surfaceY=" + n9 + " ws=" + n4 + " zone=" + n8 + " exposure=" + n21 + " slope=" + String.format(Locale.ROOT, "%.2f", d6) + " cur=" + String.valueOf(blockState4.getBlock()) + " skin=" + String.valueOf(blockState5 == null ? "none" : blockState5.getBlock()) + " face=" + (object == null ? "same" : ((BlockBehaviour.BlockStateBase)object).getBlock().toString()) + " turf=" + (object != null) + " grazed=" + (n7 != 0) + " scanTop=" + n3 + " nb=" + n17 + "/" + n18 + "/" + n19 + "/" + n20 + " biome=" + holder2.unwrapKey().map(resourceKey -> resourceKey.location().toString()).orElse("?") + " style[bed=" + String.valueOf(bankStyle.bed()) + " wl=" + String.valueOf(bankStyle.waterline()) + " bank=" + String.valueOf(bankStyle.bank()) + " bedOn=" + bankStyle.bedEnabled() + " wlOn=" + bankStyle.waterlineEnabled() + " bankOn=" + bankStyle.bankEnabled() + "]");
        }
        if (blockState5 != null && !blockState5.isAir() && !blockState5.equals(blockState4)) {
            Object object2;
            RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n, n9, n2, blockState5, n5);
            int n24 = Mth.clamp(n21, 2, 8);
            Object object3 = object2 = (Object)(object != null ? object : (Object)blockState5);
            if (RiverCarverFeature.a((BlockState)object2)) {
                object2 = Blocks.DIRT.defaultBlockState();
            }
            for (int i = 1; i <= n24; ++i) {
                mutableBlockPos.set(n, n9 - i, n2);
                BlockState blockState6 = worldGenLevel.getBlockState(mutableBlockPos);
                if (blockState6.isAir() || !blockState6.getFluidState().isEmpty()) break;
                if (blockState6.equals(object2)) continue;
                worldGenLevel.setBlock(mutableBlockPos, (BlockState)object2, 2);
            }
            mutableBlockPos.set(n, n9 + 1, n2);
            BlockState blockState7 = worldGenLevel.getBlockState(mutableBlockPos);
            if (!blockState7.isAir() && blockState7.getFluidState().isEmpty() && !blockState7.canSurvive(worldGenLevel, mutableBlockPos)) {
                worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    public static void skinAllWater(WorldGenLevel worldGenLevel, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        if (n3 <= 0 || !Services.PLATFORM.customBanks()) {
            return;
        }
        List<BankStyle> list = BankStyleLoader.configStyles();
        List<BankStyle> list2 = RiverCarverFeature.a(worldGenLevel);
        if (list.isEmpty() && list2.isEmpty()) {
            return;
        }
        int n7 = worldGenLevel.getMinBuildHeight();
        int n8 = worldGenLevel.getMaxBuildHeight() - 1;
        int n9 = 4;
        int n10 = 16 + 2 * n9;
        int[] nArray = new int[n10 * n10];
        Arrays.fill(nArray, Integer.MIN_VALUE);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        RiverEngine riverEngine = StreamsWorldgen.engine(worldGenLevel);
        for (int i = -n9; i < 16 + n9; ++i) {
            for (int j = -n9; j < 16 + n9; ++j) {
                n6 = n + i;
                n5 = n2 + j;
                n4 = Integer.MIN_VALUE;
                if (riverEngine != null) {
                    Lake lake = RiverCarverFeature.a(riverEngine, n6, n5);
                    if (lake != null) {
                        n4 = Mth.clamp(lake.level(), n7, n8);
                    } else {
                        ColumnPlan columnPlan = riverEngine.planLean(n6, n5);
                        if (columnPlan != ColumnPlan.NONE && columnPlan.inChannel()) {
                            n4 = Mth.clamp(columnPlan.waterSurfaceY(), n7, n8);
                        }
                    }
                }
                if (n4 != Integer.MIN_VALUE) {
                    nArray[(i + n9) * n10 + (j + n9)] = n4;
                    continue;
                }
                int n11 = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, n6, n5) - 1;
                mutableBlockPos.set(n6, n11, n5);
                if (!worldGenLevel.getBlockState(mutableBlockPos).getFluidState().is(FluidTags.WATER) || n3 < 2 && worldGenLevel.getUncachedNoiseBiome(n6 >> 2, n11 >> 2, n5 >> 2).is(BiomeTags.IS_OCEAN)) continue;
                nArray[(i + n9) * n10 + (j + n9)] = n11;
            }
        }
        HashMap<Holder<Biome>, BankStyle> hashMap = new HashMap<Holder<Biome>, BankStyle>();
        HashMap<Long, Holder<Biome>> hashMap2 = new HashMap<Long, Holder<Biome>>();
        for (n6 = 0; n6 < 16; ++n6) {
            for (n5 = 0; n5 < 16; ++n5) {
                int n12;
                int n13;
                int n14;
                n4 = Integer.MIN_VALUE;
                int n15 = Integer.MAX_VALUE;
                for (n14 = -n9; n14 <= n9; ++n14) {
                    for (n13 = -n9; n13 <= n9; ++n13) {
                        int n16;
                        n12 = nArray[(n6 + n14 + n9) * n10 + (n5 + n13 + n9)];
                        if (n12 == Integer.MIN_VALUE || (n16 = n14 * n14 + n13 * n13) > n9 * n9 || n16 >= n15) continue;
                        n15 = n16;
                        n4 = n12;
                    }
                }
                if (n4 == Integer.MIN_VALUE) continue;
                n14 = n + n6;
                n13 = n2 + n5;
                n12 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n14, n13) - 1;
                RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n14, n13, n12, RiverCarverFeature.a(n14, n13, worldGenLevel, n12, n7), RiverCarverFeature.a(n14, n13, worldGenLevel, n12), n4, false, list, list2, hashMap, hashMap2, n7, n8, "water");
            }
        }
    }

    private static BankStyle a(List<BankStyle> list, List<BankStyle> list2, Holder<Biome> holder) {
        ResourceKey resourceKey = holder.unwrapKey().orElse(null);
        BankStyle bankStyle = RiverCarverFeature.a(RiverCarverFeature.a(list, holder, resourceKey), resourceKey);
        if (bankStyle != null) {
            return bankStyle;
        }
        bankStyle = RiverCarverFeature.a(RiverCarverFeature.a(list2, holder, resourceKey), resourceKey);
        if (bankStyle != null) {
            return bankStyle;
        }
        int n = Math.max(RiverCarverFeature.a(list, holder), RiverCarverFeature.a(list2, holder));
        if (n > 0) {
            bankStyle = RiverCarverFeature.a(RiverCarverFeature.a(list, holder, n), resourceKey);
            if (bankStyle != null) {
                return bankStyle;
            }
            bankStyle = RiverCarverFeature.a(RiverCarverFeature.a(list2, holder, n), resourceKey);
            if (bankStyle != null) {
                return bankStyle;
            }
        }
        return (bankStyle = RiverCarverFeature.a(RiverCarverFeature.b(list, holder), resourceKey)) != null ? bankStyle : RiverCarverFeature.a(RiverCarverFeature.b(list2, holder), resourceKey);
    }

    private static List<BankStyle> a(List<BankStyle> list, Holder<Biome> holder, ResourceKey<Biome> resourceKey) {
        ArrayList<BankStyle> arrayList = new ArrayList<BankStyle>();
        for (BankStyle bankStyle : list) {
            if (!bankStyle.matchesExact(resourceKey) || bankStyle.excluded(holder)) continue;
            arrayList.add(bankStyle);
        }
        return arrayList;
    }

    private static int a(List<BankStyle> list, Holder<Biome> holder) {
        int n = 0;
        for (BankStyle bankStyle : list) {
            if (!bankStyle.matchesTags(holder) || bankStyle.excluded(holder)) continue;
            n = Math.max(n, bankStyle.tags().size());
        }
        return n;
    }

    private static List<BankStyle> a(List<BankStyle> list, Holder<Biome> holder, int n) {
        ArrayList<BankStyle> arrayList = new ArrayList<BankStyle>();
        for (BankStyle bankStyle : list) {
            if (!bankStyle.matchesTags(holder) || bankStyle.tags().size() != n || bankStyle.excluded(holder)) continue;
            arrayList.add(bankStyle);
        }
        return arrayList;
    }

    private static List<BankStyle> b(List<BankStyle> list, Holder<Biome> holder) {
        ArrayList<BankStyle> arrayList = new ArrayList<BankStyle>();
        for (BankStyle bankStyle : list) {
            if (!bankStyle.biomes().isEmpty() || !bankStyle.tags().isEmpty() || bankStyle.excluded(holder)) continue;
            arrayList.add(bankStyle);
        }
        return arrayList;
    }

    private static BankStyle a(List<BankStyle> list, ResourceKey<Biome> resourceKey) {
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1 || resourceKey == null) {
            return list.get(0);
        }
        return list.get(Math.floorMod(resourceKey.location().hashCode(), list.size()));
    }

    private static void b(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        int n5 = 0;
        int n6 = n3 - 1;
        for (int i = n3; i <= n4 && n5 <= 2; ++i) {
            mutableBlockPos.set(n, i, n2);
            BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
            if (blockState.is(BlockTags.LOGS)) {
                worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
                n5 = 0;
                n6 = i;
                continue;
            }
            if (blockState.is(BlockTags.LEAVES)) {
                n5 = 0;
                n6 = i;
                continue;
            }
            if (!blockState.isAir()) break;
            ++n5;
        }
        RiverCarverFeature.a(worldGenLevel, n, n2, n3 - 1, n6, n4);
    }

    private static void a(WorldGenLevel worldGenLevel, int n, int n2, int n3, int n4, int n5) {
        if (n4 < n3) {
            return;
        }
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        ArrayDeque<long[]> arrayDeque = new ArrayDeque<long[]>();
        HashSet<Long> hashSet = new HashSet<Long>();
        ArrayList<long[]> arrayList = new ArrayList<long[]>();
        for (int i = Math.max(n3, n4 - 10); i <= Math.min(n4 + 1, n5); ++i) {
            for (int[] object : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int n6 = n + object[0];
                int n7 = n2 + object[1];
                if (!worldGenLevel.getBlockState(mutableBlockPos.set(n6, i, n7)).is(BlockTags.LEAVES) || !hashSet.add((long)n6 << 40 ^ (long)i << 20 ^ (long)n7 & 0xFFFFFL)) continue;
                arrayDeque.add(new long[]{n6, i, n7});
            }
        }
        while (!arrayDeque.isEmpty()) {
            long[] lArray = (long[])arrayDeque.poll();
            int n8 = (int)lArray[0];
            int n9 = (int)lArray[1];
            int n10 = (int)lArray[2];
            arrayList.add(lArray);
            if (arrayList.size() > 2048) {
                return;
            }
            for (int[] nArray : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                int n11 = n8 + nArray[0];
                int n12 = n9 + nArray[1];
                int n13 = n10 + nArray[2];
                if (Math.abs(n11 - n) > 10 || Math.abs(n13 - n2) > 10 || n12 < n3 - 10 || n12 > n5) continue;
                BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos.set(n11, n12, n13));
                if (blockState.is(BlockTags.LOGS)) {
                    return;
                }
                if (!blockState.is(BlockTags.LEAVES) || !hashSet.add((long)n11 << 40 ^ (long)n12 << 20 ^ (long)n13 & 0xFFFFFL)) continue;
                arrayDeque.add(new long[]{n11, n12, n13});
            }
        }
        BlockState blockState = Blocks.AIR.defaultBlockState();
        for (long[] lArray : arrayList) {
            worldGenLevel.setBlock(mutableBlockPos.set((int)lArray[0], (int)lArray[1], (int)lArray[2]), blockState, 2);
        }
    }

    private static boolean c(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        boolean bl = false;
        int n5 = Math.min(n3 + 5, n4);
        for (int i = n3; i <= n5; ++i) {
            boolean bl2;
            mutableBlockPos.set(n, i, n2);
            BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
            if (blockState.isAir()) continue;
            if (!blockState.getFluidState().isEmpty()) break;
            mutableBlockPos.set(n, i - 1, n2);
            boolean bl3 = worldGenLevel.getBlockState(mutableBlockPos).isAir();
            mutableBlockPos.set(n, i, n2);
            if (!bl3) break;
            boolean bl4 = !blockState.canSurvive(worldGenLevel, mutableBlockPos);
            boolean bl5 = blockState.getBlock() instanceof SlabBlock;
            boolean bl6 = bl2 = blockState.is(Blocks.SNOW) || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.POWDER_SNOW);
            if (!bl4 && !bl5 && !bl2) break;
            worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
            bl = true;
        }
        return bl;
    }

    private static void a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2, int[] nArray, ColumnPlan[] columnPlanArray, int n3) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; ++i) {
            block1: for (int j = 0; j < 16; ++j) {
                int n4 = n + i;
                int n5 = n2 + j;
                ColumnPlan columnPlan = columnPlanArray[i * 16 + j];
                if (columnPlan == null) {
                    columnPlan = riverEngine.plan(n4, n5);
                }
                if (columnPlan == ColumnPlan.NONE || !columnPlan.inCorridor()) continue;
                int n6 = nArray[i * 16 + j];
                mutableBlockPos.set(n4, n6, n5);
                if (!worldGenLevel.getBlockState(mutableBlockPos).isAir()) continue;
                for (int k = n6 + 1; k <= n3; ++k) {
                    boolean bl;
                    mutableBlockPos.set(n4, k, n5);
                    BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
                    if (blockState.isAir() || !blockState.getFluidState().isEmpty()) continue block1;
                    boolean bl2 = bl = blockState.is(Blocks.SNOW) || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.POWDER_SNOW);
                    if (!bl && (!blockState.canOcclude() || blockState.is(Blocks.BEDROCK))) continue;
                    worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4, int n5, int n6) {
        boolean bl = false;
        for (int[] nArray : ah) {
            int n7 = n + nArray[0];
            int n8 = n2 + nArray[1];
            if (n7 < n3 || n7 > n3 + 15 || n8 < n4 || n8 > n4 + 15) continue;
            for (int i = n6; i >= n5; --i) {
                mutableBlockPos.set(n7, i, n8);
                BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
                if (blockState.isAir() || !blockState.getFluidState().isEmpty() || blockState.canSurvive(worldGenLevel, mutableBlockPos)) continue;
                worldGenLevel.setBlock(mutableBlockPos, Blocks.AIR.defaultBlockState(), 2);
                bl = true;
            }
        }
        return bl;
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos blockPos, BlockState blockState) {
        BlockState blockState2 = worldGenLevel.getBlockState(blockPos);
        if (blockState2.hasProperty(BlockStateProperties.WATERLOGGED)) {
            if (!blockState2.getValue(BlockStateProperties.WATERLOGGED).booleanValue()) {
                worldGenLevel.setBlock(blockPos, (BlockState)blockState2.setValue(BlockStateProperties.WATERLOGGED, true), 2);
            }
            return;
        }
        if (!blockState2.getFluidState().isEmpty()) {
            return;
        }
        if (blockState2.isCollisionShapeFullBlock(worldGenLevel, blockPos)) {
            return;
        }
        worldGenLevel.setBlock(blockPos, blockState, 2);
    }

    private static int a(ColumnPlan columnPlan, int n, int n2) {
        if (n2 <= n) {
            return n;
        }
        double d = columnPlan.distToOutlet();
        double d2 = Math.max(24.0, 3.0 * columnPlan.width());
        double d3 = Math.min(1.0, d / d2);
        double d4 = d3 * d3 * (3.0 - 2.0 * d3);
        return (int)Math.round((double)n2 + (double)(n - n2) * d4);
    }

    private static boolean a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, ColumnPlan columnPlan, RiverEngine riverEngine, int n3, int n4, int n5, BlockState blockState) {
        int n6 = n4;
        double d = columnPlan.width() > 1.0E-6 ? Math.min(1.0, columnPlan.distance() / columnPlan.width()) : 1.0;
        int n7 = Math.max(1, (int)Math.round(2.0 * columnPlan.width() * riverEngine.config().streamDepth()));
        int n8 = (int)Math.round((double)n7 * (1.0 - d * d));
        int n9 = n6 - n8;
        int n10 = RiverCarverFeature.a(columnPlan, n9, n3);
        if (n10 >= n3) {
            return false;
        }
        for (int i = n3; i > n10 && i > n5; --i) {
            mutableBlockPos.set(n, i, n2);
            worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
        }
        return true;
    }

    private static int a(RiverEngine riverEngine, WorldGenLevel worldGenLevel, ColumnPlan columnPlan, int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        if (n4 == Integer.MIN_VALUE || columnPlan.distToOutlet() > 24.0) {
            return Integer.MIN_VALUE;
        }
        return RiverCarverFeature.a(riverEngine, worldGenLevel, columnPlan, n, n2, n3, n4, n5, n6, n7, Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ()));
    }

    private static int a(RiverEngine riverEngine, WorldGenLevel worldGenLevel, ColumnPlan columnPlan, int n, int n2, int n3, int n4, int n5, int n6, int n7, double d) {
        if (n4 == Integer.MIN_VALUE || columnPlan.distToOutlet() > 24.0) {
            return Integer.MIN_VALUE;
        }
        int n8 = RiverCarverFeature.a(riverEngine, worldGenLevel, n, n2, columnPlan, n5, n6, n7, d);
        int n9 = Math.min(Math.min(n4, n8), n3);
        return (n9 = Math.min(n9, columnPlan.waterSurfaceY() + 4)) > columnPlan.waterSurfaceY() ? n9 : Integer.MIN_VALUE;
    }

    public static int nearbyLakeLevel(RiverEngine riverEngine, int n, int n2, int n3, int n4) {
        int n5 = Math.max(1, riverEngine.config().gridSpacing());
        int n6 = Integer.MIN_VALUE;
        for (int i = -2 * n5; i <= 2 * n5; i += n5) {
            for (int j = -2 * n5; j <= 2 * n5; j += n5) {
                Lake lake = riverEngine.lakeAt(n + j, n2 + i);
                if (lake == null || lake.digDepth() > 0) continue;
                n6 = Math.max(n6, Mth.clamp(lake.level(), n3, n4));
            }
        }
        return n6;
    }

    private static int a(RiverEngine riverEngine, WorldGenLevel worldGenLevel, int n, int n2, ColumnPlan columnPlan, int n3, int n4, int n5, double d) {
        if (d < 1.0E-6) {
            return Integer.MAX_VALUE;
        }
        double d2 = -columnPlan.flowDirZ() / d;
        double d3 = columnPlan.flowDirX() / d;
        double d4 = columnPlan.width();
        int n6 = Integer.MAX_VALUE;
        for (double d5 = d4 + 1.0; d5 <= d4 + 4.0; d5 += 3.0) {
            for (int i = -1; i <= 1; i += 2) {
                int n7;
                int n8 = n + (int)Math.round(d2 * d5 * (double)i);
                Lake lake = riverEngine.lakeAt(n8, n7 = n2 + (int)Math.round(d3 * d5 * (double)i));
                int n9 = lake != null && lake.digDepth() <= 0 ? Mth.clamp(lake.level(), n4, n5) : worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n8, n7) - 1;
                n6 = Math.min(n6, n9);
            }
        }
        return Math.max(n6, n3 - 1);
    }

    private static boolean a(RiverEngine riverEngine, int n, int n2, int n3) {
        for (int[] nArray : V) {
            ColumnPlan columnPlan = riverEngine.plan(n + nArray[0], n2 + nArray[1]);
            if (columnPlan.inCorridor() && columnPlan.inChannel() && columnPlan.waterSurfaceY() >= n3) continue;
            return true;
        }
        return false;
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4, int n5) {
        int n6 = Math.max(n3, n5 + 1);
        for (int[] nArray : V) {
            int n7 = n + nArray[0];
            int n8 = n2 + nArray[1];
            for (int i = n6; i <= n4; ++i) {
                mutableBlockPos.set(n7, i, n8);
                if (!worldGenLevel.getBlockState(mutableBlockPos).getFluidState().is(FluidTags.WATER)) continue;
                worldGenLevel.scheduleTick(new BlockPos(n7, i, n8), Fluids.WATER, 5 + Math.floorMod(Mth.getSeed(n7, i, n8), 20));
            }
        }
    }

    private static int a(ColumnPlan columnPlan) {
        double d;
        double d2 = columnPlan.fluidFlowDirX();
        if (Math.sqrt(d2 * d2 + (d = columnPlan.fluidFlowDirZ()) * d) < 0.5) {
            return 8;
        }
        int n = (int)Math.round(Math.atan2(columnPlan.fluidFlowDirZ(), columnPlan.fluidFlowDirX()) / 0.7853981633974483);
        return (n % 8 + 8) % 8;
    }

    private static boolean a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3) {
        return worldGenLevel.getBlockState(mutableBlockPos.set(n + 1, n2, n3)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n - 1, n2, n3)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n, n2, n3 + 1)).isAir() && worldGenLevel.getBlockState(mutableBlockPos.set(n, n2, n3 - 1)).isAir();
    }

    private static boolean d(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        BlockState blockState;
        if (n2 - 1 >= n4 && !(blockState = worldGenLevel.getBlockState(mutableBlockPos.set(n, n2 - 1, n3))).isAir() && blockState.getFluidState().isEmpty()) {
            return true;
        }
        return RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n + 1, n2, n3) || RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n - 1, n2, n3) || RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n, n2, n3 + 1) || RiverCarverFeature.b(worldGenLevel, mutableBlockPos, n, n2, n3 - 1);
    }

    private static boolean b(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3) {
        BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos.set(n, n2, n3));
        return !blockState.isAir() && blockState.getFluidState().isEmpty();
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, BlockState blockState, int n4) {
        if (blockState.getBlock() instanceof FallingBlock && n2 - 1 >= n4) {
            mutableBlockPos.set(n, n2 - 1, n3);
            BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos);
            if (blockState2.isAir() || !blockState2.getFluidState().isEmpty()) {
                worldGenLevel.setBlock(mutableBlockPos, Blocks.STONE.defaultBlockState(), 2);
            }
        }
        mutableBlockPos.set(n, n2, n3);
        worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
    }

    private static void b(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4, int n5, int n6) {
        BlockState blockState = RiverCarverFeature.a(worldGenLevel, n, n2, n5, n6);
        int n7 = Math.max(n6, Math.min(n3, n4));
        if (RiverCarverFeature.c(n, n2)) {
            mutableBlockPos.set(n, Math.max(n6, n7 - 1), n2);
            BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos);
            if (blockState2.isAir() || !blockState2.getFluidState().isEmpty()) {
                RiverCarverFeature.b(n, n2, "CUP void below floor: hanging funnel from " + n7 + " to bed " + n4);
            }
        }
        for (int i = n7; i <= n4; ++i) {
            mutableBlockPos.set(n, i, n2);
            BlockState blockState3 = worldGenLevel.getBlockState(mutableBlockPos);
            if (!blockState3.isAir()) continue;
            RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n, i, n2, blockState, n6);
        }
    }

    private static BlockState a(WorldGenLevel worldGenLevel, int n, int n2, int n3, int n4, BlockState blockState) {
        for (int[] nArray : o) {
            BlockState blockState2;
            int n5 = n + nArray[0];
            int n6 = n2 + nArray[1];
            int n7 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n5, n6) - 1;
            if (n7 < n3 || n7 < n4 || (blockState2 = worldGenLevel.getBlockState(new BlockPos(n5, n7, n6))).isAir() || !blockState2.getFluidState().isEmpty() || !blockState2.canOcclude()) continue;
            return blockState2;
        }
        return blockState;
    }

    private static BlockState a(WorldGenLevel worldGenLevel, int n, int n2, int n3) {
        BlockState blockState = worldGenLevel.getBlockState(new BlockPos(n, n3, n2));
        return blockState.isAir() || !blockState.getFluidState().isEmpty() || !blockState.canOcclude() ? null : blockState;
    }

    private static BlockState a(WorldGenLevel worldGenLevel, int n, int n2, int n3, int n4) {
        BlockState blockState = worldGenLevel.getBlockState(new BlockPos(n, n3, n2));
        BlockState blockState2 = n3 - 1 >= n4 ? worldGenLevel.getBlockState(new BlockPos(n, n3 - 1, n2)) : blockState;
        return blockState2.isAir() || !blockState2.getFluidState().isEmpty() ? blockState : blockState2;
    }

    private static int a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, ColumnPlan columnPlan, int n3, int n4, int n5, int[] nArray) {
        int n6 = RiverCarverFeature.a(n4, n5, nArray);
        if (n6 != Integer.MIN_VALUE) {
            return n6;
        }
        boolean bl = false;
        for (int i = 0; i < 256 && !bl; ++i) {
            bl = nArray[i] != Integer.MAX_VALUE;
        }
        if (bl && columnPlan.distance() <= 8.0) {
            return Integer.MIN_VALUE;
        }
        return RiverCarverFeature.a(worldGenLevel, riverEngine, mutableBlockPos, n, n2, columnPlan, n3);
    }

    private static int a(int n, int n2, int[] nArray) {
        int n3 = Integer.MAX_VALUE;
        int n4 = Integer.MAX_VALUE;
        for (int i = -8; i <= 8; ++i) {
            for (int j = -8; j <= 8; ++j) {
                int n5;
                int n6;
                int n7 = n + i;
                int n8 = n2 + j;
                if (n7 < 0 || n7 > 15 || n8 < 0 || n8 > 15 || (n6 = nArray[n7 * 16 + n8]) == Integer.MAX_VALUE || (n5 = i * i + j * j) >= n4) continue;
                n4 = n5;
                n3 = n6;
            }
        }
        return n3 == Integer.MAX_VALUE ? Integer.MIN_VALUE : n3;
    }

    private static int a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, ColumnPlan columnPlan, int n3) {
        ColumnPlan columnPlan2;
        int n4;
        int n5;
        int n6;
        int n7;
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        if (d < 1.0E-6) {
            return Integer.MIN_VALUE;
        }
        if (columnPlan.waterSurfaceY() <= worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n, n2) - 1) {
            return Integer.MIN_VALUE;
        }
        double d2 = columnPlan.flowDirX() / d;
        double d3 = columnPlan.flowDirZ() / d;
        double d4 = -columnPlan.flowDirZ() / d;
        double d5 = columnPlan.flowDirX() / d;
        int n8 = (int)Math.round(columnPlan.distance());
        int n9 = n;
        int n10 = n2;
        if (n8 > 0) {
            int n11;
            int n12 = n + (int)Math.round(d4 * (double)n8);
            n7 = n2 + (int)Math.round(d5 * (double)n8);
            n6 = n - (int)Math.round(d4 * (double)n8);
            n5 = n2 - (int)Math.round(d5 * (double)n8);
            ColumnPlan columnPlan3 = riverEngine.plan(n12, n7);
            ColumnPlan columnPlan4 = riverEngine.plan(n6, n5);
            n4 = columnPlan3 != ColumnPlan.NONE && columnPlan3.inChannel() ? 1 : 0;
            int n13 = n11 = columnPlan4 != ColumnPlan.NONE && columnPlan4.inChannel() ? 1 : 0;
            if (n4 != n11) {
                n9 = n4 != 0 ? n12 : n6;
                n10 = n4 != 0 ? n7 : n5;
            } else {
                double d6 = columnPlan3 == ColumnPlan.NONE ? Double.MAX_VALUE : columnPlan3.distance();
                double d7 = columnPlan4 == ColumnPlan.NONE ? Double.MAX_VALUE : columnPlan4.distance();
                n9 = d6 <= d7 ? n12 : n6;
                int n14 = n10 = d6 <= d7 ? n7 : n5;
            }
        }
        if ((columnPlan2 = riverEngine.plan(n9, n10)) != ColumnPlan.NONE && columnPlan2.inChannel() && (n6 = RiverCarverFeature.e(worldGenLevel, mutableBlockPos, n9, n10, n7 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n9, n10) - 1, n3)) != Integer.MIN_VALUE && columnPlan2.waterSurfaceY() <= n6 + 1) {
            return n6;
        }
        n7 = 0;
        n6 = 0;
        n5 = Integer.MAX_VALUE;
        for (int i = -3; i <= 3; i += 3) {
            ColumnPlan columnPlan5;
            int n15 = n9 + (int)Math.round(d2 * (double)i);
            n4 = n10 + (int)Math.round(d3 * (double)i);
            ColumnPlan columnPlan6 = columnPlan5 = i == 0 ? riverEngine.plan(n9, n10) : riverEngine.plan(n15, n4);
            if (columnPlan5 == ColumnPlan.NONE || !columnPlan5.inChannel()) continue;
            int n16 = Math.max(1, (int)Math.round(columnPlan5.width()));
            int n17 = Math.max(1, n16 / 2);
            int n18 = 0;
            int n19 = 0;
            int n20 = Integer.MAX_VALUE;
            for (int j = -n16; j <= n16; j += n17) {
                int n21;
                int n22 = n15 + (int)Math.round(d4 * (double)j);
                ColumnPlan columnPlan7 = riverEngine.plan(n22, n21 = n4 + (int)Math.round(d5 * (double)j));
                if (columnPlan7 == ColumnPlan.NONE || !columnPlan7.inCorridor()) continue;
                ++n18;
                int n23 = RiverCarverFeature.b(worldGenLevel, riverEngine, mutableBlockPos, n22, n21, columnPlan7, n3);
                if (n23 == Integer.MIN_VALUE) continue;
                ++n19;
                n20 = Math.min(n20, n23);
            }
            if (n18 < 3) continue;
            ++n7;
            if (n19 != n18) continue;
            ++n6;
            n5 = Math.min(n5, n20);
        }
        return n7 > 0 && n6 * 2 > n7 ? n5 : Integer.MIN_VALUE;
    }

    private static int b(WorldGenLevel worldGenLevel, RiverEngine riverEngine, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, ColumnPlan columnPlan, int n3) {
        int n4;
        int n5;
        int n6;
        long l = Hashing.mix((long)n << 32 ^ (long)n2 & 0xFFFFFFFFL);
        HashMap<Long, Integer> hashMap = A.get();
        Integer n7 = hashMap.get(l);
        if (n7 != null) {
            return n7;
        }
        int n8 = columnPlan.waterSurfaceY();
        int n9 = RiverCarverFeature.a(worldGenLevel, riverEngine, mutableBlockPos, n, n2, columnPlan, n8, n3);
        int n10 = n6 = n9 < n8 ? n9 : Integer.MIN_VALUE;
        if (n6 == Integer.MIN_VALUE && (n5 = RiverCarverFeature.e(worldGenLevel, mutableBlockPos, n, n2, n4 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n, n2) - 1, n3)) != Integer.MIN_VALUE && n8 <= n5 + 1) {
            n6 = n5;
        }
        hashMap.put(l, n6);
        return n6;
    }

    private static int a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, ColumnPlan columnPlan, int n3, int n4) {
        int n5;
        int n6;
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        if (d < 1.0E-6) {
            return n3;
        }
        double d2 = columnPlan.flowDirX() / d;
        double d3 = columnPlan.flowDirZ() / d;
        int n7 = n3;
        int n8 = RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n, n2, columnPlan, n4, d);
        if (n8 != Integer.MIN_VALUE && n8 < n7) {
            n7 = n8;
        }
        for (int i = 3; i <= 48 && RiverCarverFeature.a(n6 = n - (int)Math.round(d2 * (double)i), n5 = n2 - (int)Math.round(d3 * (double)i)); i += 3) {
            int n9;
            ColumnPlan columnPlan2 = riverEngine.plan(n6, n5);
            if (columnPlan2 == ColumnPlan.NONE || !columnPlan2.inChannel() || (n9 = RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n6, n5, columnPlan2, n4, Math.hypot(columnPlan2.flowDirX(), columnPlan2.flowDirZ()))) == Integer.MIN_VALUE || n9 >= n7) continue;
            n7 = n9;
        }
        return n7;
    }

    private static int a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, ColumnPlan columnPlan, int n3, double d) {
        if (d < 1.0E-6) {
            return Integer.MIN_VALUE;
        }
        double d2 = -columnPlan.flowDirZ() / d;
        double d3 = columnPlan.flowDirX() / d;
        int n4 = Math.max(1, (int)Math.round(columnPlan.width()));
        int n5 = (int)Math.round(Math.min(columnPlan.distance(), columnPlan.width() + 8.0));
        for (int i = -1; i <= 1; i += 2) {
            int n6 = n + (int)Math.round(d2 * (double)n5 * (double)i);
            int n7 = n2 + (int)Math.round(d3 * (double)n5 * (double)i);
            int n8 = Integer.MIN_VALUE;
            int n9 = Integer.MAX_VALUE;
            boolean bl = true;
            boolean bl2 = false;
            for (int j = -n4; j <= n4; j += n4 == 0 ? 1 : n4) {
                int n10;
                int n11 = n6 + (int)Math.round(d2 * (double)j);
                if (!RiverCarverFeature.a(n11, n10 = n7 + (int)Math.round(d3 * (double)j))) {
                    bl2 = true;
                    break;
                }
                int n12 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n11, n10) - 1;
                int n13 = RiverCarverFeature.e(worldGenLevel, mutableBlockPos, n11, n10, n12, n3);
                if (n13 == Integer.MIN_VALUE) {
                    n9 = Math.min(n9, n12);
                    continue;
                }
                n8 = Math.max(n8, n13);
            }
            if (bl2) {
                if (n5 != 0) continue;
                break;
            }
            if (n8 == Integer.MIN_VALUE) {
                bl = false;
            } else if (n9 != Integer.MAX_VALUE && n9 > n8) {
                bl = false;
            }
            if (bl) {
                return n8;
            }
            if (n5 == 0) break;
        }
        return Integer.MIN_VALUE;
    }

    private static int e(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        int n5 = Math.min(n4, n3 + 1);
        if (worldGenLevel.getBlockState(mutableBlockPos.set(n, n5, n2)).getFluidState().isEmpty()) {
            return Integer.MIN_VALUE;
        }
        int n6 = n5;
        while (n6 + 1 <= n4 && n6 - n5 < 24 && !worldGenLevel.getBlockState(mutableBlockPos.set(n, n6 + 1, n2)).getFluidState().isEmpty()) {
            ++n6;
        }
        return n6;
    }

    private static boolean a(WorldGenLevel worldGenLevel, RiverEngine riverEngine, int n, int n2, ColumnPlan columnPlan, int n3, double d) {
        if (d < 1.0E-6) {
            return false;
        }
        double d2 = columnPlan.flowDirX() / d;
        double d3 = columnPlan.flowDirZ() / d;
        for (int i = 3; i <= 12; i += 3) {
            int n4;
            int n5;
            int n6 = n + (int)Math.round(d2 * (double)i);
            ColumnPlan columnPlan2 = riverEngine.plan(n6, n5 = n2 + (int)Math.round(d3 * (double)i));
            if (columnPlan2 == ColumnPlan.NONE || !columnPlan2.inChannel() || columnPlan2.waterSurfaceY() <= n3 + 1 || (n4 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n6, n5) - 1) < columnPlan2.waterSurfaceY() - 3) continue;
            return true;
        }
        return false;
    }

    private static BlockState a(WorldGenLevel worldGenLevel, int n, int n2, int n3, int n4, BlockState blockState, int n5) {
        int n6 = n3 - n4;
        if (n6 < n5) {
            return blockState;
        }
        BlockState blockState2 = worldGenLevel.getBlockState(new BlockPos(n, n6, n2));
        return blockState2.isAir() || !blockState2.getFluidState().isEmpty() ? blockState : blockState2;
    }

    private static boolean a(BlockState blockState) {
        return ai.contains(blockState.getBlock()) || blockState.hasProperty(BlockStateProperties.SNOWY);
    }

    private static void b(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, BlockState blockState, int n4) {
        if (n2 < n4 || blockState.isAir()) {
            return;
        }
        BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos.set(n, n2, n3));
        if (RiverCarverFeature.a(blockState2) && !blockState2.equals(blockState)) {
            worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
        }
    }

    private static int a(int n, int n2, int n3, ColumnPlan columnPlan, double d) {
        return RiverCarverFeature.a(n, n2, n3, columnPlan, d, 8);
    }

    private static int a(int n, int n2, int n3, ColumnPlan columnPlan, double d, int n4) {
        double d2 = Math.max(5.0, columnPlan.width() * d);
        int n5 = Mth.clamp((int)Math.round(5.0 * columnPlan.width()), Math.min(5, n4), n4);
        double d3 = Math.max(0.0, columnPlan.distance() - columnPlan.width());
        double d4 = d2 > 1.0E-6 ? Math.min(1.0, d3 / d2) : 1.0;
        int n6 = (int)Math.round((double)n5 - (double)(n5 - 1) * d4);
        int n7 = (int)Math.round((RiverCarverFeature.b(n, n2, 5) - 0.5) * 2.0 * 2.0);
        return n3 - n6 + n7;
    }

    private static int a(WorldGenLevel worldGenLevel, int n, int n2, ColumnPlan columnPlan, int n3, int n4, int n5) {
        int n6 = columnPlan.waterSurfaceY();
        if (n3 >= n6) {
            return n6;
        }
        if (columnPlan.source()) {
            return Math.min(n6, RiverCarverFeature.a(worldGenLevel, n, n2, columnPlan, n4, n5));
        }
        return n6;
    }

    private static boolean a(WorldGenLevel worldGenLevel, int n, int n2, ColumnPlan columnPlan, int n3) {
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        if (d < 1.0E-6) {
            return false;
        }
        double d2 = -columnPlan.flowDirZ() / d;
        double d3 = columnPlan.flowDirX() / d;
        for (int i = -1; i <= 1; i += 2) {
            boolean bl = false;
            for (int j = 1; j <= 16; ++j) {
                int n4;
                int n5 = n + (int)Math.round(d2 * (double)j * (double)i);
                if (worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n5, n4 = n2 + (int)Math.round(d3 * (double)j * (double)i)) - 1 < n3) continue;
                bl = true;
                break;
            }
            if (bl) continue;
            return false;
        }
        return true;
    }

    private static boolean b(WorldGenLevel worldGenLevel, int n, int n2, int n3, int n4, int n5) {
        int n6;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (n6 = n3; n6 >= Math.max(n5, n3 - 3); --n6) {
            mutableBlockPos.set(n, n6, n2);
            BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
            if (!blockState.isAir() && blockState.getFluidState().isEmpty()) continue;
            return false;
        }
        for (n6 = 0; n6 < 4; ++n6) {
            int n7 = n + (n6 == 0 ? 1 : (n6 == 1 ? -1 : 0));
            int n8 = n2 + (n6 == 2 ? 1 : (n6 == 3 ? -1 : 0));
            int n9 = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n7, n8) - 1;
            for (int i = n3 + 1; i <= Math.min(n4, n9); ++i) {
                mutableBlockPos.set(n7, i, n8);
                BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
                if (!blockState.isAir() && blockState.getFluidState().isEmpty()) continue;
                return false;
            }
        }
        return true;
    }

    private static int a(WorldGenLevel worldGenLevel, int n, int n2, ColumnPlan columnPlan, int n3, int n4) {
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        if (d < 1.0E-6) {
            return n4;
        }
        double d2 = -columnPlan.flowDirZ() / d;
        double d3 = columnPlan.flowDirX() / d;
        double d4 = columnPlan.width() + 2.0;
        int n5 = Integer.MIN_VALUE;
        for (int i = -1; i <= 1; i += 2) {
            int n6 = n + (int)Math.round(d2 * d4 * (double)i);
            int n7 = n2 + (int)Math.round(d3 * d4 * (double)i);
            n5 = Math.max(n5, worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n6, n7) - 1);
        }
        return Mth.clamp(n5, n3, n4);
    }

    private static int b(WorldGenLevel worldGenLevel, int n, int n2, ColumnPlan columnPlan, int n3) {
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        if (d < 1.0E-6) {
            return Integer.MAX_VALUE;
        }
        double d2 = -columnPlan.flowDirZ() / d;
        double d3 = columnPlan.flowDirX() / d;
        double d4 = columnPlan.width();
        int n4 = Integer.MAX_VALUE;
        for (double d5 = d4 + 1.0; d5 <= d4 + 4.0; d5 += 3.0) {
            for (int i = -1; i <= 1; i += 2) {
                int n5 = n + (int)Math.round(d2 * d5 * (double)i);
                int n6 = n2 + (int)Math.round(d3 * d5 * (double)i);
                n4 = Math.min(n4, worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n5, n6) - 1);
            }
        }
        return Math.max(n4, n3 - 1);
    }

    private static boolean a(RiverEngine riverEngine, int n, int n2, ColumnPlan columnPlan) {
        double d = Math.hypot(columnPlan.flowDirX(), columnPlan.flowDirZ());
        if (d < 1.0E-6) {
            return false;
        }
        double d2 = -columnPlan.flowDirZ() / d;
        double d3 = columnPlan.flowDirX() / d;
        double d4 = (double)n + 0.5;
        double d5 = (double)n2 + 0.5;
        boolean bl = false;
        boolean bl2 = false;
        for (int i = 2; !(i > 8 || bl && bl2); i += 2) {
            if (!bl && RiverCarverFeature.a(riverEngine, d4 + d2 * (double)i, d5 + d3 * (double)i)) {
                bl = true;
            }
            if (bl2 || !RiverCarverFeature.a(riverEngine, d4 - d2 * (double)i, d5 - d3 * (double)i)) continue;
            bl2 = true;
        }
        return bl && bl2;
    }

    private static boolean a(RiverEngine riverEngine, double d, double d2) {
        return riverEngine.plan((int)Math.round(d), (int)Math.round(d2)).distance() <= 2.0;
    }

    private static double a(double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = d + d2 * d3;
        double d8 = 0.45 * d4;
        if (d2 <= d8 || d5 <= d8) {
            return d7;
        }
        double d9 = d + d8 * d3;
        if (d6 <= d9) {
            return d7;
        }
        double d10 = Math.min(1.0, (d6 - d) / 8.0);
        double d11 = 16.0 * d10;
        if (d11 <= 1.0E-6) {
            return d7;
        }
        double d12 = Math.min(1.0, (d2 - d8) / (d5 - d8));
        double d13 = d9 + (d6 - d9) * Math.log1p(d11 * d12) / Math.log1p(d11);
        double d14 = Math.min(1.0, d12 / 0.35);
        d14 = d14 * d14 * (3.0 - 2.0 * d14);
        return Math.max(d7, d7 + (d13 - d7) * d14);
    }

    private static int[] a() {
        String string = System.getProperty("streamsreflowing.carvetrace");
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            String[] stringArray = string.split(",");
            return new int[]{Integer.parseInt(stringArray[0].trim()), Integer.parseInt(stringArray[1].trim()), stringArray.length > 2 ? Integer.parseInt(stringArray[2].trim()) : 24};
        }
        catch (Exception exception) {
            return null;
        }
    }

    private static void a(ServerLevel serverLevel, BlockPos blockPos, int n) {
        RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
        if (riverEngine == null) {
            return;
        }
        int n2 = (blockPos.getX() >> 4) - n << 4;
        int n3 = (blockPos.getZ() >> 4) - n << 4;
        int n4 = (n * 2 + 1) * 16;
        TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>();
        StringBuilder stringBuilder = new StringBuilder();
        int n5 = 0;
        int n6 = 0;
        Constants.LOG.info("[SR-WATERMAP] ==== attribution around ({},{}) r={} chunks, plate {} ====", blockPos.getX(), blockPos.getZ(), n, riverEngine.plateAt(blockPos.getX(), blockPos.getZ()));
        for (int i = 0; i < n4; ++i) {
            StringBuilder stringBuilder2 = new StringBuilder();
            for (int j = 0; j < n4; ++j) {
                int n7;
                boolean bl;
                int n8;
                int n9;
                int n10;
                int n11;
                if (j > 0 && (j & 0xF) == 0) {
                    stringBuilder2.append('|');
                }
                boolean bl2 = (n11 = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, n10 = n2 + j, n9 = n3 + i) - 1) > (n8 = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR, n10, n9) - 1);
                ColumnPlan columnPlan = riverEngine.planLean(n10, n9);
                boolean bl3 = columnPlan != ColumnPlan.NONE && columnPlan.inChannel();
                boolean bl4 = columnPlan != ColumnPlan.NONE && !bl3 && bl2 && Math.abs(n11 - columnPlan.waterSurfaceY()) <= 2;
                Lake lake = riverEngine.lakeAt(n10, n9);
                Lake lake2 = riverEngine.lakeNear(n10, n9, 2);
                boolean bl5 = bl = lake2 != null && lake2.digDepth() == 0 && n8 + 1 < lake2.level();
                if (bl2) {
                    if (bl3) {
                        n7 = 83;
                    } else if (lake != null) {
                        n7 = 76;
                    } else if (bl) {
                        n7 = 108;
                    } else if (bl4) {
                        n7 = 99;
                    } else {
                        n7 = 79;
                        ++n5;
                        treeMap.merge(n11, 1, Integer::sum);
                        if (stringBuilder.length() < 600) {
                            stringBuilder.append(String.format(" (%d,%d,%d)", n10, n11, n9));
                        }
                    }
                } else if (bl3 || bl) {
                    n7 = 109;
                    ++n6;
                } else {
                    n7 = 46;
                }
                stringBuilder2.append((char)n7);
            }
            Constants.LOG.info("[SR-WATERMAP] {}", (Object)stringBuilder2);
            if ((i & 0xF) != 15 || i >= n4 - 1) continue;
            Constants.LOG.info("[SR-WATERMAP] {}", (Object)"-".repeat(n4 + n * 2));
        }
        Constants.LOG.info("[SR-WATERMAP] orphans={} (levels {}), expected-but-dry={}; orphan samples:{}", n5, treeMap, n6, stringBuilder);
    }

    public static void armTraceOverride(int n, int n2, int n3) {
        aA = new int[]{n, n2, n3};
    }

    public static void waterMap(ServerLevel serverLevel, BlockPos blockPos, int n) {
        RiverCarverFeature.a(serverLevel, blockPos, n);
    }

    private static boolean b(int n, int n2) {
        int[] nArray = aA != null ? aA : az;
        return nArray != null && n == nArray[0] && n2 == nArray[1];
    }

    private static void a(int n, int n2, String string) {
        Constants.LOG.info("[SR-STACK] ({},{}) {}", n, n2, string);
    }

    private static int a(int n, int n2, String string, int n3, int n4) {
        if (RiverCarverFeature.b(n, n2) && n3 != n4) {
            RiverCarverFeature.a(n, n2, "  " + string + ": " + n3 + " -> " + n4);
        }
        return n4;
    }

    private static boolean c(int n, int n2) {
        int[] nArray = aA != null ? aA : az;
        return nArray != null && Math.abs(n - nArray[0]) <= nArray[2] && Math.abs(n2 - nArray[1]) <= nArray[2];
    }

    private static void b(int n, int n2, String string) {
        Constants.LOG.info("[SR-CARVETRACE] ({},{}) {}", n, n2, string);
    }

    public static int adjacentCoveringLakeLevel(RiverEngine riverEngine, int n, int n2) {
        int n3 = Integer.MIN_VALUE;
        for (int i = 0; i < 8; ++i) {
            for (int j = 2; j <= 4; j += 2) {
                Lake lake = riverEngine.lakeAt(n + aB[i] * j, n2 + aC[i] * j);
                if (lake == null || lake.level() <= n3) continue;
                n3 = lake.level();
            }
        }
        return n3;
    }

    private static double d(int n, int n2) {
        long l = (long)n * -7046029254386353131L ^ (long)n2 * -4417276706812531889L;
        l ^= l >>> 30;
        l *= -4658895280553007687L;
        l ^= l >>> 27;
        l *= -7723592293110705685L;
        l ^= l >>> 31;
        return (double)(l >>> 11) * (double)1.110223E-16f;
    }

    private static double b(int n, int n2, int n3) {
        int n4 = Math.floorDiv(n, n3);
        int n5 = Math.floorDiv(n2, n3);
        double d = (double)(n - n4 * n3) / (double)n3;
        double d2 = (double)(n2 - n5 * n3) / (double)n3;
        d = d * d * (3.0 - 2.0 * d);
        d2 = d2 * d2 * (3.0 - 2.0 * d2);
        double d3 = RiverCarverFeature.d(n4, n5);
        double d4 = RiverCarverFeature.d(n4 + 1, n5);
        double d5 = RiverCarverFeature.d(n4, n5 + 1);
        double d6 = RiverCarverFeature.d(n4 + 1, n5 + 1);
        double d7 = d3 + (d4 - d3) * d;
        double d8 = d5 + (d6 - d5) * d;
        return d7 + (d8 - d7) * d2;
    }

    private static double c(int n, int n2, int n3) {
        long l = (long)n * -7046029254386353131L ^ (long)n2 * -4417276706812531889L ^ (long)n3 * 1609587929392839161L;
        l ^= l >>> 30;
        l *= -4658895280553007687L;
        l ^= l >>> 27;
        l *= -7723592293110705685L;
        l ^= l >>> 31;
        return (double)(l >>> 11) * (double)1.110223E-16f;
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, BlockState blockState) {
        for (int i = 0; i < 4; ++i) {
            int n4;
            int n5 = n + (i == 0 ? 1 : (i == 1 ? -1 : 0));
            BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos.set(n5, n2, n4 = n3 + (i == 2 ? 1 : (i == 3 ? -1 : 0))));
            if (!blockState2.is(Blocks.SNOW) || blockState2.isCollisionShapeFullBlock(worldGenLevel, mutableBlockPos)) continue;
            worldGenLevel.setBlock(mutableBlockPos, blockState, 2);
        }
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int[] nArray, int n, int n2, int n3, BlockState blockState) {
        int n4;
        int n5;
        boolean bl = false;
        int[] objectArray = nArray;
        int n6 = objectArray.length;
        for (n5 = 0; n5 < n6; ++n5) {
            n4 = objectArray[n5];
            if (n4 == Integer.MIN_VALUE) continue;
            bl = true;
            break;
        }
        if (!bl) {
            return;
        }
        boolean[] blArray = new boolean[256];
        for (n6 = 0; n6 < 256; ++n6) {
            if (nArray[n6] == Integer.MIN_VALUE) continue;
            n5 = nArray[n6];
            for (n4 = 0; n4 < 4; ++n4) {
                int n7 = RiverCarverFeature.e(n6, n4);
                if (n7 < 0 || blArray[n7] || nArray[n7] != Integer.MIN_VALUE) continue;
                RiverCarverFeature.a(worldGenLevel, mutableBlockPos, n7, n5, blArray, n, n2, n3, blockState);
            }
        }
    }

    private static int e(int n, int n2) {
        int n3 = n >> 4;
        int n4 = n & 0xF;
        int n5 = n3 + (n2 == 0 ? 1 : (n2 == 1 ? -1 : 0));
        int n6 = n4 + (n2 == 2 ? 1 : (n2 == 3 ? -1 : 0));
        return n5 < 0 || n5 > 15 || n6 < 0 || n6 > 15 ? -1 : n5 << 4 | n6;
    }

    private static void a(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, boolean[] blArray, int n3, int n4, int n5, BlockState blockState) {
        int n6;
        int n7;
        int n8;
        int n9;
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<Integer>();
        boolean[] blArray2 = new boolean[256];
        arrayDeque.add(n);
        blArray2[n] = true;
        boolean bl = true;
        block0: while (!arrayDeque.isEmpty() && bl) {
            int n10 = (Integer)arrayDeque.poll();
            n9 = n3 + (n10 >> 4);
            if (!worldGenLevel.getBlockState(mutableBlockPos.set(n9, n2, n8 = n4 + (n10 & 0xF))).isAir()) continue;
            arrayList.add(n10);
            if (arrayList.size() > 24 || !RiverCarverFeature.f(worldGenLevel, mutableBlockPos, n9, n2, n8, n5)) {
                bl = false;
                break;
            }
            for (n7 = 0; n7 < 4; ++n7) {
                n6 = RiverCarverFeature.e(n10, n7);
                if (n6 < 0) {
                    bl = false;
                    continue block0;
                }
                if (blArray2[n6]) continue;
                blArray2[n6] = true;
                arrayDeque.add(n6);
            }
        }
        Iterator iterator = arrayList.iterator();
        while (iterator.hasNext()) {
            n9 = (Integer)iterator.next();
            blArray[n9] = true;
        }
        if (!bl || arrayList.isEmpty()) {
            return;
        }
        iterator = arrayList.iterator();
        block3: while (iterator.hasNext()) {
            n9 = (Integer)iterator.next();
            n8 = n3 + (n9 >> 4);
            n7 = n4 + (n9 & 0xF);
            for (n6 = n2; n6 > n5; --n6) {
                mutableBlockPos.set(n8, n6, n7);
                BlockState blockState2 = worldGenLevel.getBlockState(mutableBlockPos);
                if (!blockState2.isAir()) continue block3;
                RiverCarverFeature.a(worldGenLevel, (BlockPos)mutableBlockPos, blockState);
            }
        }
    }

    private static boolean f(WorldGenLevel worldGenLevel, BlockPos.MutableBlockPos mutableBlockPos, int n, int n2, int n3, int n4) {
        for (int i = n2 - 1; i >= Math.max(n4, n2 - 6); --i) {
            BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos.set(n, i, n3));
            if (blockState.isAir() || !blockState.getFluidState().isEmpty()) continue;
            return true;
        }
        return false;
    }

    private static int a(RiverEngine riverEngine, WorldGenLevel worldGenLevel, ColumnPlan columnPlan, int n, int n2, int n3) {
        int n4 = RiverCarverFeature.a(worldGenLevel, n, n2);
        if (n4 == n3) {
            return Integer.MIN_VALUE;
        }
        int n5 = columnPlan.waterSurfaceY();
        double d = columnPlan.distance() - columnPlan.width();
        double d2 = Math.min(2.0, riverEngine.config().bankSlope() + riverEngine.config().bankSteepFactor() * Math.pow(columnPlan.slope(), 0.8));
        double d3 = RiverCarverFeature.a((double)n5, d, d2, columnPlan.width(), Math.max(5.0, columnPlan.width() * riverEngine.config().bankWidth()), (double)n4);
        double d4 = (double)n5 + columnPlan.bankBlend() * (double)(n4 - n5);
        return Mth.clamp((int)Math.floor(Math.max(d3, d4)), n5, Math.max(n5, n4));
    }

    private static int a(ColumnPlan columnPlan, ColumnPlan columnPlan2) {
        ColumnPlan columnPlan3 = columnPlan.waterSurfaceY() >= columnPlan2.waterSurfaceY() ? columnPlan : columnPlan2;
        int n = Math.min(columnPlan.waterSurfaceY(), columnPlan2.waterSurfaceY());
        double d = Math.max(0.0, columnPlan3.distance() - columnPlan3.width());
        int n2 = (int)Math.floor(Math.max(0.0, d - 3.0) * 2.0);
        return Math.max(n, columnPlan3.waterSurfaceY() + 1 - n2);
    }

    private static int a(RiverEngine riverEngine, ColumnPlan columnPlan, int n, double d) {
        double d2 = columnPlan.distance() - columnPlan.width();
        if (d2 <= 0.0) {
            return Integer.MAX_VALUE;
        }
        int n2 = Math.min(columnPlan.waterSurfaceY(), n);
        double d3 = Math.min(2.0, riverEngine.config().bankSlope() + riverEngine.config().bankSteepFactor() * Math.pow(columnPlan.slope(), 0.8));
        double d4 = RiverCarverFeature.a((double)n2, d2, d3, columnPlan.width(), Math.max(5.0, columnPlan.width() * riverEngine.config().bankWidth()), (double)n);
        double d5 = (double)n2 + columnPlan.bankBlend() * (double)(n - n2);
        return Mth.clamp((int)Math.floor(Math.max(d4, d5) + d), n2, Math.max(n2, n));
    }

    private static void b(WorldGenLevel worldGenLevel, int n, int n2, int[] nArray) {
        Iterator iterator;
        if (!aD) {
            return;
        }
        boolean bl = false;
        for (int n3 : nArray) {
            if (n3 <= 0) continue;
            bl = true;
            break;
        }
        if (!bl) {
            return;
        }
        Map<Long, int[]> object = aE.computeIfAbsent(worldGenLevel.getLevel().dimension(), resourceKey -> new ConcurrentHashMap());
        if (object.size() >= 2048 && (iterator = object.keySet().iterator()).hasNext()) {
            iterator.next();
            iterator.remove();
        }
        object.put((long)(n >> 4) << 32 ^ (long)(n2 >> 4) & 0xFFFFFFFFL, (int[])nArray.clone());
    }

    public static void drainDeferredSeams(ServerLevel serverLevel) {
        if (!aD) {
            return;
        }
        Map<Long, int[]> map = aE.get(serverLevel.dimension());
        if (map == null || map.isEmpty()) {
            return;
        }
        RiverEngine riverEngine = StreamsWorldgen.engine(serverLevel);
        if (riverEngine == null) {
            return;
        }
        int n = 0;
        Iterator<Map.Entry<Long, int[]>> iterator = map.entrySet().iterator();
        while (iterator.hasNext() && n < 2) {
            int n2;
            int n3;
            Map.Entry<Long, int[]> entry = iterator.next();
            long l = entry.getKey();
            int n4 = (int)(l >> 32);
            int n5 = (int)l;
            int n6 = n4 << 4;
            int n7 = n5 << 4;
            if (!serverLevel.hasChunk(n4, n5)) {
                iterator.remove();
                continue;
            }
            boolean bl = true;
            for (n3 = -1; n3 <= 1 && bl; ++n3) {
                for (n2 = -1; n2 <= 1 && bl; ++n2) {
                    bl = RiverCarverFeature.b(serverLevel, n6 + n3 * 16, n7 + n2 * 16);
                }
            }
            if (!bl) continue;
            iterator.remove();
            n3 = serverLevel.getMinBuildHeight();
            n2 = serverLevel.getMaxBuildHeight() - 1;
            int[] nArray = new int[256];
            ColumnPlan[] columnPlanArray = new ColumnPlan[256];
            int[] nArray2 = new int[256];
            Arrays.fill(nArray2, Integer.MAX_VALUE);
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    int n8 = i * 16 + j;
                    nArray[n8] = serverLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, n6 + i, n7 + j) - 1;
                    columnPlanArray[n8] = riverEngine.plan(n6 + i, n7 + j);
                }
            }
            RiverCarverFeature.a((WorldGenLevel)serverLevel, riverEngine, n6, n7, nArray, entry.getValue(), columnPlanArray, n3, n2);
            ++n;
        }
    }

    public static void forgetDeferredSeams(ServerLevel serverLevel) {
        aE.remove(serverLevel.dimension());
    }

    static {
        u = new ThreadLocal();
        A = ThreadLocal.withInitial(HashMap::new);
        V = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        ae = List.of();
        ah = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        ai = Set.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.DIRT_PATH);
        az = RiverCarverFeature.a();
        aA = null;
        aB = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        aC = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
        aD = Boolean.getBoolean("streamsreflowing.deferseam");
        aE = new ConcurrentHashMap<ResourceKey<Level>, Map<Long, int[]>>();
    }
}

