package com.suchika.wealth.domain;

/**
 * ADR-022 Phase 2 — how often {@code insurance_policy.premium_amount} is paid.
 * Plain VARCHAR at the DB layer (ADR-010, no SQL enum); the gateway's
 * THIRTY_SEVENTY_TARGET premium term normalizes both frequencies to a monthly
 * figure (ANNUAL / 12, MONTHLY pass-through).
 */
public enum PremiumFrequency {
    MONTHLY,
    ANNUAL
}
