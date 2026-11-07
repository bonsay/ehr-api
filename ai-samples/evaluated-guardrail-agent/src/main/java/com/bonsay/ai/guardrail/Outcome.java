package com.bonsay.ai.guardrail;

/** Terminal states of an agent turn - every path ends in exactly one of these. */
public enum Outcome {
    /** Answered normally (possibly after tool calls). */
    ANSWERED,
    /** Declined for safety/policy reasons. */
    REFUSED,
    /** Handed to a human (approval needed, or the model chose to escalate). */
    ESCALATED,
    /** System degraded (LLM outage, malformed output, step budget) - safe canned reply + ticket. */
    FALLBACK
}
