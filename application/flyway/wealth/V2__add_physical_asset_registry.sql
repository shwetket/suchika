-- ==============================================================================
-- V2__add_physical_asset_registry.sql
-- Adds the physical asset registry for vehicle ownership and compliance tracking.
-- Covers: v0.3 Epic 4 — Vehicle Asset Compliance (Use Case 4.1)
--
-- NOTE ON v0.3 EPIC 3 (Investment CSV Parsing):
--   No migration is required for investment data ingestion.
--   The BRD explicitly states this must work "without altering the core database
--   schema." The existing transaction.metadata JSONB column already handles
--   Units and NAV. The account table already includes the MUTUAL_FUND type.
--   Investment parsing is a pure application-layer change.
--
-- WHY a new table instead of extending `account`?
--   Vehicles are physical assets, not financial accounts. The account table has
--   institution_name, currency, and financial-account semantics that simply do
--   not apply to a vehicle. Reusing it would create semantic pollution and make
--   the mathematical engine's account-type queries ambiguous. A dedicated table
--   is the KISS-correct separation.
--
-- WHY are compliance deadlines in JSONB and not as columns?
--   1. Sparse: not every asset type (future: PROPERTY, EQUIPMENT) will have PUC
--      or Road Tax. Dedicated columns would be mostly NULL.
--   2. Structurally different per registration_type: BH_SERIES plates have a
--      biennial road-tax renewal; PRIVATE plates typically pay lifetime tax.
--      One set of columns cannot cleanly model both schedules.
--   3. Extensible: adding a Fitness Certificate deadline for commercial vehicles
--      requires zero DDL changes — the application just writes a new JSONB key.
--
-- WHY is registration_type a first-class column and NOT in JSONB?
--   The application's compliance scheduler filters assets by registration_type
--   to build renewal alert logic (BH_SERIES → biennial renewal vs. all others).
--   This query must be fast and indexable without JSONB operators. First-class
--   column is the correct choice here.
-- ==============================================================================


CREATE TABLE physical_asset (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- User-facing label for this asset in dashboards and compliance reports.
    asset_name          VARCHAR(100) NOT NULL,

    -- Category discriminator. Only VEHICLE supported in v0.3.
    -- Written as IN (...) not = 'VEHICLE' so future types (PROPERTY, EQUIPMENT)
    -- can be added via a single ALTER TABLE constraint modification with no data migration.
    asset_type          VARCHAR(50)  NOT NULL,

    -- ── Vehicle identity columns ──────────────────────────────────────────────
    -- These are non-sparse, universal attributes present on EVERY vehicle record.
    -- Keeping them as columns (not JSONB keys) allows direct filtering and display
    -- without JSONB extraction overhead.
    make                VARCHAR(100) NOT NULL,   -- e.g. 'Maruti Suzuki', 'Honda', 'TVS'
    model               VARCHAR(100) NOT NULL,   -- e.g. 'Swift ZXi', 'Activa 6G', 'Apache RTR'

    -- Natural business key. Must be unique — no vehicle can be registered twice.
    registration_number VARCHAR(50)  NOT NULL,

    -- Drives compliance scheduling logic in the application layer (see comment below).
    registration_type   VARCHAR(50)  NOT NULL,

    -- ── Compliance deadlines and enrichment ───────────────────────────────────
    -- All expiry dates, renewal schedules, and optional metadata live here.
    -- Documented schema below; enforced by application, not the database.
    metadata            JSONB        NOT NULL DEFAULT '{}'::jsonb,

    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_physical_asset PRIMARY KEY (id),

    CONSTRAINT chk_asset_type
        CHECK (asset_type IN ('VEHICLE')),

    CONSTRAINT chk_registration_type
        CHECK (registration_type IN (
            'PRIVATE',       -- Standard private plate, typically lifetime road tax.
            'COMMERCIAL',    -- Yellow plate, annual road tax + fitness certificate cycle.
            'GOVERNMENT',    -- White plate with blue strip; custom renewal schedule.
            'BH_SERIES'      -- Bharat Series: biennial road-tax renewal cycle.
        )),

    CONSTRAINT uq_registration_number
        UNIQUE (registration_number)
);


-- ==============================================================================
-- COMMENTS
-- ==============================================================================

COMMENT ON TABLE physical_asset IS $$
Registry of physical assets (currently: vehicles) owned by the household.
Intentionally decoupled from the financial `account` table — a vehicle has no
institution_name, currency, or transactional semantics.

All compliance deadlines (PUC, Insurance, Road Tax) are stored in the `metadata`
JSONB column because they are sparse, structurally different per registration_type,
and fully extensible without schema changes.

The `registration_type` column is kept as a first-class VARCHAR column (not in JSONB)
because it actively drives application-level compliance scheduling:
  BH_SERIES → biennial road-tax renewal cycle.
  PRIVATE   → typically lifetime road tax; annual PUC + insurance.
  COMMERCIAL → annual road tax + fitness certificate on top of PUC + insurance.
$$;

COMMENT ON COLUMN physical_asset.asset_type IS $$
Physical asset category discriminator.
Currently only VEHICLE is supported (v0.3).
The CHECK constraint is intentionally written as IN ('VEHICLE', ...) so future types
such as PROPERTY or EQUIPMENT can be added by modifying the constraint alone —
no new columns, no data migration, no downtime.
$$;

COMMENT ON COLUMN physical_asset.registration_type IS $$
Determines the compliance renewal schedule used by the application's alert engine:

  PRIVATE     — Lifetime road tax (most passenger cars in India). Annual PUC.
                Insurance renewed annually or multi-year.
  COMMERCIAL  — Annual road tax. Annual PUC. Fitness certificate cycle.
                Higher compliance burden; more entries expected in metadata.
  GOVERNMENT  — Custom schedule, tracked manually via metadata keys.
  BH_SERIES   — Bharat Series (national registration). Biennial road-tax renewal.
                Use metadata key "bh_series_renewal_year" for the next renewal year.

This column MUST remain a queryable first-class field — moving it to JSONB would
require expression indexes and break straightforward compliance-filter queries.
$$;

COMMENT ON COLUMN physical_asset.registration_number IS $$
[SENSITIVITY: MEDIUM — PII ASSET DATA]
Unique RTO-issued vehicle registration number. e.g. 'KA-01-AB-1234', 'DL-8C-AM-0001'.
Acts as the natural business key for this entity.
The UNIQUE constraint (uq_registration_number) prevents accidental double-registration.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN physical_asset.metadata IS $$
[SENSITIVITY: MEDIUM — PII ASSET DATA]
JSONB payload for all compliance deadlines and optional vehicle details.
Schema is application-enforced, not DB-enforced.

Recommended structure:
{
  "puc_expiry"             : "YYYY-MM-DD",
  "insurance_expiry"       : "YYYY-MM-DD",
  "insurance_provider"     : "HDFC Ergo",
  "insurance_policy_no"    : "...",
  "road_tax_expiry"        : "YYYY-MM-DD",           -- null if PRIVATE + lifetime tax paid
  "road_tax_type"          : "LIFETIME|ANNUAL|BIENNIAL",
  "bh_series_renewal_year" : 2026,                   -- BH_SERIES only
  "purchase_date"          : "YYYY-MM-DD",
  "chassis_number"         : "...",
  "engine_number"          : "...",
  "hypothecation_bank"     : "SBI",                  -- if vehicle loan is active
  "notes"                  : "..."
}

Adding a new compliance type (e.g., fitness_certificate_expiry for COMMERCIAL vehicles)
requires zero schema migration — the application simply writes a new top-level key.
$$;


-- ==============================================================================
-- INDEXES
-- ==============================================================================

-- GIN index for JSONB queries.
-- e.g. WHERE metadata->>'puc_expiry' < current_date + interval '30 days'
--      or   WHERE metadata @> '{"road_tax_type": "BIENNIAL"}'
CREATE INDEX idx_physical_asset_metadata
    ON physical_asset USING GIN (metadata);

-- Partial index for the dominant application query pattern:
-- "give me all active vehicles by registration type for compliance scheduling."
CREATE INDEX idx_physical_asset_active_type
    ON physical_asset (registration_type)
    WHERE is_active = TRUE;
