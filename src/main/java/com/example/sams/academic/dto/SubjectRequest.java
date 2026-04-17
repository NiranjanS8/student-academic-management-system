package com.example.sams.academic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubjectRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must be at most 50 characters")
        String code,
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,
        @NotNull(message = "credits is required")
        @DecimalMin(value = "0.50", message = "credits must be at least 0.50")
        BigDecimal credits,
        @NotNull(message = "departmentId is required")
        Long departmentId,
        @NotNull(message = "active is required")
        Boolean active
) {
}
