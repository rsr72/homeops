package com.homeops.backend.maintenanceevent;

public class InvalidMaintenanceEventRequestException extends RuntimeException {

    public InvalidMaintenanceEventRequestException(String message) {
        super(message);
    }
}
