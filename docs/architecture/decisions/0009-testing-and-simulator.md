# 0009 – Testing and the gateway simulator

**Status:** Proposed · **Date:** 2026-10-19

## Decision

| Layer | Tool | What it proves |
|---|---|---|
| Unit | JUnit 5, AssertJ, Mockito | Domain rules (V-rules from the product specs), state machine, mapping |
| Catalogue | JUnit 5 | Every message in the catalogue builds XML from a sample payload and matches the structure of its example XML; regenerating the catalogue changes nothing; Java and Python parsers agree (0004) |
| Contract (golden) | JUnit 5, XMLUnit, stored XML | For each supported message, a fixture payload builds exactly the expected XML; XML parses back to the same payload |
| Web slice | Spring MockMvc, `@WebMvcTest` | Controllers, binding, CSRF, authorisation, error display, per-corporate scoping |
| Gateway simulator | Test code in the application (embedded JMS broker) | Scripted behaviours: `SUCCEEDED`, `FAILED` with an error text, `UNAVAILABLE`, timeout, duplicate `eBankEventRef`, slow response; enquiry answers for `SearchMasters` / `RetrieveMasterDetail` |
| Integration | Spring Boot Test, **Testcontainers for SQL Server**, the simulator | Outbox claiming, single flight per master, retries, idempotent re-send, Liquibase changelogs applied from scratch and `validate` clean, schema naming conventions (30 characters, no reserved words), empty-string/null and case-sensitivity behaviour, deadlock retry |
| Architecture | **ArchUnit**, in the application's tests (`architecture/`) | Package boundaries (0002, 0005): only `gateway` uses XML APIs; `domain` depends on nothing but `catalogue`; `web` never talks to TI; no controller reads a corporate from request input. A source scan (`VendorSqlTest`) fails on vendor SQL outside `SqlServerDialect` (0003) |
| End to end | **Playwright for Java** | A maker completes a form, an approver approves, the simulator receives the expected message, status changes appear |
| Accessibility | axe-core injected by Playwright | No serious violations on new screens |
| Static | Spotless, Error Prone, OWASP dependency check, gitleaks | Format, common bugs, vulnerable dependencies, secrets |

### Maven wiring

* `./mvnw verify` (in `app/`) is the everyday check: Spotless, compile with Error Prone, unit, contract, web-slice, ArchUnit and catalogue tests. It needs no Docker.
* `./mvnw verify -Pintegration` adds Testcontainers and simulator tests (needs Docker). The SQL Server container image (Microsoft Container Registry) requires accepting the licence (EULA) in the test configuration and runs the Developer edition, which is licensed for non-production use. It is slow to start, which is why it is outside the everyday check.
* Playwright end-to-end tests are added with task T10 (a profile `e2e` then).
* CI runs both; pull requests cannot merge unless they pass.

### Gateway simulator

* Test code inside the application (0002), using the same `catalogue` package. If a running fake TI is needed for demos, the same code is enabled by a `simulator` Spring profile; it is never active in production. Behaviour is driven by **scenario files** (YAML) and by magic values in the request (for example a `TheirReference` prefix selects a scenario).
* The application's real `JmsGatewayClient` talks to it through an embedded JMS broker with the same queue names as production (updated 2026-10-22; see 0005), so tests exercise the production client code.
* It validates every incoming request against the catalogue and answers a malformed one with `FAILED` quoting the offending path, so agents get feedback they can act on.
* **Its response bodies are guesses** until real responses are captured from a TI test instance (open question ILC-6). Each guessed behaviour is tagged `ASSUMPTION` in its scenario file. When a real response arrives it replaces the guess and the tag is removed.

### Rules

* A task is not done until its tests exist and pass in CI.
* A bug fix starts with a failing test.
* Tests never call a real TI instance. A separate, manually triggered **gateway smoke suite** may, with its own credentials.
* Tests are deterministic: fixed clocks (`Clock` injected), no sleeping, no network except the simulator and Testcontainers.

**Reversal cost:** low.
