package com.example.sams.notification.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.notification.dto.AnnouncementDispatchResponse;
import com.example.sams.notification.dto.TeacherAnnouncementRequest;
import com.example.sams.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/notifications")
public class TeacherNotificationController {

    private final NotificationService notificationService;

    public TeacherNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/announcements")
    public ApiResponse<AnnouncementDispatchResponse> publishAnnouncement(@Valid @RequestBody TeacherAnnouncementRequest request) {
        return ApiResponse.success(
                "Announcement published successfully",
                notificationService.publishTeacherAnnouncement(request)
        );
    }
}
