# Health Domain State

## Objective

Give any agent or developer instant context on the health domain — vital readings and doctor visits. Includes schema (including the `visited_doctor → doctor_name NOT NULL` CHECK constraint rationale), API contract, and the v0.3+ backlog.

## Use Cases

- Before working on health backend or frontend — check Implementation Status and Key Files
- When adding a new vital type — the type list is in the Database Schema section; add to the OpenAPI enum and Java enum only (no Flyway migration needed)
- After completing health work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-03 (v0.6 — adapter/domain test coverage + doctor visit date-range filter)
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

DB constraint rule: `visited_doctor = TRUE → doctor_name NOT NULL` (enforced in DB as a CHECK constraint — this is a **business-rule check**, not a discriminator enum, so it belongs in DB).

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
- `doctor_name` NOT NULL constraint only applies when `visited_doctor = TRUE` — this is a DB CHECK constraint (business rule), not removed.
- No Google Fit tokens stored — v1.0 will add manual sync with short-lived tokens only.

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
