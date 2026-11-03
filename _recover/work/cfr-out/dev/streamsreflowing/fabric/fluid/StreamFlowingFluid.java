/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.fluid;

import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.fabric.fluid.StreamLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.Vec3;

public final class StreamFlowingFluid {
    private static final int CALM_MAX = 3;

    private StreamFlowingFluid() {
    }

    private static Vec3 bakedFlow(BlockGetter level, BlockPos pos) {
        BlockState bs = level.getBlockState(pos);
        if (!(bs.getBlock() instanceof StreamLiquidBlock)) {
            return null;
        }
        int f = bs.getValue(StreamLiquidBlock.FLOW);
        if (f != 8) {
            return StreamLiquidBlock.DIRS[f];
        }
        double x = 0.0;
        double z = 0.0;
        int n = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int nf;
            BlockState ns = level.getBlockState(pos.relative(d));
            if (!(ns.getBlock() instanceof StreamLiquidBlock) || (nf = ns.getValue(StreamLiquidBlock.FLOW).intValue()) == 8) continue;
            Vec3 v = StreamLiquidBlock.DIRS[nf];
            x += v.x;
            z += v.z;
            ++n;
        }
        return n > 0 ? new Vec3(x, 0.0, z).normalize() : null;
    }

    private static Vec3 resolve(Vec3 natural, BlockGetter level, BlockPos pos) {
        int d = StreamFlowingFluid.axisDistToVanillaWater(level, pos, 3);
        if (d > 0 && d <= StreamFlowingFluid.calmReach(pos.getX(), pos.getZ())) {
            return Vec3.ZERO;
        }
        if (natural.lengthSqr() > 1.0E-6) {
            return natural;
        }
        Vec3 baked = StreamFlowingFluid.bakedFlow(level, pos);
        return baked != null ? baked : natural;
    }

    private static int calmReach(int x, int z) {
        int h = Math.floorDiv(x, 3) * 374761393 + Math.floorDiv(z, 3) * 668265263;
        h = (h ^ h >>> 13) * 1274126177;
        return 1 + Math.floorMod(h >>> 8, 3);
    }

    private static int axisDistToVanillaWater(BlockGetter level, BlockPos pos, int max) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int r = 1; r <= max; ++r) {
            if (!StreamFlowingFluid.isVanillaWater(level, m.set(x + r, y, z)) && !StreamFlowingFluid.isVanillaWater(level, m.set(x - r, y, z)) && !StreamFlowingFluid.isVanillaWater(level, m.set(x, y, z + r)) && !StreamFlowingFluid.isVanillaWater(level, m.set(x, y, z - r))) continue;
            return r;
        }
        return -1;
    }

    private static boolean isVanillaWater(BlockGetter level, BlockPos pos) {
        BlockState ns = level.getBlockState(pos);
        return !(ns.getBlock() instanceof StreamLiquidBlock) && ns.getFluidState().is(FluidTags.WATER);
    }

    private static BlockState legacyBlock(int level) {
        return (BlockState)((BlockState)StreamFluids.STREAM_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, level)).setValue(StreamLiquidBlock.FLOW, 8);
    }

    private static Fluid waterEquivalent(Fluid fluid) {
        if (fluid == StreamFluids.SOURCE) {
            return Fluids.WATER;
        }
        return fluid == StreamFluids.FLOWING ? Fluids.FLOWING_WATER : fluid;
    }

    private static boolean spreadIntoContainer(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        Block block = state.getBlock();
        if (!(block instanceof LiquidBlockContainer)) {
            return false;
        }
        LiquidBlockContainer container = (LiquidBlockContainer)((Object)block);
        if (!container.placeLiquid(level, pos, state, fluidState) && fluidState.isSource()) {
            container.placeLiquid(level, pos, state, Fluids.WATER.defaultFluidState());
        }
        return true;
    }

    public static final class Flowing
    extends WaterFluid.Flowing {
        @Override
        public Fluid getFlowing() {
            return StreamFluids.FLOWING;
        }

        @Override
        public Fluid getSource() {
            return StreamFluids.SOURCE;
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == StreamFluids.SOURCE || fluid == StreamFluids.FLOWING || fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return StreamFlowingFluid.legacyBlock(Flowing.getLegacyLevel(state));
        }

        @Override
        public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return StreamFlowingFluid.resolve(super.getFlow(level, pos, state), level, pos);
        }

        @Override
        protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromState, Direction direction, BlockPos toPos, BlockState toState, FluidState toFluid, Fluid fluid) {
            return super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, fluid) || toState.getBlock() instanceof LiquidBlockContainer && super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, StreamFlowingFluid.waterEquivalent(fluid));
        }

        @Override
        protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
            if (!StreamFlowingFluid.spreadIntoContainer(level, pos, state, fluidState)) {
                super.spreadTo(level, pos, state, direction, fluidState);
            }
        }
    }

    public static final class Source
    extends WaterFluid.Source {
        @Override
        public Fluid getFlowing() {
            return StreamFluids.FLOWING;
        }

        @Override
        public Fluid getSource() {
            return StreamFluids.SOURCE;
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == StreamFluids.SOURCE || fluid == StreamFluids.FLOWING || fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return StreamFlowingFluid.legacyBlock(Source.getLegacyLevel(state));
        }

        @Override
        public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return StreamFlowingFluid.resolve(super.getFlow(level, pos, state), level, pos);
        }

        @Override
        protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromState, Direction direction, BlockPos toPos, BlockState toState, FluidState toFluid, Fluid fluid) {
            return super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, fluid) || toState.getBlock() instanceof LiquidBlockContainer && super.canSpreadTo(level, fromPos, fromState, direction, toPos, toState, toFluid, StreamFlowingFluid.waterEquivalent(fluid));
        }

        @Override
        protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
            if (!StreamFlowingFluid.spreadIntoContainer(level, pos, state, fluidState)) {
                super.spreadTo(level, pos, state, direction, fluidState);
            }
        }
    }
}

