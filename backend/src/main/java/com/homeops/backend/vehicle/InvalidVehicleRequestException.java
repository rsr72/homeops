package com.homeops.backend.vehicle;

public class InvalidVehicleRequestException extends RuntimeException {

    public InvalidVehicleRequestException(String message) {
        super(message);
    }
}