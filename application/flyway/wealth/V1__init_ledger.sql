-- ==============================================================================
-- V1__init_ledger.sql
-- Core schema for the Wealth & Asset Management Write Model.
-- Covers: v0.1 Epic 1 — Standardised Statement Upload & Traceability
--
-- ARCHITECTURE: CQRS Write Model only.
-- This schema is the immutable event ledger (Source of Truth).
-- All mathematical projections and dashboard state are computed by the
-- application layer and stored in the projections schema (see projections/V1).
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
-- TABLE 1: wealth.account
-- Registry of all financial accounts owned by the household.
-- Account names are driven by application.properties — no schema change needed
-- when adding a new account to the system.
-- profile_id is nullable: accounts may be seeded before profiles exist.
-- NULL profile_id is interpreted as owned by the admin profile.
-- ==============================================================================

CREATE TABLE wealth.account (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id       UUID,                               -- FK added below
    institution_name VARCHAR(100) NOT NULL,
    account_name     VARCHAR(100) NOT NULL,
    account_type     VARCHAR(50)  NOT NULL,
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_account
        PRIMARY KEY (id),

    CONSTRAINT chk_account_type
        CHECK (account_type IN (
            'SAVINGS',
            'CURRENT',
            'CREDIT_CARD',
            'LOAN',
            'MUTUAL_FUND',
            'FIXED_DEPOSIT'
        )),

    -- FK to profile.profile: nullable by design (see header comment).
    -- ON DELETE RESTRICT: a profile cannot be deleted while it owns accounts.
    CONSTRAINT fk_account_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE wealth.account IS $$
Registry of all financial accounts owned by the household.
Root anchor of the FK chain: statement_upload → account; transaction → account.

profile_id is nullable: NULL is interpreted as belonging to the admin profile.
Accounts are seeded from application.properties before profiles may exist.
$$;

COMMENT ON COLUMN wealth.account.account_name IS $$
[SENSITIVITY: MEDIUM — PII FINANCIAL DATA]
User-assigned alias from application.properties. e.g. 'Salary Savings', 'Maxgain Loan'.
Infrastructure encryption at rest required in production.
$$;

COMMENT ON COLUMN wealth.account.account_type IS $$
Discriminator used by the application to select the correct CSV parser strategy.
VARCHAR + CHECK — not ENUM — so new types are added by modifying the constraint only.
$$;

COMMENT ON COLUMN wealth.account.profile_id IS $$
FK to profile.profile(id). Identifies the household member who owns this account.
Nullable: accounts may be seeded from config before profiles are created.
NULL = owned by the admin profile (system owner).
ON DELETE RESTRICT: unlink or reassign before removing a profile.
$$;


-- Primary query: "all active accounts by type" (used by the mathematical engine).
CREATE INDEX idx_account_active_type
    ON wealth.account (account_type)
    WHERE is_active = TRUE;

CREATE INDEX idx_account_profile_id
    ON wealth.account (profile_id)
    WHERE profile_id IS NOT NULL;


-- ==============================================================================
-- TABLE 2: wealth.statement_upload
-- Source traceability anchor for every batch of ingested transactions.
--
-- Lifecycle: PENDING → SUCCESS | FAILED (status column added in V3).
--
-- CRITICAL ROLLBACK MECHANISM:
--   DELETE one row here → all child transactions vanish via ON DELETE CASCADE.
--   This is the ONLY sanctioned way to remove ledger entries.
-- ==============================================================================

CREATE TABLE wealth.statement_upload (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID         NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    upload_date TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_statement_upload
        PRIMARY KEY (id),
    CONSTRAINT fk_upload_account
        FOREIGN KEY (account_id) REFERENCES wealth.account(id)
);

COMMENT ON TABLE wealth.statement_upload IS $$
Source traceability record created for every CSV file ingested.

DELETE one row → all child transactions in the ledger are automatically removed
via ON DELETE CASCADE. This is the only approved way to remove transaction data.
The application never issues DELETE directly against the transaction table.
$$;

CREATE INDEX idx_upload_account_date
    ON wealth.statement_upload (account_id, upload_date DESC);


-- ==============================================================================
-- TABLE 3: wealth.transaction
-- The universal, immutable financial event ledger.
--
-- IMMUTABILITY CONTRACT:
--   Rows are NEVER directly updated or deleted by the application layer.
--   Amounts are ALWAYS stored as positive absolute values.
--   Money direction is encoded EXCLUSIVELY in txn_type ('CREDIT' | 'DEBIT').
--   Deletes flow ONLY through CASCADE from statement_upload.
--
-- JSONB STRATEGY:
--   All sparse, domain-specific enrichment lives in the metadata column.
--   Adding NAV+Units for mutual funds, EMI splits for loans, or reference numbers
--   requires ZERO schema changes — the application writes different JSONB keys.
-- ==============================================================================

CREATE TABLE wealth.transaction (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID          NOT NULL,
    upload_id   UUID          NOT NULL,
    txn_date    DATE          NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    txn_type    VARCHAR(10)   NOT NULL,

    -- Raw narration from the CSV. Never normalised at the DB layer.
    description TEXT          NOT NULL,

    -- Sparse enrichment per account type. Schema enforced by the application.
    metadata    JSONB         NOT NULL DEFAULT '{}'::jsonb,

    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_transaction
        PRIMARY KEY (id),
    CONSTRAINT fk_txn_account
        FOREIGN KEY (account_id) REFERENCES wealth.account(id),
    CONSTRAINT fk_txn_upload
        FOREIGN KEY (upload_id) REFERENCES wealth.statement_upload(id)
        ON DELETE CASCADE,

    -- Amounts stored positive; direction is in txn_type, never in the sign.
    CONSTRAINT chk_txn_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_txn_type
        CHECK (txn_type IN ('CREDIT', 'DEBIT')),

    -- Native idempotency: the DB natively rejects exact duplicate transactions.
    -- Same-file duplicates must be disambiguated by the parser before reaching here.
    CONSTRAINT uq_transaction_dedup
        UNIQUE (account_id, txn_date, amount, txn_type, description)
);

COMMENT ON TABLE wealth.transaction IS $$
Immutable financial event ledger — the CQRS Write Model core.
One row per parsed statement line, regardless of account type.

No UPDATE. No direct DELETE. All deletes flow through ON DELETE CASCADE
from statement_upload. The application never touches this table's rows directly.
$$;

COMMENT ON COLUMN wealth.transaction.amount IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
Always a positive absolute value. Direction is in txn_type.
NUMERIC(19,4) avoids floating-point rounding on Indian Rupee transactions.
$$;

COMMENT ON COLUMN wealth.transaction.description IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
Raw narration from the source CSV. May contain merchant names, UPI IDs,
beneficiary names, cheque numbers, reference codes.
$$;

COMMENT ON COLUMN wealth.transaction.metadata IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
JSONB enrichment by account type:
  MUTUAL_FUND  : { "nav": "53.23", "units": "18.94", "fund_name": "..." }
  LOAN         : { "emi_principal": "12500.00", "emi_interest": "4200.00" }
  SAVINGS/CC   : { "ref_no": "...", "upi_id": "...", "category": "..." }
  FIXED_DEPOSIT: { "maturity_date": "YYYY-MM-DD", "interest_rate": "7.10" }
$$;


-- "All transactions for account X between dates Y and Z" — core engine scan.
CREATE INDEX idx_txn_account_date
    ON wealth.transaction (account_id, txn_date DESC);

-- Used by PostgreSQL to execute the ON DELETE CASCADE from statement_upload.
CREATE INDEX idx_txn_upload_id
    ON wealth.transaction (upload_id);

-- DEBIT-only queries: "sum all debits for account X this month".
CREATE INDEX idx_txn_debit_date
    ON wealth.transaction (account_id, txn_date DESC)
    WHERE txn_type = 'DEBIT';

-- GIN index for JSONB metadata queries.
-- Partial: excludes empty-metadata rows (savings/CC) — reduces index size ~80%.
CREATE INDEX idx_txn_metadata
    ON wealth.transaction USING GIN (metadata)
    WHERE metadata != '{}'::jsonb;
