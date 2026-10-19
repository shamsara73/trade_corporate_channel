# MQ proof of concept

A Java 21 console app that talks to IBM MQ and keeps a JSON file of references. No database, no web, no Spring. It is a throw-away POC, outside the architecture in `docs/architecture/` (the rule "only `gateway-adapter` knows XML" does not apply here).

## What it does

| # | Behaviour | How |
|---|---|---|
| 1 | Send a sample `TFIGTAPP` and save its `TheirReference` | `send`: builds the message from `src/main/resources/tfigtapp-sample.xml`, puts it on the send queue, then adds `{theirReference, origin: CHANNEL, status: SENT}` to `references.json` |
| 2 | Listen for `TFUTIACK` / `TFUTIREJ` and update the reference | status becomes `PROCESSED` / `REJECTED`. A later `TFUTIACK` or `TFUTIREJ` overwrites it (TI can send them again after a `TFUTICRT`). |
| 3 | Listen for `TFUTICRT` and save its reference | adds `{origin: TI, status: TI_CREATED}`. If the reference already exists it is left alone. |

An ACK/REJ for a reference the app never sent is saved with origin `UNKNOWN`. A message that cannot be read is appended to `unprocessed.log` and removed from the queue, so it cannot loop. If the JSON file cannot be written the message goes back to the queue.

## Run

Java 21 and Maven; an IBM MQ queue manager with three queues.

```
mvn package                          # tests + runnable jar in target/
java -jar target/mq-channel-poc-0.1.0-SNAPSHOT.jar listen   # listeners; type send, list or quit
java -jar target/mq-channel-poc-0.1.0-SNAPSHOT.jar send     # send one sample and exit
```

Settings are environment variables. Defaults match the IBM MQ developer container.

| Variable | Default | |
|---|---|---|
| `MQ_HOST`, `MQ_PORT` | `localhost`, `1414` | |
| `MQ_QMGR`, `MQ_CHANNEL` | `QM1`, `DEV.APP.SVRCONN` | |
| `MQ_USER`, `MQ_PASSWORD` | `app`, empty | no user/password is sent when the password is empty |
| `MQ_QUEUE_SEND` | `DEV.QUEUE.1` | where `TFIGTAPP` goes (TI's inbound queue) |
| `MQ_QUEUE_ACKREJ` | `DEV.QUEUE.2` | where `TFUTIACK` / `TFUTIREJ` arrive |
| `MQ_QUEUE_CRT` | `DEV.QUEUE.3` | where `TFUTICRT` arrives |
| `MQ_RFH2` | `false` | `true` adds the JMS (RFH2) header to sent messages |
| `REFERENCES_FILE`, `UNPROCESSED_FILE` | `references.json`, `unprocessed.log` | |

Local MQ: `docker run -e LICENSE=accept -e MQ_QMGR_NAME=QM1 -e MQ_APP_PASSWORD=passw0rd -p 1414:1414 icr.io/ibm-messaging/mq`, then `MQ_PASSWORD=passw0rd`.

## Assumptions (to confirm with the TI team)

| Id | Assumption | Open question |
|---|---|---|
| POC-1 | The `TheirReference` we sent comes back in `InstructingPartyRef` (event field IRIR), or in a `TheirReference` element. If neither is present the message is matched by `eBankMasterRef`. | Q26 |
| POC-2 | Outward messages are XML text messages in which every mapping field is an element named as in `docs/outbound/TFUTIACK.md`, at any depth and in any namespace. | Q21 |
| POC-3 | The channel uses one queue to send and queues of its own to receive; message type is taken from `MessageName`, not from the queue. | Q21 |
| POC-4 | TI's MQ reader wants bare text, so no RFH2 header (`MQ_RFH2=false`). | Q21 |

The sample `TFIGTAPP` has invented values and has not been tested against a gateway; the credentials in its header are placeholders.

## Tested and not tested

* 16 unit tests cover parsing and the whole file flow (send, ACK, REJ, CRT, repeated and unknown references, unreadable messages, restart) without MQ.
* The MQ layer (`MqBus`) compiles and the jar starts and tries to connect, but it **has not been run against a queue manager**: none was available when it was written. Run the steps above before relying on it.
