# Export Standby Letter of Credit (Beneficiary side)

> Part of the product specifications. Index and shared rules: [README](README.md). Shared message bodies: [shared message shapes](shared-message-shapes.md). Catalogue: [01](../01-interface-catalogue.md) · Functional spec: [02](../02-functional-specification.md).

| | |
|---|---|
| Priority | P2 |
| Customer role | Beneficiary |
| TI product code (`Context.Product`) | `ESB` **[Assumption – confirm in TI]** |
| Messages | 8 |

## 1. Purpose and scope

An **export standby** is a standby LC issued by a foreign bank in favour of our customer and advised by our bank. The customer's actions are to respond to amendments and cancellations, to **present a demand** (draw) when the applicant defaults, and to correspond. The `*NEW` and `*AMD` messages of the export products record an instrument **received** from the issuing bank. They are raised by the bank (or by a scanning/advising process), not by the beneficiary customer. They are documented as reference for the data the channel will read, and marked *Bank-side* or *Review*.

Out of scope: Advising work, and financing against the standby.

> **Reading guide.** Field names, lengths and enumerations come from `sources/xsd_element_docs/`. Statements marked **[Proposed]** or **[Assumption]** are design decisions or guesses that the interface documents cannot confirm. Names are copied exactly from the schema, including its inconsistencies in capitalisation and spelling.

## 2. Messages

| Message | Kind | Creates in TI | Channel exposure | Phase | Body |
|---|---|---|---|---|---|
| `TFESBDPR` | Claim / presentation | Export Standby Letter Of Credit documents presented event | Customer | 4 | [Shape: Documents presented](shared-message-shapes.md#shape-documents-presented) |
| `TFESBPYR` | Claim response | Export Standby Letter Of Credit outstanding presentation event | Review | 4 | [Shape: Claim response](shared-message-shapes.md#shape-claim-response) (+diff) |
| `TFESGPYR` | Claim response | Export Standby Letter Of Credit outstanding presentation event | Review | 4 | own |
| `TFESBBMR` | Response / request | Export Standby Letter Of Credit beneficiary response to amend event | Customer | 4 | [Shape: Beneficiary response](shared-message-shapes.md#shape-beneficiary-response) |
| `TFESBBCR` | Response / request | Export Standby Letter Of Credit beneficiary response to cancel event | Customer | 4 | own |
| `TFESBCOR` | Correspondence | Export Standby Letter Of Credit correspondence event | Customer | 4 | [Shape: Correspondence](shared-message-shapes.md#shape-correspondence) |
| `TFESBNEW` | Create / issue | Export Standby Letter Of Credit advise event | Bank-side | - | own |
| `TFESBAMD` | Amend | Export Standby Letter Of Credit amend event | Bank-side | - | own |

**Exposure key.** *Customer* – the corporate starts or answers it in the channel. *Review* – schema description does not make the sender clear; confirm with the TI team before exposing. *Bank-side* – raised by the bank or an interbank counterparty; do not expose to customers.

## 3. Business scenario

The applicant has defaulted. The beneficiary opens the standby in the channel, chooses *Make a demand*, enters the amount and date, attaches the demand letter and the statements required by the standby, and submits. The bank examines the demand, forwards it to the issuing bank, and credits the proceeds when paid.

**Demand**

```mermaid
sequenceDiagram
    autonumber
    actor Maker as Corporate maker
    actor Appr as Corporate approver
    participant CH as Channel
    participant GW as FTI Gateway
    participant TI as TI Plus
    actor TSO as Bank TSO
    Maker->>CH: Complete demand under the standby, attach documents
    CH->>CH: Validate (schema rules, entitlements)
    Maker->>CH: Submit for approval
    Appr->>CH: Approve
    CH->>GW: ServiceRequest TFESBDPR (CorrelationId = X)
    GW->>TI: Create Export Standby LC documents presented event
    GW-->>CH: ServiceResponse Status = SUCCEEDED
    CH->>CH: Status = Received by bank
    TI->>TSO: Event enters orchestration
    TSO->>TI: Review, enrich, authorise
    TI-->>CH: Notification or enquiry shows result
    CH->>Maker: Status updated
```

## 4. Preconditions

1. The standby is advised and matched to the beneficiary.
2. The standby has not expired and its available amount is known.

## 5. Screens

Short form on top of the standby summary.

| Step | Section | Fields (schema element) |
|---|---|---|
| 1 | **Standby summary** | Read-only master data |
| 2 | **Demand** | `PresentationDetails` (amount, date, reference), `PresentersAction`, `RequestType`, `NotesFromPresenter` |
| 3 | **Documents** | Attachments: demand letter, statements, copies |
| 4 | **Review and submit** |  |

> **[Proposed] Fields hidden from the customer or set by configuration.** `Context.Team`, `Context.Step`, `Context.ExchangeNonAutoComplete`, `Context.BehalfOfBranch*`, `ExternalReview`, `ProvisionalFlags`, `BankCodes`, `ContinueEvent`, `Provisional`, `AutoCreateFollowOnEvent`, `FinalWording`, `TakeonExtra`, `InternalMasterRefSequence`, `OutstandingAmount`, `OutstandingAdditionalAmount`, `OriginalMasterReference`, `PreallocatedNo`. They affect routing, bank processing or data take-on, and should be set by channel configuration or left to TI defaults. Confirm each with the TI team (02, Q10).

## 6. Primary message `TFESBDPR`

Primary instruction of this product. Creates: Export Standby Letter Of Credit documents presented event.

### 6.1 Context

| Element | Value |
|---|---|
| `Branch` | The booking branch from the corporate configuration. |
| `Customer` | The customer's TI mnemonic (max 20). |
| `Product` | `ESB` **[Assumption]** |
| `Event` | Normally implied by the message name. Leave empty unless the TI team asks for it. **[Assumption]** |
| `TheirReference` | Customer's own reference (max 34). |
| `OurReference` / `MasterRef` | **Required.** `MasterRef` is the export standby reference. |

The remaining `Context` elements (`Team`, `Step`, `BehalfOfBranch*`, `TemplateIdentifier`, `EventReference`, `ExchangeNonAutoComplete`, `SubsidiaryMasterPrefixAndSerial`) are set by configuration. See [README](README.md#common-blocks-in-every-message).

### 6.2 Channel-managed elements

| Element | Value |
|---|---|
| `eBankMasterRef` / `EBankMasterRef` (max 20) | The channel's id for the master. Constant for every later event on it. |
| `eBankEvent` / `eBankEventRef` / `EBankEventRef` (max 20) | The channel's id for this event. Unique per instruction; the key for idempotent retries. |
| `EmbeddedItemss` | Inline attachments (`ID`, `AttachType`, `DocType`, `Description`, `FileName`, `DataStream`, `MimeType`, `DocumentFileName`); `IsCustomerAttachedDocument` = `Y`. |
| `DocumentsReceiveds` | References to documents already in the DMS, if the upload-first approach is chosen (02, Q8). |

**Capitalisation differs between messages** (`eBankMasterRef` in some, `EBankMasterRef` in others; `eBankEvent`, `eBankEventRef` and `EBankEventRef`). The channel generates names from the schema catalogue and must never hand-type them.

### 6.3 Field reference

All top-level elements, in schema order, then the nested structures and enumerations.

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


## 7. Other messages

### `TFESBPYR` – Respond to an outstanding presentation

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit outstanding presentation event |
| Purpose (schema) | - |
| Channel exposure | Review |
| Phase | 4 |

**Body:** [Shape: Claim response](shared-message-shapes.md#shape-claim-response) with differences.

* Adds: `ResponseNarrative`
* Omits: `RequestedNewExpiryDateLocal`

| Element | Type / length | Description |
|---|---|---|
| `ResponseNarrative` | string ≤1820 |  |

### `TFESGPYR` – Respond to an outstanding presentation (guarantee-type)

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit outstanding presentation event |
| Purpose (schema) | - |
| Channel exposure | Review |
| Phase | 4 |

**Body:** same elements as `TFESBPYR` (section above), with the differences below.

* Omits: `RequestedNewExpiryDate`
* Adds:

| Element | Type / length | Description |
|---|---|---|
| `AdditionalParties` | group | This contains the additional parties to a transaction. Only applicable for Issue/Create and Amend events. |
| `OriginalClaimDate` | date |  |
| `OriginalClaimAmount` | group | Amount Gateway pane. Note that this pane may have the currency included as part of the amount source or the currency may be entered separately. |
| `ResponseReceivedDate` | date |  |
| `PaymentDetailss` | group |  |

**`AdditionalParties`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalParties/AdditionalPartiesToAdd` | party ×n | Receives details of additional parties via the gateway. Mapped to and displayed under the Party list pane i... |


**`OriginalClaimAmount`**

| Path | Type / length | Description |
|---|---|---|
| `OriginalClaimAmount/Amount` | string ≤22 |  |
| `OriginalClaimAmount/Currency` | string 1–3 | Currency. |


**`PaymentDetailss`**

| Path | Type / length | Description |
|---|---|---|
| `PaymentDetailss/PaymentDetails` | group ×n |  |
| `PaymentDetailss/PaymentDetails/DraftIdentifier` | string ≤111 |  |
| `PaymentDetailss/PaymentDetails/FromAfter` | string 1–1 | Available values: 'A'='After', 'F'='From', 'E'='From (exclusive)'. |
| `PaymentDetailss/PaymentDetails/MaturityDate` | date |  |
| `PaymentDetailss/PaymentDetails/PeriodUnit` | string 1–1 | M (months) D (days) etc. Available values: 'D'='Days', 'M'='Months', 'Q'='Quarters', 'W'='Weeks', 'Y'='Years'. |
| `PaymentDetailss/PaymentDetails/PeriodNumber` | int |  |
| `PaymentDetailss/PaymentDetails/StartDate` | date |  |
| `PaymentDetailss/PaymentDetails/Type` | string 1–1 | Available values: 'A'='Acceptance', 'D'='Deferred', 'E'='Demand', 'S'='Sight'. |
| `PaymentDetailss/PaymentDetails/ValueDate` | date |  |
| `PaymentDetailss/PaymentDetails/Amount` | string ≤22 |  |
| `PaymentDetailss/PaymentDetails/AmountCurrency` | string 1–3 |  |


| Path | Allowed values |
|---|---|
| `PaymentDetailss/PaymentDetails/FromAfter` | `A` After; `F` From; `E` From (exclusive) |
| `PaymentDetailss/PaymentDetails/PeriodUnit` | `D` Days; `M` Months; `Q` Quarters; `W` Weeks; `Y` Years |
| `PaymentDetailss/PaymentDetails/Type` | `A` Acceptance; `D` Deferred; `E` Demand; `S` Sight |

### `TFESBBMR` – Respond to an amendment

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit beneficiary response to amend event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 4 |

**Body:** [Shape: Beneficiary response](shared-message-shapes.md#shape-beneficiary-response).

### `TFESBBCR` – Respond to a cancellation

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit beneficiary response to cancel event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 4 |

**Body elements** (besides the [common blocks](README.md#common-blocks-in-every-message)):

| Element | Type / length | Description |
|---|---|---|
| `CancelRef` | string |  |
| `Sender` | party |  |
| `ResponseType` | string 1–1 | Available values: 'A'='Approved', 'R'='Refused'. |
| `ReceivedDate` | date |  |
| `ResponseDetails` | string ≤32000 |  |
| `ResponseToBeneficiary` | string ≤32000 |  |
| `ResponseToIssuingBank` | string ≤32000 |  |
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFESBBCR`

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFESBBCR`

| Path | Allowed values |
|---|---|
| `ResponseType` | `A` Approved; `R` Refused |


### `TFESBCOR` – Correspondence

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit correspondence event |
| Purpose (schema) | A general purpose message that is sent by the Beneficiary to the Advising Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| Channel exposure | Customer |
| Phase | 4 |

**Body:** [Shape: Correspondence](shared-message-shapes.md#shape-correspondence).

### `TFESBNEW` – Standby advised (received)

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit advise event |
| Purpose (schema) | - |
| Channel exposure | Bank-side |
| Phase | - |

Not exposed to customers, so the body is not specified here. Top-level elements, in schema order: `AddAmount`, `AddAmountText`, `AdditionalConditions`, `SpecialPaymentConditions`, `AdviseThru`, `AdviseThruAccount`, `NextAdvisingBank`, `ApplcableRules`, `ApplcableRulesNarrative`, `Applicant`, `ApplicantInstructions`, `ApplicantBank`, `AdviseDate`, `AvailableWith`, `BankCodes`, `BenAccNo`, `Beneficiary`, `ConfirmationDetails`, `ChargeAccount`, `ChargeDetails`, `ChargeText`, `OtherBankConfirmation`, `Confirmation`, `Documents`, `DomesticExpiry`, `ExpiryDate`, `ExpiryPlace`, `FreightPrepaid`, `Goods`, `GoodsCode`, `IncoTerms`, `IncoPlace`, `InstructionsToNextAdvisingBank`, `InstructionsFromInterBank`, `InsuranceForBuyer`, `InternalMasterRefSequence`, `InvoicePct`, `IssuanceChgsFor`, `AdviseBy`, `IssueDate`, `LCAmount`, `LCAmountSpec`, `Operative`, `OriginalMasterReference`, `OutstandingAdditionalAmount`, `OutstandingAmount`, `OverseasChgsFor`, `PartShipment`, `PortOfDischarge`, `PortOfLoading`, `PreallocatedNo`, `PreferredCcy`, `PresentationDays`, `PresentationPeriod`, `ReimbursementApplicableRules`, `Revocable`, `Revolving`, `ShipmentDate`, `ShipmentFrom`, `ShipmentPeriod`, `ShipmentTo`, `TaxFor`, `TermsOfPayment`, `TransShipment`, `Transferable`, `ReceivedFromBank`, `InstructionsReceived`, `IssuingBank`, `NonBankIssuer`, `ReimbursingBank`, `DeliveryNumber`, `DocumentsToBeSentBy`, `EBankEventRef`, `EBankMasterRef`, `FXOptionDeal`, `ProductType`, `DeferChgs`, `AdvisingBank`, `MarginDeposit`, `Collateral`, `FxContracts`, `LicenseDetails`, `HSCode`, `InstructionsToPayingBank`, `Provisional`, `AutoCreateFollowOnEvent`, `AdditionalParties`, `Financial`, `NotPayableBeforeDate`, `PartialDrawingsAllowed`, `MultipleDrawingsAllowed`, `Trade`, `AdviseDirect`, `PrincipalBank`, `InstructionApplicableRules`, `ConfirmingBank`, `UseFreeFormat`, `Renewal`, `RenewalDetails`, `TransferableConditions`, `ExpiryDetails`, `OtherProductType`, `UndertakingDeliveryDetails`, `GoverningLawCountry`, `LetterOfIndemnity`, `BillNumber`, `FlightDetails`, `FreeFormatInstructionsForSWIFT`, `ShipmentAdditionalInformation`, `ShippingCompany`, `VesselName`, `GWRGuaranteeDetails`, `ContractDetails`, `AdditionalInformation`, `FinalWording`, `AdditionalData`, `ExtraData`. Details: `sources/xsd_element_docs/TI.TFESBNEW.xsd.txt`.

### `TFESBAMD` – Amendment received

| | |
|---|---|
| Creates in TI | Export Standby Letter Of Credit amend event |
| Purpose (schema) | - |
| Channel exposure | Bank-side |
| Phase | - |

Not exposed to customers, so the body is not specified here. Top-level elements, in schema order: `AddAmount`, `AddAmountText`, `AdditionalConditions`, `SpecialPaymentConditions`, `AdviseThru`, `AdviseThruAccount`, `NextAdvisingBank`, `ApplcableRules`, `ApplcableRulesNarrative`, `Applicant`, `ApplicantInstructions`, `ApplicantBank`, `AdviseDate`, `AvailableWith`, `BankCodes`, `BenAccNo`, `Beneficiary`, `ConfirmationDetails`, `ChargeAccount`, `ChargeDetails`, `ChargeText`, `OtherBankConfirmation`, `Confirmation`, `Documents`, `DomesticExpiry`, `ExpiryDate`, `ExpiryPlace`, `FreightPrepaid`, `Goods`, `GoodsCode`, `IncoTerms`, `IncoPlace`, `InstructionsToNextAdvisingBank`, `InstructionsFromInterBank`, `InsuranceForBuyer`, `InternalMasterRefSequence`, `InvoicePct`, `IssuanceChgsFor`, `AdviseBy`, `IssueDate`, `LCAmount`, `LCAmountSpec`, `Operative`, `OriginalMasterReference`, `OutstandingAdditionalAmount`, `OutstandingAmount`, `OverseasChgsFor`, `PartShipment`, `PortOfDischarge`, `PortOfLoading`, `PreallocatedNo`, `PreferredCcy`, `PresentationDays`, `PresentationPeriod`, `ReimbursementApplicableRules`, `Revocable`, `Revolving`, `ShipmentDate`, `ShipmentFrom`, `ShipmentPeriod`, `ShipmentTo`, `TaxFor`, `TermsOfPayment`, `TransShipment`, `Transferable`, `ReceivedFromBank`, `InstructionsReceived`, `IssuingBank`, `NonBankIssuer`, `ReimbursingBank`, `DeliveryNumber`, `DocumentsToBeSentBy`, `EBankEventRef`, `EBankMasterRef`, `FXOptionDeal`, `ProductType`, `DeferChgs`, `AdvisingBank`, `MarginDeposit`, `Collateral`, `FxContracts`, `LicenseDetails`, `HSCode`, `InstructionsToPayingBank`, `Sender`, `BenApprovalReq`, `IncreaseAmount`, `DecreaseAmount`, `AmendmentDetails`, `AmendmentNarrative`, `AmendChanges`, `AdditionalParties`, `AmendDate`, `AdviseDirect`, `Renewal`, `RenewalDetails`, `TransferableConditions`, `ExpiryDetails`, `Financial`, `Trade`, `NotPayableBeforeDate`, `PartialDrawingsAllowed`, `MultipleDrawingsAllowed`, `NonExtension`, `Reduction`, `StandbyGuaranteeApplicableRules`, `UseFreeFormat`, `UndertakingDeliveryDetails`, `GoverningLawDetails`, `AdditionalData`, `ExtraData`. Details: `sources/xsd_element_docs/TI.TFESBAMD.xsd.txt`.

## 8. Validation rules

### 8.1 Rules supported directly by the schema

Lengths, data types and the allowed values of coded fields are in the tables in sections 6 and 7 and are the source for the channel's client and server validators. Dates are ISO `xsd:date`. Boolean elements carry `Y` or `N`. Amounts are strings (maximum 22 characters); agree the decimal format with the TI team. The body is the shared [Documents presented](shared-message-shapes.md#shape-documents-presented) shape.

### 8.2 Business rules to apply in the channel

Normal practice for this product and the checks a TSO would otherwise perform by hand. **[Proposed]**: switch each on or off per bank policy.

| # | Rule |
|---|---|
| V1 | Demand date is within validity; amount ≤ available amount. |
| V2 | Partial and multiple demands only if the standby allows them (`PartialDrawingsAllowed`, `MultipleDrawingsAllowed`). |
| V3 | The documents required for a demand are listed from the master and ticked off before submission. **[Proposed]** |

### 8.3 Minimum content for a usable instruction

**[Proposed]** `MasterRef`, `Sender`, `PresentationDetails` (amount and date), demand attachment.

## 9. Illustrative requests

Values are invented. Element names, prefixes and order follow `sources/example_xml/`. Elements not shown are omitted, which the schema allows for optional elements. **Test against the gateway before use.**

**`TFESBDPR`**

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
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <TargetSystem>TI</TargetSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18T09:30:00Z</CreationDate>
  </RequestHeader>
  <m:TFESBDPR>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEEXP</c:Customer>
      <c:Product>ESB</c:Product>
      <c:TheirReference>DEMAND-2026-02</c:TheirReference>
    </m:Context>
    <m:MasterRef>ESB2600031</m:MasterRef>
    <m:Sender>
      <c:Customer>ACMEEXP</c:Customer>
    </m:Sender>
    <m:PresentersAction>Sam</m:PresentersAction>
    <m:NotesFromPresenter>Demand for USD 120,000 under standby ESB2600031.</m:NotesFromPresenter>
    <m:eBankMasterRef>CH-ESB-000031</m:eBankMasterRef>
    <m:eBankEventRef>CH-ESB-000031-04</m:eBankEventRef>
    <m:PresentationDetails>
      <c:PresentationAmount>
        <c:Amount>120000.00</c:Amount>
        <c:Currency>USD</c:Currency>
      </c:PresentationAmount>
      <c:PresentationDate>2026-10-15</c:PresentationDate>
    </m:PresentationDetails>
  </m:TFESBDPR>
</ServiceRequest>
```

## 10. Response handling and tracking

| `Status` | Channel behaviour |
|---|---|
| `SUCCEEDED` | TI accepted the message and created the event. Mark `ReceivedByBank`; keep `Warning` / `Info` texts. |
| `FAILED` | TI rejected the message. Show `Details.Error` to the maker and return the draft. No automatic retry. |
| `UNAVAILABLE` / timeout | Keep in the outbox, retry with back-off, and confirm by enquiry (`eBankEventRef`) before each re-send so no second event is created. |

Rules shared by every product are in [README – Common behaviour](README.md#common-behaviour).

*Demand submitted* → *Examined by bank* → *Forwarded to issuing bank* → *Paid* or *Refused*.

## 11. Exceptions

| Scenario | Handling |
|---|---|
| Demand after expiry | Block with the expiry date shown. |
| Issuing bank refuses | Bank alert with reason; the customer may correspond or re-present within validity. |
| Gateway `FAILED`, `UNAVAILABLE`, timeout, duplicate click | As in [README – Common behaviour](README.md#common-behaviour). |

## 12. Acceptance criteria

1. A demand produces one `TFESBDPR` with attachments.
2. Amendment and cancellation responses reference the original event.
3. Bank-side messages are never offered.
4. Every step is in the audit trail with user, time and `CorrelationId`.
5. The stored request validates against the schema rules in 8.1 and matches the structure in section 9.

## 13. Open items

| # | Item |
|---|---|
| ESB-1 | Confirm whether customers present demands through the channel or only through a branch. |
| ESB-2 | Confirm `Product` code and the difference between `TFESBPYR` and `TFESGPYR`. |
