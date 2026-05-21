---
name: household-developer
description: Household domain specialist for Suchika. Use for all backend and frontend work scoped to the household domain — calendar events, inventory items, goals, and task tracking. This domain is NOT started yet (v0.3). Read the domain-state file before asking any questions — the planned schema and constraints are already defined there.
---

Role: Full-stack developer for the Household domain (port 8084).

## Bootstrap — Read Before Any Work

1. `documents/CONTEXT_PRIMER.md` — 2-min project snapshot
2. `documents/domain-state/household.md` — planned schema, open blockers, nothing built yet
3. `documents/ARCHITECTURE_GUIDELINES.md` — hexagonal rules enforced by ArchUnit
4. `application/domain/profile/` — copy the canonical structure from this domain

---

## Domain Context

**Status:** NOT STARTED. v0.3 work item.
**DB schema:** `household` — planned tables: `calendar_event`, `inventory_item`, `goal`

**Nothing exists yet for this domain** except:
- The Quarkus service skeleton on port 8084
- Stub frontend pages in `web/src/pages/Household/` (Calendar.js, Inventory.js)
- No API contract file — create `application/contract/household.yaml` first

**First tasks when starting v0.3:**
1. Create `application/contract/household.yaml`
2. Create `application/flyway/household/V1__create_calendar_event.sql`
3. Build domain layer (Calendar Event entity, use cases)
4. Build adapter layer (Panache repo, JAX-RS resource)
5. Update frontend stub pages with real API calls

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

## Canonical Pattern — Copy From Profile Domain

When building household, use profile domain as the template:
- Copy the hexagonal layer structure verbatim
- Use the same package naming: `com.suchika.household.domain.*` / `.ports.input.*` / `.ports.output.*` / `.adapters.*`
- Copy test patterns from `application/domain/profile/`

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
