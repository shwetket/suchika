-- ==============================================================================
-- V1__init_household.sql
-- Core schema for Household Operations.
-- Covers: v0.1 Epic 1 — Family Calendar & Supply Chain Ingestion
--
-- IDENTITY BOUNDARY:
--   "Who" is in the profile domain. The household domain records "what they do"
--   and "what they consume". All household tables hold a profile_id FK pointing
--   INTO profile.profile — never the reverse.
--
-- DESIGN RULES:
--   1. No ENUMs — VARCHAR + named CHECK constraints only.
--   2. Sparse/optional data lives in JSONB metadata columns.
--   3. profile_id is nullable on all tables: NULL = assigned to the admin profile.
-- ==============================================================================


-- ==============================================================================
-- TABLE 1: household.calendar_event
-- Chronological event ledger for all household members.
-- Covers: v0.1 Use Case 1.2 — Event Scheduling
--
-- WHY a single flat table (not a hierarchical parent/child structure yet)?
--   v0.1 scope is a happy-path flat event list. Multi-day grouping (master/sub-event
--   hierarchy) is deferred to v0.2 Epic 3. A parent_event_id self-referential FK
--   will be added in V2 when that scope is reached.
-- ==============================================================================

CREATE TABLE household.calendar_event (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id      UUID,                               -- FK to profile.profile
    title           VARCHAR(200) NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE,                               -- NULL for single-day events
    location        VARCHAR(200),
    notes           TEXT,

    -- Sparse enrichment: recurrence rules, travel details, guest lists, etc.
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_calendar_event
        PRIMARY KEY (id),

    CONSTRAINT chk_event_type
        CHECK (event_type IN (
            'PERSONAL',         -- Individual activities (appointments, errands).
            'FAMILY',           -- Whole-household events (holidays, gatherings).
            'SCHOOL',           -- Child school events, exams, parent meetings.
            'TRAVEL',           -- Trips and journeys (may span multiple days).
            'MEDICAL',          -- Doctor visits, health checkups.
            'WORK',             -- Work-related events, deadlines.
            'ANNIVERSARY',      -- Birthdays, anniversaries, milestones.
            'OTHER'
        )),

    -- End date must be on or after start date when provided.
    CONSTRAINT chk_event_dates
        CHECK (end_date IS NULL OR end_date >= start_date),

    CONSTRAINT fk_event_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE household.calendar_event IS $$
Chronological event ledger for all household members.
profile_id identifies who this event belongs to. NULL = admin profile.

Multi-day event grouping (master/sub-event hierarchy) is out of scope until v0.2.
A parent_event_id self-referential FK will be added in the appropriate V2 migration.
$$;

COMMENT ON COLUMN household.calendar_event.event_type IS $$
Event category discriminator. VARCHAR + CHECK — not ENUM — so new types can be added
by modifying the constraint alone. No data migration required.
$$;

-- "All events for household member X ordered by start date" — primary query.
CREATE INDEX idx_event_profile_start
    ON household.calendar_event (profile_id, start_date DESC);

-- Date range queries: "all events in a given month".
CREATE INDEX idx_event_start_date
    ON household.calendar_event (start_date);


-- ==============================================================================
-- TABLE 2: household.inventory_item
-- Unified grocery and supply chain item ledger.
-- Covers: v0.1 Use Case 2.1 — Order History Parsing
--
-- WHY NOT a separate "order" parent table?
--   v0.1 scope is extraction into a flat, consolidated ledger. Platform-specific
--   order groupings are irrelevant once items are normalised. The source platform
--   and order reference are stored in metadata for traceability.
-- ==============================================================================

CREATE TABLE household.inventory_item (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id      UUID,                               -- FK to profile.profile

    -- Normalised item name. Raw name from the platform is preserved in metadata.
    item_name       VARCHAR(200) NOT NULL,

    -- Standardised quantity as extracted from the source file.
    quantity        NUMERIC(10,3) NOT NULL,
    unit            VARCHAR(20)  NOT NULL,

    -- Platform the order was imported from.
    source_platform VARCHAR(50)  NOT NULL,

    purchase_date   DATE         NOT NULL,

    -- Item category for inventory grouping and supply-chain analysis.
    category        VARCHAR(50),

    -- Traceability: raw platform item name, order ID, import metadata.
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_inventory_item
        PRIMARY KEY (id),

    CONSTRAINT chk_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_source_platform
        CHECK (source_platform IN (
            'INSTAMART',
            'FLIPKART_GROCERY',
            'COUNTRY_DELIGHT',
            'AMAZON_FRESH',
            'BLINKIT',
            'ZEPTO',
            'MANUAL',           -- Hand-entered items, not from a parsed file.
            'OTHER'
        )),

    CONSTRAINT chk_unit
        CHECK (unit IN (
            'KG', 'G',          -- Weight
            'L', 'ML',          -- Volume
            'UNITS',            -- Count
            'PACK',             -- Multi-item packs
            'DOZEN',
            'OTHER'
        )),

    CONSTRAINT fk_item_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE household.inventory_item IS $$
Unified grocery and supply chain item ledger. One row per item line extracted
from an external grocery order export. Platform-specific formatting is normalised;
the raw data is preserved in the metadata JSONB column for traceability.
$$;

COMMENT ON COLUMN household.inventory_item.metadata IS $$
JSONB traceability payload. Recommended structure:
{
  "raw_item_name":   "Amul Taaza Toned Milk 1L",
  "platform_order_id": "OD-12345678",
  "import_timestamp":  "2025-06-01T10:30:00Z",
  "unit_price":        "68.00"
}
$$;

-- "All items consumed by household member X" — primary query.
CREATE INDEX idx_item_profile_date
    ON household.inventory_item (profile_id, purchase_date DESC);

-- "All items in a given category" — for supply chain analysis.
CREATE INDEX idx_item_category
    ON household.inventory_item (category)
    WHERE category IS NOT NULL;

-- GIN for JSONB metadata queries.
CREATE INDEX idx_item_metadata
    ON household.inventory_item USING GIN (metadata)
    WHERE metadata != '{}'::jsonb;
