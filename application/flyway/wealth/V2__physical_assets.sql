-- ==============================================================================
-- V2__physical_assets.sql
-- Physical asset registry for vehicle ownership and compliance tracking.
-- Covers: v0.3 Epic 4 — Vehicle Asset Compliance (Use Case 4.1)
--
-- WHY a new table instead of extending wealth.account?
--   Vehicles are physical assets, not financial accounts. The account table has
--   institution_name, currency, and financial-account semantics that simply do
--   not apply to a vehicle. A dedicated table is the correct separation.
--
-- WHY compliance deadlines in JSONB and not columns?
--   1. Sparse: not every asset type (future: PROPERTY, EQUIPMENT) will have PUC
--      or Road Tax — dedicated columns would be mostly NULL.
--   2. Structurally different per registration_type: BH_SERIES plates have a
--      biennial road-tax renewal; PRIVATE plates typically pay lifetime tax.
--   3. Extensible: adding a Fitness Certificate deadline for commercial vehicles
--      requires zero DDL — the application just writes a new JSONB key.
--
-- WHY registration_type is a first-class column (not JSONB)?
--   The compliance scheduler filters assets by registration_type to build renewal
--   alerts. This query must be fast and indexable without JSONB operators.
-- ==============================================================================


CREATE TABLE wealth.physical_asset (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id          UUID,                               -- FK to profile.profile

    -- User-facing label for dashboards and compliance reports.
    asset_name          VARCHAR(100) NOT NULL,

    -- Category discriminator. Only VEHICLE in v0.3.
    -- Written as IN (...) so future types (PROPERTY, EQUIPMENT) can be added
    -- by modifying the constraint — no data migration required.
    asset_type          VARCHAR(50)  NOT NULL,

    -- Non-sparse vehicle identity columns — present on every vehicle record.
    make                VARCHAR(100) NOT NULL,
    model               VARCHAR(100) NOT NULL,

    -- Natural business key. A vehicle cannot be registered twice.
    registration_number VARCHAR(50)  NOT NULL,

    -- Drives compliance scheduling logic in the application layer.
    registration_type   VARCHAR(50)  NOT NULL,

    -- All compliance deadlines and optional metadata live here.
    metadata            JSONB        NOT NULL DEFAULT '{}'::jsonb,

    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_physical_asset
        PRIMARY KEY (id),

    CONSTRAINT chk_asset_type
        CHECK (asset_type IN ('VEHICLE')),

    CONSTRAINT chk_registration_type
        CHECK (registration_type IN (
            'PRIVATE',      -- Standard private plate; typically lifetime road tax.
            'COMMERCIAL',   -- Yellow plate; annual road tax + fitness certificate.
            'GOVERNMENT',   -- White plate with blue strip; custom renewal schedule.
            'BH_SERIES'     -- Bharat Series; biennial road-tax renewal.
        )),

    CONSTRAINT uq_registration_number
        UNIQUE (registration_number),

    -- FK to profile.profile: nullable — assets may exist before profiles are created.
    -- ON DELETE RESTRICT: a profile cannot be deleted while it owns physical assets.
    CONSTRAINT fk_physical_asset_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE wealth.physical_asset IS $$
Registry of physical assets (currently: vehicles) owned by the household.
Decoupled from wealth.account — a vehicle has no institution_name, currency,
or transactional semantics.

Compliance deadlines (PUC, Insurance, Road Tax) live in the metadata JSONB column
because they are sparse, structurally different per registration_type, and
extensible without schema changes.

profile_id is nullable. NULL = owned by the admin profile.
$$;

COMMENT ON COLUMN wealth.physical_asset.registration_type IS $$
Drives the application's compliance scheduling logic:
  PRIVATE     — Lifetime road tax. Annual PUC + insurance renewal.
  COMMERCIAL  — Annual road tax. Annual PUC. Fitness certificate cycle.
  GOVERNMENT  — Custom schedule tracked manually via metadata.
  BH_SERIES   — Biennial road-tax renewal. Use metadata key "bh_series_renewal_year".
$$;

COMMENT ON COLUMN wealth.physical_asset.registration_number IS $$
[SENSITIVITY: MEDIUM — PII ASSET DATA]
Unique RTO-issued vehicle registration number. e.g. 'KA-01-AB-1234'.
$$;

COMMENT ON COLUMN wealth.physical_asset.metadata IS $$
[SENSITIVITY: MEDIUM — PII ASSET DATA]
JSONB payload for compliance deadlines and optional vehicle details:
{
  "puc_expiry":             "YYYY-MM-DD",
  "insurance_expiry":       "YYYY-MM-DD",
  "insurance_provider":     "HDFC Ergo",
  "insurance_policy_no":    "...",
  "road_tax_expiry":        "YYYY-MM-DD",
  "road_tax_type":          "LIFETIME|ANNUAL|BIENNIAL",
  "bh_series_renewal_year": 2026,
  "purchase_date":          "YYYY-MM-DD",
  "chassis_number":         "...",
  "engine_number":          "...",
  "hypothecation_bank":     "SBI",
  "notes":                  "..."
}
$$;

COMMENT ON COLUMN wealth.physical_asset.profile_id IS $$
FK to profile.profile(id). Identifies the household member who owns this asset.
Nullable: NULL = owned by the admin profile.
ON DELETE RESTRICT: unlink assets before removing a profile.
$$;


-- GIN index for JSONB compliance deadline queries.
CREATE INDEX idx_physical_asset_metadata
    ON wealth.physical_asset USING GIN (metadata);

-- "All active vehicles by registration type" — primary compliance scheduler query.
CREATE INDEX idx_physical_asset_active_type
    ON wealth.physical_asset (registration_type)
    WHERE is_active = TRUE;

-- "All assets for household member X"
CREATE INDEX idx_physical_asset_profile_id
    ON wealth.physical_asset (profile_id)
    WHERE profile_id IS NOT NULL;
