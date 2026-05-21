# Profile Domain State

## Objective

Give any agent or developer instant context on the profile domain — what's built, the database schema, the API contract, and where the key files are. Profile is the identity anchor; read this before touching any other domain because every domain FKs into `profile.profile`.

## Use Cases

- Before working on profile backend or frontend — check Implementation Status and Key Files
- When adding a new domain table — confirm the `profile_id FK → profile.profile(id)` pattern
- After completing profile work — update Implementation Status and Open Issues

---

**Last updated:** 2026-06-20
**Version:** v0.2 complete — UAT-ready
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
| Multi-admin support | 🔲 v1.1 | Single admin per household in v0.2 |

---

## Database Schema (`profile` schema)

| Table | Key Columns |
|---|---|
| `admin` | `id UUID PK`, `username VARCHAR UNIQUE`, `email VARCHAR`, `created_at` |
| `profile` | `id UUID PK`, `admin_id UUID FK→admin.id`, `name VARCHAR`, `relation VARCHAR`, `date_of_birth DATE`, `is_active BOOLEAN` |

Relation values (VARCHAR, no SQL ENUM): `SELF`, `SPOUSE`, `CHILD`, `PARENT`, `SIBLING`, `OTHER`

Every other domain's tables hold `profile_id UUID REFERENCES profile.profile(id)`. No reverse FK from profile to other domains.

---

## API Contract

File: `application/contract/profile.yaml`
Base path: `/api/v1`
- `GET  /admins` — list all admins
- `POST /admins` — create admin
- `GET  /admins/{id}` — get admin by id
- `GET  /profiles?admin_id=` — list profiles scoped to admin
- `POST /profiles?admin_id=` — create profile
- `GET  /profiles/{id}` — get profile
- `PUT  /profiles/{id}` — update profile
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
