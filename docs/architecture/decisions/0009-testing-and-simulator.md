# 0009 – Testing and the gateway simulator

**Status:** Proposed · **Date:** 2026-10-18

## Decision

| Layer | Tool | What it proves |
|---|---|---|
| Unit | Vitest | Domain rules (V-rules from the product specs), state machine, mapping |
| Catalogue | Vitest | Every message in the catalogue can be built with an empty/sample payload and validates against the example XML structure; regenerating the catalogue changes nothing |
| Contract (golden) | Vitest + stored XML | For each supported message, a fixture payload builds the exact expected XML; XML parses back to the same payload |
| Gateway simulator | Node service | Scripted behaviours: `SUCCEEDED`, `FAILED` with an error text, `UNAVAILABLE`, timeout, duplicate `eBankEventRef`, slow response; and enquiry answers for `SearchMasters` / `RetrieveMasterDetail` |
| Integration | Vitest + real PostgreSQL in a container | Outbox claiming, single flight per master, retries, idempotent re-send |
| End to end | Playwright | A maker completes a form, an approver approves, the simulator receives the expected message, status changes appear |
| Accessibility | axe in Playwright | No serious violations on new screens |
| Static | ESLint, `tsc --noEmit`, import-boundary check, secret scan | Style, types, layering (0005), no secrets |

### Gateway simulator

* A separate package, same message catalogue, behaviour driven by **scenario files** (YAML) and by magic values in the request (for example a `TheirReference` prefix selects a scenario).
* It validates every incoming request against the catalogue and rejects malformed XML with a `FAILED` response that quotes the offending path, so agents get feedback they can act on.
* **Its response bodies are guesses** until real responses are captured from a TI test instance (open question ILC-6). Each guessed behaviour is tagged `ASSUMPTION` in the scenario file. When a real response arrives it replaces the guess and the tag is removed.

### Rules

* A task is not done until its tests exist and pass in CI.
* A bug fix starts with a failing test.
* Tests never call a real TI instance. A separate, manually triggered **gateway smoke suite** may, with its own credentials.

**Reversal cost:** low.
