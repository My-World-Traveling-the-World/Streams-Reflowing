import re
import sys

ROOT = r'D:\云游四海-Mod源码\streams-reflowing-src'
TARGET = ROOT + r'\src\main\java\dev\streamsreflowing\core\river\NetworkTracer.java'

name = sys.argv[1]
lo = int(sys.argv[2]) if len(sys.argv) > 2 else 1
hi = int(sys.argv[3]) if len(sys.argv) > 3 else 10 ** 9
lines = open(TARGET, encoding='utf-8').read().splitlines()
pat = re.compile(r'\b' + re.escape(name) + r'\b')
for i, l in enumerate(lines, 1):
    if lo <= i <= hi and pat.search(l):
        print(f'{i}\t{l.strip()}')
