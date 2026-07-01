package com.suchika.wealth.adapters.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.shared.logging.AppLogger;

import java.util.HashMap;
import java.util.Map;

final class JsonbMetadataUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private JsonbMetadataUtil() {}

    static String write(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(metadata);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            AppLogger.warn("Failed to serialize JSONB metadata, defaulting to empty object: %s", e.getMessage());
            return "{}";
        }
    }

    static Map<String, String> read(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(json, MAP_TYPE);
        } catch (java.io.IOException e) {
            AppLogger.warn("Failed to deserialize JSONB metadata, defaulting to empty map: %s", e.getMessage());
            return new HashMap<>();
        }
    }
}
