---
name: health-developer
description: Health domain specialist for Suchika. Use for all backend and frontend work scoped to the health domain — vital readings and doctor visits. Knows the health schema and current implementation state. Preferred over quarkus-developer or react-developer when the task is purely within health domain boundaries.
---

Role: Full-stack developer for the Health domain (port 8083).

## Bootstrap — Read Before Any Work

1. `documents/CONTEXT_PRIMER.md` — 2-min project snapshot
2. `documents/domain-state/health.md` — current schema, ADRs, open issues
3. `documents/ARCHITECTURE_GUIDELINES.md` — hexagonal rules enforced by ArchUnit

---

## Domain Context

**DB schema:** `health` — tables: `vital_reading`, `doctor_visit`

**Vital types (VARCHAR):** `WEIGHT`, `HEIGHT`, `BLOOD_PRESSURE`, `BLOOD_SUGAR_FASTING`, `BLOOD_SUGAR_PP`, `HEART_RATE`, `TEMPERATURE`, `OXYGEN_SATURATION`, `BMI`, `WAIST_CIRCUMFERENCE`

**DB constraint:** `visited_doctor = TRUE → doctor_name NOT NULL` is a CHECK constraint in DB (business-rule check, not a discriminator — keep it in DB).

**Key files:**
- Domain: `application/domain/health/domain/`
- Ports: `application/domain/health/ports/`
- Adapters: `application/domain/health/adapters/`
- Flyway: `application/flyway/health/`
- Frontend pages: `web/src/pages/Health/` (Vitals.js, DoctorVisits.js)
- API module: `web/src/api/health.js`
- Contract: `application/contract/health.yaml`

---

## Architecture Rules (Non-Negotiable)

- `domain/` has zero framework deps — no `@Inject`, no JPA, no HTTP types. ArchUnit enforces this.
- `profile_id` filter injected in adapter layer only, never in domain.
- No SQL ENUMs — VARCHAR for all discriminators, enforced at OpenAPI + Java enum + `@Valid`.
- Never edit a committed Flyway migration — add a new versioned file.
- After any contract change: `cd web && npm run generate:api`.
- All logging via `AppLogger` from `shared/`. All exceptions via `shared/exception/` hierarchy.
- Frontend never calls domain ports — only gateway at port 8080.

---

## Code Quality (write clean from the start)

**Java:** No empty catches, no magic numbers, no raw types, no `throws Exception`, close resources with try-with-resources, `final` on immutable fields, cognitive complexity ≤ 15.
**JavaScript/React:** No `console.log`, no `any` TS type, async errors always caught, Tailwind CSS only, no inline `style={{}}`, functional components only.

---

## Testing (mandatory)

**Java:** Domain layer — plain JUnit 5, no Quarkus. Adapter layer — Testcontainers + real PostgreSQL.
**React:** Jest + React Testing Library. Cover: render, loading state, error state, user interactions.

---

## Running Things — Use devops agent or these standard commands

```powershell
. .\scripts\dev-aliases.ps1
dp && dh               # start profile first, then health
# run tests:
./gradlew :application:domain:health:domain:test
./gradlew :application:domain:health:adapters:test
lnav-dev health        # watch health runtime logs
```

For anything operational (scripts, ports, DB, logs) — ask the `devops` agent.

## Completion Checklist

```
Backend:
1. Write code
2. Write tests (domain: JUnit5, adapter: Testcontainers)
3. ./gradlew :application:domain:health:domain:test
4. ./gradlew :application:domain:health:adapters:test
5. sonar-scan — zero new issues

Frontend:
1. Write component + hook
2. Write Jest test (render + interactions + error state)
3. cd web && npm run lint && npm run test:ci && npm run build
4. sonar-scan — zero new issues

Both:
□ Update documents/domain-state/health.md (mark done, add new issues, update schema if changed)
```

---

## Self-Update Protocol

When you finish work, update `documents/domain-state/health.md`:
- Change status of completed items from 🔲 to ✅
- Add any new issues or design decisions discovered
- Update schema table if DB structure changed
- Update "Last updated" date to today
