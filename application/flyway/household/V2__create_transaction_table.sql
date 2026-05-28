CREATE TYPE txn_type AS ENUM (
    -- Money in
    'INCOME',           -- salary, rent, freelance
    'INTEREST_EARNED',  -- savings/FD interest
    'INVESTMENT_GAIN',  -- dividend, redemption profit

    -- Money out (real expenses)
    'EXPENSE',          -- UPI, CC swipe, any real spend
    'LOAN_INTEREST',    -- interest portion of EMI
    'BANK_CHARGE',      -- annual fee, penalty

    -- Internal moves (not income, not expense)
    'TRANSFER',         -- savings → CC bill, savings → loan EMI principal
                        -- savings → SIP, CC bill payment
                        -- use transfer_to_account_id to link the other side
    
    -- Investment specific
    'INVESTMENT_BUY',   -- SIP or lumpsum into MF/NPS
    'INVESTMENT_SELL'   -- redemption out
);

CREATE TABLE transaction (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id            UUID          NOT NULL REFERENCES account(id),
    txn_type              txn_type      NOT NULL,
    amount                NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    txn_date              DATE          NOT NULL,
    description           VARCHAR(255),

    -- For TRANSFER — links the other account involved
    transfer_to_account_id UUID         REFERENCES account(id),

    -- For INVESTMENT_BUY / SELL — optional detail
    units                 NUMERIC(19,6),
    nav                   NUMERIC(19,6),

    -- For LOAN EMI — principal and interest split
    principal_component   NUMERIC(19,2),
    interest_component    NUMERIC(19,2),

    created_at            TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_txn_account_id ON transaction(account_id);
CREATE INDEX idx_txn_date       ON transaction(txn_date);
CREATE INDEX idx_txn_type       ON transaction(txn_type);