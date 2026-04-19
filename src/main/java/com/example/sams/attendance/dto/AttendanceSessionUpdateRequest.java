package com.example.sams.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AttendanceSessionUpdateRequest(
        @NotEmpty List<@Valid AttendanceRecordRequest> records
) {
}
