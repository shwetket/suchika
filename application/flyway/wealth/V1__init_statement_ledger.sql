-- ==============================================================================
-- V1__init_statement_ledger.sql
-- Core schema for the Wealth & Asset Management Write Model.
-- Covers: v0.1 Epic 1 — Standardized Statement Upload & Traceability
--
-- ARCHITECTURE: CQRS Write Model only.
-- This database is the immutable event ledger (Source of Truth).
-- All mathematical projections, aggregations, and dashboard state are computed
-- by the Read Model (application layer) and stored separately in MongoDB.
-- No Views, Stored Procedures, or aggregate Functions belong here.
--
-- DESIGN RULES ENFORCED IN EVERY TABLE:
--   1. No ENUMs     — VARCHAR + named CHECK constraints only.
--   2. No side-tables — sparse/domain data lives in JSONB metadata columns.
--   3. Immutable amounts — NUMERIC(19,4) CHECK (amount >= 0); direction via txn_type.
--   4. Full traceability — transaction → statement_upload → account (FK chain).
--   5. Native idempotency — composite UNIQUE constraint on transaction.
-- ==============================================================================


-- ==============================================================================
-- TABLE 1: account
-- The registry of all financial accounts owned by the household.
--
-- WHY a separate table and not inline on transaction?
--   Account metadata (institution, type, currency) is stable and shared across
--   hundreds of transactions. Normalising it here avoids redundant storage and
--   allows the mathematical engine to query accounts as a distinct entity
--   (e.g., "sum all MUTUAL_FUND accounts" without scanning the full ledger).
--
-- Account names are configured via application.properties — no schema change or
-- recompile is needed when adding a new account to the system.
-- ==============================================================================

CREATE TABLE account (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    institution_name VARCHAR(100) NOT NULL,
    account_name     VARCHAR(100) NOT NULL,
    account_type     VARCHAR(50)  NOT NULL,
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_account
        PRIMARY KEY (id),

    -- VARCHAR + CHECK instead of ENUM: can be extended via a simple ALTER TABLE
    -- constraint drop-and-recreate without rebuilding a type or touching existing data.
    CONSTRAINT chk_account_type
        CHECK (account_type IN (
            'SAVINGS',
            'CURRENT',
            'CREDIT_CARD',
            'LOAN',
            'MUTUAL_FUND',
            'FIXED_DEPOSIT'
        ))
);

COMMENT ON TABLE account IS $$
Registry of all financial accounts (savings, loans, investments) owned by the household.

Account records are seeded from application.properties — no schema migrations are needed
to introduce new accounts into the system.

Root anchor of the FK chain: every statement_upload and transaction traces back here.
This table is stable and queried by the mathematical engine to enumerate accounts
by type (e.g., "all active LOAN accounts") without scanning the transaction ledger.
$$;

COMMENT ON COLUMN account.institution_name IS
    'Financial institution name. e.g. ''SBI'', ''HDFC'', ''Zerodha'', ''Groww''.';

COMMENT ON COLUMN account.account_name IS $$
[SENSITIVITY: MEDIUM — PII FINANCIAL DATA]
User-assigned alias from application.properties. e.g. 'Salary Savings', 'Maxgain Loan',
'Family SIP'. May contain personally identifiable labels.
Infrastructure encryption at rest (disk/volume level) required in production.
$$;

COMMENT ON COLUMN account.account_type IS $$
Discriminator used by the application layer to select the correct CSV parser strategy.
VARCHAR + CHECK constraint — not ENUM — so new types can be added by dropping and
recreating the constraint with no type rebuild or data migration.
$$;

COMMENT ON COLUMN account.currency IS
    'ISO 4217 currency code. Defaults to INR for this deployment.';

COMMENT ON COLUMN account.is_active IS
    'Soft-delete flag. Inactive accounts are excluded from dashboard calculations.';


-- Partial index: primary query pattern is filtering active accounts by type.
-- The mathematical engine uses this to enumerate e.g. all active LOAN accounts.
CREATE INDEX idx_account_active_type
    ON account (account_type)
    WHERE is_active = TRUE;


-- ==============================================================================
-- TABLE 2: statement_upload
-- Source traceability anchor for every batch of ingested transactions.
--
-- WHY this table exists:
--   It decouples file-level metadata (filename, upload timestamp) from individual
--   transactions. It enables the single most important recovery operation:
--   DELETE one row here → every associated transaction disappears via CASCADE.
--   This is the ONLY sanctioned mechanism for removing ledger entries.
--
-- Lifecycle: PENDING → SUCCESS | FAILED
--   (status column added in V3__ when error-handling is introduced)
-- ==============================================================================

CREATE TABLE statement_upload (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID         NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    upload_date TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_statement_upload PRIMARY KEY (id),
    CONSTRAINT fk_upload_account   FOREIGN KEY (account_id) REFERENCES account(id)
);

COMMENT ON TABLE statement_upload IS $$
Source traceability record created for every CSV file ingested into the system.

CRITICAL ROLLBACK MECHANISM:
  DELETE one row from this table → all child transactions in the ledger are
  automatically removed via ON DELETE CASCADE. This is the only approved way
  to remove transaction data — the application never issues DELETE directly
  against the transaction table.

A status lifecycle column (PENDING / SUCCESS / FAILED) is added in V3__ when
the error-handling capability is introduced.
$$;

COMMENT ON COLUMN statement_upload.file_name IS
    'Original filename of the uploaded CSV. e.g. ''march_2024_sbi_statement.csv''.';

COMMENT ON COLUMN statement_upload.upload_date IS
    'Server-side timestamp of when the upload was initiated (not the statement period).';


-- Supports: "show all uploads for account X, newest first"
CREATE INDEX idx_upload_account_date
    ON statement_upload (account_id, upload_date DESC);


-- ==============================================================================
-- TABLE 3: transaction
-- The universal, immutable financial event ledger.
-- Single destination for ALL parsed statement lines, regardless of account type.
--
-- IMMUTABILITY CONTRACT:
--   - Rows are NEVER directly updated or deleted by the application layer.
--   - Amounts are ALWAYS stored as positive absolute values (chk_txn_amount).
--   - Money direction is encoded EXCLUSIVELY in txn_type ('CREDIT' | 'DEBIT').
--   - Deletes flow ONLY through CASCADE from statement_upload (controlled rollback).
--
-- JSONB STRATEGY (YAGNI):
--   All sparse, domain-specific enrichment lives in the metadata column.
--   Adding NAV+Units for mutual funds, EMI split for loans, or reference numbers
--   for bank transfers requires ZERO schema changes — the application just writes
--   different JSONB keys for each account type.
-- ==============================================================================

CREATE TABLE transaction (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID          NOT NULL,
    upload_id   UUID          NOT NULL,
    txn_date    DATE          NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    txn_type    VARCHAR(10)   NOT NULL,

    -- Raw narration string extracted directly from the CSV. Never normalised or
    -- categorised at the DB layer — that is Read Model territory.
    description TEXT          NOT NULL,

    -- JSONB enrichment payload. Schema is enforced by the application, not the DB.
    -- See column comment for per-account-type key conventions.
    metadata    JSONB         NOT NULL DEFAULT '{}'::jsonb,

    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction       PRIMARY KEY (id),
    CONSTRAINT fk_txn_account       FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_txn_upload        FOREIGN KEY (upload_id)
                                        REFERENCES statement_upload(id)
                                        ON DELETE CASCADE,

    -- Amounts are always stored positive. Direction is in txn_type, never in the sign.
    CONSTRAINT chk_txn_amount       CHECK (amount >= 0),
    CONSTRAINT chk_txn_type         CHECK (txn_type IN ('CREDIT', 'DEBIT')),

    -- Native idempotency: the DB natively rejects exact duplicate transactions.
    -- Cross-file: uploading the same statement twice → silent conflict, no duplicate inserted.
    -- Same-file: genuinely identical transactions (same date/amount/type/description)
    --            must be disambiguated by the parser BEFORE reaching this constraint
    --            (e.g., by appending a sequence suffix to description).
    CONSTRAINT uq_transaction_dedup
        UNIQUE (account_id, txn_date, amount, txn_type, description)
);

COMMENT ON TABLE transaction IS $$
The immutable, universal financial event ledger — the core of the CQRS Write Model.
One row per parsed statement line, regardless of account type or financial institution.

IMMUTABILITY CONTRACT:
  No UPDATE. No direct DELETE. Direction of money flow is in txn_type, never in amount.
  All deletes flow through the ON DELETE CASCADE from statement_upload.

CQRS BOUNDARY:
  This table is write-only from the application's perspective.
  The mathematical engine (Read Model) reads from this table and projects aggregated
  state to MongoDB for the dashboard. No SQL Views or aggregate queries are defined here.
$$;

COMMENT ON COLUMN transaction.amount IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
Always a positive absolute value. NUMERIC(19,4) provides sufficient decimal precision
for Indian Rupee transactions without floating-point rounding errors.
The sign (credit vs. debit) is encoded in txn_type — never in this column's sign.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN transaction.txn_type IS $$
Direction of money flow. Exactly two valid values:
  CREDIT — money entering the account (salary, interest, dividend, fund redemption).
  DEBIT  — money leaving the account (expense, withdrawal, loan EMI, SIP deduction).
The application parser is responsible for normalising negative CSV amounts:
  CSV amount = -5000  →  stored as  amount = 5000.0000, txn_type = 'DEBIT'.
$$;

COMMENT ON COLUMN transaction.description IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
Raw narration string from the source CSV, preserved verbatim. Never normalised.
May contain: merchant names, UPI IDs, beneficiary names, cheque numbers, ref codes.
Categorisation and enrichment are Read Model responsibilities, not stored here.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN transaction.metadata IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
JSONB enrichment payload. Schema enforced by the application layer, not the database.
Conventional keys by account type:

  MUTUAL_FUND  : { "nav": "53.2341", "units": "18.937", "fund_name": "..." }
  LOAN         : { "emi_principal": "12500.00", "emi_interest": "4200.00",
                   "outstanding_balance": "..." }
  SAVINGS/CC   : { "ref_no": "...", "upi_id": "...", "category": "..." }
  FIXED_DEPOSIT: { "maturity_date": "YYYY-MM-DD", "interest_rate": "7.10" }

Adding a new key type requires zero schema changes. The application layer owns all
JSONB evolution. Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN transaction.upload_id IS $$
Links this ledger entry to its source file upload record (statement_upload).
ON DELETE CASCADE is the only sanctioned mechanism for removing transactions:
  delete statement_upload → all its child transactions vanish automatically.
The application must never issue DELETE directly against this table.
$$;


-- ==============================================================================
-- INDEXES
-- ==============================================================================

-- Primary read pattern: "all transactions for account X between date Y and Z".
-- Covers the mathematical engine's core scan for any account-level aggregation.
CREATE INDEX idx_txn_account_date
    ON transaction (account_id, txn_date DESC);

-- Rollback and audit pattern: "all transactions that came from upload X".
-- Also used internally by PostgreSQL to execute the ON DELETE CASCADE efficiently.
CREATE INDEX idx_txn_upload_id
    ON transaction (upload_id);

-- Partial index for DEBIT-only queries.
-- The most common dashboard query: "sum all debits for account X this month".
-- Covers the Operating Budget Cap check (BRD Use Case 5.3) without scanning CREDITs.
CREATE INDEX idx_txn_debit_date
    ON transaction (account_id, txn_date DESC)
    WHERE txn_type = 'DEBIT';

-- GIN index for JSONB metadata queries.
-- e.g. WHERE metadata @> '{"category": "SIP"}' or WHERE metadata ? 'nav'
CREATE INDEX idx_txn_metadata
    ON transaction USING GIN (metadata);
