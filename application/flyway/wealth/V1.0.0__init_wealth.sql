CREATE TABLE wealth.account (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id       UUID,
    institution_name VARCHAR(50)  NOT NULL,
    account_name     VARCHAR(50)  NOT NULL,
    account_type     VARCHAR(50)  NOT NULL,
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    opening_balance  NUMERIC(19,4),
    credit_limit     NUMERIC(19,4),
    interest_rate    NUMERIC(7,4),
    emi_amount       NUMERIC(19,4),
    metadata         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT fk_account_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

CREATE TABLE wealth.statement_upload (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID         NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    upload_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status      VARCHAR(20),
    CONSTRAINT pk_statement_upload PRIMARY KEY (id),
    CONSTRAINT fk_upload_account
        FOREIGN KEY (account_id) REFERENCES wealth.account(id)
);

CREATE TABLE wealth.upload_error_log (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    upload_id       UUID         NOT NULL,
    error_type      VARCHAR(50),
    missing_columns TEXT[],
    error_detail    TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_upload_error_log PRIMARY KEY (id)
);

CREATE TABLE wealth.transaction (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID          NOT NULL,
    upload_id   UUID,
    txn_date    DATE          NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    txn_type    VARCHAR(10)   NOT NULL,
    description TEXT          NOT NULL,
    metadata    JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_transaction PRIMARY KEY (id),
    CONSTRAINT fk_txn_account
        FOREIGN KEY (account_id) REFERENCES wealth.account(id),
    CONSTRAINT fk_txn_upload
        FOREIGN KEY (upload_id) REFERENCES wealth.statement_upload(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_transaction_dedup
        UNIQUE (account_id, txn_date, amount, txn_type)
);

CREATE TABLE wealth.physical_asset (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id          UUID         NOT NULL,
    asset_name          VARCHAR(50)  NOT NULL,
    asset_type          VARCHAR(50)  NOT NULL,
    make                VARCHAR(100),
    model               VARCHAR(100),
    registration_number VARCHAR(50),
    registration_type   VARCHAR(50),
    metadata            JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_physical_asset PRIMARY KEY (id),
    CONSTRAINT uq_registration_number
        UNIQUE (registration_number),
    CONSTRAINT fk_physical_asset_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);
-- Adds valuation tracking to wealth.physical_asset so non-vehicle assets (real estate,
-- gold jewellery/bonds) can carry a current market value into net worth. Nullable, no
-- default, no CHECK — matches the project's "no CHECK constraints" rule and the existing
-- account.opening_balance/interest_rate pattern of plain nullable typed columns sitting
-- alongside the JSONB metadata column.
ALTER TABLE wealth.physical_asset
    ADD COLUMN current_value  NUMERIC(19,4),
    ADD COLUMN valuation_date DATE;
ALTER TABLE wealth.account
    ADD COLUMN balance_as_of DATE;
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
-- V5__insurance_policy.sql
-- ADR-022 Phase 2: insurance policies feeding THIRTY_SEVENTY_TARGET's premium term
-- and INSURANCE_FREE's "WITH insurance" raw-list comparison (Phase 3, gateway-side,
-- not built here). Admin-scoped (household-level), same FK pattern as goal_plan.
-- Additive only — does not touch V4__goal_plan.sql.

CREATE TABLE wealth.insurance_policy (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    admin_id          UUID          NOT NULL,
    policy_name       VARCHAR(50)   NOT NULL,
    provider          VARCHAR(50)   NOT NULL,
    policy_type       VARCHAR(50)   NOT NULL,                -- TERM/GROUP_TERM/INVESTMENT_LINKED/ENDOWMENT/HEALTH; no SQL enum (ADR-010)
    premium_amount    NUMERIC(19,4) NOT NULL,
    premium_frequency VARCHAR(20)   NOT NULL,                -- MONTHLY/ANNUAL; no SQL enum
    coverage_amount   NUMERIC(19,4),                          -- nullable: some policies are income-stream, not lump-sum
    payout_structure  JSONB         NOT NULL DEFAULT '{}'::jsonb, -- heterogeneous per policy_type — lump sum / escalating
                                                                    -- monthly income / sum-assured-at-maturity; same
                                                                    -- escape-hatch precedent as goal_plan.detail
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_insurance_policy PRIMARY KEY (id),
    CONSTRAINT fk_insurance_policy_admin FOREIGN KEY (admin_id)
        REFERENCES profile.admin(id) ON DELETE RESTRICT
);
CREATE INDEX idx_insurance_policy_admin ON wealth.insurance_policy(admin_id);
-- V6__error_log.sql
-- Phase 4 Application Console (ADR-023): general application-error log for the
-- wealth service. Deliberately separate from wealth.upload_error_log (V1) --
-- that table is CSV-upload-specific (error_type/missing_columns tied to a
-- statement_upload row); this one is for general application errors from any
-- endpoint (ADR-003: no shared/cross-domain error table -- every domain gets
-- its own). No CHECK constraints anywhere (2026-07-05 policy). Populated by
-- ApplicationExceptionMapper's optional ErrorLogRecorder hook (shared/) --
-- see WealthErrorLogRecorder.

CREATE TABLE wealth.error_log (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    error_code  VARCHAR(50)  NOT NULL,
    http_status INT          NOT NULL,
    message     VARCHAR(500) NOT NULL,
    details     VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_error_log PRIMARY KEY (id)
);

-- Supports GET /v1/errors?since=&limit= (ordered by recency).
CREATE INDEX idx_error_log_created_at ON wealth.error_log (created_at);
