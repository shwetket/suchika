# Household Domain State

## Objective

Document the planned design for the household domain so that v0.3 development can start without redesigning from scratch. Nothing is built yet — this file captures the intended schema, API contract, and the constraints agreed on before implementation begins.

## Use Cases

- Before starting v0.3 household work — read this file first; it defines the schema and the key constraints to follow
- When creating `application/contract/household.yaml` — the Planned API Contract section outlines the surface area
- After completing household milestones — update Implementation Status to reflect what's done

---

**Last updated:** 2026-06-20
**Version:** v0.3 planned — NOT started
**Port:** 8084

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Calendar Events | 🔲 v0.3 | |
| Inventory Items | 🔲 v0.3 | |
| Task Tracking | 🔲 v0.3 | Assign tasks to child profiles |
| Goals | 🔲 v0.3 | |
| Frontend — Calendar | 🔲 v0.3 | Stub page exists (`Calendar.js`) |
| Frontend — Inventory | 🔲 v0.3 | Stub page exists (`Inventory.js`) |

**Current state:** The Quarkus service starts on port 8084 with an empty schema. The frontend has stub pages that show "coming soon." No API contract exists yet for this domain.

---

## Planned Database Schema (`household` schema)

| Table | Planned Columns |
|---|---|
| `calendar_event` | `id UUID PK`, `profile_id UUID FK`, `title VARCHAR`, `start_date DATE`, `end_date DATE` (≥start_date), `master_event_id UUID nullable self-FK`, `event_type VARCHAR` |
| `inventory_item` | `id UUID PK`, `profile_id UUID FK`, `name VARCHAR`, `category VARCHAR`, `quantity NUMERIC`, `unit VARCHAR`, `source VARCHAR` (FLIPKART/INSTAMART/COUNTRY_DELIGHT/MANUAL), `imported_at TIMESTAMP` |
| `goal` | `id UUID PK`, `profile_id UUID FK`, `title VARCHAR`, `target_date DATE`, `status VARCHAR`, `assigned_to UUID FK→profile.profile(id)` |

Conflict detection rule: overlapping master events for the same profile are flagged (not blocked).

---

## Planned API Contract

File to create: `application/contract/household.yaml`
Base path: `/api/v1/household`
- Calendar events CRUD + conflict detection
- Inventory CRUD + batch import endpoint
- Goals CRUD

---

## Key Files (to create)

| Layer | Path |
|---|---|
| Domain | `application/domain/household/domain/src/main/java/com/suchika/household/domain/` |
| Ports | `application/domain/household/ports/` |
| Adapters | `application/domain/household/adapters/` |
| Flyway | `application/flyway/household/` (start at `V1__`) |
| Frontend | `web/src/pages/Household/` (Calendar.js, Inventory.js — currently stubs) |

---

## Key Design Decisions (to follow when building)

- Startup order: profile must run first (household Flyway migrations will reference `profile.profile`).
- Event types are VARCHAR, not SQL ENUM.
- `end_date >= start_date` enforced as DB CHECK constraint (business-rule check — kept in DB).
- Inventory import: raw ledger only in v0.3. No dedup or reconciliation until v0.4.
- Cross-domain task deadlines link to calendar events — defer to v0.3 design review.

---

## Open Issues / Blockers

- No API contract file yet — create `application/contract/household.yaml` before any backend work.
- Frontend stub pages need full implementation (Calendar.js, Inventory.js).
- No Flyway migrations exist — start fresh from `V1__create_calendar_event.sql`.
