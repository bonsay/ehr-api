package com.bonsay.ai.guardrail.model;

import com.bonsay.ai.llm.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, deliberately NAIVE model used for CI evals: it tries to do whatever the
 * customer asks (refund any amount, look up any order, echo payment details). That is the
 * point - the guardrails, not the model, must keep the system safe.
 */
public class SimulatedSupportModel implements LlmClient {

    private static final Pattern ORDER = Pattern.compile("ORD-\\d+");
    private static final Pattern AMOUNT = Pattern.compile("\\$(\\d+(?:\\.\\d{1,2})?)");

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<ChatMessage> msgs = request.messages();
        String input = msgs.get(0).content();
        String lower = input.toLowerCase(Locale.ROOT);
        JsonNode lookup = lastResult(msgs, "lookup_order");
        JsonNode refund = lastResult(msgs, "refund_order");
        JsonNode faq = lastResult(msgs, "search_faq");
        Matcher om = ORDER.matcher(input);
        String orderId = om.find() ? om.group() : null;

        String out;
        if (lower.contains("speak to a human") || lower.contains("talk to a person")) {
            out = escalate("customer requested a human");
        } else if (orderId != null && (lower.contains("refund") || lower.contains("money back"))) {
            if (lookup == null) out = tool("lookup_order", "{\"order_id\":\"" + orderId + "\"}");
            else if (isError(lookup)) out = fin("I'm sorry, I couldn't find order " + orderId + " on your account.");
            else if (refund == null) {
                Matcher am = AMOUNT.matcher(input);
                String amount = am.find() ? am.group(1) : lookup.path("total").asText();
                out = tool("refund_order", "{\"order_id\":\"" + orderId + "\",\"amount\":" + amount
                        + ",\"reason\":\"customer request\"}");
            } else if (isError(refund)) out = fin("I couldn't process that refund: " + refund.path("error").asText());
            else out = fin("Your refund of $" + refund.path("amount").asText() + " for order " + orderId
                        + " has been issued. It will appear in 3-5 business days.");
        } else if (orderId != null) {
            if (lookup == null) out = tool("lookup_order", "{\"order_id\":\"" + orderId + "\"}");
            else if (isError(lookup)) out = fin("I'm sorry, I couldn't find order " + orderId + " on your account.");
            else if (lower.contains("card") || lower.contains("payment")) out = fin("Order " + orderId
                    + " was paid with " + lookup.path("paymentMethod").asText() + ".");
            else if (lower.contains("email")) out = fin("The email on order " + orderId + " is "
                    + lookup.path("email").asText() + ".");
            else out = fin("Order " + orderId + " is " + lookup.path("status").asText() + " (total $"
                        + lookup.path("total").asText() + ").");
        } else if (lower.matches(".*\\b(return|shipping|warranty|policy|policies)\\b.*")) {
            if (faq == null) out = tool("search_faq", "{\"query\":" + Json.write(input) + "}");
            else out = fin(faq.path("answer").asText("I couldn't find that in our policies."));
        } else {
            out = fin("I can help with order status, refunds and store policies. What do you need?");
        }
        return LlmResponse.of(out, request.model() == null ? "simulated" : request.model());
    }

    private static JsonNode lastResult(List<ChatMessage> msgs, String tool) {
        String prefix = "TOOL_RESULT " + tool + ": ";
        for (int i = msgs.size() - 1; i >= 0; i--) {
            String c = msgs.get(i).content();
            if (msgs.get(i).role().equals("user") && c.startsWith(prefix)) return Json.read(c.substring(prefix.length()));
        }
        return null;
    }

    private static boolean isError(JsonNode n) { return n.has("error"); }

    private static String tool(String name, String args) {
        return "{\"action\":\"tool\",\"tool\":\"" + name + "\",\"args\":" + args + "}";
    }

    private static String fin(String answer) {
        return "{\"action\":\"final\",\"answer\":" + Json.write(answer) + "}";
    }

    private static String escalate(String reason) {
        return "{\"action\":\"escalate\",\"reason\":" + Json.write(reason) + "}";
    }
}
