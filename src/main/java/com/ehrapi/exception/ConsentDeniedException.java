package com.ehrapi.exception;

/**
 * Thrown when an institution requests patient data it has not been granted
 * consent to access. Maps to HTTP 403.
 */
public class ConsentDeniedException extends RuntimeException {
    public ConsentDeniedException(String message) {
        super(message);
    }
}
