package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListGoalsResponse {

    @JsonProperty("goals")
    public List<GoalDto> goals;

    @JsonProperty("total_size")
    public int totalSize;

    public ListGoalsResponse(List<GoalDto> goals) {
        this.goals = goals;
        this.totalSize = goals.size();
    }
}
