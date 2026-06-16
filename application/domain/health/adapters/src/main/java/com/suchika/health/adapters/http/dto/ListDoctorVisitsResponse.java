package com.suchika.health.adapters.http.dto;

import java.util.List;

public class ListDoctorVisitsResponse {
    public List<DoctorVisitResponse> doctor_visits;
    public int total_size;

    public ListDoctorVisitsResponse(List<DoctorVisitResponse> visits) {
        this.doctor_visits = visits;
        this.total_size = visits.size();
    }
}
