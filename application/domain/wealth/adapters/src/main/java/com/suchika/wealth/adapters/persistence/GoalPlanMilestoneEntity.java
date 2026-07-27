package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.GoalMilestone;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "goal_plan_milestone", schema = "wealth")
public class GoalPlanMilestoneEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "goal_plan_id", nullable = false, columnDefinition = "uuid")
    public UUID goalPlanId;

    @Column(name = "sequence_no", nullable = false)
    public int sequenceNo;

    @Column(name = "label", nullable = false, length = 50)
    public String label;

    @Column(name = "target_value", precision = 19, scale = 4)
    public BigDecimal targetValue;

    @Column(name = "is_manual_checklist", nullable = false)
    public boolean manualChecklist;

    @Column(name = "is_achieved", nullable = false)
    public boolean achieved;

    @Column(name = "significance", nullable = false)
    public String significance;

    public static GoalPlanMilestoneEntity from(UUID goalPlanId, GoalMilestone milestone) {
        GoalPlanMilestoneEntity e = new GoalPlanMilestoneEntity();
        e.id = milestone.getId();
        e.goalPlanId = goalPlanId;
        e.sequenceNo = milestone.getSequenceNo();
        e.label = milestone.getLabel();
        e.targetValue = milestone.getTargetValue();
        e.manualChecklist = milestone.isManualChecklist();
        e.achieved = milestone.isAchieved();
        e.significance = milestone.getSignificance();
        return e;
    }

    public GoalMilestone toDomain() {
        return GoalMilestone.create(id, sequenceNo, label, targetValue, manualChecklist, achieved, significance);
    }
}
