package com.homeops.backend.household;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HouseholdRequest(
        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be 200 characters or fewer")
        String name,

        @Size(max = 2_000, message = "notes must be 2000 characters or fewer")
        String notes
) {
}