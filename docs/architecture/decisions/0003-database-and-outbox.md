# 0003 – Enterprise relational database (SQL Server and Oracle) and the transactional outbox

**Status:** Proposed (enterprise database chosen by the project owner on 2026-10-19; the portability approach below is a proposal) · **Date:** 2026-10-19

## Context

A trade instruction must reach TI exactly once as far as TI can tell, even if the channel crashes, the gateway is down, or a response is lost. Doc 02 (FR-20 to FR-24, section 7) requires this. The bank prefers an enterprise database, **Microsoft SQL Server or Oracle Database**, and has not said which one the channel will run on (open question Q20).

## Decision

### Engines and approach

* Support **both SQL Server and Oracle** from the first migration, so the choice can be made at deployment, not in code. Integration tests run against **both** (0009).
* Application SQL is a **portable subset** (ANSI joins, `OFFSET ... FETCH`, standard constraints, no vendor hints). The few statements that cannot be portable live behind a small `SqlDialect` interface in `persistence` with one implementation per vendor. Everything else is shared.
* Data access is Spring's **`JdbcClient`** (plain, explicit SQL) with HikariCP and the vendor JDBC driver (`mssql-jdbc`, `ojdbc11`). **No ORM** on the submission and audit paths.
  * Not **jOOQ**: its open-source edition does not cover commercial databases, so it would need a licence.
  * Not **JPA/Hibernate**: possible for simple reads, but explicit SQL keeps outbox and locking behaviour visible and identical across vendors.
* If the bank settles on one engine (Q20), the other implementation, its migrations and its CI job are deleted. Nothing else changes.

### Schema conventions (both engines)

| Topic | Rule |
|---|---|
| Identifiers | Application-generated UUID, stored as `CHAR(36)` canonical text. No identity columns, so no vendor `NEXT VALUE`/`NEXTVAL` syntax. |
| Names | `lower_snake_case`, at most 30 characters (Oracle limit before 12.2), no reserved words (`user`, `comment`, `level`, `number`...). Use `app_user`, `note_text`, etc. |
| Text | SQL Server `NVARCHAR(n)`, Oracle `NVARCHAR2(n)`. Sizes match catalogue limits. Unicode throughout. |
| Large text (payloads, request/response XML) | SQL Server `NVARCHAR(MAX)`; Oracle `CLOB`. JSON payloads carry a validity check: `CHECK (ISJSON(col) = 1)` / `CHECK (col IS JSON)`. |
| Binary (attachments) | SQL Server `VARBINARY(MAX)`; Oracle `BLOB`. Behind the `DocumentStore` port (Q17). |
| Money | `DECIMAL(19,4)` / `NUMBER(19,4)`; currency in its own `CHAR(3)` column; mapped to `BigDecimal`. |
| Instants | UTC. SQL Server `DATETIME2(3)`; Oracle `TIMESTAMP(3)`. The application converts with an injected `Clock`; never use `GETDATE()`, `SYSDATE`, `SYSTIMESTAMP` in SQL. |
| Booleans | SQL Server `BIT`; Oracle `NUMBER(1)` with `CHECK (col IN (0,1))`. |
| Empty string | **Oracle stores `''` as `NULL`; SQL Server does not.** The domain never distinguishes empty from absent (the catalogue is nillable), and tests cover it. |
| Case and collation | SQL Server's default collation is case-insensitive; Oracle comparisons are case-sensitive. Key and reference columns (`e_bank_event_ref`, mnemonics, references) use a **binary collation on SQL Server** (for example `Latin1_General_100_BIN2`) so uniqueness means the same on both. |
| Paging | `ORDER BY ... OFFSET n ROWS FETCH NEXT m ROWS ONLY` (works in SQL Server 2012+ and Oracle 12c+). |
| Isolation | READ COMMITTED. On SQL Server enable `READ_COMMITTED_SNAPSHOT` so readers do not block writers **[Assumption – DBA decision]**. |
| Optimistic locking | A `version` column on mutable rows; `UPDATE ... WHERE id = ? AND version = ?` and check the row count. |
| Deadlocks | Both engines can report one (SQL Server 1205, Oracle ORA-00060). The repository layer retries the transaction a bounded number of times. |

### Migrations

* **Flyway**, one migration folder per vendor: `persistence/src/main/resources/db/migration/sqlserver/` and `.../oracle/`, files named `V<n>__<name>.sql`, applied in order, **never edited after merge**.
* A **parity test** runs both sets and compares the resulting tables, columns, nullability and unique constraints through JDBC metadata. Versions and names must match; only types differ.
* Flyway's edition determines which database versions are supported without a licence. **Check the versions the bank runs against Flyway's current support list before committing**; Liquibase (Apache 2.0) is the fallback and the conventions above do not change.
* The application connects with a **least-privilege role** (DML only). Migrations run with a separate owner account, run from the pipeline or at deployment, not by the web application at start-up in production.

### Outbox and ordering (portable, no vendor locking hints)

* **Transactional outbox.** When the last approval is recorded, the same database transaction inserts an `outbox` row containing the final request XML, `CorrelationId`, `eBankMasterRef`, `eBankEventRef`, the target master and a per-master increasing `seq`.
* **Claiming without `SKIP LOCKED` or lock hints.** The worker finds candidates with a plain `SELECT` (oldest first, `OFFSET 0 ROWS FETCH FIRST n ROWS ONLY`) and claims one with a **conditional update**:

  ```sql
  UPDATE outbox
     SET status = 'CLAIMED', claimed_by = ?, claimed_at = ?, version = version + 1
   WHERE id = ? AND status = 'PENDING' AND version = ?
  ```

  Exactly one worker sees row count 1; the others move on. Leases expire (`claimed_at` + timeout), so a crashed worker's rows are re-claimable.
* **Single flight per master and ordering.** A row is a candidate only if no earlier row of the same master is unfinished:

  ```sql
  AND NOT EXISTS (SELECT 1 FROM outbox o2
                   WHERE o2.master_id = outbox.master_id
                     AND o2.seq < outbox.seq
                     AND o2.status NOT IN ('SENT','REJECTED'))
  ```

  This is plain SQL on both engines. No advisory locks (`sp_getapplock`, `DBMS_LOCK`) are needed.
* **Idempotency.** `e_bank_event_ref` has a unique constraint. A retry reuses it. Before any re-send after a timeout or `UNAVAILABLE`, the worker asks TI (`SearchMasters` / `RetrieveMasterDetail`) whether the event already exists.
* Every attempt writes a `submission` row with the request, response (credentials removed), status and timing.
* `SKIP LOCKED` (Oracle, and SQL Server's `READPAST`) is a possible later optimisation behind `SqlDialect` if claim contention ever shows up. It is not needed at the expected volume and the exact behaviour with row limits differs by engine, so it would be introduced with a benchmark and tests on both.

### Audit

* `audit_event` is append-only: the application role is granted `INSERT` and `SELECT` only; a trigger on each engine additionally rejects `UPDATE`/`DELETE`.

## Alternatives

| Option | Why not (now) |
|---|---|
| PostgreSQL | Earlier proposal; not the bank's preference |
| One engine only | Smaller and simpler, and acceptable once Q20 is answered; supporting both until then avoids blocking |
| Message broker (Kafka, RabbitMQ, MQ) | Adds an operational component and a dual-write problem the outbox avoids |
| Direct call from API after commit | Loses messages on crash between commit and call |

## Consequences

* Each schema change is written twice (once per vendor). The parity test catches drift.
* Delivery is at-least-once to the gateway; exactly-once is achieved by idempotent references plus the pre-resend check. **[Assumption: TI treats a repeated `eBankEventRef` safely – open question Q7.]** Until Q7 is answered the pre-resend check is mandatory.
* If the bank mandates an MQ, the worker's "send" step becomes a publish and the adapter sits behind the consumer. The outbox remains.

**Reversal cost:** low for removing one vendor; medium for adding a third.
