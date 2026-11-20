package dev.streamsreflowing.core.river;

import java.util.Arrays;
import java.util.List;

/** 一次洪泛借用一轮掩码；只清本轮触及单元，避免每个小湖都扫完整板块。 */
final class TouchedCellMask implements AutoCloseable {
    private final boolean[] cells;
    private int[] touched;
    private int count;
    private boolean active;
    private boolean dense;

    TouchedCellMask(int size) {
        cells = new boolean[size];
        touched = new int[Math.min(64, size)];
    }

    TouchedCellMask prepare(List<Integer> indices) {
        if (active) throw new IllegalStateException("细湖掩码不能重入");
        active = true;
        // 实测密集输入记录索引更贵；只在稀疏候选维护触及表，密集候选交给批量清零。
        dense = indices.size() > cells.length / 64;
        try {
            if (dense) {
                for (int index : indices) cells[index] = true;
                return this;
            }
            for (int index : indices) {
                if (cells[index]) continue;
                if (count == touched.length)
                    touched = Arrays.copyOf(touched, Math.min(cells.length, Math.max(1, count * 2)));
                touched[count++] = index;
                cells[index] = true;
            }
            return this;
        } catch (RuntimeException | Error failure) {
            // 部分标记失败也必须归还，否则下一个候选会读到上一个湖的残留。
            close();
            throw failure;
        }
    }

    boolean[] cells() {
        if (!active) throw new IllegalStateException("细湖掩码尚未借用");
        return cells;
    }

    @Override public void close() {
        if (dense) Arrays.fill(cells, false);
        else for (int i = 0; i < count; i++) cells[touched[i]] = false;
        count = 0;
        active = false;
        dense = false;
    }
}
