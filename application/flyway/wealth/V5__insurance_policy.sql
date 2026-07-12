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
