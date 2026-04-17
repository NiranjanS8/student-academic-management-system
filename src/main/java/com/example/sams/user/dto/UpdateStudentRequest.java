package com.example.sams.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateStudentRequest(
        @NotBlank(message = "username is required")
        @Size(max = 100, message = "username must be at most 100 characters")
        String username,
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,
        @NotBlank(message = "studentCode is required")
        @Size(max = 50, message = "studentCode must be at most 50 characters")
        String studentCode,
        @NotNull(message = "departmentId is required")
        Long departmentId,
        @NotNull(message = "programId is required")
        Long programId,
        Long currentTermId,
        Long sectionId,
        @NotBlank(message = "academicStatus is required")
        String academicStatus,
        @NotBlank(message = "accountStatus is required")
        String accountStatus,
        @NotNull(message = "admissionDate is required")
        @PastOrPresent(message = "admissionDate cannot be in the future")
        LocalDate admissionDate
) {
}
