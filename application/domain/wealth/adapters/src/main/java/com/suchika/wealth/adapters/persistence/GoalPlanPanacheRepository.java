package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.domain.GoalType;
import com.suchika.wealth.ports.output.GoalPlanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GoalPlanPanacheRepository implements GoalPlanRepository {

    private final GoalPlanDao goalPlanDao;
    private final GoalPlanMilestoneDao milestoneDao;
    private final GoalPlanRuleDao ruleDao;
    private final GoalPlanTriggerEventDao triggerEventDao;

    public GoalPlanPanacheRepository(GoalPlanDao goalPlanDao, GoalPlanMilestoneDao milestoneDao,
                                      GoalPlanRuleDao ruleDao, GoalPlanTriggerEventDao triggerEventDao) {
        this.goalPlanDao = goalPlanDao;
        this.milestoneDao = milestoneDao;
        this.ruleDao = ruleDao;
        this.triggerEventDao = triggerEventDao;
    }

    @Override
    public GoalPlan save(GoalPlan goalPlan) {
        GoalPlanEntity entity = GoalPlanEntity.from(goalPlan);
        if (entity.id == null) {
            goalPlanDao.persist(entity);
        } else {
            entity = goalPlanDao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<GoalPlan> findById(UUID id, UUID adminId) {
        return goalPlanDao.find("id = ?1 and adminId = ?2", id, adminId)
                .firstResultOptional()
                .map(GoalPlanEntity::toDomain);
    }

    @Override
    public List<GoalPlan> findAll(UUID adminId) {
        return goalPlanDao.find("adminId = ?1 order by createdAt desc", adminId)
                .stream().map(GoalPlanEntity::toDomain).toList();
    }

    @Override
    public boolean existsByAdminGoalTypeBeneficiary(UUID adminId, GoalType goalType, UUID beneficiaryProfileId) {
        if (beneficiaryProfileId != null) {
            return goalPlanDao.count("adminId = ?1 and goalType = ?2 and beneficiaryProfileId = ?3",
                    adminId, goalType.name(), beneficiaryProfileId) > 0;
        }
        return goalPlanDao.count("adminId = ?1 and goalType = ?2 and beneficiaryProfileId is null",
                adminId, goalType.name()) > 0;
    }

    @Override
    public List<GoalMilestone> findMilestones(UUID goalPlanId) {
        return milestoneDao.find("goalPlanId = ?1 order by sequenceNo", goalPlanId)
                .stream().map(GoalPlanMilestoneEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public List<GoalMilestone> replaceMilestones(UUID goalPlanId, List<GoalMilestone> milestones) {
        milestoneDao.delete("goalPlanId", goalPlanId);
        milestoneDao.getEntityManager().flush();
        for (GoalMilestone milestone : milestones) {
            milestoneDao.persist(GoalPlanMilestoneEntity.from(goalPlanId, milestone));
        }
        return findMilestones(goalPlanId);
    }

    @Override
    public List<GoalRule> findRules(UUID goalPlanId) {
        return ruleDao.find("goalPlanId = ?1 order by sequenceNo", goalPlanId)
                .stream().map(GoalPlanRuleEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public List<GoalRule> replaceRules(UUID goalPlanId, List<GoalRule> rules) {
        ruleDao.delete("goalPlanId", goalPlanId);
        ruleDao.getEntityManager().flush();
        for (GoalRule rule : rules) {
            ruleDao.persist(GoalPlanRuleEntity.from(goalPlanId, rule));
        }
        return findRules(goalPlanId);
    }

    @Override
    public List<GoalTriggerEvent> findTriggerEvents(UUID goalPlanId) {
        return triggerEventDao.find("goalPlanId = ?1 order by sequenceNo", goalPlanId)
                .stream().map(GoalPlanTriggerEventEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public List<GoalTriggerEvent> replaceTriggerEvents(UUID goalPlanId, List<GoalTriggerEvent> triggerEvents) {
        triggerEventDao.delete("goalPlanId", goalPlanId);
        triggerEventDao.getEntityManager().flush();
        for (GoalTriggerEvent event : triggerEvents) {
            triggerEventDao.persist(GoalPlanTriggerEventEntity.from(goalPlanId, event));
        }
        return findTriggerEvents(goalPlanId);
    }

    @Override
    public Optional<GoalMilestone> findMilestoneById(UUID goalPlanId, UUID milestoneId) {
        return milestoneDao.find("id = ?1 and goalPlanId = ?2", milestoneId, goalPlanId)
                .firstResultOptional()
                .map(GoalPlanMilestoneEntity::toDomain);
    }

    @Override
    @Transactional
    public GoalMilestone saveMilestone(UUID goalPlanId, GoalMilestone milestone) {
        GoalPlanMilestoneEntity entity = milestoneDao.getEntityManager()
                .merge(GoalPlanMilestoneEntity.from(goalPlanId, milestone));
        return entity.toDomain();
    }
}
