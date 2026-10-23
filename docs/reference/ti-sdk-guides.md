# Trade Innovation SDK guides – what they tell the channel

> Hand-written summary of three Finastra guides added to `sources/` on 2026-10-22. Page numbers are the printed page numbers in each PDF. Where a guide and the gateway example files disagree, the example files win, because they come from the installed gateway version (2.11.5.2.J); the guides describe Trade Innovation 2.11 in general.

| Guide | File | Pages | Relevance to the channel |
|---|---|---|---|
| SDK – Systems Interfacing Guide (v1.0.1, Nov 2025) | `sources/SDK - Systems Interfacing Guide - Trade Innovation 2.11.pdf` | 75 | **High.** Message envelope, transports (JMS queues), customer gateway messages in both directions, notifications, enquiries. |
| SDK – Application Extension Guide (v1.0, May 2025) | `sources/SDK - Application Extension Guide - Trade Innovation 2.11.pdf` | 106 | **Medium.** How bank-defined fields reach gateway messages (`<ExtraData>`), event field codes, Open APIs. The rest (postings, Eclipse editor, end of day, fax, DMS) is TI-internal. |
| SDK – Systems Integration Guide (v1.0, May 2025) | `sources/SDK - Systems Integration Guide - Trade Innovation 2.11.pdf` | 13 | **Low.** Zone proxy, cross-zone dashboard, URL navigation into and out of TI. Useful only if the bank wants staff to jump between the channel and TI screens. |

## 1. Key facts

### 1.1 Two kinds of interface (Interfacing Guide, pp. 1–4)

* **Core banking interface services** (`Account`, `Customer`, `Limit`, `BusinessSupport`, `BackOffice`, `StandingSettlements`, …): TI is the *consumer*; the bank's core system answers. Fixed layouts.
* **Customer gateway interface service**: messages between TI and "your bank's customer access system (Finastra Corporate Channel, for example)" – that is, this channel. **Inward messages follow the published schemas; outward messages are defined by the bank** as document types and templates built from event fields. TI decides to send one when the event is released, from the document or from the addressee being a gateway user.
* Supported products for gateway messages: clean payments, export LC, export guarantees, export standby, factoring, finance standalone, import LC, import guarantees, import standby, invoices, outward collections, licences. Gateway documents can be configured for any event, not only these.

Consequence: confirms open question Q1 (the channel does not call `Customer`/`Account`/`Limit`; TI does, towards the core system) and explains why `TFUTICRT`/`TFUTIACK`/`TFUTIREJ` are bank mappings rather than published schemas.

### 1.2 Envelope and header (pp. 5–15)

* Every request is a `ServiceRequest` (`RequestHeader` + payload) or a `DeferredResponse`; every answer is a `ServiceResponse` (`ResponseHeader`) or an `Acknowledgment`.
* Namespaces: `control.services.tiplus2.misys.com` (default), `messages.service.ti.apps.tiplus2.misys.com`, `common.service.ti.apps.tiplus2.misys.com`, and **`custom.service.ti.apps.tiplus2.misys.com`** for extra data. The example files prefix these with `urn:`; follow the examples.
* Header meanings (answers Q2):

| Element | Meaning |
|---|---|
| `ReplyFormat` | `NONE` – no reply, processed through TI's async framework · `STATUS` – protocol header only · `FULL` – header and functional data |
| `ReplyTarget` | JNDI name of the reply queue, used when no JMS reply-to is set |
| `TargetSystem` / `SourceSystem` | Zone id of the target TI instance / the sending system |
| `NoRepair` | `Y`: on error, return the message instead of putting it in TI's repair queue. Default `N` (TI repairs) |
| `NoOverride` | `Y`: if TI raises warnings, refuse the transaction and return them. Default `N` (a TI user handles the warnings) |
| `CorrelationId` | Request id, echoed in the response header. **Not** the JMS correlation id |
| `TransactionControl` | Only `NONE` is in use; `NEW` and `INHERIT` are reserved |
| `CreationDate` | Business date of the data |
| `GroupingId` | Common reference for a set of messages, searchable in TI's incoming gateway message manager |
| `Credentials` | User name; `Password`, `Certificate`, `Digest` are "reserved for future use" |

* `ServiceResponse` status: `SUCCEEDED` (does **not** mean functionally correct; errors may be in `Details`; a message that went to the repair queue also returns `SUCCEEDED` with an info text), `FAILED` (nothing updated), `UNAVAILABLE`.
* The guide's example shows `<Credentials><User>`; all 431 gateway example files and the real XSD use `<Credentials><Name>`. Use `Name` (Q31, answered).
* Bulk: `TIBulk.Item` wraps several `ServiceRequest`s; maximum 130,800 characters.
* **Schemas exist as jars**: `com.misys.tiplus2.gen.ti-xsd.jar` and `com.misys.tiplus2.gen.ti-xsd.customisation.jar` (folders `control`, `services`, `common`, `custom`, `messages`). TI validates inbound messages against them. The repository has only the generated documentation text, not these XSDs (Q30). **Received 2026-10-22**: they are a bank build 2.10.3.1, older than the 2.11.5.2.J documentation; see [xsd-jars](xsd-jars.md).

> **Evidence from the bank (2026-10-23):** a working `TFFINNEW` was sent without the `ServiceRequest` envelope described here; see [working samples](working-samples.md) and Q33.

### 1.3 Transports (pp. 18–22)

* **Pseudo-synchronous JMS** (recommended): TI's transport server listens on `queue/<prefix>Incoming`; replies go to the JMS `reply-to` queue, else to the queue named in `ReplyTarget`, else to `queue/<prefix>IncomingReply`, and only when `ReplyFormat` is `FULL` or `STATUS`. A JMS correlation id on the request is copied to the reply.
* **Trade Innovation Corporate Channels (TICC) interface**: a pre-configured pseudo-synchronous JMS set-up for a corporate channel:

| Direction | JNDI queue | Content |
|---|---|---|
| Channel → TI | `queue/ticcIncoming` | `ServiceRequest` with `RequestHeader` and the gateway message (for example `TFIGTAPP`) |
| TI → channel (replies to the above) | `queue/ticcIncomingReply` | `ServiceResponse` |
| TI → channel (outward gateway messages) | `queue/ticcOutgoing` | `ServiceRequest` carrying the outward message |
| Channel → TI (optional replies to outward messages) | `queue/ticcOutgoingReply` | only read if `service.access.jms.ticc.client.timeout` is set |

  In the Finastra product the other end is Fabric Connect (FFC); for this bank the channel takes that place. JNDI names are TI-side aliases; the physical IBM MQ queue names are for the bank to give (Q13).
* Other entry points: plain JMS (legacy), EJB (`ejb/EnigmaServiceAccess`), a `TICR.ReceivedDocs` web service for imaging, REST **SPIs** for some TI-outgoing calls (OIDC-secured). None replaces the gateway queues for this channel.
* Testing aid: **APIStubs** (tables `APISERVER`, `APICLIENT`, `APIRESPONSE`) can inject inbound messages and canned replies in a TI test zone.

### 1.4 Outward gateway messages (p. 43)

> "Messages sent to corporate access systems are wholly configurable in Trade Innovation as gateway document messages. These are generated when a transaction is released. They are routed as a `GATEWAY.DOCS` service operation by default, but may be configured to be routed based on their message name as a `GATEWAY.<message name>` service operation … The structure of the message is a ServiceRequest with a payload based on the message definition configured in Trade Innovation. No functional response is expected."

So `TFUTIACK` arrives as a `ServiceRequest` whose header says `Service` = `GATEWAY` and `Operation` = `DOCS` or `TFUTIACK`, with the mapping's fields as the payload. This answers most of Q21; the payload's exact XML (element nesting, namespace) still needs one real message.

### 1.5 Transaction event messages and exchange steps (pp. 47–48)

* Inbound `TF*` messages start events, update an event in progress, or answer a waiting workflow step.
* To update an event in progress, `Context` must carry `OurReference`, `EventReference`, `Step` (and `SubsidiaryMasterPrefixAndSerial` for subsidiary events). The reply to an *exchange step* is a `DeferredResponse` with the same `TF*` message (for example `TFILCAPP`) and the original `CorrelationId`; an *external verification* reply uses `TFEXRRSP`. TI answers with an `Acknowledgment`.
* `FAILED` requires at least one error; warnings use `SUCCEEDED` + `Warning`. Do not mix errors and warnings. Data in an exchange-step reply is applied even if it carries errors.

This is the mechanism behind the bank-query loop (doc 02, state `BankQuery`): TI could send a step document to the channel and wait for the customer's answer.

### 1.6 Notifications (pp. 43–45)

Optional, enabled by configuration, sent as `Notifications.All` or per category (`Notifications.event-step`, `master-delete`, `async-job`, `async-step`). The **event-step** payload has: product and event names, `created-by`, `ti-event-reference`, `customer`, `customer-name-and-address`, `amount-and-currency`, `team`, `status`, `user`, `step-id`, `step-description`, `deadline-timestamp`, `sla`, `zone`. This is a real-time status source for ADR 0008 if the bank enables it (Q4).

### 1.7 Enquiries (p. 48)

`TI.SearchMasters`, `TI.RetrieveMasterDetail(s)` and `TI.RetrieveMasterSummaries` return content **defined by enquiry response definitions** set up in System Tailoring ("Enquiry responses"). The `DefinitionID` of Q3 names one of these definitions; the bank defines what it returns.

### 1.8 Bank-defined fields (Application Extension Guide, pp. 2, 10, 46, 60, 74–75)

* Extra fields are added with the customisation editor; each gets an **event field code starting with lower-case `c`** (for example `cAAU`), usable in documents, postings and rules. This is why the IGT event field list has lower-case codes.
* On **inbound gateway messages**, customised fields go under `<ExtraData>` with their logical names; repeating groups use the group name as container. Customer messages use `<CustomerExtraData>` and `<TICustomerExtraData>`. The schema extensions are in the customisation jar (`custom` namespace).
* Customised fields can also be exposed in TI **Open APIs** (JSON, `custom-fields`) – Finastra's REST alternative to the gateway, not in scope today.
* The available event fields for a product/event are listed in System Tailoring → *General Business Functions* → *Browse and Output Field Codes* – the likely source of `IGT_IIG_All_E.csv`. Ask for the same export for every product and event the channel covers.

Answers Q25 in part: bank-defined fields reach TI through `<ExtraData>`; their names and types are in the bank's customisation jar.

### 1.9 Systems Integration Guide

* **URL navigation into TI**: global application URL + `zone`, `masterRef`, `eventRef`, `stepId`, `sessionType=view|continue`, `pane=ExternalSystemAccessWP`, `source=<return URL>`. Needs a shared sign-on. Could give bank staff a "open in TI" link from a back-office view of the channel (not a customer feature).
* **Zone proxy**: a system can appear as a TI zone in TI's cross-zone dashboard (queues, `TI.ZoneDashboardRequest`). Not needed for the channel.

## 2. What changes in the design

| Area | Before | Now |
|---|---|---|
| Gateway transport (ADR 0005, Q13) | HTTP POST assumed | **JMS over IBM MQ**, TICC queue set-up; HTTP dropped |
| Outward messages (ADR 0005, Q21) | Delivery unknown | `ServiceRequest` with `GATEWAY.DOCS` / `GATEWAY.<name>` on the TICC outgoing queue; no reply needed |
| Header values (Q2) | Unknown meanings | Documented above; defaults unchanged |
| Schemas (ADR 0004) | Built from documentation text | Real XSD jars received; catalogue generated from them; samples validated ([xsd-jars](xsd-jars.md)) |
| Bank-defined fields (Q25) | Unknown | `<ExtraData>` with logical names, from the customisation jar |
| Status (ADR 0008, Q4) | Polling, notifications maybe | Notifications are a standard, configurable feature with known payload |
| Simulator (ADR 0009) | HTTP server | Embedded JMS broker with the same queues |

## 3. Not relevant to the channel

Postings, charges accruals, netting, Eclipse customisation project, end-of-day control actions, amount translation, fax plug-in, document windows, archive delete, DMS interfaces, APIStubs data editor installation, SWIFT, Equation, MT798, watch-list SPIs.
