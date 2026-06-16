package com.suchika.health.adapters.services;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.input.DoctorVisitUseCase;
import com.suchika.health.ports.output.DoctorVisitRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DoctorVisitService implements DoctorVisitUseCase {

    private final DoctorVisitRepository repository;

    public DoctorVisitService(DoctorVisitRepository repository) {
        this.repository = repository;
    }

    @Override
    public DoctorVisit create(UUID profileId, LocalDate fromDate, LocalDate toDate,
                              boolean visitedDoctor, String doctorName, String hospitalName,
                              String speciality, String symptoms, String diagnosis,
                              String notes, LocalDate followUpDate) {
        validateCreate(profileId, fromDate, toDate, visitedDoctor, doctorName);

        DoctorVisit visit = DoctorVisit.builder()
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
                .build();

        DoctorVisit saved = repository.save(visit);
        AppLogger.info("Created doctor visit for profile %s on %s", profileId, fromDate);
        return saved;
    }

    @Override
    public DoctorVisit getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor visit not found: " + id));
    }

    @Override
    public List<DoctorVisit> listByProfile(UUID profileId) {
        if (profileId == null) {
            throw new BadRequestException("profile_id is required");
        }
        return repository.findByProfileId(profileId);
    }

    @Override
    public DoctorVisit update(UUID id, LocalDate toDate, String doctorName, String hospitalName,
                              String speciality, String symptoms, String diagnosis,
                              String notes, LocalDate followUpDate) {
        DoctorVisit existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor visit not found: " + id));

        if (toDate != null && toDate.isBefore(existing.getFromDate())) {
            throw new BadRequestException("to_date cannot be before from_date");
        }

        DoctorVisit updated = DoctorVisit.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .fromDate(existing.getFromDate())
                .toDate(toDate != null ? toDate : existing.getToDate())
                .visitedDoctor(existing.isVisitedDoctor())
                .doctorName(doctorName != null ? doctorName : existing.getDoctorName())
                .hospitalName(hospitalName != null ? hospitalName : existing.getHospitalName())
                .speciality(speciality != null ? speciality : existing.getSpeciality())
                .symptoms(symptoms != null ? symptoms : existing.getSymptoms())
                .diagnosis(diagnosis != null ? diagnosis : existing.getDiagnosis())
                .notes(notes != null ? notes : existing.getNotes())
                .followUpDate(followUpDate != null ? followUpDate : existing.getFollowUpDate())
                .createdAt(existing.getCreatedAt())
                .build();

        return repository.save(updated);
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Doctor visit not found: " + id);
        }
        repository.deleteById(id);
        AppLogger.info("Deleted doctor visit %s", id);
    }

    private void validateCreate(UUID profileId, LocalDate fromDate, LocalDate toDate,
                                boolean visitedDoctor, String doctorName) {
        if (profileId == null) throw new BadRequestException("profile_id is required");
        if (fromDate == null) throw new BadRequestException("from_date is required");
        if (toDate != null && toDate.isBefore(fromDate)) {
            throw new BadRequestException("to_date cannot be before from_date");
        }
        if (visitedDoctor && (doctorName == null || doctorName.isBlank())) {
            throw new BadRequestException("doctor_name is required when visited_doctor is true");
        }
    }
}
