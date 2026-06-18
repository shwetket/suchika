# v0.2 Development Plan — Usable Local App

**Goal:** Reach a state where all four domains are functional end-to-end and the three
v0.2 features (deduplication, biometric history, itinerary + task management) are live.

**Status as of 2026-06-17:**
- Profile domain: fully implemented
- Health domain: backend + gateway + frontend pages done; Testcontainers tests and Jest tests still missing
- Wealth domain (Accounts): backend + gateway + frontend page done; Testcontainers repo test missing
- Wealth domain (Transactions): all production code + service-level tests + frontend page done; Testcontainers integration test missing
- Household domain: schema + contract ready, **zero Java code**
- Gateway: profile, health, wealth proxied; household not wired
- Frontend: Health pages (Vitals, DoctorVisits), Wealth pages (Accounts, Transactions) wired; Household pages are stubs; no Jest tests exist anywhere

**Effort key:** S = <1 h · M = 1–3 h · L = 3–6 h

---

## Phase 1 — Contracts & Schema (Foundation)

**Goal:** Lock every OpenAPI contract and add the two missing DB columns before any Java is written.
All downstream phases depend on this being stable.

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P1-T1 | Fill in `profile.yaml` with all 10 endpoints (list/create/get/update/delete for admins + profiles) | `application/contract/profile.yaml` | S | — |
| ✅ P1-T2 | Revise `health.yaml` — remove `health-profiles` resource; add `/v1/vitals` (CRUD + list-by-profile) and `/v1/doctor-visits` (CRUD) aligned with actual schema | `application/contract/health.yaml` | M | — |
| ✅ P1-T3 | Write `household.yaml` — define endpoints for calendar-events (CRUD + list), inventory-items (CRUD + list), goals (CRUD + list) | `application/contract/household.yaml` | M | — |
| ✅ P1-T4 | Add `parent_event_id` + `assigned_to_profile_id` columns to `household.calendar_event` via new Flyway migration `V4__sub_events_and_tasks.sql` | `application/flyway/household/V4__sub_events_and_tasks.sql` | S | — |
| ✅ P1-T5 | Add transaction endpoints to `wealth.yaml` (upload statement, list transactions, get transaction, rollback upload) | `application/contract/wealth.yaml` | M | — |
| ✅ P1-T6 | Write `application/contract/gateway.yaml` with all proxied paths (profile, wealth, health, household) | `application/contract/gateway.yaml` | M | P1-T1,T2,T3,T5 |

---

## Phase 2 — Health Domain (v0.1 + v0.2)

**Goal:** Manual biometric entry and chronological history query working end-to-end.
Health is the simplest domain — good first momentum win.

### Backend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P2-T1 | Create domain entities: `VitalReading` (profileId, vitalType, readingDate, valuePrimary, valueSecondary, unit, notes) and `DoctorVisit` (profileId, fromDate, toDate, visitedDoctor, doctorName, symptoms, diagnosis, followUpDate) | `health/domain/src/main/java/com/suchika/health/domain/` | M | — |
| ✅ P2-T2 | Create enum `VitalType` (WEIGHT, HEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR_FASTING, BLOOD_SUGAR_PP, HEART_RATE, TEMPERATURE, OXYGEN_SATURATION, BMI) | `health/domain/` | S | P2-T1 |
| ✅ P2-T3 | Define input ports: `VitalReadingUseCase` (record, getById, listByProfile ordered by date DESC, delete) + `DoctorVisitUseCase` (create, getById, listByProfile, update, delete) | `health/ports/src/main/java/com/suchika/health/ports/input/` | S | P2-T1 |
| ✅ P2-T4 | Define output ports: `VitalReadingRepository` + `DoctorVisitRepository` | `health/ports/src/main/java/com/suchika/health/ports/output/` | S | P2-T3 |
| ✅ P2-T5 | Implement `VitalReadingService` and `DoctorVisitService` (use case implementations; no framework deps) | `health/adapters/src/main/java/com/suchika/health/adapters/services/` | M | P2-T3,T4 |
| ✅ P2-T6 | Create JPA entities `VitalReadingEntity` + `DoctorVisitEntity`; implement Panache repos | `health/adapters/src/main/java/com/suchika/health/adapters/persistence/` | M | P2-T4 |
| ✅ P2-T7 | Create JAX-RS `VitalReadingResource` (`POST /v1/vitals`, `GET /v1/vitals?profile_id=&order=DATE_ASC`, `GET /v1/vitals/{id}`, `DELETE /v1/vitals/{id}`) and `DoctorVisitResource` (full CRUD) | `health/adapters/src/main/java/com/suchika/health/adapters/http/` | M | P2-T5,T6 |
| ✅ P2-T8 | Write domain unit tests for `VitalReadingService` (happy path + edge: future date, negative value) and `DoctorVisitService` (visitedDoctor=true requires doctorName) | `health/domain/src/test/` | M | P2-T5 |
| ❌ P2-T9 | Write Testcontainers adapter test for `VitalReadingPanacheRepository` (save, find ordered by date, profile_id scoping) | `health/adapters/src/test/` | M | P2-T6 |

### Gateway

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P2-T10 | Create `HealthServiceClient` (MicroProfile Rest Client) + `HealthGatewayResource` (proxies vitals + doctor-visits) | `web-gateway/src/main/java/com/suchika/gateway/health/` | S | P2-T7 |
| ✅ P2-T11 | Mirror `health.yaml` into `web-gateway/src/main/resources/` for the Rest Client | `web-gateway/src/main/resources/health.yaml` | S | P1-T2,P2-T10 |

### Frontend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P2-T12 | Regenerate API client: `cd web && npm run generate:api` | `web/src/api/generated.ts` | S | P1-T6,P2-T11 |
| ✅ P2-T13 | Implement `Vitals.js` page: form to log a reading + table showing history in chronological order | `web/src/pages/Health/Vitals.js` | M | P2-T12 |
| ✅ P2-T14 | Implement `DoctorVisits.js` page: form to log a visit + list with follow-up date highlighting | `web/src/pages/Health/DoctorVisits.js` | M | P2-T12 |
| ❌ P2-T15 | Write Jest tests for `Vitals.js` (renders table, handles empty state, form validation) | `web/src/pages/Health/Vitals.test.js` | M | P2-T13 |
| ❌ P2-T16 | SonarQube scan — fix all new issues in Health domain | local `ss` (sonar-scan alias) | S | P2-T9,P2-T15 |

---

## Phase 3 — Wealth Domain: Accounts (v0.1 foundation)

**Goal:** Account CRUD end-to-end. No transactions yet — establishes the module skeleton.

### Backend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P3-T1 | Create domain entity `Account` (id, profileId, accountName, accountType, institutionName, openingBalance, creditLimit, interestRate, emiAmount, isActive) and enum `AccountType` | `wealth/domain/src/main/java/com/suchika/finance/domain/` | M | — |
| ✅ P3-T2 | Define input port `AccountUseCase` (createAccount, getAccount, listAccounts, updateAccount, deactivateAccount) + output port `AccountRepository` | `wealth/ports/` | S | P3-T1 |
| ✅ P3-T3 | Implement `AccountService` — enforces: cannot deactivate account with transactions; accountType immutable after creation | `wealth/adapters/.../services/` | M | P3-T2 |
| ✅ P3-T4 | Create `AccountEntity` JPA entity + `AccountPanacheRepository` | `wealth/adapters/.../persistence/` | M | P3-T2 |
| ✅ P3-T5 | Create JAX-RS `AccountResource` (matches `wealth.yaml` account paths: list with pagination + filters, create, get, patch, delete) | `wealth/adapters/.../http/` | M | P3-T3,P3-T4 |
| ✅ P3-T6 | Domain unit tests for `AccountService` (create, deactivate with/without transactions, immutable type) | `wealth/domain/src/test/` | M | P3-T3 |
| ❌ P3-T7 | Testcontainers adapter test for `AccountPanacheRepository` (CRUD, profile_id scoping, active filter) | `wealth/adapters/src/test/` | M | P3-T4 |

### Gateway + Frontend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P3-T8 | Create `WealthServiceClient` + `WealthGatewayResource` (accounts only) | `web-gateway/.../wealth/` | S | P3-T5 |
| ✅ P3-T9 | Implement `Accounts.js` page: table of accounts with type badges + create form | `web/src/pages/Wealth/Accounts.js` | M | P2-T12 (regen api) |
| ❌ P3-T10 | SonarQube scan — fix all new issues in Wealth accounts | `ss` | S | P3-T7,P3-T9 |

---

## Phase 4 — Wealth Domain: Transactions & Deduplication (v0.1 + v0.2)

**Goal:** CSV upload, transaction ledger, and deduplication logic (same-file + cross-file).

### Backend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P4-T1 | Create domain entities: `Transaction` (id, accountId, uploadId, txnDate, amount, txnType, description, metadata) and `StatementUpload` (id, accountId, fileName, uploadDate, status) + enum `TxnType` (CREDIT, DEBIT) + `UploadStatus` | `wealth/domain/` | M | P3-T1 |
| ✅ P4-T2 | Define input ports: `StatementUploadUseCase` (uploadStatement, rollbackUpload, getUpload, listUploads) + `TransactionUseCase` (listByAccount, getById) | `wealth/ports/input/` | S | P4-T1 |
| ✅ P4-T3 | Define output ports: `TransactionRepository` (save, findByAccountId, existsByUniqueKey) + `StatementUploadRepository` (save, findById, updateStatus, findByAccountId) | `wealth/ports/output/` | S | P4-T2 |
| ✅ P4-T4 | Implement CSV parsing service `StatementCsvParser` (reads account-type config from `application.properties`; extracts date, amount, txnType, description; normalises amount to positive; classifies negative amounts as DEBIT) | `wealth/adapters/.../services/` | L | P4-T1 |
| ✅ P4-T5 | **v0.2 — Same-file deduplication:** in `StatementUploadService`, before DB insert, detect rows within the same upload that share (date, amount, txnType, description); assign a sequence suffix to the description to make them distinct valid events | `wealth/adapters/.../services/StatementUploadService.java` | M | P4-T4 |
| ✅ P4-T6 | **v0.2 — Cross-file deduplication:** in `StatementUploadService`, catch the DB unique-constraint violation on `(account_id, txn_date, amount, txn_type, description)` and silently skip the duplicate row (log count of skipped rows to AppLogger) | `StatementUploadService.java` | M | P4-T5 |
| ✅ P4-T7 | Implement `TransactionPanacheRepository` + `StatementUploadPanacheRepository` | `wealth/adapters/.../persistence/` | M | P4-T3 |
| ✅ P4-T8 | Create JAX-RS `TransactionResource` (`GET /v1/accounts/{id}/transactions` with date-range, type filter, pagination) + `StatementUploadResource` (`POST /v1/accounts/{id}/uploads` multipart, `DELETE /v1/accounts/{id}/uploads/{upload_id}` rollback, `GET /v1/accounts/{id}/uploads`) | `wealth/adapters/.../http/` | L | P4-T5,P4-T6,P4-T7 |
| ✅ P4-T9 | Domain unit tests for `StatementCsvParser` (HDFC savings format, credit card format, negative-to-DEBIT normalisation) | `wealth/domain/src/test/` | M | P4-T4 |
| ✅ P4-T10 | Domain unit tests for `StatementUploadService` deduplication (same-file: two identical rows → both kept with suffix; cross-file: second upload of same row → silently dropped) | `wealth/domain/src/test/` | M | P4-T5,P4-T6 |
| ❌ P4-T11 | Testcontainers adapter test (upload CSV, query transactions sorted, rollback removes transactions) | `wealth/adapters/src/test/` | M | P4-T7,P4-T8 |

### Gateway + Frontend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ✅ P4-T12 | Add transaction + upload endpoints to `WealthGatewayResource` | `web-gateway/.../wealth/` | S | P4-T8 |
| ✅ P4-T13 | Implement `Transactions.js` page: filterable ledger table (date range, type) per account + upload CSV button | `web/src/pages/Wealth/Transactions.js` | L | P2-T12 (regen api) |
| ❌ P4-T14 | SonarQube scan — fix all new issues in Wealth transactions | `ss` | S | P4-T11,P4-T13 |

---

## Phase 5 — Household Domain: Calendar & Inventory (v0.1)

**Goal:** Calendar events and inventory CRUD working end-to-end.

### Backend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ❌ P5-T1 | Create domain entity `CalendarEvent` (id, profileId, title, eventType, startDate, endDate, location, notes, parentEventId, assignedToProfileId, isTask, deadline) and enum `EventType` | `household/domain/` | M | P1-T4 |
| ❌ P5-T2 | Create domain entity `InventoryItem` (id, profileId, itemName, quantity, unit, sourcePlatform, purchaseDate, category) and enums `UnitType`, `SourcePlatform` | `household/domain/` | M | — |
| ❌ P5-T3 | Define input port `CalendarEventUseCase` (create, getById, listByProfile, update, delete) + `InventoryUseCase` (create, getById, listByProfile, update, delete) | `household/ports/input/` | S | P5-T1,P5-T2 |
| ❌ P5-T4 | Define output ports `CalendarEventRepository` + `InventoryRepository` | `household/ports/output/` | S | P5-T3 |
| ❌ P5-T5 | Implement `CalendarEventService` (basic CRUD; no conflict detection yet — that's P6) | `household/adapters/.../services/` | M | P5-T3,P5-T4 |
| ❌ P5-T6 | Implement `InventoryService` (basic CRUD) | `household/adapters/.../services/` | S | P5-T3,P5-T4 |
| ❌ P5-T7 | Create JPA entities + Panache repos for CalendarEvent + InventoryItem | `household/adapters/.../persistence/` | M | P5-T4 |
| ❌ P5-T8 | Create JAX-RS `CalendarEventResource` (CRUD + `GET /v1/calendar-events?profile_id=&from=&to=`) + `InventoryResource` (CRUD + list) | `household/adapters/.../http/` | M | P5-T5,P5-T6,P5-T7 |
| ❌ P5-T9 | Domain unit tests for `CalendarEventService` + `InventoryService` | `household/domain/src/test/` | M | P5-T5,P5-T6 |
| ❌ P5-T10 | Testcontainers adapter test for both repos (profile_id scoping, date range filter) | `household/adapters/src/test/` | M | P5-T7 |

### Gateway + Frontend

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ❌ P5-T11 | Create `HouseholdServiceClient` + `HouseholdGatewayResource` (calendar + inventory) | `web-gateway/.../household/` | S | P5-T8 |
| ❌ P5-T12 | Implement `Calendar.js` page: monthly/list view of events, create event form | `web/src/pages/Household/Calendar.js` | M | P2-T12 (regen api) |
| ❌ P5-T13 | Implement `Inventory.js` page: item list with filters, add item form | `web/src/pages/Household/Inventory.js` | M | P2-T12 |
| ❌ P5-T14 | SonarQube scan — fix all new issues in Household calendar/inventory | `ss` | S | P5-T10,P5-T13 |

---

## Phase 6 — Household Domain: Itinerary & Tasks (v0.2)

**Goal:** Sub-events under master events, conflict detection, task assignment to child profiles.

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ❌ P6-T1 | **Sub-event grouping:** extend `CalendarEventService.create()` to accept `parentEventId`; validate parent event exists + belongs to same profile; add `listSubEvents(parentId)` use case | `household/ports/input/CalendarEventUseCase.java` + `CalendarEventService.java` | M | P5-T5 |
| ❌ P6-T2 | **Conflict detection:** add `ConflictDetectionService` — given a new event (profileId, startDate, endDate), query existing top-level events for same profile and flag any overlap; throw `BadRequestException` with details on overlap | `household/adapters/.../services/ConflictDetectionService.java` | M | P5-T5,P5-T7 |
| ❌ P6-T3 | Wire conflict detection into `CalendarEventResource.create()` and `.update()` (only for top-level events — sub-events exempt) | `CalendarEventResource.java` | S | P6-T1,P6-T2 |
| ❌ P6-T4 | **Task assignment:** add `POST /v1/calendar-events/{id}/tasks` endpoint — creates a sub-event with `isTask=true`, `assignedToProfileId` (must exist in profile.profile), and `deadline` (must be ≤ parent event end date) | `CalendarEventResource.java` + `CalendarEventUseCase.java` | M | P6-T1 |
| ❌ P6-T5 | Add `assigned_to_profile_id` FK check: call profile service via gateway to verify assigned profile exists (or cache the check) | `household/adapters/.../services/CalendarEventService.java` | M | P6-T4 |
| ❌ P6-T6 | Unit tests: conflict detection (no overlap, exact boundary, partial overlap, sub-event skip); task assignment (valid child profile, deadline after parent end) | `household/domain/src/test/` | M | P6-T1,P6-T2,P6-T4 |
| ❌ P6-T7 | Testcontainers test: create master event → add sub-events → list grouped; conflict detection integration | `household/adapters/src/test/` | M | P6-T3,P6-T4 |
| ❌ P6-T8 | Update `Calendar.js` UI: expand master event to show sub-events; add task panel with assignee + deadline | `web/src/pages/Household/Calendar.js` | M | P6-T3 |
| ❌ P6-T9 | SonarQube scan — fix all new issues in Household v0.2 features | `ss` | S | P6-T7,P6-T8 |

---

## Phase 7 — Gateway & Frontend Integration

**Goal:** Complete gateway contract, regenerate typed API client, wire all remaining frontend pages.

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ❌ P7-T1 | Finalize `gateway.yaml` (merge all domain paths; verify profiles, vitals, doctor-visits, accounts, transactions, uploads, calendar-events, inventory, goals) | `application/contract/gateway.yaml` | M | P1-T6, all domain phases |
| ❌ P7-T2 | Regenerate frontend API client | `web/src/api/generated.ts` (`npm run generate:api`) | S | P7-T1 |
| ❌ P7-T3 | Wire `Profiles.js` (Household) to use generated client for profile lookups | `web/src/pages/Household/Profiles.js` | S | P7-T2 |
| ❌ P7-T4 | Implement `Health/Profile.js` page — display list of profiles with their latest vital stats | `web/src/pages/Health/Profile.js` | S | P7-T2 |
| ❌ P7-T5 | Update `Navigation.js` — ensure all domain routes are wired; remove ComingSoon stubs from pages that now have real content | `web/src/components/Navigation.js` | S | P7-T2 |
| ❌ P7-T6 | Jest tests for any untested frontend pages | `web/src/pages/` | M | P7-T5 |

---

## Phase 8 — Quality Gate

**Goal:** All tests green, SonarQube at zero new issues, manual smoke test of every domain.

| ID | Task | Scope | Effort | Deps |
|----|------|-------|--------|------|
| ❌ P8-T1 | Run `./gradlew test` — fix any remaining failures across all domains + ArchUnit | all modules | M | all phases |
| ❌ P8-T2 | Run `cd web && npm run test:ci && npm run lint && npm run format:check` — fix any failures | `web/` | S | P7-T6 |
| ❌ P8-T3 | Full SonarQube scan (`bv` — build-verify): fix all remaining issues, smells, hotspots | all | M | P8-T1,P8-T2 |
| ❌ P8-T4 | Smoke test checklist (run manually with all 5 services up): | — | M | P8-T3 |
| | · Create admin + 2 profiles (primary + child) | | | |
| | · Log 3 vital readings; confirm chronological list (Health v0.2 ✓) | | | |
| | · Create account; upload CSV; verify transactions; upload same CSV again → dedup (Wealth v0.2 ✓) | | | |
| | · Create master calendar event; add 2 sub-events; try conflicting event → rejected (Household v0.2 ✓) | | | |
| | · Assign task to child profile with deadline | | | |
| ❌ P8-T5 | Tag git commit `v0.2` once smoke test passes | git | S | P8-T4 |

---

## Dependency & Sequencing Overview

```
P1 (Contracts + Schema)
├── P2 (Health)         ← start here: simplest domain, builds confidence
├── P3 (Wealth Accounts)
│   └── P4 (Wealth Transactions + Dedup)
└── P5 (Household Calendar/Inventory)
    └── P6 (Household Itinerary/Tasks — v0.2)
        └── P7 (Gateway + Frontend Integration)
            └── P8 (Quality Gate → tag v0.2)
```

P2, P3, and P5 can be worked **in parallel** once P1 is done.
P4 requires P3 complete. P6 requires P5 complete.

---

## Task Count Summary

| Phase | Tasks | Total Effort |
|-------|-------|-------------|
| P1 — Contracts & Schema | 6 | ~4 h |
| P2 — Health (v0.1+v0.2) | 16 | ~12 h |
| P3 — Wealth Accounts | 10 | ~8 h |
| P4 — Wealth Transactions + Dedup | 14 | ~14 h |
| P5 — Household Calendar/Inventory | 14 | ~10 h |
| P6 — Household Itinerary/Tasks | 9 | ~8 h |
| P7 — Gateway + Frontend | 6 | ~4 h |
| P8 — Quality Gate | 5 | ~4 h |
| **Total** | **80** | **~64 h** |

---

## Rules for Each Task (non-negotiable)

1. **Write tests before declaring done** — domain unit + Testcontainers adapter (or Jest for frontend).
2. **Run `./gradlew test` locally** after each backend task — ArchUnit must stay green.
3. **Run `ss` (sonar-scan)** at the end of each phase — fix all new issues before moving to next phase.
4. **No framework code in `domain/`** — no `@Inject`, no JPA, no HTTP types.
5. **Every DB query must filter by `profile_id`** — injected in adapter layer only.
6. **After any contract change** — run `gapi` (`cd web && npm run generate:api`).
7. **Never edit a committed Flyway migration** — add a new versioned file.

---

## v1.0 Vision

Themes beyond v0.2 — no detailed tasks, just direction:

- **Authentication & authorization** — JWT-based login, session management, role enforcement at gateway (admin vs. member); profile self-service for non-admin members
- **Cloud deployment** — containerise all five services (Docker Compose first, then Kubernetes manifests); CI/CD pipeline to push images on tag
- **Notifications** — push/email alerts for upcoming calendar deadlines, low inventory, follow-up doctor visit reminders; pluggable notification adapter (start with email, add push later)
- **Dashboard & analytics** — net-worth trend chart (wealth CQRS projection), BMI history graph, grocery spend by category; all driven from `projections.dashboard_snapshot`
- **Statement intelligence** — auto-categorise transactions (groceries, fuel, EMI) using rule engine; surfaced as spend-by-category report in Transactions page
- **Household member onboarding** — invite flow for child profiles; member can log their own vitals and view assigned tasks without admin access
- **Data export** — per-domain CSV/PDF export (transactions, vitals history, calendar); useful before any cloud migration
- **Mobile-first PWA** — convert React app to Progressive Web App with offline support for viewing last-fetched data
