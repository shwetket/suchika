-- ==============================================================================
-- V3__upload_status.sql
-- Upload lifecycle tracking and structured error logging for file rejection.
-- Covers: v0.5 Epic 6 — Malformed Data Rejection (Use Case 6.1)
--
-- STATE MACHINE:
--
--   [App creates statement_upload row]
--            │  status = PENDING
--            ├─── Parse succeeds ──────────────► status = SUCCESS
--            └─── Parse fails ─────────────────► status = FAILED
--                                                + INSERT into upload_error_log
--
-- TWO-STEP DEFAULT TRICK:
--   When the status column is added, existing V1/V2-era rows receive SUCCESS
--   because their presence in the table proves a historically successful ingest.
--   The default is then flipped to PENDING for all future inserts.
--   Zero UPDATE statements needed.
--
-- CASCADE STRATEGY:
--   upload_error_log.upload_id has ON DELETE CASCADE.
--   When a failed upload is cleaned up, its error log entries are automatically
--   removed — no orphan cleanup required.
-- ==============================================================================


-- ==============================================================================
-- PART 1: Add status lifecycle column to wealth.statement_upload
-- ==============================================================================

-- Step A: Add column; stamp all existing rows SUCCESS.
ALTER TABLE wealth.statement_upload
    ADD COLUMN status VARCHAR(20) NOT NULL
        DEFAULT 'SUCCESS'
        CONSTRAINT chk_upload_status
            CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

-- Step B: Flip default to PENDING for all future inserts.
ALTER TABLE wealth.statement_upload
    ALTER COLUMN status SET DEFAULT 'PENDING';

COMMENT ON COLUMN wealth.statement_upload.status IS $$
Upload lifecycle state:
  PENDING  — Row created; CSV parsing is in progress.
             Rows stuck in PENDING indicate a crashed or interrupted parse job.
  SUCCESS  — All rows extracted and inserted into the transaction ledger cleanly.
  FAILED   — Parsing aborted. At least one row exists in upload_error_log.
             User may delete this record (triggers CASCADE cleanup of any partially
             inserted transactions and the error log entries) then re-upload.

Migration note (V3): pre-existing rows are stamped SUCCESS. New inserts default
to PENDING via the two-step ALTER TABLE technique above.
$$;

-- Partial index: PENDING and FAILED rows are operationally interesting.
-- SUCCESS rows (the vast majority) are excluded, keeping this index lean.
CREATE INDEX idx_upload_status_non_success
    ON wealth.statement_upload (status, upload_date DESC)
    WHERE status IN ('PENDING', 'FAILED');


-- ==============================================================================
-- PART 2: wealth.upload_error_log
-- Structured error record for every file rejected during CSV parsing.
-- ==============================================================================

CREATE TABLE wealth.upload_error_log (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- FK to the PENDING statement_upload row created before parsing began.
    upload_id       UUID         NOT NULL,

    -- Standardised error code for programmatic handling by the parser and UI.
    error_type      VARCHAR(50)  NOT NULL,

    -- The specific column names that were expected but absent.
    -- NULL when the error is not about missing column names.
    missing_columns TEXT[],

    -- Human-readable, user-facing explanation of the failure.
    error_detail    TEXT         NOT NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_upload_error_log
        PRIMARY KEY (id),
    CONSTRAINT fk_error_log_upload
        FOREIGN KEY (upload_id)
        REFERENCES wealth.statement_upload(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_error_type
        CHECK (error_type IN (
            'MISSING_DATE_COLUMN',      -- No recognisable date header found.
            'MISSING_AMOUNT_COLUMN',    -- No recognisable amount/debit/credit header.
            'MISSING_REQUIRED_COLUMN',  -- One or more required headers absent.
            'PARSE_ERROR',              -- File unreadable (binary, corrupt, wrong encoding).
            'VALIDATION_ERROR'          -- Headers found but row data fails format checks.
        ))
);

COMMENT ON TABLE wealth.upload_error_log IS $$
Structured error log for CSV file uploads rejected during parsing.
Rows exist here ONLY when a statement_upload transitions to FAILED status.

One upload_id may produce multiple error rows (e.g., both date AND amount columns
missing generate two separate MISSING_*_COLUMN entries).

Rows are cascade-deleted when their parent statement_upload record is removed.
$$;

COMMENT ON COLUMN wealth.upload_error_log.error_type IS $$
Programmatic error classification. Allows the app and UI to handle each case
distinctly without parsing the free-text error_detail field.
$$;

COMMENT ON COLUMN wealth.upload_error_log.error_detail IS $$
Human-readable, user-facing error message. Should include enough context
for the user to diagnose the problem and re-upload a corrected file.

Recommended format:
  "Could not identify a date column.
   Searched for: [Date, Txn Date, Value Date, Posting Date].
   Headers found in file: [Sl No, Description, Ref No, Debit, Credit, Balance]."
$$;

-- Primary access pattern: "fetch all error entries for upload X".
CREATE INDEX idx_error_log_upload_id
    ON wealth.upload_error_log (upload_id);
