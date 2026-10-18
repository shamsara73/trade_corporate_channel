# 0003 – PostgreSQL and the transactional outbox

**Status:** Proposed · **Date:** 2026-10-18

## Context

A trade instruction must reach TI exactly once as far as TI can tell, even if the channel crashes, the gateway is down, or a response is lost. Doc 02 (FR-20 to FR-24, section 7) requires this.

## Decision

* **PostgreSQL** (a supported major version) as the only datastore for channel state. Documents (attachments) go to an object store behind a `DocumentStore` port; the first implementation can be the database large-object/`bytea` or a bucket.
* **Transactional outbox.** When the last approval is recorded, the same database transaction inserts an `outbox` row containing the final request XML, `CorrelationId`, `eBankMasterRef`, `eBankEventRef` and the target master.
* The **worker** claims rows with `SELECT ... FOR UPDATE SKIP LOCKED`.
* **Single flight per master:** a transaction-scoped advisory lock keyed on the master id, so events for one master reach TI in order.
* **Idempotency:** `eBankEventRef` has a unique constraint. A retry reuses it. Before any re-send after a timeout or `UNAVAILABLE`, the worker asks TI (`SearchMasters` / `RetrieveMasterDetail`) whether the event already exists.
* Every attempt writes a `submission` row with the request, response (credentials removed), status and timing.
* Migrations are plain SQL files in `db/migrations`, applied in order, never edited after merge.

## Alternatives

| Option | Why not (now) |
|---|---|
| Message broker (Kafka, RabbitMQ, MQ) | Adds an operational component and a dual-write problem the outbox avoids |
| Direct call from API after commit | Loses messages on crash between commit and call |

## Consequences

* Delivery is at-least-once to the gateway; exactly-once is achieved by idempotent references plus the pre-resend check. **[Assumption: TI treats a repeated `eBankEventRef` safely – open question Q7.]** Until Q7 is answered the pre-resend check is mandatory.
* If the bank mandates an MQ, the worker's "send" step becomes a publish and the adapter sits behind the consumer. The outbox remains.

**Reversal cost:** low (database choice) to medium (outbox).
