# Open questions and working assumptions

> Part of the agent guide: [AGENTS.md](../../AGENTS.md). Questions come from [02, section 11](../02-functional-specification.md), the product specifications' open items, and the [architecture decisions](../architecture/README.md).

Agents cannot ask the TI team. For each question below, agents use the **working assumption** until a human records an answer here. Rules:

1. Implement the assumption behind a configuration value or a single named constant, never scattered.
2. Mark the code with a comment `ASSUMPTION(Qn)` at the place it matters.
3. Add a test that names the assumption, so the test shows what breaks when the answer differs.
4. List every assumption used in the pull request description.
5. When a question is answered: update the row (status `Answered`, the answer, date, who), change the code and tests, and remove the `ASSUMPTION(Qn)` tags in the same pull request.

Status: **Open** (no answer), **Answered** (human recorded the answer), **Blocking** (agents must not guess; stop and ask).

| ID | Question | Working assumption for agents | Status | Owner |
|---|---|---|---|---|
| Q1 | Which services does the channel call? Are `Customer`, `Account`, `Limit`, `BusinessSupport`, `StandingSettlements` TI → host only? | Yes, TI → host only. The channel gets customers, accounts and limits from a `ReferenceDataPort` with a stub implementation backed by test data. | Open | TI team |
| Q2 | Meaning and values of `ReplyFormat`, `ReplyTarget`, `NoRepair`, `NoOverride`, `TransactionControl`, `GroupingId`; credential mode (`Name`/`Password` vs. `Certificate`/`Digest`) | Header values come from configuration; defaults copy the example files (`ReplyFormat` = `FULL`, `NoRepair` = `Y`, `NoOverride` = `Y`, `TransactionControl` = `NONE`); password credentials. | Open | TI team |
| Q3 | Which `DefinitionID` values exist for `SearchMasters` / `RetrieveMasterDetail`, and what status values do they return? | A single configurable `DefinitionID`; status mapping from TI values to channel states held in a mapping file with plausible placeholders (`LIV`, `CAN`, `EXP`, `BKF` from the schema). | Open | TI team |
| Q4 | Will TI send `Notifications.All` to the channel, and over what transport? | No. Polling only (0008). The receiver exists behind the port but is not wired. | Open | TI team, infrastructure |
| Q5 | How does the channel get reference data (code lists, branches, charge codes, document types)? | Loaded from a versioned JSON file in `testkit/src/main/resources/reference-data` and served through `ReferenceDataPort`. | Open | TI team |
| Q6 | What is mandatory per event in our TI configuration? | The proposed minimum sets in each product spec (section 8.3), as an overlay file. | Open | Trade operations |
| Q7 | Does TI reject or ignore a duplicate `eBankEventRef`? | It does not. The worker always checks TI by enquiry before any re-send. | Open | TI team |
| Q8 | Attachments: inline `EmbeddedItems` or `DocumentAttachment` first? Size limits? | Inline, up to 10 MB per message and 5 MB per file; configurable. | Open | TI team, architecture |
| Q9 | Approval model: per amount band, per product, per account? | Per corporate and product: a configurable list of approver groups with an amount threshold; 1 to 3 approvers. | Open | Business |
| Q10 | May the channel set `Team`, `Step`, `Product`/`Event`? | Channel sets `Product` per message family from configuration; never sets `Team`, `Step`, `ExchangeNonAutoComplete`. | Open | TI team |
| Q11 | Legacy channel: features, templates and address book to migrate? | None. Out of scope for agents. | Open | Business |
| Q12 | Regulatory: retention, sanctions screening before submission, e-signature, data residency | Retention configurable (default 10 years in the audit table); no channel-side sanctions screening; no e-signature. | Open | Compliance |
| Q13 | Gateway transport (HTTP, MQ, file), endpoint, TLS/mTLS | HTTP POST of the XML to a configured URL; TLS; no mTLS. | Open | TI team, infrastructure |
| Q14 | Hosting platform, orchestrator, CI vendor | Containers (Spring Boot layered images); GitHub Actions; no cloud-specific code. | Open | Architecture |
| Q15 | Bank identity provider product and claims for corporate and role | Standard OIDC through Spring Security; claims `sub`, `email`, `corporates` (list of mnemonics), `roles`. | Open | Security |
| Q16 | Bank design system and brand | None; neutral CSS with design tokens and Thymeleaf fragments that a theme can replace. | Open | Business, design |
| Q17 | Document store and virus scanning | `DocumentStore` port backed by PostgreSQL; scanner port with a no-op that rejects nothing, clearly flagged. | Open | Security |
| Q18 | Is the proposed stack acceptable? | **Answered in part (2026-10-19, project owner):** Java 21 and Thymeleaf. Spring Boot, Maven, PostgreSQL, Flyway, htmx and the test tools are proposals; use them unless the bank's architects say otherwise. | Answered (language, UI); rest Open | Architecture |
| Q19 | Java group id and base package (placeholder `com.bank.tradechannel`) | Use the placeholder; keep it in one place (parent POM `groupId` and a single package root) so a rename is mechanical. | Open | Architecture |
| ILC-1 | Issuance `Event` code (`ISI`?) and whether the channel sends `Product`/`Event` for `TFILCAPP` | `Product` = `ILC`, `Event` = `ISI`. | Open | TI team |
| ILC-4 | Amount string format | Plain decimal with `.` separator and no thousands separator. | Open | TI team |
| ILC-5 | Which parties may be given as customer mnemonic vs. free text | Applicant: mnemonic. Beneficiary, advising bank: free text. | Open | TI team |
| ILC-6 | Real `ServiceResponse` for `TFILCAPP` and a `FAILED` case | Envelope as in the example `Account.*-RESPONSE.xml`: `Status`, `Details/Error|Warning|Info`, `CorrelationId`. | Open | TI team |
| ILC-7 | How and when TI returns the new LC reference | Not in the response; found by enquiry on `eBankMasterRef`. | Open | TI team |
| PROD | Product-specific items | See section 13 of each [product specification](../products/README.md). Same rules apply: assume, tag `ASSUMPTION(<id>)`, test, list. | Open | various |

## Questions that must stop an agent

An agent must stop and ask a human when:

* a task needs an answer marked **Blocking**,
* the specs and the schema disagree and the difference changes behaviour,
* a change would touch security, entitlements, audit, secrets, or the gateway adapter's public interface beyond the task,
* an assumption would put data in front of a different customer than the one signed in, or
* the working assumption is unsafe in production rather than merely uncertain (for example, skipping a check).
