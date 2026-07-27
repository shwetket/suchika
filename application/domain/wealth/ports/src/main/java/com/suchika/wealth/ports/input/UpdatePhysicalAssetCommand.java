package com.suchika.wealth.ports.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Partial-update payload for {@link PhysicalAssetUseCase#updateAsset}. Bundles the
 * update fields into a single command (Sonar S107 — too many parameters),
 * mirroring the {@link UpdateAccountClassificationCommand} pattern. Non-null scalar
 * fields replace the existing value; metadata, if non-null, is merged into the
 * existing metadata map rather than replacing it wholesale. id/profileId stay as
 * separate leading parameters on the use case method — this command carries only
 * the update payload, never identity/scope.
 */
public record UpdatePhysicalAssetCommand(
        String assetName,
        String make,
        String model,
        Map<String, String> metadata,
        Boolean isActive,
        BigDecimal currentValue,
        LocalDate valuationDate
) {}
