package com.ehrapi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A platform user (clinician or administrator). Each user belongs to one
 * institution and carries exactly one role, which resolves to a set of
 * permissions at authentication time.
 *
 * <p>The password hash is only meaningful in {@code local} security mode, where
 * the API authenticates users itself; in {@code oidc} mode the external identity
 * provider owns credentials and this row is used (if present) only to map the
 * subject to a role/institution.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_users_seq")
    @SequenceGenerator(name = "app_users_seq", sequenceName = "APP_USERS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /** BCrypt hash; never serialized back to clients. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "institution_id")
    private Long institutionId;

    /** References {@link Role#getCode()}. */
    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    public AppUser() {
        this.dateCreated = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
}
