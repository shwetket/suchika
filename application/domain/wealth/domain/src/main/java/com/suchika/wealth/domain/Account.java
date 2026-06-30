package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Account {

    private UUID id;
    private UUID profileId;
    private String accountName;
    private AccountType accountType;
    private String institutionName;
    private String currency;
    private BigDecimal openingBalance;
    private BigDecimal creditLimit;
    private BigDecimal interestRate;
    private BigDecimal emiAmount;
    private boolean active;
    private Instant createdAt;
    private Map<String, String> metadata;

    public Account() {}

    private Account(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.accountName = builder.accountName;
        this.accountType = builder.accountType;
        this.institutionName = builder.institutionName;
        this.currency = builder.currency;
        this.openingBalance = builder.openingBalance;
        this.creditLimit = builder.creditLimit;
        this.interestRate = builder.interestRate;
        this.emiAmount = builder.emiAmount;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.metadata = builder.metadata;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private String accountName;
        private AccountType accountType;
        private String institutionName;
        private String currency = "INR";
        private BigDecimal openingBalance;
        private BigDecimal creditLimit;
        private BigDecimal interestRate;
        private BigDecimal emiAmount;
        private boolean active = true;
        private Instant createdAt;
        private Map<String, String> metadata = new HashMap<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder accountName(String accountName) { this.accountName = accountName; return this; }
        public Builder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public Builder institutionName(String institutionName) { this.institutionName = institutionName; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder openingBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; return this; }
        public Builder creditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; return this; }
        public Builder interestRate(BigDecimal interestRate) { this.interestRate = interestRate; return this; }
        public Builder emiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Account build() { return new Account(this); }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public String getAccountName() { return accountName; }
    public AccountType getAccountType() { return accountType; }
    public String getInstitutionName() { return institutionName; }
    public String getCurrency() { return currency; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getInterestRate() { return interestRate; }
    public BigDecimal getEmiAmount() { return emiAmount; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, String> getMetadata() { return metadata; }

    public void setAccountName(String accountName) { this.accountName = accountName; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }
    public void setActive(boolean active) { this.active = active; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
