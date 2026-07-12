package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoalTriggerEventTest {

    @Test
    void create_validFields_succeeds() {
        GoalTriggerEvent event = GoalTriggerEvent.create(null, 0, "Bonus received",
                "Annual bonus credited exceeds 1 lakh", "Allocate 50% to MF corpus");

        assertEquals(0, event.getSequenceNo());
        assertEquals("Bonus received", event.getEventName());
        assertEquals("Annual bonus credited exceeds 1 lakh", event.getTriggerCondition());
        assertEquals("Allocate 50% to MF corpus", event.getResultingChange());
    }

    @Test
    void create_blankEventName_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalTriggerEvent.create(null, 0, "  ", "Condition", "Change"));
    }

    @Test
    void create_eventNameTooLong_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalTriggerEvent.create(null, 0, "x".repeat(51), "Condition", "Change"));
    }

    @Test
    void create_blankTriggerCondition_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalTriggerEvent.create(null, 0, "Event", "  ", "Change"));
    }

    @Test
    void create_blankResultingChange_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalTriggerEvent.create(null, 0, "Event", "Condition", "  "));
    }

    @Test
    void create_negativeSequenceNo_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalTriggerEvent.create(null, -1, "Event", "Condition", "Change"));
    }
}
