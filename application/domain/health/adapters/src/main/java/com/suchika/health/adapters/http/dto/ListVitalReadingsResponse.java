package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListVitalReadingsResponse {

    @JsonProperty("vital_readings")
    public List<VitalReadingResponse> vitalReadings;

    @JsonProperty("total_size")
    public int totalSize;

    public ListVitalReadingsResponse(List<VitalReadingResponse> readings) {
        this.vitalReadings = readings;
        this.totalSize = readings.size();
    }
}
