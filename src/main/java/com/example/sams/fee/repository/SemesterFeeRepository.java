package com.example.sams.fee.repository;

import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.domain.SemesterFeeStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SemesterFeeRepository extends JpaRepository<SemesterFee, Long> {

    Optional<SemesterFee> findByStudentIdAndTermId(Long studentId, Long termId);

    @Query("""
            select sf from SemesterFee sf
            where (:studentId is null or sf.student.id = :studentId)
              and (:termId is null or sf.term.id = :termId)
              and (:status is null or sf.status = :status)
            """)
    Page<SemesterFee> search(
            @Param("studentId") Long studentId,
            @Param("termId") Long termId,
            @Param("status") SemesterFeeStatus status,
            Pageable pageable
    );
}
