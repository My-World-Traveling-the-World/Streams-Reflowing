package dev.streamsreflowing.fabric.fluid;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

public final class StreamFluids {
   public static final FlowingFluid SOURCE = new StreamFlowingFluid.Source();
   public static final FlowingFluid FLOWING = new StreamFlowingFluid.Flowing();
   public static final StreamLiquidBlock STREAM_BLOCK = new StreamLiquidBlock(SOURCE, Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.WATER));

   public static void register() {
      Registry.register(BuiltInRegistries.FLUID, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream"), SOURCE);
      Registry.register(BuiltInRegistries.FLUID, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "flowing_stream"), FLOWING);
      Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "stream"), STREAM_BLOCK);
   }

   private StreamFluids() {
   }
}
