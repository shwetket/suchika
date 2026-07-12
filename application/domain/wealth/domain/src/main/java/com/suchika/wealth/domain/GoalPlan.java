package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ADR-022 Phase 1 — the product owner's "goal theory" (objective, baseline,
 * milestones, rules, step-up triggers) for one of the 5 hardcoded Epic 8 formula
 * goals. Household-scoped by {@code admin_id} (deliberate ADR-006 extension, same
 * shape as {@code profile.admin.policy_settings}) — not {@code profile_id}.
 *
 * <p>Owns 3 child collections (milestones, rules, trigger events) as an aggregate
 * root — authored/replaced as whole documents via bulk PUT, per ADR-022.
 */
public class GoalPlan {

    private UUID id;
    private UUID adminId;
    private GoalType goalType;
    private UUID beneficiaryProfileId;
    private String objective;
    private String targetState;
    private BigDecimal assumedGrowthRate;
    private BigDecimal educationBaseCost;
    private BigDecimal educationInflationRate;
    private Integer educationYearsToEntry;
    private Map<String, String> detail;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<GoalMilestone> milestones;
    private List<GoalRule> rules;
    private List<GoalTriggerEvent> triggerEvents;

    public GoalPlan() {
        this.detail = new HashMap<>();
        this.milestones = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.triggerEvents = new ArrayList<>();
    }

    private GoalPlan(Builder builder) {
        this.id = builder.id;
        this.adminId = builder.adminId;
        this.goalType = builder.goalType;
        this.beneficiaryProfileId = builder.beneficiaryProfileId;
        this.objective = builder.objective;
        this.targetState = builder.targetState;
        this.assumedGrowthRate = builder.assumedGrowthRate;
        this.educationBaseCost = builder.educationBaseCost;
        this.educationInflationRate = builder.educationInflationRate;
        this.educationYearsToEntry = builder.educationYearsToEntry;
        this.detail = builder.detail;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.milestones = builder.milestones;
        this.rules = builder.rules;
        this.triggerEvents = builder.triggerEvents;
    }

    /**
     * Validating factory (ADR-020: no CHECK constraints, validate in domain).
     * {@code beneficiary_profile_id} and the 3 education_* inputs are meaningful
     * only for {@code YEAR_ONE} (per-child rows) — enforced here, not just left
     * nullable, so a malformed request 400s instead of silently creating a
     * confusing row for the other 4 (household-level) goal types.
     */
    public static GoalPlan create(UUID adminId, GoalType goalType, UUID beneficiaryProfileId,
                                   String objective, String targetState, BigDecimal assumedGrowthRate,
                                   BigDecimal educationBaseCost, BigDecimal educationInflationRate,
                                   Integer educationYearsToEntry) {
        if (adminId == null) {
            throw new IllegalArgumentException("admin_id is required");
        }
        if (goalType == null) {
            throw new IllegalArgumentException("goal_type is required");
        }
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("objective must not be blank");
        }
        validateBeneficiaryScope(goalType, beneficiaryProfileId, educationBaseCost,
                educationInflationRate, educationYearsToEntry);

        return new Builder()
                .adminId(adminId)
                .goalType(goalType)
                .beneficiaryProfileId(beneficiaryProfileId)
                .objective(objective)
                .targetState(targetState)
                .assumedGrowthRate(assumedGrowthRate)
                .educationBaseCost(educationBaseCost)
                .educationInflationRate(educationInflationRate)
                .educationYearsToEntry(educationYearsToEntry)
                .detail(new HashMap<>())
                .active(true)
                .milestones(new ArrayList<>())
                .rules(new ArrayList<>())
                .triggerEvents(new ArrayList<>())
                .build();
    }

    private static void validateBeneficiaryScope(GoalType goalType, UUID beneficiaryProfileId,
                                                   BigDecimal educationBaseCost, BigDecimal educationInflationRate,
                                                   Integer educationYearsToEntry) {
        if (goalType == GoalType.YEAR_ONE) {
            if (beneficiaryProfileId == null) {
                throw new IllegalArgumentException("beneficiary_profile_id is required for YEAR_ONE goal plans");
            }
        } else {
            if (beneficiaryProfileId != null) {
                throw new IllegalArgumentException("beneficiary_profile_id must be null for non-YEAR_ONE goal plans");
            }
            if (educationBaseCost != null || educationInflationRate != null || educationYearsToEntry != null) {
                throw new IllegalArgumentException("education_* fields are only valid for YEAR_ONE goal plans");
            }
        }
    }

    /**
     * Validates the {@code sequence_no} uniqueness invariant shared by all 3 child
     * collections (also backstopped by each table's DB UNIQUE constraint, ADR-020 —
     * this gives a clean 400 instead of surfacing a raw constraint-violation 500).
     */
    public static void validateUniqueSequence(List<Integer> sequenceNumbers, String childName) {
        Set<Integer> seen = new HashSet<>();
        for (Integer sequenceNo : sequenceNumbers) {
            if (!seen.add(sequenceNo)) {
                throw new IllegalArgumentException(
                        "Duplicate sequence_no " + sequenceNo + " within " + childName);
            }
        }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID adminId;
        private GoalType goalType;
        private UUID beneficiaryProfileId;
        private String objective;
        private String targetState;
        private BigDecimal assumedGrowthRate;
        private BigDecimal educationBaseCost;
        private BigDecimal educationInflationRate;
        private Integer educationYearsToEntry;
        private Map<String, String> detail = new HashMap<>();
        private boolean active = true;
        private Instant createdAt;
        private Instant updatedAt;
        private List<GoalMilestone> milestones = new ArrayList<>();
        private List<GoalRule> rules = new ArrayList<>();
        private List<GoalTriggerEvent> triggerEvents = new ArrayList<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder adminId(UUID adminId) { this.adminId = adminId; return this; }
        public Builder goalType(GoalType goalType) { this.goalType = goalType; return this; }
        public Builder beneficiaryProfileId(UUID beneficiaryProfileId) { this.beneficiaryProfileId = beneficiaryProfileId; return this; }
        public Builder objective(String objective) { this.objective = objective; return this; }
        public Builder targetState(String targetState) { this.targetState = targetState; return this; }
        public Builder assumedGrowthRate(BigDecimal assumedGrowthRate) { this.assumedGrowthRate = assumedGrowthRate; return this; }
        public Builder educationBaseCost(BigDecimal educationBaseCost) { this.educationBaseCost = educationBaseCost; return this; }
        public Builder educationInflationRate(BigDecimal educationInflationRate) { this.educationInflationRate = educationInflationRate; return this; }
        public Builder educationYearsToEntry(Integer educationYearsToEntry) { this.educationYearsToEntry = educationYearsToEntry; return this; }
        public Builder detail(Map<String, String> detail) { this.detail = detail; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder milestones(List<GoalMilestone> milestones) { this.milestones = milestones; return this; }
        public Builder rules(List<GoalRule> rules) { this.rules = rules; return this; }
        public Builder triggerEvents(List<GoalTriggerEvent> triggerEvents) { this.triggerEvents = triggerEvents; return this; }
        public GoalPlan build() { return new GoalPlan(this); }
    }

    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public GoalType getGoalType() { return goalType; }
    public UUID getBeneficiaryProfileId() { return beneficiaryProfileId; }
    public String getObjective() { return objective; }
    public String getTargetState() { return targetState; }
    public BigDecimal getAssumedGrowthRate() { return assumedGrowthRate; }
    public BigDecimal getEducationBaseCost() { return educationBaseCost; }
    public BigDecimal getEducationInflationRate() { return educationInflationRate; }
    public Integer getEducationYearsToEntry() { return educationYearsToEntry; }
    public Map<String, String> getDetail() { return detail; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<GoalMilestone> getMilestones() { return milestones; }
    public List<GoalRule> getRules() { return rules; }
    public List<GoalTriggerEvent> getTriggerEvents() { return triggerEvents; }

    public void setObjective(String objective) { this.objective = objective; }
    public void setTargetState(String targetState) { this.targetState = targetState; }
    public void setAssumedGrowthRate(BigDecimal assumedGrowthRate) { this.assumedGrowthRate = assumedGrowthRate; }
    public void setEducationBaseCost(BigDecimal educationBaseCost) { this.educationBaseCost = educationBaseCost; }
    public void setEducationInflationRate(BigDecimal educationInflationRate) { this.educationInflationRate = educationInflationRate; }
    public void setEducationYearsToEntry(Integer educationYearsToEntry) { this.educationYearsToEntry = educationYearsToEntry; }
    public void setDetail(Map<String, String> detail) { this.detail = detail; }
    public void setActive(boolean active) { this.active = active; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setMilestones(List<GoalMilestone> milestones) { this.milestones = milestones; }
    public void setRules(List<GoalRule> rules) { this.rules = rules; }
    public void setTriggerEvents(List<GoalTriggerEvent> triggerEvents) { this.triggerEvents = triggerEvents; }
}
