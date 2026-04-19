package com.example.sams.reporting.dto;

import java.math.BigDecimal;

public record TeacherWorkloadReportResponse(
        Long teacherId,
        String employeeCode,
        String teacherUsername,
        String teacherEmail,
        String departmentName,
        String designation,
        long totalOfferings,
        long openOfferings,
        long closedOfferings,
        long archivedOfferings,
        long totalAssignedStudents,
        long publishedExamCount,
        BigDecimal averageCapacityUtilization
) {
}
