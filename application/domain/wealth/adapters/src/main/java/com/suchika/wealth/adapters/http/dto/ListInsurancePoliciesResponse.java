package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListInsurancePoliciesResponse {

    @JsonProperty("insurance_policies")
    public List<InsurancePolicyResponse> insurancePolicies;

    @JsonProperty("total_size")
    public int totalSize;

    public ListInsurancePoliciesResponse() {}

    public ListInsurancePoliciesResponse(List<InsurancePolicyResponse> insurancePolicies) {
        this.insurancePolicies = insurancePolicies;
        this.totalSize = insurancePolicies.size();
    }
}
