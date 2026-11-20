"""验证修复后的生产会话和缓存边界；历史缺陷输出保留在docs/evidence。"""

import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "src/main/java"
JAVA_HOME = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Java\jdk-21"))

# 只替代缓存签名中的引擎类型；不伪造任何缓存或进度实现。
STUB = """package dev.streamsreflowing.core;
public final class RiverEngine {}
"""

PROBE = """
package dev.streamsreflowing.compat.streamscontrol;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.compose.ColumnPlan;
import dev.streamsreflowing.worldgen.ChunkPlanCache;
import dev.streamsreflowing.compat.streamscontrol.StreamsStartupProgress;

public final class StartupCacheProbe {
    public static void main(String[] args) {
        RiverEngine engine = new RiverEngine();
        int diagonal = retained(engine, true);
        int horizontal = retained(engine, false);
        if (diagonal != 32 || horizontal != 32)
            throw new AssertionError("缓存未保留完整容量");
        System.out.println("CACHE diagonal_retained=" + diagonal
                + " horizontal_retained=" + horizontal + " inserted=32");
        var old = new RiverStartupSession(() -> {}, () -> {});
        old.enter(); old.progress(1000, 0, 7); old.close();
        var fresh = new RiverStartupSession(() -> {}, () -> {});
        fresh.enter(); fresh.progress(20, 0, 7);
        // 模拟旧回调的相同调用次序；不声称已复现真实世界切换的线程时序。
        old.completedOne();
        var update = StreamsStartupProgress.snapshot();
        if (!update.active() || update.completedPlates() != 0)
            throw new AssertionError("旧回调污染新进度");
        old.finish(true);
        var finish = StreamsStartupProgress.snapshot();
        if (!finish.active() || finish.completedPlates() != 0)
            throw new AssertionError("旧结案提前结束新进度");
        System.out.println("PROGRESS stale_update_completed=" + update.completedPlates()
                + " stale_finish_active=" + finish.active() + " new_session_total=20");
        fresh.close();
    }

    private static int retained(RiverEngine engine, boolean diagonal) {
        ChunkPlanCache.clear();
        ColumnPlan[][] plans = new ColumnPlan[32][];
        for (int i = 0; i < plans.length; ++i) {
            plans[i] = new ColumnPlan[] { ColumnPlan.NONE };
            ChunkPlanCache.put(engine, i * 16, diagonal ? i * 16 : 0, plans[i]);
        }
        int found = 0;
        for (int i = 0; i < plans.length; ++i)
            if (ChunkPlanCache.get(engine, i * 16, diagonal ? i * 16 : 0) == plans[i])
                ++found;
        return found;
    }
}
"""


def main():
    relative_sources = [
        "dev/streamsreflowing/core/compose/ColumnPlan.java",
        "dev/streamsreflowing/worldgen/ChunkPlanCache.java",
        "dev/streamsreflowing/worldgen/BoundedPlanTable.java",
        "dev/streamsreflowing/compat/streamscontrol/StreamsStartupProgress.java",
        "dev/streamsreflowing/compat/streamscontrol/RiverStartupSession.java",
    ]
    output_dir = ROOT / "build/reviews"
    output_dir.mkdir(parents=True, exist_ok=True)
    # 临时编译产物只放本仓build；退出自动清理，不写生产源文件或全局Gradle缓存。
    with tempfile.TemporaryDirectory(prefix="startup-cache-", dir=output_dir) as temporary:
        work = Path(temporary).resolve()
        # TemporaryDirectory退出会递归清理，因此先确认其绝对目标始终位于本仓build。
        if not work.is_relative_to(output_dir.resolve()):
            raise RuntimeError("临时目录超出审查输出范围")
        stub = work / "RiverEngine.java"
        probe = work / "StartupCacheProbe.java"
        classes = work / "classes"
        stub.write_text(STUB, encoding="utf-8")
        probe.write_text(PROBE, encoding="utf-8")
        subprocess.run(
            [str(JAVA_HOME / "bin/javac.exe"), "-encoding", "UTF-8", "-d", str(classes),
             str(stub), *(str(SOURCE / name) for name in relative_sources), str(probe)],
            check=True, capture_output=True,
        )
        result = subprocess.run(
            [str(JAVA_HOME / "bin/java.exe"), "-cp", str(classes),
             "dev.streamsreflowing.compat.streamscontrol.StartupCacheProbe"],
            check=True, capture_output=True, text=True,
        )
    evidence = {
        "scope": "production components with an empty RiverEngine type stub; no Minecraft runtime or performance claim",
        "stdout": result.stdout.splitlines(),
        "source_sha256": {
            name: hashlib.sha256((SOURCE / name).read_bytes()).hexdigest()
            for name in relative_sources
        },
        "new_junit_tests": 0,
        "production_mutations": 0,
    }
    serialized = json.dumps(evidence, ensure_ascii=False, indent=2) + "\n"
    (output_dir / "startup-cache-probe.json").write_text(serialized, encoding="utf-8", newline="\n")
    print(serialized, end="")


if __name__ == "__main__":
    main()
