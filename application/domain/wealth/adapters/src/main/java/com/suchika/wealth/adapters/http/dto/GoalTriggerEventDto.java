package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.GoalTriggerEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalTriggerEventDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("sequence_no")
    public int sequenceNo;

    @JsonProperty("event_name")
    public String eventName;

    @JsonProperty("trigger_condition")
    public String triggerCondition;

    @JsonProperty("resulting_change")
    public String resultingChange;

    public static GoalTriggerEventDto from(GoalTriggerEvent event) {
        GoalTriggerEventDto dto = new GoalTriggerEventDto();
        dto.id = event.getId();
        dto.sequenceNo = event.getSequenceNo();
        dto.eventName = event.getEventName();
        dto.triggerCondition = event.getTriggerCondition();
        dto.resultingChange = event.getResultingChange();
        return dto;
    }

    public GoalTriggerEvent toDomain() {
        return GoalTriggerEvent.create(id, sequenceNo, eventName, triggerCondition, resultingChange);
    }
}
