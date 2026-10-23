# Trade Corporate Channel – documents

Everything here is documentation; the application is in [`../app/`](../app/) and the bank's original material in [`../sources/`](../sources/). Pages marked "Generated" at their top are built by [`../tools/`](../tools/) and must not be edited by hand.

## 1. What we are building

| Document | Purpose |
|---|---|
| [02 – Functional specification](02-functional-specification.md) | Requirements, lifecycle, integration pattern, phasing, open questions |
| [Product specifications](products/README.md) | One specification per trade product |

## 2. How TI talks to the channel

| Document | Purpose |
|---|---|
| [Working samples](reference/working-samples.md) | Messages accepted by the bank's TI test environment (anonymised), and what they prove |
| [01 – Interface catalogue](01-interface-catalogue.md) | What the FTI Gateway offers: envelope, message families, all `TF*` messages |
| [TI outward messages](outbound/README.md) | What TI sends back (first mapping received: `TFUTICRT`; TI event field list for the IGT issue event), decoded, cross-checked against the inbound schema, and traced to the TI database tables |
| [TI SDK guides](reference/ti-sdk-guides.md) | What the Finastra SDK guides (Systems Interfacing, Application Extension, Systems Integration) say about transports, the message header, outward gateway messages, notifications and bank-defined fields, and what that changes |
| [TI XSD jars](reference/xsd-jars.md) | TI's real XSDs (bank build 2.10.3.1) compared with the 2.11.5.2.J documentation; sample validation; the bank's `ExtraData` fields with their event codes |

## 3. How we build it

| Document | Purpose |
|---|---|
| [Architecture and decisions](architecture/README.md) | Proposed stack and structure, with decision records |
| [Agent guide](../AGENTS.md) | How AI agents work on this repository |
| [Open questions](agents/open-questions.md) | What is unknown and the working assumption for each |
| [Task template](agents/task-template.md) · [Definition of done](agents/definition-of-done.md) · [Task backlog](agents/task-backlog.md) | Agent working agreement and first tasks |
