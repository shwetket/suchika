package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListProfilesResponse {

    @JsonProperty("profiles")
    public List<ProfileResponse> profiles;

    @JsonProperty("total_size")
    public int totalSize;

    public ListProfilesResponse(List<ProfileResponse> profiles) {
        this.profiles = profiles;
        this.totalSize = profiles.size();
    }
}
