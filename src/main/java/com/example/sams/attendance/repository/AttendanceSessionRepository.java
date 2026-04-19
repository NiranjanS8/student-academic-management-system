package com.example.sams.attendance.repository;

import com.example.sams.attendance.domain.AttendanceSession;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByCourseOfferingIdAndSessionDate(Long courseOfferingId, java.time.LocalDate sessionDate);

    Optional<AttendanceSession> findByIdAndCourseOfferingTeacherUserId(Long sessionId, Long teacherUserId);

    Page<AttendanceSession> findAllByCourseOfferingTeacherUserId(Long teacherUserId, Pageable pageable);

    Page<AttendanceSession> findAllByCourseOfferingIdAndCourseOfferingTeacherUserId(Long courseOfferingId, Long teacherUserId, Pageable pageable);
}
