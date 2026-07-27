package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the 5 hardcoded Epic 8 Phase 4 formula goal types (ADR-022) — a fixed,
 * closed set. Adding a 6th requires a deliberate ADR-022 follow-up per its own
 * "Still underspecified or risky" note on goal-type-specific inputs.
 */
class GoalTypeTest {

    @Test
    void values_matchDocumentedFiveValueContract() {
        GoalType[] values = GoalType.values();

        assertEquals(5, values.length);
        assertEquals(GoalType.DEBT_CROSSOVER, GoalType.valueOf("DEBT_CROSSOVER"));
        assertEquals(GoalType.THIRTY_SEVENTY_TARGET, GoalType.valueOf("THIRTY_SEVENTY_TARGET"));
        assertEquals(GoalType.FREEDOM_RUNWAY, GoalType.valueOf("FREEDOM_RUNWAY"));
        assertEquals(GoalType.INSURANCE_FREE, GoalType.valueOf("INSURANCE_FREE"));
        assertEquals(GoalType.YEAR_ONE, GoalType.valueOf("YEAR_ONE"));
    }
}
