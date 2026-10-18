# 0001 – TypeScript monorepo

**Status:** Proposed · **Date:** 2026-10-18

## Context

The channel is mostly forms, validation and message mapping around a large, regular schema (181 messages). The work will be done largely by AI coding agents, which work best when the whole system is in one repository, typed, and checked by fast tests.

## Decision

* One repository, **pnpm workspaces**, **TypeScript** (strict) for front end, back end, tooling and the simulator.
* **Node.js** current LTS at scaffolding time; the version is pinned in `.nvmrc` and the lockfile is committed.
* The existing Python generator in `tools/specgen/` stays for documentation. Its schema parser is **ported** into `packages/schema-catalogue` as the single source of truth (see [0004](0004-schema-catalogue.md)); the Python tool may then read the catalogue instead of parsing again.

## Alternatives

| Option | For | Against |
|---|---|---|
| Java 21 + Spring Boot back end, React front end | Common in banks; strong XML tooling | Two languages; generated types cannot be shared with the browser |
| .NET 8 back end | Common in banks | Same two-language cost |
| Python back end | Reuses the generator | Weaker static guarantees across the stack |

## Consequences

* Validators and types generated once are used in browser, API and tests.
* If the bank's platform team mandates Java or .NET, only `api`, `worker`, `gateway-adapter` and `domain` change; the catalogue stays language-neutral JSON and the web app is unaffected.

**Reversal cost:** medium (rewrite of four server packages; catalogue, specs, web app and tests' fixtures survive).
