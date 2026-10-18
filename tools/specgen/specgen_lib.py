"""Helpers that read the FTI Gateway XSD documentation and example XML in
sources/ and render Markdown fragments for the product specifications."""
import glob
import os
import re
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
XSD = os.path.join(ROOT, 'sources', 'xsd_element_docs')
EXM = os.path.join(ROOT, 'sources', 'example_xml')

SKELETON = ['Context', 'DocumentsReceiveds', 'EventNotificationss', 'FileIdentification',
            'EmbeddedItemss', 'MasterRef', 'ContinueEvent', 'ProvisionalFlags', 'ExternalReview']
LINE = re.compile(r'^(\s*)([A-Za-z]\w*)( \[repeating\])? \*\((.*?)\)\* - ?(.*)$')
NS = {'urn:messages.service.ti.apps.tiplus2.misys.com': 'm',
      'urn:common.service.ti.apps.tiplus2.misys.com': 'c'}


class Node:
    def __init__(self, name, typ, desc, rep, depth):
        self.name, self.typ, self.desc, self.rep, self.depth = name, typ, desc, rep, depth
        self.children = []
        self.parent = None

    @property
    def path(self):
        return (self.parent.path + '/' if self.parent and self.parent.parent else '') + self.name

    def walk(self):
        for c in self.children:
            yield c
            yield from c.walk()


_cache = {}


def parse(code):
    if code in _cache:
        return _cache[code]
    root, stack = None, []
    for raw in open(os.path.join(XSD, 'TI.%s.xsd.txt' % code), encoding='utf8').read().splitlines():
        m = LINE.match(raw)
        if not m:
            continue
        ind, name, rep, typ, desc = m.groups()
        n = Node(name, typ, desc.strip(), bool(rep), len(ind) // 2)
        if root is None:
            root = n
            stack = [n]
            continue
        while stack and stack[-1].depth >= n.depth:
            stack.pop()
        n.parent = stack[-1]
        stack[-1].children.append(n)
        stack.append(n)
    _cache[code] = root
    return root


def top_names(code, skeleton=False):
    return [c.name for c in parse(code).children if skeleton or c.name not in SKELETON]


def purpose(code):
    d = parse(code).desc
    m = re.search(r'Creates:?\s*(.*)$', d)
    creates = m.group(1).strip().rstrip('.') if m else ''
    pm = re.search(r'Purpose:\s*(.*?)(?:Creates|$)', d)
    p = pm.group(1).strip().rstrip('. ') if pm else ('' if m else d.rstrip('. '))
    return p, creates


def fmt_type(t, group=False):
    if group or t.strip() in ('', ','):
        return 'group'
    t = t.replace('nillable: true, ', '').replace('nillable: true', '').strip(', ')
    m = re.match(r'decimal TotalDigits: (\d+), FractionDigits: (\d+)', t)
    if m:
        return 'decimal(%s,%s)' % m.groups()
    if t == 'logical':
        return 'boolean'
    m = re.match(r'string(?: Min: (\d+))?(?:,)? ?(?:Max: (\d+))?', t)
    if t.startswith('string'):
        lo, hi = m.group(1), m.group(2)
        if lo and hi:
            return 'string %s–%s' % (lo, hi)
        if hi:
            return 'string ≤%s' % hi
        return 'string'
    return t


def esc(s):
    return s.replace('|', '/').replace('\n', ' ')


def short(s, n=150):
    s = esc(s)
    return s if len(s) <= n else s[:n - 3].rstrip() + '...'


def enums(desc):
    m = re.search(r'Available [Vv]alues[^:]*:\s*(.*)$', desc)
    if not m:
        return []
    return re.findall(r"'([^']*)'='([^']*)'", m.group(1))


def is_party(n):
    names = {c.name for c in n.children}
    return {'NameAddress', 'SwiftAddress'} <= names or {'Customer', 'NameAddress'} <= names


def field_rows(nodes):
    rows = []
    for n in nodes:
        t = fmt_type(n.typ, bool(n.children))
        if n.rep:
            t += ' ×n'
        if is_party(n):
            t = 'party'
        rows.append('| `%s` | %s | %s |' % (n.name, t, short(n.desc)))
    return rows


def table(rows):
    return '| Element | Type / length | Description |\n|---|---|---|\n' + '\n'.join(rows)


def expand(n, maxdepth=3):
    """One row per descendant of group n, parties and address blocks collapsed."""
    rows = []

    def rec(x, d):
        for c in x.children:
            if c.name in ('MXAddress', 'SwiftTradeAddress'):
                continue
            t = 'party' if is_party(c) else fmt_type(c.typ, bool(c.children))
            if c.rep:
                t += ' ×n'
            rows.append('| `%s` | %s | %s |' % (c.path, t, short(c.desc, 110)))
            if not is_party(c) and d < maxdepth:
                rec(c, d + 1)
    rec(n, 1)
    return rows


def enum_rows(root, only=None):
    rows = []
    for n in root.walk():
        if only is not None and n.path.split('/')[0] not in only:
            continue
        e = enums(n.desc)
        if e and n.name != 'Country':
            rows.append('| `%s` | %s |' % (n.path, '; '.join('`%s` %s' % (k, esc(v)) for k, v in e)))
    return rows


def best_match(names, others):
    s = set(names)
    best = (0, None)
    for code, o in others:
        so = set(o)
        j = len(s & so) / max(1, len(s | so))
        if j > best[0]:
            best = (j, code)
    return best


# ---------- sample XML ----------

def _ex_tree(code):
    p = os.path.join(EXM, 'TI.%s.xml' % code)
    return ET.parse(p).getroot()


def _local(tag):
    uri, _, name = tag[1:].partition('}')
    return NS.get(uri, ''), name


def auto_value(n):
    t, nm = n.typ, n.name
    e = enums(n.desc)
    if e:
        return e[0][0]
    mx = re.search(r'Max: (\d+)', t)
    mx = int(mx.group(1)) if mx else 30
    if 'date' in t and 'string' not in t:
        return '2027-01-31'
    if t.strip().startswith('int'):
        return '1'
    if t.strip().startswith('long'):
        return '1000000'
    if t.startswith('decimal'):
        return '1.00'
    if t.strip().startswith('logical'):
        return 'N'
    ln = nm.lower()
    if 'currency' in ln or ln.endswith('ccy') or ln == 'ccy':
        return 'USD'
    if ln == 'country' or ln.endswith('country'):
        return 'ID'
    if 'amount' in ln:
        return '100000.00'
    if 'swift' in ln or 'bic' in ln:
        return 'ABCDIDJAXXX'
    if ln in ('customer', 'mnemonic'):
        return 'ACMEIMP'
    return ('Sample ' + nm)[:mx]


def sample(code, ctx, values):
    """Build a request skeleton. `values` maps schema paths (e.g. LCAmount/Amount)
    to illustrative text; a value of None asks for an automatic one. Structure,
    order and namespace prefixes come from the example XML."""
    schema = parse(code)
    by_path = {n.path: n for n in schema.walk()}
    ex = _ex_tree(code)
    msg = [c for c in ex if _local(c.tag)[1] == code][0]
    wanted = {}
    for p, v in list(ctx.items()):
        wanted['Context/' + p] = v
    for p, v in values.items():
        wanted[p] = v
    prefixes = set()
    for p in wanted:
        parts = p.split('/')
        for i in range(1, len(parts) + 1):
            prefixes.add('/'.join(parts[:i]))
    out = ['<?xml version="1.0" standalone="yes"?>',
           '<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
           '                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"',
           '                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"',
           '                xmlns="urn:control.services.tiplus2.misys.com">',
           '  <RequestHeader>',
           '    <Service>TI</Service>',
           '    <Operation>%s</Operation>' % code,
           '    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>',
           '    <SourceSystem>CORPCHANNEL</SourceSystem>',
           '    <TargetSystem>TI</TargetSystem>',
           '    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>',
           '    <CreationDate>2026-10-18T09:30:00Z</CreationDate>',
           '  </RequestHeader>',
           '  <m:%s>' % code]

    def rec(el, path, ind):
        for ch in el:
            pfx, name = _local(ch.tag)
            p = (path + '/' if path else '') + name
            if p not in prefixes:
                continue
            tag = (pfx + ':' if pfx else '') + name
            sub = [x for x in ch]
            if sub and not (p in wanted):
                out.append('  ' * ind + '<%s>' % tag)
                rec(ch, p, ind + 1)
                out.append('  ' * ind + '</%s>' % tag)
            else:
                v = wanted[p]
                if v is None:
                    v = auto_value(by_path[p]) if p in by_path else 'x'
                out.append('  ' * ind + '<%s>%s</%s>' % (tag, str(v).replace('&', '&amp;').replace('<', '&lt;'), tag))
    used = set()
    _orig_prefixes = prefixes

    def mark(el, path):
        for ch in el:
            _, name = _local(ch.tag)
            p = (path + '/' if path else '') + name
            used.add(p)
            mark(ch, p)
    mark(msg, '')
    missing = [p for p in wanted if p not in used]
    if missing:
        raise ValueError('%s: sample paths not in example XML: %s' % (code, missing))
    rec(msg, '', 2)
    out += ['  </m:%s>' % code, '</ServiceRequest>']
    return '\n'.join(out)
