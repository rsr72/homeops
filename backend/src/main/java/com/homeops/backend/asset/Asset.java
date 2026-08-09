package com.homeops.backend.asset;

import java.time.Instant;

public record Asset(
        String id,
        AssetType assetType,
        String notes,
        VehicleDetails vehicle,
        Instant createdAt,
        Instant updatedAt
) {
}