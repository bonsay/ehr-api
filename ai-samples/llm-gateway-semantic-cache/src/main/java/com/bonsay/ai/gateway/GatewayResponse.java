package com.bonsay.ai.gateway;

import java.util.List;

public record GatewayResponse(String text, String model, String tier, boolean cacheHit, double similarity,
                              double costUsd, long latencyMs, List<String> routingReasons) {
}
