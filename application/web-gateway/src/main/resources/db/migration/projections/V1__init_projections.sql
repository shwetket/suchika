-- ==============================================================================
-- V1__init_projections.sql
-- CQRS Read Model — dashboard snapshot store.
--
-- PURPOSE:
--   The projections schema holds pre-computed, on-demand calculation results.
--   The application runs a calculation once, stores the result here, and serves
--   it from this table on every dashboard load — no re-computation per request.
--   When the user triggers a refresh, the result is overwritten via UPSERT.
--
-- DESIGN:
--   A single generic table with a (profile_id, snapshot_key) primary key.
--   The payload column is JSONB — each snapshot type has its own shape.
--   This gives MongoDB-style document flexibility without a second database.
--
-- CALCULATION FLOW:
--   1. Frontend triggers "Refresh Dashboard" (or a scheduled job runs)
--   2. web-gateway calls each domain service to aggregate data
--   3. Result is UPSERTed:
--        INSERT INTO projections.dashboard_snapshot (...) VALUES (...)
--        ON CONFLICT (profile_id, snapshot_key) DO UPDATE
--          SET payload = EXCLUDED.payload,
--              calculated_at = now();
--   4. Frontend reads from this table — always fast, never blocks on calculation
--
-- SNAPSHOT KEY CONVENTION:
--   All-caps, underscore-separated. Include a time period where relevant.
--   Examples:
--     'WEALTH_NET_WORTH'            — current net worth across all accounts
--     'WEALTH_MONTHLY_EXPENSE_2025_06'  — monthly spend for June 2025
--     'HEALTH_BMI_TREND_90D'        — BMI trend for the last 90 days
--     'HOUSEHOLD_UPCOMING_EVENTS_7D'    — events in the next 7 days
-- ==============================================================================


CREATE TABLE projections.dashboard_snapshot (
    profile_id     UUID          NOT NULL,
    snapshot_key   VARCHAR2(100) NOT NULL,
    payload        JSONB         NOT NULL,
    calculated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- Composite PK: one snapshot per (person, snapshot type).
    -- The UPSERT pattern relies on this constraint for ON CONFLICT targeting.
    CONSTRAINT pk_dashboard_snapshot
        PRIMARY KEY (profile_id, snapshot_key),

    CONSTRAINT fk_snapshot_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE projections.dashboard_snapshot IS $$
CQRS Read Model: pre-computed dashboard calculation results.

One row per (profile_id, snapshot_key) combination.
All writes use INSERT ... ON CONFLICT DO UPDATE (UPSERT) — no explicit DELETEs.

Payload shape examples:

  snapshot_key = 'WEALTH_NET_WORTH'
  payload = {
    "total_savings":     1250000.00,
    "total_investments": 3400000.00,
    "total_loans":        890000.00,
    "net_worth":         3760000.00,
    "currency":          "INR"
  }

  snapshot_key = 'WEALTH_MONTHLY_EXPENSE_2025_06'
  payload = {
    "total_debits":     42500.00,
    "by_account": [
      { "account_name": "Salary SBI", "debits": 15000.00 },
      { "account_name": "CC HDFC",    "debits": 27500.00 }
    ]
  }

  snapshot_key = 'HEALTH_BMI_TREND_90D'
  payload = {
    "readings": [
      { "date": "2025-06-01", "bmi": 24.5 },
      { "date": "2025-05-01", "bmi": 24.8 }
    ],
    "trend": "IMPROVING"
  }
$$;

COMMENT ON COLUMN projections.dashboard_snapshot.snapshot_key IS $$
Identifies the calculation type and its time scope.
Convention: DOMAIN_METRIC_PERIOD (all caps, underscores).
Period suffix examples: _7D, _30D, _90D, _YTD, _2025_06.
Timeless snapshots (e.g., WEALTH_NET_WORTH) have no period suffix.
$$;

COMMENT ON COLUMN projections.dashboard_snapshot.calculated_at IS $$
Timestamp of the last successful calculation run.
The UI can display "Last updated: X minutes ago" from this field.
$$;

-- Secondary access pattern: "show me all snapshots that are stale (older than N minutes)".
-- Useful for a future background refresh scheduler.
CREATE INDEX idx_snapshot_calculated_at
    ON projections.dashboard_snapshot (calculated_at);
