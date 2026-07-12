-- V4__goal_plan.sql
-- ADR-022 Phase 1: richer financial goal model — admin-scoped goal_plan +
-- 3 child tables (milestones, rules, trigger events). Additive only, per ADR-022's
-- "additive tables, not a computeFormulaGoals() schema rewrite" decision.
-- wealth.insurance_policy (V5) is deliberately deferred to Phase 2 — not created here.

CREATE TABLE wealth.goal_plan (
    id                        UUID          NOT NULL DEFAULT gen_random_uuid(),
    admin_id                  UUID          NOT NULL,
    goal_type                 VARCHAR(50)   NOT NULL,
    beneficiary_profile_id    UUID,                        -- nullable; non-null only for YEAR_ONE (one row per child)
    objective                 TEXT          NOT NULL,
    target_state               TEXT,
    assumed_growth_rate       NUMERIC(7,4),
    education_base_cost       NUMERIC(19,4),                -- YEAR_ONE-only, NULL for other 4 goal types
    education_inflation_rate  NUMERIC(7,4),                 -- YEAR_ONE-only
    education_years_to_entry  INTEGER,                      -- YEAR_ONE-only
    detail                    JSONB         NOT NULL DEFAULT '{}'::jsonb,
    is_active                 BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_goal_plan PRIMARY KEY (id),
    CONSTRAINT fk_goal_plan_admin FOREIGN KEY (admin_id)
        REFERENCES profile.admin(id) ON DELETE RESTRICT,
    CONSTRAINT fk_goal_plan_beneficiary FOREIGN KEY (beneficiary_profile_id)
        REFERENCES profile.profile(id) ON DELETE RESTRICT,
    -- NULLS NOT DISTINCT (Postgres 15+; this project runs 16) makes the 4 singleton
    -- goal types' NULL beneficiary_profile_id collide with each other (singleton
    -- enforced), while YEAR_ONE rows differ on a real non-null beneficiary_profile_id
    -- (multiple rows allowed). See ADR-022's "Postgres NULL-uniqueness" note.
    CONSTRAINT uq_goal_plan_admin_type_beneficiary
        UNIQUE NULLS NOT DISTINCT (admin_id, goal_type, beneficiary_profile_id)
);
CREATE INDEX idx_goal_plan_admin ON wealth.goal_plan(admin_id);
CREATE INDEX idx_goal_plan_beneficiary ON wealth.goal_plan(beneficiary_profile_id) WHERE beneficiary_profile_id IS NOT NULL;

CREATE TABLE wealth.goal_plan_milestone (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    goal_plan_id        UUID          NOT NULL,
    sequence_no         INTEGER       NOT NULL,
    label               VARCHAR(50)   NOT NULL,
    target_value        NUMERIC(19,4),                     -- nullable (skipped when is_manual_checklist)
    is_manual_checklist BOOLEAN       NOT NULL DEFAULT FALSE,
    is_achieved         BOOLEAN       NOT NULL DEFAULT FALSE, -- admin-toggled directly for checklist items;
                                                               -- for non-checklist items this is DERIVED at read
                                                               -- time (current_value vs target_value) and
                                                               -- overwritten on every refresh
    significance        TEXT          NOT NULL,
    CONSTRAINT pk_goal_plan_milestone PRIMARY KEY (id),
    CONSTRAINT fk_milestone_goal_plan FOREIGN KEY (goal_plan_id)
        REFERENCES wealth.goal_plan(id) ON DELETE CASCADE,
    CONSTRAINT uq_milestone_sequence UNIQUE (goal_plan_id, sequence_no)
);

CREATE TABLE wealth.goal_plan_rule (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    goal_plan_id  UUID          NOT NULL,
    sequence_no   INTEGER       NOT NULL,
    rule_name     VARCHAR(50)   NOT NULL,
    rule_text     TEXT          NOT NULL,
    CONSTRAINT pk_goal_plan_rule PRIMARY KEY (id),
    CONSTRAINT fk_rule_goal_plan FOREIGN KEY (goal_plan_id)
        REFERENCES wealth.goal_plan(id) ON DELETE CASCADE,
    CONSTRAINT uq_rule_sequence UNIQUE (goal_plan_id, sequence_no)
);

CREATE TABLE wealth.goal_plan_trigger_event (
    id                 UUID          NOT NULL DEFAULT gen_random_uuid(),
    goal_plan_id       UUID          NOT NULL,
    sequence_no        INTEGER       NOT NULL,
    event_name         VARCHAR(50)   NOT NULL,
    trigger_condition  TEXT          NOT NULL,
    resulting_change   TEXT          NOT NULL,
    CONSTRAINT pk_goal_plan_trigger_event PRIMARY KEY (id),
    CONSTRAINT fk_trigger_goal_plan FOREIGN KEY (goal_plan_id)
        REFERENCES wealth.goal_plan(id) ON DELETE CASCADE,
    CONSTRAINT uq_trigger_sequence UNIQUE (goal_plan_id, sequence_no)
);
