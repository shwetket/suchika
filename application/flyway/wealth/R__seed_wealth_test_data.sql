-- Repeatable migration for wealth adapter integration tests.
-- This seed keeps Flyway validation stable for the shared local Postgres test setup.

INSERT INTO wealth.account (
    id,
    profile_id,
    institution_name,
    account_name,
    account_type,
    opening_balance,
    is_active
)
SELECT
    'f3b90000-0000-0000-0000-000000000000'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    'Test Bank',
    'Test Account',
    'SAVINGS',
    0.00,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM wealth.account WHERE id = 'f3b90000-0000-0000-0000-000000000000'::uuid
);
