package com.suchika.household.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();

    @Test
    void create_happyPath_returnsGoalWithDefaults() {
        Goal goal = Goal.create(PROFILE_ID, "Emergency Fund",
                new BigDecimal("500000"), new BigDecimal("10000"),
                LocalDate.of(2027, 1, 1), "For emergencies");

        assertNotNull(goal);
        assertEquals(PROFILE_ID, goal.getProfileId());
        assertEquals("Emergency Fund", goal.getGoalName());
        assertEquals(0, new BigDecimal("500000").compareTo(goal.getTargetAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(goal.getCurrentAmount()));
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
    }

    @Test
    void create_blankGoalName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "  ", new BigDecimal("100000"),
                        null, null, null));
    }

    @Test
    void create_nullGoalName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, null, new BigDecimal("100000"),
                        null, null, null));
    }

    @Test
    void create_zeroTargetAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "Goal", BigDecimal.ZERO,
                        null, null, null));
    }

    @Test
    void create_negativeTargetAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "Goal", new BigDecimal("-1000"),
                        null, null, null));
    }

    @Test
    void progressPercent_noCurrentAmount_returnsZero() {
        Goal goal = Goal.create(PROFILE_ID, "Car Fund", new BigDecimal("200000"),
                null, null, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(goal.progressPercent()));
    }

    @Test
    void progressPercent_halfWay_returnsFifty() {
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Car Fund")
                .targetAmount(new BigDecimal("200000"))
                .currentAmount(new BigDecimal("100000"))
                .status(GoalStatus.ACTIVE)
                .build();

        assertEquals(0, new BigDecimal("50.00").compareTo(goal.progressPercent()));
    }

    @Test
    void progressPercent_fullyAchieved_returnsHundred() {
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Holiday Fund")
                .targetAmount(new BigDecimal("50000"))
                .currentAmount(new BigDecimal("50000"))
                .status(GoalStatus.ACHIEVED)
                .build();

        assertEquals(0, new BigDecimal("100.00").compareTo(goal.progressPercent()));
    }

    @Test
    void daysToCompletion_withMonthlySaving_returnsPositiveDays() {
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Laptop Fund")
                .targetAmount(new BigDecimal("60000"))
                .currentAmount(new BigDecimal("0"))
                .monthlySaving(new BigDecimal("10000"))
                .status(GoalStatus.ACTIVE)
                .build();

        // 60000 / (10000/30) = 60000 / 333.33 = 180 days
        assertTrue(goal.daysToCompletion().isPresent());
        assertTrue(goal.daysToCompletion().getAsLong() > 0);
    }

    @Test
    void daysToCompletion_withoutMonthlySaving_returnsEmpty() {
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Dream Fund")
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(BigDecimal.ZERO)
                .status(GoalStatus.ACTIVE)
                .build();

        assertTrue(goal.daysToCompletion().isEmpty());
    }

    @Test
    void daysToCompletion_zeroMonthlySaving_returnsEmpty() {
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Stalled Fund")
                .targetAmount(new BigDecimal("50000"))
                .currentAmount(BigDecimal.ZERO)
                .monthlySaving(BigDecimal.ZERO)
                .status(GoalStatus.ACTIVE)
                .build();

        assertTrue(goal.daysToCompletion().isEmpty());
    }

    @Test
    void daysToCompletion_alreadyAchieved_returnsZero() {
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Done Fund")
                .targetAmount(new BigDecimal("10000"))
                .currentAmount(new BigDecimal("10000"))
                .monthlySaving(new BigDecimal("5000"))
                .status(GoalStatus.ACHIEVED)
                .build();

        assertTrue(goal.daysToCompletion().isPresent());
        assertEquals(0L, goal.daysToCompletion().getAsLong());
    }
}
