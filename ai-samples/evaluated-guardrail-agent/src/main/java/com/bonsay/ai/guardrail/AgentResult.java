package com.bonsay.ai.guardrail;

import java.util.List;

public record AgentResult(Outcome outcome, String answer, List<String> toolsCalled, List<AuditEvent> audit, int steps) {

    public record AuditEvent(String type, String detail) {}
}
