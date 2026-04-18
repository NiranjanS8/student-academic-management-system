package com.example.sams.exam.service;

import com.example.sams.exam.domain.Exam;
import com.example.sams.exam.dto.ExamResponse;
import org.springframework.stereotype.Component;

@Component
public class ExamResponseMapper {

    public ExamResponse toResponse(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getCourseOffering().getId(),
                exam.getTitle(),
                exam.getExamType(),
                exam.getMaxMarks(),
                exam.getWeightage(),
                exam.getScheduledAt(),
                exam.isPublished(),
                exam.getPublishedAt(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
