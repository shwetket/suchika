CREATE TABLE health.vital_reading (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    profile_id      UUID          NOT NULL,
    vital_type      VARCHAR(50)   NOT NULL,
    reading_date    DATE          NOT NULL,
    value_primary   NUMERIC(8,2)  NOT NULL,
    value_secondary NUMERIC(8,2),
    unit            VARCHAR(20),
    notes           TEXT,
    metadata        JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_vital_reading PRIMARY KEY (id),
    CONSTRAINT fk_vital_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

CREATE TABLE health.doctor_visit (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id     UUID         NOT NULL,
    from_date      DATE         NOT NULL,
    to_date        DATE,
    visited_doctor BOOLEAN      NOT NULL DEFAULT TRUE,
    doctor_name    VARCHAR(50),
    hospital_name  VARCHAR(50),
    speciality     VARCHAR(50),
    symptoms       TEXT,
    diagnosis      TEXT,
    notes          TEXT,
    follow_up_date DATE,
    metadata       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_doctor_visit PRIMARY KEY (id),
    CONSTRAINT fk_visit_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);
