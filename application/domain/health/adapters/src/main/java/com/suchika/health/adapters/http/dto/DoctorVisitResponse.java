package com.suchika.health.adapters.http.dto;

import com.suchika.health.domain.DoctorVisit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class DoctorVisitResponse {

    public UUID id;
    public UUID profileId;
    public LocalDate fromDate;
    public LocalDate toDate;
    public boolean visitedDoctor;
    public String doctorName;
    public String hospitalName;
    public String speciality;
    public String symptoms;
    public String diagnosis;
    public String notes;
    public LocalDate followUpDate;
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
