# TI outward messages (response mappings)

> Part of the Trade Corporate Channel design set. Catalogue: [01](../01-interface-catalogue.md) · Functional specification: [02](../02-functional-specification.md) · Architecture: [0004](../architecture/decisions/0004-schema-catalogue.md), [0005](../architecture/decisions/0005-gateway-adapter.md), [0008](../architecture/decisions/0008-status-tracking.md).

| Message | Source | Reference |
|---|---|---|
| `TFUTICRT` – Undertaking Issued, create | `sources/ti_outbound/TFUTICRT.txt` (received from the TI team) | [TFUTICRT.md](TFUTICRT.md) (generated) |

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

Decoded from this one document; **every row below is an inference to confirm with the TI team (Q23)**.

| Construct | Reading | Confidence |
|---|---|---|
| `FieldName<<TAG,type,sub>>` | A named field; `TAG` is TI's short code for the data item, `type` its kind, `sub` a facet | High |
| `TEXT,T,<literal>` and `TEXT,E,<literal>` | Constant text (`T` the message name, `E` an event comment) | Medium |
| type `s` / `r` / `l` / `i` / `d` / `n` | string / reference / logical (Y/N) / integer / date / narrative (long text) | High |
| type `a` | account number | Medium |
| type `v`, sub `u` or `c` | monetary value: `u` the amount, `c` its currency (two fields share one tag) | High |
| type `d`, sub `g` | date in a general format | Low (format unknown) |
| type `p` | a **party**, spread over several fields by `sub`: `cu` customer mnemonic, `sw1` name, `sw2`–`sw5` address lines, `r` reference, `cc` country, `ss` SWIFT BIC, `p` phone, `x` fax, `t` telex, `y` e-mail | High |
| sub `%` | "none" (no facet) | High |
| type `*` | a repeating **table**; followed by columns in fours: `tag,type,sub,ColumnName` | High (the document is consistent) |
| tag suffix `-U` / `-S` | `-U` the undertaking as issued; `-S` the **counter-undertaking to send** (the inbound schema has `Undertaking`, `CounterReceived`, `CounterToSend`; the field names end in `Send`) | Medium |
| one blank space between fields, two between groups | grouping only | High |
| tags starting with a lower-case `c` or `t` (`cAAU`, `cAAS`, `tAATT`) | bank-defined (custom) items | Low |

The parser (`tools/mappingdoc/mapping_lib.py`) rejects anything outside this grammar, so a mapping that breaks these assumptions fails loudly.

## 3. What `TFUTICRT` contains

| Block | Fields | Notes |
|---|---:|---|
| Identity and flags | 9 | Message name, channel references echoed, **TI master and event references**, transaction date, provisional, final wording, sequence |
| Instruction context | 14 | Instructing party reference, application date, issue-by method, request type, notes, response to sender, additional conditions, goods, SWIFT instructions, documents, additional information |
| Parties | 85 | Applicant, principal, principal if not applicant, beneficiary, issuing bank, final issuing bank, advising bank, advise-through (and local), confirming bank, requested confirmation party |
| Counter-undertaking to send (`-S`) | 65 | Terms, amount, text, available with, renewal, reduction/increase, irregular schedule |
| Undertaking as issued (`-U`) | 80 | The same, plus liability, outstanding and available amount, wording type, transfer |
| Delivery, confirmation, charges, accounts, contract, shipment | 34 | Delivery method, who pays charges, charge and principal accounts, contract details, shipment and product |
| Tables | 5 | Charges (two), attached documents (two), linked licences. Two more tables, the irregular repayment schedules, are counted in their `-S` and `-U` groups, so there are 7 tables in all |

## 4. What it changes in the design

1. **There is a structured completion signal.** The arrival of `TFUTICRT` with the "issued" comment is the event for state `Completed` (doc 02, section 5) for undertakings. It answers part of Q3 and Q4: status need not be inferred only from polling. Polling remains the fallback.
2. **TI returns its references.** `MasterRef` and `EventRef` come back, together with the channel's own `eBankMasterRef` and `eBankEventRef`. That closes ILC-7 for this product and validates the reconciliation strategy in [ADR 0003](../architecture/decisions/0003-database-and-outbox.md): the channel must send its references on every inbound message, which was already a rule.
3. **The channel can show the undertaking as issued, not only as requested.** The `-U` fields are what TI actually booked, which the TSO may have changed. A before/after comparison is possible for every field with a counterpart (see the crosswalk in [TFUTICRT.md](TFUTICRT.md)).
4. **New data the customer never entered**: liability, outstanding and available amounts, a charges table (calculated amount, status, exchange rate, equivalent amount, inception and settlement dates, periodic type), attached bank documents, linked licences. These are what a customer wants to see after issuance.
5. **Bank-specific fields exist** (`cAAU` custom expiry date, `cAAS` effective date, `PUO1` sub-product code, `GDCD` goods-code description). Outward mappings are **per-bank configuration**, so the channel must read them from the mapping documents, not from code.
6. **Outward messages are not in the gateway documentation.** Every outward message the channel needs (issued, amended, cancelled, claim, payment...) needs its own mapping from the TI team. Only `TFUTICRT` exists so far.
7. **UTI vs IGT/ISB.** The first mapping provided is for the Undertaking model. That suggests the bank uses `UTI` for guarantees and standbys, but it is a hint, not an answer; the question in [Undertaking Issued](../products/undertaking-issued.md) stays open.

## 5. Consequences for the architecture

* [0004](../architecture/decisions/0004-schema-catalogue.md): the `catalogue` module gets a second kind of definition, the **outward message definition**, parsed from `sources/ti_outbound/*.txt` by the same generator. Keys are the mapping's field names; there are no inbound schema paths.
* [0005](../architecture/decisions/0005-gateway-adapter.md): the adapter gets an **outward message parser** and a receiver port; the transport is unknown (Q21).
* [0008](../architecture/decisions/0008-status-tracking.md): a fourth status source, TI outward messages.
* Backlog: new task T13 (outward message ingestion), see [task backlog](../agents/task-backlog.md).

## 6. Questions for the TI team

| # | Question |
|---|---|
| Q21 | How is the outward message delivered: what transport, what wire format (the mapping defines content only), which endpoint, authentication, acknowledgement, retry, and ordering? |
| Q22 | Which events produce an outward message? Is there a mapping for each (issue, amend, cancel, claim, payment, correspondence, and for each product)? Please send them all. How is a refusal or an abort reported (a different `BankComment` literal, or a different message)? |
| Q23 | Confirm the notation in section 2, in particular `T`/`E` literals, the date format `g`, the meaning of `%`, the `-S` / `-U` suffixes, lower-case-prefixed tags, and how tables are encoded. |
| Q24 | The mapping gives the same tag to different fields (`Amount` and `TnxAmount`, `OutstandingAmount` and `AvailableAmt`) and has two tables each named `Charges` and `AttachedDocuments`. Intended? How are they distinguished in the message? |

## 7. Regenerating

```
python3 -I tools/mappingdoc/build_outbound.py
```

`TFUTICRT.md` is generated from `sources/ti_outbound/TFUTICRT.txt` and the hand-made crosswalk in `tools/mappingdoc/crosswalk_tfutiapp.py`. The build fails if a crosswalk path is not in the `TFUTIAPP` schema. To add a message: save its mapping as `sources/ti_outbound/<NAME>.txt`, add it to `build_outbound.py`, and write its crosswalk.
