# Profile Domain State

## Objective

Give any agent or developer instant context on the profile domain — what's built, the database schema, the API contract, and where the key files are. Profile is the identity anchor; read this before touching any other domain because every domain FKs into `profile.profile`.

## Use Cases

- Before working on profile backend or frontend — check Implementation Status and Key Files
- When adding a new domain table — confirm the `profile_id FK → profile.profile(id)` pattern
- After completing profile work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-06
**Version:** v0.2 complete — UAT-ready; Epic 8 Phase 4 delivered; v0.6 adapter/domain test coverage added; Setup Wizard + gateway/contract fixes added; v1.0 retrospective/simplification pass complete (2026-07-06)
**Port:** 8081

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Admin CRUD | ✅ Complete | Household manager anchor |
| Profile CRUD | ✅ Complete | All member operations |
| Deactivate profile | ✅ Complete | Soft delete, `is_active` flag |
| Profile list/filter | ✅ Complete | Filter by admin_id |
| Frontend page | ✅ Complete | `web/src/pages/Household/Profiles.js` |
| Admin policy settings | ✅ Complete | Epic 8 Phase 4 — `PATCH /v1/admins/{id}/policy`, merge semantics, JSONB column |
| Multi-admin support | 🔲 v1.1 | Single admin per household in v0.2 |

---

## Database Schema (`profile` schema)

| Table | Key Columns |
|---|---|
| `admin` | `id UUID PK`, `display_name VARCHAR(50)`, `email_address VARCHAR`, `is_active BOOLEAN`, `created_at TIMESTAMPTZ`, `policy_settings JSONB NOT NULL DEFAULT '{}'`, `UNIQUE (email_address)` (`uq_admin_email`) |
| `profile` | `id UUID PK`, `admin_id UUID FK→admin.id ON DELETE RESTRICT` (`fk_profile_admin`, **nullable in DB** — see Open Issues), `full_name VARCHAR(50)`, `dob DATE`, `relation_to_admin VARCHAR(30)`, `email_address VARCHAR`, `gender VARCHAR(30)`, `blood_type VARCHAR(10)`, `metadata JSONB NOT NULL DEFAULT '{}'` (**dead column, unused** — see Open Issues), `is_active BOOLEAN`; partial unique index `uq_admin_self_profile ON profile(admin_id) WHERE relation_to_admin = 'SELF'` |

Relation values (VARCHAR, no SQL ENUM, 9 total): `SELF`, `SPOUSE`, `CHILD`, `PARENT`, `PARENT_IN_LAW`, `SIBLING`, `GRANDPARENT`, `GRANDCHILD`, `OTHER` (`PARENT_IN_LAW`/`GRANDPARENT`/`GRANDCHILD` added pre-v0.6; this doc previously still listed the old 6-value set — fixed 2026-07-06, see Retrospective section).

Every other domain's tables hold `profile_id UUID REFERENCES profile.profile(id)`. No reverse FK from profile to other domains.

**2026-07-05 — Flyway consolidation bug fix.** `application/flyway/profile/V1__init_profile_consolidated.sql` (the single-file replacement for the old V1-V3 profile migrations) had silently dropped `fk_profile_admin`, `uq_admin_email`, and `uq_admin_self_profile`, and had widened `display_name`/`full_name` to `VARCHAR(150)` instead of narrowing to `VARCHAR(50)` — contradicting already-resolved product-owner decisions (`documents/OpenQuestions.md` Q31, Q44, Q46). Fixed in place (Q32 already approved overwriting the consolidated V1 pre-release). CHECK constraints (`chk_profile_relation`, `chk_profile_gender`, `chk_profile_blood_type`) were correctly left out — that removal is intentional per the same migration history, not a regression.

---

## API Contract

File: `application/contract/profile.yaml`
Base path: `/v1`
- `GET    /admins` — list all admins
- `POST   /admins` — create admin
- `GET    /admins/{id}` — get admin by id
- `PATCH  /admins/{id}` — update admin fields
- `DELETE /admins/{id}` — deactivate admin
- `PATCH  /admins/{id}/policy` — merge household policy thresholds into `policy_settings` JSONB (Epic 8 Phase 4)
- `GET    /profiles?admin_id=` — list profiles scoped to admin
- `POST   /profiles` — create profile (`admin_id` is in the request body, not a query param — full_name/dob/relation_to_admin required, email_address/gender/blood_type optional)
- `GET    /profiles/{id}` — get profile
- `PATCH  /profiles/{id}` — partial update (email_address, gender, blood_type, is_active only — full_name/dob/relation_to_admin/admin_id are immutable)
- `DELETE /profiles/{id}` — deactivate (soft delete)

Gateway proxy: `application/web-gateway/.../profile/ProfileGatewayResource.java` + `ProfileServiceClient.java` proxy all of the above at the same `/v1/...` paths (no separate prefix), including `POST /admins` and `PATCH /admins/{id}/policy` (added 2026-07-03 — previously missing, silently 500ing through the gateway).

---

## Key Files

| Layer | Path |
|---|---|
| Domain | `application/domain/profile/domain/src/main/java/com/suchika/profile/domain/` |
| Ports | `application/domain/profile/ports/src/main/java/com/suchika/profile/ports/` |
| Adapters | `application/domain/profile/adapters/src/main/java/com/suchika/profile/adapters/` |
| Flyway | `application/flyway/profile/` |
| Frontend | `web/src/pages/Household/Profiles.js` |
| API module | `web/src/api/profiles.js` |
| Tests | `web/src/pages/Household/Profiles.test.js` |

---

## Key Design Decisions

- `profile.admin` is the auth anchor for future OIDC (v1.0) — don't add auth logic here yet.
- `profile.profile` holds ALL household members. Admin is a separate concept from member.
- Profile deactivation is soft delete (`is_active = false`) — never hard delete.
- No cross-domain SQL joins — other domains reference `profile_id` but profile never queries them.

---

## Open Issues / v0.3+ Backlog

- Pagination on profile list (v0.3)
- Profile avatar/photo (v0.4 or later) — would need a new column/migration if picked up; the old speculative `metadata` column is gone (see below).
- Admin authentication (v1.0)
- ✅ **v0.5.1 (2026-07-08): dead `profile.profile.metadata JSONB` column removed.** Was flagged in the 2026-07-06 retrospective as never wired up anywhere (`ProfileEntity`, contract, DTOs, service code — zero consumers). Originally scheduled to be dropped via `V2__drop_unused_profile_metadata.sql`; per the v0.5.1 Flyway V2→V1 merge workstream that V2 was folded directly into `V1__init_profile_consolidated.sql` instead (column definition removed from the `CREATE TABLE profile.profile` statement) and the V2 file deleted. Profile domain is back to a single canonical V1 Flyway file. Required a full local `db-reset` (product-owner-approved second exception to the "never edit a committed migration" rule — see CLAUDE.md).
- 🔲 **New (2026-07-06 retrospective): "SELF profile of an active admin cannot be deactivated" is documented but not implemented.** `application/contract/profile.yaml`'s `DELETE /v1/profiles/{profile_id}` promises a `409 FailedPrecondition` for this case, but `ProfileService.deactivateProfile` and `ProfileService.updateProfile` (the `is_active=false` path) have no such guard — and two green tests in `ProfileServiceTest` (`deactivateProfile_setsActiveToFalse`, `updateProfile_setIsActiveFalse_deactivatesProfile`) explicitly construct a SELF profile of a freshly-created (active) admin and assert the deactivation *succeeds*. This is a real contract/code gap, not stale documentation noise. Needs a product decision: implement the guard (mirror `AdminService`'s existing `countActiveByAdminId` check) or remove the false guarantee from the contract.
- 🔲 **New (2026-07-06 retrospective): `profile.profile.admin_id` is nullable in the DB** despite being required on create (`CreateProfileRequest.admin_id`), immutable thereafter, FK'd to `admin.id`, and 400'd on null in `ProfileResource`. This contradicts the project's own DB-constraint philosophy ("keep structural `NOT NULL` in DB, enforced everywhere including direct DB access" — CLAUDE.md). Low urgency (app layer already enforces it) but worth tightening in a future migration for defense-in-depth on the identity-anchor table.
- 🔲 **Minor (2026-07-06 retrospective): `web/src/api/profiles.js`'s `getProfile` export has no production caller** — every page uses `listProfiles`; `getProfile` is only exercised by its own unit test. Candidate for removal, or leave if a single-profile fetch is imminently needed.
- ✅ **v0.6: `AdminResource` and `ProfileResource` HTTP adapter unit tests — COMPLETE (2026-07-03).** These had zero test coverage at any layer until a new ArchUnit rule (`ports_input_interfaces_must_be_referenced_by_a_test_class`, in `shared/`) was added and immediately flagged `AdminUseCase`/`ProfileUseCase` as unreferenced by any test class — the existing `AdminServiceTest`/`ProfileServiceTest` only ever referenced the concrete `*Service` implementation type, never the interface. Added `AdminResourceTest` (9 tests) and `ProfileResourceTest` (9 tests), both plain JUnit 5 unit tests with a stub use case, matching the `StatementUploadResourceTest` pattern.
- ✅ **v0.6: `Profile` and `Admin` domain entity unit tests — COMPLETE (2026-07-03).** `ProfileTest` (builder round-trip, default `active=false`, setters) and `AdminTest` (null-safe `policySettings` handling — no-arg constructor, full constructor with null map, `setPolicySettings(null)` — all default to an empty map rather than storing null).
- ✅ **Admin Setup Wizard (`/admin/setup`) — COMPLETE (2026-07-03).** New mandatory-then-revisitable onboarding page: Step 1 (mandatory — full_name/dob/blood_type, creates the Admin + their own SELF profile and wires `admin_id`/`profile_id` onto the auth session for the first time via a new `AuthContext.updateUser()`), Step 2 (optional — liquid accounts and loans via existing `createAccount`/`updateAccountClassification`), Step 3 (optional — WEIGHT/HEIGHT vitals via existing `recordVital`). Completion sets `policy_settings.setup_completed = "true"` (no new migration — reuses the Epic 8 Phase 4 JSONB column). A new `SetupGate` component wraps `/dashboard` (not the setup page itself, to avoid a redirect loop): admins with `setup_completed !== "true"` are redirected to `/admin/setup`; the page stays reachable afterward via a persistent "Household Setup" nav link for adding more accounts/loans later. Files: `web/src/pages/Admin/Setup.js`, `web/src/components/SetupGate.js`, `web/src/context/AuthContext.js` (`updateUser`).
- ✅ **Manual QA bug fixes — COMPLETE (2026-07-03), found while building the Setup Wizard:**
  - `ProfileService.createProfile` previously let a duplicate-SELF-profile-per-admin (`uq_admin_self_profile` unique index) surface as a raw HTTP 500 with a stack trace. Now pre-checked via a new `ProfileRepository.existsSelfProfile(adminId)` and thrown as `ConflictException` (409), matching the existing `AdminService.createAdmin` email-uniqueness pattern.
  - `application/contract/profile.yaml`'s `CreateProfileRequest`/`UpdateProfileRequest` schemas and the `/v1/profiles/{id}` method (was `PUT`, code is `PATCH`) were stale relative to the real `ProfileResource.java` — fixed to match the code (the internal mirror at `application/web-gateway/src/main/resources/profile.yaml` was already correct; only the canonical contract had drifted).
  - Gateway's `ClientErrorMapper` was a global `@Provider ExceptionMapper<WebApplicationException>`, so it also intercepted RESTEasy's own routing exceptions (unmapped paths, `/q/health`) and threw a `ProcessingException` trying to read a non-existent entity, surfacing as an unrelated 500. Guarded with `hasEntity()` + try/catch.
  - Gateway was missing `POST /v1/admins` and `PATCH /v1/admins/{id}/policy` proxy routes entirely (`ProfileGatewayResource`/`ProfileServiceClient`) — both already used by the existing `PolicySettings.js` page, which was silently broken through the gateway. Added, plus documented all Profile/Admin gateway routes in `application/contract/gateway.yaml` (previously a dangling "Household Profiles" tag with zero paths). Deferred: the same gap for `/v1/vitals`/`/v1/doctor-visits` in gateway.yaml, and the stale `application/web-gateway/src/main/resources/gateway.yaml` mirror — pre-existing, unrelated to this feature.

## Design Decisions (Epic 8 Phase 4)

- `policy_settings` stored as `JSONB NOT NULL DEFAULT '{}'` on `profile.admin` — no CHECK constraint; keys validated at application layer only.
- Serialisation/deserialisation inlined in `AdminEntity` as two private static methods (Jackson `ObjectMapper`) — no cross-domain import from `com.suchika.wealth.adapters.persistence.JsonbMetadataUtil`.
- Merge semantics: `PATCH /policy` merges provided keys into the existing map; null values in the request body skip overwriting the existing value; missing keys are preserved untouched.
- `AdminUseCase.updatePolicySettings(UUID, Map<String,String>)` added to port interface; implemented in `AdminService`; exposed via `AdminResource` at `PATCH /v1/admins/{admin_id}/policy`.
- `AdminResponse` now always includes `policy_settings` (empty map `{}` when none set).

---

## Integration Test Coverage — 2026-07-03 (QA pass)

- ✅ **`SetupWizardIT` added** (`application/domain/profile/adapters/src/test/java/com/suchika/profile/adapters/http/SetupWizardIT.java`) — true `@QuarkusTest` + real Postgres integration test covering the full Setup Wizard backend flow in one test: `POST /v1/admins` -> `POST /v1/profiles` (relation SELF) -> `PATCH /v1/admins/{id}/policy`, using the real, DI-wired `AdminService`/`ProfileService` and real Panache repositories (not the stub use-case pattern `AdminResourceTest`/`ProfileResourceTest` use). Also covers the duplicate-SELF-profile -> 409 `ConflictException` path (`ProfileService.existsSelfProfile` check). Compile-verified clean (`./gradlew :application:domain:profile:adapters:compileTestJava`); **not executed** this session — see `documents/OpenQuestions.md` Q34 for why (the module's `%test` Flyway profile points at the same live shared dev Postgres the developer was manually testing against, and would TRUNCATE CASCADE via `R__seed_profile_test_data.sql` on startup).
- Follows the direct-resource-construction convention already used by `ProfileResourceTest`/`AdminResourceTest` (construct the real `@Path` resource class by hand, call its methods directly) rather than RestAssured — this module has no `rest-assured` test dependency (see Q38).

## Flyway Consolidation Fix — 2026-07-05

- ✅ **Migration bug fix** (`application/flyway/profile/V1__init_profile_consolidated.sql`): restored `fk_profile_admin` (profile.profile.admin_id → profile.admin.id, `ON DELETE RESTRICT`), `uq_admin_email` (table `UNIQUE` on admin.email_address), and `uq_admin_self_profile` (partial unique index, one SELF profile per admin); narrowed `admin.display_name` and `profile.full_name` from the wrongly-widened `VARCHAR(150)` to the resolved `VARCHAR(50)` (Q44). CHECK constraints intentionally not restored (already-accepted enum-to-contract policy, Q31/V2 history).
- ✅ **Fallout fix (discovered, not in the original bug report):** `ProfileEntity.fullName` and `AdminEntity.displayName` (`application/domain/profile/adapters/src/main/java/com/suchika/profile/adapters/persistence/`) had `@Column(length = 150)`, and `application/contract/profile.yaml`'s `CreateAdminRequest`/`UpdateAdminRequest.display_name` had `maxLength: 150`. Left at 150 after narrowing the DB column to 50, this would have failed Hibernate schema validation at startup (`quarkus.hibernate-orm.database.generation=validate` in `application.properties` — a real boot-breaking mismatch, not just a style issue). Narrowed both to 50 to match. Note: at the time this was written, `application/web-gateway/src/main/resources/profile.yaml` still had stale `maxLength: 150` (4 occurrences) and no `maxLength` on `full_name` at all — **superseded 2026-07-06:** the full contract resynthesis (see Retrospective section below) rewrote both the canonical contract and the gateway mirror from the real Java code, and both now correctly show `maxLength: 50` at all 4 occurrences, verified identical via `diff`. This line is left in place for history; the gap it describes no longer exists.
- **Test results:** `:application:domain:profile:domain:test` — 13/13 pass (`AdminTest`, `BloodTypeTest`, `ProfileTest`). `:application:domain:profile:adapters:test` — 47/49 pass; `SetupWizardIT` (2 tests) failed to boot with `FlywayValidateException: Migration checksum mismatch for migration version 1` — **expected**, not a regression: editing an already-applied V1 migration in place invalidates its recorded checksum on whatever local Postgres previously ran the old content, and Q32 already accepted "requires manual dev DB resets" as the consequence of in-place V1 edits during this consolidation. Run `db-reset` (or `db-reset -Force`) then restart profile to clear it — not performed here since it also wipes wealth/health/household schemas in the shared local `app_db`, out of scope for a profile-only fix.

## Retrospective / Simplification Pass — 2026-07-06

Ahead of v1.0 planning — no new feature work, pure audit of `domain/`, `ports/`, `adapters/`, Flyway, contract, and frontend against this doc.

- ✅ **Confirmed: the `RelationToAdmin` contract resynthesis is fully consistent.** `application/contract/profile.yaml` and its mirror `application/web-gateway/src/main/resources/profile.yaml` are **byte-identical** (verified via `diff`) — both list the correct 9-value enum (`SELF, SPOUSE, CHILD, PARENT, PARENT_IN_LAW, SIBLING, GRANDPARENT, GRANDCHILD, OTHER`), matching `RelationToAdmin.java` exactly. The frontend (`web/src/pages/Household/Profiles.js` `RELATIONS` array) was already correct at 9 values and was never behind. Only this domain-state doc still listed the old 6-value set — fixed above. As a side effect, this also fully superseded the `maxLength: 150` gateway-mirror drift noted in the 2026-07-05 entry above (now 50 everywhere, verified).
- New issues found and logged in Open Issues above: dead `metadata` JSONB column on `profile.profile` (zero code consumers), missing enforcement of the contract's "SELF profile of an active admin cannot be deactivated" rule (contradicted by two currently-green tests), `admin_id` nullable in DB despite being required/immutable everywhere else, and an unused `getProfile` export in the frontend API module.
- **Checked and found clean (no action needed):** `AdminEntity`/`ProfileEntity` JPA mappings are otherwise minimal and 1:1 with their domain classes (the `metadata` column above is the one exception); `AdminDao`/`ProfileDao` are the standard thin Panache pass-through (not speculative abstraction); `AdminService`/`ProfileService`/`AdminResource`/`ProfileResource` have no empty catches, raw types, or magic numbers beyond the `Response.status(201)` literal, which was checked against wealth/health/household resources and confirmed to be an intentional, consistent repo-wide convention (profile is its origin, not an outlier); all `AppLogger`/`shared/exception` usage is consistent with the rest of the codebase; documented test counts (13 domain, 49 adapter incl. 2 known-failing `SetupWizardIT`) reconcile exactly against actual `@Test` counts in the test source.
