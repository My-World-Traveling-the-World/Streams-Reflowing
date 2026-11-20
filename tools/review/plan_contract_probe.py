"""核实named契约与实际intermediary成品签名/消费者行为，不把常量池重排当ABI破坏。"""
import hashlib
import json
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(r"C:\Program Files\Java\jdk-21\bin")
PROBE = r"""
import dev.streamsreflowing.api.RiverPlanApi;
import java.util.List;
public class ContractConsumer {
    public static void main(String[] args) throws Exception {
        if (RiverPlanApi.open(null).availability() != RiverPlanApi.Availability.ABSENT) throw new AssertionError();
        var session = new RiverPlanApi.Session() {
            public Object identity() { return this; }
            public RiverPlanApi.Column column(int x,int z) { return new RiverPlanApi.Column(true,true,71,65,2,4); }
            public RiverPlanApi.Lake lake(int x,int z) { return new RiverPlanApi.Lake(true,75,74); }
            public List<RiverPlanApi.Plate> platesNear(double x,double z,double r) { return List.of(new RiverPlanApi.Plate(-2,4)); }
            public void prewarm(RiverPlanApi.Plate p) { if (p.x()!=-2 || p.z()!=4) throw new AssertionError(); }
            public RiverPlanApi.Startup startup() { return new RiverPlanApi.Startup(9,RiverPlanApi.Phase.FAILED,"sampling failed"); }
        };
        RiverPlanApi.register(new RiverPlanApi.Provider() {
            public RiverPlanApi.Session open(Object w) { if(w==null)throw new IllegalStateException("bad world");return session; }
            public RiverPlanApi.Startup startup(Object w) { return session.startup(); }
        });
        var opened = RiverPlanApi.open(new Object());
        if (opened.availability()!=RiverPlanApi.Availability.AVAILABLE || opened.session()!=session) throw new AssertionError();
        if (!session.column(1,2).equals(new RiverPlanApi.Column(true,true,71,65,2,4)))throw new AssertionError();
        if (!session.lake(1,2).equals(new RiverPlanApi.Lake(true,75,74)))throw new AssertionError();
        session.prewarm(session.platesNear(1,2,64).getFirst());
        if (RiverPlanApi.open(null).availability()!=RiverPlanApi.Availability.FAILED)throw new AssertionError();
        if (RiverPlanApi.startup(null).phase()!=RiverPlanApi.Phase.FAILED)throw new AssertionError();
        System.out.println("CONTRACT_PASS available/absent/failed,column/lake/plate/prewarm,startup");
    }
}
"""

def main():
    properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    production = ROOT / "build/libs" / f"{properties['archives_base_name']}-{properties['mod_version']}.jar"
    contract = ROOT / "build/contract/streams-plan-api.jar"
    prefix = "dev/streamsreflowing/api/"
    with zipfile.ZipFile(contract) as named, zipfile.ZipFile(production) as intermediary:
        classes = sorted(name for name in named.namelist() if name.endswith(".class"))
        assert len(classes) == 10, classes
        assert all(name.startswith(prefix) for name in classes)
        assert sorted(n for n in intermediary.namelist() if n.startswith(prefix) and n.endswith(".class")) == classes
        for name in classes:
            data = named.read(name)
            for forbidden in [b"net/minecraft/", b"dev/streamsreflowing/core/",
                              b"dev/streamsreflowing/worldgen/", b"dev/streamsreflowing/compat/"]:
                assert forbidden not in data and forbidden not in intermediary.read(name), (name, forbidden)
            binary_name = name[:-6].replace("/", ".")
            signatures = [subprocess.run([str(JAVA / "javap.exe"), "-p", "-s", "-classpath", str(jar), binary_name],
                                        check=True, capture_output=True).stdout for jar in [contract, production]]
            assert signatures[0] == signatures[1], binary_name
        output = ROOT / "build/reviews"
        output.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="contract-probe-", dir=output) as temporary:
            work = Path(temporary).resolve()
            # 退出清理前核对临时目录边界，消费者只编译一次，再切换真实运行时jar。
            assert work.is_relative_to(output.resolve())
            source = work / "ContractConsumer.java"
            source.write_text(PROBE, encoding="utf-8")
            subprocess.run([str(JAVA / "javac.exe"), "-encoding", "UTF-8", "-cp", str(contract),
                            "-d", str(work / "classes"), str(source)], check=True, capture_output=True)
            runs = [subprocess.run([str(JAVA / "java.exe"), "-cp", str(work / "classes") + ";" + str(jar),
                                    "ContractConsumer"], check=True, capture_output=True).stdout.decode("ascii").strip()
                    for jar in [contract, production]]
            assert runs[0] == runs[1]
        result = {
            "scope": "identical signatures and same compiled consumer on named/remapped jars; not a Minecraft launch",
            "contract_sha256": hashlib.sha256(contract.read_bytes()).hexdigest(),
            "production_sha256": hashlib.sha256(production.read_bytes()).hexdigest(),
            "identical_signatures": classes,
            "consumer_runs": runs,
        }
    target = ROOT / "build/reviews/plan-contract-artifacts.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main()
