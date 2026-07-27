package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListAccountsResponse {

    @JsonProperty("accounts")
    public List<AccountResponse> accounts;

    @JsonProperty("total_size")
    public int totalSize;

    public ListAccountsResponse(List<AccountResponse> accounts) {
        this.accounts = accounts;
        this.totalSize = accounts.size();
    }
}
