package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.RegistrationType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePhysicalAssetCommand(
        String assetName,
        AssetType assetType,
        String make,
        String model,
        String registrationNumber,
        RegistrationType registrationType,
        BigDecimal currentValue,
        LocalDate valuationDate
) {}
