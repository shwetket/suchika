package com.suchika.household.adapters.service;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.input.GoalUseCase;
import com.suchika.household.ports.output.GoalRepository;
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
public class GoalService implements GoalUseCase {

    private static final String GOAL_NOT_FOUND = "Goal not found: ";

    private final GoalRepository repository;

    public GoalService(GoalRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Goal create(UUID profileId, String goalName, BigDecimal targetAmount,
                       BigDecimal monthlySaving, LocalDate targetDate, String notes) {
        Goal goal = Goal.create(profileId, goalName, targetAmount, monthlySaving, targetDate, notes);
        Goal saved = repository.save(goal);
        AppLogger.info("Goal created: %s for profile: %s", saved.getId(), profileId);
        return saved;
    }

    @Override
    public List<Goal> list(UUID profileId, GoalStatus status) {
        return repository.findByProfileId(profileId, status);
    }

    @Override
    public Goal get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(GOAL_NOT_FOUND + id));
    }

    @Override
    @Transactional
    public Goal update(UUID id, String goalName, BigDecimal targetAmount,
                       BigDecimal monthlySaving, LocalDate targetDate,
                       GoalStatus status, String notes) {
        Goal existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(GOAL_NOT_FOUND + id));

        Goal updated = Goal.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .goalName(goalName != null ? goalName : existing.getGoalName())
                .targetAmount(targetAmount != null ? targetAmount : existing.getTargetAmount())
                .currentAmount(existing.getCurrentAmount())
                .monthlySaving(monthlySaving != null ? monthlySaving : existing.getMonthlySaving())
                .targetDate(targetDate != null ? targetDate : existing.getTargetDate())
                .status(status != null ? status : existing.getStatus())
                .notes(notes != null ? notes : existing.getNotes())
                .createdAt(existing.getCreatedAt())
                .build();

        return repository.save(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(GOAL_NOT_FOUND + id);
        }
        repository.deleteById(id);
        AppLogger.info("Goal deleted: %s", id);
    }

    @Override
    @Transactional
    public Goal updateCurrentAmount(UUID id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("current_amount must not be negative");
        }
        Goal existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(GOAL_NOT_FOUND + id));

        Goal updated = Goal.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .goalName(existing.getGoalName())
                .targetAmount(existing.getTargetAmount())
                .currentAmount(amount)
                .monthlySaving(existing.getMonthlySaving())
                .targetDate(existing.getTargetDate())
                .status(existing.getStatus())
                .notes(existing.getNotes())
                .createdAt(existing.getCreatedAt())
                .build();

        return repository.save(updated);
    }
}
