package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.health.domain.DoctorVisit;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorVisitResponse extends DoctorVisitFields {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("from_date")
    public LocalDate fromDate;

    @JsonProperty("visited_doctor")
    public boolean visitedDoctor;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static DoctorVisitResponse from(DoctorVisit visit) {
        DoctorVisitResponse r = new DoctorVisitResponse();
        r.id = visit.getId();
        r.profileId = visit.getProfileId();
        r.fromDate = visit.getFromDate();
        r.toDate = visit.getToDate();
        r.visitedDoctor = visit.isVisitedDoctor();
        r.doctorName = visit.getDoctorName();
        r.hospitalName = visit.getHospitalName();
        r.speciality = visit.getSpeciality();
        r.symptoms = visit.getSymptoms();
        r.diagnosis = visit.getDiagnosis();
        r.notes = visit.getNotes();
        r.followUpDate = visit.getFollowUpDate();
        r.createdAt = visit.getCreatedAt();
        return r;
    }
}
