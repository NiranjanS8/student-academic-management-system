package com.example.sams.fee.repository;

import com.example.sams.fee.domain.SemesterFee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterFeeRepository extends JpaRepository<SemesterFee, Long> {
}
