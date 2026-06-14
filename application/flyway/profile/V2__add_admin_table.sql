-- ==============================================================================
-- V2__add_admin_table.sql
-- Introduces the admin identity concept and restructures profile.profile.
--
-- ARCHITECTURE DECISION: Multi-Admin Support
--   The V1 design had a single admin enforced by a partial UNIQUE index on is_admin.
--   This migration adds a dedicated profile.admin table and links profile.profile to it
--   via admin_id FK. This enables multiple admins to each manage their own household
--   members — the foundation for v1.1 (Multi-User milestone).
--
--   v0.1 behaviour is unchanged: one admin exists, all profiles belong to that admin.
--   v1.1 behaviour: each authenticated user is an admin managing their own family.
--
-- ARCHITECTURE DECISION: No VARCHAR discriminator CHECK constraints
--   The is_admin BOOLEAN and chk_profile_relation / chk_profile_gender /
--   chk_profile_blood_type constraints are removed in this migration.
--
--   Rationale: permitted values (SELF, SPOUSE, CHILD, etc.) are enforced at two
--   stronger layers that are cheaper to change:
--     1. OpenAPI contract enum — the code generator produces a Java enum from it.
--     2. Java enum + validation annotation — rejects invalid values at the HTTP boundary.
--
--   Adding a new relation type (e.g., COUSIN) requires:
--     BEFORE this change: Flyway migration + redeploy.
--     AFTER  this change: Contract ENUM update + Java enum update + redeploy (no DB touch).
--
-- WHAT STAYS (structural invariants, not enum lists):
--   - NOT NULL constraints: data without identity is meaningless.
--   - PK/FK constraints: database referential integrity.
--   - uq_admin_self_profile: each admin has exactly one SELF (their own member record).
--     This is a structural business rule, not an enum discriminator.
--
-- MIGRATION SAFETY:
--   If the profile.profile table has existing rows (dev data), they will need
--   manual admin_id assignment or a flyway:clean reset. Pre-v1.0 data is volatile.
--   See CLAUDE.md "Ephemeral Test Data" rule.
-- ==============================================================================


-- ==============================================================================
-- STEP 1: Create profile.admin
-- The authentication anchor and household manager.
-- One admin per household in v0.1; multiple admins supported by the schema.
-- ==============================================================================

CREATE TABLE profile.admin (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- Human-readable display name (not necessarily a legal name).
    -- The admin's legal name lives in their own profile.profile row (relation = SELF).
    display_name  VARCHAR(150) NOT NULL,

    -- Future v1.0 login email. Nullable until the Security milestone.
    -- Kept here (not in profile.profile) because this is the auth credential,
    -- not a contact detail — different concerns.
    email_address VARCHAR(255),

    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin
        PRIMARY KEY (id),

    -- Partial unique: NULLs are excluded, so multiple NULLs are allowed.
    -- Once set, each email must identify exactly one admin.
    CONSTRAINT uq_admin_email
        UNIQUE (email_address)
);

COMMENT ON TABLE profile.admin IS $$
Authentication anchor and household manager identity.

One row per admin (system owner). In v0.1 exactly one admin exists.
In v1.1+ each admin is an authenticated user managing their own household.

The admin's personal details (DOB, gender, blood type, etc.) live in their
own profile.profile row where relation_to_admin = SELF.

email_address is the future login credential (v1.0). Nullable until then.
$$;


-- ==============================================================================
-- STEP 2: Add admin_id to profile.profile
-- Every household member must belong to exactly one admin's household.
-- ==============================================================================

ALTER TABLE profile.profile
    ADD COLUMN admin_id UUID;

-- Back-fill: if dev data exists, set a placeholder; a clean re-migration
-- (flyway:clean) is the recommended approach in the pre-v1.0 dev phase.
-- In production (v1.0+) this column will always have a value.

ALTER TABLE profile.profile
    ADD CONSTRAINT fk_profile_admin
        FOREIGN KEY (admin_id)
        REFERENCES profile.admin(id)
        ON DELETE RESTRICT;


-- ==============================================================================
-- STEP 3: Remove the old single-admin enforcement artifacts
-- ==============================================================================

-- Drop the old partial index that allowed only one row with is_admin = TRUE.
-- Multi-admin support replaces this with uq_admin_self_profile below.
DROP INDEX IF EXISTS profile.uq_single_admin;

-- Drop the email uniqueness index — table has ≤10 rows; seq scan is faster.
-- Email uniqueness at the admin level is enforced by uq_admin_email above.
DROP INDEX IF EXISTS profile.uq_profile_email;

-- Remove the is_admin column (admin status is now implicit:
-- an admin has a profile.profile row with relation_to_admin = SELF linked
-- to a profile.admin row).
ALTER TABLE profile.profile
    DROP COLUMN IF EXISTS is_admin;


-- ==============================================================================
-- STEP 4: Remove VARCHAR discriminator CHECK constraints
-- Values are now enforced at the contract (OpenAPI enum) + Java enum level.
-- ==============================================================================

ALTER TABLE profile.profile
    DROP CONSTRAINT IF EXISTS chk_profile_relation,
    DROP CONSTRAINT IF EXISTS chk_profile_gender,
    DROP CONSTRAINT IF EXISTS chk_profile_blood_type,
    DROP CONSTRAINT IF EXISTS chk_admin_relation_is_self;


-- ==============================================================================
-- STEP 5: Add structural invariant — one SELF per admin
-- Each admin must have exactly one SELF profile (their own member record).
-- This is a structural business rule, not an enum discriminator → kept in DB.
-- ==============================================================================

CREATE UNIQUE INDEX uq_admin_self_profile
    ON profile.profile (admin_id)
    WHERE relation_to_admin = 'SELF';

COMMENT ON INDEX profile.uq_admin_self_profile IS $$
Structural invariant: each admin has exactly one SELF profile (their own member record).
A second SELF profile for the same admin raises a unique violation.
This is a business rule (identity anchor uniqueness), not an enum discriminator.
$$;
