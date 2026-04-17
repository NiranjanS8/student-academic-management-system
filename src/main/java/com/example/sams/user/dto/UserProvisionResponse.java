package com.example.sams.user.dto;

import com.example.sams.user.domain.Role;

public record UserProvisionResponse(
        Long userId,
        Role role,
        String username,
        String email,
        Long profileId,
        String profileCode,
        String message
) {
}
