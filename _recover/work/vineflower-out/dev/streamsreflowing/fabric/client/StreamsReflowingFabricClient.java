package dev.streamsreflowing.fabric.client;

import dev.streamsreflowing.StallWatch;
import dev.streamsreflowing.client.FrameDiag;
import dev.streamsreflowing.client.HarnessPilot;
import dev.streamsreflowing.client.StreamTint;
import dev.streamsreflowing.fabric.compat.DistantHorizonsCompat;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.fabric.net.StreamFlowNet;
import dev.streamsreflowing.fabric.particle.StreamBubbleParticle;
import dev.streamsreflowing.fabric.particle.StreamFoamParticle;
import dev.streamsreflowing.fabric.particle.StreamLeafParticle;
import dev.streamsreflowing.fabric.particle.StreamParticles;
import dev.streamsreflowing.fabric.particle.StreamSplashParticle;
import dev.streamsreflowing.fabric.particle.StreamStickParticle;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.RTFFlowClientSweep;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.Load;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.Unload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class StreamsReflowingFabricClient implements ClientModInitializer {
   private static final ResourceLocation WATER_STILL = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
   private static final ResourceLocation WATER_FLOW = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
   private static final ResourceLocation WATER_OVERLAY = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_overlay");

   @Override
   public void onInitializeClient() {
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> FrameDiag.tick(mc.getFps()));
      BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(), new Fluid[]{StreamFluids.SOURCE, StreamFluids.FLOWING});
      SimpleFluidRenderHandler handler = new SimpleFluidRenderHandler(WATER_STILL, WATER_FLOW, WATER_OVERLAY) {
         public int getFluidColor(BlockAndTintGetter view, BlockPos pos, FluidState state) {
            return view != null && pos != null ? BiomeColors.getAverageWaterColor(view, pos) : -1;
         }
      };
      FluidRenderHandlerRegistry.INSTANCE.register(StreamFluids.SOURCE, StreamFluids.FLOWING, handler);
      ColorProviderRegistry.BLOCK
         .register(
            (BlockColor)(state, view, pos, tintIndex) -> view != null && pos != null ? BiomeColors.getAverageWaterColor(view, pos) : -1,
            new Block[]{StreamFluids.STREAM_BLOCK}
         );
      ParticleFactoryRegistry.getInstance().register(StreamParticles.STREAM_BUBBLE, StreamBubbleParticle.Provider::new);
      ParticleFactoryRegistry.getInstance().register(StreamParticles.STREAM_SPLASH, sprites -> {
         StreamParticleScanner.splashSprites = sprites;
         return new StreamSplashParticle.Provider(sprites);
      });
      ParticleFactoryRegistry.getInstance().register(StreamParticles.STREAM_FOAM, sprites -> {
         StreamParticleScanner.foamSprites = sprites;
         return new StreamFoamParticle.Provider(sprites);
      });
      ParticleFactoryRegistry.getInstance().register(StreamParticles.STREAM_LEAF, StreamLeafParticle.Provider::new);
      ParticleFactoryRegistry.getInstance().register(StreamParticles.STREAM_STICK, StreamStickParticle.Provider::new);
      ClientPlayNetworking.registerGlobalReceiver(
         StreamFlowNet.FlowGridPayload.TYPE,
         (payload, context) -> context.client().execute(() -> ClientFlowHook.store(payload.cx(), payload.cz(), payload.grid()))
      );
      ClientChunkEvents.CHUNK_LOAD.register((Load)(world, chunk) -> StreamTint.onChunkArrived(chunk));
      ClientChunkEvents.CHUNK_UNLOAD.register((Unload)(world, chunk) -> {
         ChunkPos cp = chunk.getPos();
         StreamFlowNet.onClientChunkUnload(cp.x, cp.z);
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(conn, client) -> ClientFlowGrids.clear());
      ClientChunkEvents.CHUNK_LOAD.register((Load)(level, chunk) -> {
         if (level.dimension().equals(Level.OVERWORLD)) {
            byte[] u = RTFBridge.flowUnderlay(chunk);
            if (u != null) {
               RTFFlowUnderlay.put(true, chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, u);
            }
         }
      });
      ClientChunkEvents.CHUNK_UNLOAD
         .register((Unload)(level, chunk) -> RTFFlowUnderlay.remove(true, chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> RTFFlowClientSweep.onClientTick(mc.level, mc.player != null ? mc.player.blockPosition() : null));
      Object[] streamsLastLevel = new Object[]{null};
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
         if (mc.level != streamsLastLevel[0]) {
            streamsLastLevel[0] = mc.level;
            ClientFlowGrids.clear();
         }
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> StreamParticleScanner.onClientTick());
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> StreamTint.clientTick());
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> ClientFlowHook.drainTick());
      if (StallWatch.ENABLED) {
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> StallWatch.beatClient());
      }

      if (HarnessPilot.active()) {
         ClientTickEvents.END_CLIENT_TICK.register(HarnessPilot::tick);
      }

      if (FabricLoader.getInstance().isModLoaded("distanthorizons")) {
         DistantHorizonsCompat.init();
      }
   }
}
