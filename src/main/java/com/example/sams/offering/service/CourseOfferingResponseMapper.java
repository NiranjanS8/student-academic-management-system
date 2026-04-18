package com.example.sams.offering.service;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.domain.Subject;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.user.domain.Teacher;
import com.example.sams.user.domain.User;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CourseOfferingResponseMapper {

    public CourseOfferingResponse toResponse(CourseOffering offering) {
        Subject subject = offering.getSubject();
        AcademicTerm term = offering.getTerm();
        Section section = offering.getSection();
        Teacher teacher = offering.getTeacher();
        User teacherUser = teacher.getUser();
        Instant now = Instant.now();
        boolean enrollmentCurrentlyOpen = offering.getStatus() == com.example.sams.offering.domain.CourseOfferingStatus.OPEN
                && (offering.getEnrollmentOpenAt() == null || !now.isBefore(offering.getEnrollmentOpenAt()))
                && (offering.getEnrollmentCloseAt() == null || !now.isAfter(offering.getEnrollmentCloseAt()));

        return new CourseOfferingResponse(
                offering.getId(),
                new CourseOfferingResponse.SubjectSummary(
                        subject.getId(),
                        subject.getCode(),
                        subject.getName()
                ),
                new CourseOfferingResponse.TermSummary(
                        term.getId(),
                        term.getName(),
                        term.getAcademicYear(),
                        term.getStatus()
                ),
                new CourseOfferingResponse.SectionSummary(
                        section.getId(),
                        section.getName()
                ),
                new CourseOfferingResponse.TeacherSummary(
                        teacher.getId(),
                        teacher.getEmployeeCode(),
                        teacherUser.getUsername(),
                        teacherUser.getEmail()
                ),
                offering.getCapacity(),
                offering.getRoomCode(),
                offering.getScheduleDays(),
                offering.getScheduleStartTime(),
                offering.getScheduleEndTime(),
                offering.getEnrollmentOpenAt(),
                offering.getEnrollmentCloseAt(),
                enrollmentCurrentlyOpen,
                offering.getStatus().name(),
                offering.getCreatedAt(),
                offering.getUpdatedAt()
        );
    }
}
