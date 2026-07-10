package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void builder_allFields_gettersReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-01T10:00:00Z");
        Map<String, String> metadata = Map.of("category", "SAVINGS_GOAL");

        Account account = Account.builder()
                .id(id)
                .profileId(profileId)
                .accountName("HDFC Savings")
                .accountType(AccountType.SAVINGS)
                .institutionName("HDFC Bank")
                .currency("INR")
                .openingBalance(new BigDecimal("1000.00"))
                .creditLimit(new BigDecimal("50000.00"))
                .interestRate(new BigDecimal("3.5"))
                .emiAmount(new BigDecimal("0"))
                .active(true)
                .createdAt(createdAt)
                .metadata(metadata)
                .build();

        assertEquals(id, account.getId());
        assertEquals(profileId, account.getProfileId());
        assertEquals("HDFC Savings", account.getAccountName());
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals("HDFC Bank", account.getInstitutionName());
        assertEquals("INR", account.getCurrency());
        assertEquals(0, new BigDecimal("1000.00").compareTo(account.getOpeningBalance()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(account.getCreditLimit()));
        assertEquals(0, new BigDecimal("3.5").compareTo(account.getInterestRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getEmiAmount()));
        assertTrue(account.isActive());
        assertEquals(createdAt, account.getCreatedAt());
        assertEquals(metadata, account.getMetadata());
    }

    @Test
    void builder_defaults_currencyIsInrActiveIsTrueMetadataIsEmptyMap() {
        Account account = Account.builder()
                .accountName("Cash Wallet")
                .accountType(AccountType.CURRENT)
                .institutionName("Self")
                .build();

        assertEquals("INR", account.getCurrency());
        assertTrue(account.isActive());
        assertNotNull(account.getMetadata());
        assertTrue(account.getMetadata().isEmpty());
        assertNull(account.getId());
        assertNull(account.getProfileId());
        assertNull(account.getOpeningBalance());
        assertNull(account.getCreditLimit());
        assertNull(account.getInterestRate());
        assertNull(account.getEmiAmount());
        assertNull(account.getCreatedAt());
    }

    @Test
    void noArgConstructor_producesEmptyAccount() {
        Account account = new Account();

        assertNull(account.getId());
        assertNull(account.getAccountName());
        assertFalse(account.isActive());
    }

    @Test
    void setters_mutateCorrespondingFields() {
        Account account = Account.builder()
                .accountName("Original Name")
                .accountType(AccountType.SAVINGS)
                .institutionName("Bank")
                .build();

        Map<String, String> newMetadata = new HashMap<>();
        newMetadata.put("liquidity_tier", "LIQUID");

        account.setAccountName("Updated Name");
        account.setOpeningBalance(new BigDecimal("200.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setInterestRate(new BigDecimal("4.25"));
        account.setEmiAmount(new BigDecimal("100.00"));
        account.setActive(false);
        account.setMetadata(newMetadata);

        assertEquals("Updated Name", account.getAccountName());
        assertEquals(0, new BigDecimal("200.00").compareTo(account.getOpeningBalance()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(account.getCreditLimit()));
        assertEquals(0, new BigDecimal("4.25").compareTo(account.getInterestRate()));
        assertEquals(0, new BigDecimal("100.00").compareTo(account.getEmiAmount()));
        assertFalse(account.isActive());
        assertEquals(newMetadata, account.getMetadata());
    }
}
