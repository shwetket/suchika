# Health Domain State

## Objective

Give any agent or developer instant context on the health domain — vital readings and doctor visits. Includes schema (including the `visited_doctor → doctor_name NOT NULL` CHECK constraint rationale), API contract, and the v0.3+ backlog.

## Use Cases

- Before working on health backend or frontend — check Implementation Status and Key Files
- When adding a new vital type — the type list is in the Database Schema section; add to the OpenAPI enum and Java enum only (no Flyway migration needed)
- After completing health work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-08 (v0.5.1 remediation — fixed Vitals list page zero-rows bug, `data.vitals` → `data.vital_readings`)
**Version:** v0.2 complete — UAT-ready; v0.5 Phase 0 vitals edit endpoint added; pre-v1.0 pagination pass complete
**Port:** 8083

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Vital Readings CRUD | ✅ Complete | 10 vital types |
| Doctor Visits CRUD | ✅ Complete | Full visit lifecycle |
| Vitals list/filter | ✅ Complete | Filter by vital type; **paginated (2026-07-07, Q54)** — `page`/`size` query params, defaults 0/50, max size 200 |
| Doctor visit list/filter | ✅ Complete | Filter by profile + date range; **paginated (2026-07-07, Q54)** — same `page`/`size` convention |
| Frontend — Vitals | ✅ Complete | `web/src/pages/Health/Vitals.js` — Previous/Next pagination controls, `PAGE_SIZE = 20` |
| Frontend — Doctor Visits | ✅ Complete | `web/src/pages/Health/DoctorVisits.js` — Previous/Next pagination controls, `PAGE_SIZE = 20` |
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
- `GET    /v1/vitals?profile_id=&vital_type=&page=&size=` — **paginated 2026-07-07 (Q54)**. `page` 0-indexed (default 0), `size` default 50 max 200 (`shared.yaml#/components/parameters/Page`/`Size`, same shape as wealth's transaction list). Response gains `page`/`size` fields alongside the existing `total_size`.
- `POST   /v1/vitals`
- `GET    /v1/vitals/{id}`
- `PATCH  /v1/vitals/{id}` — **new, v0.5 Phase 0.** Partial update (reading_date, value_primary, value_secondary, unit, notes). `vital_type` and `profile_id` are immutable — delete and re-record to change either.
- `DELETE /v1/vitals/{id}`
- `GET    /v1/doctor-visits?profile_id=&from=&to=&page=&size=` — **paginated 2026-07-07 (Q54)**, same `page`/`size` convention as vitals above.
- `POST   /v1/doctor-visits`
- `GET    /v1/doctor-visits/{id}`
- `PATCH  /v1/doctor-visits/{id}` (note: this is PATCH, not PUT — domain-state doc previously said PUT in error)
- `DELETE /v1/doctor-visits/{id}`

Gateway proxy: `application/web-gateway/src/main/java/com/suchika/gateway/health/HealthGatewayResource.java` + `HealthServiceClient.java` — both mirror every health-service path 1:1, including the new vitals PATCH and the `page`/`size` passthrough params on both list endpoints (raw `JsonNode` passthrough, no re-typing, same convention as every other gateway health method).

---

## Key Files

| Layer | Path |
|---|---|
| Domain | `application/domain/health/domain/src/main/java/com/suchika/health/domain/` |
| Ports | `application/domain/health/ports/src/main/java/com/suchika/health/ports/` |
| Adapters | `application/domain/health/adapters/src/main/java/com/suchika/health/adapters/` |
| Flyway | `application/flyway/health/` |
| Frontend | `web/src/pages/Health/` (Vitals.js, DoctorVisits.js; also Profile.js — a routed `/health/profile` "Coming Soon" stub with no backend, see Open Issues) |
| API module | `web/src/api/health.js` |

---

## Key Design Decisions

- Blood pressure is stored as a single numeric value (mean arterial pressure or systolic) — the specific format is defined in the OpenAPI contract.
- `doctor_name` NOT NULL constraint only applies when `visited_doctor = TRUE` — **as of 2026-07-05 this is enforced in the domain layer (`DoctorVisit.create()`), not a DB CHECK constraint** (the DB CHECK was dropped in the Flyway consolidation per Q31; see below).
- No Google Fit tokens stored — v1.0 will add manual sync with short-lived tokens only.
- **Domain-layer validation added 2026-07-05** (`VitalReading.create(...)`, `DoctorVisit.create(...)`, static factories matching the `CalendarEvent.create()`/`Goal.create()`/`InventoryItem.create()` pattern used in the household domain — throws `IllegalArgumentException`): `VitalReading.create()` enforces `value_primary > 0` (always) and `value_secondary` required when `vital_type == BLOOD_PRESSURE`; `DoctorVisit.create()` enforces `to_date >= from_date` (when `to_date` present) and `doctor_name` required (non-blank) when `visited_doctor == true`. These are the direct domain-layer replacements for the DB CHECK constraints (`chk_primary_positive`, `chk_bp_secondary_value`, `chk_visit_dates`, `chk_doctor_name_required`) dropped from `V1__init_health_consolidated.sql`. `VitalReadingService.recordReading()` and `DoctorVisitService.create()` were rewired to construct via these factories instead of the raw `Builder` — safe because each service's existing `validate()`/`validateCreate()` private method (throwing `BadRequestException`, unchanged) already runs first and enforces the same rules, so the new `IllegalArgumentException` path is defense-in-depth only and never fires on the existing HTTP-facing call paths; all pre-existing `BadRequestException`-asserting tests continue to pass unchanged. `update()` paths on both services were left using `Builder` directly (unchanged) — they already re-validate via the same private `validate()` methods before persisting, matching the equivalent pattern in the household domain (`CalendarEventService.update()` also uses `Builder`, not `create()`, for merged-state reconstruction).
- **`DoctorVisit.create()` signature simplified 2026-07-06** — reduced from 11 positional parameters to 6 (`profileId, fromDate, toDate, visitedDoctor, doctorName, VisitDetails details`) by extracting the six narrative/optional fields that carry no `create()`-time validation rule (`hospitalName, speciality, symptoms, diagnosis, notes, followUpDate`) into a new nested static value type, `DoctorVisit.VisitDetails`, with a `VisitDetails.empty()` convenience factory for the no-extra-details case. Pure call-signature simplification, no behavior change — the same two validation rules (`to_date >= from_date`, `doctor_name` required when `visited_doctor == true`) still live on the 5 non-grouped params exactly as before. `DoctorVisitService.create()` was updated to construct `new DoctorVisit.VisitDetails(command.hospitalName(), ...)` inline before calling the factory. Verified consistent: `DoctorVisitTest.java` (domain, 7 tests, all using the new signature) and `DoctorVisitServiceTest.java` (adapter) both compile against and correctly exercise the new signature. **Scoped to the domain factory only** — the ports-layer `CreateDoctorVisitCommand`/`UpdateDoctorVisitCommand` records are intentionally unchanged (still flat 11/8 fields); extending the `VisitDetails` grouping up through the port layer would be a reasonable future follow-up but is not a defect as-is.

---

## Open Issues / v0.3+ Backlog

- **[New 2026-07-06] Orphaned "Health Profile" scaffold — recommend deleting or specifying, pre-v1.0.** Three artifacts reference a `HealthProfile` concept (blood type, allergies, chronic conditions, medications) that has **zero backing** anywhere: not in `health.yaml`, not in the DB schema, not in `documents/BUSINESS_REQUIREMENTS.md` or `ROADMAP.md` (grepped, zero hits), and not previously in this file. (1) `web/src/pages/Health/Profile.js` is a live, routed "Coming Soon" stub (`/health/profile` in `App.js`, behind `ProtectedRoute`) — reachable UI for a feature that doesn't exist. (2) `web/src/types/health.ts` hand-declares a `HealthProfile` interface plus `VitalReading`/`DoctorVisit` shapes that don't match the real API at all (wrong field names, wrong enum values) — confirmed **zero importers** anywhere in `web/src` (dead code). Notably, `health.yaml`'s own top-of-file description states "*no separate health-profile entity*" — this file directly contradicts the domain's own documented design decision. (3) `web/src/api/generated.d.ts` independently declares `HealthProfile`/`CreateHealthProfileRequest`/`ListHealthProfilesResponse` types that exist in **neither** `health.yaml` nor `gateway.yaml`. Related repo-wide observation, noted here but not health-specific: no file under `web/src` imports from `api/generated` at all — every page (including Vitals.js/DoctorVisits.js) uses hand-written `api/<domain>.js` wrapper modules instead, so the entire generated client appears unused. Recommend a decision before v1.0: either write a real spec for a Health Profile feature and implement it, or delete the dead scaffold (route + page + the orphaned type declarations).
- ✅ **v0.5.1 (2026-07-08): VARCHAR length inconsistencies on `doctor_visit` name columns fixed.** All three layers now agree at `VARCHAR(50)` / `length = 50` / `maxLength: 50`: DB (`doctor_name`, `hospital_name`, `speciality` in `V1__init_health_consolidated.sql`, edited directly — no interim `V2__` since health had none yet), JPA (`DoctorVisitEntity`), and OpenAPI contract (`CreateDoctorVisitRequest` and `UpdateDoctorVisitRequest` in `application/contract/health.yaml`, mirrored byte-identical into `application/web-gateway/src/main/resources/health.yaml`). Also fixed the `UpdateDoctorVisitRequest.speciality` missing-`maxLength` asymmetry noted below (now has `maxLength: 50` matching Create). Frontend client regenerated (`npm run generate:api`) — no diff, since `gateway.yaml` doesn't mirror these domain CRUD schemas. Required a full local `db-reset` (product-owner-approved second exception to the "never edit a committed migration" rule, scoped to this v0.5.1 release — see CLAUDE.md).

- Vital trend charts / visualization (v0.3)
- BMI auto-calculation from height + weight readings (v0.3)
- Google Fit manual sync (v1.0)
- ~~v0.5 Phase 0: `PUT /v1/vitals/{id}` endpoint + edit modal~~ — **Done 2026-07-02.** Implemented as `PATCH /v1/vitals/{id}` (matches the existing doctor-visits convention, which is also PATCH, not PUT as an earlier version of this doc said). Details below.
- ✅ **v0.6: `VitalReadingResource`/`DoctorVisitResource` HTTP adapter unit tests + `VitalReading` domain entity unit test — COMPLETE (2026-07-03).** `VitalReadingResourceTest`, `DoctorVisitResourceTest`, `VitalReadingTest` — closes the re-scoped v0.6 "Testing Foundation" gaps, plain JUnit 5 unit tests with stub use cases (no `@QuarkusTest`).
- ✅ **v0.6: Date-range filter on doctor visit list — COMPLETE (2026-07-03).** `from`/`to` query params on `GET /v1/doctor-visits`, filtering by the visit's `from_date` falling within `[from, to]`. Modified `DoctorVisitRepository.findByProfileId`/`DoctorVisitUseCase.listByProfile` signatures in place (not an additive overload like the wealth transaction pagination change) since there was exactly one caller and one test fake for each. Added `DoctorVisitPanacheRepositoryTest` — this persistence layer had zero test coverage before this change; the new tests verify the JPQL date filter against a real Postgres instance, not just a fake repository.
- ✅ **v0.5 Phase 3: Consolidated Action Center biometric streak gaps — COMPLETE (2026-07-02).** No health-domain code changed — this is a gateway-only read of the existing `GET /v1/vitals?profile_id=X` endpoint. `ProjectionCalculationEngine.computeActionCenterAlerts()` (web-gateway `projection` package) calls `healthServiceClient.listVitals(memberProfileId, null)` per household member (as of 2026-07-07 this call now also passes explicit `page`/`size` args — see the pagination entry below), groups by `vital_type` keeping the latest `reading_date` (same "newest-first" assumption as the existing `computeVitalsSummary` step), and flags a gap (`ACTION_CENTER_ALERTS_FAMILY.biometric_streak_gaps`) for WEIGHT/BLOOD_PRESSURE/BLOOD_SUGAR_FASTING (Q30's "core 3" — not all 10 `VitalType` values) if the last reading is 30+ days old, or if that vital type has never been logged at all (treated as an infinite gap, not silently skipped).
- ✅ **Pre-v1.0 pagination pass (Q54) — COMPLETE (2026-07-07).** `GET /v1/vitals` and `GET /v1/doctor-visits` are now paginated, mirroring wealth's transaction-list pattern exactly (shared `page`/`size` OpenAPI params from `shared.yaml`, 0-indexed page, default size 50, max 200). Full details in the dedicated section below.

### v0.5 Phase 0 — Vital Reading Edit (done 2026-07-02)

- **Backend:** `UpdateVitalReadingCommand` (ports/input) + `VitalReadingUseCase.update(id, command)` + `VitalReadingService.update` (partial update: only non-null fields overwrite; `vital_type`/`profile_id` immutable; re-validates BLOOD_PRESSURE secondary-value rule and positive-value rule on the merged result). `VitalReadingRepository.save` already handled upsert-by-id via `EntityManager.merge`, so no new repository method was needed — same pattern as `DoctorVisitRepository`.
- `UpdateVitalReadingRequest` DTO added (adapters/http/dto) — standalone class, does NOT extend `VitalReadingFields` because that base class carries the immutable `profile_id`/`vital_type` fields.
- `VitalReadingResource.update` — new `@PATCH /{id}` handler, same shape as `DoctorVisitResource.update`.
- Contract: `health.yaml` gained `patch:` under `/v1/vitals/{id}` + `UpdateVitalReadingRequest` schema; the "readings are immutable" line in the POST description was corrected. Mirrored byte-for-byte into `web-gateway/src/main/resources/health.yaml`.
- Gateway: `HealthServiceClient.updateVital` + `HealthGatewayResource.updateVital` added, proxying `JsonNode` exactly like `updateDoctorVisit`.
- **Frontend:** `web/src/api/health.js` gained `updateVital(id, data)` (PATCH). `web/src/pages/Health/Vitals.js` gained an edit modal — Edit button per row (next to Delete), pre-filled form, `vital_type` shown read-only (disabled input) since it's immutable, Blood Pressure diastolic field conditionally shown, save/cancel/error states — mirrors `DoctorVisits.js` edit-modal UX exactly.
- **Tests added:** `VitalReadingServiceTest` (update_partial_fields, update_rejects_zero_value_primary, update_throws_not_found_for_unknown_id), `VitalReadingPanacheRepositoryTest` (save_withExistingId_updatesReadingInPlace — `@QuarkusTest` + `%integration-test` profile against shared local Postgres, corrected 2026-07-06: this was mislabeled "Testcontainers" here previously; it was never actually Testcontainers, see the 2026-07-06 Retrospective section), `HealthGatewayResourceTest` (testUpdateVital), `Vitals.test.js` (edit modal opens pre-filled, submits and reloads, shows error on failure), `health.test.js` (updateVital calls PATCH with correct path/body).

### Pre-v1.0 Pagination Pass — Q54 (done 2026-07-07)

Product-owner-directed follow-up to the v0.6 retrospective finding that wealth's transaction list was the only paginated list endpoint in the system. Extended the identical pattern to health's two list endpoints — `GET /v1/vitals` and `GET /v1/doctor-visits` — mirroring `TransactionResource`/`TransactionService`/`TransactionPanacheRepository`/`ListTransactionsResponse` structurally, not just superficially. The two shared OpenAPI parameters (`shared.yaml#/components/parameters/Page`/`Size` — 0-indexed page default 0, size default 50 max 200) are referenced via `$ref`, not redefined.

- **Ports:** New records `PagedDoctorVisits(List<DoctorVisit> visits, long totalCount)` and `PagedVitalReadings(List<VitalReading> readings, long totalCount)` in `ports/input`, mirroring `PagedTransactions`. `DoctorVisitUseCase`/`VitalReadingUseCase` each gained an additive `listByProfilePaginated(...)` method — the existing unpaginated `listByProfile(...)` is kept unchanged for any other caller. `DoctorVisitRepository`/`VitalReadingRepository` each gained a `findByProfileId(..., int page, int size)` overload plus a `countByProfileId(...)` method using the same filter parameters (no page/size).
- **Service:** `DoctorVisitService.listByProfilePaginated`/`VitalReadingService.listByProfilePaginated` validate `profile_id` non-null (same rule as the existing unpaginated method) then call the repo's paged-find and count separately, combining into the Paged record — two repo calls, not one, same as `TransactionService`.
- **Repository:** `DoctorVisitPanacheRepository`/`VitalReadingPanacheRepository` were refactored to extract a private `buildFilter(...)` helper (returning a `Filter(String query, List<Object> params)` record) shared by the unpaginated find, the paginated find (`io.quarkus.panache.common.Page.of(page, size)`), and the count method — avoids duplicating the WHERE-clause construction across three call sites (Sonar CPD), matching `TransactionPanacheRepository`'s exact structure. `VitalReadingPanacheRepository` previously had two hardcoded query strings (with/without `vital_type`) inlined directly in `findByProfileId`; these were consolidated into the same `buildFilter` helper as part of this change — pure refactor, no behavior change to the unpaginated method.
- **HTTP resource:** `DoctorVisitResource`/`VitalReadingResource` gained `@QueryParam("page") Integer pageParam, @QueryParam("size") Integer sizeParam` (boxed, nullable to distinguish "absent" from "0"), private `parsePage`/`parseSize` validators (`BadRequestException` on `page < 0` or `size` outside `[1, 200]`), and `DEFAULT_PAGE_SIZE = 50`/`MAX_PAGE_SIZE = 200` constants — byte-for-byte the same validation shape as `TransactionResource`.
- **Response DTOs:** `ListDoctorVisitsResponse`/`ListVitalReadingsResponse` kept their old items-only constructor and gained a new `(items, totalSize, page, size)` constructor. `total_size`'s Java field type was widened from `int` to `long` (matching `ListTransactionsResponse.totalSize` and the repository's `long` count return type) — the OpenAPI schema's `total_size` field itself was left as plain `type: integer` (unchanged) since only `page`/`size` were additive per the unified-shape directive; the JSON wire format is unaffected either way.
- **Contract:** Both `GET` operations in `health.yaml` gained the two shared `$ref`s; both response schemas gained `page`/`size` properties alongside the existing `total_size`. Copied byte-for-byte into `web-gateway/src/main/resources/health.yaml` (verified via `diff`).
- **Gateway:** `HealthServiceClient`/`HealthGatewayResource` `listVitals`/`listDoctorVisits` methods gained `@QueryParam("page") Integer page, @QueryParam("size") Integer size`, simply forwarded through — same raw-`JsonNode`-passthrough convention as every other health gateway method.
- **Unexpected ripple effect, fixed in the same pass:** `ProjectionCalculationEngine` (web-gateway) has two internal, non-HTTP callers of `healthServiceClient.listVitals(...)` — `computeVitalsSummary()` and `addStreakGapsForMember()` (Consolidated Action Center biometric streak gaps) — that previously called the 2-arg signature directly. Since the interface signature changed to 4 args, this broke `:application:web-gateway:compileJava`. Fixed by passing `0, VITALS_AGGREGATION_FETCH_SIZE` (`= 200`, the system's own page-size ceiling) explicitly at both call sites — both aggregations only need the latest reading per `vital_type` (10 types total, newest-first order), so 200 is functionally unbounded for realistic reading volumes without reintroducing an unpaginated code path into the contract. **Known limitation to flag if ever revisited:** a household member who logs one vital type very frequently (e.g., daily blood pressure) for a long enough period could in theory push a rarely-logged type's (e.g., annual weigh-in) latest reading outside the most recent 200 rows, causing `computeVitalsSummary`/`computeActionCenterAlerts` to miss it. This is a pre-existing-style edge case (the same risk existed implicitly before pagination too, just with no ceiling at all), not a regression introduced by this pass, and considered acceptable for a personal/household-scale dataset. `ProjectionCalculationEngineTest`'s 7 corresponding Mockito stubs were updated to match (`anyInt()` matchers for the new params); no assertions changed.
- **Frontend:** `web/src/api/health.js`'s `listVitals`/`listDoctorVisits` gained optional `page`/`size` params appended to `URLSearchParams` only when non-null/undefined (same pattern as every other optional filter param). `web/src/pages/Health/Vitals.js`/`DoctorVisits.js` gained `page`/`totalSize` state, `PAGE_SIZE = 20`, a `useCallback` load function reading `data.total_size`, a separate filter-reset `useEffect` (resets `page` to 0 when filters change, not when `page` itself changes — `eslint-disable-next-line react-hooks/exhaustive-deps` on the intentionally incomplete dependency array), a derived `totalPages` (never stored in state), and the same Previous/Next button block as `Transactions.js`. ~~Note: `Vitals.js` still reads `data.vitals` while the actual JSON key is `vital_readings`~~ — **fixed 2026-07-08, see v0.5.1 remediation entry below.**
- **Tests added:** Backend — `DoctorVisitServiceTest`/`VitalReadingServiceTest` (`listByProfilePaginated_*`, 3 tests each), `DoctorVisitResourceTest`/`VitalReadingResourceTest` (5 new pagination/validation tests each, `StubUseCase` updated to implement the new interface method), `DoctorVisitPanacheRepositoryTest`/`VitalReadingPanacheRepositoryTest` (3 new tests each against real Postgres — paginated ordering + count, scoped to distinct dates/vital-types to avoid seed-data collision). All 81 tests in `:application:domain:health:adapters:test` pass (0 failures); `:application:domain:health:domain:test` unaffected (pagination doesn't touch the domain layer). Web-gateway: `:application:web-gateway:test` passes (`ProjectionCalculationEngineTest` 27/27). Frontend — `DoctorVisits.test.js`/`Vitals.test.js` gained 3 pagination tests each (page-count display, Next-page request, last-page Next-disabled) plus updated the 2 pre-existing `listDoctorVisits` call-args assertions to include `page`/`size`; `health.test.js` gained 2 tests each for `listVitals`/`listDoctorVisits` (page/size appended vs. omitted). 42 frontend tests pass; lint clean; production build compiles.

---

## v0.5.1 Remediation — Vitals list page zero-rows bug fixed (2026-07-08)

Multi-model code review (Workstream 1, item 2 of the v0.5.1 remediation plan) found `web/src/pages/Health/Vitals.js` always rendered zero rows in production, despite showing the correct total count. Root cause: `loadVitals()` read `data.vitals ?? []`, but `ListVitalReadingsResponse` (per `application/contract/health.yaml`) actually returns the field as `vital_readings`. `data.vitals` was always `undefined`, so the table body silently fell back to `[]` while `data.total_size` (a correctly-named top-level field) rendered the real count — a "N results, 0 shown" bug. This had already been flagged as a known-but-unfixed issue in the Q54 pagination-pass entry above; this pass fixes it.

- **Fix:** `Vitals.js` line ~171-172, `setVitals(data.vitals ?? [])` → `setVitals(data.vital_readings ?? [])`; the adjacent fallback `setTotalSize(data.total_size ?? (data.vitals ?? []).length)` → `... (data.vital_readings ?? []).length)`. Grepped the whole file for `.vitals` to confirm these were the only two occurrences. `DoctorVisits.js` and every other sibling list page were already checked in the original review and read their equivalent field correctly — this was the sole outlier.
- **Root cause in the test suite too:** every existing `Vitals.test.js` mock returned `{ vitals: MOCK_VITALS }` — i.e. the tests were asserting against the same wrong field name the buggy code happened to read, so they passed against broken production code. Fixed all 14 mock call sites to return `{ vital_readings: ... }`, matching the real contract shape.
- **New regression test added:** `renders actual rows (not just the total count) matching the real API response shape` — mocks `{ vital_readings: MOCK_VITALS, total_size: 2 }`, asserts both the `Page 1 of 1 (2 total)` count text AND actual `<td>` row content (`within(screen.getByRole('table'))` scoped, to avoid colliding with the identical option text in the type-filter `<select>`). Verified this test fails against the pre-fix code (`git stash` of just `Vitals.js` — 8/16 tests failed including this one) and passes against the fix.
- **Test results:** `npx react-scripts test src/pages/Health/Vitals --watchAll=false` → 16/16 passed.

---

## Domain Knowledge Refresh & Simplification Retrospective — 2026-07-06

Pre-v1.0 retrospective per the project owner's call to stop new development and simplify before going live. Documentation-only pass — re-read the entire current state of `application/domain/health/` (domain, ports, adapters), `application/flyway/health/`, `application/contract/health.yaml`, and `web/src/pages/Health/` against this file to find drift; no production code was changed.

**Confirmed correct / consistent (no action needed):**
- The `DoctorVisit.create()` 11→6 param `VisitDetails` refactor (see Key Design Decisions above) is complete and internally consistent — domain factory, `DoctorVisitService.create()`, `DoctorVisitTest.java`, and `DoctorVisitServiceTest.java` all agree on the new shape.
- The deprecated `quarkus.hibernate-orm.database.generation` property rename to `quarkus.hibernate-orm.schema-management.strategy` (a repo-wide fix by the quarkus-developer agent) is correctly in place in both `application/domain/health/adapters/src/main/resources/application.properties` and `.../src/test/resources/application.properties` (the `%integration-test` profile variant too).
- The health domain otherwise reads clean for a pre-production simplification pass: domain entities, ports, resources, and DTOs are small, have no speculative/unused abstractions, and no test suite is bloated relative to what it covers.

**Known, centrally-tracked issues confirmed present (not health-specific, not fixed here per instruction):**
- `VitalReadingService.validate()` and `DoctorVisitService.validateCreate()` both duplicate the validation rules already enforced by the domain factories' `IllegalArgumentException` checks — dead/shadowing logic since no `ExceptionMapper` exists for `IllegalArgumentException` today. A generic `ExceptionMapper<IllegalArgumentException>` has been proposed repo-wide (not yet implemented) to retire the adapter-layer duplicates in every domain, health included. Tracked centrally, not a health-specific gap.
- Health's adapter DB tests (`DoctorVisitPanacheRepositoryTest`, `VitalReadingPanacheRepositoryTest`, `VitalReadingCreateUpdateIT`) confirmed to use the shared-local-Postgres `%integration-test` config-profile pattern (`@QuarkusTest` + `@TestProfile` pointing at `DB_URL:jdbc:postgresql://localhost:5432/app_db`), not Testcontainers — consistent with the repo-wide finding that Testcontainers adoption (Q34/Q35) remains unimplemented everywhere despite being marked "resolved." Cross-cutting gap, not a new health-specific discovery.

**New drift/findings from this pass:** see the two `[New 2026-07-06]` entries at the top of Open Issues (orphaned `HealthProfile` frontend scaffold; `doctor_visit` VARCHAR length inconsistencies across DB/JPA/contract).

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
