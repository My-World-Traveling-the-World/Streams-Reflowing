"""Compare javap -c -p disassembly of the original NetworkTracer.class and the recompiled one."""
import re
import subprocess
import sys
import zipfile
import os

ROOT = r'D:\云游四海-Mod源码\streams-reflowing-src'
WORK = os.path.join(ROOT, '_recover', 'work')
JAVAP = r'C:\Program Files\Java\jdk-21\bin\javap.exe'
CLS = 'dev.streamsreflowing.core.river.NetworkTracer'


def extract_orig():
    out = os.path.join(WORK, 'origcls')
    os.makedirs(out, exist_ok=True)
    with zipfile.ZipFile(os.path.join(WORK, 'streams-named.jar')) as z:
        for n in z.namelist():
            if n.startswith('dev/streamsreflowing/core/river/NetworkTracer'):
                target = os.path.join(out, n.replace('/', os.sep))
                os.makedirs(os.path.dirname(target), exist_ok=True)
                with open(target, 'wb') as f:
                    f.write(z.read(n))
    return out


def disasm(cp, label):
    r = subprocess.run([JAVAP, '-c', '-p', '-cp', cp, CLS], capture_output=True, text=True,
                       encoding='utf-8', errors='replace')
    txt = r.stdout
    path = os.path.join(WORK, f'dis_{label}.txt')
    open(path, 'w', encoding='utf-8').write(txt)
    return txt


METHOD_RE = re.compile(r'^  (?:public |private |protected |static |final |synchronized |native |abstract |strictfp )*[\w$.<>\[\], ?]+ [\w$]+\(.*\)(?:;| throws .*;)?\s*$')


def parse(txt):
    """Return dict method-signature -> list of normalized instruction strings."""
    methods = {}
    cur = None
    buf = []
    for line in txt.splitlines():
        if line.startswith('  ') and not line.startswith('   ') and ('(' in line) and line.rstrip().endswith(';'):
            if cur:
                methods[cur] = buf
            cur = line.strip()
            buf = []
        elif cur is not None and re.match(r'^\s+\d+: ', line):
            buf.append(normalize(line))
    if cur:
        methods[cur] = buf
    return methods


def normalize(line):
    s = line.strip()
    # drop offset
    s = re.sub(r'^\d+: ', '', s)
    # drop the "// ..." trailing comment, but keep it if it names a member
    m = re.search(r'//\s*(.*)$', s)
    comment = ''
    if m:
        comment = m.group(1)
        s = s[:m.start()].strip()
    # strip constant-pool indices
    s = re.sub(r'#\d+', '#', s)
    comment = re.sub(r'#\d+', '#', comment).strip()
    # javap prints "invokevirtual #// Method java/lang/Foo.bar:()I" style after index removal
    if comment:
        # keep only member identity (drop line-number tables etc.)
        comment = re.sub(r'^(Method|InterfaceMethod|Field|String|class|int|float|long|double)\s*', r'\1 ', comment)
        return s + '  |' + comment
    return s


def main():
    orig_cp = extract_orig()
    a = parse(disasm(orig_cp, 'orig'))
    b = parse(disasm(os.path.join(WORK, 'ntout'), 'new'))
    print(f'methods: orig={len(a)} new={len(b)}')
    only_a = sorted(set(a) - set(b))
    only_b = sorted(set(b) - set(a))
    if only_a:
        print('only in orig:'); [print('   ', m) for m in only_a]
    if only_b:
        print('only in new:'); [print('   ', m) for m in only_b]
    diffs = 0
    for m in sorted(set(a) & set(b)):
        if a[m] != b[m]:
            diffs += 1
            if diffs <= int(sys.argv[1] if len(sys.argv) > 1 else 6):
                print('=' * 70)
                print('METHOD', m, f'(orig {len(a[m])} insns, new {len(b[m])} insns)')
                import difflib
                for l in list(difflib.unified_diff(a[m], b[m], 'orig', 'new', lineterm='', n=2))[:40]:
                    print(l)
    print(f'\n{diffs} method(s) differ out of {len(set(a) & set(b))} common methods')


if __name__ == '__main__':
    main()
