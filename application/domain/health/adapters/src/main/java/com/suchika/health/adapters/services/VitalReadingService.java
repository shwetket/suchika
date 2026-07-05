package com.suchika.health.adapters.services;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.input.UpdateVitalReadingCommand;
import com.suchika.health.ports.input.VitalReadingUseCase;
import com.suchika.health.ports.output.VitalReadingRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VitalReadingService implements VitalReadingUseCase {

    private final VitalReadingRepository repository;

    public VitalReadingService(VitalReadingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public VitalReading recordReading(UUID profileId, VitalType vitalType, LocalDate readingDate,
                               BigDecimal valuePrimary, BigDecimal valueSecondary,
                               String unit, String notes) {
        validate(profileId, vitalType, readingDate, valuePrimary, valueSecondary, unit);

        VitalReading reading = VitalReading.create(profileId, vitalType, readingDate,
                valuePrimary, valueSecondary, unit, notes);

        VitalReading saved = repository.save(reading);
        AppLogger.info("Recorded vital reading %s for profile %s on %s", vitalType, profileId, readingDate);
        return saved;
    }

    @Override
    public VitalReading getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vital reading not found: " + id));
    }

    @Override
    public List<VitalReading> listByProfile(UUID profileId, VitalType vitalType) {
        if (profileId == null) {
            throw new BadRequestException("profile_id is required");
        }
        return repository.findByProfileId(profileId, vitalType);
    }

    @Override
    @Transactional
    public VitalReading update(UUID id, UpdateVitalReadingCommand command) {
        VitalReading existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vital reading not found: " + id));

        LocalDate readingDate = command.readingDate() != null ? command.readingDate() : existing.getReadingDate();
        BigDecimal valuePrimary = command.valuePrimary() != null ? command.valuePrimary() : existing.getValuePrimary();
        BigDecimal valueSecondary = command.valueSecondary() != null ? command.valueSecondary() : existing.getValueSecondary();
        String unit = command.unit() != null ? command.unit() : existing.getUnit();
        String notes = command.notes() != null ? command.notes() : existing.getNotes();

        validate(existing.getProfileId(), existing.getVitalType(), readingDate, valuePrimary, valueSecondary, unit);

        VitalReading updated = VitalReading.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .vitalType(existing.getVitalType())
                .readingDate(readingDate)
                .valuePrimary(valuePrimary)
                .valueSecondary(valueSecondary)
                .unit(unit)
                .notes(notes)
                .createdAt(existing.getCreatedAt())
                .build();

        VitalReading saved = repository.save(updated);
        AppLogger.info("Updated vital reading %s", id);
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Vital reading not found: " + id);
        }
        repository.deleteById(id);
        AppLogger.info("Deleted vital reading %s", id);
    }

    private void validate(UUID profileId, VitalType vitalType, LocalDate readingDate,
                          BigDecimal valuePrimary, BigDecimal valueSecondary, String unit) {
        if (profileId == null) throw new BadRequestException("profile_id is required");
        if (vitalType == null) throw new BadRequestException("vital_type is required");
        if (readingDate == null) throw new BadRequestException("reading_date is required");
        if (valuePrimary == null || valuePrimary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("value_primary must be greater than 0");
        }
        if (unit == null || unit.isBlank()) throw new BadRequestException("unit is required");
        if (vitalType == VitalType.BLOOD_PRESSURE && valueSecondary == null) {
            throw new BadRequestException("value_secondary (diastolic) is required for BLOOD_PRESSURE");
        }
    }
}
