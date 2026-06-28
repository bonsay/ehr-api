package com.ehrapi.security;

import com.ehrapi.common.ModuleCodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical authorization vocabulary for the platform.
 *
 * <p>A <b>permission</b> is a granted authority string. Clinical permissions are
 * scoped per module and action ({@code MODULE:ACTION}, e.g. {@code VITALS:WRITE},
 * {@code MEDICATIONS:WRITE}). Administrative permissions gate the management
 * surface ({@code ADMIN:USERS}, {@code ADMIN:ROLES}, {@code ADMIN:MODULES}).
 *
 * <p>Roles are bundles of permissions. The constants here are the <em>default</em>
 * role definitions used to seed the database; an administrator can re-shape any
 * non-system role's permissions at runtime via the admin API.
 */
public final class Permissions {

    // ---- Actions --------------------------------------------------------------
    public static final String READ = "READ";
    public static final String WRITE = "WRITE";

    // ---- Administrative permissions ------------------------------------------
    public static final String ADMIN_USERS = "ADMIN:USERS";
    public static final String ADMIN_ROLES = "ADMIN:ROLES";
    public static final String ADMIN_MODULES = "ADMIN:MODULES";

    // ---- Clinical modules that carry READ/WRITE permissions ------------------
    /** Modules that participate in the per-module READ/WRITE permission scheme. */
    public static final List<String> CLINICAL_MODULES = List.of(
            ModuleCodes.DEMOGRAPHICS, ModuleCodes.ENCOUNTERS, ModuleCodes.PROBLEMS,
            ModuleCodes.MEDICATIONS, ModuleCodes.ALLERGIES, ModuleCodes.VITALS);

    // ---- Default role codes ---------------------------------------------------
    public static final String ROLE_ADMINISTRATOR = "ADMINISTRATOR";
    public static final String ROLE_PHYSICIAN = "PHYSICIAN";
    public static final String ROLE_NURSE = "NURSE";
    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";

    private Permissions() {}

    /** Builds a {@code MODULE:ACTION} permission string. */
    public static String of(String module, String action) {
        return module + ":" + action;
    }

    /** Read permission for every clinical module. */
    public static Set<String> allReads() {
        return CLINICAL_MODULES.stream().map(m -> of(m, READ))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /** The complete catalog of assignable permissions, grouped for the admin UI. */
    public static Map<String, List<String>> catalog() {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("Administration", List.of(ADMIN_USERS, ADMIN_ROLES, ADMIN_MODULES));
        for (String module : CLINICAL_MODULES) {
            grouped.put(module, List.of(of(module, READ), of(module, WRITE)));
        }
        return grouped;
    }

    /**
     * Default permission set for a built-in role. Returns {@code null} for an
     * unknown code. These mirror the clinical reality the platform models:
     * a nurse records vitals but cannot prescribe; a physician can.
     */
    public static Set<String> defaultsFor(String roleCode) {
        Set<String> p = new java.util.LinkedHashSet<>();
        switch (roleCode) {
            case ROLE_ADMINISTRATOR -> {
                // Runs the institution: manages users, roles and modules, plus
                // read visibility across every clinical module.
                p.add(ADMIN_USERS);
                p.add(ADMIN_ROLES);
                p.add(ADMIN_MODULES);
                p.addAll(allReads());
            }
            case ROLE_PHYSICIAN -> {
                // Full clinical authorship, including prescribing.
                p.addAll(allReads());
                for (String m : CLINICAL_MODULES) {
                    p.add(of(m, WRITE));
                }
            }
            case ROLE_NURSE -> {
                // Records vitals, encounters and allergies; may NOT prescribe
                // medications or alter the problem list/demographics.
                p.addAll(allReads());
                p.add(of(ModuleCodes.VITALS, WRITE));
                p.add(of(ModuleCodes.ENCOUNTERS, WRITE));
                p.add(of(ModuleCodes.ALLERGIES, WRITE));
            }
            case ROLE_RECEPTIONIST -> {
                // Front desk: registers/updates patients, reads encounters.
                p.add(of(ModuleCodes.DEMOGRAPHICS, READ));
                p.add(of(ModuleCodes.DEMOGRAPHICS, WRITE));
                p.add(of(ModuleCodes.ENCOUNTERS, READ));
            }
            default -> {
                return null;
            }
        }
        return p;
    }

    /** Human-readable descriptions for the built-in roles (for seeding/UI). */
    public static String descriptionFor(String roleCode) {
        return switch (roleCode) {
            case ROLE_ADMINISTRATOR -> "Manages users, roles and enabled modules for the institution.";
            case ROLE_PHYSICIAN -> "Full clinical access including prescribing medications.";
            case ROLE_NURSE -> "Records vitals, encounters and allergies; cannot prescribe medications.";
            case ROLE_RECEPTIONIST -> "Registers and updates patient demographics; reads encounters.";
            default -> "";
        };
    }

    /** The built-in roles, in display order. */
    public static List<String> builtInRoles() {
        return List.of(ROLE_ADMINISTRATOR, ROLE_PHYSICIAN, ROLE_NURSE, ROLE_RECEPTIONIST);
    }
}
