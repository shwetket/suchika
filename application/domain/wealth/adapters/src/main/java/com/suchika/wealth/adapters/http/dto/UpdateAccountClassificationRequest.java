package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Epic 8 Phase 1 classification metadata write request. All fields optional —
 * only the provided keys are merged into the account's existing metadata map.
 * category is reserved for Phase 2 consumption (validation/rollup engines) but
 * accepted and stored now so no later contract/migration change is needed.
 */
@RegisterForReflection
public class UpdateAccountClassificationRequest {

    @JsonProperty("category")
    public String category;

    @JsonProperty("liquidity_tier")
    public String liquidityTier;

    @JsonProperty("purpose_tag")
    public String purposeTag;
}
