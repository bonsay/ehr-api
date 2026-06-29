package com.ehrapi.dto;

import com.ehrapi.entity.EntitlementStatus;
import com.ehrapi.entity.ModuleTier;
import com.ehrapi.entity.PriceModel;

/**
 * A catalog module together with whether a given institution has enabled it and
 * its commercial state. Drives the "pick and choose" marketplace / storefront UI.
 */
public class ModuleStatusDto {

    private String code;
    private String name;
    private String description;
    private String category;
    private String apiPath;
    private boolean enabled;

    // ---- Commercial fields ---------------------------------------------------
    private ModuleTier tier;
    private PriceModel priceModel;
    private Integer priceMonthlyCents;
    /** Whether the institution may currently enable/use this module. */
    private boolean entitled;
    /** Stored entitlement status, if any (null = never licensed). */
    private EntitlementStatus entitlementStatus;

    public ModuleStatusDto() {}

    public ModuleStatusDto(String code, String name, String description, String category,
                           String apiPath, boolean enabled, ModuleTier tier, PriceModel priceModel,
                           Integer priceMonthlyCents, boolean entitled, EntitlementStatus entitlementStatus) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.apiPath = apiPath;
        this.enabled = enabled;
        this.tier = tier;
        this.priceModel = priceModel;
        this.priceMonthlyCents = priceMonthlyCents;
        this.entitled = entitled;
        this.entitlementStatus = entitlementStatus;
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

    public ModuleTier getTier() { return tier; }
    public void setTier(ModuleTier tier) { this.tier = tier; }

    public PriceModel getPriceModel() { return priceModel; }
    public void setPriceModel(PriceModel priceModel) { this.priceModel = priceModel; }

    public Integer getPriceMonthlyCents() { return priceMonthlyCents; }
    public void setPriceMonthlyCents(Integer priceMonthlyCents) { this.priceMonthlyCents = priceMonthlyCents; }

    public boolean isEntitled() { return entitled; }
    public void setEntitled(boolean entitled) { this.entitled = entitled; }

    public EntitlementStatus getEntitlementStatus() { return entitlementStatus; }
    public void setEntitlementStatus(EntitlementStatus entitlementStatus) { this.entitlementStatus = entitlementStatus; }
}
