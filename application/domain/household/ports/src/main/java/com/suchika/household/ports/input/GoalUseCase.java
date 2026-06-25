package com.suchika.household.ports.input;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GoalUseCase {

    Goal create(UUID profileId, String goalName, BigDecimal targetAmount,
                BigDecimal monthlySaving, LocalDate targetDate, String notes);

    List<Goal> list(UUID profileId, GoalStatus status);

    Goal get(UUID id);

    Goal update(UUID id, String goalName, BigDecimal targetAmount,
                BigDecimal monthlySaving, LocalDate targetDate,
                GoalStatus status, String notes);

    void delete(UUID id);

    Goal updateCurrentAmount(UUID id, BigDecimal amount);
}
