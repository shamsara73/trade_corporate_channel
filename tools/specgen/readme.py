"""README.md of docs/products: index, shared blocks, shared behaviour."""
import glob
import os
import specgen_lib as L

ILC_ROW = dict(id='ILC', file='import-letter-of-credit.md', title='Import Letter of Credit', priority='P1, first release',
               role='Applicant', product='ILC',
               codes=['TFILCAPP', 'TFILCBTB', 'TFILCAMN', 'TFILCCAN', 'TFILCPYR', 'TFILCCLM', 'TFILCCOR', 'TFILCBMR'])


def render(products, assigned, loose, all_codes):
    out = []
    A = out.append
    A('# Product specifications')
    A('')
    A('> Part of the Trade Corporate Channel design set. Catalogue: [01](../01-interface-catalogue.md) · Functional specification: [02](../02-functional-specification.md).')
    A('')
    A('One specification per trade product, written to the same outline as the first one, [Import Letter of Credit](import-letter-of-credit.md). '
      'Together they cover every customer-facing message in the FTI Gateway interface. Bodies shared by many messages are specified once in '
      '[shared message shapes](shared-message-shapes.md).')
    A('')
    A('## Index')
    A('')
    A('| Product | Specification | Customer role | Priority | Messages |')
    A('|---|---|---|---|---:|')
    A('| `ILC` | [%s](%s) | %s | %s | %d |' % (ILC_ROW['title'], ILC_ROW['file'], ILC_ROW['role'], ILC_ROW['priority'], len(ILC_ROW['codes'])))
    for P in products:
        A('| `%s` | [%s](%s) | %s | %s | %d |' % (P['id'], P['title'], P['file'], P['role'], P['priority'], len(P['messages'])))
    A('')
    A('Shared: [shared message shapes](shared-message-shapes.md).')
    A('')
    A('## How each specification is organised')
    A('')
    A('| Section | Content |')
    A('|---|---|')
    A('| 1 Purpose and scope | What the product is, the customer\'s role, what is out of scope. |')
    A('| 2 Messages | Every message of the product, its kind, what it creates in TI, whether the channel exposes it, and the phase. |')
    A('| 3 Business scenario | A worked example and sequence diagrams. |')
    A('| 4 Preconditions | What must be true before the customer can act. |')
    A('| 5 Screens | Step-by-step capture form with the schema element behind every field. |')
    A('| 6 Primary message | `Context` values, channel-managed elements, the field reference, nested structures and enumerations. |')
    A('| 7 Other messages | Each servicing message, either by reference to a shared shape or by its own field table. |')
    A('| 8 Validation | Schema rules, business rules, and the minimum content for a usable instruction. |')
    A('| 9 Illustrative requests | Sample XML. Structure from the gateway\'s example files, values invented. |')
    A('| 10 Response handling and tracking | What to do with each gateway status and how the customer follows progress. |')
    A('| 11 Exceptions | Product-specific exceptional cases. |')
    A('| 12 Acceptance criteria | What "done" means for the product. |')
    A('| 13 Open items | Questions for the TI team and the business. |')
    A('')
    A('Tags: **[Proposed]** is a design decision; **[Assumption]** is something the interface documents do not confirm. '
      'Each product keeps its own open-items list; the cross-cutting questions are in [02, section 11](../02-functional-specification.md).')
    A('')
    A('## Common blocks in every message')
    A('')
    A('Every `TF*` body starts with the same skeleton. These blocks are not repeated in the product specifications.')
    A('')
    r = L.parse('TFILCAPP')
    for n in r.children:
        if n.name in L.SKELETON and n.children:
            A('**`%s`**%s' % (n.name, ' (repeating inner group)' if any(c.rep for c in n.children) else ''))
            A('')
            A('| Path | Type / length | Description |')
            A('|---|---|---|')
            for row in L.expand(n):
                A(row)
            A('')
        elif n.name in L.SKELETON:
            A('* `%s` – %s – %s' % (n.name, L.fmt_type(n.typ), L.short(n.desc, 120)))
    A('')
    A('### Party block')
    A('')
    A('Parties (applicant, beneficiary, sender, banks...) share one structure:')
    A('')
    ap = [c for c in r.children if c.name == 'Applicant'][0]
    A('| Element | Type / length | Description |')
    A('|---|---|---|')
    for c in ap.children:
        if c.children:
            continue
        A('| `%s` | %s | %s |' % (c.name, L.fmt_type(c.typ), L.short(c.desc, 100)))
    A('| `MXAddress` | group | ISO 20022 structured address: `Name` (≤ 140), `Department`, `SubDepartment`, `StreetName`, `BuildingNumber`, `BuildingName`, `Floor`, `PostBox`, `Room`, `PostCode`, `TownName`, `TownLocationName`, `DistrictName`, `CountrySubDivision`, `Country`, `AddressLine1`, `AddressLine2` (≤ 70) |')
    A('| `SwiftTradeAddress` | group | Four lines of ≤ 35 characters plus `Country`, `Name`, `PostCode`, `Town` |')
    A('')
    A('Give either the customer mnemonic (`Customer`) or the free-text name and address. For parties that end up in a SWIFT message prefer `SwiftTradeAddress`. **[Assumption]**')
    A('')
    A('## Common behaviour')
    A('')
    A('These rules apply to every product and are stated once.')
    A('')
    A('| Topic | Rule |')
    A('|---|---|')
    A('| Envelope | `ServiceRequest` with `RequestHeader` (`Service` = `TI`, `Operation` = message name, `Credentials`, `SourceSystem`, `TargetSystem`, `CorrelationId`, `CreationDate`...). See [01, section 2](../01-interface-catalogue.md). |')
    A('| Namespaces | Message-level elements use prefix `m:`; the contents of reusable groups (context, parties, amounts, terms) use `c:`. Sample XML in each specification is generated from the gateway\'s example files and follows this. |')
    A('| Names | Take element names from the schema catalogue for each message. Capitalisation (`eBankMasterRef` / `EBankMasterRef`), singular/plural (`Documentss`, `EmbeddedItemss`) and spelling (`ApplcableRules`, `Acount`) vary and must never be assumed. |')
    A('| Channel references | Send `eBankMasterRef` (constant per master) and `eBankEventRef` (unique per event) on every message that has them. They are the reconciliation key. |')
    A('| Idempotency | Same `eBankEventRef` on every retry. Before re-sending after a timeout or `UNAVAILABLE`, query TI for the event. |')
    A('| Ordering | One in-flight message per master, so events reach TI in business order. |')
    A('| Approval | Maker-checker in the channel before anything is sent; the stored message is exactly what was approved. |')
    A('| Response | `SUCCEEDED` = accepted by TI (not authorised). `FAILED` = show `Details.Error`, return to maker. `UNAVAILABLE` = retry. |')
    A('| Tracking | Gateway response → `SearchMasters` → `RetrieveMasterSummaries` / `RetrieveMasterDetail` by `DefinitionID`, plus `Notifications.All` if enabled. |')
    A('| Attachments | Inline `EmbeddedItems` or prior `DocumentAttachment`; document type from the bank list; size and type limits enforced at upload. |')
    A('| Dates, amounts | Dates are ISO `yyyy-mm-dd`. Amounts are strings (≤ 22 characters) in the schema; agree the decimal format with the TI team. |')
    A('| Text | Free text that TI maps to SWIFT fields must obey the SWIFT character set and line lengths. **[Assumption]** |')
    A('| Hidden fields | Routing and take-on elements (`Team`, `Step`, `BankCodes`, `ExternalReview`, `ProvisionalFlags`...) are set by configuration, never by the customer. |')
    A('| Audit | Store user, time, `CorrelationId`, request and response (without credentials) for every submission. |')
    A('')
    A('## Coverage')
    A('')
    A('Every non-migration `TF*` message belongs to exactly one specification.')
    A('')
    cover = {}
    for P in products:
        for m in P['messages']:
            cover[m['code']] = (P['title'], P['file'])
    for c in ILC_ROW['codes']:
        cover[c] = (ILC_ROW['title'], ILC_ROW['file'])
    A('| Message | Specification |')
    A('|---|---|')
    for c in all_codes:
        if c in cover:
            A('| `%s` | [%s](%s) |' % (c, cover[c][0], cover[c][1]))
        else:
            A('| `%s` | **not covered** |' % c)
    A('')
    mig = sorted(os.path.basename(f)[3:-8] for f in glob.glob(L.XSD + '/TI.TF*.xsd.txt')
                 if os.path.basename(f)[3:-8].endswith(('DTO', 'CTO')))
    A('**Excluded – data take-on (migration):** ' + ', '.join('`%s`' % c for c in mig) + '. They load existing transactions into TI and are not part of the channel.')
    A('')
    A('## Regenerating')
    A('')
    A('All product specifications except [Import Letter of Credit](import-letter-of-credit.md) are generated. Hand-written business content is in `tools/specgen/defs_*.py` and `shapes.py`; '
      'schema-derived content (field tables, nested structures, enumerations, sample XML structure) is read from `sources/`.')
    A('')
    A('```')
    A('python3 -I tools/specgen/build.py')
    A('```')
    A('')
    A('Messages that TI sends to the channel are documented separately in [TI outward messages](../outbound/README.md).')
    A('')
    A('Edit the definitions, not the generated Markdown. After a gateway upgrade, replace `sources/` and regenerate; `git diff` then shows exactly what changed in the interface.')
    return '\n'.join(out)
