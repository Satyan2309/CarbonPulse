package com.ecotrack.exception;

import com.ecotrack.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler.
 *
 * <p>Phase 6: Fixed overly broad exception handling.</p>
 * <ul>
 *   <li>Business logic errors (bad input, missing entities) → 400/404 with descriptive message.</li>
 *   <li>Internal/unexpected errors → 500 with a generic message (no internal detail leaked).</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty() || message.equalsIgnoreCase("User not found")) {
            message = "USER_NOT_FOUND";
        }
        return new ResponseEntity<>(new ApiResponse<>(false, message, null), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GoogleAuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleGoogleAuthenticationException(GoogleAuthenticationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(new ApiResponse<>(false, "Validation failed", errors), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles business-logic illegal arguments (e.g. wrong current password, bad input format).
     * Returns 400 Bad Request with the exception message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles unexpected internal state errors.
     * Logs the full stacktrace server-side, returns a generic 500 to the client.
     * No internal details are exposed to the caller.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        log.error("Internal state error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Please try again.");
    }

    /**
     * Catch-all for any other unhandled exception.
     * Logs the full stacktrace server-side, returns a generic 500 to the client.
     * No internal details (class names, stack traces) are exposed to the caller.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(HttpStatus status, String message) {
        return new ResponseEntity<>(new ApiResponse<>(false, message, null), status);
    }
}
