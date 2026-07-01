package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account", schema = "wealth")
public class AccountEntity extends PanacheEntityBase {


    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "account_name", nullable = false, length = 100)
    public String accountName;

    @Column(name = "account_type", nullable = false, length = 50)
    public String accountType;

    @Column(name = "institution_name", nullable = false, length = 100)
    public String institutionName;

    @Column(name = "currency", nullable = false, length = 10)
    public String currency = "INR";

    @Column(name = "opening_balance", precision = 19, scale = 4)
    public BigDecimal openingBalance;

    @Column(name = "credit_limit", precision = 19, scale = 4)
    public BigDecimal creditLimit;

    @Column(name = "interest_rate", precision = 7, scale = 4)
    public BigDecimal interestRate;

    @Column(name = "emi_amount", precision = 19, scale = 4)
    public BigDecimal emiAmount;

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    public String metadata = "{}";

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static AccountEntity from(Account account) {
        AccountEntity e = new AccountEntity();
        e.id = account.getId();
        e.profileId = account.getProfileId();
        e.accountName = account.getAccountName();
        e.accountType = account.getAccountType() != null ? account.getAccountType().name() : null;
        e.institutionName = account.getInstitutionName();
        e.currency = account.getCurrency();
        e.openingBalance = account.getOpeningBalance();
        e.creditLimit = account.getCreditLimit();
        e.interestRate = account.getInterestRate();
        e.emiAmount = account.getEmiAmount();
        e.active = account.isActive();
        e.createdAt = account.getCreatedAt();
        e.metadata = JsonbMetadataUtil.write(account.getMetadata());
        return e;
    }

    public Account toDomain() {
        return Account.builder()
                .id(id)
                .profileId(profileId)
                .accountName(accountName)
                .accountType(AccountType.valueOf(accountType))
                .institutionName(institutionName)
                .currency(currency)
                .openingBalance(openingBalance)
                .creditLimit(creditLimit)
                .interestRate(interestRate)
                .emiAmount(emiAmount)
                .active(active)
                .createdAt(createdAt)
                .metadata(JsonbMetadataUtil.read(metadata))
                .build();
    }

}
