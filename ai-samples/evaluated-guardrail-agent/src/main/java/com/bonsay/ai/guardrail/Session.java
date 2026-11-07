package com.bonsay.ai.guardrail;

import java.util.HashSet;
import java.util.Set;

/**
 * Authenticated conversation context. The customer id comes from the auth layer - NEVER from
 * model output - so the model cannot act on behalf of another customer.
 */
public final class Session {
    private final String customerId;
    private final String customerEmail;
    private final Set<String> refundedOrders = new HashSet<>();

    public Session(String customerId, String customerEmail) {
        this.customerId = customerId;
        this.customerEmail = customerEmail;
    }

    public String customerId() { return customerId; }

    public String customerEmail() { return customerEmail; }

    public Set<String> refundedOrders() { return refundedOrders; }
}
