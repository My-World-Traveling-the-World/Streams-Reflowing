/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.flow;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.flow.RawFlowTiles;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class RawFlowTileStore {
    private static final int MAGIC = 1397904433;
    private static final int VERSION = 1;
    private static final int REGION_CHUNKS = 32;
    private static final int PER_REGION = 1024;
    private static volatile Path storeDir;
    private static volatile long fingerprint;
    private static final Map<Long, Region> REGIONS;

    private RawFlowTileStore() {
    }

    public static void initStore(Path dir, long fp) {
        storeDir = dir;
        fingerprint = fp;
        REGIONS.clear();
    }

    private static boolean enabled() {
        return storeDir != null;
    }

    private static long regionKey(int rx, int rz) {
        return (long)rx << 32 ^ (long)rz & 0xFFFFFFFFL;
    }

    private static int slot(int cx, int cz) {
        return Math.floorMod(cz, 32) * 32 + Math.floorMod(cx, 32);
    }

    public static RawFlowTiles.Tile load(int cx, int cz) {
        if (!RawFlowTileStore.enabled()) {
            return null;
        }
        int rx = Math.floorDiv(cx, 32);
        int rz = Math.floorDiv(cz, 32);
        Region region = REGIONS.computeIfAbsent(RawFlowTileStore.regionKey(rx, rz), k -> RawFlowTileStore.readRegion(rx, rz));
        return region.tiles[RawFlowTileStore.slot(cx, cz)];
    }

    public static void store(int cx, int cz, RawFlowTiles.Tile tile) {
        if (!RawFlowTileStore.enabled()) {
            return;
        }
        int rx = Math.floorDiv(cx, 32);
        int rz = Math.floorDiv(cz, 32);
        Region region = REGIONS.computeIfAbsent(RawFlowTileStore.regionKey(rx, rz), k -> RawFlowTileStore.readRegion(rx, rz));
        region.tiles[RawFlowTileStore.slot((int)cx, (int)cz)] = tile;
        region.dirty = true;
    }

    private static Path fileFor(int rx, int rz) {
        return storeDir.resolve("t." + rx + "." + rz + ".srt");
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Region readRegion(int rx, int rz) {
        Region region = new Region();
        Path f = RawFlowTileStore.fileFor(rx, rz);
        if (!Files.isRegularFile(f, new LinkOption[0])) {
            return region;
        }
        try (InputStream raw = Files.newInputStream(f, new OpenOption[0]);
             DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(raw)));){
            if (in.readInt() != 1397904433 || in.readInt() != 1 || in.readLong() != fingerprint) {
                Region region2 = region;
                return region2;
            }
            byte[] present = new byte[128];
            in.readFully(present);
            int i = 0;
            while (i < 1024) {
                if ((present[i >> 3] & 1 << (i & 7)) != 0) {
                    byte[] dirX = new byte[256];
                    byte[] dirZ = new byte[256];
                    short[] surfaceY = new short[256];
                    in.readFully(dirX);
                    in.readFully(dirZ);
                    for (int c = 0; c < 256; ++c) {
                        surfaceY[c] = in.readShort();
                    }
                    region.tiles[i] = new RawFlowTiles.Tile(dirX, dirZ, surfaceY);
                }
                ++i;
            }
            return region;
        }
        catch (IOException | RuntimeException e) {
            Constants.LOG.debug("Flow-tile region ({},{}) unreadable, rebuilding: {}", rx, rz, e.toString());
            return new Region();
        }
    }

    public static void flush() {
        if (!RawFlowTileStore.enabled()) {
            return;
        }
        for (Map.Entry<Long, Region> e : REGIONS.entrySet()) {
            Region region = e.getValue();
            if (!region.dirty) continue;
            region.dirty = false;
            long k = e.getKey();
            RawFlowTileStore.writeRegion((int)(k >> 32), (int)k, region);
        }
    }

    private static void writeRegion(int rx, int rz, Region region) {
        try {
            Files.createDirectories(storeDir, new FileAttribute[0]);
            byte[] present = new byte[128];
            int count = 0;
            for (int i = 0; i < 1024; ++i) {
                if (region.tiles[i] == null || !RawFlowTileStore.hasFlow(region.tiles[i])) continue;
                int n = i >> 3;
                present[n] = (byte)(present[n] | (byte)(1 << (i & 7)));
                ++count;
            }
            Path tmp = RawFlowTileStore.fileFor(rx, rz).resolveSibling(String.valueOf(RawFlowTileStore.fileFor(rx, rz).getFileName()) + ".tmp");
            try (OutputStream raw = Files.newOutputStream(tmp, new OpenOption[0]);
                 DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(raw)));){
                out.writeInt(1397904433);
                out.writeInt(1);
                out.writeLong(fingerprint);
                out.write(present);
                for (int i = 0; i < 1024; ++i) {
                    if ((present[i >> 3] & 1 << (i & 7)) == 0) continue;
                    RawFlowTiles.Tile t = region.tiles[i];
                    out.write(t.dirX());
                    out.write(t.dirZ());
                    for (short s : t.surfaceY()) {
                        out.writeShort(s);
                    }
                }
            }
            Files.move(tmp, RawFlowTileStore.fileFor(rx, rz), StandardCopyOption.REPLACE_EXISTING);
            Constants.LOG.debug("Saved flow-tile region ({},{}): {} chunks with flow", rx, rz, count);
        }
        catch (IOException | RuntimeException e) {
            Constants.LOG.debug("Could not save flow-tile region ({},{}): {}", rx, rz, e.toString());
        }
    }

    private static boolean hasFlow(RawFlowTiles.Tile t) {
        for (int i = 0; i < t.dirX().length; ++i) {
            if (t.dirX()[i] == 0 && t.dirZ()[i] == 0) continue;
            return true;
        }
        return false;
    }

    public static void close() {
        RawFlowTileStore.flush();
        REGIONS.clear();
        storeDir = null;
    }

    static {
        REGIONS = new ConcurrentHashMap<Long, Region>();
    }

    private static final class Region {
        final RawFlowTiles.Tile[] tiles = new RawFlowTiles.Tile[1024];
        volatile boolean dirty;

        private Region() {
        }
    }
}

