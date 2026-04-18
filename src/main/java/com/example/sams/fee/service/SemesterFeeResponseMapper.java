package com.example.sams.fee.service;

import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.dto.SemesterFeeResponse;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.User;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SemesterFeeResponseMapper {

    public SemesterFeeResponse toResponse(SemesterFee semesterFee) {
        Student student = semesterFee.getStudent();
        User user = student.getUser();

        return new SemesterFeeResponse(
                semesterFee.getId(),
                new SemesterFeeResponse.StudentSummary(
                        student.getId(),
                        student.getStudentCode(),
                        user.getUsername(),
                        user.getEmail()
                ),
                new SemesterFeeResponse.TermSummary(
                        semesterFee.getTerm().getId(),
                        semesterFee.getTerm().getName(),
                        semesterFee.getTerm().getAcademicYear(),
                        semesterFee.getTerm().getStatus()
                ),
                semesterFee.getBaseAmount(),
                semesterFee.getFineAmount(),
                semesterFee.getTotalPayable(),
                semesterFee.getPaidAmount(),
                semesterFee.getTotalPayable().subtract(semesterFee.getPaidAmount()).max(BigDecimal.ZERO),
                semesterFee.getDueDate(),
                semesterFee.getStatus().name(),
                semesterFee.getCreatedAt(),
                semesterFee.getUpdatedAt()
        );
    }
}
