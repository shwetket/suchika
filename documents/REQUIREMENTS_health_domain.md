# Health & Biometrics Domain

| | |
|---|---|
| **Type** | Requirements |
| **Audience** | Developers, product |
| **Status** | Active |
| **Last updated** | 2026-07-12 (v0.5 Epic 4 marked DONE; added v0.5/pre-v1.0 delivered items not originally scoped here) |

## Objective

Define all functional requirements, epics, and acceptance criteria for the Health domain across every version milestone. This is the single source of truth for what the health service must do — vital readings, doctor visits, and future external fitness integrations.

## Use Cases

- Before implementing any health feature — verify its milestone and acceptance criteria here
- When writing a new epic or use case — append to the correct version section
- When reviewing a PR for the health domain — check that delivered behaviour matches the criteria marked `[DONE]`

---

**Focus:** Time-series biometric tracking, medical visit records, and personal wellness data.

## v0.1: Prototype (Minimal Features, Happy Path) [DONE]

**Objective:** Validate core biometric ingestion and foundational data model.
**Data Constraint:** Ephemeral. The database is expected to be wiped frequently.

### Epic 1: Manual Biometric Logging [DONE]

#### Use Case 1.1: Core Metric Entry

* **Data Ingestion:** The system must allow the manual entry of biometric vital readings. Each reading must record the metric type, the numeric value, and the timestamp of the observation. [DONE]
* **Storage:** Each vital reading is stored as an independent record in the `health.vital_reading` table, linked to a `profile_id`. [DONE]

## v0.2: Usable Local App (Usable Features) [COMPLETE]

**Objective:** Deliver a fully usable health tracking module covering vitals and medical visits for local UAT.

### Epic 2: Biometric Vital Readings [DONE]

#### Use Case 2.1: Full Vital Type Coverage

* **Supported Vital Types:** The system must accept readings for the following metric types: WEIGHT, HEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR_FASTING, BLOOD_SUGAR_PP, HEART_RATE, TEMPERATURE, OXYGEN_SATURATION, BMI, WAIST_CIRCUMFERENCE. [DONE]
* **Log a Reading:** The system must allow logging a new vital reading for a given `profile_id`, metric type, value, unit, and observation timestamp. [DONE]
* **List Readings:** The system must list all vital readings for a given `profile_id`. The list must be filterable by vital type. [DONE]
* **Delete a Reading:** The system must allow deletion of a single vital reading by its ID, scoped to the owning `profile_id`. [DONE]
* **Member Scoping:** All vital reading records are owned by a household member. Every create, list, and delete operation must be scoped to a valid `profile_id`. [DONE]

### Epic 3: Doctor Visits [DONE]

#### Use Case 3.1: Medical Visit Record

* **Create a Visit:** The system must allow creation of a doctor visit record with the following fields: from_date, to_date, visited_doctor (boolean flag), doctor_name, hospital_name, speciality, symptoms, diagnosis, notes, and follow_up_date. [DONE]
* **Conditional Constraint:** If visited_doctor is TRUE, then doctor_name must be provided. [DONE]
* **List Visits:** The system must list all doctor visit records for a given `profile_id`. [DONE]
* **Update a Visit:** The system must allow updating any field of an existing visit record, scoped to the owning `profile_id`. [DONE]
* **Delete a Visit:** The system must allow deletion of a single visit record by its ID, scoped to the owning `profile_id`. [DONE]
* **Member Scoping:** All visit records are owned by a household member. Every create, list, update, and delete operation must be scoped to a valid `profile_id`. [DONE]

## v0.5: Beta Release (Cross-Domain Logic) [DONE]

**Objective:** Introduce read-only composite dashboard views that include health signals alongside other domain data.

### Epic 4: Biometric Streak Alerting (Cross-Domain Dashboard) [DONE — 2026-07-02]

#### Use Case 4.1: Logging Gap Detection [DONE]

* **Streak Monitoring:** The system must be capable of identifying gaps in vital logging for a given `profile_id` and surfacing these gaps as alerts on the consolidated action center dashboard. [DONE — `ProjectionCalculationEngine.computeActionCenterAlerts()`, tracked 3 core vital types (WEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR_FASTING) with a 30-day gap threshold, evaluated per-profile. See `documents/domain-state/health.md`.]
* **Cross-Domain Rule:** This feature is only permitted from v0.5 onward. Any implementation before v0.5 violates the domain isolation constraint.

### Delivered but not originally scoped in this document (added 2026-07-12)

* **`PATCH /v1/vitals/{id}`** (v0.5 Phase 0) — partial update of a vital reading; `vital_type`/`profile_id` remain immutable. [DONE]
* **Pagination** (pre-v1.0 Q54 pass) — `GET /v1/vitals` and `GET /v1/doctor-visits` both support `page`/`size` (0-indexed, default 50, max 200), mirroring the pattern used across every other domain's list endpoints. [DONE]

## v1.0: Security & Persistence

**Objective:** Introduce external fitness APIs and transition to persistent, real-world data storage.

### Epic 5: Google Fit Integration (Manual Sync)

#### Use Case 5.1: User-Initiated Synchronization

* **Trigger Mechanism:** The synchronization of external health data must be strictly manual, requiring the user to explicitly click a "Sync Data" trigger. Automated background polling is prohibited.
* **Data Mapping:** The system must fetch data for a user-specified date range (e.g., "Last 7 Days") from the Google Fit API and map it to the internal vital reading structure.
* **Upsert Deduplication:** To prevent duplicate records during repeated syncs, the system must utilize database upsert operations keyed to a unique combination of `(profile_id, timestamp, metric_type)`.

#### Use Case 5.2: Strict Token Security

* **Token Volatility:** The system must only request short-lived access tokens from the Google OAuth consent flow.
* **Storage Prohibition:** The system is strictly forbidden from requesting, capturing, or storing offline access tokens or refresh tokens in the database.

### Epic 6: Medical Boundary Enforcement

#### Use Case 6.1: Scope Restriction

* **Exclusion Protocol:** The system must remain a strictly personal wellness tool. It must not include or accept requirements for medical diagnosis algorithms, symptom tracking workflows, or HIPAA-compliant data structures.
* **Migration Strategy:** The database is no longer treated as ephemeral. Real data persistence rules apply.
