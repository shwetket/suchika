# Architecture — Suchika v0.4

| | |
|---|---|
| **Type** | Reference |
| **Audience** | All developers, AI agents |
| **Status** | Active |
| **Last updated** | 2026-06-29 |

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

## Domain Inventory (v0.4 state)

### Profile (port 8081, schema: `profile`)

| Layer | Key classes |
|---|---|
| domain | `Admin`, `Profile`, `RelationToAdmin`, `Gender`, `BloodType` |
| ports.input | `AdminUseCase`, `ProfileUseCase` |
| ports.output | `AdminRepository`, `ProfileRepository` |
| adapters.http | `AdminResource`, `ProfileResource` |
| adapters.persistence | `AdminEntity/Dao/PanacheRepository`, `ProfileEntity/Dao/PanacheRepository` |
| adapters.services | `AdminService`, `ProfileService` |

Flyway: `V1__init_profile.sql`, `V2__add_admin_table.sql`

Profile is the identity anchor. Every other domain holds `profile_id UUID REFERENCES profile.profile(id)`. The profile domain never imports from other domains.

---

### Wealth (port 8082, schema: `wealth`)

| Layer | Key classes |
|---|---|
| domain | `Account`, `Transaction`, `StatementUpload`, `UploadErrorLog`, `AccountType`, `TxnType`, `UploadStatus` |
| ports.input | `AccountUseCase`, `TransactionUseCase`, `StatementUploadUseCase`, `UploadResult`, `CreateAccountCommand` |
| ports.output | `AccountRepository`, `TransactionRepository`, `StatementUploadRepository`, `UploadErrorLogRepository` |
| adapters.http | `AccountResource`, `TransactionResource`, `StatementUploadResource` |
| adapters.persistence | Account/Transaction/StatementUpload/UploadErrorLog Entity+Dao+PanacheRepository |
| adapters.services | `AccountService`, `TransactionService`, `StatementUploadService`, `StatementCsvParser`, `CsvParseException` |

Flyway: V1 (ledger), V2 (physical assets), V3 (upload status + error log table), V4 (enrich account), V5 (remove enum CHECK constraints)

**v0.4 additions:**
- `UploadErrorLog` domain entity + `UploadErrorLogRepository` port
- `UploadResult` return type wrapping `StatementUpload` + `insertedCount` + `List<SkippedRow>`
- `CsvParseException` extends `ApplicationException` — typed parse errors surfaced to callers
- `GET /v1/accounts/{accountId}/uploads/{uploadId}/errors` endpoint on wealth service
- 4-field dedup key: `(account_id, txn_date, amount, txn_type)` — description excluded

**Known gap:** The gateway `WealthServiceClient` and `WealthGatewayResource` do not expose the `/errors` endpoint. The frontend calls this endpoint directly against the wealth service (bypassing the gateway), which contradicts the "frontend talks only to gateway" invariant.

---

### Health (port 8083, schema: `health`)

| Layer | Key classes |
|---|---|
| domain | `VitalReading`, `DoctorVisit`, `VitalType` |
| ports.input | `VitalReadingUseCase`, `DoctorVisitUseCase`, `CreateDoctorVisitCommand`, `UpdateDoctorVisitCommand` |
| ports.output | `VitalReadingRepository`, `DoctorVisitRepository` |
| adapters.http | `VitalReadingResource`, `DoctorVisitResource` |
| adapters.persistence | VitalReading/DoctorVisit Entity+Dao+PanacheRepository |
| adapters.services | `VitalReadingService`, `DoctorVisitService` |

Flyway: V1 (init), V2 (remove enum constraints)

---

### Household (port 8084, schema: `household`)

| Layer | Key classes |
|---|---|
| domain | `CalendarEvent`, `Goal`, `InventoryItem`, `EventType`, `GoalStatus`, `ItemUnit`, `SourcePlatform` |
| ports.input | `CalendarEventUseCase`, `GoalUseCase`, `InventoryItemUseCase` |
| ports.output | `CalendarEventRepository`, `GoalRepository`, `InventoryItemRepository` |
| adapters.http | `CalendarEventResource`, `GoalResource`, `InventoryItemResource` |
| adapters.persistence | CalendarEvent/Goal/InventoryItem Entity+Dao+PanacheRepository |
| adapters.services | `CalendarEventService`, `GoalService`, `InventoryItemService` |

Flyway: V1 (init), V2 (goals), V3 (remove enum constraints)

Delivered in v0.3. Conflict detection in `CalendarEventRepository.findConflicts()`.

---

## Web Gateway / BFF (port 8080, schema: `projections`)

The gateway has no domain layer. It is a pure aggregation layer: HTTP controllers proxy requests to domain services via MicroProfile Rest Client.

```
application/web-gateway/
└── src/main/java/com/suchika/gateway/
    ├── profile/   — ProfileGatewayResource + ProfileServiceClient
    ├── wealth/    — WealthGatewayResource + WealthServiceClient
    ├── health/    — HealthGatewayResource + HealthServiceClient
    ├── household/ — HouseholdGatewayResource + HouseholdServiceClient
    ├── projection/ — ProjectionCalculationEngine, ProjectionResource,
    │                 DashboardSnapshotRepository, DashboardSnapshotEntity,
    │                 DashboardResponse, DashboardSnapshotDto, SnapshotKey
    └── ClientErrorMapper.java
```

The gateway is the only service that has a DB dependency (`projections.dashboard_snapshot`) for the CQRS read model. It reads from domain services via REST and writes computed snapshots.

**Gateway test pattern (ADR-011):** `@InjectMock @RestClient` stubs domain service calls. Tests verify only gateway routing and aggregation logic. No live domain services needed.

---

## CQRS Projection Pattern

```
POST /v1/projections/refresh/{profileId}
    └── ProjectionCalculationEngine.refreshAll(profileId)
            ├── computeNetWorth()       → WEALTH_NET_WORTH snapshot
            ├── computeGoalProgress()   → WEALTH_GOAL_PROGRESS snapshot
            ├── computeVitalsSummary()  → HEALTH_VITALS_SUMMARY snapshot
            └── computeEventSummary()  → HOUSEHOLD_EVENT_SUMMARY snapshot

GET /v1/projections/dashboard/{profileId}
    └── DashboardSnapshotRepository.findByProfileId()
            → reads projections.dashboard_snapshot (no recomputation)
```

Snapshot storage: UPSERT on `(profile_id, snapshot_key)` with JSONB payload. Calculation is on-demand; reads are instant.

**Known issue:** `computeNetWorth()` and `computeGoalProgress()` sum `opening_balance` from the account record, not the running balance derived from transactions. This produces a static number that does not reflect transaction activity.

---

## Shared Module (`shared/`)

A leaf module with no domain imports. Provides:

- `AppLogger` — structured logging wrapper (all application code must use this; SLF4J direct use is banned by ArchUnit)
- Exception hierarchy: `ApplicationException` → `NotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenException`, `UnauthorizedException`, `InternalServerException`, `NotAcceptableException`, `NotImplementedException`
- `ApplicationExceptionMapper` — converts typed exceptions to JAX-RS `Response`
- `ErrorResponse` DTO

---

## ArchUnit Rules (`shared/src/test/java/.../DomainRulesTest.java`)

Six rule groups enforced at build time:

| Group | What it checks |
|---|---|
| 1 — Domain Purity | `domain/` must not depend on persistence, HTTP, CDI, or Quarkus packages |
| 2 — Domain Isolation | `domain/` must not import from `ports/` or `adapters/` |
| 3 — Ports Rules | `ports/input/` and `ports/output/` must be interfaces; ports must not import adapters |
| 4 — JPA Placement | `@Entity` classes must reside in `adapters/` |
| 5 — Cross-Domain Isolation | Each domain must not import from any other domain |
| 6 — Shared Isolation | `shared/` must not import from any domain module |
| 7 — Logging | Application code must not use raw SLF4J or JUL; use `AppLogger` |
| 8 — Gateway Coverage | Every `*Resource` class in gateway must have a corresponding `*ResourceTest` |

**Gap:** ArchUnit does not currently check that all adapters implement the correct port interfaces, or that `profile_id` filtering is present in all persistence queries. These are convention-enforced, not build-enforced.

---

## Database Structure

Single PostgreSQL database `app_db`, five schemas:

| Schema | Owner | Tables |
|---|---|---|
| `profile` | profile service | `admin`, `profile` |
| `wealth` | wealth service | `account`, `transaction`, `statement_upload`, `upload_error_log`, `physical_asset` |
| `health` | health service | `vital_reading`, `doctor_visit` |
| `household` | household service | `calendar_event`, `inventory_item`, `goal` |
| `projections` | web-gateway | `dashboard_snapshot` |

**Constraint philosophy:**
- Keep in DB: NOT NULL, PK, FK, UNIQUE, business-rule CHECK (`amount >= 0`, `end_date >= start_date`, `visited_doctor = TRUE → doctor_name NOT NULL`)
- Never in DB: enum discriminator CHECK constraints — use VARCHAR + OpenAPI contract validation

**Timestamp rule:** All `TIMESTAMPTZ` uses `Asia/Kolkata`. Enforced at DB level (`ALTER DATABASE app_db SET timezone = 'Asia/Kolkata'`) and Hibernate level (`quarkus.hibernate-orm.jdbc.timezone=Asia/Kolkata` in every service `application.properties`).

---

## Flyway Migration Strategy

Migrations live in `application/flyway/<domain>/`. Each domain module's `application.properties` points Flyway at the correct location. `00_bootstrap.sql` is run once manually as superuser before any service starts.

| Domain | Current version |
|---|---|
| profile | V2 |
| wealth | V5 |
| health | V2 |
| household | V3 |
| projections | V1 |

Repeatable seed migrations (`R__seed_*_test_data.sql`) run only in `dev` and `test` profiles. They seed a fixed Admin UUID and Profile UUID for adapter integration tests.

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
    → produces: web/src/api/generated.ts (never hand-edit)
```

The gateway contract is the frontend's sole source of truth for API shape.

---

## ADR Summary (as of v0.4)

See `documents/ARCHITECTURE_DECISIONS.md` for full text.

| ADR | Decision | Status |
|---|---|---|
| ADR-001 | Hexagonal Architecture | Accepted |
| ADR-002 | Five Quarkus services | Accepted |
| ADR-003 | No cross-domain DB joins | Accepted |
| ADR-004 | Single PostgreSQL, schema-per-domain | Accepted |
| ADR-005 | External OIDC/OAuth2 | Deferred to v1.0 |
| ADR-006 | Profile-scoped data isolation | Accepted |
| ADR-007 | Application-layer encryption for wealth data | Deferred to v1.0 |
| ADR-008 | No stored refresh tokens | Accepted |
| ADR-009 | OpenAPI contract-driven frontend | Accepted |
| ADR-010 | No SQL ENUMs | Accepted |
| ADR-011 | Gateway tests via @InjectMock @RestClient | Accepted |
| ADR-012 | Household deferred to v0.3 | Accepted (closed) |
| ADR-013 | Projection calculation engine in web-gateway | Accepted |
