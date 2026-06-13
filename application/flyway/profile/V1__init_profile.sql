-- ==============================================================================
-- V1__init_profile.sql
-- Household member identity registry — the cross-domain identity anchor.
-- All domains hold FKs pointing INTO profile; profile never holds FKs pointing out.
--
-- DOMAIN CONTRACT:
--   Profile answers "who is this person" (name, age, relationship, blood type).
--   Profile does NOT answer:
--     "What accounts does this person own?"  → wealth.account.profile_id
--     "What assets does this person own?"    → wealth.physical_asset.profile_id
--     "What health events?"                  → health.health_profile.profile_id
--
-- ADMIN MODEL:
--   Exactly ONE row has is_admin = TRUE (enforced by partial UNIQUE index).
--   That row always has relation_to_admin = 'SELF' (enforced by CHECK constraint).
--   All other rows are dependents of the single system owner.
--
-- MUTABILITY:
--   Core identity (full_name, dob, blood_type) is effectively immutable.
--   Contact info and health snapshot in metadata may be updated over time.
--   Hard deletes are blocked if other domains hold FK references (ON DELETE RESTRICT).
--   Use is_active = FALSE for soft-delete.
-- ==============================================================================


CREATE TABLE profile.profile (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),

    full_name         VARCHAR(150) NOT NULL,
    dob               DATE         NOT NULL,

    -- Relationship of this household member to the single admin profile.
    relation_to_admin VARCHAR(30)  NOT NULL,

    -- TRUE for exactly ONE row: the system owner.
    -- Enforced unique by the uq_single_admin partial index below.
    is_admin          BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Nullable: children and elderly members may not have email.
    email_address     VARCHAR(255),

    -- Nullable: not always known at creation. Stored as a column (not JSONB)
    -- because the health domain will filter on this value.
    gender            VARCHAR(30),

    -- Nullable: immutable biological fact once recorded.
    -- Stored as a column (not JSONB) — health domain will JOIN on blood type
    -- for emergency records. Only 8 possible values; GIN would be wasteful.
    blood_type        VARCHAR(10),

    -- Sparse enrichment: contact info, health snapshot, identity documents.
    -- Schema documented in the column comment below.
    metadata          JSONB        NOT NULL DEFAULT '{}'::jsonb,

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),


    CONSTRAINT pk_profile
        PRIMARY KEY (id),

    -- Relationship taxonomy for an Indian household.
    -- PARENT_IN_LAW is explicit (not merged into PARENT) because insurance,
    -- health, and financial dependency profiles differ in Indian households.
    CONSTRAINT chk_profile_relation
        CHECK (relation_to_admin IN (
            'SELF',           -- The admin profile. Exactly one row.
            'SPOUSE',
            'CHILD',
            'PARENT',
            'PARENT_IN_LAW',
            'SIBLING',
            'GRANDPARENT',
            'GRANDCHILD',
            'OTHER'
        )),

    CONSTRAINT chk_profile_gender
        CHECK (gender IS NULL OR gender IN (
            'MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'
        )),

    CONSTRAINT chk_profile_blood_type
        CHECK (blood_type IS NULL OR blood_type IN (
            'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'
        )),

    -- The admin must always be SELF. Prevents creating SPOUSE/CHILD with is_admin=TRUE.
    CONSTRAINT chk_admin_relation_is_self
        CHECK (is_admin = FALSE OR relation_to_admin = 'SELF')
);


-- ==============================================================================
-- INDEXES
-- ==============================================================================

-- Enforces the single-admin rule at the DB level.
-- Only ONE row can have is_admin = TRUE. A second attempt raises a unique violation.
CREATE UNIQUE INDEX uq_single_admin
    ON profile.profile (is_admin)
    WHERE is_admin = TRUE;

-- Allows multiple NULL email values while enforcing uniqueness for emails that ARE provided.
CREATE UNIQUE INDEX uq_profile_email
    ON profile.profile (email_address)
    WHERE email_address IS NOT NULL;

-- No additional indexes. profile is a read-heavy table with a maximum of ~10 rows
-- (admin + spouse + children + parents). PostgreSQL will always seq-scan a table
-- this small — additional indexes add write overhead with zero query benefit.


-- ==============================================================================
-- COMMENTS
-- ==============================================================================

COMMENT ON TABLE profile.profile IS $$
Household member identity registry. The upstream cross-domain anchor.

Domain boundary: this table answers identity questions only.
Other domains reference this table via profile_id FK — profile never references them.

Admin model:
  Exactly one row has is_admin = TRUE (enforced by uq_single_admin partial index).
  This row always has relation_to_admin = SELF.
  All other rows represent dependents in the single-household system.
$$;

COMMENT ON COLUMN profile.profile.relation_to_admin IS $$
Relationship to the single admin (SELF) profile.
PARENT_IN_LAW is distinct from PARENT because in Indian household planning,
in-laws often have separate insurance, dependency, and inheritance profiles.
$$;

COMMENT ON COLUMN profile.profile.is_admin IS $$
Marks the single system owner. Exactly ONE row may have this TRUE.
Enforced by the uq_single_admin partial UNIQUE index.
The admin profile is the implicit owner of any account or asset with profile_id = NULL.
$$;

COMMENT ON COLUMN profile.profile.blood_type IS $$
ABO + Rh blood group. Nullable until known; immutable once recorded.
Stored as a column (not JSONB) for direct filtering by the health domain
(emergency records, surgical history, insurance riders).
$$;

COMMENT ON COLUMN profile.profile.metadata IS $$
[SENSITIVITY: HIGH — PII IDENTITY + HEALTH DATA]
JSONB payload for sparse, person-specific enrichment.

Recommended structure:
{
  "contact": {
    "phone":             "+91-XXXXXXXXXX",
    "alternate_phone":   "+91-XXXXXXXXXX",
    "emergency_contact": { "name": "...", "phone": "...", "relation": "..." }
  },
  "health_snapshot": {
    "known_allergies":              ["penicillin", "latex"],
    "major_surgeries":              [{ "procedure": "...", "year": 2015, "hospital": "..." }],
    "primary_recurring_conditions": ["Type 2 Diabetes", "Hypertension"],
    "notes":                        "..."
  },
  "identity_docs": {
    "aadhar_last4":    "XXXX",
    "pan_masked":      "ABCPX1234X",
    "passport_number": "...",
    "driving_license": "..."
  }
}

Contains data subject to IT Act 2000 and DPDP Act 2023.
Infrastructure encryption at rest is mandatory in production.
$$;
