package com.example.sams.offering.repository;

import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    boolean existsBySubjectIdAndTermIdAndSectionId(Long subjectId, Long termId, Long sectionId);

    boolean existsBySubjectIdAndTermIdAndSectionIdAndIdNot(Long subjectId, Long termId, Long sectionId, Long id);

    Page<CourseOffering> findAllByTermId(Long termId, Pageable pageable);

    Page<CourseOffering> findAllBySectionId(Long sectionId, Pageable pageable);

    Page<CourseOffering> findAllByTeacherId(Long teacherId, Pageable pageable);

    Page<CourseOffering> findAllBySubjectId(Long subjectId, Pageable pageable);

    Page<CourseOffering> findAllByStatus(CourseOfferingStatus status, Pageable pageable);

    Page<CourseOffering> findAllByTeacherUserId(Long userId, Pageable pageable);

    List<CourseOffering> findAllByTeacherId(Long teacherId);

    Optional<CourseOffering> findByIdAndTeacherUserId(Long offeringId, Long userId);

    @Query("""
            select co from CourseOffering co
            where (:termId is null or co.term.id = :termId)
              and (:sectionId is null or co.section.id = :sectionId)
              and (:teacherId is null or co.teacher.id = :teacherId)
              and (:subjectId is null or co.subject.id = :subjectId)
              and (:status is null or co.status = :status)
            """)
    Page<CourseOffering> search(
            @Param("termId") Long termId,
            @Param("sectionId") Long sectionId,
            @Param("teacherId") Long teacherId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseOfferingStatus status,
            Pageable pageable
    );

    @Query("""
            select co from CourseOffering co
            where co.teacher.user.id = :teacherUserId
              and (:termId is null or co.term.id = :termId)
              and (:sectionId is null or co.section.id = :sectionId)
              and (:subjectId is null or co.subject.id = :subjectId)
              and (:status is null or co.status = :status)
            """)
    Page<CourseOffering> searchAssignedToTeacher(
            @Param("teacherUserId") Long teacherUserId,
            @Param("termId") Long termId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseOfferingStatus status,
            Pageable pageable
    );

    @Query("""
            select co from CourseOffering co
            where co.section.id = :sectionId
              and co.term.id = :termId
              and (:subjectId is null or co.subject.id = :subjectId)
              and (:status is null or co.status = :status)
            """)
    Page<CourseOffering> searchVisibleToStudent(
            @Param("sectionId") Long sectionId,
            @Param("termId") Long termId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseOfferingStatus status,
            Pageable pageable
    );

    @Query("""
            select co from CourseOffering co
            where co.id = :offeringId
              and co.section.id = :sectionId
              and co.term.id = :termId
            """)
    Optional<CourseOffering> findVisibleToStudent(
            @Param("offeringId") Long offeringId,
            @Param("sectionId") Long sectionId,
            @Param("termId") Long termId
    );
}
