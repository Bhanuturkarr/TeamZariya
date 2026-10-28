package com.aitutor.backend.repository;

import com.aitutor.backend.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, UUID> {
    List<LearningPlan> findAllByProject_IdOrderByCreatedAtDesc(UUID projectId);
}
