# Finance Standalone repayment – TI responses to set up (`TFFRPACK`, `TFFRPREJ`)

> Part of [TI outward messages](README.md). Proposed by the channel team (2026-10-26) for the bank to configure in TI; codes checked against TI's FSA event field list (2026-10-27). **Not yet set up or tried in TI** (Q38). Samples of the resulting messages: [docs/samples](../samples/README.md#finance-standalone).

When a trade service officer completes or refuses a **repayment** event in TI (the event the channel starts with `TFFSARPY`), TI should send the channel an outward message, as it already does for the create event (`TFFINACK`, `TFFINREJ`). This page gives the two mappings to configure, in TI's notation, ready to paste, for the FSA **`Pay`** event.

| Message | When | Mapping text |
|---|---|---|
| `TFFRPACK` | repayment completed | [proposed/TFFRPACK.txt](proposed/TFFRPACK.txt) (36 fields) |
| `TFFRPREJ` | repayment refused | [proposed/TFFRPREJ.txt](proposed/TFFRPREJ.txt) (23 fields) |

The names follow `TFFINACK` / `TFFINREJ` (`TF` + `FRP` for *finance repayment* + `ACK` / `REJ`) and can be changed. The channel only needs a name that **ends in `ACK` or `REJ`** (Q39). It finds the repayment by `eBankMasterRef` + `eBankEventRef`, which TI stores from the `TFFSARPY` it received.

## 1. The repayment codes

Every tag is a field of the FSA `Pay` event in TI's list (`sources/ti_outbound/FSA_All_All_E.csv`), with the type the mapping uses. `tools/mappingdoc/check_mappings.py` checks this on every build. The codes also exist in `Sched Pay` and `Advise Pay`, so the mapping works if TI books the repayment as one of those. They do **not** exist in `Pay reject`.

| Fields | Code | TI's description | Data (database spec) |
|---|---|---|---|
| `RepaymentReference` | `FRR` | Repayment reference | `FINREPAY.REPAY_REF` |
| `RepaymentType` | `FRTY` | Repayment type | `FINREPAY.REPAY_TYPE` (F final, P principal plus interest, I interest then principal, …) |
| `RepaymentPayer` | `FRPP` | Repayment payer | `FINREPAY.REPAYPAYER` (D debit party, F finance party) |
| `ValueDate` | `RPD` | Repayment date | `FINREPAY.REPAY_DATE` |
| `PrincipalRepaidAmt`, `…Ccy` | `FPR` | Principal repaid | `FINREPAY.PRI_PAID` |
| `InterestRepaidAmt`, `…Ccy` | `FIR` | Interest repaid | `FINREPAY.INT_PAID` |
| `TotalRepaidAmt`, `…Ccy` | `FTR` | Total amount repaid | `FINREPAY.TOT_PAID` |
| `TFAmount`, `TFCurrency` | `B+DA` | Finance deal amount | |
| `OutstandingAmt`, `OutstandingCcy` | `OUA` | Outstanding amount | |
| `MaturityDate` | `B+MD` | Maturity date | |
| `Status` | `MSTS` | Master status | |
| `Provisional` | `MPRV` | Master Provisional (FSA has no event-level `EPRV`, unlike guarantees) | |

**Check against your `TFFINACK` mapping** (not received yet). `TFAmount`, `OutstandingAmt`, `MaturityDate`, `Status` and `Provisional` should use the same codes there as here. Where `TFFINACK` differs, take its code. Two candidates are close enough to matter:
* `TFAmount`: `B+DA` (finance deal amount), or `FAMTE` (financing deal amount event).
* `OutstandingAmt`: `OUA`, `LOUAF` (outstanding amount in finance currency) or `TOML` (total outstanding finance amount at master level).

**Check on the first test in TI:** after a partial repayment, `OutstandingAmt` must show the amount left **after** the repayment (USD 52.00 in the sample). The channel takes TI's figure as the finance's outstanding amount, so a figure from before the repayment would show the finance as unpaid.

## 2. Where each field comes from

| Fields | Tags | Taken from |
|---|---|---|
| `eBankMasterRef`, `eBankEventRef`, `MasterRef`, `EventRef` | `EBMR`, `EBER`, `MST`, `EVR` | `TFFINREJ` (bank) |
| `DebitPartyId`, `FinancingRef`, `FinanceToPartyId`, `BehalfOfBranch`, `MBE`, `ProductTypeCode`, `ProductSubTypeCode` | `B+DB`, `B+FR`, `B+FT`, `BOB`, `MBE`, `PTP`, `PUO1` | `TFFINREJ` |
| `Charges` (two tables), `AttachedDocuments` (two tables) | `AAOC`, `AABC`, `GDOCS`, `tAATT` | `TFFINREJ`, unchanged |
| `TransactionDate`, `Sequence` | `TDY`, `ESQ` | `TFUTIACK` (guarantees); also in the FSA list |
| Repayment and finance figures, `Provisional` | see section 1 | FSA event field list |
| `BankComment` | `TEXT,E,…` | new text; change as the bank likes |

`PUO1` and `tAATT` are not in TI's FSA list (it has only `evPUO1`, "User defined code 1"). They come from the bank's own `TFFINREJ`, so they are taken to be bank-defined fields that the export leaves out. If TI refuses them for `Pay`, drop them: the channel does not read either.

## 3. What the channel does with them

| Field | Use |
|---|---|
| `eBankMasterRef` + `eBankEventRef` | find the repayment event (required) |
| `MessageName` | `…ACK` completes the repayment, `…REJ` refuses it (Q39) |
| `EventRef`, `BankComment` | shown on the transaction page |
| `OutstandingAmt` + `OutstandingCcy` | the finance's new outstanding amount, in **minor units** (`5200` = USD 52.00, Q35). At zero the finance is **repaid**. If TI leaves it empty, the channel subtracts the principal itself |
| `MaturityDate`, `Status` | the finance as TI now has it |
| `AttachedDocuments` | stored; the customer can download them (for example the debit advice) |
| Repayment figures, `TFAmount`, parties, charges | kept with the message (`inbound_msg`); not used yet |

A refused repayment leaves the finance open, and the customer may send another one.

## 4. Checks in the build

`tools/mappingdoc/check_mappings.py` (CI, docs job) checks that every tag is a field of the event, with the mapping's type. `app/src/test/.../gateway/TiResponseMappingTest` reads both mapping files and the bank's `TFFINREJ`, and checks each against its sample in `docs/samples`. Each sample must have every plain field of its mapping (tables appear only when they have rows) and nothing else. Each mapping must carry the fields the channel reads. `FsaFlowTest` runs a repayment to completion and to refusal with these samples. So a mapping changed here must be changed in its sample too, and the build says so.

## 5. Open points

* **Q38:** the repayment request `TFFSARPY` and these responses are untried in TI. Confirm that the channel's repayment arrives as a `Pay` event, and how a refusal is recorded. If TI uses `Pay reject`, `TFFRPREJ` must lose its repayment fields: that event has none.
* The root element of the payload (`tffindet` in `TFFINACK`) is assumed to be the same for these messages. The channel does not depend on it.
* TI may number the repayment event differently (`RPY001` in the samples is invented).
