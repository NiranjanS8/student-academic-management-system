package com.example.sams.user.controller;

import com.example.sams.auth.dto.CurrentUserResponse;
import com.example.sams.auth.service.AuthService;
import com.example.sams.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser() {
        return ApiResponse.success("Current user fetched successfully", authService.getCurrentUser());
    }
}
