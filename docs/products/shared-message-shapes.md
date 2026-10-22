# Shared message shapes

> Part of the product specifications. Index and shared rules: [README](README.md).

Many `TF*` messages have the same body. They are documented once here. A product specification links to the shape and lists only what differs. A shape's canonical message is the one its field table and sample are taken from. Members matched at 92% or better by element name; "Loose" members matched at 85–92% and are listed with their differences.

## Shape: Correspondence

Canonical message: `TFISBCOR` – A general purpose message that is sent by the Applicant to the Issuing Bank in the form of an Ancillary message, General correspondence, Queries or Answers. Creates: Import Standby Letter Of Credit correspondence event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFCHICOR` | 100% | none |
| `TFCHOCOR` | 100% | none |
| `TFCOLAMR` | 94% | adds `AmendmentNarrative` |
| `TFCOLCOR` | 100% | none |
| `TFCOLFNR` | 94% | adds `Financing` |
| `TFCOLNACP` | 89% (loose) | adds `NonAcceptanceAllDrafts`, `NonPaymentAllDrafts` |
| `TFCPICOR` | 100% | none |
| `TFCPOCOR` | 100% | none |
| `TFCSOCOR` | 100% | none |
| `TFEGTCOR` | 100% | none |
| `TFELCCOR` | 100% | none |
| `TFELCFLC` | 88% (loose) | adds `Financing`; omits `AdditionalData` |
| `TFESBCOR` | 100% | none |
| `TFFINCOR` | 100% | none |
| `TFFSACOR` | 100% | none |
| `TFGWYACK` | 94% | omits `ExtraData` |
| `TFIDCCOR` | 100% | none |
| `TFIGTAMR` | 94% | adds `AmendmentNarrative` |
| `TFIGTCOR` | 100% | none |
| `TFILCCOR` | 100% | none |
| `TFISBAMR` | 94% | adds `AmendmentNarrative` |
| `TFISBCOR` | 100% | none |
| `TFLICCOR` | 100% | none |
| `TFODCCOR` | 100% | none |
| `TFSHGCOR` | 100% | none |
| `TFUTICOR` | 100% | none |
| `TFUTRCOR` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Sender` | party | The Sender of the message. |
| `Subject` | string ≤35 | Subject of correspondence. |
| `CorresType` | string ≤1 | Correspondence type - G = General - Q = Query - A = Answer - N = Ancillary. |
| `EventFurtherID` | string 1–5 | Used to further define the type of event. The value in this field must be a valid bank defined TF dictionary value beginning EVTxx e.g. EVT01 - Acc... |
| `ExtraInformation` | string ≤35 | Free format extra information for the event. This information will be shown on the Master summary in the WIP and event history, dashboard and Maste... |
| `RelatedRef` | string ≤20 | Related reference. |
| `MessageText` | string ≤10000 | Correspondence. |
| `RelatedInformation` | string ≤1820 | Narrative Description of the Original Message. |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `eBankCorpRef` | string ≤20 | Reference of the master in the external system. |
| `FormOfUndertaking` | string ≤4 | Available values: 'DGAR'='Demand guarantee', 'UNDK'='Dependent undertaking', 'DOCR'='Documentary credit', 'STBY'='Standby letter of credit'. |
| `Function` | string ≤8 | MT759 specific. MT759 specific. Available values: 'FRAUDMSG'='Advice of a fraud attempt', 'ISSAMEND'='Amendment of a free-form undertaking such as... |
| `Narrative` | string ≤10050 |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFISBCOR`

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFISBCOR`

| Path | Allowed values |
|---|---|
| `FormOfUndertaking` | `DGAR` Demand guarantee; `UNDK` Dependent undertaking; `DOCR` Documentary credit; `STBY` Standby letter of credit |
| `Function` | `FRAUDMSG` Advice of a fraud attempt; `ISSAMEND` Amendment of a free-form undertaking such as a dependent guarantee; `CLSVCLOS` Closing of client service call by Trade Operations; `REQFINAN` Financing request; `GENINFAD` General information advice; `ISSUANCE` Issue of a free-form undertaking such as a dependent guarantee; `CLSVOPEN` Opening of client service call by Trade Operations; `OTHERFNC` Other request; `PREDENOT` Pre-debit notification; `REIMBURS` Request related to a reimbursement; `REQAMEND` Request to amend an undertaking; `REXTMATU` Request to extend the maturity date; `REQISSUE` Request to issue an undertaking; `TRANSFER` Transfer of an undertaking |


### Sample (`TFISBCOR`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFISBCOR</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFISBCOR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>ISB</c:Product>
    </m:Context>
    <m:MasterRef>ISB2600045</m:MasterRef>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:Subject>Query on amendment 2</m:Subject>
    <m:CorresType>Q</m:CorresType>
    <m:RelatedRef>AMD-2</m:RelatedRef>
    <m:MessageText>Please confirm whether the new expiry date was advised to the beneficiary.</m:MessageText>
    <m:eBankMasterRef>CH-ISB-000045</m:eBankMasterRef>
    <m:eBankEventRef>CH-ISB-000045-07</m:eBankEventRef>
  </m:TFISBCOR>
</ServiceRequest>
```

Used for every `*COR` message and, with small additions, for the basic text amendment requests (`TFISBAMR`, `TFIGTAMR`, `TFCOLAMR`) and `TFCOLFNR`. `CorresType` is G general, Q query, A answer, N ancillary. `EventFurtherID` must be a bank-defined dictionary value (`EVTxx`). `FormOfUndertaking` and `Function` apply to SWIFT MT759-type messages. To answer a bank query, set `CorresType` = `A` and `RelatedRef` to the bank's reference.

## Shape: Cancellation request

Canonical message: `TFISBCAN` – Cancellation request. Creates: Import Standby Letter Of Credit cancel event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFIGTCAN` | 100% | none |
| `TFILCCAN` | 100% | none |
| `TFISBCAN` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Sender` | party | The Sender of the message. |
| `CancelInstructions` | string ≤32000 | The applicant's cancellation instructions. |
| `BenApprovalReq` | boolean | Whether beneficiary approval is required for the cancellation - Y = Required - N = Not required. Boolean values. |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `InstructionsToPayingAcceptingNegotiatingBank` | string ≤804 | Instructions to Paying/Accepting/Negotiating Bank. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFISBCAN`

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


### Sample (`TFISBCAN`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFISBCAN</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFISBCAN>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>ISB</c:Product>
    </m:Context>
    <m:MasterRef>ISB2600045</m:MasterRef>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:CancelInstructions>Underlying contract completed. Please cancel the standby.</m:CancelInstructions>
    <m:BenApprovalReq>N</m:BenApprovalReq>
    <m:eBankMasterRef>CH-ISB-000045</m:eBankMasterRef>
    <m:eBankEventRef>CH-ISB-000045-09</m:eBankEventRef>
  </m:TFISBCAN>
</ServiceRequest>
```

`BenApprovalReq` states whether the beneficiary's consent is required. Whether the bank can cancel without it depends on the instrument and the governing rules.

## Shape: Claim response

Canonical message: `TFISBPYR` – Claim response. Creates: Import Standby Letter Of Credit outstanding claim event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEGTPYR` | 90% (loose) | adds `ResponseNarrative`; omits `RequestedNewExpiryDateLocal` |
| `TFESBPYR` | 90% (loose) | adds `ResponseNarrative`; omits `RequestedNewExpiryDateLocal` |
| `TFIGTPYR` | 100% | none |
| `TFISBPYR` | 100% | none |
| `TFUTIPYR` | 100% | none |
| `TFUTRPYR` | 90% (loose) | adds `ResponseNarrative`; omits `RequestedNewExpiryDateLocal` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `ClaimId` | string ≤6 | Reference of the claim being approved or rejected. e.g. CLM001 This reference is used to identify the claim to the payment response is related to. |
| `Sender` | party | The Sender of the message. |
| `ResponseAction` | string ≤3 | Response action as defined by payment actions in system tailoring. Either Response action or Response type should be specified. |
| `ResponseType` | string ≤1 | Underlying behaviour for response action. This is used to find the default response action for the underlying behaviour if the ResponseAction field... |
| `ResponseDetails` | string ≤32000 | Narrative relating to the approval or refusal. |
| `RefusalDetails` | string ≤3640 | Narrative relating to the refusal. |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `NetAmountPaid` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `AuthorisationInstructions` | string 1–10 | Note keep the store list in capitals or the Swift validation for field 23 in message 752 will fail (see message.swf, search MT752) Also REJECT remo... |
| `Financing` | group | SImilar to GWR800, containing fields for creating subsidiary financing event to GWRReceivedDocument. |
| `WithRecourse` | boolean | Boolean values. |
| `FinalPay` | boolean | Boolean values. |
| `MixedPay` | boolean | Boolean values. |
| `InstructionsReceived` | string ≤32000 |  |
| `PresentedDocumentNotes` | group |  |
| `RequestedNewExpiryDate` | date | Requested new expiry date when extending the undertaking. |
| `RequestedNewExpiryDateLocal` | date | Requested new expiry date local when extending the undertaking. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFISBPYR`

**`NetAmountPaid`**

| Path | Type / length | Description |
|---|---|---|
| `NetAmountPaid/Amount` | string ≤22 |  |
| `NetAmountPaid/Currency` | string 1–3 | Currency. |

**`Financing`**

| Path | Type / length | Description |
|---|---|---|
| `Financing/Amount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `Financing/Amount/Amount` | string ≤22 |  |
| `Financing/Amount/Currency` | string 1–3 | Currency. |
| `Financing/AutoMonthlyDebit` | boolean | Boolean values. |
| `Financing/AutoRepay` | boolean | Boolean values. |
| `Financing/DebitParty` | party |  |
| `Financing/EffectiveDate` | date |  |
| `Financing/FinanceAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `Financing/FinanceAmount/Amount` | string ≤22 |  |
| `Financing/FinanceAmount/Currency` | string 1–3 | Currency. |
| `Financing/FinancePercent` | decimal(17,2) | Percentage. Available Values: 0 to 100 |
| `Financing/FinanceCurrency` | string 1–3 | Currency Code. |
| `Financing/ExchangeRate` | string | Exchange rate. |
| `Financing/FinanceToParty` | party |  |
| `Financing/FinanceInterestDate` | date |  |
| `Financing/InterestRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/InterestSettlementCcy` | string 1–3 | Currency Code. |
| `Financing/MarginRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/MaturityDate` | date |  |
| `Financing/PenaltyPercent` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/PreferredChargeCcy` | string 1–3 | Currency Code. |
| `Financing/StartDate` | date |  |
| `Financing/TrustReceiptLoan` | boolean | Boolean values. |
| `Financing/UseSchedule` | boolean | Boolean values. |
| `Financing/RelatedMasterRef` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `Financing/InterestType` | string 1–1 | Available values: 'D'='Interest in advance - discount', 'Y'='Interest in advance - discount to yield', 'I'=... |
| `Financing/InterestFromAfter` | string 1–1 | Available values: 'A'='After', 'F'='From', 'E'='From (exclusive)'. |
| `Financing/RateType` | string 1–1 | Available values: 'F'='Fixed', 'S'='Special', 'V'='Variable'. |
| `Financing/BaseCode` | string 2–5 |  |
| `Financing/ActualBaseOrGroupRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/DiffCode` | string 2–2 |  |
| `Financing/NarrativeDetails` | string ≤32000 |  |
| `Financing/InstructionsToFinanceParty` | string ≤32000 |  |
| `Financing/InstructionsToDebitParty` | string ≤32000 |  |
| `Financing/PenaltyRateCode` | string 2–2 |  |
| `Financing/GroupRate` | string 1–6 |  |
| `Financing/MessageToBank` | string ≤32000 |  |
| `Financing/eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `Financing/eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `Financing/ProductCode` | string ≤4 |  |
| `Financing/RepaymentDetailss` | group |  |
| `Financing/RepaymentDetailss/RepaymentDetails` | group ×n | Irregular repayment details. |
| `Financing/RepaymentDetailss/RepaymentDetails/Amount` | long |  |
| `Financing/RepaymentDetailss/RepaymentDetails/Percent` | decimal(17,2) | The percent figure for the data type. Available Values: 0 to 100 |
| `Financing/RepaymentDetailss/RepaymentDetails/Date` | date |  |
| `Financing/RegularRepaymentDetails` | group | Regular repayment details. |
| `Financing/RegularRepaymentDetails/AmountOrPercent` | group |  |
| `Financing/RegularRepaymentDetails/AmountOrPercent/Amount` | long |  |
| `Financing/RegularRepaymentDetails/AmountOrPercent/Percent` | decimal(17,2) | Percentage. Available Values: 0 to 100 |
| `Financing/RegularRepaymentDetails/Date` | date |  |
| `Financing/RegularRepaymentDetails/Period` | group |  |
| `Financing/RegularRepaymentDetails/Period/Unit` | string 1–1 | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `Financing/RegularRepaymentDetails/Period/Number` | int | Number of days (months) etc e.g. 30. |
| `Financing/RegularRepaymentDetails/Period/DayInMonth` | int | Day in month for renewal. |
| `Financing/FinancePeriod` | group |  |
| `Financing/FinancePeriod/TenorDays` | int | Tenor element of the term. |
| `Financing/FinancePeriod/TenorPeriod` | string | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `Financing/InterestPeriod` | group |  |
| `Financing/InterestPeriod/Unit` | string 1–1 | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `Financing/InterestPeriod/Number` | int | Number of days (months) etc e.g. 30. |
| … | | 210 more paths – see `TI.<message>.xsd.txt` for `Financing` |

**`PresentedDocumentNotes`**

| Path | Type / length | Description |
|---|---|---|
| `PresentedDocumentNotes/DispatchType` | string 1–4 | SWIFT SCORE Document dispatch type. Available values: 'AIRM'='By airmail', 'COUR'='By courier (e.g. FedEx,... |
| `PresentedDocumentNotes/DispatchDetails` | string ≤35 | Document dispatch details. |
| `PresentedDocumentNotes/AdvisePresenterDocsInOrder` | boolean | Whether to produce an advice of discharge on release. Boolean values. |
| `PresentedDocumentNotes/SentDate` | date | Date documents sent. |
| `PresentedDocumentNotes/DocumentCount` | int | Number of documents sent. |
| `PresentedDocumentNotes/DisposalInstructions` | string 1–1 | Document disposal instructions. Available values: 'Y'='Hold', 'F'='Notify', 'P'='Previous instructions', 'N... |
| `PresentedDocumentNotes/DisposalDetails` | string ≤111 | Document disposal details. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFISBPYR`

| Path | Allowed values |
|---|---|
| `ResponseType` | `B` Advice of payment; `A` Approval; `x` Pay or extend response - Extend; `p` Pay or extend response - Pay; `R` Refusal |
| `AuthorisationInstructions` | `ACCEPT` Accept; `DEBIT` Debit selves; `NEGOTIATE` Negotiate; `REIMBURSE` Pay and claim reimbursement; `REMITTED` Receive remittance; `SEE72` See details |
| `Financing/InterestType` | `D` Interest in advance - discount; `Y` Interest in advance - discount to yield; `I` Interest in advance - interest to yield; `S` Interest in advance - standard; `E` Interest in arrears - standard |
| `Financing/InterestFromAfter` | `A` After; `F` From; `E` From (exclusive) |
| `Financing/RateType` | `F` Fixed; `S` Special; `V` Variable |
| `Financing/RegularRepaymentDetails/Period/Unit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/FinancePeriod/TenorPeriod` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/InterestPeriod/Unit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/Collateral/CollateralDetail/OtherDetailsType` | `G` General; `S` Specific |
| `Financing/FinanceResponse` | `A` Offer is accepted; `R` Offer is rejected; `G` Offer is renegotiated |
| `Financing/InterestScheduleTiers/InterestScheduleTier/PeriodUnit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/SettlementInstructionss/SettlementInstructions/PayReceiveFlag` | `P` Pay; `R` Receive |
| `Financing/SettlementInstructionss/SettlementInstructions/MovementType` | `C` Charge; `I` Interest; `P` Principal |
| `Financing/SettlementInstructionss/SettlementInstructions/PayFundsVia` | `T` Account transfer; `A` Acct at nostro/vostro; `B` Beneficiary bank; `C` Beneficiary bank with cover; `X` Clearing ID; `I` Intermediary bank; `P` Intermediary with cover; `O` Other account |
| `Financing/SettlementInstructionss/SettlementInstructions/SettlesBy` | `A` Account; `X` Clearing ID; `N` Nostro; `O` Other account; `V` Vostro |
| `Financing/SettlementInstructionss/SettlementInstructions/SwiftCharges` | `BEN` Beneficiary; `OUR` Our; `SHA` Shared |
| `PresentedDocumentNotes/DispatchType` | `AIRM` By airmail; `COUR` By courier (e.g. FedEx, DHL, UPS); `ELEC` Electronically |
| `PresentedDocumentNotes/DisposalInstructions` | `Y` Hold; `F` Notify; `P` Previous instructions; `N` Return |


### Sample (`TFISBPYR`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFISBPYR</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFISBPYR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>ISB</c:Product>
    </m:Context>
    <m:MasterRef>ISB2600045</m:MasterRef>
    <m:ClaimId>CLM001</m:ClaimId>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:ResponseAction>Sam</m:ResponseAction>
    <m:ResponseDetails>Documents are in order. Approve payment.</m:ResponseDetails>
    <m:eBankMasterRef>CH-ISB-000045</m:eBankMasterRef>
    <m:eBankEventRef>CH-ISB-000045-10</m:eBankEventRef>
    <m:NetAmountPaid>
      <c:Amount>250000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:NetAmountPaid>
    <m:AuthorisationInstructions>PAY</m:AuthorisationInstructions>
  </m:TFISBPYR>
</ServiceRequest>
```

The applicant approves or refuses a claim or presentation that the bank registered on the master. `ClaimId` identifies the claim. Give `ResponseAction` **or** `ResponseType`; the allowed codes are the bank's payment actions in TI system tailoring and must be loaded into the channel. `AuthorisationInstructions` must be **upper case** because it feeds SWIFT MT752 field 23. `RequestedNewExpiryDate` supports an "extend or pay" demand. For export-side members (`TFEGTPYR`, `TFESBPYR`, `TFUTRPYR`) the same body is a beneficiary-side message and is not a customer instruction.

## Shape: Beneficiary response

Canonical message: `TFELCBMR` – Beneficiary response. Creates: Export Letter Of Credit beneficiary response to amend event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEGTBMR` | 100% | none |
| `TFELCBMR` | 100% | none |
| `TFESBBMR` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `AmendRef` | string |  |
| `Sender` | party |  |
| `ResponseType` | string 1–1 | Available values: 'A'='Approved', 'R'='Refused'. |
| `ResponseDetails` | string |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `HSCode` | string ≤65 |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFELCBMR`

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFELCBMR`

| Path | Allowed values |
|---|---|
| `ResponseType` | `A` Approved; `R` Refused |


### Sample (`TFELCBMR`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFELCBMR</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFELCBMR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>ELC</c:Product>
    </m:Context>
    <m:MasterRef>ELC2600012</m:MasterRef>
    <m:AmendRef>AMD-1</m:AmendRef>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:ResponseType>A</m:ResponseType>
    <m:ResponseDetails>Beneficiary accepts the amendment.</m:ResponseDetails>
    <m:eBankMasterRef>CH-ELC-000012</m:eBankMasterRef>
    <m:eBankEventRef>CH-ELC-000012-03</m:eBankEventRef>
  </m:TFELCBMR>
</ServiceRequest>
```

`ResponseType` is `A` approved or `R` refused. `TFxxxBMR` answers an amendment (`AmendRef`); `TFxxxBCR` answers a cancellation (`CancelRef`). Members that omit `AmendRef` carry `CancelRef` instead.

## Shape: Documents presented

Canonical message: `TFESBDPR` – Documents presented. Creates: Export Standby Letter Of Credit documents presented event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEGTDPR` | 100% | none |
| `TFESBDPR` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Sender` | party | The sender of the message. |
| `PresentersAction` | string ≤3 | Presenter's action as defined by payment actions in system tailoring. |
| `RequestType` | string 1–1 | Underlying behaviour for presenter's action. This is used to find the default presenter's action for the underlying behaviour if the PresentersActi... |
| `NotesFromPresenter` | string ≤3350 | Notes from presenter. |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `PresentationDetails` | group | Presentation details. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFESBDPR`

**`PresentationDetails`**

| Path | Type / length | Description |
|---|---|---|
| `PresentationDetails/PresentationAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `PresentationDetails/PresentationAmount/Amount` | string ≤22 |  |
| `PresentationDetails/PresentationAmount/Currency` | string 1–3 | Currency. |
| `PresentationDetails/PresentationDate` | date | Presentation date. |
| `PresentationDetails/PresentersReference` | string 1–34 | Transaction reference number - Customer reference number. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFESBDPR`

| Path | Allowed values |
|---|---|
| `RequestType` | `S` Documents sent on approval; `O` Other; `R` Pay and claim reimbursement; `F` Request acceptance; `A` Request authorisation to pay; `q` Request pay or extend; `P` Request payment; `D` Sender has debited our account |


### Sample (`TFESBDPR`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFESBDPR</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFESBDPR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>ESB</c:Product>
    </m:Context>
    <m:MasterRef>ESB2600031</m:MasterRef>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:PresentersAction>Sam</m:PresentersAction>
    <m:NotesFromPresenter>Documents presented under cover of letter dated 2026-10-15.</m:NotesFromPresenter>
    <m:eBankMasterRef>CH-ESB-000031</m:eBankMasterRef>
    <m:eBankEventRef>CH-ESB-000031-04</m:eBankEventRef>
    <m:PresentationDetails>
      <c:PresentationAmount>
        <c:Amount>120000.00</c:Amount>
        <c:Currency>USD</c:Currency>
      </c:PresentationAmount>
      <c:PresentationDate>2026-10-15</c:PresentationDate>
      <c:PresentersReference>PRES-8812</c:PresentersReference>
    </m:PresentationDetails>
  </m:TFESBDPR>
</ServiceRequest>
```

Beneficiary-side presentation of documents or a demand. `PresentersAction` / `RequestType` use the bank's payment-action tailoring, as for claim responses.

## Shape: Open account response

Canonical message: `TFIOARBC` – Open account response. Creates: Import Open Acount TMA response to buyer cancellation request event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEOARBC` | 100% | none |
| `TFEOARBE` | 100% | none |
| `TFEOATBC` | 100% | none |
| `TFEOATBE` | 100% | none |
| `TFEOATSC` | 100% | none |
| `TFEOATSE` | 100% | none |
| `TFIOARBC` | 100% | none |
| `TFIOARSC` | 91% (loose) | omits `AdditionalData` |
| `TFIOARSE` | 100% | none |
| `TFIOATBE` | 100% | none |
| `TFIOATSC` | 100% | none |
| `TFIOATSE` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `PurchaseOrderReference` | string 1–34 | Purchase order reference. |
| `TMAUsed` | boolean | TMA used. Boolean values. |
| `TransactionMatchingReference` | string ≤35 | Transaction matching reference. |
| `ResponseType` | string 1–1 | Used to indicate acceptance or rejection of the amendment. Available values: 'A'='Accepted', 'R'='Rejected'. |
| `ResponseNarrative` | string ≤32000 | Narrative sent with response. |
| `ResponseDate` | date | Date response received. |
| `Sender` | party | The Sender of the message. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFIOARBC`

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFIOARBC`

| Path | Allowed values |
|---|---|
| `ResponseType` | `A` Accepted; `R` Rejected |


### Sample (`TFIOARBC`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFIOARBC</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFIOARBC>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>IOA</c:Product>
    </m:Context>
    <m:MasterRef>IOA2600007</m:MasterRef>
    <m:eBankMasterRef>CH-IOA-000007</m:eBankMasterRef>
    <m:eBankEventRef>CH-IOA-000007-02</m:eBankEventRef>
    <m:PurchaseOrderReference>PO-2026-0457</m:PurchaseOrderReference>
    <m:TMAUsed>Y</m:TMAUsed>
    <m:TransactionMatchingReference>TMA-99817</m:TransactionMatchingReference>
    <m:ResponseType>A</m:ResponseType>
    <m:ResponseNarrative>Cancellation request accepted.</m:ResponseNarrative>
    <m:ResponseDate>2026-10-18</m:ResponseDate>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
  </m:TFIOARBC>
</ServiceRequest>
```

Answers an amendment or cancellation request on an open-account (TMA) transaction. `ResponseType` is `A` accepted or `R` rejected. Members are named for who answers whom: buyer, seller, or the bank's transaction-matching application (TMA).

## Shape: Open account cancellation request

Canonical message: `TFIOABRC` – Open account cancellation request. Creates: Import Open Acount buyer cancellation request event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFIOABRC` | 100% | none |
| `TFIOACAR` | 100% | none |
| `TFIOASRC` | 92% (loose) | omits `AdditionalData` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `PurchaseOrderReference` | string 1–34 | Purchase order reference. |
| `TMAUsed` | boolean | TMA used. Boolean values. |
| `TransactionMatchingReference` | string ≤35 | Transaction matching reference. |
| `Sender` | party | The Sender of the message. |
| `CancellationRef` | string 1–34 |  |
| `CancellationDate` | date | Cancellation date. |
| `CancellationInstructions` | string ≤15860 | Cancellation instructions. |
| `InformationForBuyer` | string ≤32000 | Instructions for buyer. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFIOABRC`

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


### Sample (`TFIOABRC`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFIOABRC</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFIOABRC>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>IOA</c:Product>
    </m:Context>
    <m:MasterRef>IOA2600007</m:MasterRef>
    <m:eBankMasterRef>CH-IOA-000007</m:eBankMasterRef>
    <m:eBankEventRef>CH-IOA-000007-03</m:eBankEventRef>
    <m:PurchaseOrderReference>PO-2026-0457</m:PurchaseOrderReference>
    <m:TMAUsed>Y</m:TMAUsed>
    <m:TransactionMatchingReference>TMA-99817</m:TransactionMatchingReference>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:CancellationRef>CXL-1</m:CancellationRef>
    <m:CancellationDate>2026-10-18</m:CancellationDate>
    <m:CancellationInstructions>Order cancelled by mutual agreement.</m:CancellationInstructions>
    <m:InformationForBuyer>Please confirm cancellation.</m:InformationForBuyer>
  </m:TFIOABRC>
</ServiceRequest>
```

## Shape: Open account amendment

Canonical message: `TFIOAAME` – Open account amendment. Creates: Import Open Acount amendment event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEOAAME` | 93% | adds `ExportOpenAccountParties`; omits `ImportOpenAccountParties` |
| `TFEOABRE` | 93% | adds `ExportOpenAccountParties`; omits `ImportOpenAccountParties` |
| `TFEOASRE` | 90% (loose) | adds `ExportOpenAccountParties`; omits `ImportOpenAccountParties`, `AdditionalData` |
| `TFIOAAME` | 100% | none |
| `TFIOABRE` | 100% | none |
| `TFIOASRE` | 96% | omits `AdditionalData` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `PurchaseOrderReference` | string 1–34 | Purchase order reference. |
| `TMAUsed` | boolean | TMA used. Boolean values. |
| `TransactionMatchingReference` | string ≤35 | Transaction matching reference. |
| `BPOIncluded` | boolean | Bank payment obligations included. Boolean values. |
| `AmendmentDate` | date | Amendment date. |
| `ExpiryDate` | date | Expiry date. |
| `ExpiryPlace` | string 1–29 | Expiry place. |
| `ProductType` | string ≤3 | Product type. |
| `AmendmentNarrative` | string ≤1820 | General narrative regarding the amendment. |
| `OpenAccountAmount` | group | Open account transaction amount. |
| `OpenAccountAmountSpec` | group | Open account transaction amount additional spec. |
| `IncreaseAmount` | group | Amount and currency of increase. |
| `DecreaseAmount` | group | Amount and currency of decrease. |
| `Narratives` | group |  |
| `GoodsAndShipment` | group |  |
| `ObligationDetails` | group |  |
| `ObligationPaymentTerms` | group |  |
| `MarginDeposit` | group |  |
| `Collaterals` | group |  |
| `FXOptionDeal` | string 1–20 | Details of any forward foreign exchange contract that this master may utilise. |
| `FxContracts` | group |  |
| `ChargeDetails` | group |  |
| `BankCodes` | group | Bank defined codes for their own purposes. Defined using 'List value' in TI Plus System tailoring. |
| `ImportOpenAccountParties` | group |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFIOAAME`

**`OpenAccountAmount`**

| Path | Type / length | Description |
|---|---|---|
| `OpenAccountAmount/Amount` | string ≤22 |  |
| `OpenAccountAmount/Currency` | string 1–3 | Currency. |

**`OpenAccountAmountSpec`**

| Path | Type / length | Description |
|---|---|---|
| `OpenAccountAmountSpec/Qualifier` | string 1–1 | Tolerance. Available values: 'A'='About', 'E'='Exact', 'M'='Maximum', 'X'='Min/Max', 'N'='Not Exceeding', '... |
| `OpenAccountAmountSpec/Min` | string 1–2 | Min percentage tolerance. |
| `OpenAccountAmountSpec/Max` | string 1–2 | Max percentage tolerance. |
| `OpenAccountAmountSpec/AmountMin` | long | Min amount tolerance. |
| `OpenAccountAmountSpec/AmountMax` | long | Min amount tolerance. |

**`IncreaseAmount`**

| Path | Type / length | Description |
|---|---|---|
| `IncreaseAmount/Amount` | string ≤22 |  |
| `IncreaseAmount/Currency` | string 1–3 | Currency. |

**`DecreaseAmount`**

| Path | Type / length | Description |
|---|---|---|
| `DecreaseAmount/Amount` | string ≤22 |  |
| `DecreaseAmount/Currency` | string 1–3 | Currency. |

**`Narratives`**

| Path | Type / length | Description |
|---|---|---|
| `Narratives/InformationForBuyerOrSeller` | string ≤32000 | Instructions for buyer (if import open account) or seller (if export open account). |
| `Narratives/InstructionsFromBuyerOrSeller` | string ≤32000 | Instructions from buyer (if import open account) or seller (if export open account). |
| `Narratives/PaymentInstructions` | string ≤150 | Payment instructions. |

**`GoodsAndShipment`**

| Path | Type / length | Description |
|---|---|---|
| `GoodsAndShipment/GoodsDescription` | string ≤26800 | Description of Goods. |
| `GoodsAndShipment/GoodsCode` | string 1–10 | Goods code. |
| `GoodsAndShipment/PartShipment` | string 1–35 | Partial shipment text. |
| `GoodsAndShipment/ShipmentFrom` | string ≤144 | Shipment from. |
| `GoodsAndShipment/ShipmentTo` | string ≤144 | Shipment to. |
| `GoodsAndShipment/PlaceOfLoading` | string ≤144 | Port of loading. |
| `GoodsAndShipment/PlaceOfDischarge` | string ≤144 | Port of discharge. |

**`ObligationDetails`**

| Path | Type / length | Description |
|---|---|---|
| `ObligationDetails/ObligationDetail` | group ×n |  |
| `ObligationDetails/ObligationDetail/ObligationType` | string 1–1 | Available values: 'B'='Other bank obligor', 'O'='Ourselves / buyer's bank'. |
| `ObligationDetails/ObligationDetail/ObligorSwiftAddress` | string ≤14 |  |
| `ObligationDetails/ObligationDetail/ObligorCustomer` | string 1–20 |  |
| `ObligationDetails/ObligationDetail/ObligationAmount` | string ≤22 |  |
| `ObligationDetails/ObligationDetail/ObligationCurrency` | string 1–3 |  |
| `ObligationDetails/ObligationDetail/ObligationPercent` | decimal(15,12) | Available Values: 0 to 100 |
| `ObligationDetails/ObligationDetail/ExpiryDate` | date |  |
| `ObligationDetails/ObligationDetail/ApplicableLawCountry` | string 1–2 |  |
| `ObligationDetails/ObligationDetail/ChargeAmount` | string ≤22 |  |
| `ObligationDetails/ObligationDetail/ChargeCurrency` | string 1–3 |  |
| `ObligationDetails/ObligationDetail/ChargePercent` | decimal(15,12) | Available Values: 0 to 100 |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentSwiftAddress` | string ≤14 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentName` | string 1–70 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentAddressStreetName` | string 1–70 | Name of street or thoroughfare. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentAddressPostCodeIdentification` | string 1–15 | ZIP or post code. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentAddressTownName` | string 1–35 | Name of a build-up area, with defined boundaries, and a local government. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentAddressCountrySubDivision` | string 1–35 | Identifies a subdivision of a country eg state, region county. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAgentAddressCountry` | string 1–2 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountIdentificationIBAN` | string 1–34 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountIdentificationOtherIdentification` | string 1–34 | Identification assigned by an institution. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountIdentificationOtherSchemeNameCode` | string 1–4 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountIdentificationOtherSchemeNameProprietary` | string 1–35 | Name of the identification scheme in a free text form. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountIdentificationOtherIssuer` | string 1–35 | Entity that assignes the identification. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountTypeCode` | string 1–4 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountTypeProprietary` | string 1–35 | Name of the identification scheme in a free text form. |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountCurrency` | string 1–3 |  |
| `ObligationDetails/ObligationDetail/SettlementTermsCreditorAccountName` | string 1–70 |  |

**`ObligationPaymentTerms`**

| Path | Type / length | Description |
|---|---|---|
| `ObligationPaymentTerms/ObligationPaymentTerm` | group ×n |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/ObligationType` | string 1–1 | Available values: 'B'='Other bank obligor', 'O'='Ourselves / buyer's bank'. |
| `ObligationPaymentTerms/ObligationPaymentTerm/ObligorSwiftAddress` | string ≤14 |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/ObligorCustomer` | string 1–20 |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/PaymentCodeCode` | string 1–4 |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/PaymentCodeNumberOfDays` | long |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/OtherPaymentTerms` | string 1–140 | Specifies payment terms not present in a code list. |
| `ObligationPaymentTerms/ObligationPaymentTerm/PaymentAmount` | string ≤22 |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/PaymentCurrency` | string 1–3 |  |
| `ObligationPaymentTerms/ObligationPaymentTerm/PaymentPercent` | decimal(15,12) | Available Values: 0 to 100 |

**`MarginDeposit`**

| Path | Type / length | Description |
|---|---|---|
| `MarginDeposit/AmountFieldCode` | string 3–10 | Code for a field (real or calculated) in an event (used in postings rules). |
| `MarginDeposit/Account` | string 1–34 | Account number. |
| `MarginDeposit/Code` | string 1–8 |  |
| `MarginDeposit/Description` | string ≤140 |  |
| `MarginDeposit/MarginAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `MarginDeposit/MarginAmount/Amount` | string ≤22 |  |
| `MarginDeposit/MarginAmount/Currency` | string 1–3 | Currency. |
| `MarginDeposit/Percentage` | decimal(17,2) | Percentage. |

**`Collaterals`**

| Path | Type / length | Description |
|---|---|---|
| `Collaterals/Collateral` | group ×n |  |
| `Collaterals/Collateral/Code` | string 1–8 |  |
| `Collaterals/Collateral/Description` | string ≤25 |  |
| `Collaterals/Collateral/Amount` | long | Amount. |
| `Collaterals/Collateral/Currency` | string 1–3 | Currency mnemonic. |
| `Collaterals/Collateral/OtherDetailsType` | string | Available values: 'G'='General', 'S'='Specific'. |
| `Collaterals/Collateral/CustomerDDA` | string ≤25 |  |
| `Collaterals/Collateral/OtherPledge` | string ≤50 |  |
| `Collaterals/Collateral/OwnPledge` | string ≤50 |  |
| `Collaterals/Collateral/Surety` | string ≤50 |  |

**`FxContracts`**

| Path | Type / length | Description |
|---|---|---|
| `FxContracts/FxContract` | group ×n |  |
| `FxContracts/FxContract/Reference` | string 1–20 |  |
| `FxContracts/FxContract/Branch` | string 1–8 |  |
| `FxContracts/FxContract/DealType` | string 1–3 |  |
| `FxContracts/FxContract/PurchaseCcy` | string 1–3 |  |
| `FxContracts/FxContract/SaleCcy` | string 1–3 |  |
| `FxContracts/FxContract/ExchangeRate` | decimal(28,20) | Available Values: 0 to 99999999 |

**`ChargeDetails`**

| Path | Type / length | Description |
|---|---|---|
| `ChargeDetails/OurChargesFor` | string 1–1 | Our charges for. Available values: 'B'='Buyer', 'S'='Seller'. |
| `ChargeDetails/OverseasChargesFor` | string 1–1 | Overseas charges for. Available values: 'B'='Buyer', 'S'='Seller'. |
| `ChargeDetails/DeferCharges` | boolean | Defer buyer charges. Boolean values. |
| `ChargeDetails/DeferSellerCharges` | boolean | Defer seller charges. Boolean values. |
| `ChargeDetails/TaxFor` | string 1–1 | Tax paid by. Tax paid by. Available values: 'P'='Charge payer', 'C'='Customer'. |
| `ChargeDetails/BillingLevel` | string 1–1 | Bill level. Available values: 'C'='Customer', 'P'='Product', 'T'='Transaction', 'X'='User-defined'. |
| `ChargeDetails/PreferredCcy` | string 1–3 | Preferred charge currency. |
| `ChargeDetails/ChargeAccount` | string 1–34 | Account from which charges are to be taken. |
| `ChargeDetails/ChargeText` | string ≤222 | Free format charge text. |

**`BankCodes`**

| Path | Type / length | Description |
|---|---|---|
| `BankCodes/BankCode1` | string ≤4 |  |
| `BankCodes/BankCode2` | string ≤4 |  |
| `BankCodes/BankCode3` | string ≤4 |  |
| `BankCodes/BankCode4` | string ≤4 |  |
| `BankCodes/BankCode5` | string ≤4 |  |

**`ImportOpenAccountParties`**

| Path | Type / length | Description |
|---|---|---|
| `ImportOpenAccountParties/Buyer` | party | Buyer. |
| `ImportOpenAccountParties/Seller` | party | Seller. |
| `ImportOpenAccountParties/SubmittingBank` | party | Submitting bank. |
| `ImportOpenAccountParties/SellerBank` | party | Seller's bank / Recipient bank. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFIOAAME`

| Path | Allowed values |
|---|---|
| `OpenAccountAmountSpec/Qualifier` | `A` About; `E` Exact; `M` Maximum; `X` Min/Max; `N` Not Exceeding; `O` Other; `U` Up to |
| `ObligationDetails/ObligationDetail/ObligationType` | `B` Other bank obligor; `O` Ourselves / buyer |
| `ObligationPaymentTerms/ObligationPaymentTerm/ObligationType` | `B` Other bank obligor; `O` Ourselves / buyer |
| `Collaterals/Collateral/OtherDetailsType` | `G` General; `S` Specific |
| `ChargeDetails/OurChargesFor` | `B` Buyer; `S` Seller |
| `ChargeDetails/OverseasChargesFor` | `B` Buyer; `S` Seller |
| `ChargeDetails/TaxFor` | `P` Charge payer; `C` Customer |
| `ChargeDetails/BillingLevel` | `C` Customer; `P` Product; `T` Transaction; `X` User-defined |


### Sample (`TFIOAAME`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFIOAAME</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFIOAAME>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>IOA</c:Product>
    </m:Context>
    <m:MasterRef>IOA2600007</m:MasterRef>
    <m:eBankMasterRef>CH-IOA-000007</m:eBankMasterRef>
    <m:eBankEventRef>CH-IOA-000007-04</m:eBankEventRef>
    <m:PurchaseOrderReference>PO-2026-0457</m:PurchaseOrderReference>
    <m:TMAUsed>Y</m:TMAUsed>
    <m:TransactionMatchingReference>TMA-99817</m:TransactionMatchingReference>
    <m:AmendmentDate>2026-10-18</m:AmendmentDate>
    <m:ExpiryDate>2027-02-28</m:ExpiryDate>
    <m:AmendmentNarrative>Extend the latest payment date by 30 days.</m:AmendmentNarrative>
    <m:OpenAccountAmount>
      <c:Amount>180000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:OpenAccountAmount>
  </m:TFIOAAME>
</ServiceRequest>
```

## Shape: Finance repayment

Canonical message: `TFIRFRPY` – Finance repayment. Creates: Buyer Centric Finance repayment event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFFCBRPY` | 100% | none |
| `TFFELRPY` | 100% | none |
| `TFFEORPY` | 100% | none |
| `TFFIARPY` | 100% | none |
| `TFFICRPY` | 100% | none |
| `TFFILRPY` | 100% | none |
| `TFFIORPY` | 100% | none |
| `TFFOCRPY` | 100% | none |
| `TFFRBRPY` | 100% | none |
| `TFFSARPY` | 100% | none |
| `TFFSCRPY` | 95% | adds `MasterSettlementInstructionss` |
| `TFIDSRPY` | 100% | none |
| `TFIRFRPY` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `EBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `EBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `EBankCorpRef` | string ≤20 | TSDS-264 (PG). |
| `RepaymentAction` | string 1–1 | Repayment action. Repayment type. Available values: 'F'='Final payment', 'I'='Interest to date then principal', 'T'='Last pay then transfer', 'M'='... |
| `RepaymentReference` | string 1–34 | Reference. |
| `ValueDate` | date | Value date. |
| `RepaymentPayer` | string ≤1 | Repayment payer. Available values: 'D'='Debit party', 'F'='Finance party'. |
| `FinanceRepaymentOutstanding` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `PrincipalRepaymentAmount` | group | Principal repayment amount. |
| `InterestRepaymentAmount` | group | Interest repayment amount. |
| `TotalRepaymentAmount` | group | Total repayment amount. |
| `WaiveInterestRefund` | boolean | Boolean values. |
| `TransferCurrency` | string 1–3 | Transfer currency. |
| `PrincipalExchangeRate` | string | Principal exchange rate. |
| `COFDetails` | group | Cost of fund details. |
| `PrincipalAccount` | string 1–34 | Principal account. |
| `FeeAccount` | string 1–34 | The account number from which fees or charges should be taken. |
| `CustomerInstructions` | string ≤804 | Instructions from repayer. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFIRFRPY`

**`FinanceRepaymentOutstanding`**

| Path | Type / length | Description |
|---|---|---|
| `FinanceRepaymentOutstanding/Amount` | string ≤22 |  |
| `FinanceRepaymentOutstanding/Currency` | string 1–3 | Currency. |

**`PrincipalRepaymentAmount`**

| Path | Type / length | Description |
|---|---|---|
| `PrincipalRepaymentAmount/Amount` | string ≤22 |  |
| `PrincipalRepaymentAmount/Currency` | string 1–3 | Currency. |

**`InterestRepaymentAmount`**

| Path | Type / length | Description |
|---|---|---|
| `InterestRepaymentAmount/Amount` | string ≤22 |  |
| `InterestRepaymentAmount/Currency` | string 1–3 | Currency. |

**`TotalRepaymentAmount`**

| Path | Type / length | Description |
|---|---|---|
| `TotalRepaymentAmount/Amount` | string ≤22 |  |
| `TotalRepaymentAmount/Currency` | string 1–3 | Currency. |

**`COFDetails`**

| Path | Type / length | Description |
|---|---|---|
| `COFDetails/InterestRepaymentAmount` | group | The amount of cost of funds repaid in this repayment. |
| `COFDetails/InterestRepaymentAmount/Amount` | string ≤22 |  |
| `COFDetails/InterestRepaymentAmount/Currency` | string 1–3 | Currency. |
| `COFDetails/WaiveInterestRefund` | boolean | COF interest refund due to over or underpayment is waived. Boolean values. |
| `COFDetails/FundingPartyRepaymentAmount` | group | The COF amount of principal repaid in this repayment. |
| `COFDetails/FundingPartyRepaymentAmount/Amount` | string ≤22 |  |
| `COFDetails/FundingPartyRepaymentAmount/Currency` | string 1–3 | Currency. |
| `COFDetails/FundingPartyInterestRepaymentAmount` | group | The total amount of cost of funds due to the funding party. |
| `COFDetails/FundingPartyInterestRepaymentAmount/Amount` | string ≤22 |  |
| `COFDetails/FundingPartyInterestRepaymentAmount/Currency` | string 1–3 | Currency. |
| `COFDetails/RepayFundingParty` | boolean | Cost of funds interest repay to funding party. Boolean values. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFIRFRPY`

| Path | Allowed values |
|---|---|
| `RepaymentAction` | `F` Final payment; `I` Interest to date then principal; `T` Last pay then transfer; `M` Minimum repayment; `O` Other; `P` Principal plus interest to date |
| `RepaymentPayer` | `D` Debit party; `F` Finance party |


### Sample (`TFIRFRPY`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFIRFRPY</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFIRFRPY>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>FIN</c:Product>
    </m:Context>
    <m:MasterRef>FIN2600101</m:MasterRef>
    <m:EBankMasterRef>CH-FIN-000101</m:EBankMasterRef>
    <m:EBankEventRef>CH-FIN-000101-02</m:EBankEventRef>
    <m:RepaymentAction>F</m:RepaymentAction>
    <m:RepaymentReference>RPY-2026-10-18</m:RepaymentReference>
    <m:ValueDate>2026-10-20</m:ValueDate>
    <m:RepaymentPayer>D</m:RepaymentPayer>
    <m:PrincipalRepaymentAmount>
      <c:Amount>500000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:PrincipalRepaymentAmount>
    <m:InterestRepaymentAmount>
      <c:Amount>4200.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:InterestRepaymentAmount>
    <m:TotalRepaymentAmount>
      <c:Amount>504200.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:TotalRepaymentAmount>
    <m:PrincipalAccount>1234567890</m:PrincipalAccount>
    <m:CustomerInstructions>Debit the operating account.</m:CustomerInstructions>
  </m:TFIRFRPY>
</ServiceRequest>
```

One body serves every repayment, whatever the underlying finance (import/export LC, collection, open account, invoice, SCF, standalone). `RepaymentAction` codes include F final payment and I interest-only; see the enumeration table. `RepaymentPayer` is D debit party or F finance party.

## Shape: Assignment of proceeds / transfer

Canonical message: `TFELCAOP` – Assignment of proceeds / transfer. Creates: Export Letter Of Credit assignment of proceeds event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFELCAOP` | 100% | none |
| `TFELCTRF` | 97% | omits `AssigneeDetailAs` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Amount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `SecondBeneficiary` | party |  |
| `Sender` | party |  |
| `NextAdvisingBank` | party | Next advising bank. |
| `AdviseThru` | party |  |
| `Instructions` | string |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `eBankCorpRef` | string ≤20 | Reference of the master in the external system. |
| `AdviseBy` | string 1–1 | Code for transfer method format, defined by its domain list. Code for transfer method format, defined by its domain list. Available values: 'e'='E-... |
| `ExpiryDate` | date | Expiry date. |
| `ExpiryPlace` | string 1–29 | Expiry place. |
| `FirstBeneficiaryReference` | string 1–34 |  |
| `AdviseDirect` | boolean | To be advised direct Y or N. Boolean values. |
| `AvailableWith` | group |  |
| `ShipmentDate` | date | Shipment date. |
| `ShipmentPeriod` | string ≤402 | Shipment period. |
| `IncoTerm` | string 1–3 | Incoterm (for example, CFR - Cost and Freight). |
| `IncoPlace` | string ≤142 |  |
| `GoodsCode` | string 1–10 | TI Plus goods code. |
| `HSCode` | string ≤65 |  |
| `Goods` | string ≤26800 | Description of Goods. |
| `PresentationDays` | int | Presentation days. |
| `PresentationPeriod` | string ≤148 | Presentation period text. |
| `PartShipment` | string 1–35 | Partial shipment. Available values (before SWIFT2018): 'A'='Allowed', 'NA'='Not allowed', 'N'='None', 'O'='Other' or text up to 35 chars. As from S... |
| `Transhipment` | string 1–35 | Transhipment. Available values (before SWIFT2018): 'A'='Allowed', 'NA'='Not allowed', 'N'='None', 'O'='Other' or text up to 35 chars. As from SWIFT... |
| `NotifyAmendments` | boolean | Direct amendment allowed. Boolean values. |
| `SubstituteInvoice` | boolean | Substitute invoices. Boolean values. |
| `SubstitutionText` | string ≤222 | Invoice substitution instructions. |
| `ConfirmationDetails` | group | Confirmation details. |
| `Confirmation` | string 1–1 | Confirmation Instructions as entered by the Issuing Bank. Available values: 'C'='Confirmed', 'M'='May Add', 'W'='Unconfirmed'. |
| `SpecialPaymentConditions` | group | Special payment conditions. |
| `InstructionsToBeneficiary` | string ≤222 | Instructions to Beneficiary. |
| `InstructionsFromTransferBank` | string ≤804 |  |
| `AssigneeDetailAs` | group |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFELCAOP`

**`Amount`**

| Path | Type / length | Description |
|---|---|---|
| `Amount/Amount` | string ≤22 |  |
| `Amount/Currency` | string 1–3 | Currency. |

**`AvailableWith`**

| Path | Type / length | Description |
|---|---|---|
| `AvailableWith/Type` | string 1–1 | Available with bank. Available values: 'O'='(named bank)', 'H'='Advise through bank', 'A'='Advising bank',... |
| `AvailableWith/Bank` | string 1–175 | Available With bank name and address. |
| `AvailableWith/Ctry` | string 1–2 | Available With country. |
| `AvailableWith/City` | string 1–65 | Available With city. |
| `AvailableWith/LegalEntityIdentifier` | string ≤20 |  |

**`ConfirmationDetails`**

| Path | Type / length | Description |
|---|---|---|
| `ConfirmationDetails/ConfirmationCharges` | string 1–1 | The party responsible for the payment of confirmation charges. Available values: 'A'='Applicant', 'B'='Bene... |
| `ConfirmationDetails/RequestedConfirmationParty` | party | Requested confirmation party. |
| `ConfirmationDetails/RequestedConfirmationPartyRole` | string ≤3 | Available values: 'THB'='Advise through bank', 'ADV'='Next advising bank', 'OTH'='Other bank', 'OUR'='Ourse... |

**`SpecialPaymentConditions`**

| Path | Type / length | Description |
|---|---|---|
| `SpecialPaymentConditions/ForBeneficiary` | string ≤53600 | Special payment conditions for beneficiary. |
| `SpecialPaymentConditions/ForReceivingBank` | string ≤53600 | Special payment conditions for receiving bank. |

**`AssigneeDetailAs`**

| Path | Type / length | Description |
|---|---|---|
| `AssigneeDetailAs/AssigneeDetailA` | group ×n |  |
| `AssigneeDetailAs/AssigneeDetailA/UniqueID` | string |  |
| `AssigneeDetailAs/AssigneeDetailA/Narrative` | string |  |
| `AssigneeDetailAs/AssigneeDetailA/TheirReference` | string |  |
| `AssigneeDetailAs/AssigneeDetailA/Amount` | long | Amount. |
| `AssigneeDetailAs/AssigneeDetailA/AmountCcy` | string 1–3 | Currency mnemonic. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFELCAOP`

| Path | Allowed values |
|---|---|
| `AdviseBy` | `e` E-mail; `o` E-mail Account Officer; `5` Fax; `a` Gateway; `i` Internal; `1` Mail; `6` No doc required; `4` SWIFT; `2` Telex; `3` Telex via SWIFT |
| `AvailableWith/Type` | `O` (named bank); `H` Advise through bank; `A` Advising bank; `W` Any bank; `T` Any bank in (city); `U` Any bank in (country); `I` Issuing Bank; `S` Ourselves; `R` Received from bank |
| `PartShipment` | `A` Allowed; `NA` Not allowed; `N` None; `O` Other; `C` Conditional |
| `Transhipment` | `A` Allowed; `NA` Not allowed; `N` None; `O` Other; `C` Conditional |
| `ConfirmationDetails/ConfirmationCharges` | `A` Applicant; `B` Beneficiary |
| `ConfirmationDetails/RequestedConfirmationPartyRole` | `THB` Advise through bank; `ADV` Next advising bank; `OTH` Other bank; `OUR` Ourselves; `RAB` Received from advising bank |
| `Confirmation` | `C` Confirmed; `M` May Add; `W` Unconfirmed |


### Sample (`TFELCAOP`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFELCAOP</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFELCAOP>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>ELC</c:Product>
    </m:Context>
    <m:MasterRef>ELC2600012</m:MasterRef>
    <m:Amount>
      <c:Amount>100000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:Amount>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:Instructions>Assign 100,000.00 of proceeds to the second beneficiary.</m:Instructions>
    <m:eBankMasterRef>CH-ELC-000012</m:eBankMasterRef>
    <m:eBankEventRef>CH-ELC-000012-05</m:eBankEventRef>
  </m:TFELCAOP>
</ServiceRequest>
```

## Shape: Undertaking beneficiary response

Canonical message: `TFUTRBCR` – Undertaking beneficiary response. Creates: Undertaking Received - Beneficiary Response to Cancel event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFUTRBCR` | 100% | none |
| `TFUTRBMR` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `OriginalEventReference` | string ≤6 | Original event reference to which the response relates. e.g. AMD001/CAN001. |
| `Sender` | party |  |
| `ResponseDate` | date | Response date. |
| `BeneficiaryResponseDetails` | group | Beneficiary response details. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFUTRBCR`

**`BeneficiaryResponseDetails`**

| Path | Type / length | Description |
|---|---|---|
| `BeneficiaryResponseDetails/ResponseType` | string 1–1 | Beneficiary response (accept / reject). Beneficiary response (accept / reject). Available values: 'A'='Appr... |
| `BeneficiaryResponseDetails/ResponseNarrative` | string ≤32000 | Response narrative from the sender. |
| `BeneficiaryResponseDetails/ReasonForRejection` | string ≤222 | Reason why cancellation has been refused. |
| `BeneficiaryResponseDetails/ResponseToInstructingParty` | string ≤222 | Response to instructing party. |
| `BeneficiaryResponseDetails/NotesToNextParty` | string ≤222 | Notes to next party. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFUTRBCR`

| Path | Allowed values |
|---|---|
| `BeneficiaryResponseDetails/ResponseType` | `A` Approved; `R` Rejected |


### Sample (`TFUTRBCR`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFUTRBCR</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFUTRBCR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>UTR</c:Product>
    </m:Context>
    <m:MasterRef>UTR2600003</m:MasterRef>
    <m:eBankMasterRef>CH-UTR-000003</m:eBankMasterRef>
    <m:eBankEventRef>CH-UTR-000003-02</m:eBankEventRef>
    <m:OriginalEventReference>AMD-1</m:OriginalEventReference>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:ResponseDate>2026-10-18</m:ResponseDate>
    <m:BeneficiaryResponseDetails>
      <c:ResponseType>A</c:ResponseType>
      <c:ResponseNarrative>Beneficiary accepts.</c:ResponseNarrative>
    </m:BeneficiaryResponseDetails>
  </m:TFUTRBCR>
</ServiceRequest>
```

## Shape: Collection payment

Canonical message: `TFCOLCON` – Sent by the Drawee to the Collecting Bank providing approval and (or) payment details relating to an Inward (Import) Collection. Creates: Inward Collection Continue payment event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFCOLCON` | 100% | none |
| `TFCOLPAY` | 96% | omits `OriginatingPaymentEvent` |
| `TFOCOLCON` | 100% | none |
| `TFOCOLPAY` | 96% | omits `OriginatingPaymentEvent` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Sender` | party | The Sender of the message. |
| `Subject` | string ≤35 | Subject of correspondence. |
| `CorresType` | string ≤1 | Correspondence type - G = General - Q = Query - A = Answer - N = Ancillary. |
| `EventFurtherID` | string 1–5 | Used to further define the type of event. The value in this field must be a valid bank defined TF dictionary value beginning EVTxx e.g. EVT01 - Acc... |
| `ExtraInformation` | string ≤35 | Free format extra information for the event. This information will be shown on the Master summary in the WIP and event history, dashboard and Maste... |
| `RelatedRef` | string ≤20 | Related reference. |
| `MessageText` | string ≤10000 | Correspondence. |
| `RelatedInformation` | string ≤1820 | Narrative Description of the Original Message. |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `eBankCorpRef` | string ≤20 | Reference of the master in the external system. |
| `FormOfUndertaking` | string ≤4 | Available values: 'DGAR'='Demand guarantee', 'UNDK'='Dependent undertaking', 'DOCR'='Documentary credit', 'STBY'='Standby letter of credit'. |
| `Function` | string ≤8 | MT759 specific. MT759 specific. Available values: 'FRAUDMSG'='Advice of a fraud attempt', 'ISSAMEND'='Amendment of a free-form undertaking such as... |
| `Narrative` | string ≤10050 |  |
| `CollectionAmount` | group | The payment amount collected. |
| `CollectionDate` | date | The value date of the payment amount. |
| `ExpireCollection` | boolean | Expire collection flag. Boolean values. |
| `Financing` | group | SImilar to GWR800, containing fields for creating subsidiary financing event to GWRReceivedDocument. |
| `PaymentAction` | string 1–1 | Available values: 'T'='Accept', 'c'='Adjust maturity', 'K'='Approve authorisation request', 'A'='Await documents', 'I'='Await replacement invoices'... |
| `PaymentDraftss` | group |  |
| `OriginatingPaymentEvent` | string ≤6 |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |
| `SubsidiaryExtraData` | group |  |

#### Nested structures of `TFCOLCON`

**`CollectionAmount`**

| Path | Type / length | Description |
|---|---|---|
| `CollectionAmount/Amount` | string ≤22 |  |
| `CollectionAmount/Currency` | string 1–3 | Currency. |

**`Financing`**

| Path | Type / length | Description |
|---|---|---|
| `Financing/Amount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `Financing/Amount/Amount` | string ≤22 |  |
| `Financing/Amount/Currency` | string 1–3 | Currency. |
| `Financing/AutoMonthlyDebit` | boolean | Boolean values. |
| `Financing/AutoRepay` | boolean | Boolean values. |
| `Financing/DebitParty` | party |  |
| `Financing/EffectiveDate` | date |  |
| `Financing/FinanceAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `Financing/FinanceAmount/Amount` | string ≤22 |  |
| `Financing/FinanceAmount/Currency` | string 1–3 | Currency. |
| `Financing/FinancePercent` | decimal(17,2) | Percentage. Available Values: 0 to 100 |
| `Financing/FinanceCurrency` | string 1–3 | Currency Code. |
| `Financing/ExchangeRate` | string | Exchange rate. |
| `Financing/FinanceToParty` | party |  |
| `Financing/FinanceInterestDate` | date |  |
| `Financing/InterestRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/InterestSettlementCcy` | string 1–3 | Currency Code. |
| `Financing/MarginRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/MaturityDate` | date |  |
| `Financing/PenaltyPercent` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/PreferredChargeCcy` | string 1–3 | Currency Code. |
| `Financing/StartDate` | date |  |
| `Financing/TrustReceiptLoan` | boolean | Boolean values. |
| `Financing/UseSchedule` | boolean | Boolean values. |
| `Financing/RelatedMasterRef` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `Financing/InterestType` | string 1–1 | Available values: 'D'='Interest in advance - discount', 'Y'='Interest in advance - discount to yield', 'I'=... |
| `Financing/InterestFromAfter` | string 1–1 | Available values: 'A'='After', 'F'='From', 'E'='From (exclusive)'. |
| `Financing/RateType` | string 1–1 | Available values: 'F'='Fixed', 'S'='Special', 'V'='Variable'. |
| `Financing/BaseCode` | string 2–5 |  |
| `Financing/ActualBaseOrGroupRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999... |
| `Financing/DiffCode` | string 2–2 |  |
| `Financing/NarrativeDetails` | string ≤32000 |  |
| `Financing/InstructionsToFinanceParty` | string ≤32000 |  |
| `Financing/InstructionsToDebitParty` | string ≤32000 |  |
| `Financing/PenaltyRateCode` | string 2–2 |  |
| `Financing/GroupRate` | string 1–6 |  |
| `Financing/MessageToBank` | string ≤32000 |  |
| `Financing/eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `Financing/eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `Financing/ProductCode` | string ≤4 |  |
| `Financing/RepaymentDetailss` | group |  |
| `Financing/RepaymentDetailss/RepaymentDetails` | group ×n | Irregular repayment details. |
| `Financing/RepaymentDetailss/RepaymentDetails/Amount` | long |  |
| `Financing/RepaymentDetailss/RepaymentDetails/Percent` | decimal(17,2) | The percent figure for the data type. Available Values: 0 to 100 |
| `Financing/RepaymentDetailss/RepaymentDetails/Date` | date |  |
| `Financing/RegularRepaymentDetails` | group | Regular repayment details. |
| `Financing/RegularRepaymentDetails/AmountOrPercent` | group |  |
| `Financing/RegularRepaymentDetails/AmountOrPercent/Amount` | long |  |
| `Financing/RegularRepaymentDetails/AmountOrPercent/Percent` | decimal(17,2) | Percentage. Available Values: 0 to 100 |
| `Financing/RegularRepaymentDetails/Date` | date |  |
| `Financing/RegularRepaymentDetails/Period` | group |  |
| `Financing/RegularRepaymentDetails/Period/Unit` | string 1–1 | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `Financing/RegularRepaymentDetails/Period/Number` | int | Number of days (months) etc e.g. 30. |
| `Financing/RegularRepaymentDetails/Period/DayInMonth` | int | Day in month for renewal. |
| `Financing/FinancePeriod` | group |  |
| `Financing/FinancePeriod/TenorDays` | int | Tenor element of the term. |
| `Financing/FinancePeriod/TenorPeriod` | string | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `Financing/InterestPeriod` | group |  |
| `Financing/InterestPeriod/Unit` | string 1–1 | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `Financing/InterestPeriod/Number` | int | Number of days (months) etc e.g. 30. |
| … | | 210 more paths – see `TI.<message>.xsd.txt` for `Financing` |

**`PaymentDraftss`**

| Path | Type / length | Description |
|---|---|---|
| `PaymentDraftss/PaymentDrafts` | group ×n |  |
| `PaymentDraftss/PaymentDrafts/ActualRateMargin` | decimal TotalDigits: 7 | Available Values: -100000 to 100000 |
| `PaymentDraftss/PaymentDrafts/AmountOrPercentAmount` | long |  |
| `PaymentDraftss/PaymentDrafts/AmountOrPercentPercent` | decimal(17,2) | The percent figure for the data type. Available Values: 0 to 100 |
| `PaymentDraftss/PaymentDrafts/AmountToCollectAmount` | long | Amount. |
| `PaymentDraftss/PaymentDrafts/AmountToCollectCurrency` | string 1–3 | Currency mnemonic. |
| `PaymentDraftss/PaymentDrafts/DateDue` | date |  |
| `PaymentDraftss/PaymentDrafts/DraftId` | string |  |
| `PaymentDraftss/PaymentDrafts/EarlyOrLate` | string ≤1 | Available values: 'E'='Early discount', 'L'='Late interest', 'N'='Neither'. |
| `PaymentDraftss/PaymentDrafts/EarlyOrLatePaymentAmountAmount` | long | Amount. |
| `PaymentDraftss/PaymentDrafts/EarlyOrLatePaymentAmountCurrency` | string 1–3 | Currency mnemonic. |
| `PaymentDraftss/PaymentDrafts/EndDate` | date |  |
| `PaymentDraftss/PaymentDrafts/EndPeriod` | string ≤35 |  |
| `PaymentDraftss/PaymentDrafts/GraceDays` | int |  |
| `PaymentDraftss/PaymentDrafts/InterestDaysBasis` | string 1–1 | Available values: '0'='0 - 365 day', '1'='1 - 360 day', '2'='2 - 365-6 day', '3'='3 - 360 / 30 day month',... |
| `PaymentDraftss/PaymentDrafts/StartAfterGrace` | string 1–1 | Available values: 'A'='After', 'B'='Before'. |
| `PaymentDraftss/PaymentDrafts/StartDate` | date |  |
| `PaymentDraftss/PaymentDrafts/StartFrom` | string 1–35 | Available values: 'Acceptance date'='Acceptance date', 'Air waybill date'='Air waybill date', 'Arrival of g... |
| `PaymentDraftss/PaymentDrafts/WaiveIfRefused` | boolean | Boolean values. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFCOLCON`

| Path | Allowed values |
|---|---|
| `FormOfUndertaking` | `DGAR` Demand guarantee; `UNDK` Dependent undertaking; `DOCR` Documentary credit; `STBY` Standby letter of credit |
| `Function` | `FRAUDMSG` Advice of a fraud attempt; `ISSAMEND` Amendment of a free-form undertaking such as a dependent guarantee; `CLSVCLOS` Closing of client service call by Trade Operations; `REQFINAN` Financing request; `GENINFAD` General information advice; `ISSUANCE` Issue of a free-form undertaking such as a dependent guarantee; `CLSVOPEN` Opening of client service call by Trade Operations; `OTHERFNC` Other request; `PREDENOT` Pre-debit notification; `REIMBURS` Request related to a reimbursement; `REQAMEND` Request to amend an undertaking; `REXTMATU` Request to extend the maturity date; `REQISSUE` Request to issue an undertaking; `TRANSFER` Transfer of an undertaking |
| `Financing/InterestType` | `D` Interest in advance - discount; `Y` Interest in advance - discount to yield; `I` Interest in advance - interest to yield; `S` Interest in advance - standard; `E` Interest in arrears - standard |
| `Financing/InterestFromAfter` | `A` After; `F` From; `E` From (exclusive) |
| `Financing/RateType` | `F` Fixed; `S` Special; `V` Variable |
| `Financing/RegularRepaymentDetails/Period/Unit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/FinancePeriod/TenorPeriod` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/InterestPeriod/Unit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/Collateral/CollateralDetail/OtherDetailsType` | `G` General; `S` Specific |
| `Financing/FinanceResponse` | `A` Offer is accepted; `R` Offer is rejected; `G` Offer is renegotiated |
| `Financing/InterestScheduleTiers/InterestScheduleTier/PeriodUnit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Financing/SettlementInstructionss/SettlementInstructions/PayReceiveFlag` | `P` Pay; `R` Receive |
| `Financing/SettlementInstructionss/SettlementInstructions/MovementType` | `C` Charge; `I` Interest; `P` Principal |
| `Financing/SettlementInstructionss/SettlementInstructions/PayFundsVia` | `T` Account transfer; `A` Acct at nostro/vostro; `B` Beneficiary bank; `C` Beneficiary bank with cover; `X` Clearing ID; `I` Intermediary bank; `P` Intermediary with cover; `O` Other account |
| `Financing/SettlementInstructionss/SettlementInstructions/SettlesBy` | `A` Account; `X` Clearing ID; `N` Nostro; `O` Other account; `V` Vostro |
| `Financing/SettlementInstructionss/SettlementInstructions/SwiftCharges` | `BEN` Beneficiary; `OUR` Our; `SHA` Shared |
| `PaymentAction` | `T` Accept; `c` Adjust maturity; `K` Approve authorisation request; `A` Await documents; `I` Await replacement invoices; `p` Claim against counter; `B` Claim reimbursement; `e` Extend; `n` Finance offer; `F` Forward documents only; `Y` General request to applicant; `H` General request to issuer; `Z` General request to presenter; `a` General request to transferee; `i` Intent to pay; `V` Issue trust receipt; `W` Issue trust receipt loan; `P` Pay; `C` Pay & claim reimbursement; `o` Pay and claim against counter; `M` Pay at maturity; `j` Pay early; `b` Pay from suspense; `D` Pay with discrepancies; `X` Pay with financing; `L` Refuse authorisation request; `G` Refuse documents; `R` Reject; `Q` Reject and claim refund; `u` Reject and continue; `x` Repay finance; `U` Request acceptance; `N` Request approval from applicant; `J` Request authorisation to pay; `d` Request original document; `q` Request pay or extend; `E` Request payment; `r` Reverse payment; `S` Send documents on approval |
| `PaymentDraftss/PaymentDrafts/EarlyOrLate` | `E` Early discount; `L` Late interest; `N` Neither |
| `PaymentDraftss/PaymentDrafts/InterestDaysBasis` | `0` 0 - 365 day; `1` 1 - 360 day; `2` 2 - 365-6 day; `3` 3 - 360 / 30 day month; `4` 4 - 365 / 30 day month; `5` 5 - 365-6 / 30 day month |
| `PaymentDraftss/PaymentDrafts/StartAfterGrace` | `A` After; `B` Before |
| `PaymentDraftss/PaymentDrafts/StartFrom` | `Acceptance date` Acceptance date; `Air waybill date` Air waybill date; `Arrival of goods date` Arrival of goods date; `Bill of exchange date` Bill of exchange date; `Bill of lading date` Bill of lading date; `Invoice date` Invoice date; `Maturity date` Maturity date; `Sight` Sight |


### Sample (`TFCOLCON`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFCOLCON</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFCOLCON>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>IDC</c:Product>
    </m:Context>
    <m:MasterRef>IDC2600020</m:MasterRef>
    <m:Sender>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Sender>
    <m:Subject>Payment instruction</m:Subject>
    <m:CorresType>G</m:CorresType>
    <m:eBankMasterRef>CH-IDC-000020</m:eBankMasterRef>
    <m:eBankEventRef>CH-IDC-000020-03</m:eBankEventRef>
    <m:CollectionAmount>
      <c:Amount>75000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:CollectionAmount>
    <m:CollectionDate>2026-10-20</m:CollectionDate>
    <m:PaymentAction>T</m:PaymentAction>
  </m:TFCOLCON>
</ServiceRequest>
```

Drawee payment or "continue payment" instruction under a collection. `Financing` lets the payment come with a financing request. `PaymentAction` is a bank-tailored code. `PaymentDraftss` lists the drafts being paid.

## Shape: Invoice finance request

Canonical message: `TFBUYFIN` – Invoice finance request. Creates: Invoice Buyer Centric Finance create event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFBUYFIN` | 100% | none |
| `TFSELFIN` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `TheirRef` | string 1–34 |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `Programme` | string ≤35 | Programme identifier - indicates the credit line to which the invoice relates. |
| `Seller` | string 1–20 | The customer who is the credit party on the invoice(s). |
| `Buyer` | string 1–20 | The customer who is the debit party on the invoice(s). |
| `AnchorParty` | string 1–20 | The buyer or seller mnemonic: the customer of the bank who originates the invoice(s). |
| `ReceivedOn` | date | Date of receipt of the incoming message. |
| `ProductType` | string ≤3 | Product type code. |
| `MaturityDate` | date | The due date of the advance - the invoice(s) settlement date is used as the default. |
| `FinanceCurrency` | string 1–3 | The currency of the advance - the invoice(s) currency is used as the default. |
| `FinancePercent` | decimal(17,2) | The percentage of the amount to be financed. Available Values: 0 to 100 |
| `FinanceDate` | date | The start date of the advance - required for interest calculation. |
| `FinanceInstructions` | string ≤32000 | Any instructions received from the customer relating to the advance. |
| `FinanceOfferOnly` | boolean | Boolean values. |
| `InvoiceNumberss` | group |  |
| `DealerFinanceTo` | string 1–1 | Finance to party for Dealer Finance. Available values: 'B'='Buyer', 'S'='Seller'. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFBUYFIN`

**`InvoiceNumberss`**

| Path | Type / length | Description |
|---|---|---|
| `InvoiceNumberss/InvoiceNumbers` | group ×n |  |
| `InvoiceNumberss/InvoiceNumbers/InvoiceNumber` | string 1–34 | The invoice number of the invoice to be financed. |
| `InvoiceNumberss/InvoiceNumbers/IssueDate` | date | The issue date of the invoice to be financed. |
| `InvoiceNumberss/InvoiceNumbers/OutstandingAmount` | string ≤22 |  |
| `InvoiceNumberss/InvoiceNumbers/OutstandingAmountCurrency` | string 1–3 |  |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFBUYFIN`

| Path | Allowed values |
|---|---|
| `DealerFinanceTo` | `B` Buyer; `S` Seller |


### Sample (`TFBUYFIN`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFBUYFIN</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFBUYFIN>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>INV</c:Product>
    </m:Context>
    <m:MasterRef>SCF2600008</m:MasterRef>
    <m:TheirRef>FIN-REQ-1</m:TheirRef>
    <m:eBankMasterRef>CH-INV-000008</m:eBankMasterRef>
    <m:eBankEventRef>CH-INV-000008-02</m:eBankEventRef>
    <m:Programme>PROG01</m:Programme>
    <m:MaturityDate>2027-01-20</m:MaturityDate>
    <m:FinanceCurrency>USD</m:FinanceCurrency>
    <m:FinancePercent>90.00</m:FinancePercent>
    <m:FinanceDate>2026-10-20</m:FinanceDate>
  </m:TFBUYFIN>
</ServiceRequest>
```

## Shape: Invoice create or amend

Canonical message: `TFINVAMD` – Invoice create or amend. Creates: Invoice amend event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFINVAMD` | 100% | none |
| `TFINVNEW` | 93% | omits `AmendmentDate`, `AmendmentNarrative` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `TheirRef` | string 1–34 |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `Programme` | string ≤35 | Programme identifier - indicates the credit line to which the invoice relates. |
| `Seller` | string 1–20 | The customer who is the credit party on the invoice(s). |
| `Buyer` | string 1–20 | The customer who is the debit party on the invoice(s). |
| `AnchorParty` | string 1–20 | The buyer or seller mnemonic: the customer of the bank who originates the invoice(s). |
| `ReceivedOn` | date | Date of receipt of the incoming message. |
| `BatchID` | string 1–20 | A batch identifier associated with the invoice, provided by the customer. |
| `InvoiceNumber` | string 1–34 | Invoice identifier - a unigue identifier for the invoice(s) , provided by the customer  SCFBDS5 (PG). |
| `IssueDate` | date | The date the invoice(s) were issued. |
| `FaceValue` | group | The total amount of the invoice in the denominated currency. |
| `AdjustmentAmount` | group | The amount of any credit notes. |
| `AdjustmentDirection` | string ≤4 |  |
| `OutstandingAmount` | group | Face value amount less any related adjustments. |
| `SettlementDate` | date | Invoice payment due date. |
| `RelatedGoodsOrServices` | string ≤222 | Products or services covered by the invoice and for which it was raised. |
| `AdditionalDetails` | string ≤370 | Any additional information relating to the invoice(s). |
| `SenderToReceiver` | string ≤148 | Sender to receiver information. |
| `PaymentInstructions` | string ≤222 | Payment instructions for the invoice(s) DC - Gateway validation :: Set type. |
| `InvoiceApproved` | boolean | For buyer centric invoices - indicates whether the invoice has been approved by the buyer for financing. Boolean values. |
| `ProductType` | string ≤3 |  |
| `VendorCode` | string ≤35 | Unique code provided by a customer to identify a counterparty. |
| `AmendmentDate` | date |  |
| `AmendmentNarrative` | string ≤222 |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFINVAMD`

**`FaceValue`**

| Path | Type / length | Description |
|---|---|---|
| `FaceValue/Amount` | string ≤22 |  |
| `FaceValue/Currency` | string 1–3 | Currency. |

**`AdjustmentAmount`**

| Path | Type / length | Description |
|---|---|---|
| `AdjustmentAmount/Amount` | string ≤22 |  |
| `AdjustmentAmount/Currency` | string 1–3 | Currency. |

**`OutstandingAmount`**

| Path | Type / length | Description |
|---|---|---|
| `OutstandingAmount/Amount` | string ≤22 |  |
| `OutstandingAmount/Currency` | string 1–3 | Currency. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


### Sample (`TFINVAMD`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFINVAMD</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFINVAMD>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>INV</c:Product>
    </m:Context>
    <m:MasterRef>INV2600155</m:MasterRef>
    <m:TheirRef>INV-77120</m:TheirRef>
    <m:eBankMasterRef>CH-INV-000155</m:eBankMasterRef>
    <m:eBankEventRef>CH-INV-000155-01</m:eBankEventRef>
    <m:Programme>PROG01</m:Programme>
    <m:InvoiceNumber>INV-77120</m:InvoiceNumber>
    <m:IssueDate>2026-10-10</m:IssueDate>
    <m:FaceValue>
      <c:Amount>60000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:FaceValue>
    <m:SettlementDate>2027-01-10</m:SettlementDate>
  </m:TFINVAMD>
</ServiceRequest>
```

## Shape: Customer payment

Canonical message: `TFCPCCRT` – Customer payment. Creates: Outward Customer Payment create event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFCPCCRT` | 100% | none |
| `TFCPHCRT` | 100% | none |
| `TFCPICRT` | 100% | none |
| `TFCPSCRT` | 88% (loose) | adds `NumberOfPayments`, `Freq`, `DayInMonth`, `NextPaymentDate`, `FinalPayment`, `StOrderAmtFlag` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Sender` | party |  |
| `OrderingParty` | party |  |
| `Remitter` | party |  |
| `RemitterAccount` | string 1–34 |  |
| `RemitterOtherAccNo` | string 1–34 | A 34 character field used to identify a specific account within SWIFT Party details. |
| `RemitterChargeAcc` | string 1–34 |  |
| `RemitterChargeOtherAcc` | string 1–34 |  |
| `Instructions` | string ≤222 |  |
| `RemitterAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `BeneficiaryAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `OurChargesFor` | string 1–1 | Our charges for. Available values: 'B'='Buyer', 'S'='Seller'. |
| `BenChargesFor` | string 1–1 | Beneficiary charges for. Available values: 'B'='Buyer', 'S'='Seller'. |
| `ReceiveDate` | date |  |
| `PaymentDate` | date |  |
| `PreferredChargeCcy` | string 1–3 | Currency Code. |
| `Beneficiary` | party |  |
| `BenAccountNo` | string 1–34 | A 34 character field used to identify a specific account within SWIFT Party details. |
| `BeneficiaryAccount` | string 1–34 |  |
| `BenPaymentDetails` | string ≤148 |  |
| `BeneficiaryBank` | party |  |
| `BeneficiaryBankId` | string 1–34 | A 34 character field used to identify a specific account within SWIFT Party details. |
| `IntermediaryBank` | party |  |
| `IntermediaryBankId` | string 1–34 | A 34 character field used to identify a specific account within SWIFT Party details. |
| `PaymentMethod` | string 2–2 | Transfer method. |
| `SettlesBy` | string 1–1 | Settles by. Available values: 'T'='Account transfer', 'A'='Acct at nostro/vostro', 'B'='Beneficiary bank', 'C'='Beneficiary bank with cover', 'X'='... |
| `InstructionCode1` | group | Instruction code 1. |
| `InstructionCode2` | group | Instruction code 2. |
| `InstructionCode3` | group | Instruction code 3. |
| `InstructionCode4` | group | Instruction code 4. |
| `InstructedAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `AmountBasis` | string 1–1 | starting point when working out amounts when FX deal involved. Either Remitter or Beneficiary. starting point when working out amounts when FX deal... |
| `ExchangeRate` | string | Exchange rate. |
| `BankCodes` | group |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `ProductType` | string ≤3 | TI Product type. |
| `SpecialFields` | group |  |
| `FXContractNo` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `AdditionalParties` | group | This contains the additional parties to a transaction. Only applicable for Issue/Create and Amend events. |
| `LicenseDetails` | group |  |
| `Nostro` | group |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFCPCCRT`

**`RemitterAmount`**

| Path | Type / length | Description |
|---|---|---|
| `RemitterAmount/Amount` | string ≤22 |  |
| `RemitterAmount/Currency` | string 1–3 | Currency. |

**`BeneficiaryAmount`**

| Path | Type / length | Description |
|---|---|---|
| `BeneficiaryAmount/Amount` | string ≤22 |  |
| `BeneficiaryAmount/Currency` | string 1–3 | Currency. |

**`InstructionCode1`**

| Path | Type / length | Description |
|---|---|---|
| `InstructionCode1/Code` | string ≤4 | Code. Available values: 'CHQB'='CHQB', 'CORT'='CORT', 'HOLD'='HOLD', 'INTC'='INTC', 'PHOB'='PHOB', 'PHOI'='... |
| `InstructionCode1/Text` | string ≤30 | Text. |

**`InstructionCode2`**

| Path | Type / length | Description |
|---|---|---|
| `InstructionCode2/Code` | string ≤4 | Code. Available values: 'CHQB'='CHQB', 'CORT'='CORT', 'HOLD'='HOLD', 'INTC'='INTC', 'PHOB'='PHOB', 'PHOI'='... |
| `InstructionCode2/Text` | string ≤30 | Text. |

**`InstructionCode3`**

| Path | Type / length | Description |
|---|---|---|
| `InstructionCode3/Code` | string ≤4 | Code. Available values: 'CHQB'='CHQB', 'CORT'='CORT', 'HOLD'='HOLD', 'INTC'='INTC', 'PHOB'='PHOB', 'PHOI'='... |
| `InstructionCode3/Text` | string ≤30 | Text. |

**`InstructionCode4`**

| Path | Type / length | Description |
|---|---|---|
| `InstructionCode4/Code` | string ≤4 | Code. Available values: 'CHQB'='CHQB', 'CORT'='CORT', 'HOLD'='HOLD', 'INTC'='INTC', 'PHOB'='PHOB', 'PHOI'='... |
| `InstructionCode4/Text` | string ≤30 | Text. |

**`InstructedAmount`**

| Path | Type / length | Description |
|---|---|---|
| `InstructedAmount/Amount` | string ≤22 |  |
| `InstructedAmount/Currency` | string 1–3 | Currency. |

**`BankCodes`**

| Path | Type / length | Description |
|---|---|---|
| `BankCodes/BankCode1` | string ≤4 |  |
| `BankCodes/BankCode2` | string ≤4 |  |
| `BankCodes/BankCode3` | string ≤4 |  |
| `BankCodes/BankCode4` | string ≤4 |  |
| `BankCodes/BankCode5` | string ≤4 |  |

**`SpecialFields`**

| Path | Type / length | Description |
|---|---|---|
| `SpecialFields/GW01` | string |  |
| `SpecialFields/GW02` | string |  |
| `SpecialFields/GW03` | string |  |
| `SpecialFields/GW04` | string |  |
| `SpecialFields/GW05` | string |  |

**`AdditionalParties`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalParties/AdditionalPartiesToAdd` | party ×n | Receives details of additional parties via the gateway. Mapped to and displayed under the Party list pane i... |

**`LicenseDetails`**

| Path | Type / length | Description |
|---|---|---|
| `LicenseDetails/LicenseDetail` | group ×n | Provides the link between a product (LC, Collection etc.) and a License. |
| `LicenseDetails/LicenseDetail/OurReference` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `LicenseDetails/LicenseDetail/EBankMasterRef` | string ≤20 |  |
| `LicenseDetails/LicenseDetail/AuthorityReference` | string ≤35 |  |
| `LicenseDetails/LicenseDetail/Number` | string ≤35 |  |
| `LicenseDetails/LicenseDetail/AllocatedAmount` | string ≤22 |  |
| `LicenseDetails/LicenseDetail/AllocatedCurrency` | string 1–3 |  |

**`Nostro`**

| Path | Type / length | Description |
|---|---|---|
| `Nostro/Branch` | string 1–8 | Code of the Main banking entity. |
| `Nostro/NostroCode` | string 1–6 | Nostro Code. |
| `Nostro/Customer` | string 1–20 | Customer mnemonic. |
| `Nostro/BOAccountNumber` | string 1–34 | Account number. |
| `Nostro/SwiftAddress` | string ≤14 | Swift Address. |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFCPCCRT`

| Path | Allowed values |
|---|---|
| `OurChargesFor` | `B` Buyer; `S` Seller |
| `BenChargesFor` | `B` Buyer; `S` Seller |
| `SettlesBy` | `T` Account transfer; `A` Acct at nostro/vostro; `B` Beneficiary bank; `C` Beneficiary bank with cover; `X` Clearing ID; `I` Intermediary bank; `P` Intermediary with cover; `O` Other account |
| `InstructionCode1/Code` | `CHQB` CHQB; `CORT` CORT; `HOLD` HOLD; `INTC` INTC; `PHOB` PHOB; `PHOI` PHOI; `PHON` PHON; `REPA` REPA; `SDVA` SDVA; `TELB` TELB; `TELE` TELE; `TELI` TELI |
| `InstructionCode2/Code` | `CHQB` CHQB; `CORT` CORT; `HOLD` HOLD; `INTC` INTC; `PHOB` PHOB; `PHOI` PHOI; `PHON` PHON; `REPA` REPA; `SDVA` SDVA; `TELB` TELB; `TELE` TELE; `TELI` TELI |
| `InstructionCode3/Code` | `CHQB` CHQB; `CORT` CORT; `HOLD` HOLD; `INTC` INTC; `PHOB` PHOB; `PHOI` PHOI; `PHON` PHON; `REPA` REPA; `SDVA` SDVA; `TELB` TELB; `TELE` TELE; `TELI` TELI |
| `InstructionCode4/Code` | `CHQB` CHQB; `CORT` CORT; `HOLD` HOLD; `INTC` INTC; `PHOB` PHOB; `PHOI` PHOI; `PHON` PHON; `REPA` REPA; `SDVA` SDVA; `TELB` TELB; `TELE` TELE; `TELI` TELI |
| `AmountBasis` | `B` Beneficiary; `R` Remitter |


### Sample (`TFCPCCRT`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFCPCCRT</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFCPCCRT>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>CPC</c:Product>
    </m:Context>
    <m:OrderingParty>
      <c:Customer>ACMEIMP</c:Customer>
    </m:OrderingParty>
    <m:Remitter>
      <c:Customer>ACMEIMP</c:Customer>
    </m:Remitter>
    <m:RemitterAccount>1234567890</m:RemitterAccount>
    <m:RemitterAmount>
      <c:Amount>15000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:RemitterAmount>
    <m:BeneficiaryAmount>
      <c:Currency>USD</c:Currency>
    </m:BeneficiaryAmount>
    <m:OurChargesFor>B</m:OurChargesFor>
    <m:BenChargesFor>B</m:BenChargesFor>
    <m:PaymentDate>2026-10-20</m:PaymentDate>
    <m:Beneficiary>
      <c:NameAddress>SHENZHEN WIDGETS CO LTD
88 NANSHAN AVENUE
SHENZHEN</c:NameAddress>
      <c:Country>CN</c:Country>
    </m:Beneficiary>
    <m:BenAccountNo>6222000011112222</m:BenAccountNo>
    <m:BeneficiaryBank>
      <c:SwiftAddress>ICBKCNBJXXX</c:SwiftAddress>
    </m:BeneficiaryBank>
    <m:eBankMasterRef>CH-CPC-000301</m:eBankMasterRef>
    <m:eBankEventRef>CH-CPC-000301-01</m:eBankEventRef>
  </m:TFCPCCRT>
</ServiceRequest>
```

## Shape: Open account payment presentation

Canonical message: `TFIOARPY` – Open account payment presentation. Creates: Import Open Account request payment event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEOAPAY` | 100% | none |
| `TFEOARPY` | 100% | none |
| `TFIOARPY` | 100% | none |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `EBankMasterRef` | string ≤20 | TSDS-264 (PG). |
| `EBankEventRef` | string ≤20 | TSDS-264 (PG). |
| `EBankCorpRef` | string ≤20 | TSDS-264 (PG). |
| `InvoiceReference` | string 1–34 |  |
| `PurchaseOrderReference` | string 1–34 |  |
| `TransactionMatchingReference` | string 1–34 |  |
| `PresentationDetails` | group | PresentationDate with members on parent. |
| `PresentersDiscrepancies` | string ≤16120 |  |
| `InvoiceIssueDate` | date |  |
| `SettlementTerms` | group | Instruction between two clearing agents stipulating the cash transfer characteristics between the two parties. |
| `PaymentTerms` | group |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFIOARPY`

**`PresentationDetails`**

| Path | Type / length | Description |
|---|---|---|
| `PresentationDetails/PresentationAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the... |
| `PresentationDetails/PresentationAmount/Amount` | string ≤22 |  |
| `PresentationDetails/PresentationAmount/Currency` | string 1–3 | Currency. |
| `PresentationDetails/PresentationDate` | date |  |
| `PresentationDetails/Presenter` | string 1–3 |  |
| `PresentationDetails/PresentingParty` | party |  |
| `PresentationDetails/PresentersReference` | string 1–34 |  |
| `PresentationDetails/PresentersAction` | string ≤3 |  |
| `PresentationDetails/NotesFromPresenter` | string ≤740 |  |

**`SettlementTerms`**

| Path | Type / length | Description |
|---|---|---|
| `SettlementTerms/CdtrAgt` | group | Financial institution that receives the payment transaction on behalf of the account owner, and posts the t... |
| `SettlementTerms/CdtrAgt/BIC` | string ≤14 | Business identifier code. |
| `SettlementTerms/CdtrAgt/NmAndAddr` | group | Name and address of an institution. |
| `SettlementTerms/CdtrAgt/NmAndAddr/Nm` | string 1–70 | Name by which a party is known. |
| `SettlementTerms/CdtrAgt/NmAndAddr/Adr` | group | Information that locates and identifies a specific address, as specified by postal services. |
| `SettlementTerms/CdtrAcct` | group | Account to be credited as a result of an instruction. |
| `SettlementTerms/CdtrAcct/Id` | group | Unique and unambiguous identification for the account between the account owner and the account service ser... |
| `SettlementTerms/CdtrAcct/Id/IBAN` | string 1–34 | International bank account number. |
| `SettlementTerms/CdtrAcct/Id/Othr` | group | Unique identification of an account, as assigned by the account servicer, using an identification scheme. |
| `SettlementTerms/CdtrAcct/Tp` | group | Specifies the nature, or use of the account. |
| `SettlementTerms/CdtrAcct/Tp/Cd` | string 1–4 |  |
| `SettlementTerms/CdtrAcct/Tp/Prty` | string 1–35 | Name of the identification scheme in a free text form. |
| `SettlementTerms/CdtrAcct/Ccy` | string 1–3 | Identification of the currency in which the account is held. |
| `SettlementTerms/CdtrAcct/Nm` | string 1–70 | Name of the account as assigned by the account servicing institution. |

**`PaymentTerms`**

| Path | Type / length | Description |
|---|---|---|
| `PaymentTerms/PaymentTerm` | group ×n |  |
| `PaymentTerms/PaymentTerm/PaymentCodeCode` | string 1–4 |  |
| `PaymentTerms/PaymentTerm/PaymentCodeNumberOfDays` | long |  |
| `PaymentTerms/PaymentTerm/OtherPaymentTerms` | string 1–140 | Specifies payment terms not present in a code list. |
| `PaymentTerms/PaymentTerm/PaymentAmount` | string ≤22 |  |
| `PaymentTerms/PaymentTerm/PaymentCurrency` | string 1–3 |  |
| `PaymentTerms/PaymentTerm/PaymentPercent` | decimal(15,12) | Available Values: 0 to 100 |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


### Sample (`TFIOARPY`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFIOARPY</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFIOARPY>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>IOA</c:Product>
    </m:Context>
    <m:MasterRef>IOA2600007</m:MasterRef>
    <m:EBankMasterRef>CH-IOA-000007</m:EBankMasterRef>
    <m:EBankEventRef>CH-IOA-000007-06</m:EBankEventRef>
    <m:InvoiceReference>INV-77120</m:InvoiceReference>
    <m:PurchaseOrderReference>PO-2026-0457</m:PurchaseOrderReference>
    <m:TransactionMatchingReference>TMA-99817</m:TransactionMatchingReference>
    <m:InvoiceIssueDate>2026-10-10</m:InvoiceIssueDate>
  </m:TFIOARPY>
</ServiceRequest>
```

## Shape: Export open account payment response

Canonical message: `TFEOACPR` – Export open account payment response. Creates: Export Open Account payment request response event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFEOACPR` | 100% | none |
| `TFEOACPY` | 100% | none |
| `TFEOARPP` | 89% (loose) | adds `AdditionalData` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `EBankMasterRef` | string ≤20 | TSDS-264 (PG). |
| `EBankEventRef` | string ≤20 | TSDS-264 (PG). |
| `EBankCorpRef` | string ≤20 | TSDS-264 (PG). |
| `InvoiceReference` | string 1–34 |  |
| `PurchaseOrderReference` | string 1–34 |  |
| `TransactionMatchingReference` | string 1–34 |  |
| `ResponseDetails` | group |  |
| `ExtraData` | group |  |

#### Nested structures of `TFEOACPR`

**`ResponseDetails`**

| Path | Type / length | Description |
|---|---|---|
| `ResponseDetails/ResponseReceivedDate` | date |  |
| `ResponseDetails/Discrepancies` | string ≤1480 |  |
| `ResponseDetails/ResponseAction` | string ≤3 |  |


### Sample (`TFEOACPR`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFEOACPR</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFEOACPR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>EOA</c:Product>
    </m:Context>
    <m:MasterRef>EOA2600011</m:MasterRef>
    <m:EBankMasterRef>CH-EOA-000011</m:EBankMasterRef>
    <m:EBankEventRef>CH-EOA-000011-02</m:EBankEventRef>
    <m:InvoiceReference>INV-88001</m:InvoiceReference>
    <m:PurchaseOrderReference>PO-9981</m:PurchaseOrderReference>
    <m:TransactionMatchingReference>TMA-10442</m:TransactionMatchingReference>
    <m:ResponseDetails>
      <c:ResponseReceivedDate>2026-10-18</c:ResponseReceivedDate>
    </m:ResponseDetails>
  </m:TFEOACPR>
</ServiceRequest>
```

## Shape: Finance create

Canonical message: `TFFINNEW` – Finance create. Creates: Finance Standalone create event with disburse to muultiple parties details.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFFECNEW` | 100% | none |
| `TFFINNEW` | 100% | none |
| `TFFSCNEW` | 94% | adds `SCFDetails`, `OtherBanksChargesAs`, `InterestDaysBasis`; omits `DisbursementDetailss` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `Amount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `AutoMonthlyDebit` | boolean | Boolean values. |
| `AutoRepay` | boolean | Boolean values. |
| `DebitParty` | party |  |
| `EffectiveDate` | date |  |
| `FinanceAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `FinancePercent` | decimal(17,2) | Percentage. Available Values: 0 to 100 |
| `FinanceCurrency` | string 1–3 | Currency Code. |
| `ExchangeRate` | string | Exchange rate. |
| `FinanceToParty` | party |  |
| `FinanceInterestDate` | date |  |
| `InterestRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999.999999- to 9999.9999999-. Available Val... |
| `InterestSettlementCcy` | string 1–3 | Currency Code. |
| `MarginRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999.999999- to 9999.9999999-. Available Val... |
| `MaturityDate` | date |  |
| `PenaltyPercent` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999.999999- to 9999.9999999-. Available Val... |
| `PreferredChargeCcy` | string 1–3 | Currency Code. |
| `StartDate` | date |  |
| `TrustReceiptLoan` | boolean | Boolean values. |
| `UseSchedule` | boolean | Boolean values. |
| `RelatedMasterRef` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `InterestType` | string 1–1 | Available values: 'D'='Interest in advance - discount', 'Y'='Interest in advance - discount to yield', 'I'='Interest in advance - interest to yield... |
| `InterestFromAfter` | string 1–1 | Available values: 'A'='After', 'F'='From', 'E'='From (exclusive)'. |
| `RateType` | string 1–1 | Available values: 'F'='Fixed', 'S'='Special', 'V'='Variable'. |
| `BaseCode` | string 2–5 |  |
| `ActualBaseOrGroupRate` | decimal(11,7) | Interest rate - an eleven digit rate with seven decimal places allows rates to be entered in the range 9999.999999- to 9999.9999999-. Available Val... |
| `DiffCode` | string 2–2 |  |
| `NarrativeDetails` | string ≤32000 |  |
| `InstructionsToFinanceParty` | string ≤32000 |  |
| `InstructionsToDebitParty` | string ≤32000 |  |
| `PenaltyRateCode` | string 2–2 |  |
| `GroupRate` | string 1–6 |  |
| `MessageToBank` | string ≤32000 |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `ProductCode` | string ≤4 |  |
| `RepaymentDetailss` | group |  |
| `RegularRepaymentDetails` | group | Regular repayment details. |
| `FinancePeriod` | group |  |
| `InterestPeriod` | group |  |
| `TheirRef` | string 1–34 |  |
| `ProductType` | string ≤3 | TI Product type. |
| `ChargeAccount` | string 1–34 | The account number from which charges should be taken. |
| `PrincipalAccount` | string 1–34 | Principal account. Creates an action item for the user to create master settlement instructions or advise to third parties as required. |
| `BankCodes` | group | Bank defined codes for their own purposes. Defined using 'List value' in TI Plus System tailoring. |
| `AdditionalParties` | group | This contains the additional parties to a transaction. Only applicable for Issue/Create and Amend events. |
| `Collateral` | group |  |
| `Provisional` | boolean | Boolean values. |
| `FinanceResponse` | string ≤1 | Available values: 'A'='Offer is accepted', 'R'='Offer is rejected', 'G'='Offer is renegotiated'. |
| `InstructionsFromFinanceToParty` | string ≤32000 |  |
| `AutoCreateFollowOnEvent` | boolean | Used to specify if the next event should be automatically created when the current event is completed. Boolean values. |
| `LicenseDetails` | group |  |
| `AdviseNoticeDays` | int | Notice days. |
| `FundingParty` | party |  |
| `InterestScheduleTiers` | group |  |
| `RepaymentNoticeDays` | int | Integer with no domain restrictions. |
| `RepaymentNoticeRequired` | boolean | Boolean values. |
| `WithRecourse` | boolean | Boolean values. |
| `DisbursementDetailss` | group |  |
| `MasterSettlementInstructionss` | group |  |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFFINNEW`

**`Amount`**

| Path | Type / length | Description |
|---|---|---|
| `Amount/Amount` | string ≤22 |  |
| `Amount/Currency` | string 1–3 | Currency. |

**`FinanceAmount`**

| Path | Type / length | Description |
|---|---|---|
| `FinanceAmount/Amount` | string ≤22 |  |
| `FinanceAmount/Currency` | string 1–3 | Currency. |

**`RepaymentDetailss`**

| Path | Type / length | Description |
|---|---|---|
| `RepaymentDetailss/RepaymentDetails` | group ×n | Irregular repayment details. |
| `RepaymentDetailss/RepaymentDetails/Amount` | long |  |
| `RepaymentDetailss/RepaymentDetails/Percent` | decimal(17,2) | The percent figure for the data type. Available Values: 0 to 100 |
| `RepaymentDetailss/RepaymentDetails/Date` | date |  |

**`RegularRepaymentDetails`**

| Path | Type / length | Description |
|---|---|---|
| `RegularRepaymentDetails/AmountOrPercent` | group |  |
| `RegularRepaymentDetails/AmountOrPercent/Amount` | long |  |
| `RegularRepaymentDetails/AmountOrPercent/Percent` | decimal(17,2) | Percentage. Available Values: 0 to 100 |
| `RegularRepaymentDetails/Date` | date |  |
| `RegularRepaymentDetails/Period` | group |  |
| `RegularRepaymentDetails/Period/Unit` | string 1–1 | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `RegularRepaymentDetails/Period/Number` | int | Number of days (months) etc e.g. 30. |
| `RegularRepaymentDetails/Period/DayInMonth` | int | Day in month for renewal. |

**`FinancePeriod`**

| Path | Type / length | Description |
|---|---|---|
| `FinancePeriod/TenorDays` | int | Tenor element of the term. |
| `FinancePeriod/TenorPeriod` | string | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |

**`InterestPeriod`**

| Path | Type / length | Description |
|---|---|---|
| `InterestPeriod/Unit` | string 1–1 | Period. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `InterestPeriod/Number` | int | Number of days (months) etc e.g. 30. |
| `InterestPeriod/DayInMonth` | int | Day in month for renewal. |

**`BankCodes`**

| Path | Type / length | Description |
|---|---|---|
| `BankCodes/BankCode1` | string ≤4 |  |
| `BankCodes/BankCode2` | string ≤4 |  |
| `BankCodes/BankCode3` | string ≤4 |  |
| `BankCodes/BankCode4` | string ≤4 |  |
| `BankCodes/BankCode5` | string ≤4 |  |

**`AdditionalParties`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalParties/AdditionalPartiesToAdd` | party ×n | Receives details of additional parties via the gateway. Mapped to and displayed under the Party list pane i... |

**`Collateral`**

| Path | Type / length | Description |
|---|---|---|
| `Collateral/CollateralDetail` | group ×n |  |
| `Collateral/CollateralDetail/Code` | string 1–8 |  |
| `Collateral/CollateralDetail/Description` | string ≤25 |  |
| `Collateral/CollateralDetail/Amount` | long | Amount. |
| `Collateral/CollateralDetail/Currency` | string 1–3 | Currency mnemonic. |
| `Collateral/CollateralDetail/OtherDetailsType` | string | Available values: 'G'='General', 'S'='Specific'. |
| `Collateral/CollateralDetail/CustomerDDA` | string ≤25 |  |
| `Collateral/CollateralDetail/OtherPledge` | string ≤50 |  |
| `Collateral/CollateralDetail/OwnPledge` | string ≤50 |  |
| `Collateral/CollateralDetail/Surety` | string ≤50 |  |

**`LicenseDetails`**

| Path | Type / length | Description |
|---|---|---|
| `LicenseDetails/LicenseDetail` | group ×n | Provides the link between a product (LC, Collection etc.) and a License. |
| `LicenseDetails/LicenseDetail/OurReference` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `LicenseDetails/LicenseDetail/EBankMasterRef` | string ≤20 |  |
| `LicenseDetails/LicenseDetail/AuthorityReference` | string ≤35 |  |
| `LicenseDetails/LicenseDetail/Number` | string ≤35 |  |
| `LicenseDetails/LicenseDetail/AllocatedAmount` | string ≤22 |  |
| `LicenseDetails/LicenseDetail/AllocatedCurrency` | string 1–3 |  |

**`InterestScheduleTiers`**

| Path | Type / length | Description |
|---|---|---|
| `InterestScheduleTiers/InterestScheduleTier` | group ×n | TIer Interest Schedule. |
| `InterestScheduleTiers/InterestScheduleTier/TierNumber` | int |  |
| `InterestScheduleTiers/InterestScheduleTier/Amount` | long |  |
| `InterestScheduleTiers/InterestScheduleTier/PeriodUnit` | string | Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `InterestScheduleTiers/InterestScheduleTier/Period` | int |  |
| `InterestScheduleTiers/InterestScheduleTier/BaseRateCode` | string 2–5 |  |
| `InterestScheduleTiers/InterestScheduleTier/ActualBaseOrGroupRate` | decimal(11,7) | Available Values: -10000 to 10000 |
| `InterestScheduleTiers/InterestScheduleTier/DifferentialRate` | string 2–2 |  |
| `InterestScheduleTiers/InterestScheduleTier/SpreadRate` | decimal(11,7) | Available Values: -10000 to 10000 |
| `InterestScheduleTiers/InterestScheduleTier/GroupRate` | string 1–6 |  |
| `InterestScheduleTiers/InterestScheduleTier/Rate` | decimal(11,7) | Available Values: -10000 to 10000 |

**`DisbursementDetailss`**

| Path | Type / length | Description |
|---|---|---|
| `DisbursementDetailss/DisbursementDetails` | party ×n |  |

**`MasterSettlementInstructionss`**

| Path | Type / length | Description |
|---|---|---|
| `MasterSettlementInstructionss/MasterSettlementInstructions` | group ×n |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/PayReceiveFlag` | string 1–1 | Available values: 'P'='Pay', 'R'='Receive'. |
| `MasterSettlementInstructionss/MasterSettlementInstructions/MovementType` | string 1–1 | Available values: 'C'='Charge', 'I'='Interest', 'P'='Principal'. |
| `MasterSettlementInstructionss/MasterSettlementInstructions/Currency` | string 1–3 | Currency mnemonic. |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SettlementCurrency` | string 1–3 | Currency mnemonic. |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SettlementPartyCustomer` | string 1–20 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SettlementPartyNameAddress` | string ≤1024 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SettlementPartySwiftAddress` | string ≤11 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SettlementPartyRole` | string 1–3 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankCustomer` | string 1–20 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankSwiftAddress` | string ≤14 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankNameAddress` | string ≤148 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressBuildingName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressBuildingNumber` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressCountry` | string 1–2 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressCountrySubDivision` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressDepartment` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressDistrictName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressFloor` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressName` | string ≤140 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressPostBox` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressPostCode` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressRoom` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressStreetName` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressSubDepartment` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressTownLocationName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressTownName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressAddressLine1` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankMXAddressAddressLine2` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/BeneficiaryBankAccount` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankCustomer` | string 1–20 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankSwiftAddress` | string ≤14 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankNameAddress` | string ≤148 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressBuildingName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressBuildingNumber` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressCountry` | string 1–2 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressCountrySubDivision` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressDepartment` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressDistrictName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressFloor` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressName` | string ≤140 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressPostBox` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressPostCode` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressRoom` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressStreetName` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressSubDepartment` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressTownLocationName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressTownName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressAddressLine1` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankMXAddressAddressLine2` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryBankAccount` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankCustomer` | string 1–20 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankSwiftAddress` | string ≤14 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankNameAddress` | string ≤148 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankMXAddressBuildingName` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankMXAddressBuildingNumber` | string ≤16 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankMXAddressCountry` | string 1–2 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankMXAddressCountrySubDivision` | string ≤35 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankMXAddressDepartment` | string ≤70 |  |
| `MasterSettlementInstructionss/MasterSettlementInstructions/IntermediaryReimbursingBankMXAddressDistrictName` | string ≤35 |  |
| … | | 82 more paths – see `TI.<message>.xsd.txt` for `MasterSettlementInstructionss` |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFFINNEW`

| Path | Allowed values |
|---|---|
| `InterestType` | `D` Interest in advance - discount; `Y` Interest in advance - discount to yield; `I` Interest in advance - interest to yield; `S` Interest in advance - standard; `E` Interest in arrears - standard |
| `InterestFromAfter` | `A` After; `F` From; `E` From (exclusive) |
| `RateType` | `F` Fixed; `S` Special; `V` Variable |
| `RegularRepaymentDetails/Period/Unit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `FinancePeriod/TenorPeriod` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `InterestPeriod/Unit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `Collateral/CollateralDetail/OtherDetailsType` | `G` General; `S` Specific |
| `FinanceResponse` | `A` Offer is accepted; `R` Offer is rejected; `G` Offer is renegotiated |
| `InterestScheduleTiers/InterestScheduleTier/PeriodUnit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `DisbursementDetailss/DisbursementDetails/PaySIPayFundsVia` | `T` Account transfer; `A` Acct at nostro/vostro; `B` Beneficiary bank; `C` Beneficiary bank with cover; `X` Clearing ID; `I` Intermediary bank; `P` Intermediary with cover; `O` Other account |
| `DisbursementDetailss/DisbursementDetails/PaySISettlesBy` | `A` Account; `X` Clearing ID; `N` Nostro; `O` Other account; `V` Vostro |
| `DisbursementDetailss/DisbursementDetails/PaySISwiftCharges` | `BEN` Beneficiary; `OUR` Our; `SHA` Shared |
| `MasterSettlementInstructionss/MasterSettlementInstructions/PayReceiveFlag` | `P` Pay; `R` Receive |
| `MasterSettlementInstructionss/MasterSettlementInstructions/MovementType` | `C` Charge; `I` Interest; `P` Principal |
| `MasterSettlementInstructionss/MasterSettlementInstructions/PayFundsVia` | `T` Account transfer; `A` Acct at nostro/vostro; `B` Beneficiary bank; `C` Beneficiary bank with cover; `X` Clearing ID; `I` Intermediary bank; `P` Intermediary with cover; `O` Other account |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SettlesBy` | `A` Account; `X` Clearing ID; `N` Nostro; `O` Other account; `V` Vostro |
| `MasterSettlementInstructionss/MasterSettlementInstructions/SwiftCharges` | `BEN` Beneficiary; `OUR` Our; `SHA` Shared |


### Sample (`TFFINNEW`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFFINNEW</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFFINNEW>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>FIN</c:Product>
    </m:Context>
    <m:AutoRepay>Y</m:AutoRepay>
    <m:DebitParty>
      <c:Customer>ACMEIMP</c:Customer>
    </m:DebitParty>
    <m:FinanceAmount>
      <c:Amount>800000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:FinanceAmount>
    <m:FinanceCurrency>USD</m:FinanceCurrency>
    <m:FinanceToParty>
      <c:Customer>ACMEIMP</c:Customer>
    </m:FinanceToParty>
    <m:MaturityDate>2027-01-18</m:MaturityDate>
    <m:StartDate>2026-10-20</m:StartDate>
    <m:RelatedMasterRef>ILC2600123</m:RelatedMasterRef>
    <m:MessageToBank>Finance against import LC ILC2600123.</m:MessageToBank>
    <m:eBankMasterRef>CH-FIN-000101</m:eBankMasterRef>
    <m:eBankEventRef>CH-FIN-000101-01</m:eBankEventRef>
    <m:ChargeAccount>1234567890</m:ChargeAccount>
    <m:PrincipalAccount>1234567890</m:PrincipalAccount>
  </m:TFFINNEW>
</ServiceRequest>
```

## Shape: Bulk payment

Canonical message: `TFIBPAMD` – Bulk payment. Creates: Invoice Bulk Payment amend event.

| Member | Match | Differences from the canonical body |
|---|---:|---|
| `TFIBPAMD` | 100% | none |
| `TFIBPCRE` | 94% | adds `EBankMasterRefx` |

### Body elements

| Element | Type / length | Description |
|---|---|---|
| `BulkPaymentBankDiscretion` | string 1–2 | Bank discretion. Bank discretion. Available values: 'ES'='Earliest settlement dates', 'FI'='First in / first out', 'HO'='Highest outstanding amount... |
| `ProductTypeCode` | string ≤3 | TI Product type. |
| `Seller` | string 1–20 | The customer who is the credit party on the invoice. |
| `Buyer` | string 1–20 | The customer who is the debit party on the invoice. |
| `SettlementAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `SettlementDate` | date |  |
| `InstructionsReceived` | string ≤32000 |  |
| `EBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `PreferredCurrency` | string 1–3 | Preferred currency for charges to be taken. (if not specified charges will be taken in transaction currency ). |
| `Invoices` | group |  |
| `Finances` | group |  |
| `FxContracts` | group |  |
| `BulkPaymentAction` | string 1–1 | Bulk payment action. Bulk payment action. Available values: 'A'='Settle later (Auto-Debit)', 'R'='Settle later (Requires Approval to Debit)'. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFIBPAMD`

**`SettlementAmount`**

| Path | Type / length | Description |
|---|---|---|
| `SettlementAmount/Amount` | string ≤22 |  |
| `SettlementAmount/Currency` | string 1–3 | Currency. |

**`Invoices`**

| Path | Type / length | Description |
|---|---|---|
| `Invoices/Invoice` | group ×n |  |
| `Invoices/Invoice/Programme` | string ≤35 |  |
| `Invoices/Invoice/Seller` | string 1–20 |  |
| `Invoices/Invoice/Buyer` | string 1–20 |  |
| `Invoices/Invoice/AnchorParty` | string 1–20 |  |
| `Invoices/Invoice/MasterReference` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `Invoices/Invoice/InvoiceNumber` | string 1–34 |  |
| `Invoices/Invoice/IssueDate` | date |  |
| `Invoices/Invoice/OutstandingAmount` | string ≤22 |  |
| `Invoices/Invoice/OutstandingAmountCurrency` | string 1–3 |  |
| `Invoices/Invoice/SettlementAmount` | string ≤22 |  |
| `Invoices/Invoice/SettlementAmountCurrency` | string 1–3 |  |
| `Invoices/Invoice/EBankEventRef` | string ≤20 |  |
| `Invoices/Invoice/EBankMasterXRef` | string ≤20 |  |
| `Invoices/Invoice/DiscountedInvoiceAmount` | string ≤22 |  |
| `Invoices/Invoice/DiscountedInvoiceAmountCurrency` | string 1–3 |  |
| `Invoices/Invoice/OutstandingFinanceRepaymentAmount` | string ≤22 |  |
| `Invoices/Invoice/OutstandingFinanceRepaymentAmountCurrency` | string 1–3 |  |

**`Finances`**

| Path | Type / length | Description |
|---|---|---|
| `Finances/Finance` | group ×n |  |
| `Finances/Finance/Programme` | string ≤35 |  |
| `Finances/Finance/Seller` | string 1–20 |  |
| `Finances/Finance/Buyer` | string 1–20 |  |
| `Finances/Finance/AnchorParty` | string 1–20 |  |
| `Finances/Finance/MasterReference` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `Finances/Finance/StartDate` | date |  |
| `Finances/Finance/OutstandingAmount` | string ≤22 |  |
| `Finances/Finance/OutstandingAmountCurrency` | string 1–3 |  |
| `Finances/Finance/SettlementAmount` | string ≤22 |  |
| `Finances/Finance/SettlementAmountCurrency` | string 1–3 |  |
| `Finances/Finance/EBankEventRef` | string ≤20 |  |
| `Finances/Finance/EBankMasterXRef` | string ≤20 |  |

**`FxContracts`**

| Path | Type / length | Description |
|---|---|---|
| `FxContracts/FxContract` | group ×n |  |
| `FxContracts/FxContract/Reference` | string 1–20 |  |
| `FxContracts/FxContract/Branch` | string 1–8 |  |
| `FxContracts/FxContract/DealType` | string 1–3 |  |
| `FxContracts/FxContract/PurchaseCcy` | string 1–3 |  |
| `FxContracts/FxContract/SaleCcy` | string 1–3 |  |
| `FxContracts/FxContract/ExchangeRate` | decimal(28,20) | Available Values: 0 to 99999999 |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFIBPAMD`

| Path | Allowed values |
|---|---|
| `BulkPaymentBankDiscretion` | `ES` Earliest settlement dates; `FI` First in / first out; `HO` Highest outstanding amount; `LI` Last in / first out; `LS` Latest settlement dates; `LO` Lowest outstanding amount; `DF` Our transaction reference |
| `BulkPaymentAction` | `A` Settle later (Auto-Debit); `R` Settle later (Requires Approval to Debit) |


### Sample (`TFIBPAMD`)

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFIBPAMD</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFIBPAMD>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEIMP</c:Customer>
      <c:Product>IBP</c:Product>
    </m:Context>
    <m:BulkPaymentBankDiscretion>ES</m:BulkPaymentBankDiscretion>
    <m:SettlementAmount>
      <c:Amount>420000.00</c:Amount>
      <c:Currency>USD</c:Currency>
    </m:SettlementAmount>
    <m:SettlementDate>2027-01-10</m:SettlementDate>
    <m:EBankEventRef>CH-IBP-000009-01</m:EBankEventRef>
    <m:PreferredCurrency>USD</m:PreferredCurrency>
  </m:TFIBPAMD>
</ServiceRequest>
```
