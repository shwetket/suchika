package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListPhysicalAssetsResponse {

    @JsonProperty("physical_assets")
    public List<PhysicalAssetResponse> physicalAssets;

    @JsonProperty("total_size")
    public long totalSize;

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListPhysicalAssetsResponse(List<PhysicalAssetResponse> physicalAssets) {
        this.physicalAssets = physicalAssets;
        this.totalSize = physicalAssets.size();
    }

    /**
     * Paginated variant (v1.0 pagination extension, Q54) — totalSize is the grand
     * total matching the filter across all pages, not just physicalAssets.size().
     */
    public ListPhysicalAssetsResponse(List<PhysicalAssetResponse> physicalAssets, long totalSize, int page, int size) {
        this.physicalAssets = physicalAssets;
        this.totalSize = totalSize;
        this.page = page;
        this.size = size;
    }
}
