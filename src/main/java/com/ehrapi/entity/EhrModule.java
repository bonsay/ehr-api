package com.ehrapi.entity;

import jakarta.persistence.*;

/**
 * Catalog entry describing a pluggable EHR capability (e.g. DEMOGRAPHICS,
 * ENCOUNTERS, PROBLEMS, MEDICATIONS, ALLERGIES, VITALS). Institutions choose
 * which of these modules to enable for their workflow.
 */
@Entity
@Table(name = "ehr_modules")
public class EhrModule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ehr_modules_seq")
    @SequenceGenerator(name = "ehr_modules_seq", sequenceName = "EHR_MODULES_ID_SEQ", allocationSize = 1)
    private Long id;

    /** Stable machine code used by the API and UI to key off this module. */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String category;

    /** API path segment exposed for this module, e.g. "encounters". */
    @Column(length = 100)
    private String apiPath;

    /** Whether the module is generally available in the catalog. */
    @Column(nullable = false)
    private boolean active = true;

    public EhrModule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
