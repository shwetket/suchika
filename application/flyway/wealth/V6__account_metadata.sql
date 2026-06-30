-- ==============================================================================
-- V6__account_metadata.sql
-- Adds a metadata JSONB column to wealth.account.
-- Epic 8 Phase 1 — Classification Foundation & Net Worth Correction
-- (see documents/EPIC8_IMPLEMENTATION_PLAN.md, documents/ARCHITECTURE_DECISIONS.md ADR-016)
--
-- WHY JSONB, NOT NEW COLUMNS:
--   Same rationale already applied to wealth.transaction.metadata (V1) and the
--   project-wide "no SQL ENUMs / sparse data in JSONB" philosophy (CLAUDE.md,
--   ARCHITECTURE_GUIDELINES.md). The fields this column will carry are either
--   enum discriminators (category, liquidity_tier, purpose_tag — values that
--   change by adding a string, never by an ALTER TABLE) or genuinely sparse,
--   account-type-specific facts (joint owners only applies to a handful of
--   shared accounts). Adding each as its own column would mean a new Flyway
--   migration every time Epic 8 introduces a new manual-entry field across
--   Phases 1-4. JSONB lets the application add new keys with zero schema change.
--
-- SHAPE DECIDED NOW SO LATER PHASES NEED NO SECOND MIGRATION
-- (per EPIC8_IMPLEMENTATION_PLAN.md Phase 1 + ADR-016 — keys are reserved here,
--  not all are populated or consumed yet):
--   category        — Use Case 8.1 asset categorization. NOT populated/consumed
--                      until Phase 2. Reserved key only.
--   liquidity_tier   — Use Case 8.1 liquidity tiering. Stored from Phase 1 on,
--                      not consumed by any computation until Phase 3.
--   purpose_tag      — Use Case 8.1 purpose grouping / Use Case 8.6 manual entry.
--                      Stored from Phase 1 on, consumed starting Phase 3.
--   joint_owners     — ADR-016 Decision 2: array of profile_id strings, the
--                      account's co-owners for DISPLAY/ATTRIBUTION ONLY.
--                      Never used as a query predicate — profile_id stays the
--                      single column every query filters on (ADR-006 unchanged).
--                      Not populated until Phase 2 (Kotak joint account).
--   Loan-specific keys (original_principal, loan_start_date,
--   original_tenure_months, linked_offset_account_id — Use Case 8.2 / Phase 3)
--   and growth-rate-assumption keys (Use Case 8.1 / Phase 3) are NOT reserved
--   with placeholder keys here — they are free-form additions to the same
--   generic JSONB column when Phase 3 needs them. No migration required then
--   either; documented here only so the column's purpose is traceable.
--
-- WHAT STAYS A TYPED COLUMN, NOT METADATA:
--   interest_rate already exists as a NUMERIC column (V4). Phase 3's loan
--   amortization work reuses that column rather than duplicating the value in
--   JSONB, to keep a single source of truth (see EPIC8_IMPLEMENTATION_PLAN.md
--   Phase 3 schema notes).
-- ==============================================================================

ALTER TABLE wealth.account
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN wealth.account.metadata IS $$
[SENSITIVITY: MEDIUM — PII FINANCIAL DATA]
Sparse, extensible classification and attribution data for Epic 8. Reserved keys:
  category      : string  — asset category discriminator (Use Case 8.1). Reserved
                   in Phase 1; not populated or consumed until Phase 2.
  liquidity_tier: string  — one of the four Use Case 8.1 liquidity tiers.
  purpose_tag   : string  — one of the Use Case 8.1 purpose groupings.
  joint_owners  : array   — profile_id strings of co-owners, attribution-only.
                   Never used as a query predicate (ADR-016). profile_id remains
                   the single column used to scope every query (ADR-006).
No CHECK constraint by design — these are enum discriminators enforced at the
OpenAPI contract + Java enum layer, consistent with every other discriminator
in this schema (account_type, txn_type, upload status, error_type).
$$;
