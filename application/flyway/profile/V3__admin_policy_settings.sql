-- ==============================================================================
-- V3__admin_policy_settings.sql
-- Adds policy_settings JSONB column to profile.admin.
--
-- PURPOSE:
--   Stores household-level policy thresholds consumed by the Epic 8 wealth
--   intelligence engine (goals engine in web-gateway). Kept as JSONB so that
--   new policy keys can be introduced without a schema change.
--
-- DESIGN DECISIONS:
--   - No CHECK constraint on the JSON structure — keys and value ranges are
--     validated at the application layer (OpenAPI contract + Java service).
--   - DEFAULT '{}'::jsonb — every existing admin row gets an empty object;
--     the engine treats any absent key as "use the built-in default".
--   - NOT NULL — a NULL value would require null-handling at every read site;
--     an empty object is the correct sentinel.
-- ==============================================================================

ALTER TABLE profile.admin
    ADD COLUMN policy_settings JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN profile.admin.policy_settings IS $$
Household-level policy thresholds for the Epic 8 wealth intelligence engine.
Keys (all optional — omit means use the engine default):
  monthly_budget_cap              : number  — total monthly household budget cap (₹)
  debt_crossover_threshold_percent: number  — max EMI-to-net-worth ratio (0–100)
  freedom_runway_months           : number  — target months of liquid runway
  insurance_multiple              : number  — investment-to-annual-income multiple for self-insurance
  year_one_annual_target          : number  — first-year savings milestone (₹)
No CHECK constraint — keys enforced at application layer.
$$;
