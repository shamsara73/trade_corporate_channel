# 01 – Core Trade Finance Interface Catalogue

> Part of the Trade Corporate Channel design set. Companion documents:
> [02 – Functional Specification](02-functional-specification.md) ·
> [Product specifications](products/README.md)

## 1. Purpose

Our core trade finance application is **Finastra Fusion Trade Innovation (FTI / "TI Plus")**, integrated through the **FTI Gateway**. This repository holds the gateway's interface documentation (version **2.11.5.2.J**). This document catalogues every interface message in `sources/`, groups them by role, and states which ones the new trade channel needs. It is the reference the other two documents build on.

**Sources used**

| Path | Content |
|---|---|
| `sources/FTI-Gateway-Messages-2.11.5.2.J.html` | Full gateway message reference (alphabetical element listing). |
| `sources/xsd_element_docs/*.xsd.txt` | 431 files, one per message: element tree with type, min/max length, nillable, enumerations and descriptions. |
| `sources/example_xml/*.xml` | 476 skeleton request/response samples. Element values are placeholders (the element name repeated), not realistic data. |

> **Limits of the source material.** The XSD documentation does not mark elements as mandatory. Whether a field is required is decided by the bank's TI tailoring (product, event, customer type) and validation rules at runtime. Request-header semantics (for example `NoRepair`, `NoOverride`, `TransactionControl`, `GroupingId`) are not described either. Both points are listed as open questions in document 02 and must be settled with the TI support team or a test environment.

## 2. Message envelope

Every message is an XML document in one of two envelopes.

**Request**

```xml
<ServiceRequest xmlns='urn:control.services.tiplus2.misys.com'
                xmlns:m='urn:messages.service.ti.apps.tiplus2.misys.com'
                xmlns:c='urn:common.service.ti.apps.tiplus2.misys.com'>
  <RequestHeader>
    <Service>TI</Service>            <!-- family: TI, Customer, Account, Limit, ... -->
    <Operation>TFILCAPP</Operation>  <!-- message name -->
    <Credentials> Name | Password | Certificate | Digest </Credentials>
    <ReplyFormat/> <ReplyTarget/> <TargetSystem/> <SourceSystem/>
    <NoRepair/> <NoOverride/> <CorrelationId/>
    <TransactionControl/> <CreationDate/> <GroupingId/>
  </RequestHeader>
  <m:TFILCAPP> ... message body ... </m:TFILCAPP>
</ServiceRequest>
```

**Response**

```xml
<ServiceResponse ...>
  <ResponseHeader>
    <Service/> <Operation/>
    <Status>SUCCEEDED | FAILED | UNAVAILABLE</Status>
    <Details> <Error/> <Warning/> <Info/> </Details>
    <CorrelationId/> <TargetSystem/> <SourceSystem/>
  </ResponseHeader>
  <m:...Response> ... </m:...Response>
</ServiceResponse>
```

Namespace conventions seen in every sample: message-level elements use prefix `m:`, and the children of reusable groups (Context, parties, amounts, terms of payment) use prefix `c:`.

`CorrelationId` appears in both headers and is the natural key for matching a response to the request that caused it.

## 3. Message families

| Family (`Service`) | Count | What it is | Direction (see note) | Needed by channel |
|---|---:|---|---|---|
| `TI` – `TF*` messages | 181 | Business instructions that create an **event** on a new or existing **master** (transaction) in TI. | Channel → TI | **Yes – core** |
| `TI` – master enquiry (`SearchMasters`, `RetrieveMasterSummaries`, `RetrieveMasterDetail`) | 3 | List and read transactions already in TI. The returned shape is chosen by `DefinitionID` (see `IncomingEnquiryResponseDefinition`). | Channel → TI | **Yes** |
| `TI` – `DocumentAttachment`, `DocumentAttachmentRetrieve` | 2 | Add, update, delete and fetch document attachments. | Channel → TI | **Yes** |
| `TI` – security enquiries (`RetrieveSecurityUser`, `RetrieveSecurityUsers`) | 2 | Read TI user, team and entitlement data. | Channel → TI | Optional |
| `TI` – static-data maintenance | ~190 | One message per reference-data entity (Customer, Currency, Country, Product, EventType, DocumentType, GoodsCode, Incoterms, ListValue, ...). Each carries `MaintType` = Define / Add / Update / Delete. | Bank admin → TI | **No**, but they define the code lists the channel must mirror |
| `Customer`, `Account`, `Limit`, `StandingSettlements` | 2 + 5 + 10 + 2 | Customer, account, balance, credit-limit and SSI lookups. | **TI → bank host systems** (assumed) | Indirect – see note |
| `BusinessSupport` | 9 | FX rate, base rate, interest calculation, payment routing, FX contracts and options. | TI → host (assumed) | Indirect |
| `BackOffice` | 12 | Postings, FX/MM deals, exposure, projections, batch verify. | TI → back office | No |
| TI outward message mappings (`TFUTICRT`) | 1 so far | Structured message TI **sends** after an event, defined by a mapping document supplied by the TI team, not by the gateway documentation. See [outbound](outbound/README.md). | TI → channel | **Yes** – completion signal and TI references |
| `Notifications.All` | 1 | Outward notification: `Category`, `Subcategory`, `DeploymentId`, `System`, `Type`, `User`, `Time`, `Payload` (name/value entries). | TI → subscribers | **Yes** – candidate status feed |
| `Broadcast` (`Dashboard`, `ReallocateTransactions`) | 2 | Work-queue dashboard and team re-allocation. | Bank staff tools | No |
| `Browser` (`DMSSearch`, `HostSearch`) | 2 | Search the document store or a host system. | TI → DMS/host | No |
| `Loopback` (`GatewayCreate`, `RepairMsg`, `CSVLogger`) | 3 | Create, resubmit (repair) and log gateway messages. | Internal | No (but `RepairMsg` explains the `NoRepair` header flag) |
| `SWIFT.SwiftOut`, `SwiftMX.MXAddressValidation`, `TI.SwiftIn` | 3 | SWIFT message output / input and ISO 20022 address validation. | TI ↔ SWIFT | No (address structure is reused by parties) |
| `TIBulk.Item` | 1 | Bulk item processing. | – | Out of scope |

> **Direction note – to be confirmed.** The repository documents *shapes*, not *who calls whom*. In a standard FTI deployment the `Account`, `Customer`, `Limit`, `StandingSettlements`, `BusinessSupport`, `BackOffice`, `SWIFT`, `Browser` and `Notifications` interfaces are **outbound from TI to the bank's own systems**: TI asks the host to validate a customer, check a balance, reserve a limit or quote an FX rate. If that holds here, the channel does **not** call them; the channel needs its own source for customer, account, limit and FX look-ups (the bank's core banking or an API layer), and TI performs its own checks when the instruction arrives. Verify this with the TI team before designing the channel's reference-data layer.

## 4. How a `TF*` message works

1. The channel sends a `ServiceRequest` whose `Operation` is the message name (for example `TFILCAPP`).
2. The body starts with **`Context`**. Per the schema documentation, `Context` carries the customer, branch, team, references, and the **`Product`** (for example `ILC`, `ELC`, `IDC`, `ODC`) and **`Event`** (for example `ISI` issuance, `COR` correspondence) codes. For `TFRCVDOC` these fields choose the type of the new master or the master to attach the event to. For all other `TF*` messages the message name itself decides the event, and `MasterRef` (or `OurReference`) selects the existing master.
3. TI creates an **event** on the master (for `TFILCAPP`, an "Import Letter Of Credit issue event"). The event then follows the bank's orchestration (`Orchestration`, `EventOrchestrationStep`, SLA maps), where a **trade service officer (TSO)** reviews, enriches and authorises it. The `Step` and `ExchangeNonAutoComplete` context fields let the sender influence this.
4. The channel gets a `ServiceResponse` with `Status`. A `SUCCEEDED` status means TI **accepted the message**. It does not mean the transaction has been authorised.

### Elements shared by all `TF*` messages

| Block | Purpose |
|---|---|
| `Context` | Branch, customer, product, event, our/their reference, team, template, event reference, step. |
| `DocumentsReceiveds` / `DocumentsReceived` | References to documents that already sit in an external DMS (`DocID`, `DMSID`, `DocType`, received date/time, mail counts). |
| `EmbeddedItemss` / `EmbeddedItems` | Documents carried **inline** (`DataStream`, `MimeType`, `FileName`, `DocType`). TI stores them in its DMS when the message is processed. |
| `EventNotificationss` | Key instructions shown to the TSO (`MessageData`, `MessageDescription`, `MessageInfo`, `Actioned`). |
| `FileIdentification`, `MasterRef`, `ContinueEvent` | File name, existing master, follow-on event. |
| `ProvisionalFlags` | `IsProvisional`, `AutoCreateContinuationEvent` – create a provisional event that is finished later. |
| `ExternalReview` | `ReviewStatus` (C = confirmed, R = rejected) and `ReasonForRejection` – used when an external system reviews the instruction first. |
| `eBankMasterRef`, `eBankEventRef` | **The channel's own references** for the master and the event. These are the keys for reconciling channel and TI records. |
| `AdditionalData`, `ExtraData` | Bank-specific extension fields that are mapped without customisation. |

## 5. Business-message catalogue (`TF*`)

181 messages. "Kind" is derived from the message description and name by a script; check it against the schema before relying on it. "Creates in TI" is quoted from the schema documentation, including its original spelling (for example "Acount"). A dash means the documentation gives no text.

Reading the codes: `TF` + product (`ILC`, `ELC`, `IGT`, ...) + action. Common actions: `APP` application, `NEW` create/advise, `AMD/AMN/AMR` amend, `CAN` cancel, `COR` correspondence, `PYR` payment / claim response, `RPY` repayment, `DTO` data take-on, `BMR/BCR` beneficiary response to amend/cancel.

### Import Letter of Credit (`ILC`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFILCAMN` | Amend | Import Letter Of Credit amend event | Sent by the Applicant to the Bank to under an Import Letter of Credit requesting an amendment to the Letter Of Credit |
| `TFILCAPP` | Create / issue | Import Letter Of Credit issue event | Sent by the Applicant to the Bank to under an Import Letter Of Credit |
| `TFILCBMR` | Response / request | Import Letter Of Credit beneficiary response to amend event | - |
| `TFILCBTB` | Create / issue | Import Letter Of Credit issue event | Sent by the Applicant to the Bank to issue a Back to back Letter of Credit |
| `TFILCCAN` | Cancel | Import Letter Of Credit cancel event | Sent by the Applicant to the Issuing bank to request the cancellation of a Letter Of Credit |
| `TFILCCLM` | Claim / presentation | ILC claim received event | - |
| `TFILCCOR` | Correspondence | Import Letter Of Credit correspondence event | A general purpose message that is sent by the Applicant to the Issuing Bank in the form of General correspondence Queries or Answers |
| `TFILCDTO` | Data take-on (migration) | Import Letter Of Credit data take-on event | - |
| `TFILCPYR` | Claim / presentation | Import Letter Of Credit outstanding claim event | Sent by the Applicant to the Issuing bank to approve or refuse a claim under an Import Letter Of Credit |

### Import Standby LC (`ISB`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFISBAMN` | Amend | Import Standby Letter Of Credit amend event | Import Standby Letter Of Credit amendment request that is sent by the Applicant to the Issuing Bank |
| `TFISBAMR` | Amend | Import Standby Letter Of Credit (old) amend event | A basic amendment request that is sent by the Applicant to the Issuing Bank in a simple text format allowing the bank to then make the appropriate changes to the details of the Import Standby Letter Of Credit |
| `TFISBAPP` | Create / issue | Import Standby Letter Of Credit issue event | - |
| `TFISBCAN` | Cancel | Import Standby Letter Of Credit cancel event | - |
| `TFISBCOR` | Correspondence | Import Standby Letter Of Credit correspondence event | A general purpose message that is sent by the Applicant to the Issuing Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFISBPYR` | Claim / presentation | Import Standby Letter Of Credit outstanding claim event | - |

### Guarantee Issued (`IGT`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFIGTAMN` | Amend | Guarantee Issued amend event | - |
| `TFIGTAMR` | Amend | Guarantee Issued (old) amend event | - |
| `TFIGTAPP` | Create / issue | Guarantee Issued issue event | Sent by the Applicant to the Bank under a Guarantee Issued |
| `TFIGTCAN` | Cancel | Guarantee Issued cancel event | Sent by the Applicant to the Issuing bank to request the cancellation of a Guarantee Issued |
| `TFIGTCOR` | Correspondence | Guarantee Issued correspondence event | A general purpose message that is sent by the Applicant to the Issuing Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFIGTDTO` | Data take-on (migration) | Guarantee Issued data take-on event | - |
| `TFIGTPYR` | Claim / presentation | Guarantee Issued outstanding claim event | - |

### Undertaking Issued (URDG/ISP-style) (`UTI`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFUTIAMN` | Amend | Undertaking Issued amend event | - |
| `TFUTIAPP` | Create / issue | Undertaking issue event | - |
| `TFUTICAN` | Cancel | Undertaking Issued cancel event | Sent by the Applicant to the Issuing bank to request the cancellation of a Undertaking Issued |
| `TFUTICLM` | Claim / presentation | Undertaking Issued claim received event | - |
| `TFUTICOR` | Correspondence | Undertaking Issued correspondence event | A general purpose message that is sent by the Applicant to the Issuing Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFUTIDTO` | Data take-on (migration) | Undertaking Issued data take-on event | - |
| `TFUTIPYR` | Claim / presentation | Undertaking Issued outstanding claim event | - |

### Shipping Guarantee (`SHG`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFSHGAPP` | Create / issue | Shipping Guarantee create event | Sent by the importer such as Applicant under an ILC or Drawee under a Collection to the Bank requesting the issuance of a Shipping Guarantee |
| `TFSHGCOR` | Correspondence | Shipping Guarantee correspondence event | - |

### Inward (Import) Collection (`IDC`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFCOLACP` | Create / issue | Inward Collection - Acceptance event | Sent by the Drawee to the Collecting Bank providing approval of acceptance and (or) payment details relating to an Inward (Import) Collection |
| `TFCOLCON` | Create / issue | Inward Collection Continue payment event | Sent by the Drawee to the Collecting Bank providing approval and (or) payment details relating to an Inward (Import) Collection |
| `TFCOLNACP` | Create / issue | Collection non-acceptance event | - |
| `TFCOLPAY` | Create / issue | Inward Collection payment event | Sent by the Drawee to the Collecting Bank providing approval and (or) payment details relating to an Inward (Import) Collection |
| `TFIDCCOR` | Correspondence | IDC correspondence event | - |
| `TFIDCCRE` | Create / issue | Inward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Inward (Import) Collection (Clean or Documentary) |
| `TFIDCNCAM` | Create / issue | Inward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Inward (Import) Collection (Clean or Documentary) |

### Import Open Account (TMA) (`IOA`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFIOAAME` | Amend | Import Open Acount amendment event | - |
| `TFIOABRC` | Response / request | Import Open Acount buyer cancellation request event | - |
| `TFIOABRE` | Amend | Import Open Acount buyer amendment request event | - |
| `TFIOACAR` | Cancel | Import Open Account cancel event | - |
| `TFIOACRE` | Create / issue | Import Open Account create event | - |
| `TFIOACTO` | Data take-on (migration) | Import Open Account data take-on event | - |
| `TFIOAPBR` | Response / request | Import Open Account payment request buyer response to mismatches event | - |
| `TFIOAPGR` | Response / request | Import Open Account payment request general response event | - |
| `TFIOAPTR` | Create / issue | Import Open Account payment request TMA notification of mismatch accept or reject event | - |
| `TFIOARBC` | Response / request | Import Open Acount TMA response to buyer cancellation request event | - |
| `TFIOARPY` | Create / issue | Import Open Account request payment event | - |
| `TFIOARSC` | Response / request | Import Open Acount buyer response to seller cancellation request event | - |
| `TFIOARSE` | Response / request | Import Open Acount buyer response to seller amendment request event | - |
| `TFIOASRC` | Cancel | Import Open Acount seller cancellation request event | - |
| `TFIOASRE` | Amend | Import Open Acount seller amendment request event | - |
| `TFIOATBE` | Response / request | Import Open Acount TMA response to buyer amendment request event | - |
| `TFIOATSC` | Response / request | Import Open Acount TMA response to seller cancellation request event | - |
| `TFIOATSE` | Response / request | Import Open Acount TMA response to seller amendment request event | - |

### Export Letter of Credit (`ELC`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFELCADD` | Response / request | Export Letter Of Credit outstanding presentation event" | processes corporate to bank advice of discrepancy response |
| `TFELCAMD` | Amend | Export Letter Of Credit amend event | Sent by the Applicant to the Bank to under an Export Letter Of Credit requesting an amendment to the Letter Of Credit |
| `TFELCAOP` | Create / issue | Export Letter Of Credit assignment of proceeds event | - |
| `TFELCBCR` | Response / request | Export Letter Of Credit beneficiary response to cancel event | - |
| `TFELCBMR` | Response / request | Export Letter Of Credit beneficiary response to amend event | - |
| `TFELCCOR` | Correspondence | Export Letter Of Credit correspondence event | A general purpose message that is sent by the Beneficiary to the Advising Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFELCDPR` | Claim / presentation | ELC documents presented event | - |
| `TFELCDTO` | Data take-on (migration) | Export Letter Of Credit data take-on event | - |
| `TFELCFLC` | Create / issue | ELC Preshipment Finance event with customisation | - |
| `TFELCNEW` | Create / issue | Export Letter of Credit advise event | - |
| `TFELCPYR` | Claim / presentation | Export Letter Of Credit outstanding presentation event | - |
| `TFELCTRF` | Create / issue | Export Letter Of Credit transfer event | - |

### Export Standby LC (`ESB`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFESBAMD` | Amend | Export Standby Letter Of Credit amend event | - |
| `TFESBBCR` | Response / request | Export Standby Letter Of Credit beneficiary response to cancel event | - |
| `TFESBBMR` | Response / request | Export Standby Letter Of Credit beneficiary response to amend event | - |
| `TFESBCOR` | Correspondence | Export Standby Letter Of Credit correspondence event | A general purpose message that is sent by the Beneficiary to the Advising Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFESBDPR` | Claim / presentation | Export Standby Letter Of Credit documents presented event | - |
| `TFESBNEW` | Create / issue | Export Standby Letter Of Credit advise event | - |
| `TFESBPYR` | Claim / presentation | Export Standby Letter Of Credit outstanding presentation event | - |
| `TFESGPYR` | Claim / presentation | Export Standby Letter Of Credit outstanding presentation event | - |

### Guarantee Received (`EGT`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFEGTAMD` | Amend | Guarantee Received amend event | - |
| `TFEGTBCR` | Response / request | Guarantee Received beneficiary response to cancel event | - |
| `TFEGTBMR` | Response / request | Guarantee Received beneficiary response to amend event | - |
| `TFEGTCOR` | Correspondence | Guarantee Received correspondence event | A general purpose message that is sent by the Beneficiary to the Advising Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFEGTDPR` | Claim / presentation | Guarantee Received documents presented event | - |
| `TFEGTDTO` | Data take-on (migration) | Guarantee Received data take-on event | - |
| `TFEGTNEW` | Create / issue | Guarantee Received advise event | - |
| `TFEGTPYR` | Claim / presentation | Guarantee Received outstanding presentation event | - |

### Undertaking Received (`UTR`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFUTRAMD` | Amend | Undertaking Received amend event | - |
| `TFUTRBCR` | Response / request | Undertaking Received - Beneficiary Response to Cancel event | - |
| `TFUTRBMR` | Response / request | Undertaking Received - Beneficiary Response to Amend event | - |
| `TFUTRCOR` | Correspondence | Undertaking Received correspondence event | A general purpose message that is sent by the Beneficiary to the Advising Bank in the form of an Ancillary message, General correspondence, Queries or Answers |
| `TFUTRDPR` | Claim / presentation | Undertaking Received documents presented event | - |
| `TFUTRDTO` | Data take-on (migration) | Undertaking Received data take-on event | - |
| `TFUTRNEW` | Create / issue | Undertaking advise event | - |
| `TFUTRPYR` | Claim / presentation | Undertaking Received outstanding presentation event | - |

### Outward (Export) Collection (`ODC`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFCOLAMD` | Create / issue | Outward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Outward (Export) Collection (Clean or Documentary) |
| `TFCOLAMR` | Create / issue | Outward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Outward (Export) Collection (Clean or Documentary) |
| `TFCOLCOR` | Correspondence | Collection correspondence event | - |
| `TFCOLDSR` | Create / issue | Outward Collection discount event | Sent by the Drawer to the Bank to request a discount under the Collection |
| `TFCOLFNR` | Create / issue | Outward Collection advance event | Sent by the Drawer to request Financing of a Collection such as an Advance |
| `TFCOLNEW` | Create / issue | Outward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Outward (Export) or Inward (Import) Collection (Clean or Documentary) |
| `TFOCOLCON` | Create / issue | Outward Collection Continue payment event | Sent by the Drawee to the Collecting Bank providing approval and (or) payment details relating to an Inward (Import) Collection |
| `TFOCOLPAY` | Create / issue | Outward Collection payment event | Sent by the Drawee to the Collecting Bank providing approval and (or) payment details relating to an Inward (Import) Collection |
| `TFODCCOR` | Correspondence | ODC correspondence event | - |
| `TFODCCRE` | Create / issue | Outward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Outward (Export) Collection (Clean or Documentary) |
| `TFODCNCAM` | Create / issue | Outward Collection create event (Clean or Documentary according to CollectionType tag) | Sent by the Drawer to the bank providing full details of an Outward (Export) Collection (Clean or Documentary) |

### Export Open Account (TMA) (`EOA`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFEOAAME` | Amend | Export Open Account amendment event | - |
| `TFEOABRC` | Response / request | Export Open Account buyer cancellation request event | - |
| `TFEOABRE` | Amend | Export Open Account buyer amendment request event | - |
| `TFEOACAR` | Cancel | Export Open Account cancel event | - |
| `TFEOACPR` | Response / request | Export Open Account payment request response event | - |
| `TFEOACPY` | Create / issue | Export Open Account continue payment event | - |
| `TFEOACRE` | Create / issue | Export Open Account create event | - |
| `TFEOACTO` | Data take-on (migration) | Export Open Account data take-on event | - |
| `TFEOAPAY` | Create / issue | Export Open Account payment event | - |
| `TFEOARBC` | Response / request | Export Open Account seller response to buyer cancellation request event | - |
| `TFEOARBE` | Response / request | Export Open Account seller response to buyer amendment request event | - |
| `TFEOARPP` | Response / request | Export Open Account payment by response party event | - |
| `TFEOARPY` | Create / issue | Export Open Account request payment event | - |
| `TFEOASRC` | Cancel | Export Open Account seller cancellation request event | - |
| `TFEOASRE` | Amend | Export Open Account seller amendment request event | - |
| `TFEOATBC` | Response / request | Export Open Account TMA response to buyer cancellation request event | - |
| `TFEOATBE` | Response / request | Export Open Account TMA response to buyer amendment request event | - |
| `TFEOATSC` | Response / request | Export Open Account TMA response to seller cancellation request event | - |
| `TFEOATSE` | Response / request | Export Open Account TMA response to seller amendment request event | - |

### Invoices, Credit Notes, SCF and Bulk Payment (`INV`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFBUYCAN` | Cancel | Invoice Buyer Centric Finance cancel event | - |
| `TFBUYFIN` | Create / issue | Invoice Buyer Centric Finance create event | - |
| `TFCRNADJ` | Create / issue | Credit Note adjust event | - |
| `TFCRNCAN` | Cancel | Credit Note cancel event | - |
| `TFCRNLNK` | Create / issue | Credit Note link or unlink event | - |
| `TFCRNNEW` | Create / issue | Create Note create event | - |
| `TFIBFCRE` | Create / issue | Invoice Bulk Finance create event | - |
| `TFIBPAMD` | Amend | Invoice Bulk Payment amend event | - |
| `TFIBPCAN` | Cancel | Invoice Bulk Payment cancel event | - |
| `TFIBPCRE` | Create / issue | Invoice Bulk Payment create event | - |
| `TFIDSRPY` | Repayment | Seller Centric Finance repayment event | - |
| `TFINVAMD` | Amend | Invoice amend event | - |
| `TFINVCAN` | Cancel | Invoice cancel event | - |
| `TFINVDIS` | Create / issue | Invoice discount event | - |
| `TFINVNEW` | Create / issue | Invoice create event | - |
| `TFINVSET` | Create / issue | Invoice settlement event | - |
| `TFIRFRPY` | Repayment | Buyer Centric Finance repayment event | - |
| `TFSELCAN` | Cancel | Invoice Seller Centric Finance cancel event | - |
| `TFSELFIN` | Create / issue | Invoice Seller Centric Finance create event | - |

### Financing and Repayments (`FIN`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFFACDRD` | Create / issue | Factoring drawdown event | - |
| `TFFACDRF` | Create / issue | Factoring drawdown forecast event | - |
| `TFFCBRPY` | Repayment | Clean BA Finance repayment event | - |
| `TFFECNEW` | Create / issue | Finance Existing Collection create event with disburse to multiple parties details | - |
| `TFFELRPY` | Repayment | Export LC Finance repayment event | - |
| `TFFEORPY` | Repayment | Export Open Account Finance repayment event | - |
| `TFFIARPY` | Repayment | Finance Invoice Advance repayment event | - |
| `TFFICRPY` | Repayment | Inward Collection Finance repayment event | - |
| `TFFILRPY` | Repayment | Import Letter Of Credit Finance repayment event | - |
| `TFFINCOR` | Correspondence | Financing correspondence event | - |
| `TFFINNEW` | Create / issue | Finance Standalone create event with disburse to muultiple parties details | - |
| `TFFIORPY` | Repayment | Import Open Account Finance repayment event | - |
| `TFFOCRPY` | Repayment | Outward Collection Finance repayment event | - |
| `TFFRBRPY` | Repayment | Reimbursement Finance repayment event | - |
| `TFFSACOR` | Correspondence | Financing correspondence event with customisation | - |
| `TFFSARPY` | Repayment | Standalone Finance repayment event | - |
| `TFFSCNEW` | Create / issue | Finance SCF create event | - |
| `TFFSCRPY` | Repayment | Finance SCF repayment event | - |

### Clean Payments, Cheques, Standing Orders (`PAY`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFCHICOR` | Correspondence | Inward Cheque correspondence event | - |
| `TFCHOCOR` | Correspondence | Outward Cheque correspondence event | - |
| `TFCPCCRT` | Create / issue | Outward Customer Payment create event | - |
| `TFCPHCRT` | Create / issue | Outward Cheque create event | - |
| `TFCPICOR` | Correspondence | Inward Customer Payment correspondence event | - |
| `TFCPICRT` | Create / issue | Inward Customer Payment create event | - |
| `TFCPOCOR` | Correspondence | Outward Customer Payment correspondence event | - |
| `TFCPSCRT` | Create / issue | Standing Order create event | - |
| `TFCSOCOR` | Correspondence | Standing Order correspondence event | - |

### Licences and Participations (`OTH`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFLICAMD` | Amend | License amend event | - |
| `TFLICCAN` | Cancel | License cancel event | - |
| `TFLICCOR` | Correspondence | License correspondence event | - |
| `TFPTNAMD` | Create / issue | Participation Create event | - |
| `TFPTNCRE` | Create / issue | Participation Create event | - |

### Generic / Utility (`GEN`)

| Message | Kind | Creates in TI | Purpose |
|---|---|---|---|
| `TFAPIDTA` | Other | - | to use as GET API |
| `TFATTDOC` | Create / issue | The event defined by the Product and Event code in the context | This is a general purpose message that is used to send details of documents and or attachments to TI Plus to either create a new master or an event against an existing master The message consists of the common section described above together with the following details |
| `TFEXPEVT` | Create / issue | Expiry event | Expire transaction |
| `TFEXRRSP` | Response / request | - | Incoming response to an event step verification request |
| `TFGENCNF` | Other | - | General confirmation. Uploads the E-banking reference |
| `TFGWYACK` | Other | - | Delivery Notification |
| `TFRCVDOC` | Create / issue | The event defined by the Product and Event code in the context | This is a general purpose message that is used to send details of documents and or attachments to TI Plus to either create a new master or an event against an existing master. The message consists of the common section described above together with the following details |
| `TFWLCRSP` | Response / request | - | Watch list checker response |

## 6. Static-data messages (reference data the channel must mirror)

TI has about 190 maintenance messages. These are not called by the channel, but the **code lists** they define populate drop-downs and validation in the channel. The most relevant:

| Channel need | TI entity (message) |
|---|---|
| Products and types | `Product`, `ProductType`, `ProductGroup`, `EventType`, `EventGroup` |
| Customers and parties | `Customer`, `CustomerAddress`, `CustomerType`, `TICustomerDetails`, `LocalCustomer`, `TeamCustomer`, `CorporateAccess`, `PartyRole` |
| Branches, teams, users | `Branch`, `BranchAddress`, `Team`, `TeamUser`, `SecurityUser`, `SecurityGroup`, `UserRole` |
| Money | `Currency`, `CurrencyCalendar`, `FXRate`, `FXRateCode`, `CurrencySpotRate`, `BaseRate`, `ChargeCode`, `ChargeType`, `ChargeSchedule` |
| Trade terms | `Incoterms`, `GoodsCode`, `Country`, `ClauseText`, `ClauseType`, `DocumentType`, `AttachedDocumentType`, `DocumentTemplate` |
| Domain lists | `ListValue`, `DomainListEntry`, `DomainListMap`, `ProductListValueMap` |
| Settlement | `StandingSettlementInstructions`, `StandingSettlementInstructionsSet`, `Nostro`, `Correspondent`, `InstructionGroup`, `TransferMethod`, `ClearingSystemCode`, `IBANStructure`, `SwiftAddress` |
| Process and SLA | `Orchestration`, `OrchestrationTemplate`, `EventOrchestrationStep`, `SLADefinition`, `EventSLAMap`, `TeamEventMap` |
| Error handling | `CustomError`, `MessageInError`, `PaymentRejectCode` |
| Enquiry shape | `IncomingEnquiryResponseDefinition` – defines what `SearchMasters` / `RetrieveMasterDetail` return for a given `DefinitionID` |

The repository contains no *retrieval* message for these entities. The channel therefore needs a reference-data feed from the bank (extract from TI, or a maintained copy). This is an open question in document 02.

## 7. Applicability to the new channel

| Priority | Scope | Messages |
|---|---|---|
| **P1** | Import trade, customer as applicant / drawee | `TFILC*` (APP, AMN, CAN, PYR, COR), `TFISB*`, `TFIGT*`, `TFUTI*`, `TFSHG*`, `TFIDC*` / inward collection acceptance and payment, `TFRCVDOC`, `TFATTDOC` |
| **P2** | Export trade, customer as beneficiary / drawer | `TFELC*`, `TFESB*`, `TFEGT*`, `TFUTR*`, `TFODC*`, `TFCOL*` |
| **P3** | Financing, open account, invoices | `TFFIN*`, `TFFxxRPY` repayments, `TFIOA*`, `TFEOA*`, `TFINV*`, `TFCRN*`, `TFBUY*`, `TFSEL*`, `TFIBP*`, `TFIBF*` |
| **P4** | Payments | `TFCP*`, `TFCH*`, `TFCSO*` |
| **Excluded** | Bank-side or migration | `*DTO` / `*CTO` (data take-on), `TFWLCRSP`, `TFEXRRSP`, `TFGWYACK`, `TFEXPEVT`, `TFAPIDTA` (system-to-system) |
| **Always** | Enquiry and support | `SearchMasters`, `RetrieveMasterSummaries`, `RetrieveMasterDetail`, `DocumentAttachment*`, `Notifications.All` |

Rationale and phasing are discussed in document 02, section 9. Each product is specified in [products/README.md](products/README.md).
