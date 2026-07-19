-- ==============================================================================
-- R__seed_household_test_data.sql
-- LOCAL-ONLY REAL DATA — gitignored (application/flyway/test-seed/) on the
-- "seed-data" branch, never pushed/merged.
-- Depends on profile seed being applied first (FK: profile_id -> profile.profile).
--
-- profile_id references (from R__seed_profile_test_data.sql):
--   00000000-0000-0000-0000-000000000002 = Ketan Verma (SELF)
-- ==============================================================================

TRUNCATE TABLE household.calendar_event CASCADE;
TRUNCATE TABLE household.inventory_item CASCADE;
TRUNCATE TABLE household.goal CASCADE;

-- No real calendar/inventory data has been provided yet (GAP) — kept generic,
-- repointed to Ketan's real profile so FKs are consistent with the rest of this file.
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

-- NOTE: the 5 real Epic-8 goals (Debt Crossover, 30-70 Target, Freedom Runway,
-- Insurance Free, Year One) are NOT stored here — they are computed live by
-- ProjectionCalculationEngine.computeFormulaGoals() from wealth data (net worth,
-- EMI, liquidity tiers, investment growth) plus profile.admin.policy_settings
-- (see R__seed_profile_test_data.sql, which now seeds real monthly_budget_cap=
-- 100000 and freedom_runway_months=6). Storing them as household.goal rows was
-- an earlier mistake in this branch's history — that table is for manual/simple
-- savings goals the engine never reads. No genuine real manual goal has been
-- supplied yet (GAP), so household.goal is intentionally left empty here.

-- checksum-bump: force Flyway repeatable-migration re-apply after profile.profile TRUNCATE CASCADE wiped this schema data (2026-07-10 22:14 IST)
