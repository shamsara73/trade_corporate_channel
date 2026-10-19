"""Reader for the FTI database specification (sources/FTI-Database-*.html).

The HTML lists every table with its columns, types and logical names. This module
turns it into plain Python data so tables and columns can be searched and so
documents can be checked against the real column names.

    python3 -I tools/dbspec/dbspec_lib.py search 'EBANK'
    python3 -I tools/dbspec/dbspec_lib.py table MASTER
"""
import glob
import html
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
_cache = {}


def _text(path):
    t = open(path, encoding='utf8', errors='ignore').read()
    t = re.sub(r'<(script|style)[^>]*>.*?</\1>', '', t, flags=re.S)
    t = re.sub(r'<(br|/p|/div|/tr|/h\d|/li|/table)[^>]*>', '\n', t)
    t = re.sub(r'</t[dh]>', ' | ', t)
    t = re.sub(r'<[^>]+>', ' ', t)
    t = html.unescape(t)
    t = re.sub(r'[ \t]+', ' ', t)
    return re.sub(r'\n\s*\n+', '\n', t)


def load(path=None):
    """Return {TABLE: {desc, note, cols: [{col, desc, type, logical}]}}."""
    path = path or sorted(glob.glob(os.path.join(ROOT, 'sources', 'FTI-Database-*.html')))[-1]
    if path in _cache:
        return _cache[path]
    lines = [l.strip().rstrip('|').strip() for l in _text(path).split('\n')]
    start = next(i for i, l in enumerate(lines) if re.match(r'^[A-Z0-9_]+ - ', l) and i > 20)
    # the index lists every table once; the real sections follow it
    seen, first_section = set(), None
    for i, l in enumerate(lines):
        m = re.match(r'^([A-Z0-9_]+) - ', l)
        if m:
            if m.group(1) in seen:
                first_section = i
                break
            seen.add(m.group(1))
    index_names = list(seen)
    pos = {}
    for i in range(first_section or start, len(lines)):
        m = re.match(r'^([A-Z0-9_]+) - (.*)$', lines[i])
        if m and m.group(1) in seen and m.group(1) not in pos:
            pos[m.group(1)] = (i, m.group(2))
    heads = sorted((i, n, d) for n, (i, d) in pos.items())
    tables = {}
    for n, (i, name, desc) in enumerate(heads):
        end = heads[n + 1][0] if n + 1 < len(heads) else len(lines)
        body = [x for x in lines[i + 1:end] if x]
        if 'Column' not in body:
            continue
        j = body.index('Column')
        note = ' '.join(body[:j])
        while j < len(body) and body[j] in ('Column', 'Description', 'Type', 'Logical name'):
            j += 1
        cols, rest, k = [], body[j:], 0
        while k + 3 < len(rest):
            c = rest[k:k + 4]
            if c[0] in ('Index', 'Indexes'):
                break
            cols.append(dict(col=c[0], desc=c[1], type=c[2], logical=re.sub(r'\s+', '', c[3])))
            k += 4
        tables[name] = dict(desc=desc, note=note, cols=cols)
    _cache[path] = tables
    return tables


def column(tables, table, col):
    for c in tables[table]['cols']:
        if c['col'] == col:
            return c
    raise KeyError('%s.%s' % (table, col))


def by_logical(tables, table, logical):
    """Column of `table` whose logical name (spaces removed) equals `logical`."""
    for c in tables[table]['cols']:
        if c['logical'] == logical:
            return c
    raise KeyError('%s: no column with logical name %s' % (table, logical))


def main(argv):
    t = load()
    if len(argv) >= 2 and argv[0] == 'search':
        rx = re.compile(argv[1], re.I)
        for n, v in t.items():
            if rx.search(n) or rx.search(v['desc']):
                print('TABLE %s - %s (%d columns)' % (n, v['desc'], len(v['cols'])))
            for c in v['cols']:
                if rx.search(c['col']) or rx.search(c['desc']) or rx.search(c['logical']):
                    print('  %s.%s | %s | %s | %s' % (n, c['col'], c['desc'][:80], c['type'], c['logical'][:60]))
    elif len(argv) >= 2 and argv[0] == 'table':
        v = t[argv[1].upper()]
        print('%s - %s\n%s' % (argv[1].upper(), v['desc'], v['note']))
        for c in v['cols']:
            print('  %-12s %-28s %s' % (c['col'], c['type'], c['desc'][:90]))
    else:
        print(__doc__)


if __name__ == '__main__':
    main(sys.argv[1:])
