# 0006 – Front end

**Status:** Proposed · **Date:** 2026-10-18

## Decision

* **React** with **Vite**, TypeScript strict, React Router, TanStack Query for server state.
* **Forms** are rendered from **form definitions** that layer on the schema catalogue: grouping, labels, help text, order, visibility rules, hidden/defaulted fields (see doc 02, section 6). A product's wizard (the *Screens* table in each product spec) is one form definition file in `packages/web/src/forms/<product>/`. Hand-built screens are the exception and need a reason in the pull request.
* **Validation** uses the same Zod schemas as the server (from the catalogue plus domain rules). The server is authoritative; client validation is for speed.
* **Component library and design system:** not chosen. The bank's design system should be used if one exists (open question). Until then, build on an accessible headless component set and keep styling in design tokens so a bank theme can replace it.
* **Accessibility:** WCAG 2.1 AA; automated checks in CI and manual keyboard review for each new form.
* **i18n:** all user-facing strings in message catalogues from the first screen; English first.
* **Coded values** are shown by label, never by code (doc 02, FR-15).

## Consequences

Adding a product means writing definitions and domain rules, not new React components, unless the product needs a special widget (for example the draft/tenor editor, the document checklist).

**Reversal cost:** medium (front-end framework), low (component library).
