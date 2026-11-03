import re, collections, os
t = open('compile8.log','rb').read().decode('utf-8','replace')
lines = t.splitlines()
blocks = []  # (key, msg, symbol)
cur = None
for ln in lines:
    m = re.match(r'^(.*?\.java):(\d+): 错误: (.*)$', ln)
    if m:
        cur = [ (os.path.basename(m.group(1)), m.group(2), m.group(3)), None ]
        blocks.append(cur); continue
    if cur is not None:
        sm = re.search(r'符号:\s*(?:变量|方法|类)\s*(\S+)', ln)
        if sm and cur[1] is None: cur[1] = sm.group(1)
seen = {}
for k, s in blocks:
    seen.setdefault(tuple(k), s)
print('错误头部行', len(blocks), '去重', len(seen))
fam = collections.Counter(k[2] for k in seen)
print('=== 族（去重）===')
for m,c in fam.most_common(12): print(f'{c:5d}  {m[:60]}')
sym = collections.Counter(s for s in seen.values() if s)
print('=== 找不到符号 符号Top12（去重）===')
for s,c in sym.most_common(12): print(f'{c:5d}  {s}')
f = collections.Counter(k[0] for k in seen)
print('=== 文件（升序前10 / 降序前5）===')
for n,c in sorted(f.items(), key=lambda kv: kv[1])[:10]: print(f'{c:4d}  {n}')
print('  ...')
for n,c in sorted(f.items(), key=lambda kv: -kv[1])[:5]: print(f'{c:4d}  {n}')
