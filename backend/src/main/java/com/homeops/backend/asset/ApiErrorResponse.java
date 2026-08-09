package com.homeops.backend.asset;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<ApiValidationError> validationErrors
) {
    public static ApiErrorResponse of(Instant timestamp, int status, String error, String message) {
        return new ApiErrorResponse(timestamp, status, error, message, List.of());
    }

    public static ApiErrorResponse of(
            Instant timestamp,
            int status,
            String error,
            String message,
            List<ApiValidationError> validationErrors
    ) {
        return new ApiErrorResponse(timestamp, status, error, message, validationErrors);
    }
}