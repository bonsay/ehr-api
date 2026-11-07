package com.bonsay.ai.guardrail.policy;

import com.bonsay.ai.guardrail.Session;
import com.bonsay.ai.guardrail.tools.OrderStore;
import com.bonsay.ai.guardrail.tools.SupportTools;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * Authorises every tool call the model proposes: allow-list, argument validation, ownership
 * (customer can only touch their own orders), business limits, and human-in-the-loop approval
 * for high-value actions.
 */
public class ToolPolicy {

    public enum Decision { ALLOW, DENY, REQUIRE_APPROVAL }

    public record Verdict(Decision decision, String reason) {
        static Verdict allow() { return new Verdict(Decision.ALLOW, null); }
        static Verdict deny(String r) { return new Verdict(Decision.DENY, r); }
    }

    private static final Set<String> REFUNDABLE = Set.of("shipped", "delivered");
    private final OrderStore store;
    private final BigDecimal autoApproveLimit;

    public ToolPolicy(OrderStore store, BigDecimal autoApproveLimit) {
        this.store = store;
        this.autoApproveLimit = autoApproveLimit;
    }

    public Verdict authorize(Session session, String tool, JsonNode args) {
        if (!SupportTools.NAMES.contains(tool)) return Verdict.deny("unknown tool");
        return switch (tool) {
            case "search_faq" -> args.path("query").isTextual() && args.get("query").asText().length() <= 500
                    ? Verdict.allow() : Verdict.deny("invalid query");
            case "lookup_order" -> ownedOrder(session, args).isPresent() ? Verdict.allow()
                    : Verdict.deny("order not found");   // same message whether it exists or not: no enumeration
            case "refund_order" -> authorizeRefund(session, args);
            default -> Verdict.deny("unknown tool");
        };
    }

    private Verdict authorizeRefund(Session session, JsonNode args) {
        Optional<OrderStore.Order> order = ownedOrder(session, args);
        if (order.isEmpty()) return Verdict.deny("order not found");
        BigDecimal amount;
        try {
            amount = new BigDecimal(args.path("amount").asText());
        } catch (NumberFormatException e) {
            return Verdict.deny("invalid amount");
        }
        if (amount.signum() <= 0) return Verdict.deny("amount must be positive");
        if (amount.compareTo(order.get().total()) > 0) return Verdict.deny("amount exceeds order total");
        if (!REFUNDABLE.contains(order.get().status())) return Verdict.deny("order is not yet refundable");
        if (session.refundedOrders().contains(order.get().id())) return Verdict.deny("order already refunded");
        if (amount.compareTo(autoApproveLimit) > 0) {
            return new Verdict(Decision.REQUIRE_APPROVAL, "refund above $" + autoApproveLimit + " needs human approval");
        }
        return Verdict.allow();
    }

    private Optional<OrderStore.Order> ownedOrder(Session session, JsonNode args) {
        String id = args.path("order_id").asText("");
        if (!id.matches("ORD-\\d{1,10}")) return Optional.empty();
        return store.find(id).filter(o -> o.customerId().equals(session.customerId()));
    }
}
