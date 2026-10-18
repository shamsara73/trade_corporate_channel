# 0005 – Gateway adapter

**Status:** Proposed · **Date:** 2026-10-18

## Decision

`packages/gateway-adapter` is the only code that knows about XML, namespaces (`m:`, `c:`, control namespace), `RequestHeader` / `ResponseHeader`, credentials, transport and TI error text.

```ts
interface GatewayClient {
  send(op: OperationName, payload: NeutralPayload, meta: SendMeta): Promise<GatewayResult>;
}
// GatewayResult = { status: 'SUCCEEDED' | 'FAILED' | 'UNAVAILABLE'; errors: string[]; warnings: string[]; infos: string[]; correlationId: string; body?: NeutralPayload }
```

* Input is a neutral JSON payload keyed by schema path; output is the same.
* The header is built from **configuration**, not code: `Service`, `SourceSystem`, `TargetSystem`, `ReplyFormat`, `NoRepair`, `NoOverride`, `TransactionControl`, `GroupingId`, credential mode. Defaults mirror the example files and are marked `ASSUMPTION(Q2)` until the TI team confirms.
* Implementations: `HttpGatewayClient` (real), `SimulatorGatewayClient` (tests). The transport (HTTP, MQ, file) is open question Q-transport; the interface hides it.
* Timeouts, TLS, and retry-safe errors are decided here; business retry is the worker's job.
* Credentials are read from the secret store at start-up, never logged, never written to `submission` rows.

## Consequences

* Gateway upgrades touch one package.
* Unit tests of `domain` and `api` need no XML.

**Reversal cost:** low.
