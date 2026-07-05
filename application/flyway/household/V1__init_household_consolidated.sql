CREATE TABLE household.calendar_event (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id  UUID,
    title       VARCHAR(200) NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE,
    location    VARCHAR(200),
    notes       TEXT,
    metadata    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_calendar_event PRIMARY KEY (id),
    CONSTRAINT fk_event_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

CREATE TABLE household.inventory_item (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    profile_id      UUID,
    item_name       VARCHAR(200)  NOT NULL,
    quantity        NUMERIC(10,3) NOT NULL,
    unit            VARCHAR(20),
    source_platform VARCHAR(50),
    purchase_date   DATE          NOT NULL,
    category        VARCHAR(50),
    metadata        JSONB         NOT NULL DEFAULT '{}'::jsonb,
    is_consumed     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_inventory_item PRIMARY KEY (id),
    CONSTRAINT fk_item_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

CREATE TABLE household.goal (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    profile_id     UUID,
    goal_name      VARCHAR(200)  NOT NULL,
    target_amount  NUMERIC(19,2) NOT NULL,
    current_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    monthly_saving NUMERIC(19,2),
    target_date    DATE,
    status         VARCHAR(20),
    notes          TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_goal PRIMARY KEY (id),
    CONSTRAINT fk_goal_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);
