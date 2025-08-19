package io.github.bluething.congestion.calculator.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler {
    @ExceptionHandler(InvalidVehicleTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVehicleType(InvalidVehicleTypeException e) {
        log.warn("Invalid vehicle type: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.of(
                "INVALID_VEHICLE_TYPE",
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidDateFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDateFormat(InvalidDateFormatException e) {
        log.warn("Invalid date format: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.of(
                "INVALID_DATE_FORMAT",
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }
    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParseException(java.time.format.DateTimeParseException e) {
        log.warn("Date time parse error: {}", e.getMessage());

        String userFriendlyMessage = "Invalid date format. Use ISO format: 2013-02-07T06:23:27";

        ErrorResponse error = ErrorResponse.of(
                "INVALID_DATE_FORMAT",
                userFriendlyMessage,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());

        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        String errorMessage = "Validation failed: " + fieldErrors.entrySet()
                .stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.withDetails(
                "VALIDATION_ERROR",
                errorMessage,
                HttpStatus.BAD_REQUEST.value(),
                new HashMap<>(fieldErrors)
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed JSON request: {}", e.getMessage());

        String message = "Invalid JSON format";
        if (e.getMessage() != null && e.getMessage().contains("LocalDateTime")) {
            message = "Invalid date format in JSON. Use ISO format: 2013-02-07T06:23:27";
        }

        ErrorResponse error = ErrorResponse.of(
                "MALFORMED_REQUEST",
                message,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getParameterName());

        ErrorResponse error = ErrorResponse.of(
                "MISSING_PARAMETER",
                "Required parameter '" + e.getParameterName() + "' is missing",
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch for parameter: {}", e.getName());

        ErrorResponse error = ErrorResponse.of(
                "TYPE_MISMATCH",
                "Invalid value for parameter '" + e.getName() + "'. Expected type: " +
                        (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown"),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.of(
                "ILLEGAL_ARGUMENT",
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);

        ErrorResponse error = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Error response record with factory methods
     */
    public record ErrorResponse(
            String errorCode,
            String message,
            LocalDateTime timestamp,
            int httpStatus,
            Map<String, Object> details
    ) {
        // Factory method for simple errors (no details)
        public static ErrorResponse of(String errorCode, String message, int httpStatus) {
            return new ErrorResponse(
                    errorCode,
                    message,
                    LocalDateTime.now(),
                    httpStatus,
                    Map.of() // Empty immutable map
            );
        }

        // Factory method for errors with details
        public static ErrorResponse withDetails(String errorCode, String message, int httpStatus,
                                                Map<String, Object> details) {
            return new ErrorResponse(
                    errorCode,
                    message,
                    LocalDateTime.now(),
                    httpStatus,
                    details != null ? Map.copyOf(details) : Map.of() // Defensive copy
            );
        }

        // Compact constructor for validation
        public ErrorResponse {
            if (errorCode == null || errorCode.isBlank()) {
                throw new IllegalArgumentException("Error code cannot be null or blank");
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Message cannot be null or blank");
            }
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }
            if (details == null) {
                details = Map.of();
            }
        }
    }
}
