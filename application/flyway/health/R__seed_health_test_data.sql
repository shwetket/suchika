-- Repeatable migration for health adapter integration tests.
-- This seed exists so Flyway validation stays consistent when the local DB already
-- contains the historical health test data row referenced by the tests.

INSERT INTO health.vital_reading (
    id,
    profile_id,
    vital_type,
    value_primary,
    value_secondary,
    unit,
    reading_date,
    notes,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000001'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    'BLOOD_PRESSURE',
    120.0,
    80.0,
    'mmHg',
    '2026-01-01'::date,
    'seeded test reading',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM health.vital_reading WHERE id = '00000000-0000-0000-0000-000000000001'::uuid
);

INSERT INTO health.doctor_visit (
    id,
    profile_id,
    from_date,
    to_date,
    visited_doctor,
    doctor_name,
    hospital_name,
    speciality,
    symptoms,
    diagnosis,
    notes,
    follow_up_date,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000003'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    '2026-01-01'::date,
    '2026-01-01'::date,
    true,
    'Dr. Seed',
    'Seed Hospital',
    'General Medicine',
    'seed symptom',
    'seed diagnosis',
    'seed note',
    '2026-01-08'::date,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM health.doctor_visit WHERE id = '00000000-0000-0000-0000-000000000003'::uuid
);
