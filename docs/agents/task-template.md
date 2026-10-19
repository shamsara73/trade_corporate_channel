# Task template

> Part of the agent guide: [AGENTS.md](../../AGENTS.md). Use this for every task given to an agent. One task = one pull request.

```markdown
# T<number> – <title>

**Goal:** one sentence of the outcome.

**Read first:**
- Product spec: docs/products/<file>.md, sections <..>
- Decisions: docs/architecture/decisions/<..>
- Open questions used: Q<..> (working assumptions apply; tag them)

**In scope:** what to build.
**Out of scope:** what not to touch (name packages and files).
**Modules allowed to change:** <module>, <module>

**Interfaces to keep:** names and signatures other tasks rely on.

**Acceptance tests (must exist and pass):**
1. <test name> – <behaviour, with example input and expected output>
2. ...

**Definition of done:** see docs/agents/definition-of-done.md, plus:
- <task-specific checks>

**Depends on:** T<..>
**Blocks:** T<..>
```

## Writing good acceptance tests

* State inputs and expected outputs concretely, using the sample payloads in the product spec section 9.
* Include at least one negative case per rule (over-length text, bad enumeration, past date, wrong corporate).
* Name the spec rule being tested (`V3`, `FR-22`, `0003`) in the test description.
* Prefer tests that fail loudly if an assumption changes.
