# 0002 – Modular monolith with a worker

**Status:** Proposed · **Date:** 2026-10-19

## Decision

Build one **web application** and one **worker** from the same Maven build, plus a **gateway simulator** used in development and CI. No microservices.

* The web application (Spring MVC + Thymeleaf) serves pages and writes to the database. It never calls the gateway.
* The worker (a second Spring Boot application) reads the outbox and talks to the gateway; it also polls for status.
* Both depend on the same `domain`, `persistence`, `catalogue` and `gateway-adapter` modules.

## Why

The domain is one consistent unit (transactions and their events); the load is modest (corporate users, not retail); the risky part is correctness of submission, not scale. Fewer deployables mean fewer failure modes and a simpler task graph for agents. Module boundaries, enforced by ArchUnit (see [README](../README.md)), keep a later split cheap.

## Consequences

* A product (for example Import LC) is a vertical slice inside the same packages, not a service.
* Web and worker scale independently because they are separate processes.

**Reversal cost:** low to medium.
