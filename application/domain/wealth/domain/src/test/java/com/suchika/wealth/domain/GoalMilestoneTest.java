package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GoalMilestoneTest {

    private static final BigDecimal TARGET = new BigDecimal("50");

    @Test
    void create_nonChecklistWithoutTargetValue_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                GoalMilestone.create(null, 0, "Halfway", null, false, false, "Significant progress marker"));

        assertTrue(ex.getMessage().contains("target_value"));
    }

    @Test
    void create_nonChecklistWithTargetValue_succeeds() {
        GoalMilestone milestone = GoalMilestone.create(null, 0, "Halfway", TARGET, false, false,
                "Significant progress marker");

        assertEquals(0, milestone.getSequenceNo());
        assertEquals("Halfway", milestone.getLabel());
        assertEquals(0, TARGET.compareTo(milestone.getTargetValue()));
        assertFalse(milestone.isManualChecklist());
        assertFalse(milestone.isAchieved());
    }

    @Test
    void create_manualChecklistWithoutTargetValue_succeeds() {
        GoalMilestone milestone = GoalMilestone.create(null, 0, "Open a term insurance policy", null, true, false,
                "One-time administrative step");

        assertTrue(milestone.isManualChecklist());
        assertNull(milestone.getTargetValue());
    }

    @Test
    void create_blankLabel_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalMilestone.create(null, 0, "  ", TARGET, false, false, "Significance"));
    }

    @Test
    void create_labelTooLong_throws() {
        String longLabel = "x".repeat(51);
        assertThrows(IllegalArgumentException.class, () ->
                GoalMilestone.create(null, 0, longLabel, TARGET, false, false, "Significance"));
    }

    @Test
    void create_blankSignificance_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalMilestone.create(null, 0, "Label", TARGET, false, false, "  "));
    }

    @Test
    void create_negativeSequenceNo_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalMilestone.create(null, -1, "Label", TARGET, false, false, "Significance"));
    }

    @Test
    void setAchieved_mutatesAchievedFlag() {
        GoalMilestone milestone = GoalMilestone.create(null, 0, "Checklist item", null, true, false, "Manual step");

        milestone.setAchieved(true);

        assertTrue(milestone.isAchieved());
    }
}
