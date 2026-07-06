package com.suchika.health.adapters.services;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.input.CreateDoctorVisitCommand;
import com.suchika.health.ports.input.DoctorVisitUseCase;
import com.suchika.health.ports.input.UpdateDoctorVisitCommand;
import com.suchika.health.ports.output.DoctorVisitRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

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
    @Transactional
    public DoctorVisit create(CreateDoctorVisitCommand command) {
        validateCreate(command.profileId(), command.fromDate(), command.toDate(),
                command.visitedDoctor(), command.doctorName());

        DoctorVisit visit = DoctorVisit.create(command.profileId(), command.fromDate(), command.toDate(),
                command.visitedDoctor(), command.doctorName(),
                new DoctorVisit.VisitDetails(command.hospitalName(), command.speciality(),
                        command.symptoms(), command.diagnosis(), command.notes(), command.followUpDate()));

        DoctorVisit saved = repository.save(visit);
        AppLogger.info("Created doctor visit for profile %s on %s", command.profileId(), command.fromDate());
        return saved;
    }

    @Override
    public DoctorVisit getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor visit not found: " + id));
    }

    @Override
    public List<DoctorVisit> listByProfile(UUID profileId, LocalDate from, LocalDate to) {
        if (profileId == null) {
            throw new BadRequestException("profile_id is required");
        }
        return repository.findByProfileId(profileId, from, to);
    }

    @Override
    @Transactional
    public DoctorVisit update(UUID id, UpdateDoctorVisitCommand command) {
        DoctorVisit existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor visit not found: " + id));

        LocalDate toDate = command.toDate();
        if (toDate != null && toDate.isBefore(existing.getFromDate())) {
            throw new BadRequestException("to_date cannot be before from_date");
        }

        DoctorVisit updated = DoctorVisit.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .fromDate(existing.getFromDate())
                .toDate(toDate != null ? toDate : existing.getToDate())
                .visitedDoctor(existing.isVisitedDoctor())
                .doctorName(command.doctorName() != null ? command.doctorName() : existing.getDoctorName())
                .hospitalName(command.hospitalName() != null ? command.hospitalName() : existing.getHospitalName())
                .speciality(command.speciality() != null ? command.speciality() : existing.getSpeciality())
                .symptoms(command.symptoms() != null ? command.symptoms() : existing.getSymptoms())
                .diagnosis(command.diagnosis() != null ? command.diagnosis() : existing.getDiagnosis())
                .notes(command.notes() != null ? command.notes() : existing.getNotes())
                .followUpDate(command.followUpDate() != null ? command.followUpDate() : existing.getFollowUpDate())
                .createdAt(existing.getCreatedAt())
                .build();

        return repository.save(updated);
    }

    @Override
    @Transactional
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
