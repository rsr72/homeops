package com.homeops.backend.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleResponse(
        String make,
        String model,
        Integer year,
        String vin,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        Integer currentMileage
) {
    static VehicleResponse from(VehicleDetails vehicleDetails) {
        return new VehicleResponse(
                vehicleDetails.make(),
                vehicleDetails.model(),
                vehicleDetails.year(),
                vehicleDetails.vin(),
                vehicleDetails.purchaseDate(),
                vehicleDetails.purchaseCost(),
                vehicleDetails.currentMileage()
        );
    }
}