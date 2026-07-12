package com.suchika.wealth.domain;

/**
 * ADR-022 Phase 2 — closed set of insurance policy shapes. Fixed enum, soft-validated
 * at the contract/domain layer only; {@code wealth.insurance_policy.policy_type} is a
 * plain VARCHAR (ADR-010, no SQL enum).
 */
public enum PolicyType {
    TERM,
    GROUP_TERM,
    INVESTMENT_LINKED,
    ENDOWMENT,
    HEALTH
}
