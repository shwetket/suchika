# v0.2 Development Plan — UAT-Ready Pilot

**Effort key:** S = <1 h · M = 1–3 h · L = 3–6 h

**v0.2 goal:** Profile + Wealth + Health fully working end-to-end. Household deferred to v0.3 — too much zero-to-running work to fit before UAT.
**v0.3 goal:** Household domain + polish pass after UAT feedback.

> **Scope decision (2026-06-19):** Household backend is 0 files. Shipping v0.2 without it keeps the UAT window short. Pilot users can still exercise Profiles, Wealth (accounts + CSV upload + transactions), and Health (vitals + doctor visits) — three domains is enough for meaningful feedback.

---

## UAT Readiness — What a Pilot User Must Be Able to Do

| # | User Action | Backend Ready | Frontend Ready |
|---|-------------|:---:|:---:|
| 1 | Open app, sign in (demo auth) | ✅ | ✅ |
| 2 | Create a profile / select existing | ✅ | ✅ |
| 3 | Log a vital reading (weight, BP, blood sugar) | ✅ | ❌ stub |
| 4 | View vital history in chronological order | ✅ | ❌ stub |
| 5 | Log a doctor visit, view visit list | ✅ | ❌ stub |
| 6 | Create a bank account | ✅ | ✅ |
| 7 | Upload a CSV statement, view transactions | ✅ | ✅ |
| 8 | Filter transactions by date/type | ✅ | ✅ |
| 9 | Navigate cleanly between all sections (no dead links) | ✅ | ⚠️ Health pages show "coming soon" |

**Blockers to UAT (v0.2):** items 3–5 only — Health frontend pages. Everything else is done or deferred.

> Items 9–10 (Household calendar + inventory) are **v0.3** — not blocking pilot.

---

## Current State (audited 2026-06-19)

| Domain | Backend | Gateway | Frontend | Backend Tests | Frontend Tests |
|--------|:-------:|:-------:|:--------:|:-------------:|:--------------:|
| Profile | ✅ | ✅ | ✅ full CRUD | ✅ | ✅ |
| Health | ✅ 29 files | ✅ | ❌ stubs | ✅ | ❌ |
| Wealth | ✅ 38 files | ✅ | ✅ full | ✅ | ✅ |
| Household | ❌ 0 files | ❌ | ❌ stubs | ❌ | ❌ |

**Remaining to reach UAT (v0.2):**
1. Health frontend pages — `Vitals.js` + `DoctorVisits.js` (backend live on :8083, gateway wired)
2. Jest tests for both Health pages
3. Remove ComingSoon stubs from Health nav links

---

## v0.2 Scope — UAT Milestone

### Phase A — Health Frontend ← ONLY REMAINING WORK FOR v0.2

Backend is fully live on :8083 and proxied through gateway :8080.

These tasks are independent and can start immediately. No gateway or contract changes needed.

| ID | Task | Files | Effort |
|----|------|-------|--------|
| ✅ A-T1 | Health backend: VitalReading + DoctorVisit domain, ports, adapters, HTTP | `health/` | done |
| ✅ A-T2 | HealthGatewayResource + HealthServiceClient | `web-gateway/` | done |
| ❌ A-T3 | Implement `Vitals.js`: form to log reading (vitalType, date, value, notes) + history table sorted by date DESC | `web/src/pages/Health/Vitals.js` | M |
| ❌ A-T4 | Implement `DoctorVisits.js`: form to log visit + list with follow-up date highlight | `web/src/pages/Health/DoctorVisits.js` | M |
| ❌ A-T5 | Jest tests: `Vitals.test.js` (renders table, empty state, form validation) + `DoctorVisits.test.js` | `web/src/pages/Health/` | M |
| ❌ A-T6 | Remove ComingSoon stubs from Health nav links | `web/src/components/Navigation.js` or routing file | S |

**Deps:** A-T3, A-T4 can run in parallel. A-T5 follows both. A-T6 follows A-T5.

---

### Phase E — v0.2 Quality Gate

| ID | Task | Effort | Deps |
|----|------|--------|------|
| ❌ E-T1 | `./gradlew test` — all domains + ArchUnit green | S | — |
| ❌ E-T2 | `cd web && npm run test:ci && npm run lint` — all frontend tests green | S | A-T5 |
| ❌ E-T3 | Manual smoke test: sign in → create profile → log vitals → view history → log doctor visit → create account → upload CSV → view/filter transactions | M | E-T1, E-T2 |
| ❌ E-T4 | Tag `v0.2` | S | E-T3 |

---

## v0.2 Sequencing

```
[Phase A — Health Frontend]   ← only remaining work
    └── [Phase E — Quality Gate → tag v0.2]
```

**Minimum critical path:** A-T3 + A-T4 (parallel) → A-T5 → A-T6 → E. One focused session.

---

## v0.2 Effort Summary

| Phase | Tasks | Est. Effort |
|-------|-------|-------------|
| A — Health Frontend | 6 | ~6 h |
| E — Quality Gate | 4 | ~2 h |
| **Total** | **10** | **~8 h** |

---

## v0.3 Scope — Household + Polish (post-UAT)

Do not start until `v0.2` is tagged and pilot feedback is collected.

### Household domain (zero files today)

| ID | Task | Effort |
|----|------|--------|
| H-T1 | Domain entities: `CalendarEvent` + enum `EventType`; `InventoryItem` | M |
| H-T2 | Ports: `CalendarEventUseCase` + `InventoryUseCase` + output repos | S |
| H-T3 | Services + persistence + HTTP: `CalendarEventService`, `InventoryService`, JPA entities, Panache repos, JAX-RS resources | L |
| H-T4 | Domain unit tests + Testcontainers adapter tests | M |
| H-T5 | `HouseholdServiceClient` + `HouseholdGatewayResource`; add household paths to `gateway.yaml` | S |
| H-T6 | Implement `Calendar.js` + `Inventory.js` frontend pages with Jest tests | M |

### Quality + Features

| ID | Task | Effort |
|----|------|--------|
| Q-T1 | SonarQube clean run — zero new issues across all domains | M |
| Q-T2 | Testcontainers coverage for Health adapters (`VitalReadingPanacheRepository`, `DoctorVisitPanacheRepository`) | M |
| Q-T3 | Calendar conflict detection — reject overlapping top-level events for same profile | M |
| Q-T4 | Sub-events — `parentEventId` support + `listSubEvents(parentId)` | M |
| Q-T5 | Reports page — wire to real transaction summary (spend by type, date range) | M |
| Q-T6 | Dashboard page wired to live data (account count, last vital, next event) | M |

---

## Non-Goals (v1.0+)

These are explicitly out of scope for v0.2 and v0.3:

- JWT / OIDC authentication
- Cloud deployment (Docker Compose, Kubernetes)
- Push or email notifications
- Mobile / PWA
- Statement auto-categorisation
- Household member invite flow

---

## Rules (non-negotiable on every task)

1. No `@Inject`, no JPA, no HTTP types in `domain/` — ArchUnit enforces this.
2. Every DB query filters by `profile_id` — injected in `adapters/` only.
3. Discriminator columns: plain `VARCHAR`, no SQL ENUM, no CHECK constraint.
4. Never edit a committed Flyway migration — add a new versioned file.
5. After any contract change: `cd web && npm run generate:api`.
6. Domain unit tests use plain `new` — no DI container.
7. Adapter tests use Testcontainers (real PostgreSQL) — no H2, no mocks.
