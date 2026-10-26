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

## Try the finance request

The first feature is the Finance Standalone request (`TFFINNEW`), built to match the message the bank's TI accepted ([working samples](docs/reference/working-samples.md)).

```
cd app
CHANNEL_DEMO_PASSWORD=choose-one ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Open http://localhost:8080, sign in as `demo`, choose **Request standalone finance**, fill in the form and press **Send to the bank**. The page then shows the transaction number (`eBankMasterRef`), event number (`eBankEventRef`) and message reference (`TheirRef`).

By default the message is written to `app/sent-messages/<TheirRef>.xml` instead of MQ. To put it on TI's queue, set these before starting (Spring reads them from the environment):

| Variable | Meaning | Default |
|---|---|---|
| `CHANNEL_GATEWAY_MODE` | `mq` to send to IBM MQ | `file` |
| `CHANNEL_GATEWAY_MQ_HOST`, `..._PORT`, `..._CHANNEL`, `..._QUEUE_MANAGER` | MQ connection | `localhost`, `1414`, `DEV.APP.SVRCONN`, `QM1` |
| `CHANNEL_GATEWAY_MQ_USER`, `CHANNEL_GATEWAY_MQ_PASSWORD` | MQ credentials | `app`, none |
| `CHANNEL_GATEWAY_MQ_SEND_QUEUE` | TI's incoming queue | `DEV.QUEUE.1` |
| `CHANNEL_GATEWAY_MQ_REPLY_QUEUE` | sent as JMS reply-to for TI's `ServiceResponse` | none |
| `CHANNEL_GATEWAY_TARGET_SYSTEM`, `CHANNEL_GATEWAY_SOURCE_SYSTEM`, `CHANNEL_GATEWAY_TI_USER` | envelope header (TI zone, our system id, TI user) | `ZONE1`, `TRADECHANNEL`, `CHANNEL_SVC` |
| `CHANNEL_CUSTOMER_MNEMONIC`, `CHANNEL_CUSTOMER_NAME`, `CHANNEL_CUSTOMER_BEHALF_OF_BRANCH` | the corporate the request is for | invented test values |

