import re
import subprocess
import sys

ROOT = r'D:\云游四海-Mod源码\streams-reflowing-src'
ARG = ROOT + r'\_recover\work\nt_args.txt'

javac = ['javac', '-J-Duser.language=en', '-J-Duser.country=US', '@' + ARG]
r = subprocess.run(javac, cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace')
out = r.stdout + r.stderr
pat = re.compile(r'NetworkTracer\.java:(\d+): error: (.*)')
lines = out.splitlines()
res = []
i = 0
while i < len(lines):
    m = pat.search(lines[i])
    if m:
        ln = int(m.group(1))
        msg = m.group(2)
        src = lines[i + 1].strip() if i + 1 < len(lines) else ''
        res.append((ln, msg, src))
    i += 1
print('total errors:', len(res), file=sys.stderr)
for ln, msg, src in res:
    print(f'{ln}\t{msg}\t{src[:160]}')
