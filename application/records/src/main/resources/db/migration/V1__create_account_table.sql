CREATE TYPE account_type AS ENUM (
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
);

CREATE TABLE account (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    account_name     VARCHAR(100) NOT NULL,          -- "HDFC Savings", "HDFC CC"
    account_type     account_type NOT NULL,
    institution_name VARCHAR(100) NOT NULL,          -- "HDFC Bank", "SBI"
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    opening_balance  NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    credit_limit     NUMERIC(19,2),                  -- only for CREDIT_CARD
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN account.opening_balance IS
    'Balance at the time this account was added to the system.
     All ledger entries after this date build on top of this.';

COMMENT ON COLUMN account.credit_limit IS
    'Applicable only for CREDIT_CARD accounts.
     NULL for all other account types.';