package com.example.sams.attendance.repository;

import com.example.sams.attendance.domain.AttendanceSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByCourseOfferingIdAndSessionDate(Long courseOfferingId, java.time.LocalDate sessionDate);
}
