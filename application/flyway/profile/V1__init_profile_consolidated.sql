CREATE TABLE profile.admin (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    display_name    VARCHAR(150) NOT NULL,
    email_address   VARCHAR(255),
    policy_settings JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_admin PRIMARY KEY (id)
);

CREATE TABLE profile.profile (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    admin_id          UUID,
    full_name         VARCHAR(150) NOT NULL,
    dob               DATE         NOT NULL,
    relation_to_admin VARCHAR(30)  NOT NULL,
    email_address     VARCHAR(255),
    gender            VARCHAR(30),
    blood_type        VARCHAR(10),
    metadata          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_profile PRIMARY KEY (id)
);
