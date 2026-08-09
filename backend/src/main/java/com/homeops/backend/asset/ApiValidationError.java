package com.homeops.backend.asset;

public record ApiValidationError(
        String field,
        String message
) {
}