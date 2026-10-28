package com.aitutor.backend.repository;

import com.aitutor.backend.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    Optional<AssessmentAttempt> findFirstByAssessment_IdOrderByAttemptNumberDesc(UUID assessmentId);

    List<AssessmentAttempt> findAllByAssessment_IdOrderByAttemptNumberDesc(UUID assessmentId);
    List<AssessmentAttempt> findAllByAssessment_Project_IdOrderBySubmittedAtDesc(UUID projectId);
}
