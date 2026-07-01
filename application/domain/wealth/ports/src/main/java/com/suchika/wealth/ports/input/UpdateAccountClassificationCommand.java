package com.suchika.wealth.ports.input;

import java.util.List;

public record UpdateAccountClassificationCommand(
        String category,
        String liquidityTier,
        String purposeTag,
        List<String> jointOwners,
        String loanOriginalPrincipal,
        String loanStartDate,
        String loanTenureMonths,
        String linkedOffsetAccountId
) {}
