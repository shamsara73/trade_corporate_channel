# MQ proof of concept

A Java 21 console app that talks to IBM MQ and keeps a JSON file of references. No database, no web, no Spring. It is a throw-away POC, outside the architecture in `docs/architecture/` (the rule "only `gateway-adapter` knows XML" does not apply here).

## What it does

| # | Behaviour | How |
|---|---|---|
| 1 | Send a sample message and save its `TheirReference` | `send`: builds the message from `src/main/resources/samples/<POC_MESSAGE>.xml` (default **`TFFINNEW`, the bank's working sample**; `TFIGTAPP` also available) with a fresh customer reference `TheirReference` (in `Context/TheirReference` and `DebitParty/Reference`), a message reference `TheirRef` (unique per message delivered), a transaction number `eBankMasterRef` (`TF` + N) and an event number `eBankEventRef` (N for this first event), N = `yyMMddHHmmss` + 2 hex, and today's start date with maturity 60 days later. It puts the message on the send queue, then adds `{theirReference, messageRef, eBankMasterRef, eBankEventRef, origin: CHANNEL, status: SENT}` to `references.json` |
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
| `MQ_QUEUE_SEND` | `DEV.QUEUE.1` | where the sample message goes (TI's inbound queue) |
| `MQ_QUEUE_ACKREJ` | `DEV.QUEUE.2` | where `TFUTIACK` / `TFUTIREJ` arrive |
| `MQ_QUEUE_CRT` | `DEV.QUEUE.3` | where `TFUTICRT` arrives |
| `MQ_RFH2` | `false` | `true` adds the JMS (RFH2) header to sent messages |
| `POC_MESSAGE` | `TFFINNEW` | template to send: `TFFINNEW` or `TFIGTAPP` |
| `POC_CUSTOMER`, `POC_CUSTOMER_NAME`, `POC_BEHALF_OF_BRANCH` | `0009990001`, `POC TEST CUSTOMER`, `001` (invented) | set these to the test customer that worked in the bank's environment; they are not committed |
| `REFERENCES_FILE`, `UNPROCESSED_FILE` | `references.json`, `unprocessed.log` | |

Local MQ: `docker run -e LICENSE=accept -e MQ_QMGR_NAME=QM1 -e MQ_APP_PASSWORD=passw0rd -p 1414:1414 icr.io/ibm-messaging/mq`, then `MQ_PASSWORD=passw0rd`.

## Assumptions (to confirm with the TI team)

| Id | Assumption | Open question |
|---|---|---|
| POC-1 | An incoming message is matched first by `eBankMasterRef` + `eBankEventRef` (transaction and event number, as the bank defines them), then by the `TheirReference` we sent coming back in `InstructingPartyRef` (event field IRIR) or a `TheirReference` element, then by `eBankMasterRef` alone. | Q26, Q34 |
| POC-2 | Outward messages are a `ServiceRequest` with `Service` = `GATEWAY` and `Operation` = `DOCS` or the message name (confirmed by the SDK Systems Interfacing Guide). **Assumed:** inside it, every mapping field is an element named as in `docs/outbound/TFUTIACK.md`, at any depth and in any namespace. The name comes from `MessageName`, else from `Operation` unless it is `DOCS`. | Q21 |
| POC-3 | Queues map to TI's Corporate Channels set-up: send queue = `ticcIncoming`, ACK/REJ and CRT queues = `ticcOutgoing` (one queue in TI; set both variables to the same name and the app listens once). A `ServiceResponse` (TI's reply to `TFIGTAPP`, on `ticcIncomingReply`) is logged with its status and errors if it reaches a listened queue. | Q13 |
| POC-4 | TI's MQ reader wants bare text, so no RFH2 header (`MQ_RFH2=false`). | Q21 |
| POC-5 | Messages are put on the queue **without** the `ServiceRequest` envelope, as in the bank's working `TFFINNEW` ([working samples](../../docs/reference/working-samples.md)). `TFIGTAPP` uses the same bare form, which is not yet proven. | Q33 |
| POC-6 | TI's outward messages for a finance (`TFFINNEW`) are not the `TFUTI…` ones. Until their mappings arrive, they land in `unprocessed.log` with their name, which tells us what to map next. | Q22 |

The `TFFINNEW` template keeps the structure, codes (`FSA`, `PSF`) and amounts of the working message; its customer data is invented and replaced at run time from the environment. Both templates validate against TI's XSD jar (`python3 -I tools/xsdcheck/check_samples.py`). The `TFIGTAPP` template has not been tested against TI.

## Tested and not tested

* 21 unit tests cover parsing and the whole file flow (send, ACK, REJ, CRT, repeated and unknown references, unreadable messages, restart) without MQ.
* The MQ layer (`MqBus`) compiles and the jar starts and tries to connect, but it **has not been run against a queue manager**: none was available when it was written. Run the steps above before relying on it.
