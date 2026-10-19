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
| Q3 | Which `DefinitionID` values exist for `SearchMasters` / `RetrieveMasterDetail`, and what status values do they return? | A single configurable `DefinitionID`; status mapping from TI values to channel states held in a mapping file. **Status values now known from the TI database specification** ([TFUTICRT-database.md](../outbound/TFUTICRT-database.md), section 5): master `STATUS` `NEW`/`LIV`/`CAN`/`EXP`/`BKF`/`PRV`; event `STATUS` `i` in progress, `c` completed, `a` aborted, plus `EVTREFUSED` with a reason. Which of them `DefinitionID` returns is still unknown. | Answered in part | TI team |
| Q4 | Will TI send `Notifications.All` to the channel, and over what transport? | No. Polling only (0008). The receiver exists behind the port but is not wired. | Open | TI team, infrastructure |
| Q5 | How does the channel get reference data (code lists, branches, charge codes, document types)? | Loaded from a versioned JSON file in `testkit/src/main/resources/reference-data` and served through `ReferenceDataPort`. | Open | TI team |
| Q6 | What is mandatory per event in our TI configuration? | The proposed minimum sets in each product spec (section 8.3), as an overlay file. | Open | Trade operations |
| Q7 | Does TI reject or ignore a duplicate `eBankEventRef`? | **Partly answered by the TI database specification:** inbound messages that TI recognises as duplicates are held in `GWYIN` with `ERRMSG` = `D` (key not stated). Keep the worker's enquiry-before-resend check regardless. | Open | TI team |
| Q8 | Attachments: inline `EmbeddedItems` or `DocumentAttachment` first? Size limits? | Inline, up to 10 MB per message and 5 MB per file; configurable. | Open | TI team, architecture |
| Q9 | Approval model: per amount band, per product, per account? | Per corporate and product: a configurable list of approver groups with an amount threshold; 1 to 3 approvers. | Open | Business |
| Q10 | May the channel set `Team`, `Step`, `Product`/`Event`? | Channel sets `Product` per message family from configuration; never sets `Team`, `Step`, `ExchangeNonAutoComplete`. | Open | TI team |
| Q11 | Legacy channel: features, templates and address book to migrate? | None. Out of scope for agents. | Open | Business |
| Q12 | Regulatory: retention, sanctions screening before submission, e-signature, data residency | Retention configurable (default 10 years in the audit table); no channel-side sanctions screening; no e-signature. | Open | Compliance |
| Q13 | Gateway transport (HTTP, MQ, file), endpoint, TLS/mTLS | HTTP POST of the XML to a configured URL; TLS; no mTLS. | Open | TI team, infrastructure |
| Q14 | Hosting platform, orchestrator, CI vendor | Containers (Spring Boot layered images); GitHub Actions; no cloud-specific code. | Open | Architecture |
| Q15 | Bank identity provider product and claims for corporate and role | Standard OIDC through Spring Security; claims `sub`, `email`, `corporates` (list of mnemonics), `roles`. | Open | Security |
| Q16 | Bank design system and brand | None; neutral CSS with design tokens and Thymeleaf fragments that a theme can replace. | Open | Business, design |
| Q17 | Document store and virus scanning | `DocumentStore` port backed by the application database (`VARBINARY(MAX)` / `BLOB`); scanner port with a no-op that rejects nothing, clearly flagged. | Open | Security |
| Q18 | Is the proposed stack acceptable? | **Answered in part (2026-10-19, project owner):** Java 21, Thymeleaf, SQL Server (preferred) and Liquibase. Spring Boot, Maven, htmx and the test tools are proposals; use them unless the bank's architects say otherwise. | Answered (language, UI, database, migrations); rest Open | Architecture |
| Q19 | Java group id and base package (placeholder `com.bank.tradechannel`) | Use the placeholder; keep it in one place (parent POM `groupId` and a single package root) so a rename is mechanical. | Open | Architecture |
| Q20 | SQL Server version and edition, DBA arrangements (logins, filegroups, `READ_COMMITTED_SNAPSHOT`, backups, who runs Liquibase in each environment) | **Answered in part (2026-10-19):** SQL Server preferred; Oracle not built now (ADR 0003). Working assumption: SQL Server 2019 or later, `READ_COMMITTED_SNAPSHOT` on, a DML-only application login, Liquibase run by the pipeline with an owner account. | Open (versions, DBA process) | Architecture, DBA |
| Q21 | Delivery of TI outward messages (transport, wire format, endpoint, authentication, acknowledgement, retry, ordering) | Polling is the only mechanism; the `OutwardMessageReceiver` port exists with a file/stub implementation fed from `testkit` samples; no network receiver. | Open | TI team |
| Q22 | Which events produce outward messages; mappings for each product and event; how refusals are reported (the other gateway templates can be collected with the SQL in [TFUTICRT-database.md](../outbound/TFUTICRT-database.md), section 7) | Only `TFUTICRT` exists. Its absence for other events means status comes from polling. The "issued" `BankComment` literal marks completion. | Open | TI team |
| Q23 | Notation of mapping documents | **Mostly answered by the TI database specification and TI's event field list (2026-10-19):** name suffixes `-U/-R/-S/-M` confirmed; tag = event field code, type = event field type, part = event field part. Remaining details as decoded in [outbound/README](../outbound/README.md); the parser rejects anything outside the grammar. | Answered in part | TI team |
| Q24 | Duplicate tags (`Amount`/`TnxAmount`, `OutstandingAmount`/`AvailableAmt`) and duplicate table names in `TFUTICRT` | Show `Amount` for the undertaking amount and `OutstandingAmount` for the outstanding amount; ignore the duplicates (`TnxAmount`, `AvailableAmt`) and log when they differ. The event field list suggests `AvailableAmt` should use `TAVE` ("Available amount after this event"); also `CMBYC` (probably `CMBY`) and `tAATT` are not in the list. | Open | TI team |
| Q25 | Do bank-defined event fields (245 in the IGT issue event, codes such as `cAAU`, `tAATT`, `evPUO1`) appear in the gateway messages, and under what names? The standard gateway schema does not describe them | Treat them as unknown extra data: do not map them; request the bank's customised gateway message definitions. Supply event field lists for the other events and products too. | Open | TI team |
| Q26 | `TFUTIACK` / `TFUTIREJ` mappings (see [igt-issue-process](../outbound/igt-issue-process.md)) | **Answered in part (2026-10-19):** mappings received; both echo `eBankMasterRef`/`eBankEventRef`; `TFUTIREJ` has no refusal reason field, only fixed text. Open: is a reason available another way? | Answered in part | TI team |
| Q27 | Does `TFUTICRT` fire for channel-initiated issues? | **Answered (2026-10-19):** no, not when TI recognises the channel origin. A `TFUTICRT` therefore creates a TI-initiated transaction. | Answered | TI team |
| Q28 | Which TI product/event produces `TFUTICRT` for an IGT issue, and how do its structured fields correspond to the flat `TFIGTAPP`? | Compare through the IGT event field list; show as-booked values only where a counterpart is confirmed. | Open | TI team |
| Q29 | Are `TFUTIACK` / `TFUTIREJ` sent only on the Issue event? | **Answered in part (2026-10-19):** after a `TFUTICRT`, TI can send further `TFUTIACK` / `TFUTIREJ` so the channel is updated for any transaction; the latest one wins. Other events: assume the same until told otherwise. | Answered in part | TI team |
| ILC-1 | Issuance `Event` code (`ISI`?) and whether the channel sends `Product`/`Event` for `TFILCAPP` | `Product` = `ILC`, `Event` = `ISI`. | Open | TI team |
| ILC-4 | Amount string format | Plain decimal with `.` separator and no thousands separator. | Open | TI team |
| ILC-5 | Which parties may be given as customer mnemonic vs. free text | Applicant: mnemonic. Beneficiary, advising bank: free text. | Open | TI team |
| ILC-6 | Real `ServiceResponse` for `TFILCAPP` and a `FAILED` case | Envelope as in the example `Account.*-RESPONSE.xml`: `Status`, `Details/Error|Warning|Info`, `CorrelationId`. | Open | TI team |
| ILC-7 | How and when TI returns the new LC reference | **Partly answered by TI's `TFUTICRT` mapping (2026-10-19):** for undertakings TI sends `MasterRef` and `EventRef` in its outward message. For the LC, assume a similar outward message; until Q22 is answered, find the reference by enquiry on `eBankMasterRef`. | Open | TI team |
| PROD | Product-specific items | See section 13 of each [product specification](../products/README.md). Same rules apply: assume, tag `ASSUMPTION(<id>)`, test, list. | Open | various |

## Questions that must stop an agent

An agent must stop and ask a human when:

* a task needs an answer marked **Blocking**,
* the specs and the schema disagree and the difference changes behaviour,
* a change would touch security, entitlements, audit, secrets, or the gateway adapter's public interface beyond the task,
* an assumption would put data in front of a different customer than the one signed in, or
* the working assumption is unsafe in production rather than merely uncertain (for example, skipping a check).
