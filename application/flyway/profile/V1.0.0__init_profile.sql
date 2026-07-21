CREATE TABLE profile.admin (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    display_name    VARCHAR(50)  NOT NULL,
    email_address   VARCHAR(255),
    policy_settings JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_admin PRIMARY KEY (id),
    CONSTRAINT uq_admin_email UNIQUE (email_address)
);

CREATE TABLE profile.profile (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    admin_id          UUID         NOT NULL,
    full_name         VARCHAR(50)  NOT NULL,
    dob               DATE         NOT NULL,
    relation_to_admin VARCHAR(30)  NOT NULL,
    email_address     VARCHAR(255),
    gender            VARCHAR(30),
    blood_type        VARCHAR(10),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_profile PRIMARY KEY (id),
    CONSTRAINT fk_profile_admin
        FOREIGN KEY (admin_id)
        REFERENCES profile.admin(id)
        ON DELETE RESTRICT
);

-- Structural invariant: each admin has exactly one SELF profile (their own member record).
-- Partial unique index (not a table CHECK/UNIQUE constraint) so multiple non-SELF
-- profiles per admin remain unrestricted.
CREATE UNIQUE INDEX uq_admin_self_profile
    ON profile.profile (admin_id)
    WHERE relation_to_admin = 'SELF';

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
