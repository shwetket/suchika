-- ==============================================================================
-- R__seed_household_test_data.sql
-- Wipes and inserts test seed data for the Household schema (dev/test environments only).
-- Depends on profile seed being applied first (FK: profile_id -> profile.profile).
-- ==============================================================================

-- Repeatable migration — Flyway re-runs whenever the checksum changes.
-- Truncate household tables, then re-insert known rows.
TRUNCATE TABLE household.calendar_event CASCADE;
TRUNCATE TABLE household.inventory_item CASCADE;
TRUNCATE TABLE household.goal CASCADE;

-- Seeded profile_id from R__seed_profile_test_data.sql
-- '00000000-0000-0000-0000-000000000002' is the Test Member profile

INSERT INTO household.calendar_event
    (id, profile_id, title, event_type, start_date, end_date, location, notes)
VALUES
    ('10000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     'Annual Checkup', 'MEDICAL',
     '2026-07-10', NULL, 'City Hospital', 'Fasting required');

INSERT INTO household.inventory_item
    (id, profile_id, item_name, quantity, unit, source_platform, purchase_date, category)
VALUES
    ('20000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     'Basmati Rice', 5.000, 'KG', 'BLINKIT',
     '2026-07-01', 'Grains');

INSERT INTO household.goal
    (id, profile_id, goal_name, target_amount, current_amount, monthly_saving, target_date, status, notes)
VALUES
    ('30000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     'Emergency Fund', 100000.00, 30000.00, 5000.00,
     '2027-12-31', 'ACTIVE', 'Safety net');
