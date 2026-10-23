# 0006 – Front end: server-rendered Thymeleaf

**Status:** Proposed (Thymeleaf chosen by the project owner on 2026-10-19; htmx and form-definition details are proposals) · **Date:** 2026-10-19

## Decision

* **Spring MVC + Thymeleaf** render every page on the server. There is no single-page application and no separate front-end build.
* **Layout:** one base layout fragment, reusable fragments for field, group, repeating group, table, status badge, alerts. Pages are composed from those, not copy-pasted.
* **Forms are rendered from form definitions.** A form definition (YAML under `app/web/src/main/resources/forms/<product>/`) layers on the schema catalogue: sections and steps, labels (message keys), help text, order, visibility rules, hidden or defaulted fields (doc 02, section 6). A product's wizard is its *Screens* table from the product specification, written as one form definition. A generic controller and the Thymeleaf fragments render any definition; adding a product means writing definitions and domain rules, not new templates, unless it needs a special widget (tenor editor, document checklist).
* **Binding:** form fields are named by catalogue path (`f['LCAmount/Amount']`, `f['Documentss/Documents[0]/DocumentDesc']`) and bound into a `Map`; the server converts it to a `Payload`. No class-per-form.
* **Validation:** the server is the only authority. The catalogue validator and the domain V-rules run on each step's POST; errors come back per path and are shown beside the field. HTML attributes from the catalogue (`maxlength`, `required`, `pattern`, `<select>` options) give instant browser feedback but are never trusted.
* **Dynamic behaviour** (add/remove repeating rows, show/hide conditional fields, step navigation without a full reload) uses **htmx** and a small amount of vanilla JavaScript, served as static files from the application (no CDN). Every page must still work, if less smoothly, with scripting disabled. **[Proposal: the team may choose plain JavaScript instead.]**
* **Wizard state:** drafts are saved on the server at each step; the browser holds no business state. A user can leave and resume.
* **Security:** Spring Security CSRF tokens on every POST; Thymeleaf's default escaping (no `th:utext` for user data); a strict Content-Security-Policy (no inline scripts); `Cache-Control: no-store` on pages with transaction data.
* **i18n:** all text from `messages*.properties` through Spring's `MessageSource`; coded values are shown by label, never by code (doc 02, FR-15).
* **Accessibility:** WCAG 2.1 AA. Native form controls, labelled fields, error summaries linked to fields, keyboard operable. Automated axe checks in the end-to-end tests ([0009](0009-testing-and-simulator.md)).
* **Styling:** CSS with design tokens (custom properties). The bank's design system, if there is one, replaces the tokens and fragments (open question Q16).

## Alternatives

| Option | Why not |
|---|---|
| React or another SPA | Not the team's strength; duplicates validation in two languages |
| Vaadin or JSF | Heavier server-side component models; harder for agents to reason about than plain templates |
| Plain Thymeleaf without htmx | Possible. Repeating groups would need full-page round trips; acceptable for a first release |

## Consequences

* Business rules exist in one place (Java).
* Pages are testable with MockMvc and end-to-end with Playwright without a JavaScript build.
* Rich interactions are limited to what htmx and small scripts can do.

**Reversal cost:** medium (templates), low (htmx).
