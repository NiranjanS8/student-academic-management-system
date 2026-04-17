package com.example.sams.user.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.user.dto.CreateStudentRequest;
import com.example.sams.user.dto.CreateTeacherRequest;
import com.example.sams.user.dto.UserProvisionResponse;
import com.example.sams.user.service.UserProvisioningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserProvisioningService userProvisioningService;

    public AdminUserController(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/teachers")
    public ApiResponse<UserProvisionResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return ApiResponse.success("Teacher account created successfully", userProvisioningService.createTeacher(request));
    }

    @PostMapping("/students")
    public ApiResponse<UserProvisionResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ApiResponse.success("Student account created successfully", userProvisioningService.createStudent(request));
    }
}
