package com.homeops.backend.asset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAssetNotFound(AssetNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidAssetRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAssetRequest(InvalidAssetRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(com.homeops.backend.household.HouseholdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleHouseholdNotFound(com.homeops.backend.household.HouseholdNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(com.homeops.backend.household.InvalidHouseholdRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidHouseholdRequest(com.homeops.backend.household.InvalidHouseholdRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiValidationError> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(fieldError -> fieldError.getField().toLowerCase()))
                .map(fieldError -> new ApiValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        ApiErrorResponse response = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                validationErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Request body could not be parsed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ApiErrorResponse response = ApiErrorResponse.of(Instant.now(), status.value(), error, message);
        return ResponseEntity.status(status).body(response);
    }
}