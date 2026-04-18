package com.example.sams.offering.repository;

import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    Page<CourseOffering> findAllByTermId(Long termId, Pageable pageable);

    Page<CourseOffering> findAllBySectionId(Long sectionId, Pageable pageable);

    Page<CourseOffering> findAllByTeacherId(Long teacherId, Pageable pageable);

    Page<CourseOffering> findAllBySubjectId(Long subjectId, Pageable pageable);

    Page<CourseOffering> findAllByStatus(CourseOfferingStatus status, Pageable pageable);

    Page<CourseOffering> findAllByTeacherUserId(Long userId, Pageable pageable);

    List<CourseOffering> findAllByTeacherId(Long teacherId);
}
