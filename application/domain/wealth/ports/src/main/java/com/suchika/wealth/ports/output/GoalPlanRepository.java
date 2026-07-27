package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.domain.GoalType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalPlanRepository {

    /**
     * Saves the goal_plan row itself only — does not touch child collections.
     * Child collections are persisted via the dedicated replace* methods below,
     * matching the bulk-PUT-as-authoring-operation shape (ADR-022).
     */
    GoalPlan save(GoalPlan goalPlan);

    Optional<GoalPlan> findById(UUID id, UUID adminId);

    List<GoalPlan> findAll(UUID adminId);

    boolean existsByAdminGoalTypeBeneficiary(UUID adminId, GoalType goalType, UUID beneficiaryProfileId);

    List<GoalMilestone> findMilestones(UUID goalPlanId);

    List<GoalMilestone> replaceMilestones(UUID goalPlanId, List<GoalMilestone> milestones);

    List<GoalRule> findRules(UUID goalPlanId);

    List<GoalRule> replaceRules(UUID goalPlanId, List<GoalRule> rules);

    List<GoalTriggerEvent> findTriggerEvents(UUID goalPlanId);

    List<GoalTriggerEvent> replaceTriggerEvents(UUID goalPlanId, List<GoalTriggerEvent> triggerEvents);

    Optional<GoalMilestone> findMilestoneById(UUID goalPlanId, UUID milestoneId);

    GoalMilestone saveMilestone(UUID goalPlanId, GoalMilestone milestone);
}
