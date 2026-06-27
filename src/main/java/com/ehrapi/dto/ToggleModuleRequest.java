package com.ehrapi.dto;

/** Request body to enable or disable a module for an institution. */
public class ToggleModuleRequest {
    private boolean enabled;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
