package com.example.sams.exam.service;

import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.dto.MarkEntryResponse;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class MarkEntryResponseMapper {

    public MarkEntryResponse toResponse(MarkEntry markEntry) {
        Student student = markEntry.getStudent();
        User user = student.getUser();

        return new MarkEntryResponse(
                markEntry.getId(),
                markEntry.getExam().getId(),
                new MarkEntryResponse.StudentSummary(
                        student.getId(),
                        student.getStudentCode(),
                        user.getUsername(),
                        user.getEmail()
                ),
                markEntry.getMarksObtained(),
                markEntry.getExam().getMaxMarks(),
                markEntry.getPercentageScore(),
                markEntry.getWeightedScore(),
                markEntry.getLetterGrade(),
                markEntry.getGradePoints(),
                markEntry.getRemarks(),
                markEntry.getCreatedAt(),
                markEntry.getUpdatedAt()
        );
    }
}
