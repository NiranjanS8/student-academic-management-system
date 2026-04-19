package com.example.sams.reporting.dto;

import com.example.sams.exam.dto.StudentResultHistoryResponse;
import com.example.sams.exam.dto.StudentResultSummaryResponse;
import java.util.List;

public record StudentAcademicSnapshotResponse(
        Long studentId,
        String studentCode,
        String studentUsername,
        String programName,
        String sectionName,
        String currentTermName,
        StudentResultSummaryResponse summary,
        List<StudentResultHistoryResponse> publishedResults
) {
}
