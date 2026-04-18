package com.example.sams.enrollment.service;

import com.example.sams.academic.domain.Subject;
import com.example.sams.academic.domain.SubjectPrerequisite;
import com.example.sams.academic.repository.SubjectPrerequisiteRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.user.domain.Student;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentPrerequisiteValidator {

    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentPrerequisiteValidator(
            SubjectPrerequisiteRepository subjectPrerequisiteRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.subjectPrerequisiteRepository = subjectPrerequisiteRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public void validate(Student student, CourseOffering offering) {
        LocalDate targetTermStartDate = offering.getTerm().getStartDate();

        List<String> missingPrerequisiteCodes = subjectPrerequisiteRepository.findAllBySubjectId(offering.getSubject().getId())
                .stream()
                .map(SubjectPrerequisite::getPrerequisiteSubject)
                .filter(prerequisiteSubject -> !isSatisfied(student, prerequisiteSubject, targetTermStartDate))
                .map(Subject::getCode)
                .toList();

        if (!missingPrerequisiteCodes.isEmpty()) {
            throw new ConflictException("Missing prerequisite subjects: " + String.join(", ", missingPrerequisiteCodes));
        }
    }

    private boolean isSatisfied(Student student, Subject prerequisiteSubject, LocalDate targetTermStartDate) {
        return enrollmentRepository.existsSatisfiedPrerequisiteEnrollment(
                student.getId(),
                prerequisiteSubject.getId(),
                EnrollmentStatus.ENROLLED,
                targetTermStartDate
        );
    }
}
