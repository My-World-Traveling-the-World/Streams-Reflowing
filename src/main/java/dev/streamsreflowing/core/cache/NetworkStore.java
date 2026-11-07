/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;

public interface NetworkStore {
    public RiverNetwork load(PlateKey var1);

    public void save(PlateKey var1, RiverNetwork var2);

    /**
     * 该 plate 是否已经有一份「通过校验」的磁盘产物（只看文件头，不解析内容、不做任何构建）。
     *
     * <p>Streams 启动预热用它区分两种「已就绪」：本局内存里建好的板，和上一局落盘、
     * 本局可以直接读回来复用的板。默认返回 {@code false}：不认识磁盘的 store 一律按
     * 未命中处理，行为与引入本方法之前完全一致。</p>
     */
    default boolean isStored(PlateKey plateKey) {
        return false;
    }
}
