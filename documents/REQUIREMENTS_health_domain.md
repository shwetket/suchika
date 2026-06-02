# Health & Biometrics Domain

**Focus:** Time-series biometric tracking, fitness profiles, and unstructured wellness data.

## v0.1: Prototype (Minimal Features, Happy Path)
**Objective:** Validate the document-database (MongoDB) connection and core biometric ingestion.
**Data Constraint:** Ephemeral. The database is expected to be wiped frequently.

### Epic 1: Manual Biometric Logging
#### Use Case 1.1: Core Metric Entry
* **Data Ingestion:** The system must allow the manual entry of fundamental biometric data, specifically Height and Weight.
* **Document Structure:** Each entry must be stored as an independent document containing a timestamp, the metric type, the numeric value, and the unit of measurement.

## v0.2: Usable Local App (Usable Features)
**Objective:** Enable logical retrieval and chronological sorting of unstructured data.

### Epic 2: Biometric History
#### Use Case 2.1: Time-Series Ledger
* **Chronological Retrieval:** The system must be able to query the document database and display biometric entries in strict chronological order for user review.

## v0.5: Beta Release (Cross-Domain Logic)
**Objective:** Introduce multi-tenancy by linking Health data to the Household Domain.

### Epic 3: Profile Association
#### Use Case 3.1: Multi-Tenant Tagging
* **Cross-Domain Reference:** The system must enforce that every biometric document is explicitly tagged with a valid `profile_id` referencing a registered individual from the Household Domain's family roster.
* **Data Isolation:** The system must securely filter biometric queries so a user only sees the health data associated with the specific profile they are requesting.

## v1.0: Security, External Sync & Persistence
**Objective:** Introduce external fitness APIs and transition to persistent, real-world data storage.

### Epic 4: Google Fit Integration (Manual Sync)
#### Use Case 4.1: User-Initiated Synchronization
* **Trigger Mechanism:** The synchronization of external health data must be strictly manual, requiring the user to explicitly click a "Sync Data" trigger. Automated background polling is prohibited.
* **Data Mapping:** The system must fetch data for a user-specified date range (e.g., "Last 7 Days") from the Google Fit API and map it to the internal biometric document structure.
* **Upsert Deduplication:** To prevent duplicate records during repeated syncs, the system must utilize database upsert operations keyed to a unique combination of `(profile_id, timestamp, metric)`.

#### Use Case 4.2: Strict Token Security
* **Token Volatility:** The system must only request short-lived access tokens from the Google OAuth consent flow.
* **Storage Prohibition:** The system is strictly forbidden from requesting, capturing, or storing offline access tokens or refresh tokens in the database.

### Epic 5: Medical Boundary Enforcement
#### Use Case 5.1: Scope Restriction
* **Exclusion Protocol:** The system must remain a strictly personal wellness tool. It must not include or accept requirements for medical diagnosis algorithms, symptom tracking workflows, or HIPAA-compliant data structures.
* **Migration Strategy:** The MongoDB database is no longer treated as ephemeral. Real data persistence rules apply.