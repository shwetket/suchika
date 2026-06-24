package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goal", schema = "household")
public class GoalEntity extends PanacheEntityBase {

    private static final String DEFAULT_STATUS = "ACTIVE";

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "goal_name", nullable = false, length = 200)
    public String goalName;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "monthly_saving", precision = 19, scale = 2)
    public BigDecimal monthlySaving;

    @Column(name = "target_date")
    public LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    public String status = DEFAULT_STATUS;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = DEFAULT_STATUS;
        if (currentAmount == null) currentAmount = BigDecimal.ZERO;
    }

    public static GoalEntity from(Goal goal) {
        GoalEntity e = new GoalEntity();
        e.id = goal.getId();
        e.profileId = goal.getProfileId();
        e.goalName = goal.getGoalName();
        e.targetAmount = goal.getTargetAmount();
        e.currentAmount = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        e.monthlySaving = goal.getMonthlySaving();
        e.targetDate = goal.getTargetDate();
        e.status = goal.getStatus() != null ? goal.getStatus().name() : DEFAULT_STATUS;
        e.notes = goal.getNotes();
        e.createdAt = goal.getCreatedAt();
        return e;
    }

    public Goal toDomain() {
        return Goal.builder()
                .id(id)
                .profileId(profileId)
                .goalName(goalName)
                .targetAmount(targetAmount)
                .currentAmount(currentAmount)
                .monthlySaving(monthlySaving)
                .targetDate(targetDate)
                .status(status != null ? GoalStatus.valueOf(status) : GoalStatus.ACTIVE)
                .notes(notes)
                .createdAt(createdAt)
                .build();
    }
}
