package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.household.domain.Goal;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("goal_name")
    public String goalName;

    @JsonProperty("target_amount")
    public BigDecimal targetAmount;

    @JsonProperty("current_amount")
    public BigDecimal currentAmount;

    @JsonProperty("monthly_saving")
    public BigDecimal monthlySaving;

    @JsonProperty("target_date")
    public LocalDate targetDate;

    @JsonProperty("status")
    public String status;

    @JsonProperty("notes")
    public String notes;

    @JsonProperty("progress_percent")
    public BigDecimal progressPercent;

    @JsonProperty("days_to_completion")
    public Long daysToCompletion;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static GoalDto from(Goal goal) {
        GoalDto dto = new GoalDto();
        dto.id = goal.getId();
        dto.profileId = goal.getProfileId();
        dto.goalName = goal.getGoalName();
        dto.targetAmount = goal.getTargetAmount();
        dto.currentAmount = goal.getCurrentAmount();
        dto.monthlySaving = goal.getMonthlySaving();
        dto.targetDate = goal.getTargetDate();
        dto.status = goal.getStatus() != null ? goal.getStatus().name() : null;
        dto.notes = goal.getNotes();
        dto.progressPercent = goal.progressPercent();
        dto.daysToCompletion = goal.daysToCompletion().isPresent() ? goal.daysToCompletion().getAsLong() : null;
        dto.createdAt = goal.getCreatedAt();
        return dto;
    }
}
