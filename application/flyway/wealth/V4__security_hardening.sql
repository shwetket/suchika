-- ==============================================================================
-- V4__security_hardening.sql
-- Database-level access control hardening for v1.0 production deployment.
-- Covers: v1.0 Epic 7 — Data Hardening (Use Case 7.1)
--
-- SCOPE OF THIS MIGRATION:
--   1. Revoke residual PUBLIC schema privileges.
--   2. Apply fine-grained, per-table DML grants to the application runtime role.
--   3. Document the data sensitivity classification for audit purposes.
--
-- WHAT THIS MIGRATION DELIBERATELY DOES NOT DO:
--   Column-level pgcrypto encryption is intentionally excluded.
--   Applying pgcrypto would require changing NUMERIC(19,4) → BYTEA on amount
--   columns, destroying all aggregate queries and the mathematical engine.
--   Encryption at rest is handled at the infrastructure layer (see checklist below).
--
-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │  INFRASTRUCTURE ENCRYPTION CHECKLIST (outside this migration)               │
-- ├──────────────────────┬──────────────────────────────────────────────────────┤
-- │  OS / Block Storage  │ Linux : enable LUKS on the PostgreSQL data volume.   │
-- │                      │ macOS : enable FileVault (covers local dev).         │
-- ├──────────────────────┼──────────────────────────────────────────────────────┤
-- │  Backup Encryption   │ pgBackRest: set cipher-type=aes-256-cbc in config.  │
-- │                      │ pg_dump   : pipe output through gpg before storage. │
-- ├──────────────────────┼──────────────────────────────────────────────────────┤
-- │  TLS in Transit      │ postgresql.conf: ssl = on                           │
-- │                      │                 ssl_min_protocol_version = TLSv1.2  │
-- ├──────────────────────┼──────────────────────────────────────────────────────┤
-- │  Role Passwords      │ Set passwords via \password or secrets manager.     │
-- │                      │ Never store passwords in SQL files or version control│
-- └──────────────────────┴──────────────────────────────────────────────────────┘
--
-- GRANT RATIONALE PER TABLE:
--
--   account            SELECT, INSERT
--                      Accounts are seeded from application.properties.
--                      No app-layer UPDATE — account config is not user-editable.
--
--   statement_upload   SELECT, INSERT, UPDATE, DELETE
--                      INSERT   : create the PENDING row before parsing begins.
--                      UPDATE   : transition status PENDING → SUCCESS | FAILED.
--                      DELETE   : controlled rollback — triggers CASCADE to
--                                 transaction and upload_error_log.
--
--   transaction        SELECT, INSERT  ← IMMUTABLE LEDGER
--                      No UPDATE. No direct DELETE.
--                      Deletes flow exclusively through CASCADE from statement_upload.
--                      This constraint is enforced at the DB grant level, not just policy.
--
--   physical_asset     SELECT, INSERT, UPDATE
--                      UPDATE required: compliance metadata (PUC, insurance expiry)
--                      must be refreshable when renewals occur.
--                      No DELETE — soft-delete via is_active = FALSE.
--
--   upload_error_log   SELECT, INSERT
--                      Written once on parse failure. Never updated.
--                      Cleanup is via CASCADE from statement_upload.
-- ==============================================================================


-- ==============================================================================
-- PART 1: Revoke residual PUBLIC schema privileges
-- The bootstrap script revokes ALL on schema public from PUBLIC, but Flyway
-- itself may reconnect under a session that re-exposes defaults. This is a
-- belt-and-suspenders step that is safe to run multiple times.
-- ==============================================================================

REVOKE CREATE ON SCHEMA public FROM PUBLIC;


-- ==============================================================================
-- PART 2: Fine-grained DML grants to the application runtime role
-- ==============================================================================

-- account: read and seed only. No UPDATE via the application layer.
GRANT SELECT, INSERT
    ON TABLE account
    TO wealth_app_user;

-- statement_upload: full DML for the upload lifecycle.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLE statement_upload
    TO wealth_app_user;

-- transaction: SELECT + INSERT only. This enforces the immutability contract
-- at the database privilege level — not just application convention.
GRANT SELECT, INSERT
    ON TABLE transaction
    TO wealth_app_user;

-- physical_asset: compliance metadata must be updatable on renewal.
GRANT SELECT, INSERT, UPDATE
    ON TABLE physical_asset
    TO wealth_app_user;

-- upload_error_log: write once on failure; cleanup via CASCADE.
GRANT SELECT, INSERT
    ON TABLE upload_error_log
    TO wealth_app_user;


-- ==============================================================================
-- PART 3: Data sensitivity manifest
-- Inline documentation for auditors and future developers.
-- Identifies which columns are PII / financial data targets for any future
-- upgrade to column-level encryption (e.g., pgcrypto application-layer AES).
-- ==============================================================================

COMMENT ON COLUMN transaction.amount IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
Always a positive absolute value. NUMERIC(19,4) provides sufficient decimal precision
for Indian Rupee transactions without floating-point rounding errors.
Direction of money flow is encoded in txn_type — never in this column's sign.
Infrastructure encryption at rest required.
Future upgrade path: application-layer AES encryption before INSERT would require
changing this column to BYTEA and rewriting all aggregate queries — avoid unless
there is an explicit regulatory mandate for column-level encryption.
$$;

COMMENT ON COLUMN transaction.description IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
Raw narration string from the source CSV. May contain: merchant names, UPI IDs,
beneficiary names, cheque numbers, reference codes that directly identify the account holder.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN transaction.metadata IS $$
[SENSITIVITY: HIGH — PII FINANCIAL DATA]
JSONB enrichment payload. May contain NAV, Units, EMI splits, loan reference numbers,
and other domain-specific fields that are personally identifiable depending on
the source financial institution's statement format.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN account.account_name IS $$
[SENSITIVITY: MEDIUM — PII FINANCIAL DATA]
User-assigned account alias. May contain personally identifiable labels
such as 'Mahesh Salary SBI' or 'Joint FD HDFC'.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN physical_asset.registration_number IS $$
[SENSITIVITY: MEDIUM — PII ASSET DATA]
Unique RTO-issued vehicle registration number. Directly identifies the owner's physical asset.
Infrastructure encryption at rest required.
$$;

COMMENT ON COLUMN physical_asset.metadata IS $$
[SENSITIVITY: MEDIUM — PII ASSET DATA]
JSONB payload containing compliance deadlines, insurance provider and policy details,
chassis and engine numbers. All fields are personally identifiable.
Infrastructure encryption at rest required.
$$;
