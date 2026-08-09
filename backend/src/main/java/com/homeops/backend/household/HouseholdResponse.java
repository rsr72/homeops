package com.homeops.backend.household;

import java.time.Instant;
import java.util.UUID;

public record HouseholdResponse(
        UUID id,
        String name,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    static HouseholdResponse from(Household household) {
        return new HouseholdResponse(
                household.getId(),
                household.getName(),
                household.getNotes(),
                household.getCreatedAt(),
                household.getUpdatedAt()
        );
    }
}