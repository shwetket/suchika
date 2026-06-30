package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Epic 8 classification metadata write request. All fields optional — only the
 * provided keys are merged into the account's existing metadata map.
 * category is reserved for Phase 2 consumption (validation/rollup engines) but
 * accepted and stored now so no later contract/migration change is needed.
 * joint_owners (Phase 2, ADR-016 Decision 2) is a list of co-owner profile_id
 * strings, attribution-only — never a query predicate.
 */
@RegisterForReflection
public class UpdateAccountClassificationRequest {

    @JsonProperty("category")
    public String category;

    @JsonProperty("liquidity_tier")
    public String liquidityTier;

    @JsonProperty("purpose_tag")
    public String purposeTag;

    @JsonProperty("joint_owners")
    public List<String> jointOwners;
}
