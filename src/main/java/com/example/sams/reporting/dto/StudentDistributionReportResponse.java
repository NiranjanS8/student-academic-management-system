package com.example.sams.reporting.dto;

public record StudentDistributionReportResponse(
        Long departmentId,
        String departmentName,
        Long programId,
        String programName,
        Long termId,
        String termName,
        String academicYear,
        Long sectionId,
        String sectionName,
        String academicStatus,
        long studentCount
) {
}
