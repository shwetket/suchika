# Household Domain State

## Objective

Document the design and implementation status for the household domain (v0.3).

## Use Cases

- Before starting household work — read this file first; it defines the schema and key constraints
- When creating or extending `application/contract/household.yaml` — the API Contract section outlines the surface
- After completing household milestones — update Implementation Status to reflect what is done

---

**Last updated:** 2026-07-03 (v0.6 — Goals page copy note)
**Version:** v0.3 — complete (backend + gateway + frontend); v0.5 Phase 0/2 items in progress, see Open Issues / Backlog
**Port:** 8084

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| OpenAPI contract | ✅ | `application/contract/household.yaml` — complete |
| Flyway migrations V1–V3 | ✅ | calendar_event, inventory_item, goal; enum CHECK constraints removed in V3 |
| Domain entities + enums | ✅ | CalendarEvent, InventoryItem, Goal + 4 enums |
| Ports (input + output) | ✅ | 3 use cases + 3 repository interfaces |
| JPA entities + DAOs | ✅ | CalendarEventEntity, InventoryItemEntity, GoalEntity + Panache DAOs |
| Panache repositories | ✅ | CalendarEventPanacheRepository, InventoryItemPanacheRepository, GoalPanacheRepository |
| Services (use case impls) | ✅ | CalendarEventService, InventoryItemService, GoalService |
| HTTP resources | ✅ | CalendarEventResource, InventoryItemResource, GoalResource |
| DTOs | ✅ | Full set of request/response DTOs including CalendarEventResponse with conflicting_events |
| HouseholdApplication | ✅ | Entry point wired |
| Domain unit tests | ✅ | CalendarEventTest, InventoryItemTest, GoalTest — all passing |
| Adapter service tests | ✅ | CalendarEventServiceTest, InventoryItemServiceTest, GoalServiceTest — all passing |
| G1: household.yaml mirrored to gateway | ✅ | `application/web-gateway/src/main/resources/household.yaml` |
| G1: HouseholdServiceClient | ✅ | `com.suchika.gateway.household.HouseholdServiceClient` — all 14 endpoints |
| G2: HouseholdGatewayResource | ✅ | `com.suchika.gateway.household.HouseholdGatewayResource` — `/v1/household/...` proxy |
| G2: gateway.yaml updated | ✅ | Household paths + projection endpoints added to `application/contract/gateway.yaml` |
| G3: ProjectionCalculationEngine | ✅ | `com.suchika.gateway.projection.ProjectionCalculationEngine` — 4 snapshot keys (per-profile: `HOUSEHOLD_EVENT_SUMMARY` stays per-profile under ADR-017 — events are per-person, never rolled up) |
| G3: DashboardSnapshotRepository | ✅ | `com.suchika.gateway.projection.DashboardSnapshotRepository` — UPSERT + read |
| G3: DashboardSnapshotEntity | ✅ | `com.suchika.gateway.adapters.projection.DashboardSnapshotEntity` — composite PK |
| G3: ProjectionResource | ✅ | `com.suchika.gateway.projection.ProjectionResource` — POST refresh + GET dashboard |
| G4: HouseholdGatewayResourceTest | ✅ | 5 tests — list/get/create calendar events, list goals, list inventory |
| G4: ProjectionResourceTest | ✅ | 3 tests — refresh 200, dashboard 200, dashboard empty |
| build.gradle.kts + app.properties | ✅ | Added hibernate-orm, jdbc-postgresql, flyway; test profile disables DB |
| Frontend — Calendar | ✅ | Full CRUD page with conflict warning banner, type filter, profile selector |
| Frontend — Inventory | ✅ | Full CRUD page with platform filter, add/delete |
| Frontend — Goals | ✅ | Full CRUD page with progress bar, status badge, edit modal |
| Frontend — Dashboard update | ✅ | Household card added; Refresh Live Data section with spinner + snapshot metrics |
| Frontend — household API module | ✅ | `web/src/api/household.js` — calendar, inventory, goals, projections |
| Frontend — API client regenerated | ✅ | `generate:api` now points at `gateway.yaml`; household + projection types generated |
| Frontend — Routes | ✅ | `/household/goals` route added to `App.js` |
| Frontend — Tests | ✅ | Calendar.test.js (7), Inventory.test.js (5), Goals.test.js (7), Dashboard.test.js (8) — 285 total passing |

---

## Database Schema (`household` schema)

### `household.calendar_event`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| profile_id | UUID | FK → profile.profile(id) ON DELETE RESTRICT |
| title | VARCHAR(200) | NOT NULL |
| event_type | VARCHAR(50) | NOT NULL (validated at API layer — no DB CHECK after V3) |
| start_date | DATE | NOT NULL |
| end_date | DATE | nullable; CHECK end_date >= start_date |
| location | VARCHAR(200) | nullable |
| notes | TEXT | nullable |
| metadata | JSONB | NOT NULL DEFAULT '{}' |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### `household.inventory_item`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| profile_id | UUID | FK → profile.profile(id) ON DELETE RESTRICT |
| item_name | VARCHAR(200) | NOT NULL |
| quantity | NUMERIC(10,3) | NOT NULL; CHECK quantity > 0 |
| unit | VARCHAR(20) | NOT NULL (validated at API layer) |
| source_platform | VARCHAR(50) | NOT NULL (validated at API layer) |
| purchase_date | DATE | NOT NULL |
| category | VARCHAR(50) | nullable |
| metadata | JSONB | NOT NULL DEFAULT '{}' |
| is_consumed | BOOLEAN | NOT NULL DEFAULT false (V4, v0.5 Phase 0) — means "used in a calculation," not "physically used up"; rows are never deleted or expired |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### `household.goal`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| profile_id | UUID | FK → profile.profile(id) ON DELETE RESTRICT |
| goal_name | VARCHAR(200) | NOT NULL |
| target_amount | NUMERIC(19,2) | NOT NULL; CHECK target_amount > 0 |
| current_amount | NUMERIC(19,2) | NOT NULL DEFAULT 0.00; CHECK current_amount >= 0 |
| monthly_saving | NUMERIC(19,2) | nullable |
| target_date | DATE | nullable |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' (validated at API layer) |
| notes | TEXT | nullable |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

---

## API Contract

File: `application/contract/household.yaml`
Base URL: `http://localhost:8084`

| Method | Path | Description |
|---|---|---|
| GET | /v1/calendar-events | List events for a profile (filter by event_type, date range) |
| POST | /v1/calendar-events | Create event; returns conflicting_events list |
| GET | /v1/calendar-events/{id} | Get single event |
| PATCH | /v1/calendar-events/{id} | Partial update |
| DELETE | /v1/calendar-events/{id} | Delete |
| GET | /v1/inventory-items | List items (filter by source_platform, category) |
| POST | /v1/inventory-items | Add item |
| GET | /v1/inventory-items/{id} | Get single item |
| PUT | /v1/inventory-items/{id} | Full update, including `is_consumed` toggle (v0.5 Phase 0) |
| DELETE | /v1/inventory-items/{id} | Delete |
| GET | /v1/goals | List goals (filter by status) |
| POST | /v1/goals | Create goal |
| GET | /v1/goals/{id} | Get single goal |
| PATCH | /v1/goals/{id} | Partial update |
| DELETE | /v1/goals/{id} | Delete |
| PUT | /v1/goals/{id}/current-amount | Update current_amount (called by gateway projection engine) |

---

## Key Files

| Layer | Path |
|---|---|
| Domain entities | `application/domain/household/domain/src/main/java/com/suchika/household/domain/` |
| Input ports (use cases) | `application/domain/household/ports/src/main/java/com/suchika/household/ports/input/` |
| Output ports (repos) | `application/domain/household/ports/src/main/java/com/suchika/household/ports/output/` |
| JPA entities + DAOs | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/persistence/` |
| Services | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/service/` |
| HTTP resources | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/http/` |
| DTOs | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/http/dto/` |
| Domain unit tests | `application/domain/household/domain/src/test/java/com/suchika/household/domain/` |
| Adapter service tests | `application/domain/household/adapters/src/test/java/com/suchika/household/adapters/service/` |
| Flyway migrations | `application/flyway/household/` |
| OpenAPI contract | `application/contract/household.yaml` |
| Frontend pages | `web/src/pages/Household/` (Calendar.js, Inventory.js, Goals.js — fully implemented) |

---

## Key Design Decisions

- Startup order: profile must run first (Flyway migrations reference `profile.profile`).
- Event types, source platforms, units, goal status are VARCHAR — no DB CHECK after V3 migration. Enforced at OpenAPI contract + Java enum layer only.
- `end_date >= start_date` kept as DB CHECK constraint (structural invariant, not a discriminator).
- `quantity > 0`, `target_amount > 0`, `current_amount >= 0` kept as DB CHECK constraints (data integrity).
- Conflict detection is warning-only — creation is not blocked. `CalendarEventResponse` includes `conflicting_events` list.
- `progressPercent()` and `daysToCompletion()` are computed by the domain `Goal` entity and included in `GoalDto`.
- `current_amount` on goals is updated by the web-gateway projection engine via `PUT /v1/goals/{id}/current-amount`.
- `profile_id` filter injected only in adapter layer (Panache repositories) — never in domain or ports.
- All logging via `AppLogger`. All exceptions via `shared/exception/` hierarchy.

---

## Open Issues / Remaining Work

- Dashboard Refresh section uses `user.profile_id` from auth context; if auth token does not include `profile_id`, the refresh button stays disabled. Link profile_id into the auth response in a future iteration.
- Task Tracking deferred to v0.4 — no `task` table exists yet. Will need `V4__tasks.sql` when scoped.
- Inventory CSV import deferred to v0.4 — v0.3 is manual CRUD only.
- ✅ **v0.5 Phase 0: `PUT /v1/inventory-items/{id}` endpoint + edit modal — COMPLETE (2026-07-02).** `InventoryItemResource.java` gained a `@PUT @Path("/{id}")` handler; `InventoryItemUseCase.update()` does a partial merge against the existing record. Edit modal added to `web/src/pages/Household/Inventory.js`.
- ✅ **v0.5 Phase 0: `is_consumed BOOLEAN` flag on inventory items — COMPLETE (2026-07-02).** Q6 resolution — means "used in a calculation," not "used up"; no deletion, no expiry. `V4__inventory_item_consumed_flag.sql` added `is_consumed BOOLEAN NOT NULL DEFAULT false`; toggled via the same PUT endpoint above rather than a separate route.
- ✅ **v0.5 Phase 2: Vacation Planner feature lives here in nav — COMPLETE (2026-07-02).** Route `/household/vacation-planner` (product owner decision, `OpenQuestions.md` Q27) — added via a new "Household" `NavDropdown` in `Navigation.js`, which also fixed a pre-existing gap (Calendar/Inventory/Goals had no nav links at all before this). The feature itself is implemented entirely in the gateway's new `com.suchika.gateway.vacationplanner` package and reads only wealth data (`WEALTH_LIQUIDITY_TIERS_FAMILY` snapshot + `physical_asset.metadata`) — it does **not** call `HouseholdServiceClient` or look at calendar events; trip dates are supplied directly by the user in the form, not derived from `household.calendar_event`. See `documents/domain-state/wealth.md` for the full implementation detail — this entry exists here only to explain the nav placement.
- ✅ **v0.5 Phase 3: Consolidated Action Center upcoming events — COMPLETE (2026-07-02).** No household-domain code changed — gateway-only read of the existing `GET /v1/calendar-events` endpoint. `ProjectionCalculationEngine.computeActionCenterAlerts()` calls `householdServiceClient.listCalendarEvents(memberProfileId, null, today, today+30days)` per household member (same 30-day lookahead as the existing per-profile `computeEventSummary` step, but looped across all members instead of just the caller), tagging each event with the member's `profile_id`/`full_name` in the `ACTION_CENTER_ALERTS_FAMILY.upcoming_events` payload. See `documents/domain-state/wealth.md` for the full cross-domain implementation detail.
- ✅ **v0.6: Goal progress auto-refresh copy note — COMPLETE (2026-07-03).** One-line note added under the Goals page header ("Progress updates when you refresh the dashboard — new transactions aren't reflected here until then.") — no code/API change, purely explains the existing manual-refresh dependency that was previously silent.

## Completed in v0.3 Gateway Pass (G1–G4)

- G1: `household.yaml` mirrored to gateway resources; `HouseholdServiceClient` created.
- G2: `HouseholdGatewayResource` proxies all 14 household endpoints under `/v1/household/...`; `gateway.yaml` updated with household + projection paths and schemas.
- G3: Full `ProjectionCalculationEngine` (4 compute methods), `DashboardSnapshotRepository` (UPSERT + read), `DashboardSnapshotEntity` (composite PK in `..adapters..` package for ArchUnit), `ProjectionResource` (`POST /refresh/{profileId}` + `GET /dashboard/{profileId}`). `build.gradle.kts` extended with `quarkus-hibernate-orm`, `quarkus-jdbc-postgresql`, `quarkus-flyway`; `application.properties` updated with datasource config and test-profile DB disable.
- G4: `HouseholdGatewayResourceTest` (5 tests) and `ProjectionResourceTest` (3 tests) both passing. All 17 web-gateway tests green. ArchUnit still green.
