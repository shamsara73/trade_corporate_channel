# Agent guide – Trade Corporate Channel

This repository designs and (soon) builds a customer-facing **trade finance channel** that sends instructions to the bank's core system, **Finastra Fusion Trade Innovation (TI Plus)**, through the **FTI Gateway**. Read this file first, then the documents it points to. Human owners: the bank's trade finance and architecture teams.

## Status today

| Exists | Does not exist yet |
|---|---|
| Interface files in `sources/` (read-only) · design documents in `docs/` · specification generator in `tools/specgen/` | Application code, database, CI, tests. They are created by task **T0** and the tasks after it ([backlog](docs/agents/task-backlog.md)). |

Commands that work **today**:

```
python3 -I tools/specgen/build.py     # regenerate docs/products/*.md from sources/ and tools/specgen/defs_*.py
python3 -I tools/mappingdoc/build_outbound.py   # regenerate docs/outbound/*.md from sources/ti_outbound/
```

Commands that **T0 must make work** (this is the contract for the scaffold). Java 21 and the Maven Wrapper are the only prerequisites; Docker is needed for integration tests and `dev`.

```
./mvnw verify                          # the everyday check: Spotless, compile (Error Prone), unit, contract, web-slice, ArchUnit and catalogue tests
./mvnw test                            # unit and contract tests only (fast)
./mvnw verify -Pintegration            # + Testcontainers SQL Server and the gateway simulator
./mvnw verify -Pe2e                    # + Playwright end-to-end and accessibility checks
./mvnw spotless:apply                  # fix formatting
./mvnw -pl catalogue -Pgenerate-catalogue generate-resources   # regenerate the catalogue from sources/
docker compose -f infra/compose.yml up -d   # SQL Server + test identity provider + gateway simulator
./mvnw -pl web spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw -pl worker spring-boot:run -Dspring-boot.run.profiles=dev
```

## Read in this order

1. [Architecture and decisions](docs/architecture/README.md) – stack (Java 21, Spring Boot, Thymeleaf), modules, rules. Java and Thymeleaf are confirmed by the project owner; the other choices are **Proposed** until the bank's architects confirm.
2. [Functional specification](docs/02-functional-specification.md) – requirements, lifecycle, integration pattern.
3. [Product specifications](docs/products/README.md) – one per product, with fields, rules, samples; **the spec for the product you are working on**.
4. [Open questions](docs/agents/open-questions.md) – what is unknown, and the **working assumption** to use.
5. [Task template](docs/agents/task-template.md), [definition of done](docs/agents/definition-of-done.md), [backlog](docs/agents/task-backlog.md).
6. [Interface catalogue](docs/01-interface-catalogue.md) for background on the gateway, and [TI outward messages](docs/outbound/README.md) for what TI sends back (first mapping: `TFUTICRT`).

## Ground rules

1. **The specs are the source of truth.** If code and spec disagree, change one on purpose, in the same pull request, and say so. If the spec is wrong or unclear, stop and raise it; do not silently pick.
2. **Never type an element name as a string literal.** Use the generated path constants from the `catalogue` module. Names differ between messages (`eBankMasterRef` / `EBankMasterRef`, `Documentss`, `ApplcableRules`).
3. **Never edit generated files.** That includes the generated catalogue JSON and path constants in `catalogue/` and every file in `docs/products/` except `import-letter-of-credit.md` (hand-written). Change `tools/specgen/defs_*.py`, `shapes.py` or the generator, then regenerate.
4. **Never edit `sources/`.** It is the bank's interface material (`xsd_element_docs/`, `example_xml/`, the gateway HTML, and `ti_outbound/` mappings received from the TI team). A new gateway version or a new mapping document is added or replaced wholesale, in its own pull request, exactly as received.
5. **Only `gateway-adapter` knows XML.** `domain`, `web` and `worker` use the neutral `Payload`. An ArchUnit test fails the build on a violation.
6. **Never call a real TI instance.** Use the gateway simulator. The manual smoke suite is run by humans.
7. **Submissions go through the outbox** in the same transaction as the final approval. Retries reuse `eBankEventRef`; before any re-send, check TI by enquiry. One in-flight message per master.
8. **Authorisation lives on the server.** Every query is scoped to the user's corporate through `CorporateContext`. The customer mnemonic in an outbound message comes from the session, never from a form field or request parameter.
9. **No secrets or sensitive data** in code, fixtures, logs, tests or pull requests. Fixtures use invented data.
10. **Fields the customer must not set** (`Team`, `Step`, `BankCodes`, `ExternalReview`, `ProvisionalFlags` and the others listed in each product spec section 5) are set by configuration only.
11. **Assumptions are explicit.** Use the working assumption in [open-questions](docs/agents/open-questions.md); tag the code `ASSUMPTION(<id>)`; name it in a test; list it in the pull request.
12. **Small, reviewable changes.** One task = one pull request. Do not widen scope; record extra work as a new task.

## How to work on a task

1. Read the task, then the spec sections and decisions it names. Read the open questions it uses.
2. Write the failing acceptance tests first.
3. Implement until they pass; keep to the packages the task allows.
4. Run `./mvnw verify` (after T0). Regenerate the catalogue and docs if you touched their inputs.
5. Self-review against the [review checklist](docs/agents/definition-of-done.md).
6. Open a pull request: what changed, spec sections implemented, assumptions used, tests added, anything not done.

## Stop and ask a human when

* the task needs an answer marked **Blocking** in the open questions,
* the specs and the schema disagree in a way that changes behaviour,
* the change touches security, entitlements, audit, secrets, or the public interface of the gateway adapter beyond the task,
* you cannot make the acceptance tests pass without weakening them,
* an assumption would be unsafe in production, not just uncertain.

Do not ask about things that have a working assumption or a clear convention; decide, tag it, and move on.

## Multiple agents

When several agents work on the same slice, give each a role and keep hand-offs in files, not in conversation:

| Role | Does | Produces | Must not |
|---|---|---|---|
| Planner | Splits a spec section into tasks using the [template](docs/agents/task-template.md) | Task files with acceptance tests | Write production code |
| Builder | Implements one task | Code, tests, pull request | Change another task's files or loosen tests |
| Reviewer | Checks a pull request against the [checklist](docs/agents/definition-of-done.md) and the spec | Findings with file and line | Fix the code in the same pass |
| Verifier | Runs the simulator scenarios and e2e suite; compares requests against product-spec samples | Pass/fail report with evidence | Edit tests to make them pass |

Parallel work is safe across tasks marked ∥ in the backlog because they own different packages. Anything touching `catalogue`, `gateway-adapter` or `persistence` migrations is serial: only one task at a time.

## Conventions

* Java 21; Spotless formatting (Google Java Format); records for value types; sealed interfaces for closed sets; no `null` returns from public methods (use `Optional` or empty collections); constructor injection only; no field injection; no static mutable state.
* Money and amounts: `BigDecimal` with explicit scale and rounding mode; never `double`.
* Time: inject `java.time.Clock`; never call `Instant.now()` directly in domain code.
* Thymeleaf: escape by default; `th:utext` only for trusted, reviewed content; no inline JavaScript or styles (strict CSP); text from `messages*.properties`.
* Names: standard Java naming; messages by their TI code (`TFILCAPP`); schema fields exactly as in the catalogue.
* Payload dates are ISO `yyyy-mm-dd`; times UTC ISO 8601; amounts are decimal strings in payloads.
* Database: **SQL Server**, written so Oracle stays possible. Follow the schema conventions in [ADR 0003](docs/architecture/decisions/0003-database-and-outbox.md): `CHAR(36)` UUID keys, names ≤ 30 characters, no reserved words, no `''` vs `NULL` distinction, UTC from an injected `Clock`, plain SQL with no vendor functions or hints. Anything vendor-specific goes in `SqlServerDialect`. Liquibase changesets are never edited after merge; fix with a new one; add `--rollback` where possible.
* Commit messages: imperative, one line summary, a body saying why.
* Comments explain why, not what. Remove commented-out code.

## Where things go

| You are adding | Put it in |
|---|---|
| A rule from a product spec (V-rule) | `domain`, with the rule id in the test name |
| A new message | No new class: add form definitions and a domain mapping; the catalogue already has it |
| A screen | `web/src/main/resources/forms/<product>/` as a form definition (YAML), rendered by the generic Thymeleaf fragments; text in `messages*.properties` |
| A gateway behaviour for tests | `gateway-simulator/src/main/resources/scenarios/` |
| A database change | A new Liquibase changeset (SQL-formatted) under `persistence/src/main/resources/db/changelog/`, included from the master changelog, with a rollback where possible |
| A required flag, hidden field or label for a message | An overlay in `catalogue/src/main/resources/overlays/` |
| A decision | `docs/architecture/decisions/NNNN-title.md` |
| An unanswered question | `docs/agents/open-questions.md` |
| Business content for a product spec | `tools/specgen/defs_*.py`, then regenerate |
| A new TI outward message mapping | Save as received in `sources/ti_outbound/<NAME>.txt`, add it to `tools/mappingdoc/build_outbound.py` with a crosswalk, regenerate `docs/outbound/` |
