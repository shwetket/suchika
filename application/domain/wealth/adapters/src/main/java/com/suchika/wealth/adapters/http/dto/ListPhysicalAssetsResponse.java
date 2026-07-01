package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListPhysicalAssetsResponse {

    @JsonProperty("physical_assets")
    public List<PhysicalAssetResponse> physicalAssets;

    @JsonProperty("total_size")
    public int totalSize;

    public ListPhysicalAssetsResponse(List<PhysicalAssetResponse> physicalAssets) {
        this.physicalAssets = physicalAssets;
        this.totalSize = physicalAssets.size();
    }
}
