"""Reader for TI event field lists exported as CSV (sources/ti_outbound/*_All_E.csv).

File format (IGT_IIG_All_E.csv holds one event; FSA_All_All_E.csv every event of a product):

    "Iss Und","Issue","A product/event","<Section>",     <- 5 cells: starts a section
    "<code>","<description>",                            <- 3 cells: one event field

The first two cells of a section row are the product and the event ("Fince SA","Pay").
A file with several events is read one event at a time; load_all() keys it as
"<stem>:<event>", for example "FSA_All:Pay".

Sections are the TI event field types (Branch, Party, Amount, Date, Logical, ...), so the
key of a field is (code, type letter): the same code can appear under several types.

    python3 -I tools/mappingdoc/eventfields_lib.py search 'renewal'
    python3 -I tools/mappingdoc/eventfields_lib.py code EXP-U
    python3 -I tools/mappingdoc/eventfields_lib.py search 'repay' FSA_All:Pay   # one list only
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


def _rows(path):
    """(event, section name, row) for every field row, in file order."""
    event = section = None
    with open(path, encoding='utf-8-sig', newline='') as fh:
        for row in csv.reader(fh):
            if not row:
                continue
            if len(row) >= 5 and row[2] == 'A product/event':
                event, section = row[1], row[3]
                if section not in SECTION_TYPE:
                    raise ValueError('%s: unknown section %r' % (os.path.basename(path), section))
            elif section is None:
                raise ValueError('field before first section: %r' % row)
            else:
                yield event, section, row


def events(path):
    """The events a list covers, in file order."""
    return list(dict.fromkeys(e for e, _, _ in _rows(path)))


def load(path, event=None):
    """Return {(code, type): description} in file order, plus the list of sections.

    A file with several events needs the event to read."""
    found = events(path)
    if event is None and len(found) > 1:
        raise ValueError('%s covers %d events; name one of %s' % (os.path.basename(path), len(found), found))
    fields, sections = {}, []
    for ev, name, row in _rows(path):
        if event is not None and ev != event:
            continue
        if not sections or sections[-1][0] != name:
            sections.append((name, SECTION_TYPE[name]))
        fields.setdefault((row[0], SECTION_TYPE[name]), html.unescape(row[1]).strip())
    return fields, sections


def load_all():
    """{key: (fields, sections)} for every *_All_E.csv under sources/ti_outbound.

    The key is the file stem (IGT_IIG), or stem:event (FSA_All:Pay) for a file with several events."""
    out = {}
    for p in sorted(glob.glob(os.path.join(ROOT, 'sources', 'ti_outbound', '*_All_E.csv'))):
        stem = os.path.basename(p)[:-len('_All_E.csv')]
        found = events(p)
        if len(found) == 1:
            out[stem] = load(p)
        else:
            for ev in found:
                out['%s:%s' % (stem, ev)] = load(p, ev)
    return out


def family(code):
    m = re.search(r'-([URSM])$', code)
    return m.group(1) if m else ''


def main(argv):
    allf = load_all()
    if len(argv) >= 2 and argv[0] in ('search', 'code'):
        rx = re.compile(argv[1] if argv[0] == 'search' else '^' + re.escape(argv[1]) + '$', re.I)
        for stem, (f, _) in allf.items():
            if len(argv) >= 3 and stem != argv[2]:
                continue
            for (c, t), d in f.items():
                if rx.search(c if argv[0] == 'code' else c + ' ' + d):
                    print('%s | %s | %s | %s' % (stem, c, t, d))
    else:
        print(__doc__)


if __name__ == '__main__':
    main(sys.argv[1:])
