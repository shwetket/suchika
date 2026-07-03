# Profile Domain State

## Objective

Give any agent or developer instant context on the profile domain — what's built, the database schema, the API contract, and where the key files are. Profile is the identity anchor; read this before touching any other domain because every domain FKs into `profile.profile`.

## Use Cases

- Before working on profile backend or frontend — check Implementation Status and Key Files
- When adding a new domain table — confirm the `profile_id FK → profile.profile(id)` pattern
- After completing profile work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-03
**Version:** v0.2 complete — UAT-ready; Epic 8 Phase 4 delivered; v0.6 adapter/domain test coverage added; Setup Wizard + gateway/contract fixes added
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
| `admin` | `id UUID PK`, `display_name VARCHAR(150)`, `email_address VARCHAR`, `is_active BOOLEAN`, `created_at TIMESTAMPTZ`, `policy_settings JSONB NOT NULL DEFAULT '{}'` |
| `profile` | `id UUID PK`, `admin_id UUID FK→admin.id`, `full_name VARCHAR(150)`, `dob DATE`, `relation_to_admin VARCHAR(30)`, `email_address VARCHAR`, `gender VARCHAR(30)`, `blood_type VARCHAR(10)`, `is_active BOOLEAN` |

Relation values (VARCHAR, no SQL ENUM): `SELF`, `SPOUSE`, `CHILD`, `PARENT`, `SIBLING`, `OTHER`

Every other domain's tables hold `profile_id UUID REFERENCES profile.profile(id)`. No reverse FK from profile to other domains.

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
- Profile avatar/photo (v0.4 or later)
- Admin authentication (v1.0)
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
