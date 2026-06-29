package com.ehrapi.exception;

/**
 * Thrown when an institution tries to use or enable a paid module it has no
 * active entitlement for. Surfaced to clients as HTTP 402 Payment Required.
 */
public class ModuleNotEntitledException extends RuntimeException {
    public ModuleNotEntitledException(String message) {
        super(message);
    }
}
