package com.bonsay.ai.rag.security;

import java.util.EnumSet;
import java.util.Set;

/** An authenticated caller. Every user implicitly holds {@link Role#ALL_EMPLOYEES}. */
public record UserPrincipal(String id, String displayName, Set<Role> roles) {
    public UserPrincipal {
        EnumSet<Role> r = EnumSet.of(Role.ALL_EMPLOYEES);
        r.addAll(roles);
        roles = Set.copyOf(r);
    }
}
