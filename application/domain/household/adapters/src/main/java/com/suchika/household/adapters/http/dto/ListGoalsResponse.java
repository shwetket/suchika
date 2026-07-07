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

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListGoalsResponse(List<GoalDto> goals) {
        this.goals = goals;
        this.totalSize = goals.size();
    }

    /**
     * Paginated variant (Q54 pagination pass) — totalSize is the grand total matching
     * the filter across all pages, not just goals.size().
     */
    public ListGoalsResponse(List<GoalDto> goals, long totalSize, int page, int size) {
        this.goals = goals;
        this.totalSize = (int) totalSize;
        this.page = page;
        this.size = size;
    }
}
