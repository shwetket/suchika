# Context Primer — Suchika

| | |
|---|---|
| **Type** | Reference |
| **Audience** | AI agents, new developers |
| **Status** | Active |
| **Last updated** | 2026-07-11 |

## Objective

Provide a compact project snapshot that any agent or developer can read in ~2 minutes to get full context before starting work. This is the mandatory first read — everything else in `documents/` is depth on a specific topic.

## Use Cases

- **Always** — read this before starting any task in this repo
- After a long gap away from the project — re-sync on current version and domain status
- When handing off work to another agent or developer

---

**Read this first.** Compact project snapshot for fast agent bootstrapping. ~2 min read.
For depth, follow the links to domain-state files or documents/.

---

## What Is Suchika

Personal household management system. Four domains: Profile, Wealth, Health, Household.
Hexagonal Architecture (Ports & Adapters). Single PostgreSQL database (`app_db`). React frontend.

---

## Current Version: v0.6 — Complete (2026-07-03)

| Domain | Backend | Frontend | Status |
|---|---|---|---|
| Profile | ✅ | ✅ | Complete — policy settings (Epic 8 Ph4); v0.6 adapter/domain tests added |
| Wealth | ✅ | ✅ | Complete — full Epic 8 engine + v0.5 fixes + v0.6 transaction pagination |
| Health | ✅ | ✅ | Complete — v0.5 vitals edit + v0.6 doctor visit date-range filter |
| Household | ✅ | ✅ | Complete — v0.3 + v0.5 inventory edit/is_consumed + v0.6 Goals copy note |
| web-gateway | ✅ | — | 12 CQRS projection steps live (Action Center added in v0.5) |

**Next milestone: v0.7** — not yet planned; see `ROADMAP.md` for the full future-milestone list (v1.0 Security & Persistence is next after any v0.7 gap-filling).

**Pre-v1.0 retrospective — 2026-07-06:** full-repo review (architect, business-analyst, all domain/infra/frontend agents); no version bump. See `ROADMAP.md`'s "Architect Review — 2026-07-06" and "Business Analyst Review — 2026-07-06" sections; all four `domain-state/*.md` files were refreshed the same day.

**Post-retrospective UX/data pass — 2026-07-08 through 2026-07-11 (no version bump, still v0.6):**
- v0.5.1 Workstream (2026-07-08/09): `profile_id`/`admin_id` enforcement (Tier A all domains, Tier B wealth-only pilot), Flyway V2→V1 merges (profile/wealth/household), ADR-021 (login auto-attaches to the sole active admin instead of a broken `localStorage` carry-forward).
- Real seeded household data loaded (2026-07-10) — `application/flyway/test-seed/{health,household,profile,wealth}/R__seed_*_test_data.sql`, 4 real profiles + 46 real wealth accounts + 8 real physical assets. **Flag:** each seed file's own header comment claims it is "gitignored... on the seed-data branch, never pushed/merged," but on this branch (`UX-Updates`) `.gitignore` has no `test-seed` entry and all four files are tracked/committed — real names, a real email address, real bank/account details, and real financial figures are currently in git history on this branch. Worth a deliberate decision (gitignore + untrack, or replace with synthetic data) before this branch merges — see `documents/domain-state/profile.md` Open Issues for the same flag.
- `AuthContext.login()` now filters out inactive admins before the single-vs-multiple-admin check (2026-07-11) — closes a real bug where one active admin alongside any deactivated admin was wrongly treated as a multi-household conflict.
- Shared `Badge` (`web/src/components/shared/Badge.js`) and `EditIcon` (`web/src/components/shared/EditIcon.js`) components introduced and adopted across Profiles, Accounts, Dashboard, PhysicalAssets, Reports, and several Public/Admin pages — icon-only edit actions, deactivate-moved-into-edit-modal, and reactivate-from-edit-modal patterns are now consistent across Profile and Wealth pages. New CSS design tokens/`card-hover` utility added in `web/src/index.css` + `tailwind.config.js`.
- Wealth UX pass (UX-001 through UX-018, `documents/UX_DECISIONS.md`) plus new `wealth.account.balance_as_of` field (Flyway V3) — see `documents/domain-state/wealth.md` for full detail, already current as of 2026-07-11.

Quality gates (v0.6): all Gradle tests green (512+ backend tests total), ArchUnit clean including a new port-interface test-coverage rule, Jest branch coverage gate enforced (branches 70%, lines 80%). 539+ JS tests as of the 2026-07-11 wealth UX pass, 30 gateway projection tests.

**v0.6 key additions (Testing Foundation, re-scoped, all complete):**

- New ArchUnit rule enforcing every `ports.input` use-case interface is referenced by at least one test class — immediately surfaced and closed 3 previously-zero-coverage resources (`AdminResource`, `ProfileResource`, `PhysicalAssetResource`)
- HTTP adapter unit tests: `AccountResource`, `TransactionResource`, `VitalReadingResource`, `DoctorVisitResource`, `AdminResource`, `ProfileResource`, `PhysicalAssetResource`
- Domain entity unit tests: `Profile`, `Admin`, `VitalReading`
- Jest branch coverage gate (real current level: branches 70%, functions 75%, lines 80%, statements 79% — not the aspirational 80% branch figure originally named, which measurement showed wasn't met yet)
- ADR-019: documents `profileId`-as-domain-field as a deliberate ADR-006 trade-off (7 entities across wealth/health/household)
- UX: transaction list pagination (`page`/`size` on `GET /transactions`), doctor visit date-range filter (`from`/`to` on `GET /doctor-visits`), Goals page auto-refresh copy note

**v0.5 key additions (Phases 0-3, all complete):**

- **Phase 0:** `PATCH /v1/vitals/{id}` edit + modal; `PUT /v1/inventory-items/{id}` edit + modal; `is_consumed` flag on inventory items (Q6); `profile_id` threaded through transaction list/dedup (closed a real ADR-006 gap); Reports page net balance bug fixed (was still summing raw `opening_balance`)
- **Phase 1:** React Query adopted for frontend server state (ADR-018, resolves PROP-005); `Dashboard.js` migrated as the reference pattern
- **Phase 2:** Vacation Planner (`/household/vacation-planner`) — trip budget check against liquid savings, vehicle compliance check against trip dates; new gateway package `com.suchika.gateway.vacationplanner`
- **Phase 3:** Consolidated Action Center (`/action-center`) — 12th `ProjectionCalculationEngine` step aggregating upcoming events, vehicle compliance deadlines, and biometric streak gaps (Q30: core 3 vital types, 30-day threshold, per-profile) across all household members

**v0.4 key additions:**

*Error Handling (core v0.4):*
- Malformed CSV rejection: structured error log to `wealth.upload_error_log`; `GET /uploads/{id}/errors` endpoint
- Dedup key fix: 4-field key `(account_id, txn_date, amount, txn_type)` — description excluded
- Frontend upload error panel and skipped-duplicates panel

*Physical Assets — full vertical slice (v0.4.1 patch):*
- `wealth.physical_asset` table with JSONB metadata, registration uniqueness constraint
- Full CRUD API (`/v1/physical-assets`), gateway proxy, frontend page at `/wealth/physical-assets`
- PUC / insurance / road-tax compliance-deadline fields with expiry-status colouring

*Manual Transaction Entry (Q7):*
- `POST /v1/accounts/{accountId}/transactions`; `upload_id` made nullable (`V7` migration)
- `source = MANUAL` in transaction metadata distinguishes from CSV-sourced rows
- Add Transaction modal in frontend Transactions page

*Epic 8 — Automated Wealth Intelligence Engine (Phases 1–4):*
- **Phase 1:** `account.metadata` JSONB column; `PATCH /accounts/{id}/classification` (category, liquidity tier, purpose tag, loan fields, joint owners); `GET /accounts/{id}/balance` (opening + transactions); `computeCategoryValidation` and `computeFamilyNetWorth` gateway steps; ADR-016 (joint accounts) and ADR-017 (family rollup)
- **Phase 2:** Expense category tagging — single (`PATCH /transactions/{id}/category`) and bulk by ID list; 5 `ExpenseCategory` enum values; `TransactionEntity.metadata` JSONB wired end-to-end
- **Phase 3:** `AmortizationCalculator` (pure domain, EMI formula with offset arbitrage); `computeEmiTracking`, `computeLiquidityTiers`, `computeGrowthProjection` gateway steps; loan details form in Accounts UI; `JsonbMetadataUtil` eliminates CPD across 3 entity classes
- **Phase 4:** `policy_settings JSONB` on `profile.admin` (Flyway V3); `PATCH /admins/{id}/policy`; `computeFormulaGoals` (5 formula goals: Debt Crossover, 30-70 Target, Freedom Runway, Insurance Free, Year One); `computeValidation` (4 advisory checks); Admin Policy Settings page; Dashboard goals + validation cards

---

## Service Map

| Service | Port | DB Schema | Start command |
|---|---|---|---|
| profile | 8081 | `profile` | `./gradlew :application:domain:profile:adapters:quarkusDev` |
| wealth | 8082 | `wealth` | `./gradlew :application:domain:wealth:adapters:quarkusDev` |
| health | 8083 | `health` | `./gradlew :application:domain:health:adapters:quarkusDev` |
| household | 8084 | `household` | `./gradlew :application:domain:household:adapters:quarkusDev` |
| web-gateway | 8080 | `projections` | `./gradlew :application:web-gateway:quarkusDev` |
| frontend | 3000 | — | `cd web && npm start` |

**Startup order matters:** profile → wealth/health/household → gateway → frontend.
Frontend talks only to gateway (8080). Never call domain ports directly from React.

---

## Architecture in One Paragraph

Each domain is a Gradle sub-project with three layers: `domain/` (pure Java, zero framework deps), `ports/` (interfaces), `adapters/` (Quarkus/JPA/HTTP). ArchUnit enforces this. The `web-gateway` is a BFF that aggregates domain REST calls and has no DB of its own. All DB queries filtered by `profile_id` (injected in adapters only). No SQL ENUMs — VARCHAR with OpenAPI enum validation.

---

## Key Invariants (breaks build if violated)

1. `domain/` layer: zero `@Inject`, zero JPA, zero HTTP types — ArchUnit test enforces this
2. `profile_id` filter in every DB query — in adapter, never in domain
3. No SQL ENUMs — VARCHAR for all discriminators
4. Never edit a committed Flyway migration — create a new versioned file
5. Frontend never calls domain ports — only gateway at port 8080
6. `web/src/api/generated.ts` is never hand-edited — always `npm run generate:api`

---

## Domain State Files (read before working on a domain)

- [documents/domain-state/profile.md](domain-state/profile.md) — schema, files, open issues
- [documents/domain-state/wealth.md](domain-state/wealth.md) — schema, ADRs, backlog
- [documents/domain-state/health.md](domain-state/health.md) — schema, backlog
- [documents/domain-state/household.md](domain-state/household.md) — calendar events, inventory, goals; projection engine in gateway; v0.3 complete

---

## Agent Protocol

**Before starting any task:**
1. Read this file (CONTEXT_PRIMER.md)
2. Read the relevant `documents/domain-state/<domain>.md`
3. Read the specific source files for your task

**After completing any task:**
Update `documents/domain-state/<domain>.md` — mark done items, add new open issues, update schema if DB changed, update "last updated" date.

---

## Branch & PR Governance

Six governance files in `.github/` (added v0.2, updated v0.4):

| File | Purpose |
|---|---|
| `.github/CODEOWNERS` | `* @ketan` — all files owned by ketan; comment lines removed in v0.4 |
| `.github/pull_request_template.md` | Standard PR description template auto-loaded on PR creation |
| `.github/labeler.yml` | Path-to-label mapping used by the pr-labeler workflow |
| `.github/workflows/branch-name-check.yml` | Branch must match `^[a-zA-Z][a-zA-Z0-9_-]{3,}$` — starts with letter, min 4 chars, letters/digits/hyphens/underscores only. No type-prefix required (changed in v0.4). |
| `.github/workflows/pr-title-lint.yml` | Enforces Conventional Commits format on PR titles (e.g. `feat(wealth): add CSV upload`) |
| `.github/workflows/pr-labeler.yml` | Auto-labels PRs based on changed file paths (domain, frontend, infra, docs, etc.) |

CI workflow (`.github/workflows/ci.yml`) triggers on `main` branch only — dead `master` trigger removed.

---

## Where to Find Things

| Need | Location |
|---|---|
| Architecture rules | `documents/ARCHITECTURE_GUIDELINES.md` |
| Business requirements | `documents/BUSINESS_REQUIREMENTS.md` |
| Roadmap / milestones | `documents/ROADMAP.md` |
| Logging & exceptions | `documents/LOGGING_AND_EXCEPTIONS.md` |
| Frontend guidelines | `documents/FRONTEND_GUIDELINES.md` |
| E2E tests | `documents/E2E_TESTING.md` |
| Scripts / dev commands | `documents/SCRIPTS.md` |
| CI/CD pipeline | `documents/CICD.md` |
| PR governance | `.github/CODEOWNERS`, `.github/workflows/branch-name-check.yml`, `.github/workflows/pr-title-lint.yml` |
| OpenAPI contracts | `application/contract/<domain>.yaml` |
| Gateway contract | `application/contract/gateway.yaml` |
| Flyway migrations | `application/flyway/<domain>/` |
| Canonical code pattern | Copy from profile domain (it was the first) |
| Epic 8 implementation detail (complete) | `documents/domain-state/wealth.md` |

## Agent Roster

| Agent | Use for |
|---|---|
| `devops` | Scripts, startup, ports, DB, logs, lnav, CI — anything about running the system |
| `wealth-developer` | Accounts, transactions, CSV upload (port 8082) |
| `health-developer` | Vitals, doctor visits (port 8083) |
| `profile-developer` | Admin, member profiles (port 8081) |
| `household-developer` | Calendar, inventory, goals — v0.3 (port 8084) |
| `quarkus-developer` | Cross-domain backend Java work |
| `react-developer` | Cross-domain frontend work |
| `architect` | New domain design, ADRs, cross-domain patterns |
| `quality-manager` | SonarQube, ArchUnit, test coverage gates |
| `document-writer` | Update docs, SCRIPTS.md, domain-state files |
