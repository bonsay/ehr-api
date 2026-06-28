package com.ehrapi.dto;

import java.util.List;

/** Replaces the permission set of a role. */
public record UpdateRolePermissionsRequest(List<String> permissions) {
}
