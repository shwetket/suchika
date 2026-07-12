package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.GoalTriggerEvent;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "goal_plan_trigger_event", schema = "wealth")
public class GoalPlanTriggerEventEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "goal_plan_id", nullable = false, columnDefinition = "uuid")
    public UUID goalPlanId;

    @Column(name = "sequence_no", nullable = false)
    public int sequenceNo;

    @Column(name = "event_name", nullable = false, length = 50)
    public String eventName;

    @Column(name = "trigger_condition", nullable = false)
    public String triggerCondition;

    @Column(name = "resulting_change", nullable = false)
    public String resultingChange;

    public static GoalPlanTriggerEventEntity from(UUID goalPlanId, GoalTriggerEvent event) {
        GoalPlanTriggerEventEntity e = new GoalPlanTriggerEventEntity();
        e.id = event.getId();
        e.goalPlanId = goalPlanId;
        e.sequenceNo = event.getSequenceNo();
        e.eventName = event.getEventName();
        e.triggerCondition = event.getTriggerCondition();
        e.resultingChange = event.getResultingChange();
        return e;
    }

    public GoalTriggerEvent toDomain() {
        return GoalTriggerEvent.create(id, sequenceNo, eventName, triggerCondition, resultingChange);
    }
}
