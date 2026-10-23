"""Generate docs/products/*.md from the product definitions and the FTI
Gateway documentation in sources/.

    python3 -I tools/specgen/build.py

Hand-written business content lives in defs_*.py. Everything derived from the
schema (field tables, enumerations, nested structures, sample XML) is produced
here, so regenerating after a gateway upgrade shows exactly what changed.
"""
import glob
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import specgen_lib as L  # noqa: E402
from shapes import SHAPES  # noqa: E402
import defs_import  # noqa: E402
import defs_export  # noqa: E402
import defs_other  # noqa: E402

OUT = os.path.join(L.ROOT, 'docs', 'products')
PRODUCTS = defs_import.PRODUCTS + defs_export.PRODUCTS + defs_other.PRODUCTS
ALL = sorted(os.path.basename(f)[3:-8] for f in glob.glob(L.XSD + '/TI.TF*.xsd.txt'))
ALL = [c for c in ALL if not (c.endswith('DTO') or c.endswith('CTO'))]
CTX = {'Branch': 'HOBR', 'Customer': 'ACMEIMP'}
MAX_ROWS = 60


def slug(s):
    return re.sub(r'[^a-z0-9 -]', '', s.lower()).replace(' ', '-')


# ---------------------------------------------------------------- shapes

def assign_shapes():
    assigned, loose = {}, {}
    for canon, _title, _sample in SHAPES:
        sig = set(L.top_names(canon))
        for o in ALL:
            so = set(L.top_names(o))
            j = len(sig & so) / max(1, len(sig | so))
            if j >= 0.92 and o not in assigned:
                assigned[o] = (canon, j)
            elif j >= 0.85 and o not in loose:
                loose[o] = (canon, j)
    count = {}
    for o, (c, _) in assigned.items():
        count[c] = count.get(c, 0) + 1
    keep = {c for c, n in count.items() if n >= 2}
    assigned = {o: v for o, v in assigned.items() if v[0] in keep}
    loose = {o: v for o, v in loose.items() if v[0] in keep and o not in assigned}
    return assigned, loose, keep


ASSIGNED, LOOSE, KEPT = assign_shapes()
SHAPE_TITLE = {c: t for c, t, _ in SHAPES}


def shape_ref(code):
    if code in ASSIGNED:
        return ASSIGNED[code][0], False
    if code in LOOSE:
        return LOOSE[code][0], True
    return None, False


def shape_link(canon, prefix=''):
    return '[Shape: %s](%sshared-message-shapes.md#%s)' % (
        SHAPE_TITLE[canon], prefix, slug('Shape: ' + SHAPE_TITLE[canon]))


# ---------------------------------------------------------------- message sections

def diff_lines(code, base):
    a, b = L.top_names(code), L.top_names(base)
    plus = [n for n in a if n not in b]
    minus = [n for n in b if n not in a]
    return plus, minus


def group_expansion(root, names):
    out = []
    for n in root.children:
        if n.name in names and n.children and not L.is_party(n) and n.name not in L.SKELETON:
            rows = L.expand(n)
            if len(rows) > MAX_ROWS:
                extra = len(rows) - MAX_ROWS
                rows = rows[:MAX_ROWS] + ['| … | | %d more paths – see `TI.<message>.xsd.txt` for `%s` |' % (extra, n.name)]
            if rows:
                out.append('**`%s`**%s\n\n| Path | Type / length | Description |\n|---|---|---|\n%s\n' % (
                    n.name, ' (repeating)' if n.rep else '', '\n'.join(rows)))
    return out


def full_message(code, level, only=None, with_nested=True):
    root = L.parse(code)
    nodes = [n for n in root.children if n.name not in L.SKELETON and (only is None or n.name in only)]
    h = '#' * level
    out = [table(L.field_rows(nodes))]
    if with_nested:
        ex = group_expansion(root, {n.name for n in nodes})
        if ex:
            out.append('\n%s Nested structures of `%s`\n\n' % (h, code) + '\n'.join(ex))
    en = L.enum_rows(root, {n.name for n in nodes})
    if en:
        out.append('\n%s Enumerated values in `%s`\n\n| Path | Allowed values |\n|---|---|\n%s\n' % (h, code, '\n'.join(en)))
    return '\n'.join(out)


def table(rows):
    return L.table(rows)


def message_section(P, msg, first_in_product, documented, level=3):
    code = msg['code']
    p, creates = L.purpose(code)
    h = '#' * level
    out = ['%s `%s` – %s\n' % (h, code, msg['title'])]
    out.append('| | |\n|---|---|\n| Creates in TI | %s |\n| Purpose (schema) | %s |\n| Channel exposure | %s |\n| Phase | %s |\n' % (
        creates or '-', L.esc(p) or '-', msg['exposure'], msg['phase']))
    if msg.get('note'):
        out.append(msg['note'] + '\n')
    canon, loose = shape_ref(code)
    base = msg.get('baseline')
    if canon and not base:
        plus, minus = diff_lines(code, canon)
        out.append('**Body:** %s%s.' % (shape_link(canon), ' with differences' if (plus or minus or loose) else ''))
        if plus or minus:
            out.append('')
            if plus:
                out.append('* Adds: ' + ', '.join('`%s`' % x for x in plus))
            if minus:
                out.append('* Omits: ' + ', '.join('`%s`' % x for x in minus))
            if plus:
                out.append('')
                root = L.parse(code)
                out.append(L.table(L.field_rows([n for n in root.children if n.name in plus])))
                for g in group_expansion(root, set(plus)):
                    out.append('\n' + g)
        out.append('')
        return '\n'.join(out), code
    if msg['exposure'] == 'Bank-side' and not base:
        out.append('Not exposed to customers, so the body is not specified here. Top-level elements, in schema order: '
                   + ', '.join('`%s`' % n for n in L.top_names(code)) + '. Details: `sources/xsd_element_docs/TI.%s.xsd.txt`.\n' % code)
        return '\n'.join(out), None
    if base:
        plus, minus = diff_lines(code, base)
        bname = msg.get('baseline_label', base)
        out.append('**Body:** same elements as `%s` (%s)%s.\n' % (bname, msg.get('baseline_where', 'see above'),
                                                                   ', with the differences below' if (plus or minus) else ''))
        if minus:
            out.append('* Omits: ' + ', '.join('`%s`' % x for x in minus))
        if plus:
            out.append('* Adds:\n')
            root = L.parse(code)
            out.append(L.table(L.field_rows([n for n in root.children if n.name in plus])))
            for g in group_expansion(root, set(plus)):
                out.append('\n' + g)
            en = L.enum_rows(root, set(plus))
            if en:
                out.append('\n| Path | Allowed values |\n|---|---|\n' + '\n'.join(en))
        out.append('')
        return '\n'.join(out), code
    # same-product near-duplicate
    best = L.best_match(L.top_names(code), [(c, L.top_names(c)) for c in documented])
    if best[1] and best[0] >= 0.75:
        plus, minus = diff_lines(code, best[1])
        out.append('**Body:** same elements as `%s` (section above)%s.\n' % (best[1], ', with the differences below' if (plus or minus) else ''))
        if minus:
            out.append('* Omits: ' + ', '.join('`%s`' % x for x in minus))
        if plus:
            out.append('* Adds:\n')
            root = L.parse(code)
            out.append(L.table(L.field_rows([n for n in root.children if n.name in plus])))
            for g in group_expansion(root, set(plus)):
                out.append('\n' + g)
            en = L.enum_rows(root, set(plus))
            if en:
                out.append('\n| Path | Allowed values |\n|---|---|\n' + '\n'.join(en))
        out.append('')
        return '\n'.join(out), code
    out.append('**Body elements** (besides the [common blocks](README.md#common-blocks-in-every-message)):\n')
    out.append(full_message(code, level + 1))
    out.append('')
    return '\n'.join(out), code


# ---------------------------------------------------------------- product doc

FLOW = {
    'issue': """sequenceDiagram
    autonumber
    actor Maker as Corporate maker
    actor Appr as Corporate approver
    participant CH as Channel
    participant GW as FTI Gateway
    participant TI as TI Plus
    actor TSO as Bank TSO
    Maker->>CH: Complete %(what)s, attach documents
    CH->>CH: Validate (schema rules, entitlements)
    Maker->>CH: Submit for approval
    Appr->>CH: Approve
    CH->>GW: ServiceRequest %(msg)s (CorrelationId = X)
    GW->>TI: Create %(event)s
    GW-->>CH: ServiceResponse Status = SUCCEEDED
    CH->>CH: Status = Received by bank
    TI->>TSO: Event enters orchestration
    TSO->>TI: Review, enrich, authorise
    TI-->>CH: Notification or enquiry shows result
    CH->>Maker: Status updated""",
    'respond': """sequenceDiagram
    autonumber
    actor Maker as Corporate maker
    actor Appr as Corporate approver
    participant CH as Channel
    participant GW as FTI Gateway
    participant TI as TI Plus
    actor TSO as Bank TSO
    TI-->>CH: %(trigger)s
    CH->>Maker: Task / alert: action required
    Maker->>CH: Prepare %(what)s
    Appr->>CH: Approve
    CH->>GW: ServiceRequest %(msg)s (MasterRef = existing master)
    GW->>TI: Create %(event)s
    GW-->>CH: ServiceResponse Status = SUCCEEDED
    TI->>TSO: Event enters orchestration
    TSO->>TI: Review and authorise
    TI-->>CH: Notification or enquiry shows result
    CH->>Maker: Status updated""",
}

RESPONSE_COMMON = """| `Status` | Channel behaviour |
|---|---|
| `SUCCEEDED` | TI accepted the message and created the event. Mark `ReceivedByBank`; keep `Warning` / `Info` texts. |
| `FAILED` | TI rejected the message. Show `Details.Error` to the maker and return the draft. No automatic retry. |
| `UNAVAILABLE` / timeout | Keep in the outbox, retry with back-off, and confirm by enquiry (`eBankEventRef`) before each re-send so no second event is created. |

Rules shared by every product are in [README – Common behaviour](README.md#common-behaviour)."""


def render_product(P):
    out = []
    A = out.append
    A('# %s' % P['title'])
    A('')
    A('> Part of the product specifications. Index and shared rules: [README](README.md). '
      'Shared message bodies: [shared message shapes](shared-message-shapes.md). '
      'Catalogue: [01](../01-interface-catalogue.md) · Functional spec: [02](../02-functional-specification.md).')
    A('')
    A('| | |')
    A('|---|---|')
    A('| Priority | %s |' % P['priority'])
    A('| Customer role | %s |' % P['role'])
    A('| TI product code (`Context.Product`) | %s |' % P.get('product_text', '`%s` **[Assumption – confirm in TI]**' % P['product']))
    A('| Messages | %d |' % len(P['messages']))
    A('')
    A('## 1. Purpose and scope')
    A('')
    A(P['intro'])
    A('')
    A('Out of scope: %s' % P['out_of_scope'])
    A('')
    A('> **Reading guide.** Field names, lengths and enumerations come from `sources/xsd_element_docs/`. '
      'Statements marked **[Proposed]** or **[Assumption]** are design decisions or guesses that the interface documents cannot confirm. '
      'Names are copied exactly from the schema, including its inconsistencies in capitalisation and spelling.')
    A('')
    A('## 2. Messages')
    A('')
    A('| Message | Kind | Creates in TI | Channel exposure | Phase | Body |')
    A('|---|---|---|---|---|---|')
    for m in P['messages']:
        _, cr = L.purpose(m['code'])
        canon, loose = shape_ref(m['code'])
        if m.get('baseline'):
            body = 'as `%s`' % m.get('baseline_label', m['baseline'])
        elif canon:
            body = shape_link(canon) + (' (+diff)' if loose else '')
        else:
            body = 'own'
        A('| `%s` | %s | %s | %s | %s | %s |' % (m['code'], m['kind'], cr or '-', m['exposure'], m['phase'], body))
    A('')
    A('**Exposure key.** *Customer* – the corporate starts or answers it in the channel. '
      '*Review* – schema description does not make the sender clear; confirm with the TI team before exposing. '
      '*Bank-side* – raised by the bank or an interbank counterparty; do not expose to customers.')
    A('')
    A('## 3. Business scenario')
    A('')
    A(P['scenario'])
    A('')
    for fl in P['flows']:
        A('**%s**' % fl['title'])
        A('')
        A('```mermaid')
        A(FLOW[fl['kind']] % fl)
        A('```')
        A('')
    A('## 4. Preconditions')
    A('')
    for i, x in enumerate(P['pre'], 1):
        A('%d. %s' % (i, x))
    A('')
    A('## 5. Screens')
    A('')
    A(P['screens_intro'])
    A('')
    A('| Step | Section | Fields (schema element) |')
    A('|---|---|---|')
    for i, (sec, fields) in enumerate(P['screens'], 1):
        A('| %d | **%s** | %s |' % (i, sec, fields))
    A('')
    A('> **[Proposed] Fields hidden from the customer or set by configuration.** %s' % P['hidden'])
    A('')
    A('## 6. Primary message `%s`' % P['primary'])
    A('')
    pm = [m for m in P['messages'] if m['code'] == P['primary']][0]
    pp, pc = L.purpose(P['primary'])
    A('%s. Creates: %s.' % (L.esc(pp) or 'Primary instruction of this product', pc or '-'))
    A('')
    A('### 6.1 Context')
    A('')
    A('| Element | Value |')
    A('|---|---|')
    A('| `Branch` | The booking branch from the corporate configuration. |')
    A('| `Customer` | The customer\'s TI mnemonic (max 20). |')
    A('| `Product` | %s |' % P.get('context_product_text', '`%s` **[Assumption]**' % P['product']))
    A('| `Event` | %s |' % P.get('event_note', 'Normally implied by the message name. Leave empty unless the TI team asks for it. **[Assumption]**'))
    A('| `TheirReference` | Customer\'s own reference (max 34). |')
    A('| `OurReference` / `MasterRef` | %s |' % P['master_note'])
    A('')
    A('The remaining `Context` elements (`Team`, `Step`, `BehalfOfBranch*`, `TemplateIdentifier`, `EventReference`, `ExchangeNonAutoComplete`, `SubsidiaryMasterPrefixAndSerial`) are set by configuration. See [README](README.md#common-blocks-in-every-message).')
    A('')
    A('### 6.2 Channel-managed elements')
    A('')
    A(P['channel_managed'])
    A('')
    A('### 6.3 Field reference')
    A('')
    base = pm.get('baseline')
    if base:
        plus, minus = diff_lines(P['primary'], base)
        A('`%s` carries the same top-level elements as `%s` (%s). Differences:\n' % (
            P['primary'], pm.get('baseline_label', base), pm.get('baseline_where', 'see the Import LC specification')))
        if minus:
            A('* Omits: ' + ', '.join('`%s`' % x for x in minus))
        A('* Adds:\n')
        root = L.parse(P['primary'])
        A(L.table(L.field_rows([n for n in root.children if n.name in plus])))
        A('')
        for g in group_expansion(root, set(plus)):
            A(g)
        en = L.enum_rows(root, set(plus))
        if en:
            A('\n| Path | Allowed values |\n|---|---|\n' + '\n'.join(en))
        A('')
        A('For the shared elements, use the field reference in %s.' % pm.get('baseline_where', 'the Import LC specification'))
    else:
        A('All top-level elements, in schema order, then the nested structures and enumerations.\n')
        A(full_message(P['primary'], 4))
    A('')
    if P.get('extra_sections'):
        for t, body in P['extra_sections']:
            A('### %s' % t)
            A('')
            A(body)
            A('')
    A('## 7. Other messages')
    A('')
    documented = [P['primary']]
    for m in P['messages']:
        if m['code'] == P['primary']:
            continue
        sec, code = message_section(P, m, False, documented)
        A(sec)
        if code:
            documented.append(code)
    A('## 8. Validation rules')
    A('')
    A('### 8.1 Rules supported directly by the schema')
    A('')
    A(P['schema_rules'])
    A('')
    A('### 8.2 Business rules to apply in the channel')
    A('')
    A('Normal practice for this product and the checks a TSO would otherwise perform by hand. **[Proposed]**: switch each on or off per bank policy.')
    A('')
    A('| # | Rule |')
    A('|---|---|')
    for i, r in enumerate(P['rules'], 1):
        A('| V%d | %s |' % (i, r))
    A('')
    A('### 8.3 Minimum content for a usable instruction')
    A('')
    A(P['minimum'])
    A('')
    A('## 9. Illustrative requests')
    A('')
    A('Values are invented. Element names, prefixes and order follow `sources/example_xml/`. Elements not shown are omitted, which the schema allows for optional elements. **Test against the gateway before use.**')
    A('')
    ctx = dict(CTX)
    ctx['Product'] = P['product']
    ctx['TheirReference'] = P['their_ref']
    for code, vals in P['samples']:
        c2 = dict(ctx)
        c2.update(P.get('ctx_extra', {}).get(code, {}))
        A('**`%s`**' % code)
        A('')
        A('```xml')
        A(L.sample(code, c2, vals))
        A('```')
        A('')
    A('## 10. Response handling and tracking')
    A('')
    A(RESPONSE_COMMON)
    A('')
    A(P['tracking'])
    A('')
    A('## 11. Exceptions')
    A('')
    A('| Scenario | Handling |')
    A('|---|---|')
    for s, h in P['exceptions']:
        A('| %s | %s |' % (s, h))
    A('| Gateway `FAILED`, `UNAVAILABLE`, timeout, duplicate click | As in [README – Common behaviour](README.md#common-behaviour). |')
    A('')
    A('## 12. Acceptance criteria')
    A('')
    for i, c in enumerate(P['acceptance'] + [
            'Every step is in the audit trail with user, time and `CorrelationId`.',
            'The stored request validates against the schema rules in 8.1 and matches the structure in section 9.'], 1):
        A('%d. %s' % (i, c))
    A('')
    A('## 13. Open items')
    A('')
    A('| # | Item |')
    A('|---|---|')
    for i, o in enumerate(P['open'], 1):
        A('| %s-%d | %s |' % (P['id'], i, o))
    A('')
    return '\n'.join(out)


# ---------------------------------------------------------------- shapes document

def render_shapes():
    out = ['# Shared message shapes', '',
           '> Part of the product specifications. Index and shared rules: [README](README.md).', '',
           'Many `TF*` messages have the same body. They are documented once here. A product specification links to the shape and lists only what differs. '
           'A shape\'s canonical message is the one its field table and sample are taken from. Members matched at 92% or better by element name; '
           '"Loose" members matched at 85–92% and are listed with their differences.', '']
    members = {}
    for o, (c, j) in ASSIGNED.items():
        members.setdefault(c, []).append((o, j, False))
    for o, (c, j) in LOOSE.items():
        members.setdefault(c, []).append((o, j, True))
    for canon, title, sample in SHAPES:
        if canon not in KEPT:
            continue
        out.append('## Shape: %s' % title)
        out.append('')
        p, cr = L.purpose(canon)
        out.append('Canonical message: `%s` – %s. Creates: %s.' % (canon, L.esc(p) or title, cr or '-'))
        out.append('')
        out.append('| Member | Match | Differences from the canonical body |')
        out.append('|---|---:|---|')
        for o, j, loose in sorted(members[canon]):
            plus, minus = diff_lines(o, canon)
            d = []
            if plus:
                d.append('adds ' + ', '.join('`%s`' % x for x in plus))
            if minus:
                d.append('omits ' + ', '.join('`%s`' % x for x in minus))
            out.append('| `%s` | %d%%%s | %s |' % (o, round(j * 100), ' (loose)' if loose else '', '; '.join(d) or 'none'))
        out.append('')
        out.append('### Body elements')
        out.append('')
        out.append(full_message(canon, 4))
        out.append('')
        out.append('### Sample (`%s`)' % canon)
        out.append('')
        out.append('```xml')
        ctx = dict(CTX)
        ctx.update(sample.get('ctx', {}))
        out.append(L.sample(canon, ctx, sample['values']))
        out.append('```')
        out.append('')
        if sample.get('notes'):
            out.append(sample['notes'])
            out.append('')
    return '\n'.join(out)


def main():
    os.makedirs(OUT, exist_ok=True)
    for P in PRODUCTS:
        text = render_product(P)
        with open(os.path.join(OUT, P['file']), 'w', encoding='utf8') as f:
            f.write(text.rstrip() + '\n')
        print('wrote', P['file'], len(text.splitlines()), 'lines')
    with open(os.path.join(OUT, 'shared-message-shapes.md'), 'w', encoding='utf8') as f:
        f.write(render_shapes().rstrip() + '\n')
    print('wrote shared-message-shapes.md')
    import readme
    with open(os.path.join(OUT, 'README.md'), 'w', encoding='utf8') as f:
        f.write(readme.render(PRODUCTS, ASSIGNED, LOOSE, ALL).rstrip() + '\n')
    covered = {m['code'] for P in PRODUCTS for m in P['messages']}
    print('uncovered messages:', [c for c in ALL if c not in covered])


if __name__ == '__main__':
    main()
