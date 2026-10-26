# Transactions and events – Finance Standalone

> Part of the [architecture](README.md). Implemented in task P2 (see the [backlog](../agents/task-backlog.md)). Tables: `app/src/main/resources/db/changelog/changes/001-fsa-transactions.sql`. Rules: `domain/finance/FsaRules.java`.

A **transaction** is one TI master (one finance). An **event** is one instruction on it, each with its own form and its own message to TI. Finance Standalone (product `FSA`) has two event types:

| Event | Form | Message to TI | May start when |
|---|---|---|---|
| `CREATE` | `/finance/new` | `TFFINNEW` | always (it starts the transaction) |
| `REPAY` | `/transactions/{id}/repay` | `TFFSARPY` (Q38) | the `CREATE` event is `COMPLETED`, the finance is `LIVE`, something is outstanding and no other event is with TI |

## Life cycle

```
Event:        PENDING ──dispatcher──▶ SENT ──ServiceResponse SUCCEEDED──▶ RECEIVED ──outward ACK──▶ COMPLETED
                 │                     │                                     │      ──outward REJ──▶ REJECTED
                 └─ 5 failed sends ─▶ FAILED ◀──── ServiceResponse FAILED ───┘
              (COMPLETED and REJECTED may replace each other: TI's last word wins. Nothing moves back.)

Transaction:  PENDING ──CREATE completed──▶ LIVE ──REPAY completed, nothing outstanding──▶ REPAID
                 │──CREATE refused──▶ REJECTED
                 └──CREATE failed───▶ FAILED
```

`PENDING`, `SENT` and `RECEIVED` are "in flight": while one event is in flight no other may start (ground rule 7).

## Rules

| Id | Rule | Message key |
|---|---|---|
| FSA-1 | A repayment starts only after TI completed the `CREATE` event and while the finance is `LIVE` with something outstanding | `fsa.repay.notYet`, `fsa.repay.closed`, `fsa.repay.nothingOutstanding` |
| FSA-2 | One event in flight per transaction; starting a repayment takes the transaction's row version, so two simultaneous repayments cannot both pass | `fsa.repay.inFlight`, `fsa.repay.concurrent` |
| FSA-3 | The principal is in the finance currency, not more than what is outstanding; a `FINAL` repayment repays all of it | `fsa.repay.currency`, `fsa.repay.tooMuch`, `fsa.repay.finalMustClear` |
| FSA-4 | The value date is not before the start date | `fsa.repay.beforeStart` |

## Tables (SQL Server, ADR 0003 conventions)

| Table | One row per | Key columns |
|---|---|---|
| `trade_txn` | transaction (TI master) | `e_bank_master_ref` (unique), `customer_mnemonic` (every query is scoped by it), `status`, `finance_amount`, `outstanding_amount`, `maturity_date`, `ti_master_ref` (from TI's ACK), `row_version` |
| `trade_event` | event | `txn_id`, `event_type`, `event_seq` (unique per transaction), `e_bank_event_ref` (unique), `status`, `ti_event_ref`, `bank_comment`, `error_text`, `completed_at`, `row_version` |
| `fsa_create_det` | `CREATE` event | what the customer asked for (the `TFFINNEW` fields) |
| `fsa_repay_det` | `REPAY` event | what the customer asked for (the `TFFSARPY` fields) |
| `outbox_msg` | delivery of an event | `their_ref` (unique; = `CorrelationId`), `operation`, `status` (`PENDING`/`SENDING`/`SENT`/`FAILED`), `attempts`, `next_attempt_at`, `claimed_at`, the message as sent, TI's `ServiceResponse` status and details |
| `inbound_msg` | message from TI, matched or not | `kind`, `operation`, `message_name`, `correlation_id`, the echoed references, `event_id` if matched, `outcome` (`MATCHED`/`DUPLICATE`/`UNMATCHED`/`UNREADABLE`), the message as received |
| `ti_document` | document TI attached | `event_id`, `document_id`, `mime_type`, `content` (downloadable by the owning customer only) |

## How a message travels

1. The form saves the transaction (for `CREATE`), the event, its details and an `outbox_msg` row in **one database transaction**. The page never talks to TI.
2. The outbox job (every 2 s) claims due rows with a conditional update on `row_version` and a 2-minute lease, writes the message (`gateway`) and puts it on TI's queue (or in a file). A transport failure is retried after 30 s, 1, 2, 4 minutes; after 5 attempts the event is `FAILED`.
3. TI's `ServiceResponse` is matched by `CorrelationId` = `TheirRef`: `SUCCEEDED` → `RECEIVED`, otherwise `FAILED` with TI's details.
4. TI's outward message (`TFFINACK`, Service `GATEWAY`) is matched by `eBankMasterRef` + `eBankEventRef`: ACK → `COMPLETED` with TI's references, booked amount, outstanding (minor units, Q35), maturity and documents; REJ → `REJECTED`. The kind is read from the last three letters of the name (Q39).
5. Every message from TI is stored in `inbound_msg`, including duplicates and those that match nothing.

Transports: `channel.gateway.mode=mq` (send queue; one listener per reply/outward queue) or `file` (`sent-messages/` out, `ti-inbox/` in). `channel.gateway.simulator.enabled=true` (file mode only, demos) answers every message like TI did in the bank's test.

## Not yet

Approvals (T8), the catalogue (T1), enquiry before a re-send (Q40, T9), status polling (T11), TI-created transactions (`CRT`), interest-only repayments (the principal must be above zero).
