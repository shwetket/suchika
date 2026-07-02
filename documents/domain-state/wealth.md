# Wealth Domain State

## Objective

Give any agent or developer instant context on the wealth domain — accounts, transactions, CSV uploads, physical assets. Includes schema, ADRs baked into Key Design Decisions, and the current backlog.

## Use Cases

- Before working on wealth backend or frontend — check Implementation Status and Key Files
- When adding a new feature to wealth — review Key Design Decisions to avoid re-introducing known anti-patterns (e.g., profileId in CreateAccountCommand)
- After completing wealth work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-02 (v0.5 Phase 0 — TransactionResource/TransactionService profile_id threading fix)
**Version:** v0.4 Phase 1+2+3+4 complete; Epic 8 fully delivered (all 11 gateway projection steps live); v0.5 Phase 0 data-isolation gap closed
**Port:** 8082

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Account CRUD | ✅ Complete | 7 account types |
| Transaction list/filter | ✅ Complete | Date range + type filters |
| CSV Statement Upload | ✅ Complete | PENDING → SUCCESS/FAILED |
| Upload rollback | ✅ Complete | Deletes all txns for an upload |
| Deduplication | ✅ Complete | Cross-file dups rejected; 4-field key (no description) |
| Physical Assets | ✅ Complete | Full vertical slice as of 2026-06-30 — see entry below. Prior "Complete" status was stale: only the `V2__physical_assets.sql` migration existed, zero Java backend or frontend |
| Malformed CSV rejection | ✅ Complete | CsvParseException → upload_error_log |
| Upload error log endpoint | ✅ Complete | GET /{account_id}/uploads/{upload_id}/errors |
| Skipped rows in upload response | ✅ Complete | skipped_duplicates list + inserted_count |
| Dashboard CQRS projections | 🔲 v0.3 | projections.dashboard_snapshot not live |
| Transaction pagination | 🔲 v0.3 | |
| Epic 8 Phase 1: account.metadata JSONB column | ✅ Complete | Flyway V6, `Account.metadata`/`AccountEntity.metadata` (JSONB, Jackson round-trip) |
| Epic 8 Phase 1, Bug 2: net worth formula fix | ✅ Complete | New `GET /v1/accounts/{id}/balance`; `current_balance = opening_balance + SUM(CREDIT) - SUM(DEBIT)`; gateway `ProjectionCalculationEngine.currentBalanceFor()` calls it instead of reading `opening_balance` |
| Epic 8 Phase 1, Bug 4: missing profile_id filter | ✅ Complete | `TransactionRepository.findByAccountId`/`existsByDeduplicationKey` now take `profileId`; Panache adapter filters via subquery against `AccountEntity.profileId` when non-null |
| Epic 8 Phase 1: account classification metadata write path | ✅ Complete | `AccountUseCase.updateAccountClassification`; `PATCH /v1/accounts/{id}/classification`; merges category/liquidity_tier/purpose_tag into existing metadata map |
| Epic 8 Phase 1: validation seed (category resolution check) | ✅ Complete | `ProjectionCalculationEngine.computeCategoryValidation` → `WEALTH_CATEGORY_VALIDATION` snapshot key; correctly flags all accounts as uncategorized in Phase 1 (category not populated until Phase 2 — expected, not faked) |
| Epic 8 Phase 2: manual expense category tagging | ✅ Complete | `ExpenseCategory` enum (5 values); `PATCH /accounts/{id}/transactions/{txnId}/category` (single) + `PATCH /accounts/{id}/transactions/category` (bulk-by-selection, Q24 resolution — not a rules engine) |
| Epic 8 Phase 2, bug fix: `TransactionEntity.metadata` never wired | ✅ Complete | JSONB `metadata` column existed since V1 but `from()`/`toDomain()` never touched it — every `save()` silently reset metadata to `{}`. Fixed with Jackson round-trip helpers mirroring `AccountEntity` (hard prerequisite for category tagging to persist at all) |
| Epic 8 Phase 2: joint_owners on account classification | ✅ Complete | `UpdateAccountClassificationRequest.jointOwners` (List\<String\>); stored comma-joined in `metadata.joint_owners` (flat `Map<String,String>` — no domain type widening) |
| Epic 8 Phase 2: gateway proxies for Phase 1+2 endpoints | ✅ Complete | `getAccountBalance`, `updateAccountClassification`, `updateTransactionCategory`, `bulkUpdateTransactionCategory`, `getUploadErrors` (Bug 1 fix) all added to `WealthGatewayResource`/`WealthServiceClient` |
| Epic 8 Phase 2: Kotak joint account placeholder | ⚠️ Partial | Created live (`account_id 7e3c4712-cb82-4b90-9a73-9f7f4acf29db`, opening_balance 0) but attached to the **seed `Test Member` profile** (`00000000-0000-0000-0000-000000000002`) — dev DB has no real Shweta/Ketan profile records yet. `joint_owners` left unset (no second real profile to reference); tagged `purpose_tag: PENDING_JOINT_OWNER_ASSIGNMENT` as a findable marker. Re-point `profile_id` and set `joint_owners` via `PATCH /accounts/{id}` + `/classification` once real household profiles exist |
| Epic 8 Phase 2: `StatementCsvParser` header-list extension (BoB/BoI/Kotak/credit card) | 🔲 Deferred | User has no real bank statement files yet to validate header formats against — do not guess candidate headers. Revisit when sample CSVs are available |
| Bug 3: `refreshAll()` per-step exception isolation | ✅ Complete | `ProjectionCalculationEngine.refreshAll()` now wraps each of the 6 compute steps via a `runStep()` helper (try-catch + `AppLogger.error`); one step failing no longer blocks the others. Pulled forward from its originally-planned Phase 4 slot per `OpenQuestions.md` Q4 (product owner chose to fix now rather than wait) |
| `AppLogger.error(String, Throwable, Object...)` argument-order bug | ✅ Fixed | Pre-existing bug in `shared/`: called `Log.errorf(message, throwable, params)` but JBoss Logging's matching overload is `errorf(Throwable, String, Object...)` — Throwable must be first. The mismatched call silently fell back to `errorf(String, Object...)`, packing `throwable` and the `params` array into one mangled varargs array (visible as `[Ljava.lang.Object;@hash` in log output). Fixed to `Log.errorf(throwable, message, params)`. No other call site used this overload, so no behavior change elsewhere |
| Physical Assets — full vertical slice | ✅ Complete (2026-06-30) |
| Epic 8 Phase 3: `AmortizationCalculator` (pure domain function) | ✅ Complete (2026-07-01) | EMI formula (`P*r*(1+r)^n/((1+r)^n-1)`), outstanding balance, remaining tenure, interest split. Zero framework deps, `plain new` testable |
| Epic 8 Phase 3: `GET /v1/accounts/{id}/amortization` endpoint | ✅ Complete (2026-07-01) | Reads `interest_rate` typed column + `original_principal`/`loan_start_date`/`original_tenure_months` from metadata; BadRequest if loan metadata not set; 404 if not a loan account type |
| Epic 8 Phase 3: 4 loan classification fields on `UpdateAccountClassificationRequest` | ✅ Complete (2026-07-01) | `loan_original_principal`, `loan_start_date`, `loan_tenure_months`, `linked_offset_account_id` — all merged into `account.metadata` via existing PATCH /classification endpoint |
| Epic 8 Phase 3: `computeEmiTracking` gateway step | ✅ Complete (2026-07-01) | Filters loan accounts, calls getAmortization per loan, applies offset arbitrage (interest saved = offsetBalance × monthlyRate), aggregates into `WEALTH_EMI_TRACKING_FAMILY` snapshot |
| Epic 8 Phase 3: `computeLiquidityTiers` gateway step | ✅ Complete (2026-07-01) | Groups accounts by `metadata.liquidity_tier`, sums balances per tier (LIQUID/SEMI_LIQUID/ILLIQUID/LOCKED/UNCLASSIFIED), stores `WEALTH_LIQUIDITY_TIERS_FAMILY` snapshot |
| Epic 8 Phase 3: `computeGrowthProjection` gateway step | ✅ Complete (2026-07-01) | Reads growth rates from `application.properties` (MUTUAL_FUND=12%, NPS=10%, PPF=7.1%), computes 5yr/10yr projected values, stores `WEALTH_GROWTH_PROJECTION_FAMILY` snapshot |
| Epic 8 Phase 3: Dashboard EMI + Liquidity cards | ✅ Complete (2026-07-01) | Dashboard shows Monthly EMI card (with offset savings), Liquidity Tier breakdown. Both conditionally rendered when snapshots present |
| Epic 8 Phase 3: Loan Details form in Accounts.js | ✅ Complete (2026-07-01) | Edit modal shows loan fields (principal, start date, tenure, offset account dropdown) when account_type is HOME_LOAN/CAR_LOAN/PERSONAL_LOAN; saves via PATCH /classification | Built domain (`PhysicalAsset`, `AssetType`, `RegistrationType`), ports (`PhysicalAssetUseCase`, `PhysicalAssetRepository`), adapters (`PhysicalAssetEntity` w/ JSONB metadata Jackson round-trip mirroring `AccountEntity`, `PhysicalAssetPanacheRepository`, `PhysicalAssetService`, `PhysicalAssetResource` at `/v1/physical-assets`). Validates `registration_number` uniqueness (`ConflictException`) since the DB has a `uq_registration_number` constraint but no prior app-layer check existed. `wealth.yaml`/gateway mirror/`gateway.yaml` updated with full CRUD paths + `AssetType`/`RegistrationType`/`PhysicalAsset*` schemas; gateway proxy added to `WealthServiceClient`/`WealthGatewayResource`; frontend client regenerated. New `web/src/pages/Wealth/PhysicalAssets.js` page (list/filter/add/edit/deactivate, compliance-deadline fields for PUC/insurance/road-tax with expiry-status coloring), wired into routing (`/wealth/physical-assets`) and nav. 17 new `PhysicalAssetServiceTest` cases + 6 new frontend tests. No adapter-layer (real-Postgres) integration test written this pass — service-layer unit tests + manual contract validation only; consider adding one alongside the next wealth adapter test pass |
| Epic 8 Phase 4: `profile.admin.policy_settings` JSONB column | ✅ Complete (2026-07-02) | `V3__admin_policy_settings.sql` adds `policy_settings JSONB NOT NULL DEFAULT '{}'` on `profile.admin`. Keys: `monthly_budget_cap`, `debt_crossover_threshold_percent`, `freedom_runway_months`, `insurance_multiple`, `year_one_annual_target`. Resolution of Q23 — admin-scoped, rarely-changes config lives near the identity layer, not in wealth schema |
| Epic 8 Phase 4: `PATCH /v1/admins/{adminId}/policy` endpoint | ✅ Complete (2026-07-02) | Profile domain: `Admin.policySettings` field, `AdminEntity.policySettings` JSONB column (inline Jackson helpers, no cross-domain imports), `UpdateAdminPolicyRequest` DTO, `PATCH /policy` route in `AdminResource`. Gateway reads via new `ProfileServiceClient.getAdmin(adminId)` call |
| Epic 8 Phase 4: `computeFormulaGoals` gateway step | ✅ Complete (2026-07-02) | Reads Phase 3 snapshots (net worth, EMI, liquidity, growth) + `policy_settings` from admin. Evaluates 5 hardcoded formula goals: DEBT_CROSSOVER (EMI/NW < threshold%), THIRTY_SEVENTY_TARGET (equity allocation), FREEDOM_RUNWAY (liquid months of expenses), INSURANCE_FREE (net worth > N× annual income), YEAR_ONE (NW > annual target). Stores `WEALTH_FORMULA_GOALS_FAMILY` with `goals[]`, `total_count`, `achieved_count` |
| Epic 8 Phase 4: `computeValidation` gateway step | ✅ Complete (2026-07-02) | 4 advisory checks: (1) category resolution (accounts with `UNCATEGORIZED` tag), (2) missing growth rate config, (3) EMI data completeness (loans with no amortization metadata), (4) budget cap unset (monthly_budget_cap=0 in policy). Stores `WEALTH_VALIDATION_REPORT_FAMILY` with `checks[]`, `overall_status` (PASS/WARNING/FAIL), `warning_count`. Non-blocking — advisory only per Q16 resolution |
| Epic 8 Phase 4: PolicySettings admin page | ✅ Complete (2026-07-02) | `web/src/pages/Admin/PolicySettings.js` — form with 5 policy fields, loads from `getAdmin()`, saves via `updateAdminPolicy()`. Route: `/admin/policy` (admin role required). Nav: Admin → Policy Settings dropdown |
| Epic 8 Phase 4: Dashboard formula goals + validation cards | ✅ Complete (2026-07-02) | Dashboard shows Household Goals card (N/M achieved, each goal with ACHIEVED/IN_PROGRESS badge) and Data Quality banner (PASS=green, WARNING=yellow, FAIL=red) when snapshots present |
| Epic 8 Phase 4: bug fix — `total_monthly_emi` field name | ✅ Fixed (2026-07-02) | `computeFormulaGoals` and `computeValidation` incorrectly read `MONTHLY_EMI_FIELD` ("monthly_emi") from the EMI snapshot; the actual key written by `computeEmiTracking` is `"total_monthly_emi"`. Fixed to use the string literal directly |
| v0.5 Phase 0: `TransactionResource`/`TransactionService` profile_id threading fix | ✅ Complete (2026-07-02) | Closed a real ADR-006 gap found by architect review: the repo-layer filter (`TransactionPanacheRepository.findByAccountId`/`existsByDeduplicationKey`) already existed and worked (proven by adapter tests since Epic 8 Phase 1), but `TransactionService.listByAccount()` hardcoded `null` for profileId and `TransactionResource.listTransactions()` had no `profile_id` query param at all, so the filter was never actually invoked for transaction listing. Fixed: `TransactionUseCase.listByAccount()` now takes `profileId`; `TransactionResource.listTransactions()` accepts `@QueryParam("profile_id")`; stale "out of scope" comment removed. `StatementUploadService`'s dedup call site (`existsByDeduplicationKey`) was checked and found to already pass the real `profileId` resolved from `account.getProfileId()` — it was never part of the gap, no change needed there. Contract (`wealth.yaml` + web-gateway mirror + `gateway.yaml`) updated with `profile_id` param on `listTransactions`; `WealthServiceClient`/`WealthGatewayResource` updated to thread `profile_id` through following the same pattern as `getAccountBalance`/`getAmortization`; frontend `web/src/api/wealth.js` `listTransactions()` and its caller in `Transactions.js` (`TransactionsTab`, which already had `profileId` in scope for `createTransaction`) updated; `web/src/api/generated.ts` regenerated. New tests: `TransactionServiceTest.listByAccount_passesProfileIdThroughToRepo`, `listByAccount_profileFilter_excludesTransactionsOnDifferentProfilesAccount` (fake repo simulates the real subquery filter semantics); `WealthGatewayResourceTest` mock signature updated. `:application:domain:wealth:adapters:test` and `:application:web-gateway:test` both pass. |

---

## Database Schema (`wealth` schema)

| Table | Key Columns |
|---|---|
| `account` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `type VARCHAR`, `balance NUMERIC`, `is_active BOOLEAN`, `metadata JSONB NOT NULL DEFAULT '{}'` (V6 — category [reserved Phase 2], liquidity_tier, purpose_tag, joint_owners[] per ADR-016) |
| `transaction` | `id UUID PK`, `account_id UUID FK`, `profile_id UUID FK`, `amount NUMERIC ≥0`, `type VARCHAR`, `date DATE`, `description VARCHAR` |
| `statement_upload` | `id UUID PK`, `account_id UUID FK`, `profile_id UUID FK`, `file_name VARCHAR`, `status VARCHAR`, `upload_date TIMESTAMP` |
| `upload_error_log` | `id UUID PK`, `upload_id UUID FK`, `error_type VARCHAR(50)`, `missing_columns TEXT[]`, `error_detail TEXT`, `created_at TIMESTAMPTZ` |
| `physical_asset` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `type VARCHAR`, `value NUMERIC`, `acquisition_date DATE` |

Account types (VARCHAR, no SQL ENUM): `SAVINGS`, `CURRENT`, `CREDIT_CARD`, `HOME_LOAN`, `PERSONAL_LOAN`, `INVESTMENT`, `FD`
Transaction types: `CREDIT`, `DEBIT`
Upload status: `PENDING` → `SUCCESS` | `FAILED`
Error types: `MISSING_DATE_COLUMN`, `MISSING_AMOUNT_COLUMN`, `MISSING_REQUIRED_COLUMN`, `PARSE_ERROR`, `VALIDATION_ERROR`

---

## API Contract

File: `application/contract/wealth.yaml`
Base path: `/api/v1/wealth`
- `GET    /accounts?profile_id=`
- `POST   /accounts?profile_id=`
- `GET    /accounts/{id}`
- `PUT    /accounts/{id}`
- `DELETE /accounts/{id}`
- `GET    /accounts/{id}/transactions?profile_id=` — v0.5 Phase 0: `profile_id` param added (was previously missing; filter existed at repo layer but was unreachable)
- `POST   /accounts/{accountId}/uploads?profile_id=` — upload CSV (returns `StatementUploadResponse` with `inserted_count` + `skipped_duplicates`)
- `GET    /accounts/{accountId}/uploads`
- `DELETE /accounts/{accountId}/uploads/{uploadId}/rollback`
- `GET    /accounts/{accountId}/uploads/{uploadId}/errors` — NEW: returns `List<UploadErrorLogResponse>`
- `GET    /accounts/{id}/balance` — Epic 8 Phase 1, Bug 2 fix: returns `AccountBalance` (opening_balance, total_credits, total_debits, current_balance)
- `PATCH  /accounts/{id}/classification` — Epic 8 Phase 1+2: merges category/liquidity_tier/purpose_tag/joint_owners into account.metadata
- `PATCH  /accounts/{accountId}/transactions/{txnId}/category` — Epic 8 Phase 2 NEW: tags one transaction with an `ExpenseCategory`
- `PATCH  /accounts/{accountId}/transactions/category` — Epic 8 Phase 2 NEW: bulk-tag-by-selection (`transaction_ids[]` + `category`)
- `GET    /physical-assets?profile_id=` — NEW (2026-06-30): list, filterable by `asset_type`/`is_active`
- `POST   /physical-assets?profile_id=` — NEW: create; `registration_number` must be unique (409 on duplicate)
- `GET    /physical-assets/{id}` — NEW
- `PATCH  /physical-assets/{id}` — NEW: partial update; `metadata` merges into existing map (compliance deadlines)
- `DELETE /physical-assets/{id}` — NEW: soft-delete (`is_active = false`)

Note: before this Phase 2 pass, the contract had **zero documented paths/schemas for the Transaction resource at all** (not even the pre-existing `listTransactions`/`getTransaction`) — a pre-existing gap, now closed alongside the new category endpoints. Also fixed: `listAccounts`/`createAccount` were missing the `profile_id` query param in the contract even though the Java code always had it.

---

## Key Files

| Layer | Path |
|---|---|
| Domain | `application/domain/wealth/domain/src/main/java/com/suchika/wealth/domain/` |
| Ports | `application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/` |
| Adapters | `application/domain/wealth/adapters/src/main/java/com/suchika/wealth/adapters/` |
| Flyway | `application/flyway/wealth/` |
| Frontend | `web/src/pages/Wealth/` (Accounts.js, Transactions.js, Reports.js, PhysicalAssets.js) |
| API module | `web/src/api/wealth.js` |

---

## Key Design Decisions (ADRs)

- `CreateAccountCommand` has **7 fields** — `profileId` is passed separately: `createAccount(UUID profileId, CreateAccountCommand cmd)`. Never add `profileId` back into the command (SonarQube S107 — too many constructor params).
- Dedup key is **4 fields**: `(account_id, txn_date, amount, txn_type)` — description excluded to handle cases where the same transaction has slightly different narrations across files. DB unique constraint `uq_transaction_dedup` still includes description as a last-resort safety net.
- Same-file duplicate rows are disambiguated by appending ` #2`, ` #3` etc. to the description. These variants bypass the cross-file dedup check (tracked in `insertedThisBatch` set in `StatementUploadService`).
- `CsvParseException` extends `BadRequestException` and lives in `adapters/services/` — it carries a machine-readable `errorType` and `missingColumns` list so the upload service can write a structured `upload_error_log` row before re-throwing.
- `StatementUploadUseCase.uploadStatement()` returns `UploadResult` (not `StatementUpload`) — wraps the upload entity with `insertedCount` and `List<SkippedRow>`.
- No SQL ENUMs anywhere — all discriminators are `VARCHAR` enforced at the OpenAPI contract layer.
- All DB queries filtered by `profile_id` in the adapter layer only.

---

## Open Issues / Backlog

- ✅ **v0.5 Phase 0: `TransactionResource`/`TransactionService` profile_id threading fix — COMPLETE (2026-07-02).** See Implementation Status table entry above for full detail. Product owner decision `OpenQuestions.md` Q28 — fixed now rather than deferred.
- **v0.5 Phase 2 (planned): Vacation Planner cross-domain feature.** Lives under Household nav (`/household/vacation-planner` — product owner decision, `OpenQuestions.md` Q27), but reads wealth data: liquid savings from `WEALTH_LIQUIDITY_TIERS_FAMILY` snapshot for budget validation, and `physical_asset.metadata` (PUC/insurance/road-tax expiry dates) for the asset-compliance check. Physical asset dates stay JSONB — no schema promotion (`OpenQuestions.md` Q29); parse defensively (null-safe) in the gateway composing this feature.
- ✅ Frontend: display `skipped_duplicates` panel and `error log` panel in upload UI (v0.4 Phase 3)
- Duplicate resolution UI for `is_duplicate=TRUE` rows (future)
- Wire `projections.dashboard_snapshot` to live wealth data (future)
- Add transaction pagination (future)
- **Epic 8 Phase 1 — COMPLETE (2026-06-30).** Delivered: V6 migration (`wealth.account.metadata JSONB`), Bug 2 fix (net worth formula — new `GET /accounts/{id}/balance`, gateway `currentBalanceFor()` helper used by both `computeNetWorth` and `computeTotalBalance`), Bug 4 fix (`profile_id` filter added to `TransactionRepository.findByAccountId`/`existsByDeduplicationKey`, proven by two new adapter tests against real PostgreSQL — `findByAccountId_profileFilter_blocksCrossProfileAccess`, `existsByDeduplicationKey_profileFilter_blocksCrossProfileMatch`), account classification metadata write path (`PATCH /accounts/{id}/classification`), and the Phase 1 validation seed (`ProjectionCalculationEngine.computeCategoryValidation` → `WEALTH_CATEGORY_VALIDATION` snapshot key, correctly reports 100% uncategorized since category isn't populated by any real flow yet — not faked). Both `application/contract/wealth.yaml` and the web-gateway mirror copy updated with the two new paths + `AccountBalance`/`UpdateAccountClassificationRequest` schemas + `metadata` on `Account`. Frontend untouched this phase (backend-only). Phases 2-4 (statement source expansion, expense auto-categorization, loan amortization/EMI arbitrage, goals + full validation engine, Bug 1 gateway `/errors` proxy, Bug 3 `refreshAll()` isolation) remain not started — see `documents/EPIC8_IMPLEMENTATION_PLAN.md`.
- **Process note:** during Phase 1 implementation, two wealth-developer subagent runs independently declined to implement the ADR-017 household rollup because they could not independently verify, from working-tree state alone, that it was a real user-confirmed requirement rather than injected/relayed instruction — correct, cautious behavior for a subagent with no direct channel to the user. The orchestrating session (which does have the direct, verified conversation with the product owner) implemented it directly instead — see the Phase 1b entry below for what was delivered.
- **Epic 8 Phase 1b — Family Net Worth Rollup — COMPLETE (2026-06-30).** Delivered the first working `_FAMILY` snapshot per ADR-017: `ProjectionCalculationEngine.computeFamilyNetWorth(UUID profileId)` resolves the caller's `admin_id` via `ProfileServiceClient.getProfile()`, lists active household members via `listProfiles(adminId, true)`, sums each member's net worth (reusing the existing `computeTotalBalance()` per-account-balance helper — no duplicated summation logic), and UPSERTs the family total plus a per-member `members[]` breakdown under `WEALTH_NET_WORTH_FAMILY`, keyed by the admin's own `profile_id`. Wired into `refreshAll()` as a 6th step. `SnapshotKey` gained `WEALTH_NET_WORTH_FAMILY` (implemented) plus three reserved placeholder keys for later phases — `WEALTH_GOAL_PROGRESS_FAMILY`, `WEALTH_VALIDATION_REPORT_FAMILY`, `WEALTH_EMI_TRACKING_FAMILY` (not yet populated by any compute method). Tests: multi-member sum (3 profiles, distinct balances), single-member degenerate case, zero-active-members edge case, plus `refreshAll_callsAllSixComputeMethods` updated from five to six. Full `:application:web-gateway:test` suite passes. No new REST endpoint or DB migration — reuses the existing `GET /v1/projections/dashboard/{profileId}` read path and the unchanged `(profile_id, snapshot_key)` UPSERT shape. Goal/validation/EMI family aggregation explicitly out of scope here — Phase 3/4 work.
- **Epic 8 Phase 1c — Family Net Worth on the Home Dashboard — COMPLETE (2026-06-30).** `web/src/pages/User/Dashboard.js` now reads `WEALTH_NET_WORTH_FAMILY` as the primary net worth card ("Family Net Worth") with a `MemberBreakdown` list (name, relation, per-member net worth) underneath. Falls back to the legacy `WEALTH_NET_WORTH` per-profile snapshot if the family key isn't present yet (e.g. before the first manual refresh post-deploy), so nothing breaks mid-rollout. No API client regeneration needed — reuses the existing `getDashboard`/`refreshProjections` calls in `web/src/api/household.js`; the new field is just additional JSON inside the existing snapshot payload. 2 new tests added (family view rendering, fallback-to-per-profile view); full frontend suite (357 tests) and lint both pass.
- **Epic 8 Phase 2 — Statement Source Expansion & Expense Categorization — backend COMPLETE (2026-06-30); `StatementCsvParser` extension explicitly DEFERRED.** Two blockers were resolved directly by the product owner before implementation started: (1) no real bank statement files exist yet for Bank of Baroda/Bank of India/Kotak/credit card, so `StatementCsvParser`'s candidate header lists are **not** being extended speculatively — stays generic, revisit once real CSVs are available; (2) the Kotak joint account did not exist yet, so a placeholder was created live via the running services. Delivered: `ExpenseCategory` domain enum (5 values, matches `documents/EPIC8_IMPLEMENTATION_PLAN.md` Decision 1); `TransactionUseCase.updateCategory`/`bulkUpdateCategory` + matching `TransactionResource` PATCH endpoints (single + bulk-by-selection per Q24 — explicitly not a rules engine); a real latent bug fix in `TransactionEntity` where the `metadata` JSONB column (present since V1) was never wired into `from()`/`toDomain()`, so every `save()` silently reset metadata to `{}` — fixed with the same Jackson round-trip pattern as `AccountEntity`, and was a hard prerequisite for category tagging to persist at all; `joint_owners` added to `UpdateAccountClassificationRequest`/`AccountUseCase.updateAccountClassification`, stored comma-joined inside the existing flat `metadata` map (chose not to widen `Account.metadata` to `Map<String,Object>` — narrower change, field is attribution-only per ADR-016); all five new/previously-undocumented gateway proxies wired (`getAccountBalance`, `updateAccountClassification`, `updateTransactionCategory`, `bulkUpdateTransactionCategory`, `getUploadErrors` — the last one is the Bug 1 fix, was implemented in code but never gateway-proxied). `application/contract/wealth.yaml`, its web-gateway mirror, and `application/contract/gateway.yaml` all updated and re-synced (the wealth.yaml mirror had drifted out of sync pre-existing — now byte-identical to the canonical contract); `web/src/api/generated.ts` regenerated. New tests: `AccountServiceTest.updateAccountClassification_jointOwners_storedCommaJoined`, six new `TransactionServiceTest` cases for `updateCategory`/`bulkUpdateCategory` (happy path, null category, not-found, bulk happy path, empty list, unknown id). Full `:application:domain:wealth:adapters:test` and `:application:web-gateway:test` suites pass. Kotak placeholder account created live (`account_id 7e3c4712-cb82-4b90-9a73-9f7f4acf29db`, SAVINGS, Kotak Mahindra Bank, opening_balance 0) but attached to the dev DB's seed `Test Member` profile rather than a real Shweta profile, since **no real household-member profile records exist in the dev database yet** — only the generic seed Test Admin/Test Member pair. `joint_owners` intentionally left unset (no second real profile to reference) and the account tagged `purpose_tag: PENDING_JOINT_OWNER_ASSIGNMENT` so it's easy to find and correct once real profiles are created. Frontend categorization UI (a way for the user to actually tag transactions by category in the browser) was **not** built this pass — backend-only, matching the Phase 1 pattern of shipping the vertical backend slice first.

---

## Key Design Decisions (ADRs) — Epic 8 additions (2026-06-30)

- ADR-016: joint accounts keep a single `profile_id` of record; co-owners are attribution-only in `metadata.joint_owners`, never a query predicate. No many-to-many ownership table. Kotak account: Shweta is designated owner, Ketan in `joint_owners`.
- Expense category is a hardcoded 5-value enum (`HOUSEHOLD_CORE`, `CHILD_RELATED`, `MAINTENANCE`, `DISCRETIONARY`, `UNCATEGORIZED`) stored in `transaction.metadata.category` — not the v1.3 rules engine pulled forward. Manual tagging only in Epic 8; auto-tagging stays v1.3 scope.
- `StatementCsvParser` stays generic (header-candidate-list matching) for new statement sources (Bank of Baroda, Bank of India, Kotak, credit card) — extend candidate lists per bank, do not build a per-bank format registry unless a real file defeats the generic approach.
- **ADR-017 — Household-Level Dashboard Aggregation (2026-06-30):** All Epic 8 dashboard outputs (net worth, goals, EMI tracking, validation) are a **household rollup**, not per-profile figures — the product owner manages all family finances as head of household ("Family Financial Data — Combined"). `ProjectionCalculationEngine` resolves household members via the already-existing `ProfileServiceClient.listProfiles(adminId, isActive)`, loops the existing per-profile compute call once per member, and sums into a family total with each member's result nested (`members[]` array) inside the payload — matches the product owner's `assets_06062026.json` reference shape. New snapshot keys are suffixed `_FAMILY` (e.g. `WEALTH_NET_WORTH_FAMILY`) and UPSERTed under the **admin's own SELF `profile_id`** (not `admin.id` — reuses the existing identifier space, `dashboard_snapshot` PK shape `(profile_id, snapshot_key)` is unchanged). Old singular per-profile keys are not deleted but are no longer the dashboard's primary read path. No DB schema change. Does **not** violate ADR-006 — the engine does zero SQL against domain schemas; it composes N individually ADR-006-compliant per-`profile_id` REST calls and aggregates in gateway memory (see ADR-017 for the full reasoning). Only the admin (Ketan) ever authenticates; per-member "drill-down" views (e.g., "just Shweta's accounts") are a client-side filter over the one family payload, not a separate compute path or auth-gated view. `HEALTH_VITALS_SUMMARY` / `HOUSEHOLD_EVENT_SUMMARY` are unaffected — they stay per-profile (inherently per-person data).
