package com.example.sams.auth.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank(message = "security.jwt.secret must be configured")
        String secret,
        @Min(value = 1, message = "access token expiration must be at least 1 minute")
        long accessTokenExpirationMinutes,
        @Min(value = 1, message = "refresh token expiration must be at least 1 day")
        long refreshTokenExpirationDays
) {
}
