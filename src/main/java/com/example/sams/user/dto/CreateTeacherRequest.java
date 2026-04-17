package com.example.sams.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeacherRequest(
        @NotBlank(message = "username is required")
        @Size(max = 100, message = "username must be at most 100 characters")
        String username,
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,
        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,
        @NotBlank(message = "employeeCode is required")
        @Size(max = 50, message = "employeeCode must be at most 50 characters")
        String employeeCode,
        @NotNull(message = "departmentId is required")
        Long departmentId,
        @Size(max = 100, message = "designation must be at most 100 characters")
        String designation
) {
}
