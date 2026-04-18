package com.example.sams.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotNull(message = "courseOfferingId is required")
        Long courseOfferingId
) {
}
