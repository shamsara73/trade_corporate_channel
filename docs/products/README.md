# Product specifications

> Part of the Trade Corporate Channel design set. Catalogue: [01](../01-interface-catalogue.md) · Functional specification: [02](../02-functional-specification.md).

One specification per trade product, written to the same outline as the first one, [Import Letter of Credit](import-letter-of-credit.md). Together they cover every customer-facing message in the FTI Gateway interface. Bodies shared by many messages are specified once in [shared message shapes](shared-message-shapes.md).

## Index

| Product | Specification | Customer role | Priority | Messages |
|---|---|---|---|---:|
| `ILC` | [Import Letter of Credit](import-letter-of-credit.md) | Applicant | P1, first release | 8 |
| `ISB` | [Import Standby Letter of Credit](import-standby-lc.md) | Applicant | P1, wave 2 (after Import LC) | 6 |
| `IGT` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) | Applicant (principal) | P1, wave 2 | 6 |
| `UTI` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) | Applicant (principal) | P1, wave 2 (decide IGT/ISB vs UTI first) | 6 |
| `SHG` | [Shipping Guarantee](shipping-guarantee.md) | Importer (applicant under an LC, or drawee under a collection) | P1, wave 3 | 2 |
| `IDC` | [Inward (Import) Collection](inward-collection.md) | Drawee (importer) | P1, wave 3 | 7 |
| `IOA` | [Import Open Account (Buyer side, TMA)](import-open-account.md) | Buyer | P3 | 17 |
| `ELC` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) | Beneficiary (exporter) | P2 | 11 |
| `ESB` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) | Beneficiary | P2 | 8 |
| `EGT` | [Guarantee Received (Beneficiary side)](guarantee-received.md) | Beneficiary | P2 | 7 |
| `UTR` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) | Beneficiary | P2 | 7 |
| `ODC` | [Outward (Export) Collection](outward-collection.md) | Drawer (exporter) | P2 | 11 |
| `EOA` | [Export Open Account (Seller side, TMA)](export-open-account.md) | Seller | P3 | 18 |
| `INV` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) | Seller, buyer or anchor party in a supply chain finance (SCF) programme | P3 | 17 |
| `FIN` | [Trade Finance and Repayments](financing.md) | Borrower | P3 | 20 |
| `PAY` | [Clean Payments, Cheques and Standing Orders](payments.md) | Ordering customer (remitter) | P4 | 9 |
| `LIC` | [Licences and Participations](licence-and-participation.md) | Licence holder (customer); participations are bank-side | P4 | 5 |
| `GEN` | [Documents and System Messages](common-documents-and-system.md) | Any | Always (supports every product) | 8 |

Shared: [shared message shapes](shared-message-shapes.md).

## How each specification is organised

| Section | Content |
|---|---|
| 1 Purpose and scope | What the product is, the customer's role, what is out of scope. |
| 2 Messages | Every message of the product, its kind, what it creates in TI, whether the channel exposes it, and the phase. |
| 3 Business scenario | A worked example and sequence diagrams. |
| 4 Preconditions | What must be true before the customer can act. |
| 5 Screens | Step-by-step capture form with the schema element behind every field. |
| 6 Primary message | `Context` values, channel-managed elements, the field reference, nested structures and enumerations. |
| 7 Other messages | Each servicing message, either by reference to a shared shape or by its own field table. |
| 8 Validation | Schema rules, business rules, and the minimum content for a usable instruction. |
| 9 Illustrative requests | Sample XML. Structure from the gateway's example files, values invented. |
| 10 Response handling and tracking | What to do with each gateway status and how the customer follows progress. |
| 11 Exceptions | Product-specific exceptional cases. |
| 12 Acceptance criteria | What "done" means for the product. |
| 13 Open items | Questions for the TI team and the business. |

Tags: **[Proposed]** is a design decision; **[Assumption]** is something the interface documents do not confirm. Each product keeps its own open-items list; the cross-cutting questions are in [02, section 11](../02-functional-specification.md).

## Common blocks in every message

Every `TF*` body starts with the same skeleton. These blocks are not repeated in the product specifications.

**`Context`**

| Path | Type / length | Description |
|---|---|---|
| `Context/Branch` | string 1–8 | The input branch for the transaction. |
| `Context/Customer` | string 1–20 | Sender's customer mnemonic. |
| `Context/CustomerSwiftAddress` | string ≤14 | The SWIFT BIC of the customer sending the instruction. |
| `Context/Product` | string ≤4 | TI Plus Product code associated with the event is to be created  e.g. - ICC - Inward documentary - OCC - Ou... |
| `Context/Event` | string ≤4 | Code of the event to be created e.g. - ISI - Issuance - COR - Correspondence. |
| `Context/OurReference` | string 1–20 | Bank's Master reference for this transaction (if known). |
| `Context/TheirReference` | string 1–34 | Sender's reference for this transaction (if known). |
| `Context/Team` | string 1–15 | Team to which the transaction should be assigned. |
| `Context/BehalfOfBranch` | string 1–8 | The behalf of branch for the transaction. |
| `Context/BehalfOfBranchSwiftAddress` | string ≤14 | The SWIFT BIC of the Behalf of branch of the transaction. |
| `Context/TemplateIdentifier` | string ≤35 | The Deal template reference upon which the transaction is based. |
| `Context/EventReference` | string ≤6 | Event reference consisting of a prefix and a number. |
| `Context/Step` | string 1–15 | ID of the step to be enriched  TSDS-264 (PG). |
| `Context/SubsidiaryMasterPrefixAndSerial` | group | The subsidiary master prefix and serial number. |
| `Context/SubsidiaryMasterPrefixAndSerial/MasterPrefix` | string 1–3 | Master reference prefix. |
| `Context/SubsidiaryMasterPrefixAndSerial/MasterSerialNumber` | int | Master reference serial number. Available Values: 0 to 99999999 |
| `Context/ExchangeNonAutoComplete` | boolean | Whether the exchange step always requires user intervention. Boolean values. |

**`DocumentsReceiveds`** (repeating inner group)

| Path | Type / length | Description |
|---|---|---|
| `DocumentsReceiveds/DocumentsReceived` | group ×n | Details of the documents associated with this transaction (e.g. scanned documents, PDFs, Images or spreadsh... |
| `DocumentsReceiveds/DocumentsReceived/DocID` | string ≤128 | Unique identifier in external system. |
| `DocumentsReceiveds/DocumentsReceived/DMSID` | string ≤35 | The DMS id. |
| `DocumentsReceiveds/DocumentsReceived/DocType` | string ≤3 | The document type code - this must match one of the bank defined document types in TI Plus. e.g. - BOL - Bi... |
| `DocumentsReceiveds/DocumentsReceived/Description` | string ≤35 | Description of the document  e.g. "Letter from customer". |
| `DocumentsReceiveds/DocumentsReceived/ReceivedDate` | date | Date document was received by the bank. |
| `DocumentsReceiveds/DocumentsReceived/ReceivedTime` | string ≤8 | Time document was received by the bank. |
| `DocumentsReceiveds/DocumentsReceived/BankDoc` | boolean | Flag that denotes if document is an internal bank document. Boolean values. |
| `DocumentsReceiveds/DocumentsReceived/BatchID` | string ≤100 | Batch identifier. |
| `DocumentsReceiveds/DocumentsReceived/DocFaceRef` | string ≤20 | Document face reference. |
| `DocumentsReceiveds/DocumentsReceived/FirstMail` | string ≤20 |  |
| `DocumentsReceiveds/DocumentsReceived/SecondMail` | string ≤20 |  |
| `DocumentsReceiveds/DocumentsReceived/MailingTotal` | string ≤20 |  |
| `DocumentsReceiveds/DocumentsReceived/TransportDocRef` | string ≤256 | Transport document reference. |

**`EventNotificationss`** (repeating inner group)

| Path | Type / length | Description |
|---|---|---|
| `EventNotificationss/EventNotifications` | group ×n | Details of any key instructions or data relating to the transaction for the attention of the Input user. |
| `EventNotificationss/EventNotifications/MessageData` | string | Message data - e.g. "Finance details have been arranged for this transaction". |
| `EventNotificationss/EventNotifications/MessageDescription` | string ≤50 | Message description - e.g. "Input action required". |
| `EventNotificationss/EventNotifications/MessageInfo` | string ≤222 | Message Information - e.g. "Contact the Finance department with reference 09020019-001 for further informat... |
| `EventNotificationss/EventNotifications/MessageNumber` | string ≤10 | Free format Number/Identifier e.g. "0001". |
| `EventNotificationss/EventNotifications/Actioned` | boolean | Whether the notification needs to be actioned by the input user - N - Requires actioning - Y - Actioned. Bo... |

* `FileIdentification` – string 4–70 – File Identification and associated file name.
**`EmbeddedItemss`** (repeating inner group)

| Path | Type / length | Description |
|---|---|---|
| `EmbeddedItemss/EmbeddedItems` | group ×n | Details of documents such as PDFs, images, word documents, spreadsheets etc that relate to this transaction... |
| `EmbeddedItemss/EmbeddedItems/ID` | string ≤128 | Name of the embedded item e.g. Cusletter.doc. |
| `EmbeddedItemss/EmbeddedItems/AttachType` | string ≤35 | Type - e.g. PDF, TIFF, XLS, DOC - free format field. |
| `EmbeddedItemss/EmbeddedItems/DocType` | string ≤3 | The document type code - this must match one of the bank defined document types in TI Plus. e.g. - BOL - Bi... |
| `EmbeddedItemss/EmbeddedItems/DocumentDate` | date | Date of the document. |
| `EmbeddedItemss/EmbeddedItems/Description` | string ≤35 | Description of the document. |
| `EmbeddedItemss/EmbeddedItems/FileName` | string |  |
| `EmbeddedItemss/EmbeddedItems/DataStream` | string | The actual data stream associated with the document type. |
| `EmbeddedItemss/EmbeddedItems/MimeType` | string ≤128 | The Mime type: Application/pdf, Image/jpeg etc. |
| `EmbeddedItemss/EmbeddedItems/BankDoc` | boolean | Boolean values. |
| `EmbeddedItemss/EmbeddedItems/BatchID` | string ≤32 |  |
| `EmbeddedItemss/EmbeddedItems/DocFaceRef` | string ≤20 | Document face reference. |
| `EmbeddedItemss/EmbeddedItems/FirstMail` | string ≤20 | Attachment's 1st mail field. |
| `EmbeddedItemss/EmbeddedItems/SecondMail` | string ≤20 | Attachment's 2nd mail field. |
| `EmbeddedItemss/EmbeddedItems/MailingTotal` | string ≤20 | Attachment's total mail field. |
| `EmbeddedItemss/EmbeddedItems/IsCustomerAttachedDocument` | boolean | Boolean values. |
| `EmbeddedItemss/EmbeddedItems/TransportDocRef` | string ≤256 | Transport document reference. |
| `EmbeddedItemss/EmbeddedItems/DocumentFileName` | string ≤128 | Filename. |

* `MasterRef` – string 1–20 – e.g. Documentary Credit Number, FX contract number.
* `ContinueEvent` – string ≤6 – 
**`ProvisionalFlags`**

| Path | Type / length | Description |
|---|---|---|
| `ProvisionalFlags/IsProvisional` | boolean | Boolean values. |
| `ProvisionalFlags/AutoCreateContinuationEvent` | boolean | Boolean values. |

**`ExternalReview`**

| Path | Type / length | Description |
|---|---|---|
| `ExternalReview/ReviewStatus` | string ≤1 | Available values: 'C'='Confirmed', 'R'='Rejected'. |
| `ExternalReview/ReasonForRejection` | string ≤1024 |  |


### Party block

Parties (applicant, beneficiary, sender, banks...) share one structure:

| Element | Type / length | Description |
|---|---|---|
| `Customer` | string 1–20 | Customer mnemonic. |
| `LegalEntityIdentifier` | string ≤20 |  |
| `NameAddress` | string ≤1024 | Full name and address. |
| `SwiftAddress` | string ≤14 | SWIFT BIC. |
| `Reference` | string 1–34 | The party's reference. |
| `Contact` | string ≤35 | Contact name. |
| `ZipCode` | string 1–15 | Zip/Postcode. |
| `Telephone` | string 1–20 | Telephone number. |
| `FaxNumber` | string 1–20 | Fax number. |
| `TelexNumber` | string 1–20 | Telex number. |
| `Country` | string 1–2 | Country. |
| `TelexAnswerBack` | string 1–8 | Telex answerback. |
| `Email` | string ≤128 | Email address. |
| `Language` | string 1–2 | Language code. |
| `MXAddress` | group | ISO 20022 structured address: `Name` (≤ 140), `Department`, `SubDepartment`, `StreetName`, `BuildingNumber`, `BuildingName`, `Floor`, `PostBox`, `Room`, `PostCode`, `TownName`, `TownLocationName`, `DistrictName`, `CountrySubDivision`, `Country`, `AddressLine1`, `AddressLine2` (≤ 70) |
| `SwiftTradeAddress` | group | Four lines of ≤ 35 characters plus `Country`, `Name`, `PostCode`, `Town` |

Give either the customer mnemonic (`Customer`) or the free-text name and address. For parties that end up in a SWIFT message prefer `SwiftTradeAddress`. **[Assumption]**

## Common behaviour

These rules apply to every product and are stated once.

| Topic | Rule |
|---|---|
| Envelope | `ServiceRequest` with `RequestHeader` (`Service` = `TI`, `Operation` = message name, `Credentials`, `SourceSystem`, `TargetSystem`, `CorrelationId`, `CreationDate`...). See [01, section 2](../01-interface-catalogue.md). |
| Namespaces | Message-level elements use prefix `m:`; the contents of reusable groups (context, parties, amounts, terms) use `c:`. Sample XML in each specification is generated from the gateway's example files and follows this. |
| Names | Take element names from the schema catalogue for each message. Capitalisation (`eBankMasterRef` / `EBankMasterRef`), singular/plural (`Documentss`, `EmbeddedItemss`) and spelling (`ApplcableRules`, `Acount`) vary and must never be assumed. |
| Channel references | Send `eBankMasterRef` (constant per master) and `eBankEventRef` (unique per event) on every message that has them. They are the reconciliation key. |
| Idempotency | Same `eBankEventRef` on every retry. Before re-sending after a timeout or `UNAVAILABLE`, query TI for the event. |
| Ordering | One in-flight message per master, so events reach TI in business order. |
| Approval | Maker-checker in the channel before anything is sent; the stored message is exactly what was approved. |
| Response | `SUCCEEDED` = accepted by TI (not authorised). `FAILED` = show `Details.Error`, return to maker. `UNAVAILABLE` = retry. |
| Tracking | Gateway response → `SearchMasters` → `RetrieveMasterSummaries` / `RetrieveMasterDetail` by `DefinitionID`, plus `Notifications.All` if enabled. |
| Attachments | Inline `EmbeddedItems` or prior `DocumentAttachment`; document type from the bank list; size and type limits enforced at upload. |
| Dates, amounts | Dates are ISO `yyyy-mm-dd`. Amounts are strings (≤ 22 characters) in the schema; agree the decimal format with the TI team. |
| Text | Free text that TI maps to SWIFT fields must obey the SWIFT character set and line lengths. **[Assumption]** |
| Hidden fields | Routing and take-on elements (`Team`, `Step`, `BankCodes`, `ExternalReview`, `ProvisionalFlags`...) are set by configuration, never by the customer. |
| Audit | Store user, time, `CorrelationId`, request and response (without credentials) for every submission. |

## Coverage

Every non-migration `TF*` message belongs to exactly one specification.

| Message | Specification |
|---|---|
| `TFAPIDTA` | [Documents and System Messages](common-documents-and-system.md) |
| `TFATTDOC` | [Documents and System Messages](common-documents-and-system.md) |
| `TFBUYCAN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFBUYFIN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFCHICOR` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCHOCOR` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCOLACP` | [Inward (Import) Collection](inward-collection.md) |
| `TFCOLAMD` | [Outward (Export) Collection](outward-collection.md) |
| `TFCOLAMR` | [Outward (Export) Collection](outward-collection.md) |
| `TFCOLCON` | [Inward (Import) Collection](inward-collection.md) |
| `TFCOLCOR` | [Outward (Export) Collection](outward-collection.md) |
| `TFCOLDSR` | [Outward (Export) Collection](outward-collection.md) |
| `TFCOLFNR` | [Outward (Export) Collection](outward-collection.md) |
| `TFCOLNACP` | [Inward (Import) Collection](inward-collection.md) |
| `TFCOLNEW` | [Outward (Export) Collection](outward-collection.md) |
| `TFCOLPAY` | [Inward (Import) Collection](inward-collection.md) |
| `TFCPCCRT` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCPHCRT` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCPICOR` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCPICRT` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCPOCOR` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCPSCRT` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFCRNADJ` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFCRNCAN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFCRNLNK` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFCRNNEW` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFCSOCOR` | [Clean Payments, Cheques and Standing Orders](payments.md) |
| `TFEGTAMD` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFEGTBCR` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFEGTBMR` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFEGTCOR` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFEGTDPR` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFEGTNEW` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFEGTPYR` | [Guarantee Received (Beneficiary side)](guarantee-received.md) |
| `TFELCADD` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCAMD` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCAOP` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCBCR` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCBMR` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCCOR` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCDPR` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCFLC` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCNEW` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCPYR` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFELCTRF` | [Export Letter of Credit (Beneficiary side)](export-letter-of-credit.md) |
| `TFEOAAME` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOABRC` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOABRE` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOACAR` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOACPR` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOACPY` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOACRE` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOAPAY` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOARBC` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOARBE` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOARPP` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOARPY` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOASRC` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOASRE` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOATBC` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOATBE` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOATSC` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFEOATSE` | [Export Open Account (Seller side, TMA)](export-open-account.md) |
| `TFESBAMD` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESBBCR` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESBBMR` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESBCOR` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESBDPR` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESBNEW` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESBPYR` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFESGPYR` | [Export Standby Letter of Credit (Beneficiary side)](export-standby-lc.md) |
| `TFEXPEVT` | [Documents and System Messages](common-documents-and-system.md) |
| `TFEXRRSP` | [Documents and System Messages](common-documents-and-system.md) |
| `TFFACDRD` | [Trade Finance and Repayments](financing.md) |
| `TFFACDRF` | [Trade Finance and Repayments](financing.md) |
| `TFFCBRPY` | [Trade Finance and Repayments](financing.md) |
| `TFFECNEW` | [Trade Finance and Repayments](financing.md) |
| `TFFELRPY` | [Trade Finance and Repayments](financing.md) |
| `TFFEORPY` | [Trade Finance and Repayments](financing.md) |
| `TFFIARPY` | [Trade Finance and Repayments](financing.md) |
| `TFFICRPY` | [Trade Finance and Repayments](financing.md) |
| `TFFILRPY` | [Trade Finance and Repayments](financing.md) |
| `TFFINCOR` | [Trade Finance and Repayments](financing.md) |
| `TFFINNEW` | [Trade Finance and Repayments](financing.md) |
| `TFFIORPY` | [Trade Finance and Repayments](financing.md) |
| `TFFOCRPY` | [Trade Finance and Repayments](financing.md) |
| `TFFRBRPY` | [Trade Finance and Repayments](financing.md) |
| `TFFSACOR` | [Trade Finance and Repayments](financing.md) |
| `TFFSARPY` | [Trade Finance and Repayments](financing.md) |
| `TFFSCNEW` | [Trade Finance and Repayments](financing.md) |
| `TFFSCRPY` | [Trade Finance and Repayments](financing.md) |
| `TFGENCNF` | [Documents and System Messages](common-documents-and-system.md) |
| `TFGWYACK` | [Documents and System Messages](common-documents-and-system.md) |
| `TFIBFCRE` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFIBPAMD` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFIBPCAN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFIBPCRE` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFIDCCOR` | [Inward (Import) Collection](inward-collection.md) |
| `TFIDCCRE` | [Inward (Import) Collection](inward-collection.md) |
| `TFIDCNCAM` | [Inward (Import) Collection](inward-collection.md) |
| `TFIDSRPY` | [Trade Finance and Repayments](financing.md) |
| `TFIGTAMN` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) |
| `TFIGTAMR` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) |
| `TFIGTAPP` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) |
| `TFIGTCAN` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) |
| `TFIGTCOR` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) |
| `TFIGTPYR` | [Guarantee Issued (Import Guarantee)](guarantee-issued.md) |
| `TFILCAMN` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCAPP` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCBMR` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCBTB` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCCAN` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCCLM` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCCOR` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFILCPYR` | [Import Letter of Credit](import-letter-of-credit.md) |
| `TFINVAMD` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFINVCAN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFINVDIS` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFINVNEW` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFINVSET` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFIOAAME` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOABRC` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOABRE` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOACAR` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOACRE` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOAPBR` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOAPGR` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOAPTR` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOARBC` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOARPY` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOARSC` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOARSE` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOASRC` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOASRE` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOATBE` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOATSC` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIOATSE` | [Import Open Account (Buyer side, TMA)](import-open-account.md) |
| `TFIRFRPY` | [Trade Finance and Repayments](financing.md) |
| `TFISBAMN` | [Import Standby Letter of Credit](import-standby-lc.md) |
| `TFISBAMR` | [Import Standby Letter of Credit](import-standby-lc.md) |
| `TFISBAPP` | [Import Standby Letter of Credit](import-standby-lc.md) |
| `TFISBCAN` | [Import Standby Letter of Credit](import-standby-lc.md) |
| `TFISBCOR` | [Import Standby Letter of Credit](import-standby-lc.md) |
| `TFISBPYR` | [Import Standby Letter of Credit](import-standby-lc.md) |
| `TFLICAMD` | [Licences and Participations](licence-and-participation.md) |
| `TFLICCAN` | [Licences and Participations](licence-and-participation.md) |
| `TFLICCOR` | [Licences and Participations](licence-and-participation.md) |
| `TFOCOLCON` | [Outward (Export) Collection](outward-collection.md) |
| `TFOCOLPAY` | [Outward (Export) Collection](outward-collection.md) |
| `TFODCCOR` | [Outward (Export) Collection](outward-collection.md) |
| `TFODCCRE` | [Outward (Export) Collection](outward-collection.md) |
| `TFODCNCAM` | [Outward (Export) Collection](outward-collection.md) |
| `TFPTNAMD` | [Licences and Participations](licence-and-participation.md) |
| `TFPTNCRE` | [Licences and Participations](licence-and-participation.md) |
| `TFRCVDOC` | [Documents and System Messages](common-documents-and-system.md) |
| `TFSELCAN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFSELFIN` | [Invoices, Credit Notes and Supply Chain Finance](invoice-and-scf.md) |
| `TFSHGAPP` | [Shipping Guarantee](shipping-guarantee.md) |
| `TFSHGCOR` | [Shipping Guarantee](shipping-guarantee.md) |
| `TFUTIAMN` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) |
| `TFUTIAPP` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) |
| `TFUTICAN` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) |
| `TFUTICLM` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) |
| `TFUTICOR` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) |
| `TFUTIPYR` | [Undertaking Issued (Guarantee / Standby, new model)](undertaking-issued.md) |
| `TFUTRAMD` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFUTRBCR` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFUTRBMR` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFUTRCOR` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFUTRDPR` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFUTRNEW` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFUTRPYR` | [Undertaking Received (Beneficiary side, new model)](undertaking-received.md) |
| `TFWLCRSP` | [Documents and System Messages](common-documents-and-system.md) |

**Excluded – data take-on (migration):** `TFEGTDTO`, `TFELCDTO`, `TFEOACTO`, `TFIGTDTO`, `TFILCDTO`, `TFIOACTO`, `TFUTIDTO`, `TFUTRDTO`. They load existing transactions into TI and are not part of the channel.

## Regenerating

All product specifications except [Import Letter of Credit](import-letter-of-credit.md) are generated. Hand-written business content is in `tools/specgen/defs_*.py` and `shapes.py`; schema-derived content (field tables, nested structures, enumerations, sample XML structure) is read from `sources/`.

```
python3 -I tools/specgen/build.py
```

Messages that TI sends to the channel are documented separately in [TI outward messages](../outbound/README.md).

Edit the definitions, not the generated Markdown. After a gateway upgrade, replace `sources/` and regenerate; `git diff` then shows exactly what changed in the interface.
