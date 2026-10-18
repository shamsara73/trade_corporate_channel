# 0004 – One schema catalogue drives validators, builders and forms

**Status:** Proposed · **Date:** 2026-10-18

## Context

There are 181 `TF*` messages with deeply nested bodies. Element names are inconsistent (`eBankMasterRef` / `EBankMasterRef`, `Documentss`, `ApplcableRules`). Hand-typed names and hand-written validators will drift and fail at the gateway.

## Decision

* Generate a **schema catalogue** (JSON) from `sources/xsd_element_docs/*.xsd.txt` and `sources/example_xml/*.xml`. For each message it records, per element path: type, min/max length, nillable, repeating, enumeration values and labels, description, XML namespace prefix and order.
* Everything else is **derived** from it:
  * request builders in `gateway-adapter`,
  * runtime validators (Zod schemas) shared by browser and server,
  * default form definitions for the web app,
  * the product specifications (via `tools/specgen`).
* The catalogue is committed. `pnpm catalogue:build` regenerates it; CI fails if the committed copy differs from a fresh build.
* On a gateway upgrade: replace `sources/`, regenerate, and review the diff. A diff in the catalogue is the change list.
* Business rules that the schema cannot express (the V-rules in the product specs) live in `domain`, referenced by rule id, not in the catalogue.

## Rules for contributors

* **Never type an element name by hand in application code.** Reference it through the generated types.
* **Never edit generated files.** Change the generator or the hand-written definitions it reads.

## Consequences

The catalogue is only as good as the documentation text (it has no mandatory markers, no cross-field rules). Those gaps are tracked in the [open questions](../../agents/open-questions.md) and filled by explicit overlay files (`catalogue-overlays/*.json`: required flags, hidden fields, labels), never by editing the generated output.

**Reversal cost:** low (the catalogue format is internal).
