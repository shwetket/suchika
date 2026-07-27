package com.suchika.wealth.ports.input;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial-update payload for {@link AccountUseCase#updateAccount}. Bundles the
 * update fields into a single command (Sonar S107 — too many parameters),
 * mirroring the {@link UpdateAccountClassificationCommand} pattern. Non-null
 * fields replace the existing value; null fields are left unchanged. id/profileId
 * stay as separate leading parameters on the use case method — this command
 * carries only the update payload, never identity/scope.
 */
public record UpdateAccountCommand(
        String accountName,
        BigDecimal openingBalance,
        BigDecimal creditLimit,
        BigDecimal interestRate,
        BigDecimal emiAmount,
        Boolean isActive,
        LocalDate balanceAsOf
) {}
