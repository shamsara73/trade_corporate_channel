"""Check outward-message mappings against TI's event field list for their event.

Every tag must be an event field of the event that sends the message, with the type the
mapping gives it. TI fills a tag it does not know with nothing, so a wrong code shows up
only as an empty field in production; this check finds it first.

    python3 -I tools/mappingdoc/check_mappings.py      # exit 1 on a problem

Table column codes (CID, SCHD, ...) are local to their table and are not checked.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import eventfields_lib as E  # noqa: E402
import mapping_lib as M  # noqa: E402

# (mapping file, event field list key)
MAPPINGS = [
    ('sources/ti_outbound/TFFINREJ.txt', 'FSA_All:Create'),
    ('docs/outbound/proposed/TFFRPACK.txt', 'FSA_All:Pay'),
    ('docs/outbound/proposed/TFFRPREJ.txt', 'FSA_All:Pay'),
]

# In the bank's own TFFINREJ, so accepted by TI, but not in the exported list: bank-defined
# fields (the export has only "evPUO1", user defined code 1; tAATT is a custom table).
BANK_DEFINED = {'PUO1', 'tAATT'}

# The mapping's type letter for the list's section type, where they differ. "c" is the
# short form the bank uses for a party's customer ("DebitPartyId<<B+DB,c>>").
MAPPING_TYPE = {'c': 'p'}


def check(path, key, lists):
    fields = lists[key][0]
    types = {}
    for code, typ in fields:
        types.setdefault(code, set()).add(typ)
    problems, rows = [], []
    with open(os.path.join(E.ROOT, path), encoding='utf8') as fh:
        groups = M.parse(fh.read().strip())
    for f in (f for g in groups for f in g):
        if f['tag'] == 'TEXT':
            continue
        typ = MAPPING_TYPE.get(f['type'], f['type'])
        if f['tag'] in BANK_DEFINED:
            rows.append((f['name'], f['tag'], 'bank-defined'))
        elif f['tag'] not in types:
            problems.append('%s: %s <<%s>> is not a field of %s' % (path, f['name'], f['tag'], key))
        elif typ not in types[f['tag']]:
            problems.append('%s: %s <<%s,%s>> has type %s in %s' % (
                path, f['name'], f['tag'], f['type'], '/'.join(sorted(types[f['tag']])), key))
        else:
            rows.append((f['name'], f['tag'], fields[(f['tag'], typ)]))
    return problems, rows


def main(argv):
    lists = E.load_all()
    problems = []
    for path, key in MAPPINGS:
        found, rows = check(path, key, lists)
        problems += found
        if '-v' in argv:
            print('== %s (%s)' % (path, key))
            for name, tag, desc in rows:
                print('  %-20s %-6s %s' % (name, tag, desc))
    for p in problems:
        print(p)
    print('%d mappings checked, %d problems' % (len(MAPPINGS), len(problems)))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
