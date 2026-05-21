# Health Domain State

**Last updated:** 2026-06-20
**Version:** v0.2 complete — UAT-ready
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

File: `application/contract/health.yaml`
Base path: `/api/v1/health`
- `GET    /vitals?profile_id=&vital_type=`
- `POST   /vitals?profile_id=`
- `GET    /vitals/{id}`
- `DELETE /vitals/{id}`
- `GET    /visits?profile_id=`
- `POST   /visits?profile_id=`
- `GET    /visits/{id}`
- `PUT    /visits/{id}`
- `DELETE /visits/{id}`

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
