package com.bonsay.ai.gateway;

/** Maps to an HTTP status in the API layer. */
public class GatewayException extends RuntimeException {
    private final int status;

    public GatewayException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() { return status; }
}
