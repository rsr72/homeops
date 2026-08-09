package com.homeops.backend.asset;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final InMemoryAssetRepository assetRepository;
    private final Clock clock = Clock.systemUTC();

    public AssetService(InMemoryAssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Asset createAsset(CreateAssetRequest request) {
        VehicleDetails vehicleDetails = toVehicleDetails(request.assetType(), request.vehicle());
        Instant now = Instant.now(clock);

        Asset asset = new Asset(
                UUID.randomUUID().toString(),
                request.assetType(),
                normalizeOptionalText(request.notes()),
                vehicleDetails,
                now,
                now
        );

        return assetRepository.save(asset);
    }

    public List<Asset> getAssets() {
        return assetRepository.findAll();
    }

    public Asset getAsset(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    public Asset updateAsset(String assetId, UpdateAssetRequest request) {
        Asset existingAsset = getAsset(assetId);
        VehicleDetails vehicleDetails = toVehicleDetails(request.assetType(), request.vehicle());

        Asset updatedAsset = new Asset(
                existingAsset.id(),
                request.assetType(),
                normalizeOptionalText(request.notes()),
                vehicleDetails,
                existingAsset.createdAt(),
                Instant.now(clock)
        );

        return assetRepository.save(updatedAsset);
    }

    public void deleteAsset(String assetId) {
        getAsset(assetId);
        assetRepository.deleteById(assetId);
    }

    private VehicleDetails toVehicleDetails(AssetType assetType, VehicleRequest vehicleRequest) {
        if (assetType != AssetType.VEHICLE) {
            throw new InvalidAssetRequestException("Unsupported asset type: %s".formatted(assetType));
        }

        if (vehicleRequest == null) {
            throw new InvalidAssetRequestException("vehicle details are required for VEHICLE assets");
        }

        return new VehicleDetails(
                vehicleRequest.make().trim(),
                vehicleRequest.model().trim(),
                vehicleRequest.year(),
                normalizeOptionalText(vehicleRequest.vin()),
                vehicleRequest.purchaseDate(),
                vehicleRequest.purchaseCost(),
                vehicleRequest.currentMileage()
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}