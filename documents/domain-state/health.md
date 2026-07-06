# Health Domain State

## Objective

Give any agent or developer instant context on the health domain — vital readings and doctor visits. Includes schema (including the `visited_doctor → doctor_name NOT NULL` CHECK constraint rationale), API contract, and the v0.3+ backlog.

## Use Cases

- Before working on health backend or frontend — check Implementation Status and Key Files
- When adding a new vital type — the type list is in the Database Schema section; add to the OpenAPI enum and Java enum only (no Flyway migration needed)
- After completing health work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-05 (Flyway consolidation architect-review fix — FKs restored, domain-layer validation added)
**Version:** v0.2 complete — UAT-ready; v0.5 Phase 0 vitals edit endpoint added
**Port:** 8083

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Vital Readings CRUD | ✅ Complete | 10 vital types |
| Doctor Visits CRUD | ✅ Complete | Full visit lifecycle |
| Vitals list/filter | ✅ Complete | Filter by vital type |
| Doctor visit list/filter | ✅ Complete | Filter by profile |
| Frontend — Vitals | ✅ Complete | `web/src/pages/Health/Vitals.js` |
| Frontend — Doctor Visits | ✅ Complete | `web/src/pages/Health/DoctorVisits.js` |
| Trend analysis / charts | 🔲 v0.3+ | |
| Google Fit integration | 🔲 v1.0 | Manual sync only |

---

## Database Schema (`health` schema)

| Table | Key Columns |
|---|---|
| `vital_reading` | `id UUID PK`, `profile_id UUID FK`, `vital_type VARCHAR`, `value NUMERIC`, `unit VARCHAR`, `recorded_at TIMESTAMP` |
| `doctor_visit` | `id UUID PK`, `profile_id UUID FK`, `from_date DATE`, `to_date DATE`, `visited_doctor BOOLEAN`, `doctor_name VARCHAR` (NOT NULL if visited_doctor=TRUE), `hospital_name VARCHAR`, `speciality VARCHAR`, `symptoms TEXT`, `diagnosis TEXT`, `notes TEXT`, `follow_up_date DATE` |

Vital types (VARCHAR, no SQL ENUM): `WEIGHT`, `HEIGHT`, `BLOOD_PRESSURE`, `BLOOD_SUGAR_FASTING`, `BLOOD_SUGAR_PP`, `HEART_RATE`, `TEMPERATURE`, `OXYGEN_SATURATION`, `BMI`, `WAIST_CIRCUMFERENCE`

**Post-Q31/Q32 consolidation (2026-07-05):** `application/flyway/health/V1__init_health_consolidated.sql` now defines only PK + FK constraints — `fk_vital_profile` (`vital_reading.profile_id → profile.profile(id)`, `ON DELETE RESTRICT`) and `fk_visit_profile` (`doctor_visit.profile_id → profile.profile(id)`, `ON DELETE RESTRICT`). **No CHECK constraints remain in the DB** — `chk_vital_type`, `chk_bp_secondary_value`, `chk_primary_positive`, `chk_visit_dates`, and `chk_doctor_name_required` (all present in the old pre-consolidation `V1__init_health.sql`/`V2__remove_enum_constraints.sql`) were dropped and are **not** replacements at the DB level. The business-rule ones (not the enum one, already covered by the OpenAPI/Java enum) are now enforced in the domain layer instead — see Key Design Decisions below. `visited_doctor = TRUE → doctor_name NOT NULL` is therefore enforced in application code only, not by DB CHECK, as of this change.

---

## API Contract

File: `application/contract/health.yaml` (mirrored verbatim into `application/web-gateway/src/main/resources/health.yaml`)
Service base URL: `http://localhost:8083` — frontend never calls this directly, only via gateway (`http://localhost:8080/v1/...`, same paths).
- `GET    /v1/vitals?profile_id=&vital_type=`
- `POST   /v1/vitals`
- `GET    /v1/vitals/{id}`
- `PATCH  /v1/vitals/{id}` — **new, v0.5 Phase 0.** Partial update (reading_date, value_primary, value_secondary, unit, notes). `vital_type` and `profile_id` are immutable — delete and re-record to change either.
- `DELETE /v1/vitals/{id}`
- `GET    /v1/doctor-visits?profile_id=`
- `POST   /v1/doctor-visits`
- `GET    /v1/doctor-visits/{id}`
- `PATCH  /v1/doctor-visits/{id}` (note: this is PATCH, not PUT — domain-state doc previously said PUT in error)
- `DELETE /v1/doctor-visits/{id}`

Gateway proxy: `application/web-gateway/src/main/java/com/suchika/gateway/health/HealthGatewayResource.java` + `HealthServiceClient.java` — both mirror every health-service path 1:1, including the new vitals PATCH.

---

## Key Files

| Layer | Path |
|---|---|
| Domain | `application/domain/health/domain/src/main/java/com/suchika/health/domain/` |
| Ports | `application/domain/health/ports/src/main/java/com/suchika/health/ports/` |
| Adapters | `application/domain/health/adapters/src/main/java/com/suchika/health/adapters/` |
| Flyway | `application/flyway/health/` |
| Frontend | `web/src/pages/Health/` (Vitals.js, DoctorVisits.js) |
| API module | `web/src/api/health.js` |

---

## Key Design Decisions

- Blood pressure is stored as a single numeric value (mean arterial pressure or systolic) — the specific format is defined in the OpenAPI contract.
- `doctor_name` NOT NULL constraint only applies when `visited_doctor = TRUE` — **as of 2026-07-05 this is enforced in the domain layer (`DoctorVisit.create()`), not a DB CHECK constraint** (the DB CHECK was dropped in the Flyway consolidation per Q31; see below).
- No Google Fit tokens stored — v1.0 will add manual sync with short-lived tokens only.
- **Domain-layer validation added 2026-07-05** (`VitalReading.create(...)`, `DoctorVisit.create(...)`, static factories matching the `CalendarEvent.create()`/`Goal.create()`/`InventoryItem.create()` pattern used in the household domain — throws `IllegalArgumentException`): `VitalReading.create()` enforces `value_primary > 0` (always) and `value_secondary` required when `vital_type == BLOOD_PRESSURE`; `DoctorVisit.create()` enforces `to_date >= from_date` (when `to_date` present) and `doctor_name` required (non-blank) when `visited_doctor == true`. These are the direct domain-layer replacements for the DB CHECK constraints (`chk_primary_positive`, `chk_bp_secondary_value`, `chk_visit_dates`, `chk_doctor_name_required`) dropped from `V1__init_health_consolidated.sql`. `VitalReadingService.recordReading()` and `DoctorVisitService.create()` were rewired to construct via these factories instead of the raw `Builder` — safe because each service's existing `validate()`/`validateCreate()` private method (throwing `BadRequestException`, unchanged) already runs first and enforces the same rules, so the new `IllegalArgumentException` path is defense-in-depth only and never fires on the existing HTTP-facing call paths; all pre-existing `BadRequestException`-asserting tests continue to pass unchanged. `update()` paths on both services were left using `Builder` directly (unchanged) — they already re-validate via the same private `validate()` methods before persisting, matching the equivalent pattern in the household domain (`CalendarEventService.update()` also uses `Builder`, not `create()`, for merged-state reconstruction).

---

## Open Issues / v0.3+ Backlog

- Vital trend charts / visualization (v0.3)
- BMI auto-calculation from height + weight readings (v0.3)
- Google Fit manual sync (v1.0)
- ~~v0.5 Phase 0: `PUT /v1/vitals/{id}` endpoint + edit modal~~ — **Done 2026-07-02.** Implemented as `PATCH /v1/vitals/{id}` (matches the existing doctor-visits convention, which is also PATCH, not PUT as an earlier version of this doc said). Details below.
- ✅ **v0.6: `VitalReadingResource`/`DoctorVisitResource` HTTP adapter unit tests + `VitalReading` domain entity unit test — COMPLETE (2026-07-03).** `VitalReadingResourceTest`, `DoctorVisitResourceTest`, `VitalReadingTest` — closes the re-scoped v0.6 "Testing Foundation" gaps, plain JUnit 5 unit tests with stub use cases (no `@QuarkusTest`).
- ✅ **v0.6: Date-range filter on doctor visit list — COMPLETE (2026-07-03).** `from`/`to` query params on `GET /v1/doctor-visits`, filtering by the visit's `from_date` falling within `[from, to]`. Modified `DoctorVisitRepository.findByProfileId`/`DoctorVisitUseCase.listByProfile` signatures in place (not an additive overload like the wealth transaction pagination change) since there was exactly one caller and one test fake for each. Added `DoctorVisitPanacheRepositoryTest` — this persistence layer had zero test coverage before this change; the new tests verify the JPQL date filter against a real Postgres instance, not just a fake repository.
- ✅ **v0.5 Phase 3: Consolidated Action Center biometric streak gaps — COMPLETE (2026-07-02).** No health-domain code changed — this is a gateway-only read of the existing `GET /v1/vitals?profile_id=X` endpoint. `ProjectionCalculationEngine.computeActionCenterAlerts()` (web-gateway `projection` package) calls `healthServiceClient.listVitals(memberProfileId, null)` per household member, groups by `vital_type` keeping the latest `reading_date` (same "newest-first" assumption as the existing `computeVitalsSummary` step), and flags a gap (`ACTION_CENTER_ALERTS_FAMILY.biometric_streak_gaps`) for WEIGHT/BLOOD_PRESSURE/BLOOD_SUGAR_FASTING (Q30's "core 3" — not all 10 `VitalType` values) if the last reading is 30+ days old, or if that vital type has never been logged at all (treated as an infinite gap, not silently skipped).

### v0.5 Phase 0 — Vital Reading Edit (done 2026-07-02)

- **Backend:** `UpdateVitalReadingCommand` (ports/input) + `VitalReadingUseCase.update(id, command)` + `VitalReadingService.update` (partial update: only non-null fields overwrite; `vital_type`/`profile_id` immutable; re-validates BLOOD_PRESSURE secondary-value rule and positive-value rule on the merged result). `VitalReadingRepository.save` already handled upsert-by-id via `EntityManager.merge`, so no new repository method was needed — same pattern as `DoctorVisitRepository`.
- `UpdateVitalReadingRequest` DTO added (adapters/http/dto) — standalone class, does NOT extend `VitalReadingFields` because that base class carries the immutable `profile_id`/`vital_type` fields.
- `VitalReadingResource.update` — new `@PATCH /{id}` handler, same shape as `DoctorVisitResource.update`.
- Contract: `health.yaml` gained `patch:` under `/v1/vitals/{id}` + `UpdateVitalReadingRequest` schema; the "readings are immutable" line in the POST description was corrected. Mirrored byte-for-byte into `web-gateway/src/main/resources/health.yaml`.
- Gateway: `HealthServiceClient.updateVital` + `HealthGatewayResource.updateVital` added, proxying `JsonNode` exactly like `updateDoctorVisit`.
- **Frontend:** `web/src/api/health.js` gained `updateVital(id, data)` (PATCH). `web/src/pages/Health/Vitals.js` gained an edit modal — Edit button per row (next to Delete), pre-filled form, `vital_type` shown read-only (disabled input) since it's immutable, Blood Pressure diastolic field conditionally shown, save/cancel/error states — mirrors `DoctorVisits.js` edit-modal UX exactly.
- **Tests added:** `VitalReadingServiceTest` (update_partial_fields, update_rejects_zero_value_primary, update_throws_not_found_for_unknown_id), `VitalReadingPanacheRepositoryTest` (save_withExistingId_updatesReadingInPlace, Testcontainers), `HealthGatewayResourceTest` (testUpdateVital), `Vitals.test.js` (edit modal opens pre-filled, submits and reloads, shows error on failure), `health.test.js` (updateVital calls PATCH with correct path/body).

---

## Flyway Consolidation Architect-Review Fix — 2026-07-05

Architect review of the Flyway consolidation on the `quesitons` branch found that `V1__init_health_consolidated.sql` had silently dropped **both FK constraints** (`fk_vital_profile`, `fk_visit_profile`) — not just the CHECK constraints Q31 approved removing — and that no domain-layer validation existed to replace the dropped business-rule CHECK constraints (`chk_primary_positive`, `chk_bp_secondary_value`, `chk_visit_dates`, `chk_doctor_name_required`), a live correctness regression (nothing stopped invalid data). Per Q31's resolution ("Remove CHECK constraints, but keep FKs for referential integrity"), fixed both issues in place (Q32 pre-approved editing the consolidated V1 file directly, pre-release):

- ✅ **FKs restored** — `fk_vital_profile` and `fk_visit_profile` added back verbatim (from `git show main:application/flyway/health/V1__init_health.sql`) to `V1__init_health_consolidated.sql`. No CHECK constraints re-added (enum or business-rule) — those stay removed per policy.
- ✅ **Domain-layer validation added** — `VitalReading.create(...)` and `DoctorVisit.create(...)` static factories (see Key Design Decisions above) now enforce the two business rules per entity that the dropped CHECK constraints used to guard. `VitalReadingService.recordReading()`/`DoctorVisitService.create()` rewired to use them.
- ✅ **Tests added:** `VitalReadingTest` gained 5 new `create()` test methods (happy path, BP-with-secondary, BP-missing-secondary throws, zero/negative/null `value_primary` throws). New file `DoctorVisitTest.java` (domain layer, didn't exist before) added with 7 tests covering `create()` happy paths and both validation rules.
- ✅ **Local dev DB reset performed** (the accepted Q32 consequence of editing a committed V1 in place): editing the migration file changed its checksum, so the local `app_db`'s `health.flyway_schema_history` row for version 1 no longer matched, causing `FlywayValidateException` on adapter test runs (3 failed, 12 skipped). Fixed by dropping the three `health`-schema tables owned by `app_user` (`health.vital_reading`, `health.doctor_visit`, `health.flyway_schema_history` — connecting as `app_user`, not the Postgres superuser, was sufficient since `app_user` owns these tables even though it didn't create the schema itself) and letting Flyway re-apply `V1__init_health_consolidated.sql` fresh on the next test run. Only the `health` schema was touched — `profile`/`wealth`/`household` schemas and data were left intact.
- ✅ **Test results (verified, not assumed):** `:application:domain:health:domain:test` → **BUILD SUCCESSFUL**, 16 tests, 0 failures. `:application:domain:health:adapters:test` → **BUILD SUCCESSFUL**, 59 tests, 0 failures, 0 errors, 0 skipped (was 3 failed + 12 skipped before the DB reset, all via `FlywayValidateException`, none related to the validation logic itself).
- Per Q48 (create a `scripts/reset-local-db.ps1`/`.sh` helper for this exact scenario, owned by the `devops` agent) — not built in this session; this was a one-off manual reset scoped to the `health` schema only, done via `psql -U app_user -d app_db`.

---

## Integration Test Coverage — 2026-07-03 (QA pass)

- ✅ **`VitalReadingCreateUpdateIT` added** (`application/domain/health/adapters/src/test/java/com/suchika/health/adapters/http/VitalReadingCreateUpdateIT.java`) — true `@QuarkusTest` + real Postgres integration test: `POST /v1/vitals` -> `PATCH /v1/vitals/{id}` round trip, using the real, DI-wired `VitalReadingService` and real Panache repository (not the stub use-case pattern `VitalReadingResourceTest` uses). Verifies a partial update only overwrites the fields provided (`value_primary`/`notes`) while `reading_date`/`unit` are preserved, and re-fetches via `GET` to prove the write landed in real Postgres. Also covers the BLOOD_PRESSURE-missing-diastolic `BadRequestException` guard and unknown-id `NotFoundException` on PATCH. Compile-verified clean; **not executed** this session — see `documents/OpenQuestions.md` Q34 (same live-shared-DB-truncation risk as profile/wealth).
- ✅ **Cross-domain gateway dashboard integration test added** (`application/web-gateway/src/test/java/com/suchika/gateway/e2e/CrossDomainDashboardE2ETest.java`, plain JUnit 5, no `@QuarkusTest`) — posts a `WEIGHT` vital reading via the live health service (`:8083`) as part of a full profile->wealth->health->gateway flow, then asserts the real `HEALTH_VITALS_SUMMARY` snapshot reflects that exact reading after `POST /v1/projections/refresh/{profileId}`. This is the highest-value new coverage this session — see `documents/OpenQuestions.md` Q36/Q37 for the architecture rationale (why it's plain JUnit hitting live HTTP ports rather than a `@QuarkusTest`) and its current execution status (compiles clean; skips via `assumeTrue` if services aren't reachable — confirmed to skip cleanly in this session's sandboxed environment, not a defect in the test itself).
