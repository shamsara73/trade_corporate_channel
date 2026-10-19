# Architecture

> Part of the Trade Corporate Channel design set. Functional specification: [02](../02-functional-specification.md) · Product specifications: [products](../products/README.md) · Agent guide: [AGENTS.md](../../AGENTS.md).

**Status: Proposed.** Java 21 and Thymeleaf were chosen by the project owner on 2026-10-19. SQL Server and/or Oracle was chosen as the database on the same date. The rest (Spring Boot, Maven, htmx, test tools, the portability approach) are proposals for the bank's architects to confirm or change. Each decision is a short record with its alternatives and what it costs to reverse, so a change is a documented edit, not a surprise.

## Decisions

| # | Decision | Status |
|---|---|---|
| [0001](decisions/0001-language-and-monorepo.md) | Java 21, Spring Boot, Maven multi-module | Proposed (Java chosen) |
| [0002](decisions/0002-modular-monolith.md) | Modular monolith: web application, worker, gateway simulator | Proposed |
| [0003](decisions/0003-database-and-outbox.md) | SQL Server and Oracle (both supported); Flyway per vendor; transactional outbox for every submission to TI | Proposed (enterprise database chosen) |
| [0004](decisions/0004-schema-catalogue.md) | One generated schema catalogue drives validators, builders and forms | Proposed |
| [0005](decisions/0005-gateway-adapter.md) | All XML and header details live in one gateway adapter module | Proposed |
| [0006](decisions/0006-frontend.md) | Server-rendered Thymeleaf with schema-driven forms | Proposed (Thymeleaf chosen) |
| [0007](decisions/0007-identity-and-entitlements.md) | OIDC login; entitlements and maker-checker enforced on the server | Proposed |
| [0008](decisions/0008-status-tracking.md) | Status from gateway response, polling and notifications, behind one port | Proposed |
| [0009](decisions/0009-testing-and-simulator.md) | Layered tests, ArchUnit, gateway simulator, Playwright | Proposed |
| [0010](decisions/0010-security-audit-observability.md) | Secrets, logging, audit and observability rules | Proposed |

## System context

```mermaid
flowchart LR
    U[Corporate user] --> WEB["Web application<br/>(Spring MVC + Thymeleaf)"]
    WEB --> DB[("SQL Server or Oracle")]
    DB --> WRK[Worker]
    WRK --> ADP[Gateway adapter]
    ADP --> GW[FTI Gateway]
    GW --> TI[TI Plus]
    GW -. notifications .-> WRK
    WEB --> IDP[Bank identity provider]
    WEB --> REF[Reference data port]
    REF -. bank systems .-> HOST[Core banking / limits / FX]
    SIM[Gateway simulator] -. dev and CI instead of .-> GW
```

## Target repository layout

Today the repository holds documentation and the specification generator. The layout below is the target that the first scaffolding task creates (see [task T0](../agents/task-backlog.md)).

```
/
├─ AGENTS.md                         agent guide (CLAUDE.md imports it)
├─ pom.xml                           parent POM: Java 21, dependency management, plugins
├─ mvnw, .mvn/                       Maven Wrapper
├─ docs/                             design documents (this folder, specs, agents)
├─ sources/                          FTI Gateway interface files (read-only input)
├─ tools/specgen/                    existing Python tool: generates docs/products from sources/
├─ catalogue/                        generator, loader, validator, path constants      (0004)
├─ gateway-adapter/                  XML and header handling, GatewayClient            (0005)
├─ domain/                           transactions, instructions, state machine, approvals, V-rules, ports
├─ persistence/                      JDBC repositories, SqlDialect, Flyway migrations per vendor (sqlserver/, oracle/), outbox SQL  (0003)
├─ web/                              Spring Boot app: controllers, Thymeleaf, security (0006, 0007)
├─ worker/                           Spring Boot app: outbox dispatcher, status poller (0003, 0008)
├─ gateway-simulator/                fake FTI Gateway for dev and CI                   (0009)
├─ testkit/                          fixtures, builders, golden XML, sample payloads
├─ e2e/                              Playwright for Java tests
├─ infra/                            Dockerfiles, compose file for local development
└─ .github/workflows/                CI
```

Java base package: `com.bank.tradechannel` (placeholder, open question Q19), with one sub-package per module (`...catalogue`, `...gateway`, `...domain`, `...persistence`, `...web`, `...worker`, `...simulator`).

### Dependency rule

```
web, worker  →  domain, persistence, gateway-adapter, catalogue
persistence  →  domain, catalogue
gateway-adapter → catalogue
domain       →  catalogue          (ports: repositories, GatewayClient, ReferenceDataPort, DocumentStore, Clock)
catalogue    →  nothing
```

**Nothing except `gateway-adapter` knows XML, namespaces or header elements.** ArchUnit tests enforce the rule on every build (see 0009). `domain` defines the interfaces it needs (ports); `persistence`, `gateway-adapter` and the web/worker wiring implement them.

## Module responsibilities

| Module | Owns | Must not |
|---|---|---|
| `catalogue` | Machine-readable description of every message: paths, types, lengths, enumerations, repeating flags, prefix, order; validator; path constants; overlays | Contain business rules |
| `gateway-adapter` | Request/response XML, `RequestHeader`, namespaces, credentials, error mapping, timeouts, the `GatewayClient` implementation | Know about screens, users or approvals |
| `domain` | Transaction and instruction model, state machine, approval rules, business validations (V-rules in the specs), neutral `Payload`, ports | Do I/O or use Spring MVC/XML/JDBC APIs |
| `persistence` | SQL, migrations for each vendor, repositories, the `SqlDialect` interface and its two implementations, outbox claiming, audit writes | Decide business rules; use vendor SQL outside `SqlDialect` |
| `web` | Controllers, Thymeleaf templates and fragments, form definitions, security configuration, entitlement checks | Call the gateway; hold business rules that `domain` doesn't also own |
| `worker` | Outbox dispatch, retries, status polling, notification intake | Serve pages |
| `gateway-simulator` | Scripted gateway behaviour for tests | Be deployed to production |
| `testkit` | Shared test fixtures and builders | Be depended on by production code |

## Not decided here

Which database engine and version is the production target (both are supported until then), cloud or on-premises, orchestrator, CI vendor, the bank's identity provider product, the document store, and the Java base package. They are recorded in [open questions](../agents/open-questions.md).
