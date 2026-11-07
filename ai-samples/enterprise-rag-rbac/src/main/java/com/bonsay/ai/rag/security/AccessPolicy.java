package com.bonsay.ai.rag.security;

import java.util.Collections;
import java.util.Set;

/**
 * Deny-by-default ACL check: a user may read a document only if they hold at least one of the
 * document's allowed roles. There is intentionally no "admin bypass" - executives get access by
 * being listed on documents, which keeps the audit trail honest.
 */
public final class AccessPolicy {
    private AccessPolicy() {}

    public static boolean canRead(UserPrincipal user, Set<Role> allowedRoles) {
        if (allowedRoles == null || allowedRoles.isEmpty()) return false;
        return !Collections.disjoint(user.roles(), allowedRoles);
    }
}
