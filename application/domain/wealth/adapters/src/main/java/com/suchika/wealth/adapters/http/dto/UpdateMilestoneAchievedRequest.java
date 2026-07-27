package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UpdateMilestoneAchievedRequest {

    @JsonProperty("is_achieved")
    public Boolean achieved;
}
