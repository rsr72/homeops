package com.homeops.backend.vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID householdId,
        String make,
        String model,
        Integer year,
        String vin,
        String notes,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        Integer currentMileage,
        Instant createdAt,
        Instant updatedAt
) {
    static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getHousehold().getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getVin(),
                vehicle.getNotes(),
                vehicle.getPurchaseDate(),
                vehicle.getPurchaseCost(),
                vehicle.getCurrentMileage(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}