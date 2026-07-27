package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goal_plan", schema = "wealth")
public class GoalPlanEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "admin_id", nullable = false, columnDefinition = "uuid")
    public UUID adminId;

    @Column(name = "goal_type", nullable = false, length = 50)
    public String goalType;

    @Column(name = "beneficiary_profile_id", columnDefinition = "uuid")
    public UUID beneficiaryProfileId;

    @Column(name = "objective", nullable = false)
    public String objective;

    @Column(name = "target_state")
    public String targetState;

    @Column(name = "assumed_growth_rate", precision = 7, scale = 4)
    public BigDecimal assumedGrowthRate;

    @Column(name = "education_base_cost", precision = 19, scale = 4)
    public BigDecimal educationBaseCost;

    @Column(name = "education_inflation_rate", precision = 7, scale = 4)
    public BigDecimal educationInflationRate;

    @Column(name = "education_years_to_entry")
    public Integer educationYearsToEntry;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb", nullable = false)
    public String detail = "{}";

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public static GoalPlanEntity from(GoalPlan plan) {
        GoalPlanEntity e = new GoalPlanEntity();
        e.id = plan.getId();
        e.adminId = plan.getAdminId();
        e.goalType = plan.getGoalType() != null ? plan.getGoalType().name() : null;
        e.beneficiaryProfileId = plan.getBeneficiaryProfileId();
        e.objective = plan.getObjective();
        e.targetState = plan.getTargetState();
        e.assumedGrowthRate = plan.getAssumedGrowthRate();
        e.educationBaseCost = plan.getEducationBaseCost();
        e.educationInflationRate = plan.getEducationInflationRate();
        e.educationYearsToEntry = plan.getEducationYearsToEntry();
        e.detail = JsonbMetadataUtil.write(plan.getDetail());
        e.active = plan.isActive();
        e.createdAt = plan.getCreatedAt();
        e.updatedAt = plan.getUpdatedAt();
        return e;
    }

    public GoalPlan toDomain() {
        return GoalPlan.builder()
                .id(id)
                .adminId(adminId)
                .goalType(GoalType.valueOf(goalType))
                .beneficiaryProfileId(beneficiaryProfileId)
                .objective(objective)
                .targetState(targetState)
                .assumedGrowthRate(assumedGrowthRate)
                .educationBaseCost(educationBaseCost)
                .educationInflationRate(educationInflationRate)
                .educationYearsToEntry(educationYearsToEntry)
                .detail(JsonbMetadataUtil.read(detail))
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
