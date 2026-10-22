# Export Open Account (Seller side, TMA)

> Part of the product specifications. Index and shared rules: [README](README.md). Shared message bodies: [shared message shapes](shared-message-shapes.md). Catalogue: [01](../01-interface-catalogue.md) · Functional spec: [02](../02-functional-specification.md).

| | |
|---|---|
| Priority | P3 |
| Customer role | Seller |
| TI product code (`Context.Product`) | `EOA` **[Assumption – confirm in TI]** |
| Messages | 18 |

## 1. Purpose and scope

The seller-side counterpart of [Import Open Account](import-open-account.md): the seller ships on open account and the bank matches purchase orders, invoices and shipment data, optionally holding a Bank Payment Obligation (BPO) from the buyer's bank. The seller's messages cover the baseline, amendment and cancellation requests, responses to the buyer's requests, and requesting or receiving payment.

Out of scope: Buyer-side messages (see Import Open Account); financing of the receivable.

> **Reading guide.** Field names, lengths and enumerations come from `sources/xsd_element_docs/`. Statements marked **[Proposed]** or **[Assumption]** are design decisions or guesses that the interface documents cannot confirm. Names are copied exactly from the schema, including its inconsistencies in capitalisation and spelling.

## 2. Messages

| Message | Kind | Creates in TI | Channel exposure | Phase | Body |
|---|---|---|---|---|---|
| `TFEOACRE` | Create / issue | Export Open Account create event | Review | 5 | own |
| `TFEOAAME` | Amend | Export Open Account amendment event | Customer | 5 | [Shape: Open account amendment](shared-message-shapes.md#shape-open-account-amendment) |
| `TFEOASRE` | Response / request | Export Open Account seller amendment request event | Customer | 5 | [Shape: Open account amendment](shared-message-shapes.md#shape-open-account-amendment) (+diff) |
| `TFEOASRC` | Response / request | Export Open Account seller cancellation request event | Customer | 5 | own |
| `TFEOACAR` | Cancel | Export Open Account cancel event | Customer | 5 | own |
| `TFEOABRE` | Response / request | Export Open Account buyer amendment request event | Review | 5 | [Shape: Open account amendment](shared-message-shapes.md#shape-open-account-amendment) |
| `TFEOABRC` | Response / request | Export Open Account buyer cancellation request event | Review | 5 | own |
| `TFEOARBE` | Response / request | Export Open Account seller response to buyer amendment request event | Customer | 5 | [Shape: Open account response](shared-message-shapes.md#shape-open-account-response) |
| `TFEOARBC` | Response / request | Export Open Account seller response to buyer cancellation request event | Customer | 5 | [Shape: Open account response](shared-message-shapes.md#shape-open-account-response) |
| `TFEOATBE` | Response / request | Export Open Account TMA response to buyer amendment request event | Bank-side | - | [Shape: Open account response](shared-message-shapes.md#shape-open-account-response) |
| `TFEOATBC` | Response / request | Export Open Account TMA response to buyer cancellation request event | Bank-side | - | [Shape: Open account response](shared-message-shapes.md#shape-open-account-response) |
| `TFEOATSE` | Response / request | Export Open Account TMA response to seller amendment request event | Bank-side | - | [Shape: Open account response](shared-message-shapes.md#shape-open-account-response) |
| `TFEOATSC` | Response / request | Export Open Account TMA response to seller cancellation request event | Bank-side | - | [Shape: Open account response](shared-message-shapes.md#shape-open-account-response) |
| `TFEOARPY` | Payment / acceptance | Export Open Account request payment event | Customer | 5 | [Shape: Open account payment presentation](shared-message-shapes.md#shape-open-account-payment-presentation) |
| `TFEOAPAY` | Payment / acceptance | Export Open Account payment event | Review | 5 | [Shape: Open account payment presentation](shared-message-shapes.md#shape-open-account-payment-presentation) |
| `TFEOACPR` | Payment / acceptance | Export Open Account payment request response event | Customer | 5 | [Shape: Export open account payment response](shared-message-shapes.md#shape-export-open-account-payment-response) |
| `TFEOACPY` | Payment / acceptance | Export Open Account continue payment event | Customer | 5 | [Shape: Export open account payment response](shared-message-shapes.md#shape-export-open-account-payment-response) |
| `TFEOARPP` | Payment / acceptance | Export Open Account payment by response party event | Review | 5 | [Shape: Export open account payment response](shared-message-shapes.md#shape-export-open-account-payment-response) (+diff) |

**Exposure key.** *Customer* – the corporate starts or answers it in the channel. *Review* – schema description does not make the sender clear; confirm with the TI team before exposing. *Bank-side* – raised by the bank or an interbank counterparty; do not expose to customers.

## 3. Business scenario

A seller agrees a baseline with a buyer (purchase order, amount, shipment, payment terms, and optional BPO). After shipping, the seller sends the invoice and shipment data and **requests payment** (`TFEOARPY`). TI matches the data. If the buyer's bank shows a mismatch the seller sees it and responds (`TFEOACPR`); on a match the payment obligation is settled. Either side may request amendment or cancellation of the baseline, which the other side accepts or rejects.

**Request payment**

```mermaid
sequenceDiagram
    autonumber
    actor Maker as Corporate maker
    actor Appr as Corporate approver
    participant CH as Channel
    participant GW as FTI Gateway
    participant TI as TI Plus
    actor TSO as Bank TSO
    Maker->>CH: Complete payment request, attach documents
    CH->>CH: Validate (schema rules, entitlements)
    Maker->>CH: Submit for approval
    Appr->>CH: Approve
    CH->>GW: ServiceRequest TFEOARPY (CorrelationId = X)
    GW->>TI: Create Export Open Account request payment event
    GW-->>CH: ServiceResponse Status = SUCCEEDED
    CH->>CH: Status = Received by bank
    TI->>TSO: Event enters orchestration
    TSO->>TI: Review, enrich, authorise
    TI-->>CH: Notification or enquiry shows result
    CH->>Maker: Status updated
```

## 4. Preconditions

1. Open account is enabled for the customer; a baseline exists and is matched.
2. TMA connectivity is in place if the bank offers matching.

## 5. Screens

A timeline on the master with the next action.

| Step | Section | Fields (schema element) |
|---|---|---|
| 1 | **Baseline** | `Buyer`, `Seller`, `OpenAccountAmount`, `ShipmentFrom/To`, `Goods`, `PartShipment`, `ObligationDetails`, `ObligationPaymentTerms`, `BuyersBank` |
| 2 | **Requests** | Amend or cancel (`TFEOAAME`, `TFEOASRE`, `TFEOASRC`); respond to the buyer (`TFEOARBE`, `TFEOARBC`) |
| 3 | **Payment** | Request payment (`TFEOARPY`) with `InvoiceReference`, `PresentationDetails`, `InvoiceIssueDate`, `PaymentTerms`, `SettlementTerms`; respond (`TFEOACPR`) |
| 4 | **Attachments and submit** | Invoice, transport documents |

> **[Proposed] Fields hidden from the customer or set by configuration.** `Context.Team`, `Context.Step`, `Context.ExchangeNonAutoComplete`, `Context.BehalfOfBranch*`, `ExternalReview`, `ProvisionalFlags`, `BankCodes`, `ContinueEvent`, `Provisional`, `AutoCreateFollowOnEvent`, `FinalWording`, `TakeonExtra`, `InternalMasterRefSequence`, `OutstandingAmount`, `OutstandingAdditionalAmount`, `OriginalMasterReference`, `PreallocatedNo`. They affect routing, bank processing or data take-on, and should be set by channel configuration or left to TI defaults. Confirm each with the TI team (02, Q10). Also `MatchStatus`, `BankAction`, `BPOIncluded`, `TMAUsed`, `ConfirmObligations`.

## 6. Primary message `TFEOACRE`

Primary instruction of this product. Creates: Export Open Account create event.

### 6.1 Context

| Element | Value |
|---|---|
| `Branch` | The booking branch from the corporate configuration. |
| `Customer` | The customer's TI mnemonic (max 20). |
| `Product` | `EOA` **[Assumption]** |
| `Event` | Normally implied by the message name. Leave empty unless the TI team asks for it. **[Assumption]** |
| `TheirReference` | Customer's own reference (max 34). |
| `OurReference` / `MasterRef` | Empty for a new baseline; **required** for later messages. |

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
| `eBankMasterRef` | string ≤20 | Unique reference meaningful to the sending system which applies to the master. |
| `eBankEventRef` | string ≤20 | Unique reference meaningful to the sending system which applies to this transaction. |
| `PurchaseOrderReference` | string 1–34 | Purchase order reference. |
| `TMAUsed` | boolean | TMA used. Boolean values. |
| `TransactionMatchingReference` | string ≤35 | Transaction matching reference. |
| `BPOIncluded` | boolean | Bank payment obligations included. Boolean values. |
| `ReceivedOnDate` | date |  |
| `IssueDate` | date | Issue date. |
| `ExpiryDate` | date | Expiry date. |
| `ExpiryPlace` | string 1–29 | Expiry place. |
| `Buyer` | party |  |
| `Seller` | party |  |
| `SubmittingBank` | party |  |
| `OpenAccountAmount` | group | Open account transaction amount. |
| `LCAmountSpec` | group | Open account transaction amount additional spec. |
| `InstructionsFromBuyer` | string ≤32000 | Instructions from buyer. |
| `InformationForBuyer` | string ≤32000 |  |
| `PaymentInstructions` | string ≤150 |  |
| `ShipmentFrom` | string ≤144 | Shipment from. |
| `ShipmentTo` | string ≤144 | Shipment to. |
| `PortOfLoading` | string ≤144 | Port of loading. |
| `PortOfDischarge` | string ≤144 | Port of discharge. |
| `PartShipment` | string 1–35 | Partial shipment text. |
| `GoodsCode` | string 1–10 | TI Plus goods code. |
| `Goods` | string ≤26800 | Description of Goods. |
| `FXOptionDeal` | string 1–20 | Details of any forward foreign exchange contract that this master may utilise. |
| `BankCodes` | group | Bank defined codes for their own purposes. Defined using 'List value' in TI Plus System tailoring. |
| `ObligationDetails` | group |  |
| `ObligationPaymentTerms` | group |  |
| `OurCharges` | string 1–1 | Available values: 'A'='Applicant', 'B'='Beneficiary'. |
| `OverseaChargesFor` | string 1–1 | Available values: 'A'='Applicant', 'B'='Beneficiary'. |
| `DeferChgs` | boolean | Defer bank's charges. Boolean values. |
| `PreferredCcy` | string 1–3 | Preferred charge currency. |
| `TaxFor` | string 1–1 | Tax paid by. Tax paid by. Available values: 'P'='Charge payer', 'C'='Customer'. |
| `BillingLevel` | string 1–1 | Available values: 'C'='Customer', 'P'='Product', 'T'='Transaction', 'X'='User-defined'. |
| `PrincipalAccount` | string 1–34 | Principal account. Creates an action item for the user to create master settlement instructions or advise to third parties as required. |
| `ChargeAccount` | string 1–34 | Account from which charges are to be taken. |
| `ChargeText` | string ≤222 | Free format charge text. |
| `MarginDeposit` | group |  |
| `ProductType` | string 1–3 | TI Product type. |
| `PreallocatedNo` | int | Used only if your bank has pre-allocated a block of reference numbers to the customer for the product as described in the TI Plus System Tailoring... |
| `Collaterals` | group |  |
| `FxContracts` | group |  |
| `OriginalMasterReference` | string 1–20 | e.g. Documentary Credit Number, FX contract number. |
| `InternalMasterRefSequence` | int | Internal master sequence. |
| `Provisional` | boolean | Used to specify if the message is to create a provisional event. Boolean values. |
| `PurchaseOrderIssueDate` | date | Purchase order issue date. |
| `MatchStatus` | string 1–1 | Available values: 'F'='Full', 'P'='Partial', 'U'='Status not yet available'. |
| `BankAction` | string 1–1 | The bank action the banking user makes when creating a new open account transaction. The bank action the banking user makes when creating a new ope... |
| `BuyersBank` | party |  |
| `ConfirmObligations` | boolean | Boolean values. |
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |
| `ExtraData` | group |  |

#### Nested structures of `TFEOACRE`

**`OpenAccountAmount`**

| Path | Type / length | Description |
|---|---|---|
| `OpenAccountAmount/Amount` | string ≤22 |  |
| `OpenAccountAmount/Currency` | string 1–3 | Currency. |

**`LCAmountSpec`**

| Path | Type / length | Description |
|---|---|---|
| `LCAmountSpec/Qualifier` | string 1–1 | Tolerance. Available values: 'A'='About', 'E'='Exact', 'M'='Maximum', 'X'='Min/Max', 'N'='Not Exceeding', '... |
| `LCAmountSpec/Min` | string 1–2 | Min percentage tolerance. |
| `LCAmountSpec/Max` | string 1–2 | Max percentage tolerance. |
| `LCAmountSpec/AmountMin` | long | Min amount tolerance. |
| `LCAmountSpec/AmountMax` | long | Min amount tolerance. |

**`BankCodes`**

| Path | Type / length | Description |
|---|---|---|
| `BankCodes/BankCode1` | string ≤4 |  |
| `BankCodes/BankCode2` | string ≤4 |  |
| `BankCodes/BankCode3` | string ≤4 |  |
| `BankCodes/BankCode4` | string ≤4 |  |
| `BankCodes/BankCode5` | string ≤4 |  |

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

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


#### Enumerated values in `TFEOACRE`

| Path | Allowed values |
|---|---|
| `LCAmountSpec/Qualifier` | `A` About; `E` Exact; `M` Maximum; `X` Min/Max; `N` Not Exceeding; `O` Other; `U` Up to |
| `ObligationDetails/ObligationDetail/ObligationType` | `B` Other bank obligor; `O` Ourselves / buyer |
| `ObligationPaymentTerms/ObligationPaymentTerm/ObligationType` | `B` Other bank obligor; `O` Ourselves / buyer |
| `OurCharges` | `A` Applicant; `B` Beneficiary |
| `OverseaChargesFor` | `A` Applicant; `B` Beneficiary |
| `TaxFor` | `P` Charge payer; `C` Customer |
| `BillingLevel` | `C` Customer; `P` Product; `T` Transaction; `X` User-defined |
| `Collaterals/Collateral/OtherDetailsType` | `G` General; `S` Specific |
| `MatchStatus` | `F` Full; `P` Partial; `U` Status not yet available |
| `BankAction` | `F` Finalise this transaction in the bank; `J` Proceed to baseline re-submission; `C` Proceed to change status to close; `I` Proceed to initial baseline submission; `R` Request customer to reconsider; `S` Request customer to review and revise |


## 7. Other messages

### `TFEOAAME` – Seller amends baseline

| | |
|---|---|
| Creates in TI | Export Open Account amendment event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Open account amendment](shared-message-shapes.md#shape-open-account-amendment) with differences.

* Adds: `ExportOpenAccountParties`
* Omits: `ImportOpenAccountParties`

| Element | Type / length | Description |
|---|---|---|
| `ExportOpenAccountParties` | group |  |

**`ExportOpenAccountParties`**

| Path | Type / length | Description |
|---|---|---|
| `ExportOpenAccountParties/Buyer` | party | Buyer. |
| `ExportOpenAccountParties/Seller` | party | Seller. |
| `ExportOpenAccountParties/SubmittingBank` | party | Submitting bank. |
| `ExportOpenAccountParties/BuyerBank` | party | Buyer's bank. |


### `TFEOASRE` – Seller amendment request

| | |
|---|---|
| Creates in TI | Export Open Account seller amendment request event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Open account amendment](shared-message-shapes.md#shape-open-account-amendment) with differences.

* Adds: `ExportOpenAccountParties`
* Omits: `ImportOpenAccountParties`, `AdditionalData`

| Element | Type / length | Description |
|---|---|---|
| `ExportOpenAccountParties` | group |  |

**`ExportOpenAccountParties`**

| Path | Type / length | Description |
|---|---|---|
| `ExportOpenAccountParties/Buyer` | party | Buyer. |
| `ExportOpenAccountParties/Seller` | party | Seller. |
| `ExportOpenAccountParties/SubmittingBank` | party | Submitting bank. |
| `ExportOpenAccountParties/BuyerBank` | party | Buyer's bank. |


### `TFEOASRC` – Seller cancellation request

| | |
|---|---|
| Creates in TI | Export Open Account seller cancellation request event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body elements** (besides the [common blocks](README.md#common-blocks-in-every-message)):

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
| `InformationForSeller` | string ≤32000 |  |
| `ExtraData` | group |  |

### `TFEOACAR` – Cancel

| | |
|---|---|
| Creates in TI | Export Open Account cancel event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** same elements as `TFEOASRC` (section above), with the differences below.

* Adds:

| Element | Type / length | Description |
|---|---|---|
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


### `TFEOABRE` – Buyer amendment request (recorded)

| | |
|---|---|
| Creates in TI | Export Open Account buyer amendment request event |
| Purpose (schema) | - |
| Channel exposure | Review |
| Phase | 5 |

**Body:** [Shape: Open account amendment](shared-message-shapes.md#shape-open-account-amendment) with differences.

* Adds: `ExportOpenAccountParties`
* Omits: `ImportOpenAccountParties`

| Element | Type / length | Description |
|---|---|---|
| `ExportOpenAccountParties` | group |  |

**`ExportOpenAccountParties`**

| Path | Type / length | Description |
|---|---|---|
| `ExportOpenAccountParties/Buyer` | party | Buyer. |
| `ExportOpenAccountParties/Seller` | party | Seller. |
| `ExportOpenAccountParties/SubmittingBank` | party | Submitting bank. |
| `ExportOpenAccountParties/BuyerBank` | party | Buyer's bank. |


### `TFEOABRC` – Buyer cancellation request (recorded)

| | |
|---|---|
| Creates in TI | Export Open Account buyer cancellation request event |
| Purpose (schema) | - |
| Channel exposure | Review |
| Phase | 5 |

**Body:** same elements as `TFEOACAR` (section above).


### `TFEOARBE` – Seller response to buyer amendment request

| | |
|---|---|
| Creates in TI | Export Open Account seller response to buyer amendment request event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Open account response](shared-message-shapes.md#shape-open-account-response).

### `TFEOARBC` – Seller response to buyer cancellation request

| | |
|---|---|
| Creates in TI | Export Open Account seller response to buyer cancellation request event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Open account response](shared-message-shapes.md#shape-open-account-response).

### `TFEOATBE` – TMA response to buyer amendment request

| | |
|---|---|
| Creates in TI | Export Open Account TMA response to buyer amendment request event |
| Purpose (schema) | - |
| Channel exposure | Bank-side |
| Phase | - |

**Body:** [Shape: Open account response](shared-message-shapes.md#shape-open-account-response).

### `TFEOATBC` – TMA response to buyer cancellation request

| | |
|---|---|
| Creates in TI | Export Open Account TMA response to buyer cancellation request event |
| Purpose (schema) | - |
| Channel exposure | Bank-side |
| Phase | - |

**Body:** [Shape: Open account response](shared-message-shapes.md#shape-open-account-response).

### `TFEOATSE` – TMA response to seller amendment request

| | |
|---|---|
| Creates in TI | Export Open Account TMA response to seller amendment request event |
| Purpose (schema) | - |
| Channel exposure | Bank-side |
| Phase | - |

**Body:** [Shape: Open account response](shared-message-shapes.md#shape-open-account-response).

### `TFEOATSC` – TMA response to seller cancellation request

| | |
|---|---|
| Creates in TI | Export Open Account TMA response to seller cancellation request event |
| Purpose (schema) | - |
| Channel exposure | Bank-side |
| Phase | - |

**Body:** [Shape: Open account response](shared-message-shapes.md#shape-open-account-response).

### `TFEOARPY` – Request payment

| | |
|---|---|
| Creates in TI | Export Open Account request payment event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Open account payment presentation](shared-message-shapes.md#shape-open-account-payment-presentation).

### `TFEOAPAY` – Payment

| | |
|---|---|
| Creates in TI | Export Open Account payment event |
| Purpose (schema) | - |
| Channel exposure | Review |
| Phase | 5 |

**Body:** [Shape: Open account payment presentation](shared-message-shapes.md#shape-open-account-payment-presentation).

### `TFEOACPR` – Response to a payment request

| | |
|---|---|
| Creates in TI | Export Open Account payment request response event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Export open account payment response](shared-message-shapes.md#shape-export-open-account-payment-response).

### `TFEOACPY` – Continue payment

| | |
|---|---|
| Creates in TI | Export Open Account continue payment event |
| Purpose (schema) | - |
| Channel exposure | Customer |
| Phase | 5 |

**Body:** [Shape: Export open account payment response](shared-message-shapes.md#shape-export-open-account-payment-response).

### `TFEOARPP` – Payment by response party

| | |
|---|---|
| Creates in TI | Export Open Account payment by response party event |
| Purpose (schema) | - |
| Channel exposure | Review |
| Phase | 5 |

**Body:** [Shape: Export open account payment response](shared-message-shapes.md#shape-export-open-account-payment-response) with differences.

* Adds: `AdditionalData`

| Element | Type / length | Description |
|---|---|---|
| `AdditionalData` | group | Additional data for the event - where customization is not used the bank can attach details to the event which are mapped to the additional data he... |

**`AdditionalData`**

| Path | Type / length | Description |
|---|---|---|
| `AdditionalData/DataItem` | group ×n | Receives "additional data" from Gateway, which are recorded as event notifications. |
| `AdditionalData/DataItem/Name` | string ≤50 |  |
| `AdditionalData/DataItem/Value` | string |  |


## 8. Validation rules

### 8.1 Rules supported directly by the schema

Lengths, data types and the allowed values of coded fields are in the tables in sections 6 and 7 and are the source for the channel's client and server validators. Dates are ISO `xsd:date`. Boolean elements carry `Y` or `N`. Amounts are strings (maximum 22 characters); agree the decimal format with the TI team. Responses use `ResponseType` ∈ {`A` accepted, `R` rejected}.

### 8.2 Business rules to apply in the channel

Normal practice for this product and the checks a TSO would otherwise perform by hand. **[Proposed]**: switch each on or off per bank policy.

| # | Rule |
|---|---|
| V1 | Only actions valid for the current baseline state are offered. |
| V2 | Invoice amount ≤ outstanding baseline amount; invoice references are unique per baseline. |
| V3 | A payment request lists all required documents and data for matching. |
| V4 | A response to a mismatch states the seller's decision for each discrepancy. |

### 8.3 Minimum content for a usable instruction

**[Proposed]** `MasterRef`, `PurchaseOrderReference`, `InvoiceReference`, `eBankMasterRef`, `eBankEventRef`, plus the action's elements.

## 9. Illustrative requests

Values are invented. Element names, prefixes and order follow `sources/example_xml/`. Elements not shown are omitted, which the schema allows for optional elements. **Test against the gateway before use.**

**`TFEOARPY`**

```xml
<?xml version="1.0" standalone="yes"?>
<ServiceRequest xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:m="urn:messages.service.ti.apps.tiplus2.misys.com"
                xmlns:c="urn:common.service.ti.apps.tiplus2.misys.com"
                xmlns="urn:control.services.tiplus2.misys.com">
  <RequestHeader>
    <Service>TI</Service>
    <Operation>TFEOARPY</Operation>
    <Credentials><Name>CHANNEL_SVC</Name><Password>********</Password></Credentials>
    <TargetSystem>TI</TargetSystem>
    <SourceSystem>CORPCHANNEL</SourceSystem>
    <CorrelationId>9b1f4c2e-5a0d-4c47-9f3e-2c1d7a6b8e10</CorrelationId>
    <CreationDate>2026-10-18</CreationDate>
  </RequestHeader>
  <m:TFEOARPY>
    <m:Context>
      <c:Branch>HOBR</c:Branch>
      <c:Customer>ACMEEXP</c:Customer>
      <c:Product>EOA</c:Product>
      <c:TheirReference>PO-9981</c:TheirReference>
    </m:Context>
    <m:MasterRef>EOA2600011</m:MasterRef>
    <m:EBankMasterRef>CH-EOA-000011</m:EBankMasterRef>
    <m:EBankEventRef>CH-EOA-000011-02</m:EBankEventRef>
    <m:InvoiceReference>INV-88001</m:InvoiceReference>
    <m:PurchaseOrderReference>PO-9981</m:PurchaseOrderReference>
    <m:TransactionMatchingReference>TMA-10442</m:TransactionMatchingReference>
    <m:InvoiceIssueDate>2026-10-10</m:InvoiceIssueDate>
  </m:TFEOARPY>
</ServiceRequest>
```

## 10. Response handling and tracking

| `Status` | Channel behaviour |
|---|---|
| `SUCCEEDED` | TI accepted the message and created the event. Mark `ReceivedByBank`; keep `Warning` / `Info` texts. |
| `FAILED` | TI rejected the message. Show `Details.Error` to the maker and return the draft. No automatic retry. |
| `UNAVAILABLE` / timeout | Keep in the outbox, retry with back-off, and confirm by enquiry (`eBankEventRef`) before each re-send so no second event is created. |

Rules shared by every product are in [README – Common behaviour](README.md#common-behaviour).

Timeline of events on the master: *Baseline matched*, *Amendment requested*, *Payment requested*, *Mismatch*, *Settled*.

## 11. Exceptions

| Scenario | Handling |
|---|---|
| Buyer rejects the amendment | Status shown; the seller may submit another request. |
| Mismatch unanswered | Alert with deadline. |
| Gateway `FAILED`, `UNAVAILABLE`, timeout, duplicate click | As in [README – Common behaviour](README.md#common-behaviour). |

## 12. Acceptance criteria

1. Seller can see the baseline timeline.
2. Each action produces one correct message.
3. Mismatch responses are captured per discrepancy.
4. Every step is in the audit trail with user, time and `CorrelationId`.
5. The stored request validates against the schema rules in 8.1 and matches the structure in section 9.

## 13. Open items

| # | Item |
|---|---|
| EOA-1 | Which party sends the baseline (`TFEOACRE`)? |
| EOA-2 | Is the BPO offered to customers? |
| EOA-3 | Confirm customer demand for open account in the first release. |
