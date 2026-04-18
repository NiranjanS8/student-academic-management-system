package com.example.sams.enrollment.repository;

import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByIdAndStudentUserId(Long enrollmentId, Long userId);

    Optional<Enrollment> findByStudentIdAndCourseOfferingId(Long studentId, Long courseOfferingId);

    @Query("""
            select e from Enrollment e
            join e.courseOffering co
            where e.student.user.id = :userId
              and (:termId is null or co.term.id = :termId)
              and (:status is null or e.status = :status)
            """)
    Page<Enrollment> searchStudentEnrollments(
            @Param("userId") Long userId,
            @Param("termId") Long termId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable
    );
}
