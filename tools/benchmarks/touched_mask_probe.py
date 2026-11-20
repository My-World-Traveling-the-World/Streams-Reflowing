"""实测完整清理与生产触及掩码；同时报告稀疏和密集输入，不外推游戏加速。"""
import json
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(r"C:\Program Files\Java\jdk-21\bin")
SOURCE = ROOT / "src/main/java/dev/streamsreflowing/core/river/TouchedCellMask.java"
PROBE = r"""
package dev.streamsreflowing.core.river;
import java.util.*;
public class MaskBench {
    static volatile long sink;
    static long run(boolean old, boolean[] values, TouchedCellMask mask, List<Integer> indices) {
        long start = System.nanoTime(), sum = 0;
        for (int round = 0; round < 100; round++) {
            if (old) {
                Arrays.fill(values, false);
                for (int index : indices) values[index] = true;
                sum += values[indices.get(0)] ? 1 : 0;
            } else {
                try (var lease = mask.prepare(indices)) {
                    sum += lease.cells()[indices.get(0)] ? 1 : 0;
                }
            }
        }
        sink = sum;
        return System.nanoTime() - start;
    }
    public static void main(String[] args) {
        int size = 262144;
        for (int count : new int[]{16, 1024, 4096, 8192, size / 2, size}) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < count; i++) indices.add(i * (size / count));
            boolean[] old = new boolean[size]; var mask = new TouchedCellMask(size);
            boolean[] actual;
            try (var lease = mask.prepare(indices)) {
                actual = lease.cells();
                for (int index : indices) old[index] = true;
                if (!Arrays.equals(old, actual)) throw new AssertionError("标记不等价");
            }
            for (boolean value : actual) if (value) throw new AssertionError("清理残留");
            for (int warm = 0; warm < 5; warm++) { run(true, old, mask, indices); run(false, old, mask, indices); }
            long[] a = new long[7], b = new long[7];
            for (int i = 0; i < 7; i++) {
                if ((i & 1) == 0) { a[i] = run(true, old, mask, indices); b[i] = run(false, old, mask, indices); }
                else { b[i] = run(false, old, mask, indices); a[i] = run(true, old, mask, indices); }
            }
            System.out.println("MASK " + size + " " + count + " " + Arrays.toString(a) + " " + Arrays.toString(b));
        }
    }
}
"""

def main():
    output = ROOT / "build/reviews"
    output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="mask-bench-", dir=output) as temporary:
        work = Path(temporary).resolve()
        # 自动递归清理只能发生在本仓build内。
        assert work.is_relative_to(output.resolve())
        probe = work / "MaskBench.java"
        probe.write_text(PROBE, encoding="utf-8")
        subprocess.run([str(JAVA / "javac.exe"), "-encoding", "UTF-8", "-d", str(work / "classes"),
                        str(SOURCE), str(probe)], check=True, capture_output=True)
        run = subprocess.run([str(JAVA / "java.exe"), "-Xms512m", "-Xmx512m", "-cp",
                              str(work / "classes"), "dev.streamsreflowing.core.river.MaskBench"],
                             check=True, capture_output=True, text=True)
    result = {"scope": "isolated production mask; excludes sampler and lake flood; no game speed claim",
              "rounds_per_sample": 100, "warmups": 5, "samples": 7, "stdout": run.stdout.splitlines()}
    data = json.dumps(result, indent=2) + "\n"
    (output / "touched-mask-benchmark.json").write_text(data, encoding="utf-8", newline="\n")
    print(data, end="")

if __name__ == "__main__":
    main()
