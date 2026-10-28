package com.aitutor.backend.repository;

import com.aitutor.backend.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {
    List<ExerciseAttempt> findAllByExercise_Lesson_PlanItem_Plan_Project_IdOrderBySubmittedAtDesc(UUID projectId);
}
