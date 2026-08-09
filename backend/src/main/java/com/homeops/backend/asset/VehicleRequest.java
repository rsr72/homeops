package com.homeops.backend.asset;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleRequest(
        @NotBlank(message = "make is required")
        String make,

        @NotBlank(message = "model is required")
        String model,

        @NotNull(message = "year is required")
        @Min(value = 1886, message = "year must be 1886 or later")
        @Max(value = 2100, message = "year must be 2100 or earlier")
        Integer year,

        @Size(max = 17, message = "vin must be 17 characters or fewer")
        String vin,

        LocalDate purchaseDate,

        @PositiveOrZero(message = "purchaseCost must be zero or greater")
        BigDecimal purchaseCost,

        @PositiveOrZero(message = "currentMileage must be zero or greater")
        Integer currentMileage
) {
}