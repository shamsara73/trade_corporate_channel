# 0007 – Identity and entitlements

**Status:** Proposed · **Date:** 2026-10-19

## Decision

* **Authentication** by OpenID Connect against the bank's identity provider, using **Spring Security's OAuth2 login** (authorisation-code flow with PKCE) and a server-side session cookie (`HttpOnly`, `Secure`, `SameSite=Lax`, session fixation protection, idle and absolute timeouts). Local development uses a containerised IdP with fixed test users. Strong-authentication requirements are the bank's and enforced at the IdP.
* **Authorisation** is enforced on the **server** (service layer plus `@PreAuthorize` on controllers), never only by hiding links.
  * A user belongs to one or more **corporates** (TI customer mnemonic).
  * **Entitlements** are `(corporate, product, action, currency, amount limit)` with actions `view`, `create`, `approve`.
  * **Maker-checker:** the approver must differ from the maker; the number and order of approvers follow the corporate's approval rule (doc 02, FR-04).
* The message sent to TI is **built from the approved snapshot**. Nothing the user sends after approval can change it.
* The `Context.Customer` of every outbound message is taken from the selected corporate, never from client input.
* Bank staff (channel admin) use a separate role set and a separate URL space.
* A request-scoped `CorporateContext` bean carries the selected corporate; repositories take it as a parameter, so a query without it does not compile.

## Consequences

* Row-level checks (corporate ownership) are required on every query. A shared helper enforces them; code review and tests treat a missing check as a defect.
* Entitlement and approval models are not yet specified in detail (open question Q9).

**Reversal cost:** medium.
