# 0001 – Java 21, Spring Boot, Thymeleaf, Maven multi-module

**Status:** Proposed (language and UI technology chosen by the project owner on 2026-10-19; the rest are proposals) · **Date:** 2026-10-19

## Context

The channel is mostly forms, validation and message mapping around a large, regular schema (181 messages). The delivery team is most confident with **Java 21 and Thymeleaf**, and AI agents will do much of the coding, so the stack should be mainstream, strongly typed, and checkable by fast automated tests. Banks commonly run Java, so operations and security reviewers will know it.

An earlier draft proposed a TypeScript monorepo with a React single-page app. It was replaced by this decision before any code existed.

## Decision

| Concern | Choice |
|---|---|
| Language | **Java 21** (LTS). Records, sealed types and pattern matching for the domain; virtual threads allowed for blocking I/O (JDBC, gateway calls). |
| Framework | **Spring Boot 3.x**, Spring MVC (servlet), Spring Security, Spring JDBC. |
| UI | **Server-rendered Thymeleaf** pages (see [0006](0006-frontend.md)). |
| Build | **Maven**, one parent POM, multi-module, Maven Wrapper (`./mvnw`) checked in. Java toolchain pinned to 21. |
| Layout | One repository, one multi-module build ([architecture README](../README.md)). |
| Formatting and static checks | Spotless (formatter), Error Prone, ArchUnit tests for layering, OWASP dependency check, gitleaks. |
| Tests | JUnit 5, AssertJ, Mockito, XMLUnit, Testcontainers (SQL Server and Oracle), Playwright for Java ([0009](0009-testing-and-simulator.md)). |
| Database | **SQL Server and Oracle** (chosen by the project owner on 2026-10-19), Spring `JdbcClient`, Flyway, one migration set per vendor ([0003](0003-database-and-outbox.md)). |

The Java base package is `com.bank.tradechannel` **[Assumption – rename to the institution's package once known; open question Q19]**.

The existing Python tool in `tools/specgen/` stays for generating the documentation. The application's schema catalogue is produced by a **Java** generator module (see [0004](0004-schema-catalogue.md)), so the application build needs only a JDK.

## Alternatives

| Option | For | Against |
|---|---|---|
| Gradle instead of Maven | Faster builds, flexible | Less familiar in many banks; Maven's fixed lifecycle is easier for agents to get right |
| Quarkus or Micronaut | Fast start-up | Smaller ecosystem for Thymeleaf and Spring Security patterns the team knows |
| TypeScript monorepo, React | One language across stack | Not the team's strength; rejected by the project owner |

## Consequences

* One language end to end; the catalogue, validators, XML builder and forms share types.
* Server-rendered pages make the server the only place rules run, which suits a bank (see 0006, 0007).
* Dynamic form behaviour (repeating groups, conditional fields) needs a small amount of JavaScript; see 0006.

**Reversal cost:** high once code exists (framework), low for Maven vs. Gradle.
