package com.homeops.backend.maintenanceevent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceEventRequest(
        @NotNull(message = "serviceDate is required")
        LocalDate serviceDate,

        @NotBlank(message = "description is required")
        @Size(max = 500, message = "description must be 500 characters or fewer")
        String description,

        @PositiveOrZero(message = "mileage must be zero or greater")
        Integer mileage,

        @PositiveOrZero(message = "cost must be zero or greater")
        BigDecimal cost,

        @Size(max = 2_000, message = "notes must be 2000 characters or fewer")
        String notes
) {
}
