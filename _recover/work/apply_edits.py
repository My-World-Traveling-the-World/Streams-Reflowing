"""Apply line-anchored edits to a file, verifying the expected old text is present.

Usage: python apply_edits.py <edits.json>

edits.json = [{"line": 285, "old": "Object object4", "new": "Iterator<Integer> iteratorCell"}, ...]
`old` is matched as a substring of that 1-based line (must match exactly once).
Edits are applied bottom-up so line numbers of later edits stay valid.
"""
import json
import sys

ROOT = r'D:\云游四海-Mod源码\streams-reflowing-src'
TARGET = ROOT + r'\src\main\java\dev\streamsreflowing\core\river\NetworkTracer.java'


def main():
    edits_path = sys.argv[1]
    edits = json.load(open(edits_path, encoding='utf-8'))
    with open(TARGET, encoding='utf-8', newline='') as f:
        lines = f.readlines()
    problems = []
    for e in sorted(edits, key=lambda x: -x['line']):
        idx = e['line'] - 1
        if idx < 0 or idx >= len(lines):
            problems.append(f"line {e['line']} out of range")
            continue
        src = lines[idx]
        if 'insert_after' in e:
            lines.insert(idx + 1, e['insert_after'] + '\n')
            continue
        if e.get('delete'):
            if src.count(e['old']) != 1:
                problems.append(f"line {e['line']}: delete expected 1 occurrence of {e['old']!r}")
                continue
            del lines[idx]
            continue
        if src.count(e['old']) != 1:
            problems.append(f"line {e['line']}: expected 1 occurrence of {e['old']!r}, found {src.count(e['old'])}; line={src.rstrip()!r}")
            continue
        lines[idx] = src.replace(e['old'], e['new'], 1)
    if problems:
        print('ABORT - no changes written:')
        for p in problems:
            print('  ' + p)
        sys.exit(1)
    with open(TARGET, 'w', encoding='utf-8', newline='') as f:
        f.writelines(lines)
    print(f'applied {len(edits)} edits, file now {len(lines)} lines')


if __name__ == '__main__':
    main()
