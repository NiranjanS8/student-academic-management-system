package com.example.sams.user.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.user.dto.CreateStudentRequest;
import com.example.sams.user.dto.CreateTeacherRequest;
import com.example.sams.user.dto.TeacherProfileResponse;
import com.example.sams.user.dto.UpdateTeacherRequest;
import com.example.sams.user.dto.UserProvisionResponse;
import com.example.sams.user.service.TeacherAdministrationService;
import com.example.sams.user.service.UserProvisioningService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserProvisioningService userProvisioningService;
    private final TeacherAdministrationService teacherAdministrationService;

    public AdminUserController(
            UserProvisioningService userProvisioningService,
            TeacherAdministrationService teacherAdministrationService
    ) {
        this.userProvisioningService = userProvisioningService;
        this.teacherAdministrationService = teacherAdministrationService;
    }

    @PostMapping("/teachers")
    public ApiResponse<UserProvisionResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return ApiResponse.success("Teacher account created successfully", userProvisioningService.createTeacher(request));
    }

    @PostMapping("/students")
    public ApiResponse<UserProvisionResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ApiResponse.success("Student account created successfully", userProvisioningService.createStudent(request));
    }

    @GetMapping("/teachers/{teacherId}")
    public ApiResponse<TeacherProfileResponse> getTeacher(@PathVariable Long teacherId) {
        return ApiResponse.success("Teacher fetched successfully", teacherAdministrationService.getTeacherById(teacherId));
    }

    @GetMapping("/teachers")
    public ApiResponse<Page<TeacherProfileResponse>> listTeachers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeCode") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Teachers fetched successfully",
                teacherAdministrationService.listTeachers(departmentId, query, pageable)
        );
    }

    @PutMapping("/teachers/{teacherId}")
    public ApiResponse<TeacherProfileResponse> updateTeacher(
            @PathVariable Long teacherId,
            @Valid @RequestBody UpdateTeacherRequest request
    ) {
        return ApiResponse.success("Teacher updated successfully", teacherAdministrationService.updateTeacher(teacherId, request));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}
