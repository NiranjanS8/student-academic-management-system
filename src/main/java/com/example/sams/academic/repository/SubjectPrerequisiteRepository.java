package com.example.sams.academic.repository;

import com.example.sams.academic.domain.SubjectPrerequisite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectPrerequisiteRepository extends JpaRepository<SubjectPrerequisite, Long> {

    boolean existsBySubjectIdAndPrerequisiteSubjectId(Long subjectId, Long prerequisiteSubjectId);

    List<SubjectPrerequisite> findAllBySubjectId(Long subjectId);

    void deleteBySubjectIdAndPrerequisiteSubjectId(Long subjectId, Long prerequisiteSubjectId);
}
