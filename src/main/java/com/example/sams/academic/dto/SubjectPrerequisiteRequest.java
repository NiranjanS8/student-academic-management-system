package com.example.sams.academic.dto;

import jakarta.validation.constraints.NotNull;

public record SubjectPrerequisiteRequest(
        @NotNull(message = "prerequisiteSubjectId is required")
        Long prerequisiteSubjectId
) {
}
