import io, os, shutil, subprocess, sys

WORK = r"D:\云游四海-Mod源码\streams-reflowing-src\_recover\work"
SRC = r"D:\云游四海-Mod源码\streams-reflowing-src\src\main\java\dev\streamsreflowing\worldgen\StreamPrefetcher.java"
TMP = r"C:\Users\18364\AppData\Local\Temp\spcheck"
JAVAC = r"C:\Program Files\Java\jdk-21\bin\javac.exe"
MOJMAP = (r"C:\Users\18364\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft"
          r"\minecraft-merged\1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2"
          r"\minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar")
INTERMEDIARY = r"C:\Users\18364\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.21.1-net.fabricmc.intermediary.1_21_1.1.21.1-v2\minecraft-merged-1.21.1-net.fabricmc.intermediary.1_21_1.1.21.1-v2.jar"

os.makedirs(TMP, exist_ok=True)
shutil.copy2(os.path.join(WORK, "streams-named.jar"), os.path.join(TMP, "mod-classes.jar"))
shutil.copy2(SRC, os.path.join(TMP, "StreamPrefetcher.java"))

entries = [l.strip() for l in io.open(os.path.join(WORK, "classpath.txt"), encoding="utf-8") if l.strip()]
out = []
n = 0
for e in entries:
    if e == INTERMEDIARY:
        continue  # intermediary names, irrelevant; use mojmap below
    if any(ord(c) > 127 for c in e):
        n += 1
        dst = os.path.join(TMP, "extra%d.jar" % n)
        shutil.copy2(e, dst)
        out.append(dst)
        continue
    out.append(e)
out.insert(0, MOJMAP)
out.insert(1, os.path.join(TMP, "mod-classes.jar"))

cp = ";".join(out)
cp = ";".join(out).replace("\\", "/")
argfile = os.path.join(TMP, "args.txt")
fwd = lambda p: p.replace("\\", "/")
with io.open(argfile, "w", encoding="ascii", newline="\n") as f:
    f.write('-proc:none\n')
    f.write('-nowarn\n')
    f.write('-Xmaxerrs\n500\n')
    f.write('-encoding\nUTF-8\n')
    f.write('-d\n"%s"\n' % fwd(os.path.join(TMP, "out")))
    f.write('-cp\n"%s"\n' % cp)
    f.write('"%s"\n' % fwd(os.path.join(TMP, "StreamPrefetcher.java")))

print("jars:", len(out))
r = subprocess.run([JAVAC, "@" + argfile], capture_output=True)
txt = (r.stdout + r.stderr).decode("gbk", "replace")
lines = [l for l in txt.splitlines() if l.strip()]
sp = [l for l in lines if "StreamPrefetcher.java" in l]
rep = os.path.join(TMP, "result.txt")
with io.open(rep, "w", encoding="utf-8") as f:
    f.write("EXIT %d lines %d\n" % (r.returncode, len(lines)))
    f.write("SP_LINES %d\n" % len(sp))
    for l in sp:
        f.write(l + "\n")
    f.write("---- first 40 lines ----\n")
    for l in lines[:40]:
        f.write(l + "\n")
print("wrote", rep, "exit", r.returncode, "splines", len(sp))
