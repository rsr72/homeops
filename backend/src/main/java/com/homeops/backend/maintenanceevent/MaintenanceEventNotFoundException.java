package com.homeops.backend.maintenanceevent;

import java.util.UUID;

public class MaintenanceEventNotFoundException extends RuntimeException {

    public MaintenanceEventNotFoundException(UUID eventId) {
        super("Maintenance event with id '%s' was not found".formatted(eventId));
    }
}
