# Wealth & Asset Management Domain

**Focus:** Financial liquidity, transaction ledgers, physical asset lifecycle, CQRS mathematical modeling, and core data normalization based on YAGNI/KISS principles.

## v0.1: Prototype (Minimal Features, Happy Path)

**Objective:** Validate core data ingestion, unified ledger storage (Write Model), and source traceability.
**Data Constraint:** Ephemeral. The database is expected to be wiped frequently.

### Epic 1: Local Banking Data & Ledger Ingestion

#### Use Case 1.1: Standardized Statement Upload & Traceability

* **Extraction:** The system must accurately extract the transaction date, amount, and type (Credit/Debit) from standard banking CSVs (Savings, Credit Card, Loan).
* **Upload Tracking:** Every ingested file must generate a unique upload record. All extracted transactions must link to this upload ID to enable cascading rollbacks if a file is parsed incorrectly.
* **Normalization:** All transaction amounts must be converted and stored as positive absolute values.
* **Type Classification:** If an uploaded CSV represents a debit as a negative number, the system must explicitly classify that row with a "Debit" transaction type flag upon storage.
* **Flexible Metadata (JSONB):** The system must utilize a unified ledger structure. Sparse or domain-specific data (e.g., loan terms, reference numbers, categorization) must be written to a flexible JSONB metadata column rather than rigid, isolated tables.
* **Configuration:** Account names must be driven by an external configuration file (e.g., `application.properties`), allowing the addition of new accounts without recompiling the application codebase.

## v0.2: Usable Local App (Usable Features)

**Objective:** Introduce native database integrity rules to support actual local usage and prevent ledger corruption.

### Epic 2: Ledger Integrity

#### Use Case 2.1: Deduplication Logic

* **Native Idempotency:** The database must enforce a unique constraint across core transaction attributes (Account ID, Date, Amount, Type, Description) to naturally reject overlapping statement ingestion.
* **Same-File Duplicates:** If a single uploaded file contains multiple genuinely identical transactions (matching date, amount, type, and description), the parser logic must explicitly handle these distinct physical events before hitting the unique constraint.
* **Cross-File Duplicates:** If a newly uploaded file contains a transaction that perfectly matches an existing record, the system must silently reject the duplicate transactions.

## v0.3: Enhanced Local App (More Features)

**Objective:** Expand parsing capabilities and introduce physical asset compliance.

### Epic 3: Investment Data Ingestion

#### Use Case 3.1: Investment CSV Parsing

* **Extraction:** The system must extract the transaction date, amount, and type from investment/mutual fund CSVs.
* **Metadata Processing:** The system must extract and seamlessly inject domain-specific metadata, specifically "Units" and "NAV" (Net Asset Value), into the transaction's JSONB column without altering the core database schema.

### Epic 4: Vehicle Asset Compliance

#### Use Case 4.1: Asset Lifecycle Tracking

* **Metadata Registry:** The system must store vehicle identifying details (Make, Model, Registration Number, Registration Type).
* **Compliance Deadlines:** The system must track and store recurring regulatory deadlines, specifically PUC (Pollution Under Control), Insurance expiry, and Road Tax renewals (including biennial BH-Series schedules).

## v0.4: Advanced Business Logic & Dashboard Generation (Read Model)

**Objective:** Implement the CQRS Read Model. Execute mathematical logic against the immutable ledger to output strict JSON state files for the dashboard.

### Epic 5: The Mathematical Engine & Zero Leakage

#### Use Case 5.1: "The Mahesh Summation Rule" (Zero Leakage)

* **Dynamic Header Summation:** Top-level metrics (e.g., Total Gross Assets, Net Worth, Current Liquidity) must be dynamically calculated directly from the sum of underlying ledger transactions.
* **Constraint Validation:** The mathematical model must flag a "Critical Failure" if a manual override is detected or if a parsed category header deviates by > ₹100 from its sub-items.

#### Use Case 5.2: EMI Arbitrage & Liquidity Monitoring

* **Offset Arbitrage Tracking:** The engine must continuously calculate the net benefit of parking liquidity in offset accounts (e.g., Maxgain) against outstanding home loan balances.
* **Prepayment vs. Wealth Building:** The system must monitor market return rates against loan interest rates (e.g., maintaining an aggressive 80k+ family SIP vs. a ~7.2% loan). It must trigger a prepayment alert only if the net arbitrage benefit falls below a defined threshold (e.g., 3-4%).
* **Safety Net Validation:** The engine must track structural liquidity to ensure ultimate safety nets (e.g., isolated Fixed Deposits, Gratuity) remain distinct and untouched by monthly operating cash flow calculations.

#### Use Case 5.3: Dynamic Triggers & Operating Limits

* **Reallocation Triggers:** The model must identify fixed temporal triggers (e.g., a loan closure date) stored in the JSONB metadata and automatically simulate the reallocation of the freed-up EMI capital toward subsequent wealth-building portfolios.
* **Operating Budget Cap:** The system must sum monthly household and discretionary DEBIT transactions, throwing an alert if the configured monthly expense boundary is breached, ensuring SIPs remain protected.

## v0.5: Error Handling (Unhappy Path)

**Objective:** Build system resilience to handle malformed data and edge cases.

### Epic 6: Malformed Data Rejection

#### Use Case 6.1: Missing Required Columns

* **Rejection Protocol:** If an uploaded file is missing identifiable date or amount columns, the system must reject the entire file.
* **Error Logging:** The system must explicitly log which required data points were missing to inform the user.

## v1.0: Security & Persistence

**Objective:** Lock down the application architecture for persistent, real-world data storage.

### Epic 7: Data Hardening

#### Use Case 7.1: Transition to Persistent Data

* **Migration Strategy:** The database is no longer treated as ephemeral. All database modifications must strictly utilize versioned schema migration tools (e.g., Flyway) to prevent data loss.
* **Encryption:** Sensitive financial ledgers must utilize encryption at rest.