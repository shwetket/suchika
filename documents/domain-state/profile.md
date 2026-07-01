# Profile Domain State

## Objective

Give any agent or developer instant context on the profile domain — what's built, the database schema, the API contract, and where the key files are. Profile is the identity anchor; read this before touching any other domain because every domain FKs into `profile.profile`.

## Use Cases

- Before working on profile backend or frontend — check Implementation Status and Key Files
- When adding a new domain table — confirm the `profile_id FK → profile.profile(id)` pattern
- After completing profile work — update Implementation Status and Open Issues

---

**Last updated:** 2026-07-01
**Version:** v0.2 complete — UAT-ready; Epic 8 Phase 4 delivered
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
| `profile` | `id UUID PK`, `admin_id UUID FK→admin.id`, `name VARCHAR`, `relation VARCHAR`, `date_of_birth DATE`, `is_active BOOLEAN` |

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
- `POST   /profiles?admin_id=` — create profile
- `GET    /profiles/{id}` — get profile
- `PUT    /profiles/{id}` — update profile
- `DELETE /profiles/{id}` — deactivate (soft delete)

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

## Design Decisions (Epic 8 Phase 4)

- `policy_settings` stored as `JSONB NOT NULL DEFAULT '{}'` on `profile.admin` — no CHECK constraint; keys validated at application layer only.
- Serialisation/deserialisation inlined in `AdminEntity` as two private static methods (Jackson `ObjectMapper`) — no cross-domain import from `com.suchika.wealth.adapters.persistence.JsonbMetadataUtil`.
- Merge semantics: `PATCH /policy` merges provided keys into the existing map; null values in the request body skip overwriting the existing value; missing keys are preserved untouched.
- `AdminUseCase.updatePolicySettings(UUID, Map<String,String>)` added to port interface; implemented in `AdminService`; exposed via `AdminResource` at `PATCH /v1/admins/{admin_id}/policy`.
- `AdminResponse` now always includes `policy_settings` (empty map `{}` when none set).
