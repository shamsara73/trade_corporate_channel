# 0008 – Status tracking

**Status:** Proposed · **Date:** 2026-10-19

## Decision

Define one port, `StatusSource`, with four implementations that all feed the same state machine in `domain`. The worker runs them with Spring's scheduling and a database lock so that only one instance polls at a time:

1. **Gateway response** – `SUCCEEDED` → `ReceivedByBank`; `FAILED` → `ReturnedToCustomer`; `UNAVAILABLE` → `Retry`.
2. **TI outward messages** – for example `TFUTICRT` (undertaking issued). Received through the adapter's `OutwardMessageReceiver` (0005), matched by `eBankMasterRef` / `eBankEventRef`; carries TI's `MasterRef` and `EventRef` and the values as issued. Treated as the authoritative completion signal for the events it covers, once Q21 and Q22 are answered. See [outbound](../../outbound/README.md).
3. **Poller** – periodic `SearchMasters` and `RetrieveMasterSummaries` / `RetrieveMasterDetail` (with the agreed `DefinitionID`) for open transactions; configurable interval.
4. **Notification receiver** – `Notifications.All` messages, if the bank enables them (open question Q4); de-duplicated by `Category` + `Subcategory` + `Time` + `Payload`.

* **Status values in TI** (from the database specification, see [TFUTICRT-database.md](../../outbound/TFUTICRT-database.md)): a master is `NEW`, `LIV`, `CAN`, `EXP`, `BKF` or `PRV`; an event is `i` in progress, `c` completed or `a` aborted, and can be refused (`EVTREFUSED`, with a reason). The mapping file maps these to the channel states below.
* The state machine and its transitions are those in doc 02, section 5. States after `ReceivedByBank` are **inferred**; the mapping from TI status values to channel states is configuration, filled in when the `DefinitionID` output is known (Q3).
* A transaction that fails to match any TI master after N polls raises an operations alert, not a customer message.
* Every status change is stored with its source and the raw evidence.

## Consequences

Polling alone is a complete first implementation; notifications improve latency later without changing the domain.

**Reversal cost:** low.
