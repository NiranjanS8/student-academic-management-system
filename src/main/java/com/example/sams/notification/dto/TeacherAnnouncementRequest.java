package com.example.sams.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeacherAnnouncementRequest(
        @NotNull Long courseOfferingId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 500) String message
) {
}
