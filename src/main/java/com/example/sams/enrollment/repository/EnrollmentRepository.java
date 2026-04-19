package com.example.sams.enrollment.repository;

import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByIdAndStudentUserId(Long enrollmentId, Long userId);

    Optional<Enrollment> findByStudentIdAndCourseOfferingId(Long studentId, Long courseOfferingId);

    List<Enrollment> findAllByCourseOfferingIdAndStatus(Long courseOfferingId, EnrollmentStatus status);

    long countByCourseOfferingIdAndStatus(Long courseOfferingId, EnrollmentStatus status);

    java.util.List<Enrollment> findAllByStatus(EnrollmentStatus status);

    long countByStatus(EnrollmentStatus status);

    @Query("""
            select count(e) > 0 from Enrollment e
            join e.courseOffering co
            join co.term term
            where e.student.id = :studentId
              and co.subject.id = :subjectId
              and e.status = :status
              and term.endDate < :targetTermStartDate
            """)
    boolean existsSatisfiedPrerequisiteEnrollment(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("status") EnrollmentStatus status,
            @Param("targetTermStartDate") LocalDate targetTermStartDate
    );

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

    @Query("""
            select distinct e from Enrollment e
            join e.courseOffering co
            join Exam exam on exam.courseOffering = co
            where e.student.user.id = :userId
              and (:termId is null or co.term.id = :termId)
              and exam.published = true
            """)
    Page<Enrollment> findStudentResultEnrollments(
            @Param("userId") Long userId,
            @Param("termId") Long termId,
            Pageable pageable
    );
}
