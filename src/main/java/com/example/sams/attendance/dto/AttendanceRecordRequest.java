package com.example.sams.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceRecordRequest(
        @NotNull Long studentId,
        @NotBlank String status
) {
}
