package com.aitutor.backend.repository;

import com.aitutor.backend.entity.LearningPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningPlanItemRepository extends JpaRepository<LearningPlanItem, UUID> {

    List<LearningPlanItem> findAllByPlan_IdOrderBySequenceNumberAsc(UUID planId);
}
