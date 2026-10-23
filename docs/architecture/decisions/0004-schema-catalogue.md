# 0004 – One schema catalogue drives validators, builders and forms

**Status:** Proposed · **Date:** 2026-10-19

## Context

There are 181 `TF*` messages with deeply nested bodies. Element names are inconsistent (`eBankMasterRef` / `EBankMasterRef`, `Documentss`, `ApplcableRules`). Hand-typed names and hand-written validators will drift and fail at the gateway.

## Decision

* A Maven module **`catalogue`** contains:
  1. a **generator** (a Java main class run by Maven) that reads **TI's real XSDs** (`sources/com.misys.tiplus2.gen.ti-xsd*.jar`, received 2026-10-22) for structure, types, lengths, order and `ExtraData`, and `sources/xsd_element_docs/*.xsd.txt` for descriptions and enumeration labels, and writes one JSON file per message under `app/catalogue/src/main/resources/catalogue/`. Where the two disagree the jar wins, because TI validates against it (open question Q32; differences in [xsd-jars](../../reference/xsd-jars.md));
  2. a **loader** and an immutable model (`MessageDefinition`, `ElementDefinition` as Java records);
  3. a **validator** that checks a payload against a definition;
  4. generated **path constants** (for example `TFILCAPP.LC_AMOUNT_AMOUNT = "LCAmount/Amount"`) so application code references element names through the compiler, never as string literals.
* Per element the JSON records: path, type, min/max length, nillable, repeating, enumeration values and labels, description, XML namespace prefix and order.
* Everything else is **derived** from it: the XML builder and parser in `gateway-adapter`, validation in `domain` and `web`, HTML constraints (`maxlength`, `pattern`, `required`) and select options in Thymeleaf forms, and the simulator's request checks.
* Generated JSON and constants are **committed**. `./mvnw -pl app/catalogue -Pgenerate-catalogue generate-resources` regenerates them; the build fails if a fresh generation differs from the committed files.
* On a gateway upgrade: replace `sources/`, regenerate, review the diff. A catalogue diff is the change list.
* Business rules the schema cannot express (the V-rules in the product specs) live in `domain`, referenced by rule id, not in the catalogue.
* Required flags, hidden fields and labels are added through **overlay** files (`app/catalogue/src/main/resources/overlays/<MESSAGE>.yml`), never by editing the generated JSON.

* A **contract test** validates every message the builder produces against the jars with `javax.xml.validation` (the jars go on the test classpath as-is). This replaces guessing from the documentation text.

### Outward message definitions

TI also **sends** structured messages whose content is defined by mapping documents (`sources/ti_outbound/*.txt`, first example `TFUTICRT`; see [outbound](../../outbound/README.md)). The `catalogue` module parses them with the same generator into `OutwardMessageDefinition` records (field name, tag, type, facet, repeating-table columns, literal text). Their keys are the mapping's field names, not inbound schema paths. The Python tool in `tools/mappingdoc/` documents them and checks a crosswalk to the inbound schema; the Java parser must agree with it (same contract test as below). The notation is inferred and tracked as open question Q23. Each mapping's tags are validated against the TI event field list for its event (`sources/ti_outbound/*.csv`); unknown tags are reported, not fatal, because bank-defined fields may be missing from a list (Q25).

### Two parsers

The documentation generator in `tools/specgen/` (Python) and the catalogue generator (Java) read the same source files with separate parsers. To prevent drift, a contract test compares them: for a fixed list of messages the Java catalogue must report the same top-level element names, counts and enumerations as the Python tool. A later task can retire one parser, preferably by having `tools/specgen` read the catalogue JSON.

## Rules for contributors

* **Never type an element name as a string literal in application code.** Use the generated constants.
* **Never edit generated files.** Change the generator, an overlay, or the hand-written definitions it reads.

## Consequences

* The catalogue is only as good as the documentation text (no mandatory markers, no cross-field rules). Gaps are tracked in [open questions](../../agents/open-questions.md) and filled by overlays.
* Payloads in `domain` are generic trees (`Payload`, a thin wrapper over Jackson `ObjectNode`) addressed by catalogue path, not one Java class per message. 181 generated classes would add bulk without safety the constants and validator don't already give.

**Reversal cost:** low (the catalogue format is internal).
