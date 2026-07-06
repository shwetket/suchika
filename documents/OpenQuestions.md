                                                                                                                                                                                                                                                                        # Open Questions

| | |
|---|---|
| **Type** | Decision Log |
| **Audience** | Product owner (Ketan) |
| **Status** | Active |
| **Last updated** | 2026-07-05 (Q31-Q53 all resolved 2026-07-04; Q54 added) |

## Purpose

Single shared log for all open questions raised during reviews. Answer each question here so agents can proceed. Questions are numbered for easy reference.

---

<!-- Architect, Business Analyst, and QA agents append questions below. Keep the numbering sequential across all three sections. -->

## Architect Review Questions — 2026-06-29

**Q1.** ~~Should `profile_id` be stored inside domain entities...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option A — keep `profileId` as a plain `UUID` field in domain entities. Pragmatic trade-off, document in an ADR; ArchUnit rules stay as-is.

*Original question, preserved for context:*
Should `profile_id` be stored inside domain entities (e.g., `CalendarEvent.profileId`) or kept only in the adapter layer?
*Context:* ADR-006 says the `profile_id` filter belongs in the adapter layer only — domain entities must not store or reason about it. But the current `CalendarEvent` domain entity holds a `profileId` field, set in `CalendarEventService.create()` by passing `profileId` into the domain factory method `CalendarEvent.create(profileId, ...)`. This is repeated in household domain entities. The same pattern may apply to other domains. Removing `profileId` from domain entities would require the adapter to re-inject it at every persistence call, which is the architecturally correct form but adds boilerplate. Keeping it in domain entities is pragmatically simpler and is already working.
*Options:*
A) Keep `profileId` in domain entities as a plain `UUID` field — accept it as a pragmatic trade-off and document this decision in an ADR. ArchUnit rules stay as-is.
B) Remove `profileId` from domain entities; pass it as an explicit parameter to every persistence adapter method. Tighten the ADR-006 wording to reflect this. Update ArchUnit to flag UUID fields named `profileId` in domain packages.

**Q2.** ~~Should the net worth calculation use `opening_balance` only, or derive the running balance...~~ **RESOLVED — already implemented (Epic 8 Phase 1, Bug 2 fix).**
*Resolution:* Option A — `ProjectionCalculationEngine.computeNetWorth()` now calls `WealthServiceClient.getAccountBalance()` (the dedicated `GET /v1/accounts/{accountId}/balance` endpoint) instead of reading `opening_balance` directly off the account payload.

*Original question, preserved for context:*
Should the net worth calculation use `opening_balance` only, or derive the running balance from transaction history?
*Context:* `ProjectionCalculationEngine.computeNetWorth()` currently sums the `opening_balance` column from the `wealth.account` record. This is the static balance at account creation time. Credit and debit transactions after account creation are not reflected. This means the dashboard net worth figure drifts further from reality as transaction history grows. The correct formula is `opening_balance + sum(CREDIT transactions) - sum(DEBIT transactions)` per account. However, calling this adds a second REST call per account (or a dedicated endpoint), increasing gateway projection latency. The vacation planner in v0.5 will use the net worth figure for budget validation, so accuracy matters.
*Options:*
A) Fix now (v0.5 backlog): add `GET /v1/accounts/{accountId}/balance` endpoint on wealth service returning the computed running balance; call it from the engine during net worth computation.
B) Accept the current `opening_balance` approximation for now; add a note in the dashboard UI that the figure is "as of account setup date." Revisit before v1.0 with a dedicated balance ledger view.

**Q3.** ~~Should the `/errors` endpoint on the wealth service be proxied through the web gateway...~~ **RESOLVED — already implemented (Epic 8 Phase 2).**
*Resolution:* Option A — `getUploadErrors` is now proxied via `WealthServiceClient`/`WealthGatewayResource` and documented in `application/contract/gateway.yaml`. The gateway-bypass invariant is restored; no policy exception needed.

*Original question, preserved for context:*
Should the `/errors` endpoint on the wealth service be proxied through the web gateway, or is direct domain service access acceptable for this specific endpoint?
*Context:* ADR-002 and the CONTEXT_PRIMER both state "frontend talks only to gateway (8080)" — domain services (8081–8084) are internal. The `GET /accounts/{accountId}/uploads/{uploadId}/errors` endpoint was added to the wealth service in v0.4 but was not added to `WealthServiceClient` or `WealthGatewayResource`. The frontend currently calls the wealth service directly at port 8082 to retrieve error logs. This is the only known gateway bypass in the codebase.
*Options:*
A) Add the `/errors` proxy to `WealthServiceClient`, `WealthGatewayResource`, and `application/contract/gateway.yaml`. Regenerate the API client. This is the correct fix and restores the invariant.
B) Designate certain diagnostic/admin endpoints as "internal direct access permitted" — formalize this as a policy exception in the architecture docs. Risk: opens a precedent for bypassing the gateway.

**Q4.** ~~Is the synchronous `ProjectionCalculationEngine.refreshAll()` acceptable...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option A — add per-step try-catch isolation in `refreshAll()` so a failure in one compute step does not block the others. Stay synchronous overall. v0.5 target. Implement alongside the same per-step isolation principle already agreed for Q16 (Bug 3, Phase 4).

*Original question, preserved for context:*
Is the synchronous `ProjectionCalculationEngine.refreshAll()` acceptable for v1.0, or does it need async isolation before auth is added?
*Context:* `refreshAll()` runs all four compute steps synchronously in sequence. If the household service call times out (e.g., the service is stopped), the entire dashboard refresh fails with an unhandled exception — no partial snapshot is written. The method is also currently called from `ProjectionResource` as a synchronous HTTP request-response, meaning the frontend blocks until all four steps complete. For v0.5 (vacation planner) and v1.0 (auth, multiple users), this becomes a reliability concern: a single domain service slowdown blocks every user's dashboard.
*Options:*
A) Add per-step try-catch isolation in `refreshAll()` so a failure in one compute step does not block the others. Keep synchronous overall. Simple fix, v0.5 target.
B) Switch to async: `refreshAll()` returns immediately with `202 Accepted`; compute steps run in background threads or a Quarkus reactive pipeline. Frontend polls or receives a push notification when refresh is complete. Higher complexity, v0.6 target.
C) Keep synchronous for now; add a timeout per step and a global refresh timeout. If all steps succeed within the timeout, return 200. If any step times out, return 206 Partial Content with which snapshots were updated. Middle ground, v0.5 target.

---

## Business Analyst Review Questions — 2026-06-29

**Q5.** ~~Should the Physical Assets feature be treated as a v0.5 delivery gap or deferred...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option B — scope the Physical Assets frontend as a small v0.4.1 patch release, delivered before v0.5 work begins. The v0.5 Action Center can then rely on it for vehicle compliance deadlines as originally planned.

*Original question, preserved for context:*
Should the Physical Assets feature be treated as a v0.5 delivery gap (build the frontend now) or formally deferred to a named milestone?
*Context:* The `wealth.physical_asset` table and the backend API (`POST /physical-assets`, `GET /physical-assets`, etc.) are implemented and in production. No frontend page exists. The user has no way to view or manage physical assets through the UI. The v0.3 requirements (REQUIREMENTS_wealth_domain.md, Epic 7) specify vehicle asset compliance tracking including PUC, insurance, and road tax deadlines. The v0.5 Consolidated Action Center plan explicitly references "vehicle compliance deadlines" as a data source — which means the Action Center cannot be built without physical assets data being accessible in the UI first.
*Options:*
A) Treat as a v0.5 delivery gap — build the Physical Assets frontend page in v0.5 before starting the Action Center. Update ROADMAP.md to reflect this explicitly.
B) Formally scope the Physical Assets frontend as v0.4.1 (a small patch release) and deliver it before v0.5 work begins.
C) Remove vehicle compliance deadlines from the v0.5 Action Center scope entirely — defer the full Physical Assets lifecycle (including compliance deadlines) to v0.6.

**Q6.** ~~Should the inventory item lifecycle (consumed / in-stock state) be added...~~ **RESOLVED — 2026-06-30 (product owner, custom).**
*Resolution:* Closest to Option A but with different semantics than originally framed. Add an `is_consumed` flag, but it does **not** mean "the grocery item was used up" — it means "this record has been successfully used in a calculation." All inventory data stays permanently available (no deletion, no expiry) so the application can track year-over-year progress from the start of usage. Scope the flag + this semantics in v0.5.

*Original question, preserved for context:*
Should the inventory item lifecycle (consumed / in-stock state) be added in v0.5, or is the current append/delete ledger sufficient for the near term?
*Context:* REQUIREMENTS_household_domain.md v0.3 Epic 4 states: "The system must support marking items as consumed or restocking them." The current inventory implementation is a flat ledger — items are created and deleted but have no lifecycle state. A user cannot distinguish between items that are in stock and items that have been used. The requirements were written for v0.3 but were not delivered. The Quarantine Protocol for grocery CSVs (v0.4 requirement, also not delivered) depends on the inventory import flow, which itself has not been built. The lifecycle state question must be answered before the inventory import and quarantine features are scoped.
*Options:*
A) Add a minimal `is_consumed BOOLEAN DEFAULT FALSE` column and a toggle button in v0.5. No quarantine protocol needed until CSV import exists.
B) Skip lifecycle state entirely for now — treat inventory as a reference ledger only (not a live stock tracker). Remove "consumed/restock" from active requirements and move it to v1.3 (Export/Import) when batch management is planned.
C) Scope the full inventory lifecycle (add, consume, restock, expiry date) as part of the v0.5 Household enhancements alongside the inventory CSV import.

**Q7.** ~~Should manual transaction entry be added to the Wealth domain...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option C — add `POST /v1/accounts/{accountId}/transactions` + a form in the Transactions page now, tagged with a distinct `source = MANUAL` flag in transaction metadata so manually-entered rows stay distinguishable from CSV-sourced data for future AI analysis.

*Original question, preserved for context:*
Should manual transaction entry (adding a single transaction without a CSV upload) be added to the Wealth domain, and if so, in which milestone?
*Context:* The current wealth domain only accepts transactions via CSV upload. There is no UI form or API endpoint for manually entering a single transaction (e.g., a cash purchase, an ATM withdrawal, a peer-to-peer transfer). For a household user who makes frequent cash or informal transactions, the CSV-only path creates a data gap: those transactions are either omitted from the ledger or require constructing a synthetic CSV file. The REQUIREMENTS_wealth_domain.md does not explicitly include or exclude manual transaction entry — it focuses on CSV-based ingestion. Adding a manual entry form would require a new `POST /transactions` endpoint on the wealth service and a form in the Transactions UI.
*Options:*
A) Add manual transaction entry as a v0.5 feature — `POST /v1/accounts/{accountId}/transactions` endpoint (single transaction body) plus a form in the Transactions page. No upload required.
B) Defer to v1.3 (Export/Import) when the full data management framework is planned — keep the wealth ledger as an upload-only source of truth to preserve data traceability.
C) Allow manual entry but tag it with a distinct `source = MANUAL` flag in the `statement_upload` or transaction metadata, so it remains distinguishable from CSV-sourced data for future AI analysis.

**Q8.** ~~Should the original Epic 8 advanced financial engine features be rescheduled...~~ **SUPERSEDED by Q13** (expanded scope) — see Q13 resolution below; same milestone decision applies to both.

*Original question, preserved for context:*
The REQUIREMENTS_wealth_domain.md v0.4 section ("The Mahesh Summation Rule," EMI arbitrage, operating budget cap) was not delivered. Should these advanced financial engine features be rescheduled into a named future milestone, or are they still active requirements for an upcoming release?
*Context:* The wealth domain requirements at v0.4 describe a sophisticated CQRS calculation engine: dynamic header summation, offset account arbitrage tracking, EMI reallocation triggers, and monthly budget cap alerts. None of these were delivered in v0.4 — the version instead focused on error handling (malformed CSV rejection, structured error logging, dedup key fix). The gap exists because the implementation team reframed v0.4 as an error-handling milestone while the requirements document retained the original "Advanced Business Logic" definition. These features require deeper financial modelling and likely depend on the net worth calculation fix (Q2) before they can be meaningfully computed.
*Options:*
A) Move the advanced financial engine features (Use Cases 8.1–8.3) into v0.6 as a dedicated "Financial Intelligence" sub-milestone. Update REQUIREMENTS_wealth_domain.md to reflect the corrected milestone.
B) Scope a subset (operating budget cap alert and net worth corrected calculation) into v0.5; defer EMI arbitrage and reallocation triggers to v1.3.
C) Treat the original v0.4 requirements as superseded by the implemented error-handling scope — archive the advanced engine use cases as future backlog items without a committed milestone, revisit at v1.0 planning when persistent data makes financial modelling meaningful.

---

## QA Review Questions — 2026-06-29

**Q9.** ~~What is the minimum acceptable Java test coverage percentage...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option A — a single project-wide line coverage floor of 70%, enforced in CI (not pre-commit). Profile and health domain layers are currently below this; fix them as v0.6 testing work before enabling the gate.

*Original question, preserved for context:*
What is the minimum acceptable Java test coverage percentage (line and branch) before coverage enforcement is added to the build gate?
*Context:* Java JaCoCo reports are generated on every `./gradlew test` run but no coverage threshold is configured — a build with 0% coverage passes. The v0.6 milestone includes "Pre-commit test gate via Gradle" but does not specify a numeric floor. Wealth domain adapter tests are the most complete; profile domain has near-zero domain-layer coverage. Setting a single project-wide floor may either be unachievable (profile domain would fail immediately) or too low to be meaningful. A per-module floor is more precise but requires individual configuration in each `build.gradle.kts`.
*Options:*
A) Set a project-wide line coverage floor of 70% enforced in CI (not pre-commit). Accept that profile and health domain layers are below threshold and fix them as part of v0.6 testing work before enabling the gate.
B) Set per-module floors calibrated to current coverage: wealth adapters at 80%, household adapters at 80%, health adapters at 70%, profile adapters at 60%, domain layers at 50%. Raise floors as coverage improves over v0.6 and v1.0.
C) Defer the coverage gate entirely to v1.0 — focus v0.6 on writing the missing tests first, then set a floor once coverage is measured across all modules.

**Q10.** ~~Should Playwright E2E tests be required to pass before a PR is merged...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option A — add E2E tests to CI as a required check using a lightweight stub backend (MSW or WireMock). Cover at minimum: upload success, upload error panel display, and dashboard refresh. Keep real domain service startup out of CI.

*Original question, preserved for context:*
Should Playwright E2E tests be required to pass before a PR is merged, or should they remain advisory-only (run manually before releases)?
*Context:* 17 Playwright E2E tests exist across 5 spec files but they require a live dev server at `http://localhost:3000` and are not run in any CI step. The E2E suite has no coverage for CSV upload flow, the upload error panel, skipped-duplicates panel, household domain pages, or dashboard refresh — areas that are most likely to break silently when the frontend and backend contracts drift. Running E2E in CI requires either a Docker-compose setup with all four domain services or a stub server. SonarQube is already excluded from Codespaces for resource reasons; adding a full E2E run would further pressure the free-tier environment.
*Options:*
A) Add E2E tests to CI as a required check using a lightweight stub backend (MSW or WireMock). Cover at minimum: upload success, upload error panel display, and dashboard refresh. Keep domain service startup out of CI.
B) Keep E2E as a manual pre-release gate only. Document the required run steps in CICD.md. Accept the risk that contract drift is caught only at release time.
C) Add E2E to CI using Docker Compose with all services. Exclude from Codespaces, run only on GitHub Actions on PR-to-main. Adds ~5 minutes to CI time.

**Q11.** ~~Should contract tests be added as a formal quality gate before v1.0...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option A — add OpenAPI schema validation using Atlassian Swagger Request Validator in integration tests; each `@QuarkusTest` in the adapter layer validates response bodies against the domain contract YAML. Target the v0.6 testing milestone.

*Original question, preserved for context:*
Should contract tests (OpenAPI schema validation of live service responses) be added as a formal quality gate before v1.0, and which tool/approach is preferred?
*Context:* No contract tests exist. The four domain OpenAPI contracts in `application/contract/` and the gateway contract are the source of truth for both the frontend generated client and the `WealthServiceClient`/`HealthServiceClient`/etc. MicroProfile Rest Client interfaces. There is currently no automated check that a live service response matches its OpenAPI schema. The risk: a domain service developer changes a response shape (adds a required field, renames a field), the contract file is not updated, and the gateway silently passes bad data to the frontend. This has already happened informally (the `opening_balance` field name is used directly in `ProjectionCalculationEngine` without any schema validation).
*Options:*
A) Add OpenAPI schema validation using Atlassian Swagger Request Validator in integration tests — each `@QuarkusTest` in the adapter layer validates response bodies against the domain contract YAML. Add as part of v0.6 testing milestone.
B) Use Pact (consumer-driven contract testing): the gateway defines consumer contracts against each domain client, verified when domain tests run. Higher setup cost but provides bidirectional enforcement. Target v1.0.
C) Treat the generated `web/src/api/generated.ts` as the contract test proxy — if `npm run generate:api` fails or produces type errors, the contract has drifted. This is already partly enforced via the TypeScript compiler. Accept this as sufficient for now.

**Q12.** ~~Should the `TransactionPanacheRepository` implicit ownership chain be documented as a trade-off, or fixed with an explicit filter...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option A — add `profile_id` as a secondary filter to `findByAccountId()` and `existsByDeduplicationKey()` in `TransactionPanacheRepository`. Update the `TransactionRepository` output port interface to accept `profileId` as a parameter. Document in an ADR-006 addendum.

*Original question, preserved for context:*
The `TransactionPanacheRepository` does not filter by `profile_id` directly — it relies on the caller having verified account ownership. Should this implicit chain be documented as a deliberate ADR trade-off, or should an explicit secondary filter be added to the repository queries?
*Context:* `findByAccountId()` and `existsByDeduplicationKey()` query only on `accountId`. The upstream caller (`StatementUploadService`) calls `accountRepo.findById(accountId)` first, which does check the account exists, but does not verify `account.profileId == requestingProfileId` — that check is in `AccountPanacheRepository.findById()` which is scope-neutral. The implied chain is: "if account X belongs to the wrong profile, the account lookup would still succeed because findById does not filter by profile_id either." A direct `profile_id AND accountId` filter in the transaction queries is the safest fix but adds a parameter to the `TransactionRepository` output port interface, which is a domain-layer change.
*Options:*
A) Add `profile_id` as a secondary filter to `findByAccountId()` and `existsByDeduplicationKey()` in `TransactionPanacheRepository`. Update the `TransactionRepository` output port interface to accept `profileId` as a parameter. Document in ADR-006-addendum.
B) Add an account ownership assertion in `StatementUploadService` before calling any transaction repository method: verify `account.getProfileId().equals(requestingProfileId)`. No interface change needed. Document the check explicitly so future developers know it is load-bearing.
C) Accept the current implicit chain as a pragmatic trade-off for now. Add a comment in `TransactionPanacheRepository` explaining the reliance on upstream ownership verification. Revisit when profile_id auth enforcement is added in v1.0.

---

## Business Analyst Review Questions — Automated Wealth Intelligence Engine — 2026-06-30

*Context for all questions below: these arise from expanding `REQUIREMENTS_wealth_domain.md` Epic 8 (the Mathematical Engine & Zero Leakage) into six concrete use cases, scoped from the product owner's existing manual markdown-and-Python financial tracking workflow. They are more specific successors to Q8, which already flagged that Epic 8 was undelivered and needed a milestone decision. Do not start implementation against Epic 8 until these are answered.*

**Q13.** ~~Should the expanded Epic 8 (Use Cases 8.1–8.6) be assigned to v0.6...~~ **RESOLVED — 2026-06-30 (product owner). Also resolves Q8.**
*Resolution:* Option A — assign to v0.6, sequenced after the v0.5 net-worth-formula fix (Q2 — already delivered, so the gate condition is met). Extend the v0.6 milestone focus to include "Financial Intelligence Engine" alongside the existing testing-foundation re-scope (Q9, Q10, Q11).

*Original question, preserved for context:*
Should the expanded Epic 8 (Use Cases 8.1–8.6) be assigned to v0.6, folded into a new v0.4.2 patch milestone, or kept as unscheduled backlog?
*Context:* Q8 already asked whether the original three-use-case Epic 8 should move to v0.6, be partially scoped into v0.5, or be archived. That question is now superseded by a much larger scope — six use cases instead of three, including a five-type goals engine and an automated validation gate. The BA review appended to `ROADMAP.md` on 2026-06-30 recommends v0.6 (after the v0.5 net-worth-formula fix lands, since the engine's accuracy depends on it) but explicitly defers the final call to the product owner.
*Options:*
A) Assign to v0.6, sequenced after the v0.5 net-worth-formula fix (Q2) is delivered. Rename or extend the v0.6 milestone focus to include "Financial Intelligence Engine" alongside the existing testing-foundation re-scope.
B) Carve out a dedicated v0.4.2 patch milestone immediately after the current v0.4, run in parallel with v0.5 cross-domain work since this epic has no cross-domain dependency.
C) Leave unscheduled — treat `REQUIREMENTS_wealth_domain.md` Epic 8 as approved future scope but do not commit it to any milestone until v0.5 and v0.6 testing work is complete.

**Q14.** ~~Should the five goal formulas... be hardcoded or generalized?~~ **RESOLVED — 2026-06-30 (default, high confidence).**
*Resolution:* Option C — hardcode the five named types now; flag generalization as v1.3+ backlog only if a sixth formula-driven goal is ever actually requested. Matches CLAUDE.md's explicit project philosophy ("don't design for hypothetical future requirements... three similar lines is better than a premature abstraction") and the BA's own recommendation. No generalized engine built speculatively.

**Q15.** ~~Should non-account assets be manual valuation or derived?~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option B — manual valuation entry now (real estate, gold, vehicles, gratuity, insurance), with the data model leaving room for a future "refresh from external price index" automation. Nothing automated built now. Important: per ADR-017's family rollup, these assets must be included in the household net worth total (they're ~70% of total assets per `Financial_Data.md`) — the manual valuation entry path must feed the same rollup payload, not a separate disconnected figure.

**Q16.** ~~Is the validation engine a blocking gate or advisory?~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option B — partially blocking. A CRITICAL FAILURE withholds only the specific affected snapshot key (e.g., `WEALTH_NET_WORTH_FAMILY` shows "unavailable — validation failure" instead of a silently wrong number), while unaffected snapshot keys (vitals, events) still refresh normally. This is the same per-step isolation principle already being fixed as Bug 3 in Phase 4 — implement together.

**Q17.** ~~Where does the household income figure come from?~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option C — auto-detect from recurring CREDIT transactions (same amount ± tolerance, same day-of-month, 3+ consecutive months), with manual override available. Higher engineering cost than manual entry, accepted deliberately. Note for Phase 4 design: `Financial_Data.md` shows **two** separate recurring income streams (Ketan's salary + Shweta's rental income) — the detector must sum multiple independently-recurring streams, not assume a single salary credit. Flag this as a Phase 4 scoping note for the architect/wealth-developer, not a new open question — the auto-detect approach already implies handling N streams, just don't let the implementation assume exactly one.

**Q18.** ~~Does the wealth goals engine replace, coexist with, or absorb `household.goal`?~~ **RESOLVED — 2026-06-30 (default, low-risk, easily revisited).**
*Resolution:* Option A — keep both permanently separate, clearly labeled: `household.goal` stays a user-created savings target (e.g., "house deposit"); the new wealth goals engine is the five fixed formula-driven goals, shipped as a distinct feature. Zero migration risk, zero rework if this turns out wrong later — the two systems don't share schema. Revisit only if real usage shows the distinction is confusing.

**Q19.** ~~Does Epic 8 replace the Python scripts immediately or run in parallel?~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Option B — cut over as soon as the in-app engine ships its first complete version (all of Phases 1–4 delivered). No parallel-run safety net. **This raises the bar on QA verification for Phase 4 specifically** — since there is no side-by-side comparison period against the trusted Python output, QA must verify Phase 4's goals/validation engine against `Financial_Data.md`'s known-correct figures directly before sign-off, not just via unit tests in isolation.

**Q20.** ~~Should account classification be one-time or editable?~~ **RESOLVED — 2026-06-30 (default, matches existing pattern).**
*Resolution:* Option A — editable via the existing `PUT /accounts/{id}` metadata path, no dedicated classification UI built now. Identical precedent already exists for `physical_asset.metadata` edits. If reclassification turns out to be frequent enough to be annoying, a dedicated view is a small, isolated addition later — not worth building speculatively now.

---

## Architect Review Questions — Epic 8 Implementation Plan — 2026-06-30

*Context for all questions below: raised while producing `documents/EPIC8_IMPLEMENTATION_PLAN.md`, the phased build plan for the expanded Epic 8 (Use Cases 8.1–8.6). These are genuine product/policy tradeoffs surfaced during planning — the joint-account schema design itself (ADR-016) was resolved without escalation; only the financial-modeling and category-naming decisions below need the product owner.*

**Q21.** ~~Does a joint account's transaction activity count toward both owners' individual net worth figures...~~ **RESOLVED — 2026-06-30 (product owner).**
*Resolution:* Superseded by a broader decision — the wealth dashboard's primary view is a **consolidated family/household rollup**, not per-individual-profile totals (confirmed by the product owner: he manages all family finances as head of household, and his own `Financial_Data.md` is titled "Family Financial Data — Combined," with one net worth and one goal set, not per-person figures). This makes the original A/B/C framing moot — the Kotak joint account simply contributes to the one family total like every other account. Individual profiles (Ketan, Shweta, Gayan, Vamika) remain for attribution only (e.g., "Gayan's SIP portfolio: ₹6,000" shown as a sub-breakdown), not as separate dashboards.
*Secondary resolution (designated owner for ADR-016):* For the Kotak joint expense account specifically, **Shweta is the designated `profile_id` of record** (primary account holder), with Ketan recorded in `metadata.joint_owners` for attribution. This was a clean, separate answer the product owner gave directly — no ambiguity.
*Follow-on:* This reopens the schema/aggregation question as a new architecture decision — see Q25.

**Q25.** ~~How should household-level aggregation be added to `ProjectionCalculationEngine` without violating ADR-006...~~ **RESOLVED — 2026-06-30 (architect call, per ADR-017).**
*Resolution:* No ADR-006 amendment needed. ADR-006 governs domain-adapter SQL query scoping; `ProjectionCalculationEngine` does no SQL against domain schemas — it composes per-`profile_id` REST calls (already ADR-006-compliant on the domain side) and aggregates in gateway memory. Looping the engine's existing per-profile compute calls across every `profile.profile` row sharing one `admin_id` (resolved via the already-existing `ProfileServiceClient.listProfiles(adminId, isActive)`) is therefore not a violation — full reasoning in ADR-017.
*Storage shape:* One new family-scoped snapshot row per metric, keyed by the admin's own SELF `profile_id` (not `admin.id` — reuses the existing identifier space) and a new `..._FAMILY` snapshot key (e.g. `WEALTH_NET_WORTH_FAMILY`). Old singular per-profile keys are not deleted, just no longer the dashboard's primary read path.
*Per-member breakdown:* Nested inside the family payload's `members[]` array (matches the product owner's `assets_06062026.json` reference shape) — not a separate snapshot row, not a separate REST call. Confirmed by the product owner's follow-up clarification: only he (the admin) ever logs in; Shweta/Gayan/Vamika are data-attribution targets, not independent sessions. The "show just Shweta's accounts" drill-down is a client-side filter over the one family payload, not a second compute path.
*Scope:* Wealth-domain Epic 8 outputs only (net worth, goals, EMI, validation). `HEALTH_VITALS_SUMMARY` and `HOUSEHOLD_EVENT_SUMMARY` stay per-profile — vitals/events are inherently per-person, never summed.
*Full design:* `documents/ARCHITECTURE_DECISIONS.md` ADR-017.

**Q22.** ~~Should the five hardcoded expense categories...~~ **RESOLVED — 2026-06-30 (product owner, via direct file reference).**
*Resolution:* Confirmed Option A. `Financial_Data.md` Section 3 lists exactly: Household Core (₹55,000), Child Related (₹20,878), Maintenance (₹22,234), Discretionary (₹14,000) — matching the proposed enum exactly. The fifth line item, "UNACCOUNTED BUFFER" (₹12,112), is **not a transaction category** — it is a derived planning slack value (`100k operating cap − sum of the 4 itemized categories`), recalculated from the budget cap, not assigned to any transaction. `UNCATEGORIZED` (the Phase 2 default for any untagged transaction) is a distinct, necessary technical state and is not a replacement for "buffer." No enum change needed: `HOUSEHOLD_CORE`, `CHILD_RELATED`, `MAINTENANCE`, `DISCRETIONARY`, `UNCATEGORIZED` ship as planned.

**Q23.** ~~For Phase 4's policy/threshold values, is a new `wealth.policy_setting` key-value table the right home...~~ **RESOLVED — 2026-07-01 (product owner + architect).**
*Resolution:* Store in `profile.admin.policy_settings JSONB NOT NULL DEFAULT '{}'` — a new column on the existing admin entity (new Flyway migration in the profile module). Policy is admin-scoped (one set per household), rarely changes, and belongs near the identity layer, not in transactional wealth data. Analogy: `blood_type` on `profile.profile`. No new table needed in any schema. Keys: `monthly_budget_cap`, `debt_crossover_threshold_percent`, `freedom_runway_months`, `insurance_multiple`, `year_one_annual_target`. The gateway reads it via a new `profileServiceClient.getAdmin(adminId)` call — admin_id is already resolved in every Phase 3 family rollup method (`resolveAdminId()`), so no new resolution step needed.

**Q24.** ~~Should Phase 2's manual transaction-category tagging reuse the existing single-row update path...~~ **DEFAULTED — 2026-06-30 (auto mode, low-risk call).**
*Resolution:* Option B — minimal bulk-tag-by-selection endpoint (tag a list of transaction IDs in one call) ships in Phase 2. Rationale: a 12-month statement backlog tagged one row at a time is unusable, and the bulk-by-ID-list shape is small (no pattern-matching, no rules, no admin UI) so it does not meaningfully encroach on v1.3's Rule-Based Tagging Engine scope. Flagging this as a default I made rather than a silent decision — say the word if you'd rather start with single-row only.

---

## v0.5 Readiness Review Questions — 2026-07-02

*Context for all questions below: raised during the architect's pre-v0.5 code-verification pass (Vacation Planner + Consolidated Action Center readiness). These surfaced as genuine product-owner decisions gating v0.5 phased work, distinct from things the architect could decide independently.*

**Q26.** ~~Which frontend state-management approach (PROP-005) should be adopted before the Consolidated Action Center starts...~~ **RESOLVED — 2026-07-02 (product owner).**
*Resolution:* Option A — React Query for server state, existing Context API kept for auth/global state, no Redux/Zustand. Full rationale and rejected alternatives: see `documents/ARCHITECTURE_DECISIONS.md` ADR-018. Unblocks v0.5 Phase 3 (Consolidated Action Center).

*Original question, preserved for context:* PROP-005 (frontend state management) was still marked Open with no ADR despite ROADMAP.md flagging it as a pre-Action-Center blocker since 2026-06-29. Options were React Query + local state (A), Redux Toolkit (B), Zustand (C).

**Q27.** ~~Where should the Vacation Planner page live in the frontend nav?~~ **RESOLVED — 2026-07-02 (product owner).**
*Resolution:* Under Household nav, route `/household/vacation-planner`, even though the feature reads cross-domain data (wealth liquid savings + household calendar + wealth physical assets).

**Q28.** ~~Should the `TransactionResource`/`TransactionService` profile_id threading gap be fixed now or deferred?~~ **RESOLVED — 2026-07-02 (product owner).**
*Resolution:* Fix now, as part of v0.5 Phase 0 (small independent CRUD/fix work), not deferred. Scope: add `profile_id` query param to `TransactionResource.listTransactions()`, thread it through `TransactionUseCase.listByAccount()` and `TransactionService.listByAccount()` (currently hardcodes `null` with an explicit comment opting out — see `TransactionService.java:32-37`), reusing the filter logic that already exists and works correctly in `TransactionPanacheRepository` (verified present at `findByAccountId`/`existsByDeduplicationKey`/`sumAmountByTxnType`, just never invoked from the HTTP layer with a real profileId).

**Q29.** ~~Should physical asset PUC/insurance/road-tax expiry dates be promoted from JSONB metadata to typed columns before the Vacation Planner's asset-compliance check is built?~~ **RESOLVED — 2026-07-02 (product owner).**
*Resolution:* Keep as JSONB (no schema promotion). The Vacation Planner's compliance check parses `metadata.puc_expiry`/`metadata.insurance_expiry`/`metadata.road_tax_expiry` (string dates from the existing `Map<String,String>` metadata) defensively in the gateway — null-safe, tolerant of missing/malformed keys, no DB-level date validation added. Matches the existing precedent (`wealth.physical_asset.metadata` documented shape in `V2__physical_assets.sql`).

**Q30.** ~~What counts as a "biometric streak gap" for the Consolidated Action Center (v0.5 Phase 3)?~~ **RESOLVED — 2026-07-02 (product owner).**

*Resolution:* Tracked vital types: WEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR_FASTING ("core 3" — not all 10 VitalType values). Gap threshold: 30 days without a reading. Scope: per-profile, evaluated independently for each household member (consistent with ADR-017's rule that vitals stay per-person, never rolled up). Engineering default not explicitly asked but applied consistently: a vital type with zero readings ever is also flagged as a gap (treated as an infinite gap) rather than silently skipped — matches the "honest gap reporting" precedent set by `computeCategoryValidation` in Epic 8 Phase 1.

*Original question, preserved for context:* The v0.5 Consolidated Action Center scope lists "biometric streak gaps" as one of three alert sources (alongside upcoming calendar events and vehicle compliance deadlines), but no acceptance criteria existed anywhere in `BUSINESS_REQUIREMENTS.md` or the health domain-state file. Needed product-owner input on: which vital types count, what gap threshold triggers an alert, and whether the gap is evaluated per-profile or family-wide.

*Implemented 2026-07-02:* `ProjectionCalculationEngine.computeActionCenterAlerts()` → `ACTION_CENTER_ALERTS_FAMILY` snapshot key. See `documents/domain-state/wealth.md` and `documents/domain-state/health.md` for implementation detail.

---

## Flyway/Contract Consolidation Questions — 2026-07-03

*Context: raised while briefing agents for the Flyway consolidation plan (Step 4) and shared-contract consolidation plan (Step 5) requested this session. Per instruction, logged here rather than blocking — proceed with the requested direction for planning purposes, but these are material policy reversals worth an explicit confirmation before any actual migration/contract file is executed.*

**Q31.** ~~The new Flyway consolidation instruction says DB scripts must **not** include any DB-level constraints (FK, CHECK) — all enum/business-rule validation moves to the contract layer only...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Remove CHECK constraints, but keep FKs for referential integrity.

*Original question, preserved for context:*
The new Flyway consolidation instruction says DB scripts must **not** include any DB-level constraints (FK, CHECK) — all enum/business-rule validation moves to the contract layer only. This reverses the DB constraint philosophy currently documented in `CLAUDE.md` ("Keep in DB: NOT NULL, PK, FK, UNIQUE, and business-rule checks like `amount >= 0`... Do NOT add to DB: enum discriminators only"). The new instruction goes further — no FK, no CHECK at all, not just enums.
*Proceeding as instructed* for the Flyway consolidation plan (Step 4.4). Flagging because this is a bigger reversal than "don't over-constrain enums" — it also drops referential-integrity FKs, which today catch real bugs (e.g. orphaned `profile_id` rows) at the DB layer regardless of application-layer bugs. Worth an explicit yes/no on FK removal specifically (vs. just CHECK/enum removal) before the plan is executed, and `CLAUDE.md` should be updated to match whichever direction is confirmed.

**Q32.** ~~The instruction to "override/replace" existing Flyway scripts during consolidation (since still in dev phase) reverses the current rule in `CLAUDE...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Yes, override existing V1 migrations (requires manual dev DB resets).

*Original question, preserved for context:*
The instruction to "override/replace" existing Flyway scripts during consolidation (since still in dev phase) reverses the current rule in `CLAUDE.md`: "Never edit a committed migration — create a new versioned file." Proceeding as instructed for the *plan* (Step 4.1) since this is an explicit, scoped, current-session directive — but noting that once any domain's migrations are actually rewritten in place, `flyway_schema_history` in any existing local/dev database will need a manual reset (`DROP SCHEMA` + re-migrate — `flyway repair` won't help across a rewritten V1) for every developer's local DB, not just the one this session is running against. Confirm this is acceptable before Step 4's plan moves from "plan" to "execute."

**Q33.** ~~Step 4...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Primary tables have profile id, and rest tables either directly or indirectly use profile id to associate data with a profile. Still there may be some tables that do not need profile attachment.

*Original question, preserved for context:*
Step 4.3 says every domain's script "must include `profileId`" except the core/profile domain itself — some existing tables reference `profile_id` only indirectly today (e.g. `wealth.transaction` has no `profile_id` column of its own; it's resolved via a join through `wealth.account.profile_id`, per ADR-006/ADR-019's documented profileId-as-domain-field trade-off). Does "must include profileId" mean every table gets its own direct `profile_id` column (denormalizing away the current join-through-account pattern), or only every domain's *root* aggregate table? Left for the architect agent to investigate current schemas and propose in the plan — flagging here so the tradeoff is visible for confirmation, not decided silently.

*Resolution proposed in `documents/flyway-consolidation-plan.md` (Phase 0.3) — not yet product-owner-confirmed.* Recommendation: "must include profileId" means every domain's **root/primary aggregate table** gets a direct column; child/detail tables unambiguously owned by exactly one already-scoped parent row (e.g. `wealth.transaction` via `account_id`, `wealth.statement_upload` via `account_id`, `wealth.upload_error_log` via `upload_id`) do not need their own copy. Extends the ADR-019 domain-entity-layer reasoning to the schema layer for consistency. See the plan document for the full table-by-table application of this rule.

---

## Flyway Consolidation Plan — New Questions Raised — 2026-07-03

*Context: raised while producing `documents/flyway-consolidation-plan.md`, the phased plan for consolidating each domain's Flyway migrations into one script per domain, per the product owner's explicit current-session instruction (see Q31-Q33 above for the policy overrides this plan operates under). These are genuine open points beyond what Q31-Q33 already covered — proceeding with the plan's stated defaults for now, confirm before Phase 6 (execution) begins.*

**Q44.** ~~Should `wealth...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* We should limit varchar length. We should use varchar2 (or varchar) and where ever applicable we should use limited length like name should have only varchar(50).

*Original question, preserved for context:*
Should `wealth.account.account_name`/`institution_name` and `wealth.physical_asset.asset_name` widen from `VARCHAR(100)` to `VARCHAR(200)` (household's precedent — `calendar_event.title`, `inventory_item.item_name`, `goal.goal_name` are all 200) or `VARCHAR(150)` (profile's precedent — `profile.full_name`, `admin.display_name`)? No technical reason found for either domain's original choice — this is pure standardization. The plan (Phase 4, "Q47 resolution") recommends 200 as the project-wide standard since household has 3 independent uses vs. profile's 2, and 200 is a strict superset. Confirm before Phase 1/2 scripts are finalized for execution.

**Q45.** ~~Before dropping the 8 business-rule CHECK constraints identified in the consolidation plan (`amount >= 0`, `value_primary > 0`, `end_date >= start_date` ×2, `quantity > 0`, `target_amount > 0`, `current_amount >= 0`, `chk_bp_secondary_value`, `chk_doctor_name_required`), has each domain's application/domain layer been confirmed to already independently enforce the same rule? This is different in kind from dropping enum CHECKs (which are already redundant with contract validation) — these are structural invariants `CLAUDE...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* We need to know these rules and apply them into the contract, as of now code might not be enforcing it.

*Original question, preserved for context:*
Before dropping the 8 business-rule CHECK constraints identified in the consolidation plan (`amount >= 0`, `value_primary > 0`, `end_date >= start_date` ×2, `quantity > 0`, `target_amount > 0`, `current_amount >= 0`, `chk_bp_secondary_value`, `chk_doctor_name_required`), has each domain's application/domain layer been confirmed to already independently enforce the same rule? This is different in kind from dropping enum CHECKs (which are already redundant with contract validation) — these are structural invariants `CLAUDE.md` currently says to keep in the DB specifically because they're not enum lists. The plan could not verify Java-layer validation exhaustively from schema files alone. Recommend a dedicated verification pass (candidate: `quality-manager` or each domain developer agent, one pass per domain) before Phase 2/3/4 scripts execute — a silently-missing validation here is a correctness regression accepted blind, not just a reduced safety margin.

**Q46.** ~~The consolidation instruction named "no FK, no CHECK constraints" specifically...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Keep UNIQUE constraints.

*Original question, preserved for context:*
The consolidation instruction named "no FK, no CHECK constraints" specifically. It did not mention UNIQUE constraints. Should `uq_transaction_dedup` (wealth.transaction's 5-field natural dedup key) and `uq_admin_email` (profile.admin) also be dropped for consistency with the stated spirit of "validation moves to the contract/app layer," or kept as a narrower, literal reading of the instruction (FK/CHECK only, UNIQUE stays)? The plan defaulted to dropping them per the broader spirit, but this is a literal-reading gap worth an explicit yes/no — `uq_registration_number` (physical_asset) and `uq_admin_self_profile` (profile) already have confirmed app-layer equivalents per existing domain-state notes, but `uq_transaction_dedup`'s app-layer equivalent (the multi-field dedup check in `StatementUploadService`) needs the same Q45-style confirmation before this is safe to drop.

**Q47.** (Resolved within the plan itself, logged here for traceability — no action needed unless overturned.) See Q44 above — 200 vs 150 for "entity name" columns. Resolved as 200 in `documents/flyway-consolidation-plan.md` Phase 4.

**Q48.** ~~Should the per-developer local DB reset required at Phase 6 cutover (every developer must `DROP SCHEMA ...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Yes, create a helper script and assign to devops agent.

*Original question, preserved for context:*
Should the per-developer local DB reset required at Phase 6 cutover (every developer must `DROP SCHEMA ... CASCADE` and re-migrate fresh, since `flyway repair` cannot reconcile a rewritten `V1__` against old checksums) be automated via a new `scripts/reset-local-db.ps1`/`.sh` helper, rather than relying on each developer running the correct manual commands themselves? Recommend routing to the `devops` agent once Phase 6 is authorized to execute — out of scope for this planning pass, but a real rollout risk (a developer who misses this step gets a broken local Flyway run, not an obvious error) if left unaddressed.

---

## Shared OpenAPI Contract Consolidation Questions — 2026-07-03

*Context: raised while writing `documents/contract-consolidation-plan.md` (shared cross-domain OpenAPI components — error responses, `profile_id` param, pagination — consolidated into `application/contract/shared.yaml`, referenced via `$ref` from every domain contract). Full detail in that plan document; summarized here per the reserved Q49-Q53 numbering for this task.*

**Q49.** ~~The existing `shared...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Standardize profile_id as a required parameter across all domains first.

*Original question, preserved for context:*
The existing `shared.yaml` `ProfileIdParam` is a required **path** parameter. `wealth.yaml`/`gateway.yaml` need `profile_id` as an **optional query** parameter; `health.yaml`/`household.yaml` need it as a **required query** parameter. Three distinct shapes share one field name today. Plan proposes adding `ProfileIdQueryParam` (optional) and `ProfileIdRequiredQueryParam` (required) alongside the existing path variant. Confirm naming, or whether required-ness should first be standardized across domains as a product decision (e.g., should wealth's account list actually require `profile_id` too?) before adding a second/third shared parameter to match today's inconsistency.

**Q50.** ~~`wealth...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* The pagination should be part of shared contract and every domain must use same pagination.

*Original question, preserved for context:*
`wealth.yaml`'s v0.6 `listTransactions` endpoint uses a third, incompatible pagination shape (`page`/`size`, 0-indexed integers) versus the `page_size`/`page_token` shape used by `listAccounts`/`listPhysicalAssets` and `shared.yaml`. Plan treats this as pre-existing and out of scope for consolidation. Confirm acceptable, or schedule a separate future pagination-unification pass.

**Q51.** ~~Needs verification **before** Phase 1 of the contract-consolidation plan begins: does `openapi-typescript` (tool behind `npm run generate:api`) correctly resolve external multi-file `$ref` (`...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Yes, do a spike to verify openapi-typescript behavior.

*Original question, preserved for context:*
Needs verification **before** Phase 1 of the contract-consolidation plan begins: does `openapi-typescript` (tool behind `npm run generate:api`) correctly resolve external multi-file `$ref` (`./shared.yaml#/components/...`) from `application/contract/gateway.yaml`, and does it emit an identical TS type for a schema reached via external named `$ref` vs. today's inline-duplicated version? If it requires a pre-bundle step, the `generate:api` npm script itself needs to change — bigger than this plan currently assumes. Recommend a scratch-test spike before Phase 1, not discovery at Phase 5.

**Q52.** ~~The domain contract mirrors under `application/web-gateway/src/main/resources/*...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Keep manual sync for now (out of scope).

*Original question, preserved for context:*
The domain contract mirrors under `application/web-gateway/src/main/resources/*.yaml` are kept in sync with canonical `application/contract/*.yaml` by hand today (no automated copy step). This plan's phases assume continuing that manual-sync-per-PR convention. Should this consolidation also introduce an automated canonical-to-mirror sync step, given that hand-synced mirrors are themselves a duplication-risk pattern? Out of scope for now — flagging since the plan doesn't fix this meta-problem.

**Q53.** ~~Phase 4 of the plan (`profile...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* All domain contracts must use error from shared contract. Ship it as part of the consolidation initiative.

*Original question, preserved for context:*
Phase 4 of the plan (`profile.yaml`) is not a pure refactor — `profile.yaml` currently has bare `description`-only error responses with no schema at all, so adopting `shared.yaml`'s `Error` there is additive (first time this contract declares a typed error body), not behavior-neutral. Should this ship as part of the same consolidation initiative, or be split into its own PR/ADR so consolidation PRs 1/2/3/5 stay honestly zero-behavior-change while Phase 4 gets reviewed under an explicit-sign-off bar? Recommend splitting; product owner should confirm before Phase 4 is scheduled.

---

## Architect Review — Contract Consolidation Reconciliation — 2026-07-05

*Context: raised while reconciling `documents/contract-consolidation-plan.md` against Q49-Q53's actual resolutions (all resolved 2026-07-04, plan not yet executed). Q50's resolution mandates unifying pagination but doesn't pick a winning shape — that's a genuine new gap, not something the original plan or Q50 already answered.*

**Q54.** Q50 resolved "every domain must use the same shared pagination" — but three shapes currently exist in the codebase: (a) `shared.yaml`'s token-based `page_size`/`page_token` (AIP-158, unused by any domain yet), (b) `wealth.yaml`'s near-identical local copy of (a), used by `listAccounts`/`listPhysicalAssets`, (c) `wealth.yaml`'s v0.6 `listTransactions` endpoint using 0-indexed integer `page`/`size` — a different paradigm, already shipped and presumably already consumed by the frontend Transactions page. Which shape wins project-wide?
*Options:*
A) Token-based `page_size`/`page_token` (AIP-158) wins everywhere — `listTransactions` must migrate off `page`/`size`, a breaking change to an already-shipped v0.6 endpoint and its frontend caller (`Transactions.js` pagination controls).
B) Integer `page`/`size` wins everywhere — `listAccounts`/`listPhysicalAssets` migrate to it instead; `shared.yaml`'s existing `PageSize`/`PageToken` components are deleted/redefined as integer-based, a breaking change to whichever endpoint's frontend caller currently expects an opaque token.
C) Keep both shapes, but make each a named shared component (`shared.yaml` gets both `TokenPagination` and `OffsetPagination` parameter groups) — every domain picks one of the two *shared* shapes rather than inventing a third. Satisfies "every domain must use the same shared [components]" literally without forcing a breaking change on the one already-shipped endpoint. Recommended default if not answered before Phase 3 of the contract-consolidation plan, since it's the only option with zero breaking changes — but flagging since Q50's literal wording ("the same shared pagination," singular) may have intended A or B specifically.

---

## Backend Integration Test Coverage Questions — 2026-07-03

*Context: raised while adding true HTTP -> service -> repository -> real-Postgres integration tests per this session's QA task (SetupWizardIT, AccountTransactionBalanceIT, VitalReadingCreateUpdateIT, InventoryItemCreateUpdateIT, CrossDomainDashboardE2ETest). Reserved Q34-Q38 numbering for this task per instruction.*

**Q34.** ~~Every domain adapter module's `%test` Quarkus profile (`profile`, `wealth`, `health` — inherited from the main `application...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Both - fix profiles and move to Testcontainers.

*Original question, preserved for context:*
Every domain adapter module's `%test` Quarkus profile (`profile`, `wealth`, `health` — inherited from the main `application.properties`, no test-scope override) points `quarkus.datasource.jdbc.url` at the **same shared dev Postgres** (`localhost:5432/app_db`) that `quarkusDev` instances use, and `%test.quarkus.flyway.locations` includes the `R__seed_*_test_data.sql` repeatable migrations, every one of which starts with `TRUNCATE TABLE ... CASCADE`. Starting *any* `@QuarkusTest` in these three modules — even scoped to a single new test class via `--tests` — triggers Flyway `migrate-at-start=true` against that live connection and truncates real manually-tested data, independent of what the test itself does. Household and web-gateway already avoid this via `%test.quarkus.datasource.active=false` + a separate `%integration-test` profile (`InventoryItemPanacheRepositoryTest`'s `DatabaseIntegrationProfile` pattern) — but that `%integration-test` profile *still* points at the same shared DB and *still* truncates via its own `R__seed_household_test_data.sql`, so it is only safe to run against an empty/CI-fresh database, never against a live dev session. This is a pre-existing repo-wide gap (not introduced this session) that made it unsafe to actually execute the new `SetupWizardIT` (profile), `AccountTransactionBalanceIT` (wealth), and `VitalReadingCreateUpdateIT` (health) tests during this session — they are written and compile-clean but were only `compileTestJava`-verified, never run, per the explicit "do not disrupt live services" constraint. Recommend: give every domain adapter module the same `%test.quarkus.datasource.active=false` + dedicated `%integration-test` profile pattern household already uses (closes the "accidentally truncate live dev data by running `./gradlew test`" foot-gun repo-wide), and separately decide whether the seed-truncate strategy itself should move to an ephemeral Testcontainers-per-run database instead of a shared persistent one for adapter-layer integration tests generally.

**Q35.** ~~`InventoryItemCreateUpdateIT` (household) reuses the exact `DatabaseIntegrationProfile` pattern from `InventoryItemPanacheRepositoryTest` and was compile-verified but likewise never executed this session, for the same live-DB-truncation reason as Q34...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Change it to use Testcontainers to be safe anywhere.

*Original question, preserved for context:*
`InventoryItemCreateUpdateIT` (household) reuses the exact `DatabaseIntegrationProfile` pattern from `InventoryItemPanacheRepositoryTest` and was compile-verified but likewise never executed this session, for the same live-DB-truncation reason as Q34. Should household's existing `%integration-test` profile be considered "safe to run any time" (since it's already the documented convention and presumably is run in CI against a disposable DB), or does it carry the same live-session risk as Q34 whenever a developer happens to run it locally against their live dev Postgres instead of a CI-fresh one? If the intent is "CI-only, never run locally while `quarkusDev` is live," that constraint isn't written down anywhere (`ARCHITECTURE_GUIDELINES.md`'s Testing section describes the mechanism but not this hazard) — recommend documenting it explicitly next to the `DatabaseIntegrationProfile` javadoc in each affected test file, or in `documents/CICD.md`.

**Q36.** ~~The genuinely new gap — a true cross-domain integration test through the web-gateway hitting real domain services with real computed dashboard values — does not fit any existing `@QuarkusTest` convention in this repo: the gateway's own `%test` profile mocks every `@RestClient` (ADR-011) and disables its datasource, by design, specifically so gateway tests don't need live domain services...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Move to a dedicated Testcontainers-based full E2E setup.

*Original question, preserved for context:*
The genuinely new gap — a true cross-domain integration test through the web-gateway hitting real domain services with real computed dashboard values — does not fit any existing `@QuarkusTest` convention in this repo: the gateway's own `%test` profile mocks every `@RestClient` (ADR-011) and disables its datasource, by design, specifically so gateway tests don't need live domain services. `CrossDomainDashboardE2ETest` was written instead as a plain JUnit 5 test (`java.net.http.HttpClient`, no new test framework, no Quarkus harness) that drives the already-running dev-mode services over HTTP — the automated equivalent of the manual curl QA pass done earlier this session. It compiles clean and correctly skips via `assumeTrue` when services aren't reachable (verified: it does skip cleanly rather than fail when run under this session's sandboxed Gradle test-worker network policy, which could not reach `localhost:8080` even though plain `curl`/`java` from the same shell could — an environment quirk of this session, not a defect in the test). Is this the intended long-term shape for cross-domain integration coverage, or should there instead be a dedicated CI job that boots all five services against an ephemeral/Testcontainers Postgres and runs true end-to-end tests like this one un-skipped? If the latter, this test's `assumeTrue`-skip pattern should be replaced with a hard dependency on that CI job actually starting the services first.

**Q37.** ~~`CrossDomainDashboardE2ETest` calls the profile and health domain services **directly** (ports 8081/8083) rather than exclusively through the gateway (8080), even though `ARCHITECTURE_GUIDELINES...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Sufficient, since proxy routes are unit-tested separately.

*Original question, preserved for context:*
`CrossDomainDashboardE2ETest` calls the profile and health domain services **directly** (ports 8081/8083) rather than exclusively through the gateway (8080), even though `ARCHITECTURE_GUIDELINES.md` states "Frontend talks only to the Web Gateway." This is a deliberate scope choice for this test — it's asserting gateway *aggregation* behavior (the dashboard), not proxying, and the gateway's own admin/profile proxy routes are already covered by `ProfileGatewayResourceTest`. But it means this new test is not itself a demonstration of "the frontend's actual path" end-to-end (frontend -> gateway -> domain), only "domain writes -> gateway reads/aggregates." Should a second cross-domain test exist that goes exclusively through gateway-proxied writes (`POST /v1/admins`, `POST /v1/profiles` via `ProfileGatewayResource`, and equivalent wealth/health gateway proxy routes, if/when those exist) to prove the fully-proxied path end-to-end, or is direct-to-domain-service writes + gateway-read-aggregation considered sufficient coverage since the proxy routes themselves are unit-tested separately?

**Q38.** ~~`AccountTransactionBalanceIT` and `SetupWizardIT` both discovered that HTTP resource tests in the `profile`/`wealth`/`health` adapter modules use a **direct Java method call** convention (construct the real `@Path` resource class by hand with a real or stub use-case, call its methods directly — no RestAssured, no HTTP wire format) rather than the RestAssured-over-`@QuarkusTest`-HTTP convention `household` and `web-gateway` use...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Standardize on RestAssured project-wide.

*Original question, preserved for context:*
`AccountTransactionBalanceIT` and `SetupWizardIT` both discovered that HTTP resource tests in the `profile`/`wealth`/`health` adapter modules use a **direct Java method call** convention (construct the real `@Path` resource class by hand with a real or stub use-case, call its methods directly — no RestAssured, no HTTP wire format) rather than the RestAssured-over-`@QuarkusTest`-HTTP convention `household` and `web-gateway` use. Both conventions coexist today with no documented rule for which a new domain should follow (`ARCHITECTURE_GUIDELINES.md`'s Testing section doesn't mention it; the difference appears to trace to `household`/`web-gateway` declaring `io.rest-assured:rest-assured` as a test dependency while `profile`/`wealth`/`health` never added it). Should this be standardized project-wide (and if so, which convention wins), or is "match whatever the existing module already does" the accepted rule going forward? Recommend the `architect` agent make an explicit ADR either way, since it currently reads as historical accident rather than a decision.

---

## Frontend E2E Test Coverage Questions — 2026-07-03

*Context: raised while adding Playwright E2E coverage for household pages, Action Center, the Admin Setup Wizard, and Policy Settings (`web/e2e/household.spec.js`, `action-center.spec.js`, `admin-setup.spec.js`, `admin-policy.spec.js`). Reserved Q39-Q43 numbering for this task per instruction.*

**Q39.** ~~Live, currently-reproducible finding, not a hypothetical: the gateway process running on `localhost:8080` right now returns **zero `Access-Control-Allow-*` response headers** on both the CORS preflight (`OPTIONS /v1/admins` with `Origin: http://localhost:3000`) and the actual response (`GET /v1/admins` with the same Origin header) — verified directly with `curl -i`, twice, several minutes apart, same result both times...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Restart gateway dev instance and re-verify.

*Original question, preserved for context:*
Live, currently-reproducible finding, not a hypothetical: the gateway process running on `localhost:8080` right now returns **zero `Access-Control-Allow-*` response headers** on both the CORS preflight (`OPTIONS /v1/admins` with `Origin: http://localhost:3000`) and the actual response (`GET /v1/admins` with the same Origin header) — verified directly with `curl -i`, twice, several minutes apart, same result both times. `application/web-gateway/src/main/resources/application.properties` clearly sets `quarkus.http.cors=true` / `quarkus.http.cors.origins=http://localhost:3000` / `...cors.methods=...` / `...cors.headers=...`, so the config is correct on disk but not being applied by the running instance. Effect on the frontend: any "simple" GET still works in the browser (fetch delivers the body regardless of missing ACAO in practice for the read-only pages that swallow fetch errors with `.catch(() => setX([]))`), but any non-simple cross-origin request — e.g. `POST /v1/admins` with `Content-Type: application/json`, which is exactly what `Setup.js`'s Step 1 "Save and Continue" does via `createAdmin()` — fails in the browser with `net::ERR_FAILED` before the request ever reaches the server (confirmed via Playwright trace network log: `"_failureText":"net::ERR_FAILED"`, `"status":-1`). This made the two full-wizard-happy-path tests in `admin-setup.spec.js` fail deterministically across repeated runs, while a `curl -X POST` to the same endpoint from the shell succeeds fine (`201 Created`) because curl doesn't enforce CORS. Recommend: restart the gateway dev instance (a stale `quarkusDev` process that predates a CORS property change, or one that needs a manual restart rather than relying on hot-reload for `application.properties`, is the most likely explanation) and re-verify with `curl -i -X OPTIONS http://localhost:8080/v1/admins -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: POST"` — a correctly-behaving instance should echo back `Access-Control-Allow-Origin: http://localhost:3000` at minimum. This blocks not just my new E2E tests but any real manual admin-setup testing being done concurrently through the browser right now. Left the two affected tests in place asserting the *correct* behavior (they are right about what the product should do) rather than weakening them to match the current broken environment.

**Q40.** ~~`Setup...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Add htmlFor/id to the shared Field component.

*Original question, preserved for context:*
`Setup.js`'s `Field` component (and the near-identical `Field` components duplicated in `Calendar.js`, `Inventory.js`, and `Goals.js`) renders a `<label>` as a sibling of its input, not as a wrapper, and never sets `htmlFor`/`id`. This means none of these form fields are reachable via Playwright's (or Testing Library's) `getByLabel()` — confirmed by reading the DOM structure directly; `Setup.test.js` (Jest) already works around this today via `document.querySelector('input[name="dob"]')`, which is itself only possible because RTL/jsdom don't enforce the "no CSS selector" convention this project's own `FRONTEND_GUIDELINES.md` §9 mandates for Playwright specs. My new `admin-setup.spec.js` had no compliant way to target the Date of Birth field (no placeholder, no label association) and used a narrowly-scoped `page.locator('form').filter({ hasText: 'Date of Birth' }).locator('input[type="date"]')` as the least-bad option — an attribute selector, not a class/id selector, but still short of the "role-based locators only" rule as strictly read. Recommend: add `htmlFor`/`id` pairing to the shared `Field` component pattern (one shared component instead of 4 duplicated copies would also close a separate, smaller duplication smell) so every form field in the app becomes properly labelled and both Jest and Playwright specs can use `getByLabel()` going forward without exceptions.

**Q41.** ~~`ActionCenter...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Verify empty/gated state is sufficient for demo-mode E2E.

*Original question, preserved for context:*
`ActionCenter.js` derives `profileId` from `user?.profile_id`, but the demo sign-in fallback in `web/src/api/auth.js` (`signIn()`'s catch branch, used whenever the real `/v1/auth/signin` backend call fails or doesn't exist) only ever returns `{ username, role, token, issued_at }` — never a `profile_id`. That means **every** demo-mode sign-in, by construction, lands on Action Center in the "Sign in with a linked profile to view alerts" empty state, and the Refresh button stays permanently disabled, regardless of whether the signed-in user's household actually has calendar events, vehicle compliance deadlines, or biometric gaps to show. My `action-center.spec.js` tests this actual behavior (the guidance message + disabled Refresh button) rather than the populated-alerts happy path, since there is no way to reach the populated state through the demo sign-in flow at all — only by first completing the Admin Setup Wizard (which attaches `profile_id` via `updateUser()`) and then navigating to Action Center as that same session. Is a "sign in as the linked demo profile, then check Action Center" E2E flow (chaining through Setup Wizard first) worth adding as a follow-up, or is the current "verify the empty/gated state" coverage considered sufficient for demo-mode E2E and the populated-alerts path left to Jest component tests with mocked data (`ActionCenter.test.js` already exists and presumably covers this)?

**Q42.** ~~`PolicySettings...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Assume single-admin-per-database for v0.6, just add ADR note.

*Original question, preserved for context:*
`PolicySettings.js` resolves which admin to edit via `listAdmins()` and unconditionally picks `admins[0]` — there is no admin selector UI and no `admin_id` scoping tied to the signed-in user at all. In the shared dev database, multiple admin rows already exist (created by concurrent manual testing, other agents' setup-wizard test runs, etc. — confirmed via a live `GET /v1/admins` during this session, which returned 5 distinct admin rows). This means Policy Settings is currently, structurally, editing **whichever admin happens to be first in an unspecified list order**, not necessarily the signed-in admin's own household policy — a real correctness gap once more than one household/admin exists in the same database, not just an E2E-test-isolation inconvenience. My `admin-policy.spec.js` only asserts the page loads and the form is editable/re-fillable (it does not assert whose policy gets saved, since "first admin in the list" is not a meaningful contract to lock a test to). Is single-admin-per-database still the intended v0.6 assumption (in which case this is fine and just needs a comment/ADR note), or should `PolicySettings.js` scope to `user.admin_id` the same way `Setup.js` does, ahead of any future multi-household scenario?

**Q43.** ~~`SetupGate...~~ **RESOLVED — 2026-07-04 (product owner).**
*Resolution:* Add missing 'already-set-up' E2E case once CORS is fixed.

*Original question, preserved for context:*
`SetupGate.js` treats "no `admin_id` on the signed-in user object" and "an existing admin whose `getAdmin()` call throws" identically — both redirect to `/admin/setup` (confirmed by both the existing `SetupGate.test.js` Jest suite and my new E2E `SetupGate redirect` test). Given the Q39 CORS finding above, this means that *right now*, on this live environment, **any** admin login that has already completed setup (has a real `admin_id` with `policy_settings.setup_completed === 'true'`) would *also* incorrectly bounce back to the setup wizard, because the `getAdmin()` fetch that should confirm `setup_completed: 'true'` fails the same way `createAdmin()` does. I could not add an E2E test asserting "a fully-set-up admin reaches the dashboard and stays there" for this reason — it's not testable end-to-end against the current broken CORS state, only against a mocked `getAdmin` (which the existing Jest suite already does correctly). Once Q39 is resolved, recommend adding that missing "already-set-up admin lands on /dashboard" Playwright case to `admin-setup.spec.js` — I left a step-1-only redirect test in place instead, which is real coverage but not the full gate matrix.
