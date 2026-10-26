# 0005 – Gateway adapter

**Status:** Proposed · **Date:** 2026-10-19

## Decision

The package `gateway` is the only code that knows about XML, namespaces (`m:`, `c:`, control namespace), `RequestHeader` / `ResponseHeader`, credentials, transport and TI error text. An ArchUnit test fails the build if any other package imports `javax.xml.*`, `jakarta.xml.*`, `org.w3c.dom.*` or `org.xml.*`.

```java
public interface GatewayClient {
    GatewayResult send(Operation operation, Payload payload, SendMeta meta);
}

public record GatewayResult(Status status, List<String> errors, List<String> warnings,
                            List<String> infos, String correlationId, Optional<Payload> body) {
    public enum Status { SUCCEEDED, FAILED, UNAVAILABLE }
}
```

* Input and output are neutral `Payload` trees keyed by catalogue path.
* XML is written and read with **StAX** driven by the catalogue (element order, prefixes and repeating groups come from it). No JAXB, because there are no usable XSD files, only the documentation text. The real XSDs are now in `sources/` (TI's `ti-xsd` jars, bank build 2.10.3.1); the adapter's tests validate every generated message against them, and JAXB remains unnecessary.
* The header is built from **configuration properties** (`gateway.header.*`): `Service`, `SourceSystem`, `TargetSystem`, `ReplyFormat`, `NoRepair`, `NoOverride`, `TransactionControl`, `GroupingId`, credential mode. Defaults mirror the example files and are marked `ASSUMPTION(Q2)` until the TI team confirms.
* **Outward messages.** The adapter also parses messages TI sends (`OutwardMessageParser`, driven by the outward definitions of [0004](0004-schema-catalogue.md)) and exposes them as neutral `Payload` trees through an `OutwardMessageReceiver` port. They arrive as a `ServiceRequest` with `Service` = `GATEWAY` and `Operation` = `DOCS` or the message name, on the TICC outgoing queue; no functional reply is expected ([SDK guides](../../reference/ti-sdk-guides.md), section 1.4; Q21). The receiver reads the operation, then the payload.
* **Transport: JMS over IBM MQ** (updated 2026-10-22 after the [SDK guides](../../reference/ti-sdk-guides.md), section 1.3; Q13). `JmsGatewayClient` puts the `ServiceRequest` on the TI incoming queue (`ticcIncoming` in TI's terms) with a JMS reply-to and JMS correlation id, and reads the `ServiceResponse` from the reply queue by correlation id, with a timeout that maps to `UNAVAILABLE`. A JMS listener on the TI outgoing queue (`ticcOutgoing`) feeds `OutwardMessageReceiver`. Physical queue names and the queue manager come from configuration. The simulator (0009) uses an embedded JMS broker, so tests exercise the same client. HTTP is no longer assumed.
* The `RequestHeader` `CorrelationId` (TI's request id) and the JMS correlation id are different things; both are stored with the submission.
* **Envelope confirmed (2026-10-23).** The bank's successful end-to-end test sent `TFFINNEW` inside the `ServiceRequest` envelope with `ReplyFormat` = `FULL`, and TI answered with a `ServiceResponse` echoing `CorrelationId` ([working samples](../../reference/working-samples.md), section 2; Q33). The adapter always sends the envelope; `CorrelationId` carries the message reference (`TheirRef`).
* **Outward messages** arrive as `ServiceRequest` with `Service` = `GATEWAY` and `Operation` = the message name (for example `TFFINACK`); their `CorrelationId` is TI's own number, so the receiver matches them by `eBankMasterRef` + `eBankEventRef`. The parser must accept `xsi:nil`, `CDATA` and base64 attachments of tens of kilobytes, and convert amounts from minor units (Q35).
* Timeouts, TLS and error mapping are decided here; business retry is the outbox dispatcher's job (package `jobs`).
* Credentials are read from the secret store at start-up (Spring configuration properties from the environment), never logged, never written to `submission` rows.

## Consequences

* A gateway upgrade touches one package.
* Tests of `domain`, `web` and `jobs` need no XML.

**Reversal cost:** low.
