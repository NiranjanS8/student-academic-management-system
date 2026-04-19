package com.example.sams.reporting.dto;

import java.util.List;

public record AdminStudentAnalyticsSummaryResponse(
        long totalStudents,
        long activeStudents,
        long onHoldStudents,
        long graduatedStudents,
        long droppedStudents,
        long studentsWithoutSection,
        long studentsWithoutCurrentTerm,
        List<CountBreakdown> departments,
        List<CountBreakdown> programs,
        List<CountBreakdown> terms
) {
    public record CountBreakdown(
            Long id,
            String name,
            long studentCount
    ) {
    }
}
