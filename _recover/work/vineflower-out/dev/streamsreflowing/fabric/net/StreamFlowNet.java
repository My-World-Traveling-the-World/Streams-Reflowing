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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
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
   private static final Map<UUID, StreamFlowNet.PlayerFlow> STATES = new ConcurrentHashMap<>();
   private static final int GRID_READY_MARGIN = 24;

   private StreamFlowNet() {
   }

   public static void registerCommon() {
      PayloadTypeRegistry.playS2C().register(StreamFlowNet.FlowGridPayload.TYPE, StreamFlowNet.FlowGridPayload.CODEC);
   }

   private static long packKey(int x, int z) {
      return (long)x << 32 | (long)z & 4294967295L;
   }

   static boolean canReceive(ServerPlayer player) {
      return ServerPlayNetworking.canSend(player, StreamFlowNet.FlowGridPayload.TYPE);
   }

   static void sendGrid(ServerPlayer player, int cx, int cz, byte[] grid) {
      if (ServerPlayNetworking.canSend(player, StreamFlowNet.FlowGridPayload.TYPE)) {
         ServerPlayNetworking.send(player, new StreamFlowNet.FlowGridPayload(cx, cz, grid));
      }
   }

   public static void onServerTick(MinecraftServer server) {
      if (StreamsConfigFabric.vanillaWaterFlow() && (Services.PLATFORM.generateRivers() || Services.PLATFORM.flowVanillaRivers())) {
         ServerLevel level = server.overworld();
         RiverEngine engine = StreamsWorldgen.engine(level);
         if (engine != null) {
            if (++scanThrottle >= 10) {
               scanThrottle = 0;
               RiverFlowField.BiomeClass classifier = RiverFlowFields.classifierFor(level);
               RiverFlowField.OrientationField orient = RiverFlowFields.orientationFor(level);
               boolean flowRivers = StreamsConfigFabric.flowVanillaRivers();
               int view = Mth.clamp(server.getPlayerList().getViewDistance(), 2, 16);
               Set<UUID> present = new HashSet<>();

               for (ServerPlayer player : level.players()) {
                  present.add(player.getUUID());
                  scanPlayer(level, engine, classifier, orient, flowRivers, view, player);
               }

               STATES.keySet().retainAll(present);
            }
         }
      }
   }

   private static void scanPlayer(
      ServerLevel level,
      RiverEngine engine,
      RiverFlowField.BiomeClass classifier,
      RiverFlowField.OrientationField orient,
      boolean flowRivers,
      int view,
      ServerPlayer player
   ) {
      int cx = player.getBlockX() >> 4;
      int cz = player.getBlockZ() >> 4;
      StreamFlowNet.PlayerFlow st = STATES.computeIfAbsent(player.getUUID(), u -> new StreamFlowNet.PlayerFlow());
      int keep = view + 2;
      Iterator<Long> it = st.sent.iterator();

      while (it.hasNext()) {
         long k = it.next();
         if (Math.abs((int)(k >> 32) - cx) > keep || Math.abs((int)k - cz) > keep) {
            it.remove();
         }
      }

      for (int dx = -view; dx <= view; dx++) {
         for (int dz = -view; dz <= view; dz++) {
            int x = cx + dx;
            int z = cz + dz;
            if (!st.sent.contains(packKey(x, z))) {
               deliver(player, st, level, engine, classifier, orient, flowRivers, x, z);
            }
         }
      }
   }

   private static void deliver(
      ServerPlayer player,
      StreamFlowNet.PlayerFlow st,
      ServerLevel level,
      RiverEngine engine,
      RiverFlowField.BiomeClass classifier,
      RiverFlowField.OrientationField orient,
      boolean flowRivers,
      int cx,
      int cz
   ) {
      if (canReceive(player)) {
         long key = packKey(cx, cz);
         byte[] cached = StreamFlowGrid.peek(cx, cz);
         if (cached != null) {
            st.sent.add(key);
            if (cached.length > 0) {
               sendGrid(player, cx, cz, cached);
            }
         } else {
            StreamPrefetcher.submitGridBuild(level, engine, classifier, orient, flowRivers, cx, cz, true, () -> {
               byte[] grid = StreamFlowGrid.peek(cx, cz);
               if (grid != null && grid.length != 0) {
                  level.getServer().execute(() -> {
                     for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(cx, cz), false)) {
                        sendGrid(watcher, cx, cz, grid);
                     }
                  });
               }
            });
         }
      }
   }

   public static void onClientChunkUnload(int cx, int cz) {
      ClientFlowGrids.remove(cx, cz);
   }

   public static void onChunkSentToPlayer(ServerPlayer player, ServerLevel level, int cx, int cz) {
      if (StreamsConfigFabric.vanillaWaterFlow() && (Services.PLATFORM.generateRivers() || Services.PLATFORM.flowVanillaRivers())) {
         if (level.dimension().equals(Level.OVERWORLD)) {
            RiverEngine engine = StreamsWorldgen.engine(level);
            if (engine != null) {
               StreamFlowNet.PlayerFlow st = STATES.computeIfAbsent(player.getUUID(), u -> new StreamFlowNet.PlayerFlow());
               if (!st.sent.contains(packKey(cx, cz))) {
                  deliver(
                     player,
                     st,
                     level,
                     engine,
                     RiverFlowFields.classifierFor(level),
                     RiverFlowFields.orientationFor(level),
                     StreamsConfigFabric.flowVanillaRivers(),
                     cx,
                     cz
                  );
               }
            }
         }
      }
   }

   public static record FlowGridPayload(int cx, int cz, byte[] grid) implements CustomPacketPayload {
      public static final Type<StreamFlowNet.FlowGridPayload> TYPE = new Type<>(StreamFlowNet.FLOW_ID);
      public static final StreamCodec<RegistryFriendlyByteBuf, StreamFlowNet.FlowGridPayload> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeVarInt(p.cx());
         buf.writeVarInt(p.cz());
         buf.writeByteArray(p.grid());
      }, buf -> new StreamFlowNet.FlowGridPayload(buf.readVarInt(), buf.readVarInt(), buf.readByteArray()));

      @Override
      public Type<StreamFlowNet.FlowGridPayload> type() {
         return TYPE;
      }
   }

   private static final class PlayerFlow {
      final Set<Long> sent = new HashSet<>();
   }
}
