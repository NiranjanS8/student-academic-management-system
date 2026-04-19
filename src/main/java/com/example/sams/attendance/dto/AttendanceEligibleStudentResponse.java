package com.example.sams.attendance.dto;

public record AttendanceEligibleStudentResponse(
        Long studentId,
        String studentCode,
        String username
) {
}
