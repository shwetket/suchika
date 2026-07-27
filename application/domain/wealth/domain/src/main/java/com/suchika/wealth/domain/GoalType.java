package com.suchika.wealth.domain;

/**
 * The 5 hardcoded Epic 8 Phase 4 formula goals (ADR-022). Fixed, closed set —
 * {@code wealth.goal_plan.goal_type} is a plain VARCHAR (ADR-010, no SQL enum),
 * soft-validated against this enum at the contract/domain layer only.
 */
public enum GoalType {
    DEBT_CROSSOVER,
    THIRTY_SEVENTY_TARGET,
    FREEDOM_RUNWAY,
    INSURANCE_FREE,
    YEAR_ONE
}
