# Definition of done and review checklist

> Part of the agent guide: [AGENTS.md](../../AGENTS.md).

## Definition of done (every task)

1. The acceptance tests in the task exist and pass; new behaviour has tests; a bug fix has a failing test written first.
2. `./mvnw verify` passes locally: Spotless, compile with Error Prone, unit, contract, web-slice, ArchUnit and catalogue tests. Run `-Pintegration` (both database engines) when the change touches persistence, the worker or the gateway adapter, and `-Pe2e` when it touches templates, forms or security.
3. The generated artefacts are fresh: the catalogue generator and the documentation generator produce no diff.
4. No schema element name appears as a string literal in application code (use the generated constants); no generated file is edited by hand.
5. Every assumption used is tagged `ASSUMPTION(<id>)` in code, named in a test, and listed in the pull request.
6. No secrets, credentials, personal data, or full payloads in code, fixtures, logs or the pull request.
7. Server-side authorisation and corporate scoping are present for every new endpoint or query, with a test that a user from another corporate gets nothing.
8. User-facing text is in `messages*.properties`; templates use no inline scripts or styles and no `th:utext` on user data; new screens pass the automated accessibility check.
9. Documentation updated when behaviour or a decision changed: product spec definitions (`tools/specgen/defs_*.py`), ADRs, `open-questions.md`.
10. The pull request description lists: what changed, which spec sections it implements, assumptions used, tests added, anything deliberately not done.

## Review checklist (reviewer agent or human)

**Correctness against the spec**
- [ ] Message names, element paths, lengths and enumerations match the schema catalogue.
- [ ] Business rules (V-rules) implemented as written, or the difference is justified.
- [ ] State transitions match doc 02, section 5.

**Reliability**
- [ ] Submission goes through the outbox in the same transaction as the approval.
- [ ] Retries reuse `eBankEventRef`; a re-send is preceded by an enquiry.
- [ ] One in-flight message per master.
- [ ] Timeouts are treated as unknown, not failed.
- [ ] SQL is portable (no vendor syntax outside `SqlDialect`); a migration exists for **both** SQL Server and Oracle, with the same version.

**Security and privacy**
- [ ] Corporate scoping and entitlement checks on the server (`CorporateContext`, `@PreAuthorize`); CSRF intact.
- [ ] Maker cannot approve own instruction; approval snapshot is what is sent.
- [ ] No credentials or sensitive data in logs or error messages.
- [ ] Uploads checked for type and size.
- [ ] Amounts are `BigDecimal` with explicit scale; time comes from an injected `Clock`.

**Maintainability**
- [ ] Module boundaries respected (only `gateway-adapter` knows XML; `domain` is free of Spring MVC, JDBC and XML).
- [ ] No duplication of catalogue-derived rules.
- [ ] Tests are deterministic and fast; no real network.

**Scope**
- [ ] The change does only what the task says; extra work is a separate task.
