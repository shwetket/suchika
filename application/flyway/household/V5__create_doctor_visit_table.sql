CREATE TABLE doctor_visit (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id      UUID         NOT NULL REFERENCES health_profile(id),
    
    -- When sick with no doctor visit, doctor_name is null
    visited_doctor  BOOLEAN      NOT NULL DEFAULT TRUE,
    from_date       DATE         NOT NULL,
    to_date         DATE,                    -- null if single day or ongoing
    
    -- Doctor details (null if visited_doctor = false)
    doctor_name     VARCHAR(100),
    hospital_name   VARCHAR(200),
    speciality      VARCHAR(100),
    
    -- What happened
    symptoms        TEXT,                    -- "fever, body ache"
    diagnosis       TEXT,
    notes           TEXT,                    -- free observations, advice
    follow_up_date  DATE,

    created_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CHECK (to_date IS NULL OR to_date >= from_date),
    CHECK (
        visited_doctor = FALSE OR doctor_name IS NOT NULL
    )
);

CREATE INDEX idx_visit_profile_id ON doctor_visit(profile_id);
CREATE INDEX idx_visit_from_date  ON doctor_visit(from_date);

COMMENT ON COLUMN doctor_visit.visited_doctor IS
    'FALSE = illness period with no doctor visit.
     Symptoms + from/to date captured, doctor fields left null.
     TRUE = actual visit, doctor_name is required.';