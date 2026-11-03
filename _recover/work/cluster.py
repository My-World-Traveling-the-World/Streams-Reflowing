import re, collections, os, sys
log = sys.argv[1] if len(sys.argv)>1 else 'compile8.log'
t = open(log,'rb').read().decode('utf-8','replace')
hdr = re.compile(r'^(.*?\.java):(\d+): 错误: (.*)$', re.M)
recs = [(os.path.basename(f), n, m) for f,n,m in hdr.findall(t)]
print('总记录', len(recs), ' 去重后', len(set(recs)))
msgs = collections.Counter(m for _,_,m in set(recs))
print('=== 消息类型 Top12（去重口径）===')
for m,c in msgs.most_common(12): print(f'{c:5d}  {m[:70]}')
syms = collections.Counter(re.findall(r'符号:\s*(?:变量|方法|类)\s*(\S+)', t))
print('=== 找不到符号 Top12 ===')
for s,c in syms.most_common(12): print(f'{c:5d}  {s}')
files = collections.Counter(n for n,_,_ in set(recs))
print('=== 文件错误数（升序前12）===')
for f,c in sorted(files.items(), key=lambda kv: kv[1])[:12]: print(f'{c:5d}  {f}')
