package com.example.sams.auth.dto;

import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Role;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        Role role,
        AccountStatus accountStatus,
        StudentSummary student,
        TeacherSummary teacher
) {
    public record StudentSummary(
            Long id,
            String studentCode,
            String academicStatus
    ) {
    }

    public record TeacherSummary(
            Long id,
            String employeeCode,
            String designation
    ) {
    }
}
