package com.suchika.wealth.domain;

import java.util.UUID;

/**
 * A step-up trigger attached to a {@link GoalPlan} ("if X happens, change Y").
 * {@code triggerCondition} stays free text, never system-enforced — this is
 * Epic 8 Use Case 8.3 (dynamic triggers), deliberately still unbuilt (ADR-022).
 */
public class GoalTriggerEvent {

    private static final int EVENT_NAME_MAX_LENGTH = 50;

    private UUID id;
    private int sequenceNo;
    private String eventName;
    private String triggerCondition;
    private String resultingChange;

    public GoalTriggerEvent() {}

    private GoalTriggerEvent(Builder builder) {
        this.id = builder.id;
        this.sequenceNo = builder.sequenceNo;
        this.eventName = builder.eventName;
        this.triggerCondition = builder.triggerCondition;
        this.resultingChange = builder.resultingChange;
    }

    public static GoalTriggerEvent create(UUID id, int sequenceNo, String eventName,
                                           String triggerCondition, String resultingChange) {
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("event_name must not be blank");
        }
        if (eventName.length() > EVENT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("event_name must not exceed " + EVENT_NAME_MAX_LENGTH + " characters");
        }
        if (triggerCondition == null || triggerCondition.isBlank()) {
            throw new IllegalArgumentException("trigger_condition must not be blank");
        }
        if (resultingChange == null || resultingChange.isBlank()) {
            throw new IllegalArgumentException("resulting_change must not be blank");
        }
        if (sequenceNo < 0) {
            throw new IllegalArgumentException("sequence_no must be >= 0");
        }
        return new Builder()
                .id(id)
                .sequenceNo(sequenceNo)
                .eventName(eventName)
                .triggerCondition(triggerCondition)
                .resultingChange(resultingChange)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private int sequenceNo;
        private String eventName;
        private String triggerCondition;
        private String resultingChange;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder sequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; return this; }
        public Builder eventName(String eventName) { this.eventName = eventName; return this; }
        public Builder triggerCondition(String triggerCondition) { this.triggerCondition = triggerCondition; return this; }
        public Builder resultingChange(String resultingChange) { this.resultingChange = resultingChange; return this; }
        public GoalTriggerEvent build() { return new GoalTriggerEvent(this); }
    }

    public UUID getId() { return id; }
    public int getSequenceNo() { return sequenceNo; }
    public String getEventName() { return eventName; }
    public String getTriggerCondition() { return triggerCondition; }
    public String getResultingChange() { return resultingChange; }
}
