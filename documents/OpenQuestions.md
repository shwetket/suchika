                                                                                                                                                                                                                                                                        # Open Questions

| | |
|---|---|
| **Type** | Decision Log |
| **Audience** | Product owner (Ketan) |
| **Status** | Active |
| **Last updated** | 2026-07-02 (Q26-Q30 all resolved) |

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
