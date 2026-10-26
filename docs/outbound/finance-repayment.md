# Finance Standalone repayment – TI responses to set up (`TFFRPACK`, `TFFRPREJ`)

> Part of [TI outward messages](README.md). Proposed by the channel team (2026-10-26) for the bank to configure in TI. **Not yet set up or tried in TI** (Q38). Samples of the resulting messages: [docs/samples](../samples/README.md#finance-standalone).

When a trade service officer completes or refuses a **repayment** event in TI (the event the channel starts with `TFFSARPY`), TI should send the channel an outward message, as it already does for the create event (`TFFINACK`, `TFFINREJ`). This page gives the two mappings to configure, in TI's notation, ready to paste once the placeholders are replaced.

| Message | When | Mapping text |
|---|---|---|
| `TFFRPACK` | repayment completed | [proposed/TFFRPACK.txt](proposed/TFFRPACK.txt) (36 fields) |
| `TFFRPREJ` | repayment refused | [proposed/TFFRPREJ.txt](proposed/TFFRPREJ.txt) (23 fields) |

The names follow `TFFINACK` / `TFFINREJ` (`TF` + `FRP` for *finance repayment* + `ACK` / `REJ`) and can be changed. The channel only needs a name that **ends in `ACK` or `REJ`** (Q39). It finds the repayment by `eBankMasterRef` + `eBankEventRef`, which TI stores from the `TFFSARPY` it received.

## 1. Replace the placeholders

Most tags come from mappings the bank has already set up. Eleven are written as `?…?` because the channel team does not know the event field code. Replace each with the code TI shows for that field. All of these are value (`v`), date (`d`) or string fields of the **Finance Repay** event.

| Placeholder | Fields | Pick in TI |
|---|---|---|
| `?TFFINACK.TFAmount?` | `TFAmount`, `TFCurrency` | **the same code as `TFAmount` in your `TFFINACK` mapping** (the finance amount) |
| `?TFFINACK.OutstandingAmt?` | `OutstandingAmt`, `OutstandingCcy` | the same code as `OutstandingAmt` in `TFFINACK`. It must give the outstanding **after** this repayment. In the guarantee list this is `OUA` ("Outstanding amount") |
| `?TFFINACK.MaturityDate?` | `MaturityDate` | the same code as `MaturityDate` in `TFFINACK` |
| `?TFFINACK.Status?` | `Status` | the same code as `Status` in `TFFINACK` (master status, `LIV` in the bank's test) |
| `?FINREPAY.REPAY_REF?` | `RepaymentReference` | the event field for *Repayment details repayment reference* (`FINREPAY.REPAY_REF`) |
| `?FINREPAY.REPAY_TYPE?` | `RepaymentType` | *Repayment details financing* (`FINREPAY.REPAY_TYPE`: F final, P principal plus interest, I interest then principal, …) |
| `?FINREPAY.REPAYPAYER?` | `RepaymentPayer` | *Repayment details repayment payer* (`FINREPAY.REPAYPAYER`: D debit party, F finance party) |
| `?FINREPAY.REPAY_DATE?` | `ValueDate` | *Repayment details repayment date* (`FINREPAY.REPAY_DATE`) |
| `?FINREPAY.PRI_PAID?` | `PrincipalRepaidAmt`, `PrincipalRepaidCcy` | *Repayment details principal amount paid* (`FINREPAY.PRI_PAID` / `PRIPAIDCCY`) |
| `?FINREPAY.INT_PAID?` | `InterestRepaidAmt`, `InterestRepaidCcy` | *Repayment details interest amount paid* (`FINREPAY.INT_PAID` / `INTPAIDCCY`) |
| `?FINREPAY.TOT_PAID?` | `TotalRepaidAmt`, `TotalRepaidCcy` | *Repayment details total amount repaid* (`FINREPAY.TOT_PAID` / `TOTPAIDCCY`) |

The `FINREPAY` column names come from the TI database specification (`sources/FTI-Database-*.html`, table `FINREPAY` "Finance Repay Event"). They identify the data, not the event field code. **The quickest way to settle all eleven** is an export of the Finance Repay event field list, like the one received for guarantees (`sources/ti_outbound/IGT_IIG_All_E.csv`). With it, the channel team can fill in the codes and check them.

## 2. Where each field comes from

| Fields | Tags | Taken from |
|---|---|---|
| `eBankMasterRef`, `eBankEventRef`, `MasterRef`, `EventRef` | `EBMR`, `EBER`, `MST`, `EVR` | `TFFINREJ` (bank) |
| `DebitPartyId`, `FinancingRef`, `FinanceToPartyId`, `BehalfOfBranch`, `MBE`, `ProductTypeCode`, `ProductSubTypeCode` | `B+DB`, `B+FR`, `B+FT`, `BOB`, `MBE`, `PTP`, `PUO1` | `TFFINREJ` |
| `TransactionDate`, `Provisional`, `Sequence` (ACK only) | `TDY`, `EPRV`, `ESQ` | `TFUTIACK` (guarantees). These are general event fields; `TFFINACK` carries `Provisional` and `Sequence` too, so the bank's `TFFINACK` mapping can confirm the codes |
| `Charges` (two tables), `AttachedDocuments` (two tables) | `AAOC`, `AABC`, `GDOCS`, `tAATT` | `TFFINREJ`, unchanged |
| Repayment and finance figures | placeholders | section 1 |
| `BankComment` | `TEXT,E,…` | new text; change as the bank likes |

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

`app/src/test/.../gateway/TiResponseMappingTest` reads both mapping files and the bank's `TFFINREJ`, and checks each against its sample in `docs/samples`. Each sample must have every plain field of its mapping (tables appear only when they have rows) and nothing else. Each mapping must carry the fields the channel reads. `FsaFlowTest` runs a repayment to completion and to refusal with these samples. So a mapping changed here must be changed in its sample too, and the build says so.

## 5. Open points

* **Q38:** the repayment request `TFFSARPY` and these responses are untried in TI.
* The root element of the payload (`tffindet` in `TFFINACK`) is assumed to be the same for these messages. The channel does not depend on it.
* TI may number the repayment event differently (`RPY001` in the samples is invented).
