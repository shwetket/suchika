package com.suchika.health.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class DoctorVisit {

    private UUID id;
    private UUID profileId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private boolean visitedDoctor;
    private String doctorName;
    private String hospitalName;
    private String speciality;
    private String symptoms;
    private String diagnosis;
    private String notes;
    private LocalDate followUpDate;
    private Instant createdAt;

    public DoctorVisit() {}

    private DoctorVisit(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.visitedDoctor = builder.visitedDoctor;
        this.doctorName = builder.doctorName;
        this.hospitalName = builder.hospitalName;
        this.speciality = builder.speciality;
        this.symptoms = builder.symptoms;
        this.diagnosis = builder.diagnosis;
        this.notes = builder.notes;
        this.followUpDate = builder.followUpDate;
        this.createdAt = builder.createdAt;
    }

    public static DoctorVisit create(UUID profileId, LocalDate fromDate, LocalDate toDate,
                                      boolean visitedDoctor, String doctorName, VisitDetails details) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("to_date must not be before from_date");
        }
        if (visitedDoctor && (doctorName == null || doctorName.isBlank())) {
            throw new IllegalArgumentException("doctor_name is required when visited_doctor is true");
        }
        VisitDetails d = details != null ? details : VisitDetails.empty();
        return new Builder()
                .profileId(profileId)
                .fromDate(fromDate)
                .toDate(toDate)
                .visitedDoctor(visitedDoctor)
                .doctorName(doctorName)
                .hospitalName(d.hospitalName)
                .speciality(d.speciality)
                .symptoms(d.symptoms)
                .diagnosis(d.diagnosis)
                .notes(d.notes)
                .followUpDate(d.followUpDate)
                .build();
    }

    /** Groups the narrative/optional fields that carry no create()-time validation rule. */
    public static final class VisitDetails {
        private final String hospitalName;
        private final String speciality;
        private final String symptoms;
        private final String diagnosis;
        private final String notes;
        private final LocalDate followUpDate;

        public VisitDetails(String hospitalName, String speciality, String symptoms,
                             String diagnosis, String notes, LocalDate followUpDate) {
            this.hospitalName = hospitalName;
            this.speciality = speciality;
            this.symptoms = symptoms;
            this.diagnosis = diagnosis;
            this.notes = notes;
            this.followUpDate = followUpDate;
        }

        public static VisitDetails empty() {
            return new VisitDetails(null, null, null, null, null, null);
        }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private LocalDate fromDate;
        private LocalDate toDate;
        private boolean visitedDoctor = true;
        private String doctorName;
        private String hospitalName;
        private String speciality;
        private String symptoms;
        private String diagnosis;
        private String notes;
        private LocalDate followUpDate;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder fromDate(LocalDate fromDate) { this.fromDate = fromDate; return this; }
        public Builder toDate(LocalDate toDate) { this.toDate = toDate; return this; }
        public Builder visitedDoctor(boolean visitedDoctor) { this.visitedDoctor = visitedDoctor; return this; }
        public Builder doctorName(String doctorName) { this.doctorName = doctorName; return this; }
        public Builder hospitalName(String hospitalName) { this.hospitalName = hospitalName; return this; }
        public Builder speciality(String speciality) { this.speciality = speciality; return this; }
        public Builder symptoms(String symptoms) { this.symptoms = symptoms; return this; }
        public Builder diagnosis(String diagnosis) { this.diagnosis = diagnosis; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder followUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public DoctorVisit build() { return new DoctorVisit(this); }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public boolean isVisitedDoctor() { return visitedDoctor; }
    public String getDoctorName() { return doctorName; }
    public String getHospitalName() { return hospitalName; }
    public String getSpeciality() { return speciality; }
    public String getSymptoms() { return symptoms; }
    public String getDiagnosis() { return diagnosis; }
    public String getNotes() { return notes; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public Instant getCreatedAt() { return createdAt; }
}
