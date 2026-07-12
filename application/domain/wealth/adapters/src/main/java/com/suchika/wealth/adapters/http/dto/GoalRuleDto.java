package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.GoalRule;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalRuleDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("sequence_no")
    public int sequenceNo;

    @JsonProperty("rule_name")
    public String ruleName;

    @JsonProperty("rule_text")
    public String ruleText;

    public static GoalRuleDto from(GoalRule rule) {
        GoalRuleDto dto = new GoalRuleDto();
        dto.id = rule.getId();
        dto.sequenceNo = rule.getSequenceNo();
        dto.ruleName = rule.getRuleName();
        dto.ruleText = rule.getRuleText();
        return dto;
    }

    public GoalRule toDomain() {
        return GoalRule.create(id, sequenceNo, ruleName, ruleText);
    }
}
