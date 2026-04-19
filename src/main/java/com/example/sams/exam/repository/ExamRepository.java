package com.example.sams.exam.repository;

import com.example.sams.exam.domain.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    long countByPublishedTrue();

    Optional<Exam> findByIdAndCourseOfferingTeacherUserId(Long examId, Long userId);

    Page<Exam> findAllByCourseOfferingTeacherUserId(Long userId, Pageable pageable);

    Page<Exam> findAllByCourseOfferingIdAndCourseOfferingTeacherUserId(Long offeringId, Long userId, Pageable pageable);

    List<Exam> findAllByCourseOfferingIdAndPublishedTrue(Long offeringId);
}
