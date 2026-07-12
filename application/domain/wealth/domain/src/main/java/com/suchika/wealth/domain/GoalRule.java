package com.suchika.wealth.domain;

import java.util.UUID;

/**
 * Household discipline text ("No Liquidation", "SIP is Sacred") attached to a
 * {@link GoalPlan}. Narrative only — never code-enforced (ADR-022).
 */
public class GoalRule {

    private static final int RULE_NAME_MAX_LENGTH = 50;

    private UUID id;
    private int sequenceNo;
    private String ruleName;
    private String ruleText;

    public GoalRule() {}

    private GoalRule(Builder builder) {
        this.id = builder.id;
        this.sequenceNo = builder.sequenceNo;
        this.ruleName = builder.ruleName;
        this.ruleText = builder.ruleText;
    }

    public static GoalRule create(UUID id, int sequenceNo, String ruleName, String ruleText) {
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("rule_name must not be blank");
        }
        if (ruleName.length() > RULE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("rule_name must not exceed " + RULE_NAME_MAX_LENGTH + " characters");
        }
        if (ruleText == null || ruleText.isBlank()) {
            throw new IllegalArgumentException("rule_text must not be blank");
        }
        if (sequenceNo < 0) {
            throw new IllegalArgumentException("sequence_no must be >= 0");
        }
        return new Builder().id(id).sequenceNo(sequenceNo).ruleName(ruleName).ruleText(ruleText).build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private int sequenceNo;
        private String ruleName;
        private String ruleText;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder sequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder ruleText(String ruleText) { this.ruleText = ruleText; return this; }
        public GoalRule build() { return new GoalRule(this); }
    }

    public UUID getId() { return id; }
    public int getSequenceNo() { return sequenceNo; }
    public String getRuleName() { return ruleName; }
    public String getRuleText() { return ruleText; }
}
