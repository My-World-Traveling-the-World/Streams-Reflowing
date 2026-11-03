/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.command;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.worldgen.RiverCarverFeature;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

public final class RegenPending {
    private RegenPending() {
    }

    private static Path pendingFile(Path worldRoot) {
        return worldRoot.resolve("streamsreflowing").resolve("regen_pending.txt");
    }

    public static void mark(ServerLevel level, int x, int z, int radiusChunks) throws IOException {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path file = RegenPending.pendingFile(root);
        Files.createDirectories(file.getParent(), new FileAttribute[0]);
        StringBuilder sb = new StringBuilder();
        if (Files.exists(file, new LinkOption[0])) {
            sb.append(Files.readString(file, StandardCharsets.UTF_8));
        }
        sb.append("trace ").append(x).append(' ').append(z).append(' ').append(8 + radiusChunks * 16).append('\n');
        int ccx = x >> 4;
        int ccz = z >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; ++dx) {
            for (int dz = -radiusChunks; dz <= radiusChunks; ++dz) {
                sb.append("chunk ").append(ccx + dx).append(' ').append(ccz + dz).append('\n');
            }
        }
        Files.writeString(file, (CharSequence)sb.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
    }

    public static void applyAtBoot(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path file = RegenPending.pendingFile(root);
        if (!Files.exists(file, new LinkOption[0])) {
            return;
        }
        ArrayList<int[]> chunks = new ArrayList<int[]>();
        int[] trace = null;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.trim().split("\\s+");
                if (p.length == 4 && p[0].equals("trace")) {
                    trace = new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])};
                    continue;
                }
                if (p.length != 3 || !p[0].equals("chunk")) continue;
                chunks.add(new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2])});
            }
        }
        catch (IOException | NumberFormatException e) {
            Constants.LOG.warn("[SR-REGEN] unreadable regen_pending.txt; ignoring", e);
            return;
        }
        int wiped = 0;
        for (String dir : new String[]{"region", "entities", "poi"}) {
            HashMap<Path, List> byRegion = new HashMap<Path, List>();
            for (int[] nArray : chunks) {
                Path mca = root.resolve(dir).resolve("r." + (nArray[0] >> 5) + "." + (nArray[1] >> 5) + ".mca");
                if (!Files.exists(mca, new LinkOption[0])) continue;
                byRegion.computeIfAbsent(mca, k -> new ArrayList()).add(nArray);
            }
            for (Map.Entry entry : byRegion.entrySet()) {
                try (RandomAccessFile raf = new RandomAccessFile(((Path)entry.getKey()).toFile(), "rw");){
                    for (int[] c : (List)entry.getValue()) {
                        int idx = (c[0] & 0x1F) + (c[1] & 0x1F) * 32;
                        raf.seek((long)idx * 4L);
                        raf.writeInt(0);
                        raf.seek(4096L + (long)idx * 4L);
                        raf.writeInt(0);
                        if (!dir.equals("region")) continue;
                        ++wiped;
                    }
                }
                catch (IOException ex) {
                    Constants.LOG.warn("[SR-REGEN] failed wiping {} -- skipped", entry.getKey(), (Object)ex);
                }
            }
        }
        if (trace != null) {
            RiverCarverFeature.armTraceOverride((int)trace[0], (int)trace[1], (int)trace[2]);
        }
        try {
            Files.delete(file);
        }
        catch (IOException iOException) {
            // empty catch block
        }
        Constants.LOG.info("[SR-REGEN] wiped {} chunk(s) for regeneration; carve trace armed{}", (Object)wiped, trace == null ? "" : " at (" + (int)trace[0] + "," + trace[1] + ") r=" + trace[2]);
    }
}

