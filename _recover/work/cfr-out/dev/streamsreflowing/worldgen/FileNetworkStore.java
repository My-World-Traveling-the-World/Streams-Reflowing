/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.core.cache.NetworkStore;
import dev.streamsreflowing.core.config.StreamsConfig;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.Lake;
import dev.streamsreflowing.core.river.NetworkTracer;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSegment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FileNetworkStore
implements NetworkStore {
    private static final int a = 1397902897;
    private static final int b = 28;
    private static final int c = 20000000;
    private static final int d = 1000000;
    private static final int e = 4000000;
    private final Path f;
    private final StreamsConfig g;
    private final long h;
    private volatile boolean i;

    public FileNetworkStore(Path path, long l, StreamsConfig streamsConfig) {
        this.f = path;
        this.g = streamsConfig;
        this.h = FileNetworkStore.a(l, streamsConfig);
    }

    public long fingerprint() {
        return this.h;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public RiverNetwork load(PlateKey plateKey) {
        Path path = this.f.resolve(FileNetworkStore.a(plateKey));
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return null;
        }
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(Files.newInputStream(path, new OpenOption[0])));){
            int n;
            if (dataInputStream.readInt() != 1397902897 || dataInputStream.readInt() != 28 || dataInputStream.readLong() != this.h) {
                RiverNetwork riverNetwork = null;
                return riverNetwork;
            }
            int n2 = dataInputStream.readInt();
            if (n2 < 0 || n2 > 20000000) {
                RiverNetwork riverNetwork = null;
                return riverNetwork;
            }
            ArrayList<RiverSegment> arrayList = new ArrayList<RiverSegment>(n2);
            for (n = 0; n < n2; ++n) {
                arrayList.add(new RiverSegment(dataInputStream.readDouble(), dataInputStream.readDouble(), dataInputStream.readDouble(), dataInputStream.readDouble(), dataInputStream.readDouble(), dataInputStream.readDouble(), dataInputStream.readDouble(), dataInputStream.readDouble()));
            }
            n = dataInputStream.readInt();
            if (n < 0 || n > 1000000) {
                RiverNetwork riverNetwork = null;
                return riverNetwork;
            }
            ArrayList<Lake> arrayList2 = new ArrayList<Lake>(n);
            for (int i = 0; i < n; ++i) {
                long[] lArray;
                int n3 = dataInputStream.readInt();
                int n4 = dataInputStream.readInt();
                int n5 = dataInputStream.readInt();
                int n6 = dataInputStream.readInt();
                int n7 = dataInputStream.readInt();
                int n8 = dataInputStream.readInt();
                int n9 = dataInputStream.readInt();
                int n10 = dataInputStream.readInt();
                if (n10 < 0 || n10 > 4000000) {
                    lArray = null;
                    return lArray;
                }
                lArray = new long[n10];
                for (int j = 0; j < n10; ++j) {
                    lArray[j] = dataInputStream.readLong();
                }
                arrayList2.add(new Lake(n3, n4, n5, n6, n7, n8, n9, lArray));
            }
            RiverNetwork riverNetwork = NetworkTracer.assemble(plateKey, arrayList, arrayList2, this.g);
            NetworkTracer.dumpLoadedSegments(plateKey, arrayList, arrayList2);
            Constants.LOG.debug("Loaded river region {} from cache ({} segments, {} lakes)", plateKey, n2, n);
            RiverNetwork riverNetwork2 = riverNetwork;
            return riverNetwork2;
        }
        catch (IOException | RuntimeException exception) {
            Constants.LOG.warn("Failed to read cached river region {} ({}); rebuilding", (Object)plateKey, (Object)exception.toString());
            return null;
        }
    }

    @Override
    public void save(PlateKey plateKey, RiverNetwork riverNetwork) {
        try {
            this.a();
            Path path = this.f.resolve(FileNetworkStore.a(plateKey));
            Path path2 = this.f.resolve(FileNetworkStore.a(plateKey) + ".tmp");
            List<RiverSegment> list = riverNetwork.segments();
            try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path2, new OpenOption[0])));){
                dataOutputStream.writeInt(1397902897);
                dataOutputStream.writeInt(28);
                dataOutputStream.writeLong(this.h);
                dataOutputStream.writeInt(list.size());
                for (RiverSegment object2 : list) {
                    dataOutputStream.writeDouble(object2.x1());
                    dataOutputStream.writeDouble(object2.z1());
                    dataOutputStream.writeDouble(object2.y1());
                    dataOutputStream.writeDouble(object2.w1());
                    dataOutputStream.writeDouble(object2.x2());
                    dataOutputStream.writeDouble(object2.z2());
                    dataOutputStream.writeDouble(object2.y2());
                    dataOutputStream.writeDouble(object2.w2());
                }
                List<Lake> list2 = riverNetwork.lakes();
                dataOutputStream.writeInt(list2.size());
                Iterator iterator = list2.iterator();
                while (iterator.hasNext()) {
                    Lake lake = (Lake)iterator.next();
                    dataOutputStream.writeInt(lake.level());
                    dataOutputStream.writeInt(lake.digDepth());
                    dataOutputStream.writeInt(lake.spacing());
                    dataOutputStream.writeInt(lake.cellMinX());
                    dataOutputStream.writeInt(lake.cellMinZ());
                    dataOutputStream.writeInt(lake.cellsWide());
                    dataOutputStream.writeInt(lake.cellsHigh());
                    long[] lArray = lake.mask();
                    dataOutputStream.writeInt(lArray.length);
                    for (long l : lArray) {
                        dataOutputStream.writeLong(l);
                    }
                }
            }
            Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException | RuntimeException exception) {
            Constants.LOG.warn("Failed to persist river region {} ({})", (Object)plateKey, (Object)exception.toString());
        }
    }

    private void a() throws IOException {
        if (!this.i) {
            Files.createDirectories(this.f, new FileAttribute[0]);
            this.i = true;
        }
    }

    private static String a(PlateKey plateKey) {
        return "r." + plateKey.cx() + "." + plateKey.cz() + ".srn";
    }

    private static long a(long l, StreamsConfig streamsConfig) {
        long l2 = 1469598103934665603L;
        l2 = FileNetworkStore.a(l2, l);
        l2 = FileNetworkStore.a(l2, streamsConfig.seaLevel());
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.plateScale()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.plateJitter()));
        l2 = FileNetworkStore.a(l2, streamsConfig.gridSpacing());
        l2 = FileNetworkStore.a(l2, streamsConfig.terrainSampleSpacing());
        l2 = FileNetworkStore.a(l2, streamsConfig.valleySnapRadius());
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.minRiverAccumulation()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.wetnessMultiplier()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.baseWidth()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.maxWidth()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.widthScale()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.borderMargin()));
        l2 = FileNetworkStore.a(l2, streamsConfig.smoothingPasses());
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.meanderStrength()));
        l2 = FileNetworkStore.a(l2, streamsConfig.seaLevelExtension());
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.reconnectRadius()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.wetnessDryCutoff()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.wetnessWetReference()));
        l2 = FileNetworkStore.a(l2, streamsConfig.sinkRooted() ? 1L : 0L);
        l2 = FileNetworkStore.a(l2, streamsConfig.growFromWater() ? 1L : 0L);
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.growDensity()));
        l2 = FileNetworkStore.a(l2, streamsConfig.confineToBanks() ? 1L : 0L);
        l2 = FileNetworkStore.a(l2, streamsConfig.outletBiomes() == null ? 0L : (long)streamsConfig.outletBiomes().hashCode());
        return l2;
    }

    private static long a(long l, long l2) {
        return (l ^ l2) * 1099511628211L;
    }
}

