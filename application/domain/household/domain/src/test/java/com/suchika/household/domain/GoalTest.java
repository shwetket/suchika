package com.suchika.household.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
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
                LocalDate.of(2027, Month.JANUARY, 1), "For emergencies");

        assertNotNull(goal);
        assertEquals(PROFILE_ID, goal.getProfileId());
        assertEquals("Emergency Fund", goal.getGoalName());
        assertEquals(0, new BigDecimal("500000").compareTo(goal.getTargetAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(goal.getCurrentAmount()));
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
    }

    @Test
    void create_blankGoalName_throwsIllegalArgumentException() {
        BigDecimal amount100k = new BigDecimal("100000");
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "  ", amount100k, null, null, null));
    }

    @Test
    void create_nullGoalName_throwsIllegalArgumentException() {
        BigDecimal amount100k = new BigDecimal("100000");
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, null, amount100k, null, null, null));
    }

    @Test
    void create_zeroTargetAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "Goal", BigDecimal.ZERO, null, null, null));
    }

    @Test
    void create_negativeTargetAmount_throwsIllegalArgumentException() {
        BigDecimal negative1000 = new BigDecimal("-1000");
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "Goal", negative1000, null, null, null));
    }

    @Test
    void create_nullTargetAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                Goal.create(PROFILE_ID, "Goal", null, null, null, null));
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
    void progressPercent_nullTargetAmount_returnsZero() {
        // Only reachable via the builder directly (Goal.create() rejects a null
        // target_amount) — exercises the targetAmount == null branch explicitly.
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Unset Goal")
                .currentAmount(new BigDecimal("100"))
                .status(GoalStatus.ACTIVE)
                .build();

        assertEquals(0, BigDecimal.ZERO.compareTo(goal.progressPercent()));
    }

    @Test
    void progressPercent_zeroTargetAmount_returnsZero() {
        // Only reachable via the builder directly (Goal.create() rejects a
        // target_amount <= 0) — exercises the targetAmount == 0 branch explicitly.
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Zero Target Goal")
                .targetAmount(BigDecimal.ZERO)
                .currentAmount(new BigDecimal("100"))
                .status(GoalStatus.ACTIVE)
                .build();

        assertEquals(0, BigDecimal.ZERO.compareTo(goal.progressPercent()));
    }

    @Test
    void progressPercent_nullCurrentAmount_computesUsingZero() {
        // currentAmount left unset (null) — Goal.create() always defaults it to
        // ZERO, so the null branch of the ternary is only reachable via the builder.
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("No Progress Yet")
                .targetAmount(new BigDecimal("40000"))
                .status(GoalStatus.ACTIVE)
                .build();

        assertEquals(0, BigDecimal.ZERO.compareTo(goal.progressPercent()));
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

    @Test
    void daysToCompletion_nullCurrentAmount_computesFromZero() {
        // currentAmount left unset (null) — exercises the ternary's null branch
        // inside daysToCompletion(), distinct from the same-shaped branch in
        // progressPercent().
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Fresh Fund")
                .targetAmount(new BigDecimal("60000"))
                .monthlySaving(new BigDecimal("10000"))
                .status(GoalStatus.ACTIVE)
                .build();

        assertTrue(goal.daysToCompletion().isPresent());
        assertTrue(goal.daysToCompletion().getAsLong() > 0);
    }

    @Test
    void daysToCompletion_tinyMonthlySavingRoundsDailyRateToZero_returnsEmpty() {
        // monthlySaving is positive (passes the <= 0 guard) but small enough that
        // dividing by 30 and rounding to 2 decimal places (HALF_UP) yields exactly
        // 0.00 — exercises the dailyRate == 0 branch distinctly from monthlySaving <= 0.
        Goal goal = Goal.builder()
                .profileId(PROFILE_ID)
                .goalName("Trickle Fund")
                .targetAmount(new BigDecimal("100"))
                .currentAmount(BigDecimal.ZERO)
                .monthlySaving(new BigDecimal("0.01"))
                .status(GoalStatus.ACTIVE)
                .build();

        assertTrue(goal.daysToCompletion().isEmpty());
    }

    @Test
    void builder_allFieldsSet_gettersReflectBuilderValues() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        LocalDate targetDate = LocalDate.of(2028, Month.MARCH, 15);

        Goal goal = Goal.builder()
                .id(id)
                .profileId(PROFILE_ID)
                .goalName("Retirement Fund")
                .targetAmount(new BigDecimal("2000000"))
                .currentAmount(new BigDecimal("500000"))
                .monthlySaving(new BigDecimal("15000"))
                .targetDate(targetDate)
                .status(GoalStatus.ACTIVE)
                .notes("Long-term goal")
                .createdAt(createdAt)
                .build();

        assertEquals(id, goal.getId());
        assertEquals(PROFILE_ID, goal.getProfileId());
        assertEquals("Retirement Fund", goal.getGoalName());
        assertEquals(0, new BigDecimal("2000000").compareTo(goal.getTargetAmount()));
        assertEquals(0, new BigDecimal("500000").compareTo(goal.getCurrentAmount()));
        assertEquals(0, new BigDecimal("15000").compareTo(goal.getMonthlySaving()));
        assertEquals(targetDate, goal.getTargetDate());
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
        assertEquals("Long-term goal", goal.getNotes());
        assertEquals(createdAt, goal.getCreatedAt());
    }
}
