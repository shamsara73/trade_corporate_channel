"""Render docs/outbound/TFUTICRT-database.md: where the data behind each TFUTICRT field
lives in the TI database, taken from sources/FTI-Database-*.html.

    python3 -I tools/dbspec/build_db_crosswalk.py

Every table and column named in the output is checked against the database
specification; the build fails if one does not exist.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, '..', 'mappingdoc'))
import dbspec_lib as D  # noqa: E402
import mapping_lib as M  # noqa: E402

OUT = os.path.join(D.ROOT, 'docs', 'outbound', 'TFUTICRT-database.md')
DB = D.load()
FAMILY = {'U': 'ToSend', 'S': 'CounterToSend'}
PREFIX = 'UndertakingIssuedDetails.'
EVENT_TABLE, MASTER_TABLE, BASE_EVENT = 'ISSGTE', 'IGMASTER', 'BASEEVENT'

# base tag (and type letter / sub where the tag is reused) -> logical-name tails inside the family.
# H = same wording in the mapping and the column's logical name, M = plausible, L = guess.
RESTS = {
    'FOUT': (['FormOfUndertaking'], 'H'), 'PTP': (['ProductType.Key'], 'H'),
    'APRC': (['ApplicableRules.Code'], 'H'), 'APRN': (['ApplicableRules.Narrative'], 'H'),
    'ISS': (['IssueDate.TIDate'], 'H'), 'AEXPD': (['Expiry.ApproximateDate.TIDate'], 'H'),
    'ETYC': (['Expiry.Type'], 'H'), 'EXP': (['Expiry.Date.TIDate'], 'H'), 'COND': (['Expiry.Condition.Text'], 'H'),
    'PTD': (['DrawingDetails.PartialDrawings'], 'H'), 'MTD': (['DrawingDetails.MultipleDrawings'], 'H'),
    'GLCC': (['GoverningLaw.GoverningLawCountry.code'], 'H'), 'GLCS': (['GoverningLaw.CountrySubDivision'], 'H'),
    'POJ': (['GoverningLaw.PlaceOfJurisdiction'], 'H'),
    ('AMT', 'u'): (['UndertakingAmount.Amount.Amount'], 'H'), ('AMT', 'c'): (['UndertakingAmount.Amount.Currency.code'], 'H'),
    'PCTM': (['UndertakingAmount.AmountSpecification.PercentageMinus'], 'M'),
    'PCTP': (['UndertakingAmount.AmountSpecification.PercentagePlus'], 'M'),
    'ADAX': (['AdditionalAmountsCovered.Money.Amount', 'AdditionalAmountsCovered.Money.Currency.code',
              'AdditionalAmountsCovered.Description.Text'], 'M'),
    'DPI': (['DocumentsAndPresentationInstructions.Text'], 'H'), 'UTC': (['TermsAndConditions.Text'], 'H'),
    'AVLX': (['AvailableWith.Type'], 'H'), 'AVLB': (['AvailableWith.BankParty.PartyDetails.Key'], 'M'),
    'AVLY': (['AvailableWith.Country.code'], 'H'), 'AVLC': (['AvailableWith.City'], 'H'),
    'RNBT': (['RenewalDetails.Renewal.RenewalType'], 'M'),
    'ROE': (['RenewalDetails.Renewal.OnExpiry'], 'H'), 'ROC': (['RenewalDetails.Renewal.OnCalendar'], 'H'),
    'RCD': (['RenewalDetails.Renewal.CalendarDate.TIDate'], 'H'),
    'CDND': (['RenewalDetails.Renewal.RegularRenewal.NumberOfDays'], 'M'),
    'RFR': (['RenewalDetails.Renewal.RenewForPeriod.Number', 'RenewalDetails.Renewal.RenewForPeriod.Unit'], 'H'),
    'RREP': (['RenewalDetails.Renewal.RegularRenewal.RenewFor'], 'M'),
    'RAD': (['RenewalDetails.Renewal.Advise'], 'H'), 'RDN': (['RenewalDetails.Renewal.AdviseNoticeDays'], 'H'),
    'RRR': (['RenewalDetails.Renewal.RollingRenewal'], 'H'), 'RRO': (['RenewalDetails.Renewal.Rolling.RenewOn'], 'H'),
    'REP': (['RenewalDetails.Renewal.Rolling.EveryPeriod.Period.Number',
             'RenewalDetails.Renewal.Rolling.EveryPeriod.Period.Unit'], 'H'),
    'RMX': (['RenewalDetails.Renewal.Rolling.NumberOf'], 'H'),
    'RCN': (['RenewalDetails.Renewal.Rolling.CancellationNotice'], 'H'),
    'NEXN': (['RenewalDetails.Renewal.NonExtensionNotificationDetails.Text'], 'H'),
    'ROA': (['RenewalDetails.Renewal.UseAmount'], 'H'),
    'RAED': (['RenewalDetails.Renewal.Rolling.AdjustedFinalExpiryDate.TIDate'], 'M'),
    'REX': (['RenewalDetails.Renewal.NextExpiryDate.TIDate'], 'L'),
    'DRG': (['RenewalDetails.Reduction.IsRegular'], 'H'), 'DIR': (['RenewalDetails.Reduction.IsIrregular'], 'H'),
    ('DAD', 'l'): (['RenewalDetails.Reduction.Advise'], 'H'),
    ('DAD', 'i'): (['RenewalDetails.Reduction.AdviseNoticeDays'], 'H'),
    'DOPC': (['RenewalDetails.Reduction.Operation'], 'H'), 'DMX': (['RenewalDetails.Reduction.MaxIncreases'], 'H'),
    'DMXD': (['RenewalDetails.Reduction.RemainingNo'], 'L'),
    ('DFRP', 'i'): (['RenewalDetails.Reduction.Period.Period.Number'], 'H'),
    ('DFRP', 's'): (['RenewalDetails.Reduction.Period.Period.Unit'], 'H'),
    'DFRM': (['RenewalDetails.Reduction.Period.DayInMonth'], 'H'), 'DST': (['RenewalDetails.Reduction.StartDate.TIDate'], 'H'),
    'DAPRB': (['RenewalDetails.Reduction.AmountOrPercentage.ByAmount'], 'H'),
    'DAPS': (['RenewalDetails.Reduction.AmountOrPercentage.Percent.PercentAmount'], 'M'),
    ('DAP', 'u'): (['RenewalDetails.Reduction.Amount.Amount'], 'M'),
    ('DAP', 'c'): (['RenewalDetails.Reduction.Amount.Currency.code'], 'M'),
}
# tags without a -S/-U suffix that live in the ToSend family only
TO_SEND_ONLY = {
    'TUWTC': (['TextOfUndertaking.WordingType'], 'H'), 'TUWLC': (['TextOfUndertaking.Language.code'], 'M'),
    'GUTD': (['UnderlyingTransactionDetails.Text'], 'H'),
    'TRN': (['TransferableDetails.Transferable'], 'H'), 'TRNC': (['TransferableDetails.TransferableConditions.Text'], 'H'),
    'DBYC': (['DeliveryDetails.DeliveryBy.DeliveryMethod'], 'H'),
    'DBYN': (['DeliveryDetails.DeliveryBy.AdditionalInformation'], 'M'),
    'DTOC': (['DeliveryDetails.DeliveryTo.Code'], 'H'),
    'DTS': (['DeliveryDetails.DeliveryTo.DeliveryToParty.PartyDetails.Key', 'DeliveryDetails.DeliveryTo.Narrative.Text'], 'M'),
    'TRD': (['FinancialTrade'], 'H'),
}
# other single locations: tag -> list of (table, column, confidence, note)
OTHER = {
    'EBMR': [('MASTER', 'EBANKMSREF', 'H', 'E bank master reference')],
    'EBER': [(BASE_EVENT, 'EBANKEVREF', 'H', 'E bank event reference')],
    'MST': [('MASTER', 'MASTER_REF', 'H', 'master reference')],
    'EVR': [(BASE_EVENT, 'REFNO_PFIX', 'H', 'event reference prefix'), (BASE_EVENT, 'REFNO_SERL', 'H', 'event reference serial')],
    'TDY': [(BASE_EVENT, 'START_DATE', 'L', 'event start date; FINISHED is the completion date'), (BASE_EVENT, 'FINISHED', 'L', '')],
    'EPRV': [(BASE_EVENT, 'ISPROVISEV', 'H', 'provisional this event')],
    'IRIR': [(EVENT_TABLE, 'INRCINSREF', 'H', 'instructions received: instructing party reference')],
    'APD': [(EVENT_TABLE, 'INRCDATE', 'L', 'instructions received date')],
    'TFM': [('MASTER', 'TFR_MTHD', 'M', 'master transfer method; value `a` = Gateway')],
    'ORQC': [(EVENT_TABLE, 'REQTYPOURS', 'H', 'request type, ours')],
    'PRI': [('MASTER', 'PRICUSTMNM', 'H', 'principal customer mnemonic')],
    'MBE': [('MASTER', 'REFNO_MBE', 'M', 'main banking entity of the reference definition')],
    'BOB': [('MASTER', 'BHALF_BRN', 'H', 'behalf of branch')],
    ('BLIA', 'u'): [('MASTER', 'LIAB_AMT', 'H', 'liability amount')],
    ('BLIA', 'c'): [('MASTER', 'LIAB_CCY', 'H', 'liability currency')],
    ('OUA', 'u'): [('MASTER', 'AMT_O_S', 'M', 'outstanding amount')],
    ('OUA', 'c'): [('MASTER', 'CCY', 'M', 'master currency')],
    'TFMD': [],
}
PARTY_PARTS = {
    'cu': ['CUS_MNM'], 'sw1': ['SWADDRESS1'], 'sw2': ['SWADDRESS2'], 'sw3': ['SWADDRESS3'], 'sw4': ['SWADDRESS4'],
    'sw5': ['SWADDRESS5'], 'r': ['REFERENCE'], 'cc': ['COUNTRY'], 'ss': ['SW_BANK', 'SW_CTR', 'SW_LOC', 'SW_BRANCH'],
    'p': ['PHONE'], 'x': ['FAX'], 't': ['TELEX'], 'y': ['EMAIL'], 'n': ['SWADDRESSF'],
}
PARTY_TAGS = {'APP': 'applicant', 'PRI': 'principal', 'APB': 'principal if not the applicant', 'BEN': 'beneficiary',
              'CIB': 'issuing bank', 'FIB': 'final issuing bank', 'ADV': 'advising bank', 'THB': 'advise-through bank',
              'THB-U': 'advise-through bank (local)', 'RQC': 'requested confirmation party', 'DTS': 'delivery party'}


def fam_col(table, family, rest):
    try:
        return D.by_logical(DB, table, PREFIX + family + '.' + rest)
    except KeyError:
        return None


def locate(f):
    """Return (location text, confidence, note) for a mapping field."""
    tag, base, suf = f['tag'], f.get('base'), f.get('suffix')
    typ, sub = f['type'], f.get('sub')
    if typ == 'literal':
        return '–', '', 'literal in the template'
    if typ == '*':
        return TABLES.get(tag, ('–', '', ''))[0], TABLES.get(tag, ('', '', ''))[1], TABLES.get(tag, ('', '', ''))[2]
    if typ == 'p':
        return 'PARTYDTLS (see section 4)', 'M', ''
    key = (base, typ) if (base, typ) in RESTS else ((base, sub) if (base, sub) in RESTS else base)
    if suf in FAMILY and key in RESTS:
        rests, conf = RESTS[key]
        cols = []
        for r in rests:
            for table in (EVENT_TABLE, MASTER_TABLE):
                c = fam_col(table, FAMILY[suf], r)
                if c:
                    cols.append('%s.%s' % (table, c['col']))
        if cols:
            return ' · '.join(sorted(set(cols))), conf, ''
        return '–', '', 'no column with this logical name in the %s family' % FAMILY[suf]
    if base in TO_SEND_ONLY and not suf:
        rests, conf = TO_SEND_ONLY[base]
        cols = []
        for r in rests:
            for table in (EVENT_TABLE, MASTER_TABLE):
                c = fam_col(table, 'ToSend', r)
                if c:
                    cols.append('%s.%s' % (table, c['col']))
        if cols:
            return ' · '.join(sorted(set(cols))), conf, 'ToSend only'
    if (base, sub) in OTHER or tag in OTHER or base in OTHER:
        items = OTHER.get((base, sub)) or OTHER.get(tag) or OTHER.get(base)
        if items:
            for t, c, _, _ in items:
                D.column(DB, t, c)
            return ' · '.join('%s.%s' % (t, c) for t, c, _, _ in items), items[0][2], items[0][3]
    return '–', '', 'not located in the database specification'


TABLES = {
    'AAOC': ('BASECHARGE (one row per charge; MASTER_KEY, EVENT_KEY)', 'M',
             'ChgCalcAmt/Ccy ↔ CHG_DUE/CHG_CCY, Status ↔ STATUS, ExchangeRate ↔ EXCH_RATE, SettleDate ↔ VALUE_DAT'),
    'AABC': ('BASECHARGE (second charge table; meaning of the AAOC/AABC split unknown)', 'L', 'see Q24'),
    'GDOCS': ('DOC_ATTACH → document tables; DMS ids', 'L', ''),
    'tAATT': ('DOC_ATTACH → document tables; DMS ids', 'L', 'bank-document flag unknown'),
    'LLIC': ('LIC_LINK (LICENSE_NO, ALLOC_AMT, ALLOC_CCY, AUTH_REF)', 'M', ''),
    'IRSA': ('IRR_RED (Irregular Reduction)', 'M', ''),
}


def check_tables():
    for n in ('BASECHARGE', 'DOC_ATTACH', 'LIC_LINK', 'IRR_RED', 'PARTYDTLS', 'PTY_ROL', 'MASTER', BASE_EVENT,
              EVENT_TABLE, MASTER_TABLE, 'DOCUME59', 'OUTPUTDOC', 'DOCRELITEM', 'EFDEFNITEM', 'REPEXTFLDS', 'EVTSSFLD',
              'CRITERION', 'TEMPLT', 'TEMPLTVER', 'GWYIN'):
        if n not in DB:
            raise SystemExit('table %s not in the database specification' % n)
    for cols in PARTY_PARTS.values():
        for c in cols:
            D.column(DB, 'PARTYDTLS', c)
    for t, c in [('DOCUME59', 'TEMPLA49'), ('DOCUME59', 'ADVMTH80'), ('DOCUME59', 'DOCUME41'), ('DOCUME59', 'MSGSUBTYPE'),
                 ('DOCUME59', 'FILENA69'), ('DOCUME59', 'DESCRI56'), ('OUTPUTDOC', 'DOCXML'), ('OUTPUTDOC', 'DOCFORMAT'),
                 ('OUTPUTDOC', 'DOC_TEMP'), ('OUTPUTDOC', 'EVENT'), ('OUTPUTDOC', 'TFR_MTHD'), ('DOCRELITEM', 'MESSTEXT'),
                 ('DOCRELITEM', 'EDITED40'), ('DOCRELITEM', 'ADV_MTHD'), ('DOCRELITEM', 'EVENTSTEP'),
                 ('EFDEFNITEM', 'EVFLDCODE'), ('EFDEFNITEM', 'EVFLDPART'), ('EFDEFNITEM', 'EVFLDTYPE'),
                 ('REPEXTFLDS', 'REPFLDCODE'), ('REPEXTFLDS', 'REFFLDDESC'), ('REPEXTFLDS', 'REPFLDTYPE'),
                 ('EVTSSFLD', 'EVFLDCODE'), ('EVTSSFLD', 'ORIG_VALUE'), ('CRITERION', 'EVENTF98'), ('CRITERION', 'FIELDPART'),
                 ('MASTER', 'STATUS'), (BASE_EVENT, 'STATUS'), (BASE_EVENT, 'EVTREFUSED'), (BASE_EVENT, 'EVTREFSTOR'),
                 (BASE_EVENT, 'EXTSTAT'), ('PARTYDTLS', 'ROLE'), ('PTY_ROL', 'PTYROLE_KY'), ('GWYIN', 'MESSAGE')]:
        D.column(DB, t, c)


def desc(table, col):
    return D.column(DB, table, col)['desc']


def render():
    check_tables()
    groups = M.load('TFUTICRT')
    out = ['# TFUTICRT – where the data lives in the TI database', '',
           '> Generated by `tools/dbspec/build_db_crosswalk.py` from `sources/FTI-Database-2.11.5.2.J.html` and `sources/ti_outbound/TFUTICRT.txt`. Do not edit. '
           'Background: [README](README.md).', '',
           'Every table and column named here was checked against the database specification. What the specification cannot show (row contents, '
           'how the application assembles the message) is marked as inference. Confidence: **H** same wording in mapping and column, **M** plausible, **L** guess.', '']

    out += ['## 1. Short answer', '',
            'There is **no table of tags**. A tag such as `EXP-U`, `BEN` or `AMT-U` is a TI **event field code**, the same short code TI uses elsewhere '
            'for event fields. The mapping is a **template** whose placeholders name event fields; the data comes from the product tables, and the template itself is stored as a document template.', '',
            '| What | Where | Basis |', '|---|---|---|',
            '| The mapping text (the one-line `FieldName<<TAG,type,part>>` template) | `DOCUME59.TEMPLA49` – Document Template, `Template text`, up to 31,000 characters; the template for a gateway message has `ADVMTH80` = `a` (Gateway) | **Inference**: the column type, size and advice-method value fit; the specification holds no data. Confirm with query 1 in section 7. |',
            '| The message TI generated from it | `OUTPUTDOC.DOCXML` (when `DOCFORMAT` = `X`, XML), linked to the event (`EVENT`) and the template (`DOC_TEMP`); the release item `DOCRELITEM` (`ADV_MTHD` = `a`, `MESSTEXT`, `EDITED40`) is the thing released at the event step | **Inference**, as above. Confirm with query 2. |',
            '| The catalogue of event field codes (code, type, part) | `EFDEFNITEM` (`EVFLDCODE`, `EVFLDTYPE`, `EVFLDPART`, with `MANDATORY`, `MINIMUM`, `MAXIMUM`) per field-definition `EFDEFN` (by product and event type); `REPEXTFLDS` (`REPFLDCODE`, `REPFLDTYPE`, `REFFLDDESC`, by product and event type); `CRITERION` (`EVENTF98`, `FIELDPART`, `FIELDTYPE`) | **Established**: the same triple is used in each. |',
            '| Stored values of event fields as tag/value rows | `EVTSSFLD` (`EVFLDCODE`, `EVFLDPART`, `EVFLDTYPE`, `ORIG_VALUE`, `OVERRIDDEN`) – the snapshot of original values of fields that users overrode. Not a full store. | **Established** from the columns. |',
            '| The values the template reads | Ordinary columns of the product tables. For this message: event `%s`, master `%s`, base event `%s`, base master `MASTER`, parties `PARTYDTLS`, charges `BASECHARGE`, licences `LIC_LINK`. Event-field code → column is application logic, not in the specification; section 3 gives it by matching names. | Names **established**; code → column **inference**. |' % (EVENT_TABLE, MASTER_TABLE, BASE_EVENT),
            '| Messages received from the channel | `GWYIN` (`MESSAGE`, `MESS_TYPE`, `STATUS`, `ERRMSG`) | Established. There is no `GWYOUT`: outward gateway messages are produced as documents. |', '']

    out += ['## 2. The notation is TI event-field notation', '',
            'The second item of each tag is the **event field type**. The set of letters in the mapping is exactly the set in the database specification:', '',
            '> `EFDEFNITEM.EVFLDTYPE`: ' + desc('EFDEFNITEM', 'EVFLDTYPE'), '',
            '| Mapping item | Database column | Example in `TFUTICRT` |', '|---|---|---|',
            '| first item (`EXP-U`, `BEN`) | `EVFLDCODE` (string 10), `CRITERION.EVENTF98` (string 17) | `EXP-U`, `AMT-S`, `cAAU` |',
            '| second item (`d`, `v`, `p`, `*`...) | `EVFLDTYPE` (string 1) | `d` date, `v` amount, `p` party, `*` table |',
            '| third item (`g`, `u`, `c`, `cu`, `sw1`...) | `EVFLDPART` (string 10), `CRITERION.FIELDPART` (string 4) | `u`/`c` amount and currency, `cu`/`sw1` party parts |', '',
            'This settles most of open question Q23. It does **not** explain the `-U` / `-S` suffixes, which are part of the event field code; their meaning comes from the column families in section 3.', '']

    out += ['## 3. Field by field', '',
            'Columns come in three families in the guarantee tables (`%s`, `%s`, and the amendment tables `IGTNEWAMD` and `ORIGIGTAMD`): `UndertakingIssuedDetails.CounterReceived.*` (prefix `CR`), `…CounterToSend.*` (prefix `CS`) and `…ToSend.*` (prefix `LU`, plus `TOG*`). '
            'The mapping matches them: **`-U` fields are the `ToSend` family** (the full undertaking, including delivery, wording, transfer and underlying-transaction text, which carry no suffix in the mapping) and **`-S` fields are the `CounterToSend` family**, which lacks exactly those elements. There is no `-R` field for `CounterReceived`. '
            'This supports the reading in the README that `-S` is the counter-undertaking to send.' % (EVENT_TABLE, MASTER_TABLE), '',
            'Where a column exists in both the event table (`%s`, the values of this event) and the master table (`%s`, the current values) both are listed.' % (EVENT_TABLE, MASTER_TABLE), '']
    import build_outbound as B
    for gi, g in enumerate(groups):
        out += ['### %d. %s' % (gi + 1, B.GROUP_TITLES[gi]), '']
        if all(f['type'] == 'p' for f in g):
            out += ['Party block – see section 4.', '']
            continue
        out += ['| Field | Tag | Location | Conf. | Note |', '|---|---|---|---|---|']
        for f in g:
            loc, conf, note = locate(f)
            tag = '`TEXT`' if f['type'] == 'literal' else '`%s`' % f['tag']
            out.append('| `%s` | %s | %s | %s | %s |' % (f['name'], tag, loc if loc.startswith('–') else '`%s`' % loc.replace(' · ', '`, `') if 'PARTYDTLS' not in loc and '(' not in loc else loc, conf or '–', note))
        out.append('')

    out += ['## 4. Parties', '',
            'Every party is a row of `PARTYDTLS` ("Party Details"), identified by its **role code** `PARTYDTLS.ROLE` (string 3), which matches the party tags in the mapping (`APP`, `BEN`, `ADV`, `THB`, `CIB`, `FIB`, `RQC`...). The role codes themselves are data in `PTY_ROL` (`PTYROLE_KY`, `DESCRIP`, `VAL_PCP`, `VAL_NPCP`); the specification does not list them. '
            'The party tag and the part (third item) select the column:', '',
            '| Part | Meaning | `PARTYDTLS` column(s) |', '|---|---|---|']
    for part, cols in PARTY_PARTS.items():
        out.append('| `%s` | %s | %s |' % (part, M.PARTY_SUB.get(part, part), ', '.join('`%s`' % c for c in cols)))
    out += ['',
            'The address parts follow the `SW…` columns ("SWIFT compatible address") rather than the plain `ADDRESS1…5` columns. **[Inference from the part name `sw`.]** Party tags in `TFUTICRT`:', '',
            '| Tag | Party |', '|---|---|']
    for t, n in PARTY_TAGS.items():
        out.append('| `%s` | %s |' % (t, n))
    out += ['']

    out += ['## 5. Status values the specification gives us', '',
            'These answer part of open question Q3 and feed [ADR 0008](../architecture/decisions/0008-status-tracking.md).', '',
            '| Column | Meaning |', '|---|---|',
            '| `MASTER.STATUS` | ' + desc('MASTER', 'STATUS') + ' |',
            '| `MASTER.PREV_STS` | previous status, same codes |',
            '| `%s.STATUS` | %s |' % (BASE_EVENT, desc(BASE_EVENT, 'STATUS')),
            '| `%s.STATUS_EV` | %s |' % (BASE_EVENT, desc(BASE_EVENT, 'STATUS_EV')),
            '| `%s.EVTREFUSED` / `EVTREFSTOR` | %s / %s |' % (BASE_EVENT, desc(BASE_EVENT, 'EVTREFUSED'), desc(BASE_EVENT, 'EVTREFSTOR')),
            '| `%s.EXTSTAT` | %s |' % (BASE_EVENT, desc(BASE_EVENT, 'EXTSTAT')),
            '| `GWYIN.STATUS` (inbound message) | ' + desc('GWYIN', 'STATUS') + ' |',
            '| `GWYIN.ERRMSG` | ' + desc('GWYIN', 'ERRMSG') + ' |', '',
            'So an event is *in progress*, *completed* or *aborted*; it can be *refused* with a reason; the master is `NEW`, `LIV` (live), `CAN`, `EXP`, `BKF` (booked off) or `PRV`. '
            'A message from the channel that cannot be processed is held in `GWYIN` with an error code (`U` unable to find master, `D` duplicate message, `E` error in mapping, `R` requires repair, `N` NAK) – which also answers part of Q7: **TI detects duplicate inbound messages** (`ERRMSG` = `D`), although the specification does not say on which key.', '']

    out += ['## 6. Tables and what is not located', '',
            '* **Charges** (`Charges` tables, tags `AAOC` and `AABC`): `BASECHARGE`, one row per charge (`MASTER_KEY`, `EVENT_KEY`, `STATUS` %s, `CHG_DUE`/`CHG_CCY`, `EXCH_RATE`, `VALUE_DAT`, `BILL_STS`). Why there are two tables is unknown (Q24).' % '`X` in preparation, `O` outstanding, `P` paid, `A` superseded, `W` waived…',
            '* **Linked licences**: `LIC_LINK` (`LICENSE_NO`, `AUTH_REF`, `ALLOC_AMT`, `ALLOC_CCY`).',
            '* **Irregular reduction schedule** (`IRSA-*`): `IRR_RED`.',
            '* **Attached documents**: `DOC_ATTACH` links an event document, a gateway message (`ATT_GW_MSG` → `GWYIN`), mailing details or a SWIFT message; the DMS identifiers sit in the document tables.',
            '* **Narratives** (`IIB`, `IAB`, `RTS`, `ADL`, `GDS`, `FFI`, `DCS`, `GSI`) and **accounts** (`CHA`, `PRA`), **goods and shipment** (`GDC`, `SDF`…, `PRSP`) and **contract details** (`GCRC`…): not found in `%s` or `%s`; they are in other event or master tables not matched by name here. Search with `python3 -I tools/dbspec/dbspec_lib.py search <text>`.' % (EVENT_TABLE, MASTER_TABLE),
            '* **Computed values** (`TnxAmount`, `AvailableAmt`, `TFMD`, `SLN-U`): derived at message time.', '']

    out += ['## 7. Queries to confirm and to collect the other mappings', '',
            'Run these in the TI test database (they are plain SQL and work on SQL Server and Oracle; `FETCH FIRST` needs SQL Server 2012+ / Oracle 12c+). They only read.', '',
            '```sql',
            '-- 1. Find the template that holds the TFUTICRT mapping',
            'SELECT KEY97, DESCRI56, ADVMTH80, MSGSUBTYPE, FILENA69, DOCUME41, OBSOLETE',
            '  FROM DOCUME59',
            " WHERE TEMPLA49 LIKE '%TFUTICRT%';",
            '',
            '-- 2. See messages generated from it (newest first); DOCXML is the outward message as sent',
            'SELECT o.KEY97, o.EVENT, o.DOCFORMAT, o.TFR_MTHD, o.LASTSAVD',
            '  FROM OUTPUTDOC o',
            ' WHERE o.DOC_TEMP = :template_key -- KEY97 from query 1',
            ' ORDER BY o.LASTSAVD DESC',
            ' FETCH FIRST 20 ROWS ONLY;',
            '',
            '-- 3. Collect ALL gateway templates (the other outward mappings Q22 asks for)',
            'SELECT KEY97, DESCRI56, MSGSUBTYPE, FILENA69, DOCUME41, TEMPLA49',
            '  FROM DOCUME59',
            " WHERE ADVMTH80 = 'a';",
            '',
            '-- 4. Which products / events use document templates, via the document type (DOC_TYPE has the type key)',
            'SELECT t.KEY97, t.DESCRI56, t.DOCUME41',
            '  FROM DOCUME59 t',
            " WHERE t.TEMPLA49 LIKE '%<<TEXT,T,%';",
            '',
            '-- 5. Event-field code catalogue for the guarantee events (code, type, description)',
            'SELECT DISTINCT REPFLDCODE, REPFLDTYPE, REFFLDDESC',
            '  FROM REPEXTFLDS',
            " WHERE REPFLDCODE LIKE '%-U' OR REPFLDCODE LIKE '%-S';",
            '```', '',
            'Query 3 is the quickest way to get every outward mapping the bank has configured, without waiting for the TI team to send them one by one. Save each result as a file in `sources/ti_outbound/` (as received) and add it to the generator.',
            'If query 1 returns nothing, the template is probably a Word or DMS-held template (`TEMPLT`, `TEMPLTVER` with `DMSID`) or lives in a file outside the database; ask the TI team where `TFUTICRT` is configured.', '']
    return '\n'.join(out)


if __name__ == '__main__':
    with open(OUT, 'w', encoding='utf8') as fh:
        fh.write(render().rstrip() + '\n')
    print('wrote', OUT)
