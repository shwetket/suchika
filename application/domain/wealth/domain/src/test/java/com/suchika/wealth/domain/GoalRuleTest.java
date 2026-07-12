package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoalRuleTest {

    @Test
    void create_validFields_succeeds() {
        GoalRule rule = GoalRule.create(null, 0, "No Liquidation", "Never liquidate mutual funds mid-goal");

        assertEquals(0, rule.getSequenceNo());
        assertEquals("No Liquidation", rule.getRuleName());
        assertEquals("Never liquidate mutual funds mid-goal", rule.getRuleText());
    }

    @Test
    void create_blankRuleName_throws() {
        assertThrows(IllegalArgumentException.class, () -> GoalRule.create(null, 0, "  ", "Rule text"));
    }

    @Test
    void create_ruleNameTooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> GoalRule.create(null, 0, "x".repeat(51), "Rule text"));
    }

    @Test
    void create_blankRuleText_throws() {
        assertThrows(IllegalArgumentException.class, () -> GoalRule.create(null, 0, "Rule name", "  "));
    }

    @Test
    void create_negativeSequenceNo_throws() {
        assertThrows(IllegalArgumentException.class, () -> GoalRule.create(null, -1, "Rule name", "Rule text"));
    }
}
