-- ==============================================================================
-- V5__remove_enum_constraints.sql
-- Removes all VARCHAR discriminator CHECK constraints from the wealth schema.
--
-- REASON: See V4 header and CLAUDE.md for the full architectural decision.
-- TL;DR: enum values are enforced at the OpenAPI contract + Java layer.
--        No DB migration is needed when a new value is added.
--
-- WHAT IS REMOVED (enum discriminators — now enforced in code):
--   chk_txn_type        — CREDIT/DEBIT values on wealth.transaction
--   chk_registration_type — PRIVATE/COMMERCIAL/etc. on wealth.physical_asset
--   chk_asset_type      — VEHICLE on wealth.physical_asset
--   chk_upload_status   — PENDING/SUCCESS/FAILED on wealth.statement_upload
--   chk_error_type      — error codes on wealth.upload_error_log
--
-- WHAT STAYS (structural invariants, not enum lists):
--   chk_txn_amount       — amount >= 0: amounts are always positive (direction in txn_type)
--   uq_transaction_dedup — native idempotency; not a type constraint
--   uq_registration_number — natural business key; not a type constraint
--   All PK/FK constraints
--
-- INDEXES REMOVED (tables too small to benefit; seq scan is faster):
--   idx_account_active_type    — wealth.account is tiny
--   idx_account_profile_id     — wealth.account is tiny
--   idx_upload_account_date    — wealth.statement_upload is tiny
--   idx_physical_asset_active_type  — physical_asset is tiny
--   idx_physical_asset_profile_id   — physical_asset is tiny
--   idx_physical_asset_metadata     — physical_asset is tiny; GIN overhead unwarranted
--
-- INDEXES KEPT (these tables can grow to thousands of rows):
--   idx_txn_account_date   — core engine scan on wealth.transaction
--   idx_txn_upload_id      — supports ON DELETE CASCADE from statement_upload
--   idx_txn_debit_date     — DEBIT-only aggregation queries
--   idx_txn_metadata       — JSONB queries on transaction metadata
--   idx_upload_status_non_success  — operational: find stuck/failed uploads
--   idx_error_log_upload_id        — "all errors for upload X"
-- ==============================================================================


-- ---- wealth.transaction ----
ALTER TABLE wealth.transaction
    DROP CONSTRAINT IF EXISTS chk_txn_type;


-- ---- wealth.physical_asset ----
ALTER TABLE wealth.physical_asset
    DROP CONSTRAINT IF EXISTS chk_asset_type,
    DROP CONSTRAINT IF EXISTS chk_registration_type;


-- ---- wealth.statement_upload ----
ALTER TABLE wealth.statement_upload
    DROP CONSTRAINT IF EXISTS chk_upload_status;


-- ---- wealth.upload_error_log ----
ALTER TABLE wealth.upload_error_log
    DROP CONSTRAINT IF EXISTS chk_error_type;


-- ---- Remove indexes on small tables ----
DROP INDEX IF EXISTS wealth.idx_account_active_type;
DROP INDEX IF EXISTS wealth.idx_account_profile_id;
DROP INDEX IF EXISTS wealth.idx_upload_account_date;
DROP INDEX IF EXISTS wealth.idx_physical_asset_active_type;
DROP INDEX IF EXISTS wealth.idx_physical_asset_profile_id;
DROP INDEX IF EXISTS wealth.idx_physical_asset_metadata;
