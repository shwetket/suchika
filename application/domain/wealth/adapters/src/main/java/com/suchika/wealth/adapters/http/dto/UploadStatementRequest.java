package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadStatementRequest {

    @JsonProperty("file_name")
    public String fileName;

    @JsonProperty("csv_content")
    public String csvContent;
}
