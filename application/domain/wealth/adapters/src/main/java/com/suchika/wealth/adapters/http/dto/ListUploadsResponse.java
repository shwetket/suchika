package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ListUploadsResponse {

    @JsonProperty("uploads")
    public List<StatementUploadResponse> uploads;

    public ListUploadsResponse(List<StatementUploadResponse> uploads) {
        this.uploads = uploads;
    }
}
