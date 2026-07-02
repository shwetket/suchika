# Roadmap — Future Milestones

| | |
|---|---|
| **Type** | Reference |
| **Audience** | All developers, product |
| **Status** | Active |
| **Last updated** | 2026-07-02 (v0.5 phased plan added, PROP-005 resolved) |

## Objective

Show what has been shipped and what is planned at each milestone, with the features delivered per version. This is the milestone-level view — for business rules and acceptance criteria within each version, see [BUSINESS_REQUIREMENTS.md](./BUSINESS_REQUIREMENTS.md). The version table in BUSINESS_REQUIREMENTS.md and this document cover the same milestones from different angles; keep them in sync when adding new milestones.

## Use Cases

- Quick overview of where the project stands and what comes next
- When planning sprint scope — identify which features belong to the upcoming milestone
- When onboarding — understand the evolution of the system at a glance

---

## v0.2 — Usable Local App [COMPLETE — UAT-READY]

**Focus:** Profile, Wealth, and Health domains fully usable as a local pilot. UAT window covers these three domains only.

### Features Delivered

- [x] **Profile Domain**
  - Create household admin
  - Create, list, view, edit, and deactivate household member profiles
  - Supported relation types: SELF, SPOUSE, CHILD, PARENT, SIBLING, OTHER

- [x] **Wealth — Accounts**
  - Create, list (filter by type and active status), view, update, and deactivate accounts
  - Supported account types: SAVINGS, CURRENT, CREDIT_CARD, HOME_LOAN, PERSONAL_LOAN, INVESTMENT, FD
  - All account records scoped to `profile_id`

- [x] **Wealth — Transactions**
  - List transactions with filter by date range and transaction type (CREDIT / DEBIT)
  - Transactions scoped to account and `profile_id`

- [x] **Wealth — Statement Upload**
  - Upload CSV file and parse transactions
  - Upload lifecycle tracked as PENDING → SUCCESS / FAILED
  - Rollback: delete all transactions linked to a specific upload

- [x] **Wealth — Deduplication Logic**
  - Same-file identical rows stored as distinct valid events
  - Cross-file duplicates (matching record already exists) are rejected

- [x] **Health — Vital Readings**
  - Log readings for: WEIGHT, HEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR_FASTING, BLOOD_SUGAR_PP, HEART_RATE, TEMPERATURE, OXYGEN_SATURATION, BMI, WAIST_CIRCUMFERENCE
  - List and filter by vital type
  - Delete a reading
  - All readings scoped to `profile_id`

- [x] **Health — Doctor Visits**
  - Create a visit record: from_date, to_date, visited_doctor flag, doctor_name, hospital_name, speciality, symptoms, diagnosis, notes, follow_up_date
  - List visits filtered by profile
  - Update and delete a visit record

- [x] **Frontend**
  - React pages for Profile, Wealth (Accounts, Transactions, Upload), and Health (Vitals, Doctor Visits) — all complete

### Out of Scope for v0.2 UAT
- Household domain (calendar events, inventory items, goals) — deferred to v0.3
- SonarQube clean pass — deferred to v0.3
- Dashboard wired to live data — deferred to v0.3

---

## v0.3 — Enhanced Local App [COMPLETE — PR OPEN]

**Focus:** Household domain, code quality gate, and dashboard live data. Completes the full three-domain local app.

### Features

- [x] **Household — Calendar Events**
  - Create calendar events with start date, end date, and assigned `profile_id`
  - Conflict detection: flag overlapping events for the same profile (warning, not block)
  - CRUD fully implemented (port 8084)

- [x] **Household — Inventory Items**
  - Manual entry into a unified raw inventory ledger (v0.3 scope)
  - Source platform tracking (INSTAMART, FLIPKART_GROCERY, COUNTRY_DELIGHT, etc.)
  - CSV import deferred to v0.4

- [ ] **Household — Task Tracking** — deferred to v0.4
  - Assign tasks to specific child profiles with hard deadlines linked to calendar

- [x] **Household — Goals**
  - Financial savings goal CRUD with progress tracking
  - `current_amount` computed from wealth transactions by gateway projection engine

- [x] **Household — Frontend**
  - React pages: Calendar Events (with conflict warning), Inventory, Goals (with progress bar)

- [x] **Dashboard — Live Data**
  - `ProjectionCalculationEngine` in web-gateway: on-demand refresh computes net worth, goal progress, vitals summary, upcoming events
  - `POST /v1/projections/refresh/{profileId}` + `GET /v1/projections/dashboard/{profileId}`
  - Dashboard "Refresh Live Data" button with non-blocking spinner

- [x] **SonarQube Clean Pass**
  - 0 BLOCKER, 0 CRITICAL issues; 285 Jest tests passing

---

## v0.4 — Error Handling + Physical Assets + Epic 8 Wealth Intelligence Engine [COMPLETE — tagged v0.4]

**Focus:** System resilience, complete physical-asset lifecycle, and the full Automated Wealth Intelligence Engine (Epic 8 Phases 1–4).

### Features Delivered

**Error Handling (core v0.4):**

- [x] **Malformed CSV Rejection**
  - Reject entire file if date or amount columns are missing
  - `wealth.upload_error_log` stores structured error records (`error_type`, `missing_columns`, `error_detail`)
  - `GET /v1/accounts/{accountId}/uploads/{uploadId}/errors` returns the error log; proxied via gateway
  - Frontend upload error panel displays parse failure reasons

- [x] **Dedup Key Fix (4-field)**
  - Cross-file dedup key: `(account_id, txn_date, amount, txn_type)` — description excluded
  - Same-file identical rows receive a sequence suffix (stored as distinct events)
  - `UploadResult` wraps upload entity with `insertedCount` + `List<SkippedRow>`
  - Frontend skipped-duplicates panel shows what was rejected and why

- [x] **Gateway Contract Update**
  - `application/contract/gateway.yaml` updated for new upload response shape; `web/src/api/generated.ts` regenerated

**Physical Assets — full vertical slice (v0.4.1 patch):**

- [x] **Backend + API**
  - `wealth.physical_asset` table with JSONB metadata and `uq_registration_number` unique constraint
  - Full CRUD: `POST /physical-assets`, `GET /physical-assets` (filter by type/active), `GET /{id}`, `PATCH /{id}`, `DELETE /{id}`
  - Domain/ports/adapters (hexagonal) + gateway proxy + `wealth.yaml`/`gateway.yaml` updated

- [x] **Frontend — `/wealth/physical-assets`**
  - List/filter, add/edit modal, deactivate; PUC / insurance / road-tax compliance deadlines with expiry-status colouring

**Manual Transaction Entry (Q7):**

- [x] `POST /v1/accounts/{accountId}/transactions`; `upload_id` column made nullable (`V7` migration)
- [x] `source = MANUAL` tag in transaction metadata — distinguishable from CSV-sourced rows
- [x] Add Transaction modal in frontend Transactions page

**Epic 8 Phase 1 — Account Classification & Net Worth Foundation:**

- [x] `account.metadata JSONB NOT NULL DEFAULT '{}'` column (`V6` migration)
- [x] `PATCH /v1/accounts/{id}/classification` — writes `category`, `liquidity_tier`, `purpose_tag`, `joint_owners`, and 4 loan fields (principal, start date, tenure, offset account) into metadata
- [x] `GET /v1/accounts/{id}/balance` — returns `opening_balance + SUM(CREDIT) - SUM(DEBIT)` (Bug 2 fix — dashboard net worth now uses transaction history)
- [x] `computeCategoryValidation` gateway step → `WEALTH_CATEGORY_VALIDATION` snapshot
- [x] `computeFamilyNetWorth` gateway step → `WEALTH_NET_WORTH_FAMILY` snapshot (ADR-017 family rollup across all household members)
- [x] ADR-016 (joint account ownership), ADR-017 (family-level aggregation)

**Epic 8 Phase 2 — Expense Category Tagging:**

- [x] `PATCH /v1/accounts/{accountId}/transactions/{txnId}/category` — single-transaction tagging
- [x] `PATCH /v1/accounts/{accountId}/transactions/category` — bulk tag by ID list (Q24)
- [x] 5 `ExpenseCategory` enum values: `HOUSEHOLD_CORE`, `CHILD_RELATED`, `MAINTENANCE`, `DISCRETIONARY`, `UNCATEGORIZED`
- [x] `TransactionEntity.metadata` JSONB fully wired (was silently reset to `{}` on every save — Bug fix)
- [x] Joint account `joint_owners` stored in `account.metadata` per ADR-016

**Epic 8 Phase 3 — Loan Amortization, EMI Arbitrage & Liquidity:**

- [x] `AmortizationCalculator` — pure domain class: EMI formula, outstanding balance, remaining tenure, interest split
- [x] `GET /v1/accounts/{id}/amortization` — returns `AmortizationSummary`; BadRequest if metadata not set, 404 if not a loan type
- [x] `computeEmiTracking` gateway step → `WEALTH_EMI_TRACKING_FAMILY` (per-loan EMI + offset arbitrage savings)
- [x] `computeLiquidityTiers` gateway step → `WEALTH_LIQUIDITY_TIERS_FAMILY` (LIQUID / SEMI_LIQUID / ILLIQUID / LOCKED / UNCLASSIFIED totals)
- [x] `computeGrowthProjection` gateway step → `WEALTH_GROWTH_PROJECTION_FAMILY` (5yr/10yr projections for MUTUAL_FUND 12%, NPS 10%, PPF 7.1%)
- [x] Loan details form in Accounts UI; Dashboard EMI card and Liquidity Tier breakdown
- [x] `JsonbMetadataUtil` — package-private utility extracted to eliminate CPD across `AccountEntity`, `PhysicalAssetEntity`, `TransactionEntity`
- [x] `refreshAll()` per-step exception isolation via `runStep()` helper (Bug 3 — one failing step no longer blocks others)

**Epic 8 Phase 4 — Formula Goals Engine & Validation Report:**

- [x] `policy_settings JSONB NOT NULL DEFAULT '{}'` on `profile.admin` (Flyway V3); 5 configurable thresholds
- [x] `PATCH /v1/admins/{adminId}/policy` with merge semantics
- [x] Admin Policy Settings page at `/admin/policy` (admin role required)
- [x] `computeFormulaGoals` gateway step → `WEALTH_FORMULA_GOALS_FAMILY`; 5 hardcoded formula goals: DEBT_CROSSOVER, THIRTY_SEVENTY_TARGET, FREEDOM_RUNWAY, INSURANCE_FREE, YEAR_ONE
- [x] `computeValidation` gateway step → `WEALTH_VALIDATION_REPORT_FAMILY`; 4 advisory checks (category resolution, missing growth rate, EMI completeness, budget cap unset)
- [x] Dashboard: Household Goals card (N/M achieved) and Data Quality banner (PASS / WARNING / FAIL)

**Deferred:**

- [ ] **Quarantine Protocol (Grocery Data)** — deferred to v0.6 (inventory CSV import not yet built)
- [ ] **Duplicate Resolution UI** — deferred to v0.6

---

## v0.5 — Beta Release

**Focus:** Cross-domain intelligence features + UX completeness gaps from v0.4 reviews.

**Readiness verified against actual code 2026-07-02** (not just docs — see architect's verification pass). PROP-005 resolved same day → ADR-018 (React Query). Product owner also resolved Vacation Planner nav placement, `profile_id` fix timing, and physical-asset date storage (see `OpenQuestions.md` Q26-Q29). Plan below is phased by dependency: Phase 0 (independent fixes) → Phase 1 (ADR-018 groundwork) → Phase 2 (Vacation Planner, does not depend on Phase 1) → Phase 3 (Consolidated Action Center, depends on Phase 1).

### Phase 0 — Small independent CRUD/fix work (no dependencies, parallelizable)

- [ ] **Vital Readings — Edit**
  - `PUT /v1/vitals/{id}` endpoint on health service (`VitalReadingResource.java` currently has GET/POST/GET-by-id/DELETE only — verified, no PUT exists)
  - Edit modal in Vitals frontend page (mirrors doctor visits UX)

- [ ] **Inventory Items — Edit**
  - `PUT /v1/inventory-items/{id}` endpoint on household service (`InventoryItemResource.java` currently has GET/POST/GET-by-id/DELETE only — verified, no PUT exists)
  - Edit modal in Inventory frontend page

- [ ] **Inventory `is_consumed` flag** (Q6 resolution — "used in a calculation," not "used up"; no deletion, no expiry)
  - New Flyway `application/flyway/household/V4__inventory_item_consumed_flag.sql`
  - `InventoryItem` domain field, entity, DTOs, toggle in UI

- [ ] **`TransactionResource`/`TransactionService` profile_id threading fix** (Q28 — fix now, not deferred)
  - Verified actual state: repo-layer filter is fully implemented and correct (`TransactionPanacheRepository.findByAccountId`/`existsByDeduplicationKey`/`sumAmountByTxnType` all accept and apply an optional `profileId`), but `TransactionService.listByAccount()` hardcodes `null` with an explicit code comment opting out, and `TransactionResource.listTransactions()` has no `profile_id` query param at all — so the filter exists but is never exercised from the HTTP surface. (Corrects prior "partially done" language — the gap is entirely in the service/resource layer, not the repository.)
  - Fix: add `profile_id` query param to the resource, thread through the use case and service, remove the null-hardcode

- [ ] Verify/remove stale "opening balances only" copy on Reports/Dashboard (moot since net worth formula fixed in Epic 8 Phase 1 — confirm no leftover interim-fix text remains)

### Phase 1 — PROP-005 groundwork (React Query)

- [x] **Decision resolved 2026-07-02** — see ADR-018. React Query for server state; Context API stays for auth/global state; no Redux/Zustand.
- [ ] Add `@tanstack/react-query` dependency, `QueryClientProvider` at app root
- [ ] Migrate one reference page (e.g., Dashboard) to establish the pattern before Phase 3 begins
- [ ] Update `documents/FRONTEND_GUIDELINES.md` with the React Query convention

### Phase 2 — Vacation Planner (cross-domain; depends on Phase 0 wealth/physical-asset work only, NOT on Phase 1)

- [ ] **Nav placement confirmed:** under Household nav, route `/household/vacation-planner` (Q27)
- [ ] Budget validation: liquid savings (wealth `WEALTH_LIQUIDITY_TIERS_FAMILY`) vs trip cost
- [ ] Asset compliance block: vehicle PUC/Insurance expiry (wealth `physical_asset.metadata` — parsed defensively as JSONB, no schema promotion per Q29) vs trip date (household calendar)
- [ ] New gateway `VacationPlannerResource`/`VacationPlannerService` composing `WealthServiceClient` + `HouseholdServiceClient` calls (same pattern as ADR-013's projection engine)
- [ ] Frontend page under `web/src/pages/Household/`

### Phase 3 — Consolidated Action Center (blocked on Phase 1 landing; ALSO blocked on Q30 — biometric streak gap definition — before implementation starts)

- [ ] Single read-only dashboard aggregating alerts from all 3 domains
- [ ] Upcoming calendar events, vehicle compliance deadlines (reuses Phase 2's JSONB parsing), biometric streak gaps
- [ ] **Biometric streak gap definition is still an open question (Q30)** — no threshold invented; must be resolved by product owner before this phase starts, not before Phase 0-2
- [ ] Frontend consumes the new aggregation endpoint using the React Query pattern established in Phase 1

Note: Profile-scoped data isolation (`profile_id` filtering on all domains) was delivered in v0.2. All Epic 8 pre-v0.5 blockers (gateway /errors proxy, net worth formula fix, refreshAll() isolation) delivered in v0.4.

---

## v0.6 — Testing Foundation

**Focus:** Automated test coverage.

### Features

- [ ] Unit tests for all domain use cases
- [ ] Integration tests for adapters
- [ ] Contract tests for OpenAPI endpoints
- [ ] Pre-commit test gate via Gradle

---

## v1.0 — Security & Persistence

**Focus:** Auth, encryption, persistent real-world data. No more ephemeral DB.

### Features

- [ ] **Persistent Data Migration**
  - Flyway versioned migrations enforced and locked — no more ephemeral resets
  - All five schemas treated as production data from this point forward

- [ ] **Authentication (OIDC/OAuth2)**
  - External Identity Provider integration
  - Role-Based Access Control: Admin (Adult) vs Restricted (Child)

- [ ] **Encryption at Rest**
  - Financial ledgers encrypted at application layer before DB insert

- [ ] **Google Fit Integration (Manual Sync)**
  - User-triggered sync only — no background polling
  - Upsert deduplication keyed on `(profile_id, timestamp, metric)`
  - Short-lived tokens only — refresh/offline tokens strictly prohibited

- [ ] **Cross-Domain Security Enforcement**
  - Restricted profiles blocked from triggering Wealth domain queries
  - All cross-domain queries scoped to active `profile_id`

---

## v1.1 — Multi-User

**Focus:** Multiple user accounts within a household.

### Features

- [ ] Multiple user accounts per household
- [ ] Family sharing with role assignments
- [ ] Admin can invite members
- [ ] Viewer role: read-only access

---

## v1.2 — Public Local Release

**Focus:** Stable local release for general users.

### Features

- [ ] Packaging for easy local installation
- [ ] Setup wizard for first-time users
- [ ] Full documentation for non-developer users

---

## v1.3 — Export / Import

**Focus:** Cross-domain data archiving and portability.

### Features

- [ ] **Unified Data Export**
  - Single trigger exports all data from all five PostgreSQL schemas
  - Packaged as structured JSON/CSV local backup

- [ ] **1-Click Batch Folder Import**
  - Scan local folder and upload multiple CSVs in one action
  - Batch status dashboard

- [ ] **Rule-Based Tagging Engine**
  - If description contains "SWIGGY" → tag "Food"
  - If description contains "FUEL" → tag "Transport"
  - Admin UI to create/manage rules

- [ ] **Unified Search & Export**
  - Full-text search on transaction descriptions
  - Export filtered results to clean CSV
  - Date range, amount range, account filters

---

## v2.0 — Local AI

**Focus:** Local LLM as unified reasoning engine over personal data.

### Features

- [ ] **Cross-Domain Context API**
  - Read-only API layer for local AI to simultaneously query Wealth, Household, and Health data

- [ ] **Daily Briefing Generation**
  - AI generates contextual insights across all domains
  - Example: *"You have a road trip to Munnar tomorrow, your Tata Nexon insurance expires today, and savings need topping up to cover the trip budget."*

- [ ] **Transfer Reconciliation**
  - Auto-link transfers between accounts (same amount, opposite direction, same date)
  - Manual override for fuzzy matches

---

## v2.1 — Cloud Ready

**Focus:** Architecture preparation for cloud deployment.

### Features

- [ ] Docker containerization
- [ ] Multi-region DB replication design
- [ ] Load balancer + auto-scaling setup
- [ ] Redis session store

---

## v2.2 — Mobile App

**Focus:** Companion mobile application.

### Features

- [ ] Mobile-responsive web frontend
- [ ] Native mobile app (iOS/Android) — evaluation phase

---

## v3.0 — GitHub Ready

**Focus:** Open-source collaboration readiness.

### Features

- [ ] Contribution guidelines finalized
- [ ] Issue templates and PR templates
- [ ] Public roadmap published

---

## v3.1 — Integrations

**Focus:** External service connections.

### Features

- [ ] Google Drive sync
- [ ] Google Calendar integration
- [ ] Fitbit data import
- [ ] Automated bank integration (Plaid or Setu API)

---

## v3.2 — Plugin Framework

**Focus:** System extensibility.

### Features

- [ ] Plugin interface definition
- [ ] First-party plugin examples

---

## v3.3 — Marketplace

**Focus:** Plugin/module ecosystem.

### Features

- [ ] Plugin registry
- [ ] Community submissions

---

## v4.0 — Cloud Launch

**Focus:** Full commercial cloud deployment.

### Features

- [ ] Multi-tenant PostgreSQL (row-level security or per-tenant schemas)
- [ ] Public domain deployment
- [ ] CDN for static assets
- [ ] SLA: 99.5% uptime

---

## v4.1 — Commercial Launch

**Focus:** Licensing, billing, regulatory compliance.

### Features

- [ ] Subscription billing (Stripe or Razorpay)
- [ ] Free tier / Pro tier definition
- [ ] GDPR compliance (data deletion, export, right-to-be-forgotten)
- [ ] Terms of Service and Privacy Policy
- [ ] Public API with rate limiting and API key auth

---

## Dependency Chain
v0.1 → v0.2 → v0.3 → v0.4 → v0.5 → v0.6
↓
v1.0 → v1.1 → v1.2 → v1.3
↓
v2.0 → v2.1 → v2.2
↓
v3.0 → v3.1 → v3.2 → v3.3
↓
v4.0 → v4.1

Each milestone requires the previous to be stable before starting.

---

## Success Metrics

| Milestone | Key Metric | Status |
|---|---|---|
| v0.1 | Upload 100+ transactions from 3+ CSVs without data loss | DONE |
| v0.2 | Profile + Wealth + Health UAT-ready; statement upload lifecycle (PENDING/SUCCESS/FAILED) verified; all data member-scoped | DONE |
| v0.3 | Household domain live; SonarQube zero blockers; dashboard shows live data | DONE |
| v0.4 | Zero silent data drops on malformed input; full Epic 8 wealth intelligence engine live; 375 JS tests + 0 Sonar issues | DONE |
| v0.5 | Cross-domain vacation planner works end-to-end | PLANNED |
| v1.0 | Auth + encryption pass local security review | PLANNED |
| v1.3 | Full data export/import round-trip verified | PLANNED |
| v2.0 | Local AI daily briefing generates without errors | PLANNED |
| v4.1 | 1000+ active Pro users, <100ms API p99 | PLANNED |

---

## Communication

- **Feature requests:** GitHub Issues with `vX.Y` milestone label
- **Roadmap updates:** This file + project announcements
- **Breaking changes:** Changelog + notification to active users
- **Security issues:** GitHub Issues with `security` label

---

## Architect Review — 2026-06-29

### Best Practices Applied

- Hexagonal Architecture strictly followed across all four domains — domain layer has zero framework dependencies in every service; ArchUnit proves this at build time
- ArchUnit rules are comprehensive: 8 rule groups covering domain purity, layer isolation, cross-domain isolation, JPA placement, shared module isolation, logging, and gateway test coverage
- `shared/` is a true leaf module — no domain imports, provides AppLogger and the typed exception hierarchy to all layers
- All exception handling is typed: `CsvParseException`, `NotFoundException`, `ConflictException` etc. — no bare `throws Exception` in public interfaces
- No SQL ENUMs anywhere in the codebase — VARCHAR + OpenAPI validation throughout (ADR-010 consistently followed, with V5 migration cleaning up any early-version CHECK constraints)
- Flyway migration discipline maintained — no committed migrations edited; every schema change is a new versioned file
- IST timezone enforced at two levels (DB `ALTER DATABASE` + Hibernate property) in every service
- Gateway test isolation via `@InjectMock @RestClient` (ADR-011) — gateway tests do not require live domain services
- `StatementCsvParser.parseCsvLine()` uses a while-loop to avoid Sonar S127 (for-loop counter mutation) — SonarQube compliance considered at implementation time
- Repeatable seed migrations (`R__seed_*_test_data.sql`) provide deterministic test data for adapter integration tests without polluting production migrations
- `UploadResult` record type correctly placed in `ports/input/` — it is a ports-layer contract object, not a domain entity, not an adapter DTO
- `CsvParseException` extends `ApplicationException` — parse errors are surfaced as typed, HTTP-mappable exceptions without leaking stack traces

### Architectural Debt / Improvement Recommendations

1. **[HIGH — v0.5]** Gateway `/errors` endpoint gap: `WealthServiceClient` and `WealthGatewayResource` do not expose `GET /accounts/{accountId}/uploads/{uploadId}/errors`. The frontend currently bypasses the gateway to reach the wealth service directly — a violation of the "frontend talks only to gateway" invariant (ADR-002 / ADR-009). Add the proxy endpoint to the gateway client and resource, update `gateway.yaml`, and regenerate the API client.

2. **[HIGH — v0.5]** Net worth calculation uses `opening_balance` field, not running balance from transactions. `ProjectionCalculationEngine.computeNetWorth()` and `computeTotalBalance()` sum the static `opening_balance` column from the account record. This does not reflect credit/debit activity after account creation. The correct calculation is `opening_balance + sum(CREDIT transactions) - sum(DEBIT transactions)` per account. Fix before v0.5 cross-domain vacation planner uses the net worth figure for budget validation.

3. **[HIGH — v0.5]** `TransactionPanacheRepository` does not directly filter by `profile_id` in `findByAccountId()` or `existsByDeduplicationKey()`. This relies on the caller having verified account ownership (account belongs to the right profile). The implied chain is correct in current code, but it is not enforced by ArchUnit and is invisible to future developers. Add an explicit `profile_id` join or secondary filter, or document this as a deliberate performance trade-off in an ADR.

4. **[MEDIUM — v0.5]** `ARCHITECTURE.md` was used as an agent definition + skill definition dump rather than a real architecture document. This has been corrected in this review (v0.4 architecture now written there), but the `.claude/` or `.github/` directory should be the canonical home for agent definitions going forward. Agent YAML front-matter should not live in the root documents/ folder.

5. **[MEDIUM — v0.5]** V3 migration created `chk_error_type` CHECK constraint on `upload_error_log.error_type`, then V5 dropped it. The corrective migration is fine per the "add a new file" rule, but the root cause is that the initial V3 author added a CHECK constraint to an enum discriminator column — violating ADR-010. Agent prompts should explicitly call out the `upload_error_log.error_type` column as an example of a discriminator to reinforce the rule.

6. **[MEDIUM — v0.5]** `CalendarEvent` domain entity holds `profileId` as a field (passed into `CalendarEvent.create(profileId, ...)`). This means the domain layer is aware of the tenant isolation key, which ADR-006 says belongs only in the adapter layer. The current ArchUnit rules do not catch this because `profileId` is a plain `UUID` — it looks like a business field. Audit all four domain entities for `profileId` field storage. If it must stay for convenience, document the deliberate trade-off.

7. **[MEDIUM — v0.6]** No ArchUnit rule verifies that `profile_id` filtering exists in adapter persistence queries. This is convention-enforced only. A future developer could add a query without the filter and no build gate would catch it. Consider adding a ArchUnit custom rule that flags `PanacheRepositoryBase` subclasses that have `find()` calls without `profile_id` in the predicate string. Low urgency — current code is clean — but worth investing before v1.0 auth is added.

8. **[MEDIUM — v0.6]** API versioning strategy is unresolved (PROP-004). All endpoints are under `/v1/`. Before v1.0 launch, the team must decide on URL versioning vs. deprecation-first. This becomes critical when auth is added in v1.0 (token format changes are breaking changes). Resolve PROP-004 as part of v0.6 or early v1.0 planning.

9. **[LOW — v0.6]** `v0.6 — Testing Foundation` milestone items (unit tests, integration tests, contract tests) are already substantially done in v0.2–v0.4 for the implemented domains. The milestone as written reads as if this is all future work. The actual gap is: contract tests (OpenAPI schema validation against running service) and full branch coverage on wealth domain use case services. Re-scope v0.6 accordingly.

10. **[LOW — v1.0]** `ProjectionCalculationEngine.refreshAll()` is synchronous — all four compute steps run in a single transaction. If one step's REST call to a domain service is slow or times out, the entire refresh fails. Before v1.0, add per-step error isolation (each compute step in try-catch; partial refresh is acceptable) and consider async refresh via a background job or reactive pipeline.

### Recommended Additions to Upcoming Milestones

- **Add to v0.5:** Fix gateway `/errors` endpoint proxy (gap #1 above) — required for gateway invariant compliance
- **Add to v0.5:** Fix net worth calculation to use transaction history, not opening_balance (gap #2 above) — the vacation planner budget check will use this figure
- **Add to v0.5:** Resolve PROP-005 (frontend state management) before building the Unified Dashboard — it is a cross-domain view that will need shared state
- **Add to v0.6:** Re-scope testing milestone to reflect what is already done; focus remaining work on contract tests and wealth service use case branch coverage
- **Add to v0.6:** Add ADR for profile_id-in-domain trade-off (gap #6 above) — document decision clearly so future agents don't flip it silently
- **Add to v1.0:** Resolve PROP-004 (API versioning) — required before any breaking change from auth integration; URL versioning is the low-risk default
- **Add to v1.0:** Add per-step error isolation in `ProjectionCalculationEngine.refreshAll()` (gap #10 above) — synchronous all-or-nothing refresh is too fragile for production
- **Add to v1.0:** Resolve ADR-007 (application-layer encryption for wealth data) — must be decided and implemented before auth is added

---

## Business Analyst Review — 2026-06-29

### Delivered vs Requirements (v0.2–v0.4)

| Feature | Milestone | Status | Gaps |
|---|---|---|---|
| Profile CRUD (admin + members, 6 relation types, soft deactivate) | v0.2 | Delivered | No pagination on member list (deferred); no avatar/photo |
| Wealth — Account CRUD (7 types, active/inactive filter) | v0.2 | Delivered | Balance shown is opening balance only — does not reflect transaction history |
| Wealth — Transaction list with date range + type filter | v0.2 | Delivered | No pagination; no sort control; no "clear filters" button |
| Wealth — Statement Upload (PENDING/SUCCESS/FAILED lifecycle, rollback) | v0.2 | Delivered | None within scope |
| Wealth — Deduplication (cross-file 4-field key, same-file sequence suffix) | v0.2 | Delivered | Dedup key corrected in v0.4 (description removed) |
| Health — Vital Readings (10 types, log/list/filter/delete) | v0.2 | Delivered | No edit on a logged reading (delete-and-re-log only); no trend chart |
| Health — Doctor Visits (full CRUD, conditional doctor_name constraint) | v0.2 | Delivered | No date-range filter on visit list |
| Household — Calendar Events (CRUD, conflict warning, 8 event types) | v0.3 | Delivered | Conflict warning shown but creation is not blocked — user may not notice the warning |
| Household — Inventory (manual CRUD, platform/category filter, 5+ platforms) | v0.3 | Delivered | No edit on an inventory item (delete-and-re-add only); no item lifecycle (consumed/restock) |
| Household — Goals (CRUD, progress bar, status badge, projection engine) | v0.3 | Delivered | `current_amount` only updated via dashboard refresh, not automatically on transaction import |
| Dashboard — Live Data (4 snapshot keys, refresh button, spinner) | v0.3 | Delivered | Net worth figure based on opening balance only — does not sum transaction history (architectural debt, logged as Q2) |
| SonarQube Clean Pass | v0.3 | Delivered | 0 BLOCKER/CRITICAL, 285 tests passing |
| Wealth — Malformed CSV rejection + structured error log | v0.4 | Delivered | `/errors` endpoint not proxied through gateway — frontend calls wealth service directly (architectural violation, logged as Q3) |
| Wealth — Dedup key fix (4-field, description excluded) | v0.4 | Delivered | None |
| Frontend — Upload error panel + skipped duplicates UI | v0.4 | Delivered | Error panel shows "Upload failed — check your file" as fallback when error fetch fails; gives no actionable detail in that scenario |
| Gateway contract updated for new upload response shape | v0.4 | Delivered | None |
| Wealth — Physical Assets CRUD (backend + frontend) | v0.4.1 patch (2026-06-30) | Delivered | Corrected prior table entry — the "v0.2 Delivered" claim below was wrong; only the `V2__physical_assets.sql` migration existed before today, zero Java backend or frontend. Full vertical slice (domain/ports/adapters/contract/frontend page) built 2026-06-30 — see `documents/domain-state/wealth.md` |
| Wealth CQRS Read Model ("Mahesh Summation Rule", EMI arbitrage, reallocation triggers) | v0.4 per REQUIREMENTS | Not Delivered | These use cases from REQUIREMENTS_wealth_domain.md v0.4 section were not built; the dashboard engine computes only opening-balance net worth and basic goal/event summaries |
| Household — Task Tracking | v0.3 (deferred from) | Not Delivered | Deferred; no schema exists |
| Household — Item lifecycle (consumed/restock) | v0.3 | Not Delivered | Inventory is an append/delete ledger; no consumed or restock state |
| Household — Home Automation Mapping | v0.3 | Not Delivered | Not started; no schema |
| Health — Vital trend charts / BMI auto-calculation | v0.3 per domain-state | Not Delivered | Deferred; Reports page shows "Coming Soon" placeholder |
| Duplicate Resolution UI (accept/reject flagged duplicates) | v0.4 | Not Delivered | Deferred to v0.5; only read-only skipped-duplicates panel was built |
| Quarantine Protocol (row-level quarantine for malformed grocery data) | v0.4 | Not Delivered | Deferred to v0.5 |

---

### User-Facing Gaps (UX / Missing Flows)

1. **No physical asset UI.** The `wealth.physical_asset` table exists and the API is implemented, but there is no frontend page. A user who owns a vehicle or property has no way to view, add, or manage physical assets from the browser. This is the most significant invisible feature — delivered in the backend, invisible in the UI.

2. **Account balance shows opening balance, not live balance.** The Accounts page displays "Balance: ₹X" where X is the `opening_balance` set at account creation. After months of transactions, this figure is wrong. A user who has uploaded 12 months of statements still sees the same static number they entered on day one. The Reports page confirms this by summing `opening_balance` and labelling it "Sum of opening balances." This is immediately misleading for any financial review.

3. **No edit on a vital reading.** When a user logs an incorrect vital (wrong value, wrong date), the only correction path is delete and re-enter. There is no edit modal for vitals. The doctor visits page does support edit — this inconsistency is noticeable across the health section.

4. **No edit on an inventory item.** Same pattern as vitals: inventory items can be added and deleted but not edited. A user who enters the wrong quantity must delete and re-add.

5. **No item lifecycle for inventory.** The requirements specify "marking items as consumed or restocking them." The current inventory is a flat ledger with no consumed/in-stock state. A user cannot tell whether an item has been used or still needs to be purchased.

6. **Goal current_amount only updates on manual dashboard refresh.** After uploading a bank statement with new transactions, a user expects their goal progress to reflect the new wealth. It does not — `current_amount` is only updated when the user manually clicks "Refresh Live Data" on the dashboard. There is no in-page feedback on the Goals page itself explaining this, so the stale progress bar is silently wrong.

7. **Transaction list has no pagination and no sort control.** A user with 500+ transactions from 12 months of uploads sees all rows at once with no way to sort by amount or narrow to a specific month without using the date filter. The date filter clears when switching accounts, which adds friction.

8. **The calendar conflict warning is easy to miss.** Conflict detection is implemented correctly but the UI only shows a banner at the bottom of the create-event response. A user who quickly creates an event may not notice they have created a scheduling conflict.

9. **No navigation from the Wealth Reports page to individual transactions.** The Reports page shows a "Total Accounts" count and a net balance card, but the account names are not links. A user cannot drill from a report summary into the transaction list for a specific account. The experience dead-ends at the summary.

10. **Dashboard net worth figure is unlabelled as approximate.** The dashboard displays net worth from opening balances with no caveat. A user with a loan account that has been partially repaid via EMIs over the year will see the wrong net worth figure with no indication it is stale or approximate.

11. **No transaction creation from the UI.** Users can only add transactions via CSV upload. There is no manual "Add Transaction" form. A user who wants to log a single cash purchase (e.g., petrol, a medical bill) has no path to do so without creating a synthetic CSV.

12. **Profile list has no pagination.** For a household with 6+ members the full list is shown without pagination. Minor for the current single-user context, but noted in the profile domain-state backlog.

---

### Quick-Win Recommendations (add to v0.5 or a new v0.4.1 patch)

The following items are low-scope, high-impact for the active user. They do not require cross-domain logic and do not violate milestone rules.

1. ~~**Build the Physical Assets frontend page**~~ **DELIVERED 2026-06-30.** Correction: the premise here was wrong — the backend was never actually built either (only the Flyway migration existed). Built the full vertical slice (domain/ports/adapters/contract + frontend page at `/wealth/physical-assets`) as a v0.4.1 patch. See `documents/domain-state/wealth.md`.

2. **Add edit to Vital Readings** — the health API already supports `GET /vitals/{id}`. Adding an edit modal to the vitals page removes the delete-and-re-log friction and makes the health domain consistent with the doctor visits page. Backend change: add `PUT /vitals/{id}` if not already present; otherwise frontend-only.

3. **Add edit to Inventory Items** — same pattern. The household API supports `GET /inventory-items/{id}`. Add an edit modal to the inventory page.

4. **Label the "Net Balance" on Reports page as "opening balances only"** — a one-line copy change: add a tooltip or static note "Based on account opening balances — does not include transaction history." This is a one-line frontend fix that prevents the user from trusting an incorrect figure. No backend work required.

5. **Add a "Add Transaction" manual entry form** — the transaction API already accepts individual POSTed records (or add a simple endpoint). A form with date, amount, type, and description lets the user log cash or informal transactions without generating a CSV. This directly addresses the most common real-world data gap for a household financial app.

6. **Fix the `/errors` gateway proxy** — this is already flagged as HIGH priority by the Architect (Q3). The frontend currently calls the wealth service at port 8082 directly, violating the gateway invariant. Adding the proxy to `WealthServiceClient` and `WealthGatewayResource` and updating `gateway.yaml` restores the invariant. Estimate: one backend task.

---

### Recommended Updates to Upcoming Milestones

**Add to v0.5:**

- ~~Physical Assets frontend page~~ — DELIVERED 2026-06-30 as a v0.4.1 patch (full vertical slice, not just frontend — backend never existed prior). Remove from v0.5 scope.
- Vital Readings edit endpoint and edit UI — completes the health CRUD symmetry
- Inventory Items edit UI — completes the household CRUD symmetry
- Inventory item lifecycle state (consumed / in-stock) — minimum: add a `is_consumed BOOLEAN` column and toggle in the UI; this was a stated v0.3 requirement that was not delivered
- Manual transaction entry form — high-value for daily use; removes the CSV-only constraint for individual entries
- Fix `/errors` gateway proxy (carry-over from Architect recommendation)
- Fix net worth calculation to use transaction history (carry-over from Architect recommendation)
- Add copy/tooltip on Reports page and Dashboard labelling the balance figures as "opening balances only" — interim UX fix until the calculation is corrected
- Add `PUT /vitals/{id}` endpoint to health service if not already present

**Add to v0.6:**

- Transaction list pagination — becomes necessary once statement history grows beyond a few months
- Date-range filter on doctor visit list — the visit list currently has no date filter; for a multi-year user this becomes unmanageable
- Goal progress auto-refresh cue — add a UI note on the Goals page: "Progress updates when you refresh the dashboard" so the user understands the manual refresh dependency
- Vital trend charts — listed in health domain-state as v0.3 backlog; now more than one version behind; should be prioritised before v1.0

**Defer from v0.5 to v0.6:**

- Duplicate Resolution UI (accept/reject flagged duplicates) — already deferred from v0.4; given that the Quarantine Protocol for grocery data is also deferred, neither input source (bank statements nor grocery exports) has enough user volume to make this urgent before v0.6
- Quarantine Protocol (grocery CSV row-level quarantine) — the inventory CSV import itself has not been built yet; quarantine logic for a feature that doesn't exist yet should not block v0.5

**Clarify scope of v0.5 Consolidated Action Center:**

- The v0.5 plan says "Upcoming calendar events, vehicle compliance deadlines, biometric streak gaps." Vehicle compliance deadlines belong to the Physical Assets feature (vehicle PUC/insurance expiry). Ensure the Physical Assets frontend page is complete before v0.5 scope is confirmed, or remove vehicle compliance deadlines from the v0.5 Action Center scope.

---

## QA Review — 2026-06-29

### Test Coverage Summary

| Domain | Unit Tests (domain layer) | Adapter / Service Tests | Key Gaps |
|---|---|---|---|
| profile | 1 test class — `BloodTypeTest` (enum only) | `AdminServiceTest`, `ProfileServiceTest` (service-layer, stub repos) | No `Profile` entity factory tests; no `Admin` entity tests; no HTTP resource tests (`AdminResource`, `ProfileResource`); no Testcontainers persistence tests |
| wealth | `TransactionTest` (builder), `StatementUploadTest` (builder/status) | `AccountServiceTest`, `StatementUploadServiceTest` (full v0.4 coverage inc. error log + dedup), `TransactionServiceTest`, `StatementCsvParserTest`, `AccountPanacheRepositoryTest`, `TransactionPanacheRepositoryTest`, `StatementUploadIntegrationTest`, `UploadErrorLogPanacheRepositoryTest`, `StatementUploadResourceTest` | No `Account` domain entity unit tests; no `AccountResource` HTTP adapter test; `TransactionResource` HTTP adapter not tested |
| health | None — no domain-layer test class exists | `DoctorVisitServiceTest`, `VitalReadingServiceTest`, `VitalReadingPanacheRepositoryTest` | No `VitalReading` or `DoctorVisit` entity factory tests; no `VitalReadingResource` HTTP adapter test; no `DoctorVisitResource` HTTP adapter test |
| household | `CalendarEventTest`, `GoalTest`, `InventoryItemTest` (domain factory + validation) | `CalendarEventServiceTest`, `GoalServiceTest`, `InventoryItemServiceTest`, `CalendarEventPanacheRepositoryTest`, `GoalPanacheRepositoryTest`, `InventoryItemPanacheRepositoryTest`, `CalendarEventResourceTest`, `GoalResourceTest`, `InventoryItemResourceTest` | Most complete domain; no edit path tests for inventory or calendar (no PUT endpoint exists) |
| web-gateway | N/A | `ProjectionCalculationEngineTest` (unit, all 4 compute paths + edge cases), `ProjectionResourceTest`, `WealthGatewayResourceTest`, `ProfileGatewayResourceTest`, `HealthGatewayResourceTest`, `HouseholdGatewayResourceTest` | No test for `GET /accounts/{id}/uploads/{id}/errors` proxy — this endpoint does not exist in gateway; `refreshAll` has no test for downstream service failure (exception propagation untested) |
| shared / arch | `DomainRulesTest` — 8 ArchUnit rule groups | — | No ArchUnit rule enforcing `profile_id` presence in adapter queries; no ArchUnit rule verifying every use-case interface has at least one test class |
| frontend (Jest) | 25 test suites, 355 tests | — | No test suite for upload error panel rendering (covered only via inline mock in `Transactions.test.js`); no test for `Physical Assets` (no page exists); no test for `Vitals` edit flow (no edit path exists); JS branch coverage 74.55% — below any sensible gate |
| E2E (Playwright) | 17 tests across 5 spec files | — | No E2E coverage for CSV upload flow, error panel, skipped-duplicates panel, household (calendar/inventory/goals), health vitals, doctor visits, dashboard refresh |

### Confirmed Bugs

1. **Gateway `/errors` bypass** — `web/src/api/wealth.js` line 47–48: `getUploadErrors` calls `GET /v1/accounts/{accountId}/uploads/{uploadId}/errors` via the shared `client.js` wrapper which uses `API_BASE_URL` (port 8080, gateway). However `WealthGatewayResource.java` and `WealthServiceClient.java` do not declare this endpoint — the gateway returns 404 and the frontend silently falls back to `errorsFetchFailed = true`. The call never reaches the wealth service through the gateway; it fails at the gateway routing layer. Severity: HIGH. Impact: upload error details are never visible to the user when a CSV parse fails; the UI shows "Upload failed — check your file" with no structured error detail.

2. **Net worth formula uses static opening_balance** — `ProjectionCalculationEngine.java` lines 72–74 (`computeNetWorth`) and lines 128–131 (`computeTotalBalance`): both methods sum `account.path("opening_balance")` across active accounts. No transaction history is consulted. The goal progress calculation at line 100 also uses `totalBalance` derived from opening balances only. Correct formula: `opening_balance + SUM(CREDIT transactions) - SUM(DEBIT transactions)` per account, requiring a new `/balance` endpoint on the wealth service. Severity: HIGH. Impact: dashboard net worth and goal progress figures drift from reality as transaction volume grows; vacation planner budget check in v0.5 will use the wrong figure.

3. **`TransactionPanacheRepository` — no profile_id filter** — `TransactionPanacheRepository.java` lines 40–62 (`findByAccountId`) and lines 65–68 (`existsByDeduplicationKey`): both queries filter only on `accountId`, not on `profile_id`. The security relies on the caller (StatementUploadService) having verified that the account belongs to the correct profile. This implied chain is not enforced at the repository layer and is invisible to future developers. Severity: MEDIUM. Impact: if a caller passes an accountId without profile verification, transactions from other profiles are visible; the dedup check could incorrectly match across profiles.
   **Correction — 2026-07-02 (architect verification pass):** Q12's resolution (Option A) was implemented at the repository layer — `findByAccountId`/`existsByDeduplicationKey`/`sumAmountByTxnType` in `TransactionPanacheRepository` all now accept an optional `profileId` and apply it via a subquery when non-null. However, `TransactionService.listByAccount()` still hardcodes `null` for profileId (explicit code comment opting out) and `TransactionResource.listTransactions()` has no `profile_id` query param at all — so the filter exists and works, but is never invoked from the HTTP surface. This is **not** "partially done" in the sense of an incomplete repo fix; the repo fix is complete. The gap is entirely in the service/resource layer not threading the parameter through. Scheduled as a v0.5 Phase 0 fix per product owner decision (`OpenQuestions.md` Q28).

4. **`refreshAll` is not exception-isolated per step** — `ProjectionCalculationEngine.java` lines 54–59: four compute calls execute sequentially with no try-catch around individual steps. If `computeVitalsSummary` throws (e.g., health service is down), `computeEventSummary` never runs and the partial snapshot is not written. The Architect's comment at line 50 says "Each compute step is independent; a failure in one does not block the others" — this is incorrect; the implementation does not match the Javadoc. Severity: MEDIUM. Impact: any domain service outage causes a total dashboard refresh failure with no partial result.

5. **No `PUT /vitals/{id}` endpoint** — `VitalReadingResource.java` has `@GET`, `@POST`, `@GET /{id}`, and `@DELETE /{id}` only (lines 27–58). No `@PUT` or `@PATCH` method exists. Users cannot edit a logged vital reading — delete and re-enter is the only correction path. Severity: LOW (UX gap, confirmed). Impact: inconsistent with doctor visits which support full CRUD.

6. **No `PUT /inventory-items/{id}` endpoint** — `InventoryItemResource.java` has `@GET`, `@POST`, `@GET /{id}`, and `@DELETE /{id}` only (lines 35–76). No edit path. Users cannot update quantity, category, or platform on an existing inventory item. Severity: LOW (UX gap, confirmed). Impact: delete-and-re-add is the only correction path.

7. ~~**No Physical Assets frontend page**~~ **RESOLVED 2026-06-30.** Correction: the backend was also not implemented prior to this fix — this finding's premise ("the backend is implemented") was wrong. Full vertical slice delivered as v0.4.1; page now at `/wealth/physical-assets`.

### Missing Test Cases (Priority Order)

1. **`WealthGatewayResourceTest` — `GET /accounts/{id}/uploads/{id}/errors` proxy** | `application/web-gateway/src/test/` | Blocks gateway fix verification; without this test, adding the proxy endpoint has no coverage gate.

2. **`ProjectionCalculationEngineTest` — downstream service failure isolation** | `application/web-gateway/src/test/` | Verifies that a `RuntimeException` in `computeVitalsSummary` does not prevent `computeEventSummary` from running. Currently the Javadoc claims isolation but the code and tests do not enforce it.

3. **`AccountResource` HTTP adapter unit test** | `application/domain/wealth/adapters/src/test/` | `AccountResource.java` has no test class. The ArchUnit `gateway_resources_must_have_corresponding_test` rule covers gateway resources only — domain HTTP resources are unguarded.

4. **`TransactionResource` HTTP adapter unit test** | `application/domain/wealth/adapters/src/test/` | Same gap as above; `TransactionResource.java` has no test class.

5. **`VitalReadingResource` and `DoctorVisitResource` HTTP adapter unit tests** | `application/domain/health/adapters/src/test/` | Health HTTP layer is entirely untested at the unit level; only service and one persistence test exist.

6. **`Profile` domain entity factory tests** | `application/domain/profile/domain/src/test/` | Only `BloodTypeTest` exists. `Profile.java` and `Admin.java` have no unit tests covering creation, field validation, or edge cases.

7. **`VitalReading` domain entity unit test** | `application/domain/health/domain/src/test/` | No test class exists for the health domain layer at all.

8. **`Account` domain entity unit test** | `application/domain/wealth/domain/src/test/` | `Account.java` has no unit test; only `Transaction` and `StatementUpload` builders are covered.

9. **Frontend — upload error panel render test** | `web/src/pages/Wealth/` | The `UploadErrorPanel` component renders structured error rows, but its rendering under `errorsFetchFailed = true` is not tested in isolation as a component test. It is only covered as a side-effect in the upload flow tests.

10. **Frontend — branch coverage below 74.55%** | `web/src/` | No coverage gate is enforced on branches. The Reports page, Dashboard projection display, and goal progress bar have low branch coverage; failures in those paths are not caught by the current test suite.

### Fix Plan (by milestone)

**v0.5 fixes (must complete before vacation planner is built):**

- ~~Add `GET /accounts/{accountId}/uploads/{uploadId}/errors` to `WealthServiceClient`...~~ DONE (Epic 8 Phase 2).
- ~~Fix `ProjectionCalculationEngine.computeNetWorth()` and `computeTotalBalance()`...~~ DONE (Epic 8 Phase 1, Bug 2 fix).
- ~~Wrap each compute step in `refreshAll()` in its own try-catch...~~ DONE 2026-06-30 (Bug 3 fix). New test: `refreshAll_oneStepThrows_othersStillRun`.
- ~~Build Physical Assets frontend page at `/wealth/assets`...~~ DONE 2026-06-30 as full vertical slice (page is at `/wealth/physical-assets`, not `/wealth/assets` — path corrected during implementation).
- Add `PUT /vitals/{id}` endpoint to `VitalReadingResource` and edit modal to the Vitals frontend page.
- Add `PUT /inventory-items/{id}` endpoint to `InventoryItemResource` and edit modal to the Inventory frontend page.

**v0.6 fixes (testing foundation, re-scoped):**

- Add ArchUnit rule: every class in `..ports.input..` that is an interface must have at least one test class in the same domain's test classpath that references it.
- Add `AccountResource` and `TransactionResource` HTTP adapter unit tests (stub use case pattern as in `StatementUploadResourceTest`).
- Add `VitalReadingResource` and `DoctorVisitResource` HTTP adapter unit tests.
- Add `Profile` domain entity unit tests covering field validation and creation factory.
- Add `VitalReading` domain entity unit test.
- Add explicit `profile_id` secondary filter to `TransactionPanacheRepository.findByAccountId()` and `existsByDeduplicationKey()`, or document the implied chain as an ADR trade-off with a test that verifies the account ownership check happens before these are called.
- Enforce Jest branch coverage gate at 80% minimum in `package.json` Jest config.

**v1.0 fixes:**

- Add ArchUnit rule flagging `PanacheRepositoryBase` subclasses whose `find()` calls do not include `profile_id` in the predicate. Low-urgency now (current code is clean) but required before multi-user auth in v1.0.
- Contract tests: validate all four domain OpenAPI contracts (`application/contract/*.yaml`) against live service responses using a schema validation library (e.g., Atlassian Swagger Request Validator). Add to CI pipeline as a post-deploy check.
- Add ADR for `profile_id`-in-domain trade-off (Architect gap #6 — `CalendarEvent` and household entities hold `profileId` as a domain field despite ADR-006).

---

## Consolidated Next Steps — Architect Decision — 2026-06-29

### Immediate Fixes (before v0.5 work begins)

These are correctness failures or invariant violations in the current codebase. Do them first — they are small and block other work.

**1. Gateway `/errors` proxy — wealth-developer**
- File: `application/web-gateway/src/main/java/.../WealthServiceClient.java`
- File: `application/web-gateway/src/main/java/.../WealthGatewayResource.java`
- File: `application/contract/gateway.yaml`
- Add one method to `WealthServiceClient`: `@GET @Path("/v1/accounts/{accountId}/uploads/{uploadId}/errors") List<UploadErrorResponse> getUploadErrors(...)`.
- Add one proxy method to `WealthGatewayResource` delegating to the client.
- Add the path to `gateway.yaml` under the wealth tag.
- Run `cd web && npm run generate:api` after the contract update.
- Add a test case to `WealthGatewayResourceTest` for the new proxy method using `@InjectMock @RestClient`.
- This is the only known gateway bypass in the codebase. ADR-002 requires it to be fixed before any v0.5 work starts.

**2. Net worth formula — wealth-developer + quarkus-developer (gateway)**
- File: `application/web-gateway/src/main/java/.../ProjectionCalculationEngine.java` lines 72–74 (`computeNetWorth`) and lines 128–131 (`computeTotalBalance`)
- File: `application/web-gateway/src/test/java/.../ProjectionCalculationEngineTest.java` (test validates wrong formula — must be updated)
- Add `GET /v1/accounts/{accountId}/balance` endpoint on the wealth service returning `opening_balance + SUM(CREDIT) - SUM(DEBIT)`.
- Update `WealthServiceClient` to call this endpoint.
- Update `computeNetWorth()` and `computeTotalBalance()` to use the balance endpoint response.
- Update `ProjectionCalculationEngineTest` to assert against the transaction-history-based result.
- The vacation planner budget check in v0.5 uses this figure. Wrong formula = wrong budget validation.

**3. `refreshAll()` per-step exception isolation — quarkus-developer (gateway)**
- File: `application/web-gateway/src/main/java/.../ProjectionCalculationEngine.java` lines 54–59
- Wrap each of the four `compute*()` calls in its own try-catch block. Log the exception via `AppLogger` and continue to the next step. A partial snapshot written is better than no snapshot at all.
- The Javadoc at line 50 already documents this intent — the code just does not implement it.
- Update `ProjectionCalculationEngineTest` to verify that a `RuntimeException` in `computeVitalsSummary` does not prevent `computeEventSummary` from executing.

**4. `TransactionPanacheRepository` ownership assertion — wealth-developer**
- File: `application/domain/wealth/adapters/src/main/java/.../TransactionPanacheRepository.java` lines 40–62 (`findByAccountId`) and lines 65–68 (`existsByDeduplicationKey`)
- Decision (pending Q12 answer — see blocking questions below): add an explicit `account.profile_id = ?` join condition to both queries, OR add an ownership assertion in `StatementUploadService` before the repository call.
- If the join approach is chosen: update the `TransactionRepository` output port interface to accept `profileId` and write an ADR-006 addendum.
- If the assertion approach is chosen: add the check in `StatementUploadService` with a comment marking it as load-bearing.
- Do not leave this as a silent implicit chain — document the decision wherever the code ends up.

---

### v0.5 Scope (revised based on review findings)

The existing v0.5 plan (Vacation Planner, Consolidated Action Center) remains, with additions from the three reviews. The immediate fixes above must land before any v0.5 feature work.

**Carry-in from reviews (gaps not in original v0.5 plan):**

- ~~Physical Assets frontend page~~ — DONE 2026-06-30 (full vertical slice, see `domain-state/wealth.md`). No longer blocks the Action Center.
- `PUT /vitals/{id}` endpoint on health service — `VitalReadingResource.java` has no `@PUT` method. health-developer. Small: one use case method, one adapter handler, update `health.yaml`, update frontend vitals page edit modal.
- `PUT /inventory-items/{id}` endpoint on household service — `InventoryItemResource.java` has no `@PUT` method. household-developer. Same pattern as vitals edit.
- Edit modal on Vitals frontend page — react-developer, after health-developer adds the endpoint.
- Edit modal on Inventory Items frontend page — react-developer, after household-developer adds the endpoint.
- Add copy to Reports page and Dashboard: label balance figures as "Based on account opening balances" as an interim UX fix until the net worth formula fix lands. One-line frontend change. react-developer. Do this immediately; do not wait for the formula fix.

**Original v0.5 features (unchanged):**

- Vacation Planner: budget validation against liquid savings, asset compliance warning (PUC/insurance expiry before trip date). Physical Assets frontend dependency now satisfied (delivered 2026-06-30). Nav home confirmed 2026-07-02: under Household, route `/household/vacation-planner` (`OpenQuestions.md` Q27). gateway quarkus-developer + react-developer.
- Consolidated Action Center: upcoming events, vehicle compliance deadlines, biometric streak gaps. Physical Assets frontend dependency now satisfied. Biometric streak gap definition still unresolved (`OpenQuestions.md` Q30) — blocks this feature's implementation only, not the rest of v0.5.

**Resolved before v0.5 feature work started:**
- PROP-005 (frontend state management) — **RESOLVED 2026-07-02 → ADR-018 (React Query).** See `documents/ARCHITECTURE_DECISIONS.md` ADR-018 and `OpenQuestions.md` Q26.

---

### v0.6 Scope (revised)

The original v0.6 milestone ("Testing Foundation") is partially already done for the implemented domains. Re-scope it to cover the real remaining gaps.

**Testing work (re-scoped from original v0.6):**

- `AccountResource` HTTP adapter unit test — wealth-developer. Pattern: copy `StatementUploadResourceTest`, stub use cases.
- `TransactionResource` HTTP adapter unit test — wealth-developer.
- `VitalReadingResource` and `DoctorVisitResource` HTTP adapter unit tests — health-developer.
- `Profile` domain entity unit tests (`Profile.java`, `Admin.java`) — profile-developer.
- `VitalReading` domain entity unit test — health-developer.
- ArchUnit rule: every interface in `..ports.input..` must have at least one test class referencing it — quality-manager.
- Jest branch coverage gate at 80% minimum in `web/package.json` Jest config — react-developer.
- ADR for `profile_id`-in-domain trade-off (Architect gap #6) — architect.

**UX items deferred from v0.5:**

- Transaction list pagination — react-developer + wealth-developer. Add `page` and `size` query params to `GET /transactions`.
- Date-range filter on doctor visit list — health-developer + react-developer.
- Goal progress auto-refresh cue — add a note on the Goals page explaining manual refresh dependency. react-developer. One-line copy change.

**Duplicate Resolution UI and Quarantine Protocol stay deferred here:**

- Duplicate Resolution UI (accept/reject flagged duplicates) — deferred to v0.6 from v0.4. Keep here.
- Quarantine Protocol (grocery CSV row-level quarantine) — the inventory CSV import itself does not exist yet. Quarantine logic for a non-existent import path should not block v0.5. Keep in v0.6 after inventory CSV import is built.

**Resolve before v0.6 starts:**
- PROP-004 (API versioning strategy) — must be answered before v1.0 auth integration; do it at v0.6 planning.
- Q9 (Java coverage floor per module) — must be answered to configure the build gate.

---

### What Stays Deferred (v1.0+)

- **Async `refreshAll()`** — the per-step try-catch (immediate fix #3) buys enough resilience for v0.5 and v0.6. A full async reactive pipeline is a v1.0 concern when multiple users are hitting the dashboard simultaneously.
- **ArchUnit `profile_id` query rule** — current code is clean. Add this before v1.0 auth when the risk of a new developer bypassing the filter becomes real.
- **Contract tests (OpenAPI schema validation)** — add in v1.0 alongside auth integration; the risk surface grows when token-scoped data isolation is added.
- **Vital trend charts / BMI auto-calculation** — health domain backlog. Deferred past v0.5.
- **Inventory item lifecycle (consumed/restock)** — pending Q6 decision. If the product owner picks option A or C, pull into v0.5 or v0.6; otherwise defer to v1.3.
- **Manual transaction entry form** — pending Q7 decision. If approved, pull into v0.5 (small: one endpoint + one form). If deferred, moves to v1.3.
- **Advanced financial engine ("Mahesh Summation Rule", EMI arbitrage, reallocation triggers)** — not delivered in v0.4. Pending Q8 decision for milestone assignment.
- **Task Tracking (household)** — no schema exists. v0.5 or later per product owner priority.
- **Home Automation Mapping** — not started. No schema. v1.0+.
- **E2E CI gate** — pending Q10 decision. Keeping Playwright as a manual pre-release gate for now is acceptable until Q10 is answered.

---

### Open Questions Blocking v0.5 Planning

**Status update (2026-06-30): Q1–Q13 are now all resolved** — see `documents/OpenQuestions.md` for the authoritative resolutions. Kept the original list below for historical traceability, annotated with outcomes.

- ~~**Q1**~~ — RESOLVED: keep `profileId` in domain entities (pragmatic trade-off, documented).
- ~~**Q2**~~ — RESOLVED + DONE: net worth fix shipped (Epic 8 Phase 1, Bug 2).
- ~~**Q3**~~ — RESOLVED + DONE: `/errors` gateway proxy shipped (Epic 8 Phase 2, Bug 1).
- ~~**Q4**~~ — RESOLVED + DONE: per-step try-catch isolation in `refreshAll()` shipped 2026-06-30 (Bug 3).
- ~~**Q5**~~ — RESOLVED + DONE: Physical Assets full vertical slice shipped 2026-06-30 as a v0.4.1 patch.
- ~~**Q6**~~ — RESOLVED (custom): `is_consumed` flag means "used in a calculation," not "used up" — no deletion/expiry, full year-over-year history retained. Not yet implemented.
- ~~**Q7**~~ — RESOLVED: manual transaction entry, Option C (`source = MANUAL` tag). Not yet implemented.
- ~~**Q8**~~ — RESOLVED: superseded by Q13 — assigned to v0.6 "Financial Intelligence Engine."
- ~~**Q9**~~ — RESOLVED: 70% project-wide Java coverage floor, CI-enforced, v0.6.
- ~~**Q10**~~ — RESOLVED: Playwright E2E required in CI via stub backend.
- ~~**Q11**~~ — RESOLVED: Swagger Request Validator contract tests, v0.6.
- ~~**Q12**~~ — RESOLVED: add `profile_id` as a secondary filter in `TransactionPanacheRepository`. Partially implemented already (`findByAccountId`/`existsByDeduplicationKey`/`sumAmountByTxnType` all accept an optional `profileId`, used by the balance endpoint) — but `TransactionService.listByAccount`/`TransactionResource` still don't thread it through end-to-end. Remaining work, not blocked on a decision anymore.
- ~~**Q13**~~ — RESOLVED: Epic 8 (8.1–8.6) assigned to v0.6, sequenced after the (now-delivered) net-worth fix.

---

### Developer Assignments (recommended)

| Workstream | Owner | When |
|---|---|---|
| ~~Gateway `/errors` proxy (fix #1)~~ | wealth-developer | DONE (Epic 8 Phase 2) |
| ~~Net worth formula fix — wealth balance endpoint (fix #2, backend)~~ | wealth-developer | DONE (Epic 8 Phase 1) |
| ~~Net worth formula fix — gateway engine + test (fix #2, gateway)~~ | quarkus-developer (gateway) | DONE (Epic 8 Phase 1) |
| ~~`refreshAll()` per-step try-catch (fix #3)~~ | quarkus-developer (gateway) | DONE 2026-06-30 |
| `TransactionPanacheRepository` profile ownership (fix #4) | wealth-developer | Q1/Q12 now answered; repo-level filter exists, HTTP-layer wiring still open |
| ~~Physical Assets frontend page `/wealth/assets`~~ | react-developer | DONE 2026-06-30 (full vertical slice, path is `/wealth/physical-assets`) |
| `PUT /vitals/{id}` endpoint | health-developer | v0.5 |
| Vitals edit modal (frontend) | react-developer | v0.5, after health endpoint merged |
| `PUT /inventory-items/{id}` endpoint | household-developer | v0.5 |
| Inventory Items edit modal (frontend) | react-developer | v0.5, after household endpoint merged |
| "Opening balances only" label on Reports + Dashboard | react-developer | Immediate — no backend dependency |
| Vacation Planner (cross-domain) | quarkus-developer (gateway) + react-developer | v0.5, after immediate fixes are merged |
| Consolidated Action Center | quarkus-developer (gateway) + react-developer | v0.5 Phase 3 — Physical Assets dependency satisfied (2026-06-30); PROP-005 resolved (2026-07-02); blocked only on biometric streak gap definition (Q30) |
| PROP-005 state management decision | architect | RESOLVED 2026-07-02 → ADR-018 (React Query) |
| TransactionResource/TransactionService profile_id threading fix | wealth-developer | v0.5 Phase 0 — fix now, per product owner (Q28) |
| v0.6 adapter unit tests (wealth HTTP layer) | wealth-developer | v0.6 |
| v0.6 adapter unit tests (health HTTP layer) | health-developer | v0.6 |
| v0.6 domain entity unit tests (profile) | profile-developer | v0.6 |
| v0.6 domain entity unit tests (health) | health-developer | v0.6 |
| ArchUnit port interface coverage rule | quality-manager | v0.6 |
| Jest branch coverage gate | react-developer | v0.6 |
| PROP-004 API versioning ADR | architect | v0.6 planning session |
| ADR for `profile_id`-in-domain trade-off | architect | v0.6 |
| Duplicate Resolution UI | wealth-developer + react-developer | v0.6 |
| Transaction list pagination | wealth-developer + react-developer | v0.6 |
| Doctor visit date-range filter | health-developer + react-developer | v0.6 |

---

## Business Analyst Review — Automated Wealth Intelligence Engine — 2026-06-30

### Scope Summary

The product owner currently maintains his family's complete financial picture (assets, liabilities, goals, validation rules) in a manually edited markdown file, parsed by standalone Python scripts into JSON outputs and a validation report. He wants this same intelligence — net worth and asset categorization, liquidity tiering, loan amortization and arbitrage tracking, five formula-driven financial goals, and a zero-leakage validation rule engine — built natively into the wealth domain's CQRS read model, driven by uploaded bank/investment/loan statements rather than manual markdown editing. Manual entry is retained only for true one-time or rarely-changing policy facts (loan principal/start date/tenure, expected-return assumptions, goal target thresholds) — everything else (net worth, category subtotals, EMI splits, outstanding balances, goal progress) must be derived from `transaction` and `physical_asset` records on every dashboard refresh.

### Relationship to Existing Epic 8

This is not a new idea — it is the corrected, fully specified version of Epic 8 ("The Mathematical Engine & Zero Leakage"), which has existed in `REQUIREMENTS_wealth_domain.md` since before v0.4 planning but was never implemented. v0.4 was executed as a CSV error-handling sprint instead, leaving Epic 8's three original use cases (8.1 Mahesh Summation Rule, 8.2 EMI Arbitrage, 8.3 Dynamic Triggers) as undelivered requirements. This gap was already flagged by the BA review on 2026-06-29 (table row: "Wealth CQRS Read Model... Not Delivered") and by `OpenQuestions.md` Q8. `REQUIREMENTS_wealth_domain.md` Epic 8 has now been expanded in place with six concrete use cases (8.1–8.6), replacing the three short originals, scoped entirely within the wealth domain's existing data (`account`, `transaction`, `physical_asset`, `metadata JSONB`) to remain compliant with the "no cross-domain features before v0.5" rule.

### Recommended Milestone Placement

This is a large feature set — six use cases covering categorization, liquidity tiering, loan amortization, arbitrage monitoring, a five-type goals engine, and an automated validation gate. It should not be folded into the existing v0.5 scope as currently defined (Vacation Planner, Consolidated Action Center), which is already a cross-domain milestone with its own immediate-fix backlog (gateway `/errors` proxy, net worth formula fix, `refreshAll()` isolation — see Architect Review above). Recommendation:

- Treat this as a dedicated sub-milestone — **v0.4.2 "Financial Intelligence Engine"** or a renamed **v0.6** focus (the current v0.6 "Testing Foundation" is already being re-scoped per the Architect/QA reviews above, since most of its original test-coverage items are substantially done). Placing the engine at v0.6 sequences it after the v0.5 net-worth-formula fix (Q2/immediate fix #2), which this engine depends on directly — the engine cannot compute accurate liquidity, debt-to-asset, or goal-progress figures while net worth is still calculated from `opening_balance` alone.
- Do not attempt to deliver this inside v0.5 alongside the Vacation Planner and Action Center. Both v0.5 features are cross-domain aggregation views; this engine is deep single-domain computation. Mixing them risks both slipping.
- This recommendation is for product owner confirmation, not a unilateral schedule decision — see `OpenQuestions.md` Q14 (new) for the explicit milestone question.

### Data Model Implications (flag only, do not design)

The following are implications for the architect and developer agents to evaluate — no schema is proposed here:

- **Goal model fork:** the household domain's `goal` table (simple savings-target-with-progress-bar) cannot represent the five formula-driven goal types (Debt Crossover, 30-70 Target, Freedom Runway, Insurance Free, Year One). A new goal representation is needed inside the wealth domain's CQRS projection layer — distinct from `household.goal`. Whether `household.goal` and this new wealth goals engine coexist as two goal systems, or whether `household.goal` is eventually superseded, is a product decision, not assumed here.
- **Loan metadata completeness:** `wealth.account.metadata JSONB` needs to reliably carry original principal, start date, tenure, and interest rate for every loan account before EMI-split derivation (Use Case 8.2) can run. This is already directionally planned per `ARCHITECTURE_DECISIONS.md`/wealth-developer's current schema notes, but no account currently has this populated.
- **Account/asset classification fields:** asset category (real estate, financial investment, precious metal, vehicle, cash/bank) and liquidity tier (0–3mo, 3–12mo, 1–5yr, 5+yr) and purpose tag (emergency fund, retirement, growth, income, education, long-term reserve) need to exist per account/asset — likely as `metadata JSONB` entries rather than new typed columns, consistent with the existing enum-discriminator policy (no SQL ENUMs; OpenAPI + Java enum validation).
- **Income figure absence:** no domain in this codebase currently models household income. The 30-70 Target and Year One goal formulas need an income/expense figure that is not derivable from wealth transactions alone (it could be approximated from recurring CREDIT transactions, but that is fragile and was explicitly called a manual policy choice by the product owner's brief). This is a genuine gap — flagged to `OpenQuestions.md` Q17.
- **Validation result storage:** the zero-leakage validation report (PASS/WARNING/CRITICAL FAILURE per check) needs to be persisted as part of (or alongside) the `projections.dashboard_snapshot` CQRS output so the frontend can render it — this is new payload shape, not a new table necessarily, but flagged for the architect's schema review.
