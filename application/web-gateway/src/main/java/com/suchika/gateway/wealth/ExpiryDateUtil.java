package com.suchika.gateway.wealth;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Null-safe parsing of physical_asset.metadata compliance dates (Q29 — plain
 * JSONB strings, no schema promotion). Shared by VacationPlannerService and
 * ProjectionCalculationEngine's Action Center step so the "unparseable date is
 * skipped, not fatal" rule lives in exactly one place.
 */
public final class ExpiryDateUtil {

    private ExpiryDateUtil() {
    }

    /**
     * Reads {@code metadata.<key>} as a date; returns null if the field is
     * absent, blank, or not a valid ISO date rather than throwing.
     */
    public static LocalDate parse(JsonNode metadata, String key) {
        String text = metadata.path(key).asText("");
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
