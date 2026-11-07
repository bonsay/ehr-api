package com.bonsay.ai.gateway;

import com.bonsay.ai.gateway.routing.ModelTier;

/**
 * @param tierOverride  force a tier (null = auto-route)
 * @param cacheable     callers set false for personalised or time-sensitive prompts
 */
public record GatewayRequest(String tenant, String system, String prompt, double temperature,
                             ModelTier tierOverride, boolean cacheable) {

    public static GatewayRequest of(String tenant, String prompt) {
        return new GatewayRequest(tenant, "", prompt, 0.0, null, true);
    }
}
