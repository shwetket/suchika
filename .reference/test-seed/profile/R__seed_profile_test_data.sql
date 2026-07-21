-- ==============================================================================
-- R__seed_profile_test_data.sql
-- LOCAL-ONLY REAL DATA — this file is gitignored (application/flyway/test-seed/) on
-- the "seed-data" branch, which is never pushed or merged. It uses the real Verma
-- household so local testing/dashboards reflect real-world shapes. See the bottom
-- of this file for known data gaps flagged for the household to confirm.
-- ==============================================================================

TRUNCATE TABLE profile.profile CASCADE;
TRUNCATE TABLE profile.admin CASCADE;

-- Admin: Ketan Verma — household manager.
-- policy_settings drives the Epic 8 formula-goals engine (ProjectionCalculationEngine
-- .computeFormulaGoals): monthly_budget_cap is the REAL operating-expense cap from
-- Financial_Data.md Rule L3 (Rs 1,00,000/month); freedom_runway_months is the REAL
-- target coverage from assets_06062026.json's liquidity_coverage.target_coverage_months
-- (6). debt_crossover_threshold_percent / insurance_multiple left at engine defaults —
-- the real goal_06062026.json values use a slightly different formula shape than what's
-- implemented, so they aren't directly portable as thresholds (documented gap).
-- setup_completed: "true" — this household's data was inserted directly via seed, not
-- through the app's setup wizard, but it IS a fully set-up household (4 real profiles,
-- 46 real accounts, 8 real physical assets). Without this flag, SetupGate.js:31 bounces
-- every admin-role sign-in to /admin/setup on every login (QA finding #2, 2026-07-10).
INSERT INTO profile.admin (id, display_name, email_address, policy_settings, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Ketan Verma', 'shwetaketanverma@gmail.com',
        '{"monthly_budget_cap": 100000, "freedom_runway_months": 6, "setup_completed": "true"}'::jsonb, true);

-- SELF: Ketan Verma — holds Kotak Bellandur (4713330966) + HDFC Gachibowli (...5889),
-- and is joint holder on the Kotak Varthur account below. Real net salary Rs 2,40,383/mo
-- + annual bonus (net) Rs 6,32,272 per Financial_Data.md — not representable as a single
-- DB field yet (see wealth seed notes). Real DOB confirmed 2026-07-10.
INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin, email_address, gender, blood_type, is_active)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
        'Ketan Verma', '1990-04-14', 'SELF', 'shwetaketanverma@gmail.com', 'MALE', NULL, true);

-- SPOUSE: Shweta Ketan Verma — primary holder of Kotak Varthur joint account (9914262252),
-- and sole owner of the Indore land + gold holdings (see wealth seed). Real rental income
-- Rs 40,000/mo (matches the real bank credits). Real DOB confirmed 2026-07-10.
INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin, email_address, gender, blood_type, is_active)
VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001',
        'Shweta Ketan Verma', '1991-07-02', 'SPOUSE', NULL, 'FEMALE', NULL, true);

-- Gayan Verma — son (confirmed CHILD via assets_06062026.json: "owner Gayan, status Son"),
-- nominee on the Kotak Varthur joint account. Real age 5 as of 2026-06-06 per the same
-- file — DOB below is an age-consistent estimate (day/month still a gap).
INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin, email_address, gender, blood_type, is_active)
VALUES ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001',
        'Gayan Verma', '2021-03-01', 'CHILD', NULL, 'MALE', NULL, true);

-- Vamika Verma — daughter (confirmed CHILD via assets_06062026.json: "owner Vamika,
-- status Daughter"). Real age 1 as of 2026-06-06, and her education FD entry_date is
-- 2025-01-01 (children's FDs are typically opened right after birth) — DOB below uses
-- that date as the best available anchor; exact day still a gap.
INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin, email_address, gender, blood_type, is_active)
VALUES ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001',
        'Vamika Verma', '2025-01-01', 'CHILD', NULL, 'FEMALE', NULL, true);

-- NOTE: Jagdish Verma / Sharda Verma (Ketan's parents) are deliberately excluded per
-- earlier decision this session — they manage their own finances separately.
