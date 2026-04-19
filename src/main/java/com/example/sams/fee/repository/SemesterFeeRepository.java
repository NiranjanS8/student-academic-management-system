package com.example.sams.fee.repository;

import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.domain.SemesterFeeStatus;
import java.time.LocalDate;
import java.util.List;
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

    Page<SemesterFee> findAllByStudentUserId(Long userId, Pageable pageable);

    @Query("""
            select sf from SemesterFee sf
            where sf.student.id = :studentId
              and sf.totalPayable > sf.paidAmount
              and (:termId is null or sf.term.id <= :termId)
            """)
    List<SemesterFee> findBlockingDues(
            @Param("studentId") Long studentId,
            @Param("termId") Long termId
    );

    List<SemesterFee> findAllByStudentId(Long studentId);

    @Query("""
            select sf from SemesterFee sf
            where sf.totalPayable > sf.paidAmount
              and sf.dueDate <= :maxDueDate
            """)
    List<SemesterFee> findOutstandingReminderCandidates(@Param("maxDueDate") LocalDate maxDueDate);

    @Query("""
            select sf from SemesterFee sf
            join sf.student s
            join s.user u
            where sf.totalPayable > sf.paidAmount
              and (:termId is null or sf.term.id = :termId)
              and (:programId is null or s.program.id = :programId)
              and (:sectionId is null or s.section.id = :sectionId)
              and (
                    :query is null
                    or lower(s.studentCode) like lower(concat('%', :query, '%'))
                    or lower(u.username) like lower(concat('%', :query, '%'))
                    or lower(u.email) like lower(concat('%', :query, '%'))
              )
            """)
    Page<SemesterFee> searchDefaulters(
            @Param("termId") Long termId,
            @Param("programId") Long programId,
            @Param("sectionId") Long sectionId,
            @Param("query") String query,
            Pageable pageable
    );
}
