# Trade Corporate Channel

A customer-facing trade finance channel: corporates enter trade instructions (finance, guarantees, letters of credit …), approve them, and the channel sends them to the bank's core trade system, **Finastra Trade Innovation (TI Plus)**, over IBM MQ. TI's replies and completion messages come back the same way.

## Where things are

| Folder | What it holds | Who edits it |
|---|---|---|
| [`app/`](app/) | **The application.** One Spring Boot app and its Maven build; packages for web pages, background jobs, the TI gateway, domain, persistence. Layout: [architecture](docs/architecture/README.md#repository-layout). | Developers and agents, task by task |
| [`docs/`](docs/README.md) | **The documents.** Specifications, architecture decisions, TI interface knowledge, agent backlog and open questions. | Hand-written, except the generated pages noted in each file |
| [`sources/`](sources/) | **The bank's material**, kept exactly as received: gateway documentation, XSD jars, TI mappings, SDK guides, database specification. | Never edited; replaced wholesale |
| [`tools/`](tools/) | Python generators that build parts of `docs/` from `sources/`. | When a generated document needs to change |
| [`infra/`](infra/) | `compose.yml` for local SQL Server and IBM MQ. | – |
| [`poc/`](poc/mq-channel/README.md) | A throw-away IBM MQ proof of concept. Not part of the app. | – |

## Start here

* **Understand the product:** [functional specification](docs/02-functional-specification.md), then the [product specification](docs/products/README.md) you work on (first slice: [Finance Standalone](docs/products/financing.md)).
* **Understand how TI talks to us:** [working samples](docs/reference/working-samples.md) (real messages), [TI SDK guides](docs/reference/ti-sdk-guides.md), [outward messages](docs/outbound/README.md).
* **Build the app:** [AGENTS.md](AGENTS.md) (rules and commands), [architecture](docs/architecture/README.md), [task backlog](docs/agents/task-backlog.md), [open questions](docs/agents/open-questions.md).

## Build

Java 21 is the only prerequisite. Everything runs in `app/`:

```
cd app
./mvnw verify                                        # format, compile, unit, web and architecture tests
./mvnw verify -Pintegration                          # + tests that need Docker (SQL Server)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # http://localhost:8080
```
