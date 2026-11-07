package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.Constants;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * 模组集指纹：把当前加载的全部模组（id + 版本）排序后做 FNV-1a，得到一枚稳定的 long。
 *
 * <p>它进 {@link FileNetworkStore} 的失效指纹：模组增删、升版、换分支都会让磁盘上的
 * {@code saves/<world>/streamsreflowing/<dimension>/r.*.srn} 整份失效并重算。这样做的理由是
 * 河流网络的地形采样依赖地形噪声、生物群系与世界生成注册表 —— 这些东西随模组集变化，
 * 而 {@link dev.streamsreflowing.core.config.StreamsConfig} 只覆盖本模组自己的参数。</p>
 *
 * <p>只做 id + 版本，不含绝对路径/加载顺序，保证同一套模组在不同机器上得到同一个值；
 * 同一个进程内只算一次。</p>
 */
public final class ModSetFingerprint {
    private static volatile boolean computed;
    private static volatile long value;

    private ModSetFingerprint() {
    }

    /** 当前模组集的指纹（进程内缓存）。 */
    public static long current() {
        if (computed) {
            return value;
        }
        synchronized (ModSetFingerprint.class) {
            if (!computed) {
                value = of(loadedModIds());
                computed = true;
                Constants.LOG.debug("Streams mod-set fingerprint = {}", value);
            }
            return value;
        }
    }

    private static List<String> loadedModIds() {
        List<String> ids = new ArrayList<>();
        try {
            for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
                ids.add(container.getMetadata().getId() + "@"
                        + container.getMetadata().getVersion().getFriendlyString());
            }
        } catch (Throwable failure) {
            // 加载器不可用（例如脱离加载器的单元测试）：退化成空集常量，绝不抛。
            Constants.LOG.debug("Streams mod-set fingerprint unavailable ({})", failure.toString());
        }
        return ids;
    }

    /**
     * 纯函数形态：同样的模组集合（与顺序无关）得到同样的指纹；任何一条 id/版本变化都会改变它。
     * 包内可见，供单测直接覆盖，不需要加载器运行时。
     */
    static long of(Iterable<String> modIdAndVersion) {
        List<String> sorted = new ArrayList<>();
        for (String entry : modIdAndVersion) {
            if (entry != null) {
                sorted.add(entry);
            }
        }
        sorted.sort(null);
        long hash = 1469598103934665603L;
        for (String entry : sorted) {
            for (int i = 0; i < entry.length(); i++) {
                hash = (hash ^ entry.charAt(i)) * 1099511628211L;
            }
            hash = (hash ^ '\n') * 1099511628211L;
        }
        return hash;
    }
}
