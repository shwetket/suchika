-- V2__error_log.sql
-- Phase 4 Application Console (ADR-023): general application-error log for the
-- profile service, separate from wealth.upload_error_log (that one is CSV-upload
-- specific). One error_log table per domain schema -- no shared/cross-domain
-- table (ADR-003). No CHECK constraints anywhere (2026-07-05 policy) -- name/
-- message length caps enforced here instead, business validation stays in the
-- domain layer. Populated by ApplicationExceptionMapper's optional
-- ErrorLogRecorder hook (shared/) -- see ProfileErrorLogRecorder.

CREATE TABLE profile.error_log (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    error_code  VARCHAR(50)  NOT NULL,
    http_status INT          NOT NULL,
    message     VARCHAR(500) NOT NULL,
    details     VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_error_log PRIMARY KEY (id)
);

-- Supports GET /v1/errors?since=&limit= (ordered by recency).
CREATE INDEX idx_error_log_created_at ON profile.error_log (created_at);
