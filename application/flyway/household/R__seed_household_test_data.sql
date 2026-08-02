-- Repeatable migration for household adapter integration tests.
-- This seed keeps Flyway validation stable for the shared local Postgres test setup.

INSERT INTO household.inventory_item (
    id,
    profile_id,
    item_name,
    quantity,
    unit,
    source_platform,
    purchase_date,
    category,
    is_consumed,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000001'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    'Basmati Rice',
    5.0,
    'KG',
    'BLINKIT',
    '2026-01-01'::date,
    'Grains',
    false,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM household.inventory_item WHERE id = '00000000-0000-0000-0000-000000000001'::uuid
);

INSERT INTO household.calendar_event (
    id,
    profile_id,
    title,
    event_type,
    start_date,
    end_date,
    location,
    notes,
    metadata,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000003'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    'Seed Event',
    'PERSONAL',
    '2026-01-01'::date,
    '2026-01-01'::date,
    'Home',
    'seed event description',
    '{}'::jsonb,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM household.calendar_event WHERE id = '00000000-0000-0000-0000-000000000003'::uuid
);

INSERT INTO household.goal (
    id,
    profile_id,
    goal_name,
    target_amount,
    current_amount,
    monthly_saving,
    target_date,
    status,
    notes,
    created_at
)
SELECT
    '00000000-0000-0000-0000-000000000005'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    'Seed Goal',
    100.0,
    10.0,
    5.0,
    '2026-01-31'::date,
    'ACTIVE',
    'seed goal description',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM household.goal WHERE id = '00000000-0000-0000-0000-000000000005'::uuid
);
