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
import java.util.concurrent.ConcurrentHashMap;

public final class FileNetworkStore
implements NetworkStore {
    private static final int a = 1397902897;
    /**
     * 文件版本号。数字一变，上一版写出的所有 {@code r.<cx>.<cz>.srn} 都会在文件头校验处被拒收
     * 并按「未命中」重建 —— 改布局、改指纹口径（世界种子 / plateScale / 采样步长 / 模组集）
     * 时必须同步 +1。
     */
    private static final int b = 29;
    private static final int c = 20000000;
    private static final int d = 1000000;
    private static final int e = 4000000;
    private final Path f;
    private final StreamsConfig g;
    private final long h;
    private volatile boolean i;
    /**
     * 模组集指纹盐：模组增删/升版即整份 plate 缓存失效。由调用方（StreamsWorldgen）用
     * {@link ModSetFingerprint} 算好后传进来，本类自身不依赖加载器 API。
     */
    private final long modSetSalt;
    /**
     * 已确认存在可用缓存的 plate。load/save 双向维护；{@link #isStored} 只在未命中记忆时
     * 才去读 16 字节文件头，避免预热每块板都 stat 一次。
     */
    private final ConcurrentHashMap<PlateKey, Boolean> storedKeys = new ConcurrentHashMap<>();
    /** 首次遇到文件头不匹配时只告警一次，避免一次预热刷 144 行同样的日志。 */
    private volatile boolean mismatchReported;

    public FileNetworkStore(Path path, long l, StreamsConfig streamsConfig) {
        this(path, l, streamsConfig, 0L);
    }

    public FileNetworkStore(Path path, long l, StreamsConfig streamsConfig, long modSetSalt) {
        this.f = path;
        this.g = streamsConfig;
        this.modSetSalt = modSetSalt;
        this.h = FileNetworkStore.a(l, streamsConfig, modSetSalt);
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
            if (dataInputStream.readInt() != 1397902897 || dataInputStream.readInt() != b || dataInputStream.readLong() != this.h) {
                // 版本号 / 世界种子 / plateScale / 采样步长 / 模组集任一不同：整份丢弃重算。
                this.storedKeys.remove(plateKey);
                this.reportMismatch(plateKey);
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
                    return null;
                }
                lArray = new long[n10];
                for (int j = 0; j < n10; ++j) {
                    lArray[j] = dataInputStream.readLong();
                }
                arrayList2.add(new Lake(n3, n4, n5, n6, n7, n8, n9, lArray));
            }
            RiverNetwork riverNetwork = NetworkTracer.assemble(plateKey, arrayList, arrayList2, this.g);
            NetworkTracer.dumpLoadedSegments(plateKey, arrayList, arrayList2);
            this.storedKeys.put(plateKey, Boolean.TRUE);
            Constants.LOG.debug("Loaded river region {} from cache ({} segments, {} lakes)", plateKey, n2, n);
            RiverNetwork riverNetwork2 = riverNetwork;
            return riverNetwork2;
        }
        catch (IOException | RuntimeException exception) {
            // 截断/损坏：这一份不能再算「有缓存」，否则预热会把它记成已就绪。
            this.storedKeys.remove(plateKey);
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
                dataOutputStream.writeInt(b);
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
            this.storedKeys.put(plateKey, Boolean.TRUE);
        }
        catch (IOException | RuntimeException exception) {
            Constants.LOG.warn("Failed to persist river region {} ({})", (Object)plateKey, (Object)exception.toString());
        }
    }

    /**
     * 该 plate 是否已有可用产物。命中过 load/save 的直接答 yes；否则只读 16 字节文件头
     * （magic + 版本号 + 指纹）判断，绝不解析段/湖内容，也绝不构建网络。
     */
    @Override
    public boolean isStored(PlateKey plateKey) {
        Boolean known = this.storedKeys.get(plateKey);
        if (known != null) {
            return known.booleanValue();
        }
        boolean stored = this.headerMatches(plateKey);
        this.storedKeys.put(plateKey, stored);
        return stored;
    }

    /** 文件头是否与本次会话的镜像口径一致：magic、版本号、指纹（种子/配置/模组集）。 */
    private boolean headerMatches(PlateKey plateKey) {
        Path path = this.f.resolve(FileNetworkStore.a(plateKey));
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return false;
        }
        try (DataInputStream dataInputStream = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path, new OpenOption[0])))) {
            return dataInputStream.readInt() == 1397902897
                    && dataInputStream.readInt() == b
                    && dataInputStream.readLong() == this.h;
        } catch (IOException | RuntimeException exception) {
            // 截断/损坏：安全降级为未命中，绝不向外抛。
            return false;
        }
    }

    private void reportMismatch(PlateKey plateKey) {
        if (this.mismatchReported) {
            return;
        }
        this.mismatchReported = true;
        Constants.LOG.info("Streams river plate cache ignored for {}: file version or fingerprint "
                + "(world seed / plate scale / sampling step / mod set) differs from this session; "
                + "cached plates will be rebuilt.", plateKey);
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

    /**
     * 整份 plate 缓存的失效指纹（FNV-1a）。
     *
     * <p>参与运算的每一项变化都会让磁盘上所有 {@code r.<cx>.<cz>.srn} 被文件头校验拒收：
     * 文件版本号 b、世界种子 l、模组集盐 modSetSalt，以及下面覆盖 plateScale / 采样步长
     * （terrainSampleSpacing）/ 格距 / 各种宽度与湖参的全部 StreamsConfig 字段。</p>
     */
    private static long a(long l, StreamsConfig streamsConfig, long modSetSalt) {
        long l2 = 1469598103934665603L;
        l2 = FileNetworkStore.a(l2, b);
        l2 = FileNetworkStore.a(l2, l);
        l2 = FileNetworkStore.a(l2, modSetSalt);
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
        // 下面这些也会改网络几何（河深/河岸坡度与噪声/沟内缩/湖参数/跨板连接），
        // 漏掉任何一项都会让旧缓存被误判成仍然有效，故一律入指纹。
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.streamDepth()));
        l2 = FileNetworkStore.a(l2, streamsConfig.stepDeltaY());
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.bankWidth()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.bankSlope()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.bankNoise()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.bankSteepFactor()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.ditchInset()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.aRiver()));
        l2 = FileNetworkStore.a(l2, streamsConfig.connectNearbyWater() ? 1L : 0L);
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.connectRadius()));
        l2 = FileNetworkStore.a(l2, streamsConfig.containmentBerm() ? 1L : 0L);
        l2 = FileNetworkStore.a(l2, streamsConfig.minSeaChainNodes());
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.lakeMinArea()));
        l2 = FileNetworkStore.a(l2, Double.doubleToLongBits(streamsConfig.lakeMaxArea()));
        l2 = FileNetworkStore.a(l2, streamsConfig.outletBiomes() == null ? 0L : (long)streamsConfig.outletBiomes().hashCode());
        return l2;
    }

    private static long a(long l, long l2) {
        return (l ^ l2) * 1099511628211L;
    }
}

