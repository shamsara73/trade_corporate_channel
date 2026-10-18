# 0008 – Status tracking

**Status:** Proposed · **Date:** 2026-10-18

## Decision

Define one port, `StatusSource`, with three implementations that all feed the same state machine in `domain`:

1. **Gateway response** – `SUCCEEDED` → `ReceivedByBank`; `FAILED` → `ReturnedToCustomer`; `UNAVAILABLE` → `Retry`.
2. **Poller** – periodic `SearchMasters` and `RetrieveMasterSummaries` / `RetrieveMasterDetail` (with the agreed `DefinitionID`) for open transactions; configurable interval.
3. **Notification receiver** – `Notifications.All` messages, if the bank enables them (open question Q4); de-duplicated by `Category` + `Subcategory` + `Time` + `Payload`.

* The state machine and its transitions are those in doc 02, section 5. States after `ReceivedByBank` are **inferred**; the mapping from TI status values to channel states is configuration, filled in when the `DefinitionID` output is known (Q3).
* A transaction that fails to match any TI master after N polls raises an operations alert, not a customer message.
* Every status change is stored with its source and the raw evidence.

## Consequences

Polling alone is a complete first implementation; notifications improve latency later without changing the domain.

**Reversal cost:** low.
