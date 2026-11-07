package com.bonsay.ai.rag.security;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Stand-in for an IdP / LDAP lookup. In production resolve roles from a verified JWT. */
public class UserDirectory {
    private final Map<String, UserPrincipal> users = Map.of(
            "alice", new UserPrincipal("alice", "Alice (Finance)", Set.of(Role.FINANCE)),
            "bob", new UserPrincipal("bob", "Bob (HR)", Set.of(Role.HR)),
            "carol", new UserPrincipal("carol", "Carol (Engineering)", Set.of(Role.ENGINEERING)),
            "dave", new UserPrincipal("dave", "Dave (CFO)", Set.of(Role.FINANCE, Role.EXECUTIVE)),
            "erin", new UserPrincipal("erin", "Erin (New hire)", Set.of()));

    public Optional<UserPrincipal> find(String id) {
        return Optional.ofNullable(id == null ? null : users.get(id));
    }
}
