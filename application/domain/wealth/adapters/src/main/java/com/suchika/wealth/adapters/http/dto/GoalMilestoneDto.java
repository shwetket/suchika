package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.GoalMilestone;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Doubles as both request and response shape for the milestone bulk-PUT (ADR-022:
 * "authored as one document") — the same fields are echoed back on read.
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalMilestoneDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("sequence_no")
    public int sequenceNo;

    @JsonProperty("label")
    public String label;

    @JsonProperty("target_value")
    public BigDecimal targetValue;

    @JsonProperty("is_manual_checklist")
    public boolean manualChecklist;

    @JsonProperty("is_achieved")
    public boolean achieved;

    @JsonProperty("significance")
    public String significance;

    public static GoalMilestoneDto from(GoalMilestone milestone) {
        GoalMilestoneDto dto = new GoalMilestoneDto();
        dto.id = milestone.getId();
        dto.sequenceNo = milestone.getSequenceNo();
        dto.label = milestone.getLabel();
        dto.targetValue = milestone.getTargetValue();
        dto.manualChecklist = milestone.isManualChecklist();
        dto.achieved = milestone.isAchieved();
        dto.significance = milestone.getSignificance();
        return dto;
    }

    public GoalMilestone toDomain() {
        return GoalMilestone.create(id, sequenceNo, label, targetValue, manualChecklist, achieved, significance);
    }
}
