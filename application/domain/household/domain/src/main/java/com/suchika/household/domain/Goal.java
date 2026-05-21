package com.suchika.household.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.OptionalLong;
import java.util.UUID;

public class Goal {

    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");
    private static final int PERCENT_SCALE = 2;

    private final UUID id;
    private final UUID profileId;
    private final String goalName;
    private final BigDecimal targetAmount;
    private final BigDecimal currentAmount;
    private final BigDecimal monthlySaving;
    private final LocalDate targetDate;
    private final GoalStatus status;
    private final String notes;
    private final Instant createdAt;

    private Goal(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.goalName = builder.goalName;
        this.targetAmount = builder.targetAmount;
        this.currentAmount = builder.currentAmount;
        this.monthlySaving = builder.monthlySaving;
        this.targetDate = builder.targetDate;
        this.status = builder.status;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt;
    }

    public static Goal create(UUID profileId, String goalName, BigDecimal targetAmount,
                              BigDecimal monthlySaving, LocalDate targetDate, String notes) {
        if (goalName == null || goalName.isBlank()) {
            throw new IllegalArgumentException("goal_name must not be blank");
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("target_amount must be greater than 0");
        }
        return new Builder()
                .profileId(profileId)
                .goalName(goalName)
                .targetAmount(targetAmount)
                .currentAmount(BigDecimal.ZERO)
                .monthlySaving(monthlySaving)
                .targetDate(targetDate)
                .status(GoalStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public BigDecimal progressPercent() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal current = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        return current.multiply(new BigDecimal("100"))
                .divide(targetAmount, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    public OptionalLong daysToCompletion() {
        if (monthlySaving == null || monthlySaving.compareTo(BigDecimal.ZERO) <= 0) {
            return OptionalLong.empty();
        }
        BigDecimal current = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        BigDecimal remaining = targetAmount.subtract(current);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return OptionalLong.of(0L);
        }
        BigDecimal dailyRate = monthlySaving.divide(DAYS_PER_MONTH, PERCENT_SCALE, RoundingMode.HALF_UP);
        if (dailyRate.compareTo(BigDecimal.ZERO) == 0) {
            return OptionalLong.empty();
        }
        long days = remaining.divide(dailyRate, 0, RoundingMode.CEILING).longValue();
        return OptionalLong.of(days);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private String goalName;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private BigDecimal monthlySaving;
        private LocalDate targetDate;
        private GoalStatus status;
        private String notes;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder goalName(String goalName) { this.goalName = goalName; return this; }
        public Builder targetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; return this; }
        public Builder currentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; return this; }
        public Builder monthlySaving(BigDecimal monthlySaving) { this.monthlySaving = monthlySaving; return this; }
        public Builder targetDate(LocalDate targetDate) { this.targetDate = targetDate; return this; }
        public Builder status(GoalStatus status) { this.status = status; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Goal build() {
            return new Goal(this);
        }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public String getGoalName() { return goalName; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public BigDecimal getMonthlySaving() { return monthlySaving; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
