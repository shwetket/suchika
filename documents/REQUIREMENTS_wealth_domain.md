# Wealth & Asset Management Domain

| | |
|---|---|
| **Type** | Requirements |
| **Audience** | Developers, product |
| **Status** | Active |
| **Last updated** | 2026-06-30 |

## Objective

Define all functional requirements, epics, and acceptance criteria for the Wealth domain across every version milestone. This is the single source of truth for what the wealth service must do — accounts, transactions, CSV uploads, physical assets, and the CQRS read model.

## Use Cases

- Before implementing any wealth feature — verify its milestone and acceptance criteria here
- When writing a new epic or use case — append to the correct version section
- When reviewing a PR for the wealth domain — check that delivered behaviour matches the criteria marked `[DONE]`

---

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

## v0.2: Usable Local App (Usable Features) [COMPLETE]

**Objective:** Introduce native database integrity rules and deliver a fully usable financial ledger for local UAT.

### Epic 2: Account Management [DONE]

#### Use Case 2.1: Financial Account Registry

* **Account Creation:** The system must allow the creation of a financial account with a name, account type, and owning `profile_id`. [DONE]
* **Account Types:** Supported account types are: SAVINGS, CURRENT, CREDIT_CARD, HOME_LOAN, PERSONAL_LOAN, INVESTMENT, FD. [DONE]
* **Account Listing:** The system must list all accounts for a given `profile_id`. The list must be filterable by account type and by active/inactive status. [DONE]
* **Account View:** The system must return the full details of a single account by its ID, scoped to the owning `profile_id`. [DONE]
* **Account Update:** The system must allow updating the name and metadata of an account. [DONE]
* **Account Deactivation:** The system must allow deactivating an account. A deactivated account remains in the ledger and is not deleted. [DONE]
* **Member Scoping:** Every account record is owned by a household member. All create, list, view, and update operations must be scoped to a valid `profile_id`. [DONE]

### Epic 3: Transaction Ledger [DONE]

#### Use Case 3.1: Transaction Retrieval

* **Transaction Listing:** The system must list all transactions linked to a given account and `profile_id`. [DONE]
* **Date Range Filter:** The list must be filterable by a from-date and to-date range. [DONE]
* **Type Filter:** The list must be filterable by transaction type: CREDIT or DEBIT. [DONE]

### Epic 4: Statement Upload Lifecycle [DONE]

#### Use Case 4.1: CSV Upload and Parsing

* **Upload Trigger:** The system must accept a CSV file upload for a specified account and `profile_id`. [DONE]
* **Upload Record:** Every upload must create a statement upload record with a unique ID and an initial status of PENDING. [DONE]
* **Status Transitions:** The upload status must transition from PENDING to SUCCESS upon complete parsing, or to FAILED if parsing cannot be completed. [DONE]
* **Transaction Linking:** All transactions extracted from an upload must carry the upload ID as their source reference. [DONE]
* **Rollback:** The system must allow rollback of a specific upload, which deletes all transactions linked to that upload's ID. [DONE]

### Epic 5: Ledger Integrity [DONE]

#### Use Case 5.1: Deduplication Logic

* **Native Idempotency:** The database must enforce a unique constraint across core transaction attributes (Account ID, Date, Amount, Type, Description) to naturally reject overlapping statement ingestion. [DONE]
* **Same-File Duplicates:** If a single uploaded file contains multiple genuinely identical transactions (matching date, amount, type, and description), the parser logic must explicitly handle these distinct physical events before hitting the unique constraint. [DONE]
* **Cross-File Duplicates:** If a newly uploaded file contains a transaction that perfectly matches an existing record, the system must silently reject the duplicate transactions. [DONE]

## v0.3: Enhanced Local App (More Features)

**Objective:** Expand parsing capabilities and introduce physical asset compliance.

### Epic 6: Investment Data Ingestion

#### Use Case 6.1: Investment CSV Parsing

* **Extraction:** The system must extract the transaction date, amount, and type from investment/mutual fund CSVs.
* **Metadata Processing:** The system must extract and seamlessly inject domain-specific metadata, specifically "Units" and "NAV" (Net Asset Value), into the transaction's JSONB column without altering the core database schema.

### Epic 7: Vehicle Asset Compliance

#### Use Case 7.1: Asset Lifecycle Tracking

* **Metadata Registry:** The system must store vehicle identifying details (Make, Model, Registration Number, Registration Type).
* **Compliance Deadlines:** The system must track and store recurring regulatory deadlines, specifically PUC (Pollution Under Control), Insurance expiry, and Road Tax renewals (including biennial BH-Series schedules).

## v0.4: Advanced Business Logic & Dashboard Generation (Read Model)

**Objective:** Implement the CQRS Read Model. Execute mathematical logic against the immutable ledger to output strict JSON state files for the dashboard.

**Status note (2026-06-30):** Epic 8 was not implemented at v0.4 — the milestone was executed as an error-handling sprint instead (see `ROADMAP.md`, v0.4 section, and `OpenQuestions.md` Q8). The use cases below are the corrected, expanded definition of Epic 8, carried forward to the milestone confirmed by the product owner. They replace the three short use cases previously listed here. Until the product owner answers `OpenQuestions.md` Q8/Q13–Q20, these remain wealth-domain-only requirements not yet scheduled into a sprint.

**Domain boundary note:** Every use case in this epic operates exclusively on data already owned by the wealth domain (`account`, `transaction`, `physical_asset`, and the `metadata JSONB` field on `account`). No use case in this epic reads from `household`, `health`, or `profile` beyond the existing `profile_id` ownership scope. This keeps Epic 8 a single-domain epic, compliant with the "no cross-domain features before v0.5" rule (`REQUIREMENTS_cross_domain.md`). Where a calculation in the product owner's reference tooling depends on data this domain does not own (e.g., household income), the use case below explicitly excludes it and routes it to manual entry or to `OpenQuestions.md` instead of silently assuming a cross-domain read.

### Epic 8: The Mathematical Engine & Zero Leakage

#### Use Case 8.1: "The Mahesh Summation Rule" (Zero Leakage)

* **Dynamic Header Summation:** Top-level metrics (e.g., Total Gross Assets, Net Worth, Current Liquidity) must be dynamically calculated directly from the sum of underlying ledger transactions and active `physical_asset` records — never from a manually entered total.
* **Asset Categorization:** Every `account` and `physical_asset` record must resolve to exactly one asset category (e.g., real estate, financial investment, precious metal, vehicle, cash/bank) for summation purposes. Category is derived from `account.type` / `physical_asset.type` and, where the type alone is ambiguous (e.g., an INVESTMENT account holding SIP units vs. a PF account), from a category value stored in the account's `metadata JSONB` field. Category values are enum discriminators (per architecture rules) — not free text.
* **Category Subtotal Reconciliation:** For each asset category, the engine must compute a subtotal as the sum of its member records. This subtotal is the category's only source of truth — there is no separate manually entered category header value to reconcile against in this domain (unlike the product owner's source MD file, where category headers are manually typed and checked against line items). The "zero leakage" check in this system instead validates internal consistency: every active account/asset belongs to exactly one category and is counted exactly once.
* **Constraint Validation:** The mathematical model must flag a "Critical Failure" validation result if any account or physical asset cannot be resolved to a category, or if the same record is counted in more than one category subtotal during a single calculation pass.
* **Liquidity Tiering:** Each account and physical asset must resolve to one liquidity tier: 0–3 months, 3–12 months, 1–5 years, or 5+ years. Tier is derived from `account.type` (e.g., SAVINGS/CURRENT → 0–3 months; FD → tier based on maturity metadata; HOME_LOAN-linked offset balances → 0–3 months) and, where ambiguous, from `metadata JSONB`. The engine must compute a total liquid net worth figure per tier.
* **Purpose Grouping:** Each account/asset must resolve to one purpose tag (e.g., emergency fund, retirement corpus, growth investment, income generation, education, long-term reserve), sourced from `metadata JSONB`. This is a one-time manual classification per account (see manual-entry list below), not a derived value — the engine consumes it but does not compute it.
* **Growth Projection:** The engine must compute a weighted-average growth projection at 5-year and 10-year horizons across all financial investment accounts, using a per-account expected-return-rate assumption stored in `metadata JSONB` (manual entry — see below). Accounts without a stored expected-return-rate are excluded from the projection and flagged in the validation report (Use Case 8.4) rather than defaulted to zero or assumed.

#### Use Case 8.2: EMI Arbitrage & Liquidity Monitoring

* **Loan Metadata (Manual, One-Time):** Each loan account (`HOME_LOAN`, `PERSONAL_LOAN`) must carry, in `metadata JSONB`: original principal disbursed, loan start date, original tenure in months, and interest rate. These are one-time facts set at loan origination and updated only when the bank changes the rate — they are not derivable from transaction history and must remain manual entry.
* **EMI Principal/Interest Split (Derived):** The engine must derive the EMI principal/interest split per repayment period from the loan metadata (principal, rate, tenure) using standard amortization, cross-checked against the actual DEBIT transactions recorded against the loan account. The split must not be hardcoded or manually entered per period.
* **Outstanding Balance (Derived):** Current outstanding principal must be calculated as original principal minus the sum of all principal components of EMI payments recorded to date — not entered manually and not read from a bank-reported field, since no such field exists in transaction data.
* **Tenure Tracking (Derived):** Elapsed tenure, remaining tenure, and estimated closing date must be calculated from the loan start date (manual metadata) and the count/cadence of recorded EMI transactions.
* **Offset Arbitrage Tracking:** Where a loan's `metadata JSONB` designates a linked offset/MaxGain account, the engine must continuously calculate the net interest benefit of the linked account's balance against the loan's outstanding balance and current interest rate.
* **Prepayment vs. Wealth Building:** The engine must compare the loan's interest rate (from metadata) against a configured expected-market-return-rate assumption (manual entry — a policy choice, not derivable from transactions) and trigger a prepayment-vs-invest advisory alert when the net arbitrage margin falls below a configured threshold. The threshold value is manual, one-time policy configuration.
* **Safety Net Validation:** The engine must track structural liquidity such that accounts/assets tagged with purpose `EMERGENCY_FUND` or asset type `GRATUITY`-equivalent (via `metadata JSONB` purpose tag) remain excluded from monthly operating cash flow and liquidity-coverage calculations, regardless of their liquidity tier.

#### Use Case 8.3: Dynamic Triggers & Operating Limits

* **Reallocation Triggers:** The engine must identify a loan's estimated closing date (derived per Use Case 8.2) and, once that date is reached or passed, simulate the reallocation of the freed EMI amount toward a configured target portfolio (a `metadata JSONB`-stored target account or category, set as one-time manual policy).
* **Operating Budget Cap:** The engine must sum monthly DEBIT transactions tagged as household/discretionary spend (via category metadata on the transaction or account) and raise an advisory alert if the sum exceeds a configured monthly expense boundary. The boundary value is manual, one-time policy configuration, not derived.
* **SIP Protection Check:** The engine must verify that recurring DEBIT transactions identified as SIP/investment contributions (via account category) are not interrupted in a given month — flagging a gap as an advisory warning, not a blocking error.

#### Use Case 8.4: Validation & Zero-Leakage Rule Engine

* **Automated Execution:** All validation checks defined in this epic (category resolution, category double-counting, liquidity coverage, debt-to-asset ratio, EMI-to-income ratio where income is available, missing growth-rate assumptions, monthly cashflow) must run automatically as part of every CQRS dashboard refresh (`POST /v1/projections/refresh/{profileId}` in `web-gateway`) — not as a standalone script and not requiring a separate manual trigger.
* **Pass/Fail/Warning Result Set:** Each check must produce one of three outcomes: PASS, WARNING (advisory, refresh continues), or CRITICAL FAILURE (a structural data problem — e.g., uncategorized account, double-counted record). The full result set must be persisted as part of the dashboard projection output so the UI can display it.
* **Non-Blocking by Default:** A CRITICAL FAILURE in this engine must not prevent the dashboard refresh from completing for the metrics unaffected by the failure (per the existing per-step refresh isolation principle already adopted for `ProjectionCalculationEngine.refreshAll()` — see `ROADMAP.md` Architect Review). Whether validation failures should instead block the affected snapshot key specifically is an open product decision (`OpenQuestions.md` Q16).
* **Named Rule Preservation:** The zero-leakage category-summation check defined in Use Case 8.1 is referred to as the "Mahesh Summation Rule," preserving the product owner's existing terminology from his prior Python tooling.

#### Use Case 8.5: Goals Engine (Formula-Driven Financial Goals)

* **Distinction from Household Goals:** This is a materially different goal model from the household domain's existing `household.goal` table (simple savings-target-with-progress-bar, `REQUIREMENTS_household_domain.md` v0.3 Epic 4 equivalent / `ROADMAP.md` v0.3 delivered feature). The five goal types below are formula-driven against wealth-domain data and must be computed and stored as part of the wealth domain's CQRS read model, not the household `goal` table. See `ROADMAP.md` Business Analyst Review section appended below for the explicit recommendation on where this lives.
* **Goal Types (built-in, v0.4 scope):** The engine must support exactly five formula-driven goal types, each computing a `completion_pct` from a current value (derived from wealth data) against a target value (manual policy input, per Use Case 8.6):
  - **Debt Crossover:** current = total liquid assets ÷ total outstanding debt; target = 100%.
  - **30-70 Target:** current = (sum of EMI + SIP/investment + insurance DEBIT transactions in the period) ÷ income; target = ≤30% fixed / ≥70% free. Income is not a wealth-domain-derivable figure — see manual-entry list and `OpenQuestions.md` Q17.
  - **Freedom Runway:** current = current liquid "runway capital" (liquid assets excluding safety-net-tagged accounts); target = a configured multiple of monthly expenses (manual policy input).
  - **Insurance Free:** current = current liquid buffer; target = a configured self-insurance buffer amount (manual policy input).
  - **Year One / Emergency Influx:** current = emergency-fund-tagged liquid assets total; target = one year of household expenses (manual policy input; household expense figure itself may require manual entry or a cross-domain read — see `OpenQuestions.md` Q17).
* **Computed, Not Editable:** `completion_pct` and the current value for every goal type must be system-computed on every dashboard refresh, never directly editable by the user, consistent with the read-model/CQRS principle already established for `WEALTH_GOAL_PROGRESS`.
* **Extensibility Question Flagged:** Whether these five types are the complete, hardcoded set or whether the engine must support user-defined custom formulas is an open product decision — do not build a generalized formula engine without an explicit decision (`OpenQuestions.md` Q15).

#### Use Case 8.6: Manual Entry Boundary (What Remains Non-Derivable)

* **Explicit Non-Derivable Inputs:** The following must remain one-time or rarely-changing manual entry, written to `account.metadata JSONB` or an equivalent wealth-domain policy store — they are not parseable from any uploaded statement:
  - Loan original principal disbursed, loan start date, original tenure (Use Case 8.2)
  - Interest rate, when changed by the lender (Use Case 8.2)
  - Expected market return rate assumption used in prepayment-vs-invest comparison (Use Case 8.2)
  - Growth-rate assumption per investment account, used in 5/10-year projections (Use Case 8.1)
  - Asset category and purpose tag per account/asset, where not derivable from `account.type` alone (Use Case 8.1)
  - Goal target thresholds for all five goal types — these are policy choices the product owner sets, not data the system can derive (Use Case 8.5)
  - Monthly operating budget cap boundary (Use Case 8.3)
* **Statement-Derived Inputs (must not be manually entered once this epic ships):** Net worth, category subtotals, liquidity tiering, EMI principal/interest split, outstanding loan balance, tenure elapsed/remaining, goal current values, and all validation check results must be computed from `transaction` and `physical_asset` records — manual override of any of these values must be treated as a Critical Failure per Use Case 8.1's constraint validation.

## v0.5: Error Handling (Unhappy Path)

**Objective:** Build system resilience to handle malformed data and edge cases.

### Epic 9: Malformed Data Rejection

#### Use Case 9.1: Missing Required Columns

* **Rejection Protocol:** If an uploaded file is missing identifiable date or amount columns, the system must reject the entire file.
* **Error Logging:** The system must explicitly log which required data points were missing to inform the user.

## v1.0: Security & Persistence

**Objective:** Lock down the application architecture for persistent, real-world data storage.

### Epic 10: Data Hardening

#### Use Case 10.1: Transition to Persistent Data

* **Migration Strategy:** The database is no longer treated as ephemeral. All database modifications must strictly utilize versioned schema migration tools (e.g., Flyway) to prevent data loss.
* **Encryption:** Sensitive financial ledgers must utilize encryption at rest.