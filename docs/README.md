# Trade Corporate Channel – design documents

| Document | Purpose |
|---|---|
| [01 – Interface catalogue](01-interface-catalogue.md) | What the FTI Gateway offers: envelope, message families, all `TF*` messages |
| [02 – Functional specification](02-functional-specification.md) | Requirements, lifecycle, integration pattern, phasing, open questions |
| [Product specifications](products/README.md) | One specification per trade product |
| [TI outward messages](outbound/README.md) | What TI sends back (first mapping received: `TFUTICRT`), decoded, cross-checked against the inbound schema, and traced to the TI database tables |
| [Architecture and decisions](architecture/README.md) | Proposed stack and structure, with decision records |
| [Agent guide](../AGENTS.md) | How AI agents work on this repository |
| [Open questions](agents/open-questions.md) | What is unknown and the working assumption for each |
| [Task template](agents/task-template.md) · [Definition of done](agents/definition-of-done.md) · [Task backlog](agents/task-backlog.md) | Agent working agreement and first tasks |
