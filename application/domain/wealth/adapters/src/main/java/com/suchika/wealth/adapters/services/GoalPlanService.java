package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.ports.input.CreateGoalPlanCommand;
import com.suchika.wealth.ports.input.GoalPlanUseCase;
import com.suchika.wealth.ports.input.UpdateGoalPlanCommand;
import com.suchika.wealth.ports.output.GoalPlanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class GoalPlanService implements GoalPlanUseCase {

    private static final String GOAL_PLAN_NOT_FOUND = "Goal plan not found: ";
    private static final String MILESTONE_NOT_FOUND = "Milestone not found: ";

    private final GoalPlanRepository repository;

    public GoalPlanService(GoalPlanRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public GoalPlan createGoalPlan(UUID adminId, CreateGoalPlanCommand command) {
        GoalPlan plan = GoalPlan.create(
                adminId,
                command.goalType(),
                command.beneficiaryProfileId(),
                command.objective(),
                command.targetState(),
                command.assumedGrowthRate(),
                command.educationBaseCost(),
                command.educationInflationRate(),
                command.educationYearsToEntry());

        if (repository.existsByAdminGoalTypeBeneficiary(adminId, command.goalType(), command.beneficiaryProfileId())) {
            throw new ConflictException("A goal plan for " + command.goalType()
                    + (command.beneficiaryProfileId() != null ? " and beneficiary " + command.beneficiaryProfileId() : "")
                    + " already exists for this admin");
        }

        GoalPlan saved = repository.save(plan);
        AppLogger.info("Goal plan created: %s (%s)", saved.getId(), command.goalType());
        return saved;
    }

    @Override
    public GoalPlan getGoalPlan(UUID id, UUID adminId) {
        GoalPlan plan = findOrThrow(id, adminId);
        return withChildren(plan);
    }

    @Override
    public List<GoalPlan> listGoalPlans(UUID adminId) {
        return repository.findAll(adminId).stream().map(this::withChildren).toList();
    }

    private GoalPlan withChildren(GoalPlan plan) {
        plan.setMilestones(repository.findMilestones(plan.getId()));
        plan.setRules(repository.findRules(plan.getId()));
        plan.setTriggerEvents(repository.findTriggerEvents(plan.getId()));
        return plan;
    }

    @Override
    @Transactional
    public GoalPlan updateGoalPlan(UUID id, UUID adminId, UpdateGoalPlanCommand command) {
        GoalPlan plan = findOrThrow(id, adminId);

        String objective = command.objective();
        if (objective != null) {
            if (objective.isBlank()) throw new BadRequestException("objective must not be blank");
            plan.setObjective(objective);
        }
        if (command.targetState() != null) plan.setTargetState(command.targetState());
        if (command.assumedGrowthRate() != null) plan.setAssumedGrowthRate(command.assumedGrowthRate());
        if (command.educationBaseCost() != null) plan.setEducationBaseCost(command.educationBaseCost());
        if (command.educationInflationRate() != null) plan.setEducationInflationRate(command.educationInflationRate());
        if (command.educationYearsToEntry() != null) plan.setEducationYearsToEntry(command.educationYearsToEntry());
        if (command.detail() != null) {
            Map<String, String> merged = new HashMap<>(plan.getDetail());
            merged.putAll(command.detail());
            plan.setDetail(merged);
        }
        if (command.isActive() != null) plan.setActive(command.isActive());

        return withChildren(repository.save(plan));
    }

    @Override
    @Transactional
    public void deactivateGoalPlan(UUID id, UUID adminId) {
        GoalPlan plan = findOrThrow(id, adminId);
        plan.setActive(false);
        repository.save(plan);
        AppLogger.info("Goal plan deactivated: %s", id);
    }

    @Override
    @Transactional
    public List<GoalMilestone> replaceMilestones(UUID goalPlanId, UUID adminId, List<GoalMilestone> milestones) {
        findOrThrow(goalPlanId, adminId);
        GoalPlan.validateUniqueSequence(milestones.stream().map(GoalMilestone::getSequenceNo).toList(), "milestones");
        List<GoalMilestone> saved = repository.replaceMilestones(goalPlanId, milestones);
        AppLogger.info("Goal plan %s milestones replaced (%d)", goalPlanId, saved.size());
        return saved;
    }

    @Override
    @Transactional
    public List<GoalRule> replaceRules(UUID goalPlanId, UUID adminId, List<GoalRule> rules) {
        findOrThrow(goalPlanId, adminId);
        GoalPlan.validateUniqueSequence(rules.stream().map(GoalRule::getSequenceNo).toList(), "rules");
        List<GoalRule> saved = repository.replaceRules(goalPlanId, rules);
        AppLogger.info("Goal plan %s rules replaced (%d)", goalPlanId, saved.size());
        return saved;
    }

    @Override
    @Transactional
    public List<GoalTriggerEvent> replaceTriggerEvents(UUID goalPlanId, UUID adminId, List<GoalTriggerEvent> triggerEvents) {
        findOrThrow(goalPlanId, adminId);
        GoalPlan.validateUniqueSequence(triggerEvents.stream().map(GoalTriggerEvent::getSequenceNo).toList(), "trigger_events");
        List<GoalTriggerEvent> saved = repository.replaceTriggerEvents(goalPlanId, triggerEvents);
        AppLogger.info("Goal plan %s trigger events replaced (%d)", goalPlanId, saved.size());
        return saved;
    }

    @Override
    @Transactional
    public GoalMilestone updateMilestoneAchieved(UUID goalPlanId, UUID adminId, UUID milestoneId, boolean achieved) {
        findOrThrow(goalPlanId, adminId);
        GoalMilestone milestone = repository.findMilestoneById(goalPlanId, milestoneId)
                .orElseThrow(() -> new NotFoundException(MILESTONE_NOT_FOUND + milestoneId));

        if (!milestone.isManualChecklist()) {
            throw new BadRequestException(
                    "Milestone " + milestoneId + " is not a manual checklist item — its achieved status "
                            + "is derived automatically and cannot be toggled directly");
        }

        milestone.setAchieved(achieved);
        return repository.saveMilestone(goalPlanId, milestone);
    }

    private GoalPlan findOrThrow(UUID id, UUID adminId) {
        return repository.findById(id, adminId)
                .orElseThrow(() -> new NotFoundException(GOAL_PLAN_NOT_FOUND + id));
    }
}
