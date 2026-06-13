-- ==============================================================================
-- V4__enrich_account.sql
-- Enriches wealth.account with static financial fields and granular account types.
--
-- WHAT CHANGED:
--   1. account_type CHECK constraint expanded to granular loan and investment types.
--      Old: LOAN, FIXED_DEPOSIT
--      New: HOME_LOAN, PERSONAL_LOAN, CAR_LOAN, FD, NPS, PPF
--      Rationale: granular types allow the parser and projection engine to apply
--      type-specific rules (e.g., EMI split for HOME_LOAN vs PERSONAL_LOAN,
--      lock-in rules for PPF vs NPS vs FD).
--
--   2. Four static financial columns added (all nullable — not every account type
--      uses all fields):
--        opening_balance  — balance at the time the account was registered.
--        credit_limit     — applicable to CREDIT_CARD only.
--        interest_rate    — applicable to loans and fixed-return investments.
--        emi_amount       — applicable to loan accounts (HOME_LOAN, PERSONAL_LOAN, CAR_LOAN).
--
-- WHAT IS NOT HERE (these are calculated values → live in projections schema):
--   - current_balance     : SUM(credits) - SUM(debits) from wealth.transaction
--   - loan_outstanding    : changes every month as EMIs reduce principal
--   - current_value       : market value for MUTUAL_FUND; fluctuates daily
--
-- MIGRATION SAFETY:
--   The old CHECK constraint is dropped by name before the new one is added.
--   The four new columns have DEFAULT NULL so existing rows are unaffected.
--   No data migration required.
-- ==============================================================================


-- Step 1: Drop the old coarse account_type constraint.
ALTER TABLE wealth.account
    DROP CONSTRAINT chk_account_type;


-- Step 2: Add the granular constraint.
ALTER TABLE wealth.account
    ADD CONSTRAINT chk_account_type
        CHECK (account_type IN (
            'SAVINGS',
            'CURRENT',
            'CREDIT_CARD',
            'HOME_LOAN',
            'PERSONAL_LOAN',
            'CAR_LOAN',
            'MUTUAL_FUND',
            'NPS',
            'PPF',
            'FD'
        ));


-- Step 3: Add static financial fields.
-- All nullable: not every account type uses every field.
ALTER TABLE wealth.account
    ADD COLUMN opening_balance NUMERIC(19,4),
    ADD COLUMN credit_limit    NUMERIC(19,4),
    ADD COLUMN interest_rate   NUMERIC(7,4),
    ADD COLUMN emi_amount      NUMERIC(19,4);


-- Step 4: Add column comments.
COMMENT ON COLUMN wealth.account.opening_balance IS $$
Balance at the time the account was registered in the system.
This is the starting point for balance calculation:
  current_balance = opening_balance + SUM(CREDIT) - SUM(DEBIT)
Calculated current_balance lives in projections.dashboard_snapshot, not here.
$$;

COMMENT ON COLUMN wealth.account.credit_limit IS $$
Applicable to CREDIT_CARD accounts only.
The maximum credit line sanctioned by the issuing institution.
NULL for all other account types.
$$;

COMMENT ON COLUMN wealth.account.interest_rate IS $$
Annual interest rate as a percentage (e.g., 8.75 = 8.75% p.a.).
Applicable to: HOME_LOAN, PERSONAL_LOAN, CAR_LOAN, FD, NPS, PPF.
NULL for SAVINGS, CURRENT, CREDIT_CARD, MUTUAL_FUND.
$$;

COMMENT ON COLUMN wealth.account.emi_amount IS $$
Fixed monthly EMI amount for loan accounts.
Applicable to: HOME_LOAN, PERSONAL_LOAN, CAR_LOAN.
NULL for all other account types.
$$;
