CREATE TYPE goal_status AS ENUM (
    'ACTIVE',
    'ACHIEVED',
    'PAUSED'
);

CREATE TABLE goal (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_name        VARCHAR(200)  NOT NULL,   -- "1 Cr Liquid Fund"
    target_amount    NUMERIC(19,2) NOT NULL,   -- 10,000,000
    current_amount   NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    monthly_saving   NUMERIC(19,2),            -- how much you add per month
    target_date      DATE,                     -- optional deadline
    status           goal_status   NOT NULL DEFAULT 'ACTIVE',
    notes            TEXT,
    created_at       TIMESTAMP     NOT NULL DEFAULT now()
);

COMMENT ON TABLE goal IS
    'Days to complete = (target_amount - current_amount) / (monthly_saving / 30).
     System calculates this live — no extra columns needed.';