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
public class DoctorVisitResponse {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("from_date")
    public LocalDate fromDate;

    @JsonProperty("to_date")
    public LocalDate toDate;

    @JsonProperty("visited_doctor")
    public boolean visitedDoctor;

    @JsonProperty("doctor_name")
    public String doctorName;

    @JsonProperty("hospital_name")
    public String hospitalName;

    @JsonProperty("speciality")
    public String speciality;

    @JsonProperty("symptoms")
    public String symptoms;

    @JsonProperty("diagnosis")
    public String diagnosis;

    @JsonProperty("notes")
    public String notes;

    @JsonProperty("follow_up_date")
    public LocalDate followUpDate;

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
