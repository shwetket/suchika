-- Repeatable migration for profile adapter integration tests.
-- This seed keeps Flyway validation stable for the shared local Postgres test setup.

INSERT INTO profile.admin (
    id,
    display_name,
    email_address,
    policy_settings,
    is_active,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Admin User',
    'admin@example.com',
    '{}'::jsonb,
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM profile.admin WHERE id = '00000000-0000-0000-0000-000000000001'::uuid
);

INSERT INTO profile.profile (
    id,
    admin_id,
    full_name,
    dob,
    relation_to_admin,
    email_address,
    gender,
    blood_type,
    is_active,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Seed Profile',
    '1990-01-01'::date,
    'SELF',
    'seed@example.com',
    'OTHER',
    'O+',
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM profile.profile WHERE id = '00000000-0000-0000-0000-000000000002'::uuid
);
