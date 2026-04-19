package com.example.sams.notification.dto;

public record AnnouncementDispatchResponse(
        Long courseOfferingId,
        String title,
        int recipientCount
) {
}
