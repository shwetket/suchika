-- V6__error_log.sql
-- Phase 4 Application Console (ADR-023): general application-error log for the
-- wealth service. Deliberately separate from wealth.upload_error_log (V1) --
-- that table is CSV-upload-specific (error_type/missing_columns tied to a
-- statement_upload row); this one is for general application errors from any
-- endpoint (ADR-003: no shared/cross-domain error table -- every domain gets
-- its own). No CHECK constraints anywhere (2026-07-05 policy). Populated by
-- ApplicationExceptionMapper's optional ErrorLogRecorder hook (shared/) --
-- see WealthErrorLogRecorder.

CREATE TABLE wealth.error_log (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    error_code  VARCHAR(50)  NOT NULL,
    http_status INT          NOT NULL,
    message     VARCHAR(500) NOT NULL,
    details     VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_error_log PRIMARY KEY (id)
);

-- Supports GET /v1/errors?since=&limit= (ordered by recency).
CREATE INDEX idx_error_log_created_at ON wealth.error_log (created_at);
