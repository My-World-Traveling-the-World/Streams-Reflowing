/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.StallWatch;
import dev.streamsreflowing.StreamsReflowing;
import dev.streamsreflowing.command.RegenPending;
import dev.streamsreflowing.command.StreamsCommands;
import dev.streamsreflowing.compat.FlowingFluidsCompat;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.fabric.compat.SableFlowCompat;
import dev.streamsreflowing.fabric.fluid.StreamFluids;
import dev.streamsreflowing.fabric.net.StreamFlowNet;
import dev.streamsreflowing.fabric.particle.StreamParticles;
import dev.streamsreflowing.flow.RTFFlowUnderlay;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import dev.streamsreflowing.worldgen.bank.BankFeature;
import dev.streamsreflowing.worldgen.bank.BankStyle;
import dev.streamsreflowing.worldgen.compat.RTFBridge;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.GenerationStep;

public class StreamsReflowingFabric
implements ModInitializer {
    private static boolean streamFluidRegistered;

    public static boolean streamFluidRegistered() {
        return streamFluidRegistered;
    }

    @Override
    public void onInitialize() {
        boolean dedicated;
        StreamsConfigFabric.load();
        StreamsConfigFabric.startWatching();
        StreamsReflowing.init();
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            byte[] u;
            if (level.dimension().equals(Level.OVERWORLD) && (u = RTFBridge.flowUnderlay(chunk)) != null) {
                RTFFlowUnderlay.put(false, chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, u);
            }
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> RTFFlowUnderlay.remove(false, chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4));
        DynamicRegistries.register(BankStyle.REGISTRY_KEY, BankStyle.CODEC);
        DynamicRegistries.register(BankFeature.REGISTRY_KEY, BankFeature.CODEC);
        boolean bl = dedicated = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        if (!dedicated || StreamsConfigFabric.bakedStreamFluid() || StreamsConfigFabric.legacyStreamFluid()) {
            StreamFluids.register();
            streamFluidRegistered = true;
        }
        if (!dedicated) {
            StreamParticles.register();
        }
        StreamFlowNet.registerCommon();
        if (FabricLoader.getInstance().isModLoaded("sable")) {
            SableFlowCompat.register();
        }
        Registry.register(BuiltInRegistries.FEATURE, StreamsWorldgen.RIVER_CARVER_ID, StreamsWorldgen.RIVER_CARVER);
        Registry.register(BuiltInRegistries.FEATURE, StreamsWorldgen.STREAM_VEGETATION_ID, StreamsWorldgen.STREAM_VEGETATION);
        Registry.register(BuiltInRegistries.FEATURE, StreamsWorldgen.SNOW_CLEANUP_ID, StreamsWorldgen.SNOW_CLEANUP);
        Registry.register(BuiltInRegistries.FEATURE, StreamsWorldgen.RIVER_FLOW_ID, StreamsWorldgen.RIVER_FLOW);
        Registry.register(BuiltInRegistries.FEATURE, StreamsWorldgen.WATER_SKIN_ID, StreamsWorldgen.WATER_SKIN);
        BiomeModifications.addFeature((Predicate)BiomeSelectors.foundInOverworld(), (GenerationStep.Decoration)GenerationStep.Decoration.LAKES, StreamsWorldgen.RIVER_PLACED);
        BiomeModifications.addFeature((Predicate)BiomeSelectors.foundInOverworld(), (GenerationStep.Decoration)GenerationStep.Decoration.LOCAL_MODIFICATIONS, StreamsWorldgen.WATER_SKIN_PLACED);
        BiomeModifications.addFeature((Predicate)BiomeSelectors.foundInOverworld(), (GenerationStep.Decoration)GenerationStep.Decoration.VEGETAL_DECORATION, StreamsWorldgen.STREAM_VEGETATION_PLACED);
        BiomeModifications.addFeature((Predicate)BiomeSelectors.foundInOverworld(), (GenerationStep.Decoration)GenerationStep.Decoration.TOP_LAYER_MODIFICATION, StreamsWorldgen.SNOW_CLEANUP_PLACED);
        BiomeModifications.addFeature((Predicate)BiomeSelectors.foundInOverworld(), (GenerationStep.Decoration)GenerationStep.Decoration.TOP_LAYER_MODIFICATION, StreamsWorldgen.RIVER_FLOW_PLACED);
        ServerTickEvents.END_SERVER_TICK.register(server -> StreamPrefetcher.onServerTick(server));
        ServerTickEvents.END_SERVER_TICK.register(server -> StreamFlowNet.onServerTick(server));
        if (StallWatch.ENABLED) {
            ServerTickEvents.END_SERVER_TICK.register(server -> StallWatch.beatServer());
        }
        ServerLifecycleEvents.SERVER_STARTING.register(server -> StreamsConfigFabric.load());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> RegenPending.applyAtBoot(server));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            String at;
            if (streamFluidRegistered) {
                FlowingFluidsCompat.exemptFluids(server, StreamFluids.SOURCE, StreamFluids.FLOWING);
            }
            if ((at = System.getProperty("streamsreflowing.autotrace")) != null && !at.isBlank()) {
                try {
                    String[] p = at.split(",");
                    StreamPrefetcher.autoTrace(server, Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
                }
                catch (RuntimeException ex) {
                    Constants.LOG.warn("[SR-AUTOTRACE] bad streamsreflowing.autotrace: {}", (Object)at, (Object)ex);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            StreamPrefetcher.onServerStopping();
            StreamsWorldgen.clearActive();
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> StreamsWorldgen.unloadLevel(world));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> StreamsCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (StreamsCommands.shouldWelcome(handler.player)) {
                handler.player.sendSystemMessage(StreamsCommands.welcomeMessage());
            }
        });
    }
}

