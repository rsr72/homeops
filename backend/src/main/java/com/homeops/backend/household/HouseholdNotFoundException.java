package com.homeops.backend.household;

import java.util.UUID;

public class HouseholdNotFoundException extends RuntimeException {

    public HouseholdNotFoundException(UUID householdId) {
        super("Household with id '%s' was not found".formatted(householdId));
    }
}