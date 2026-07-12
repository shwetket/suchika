package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;

import java.util.List;
import java.util.UUID;

/**
 * ADR-022 Phase 1 — CRUD + child-collection management for goal plans.
 * Every method is scoped by {@code adminId} (the household unit), not
 * {@code profileId} — same shape as {@code profile.admin.policy_settings}.
 */
public interface GoalPlanUseCase {

    GoalPlan createGoalPlan(UUID adminId, CreateGoalPlanCommand command);

    GoalPlan getGoalPlan(UUID id, UUID adminId);

    List<GoalPlan> listGoalPlans(UUID adminId);

    GoalPlan updateGoalPlan(UUID id, UUID adminId, UpdateGoalPlanCommand command);

    void deactivateGoalPlan(UUID id, UUID adminId);

    /**
     * Bulk-replace — authoring operation. Ordered, whole-document replace, per
     * ADR-022 ("authored as one document"). Validates sequence_no uniqueness
     * before persisting.
     */
    List<GoalMilestone> replaceMilestones(UUID goalPlanId, UUID adminId, List<GoalMilestone> milestones);

    List<GoalRule> replaceRules(UUID goalPlanId, UUID adminId, List<GoalRule> rules);

    List<GoalTriggerEvent> replaceTriggerEvents(UUID goalPlanId, UUID adminId, List<GoalTriggerEvent> triggerEvents);

    /**
     * Single-field achieved toggle for a checklist milestone (ADR-022) — separate
     * from the bulk-replace endpoint to avoid two admins racing on a whole-array
     * resend for a one-checkbox click. Rejects (400) when the target milestone's
     * is_manual_checklist is false — a formula-derived milestone's achieved status
     * is recomputed on every dashboard refresh and would silently be overwritten.
     */
    GoalMilestone updateMilestoneAchieved(UUID goalPlanId, UUID adminId, UUID milestoneId, boolean achieved);
}
