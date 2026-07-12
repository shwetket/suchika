# Architecture — Suchika v0.6

| | |
|---|---|
| **Type** | Reference |
| **Audience** | All developers, AI agents |
| **Status** | Active |
| **Last updated** | 2026-07-12 (refreshed from v0.4 — Epic 8/ADR-022, Flyway consolidation, 13-step projection engine, current ArchUnit rule set) |

## Objective

Single source of truth for how the system is structured at the code level. Read this alongside `ARCHITECTURE_DECISIONS.md` (the why) and `ARCHITECTURE_GUIDELINES.md` (the rules). The ADRs record decisions; this document records the current realized structure.

---

## System Overview

Suchika is a personal household management system built as five Quarkus services sharing one PostgreSQL database. Four services own business domains; one is a BFF (Backend for Frontend) aggregator. The React frontend talks only to the BFF.

```
React (3000)
    │
    ▼
Web Gateway / BFF (8080) ──── projections.dashboard_snapshot (CQRS read model)
    │         │         │
    ▼         ▼         ▼
Profile    Wealth    Health    Household
(8081)     (8082)    (8083)     (8084)
    │         │         │         │
    └─────────┴─────────┴─────────┘
                 app_db (PostgreSQL)
         profile | wealth | health | household
```

---

## Hexagonal Architecture — Per Domain

Every domain is structured identically:

```
application/domain/<domain>/
├── domain/       — Pure Java. Zero framework deps.
│   ├── <Entity>.java          — Business entity (plain POJO with builder)
│   └── <EnumType>.java        — Domain enum used in entities
├── ports/
│   ├── input/<UseCase>.java   — Interface (use case contract)
│   └── output/<Repo>.java     — Interface (persistence contract)
└── adapters/
    ├── http/
    │   ├── <Entity>Resource.java   — JAX-RS controller
    │   └── dto/                    — Request/Response DTOs
    ├── persistence/
    │   ├── <Entity>Entity.java         — JPA/Panache entity
    │   ├── <Entity>Dao.java            — PanacheRepositoryBase<E, UUID>
    │   └── <Entity>PanacheRepository.java  — implements output port
    └── services/
        └── <Entity>Service.java    — implements input port (use case)
```

Dependency arrow: `adapters → ports → domain`. Never reversed. ArchUnit enforces this at build time.

---

## Package Naming Convention

```
com.suchika.<domain>.domain.*       — entities, enums
com.suchika.<domain>.ports.input.*  — use case interfaces
com.suchika.<domain>.ports.output.* — repository interfaces
com.suchika.<domain>.adapters.*     — all framework-aware code
com.suchika.shared.*                — cross-cutting: AppLogger, exceptions
com.suchika.gateway.*               — BFF aggregation layer
```

Where `<domain>` is one of: `profile` | `wealth` | `health` | `household`

---

## Domain Inventory (v0.6 state)

### Profile (port 8081, schema: `profile`)

| Layer | Key classes |
|---|---|
| domain | `Admin`, `Profile`, `RelationToAdmin` (9 values), `Gender`, `BloodType` |
| ports.input | `AdminUseCase`, `ProfileUseCase` |
| ports.output | `AdminRepository`, `ProfileRepository` |
| adapters.http | `AdminResource`, `ProfileResource` |
| adapters.persistence | `AdminEntity/Dao/PanacheRepository`, `ProfileEntity/Dao/PanacheRepository` |
| adapters.services | `AdminService`, `ProfileService` |

Flyway: `V1__init_profile_consolidated.sql` (single file since the 2026-07-05 consolidation, ADR-020 — the pre-consolidation V1/V2/V3 history no longer exists).

Profile is the identity anchor. Every other domain holds `profile_id UUID REFERENCES profile.profile(id)`. The profile domain never imports from other domains. `admin.policy_settings JSONB` (Epic 8 Phase 4) holds household-level policy thresholds (budget cap, formula-goal targets, etc.) — see the wealth section below for what reads it.

---

### Wealth (port 8082, schema: `wealth`)

| Layer | Key classes |
|---|---|
| domain | `Account`, `Transaction`, `StatementUpload`, `UploadErrorLog`, `PhysicalAsset`, `AmortizationCalculator`, `AmortizationSummary`, `GoalPlan`, `GoalMilestone`, `GoalRule`, `GoalTriggerEvent`, `InsurancePolicy`; enums `AccountType` (11 values), `TxnType`, `UploadStatus`, `AssetType`, `RegistrationType`, `ExpenseCategory` (8 values), `GoalType`, `PolicyType`, `PremiumFrequency` |
| ports.input | `AccountUseCase`, `TransactionUseCase`, `StatementUploadUseCase`, `PhysicalAssetUseCase`, `GoalPlanUseCase`, `InsurancePolicyUseCase`, plus command/result records (`CreateAccountCommand`, `CreateGoalPlanCommand`, `CreateInsurancePolicyCommand`, `CreatePhysicalAssetCommand`, `CreateTransactionCommand`, `UploadResult`, `AccountBalance`, `PagedTransactions`, `PagedPhysicalAssets`) |
| ports.output | `AccountRepository`, `TransactionRepository`, `StatementUploadRepository`, `UploadErrorLogRepository`, `PhysicalAssetRepository`, `GoalPlanRepository`, `InsurancePolicyRepository` |
| adapters.http | `AccountResource`, `TransactionResource`, `StatementUploadResource`, `PhysicalAssetResource`, `GoalPlanResource`, `InsurancePolicyResource` |
| adapters.persistence | Entity+Dao+PanacheRepository per aggregate above (all flat, no JPA relationships — `JsonbMetadataUtil` shared helper for JSONB round-trips) |
| adapters.services | `AccountService`, `TransactionService`, `StatementUploadService`, `StatementCsvParser`, `CsvParseException`, `PhysicalAssetService`, `GoalPlanService`, `InsurancePolicyService` |

Flyway: `V1__init_wealth_consolidated.sql` (consolidated 2026-07-05), `V2__physical_asset_valuation.sql`, `V3__account_balance_as_of.sql`, `V4__goal_plan.sql` (+ 3 child tables), `V5__insurance_policy.sql`.

**Epic 8 — Automated Wealth Intelligence Engine — complete except Use Case 8.3 (deliberately not built):**
- Phase 1: `account.metadata JSONB`, corrected net-worth formula (`GET /v1/accounts/{id}/balance`), account classification, category validation
- Phase 2: expense category tagging, joint-owner attribution (ADR-016)
- Phase 3: `AmortizationCalculator` (pure domain function — EMI split, outstanding balance, offset arbitrage), liquidity tiers, growth projection
- Phase 4: 5 formula-driven goals (Debt Crossover, 30-70 Target, Freedom Runway, Insurance Free, Year One), validation engine, `policy_settings`-driven thresholds
- Use Case 8.3 (dynamic reallocation triggers, budget-cap alerts, SIP-gap checks) was **deliberately not built** — no real household data has ever exercised those thresholds (see `REQUIREMENTS_wealth_domain.md`)

**ADR-022 (all 3 phases complete)** — `wealth.goal_plan`/`insurance_policy`: richer per-goal configuration (milestones, rules, trigger events) merged with the live formula-goal figures via `computeGoalDetail()`, plus a full frontend management UI (`GoalPlans.js`, `InsurancePolicies.js`). Corrected all 5 formula-goal calculations in place (see ADR-022 for the old-vs-new table).

**Gateway proxy gap now closed:** the `/errors` endpoint (upload error log retrieval) is proxied through `WealthGatewayResource`/`WealthServiceClient` — the "frontend bypasses the gateway" gap noted in earlier versions of this document is fixed.

---

### Health (port 8083, schema: `health`)

| Layer | Key classes |
|---|---|
| domain | `VitalReading`, `DoctorVisit` (with nested `VisitDetails` value type), `VitalType` (10 values) |
| ports.input | `VitalReadingUseCase`, `DoctorVisitUseCase`, `CreateDoctorVisitCommand`, `UpdateDoctorVisitCommand`, `UpdateVitalReadingCommand`, `PagedVitalReadings`, `PagedDoctorVisits` |
| ports.output | `VitalReadingRepository`, `DoctorVisitRepository` |
| adapters.http | `VitalReadingResource`, `DoctorVisitResource` |
| adapters.persistence | VitalReading/DoctorVisit Entity+Dao+PanacheRepository |
| adapters.services | `VitalReadingService`, `DoctorVisitService` |

Flyway: `V1__init_health_consolidated.sql` (single file since 2026-07-05).

Both list endpoints (`GET /v1/vitals`, `GET /v1/doctor-visits`) support `page`/`size` pagination (0-indexed, default 50, max 200 — pre-v1.0 Q54 pass) and require `profile_id` (400 if omitted). `PATCH /v1/vitals/{id}` (v0.5) supports partial updates; `vital_type`/`profile_id` are immutable.

---

### Household (port 8084, schema: `household`)

| Layer | Key classes |
|---|---|
| domain | `CalendarEvent`, `Goal`, `InventoryItem`, `EventType`, `GoalStatus`, `ItemUnit`, `SourcePlatform` |
| ports.input | `CalendarEventUseCase`, `GoalUseCase`, `InventoryItemUseCase`, plus `PagedCalendarEvents`/`PagedGoals`/`PagedInventoryItems` |
| ports.output | `CalendarEventRepository`, `GoalRepository`, `InventoryItemRepository` |
| adapters.http | `CalendarEventResource`, `GoalResource`, `InventoryItemResource` |
| adapters.persistence | CalendarEvent/Goal/InventoryItem Entity+Dao+PanacheRepository |
| adapters.services | `CalendarEventService`, `GoalService`, `InventoryItemService` |

Flyway: `V1__init_household_consolidated.sql` (single file since 2026-07-05).

Delivered in v0.3; complete and stable through v0.6 with no household-specific gaps. All three list endpoints paginated (Q54 pass) and require `profile_id`. `household.goal` is a distinct, permanently-separate system from wealth's Epic 8 formula goals — see ADR entries in `documents/domain-state/household.md`. Task Tracking (assigning tasks to child profiles) remains unbuilt, no schema, not on any milestone.

---

## Web Gateway / BFF (port 8080, schema: `projections`)

The gateway has no domain layer. It is a pure aggregation layer: HTTP controllers proxy requests to domain services via MicroProfile Rest Client.

```
application/web-gateway/
└── src/main/java/com/suchika/gateway/
    ├── profile/          — ProfileGatewayResource + ProfileServiceClient
    ├── wealth/            — WealthGatewayResource + WealthServiceClient
    ├── health/            — HealthGatewayResource + HealthServiceClient
    ├── household/         — HouseholdGatewayResource + HouseholdServiceClient
    ├── vacationplanner/   — VacationPlannerResource + VacationPlannerService (reads wealth only)
    ├── projection/        — ProjectionCalculationEngine, ProjectionResource,
    │                        DashboardSnapshotRepository, DashboardSnapshotEntity,
    │                        DashboardResponse, DashboardSnapshotDto, SnapshotKey
    └── ClientErrorMapper.java
```

The gateway is the only service that has a DB dependency (`projections.dashboard_snapshot`) for the CQRS read model. It reads from domain services via REST and writes computed snapshots.

**Gateway test pattern (ADR-011):** `@InjectMock @RestClient` stubs domain service calls. Tests verify only gateway routing and aggregation logic. No live domain services needed.

---

## CQRS Projection Pattern

`ProjectionCalculationEngine` has grown from the original 4 compute steps to **13**, each wrapped individually in `refreshAll()`'s per-step try/catch isolation (`runStep()` helper — one step failing does not block the others):

```
POST /v1/projections/refresh/{profileId}
    └── ProjectionCalculationEngine.refreshAll(profileId)
            ├── computeNetWorth()          → WEALTH_NET_WORTH
            ├── computeGoalProgress()      → WEALTH_GOAL_PROGRESS
            ├── computeVitalsSummary()     → HEALTH_VITALS_SUMMARY
            ├── computeEventSummary()      → HOUSEHOLD_EVENT_SUMMARY
            ├── computeCategoryValidation() → WEALTH_CATEGORY_VALIDATION
            ├── computeFamilyNetWorth()    → WEALTH_NET_WORTH_FAMILY
            ├── computeEmiTracking()       → WEALTH_EMI_TRACKING_FAMILY
            ├── computeLiquidityTiers()    → WEALTH_LIQUIDITY_TIERS_FAMILY
            ├── computeGrowthProjection()  → WEALTH_GROWTH_PROJECTION_FAMILY
            ├── computeFormulaGoals()      → WEALTH_FORMULA_GOALS_FAMILY
            ├── computeGoalDetail()        → WEALTH_GOAL_DETAIL_FAMILY
            ├── computeValidation()        → WEALTH_VALIDATION_REPORT_FAMILY
            └── computeActionCenterAlerts() → ACTION_CENTER_ALERTS_FAMILY

GET /v1/projections/dashboard/{profileId}
    └── DashboardSnapshotRepository.findByProfileId()
            → reads projections.dashboard_snapshot (no recomputation)
```

Snapshot storage: UPSERT on `(profile_id, snapshot_key)` with JSONB payload. Calculation is on-demand; reads are instant. The original singular per-profile keys (`WEALTH_NET_WORTH`, etc.) are kept, not removed — the `_FAMILY` keys are additive and are what the dashboard reads by default (ADR-017, household-level rollup).

**Net worth formula (fixed since Epic 8 Phase 1):** `computeNetWorth()`/`computeFamilyNetWorth()` call `GET /v1/accounts/{id}/balance` (`opening_balance + SUM(CREDIT) - SUM(DEBIT)`), not a static `opening_balance` read.

**Known scale caveat:** several steps that loop internal list-endpoint calls (goals, calendar events, vitals, physical-asset compliance) now pass explicit page/size and so silently cap at that many rows per profile per refresh — low real-world risk today, tracked per-domain in `documents/domain-state/*.md`.

---

## Shared Module (`shared/`)

A leaf module with no domain imports. Provides:

- `AppLogger` — structured logging wrapper (all application code must use this; SLF4J direct use is banned by ArchUnit)
- Exception hierarchy: `ApplicationException` → `NotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenException`, `UnauthorizedException`, `InternalServerException`, `NotAcceptableException`, `NotImplementedException`
- `ApplicationExceptionMapper` — converts typed exceptions to JAX-RS `Response`
- `IllegalArgumentExceptionMapper` — maps domain-layer factory validation failures (`Type.create(...)` throwing `IllegalArgumentException`, per ADR-020) to 400, closing the "unmapped 500 for a business-rule violation" gap that existed pre-v0.6
- `ErrorResponse` DTO

---

## ArchUnit Rules (`shared/src/test/java/.../DomainRulesTest.java`)

9 conceptual rule groups, 15 `@Test` methods, enforced on every `./gradlew test`:

| Group | What it checks | # tests |
|---|---|---|
| 1 — Domain Purity | `domain/` must not depend on persistence, HTTP, or CDI/Quarkus packages | 2 |
| 2 — Domain Isolation | `domain/` must not import from `ports/` or `adapters/` | 1 |
| 3 — Ports Rules | `ports/` must not depend on `adapters/`; `ports.input`/`ports.output` classes must be interfaces | 3 |
| 4 — Adapters Rules | `@Entity`-annotated classes must reside in `adapters/` | 1 |
| 5 — Cross-Domain Isolation | wealth/health/household/profile must not import each other | 4 |
| 6 — Shared Isolation | `shared/` must not import from any domain module | 1 |
| 7 — Logging | No raw SLF4J/JUL outside `shared/`; use `AppLogger` | 1 |
| 8 — Gateway Coverage (v0.5) | Every `..gateway..*Resource` class needs a `*Test`/`*IT` | 1 |
| 9 — Ports.input Coverage (v0.6) | Every `ports.input` interface must be referenced by at least one test class | 1 |

Group 9 is the rule that immediately surfaced 3 previously zero-coverage resources (`AdminResource`, `ProfileResource`, `PhysicalAssetResource`) when it was added.

**Known, accepted deviation:** ADR-019 documents `profileId` as a plain field on 7 domain entities across wealth/health/household — a deliberate departure from ADR-006's stricter wording, explicitly not flagged by any ArchUnit rule (considered and rejected).

**Gap:** ArchUnit does not check that adapters implement the correct port interfaces, or that `profile_id` filtering is present in all persistence queries. These are convention-enforced, not build-enforced (tracked as a v1.0 backlog item in `ROADMAP.md`).

---

## Database Structure

Single PostgreSQL database `app_db`, five schemas:

| Schema | Owner | Tables |
|---|---|---|
| `profile` | profile service | `admin`, `profile` |
| `wealth` | wealth service | `account`, `transaction`, `statement_upload`, `upload_error_log`, `physical_asset`, `goal_plan` (+3 child tables), `insurance_policy` |
| `health` | health service | `vital_reading`, `doctor_visit` |
| `household` | household service | `calendar_event`, `inventory_item`, `goal` |
| `projections` | web-gateway | `dashboard_snapshot` |

**Constraint philosophy (revised 2026-07-05, ADR-020 — supersedes any earlier framing):**
- Keep in DB: `NOT NULL`, `PK`, `FK`, `UNIQUE`.
- **Never in DB: any `CHECK` constraint** — this covers enum discriminators *and* business-rule checks (`amount >= 0`, `end_date >= start_date`, `visited_doctor = TRUE → doctor_name NOT NULL`, etc.). All business rules move to a domain-layer validating static factory (`Type.create(...)`, throws `IllegalArgumentException`, mapped to 400 by `IllegalArgumentExceptionMapper`).
- `VARCHAR` name columns are capped at `VARCHAR(50)` project-wide (`account_name`, `institution_name`, `asset_name`, `display_name`, `full_name`).

**Timestamp rule:** All `TIMESTAMPTZ` uses `Asia/Kolkata`. Enforced at DB level (`ALTER DATABASE app_db SET timezone = 'Asia/Kolkata'`) and Hibernate level (`quarkus.hibernate-orm.jdbc.timezone=Asia/Kolkata` in every service `application.properties`).

---

## Flyway Migration Strategy

Migrations live in `application/flyway/<domain>/`. Each domain module's `application.properties` points Flyway at the correct location. `00_bootstrap.sql` is run once manually as superuser before any service starts.

| Domain | Current files |
|---|---|
| profile | `V1__init_profile_consolidated.sql` |
| wealth | `V1__init_wealth_consolidated.sql`, `V2__physical_asset_valuation.sql`, `V3__account_balance_as_of.sql`, `V4__goal_plan.sql`, `V5__insurance_policy.sql` |
| health | `V1__init_health_consolidated.sql` |
| household | `V1__init_household_consolidated.sql` |
| projections | `V1__init_projections_consolidated.sql` |

Every domain's pre-2026-07-05 migration history was collapsed into a single consolidated `V1__` file (ADR-020, product-owner-approved one-time exception to "never edit a committed migration" — required a full local DB reset). That exception has closed; the normal rule resumes for every file added since (V2 onward on wealth).

Repeatable seed migrations (`R__seed_*_test_data.sql`) run only in `dev`/`test` profiles. **Flag:** these currently contain real household data (names, email, bank details) and are tracked in git contrary to their own header comments — see `documents/domain-state/profile.md` Open Issues, not yet resolved.

**Rule:** Never edit a committed migration. Always add a new versioned file.

---

## Startup Order

```
profile (8081) → wealth (8082) → health (8083) → household (8084) → web-gateway (8080) → frontend (3000)
```

Profile must start first. Other domains' Flyway migrations reference `profile.profile`.

---

## OpenAPI Contract Flow

```
application/contract/<domain>.yaml
    → mirrored into web-gateway/src/main/resources/<domain>.yaml
    → consumed by MicroProfile @RegisterRestClient

application/contract/gateway.yaml
    → consumed by: cd web && npm run generate:api
    → produces: web/src/api/generated.ts (never hand-edit, though currently orphaned —
      every frontend page calls hand-written web/src/api/<domain>.js wrapper modules
      instead; see documents/domain-state/health.md Open Issues)
```

The gateway contract is the frontend's documented source of truth for API shape, though in practice no frontend code currently imports the generated client — a known, tracked gap, not a contradiction to "fix" casually (see `react-developer.md`'s note on this).

Shared cross-domain OpenAPI components (error responses, `profile_id` params, pagination) live in one canonical `application/contract/shared.yaml`, `$ref`'d from all 5 domain/gateway contracts — no domain contract defines its own local `Error` schema. `profile_id` ships as exactly 2 shared parameters (`ProfileIdParam` — path, required; `ProfileIdRequiredQueryParam` — query, required); pagination ships as `Page`/`Size` (offset-based, 0-indexed, default 50, max 200) applied uniformly to every list endpoint. History and rationale: `documents/CONTRACT_CONSOLIDATION.md`.

---

## ADR Summary (as of v0.6)

See `documents/ARCHITECTURE_DECISIONS.md` for full text.

| ADR | Decision | Status |
|---|---|---|
| ADR-001 | Hexagonal Architecture | Accepted |
| ADR-002 | Five Quarkus services | Accepted (revisit before v1.0 — see `OpenQuestions.md` Q55) |
| ADR-003 | No cross-domain DB joins | Accepted |
| ADR-004 | Single PostgreSQL, schema-per-domain | Accepted |
| ADR-005 | External OIDC/OAuth2 | Deferred to v1.0 |
| ADR-006 | Profile-scoped data isolation | Accepted |
| ADR-007 | Application-layer encryption for wealth data | Deferred to v1.0 |
| ADR-008 | No stored refresh tokens | Accepted |
| ADR-009 | OpenAPI contract-driven frontend | Accepted |
| ADR-010 | No SQL ENUMs | Accepted |
| ADR-011 | Gateway tests via @InjectMock @RestClient | Accepted |
| ADR-012 | Household deferred to v0.3 | Accepted (closed, delivered) |
| ADR-013 | Projection calculation engine in web-gateway | Accepted |
| ADR-014 | Typed parse exception extending ApplicationException | Accepted |
| ADR-015 | UploadResult as a ports-layer return type | Accepted |
| ADR-016 | Joint account ownership via designated profile_id + metadata attribution | Accepted |
| ADR-017 | Household-level dashboard aggregation (family rollup) | Accepted |
| ADR-018 | React Query for frontend server state | Accepted |
| ADR-019 | `profileId` as a plain field on domain entities (ADR-006 addendum) | Accepted |
| ADR-020 | Flyway consolidation & DB constraint policy — keep FK/UNIQUE/PK/NOT NULL, drop CHECK only | Accepted |
| ADR-021 | Login auto-attaches to the single existing admin (no client-side carry-forward) | Accepted |
| ADR-022 | Richer financial goal model — additive `wealth.goal_plan` tables | Accepted, all 3 phases complete |
