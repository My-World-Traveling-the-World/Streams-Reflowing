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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos.MutableBlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

public class RiverCarverFeature extends Feature<NoneFeatureConfiguration> {
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
   private static final Map<Long, BlockState[]> q = new ConcurrentHashMap<>();
   private static final Queue<Long> r = new ConcurrentLinkedQueue<>();
   private static volatile Object s;
   private static final int t = 24;
   private static final ThreadLocal<int[]> u = new ThreadLocal<>();
   private static final int v = 8;
   private static final int w = 3;
   private static final int x = 4;
   private static final int y = 4;
   private static final int z = 3;
   private static final ThreadLocal<HashMap<Long, Integer>> A = ThreadLocal.withInitial(HashMap::new);
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
   private static final int[][] V = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final int W = 6;
   private static final int X = 6;
   private static final int Y = 16;
   private static final int Z = 4;
   private static final int aa = 48;
   private static final int ab = 3;
   private static final int ac = 3;
   private static volatile Object ad;
   private static volatile List<BankStyle> ae = List.of();
   private static final int af = 2048;
   private static final int ag = 10;
   private static final int[][] ah = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final Set<Block> ai = Set.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.DIRT_PATH);
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
   private static final int[] az = a();
   private static volatile int[] aA = null;
   private static final int[] aB = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
   private static final int[] aC = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
   private static final boolean aD = Boolean.getBoolean("streamsreflowing.deferseam");
   private static final Map<ResourceKey<Level>, Map<Long, int[]>> aE = new ConcurrentHashMap<>();
   private static final int aF = 2048;
   private static final int aG = 2;

   private static void a(WorldGenLevel var0, int var1, int var2, BlockState[] var3, BlockState[] var4) {
      ServerLevel var5 = var0.getLevel();
      if (s != var5) {
         q.clear();
         r.clear();
         s = var5;
      }

      BlockState[] var6 = new BlockState[512];
      System.arraycopy(var3, 0, var6, 0, 256);
      System.arraycopy(var4, 0, var6, 256, 256);
      Long var7 = (long)(var1 >> 4) << 32 ^ (long)(var2 >> 4) & 4294967295L;
      if (q.put(var7, var6) == null) {
         r.add(var7);

         while (q.size() > 192) {
            Long var8 = r.poll();
            if (var8 == null) {
               break;
            }

            q.remove(var8);
         }
      }
   }

   private static BlockState a(int var0, int var1, int var2) {
      BlockState[] var3 = q.get((long)(var0 >> 4) << 32 ^ (long)(var1 >> 4) & 4294967295L);
      return var3 == null ? null : var3[var2 + (var0 & 15) * 16 + (var1 & 15)];
   }

   private static BlockState a(int var0, int var1, WorldGenLevel var2, int var3, int var4) {
      BlockState var5 = a(var0, var1, 0);
      return var5 != null ? var5 : a(var2, var0, var1, var3, var4);
   }

   private static BlockState a(int var0, int var1, WorldGenLevel var2, int var3) {
      BlockState var4 = a(var0, var1, 256);
      return var4 != null ? var4 : a(var2, var0, var1, var3);
   }

   private static boolean a(int var0, int var1) {
      int[] var2 = u.get();
      return var2 == null || var0 >= var2[0] && var0 <= var2[1] && var1 >= var2[2] && var1 <= var2[3];
   }

   public RiverCarverFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> var1) {
      if (!Services.PLATFORM.generateRivers()) {
         return false;
      } else {
         WorldGenLevel var2 = var1.level();
         RiverEngine var3 = StreamsWorldgen.engine(var2);
         RandomSource var4 = var1.random();
         FlowTrace.carvedChunks.incrementAndGet();
         ChunkPos var5 = new ChunkPos(var1.origin());
         int var6 = var5.getMinBlockX();
         int var7 = var5.getMinBlockZ();
         u.set(new int[]{var6 - 16, var6 + 31, var7 - 16, var7 + 31});
         int var8 = var2.getMinBuildHeight() + 1;
         int var9 = var2.getMaxBuildHeight() - 1;
         int var10 = var3.config().seaLevel();
         boolean var11 = Services.PLATFORM.customBanks();
         BlockState var12 = Blocks.WATER.defaultBlockState();
         BlockState var13 = Blocks.AIR.defaultBlockState();
         BlockState var14 = Blocks.GRASS_BLOCK.defaultBlockState();
         BlockState var15 = Blocks.SHORT_GRASS.defaultBlockState();
         BlockState var16 = Blocks.TALL_GRASS.defaultBlockState();
         BlockState var17 = Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
         MutableBlockPos var18 = new MutableBlockPos();
         boolean var19 = false;
         int[] var20 = new int[256];
         BlockState[] var21 = new BlockState[256];
         BlockState[] var22 = new BlockState[256];

         for (int var23 = 0; var23 < 16; var23++) {
            for (int var24 = 0; var24 < 16; var24++) {
               int var25 = var2.getHeight(Types.OCEAN_FLOOR_WG, var6 + var23, var7 + var24) - 1;
               var20[var23 * 16 + var24] = var25;
               var21[var23 * 16 + var24] = a(var2, var6 + var23, var7 + var24, var25, var8);
               var22[var23 * 16 + var24] = a(var2, var6 + var23, var7 + var24, var25);
            }
         }

         int[] var92 = new int[256];
         A.get().clear();
         int[] var93 = new int[256];
         Arrays.fill(var93, Integer.MIN_VALUE);
         int[] var94 = new int[256];
         Arrays.fill(var94, Integer.MIN_VALUE);
         int[] var26 = new int[256];
         Arrays.fill(var26, Integer.MIN_VALUE);
         int[] var27 = new int[256];
         Arrays.fill(var27, Integer.MIN_VALUE);
         int[] var28 = new int[256];
         if (var3.config().growFromWater()) {
            for (int var29 = 0; var29 < 16; var29++) {
               for (int var30 = 0; var30 < 16; var30++) {
                  int var31 = var6 + var29;
                  int var32 = var7 + var30;
                  Lake var33 = var3.lakeNear(var31, var32, 2);
                  if (var33 != null && var33.digDepth() <= 0 && var33.coverageSmooth(var31, var32) > 0.5) {
                     int var34 = var29 * 16 + var30;
                     var27[var34] = Mth.clamp(var33.level(), var8, var9);
                     var28[var34] = 3;
                  }
               }
            }

            if (var3.config().containmentBerm()) {
               for (int var95 = 0; var95 < 16; var95++) {
                  for (int var97 = 0; var97 < 16; var97++) {
                     int var99 = var95 * 16 + var97;
                     if (var27[var99] == Integer.MIN_VALUE) {
                        int var102 = var6 + var95;
                        int var105 = var7 + var97;
                        Lake var107 = var3.lakeNear(var102, var105, 4);
                        if (var107 != null && var107.digDepth() <= 0 && !(var107.coverageSmooth(var102, var105) > 0.5)) {
                           int var35 = 0;
                           int var36 = 0;
                           int var37 = 0;

                           label1789:
                           for (int var38 = 1; var38 <= 8; var38++) {
                              for (int var39 = -var38; var39 <= var38; var39++) {
                                 for (int var40 = -var38; var40 <= var38; var40++) {
                                    if (Math.max(Math.abs(var39), Math.abs(var40)) == var38 && var107.coverageSmooth(var102 + var39, var105 + var40) > 0.5) {
                                       var35 = var38;
                                       var36 = var39;
                                       var37 = var40;
                                       break label1789;
                                    }
                                 }
                              }
                           }

                           if (var35 != 0 && !var3.inCorridor(var102, var105)) {
                              int var120 = Mth.clamp(var107.level(), var8, var9);
                              int var129 = var20[var99];
                              if (var129 < var120) {
                                 boolean var137 = false;
                                 int var41 = Math.max(Math.abs(var36), Math.abs(var37));

                                 for (int var42 = 1; var42 < var41 && !var137; var42++) {
                                    int var43 = var102 + (int)Math.round((double)var36 * ((double)var42 / (double)var41));
                                    int var44 = var105 + (int)Math.round((double)var37 * ((double)var42 / (double)var41));
                                    if (var107.coverageSmooth(var43, var44) > 0.5) {
                                       break;
                                    }

                                    int var45 = var43 - var6;
                                    int var46 = var44 - var7;
                                    int var47 = var45 >= 0 && var45 < 16 && var46 >= 0 && var46 < 16
                                       ? var20[var45 * 16 + var46]
                                       : var2.getHeight(Types.OCEAN_FLOOR_WG, var43, var44) - 1;
                                    var137 = var47 >= var120;
                                 }

                                 if (!var137) {
                                    double var149 = d(var102, var105);
                                    int var161 = 1 + (var149 > 0.8 ? 1 : 0);
                                    int var168 = var161 + 2 + (var149 < 0.2 ? 1 : 0);
                                    int var177;
                                    if (var35 <= var161) {
                                       var177 = var120;
                                    } else if (var35 <= var168) {
                                       var177 = Math.min(var9, var120 + 1);
                                    } else {
                                       int var188 = Math.min(var9, var120 + 1);
                                       double var48 = Math.min(1.0, (double)(var35 - var168) / 4.0);
                                       var177 = (int)Math.round((double)var188 - var48 * (double)(var188 - var129));
                                       if (var177 <= var129) {
                                          continue;
                                       }
                                    }

                                    BlockState var189 = a(var2, var102, var105, var129, var8);
                                    boolean var200 = false;

                                    for (int var49 = var129 + 1; var49 <= var177; var49++) {
                                       var18.set(var102, var49, var105);
                                       BlockState var50 = var2.getBlockState(var18);
                                       if (var50.isAir()) {
                                          BlockState var51 = a(var2, var102, var105, var129, var177 - var49, var189, var8);
                                          a(var2, var18, var102, var49, var105, var51, var8);
                                          var200 = true;
                                       }
                                    }

                                    if (var200) {
                                       var19 = true;
                                       if (c(var102, var105)) {
                                          b(var102, var105, "LAKE-BERM ground=" + var129 + " top=" + var177);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         ColumnPlan[] var96 = new ColumnPlan[256];
         int[] var98 = new int[256];
         Arrays.fill(var98, Integer.MAX_VALUE);

         for (int var100 = 0; var100 < 16; var100++) {
            for (int var103 = 0; var103 < 16; var103++) {
               var96[var100 * 16 + var103] = var3.plan(var6 + var100, var7 + var103);
            }
         }

         byte var101 = 20;
         int[] var104 = new int[400];
         double[] var106 = new double[400];
         Arrays.fill(var104, Integer.MIN_VALUE);

         for (int var108 = -2; var108 < 18; var108++) {
            for (int var110 = -2; var110 < 18; var110++) {
               int var113 = (var108 + 2) * 20 + var110 + 2;
               int var116 = var6 + var108;
               int var121 = var7 + var110;
               boolean var130 = var108 >= 0 && var108 < 16 && var110 >= 0 && var110 < 16;
               ColumnPlan var138 = var130 ? var96[var108 * 16 + var110] : var3.planLean(var116, var121);
               if (var138 != null && var138.inCorridor() && !var138.inChannel()) {
                  var106[var113] = var138.distance();
                  var104[var113] = a(var3, var2, var138, var116, var121, var8);
               }
            }
         }

         int[] var109 = new int[256];
         Arrays.fill(var109, -2147483647);

         for (int var111 = 0; var111 < 16; var111++) {
            for (int var114 = 0; var114 < 16; var114++) {
               int var117 = var6 + var111;
               int var122 = var7 + var114;
               ColumnPlan var131 = var3.plan(var117, var122);
               var96[var111 * 16 + var114] = var131;
               if (var131 != ColumnPlan.NONE && var131.inChannel()) {
                  var98[var111 * 16 + var114] = a(var2, var3, var18, var117, var122, var131, var9);
               }
            }
         }

         int[] var112 = new int[256];
         int[] var115 = new int[256];

         for (int var118 = 0; var118 < 16; var118++) {
            for (int var123 = 0; var123 < 16; var123++) {
               ColumnPlan var132 = var96[var118 * 16 + var123];
               if (var132 != null && var132.inCorridor()) {
                  int var139 = var132.waterSurfaceY();
                  int var144 = Integer.MAX_VALUE;
                  int var150 = Integer.MAX_VALUE;

                  for (int var155 = -2; var155 <= 2; var155++) {
                     for (int var162 = -2; var162 <= 2; var162++) {
                        if (var155 != 0 || var162 != 0) {
                           int var169 = var118 + var155;
                           int var178 = var123 + var162;
                           ColumnPlan var190 = var169 >= 0 && var169 < 16 && var178 >= 0 && var178 < 16
                              ? var96[var169 * 16 + var178]
                              : var3.planLean(var6 + var169, var7 + var178);
                           if (var190 != null && var190.inCorridor()) {
                              int var201 = var190.waterSurfaceY();
                              if (Math.max(Math.abs(var155), Math.abs(var162)) <= 1) {
                                 var144 = Math.min(var144, var201);
                              }

                              var150 = Math.min(var150, var201);
                           }
                        }
                     }
                  }

                  if (var144 != Integer.MAX_VALUE && var139 - var144 >= 4) {
                     var112[var118 * 16 + var123] = 2;
                     var115[var118 * 16 + var123] = var144 + 1;
                  } else if (var150 != Integer.MAX_VALUE && var139 - var150 >= 4) {
                     var112[var118 * 16 + var123] = 1;
                     var115[var118 * 16 + var123] = var150 + 1;
                  }
               }
            }
         }

         int[] var119 = (int[])var98.clone();

         for (int var124 = 0; var124 < 16; var124++) {
            for (int var133 = 0; var133 < 16; var133++) {
               if (var98[var124 * 16 + var133] != Integer.MAX_VALUE) {
                  int var140 = 0;
                  int var145 = 0;
                  int var151 = Integer.MAX_VALUE;

                  for (int var156 = -4; var156 <= 4; var156++) {
                     for (int var163 = -4; var163 <= 4; var163++) {
                        int var170 = var124 + var156;
                        int var179 = var133 + var163;
                        if (var170 >= 0 && var170 <= 15 && var179 >= 0 && var179 <= 15) {
                           int var191 = var98[var170 * 16 + var179];
                           if (var191 != Integer.MAX_VALUE) {
                              var145++;
                              if (var191 != Integer.MIN_VALUE) {
                                 var140++;
                                 var151 = Math.min(var151, var191);
                              }
                           }
                        }
                     }
                  }

                  var119[var124 * 16 + var133] = var140 * 2 > var145 ? var151 : Integer.MIN_VALUE;
               }
            }
         }

         System.arraycopy(var119, 0, var98, 0, var98.length);

         for (int var125 = 0; var125 < 16; var125++) {
            for (int var134 = 0; var134 < 16; var134++) {
               int var141 = var6 + var125;
               int var146 = var7 + var134;
               int var152 = var20[var125 * 16 + var134];
               var18.set(var141, var152, var146);
               if (!StreamBiomeBlacklist.isBlocked(var2.getUncachedNoiseBiome(var141 >> 2, var152 >> 2, var146 >> 2))) {
                  boolean var157 = false;
                  int var164 = var27[var125 * 16 + var134];
                  if (var164 != Integer.MIN_VALUE) {
                     ColumnPlan var171 = var3.plan(var141, var146);
                     if (c(var141, var146)) {
                        b(
                           var141,
                           var146,
                           String.format(
                              Locale.ROOT,
                              "LAKE-SNAP snapY=%d ground=%d corr=%s chan=%s dist=%.1f w=%.1f surf=%d%s",
                              var164,
                              var152,
                              var171.inCorridor(),
                              var171.inChannel(),
                              var171.distance(),
                              var171.width(),
                              var171.waterSurfaceY(),
                              var171.inCorridor() ? "" : " reject=[" + var3.planDiag(var141, var146) + "]"
                           )
                        );
                     }

                     if (var171.inChannel()) {
                        double var180 = var171.width() > 1.0E-6 ? Math.min(1.0, var171.distance() / var171.width()) : 1.0;
                        int var202 = Math.max(1, (int)Math.round(2.0 * var171.width() * var3.config().streamDepth()));
                        int var208 = Math.min(var164, var171.waterSurfaceY());
                        int var217 = var208 - (int)Math.round((double)var202 * (1.0 - var180 * var180));
                        int var226 = Math.max(var8, Math.min(var152, var217));
                        if (c(var141, var146)) {
                           b(var141, var146, "LAKE-SNAP-DIG ground=" + var152 + " digTo=" + var226 + " bowlRef=" + var208);
                        }

                        for (int var52 = var152; var52 > var226; var52--) {
                           var18.set(var141, var52, var146);
                           var2.setBlock(var18, var12, 2);
                        }

                        if (var226 < var152) {
                           b(var2, var18, var141, var146, var226, var226, var152, var8);
                        }
                     }

                     if (var152 >= var164 && !var171.inChannel()) {
                        int var181 = Math.max(var8, var164 - 2);

                        for (int var192 = var152; var192 > var164 && var192 > var8; var192--) {
                           var18.set(var141, var192, var146);
                           var2.setBlock(var18, var13, 2);
                        }

                        for (int var193 = Math.min(var164, var152); var193 > var181 && var193 > var8; var193--) {
                           var18.set(var141, var193, var146);
                           var2.setBlock(var18, var12, 2);
                        }

                        b(var2, var18, var141, var146, var181, var181, var152, var8);
                        if (c(var141, var146)) {
                           b(var141, var146, "LAKE-EXCAVATE ground=" + var152 + " level=" + var164 + " bedTop=" + var181);
                        }
                     }

                     for (int var182 = var164; var182 > var152 && var182 > var8; var182--) {
                        var18.set(var141, var182, var146);
                        a(var2, var18, var12);
                     }

                     a(var2, var18, var141, var164, var146, var12);
                     c(var2, var18, var141, var146, Math.max(var152 + 1, var164 + 1), var9);
                     var19 = true;
                     var96[var125 * 16 + var134] = var171;
                     if (!var171.inCorridor()) {
                        continue;
                     }

                     var157 = true;
                  }

                  int var172 = var125 * 16 + var134;
                  int var183 = Integer.MIN_VALUE;

                  for (int[] var218 : o) {
                     int var227 = var141 + var218[0];
                     int var234 = var146 + var218[1];
                     Lake var53 = var3.lakeNear(var227, var234, 2);
                     if (var53 != null && var53.digDepth() <= 0 && var53.coverageSmooth(var227, var234) > 0.5) {
                        var183 = Math.max(var183, Mth.clamp(var53.level(), var8, var9));
                     }
                  }

                  Lake var195 = a(var3, var141, var146);
                  var109[var172] = var195 == null ? Integer.MIN_VALUE : var195.level();
                  if (!var157 && var195 != null) {
                     int var204 = Mth.clamp(var195.level(), var8, var9);
                     if (c(var141, var146)) {
                        b(
                           var141,
                           var146,
                           "LAKE level=" + var204 + " dig=" + var195.digDepth() + " ground=" + var152 + " fills=" + (var195.digDepth() > 0 || var152 < var204)
                        );
                     }

                     if (var195.digDepth() > 0) {
                        int var214 = Mth.clamp(Math.min(var152, var195.level() - var195.digDepth()), var8, var204);
                        ColumnPlan var224 = var3.plan(var141, var146);
                        var96[var125 * 16 + var134] = var224;
                        int var232 = Integer.MAX_VALUE;
                        if (var224.inChannel()) {
                           double var239 = var224.width() > 1.0E-6 ? Math.min(1.0, var224.distance() / var224.width()) : 1.0;
                           int var250 = Math.max(1, (int)Math.round(2.0 * var224.width() * var3.config().streamDepth()));
                           var232 = var204 - (int)Math.round((double)var250 * (1.0 - var239 * var239));
                           var214 = Mth.clamp(Math.min(var214, var232), var8, var204);
                        }

                        int var240 = Mth.clamp(Math.max(var152, var195.level()), var8, var9);

                        for (int var246 = var240; var246 > var214; var246--) {
                           var18.set(var141, var246, var146);
                           var2.setBlock(var18, var246 <= var204 ? var12 : var13, 2);
                        }

                        if (var232 != Integer.MAX_VALUE && var214 < var152) {
                           b(var2, var18, var141, var146, var214, var214, var152, var8);
                        }

                        c(var2, var18, var141, var146, var204 + 1, var9);
                        var19 = true;
                        continue;
                     }

                     if (var152 < var204) {
                        ColumnPlan var219 = var3.plan(var141, var146);
                        var96[var125 * 16 + var134] = var219;
                        if (c(var141, var146)) {
                           b(
                              var141,
                              var146,
                              String.format(
                                 Locale.ROOT,
                                 "LAKE-CROSS corr=%s chan=%s dist=%.1f w=%.1f surf=%d%s",
                                 var219.inCorridor(),
                                 var219.inChannel(),
                                 var219.distance(),
                                 var219.width(),
                                 var219.waterSurfaceY(),
                                 var219.inCorridor() ? "" : " reject=[" + var3.planDiag(var141, var146) + "]"
                              )
                           );
                        }

                        if (var219.inChannel()) {
                           double var228 = var219.width() > 1.0E-6 ? Math.min(1.0, var219.distance() / var219.width()) : 1.0;
                           int var242 = Math.max(1, (int)Math.round(2.0 * var219.width() * var3.config().streamDepth()));
                           int var54 = Math.min(var204, var219.waterSurfaceY());
                           int var55 = var54 - (int)Math.round((double)var242 * (1.0 - var228 * var228));
                           int var210 = Math.max(var8, Math.min(var152, var55));
                           if (c(var141, var146)) {
                              b(var141, var146, "LAKE-DIG ground=" + var152 + " digTo=" + var210 + " bowlRef=" + var54 + " centerDepth=" + var242);
                           }

                           for (int var56 = var152; var56 > var210; var56--) {
                              var18.set(var141, var56, var146);
                              var2.setBlock(var18, var12, 2);
                           }

                           if (var210 < var152) {
                              b(var2, var18, var141, var146, var210, var210, var152, var8);
                           }
                        }

                        for (int var229 = var204; var229 > var152 && var229 > var8; var229--) {
                           var18.set(var141, var229, var146);
                           a(var2, var18, var12);
                        }

                        a(var2, var18, var141, var204, var146, var12);
                        c(var2, var18, var141, var146, Math.max(var152 + 1, var204 + 1), var9);
                        var19 = true;
                        if (!var3.plan(var141, var146).inCorridor()) {
                           if (c(var141, var146)) {
                              b(var141, var146, "LAKE-STOP not in any corridor -> lake fill only");
                           }
                           continue;
                        }

                        var157 = true;
                     }

                     if (var2.getBlockState(var18.set(var141, var204, var146)).isAir()) {
                        int var211 = Integer.MIN_VALUE;

                        for (int var220 = var204 - 1; var220 >= Math.max(var8, var204 - 24); var220--) {
                           var18.set(var141, var220, var146);
                           if (!var2.getBlockState(var18).isAir() && var2.getBlockState(var18).getFluidState().isEmpty()) {
                              var211 = var220;
                              break;
                           }
                        }

                        if (var211 != Integer.MIN_VALUE) {
                           for (int var223 = var204; var223 > var211; var223--) {
                              var18.set(var141, var223, var146);
                              a(var2, var18, var12);
                           }

                           var19 = true;
                           continue;
                        }
                     }
                  }

                  ColumnPlan var205 = var3.plan(var141, var146);
                  var96[var172] = var205;
                  if (c(var141, var146)) {
                     b(
                        var141,
                        var146,
                        String.format(
                           Locale.ROOT,
                           "plan corr=%s chan=%s dist=%.1f w=%.1f surf=%d term=%s src=%s ground=%d lake=%s samp=%.1f exact=%.1f%s",
                           var205.inCorridor(),
                           var205.inChannel(),
                           var205.distance(),
                           var205.width(),
                           var205.waterSurfaceY(),
                           var205.terminal(),
                           var205.source(),
                           var152,
                           var195 == null ? "-" : String.valueOf(var195.level()),
                           var3.sampledHeight(var141, var146),
                           var3.exactHeight(var141, var146),
                           var205.inCorridor() ? "" : " reject=[" + var3.planDiag(var141, var146) + "]"
                        )
                     );
                  }

                  if (!var205.inCorridor()) {
                     if (b(var141, var146)) {
                        a(var141, var146, "EXIT: not in any corridor -- the carve never touches this column");
                     }
                  } else {
                     if (b(var141, var146)) {
                        a(var141, var146, "=== STACK for (" + var141 + "," + var146 + ") ===");
                        a(
                           var141,
                           var146,
                           "  plan: chan="
                              + var205.inChannel()
                              + " dist="
                              + String.format(Locale.ROOT, "%.2f", var205.distance())
                              + " width="
                              + String.format(Locale.ROOT, "%.2f", var205.width())
                              + " grade="
                              + var205.waterSurfaceY()
                              + " ground="
                              + var152
                              + " terminal="
                              + var205.terminal()
                              + " source="
                              + var205.source()
                        );
                     }

                     var18.set(var141, var152 + 1, var146);
                     if (!var157 && !var2.getBlockState(var18).getFluidState().isEmpty()) {
                        int var212 = var152 + 1;

                        while (var212 + 1 <= var9 && !var2.getBlockState(var18.set(var141, var212 + 1, var146)).getFluidState().isEmpty()) {
                           var212++;
                        }

                        byte var221 = 6;
                        boolean var230 = a(var2, var18, var141, var146, var205, var9) != Integer.MIN_VALUE;
                        if (c(var141, var146)) {
                           b(
                              var141,
                              var146,
                              "FLUSH check fluidTop="
                                 + var212
                                 + " margin="
                                 + var221
                                 + " fullWidth="
                                 + var230
                                 + " chan="
                                 + var205.inChannel()
                                 + " skip="
                                 + (var205.terminal() || var205.inChannel() && var230 && var205.waterSurfaceY() <= var212 + var221)
                           );
                        }

                        boolean var235 = var205.inChannel() && a(var2, var3, var18, var141, var146, var205, var9, var125, var134, var98) != Integer.MIN_VALUE;
                        if (b(var141, var146)) {
                           a(var141, var146, "== FLUSH stage ==");
                           a(
                              var141,
                              var146,
                              "  fluidTop="
                                 + var212
                                 + " grade="
                                 + var205.waterSurfaceY()
                                 + " terminal="
                                 + var205.terminal()
                                 + " reachDropped="
                                 + var235
                                 + " -> "
                                 + (!var205.terminal() && !var235 ? "carve on" : "SKIP (nothing built)")
                           );
                        }

                        if (c(var141, var146)) {
                           b(
                              var141,
                              var146,
                              "FLUSH check fluidTop="
                                 + var212
                                 + " chan="
                                 + var205.inChannel()
                                 + " reachDropped="
                                 + var235
                                 + " skip="
                                 + (var205.terminal() || var235)
                           );
                        }

                        if (var205.terminal() || var235) {
                           if (var205.inChannel() && a(var2, var18, var141, var146, var205, var3, var152, var212, var8, var12)) {
                              var19 = true;
                           }
                           continue;
                        }
                     }

                     var18.set(var141, var152, var146);
                     boolean var213 = var2.getBlockState(var18).is(Blocks.GRASS_BLOCK);
                     boolean var222 = var205.terminal()
                        && (var205.waterSurfaceY() <= var10 - 1 + 3 || var195 != null && var195.level() >= var205.waterSurfaceY() - 3);
                     int var231 = var205.waterSurfaceY();
                     if (var195 == null
                        && !var157
                        && var3.config().containmentBerm()
                        && !var222
                        && !var205.source()
                        && var152 < var231
                        && var152 >= var231 - 4
                        && var205.distance() <= var205.width()
                        && a(var2, var141, var146, var205, var231)
                        && b(var2, var141, var146, var152, var231, var8)) {
                        int var238 = Math.min(var9, var231);
                        if (c(var141, var146)) {
                           b(var141, var146, "POOL surface=" + var238 + " ground=" + var152);
                        }

                        for (int var245 = var238; var245 > var152 && var245 > var8; var245--) {
                           var18.set(var141, var245, var146);
                           a(var2, var18, var12);
                        }

                        c(var2, var18, var141, var146, Math.max(var152 + 1, var238 + 1), var9);
                        var19 = true;
                     } else if (var205.inChannel()) {
                        boolean var237 = var3.config().containmentBerm() && !var222 && !var157;
                        int var244 = var205.waterSurfaceY();
                        int var249 = Integer.MAX_VALUE;
                        int var253 = var237 ? a(var2, var141, var146, var205, var152, var8, var9) : Math.min(Math.min(var244, var152), var249);
                        var253 = Mth.clamp(var253, var8, var9);
                        if (b(var141, var146)) {
                           a(var141, var146, "== CHANNEL surface derivation ==");
                           a(
                              var141,
                              var146,
                              "  plan grade="
                                 + var244
                                 + " ground="
                                 + var152
                                 + " dist="
                                 + String.format(Locale.ROOT, "%.2f", var205.distance())
                                 + " width="
                                 + String.format(Locale.ROOT, "%.2f", var205.width())
                                 + " contain="
                                 + var237
                                 + " slopeCap="
                                 + var249
                                 + " terminalOpen="
                                 + var222
                                 + " throughLake="
                                 + var157
                           );
                           a(var141, var146, "  base (containedWaterLevel): " + var253);
                        }

                        if (var205.source() && var152 - 1 > var8) {
                           var253 = a(var141, var146, "source recess", var253, Math.min(var253, var152 - 1));
                        }

                        int var258 = a(var3, var2, var205, var141, var146, var152, nearbyLakeLevel(var3, var141, var146, var8, var9), var10, var8, var9);
                        if (var258 != Integer.MIN_VALUE) {
                           var253 = a(var141, var146, "lake level-out (" + var258 + ")", var253, Mth.clamp(Math.max(var253, var258), var8, var9));
                        }

                        if (var195 != null && var195.level() >= var253 && var152 < var195.level()) {
                           if (c(var141, var146)) {
                              b(var141, var146, "LAKE-FLUSH surface " + var253 + " -> " + var195.level());
                           }

                           var253 = a(var141, var146, "submerged in lake " + var195.level(), var253, Mth.clamp(var195.level(), var8, var9));
                        }

                        if (var183 != Integer.MIN_VALUE) {
                           var253 = a(var141, var146, "flood level (" + var183 + ")", var253, Mth.clamp(Math.max(var253, var183), var8, var9));
                        }

                        double var260 = Math.hypot(var205.flowDirX(), var205.flowDirZ());
                        if (var260 > 1.0E-6) {
                           double var59 = -var205.flowDirZ() / var260;
                           double var61 = var205.flowDirX() / var260;

                           for (int var268 = -1; var268 <= 1; var268 += 2) {
                              for (int var270 = 2; var270 <= 6; var270 += 2) {
                                 ColumnPlan var273 = var3.plan(
                                    var141 + (int)Math.round(var59 * (double)var270 * (double)var268),
                                    var146 + (int)Math.round(var61 * (double)var270 * (double)var268)
                                 );
                                 if (var273.inCorridor() && var273.inChannel() && var273.waterSurfaceY() < var253 && var253 - var273.waterSurfaceY() <= 4) {
                                    var253 = a(
                                       var141, var146, "junction pool (neighbour at r=" + var270 + " side=" + var268 + ")", var253, var273.waterSurfaceY()
                                    );
                                 }
                              }
                           }
                        }

                        int var262 = a(var2, var3, var18, var141, var146, var205, var9, var125, var134, var98);
                        int var264 = var262 == Integer.MIN_VALUE ? var253 : Math.min(var253, var262);
                        if (var264 != var253 && c(var141, var146)) {
                           b(var141, var146, "GRADE-CLAMP " + var253 + " -> " + var264 + " (fell into water upstream)");
                        }

                        var253 = a(var141, var146, "drop clamp (inherited=" + var262 + ")", var253, var264);
                        if (var112[var172] > 0) {
                           int var265 = Math.max(var115[var172], var253 - var112[var172]);
                           if (var265 < var253) {
                              if (c(var141, var146)) {
                                 b(var141, var146, "LIP-SHAVE surface " + var253 + " -> " + var265 + " floor=" + var115[var172] + " shave=" + var112[var172]);
                              }

                              var253 = a(var141, var146, "lip shave", var253, var265);
                           }
                        }

                        double var266 = var205.width() > 1.0E-6 ? Math.min(1.0, var205.distance() / var205.width()) : 1.0;
                        int var269 = Math.max(1, (int)Math.round(2.0 * var205.width() * var3.config().streamDepth()));
                        int var271 = (int)Math.round((double)var269 * (1.0 - var266 * var266));
                        int var274 = Mth.clamp(var253 - var271, var8, var253 - 1);
                        int var277 = Math.min(var9, Math.max(var152, var253));
                        var18.set(var141, var274, var146);
                        BlockState var280 = var2.getBlockState(var18);
                        boolean var282 = !var280.isAir() && var280.getFluidState().isEmpty();
                        var93[var172] = var253;
                        if (b(var141, var146)) {
                           a(var141, var146, "  FINAL channel surface=" + var253 + " bed=" + var274 + " supported=" + var282);
                        }

                        if (c(var141, var146)) {
                           b(
                              var141,
                              var146,
                              "CHANNEL surface="
                                 + var253
                                 + " bed="
                                 + var274
                                 + " top="
                                 + Math.min(var9, Math.max(var152, var253))
                                 + " supported="
                                 + var282
                                 + " contain="
                                 + var237
                           );
                        }

                        int var284 = var274 - var152 >= 4 ? a(var2, var18, var141, var146, var205, var9) : Integer.MIN_VALUE;
                        boolean var286 = var284 != Integer.MIN_VALUE
                           && (var274 - var152 >= 4 && !a(var2, var3, var141, var146, var205, var284) || var205.distToOutlet() <= 24.0);
                        if (var286 && c(var141, var146)) {
                           b(var141, var146, "CUP released: waterfall over water (ground=" + var152 + " waterTop=" + var284 + " bed=" + var274 + ")");
                        }

                        if (var237 && !var205.source() && !var286) {
                           int var287 = a(var141, var146, var253, var205, var3.config().bankWidth());
                           b(var2, var18, var141, var146, var287, var274, var152, var8);
                           var282 = true;
                        }

                        BlockState var288 = Services.PLATFORM.streamWaterState(a(var205));

                        for (int var289 = var277; var289 > var274; var289--) {
                           var18.set(var141, var289, var146);
                           if (var289 > var253) {
                              BlockState var291 = var2.getBlockState(var18);
                              if (!var291.isAir() && !var291.is(BlockTags.LEAVES)) {
                                 if (var291.is(BlockTags.LOGS)) {
                                    var2.setBlock(var18, var13, 2);
                                    b(var2, var18, var141, var146, var289 + 1, var9);
                                 } else if (var282 || var291.getFluidState().isEmpty()) {
                                    var2.setBlock(var18, var13, 2);
                                 }
                              }
                           } else if (var282) {
                              var2.setBlock(var18, var288, 2);
                           } else {
                              BlockState var292 = var2.getBlockState(var18);
                              if (!var292.isAir() && var292.getFluidState().isEmpty()) {
                                 var2.setBlock(var18, var13, 2);
                              }
                           }
                        }

                        if (var282 && a(var3, var141, var146, var253)) {
                           int var290 = 5 + Math.floorMod(Mth.getSeed(var141, 0, var146), 20);
                           var2.scheduleTick(new BlockPos(var141, var253, var146), var288.getFluidState().getType(), var290);
                        }

                        c(var2, var18, var141, var146, var152 + 1, var9);
                        if (var253 < var152) {
                           a(var2, var18, var141, var146, var253 + 1, var152 + 1, var10);
                        }

                        var19 = true;
                     } else {
                        int var236 = var205.waterSurfaceY();
                        if (b(var141, var146)) {
                           a(var141, var146, "== BERM derivation ==");
                           a(var141, var146, "  base waterLevel=" + var236 + " ground=" + var152);
                        }

                        int var243 = a(var3, var2, var205, var141, var146, var152, nearbyLakeLevel(var3, var141, var146, var8, var9), var10, var8, var9);
                        if (var243 != Integer.MIN_VALUE) {
                           var236 = a(var141, var146, "berm lake level-out (" + var243 + ")", var236, Math.max(var236, var243));
                        }

                        if (var195 != null && var195.level() >= var236) {
                           var236 = var195.level();
                        }

                        Object var248 = null;

                        for (int var251 = 0; var251 < 4; var251++) {
                           int var256 = var141 + (var251 == 0 ? 1 : (var251 == 1 ? -1 : 0));
                           int var57 = var146 + (var251 == 2 ? 1 : (var251 == 3 ? -1 : 0));
                           Lake var58 = var3.lakeNear(var256, var57, 2);
                           if (var58 != null && var58.digDepth() <= 0 && var58.level() > var236 && var58.coverageSmooth(var256, var57) > 0.5) {
                              var236 = a(var141, var146, "adjacent lake fill (" + var58.level() + ")", var236, var58.level());
                           }
                        }

                        ColumnPlan var252 = var3.altPlan(var141, var146);
                        if (var252 != null && var252.waterSurfaceY() > var236) {
                           var236 = a(var141, var146, "alt stream water (" + var252.waterSurfaceY() + ")", var236, var252.waterSurfaceY());
                        }

                        var26[var125 * 16 + var134] = var236;
                        int var257 = a(var2, var3, var18, var141, var146, var205, var9, var125, var134, var98);
                        boolean var259 = var257 != Integer.MIN_VALUE;
                        if (var259) {
                           var236 = a(var141, var146, "berm drop clamp (" + var257 + ")", var236, Math.min(var236, var257));
                        }

                        var26[var125 * 16 + var134] = var236;
                        double var261 = Math.hypot(var205.flowDirX(), var205.flowDirZ());
                        if (var261 > 1.0E-6) {
                           ColumnPlan var60 = var3.plan(
                              var141 - (int)Math.round(var205.flowDirX() / var261), var146 - (int)Math.round(var205.flowDirZ() / var261)
                           );
                           if (var60 != ColumnPlan.NONE && var60.waterSurfaceY() - var236 <= 2) {
                              var236 = a(var141, var146, "upstream push (" + var60.waterSurfaceY() + ")", var236, Math.max(var236, var60.waterSurfaceY()));
                           }
                        }

                        double var263 = var205.upstreamSlope();
                        boolean var62 = false;
                        if (var263 >= 0.15) {
                           double var63 = Math.hypot(var205.flowDirX(), var205.flowDirZ());
                           if (var63 > 1.0E-6) {
                              int var65 = var141 - (int)Math.round(4.0 * var205.flowDirX() / var63);
                              int var66 = var146 - (int)Math.round(4.0 * var205.flowDirZ() / var63);
                              ColumnPlan var67 = var3.plan(var65, var66);
                              if (var67 != null && var67 != ColumnPlan.NONE) {
                                 double var68 = Math.hypot(var67.flowDirX(), var67.flowDirZ());
                                 if (var68 > 1.0E-6) {
                                    double var70 = (var205.flowDirX() * var67.flowDirX() + var205.flowDirZ() * var67.flowDirZ()) / (var63 * var68);
                                    var62 = var70 < 0.906;
                                 }
                              }
                           }
                        }

                        int var267 = var62 && var263 >= 0.15 ? 1 : 0;
                        if (var205.waterSurfaceY() < var10) {
                           var267 = Math.max(0, var267 - 1);
                        }

                        int var64 = var236 + var267;
                        var18.set(var141, var236, var146);
                        boolean var272 = var259 && (var152 >= var236 || e(var2, var18, var141, var146, var152, var9) != Integer.MIN_VALUE);
                        if (var3.config().containmentBerm()
                           && (var205.waterSurfaceY() >= var10 || var205.distToOutlet() > 24.0)
                           && var152 < var64
                           && !var222
                           && !var205.source()
                           && var27[var125 * 16 + var134] == Integer.MIN_VALUE
                           && !var272) {
                           int var275 = var267 > 0 ? 2 : 0;
                           double var278 = var205.distance() - var205.width();
                           double var69 = var278 - (double)var275;
                           double var71 = Math.min(2.0, var3.config().bankSlope() + var3.config().bankSteepFactor() * Math.pow(var205.slope(), 0.8));
                           double var73 = 2.0 * (double)var236 - (double)var152;
                           double var75 = (double)var236 + Math.max(0.0, var69) * var71 / 2.0;
                           double var77 = (double)var236 + var205.bankBlend() * (var73 - (double)var236);
                           double var79 = 0.0;
                           int var81 = Mth.clamp((int)Math.floor(Math.max(var75, var77) + var79), var236, (int)Math.max((double)var236, var73));
                           int var82 = 2 * var236 - var81 + var267;
                           if (var278 < (double)var275) {
                              var82 = Math.min(var9, var236);
                           } else {
                              if (var69 <= Math.max(1.0, var205.width() / 3.0) * 2.0) {
                                 int var83 = var64 + (int)Math.round(Math.max(0.0, var79));
                                 var82 = Math.min(var9, Math.max(var82, var83));
                              }

                              int var296 = var267 * 3;
                              if (var296 > 0 && var69 <= (double)var296) {
                                 int var84 = var64 - (int)Math.round(var69 / (double)var296 * (double)var267);
                                 var82 = Math.min(var9, Math.max(var82, var84));
                              }
                           }

                           if (var267 >= 2 && var278 >= (double)var275) {
                              int var297 = var236 + 1 + (int)Math.floor(var278 - (double)var275);
                              var82 = Math.min(var82, var297);
                           }

                           BlockState var298 = a(var2, var141, var146, var152, var8);
                           boolean var307 = false;
                           if (c(var141, var146)) {
                              int var85 = e(var2, var18, var141, var146, var152, var9);
                              var94[var125 * 16 + var134] = Math.max(var94[var125 * 16 + var134], var82);
                              if (b(var141, var146)) {
                                 a(var141, var146, "  BERM BUILT water=" + var236 + " freeboard=" + var267 + " top=" + var82);
                              }

                              b(
                                 var141,
                                 var146,
                                 "BERM water="
                                    + var236
                                    + " freeboard="
                                    + var267
                                    + " dropVerdict="
                                    + (var257 == Integer.MIN_VALUE ? "none" : String.valueOf(var257))
                                    + " top="
                                    + var82
                                    + " ground="
                                    + var152
                                    + " dto="
                                    + (var205.distToOutlet() > 999.0 ? "999" : (int)var205.distToOutlet())
                                    + " wtb="
                                    + (var85 == Integer.MIN_VALUE ? "dry" : String.valueOf(var85))
                              );
                           }

                           int var314 = e(var2, var18, var141, var146, var152, var9);
                           if (var314 != Integer.MIN_VALUE && var314 >= var236) {
                              if (c(var141, var146)) {
                                 b(
                                    var141,
                                    var146,
                                    "BERM skipped: submerged to " + var314 + " >= containment water " + var236 + " (crest would be " + var82 + ")"
                                 );
                              }
                              continue;
                           }

                           var94[var125 * 16 + var134] = Math.max(var94[var125 * 16 + var134], var82);
                           int var86 = Math.max(var8, a(var141, var146, var236, var205, var3.config().bankWidth(), 4));

                           for (int var87 = var86; var87 <= var82 && var87 <= var9; var87++) {
                              var18.set(var141, var87, var146);
                              BlockState var88 = var2.getBlockState(var18);
                              if (var88.isAir()) {
                                 BlockState var89 = a(var2, var141, var146, var152, var82 - var87, var298, var8);
                                 a(var2, var18, var141, var87, var146, var89, var8);
                                 var307 = true;
                              }
                           }

                           if (var307) {
                              var19 = true;
                              continue;
                           }
                        } else if (c(var141, var146) && var3.config().containmentBerm() && var152 < var64 && !var222 && !var205.source()) {
                           if (b(var141, var146)) {
                              a(var141, var146, "  BERM SKIPPED (gates below)");
                           }

                           b(
                              var141,
                              var146,
                              "BERM skipped: waterLevel="
                                 + var236
                                 + " dropped="
                                 + var259
                                 + " mouthGate="
                                 + (var205.waterSurfaceY() < var10 && !(var205.distToOutlet() > 24.0))
                                 + " lakeFill="
                                 + (var27[var125 * 16 + var134] != Integer.MIN_VALUE)
                                 + " terminalOpen="
                                 + var222
                                 + " src="
                                 + var205.source()
                           );
                        }

                        int var276 = Integer.MAX_VALUE;
                        int var279 = Math.min(Math.min(var205.waterSurfaceY(), var152), var276);
                        int var281 = nearbyLakeLevel(var3, var141, var146, var8, var9);
                        int var283 = a(var3, var2, var205, var141, var146, var152, var281, var10, var8, var9);
                        if (var283 != Integer.MIN_VALUE) {
                           var279 = Math.max(var279, var283);
                        }

                        if (var195 != null && var195.level() >= var279 && var152 < var195.level()) {
                           if (c(var141, var146)) {
                              b(var141, var146, "BANK-LAKE-FLUSH surface " + var279 + " -> " + var195.level());
                           }

                           var279 = var195.level();
                        }

                        if (var183 != Integer.MIN_VALUE) {
                           var279 = Math.max(var279, var183);
                        }

                        double var285 = var205.distance() - var205.width();
                        double var72 = Math.min(2.0, var3.config().bankSlope() + var3.config().bankSteepFactor() * Math.pow(var205.slope(), 0.8));
                        double var74 = a(
                           (double)var279, var285, var72, var205.width(), Math.max(5.0, var205.width() * var3.config().bankWidth()), (double)var152
                        );
                        double var76 = (double)var279 + var205.bankBlend() * (double)(var152 - var279);
                        double var78 = (d(var141, var146) - 0.5) * var3.config().bankNoise();
                        int var80 = Mth.clamp((int)Math.floor(Math.max(var74, var76) + var78), var279, Math.max(var279, var152));
                        ColumnPlan var293 = var3.altPlan(var141, var146);
                        if (var293 != null) {
                           int var294 = a(var3, var293, var152, var78);
                           if (var294 != Integer.MAX_VALUE) {
                              int var299 = a(var205, var293);
                              int var308 = Math.max(var294, var299);
                              if (var308 < var80) {
                                 if (c(var141, var146)) {
                                    b(
                                       var141,
                                       var146,
                                       "ALT-MERGE was="
                                          + var80
                                          + " now="
                                          + var308
                                          + " altCut="
                                          + var294
                                          + " floor="
                                          + var299
                                          + " altW="
                                          + String.format(Locale.ROOT, "%.1f", var293.width())
                                          + " altD="
                                          + String.format(Locale.ROOT, "%.1f", var293.distance())
                                          + " altSurf="
                                          + var293.waterSurfaceY()
                                    );
                                 }

                                 var80 = var308;
                              }
                           }
                        }

                        int var295 = Integer.MAX_VALUE;
                        if (var80 >= var152) {
                           for (int var300 = 1; var300 <= 2; var300++) {
                              for (int var309 = 0; var309 < 4; var309++) {
                                 int var315 = var141 + (var309 == 0 ? var300 : (var309 == 1 ? -var300 : 0));
                                 int var321 = var146 + (var309 == 2 ? var300 : (var309 == 3 ? -var300 : 0));
                                 int var325 = var315 - var6 + 2;
                                 int var328 = var321 - var7 + 2;
                                 if (var325 >= 0 && var325 < 20 && var328 >= 0 && var328 < 20) {
                                    int var330 = var325 * 20 + var328;
                                    if (var104[var330] != Integer.MIN_VALUE && !(Math.abs(var106[var330] - var205.distance()) > 2.0)) {
                                       var295 = Math.min(var295, var104[var330] + 1 * (var300 - 1));
                                    }
                                 }
                              }
                           }
                        }

                        if (var295 != Integer.MAX_VALUE && var80 > var295 + 1 + 1) {
                           int var301 = Mth.clamp(var295 + 1, var279, Math.max(var279, var152));
                           if (c(var141, var146)) {
                              b(
                                 var141,
                                 var146,
                                 "BANK-TIE was=" + var80 + " now=" + var301 + " neighbourAllow=" + var295 + " surf=" + var279 + " ground=" + var152
                              );
                           }

                           var80 = var301;
                        }

                        if (var80 < var152) {
                           int var302 = Integer.MAX_VALUE;
                           int var310 = 0;

                           for (int var316 = 0; var316 < 4; var316++) {
                              int var322 = var141 + (var316 == 0 ? 1 : (var316 == 1 ? -1 : 0));
                              int var326 = var146 + (var316 == 2 ? 1 : (var316 == 3 ? -1 : 0));
                              int var329 = var322 - var6 + 2;
                              int var331 = var326 - var7 + 2;
                              if (var329 >= 0 && var329 < 20 && var331 >= 0 && var331 < 20) {
                                 int var90 = var104[var329 * 20 + var331];
                                 if (var90 != Integer.MIN_VALUE) {
                                    var310++;
                                    var302 = Math.min(var302, var90);
                                 }
                              }
                           }

                           if (var310 >= 2 && var80 < var302) {
                              int var317 = Math.min(Math.min(var302, var152), var80 + 2);
                              if (var317 > var80) {
                                 if (c(var141, var146)) {
                                    b(
                                       var141,
                                       var146,
                                       "BANK-DIMPLE was=" + var80 + " now=" + var317 + " lowestBeside=" + var302 + " beside=" + var310 + " ground=" + var152
                                    );
                                 }

                                 var80 = var317;
                              }
                           }
                        }

                        if (var281 != Integer.MIN_VALUE && var195 == null && var80 < var152 && var205.distToOutlet() > 24.0) {
                           int var303 = adjacentCoveringLakeLevel(var3, var141, var146);
                           if (var303 != Integer.MIN_VALUE && var152 > var303 && var80 <= var303 && var205.waterSurfaceY() < var303) {
                              int var311 = Mth.clamp(var303 + 1, var80, var152);
                              if (c(var141, var146)) {
                                 b(
                                    var141,
                                    var146,
                                    "LAKE-RIM-HOLD was="
                                       + var80
                                       + " now="
                                       + var311
                                       + " rim="
                                       + var303
                                       + " streamSurf="
                                       + var205.waterSurfaceY()
                                       + " ground="
                                       + var152
                                       + " distToOutlet="
                                       + String.format(Locale.ROOT, "%.1f", var205.distToOutlet())
                                 );
                              }

                              var80 = var311;
                           }
                        }

                        if (!Boolean.getBoolean("streamsreflowing.noedgegrade")
                           && var281 == Integer.MIN_VALUE
                           && var80 >= var152
                           && var152 > var279
                           && var285 <= 8.0) {
                           double var304 = Math.max(0.6, var72);
                           int var318 = var279 + (int)Math.round(Math.max(0.0, var285) * var304);
                           int var323 = var80;
                           var80 = Mth.clamp(var318, var279, var152);
                           if (var80 != var323 && c(var141, var146)) {
                              b(
                                 var141,
                                 var146,
                                 "EDGE-GRADE was="
                                    + var323
                                    + " now="
                                    + var80
                                    + " graded="
                                    + var318
                                    + " ground="
                                    + var152
                                    + " surf="
                                    + var279
                                    + String.format(Locale.ROOT, " d=%.1f gradeSlope=%.2f", var285, var304)
                              );
                           }
                        }

                        if (var112[var172] > 0 && var80 > var279) {
                           int var305 = Math.max(Math.max(var279, var115[var172]), var80 - var112[var172]);
                           if (var305 < var80) {
                              if (c(var141, var146)) {
                                 b(var141, var146, "LIP-SHAVE cutTop " + var80 + " -> " + var305 + " floor=" + var115[var172] + " shave=" + var112[var172]);
                              }

                              var80 = var305;
                           }
                        }

                        if (c(var141, var146)) {
                           var94[var125 * 16 + var134] = Math.max(var94[var125 * 16 + var134], Math.max(var80, var152));
                           b(
                              var141,
                              var146,
                              "BANK ref="
                                 + var279
                                 + " cutTop="
                                 + var80
                                 + " ground="
                                 + var152
                                 + " d="
                                 + String.format(Locale.ROOT, "%.1f", var285)
                                 + " cut="
                                 + (var152 > var80)
                                 + String.format(
                                    Locale.ROOT,
                                    " slope=%.3f effSlope=%.2f slopeLine=%.2f terrainLine=%.2f blend=%.3f jitter=%+.2f",
                                    var205.slope(),
                                    var72,
                                    var74,
                                    var76,
                                    var205.bankBlend(),
                                    var78
                                 )
                           );
                        }

                        int var306 = var152;
                        if (var152 > var80) {
                           for (int var312 = var152; var312 > var80; var312--) {
                              var18.set(var141, var312, var146);
                              BlockState var319 = var2.getBlockState(var18);
                              if (!var319.isAir() && !var319.is(BlockTags.LEAVES)) {
                                 if (var319.is(BlockTags.LOGS)) {
                                    var2.setBlock(var18, var13, 2);
                                    b(var2, var18, var141, var146, var312 + 1, var9);
                                    var19 = true;
                                 } else {
                                    var2.setBlock(var18, var13, 2);
                                    var19 = true;
                                 }
                              }
                           }

                           var306 = var80;
                           var92[var125 * 16 + var134] = var152 - var80;
                        }

                        if (c(var2, var18, var141, var146, var152 + 1, var9)) {
                           var19 = true;
                        }

                        if (var306 < var152 && a(var2, var18, var141, var146, var6, var7, var306 + 1, var152)) {
                           var19 = true;
                        }

                        if (var306 < var152) {
                           a(var2, var18, var141, var146, var306 + 1, var152 + 1, var10);
                        }

                        if (var213 && var306 >= var8 && var306 < var9) {
                           BlockPos var313 = new BlockPos(var141, var306, var146);
                           BlockState var320 = var2.getBlockState(var313);
                           if (var320.is(Blocks.DIRT) || var320.is(Blocks.COARSE_DIRT) || var320.is(Blocks.GRASS_BLOCK)) {
                              var2.setBlock(var313, var14, 2);
                              BlockPos var324 = var313.above();
                              if (var2.getBlockState(var324).isAir()) {
                                 float var327 = var4.nextFloat();
                                 if (var327 < 0.2F && var324.getY() + 1 < var9 && var2.getBlockState(var324.above()).isAir()) {
                                    var2.setBlock(var324, var16, 2);
                                    var2.setBlock(var324.above(), var17, 2);
                                 } else if (var327 < 0.55F) {
                                    var2.setBlock(var324, var15, 2);
                                 }
                              }

                              var19 = true;
                           }
                        }
                     }
                  }
               }
            }
         }

         for (int var126 = 0; var126 < 16; var126++) {
            for (int var135 = 0; var135 < 16; var135++) {
               int var142 = var126 * 16 + var135;
               int var147 = var94[var142];
               int var153 = var26[var142];
               if (var147 != Integer.MIN_VALUE && var153 != Integer.MIN_VALUE && var147 <= var153) {
                  int var158 = 0;
                  int[] var165 = new int[24];

                  for (int var173 = -2; var173 <= 2; var173++) {
                     for (int var184 = -2; var184 <= 2; var184++) {
                        if (var173 != 0 || var184 != 0) {
                           int var196 = var126 + var173;
                           int var206 = var135 + var184;
                           if (var196 >= 0 && var196 <= 15 && var206 >= 0 && var206 <= 15) {
                              int var215 = var94[var196 * 16 + var206];
                              if (var215 != Integer.MIN_VALUE) {
                                 var165[var158++] = var215;
                              }
                           }
                        }
                     }
                  }

                  if (var158 >= 4) {
                     Arrays.sort(var165, 0, var158);
                     int var174 = var165[var158 / 2];
                     int var185 = Math.min(var174, var153 + 1);
                     if (var185 - var147 >= 2) {
                        int var197 = var6 + var126;
                        int var207 = var7 + var135;
                        int var216 = var20[var142];
                        BlockState var225 = a(var2, var197, var207, var216, var8);
                        boolean var233 = false;

                        for (int var241 = var147 + 1; var241 <= var185 && var241 <= var9; var241++) {
                           var18.set(var197, var241, var207);
                           BlockState var247 = var2.getBlockState(var18);
                           if (var247.isAir()) {
                              if (!d(var2, var18, var197, var241, var207, var8)) {
                                 break;
                              }

                              a(var2, var18, var197, var241, var207, a(var2, var197, var207, var216, Math.max(1, var185 - var241), var225, var8), var8);
                              var233 = true;
                           }
                        }

                        if (var233) {
                           var19 = true;
                           if (c(var197, var207)) {
                              b(var197, var207, "DIP FILL contain " + var147 + " -> " + var185 + " (median " + var174 + ", holds " + var153 + ")");
                           }

                           if (b(var197, var207)) {
                              a(var197, var207, "  DIP FILL PASS contain " + var147 + " -> " + var185);
                           }
                        }
                     }
                  }
               }
            }
         }

         for (int var127 = 0; var127 < 16; var127++) {
            for (int var136 = 0; var136 < 16; var136++) {
               int var143 = var93[var127 * 16 + var136];
               if (var143 != Integer.MIN_VALUE) {
                  int var148 = 0;
                  int[] var154 = new int[24];

                  for (int var159 = -2; var159 <= 2; var159++) {
                     for (int var166 = -2; var166 <= 2; var166++) {
                        if (var159 != 0 || var166 != 0) {
                           int var175 = var127 + var159;
                           int var186 = var136 + var166;
                           if (var175 >= 0 && var175 <= 15 && var186 >= 0 && var186 <= 15) {
                              int var198 = var93[var175 * 16 + var186];
                              if (var198 != Integer.MIN_VALUE) {
                                 var154[var148++] = var198;
                              }
                           }
                        }
                     }
                  }

                  if (var148 >= 4) {
                     Arrays.sort(var154, 0, var148);
                     int var160 = var154[var148 / 2];
                     if (var143 - var160 >= 2) {
                        int var167 = var6 + var127;
                        int var176 = var7 + var136;
                        if (c(var167, var176)) {
                           b(var167, var176, "OUTLIER surface " + var143 + " vs median " + var160 + " -> re-levelled");
                        }

                        if (b(var167, var176)) {
                           a(var167, var176, "  OUTLIER PASS re-levelled " + var143 + " -> " + var160);
                        }

                        for (int var187 = var143; var187 > var160; var187--) {
                           var18.set(var167, var187, var176);
                           BlockState var199 = var2.getBlockState(var18);
                           if (!var199.isAir() && (!var199.getFluidState().isEmpty() || var187 > var160 + 1)) {
                              var2.setBlock(var18, var13, 2);
                           }
                        }

                        var18.set(var167, var160, var176);
                        if (var2.getBlockState(var18).isAir() && d(var2, var18, var167, var160, var176, var8)) {
                           var2.setBlock(var18, var12, 2);
                        }

                        var19 = true;
                     }
                  }
               }
            }
         }

         if (var19) {
            a(var2, var3, var6, var7, var20, var96, var9);
            a(var2, var3, var6, var7, var20, var92, var96, var8, var9);
            b(var2, var6, var7, var92);
            a(var2, var6, var7, var21, var22);
            if (var11) {
               a(var2, var3, var6, var7, var20, var21, var22, var96, var109, var8, var9);
            }
         }

         a(var2, var18, var27, var6, var7, var8, var12);
         if (var19 && !Boolean.getBoolean("streamsreflowing.nobakeflow")) {
            try {
               ServerLevel var128 = var2.getLevel();
               StreamPrefetcher.submitGridBuild(
                  var128,
                  var3,
                  RiverFlowFields.classifierFor(var128),
                  RiverFlowFields.orientationFor(var128),
                  Services.PLATFORM.flowVanillaRivers(),
                  var6 >> 4,
                  var7 >> 4,
                  false,
                  null
               );
            } catch (RuntimeException var91) {
            }
         }

         ChunkPlanCache.put(var3, var6, var7, var96);
         a(var6, var7, var96);
         return var19;
      }
   }

   private static void a(int var0, int var1, ColumnPlan[] var2) {
      try {
         byte[] var3 = new byte[256];
         byte[] var4 = new byte[256];
         short[] var5 = new short[256];
         Arrays.fill(var5, (short)-32768);

         for (int var6 = 0; var6 < var2.length && var6 < var3.length; var6++) {
            ColumnPlan var7 = var2[var6];
            if (var7 != null && var7.inChannel()) {
               double var8 = var7.fluidFlowDirX();
               double var10 = var7.fluidFlowDirZ();
               double var12 = Math.sqrt(var8 * var8 + var10 * var10);
               if (!(var12 < 0.5)) {
                  var3[var6] = (byte)((int)Math.round(var8 / var12 * 127.0));
                  var4[var6] = (byte)((int)Math.round(var10 / var12 * 127.0));
                  int var14 = var7.waterSurfaceY();
                  var5[var6] = var14 >= -32768 && var14 <= 32767 ? (short)var14 : -32768;
               }
            }
         }

         RawFlowTiles.put(var0 >> 4, var1 >> 4, new RawFlowTiles.Tile(var3, var4, var5));
         FlowTrace.tilesFromCarve.incrementAndGet();
      } catch (RuntimeException var15) {
      }
   }

   private static boolean a(WorldGenLevel var0, int var1, int var2, int[] var3) {
      byte var4 = 18;
      int[] var5 = new int[324];

      for (int var6 = -1; var6 <= 16; var6++) {
         for (int var7 = -1; var7 <= 16; var7++) {
            var5[(var6 + 1) * 18 + var7 + 1] = a(var0, var1 + var6, var2 + var7);
         }
      }

      for (int var15 = 0; var15 < 2; var15++) {
         for (int var16 = -1; var16 <= 15; var16++) {
            int var8 = 0;

            for (int var9 = 0; var9 < 16; var9++) {
               int var10 = var15 == 0 ? var9 : var16;
               int var11 = var15 == 0 ? var16 : var9;
               int var12 = var15 == 0 ? var9 : var16 + 1;
               int var13 = var15 == 0 ? var16 + 1 : var9;
               boolean var14 = (a(var3, var10, var11) || a(var3, var12, var13))
                  && Math.abs(var5[(var10 + 1) * 18 + var11 + 1] - var5[(var12 + 1) * 18 + var13 + 1]) >= 3;
               var8 = var14 ? var8 + 1 : 0;
               if (var8 >= 3) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean a(int[] var0, int var1, int var2) {
      return var1 >= 0 && var1 < 16 && var2 >= 0 && var2 < 16 && var0[var1 * 16 + var2] > 0;
   }

   private static void a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, int[] var4, int[] var5, ColumnPlan[] var6, int var7, int var8) {
      if (!Boolean.getBoolean("streamsreflowing.nobanksmooth")) {
         if (!Boolean.getBoolean("streamsreflowing.alwaysbanksmooth") && !a(var0, var2, var3, var5)) {
            if (c(var2, var3)) {
               b(var2, var3, "SEAM-SKIP no wall at a border, a carve boundary or a stream drop -> blend not run");
            }
         } else {
            int var9 = Math.max(1, Integer.getInteger("streamsreflowing.banksmoothstep", 2));
            int var10 = Math.max(0, Integer.getInteger("streamsreflowing.banksmoothmincut", 4));
            byte var11 = 20;
            int[] var12 = new int[400];
            boolean[] var13 = new boolean[400];
            boolean[] var14 = new boolean[400];
            boolean[] var15 = new boolean[400];
            boolean[] var16 = new boolean[400];
            boolean[] var17 = new boolean[400];
            Arrays.fill(var12, var7);

            for (int var18 = -6; var18 < 22; var18++) {
               for (int var19 = -6; var19 < 22; var19++) {
                  boolean var20;
                  if (var18 >= 0 && var18 < 16 && var19 >= 0 && var19 < 16) {
                     ColumnPlan var21 = var6[var18 * 16 + var19];
                     var20 = var21 != null && var21 != ColumnPlan.NONE && var21.inCorridor();
                  } else {
                     var20 = var1.inCorridor(var2 + var18, var3 + var19);
                  }

                  if (var20) {
                     for (int var62 = -6; var62 <= 6; var62++) {
                        for (int var22 = -6; var22 <= 6; var22++) {
                           int var23 = var18 + var62;
                           int var24 = var19 + var22;
                           if (var23 >= -2 && var23 < 18 && var24 >= -2 && var24 < 18) {
                              var15[(var23 + 2) * 20 + var24 + 2] = true;
                           }
                        }
                     }
                  }
               }
            }

            MutableBlockPos var57 = new MutableBlockPos();

            for (int var58 = 0; var58 < 16; var58++) {
               for (int var60 = 0; var60 < 16; var60++) {
                  int var63 = (var58 + 2) * 20 + var60 + 2;
                  var16[var63] = true;
                  ColumnPlan var65 = var6[var58 * 16 + var60];
                  Lake var67 = a(var1, var2 + var58, var3 + var60);
                  if (var65 != null && var65 != ColumnPlan.NONE && var65.inChannel()) {
                     var12[var63] = var65.waterSurfaceY();
                     var13[var63] = true;
                     var14[var63] = true;
                  } else if (var67 != null) {
                     var12[var63] = var67.level();
                     var13[var63] = true;
                     var14[var63] = true;
                  } else {
                     int var71 = var0.getHeight(Types.OCEAN_FLOOR, var2 + var58, var3 + var60);
                     int var25 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var2 + var58, var3 + var60);
                     int var26 = Math.min(var71, var25) - 1;
                     BlockState var27 = var0.getBlockState(var57.set(var2 + var58, var26, var3 + var60));
                     if (var27.is(BlockTags.LOGS) || var27.is(BlockTags.LEAVES)) {
                        int var28 = var26;

                        for (int var29 = Math.max(var7, var26 - 48); var28 > var29; var28--) {
                           BlockState var30 = var0.getBlockState(var57.set(var2 + var58, var28, var3 + var60));
                           if (!var30.is(BlockTags.LOGS) && !var30.is(BlockTags.LEAVES)) {
                              break;
                           }
                        }

                        var26 = var28;
                        var13[var63] = true;
                     }

                     var12[var63] = var26;
                  }
               }
            }

            boolean var59 = false;
            boolean var61 = false;
            boolean var64 = false;
            boolean var66 = false;

            for (int var68 = 0; var68 < 16; var68++) {
               var59 |= var15[40 + var68 + 2];
               var61 |= var15[340 + var68 + 2];
               var64 |= var15[(var68 + 2) * 20 + 2];
               var66 |= var15[(var68 + 2) * 20 + 17];
            }

            for (int var69 = -2; var69 <= 17; var69++) {
               for (int var72 = -2; var72 <= -1; var72++) {
                  if (var59) {
                     a(var0, var1, var2, var3, var72, var69, 20, var12, var14, var16, var17, var7);
                  }

                  if (var64) {
                     a(var0, var1, var2, var3, var69, var72, 20, var12, var14, var16, var17, var7);
                  }
               }

               for (int var73 = 16; var73 <= 17; var73++) {
                  if (var61) {
                     a(var0, var1, var2, var3, var73, var69, 20, var12, var14, var16, var17, var7);
                  }

                  if (var66) {
                     a(var0, var1, var2, var3, var69, var73, 20, var12, var14, var16, var17, var7);
                  }
               }
            }

            int[] var70 = (int[])var12.clone();
            MutableBlockPos var74 = new MutableBlockPos();
            int[] var75 = new int[400];
            int[] var76 = new int[400];

            for (int var77 = 0; var77 < 20; var77++) {
               for (int var81 = 0; var81 < 20; var81++) {
                  int var86 = var77 * 20 + var81;
                  if (var12[var86] == var7) {
                     var75[var86] = var7;
                  } else {
                     int var92 = var12[var86];
                     if (var77 > 0 && var12[var86 - 20] != var7) {
                        var92 = Math.min(var92, var12[var86 - 20]);
                     }

                     if (var77 < 19 && var12[var86 + 20] != var7) {
                        var92 = Math.min(var92, var12[var86 + 20]);
                     }

                     if (var81 > 0 && var12[var86 - 1] != var7) {
                        var92 = Math.min(var92, var12[var86 - 1]);
                     }

                     if (var81 < 19 && var12[var86 + 1] != var7) {
                        var92 = Math.min(var92, var12[var86 + 1]);
                     }

                     var75[var86] = var92;
                  }
               }
            }

            for (int var78 = 0; var78 < 20; var78++) {
               for (int var82 = 0; var82 < 20; var82++) {
                  int var87 = var78 * 20 + var82;
                  if (var12[var87] == var7) {
                     var76[var87] = var7;
                  } else {
                     int var93 = var75[var87];
                     if (var78 > 0 && var75[var87 - 20] != var7) {
                        var93 = Math.max(var93, var75[var87 - 20]);
                     }

                     if (var78 < 19 && var75[var87 + 20] != var7) {
                        var93 = Math.max(var93, var75[var87 + 20]);
                     }

                     if (var82 > 0 && var75[var87 - 1] != var7) {
                        var93 = Math.max(var93, var75[var87 - 1]);
                     }

                     if (var82 < 19 && var75[var87 + 1] != var7) {
                        var93 = Math.max(var93, var75[var87 + 1]);
                     }

                     var76[var87] = var93;
                  }
               }
            }

            int var79 = 0;
            int var83 = 0;

            for (int var88 = 0; var88 < 400; var88++) {
               int var94 = var88 / 20 - 2;
               int var31 = var88 % 20 - 2;
               if ((var94 < 0 || var94 >= 16 || var31 < 0 || var31 >= 16) && var15[var88]) {
                  var79++;
                  if (!var16[var88]) {
                     var83++;
                  }
               }
            }

            if (c(var2 + 8, var3 + 8)) {
               b(var2 + 8, var3 + 8, "SEAM-TRUST apronZoned=" + var79 + " untrusted=" + var83 + " (untrusted apron = a seam this side cannot close)");
            }

            boolean[] var80 = new boolean[400];
            MutableBlockPos var84 = new MutableBlockPos();

            for (int var89 = 0; var89 < var80.length; var89++) {
               if (var15[var89] && var12[var89] != var7) {
                  int var95 = var2 + var89 / 20 - 2;
                  int var98 = var3 + var89 % 20 - 2;
                  BlockState var32 = var0.getBlockState(var84.set(var95, var12[var89], var98));
                  var80[var89] = var32.is(BlockTags.LOGS) || var32.is(BlockTags.LEAVES);
               }
            }

            int[] var85 = (int[])var12.clone();

            for (int var90 = 0; var90 < 32; var90++) {
               boolean var96 = false;

               for (int var99 = -2; var99 < 18; var99++) {
                  for (int var102 = -2; var102 < 18; var102++) {
                     int var33 = (var99 + 2) * 20 + var102 + 2;
                     if (var15[var33] && !var13[var33] && var16[var33] && var12[var33] != var7) {
                        boolean var34 = var99 >= 0 && var99 < 16 && var102 >= 0 && var102 < 16;
                        if (var34 && var5[var99 * 16 + var102] >= var10) {
                           int var35 = var99 + 2;
                           int var36 = var102 + 2;
                           int var37 = var2 + var99;
                           int var38 = var3 + var102;
                           long var39 = Math.floorMod(Mth.getSeed(var37, 0, var38), 6L);
                           int var41 = Math.min(6, var9 + var90);
                           int var42 = Math.max(1, var41 + (var39 == 0L ? -1 : (var39 == 5L ? 1 : 0)));
                           int var43 = var12[var33];
                           int var44 = Integer.MIN_VALUE;
                           if (var35 > 0 && !var80[var33 - 20]) {
                              var44 = Math.max(var44, var76[var33 - 20]);
                           }

                           if (var35 < 19 && !var80[var33 + 20]) {
                              var44 = Math.max(var44, var76[var33 + 20]);
                           }

                           if (var36 > 0 && !var80[var33 - 1]) {
                              var44 = Math.max(var44, var76[var33 - 1]);
                           }

                           if (var36 < 19 && !var80[var33 + 1]) {
                              var44 = Math.max(var44, var76[var33 + 1]);
                           }

                           if (var44 == Integer.MIN_VALUE || var44 - var12[var33] <= 16) {
                              int var45 = var44 - var42;
                              if (var35 > 0 && var14[var33 - 20]) {
                                 var45 = Math.min(var45, var12[var33 - 20] + 1);
                              }

                              if (var35 < 19 && var14[var33 + 20]) {
                                 var45 = Math.min(var45, var12[var33 + 20] + 1);
                              }

                              if (var36 > 0 && var14[var33 - 1]) {
                                 var45 = Math.min(var45, var12[var33 - 1] + 1);
                              }

                              if (var36 < 19 && var14[var33 + 1]) {
                                 var45 = Math.min(var45, var12[var33 + 1] + 1);
                              }

                              var45 = Math.min(var45, var70[var33]);
                              if (var12[var33] < var45 && !a(var0, var74, var37, var38, var12[var33] + 1, var45)) {
                                 int var46 = var12[var33];
                                 int var47 = var70[var33];
                                 BlockState var48 = a(var0, var37, var38, var47, var7);

                                 for (int var49 = var12[var33] + 1; var49 <= var45; var46 = var49++) {
                                    var74.set(var37, var49, var38);
                                    BlockState var50 = var0.getBlockState(var74);
                                    if (!var50.getFluidState().isEmpty()) {
                                       var13[var33] = true;
                                       break;
                                    }

                                    if (a(var0, var74, var37, var49, var38)) {
                                       var13[var33] = true;
                                       break;
                                    }

                                    var74.set(var37, var49, var38);
                                    if (!d(var0, var74, var37, var49, var38, var7)) {
                                       var13[var33] = true;
                                       break;
                                    }

                                    BlockState var51 = var49 == var45
                                       ? a(var0, var37, var38, var45, var7, a(var0, var37, var38, var47, 0, var48, var7))
                                       : a(var0, var37, var38, var47, var45 - var49, var48, var7);
                                    a(var0, var74, var37, var49, var38, var51, var7);
                                    b(var0, var74, var37, var49 - 1, var38, var48, var7);
                                 }

                                 if (var46 > var12[var33]) {
                                    if (c(var37, var38)) {
                                       b(
                                          var37,
                                          var38,
                                          "FILL iter="
                                             + var90
                                             + " from="
                                             + var43
                                             + " to="
                                             + var46
                                             + " target="
                                             + var45
                                             + " pulledBy="
                                             + var44
                                             + " step="
                                             + var42
                                             + " orig="
                                             + var70[var33]
                                       );
                                    }

                                    var12[var33] = var46;
                                    var76[var33] = Math.max(var76[var33], var46);
                                    if (var99 >= 0 && var99 < 16 && var102 >= 0 && var102 < 16) {
                                       var4[var99 * 16 + var102] = Mth.clamp(var46, var7, var8);
                                    }

                                    var96 = true;
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (!var96) {
                  break;
               }
            }

            byte var91 = 3;
            byte var97 = 2;

            for (int var100 = 0; var100 < 2; var100++) {
               boolean var103 = false;

               for (int var105 = -2; var105 < 18; var105++) {
                  for (int var107 = -2; var107 < 18; var107++) {
                     int var110 = (var105 + 2) * 20 + var107 + 2;
                     if (var15[var110] && !var13[var110] && var16[var110] && var12[var110] != var7) {
                        boolean var114 = var105 >= 0 && var105 < 16 && var107 >= 0 && var107 < 16;
                        if (var114) {
                           boolean var117 = var5[var105 * 16 + var107] >= var10;

                           for (int var120 = -3; var120 <= 3 && !var117; var120++) {
                              if (var120 != 0) {
                                 int var126 = var105 + var120;
                                 if (var126 >= -2 && var126 < 18) {
                                    int var40 = (var126 + 2) * 20 + var107 + 2;
                                    var117 = var12[var40] != var7 && var12[var40] > var85[var40];
                                 }

                                 int var133 = var107 + var120;
                                 if (!var117 && var133 >= -2 && var133 < 18) {
                                    int var141 = (var105 + 2) * 20 + var133 + 2;
                                    var117 = var12[var141] != var7 && var12[var141] > var85[var141];
                                 }
                              }
                           }

                           if (var117) {
                              int var121 = Integer.MIN_VALUE;

                              for (int var127 = 0; var127 < 2; var127++) {
                                 int var134 = Integer.MIN_VALUE;
                                 int var142 = Integer.MIN_VALUE;

                                 for (int var149 = 1; var149 <= 3; var149++) {
                                    int var156 = var127 == 0 ? var105 + var149 : var105;
                                    int var164 = var127 == 0 ? var107 : var107 + var149;
                                    if (var156 > 17 || var164 > 17) {
                                       break;
                                    }

                                    int var172 = var12[(var156 + 2) * 20 + var164 + 2];
                                    if (var172 != var7 && var172 >= var12[var110] + 2) {
                                       var134 = var172;
                                       break;
                                    }
                                 }

                                 if (var134 != Integer.MIN_VALUE) {
                                    for (int var150 = 1; var150 <= 3; var150++) {
                                       int var157 = var127 == 0 ? var105 - var150 : var105;
                                       int var165 = var127 == 0 ? var107 : var107 - var150;
                                       if (var157 < -2 || var165 < -2) {
                                          break;
                                       }

                                       int var173 = var12[(var157 + 2) * 20 + var165 + 2];
                                       if (var173 != var7 && var173 >= var12[var110] + 2) {
                                          var142 = var173;
                                          break;
                                       }
                                    }

                                    if (var142 != Integer.MIN_VALUE) {
                                       var121 = Math.max(var121, Math.min(var134, var142));
                                    }
                                 }
                              }

                              if (var121 != Integer.MIN_VALUE) {
                                 int var128 = var105 + 2;
                                 int var135 = var107 + 2;
                                 if (var128 > 0 && var14[var110 - 20]) {
                                    var121 = Math.min(var121, var12[var110 - 20] + 1);
                                 }

                                 if (var128 < 19 && var14[var110 + 20]) {
                                    var121 = Math.min(var121, var12[var110 + 20] + 1);
                                 }

                                 if (var135 > 0 && var14[var110 - 1]) {
                                    var121 = Math.min(var121, var12[var110 - 1] + 1);
                                 }

                                 if (var135 < 19 && var14[var110 + 1]) {
                                    var121 = Math.min(var121, var12[var110 + 1] + 1);
                                 }

                                 var121 = Math.min(var121, var70[var110]);
                                 if (var12[var110] < var121) {
                                    int var143 = var2 + var105;
                                    int var151 = var3 + var107;
                                    if (a(var0, var74, var143, var151, var12[var110] + 1, var121)) {
                                       var13[var110] = true;
                                    } else {
                                       int var158 = var12[var110];
                                       int var166 = var70[var110];
                                       BlockState var174 = a(var0, var143, var151, var166, var7);

                                       for (int var179 = var12[var110] + 1; var179 <= var121; var158 = var179++) {
                                          var74.set(var143, var179, var151);
                                          BlockState var182 = var0.getBlockState(var74);
                                          if (!var182.getFluidState().isEmpty()) {
                                             var13[var110] = true;
                                             break;
                                          }

                                          if (a(var0, var74, var143, var179, var151)) {
                                             var13[var110] = true;
                                             break;
                                          }

                                          var74.set(var143, var179, var151);
                                          if (!d(var0, var74, var143, var179, var151, var7)) {
                                             var13[var110] = true;
                                             break;
                                          }

                                          BlockState var185 = var179 == var121
                                             ? a(var0, var143, var151, var121, var7, a(var0, var143, var151, var166, 0, var174, var7))
                                             : a(var0, var143, var151, var166, var121 - var179, var174, var7);
                                          a(var0, var74, var143, var179, var151, var185, var7);
                                          b(var0, var74, var143, var179 - 1, var151, var174, var7);
                                       }

                                       if (var158 > var12[var110]) {
                                          var12[var110] = var158;
                                          if (var114) {
                                             var4[var105 * 16 + var107] = Mth.clamp(var158, var7, var8);
                                          }

                                          var103 = true;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (!var103) {
                  break;
               }
            }

            byte var101 = 4;
            byte var104 = 2;
            int[] var106 = (int[])var12.clone();

            for (int var108 = 0; var108 < 16; var108++) {
               for (int var111 = 0; var111 < 16; var111++) {
                  int var115 = (var108 + 2) * 20 + var111 + 2;
                  if (var15[var115] && !var13[var115] && var16[var115] && var106[var115] != var7 && !var14[var115]) {
                     boolean var118 = var5[var108 * 16 + var111] >= var10;

                     for (int var123 = -3; var123 <= 3 && !var118; var123++) {
                        if (var123 != 0) {
                           int var129 = var108 + var123;
                           if (var129 >= 0 && var129 < 16) {
                              var118 = var5[var129 * 16 + var111] >= var10;
                              if (!var118) {
                                 int var136 = (var129 + 2) * 20 + var111 + 2;
                                 var118 = var106[var136] != var7 && var106[var136] > var85[var136];
                              }
                           }

                           int var137 = var111 + var123;
                           if (!var118 && var137 >= 0 && var137 < 16) {
                              var118 = var5[var108 * 16 + var137] >= var10;
                              if (!var118) {
                                 int var144 = (var108 + 2) * 20 + var137 + 2;
                                 var118 = var106[var144] != var7 && var106[var144] > var85[var144];
                              }
                           }
                        }
                     }

                     if (var118) {
                        int var124 = Integer.MIN_VALUE;

                        for (int var130 = 0; var130 < 2; var130++) {
                           int var138 = Integer.MIN_VALUE;
                           int var145 = Integer.MIN_VALUE;

                           for (int var152 = 1; var152 <= 2; var152++) {
                              int var159 = var130 == 0 ? var108 + var152 : var108;
                              int var167 = var130 == 0 ? var111 : var111 + var152;
                              if (var159 > 15 || var167 > 15) {
                                 break;
                              }

                              int var175 = var106[(var159 + 2) * 20 + var167 + 2];
                              if (var175 != var7 && var175 <= var106[var115] - 4) {
                                 var138 = var175;
                                 break;
                              }
                           }

                           if (var138 != Integer.MIN_VALUE) {
                              for (int var153 = 1; var153 <= 2; var153++) {
                                 int var160 = var130 == 0 ? var108 - var153 : var108;
                                 int var168 = var130 == 0 ? var111 : var111 - var153;
                                 if (var160 < 0 || var168 < 0) {
                                    break;
                                 }

                                 int var176 = var106[(var160 + 2) * 20 + var168 + 2];
                                 if (var176 != var7 && var176 <= var106[var115] - 4) {
                                    var145 = var176;
                                    break;
                                 }
                              }

                              if (var145 != Integer.MIN_VALUE) {
                                 var124 = Math.max(var124, Math.max(var138, var145) + 1);
                              }
                           }
                        }

                        if (var124 != Integer.MIN_VALUE && var124 < var106[var115]) {
                           int var131 = var2 + var108;
                           int var139 = var3 + var111;
                           if (a(var0, var74, var131, var139, var124 + 1, var106[var115] + 2)) {
                              var13[var115] = true;
                           } else {
                              boolean var146 = false;

                              for (int var154 = var106[var115]; var154 > var124; var154--) {
                                 BlockState var161 = var0.getBlockState(var74.set(var131, var154, var139));
                                 if (!var161.getFluidState().isEmpty()) {
                                    break;
                                 }

                                 if (!var161.isAir()) {
                                    var0.setBlock(var74, Blocks.AIR.defaultBlockState(), 2);
                                    var146 = true;
                                 }
                              }

                              if (var146) {
                                 if (c(var131, var139)) {
                                    b(var131, var139, "PILLAR-CULL was=" + var106[var115] + " now=" + var124);
                                 }

                                 c(var0, var74, var131, var139, var124 + 1, var8);
                                 var12[var115] = var124;
                                 var4[var108 * 16 + var111] = Mth.clamp(var124, var7, var8);
                              }
                           }
                        }
                     }
                  }
               }
            }

            boolean[] var109 = new boolean[400];

            for (int var112 = 0; var112 < var109.length; var112++) {
               var109[var112] = var15[var112] && var16[var112] && !var13[var112] && var12[var112] != var7 && var12[var112] > var70[var112];
            }

            BlockState var113 = Blocks.AIR.defaultBlockState();
            boolean[] var116 = new boolean[400];
            int[] var119 = new int[400];
            int[] var125 = new int[400];

            for (int var132 = 0; var132 < var109.length; var132++) {
               if (var109[var132] && !var116[var132]) {
                  int var140 = 0;
                  int var147 = 0;
                  int var155 = 0;
                  var119[var147++] = var132;
                  var116[var132] = true;

                  while (var140 < var147) {
                     int var162 = var119[var140++];
                     var125[var155++] = var162;
                     int var169 = var162 / 20;
                     int var177 = var162 % 20;
                     if (var169 > 0 && var109[var162 - 20] && !var116[var162 - 20]) {
                        var116[var162 - 20] = true;
                        var119[var147++] = var162 - 20;
                     }

                     if (var169 < 19 && var109[var162 + 20] && !var116[var162 + 20]) {
                        var116[var162 + 20] = true;
                        var119[var147++] = var162 + 20;
                     }

                     if (var177 > 0 && var109[var162 - 1] && !var116[var162 - 1]) {
                        var116[var162 - 1] = true;
                        var119[var147++] = var162 - 1;
                     }

                     if (var177 < 19 && var109[var162 + 1] && !var116[var162 + 1]) {
                        var116[var162 + 1] = true;
                        var119[var147++] = var162 + 1;
                     }
                  }

                  int var163 = 0;
                  int var170 = 0;
                  int var178 = 0;

                  for (int var180 = 0; var180 < var155; var180++) {
                     int var183 = var125[var180];
                     var163 = Math.max(var163, var12[var183] - var70[var183]);
                     int var186 = var183 / 20;
                     int var188 = var183 % 20;

                     for (int var190 = 0; var190 < 4; var190++) {
                        int var192 = switch (var190) {
                           case 0 -> var186 > 0 ? var183 - 20 : -1;
                           case 1 -> var186 < 19 ? var183 + 20 : -1;
                           case 2 -> var188 > 0 ? var183 - 1 : -1;
                           default -> var188 < 19 ? var183 + 1 : -1;
                        };
                        if (var192 >= 0 && !var109[var192] && var12[var192] != var7) {
                           var170++;
                           if (var12[var192] >= var12[var183] - (var9 + 1)) {
                              var178++;
                           }
                        }
                     }
                  }

                  boolean var181 = var155 <= 24 && var163 > var9 && (var170 == 0 || var178 * 4 < var170);
                  if (var181) {
                     for (int var184 = 0; var184 < var155; var184++) {
                        int var187 = var125[var184];
                        int var189 = var187 / 20 - 2;
                        int var191 = var187 % 20 - 2;
                        boolean var193 = var189 >= 0 && var189 < 16 && var191 >= 0 && var191 < 16;
                        if (var193) {
                           int var52 = var2 + var189;
                           int var53 = var3 + var191;
                           int var54 = var12[var187];

                           for (int var55 = var54; var55 > var70[var187]; var55--) {
                              var74.set(var52, var55, var53);
                              BlockState var56 = var0.getBlockState(var74);
                              if (!var56.getFluidState().isEmpty()) {
                                 break;
                              }

                              if (!var56.isAir()) {
                                 var0.setBlock(var74, var113, 2);
                              }
                           }

                           var12[var187] = var70[var187];
                           if (var193) {
                              var4[var189 * 16 + var191] = Mth.clamp(var70[var187], var7, var8);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static Lake a(RiverEngine var0, int var1, int var2) {
      Lake var3 = var0.lakeNear(var1, var2, 2);
      if (var3 != null && var3.digDepth() == 0 && var3.coverageSmooth(var1, var2) > 0.5) {
         return var3;
      } else {
         Lake var4 = var0.lakeAt(var1, var2);
         return var4 != null && var4.digDepth() > 0 ? var4 : null;
      }
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int[] var7,
      boolean[] var8,
      boolean[] var9,
      boolean[] var10,
      int var11
   ) {
      int var12 = (var4 + 2) * var6 + var5 + 2;
      if (var7[var12] == var11) {
         int var13 = var2 + var4;
         int var14 = var3 + var5;

         try {
            ColumnPlan var15 = var1.plan(var13, var14);
            if (var15 != null && var15 != ColumnPlan.NONE && var15.inChannel()) {
               var8[var12] = true;
               var7[var12] = var15.waterSurfaceY();
               return;
            }

            Lake var16 = a(var1, var13, var14);
            if (var16 != null) {
               var8[var12] = true;
               var7[var12] = var16.level();
               return;
            }

            if (!b(var0, var13, var14)) {
               if (var15 != null && var15 != ColumnPlan.NONE && var15.inCorridor()) {
                  int var25 = a(var1, var0, var15, var13, var14, var11);
                  if (var25 != Integer.MIN_VALUE) {
                     var7[var12] = var25;
                     var9[var12] = true;
                  }

                  return;
               }

               int var24 = var0.getHeight(Types.OCEAN_FLOOR, var13, var14);
               int var26 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var13, var14);
               var7[var12] = Math.min(var24, var26) - 1;
               var9[var12] = true;
               return;
            }

            var9[var12] = true;
            var10[var12] = c(var0, var13, var14);
            int var17 = var0.getHeight(Types.OCEAN_FLOOR, var13, var14);
            int var18 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var13, var14);
            int var19 = Math.min(var17, var18) - 1;
            MutableBlockPos var20 = new MutableBlockPos();

            for (int var21 = 0; var21 < 48 && var19 > var11; var21++) {
               var20.set(var13, var19, var14);
               BlockState var22 = var0.getBlockState(var20);
               if (!var22.is(BlockTags.LOGS) && !var22.is(BlockTags.LEAVES) && !var22.isAir()) {
                  break;
               }

               var19--;
            }

            var7[var12] = var19;
         } catch (RuntimeException var23) {
         }
      }
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      for (int var6 = var4; var6 <= var5; var6++) {
         BlockState var7 = var0.getBlockState(var1.set(var2, var6, var3));
         if (var7.is(BlockTags.LOGS) || var7.is(BlockTags.LEAVES)) {
            return true;
         }
      }

      return false;
   }

   private static int a(WorldGenLevel var0, int var1, int var2) {
      int var3 = var0.getHeight(Types.OCEAN_FLOOR, var1, var2);
      int var4 = var0.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var1, var2);
      return Math.min(var3, var4) - 1;
   }

   private static boolean b(WorldGenLevel var0, int var1, int var2) {
      ChunkAccess var3 = var0.getChunk(var1 >> 4, var2 >> 4, ChunkStatus.EMPTY, false);
      return var3 == null ? false : var3.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES);
   }

   private static boolean c(WorldGenLevel var0, int var1, int var2) {
      ChunkAccess var3 = var0.getChunk(var1 >> 4, var2 >> 4, ChunkStatus.EMPTY, false);
      if (var3 == null) {
         return false;
      } else {
         ChunkStatus var4 = var3.getPersistedStatus();
         return var4.isOrAfter(ChunkStatus.FEATURES) && !var4.isOrAfter(ChunkStatus.LIGHT);
      }
   }

   private static List<BankStyle> a(WorldGenLevel var0) {
      RegistryAccess var1 = var0.registryAccess();
      if (ad != var1) {
         ArrayList var2 = new ArrayList();
         var0.registryAccess().registry(BankStyle.REGISTRY_KEY).ifPresent(var1x -> var1x.forEach(var2::add));
         ae = List.copyOf(var2);
         ad = var1;
      }

      return ae;
   }

   private static void a(
      WorldGenLevel var0,
      RiverEngine var1,
      int var2,
      int var3,
      int[] var4,
      BlockState[] var5,
      BlockState[] var6,
      ColumnPlan[] var7,
      int[] var8,
      int var9,
      int var10
   ) {
      List var11 = BankStyleLoader.configStyles();
      List var12 = a(var0);
      if (!var11.isEmpty() || !var12.isEmpty()) {
         MutableBlockPos var13 = new MutableBlockPos();
         HashMap var14 = new HashMap();
         HashMap var15 = new HashMap();

         for (int var16 = 0; var16 < 16; var16++) {
            for (int var17 = 0; var17 < 16; var17++) {
               int var18 = var2 + var16;
               int var19 = var3 + var17;
               int var20 = var16 * 16 + var17;
               int var21 = var4[var20];
               ColumnPlan var22 = var7[var20];
               if (var22 == null) {
                  var22 = var1.plan(var18, var19);
               }

               boolean var24 = false;
               int var23;
               if (var22 != ColumnPlan.NONE && var22.inCorridor()) {
                  var23 = var22.waterSurfaceY();
               } else {
                  int var25 = var8[var20];
                  if (var25 == -2147483647) {
                     var25 = var1.lakeSurfaceAt(var18, var19);
                  }

                  if (var25 == Integer.MIN_VALUE) {
                     continue;
                  }

                  var23 = var25;
                  var24 = true;
               }

               a(var0, var13, var18, var19, var21, var5[var20], var6[var20], var23, !var24, var11, var12, var14, var15, var9, var10, "bank");
            }
         }
      }
   }

   private static void a(
      WorldGenLevel var0,
      MutableBlockPos var1,
      int var2,
      int var3,
      int var4,
      BlockState var5,
      BlockState var6,
      int var7,
      boolean var8,
      List<BankStyle> var9,
      List<BankStyle> var10,
      Map<Holder<Biome>, BankStyle> var11,
      Map<Long, Holder<Biome>> var12,
      int var13,
      int var14,
      String var15
   ) {
      int var16 = Integer.MIN_VALUE;
      int var17 = Math.max(var13 + 1, var7 - 24);

      for (int var18 = Math.min(var14, var4); var18 >= var17; var18--) {
         var1.set(var2, var18, var3);
         BlockState var19 = var0.getBlockState(var1);
         if (!var19.isAir() && var19.getFluidState().isEmpty() && var19.canOcclude()) {
            var16 = var18;
            break;
         }
      }

      if (var16 != Integer.MIN_VALUE && (!var8 || var16 < var4)) {
         var1.set(var2, var16, var3);
         BlockState var53 = var0.getBlockState(var1);
         if (var53.is(Blocks.BEDROCK)) {
            if (c(var2, var3)) {
               b(var2, var3, "SKIN-SKIP pass=" + var15 + " why=bedrock surfaceY=" + var16 + " scanTop=" + var4);
            }
         } else {
            int var54 = var2 + (int)(c(var2, 0, var3) * 5.0) - 2;
            int var20 = var3 + (int)(c(var3, 1, var2) * 5.0) - 2;
            int var21 = var16 >> 2;
            long var22 = (long)(var54 >> 2) << 32 ^ (long)(var20 >> 2) & 4294967295L;
            Holder var24 = var12.computeIfAbsent(var22, var4x -> var0.getUncachedNoiseBiome(var54 >> 2, var21, var20 >> 2));
            BankStyle var25 = var11.computeIfAbsent(var24, var2x -> a(var9, var10, (Holder<Biome>)var2x));
            if (var25 != null && var25.enabled()) {
               int var26 = var7 - var25.waterlineBelow();
               int var27 = var7 + var25.waterlineAbove();
               double var28 = (double)var25.underwaterNoise() * 3.0;
               double var30 = (double)var25.aboveWaterNoise() * 3.0;
               double var32 = (double)(var16 - var26);
               double var34 = (double)(var16 - var27);
               double var36 = c(var2, var16, var3);
               int var38;
               if (var30 > 0.0 && Math.abs(var34) <= Math.abs(var32) && Math.abs(var34) < var30) {
                  var38 = var36 < 0.5 + 0.5 * var34 / var30 ? 2 : 1;
               } else if (var28 > 0.0 && Math.abs(var32) < var28) {
                  var38 = var36 < 0.5 + 0.5 * var32 / var28 ? 1 : 0;
               } else {
                  var38 = var16 > var27 ? 2 : (var16 < var26 ? 0 : 1);
               }

               if (var25.zoneEnabled(var38)) {
                  BlockState var39 = var25.zoneBlock(var38, c(var3, var16, var2));
                  int var40 = var0.getHeight(Types.OCEAN_FLOOR, var2 + 1, var3) - 1;
                  int var41 = var0.getHeight(Types.OCEAN_FLOOR, var2 - 1, var3) - 1;
                  int var42 = var0.getHeight(Types.OCEAN_FLOOR, var2, var3 + 1) - 1;
                  int var43 = var0.getHeight(Types.OCEAN_FLOOR, var2, var3 - 1) - 1;
                  int var44 = Math.max(Math.max(var16 - var40, var16 - var41), Math.max(var16 - var42, var16 - var43));
                  double var45 = Double.MAX_VALUE;

                  for (int var50 : new int[]{var40, var41, var42, var43}) {
                     var45 = Math.min(var45, (double)Math.abs(var16 - var50));
                  }

                  BlockState var55 = null;
                  boolean var56 = var16 >= var4 - 1;
                  if (BankStyle.isChameleon(var39)) {
                     var39 = var5;
                     if (var6 != null && var38 == 2 && (var56 || var45 <= 1.0)) {
                        var55 = var5;
                        var39 = var6;
                     }
                  }

                  if (c(var2, var3)) {
                     b(
                        var2,
                        var3,
                        "SKIN pass="
                           + var15
                           + " surfaceY="
                           + var16
                           + " ws="
                           + var7
                           + " zone="
                           + var38
                           + " exposure="
                           + var44
                           + " slope="
                           + String.format(Locale.ROOT, "%.2f", var45)
                           + " cur="
                           + var53.getBlock()
                           + " skin="
                           + (var39 == null ? "none" : var39.getBlock())
                           + " face="
                           + (var55 == null ? "same" : var55.getBlock().toString())
                           + " turf="
                           + (var55 != null)
                           + " grazed="
                           + var56
                           + " scanTop="
                           + var4
                           + " nb="
                           + var40
                           + "/"
                           + var41
                           + "/"
                           + var42
                           + "/"
                           + var43
                           + " biome="
                           + var24.unwrapKey().map(var0x -> var0x.location().toString()).orElse("?")
                           + " style[bed="
                           + var25.bed()
                           + " wl="
                           + var25.waterline()
                           + " bank="
                           + var25.bank()
                           + " bedOn="
                           + var25.bedEnabled()
                           + " wlOn="
                           + var25.waterlineEnabled()
                           + " bankOn="
                           + var25.bankEnabled()
                           + "]"
                     );
                  }

                  if (var39 != null && !var39.isAir() && !var39.equals(var53)) {
                     a(var0, var1, var2, var16, var3, var39, var13);
                     int var57 = Mth.clamp(var44, 2, 8);
                     BlockState var58 = var55 != null ? var55 : var39;
                     if (a(var58)) {
                        var58 = Blocks.DIRT.defaultBlockState();
                     }

                     for (int var51 = 1; var51 <= var57; var51++) {
                        var1.set(var2, var16 - var51, var3);
                        BlockState var52 = var0.getBlockState(var1);
                        if (var52.isAir() || !var52.getFluidState().isEmpty()) {
                           break;
                        }

                        if (!var52.equals(var58)) {
                           var0.setBlock(var1, var58, 2);
                        }
                     }

                     var1.set(var2, var16 + 1, var3);
                     BlockState var59 = var0.getBlockState(var1);
                     if (!var59.isAir() && var59.getFluidState().isEmpty() && !var59.canSurvive(var0, var1)) {
                        var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
                     }
                  }
               }
            } else {
               if (c(var2, var3)) {
                  b(
                     var2,
                     var3,
                     "SKIN-SKIP pass="
                        + var15
                        + " why="
                        + (var25 == null ? "nostyle" : "styleoff")
                        + " surfaceY="
                        + var16
                        + " scanTop="
                        + var4
                        + " biome="
                        + var24.unwrapKey().map(var0x -> var0x.location().toString()).orElse("?")
                  );
               }
            }
         }
      } else {
         if (c(var2, var3)) {
            b(
               var2,
               var3,
               "SKIN-SKIP pass="
                  + var15
                  + " why="
                  + (var16 == Integer.MIN_VALUE ? "nosurface" : "notlowered")
                  + " surfaceY="
                  + var16
                  + " scanTop="
                  + var4
                  + " ws="
                  + var7
            );
         }
      }
   }

   public static void skinAllWater(WorldGenLevel var0, int var1, int var2, int var3) {
      if (var3 > 0 && Services.PLATFORM.customBanks()) {
         List var4 = BankStyleLoader.configStyles();
         List var5 = a(var0);
         if (!var4.isEmpty() || !var5.isEmpty()) {
            int var6 = var0.getMinBuildHeight();
            int var7 = var0.getMaxBuildHeight() - 1;
            byte var8 = 4;
            int var9 = 16 + 2 * var8;
            int[] var10 = new int[var9 * var9];
            Arrays.fill(var10, Integer.MIN_VALUE);
            MutableBlockPos var11 = new MutableBlockPos();
            RiverEngine var12 = StreamsWorldgen.engine(var0);

            for (int var13 = -var8; var13 < 16 + var8; var13++) {
               for (int var14 = -var8; var14 < 16 + var8; var14++) {
                  int var15 = var1 + var13;
                  int var16 = var2 + var14;
                  int var17 = Integer.MIN_VALUE;
                  if (var12 != null) {
                     Lake var18 = a(var12, var15, var16);
                     if (var18 != null) {
                        var17 = Mth.clamp(var18.level(), var6, var7);
                     } else {
                        ColumnPlan var19 = var12.planLean(var15, var16);
                        if (var19 != ColumnPlan.NONE && var19.inChannel()) {
                           var17 = Mth.clamp(var19.waterSurfaceY(), var6, var7);
                        }
                     }
                  }

                  if (var17 != Integer.MIN_VALUE) {
                     var10[(var13 + var8) * var9 + var14 + var8] = var17;
                  } else {
                     int var28 = var0.getHeight(Types.WORLD_SURFACE_WG, var15, var16) - 1;
                     var11.set(var15, var28, var16);
                     if (var0.getBlockState(var11).getFluidState().is(FluidTags.WATER)
                        && (var3 >= 2 || !var0.getUncachedNoiseBiome(var15 >> 2, var28 >> 2, var16 >> 2).is(BiomeTags.IS_OCEAN))) {
                        var10[(var13 + var8) * var9 + var14 + var8] = var28;
                     }
                  }
               }
            }

            HashMap var23 = new HashMap();
            HashMap var24 = new HashMap();

            for (int var25 = 0; var25 < 16; var25++) {
               for (int var26 = 0; var26 < 16; var26++) {
                  int var27 = Integer.MIN_VALUE;
                  int var29 = Integer.MAX_VALUE;

                  for (int var30 = -var8; var30 <= var8; var30++) {
                     for (int var20 = -var8; var20 <= var8; var20++) {
                        int var21 = var10[(var25 + var30 + var8) * var9 + var26 + var20 + var8];
                        if (var21 != Integer.MIN_VALUE) {
                           int var22 = var30 * var30 + var20 * var20;
                           if (var22 <= var8 * var8 && var22 < var29) {
                              var29 = var22;
                              var27 = var21;
                           }
                        }
                     }
                  }

                  if (var27 != Integer.MIN_VALUE) {
                     int var31 = var1 + var25;
                     int var32 = var2 + var26;
                     int var33 = var0.getHeight(Types.OCEAN_FLOOR_WG, var31, var32) - 1;
                     a(
                        var0,
                        var11,
                        var31,
                        var32,
                        var33,
                        a(var31, var32, var0, var33, var6),
                        a(var31, var32, var0, var33),
                        var27,
                        false,
                        var4,
                        var5,
                        var23,
                        var24,
                        var6,
                        var7,
                        "water"
                     );
                  }
               }
            }
         }
      }
   }

   private static BankStyle a(List<BankStyle> var0, List<BankStyle> var1, Holder<Biome> var2) {
      ResourceKey var3 = (ResourceKey)var2.unwrapKey().orElse(null);
      BankStyle var4 = a(a(var0, var2, var3), var3);
      if (var4 != null) {
         return var4;
      } else {
         var4 = a(a(var1, var2, var3), var3);
         if (var4 != null) {
            return var4;
         } else {
            int var5 = Math.max(a(var0, var2), a(var1, var2));
            if (var5 > 0) {
               var4 = a(a(var0, var2, var5), var3);
               if (var4 != null) {
                  return var4;
               }

               var4 = a(a(var1, var2, var5), var3);
               if (var4 != null) {
                  return var4;
               }
            }

            var4 = a(b(var0, var2), var3);
            return var4 != null ? var4 : a(b(var1, var2), var3);
         }
      }
   }

   private static List<BankStyle> a(List<BankStyle> var0, Holder<Biome> var1, ResourceKey<Biome> var2) {
      ArrayList var3 = new ArrayList();

      for (BankStyle var5 : var0) {
         if (var5.matchesExact(var2) && !var5.excluded(var1)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static int a(List<BankStyle> var0, Holder<Biome> var1) {
      int var2 = 0;

      for (BankStyle var4 : var0) {
         if (var4.matchesTags(var1) && !var4.excluded(var1)) {
            var2 = Math.max(var2, var4.tags().size());
         }
      }

      return var2;
   }

   private static List<BankStyle> a(List<BankStyle> var0, Holder<Biome> var1, int var2) {
      ArrayList var3 = new ArrayList();

      for (BankStyle var5 : var0) {
         if (var5.matchesTags(var1) && var5.tags().size() == var2 && !var5.excluded(var1)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static List<BankStyle> b(List<BankStyle> var0, Holder<Biome> var1) {
      ArrayList var2 = new ArrayList();

      for (BankStyle var4 : var0) {
         if (var4.biomes().isEmpty() && var4.tags().isEmpty() && !var4.excluded(var1)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static BankStyle a(List<BankStyle> var0, ResourceKey<Biome> var1) {
      if (var0.isEmpty()) {
         return null;
      } else {
         return var0.size() != 1 && var1 != null ? (BankStyle)var0.get(Math.floorMod(var1.location().hashCode(), var0.size())) : (BankStyle)var0.get(0);
      }
   }

   private static void b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      int var6 = 0;
      int var7 = var4 - 1;

      for (int var8 = var4; var8 <= var5 && var6 <= 2; var8++) {
         var1.set(var2, var8, var3);
         BlockState var9 = var0.getBlockState(var1);
         if (var9.is(BlockTags.LOGS)) {
            var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
            var6 = 0;
            var7 = var8;
         } else if (var9.is(BlockTags.LEAVES)) {
            var6 = 0;
            var7 = var8;
         } else {
            if (!var9.isAir()) {
               break;
            }

            var6++;
         }
      }

      a(var0, var2, var3, var4 - 1, var7, var5);
   }

   private static void a(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5) {
      if (var4 >= var3) {
         MutableBlockPos var6 = new MutableBlockPos();
         ArrayDeque var7 = new ArrayDeque();
         HashSet var8 = new HashSet();
         ArrayList var9 = new ArrayList();

         for (int var10 = Math.max(var3, var4 - 10); var10 <= Math.min(var4 + 1, var5); var10++) {
            for (int[] var14 : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
               int var15 = var1 + var14[0];
               int var16 = var2 + var14[1];
               if (var0.getBlockState(var6.set(var15, var10, var16)).is(BlockTags.LEAVES)
                  && var8.add((long)var15 << 40 ^ (long)var10 << 20 ^ (long)var16 & 1048575L)) {
                  var7.add(new long[]{(long)var15, (long)var10, (long)var16});
               }
            }
         }

         while (!var7.isEmpty()) {
            long[] var22 = (long[])var7.poll();
            int var24 = (int)var22[0];
            int var26 = (int)var22[1];
            int var28 = (int)var22[2];
            var9.add(var22);
            if (var9.size() > 2048) {
               return;
            }

            for (int[] var17 : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
               int var18 = var24 + var17[0];
               int var19 = var26 + var17[1];
               int var20 = var28 + var17[2];
               if (Math.abs(var18 - var1) <= 10 && Math.abs(var20 - var2) <= 10 && var19 >= var3 - 10 && var19 <= var5) {
                  BlockState var21 = var0.getBlockState(var6.set(var18, var19, var20));
                  if (var21.is(BlockTags.LOGS)) {
                     return;
                  }

                  if (var21.is(BlockTags.LEAVES) && var8.add((long)var18 << 40 ^ (long)var19 << 20 ^ (long)var20 & 1048575L)) {
                     var7.add(new long[]{(long)var18, (long)var19, (long)var20});
                  }
               }
            }
         }

         BlockState var23 = Blocks.AIR.defaultBlockState();

         for (long[] var27 : var9) {
            var0.setBlock(var6.set((int)var27[0], (int)var27[1], (int)var27[2]), var23, 2);
         }
      }
   }

   private static boolean c(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      boolean var6 = false;
      int var7 = Math.min(var4 + 5, var5);

      for (int var8 = var4; var8 <= var7; var8++) {
         var1.set(var2, var8, var3);
         BlockState var9 = var0.getBlockState(var1);
         if (!var9.isAir()) {
            if (!var9.getFluidState().isEmpty()) {
               break;
            }

            var1.set(var2, var8 - 1, var3);
            boolean var10 = var0.getBlockState(var1).isAir();
            var1.set(var2, var8, var3);
            if (!var10) {
               break;
            }

            boolean var11 = !var9.canSurvive(var0, var1);
            boolean var12 = var9.getBlock() instanceof SlabBlock;
            boolean var13 = var9.is(Blocks.SNOW) || var9.is(Blocks.SNOW_BLOCK) || var9.is(Blocks.POWDER_SNOW);
            if (!var11 && !var12 && !var13) {
               break;
            }

            var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
            var6 = true;
         }
      }

      return var6;
   }

   private static void a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, int[] var4, ColumnPlan[] var5, int var6) {
      MutableBlockPos var7 = new MutableBlockPos();

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var2 + var8;
            int var11 = var3 + var9;
            ColumnPlan var12 = var5[var8 * 16 + var9];
            if (var12 == null) {
               var12 = var1.plan(var10, var11);
            }

            if (var12 != ColumnPlan.NONE && var12.inCorridor()) {
               int var13 = var4[var8 * 16 + var9];
               var7.set(var10, var13, var11);
               if (var0.getBlockState(var7).isAir()) {
                  for (int var14 = var13 + 1; var14 <= var6; var14++) {
                     var7.set(var10, var14, var11);
                     BlockState var15 = var0.getBlockState(var7);
                     if (var15.isAir() || !var15.getFluidState().isEmpty()) {
                        break;
                     }

                     boolean var16 = var15.is(Blocks.SNOW) || var15.is(Blocks.SNOW_BLOCK) || var15.is(Blocks.POWDER_SNOW);
                     if (var16 || var15.canOcclude() && !var15.is(Blocks.BEDROCK)) {
                        var0.setBlock(var7, Blocks.AIR.defaultBlockState(), 2);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      boolean var8 = false;

      for (int[] var12 : ah) {
         int var13 = var2 + var12[0];
         int var14 = var3 + var12[1];
         if (var13 >= var4 && var13 <= var4 + 15 && var14 >= var5 && var14 <= var5 + 15) {
            for (int var15 = var7; var15 >= var6; var15--) {
               var1.set(var13, var15, var14);
               BlockState var16 = var0.getBlockState(var1);
               if (!var16.isAir() && var16.getFluidState().isEmpty() && !var16.canSurvive(var0, var1)) {
                  var0.setBlock(var1, Blocks.AIR.defaultBlockState(), 2);
                  var8 = true;
               }
            }
         }
      }

      return var8;
   }

   private static void a(WorldGenLevel var0, BlockPos var1, BlockState var2) {
      BlockState var3 = var0.getBlockState(var1);
      if (var3.hasProperty(BlockStateProperties.WATERLOGGED)) {
         if (!var3.getValue(BlockStateProperties.WATERLOGGED)) {
            var0.setBlock(var1, var3.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)), 2);
         }
      } else if (var3.getFluidState().isEmpty()) {
         if (!var3.isCollisionShapeFullBlock(var0, var1)) {
            var0.setBlock(var1, var2, 2);
         }
      }
   }

   private static int a(ColumnPlan var0, int var1, int var2) {
      if (var2 <= var1) {
         return var1;
      } else {
         double var3 = var0.distToOutlet();
         double var5 = Math.max(24.0, 3.0 * var0.width());
         double var7 = Math.min(1.0, var3 / var5);
         double var9 = var7 * var7 * (3.0 - 2.0 * var7);
         return (int)Math.round((double)var2 + (double)(var1 - var2) * var9);
      }
   }

   private static boolean a(
      WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, ColumnPlan var4, RiverEngine var5, int var6, int var7, int var8, BlockState var9
   ) {
      double var11 = var4.width() > 1.0E-6 ? Math.min(1.0, var4.distance() / var4.width()) : 1.0;
      int var13 = Math.max(1, (int)Math.round(2.0 * var4.width() * var5.config().streamDepth()));
      int var14 = (int)Math.round((double)var13 * (1.0 - var11 * var11));
      int var15 = var7 - var14;
      int var16 = a(var4, var15, var6);
      if (var16 >= var6) {
         return false;
      } else {
         for (int var17 = var6; var17 > var16 && var17 > var8; var17--) {
            var1.set(var2, var17, var3);
            var0.setBlock(var1, var9, 2);
         }

         return true;
      }
   }

   private static int a(RiverEngine var0, WorldGenLevel var1, ColumnPlan var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      if (var6 != Integer.MIN_VALUE && !(var2.distToOutlet() > 24.0)) {
         int var10 = a(var0, var1, var3, var4, var2, var7, var8, var9);
         int var11 = Math.min(Math.min(var6, var10), var5);
         var11 = Math.min(var11, var2.waterSurfaceY() + 4);
         return var11 > var2.waterSurfaceY() ? var11 : Integer.MIN_VALUE;
      } else {
         return Integer.MIN_VALUE;
      }
   }

   public static int nearbyLakeLevel(RiverEngine var0, int var1, int var2, int var3, int var4) {
      int var5 = Math.max(1, var0.config().gridSpacing());
      int var6 = Integer.MIN_VALUE;

      for (int var7 = -2 * var5; var7 <= 2 * var5; var7 += var5) {
         for (int var8 = -2 * var5; var8 <= 2 * var5; var8 += var5) {
            Lake var9 = var0.lakeAt(var1 + var8, var2 + var7);
            if (var9 != null && var9.digDepth() <= 0) {
               var6 = Math.max(var6, Mth.clamp(var9.level(), var3, var4));
            }
         }
      }

      return var6;
   }

   private static int a(RiverEngine var0, WorldGenLevel var1, int var2, int var3, ColumnPlan var4, int var5, int var6, int var7) {
      double var8 = Math.hypot(var4.flowDirX(), var4.flowDirZ());
      if (var8 < 1.0E-6) {
         return Integer.MAX_VALUE;
      } else {
         double var10 = -var4.flowDirZ() / var8;
         double var12 = var4.flowDirX() / var8;
         double var14 = var4.width();
         int var16 = Integer.MAX_VALUE;

         for (double var17 = var14 + 1.0; var17 <= var14 + 4.0; var17 += 3.0) {
            for (byte var19 = -1; var19 <= 1; var19 += 2) {
               int var20 = var2 + (int)Math.round(var10 * var17 * (double)var19);
               int var21 = var3 + (int)Math.round(var12 * var17 * (double)var19);
               Lake var22 = var0.lakeAt(var20, var21);
               int var23 = var22 != null && var22.digDepth() <= 0
                  ? Mth.clamp(var22.level(), var6, var7)
                  : var1.getHeight(Types.OCEAN_FLOOR_WG, var20, var21) - 1;
               var16 = Math.min(var16, var23);
            }
         }

         return Math.max(var16, var5 - 1);
      }
   }

   private static boolean a(RiverEngine var0, int var1, int var2, int var3) {
      for (int[] var7 : V) {
         ColumnPlan var8 = var0.plan(var1 + var7[0], var2 + var7[1]);
         if (!var8.inCorridor() || !var8.inChannel() || var8.waterSurfaceY() < var3) {
            return true;
         }
      }

      return false;
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = Math.max(var4, var6 + 1);

      for (int[] var11 : V) {
         int var12 = var2 + var11[0];
         int var13 = var3 + var11[1];

         for (int var14 = var7; var14 <= var5; var14++) {
            var1.set(var12, var14, var13);
            if (var0.getBlockState(var1).getFluidState().is(FluidTags.WATER)) {
               var0.scheduleTick(new BlockPos(var12, var14, var13), Fluids.WATER, 5 + Math.floorMod(Mth.getSeed(var12, var14, var13), 20));
            }
         }
      }
   }

   private static int a(ColumnPlan var0) {
      double var1 = var0.fluidFlowDirX();
      double var3 = var0.fluidFlowDirZ();
      if (Math.sqrt(var1 * var1 + var3 * var3) < 0.5) {
         return 8;
      } else {
         int var5 = (int)Math.round(Math.atan2(var0.fluidFlowDirZ(), var0.fluidFlowDirX()) / (Math.PI / 4));
         return (var5 % 8 + 8) % 8;
      }
   }

   private static boolean a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      return var0.getBlockState(var1.set(var2 + 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2 - 1, var3, var4)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 + 1)).isAir()
         && var0.getBlockState(var1.set(var2, var3, var4 - 1)).isAir();
   }

   private static boolean d(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      if (var3 - 1 >= var5) {
         BlockState var6 = var0.getBlockState(var1.set(var2, var3 - 1, var4));
         if (!var6.isAir() && var6.getFluidState().isEmpty()) {
            return true;
         }
      }

      return b(var0, var1, var2 + 1, var3, var4)
         || b(var0, var1, var2 - 1, var3, var4)
         || b(var0, var1, var2, var3, var4 + 1)
         || b(var0, var1, var2, var3, var4 - 1);
   }

   private static boolean b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4) {
      BlockState var5 = var0.getBlockState(var1.set(var2, var3, var4));
      return !var5.isAir() && var5.getFluidState().isEmpty();
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5, int var6) {
      if (var5.getBlock() instanceof FallingBlock && var3 - 1 >= var6) {
         var1.set(var2, var3 - 1, var4);
         BlockState var7 = var0.getBlockState(var1);
         if (var7.isAir() || !var7.getFluidState().isEmpty()) {
            var0.setBlock(var1, Blocks.STONE.defaultBlockState(), 2);
         }
      }

      var1.set(var2, var3, var4);
      var0.setBlock(var1, var5, 2);
   }

   private static void b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      BlockState var8 = a(var0, var2, var3, var6, var7);
      int var9 = Math.max(var7, Math.min(var4, var5));
      if (c(var2, var3)) {
         var1.set(var2, Math.max(var7, var9 - 1), var3);
         BlockState var10 = var0.getBlockState(var1);
         if (var10.isAir() || !var10.getFluidState().isEmpty()) {
            b(var2, var3, "CUP void below floor: hanging funnel from " + var9 + " to bed " + var5);
         }
      }

      for (int var12 = var9; var12 <= var5; var12++) {
         var1.set(var2, var12, var3);
         BlockState var11 = var0.getBlockState(var1);
         if (var11.isAir()) {
            a(var0, var1, var2, var12, var3, var8, var7);
         }
      }
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3, int var4, BlockState var5) {
      for (int[] var9 : o) {
         int var10 = var1 + var9[0];
         int var11 = var2 + var9[1];
         int var12 = var0.getHeight(Types.OCEAN_FLOOR, var10, var11) - 1;
         if (var12 >= var3 && var12 >= var4) {
            BlockState var13 = var0.getBlockState(new BlockPos(var10, var12, var11));
            if (!var13.isAir() && var13.getFluidState().isEmpty() && var13.canOcclude()) {
               return var13;
            }
         }
      }

      return var5;
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3) {
      BlockState var4 = var0.getBlockState(new BlockPos(var1, var3, var2));
      return !var4.isAir() && var4.getFluidState().isEmpty() && var4.canOcclude() ? var4 : null;
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3, int var4) {
      BlockState var5 = var0.getBlockState(new BlockPos(var1, var3, var2));
      BlockState var6 = var3 - 1 >= var4 ? var0.getBlockState(new BlockPos(var1, var3 - 1, var2)) : var5;
      return !var6.isAir() && var6.getFluidState().isEmpty() ? var6 : var5;
   }

   private static int a(
      WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6, int var7, int var8, int[] var9
   ) {
      int var10 = a(var7, var8, var9);
      if (var10 != Integer.MIN_VALUE) {
         return var10;
      } else {
         boolean var11 = false;

         for (int var12 = 0; var12 < 256 && !var11; var12++) {
            var11 = var9[var12] != Integer.MAX_VALUE;
         }

         return var11 && var5.distance() <= 8.0 ? Integer.MIN_VALUE : a(var0, var1, var2, var3, var4, var5, var6);
      }
   }

   private static int a(int var0, int var1, int[] var2) {
      int var3 = Integer.MAX_VALUE;
      int var4 = Integer.MAX_VALUE;

      for (int var5 = -8; var5 <= 8; var5++) {
         for (int var6 = -8; var6 <= 8; var6++) {
            int var7 = var0 + var5;
            int var8 = var1 + var6;
            if (var7 >= 0 && var7 <= 15 && var8 >= 0 && var8 <= 15) {
               int var9 = var2[var7 * 16 + var8];
               if (var9 != Integer.MAX_VALUE) {
                  int var10 = var5 * var5 + var6 * var6;
                  if (var10 < var4) {
                     var4 = var10;
                     var3 = var9;
                  }
               }
            }
         }
      }

      return var3 == Integer.MAX_VALUE ? Integer.MIN_VALUE : var3;
   }

   private static int a(WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6) {
      double var7 = Math.hypot(var5.flowDirX(), var5.flowDirZ());
      if (var7 < 1.0E-6) {
         return Integer.MIN_VALUE;
      } else if (var5.waterSurfaceY() <= var0.getHeight(Types.OCEAN_FLOOR_WG, var3, var4) - 1) {
         return Integer.MIN_VALUE;
      } else {
         double var9 = var5.flowDirX() / var7;
         double var11 = var5.flowDirZ() / var7;
         double var13 = -var5.flowDirZ() / var7;
         double var15 = var5.flowDirX() / var7;
         int var17 = (int)Math.round(var5.distance());
         int var18 = var3;
         int var19 = var4;
         if (var17 > 0) {
            int var20 = var3 + (int)Math.round(var13 * (double)var17);
            int var21 = var4 + (int)Math.round(var15 * (double)var17);
            int var22 = var3 - (int)Math.round(var13 * (double)var17);
            int var23 = var4 - (int)Math.round(var15 * (double)var17);
            ColumnPlan var24 = var1.plan(var20, var21);
            ColumnPlan var25 = var1.plan(var22, var23);
            boolean var26 = var24 != ColumnPlan.NONE && var24.inChannel();
            boolean var27 = var25 != ColumnPlan.NONE && var25.inChannel();
            if (var26 != var27) {
               var18 = var26 ? var20 : var22;
               var19 = var26 ? var21 : var23;
            } else {
               double var28 = var24 == ColumnPlan.NONE ? Double.MAX_VALUE : var24.distance();
               double var30 = var25 == ColumnPlan.NONE ? Double.MAX_VALUE : var25.distance();
               var18 = var28 <= var30 ? var20 : var22;
               var19 = var28 <= var30 ? var21 : var23;
            }
         }

         ColumnPlan var38 = var1.plan(var18, var19);
         if (var38 != ColumnPlan.NONE && var38.inChannel()) {
            int var39 = var0.getHeight(Types.OCEAN_FLOOR_WG, var18, var19) - 1;
            int var41 = e(var0, var2, var18, var19, var39, var6);
            if (var41 != Integer.MIN_VALUE && var38.waterSurfaceY() <= var41 + 1) {
               return var41;
            }
         }

         int var40 = 0;
         int var42 = 0;
         int var43 = Integer.MAX_VALUE;

         for (byte var44 = -3; var44 <= 3; var44 += 3) {
            int var45 = var18 + (int)Math.round(var9 * (double)var44);
            int var46 = var19 + (int)Math.round(var11 * (double)var44);
            ColumnPlan var47 = var44 == 0 ? var1.plan(var18, var19) : var1.plan(var45, var46);
            if (var47 != ColumnPlan.NONE && var47.inChannel()) {
               int var48 = Math.max(1, (int)Math.round(var47.width()));
               int var29 = Math.max(1, var48 / 2);
               int var49 = 0;
               int var31 = 0;
               int var32 = Integer.MAX_VALUE;

               for (int var33 = -var48; var33 <= var48; var33 += var29) {
                  int var34 = var45 + (int)Math.round(var13 * (double)var33);
                  int var35 = var46 + (int)Math.round(var15 * (double)var33);
                  ColumnPlan var36 = var1.plan(var34, var35);
                  if (var36 != ColumnPlan.NONE && var36.inCorridor()) {
                     var49++;
                     int var37 = b(var0, var1, var2, var34, var35, var36, var6);
                     if (var37 != Integer.MIN_VALUE) {
                        var31++;
                        var32 = Math.min(var32, var37);
                     }
                  }
               }

               if (var49 >= 3) {
                  var40++;
                  if (var31 == var49) {
                     var42++;
                     var43 = Math.min(var43, var32);
                  }
               }
            }
         }

         return var40 > 0 && var42 * 2 > var40 ? var43 : Integer.MIN_VALUE;
      }
   }

   private static int b(WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6) {
      long var7 = Hashing.mix((long)var3 << 32 ^ (long)var4 & 4294967295L);
      HashMap var9 = A.get();
      Integer var10 = (Integer)var9.get(var7);
      if (var10 != null) {
         return var10;
      } else {
         int var11 = var5.waterSurfaceY();
         int var12 = a(var0, var1, var2, var3, var4, var5, var11, var6);
         int var13 = var12 < var11 ? var12 : Integer.MIN_VALUE;
         if (var13 == Integer.MIN_VALUE) {
            int var14 = var0.getHeight(Types.OCEAN_FLOOR_WG, var3, var4) - 1;
            int var15 = e(var0, var2, var3, var4, var14, var6);
            if (var15 != Integer.MIN_VALUE && var11 <= var15 + 1) {
               var13 = var15;
            }
         }

         var9.put(var7, var13);
         return var13;
      }
   }

   private static int a(WorldGenLevel var0, RiverEngine var1, MutableBlockPos var2, int var3, int var4, ColumnPlan var5, int var6, int var7) {
      double var8 = Math.hypot(var5.flowDirX(), var5.flowDirZ());
      if (var8 < 1.0E-6) {
         return var6;
      } else {
         double var10 = var5.flowDirX() / var8;
         double var12 = var5.flowDirZ() / var8;
         int var14 = var6;
         int var15 = a(var0, var2, var3, var4, var5, var7);
         if (var15 != Integer.MIN_VALUE && var15 < var6) {
            var14 = var15;
         }

         for (byte var16 = 3; var16 <= 48; var16 += 3) {
            int var17 = var3 - (int)Math.round(var10 * (double)var16);
            int var18 = var4 - (int)Math.round(var12 * (double)var16);
            if (!a(var17, var18)) {
               break;
            }

            ColumnPlan var19 = var1.plan(var17, var18);
            if (var19 != ColumnPlan.NONE && var19.inChannel()) {
               int var20 = a(var0, var2, var17, var18, var19, var7);
               if (var20 != Integer.MIN_VALUE && var20 < var14) {
                  var14 = var20;
               }
            }
         }

         return var14;
      }
   }

   private static int a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, ColumnPlan var4, int var5) {
      double var6 = Math.hypot(var4.flowDirX(), var4.flowDirZ());
      if (var6 < 1.0E-6) {
         return Integer.MIN_VALUE;
      } else {
         double var8 = -var4.flowDirZ() / var6;
         double var10 = var4.flowDirX() / var6;
         int var12 = Math.max(1, (int)Math.round(var4.width()));
         int var13 = (int)Math.round(Math.min(var4.distance(), var4.width() + 8.0));

         for (byte var14 = -1; var14 <= 1; var14 += 2) {
            int var15 = var2 + (int)Math.round(var8 * (double)var13 * (double)var14);
            int var16 = var3 + (int)Math.round(var10 * (double)var13 * (double)var14);
            int var17 = Integer.MIN_VALUE;
            int var18 = Integer.MAX_VALUE;
            boolean var19 = true;
            boolean var20 = false;

            for (int var21 = -var12; var21 <= var12; var21 += var12 == 0 ? 1 : var12) {
               int var22 = var15 + (int)Math.round(var8 * (double)var21);
               int var23 = var16 + (int)Math.round(var10 * (double)var21);
               if (!a(var22, var23)) {
                  var20 = true;
                  break;
               }

               int var24 = var0.getHeight(Types.OCEAN_FLOOR_WG, var22, var23) - 1;
               int var25 = e(var0, var1, var22, var23, var24, var5);
               if (var25 == Integer.MIN_VALUE) {
                  var18 = Math.min(var18, var24);
               } else {
                  var17 = Math.max(var17, var25);
               }
            }

            if (var20) {
               if (var13 == 0) {
                  break;
               }
            } else {
               if (var17 == Integer.MIN_VALUE) {
                  var19 = false;
               } else if (var18 != Integer.MAX_VALUE && var18 > var17) {
                  var19 = false;
               }

               if (var19) {
                  return var17;
               }

               if (var13 == 0) {
                  break;
               }
            }
         }

         return Integer.MIN_VALUE;
      }
   }

   private static int e(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      int var6 = Math.min(var5, var4 + 1);
      if (var0.getBlockState(var1.set(var2, var6, var3)).getFluidState().isEmpty()) {
         return Integer.MIN_VALUE;
      } else {
         int var7 = var6;

         while (var7 + 1 <= var5 && var7 - var6 < 24 && !var0.getBlockState(var1.set(var2, var7 + 1, var3)).getFluidState().isEmpty()) {
            var7++;
         }

         return var7;
      }
   }

   private static boolean a(WorldGenLevel var0, RiverEngine var1, int var2, int var3, ColumnPlan var4, int var5) {
      double var6 = Math.hypot(var4.flowDirX(), var4.flowDirZ());
      if (var6 < 1.0E-6) {
         return false;
      } else {
         double var8 = var4.flowDirX() / var6;
         double var10 = var4.flowDirZ() / var6;

         for (byte var12 = 3; var12 <= 12; var12 += 3) {
            int var13 = var2 + (int)Math.round(var8 * (double)var12);
            int var14 = var3 + (int)Math.round(var10 * (double)var12);
            ColumnPlan var15 = var1.plan(var13, var14);
            if (var15 != ColumnPlan.NONE && var15.inChannel() && var15.waterSurfaceY() > var5 + 1) {
               int var16 = var0.getHeight(Types.OCEAN_FLOOR_WG, var13, var14) - 1;
               if (var16 >= var15.waterSurfaceY() - 3) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static BlockState a(WorldGenLevel var0, int var1, int var2, int var3, int var4, BlockState var5, int var6) {
      int var7 = var3 - var4;
      if (var7 < var6) {
         return var5;
      } else {
         BlockState var8 = var0.getBlockState(new BlockPos(var1, var7, var2));
         return !var8.isAir() && var8.getFluidState().isEmpty() ? var8 : var5;
      }
   }

   private static boolean a(BlockState var0) {
      return ai.contains(var0.getBlock()) || var0.hasProperty(BlockStateProperties.SNOWY);
   }

   private static void b(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5, int var6) {
      if (var3 >= var6 && !var5.isAir()) {
         BlockState var7 = var0.getBlockState(var1.set(var2, var3, var4));
         if (a(var7) && !var7.equals(var5)) {
            var0.setBlock(var1, var5, 2);
         }
      }
   }

   private static int a(int var0, int var1, int var2, ColumnPlan var3, double var4) {
      return a(var0, var1, var2, var3, var4, 8);
   }

   private static int a(int var0, int var1, int var2, ColumnPlan var3, double var4, int var6) {
      double var7 = Math.max(5.0, var3.width() * var4);
      int var9 = Mth.clamp((int)Math.round(5.0 * var3.width()), Math.min(5, var6), var6);
      double var10 = Math.max(0.0, var3.distance() - var3.width());
      double var12 = var7 > 1.0E-6 ? Math.min(1.0, var10 / var7) : 1.0;
      int var14 = (int)Math.round((double)var9 - (double)(var9 - 1) * var12);
      int var15 = (int)Math.round((b(var0, var1, 5) - 0.5) * 2.0 * 2.0);
      return var2 - var14 + var15;
   }

   private static int a(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4, int var5, int var6) {
      int var7 = var3.waterSurfaceY();
      if (var4 >= var7) {
         return var7;
      } else {
         return var3.source() ? Math.min(var7, a(var0, var1, var2, var3, var5, var6)) : var7;
      }
   }

   private static boolean a(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4) {
      double var5 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var5 < 1.0E-6) {
         return false;
      } else {
         double var7 = -var3.flowDirZ() / var5;
         double var9 = var3.flowDirX() / var5;

         for (byte var11 = -1; var11 <= 1; var11 += 2) {
            boolean var12 = false;

            for (int var13 = 1; var13 <= 16; var13++) {
               int var14 = var1 + (int)Math.round(var7 * (double)var13 * (double)var11);
               int var15 = var2 + (int)Math.round(var9 * (double)var13 * (double)var11);
               if (var0.getHeight(Types.OCEAN_FLOOR_WG, var14, var15) - 1 >= var4) {
                  var12 = true;
                  break;
               }
            }

            if (!var12) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean b(WorldGenLevel var0, int var1, int var2, int var3, int var4, int var5) {
      MutableBlockPos var6 = new MutableBlockPos();

      for (int var7 = var3; var7 >= Math.max(var5, var3 - 3); var7--) {
         var6.set(var1, var7, var2);
         BlockState var8 = var0.getBlockState(var6);
         if (var8.isAir() || !var8.getFluidState().isEmpty()) {
            return false;
         }
      }

      for (int var13 = 0; var13 < 4; var13++) {
         int var14 = var1 + (var13 == 0 ? 1 : (var13 == 1 ? -1 : 0));
         int var9 = var2 + (var13 == 2 ? 1 : (var13 == 3 ? -1 : 0));
         int var10 = var0.getHeight(Types.OCEAN_FLOOR_WG, var14, var9) - 1;

         for (int var11 = var3 + 1; var11 <= Math.min(var4, var10); var11++) {
            var6.set(var14, var11, var9);
            BlockState var12 = var0.getBlockState(var6);
            if (var12.isAir() || !var12.getFluidState().isEmpty()) {
               return false;
            }
         }
      }

      return true;
   }

   private static int a(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4, int var5) {
      double var6 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var6 < 1.0E-6) {
         return var5;
      } else {
         double var8 = -var3.flowDirZ() / var6;
         double var10 = var3.flowDirX() / var6;
         double var12 = var3.width() + 2.0;
         int var14 = Integer.MIN_VALUE;

         for (byte var15 = -1; var15 <= 1; var15 += 2) {
            int var16 = var1 + (int)Math.round(var8 * var12 * (double)var15);
            int var17 = var2 + (int)Math.round(var10 * var12 * (double)var15);
            var14 = Math.max(var14, var0.getHeight(Types.OCEAN_FLOOR_WG, var16, var17) - 1);
         }

         return Mth.clamp(var14, var4, var5);
      }
   }

   private static int b(WorldGenLevel var0, int var1, int var2, ColumnPlan var3, int var4) {
      double var5 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var5 < 1.0E-6) {
         return Integer.MAX_VALUE;
      } else {
         double var7 = -var3.flowDirZ() / var5;
         double var9 = var3.flowDirX() / var5;
         double var11 = var3.width();
         int var13 = Integer.MAX_VALUE;

         for (double var14 = var11 + 1.0; var14 <= var11 + 4.0; var14 += 3.0) {
            for (byte var16 = -1; var16 <= 1; var16 += 2) {
               int var17 = var1 + (int)Math.round(var7 * var14 * (double)var16);
               int var18 = var2 + (int)Math.round(var9 * var14 * (double)var16);
               var13 = Math.min(var13, var0.getHeight(Types.OCEAN_FLOOR_WG, var17, var18) - 1);
            }
         }

         return Math.max(var13, var4 - 1);
      }
   }

   private static boolean a(RiverEngine var0, int var1, int var2, ColumnPlan var3) {
      double var4 = Math.hypot(var3.flowDirX(), var3.flowDirZ());
      if (var4 < 1.0E-6) {
         return false;
      } else {
         double var6 = -var3.flowDirZ() / var4;
         double var8 = var3.flowDirX() / var4;
         double var10 = (double)var1 + 0.5;
         double var12 = (double)var2 + 0.5;
         boolean var14 = false;
         boolean var15 = false;

         for (byte var16 = 2; var16 <= 8 && (!var14 || !var15); var16 += 2) {
            if (!var14 && a(var0, var10 + var6 * (double)var16, var12 + var8 * (double)var16)) {
               var14 = true;
            }

            if (!var15 && a(var0, var10 - var6 * (double)var16, var12 - var8 * (double)var16)) {
               var15 = true;
            }
         }

         return var14 && var15;
      }
   }

   private static boolean a(RiverEngine var0, double var1, double var3) {
      return var0.plan((int)Math.round(var1), (int)Math.round(var3)).distance() <= 2.0;
   }

   private static double a(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = var0 + var2 * var4;
      double var14 = 0.45 * var6;
      if (!(var2 <= var14) && !(var8 <= var14)) {
         double var16 = var0 + var14 * var4;
         if (var10 <= var16) {
            return var12;
         } else {
            double var18 = Math.min(1.0, (var10 - var0) / 8.0);
            double var20 = 16.0 * var18;
            if (var20 <= 1.0E-6) {
               return var12;
            } else {
               double var22 = Math.min(1.0, (var2 - var14) / (var8 - var14));
               double var24 = var16 + (var10 - var16) * Math.log1p(var20 * var22) / Math.log1p(var20);
               double var26 = Math.min(1.0, var22 / 0.35);
               var26 = var26 * var26 * (3.0 - 2.0 * var26);
               return Math.max(var12, var12 + (var24 - var12) * var26);
            }
         }
      } else {
         return var12;
      }
   }

   private static int[] a() {
      String var0 = System.getProperty("streamsreflowing.carvetrace");
      if (var0 != null && !var0.isBlank()) {
         try {
            String[] var1 = var0.split(",");
            return new int[]{Integer.parseInt(var1[0].trim()), Integer.parseInt(var1[1].trim()), var1.length > 2 ? Integer.parseInt(var1[2].trim()) : 24};
         } catch (Exception var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void a(ServerLevel var0, BlockPos var1, int var2) {
      RiverEngine var3 = StreamsWorldgen.engine(var0);
      if (var3 != null) {
         int var4 = (var1.getX() >> 4) - var2 << 4;
         int var5 = (var1.getZ() >> 4) - var2 << 4;
         int var6 = (var2 * 2 + 1) * 16;
         TreeMap var7 = new TreeMap();
         StringBuilder var8 = new StringBuilder();
         int var9 = 0;
         int var10 = 0;
         Constants.LOG
            .info(
               "[SR-WATERMAP] ==== attribution around ({},{}) r={} chunks, plate {} ====",
               var1.getX(),
               var1.getZ(),
               var2,
               var3.plateAt(var1.getX(), var1.getZ())
            );

         for (int var11 = 0; var11 < var6; var11++) {
            StringBuilder var12 = new StringBuilder();

            for (int var13 = 0; var13 < var6; var13++) {
               if (var13 > 0 && (var13 & 15) == 0) {
                  var12.append('|');
               }

               int var14 = var4 + var13;
               int var15 = var5 + var11;
               int var16 = var0.getHeight(Types.MOTION_BLOCKING, var14, var15) - 1;
               int var17 = var0.getHeight(Types.OCEAN_FLOOR, var14, var15) - 1;
               boolean var18 = var16 > var17;
               ColumnPlan var19 = var3.planLean(var14, var15);
               boolean var20 = var19 != ColumnPlan.NONE && var19.inChannel();
               boolean var21 = var19 != ColumnPlan.NONE && !var20 && var18 && Math.abs(var16 - var19.waterSurfaceY()) <= 2;
               Lake var22 = var3.lakeAt(var14, var15);
               Lake var23 = var3.lakeNear(var14, var15, 2);
               boolean var24 = var23 != null && var23.digDepth() == 0 && var17 + 1 < var23.level();
               char var25;
               if (var18) {
                  if (var20) {
                     var25 = 'S';
                  } else if (var22 != null) {
                     var25 = 'L';
                  } else if (var24) {
                     var25 = 'l';
                  } else if (var21) {
                     var25 = 'c';
                  } else {
                     var25 = 'O';
                     var9++;
                     var7.merge(var16, 1, Integer::sum);
                     if (var8.length() < 600) {
                        var8.append(String.format(" (%d,%d,%d)", var14, var16, var15));
                     }
                  }
               } else if (!var20 && !var24) {
                  var25 = '.';
               } else {
                  var25 = 'm';
                  var10++;
               }

               var12.append(var25);
            }

            Constants.LOG.info("[SR-WATERMAP] {}", var12);
            if ((var11 & 15) == 15 && var11 < var6 - 1) {
               Constants.LOG.info("[SR-WATERMAP] {}", "-".repeat(var6 + var2 * 2));
            }
         }

         Constants.LOG.info("[SR-WATERMAP] orphans={} (levels {}), expected-but-dry={}; orphan samples:{}", var9, var7, var10, var8);
      }
   }

   public static void armTraceOverride(int var0, int var1, int var2) {
      aA = new int[]{var0, var1, var2};
   }

   public static void waterMap(ServerLevel var0, BlockPos var1, int var2) {
      a(var0, var1, var2);
   }

   private static boolean b(int var0, int var1) {
      int[] var2 = aA != null ? aA : az;
      return var2 != null && var0 == var2[0] && var1 == var2[1];
   }

   private static void a(int var0, int var1, String var2) {
      Constants.LOG.info("[SR-STACK] ({},{}) {}", var0, var1, var2);
   }

   private static int a(int var0, int var1, String var2, int var3, int var4) {
      if (b(var0, var1) && var3 != var4) {
         a(var0, var1, "  " + var2 + ": " + var3 + " -> " + var4);
      }

      return var4;
   }

   private static boolean c(int var0, int var1) {
      int[] var2 = aA != null ? aA : az;
      return var2 != null && Math.abs(var0 - var2[0]) <= var2[2] && Math.abs(var1 - var2[1]) <= var2[2];
   }

   private static void b(int var0, int var1, String var2) {
      Constants.LOG.info("[SR-CARVETRACE] ({},{}) {}", var0, var1, var2);
   }

   public static int adjacentCoveringLakeLevel(RiverEngine var0, int var1, int var2) {
      int var3 = Integer.MIN_VALUE;

      for (int var4 = 0; var4 < 8; var4++) {
         for (byte var5 = 2; var5 <= 4; var5 += 2) {
            Lake var6 = var0.lakeAt(var1 + aB[var4] * var5, var2 + aC[var4] * var5);
            if (var6 != null && var6.level() > var3) {
               var3 = var6.level();
            }
         }
      }

      return var3;
   }

   private static double d(int var0, int var1) {
      long var2 = (long)var0 * -7046029254386353131L ^ (long)var1 * -4417276706812531889L;
      var2 ^= var2 >>> 30;
      var2 *= -4658895280553007687L;
      var2 ^= var2 >>> 27;
      var2 *= -7723592293110705685L;
      var2 ^= var2 >>> 31;
      return (double)(var2 >>> 11) * 1.110223E-16F;
   }

   private static double b(int var0, int var1, int var2) {
      int var3 = Math.floorDiv(var0, var2);
      int var4 = Math.floorDiv(var1, var2);
      double var5 = (double)(var0 - var3 * var2) / (double)var2;
      double var7 = (double)(var1 - var4 * var2) / (double)var2;
      var5 = var5 * var5 * (3.0 - 2.0 * var5);
      var7 = var7 * var7 * (3.0 - 2.0 * var7);
      double var9 = d(var3, var4);
      double var11 = d(var3 + 1, var4);
      double var13 = d(var3, var4 + 1);
      double var15 = d(var3 + 1, var4 + 1);
      double var17 = var9 + (var11 - var9) * var5;
      double var19 = var13 + (var15 - var13) * var5;
      return var17 + (var19 - var17) * var7;
   }

   private static double c(int var0, int var1, int var2) {
      long var3 = (long)var0 * -7046029254386353131L ^ (long)var1 * -4417276706812531889L ^ (long)var2 * 1609587929392839161L;
      var3 ^= var3 >>> 30;
      var3 *= -4658895280553007687L;
      var3 ^= var3 >>> 27;
      var3 *= -7723592293110705685L;
      var3 ^= var3 >>> 31;
      return (double)(var3 >>> 11) * 1.110223E-16F;
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, BlockState var5) {
      for (int var6 = 0; var6 < 4; var6++) {
         int var7 = var2 + (var6 == 0 ? 1 : (var6 == 1 ? -1 : 0));
         int var8 = var4 + (var6 == 2 ? 1 : (var6 == 3 ? -1 : 0));
         BlockState var9 = var0.getBlockState(var1.set(var7, var3, var8));
         if (var9.is(Blocks.SNOW) && !var9.isCollisionShapeFullBlock(var0, var1)) {
            var0.setBlock(var1, var5, 2);
         }
      }
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int[] var2, int var3, int var4, int var5, BlockState var6) {
      boolean var7 = false;

      for (int var11 : var2) {
         if (var11 != Integer.MIN_VALUE) {
            var7 = true;
            break;
         }
      }

      if (var7) {
         boolean[] var13 = new boolean[256];

         for (int var14 = 0; var14 < 256; var14++) {
            if (var2[var14] != Integer.MIN_VALUE) {
               int var15 = var2[var14];

               for (int var16 = 0; var16 < 4; var16++) {
                  int var12 = e(var14, var16);
                  if (var12 >= 0 && !var13[var12] && var2[var12] == Integer.MIN_VALUE) {
                     a(var0, var1, var12, var15, var13, var3, var4, var5, var6);
                  }
               }
            }
         }
      }
   }

   private static int e(int var0, int var1) {
      int var2 = var0 >> 4;
      int var3 = var0 & 15;
      int var4 = var2 + (var1 == 0 ? 1 : (var1 == 1 ? -1 : 0));
      int var5 = var3 + (var1 == 2 ? 1 : (var1 == 3 ? -1 : 0));
      return var4 >= 0 && var4 <= 15 && var5 >= 0 && var5 <= 15 ? var4 << 4 | var5 : -1;
   }

   private static void a(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, boolean[] var4, int var5, int var6, int var7, BlockState var8) {
      ArrayList var9 = new ArrayList();
      ArrayDeque var10 = new ArrayDeque();
      boolean[] var11 = new boolean[256];
      var10.add(var2);
      var11[var2] = true;
      boolean var12 = true;

      while (!var10.isEmpty() && var12) {
         int var13 = (Integer)var10.poll();
         int var14 = var5 + (var13 >> 4);
         int var15 = var6 + (var13 & 15);
         if (var0.getBlockState(var1.set(var14, var3, var15)).isAir()) {
            var9.add(var13);
            if (var9.size() > 24 || !f(var0, var1, var14, var3, var15, var7)) {
               var12 = false;
               break;
            }

            for (int var16 = 0; var16 < 4; var16++) {
               int var17 = e(var13, var16);
               if (var17 < 0) {
                  var12 = false;
                  break;
               }

               if (!var11[var17]) {
                  var11[var17] = true;
                  var10.add(var17);
               }
            }
         }
      }

      for (int var21 : var9) {
         var4[var21] = true;
      }

      if (var12 && !var9.isEmpty()) {
         for (int var22 : var9) {
            int var23 = var5 + (var22 >> 4);
            int var24 = var6 + (var22 & 15);

            for (int var25 = var3; var25 > var7; var25--) {
               var1.set(var23, var25, var24);
               BlockState var18 = var0.getBlockState(var1);
               if (!var18.isAir()) {
                  break;
               }

               a(var0, var1, var8);
            }
         }
      }
   }

   private static boolean f(WorldGenLevel var0, MutableBlockPos var1, int var2, int var3, int var4, int var5) {
      for (int var6 = var3 - 1; var6 >= Math.max(var5, var3 - 6); var6--) {
         BlockState var7 = var0.getBlockState(var1.set(var2, var6, var4));
         if (!var7.isAir() && var7.getFluidState().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static int a(RiverEngine var0, WorldGenLevel var1, ColumnPlan var2, int var3, int var4, int var5) {
      int var6 = a(var1, var3, var4);
      if (var6 == var5) {
         return Integer.MIN_VALUE;
      } else {
         int var7 = var2.waterSurfaceY();
         double var8 = var2.distance() - var2.width();
         double var10 = Math.min(2.0, var0.config().bankSlope() + var0.config().bankSteepFactor() * Math.pow(var2.slope(), 0.8));
         double var12 = a((double)var7, var8, var10, var2.width(), Math.max(5.0, var2.width() * var0.config().bankWidth()), (double)var6);
         double var14 = (double)var7 + var2.bankBlend() * (double)(var6 - var7);
         return Mth.clamp((int)Math.floor(Math.max(var12, var14)), var7, Math.max(var7, var6));
      }
   }

   private static int a(ColumnPlan var0, ColumnPlan var1) {
      ColumnPlan var2 = var0.waterSurfaceY() >= var1.waterSurfaceY() ? var0 : var1;
      int var3 = Math.min(var0.waterSurfaceY(), var1.waterSurfaceY());
      double var4 = Math.max(0.0, var2.distance() - var2.width());
      int var6 = (int)Math.floor(Math.max(0.0, var4 - 3.0) * 2.0);
      return Math.max(var3, var2.waterSurfaceY() + 1 - var6);
   }

   private static int a(RiverEngine var0, ColumnPlan var1, int var2, double var3) {
      double var5 = var1.distance() - var1.width();
      if (var5 <= 0.0) {
         return Integer.MAX_VALUE;
      } else {
         int var7 = Math.min(var1.waterSurfaceY(), var2);
         double var8 = Math.min(2.0, var0.config().bankSlope() + var0.config().bankSteepFactor() * Math.pow(var1.slope(), 0.8));
         double var10 = a((double)var7, var5, var8, var1.width(), Math.max(5.0, var1.width() * var0.config().bankWidth()), (double)var2);
         double var12 = (double)var7 + var1.bankBlend() * (double)(var2 - var7);
         return Mth.clamp((int)Math.floor(Math.max(var10, var12) + var3), var7, Math.max(var7, var2));
      }
   }

   private static void b(WorldGenLevel var0, int var1, int var2, int[] var3) {
      if (aD) {
         boolean var4 = false;

         for (int var8 : var3) {
            if (var8 > 0) {
               var4 = true;
               break;
            }
         }

         if (var4) {
            Map var9 = aE.computeIfAbsent(var0.getLevel().dimension(), var0x -> new ConcurrentHashMap<>());
            if (var9.size() >= 2048) {
               Iterator var10 = var9.keySet().iterator();
               if (var10.hasNext()) {
                  var10.next();
                  var10.remove();
               }
            }

            var9.put((long)(var1 >> 4) << 32 ^ (long)(var2 >> 4) & 4294967295L, (int[])var3.clone());
         }
      }
   }

   public static void drainDeferredSeams(ServerLevel var0) {
      if (aD) {
         Map var1 = aE.get(var0.dimension());
         if (var1 != null && !var1.isEmpty()) {
            RiverEngine var2 = StreamsWorldgen.engine(var0);
            if (var2 != null) {
               int var3 = 0;
               Iterator var4 = var1.entrySet().iterator();

               while (var4.hasNext() && var3 < 2) {
                  Entry var5 = (Entry)var4.next();
                  long var6 = (Long)var5.getKey();
                  int var8 = (int)(var6 >> 32);
                  int var9 = (int)var6;
                  int var10 = var8 << 4;
                  int var11 = var9 << 4;
                  if (!var0.hasChunk(var8, var9)) {
                     var4.remove();
                  } else {
                     boolean var12 = true;

                     for (int var13 = -1; var13 <= 1 && var12; var13++) {
                        for (int var14 = -1; var14 <= 1 && var12; var14++) {
                           var12 = b(var0, var10 + var13 * 16, var11 + var14 * 16);
                        }
                     }

                     if (var12) {
                        var4.remove();
                        int var21 = var0.getMinBuildHeight();
                        int var22 = var0.getMaxBuildHeight() - 1;
                        int[] var15 = new int[256];
                        ColumnPlan[] var16 = new ColumnPlan[256];
                        int[] var17 = new int[256];
                        Arrays.fill(var17, Integer.MAX_VALUE);

                        for (int var18 = 0; var18 < 16; var18++) {
                           for (int var19 = 0; var19 < 16; var19++) {
                              int var20 = var18 * 16 + var19;
                              var15[var20] = var0.getHeight(Types.OCEAN_FLOOR_WG, var10 + var18, var11 + var19) - 1;
                              var16[var20] = var2.plan(var10 + var18, var11 + var19);
                           }
                        }

                        a(var0, var2, var10, var11, var15, (int[])var5.getValue(), var16, var21, var22);
                        var3++;
                     }
                  }
               }
            }
         }
      }
   }

   public static void forgetDeferredSeams(ServerLevel var0) {
      aE.remove(var0.dimension());
   }
}
