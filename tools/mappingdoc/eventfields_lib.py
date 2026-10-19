"""Reader for TI event field lists exported as CSV (sources/ti_outbound/*_All_E.csv).

File format (one file per product/event, e.g. IGT_IIG_All_E.csv):

    "Iss Und","Issue","A product/event","<Section>",     <- 5 cells: starts a section
    "<code>","<description>",                            <- 3 cells: one event field

Sections are the TI event field types (Branch, Party, Amount, Date, Logical, ...), so the
key of a field is (code, type letter): the same code can appear under several types.

    python3 -I tools/mappingdoc/eventfields_lib.py search 'renewal'
    python3 -I tools/mappingdoc/eventfields_lib.py code EXP-U
"""
import csv
import glob
import html
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SECTION_TYPE = {'Branch': 'b', 'Customer address': 'm', 'Country': 'y', 'Account': 'a', 'Currency': 'z', 'Team': 'g',
                'Timestamp': 't', 'User': 'u', 'Table': '*', 'Integer': 'i', 'Party': 'p', 'Amount': 'v', 'Date': 'd',
                'Logical': 'l', 'Reference': 'r', 'Narrative': 'n', 'String': 's', 'List': '+', 'Address': '-',
                'Party settlement': 'q', 'System account': 'f', 'Watch list checker party': 'x'}
FAMILY = {'U': 'Undertaking', 'R': 'Counter received', 'S': 'Counter to send', 'M': 'Main'}


def load(path):
    """Return {(code, type): description} in file order, plus the list of sections."""
    fields, sections, cur = {}, [], None
    with open(path, encoding='utf-8-sig', newline='') as fh:
        for row in csv.reader(fh):
            if not row:
                continue
            if len(row) >= 5 and row[0] == 'Iss Und':
                name = row[3]
                if name not in SECTION_TYPE:
                    raise ValueError('%s: unknown section %r' % (os.path.basename(path), name))
                cur = SECTION_TYPE[name]
                sections.append((name, cur))
            elif cur is None:
                raise ValueError('field before first section: %r' % row)
            else:
                fields.setdefault((row[0], cur), html.unescape(row[1]).strip())
    return fields, sections


def load_all():
    """{file stem: (fields, sections)} for every *_All_E.csv under sources/ti_outbound."""
    out = {}
    for p in sorted(glob.glob(os.path.join(ROOT, 'sources', 'ti_outbound', '*_All_E.csv'))):
        out[os.path.basename(p)[:-len('_All_E.csv')]] = load(p)
    return out


def family(code):
    m = re.search(r'-([URSM])$', code)
    return m.group(1) if m else ''


def main(argv):
    allf = load_all()
    if len(argv) >= 2 and argv[0] in ('search', 'code'):
        rx = re.compile(argv[1] if argv[0] == 'search' else '^' + re.escape(argv[1]) + '$', re.I)
        for stem, (f, _) in allf.items():
            for (c, t), d in f.items():
                if rx.search(c if argv[0] == 'code' else c + ' ' + d):
                    print('%s | %s | %s | %s' % (stem, c, t, d))
    else:
        print(__doc__)


if __name__ == '__main__':
    main(sys.argv[1:])
