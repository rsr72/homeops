package com.homeops.backend.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
        @NotNull(message = "assetType is required")
        AssetType assetType,

        @Size(max = 2_000, message = "notes must be 2000 characters or fewer")
        String notes,

        @Valid
        VehicleRequest vehicle
) {
}