package com.homeops.backend.household;

public class InvalidHouseholdRequestException extends RuntimeException {

    public InvalidHouseholdRequestException(String message) {
        super(message);
    }
}