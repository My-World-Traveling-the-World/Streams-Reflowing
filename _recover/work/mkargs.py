import os, sys

root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
work = os.path.join(root, '_recover', 'work')
cp = [l.strip() for l in open(os.path.join(work, 'classpath.txt'), encoding='utf-8', errors='replace') if l.strip()]
cp.append(os.path.join(work, 'streams-named.jar'))
exist = [p.replace('\\', '/') for p in cp if os.path.exists(p)]

target = sys.argv[1] if len(sys.argv) > 1 else 'src/main/java/dev/streamsreflowing/core/river/NetworkTracer.java'
args = os.path.join(work, 'nt_args.txt')
with open(args, 'w', encoding='utf-8') as f:
    f.write('-cp "' + ';'.join(exist) + '"\n')
    f.write('-d _recover/work/ntout\n')
    f.write('-nowarn -proc:none -Xmaxerrs 500 --release 21 -encoding UTF-8\n')
    f.write(target + '\n')
print('wrote', args, 'entries', len(exist))
