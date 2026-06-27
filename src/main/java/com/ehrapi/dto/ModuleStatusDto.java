package com.ehrapi.dto;

/**
 * A catalog module together with whether a given institution has enabled it.
 * Drives the "pick and choose" marketplace UI.
 */
public class ModuleStatusDto {

    private String code;
    private String name;
    private String description;
    private String category;
    private String apiPath;
    private boolean enabled;

    public ModuleStatusDto() {}

    public ModuleStatusDto(String code, String name, String description, String category,
                           String apiPath, boolean enabled) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.apiPath = apiPath;
        this.enabled = enabled;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
