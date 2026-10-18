# 0002 – Modular monolith with a worker

**Status:** Proposed · **Date:** 2026-10-18

## Decision

Build one deployable **API** and one deployable **worker** from the same code base, plus a **gateway simulator** used in development and CI. No microservices.

* The API handles requests and writes to the database. It never calls the gateway.
* The worker reads the outbox and talks to the gateway; it also polls for status.
* Both import the same `domain` and `gateway-adapter` packages.

## Why

The domain is one consistent unit (transactions and their events); the load is modest (corporate users, not retail); the risky part is correctness of submission, not scale. Fewer deployables mean fewer failure modes and a simpler task graph for agents. Package boundaries (see [README](../README.md)) keep a later split cheap.

## Consequences

* A product (for example Import LC) is a vertical slice inside the same packages, not a service.
* API and worker scale independently because they are separate processes.

**Reversal cost:** low to medium.
