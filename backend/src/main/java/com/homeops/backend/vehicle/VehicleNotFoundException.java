package com.homeops.backend.vehicle;

import java.util.UUID;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(UUID vehicleId) {
        super("Vehicle with id '%s' was not found".formatted(vehicleId));
    }
}