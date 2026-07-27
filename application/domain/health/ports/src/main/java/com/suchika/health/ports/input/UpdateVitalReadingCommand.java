package com.suchika.health.ports.input;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateVitalReadingCommand(
        LocalDate readingDate,
        BigDecimal valuePrimary,
        BigDecimal valueSecondary,
        String unit,
        String notes
) {}
