# Definition of done and review checklist

> Part of the agent guide: [AGENTS.md](../../AGENTS.md).

## Definition of done (every task)

1. The acceptance tests in the task exist and pass; new behaviour has tests; a bug fix has a failing test written first.
2. `pnpm check` passes locally: format, lint, `tsc --noEmit`, import-boundary check, unit and contract tests, secret scan.
3. The generated artefacts are fresh: `pnpm catalogue:build` and the documentation generator produce no diff.
4. No element name is hand-typed in application code; no generated file is edited by hand.
5. Every assumption used is tagged `ASSUMPTION(<id>)` in code, named in a test, and listed in the pull request.
6. No secrets, credentials, personal data, or full payloads in code, fixtures, logs or the pull request.
7. Server-side authorisation and corporate scoping are present for every new endpoint or query, with a test that a user from another corporate gets nothing.
8. User-facing text is in the message catalogue; new screens pass the automated accessibility check.
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

**Security and privacy**
- [ ] Corporate scoping and entitlement checks on the server.
- [ ] Maker cannot approve own instruction; approval snapshot is what is sent.
- [ ] No credentials or sensitive data in logs or error messages.
- [ ] Uploads checked for type and size.

**Maintainability**
- [ ] Package boundaries respected (only `gateway-adapter` knows XML).
- [ ] No duplication of catalogue-derived rules.
- [ ] Tests are deterministic and fast; no real network.

**Scope**
- [ ] The change does only what the task says; extra work is a separate task.
