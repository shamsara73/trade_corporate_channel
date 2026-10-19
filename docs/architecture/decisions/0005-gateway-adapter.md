# 0005 – Gateway adapter

**Status:** Proposed · **Date:** 2026-10-19

## Decision

The Maven module `gateway-adapter` is the only code that knows about XML, namespaces (`m:`, `c:`, control namespace), `RequestHeader` / `ResponseHeader`, credentials, transport and TI error text. An ArchUnit test fails the build if any other module imports `javax.xml.*`, `jakarta.xml.*`, `org.w3c.dom.*` or `org.xml.*`.

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
* XML is written and read with **StAX** driven by the catalogue (element order, prefixes and repeating groups come from it). No JAXB, because there are no usable XSD files, only the documentation text.
* The header is built from **configuration properties** (`gateway.header.*`): `Service`, `SourceSystem`, `TargetSystem`, `ReplyFormat`, `NoRepair`, `NoOverride`, `TransactionControl`, `GroupingId`, credential mode. Defaults mirror the example files and are marked `ASSUMPTION(Q2)` until the TI team confirms.
* **Outward messages.** The adapter also parses messages TI sends (`OutwardMessageParser`, driven by the outward definitions of [0004](0004-schema-catalogue.md)) and exposes them as neutral `Payload` trees through an `OutwardMessageReceiver` port. How they arrive (HTTP callback, queue, file, polling an endpoint) is open question Q21; the port hides it.
* Implementations: `HttpGatewayClient` (JDK `HttpClient` or Spring `RestClient`) and the simulator (a real HTTP server, see 0009), so tests exercise the same client. The transport itself (HTTP, MQ, file) is open question Q13; the interface hides it.
* Timeouts, TLS and error mapping are decided here; business retry is the worker's job.
* Credentials are read from the secret store at start-up (Spring configuration properties from the environment), never logged, never written to `submission` rows.

## Consequences

* A gateway upgrade touches one module.
* Tests of `domain`, `web` and `worker` need no XML.

**Reversal cost:** low.
