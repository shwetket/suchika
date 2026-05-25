CREATE TYPE vital_type AS ENUM (
    'BLOOD_PRESSURE',
    'WEIGHT',
    'BLOOD_SUGAR_FASTING',
    'BLOOD_SUGAR_PP',
    'HEART_RATE',
    'TEMPERATURE',
    'OXYGEN_SATURATION'
);

CREATE TABLE vital_reading (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id      UUID        NOT NULL REFERENCES health_profile(id),
    vital_type      vital_type  NOT NULL,
    reading_date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    value_primary   NUMERIC(8,2) NOT NULL,  -- BP systolic, weight, sugar
    value_secondary NUMERIC(8,2),           -- BP diastolic only, null otherwise
    unit            VARCHAR(20) NOT NULL,   -- "mmHg", "kg", "mg/dL"
    notes           TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_vital_profile_id ON vital_reading(profile_id);
CREATE INDEX idx_vital_type       ON vital_reading(vital_type);
CREATE INDEX idx_vital_date       ON vital_reading(reading_date);