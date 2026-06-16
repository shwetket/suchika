package com.suchika.health.adapters.http.dto;

import java.util.List;

public class ListVitalReadingsResponse {
    public List<VitalReadingResponse> vital_readings;
    public int total_size;

    public ListVitalReadingsResponse(List<VitalReadingResponse> readings) {
        this.vital_readings = readings;
        this.total_size = readings.size();
    }
}
