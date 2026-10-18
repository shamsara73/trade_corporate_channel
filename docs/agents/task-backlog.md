# Task backlog – first vertical slice (Import LC issuance)

> Part of the agent guide: [AGENTS.md](../../AGENTS.md). Format: [task template](task-template.md). Spec: [Import Letter of Credit](../products/import-letter-of-credit.md).

The slice is ordered so each task has a test that proves it before the next starts. Tasks marked ∥ can run in parallel once their dependencies are done. Only T0 is blocked on a human answer (Q18).

| Task | Goal | Depends on | Key acceptance tests |
|---|---|---|---|
| **T0** Scaffold | pnpm workspace with the packages in [the architecture](../architecture/README.md); TypeScript strict; lint, format, test runner; CI that runs `pnpm check`; import-boundary check; secret scan; `.nvmrc`; container-based PostgreSQL for tests | Q18 | `pnpm check` passes on an empty slice; a deliberate boundary violation fails CI; the commands in [AGENTS.md](../../AGENTS.md) all work |
| **T1** Schema catalogue | Port the parser from `tools/specgen/specgen_lib.py` into `packages/schema-catalogue`; emit the JSON catalogue for all 181 messages (including prefix and order from the example XML); loader and path-typed API | T0 | Catalogue for `TFILCAPP` has 110 top-level elements, matching `docs/products/import-letter-of-credit.md` section 5.4; regenerate = no diff; enumerations of `Confirmation`, `ApplcableRules` present; a malformed source line fails the build |
| **T2** Validators ∥ | Generate Zod validators from the catalogue; add overlay support (required flags, hidden fields, labels) | T1 | Over-length `Goods` rejected; bad `Confirmation` rejected; overlay marks `Applicant` required; error messages name the path |
| **T3** Gateway adapter | Build `ServiceRequest` XML from a neutral payload and parse `ServiceResponse`; configurable header; `GatewayClient` interface | T1 | Golden test: the ILC sample payload in product spec section 9 builds XML equal to the stored fixture; round trip; response with `Status` FAILED and `Details/Error` parsed; header values come from config |
| **T4** Gateway simulator ∥ | Fake gateway that validates requests against the catalogue and plays scenarios | T1, T3 | Scenario files for `SUCCEEDED`, `FAILED`, `UNAVAILABLE`, timeout, duplicate event; an invalid request returns `FAILED` quoting the bad path |
| **T5** Database and outbox | SQL migrations for transactions, instructions, approvals, submissions, outbox, audit; outbox claiming with `SKIP LOCKED`; per-master advisory lock | T0 | Two workers never claim the same row; events for one master are sent in order; unique `eBankEventRef`; audit table rejects update and delete |
| **T6** Domain core | Transaction, instruction and approval model; state machine of doc 02 section 5; the ILC V-rules (V1–V9) | T2 | Every transition in the state diagram has a test; maker cannot approve own instruction; V1 to V9 each have a positive and negative test |
| **T7** Identity and entitlements | OIDC login, corporate scoping, entitlements, approval routes | T5, T6 | User of corporate A cannot read corporate B's transaction; amount above limit needs an extra approver; unauthenticated call rejected |
| **T8** API for ILC | Endpoints: create/update draft, validate, submit for approval, approve/return/reject, list/detail; documents upload | T5, T6, T7 | Full draft→approved flow over HTTP; approved snapshot is immutable; invalid draft returns path-named errors |
| **T9** Worker dispatch | Outbox dispatcher using the gateway client; response handling per product spec section 10; retries with back-off; enquiry-before-resend | T3, T4, T5, T8 | `SUCCEEDED` → `ReceivedByBank`; `FAILED` → returned to maker with error text; `UNAVAILABLE` → retry, and after a simulated lost response no second event is created |
| **T10** Web: ILC form ∥ | Schema-driven wizard for `TFILCAPP` (screens table), draft/resume, review and submit, approver inbox | T2, T8 | Playwright: maker completes the form with the spec sample, approver approves, simulator receives the expected message; accessibility check passes; coded values shown as labels |
| **T11** Status tracking | Poller via `SearchMasters` / `RetrieveMasterDetail`; mapping file; status timeline in the web app | T4, T9 | Simulator moves an LC to issued; channel shows "Issued" with reference; unmatched transaction raises an alert |
| **T12** ILC servicing | Amend (`TFILCAMN`), cancel (`TFILCCAN`), claim response (`TFILCPYR`), correspondence (`TFILCCOR`), documents (`TFRCVDOC`) | T10, T11 | Each message built from a form and accepted by the simulator; claim response upper-cases `AuthorisationInstructions`; amend form pre-filled from master detail |

After T12, repeat the pattern per product in the order of [doc 02, section 9](../02-functional-specification.md). A new product normally needs: form definitions, domain rules from its spec, simulator scenarios, and tests. It should not need new infrastructure.

## Before production (not agent tasks)

Answers to Q1–Q17, capture of real gateway responses, security review, performance test, bank sign-off, operational runbook.
