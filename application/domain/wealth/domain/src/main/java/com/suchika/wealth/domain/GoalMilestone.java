package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single checkpoint on a {@link GoalPlan}. Two flavors (ADR-022): a
 * formula-derived milestone (has a {@code targetValue}, {@code achieved} is
 * recomputed by the gateway's goal-detail step on every refresh) or a manual
 * checklist item ({@code manualChecklist = true}, {@code targetValue} may be
 * null, {@code achieved} is admin-toggled directly via the single-milestone
 * PATCH endpoint).
 */
public class GoalMilestone {

    private static final int LABEL_MAX_LENGTH = 50;

    private UUID id;
    private int sequenceNo;
    private String label;
    private BigDecimal targetValue;
    private boolean manualChecklist;
    private boolean achieved;
    private String significance;

    public GoalMilestone() {}

    private GoalMilestone(Builder builder) {
        this.id = builder.id;
        this.sequenceNo = builder.sequenceNo;
        this.label = builder.label;
        this.targetValue = builder.targetValue;
        this.manualChecklist = builder.manualChecklist;
        this.achieved = builder.achieved;
        this.significance = builder.significance;
    }

    /**
     * Validating factory (ADR-020: no CHECK constraints, validate in domain).
     * A non-checklist milestone must carry a target_value to compare against the
     * parent goal's live current_value; a checklist milestone's target_value is
     * genuinely optional (it may be skipped entirely — an admin-toggled checkbox
     * has nothing numeric to compare).
     */
    public static GoalMilestone create(UUID id, int sequenceNo, String label, BigDecimal targetValue,
                                        boolean manualChecklist, boolean achieved, String significance) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (label.length() > LABEL_MAX_LENGTH) {
            throw new IllegalArgumentException("label must not exceed " + LABEL_MAX_LENGTH + " characters");
        }
        if (significance == null || significance.isBlank()) {
            throw new IllegalArgumentException("significance must not be blank");
        }
        if (sequenceNo < 0) {
            throw new IllegalArgumentException("sequence_no must be >= 0");
        }
        if (!manualChecklist && targetValue == null) {
            throw new IllegalArgumentException("target_value is required unless is_manual_checklist is true");
        }
        return new Builder()
                .id(id)
                .sequenceNo(sequenceNo)
                .label(label)
                .targetValue(targetValue)
                .manualChecklist(manualChecklist)
                .achieved(achieved)
                .significance(significance)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private int sequenceNo;
        private String label;
        private BigDecimal targetValue;
        private boolean manualChecklist;
        private boolean achieved;
        private String significance;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder sequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder targetValue(BigDecimal targetValue) { this.targetValue = targetValue; return this; }
        public Builder manualChecklist(boolean manualChecklist) { this.manualChecklist = manualChecklist; return this; }
        public Builder achieved(boolean achieved) { this.achieved = achieved; return this; }
        public Builder significance(String significance) { this.significance = significance; return this; }
        public GoalMilestone build() { return new GoalMilestone(this); }
    }

    public UUID getId() { return id; }
    public int getSequenceNo() { return sequenceNo; }
    public String getLabel() { return label; }
    public BigDecimal getTargetValue() { return targetValue; }
    public boolean isManualChecklist() { return manualChecklist; }
    public boolean isAchieved() { return achieved; }
    public String getSignificance() { return significance; }

    public void setAchieved(boolean achieved) { this.achieved = achieved; }
}
