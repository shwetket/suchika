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

**Status:** Complete since v0.3 (backend + gateway + frontend); carried through v0.4/v0.5/v0.6 with no household-specific gaps. Task Tracking (assigning tasks to child profiles) is the one originally-scoped v0.3 item still unbuilt — no schema, not on any milestone.
**DB schema:** `household` — tables: `calendar_event`, `inventory_item`, `goal`
**Enums (VARCHAR, no DB CHECK):** `EventType`, `ItemUnit`, `SourcePlatform`, `GoalStatus`

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
- `end_date >= start_date` for calendar events: this is a business-rule CHECK constraint — keep it in DB.
- Never edit a committed Flyway migration — add a new versioned file.
- After any contract change: `cd web && npm run generate:api`.
- All logging via `AppLogger`. All exceptions via `shared/exception/` hierarchy.
- Frontend never calls domain ports — only gateway at port 8080.

---

## Known Open Issues (see domain-state/household.md for detail)

- `inventory_item.unit`/`source_platform` and `goal.status` lost `NOT NULL` (and `status` its `DEFAULT 'ACTIVE'`) in the V1 Flyway consolidation rewrite — app-layer validation covers it in practice, DB does not. Fix via a new Flyway file, never edit the committed V1.
- Adapter DB tests run against shared local Postgres via an `%integration-test` config profile, not Testcontainers — a repo-wide gap, not household-specific.
- Vacation Planner takes trip dates as fresh input instead of letting the user pick an existing `EventType.TRAVEL` calendar event — unexploited integration, not a bug.

---

## Testing (mandatory)

**Java:** Domain layer — plain JUnit 5. Adapter layer — Testcontainers + real PostgreSQL.
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
