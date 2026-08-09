package com.homeops.backend.vehicle;

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
        @Size(max = 200, message = "make must be 200 characters or fewer")
        String make,

        @NotBlank(message = "model is required")
        @Size(max = 200, message = "model must be 200 characters or fewer")
        String model,

        @NotNull(message = "year is required")
        @Min(value = 1886, message = "year must be 1886 or later")
        @Max(value = 2100, message = "year must be 2100 or earlier")
        Integer year,

        @Size(max = 17, message = "vin must be 17 characters or fewer")
        String vin,

        @Size(max = 2_000, message = "notes must be 2000 characters or fewer")
        String notes,

        LocalDate purchaseDate,

        @PositiveOrZero(message = "purchaseCost must be zero or greater")
        BigDecimal purchaseCost,

        @PositiveOrZero(message = "currentMileage must be zero or greater")
        Integer currentMileage
) {
}