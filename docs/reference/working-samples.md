# Working messages from the bank's test environment

> Hand-written. Messages here were **accepted by the bank's TI test environment**. They are stronger evidence than the gateway documentation or the SDK guides. Customer data has been replaced with invented values before committing (ground rule 9); structure, element order, codes and reference formats are kept exactly.

## 1. `TFFINNEW` – Finance Standalone create (received 2026-10-23)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns2:TFFINNEW xmlns="urn:control.services.tiplus2.misys.com" xmlns:ns2="urn:messages.service.ti.apps.tiplus2.misys.com" xmlns:ns4="urn:custom.service.ti.apps.tiplus2.misys.com" xmlns:ns3="urn:common.service.ti.apps.tiplus2.misys.com">
  <ns2:Context>
    <ns3:Customer>0009990001</ns3:Customer>
    <ns3:TheirReference>TESTING_20260813_2</ns3:TheirReference>
    <ns3:BehalfOfBranch>001</ns3:BehalfOfBranch>
  </ns2:Context>
  <ns2:EventNotificationss>
    <ns2:EventNotifications>
      <ns2:MessageData>TESTING2</ns2:MessageData>
      <ns2:MessageDescription>Description of Goods</ns2:MessageDescription>
      <ns2:MessageInfo>Additional information</ns2:MessageInfo>
      <ns2:Actioned>N</ns2:Actioned>
    </ns2:EventNotifications>
  </ns2:EventNotificationss>
  <ns2:Amount>
    <ns3:Amount>111.01</ns3:Amount>
    <ns3:Currency>USD</ns3:Currency>
  </ns2:Amount>
  <ns2:DebitParty>
    <ns3:Customer>0009990001</ns3:Customer>
    <ns3:NameAddress>POC TEST CUSTOMER</ns3:NameAddress>
    <ns3:Reference>TESTING_20260813_2</ns3:Reference>
  </ns2:DebitParty>
  <ns2:EffectiveDate>2025-11-13</ns2:EffectiveDate>
  <ns2:FinanceAmount>
    <ns3:Amount>111.01</ns3:Amount>
    <ns3:Currency>USD</ns3:Currency>
  </ns2:FinanceAmount>
  <ns2:InterestSettlementCcy>USD</ns2:InterestSettlementCcy>
  <ns2:MaturityDate>2026-01-12</ns2:MaturityDate>
  <ns2:StartDate>2025-11-13</ns2:StartDate>
  <ns2:TrustReceiptLoan>N</ns2:TrustReceiptLoan>
  <ns2:InterestFromAfter>A</ns2:InterestFromAfter>
  <ns2:eBankMasterRef>TF2607130000XX03</ns2:eBankMasterRef>
  <ns2:eBankEventRef>2607130000XX03</ns2:eBankEventRef>
  <ns2:ProductCode>FSA</ns2:ProductCode>
  <ns2:FinancePeriod>
    <ns3:TenorDays>60</ns3:TenorDays>
    <ns3:TenorPeriod>D</ns3:TenorPeriod>
  </ns2:FinancePeriod>
  <ns2:TheirRef>TESTING_20260813_2</ns2:TheirRef>
  <ns2:ProductType>PSF</ns2:ProductType>
</ns2:TFFINNEW>
```

It is valid against TI's XSD jar (`python3 -I tools/xsdcheck/check_samples.py` checks the POC copy of it).

### What it tells us

| Observation | Consequence | Where recorded |
|---|---|---|
| **No `ServiceRequest` / `RequestHeader` envelope.** The root element is the message itself (`TFFINNEW`), so `Service`, `Operation`, `Credentials`, `CorrelationId` are not in the message. | Either the queue the sender writes to takes bare messages and the operation comes from the root element name, or a middleware layer adds the envelope (the original references contained a middleware name). Our documents and ADR 0005 assumed the envelope. | Q33; ADR 0005; POC assumption POC-5 |
| Namespace prefixes `ns2`, `ns3`, `ns4` and the default control namespace on the root: the typical output of a JAXB marshaller built from the XSD jar. | Prefixes do not matter; the namespaces do. Generating from the jar (ADR 0004) is consistent with what works. | – |
| Valid against the **2.10.3.1 bank build** in the jar. | Supports the working assumption of Q32 (the jar wins). | Q32 |
| `Context` has only `Customer`, `TheirReference`, `BehalfOfBranch`: **no `Branch`, no `Product`, no `Event`.** | `Context/Product` is not needed for `TFFINNEW`; the product is given in the body. Our specs filled `Branch` and `Product` in every sample. | Financing spec |
| Product **`FSA`** (Finance Standalone) in `ProductCode`, product type `PSF` in `ProductType`. | The financing spec assumed `FIN`; corrected for `TFFINNEW`. Other products may also carry the code in the body; check per message. | `docs/products/financing.md` |
| The customer's reference appears **three times**: `Context/TheirReference`, `DebitParty/Reference`, `TheirRef`. | The channel fills all three with the same value (max 34 / 35 / 35). | POC; financing sample |
| `eBankMasterRef` = `"TF"` + `eBankEventRef` (16 and 14 characters). | A usable channel reference format: event reference first, master reference derived. Both elements allow at least 20 characters. | POC; Q34 |
| `EventNotifications` carries free text (`MessageData`, `MessageDescription`, `MessageInfo`, `Actioned` = `N`). | A way to pass a note to the trade service officer with the event. | – |
| `EffectiveDate` = `StartDate`; `MaturityDate` = `StartDate` + `TenorDays` (60). | Dates must be consistent; the POC computes them from today. | POC |
