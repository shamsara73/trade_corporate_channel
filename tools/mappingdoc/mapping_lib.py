"""Parser for TI outward-message mapping documents (sources/ti_outbound/*.txt).

Notation (decoded from TFUTICRT; see docs/outbound/README.md for confidence):

    FieldName<<TAG,type,sub>>
    FieldName<<TAG,c>>                                       party customer (TFFINREJ: B+DB, B+FT)
    FieldName<<TEXT,T|E,literal text>>
    FieldName<<TAG,*,col-tag,col-type,col-sub,ColName,...>>   repeating table

Fields are separated by one space (TFFINREJ also has none before its second field);
groups of related fields by two spaces. TFFINREJ has a space between a field name and its
"<<" ("AttachedDocuments <<GDOCS..."), which is read as part of the field.
"""
import json
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SRC = os.path.join(ROOT, 'sources', 'ti_outbound')

TOKEN = re.compile(r'(\S+?) ?<<(.*?)>>')

TYPES = {'s': 'string', 'r': 'reference', 'd': 'date', 'l': 'logical', 'i': 'integer', 'n': 'narrative',
         'p': 'party', 'v': 'value', 'a': 'account', '*': 'table', 'T': 'literal', 'E': 'literal',
         'c': 'party customer'}
PARTY_SUB = {'cu': 'customer mnemonic', 'sw1': 'name (line 1)', 'sw2': 'address line 2', 'sw3': 'address line 3',
             'sw4': 'address line 4', 'sw5': 'address line 5', 'r': 'party reference', 'cc': 'country code',
             'ss': 'SWIFT BIC', 'p': 'telephone', 'x': 'fax', 't': 'telex', 'y': 'e-mail', 'n': 'name and address text'}
VALUE_SUB = {'u': 'amount', 'c': 'currency'}


def parse(text):
    """Return a list of groups; each group is a list of field dicts."""
    groups, pos, cur = [], 0, []
    for m in TOKEN.finditer(text):
        gap = text[pos:m.start()]
        if gap not in ('', ' ', '  ') and pos != 0:
            raise ValueError('unexpected text %r before %s' % (gap, m.group(1)))
        if gap == '  ' and cur:
            groups.append(cur)
            cur = []
        pos = m.end()
        name, body = m.group(1), m.group(2)
        cur.append(_field(name, body))
    if text[pos:].strip():
        raise ValueError('trailing text %r' % text[pos:])
    if cur:
        groups.append(cur)
    return groups


def _field(name, body):
    parts = body.split(',')
    tag = parts[0]
    if tag == 'TEXT':
        return dict(name=name, tag='TEXT', kind=parts[1], literal=','.join(parts[2:]), type='literal')
    base, _, suffix = tag.partition('-')
    f = dict(name=name, tag=tag, base=base, suffix=suffix, type=parts[1])
    if len(parts) > 2:
        f['sub'] = parts[2]
    if parts[1] == '*':
        cols = parts[2:]
        if len(cols) % 4:
            raise ValueError('%s: table columns not in fours: %r' % (name, cols))
        f['columns'] = [dict(tag=cols[i], type=cols[i + 1], sub=cols[i + 2], name=cols[i + 3]) for i in range(0, len(cols), 4)]
        f.pop('sub', None)
    elif len(parts) > 3:
        raise ValueError('%s: too many parts %r' % (name, body))
    if f['type'] not in TYPES:
        raise ValueError('%s: unknown type %r' % (name, f['type']))
    return f


def load(name):
    with open(os.path.join(SRC, name + '.txt'), encoding='utf8') as fh:
        return parse(fh.read().strip())


if __name__ == '__main__':
    import sys
    g = load(sys.argv[1] if len(sys.argv) > 1 else 'TFUTICRT')
    print(json.dumps(g, indent=1)[:2000])
