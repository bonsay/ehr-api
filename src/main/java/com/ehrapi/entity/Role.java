package com.ehrapi.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A named bundle of permissions assigned to users (e.g. PHYSICIAN, NURSE,
 * ADMINISTRATOR). The permission set determines which clinical modules a user
 * may read or write and whether they can reach the administrative surface.
 *
 * <p>Built-in roles are flagged {@code systemRole} and cannot be deleted, but an
 * administrator may still adjust their permissions to fit local policy.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_seq")
    @SequenceGenerator(name = "roles_seq", sequenceName = "ROLES_ID_SEQ", allocationSize = 1)
    private Long id;

    /** Stable machine code, e.g. "PHYSICIAN". Used as the user's role reference. */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** Built-in role: editable permissions but not removable. */
    @Column(name = "system_role", nullable = false)
    private boolean systemRole = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", length = 100, nullable = false)
    private Set<String> permissions = new LinkedHashSet<>();

    public Role() {}

    public Role(String code, String name, String description, boolean systemRole, Set<String> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        this.permissions = permissions != null ? new LinkedHashSet<>(permissions) : new LinkedHashSet<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemRole() { return systemRole; }
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions != null ? new LinkedHashSet<>(permissions) : new LinkedHashSet<>();
    }
}
