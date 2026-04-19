package com.example.sams.attendance.service;

import com.example.sams.attendance.domain.AttendanceRecord;
import com.example.sams.attendance.domain.AttendanceSession;
import com.example.sams.attendance.dto.AttendanceEligibleStudentResponse;
import com.example.sams.attendance.dto.AttendanceRecordResponse;
import com.example.sams.attendance.dto.AttendanceSessionResponse;
import com.example.sams.user.domain.Student;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AttendanceResponseMapper {

    public AttendanceSessionResponse toSessionResponse(AttendanceSession session, List<AttendanceRecord> records) {
        List<AttendanceRecordResponse> content = records.stream()
                .map(this::toRecordResponse)
                .toList();

        return new AttendanceSessionResponse(
                session.getId(),
                session.getCourseOffering().getId(),
                session.getCourseOffering().getSubject().getCode(),
                session.getCourseOffering().getSubject().getName(),
                session.getSessionDate(),
                content.size(),
                content
        );
    }

    public AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getId(),
                record.getStudent().getId(),
                record.getStudent().getStudentCode(),
                record.getStudent().getUser().getUsername(),
                record.getStatus().name(),
                record.getMarkedAt()
        );
    }

    public AttendanceEligibleStudentResponse toEligibleStudentResponse(Student student) {
        return new AttendanceEligibleStudentResponse(
                student.getId(),
                student.getStudentCode(),
                student.getUser().getUsername()
        );
    }
}
