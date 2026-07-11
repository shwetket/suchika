-- ==============================================================================
-- R__seed_wealth_test_data.sql
-- Wipes and inserts test seed data for the Wealth schema (dev/test environments only).
-- ==============================================================================

TRUNCATE TABLE wealth.account CASCADE;

-- Insert seed account
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active)
VALUES ('f3b90000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000002', 'Test Bank', 'Test Account', 'SAVINGS', 'INR', true);
