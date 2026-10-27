# TI outward messages (response mappings)

> Part of the Trade Corporate Channel design set. Catalogue: [01](../01-interface-catalogue.md) · Functional specification: [02](../02-functional-specification.md) · Architecture: [0004](../architecture/decisions/0004-schema-catalogue.md), [0005](../architecture/decisions/0005-gateway-adapter.md), [0008](../architecture/decisions/0008-status-tracking.md).

| Message | Source | Reference |
|---|---|---|
| `IGT_IIG_All_E.csv` – TI event field list for the Issue Undertaking event | `sources/ti_outbound/IGT_IIG_All_E.csv` (received from the TI team) | [event-fields-IGT-IIG.md](event-fields-IGT-IIG.md) (every available event field, name families, coverage by `TFUTICRT`) |
| `TFUTIACK` – acknowledgement of the Issue event | `sources/ti_outbound/TFUTIACK.txt` | [TFUTIACK.md](TFUTIACK.md) (same fields as `TFUTICRT`) |
| `TFUTIREJ` – refusal of the Issue event | `sources/ti_outbound/TFUTIREJ.txt` | [TFUTIREJ.md](TFUTIREJ.md) (19 fields, no reason code) |
| `TFFINREJ` – refusal of the Finance Standalone create event | `sources/ti_outbound/TFFINREJ.txt` (received 2026-10-26) | [sample](../samples/finance-standalone/create/3-ti-response-TFFINREJ.xml); fields reused for the repayment responses |
| `TFFRPACK`, `TFFRPREJ` – Finance Standalone **repayment** completed / refused | **proposed by the channel team** in `docs/outbound/proposed/`, for the bank to set up in TI | [finance-repayment.md](finance-repayment.md) (ready to paste; every code checked against the FSA list) |
| `FSA_All_All_E.csv` – TI event field list for **every** Finance Standalone event (17 events) | `sources/ti_outbound/FSA_All_All_E.csv` (received 2026-10-27) | search with `eventfields_lib.py search <text> FSA_All:<event>`; `check_mappings.py` checks the FSA mappings against it |
| `TFUTICRT` – Undertaking Issued, create | `sources/ti_outbound/TFUTICRT.txt` (received from the TI team) | [TFUTICRT.md](TFUTICRT.md) (fields and crosswalk to the inbound schema) · [TFUTICRT-database.md](TFUTICRT-database.md) (where the data lives in the TI database) |

> **Process.** How `TFIGTAPP`, `TFUTICRT`, `TFUTIACK` and `TFUTIREJ` fit together for the IGT issue: [igt-issue-process.md](igt-issue-process.md).

> **Delivery.** The SDK Systems Interfacing Guide says outward gateway messages are sent on release as a `ServiceRequest` with `Service` = `GATEWAY` and `Operation` = `DOCS` or the message name, with no functional reply expected. See [SDK guides](../reference/ti-sdk-guides.md), section 1.4. **Seen in the bank's test (2026-10-23):** a finance completion produced `GATEWAY.TFFINACK` with `MessageName` `TFFSAACK`, one flat element per field and amounts in minor units; the full message is in [working samples](../reference/working-samples.md), section 2. Its mapping document has not been received yet.

## 1. What this is

Everything documented so far (`TF*` messages in `sources/xsd_element_docs/`) flows **from the channel into TI**. The TI team has now provided the first document that goes the other way: a **mapping that defines the content of a message TI produces**. It is one very long line of `FieldName<<tag,type,sub>>` definitions, 292 fields in 36 groups.

Evidence that it describes TI's **response to the customer** after an undertaking is issued **[Assumption]**:

* the message name literal is `TFUTICRT` (the inbound message is `TFUTIAPP`; `TFUTICRT` does not appear in the gateway documentation);
* a bank comment literal reads "We have Issued this Undertaking";
* it echoes the channel's own references (`eBankMasterRef`, `eBankEventRef`) and adds TI's (`MasterRef`, `EventRef`, `TransactionDate`);
* it carries a `ResponseToSender` narrative and the instruction as it was received (`InstructionFromInstructingParty`, `OurRequestTypeCodeSend`, `IssueByCode`);
* it carries data only TI can compute: liability, outstanding and available amounts, charges with status and exchange rates, bank documents.

The mapping gives the **content** of the message. It does not say how the message is **delivered** (transport, wire format, acknowledgement). That is the main open item (Q21).

## 2. Notation

First decoded from this one document; then **checked against the TI database specification** (`sources/FTI-Database-*.html`, see [TFUTICRT-database.md](TFUTICRT-database.md)). The database specification confirms that a tag is a TI **event field**: first item = event field code (`EVFLDCODE`), second = event field type (`EVFLDTYPE`, the same letters), third = event field part (`EVFLDPART`). Rows marked *Confirmed* rest on that; the others are still inferences to confirm with the TI team (Q23).

| Construct | Reading | Confidence |
|---|---|---|
| `FieldName<<TAG,type,sub>>` | A named field: `TAG` = event field code, `type` = event field type, `sub` = event field part | **Confirmed** (database spec) |
| `FieldName<<B+DB,c>>` (`TFFINREJ`) | A finance party's customer mnemonic: `B+DB` debit party, `B+FT` finance-to party (`B+FR` with type `r` is the financing reference). The `TFFINACK` received fills `DebitPartyId` with the customer number | Medium (from the message received) |
| `FieldName <<…>>` (space before `<<`, `TFFINREJ`) | Same as without the space | High |
| `TEXT,T,<literal>` and `TEXT,E,<literal>` | Constant text (`T` the message name, `E` an event comment) | Medium |
| type `s` / `r` / `l` / `i` / `d` / `n` / `a` | string / reference / logical (Y/N) / integer / date / narrative (long text) / account | **Confirmed** (`EVFLDTYPE` list) |
| type `v`, sub `u` or `c` | monetary value: `u` the amount, `c` its currency (two fields share one tag) | Type confirmed; parts inferred |
| type `d`, sub `g` | date in a general format | Low (format unknown) |
| type `p` | a **party**, spread over several fields by `sub`: `cu` customer mnemonic, `sw1` name, `sw2`–`sw5` address lines, `r` reference, `cc` country, `ss` SWIFT BIC, `p` phone, `x` fax, `t` telex, `y` e-mail. The tag is the **party role** (`PARTYDTLS.ROLE`) | Type confirmed; parts match `PARTYDTLS` columns |
| sub `%` | "none" (no facet) | High |
| type `*` | a repeating **table**; followed by columns in fours: `tag,type,sub,ColumnName` | Type confirmed (`*` = Table); layout consistent |
| tag suffix `-U` / `-S` | Part of the event field code. `-U` = the undertaking to send (`ToSend` column family, prefix `LU`), `-S` = the **counter-undertaking to send** (`CounterToSend`, prefix `CS`). The mapping puts delivery, wording, transfer and underlying-transaction fields without suffix, and these exist only in `ToSend` | Medium-high (the column families match the groups) |
| one blank space between fields, two between groups | grouping only | High |
| tags starting with a lower-case `c` or `t` (`cAAU`, `cAAS`, `tAATT`) | bank-defined (custom) items | Low |

The parser (`tools/mappingdoc/mapping_lib.py`) rejects anything outside this grammar, so a mapping that breaks these assumptions fails loudly.

## 3. Where the data lives (database)

There is no table of tags. The mapping text is a document template (`DOCUME59.TEMPLA49`, advice method `a` = Gateway) and the generated message is held as a document (`OUTPUTDOC.DOCXML`); the values come from the product tables (here `ISSGTE`, `IGMASTER`, `MASTER`, `BASEEVENT`, `PARTYDTLS`, `BASECHARGE`, `LIC_LINK`). The first two are **inferences** from column names and types; [TFUTICRT-database.md](TFUTICRT-database.md) lists every field with its table and column, the party columns, status values, and the SQL to confirm and to collect all other gateway templates.

## 4. What `TFUTICRT` contains

| Block | Fields | Notes |
|---|---:|---|
| Identity and flags | 9 | Message name, channel references echoed, **TI master and event references**, transaction date, provisional, final wording, sequence |
| Instruction context | 14 | Instructing party reference, application date, issue-by method, request type, notes, response to sender, additional conditions, goods, SWIFT instructions, documents, additional information |
| Parties | 85 | Applicant, principal, principal if not applicant, beneficiary, issuing bank, final issuing bank, advising bank, advise-through (and local), confirming bank, requested confirmation party |
| Counter-undertaking to send (`-S`) | 65 | Terms, amount, text, available with, renewal, reduction/increase, irregular schedule |
| Undertaking as issued (`-U`) | 80 | The same, plus liability, outstanding and available amount, wording type, transfer |
| Delivery, confirmation, charges, accounts, contract, shipment | 34 | Delivery method, who pays charges, charge and principal accounts, contract details, shipment and product |
| Tables | 5 | Charges (two), attached documents (two), linked licences. Two more tables, the irregular repayment schedules, are counted in their `-S` and `-U` groups, so there are 7 tables in all |

## 5. What it changes in the design

1. **There is a structured completion signal.** **Corrected (2026-10-19):** `TFUTICRT` is TI creating a transaction (TI-initiated); the completion signals are `TFUTIACK` and `TFUTIREJ` ([igt-issue-process.md](igt-issue-process.md)). The earlier reading of `TFUTICRT` as the completion signal for undertakings, which follows, is superseded. It answers part of Q3 and Q4: status need not be inferred only from polling. Polling remains the fallback.
2. **TI returns its references.** `MasterRef` and `EventRef` come back, together with the channel's own `eBankMasterRef` and `eBankEventRef`. That closes ILC-7 for this product and validates the reconciliation strategy in [ADR 0003](../architecture/decisions/0003-database-and-outbox.md): the channel must send its references on every inbound message, which was already a rule.
3. **The channel can show the undertaking as issued, not only as requested.** The `-U` fields are what TI actually booked, which the TSO may have changed. A before/after comparison is possible for every field with a counterpart (see the crosswalk in [TFUTICRT.md](TFUTICRT.md)).
4. **New data the customer never entered**: liability, outstanding and available amounts, a charges table (calculated amount, status, exchange rate, equivalent amount, inception and settlement dates, periodic type), attached bank documents, linked licences. These are what a customer wants to see after issuance.
5. **Bank-specific fields exist** (`cAAU` custom expiry date, `cAAS` effective date, `PUO1` sub-product code, `GDCD` goods-code description). Outward mappings are **per-bank configuration**, so the channel must read them from the mapping documents, not from code.
6. **Outward messages are not in the gateway documentation.** Every outward message the channel needs (issued, amended, cancelled, claim, payment...) needs its own mapping from the TI team. Only `TFUTICRT` exists so far.
7. **UTI vs IGT/ISB.** The first mapping provided is for the Undertaking model. That suggests the bank uses `UTI` for guarantees and standbys, but it is a hint, not an answer; the question in [Undertaking Issued](../products/undertaking-issued.md) stays open.

## 6. Consequences for the architecture

* [0004](../architecture/decisions/0004-schema-catalogue.md): the `catalogue` module gets a second kind of definition, the **outward message definition**, parsed from `sources/ti_outbound/*.txt` by the same generator. Keys are the mapping's field names; there are no inbound schema paths.
* [0005](../architecture/decisions/0005-gateway-adapter.md): the adapter gets an **outward message parser** and a receiver port; the transport is unknown (Q21).
* [0008](../architecture/decisions/0008-status-tracking.md): a fourth status source, TI outward messages.
* Backlog: new task T13 (outward message ingestion), see [task backlog](../agents/task-backlog.md).

## 7. Questions for the TI team

| # | Question |
|---|---|
| Q21 | How is the outward message delivered: what transport, what wire format (the mapping defines content only), which endpoint, authentication, acknowledgement, retry, and ordering? |
| Q22 | Which events produce an outward message? Is there a mapping for each (issue, amend, cancel, claim, payment, correspondence, and for each product)? Please send them all. How is a refusal or an abort reported (a different `BankComment` literal, or a different message)? |
| Q23 | Mostly settled by the database specification (event field code/type/part). Still to confirm: `T`/`E` literals, the date format `g`, `%`, the `-S` / `-U` reading, lower-case-prefixed (bank-defined) tags, and how tables are encoded in the delivered message. |
| Q24 | The mapping gives the same tag to different fields (`Amount` and `TnxAmount`, `OutstandingAmount` and `AvailableAmt`) and has two tables each named `Charges` and `AttachedDocuments`. Intended? How are they distinguished in the message? |

## 8. Regenerating

```
python3 -I tools/mappingdoc/build_outbound.py     # TFUTICRT.md
python3 -I tools/dbspec/build_db_crosswalk.py     # TFUTICRT-database.md
python3 -I tools/dbspec/dbspec_lib.py search EBANK   # search the database specification
python3 -I tools/mappingdoc/build_eventfields.py  # event-fields-IGT-IIG.md
python3 -I tools/mappingdoc/eventfields_lib.py search <text>   # find an event field by code or description
```

`TFUTICRT.md` is generated from `sources/ti_outbound/TFUTICRT.txt` and the hand-made crosswalk in `tools/mappingdoc/crosswalk_tfutiapp.py`. The build fails if a crosswalk path is not in the `TFUTIAPP` schema. To add a message: save its mapping as `sources/ti_outbound/<NAME>.txt`, add it to `build_outbound.py`, and write its crosswalk.

## Event field list (IGT issue event)

TI supplied the list of all event fields available for the Issue Undertaking event (`IGT_IIG_All_E.csv`). Findings, detailed in [event-fields-IGT-IIG.md](event-fields-IGT-IIG.md):

* The name suffixes are confirmed by TI's descriptions: `-U` undertaking (as booked), `-R` counter-undertaking received, `-S` counter-undertaking to send, `-M` main.
* 197 of the 199 distinct tags in `TFUTICRT` exist in the list. Two do not: `CMBYC` (probably a typo for `CMBY`) and `tAATT` (a bank-defined code not defined for this event).
* `AvailableAmt` reuses the outstanding-amount tag; the list has `TAVE`, "Available amount after this event", which is probably what was meant. `TnxAmount` reuses the undertaking-amount tag. Both are mapping defects to raise with TI (Q24).
* The charges tables (`AAOC`, `AABC`) and `GDOCS` are explained by the descriptions in the list.
* 245 fields are bank-defined; the standard gateway schema does not describe them (Q25).
