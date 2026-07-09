# Household Domain State

## Objective

Document the actual, current implementation status for the household domain — schema, contract, key files, and open issues. This file is a living reference, not a session log: historical narrative belongs in git history / ROADMAP.md, not here.

## Use Cases

- Before starting household work — read this file first for the real schema and constraints
- When extending `application/contract/household.yaml` — the API Contract section outlines the surface
- After completing household work — update Implementation Status and Open Issues to reflect what changed

---

**Last updated:** 2026-07-08 (v0.5.1 Workstream 2 Tier A — `profile_id` enforcement on list endpoints)
**Version:** Backend + gateway + frontend complete since v0.3; carried through v0.4 (no household-specific work)/v0.5 (Phases 0, 2, 3)/v0.6 (one copy-note change)/pre-v1.0 Q54 pagination pass (2026-07-07)/v0.5.1 Workstream 2 Tier A (2026-07-08). No other open feature work. Next planned milestone touching this domain is v1.0 (auth/persistence, cross-cutting — see `documents/ROADMAP.md`).
**Port:** 8084

---

## Retrospective Findings (2026-07-06)

Requested by the project owner ahead of v1.0 planning: no new features, just verify actual state and simplify. Full findings reported to the user; summarized here for future readers of this file.

1. **`.claude/agents/household-developer.md` is badly stale and should be rewritten by its owner.** It states household is "NOT STARTED... nothing exists yet... stub frontend pages... no API contract file." None of that is true — every layer described below has existed since v0.3 (2026-07-02/03) and is still in active use. Flagged to the user; not edited here (out of scope for this file).
2. **This file (before today) was mostly accurate but had drifted in three concrete places**, corrected below:
   - The schema table claimed `unit`, `source_platform`, and `goal.status` are `NOT NULL` — the actual `V1__init_household_consolidated.sql` has all three as bare nullable columns (no `NOT NULL`, no `DEFAULT 'ACTIVE'` on `status`). This looks like an unintended casualty of the V1 consolidation rewrite: the project's own two-category constraint philosophy (see `CLAUDE.md`) says `NOT NULL` should be *kept*, only `CHECK` was supposed to be dropped. Not fixed here (no new dev this pass) — logged as an open issue below.
   - The doc described adapter-layer DB tests as blocked on a `flyway_schema_history` mismatch against "the shared dev Postgres," without flagging that this whole test strategy contradicts both `ARCHITECTURE_GUIDELINES.md` ("Adapter tests use the real DB — Testcontainers with real PostgreSQL") and `documents/OpenQuestions.md` Q34/Q35, which the product owner resolved 2026-07-04 as "move to Testcontainers... to be safe anywhere." A repo-wide search found **zero** Testcontainers usage anywhere in `application/`. Household's `GoalPanacheRepositoryTest`, `CalendarEventPanacheRepositoryTest`, `InventoryItemPanacheRepositoryTest`, and `InventoryItemCreateUpdateIT` all still use a `%integration-test` Quarkus config profile pointed at `jdbc:postgresql://localhost:5432/app_db` — the same shared local instance dev services use. The Q34/Q35 resolution was never implemented, in household or (as far as this pass checked) elsewhere.
   - The "G3: ProjectionCalculationEngine — 4 snapshot keys" line was vague and reads as stale. Actual code has exactly **two** compute steps that touch household data: `computeGoalProgress` (snapshot key is literally `WEALTH_GOAL_PROGRESS`, not household-prefixed — it's named for the financial-goal concept, not the schema it reads from) and `computeEventSummary` (`HOUSEHOLD_EVENT_SUMMARY`). Corrected below.
3. **Dead code found:** `SnapshotKey.WEALTH_GOAL_PROGRESS_FAMILY` (`application/web-gateway/.../projection/SnapshotKey.java`) is defined but never computed or read anywhere — a speculative "family rollup" constant for goal progress that was never built. Safe to delete (gateway file, not edited here — out of scope for this pass).
4. **Vacation Planner / gateway boundary — confirmed correctly scoped, not blurred.** `com.suchika.gateway.vacationplanner` (`VacationPlannerService`/`Resource`) depends only on `WealthServiceClient` — verified by reading its imports/constructor. It never calls `HouseholdServiceClient` and has zero household domain/adapter code involved. This is the architecturally correct place for a genuine cross-domain BFF composition (no cross-domain DB joins, no household code pulled into the gateway beyond what's already there for goals/events). The only actual "household" tie is a nav/UX placement decision (`/household/vacation-planner`, product-owner-approved, Q27) — not a backend boundary issue. One real gap worth a future look: trip dates are manually re-entered by the user even though `household.calendar_event` already has `EventType.TRAVEL` — no integration between the two, so a user planning a trip populates the same date range twice in two different places. Not fixed here (no new dev this pass).
5. **Household's own backend footprint is lean and not over-built.** 3 domain entities, 3 use cases, 3 output ports, 3 JAX-RS resources, validation correctly placed in domain-layer `create()` factories, no speculative generic abstractions, no unused config toggles beyond item 3 above. One minor internal inconsistency: `InventoryItemUseCase.update()` takes a `UpdateInventoryItemCommand` record, while `CalendarEventUseCase.update()` and `GoalUseCase.update()` take 6-7 positional parameters each. Not a bug — worth normalizing to the Command-record style if either is touched again, not urgent enough to warrant a change on its own.
6. `web/src/pages/Household/` also contains `Profiles.js` — this is profile-domain data (`profile.profile`/`profile.admin` via `admins`/`profiles` API modules), not household calendar/inventory/goal data. It sits there because the frontend nav groups a standalone "Profiles" link next to the "Household" dropdown (Calendar/Inventory/Goals/Vacation Planner) — a UX grouping choice, not misplaced code. Noted for clarity, not a defect.

---

## Q54 Pagination Pass (2026-07-07)

Roadmap item Q54: extend real pagination (previously only on wealth's transaction list) to every list endpoint whose data can grow large over years of use. All three household list endpoints — `GET /v1/calendar-events`, `GET /v1/inventory-items`, `GET /v1/goals` — now support `page`/`size` query params (shared OpenAPI params in `shared.yaml`, 0-indexed page, default size 50, max 200), mirroring the wealth transaction-list pattern exactly:

- New per-endpoint paged-result records in `ports/input`: `PagedCalendarEvents`, `PagedInventoryItems`, `PagedGoals` (each `(items, totalCount)`), plus a new `listPaginated(...)` method on each use-case interface — the old unpaginated `list(...)` method is kept on all three for any caller that wants the full list.
- Output ports gained a paged `findByProfileId(..., page, size)` overload and a `countByProfileId(...)` method on all three repositories, reusing each Panache repo's existing filter-building helper (added one to `GoalPanacheRepository`, which didn't have one before, mirroring `InventoryItemPanacheRepository`/`CalendarEventPanacheRepository`'s pre-existing `buildQuery`/`buildParams` style).
- HTTP resources now always call the paginated use-case method for their `GET` list endpoint; `page`/`size` `@QueryParam`s default to 0/50 and validate (400 on negative page, size outside 1-200).
- Response DTOs (`ListCalendarEventsResponse`, `ListInventoryItemsResponse`, `ListGoalsResponse`) gained a second constructor with `page`/`size` fields; the original items-only constructor is kept.
- Gateway (`HouseholdServiceClient`, `HouseholdGatewayResource`) passes `page`/`size` straight through as an extra pair of query params on all three methods.
- Frontend: `web/src/api/household.js`'s three list functions gained trailing `page`/`size` params (optional-append, same pattern as `wealth.js`); `Calendar.js`, `Inventory.js`, `Goals.js` each gained `PAGE_SIZE = 20`, `page`/`totalSize` state, a filter-reset-to-page-0 `useEffect` (separate from the load effect, matching `Transactions.js`), and a Previous/Next control block.

**Consequential fix, not itself part of the Q54 scope:** widening `HouseholdServiceClient.listGoals`/`.listCalendarEvents` broke every existing call site with the old arity. `application/web-gateway/src/main/java/com/suchika/gateway/projection/ProjectionCalculationEngine.java` calls both methods directly (`computeGoalProgress`, `computeEventSummary`, `addUpcomingEventsForMember`) — these 3 call sites now pass explicit `null` for `page`/`size`, which resolves to the resource's default (page 0, size 50). This was a **required** compile fix, not a design choice; see Open Issues below for the resulting behavioral risk it introduces.

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| OpenAPI contract | Done | `application/contract/household.yaml` — 3 resource groups, full CRUD + conflict detection + goal current-amount endpoint |
| Flyway migration | Done | `V1__init_household_consolidated.sql` — `calendar_event`, `inventory_item`, `goal`. No CHECK constraints anywhere (by design). FKs to `profile.profile(id)` present. See Retrospective item 2 for a nullability gap found in this pass. |
| Domain entities + enums | Done | `CalendarEvent`, `InventoryItem`, `Goal` + `EventType`, `ItemUnit`, `SourcePlatform`, `GoalStatus` |
| Ports (input + output) | Done | 3 use cases + 3 repository interfaces, each now with a paginated method/overload (Q54) alongside the original unpaginated ones; new `PagedCalendarEvents`/`PagedInventoryItems`/`PagedGoals` records |
| JPA entities + DAOs + Panache repos | Done | One of each per aggregate; each Panache repo now has a paged-find + count method sharing a filter-builder helper |
| Services (use case impls) | Done | `CalendarEventService`, `InventoryItemService`, `GoalService` — each gained a `listPaginated(...)` method |
| HTTP resources | Done | `CalendarEventResource`, `InventoryItemResource`, `GoalResource` — GET list endpoints now take `page`/`size` `@QueryParam`s (default 0/50, validated 1-200) and call the paginated use-case method |
| DTOs | Done | Full request/response set, including `CalendarEventResponse.conflicting_events`; the 3 `ListXxxResponse` classes gained a second constructor carrying `page`/`size` |
| Domain unit tests | Done | `CalendarEventTest`, `InventoryItemTest`, `GoalTest` — plain JUnit 5, no framework |
| Adapter service tests | Done | `CalendarEventServiceTest`, `InventoryItemServiceTest`, `GoalServiceTest` — mocked repositories; each gained `listPaginated` coverage |
| Adapter DB tests | Done, but see caveat | `CalendarEventPanacheRepositoryTest`, `GoalPanacheRepositoryTest`, `InventoryItemPanacheRepositoryTest`, `InventoryItemCreateUpdateIT` — all run against the shared local Postgres via an `%integration-test` config profile, not Testcontainers (Retrospective item 2). Each paged-find/count test uses a freshly-inserted test-scoped profile (native SQL, rolled back per test) rather than the shared seeded profile, since `R__seed_household_test_data.sql` seeds exactly one row per table for that profile — sharing it would make exact-count pagination assertions flaky. |
| Resource tests | Done | `CalendarEventResourceTest`, `InventoryItemResourceTest`, `GoalResourceTest` (`@QuarkusTest` + `@InjectMock` + RestAssured) — updated to stub `listPaginated` instead of `list`; added explicit page/size and validation-error (negative page, size 0, size > 200) cases |
| Gateway: `household.yaml` mirrored | Done | `application/web-gateway/src/main/resources/household.yaml` |
| Gateway: `HouseholdServiceClient` | Done | `com.suchika.gateway.household.HouseholdServiceClient` — full REST client; 3 list methods now also take `page`/`size` |
| Gateway: `HouseholdGatewayResource` | Done | Proxies household endpoints under `/v1/household/...`; passes `page`/`size` straight through |
| Gateway: projection engine integration | Done, see Open Issues | 2 of `ProjectionCalculationEngine`'s 12 compute steps touch household data (see Retrospective item 2); household calendar events also read (not written) by `computeActionCenterAlerts`. All 3 internal call sites (`computeGoalProgress`, `computeEventSummary`, `addUpcomingEventsForMember`) now pass explicit `null` page/size, landing on the endpoint's default page 0/size 50 — see the new Open Issues entry on the resulting truncation risk. |
| Frontend — Calendar | Done | `web/src/pages/Household/Calendar.js` — CRUD, conflict warning banner, type filter, profile selector, pagination (Q54) |
| Frontend — Inventory | Done | `Inventory.js` — CRUD, platform filter, edit modal, `is_consumed` toggle, pagination (Q54) |
| Frontend — Goals | Done | `Goals.js` — CRUD, progress bar, status badge, edit modal, pagination (Q54) |
| Frontend — Vacation Planner | Done | `VacationPlanner.js` — gateway-native feature, wealth-only data (see Retrospective item 4) |
| Frontend — tests | Done | `Calendar.test.js`, `Inventory.test.js`, `Goals.test.js`, `VacationPlanner.test.js` under `web/src/pages/Household/` — cover render/loading/error states plus pagination mechanics (page-0-on-load, Previous/Next, last-page disabling). 123 tests passing across the 5 suites in this directory as of this pass (`npx react-scripts test src/pages/Household --watchAll=false` — plain `npx jest` does not work in this CRA project, it skips the Babel/JSX transform `react-scripts` wires up). |

---

## Database Schema (`household` schema)

Source: `application/flyway/household/V1__init_household_consolidated.sql`, read directly for this update (not inferred from prior doc text).

### `household.calendar_event`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| profile_id | UUID | nullable column; FK → profile.profile(id) ON DELETE RESTRICT |
| title | VARCHAR(200) | NOT NULL |
| event_type | VARCHAR(50) | NOT NULL (validated at API layer only — no DB CHECK) |
| start_date | DATE | NOT NULL |
| end_date | DATE | nullable; `end_date >= start_date` enforced only in `CalendarEvent.create()` (domain layer) — no DB CHECK |
| location | VARCHAR(200) | nullable |
| notes | TEXT | nullable |
| metadata | JSONB | NOT NULL DEFAULT '{}' |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### `household.inventory_item`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| profile_id | UUID | nullable column; FK → profile.profile(id) ON DELETE RESTRICT |
| item_name | VARCHAR(200) | NOT NULL |
| quantity | NUMERIC(10,3) | NOT NULL; `quantity > 0` enforced only in `InventoryItem.create()` (domain layer) — no DB CHECK |
| unit | VARCHAR(20) | **nullable in the actual DDL** — validated at API/Java-enum layer when going through normal request flow, but the column itself has no `NOT NULL`. Drift vs. this doc's prior claim; see Retrospective item 2 and Open Issues. |
| source_platform | VARCHAR(50) | **nullable in the actual DDL**, same caveat as `unit` |
| purchase_date | DATE | NOT NULL |
| category | VARCHAR(50) | nullable |
| metadata | JSONB | NOT NULL DEFAULT '{}' |
| is_consumed | BOOLEAN | NOT NULL DEFAULT false — means "used in a calculation," not "physically used up"; rows are never deleted or expired |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### `household.goal`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() |
| profile_id | UUID | nullable column; FK → profile.profile(id) ON DELETE RESTRICT |
| goal_name | VARCHAR(200) | NOT NULL |
| target_amount | NUMERIC(19,2) | NOT NULL; `target_amount > 0` enforced in `Goal.create()` (domain layer) — no DB CHECK |
| current_amount | NUMERIC(19,2) | NOT NULL DEFAULT 0.00; `current_amount >= 0` enforced in `GoalService.updateCurrentAmount()` via an explicit guard (`amount == null \|\| amount.compareTo(BigDecimal.ZERO) < 0` → `BadRequestException`), since that method builds the updated `Goal` via `Goal.builder()` directly and bypasses `Goal.create()`'s validation. Verified present in code as of this update. |
| monthly_saving | NUMERIC(19,2) | nullable |
| target_date | DATE | nullable |
| status | VARCHAR(20) | **nullable in the actual DDL, no default.** `Goal.create()` always sets `GoalStatus.ACTIVE` in the domain layer for rows created through the normal API path, so this is unlikely to surface a bug in practice — but the column itself does not enforce it. Drift vs. this doc's prior claim; see Open Issues. |
| notes | TEXT | nullable |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

---

## API Contract

File: `application/contract/household.yaml` · Base URL: `http://localhost:8084`

| Method | Path | Description |
|---|---|---|
| GET | /v1/calendar-events | List events for a profile (filter by event_type, date range; paginated — `page`/`size`, Q54) |
| POST | /v1/calendar-events | Create event; returns conflicting_events list (warning only, not blocking) |
| GET | /v1/calendar-events/{id} | Get single event |
| PATCH | /v1/calendar-events/{id} | Partial update |
| DELETE | /v1/calendar-events/{id} | Delete |
| GET | /v1/inventory-items | List items (filter by source_platform, category; paginated — `page`/`size`, Q54) |
| POST | /v1/inventory-items | Add item |
| GET | /v1/inventory-items/{id} | Get single item |
| PUT | /v1/inventory-items/{id} | Full/partial update, including `is_consumed` toggle |
| DELETE | /v1/inventory-items/{id} | Delete |
| GET | /v1/goals | List goals (filter by status; paginated — `page`/`size`, Q54) |
| POST | /v1/goals | Create goal |
| GET | /v1/goals/{id} | Get single goal |
| PATCH | /v1/goals/{id} | Partial update |
| DELETE | /v1/goals/{id} | Delete |
| PUT | /v1/goals/{id}/current-amount | Update current_amount (called by the gateway projection engine only — not intended for direct client use) |

`page`/`size` are the shared `Page`/`Size` OpenAPI params (`application/contract/shared.yaml`): 0-indexed page, default size 50, max 200. All three `ListXxxResponse` bodies include `total_size` (grand total across all pages), `page`, and `size`.

---

## Key Files

| Layer | Path |
|---|---|
| Domain entities + enums | `application/domain/household/domain/src/main/java/com/suchika/household/domain/` |
| Input ports (use cases) | `application/domain/household/ports/src/main/java/com/suchika/household/ports/input/` |
| Output ports (repos) | `application/domain/household/ports/src/main/java/com/suchika/household/ports/output/` |
| JPA entities + DAOs + Panache repos | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/persistence/` |
| Services | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/service/` |
| HTTP resources + DTOs | `application/domain/household/adapters/src/main/java/com/suchika/household/adapters/http/` |
| Domain unit tests | `application/domain/household/domain/src/test/java/com/suchika/household/domain/` |
| Adapter tests (service + DB) | `application/domain/household/adapters/src/test/java/com/suchika/household/adapters/` |
| Flyway migration | `application/flyway/household/V1__init_household_consolidated.sql` |
| OpenAPI contract | `application/contract/household.yaml` |
| Gateway client/resource | `application/web-gateway/src/main/java/com/suchika/gateway/household/` (`HouseholdServiceClient`, `HouseholdGatewayResource`) |
| Gateway projection engine | `application/web-gateway/src/main/java/com/suchika/gateway/projection/ProjectionCalculationEngine.java` (2 of 12 steps; see Retrospective item 2) |
| Vacation Planner (gateway-native, not household code) | `application/web-gateway/src/main/java/com/suchika/gateway/vacationplanner/` |
| Frontend pages | `web/src/pages/Household/` — `Calendar.js`, `Inventory.js`, `Goals.js`, `VacationPlanner.js` (household data); `Profiles.js` (profile-domain data, grouped here for nav UX only — see Retrospective item 6) |

---

## Key Design Decisions

- Startup order: profile must run first (Flyway migrations reference `profile.profile`).
- Discriminator columns (`event_type`, `unit`, `source_platform`, `goal.status`) are VARCHAR, no DB CHECK — enforced at OpenAPI contract + Java enum layer only, per project-wide policy.
- Business-rule validation (`end_date >= start_date`, `quantity > 0`, `target_amount > 0`) lives in the domain layer's `create()` factory methods, which throw `IllegalArgumentException`. `current_amount >= 0` is the one exception — enforced in `GoalService.updateCurrentAmount()` with an explicit guard throwing `BadRequestException`, because that code path builds a `Goal` directly via the builder and never goes through `Goal.create()`.
- Conflict detection on calendar events is warning-only — creation is not blocked. `CalendarEventResponse` includes a `conflicting_events` list.
- `progressPercent()` and `daysToCompletion()` are computed by the domain `Goal` entity, not stored, and surfaced read-only in `GoalDto`.
- `goal.current_amount` is written by the web-gateway's `ProjectionCalculationEngine.computeGoalProgress()` step via `PUT /v1/goals/{id}/current-amount` — not by any direct client. Its snapshot key is `WEALTH_GOAL_PROGRESS` (named for the financial-goal concept, not the household schema it reads from — see Retrospective item 2).
- `profile_id` filter is injected only in the adapter layer (Panache repositories) — never in domain or ports, per ADR-006. Domain entities (`CalendarEvent`, `InventoryItem`, `Goal`) do hold `profileId` as a plain field, which is a deliberate, documented trade-off — see ADR-019 in `documents/ARCHITECTURE_DECISIONS.md`.
- All logging via `AppLogger`. All exceptions via `shared/exception/` hierarchy (`NotFoundException`, `BadRequestException`).

---

## Open Issues

- ✅ **v0.5.1 (2026-07-08): `profile_id` enforcement Tier A fixed for all three list endpoints.** `CalendarEventResource.list()`, `InventoryItemResource.list()`, and `GoalResource.list()` now call the shared `com.suchika.shared.utils.ResourceUtils.requireProfileId(profileId)` (added to `ResourceUtils` in this pass alongside the pre-existing `parsePage`/`parseSize`) as the first line, replacing an equivalent but ad-hoc inline `if (profileId == null) throw new BadRequestException(...)` check that had already been added to all three list methods independently (likely during the Q54 pagination pass) — behavior is unchanged, this normalizes household onto the same shared helper health already uses (`VitalReadingResource`, `DoctorVisitResource`). `BadRequestException` → automatic 400 via `ApplicationExceptionMapper`, unchanged. The three `POST` create endpoints take `profile_id` from the request body, not a query param, and were left alone per the plan (out of scope, different mechanism, not the gap being fixed). Verified via existing `listCalendarEvents_missingProfileId_returns400`, `listInventoryItems_missingProfileId_returns400`, `listGoals_missingProfileId_returns400` in the three `*ResourceTest` classes (already present, no new test methods needed — they already assert exactly this behavior and now exercise the shared-helper code path); all 46 tests across `CalendarEventResourceTest`/`GoalResourceTest`/`InventoryItemResourceTest` pass (0 failures/errors). **Tier B is explicitly out of scope for v0.5.1** — household's single-resource-by-id endpoints (`GET/PATCH/PUT/DELETE /v1/{calendar-events,inventory-items,goals}/{id}`) take no `profile_id` at all, so any caller who knows another profile's UUID can read/mutate it. Per the v0.5.1 plan, wealth is the sole Tier B pilot domain this release (highest-risk, financial data); household's Tier B fix (add `profile_id` as a required query param on all by-id endpoints, filter/404 on mismatch in the Panache repos) is planned as fast-follow work for v0.5.2/v0.6, not scheduled yet.
- ✅ **v0.5.1 (2026-07-08): schema nullability gap fixed.** `inventory_item.unit`/`inventory_item.source_platform` are `NOT NULL` again and `goal.status` has `DEFAULT 'ACTIVE' NOT NULL` again. Originally scheduled as a separate `V2__restore_not_null_constraints.sql`; per the v0.5.1 Flyway V2→V1 merge workstream that V2 was folded directly into `V1__init_household_consolidated.sql` instead (constraints declared inline on the original `CREATE TABLE` statements) and the V2 file deleted. Household domain is back to a single canonical V1 Flyway file. Required a full local `db-reset` (product-owner-approved second exception to the "never edit a committed migration" rule — see CLAUDE.md).
- **Adapter DB tests don't use Testcontainers**, contradicting both `ARCHITECTURE_GUIDELINES.md`'s stated standard and the product owner's Q34/Q35 resolution (2026-07-04) to move to Testcontainers. They instead run against the shared local dev Postgres via an `%integration-test` config-profile switch. This is a repo-wide gap, not household-specific, but household's four DB-backed test classes are the concrete instance checked in this pass. Worth a dedicated pass before v1.0 (real persistence, less tolerance for shared-DB test fragility).
- **Vacation Planner duplicates trip-date entry.** `household.calendar_event` already models `EventType.TRAVEL`, but the gateway's Vacation Planner takes trip dates as fresh user input instead of letting the user pick an existing travel event. Not a bug, just an unexploited integration opportunity.
- Task Tracking (assigning tasks to child profiles with calendar-linked deadlines) remains unbuilt — no schema, no code. Originally deferred from v0.3; still not scoped for any milestone.
- Dashboard/Action Center "Refresh Live Data" depends on `user.profile_id` being present in the auth context; if a future auth change omits it, the refresh action silently disables. Noted for whoever builds v1.0 auth.
- **New (2026-07-07, Q54 pagination pass): `ProjectionCalculationEngine`'s internal consumption of `listGoals`/`listCalendarEvents` now silently caps at 50 rows per profile.** Widening `HouseholdServiceClient`'s list methods to add `page`/`size` was a required, mechanical change (the alternative — a second, unpaginated client method — wasn't asked for and isn't how the wealth precedent was extended either, since wealth's own `ProjectionCalculationEngine` usage never called its list endpoint at all, only a dedicated balance-aggregate endpoint). The 3 internal call sites (`computeGoalProgress`, `computeEventSummary`, `addUpcomingEventsForMember`) now pass `null` for page/size, which the household resource layer resolves to the endpoint's default (page 0, size 50) — the same default any unpaginated caller gets. For any household with **more than 50 active goals** or **more than 50 calendar events matching the 30-day lookahead window**, this would silently truncate the data these compute steps see (goal progress stops updating for the excluded goals; the action-center/event-summary alerts miss the excluded events). Given goals are ordered `createdAt DESC` and events `startDate DESC`, the practical exposure is low today (no realistic household has 50+ goals; the calendar query already filters to a 30-day window before pagination even applies) — but it's a latent scale ceiling worth flagging for whoever next touches the projection engine or plans real multi-year usage. Not fixed here — doing so (e.g., looping the engine through pages, or adding a dedicated non-paginated internal-only endpoint) is a materially different, larger change than "add pagination to 3 list endpoints" and was out of scope for this pass.
