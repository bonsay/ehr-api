package com.ehrapi.dto;

import com.ehrapi.entity.AppUser;

import java.time.LocalDateTime;

/** A user as exposed to admins — never includes the password hash. */
public record UserDto(
        Long id,
        String username,
        String fullName,
        Long institutionId,
        String roleCode,
        boolean enabled,
        LocalDateTime dateCreated) {

    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getInstitutionId(),
                u.getRoleCode(), u.isEnabled(), u.getDateCreated());
    }
}
