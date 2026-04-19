package com.example.sams.attendance.dto;

import java.time.Instant;

public record AttendanceRecordResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentUsername,
        String status,
        Instant markedAt
) {
}
