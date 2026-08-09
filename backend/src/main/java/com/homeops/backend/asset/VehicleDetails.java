package com.homeops.backend.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleDetails(
        String make,
        String model,
        Integer year,
        String vin,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        Integer currentMileage
) {
}