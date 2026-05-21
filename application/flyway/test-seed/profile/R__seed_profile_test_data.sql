-- ==============================================================================
-- R__seed_profile_test_data.sql
-- Wipes and inserts test seed data for the Profile schema (dev/test environments only).
-- ==============================================================================

-- Truncate tables (CASCADE deletes child references in other schemas as well)
TRUNCATE TABLE profile.profile CASCADE;
TRUNCATE TABLE profile.admin CASCADE;

-- Insert seed Admin
INSERT INTO profile.admin (id, display_name, email_address, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Test Admin', 'admin@test.com', true);

-- Insert seed Profile (relation_to_admin = SELF)
INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin, email_address, gender, blood_type, is_active)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Test Member', '1990-01-15', 'SELF', 'member@test.com', 'MALE', 'O+', true);
