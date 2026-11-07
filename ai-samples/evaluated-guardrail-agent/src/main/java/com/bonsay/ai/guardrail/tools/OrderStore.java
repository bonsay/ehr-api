package com.bonsay.ai.guardrail.tools;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Fake order system. Note paymentMethod contains a full card number - a realistic data hazard. */
public class OrderStore {

    public record Order(String id, String customerId, String email, String status, BigDecimal total, String paymentMethod) {}

    private final Map<String, Order> orders = Map.of(
            "ORD-1001", new Order("ORD-1001", "C-1", "jane@example.com", "shipped", new BigDecimal("59.99"), "Visa 4111 1111 1111 1111"),
            "ORD-1002", new Order("ORD-1002", "C-2", "omar@example.com", "delivered", new BigDecimal("250.00"), "Mastercard 5555 5555 5555 4444"),
            "ORD-1003", new Order("ORD-1003", "C-1", "jane@example.com", "delivered", new BigDecimal("180.00"), "Visa 4111 1111 1111 1111"),
            "ORD-1004", new Order("ORD-1004", "C-1", "jane@example.com", "processing", new BigDecimal("35.00"), "PayPal"));

    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }
}
