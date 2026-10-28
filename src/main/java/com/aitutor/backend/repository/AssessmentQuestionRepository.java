package com.aitutor.backend.repository;

import com.aitutor.backend.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    List<AssessmentQuestion> findAllByAssessment_IdOrderByQuestionOrderAsc(UUID assessmentId);
}
