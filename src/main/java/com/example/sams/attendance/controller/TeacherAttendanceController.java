package com.example.sams.attendance.controller;

import com.example.sams.attendance.dto.AttendanceEligibleStudentResponse;
import com.example.sams.attendance.dto.AttendanceSessionRequest;
import com.example.sams.attendance.dto.AttendanceSessionResponse;
import com.example.sams.attendance.dto.AttendanceSessionUpdateRequest;
import com.example.sams.attendance.service.TeacherAttendanceService;
import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/attendance")
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;

    public TeacherAttendanceController(TeacherAttendanceService teacherAttendanceService) {
        this.teacherAttendanceService = teacherAttendanceService;
    }

    @PostMapping("/sessions")
    public ApiResponse<AttendanceSessionResponse> createSession(@Valid @RequestBody AttendanceSessionRequest request) {
        return ApiResponse.success("Attendance session created successfully", teacherAttendanceService.createAttendanceSession(request));
    }

    @PutMapping("/sessions/{sessionId}")
    public ApiResponse<AttendanceSessionResponse> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody AttendanceSessionUpdateRequest request
    ) {
        return ApiResponse.success("Attendance session updated successfully", teacherAttendanceService.updateAttendanceSession(sessionId, request));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResponse<AttendanceSessionResponse>> listSessions(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sessionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Attendance sessions fetched successfully",
                PageResponse.from(teacherAttendanceService.listAssignedSessions(offeringId, pageable))
        );
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AttendanceSessionResponse> getSession(@PathVariable Long sessionId) {
        return ApiResponse.success("Attendance session fetched successfully", teacherAttendanceService.getAssignedSessionById(sessionId));
    }

    @GetMapping("/offerings/{offeringId}/students")
    public ApiResponse<List<AttendanceEligibleStudentResponse>> listEligibleStudents(@PathVariable Long offeringId) {
        return ApiResponse.success(
                "Eligible students fetched successfully",
                teacherAttendanceService.listEligibleStudents(offeringId)
        );
    }
}
