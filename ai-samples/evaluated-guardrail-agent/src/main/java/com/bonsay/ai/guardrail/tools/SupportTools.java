package com.bonsay.ai.guardrail.tools;

import com.bonsay.ai.guardrail.Session;
import com.bonsay.ai.llm.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Tool implementations. Authorization lives in {@code ToolPolicy}; these assume an approved call. */
public class SupportTools {

    public static final Set<String> NAMES = Set.of("lookup_order", "refund_order", "search_faq");

    private static final List<Map.Entry<String, String>> FAQ = List.of(
            Map.entry("return", "You can return unused items within 30 days of delivery for a full refund."),
            Map.entry("shipping", "Standard shipping takes 3-5 business days; express takes 1-2 business days."),
            Map.entry("warranty", "Electronics carry a 1-year limited manufacturer warranty."));

    private final OrderStore store;

    public SupportTools(OrderStore store) {
        this.store = store;
    }

    public OrderStore store() { return store; }

    public String execute(Session session, String tool, JsonNode args) {
        return switch (tool) {
            case "lookup_order" -> store.find(args.path("order_id").asText()).map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", o.id());
                m.put("status", o.status());
                m.put("total", o.total().toPlainString());
                m.put("email", o.email());
                m.put("paymentMethod", o.paymentMethod());
                return Json.write(m);
            }).orElse("{\"error\":\"order not found\"}");
            case "refund_order" -> {
                String id = args.path("order_id").asText();
                BigDecimal amount = new BigDecimal(args.path("amount").asText()).setScale(2, RoundingMode.HALF_UP);
                session.refundedOrders().add(id);
                yield Json.write(Map.of("order_id", id, "amount", amount.toPlainString(), "status", "refunded"));
            }
            case "search_faq" -> {
                String q = args.path("query").asText().toLowerCase(Locale.ROOT);
                yield FAQ.stream().filter(e -> q.contains(e.getKey())).findFirst()
                        .map(e -> Json.write(Map.of("answer", e.getValue())))
                        .orElse("{\"answer\":\"No matching policy found.\"}");
            }
            default -> throw new IllegalArgumentException("unknown tool " + tool);
        };
    }
}
