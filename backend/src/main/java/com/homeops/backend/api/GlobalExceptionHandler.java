package com.homeops.backend.api;

import com.homeops.backend.asset.ApiErrorResponse;
import com.homeops.backend.asset.ApiValidationError;
import com.homeops.backend.household.HouseholdNotFoundException;
import com.homeops.backend.household.InvalidHouseholdRequestException;
import com.homeops.backend.vehicle.InvalidVehicleRequestException;
import com.homeops.backend.vehicle.VehicleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleNotFound(VehicleNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidVehicleRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidVehicleRequest(InvalidVehicleRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String fieldName = exception.getName();
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Path variable '%s' is invalid".formatted(fieldName));
    }

    @ExceptionHandler(HouseholdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleHouseholdNotFound(HouseholdNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidHouseholdRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidHouseholdRequest(InvalidHouseholdRequestException exception) {
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