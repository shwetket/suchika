package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.GoalRule;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "goal_plan_rule", schema = "wealth")
public class GoalPlanRuleEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "goal_plan_id", nullable = false, columnDefinition = "uuid")
    public UUID goalPlanId;

    @Column(name = "sequence_no", nullable = false)
    public int sequenceNo;

    @Column(name = "rule_name", nullable = false, length = 50)
    public String ruleName;

    @Column(name = "rule_text", nullable = false)
    public String ruleText;

    public static GoalPlanRuleEntity from(UUID goalPlanId, GoalRule rule) {
        GoalPlanRuleEntity e = new GoalPlanRuleEntity();
        e.id = rule.getId();
        e.goalPlanId = goalPlanId;
        e.sequenceNo = rule.getSequenceNo();
        e.ruleName = rule.getRuleName();
        e.ruleText = rule.getRuleText();
        return e;
    }

    public GoalRule toDomain() {
        return GoalRule.create(id, sequenceNo, ruleName, ruleText);
    }
}
