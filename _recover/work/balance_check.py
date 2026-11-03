import io, re, sys

p = sys.argv[1]
s = io.open(p, encoding='utf-8').read()
s2 = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
s2 = re.sub(r"'(?:\\.|[^'\\])*'", "''", s2)
s2 = re.sub(r'/\*.*?\*/', '', s2, flags=re.S)
s2 = re.sub(r'//[^\n]*', '', s2)
for a, b in [('{', '}'), ('(', ')'), ('[', ']')]:
    print(a, b, s2.count(a), s2.count(b))
print('lines', s.count('\n'))
print('tabs', s.count('\t'), 'crlf', s.count('\r\n'))
