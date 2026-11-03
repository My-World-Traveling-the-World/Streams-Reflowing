"""Structural (offset/slot/constant-pool independent) comparison of javap output."""
import re
import subprocess
import os
import difflib

ROOT = r'D:\云游四海-Mod源码\streams-reflowing-src'
WORK = os.path.join(ROOT, '_recover', 'work')
JAVAP = r'C:\Program Files\Java\jdk-21\bin\javap.exe'
CLS = 'dev.streamsreflowing.core.river.NetworkTracer'

SLOT_OPS = re.compile(r'^(?:[ilfda]load|astore|[ilfda]store|ret|aload|istore|iinc)(?:_\d+|\s+\d+)?.*$')


def shape(line):
    s = line.strip()
    m = re.match(r'^\d+:\s+(.*)$', s)
    if not m:
        return None
    body = m.group(1)
    comment = ''
    if '//' in body:
        body, comment = body.split('//', 1)
        body = body.strip()
        comment = comment.strip()
    body = re.sub(r'#\d+', '#', body)
    comment = re.sub(r'#\d+', '#', comment)
    body = re.sub(r'\s+\d+$', '', body)          # trailing branch target
    body = re.sub(r'^(iinc)\s+\d+,\s*', r'\1, ', body)
    body = re.sub(r'\b\d+:\s', '', body)
    key = body.split()[0] if body else ''
    if key in ('iload', 'lload', 'fload', 'dload', 'aload', 'istore', 'lstore', 'fstore',
               'dstore', 'astore', 'ret', 'iinc'):
        if key != 'iinc':
            body = key
    return body + ('  //' + comment if comment else '')


def parse(txt):
    methods = {}
    cur = None
    buf = []
    for line in txt.splitlines():
        if line.startswith('  ') and not line.startswith('   ') and '(' in line and line.rstrip().endswith(';'):
            if cur:
                methods[cur] = buf
            cur = line.strip()
            buf = []
        elif cur is not None:
            sh = shape(line)
            if sh:
                buf.append(sh)
    if cur:
        methods[cur] = buf
    return methods


def dis(cp):
    return subprocess.run([JAVAP, '-c', '-p', '-cp', cp, CLS], capture_output=True, text=True,
                          encoding='utf-8', errors='replace').stdout


def main():
    a = parse(dis(os.path.join(WORK, 'origcls')))
    b = parse(dis(os.path.join(WORK, 'ntout')))
    common = sorted(set(a) & set(b))
    differ = []
    for k in common:
        if a[k] != b[k]:
            differ.append(k)
    print('common methods:', len(common), 'identical:', len(common) - len(differ), 'differing:', len(differ))
    for k in differ:
        print(f'  DIFF {k}  orig={len(a[k])} new={len(b[k])}')
    if len(os.sys.argv) > 1:
        target = [k for k in differ if os.sys.argv[1] in k]
        for k in target[:1]:
            for l in list(difflib.unified_diff(a[k], b[k], 'orig', 'new', lineterm='', n=1))[:60]:
                print(l)


main()
