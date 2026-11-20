"""核对真实named/正式intermediary颜色字段绑定；不初始化Minecraft或把静态绑定当完整Mixin启动。"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVAP = Path(r"C:\Program Files\Java\jdk-21\bin\javap.exe")
MIXIN = "dev.streamsreflowing.fabric.mixin.WaterTintMixin"
ENTRY = MIXIN.replace(".", "/") + ".class"


def inspect(classpath, classname):
    run = subprocess.run([str(JAVAP), "-p", "-c", "-s", "-classpath", str(classpath), classname],
                         check=True, capture_output=True)
    text = run.stdout.decode("utf-8")
    fields = dict(re.findall(r"public static (?:final )?\S+ (\w+);\s+descriptor: (\S+)", text))
    return text, fields


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--before", type=Path, help="可选：本轮修复前正式包，只比较相同类的字节码")
    args = parser.parse_args()
    properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    release = ROOT / "build/libs" / f"{properties['archives_base_name']}-{properties['mod_version']}.jar"
    games = list(ROOT.glob(".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/*loom.mappings*/*.jar"))
    assert len(games) == 1, "必须明确唯一当前official开发目标，不能随意选择旧版Minecraft"
    named_game = games[0]
    version = named_game.parent.name
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    production_game = (gradle_home / "caches/fabric-loom/minecraftMaven/net/minecraft/"
                       "minecraft-merged-intermediary" / version / f"minecraft-merged-intermediary-{version}.jar")
    assert production_game.is_file(), production_game
    namespaces = [
        ("named", ROOT / "build/classes/java/main", named_game, "net.minecraft.client.renderer.BiomeColors",
         {"WATER_COLOR_RESOLVER", "GRASS_COLOR_RESOLVER", "FOLIAGE_COLOR_RESOLVER"}),
        ("intermediary", release, production_game, "net.minecraft.class_1163",
         {"field_5666", "field_5665", "field_5664"}),
    ]
    records = []
    for namespace, artifact, game, target, expected in namespaces:
        _, shadows = inspect(artifact, MIXIN)
        _, fields = inspect(game, target)
        assert set(shadows) == expected, (namespace, shadows)
        assert all(fields.get(name) == descriptor for name, descriptor in shadows.items()), (namespace, shadows, fields)
        records.append(dict(namespace=namespace, resolved_fields=shadows,
                            target_sha256=hashlib.sha256(game.read_bytes()).hexdigest()))
    with zipfile.ZipFile(release) as jar:
        current = jar.read(ENTRY)
        assert b"Lnet/minecraft/class_1163;" in current, "正式Mixin目标也必须被映射"
    result = dict(scope="real class field/descriptor binding; not full Mixin application or a client launch",
                  namespaces=records, production_sha256=hashlib.sha256(release.read_bytes()).hexdigest(),
                  mixin_class_sha256=hashlib.sha256(current).hexdigest())
    if args.before:
        with zipfile.ZipFile(args.before) as jar:
            previous = jar.read(ENTRY)
        before_text, _ = inspect(args.before, MIXIN)
        after_text, _ = inspect(release, MIXIN)
        # 常量池编号不是行为；仍要求全部反汇编方法/字段/描述符一致，不能只比三个名字。
        normalize = lambda text: re.sub(r"#\d+", "#", text)
        assert normalize(before_text) == normalize(after_text), "正式方法指令或字段契约发生变化"
        result.update(before_mixin_class_sha256=hashlib.sha256(previous).hexdigest(),
                      identical_production_class_bytes=previous == current,
                      identical_production_disassembly=True)
    output = ROOT / "build/reviews/water-tint-bindings.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
