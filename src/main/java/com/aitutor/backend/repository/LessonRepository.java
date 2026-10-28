package com.aitutor.backend.repository;

import com.aitutor.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findAllByPlanItem_Plan_IdOrderByPlanItem_SequenceNumberAsc(UUID planId);

    List<Lesson> findAllByPlanItem_IdInOrderByPlanItem_SequenceNumberAsc(List<UUID> planItemIds);

    java.util.Optional<Lesson> findFirstByPlanItem_Id(UUID planItemId);
}
