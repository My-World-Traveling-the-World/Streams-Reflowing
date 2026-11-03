/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.net;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamPrefetcher;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class StreamFlowNet {
    public static final ResourceLocation FLOW_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "flow_grid");
    private static final int SCAN_RADIUS_CAP = 16;
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static int scanThrottle = 0;
    private static final Map<UUID, PlayerFlow> STATES = new ConcurrentHashMap<UUID, PlayerFlow>();
    private static final int GRID_READY_MARGIN = 24;

    private StreamFlowNet() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(FlowGridPayload.TYPE, FlowGridPayload.CODEC);
    }

    private static long packKey(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }

    static boolean canReceive(ServerPlayer player) {
        return ServerPlayNetworking.canSend((ServerPlayer)player, FlowGridPayload.TYPE);
    }

    static void sendGrid(ServerPlayer player, int cx, int cz, byte[] grid) {
        if (ServerPlayNetworking.canSend((ServerPlayer)player, FlowGridPayload.TYPE)) {
            ServerPlayNetworking.send((ServerPlayer)player, (CustomPacketPayload)new FlowGridPayload(cx, cz, grid));
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (!StreamsConfigFabric.vanillaWaterFlow() || !Services.PLATFORM.generateRivers() && !Services.PLATFORM.flowVanillaRivers()) {
            return;
        }
        ServerLevel level = server.overworld();
        RiverEngine engine = StreamsWorldgen.engine(level);
        if (engine == null) {
            return;
        }
        if (++scanThrottle < 10) {
            return;
        }
        scanThrottle = 0;
        RiverFlowField.BiomeClass classifier = RiverFlowFields.classifierFor(level);
        RiverFlowField.OrientationField orient = RiverFlowFields.orientationFor(level);
        boolean flowRivers = StreamsConfigFabric.flowVanillaRivers();
        int view = Mth.clamp(server.getPlayerList().getViewDistance(), 2, 16);
        HashSet<UUID> present = new HashSet<UUID>();
        for (ServerPlayer player : level.players()) {
            present.add(player.getUUID());
            StreamFlowNet.scanPlayer(level, engine, classifier, orient, flowRivers, view, player);
        }
        STATES.keySet().retainAll(present);
    }

    private static void scanPlayer(ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, boolean flowRivers, int view, ServerPlayer player) {
        int cx = player.getBlockX() >> 4;
        int cz = player.getBlockZ() >> 4;
        PlayerFlow st = STATES.computeIfAbsent(player.getUUID(), u -> new PlayerFlow());
        int keep = view + 2;
        Iterator<Long> it = st.sent.iterator();
        while (it.hasNext()) {
            long k = it.next();
            if (Math.abs((int)(k >> 32) - cx) <= keep && Math.abs((int)k - cz) <= keep) continue;
            it.remove();
        }
        for (int dx = -view; dx <= view; ++dx) {
            for (int dz = -view; dz <= view; ++dz) {
                int x = cx + dx;
                int z = cz + dz;
                if (st.sent.contains(StreamFlowNet.packKey(x, z))) continue;
                StreamFlowNet.deliver(player, st, level, engine, classifier, orient, flowRivers, x, z);
            }
        }
    }

    private static void deliver(ServerPlayer player, PlayerFlow st, ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier, RiverFlowField.OrientationField orient, boolean flowRivers, int cx, int cz) {
        if (!StreamFlowNet.canReceive(player)) {
            return;
        }
        long key = StreamFlowNet.packKey(cx, cz);
        byte[] cached = StreamFlowGrid.peek(cx, cz);
        if (cached != null) {
            st.sent.add(key);
            if (cached.length > 0) {
                StreamFlowNet.sendGrid(player, cx, cz, cached);
            }
            return;
        }
        StreamPrefetcher.submitGridBuild(level, engine, classifier, orient, flowRivers, cx, cz, true, () -> {
            byte[] grid = StreamFlowGrid.peek(cx, cz);
            if (grid == null || grid.length == 0) {
                return;
            }
            level.getServer().execute(() -> {
                for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(cx, cz), false)) {
                    StreamFlowNet.sendGrid(watcher, cx, cz, grid);
                }
            });
        });
    }

    public static void onClientChunkUnload(int cx, int cz) {
        ClientFlowGrids.remove(cx, cz);
    }

    public static void onChunkSentToPlayer(ServerPlayer player, ServerLevel level, int cx, int cz) {
        if (!StreamsConfigFabric.vanillaWaterFlow() || !Services.PLATFORM.generateRivers() && !Services.PLATFORM.flowVanillaRivers()) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        RiverEngine engine = StreamsWorldgen.engine(level);
        if (engine == null) {
            return;
        }
        PlayerFlow st = STATES.computeIfAbsent(player.getUUID(), u -> new PlayerFlow());
        if (st.sent.contains(StreamFlowNet.packKey(cx, cz))) {
            return;
        }
        StreamFlowNet.deliver(player, st, level, engine, RiverFlowFields.classifierFor(level), RiverFlowFields.orientationFor(level), StreamsConfigFabric.flowVanillaRivers(), cx, cz);
    }

    public record FlowGridPayload(int cx, int cz, byte[] grid) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<FlowGridPayload> TYPE = new CustomPacketPayload.Type(FLOW_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, FlowGridPayload> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.cx());
            buf.writeVarInt(p.cz());
            buf.writeByteArray(p.grid());
        }, buf -> new FlowGridPayload(buf.readVarInt(), buf.readVarInt(), buf.readByteArray()));

        public CustomPacketPayload.Type<FlowGridPayload> type() {
            return TYPE;
        }
    }

    private static final class PlayerFlow {
        final Set<Long> sent = new HashSet<Long>();

        private PlayerFlow() {
        }
    }
}

