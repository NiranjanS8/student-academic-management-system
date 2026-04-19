package com.example.sams.attendance.repository;

import com.example.sams.attendance.domain.AttendanceRecord;
import com.example.sams.attendance.domain.AttendanceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    long countBySessionCourseOfferingIdAndStudentId(Long courseOfferingId, Long studentId);

    long countBySessionCourseOfferingIdAndStudentIdAndStatus(Long courseOfferingId, Long studentId, AttendanceStatus status);

    List<AttendanceRecord> findAllByStudentId(Long studentId);

    List<AttendanceRecord> findAllBySessionIdOrderByStudentStudentCodeAsc(Long sessionId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
}
