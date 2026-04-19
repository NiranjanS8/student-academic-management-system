package com.example.sams.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceSessionResponse(
        Long id,
        Long courseOfferingId,
        String subjectCode,
        String subjectName,
        LocalDate sessionDate,
        int totalRecords,
        List<AttendanceRecordResponse> records
) {
}
