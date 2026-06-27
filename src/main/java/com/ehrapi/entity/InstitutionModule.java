package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Join record realising the "pick and choose" model: it records that a given
 * institution has enabled a given module from the catalog.
 */
@Entity
@Table(name = "institution_modules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"institution_id", "module_code"}))
public class InstitutionModule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inst_modules_seq")
    @SequenceGenerator(name = "inst_modules_seq", sequenceName = "INST_MODULES_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "module_code", nullable = false, length = 50)
    private String moduleCode;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "enabled_date")
    private LocalDateTime enabledDate;

    public InstitutionModule() {
        this.enabledDate = LocalDateTime.now();
    }

    public InstitutionModule(Long institutionId, String moduleCode) {
        this();
        this.institutionId = institutionId;
        this.moduleCode = moduleCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getEnabledDate() { return enabledDate; }
    public void setEnabledDate(LocalDateTime enabledDate) { this.enabledDate = enabledDate; }
}
