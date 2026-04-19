package com.example.sams.reporting.dto;

public record AdminDashboardSummaryResponse(
        long totalStudents,
        long totalTeachers,
        long totalOfferings,
        long activeEnrollments,
        long publishedResults,
        long studentsWithOutstandingDues,
        long lowAttendanceCases
) {
}
