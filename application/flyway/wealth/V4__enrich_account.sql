-- ==============================================================================
-- V4__enrich_account.sql
-- Enriches wealth.account with static financial fields and granular account types.
--
-- ARCHITECTURE DECISION: No VARCHAR discriminator CHECK constraints
--   The chk_account_type constraint from V1 is DROPPED and NOT replaced.
--   account_type values (HOME_LOAN, SAVINGS, etc.) are enforced at:
--     1. OpenAPI contract enum (wealth.yaml → AccountType schema).
--     2. Java enum + @Valid annotation — rejects invalid values at the HTTP layer.
--   Adding a new account type no longer requires a Flyway migration.
--
-- STATIC FIELDS ADDED (stored in wealth.account — set once, rarely change):
--   opening_balance — starting balance when the account was registered.
--   credit_limit    — sanctioned limit for CREDIT_CARD accounts.
--   interest_rate   — annual rate % for loans and fixed-return investments.
--   emi_amount      — fixed monthly EMI for loan accounts.
--
-- DYNAMIC FIELDS NOT HERE (calculated → live in projections.dashboard_snapshot):
--   current_balance  — opening_balance + SUM(CREDIT) - SUM(DEBIT)
--   loan_outstanding — decreases with each EMI payment
--   current_value    — market value for MUTUAL_FUND; fluctuates daily
-- ==============================================================================


-- Drop the V1 enum CHECK constraint.
-- Not replaced — values enforced at contract + Java layer.
ALTER TABLE wealth.account
    DROP CONSTRAINT chk_account_type;


-- Add static financial fields. All nullable — not every account type uses all.
ALTER TABLE wealth.account
    ADD COLUMN opening_balance NUMERIC(19,4),
    ADD COLUMN credit_limit    NUMERIC(19,4),
    ADD COLUMN interest_rate   NUMERIC(7,4),
    ADD COLUMN emi_amount      NUMERIC(19,4);


COMMENT ON COLUMN wealth.account.opening_balance IS $$
Balance at the time the account was registered.
Used as the starting point for balance projection:
  current_balance = opening_balance + SUM(CREDIT) - SUM(DEBIT)
The calculated current_balance lives in projections.dashboard_snapshot.
$$;

COMMENT ON COLUMN wealth.account.credit_limit IS $$
Sanctioned credit limit. Applicable to CREDIT_CARD accounts only.
NULL for all other account types.
$$;

COMMENT ON COLUMN wealth.account.interest_rate IS $$
Annual interest rate as a percentage (e.g., 8.75 = 8.75% p.a.).
Applicable to: HOME_LOAN, PERSONAL_LOAN, CAR_LOAN, FD, NPS, PPF.
NULL for SAVINGS, CURRENT, CREDIT_CARD, MUTUAL_FUND.
$$;

COMMENT ON COLUMN wealth.account.emi_amount IS $$
Fixed monthly EMI amount. Applicable to loan accounts only.
NULL for all other account types.
$$;
