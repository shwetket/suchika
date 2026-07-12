package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListGoalPlansResponse {

    @JsonProperty("goal_plans")
    public List<GoalPlanResponse> goalPlans;

    @JsonProperty("total_size")
    public long totalSize;

    public ListGoalPlansResponse(List<GoalPlanResponse> goalPlans) {
        this.goalPlans = goalPlans;
        this.totalSize = goalPlans.size();
    }
}
