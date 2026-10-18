# 0010 – Security, audit and observability

**Status:** Proposed · **Date:** 2026-10-18

## Security

* TLS for all traffic, including to the gateway. Mutual TLS or signed requests if the gateway requires `Certificate` / `Digest` (open question Q2).
* **Secrets** (gateway credentials, database passwords, signing keys) come from the platform secret store through environment/config at start-up. Never in source, images, logs, or test fixtures. A secret scan runs in CI and pre-commit.
* Input handling: all input validated on the server against the catalogue; output escaped; file uploads type- and size-checked and scanned **[Assumption: bank's scanner]**.
* Dependencies: lockfile committed; automated vulnerability checks in CI; no dependency added without a stated reason in the pull request.
* Data: personal and commercial data minimised in logs. Attachments and request/response bodies are stored encrypted at rest.
* Review: a security review (`/security-review` or the bank's process) before any release that touches authentication, entitlements, the gateway adapter, or file handling.

## Audit

* An **append-only** `audit_event` table: actor, corporate, action, entity, before/after reference, time, correlation id. No update or delete is granted to the application role.
* Every submission stores the exact request and response (credentials removed) with `CorrelationId`.
* Retention period is a bank decision (open question Q12).

## Observability

* Structured JSON logs with `correlationId`, `eBankMasterRef`, `eBankEventRef`, `userId`, `corporate`. No credentials, no full payloads at info level.
* OpenTelemetry traces across API → outbox → worker → gateway adapter.
* Metrics: outbox depth and age, submission outcomes by status, gateway latency, poll lag, notification intake.
* Alerts: stuck outbox rows, repeated `UNAVAILABLE`, unmatched submissions, failed audit writes.

## Not decided

Hosting platform, secret-store product, log/metrics vendor, document scanner. These are listed in [open questions](../../agents/open-questions.md).
