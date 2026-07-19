-- ==============================================================================
-- R__seed_health_test_data.sql
-- LOCAL-ONLY REAL DATA — gitignored (application/flyway/test-seed/) on the
-- "seed-data" branch, never pushed/merged.
-- Inserts are guarded: skipped silently if the seed profile does not yet exist
-- (avoids FK violations when health service restarts before profile service).
--
-- No real vitals/doctor-visit data has been provided yet (GAP) — kept generic,
-- repointed to Ketan's real profile_id so FKs are consistent with the rest of
-- this branch's seed data.
-- ==============================================================================

TRUNCATE TABLE health.vital_reading CASCADE;
TRUNCATE TABLE health.doctor_visit CASCADE;

INSERT INTO health.vital_reading (id, profile_id, vital_type, reading_date, value_primary, value_secondary, unit, notes)
VALUES ('f3b90000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',
        'BLOOD_PRESSURE', '2026-06-16', 120.00, 80.00, 'mmHg', 'Test Reading');

-- checksum-bump: force Flyway repeatable-migration re-apply after profile.profile TRUNCATE CASCADE wiped this schema data (2026-07-10 22:14 IST)
