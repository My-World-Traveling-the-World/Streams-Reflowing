"""隔离缓存命中/查找代价；不把无世界的缓存微基准换算成游戏FPS。"""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Java\jdk-21")) / "bin"
BASE = "src/main/java/dev/streamsreflowing"
BENCH = r"""
package dev.streamsreflowing.worldgen;
import dev.streamsreflowing.core.compose.ColumnPlan;
import java.lang.management.ManagementFactory;
public final class CacheBench {
    static volatile Object objectSink;
    static volatile long numberSink;
    static final ColumnPlan[][] PLANS = new ColumnPlan[32][];
    public static void main(String[] args) throws Exception {
        for(int i=0;i<32;i++) PLANS[i]=new ColumnPlan[]{ColumnPlan.NONE};
        for(boolean diagonal:new boolean[]{false,true}) {
            ChunkPlanCache.clear(); FrozenChunkPlanCache.clear();
            for(int i=0;i<32;i++) {
                int x=i*16,z=diagonal?x:0;
                ChunkPlanCache.put(null,x,z,PLANS[i]);
                FrozenChunkPlanCache.put(null,x,z,PLANS[i]);
            }
            int oldMiss=0,newMiss=0;
            for(int i=0;i<32;i++) {
                int x=i*16,z=diagonal?x:0;
                var a=FrozenChunkPlanCache.get(null,x,z);var b=ChunkPlanCache.get(null,x,z);
                if(a==null)oldMiss++;else if(a!=PLANS[i])throw new AssertionError();
                if(b==null)newMiss++;else if(b!=PLANS[i])throw new AssertionError();
            }
            if(newMiss!=0||oldMiss!=(diagonal?31:0))throw new AssertionError("命中前提变化");
            for(int i=0;i<3;i++){run(false,diagonal);run(true,diagonal);}
            long[] oldTimes=new long[9],newTimes=new long[9];
            for(int i=0;i<9;i++){
                if((i&1)==0){oldTimes[i]=run(false,diagonal);newTimes[i]=run(true,diagonal);}
                else{newTimes[i]=run(true,diagonal);oldTimes[i]=run(false,diagonal);}
            }
            System.out.println("QUERY "+(diagonal?"diagonal":"horizontal")+" "+oldMiss+" "+newMiss
                +" "+java.util.Arrays.toString(oldTimes)+" "+java.util.Arrays.toString(newTimes));
        }
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        System.out.println("RING_BYTES "+allocated(bean,"dev.streamsreflowing.worldgen.FrozenChunkPlanCache$Ring")
            +" "+allocated(bean,"dev.streamsreflowing.worldgen.ChunkPlanCache$Ring"));
    }
    static long run(boolean next,boolean diagonal) {
        long sum=0,start=System.nanoTime();
        for(int repeat=0;repeat<32768;repeat++)for(int i=0;i<32;i++){
            int x=i*16,z=diagonal?x:0;
            var plan=next?ChunkPlanCache.get(null,x,z):FrozenChunkPlanCache.get(null,x,z);
            // miss仍由独立oracle提供同一产物；这里只量缓存，不假装量过真实plan()成本。
            if(plan==null)plan=PLANS[i];
            if(plan!=PLANS[i])throw new AssertionError("产物不同");
            sum+=System.identityHashCode(plan);
        }
        numberSink=sum;return System.nanoTime()-start;
    }
    static long allocated(com.sun.management.ThreadMXBean bean,String name)throws Exception {
        var ctor=Class.forName(name).getDeclaredConstructor();ctor.setAccessible(true);
        for(int i=0;i<2000;i++)objectSink=ctor.newInstance();
        long thread=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(thread);
        for(int i=0;i<20000;i++)objectSink=ctor.newInstance();
        return (bean.getThreadAllocatedBytes(thread)-before)/20000;
    }
}
"""

def main():
    output = ROOT / "build/reviews"
    output.mkdir(parents=True, exist_ok=True)
    old = subprocess.check_output(
        ["git", "show", f"7417352:{BASE}/worldgen/ChunkPlanCache.java"], cwd=ROOT).decode("utf-8")
    assert old.count("public final class ChunkPlanCache") == 1
    assert old.count("ChunkPlanCache.slot(l)") == 3
    renamed = old.replace("ChunkPlanCache", "FrozenChunkPlanCache")
    with tempfile.TemporaryDirectory(prefix="cache-bench-", dir=output) as temporary:
        work = Path(temporary).resolve()
        # 退出递归清理之前先核对范围，避免临时路径越出本仓build。
        assert work.is_relative_to(output.resolve())
        (work / "FrozenChunkPlanCache.java").write_text(renamed, encoding="utf-8")
        (work / "CacheBench.java").write_text(BENCH, encoding="utf-8")
        (work / "RiverEngine.java").write_text(
            "package dev.streamsreflowing.core; public final class RiverEngine {}", encoding="utf-8")
        sources = [
            ROOT / BASE / "core/compose/ColumnPlan.java",
            ROOT / BASE / "worldgen/ChunkPlanCache.java",
            ROOT / BASE / "worldgen/BoundedPlanTable.java",
            work / "FrozenChunkPlanCache.java", work / "CacheBench.java", work / "RiverEngine.java",
        ]
        subprocess.run([str(JAVA / "javac.exe"), "-encoding", "UTF-8", "-d", str(work / "classes"),
                        *map(str, sources)], check=True, capture_output=True)
        run = subprocess.run([str(JAVA / "java.exe"), "-Xms512m", "-Xmx512m", "-cp",
                              str(work / "classes"), "dev.streamsreflowing.worldgen.CacheBench"],
                             check=True, capture_output=True, text=True)
    result = {
        "baseline": "7417352",
        "scope": "isolated production cache; empty engine type; no real plan cost or game speed claim",
        "operations_per_sample": 1048576,
        "warmups": 3, "alternating_samples": 9,
        "old_source_sha256": hashlib.sha256(old.encode("utf-8")).hexdigest(),
        "stdout": run.stdout.splitlines(),
    }
    data = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    (output / "chunk-plan-cache-benchmark.json").write_text(data, encoding="utf-8", newline="\n")
    print(data, end="")

if __name__ == "__main__":
    main()
