package com.example.sams.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProgramRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must be at most 50 characters")
        String code,
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,
        @NotNull(message = "departmentId is required")
        Long departmentId
) {
}
