package com.example.sams.user.dto;

import com.example.sams.user.domain.AccountStatus;
import java.time.Instant;

public record TeacherProfileResponse(
        Long teacherId,
        Long userId,
        String username,
        String email,
        AccountStatus accountStatus,
        String employeeCode,
        DepartmentSummary department,
        String designation,
        Instant createdAt,
        Instant updatedAt
) {
    public record DepartmentSummary(
            Long id,
            String code,
            String name
    ) {
    }
}
