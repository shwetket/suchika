package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.DoctorVisit;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "doctor_visit", schema = "health")
public class DoctorVisitEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", nullable = false, columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "from_date", nullable = false)
    public LocalDate fromDate;

    @Column(name = "to_date")
    public LocalDate toDate;

    @Column(name = "visited_doctor", nullable = false)
    public boolean visitedDoctor = true;

    @Column(name = "doctor_name", length = 100)
    public String doctorName;

    @Column(name = "hospital_name", length = 200)
    public String hospitalName;

    @Column(name = "speciality", length = 100)
    public String speciality;

    @Column(name = "symptoms")
    public String symptoms;

    @Column(name = "diagnosis")
    public String diagnosis;

    @Column(name = "notes")
    public String notes;

    @Column(name = "follow_up_date")
    public LocalDate followUpDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static DoctorVisitEntity from(DoctorVisit visit) {
        DoctorVisitEntity e = new DoctorVisitEntity();
        e.id = visit.getId();
        e.profileId = visit.getProfileId();
        e.fromDate = visit.getFromDate();
        e.toDate = visit.getToDate();
        e.visitedDoctor = visit.isVisitedDoctor();
        e.doctorName = visit.getDoctorName();
        e.hospitalName = visit.getHospitalName();
        e.speciality = visit.getSpeciality();
        e.symptoms = visit.getSymptoms();
        e.diagnosis = visit.getDiagnosis();
        e.notes = visit.getNotes();
        e.followUpDate = visit.getFollowUpDate();
        e.createdAt = visit.getCreatedAt();
        return e;
    }

    public DoctorVisit toDomain() {
        return DoctorVisit.builder()
                .id(id)
                .profileId(profileId)
                .fromDate(fromDate)
                .toDate(toDate)
                .visitedDoctor(visitedDoctor)
                .doctorName(doctorName)
                .hospitalName(hospitalName)
                .speciality(speciality)
                .symptoms(symptoms)
                .diagnosis(diagnosis)
                .notes(notes)
                .followUpDate(followUpDate)
                .createdAt(createdAt)
                .build();
    }
}
