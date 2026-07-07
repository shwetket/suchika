package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListVitalReadingsResponse {

    @JsonProperty("vital_readings")
    public List<VitalReadingResponse> vitalReadings;

    @JsonProperty("total_size")
    public long totalSize;

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListVitalReadingsResponse(List<VitalReadingResponse> readings) {
        this.vitalReadings = readings;
        this.totalSize = readings.size();
    }

    /**
     * Paginated variant (pre-v1.0 pagination pass, Q54) — totalSize is the grand
     * total matching the filter across all pages, not just readings.size().
     */
    public ListVitalReadingsResponse(List<VitalReadingResponse> readings, long totalSize, int page, int size) {
        this.vitalReadings = readings;
        this.totalSize = totalSize;
        this.page = page;
        this.size = size;
    }
}
