# 0003 – SQL Server, Liquibase and the transactional outbox

**Status:** Proposed (SQL Server as the preferred engine and Liquibase for migrations stated by the project owner on 2026-10-19; the rest is a proposal) · **Date:** 2026-10-19

## Context

A trade instruction must reach TI exactly once as far as TI can tell, even if the channel crashes, the gateway is down, or a response is lost. Doc 02 (FR-20 to FR-24, section 7) requires this. The bank prefers an enterprise database: **Microsoft SQL Server, preferably**; Oracle was mentioned as the alternative. It prefers **Liquibase** for schema migrations.

An earlier draft supported SQL Server and Oracle together from the first migration. With SQL Server named as the preference, that doubling was dropped (see "Oracle later").

## Decision

### Engine and data access

* **Microsoft SQL Server** (a currently supported version; working assumption 2019 or later, Q20) is the only datastore for channel state.
* Data access is Spring's **`JdbcClient`** (plain, explicit SQL) with HikariCP and the Microsoft JDBC driver (`mssql-jdbc`). **No ORM** on the submission and audit paths, so locking and outbox behaviour stay visible.
  * Not **jOOQ**: its open-source edition does not cover commercial databases, so it would need a licence.
  * Not **JPA/Hibernate**: possible for simple reads later; explicit SQL is clearer for outbox and audit.
* Vendor-specific SQL is kept behind a small `SqlDialect` interface in `persistence` (one implementation, `SqlServerDialect`, today). Everything else is plain, portable SQL. This costs almost nothing now and keeps Oracle possible.

### Schema conventions

| Topic | Rule |
|---|---|
| Identifiers | Application-generated UUID, stored as `CHAR(36)` canonical text. No identity columns or sequences. |
| Names | `lower_snake_case`, at most 30 characters, no reserved words in either SQL Server or Oracle (`user`, `comment`, `level`, `number`...). Use `app_user`, `note_text`, etc. A test checks this from the database metadata. |
| Text | `NVARCHAR(n)`, sizes from the catalogue limits. Unicode throughout. |
| Large text (payloads, request/response XML) | `NVARCHAR(MAX)`. JSON payloads carry `CHECK (ISJSON(col) = 1)`. |
| Binary (attachments) | `VARBINARY(MAX)` behind the `DocumentStore` port (Q17). |
| Money | `DECIMAL(19,4)`, currency in its own `CHAR(3)` column; mapped to `BigDecimal`. |
| Instants | UTC `DATETIME2(3)`. The application writes them from an injected `Clock`; SQL never calls `GETDATE()`/`SYSDATETIME()`. |
| Booleans | `BIT`. |
| Empty string | The domain never distinguishes `''` from `NULL` (the catalogue is nillable). SQL Server stores them differently and Oracle does not; this rule keeps Oracle possible. |
| Case and collation | SQL Server's default collation is case-insensitive. Key and reference columns (`e_bank_event_ref`, mnemonics, references) use a **binary collation** (for example `Latin1_General_100_BIN2`) so uniqueness is case-sensitive, matching the gateway's references. |
| Paging | `ORDER BY ... OFFSET n ROWS FETCH NEXT m ROWS ONLY` (standard, no `TOP` in application SQL). |
| Isolation | READ COMMITTED with **`READ_COMMITTED_SNAPSHOT` on** so readers do not block writers **[Assumption – DBA decision]**. |
| Optimistic locking | A `version` column on mutable rows; `UPDATE ... WHERE id = ? AND version = ?` and check the row count. |
| Deadlocks | SQL Server error 1205 is retried by the repository layer a bounded number of times. |

### Migrations with Liquibase

* **Liquibase Community** (Apache 2.0). Changelogs are **SQL-formatted** files (`--liquibase formatted sql`) so DBAs can read them, under `app/src/main/resources/db/changelog/`, included from one master file `db.changelog-master.yaml`.
* One **changeset per logical change**, `id` = the task or ticket, `author` = person or agent. A changeset **is never edited after merge** (Liquibase checksums it); a mistake is fixed by a new changeset.
* Every changeset that can be undone declares a `--rollback` block; changesets that cannot be undone say so.
* Migrations are applied by the **pipeline or the DBA** with a separate owner account (`liquibase update`), not by the application at start-up in production (`spring.liquibase.enabled=false`). In development and tests they are applied automatically by Spring Boot's Liquibase integration.
* For review, the pipeline produces the SQL that would run (`updateSQL`) and attaches it to the pull request; `liquibase validate` runs in CI.
* The application connects with a **least-privilege login** (DML only on application tables).
* Changelog contexts are not used to select data per environment; reference data comes from the `ReferenceDataPort`, not from migrations.

### Outbox and ordering (plain SQL, no lock hints)

* **Transactional outbox.** When the last approval is recorded, the same database transaction inserts an `outbox` row containing the final request XML, `CorrelationId`, `eBankMasterRef`, `eBankEventRef`, the target master and a per-master increasing `seq`.
* **Claiming without `READPAST` or `UPDLOCK`.** The outbox dispatcher finds candidates with a plain `SELECT` (oldest first, `OFFSET 0 ROWS FETCH FIRST n ROWS ONLY`) and claims one with a **conditional update**:

  ```sql
  UPDATE outbox
     SET status = 'CLAIMED', claimed_by = ?, claimed_at = ?, version = version + 1
   WHERE id = ? AND status = 'PENDING' AND version = ?
  ```

  Exactly one dispatcher sees row count 1; the others move on. Leases expire (`claimed_at` + timeout), so a crashed dispatcher's rows can be claimed again.
* **Single flight per master and ordering.** A row is a candidate only if no earlier row of the same master is unfinished:

  ```sql
  AND NOT EXISTS (SELECT 1 FROM outbox o2
                   WHERE o2.master_id = outbox.master_id
                     AND o2.seq < outbox.seq
                     AND o2.status NOT IN ('SENT','REJECTED'))
  ```

  No application locks (`sp_getapplock`) are needed.
* **Idempotency.** `e_bank_event_ref` has a unique constraint. A retry reuses it. Before any re-send after a timeout or `UNAVAILABLE`, the dispatcher asks TI (`SearchMasters` / `RetrieveMasterDetail`) whether the event already exists.
* Every attempt writes a `submission` row with the request, response (credentials removed), status and timing.
* `READPAST`/`UPDLOCK` hints are a possible later optimisation inside `SqlServerDialect` if claim contention ever appears. They are not needed at the expected volume; they would be introduced with a benchmark and tests.

### Audit

* `audit_event` is append-only: the application login has `INSERT` and `SELECT` only, and a trigger rejects `UPDATE`/`DELETE`.

### Oracle later

Oracle is **not** built now: no Oracle migrations, no Oracle container in tests, no `OracleDialect`. The conventions above (30-character names, no reserved words, no empty-string meaning, UUID text keys, no vendor functions outside the dialect, `Clock` for time) are kept so that adding it is a bounded task: an `OracleDialect`, an Oracle changelog directory, an Oracle Testcontainers job, and a metadata parity test. If the bank later requires Oracle, create that task then.

## Alternatives

| Option | Why not |
|---|---|
| SQL Server and Oracle together from the start | Doubles migrations and test time before the bank has chosen; rejected once SQL Server was named the preference |
| PostgreSQL | Earlier proposal; not the bank's preference |
| Flyway | Replaced by the bank's preference for Liquibase |
| Message broker (Kafka, RabbitMQ, MQ) | Adds an operational component and a dual-write problem the outbox avoids |
| Direct call after commit | Loses messages on crash between commit and call |

## Consequences

* Delivery is at-least-once to the gateway; exactly-once is achieved by idempotent references plus the pre-resend check. **[Assumption: TI treats a repeated `eBankEventRef` safely – open question Q7.]** Until Q7 is answered the pre-resend check is mandatory.
* If the bank mandates an MQ, the dispatcher's "send" step becomes a publish and the adapter sits behind the consumer. The outbox remains.

**Reversal cost:** low for adding Oracle (bounded task); medium for changing the outbox.
