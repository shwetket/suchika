package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListDoctorVisitsResponse {

    @JsonProperty("doctor_visits")
    public List<DoctorVisitResponse> doctorVisits;

    @JsonProperty("total_size")
    public int totalSize;

    public ListDoctorVisitsResponse(List<DoctorVisitResponse> visits) {
        this.doctorVisits = visits;
        this.totalSize = visits.size();
    }
}
