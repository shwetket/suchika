-- ==============================================================================
-- R__seed_health_test_data.sql
-- Wipes and inserts test seed data for the Health schema (dev/test environments only).
-- Inserts are guarded: skipped silently if the seed profile does not yet exist
-- (avoids FK violations when health service restarts before profile service).
-- ==============================================================================

TRUNCATE TABLE health.vital_reading CASCADE;
TRUNCATE TABLE health.doctor_visit CASCADE;

INSERT INTO health.vital_reading (id, profile_id, vital_type, reading_date, value_primary, value_secondary, unit, notes)
VALUES ('f3b90000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',
        'BLOOD_PRESSURE', '2026-06-16', 120.00, 80.00, 'mmHg', 'Test Reading');
