-- ==============================================================================
-- V1__init_health.sql
-- Core schema for the Health & Biometrics domain.
-- Covers: v0.1 Epic 1 — Manual Biometric Logging
--         v0.2 Epic 2 — Biometric History (covered by indexes, not new tables)
--         v0.5 Epic 3 — Profile Association (profile_id FK is included from day one)
--
-- WHY PostgreSQL (not MongoDB)?
--   Health data is time-series with consistent structure per metric type.
--   PostgreSQL with JSONB handles all the flexibility MongoDB offered, while
--   providing ACID guarantees, standard indexing, and a single database to manage.
--
-- IDENTITY BOUNDARY:
--   The old health_profile table is eliminated.
--   "Who" is answered by profile.profile.
--   "What happened to them healthwise" is answered by this domain's tables.
--
-- DESIGN RULES:
--   1. No ENUMs — VARCHAR + named CHECK constraints only.
--   2. Sparse/domain-specific data in JSONB metadata.
--   3. profile_id is NOT NULL here (unlike wealth/household):
--      health data without a known person is meaningless and unsafe.
-- ==============================================================================


-- ==============================================================================
-- TABLE 1: health.vital_reading
-- Time-series biometric measurements for any household member.
-- Covers: v0.1 Use Case 1.1 — Core Metric Entry (Height, Weight initially)
--
-- WHY TWO VALUE COLUMNS (value_primary + value_secondary)?
--   Blood pressure requires two numbers: systolic (value_primary) and
--   diastolic (value_secondary). All other metrics use only value_primary.
--   A JSONB column would also work, but two typed NUMERIC columns give
--   precise aggregate queries (e.g., "average systolic this month") without
--   JSONB extraction overhead on a potentially large time-series table.
-- ==============================================================================

CREATE TABLE health.vital_reading (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    profile_id       UUID          NOT NULL,           -- FK to profile.profile; NOT nullable
    vital_type       VARCHAR(30)   NOT NULL,
    reading_date     DATE          NOT NULL DEFAULT CURRENT_DATE,

    -- Primary value: BP systolic, weight, blood sugar, heart rate, etc.
    value_primary    NUMERIC(8,2)  NOT NULL,

    -- Secondary value: BP diastolic only. NULL for all other vital types.
    value_secondary  NUMERIC(8,2),

    unit             VARCHAR(20)   NOT NULL,           -- 'mmHg', 'kg', 'mg/dL', 'bpm', etc.
    notes            TEXT,

    -- Sparse enrichment: measurement context, device, time-of-day, etc.
    metadata         JSONB         NOT NULL DEFAULT '{}'::jsonb,

    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_vital_reading
        PRIMARY KEY (id),

    CONSTRAINT chk_vital_type
        CHECK (vital_type IN (
            'WEIGHT',
            'HEIGHT',
            'BLOOD_PRESSURE',        -- value_primary = systolic, value_secondary = diastolic
            'BLOOD_SUGAR_FASTING',
            'BLOOD_SUGAR_PP',        -- post-prandial (2 hours after eating)
            'HEART_RATE',
            'TEMPERATURE',
            'OXYGEN_SATURATION',
            'BMI',                   -- calculated; stored for history
            'WAIST_CIRCUMFERENCE'
        )),

    -- Blood pressure requires both columns; all other types use only value_primary.
    CONSTRAINT chk_bp_secondary_value
        CHECK (vital_type != 'BLOOD_PRESSURE' OR value_secondary IS NOT NULL),

    CONSTRAINT chk_primary_positive
        CHECK (value_primary > 0),

    CONSTRAINT fk_vital_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE health.vital_reading IS $$
Time-series biometric measurement ledger. One row per reading per person.
All metric types share this table — vital_type discriminates the measurement.

profile_id is NOT NULL: health data without a named person is meaningless.
Covered metrics include the v0.1 scope (Height, Weight) and are extensible via
the CHECK constraint without data migration.
$$;

COMMENT ON COLUMN health.vital_reading.vital_type IS $$
Biometric measurement discriminator. VARCHAR + CHECK — not ENUM — so new metric
types (e.g., 'HBA1C', 'CHOLESTEROL') can be added by modifying the constraint alone.
$$;

COMMENT ON COLUMN health.vital_reading.value_secondary IS $$
Used ONLY for BLOOD_PRESSURE (diastolic reading).
NULL for all other vital_type values.
The chk_bp_secondary_value constraint enforces that BP readings always supply both values.
$$;

COMMENT ON COLUMN health.vital_reading.metadata IS $$
JSONB payload for measurement context. Recommended keys:
{
  "time_of_day":  "MORNING|AFTERNOON|EVENING|NIGHT",
  "device":       "Omron HEM-7120 (BP machine)",
  "fasting_hours": 12,                        -- for blood sugar readings
  "context":      "Post-exercise reading"
}
$$;

-- Primary query: "all readings for person X, newest first" — chronological review.
CREATE INDEX idx_vital_profile_date
    ON health.vital_reading (profile_id, reading_date DESC);

-- Filter by vital type: "all weight readings for person X this year".
CREATE INDEX idx_vital_type_date
    ON health.vital_reading (profile_id, vital_type, reading_date DESC);


-- ==============================================================================
-- TABLE 2: health.doctor_visit
-- Episodic illness and doctor visit record.
-- Covers: future v0.x — medical event tracking
--
-- WHY NOT wait for a later version?
--   The table design is well-understood and the schema is simple. Deferring it
--   adds no value; including it now avoids a future migration to a live DB.
--
-- NOTE: scope restriction from BRD Use Case 5.1 applies —
--   this is a WELLNESS tool. No diagnosis algorithms. No HIPAA scope.
--   This table stores personal notes, not medical records.
-- ==============================================================================

CREATE TABLE health.doctor_visit (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id      UUID         NOT NULL,             -- FK to profile.profile; NOT nullable
    from_date       DATE         NOT NULL,
    to_date         DATE,                              -- NULL for single-day or ongoing

    -- TRUE = an actual doctor was seen. FALSE = illness period, no visit.
    visited_doctor  BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Doctor and facility details (required when visited_doctor = TRUE).
    doctor_name     VARCHAR(100),
    hospital_name   VARCHAR(200),
    speciality      VARCHAR(100),

    -- Personal wellness notes — NOT medical records or diagnoses.
    symptoms        TEXT,
    diagnosis       TEXT,
    notes           TEXT,
    follow_up_date  DATE,

    -- Sparse enrichment: prescription reference, test results, etc.
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_doctor_visit
        PRIMARY KEY (id),

    CONSTRAINT chk_visit_dates
        CHECK (to_date IS NULL OR to_date >= from_date),

    -- Logical gate: if you visited a doctor, you must record who.
    CONSTRAINT chk_doctor_name_required
        CHECK (visited_doctor = FALSE OR doctor_name IS NOT NULL),

    CONSTRAINT fk_visit_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE health.doctor_visit IS $$
Personal wellness event log: illness periods and doctor visits.
Scope: personal wellness notes only. Not a medical record system.

visited_doctor = FALSE captures illness periods where no doctor was seen
(fever tracked at home, for example). Doctor fields are NULL in this case.
visited_doctor = TRUE requires doctor_name (enforced by chk_doctor_name_required).
$$;

COMMENT ON COLUMN health.doctor_visit.metadata IS $$
JSONB payload for sparse visit details. Recommended keys:
{
  "prescription_ref": "...",
  "tests_ordered":    ["CBC", "Thyroid Panel"],
  "test_results_url": "...",
  "insurance_claim":  false
}
$$;

CREATE INDEX idx_visit_profile_date
    ON health.doctor_visit (profile_id, from_date DESC);

CREATE INDEX idx_visit_follow_up
    ON health.doctor_visit (follow_up_date)
    WHERE follow_up_date IS NOT NULL;
