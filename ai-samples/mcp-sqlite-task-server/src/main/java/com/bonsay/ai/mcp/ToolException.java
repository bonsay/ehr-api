package com.bonsay.ai.mcp;

/** A tool-level failure that is reported back to the model (isError=true) instead of crashing. */
public class ToolException extends RuntimeException {
    public ToolException(String message) { super(message); }

    public ToolException(String message, Throwable cause) { super(message, cause); }
}
