package com.example.sams.notification.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.notification.dto.NotificationBulkActionResponse;
import com.example.sams.notification.dto.NotificationResponse;
import com.example.sams.notification.dto.UnreadNotificationCountResponse;
import com.example.sams.notification.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class UserNotificationController {

    private final NotificationService notificationService;

    public UserNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<NotificationResponse>> listMyNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Notifications fetched successfully",
                PageResponse.from(notificationService.listMyNotifications(read, pageable))
        );
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> getUnreadCount() {
        return ApiResponse.success("Unread notification count fetched successfully", notificationService.getUnreadCount());
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        return ApiResponse.success("Notification marked as read", notificationService.markAsRead(notificationId));
    }

    @PostMapping("/{notificationId}/unread")
    public ApiResponse<NotificationResponse> markAsUnread(@PathVariable Long notificationId) {
        return ApiResponse.success("Notification marked as unread", notificationService.markAsUnread(notificationId));
    }

    @PostMapping("/me/read-all")
    public ApiResponse<NotificationBulkActionResponse> markAllAsRead() {
        return ApiResponse.success("All notifications marked as read", notificationService.markAllAsRead());
    }
}
