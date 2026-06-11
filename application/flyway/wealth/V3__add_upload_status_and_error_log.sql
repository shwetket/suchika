-- ==============================================================================
-- V3__add_upload_status_and_error_log.sql
-- Adds upload lifecycle tracking and structured error logging for file rejection.
-- Covers: v0.5 Epic 6 — Malformed Data Rejection (Use Case 6.1)
--
-- STATE MACHINE:
--
--   [App creates statement_upload row]
--            │  status = PENDING
--            │
--            ├─── Parse succeeds ──────────────► status = SUCCESS
--            │
--            └─── Parse fails (missing cols,
--                 bad encoding, etc.)  ────────► status = FAILED
--                                                + INSERT into upload_error_log
--                                                  (FK → statement_upload)
--
-- TWO-STEP DEFAULT TRICK (explained):
--   When the status column is added, existing rows (uploaded under V1/V2) must
--   not be left as PENDING — their existence in the table is proof of a
--   historically successful ingestion. Setting the initial DEFAULT to 'SUCCESS'
--   backfills all legacy rows correctly in a single DDL statement.
--   We immediately flip the column default to 'PENDING' so all future inserts
--   start in the correct lifecycle state. Zero UPDATE needed, zero data backfill.
--
-- CASCADE STRATEGY:
--   upload_error_log.upload_id has ON DELETE CASCADE.
--   When a failed upload record is cleaned up (e.g., user retries), its error
--   log entries are automatically removed — no orphan cleanup required.
-- ==============================================================================


-- ==============================================================================
-- PART 1: Add status lifecycle column to statement_upload
-- ==============================================================================

-- Step A: Add column. All existing rows (V1/V2 era) receive 'SUCCESS' as their
--         status — their presence proves they were historically successful.
ALTER TABLE statement_upload
    ADD COLUMN status VARCHAR(20) NOT NULL
        DEFAULT 'SUCCESS'
        CONSTRAINT chk_upload_status
            CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

-- Step B: Flip the default to 'PENDING' for all future inserts.
--         New uploads now start as PENDING and the application must explicitly
--         transition them to SUCCESS or FAILED after the parse attempt completes.
ALTER TABLE statement_upload
    ALTER COLUMN status SET DEFAULT 'PENDING';

COMMENT ON COLUMN statement_upload.status IS $$
Upload lifecycle state. Three valid transitions:

  PENDING  — Row created; CSV parsing is in progress.
             Application must transition this within the same request/job.
  SUCCESS  — All rows successfully extracted and inserted into the transaction ledger.
  FAILED   — Parsing aborted. At least one row exists in upload_error_log for this upload_id.
             The application may allow the user to delete this record (which cascades
             the cleanup to any partially-inserted transactions and the error log).

MIGRATION NOTE (V3):
  Rows created before this migration (V1/V2 era) are stamped SUCCESS.
  Their existence in the table is empirical proof of a successful historical ingestion.
  New inserts default to PENDING via a two-step ALTER TABLE technique (no UPDATE needed).
$$;

-- Partial index for operational monitoring: find stuck PENDING or failed uploads.
-- Intentionally excludes SUCCESS rows (the vast majority) to keep the index lean.
CREATE INDEX idx_upload_status_non_success
    ON statement_upload (status, upload_date DESC)
    WHERE status IN ('PENDING', 'FAILED');


-- ==============================================================================
-- PART 2: upload_error_log
-- Structured error record for every file rejected during parsing.
-- Populated only when a statement_upload transitions to FAILED.
-- ==============================================================================

CREATE TABLE upload_error_log (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- FK to the PENDING statement_upload row created before parsing began.
    -- CASCADE: cleaning up a FAILED upload automatically removes its error entries.
    upload_id       UUID         NOT NULL,

    -- Standardised error code for programmatic handling by the parser and UI.
    -- Allows the application to distinguish structural file problems (missing headers)
    -- from runtime problems (corrupt file) without parsing free-text messages.
    error_type      VARCHAR(50)  NOT NULL,

    -- PostgreSQL TEXT ARRAY: the specific column names that were expected but absent.
    -- e.g. ARRAY['transaction_date', 'withdrawal_amt']
    -- NULL when the error is not about missing column names (PARSE_ERROR, VALIDATION_ERROR).
    missing_columns TEXT[],

    -- Human-readable, user-facing explanation of the failure.
    -- Should include what was searched for and what was actually found.
    -- e.g. "Could not identify a date column. Searched for: [Date, Txn Date, Value Date].
    --       Found in file: [Sl No, Description, Debit, Credit, Balance]."
    error_detail    TEXT         NOT NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_upload_error_log    PRIMARY KEY (id),
    CONSTRAINT fk_error_log_upload    FOREIGN KEY (upload_id)
                                          REFERENCES statement_upload(id)
                                          ON DELETE CASCADE,
    CONSTRAINT chk_error_type
        CHECK (error_type IN (
            'MISSING_DATE_COLUMN',      -- No recognisable date header found in the file.
            'MISSING_AMOUNT_COLUMN',    -- No recognisable amount/debit/credit header found.
            'MISSING_REQUIRED_COLUMN',  -- Catch-all: one or more required headers are absent.
            'PARSE_ERROR',              -- File is unreadable (binary, corrupt, wrong encoding).
            'VALIDATION_ERROR'          -- Headers found but row data fails format validation.
        ))
);


-- ==============================================================================
-- COMMENTS
-- ==============================================================================

COMMENT ON TABLE upload_error_log IS $$
Structured error log for file uploads rejected during CSV parsing.
Rows are inserted here ONLY when a statement_upload transitions to FAILED status.

PURPOSE:
  Provides the user with actionable, precise feedback: which column headers were
  absent or unrecognisable, and a human-readable explanation sufficient to fix
  and re-upload the file.

CARDINALITY:
  One upload_id may produce multiple error rows (e.g., both date AND amount columns
  missing generate two separate MISSING_*_COLUMN entries).

CLEANUP:
  Rows are cascade-deleted when their parent statement_upload record is removed.
  The application does not need to issue explicit DELETEs against this table.
$$;

COMMENT ON COLUMN upload_error_log.error_type IS $$
Programmatic error classification. Standardised codes allow the application and UI
to handle each case distinctly without parsing the free-text error_detail field.

  MISSING_DATE_COLUMN     — Parser scanned all headers; none matched any known date
                            column alias (Date, Txn Date, Value Date, Posting Date, etc.).
  MISSING_AMOUNT_COLUMN   — Parser found no recognisable amount/debit/credit column.
  MISSING_REQUIRED_COLUMN — Generic: one or more mandatory headers are absent.
                            Use when both date and amount are missing simultaneously.
  PARSE_ERROR             — File could not be read at all (binary file, BOM issues,
                            unsupported encoding, truncated CSV).
  VALIDATION_ERROR        — Column headers are present but data rows fail format checks
                            (e.g., amount column contains non-numeric strings).
$$;

COMMENT ON COLUMN upload_error_log.missing_columns IS $$
PostgreSQL TEXT ARRAY of the specific column names that were expected but not found.
Populated by the application layer; not enforced by the database.

Example values:
  ARRAY['transaction_date', 'withdrawal_amt']
  ARRAY['Date']

Convention: set to NULL for PARSE_ERROR and VALIDATION_ERROR, since those errors
are not about missing column names.
$$;

COMMENT ON COLUMN upload_error_log.error_detail IS $$
Human-readable, user-facing error message. The application should include enough
context for the user to diagnose the problem and fix the file.

Recommended format:
  "Could not identify a date column.
   Searched for: [Date, Txn Date, Value Date, Posting Date, Trans. Date].
   Headers found in file: [Sl No, Description, Ref No, Debit, Credit, Balance]."
$$;


-- ==============================================================================
-- INDEXES
-- ==============================================================================

-- Primary access pattern: fetch all error entries for a given upload record.
-- Used when the UI displays the rejection reason after a FAILED upload.
CREATE INDEX idx_error_log_upload_id
    ON upload_error_log (upload_id);
