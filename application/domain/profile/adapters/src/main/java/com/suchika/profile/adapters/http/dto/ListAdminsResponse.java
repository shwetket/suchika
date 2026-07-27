package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListAdminsResponse {

    @JsonProperty("admins")
    public List<AdminResponse> admins;

    @JsonProperty("total_size")
    public int totalSize;

    public ListAdminsResponse(List<AdminResponse> admins) {
        this.admins = admins;
        this.totalSize = admins.size();
    }
}
