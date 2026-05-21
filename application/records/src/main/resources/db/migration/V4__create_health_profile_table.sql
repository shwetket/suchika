CREATE TABLE health_profile (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    relationship  VARCHAR(50),     -- "Self", "Wife", "Father"
    date_of_birth DATE,
    blood_group   VARCHAR(10),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

INSERT INTO health_profile (name, relationship)
VALUES ('Ketan', 'Self');