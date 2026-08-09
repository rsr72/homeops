package com.homeops.backend.asset;

import java.time.Instant;

public record AssetResponse(
        String id,
        AssetType assetType,
        String notes,
        VehicleResponse vehicle,
        Instant createdAt,
        Instant updatedAt
) {
    static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.id(),
                asset.assetType(),
                asset.notes(),
                asset.vehicle() == null ? null : VehicleResponse.from(asset.vehicle()),
                asset.createdAt(),
                asset.updatedAt()
        );
    }
}