package com.homeops.backend.asset;

public class InvalidAssetRequestException extends RuntimeException {

    public InvalidAssetRequestException(String message) {
        super(message);
    }
}