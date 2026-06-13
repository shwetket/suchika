-- ==============================================================================
-- V2__remove_enum_constraints.sql
-- Removes the VARCHAR discriminator CHECK constraint from the health schema.
--
-- REASON: See CLAUDE.md "DB Constraint Philosophy".
-- vital_type values (WEIGHT, BLOOD_PRESSURE, etc.) are enforced at the
-- OpenAPI contract + Java layer. No DB migration needed to add a new metric.
--
-- WHAT IS REMOVED (enum discriminator):
--   chk_vital_type — WEIGHT/HEIGHT/BLOOD_PRESSURE/etc. on health.vital_reading
--
-- WHAT STAYS (structural business invariants — these are NOT enum lists):
--   chk_bp_secondary_value  — BLOOD_PRESSURE readings must supply value_secondary
--                             (diastolic). This is a structural data integrity rule
--                             ("if type is X, field Y is required"), not a list of
--                             permitted string values. Kept in DB to prevent NULL
--                             diastolic values from slipping in via any code path.
--   chk_primary_positive    — value_primary > 0: biometric values are always positive.
--   chk_visit_dates         — to_date >= from_date: date ordering is a structural rule.
--   chk_doctor_name_required — visited_doctor = TRUE requires doctor_name NOT NULL.
--                              Conditional required-field rule, not an enum list.
--   All PK/FK constraints
--
-- INDEXES REMOVED:
--   None. Health tables (vital_reading, doctor_visit) are expected to grow to
--   thousands of rows over years of personal use. All existing indexes are justified.
-- ==============================================================================


-- ---- health.vital_reading ----
ALTER TABLE health.vital_reading
    DROP CONSTRAINT IF EXISTS chk_vital_type;
