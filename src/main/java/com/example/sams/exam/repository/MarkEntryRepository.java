package com.example.sams.exam.repository;

import com.example.sams.exam.domain.MarkEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkEntryRepository extends JpaRepository<MarkEntry, Long> {

    Optional<MarkEntry> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<MarkEntry> findByIdAndExamCourseOfferingTeacherUserId(Long markEntryId, Long userId);

    Page<MarkEntry> findAllByExamIdAndExamCourseOfferingTeacherUserId(Long examId, Long userId, Pageable pageable);

    long countByExamId(Long examId);

    List<MarkEntry> findAllByStudentIdAndExamCourseOfferingIdAndExamPublishedTrue(Long studentId, Long courseOfferingId);
}
