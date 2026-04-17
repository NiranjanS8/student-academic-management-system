package com.example.sams.auth.dto;

import com.example.sams.user.domain.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserProfileResponse user
) {
    public record UserProfileResponse(
            Long id,
            String username,
            String email,
            Role role
    ) {
    }
}
