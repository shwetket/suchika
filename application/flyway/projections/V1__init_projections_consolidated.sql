CREATE TABLE projections.dashboard_snapshot (
    profile_id    UUID         NOT NULL,
    snapshot_key  VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL,
    calculated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_dashboard_snapshot PRIMARY KEY (profile_id, snapshot_key)
);
