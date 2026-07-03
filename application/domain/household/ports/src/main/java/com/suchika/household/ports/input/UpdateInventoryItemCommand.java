package com.suchika.household.ports.input;

import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInventoryItemCommand(
        String itemName,
        BigDecimal quantity,
        ItemUnit unit,
        SourcePlatform sourcePlatform,
        LocalDate purchaseDate,
        String category,
        Boolean isConsumed
) {}
