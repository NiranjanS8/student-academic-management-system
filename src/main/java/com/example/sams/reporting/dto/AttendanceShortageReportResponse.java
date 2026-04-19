package com.example.sams.reporting.dto;

import java.math.BigDecimal;

public record AttendanceShortageReportResponse(
        Long studentId,
        String studentCode,
        String studentUsername,
        Long courseOfferingId,
        String subjectCode,
        String subjectName,
        long totalSessions,
        long presentSessions,
        BigDecimal attendancePercentage
) {
}
