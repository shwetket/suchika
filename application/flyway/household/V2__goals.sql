-- ==============================================================================
-- V2__goals.sql
-- Financial goal tracking table for the Household domain.
-- Covers: v0.1 Epic 1 — Goal Registry
--
-- WHY in household (not wealth)?
--   Goals are household planning constructs ("Save 1 Cr for kids' education").
--   The mathematical engine reads current_amount from wealth.transaction to
--   compute progress — but the goal definition itself belongs here.
--
-- NOTE: No ENUM types. Uses VARCHAR + CHECK constraints throughout.
-- ==============================================================================


CREATE TABLE household.goal (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    profile_id      UUID,                               -- FK to profile.profile
    goal_name       VARCHAR(200)  NOT NULL,
    target_amount   NUMERIC(19,2) NOT NULL,
    current_amount  NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    monthly_saving  NUMERIC(19,2),                      -- expected monthly contribution
    target_date     DATE,                               -- optional deadline
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    notes           TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_goal
        PRIMARY KEY (id),

    CONSTRAINT chk_goal_target_positive
        CHECK (target_amount > 0),

    CONSTRAINT chk_goal_current_non_negative
        CHECK (current_amount >= 0),

    CONSTRAINT chk_goal_status
        CHECK (status IN ('ACTIVE', 'ACHIEVED', 'PAUSED')),

    CONSTRAINT fk_goal_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE household.goal IS $$
Financial goal registry for household planning.

Days to completion estimate (application layer):
  (target_amount - current_amount) / (monthly_saving / 30)

The application calculates progress by reading wealth.transaction to derive
the actual current_amount. This column is updated by the projections engine.

profile_id nullable: NULL = goal belongs to the whole household (admin profile).
$$;

-- "All active goals for household member X"
CREATE INDEX idx_goal_profile_status
    ON household.goal (profile_id, status);
