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
* **Understand how TI talks to us:** [sample messages](docs/samples/README.md) (request, reply and response per event), [working samples](docs/reference/working-samples.md) (what the bank's test showed), [TI SDK guides](docs/reference/ti-sdk-guides.md), [outward messages](docs/outbound/README.md).
* **Build the app:** [AGENTS.md](AGENTS.md) (rules and commands), [architecture](docs/architecture/README.md), [task backlog](docs/agents/task-backlog.md), [open questions](docs/agents/open-questions.md).

## Build

Java 21 is the only prerequisite. Everything runs in `app/`:

```
cd app
./mvnw verify                                        # format, compile, unit, web and architecture tests
./mvnw verify -Pintegration                          # + tests that need Docker (SQL Server)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,h2   # http://localhost:8880
```

## Try Finance Standalone

The first product is Finance Standalone: a **transaction** with a `CREATE` event (`TFFINNEW`, the message the bank's TI accepted, see [working samples](docs/reference/working-samples.md)) and, once TI has booked it, `REPAY` events (`TFFSARPY`). How it works: [transactions and events](docs/architecture/fsa-transaction-model.md).

Without SQL Server or TI (H2 database in `app/data/`, messages in files, a pretend TI that books everything after 10 s):

```
cd app
CHANNEL_DEMO_PASSWORD=choose-one ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,h2 \
  -Dspring-boot.run.arguments="--channel.gateway.mode=file --channel.gateway.simulator.enabled=true"
```

Open http://localhost:8880, sign in as `demo`, choose **Request standalone finance** and send it. The transaction page follows the event (refreshing itself while TI works): sent, received by the bank, completed. Then **Repay this finance** opens the repayment form. Messages written are in `app/sent-messages/`, TI's answers in `app/ti-inbox/processed/`.

Against SQL Server and TI's queues, set the database and the gateway in the environment (Spring reads `CHANNEL_GATEWAY_MQ_HOST` for `channel.gateway.mq.host` and so on; defaults are in `app/src/main/resources/application.yml`):

| Variable | Meaning |
|---|---|
| `CHANNEL_DB_URL`, `CHANNEL_DB_USER`, `CHANNEL_DB_PASSWORD` | SQL Server; Liquibase creates the tables at start-up |
| `CHANNEL_GATEWAY_MODE` | `mq` or `file` |
| `CHANNEL_GATEWAY_MQ_HOST`, `..._PORT`, `..._CHANNEL`, `..._QUEUE_MANAGER`, `..._USER`, `..._PASSWORD` | IBM MQ connection |
| `CHANNEL_GATEWAY_MQ_SEND_QUEUE` | TI's incoming queue |
| `CHANNEL_GATEWAY_MQ_REPLY_QUEUE`, `CHANNEL_GATEWAY_MQ_OUTWARD_QUEUE` | where TI puts its `ServiceResponse` and its outward messages (`TFFINACK`); the channel listens to both |
| `CHANNEL_GATEWAY_TARGET_SYSTEM`, `CHANNEL_GATEWAY_SOURCE_SYSTEM`, `CHANNEL_GATEWAY_TI_USER` | envelope header |
| `CHANNEL_CUSTOMER_MNEMONIC`, `CHANNEL_CUSTOMER_NAME`, `CHANNEL_CUSTOMER_BEHALF_OF_BRANCH` | the corporate the channel acts for |
| `CHANNEL_JOBS_ENABLED` | `false` stops the outbox dispatcher and the listeners |

Passwords belong in the environment, not in `application.yml` (ground rule 9).

