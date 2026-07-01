package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.RegistrationType;

public record CreatePhysicalAssetCommand(
        String assetName,
        AssetType assetType,
        String make,
        String model,
        String registrationNumber,
        RegistrationType registrationType
) {}
