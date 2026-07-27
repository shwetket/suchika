package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListDoctorVisitsResponse {

    @JsonProperty("doctor_visits")
    public List<DoctorVisitResponse> doctorVisits;

    @JsonProperty("total_size")
    public long totalSize;

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListDoctorVisitsResponse(List<DoctorVisitResponse> visits) {
        this.doctorVisits = visits;
        this.totalSize = visits.size();
    }

    /**
     * Paginated variant (pre-v1.0 pagination pass, Q54) — totalSize is the grand
     * total matching the filter across all pages, not just visits.size().
     */
    public ListDoctorVisitsResponse(List<DoctorVisitResponse> visits, long totalSize, int page, int size) {
        this.doctorVisits = visits;
        this.totalSize = totalSize;
        this.page = page;
        this.size = size;
    }
}
