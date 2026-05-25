# Wealth & Asset Management Domain

**Focus:** Financial liquidity, transaction ledgers, physical asset lifecycle, and core data normalization.

## v0.1: Prototype (Minimal Features, Happy Path)
**Objective:** Validate core data ingestion and relational storage using perfectly formatted inputs.
**Data Constraint:** Ephemeral. The database is expected to be wiped frequently.

### Epic 1: Local Banking Data Ingestion
#### Use Case 1.1: Standardized Statement Upload
* **Extraction:** The system must accurately extract the transaction date, amount, and type (Credit/Debit) from standard banking CSVs (Savings, Credit Card, Loan).
* **Normalization:** All transaction amounts must be converted and stored as positive absolute values. 
* **Type Classification:** If an uploaded CSV represents a debit as a negative number, the system must explicitly classify that row with a "Debit" transaction type flag upon storage.
* **Configuration:** Account names must be driven by an external configuration file (e.g., `application.properties`), allowing the addition of new accounts without recompiling the application codebase.

## v0.2: Usable Local App (Usable Features)
**Objective:** Introduce logical data integrity rules to support actual local usage.

### Epic 2: Ledger Integrity
#### Use Case 2.1: Deduplication Logic
* **Same-File Duplicates:** If a single uploaded file contains multiple identical transactions (matching date, amount, and type), the system must process and store all of them as distinct, valid events.
* **Cross-File Duplicates:** If a newly uploaded file contains a transaction that perfectly matches an existing record already stored in the database, the system must reject the duplicate transactions from the new upload.

## v0.3: Enhanced Local App (More Features)
**Objective:** Expand parsing capabilities and introduce physical asset compliance.

### Epic 3: Investment Data Ingestion
#### Use Case 3.1: Investment CSV Parsing
* **Extraction:** The system must extract the transaction date, amount, and type from investment/mutual fund CSVs.
* **Metadata Processing:** The system must extract and store domain-specific metadata, specifically "Units" and "NAV" (Net Asset Value), attached to the core transaction record.

### Epic 4: Vehicle Asset Compliance
#### Use Case 4.1: Asset Lifecycle Tracking
* **Metadata Registry:** The system must store vehicle identifying details (Make, Model, Registration Number, Registration Type).
* **Compliance Deadlines:** The system must track and store recurring regulatory deadlines, specifically PUC (Pollution Under Control), Insurance expiry, and Road Tax renewals (including biennial BH-Series schedules).

## v0.4: Error Handling (Unhappy Path)
**Objective:** Build system resilience to handle malformed data and edge cases.

### Epic 5: Malformed Data Rejection
#### Use Case 5.1: Missing Required Columns
* **Rejection Protocol:** If an uploaded file is missing identifiable date or amount columns, the system must reject the entire file.
* **Error Logging:** The system must explicitly log which required data points were missing to inform the user.

## v1.0: Security & Persistence
**Objective:** Lock down the application architecture for persistent, real-world data storage.

### Epic 6: Data Hardening
#### Use Case 6.1: Transition to Persistent Data
* **Migration Strategy:** The database is no longer treated as ephemeral. All database modifications must strictly utilize versioned schema migration tools (e.g., Flyway) to prevent data loss.
* **Encryption:** Sensitive financial ledgers must utilize encryption at rest.