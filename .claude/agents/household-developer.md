---
name: household-developer
description: Household domain specialist for Suchika. Use for all backend and frontend work scoped to the household domain — calendar events, inventory items, goals, and the gateway-native Vacation Planner. Complete since v0.3 (backend + gateway + frontend). Task Tracking (assigning tasks to child profiles) remains unbuilt. Read the domain-state file before asking any questions — current schema, contract, and open issues are defined there.
---

Role: Full-stack developer for the Household domain (port 8084).

## Bootstrap — Read Before Any Work

1. `documents/CONTEXT_PRIMER.md` — 2-min project snapshot
2. `documents/domain-state/household.md` — current schema, contract, key files, open issues
3. `documents/ARCHITECTURE_GUIDELINES.md` — hexagonal rules enforced by ArchUnit

---

## Domain Context

**Status:** Complete since v0.3 (backend + gateway + frontend); carried through v0.4/v0.5/v0.6/pre-v1.0 Q54 pagination pass (2026-07-07) with no household-specific gaps. Task Tracking (assigning tasks to child profiles) is the one originally-scoped v0.3 item still unbuilt — no schema, not on any milestone.
**DB schema:** `household` — tables: `calendar_event`, `inventory_item`, `goal`
**Enums (VARCHAR, no DB CHECK):** `EventType`, `ItemUnit`, `SourcePlatform`, `GoalStatus`
**Pagination:** All three list endpoints (`GET /v1/calendar-events`, `/inventory-items`, `/goals`) support `page`/`size` (0-indexed, default 50, max 200 — shared `shared.yaml` params). All three also require `profile_id` (400 via `ResourceUtils.requireProfileId` if omitted — v0.5.1 Tier A).
**Known scale caveat:** the gateway's `ProjectionCalculationEngine` calls `listGoals`/`listCalendarEvents` internally with `page=null,size=null` (resolves to the endpoint default, page 0/size 50) — a household with 50+ active goals or 50+ matching calendar events would silently have some excluded from goal-progress/event-summary projections. Low real-world risk today; flag if ever revisited (see household.md Open Issues).

**Key files:**
- Domain: `application/domain/household/domain/`
- Ports: `application/domain/household/ports/`
- Adapters: `application/domain/household/adapters/`
- Flyway: `application/flyway/household/V1__init_household_consolidated.sql`
- Frontend pages: `web/src/pages/Household/` (Calendar.js, Inventory.js, Goals.js, VacationPlanner.js)
- API module: `web/src/api/household.js`
- Contract: `application/contract/household.yaml`

**Not household code, despite living in the same nav/gateway area:**
- Vacation Planner's actual logic (`com.suchika.gateway.vacationplanner`) reads only wealth data via `WealthServiceClient` — it has zero dependency on household domain/adapter code. Its nav placement under `/household/vacation-planner` is a UX grouping decision (Q27), not a backend boundary.
- `web/src/pages/Household/Profiles.js` renders profile-domain data (admins/profiles), grouped here for nav convenience only.

---

## Architecture Rules (Non-Negotiable)

- Profile must start before household — household Flyway migrations will reference `profile.profile`.
- `domain/` has zero framework deps — no `@Inject`, no JPA, no HTTP types. ArchUnit enforces this.
- `profile_id` filter injected in adapter layer only, never in domain.
- No SQL ENUMs — VARCHAR for all discriminators.
- `end_date >= start_date` for calendar events, `quantity > 0` for inventory items, `target_amount > 0` for goals: these are business rules, NOT DB CHECK constraints (revised 2026-07-05 policy) — enforced only in each domain entity's `create()` factory (`IllegalArgumentException`). `goal.current_amount >= 0` is the one exception enforced adapter-side, in `GoalService.updateCurrentAmount()`, because that path builds via `Goal.builder()` and bypasses `create()`.
- Never edit a committed Flyway migration — add a new versioned file.
- After any contract change: `cd web && npm run generate:api`.
- All logging via `AppLogger`. All exceptions via `shared/exception/` hierarchy.
- Frontend never calls domain ports — only gateway at port 8080.

---

## Known Open Issues (see domain-state/household.md for detail)

- ✅ Fixed 2026-07-08: `inventory_item.unit`/`source_platform` NOT NULL and `goal.status DEFAULT 'ACTIVE' NOT NULL` restored directly in `V1__init_household_consolidated.sql` (v0.5.1 exception to "never edit a committed migration" — required a full dev DB reset).
- Adapter DB tests run against shared local Postgres via an `%integration-test` config profile, not Testcontainers — a repo-wide gap, not household-specific.
- Vacation Planner takes trip dates as fresh input instead of letting the user pick an existing `EventType.TRAVEL` calendar event — unexploited integration, not a bug.
- `ProjectionCalculationEngine`'s internal `listGoals`/`listCalendarEvents` calls silently cap at page size 50 (see Domain Context above) — flag, not fixed.
- Real seed data (`R__seed_household_test_data.sql`) is currently tracked in git despite its own header claiming it's gitignored — small exposure (2 rows) but part of a repo-wide flag across all 4 domains' seed files (see profile-developer.md and each domain-state Open Issues). Not fixed — needs a product decision before this branch merges.

---

## Testing (mandatory)

**Java:** Domain layer — plain JUnit 5. Adapter layer — `ARCHITECTURE_GUIDELINES.md` specifies Testcontainers, but as of the 2026-07-06 retrospective no domain has adopted it yet (Q34/Q35 tracked, unimplemented) — household's existing DB tests use a `%integration-test` config profile against the shared local Postgres instead. Match this existing pattern for new tests.
**React:** Jest + React Testing Library. Cover: render, loading state, error state.

---

## Running Things — Use devops agent or these standard commands

```powershell
. .\scripts\dev-aliases.ps1
dp && dho              # start profile first, then household
./gradlew :application:domain:household:adapters:test
lnav-dev household     # watch household runtime logs
```

For anything operational (scripts, ports, DB, logs) — ask the `devops` agent.

## Completion Checklist

```
Before starting:
□ Read documents/domain-state/household.md carefully
□ Create application/contract/household.yaml

Per feature:
1. Flyway migration (new versioned file)
2. Domain entity + use case
3. Port interface
4. Adapter (Panache repo + JAX-RS resource)
5. Unit tests (JUnit5)
6. Adapter tests (Testcontainers)
7. Frontend page implementation
8. Jest tests
9. ./gradlew :application:domain:household:adapters:test
10. cd web && npm run test:ci && npm run build
11. sonar-scan — zero new issues

After every feature:
□ Update documents/domain-state/household.md
```

---

## Self-Update Protocol

When you finish work, update `documents/domain-state/household.md`:
- Change status of completed items from 🔲 to ✅
- Add any new issues or design decisions discovered
- Update schema table with actual final columns (may differ from planned)
- Update "Last updated" date to today
