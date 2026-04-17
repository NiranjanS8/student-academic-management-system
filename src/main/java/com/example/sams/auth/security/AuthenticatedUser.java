package com.example.sams.auth.security;

import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Role;

public record AuthenticatedUser(
        Long userId,
        String username,
        String email,
        Role role,
        AccountStatus accountStatus
) {
}
