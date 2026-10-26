# 0002 – One application for the proof of concept

**Status:** Decided by the project owner (2026-10-26) · supersedes the 2026-10-19 proposal of separate web, worker and simulator applications

## Decision

Build **one Spring Boot application**, in one Maven project under `app/`. It serves the pages, runs the background jobs (outbox dispatcher, listeners for TI's replies and outward messages, status polling) and talks to TI over IBM MQ. No separate worker, no separate simulator application, no microservices.

* The parts that were planned as Maven modules are **packages** of the one application: `catalogue`, `domain`, `gateway`, `persistence`, `web`, `jobs`.
* Their boundaries still hold and are checked by ArchUnit on every build (see [architecture README](../README.md#package-rule)): only `gateway` knows XML, `domain` knows no infrastructure, `web` never talks to TI directly.
* The **gateway simulator** is test code: an embedded JMS broker and scripted TI behaviour in the application's tests (and, if needed for local demos, a `simulator` Spring profile). It is never active in production.

## Why

The project owner judged the multi-module, three-application layout too wide for a proof of concept. One deployable and one build are faster to understand, run and change. The package boundaries keep the later split cheap: each package can become a Maven module or a separate application without rewriting its code.

## When to revisit

When the actual project planning starts: if web traffic and background work need to scale separately, if the bank's deployment rules require separate processes, or if teams need to own parts independently. Splitting means turning packages into modules along the existing package rule; the architecture tests show where the seams are.

## Consequences

* A product (for example Finance Standalone) is a vertical slice through the packages.
* Background jobs run in the same process as the web pages. If several instances run, jobs coordinate through the database (outbox claiming, ADR 0003), so no instance double-sends.

**Reversal cost:** low (the package rule is the module plan).
