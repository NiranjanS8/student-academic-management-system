package com.example.sams.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AcademicTermRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,
        @NotBlank(message = "academicYear is required")
        @Size(max = 20, message = "academicYear must be at most 20 characters")
        String academicYear,
        @NotNull(message = "startDate is required")
        LocalDate startDate,
        @NotNull(message = "endDate is required")
        LocalDate endDate,
        @NotBlank(message = "status is required")
        @Size(max = 30, message = "status must be at most 30 characters")
        String status
) {
}
